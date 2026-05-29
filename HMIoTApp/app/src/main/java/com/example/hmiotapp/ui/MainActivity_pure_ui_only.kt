package com.example.identitylogin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val AppBlue = Color(0xFF12A8FF)
private val AppBlueDark = Color(0xFF0587DC)
private val TextMain = Color(0xFF121826)
private val TextMuted = Color(0xFF8B92A1)
private val Line = Color(0xFFEEF2F7)
private val Orange = Color(0xFFF59E0B)
private val Green = Color(0xFF20C997)
private val Red = Color(0xFFEF4444)

class MainActivity_pure_ui_only : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PureUiApp {
                LoginPageUi()
            }
        }
    }
}

/**
 * 纯前端 UI 文件：
 * - 不包含登录、注册、证书保存、页面跳转等业务逻辑
 * - 按钮只保留空点击占位
 * - 输入框只用于界面展示
 * - 可以通过下方 @Preview 分别预览登录、注册、证书绑定、审核记录页面
 */

@Composable
private fun PureUiApp(content: @Composable BoxScope.() -> Unit) {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = Color.White) {
            AppScreenBackground {
                content()
            }
        }
    }
}

@Composable
private fun AppScreenBackground(content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0f to Color(0xFFFBFCFF),
                    0.40f to Color.White,
                    1f to Color(0xFFF4F7FB)
                )
            )
    ) {
        SoftGlow(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = (-88).dp, y = 80.dp)
                .size(210.dp),
            color = AppBlue.copy(alpha = 0.13f)
        )

        SoftGlow(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 96.dp, y = 220.dp)
                .size(190.dp),
            color = Color(0xFFB5E4FF).copy(alpha = 0.34f)
        )

        SoftGlow(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = 26.dp, y = (-28).dp)
                .size(190.dp),
            color = AppBlue.copy(alpha = 0.09f)
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 28.dp),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                FakeStatusBar()
                Box(modifier = Modifier.fillMaxSize(), content = content)
            }
        }
    }
}

@Composable
private fun SoftGlow(
    modifier: Modifier,
    color: Color
) {
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
            .height(38.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("9:41", fontSize = 13.sp, color = Color(0xFF1D2433), fontWeight = FontWeight.Bold)

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.height(12.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                Box(Modifier.width(3.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFF1D2433)))
                Box(Modifier.width(3.dp).height(7.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFF1D2433)))
                Box(Modifier.width(3.dp).height(10.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFF1D2433)))
            }

            Text("5G", fontSize = 13.sp, color = Color(0xFF1D2433), fontWeight = FontWeight.Bold)
            BatteryIcon()
        }
    }
}

