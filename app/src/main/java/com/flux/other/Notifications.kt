package com.flux.other

import android.Manifest
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object Notifications {
    const val REMINDER_CHANNEL_ID =
        "notification_channel" // TODO: leave notification_channel for compatibility or rename?
    const val ATTENTIONSERVICE_CHANNEL_ID = "attention_service_channel"

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun setupNotifications(context: Context) {
        if (!isNotificationPermissionGranted(context)) {
            requestNotificationPermission(context as Activity)
        } // TODO: what if the user didn't agree

        createReminderNotificationChannel(context)
        createAttentionServiceNotificationChannel(context)
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun createAttentionServiceNotificationChannel(context: Context) {
        val importance = NotificationManager.IMPORTANCE_LOW
        val channel =
            NotificationChannel(ATTENTIONSERVICE_CHANNEL_ID, "Attention Manager", importance)

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun createReminderNotificationChannel(context: Context) {
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(REMINDER_CHANNEL_ID, "Reminders", importance)

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    fun getUniqueRequestCode(type: String, uuid: String): Int {
        return (type + uuid).hashCode()
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun isNotificationPermissionGranted(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun requestNotificationPermission(activity: Activity, requestCode: Int = 1001) {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
            requestCode
        )
    }

    fun openAppNotificationSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        }
        context.startActivity(intent)
    }
}
