package com.hmiot.app.core

import android.content.Context
import com.hmiot.app.core.network.ApiClient
import com.hmiot.app.core.session.SessionHolder
import com.hmiot.app.core.session.SessionManager
import com.hmiot.app.core.storage.datastore.AppPreferenceStore
import com.hmiot.app.feature.auth.data.AuthApi
import com.hmiot.app.feature.auth.data.TerminalEventApi
import com.hmiot.app.feature.auth.domain.AuthRepository
import com.hmiot.app.feature.biometric.data.BiometricApi
import com.hmiot.app.feature.biometric.domain.BiometricRepository
import com.hmiot.app.feature.biometric.domain.VoiceSampleCollector
import com.hmiot.app.feature.certificate.domain.CertificateProfileRepository
import com.hmiot.app.feature.registration.data.RegistrationApi
import com.hmiot.app.feature.registration.domain.RegistrationRepository
import com.hmiot.app.feature.policy.data.PolicyApi
import com.hmiot.app.feature.policy.domain.PolicyEngine
import com.hmiot.app.feature.policy.domain.PolicyRepository
import com.hmiot.app.feature.access.data.AccessApi
import com.hmiot.app.feature.access.domain.AccessRepository
import com.hmiot.app.feature.profile.data.UserProfileApi
import com.hmiot.app.feature.profile.domain.UserProfileRepository
import com.hmiot.app.feature.trust.domain.TrustGuardRepository
import com.hmiot.app.feature.release.domain.ReleaseValidationRepository
import com.hmiot.app.feature.terminal.data.TerminalMonitorApi
import com.hmiot.app.feature.terminal.domain.TerminalMonitorRepository
import com.hmiot.app.feature.terminal.domain.TerminalPayloadCleaner
import com.hmiot.app.feature.terminal.domain.TerminalStatusCollector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AppContainer(context: Context) {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val preferenceStore = AppPreferenceStore(context)
    private val sessionManager = SessionManager(preferenceStore)
    private val authApi = ApiClient.retrofit.create(AuthApi::class.java)
    private val terminalEventApi = ApiClient.retrofit.create(TerminalEventApi::class.java)
    private val registrationApi = ApiClient.retrofit.create(RegistrationApi::class.java)
    private val biometricApi = ApiClient.retrofit.create(BiometricApi::class.java)
    private val terminalMonitorApi = ApiClient.retrofit.create(TerminalMonitorApi::class.java)
    private val policyApi = ApiClient.retrofit.create(PolicyApi::class.java)
    private val accessApi = ApiClient.retrofit.create(AccessApi::class.java)
    private val userProfileApi = ApiClient.retrofit.create(UserProfileApi::class.java)

    val authRepository = AuthRepository(authApi, terminalEventApi, sessionManager)
    val registrationRepository = RegistrationRepository(registrationApi)
    val certificateProfileRepository = CertificateProfileRepository(preferenceStore)
    val biometricRepository = BiometricRepository(biometricApi, VoiceSampleCollector(), preferenceStore)
    val terminalMonitorRepository = TerminalMonitorRepository(
        api = terminalMonitorApi,
        store = preferenceStore,
        collector = TerminalStatusCollector(context),
        cleaner = TerminalPayloadCleaner()
    )
    val policyRepository = PolicyRepository(policyApi, preferenceStore)
    val policyEngine = PolicyEngine()
    val trustGuardRepository = TrustGuardRepository(preferenceStore, terminalMonitorRepository)
    val accessRepository = AccessRepository(accessApi, policyRepository, policyEngine, trustGuardRepository)
    val userProfileRepository = UserProfileRepository(userProfileApi)
    val releaseValidationRepository = ReleaseValidationRepository(
        policyRepository = policyRepository,
        accessRepository = accessRepository,
        terminalMonitorRepository = terminalMonitorRepository,
        trustGuardRepository = trustGuardRepository
    )

    init {
        appScope.launch {
            SessionHolder.updateToken(sessionManager.session.first()?.token)
        }
    }
}
