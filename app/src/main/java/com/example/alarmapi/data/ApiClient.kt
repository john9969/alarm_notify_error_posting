package com.example.alarmapi.data


import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject


object ApiClient {
    // TODO: thay URL của bạn
    private const val API_URL = "https://donuoctrieuduong.xyz/water_level_api/test/update_water.php"


    private val client = OkHttpClient()


    data class ApiResult(val alert: Boolean, val title: String?, val message: String?)


    fun checkAlert(): ApiResult {
        val req = Request.Builder().url(API_URL).build()
        client.newCall(req).execute().use { resp ->
            val body = resp.body?.string() ?: return ApiResult(false, null, null)
            val json = JSONObject(body)
            val alert = json.optBoolean("alert", false)
            val title = json.optString("title", "Cảnh báo")
            val message = json.optString("message", "Có tín hiệu!")
            return ApiResult(alert, title, message)
        }
    }
}