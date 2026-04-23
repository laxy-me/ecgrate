package com.laxy.ecgrate.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.laxy.ecgrate.global.RateTask

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        context ?: return
        RateTask.initSp(context)
        RateTask.scheduleNextAlarm(context)
    }
}
