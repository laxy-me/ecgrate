package com.laxy.ecgrate

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.SystemClock
import com.laxy.ecgrate.receiver.RefreshBroadcastReceiver
import com.laxy.ecgrate.widget.RateWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object RateManager {
    private const val SP_NAME = "rate"
    private const val KEY_CURRENCY = "currency"
    private const val KEY_INTERVAL = "interval"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var appContext: Context? = null
    private var sp: SharedPreferences? = null
    private var client: RateClient? = null
    private var pollingJob: Job? = null

    var interval: Int = 30
        set(value) {
            field = value
            sp?.edit()?.putInt(KEY_INTERVAL, value)?.apply()
        }

    var screenOn = true
        set(value) {
            field = value
            if (value) startPolling() else stopPolling()
        }

    fun init(context: Context, rateClient: RateClient) {
        appContext = context.applicationContext
        client = rateClient
        sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE).also { prefs ->
            rateClient.setSelectedCurrency(prefs.getString(KEY_CURRENCY, "美元") ?: "美元")
            interval = prefs.getInt(KEY_INTERVAL, 30)
        }
        scope.launch {
            rateClient.state.collect { state ->
                if (state.rates.isNotEmpty()) notifyWidget()
            }
        }
    }

    fun setSelectedCurrency(currency: String) {
        sp?.edit()?.putString(KEY_CURRENCY, currency)?.apply()
        client?.setSelectedCurrency(currency)
    }

    fun startPolling() {
        if (pollingJob?.isActive == true) return
        pollingJob = scope.launch {
            while (true) {
                if (!screenOn) break
                client?.load()
                delay(interval * 1000L)
            }
        }
    }

    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    fun flush() {
        client?.load()
    }

    fun scheduleNextAlarm(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = PendingIntent.getBroadcast(
            context, 0,
            Intent(context, RefreshBroadcastReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val intervalMs = interval * 1000L
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && am.canScheduleExactAlarms()) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + intervalMs, pi)
        } else {
            am.setInexactRepeating(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + intervalMs,
                intervalMs,
                pi
            )
        }
    }

    private fun notifyWidget() {
        val ctx = appContext ?: return
        ctx.sendBroadcast(Intent(ctx, RateWidget::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        })
    }
}
