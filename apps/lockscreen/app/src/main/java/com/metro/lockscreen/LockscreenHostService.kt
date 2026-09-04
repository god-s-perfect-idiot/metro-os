package com.metro.lockscreen

import android.app.ActivityOptions
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.telephony.TelephonyManager
import android.util.Log
import android.view.Display
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.metro.system.MetroLockscreen
import com.metro.system.MetroStatusBar
import com.metro.ui.MetroSystemTheme
/**
 * Foreground host for the Metro lock surface.
 *
 * **Render path:** [TYPE_ACCESSIBILITY_OVERLAY] hosted by [LockscreenAccessibilityService].
 *
 * **Swipe-up path:** surface animates fully off-screen past threshold, then this service
 * hands off to SystemUI via [LockscreenBouncerActivity]. After a committed hand-off the
 * Metro fill stays suppressed until the next [Intent.ACTION_SCREEN_OFF] (sleep / lock).
 */
class LockscreenHostService :
    Service(),
    LifecycleOwner,
    SavedStateRegistryOwner,
    ViewModelStoreOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    override val viewModelStore = ViewModelStore()

    private val handler = Handler(Looper.getMainLooper())
    private val attachLock = Any()

    private var overlayRoot: FrameLayout? = null
    private var overlayBlackCover: View? = null
    private var overlayView: ComposeView? = null
    private var overlayManager: WindowManager? = null
    private var overlayMode: LockscreenPresentationMode? = null
    private var receiverRegistered = false
    private var phoneReceiverRegistered = false
    private var displayListenerRegistered = false
    private var powerSaveReceiverRegistered = false

    /**
     * After a committed swipe-up, do not re-attach the Metro fill until the next screen-off
     * (power button / sleep), regardless of biometric success or failure.
     */
    @Volatile
    private var handedOffUntilScreenOff = false

    /** Suppress requests from [MetroLockscreen] (incoming call UI, alarms, …). */
    private var criticalOverlaySuppressedByContract = false

    /** Suppress while telephony reports RINGING / OFFHOOK (fallback when dialer broadcast lags). */
    private var criticalOverlaySuppressedByPhone = false

    private val presentRetries = longArrayOf(0L, 50L, 150L, 400L, 1000L)
    private val glancePresentRetries = longArrayOf(0L, 8L, 16L, 32L, 64L, 128L, 250L, 500L, 1000L)

    private val glanceSleepWatcher = object : Runnable {
        override fun run() {
            val prefs = LockscreenPreferences(this@LockscreenHostService)
            if (prefs.enabled && prefs.glanceEnabled) {
                tryPresentGlanceEarly()
            }
            val delay =
                if (prefs.enabled && prefs.glanceEnabled) GLANCE_SLEEP_WATCH_MS
                else GLANCE_SLEEP_WATCH_IDLE_MS
            handler.postDelayed(this, delay)
        }
    }

    private val tickRunnable = object : Runnable {
        override fun run() {
            syncOverlayToKeyguard()
            handler.postDelayed(this, TICK_MS)
        }
    }

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_ON -> {
                    schedulePresentAttempts()
                }
                Intent.ACTION_SCREEN_OFF -> {
                    // Next wake may show Metro again — clear hand-off from this lock session.
                    handedOffUntilScreenOff = false
                    handler.removeCallbacksAndMessages(PRESENT_TOKEN)
                    LockscreenBouncerActivity.finishIfShowing()
                    // Synchronous — must beat system AOD paint on the first doze frame.
                    tryPresentGlanceEarly()
                    schedulePresentAttempts()
                }
                Intent.ACTION_USER_PRESENT -> {
                    removeOverlay()
                    LockscreenBouncerActivity.finishIfShowing()
                }
            }
        }
    }

    private val phoneReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return
            val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
            val suppress = LockscreenLogic.shouldSuppressForPhoneState(state)
            handler.post {
                criticalOverlaySuppressedByPhone = suppress
                syncOverlayToKeyguard()
            }
        }
    }

    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayChanged(displayId: Int) {
            if (displayId != Display.DEFAULT_DISPLAY) return
            syncOverlayToKeyguard()
        }

        override fun onDisplayAdded(displayId: Int) = Unit

        override fun onDisplayRemoved(displayId: Int) = Unit
    }

    private val powerSaveReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != PowerManager.ACTION_POWER_SAVE_MODE_CHANGED) return
            handler.post { syncOverlayToKeyguard() }
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        criticalOverlaySuppressedByContract = criticalOverlaySuppressedPersisted
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        startForeground(NOTIFICATION_ID, buildNotification())
        registerScreenReceiver()
        registerPhoneReceiver()
        registerDisplayListener()
        registerPowerSaveReceiver()
        handler.post(tickRunnable)
        handler.post(glanceSleepWatcher)
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
        schedulePresentAttempts()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!LockscreenPreferences(this).enabled) {
            stopSelf()
            return START_NOT_STICKY
        }
        when (intent?.action) {
            MetroLockscreen.ACTION_SET_SUPPRESSED -> {
                val suppressed = intent.getBooleanExtra(
                    MetroLockscreen.EXTRA_SUPPRESSED,
                    criticalOverlaySuppressedPersisted,
                )
                handler.post { applyCriticalSuppressFromContract(suppressed) }
            }
        }
        schedulePresentAttempts()
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        unregisterScreenReceiver()
        unregisterPhoneReceiver()
        unregisterDisplayListener()
        unregisterPowerSaveReceiver()
        removeOverlay()
        LockscreenBouncerActivity.finishIfShowing()
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        viewModelStore.clear()
        instance = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun schedulePresentAttempts() {
        handler.removeCallbacksAndMessages(PRESENT_TOKEN)
        val delays = if (LockscreenPreferences(this).glanceEnabled) {
            glancePresentRetries
        } else {
            presentRetries
        }
        for (delay in delays) {
            handler.postAtTime(
                { syncOverlayToKeyguard() },
                PRESENT_TOKEN,
                SystemClock.uptimeMillis() + delay,
            )
        }
    }

    private fun syncOverlayToKeyguard() {
        val prefs = LockscreenPreferences(this)
        if (!prefs.enabled) {
            removeOverlay()
            return
        }
        if (LockscreenBouncerActivity.isShowing()) {
            removeOverlay()
            return
        }

        val locked = LockscreenKeyguard.isLocked(this)
        val displayAwake = LockscreenKeyguard.isDisplayAwake(this)
        val critical = isCriticalOverlaySuppressed()
        val mode = when {
            LockscreenLogic.shouldPresentLock(
                enabled = true,
                keyguardLocked = locked,
                displayAwake = displayAwake,
                handedOff = handedOffUntilScreenOff,
                criticalOverlaySuppressed = critical,
            ) -> LockscreenPresentationMode.Lock
            LockscreenLogic.shouldPresentGlance(
                enabled = true,
                glanceEnabled = prefs.glanceEnabled,
                keyguardLocked = locked,
                displayAwake = displayAwake,
                batterySaverOn = LockscreenKeyguard.isBatterySaverOn(this),
                criticalOverlaySuppressed = critical,
            ) -> LockscreenPresentationMode.Glance
            else -> null
        }
        if (mode != null) {
            ensureOverlayShowing(mode)
        } else {
            removeOverlay()
        }
    }

    /**
     * Fast path for glance: present black chrome the moment the device stops being interactive.
     * Called synchronously from [Intent.ACTION_SCREEN_OFF] and from the 16ms sleep watcher.
     */
    private fun tryPresentGlanceEarly(): Boolean {
        val prefs = LockscreenPreferences(this)
        if (!prefs.enabled || !prefs.glanceEnabled) return false
        if (LockscreenBouncerActivity.isShowing()) return false
        if (!LockscreenKeyguard.isLocked(this)) return false
        if (LockscreenKeyguard.isDisplayAwake(this)) return false
        if (LockscreenKeyguard.isBatterySaverOn(this)) return false
        if (isCriticalOverlaySuppressed()) return false
        ensureOverlayShowing(LockscreenPresentationMode.Glance)
        return true
    }

    private fun ensureOverlayShowing(mode: LockscreenPresentationMode) {
        synchronized(attachLock) {
            if (overlayView != null && overlayMode == mode) return
            val existing = overlayView
            if (existing != null) {
                updateOverlayMode(existing, mode)
                return
            }
            attachOverlayLocked(mode)
        }
    }

    private fun removeOverlay() {
        synchronized(attachLock) {
            removeOverlayLocked()
        }
    }

    private fun attachOverlayLocked(mode: LockscreenPresentationMode) {
        val accessibilityHost = LockscreenAccessibilityService.getInstance()
        if (accessibilityHost == null) {
            Log.w(TAG, "Accessibility service not connected — cannot draw over keyguard")
            return
        }

        val isGlance = mode == LockscreenPresentationMode.Glance
        val host: Context = accessibilityHost
        val windowType = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        val topInsetPx = statusBarInsetPx()
        val blackCover = View(host).apply {
            setBackgroundColor(android.graphics.Color.BLACK)
            visibility = if (isGlance) View.VISIBLE else View.GONE
        }
        val composeView = ComposeView(host).apply {
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            suppressSystemBarInsets()
            bindOverlayContent(mode, topInsetPx)
        }
        val root = FrameLayout(host).apply {
            setViewTreeLifecycleOwner(this@LockscreenHostService)
            setViewTreeSavedStateRegistryOwner(this@LockscreenHostService)
            setViewTreeViewModelStoreOwner(this@LockscreenHostService)
            addView(
                blackCover,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                ),
            )
            addView(
                composeView,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                ),
            )
            suppressSystemBarInsets()
        }

        val manager = host.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        try {
            manager.addView(root, createLayoutParams(windowType, notTouchable = isGlance))
            overlayRoot = root
            overlayBlackCover = blackCover
            overlayView = composeView
            overlayManager = manager
            overlayMode = mode
            // Lock draws its own transparent tray; hide the opaque system Metro tray.
            MetroStatusBar.requestFullscreen(this, fullscreen = true)
            Log.i(TAG, "Lock overlay attached ($mode)")
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to attach lock overlay", t)
            runCatching { manager.removeView(root) }
            overlayRoot = null
            overlayBlackCover = null
            overlayView = null
            overlayManager = null
            overlayMode = null
        }
    }

    private fun updateOverlayMode(view: ComposeView, mode: LockscreenPresentationMode) {
        overlayBlackCover?.visibility =
            if (mode == LockscreenPresentationMode.Glance) View.VISIBLE else View.GONE
        applyOverlayTouchPolicy(mode)
        view.bindOverlayContent(mode, statusBarInsetPx())
        overlayMode = mode
        Log.i(TAG, "Lock overlay morphed ($mode)")
    }

    private fun ComposeView.bindOverlayContent(
        mode: LockscreenPresentationMode,
        topInsetPx: Int,
    ) {
        setContent {
            MetroSystemTheme {
                LockscreenSurface(
                    mode = mode,
                    onUnlockCommitted = {
                        handler.post { commitSwipeUnlock() }
                    },
                    topInsetPx = topInsetPx,
                )
            }
        }
    }

    private fun applyOverlayTouchPolicy(mode: LockscreenPresentationMode) {
        val manager = overlayManager ?: return
        val root = overlayRoot ?: return
        val params = root.layoutParams as? WindowManager.LayoutParams ?: return
        val notTouchable = mode == LockscreenPresentationMode.Glance
        var flags = params.flags
        flags = if (notTouchable) {
            flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        } else {
            flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
        }
        if (flags == params.flags) return
        params.flags = flags
        manager.updateViewLayout(root, params)
    }

    private fun removeOverlayLocked() {
        val root = overlayRoot
        val manager = overlayManager
        overlayRoot = null
        overlayBlackCover = null
        overlayView = null
        overlayManager = null
        overlayMode = null
        if (root != null && manager != null) {
            runCatching { manager.removeView(root) }
                .onFailure { Log.w(TAG, "removeView failed", it) }
            MetroStatusBar.requestFullscreen(this, fullscreen = false)
        }
    }

    /**
     * Panel has finished sliding off-screen. Hand off to SystemUI and keep Metro suppressed
     * until the next screen-off — biometric fail/cancel must not bring the fill back.
     *
     * Always remove the fill first so touches reach SystemUI, then start the trampoline
     * and retry aggressively — a single BAL-blocked start must not leave a dead lock.
     */
    private fun commitSwipeUnlock() {
        handedOffUntilScreenOff = true
        removeOverlay()
        launchBouncerActivity()
        // Immediate a11y swipe in parallel — covers devices where activity start is delayed.
        handler.post {
            if (!LockscreenBouncerActivity.isShowing()) {
                LockscreenAccessibilityService.getInstance()?.injectSwipeUpToBouncer()
            }
        }
        handler.postDelayed({
            if (!LockscreenBouncerActivity.isShowing()) {
                Log.w(TAG, "Bouncer activity not up — retry + gesture fallback")
                LockscreenAccessibilityService.getInstance()?.injectSwipeUpToBouncer()
                launchBouncerActivity()
            }
        }, 180L)
        handler.postDelayed({
            if (!LockscreenBouncerActivity.isShowing() && LockscreenKeyguard.isLocked(this)) {
                Log.w(TAG, "Bouncer still missing — final retry")
                launchBouncerActivity()
                LockscreenAccessibilityService.getInstance()?.injectSwipeUpToBouncer()
            }
        }, 500L)
    }

    private fun launchBouncerActivity() {
        val intent = bouncerIntent()

        val a11y = LockscreenAccessibilityService.getInstance()
        if (a11y != null) {
            runCatching { a11y.startActivity(intent) }
                .onSuccess { Log.i(TAG, "bouncer started via accessibility") }
                .onFailure { Log.w(TAG, "bouncer a11y startActivity failed", it) }
        }

        runCatching {
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            val pending = PendingIntent.getActivity(this, REQUEST_BOUNCER, intent, flags)
            pending.send(this, 0, null, null, null, null, activityStartOptions())
        }.onFailure { Log.w(TAG, "bouncer PendingIntent send failed", it) }

        runCatching { fireBouncerFullScreenIntent(intent) }
            .onFailure { Log.w(TAG, "bouncer FSI failed", it) }
    }

    private fun bouncerIntent(): Intent =
        Intent(this, LockscreenBouncerActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
            addFlags(Intent.FLAG_ACTIVITY_NO_USER_ACTION)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        }

    private fun fireBouncerFullScreenIntent(intent: Intent) {
        val manager = getSystemService(NotificationManager::class.java) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !areNotificationsEnabled()
        ) {
            Log.w(TAG, "notifications disabled — skipping FSI")
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
            !manager.canUseFullScreenIntent()
        ) {
            Log.w(TAG, "USE_FULL_SCREEN_INTENT not granted — skipping FSI")
            return
        }
        val channelId = "metro_lockscreen_bouncer"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    channelId,
                    getString(R.string.unlock_notification_channel),
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    setSound(null, null)
                    enableVibration(false)
                },
            )
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val fullScreen = PendingIntent.getActivity(this, REQUEST_BOUNCER_FSI, intent, flags)
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(getString(R.string.unlock_title))
            .setContentText(getString(R.string.unlock_description))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreen, true)
            .setAutoCancel(true)
            .setTimeoutAfter(2_500L)
            .build()
        manager.notify(NOTIFICATION_BOUNCER_ID, notification)
        handler.postDelayed({ manager.cancel(NOTIFICATION_BOUNCER_ID) }, 2_000L)
    }

    private fun areNotificationsEnabled(): Boolean {
        val manager = getSystemService(NotificationManager::class.java) ?: return false
        return manager.areNotificationsEnabled()
    }

    /** Options for PendingIntent.send — never for PendingIntent.getActivity. */
    private fun activityStartOptions(): android.os.Bundle? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return null
        return ActivityOptions.makeBasic().apply {
            setPendingIntentBackgroundActivityStartMode(
                ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED,
            )
        }.toBundle()
    }

    private fun createLayoutParams(
        windowType: Int,
        notTouchable: Boolean = false,
    ): WindowManager.LayoutParams {
        var flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        if (notTouchable) {
            flags = flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        }
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            windowType,
            flags,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                fitInsetsTypes = 0
                fitInsetsSides = 0
            }
        }
    }

    private fun View.suppressSystemBarInsets() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            setOnApplyWindowInsetsListener { _, _ -> WindowInsets.CONSUMED }
        } else {
            @Suppress("DEPRECATION")
            systemUiVisibility = systemUiVisibility or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        }
    }

    /**
     * Height of the system status-bar / cutout band (same region as Metro status tray).
     * Floor at [MetroStatusBar.HEIGHT_DP] so icons always have a tray-sized band.
     */
    private fun statusBarInsetPx(): Int {
        val density = resources.displayMetrics.density
        val topPx: Int =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val wm = getSystemService(WINDOW_SERVICE) as WindowManager
                val insets = wm.currentWindowMetrics.windowInsets
                val statusBars = insets.getInsets(WindowInsets.Type.statusBars())
                val cutoutTop = insets.getInsets(WindowInsets.Type.displayCutout()).top
                maxOf(statusBars.top, cutoutTop)
            } else {
                val resId = resources.getIdentifier("status_bar_height", "dimen", "android")
                if (resId > 0) {
                    resources.getDimensionPixelSize(resId)
                } else {
                    (24 * density).toInt()
                }
            }
        val minTrayPx = (MetroStatusBar.HEIGHT_DP * density).toInt()
        return maxOf(topPx, minTrayPx)
    }

    private fun isCriticalOverlaySuppressed(): Boolean =
        criticalOverlaySuppressedByContract || criticalOverlaySuppressedByPhone

    private fun applyCriticalSuppressFromContract(suppressed: Boolean) {
        criticalOverlaySuppressedPersisted = suppressed
        criticalOverlaySuppressedByContract = suppressed
        syncOverlayToKeyguard()
    }

    private fun registerPhoneReceiver() {
        if (phoneReceiverRegistered || !LockscreenSignalSource.canReadCellular(this)) return
        val filter = IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED)
        ContextCompat.registerReceiver(
            this,
            phoneReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        phoneReceiverRegistered = true
    }

    private fun unregisterPhoneReceiver() {
        if (!phoneReceiverRegistered) return
        runCatching { unregisterReceiver(phoneReceiver) }
        phoneReceiverRegistered = false
    }

    private fun registerDisplayListener() {
        if (displayListenerRegistered) return
        val manager = getSystemService(DisplayManager::class.java) ?: return
        manager.registerDisplayListener(displayListener, handler)
        displayListenerRegistered = true
    }

    private fun unregisterDisplayListener() {
        if (!displayListenerRegistered) return
        val manager = getSystemService(DisplayManager::class.java) ?: return
        runCatching { manager.unregisterDisplayListener(displayListener) }
        displayListenerRegistered = false
    }

    private fun registerPowerSaveReceiver() {
        if (powerSaveReceiverRegistered) return
        val filter = IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
        ContextCompat.registerReceiver(
            this,
            powerSaveReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        powerSaveReceiverRegistered = true
    }

    private fun unregisterPowerSaveReceiver() {
        if (!powerSaveReceiverRegistered) return
        runCatching { unregisterReceiver(powerSaveReceiver) }
        powerSaveReceiverRegistered = false
    }

    private fun registerScreenReceiver() {
        if (receiverRegistered) return
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        ContextCompat.registerReceiver(
            this,
            screenReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        receiverRegistered = true
    }

    private fun unregisterScreenReceiver() {
        if (!receiverRegistered) return
        runCatching { unregisterReceiver(screenReceiver) }
        receiverRegistered = false
    }

    private fun buildNotification(): Notification {
        val channelId = "metro_lockscreen_host"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(
                    channelId,
                    getString(R.string.overlay_notification_title),
                    NotificationManager.IMPORTANCE_LOW,
                ),
            )
        }
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle(getString(R.string.overlay_notification_title))
            .setContentText(getString(R.string.overlay_notification_body))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val TAG = "LockscreenHost"
        private const val NOTIFICATION_ID = 1006
        private const val NOTIFICATION_BOUNCER_ID = 1008
        private const val REQUEST_BOUNCER = 46
        private const val REQUEST_BOUNCER_FSI = 47
        private const val TICK_MS = 500L
        private const val GLANCE_SLEEP_WATCH_MS = 16L
        private const val GLANCE_SLEEP_WATCH_IDLE_MS = 500L
        private val PRESENT_TOKEN = Any()

        @Volatile
        private var instance: LockscreenHostService? = null

        @Volatile
        private var criticalOverlaySuppressedPersisted = false

        fun isRunning(): Boolean = instance != null

        fun notifyBouncerVisible() {
            instance?.let { svc ->
                svc.handedOffUntilScreenOff = true
                svc.handler.post {
                    svc.removeOverlay()
                    svc.getSystemService(NotificationManager::class.java)
                        ?.cancel(NOTIFICATION_BOUNCER_ID)
                }
            }
        }

        fun requestGestureBouncerFallback() {
            instance?.handler?.post {
                LockscreenAccessibilityService.getInstance()?.injectSwipeUpToBouncer()
            }
        }

        fun onAccessibilityServiceConnected() {
            instance?.let { svc ->
                svc.handler.post { svc.schedulePresentAttempts() }
            }
        }

        fun onAccessibilityServiceDisconnected() {
            instance?.let { svc -> svc.handler.post { svc.removeOverlay() } }
        }

        fun start(context: Context) {
            if (!LockscreenPreferences(context).enabled) return
            val intent = Intent(context, LockscreenHostService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, LockscreenHostService::class.java))
        }

        fun applyMasterToggle(context: Context, enabled: Boolean) {
            LockscreenPreferences(context).enabled = enabled
            if (enabled) start(context) else stop(context)
        }

        /**
         * Tears down and re-attaches the lock overlay so background / accent changes apply
         * while the host is already running and the keyguard is showing.
         */
        fun requestRehost() {
            instance?.let { svc ->
                svc.handler.post {
                    svc.removeOverlay()
                    svc.syncOverlayToKeyguard()
                }
            }
        }

        /**
         * Forwards a [MetroLockscreen] contract request to the running host (or starts it).
         * Mirrors [com.metro.statusbar.StatusBarOverlayService.deliver].
         */
        fun deliver(context: Context, action: String, source: Intent) {
            when (action) {
                MetroLockscreen.ACTION_SET_SUPPRESSED -> {
                    val suppressed = source.getBooleanExtra(
                        MetroLockscreen.EXTRA_SUPPRESSED,
                        criticalOverlaySuppressedPersisted,
                    )
                    criticalOverlaySuppressedPersisted = suppressed
                    val svc = instance
                    if (svc != null) {
                        svc.handler.post { svc.applyCriticalSuppressFromContract(suppressed) }
                    } else if (LockscreenPreferences(context).enabled) {
                        val intent = Intent(context, LockscreenHostService::class.java).apply {
                            this.action = action
                            putExtras(source)
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            context.startForegroundService(intent)
                        } else {
                            context.startService(intent)
                        }
                    }
                }
            }
        }
    }
}
