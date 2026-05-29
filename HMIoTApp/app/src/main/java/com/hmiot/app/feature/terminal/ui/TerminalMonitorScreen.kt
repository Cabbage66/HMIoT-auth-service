package com.hmiot.app.feature.terminal.ui

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
import com.hmiot.app.ui.components.StatusBlock

@Composable
fun TerminalMonitorScreen(
    factory: TerminalMonitorViewModelFactory,
    onBack: () -> Unit
) {
    val vm: TerminalMonitorViewModel = viewModel(factory = factory)
    val state by vm.state.collectAsState()
    AppPage(title = "终端监控中心", subtitle = "终端状态采集、上报、回读、信任联动") {
        Column {
        Button(onClick = vm::collectAndQueue, modifier = Modifier.padding(top = 8.dp)) { Text("采集并入队") }
        Button(onClick = vm::flushQueue, modifier = Modifier.padding(top = 8.dp)) { Text("上报队列") }
        Button(onClick = vm::loadRecent, modifier = Modifier.padding(top = 8.dp)) { Text("查询最近事件") }
        Button(onClick = vm::checkTrust, modifier = Modifier.padding(top = 8.dp)) { Text("触发信任检查") }
        Button(onClick = onBack, modifier = Modifier.padding(top = 8.dp)) { Text("返回首页") }
        StatusBlock(state.info, state.error)
        state.trust?.let { Text("TRUST: trusted=${it.trusted} risk=${it.riskLevel} decision=${it.decision}") }
        }
        ListPanel {
            repeat(state.pending.size) { idx ->
                val p = state.pending[idx]
                Text("待上报: ${p.eventType} ${p.eventLevel} ${p.eventTime}")
            }
            repeat(state.recent.size) { idx ->
                val r = state.recent[idx]
                Text("已上报#${r.id} ${r.eventType} ${r.eventLevel} @${r.reportedAt}")
            }
        }
    }
}
