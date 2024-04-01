package com.laxy.ecgrate.network

import com.laxy.ecgrate.entity.CurrencyRate
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

/**
 *
 * @author laxy
 * @date 2024/3/29
 */
object RateRepository {
    val rateApi: RateApi = Retrofit.Builder()
        .baseUrl("https://fx.cmbchina.com")
        .addConverterFactory(GsonConverterFactory.create())
        .client(ApiConfig().configOkHttp(timeOut = 3L))
        .build().create(RateApi::class.java)

    interface RateApi {
        @GET("/api/v1/fx/rate")
        suspend fun rate(): CurrencyRate
    }
}