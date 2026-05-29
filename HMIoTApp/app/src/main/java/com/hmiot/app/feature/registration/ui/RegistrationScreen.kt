package com.hmiot.app.feature.registration.ui

import android.Manifest
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.StrokeCap
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hmiot.app.feature.biometric.domain.VoiceSampleCollector
import com.hmiot.app.ui.components.ListPanel
import com.hmiot.app.ui.components.StatusBlock
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

private val AppBlue = Color(0xFF12A8FF)
private val AppBlueDark = Color(0xFF0587DC)
private val TextMain = Color(0xFF121826)
private val TextMuted = Color(0xFF8B92A1)
private val FieldBg = Color(0xFFF6F8FB)
private val Line = Color(0xFFEDF1F6)

@Composable
fun RegistrationScreen(
    factory: RegistrationViewModelFactory,
    onBackToLogin: () -> Unit,
    onViewRecords: (String) -> Unit
) {
    val vm: RegistrationViewModel = viewModel(factory = factory)
    val state by vm.state.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var fingerprint by remember { mutableStateOf(false) }
    var face by remember { mutableStateOf(false) }
    var voice by remember { mutableStateOf(false) }
    var faceBase64 by remember { mutableStateOf("") }
    var voiceBase64 by remember { mutableStateOf("") }
    var bioInfo by remember { mutableStateOf<String?>(null) }
    var bioError by remember { mutableStateOf<String?>(null) }

    val takePhotoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap: Bitmap? ->
        if (bitmap == null) {
            bioError = "人脸采集失败：未获取到图像"
        } else {
            faceBase64 = bitmap.toJpegBase64()
            face = true
            bioInfo = "人脸采集完成"
            bioError = null
        }
    }
    val audioPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) {
            bioError = "未授予麦克风权限，无法采集声纹"
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            runCatching { VoiceSampleCollector().collectBase64Wav(3) }
                .onSuccess {
                    voiceBase64 = it
                    voice = true
                    bioInfo = "声纹采集完成"
                    bioError = null
                }
                .onFailure { ex -> bioError = "声纹采集失败：${ex.message}" }
        }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .background(Brush.verticalGradient(listOf(Color(0xFFF9FCFF), Color.White, Color(0xFFF7F9FC))))
            .padding(horizontal = 28.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(52.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("‹", fontSize = 26.sp, modifier = Modifier.clickable { onBackToLogin() })
            Box(Modifier.background(Color(0xFFF4F7FB), RoundedCornerShape(99.dp)).padding(horizontal = 14.dp, vertical = 9.dp)) {
                Text("注册申请", color = Color(0xFF586174), fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(18.dp))
        Text("用户注册", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TextMain)
        Spacer(Modifier.height(18.dp))

        FlatTextField(state.username, vm::updateUsername, "用户名")
        Spacer(Modifier.height(10.dp))
        FlatTextField(state.password, vm::updatePassword, "密码", KeyboardType.Password, PasswordVisualTransformation())
        Spacer(Modifier.height(10.dp))
        FlatTextField(state.realName, vm::updateRealName, "姓名")
        Spacer(Modifier.height(10.dp))
        FlatTextField(state.phone, vm::updatePhone, "手机号", KeyboardType.Phone)
        Spacer(Modifier.height(10.dp))
        FlatTextField(state.email, vm::updateEmail, "邮箱", KeyboardType.Email)
        Spacer(Modifier.height(10.dp))
        FlatTextField(state.idCardNo, vm::updateIdCardNo, "证件号")

        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            BioCard("指纹", fingerprint, {
                runBiometricPrompt(
                    context = context,
                    onSuccess = {
                        fingerprint = true
                        bioInfo = "指纹认证成功"
                        bioError = null
                    },
                    onFailure = { bioError = it }
                )
            }, Modifier.weight(1f)) { FingerprintFlatIcon(Modifier.size(22.dp), AppBlueDark) }
            BioCard("人脸", face, { takePhotoLauncher.launch(null) }, Modifier.weight(1f)) { FaceFlatIcon(Modifier.size(22.dp), AppBlueDark) }
            BioCard("声纹", voice, { audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }, Modifier.weight(1f)) { VoiceFlatIcon(Modifier.size(22.dp), AppBlueDark) }
        }

        Spacer(Modifier.height(12.dp))
        Button(onClick = vm::submit, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = AppBlueDark)) {
            Text("提交申请", fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(10.dp))
        Button(onClick = {
            vm.queryProgress()
            onViewRecords(state.username.trim())
        }, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF8FAFC), contentColor = Color(0xFF566174))) {
            Text("查看注册记录", fontSize = 14.sp)
        }
        StatusBlock(state.info, state.error)
        StatusBlock(bioInfo, bioError)
        ListPanel {
            repeat(state.progressRecords.size) { idx ->
                val r = state.progressRecords[idx]
                Text("${r.username} · ${r.status} · ${r.createdAt}", color = TextMain, fontSize = 13.sp)
                if (!r.rejectReason.isNullOrBlank()) Text("审核未通过：${r.rejectReason}", color = Color(0xFFC92A2A), fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

private fun runBiometricPrompt(
    context: Context,
    onSuccess: () -> Unit,
    onFailure: (String) -> Unit
) {
    val activity = context.findFragmentActivity() ?: run {
        onFailure("当前上下文不支持生物认证")
        return
    }
    val capability = BiometricManager.from(activity).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)
    if (capability != BiometricManager.BIOMETRIC_SUCCESS) {
        onFailure("设备未配置可用生物认证，code=$capability")
        return
    }
    val prompt = BiometricPrompt(activity, ContextCompat.getMainExecutor(activity), object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onSuccess()
        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) = onFailure("生物认证错误($errorCode): $errString")
        override fun onAuthenticationFailed() = onFailure("生物认证失败")
    })
    prompt.authenticate(
        BiometricPrompt.PromptInfo.Builder()
            .setTitle("生物认证采集")
            .setSubtitle("用于注册阶段生物因子确认")
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

private fun Bitmap.toJpegBase64(): String {
    val output = ByteArrayOutputStream()
    compress(Bitmap.CompressFormat.JPEG, 90, output)
    return Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
}

@Composable
private fun FlatTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    var focused by remember { mutableStateOf(false) }
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = visualTransformation,
        textStyle = TextStyle(color = Color(0xFF222A38), fontSize = 14.5.sp),
        modifier = Modifier.fillMaxWidth().height(50.dp).background(FieldBg, RoundedCornerShape(16.dp)).border(1.dp, if (focused) AppBlue.copy(alpha = 0.45f) else Line, RoundedCornerShape(16.dp)).padding(horizontal = 15.dp).onFocusChanged { focused = it.isFocused },
        decorationBox = { inner ->
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) Text(placeholder, color = Color(0xFFA5ACB8), fontSize = 14.5.sp)
                inner()
            }
        }
    )
}

