package com.laxy.ecgrate.viewmodel

import androidx.lifecycle.ViewModel
import com.laxy.ecgrate.global.RateTask

class MainViewModel : ViewModel() {
    fun getRate() = RateTask.flush()
}
