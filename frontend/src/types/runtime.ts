export type RuntimeApiLog = {
  id: string
  method: string
  url: string
  status: number
  ok: boolean
  durationMs: number
  timestamp: string
  traceId: string | null
  message: string
}
