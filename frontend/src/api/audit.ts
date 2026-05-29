import { queryBiometricAuthRecords } from './biometric'
import { queryCertificates } from './certificate'
import { queryTrustAssessments, queryTerminalEvents } from './trust-risk'
import type { AuditEntry, AuditSourceType } from '../types/audit'
import { queryUsers } from './user'

const toJson = (value: unknown) => {
  try {
    return value == null ? '' : JSON.stringify(value)
  } catch {
    return ''
  }
}

export const queryAuditEntries = async (
  sourceTypes: AuditSourceType[],
  keyword?: string,
  fromTime?: string,
  toTime?: string,
): Promise<AuditEntry[]> => {
  const include = (type: AuditSourceType) => sourceTypes.length === 0 || sourceTypes.includes(type)
  const tasks: Promise<AuditEntry[]>[] = []

  if (include('LOGIN')) {
    tasks.push(
      queryUsers({ current: 1, size: 50, keyword }).then((res) =>
        res.records.map((u) => ({
          id: `LOGIN-${u.id}-${u.updatedAt}`,
          sourceType: 'LOGIN' as const,
          operationType: 'USER_PROFILE_VIEW_OR_UPDATE',
          operationResult: 'SUCCESS' as const,
          actorName: 'ADMIN',
          actorId: '-',
          objectType: 'USER_ACCOUNT',
          objectId: String(u.id),
          requestIp: '-',
          traceId: '-',
          failReason: '',
          eventTime: u.updatedAt || u.createdAt,
          detail: toJson({ username: u.username, realName: u.realName, status: u.status }),
        })),
      ),
    )
  }

  if (include('CERTIFICATE')) {
    tasks.push(
      queryCertificates({ current: 1, size: 100, username: keyword }).then((res) =>
        res.records.map((c) => ({
          id: `CERT-${c.id}`,
          sourceType: 'CERTIFICATE' as const,
          operationType: c.status === 'REVOKED' ? 'CERTIFICATE_REVOKE' : 'CERTIFICATE_ISSUE_OR_BIND',
          operationResult: 'SUCCESS' as const,
          actorName: c.username || '-',
          actorId: String(c.userId ?? '-'),
          objectType: 'USER_CERTIFICATE',
          objectId: String(c.id),
          requestIp: '-',
          traceId: '-',
          failReason: '',
          eventTime: c.updatedAt || c.createdAt,
          detail: toJson({ serialNumber: c.serialNumber, status: c.status, revokeReason: c.revokeReason }),
        })),
      ),
    )
  }

  if (include('BIOMETRIC')) {
    tasks.push(
      queryBiometricAuthRecords({ current: 1, size: 100 }).then((res) =>
        res.records.map((b) => ({
          id: `BIO-${b.id}`,
          sourceType: 'BIOMETRIC' as const,
          operationType: `BIOMETRIC_${b.biometricType}_AUTH`,
          operationResult: b.authResult === 'PASS' || b.authResult === 'SUCCESS' ? 'SUCCESS' : 'FAIL',
          actorName: b.username || '-',
          actorId: String(b.userId ?? '-'),
          objectType: 'BIOMETRIC_AUTH_RECORD',
          objectId: String(b.id),
          requestIp: '-',
          traceId: b.requestId || '-',
          failReason: b.failReason || '',
          eventTime: b.createdAt,
          detail: toJson({ score: b.score, threshold: b.threshold, modelSource: b.modelSource }),
        })),
      ),
    )
  }

  if (include('TRUST')) {
    tasks.push(
      queryTrustAssessments({ pageCurrent: 1, pageSize: 100 }).then((res) =>
        res.records.map((t) => ({
          id: `TRUST-${t.id}`,
          sourceType: 'TRUST' as const,
          operationType: 'TRUST_ASSESSMENT',
          operationResult: t.decision === 'ALLOW' ? 'SUCCESS' : 'FAIL',
          actorName: String(t.userId ?? '-'),
          actorId: String(t.userId ?? '-'),
          objectType: 'TRUST_ASSESSMENT',
          objectId: String(t.id),
          requestIp: '-',
          traceId: t.requestId || '-',
          failReason: t.rejectReason || '',
          eventTime: t.assessedAt || t.requestTime,
          detail: toJson({ riskLevel: t.riskLevel, trustLevel: t.trustLevel, trustScore: t.trustScore }),
        })),
      ),
    )
  }

  if (include('TERMINAL')) {
    tasks.push(
      queryTerminalEvents({ pageCurrent: 1, pageSize: 100 }).then((res) =>
        res.records.map((e) => ({
          id: `TERMINAL-${e.id}`,
          sourceType: 'TERMINAL' as const,
          operationType: `TERMINAL_EVENT_${e.eventType}`,
          operationResult: e.eventLevel === 'HIGH' || e.eventLevel === 'CRITICAL' ? 'FAIL' : 'SUCCESS',
          actorName: String(e.userId ?? '-'),
          actorId: String(e.userId ?? '-'),
          objectType: 'TERMINAL_EVENT',
          objectId: String(e.id),
          requestIp: '-',
          traceId: '-',
          failReason: '',
          eventTime: e.eventTime || e.reportedAt,
          detail: toJson({ terminalId: e.terminalId, eventLevel: e.eventLevel }),
        })),
      ),
    )
  }

  const merged = (await Promise.all(tasks)).flat()
  let rows = merged.sort((a, b) => new Date(b.eventTime).getTime() - new Date(a.eventTime).getTime())
  if (keyword) {
    rows = rows.filter(
      (row) =>
        row.actorName.includes(keyword) ||
        row.actorId.includes(keyword) ||
        row.objectId.includes(keyword) ||
        row.operationType.includes(keyword) ||
        row.detail.includes(keyword),
    )
  }
  if (fromTime) {
    const from = new Date(fromTime).getTime()
    rows = rows.filter((row) => new Date(row.eventTime).getTime() >= from)
  }
  if (toTime) {
    const to = new Date(toTime).getTime()
    rows = rows.filter((row) => new Date(row.eventTime).getTime() <= to)
  }
  return rows
}
