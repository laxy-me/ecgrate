package com.laxy.ecgrate.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.laxy.ecgrate.global.RateTask

class ScreenReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_SCREEN_OFF -> RateTask.screenOn = false
            Intent.ACTION_SCREEN_ON -> RateTask.screenOn = true
        }
    }
}
