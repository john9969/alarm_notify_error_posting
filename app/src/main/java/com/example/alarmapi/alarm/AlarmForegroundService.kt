package com.example.alarmapi.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import com.example.alarmapi.R
import com.example.alarmapi.data.ApiClient
import com.example.alarmapi.notify.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class AlarmForegroundService : LifecycleService() {

    companion object {
        private const val TAG = "Alarm_Check"
        const val CHANNEL_ID = "alarm_foreground"
        const val NOTIF_ID = 2001

        // Truyền giờ/phút hẹn qua Intent
        fun start(context: Context, hour: Int, minute: Int) {
            val i = Intent(context, AlarmForegroundService::class.java).apply {
                putExtra("hour", hour)
                putExtra("minute", minute)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Log.d(TAG, "startForegroundService()")
                context.startForegroundService(i)
            } else {
                Log.d(TAG, "startService()")
                context.startService(i)
            }
        }

        fun stop(context: Context) {
            Log.d(TAG, "stopService()")
            val i = Intent(context, AlarmForegroundService::class.java)
            context.stopService(i)
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var loopJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate()")
        createChannel()
        startForeground(NOTIF_ID, buildNotification("Đang chạy nền", "Đang theo dõi các ốp điện báo"))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // GỌI SUPER để LifecycleService xử lý nội bộ
        Log.d(TAG, "onStartCommand() flags=$flags startId=$startId intent=$intent")
        super.onStartCommand(intent, flags, startId)

        val hour   = intent?.getIntExtra("hour", 7) ?: 7
        val minute = intent?.getIntExtra("minute", 0) ?: 0
        Log.d(TAG, "onStartCommand() target = %02d:%02d".format(hour, minute))

        // Tránh tạo nhiều vòng lặp nếu start nhiều lần
        if (loopJob?.isActive == true) {
            Log.d(TAG, "Loop is already running, skip new start")
            return START_STICKY
        }

        loopJob = serviceScope.launch {
            var lastTriggeredMinute: Int? = null
            while (isActive) {
                val now = Calendar.getInstance()
                val h = now.get(Calendar.HOUR_OF_DAY)
                val m = now.get(Calendar.MINUTE)
                Log.d(TAG, "tick %02d:%02d (target %02d:%02d)".format(h, m, hour, minute))

                // Đến đúng giờ-phút, chỉ chạy 1 lần trong phút đó
                val triggerHours = setOf(1, 7, 13, 19)

                if (h in triggerHours && m in 9..13 && lastTriggeredMinute != h) {
                //if (m in 50..55 && lastTriggeredMinute != h) {
                    withContext(Dispatchers.IO) {
                        val r = ApiClient.checkAlert() // checkAlert nên là suspend; nếu không thì vẫn OK vì đang ở IO
                        if (r.alert) {
                            Log.d(TAG, "ALERT: ${r.title} - ${r.message}")
                            // Ví dụ: bắn thông báo toàn màn hình nếu cần
                            NotificationHelper.showAlarm(applicationContext, r.title, r.message)
                        } else {
                            Log.d(TAG, "Có dữ liệu")
                        }
                    }
                    lastTriggeredMinute = h
                }

                delay(45_000) // ngủ 45s
            }
        }

        // Duy trì chạy nền, nếu hệ thống kill sẽ cố tạo lại service
        return START_STICKY
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy()")
        loopJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                CHANNEL_ID,
                "Alarm Background",
                NotificationManager.IMPORTANCE_LOW
            )
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(ch)
        }
    }

    private fun buildNotification(title: String, msg: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notifications_black_24dp) // đảm bảo icon tồn tại (Vector Asset)
            .setContentTitle(title)
            .setContentText(msg)
            .setOngoing(true)
            .build()
    }
}
