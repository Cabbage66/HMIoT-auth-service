import { httpGet, httpPost, httpPut } from './http'
import type {
  AppListResponse,
  AppResourceVO,
  AuthMethodVO,
  AuthPolicyVO,
  BaselineListResponse,
  MethodListResponse,
  PolicyListResponse,
  PolicyPublishRecordVO,
  PublishRecordListResponse,
  SecurityBaselineVO,
} from '../types/policy-app'

export const queryApps = (params: Record<string, unknown>) => httpGet<AppListResponse>('/apps', params, 'admin')
export const createApp = (payload: Record<string, unknown>) => httpPost<AppResourceVO>('/apps', payload, 'admin')
export const updateApp = (id: number, payload: Record<string, unknown>) => httpPut<AppResourceVO>(`/apps/${id}`, payload, 'admin')
export const updateAppStatus = (id: number, status: string) => httpPost<AppResourceVO>(`/apps/${id}/status`, { status }, 'admin')

export const queryAuthMethods = (params: Record<string, unknown>) =>
  httpGet<MethodListResponse>('/auth-methods', params, 'admin')
export const createAuthMethod = (payload: Record<string, unknown>) =>
  httpPost<AuthMethodVO>('/auth-methods', payload, 'admin')
export const updateAuthMethod = (id: number, payload: Record<string, unknown>) =>
  httpPut<AuthMethodVO>(`/auth-methods/${id}`, payload, 'admin')
export const updateAuthMethodStatus = (id: number, enabled: boolean) =>
  httpPost<AuthMethodVO>(`/auth-methods/${id}/status`, { enabled }, 'admin')

export const queryAuthPolicies = (params: Record<string, unknown>) =>
  httpGet<PolicyListResponse>('/auth-policies', params, 'admin')
export const createAuthPolicy = (payload: Record<string, unknown>) =>
  httpPost<AuthPolicyVO>('/auth-policies', payload, 'admin')
export const updateAuthPolicy = (id: number, payload: Record<string, unknown>) =>
  httpPut<AuthPolicyVO>(`/auth-policies/${id}`, payload, 'admin')
export const updateAuthPolicyStatus = (id: number, status: string) =>
  httpPost<AuthPolicyVO>(`/auth-policies/${id}/status`, { status }, 'admin')
export const publishAuthPolicy = (id: number, targetType: string, targetId?: number) =>
  httpPost<PolicyPublishRecordVO>(`/auth-policies/${id}/publish`, { targetType, targetId }, 'admin')

export const queryBaselines = (params: Record<string, unknown>) =>
  httpGet<BaselineListResponse>('/security-baselines', params, 'admin')
export const createBaseline = (payload: Record<string, unknown>) =>
  httpPost<SecurityBaselineVO>('/security-baselines', payload, 'admin')
export const updateBaseline = (id: number, payload: Record<string, unknown>) =>
  httpPut<SecurityBaselineVO>(`/security-baselines/${id}`, payload, 'admin')
export const updateBaselineStatus = (id: number, status: string) =>
  httpPost<SecurityBaselineVO>(`/security-baselines/${id}/status`, { status }, 'admin')
export const publishBaseline = (id: number, targetType: string, targetId?: number) =>
  httpPost<PolicyPublishRecordVO>(`/security-baselines/${id}/publish`, { targetType, targetId }, 'admin')

export const queryPublishRecords = (params: Record<string, unknown>) =>
  httpGet<PublishRecordListResponse>('/policies/publish-records', params, 'admin')

