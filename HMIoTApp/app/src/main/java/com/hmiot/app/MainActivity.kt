package com.hmiot.app

import android.Manifest
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Patterns
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.identitylogin.InternalIdentityUiData
import com.example.identitylogin.InternalSystemScreen
import com.hmiot.app.core.network.model.ApiResult
import com.hmiot.app.feature.auth.ui.LoginScreen
import com.hmiot.app.feature.auth.ui.LoginViewModelFactory
import com.hmiot.app.feature.access.data.AppResource
import com.hmiot.app.feature.certificate.ui.CertificateScreen
import com.hmiot.app.feature.certificate.ui.CertificateViewModelFactory
import com.hmiot.app.feature.biometric.data.FingerprintSubmitRequest
import com.hmiot.app.feature.biometric.domain.FingerprintCryptoManager
import com.hmiot.app.feature.biometric.domain.FingerprintKeyId
import com.hmiot.app.feature.biometric.domain.VoiceSampleCollector
import com.hmiot.app.feature.profile.data.UserProfileResponse
import com.hmiot.app.feature.profile.data.UserProfileUpdateRequest
import com.hmiot.app.feature.registration.ui.RegistrationScreen
import com.hmiot.app.feature.registration.ui.RegistrationViewModelFactory
import com.hmiot.app.feature.registration.ui.RecordsScreen
import com.hmiot.app.ui.theme.HMIoTAppTheme
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HMIoTAppTheme { AppEntry() }
        }
    }
}

private tailrec fun Context.findFragmentActivity(): FragmentActivity? = when (this) {
    is FragmentActivity -> this
    is ContextWrapper -> baseContext.findFragmentActivity()
    else -> null
}

private fun Bitmap.toJpegBase64(): String {
    val output = ByteArrayOutputStream()
    compress(Bitmap.CompressFormat.JPEG, 90, output)
    return Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
}

