package com.laxy.ecgrate.network

import android.os.Debug
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

class ApiConfig {
    private val logger = HttpLoggingInterceptor(object : HttpLoggingInterceptor.Logger {
        override fun log(message: String) {
            takeIf { Debug.isDebuggerConnected() }?.let {
                HttpLoggingInterceptor.Logger.DEFAULT.log(message)
            }
        }
    }).apply {
        setLevel(HttpLoggingInterceptor.Level.BODY)
    }

    private val headerInterceptor = Interceptor { chain ->
        val builder = chain.request().newBuilder()
        chain.proceed(builder.build())
    }

    fun configOkHttp(isNeedHeader: Boolean = true, timeOut: Long = 10L): OkHttpClient {
        val builder = OkHttpClient.Builder()
        if (isNeedHeader) {
            builder.addInterceptor(headerInterceptor)
        }
        builder.addInterceptor(logger)
        builder.connectTimeout(timeOut, TimeUnit.SECONDS)
        builder.readTimeout(timeOut, TimeUnit.SECONDS)
        builder.writeTimeout(timeOut, TimeUnit.SECONDS)
        return builder.build()
    }
}