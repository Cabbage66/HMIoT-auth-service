export type ApiResponse<T> = {
  code: string
  message: string
  data: T
  traceId: string | null
}

export type ApiErrorPayload = {
  code?: string
  message?: string
  traceId?: string | null
}

