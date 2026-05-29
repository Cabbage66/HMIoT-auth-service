import { httpGet, httpPost } from './http'
import type { IotStatusListResponse, TerminalEventListResponse, TrustAssessmentResultVO, TrustListResponse, UebaHealthResponse } from '../types/trust-risk'

export const getUebaHealth = () => httpGet<UebaHealthResponse>('/trust-assessments/ueba/health', undefined, 'admin')
export const queryTrustAssessments = (params: Record<string, unknown>) =>
  httpGet<TrustListResponse>('/trust-assessments', params, 'admin')
export const getTrustAssessmentDetail = (id: number) =>
  httpGet<TrustAssessmentResultVO>(`/trust-assessments/${id}`, undefined, 'admin')

export const queryTerminalEvents = (params: Record<string, unknown>) =>
  httpGet<TerminalEventListResponse>('/terminal-events', params, 'admin')

export const queryIotStatuses = (params: Record<string, unknown>) =>
  httpGet<IotStatusListResponse>('/iot-device-statuses', params, 'admin')
export const refreshIotStatus = (deviceEntityId: number, requestId?: string) =>
  httpPost(`/devices/${deviceEntityId}/iot-status/query`, { requestId }, 'admin')

