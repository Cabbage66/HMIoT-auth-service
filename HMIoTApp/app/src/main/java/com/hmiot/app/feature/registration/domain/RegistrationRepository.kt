package com.hmiot.app.feature.registration.domain

import com.hmiot.app.core.network.model.ApiEnvelope
import com.hmiot.app.core.network.model.ApiResult
import com.hmiot.app.feature.registration.data.PageEnvelope
import com.hmiot.app.feature.registration.data.RegistrationApi
import com.hmiot.app.feature.registration.data.RegistrationProgressItem
import com.hmiot.app.feature.registration.data.RegistrationSubmitRequest
import com.hmiot.app.feature.registration.data.RegistrationSubmitResponse
import retrofit2.HttpException

class RegistrationRepository(
    private val registrationApi: RegistrationApi
) {
    suspend fun submit(request: RegistrationSubmitRequest): ApiResult<RegistrationSubmitResponse> {
        return runApi { registrationApi.submit(request) }
    }

    suspend fun queryProgress(username: String): ApiResult<PageEnvelope<RegistrationProgressItem>> {
        return runApi { registrationApi.progress(username = username.trim()) }
    }

    private suspend fun <T> runApi(block: suspend () -> ApiEnvelope<T>): ApiResult<T> {
        return try {
            val envelope = block()
            if (envelope.code == "0" && envelope.data != null) {
                ApiResult.Success(envelope.data, envelope.traceId)
            } else {
                ApiResult.Failure(envelope.code, envelope.message, envelope.traceId)
            }
        } catch (http: HttpException) {
            ApiResult.Failure(http.code().toString(), "HTTP ${http.code()}", null)
        } catch (ex: Exception) {
            ApiResult.Failure("NETWORK_ERROR", ex.message ?: "网络异常", null)
        }
    }
}
