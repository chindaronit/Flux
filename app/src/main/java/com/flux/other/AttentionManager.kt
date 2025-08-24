package com.flux.other

import android.app.ForegroundServiceStartNotAllowedException
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Process
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.flux.R

class AttentionManagerMonitorService : Service() {

    private var serviceLooper: Looper? = null
    private lateinit var serviceHandler: ServiceHandler
    private lateinit var serviceThread: HandlerThread
    private val MSG_PRINT_CONSOLE = 1
    private var isRunning: Boolean = false;

    private inner class ServiceHandler(looper: Looper) : Handler(looper) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                MSG_PRINT_CONSOLE -> {
                    Log.d("AttentionManagerFlux", "Service is looping and printing...")

                    try {
                        Thread.sleep(2000)
                    } catch (e: InterruptedException) {
                        Thread.currentThread().interrupt()
                        Log.w("AttentionManagerFlux", "Loop interrupted", e)
                        return
                    }

                    if (!serviceThread.isInterrupted && isRunning) {
                        sendEmptyMessage(MSG_PRINT_CONSOLE)
                    }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        serviceThread =
            HandlerThread(
                "AttentionManagerServiceThread",
                Process.THREAD_PRIORITY_BACKGROUND
            ).apply {
                start()

                serviceLooper = looper
                serviceHandler = ServiceHandler(looper)
            }
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            val notificationId = Notifications.getUniqueRequestCode("", startId.toLong()).toInt()

            val notification = NotificationCompat.Builder(
                applicationContext,
                Notifications.ATTENTIONSERVICE_CHANNEL_ID
            )
                .setSmallIcon(R.mipmap.ic_launcher_foreground)
                .setContentTitle("title")
                .setContentText("description")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setAutoCancel(true)
                .build()

            ServiceCompat.startForeground(
                this,
                notificationId,
                notification,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                } else {
                    0
                },
            )
        } catch (e: Exception) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                && e is ForegroundServiceStartNotAllowedException
            ) {
                Log.e("AttentionManagerFlux", "Foreground service start not allowed", e)
                stopSelf()
            }
            Log.w(
                "AttentionManagerFlux",
                "Error occurred during attention manager service start",
                e
            )
        }

        if (!isRunning) {
            isRunning = true
            Log.d("AttentionManagerFlux", "Starting the console print loop.")
            serviceHandler.sendEmptyMessage(MSG_PRINT_CONSOLE)
        } else {
            Log.d("AttentionManagerFlux", "Loop already running.")
        }

        return Service.START_STICKY
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null // we do not need binding
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            serviceThread.quitSafely()
        } else {
            serviceThread.quit()
        }

        try {
            serviceThread.join() // wait for the thread to finish
        } catch (e: InterruptedException) {
            Log.w("AttentionManagerFlux", "Failed to join service thread", e)
        }
        Log.d("AttentionManagerFlux", "Service thread stopped.")
    }
}

fun startAttentionManager(context: Context) {
    val serviceIntent = Intent(context, AttentionManagerMonitorService::class.java)

    Log.d("", "STARTING!!!!!")
    Log.d("", "STARTING!!!!!")
    Log.d("", "STARTING!!!!!")
    Log.d("", "STARTING!!!!!")
    Log.d("", "STARTING!!!!!")
    Log.d("", "STARTING!!!!!")
    Log.d("", "STARTING!!!!!")
    Log.d("", "STARTING!!!!!")
    Log.d("", "STARTING!!!!!")
    Log.d("", "STARTING!!!!!")
    Log.d("", "STARTING!!!!!")
    Log.d("", "STARTING!!!!!")
    Log.d("", "STARTING!!!!!")
    Log.d("", "STARTING!!!!!")
    Log.d("", "STARTING!!!!!")
    Log.d("", "STARTING!!!!!")
    Log.d("", "STARTING!!!!!")
    Log.d("", "STARTING!!!!!")
    Log.d("", "STARTING!!!!!")
    Log.d("", "STARTING!!!!!")
    Log.d("", "STARTING!!!!!")
    Log.d("", "STARTING!!!!!")
    Log.d("", "STARTING!!!!!")
    Log.d("", "STARTING!!!!!")
    Log.d("", "STARTING!!!!!")
    Log.d("", "STARTING!!!!!")
    Log.d("", "STARTING!!!!!")
    Log.d("", "STARTING!!!!!")
    Log.d("", "STARTING!!!!!")
    Log.d("", "STARTING!!!!!")
    Log.d("", "STARTING!!!!!")
    Log.d("", "STARTING!!!!!")
    Log.d("", "STARTING!!!!!")
    Log.d("", "STARTING!!!!!")
    Log.d("", "STARTING!!!!!")
    Log.d("", "STARTING!!!!!")
    Log.d("", "STARTING!!!!!")
    Log.d("", "STARTING!!!!!")
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(serviceIntent)
    } else {
        context.startService(serviceIntent)
    }
}