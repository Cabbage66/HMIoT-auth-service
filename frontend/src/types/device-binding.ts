import type { PageVO, UserVO } from './user'

export type DeviceEntityVO = {
  id: number
  entityId: number | null
  deviceCode: string
  deviceName: string
  deviceType: string
  deviceFingerprint: string
  certSubject: string
  status: string
  sourceSystem: string
  description: string
  metadata: string
  createdAt: string
  updatedAt: string
}

export type TerminalInfoVO = {
  id: number
  terminalCode: string
  deviceEntityId: number | null
  userAgent: string
  osName: string
  osVersion: string
  clientType: string
  clientVersion: string
  ipAddress: string
  status: string
  lastSeenAt: string
  metadata: string
  createdAt: string
  updatedAt: string
  device: DeviceEntityVO | null
}

export type BindingRelationVO = {
  id: number
  subjectType: string
  subjectId: number
  targetType: string
  targetId: number
  relationType: string
  status: string
  effectiveAt: string
  expiredAt: string
  createdBy: number
  reason: string
  createdAt: string
  updatedAt: string
  user: UserVO | null
  terminal: TerminalInfoVO | null
}

export type DeviceQueryParams = {
  keyword?: string
  deviceType?: string
  status?: string
  sourceSystem?: string
  current?: number
  size?: number
}

export type TerminalQueryParams = {
  keyword?: string
  clientType?: string
  status?: string
  deviceEntityId?: number
  current?: number
  size?: number
}

export type BindingQueryParams = {
  subjectType?: string
  subjectId?: number
  targetType?: string
  targetId?: number
  relationType?: string
  status?: string
  current?: number
  size?: number
}

export type DeviceListResponse = PageVO<DeviceEntityVO>
export type TerminalListResponse = PageVO<TerminalInfoVO>
export type BindingListResponse = PageVO<BindingRelationVO>

