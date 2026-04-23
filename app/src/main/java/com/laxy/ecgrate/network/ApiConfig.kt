package com.laxy.ecgrate.network

import android.os.Debug
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

object ApiConfig {
    private val logger = HttpLoggingInterceptor { message ->
        if (Debug.isDebuggerConnected()) HttpLoggingInterceptor.Logger.DEFAULT.log(message)
    }.apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    fun configOkHttp(timeOut: Long = 10L): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(logger)
            .connectTimeout(timeOut, TimeUnit.SECONDS)
            .readTimeout(timeOut, TimeUnit.SECONDS)
            .writeTimeout(timeOut, TimeUnit.SECONDS)
            .build()
}
