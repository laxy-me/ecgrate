package com.laxy.ecgrate.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.laxy.ecgrate.global.RateTask

/**
 *
 * @author laxy
 * @date 2024/3/30
 */
class ScreenReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent) {
        if (intent.action == Intent.ACTION_SCREEN_OFF) {
            Log.e("wtf","aaa")
            RateTask.screenOn = false
        } else if (intent.action == Intent.ACTION_SCREEN_ON) {
            Log.e("wtf","bbb")
            RateTask.screenOn = true
        }
    }
}