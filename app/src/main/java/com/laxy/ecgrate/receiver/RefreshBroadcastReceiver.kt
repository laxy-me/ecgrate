package com.laxy.ecgrate.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.laxy.ecgrate.global.RateTask

/**
 *
 * @author laxy
 * @date 2024/4/1
 */
class RefreshBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent) {
        Log.e("wtf", "hahah")
        RateTask.flush()
    }
}