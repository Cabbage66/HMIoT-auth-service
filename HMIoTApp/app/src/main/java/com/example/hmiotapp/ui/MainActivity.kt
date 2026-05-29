package com.example.hmiotapp.ui

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val AppBlue = Color(0xFF12A8FF)
private val AppBlueDark = Color(0xFF0587DC)
private val TextMain = Color(0xFF121826)
private val TextMuted = Color(0xFF8B92A1)
private val FieldBg = Color(0xFFF6F8FB)
private val Line = Color(0xFFEDF1F6)
private val Green = Color(0xFF20C997)
private val Orange = Color(0xFFF59E0B)
private val Red = Color(0xFFEF4444)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { IdentityLoginApp() }
    }
}

private enum class AppPage { Login, Register, Records }
private enum class ReviewStatus(val label: String) { Reviewing("审核中"), Passed("已通过"), Rejected("未通过") }

private data class RegisterRecord(
    val username: String,
    val name: String,
    val phone: String,
    val submitTime: String,
    val biometrics: String,
    val status: ReviewStatus,
    val result: String
)

@Composable
fun IdentityLoginApp() {
    MaterialTheme {
        var page by rememberSaveable { mutableStateOf(AppPage.Login) }
        val records = remember {
            mutableStateListOf(
                RegisterRecord("zhangsan", "张三", "138****8899", "2026-05-29 09:30", "指纹、人脸", ReviewStatus.Reviewing, "资料已提交，等待人工复核"),
                RegisterRecord("lisi", "李四", "186****1024", "2026-05-28 16:12", "指纹、人脸、声纹", ReviewStatus.Passed, "审核通过，可返回登录页登录"),
                RegisterRecord("wangwu", "王五", "159****7712", "2026-05-27 11:05", "声纹", ReviewStatus.Rejected, "审核未通过：证件号与姓名信息不一致")
            )
        }

        Surface(color = Color.White, modifier = Modifier.fillMaxSize()) {
            AppShell {
                when (page) {
                    AppPage.Login -> LoginScreen(
                        onRegisterClick = { page = AppPage.Register },
                        onRecordsClick = { page = AppPage.Records }
                    )
                    AppPage.Register -> RegisterScreen(
                        onBack = { page = AppPage.Login },
                        onViewRecords = { page = AppPage.Records },
                        onSubmit = {
                            records.add(0, it)
                            page = AppPage.Records
                        }
                    )
                    AppPage.Records -> RecordsScreen(
                        records = records,
                        onBack = { page = AppPage.Login },
                        onCreateAnother = { page = AppPage.Register }
                    )
                }
            }
        }
    }
}

@Composable
private fun AppShell(content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFFF9FCFF), Color.White, Color(0xFFF7F9FC))
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 28.dp),
        content = content
    )
}

