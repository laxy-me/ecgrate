package com.laxy.ecgrate.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laxy.ecgrate.global.RateTask.data
import com.laxy.ecgrate.global.RateTask.selectedCurrency
import com.laxy.ecgrate.global.RateTask.selectedData
import com.laxy.ecgrate.network.RateRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 *
 * @author laxy
 * @date 2024/3/29
 */
class MainViewModel : ViewModel() {
    fun getRate() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val rate = RateRepository.rateApi.rate()
                if (rate.returnCode == "SUC0000") {
                    data.postValue(rate.body)
                    selectedData.postValue(rate.body.find { it.ccyNbr == selectedCurrency })
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}