package com.hmiot.app.feature.biometric.domain

object FingerprintKeyId {
    fun fromUsername(username: String): String {
        val normalized = username.trim().lowercase()
            .replace(Regex("[^a-z0-9._-]"), "_")
            .ifBlank { "anonymous" }
        return "hmiot_fp_$normalized"
    }
}

