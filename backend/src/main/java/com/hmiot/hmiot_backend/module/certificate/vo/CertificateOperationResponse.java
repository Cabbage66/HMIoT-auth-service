package com.hmiot.hmiot_backend.module.certificate.vo;

import com.hmiot.hmiot_backend.module.user.vo.ExternalCallSummaryVO;

public record CertificateOperationResponse(
        UserCertificateVO certificate,
        ExternalCallSummaryVO externalCall
) {
}
