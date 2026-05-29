package com.hmiot.app.feature.home.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.hmiot.app.ui.components.AppPage

@Composable
fun HomeScreen(
    onGoReleaseCenter: () -> Unit,
    onGoTrustCenter: () -> Unit,
    onGoAccessCenter: () -> Unit,
    onGoPolicyCenter: () -> Unit,
    onGoTerminalMonitor: () -> Unit,
    onGoBiometric: () -> Unit,
    onLogout: () -> Unit
) {
    AppPage(title = "控制台", subtitle = "按业务域进入对应功能中心") {
        Column {
            Button(onClick = onGoBiometric, modifier = androidx.compose.ui.Modifier.padding(top = 8.dp)) { Text("生物认证中心") }
            Button(onClick = onGoAccessCenter, modifier = androidx.compose.ui.Modifier.padding(top = 8.dp)) { Text("应用访问中心") }
            Button(onClick = onGoTrustCenter, modifier = androidx.compose.ui.Modifier.padding(top = 8.dp)) { Text("信任中心") }
            Button(onClick = onGoReleaseCenter, modifier = androidx.compose.ui.Modifier.padding(top = 8.dp)) { Text("上线验收中心") }
            Button(onClick = onGoPolicyCenter, modifier = androidx.compose.ui.Modifier.padding(top = 8.dp)) { Text("策略中心") }
            Button(onClick = onGoTerminalMonitor, modifier = androidx.compose.ui.Modifier.padding(top = 8.dp)) { Text("终端监控中心") }
            Button(onClick = onLogout, modifier = androidx.compose.ui.Modifier.padding(top = 8.dp)) { Text("退出登录") }
        }
    }
}