@Composable
private fun BioCard(title: String, captured: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, icon: @Composable () -> Unit) {
    Column(
        modifier = modifier.height(86.dp).background(if (captured) Color(0xFFEFF9FF) else Color(0xFFF8FAFC), RoundedCornerShape(18.dp)).border(1.dp, if (captured) AppBlue.copy(alpha = 0.35f) else Line, RoundedCornerShape(18.dp)).clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        icon()
        Spacer(Modifier.height(4.dp))
        Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4B5565))
        Spacer(Modifier.height(3.dp))
        Text(if (captured) "已采集" else "未采集", fontSize = 10.5.sp, color = if (captured) AppBlueDark else Color(0xFF9AA3B2))
    }
}

@Composable private fun FingerprintFlatIcon(modifier: Modifier, color: Color) { Canvas(modifier) { drawCircle(color, radius = size.minDimension * .34f, style = Stroke(width = 1.8.dp.toPx())); drawCircle(color, radius = size.minDimension * .15f, style = Stroke(width = 1.8.dp.toPx())) } }
@Composable private fun FaceFlatIcon(modifier: Modifier, color: Color) { Canvas(modifier) { drawRoundRect(color, size = size, style = Stroke(width = 1.8.dp.toPx())); drawCircle(color, radius = 1.8.dp.toPx(), center = androidx.compose.ui.geometry.Offset(size.width * .35f, size.height * .42f), style = Fill); drawCircle(color, radius = 1.8.dp.toPx(), center = androidx.compose.ui.geometry.Offset(size.width * .65f, size.height * .42f), style = Fill); drawLine(color, androidx.compose.ui.geometry.Offset(size.width * .35f, size.height * .68f), androidx.compose.ui.geometry.Offset(size.width * .65f, size.height * .68f), 1.8.dp.toPx(), cap = StrokeCap.Round) } }
@Composable private fun VoiceFlatIcon(modifier: Modifier, color: Color) { Canvas(modifier) { val w = 1.8.dp.toPx(); listOf(.25f,.4f,.55f,.7f,.85f).forEachIndexed { i, x -> val h1 = if (i % 2 == 0) .3f else .2f; drawLine(color, androidx.compose.ui.geometry.Offset(size.width*x, size.height*(.5f-h1)), androidx.compose.ui.geometry.Offset(size.width*x, size.height*(.5f+h1)), w, cap = StrokeCap.Round) } } }
