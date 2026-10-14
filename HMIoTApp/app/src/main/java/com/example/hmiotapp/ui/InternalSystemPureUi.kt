package com.example.identitylogin

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val LightTextMain = Color(0xFF121826)
private val LightTextMuted = Color(0xFF8B92A1)
private val AppBlue = Color(0xFF12A8FF)
private val AppBlueDark = Color(0xFF0587DC)
private val Green = Color(0xFF20C997)
private val Orange = Color(0xFFF59E0B)
private val Red = Color(0xFFEF4444)
private val Purple = Color(0xFF7C3AED)
private val LightLine = Color(0xFFEEF2F7)

private data class InternalSystemColors(
    val backgroundBrush: Brush,
    val surface: Color,
    val panel: Color,
    val nestedPanel: Color,
    val line: Color,
    val textMain: Color,
    val textMuted: Color,
    val textSecondary: Color,
    val statusIcon: Color,
    val scoreInner: Color,
    val scoreTrack: Color
)

@Composable
private fun internalSystemColors(): InternalSystemColors {
    val dark = isSystemInDarkTheme()
    return if (dark) {
        InternalSystemColors(
            backgroundBrush = Brush.verticalGradient(
                0f to Color(0xFF07111F),
                0.48f to Color(0xFF0B1624),
                1f to Color(0xFF101826)
            ),
            surface = Color(0xFF07111F),
            panel = Color(0xE61A2636),
            nestedPanel = Color(0xFF111D2B),
            line = Color(0xFF27364A),
            textMain = Color(0xFFF4F7FB),
            textMuted = Color(0xFFA8B3C7),
            textSecondary = Color(0xFFC5CEDD),
            statusIcon = Color(0xFFE6EDF7),
            scoreInner = Color(0xFF0F1B2A),
            scoreTrack = Color(0xFF2A3950)
        )
    } else {
        InternalSystemColors(
            backgroundBrush = Brush.verticalGradient(
                0f to Color(0xFFFBFCFF),
                0.40f to Color.White,
                1f to Color(0xFFF4F7FB)
            ),
            surface = Color.White,
            panel = Color.White.copy(alpha = 0.86f),
            nestedPanel = Color(0xFFF7FAFC).copy(alpha = 0.78f),
            line = LightLine,
            textMain = LightTextMain,
            textMuted = LightTextMuted,
            textSecondary = Color(0xFF667085),
            statusIcon = Color(0xFF1D2433),
            scoreInner = Color.White.copy(alpha = 0.96f),
            scoreTrack = Color(0xFFE4E9F2)
        )
    }
}

@Composable private fun textMainColor(): Color = internalSystemColors().textMain
@Composable private fun textMutedColor(): Color = internalSystemColors().textMuted
@Composable private fun textSecondaryColor(): Color = internalSystemColors().textSecondary
@Composable private fun lineColor(): Color = internalSystemColors().line
@Composable private fun panelColor(): Color = internalSystemColors().panel
@Composable private fun nestedPanelColor(): Color = internalSystemColors().nestedPanel
@Composable private fun statusIconColor(): Color = internalSystemColors().statusIcon

data class InternalIdentityUiData(
    val username: String = "zhangsan",
    val realName: String = "张三",
    val phoneMasked: String = "138****8899",
    val emailMasked: String = "zhangsan@example.com",
    val idCardMasked: String = "110***********001",
    val certificateNo: String = "CERT-202605290001"
)

data class TrustDashboardUiData(
    val trustScore: Int = 88,
    val trustLevelLabel: String = "高信任等级",
    val trustLevelColor: Color = Color(0xFF087F5B),
    val trustLevelBg: Color = Green.copy(alpha = 0.12f),
    val riskLevel: String = "LOW",
    val decision: String = "ALLOW",
    val evaluatedAtText: String = "刚刚评估",
    val summaryText: String = "当前设备、账号与生物特征状态稳定，可访问已授权应用。",
    val deviceTrustText: String = "96%",
    val behaviorText: String = "82%",
    val requestIdText: String = "",
    val resultLogPathText: String = ""
)

data class SecurityCategoryUiData(
    val softwareBadge: String = "正常",
    val softwareDesc: String = "应用签名、运行版本、运行环境均通过检测。",
    val hardwareBadge: String = "可信",
    val hardwareDesc: String = "设备指纹、传感器、安全芯片与本机标识可信。",
    val softwareLines: List<String> = emptyList(),
    val hardwareLines: List<String> = emptyList(),
    val detailSections: List<SecurityDetailSectionUiData> = emptyList()
)

data class SecurityDetailSectionUiData(
    val title: String,
    val badge: String,
    val status: String,
    val rows: List<SecurityDetailRowUiData>
)

data class SecurityDetailRowUiData(
    val label: String,
    val value: String
)

@Composable
fun InternalSystemScreen(
    profile: InternalIdentityUiData = InternalIdentityUiData(),
    appsCountLabel: String = "8 个",
    trustDashboard: TrustDashboardUiData = TrustDashboardUiData(),
    securityCategory: SecurityCategoryUiData = SecurityCategoryUiData(),
    actionInfo: String? = null,
    actionError: String? = null,
    onLogout: () -> Unit,
    onAccessApp: (String) -> Unit,
    onRunTrustCheck: () -> Unit,
    onRunReleaseCheck: () -> Unit,
    onSyncPolicy: () -> Unit,
    onRunSecurityCheck: () -> Unit,
    onBiometricTip: () -> Unit,
    onGoCertificateCenter: () -> Unit
) {
    var tab by remember { mutableStateOf("首页") }
    InternalSystemPureUiApp {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f)) {
                when (tab) {
                    "首页" -> HomePageUi(
                        username = profile.username,
                        trustDashboard = trustDashboard,
                        securityCategory = securityCategory,
                        actionInfo = actionInfo,
                        actionError = actionError,
                        onLogout = onLogout,
                        onAccessApp = onAccessApp,
                        onRunSecurityCheck = onRunSecurityCheck,
                        onBiometricTip = onBiometricTip
                    )
                    "应用" -> AppsPageUi(
                        appsCountLabel = appsCountLabel,
                        actionInfo = actionInfo,
                        actionError = actionError,
                        onAccessApp = onAccessApp,
                        onSyncPolicy = onSyncPolicy,
                        onRunReleaseCheck = onRunReleaseCheck
                    )
                    "安全" -> SecurityPageUi(
                        securityCategory = securityCategory,
                        actionInfo = actionInfo,
                        actionError = actionError,
                        onRunSecurityCheck = onRunSecurityCheck,
                        onRunTrustCheck = onRunTrustCheck
                    )
                    else -> ProfilePageUi(
                        profile = profile,
                        actionInfo = actionInfo,
                        actionError = actionError,
                        onRunReleaseCheck = onRunReleaseCheck,
                        onBiometricTip = onBiometricTip,
                        onGoCertificateCenter = onGoCertificateCenter
                    )
                }
            }
            BottomNavigationUi(selectedTab = tab, onTabChange = { tab = it })
        }
    }
}

/**
 * 纯 UI 文件：
 * - 不包含业务逻辑
 * - 不包含真实页面跳转
 * - 所有按钮仅为空点击占位
 * - 通过底部 Preview 可分别查看：首页、应用、安全、我的
 */
