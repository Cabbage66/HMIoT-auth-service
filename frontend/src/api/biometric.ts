import { httpGet, httpPost } from './http'
import type {
  BiometricAuthRecordListResponse,
  BiometricAuthRecordQueryParams,
  BiometricTemplateListResponse,
  BiometricTemplateQueryParams,
  BiometricTemplateVO,
} from '../types/biometric'

export const queryBiometricTemplates = (params: BiometricTemplateQueryParams) =>
  httpGet<BiometricTemplateListResponse>('/biometric-templates', params, 'admin')

export const reviewBiometricTemplate = (id: number, auditStatus: 'APPROVED' | 'REJECTED', rejectReason?: string) =>
  httpPost<BiometricTemplateVO>(`/biometric-templates/${id}/review`, { auditStatus, rejectReason }, 'admin')

export const revokeBiometricTemplate = (id: number, revokeReason: string) =>
  httpPost<BiometricTemplateVO>(`/biometric-templates/${id}/revoke`, { revokeReason }, 'admin')

export const queryBiometricAuthRecords = (params: BiometricAuthRecordQueryParams) =>
  httpGet<BiometricAuthRecordListResponse>('/biometric-auth-records', params, 'admin')

