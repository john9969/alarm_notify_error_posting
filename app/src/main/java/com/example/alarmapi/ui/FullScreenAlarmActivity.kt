package com.example.alarmapi.ui


import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.example.alarmapi.R


class FullScreenAlarmActivity : ComponentActivity() {
    private var ringtone: Ringtone? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alarm)

        // Bật màn hình + hiện trên lockscreen cho mọi API
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        val title = intent.getStringExtra("title") ?: "Báo thức"
        val msg   = intent.getStringExtra("message") ?: "Đến giờ rồi!"
        findViewById<TextView>(R.id.txtTitle).text = title
        findViewById<TextView>(R.id.txtMessage).text = msg

        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        ringtone = RingtoneManager.getRingtone(this, uri)
        ringtone?.play()
        Log.d("AlarmActivity", "ĐÃ MỞ MÀN BÁO THỨC + PHÁT CHUÔNG")

        findViewById<Button>(R.id.btnDismiss).setOnClickListener {
            ringtone?.stop()
            finish()
        }
    }

    override fun onDestroy() {
        try { ringtone?.stop() } catch (_: Exception) {}
        super.onDestroy()
    }
}