@Composable
private fun InternalSystemPureUiApp(content: @Composable BoxScope.() -> Unit) {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = internalSystemColors().surface) {
            AppBackground {
                content()
            }
        }
    }
}

@Composable
private fun AppBackground(content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(internalSystemColors().backgroundBrush)
    ) {
        SoftGlow(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = (-88).dp, y = 80.dp)
                .size(210.dp),
            color = AppBlue.copy(alpha = if (isSystemInDarkTheme()) 0.08f else 0.13f)
        )

        SoftGlow(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 96.dp, y = 220.dp)
                .size(190.dp),
            color = Color(0xFFB5E4FF).copy(alpha = if (isSystemInDarkTheme()) 0.10f else 0.34f)
        )

        SoftGlow(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = 26.dp, y = (-28).dp)
                .size(190.dp),
            color = AppBlue.copy(alpha = if (isSystemInDarkTheme()) 0.06f else 0.09f)
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 22.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                FakeStatusBar()
                Box(modifier = Modifier.fillMaxSize(), content = content)
            }
        }
    }
}

@Composable
private fun SoftGlow(modifier: Modifier, color: Color) {
    Box(
        modifier = modifier
            .blur(34.dp)
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
private fun FakeStatusBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp)
            .padding(horizontal = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("9:41", fontSize = 13.sp, color = statusIconColor(), fontWeight = FontWeight.Bold)

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.height(12.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                Box(Modifier.width(3.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(statusIconColor()))
                Box(Modifier.width(3.dp).height(7.dp).clip(RoundedCornerShape(2.dp)).background(statusIconColor()))
                Box(Modifier.width(3.dp).height(10.dp).clip(RoundedCornerShape(2.dp)).background(statusIconColor()))
            }

            Text("5G", fontSize = 13.sp, color = statusIconColor(), fontWeight = FontWeight.Bold)
            BatteryIcon()
        }
    }
}

@Composable
private fun BatteryIcon() {
    val color = statusIconColor()
    Canvas(modifier = Modifier.size(width = 26.dp, height = 13.dp)) {
        drawRoundRect(
            color = color,
            topLeft = Offset(0f, 1.dp.toPx()),
            size = Size(22.dp.toPx(), 11.dp.toPx()),
            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()),
            style = Stroke(width = 1.8.dp.toPx())
        )
        drawRoundRect(
            color = color,
            topLeft = Offset(22.dp.toPx(), 4.dp.toPx()),
            size = Size(2.dp.toPx(), 5.dp.toPx()),
            cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
        )
    }
}

@Composable
private fun PageScaffold(content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 6.dp, vertical = 0.dp),
            content = content
        )
    }
}

@Composable
private fun HomePageUi(
    username: String,
    trustDashboard: TrustDashboardUiData,
    securityCategory: SecurityCategoryUiData,
    actionInfo: String?,
    actionError: String?,
    onLogout: () -> Unit,
    onAccessApp: (String) -> Unit,
    onRunSecurityCheck: () -> Unit,
    onBiometricTip: () -> Unit
) {
    PageScaffold {
        TopTitleBar(
            title = "身份信任中心",
            subtitle = "$username · 在线",
            buttonText = "退出",
            buttonDanger = true,
            onClick = onLogout
        )

        TrustDashboardCard(trustDashboard)
        ActionTip(actionInfo, actionError)

        SectionTitleBar(title = "当前系统安全情况", note = "${securityCategory.detailSections.size.coerceAtLeast(2)} 项检测")

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SecurityStatusCard(
                title = "软件状态",
                description = securityCategory.softwareDesc,
                badge = securityCategory.softwareBadge,
                iconColor = AppBlueDark,
                iconBg = AppBlue.copy(alpha = 0.10f),
                modifier = Modifier.weight(1f)
                    .clickable { onRunSecurityCheck() }
            ) {
                SoftwareIcon(Modifier.size(23.dp), AppBlueDark)
            }

            SecurityStatusCard(
                title = "硬件状态",
                description = securityCategory.hardwareDesc,
                badge = securityCategory.hardwareBadge,
                iconColor = Color(0xFF10B981),
                iconBg = Color(0xFF10B981).copy(alpha = 0.10f),
                modifier = Modifier.weight(1f)
                    .clickable { onRunSecurityCheck() }
            ) {
                HardwareIcon(Modifier.size(23.dp), Color(0xFF10B981))
            }
        }

        SectionTitleBar(title = "可访问应用", note = "内置应用")

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            AppItem("公文", AppBlueDark, Modifier.weight(1f), onClick = { onAccessApp("公文") }) { DocumentIcon(Modifier.size(25.dp), AppBlueDark) }
            AppItem("业务", Purple, Modifier.weight(1f), onClick = { onAccessApp("业务") }) { BriefcaseIcon(Modifier.size(25.dp), Purple) }
            AppItem("档案", Orange, Modifier.weight(1f), onClick = { onAccessApp("档案") }) { FileIcon(Modifier.size(25.dp), Orange) }
            AppItem("安全", Color(0xFF10B981), Modifier.weight(1f), onClick = { onAccessApp("安全") }) { ShieldIcon(Modifier.size(25.dp), Color(0xFF10B981)) }
        }

        SectionTitleBar(title = "生物特征信息补充", note = "可补充 / 更改")

        GlassPanel {
            BioRow(
                title = "指纹信息",
                subtitle = "可更改已录入指纹",
                badge = "已录入",
                badgeType = BadgeType.Success,
                iconColor = AppBlueDark,
                iconBg = AppBlue.copy(alpha = 0.10f),
                onClick = onBiometricTip
            ) {
                FingerprintIcon(Modifier.size(23.dp), AppBlueDark)
            }

            Spacer(Modifier.height(10.dp))

            BioRow(
                title = "人脸信息",
                subtitle = "补充人脸后提高认证通过率",
                badge = "待补充",
                badgeType = BadgeType.Warning,
                iconColor = Purple,
                iconBg = Purple.copy(alpha = 0.10f),
                onClick = onBiometricTip
            ) {
                FaceIcon(Modifier.size(23.dp), Purple)
            }

            Spacer(Modifier.height(10.dp))

            BioRow(
                title = "声纹信息",
                subtitle = "可重新采集或补充声纹",
                badge = "已录入",
                badgeType = BadgeType.Success,
                iconColor = Orange,
                iconBg = Orange.copy(alpha = 0.10f),
                onClick = onBiometricTip
            ) {
                VoiceIcon(Modifier.size(23.dp), Orange)
            }
        }

        Spacer(Modifier.height(18.dp))
    }
}