@Composable
private fun BatteryIcon() {
    Canvas(modifier = Modifier.size(width = 26.dp, height = 13.dp)) {
        val color = Color(0xFF1D2433)
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
private fun LoginPageUi() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TopBar(title = "简体中文")

        Spacer(Modifier.height(24.dp))

        QqLikeLogo()

        Spacer(Modifier.height(14.dp))

        Text(
            text = "QQ",
            color = TextMain,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        Spacer(Modifier.height(30.dp))

        UiTextField(placeholder = "用户名", big = true)

        Spacer(Modifier.height(14.dp))

        UiTextField(
            placeholder = "输入密码",
            big = true,
            visualTransformation = PasswordVisualTransformation(),
            trailing = {
                EyeOffIcon(
                    modifier = Modifier.size(24.dp),
                    color = Color(0xFF9DA6B5)
                )
            }
        )

        Spacer(Modifier.height(24.dp))

        CircleLoginButton()

        Spacer(Modifier.height(108.dp))

        SectionTitle("更多登录方式")

        Spacer(Modifier.height(15.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LoginMethodItem("指纹", Modifier.weight(1f)) {
                FingerprintIcon(Modifier.size(24.dp), Color(0xFF2F80ED))
            }

            LoginMethodItem("人脸", Modifier.weight(1f)) {
                FaceIcon(Modifier.size(24.dp), Color(0xFF7C3AED))
            }

            LoginMethodItem("声纹", Modifier.weight(1f)) {
                VoiceIcon(Modifier.size(24.dp), Orange)
            }

            LoginMethodItem("数字证书", Modifier.weight(1f)) {
                CertificateIcon(Modifier.size(24.dp), Color(0xFF10B981))
            }
        }

        Spacer(Modifier.height(42.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            FooterTextButton("找回密码")
            FooterTextButton("新用户注册")
            FooterTextButton("审核进度")
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun CertificateBindPageUi() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        TopBar(title = "证书登录", showBack = true)

        Spacer(Modifier.height(18.dp))

        Text(
            text = "绑定证书",
            color = TextMain,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(18.dp))

        CertificateHero()

        Spacer(Modifier.height(18.dp))

        Text(
            text = "由管理员根据用户名签发证书后，在本设备填写证书编号并保存。",
            color = TextMuted,
            fontSize = 13.sp,
            lineHeight = 20.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        )

        Spacer(Modifier.height(18.dp))

        GlassPanel {
            UiTextField(placeholder = "用户名")

            Spacer(Modifier.height(10.dp))

            UiTextField(placeholder = "用户证书编号")

            Spacer(Modifier.height(6.dp))

            PrimaryButtonUi("保存证书")

            Spacer(Modifier.height(10.dp))

            SecondaryButtonUi("返回登录")

            Spacer(Modifier.height(14.dp))

            SavedCertificateBox()
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun RegisterPageUi() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        TopBar(title = "注册申请", showBack = true)

        Spacer(Modifier.height(18.dp))

        Text(
            text = "用户注册",
            color = TextMain,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(16.dp))

        GlassPanel {
            UiTextField(placeholder = "用户名")

            Spacer(Modifier.height(10.dp))

            UiTextField(
                placeholder = "密码",
                visualTransformation = PasswordVisualTransformation(),
                trailing = {
                    EyeOffIcon(
                        modifier = Modifier.size(24.dp),
                        color = Color(0xFF9DA6B5)
                    )
                }
            )

            Spacer(Modifier.height(10.dp))

            UiTextField(placeholder = "姓名")

            Spacer(Modifier.height(10.dp))

            UiTextField(placeholder = "手机号")

            Spacer(Modifier.height(10.dp))

            UiTextField(placeholder = "邮箱")

            Spacer(Modifier.height(10.dp))

            UiTextField(placeholder = "证件号")

            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                BioCardUi(
                    title = "指纹",
                    statusText = "未采集",
                    captured = false,
                    modifier = Modifier.weight(1f)
                ) {
                    FingerprintIcon(Modifier.size(25.dp), Color(0xFF4B5565))
                }

                BioCardUi(
                    title = "人脸",
                    statusText = "已采集",
                    captured = true,
                    modifier = Modifier.weight(1f)
                ) {
                    FaceIcon(Modifier.size(25.dp), AppBlueDark)
                }

                BioCardUi(
                    title = "声纹",
                    statusText = "未采集",
                    captured = false,
                    modifier = Modifier.weight(1f)
                ) {
                    VoiceIcon(Modifier.size(25.dp), Color(0xFF4B5565))
                }
            }

            Spacer(Modifier.height(12.dp))

            PrimaryButtonUi("提交申请")

            Spacer(Modifier.height(10.dp))

            SecondaryButtonUi("查看注册记录")
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun RecordsPageUi() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        TopBar(title = "审核中心", showBack = true)

        Spacer(Modifier.height(18.dp))

        Text(
            text = "注册记录",
            color = TextMain,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(18.dp))

        SegmentTabsUi()

        Spacer(Modifier.height(16.dp))

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            RecordCardUi(
                username = "zhangsan",
                detail = "张三 · 138****8899 · 2026-05-29 09:30",
                status = "审核中",
                statusColor = Color(0xFFB77905),
                statusBg = Orange.copy(alpha = 0.12f),
                progress = 2,
                result = "指纹、人脸 · 等待审核"
            )

            RecordCardUi(
                username = "lisi",
                detail = "李四 · 186****1024 · 2026-05-28 16:12",
                status = "已通过",
                statusColor = Color(0xFF087F5B),
                statusBg = Green.copy(alpha = 0.12f),
                progress = 3,
                result = "指纹、人脸、声纹 · 审核通过"
            )

            RecordCardUi(
                username = "wangwu",
                detail = "王五 · 159****7712 · 2026-05-27 11:05",
                status = "未通过",
                statusColor = Color(0xFFC92A2A),
                statusBg = Red.copy(alpha = 0.10f),
                progress = 2,
                result = "声纹 · 审核未通过"
            )
        }

        Spacer(Modifier.height(14.dp))

        SecondaryButtonUi("继续提交注册申请")

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun TopBar(
    title: String,
    showBack: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (showBack) Arrangement.SpaceBetween else Arrangement.End
    ) {
        if (showBack) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .shadow(
                        elevation = 8.dp,
                        shape = CircleShape,
                        ambientColor = Color(0xFF202F4D).copy(alpha = 0.05f),
                        spotColor = Color(0xFF202F4D).copy(alpha = 0.08f)
                    )
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.76f))
                    .clickable { },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "‹",
                    color = Color(0xFF586174),
                    fontSize = 25.sp
                )
            }
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
                .background(Color.White.copy(alpha = 0.76f))
                .padding(horizontal = 14.dp, vertical = 9.dp)
        ) {
            Text(
                text = title,
                color = Color(0xFF586174),
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun QqLikeLogo() {
    Box(
        modifier = Modifier.size(104.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(92.dp)
                .offset(y = 7.dp)
                .blur(20.dp)
                .clip(RoundedCornerShape(30.dp))
                .background(AppBlue.copy(alpha = 0.22f))
        )

        Box(
            modifier = Modifier
                .size(82.dp)
                .shadow(
                    elevation = 16.dp,
                    shape = RoundedCornerShape(26.dp),
                    ambientColor = Color(0xFF293856).copy(alpha = 0.12f),
                    spotColor = Color(0xFF293856).copy(alpha = 0.16f)
                )
                .clip(RoundedCornerShape(26.dp))
                .background(Color(0xFF10131A))
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(37.dp)
                    .background(Color.White)
            )

            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 20.dp)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            )

            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 30.dp),
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(Color.Black))
                Box(Modifier.size(8.dp).clip(CircleShape).background(Color.Black))
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 23.dp)
                    .width(48.dp)
                    .height(13.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0xFFFF3B30))
            )

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 9.dp)
                    .width(38.dp)
                    .height(16.dp)
                    .clip(RoundedCornerShape(bottomStart = 22.dp, bottomEnd = 22.dp))
                    .background(Color(0xFFFFC83D))
            )
        }
    }
}

