package com.hmiot.app.core.network.model

data class ApiEnvelope<T>(
    val code: String,
    val message: String,
    val data: T?,
    val traceId: String?
)
