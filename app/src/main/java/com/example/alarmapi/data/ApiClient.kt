package com.example.alarmapi.data
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import android.util.Log

object ApiClient {
    private const val API_URL =
        "https://donuoctrieuduong.xyz/water_level_api/test/update_water.php"

    private const val KEY = "tvtd74194" // key cần tìm
    private const val TAG = "ApiClient"
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    data class ApiResult(
        val alert: Boolean,
        val title: String?,
        val message: String?,
        val errorType: String? = null
    )

    private fun parseIsoDate(timeStr: String): Date? {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)
            sdf.parse(timeStr)
        } catch (e: Exception) {
            null
        }
    }

    fun checkAlert(): ApiResult {
        Log.d("ApiClient","CHECKING")
        val req = Request.Builder().url(API_URL).build()
        return try {
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    return ApiResult(false, "Server Error", "HTTP ${resp.code}", "HTTP_ERROR")
                }

                val body = resp.body?.string()
                    ?: return ApiResult(false, "Server Error", "Không có dữ liệu trả về", "EMPTY_BODY")

                println("==== Raw server data ====")
                println(body)

                Log.d(TAG, "==== Raw server data ====")
                Log.d(TAG, body)
                val lines = body.split("\n").filter { it.isNotBlank() }
                for (line in lines.asReversed()) {
                    try {
                        val parts = line.split(" ", limit = 2)
                        if (parts.size < 2) continue

                        val timeStr = parts[0]
                        val jsonStr = parts[1]

                        val json = JSONObject(jsonStr)
                        val text = json.optString("text", "")

                        if (text.contains(KEY, ignoreCase = true)) {
                            val date = parseIsoDate(timeStr)
                            if (date != null) {
                                val deltaMinutes =
                                    (Date().time - date.time) / 60000 // ms -> phút

                                return if (deltaMinutes > 30) {
                                    ApiResult(true, "Cảnh báo", "Không có dữ liệu điện báo", null)
                                } else {
                                    println("[có dữ liệu] $text")
                                    Log.d("ApiClient", "Phát hiện dữ liệu")
                                    ApiResult(false, "Có dữ liệu", text, null)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        println("Parse error at line: $line -> ${e.message}")
                    }
                }

                ApiResult(true, "Cảnh báo", "Không tìm thấy dữ liệu với key $KEY", "NO_KEY")
            }
        } catch (e: UnknownHostException) {
            ApiResult(false, "Network Error", "Không có mạng hoặc DNS lỗi", "NO_NETWORK")
        } catch (e: SocketTimeoutException) {
            ApiResult(true, "Timeout", "Kết nối hoặc đọc dữ liệu quá lâu", "TIMEOUT")
        } catch (e: ConnectException) {
            ApiResult(true, "Connection Error", "Không kết nối được tới server", "CONNECTION_ERROR")
        } catch (e: IOException) {
            ApiResult(true, "IO Error", e.localizedMessage, "IO_ERROR")
        }
    }
}
