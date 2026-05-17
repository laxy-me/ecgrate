package com.laxy.ecgrate

import com.laxy.ecgrate.entity.CurrencyRate
import com.laxy.ecgrate.network.RateRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RateState(
    val rates: List<CurrencyRate.Body> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val lastUpdated: String = "",
    val selectedCurrency: String = "美元"
)

class RateClient {
    private val repository = RateRepository()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _state = MutableStateFlow(RateState())
    val state: StateFlow<RateState> = _state.asStateFlow()

    fun load() {
        scope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val rate = repository.getRate()
                if (rate.returnCode == "SUC0000") {
                    _state.update { current ->
                        current.copy(
                            rates = rate.body,
                            isLoading = false,
                            lastUpdated = rate.body.firstOrNull()
                                ?.let { b -> "${b.ratDat} ${b.ratTim}" } ?: ""
                        )
                    }
                } else {
                    _state.update { it.copy(isLoading = false, error = rate.errorMsg ?: "获取汇率失败") }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message ?: "网络错误") }
            }
        }
    }

    fun setSelectedCurrency(currency: String) {
        _state.update { it.copy(selectedCurrency = currency) }
    }

    fun selectedBody(): CurrencyRate.Body? =
        _state.value.rates.find { it.ccyNbr == _state.value.selectedCurrency }

    fun dispose() {
        scope.cancel()
    }
}
