package com.hmiot.app.core.storage.datastore

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.security.MessageDigest
import java.security.SecureRandom

private val Context.dataStore by preferencesDataStore(name = "hmiot_preferences")

class AppPreferenceStore(private val context: Context) {
    private val gson = Gson()
    private val sessionTokenKey = stringPreferencesKey("session_token")
    private val tokenTypeKey = stringPreferencesKey("token_type")
    private val userIdKey = longPreferencesKey("user_id")
    private val usernameKey = stringPreferencesKey("username")
    private val realNameKey = stringPreferencesKey("real_name")
    private val authMethodKey = stringPreferencesKey("auth_method")
    private val terminalIdKey = longPreferencesKey("terminal_id")
    private val terminalCodeKey = stringPreferencesKey("terminal_code")
    private val clientTypeKey = stringPreferencesKey("client_type")
    private val certSerialKey = stringPreferencesKey("cert_serial")
    private val certFingerprintKey = stringPreferencesKey("cert_fingerprint")
    private val certPemKey = stringPreferencesKey("cert_pem")
    private val terminalPendingEventsKey = stringPreferencesKey("terminal_pending_events")
    private val localPolicyBundleKey = stringPreferencesKey("local_policy_bundle")
    private val trustBlockedKey = stringPreferencesKey("trust_blocked")
    private val trustBlockedReasonKey = stringPreferencesKey("trust_blocked_reason")
    private val trustBlockedAtKey = stringPreferencesKey("trust_blocked_at")
    private val trustScoreKey = stringPreferencesKey("trust_score")
    private val trustLevelKey = stringPreferencesKey("trust_level")
    private val trustRiskLevelKey = stringPreferencesKey("trust_risk_level")
    private val trustDecisionKey = stringPreferencesKey("trust_decision")
    private val trustRequestIdKey = stringPreferencesKey("trust_request_id")
    private val trustRequestLogPathKey = stringPreferencesKey("trust_request_log_path")
    private val trustResultLogPathKey = stringPreferencesKey("trust_result_log_path")
    private val biometricPassHashKey = stringPreferencesKey("biometric_pass_hash")
    private val biometricPassSaltKey = stringPreferencesKey("biometric_pass_salt")
    private val biometricAuditLogsKey = stringPreferencesKey("biometric_audit_logs")

    val session: Flow<LocalSession?> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { prefs ->
            val token = prefs[sessionTokenKey] ?: return@map null
            LocalSession(
                token = token,
                tokenType = prefs[tokenTypeKey].orEmpty(),
                userId = prefs[userIdKey] ?: 0L,
                username = prefs[usernameKey].orEmpty(),
                realName = prefs[realNameKey].orEmpty(),
                authMethod = prefs[authMethodKey].orEmpty(),
                terminalId = prefs[terminalIdKey] ?: 0L,
                terminalCode = prefs[terminalCodeKey].orEmpty(),
                clientType = prefs[clientTypeKey].orEmpty()
            )
        }

    suspend fun updateSession(session: LocalSession?) {
        context.dataStore.edit { prefs ->
            if (session == null || session.token.isBlank()) {
                prefs.remove(sessionTokenKey)
                prefs.remove(tokenTypeKey)
                prefs.remove(userIdKey)
                prefs.remove(usernameKey)
                prefs.remove(realNameKey)
                prefs.remove(authMethodKey)
                prefs.remove(terminalIdKey)
                prefs.remove(terminalCodeKey)
                prefs.remove(clientTypeKey)
            } else {
                prefs[sessionTokenKey] = session.token
                prefs[tokenTypeKey] = session.tokenType
                prefs[userIdKey] = session.userId
                prefs[usernameKey] = session.username
                prefs[realNameKey] = session.realName
                prefs[authMethodKey] = session.authMethod
                prefs[terminalIdKey] = session.terminalId
                prefs[terminalCodeKey] = session.terminalCode
                prefs[clientTypeKey] = session.clientType
            }
        }
    }

    val certificateProfile: Flow<LocalCertificateProfile?> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { prefs ->
            val serial = prefs[certSerialKey] ?: return@map null
            LocalCertificateProfile(
                serialNumber = serial,
                fingerprintSha256 = prefs[certFingerprintKey].orEmpty(),
                certificatePem = prefs[certPemKey].orEmpty()
            )
        }

    suspend fun updateCertificateProfile(profile: LocalCertificateProfile?) {
        context.dataStore.edit { prefs ->
            if (profile == null || profile.serialNumber.isBlank()) {
                prefs.remove(certSerialKey)
                prefs.remove(certFingerprintKey)
                prefs.remove(certPemKey)
            } else {
                prefs[certSerialKey] = profile.serialNumber
                prefs[certFingerprintKey] = profile.fingerprintSha256
                prefs[certPemKey] = profile.certificatePem
            }
        }
    }

    val pendingTerminalEvents: Flow<List<PendingTerminalEvent>> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { prefs ->
            val json = prefs[terminalPendingEventsKey].orEmpty()
            if (json.isBlank()) {
                emptyList()
            } else {
                runCatching {
                    gson.fromJson<List<PendingTerminalEvent>>(json, object : TypeToken<List<PendingTerminalEvent>>() {}.type)
                }.getOrElse { emptyList() }
            }
        }

    suspend fun updatePendingTerminalEvents(events: List<PendingTerminalEvent>) {
        context.dataStore.edit { prefs ->
            if (events.isEmpty()) {
                prefs.remove(terminalPendingEventsKey)
            } else {
                prefs[terminalPendingEventsKey] = gson.toJson(events)
            }
        }
    }

    suspend fun pendingTerminalEventsSnapshot(): List<PendingTerminalEvent> = pendingTerminalEvents.first()

