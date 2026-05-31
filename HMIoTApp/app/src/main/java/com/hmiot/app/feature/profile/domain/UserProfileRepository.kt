package com.hmiot.app.feature.profile.domain

import com.hmiot.app.core.network.model.ApiResult
import com.hmiot.app.feature.profile.data.UserProfileApi
import com.hmiot.app.feature.profile.data.UserProfileResponse
import com.hmiot.app.feature.profile.data.UserProfileUpdateRequest
import retrofit2.HttpException

class UserProfileRepository(
    private val api: UserProfileApi
) {
    suspend fun me(): ApiResult<UserProfileResponse> = runApi { api.me() }

    suspend fun updateMe(request: UserProfileUpdateRequest): ApiResult<UserProfileResponse> =
        runApi { api.updateMe(request) }

    private suspend fun <T> runApi(block: suspend () -> com.hmiot.app.core.network.model.ApiEnvelope<T>): ApiResult<T> {
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
