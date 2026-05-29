import axios, { AxiosError, type InternalAxiosRequestConfig } from 'axios'
import type { ApiErrorPayload, ApiResponse } from '../types/api'
import { appendRuntimeApiLog } from '../utils/runtime-monitor'

export type AuthScope = 'admin' | 'user' | 'none'

const http = axios.create({
  baseURL: '/api/v1',
  timeout: 15000,
})

const sleep = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms))

const clearSessionByScope = (scope: AuthScope) => {
  if (scope === 'admin') {
    localStorage.removeItem('admin_access_token')
    localStorage.removeItem('admin_profile')
    if (window.location.pathname !== '/admin/login') window.location.href = '/admin/login'
    return
  }
  if (scope === 'user') {
    localStorage.removeItem('user_access_token')
    localStorage.removeItem('user_profile')
    if (window.location.pathname !== '/web/login') window.location.href = '/web/login'
  }
}

http.interceptors.request.use((config: InternalAxiosRequestConfig<unknown>) => {
  const scope = String(config.headers['X-Auth-Scope'] ?? 'admin') as AuthScope
  ;(config as InternalAxiosRequestConfig<unknown> & { _scope?: AuthScope; _startAt?: number; _retryCount?: number })._scope = scope
  ;(config as InternalAxiosRequestConfig<unknown> & { _scope?: AuthScope; _startAt?: number; _retryCount?: number })._startAt =
    Date.now()
  ;(config as InternalAxiosRequestConfig<unknown> & { _scope?: AuthScope; _startAt?: number; _retryCount?: number })._retryCount =
    (config as InternalAxiosRequestConfig<unknown> & { _retryCount?: number })._retryCount ?? 0
  const token =
    scope === 'none'
      ? ''
      : scope === 'user'
        ? (localStorage.getItem('user_access_token') ?? '')
        : (localStorage.getItem('admin_access_token') ?? '')

  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  delete config.headers['X-Auth-Scope']
  return config
})

http.interceptors.response.use(
  (response) => {
    const cfg = response.config as InternalAxiosRequestConfig<unknown> & {
      _startAt?: number
      _scope?: AuthScope
    }
    const body = response.data as ApiResponse<unknown>
    if (body.code !== '0') {
      return Promise.reject({
        message: body.message,
        code: body.code,
        traceId: body.traceId,
      })
    }
    appendRuntimeApiLog({
      id: `${Date.now()}-${Math.random()}`,
      method: String(response.config.method ?? 'GET').toUpperCase(),
      url: String(response.config.url ?? ''),
      status: response.status,
      ok: true,
      durationMs: cfg._startAt ? Date.now() - cfg._startAt : 0,
      timestamp: new Date().toISOString(),
      traceId: body.traceId ?? null,
      message: body.message || 'OK',
    })
    response.data = body.data
    return response
  },
  async (error: AxiosError<ApiErrorPayload>) => {
    const cfg = (error.config ?? {}) as InternalAxiosRequestConfig<unknown> & {
      _scope?: AuthScope
      _startAt?: number
      _retryCount?: number
    }
    const method = String(cfg.method ?? 'get').toUpperCase()
    const status = Number(error.response?.status ?? 0)
    const payload = error.response?.data
    const retryable = method === 'GET' && (status === 0 || status >= 500)
    const retryCount = cfg._retryCount ?? 0
    if (retryable && retryCount < 2) {
      cfg._retryCount = retryCount + 1
      await sleep(300 * cfg._retryCount)
      return http.request(cfg)
    }

    if (status === 401) {
      clearSessionByScope(cfg._scope ?? 'admin')
    }

    appendRuntimeApiLog({
      id: `${Date.now()}-${Math.random()}`,
      method,
      url: String(cfg.url ?? ''),
      status,
      ok: false,
      durationMs: cfg._startAt ? Date.now() - cfg._startAt : 0,
      timestamp: new Date().toISOString(),
      traceId: payload?.traceId ?? null,
      message: payload?.message ?? error.message ?? '请求失败',
    })

    return Promise.reject({
      message: payload?.message ?? error.message ?? '请求失败',
      code: payload?.code ?? String(error.response?.status ?? ''),
      traceId: payload?.traceId ?? null,
    })
  },
)

export const httpPost = <T>(url: string, payload?: unknown, authScope: AuthScope = 'admin'): Promise<T> =>
  http.post(url, payload, { headers: { 'X-Auth-Scope': authScope } }).then((response) => response.data as T)

export const httpGet = <T>(
  url: string,
  params?: Record<string, unknown>,
  authScope: AuthScope = 'admin',
): Promise<T> =>
  http.get(url, { params, headers: { 'X-Auth-Scope': authScope } }).then((response) => response.data as T)

export const httpPut = <T>(url: string, payload?: unknown, authScope: AuthScope = 'admin'): Promise<T> =>
  http.put(url, payload, { headers: { 'X-Auth-Scope': authScope } }).then((response) => response.data as T)

export default http
