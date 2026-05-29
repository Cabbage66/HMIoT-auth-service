import type { PageVO } from './user'

export type UebaHealthResponse = {
  healthy: boolean
  targetSystem: string
  modelVersion: string
}

export type TrustAssessmentResultVO = {
  id: number
  assessmentRequestId: number
  requestId: string
  userId: number
  terminalId: number
  appResourceId: number
  modelSource: string
  modelVersion: string
  trustScore: number
  trustLevel: number
  riskLevel: string
  anomaly: boolean
  anomalyIndicators: Record<string, unknown>
  alerts: Record<string, unknown>
  decision: string
  rejectReason: string
  inputPayload: Record<string, unknown>
  rawResponse: Record<string, unknown>
  requestTime: string
  assessedAt: string
}

export type TerminalEventVO = {
  id: number
  terminalId: number
  userId: number
  eventType: string
  eventLevel: string
  eventTime: string
  eventPayload: Record<string, unknown>
  normalizedPayload: Record<string, unknown>
  reportedAt: string
}

export type IotDeviceStatusVO = {
  id: number
  deviceEntityId: number
  connectStatus: string
  trustStatus: string
  lastReportAt: string
  lastPayload: string
  createdAt: string
  updatedAt: string
}

export type TrustListResponse = PageVO<TrustAssessmentResultVO>
export type TerminalEventListResponse = PageVO<TerminalEventVO>
export type IotStatusListResponse = PageVO<IotDeviceStatusVO>

