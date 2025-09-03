package com.example.alarmapi.boot


import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.alarmapi.util.Scheduler


class BootCompletedReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
// TODO: Lưu giờ trong SharedPreferences nếu bạn cho người dùng chọn giờ → đọc lại để reschedule.
// Ví dụ mặc định 08:00 mỗi ngày:
            Scheduler.scheduleDaily(context, 8, 0)
        }
    }
}