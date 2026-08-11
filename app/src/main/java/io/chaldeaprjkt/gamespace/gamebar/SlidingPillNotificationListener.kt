package io.chaldeaprjkt.gamespace.gamebar

import android.app.Notification
import android.graphics.drawable.Drawable
import android.graphics.drawable.Icon
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

interface SlidingPillNotificationCallback {
    val slidingPillNotificationsEnabled: Boolean
    fun onNotificationReceived(pill: SlidingPillNotification)
    fun onNotificationRemoved(pill: SlidingPillNotification)
    fun getAppLabel(packageName: String): String
    fun loadAppIcon(packageName: String): Drawable?
    fun loadAppAccentColor(packageName: String): Int
}

data class SlidingPillNotification(
    val key: String,
    val packageName: String,
    val appLabel: String,
    val sender: String?,
    val message: String?,
    val timestamp: Long,
    val avatarIcon: Drawable?,
    val appIcon: Drawable?,
    val accentColor: Int,
)

class SlidingPillNotificationListener : NotificationListenerService() {

    private val postedNotifications = mutableMapOf<String, Long>()

    var callback: SlidingPillNotificationCallback? = null

    override fun onListenerConnected() {
        super.onListenerConnected()
        getActiveNotifications()?.forEach { sbn ->
            if (sbn.isClearable && !sbn.isOngoing) {
                val pill = extractNotification(sbn) ?: return@forEach
                postedNotifications[pill.key] = System.currentTimeMillis()
            }
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val enabled = callback?.slidingPillNotificationsEnabled ?: return
        if (!enabled || !sbn.isClearable || sbn.isOngoing) return

        val pill = extractNotification(sbn) ?: return

        postedNotifications[pill.key] = System.currentTimeMillis()
        callback?.onNotificationReceived(pill)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        val pill = extractNotification(sbn) ?: return
        postedNotifications.remove(pill.key)
        callback?.onNotificationRemoved(pill)
    }

    private fun extractNotification(sbn: StatusBarNotification): SlidingPillNotification? {
        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_TITLE_BIG)?.toString()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()

        val cb = callback ?: return null

        val appLabel = cb.getAppLabel(sbn.packageName)

        if (!title.isNullOrBlank() && appLabel.isNotBlank()) {
            if (sbn.isGroup && title.contains(appLabel, ignoreCase = true)) return null
        }

        val message = text ?: subText

        if (title.isNullOrBlank() && message.isNullOrBlank()) return null
        val displayMessage = if (!message.isNullOrBlank()) message else "\uD83D\uDCCE Attachment"

        val appIcon = cb.loadAppIcon(sbn.packageName)
        val accentColor = cb.loadAppAccentColor(sbn.packageName)

        val avatarDrawable = try {
            val people = sbn.notification.extras.getStringArrayList(Notification.EXTRA_PEOPLE)
            if (!people.isNullOrEmpty()) {
                val uri = people[0]
                if (!uri.isNullOrBlank()) {
                    val icon = Icon.createWithContentUri(uri)
                    icon?.loadDrawable(this)
                } else null
            } else null
        } catch (_: Exception) { null }

        val displayTitle = if (!title.isNullOrBlank() &&
            title.contains(appLabel, ignoreCase = true) &&
            title.length <= appLabel.length + 3) null else title

        return SlidingPillNotification(
            key = sbn.key,
            packageName = sbn.packageName,
            appLabel = appLabel,
            sender = displayTitle,
            message = displayMessage,
            timestamp = sbn.postTime,
            avatarIcon = avatarDrawable,
            appIcon = appIcon,
            accentColor = accentColor,
        )
    }
}
