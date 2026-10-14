package com.hmiot.app.feature.registration.domain

import com.hmiot.app.core.network.model.ApiEnvelope
import com.hmiot.app.core.network.model.ApiResult
import com.hmiot.app.core.storage.datastore.AppPreferenceStore
import com.hmiot.app.core.storage.datastore.LocalRegistrationRecord
import com.hmiot.app.feature.registration.data.PageEnvelope
import com.hmiot.app.feature.registration.data.RegistrationApi
import com.hmiot.app.feature.registration.data.RegistrationProgressItem
import com.hmiot.app.feature.registration.data.RegistrationSubmitRequest
import com.hmiot.app.feature.registration.data.RegistrationSubmitResponse
import kotlinx.coroutines.flow.Flow
import retrofit2.HttpException
import java.time.OffsetDateTime

class RegistrationRepository(
    private val registrationApi: RegistrationApi,
    private val store: AppPreferenceStore
) {
    val localRegistrationRecords: Flow<List<LocalRegistrationRecord>> = store.localRegistrationRecords

    suspend fun submit(request: RegistrationSubmitRequest): ApiResult<RegistrationSubmitResponse> {
        val result = runApi { registrationApi.submit(request) }
        if (result is ApiResult.Success) {
            runCatching {
                store.appendLocalRegistrationRecord(
                    LocalRegistrationRecord(
                        registrationId = result.data.registrationId,
                        userId = result.data.userId,
                        username = result.data.username,
                        realName = request.realName,
                        phone = request.phone,
                        email = request.email,
                        status = result.data.status,
                        rejectReason = null,
                        submittedAt = result.data.submittedAt ?: OffsetDateTime.now().toString(),
                        terminalCode = ""
                    )
                )
            }
        }
        return result
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
