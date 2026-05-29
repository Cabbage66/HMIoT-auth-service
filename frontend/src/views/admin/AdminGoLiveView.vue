<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { queryUsers } from '../../api/user'
import { queryCertificates } from '../../api/certificate'
import { queryBiometricAuthRecords } from '../../api/biometric'
import { queryDevices, queryBindings } from '../../api/device-binding'
import { queryAuthPolicies } from '../../api/policy-app'
import { getUebaHealth, queryTrustAssessments } from '../../api/trust-risk'
import { queryAuditEntries } from '../../api/audit'
import { clearRuntimeApiLogs, runtimeApiLogs } from '../../utils/runtime-monitor'

type CheckItem = {
  key: string
  name: string
  status: 'PENDING' | 'RUNNING' | 'PASS' | 'FAIL'
  durationMs: number
  message: string
}

const checks = ref<CheckItem[]>([
  { key: 'user', name: '用户管理接口', status: 'PENDING', durationMs: 0, message: '-' },
  { key: 'credential', name: '证书与生物接口', status: 'PENDING', durationMs: 0, message: '-' },
  { key: 'device', name: '设备与绑定接口', status: 'PENDING', durationMs: 0, message: '-' },
  { key: 'policy', name: '策略接口', status: 'PENDING', durationMs: 0, message: '-' },
  { key: 'trust', name: '信任评估接口', status: 'PENDING', durationMs: 0, message: '-' },
  { key: 'audit', name: '审计聚合接口', status: 'PENDING', durationMs: 0, message: '-' },
])

const running = ref(false)

const runOne = async (item: CheckItem) => {
  item.status = 'RUNNING'
  item.message = '-'
  const start = Date.now()
  try {
    if (item.key === 'user') {
      const res = await queryUsers({ current: 1, size: 1 })
      item.message = `OK records=${res.records.length}`
    }
    if (item.key === 'credential') {
      const [certRes, bioRes] = await Promise.all([
        queryCertificates({ current: 1, size: 1 }),
        queryBiometricAuthRecords({ current: 1, size: 1 }),
      ])
      item.message = `OK cert=${certRes.total} bio=${bioRes.total}`
    }
    if (item.key === 'device') {
      const [deviceRes, bindRes] = await Promise.all([
        queryDevices({ current: 1, size: 1 }),
        queryBindings({ current: 1, size: 1 }),
      ])
      item.message = `OK device=${deviceRes.total} binding=${bindRes.total}`
    }
    if (item.key === 'policy') {
      const res = await queryAuthPolicies({ current: 1, size: 1 })
      item.message = `OK policy=${res.total}`
    }
    if (item.key === 'trust') {
      const [health, trust] = await Promise.all([getUebaHealth(), queryTrustAssessments({ pageCurrent: 1, pageSize: 1 })])
      item.message = `OK ueba=${health.healthy ? 'UP' : 'DOWN'} trust=${trust.total}`
    }
    if (item.key === 'audit') {
      const rows = await queryAuditEntries([], undefined, undefined, undefined)
      item.message = `OK audit=${rows.length}`
    }
    item.status = 'PASS'
  } catch (e) {
    item.status = 'FAIL'
    item.message = (e as { message?: string; traceId?: string }).traceId
      ? `${(e as { message?: string }).message} | traceId=${(e as { traceId?: string }).traceId}`
      : (e as { message?: string }).message ?? 'FAIL'
  } finally {
    item.durationMs = Date.now() - start
  }
}

const runAll = async () => {
  if (running.value) return
  running.value = true
  try {
    for (const item of checks.value) {
      await runOne(item)
    }
    if (checks.value.every((x) => x.status === 'PASS')) ElMessage.success('检查通过')
    else ElMessage.warning('检查存在失败项')
  } finally {
    running.value = false
  }
}
</script>

<template>
  <div class="dash-page">
    <header class="dash-header admin">
      <strong>面向多网融合的人机物电子认证服务软件管理后台</strong>
      <span>管理员 · 系统联调</span>
    </header>
    <div class="dash-layout">
      <aside class="dash-aside">
        <router-link to="/admin/dashboard" class="menu-item">概览面板</router-link>
        <router-link to="/admin/login-logs" class="menu-item">登录日志</router-link>
        <router-link to="/admin/users" class="menu-item">用户管理</router-link>
        <router-link to="/admin/registrations" class="menu-item">身份注册</router-link>
        <router-link to="/admin/credentials" class="menu-item">生物凭据管理</router-link>
        <router-link to="/admin/device-binding" class="menu-item">设备与绑定</router-link>
        <router-link to="/admin/policy-app" class="menu-item">策略与应用</router-link>
        <router-link to="/admin/trust-risk" class="menu-item">信任与风险</router-link>
        <router-link to="/admin/audit-center" class="menu-item">审计中心</router-link>
        <div class="menu-item active">系统联调</div>
        <div class="menu-item">数据资源管理</div>
      </aside>
      <main class="dash-content">
        <div class="crumb">管理后台 / 系统联调</div>

        <section class="toolbar">
          <el-space>
            <el-button type="primary" :loading="running" @click="runAll">执行检查</el-button>
            <el-button :disabled="running" @click="clearRuntimeApiLogs">清空运行日志</el-button>
            <router-link to="/web/apps" class="menu-item" style="height:auto;line-height:normal;">网页客户端入口</router-link>
          </el-space>
        </section>

        <el-card class="panel-card" header="检查结果">
          <el-table :data="checks">
            <el-table-column prop="name" label="检查项" min-width="220" />
            <el-table-column prop="status" label="状态" width="120" />
            <el-table-column prop="durationMs" label="耗时(ms)" width="120" />
            <el-table-column prop="message" label="结果信息" min-width="420" />
          </el-table>
        </el-card>

        <el-card class="panel-card" header="API运行日志（最近200条）" style="margin-top:12px;">
          <el-table :data="runtimeApiLogs">
            <el-table-column prop="timestamp" label="时间" min-width="180" />
            <el-table-column prop="method" label="方法" width="90" />
            <el-table-column prop="url" label="URL" min-width="220" />
            <el-table-column prop="status" label="状态码" width="90" />
            <el-table-column prop="durationMs" label="耗时(ms)" width="100" />
            <el-table-column prop="traceId" label="traceId" min-width="180" />
            <el-table-column prop="message" label="消息" min-width="220" />
          </el-table>
        </el-card>
      </main>
    </div>
  </div>
</template>