@Composable
private fun AppsPageUi(
    appsCountLabel: String,
    actionInfo: String?,
    actionError: String?,
    onAccessApp: (String) -> Unit,
    onSyncPolicy: () -> Unit,
    onRunReleaseCheck: () -> Unit
) {
    PageScaffold {
        TopTitleBar(title = "可访问应用", subtitle = "基于当前信任等级授权", buttonText = appsCountLabel, onClick = onRunReleaseCheck)
        ActionTip(actionInfo, actionError)

        AppGridAll(onAccessApp)

        Spacer(Modifier.height(18.dp))

        GlassPanel {
            MenuRow(
                title = "应用权限同步",
                subtitle = "根据最新信任等级刷新应用访问权限",
                badge = "已同步",
                badgeType = BadgeType.Success,
                iconColor = AppBlueDark,
                iconBg = AppBlue.copy(alpha = 0.10f),
                onClick = onSyncPolicy
            ) {
                PlusIcon(Modifier.size(23.dp), AppBlueDark)
            }

            Spacer(Modifier.height(10.dp))

            MenuRow(
                title = "访问策略",
                subtitle = "高信任等级可访问全部内置应用",
                badge = "正常",
                badgeType = BadgeType.Success,
                iconColor = Color(0xFF10B981),
                iconBg = Color(0xFF10B981).copy(alpha = 0.10f),
                onClick = onSyncPolicy
            ) {
                ShieldIcon(Modifier.size(23.dp), Color(0xFF10B981))
            }
        }

        Spacer(Modifier.height(18.dp))
    }
}

@Composable
private fun SecurityPageUi(
    securityCategory: SecurityCategoryUiData,
    actionInfo: String?,
    actionError: String?,
    onRunSecurityCheck: () -> Unit,
    onRunTrustCheck: () -> Unit
) {
    var detailType by remember { mutableStateOf<String?>(null) }
    val sections = securitySections(securityCategory)
    val softwareSections = sections.filter { it.isSoftwareSection() }
    val hardwareSections = sections.filterNot { it.isSoftwareSection() }
    if (detailType == "software") {
        SecurityCategoryDetailPage(
            title = "软件状态",
            subtitle = "系统完整性、运行环境、基线、网络与应用风险",
            badge = securityCategory.softwareBadge,
            sections = softwareSections,
            actionInfo = actionInfo,
            actionError = actionError,
            onBack = { detailType = null },
            onRunSecurityCheck = onRunSecurityCheck
        )
        return
    }
    if (detailType == "hardware") {
        SecurityCategoryDetailPage(
            title = "硬件状态",
            subtitle = "设备身份、上下文与生物能力",
            badge = securityCategory.hardwareBadge,
            sections = hardwareSections,
            actionInfo = actionInfo,
            actionError = actionError,
            onBack = { detailType = null },
            onRunSecurityCheck = onRunSecurityCheck
        )
        return
    }

    PageScaffold {
        TopTitleBar(title = "安全状态", subtitle = "软件与硬件可信检测", buttonText = "检测", onClick = onRunSecurityCheck)
        ActionTip(actionInfo, actionError)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SecurityStatusCard(
                title = "软件状态",
                description = securityCategory.softwareDesc,
                badge = securityCategory.softwareBadge,
                iconColor = AppBlueDark,
                iconBg = AppBlue.copy(alpha = 0.10f),
                modifier = Modifier.weight(1f)
                    .clickable {
                        detailType = "software"
                        onRunSecurityCheck()
                    }
            ) {
                SoftwareIcon(Modifier.size(23.dp), AppBlueDark)
            }

            SecurityStatusCard(
                title = "硬件状态",
                description = securityCategory.hardwareDesc,
                badge = securityCategory.hardwareBadge,
                iconColor = Color(0xFF10B981),
                iconBg = Color(0xFF10B981).copy(alpha = 0.10f),
                modifier = Modifier.weight(1f)
                    .clickable {
                        detailType = "hardware"
                        onRunSecurityCheck()
                    }
            ) {
                HardwareIcon(Modifier.size(23.dp), Color(0xFF10B981))
            }
        }

        Spacer(Modifier.height(18.dp))

        GlassPanel {
            MenuRow(
                title = "软件安全明细",
                subtitle = "查看 ${softwareSections.size} 个软件侧采集模块",
                badge = securityCategory.softwareBadge,
                badgeType = badgeTypeFor(securityCategory.softwareBadge),
                iconColor = AppBlueDark,
                iconBg = AppBlue.copy(alpha = 0.10f),
                onClick = {
                    detailType = "software"
                    onRunSecurityCheck()
                }
            ) {
                SoftwareIcon(Modifier.size(23.dp), AppBlueDark)
            }

            Spacer(Modifier.height(10.dp))

            MenuRow(
                title = "硬件安全明细",
                subtitle = "查看 ${hardwareSections.size} 个硬件与设备能力模块",
                badge = securityCategory.hardwareBadge,
                badgeType = badgeTypeFor(securityCategory.hardwareBadge),
                iconColor = Color(0xFF10B981),
                iconBg = Color(0xFF10B981).copy(alpha = 0.10f),
                onClick = {
                    detailType = "hardware"
                    onRunSecurityCheck()
                }
            ) {
                HardwareIcon(Modifier.size(23.dp), Color(0xFF10B981))
            }
        }

        Spacer(Modifier.height(10.dp))

        GlassPanel {
            MenuRow(
                title = "信任评估",
                subtitle = "将最新终端安全信息提交模型评估",
                badge = "评估",
                badgeType = BadgeType.Warning,
                iconColor = Orange,
                iconBg = Orange.copy(alpha = 0.10f),
                onClick = onRunTrustCheck
            ) {
                WarningIcon(Modifier.size(23.dp), Orange)
            }
        }

        Spacer(Modifier.height(18.dp))
    }
}

@Composable
private fun SecurityCategoryDetailPage(
    title: String,
    subtitle: String,
    badge: String,
    sections: List<SecurityDetailSectionUiData>,
    actionInfo: String?,
    actionError: String?,
    onBack: () -> Unit,
    onRunSecurityCheck: () -> Unit
) {
    PageScaffold {
        TopTitleBar(title = title, subtitle = subtitle, buttonText = "返回", onClick = onBack)
        ActionTip(actionInfo, actionError)

        SecuritySummaryPanel(
            title = title,
            subtitle = "已展示 ${sections.size} 个采集模块",
            badge = badge,
            onRefresh = onRunSecurityCheck
        )

        Spacer(Modifier.height(12.dp))

        if (sections.isEmpty()) {
            GlassPanel {
                Text("暂无采集数据", color = textMainColor(), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text("请点击检测后刷新当前分类。", color = textMutedColor(), fontSize = 12.sp, lineHeight = 18.sp)
            }
        } else {
            sections.forEachIndexed { index, section ->
                SecurityDetailSection(section)
                if (index != sections.lastIndex) Spacer(Modifier.height(10.dp))
            }
        }

        Spacer(Modifier.height(18.dp))
    }
}

@Composable
private fun SecuritySummaryPanel(
    title: String,
    subtitle: String,
    badge: String,
    onRefresh: () -> Unit
) {
    GlassPanel {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = textMainColor(), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(5.dp))
                Text(subtitle, color = textMutedColor(), fontSize = 12.sp, lineHeight = 18.sp)
            }
            Badge(badge, badgeTypeFor(badge))
        }

        Spacer(Modifier.height(12.dp))

        MenuRow(
            title = "重新检测",
            subtitle = "重新采集当前终端安全信息并同步日志",
            badge = "刷新",
            badgeType = BadgeType.Warning,
            iconColor = Orange,
            iconBg = Orange.copy(alpha = 0.10f),
            onClick = onRefresh
        ) {
            SyncIcon(Modifier.size(23.dp), Orange)
        }
    }
}

