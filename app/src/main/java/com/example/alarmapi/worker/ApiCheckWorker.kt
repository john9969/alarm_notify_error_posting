package com.example.alarmapi.worker


import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.alarmapi.data.ApiClient
import com.example.alarmapi.notify.NotificationHelper
import android.util.Log

class ApiCheckWorker(appContext: Context, params: WorkerParameters): CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        return try {
            val r = ApiClient.checkAlert()
            if (r.alert) {
                NotificationHelper.showAlarm(applicationContext, r.title, r.message)
            }
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}