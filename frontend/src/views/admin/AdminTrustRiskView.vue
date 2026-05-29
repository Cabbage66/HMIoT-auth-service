<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import { getTrustAssessmentDetail, getUebaHealth, queryIotStatuses, queryTerminalEvents, queryTrustAssessments, refreshIotStatus } from '../../api/trust-risk'
import type { IotDeviceStatusVO, TerminalEventVO, TrustAssessmentResultVO, UebaHealthResponse } from '../../types/trust-risk'

const health = ref<UebaHealthResponse | null>(null)

const trustRows = ref<TrustAssessmentResultVO[]>([])
const trustTotal = ref(0)
const trustCurrent = ref(1)
const trustSize = ref(10)
const trustLoading = ref(false)
const trustQuery = reactive({ riskLevel: '', decision: '' })
const trustDetail = ref<TrustAssessmentResultVO | null>(null)
const trustDetailDialog = ref(false)

const eventRows = ref<TerminalEventVO[]>([])
const eventTotal = ref(0)
const eventCurrent = ref(1)
const eventSize = ref(10)
const eventLoading = ref(false)
const eventQuery = reactive({ eventType: '', eventLevel: '' })

const iotRows = ref<IotDeviceStatusVO[]>([])
const iotTotal = ref(0)
const iotCurrent = ref(1)
const iotSize = ref(10)
const iotLoading = ref(false)
const iotQuery = reactive({ connectStatus: '', trustStatus: '' })

const riskChartEl = ref<HTMLElement | null>(null)
const iotChartEl = ref<HTMLElement | null>(null)
let riskChart: echarts.ECharts | null = null
let iotChart: echarts.ECharts | null = null

const riskStats = computed(() => {
  const counts: Record<string, number> = {}
  for (const row of trustRows.value) {
    counts[row.riskLevel || 'UNKNOWN'] = (counts[row.riskLevel || 'UNKNOWN'] || 0) + 1
  }
  return counts
})

const iotStats = computed(() => {
  const counts: Record<string, number> = {}
  for (const row of iotRows.value) {
    counts[row.trustStatus || 'UNKNOWN'] = (counts[row.trustStatus || 'UNKNOWN'] || 0) + 1
  }
  return counts
})

const loadHealth = async () => {
  try {
    health.value = await getUebaHealth()
  } catch (e) {
    ElMessage.error((e as { message?: string }).message ?? 'UEBA健康状态获取失败')
  }
}

const loadTrust = async () => {
  trustLoading.value = true
  try {
    const res = await queryTrustAssessments({
      riskLevel: trustQuery.riskLevel || undefined,
      decision: trustQuery.decision || undefined,
      pageCurrent: trustCurrent.value,
      pageSize: trustSize.value,
    })
    trustRows.value = res.records
    trustTotal.value = res.total
  } catch (e) {
    ElMessage.error((e as { message?: string }).message ?? '信任评估记录加载失败')
  } finally {
    trustLoading.value = false
  }
}

const openTrustDetail = async (id: number) => {
  try {
    trustDetail.value = await getTrustAssessmentDetail(id)
    trustDetailDialog.value = true
  } catch (e) {
    ElMessage.error((e as { message?: string }).message ?? '详情加载失败')
  }
}

const loadEvents = async () => {
  eventLoading.value = true
  try {
    const res = await queryTerminalEvents({
      eventType: eventQuery.eventType || undefined,
      eventLevel: eventQuery.eventLevel || undefined,
      pageCurrent: eventCurrent.value,
      pageSize: eventSize.value,
    })
    eventRows.value = res.records
    eventTotal.value = res.total
  } catch (e) {
    ElMessage.error((e as { message?: string }).message ?? '终端事件加载失败')
  } finally {
    eventLoading.value = false
  }
}

const loadIot = async () => {
  iotLoading.value = true
  try {
    const res = await queryIotStatuses({
      connectStatus: iotQuery.connectStatus || undefined,
      trustStatus: iotQuery.trustStatus || undefined,
      current: iotCurrent.value,
      size: iotSize.value,
    })
    iotRows.value = res.records
    iotTotal.value = res.total
  } catch (e) {
    ElMessage.error((e as { message?: string }).message ?? 'IoT状态加载失败')
  } finally {
    iotLoading.value = false
  }
}

