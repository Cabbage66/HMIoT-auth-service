package com.hmiot.app

import android.os.Bundle
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
import com.example.identitylogin.SecurityCategoryUiData
import com.example.identitylogin.InternalSystemScreen
import com.example.identitylogin.TrustDashboardUiData
import com.hmiot.app.core.network.model.ApiResult
import com.hmiot.app.feature.auth.ui.LoginScreen
import com.hmiot.app.feature.auth.ui.LoginViewModelFactory
import com.hmiot.app.feature.access.data.AppResource
import com.hmiot.app.feature.certificate.ui.CertificateScreen
import com.hmiot.app.feature.certificate.ui.CertificateViewModelFactory
import com.hmiot.app.feature.registration.ui.RegistrationScreen
import com.hmiot.app.feature.registration.ui.RegistrationViewModelFactory
import com.hmiot.app.feature.registration.ui.RecordsScreen
import com.hmiot.app.ui.theme.HMIoTAppTheme
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

@Composable
private fun AppEntry() {
    val app = androidx.compose.ui.platform.LocalContext.current.applicationContext as HmiotApplication
    val authRepository = app.appContainer.authRepository
    val registrationRepository = app.appContainer.registrationRepository
    val certificateProfileRepository = app.appContainer.certificateProfileRepository
    val terminalMonitorRepository = app.appContainer.terminalMonitorRepository
    val policyRepository = app.appContainer.policyRepository
    val accessRepository = app.appContainer.accessRepository
    val trustGuardRepository = app.appContainer.trustGuardRepository
    val releaseRepository = app.appContainer.releaseValidationRepository

    val loginFactory = LoginViewModelFactory(authRepository)
    val registrationFactory = RegistrationViewModelFactory(registrationRepository)
    val certificateFactory = CertificateViewModelFactory(certificateProfileRepository)

    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    val session by authRepository.session.collectAsState(initial = null)
    val trustGuardState by trustGuardRepository.state.collectAsState(
        initial = com.hmiot.app.core.storage.datastore.TrustGuardState(
            blocked = false,
            reason = "",
            updatedAt = "",
            trustScore = null,
            trustLevel = null,
            riskLevel = null,
            decision = null,
            requestId = null,
            requestLogPath = null,
            resultLogPath = null
        )
    )
    val apps = remember { mutableStateListOf<AppResource>() }
    var actionInfo by remember { mutableStateOf<String?>(null) }
    var actionError by remember { mutableStateOf<String?>(null) }
    var securityCategory by remember { mutableStateOf(SecurityCategoryUiData()) }

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

    LaunchedEffect(Unit) {
        loadAppsIfNeeded()
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
                    profile = InternalIdentityUiData(
                        username = session?.username ?: "zhangsan",
                        realName = session?.realName ?: "张三",
                        certificateNo = session?.let { "TERMINAL-${it.terminalCode}" } ?: "CERT-202605290001"
                    ),
                    appsCountLabel = "${apps.size} 个",
                    trustDashboard = trustDashboardFromState(trustGuardState),
                    securityCategory = securityCategory,
                    actionInfo = actionInfo,
                    actionError = actionError,
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
                            val snapshot = terminalMonitorRepository.collectSecuritySnapshot()
                            securityCategory = securityCategoryFromSnapshot(snapshot)
                            terminalMonitorRepository.collectAndQueue()
                            val flush = terminalMonitorRepository.flushQueue()
                            val trust = trustGuardRepository.evaluateAndUpdate()
                            val flushed = (flush as? ApiResult.Success)?.data ?: 0
                            val blocked = (trust as? ApiResult.Success)?.data?.blocked
                            actionInfo = "安全检测完成：上报${flushed}条，信任阻断=${blocked ?: "-"}"
                            actionError = if (flush is ApiResult.Failure) flush.message else if (trust is ApiResult.Failure) trust.message else null
                        }
                    },
                    onBiometricTip = {
                        actionInfo = "生物特征功能已接入登录与注册流程。"
                        actionError = null
                    },
                    onGoCertificateCenter = { navController.navigate("certificate") }
                )
            }
            composable("certificate") { CertificateScreen(factory = certificateFactory, onBack = { navController.navigate("login") }) }
        }
    }
}

