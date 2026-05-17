package com.laxy.ecgrate.network

import com.laxy.ecgrate.entity.CurrencyRate
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

private const val BASE_URL = "https://fx.cmbchina.com"

class RateRepository {
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 15_000
            socketTimeoutMillis = 30_000
        }
        install(Logging) {
            level = LogLevel.NONE
        }
        defaultRequest {
            header("User-Agent", "Mozilla/5.0 (compatible; EcgRate/1.0)")
            header("Referer", "$BASE_URL/")
            header("Accept", "application/json, */*")
        }
    }

    suspend fun getRate(): CurrencyRate =
        client.get("$BASE_URL/api/v1/fx/rate").body()
}
