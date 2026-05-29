import type { PageVO } from './user'

export type CertificateQueryParams = {
  userId?: number
  username?: string
  status?: string
  current?: number
  size?: number
}

export type UserCertificateVO = {
  id: number
  userId: number
  username: string
  realName: string
  serialNumber: string
  fingerprintSha256: string
  certificatePem: string
  issuer: string
  subjectDn: string
  status: string
  issuedAt: string
  expiresAt: string
  revokedAt: string
  revokeReason: string
  createdAt: string
  updatedAt: string
}

export type CertificateIssueRequest = {
  userId?: number
  username?: string
  publicKeyPem: string
}

export type CertificateBindRequest = {
  userId?: number
  username?: string
  serialNumber: string
  fingerprintSha256: string
  certificatePem: string
  issuer?: string
  subjectDn?: string
}

export type CertificateOperationResponse = {
  certificate: UserCertificateVO
  externalCall: {
    callId: number
    callType: string
    targetSystem: string
    relatedObjectId: string
    result: string
  } | null
}

export type CertificateListResponse = PageVO<UserCertificateVO>

