package com.hmiot.app.core.network

import com.hmiot.app.core.session.SessionHolder
import okhttp3.Interceptor
import okhttp3.Response
import java.util.UUID

class AuthHeaderInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = SessionHolder.token.value
        val requestBuilder = chain.request().newBuilder()
            .header("X-Trace-Id", UUID.randomUUID().toString())
        if (!token.isNullOrBlank()) {
            requestBuilder.header("Authorization", "Bearer $token")
        }
        return chain.proceed(requestBuilder.build())
    }
}