@Composable
private fun UiTextField(
    placeholder: String,
    big: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailing: (@Composable () -> Unit)? = null
) {
    var value by remember { mutableStateOf("") }
    var focused by remember { mutableStateOf(false) }
    val height = if (big) 56.dp else 50.dp
    val radius = if (big) 18.dp else 16.dp
    val fontSize = if (big) 16.sp else 14.5.sp

    BasicTextField(
        value = value,
        onValueChange = { value = it },
        singleLine = true,
        visualTransformation = visualTransformation,
        textStyle = TextStyle(
            color = Color(0xFF222A38),
            fontSize = fontSize
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .shadow(
                elevation = 7.dp,
                shape = RoundedCornerShape(radius),
                ambientColor = Color(0xFF202F4D).copy(alpha = 0.025f),
                spotColor = Color(0xFF202F4D).copy(alpha = 0.045f)
            )
            .clip(RoundedCornerShape(radius))
            .background(Color(0xFFF6F8FB).copy(alpha = 0.92f))
            .border(
                width = 1.dp,
                color = if (focused) AppBlue.copy(alpha = 0.38f) else Color(0xFFEDF1F6).copy(alpha = 0.95f),
                shape = RoundedCornerShape(radius)
            )
            .padding(horizontal = 15.dp)
            .onFocusChanged { focused = it.isFocused },
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            color = Color(0xFFA5ACB8),
                            fontSize = fontSize
                        )
                    }
                    innerTextField()
                }

                if (trailing != null) {
                    Spacer(Modifier.width(10.dp))
                    trailing()
                }
            }
        }
    )
}

@Composable
private fun CircleLoginButton() {
    Box(
        modifier = Modifier
            .size(68.dp)
            .shadow(
                elevation = 18.dp,
                shape = CircleShape,
                ambientColor = AppBlue.copy(alpha = 0.20f),
                spotColor = AppBlue.copy(alpha = 0.32f)
            )
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF2AC3FF), AppBlueDark)
                )
            )
            .border(14.dp, AppBlue.copy(alpha = 0.045f), CircleShape)
            .clickable { },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "→",
            color = Color.White,
            fontSize = 31.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun GlassPanel(content: @Composable ColumnScope.() -> Unit) {
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
            .background(Color.White.copy(alpha = 0.88f))
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.74f),
                shape = RoundedCornerShape(28.dp)
            )
            .padding(14.dp),
        content = content
    )
}

