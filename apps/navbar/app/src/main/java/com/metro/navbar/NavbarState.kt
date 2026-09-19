package com.metro.navbar

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.metro.system.MetroAppBranding
import com.metro.system.MetroAppRegistry
import com.metro.system.MetroBroadcasts
import com.metro.system.MetroPreferences
import java.util.concurrent.Executors

class NavbarState(context: Context) {
    private val appContext = context.applicationContext
    private val preferences = MetroPreferences(appContext)
    private val navbarPrefs = NavbarPreferences(appContext)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val brandingExecutor = Executors.newSingleThreadExecutor()

    var theme by mutableStateOf(NavbarThemeResolver.resolve(preferences, navbarPrefs.backgroundMode))
        private set

    /** User swipe hide/reveal — independent of immersive / contract hide. */
    var userVisible by mutableStateOf(true)
        private set

    var visibilityMode by mutableStateOf(NavbarVisibilityMode.Opaque)
        private set

    var foregroundPackage by mutableStateOf<String?>(null)
        private set

    var appBackgroundColor by mutableStateOf<Color?>(null)
        private set

    /**
     * True while Android system navigation bars are hidden (immersive / fullscreen). The Metro
     * overlay must not paint over fullscreen content.
     */
    var systemNavigationBarsHidden by mutableStateOf(false)
        private set

    /** Contract or immersive hide — no reveal strip, full creep away. */
    val chromeForcedHidden: Boolean
        get() = visibilityMode == NavbarVisibilityMode.Hidden || systemNavigationBarsHidden

    /** Full three-key bar on screen. */
    val showFullBar: Boolean
        get() = userVisible && !chromeForcedHidden

    /** Slim swipe-reveal strip (user hid the bar; not immersive). */
    val showRevealStrip: Boolean
        get() = !userVisible && !chromeForcedHidden

