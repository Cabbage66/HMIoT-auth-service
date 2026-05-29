import { httpGet, httpPost, httpPut } from './http'
import type {
  BindingListResponse,
  BindingQueryParams,
  BindingRelationVO,
  DeviceListResponse,
  DeviceQueryParams,
  DeviceEntityVO,
  TerminalListResponse,
  TerminalQueryParams,
  TerminalInfoVO,
} from '../types/device-binding'

export const queryDevices = (params: DeviceQueryParams) => httpGet<DeviceListResponse>('/devices', params, 'admin')
export const createDevice = (payload: Record<string, unknown>) => httpPost<DeviceEntityVO>('/devices', payload, 'admin')
export const updateDevice = (id: number, payload: Record<string, unknown>) => httpPut<DeviceEntityVO>(`/devices/${id}`, payload, 'admin')
export const updateDeviceStatus = (id: number, status: string) =>
  httpPost<DeviceEntityVO>(`/devices/${id}/status`, { status }, 'admin')

export const queryTerminals = (params: TerminalQueryParams) =>
  httpGet<TerminalListResponse>('/terminals', params, 'admin')
export const createTerminal = (payload: Record<string, unknown>) => httpPost<TerminalInfoVO>('/terminals', payload, 'admin')
export const updateTerminal = (id: number, payload: Record<string, unknown>) =>
  httpPut<TerminalInfoVO>(`/terminals/${id}`, payload, 'admin')
export const updateTerminalStatus = (id: number, status: string) =>
  httpPost<TerminalInfoVO>(`/terminals/${id}/status`, { status }, 'admin')

export const queryBindings = (params: BindingQueryParams) => httpGet<BindingListResponse>('/bindings', params, 'admin')
export const bindUserTerminal = (payload: { userId: number; terminalId: number; reason?: string }) =>
  httpPost<BindingRelationVO>('/bindings/user-terminal', payload, 'admin')
export const bindUserDynamicBiometric = (payload: {
  userId: number
  externalSubjectId: string
  biometricType?: string
  modelSource?: string
  reason?: string
}) => httpPost<BindingRelationVO>('/bindings/user-dynamic-biometric', payload, 'admin')
export const unbindRelation = (id: number, reason?: string) =>
  httpPost<BindingRelationVO>(`/bindings/${id}/unbind`, { reason }, 'admin')