@Composable
private fun ProfilePageUi(
    profile: InternalIdentityUiData,
    actionInfo: String?,
    actionError: String?,
    onRunReleaseCheck: () -> Unit,
    onBiometricTip: () -> Unit,
    onGoCertificateCenter: () -> Unit
) {
    PageScaffold {
        TopTitleBar(title = "我的身份", subtitle = "用户信息与身份凭据", buttonText = "编辑", onClick = onRunReleaseCheck)
        ActionTip(actionInfo, actionError)

        InfoPanel(profile)

        SectionTitleBar(title = "生物特征信息补充", note = "可补充 / 更改")

        GlassPanel {
            BioRow(
                title = "指纹信息",
                subtitle = "可更改已录入指纹",
                badge = "已录入",
                badgeType = BadgeType.Success,
                iconColor = AppBlueDark,
                iconBg = AppBlue.copy(alpha = 0.10f),
                onClick = onBiometricTip
            ) {
                FingerprintIcon(Modifier.size(23.dp), AppBlueDark)
            }

            Spacer(Modifier.height(10.dp))

            BioRow(
                title = "人脸信息",
                subtitle = "补充人脸后提高认证通过率",
                badge = "待补充",
                badgeType = BadgeType.Warning,
                iconColor = Purple,
                iconBg = Purple.copy(alpha = 0.10f),
                onClick = onBiometricTip
            ) {
                FaceIcon(Modifier.size(23.dp), Purple)
            }

            Spacer(Modifier.height(10.dp))

            BioRow(
                title = "声纹信息",
                subtitle = "可重新采集或补充声纹",
                badge = "已录入",
                badgeType = BadgeType.Success,
                iconColor = Orange,
                iconBg = Orange.copy(alpha = 0.10f),
                onClick = onBiometricTip
            ) {
                VoiceIcon(Modifier.size(23.dp), Orange)
            }

            Spacer(Modifier.height(10.dp))

            BioRow(
                title = "数字证书",
                subtitle = "查看或更改本机证书编号",
                badge = "已保存",
                badgeType = BadgeType.Success,
                iconColor = Color(0xFF10B981),
                iconBg = Color(0xFF10B981).copy(alpha = 0.10f),
                onClick = onGoCertificateCenter
            ) {
                CertificateIcon(Modifier.size(23.dp), Color(0xFF10B981))
            }
        }

        Spacer(Modifier.height(18.dp))
    }
}

