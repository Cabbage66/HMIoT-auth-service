import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useAdminAuthStore } from '../stores/auth'

const routes: RouteRecordRaw[] = [
  { path: '/', redirect: '/admin/login' },
  {
    path: '/admin',
    children: [
      { path: '', redirect: '/admin/login', meta: { authScope: 'none' } },
      { path: 'login', component: () => import('../views/admin/AdminLoginView.vue'), meta: { authScope: 'none' } },
      { path: 'dashboard', component: () => import('../views/admin/AdminDashboardView.vue'), meta: { authScope: 'admin' } },
      { path: 'login-logs', component: () => import('../views/admin/AdminLoginLogsView.vue'), meta: { authScope: 'admin' } },
      { path: 'registrations', component: () => import('../views/admin/AdminRegistrationsView.vue'), meta: { authScope: 'admin' } },
      { path: 'users', component: () => import('../views/admin/AdminUsersView.vue'), meta: { authScope: 'admin' } },
      { path: 'credentials', component: () => import('../views/admin/AdminCredentialsView.vue'), meta: { authScope: 'admin' } },
      { path: 'device-binding', component: () => import('../views/admin/AdminDeviceBindingView.vue'), meta: { authScope: 'admin' } },
      { path: 'policy-app', component: () => import('../views/admin/AdminPolicyAppView.vue'), meta: { authScope: 'admin' } },
      { path: 'trust-risk', component: () => import('../views/admin/AdminTrustRiskView.vue'), meta: { authScope: 'admin' } },
      { path: 'audit-center', component: () => import('../views/admin/AdminAuditCenterView.vue'), meta: { authScope: 'admin' } },
      { path: 'go-live', component: () => import('../views/admin/AdminGoLiveView.vue'), meta: { authScope: 'admin' } },
    ],
  },
  {
    path: '/web',
    children: [
      { path: '', redirect: '/web/login', meta: { authScope: 'none' } },
      { path: 'login', component: () => import('../views/web/WebLoginView.vue'), meta: { authScope: 'none' } },
      { path: 'apps', component: () => import('../views/web/WebAppsView.vue'), meta: { authScope: 'user' } },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to) => {
  const authStore = useAdminAuthStore()
  const scope = String(to.meta.authScope ?? 'none')
  if (scope === 'admin' && !authStore.isAdminAuthenticated) {
    return '/admin/login'
  }
  if (scope === 'user' && !authStore.isUserAuthenticated) {
    return '/web/login'
  }
  if (scope === 'none' && to.path === '/admin/login' && authStore.isAdminAuthenticated) {
    return '/admin/dashboard'
  }
  if (scope === 'none' && to.path === '/web/login' && authStore.isUserAuthenticated) {
    return '/web/apps'
  }
  return true
})

export default router
