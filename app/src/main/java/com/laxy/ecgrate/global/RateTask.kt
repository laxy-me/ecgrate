package com.laxy.ecgrate.global

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import com.laxy.ecgrate.entity.CurrencyRate
import com.laxy.ecgrate.network.RateRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 *
 * @author laxy
 * @date 2024/3/30
 */
object RateTask {
    private const val SP_NAME = "rate"
    private const val KEY_CURRENCY = "currency"
    private const val KEY_INTERVAL = "interval"

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
    var screenOn = true
        set(value) {
            field = value
            if (value) {
                schedule()
            }
        }
    val data = MutableLiveData<List<CurrencyRate.Body>>(emptyList())
    val selectedData = MutableLiveData<CurrencyRate.Body>(null)
    private var running = false

    fun initSp(context: Context) {
        sp = context.getSharedPreferences(SP_NAME, AppCompatActivity.MODE_PRIVATE)
    }

    fun schedule() {
        if (running) {
            return
        }
        running = true
        GlobalScope.launch(Dispatchers.IO) {
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

    private const val CONST_SUCCESS = "SUC0000"

    private suspend fun refresh() {
        try {
            val rate = RateRepository.rateApi.rate()
            if (rate.returnCode == CONST_SUCCESS) {
                data.postValue(rate.body)
                selectedData.postValue(rate.body.find { it.ccyNbr == selectedCurrency })
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun flush() {
        GlobalScope.launch(Dispatchers.IO) {
            refresh()
        }
    }
}