@Composable
private fun LoginScreen(onRegisterClick: () -> Unit, onRecordsClick: () -> Unit) {
    val context = LocalContext.current
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TopBar(title = "简体中文")
        Spacer(Modifier.height(24.dp))
        QqLikeLogo()
        Spacer(Modifier.height(16.dp))
        Text("HMIoT", color = TextMain, fontSize = 32.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Spacer(Modifier.height(30.dp))

        FlatTextField(username, { username = it }, "用户名")
        Spacer(Modifier.height(14.dp))
        FlatTextField(
            value = password,
            onValueChange = { password = it },
            placeholder = "输入密码",
            keyboardType = KeyboardType.Password,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailing = {
                EyeOffIcon(
                    modifier = Modifier.size(24.dp).clickable { passwordVisible = !passwordVisible },
                    color = Color(0xFF9DA6B5)
                )
            }
        )

        Spacer(Modifier.height(24.dp))
        Box(
            modifier = Modifier
                .size(68.dp)
                .shadow(16.dp, CircleShape, spotColor = AppBlue.copy(alpha = 0.25f))
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(Color(0xFF2AC3FF), AppBlueDark)))
                .clickable { Toast.makeText(context, "这里接入登录逻辑", Toast.LENGTH_SHORT).show() },
            contentAlignment = Alignment.Center
        ) {
            Text("→", color = Color.White, fontSize = 31.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(104.dp))
        SectionTitle("更多登录方式")
        Spacer(Modifier.height(15.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LoginMethodItem("指纹", Color(0xFF2F80ED), Modifier.weight(1f)) { FingerprintIcon(Modifier.size(25.dp), Color(0xFF2F80ED)) }
            LoginMethodItem("人脸", Color(0xFF7C3AED), Modifier.weight(1f)) { FaceIcon(Modifier.size(25.dp), Color(0xFF7C3AED)) }
            LoginMethodItem("声纹", Orange, Modifier.weight(1f)) { VoiceIcon(Modifier.size(25.dp), Orange) }
            LoginMethodItem("数字证书", Color(0xFF10B981), Modifier.weight(1f)) { CertificateIcon(Modifier.size(25.dp), Color(0xFF10B981)) }
        }

        Spacer(Modifier.height(42.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            FooterTextButton("找回密码") { Toast.makeText(context, "这里接入找回密码", Toast.LENGTH_SHORT).show() }
            FooterTextButton("新用户注册", onRegisterClick)
            FooterTextButton("审核进度", onRecordsClick)
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun RegisterScreen(
    onBack: () -> Unit,
    onViewRecords: () -> Unit,
    onSubmit: (RegisterRecord) -> Unit
) {
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var idNumber by rememberSaveable { mutableStateOf("") }
    var fingerprint by rememberSaveable { mutableStateOf(false) }
    var face by rememberSaveable { mutableStateOf(false) }
    var voice by rememberSaveable { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        TopBar(title = "注册申请", onBack = onBack)
        Spacer(Modifier.height(18.dp))
        Text("用户注册", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TextMain)
        Spacer(Modifier.height(18.dp))

        FlatTextField(username, { username = it }, "用户名")
        Spacer(Modifier.height(10.dp))
        FlatTextField(password, { password = it }, "密码", KeyboardType.Password, PasswordVisualTransformation())
        Spacer(Modifier.height(10.dp))
        FlatTextField(name, { name = it }, "姓名")
        Spacer(Modifier.height(10.dp))
        FlatTextField(phone, { phone = it }, "手机号", KeyboardType.Phone)
        Spacer(Modifier.height(10.dp))
        FlatTextField(email, { email = it }, "邮箱", KeyboardType.Email)
        Spacer(Modifier.height(10.dp))
        FlatTextField(idNumber, { idNumber = it }, "证件号")

        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            BioCard("指纹", fingerprint, { fingerprint = !fingerprint }, Modifier.weight(1f)) { FingerprintIcon(Modifier.size(25.dp), AppBlueDark) }
            BioCard("人脸", face, { face = !face }, Modifier.weight(1f)) { FaceIcon(Modifier.size(25.dp), AppBlueDark) }
            BioCard("声纹", voice, { voice = !voice }, Modifier.weight(1f)) { VoiceIcon(Modifier.size(25.dp), AppBlueDark) }
        }

        Spacer(Modifier.height(12.dp))
        Button(
            onClick = {
                val biometrics = buildList {
                    if (fingerprint) add("指纹")
                    if (face) add("人脸")
                    if (voice) add("声纹")
                }.joinToString("、").ifBlank { "未采集生物信息" }

                onSubmit(
                    RegisterRecord(
                        username = username.ifBlank { "new_user" },
                        name = name.ifBlank { "新用户" },
                        phone = maskPhone(phone.ifBlank { "13800000000" }),
                        submitTime = nowText(),
                        biometrics = biometrics,
                        status = ReviewStatus.Reviewing,
                        result = "等待审核"
                    )
                )
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AppBlueDark),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp)
        ) {
            Text("提交申请", fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(10.dp))
        FlatSecondaryButton("查看注册记录", onViewRecords)
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun RecordsScreen(
    records: List<RegisterRecord>,
    onBack: () -> Unit,
    onCreateAnother: () -> Unit
) {
    var onlyReviewing by rememberSaveable { mutableStateOf(false) }
    val visibleRecords = if (onlyReviewing) records.filter { it.status == ReviewStatus.Reviewing } else records

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        TopBar(title = "审核中心", onBack = onBack)
        Spacer(Modifier.height(18.dp))
        Text("注册记录", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TextMain)
        Spacer(Modifier.height(18.dp))

        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(999.dp)).background(Color(0xFFF1F3F7)).padding(5.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterTab("全部记录", !onlyReviewing, Modifier.weight(1f)) { onlyReviewing = false }
            FilterTab("审核中", onlyReviewing, Modifier.weight(1f)) { onlyReviewing = true }
        }

        Spacer(Modifier.height(16.dp))
        if (visibleRecords.isEmpty()) EmptyState() else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                visibleRecords.forEach { RecordCard(it) }
            }
        }

        Spacer(Modifier.height(14.dp))
        FlatSecondaryButton("继续提交注册申请", onCreateAnother)
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun TopBar(title: String, onBack: (() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().height(52.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (onBack == null) Arrangement.End else Arrangement.SpaceBetween
    ) {
        if (onBack != null) {
            Box(
                Modifier.size(38.dp).clip(CircleShape).background(Color(0xFFF4F7FB)).clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Text("‹", color = Color(0xFF3D4658), fontSize = 26.sp)
            }
        }
        Box(Modifier.clip(RoundedCornerShape(99.dp)).background(Color(0xFFF4F7FB)).padding(horizontal = 14.dp, vertical = 9.dp)) {
            Text(title, color = Color(0xFF586174), fontSize = 13.sp)
        }
    }
}

@Composable
private fun QqLikeLogo() {
    Box(
        modifier = Modifier
            .size(82.dp)
            .shadow(14.dp, RoundedCornerShape(26.dp), spotColor = AppBlue.copy(alpha = 0.15f))
            .clip(RoundedCornerShape(26.dp))
            .background(Color(0xFF10131A))
    ) {
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(37.dp).background(Color.White))
        Box(Modifier.align(Alignment.TopCenter).padding(top = 20.dp).size(18.dp).clip(CircleShape).background(Color.White))
        Row(Modifier.align(Alignment.TopCenter).padding(top = 30.dp), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(Color.Black))
            Box(Modifier.size(8.dp).clip(CircleShape).background(Color.Black))
        }
        Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 23.dp).width(48.dp).height(13.dp).clip(RoundedCornerShape(99.dp)).background(Color(0xFFFF3B30)))
        Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 9.dp).width(38.dp).height(16.dp).clip(RoundedCornerShape(bottomStart = 22.dp, bottomEnd = 22.dp)).background(Color(0xFFFFC83D)))
    }
}

