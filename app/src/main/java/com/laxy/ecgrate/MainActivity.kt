package com.laxy.ecgrate

import android.app.AlarmManager
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.laxy.ecgrate.receiver.RefreshBroadcastReceiver
import com.laxy.ecgrate.receiver.ScreenReceiver
import com.laxy.ecgrate.ui.MainScreen
import com.laxy.ecgrate.ui.EcgrateTheme
import com.laxy.ecgrate.widget.RateWidget

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    private val screenReceiver by lazy { ScreenReceiver() }
    private val refreshReceiver by lazy { RefreshBroadcastReceiver() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        RateManager.startPolling()
        setContent {
            EcgrateTheme {
                MainScreen(
                    viewModel = viewModel,
                    onRequestAlarmPermission = { requestAlarmPermission() }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(screenReceiver, IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        })
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            registerReceiver(
                refreshReceiver,
                IntentFilter(AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED)
            )
        }
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(screenReceiver)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            unregisterReceiver(refreshReceiver)
        }
    }

    override fun onStop() {
        super.onStop()
        sendBroadcast(Intent(this, RateWidget::class.java).apply {
            action = RateWidget.ACTION_REFRESH
        })
        RateManager.scheduleNextAlarm(this)
        RateManager.startPolling()
    }

    private fun requestAlarmPermission() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                startActivity(Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:$packageName")
                })
            } else {
                startActivity(Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:$packageName")
                })
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
