package com.example.alarmapi.alarm


import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.alarmapi.worker.ApiCheckWorker


class AlarmReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val work = OneTimeWorkRequestBuilder<ApiCheckWorker>().build()
        WorkManager.getInstance(context).enqueue(work)
    }
}