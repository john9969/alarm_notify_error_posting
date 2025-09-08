package com.example.alarmapi.ui
import kotlinx.coroutines.delay

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
import androidx.lifecycle.lifecycleScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.alarmapi.R
import com.example.alarmapi.notify.NotificationHelper
import com.example.alarmapi.util.Scheduler
import com.example.alarmapi.worker.ApiCheckWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import com.example.alarmapi.alarm.AlarmForegroundService
import com.example.alarmapi.data.ApiClient
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



        findViewById<Button>(R.id.btnStartBackground).setOnClickListener {
//// WorkManager: 15 phút/lần (tối thiểu). Nếu cần nhanh hơn → dùng server push hoặc Foreground service.
//            val work = PeriodicWorkRequestBuilder<ApiCheckWorker>(15, TimeUnit.MINUTES).build()
//            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
//                "api_check_periodic", ExistingPeriodicWorkPolicy.UPDATE, work
//            )

            AlarmForegroundService.start(this, hour = 20, minute = 40)
        }


        findViewById<Button>(R.id.btnTestAlarm).setOnClickListener {
            lifecycleScope.launch {
                val r = withContext(Dispatchers.IO) { ApiClient.checkAlert() } // ĐẨY SANG IO Ở ĐIỂM GỌI

                if (r.alert) {
                    NotificationHelper.showAlarm(this@MainActivity, r.title, r.message)
                }
            }
        }
    }
}