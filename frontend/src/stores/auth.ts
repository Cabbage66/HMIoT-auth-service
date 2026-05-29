import { defineStore } from 'pinia'
import type { AdminLoginResponse, UserLoginResponse } from '../types/auth'

type AuthState = {
  adminToken: string
  userToken: string
  adminProfile: AdminLoginResponse | null
  userProfile: UserLoginResponse | null
}

export const useAdminAuthStore = defineStore('admin-auth', {
  state: (): AuthState => ({
    adminToken: localStorage.getItem('admin_access_token') ?? '',
    userToken: localStorage.getItem('user_access_token') ?? '',
    adminProfile: JSON.parse(localStorage.getItem('admin_profile') ?? 'null') as AdminLoginResponse | null,
    userProfile: JSON.parse(localStorage.getItem('user_profile') ?? 'null') as UserLoginResponse | null,
  }),
  getters: {
    isAdminAuthenticated: (state) => Boolean(state.adminToken),
    isUserAuthenticated: (state) => Boolean(state.userToken),
  },
  actions: {
    setAdminSession(session: AdminLoginResponse) {
      this.adminToken = session.token
      this.adminProfile = session
      localStorage.setItem('admin_access_token', session.token)
      localStorage.setItem('admin_profile', JSON.stringify(session))
    },
    clearAdminSession() {
      this.adminToken = ''
      this.adminProfile = null
      localStorage.removeItem('admin_access_token')
      localStorage.removeItem('admin_profile')
    },
    setUserSession(session: UserLoginResponse) {
      this.userToken = session.token
      this.userProfile = session
      localStorage.setItem('user_access_token', session.token)
      localStorage.setItem('user_profile', JSON.stringify(session))
    },
    clearUserSession() {
      this.userToken = ''
      this.userProfile = null
      localStorage.removeItem('user_access_token')
      localStorage.removeItem('user_profile')
    },
  },
})
