import { httpPost } from './http'
import type {
  AdminLoginRequest,
  AdminLoginResponse,
  UsbChallengeRequest,
  UsbChallengeResponse,
  UsbVerifyRequest,
  UserLoginResponse,
} from '../types/auth'

export const loginAdmin = (payload: AdminLoginRequest) =>
  httpPost<AdminLoginResponse>('/auth/admin/login', payload, 'none')

export const requestUsbChallenge = (payload: UsbChallengeRequest) =>
  httpPost<UsbChallengeResponse>('/auth/usb/challenge', payload, 'none')

export const verifyUsbLogin = (payload: UsbVerifyRequest) =>
  httpPost<UserLoginResponse>('/auth/usb/verify', payload, 'none')
