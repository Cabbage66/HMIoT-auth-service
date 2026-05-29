package com.hmiot.app.feature.trust.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hmiot.app.ui.components.AppPage
import com.hmiot.app.ui.components.StatusBlock

@Composable
fun TrustCenterScreen(
    factory: TrustCenterViewModelFactory,
    onBack: () -> Unit
) {
    val vm: TrustCenterViewModel = viewModel(factory = factory)
    val state by vm.state.collectAsState()
    AppPage(title = "信任中心", subtitle = "信任评估结果与风险阻断联动") {
        Column {
        Text("当前阻断状态: ${if (state.guard.blocked) "已阻断" else "未阻断"}")
        Text("原因: ${state.guard.reason.ifBlank { "-" }}")
        Text("更新时间: ${state.guard.updatedAt.ifBlank { "-" }}")
        Button(onClick = vm::runTrustCheck, modifier = Modifier.padding(top = 8.dp)) { Text("执行信任评估") }
        Button(onClick = vm::clearBlock, modifier = Modifier.padding(top = 8.dp)) { Text("手动解除阻断") }
        Button(onClick = onBack, modifier = Modifier.padding(top = 8.dp)) { Text("返回首页") }
        StatusBlock(state.info, state.error)
        }
    }
}
