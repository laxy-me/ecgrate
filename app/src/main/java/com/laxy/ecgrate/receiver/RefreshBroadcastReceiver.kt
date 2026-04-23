package com.laxy.ecgrate.receiver

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.laxy.ecgrate.global.RateTask

class RefreshBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent) {
        context ?: return
        RateTask.initSp(context)
        RateTask.flush()
        // API >= S 的精确闹钟是一次性的，需要手动续期
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (am.canScheduleExactAlarms()) {
                RateTask.scheduleNextAlarm(context)
            }
        }
    }
}
