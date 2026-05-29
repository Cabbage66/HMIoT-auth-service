package com.hmiot.app.feature.auth.ui

import android.Manifest
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.media.FaceDetector
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.fragment.app.FragmentActivity
import com.hmiot.app.feature.auth.domain.LoginMode
import com.hmiot.app.feature.biometric.domain.VoiceSampleCollector
import com.hmiot.app.ui.components.StatusBlock
import kotlinx.coroutines.launch

private val AppBlue = Color(0xFF12A8FF)
private val AppBlueDark = Color(0xFF0587DC)
private val TextMain = Color(0xFF121826)
private val TextMuted = Color(0xFF8B92A1)
private val FieldBg = Color(0xFFF6F8FB)
private val Line = Color(0xFFEDF1F6)

@Composable
fun LoginScreen(
    factory: LoginViewModelFactory,
    onGoRegister: () -> Unit,
    onGoCertificateCenter: () -> Unit,
    onGoRecords: (String) -> Unit,
    onLogin: () -> Unit
) {
    val vm: LoginViewModel = viewModel(factory = factory)
    val state by vm.state.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var passwordVisible by remember { mutableStateOf(false) }
    var bioInfo by remember { mutableStateOf<String?>(null) }
    var bioError by remember { mutableStateOf<String?>(null) }

    val takePhotoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap: Bitmap? ->
        if (bitmap == null) {
            bioError = "人脸识别失败：未获取到图像"
            return@rememberLauncherForActivityResult
        }
        val faceCount = detectFaces(bitmap)
        if (faceCount > 0) {
            bioInfo = "人脸识别成功（检测到${faceCount}张人脸），正在登录..."
            bioError = null
            vm.faceLogin(onLogin)
        } else {
            bioError = "人脸识别失败：未检测到人脸"
        }
    }
    val audioPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) {
            bioError = "未授予麦克风权限，无法进行声纹识别"
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            runCatching { VoiceSampleCollector().collectBase64Wav(3) }
                .onSuccess { sample ->
                    bioInfo = "声纹样本提取完成（3秒），正在登录..."
                    bioError = null
                    vm.voiceprintLogin(sample, onLogin)
                }
                .onFailure { ex ->
                    bioError = "声纹识别失败：${ex.message}"
                }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFFF9FCFF), Color.White, Color(0xFFF7F9FC))))
            .padding(horizontal = 28.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TopBar("简体中文")
            Spacer(Modifier.height(24.dp))
            QqLikeLogo()
            Spacer(Modifier.height(16.dp))
            Text("HMIoT", color = TextMain, fontSize = 32.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(Modifier.height(30.dp))

            FlatTextField(state.username, vm::updateUsername, "用户名")
            Spacer(Modifier.height(14.dp))
            FlatTextField(
                value = state.password,
                onValueChange = vm::updatePassword,
                placeholder = "输入密码",
                keyboardType = KeyboardType.Password,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailing = {
                    EyeOffIcon(Modifier.size(24.dp).clickable { passwordVisible = !passwordVisible }, Color(0xFF9DA6B5))
                }
            )
            Spacer(Modifier.height(14.dp))
            FlatTextField(state.terminalCode, vm::updateTerminalCode, "终端编码")

            if (state.loginMode == LoginMode.CERTIFICATE) {
                Spacer(Modifier.height(10.dp))
                Button(onClick = vm::requestChallenge, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(16.dp)) { Text("获取 Challenge") }
                FlatTextField(state.signatureB64, vm::updateSignature, "签名(Base64)")
            }

            Spacer(Modifier.height(24.dp))
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .shadow(16.dp, CircleShape, spotColor = AppBlue.copy(alpha = 0.25f))
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(Color(0xFF2AC3FF), AppBlueDark)))
                    .clickable {
                        if (state.loginMode == LoginMode.CERTIFICATE) vm.verifyLogin(onLogin)
                        else {
                            vm.updateMode(LoginMode.PASSWORD)
                            vm.verifyLogin(onLogin)
                        }
                    },
                contentAlignment = Alignment.Center
            ) { Text("→", color = Color.White, fontSize = 31.sp, fontWeight = FontWeight.Bold) }

            Spacer(Modifier.height(104.dp))
            SectionTitle("更多登录方式")
            Spacer(Modifier.height(15.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LoginMethodItem("指纹", Modifier.weight(1f), onClick = {
                    runBiometricPrompt(
                        context = context,
                        onSuccess = {
                            bioInfo = "指纹识别成功，正在登录..."
                            bioError = null
                            vm.fingerprintLogin(onLogin)
                        },
                        onFailure = { msg ->
                            bioError = msg
                        }
                    )
                }) { FingerprintIcon(Modifier.size(25.dp), Color(0xFF2F80ED)) }
                LoginMethodItem("人脸", Modifier.weight(1f), onClick = {
                    takePhotoLauncher.launch(null)
                }) { FaceIcon(Modifier.size(25.dp), Color(0xFF7C3AED)) }
                LoginMethodItem("声纹", Modifier.weight(1f), onClick = {
                    audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }) { VoiceIcon(Modifier.size(25.dp), Color(0xFFF59E0B)) }
                LoginMethodItem("数字证书", Modifier.weight(1f), onClick = { vm.updateMode(LoginMode.CERTIFICATE); onGoCertificateCenter() }) { CertificateIcon(Modifier.size(25.dp), Color(0xFF10B981)) }
            }
            StatusBlock(state.info, state.error)
            StatusBlock(bioInfo, bioError)
            Spacer(Modifier.height(42.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                FooterTextButton("找回密码") {}
                FooterTextButton("新用户注册", onGoRegister)
                FooterTextButton("审核进度") { onGoRecords(state.username.trim()) }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun TopBar(title: String) {
    Row(Modifier.fillMaxWidth().height(52.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End) {
        Box(Modifier.clip(RoundedCornerShape(99.dp)).background(Color(0xFFF4F7FB)).padding(horizontal = 14.dp, vertical = 9.dp)) {
            Text(title, color = Color(0xFF586174), fontSize = 13.sp)
        }
    }
}

@Composable
private fun QqLikeLogo() {
    Box(Modifier.size(82.dp).clip(RoundedCornerShape(26.dp)).background(Color(0xFF10131A))) {
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(37.dp).background(Color.White))
        Box(Modifier.align(Alignment.TopCenter).padding(top = 20.dp).size(18.dp).clip(CircleShape).background(Color.White))
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
        modifier = Modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(16.dp)).background(FieldBg).border(1.dp, if (focused) AppBlue.copy(alpha = 0.45f) else Line, RoundedCornerShape(16.dp)).padding(horizontal = 15.dp).onFocusChanged { focused = it.isFocused },
        decorationBox = { inner ->
            Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty()) Text(placeholder, color = Color(0xFFA5ACB8), fontSize = 14.5.sp)
                    inner()
                }
                if (trailing != null) { Spacer(Modifier.size(10.dp)); trailing() }
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
private fun LoginMethodItem(title: String, modifier: Modifier = Modifier, onClick: () -> Unit = {}, icon: @Composable () -> Unit) {
    Column(modifier.clickable { onClick() }, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(48.dp).clip(RoundedCornerShape(17.dp)).background(Color(0xFFF4F7FB)).border(1.dp, Color(0xFFEEF2F7), RoundedCornerShape(17.dp)), contentAlignment = Alignment.Center) { icon() }
        Spacer(Modifier.height(6.dp))
        Text(title, fontSize = 11.sp, color = Color(0xFF5F6878), maxLines = 1)
    }
}

@Composable private fun FooterTextButton(text: String, onClick: () -> Unit) { Text(text, color = Color(0xFF8F97A6), fontSize = 13.sp, modifier = Modifier.clickable { onClick() }) }

@Composable private fun FingerprintIcon(modifier: Modifier, color: Color) { Canvas(modifier) { drawCircle(color, radius = size.minDimension * .34f, style = Stroke(width = 1.8.dp.toPx())); drawCircle(color, radius = size.minDimension * .15f, style = Stroke(width = 1.8.dp.toPx())) } }
@Composable private fun FaceIcon(modifier: Modifier, color: Color) { Canvas(modifier) { drawRoundRect(color, size = size, style = Stroke(width = 1.8.dp.toPx())); drawCircle(color, radius = 1.8.dp.toPx(), center = androidx.compose.ui.geometry.Offset(size.width * .35f, size.height * .42f), style = Fill); drawCircle(color, radius = 1.8.dp.toPx(), center = androidx.compose.ui.geometry.Offset(size.width * .65f, size.height * .42f), style = Fill); drawLine(color, androidx.compose.ui.geometry.Offset(size.width * .35f, size.height * .68f), androidx.compose.ui.geometry.Offset(size.width * .65f, size.height * .68f), 1.8.dp.toPx(), cap = StrokeCap.Round) } }
@Composable private fun VoiceIcon(modifier: Modifier, color: Color) { Canvas(modifier) { val w = 1.8.dp.toPx(); listOf(.25f,.4f,.55f,.7f,.85f).forEachIndexed { i, x -> val h1 = if (i % 2 == 0) .3f else .2f; drawLine(color, androidx.compose.ui.geometry.Offset(size.width*x, size.height*(.5f-h1)), androidx.compose.ui.geometry.Offset(size.width*x, size.height*(.5f+h1)), w, cap = StrokeCap.Round) } } }
@Composable private fun CertificateIcon(modifier: Modifier, color: Color) { Canvas(modifier) { drawRoundRect(color, style = Stroke(width = 1.8.dp.toPx())); drawLine(color, androidx.compose.ui.geometry.Offset(size.width*.25f, size.height*.35f), androidx.compose.ui.geometry.Offset(size.width*.75f, size.height*.35f), 1.8.dp.toPx(), cap = StrokeCap.Round); drawLine(color, androidx.compose.ui.geometry.Offset(size.width*.25f, size.height*.55f), androidx.compose.ui.geometry.Offset(size.width*.6f, size.height*.55f), 1.8.dp.toPx(), cap = StrokeCap.Round) } }
@Composable private fun EyeOffIcon(modifier: Modifier, color: Color) { Canvas(modifier) { drawOval(color, style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)); drawLine(color, androidx.compose.ui.geometry.Offset(size.width*.12f, size.height*.82f), androidx.compose.ui.geometry.Offset(size.width*.88f, size.height*.18f), 1.8.dp.toPx(), cap = StrokeCap.Round) } }

private fun detectFaces(bitmap: Bitmap): Int {
    val rgb565 = bitmap.copy(Bitmap.Config.RGB_565, true)
    val maxFaces = 3
    val faces = arrayOfNulls<FaceDetector.Face>(maxFaces)
    return runCatching {
        FaceDetector(rgb565.width, rgb565.height, maxFaces).findFaces(rgb565, faces)
    }.getOrDefault(0)
}

private fun runBiometricPrompt(
    context: Context,
    onSuccess: () -> Unit,
    onFailure: (String) -> Unit
) {
    val activity = context.findFragmentActivity() ?: run {
        onFailure("当前页面上下文不支持生物识别")
        return
    }
    val capability = BiometricManager.from(activity).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)
    if (capability != BiometricManager.BIOMETRIC_SUCCESS) {
        onFailure("设备未配置可用生物识别，code=$capability")
        return
    }
    val prompt = BiometricPrompt(activity, ContextCompat.getMainExecutor(activity), object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onSuccess()
        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) = onFailure("生物识别错误($errorCode): $errString")
        override fun onAuthenticationFailed() = onFailure("生物识别失败")
    })
    prompt.authenticate(
        BiometricPrompt.PromptInfo.Builder()
            .setTitle("生物识别登录")
            .setSubtitle("请使用系统指纹/人脸识别")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
            .setNegativeButtonText("取消")
            .build()
    )
}

private tailrec fun Context.findFragmentActivity(): FragmentActivity? = when (this) {
    is FragmentActivity -> this
    is ContextWrapper -> baseContext.findFragmentActivity()
    else -> null
}
