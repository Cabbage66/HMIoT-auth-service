import { httpGet, httpPost } from './http'
import type {
  CertificateBindRequest,
  CertificateIssueRequest,
  CertificateListResponse,
  CertificateOperationResponse,
  CertificateQueryParams,
  UserCertificateVO,
} from '../types/certificate'

export const queryCertificates = (params: CertificateQueryParams) =>
  httpGet<CertificateListResponse>('/certificates', params, 'admin')

export const issueCertificate = (payload: CertificateIssueRequest) =>
  httpPost<CertificateOperationResponse>('/certificates/issue', payload, 'admin')

export const bindCertificate = (payload: CertificateBindRequest) =>
  httpPost<UserCertificateVO>('/certificates/bind', payload, 'admin')

export const revokeCertificate = (id: number, revokeReason: string) =>
  httpPost<UserCertificateVO>(`/certificates/${id}/revoke`, { revokeReason }, 'admin')

