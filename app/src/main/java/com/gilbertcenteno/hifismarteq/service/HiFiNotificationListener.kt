package com.gilbertcenteno.hifismarteq.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class HiFiNotificationListener : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        // Mantiene activo el listener de eventos multimedia
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
    }
}
