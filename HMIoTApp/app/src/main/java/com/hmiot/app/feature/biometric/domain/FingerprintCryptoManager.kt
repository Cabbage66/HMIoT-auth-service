package com.hmiot.app.feature.biometric.domain

import android.os.Build
import android.util.Base64
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.nio.charset.StandardCharsets
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties

data class FingerprintSignatureProof(
    val keyId: String,
    val publicKeyBase64: String,
    val signatureBase64: String,
    val signAlgorithm: String
)

class FingerprintCryptoManager {
    companion object {
        const val SIGN_ALGORITHM = "SHA256withECDSA"
    }

    fun ensureKeyPair(keyId: String): String {
        val keyStore = keyStore()
        if (keyStore.getKey(keyId, null) == null || keyStore.getCertificate(keyId) == null) {
            generateKeyPair(keyId)
        }
        val publicKey = keyStore.getCertificate(keyId)?.publicKey
            ?: error("指纹密钥公钥不存在")
        return Base64.encodeToString(publicKey.encoded, Base64.NO_WRAP)
    }

    suspend fun signChallengeWithBiometric(
        activity: FragmentActivity,
        keyId: String,
        challenge: String,
        title: String,
        subtitle: String
    ): Result<FingerprintSignatureProof> {
        val capability = BiometricManager.from(activity)
            .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
        if (capability != BiometricManager.BIOMETRIC_SUCCESS) {
            return Result.failure(IllegalStateException("设备未配置可用强生物识别，code=$capability"))
        }

        val publicKeyBase64 = ensureKeyPair(keyId)
        val signature = prepareSignature(keyId)
            ?: return Result.failure(IllegalStateException("无法初始化指纹签名器"))

        return suspendCancellableCoroutine { continuation ->
            var completed = false
            val callbackPrompt = BiometricPrompt(
                activity,
                ContextCompat.getMainExecutor(activity),
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        if (!completed) {
                            completed = true
                            continuation.resume(Result.failure(IllegalStateException("生物识别错误($errorCode): $errString")))
                        }
                    }

                    override fun onAuthenticationFailed() {
                        // 非终止回调，继续等待用户再次尝试
                    }

                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        if (completed) return
                        try {
                            val cryptoSignature = result.cryptoObject?.signature
                                ?: throw IllegalStateException("未获取到签名上下文")
                            cryptoSignature.update(challenge.toByteArray(StandardCharsets.UTF_8))
                            val signed = cryptoSignature.sign()
                            completed = true
                            continuation.resume(
                                Result.success(
                                    FingerprintSignatureProof(
                                        keyId = keyId,
                                        publicKeyBase64 = publicKeyBase64,
                                        signatureBase64 = Base64.encodeToString(signed, Base64.NO_WRAP),
                                        signAlgorithm = SIGN_ALGORITHM
                                    )
                                )
                            )
                        } catch (ex: Exception) {
                            if (!completed) {
                                completed = true
                                continuation.resume(Result.failure(ex))
                            }
                        }
                    }
                }
            )
            callbackPrompt.authenticate(
                BiometricPrompt.PromptInfo.Builder()
                    .setTitle(title)
                    .setSubtitle(subtitle)
                    .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                    .setNegativeButtonText("取消")
                    .build(),
                BiometricPrompt.CryptoObject(signature)
            )
        }
    }

    private fun keyStore(): KeyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    private fun generateKeyPair(keyId: String) {
        val generator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore")
        val builder = KeyGenParameterSpec.Builder(
            keyId,
            KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
        )
            .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
            .setDigests(
                KeyProperties.DIGEST_SHA256,
                KeyProperties.DIGEST_SHA384,
                KeyProperties.DIGEST_SHA512
            )
            .setUserAuthenticationRequired(true)
            .setInvalidatedByBiometricEnrollment(true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            builder.setUserAuthenticationParameters(
                0,
                KeyProperties.AUTH_BIOMETRIC_STRONG
            )
        } else {
            builder.setUserAuthenticationValidityDurationSeconds(-1)
        }

        generator.initialize(builder.build())
        generator.generateKeyPair()
    }

    private fun prepareSignature(keyId: String): Signature? {
        return try {
            val privateKey = loadPrivateKey(keyId) ?: return null
            Signature.getInstance(SIGN_ALGORITHM).apply {
                initSign(privateKey)
            }
        } catch (_: KeyPermanentlyInvalidatedException) {
            keyStore().deleteEntry(keyId)
            ensureKeyPair(keyId)
            val privateKey = loadPrivateKey(keyId) ?: return null
            Signature.getInstance(SIGN_ALGORITHM).apply {
                initSign(privateKey)
            }
        }
    }

    private fun loadPrivateKey(keyId: String): PrivateKey? {
        return keyStore().getKey(keyId, null) as? PrivateKey
    }
}
