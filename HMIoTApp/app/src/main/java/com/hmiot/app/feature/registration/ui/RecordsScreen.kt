package com.hmiot.app.feature.registration.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hmiot.app.ui.components.StatusBlock

@Composable
fun RecordsScreen(
    factory: RegistrationViewModelFactory,
    username: String,
    onBack: () -> Unit,
    onCreateAnother: () -> Unit
) {
    val vm: RegistrationViewModel = viewModel(factory = factory)
    val state by vm.state.collectAsState()
    var onlyReviewing by remember { mutableStateOf(false) }
    LaunchedEffect(username) {
        if (username.isNotBlank()) {
            vm.updateUsername(username)
            vm.queryProgress()
        } else {
            vm.refreshLocalProgress()
        }
    }
    LaunchedEffect(state.localRecords.size, username) {
        if (username.isBlank()) {
            vm.refreshLocalProgress()
        }
    }
    val records = state.progressRecords.filter {
        if (!onlyReviewing) true else it.status == "PENDING" || it.status.contains("REVIEW", true)
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .background(Brush.verticalGradient(listOf(Color(0xFFFBFCFF), Color.White, Color(0xFFF4F7FB))))
            .padding(horizontal = 28.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(52.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("‹", fontSize = 25.sp, color = Color(0xFF586174), modifier = Modifier.clickable { onBack() })
            Box(Modifier.background(Color(0xFFF4F7FB), RoundedCornerShape(999.dp)).padding(horizontal = 14.dp, vertical = 9.dp)) {
                Text("审核中心", color = Color(0xFF586174), fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(18.dp))
        Text("注册记录", color = Color(0xFF121826), fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(18.dp))
        Row(
            Modifier.fillMaxWidth().background(Color(0xFFF1F3F7), RoundedCornerShape(999.dp)).padding(5.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            TabBtn("全部记录", !onlyReviewing, Modifier.weight(1f)) { onlyReviewing = false }
            TabBtn("审核中", onlyReviewing, Modifier.weight(1f)) { onlyReviewing = true }
        }
        Spacer(Modifier.height(16.dp))
        StatusBlock(state.info, state.error)
        if (records.isEmpty()) {
            Box(Modifier.fillMaxWidth().height(144.dp).background(Color(0xFFF7F9FC), RoundedCornerShape(26.dp)), contentAlignment = Alignment.Center) {
                Text("暂无符合条件的注册记录", color = Color(0xFF8B92A1), fontSize = 14.sp)
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                records.forEach { r ->
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F9FC)), shape = RoundedCornerShape(24.dp)) {
                        Column(Modifier.padding(15.dp)) {
                            Text(r.username, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF121826))
                            Spacer(Modifier.height(4.dp))
                            Text("${r.realName} · ${r.phone ?: "-"} · ${r.createdAt}", color = Color(0xFF8B92A1), fontSize = 12.sp)
                            Spacer(Modifier.height(8.dp))
                            Text("状态：${r.status}" + (if (!r.rejectReason.isNullOrBlank()) "，原因：${r.rejectReason}" else ""), color = Color(0xFF667085), fontSize = 12.sp)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Button(
            onClick = onCreateAnother,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF8FAFC), contentColor = Color(0xFF566174))
        ) { Text("继续提交注册申请", fontSize = 14.sp) }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun TabBtn(text: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier.height(42.dp).background(if (selected) Color.White else Color.Transparent, RoundedCornerShape(999.dp)).clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = 14.sp, color = if (selected) Color(0xFF121826) else Color(0xFF7B8494), fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
    }
}
