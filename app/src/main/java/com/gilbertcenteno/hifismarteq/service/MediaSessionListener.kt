package com.gilbertcenteno.hifismarteq.service

import android.service.notification.NotificationListenerService
import android.content.Intent
import android.provider.Settings

class MediaSessionListener : NotificationListenerService() {
    override fun onListenerConnected() {
        super.onListenerConnected()
        getSharedPreferences("listener", MODE_PRIVATE).edit().putBoolean("connected", true).apply()
    }

    override fun onListenerDisconnected() {
        getSharedPreferences("listener", MODE_PRIVATE).edit().putBoolean("connected", false).apply()
        super.onListenerDisconnected()
    }
}