@Composable
private fun CertificateHero() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(78.dp)
                .offset(y = 8.dp)
                .blur(18.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(AppBlue.copy(alpha = 0.11f))
        )

        Box(
            modifier = Modifier
                .size(96.dp)
                .shadow(
                    elevation = 18.dp,
                    shape = RoundedCornerShape(32.dp),
                    ambientColor = AppBlue.copy(alpha = 0.08f),
                    spotColor = AppBlue.copy(alpha = 0.16f)
                )
                .clip(RoundedCornerShape(32.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color(0xFFE9F8FF),
                            Color.White,
                            Color(0xFFEDF9FF)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.80f),
                    shape = RoundedCornerShape(32.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            CertificateIcon(
                modifier = Modifier.size(46.dp),
                color = AppBlueDark
            )
        }
    }
}

@Composable
private fun SavedCertificateBox() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFFEFF9FF).copy(alpha = 0.88f))
            .border(
                width = 1.dp,
                color = AppBlue.copy(alpha = 0.18f),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(14.dp)
    ) {
        Text(
            text = "本机已保存证书",
            color = Color(0xFF152033),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = "zhangsan · CERT-202605290001",
            color = Color(0xFF4D6177),
            fontSize = 13.sp
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(Line)
        )

        Text(
            text = text,
            color = Color(0xFFA0A7B5),
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(Line)
        )
    }
}

@Composable
private fun LoginMethodItem(
    title: String,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit
) {
    Column(
        modifier = modifier.clickable { },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .shadow(
                    elevation = 10.dp,
                    shape = RoundedCornerShape(17.dp),
                    ambientColor = Color(0xFF22304E).copy(alpha = 0.04f),
                    spotColor = Color(0xFF22304E).copy(alpha = 0.08f)
                )
                .clip(RoundedCornerShape(17.dp))
                .background(Color.White.copy(alpha = 0.82f))
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.88f),
                    shape = RoundedCornerShape(17.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            icon()
        }

        Spacer(Modifier.height(6.dp))

        Text(
            text = title,
            fontSize = 11.sp,
            color = Color(0xFF5F6878),
            maxLines = 1
        )
    }
}

@Composable
private fun BioCardUi(
    title: String,
    statusText: String,
    captured: Boolean,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .height(86.dp)
            .shadow(
                elevation = if (captured) 12.dp else 10.dp,
                shape = RoundedCornerShape(18.dp),
                ambientColor = Color(0xFF22304E).copy(alpha = if (captured) 0.04f else 0.03f),
                spotColor = if (captured) AppBlue.copy(alpha = 0.12f) else Color(0xFF22304E).copy(alpha = 0.07f)
            )
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (captured) Color(0xFFEFF9FF).copy(alpha = 0.94f)
                else Color.White.copy(alpha = 0.78f)
            )
            .border(
                width = 1.dp,
                color = if (captured) AppBlue.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.86f),
                shape = RoundedCornerShape(18.dp)
            )
            .clickable { },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        icon()

        Spacer(Modifier.height(5.dp))

        Text(
            text = title,
            color = Color(0xFF4B5565),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(3.dp))

        Text(
            text = statusText,
            color = if (captured) AppBlueDark else Color(0xFF9AA3B2),
            fontSize = 10.5.sp
        )
    }
}

@Composable
private fun PrimaryButtonUi(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .shadow(
                elevation = 18.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = AppBlue.copy(alpha = 0.14f),
                spotColor = AppBlue.copy(alpha = 0.22f)
            )
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF2AC3FF), AppBlueDark)
                )
            )
            .clickable { },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun SecondaryButtonUi(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = Color(0xFF22304E).copy(alpha = 0.03f),
                spotColor = Color(0xFF22304E).copy(alpha = 0.06f)
            )
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.82f))
            .border(
                width = 1.dp,
                color = Color(0xFFEDF1F6).copy(alpha = 0.96f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color(0xFF566174),
            fontSize = 14.sp
        )
    }
}

