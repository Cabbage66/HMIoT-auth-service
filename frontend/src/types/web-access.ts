export type IdentityTicketVO = {
  ticketId: string
  ticketPayload: string
  userId: number
  username: string
  realName: string
  terminalId: number
  terminalCode: string
  clientType: string
  issuerSystem: string
  audienceSystem: string
  appCode: string
  appResourceId: number
  trustLevel: number
  trustScore: number
  authMethod: string
  status: string
  issuedAt: string
  expiresAt: string
  expiresInSeconds: number
}

export type TicketVerifyResponse = {
  verified: boolean
  resultCode: string
  message: string
  ticketId: string
  userId: number | null
  username: string | null
  realName: string | null
  issuerSystem: string | null
  audienceSystem: string | null
  verifierSystem: string
  appCode: string
  trustLevel: number | null
  trustScore: number | null
}

export type SsoAccessResponse = {
  success: boolean
  message: string
  sourceSystem: string
  targetSystem: string
  appCode: string
  accessUrl: string
  ticket: IdentityTicketVO
}

export type SsoValidateResponse = {
  success: boolean
  message: string
  sourceSystem: string
  targetSystem: string
  appCode: string
  ticketVerify: TicketVerifyResponse
}