@Composable
private fun FlatTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailing: (@Composable () -> Unit)? = null
) {
    var focused by remember { mutableStateOf(false) }
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = visualTransformation,
        textStyle = TextStyle(color = Color(0xFF222A38), fontSize = 14.5.sp),
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(FieldBg)
            .border(1.dp, if (focused) AppBlue.copy(alpha = 0.45f) else Line, RoundedCornerShape(16.dp))
            .padding(horizontal = 15.dp)
            .onFocusChanged { focused = it.isFocused },
        decorationBox = { innerTextField ->
            Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty()) Text(placeholder, color = Color(0xFFA5ACB8), fontSize = 14.5.sp)
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
private fun SectionTitle(text: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f).height(1.dp).background(Color(0xFFEEF0F5)))
        Text(text, color = Color(0xFFA0A7B5), fontSize = 12.sp, modifier = Modifier.padding(horizontal = 12.dp))
        Box(Modifier.weight(1f).height(1.dp).background(Color(0xFFEEF0F5)))
    }
}

@Composable
private fun LoginMethodItem(title: String, color: Color, modifier: Modifier = Modifier, icon: @Composable () -> Unit) {
    Column(modifier.clickable {}, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(48.dp).clip(RoundedCornerShape(17.dp)).background(Color(0xFFF4F7FB)).border(1.dp, Color(0xFFEEF2F7), RoundedCornerShape(17.dp)),
            contentAlignment = Alignment.Center
        ) { icon() }
        Spacer(Modifier.height(6.dp))
        Text(title, fontSize = 11.sp, color = Color(0xFF5F6878), maxLines = 1)
    }
}

@Composable
private fun BioCard(title: String, captured: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, icon: @Composable () -> Unit) {
    Column(
        modifier = modifier
            .height(86.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(if (captured) Color(0xFFEFF9FF) else Color(0xFFF8FAFC))
            .border(1.dp, if (captured) AppBlue.copy(alpha = 0.35f) else Line, RoundedCornerShape(18.dp))
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        icon()
        Spacer(Modifier.height(5.dp))
        Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4B5565))
        Spacer(Modifier.height(3.dp))
        Text(if (captured) "已采集" else "未采集", fontSize = 10.5.sp, color = if (captured) AppBlueDark else Color(0xFF9AA3B2))
    }
}