@Composable
private fun AppEntry() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val app = context.applicationContext as HmiotApplication
    val authRepository = app.appContainer.authRepository
    val registrationRepository = app.appContainer.registrationRepository
    val certificateProfileRepository = app.appContainer.certificateProfileRepository
    val terminalMonitorRepository = app.appContainer.terminalMonitorRepository
    val policyRepository = app.appContainer.policyRepository
    val accessRepository = app.appContainer.accessRepository
    val biometricRepository = app.appContainer.biometricRepository
    val userProfileRepository = app.appContainer.userProfileRepository
    val trustGuardRepository = app.appContainer.trustGuardRepository
    val releaseRepository = app.appContainer.releaseValidationRepository

    val loginFactory = LoginViewModelFactory(authRepository)
    val registrationFactory = RegistrationViewModelFactory(registrationRepository)
    val certificateFactory = CertificateViewModelFactory(certificateProfileRepository)

    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    val fingerprintCryptoManager = remember { FingerprintCryptoManager() }
    val session by authRepository.session.collectAsState(initial = null)
    val apps = remember { mutableStateListOf<AppResource>() }
    var actionInfo by remember { mutableStateOf<String?>(null) }
    var actionError by remember { mutableStateOf<String?>(null) }
    var showProfileEditDialog by remember { mutableStateOf(false) }
    var profileEditSaving by remember { mutableStateOf(false) }
    var profileEditError by remember { mutableStateOf<String?>(null) }
    var profileUi by remember {
        mutableStateOf(
            InternalIdentityUiData(
                username = "zhangsan",
                realName = "张三",
                phoneMasked = "",
                emailMasked = "",
                idCardMasked = "",
                certificateNo = "CERT-202605290001"
            )
        )
    }

    fun normalize(value: String?): String = value?.trim().orEmpty()

    fun mergeProfile(sessionFallback: com.hmiot.app.core.storage.datastore.LocalSession?, remote: UserProfileResponse?): InternalIdentityUiData {
        val certificateNo = sessionFallback?.let { "TERMINAL-${it.terminalCode}" } ?: "CERT-202605290001"
        return InternalIdentityUiData(
            username = normalize(remote?.username).ifBlank { sessionFallback?.username ?: "zhangsan" },
            realName = normalize(remote?.realName).ifBlank { sessionFallback?.realName ?: "张三" },
            phoneMasked = normalize(remote?.phone),
            emailMasked = normalize(remote?.email),
            idCardMasked = normalize(remote?.idCardNo),
            certificateNo = certificateNo
        )
    }

    suspend fun loadAppsIfNeeded() {
        if (apps.isNotEmpty()) return
        when (val result = accessRepository.listApps()) {
            is ApiResult.Success -> {
                apps.clear()
                apps.addAll(result.data)
                actionInfo = "应用已加载：${apps.size}个"
                actionError = null
            }
            is ApiResult.Failure -> {
                actionError = result.message
            }
        }
    }

    fun resolveAppByTitle(title: String): AppResource? {
        if (apps.isEmpty()) return null
        val index = when (title) {
            "公文", "消息" -> 0
            "业务", "审批" -> 1
            "档案", "协同" -> 2
            else -> 0
        }
        return apps.getOrNull(index % apps.size)
    }

    suspend fun enrollOrRecollectVoiceprint() {
        actionInfo = "正在采集3秒声纹样本..."
        actionError = null
        val sample = runCatching { VoiceSampleCollector().collectBase64Wav(3) }
            .getOrElse { ex ->
                actionError = "声纹采集失败：${ex.message}"
                return
            }
        val templates = when (val result = biometricRepository.voiceTemplates()) {
            is ApiResult.Success -> result.data
            is ApiResult.Failure -> {
                actionError = result.message
                return
            }
        }
        val hasVoiceTemplate = templates.any { it.templateStatus == "ACTIVE" }
        val action = if (hasVoiceTemplate) "补采替换" else "注册绑定"
        val submitResult = if (hasVoiceTemplate) {
            biometricRepository.voiceRecollect(sample)
        } else {
            biometricRepository.voiceEnroll(sample)
        }
        when (submitResult) {
            is ApiResult.Success -> {
                val template = submitResult.data
                val healthText = when (val health = biometricRepository.voiceHealth()) {
                    is ApiResult.Success -> "，模型=${health.data.modelVersion}"
                    is ApiResult.Failure -> ""
                }
                actionInfo = "声纹${action}提交成功：模板#${template.id}，审核=${template.auditStatus}${healthText}"
                actionError = null
            }
            is ApiResult.Failure -> {
                actionError = "声纹${action}失败：${submitResult.message}"
            }
        }
    }

    suspend fun enrollOrRecollectFingerprint() {
        val currentSession = session
        if (currentSession == null) {
            actionError = "请先登录后再管理指纹"
            return
        }
        val activity = context.findFragmentActivity()
        if (activity == null) {
            actionError = "当前页面上下文不支持指纹采集"
            return
        }
        val keyId = FingerprintKeyId.fromUsername(currentSession.username)

        val templates = when (val result = biometricRepository.fingerprintTemplates()) {
            is ApiResult.Success -> result.data
            is ApiResult.Failure -> {
                actionError = result.message
                return
            }
        }
        val hasActive = templates.any { it.templateStatus == "ACTIVE" }
        val purpose = if (hasActive) "RECOLLECT" else "ENROLL"
        val action = if (hasActive) "补采替换" else "注册绑定"

        val challenge = when (val result = biometricRepository.fingerprintChallenge(purpose = purpose, keyId = keyId)) {
            is ApiResult.Success -> result.data
            is ApiResult.Failure -> {
                actionError = "指纹${action}挑战获取失败：${result.message}"
                return
            }
        }

        actionInfo = "请完成系统指纹验证，正在签名提交..."
        val signature = fingerprintCryptoManager.signChallengeWithBiometric(
            activity = activity,
            keyId = challenge.keyId ?: keyId,
            challenge = challenge.challenge,
            title = "指纹$action",
            subtitle = "验证后提交模板到服务端"
        ).getOrElse { ex ->
            actionError = "指纹签名失败：${ex.message}"
            return
        }

        val request = FingerprintSubmitRequest(
            sessionId = challenge.sessionId,
            challengeId = challenge.challengeId,
            keyId = signature.keyId,
            publicKeyBase64 = signature.publicKeyBase64,
            signatureBase64 = signature.signatureBase64,
            signAlgorithm = signature.signAlgorithm,
            metadata = mapOf(
                "clientType" to "ANDROID",
                "osVersion" to (android.os.Build.VERSION.RELEASE ?: "unknown")
            )
        )
        val submitResult = if (hasActive) {
            biometricRepository.fingerprintRecollect(request)
        } else {
            biometricRepository.fingerprintEnroll(request)
        }

        when (submitResult) {
            is ApiResult.Success -> {
                val template = submitResult.data
                val healthText = when (val health = biometricRepository.fingerprintHealth()) {
                    is ApiResult.Success -> "，模型=${health.data.modelVersion}"
                    is ApiResult.Failure -> ""
                }
                actionInfo = "指纹${action}提交成功：模板#${template.id}，审核=${template.auditStatus}${healthText}"
                actionError = null
            }
            is ApiResult.Failure -> {
                actionError = "指纹${action}失败：${submitResult.message}"
            }
        }
    }

    suspend fun enrollOrRecollectFace(imageBase64: String, imageFormat: String = "jpeg") {
        val currentSession = session
        if (currentSession == null) {
            actionError = "请先登录后再管理人脸"
            return
        }
        val templates = when (val result = biometricRepository.faceTemplates()) {
            is ApiResult.Success -> result.data
            is ApiResult.Failure -> {
                actionError = result.message
                return
            }
        }
        val hasActive = templates.any { it.templateStatus == "ACTIVE" }
        val action = if (hasActive) "补采替换" else "注册绑定"
        val submitResult = if (hasActive) {
            biometricRepository.faceRecollect(imageBase64, imageFormat)
        } else {
            biometricRepository.faceEnroll(imageBase64, imageFormat)
        }
        when (submitResult) {
            is ApiResult.Success -> {
                val template = submitResult.data
                val healthText = when (val health = biometricRepository.faceHealth()) {
                    is ApiResult.Success -> "，模型=${health.data.modelVersion}"
                    is ApiResult.Failure -> ""
                }
                actionInfo = "人脸${action}提交成功：模板#${template.id}，审核=${template.auditStatus}${healthText}"
                actionError = null
            }
            is ApiResult.Failure -> {
                actionError = "人脸${action}失败：${submitResult.message}"
            }
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) {
            actionError = "未授予麦克风权限，无法采集声纹"
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            enrollOrRecollectVoiceprint()
        }
    }
    val faceCaptureLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap: Bitmap? ->
        if (bitmap == null) {
            actionError = "未获取到人脸图像"
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            actionInfo = "人脸采集完成，正在提交模型比对..."
            actionError = null
            enrollOrRecollectFace(bitmap.toJpegBase64())
        }
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) {
            actionError = "未授予相机权限，无法采集人脸"
            return@rememberLauncherForActivityResult
        }
        faceCaptureLauncher.launch(null)
    }

    LaunchedEffect(Unit) {
        loadAppsIfNeeded()
    }
    LaunchedEffect(session?.userId, session?.terminalCode) {
        val currentSession = session
        if (currentSession == null) return@LaunchedEffect
        profileUi = mergeProfile(currentSession, null)
        when (val result = userProfileRepository.me()) {
            is ApiResult.Success -> {
                profileUi = mergeProfile(currentSession, result.data)
                actionError = null
            }
            is ApiResult.Failure -> {
                actionError = result.message
            }
        }
    }

    Scaffold(modifier = Modifier.fillMaxSize()) {
        NavHost(navController = navController, startDestination = "login") {
            composable("login") {
                LoginScreen(
                    factory = loginFactory,
                    onGoRegister = { navController.navigate("register") },
                    onGoCertificateCenter = { navController.navigate("certificate") },
                    onGoRecords = { username -> navController.navigate("records/${username.ifBlank { "_" }}") },
                    onLogin = { navController.navigate("home") }
                )
            }
            composable("register") {
                RegistrationScreen(
                    factory = registrationFactory,
                    onBackToLogin = { navController.navigate("login") },
                    onViewRecords = { username -> navController.navigate("records/${username.ifBlank { "_" }}") }
                )
            }
            composable("records/{username}") { backStackEntry ->
                val username = backStackEntry.arguments?.getString("username").orEmpty().let { if (it == "_") "" else it }
                RecordsScreen(
                    factory = registrationFactory,
                    username = username,
                    onBack = { navController.navigate("login") },
                    onCreateAnother = { navController.navigate("register") }
                )
            }
            composable("home") {
                InternalSystemScreen(
                    profile = profileUi,
                    appsCountLabel = "${apps.size} 个",
                    actionInfo = actionInfo,
                    actionError = actionError,
                    profileEditDialogVisible = showProfileEditDialog,
                    profileEditSaving = profileEditSaving,
                    profileEditError = profileEditError,
                    onLogout = {
                        scope.launch {
                            authRepository.logout()
                            navController.navigate("login")
                        }
                    },
                    onAccessApp = { title ->
                        scope.launch {
                            loadAppsIfNeeded()
                            val appTarget = resolveAppByTitle(title)
                            if (appTarget == null) {
                                actionError = "暂无可访问应用"
                                return@launch
                            }
                            when (val trust = accessRepository.checkTrust(appTarget)) {
                                is ApiResult.Failure -> {
                                    actionError = trust.message
                                }
                                is ApiResult.Success -> {
                                    val trustLevel = trust.data.trustLevel ?: 0
                                    val authMethod = session?.authMethod ?: "PASSWORD_LOGIN"
                                    val decision = accessRepository.evaluatePolicy(appTarget, authMethod, trustLevel)
                                    if (!decision.allowed) {
                                        actionInfo = "应用访问需二次认证：${decision.reason}"
                                        actionError = null
                                        return@launch
                                    }
                                    when (val access = accessRepository.ssoAccess(appTarget)) {
                                        is ApiResult.Success -> {
                                            actionInfo = if (access.data.success) {
                                                "应用访问成功：${appTarget.appCode}"
                                            } else {
                                                "应用访问失败：${access.data.message}"
                                            }
                                            actionError = null
                                        }
                                        is ApiResult.Failure -> actionError = access.message
                                    }
                                }
                            }
                        }
                    },
                    onRunTrustCheck = {
                        scope.launch {
                            when (val result = trustGuardRepository.evaluateAndUpdate()) {
                                is ApiResult.Success -> {
                                    actionInfo = "信任评估完成：${if (result.data.blocked) "已阻断" else "通过"}"
                                    actionError = null
                                }
                                is ApiResult.Failure -> actionError = result.message
                            }
                        }
                    },
                    onRunReleaseCheck = {
                        scope.launch {
                            val checks = releaseRepository.runAllChecks()
                            val pass = checks.count { it.passed }
                            actionInfo = "系统联调检查：通过 $pass/${checks.size}"
                            actionError = null
                        }
                    },
                    onSyncPolicy = {
                        scope.launch {
                            when (val result = policyRepository.syncPolicies()) {
                                is ApiResult.Success -> {
                                    actionInfo = "策略同步成功：${result.data.policies.size}条"
                                    actionError = null
                                }
                                is ApiResult.Failure -> actionError = result.message
                            }
                        }
                    },
                    onRunSecurityCheck = {
                        scope.launch {
                            terminalMonitorRepository.collectAndQueue()
                            val flush = terminalMonitorRepository.flushQueue()
                            val trust = trustGuardRepository.evaluateAndUpdate()
                            val flushed = (flush as? ApiResult.Success)?.data ?: 0
                            val blocked = (trust as? ApiResult.Success)?.data?.blocked
                            actionInfo = "安全检测完成：上报${flushed}条，信任阻断=${blocked ?: "-"}"
                            actionError = if (flush is ApiResult.Failure) flush.message else if (trust is ApiResult.Failure) trust.message else null
                        }
                    },
                    onManageFingerprint = {
                        scope.launch { enrollOrRecollectFingerprint() }
                    },
                    onManageFace = {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    },
                    onManageVoiceprint = {
                        audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    },
                    onGoCertificateCenter = { navController.navigate("certificate") },
                    onOpenProfileEdit = {
                        profileEditError = null
                        showProfileEditDialog = true
                    },
                    onDismissProfileEdit = {
                        profileEditError = null
                        showProfileEditDialog = false
                    },
                    onSubmitProfileEdit = { realName, phone, email, idCardNo ->
                        scope.launch {
                            val emailInput = email.trim()
                            if (emailInput.isNotBlank() && !Patterns.EMAIL_ADDRESS.matcher(emailInput).matches()) {
                                profileEditError = "邮箱格式不正确"
                                return@launch
                            }
                            profileEditError = null
                            profileEditSaving = true
                            when (
                                val result = userProfileRepository.updateMe(
                                    UserProfileUpdateRequest(
                                        realName = realName.trim(),
                                        phone = phone.trim().ifBlank { null },
                                        email = emailInput.ifBlank { null },
                                        idCardNo = idCardNo.trim().ifBlank { null }
                                    )
                                )
                            ) {
                                is ApiResult.Success -> {
                                    profileUi = mergeProfile(session, result.data)
                                    actionInfo = "资料更新成功"
                                    actionError = null
                                    profileEditError = null
                                    showProfileEditDialog = false
                                }
                                is ApiResult.Failure -> {
                                    profileEditError = result.message
                                }
                            }
                            profileEditSaving = false
                        }
                    }
                )
            }
            composable("certificate") { CertificateScreen(factory = certificateFactory, onBack = { navController.navigate("login") }) }
        }
    }
}
