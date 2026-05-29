<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAdminAuthStore } from '../../stores/auth'

const authStore = useAdminAuthStore()
const router = useRouter()
const adminName = computed(() => authStore.adminProfile?.displayName || authStore.adminProfile?.username || 'admin')

const logout = async () => {
  authStore.clearAdminSession()
  await router.push('/admin/login')
}
</script>

<template>
  <div class="dash-page">
    <header class="dash-header admin">
      <strong>面向多网融合的人机物电子认证服务软件管理后台</strong>
      <el-space>
        <span>管理员 · {{ adminName }}</span>
        <el-button link style="color: #fff;" @click="logout">退出</el-button>
      </el-space>
    </header>

    <div class="dash-layout">
      <aside class="dash-aside">
        <router-link to="/admin/dashboard" class="menu-item active">概览面板</router-link>
        <router-link to="/admin/login-logs" class="menu-item">登录日志</router-link>
        <router-link to="/admin/users" class="menu-item">用户管理</router-link>
        <router-link to="/admin/registrations" class="menu-item">身份注册</router-link>
        <router-link to="/admin/credentials" class="menu-item">生物凭据管理</router-link>
        <router-link to="/admin/device-binding" class="menu-item">设备与绑定</router-link>
        <router-link to="/admin/policy-app" class="menu-item">策略与应用</router-link>
        <router-link to="/admin/trust-risk" class="menu-item">信任与风险</router-link>
        <router-link to="/admin/audit-center" class="menu-item">审计中心</router-link>
        <router-link to="/admin/go-live" class="menu-item">系统联调</router-link>
        <div class="menu-item">数据资源管理</div>
      </aside>

      <main class="dash-content">
        <div class="crumb">管理后台 / 概览面板</div>
        <div class="row stats">
          <div class="stats-card g1"><div class="label">管理员ID</div><div class="num">{{ authStore.adminProfile?.adminId ?? '-' }}</div></div>
          <div class="stats-card g2"><div class="label">管理员账号</div><div class="num">{{ authStore.adminProfile?.username ?? '-' }}</div></div>
          <div class="stats-card g3"><div class="label">角色</div><div class="num">{{ authStore.adminProfile?.roleCode ?? '-' }}</div></div>
          <div class="stats-card g4"><div class="label">会话状态</div><div class="num">{{ authStore.adminToken ? '有效' : '无效' }}</div></div>
        </div>

        <div class="row panels">
          <el-card class="panel-card" header="会话信息">
            <el-descriptions :column="1" border>
              <el-descriptions-item label="管理员ID">{{ authStore.adminProfile?.adminId }}</el-descriptions-item>
              <el-descriptions-item label="账号">{{ authStore.adminProfile?.username }}</el-descriptions-item>
              <el-descriptions-item label="角色">{{ authStore.adminProfile?.roleCode }}</el-descriptions-item>
            </el-descriptions>
          </el-card>
          <el-card class="panel-card" header="功能入口">
            <div class="quick-links">
              <router-link to="/admin/users" class="quick-link">用户管理</router-link>
              <router-link to="/admin/credentials" class="quick-link">生物凭据管理</router-link>
              <router-link to="/admin/device-binding" class="quick-link">设备与绑定</router-link>
              <router-link to="/admin/policy-app" class="quick-link">策略与应用</router-link>
              <router-link to="/admin/trust-risk" class="quick-link">信任与风险</router-link>
              <router-link to="/admin/audit-center" class="quick-link">审计中心</router-link>
              <router-link to="/admin/go-live" class="quick-link">系统联调</router-link>
            </div>
          </el-card>
        </div>
      </main>
    </div>
  </div>
</template>