@Composable
private fun RecordCard(record: RegisterRecord) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F9FC)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, Color(0xFFEEF2F7))
    ) {
        Column(Modifier.padding(15.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text(record.username, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextMain)
                    Spacer(Modifier.height(4.dp))
                    Text("${record.name} · ${record.phone} · ${record.submitTime}", color = TextMuted, fontSize = 12.sp)
                }
                StatusBadge(record.status)
            }
            Spacer(Modifier.height(12.dp))
            ProgressBars(record.status)
            Spacer(Modifier.height(10.dp))
            Text("${record.biometrics} · ${record.result}", color = Color(0xFF667085), fontSize = 12.5.sp, lineHeight = 18.sp)
        }
    }
}

@Composable
private fun StatusBadge(status: ReviewStatus) {
    val bg = when (status) {
        ReviewStatus.Reviewing -> Orange.copy(alpha = 0.12f)
        ReviewStatus.Passed -> Green.copy(alpha = 0.12f)
        ReviewStatus.Rejected -> Red.copy(alpha = 0.10f)
    }
    val fg = when (status) {
        ReviewStatus.Reviewing -> Color(0xFFB77905)
        ReviewStatus.Passed -> Color(0xFF087F5B)
        ReviewStatus.Rejected -> Color(0xFFC92A2A)
    }
    Box(Modifier.clip(RoundedCornerShape(99.dp)).background(bg).padding(horizontal = 9.dp, vertical = 5.dp)) {
        Text(status.label, color = fg, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ProgressBars(status: ReviewStatus) {
    val doneCount = when (status) {
        ReviewStatus.Reviewing -> 2
        ReviewStatus.Passed -> 3
        ReviewStatus.Rejected -> 2
    }
    Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth()) {
        repeat(3) { index ->
            Box(Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(99.dp)).background(if (index < doneCount) AppBlue else Color(0xFFE4E9F2)))
        }
    }
}

@Composable
private fun EmptyState() {
    Box(
        Modifier.fillMaxWidth().height(144.dp).clip(RoundedCornerShape(26.dp)).background(Color(0xFFF7F9FC)).border(1.dp, Color(0xFFDFE5EF), RoundedCornerShape(26.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text("暂无符合条件的注册记录", color = TextMuted, fontSize = 14.sp)
    }
}

@Composable
private fun FilterTab(text: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .height(42.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) Color.White else Color.Transparent)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = 14.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, color = if (selected) TextMain else Color(0xFF7B8494))
    }
}

@Composable
private fun FlatSecondaryButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(48.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF8FAFC), contentColor = Color(0xFF566174)),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp)
    ) { Text(text, fontSize = 14.sp) }
}

@Composable
private fun FooterTextButton(text: String, onClick: () -> Unit) {
    Text(text, color = Color(0xFF8F97A6), fontSize = 13.sp, modifier = Modifier.clickable { onClick() })
}

private fun maskPhone(phone: String): String = if (phone.length >= 7) phone.take(3) + "****" + phone.takeLast(4) else phone
private fun nowText(): String = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA).format(Date())

@Composable
private fun FingerprintIcon(modifier: Modifier = Modifier, color: Color) {
    Canvas(modifier = modifier) {
        val stroke = Stroke(width = 1.9.dp.toPx(), cap = StrokeCap.Round)
        drawArc(color, 200f, 140f, false, Offset(size.width * .18f, size.height * .08f), Size(size.width * .64f, size.height * .58f), style = stroke)
        drawArc(color, 190f, 160f, false, Offset(size.width * .08f, size.height * .18f), Size(size.width * .84f, size.height * .72f), style = stroke)
        drawArc(color, 210f, 120f, false, Offset(size.width * .30f, size.height * .25f), Size(size.width * .40f, size.height * .42f), style = stroke)
        drawLine(color, Offset(size.width * .50f, size.height * .50f), Offset(size.width * .42f, size.height * .88f), strokeWidth = 1.9.dp.toPx(), cap = StrokeCap.Round)
        drawLine(color, Offset(size.width * .62f, size.height * .54f), Offset(size.width * .56f, size.height * .82f), strokeWidth = 1.9.dp.toPx(), cap = StrokeCap.Round)
        drawLine(color, Offset(size.width * .34f, size.height * .56f), Offset(size.width * .25f, size.height * .78f), strokeWidth = 1.9.dp.toPx(), cap = StrokeCap.Round)
    }
}

