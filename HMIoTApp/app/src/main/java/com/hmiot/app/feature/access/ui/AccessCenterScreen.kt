package com.hmiot.app.feature.access.ui

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
fun AccessCenterScreen(
    factory: AccessCenterViewModelFactory,
    onBack: () -> Unit
) {
    val vm: AccessCenterViewModel = viewModel(factory = factory)
    val state by vm.state.collectAsState()
    AppPage(title = "应用访问中心", subtitle = "访问前执行策略引导与信任检查") {
        Column {
        Button(onClick = vm::loadApps, modifier = Modifier.padding(top = 8.dp)) { Text("加载应用资源") }
        Button(onClick = onBack, modifier = Modifier.padding(top = 8.dp)) { Text("返回首页") }
        StatusBlock(state.info, state.error)
        }
        ListPanel {
            repeat(state.apps.size) { idx ->
                val app = state.apps[idx]
                Text("${app.appName}(${app.appCode}) sec=${app.securityLevel} trust=${app.requiredTrustLevel}")
                Button(onClick = { vm.startAccess(app) }, modifier = Modifier.padding(bottom = 8.dp)) {
                    Text("访问应用（策略引导）")
                }
            }
        }
    }
}
