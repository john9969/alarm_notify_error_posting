package com.example.alarmapi.notify

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.alarmapi.R
import com.example.alarmapi.ui.FullScreenAlarmActivity

object NotificationHelper {

    // Kênh mặc định (nếu cần thông báo thường)
    const val CHANNEL_ID = "alarm_channel"

    // Kênh báo thức (có âm thanh hệ thống + full screen)
    const val CHANNEL_ID_ALARM = "alarm_channel_v2"

    private const val CHANNEL_NAME = "Alarm & Reminders"
    private const val CHANNEL_DESC = "Kênh thông báo cho cảnh báo thử"

    /** Tạo kênh thường */
    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                val ch = NotificationChannel(
                    CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = CHANNEL_DESC
                    enableLights(true)
                    lightColor = Color.RED
                    enableVibration(true)
                    setShowBadge(true)
                    lockscreenVisibility = Notification.VISIBILITY_PRIVATE
                }
                nm.createNotificationChannel(ch)
            }
        }
    }

    /** Tạo kênh báo thức (âm thanh báo thức + rung mạnh + hiện public trên lockscreen) */
    fun createAlarmChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.getNotificationChannel(CHANNEL_ID_ALARM) == null) {
                val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                val attrs = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()

                val ch = NotificationChannel(
                    CHANNEL_ID_ALARM, "Alarms", NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Kênh báo thức"
                    setSound(alarmSound, attrs)
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 800, 600, 800)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                }
                nm.createNotificationChannel(ch)
            }
        }
    }

    /** Kiểm tra quyền POST_NOTIFICATIONS an toàn */
    private fun hasPostNotif(ctx: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= 33) {
            ContextCompat.checkSelfPermission(
                ctx, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(ctx).areNotificationsEnabled()
        }
    }

    private fun safeNotify(ctx: Context, id: Int, n: Notification) {
        try {
            if (!hasPostNotif(ctx)) {
                Log.w("NotificationHelper", "Permission not granted → skip notify($id)")
                return
            }
            NotificationManagerCompat.from(ctx).notify(id, n)
        } catch (se: SecurityException) {
            Log.e("NotificationHelper", "SecurityException when notify", se)
        }
    }

    /** Gửi thông báo báo thức: có âm thanh + mở Activity toàn màn hình khi màn hình tắt */
    fun showAlarm(context: Context, title: String?, message: String?, id: Int = 1002) {
        // Đảm bảo đã có kênh báo thức
        createAlarmChannel(context)

        val finalTitle = title ?: "Báo thức"
        val finalMessage = message ?: "Đến giờ rồi!"

        // Intent full screen
        val fullScreenIntent = Intent(context, FullScreenAlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("title", finalTitle)
            putExtra("message", finalMessage)
        }
        val fullScreenPI = PendingIntent.getActivity(
            context, 0, fullScreenIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID_ALARM)
            .setSmallIcon(R.drawable.ic_notifications_black_24dp) // <-- tạo vector asset tên này
            .setContentTitle(finalTitle)
            .setContentText(finalMessage)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX) // cho < O
            .setOngoing(true) // giữ đến khi user tắt
            .setFullScreenIntent(fullScreenPI, true)
            .setAutoCancel(false)

        // Với API < 26, set sound trực tiếp trên builder
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            builder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
            builder.setVibrate(longArrayOf(0, 800, 600, 800))
        }

        safeNotify(context, id, builder.build())
        Log.d("NotificationHelper", "showAlarm(): sent full-screen notification, id=$id")
    }
}