const doRefreshIot = async (deviceEntityId: number) => {
  try {
    await refreshIotStatus(deviceEntityId)
    ElMessage.success('IoT状态刷新成功')
    await loadIot()
  } catch (e) {
    ElMessage.error((e as { message?: string }).message ?? 'IoT状态刷新失败')
  }
}

const renderCharts = () => {
  if (riskChartEl.value) {
    riskChart ??= echarts.init(riskChartEl.value)
    riskChart.setOption({
      tooltip: { trigger: 'item' },
      series: [
        {
          type: 'pie',
          radius: ['35%', '70%'],
          data: Object.entries(riskStats.value).map(([name, value]) => ({ name, value })),
        },
      ],
    })
  }
  if (iotChartEl.value) {
    iotChart ??= echarts.init(iotChartEl.value)
    iotChart.setOption({
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: Object.keys(iotStats.value) },
      yAxis: { type: 'value' },
      series: [{ type: 'bar', data: Object.values(iotStats.value), itemStyle: { color: '#409eff' } }],
    })
  }
}

watch([riskStats, iotStats], async () => {
  await nextTick()
  renderCharts()
})

onMounted(async () => {
  await Promise.all([loadHealth(), loadTrust(), loadEvents(), loadIot()])
  await nextTick()
  renderCharts()
})

onBeforeUnmount(() => {
  riskChart?.dispose()
  iotChart?.dispose()
  riskChart = null
  iotChart = null
})
</script>

