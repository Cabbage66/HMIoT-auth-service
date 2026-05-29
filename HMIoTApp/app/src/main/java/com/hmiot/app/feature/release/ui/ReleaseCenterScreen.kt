package com.hmiot.app.feature.release.ui

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
import com.hmiot.app.ui.components.ListPanel

@Composable
fun ReleaseCenterScreen(
    factory: ReleaseCenterViewModelFactory,
    onBack: () -> Unit
) {
    val vm: ReleaseCenterViewModel = viewModel(factory = factory)
    val state by vm.state.collectAsState()
    AppPage(title = "上线验收中心", subtitle = "执行关键链路一键验收并输出结果") {
        Column {
        Button(onClick = vm::runChecks, modifier = Modifier.padding(top = 8.dp), enabled = !state.running) {
            Text(if (state.running) "验收中..." else "执行一键验收")
        }
        Button(onClick = onBack, modifier = Modifier.padding(top = 8.dp)) { Text("返回首页") }
        state.summary?.let { Text("SUMMARY: $it", modifier = Modifier.padding(top = 8.dp)) }
        }
        ListPanel {
            repeat(state.items.size) { idx ->
                val item = state.items[idx]
                Text("${if (item.passed) "PASS" else "FAIL"} | ${item.name} | ${item.detail}")
            }
        }
    }
}
