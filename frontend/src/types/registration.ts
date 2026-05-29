import type { PageVO } from './user'

export type RegistrationSubmitRequest = {
  username: string
  password: string
  realName: string
  phone?: string
  email?: string
  idCardNo?: string
}

export type RegistrationSubmitResponse = {
  registrationId: number
  userId: number
  username: string
  status: string
  submittedAt: string
}

export type RegistrationProgressVO = {
  registrationId: number
  username: string
  realName: string
  phone: string
  email: string
  status: string
  rejectReason: string
  reviewedAt: string
  createdAt: string
}

export type RegistrationProgressListResponse = PageVO<RegistrationProgressVO>

export type AdminRegistrationVO = {
  registrationId: number
  userId: number
  username: string
  realName: string
  phone: string
  email: string
  idCardNo: string
  registerSource: string
  status: string
  reviewerId: number | null
  reviewedAt: string | null
  rejectReason: string | null
  createdAt: string
  updatedAt: string
}

export type AdminRegistrationListResponse = PageVO<AdminRegistrationVO>

export type OfflineRegistrationCreateRequest = {
  username: string
  password?: string
  realName: string
  phone?: string
  email?: string
  idCardNo?: string
  autoApprove?: boolean
}

export type OfflineRegistrationBatchResponse = {
  total: number
  successCount: number
  failCount: number
  items: Array<{
    username: string
    success: boolean
    message: string
    registrationId: number | null
    userId: number | null
  }>
}
