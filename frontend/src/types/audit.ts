export type AuditSourceType = 'LOGIN' | 'CERTIFICATE' | 'BIOMETRIC' | 'TRUST' | 'TERMINAL'

export type AuditEntry = {
  id: string
  sourceType: AuditSourceType
  operationType: string
  operationResult: 'SUCCESS' | 'FAIL' | 'UNKNOWN'
  actorName: string
  actorId: string
  objectType: string
  objectId: string
  requestIp: string
  traceId: string
  failReason: string
  eventTime: string
  detail: string
}
