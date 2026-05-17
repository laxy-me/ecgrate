package com.laxy.ecgrate

import android.app.Application

class EcgrateApp : Application() {
    companion object {
        lateinit var instance: EcgrateApp
            private set
    }

    val rateClient by lazy { RateClient() }

    override fun onCreate() {
        super.onCreate()
        instance = this
        RateManager.init(this, rateClient)
    }
}
