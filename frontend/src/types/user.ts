export type PageVO<T> = {
  current: number
  size: number
  total: number
  records: T[]
}

export type UserProfileVO = {
  department: string
  positionName: string
  attributeVersion: string
  externalSource: string
  externalUserId: string
  externalAttributes: string
  syncedAt: string
}

export type UserVO = {
  id: number
  username: string
  realName: string
  phone: string
  email: string
  idCardNo: string
  status: string
  registerSource: string
  createdAt: string
  updatedAt: string
  profile: UserProfileVO | null
}

export type UserQueryParams = {
  keyword?: string
  status?: string
  current?: number
  size?: number
}

export type UserUpdateRequest = {
  realName: string
  phone?: string
  email?: string
  idCardNo?: string
  status?: string
}

export type UserAttributeSyncResponse = {
  user: UserVO
  beforeProfile: UserProfileVO | null
  afterProfile: UserProfileVO | null
  externalCall: {
    callId: number
    callType: string
    targetSystem: string
    relatedObjectId: string
    result: string
  } | null
}

