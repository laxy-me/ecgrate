package com.laxy.ecgrate.entity

import kotlinx.serialization.Serializable

@Serializable
data class CurrencyRate(
    val returnCode: String = "",
    val errorMsg: String? = null,
    val body: List<Body> = emptyList()
) {
    @Serializable
    data class Body(
        val ccyNbr: String = "",
        val ccyNbrEng: String = "",
        val rthOfr: String = "",
        val rtcOfr: String = "",
        val rthBid: String = "",
        val rtcBid: String = "",
        val ratTim: String = "",
        val ratDat: String = "",
        val ccyExc: String = ""
    )
}
