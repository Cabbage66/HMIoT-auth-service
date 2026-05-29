package com.hmiot.app.feature.biometric.ui

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hmiot.app.ui.components.AppPage
import com.hmiot.app.ui.components.ListPanel
import com.hmiot.app.ui.components.StatusBlock

@Composable
fun BiometricScreen(
    factory: BiometricViewModelFactory,
    onBack: () -> Unit
) {
    val vm: BiometricViewModel = viewModel(factory = factory)
    val state by vm.state.collectAsState()
    val context = LocalContext.current
    AppPage(title = "生物认证中心", subtitle = "本地生物验证与声纹/动态生物生命周期管理") {
        Column {
        OutlinedTextField(
            value = state.passphrase,
            onValueChange = vm::updatePassphrase,
            label = { Text(if (state.hasProtectionPassphrase) "生物模板保护口令" else "设置生物模板保护口令") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )
        Button(onClick = vm::setupProtectionPassphrase, modifier = Modifier.padding(top = 8.dp)) { Text("设置/更新口令保护") }
        Button(onClick = { runBiometricPrompt(context, vm) }, modifier = Modifier.padding(top = 8.dp)) {
            Text(if (state.fingerprintPassed) "本地指纹/人脸验证通过" else "本地指纹/人脸验证")
        }
        Button(onClick = vm::checkVoiceHealth, modifier = Modifier.padding(top = 8.dp)) { Text("检查声纹服务健康") }
        Button(onClick = vm::collectVoiceSample, modifier = Modifier.padding(top = 8.dp)) { Text("采集声纹样本(3秒)") }
        OutlinedTextField(
            value = state.voiceSampleBase64,
            onValueChange = vm::updateVoiceSample,
            label = { Text("声纹音频Base64") },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )
        Button(onClick = vm::enrollVoice, modifier = Modifier.padding(top = 8.dp)) { Text("声纹注册模板") }
        Button(onClick = vm::recollectVoice, modifier = Modifier.padding(top = 8.dp)) { Text("声纹模板重采") }
        Button(onClick = vm::verifyVoice, modifier = Modifier.padding(top = 8.dp)) { Text("调用声纹认证") }
        Button(onClick = vm::loadVoiceTemplates, modifier = Modifier.padding(top = 8.dp)) { Text("查询我的声纹模板") }
        Button(onClick = vm::loadVoiceAuthRecords, modifier = Modifier.padding(top = 8.dp)) { Text("查询我的声纹认证记录") }
        Button(onClick = vm::checkDynamicHealth, modifier = Modifier.padding(top = 8.dp)) { Text("检查动态生物服务健康") }
        OutlinedTextField(
            value = state.dynamicRequestId,
            onValueChange = vm::updateDynamicRequestId,
            label = { Text("动态生物requestId（可空）") },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )
        Button(onClick = vm::queryDynamic, modifier = Modifier.padding(top = 8.dp)) { Text("调用动态生物认证") }
        OutlinedTextField(
            value = state.faceRequestId,
            onValueChange = vm::updateFaceRequestId,
            label = { Text("人脸远端认证requestId（可空）") },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )
        Button(onClick = vm::verifyFaceRemote, modifier = Modifier.padding(top = 8.dp)) { Text("调用人脸远端认证") }
        Button(onClick = vm::loadDynamicAuthRecords, modifier = Modifier.padding(top = 8.dp)) { Text("查询动态生物认证记录") }
        Button(onClick = onBack, modifier = Modifier.padding(top = 8.dp)) { Text("返回首页") }
        StatusBlock(state.info, state.error)
        }
        ListPanel {
            repeat(state.voiceTemplates.size) { idx ->
                val t = state.voiceTemplates[idx]
                Text("模板#${t.id} audit=${t.auditStatus} status=${t.templateStatus} version=${t.versionNo}")
            }
            repeat(state.voiceRecords.size) { idx ->
                val r = state.voiceRecords[idx]
                Text("声纹记录#${r.id} result=${r.authResult} score=${r.score}")
            }
            repeat(state.dynamicRecords.size) { idx ->
                val r = state.dynamicRecords[idx]
                Text("动态记录#${r.id} result=${r.authResult} score=${r.score}")
            }
            repeat(state.auditLogs.size) { idx ->
                val a = state.auditLogs[idx]
                Text("留痕 ${a.action}/${a.biometricType}/${a.compareMode} ${a.result} at=${a.at}")
            }
        }
    }
}

private fun runBiometricPrompt(context: android.content.Context, vm: BiometricViewModel) {
    val activity = context as? FragmentActivity ?: run {
        vm.setFingerprintResult(false, "当前上下文不支持生物认证")
        return
    }
    val capability = BiometricManager.from(activity).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)
    if (capability != BiometricManager.BIOMETRIC_SUCCESS) {
        vm.setFingerprintResult(false, "设备不支持或未配置生物认证，code=$capability")
        return
    }
    val executor = ContextCompat.getMainExecutor(activity)
    val prompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            vm.setFingerprintResult(true, "本地指纹验证成功")
        }

        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            vm.setFingerprintResult(false, "生物认证错误($errorCode): $errString")
        }

        override fun onAuthenticationFailed() {
            vm.setFingerprintResult(false, "生物认证失败")
        }
    })
    prompt.authenticate(
        BiometricPrompt.PromptInfo.Builder()
            .setTitle("HMIoT 本地生物验证")
            .setSubtitle("用于本地辅助认证")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
            .setNegativeButtonText("取消")
            .build()
    )
}
