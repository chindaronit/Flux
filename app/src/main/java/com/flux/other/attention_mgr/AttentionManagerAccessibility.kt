package com.flux.other.attention_mgr

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class MyAccessibilityService : AccessibilityService() {
    companion object {
        const val TAG = "FluxAccessibilityService"
        const val INTENT_ACTION_REFRESH = "com.flux.attention_manager.refresh"

        fun sendRefreshRequest(context: Context, action: String) {
            val intent = Intent(action)
            context.sendBroadcast(intent)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(refreshReceiver)
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "refreshReceiver not registered", e)
        }
    }

    private var lastPackage = ""
    private var blockedAppsList = HashSet<String>()

    override fun onInterrupt() {}

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onServiceConnected() {
        super.onServiceConnected()
        createBlockedAppsList()
        Log.d(TAG, "Service started")

        val filter = IntentFilter().apply {
            addAction(INTENT_ACTION_REFRESH)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(refreshReceiver, filter, RECEIVER_EXPORTED)
        } else {
            registerReceiver(refreshReceiver, filter)
        }
    }

    private val refreshReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == INTENT_ACTION_REFRESH) {
                Log.d(TAG, "Received refresh intent")
                createBlockedAppsList()
            }
        }
    }

    private fun createBlockedAppsList() {
        blockedAppsList.clear()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val packageName = event?.packageName.toString()
        if (lastPackage == packageName || packageName == getPackageName()) return

        lastPackage = packageName
        Log.d(TAG, "Switched to app $packageName")
    }
}

//class AttentionManagerMonitorService : Service() {
//
//    private var serviceLooper: Looper? = null
//    private lateinit var serviceHandler: ServiceHandler
//    private lateinit var serviceThread: HandlerThread
//    private val MSG_PRINT_CONSOLE = 1
//    private var isRunning: Boolean = false;
//
//    private inner class ServiceHandler(looper: Looper) : Handler(looper) {
//        override fun handleMessage(msg: Message) {
//            when (msg.what) {
//                MSG_PRINT_CONSOLE -> {
//                    Log.d("AttentionManagerFlux", "Service is looping and printing...")
//
//                    try {
//                        Thread.sleep(2000)
//                    } catch (e: InterruptedException) {
//                        Thread.currentThread().interrupt()
//                        Log.w("AttentionManagerFlux", "Loop interrupted", e)
//                        return
//                    }
//
//                    if (!serviceThread.isInterrupted && isRunning) {
//                        sendEmptyMessage(MSG_PRINT_CONSOLE)
//                    }
//                }
//            }
//        }
//    }
//
//    override fun onCreate() {
//        super.onCreate()
//        serviceThread =
//            HandlerThread(
//                "AttentionManagerServiceThread",
//                Process.THREAD_PRIORITY_BACKGROUND
//            ).apply {
//                start()
//
//                serviceLooper = looper
//                serviceHandler = ServiceHandler(looper)
//            }
//    }
//
//    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
//    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
//        try {
//            val notificationId = Notifications.getUniqueRequestCode("", startId.toLong()).toInt()
//
//            val notification = NotificationCompat.Builder(
//                applicationContext,
//                Notifications.ATTENTIONSERVICE_CHANNEL_ID
//            )
//                .setSmallIcon(R.mipmap.ic_launcher_foreground)
//                .setContentTitle("title")
//                .setContentText("description")
//                .setPriority(NotificationCompat.PRIORITY_LOW)
//                .setAutoCancel(true)
//                .build()
//
//            ServiceCompat.startForeground(
//                this,
//                notificationId,
//                notification,
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
//                } else {
//                    0
//                },
//            )
//        } catch (e: Exception) {
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
//                && e is ForegroundServiceStartNotAllowedException
//            ) {
//                Log.e("AttentionManagerFlux", "Foreground service start not allowed", e)
//                stopSelf()
//            }
//            Log.w(
//                "AttentionManagerFlux",
//                "Error occurred during attention manager service start",
//                e
//            )
//        }
//
//        if (!isRunning) {
//            isRunning = true
//            Log.d("AttentionManagerFlux", "Starting the console print loop.")
//            serviceHandler.sendEmptyMessage(MSG_PRINT_CONSOLE)
//        } else {
//            Log.d("AttentionManagerFlux", "Loop already running.")
//        }
//
//        return START_STICKY
//    }
//
//    override fun onBind(p0: Intent?): IBinder? {
//        return null // we do not need binding
//    }
//
//    override fun onDestroy() {
//        super.onDestroy()
//        isRunning = false
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
//            serviceThread.quitSafely()
//        } else {
//            serviceThread.quit()
//        }
//
//        try {
//            serviceThread.join() // wait for the thread to finish
//        } catch (e: InterruptedException) {
//            Log.w("AttentionManagerFlux", "Failed to join service thread", e)
//        }
//        Log.d("AttentionManagerFlux", "Service thread stopped.")
//    }
//}
//
//fun startAttentionManager(context: Context) {
//    val serviceIntent = Intent(context, AttentionManagerMonitorService::class.java)
//
//    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//        context.startForegroundService(serviceIntent)
//    } else {
//        context.startService(serviceIntent)
//    }
//}