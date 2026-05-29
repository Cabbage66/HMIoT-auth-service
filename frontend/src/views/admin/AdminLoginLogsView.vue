<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { queryAuditEntries } from '../../api/audit'
import type { AuditEntry } from '../../types/audit'

const loading = ref(false)
const rows = ref<AuditEntry[]>([])

const load = async () => {
  loading.value = true
  try {
    rows.value = (await queryAuditEntries(['LOGIN'])).slice(0, 200)
  } catch (e) {
    ElMessage.error((e as { message?: string }).message ?? '登录日志加载失败')
  } finally {
    loading.value = false
  }
}

void load()
</script>

<template>
  <div class="dash-page">
    <header class="dash-header admin">
      <strong>面向多网融合的人机物电子认证服务软件管理后台</strong>
      <span>管理员 · 登录日志</span>
    </header>
    <div class="dash-layout">
      <aside class="dash-aside">
        <router-link to="/admin/dashboard" class="menu-item">概览面板</router-link>
        <div class="menu-item active">登录日志</div>
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
        <div class="crumb">管理后台 / 登录日志</div>
        <el-card class="panel-card">
          <el-table v-loading="loading" :data="rows">
            <el-table-column prop="eventTime" label="时间" min-width="170" />
            <el-table-column prop="operationType" label="操作类型" min-width="180" />
            <el-table-column prop="operationResult" label="结果" width="100" />
            <el-table-column prop="actorName" label="账号" width="160" />
            <el-table-column prop="requestIp" label="IP" width="140" />
            <el-table-column prop="traceId" label="TraceId" min-width="180" />
          </el-table>
        </el-card>
      </main>
    </div>
  </div>
</template>
