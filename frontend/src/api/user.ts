import { httpGet, httpPost, httpPut } from './http'
import type { PageVO, UserAttributeSyncResponse, UserQueryParams, UserUpdateRequest, UserVO } from '../types/user'

export const queryUsers = (params: UserQueryParams) => httpGet<PageVO<UserVO>>('/users', params, 'admin')

export const getUserDetail = (id: number) => httpGet<UserVO>(`/users/${id}`, undefined, 'admin')

export const updateUser = (id: number, payload: UserUpdateRequest) => httpPut<UserVO>(`/users/${id}`, payload, 'admin')

export const syncUserAttributes = (id: number) =>
  httpPost<UserAttributeSyncResponse>(`/users/${id}/attributes/sync`, undefined, 'admin')

