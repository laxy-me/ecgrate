package com.laxy.ecgrate.entity

sealed class Results<out T> {
    companion object {
        fun <T> success(result: T): Results<T> = Success(result)
        fun <T> failure(error: RequestError): Results<T> = Failure(error)
    }

    data class Failure(val error: RequestError) : Results<Nothing>()
    data class Success<out T>(val data: T) : Results<T>()
}

class RequestError(val code: Int, message: String? = null, cause: Throwable? = null) : Exception(message, cause)

data class BaseResp<T>(val success: Boolean, val data: T? = null)
