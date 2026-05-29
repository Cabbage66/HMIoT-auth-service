package com.hmiot.app.feature.certificate.ui

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hmiot.app.ui.components.StatusBlock

@Composable
fun CertificateScreen(
    factory: CertificateViewModelFactory,
    onBack: () -> Unit
) {
    val vm: CertificateViewModel = viewModel(factory = factory)
    val state by vm.state.collectAsState()
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .background(Brush.verticalGradient(listOf(Color(0xFFFBFCFF), Color.White, Color(0xFFF4F7FB))))
            .padding(horizontal = 28.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(52.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("‹", fontSize = 25.sp, color = Color(0xFF586174), modifier = Modifier.clickable { onBack() })
            Box(Modifier.background(Color(0xFFF4F7FB), RoundedCornerShape(999.dp)).padding(horizontal = 14.dp, vertical = 9.dp)) {
                Text("证书登录", color = Color(0xFF586174), fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(18.dp))
        Text("绑定证书", color = Color(0xFF121826), fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(18.dp))
        Text("由管理员根据用户名签发证书后，在本设备填写证书编号并保存。", color = Color(0xFF8B92A1), fontSize = 13.sp, lineHeight = 20.sp)
        Spacer(Modifier.height(18.dp))
        Column(
            modifier = Modifier.fillMaxWidth().background(Color.White.copy(alpha = 0.88f), RoundedCornerShape(28.dp))
                .border(1.dp, Color.White.copy(alpha = 0.74f), RoundedCornerShape(28.dp))
                .padding(14.dp)
        ) {
            UiField(state.serialNumber, vm::updateSerial, "用户证书编号")
            Spacer(Modifier.height(10.dp))
            UiField(state.fingerprint, vm::updateFingerprint, "证书指纹(SHA256)")
            Spacer(Modifier.height(10.dp))
            UiField(state.pem, vm::updatePem, "证书PEM")
            Spacer(Modifier.height(10.dp))
            Button(onClick = vm::save, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0587DC))) { Text("保存证书") }
            Spacer(Modifier.height(10.dp))
            Button(onClick = onBack, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF8FAFC), contentColor = Color(0xFF566174))) { Text("返回登录") }
            Spacer(Modifier.height(14.dp))
            Column(
                modifier = Modifier.fillMaxWidth().background(Color(0xFFEFF9FF), RoundedCornerShape(20.dp)).border(1.dp, Color(0x2E12A8FF), RoundedCornerShape(20.dp)).padding(14.dp)
            ) {
                Text("本机已保存证书", color = Color(0xFF152033), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text("${state.serialNumber.ifBlank { "-" }} · ${state.fingerprint.ifBlank { "-" }}", color = Color(0xFF4D6177), fontSize = 13.sp)
            }
            StatusBlock(state.info, state.error)
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun UiField(value: String, onChange: (String) -> Unit, placeholder: String) {
    BasicTextField(
        value = value,
        onValueChange = onChange,
        textStyle = TextStyle(color = Color(0xFF222A38), fontSize = 14.5.sp),
        singleLine = true,
        modifier = Modifier.fillMaxWidth().height(50.dp).background(Color(0xFFF6F8FB), RoundedCornerShape(16.dp)).border(1.dp, Color(0xFFEDF1F6), RoundedCornerShape(16.dp)).padding(horizontal = 15.dp),
        decorationBox = { inner ->
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) Text(placeholder, color = Color(0xFFA5ACB8), fontSize = 14.5.sp)
                inner()
            }
        }
    )
}