    val localPolicyBundle: Flow<String?> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { prefs -> prefs[localPolicyBundleKey] }

    suspend fun updateLocalPolicyBundle(bundleJson: String?) {
        context.dataStore.edit { prefs ->
            if (bundleJson.isNullOrBlank()) {
                prefs.remove(localPolicyBundleKey)
            } else {
                prefs[localPolicyBundleKey] = bundleJson
            }
        }
    }

    val trustGuardState: Flow<TrustGuardState> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { prefs ->
            TrustGuardState(
                blocked = prefs[trustBlockedKey] == "1",
                reason = prefs[trustBlockedReasonKey].orEmpty(),
                updatedAt = prefs[trustBlockedAtKey].orEmpty(),
                trustScore = prefs[trustScoreKey]?.toDoubleOrNull(),
                trustLevel = prefs[trustLevelKey]?.toIntOrNull(),
                riskLevel = prefs[trustRiskLevelKey],
                decision = prefs[trustDecisionKey],
                requestId = prefs[trustRequestIdKey],
                requestLogPath = prefs[trustRequestLogPathKey],
                resultLogPath = prefs[trustResultLogPathKey]
            )
        }

    suspend fun updateTrustGuardState(state: TrustGuardState) {
        context.dataStore.edit { prefs ->
            prefs[trustBlockedKey] = if (state.blocked) "1" else "0"
            prefs[trustBlockedReasonKey] = state.reason
            prefs[trustBlockedAtKey] = state.updatedAt
            if (state.trustScore == null) prefs.remove(trustScoreKey) else prefs[trustScoreKey] = state.trustScore.toString()
            if (state.trustLevel == null) prefs.remove(trustLevelKey) else prefs[trustLevelKey] = state.trustLevel.toString()
            if (state.riskLevel.isNullOrBlank()) prefs.remove(trustRiskLevelKey) else prefs[trustRiskLevelKey] = state.riskLevel
            if (state.decision.isNullOrBlank()) prefs.remove(trustDecisionKey) else prefs[trustDecisionKey] = state.decision
            if (state.requestId.isNullOrBlank()) prefs.remove(trustRequestIdKey) else prefs[trustRequestIdKey] = state.requestId
            if (state.requestLogPath.isNullOrBlank()) prefs.remove(trustRequestLogPathKey) else prefs[trustRequestLogPathKey] = state.requestLogPath
            if (state.resultLogPath.isNullOrBlank()) prefs.remove(trustResultLogPathKey) else prefs[trustResultLogPathKey] = state.resultLogPath
        }
    }

    suspend fun setBiometricProtectionPassphrase(passphrase: String) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val hash = sha256Hex(salt + passphrase.toByteArray(Charsets.UTF_8))
        context.dataStore.edit { prefs ->
            prefs[biometricPassSaltKey] = salt.joinToString("") { "%02x".format(it) }
            prefs[biometricPassHashKey] = hash
        }
    }

    suspend fun verifyBiometricProtectionPassphrase(passphrase: String): Boolean {
        val prefs = context.dataStore.data.first()
        val saltHex = prefs[biometricPassSaltKey].orEmpty()
        val hash = prefs[biometricPassHashKey].orEmpty()
        if (saltHex.isBlank() || hash.isBlank()) return false
        val salt = hexToBytes(saltHex)
        val current = sha256Hex(salt + passphrase.toByteArray(Charsets.UTF_8))
        return current == hash
    }

    suspend fun hasBiometricProtectionPassphrase(): Boolean {
        val prefs = context.dataStore.data.first()
        return prefs[biometricPassHashKey]?.isNotBlank() == true && prefs[biometricPassSaltKey]?.isNotBlank() == true
    }

    val biometricAuditLogs: Flow<List<BiometricAuditLog>> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { prefs ->
            val json = prefs[biometricAuditLogsKey].orEmpty()
            if (json.isBlank()) emptyList()
            else runCatching {
                gson.fromJson<List<BiometricAuditLog>>(json, object : TypeToken<List<BiometricAuditLog>>() {}.type)
            }.getOrElse { emptyList() }
        }

    suspend fun appendBiometricAuditLog(log: BiometricAuditLog) {
        val current = biometricAuditLogs.first().toMutableList()
        current.add(0, log)
        val trimmed = current.take(200)
        context.dataStore.edit { prefs -> prefs[biometricAuditLogsKey] = gson.toJson(trimmed) }
    }

    private fun sha256Hex(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes)
        .joinToString("") { "%02x".format(it) }

    private fun hexToBytes(hex: String): ByteArray {
        val cleaned = hex.trim()
        return ByteArray(cleaned.length / 2) { idx ->
            cleaned.substring(idx * 2, idx * 2 + 2).toInt(16).toByte()
        }
    }
}

data class LocalSession(
    val token: String,
    val tokenType: String,
    val userId: Long,
    val username: String,
    val realName: String,
    val authMethod: String,
    val terminalId: Long,
    val terminalCode: String,
    val clientType: String
)

data class LocalCertificateProfile(
    val serialNumber: String,
    val fingerprintSha256: String,
    val certificatePem: String
)

data class PendingTerminalEvent(
    val eventType: String,
    val eventLevel: String,
    val eventTime: String,
    val eventPayload: Map<String, Any?>
)

data class TrustGuardState(
    val blocked: Boolean,
    val reason: String,
    val updatedAt: String,
    val trustScore: Double? = null,
    val trustLevel: Int? = null,
    val riskLevel: String? = null,
    val decision: String? = null,
    val requestId: String? = null,
    val requestLogPath: String? = null,
    val resultLogPath: String? = null
)

data class BiometricAuditLog(
    val action: String,
    val biometricType: String,
    val compareMode: String,
    val result: String,
    val detail: String,
    val at: String
)