@Composable
private fun TopTitleBar(
    title: String,
    subtitle: String,
    buttonText: String,
    buttonDanger: Boolean = false,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(title, color = textMainColor(), fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(5.dp))
            Text(subtitle, color = textMutedColor(), fontSize = 12.sp)
        }

        Box(
            modifier = Modifier
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(999.dp),
                    ambientColor = Color(0xFF202F4D).copy(alpha = 0.04f),
                    spotColor = Color(0xFF202F4D).copy(alpha = 0.07f)
                )
                .clip(RoundedCornerShape(999.dp))
                .background(panelColor())
                .clickable(onClick = onClick)
                .padding(horizontal = 13.dp, vertical = 9.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = buttonText,
                color = if (buttonDanger) Color(0xFFC92A2A) else textSecondaryColor(),
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun TrustDashboardCard(data: TrustDashboardUiData) {
    GlassPanel(padding = 20.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("信任监控仪表盘", color = textMainColor(), fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Text(data.evaluatedAtText, color = textMutedColor(), fontSize = 12.sp)
        }

        Spacer(Modifier.height(18.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ScoreRing(data.trustScore)

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                LevelBadge(data.trustLevelLabel, data.trustLevelColor, data.trustLevelBg)
                Spacer(Modifier.height(11.dp))
                Text(
                    text = data.summaryText,
                    color = textSecondaryColor(),
                    fontSize = 13.sp,
                    lineHeight = 20.sp
                )

                Spacer(Modifier.height(14.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    MiniMetric(data.deviceTrustText, "设备可信度", Modifier.weight(1f))
                    MiniMetric(data.behaviorText, "行为一致性", Modifier.weight(1f))
                }
                if (data.requestIdText.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text("请求ID: ${data.requestIdText}", color = textMutedColor(), fontSize = 11.sp)
                }
                if (data.resultLogPathText.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text("日志: ${data.resultLogPathText}", color = textMutedColor(), fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun ScoreRing(score: Int) {
    Box(
        modifier = Modifier
            .size(138.dp)
            .shadow(
                elevation = 18.dp,
                shape = CircleShape,
                ambientColor = AppBlue.copy(alpha = 0.10f),
                spotColor = AppBlue.copy(alpha = 0.22f)
            )
            .clip(CircleShape)
            .background(
                Brush.sweepGradient(
                    0.00f to AppBlue,
                    0.88f to AppBlue,
                    0.89f to internalSystemColors().scoreTrack,
                    1.00f to internalSystemColors().scoreTrack
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(114.dp)
                .clip(CircleShape)
                .background(internalSystemColors().scoreInner)
                .border(1.dp, lineColor(), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(score.toString(), color = textMainColor(), fontSize = 38.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(4.dp))
                Text("信任分", color = textMutedColor(), fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun LevelBadge(label: String, labelColor: Color, bgColor: Color) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bgColor)
            .padding(horizontal = 11.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(labelColor)
        )
        Text(label, color = labelColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ActionTip(info: String?, error: String?) {
    if (info.isNullOrBlank() && error.isNullOrBlank()) return
    val text = error ?: info.orEmpty()
    val isError = !error.isNullOrBlank()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isError) Red.copy(alpha = if (isSystemInDarkTheme()) 0.18f else 0.14f) else Green.copy(alpha = if (isSystemInDarkTheme()) 0.18f else 0.14f))
            .border(1.dp, if (isError) Red.copy(alpha = 0.45f) else Green.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            color = if (isError) if (isSystemInDarkTheme()) Color(0xFFFCA5A5) else Color(0xFFB91C1C) else if (isSystemInDarkTheme()) Color(0xFF86EFAC) else Color(0xFF065F46),
            fontSize = 12.sp,
            lineHeight = 17.sp
        )
    }
    Spacer(Modifier.height(10.dp))
}

@Composable
private fun MiniMetric(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(nestedPanelColor())
            .border(1.dp, lineColor(), RoundedCornerShape(16.dp))
            .padding(10.dp)
    ) {
        Text(value, color = textMainColor(), fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(3.dp))
        Text(label, color = textMutedColor(), fontSize = 11.sp)
    }
}

@Composable
private fun SectionTitleBar(title: String, note: String) {
    Spacer(Modifier.height(18.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = textMainColor(), fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Text(note, color = textMutedColor(), fontSize = 12.sp)
    }
    Spacer(Modifier.height(12.dp))
}

@Composable
private fun SecurityStatusCard(
    title: String,
    description: String,
    badge: String,
    iconColor: Color,
    iconBg: Color,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(22.dp),
                ambientColor = Color(0xFF22304E).copy(alpha = 0.04f),
                spotColor = Color(0xFF22304E).copy(alpha = 0.075f)
            )
            .clip(RoundedCornerShape(22.dp))
            .background(panelColor())
            .border(1.dp, lineColor().copy(alpha = 0.72f), RoundedCornerShape(22.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconBox(iconColor = iconColor, iconBg = iconBg, size = 42.dp, icon = icon)
            Badge(text = badge, type = BadgeType.Success)
        }

        Spacer(Modifier.height(13.dp))

        Text(title, color = textMainColor(), fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(5.dp))
        Text(description, color = textMutedColor(), fontSize = 12.sp, lineHeight = 17.sp)
    }
}

@Composable
private fun AppGridAll(onAppClick: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            AppItem("公文", AppBlueDark, Modifier.weight(1f), onClick = { onAppClick("公文") }) { DocumentIcon(Modifier.size(25.dp), AppBlueDark) }
            AppItem("业务", Purple, Modifier.weight(1f), onClick = { onAppClick("业务") }) { BriefcaseIcon(Modifier.size(25.dp), Purple) }
            AppItem("档案", Orange, Modifier.weight(1f), onClick = { onAppClick("档案") }) { FileIcon(Modifier.size(25.dp), Orange) }
            AppItem("安全", Color(0xFF10B981), Modifier.weight(1f), onClick = { onAppClick("安全") }) { ShieldIcon(Modifier.size(25.dp), Color(0xFF10B981)) }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            AppItem("消息", Color(0xFF06B6D4), Modifier.weight(1f), onClick = { onAppClick("消息") }) { MessageIcon(Modifier.size(25.dp), Color(0xFF06B6D4)) }
            AppItem("审批", Color(0xFF64748B), Modifier.weight(1f), onClick = { onAppClick("审批") }) { ClipboardIcon(Modifier.size(25.dp), Color(0xFF64748B)) }
            AppItem("协同", Color(0xFFEC4899), Modifier.weight(1f), onClick = { onAppClick("协同") }) { SyncIcon(Modifier.size(25.dp), Color(0xFFEC4899)) }
            AppItem("日志", Color(0xFF0F766E), Modifier.weight(1f), onClick = { onAppClick("日志") }) { ClockIcon(Modifier.size(25.dp), Color(0xFF0F766E)) }
        }
    }
}

@Composable
private fun AppItem(
    title: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    icon: @Composable () -> Unit
) {
    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .shadow(
                    elevation = 10.dp,
                    shape = RoundedCornerShape(18.dp),
                    ambientColor = Color(0xFF22304E).copy(alpha = 0.04f),
                    spotColor = Color(0xFF22304E).copy(alpha = 0.08f)
                )
                .clip(RoundedCornerShape(18.dp))
                .background(color.copy(alpha = 0.08f))
                .border(1.dp, lineColor().copy(alpha = 0.72f), RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center
        ) {
            icon()
        }

        Spacer(Modifier.height(7.dp))

        Text(title, color = textSecondaryColor(), fontSize = 11.sp, maxLines = 1)
    }
}

@Composable
private fun InfoPanel(profile: InternalIdentityUiData) {
    GlassPanel(padding = 16.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("用户信息", color = textMainColor(), fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Text("基础资料", color = textMutedColor(), fontSize = 12.sp)
        }

        Spacer(Modifier.height(12.dp))

        InfoLine("用户名", profile.username)
        InfoLine("姓名", profile.realName)
        InfoLine("手机号", profile.phoneMasked)
        InfoLine("邮箱", profile.emailMasked)
        InfoLine("证件号", profile.idCardMasked)
        InfoLine("证书编号", profile.certificateNo, last = true)
    }
}

@Composable
private fun InfoLine(label: String, value: String, last: Boolean = false) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = textMutedColor(), fontSize = 13.sp)
            Text(value, color = textMainColor(), fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        if (!last) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(lineColor().copy(alpha = 0.90f))
            )
        }
    }
}

@Composable
private fun GlassPanel(
    padding: androidx.compose.ui.unit.Dp = 14.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 18.dp,
                shape = RoundedCornerShape(28.dp),
                ambientColor = Color(0xFF22304E).copy(alpha = 0.06f),
                spotColor = Color(0xFF22304E).copy(alpha = 0.10f)
            )
            .clip(RoundedCornerShape(28.dp))
            .background(panelColor())
            .border(1.dp, lineColor().copy(alpha = 0.72f), RoundedCornerShape(28.dp))
            .padding(padding),
        content = content
    )
}

@Composable
private fun BioRow(
    title: String,
    subtitle: String,
    badge: String,
    badgeType: BadgeType,
    iconColor: Color,
    iconBg: Color,
    onClick: () -> Unit = {},
    icon: @Composable () -> Unit
) {
    MenuRow(
        title = title,
        subtitle = subtitle,
        badge = badge,
        badgeType = badgeType,
        iconColor = iconColor,
        iconBg = iconBg,
        onClick = onClick,
        icon = icon
    )
}

@Composable
private fun MenuRow(
    title: String,
    subtitle: String,
    badge: String,
    badgeType: BadgeType,
    iconColor: Color,
    iconBg: Color,
    onClick: () -> Unit = {},
    icon: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(nestedPanelColor())
            .border(1.dp, lineColor(), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBox(iconColor = iconColor, iconBg = iconBg, size = 42.dp, icon = icon)

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = textMainColor(), fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(3.dp))
            Text(subtitle, color = textMutedColor(), fontSize = 12.sp)
        }

        Badge(text = badge, type = badgeType)
    }
}

@Composable
private fun SecurityDetailSection(section: SecurityDetailSectionUiData, onClick: (() -> Unit)? = null) {
    val badgeType = when (section.status.uppercase()) {
        "DANGER", "CRITICAL", "ERROR" -> BadgeType.Danger
        "WARN", "WARNING" -> BadgeType.Warning
        else -> BadgeType.Success
    }
    val color = when (badgeType) {
        BadgeType.Danger -> Red
        BadgeType.Warning -> Orange
        BadgeType.Success -> AppBlueDark
    }
    val cardModifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(20.dp))
        .background(nestedPanelColor())
        .border(1.dp, lineColor(), RoundedCornerShape(20.dp))
        .let { base -> if (onClick == null) base else base.clickable(onClick = onClick) }
        .padding(12.dp)
    Column(
        modifier = cardModifier
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBox(
                    iconColor = color,
                    iconBg = color.copy(alpha = 0.10f),
                    size = 38.dp
                ) {
                    ShieldCheckIcon(Modifier.size(21.dp), color)
                }
                Spacer(Modifier.width(10.dp))
                Text(section.title, color = textMainColor(), fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            Badge(section.badge, badgeType)
        }

        Spacer(Modifier.height(10.dp))

        section.rows.forEachIndexed { index, row ->
            SecurityDetailRowItem(row)
            if (index != section.rows.lastIndex) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(lineColor().copy(alpha = 0.72f))
                )
            }
        }
    }
}

