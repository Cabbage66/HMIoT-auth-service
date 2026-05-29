package com.hmiot.hmiot_backend;

import com.hmiot.hmiot_backend.module.audit.mapper.AuditLogMapper;
import com.hmiot.hmiot_backend.module.auth.mapper.AuthSessionMapper;
import com.hmiot.hmiot_backend.module.auth.mapper.SysAdminMapper;
import com.hmiot.hmiot_backend.module.app.mapper.AppResourceMapper;
import com.hmiot.hmiot_backend.module.biometric.mapper.BiometricAuthRecordMapper;
import com.hmiot.hmiot_backend.module.biometric.mapper.BiometricTemplateMapper;
import com.hmiot.hmiot_backend.module.certificate.mapper.UserCertificateMapper;
import com.hmiot.hmiot_backend.module.device.mapper.DeviceEntityMapper;
import com.hmiot.hmiot_backend.module.device.mapper.IotDeviceStatusMapper;
import com.hmiot.hmiot_backend.module.device.mapper.TerminalInfoMapper;
import com.hmiot.hmiot_backend.module.binding.mapper.BindingRelationMapper;
import com.hmiot.hmiot_backend.module.integration.mapper.ExternalIntegrationCallMapper;
import com.hmiot.hmiot_backend.module.policy.mapper.AuthMethodMapper;
import com.hmiot.hmiot_backend.module.policy.mapper.AuthPolicyMapper;
import com.hmiot.hmiot_backend.module.policy.mapper.PolicyPublishRecordMapper;
import com.hmiot.hmiot_backend.module.policy.mapper.SecurityBaselineMapper;
import com.hmiot.hmiot_backend.module.ticket.mapper.IdentityTicketMapper;
import com.hmiot.hmiot_backend.module.ticket.mapper.SsoRecordMapper;
import com.hmiot.hmiot_backend.module.ticket.mapper.TicketVerifyRecordMapper;
import com.hmiot.hmiot_backend.module.trust.mapper.TerminalEventMapper;
import com.hmiot.hmiot_backend.module.trust.mapper.TrustAssessmentRequestMapper;
import com.hmiot.hmiot_backend.module.trust.mapper.TrustAssessmentResultMapper;
import com.hmiot.hmiot_backend.module.user.mapper.UserAccountMapper;
import com.hmiot.hmiot_backend.module.user.mapper.UserProfileMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = {
		"spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
		"hmiot.jwt.secret=test-only-hmiot-auth-service-secret-key-change-before-production"
})
class HmiotBackendApplicationTests {

	@MockitoBean
	private SysAdminMapper sysAdminMapper;

	@MockitoBean
	private UserAccountMapper userAccountMapper;

	@MockitoBean
	private UserProfileMapper userProfileMapper;

	@MockitoBean
	private AuditLogMapper auditLogMapper;

	@MockitoBean
	private ExternalIntegrationCallMapper externalIntegrationCallMapper;

	@MockitoBean
	private UserCertificateMapper userCertificateMapper;

	@MockitoBean
	private AuthSessionMapper authSessionMapper;

	@MockitoBean
	private AppResourceMapper appResourceMapper;

	@MockitoBean
	private IdentityTicketMapper identityTicketMapper;

	@MockitoBean
	private TicketVerifyRecordMapper ticketVerifyRecordMapper;

	@MockitoBean
	private SsoRecordMapper ssoRecordMapper;

	@MockitoBean
	private DeviceEntityMapper deviceEntityMapper;

	@MockitoBean
	private TerminalInfoMapper terminalInfoMapper;

	@MockitoBean
	private IotDeviceStatusMapper iotDeviceStatusMapper;

	@MockitoBean
	private BindingRelationMapper bindingRelationMapper;

	@MockitoBean
	private AuthMethodMapper authMethodMapper;

	@MockitoBean
	private AuthPolicyMapper authPolicyMapper;

	@MockitoBean
	private SecurityBaselineMapper securityBaselineMapper;

	@MockitoBean
	private PolicyPublishRecordMapper policyPublishRecordMapper;

	@MockitoBean
	private BiometricTemplateMapper biometricTemplateMapper;

	@MockitoBean
	private BiometricAuthRecordMapper biometricAuthRecordMapper;

	@MockitoBean
	private TerminalEventMapper terminalEventMapper;

	@MockitoBean
	private TrustAssessmentRequestMapper trustAssessmentRequestMapper;

	@MockitoBean
	private TrustAssessmentResultMapper trustAssessmentResultMapper;

	@Test
	void contextLoads() {
	}

}
