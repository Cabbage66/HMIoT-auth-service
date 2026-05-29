export type AdminLoginRequest = {
  username: string
  password: string
}

export type AdminLoginResponse = {
  token: string
  tokenType: string
  adminId: number
  username: string
  displayName: string
  roleCode: string
}

export type UsbChallengeRequest = {
  username: string
  terminalCode: string
  deviceFingerprint?: string
  clientType?: string
  clientVersion?: string
  osName?: string
  osVersion?: string
  userAgent?: string
  metadata?: string
}

export type UsbChallengeResponse = {
  sessionId: string
  challengeId: string
  challenge: string
  expiresInSeconds: number
  expiresAt: string
}

export type UsbVerifyRequest = {
  username: string
  sessionId: string
  challengeId: string
  signatureB64: string
  terminalCode?: string
  dynamicBiometricAuthRecordId?: number
}

export type UserLoginResponse = {
  token: string
  tokenType: string
  userId: number
  username: string
  realName: string
  authMethod: string
  terminalId: number
  terminalCode: string
  clientType: string
}

