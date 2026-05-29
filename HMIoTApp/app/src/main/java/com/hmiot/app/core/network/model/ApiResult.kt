package com.hmiot.app.core.network.model

sealed interface ApiResult<out T> {
    data class Success<T>(val data: T, val traceId: String?) : ApiResult<T>
    data class Failure(val code: String, val message: String, val traceId: String?) : ApiResult<Nothing>
}
