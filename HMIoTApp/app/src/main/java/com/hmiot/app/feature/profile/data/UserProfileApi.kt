package com.hmiot.app.feature.profile.data

import com.hmiot.app.core.network.model.ApiEnvelope
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT

interface UserProfileApi {
    @GET("profile/me")
    suspend fun me(): ApiEnvelope<UserProfileResponse>

    @PUT("profile/me")
    suspend fun updateMe(@Body request: UserProfileUpdateRequest): ApiEnvelope<UserProfileResponse>
}

data class UserProfileResponse(
    val userId: Long,
    val username: String,
    val realName: String,
    val phone: String?,
    val email: String?,
    val idCardNo: String?
)

data class UserProfileUpdateRequest(
    val realName: String,
    val phone: String?,
    val email: String?,
    val idCardNo: String?
)
