package com.laxy.ecgrate.global

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.SystemClock
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import com.laxy.ecgrate.entity.CurrencyRate
import com.laxy.ecgrate.network.RateRepository
import com.laxy.ecgrate.receiver.RefreshBroadcastReceiver
import com.laxy.ecgrate.widget.RateWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object RateTask {
    private const val SP_NAME = "rate"
    private const val KEY_CURRENCY = "currency"
    private const val KEY_INTERVAL = "interval"
    private const val CONST_SUCCESS = "SUC0000"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var appContext: Context? = null

    var interval: Int = 30
        set(value) {
            field = value
            sp?.edit()?.putInt(KEY_INTERVAL, value)?.apply()
        }

    var sp: SharedPreferences? = null
        set(value) {
            if (value != null) {
                field = value
                selectedCurrency = value.getString(KEY_CURRENCY, null) ?: "美元"
                interval = value.getInt(KEY_INTERVAL, 30)
            }
        }

    var selectedCurrency = "美元"
        set(value) {
            field = value
            sp?.edit()?.putString(KEY_CURRENCY, value)?.apply()
        }

    var screenOn = true
        set(value) {
            field = value
            if (value) schedule()
        }

    val data = MutableLiveData<List<CurrencyRate.Body>>(emptyList())
    val selectedData = MutableLiveData<CurrencyRate.Body>(null)
    private var running = false

    fun initSp(context: Context) {
        appContext = context.applicationContext
        sp = context.getSharedPreferences(SP_NAME, AppCompatActivity.MODE_PRIVATE)
    }

    fun schedule() {
        if (running) return
        running = true
        scope.launch {
            while (true) {
                if (!screenOn) {
                    running = false
                    break
                }
                refresh()
                delay(interval * 1000L)
            }
        }
    }

    private suspend fun refresh() {
        try {
            val rate = RateRepository.rateApi.rate()
            if (rate.returnCode == CONST_SUCCESS) {
                data.postValue(rate.body)
                selectedData.postValue(rate.body.find { it.ccyNbr == selectedCurrency })
                // postValue 先入队主线程，withContext 后入队，保证 widget 读到最新值
                withContext(Dispatchers.Main) { notifyWidgetUpdate() }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun notifyWidgetUpdate() {
        val ctx = appContext ?: return
        ctx.sendBroadcast(Intent(ctx, RateWidget::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        })
    }

    fun flush() {
        scope.launch { refresh() }
    }

    fun scheduleNextAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = PendingIntent.getBroadcast(
            context, 0,
            Intent(context, RefreshBroadcastReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val intervalMillis = interval * 1000L
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
            // setExactAndAllowWhileIdle 是一次性的，RefreshBroadcastReceiver 收到后会重新调度
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + intervalMillis,
                pendingIntent
            )
        } else {
            // setInexactRepeating 会自动重复，无需手动续期
            alarmManager.setInexactRepeating(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + intervalMillis,
                intervalMillis,
                pendingIntent
            )
        }
    }
}