    private val themeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == MetroBroadcasts.ACTION_THEME_CHANGED) {
                refreshTheme()
            }
        }
    }

    fun refreshTheme() {
        preferences.pullThemeFromProvider()
        // Drop legacy override so ListPicker modes (default black / match / accent) apply.
        if (preferences.navBarColorHex != null) {
            preferences.navBarColorHex = null
        }
        when (navbarPrefs.backgroundMode) {
            NavbarBackgroundMode.MatchAppBackground -> Unit
            NavbarBackgroundMode.DefaultBlackBackground,
            NavbarBackgroundMode.ShowAccentColor,
            -> appBackgroundColor = null
        }
        theme = resolveTheme()
    }

    fun applyVisibilityMode(mode: NavbarVisibilityMode) {
        if (visibilityMode == mode) return
        visibilityMode = mode
    }

    fun applySystemNavigationBarsHidden(hidden: Boolean) {
        if (systemNavigationBarsHidden == hidden) return
        systemNavigationBarsHidden = hidden
    }

    /**
     * Monotonic stamp for foreground updates. Event-sourced packages (and Start) bump this so a
     * slow windows probe cannot re-apply the previous app after returning to the Start screen.
     */
    private var foregroundEpoch: Long = 0L
    private var lastAuthoritativeUptimeMs: Long = 0L

    /** Bumps and returns a new foreground epoch for authoritative package updates. */
    fun nextForegroundEpoch(): Long {
        foregroundEpoch += 1L
        return foregroundEpoch
    }

    fun foregroundEpochPeek(): Long = foregroundEpoch

    /**
     * Tracks the foreground package. In match-app mode Metro suite apps stay black; other apps
     * use their launcher-icon tile brand color. Accent / default-black modes ignore the package
     * for fill.
     *
     * @param epoch when non-null, ignored if older than the latest authoritative update
     * @param authoritative when true, bumps [foregroundEpoch] (Start key / window-state events)
     * @param fromProbe when true, ignored briefly after an authoritative update and when the
     *   captured [epoch] no longer matches (stale getRoot results after Start)
     */
    fun applyForegroundPackage(
        packageName: String?,
        epoch: Long? = null,
        authoritative: Boolean = false,
        fromProbe: Boolean = false,
    ) {
        if (epoch != null && epoch < foregroundEpoch) return
        if (fromProbe) {
            if (epoch != null && epoch != foregroundEpoch) return
            val sinceAuth = SystemClock.uptimeMillis() - lastAuthoritativeUptimeMs
            if (sinceAuth < PROBE_QUIET_AFTER_AUTHORITATIVE_MS) return
        }
        when {
            authoritative -> {
                nextForegroundEpoch()
                lastAuthoritativeUptimeMs = SystemClock.uptimeMillis()
            }
            epoch != null && epoch > foregroundEpoch -> foregroundEpoch = epoch
        }

        val normalized = packageName?.takeUnless { ForegroundAppDetector.isIgnored(it) }
        if (foregroundPackage == normalized) {
            if (navbarPrefs.backgroundMode == NavbarBackgroundMode.MatchAppBackground &&
                appBackgroundColor == null
            ) {
                resolveIconBackgroundForForeground()
            }
            return
        }
        foregroundPackage = normalized
        if (navbarPrefs.backgroundMode != NavbarBackgroundMode.MatchAppBackground) {
            appBackgroundColor = null
            theme = resolveTheme()
            return
        }
        resolveIconBackgroundForForeground()
    }

    /** Re-reads the navbar background ListPicker and resolves fill for the current app. */
    fun applyBackgroundModePreference() {
        if (navbarPrefs.backgroundMode != NavbarBackgroundMode.MatchAppBackground) {
            appBackgroundColor = null
            theme = resolveTheme()
            return
        }
        resolveIconBackgroundForForeground()
    }

    private fun resolveIconBackgroundForForeground() {
        val pkg = foregroundPackage
        when {
            pkg.isNullOrBlank() || MetroAppRegistry.isMetroSuite(pkg) -> {
                applyResolvedBackground(Color.Black)
            }
            else -> {
                // Bitmap / adaptive-icon sampling is too heavy for the main thread.
                brandingExecutor.execute {
                    val next = MetroAppBranding.resolveIconForegroundColor(appContext, pkg)
                    mainHandler.post {
                        if (foregroundPackage != pkg) return@post
                        applyResolvedBackground(next)
                    }
                }
            }
        }
    }

    private fun applyResolvedBackground(next: Color) {
        if (!isMateriallyDifferent(appBackgroundColor, next)) {
            theme = resolveTheme()
            return
        }
        appBackgroundColor = next
        theme = resolveTheme()
    }

    private fun resolveTheme(): NavbarThemeSnapshot =
        NavbarThemeResolver.resolve(
            preferences = preferences,
            backgroundMode = navbarPrefs.backgroundMode,
            appBackgroundColor = appBackgroundColor,
        )

    fun toggleVisibility() {
        if (chromeForcedHidden) return
        userVisible = !userVisible
    }

    fun hide() {
        if (chromeForcedHidden) return
        userVisible = false
    }

    fun show() {
        userVisible = true
    }

    fun registerReceivers(context: Context) {
        val filter = IntentFilter(MetroBroadcasts.ACTION_THEME_CHANGED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(themeReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            context.registerReceiver(themeReceiver, filter)
        }
    }

    fun unregisterReceivers(context: Context) {
        runCatching { context.unregisterReceiver(themeReceiver) }
    }

    companion object {
        /** Ignore windows probes right after Start / WINDOW_STATE so outgoing apps do not stick. */
        private const val PROBE_QUIET_AFTER_AUTHORITATIVE_MS = 450L

        fun isMateriallyDifferent(previous: Color?, next: Color): Boolean {
            if (previous == null) return true
            val dr = kotlin.math.abs(previous.red - next.red)
            val dg = kotlin.math.abs(previous.green - next.green)
            val db = kotlin.math.abs(previous.blue - next.blue)
            return dr + dg + db > 0.04f
        }
    }
}
