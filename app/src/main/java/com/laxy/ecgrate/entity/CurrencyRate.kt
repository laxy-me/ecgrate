package com.laxy.ecgrate.entity


import com.google.gson.annotations.SerializedName

data class CurrencyRate(
    val returnCode: String = "",
    val errorMsg: Any? = null,
    val body: List<Body> = listOf()
) {
    data class Body(
        val ccyNbr: String = "",
        val ccyNbrEng: String = "",
        val rtbBid: String = "",
        /**
         * 现汇卖出价
         */
        val rthOfr: String = "",
        /**
         * 现钞卖出价
         */
        val rtcOfr: String = "",
        /**
         * 现汇买入价
         */
        val rthBid: String = "",
        /**
         * 现钞买入价
         */
        val rtcBid: String = "",
        val ratTim: String = "",
        val ratDat: String = "",
        val ccyExc: String = ""
    )
}
//rtbBid: "722.28"