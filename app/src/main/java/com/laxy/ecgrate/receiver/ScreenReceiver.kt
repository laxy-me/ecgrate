package com.laxy.ecgrate.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.laxy.ecgrate.RateManager

class ScreenReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_SCREEN_OFF -> RateManager.screenOn = false
            Intent.ACTION_SCREEN_ON -> RateManager.screenOn = true
        }
    }
}