@Composable
private fun FaceIcon(modifier: Modifier = Modifier, color: Color) {
    Canvas(modifier = modifier) {
        val stroke = Stroke(width = 1.9.dp.toPx(), cap = StrokeCap.Round)
        val sw = 1.9.dp.toPx()
        drawLine(color, Offset(size.width * .18f, size.height * .12f), Offset(size.width * .32f, size.height * .12f), sw, cap = StrokeCap.Round)
        drawLine(color, Offset(size.width * .12f, size.height * .18f), Offset(size.width * .12f, size.height * .32f), sw, cap = StrokeCap.Round)
        drawLine(color, Offset(size.width * .68f, size.height * .12f), Offset(size.width * .82f, size.height * .12f), sw, cap = StrokeCap.Round)
        drawLine(color, Offset(size.width * .88f, size.height * .18f), Offset(size.width * .88f, size.height * .32f), sw, cap = StrokeCap.Round)
        drawLine(color, Offset(size.width * .18f, size.height * .88f), Offset(size.width * .32f, size.height * .88f), sw, cap = StrokeCap.Round)
        drawLine(color, Offset(size.width * .12f, size.height * .82f), Offset(size.width * .12f, size.height * .68f), sw, cap = StrokeCap.Round)
        drawLine(color, Offset(size.width * .68f, size.height * .88f), Offset(size.width * .82f, size.height * .88f), sw, cap = StrokeCap.Round)
        drawLine(color, Offset(size.width * .88f, size.height * .82f), Offset(size.width * .88f, size.height * .68f), sw, cap = StrokeCap.Round)
        drawCircle(color, radius = 1.4.dp.toPx(), center = Offset(size.width * .36f, size.height * .44f), style = Fill)
        drawCircle(color, radius = 1.4.dp.toPx(), center = Offset(size.width * .64f, size.height * .44f), style = Fill)
        drawLine(color, Offset(size.width * .50f, size.height * .48f), Offset(size.width * .50f, size.height * .58f), sw, cap = StrokeCap.Round)
        drawArc(color, 30f, 120f, false, Offset(size.width * .34f, size.height * .54f), Size(size.width * .32f, size.height * .25f), style = stroke)
    }
}

@Composable
private fun VoiceIcon(modifier: Modifier = Modifier, color: Color) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val sw = 1.9.dp.toPx()
        val xs = listOf(.14f, .30f, .46f, .62f, .78f, .92f)
        val hs = listOf(.16f, .56f, .82f, .56f, .30f, .16f)
        xs.forEachIndexed { i, x ->
            val barH = h * hs[i]
            drawLine(color, Offset(w * x, h / 2 - barH / 2), Offset(w * x, h / 2 + barH / 2), sw, cap = StrokeCap.Round)
        }
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
        drawLine(color, Offset(size.width * .63f, size.height * .10f), Offset(size.width * .63f, size.height * .30f), sw, cap = StrokeCap.Round)
        drawLine(color, Offset(size.width * .63f, size.height * .30f), Offset(size.width * .80f, size.height * .30f), sw, cap = StrokeCap.Round)
        drawLine(color, Offset(size.width * .35f, size.height * .62f), Offset(size.width * .45f, size.height * .72f), sw, cap = StrokeCap.Round)
        drawLine(color, Offset(size.width * .45f, size.height * .72f), Offset(size.width * .68f, size.height * .48f), sw, cap = StrokeCap.Round)
        drawLine(color, Offset(size.width * .36f, size.height * .42f), Offset(size.width * .50f, size.height * .42f), sw, cap = StrokeCap.Round)
    }
}

@Composable
private fun EyeOffIcon(modifier: Modifier = Modifier, color: Color) {
    Canvas(modifier = modifier) {
        val sw = 1.8.dp.toPx()
        drawOval(color, Offset(size.width * .10f, size.height * .26f), Size(size.width * .80f, size.height * .48f), style = Stroke(sw, cap = StrokeCap.Round))
        drawCircle(color, radius = size.minDimension * .11f, center = Offset(size.width * .50f, size.height * .50f), style = Fill)
        drawLine(color, Offset(size.width * .12f, size.height * .82f), Offset(size.width * .88f, size.height * .18f), sw, cap = StrokeCap.Round)
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun IdentityLoginPreview() {
    IdentityLoginApp()
}
