import { httpPost } from './http'
import type { IdentityTicketVO, SsoAccessResponse, SsoValidateResponse, TicketVerifyResponse } from '../types/web-access'

export const issueTicket = (payload: {
  appCode: string
  audienceSystem?: string
  terminalId?: number
  trustLevel?: number
  trustScore?: number
  voiceprintAuthRecordId?: number
  dynamicBiometricAuthRecordId?: number
}) => httpPost<IdentityTicketVO>('/tickets/issue', payload, 'user')

export const verifyTicket = (payload: {
  ticketId: string
  ticketPayload: string
  appCode: string
  verifierSystem: string
  requiredTrustLevel?: number
}) => httpPost<TicketVerifyResponse>('/tickets/verify', payload, 'none')

export const refreshTicket = (ticketId: string) => httpPost<IdentityTicketVO>(`/tickets/${ticketId}/refresh`, {}, 'user')
export const keepAliveTicket = (ticketId: string) => httpPost<IdentityTicketVO>(`/tickets/${ticketId}/keepalive`, {}, 'user')
export const revokeTicket = (ticketId: string, reason?: string) =>
  httpPost<IdentityTicketVO>(`/tickets/${ticketId}/revoke`, { reason }, 'user')

export const ssoAccess = (payload: { appCode: string; targetSystem: string }) =>
  httpPost<SsoAccessResponse>('/sso/access', payload, 'user')

export const ssoValidate = (payload: {
  ticketId: string
  ticketPayload: string
  sourceSystem: string
  targetSystem: string
  appCode: string
  requiredTrustLevel?: number
}) => httpPost<SsoValidateResponse>('/sso/validate', payload, 'none')

export const mockBusinessAppAccess = (payload: {
  ticketId: string
  ticketPayload: string
  sourceSystem: string
  targetSystem: string
  appCode: string
  requiredTrustLevel?: number
}) => httpPost<SsoValidateResponse>('/mock-business-app/access', payload, 'none')

