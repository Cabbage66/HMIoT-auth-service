package com.hmiot.app.feature.terminal.domain

import android.content.Context
import android.os.Build
import android.provider.Settings
import java.security.MessageDigest

data class TerminalIdentity(
    val terminalCode: String,
    val deviceFingerprint: String,
    val metadataJson: String
)

object TerminalIdentityProvider {
    fun resolve(context: Context): TerminalIdentity {
        val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID).orEmpty()
        val rawDevice = listOf(
            androidId,
            Build.BRAND.orEmpty(),
            Build.MANUFACTURER.orEmpty(),
            Build.MODEL.orEmpty(),
            Build.DEVICE.orEmpty()
        ).joinToString("|")
        val deviceHash = sha256(rawDevice).take(24).uppercase()
        val terminalCode = "ANDROID-$deviceHash"
        val deviceFingerprint = sha256(listOf(rawDevice, Build.FINGERPRINT.orEmpty()).joinToString("|"))
        val metadata = """{"source":"ANDROID_AUTO","androidIdHash":"${sha256(androidId).take(16)}","deviceFingerprintHash":"${deviceFingerprint.take(16)}","brand":"${escape(Build.BRAND.orEmpty())}","model":"${escape(Build.MODEL.orEmpty())}"}"""
        return TerminalIdentity(
            terminalCode = terminalCode,
            deviceFingerprint = deviceFingerprint,
            metadataJson = metadata
        )
    }

    private fun sha256(value: String): String {
        return MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }

    private fun escape(value: String): String {
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
    }
}

