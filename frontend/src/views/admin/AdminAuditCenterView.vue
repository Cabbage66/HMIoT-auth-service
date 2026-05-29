<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { queryAuditEntries } from '../../api/audit'
import type { AuditEntry, AuditSourceType } from '../../types/audit'

const loading = ref(false)
const rows = ref<AuditEntry[]>([])
const current = ref(1)
const size = ref(20)
const detailDialog = ref(false)
const detailText = ref('')

const query = reactive({
  keyword: '',
  fromTime: '',
  toTime: '',
  sourceTypes: [] as AuditSourceType[],
})

const pagedRows = computed(() => {
  const start = (current.value - 1) * size.value
  return rows.value.slice(start, start + size.value)
})

const summary = computed(() => {
  let success = 0
  let fail = 0
  for (const row of rows.value) {
    if (row.operationResult === 'SUCCESS') success += 1
    if (row.operationResult === 'FAIL') fail += 1
  }
  return {
    total: rows.value.length,
    success,
    fail,
    sourceCount: new Set(rows.value.map((x) => x.sourceType)).size,
  }
})

const load = async () => {
  loading.value = true
  try {
    rows.value = await queryAuditEntries(query.sourceTypes, query.keyword || undefined, query.fromTime || undefined, query.toTime || undefined)
    current.value = 1
  } catch (e) {
    ElMessage.error((e as { message?: string }).message ?? '审计数据加载失败')
  } finally {
    loading.value = false
  }
}

const exportCsv = () => {
  const headers = ['eventTime', 'sourceType', 'operationType', 'operationResult', 'actorName', 'actorId', 'objectType', 'objectId', 'requestIp', 'traceId', 'failReason', 'detail']
  const text = [
    headers.join(','),
    ...rows.value.map((r) =>
      [
        r.eventTime,
        r.sourceType,
        r.operationType,
        r.operationResult,
        r.actorName,
        r.actorId,
        r.objectType,
        r.objectId,
        r.requestIp,
        r.traceId,
        r.failReason.replaceAll(',', ' '),
        r.detail.replaceAll(',', ' '),
      ]
        .map((x) => `"${String(x ?? '').replaceAll('"', '""')}"`)
        .join(','),
    ),
  ].join('\n')
  const blob = new Blob([`\uFEFF${text}`], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `audit-center-${new Date().toISOString().slice(0, 19).replaceAll(':', '-')}.csv`
  link.click()
  URL.revokeObjectURL(url)
}

void load()
</script>

<template>
  <div class="dash-page">
    <header class="dash-header admin">
      <strong>面向多网融合的人机物电子认证服务软件管理后台</strong>
      <span>管理员 · 审计中心与检索导出</span>
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
        <div class="menu-item active">审计中心</div>
        <router-link to="/admin/go-live" class="menu-item">系统联调</router-link>
        <div class="menu-item">数据资源管理</div>
      </aside>
      <main class="dash-content">
        <div class="crumb">管理后台 / 审计中心与检索导出</div>

        <div class="row stats">
          <div class="stats-card g1"><div class="label">审计总量</div><div class="num">{{ summary.total }}</div></div>
          <div class="stats-card g2"><div class="label">成功记录</div><div class="num">{{ summary.success }}</div></div>
          <div class="stats-card g3"><div class="label">失败记录</div><div class="num">{{ summary.fail }}</div></div>
          <div class="stats-card g4"><div class="label">日志源数量</div><div class="num">{{ summary.sourceCount }}</div></div>
        </div>

        <section class="toolbar">
          <el-form :inline="true">
            <el-form-item label="关键字">
              <el-input v-model.trim="query.keyword" clearable placeholder="操作者/对象ID/操作类型" style="width:220px;" />
            </el-form-item>
            <el-form-item label="开始时间">
              <el-date-picker v-model="query.fromTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" />
            </el-form-item>
            <el-form-item label="结束时间">
              <el-date-picker v-model="query.toTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" />
            </el-form-item>
            <el-form-item label="日志源">
              <el-select v-model="query.sourceTypes" multiple clearable style="width:320px;">
                <el-option label="登录与用户" value="LOGIN" />
                <el-option label="证书操作" value="CERTIFICATE" />
                <el-option label="生物认证" value="BIOMETRIC" />
                <el-option label="信任评估" value="TRUST" />
                <el-option label="终端事件" value="TERMINAL" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="load">检索</el-button>
              <el-button @click="exportCsv">导出CSV</el-button>
            </el-form-item>
          </el-form>
        </section>

        <el-card class="panel-card">
          <el-table v-loading="loading" :data="pagedRows">
            <el-table-column prop="eventTime" label="时间" min-width="170" />
            <el-table-column prop="sourceType" label="日志源" width="110" />
            <el-table-column prop="operationType" label="操作类型" min-width="200" />
            <el-table-column prop="operationResult" label="结果" width="90" />
            <el-table-column prop="actorName" label="操作者" width="120" />
            <el-table-column prop="objectType" label="对象类型" width="140" />
            <el-table-column prop="objectId" label="对象ID" width="110" />
            <el-table-column prop="traceId" label="TraceId" min-width="160" />
            <el-table-column label="详情" width="90">
              <template #default="{ row }">
                <el-button link type="primary" @click="detailText = row.detail || '-'; detailDialog = true;">查看</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div style="display:flex;justify-content:flex-end;margin-top:12px;">
            <el-pagination v-model:current-page="current" v-model:page-size="size" :total="rows.length" :page-sizes="[20,50,100]" layout="total, sizes, prev, pager, next" />
          </div>
        </el-card>
      </main>
    </div>
  </div>

  <el-dialog v-model="detailDialog" title="审计详情" width="760px">
    <el-input type="textarea" :model-value="detailText" :rows="16" readonly />
  </el-dialog>
</template>
