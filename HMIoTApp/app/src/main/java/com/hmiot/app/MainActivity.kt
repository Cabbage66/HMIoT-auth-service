package com.hmiot.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.identitylogin.InternalIdentityUiData
import com.example.identitylogin.InternalSystemScreen
import com.hmiot.app.feature.access.ui.AccessCenterScreen
import com.hmiot.app.feature.access.ui.AccessCenterViewModelFactory
import com.hmiot.app.feature.auth.ui.LoginScreen
import com.hmiot.app.feature.auth.ui.LoginViewModelFactory
import com.hmiot.app.feature.biometric.ui.BiometricScreen
import com.hmiot.app.feature.biometric.ui.BiometricViewModelFactory
import com.hmiot.app.feature.certificate.ui.CertificateScreen
import com.hmiot.app.feature.certificate.ui.CertificateViewModelFactory
import com.hmiot.app.feature.policy.ui.PolicyCenterScreen
import com.hmiot.app.feature.policy.ui.PolicyCenterViewModelFactory
import com.hmiot.app.feature.registration.ui.RegistrationScreen
import com.hmiot.app.feature.registration.ui.RegistrationViewModelFactory
import com.hmiot.app.feature.registration.ui.RecordsScreen
import com.hmiot.app.feature.release.ui.ReleaseCenterScreen
import com.hmiot.app.feature.release.ui.ReleaseCenterViewModelFactory
import com.hmiot.app.feature.terminal.ui.TerminalMonitorScreen
import com.hmiot.app.feature.terminal.ui.TerminalMonitorViewModelFactory
import com.hmiot.app.feature.trust.ui.TrustCenterScreen
import com.hmiot.app.feature.trust.ui.TrustCenterViewModelFactory
import com.hmiot.app.ui.theme.HMIoTAppTheme
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HMIoTAppTheme { AppEntry() }
        }
    }
}

@Composable
private fun AppEntry() {
    val app = androidx.compose.ui.platform.LocalContext.current.applicationContext as HmiotApplication
    val authRepository = app.appContainer.authRepository
    val registrationRepository = app.appContainer.registrationRepository
    val certificateProfileRepository = app.appContainer.certificateProfileRepository
    val biometricRepository = app.appContainer.biometricRepository
    val terminalMonitorRepository = app.appContainer.terminalMonitorRepository
    val policyRepository = app.appContainer.policyRepository
    val policyEngine = app.appContainer.policyEngine
    val accessRepository = app.appContainer.accessRepository
    val trustGuardRepository = app.appContainer.trustGuardRepository
    val releaseRepository = app.appContainer.releaseValidationRepository

    val loginFactory = LoginViewModelFactory(authRepository)
    val registrationFactory = RegistrationViewModelFactory(registrationRepository)
    val certificateFactory = CertificateViewModelFactory(certificateProfileRepository)
    val biometricFactory = BiometricViewModelFactory(biometricRepository)
    val terminalMonitorFactory = TerminalMonitorViewModelFactory(terminalMonitorRepository)
    val policyFactory = PolicyCenterViewModelFactory(policyRepository, policyEngine)
    val accessFactory = AccessCenterViewModelFactory(accessRepository)
    val trustFactory = TrustCenterViewModelFactory(trustGuardRepository)
    val releaseFactory = ReleaseCenterViewModelFactory(releaseRepository)

    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    val session by authRepository.session.collectAsState(initial = null)

    Scaffold(modifier = Modifier.fillMaxSize()) {
        NavHost(navController = navController, startDestination = "login") {
            composable("login") {
                LoginScreen(
                    factory = loginFactory,
                    onGoRegister = { navController.navigate("register") },
                    onGoCertificateCenter = { navController.navigate("certificate") },
                    onGoRecords = { username -> navController.navigate("records/${username.ifBlank { "_" }}") },
                    onLogin = { navController.navigate("home") }
                )
            }
            composable("register") {
                RegistrationScreen(
                    factory = registrationFactory,
                    onBackToLogin = { navController.navigate("login") },
                    onViewRecords = { username -> navController.navigate("records/${username.ifBlank { "_" }}") }
                )
            }
            composable("records/{username}") { backStackEntry ->
                val username = backStackEntry.arguments?.getString("username").orEmpty().let { if (it == "_") "" else it }
                RecordsScreen(
                    factory = registrationFactory,
                    username = username,
                    onBack = { navController.navigate("login") },
                    onCreateAnother = { navController.navigate("register") }
                )
            }
            composable("home") {
                InternalSystemScreen(
                    profile = InternalIdentityUiData(
                        username = session?.username ?: "zhangsan",
                        realName = session?.realName ?: "张三",
                        certificateNo = session?.let { "TERMINAL-${it.terminalCode}" } ?: "CERT-202605290001"
                    ),
                    onLogout = {
                        scope.launch {
                            authRepository.logout()
                            navController.navigate("login")
                        }
                    },
                    onGoAccessCenter = { navController.navigate("access-center") },
                    onGoTrustCenter = { navController.navigate("trust-center") },
                    onGoReleaseCenter = { navController.navigate("release-center") },
                    onGoPolicyCenter = { navController.navigate("policy-center") },
                    onGoTerminalMonitor = { navController.navigate("terminal-monitor") },
                    onGoBiometric = { navController.navigate("biometric") },
                    onGoCertificateCenter = { navController.navigate("certificate") }
                )
            }
            composable("certificate") { CertificateScreen(factory = certificateFactory, onBack = { navController.navigate("login") }) }
            composable("biometric") { BiometricScreen(factory = biometricFactory, onBack = { navController.navigate("home") }) }
            composable("terminal-monitor") { TerminalMonitorScreen(factory = terminalMonitorFactory, onBack = { navController.navigate("home") }) }
            composable("policy-center") { PolicyCenterScreen(factory = policyFactory, onBack = { navController.navigate("home") }) }
            composable("access-center") { AccessCenterScreen(factory = accessFactory, onBack = { navController.navigate("home") }) }
            composable("trust-center") { TrustCenterScreen(factory = trustFactory, onBack = { navController.navigate("home") }) }
            composable("release-center") { ReleaseCenterScreen(factory = releaseFactory, onBack = { navController.navigate("home") }) }
        }
    }
}
