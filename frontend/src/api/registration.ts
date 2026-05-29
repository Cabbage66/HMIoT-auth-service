import { httpGet, httpPost } from './http'
import type {
  AdminRegistrationListResponse,
  OfflineRegistrationBatchResponse,
  OfflineRegistrationCreateRequest,
  RegistrationProgressListResponse,
  RegistrationSubmitRequest,
  RegistrationSubmitResponse,
} from '../types/registration'

export const submitRegistration = (payload: RegistrationSubmitRequest) =>
  httpPost<RegistrationSubmitResponse>('/registrations', payload, 'none')

export const queryRegistrationProgress = (username: string, current = 1, size = 10) =>
  httpGet<RegistrationProgressListResponse>('/registrations/progress', { username, current, size }, 'none')

export const queryAdminRegistrations = (params: {
  keyword?: string
  status?: string
  current?: number
  size?: number
}) => httpGet<AdminRegistrationListResponse>('/admin/registrations', params, 'admin')

export const approveRegistration = (registrationId: number) =>
  httpPost(`/admin/registrations/${registrationId}/approve`, {}, 'admin')

export const rejectRegistration = (registrationId: number, rejectReason: string) =>
  httpPost(`/admin/registrations/${registrationId}/reject`, { rejectReason }, 'admin')

export const createOfflineRegistration = (payload: OfflineRegistrationCreateRequest) =>
  httpPost<RegistrationSubmitResponse>('/admin/registrations/offline', payload, 'admin')

export const createOfflineBatch = (items: OfflineRegistrationCreateRequest[]) =>
  httpPost<OfflineRegistrationBatchResponse>('/admin/registrations/offline/batch', { items }, 'admin')
