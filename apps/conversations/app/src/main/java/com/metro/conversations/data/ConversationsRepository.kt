package com.metro.conversations.data

import android.app.ActivityOptions
import android.app.Notification
import android.app.PendingIntent
import android.app.Person
import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.graphics.drawable.toBitmap
import com.metro.conversations.ConversationsListenerService
import com.metro.system.MetroAppBranding

/**
 * Reads active shade notifications that expose free-form reply (plus Gmail) and maps
 * them into [ReplyableConversation] groups for the hub UI.
 */
class ConversationsRepository(
    private val appContext: Context,
) {
    private val labelCache = mutableMapOf<String, String>()

    fun hasNotificationAccess(): Boolean =
        ConversationsListenerService.isNotificationAccessEnabled(appContext)

    fun notificationAccessSettingsIntent(): Intent =
        ConversationsListenerService.notificationAccessSettingsIntent()

    fun loadGroups(): List<AppConversationGroup> {
        val active = ConversationsListenerService.activeNotificationsOrEmpty()
        val conversations = active.mapNotNull { sbn ->
            val snapshot = snapshotFrom(sbn) ?: return@mapNotNull null
            if (!ConversationsLogic.isReplyableCandidate(snapshot)) return@mapNotNull null
            val substitute = sbn.notification.extras
                .getCharSequence("android.substName")
                ?.toString()
            ReplyableConversation(
                key = snapshot.key,
                packageName = snapshot.packageName,
                appLabel = appLabel(snapshot.packageName, substitute),
                title = snapshot.title,
                preview = snapshot.preview,
                postTimeMs = snapshot.postTimeMs,
                messages = snapshot.messages,
                canReply = snapshot.hasReplyAction,
                senderPhoto = senderPhoto(sbn.packageName, sbn.notification),
            )
        }
        return ConversationsLogic.groupByApp(conversations)
    }

    fun sendReply(key: String, text: String): Boolean {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return false
        val sbn = ConversationsListenerService.findActive(key) ?: return false
        val action = replyAction(sbn.notification) ?: return false
        val remoteInputs = action.remoteInputs ?: return false
        val freeForm = remoteInputs.firstOrNull { it.allowFreeFormInput } ?: return false
        val results = Bundle().apply {
            putCharSequence(freeForm.resultKey, trimmed)
        }
        val fillIn = Intent()
        RemoteInput.addResultsToIntent(arrayOf(freeForm), fillIn, results)
        return try {
            action.actionIntent.send(appContext, 0, fillIn)
            true
        } catch (error: PendingIntent.CanceledException) {
            Log.w(TAG, "Reply PendingIntent canceled for $key", error)
            false
        } catch (error: Exception) {
            Log.w(TAG, "Failed to send reply for $key", error)
            false
        }
    }

    fun openConversation(key: String): Boolean {
        val sbn = ConversationsListenerService.findActive(key) ?: return false
        val content = sbn.notification.contentIntent
        if (content != null) {
            val sent = runCatching {
                sendContentIntent(content)
                true
            }.onFailure { error ->
                Log.w(TAG, "contentIntent failed for $key", error)
            }.getOrDefault(false)
            if (sent) return true
        }
        return runCatching {
            launchPackage(sbn.packageName)
        }.onFailure { error ->
            Log.w(TAG, "Failed to open conversation $key", error)
        }.getOrDefault(false)
    }

    /** Remove one conversation notification from the shade. */
    fun dismissConversation(key: String): Boolean =
        ConversationsListenerService.dismissNotification(key)

    /**
     * Remove every replyable conversation notification Conversations is tracking.
     * Leaves unrelated shade posts alone.
     */
    fun clearAllConversations(): Int {
        val keys = ConversationsLogic.conversationsFor(loadGroups(), packageName = null)
            .map { it.key }
        var cleared = 0
        for (key in keys) {
            if (ConversationsListenerService.dismissNotification(key)) cleared++
        }
        return cleared
    }

    /**
     * Fire a notification content PendingIntent. On API 34+ allow background activity
     * starts (same pattern as Action Center toast tap).
     */
    private fun sendContentIntent(intent: PendingIntent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val options = ActivityOptions.makeBasic().apply {
                pendingIntentBackgroundActivityStartMode =
                    ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
            }
            intent.send(appContext, 0, null, null, null, null, options.toBundle())
        } else {
            intent.send()
        }
    }

    private fun launchPackage(packageName: String): Boolean {
        val launch = appContext.packageManager.getLaunchIntentForPackage(packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        } ?: return false
        appContext.startActivity(launch)
        return true
    }

    private fun snapshotFrom(sbn: StatusBarNotification): ReplyableNotificationSnapshot? {
        val notification = sbn.notification ?: return null
        val hasReply = replyAction(notification) != null
        val extras = notification.extras
        val messages = extractMessages(notification)
        val copy = ConversationsLogic.resolveNotificationCopy(
            NotificationCopyInput(
                packageName = sbn.packageName,
                title = extras.charSeq(Notification.EXTRA_TITLE)
                    ?: extras.charSeq(Notification.EXTRA_TITLE_BIG),
                text = extras.charSeq(Notification.EXTRA_TEXT),
                bigText = extras.charSeq(Notification.EXTRA_BIG_TEXT),
                conversationTitle = extras.charSeq(Notification.EXTRA_CONVERSATION_TITLE),
                subText = extras.charSeq(Notification.EXTRA_SUB_TEXT),
                infoText = extras.charSeq(Notification.EXTRA_INFO_TEXT),
                textLines = extras.textLines(),
                messages = messages,
            ),
        )
        return ReplyableNotificationSnapshot(
            key = sbn.key,
            packageName = sbn.packageName,
            title = copy.title,
            preview = copy.preview,
            postTimeMs = sbn.postTime,
            messages = copy.messages,
            isGroupSummary = notification.flags and Notification.FLAG_GROUP_SUMMARY != 0,
            hasReplyAction = hasReply,
        )
    }

    private fun replyAction(notification: Notification): Notification.Action? {
        val actions = notification.actions ?: return null
        return actions.firstOrNull { action ->
            val inputs = action.remoteInputs ?: return@firstOrNull false
            inputs.any { it.allowFreeFormInput }
        }
    }

    private fun extractMessages(notification: Notification): List<ConversationMessage> {
        val extras = notification.extras
        @Suppress("DEPRECATION")
        val raw = extras.getParcelableArray(Notification.EXTRA_MESSAGES)
        val fromStyle = if (raw == null) {
            emptyList()
        } else {
            raw.mapIndexedNotNull { index, item ->
                val bundle = item as? Bundle ?: return@mapIndexedNotNull null
                val text = bundle.getCharSequence("text")?.toString()?.trim().orEmpty()
                val image = messageImageBitmap(bundle)
                if (text.isEmpty() && image == null) return@mapIndexedNotNull null
                if (text.isNotEmpty() && ConversationsLogic.isCountSummary(text) && image == null) {
                    return@mapIndexedNotNull null
                }
                val sender = bundle.getCharSequence("sender")?.toString()?.trim()?.takeIf { it.isNotEmpty() }
                    ?: personName(bundle)
                val timestamp = if (bundle.containsKey("time")) bundle.getLong("time") else 0L
                ConversationMessage(
                    id = "$index:$timestamp:${text.ifEmpty { "img" }}",
                    sender = sender,
                    text = text,
                    timestampMs = timestamp,
                    fromSelf = ConversationsLogic.isSelfSender(sender),
                    image = image,
                )
            }
        }
        return attachNotificationPicture(fromStyle, notification)
    }

    /**
     * When no MessagingStyle message carries an image, attach BigPicture / picture icon
     * to the latest bubble — matching what the shade shows expanded.
     */
    private fun attachNotificationPicture(
        messages: List<ConversationMessage>,
        notification: Notification,
    ): List<ConversationMessage> {
        if (messages.any { it.image != null }) return messages
        val picture = notificationPictureBitmap(notification) ?: return messages
        if (messages.isEmpty()) {
            return listOf(
                ConversationMessage(
                    id = "picture",
                    sender = null,
                    text = "",
                    timestampMs = 0L,
                    fromSelf = false,
                    image = picture,
                ),
            )
        }
        val last = messages.last()
        return messages.dropLast(1) + last.copy(image = picture)
    }

    /** MessagingStyle.Message data URI (mime type image/… + content uri). */
    private fun messageImageBitmap(bundle: Bundle): Bitmap? {
        val mime = bundle.getString("type")?.lowercase() ?: return null
        if (!mime.startsWith("image/")) return null
        val uri = messageDataUri(bundle) ?: return null
        return loadBitmapFromUri(uri)
    }

    private fun messageDataUri(bundle: Bundle): android.net.Uri? {
        return if (Build.VERSION.SDK_INT >= 33) {
            bundle.getParcelable("uri", android.net.Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            bundle.getParcelable("uri") as? android.net.Uri
        }
    }

    private fun notificationPictureBitmap(notification: Notification): Bitmap? {
        val extras = notification.extras
        @Suppress("DEPRECATION")
        val picture = if (Build.VERSION.SDK_INT >= 33) {
            extras.getParcelable(Notification.EXTRA_PICTURE, Bitmap::class.java)
        } else {
            extras.getParcelable(Notification.EXTRA_PICTURE) as? Bitmap
        }
        if (picture != null) return scaleMessageBitmap(picture)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val icon = extras.getParcelable(Notification.EXTRA_PICTURE_ICON, Icon::class.java)
            iconToMessageBitmap(icon)?.let { return it }
        }
        return null
    }

    private fun loadBitmapFromUri(uri: android.net.Uri): Bitmap? {
        return runCatching {
            appContext.contentResolver.openInputStream(uri)?.use { stream ->
                android.graphics.BitmapFactory.decodeStream(stream)
            }
        }.getOrNull()?.let { scaleMessageBitmap(it) }
    }

    private fun iconToMessageBitmap(icon: Icon?): Bitmap? {
        if (icon == null) return null
        return runCatching {
            val drawable = icon.loadDrawable(appContext) ?: return null
            val w = drawable.intrinsicWidth.coerceAtLeast(1)
            val h = drawable.intrinsicHeight.coerceAtLeast(1)
            scaleMessageBitmap(drawable.toBitmap(w, h))
        }.getOrNull()
    }

    /** Cap message photos so bubble decode stays light. */
    private fun scaleMessageBitmap(source: Bitmap): Bitmap {
        val maxEdge = (appContext.resources.displayMetrics.density * 320f).toInt().coerceAtLeast(240)
        val longest = maxOf(source.width, source.height)
        if (longest <= maxEdge) return source
        val scale = maxEdge.toFloat() / longest.toFloat()
        val w = (source.width * scale).toInt().coerceAtLeast(1)
        val h = (source.height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(source, w, h, true)
    }

    /**
     * Prefer the latest incoming MessagingStyle [Person] icon, then the conversation
     * MessagingStyle user, then the notification large icon / EXTRA_LARGE_ICON.
     */
    private fun senderPhoto(packageName: String, notification: Notification): Bitmap? {
        val sizePx = (appContext.resources.displayMetrics.density * 64f).toInt().coerceAtLeast(48)
        val extras = notification.extras

        @Suppress("DEPRECATION")
        val messageBundles = extras.getParcelableArray(Notification.EXTRA_MESSAGES)
            ?.mapNotNull { it as? Bundle }
            .orEmpty()
        for (bundle in messageBundles.asReversed()) {
            val sender = bundle.getCharSequence("sender")?.toString()
                ?: personName(bundle)
            if (ConversationsLogic.isSelfSender(sender)) continue
            iconToBitmap(personIcon(bundle), sizePx)?.let { return it }
        }

        iconToBitmap(messagingPersonIcon(extras), sizePx)?.let { return it }
        iconToBitmap(notification.getLargeIcon(), sizePx)?.let { return it }

        @Suppress("DEPRECATION")
        val legacyLarge = extras.getParcelable<Bitmap>(Notification.EXTRA_LARGE_ICON)
            ?: extras.getParcelable(Notification.EXTRA_LARGE_ICON_BIG)
        if (legacyLarge != null) {
            return scaleBitmap(legacyLarge, sizePx)
        }

        // Last resort: app glyph so the row still has a leading face.
        return MetroAppBranding.loadAppIcon(appContext, packageName)?.let { drawable ->
            drawableToBitmap(drawable, sizePx)
        }
    }

    private fun messagingPersonIcon(extras: Bundle): Icon? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return null
        val person = if (Build.VERSION.SDK_INT >= 33) {
            extras.getParcelable(Notification.EXTRA_MESSAGING_PERSON, Person::class.java)
        } else {
            @Suppress("DEPRECATION")
            extras.getParcelable(Notification.EXTRA_MESSAGING_PERSON) as? Person
        }
        return person?.icon
    }

    private fun personIcon(bundle: Bundle): Icon? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return null
        val person = if (Build.VERSION.SDK_INT >= 33) {
            bundle.getParcelable("sender_person", Person::class.java)
        } else {
            @Suppress("DEPRECATION")
            bundle.getParcelable("sender_person") as? Person
        }
        return person?.icon
    }

    private fun personName(bundle: Bundle): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return null
        val person = if (Build.VERSION.SDK_INT >= 33) {
            bundle.getParcelable("sender_person", Person::class.java)
        } else {
            @Suppress("DEPRECATION")
            bundle.getParcelable("sender_person") as? Person
        }
        return person?.name?.toString()?.trim()?.takeIf { it.isNotEmpty() }
    }

    private fun iconToBitmap(icon: Icon?, sizePx: Int): Bitmap? {
        if (icon == null) return null
        return try {
            val drawable = icon.loadDrawable(appContext) ?: return null
            // Adaptive / system icons often paint with a circular mask — unwrap to a
            // square Metro face before rasterizing.
            val square = MetroAppBranding.metroGlyphDrawable(drawable) ?: drawable
            drawableToBitmap(square, sizePx)
        } catch (_: Exception) {
            null
        }
    }

    private fun drawableToBitmap(drawable: Drawable, sizePx: Int): Bitmap {
        val unwrapped = MetroAppBranding.metroGlyphDrawable(drawable) ?: drawable
        if (unwrapped is BitmapDrawable && unwrapped.bitmap != null) {
            return toSquareFaceBitmap(unwrapped.bitmap, sizePx)
        }
        val raw = unwrapped.toBitmap(sizePx, sizePx)
        return toSquareFaceBitmap(raw, sizePx)
    }

    private fun scaleBitmap(source: Bitmap, sizePx: Int): Bitmap =
        toSquareFaceBitmap(source, sizePx)

    /**
     * Messaging apps often ship circular avatars (transparent corners). Metro list
     * faces are square — crop to the inscribed square so the tile reads as a hard
     * rectangle, not a circle on a square field.
     */
    private fun toSquareFaceBitmap(source: Bitmap, sizePx: Int): Bitmap {
        val prepared = if (hasTransparentCorners(source)) {
            val edge = minOf(source.width, source.height)
            // Inscribed square in a circle that fills the bitmap (side = diameter / √2).
            val crop = (edge * 0.72f).toInt().coerceAtLeast(1).coerceAtMost(edge)
            val left = ((source.width - crop) / 2).coerceAtLeast(0)
            val top = ((source.height - crop) / 2).coerceAtLeast(0)
            Bitmap.createBitmap(source, left, top, crop, crop)
        } else {
            source
        }
        if (prepared.width == sizePx && prepared.height == sizePx) {
            return flattenOntoOpaqueSquare(prepared, sizePx)
        }
        val scaled = Bitmap.createScaledBitmap(prepared, sizePx, sizePx, true)
        return flattenOntoOpaqueSquare(scaled, sizePx)
    }

    /** Drop residual transparent padding so Compose does not show a round silhouette. */
    private fun flattenOntoOpaqueSquare(source: Bitmap, sizePx: Int): Bitmap {
        val out = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(out)
        canvas.drawColor(android.graphics.Color.BLACK)
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            isFilterBitmap = true
        }
        canvas.drawBitmap(source, null, android.graphics.Rect(0, 0, sizePx, sizePx), paint)
        return out
    }

    private fun hasTransparentCorners(bitmap: Bitmap): Boolean {
        val w = bitmap.width
        val h = bitmap.height
        if (w < 4 || h < 4) return false
        val corners = intArrayOf(
            bitmap.getPixel(0, 0),
            bitmap.getPixel(w - 1, 0),
            bitmap.getPixel(0, h - 1),
            bitmap.getPixel(w - 1, h - 1),
        )
        return corners.count { pixel -> android.graphics.Color.alpha(pixel) < 40 } >= 3
    }

    private fun appLabel(packageName: String, substituteAppName: String?): String {
        labelCache[packageName]?.let { cached ->
            if (!ConversationsLogic.looksLikePackageName(cached)) return cached
        }
        val pmLabel = packageManagerLabel(packageName)
        val resolved = ConversationsLogic.resolveAppLabel(
            packageName = packageName,
            packageManagerLabel = pmLabel,
            substituteAppName = substituteAppName,
        )
        labelCache[packageName] = resolved
        return resolved
    }

    private fun packageManagerLabel(packageName: String): String? {
        val pm = appContext.packageManager
        return try {
            val info = if (Build.VERSION.SDK_INT >= 33) {
                pm.getApplicationInfo(
                    packageName,
                    PackageManager.ApplicationInfoFlags.of(0),
                )
            } else {
                @Suppress("DEPRECATION")
                pm.getApplicationInfo(packageName, 0)
            }
            pm.getApplicationLabel(info)?.toString()
        } catch (_: PackageManager.NameNotFoundException) {
            tryLoadLabelViaMatchAll(pm, packageName)
        }
    }

    private fun tryLoadLabelViaMatchAll(pm: PackageManager, packageName: String): String? {
        return try {
            @Suppress("DEPRECATION")
            val info = pm.getApplicationInfo(
                packageName,
                PackageManager.MATCH_ALL or PackageManager.MATCH_UNINSTALLED_PACKAGES,
            )
            pm.getApplicationLabel(info)?.toString()
        } catch (_: Exception) {
            null
        }
    }

    private fun Bundle.charSeq(key: String): String? =
        getCharSequence(key)?.toString()?.trim()?.takeIf { it.isNotEmpty() }

    private fun Bundle.textLines(): List<String> {
        @Suppress("DEPRECATION")
        val raw = getCharSequenceArray(Notification.EXTRA_TEXT_LINES) ?: return emptyList()
        return raw.mapNotNull { it?.toString()?.trim()?.takeIf { line -> line.isNotEmpty() } }
    }

    companion object {
        private const val TAG = "ConversationsRepo"
    }
}
