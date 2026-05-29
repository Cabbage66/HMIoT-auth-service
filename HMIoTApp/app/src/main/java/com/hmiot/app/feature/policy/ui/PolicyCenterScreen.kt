package com.hmiot.app.feature.policy.ui

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
fun PolicyCenterScreen(
    factory: PolicyCenterViewModelFactory,
    onBack: () -> Unit
) {
    val vm: PolicyCenterViewModel = viewModel(factory = factory)
    val state by vm.state.collectAsState()
    AppPage(title = "策略中心", subtitle = "同步策略并执行本地策略决策") {
        Column {
        Button(onClick = vm::syncPolicies, modifier = Modifier.padding(top = 8.dp)) { Text("同步策略") }
        Button(onClick = { vm.evaluateLogin("CERTIFICATE_LOGIN") }, modifier = Modifier.padding(top = 8.dp)) { Text("执行登录策略决策(CERTIFICATE_LOGIN)") }
        Button(onClick = { vm.evaluateAccess("CERTIFICATE_LOGIN", 2) }, modifier = Modifier.padding(top = 8.dp)) { Text("执行访问策略决策(trust=2)") }
        Button(onClick = onBack, modifier = Modifier.padding(top = 8.dp)) { Text("返回首页") }
        StatusBlock(state.info, state.error)
        }
        state.bundle?.let { b ->
            Text("本地策略包 user=${b.userId} terminal=${b.terminalCode} policies=${b.policies.size} pending=${b.pendingPublishRecords.size}")
            ListPanel {
                repeat(b.policies.size) { idx ->
                    val p = b.policies[idx]
                    Text("${p.sceneType} | ${p.policyName} | chain=${p.authChain.joinToString(",")}")
                }
            }
        }
    }
}