@Composable
private fun SecurityDetailRowItem(row: SecurityDetailRowUiData) {
    val normalized = row.value.trim()
    val positive = normalized in setOf("是", "支持", "可信", "已锁定", "已采集") ||
        normalized.contains("正常") || normalized.contains("LOW") || normalized.contains("ALLOW")
    val negative = normalized in setOf("否", "不支持", "不可信", "已解锁") ||
        normalized.contains("风险") || normalized.contains("未授权") || normalized.contains("未获取") || normalized.contains("未知")
    val dotColor = when {
        negative -> Orange
        positive -> Green
        else -> AppBlueDark
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 9.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 5.dp)
                .size(8.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(row.label, color = textMutedColor(), fontSize = 12.sp, lineHeight = 16.sp)
            Spacer(Modifier.height(3.dp))
            Text(
                row.value,
                color = textMainColor(),
                fontSize = 13.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun securitySections(securityCategory: SecurityCategoryUiData): List<SecurityDetailSectionUiData> {
    return securityCategory.detailSections.ifEmpty {
        listOf(
            SecurityDetailSectionUiData(
                title = "系统完整性",
                badge = securityCategory.softwareBadge,
                status = "SUCCESS",
                rows = securityCategory.softwareLines.mapIndexed { index, value -> SecurityDetailRowUiData("检测项${index + 1}", value) }
            ),
            SecurityDetailSectionUiData(
                title = "设备身份与上下文",
                badge = securityCategory.hardwareBadge,
                status = "SUCCESS",
                rows = securityCategory.hardwareLines.mapIndexed { index, value -> SecurityDetailRowUiData("检测项${index + 1}", value) }
            )
        )
    }
}

private fun SecurityDetailSectionUiData.isSoftwareSection(): Boolean {
    return title in setOf("系统完整性", "运行环境风险", "系统基线", "网络安全", "应用风险")
}

private fun badgeTypeFor(text: String): BadgeType {
    return when {
        text.contains("风险") || text.contains("高") || text.contains("拒绝") -> BadgeType.Danger
        text.contains("告警") || text.contains("一般") || text.contains("建议") || text.contains("评估") -> BadgeType.Warning
        else -> BadgeType.Success
    }
}

private enum class BadgeType {
    Success, Warning, Danger
}

@Composable
private fun Badge(text: String, type: BadgeType) {
    val bg = when (type) {
        BadgeType.Success -> Green.copy(alpha = 0.12f)
        BadgeType.Warning -> Orange.copy(alpha = 0.12f)
        BadgeType.Danger -> Red.copy(alpha = 0.10f)
    }
    val fg = when (type) {
        BadgeType.Success -> Color(0xFF087F5B)
        BadgeType.Warning -> Color(0xFFB77905)
        BadgeType.Danger -> Color(0xFFC92A2A)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 5.dp)
    ) {
        Text(text, color = fg, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun IconBox(
    iconColor: Color,
    iconBg: Color,
    size: androidx.compose.ui.unit.Dp,
    icon: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(16.dp))
            .background(iconBg),
        contentAlignment = Alignment.Center
    ) {
        icon()
    }
}

@Composable
private fun BottomNavigationUi(selectedTab: String, onTabChange: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(26.dp),
                ambientColor = Color(0xFF22304E).copy(alpha = 0.06f),
                spotColor = Color(0xFF22304E).copy(alpha = 0.10f)
            )
            .clip(RoundedCornerShape(26.dp))
            .background(panelColor())
            .border(1.dp, lineColor().copy(alpha = 0.72f), RoundedCornerShape(26.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BottomNavItem("首页", selectedTab == "首页", Modifier.weight(1f), onClick = { onTabChange("首页") })
        BottomNavItem("应用", selectedTab == "应用", Modifier.weight(1f), onClick = { onTabChange("应用") })
        BottomNavItem("安全", selectedTab == "安全", Modifier.weight(1f), onClick = { onTabChange("安全") })
        BottomNavItem("我的", selectedTab == "我的", Modifier.weight(1f), onClick = { onTabChange("我的") })
    }
}

@Composable
private fun BottomNavItem(
    title: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .width(24.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(if (selected) AppBlue else Color.Transparent)
        )

        Spacer(Modifier.height(5.dp))

        Text(
            text = title,
            color = if (selected) AppBlueDark else textMutedColor(),
            fontSize = 10.5.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

/* ---------------- Icon drawing ---------------- */

@Composable
private fun SoftwareIcon(modifier: Modifier = Modifier, color: Color) {
    Canvas(modifier = modifier) {
        val sw = 1.9.dp.toPx()
        val stroke = Stroke(width = sw, cap = StrokeCap.Round)
        drawRoundRect(color, Offset(size.width * .10f, size.height * .10f), Size(size.width * .80f, size.height * .80f), CornerRadius(4.dp.toPx(), 4.dp.toPx()), style = stroke)
        drawLine(color, Offset(size.width * .25f, size.height * .32f), Offset(size.width * .75f, size.height * .32f), sw, StrokeCap.Round)
        drawLine(color, Offset(size.width * .25f, size.height * .50f), Offset(size.width * .58f, size.height * .50f), sw, StrokeCap.Round)
        drawLine(color, Offset(size.width * .25f, size.height * .68f), Offset(size.width * .70f, size.height * .68f), sw, StrokeCap.Round)
    }
}

@Composable
private fun HardwareIcon(modifier: Modifier = Modifier, color: Color) {
    Canvas(modifier = modifier) {
        val sw = 1.9.dp.toPx()
        val stroke = Stroke(width = sw, cap = StrokeCap.Round)
        drawRect(color, Offset(size.width * .28f, size.height * .15f), Size(size.width * .44f, size.height * .70f), style = stroke)
        drawRect(color, Offset(size.width * .38f, size.height * .38f), Size(size.width * .24f, size.height * .24f), style = stroke)
        listOf(.25f, .50f, .75f).forEach { y ->
            drawLine(color, Offset(size.width * .12f, size.height * y), Offset(size.width * .28f, size.height * y), sw, StrokeCap.Round)
            drawLine(color, Offset(size.width * .72f, size.height * y), Offset(size.width * .88f, size.height * y), sw, StrokeCap.Round)
        }
    }
}

@Composable
private fun DocumentIcon(modifier: Modifier = Modifier, color: Color) {
    Canvas(modifier = modifier) {
        val sw = 1.9.dp.toPx()
        val stroke = Stroke(width = sw, cap = StrokeCap.Round)
        drawRoundRect(color, Offset(size.width * .18f, size.height * .12f), Size(size.width * .64f, size.height * .76f), CornerRadius(4.dp.toPx(), 4.dp.toPx()), style = stroke)
        drawLine(color, Offset(size.width * .32f, size.height * .34f), Offset(size.width * .68f, size.height * .34f), sw, StrokeCap.Round)
        drawLine(color, Offset(size.width * .32f, size.height * .50f), Offset(size.width * .56f, size.height * .50f), sw, StrokeCap.Round)
        drawLine(color, Offset(size.width * .32f, size.height * .66f), Offset(size.width * .62f, size.height * .66f), sw, StrokeCap.Round)
    }
}

@Composable
private fun BriefcaseIcon(modifier: Modifier = Modifier, color: Color) {
    Canvas(modifier = modifier) {
        val sw = 1.9.dp.toPx()
        val stroke = Stroke(width = sw, cap = StrokeCap.Round)
        drawLine(color, Offset(size.width * .14f, size.height * .34f), Offset(size.width * .86f, size.height * .34f), sw, StrokeCap.Round)
        drawRoundRect(color, Offset(size.width * .22f, size.height * .34f), Size(size.width * .56f, size.height * .52f), CornerRadius(4.dp.toPx(), 4.dp.toPx()), style = stroke)
        drawLine(color, Offset(size.width * .38f, size.height * .34f), Offset(size.width * .38f, size.height * .24f), sw, StrokeCap.Round)
        drawLine(color, Offset(size.width * .62f, size.height * .34f), Offset(size.width * .62f, size.height * .24f), sw, StrokeCap.Round)
        drawLine(color, Offset(size.width * .42f, size.height * .24f), Offset(size.width * .58f, size.height * .24f), sw, StrokeCap.Round)
        drawLine(color, Offset(size.width * .42f, size.height * .58f), Offset(size.width * .58f, size.height * .58f), sw, StrokeCap.Round)
    }
}

@Composable
private fun FileIcon(modifier: Modifier = Modifier, color: Color) {
    Canvas(modifier = modifier) {
        val sw = 1.9.dp.toPx()
        val stroke = Stroke(width = sw, cap = StrokeCap.Round)
        drawRect(color, Offset(size.width * .26f, size.height * .12f), Size(size.width * .48f, size.height * .76f), style = stroke)
        drawLine(color, Offset(size.width * .36f, size.height * .32f), Offset(size.width * .64f, size.height * .32f), sw, StrokeCap.Round)
        drawLine(color, Offset(size.width * .36f, size.height * .50f), Offset(size.width * .64f, size.height * .50f), sw, StrokeCap.Round)
        drawLine(color, Offset(size.width * .36f, size.height * .68f), Offset(size.width * .54f, size.height * .68f), sw, StrokeCap.Round)
    }
}

@Composable
private fun ShieldIcon(modifier: Modifier = Modifier, color: Color) {
    Canvas(modifier = modifier) {
        val sw = 1.9.dp.toPx()
        val stroke = Stroke(width = sw, cap = StrokeCap.Round)
        val path = Path().apply {
            moveTo(size.width * .50f, size.height * .12f)
            lineTo(size.width * .80f, size.height * .28f)
            lineTo(size.width * .80f, size.height * .50f)
            cubicTo(size.width * .80f, size.height * .72f, size.width * .64f, size.height * .84f, size.width * .50f, size.height * .90f)
            cubicTo(size.width * .36f, size.height * .84f, size.width * .20f, size.height * .72f, size.width * .20f, size.height * .50f)
            lineTo(size.width * .20f, size.height * .28f)
            close()
        }
        drawPath(path, color, style = stroke)
        drawLine(color, Offset(size.width * .38f, size.height * .52f), Offset(size.width * .47f, size.height * .61f), sw, StrokeCap.Round)
        drawLine(color, Offset(size.width * .47f, size.height * .61f), Offset(size.width * .64f, size.height * .42f), sw, StrokeCap.Round)
    }
}

@Composable
private fun MessageIcon(modifier: Modifier = Modifier, color: Color) {
    Canvas(modifier = modifier) {
        val sw = 1.9.dp.toPx()
        val stroke = Stroke(width = sw, cap = StrokeCap.Round)
        drawRoundRect(color, Offset(size.width * .12f, size.height * .18f), Size(size.width * .76f, size.height * .64f), CornerRadius(4.dp.toPx(), 4.dp.toPx()), style = stroke)
        drawLine(color, Offset(size.width * .12f, size.height * .34f), Offset(size.width * .88f, size.height * .34f), sw, StrokeCap.Round)
        drawLine(color, Offset(size.width * .28f, size.height * .54f), Offset(size.width * .50f, size.height * .54f), sw, StrokeCap.Round)
        drawLine(color, Offset(size.width * .28f, size.height * .68f), Offset(size.width * .62f, size.height * .68f), sw, StrokeCap.Round)
    }
}

@Composable
private fun ClipboardIcon(modifier: Modifier = Modifier, color: Color) {
    Canvas(modifier = modifier) {
        val sw = 1.9.dp.toPx()
        val stroke = Stroke(width = sw, cap = StrokeCap.Round)
        drawRect(color, Offset(size.width * .24f, size.height * .20f), Size(size.width * .52f, size.height * .68f), style = stroke)
        drawLine(color, Offset(size.width * .36f, size.height * .20f), Offset(size.width * .36f, size.height * .12f), sw, StrokeCap.Round)
        drawLine(color, Offset(size.width * .64f, size.height * .20f), Offset(size.width * .64f, size.height * .12f), sw, StrokeCap.Round)
        drawLine(color, Offset(size.width * .36f, size.height * .12f), Offset(size.width * .64f, size.height * .12f), sw, StrokeCap.Round)
        drawLine(color, Offset(size.width * .36f, size.height * .46f), Offset(size.width * .64f, size.height * .46f), sw, StrokeCap.Round)
        drawLine(color, Offset(size.width * .36f, size.height * .62f), Offset(size.width * .54f, size.height * .62f), sw, StrokeCap.Round)
    }
}

@Composable
private fun SyncIcon(modifier: Modifier = Modifier, color: Color) {
    Canvas(modifier = modifier) {
        val sw = 1.9.dp.toPx()
        drawLine(color, Offset(size.width * .50f, size.height * .18f), Offset(size.width * .50f, size.height * .82f), sw, StrokeCap.Round)
        drawLine(color, Offset(size.width * .18f, size.height * .50f), Offset(size.width * .82f, size.height * .50f), sw, StrokeCap.Round)
        drawLine(color, Offset(size.width * .28f, size.height * .28f), Offset(size.width * .72f, size.height * .72f), sw, StrokeCap.Round)
        drawLine(color, Offset(size.width * .72f, size.height * .28f), Offset(size.width * .28f, size.height * .72f), sw, StrokeCap.Round)
    }
}

@Composable
private fun ClockIcon(modifier: Modifier = Modifier, color: Color) {
    Canvas(modifier = modifier) {
        val sw = 1.9.dp.toPx()
        val stroke = Stroke(width = sw, cap = StrokeCap.Round)
        drawCircle(color, radius = size.width * .34f, center = Offset(size.width * .50f, size.height * .50f), style = stroke)
        drawLine(color, Offset(size.width * .50f, size.height * .32f), Offset(size.width * .50f, size.height * .50f), sw, StrokeCap.Round)
        drawLine(color, Offset(size.width * .50f, size.height * .50f), Offset(size.width * .62f, size.height * .62f), sw, StrokeCap.Round)
    }
}

@Composable
private fun PlusIcon(modifier: Modifier = Modifier, color: Color) {
    Canvas(modifier = modifier) {
        val sw = 1.9.dp.toPx()
        drawLine(color, Offset(size.width * .18f, size.height * .50f), Offset(size.width * .82f, size.height * .50f), sw, StrokeCap.Round)
        drawLine(color, Offset(size.width * .50f, size.height * .18f), Offset(size.width * .50f, size.height * .82f), sw, StrokeCap.Round)
    }
}

@Composable
private fun ShieldCheckIcon(modifier: Modifier = Modifier, color: Color) = ShieldIcon(modifier, color)

@Composable
private fun WarningIcon(modifier: Modifier = Modifier, color: Color) {
    Canvas(modifier = modifier) {
        val sw = 1.9.dp.toPx()
        val stroke = Stroke(width = sw, cap = StrokeCap.Round)
        val path = Path().apply {
            moveTo(size.width * .50f, size.height * .14f)
            lineTo(size.width * .88f, size.height * .82f)
            lineTo(size.width * .12f, size.height * .82f)
            close()
        }
        drawPath(path, color, style = stroke)
        drawLine(color, Offset(size.width * .50f, size.height * .38f), Offset(size.width * .50f, size.height * .60f), sw, StrokeCap.Round)
        drawCircle(color, radius = 1.4.dp.toPx(), center = Offset(size.width * .50f, size.height * .70f))
    }
}

@Composable
private fun FingerprintIcon(modifier: Modifier = Modifier, color: Color) {
    Canvas(modifier = modifier) {
        val stroke = Stroke(width = 1.9.dp.toPx(), cap = StrokeCap.Round)
        drawArc(color, 200f, 140f, false, Offset(size.width * .18f, size.height * .08f), Size(size.width * .64f, size.height * .58f), style = stroke)
        drawArc(color, 190f, 160f, false, Offset(size.width * .08f, size.height * .18f), Size(size.width * .84f, size.height * .72f), style = stroke)
        drawArc(color, 210f, 120f, false, Offset(size.width * .30f, size.height * .25f), Size(size.width * .40f, size.height * .42f), style = stroke)
        drawLine(color, Offset(size.width * .50f, size.height * .50f), Offset(size.width * .42f, size.height * .88f), 1.9.dp.toPx(), StrokeCap.Round)
        drawLine(color, Offset(size.width * .62f, size.height * .54f), Offset(size.width * .56f, size.height * .82f), 1.9.dp.toPx(), StrokeCap.Round)
        drawLine(color, Offset(size.width * .34f, size.height * .56f), Offset(size.width * .25f, size.height * .78f), 1.9.dp.toPx(), StrokeCap.Round)
    }
}

@Composable
private fun FaceIcon(modifier: Modifier = Modifier, color: Color) {
    Canvas(modifier = modifier) {
        val sw = 1.9.dp.toPx()
        val stroke = Stroke(width = sw, cap = StrokeCap.Round)
        drawLine(color, Offset(size.width * .18f, size.height * .12f), Offset(size.width * .32f, size.height * .12f), sw, StrokeCap.Round)
        drawLine(color, Offset(size.width * .12f, size.height * .18f), Offset(size.width * .12f, size.height * .32f), sw, StrokeCap.Round)
        drawLine(color, Offset(size.width * .68f, size.height * .12f), Offset(size.width * .82f, size.height * .12f), sw, StrokeCap.Round)
        drawLine(color, Offset(size.width * .88f, size.height * .18f), Offset(size.width * .88f, size.height * .32f), sw, StrokeCap.Round)
        drawLine(color, Offset(size.width * .18f, size.height * .88f), Offset(size.width * .32f, size.height * .88f), sw, StrokeCap.Round)
        drawLine(color, Offset(size.width * .12f, size.height * .82f), Offset(size.width * .12f, size.height * .68f), sw, StrokeCap.Round)
        drawLine(color, Offset(size.width * .68f, size.height * .88f), Offset(size.width * .82f, size.height * .88f), sw, StrokeCap.Round)
        drawLine(color, Offset(size.width * .88f, size.height * .82f), Offset(size.width * .88f, size.height * .68f), sw, StrokeCap.Round)
        drawCircle(color, 1.4.dp.toPx(), Offset(size.width * .36f, size.height * .44f))
        drawCircle(color, 1.4.dp.toPx(), Offset(size.width * .64f, size.height * .44f))
        drawLine(color, Offset(size.width * .50f, size.height * .48f), Offset(size.width * .50f, size.height * .58f), sw, StrokeCap.Round)
        drawArc(color, 30f, 120f, false, Offset(size.width * .34f, size.height * .54f), Size(size.width * .32f, size.height * .25f), style = stroke)
    }
}

@Composable
private fun VoiceIcon(modifier: Modifier = Modifier, color: Color) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val sw = 1.9.dp.toPx()
        val xs = listOf(.14f, .30f, .46f, .62f, .78f)
        val heights = listOf(.16f, .56f, .82f, .56f, .30f)
        xs.forEachIndexed { index, x ->
            val barH = h * heights[index]
            drawLine(color, Offset(w * x, h / 2 - barH / 2), Offset(w * x, h / 2 + barH / 2), sw, StrokeCap.Round)
        }
        drawLine(color, Offset(w * .92f, h * .42f), Offset(w * .92f, h * .58f), sw, StrokeCap.Round)
    }
}

@Composable
private fun CertificateIcon(modifier: Modifier = Modifier, color: Color) {
    Canvas(modifier = modifier) {
        val sw = 1.9.dp.toPx()
        val stroke = Stroke(width = sw, cap = StrokeCap.Round)
        val path = Path().apply {
            moveTo(size.width * .26f, size.height * .10f)
            lineTo(size.width * .63f, size.height * .10f)
            lineTo(size.width * .80f, size.height * .27f)
            lineTo(size.width * .80f, size.height * .88f)
            lineTo(size.width * .26f, size.height * .88f)
            close()
        }
        drawPath(path, color, style = stroke)
        drawLine(color, Offset(size.width * .63f, size.height * .10f), Offset(size.width * .63f, size.height * .30f), sw, StrokeCap.Round)
        drawLine(color, Offset(size.width * .63f, size.height * .30f), Offset(size.width * .80f, size.height * .30f), sw, StrokeCap.Round)
        drawLine(color, Offset(size.width * .35f, size.height * .62f), Offset(size.width * .45f, size.height * .72f), sw, StrokeCap.Round)
        drawLine(color, Offset(size.width * .45f, size.height * .72f), Offset(size.width * .68f, size.height * .48f), sw, StrokeCap.Round)
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844, name = "首页")
@Composable
private fun HomePreview() {
    InternalSystemScreen(
        onLogout = {},
        onAccessApp = {},
        onRunTrustCheck = {},
        onRunReleaseCheck = {},
        onSyncPolicy = {},
        onRunSecurityCheck = {},
        onBiometricTip = {},
        onGoCertificateCenter = {}
    )
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844, name = "应用")
@Composable
private fun AppsPreview() {
    InternalSystemScreen(
        onLogout = {},
        onAccessApp = {},
        onRunTrustCheck = {},
        onRunReleaseCheck = {},
        onSyncPolicy = {},
        onRunSecurityCheck = {},
        onBiometricTip = {},
        onGoCertificateCenter = {}
    )
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844, name = "安全")
@Composable
private fun SecurityPreview() {
    InternalSystemScreen(
        onLogout = {},
        onAccessApp = {},
        onRunTrustCheck = {},
        onRunReleaseCheck = {},
        onSyncPolicy = {},
        onRunSecurityCheck = {},
        onBiometricTip = {},
        onGoCertificateCenter = {}
    )
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844, name = "我的")
@Composable
private fun ProfilePreview() {
    InternalSystemScreen(
        onLogout = {},
        onAccessApp = {},
        onRunTrustCheck = {},
        onRunReleaseCheck = {},
        onSyncPolicy = {},
        onRunSecurityCheck = {},
        onBiometricTip = {},
        onGoCertificateCenter = {}
    )
}