<template>
  <div class="dash-page">
    <header class="dash-header admin">
      <strong>面向多网融合的人机物电子认证服务软件管理后台</strong>
      <span>管理员 · 信任评估与风险可视化</span>
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
        <div class="menu-item active">信任与风险</div>
        <router-link to="/admin/audit-center" class="menu-item">审计中心</router-link>
        <router-link to="/admin/go-live" class="menu-item">系统联调</router-link>
        <div class="menu-item">数据资源管理</div>
      </aside>
      <main class="dash-content">
        <div class="crumb">管理后台 / 信任评估与风险可视化</div>

        <div class="row stats">
          <div class="stats-card g1"><div class="label">UEBA健康</div><div class="num">{{ health?.healthy ? 'UP' : 'DOWN' }}</div></div>
          <div class="stats-card g2"><div class="label">评估记录数</div><div class="num">{{ trustTotal }}</div></div>
          <div class="stats-card g3"><div class="label">终端事件数</div><div class="num">{{ eventTotal }}</div></div>
          <div class="stats-card g4"><div class="label">IoT状态数</div><div class="num">{{ iotTotal }}</div></div>
        </div>

        <div class="row panels">
          <el-card class="panel-card" header="风险等级分布">
            <div ref="riskChartEl" style="height:260px;"></div>
          </el-card>
          <el-card class="panel-card" header="IoT可信状态分布">
            <div ref="iotChartEl" style="height:260px;"></div>
          </el-card>
        </div>

        <el-tabs type="border-card" style="margin-top:12px;">
          <el-tab-pane label="信任评估记录">
            <section class="toolbar">
              <el-form :inline="true">
                <el-form-item label="风险等级"><el-input v-model.trim="trustQuery.riskLevel" clearable /></el-form-item>
                <el-form-item label="决策"><el-input v-model.trim="trustQuery.decision" clearable /></el-form-item>
                <el-form-item><el-button type="primary" @click="trustCurrent=1;loadTrust()">查询</el-button></el-form-item>
              </el-form>
            </section>
            <el-card class="panel-card">
              <el-table v-loading="trustLoading" :data="trustRows">
                <el-table-column prop="id" label="ID" width="70" />
                <el-table-column prop="userId" label="用户ID" width="90" />
                <el-table-column prop="terminalId" label="终端ID" width="90" />
                <el-table-column prop="riskLevel" label="风险等级" width="100" />
                <el-table-column prop="trustLevel" label="信任等级" width="100" />
                <el-table-column prop="decision" label="决策" width="100" />
                <el-table-column prop="assessedAt" label="评估时间" min-width="180" />
                <el-table-column label="操作" width="90">
                  <template #default="{ row }"><el-button link type="primary" @click="openTrustDetail(row.id)">详情</el-button></template>
                </el-table-column>
              </el-table>
              <div style="display:flex;justify-content:flex-end;margin-top:12px;">
                <el-pagination v-model:current-page="trustCurrent" v-model:page-size="trustSize" :total="trustTotal" layout="total, sizes, prev, pager, next" :page-sizes="[10,20,50]" @current-change="loadTrust" @size-change="trustCurrent=1;loadTrust()" />
              </div>
            </el-card>
          </el-tab-pane>

          <el-tab-pane label="终端事件">
            <section class="toolbar">
              <el-form :inline="true">
                <el-form-item label="事件类型"><el-input v-model.trim="eventQuery.eventType" clearable /></el-form-item>
                <el-form-item label="事件等级"><el-input v-model.trim="eventQuery.eventLevel" clearable /></el-form-item>
                <el-form-item><el-button type="primary" @click="eventCurrent=1;loadEvents()">查询</el-button></el-form-item>
              </el-form>
            </section>
            <el-card class="panel-card">
              <el-table v-loading="eventLoading" :data="eventRows">
                <el-table-column prop="id" label="ID" width="70" />
                <el-table-column prop="terminalId" label="终端ID" width="90" />
                <el-table-column prop="eventType" label="事件类型" min-width="160" />
                <el-table-column prop="eventLevel" label="等级" width="100" />
                <el-table-column prop="eventTime" label="事件时间" min-width="180" />
              </el-table>
              <div style="display:flex;justify-content:flex-end;margin-top:12px;">
                <el-pagination v-model:current-page="eventCurrent" v-model:page-size="eventSize" :total="eventTotal" layout="total, sizes, prev, pager, next" :page-sizes="[10,20,50]" @current-change="loadEvents" @size-change="eventCurrent=1;loadEvents()" />
              </div>
            </el-card>
          </el-tab-pane>

          <el-tab-pane label="IoT状态">
            <section class="toolbar">
              <el-form :inline="true">
                <el-form-item label="连接状态"><el-input v-model.trim="iotQuery.connectStatus" clearable /></el-form-item>
                <el-form-item label="可信状态"><el-input v-model.trim="iotQuery.trustStatus" clearable /></el-form-item>
                <el-form-item><el-button type="primary" @click="iotCurrent=1;loadIot()">查询</el-button></el-form-item>
              </el-form>
            </section>
            <el-card class="panel-card">
              <el-table v-loading="iotLoading" :data="iotRows">
                <el-table-column prop="deviceEntityId" label="设备实体ID" width="120" />
                <el-table-column prop="connectStatus" label="连接状态" width="120" />
                <el-table-column prop="trustStatus" label="可信状态" width="120" />
                <el-table-column prop="lastReportAt" label="上报时间" min-width="180" />
                <el-table-column label="操作" width="120">
                  <template #default="{ row }"><el-button link type="primary" @click="doRefreshIot(row.deviceEntityId)">刷新状态</el-button></template>
                </el-table-column>
              </el-table>
              <div style="display:flex;justify-content:flex-end;margin-top:12px;">
                <el-pagination v-model:current-page="iotCurrent" v-model:page-size="iotSize" :total="iotTotal" layout="total, sizes, prev, pager, next" :page-sizes="[10,20,50]" @current-change="loadIot" @size-change="iotCurrent=1;loadIot()" />
              </div>
            </el-card>
          </el-tab-pane>
        </el-tabs>

        <el-dialog v-model="trustDetailDialog" title="信任评估详情" width="920px">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="requestId">{{ trustDetail?.requestId }}</el-descriptions-item>
            <el-descriptions-item label="模型">{{ trustDetail?.modelSource }} / {{ trustDetail?.modelVersion }}</el-descriptions-item>
            <el-descriptions-item label="riskLevel">{{ trustDetail?.riskLevel }}</el-descriptions-item>
            <el-descriptions-item label="decision">{{ trustDetail?.decision }}</el-descriptions-item>
          </el-descriptions>
          <el-divider />
          <el-form-item label="inputPayload"><el-input :model-value="JSON.stringify(trustDetail?.inputPayload ?? {}, null, 2)" type="textarea" :rows="8" readonly /></el-form-item>
          <el-form-item label="rawResponse"><el-input :model-value="JSON.stringify(trustDetail?.rawResponse ?? {}, null, 2)" type="textarea" :rows="8" readonly /></el-form-item>
        </el-dialog>
      </main>
    </div>
  </div>
</template>
