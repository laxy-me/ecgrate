package com.laxy.ecgrate

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class IOSRateClient {
    val client = RateClient()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    fun startObserving(onState: (RateState) -> Unit) {
        scope.launch {
            client.state.collect { state -> onState(state) }
        }
    }

    fun refresh() = client.load()

    fun setSelectedCurrency(currency: String) = client.setSelectedCurrency(currency)

    fun dispose() {
        scope.cancel()
        client.dispose()
    }
}