private fun trustDashboardFromState(state: com.hmiot.app.core.storage.datastore.TrustGuardState): TrustDashboardUiData {
    val score = (state.trustScore ?: 88.0).toInt().coerceIn(0, 100)
    val risk = (state.riskLevel ?: "LOW").uppercase()
    val blocked = state.blocked
    val levelLabel = when (state.trustLevel ?: 0) {
        3 -> "高信任等级"
        2 -> "中信任等级"
        1 -> "低信任等级"
        else -> if (blocked) "风险阻断" else "待评估"
    }
    val levelColor = if (blocked || risk == "HIGH") androidx.compose.ui.graphics.Color(0xFFC92A2A)
    else if (risk == "MEDIUM") androidx.compose.ui.graphics.Color(0xFFB45309)
    else androidx.compose.ui.graphics.Color(0xFF087F5B)
    val levelBg = levelColor.copy(alpha = 0.12f)
    val summary = if (blocked) {
        "当前终端处于风险阻断状态，请完成二次认证或联系管理员。"
    } else {
        "当前设备、账号与生物特征状态稳定，可访问已授权应用。"
    }
    return TrustDashboardUiData(
        trustScore = score,
        trustLevelLabel = levelLabel,
        trustLevelColor = levelColor,
        trustLevelBg = levelBg,
        riskLevel = risk,
        decision = state.decision ?: if (blocked) "DENY" else "ALLOW",
        evaluatedAtText = if (state.updatedAt.isBlank()) "未评估" else state.updatedAt,
        summaryText = summary,
        deviceTrustText = "${score.coerceIn(1, 99)}%",
        behaviorText = "${((state.trustLevel ?: 0) * 28 + 16).coerceIn(1, 99)}%",
        requestIdText = state.requestId.orEmpty(),
        resultLogPathText = state.resultLogPath.orEmpty()
    )
}

private fun securityCategoryFromSnapshot(snapshot: com.hmiot.app.feature.terminal.domain.TerminalSecuritySnapshot): SecurityCategoryUiData {
    val sw = snapshot.software
    val hw = snapshot.hardware
    val rooted = sw["isRooted"] == true
    val emulator = sw["isEmulator"] == true
    val adb = sw["isAdbEnabled"] == true
    val hook = sw["hasHookRisk"] == true
    val swBadge = if (rooted || emulator || hook) "高风险" else if (adb) "告警" else "正常"
    val hwBadge = if (hw["secureHardware"] == true) "可信" else "一般"
    val swDesc = "Root=$rooted, 模拟器=$emulator, ADB=$adb, Hook=$hook"
    val hwDesc = "指纹=${hw["fingerprintSupported"]}, 人脸=${hw["faceSupported"]}, 声纹=${hw["voiceprintSupported"]}, 安全硬件=${hw["secureHardware"]}"
    val swLines = listOf(
        "系统完整性: rooted=$rooted, os签名可信=${sw["isOsSignatureTrusted"]}",
        "运行环境: emulator=$emulator, adb=$adb, hookRisk=$hook",
        "网络安全: ${sw["connectionType"]}, vpn=${sw["isVpnActive"]}, wifiSecured=${sw["isWifiSecured"]}",
        "应用风险: highRiskApps=${sw["hasHighRiskApps"]}, appSig=${sw["isAppSignatureTrusted"]}, unknownSources=${sw["allowUnknownSources"]}"
    )
    val hwLines = listOf(
        "设备标识: deviceId=${hw["deviceId"]}, androidId=${hw["androidId"]}",
        "终端上下文: appVersion=${hw["appVersion"]}, ip=${hw["clientIp"]}, location=${hw["location"]}",
        "生物能力: fp=${hw["fingerprintSupported"]}, face=${hw["faceSupported"]}, voice=${hw["voiceprintSupported"]}"
    )
    return SecurityCategoryUiData(
        softwareBadge = swBadge,
        softwareDesc = swDesc,
        hardwareBadge = hwBadge,
        hardwareDesc = hwDesc,
        softwareLines = swLines,
        hardwareLines = hwLines
    )
}
