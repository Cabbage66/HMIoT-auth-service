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
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.identitylogin.InternalIdentityUiData
import com.example.identitylogin.SecurityCategoryUiData
import com.example.identitylogin.SecurityDetailRowUiData
import com.example.identitylogin.SecurityDetailSectionUiData
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
    var loggedSessionKey by remember { mutableStateOf<String?>(null) }

    suspend fun loadAppsIfNeeded() {
        if (session == null) return
        if (apps.isNotEmpty()) return
        when (val result = accessRepository.listApps()) {
            is ApiResult.Success -> {
                apps.clear()
                apps.addAll(result.data)
                actionInfo = "应用已加载：${apps.size}个"
                actionError = null
            }
            is ApiResult.Failure -> {
                actionError = mainMessage(result.message)
            }
        }
    }

    suspend fun runSecurityEvaluation(
        triggerScene: String,
        appCode: String? = null,
        showSuccess: Boolean = true
    ): Boolean {
        return try {
            val snapshot = terminalMonitorRepository.collectSecuritySnapshot()
            securityCategory = securityCategoryFromSnapshot(snapshot)
            val queued = terminalMonitorRepository.collectAndQueue(triggerScene, appCode)
            val flush = terminalMonitorRepository.flushQueue()
            val collectMessage = when {
                flush is ApiResult.Failure -> "安全信息已更新，服务端同步稍后重试"
                queued is ApiResult.Success -> "安全信息已更新，日志已记录"
                else -> "安全信息已更新"
            }
            actionInfo = collectMessage
            actionError = null
            when (val result = trustGuardRepository.evaluateAndUpdate(triggerScene, appCode, collectBeforeCheck = false)) {
                is ApiResult.Success -> {
                    if (showSuccess) {
                        actionInfo = if (result.data.blocked) {
                            "$collectMessage，检测到风险，请按提示处理"
                        } else {
                            "$collectMessage，当前状态正常"
                        }
                    }
                    actionError = null
                    true
                }
                is ApiResult.Failure -> {
                    actionInfo = "$collectMessage，信任评估服务暂不可用"
                    actionError = null
                    false
                }
            }
        } catch (ex: Exception) {
            actionInfo = null
            actionError = "安全信息采集失败：${mainMessage(ex.message)}"
            false
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

    LaunchedEffect(session?.token, session?.terminalCode) {
        if (session == null) return@LaunchedEffect
        loadAppsIfNeeded()
        val key = "${session?.userId}:${session?.terminalCode}:${session?.token?.take(8)}"
        if (loggedSessionKey != key) {
            loggedSessionKey = key
            runSecurityEvaluation("LOGIN", showSuccess = false)
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
                            terminalMonitorRepository.collectAndQueue("APP_ACCESS", appTarget.appCode)
                            terminalMonitorRepository.flushQueue()
                            val guard = when (val evaluated = trustGuardRepository.evaluateAndUpdate("APP_ACCESS", appTarget.appCode, collectBeforeCheck = false)) {
                                is ApiResult.Success -> evaluated.data
                                is ApiResult.Failure -> {
                                    actionInfo = null
                                    actionError = mainMessage(evaluated.message)
                                    return@launch
                                }
                            }
                            securityCategory = securityCategoryFromSnapshot(terminalMonitorRepository.collectSecuritySnapshot())
                            val trustLevel = guard.trustLevel ?: 0
                            val authMethod = session?.authMethod ?: "PASSWORD_LOGIN"
                            val decision = accessRepository.evaluatePolicy(appTarget, authMethod, trustLevel)
                            if (!decision.allowed) {
                                actionInfo = "应用访问需二次认证：${mainMessage(decision.reason)}"
                                actionError = null
                                return@launch
                            }
                            when (val access = accessRepository.ssoAccess(appTarget)) {
                                is ApiResult.Success -> {
                                    actionInfo = if (access.data.success) {
                                        "应用访问成功：${appTarget.appCode}"
                                    } else {
                                        "应用访问失败：${mainMessage(access.data.message)}"
                                    }
                                    actionError = null
                                }
                                is ApiResult.Failure -> {
                                    actionInfo = null
                                    actionError = mainMessage(access.message)
                                }
                            }
                        }
                    },
                    onRunTrustCheck = {
                        scope.launch {
                            runSecurityEvaluation("MANUAL_TRUST_CHECK")
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
                                is ApiResult.Failure -> actionError = mainMessage(result.message)
                            }
                        }
                    },
                    onRunSecurityCheck = {
                        scope.launch {
                            runSecurityEvaluation("SECURITY_PAGE")
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
    val levelColor = if (blocked || risk == "HIGH") Color(0xFFC92A2A)
    else if (risk == "MEDIUM") Color(0xFFB45309)
    else Color(0xFF087F5B)
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
    val bootloaderUnlocked = sw["isBootloaderUnlocked"] == true
    val emulator = sw["isEmulator"] == true
    val adb = sw["isAdbEnabled"] == true
    val hook = sw["hasHookRisk"] == true
    val highRiskApps = sw["hasHighRiskApps"] == true
    val swBadge = if (rooted || bootloaderUnlocked || emulator || hook || highRiskApps) "高风险" else if (adb || sw["isVpnActive"] == true || sw["allowUnknownSources"] == true) "告警" else "正常"
    val hwBadge = if (hw["secureHardware"] == true) "可信" else "一般"
    val swDesc = "Root ${yesNo(rooted)}，ADB ${yesNo(adb)}，Hook ${yesNo(hook)}，高风险应用 ${yesNo(highRiskApps)}"
    val hwDesc = "指纹 ${supportText(hw["fingerprintSupported"])}，人脸 ${supportText(hw["faceSupported"])}，声纹 ${supportText(hw["voiceprintSupported"])}，安全硬件 ${supportText(hw["secureHardware"])}"
    val swLines = listOf(
        "Root=${yesNo(rooted)}, Bootloader=${bootloaderText(sw["isBootloaderUnlocked"])}, OS签名=${trustedText(sw["isOsSignatureTrusted"])}",
        "模拟器=${yesNo(emulator)}, ADB=${yesNo(adb)}, Hook=${yesNo(hook)}",
        "网络=${valueText(sw["connectionType"])}, VPN=${yesNo(sw["isVpnActive"])}, Wi-Fi安全=${nullableYesNo(sw["isWifiSecured"], "非 Wi-Fi/系统未开放")}",
        "高风险应用=${yesNo(highRiskApps)}, 应用签名=${trustedText(sw["isAppSignatureTrusted"])}, 未知来源=${yesNo(sw["allowUnknownSources"])}"
    )
    val hwLines = listOf(
        "设备ID=${shortValue(hw["deviceId"])}, AndroidID=${shortValue(hw["androidId"])}",
        "版本=${valueText(hw["appVersion"])}, IP=${valueText(hw["clientIp"])}, 位置=${valueText(hw["location"], "未授权/未采集")}",
        "指纹=${supportText(hw["fingerprintSupported"])}, 人脸=${supportText(hw["faceSupported"])}, 声纹=${supportText(hw["voiceprintSupported"])}"
    )
    val sections = listOf(
        SecurityDetailSectionUiData(
            title = "系统完整性",
            badge = if (rooted || bootloaderUnlocked) "风险" else "正常",
            status = if (rooted || bootloaderUnlocked) "DANGER" else "SUCCESS",
            rows = listOf(
                SecurityDetailRowUiData("Root 状态", yesNo(rooted)),
                SecurityDetailRowUiData("Bootloader", bootloaderText(sw["isBootloaderUnlocked"])),
                SecurityDetailRowUiData("OS 签名", trustedText(sw["isOsSignatureTrusted"]))
            )
        ),
        SecurityDetailSectionUiData(
            title = "运行环境风险",
            badge = if (emulator || adb || hook) "告警" else "正常",
            status = if (emulator || hook) "DANGER" else if (adb) "WARN" else "SUCCESS",
            rows = listOf(
                SecurityDetailRowUiData("模拟器", yesNo(emulator)),
                SecurityDetailRowUiData("ADB 调试", yesNo(adb)),
                SecurityDetailRowUiData("Hook 风险", yesNo(hook))
            )
        ),
        SecurityDetailSectionUiData(
            title = "系统基线",
            badge = "已采集",
            status = "SUCCESS",
            rows = listOf(
                SecurityDetailRowUiData("API Level", valueText(sw["apiLevel"])),
                SecurityDetailRowUiData("安全补丁", valueText(sw["securityPatch"], "未知")),
                SecurityDetailRowUiData("系统版本", valueText(sw["osRelease"], "未知"))
            )
        ),
        SecurityDetailSectionUiData(
            title = "网络安全",
            badge = if (sw["isVpnActive"] == true || sw["isWifiSecured"] == false) "告警" else "正常",
            status = if (sw["isVpnActive"] == true || sw["isWifiSecured"] == false) "WARN" else "SUCCESS",
            rows = listOf(
                SecurityDetailRowUiData("连接类型", valueText(sw["connectionType"])),
                SecurityDetailRowUiData("VPN", yesNo(sw["isVpnActive"])),
                SecurityDetailRowUiData("Wi-Fi 加密", nullableYesNo(sw["isWifiSecured"], "非 Wi-Fi/系统未开放"))
            )
        ),
        SecurityDetailSectionUiData(
            title = "应用风险",
            badge = if (highRiskApps || sw["isAppSignatureTrusted"] != true || sw["allowUnknownSources"] == true) "告警" else "正常",
            status = if (highRiskApps || sw["isAppSignatureTrusted"] != true || sw["allowUnknownSources"] == true) "WARN" else "SUCCESS",
            rows = listOf(
                SecurityDetailRowUiData("高风险应用", yesNo(highRiskApps)),
                SecurityDetailRowUiData("应用签名", trustedText(sw["isAppSignatureTrusted"])),
                SecurityDetailRowUiData("允许未知来源", yesNo(sw["allowUnknownSources"]))
            )
        ),
        SecurityDetailSectionUiData(
            title = "设备身份与上下文",
            badge = "已采集",
            status = "SUCCESS",
            rows = listOf(
                SecurityDetailRowUiData("设备标识", shortValue(hw["deviceIdentifier"])),
                SecurityDetailRowUiData("设备 ID", shortValue(hw["deviceId"])),
                SecurityDetailRowUiData("Android ID", shortValue(hw["androidId"])),
                SecurityDetailRowUiData("应用版本", valueText(hw["appVersion"])),
                SecurityDetailRowUiData("设备指纹", shortValue(hw["deviceFingerprint"], 24)),
                SecurityDetailRowUiData("客户端 IP", valueText(hw["clientIp"], "未获取")),
                SecurityDetailRowUiData("位置", valueText(hw["location"], "未授权/未采集"))
            )
        ),
        SecurityDetailSectionUiData(
            title = "生物能力",
            badge = hwBadge,
            status = if (hw["secureHardware"] == true) "SUCCESS" else "WARN",
            rows = listOf(
                SecurityDetailRowUiData("指纹", supportText(hw["fingerprintSupported"])),
                SecurityDetailRowUiData("人脸", supportText(hw["faceSupported"])),
                SecurityDetailRowUiData("声纹", supportText(hw["voiceprintSupported"])),
                SecurityDetailRowUiData("安全硬件", supportText(hw["secureHardware"])),
                SecurityDetailRowUiData("本地人脸比对", supportText(hw["localFaceCompare"]))
            )
        )
    )
    return SecurityCategoryUiData(
        softwareBadge = swBadge,
        softwareDesc = swDesc,
        hardwareBadge = hwBadge,
        hardwareDesc = hwDesc,
        softwareLines = swLines,
        hardwareLines = hwLines,
        detailSections = sections
    )
}

private fun yesNo(value: Any?): String = if (value == true) "是" else "否"

private fun nullableYesNo(value: Any?, unknown: String): String = when (value) {
    true -> "是"
    false -> "否"
    else -> unknown
}

private fun supportText(value: Any?): String = if (value == true) "支持" else "不支持"

private fun trustedText(value: Any?): String = if (value == true) "可信" else "不可信"

private fun bootloaderText(value: Any?): String = when (value) {
    true -> "已解锁"
    false -> "已锁定"
    else -> "系统未开放"
}

private fun valueText(value: Any?, fallback: String = "未获取"): String {
    val text = value?.toString().orEmpty()
    return text.ifBlank { fallback }
}

private fun shortValue(value: Any?, maxLength: Int = 18): String {
    val text = valueText(value)
    if (text.length <= maxLength) return text
    val left = (maxLength - 3) / 2
    val right = maxLength - 3 - left
    return text.take(left) + "..." + text.takeLast(right)
}

private fun mainMessage(message: String?): String {
    val raw = message.orEmpty().ifBlank { return "请求失败，请稍后重试" }
    val withoutTrace = raw
        .replace(Regex("traceId=[A-Za-z0-9_\\-]+"), "")
        .replace(Regex("Failed to connect to\\s*"), "无法连接服务端 ")
        .replace(Regex("HTTP\\s*\\d{3}", RegexOption.IGNORE_CASE), "服务端暂不可用")
        .replace(Regex("java\\.[A-Za-z0-9_.: ]+"), "")
        .trim()
    return withoutTrace
        .lineSequence()
        .firstOrNull { it.isNotBlank() }
        ?.take(80)
        ?: "请求失败，请稍后重试"
}