@Composable
private fun SegmentTabsUi() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(Color(0xFFF1F3F7).copy(alpha = 0.82f))
            .padding(5.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        SegmentButtonUi(
            text = "全部记录",
            selected = true,
            modifier = Modifier.weight(1f)
        )

        SegmentButtonUi(
            text = "审核中",
            selected = false,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SegmentButtonUi(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .shadow(
                elevation = if (selected) 8.dp else 0.dp,
                shape = RoundedCornerShape(999.dp),
                ambientColor = Color(0xFF273450).copy(alpha = 0.04f),
                spotColor = Color(0xFF273450).copy(alpha = 0.08f)
            )
            .clip(RoundedCornerShape(999.dp))
            .background(
                if (selected) Color.White.copy(alpha = 0.95f)
                else Color.Transparent
            )
            .clickable { },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (selected) Color(0xFF152033) else Color(0xFF7B8494),
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun RecordCardUi(
    username: String,
    detail: String,
    status: String,
    statusColor: Color,
    statusBg: Color,
    progress: Int,
    result: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = Color(0xFF22304E).copy(alpha = 0.04f),
                spotColor = Color(0xFF22304E).copy(alpha = 0.08f)
            )
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = 0.84f))
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.86f),
                shape = RoundedCornerShape(24.dp)
            )
            .padding(15.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = username,
                    fontSize = 15.sp,
                    color = TextMain,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = detail,
                    color = Color(0xFF8B94A5),
                    fontSize = 12.sp
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(statusBg)
                    .padding(horizontal = 9.dp, vertical = 5.dp)
            ) {
                Text(
                    text = status,
                    color = statusColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            repeat(3) { index ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(
                            if (index < progress) AppBlue
                            else Color(0xFFE4E9F2)
                        )
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        Text(
            text = result,
            color = Color(0xFF667085),
            fontSize = 12.5.sp,
            lineHeight = 18.sp
        )
    }
}

@Composable
private fun FooterTextButton(text: String) {
    Text(
        text = text,
        color = Color(0xFF8F97A6),
        fontSize = 13.sp,
        modifier = Modifier.clickable { }
    )
}

@Composable
private fun FingerprintIcon(modifier: Modifier = Modifier, color: Color) {
    Canvas(modifier = modifier) {
        val stroke = Stroke(width = 1.9.dp.toPx(), cap = StrokeCap.Round)
        drawArc(color, 200f, 140f, false, Offset(size.width * .18f, size.height * .08f), Size(size.width * .64f, size.height * .58f), style = stroke)
        drawArc(color, 190f, 160f, false, Offset(size.width * .08f, size.height * .18f), Size(size.width * .84f, size.height * .72f), style = stroke)
        drawArc(color, 210f, 120f, false, Offset(size.width * .30f, size.height * .25f), Size(size.width * .40f, size.height * .42f), style = stroke)
        drawArc(color, 210f, 120f, false, Offset.Zero, Size(size.width, size.height), style = stroke)
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
        drawCircle(color, 1.4.dp.toPx(), Offset(size.width * .36f, size.height * .44f), style = Fill)
        drawCircle(color, 1.4.dp.toPx(), Offset(size.width * .64f, size.height * .44f), style = Fill)
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
        drawLine(color, Offset(size.width * .36f, size.height * .42f), Offset(size.width * .50f, size.height * .42f), sw, StrokeCap.Round)
    }
}

@Composable
private fun EyeOffIcon(modifier: Modifier = Modifier, color: Color) {
    Canvas(modifier = modifier) {
        val sw = 1.8.dp.toPx()
        val stroke = Stroke(width = sw, cap = StrokeCap.Round)
        drawOval(
            color = color,
            topLeft = Offset(size.width * .10f, size.height * .26f),
            size = Size(size.width * .80f, size.height * .48f),
            style = stroke
        )
        drawCircle(
            color = color,
            radius = size.minDimension * .11f,
            center = Offset(size.width * .50f, size.height * .50f),
            style = Fill
        )
        drawLine(
            color = color,
            start = Offset(size.width * .12f, size.height * .82f),
            end = Offset(size.width * .88f, size.height * .18f),
            strokeWidth = sw,
            cap = StrokeCap.Round
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844, name = "登录页")
@Composable
private fun LoginPagePreview() {
    PureUiApp {
        LoginPageUi()
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844, name = "证书绑定页")
@Composable
private fun CertificateBindPagePreview() {
    PureUiApp {
        CertificateBindPageUi()
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844, name = "注册页")
@Composable
private fun RegisterPagePreview() {
    PureUiApp {
        RegisterPageUi()
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844, name = "审核记录页")
@Composable
private fun RecordsPagePreview() {
    PureUiApp {
        RecordsPageUi()
    }
}
