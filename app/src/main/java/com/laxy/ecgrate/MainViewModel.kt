package com.laxy.ecgrate

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val client = (application as EcgrateApp).rateClient

    val state = client.state.stateIn(viewModelScope, SharingStarted.Eagerly, RateState())

    fun refresh() = client.load()

    fun setSelectedCurrency(currency: String) {
        RateManager.setSelectedCurrency(currency)
        client.load()
    }

    fun setInterval(seconds: Int) {
        RateManager.interval = seconds
    }
}
