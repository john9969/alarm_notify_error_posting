package com.example.alarmapi.ui

import android.util.Log
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TimePicker
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.alarmapi.R
import com.example.alarmapi.notify.NotificationHelper
import com.example.alarmapi.util.Scheduler
import com.example.alarmapi.worker.ApiCheckWorker
import java.util.concurrent.TimeUnit


class MainActivity : ComponentActivity() {
    private val requestNotifPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NotificationHelper.createChannel(this) // thêm dòng này
        setContentView(R.layout.activity_main)

        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestNotifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }


        val tp = findViewById<TimePicker>(R.id.timePicker)
        tp.setIs24HourView(true)


        findViewById<Button>(R.id.btnScheduleDaily).setOnClickListener {
            val h = if (Build.VERSION.SDK_INT >= 23) tp.hour else tp.currentHour
            val m = if (Build.VERSION.SDK_INT >= 23) tp.minute else tp.currentMinute
            Scheduler.scheduleDaily(this, h, m)
        }


        findViewById<Button>(R.id.btnStartBackground).setOnClickListener {
// WorkManager: 15 phút/lần (tối thiểu). Nếu cần nhanh hơn → dùng server push hoặc Foreground service.
            val work = PeriodicWorkRequestBuilder<ApiCheckWorker>(15, TimeUnit.MINUTES).build()
            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "api_check_periodic", ExistingPeriodicWorkPolicy.UPDATE, work
            )
        }


        findViewById<Button>(R.id.btnTestAlarm).setOnClickListener {
            Thread.sleep(3000) // chặn thread hiện tại 3 giây
            NotificationHelper.showAlarm(this, "Test Cảnh báo", "Đây là thông báo thử!")
        }
    }
}