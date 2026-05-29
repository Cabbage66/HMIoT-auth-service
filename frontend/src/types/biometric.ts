import type { PageVO } from './user'

export type BiometricTemplateQueryParams = {
  userId?: number
  biometricType?: string
  auditStatus?: string
  templateStatus?: string
  current?: number
  size?: number
}

export type BiometricTemplateVO = {
  id: number
  userId: number
  username: string
  realName: string
  biometricType: string
  templateDigest: string
  templateRef: string
  modelSource: string
  modelVersion: string
  threshold: number
  auditStatus: string
  templateStatus: string
  versionNo: number
  submittedAt: string
  reviewedBy: number
  reviewedAt: string
  revokedAt: string
  rejectReason: string
  metadata: string
  createdAt: string
  updatedAt: string
}

export type BiometricAuthRecordQueryParams = {
  userId?: number
  biometricType?: string
  authResult?: string
  current?: number
  size?: number
}

export type BiometricAuthRecordVO = {
  id: number
  userId: number
  username: string
  realName: string
  biometricType: string
  templateId: number
  requestId: string
  score: number
  threshold: number
  authResult: string
  failReason: string
  modelSource: string
  rawResponse: string
  createdAt: string
}

export type BiometricTemplateListResponse = PageVO<BiometricTemplateVO>
export type BiometricAuthRecordListResponse = PageVO<BiometricAuthRecordVO>

