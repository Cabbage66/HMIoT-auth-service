<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createApp,
  createAuthMethod,
  createAuthPolicy,
  createBaseline,
  publishAuthPolicy,
  publishBaseline,
  queryApps,
  queryAuthMethods,
  queryAuthPolicies,
  queryBaselines,
  queryPublishRecords,
  updateApp,
  updateAppStatus,
  updateAuthMethod,
  updateAuthMethodStatus,
  updateAuthPolicy,
  updateAuthPolicyStatus,
  updateBaseline,
  updateBaselineStatus,
} from '../../api/policy-app'
import type { AppResourceVO, AuthMethodVO, AuthPolicyVO, PolicyPublishRecordVO, SecurityBaselineVO } from '../../types/policy-app'

const activeTab = ref('apps')

const appRows = ref<AppResourceVO[]>([])
const appLoading = ref(false)
const appTotal = ref(0)
const appCurrent = ref(1)
const appSize = ref(10)
const appQuery = reactive({ keyword: '', status: '' })
const appDialog = ref(false)
const appForm = reactive({
  id: undefined as number | undefined,
  appCode: '',
  appName: '',
  resourceType: 'APP',
  accessUrl: '',
  securityLevel: 1,
  requiredTrustLevel: 1,
  status: 'ENABLED',
  description: '',
  metadata: '',
})

const methodRows = ref<AuthMethodVO[]>([])
const methodLoading = ref(false)
const methodTotal = ref(0)
const methodCurrent = ref(1)
const methodSize = ref(10)
const methodQuery = reactive({ keyword: '', enabled: undefined as boolean | undefined })
const methodDialog = ref(false)
const methodForm = reactive({
  id: undefined as number | undefined,
  methodCode: '',
  methodName: '',
  methodType: '',
  compareMode: '',
  enabled: true,
  sortOrder: 0,
  description: '',
})

const policyRows = ref<AuthPolicyVO[]>([])
const policyLoading = ref(false)
const policyTotal = ref(0)
const policyCurrent = ref(1)
const policySize = ref(10)
const policyQuery = reactive({ keyword: '', sceneType: '', status: '' })
const policyDialog = ref(false)
const policyForm = reactive({
  id: undefined as number | undefined,
  policyCode: '',
  policyName: '',
  sceneType: '',
  appResourceId: undefined as number | undefined,
  userScope: '',
  deviceScope: '',
  authChain: '',
  minTrustLevel: 1,
  sessionTtlSeconds: 1800,
  actionOnFail: 'DENY',
  status: 'ENABLED',
})

const baselineRows = ref<SecurityBaselineVO[]>([])
const baselineLoading = ref(false)
const baselineTotal = ref(0)
const baselineCurrent = ref(1)
const baselineSize = ref(10)
const baselineQuery = reactive({ keyword: '', targetType: '', status: '' })
const baselineDialog = ref(false)
const baselineForm = reactive({
  id: undefined as number | undefined,
  baselineCode: '',
  baselineName: '',
  targetType: '',
  targetScope: '',
  ruleConfig: '{}',
  riskLevel: 1,
  actionOnHit: 'SECOND_AUTH',
  status: 'ENABLED',
})

const publishRows = ref<PolicyPublishRecordVO[]>([])
const publishLoading = ref(false)
const publishTotal = ref(0)
const publishCurrent = ref(1)
const publishSize = ref(10)
const publishQuery = reactive({ policyType: '', publishStatus: '' })

const loadApps = async () => {
  appLoading.value = true
  try {
    const res = await queryApps({ keyword: appQuery.keyword || undefined, status: appQuery.status || undefined, current: appCurrent.value, size: appSize.value })
    appRows.value = res.records
    appTotal.value = res.total
  } catch (e) { ElMessage.error((e as { message?: string }).message ?? '应用列表加载失败') } finally { appLoading.value = false }
}
const openApp = (row?: AppResourceVO) => {
  Object.assign(appForm, row ?? { id: undefined, appCode: '', appName: '', resourceType: 'APP', accessUrl: '', securityLevel: 1, requiredTrustLevel: 1, status: 'ENABLED', description: '', metadata: '' })
  appDialog.value = true
}
const submitApp = async () => {
  try {
    if (appForm.id) {
      await updateApp(appForm.id, { appName: appForm.appName, resourceType: appForm.resourceType, accessUrl: appForm.accessUrl, securityLevel: appForm.securityLevel, requiredTrustLevel: appForm.requiredTrustLevel, status: appForm.status, description: appForm.description, metadata: appForm.metadata })
    } else {
      await createApp({ appCode: appForm.appCode, appName: appForm.appName, resourceType: appForm.resourceType, accessUrl: appForm.accessUrl, securityLevel: appForm.securityLevel, requiredTrustLevel: appForm.requiredTrustLevel, status: appForm.status, description: appForm.description, metadata: appForm.metadata })
    }
    appDialog.value = false
    ElMessage.success('应用资源保存成功')
    await loadApps()
  } catch (e) { ElMessage.error((e as { message?: string }).message ?? '应用保存失败') }
}

const loadMethods = async () => {
  methodLoading.value = true
  try {
    const res = await queryAuthMethods({ keyword: methodQuery.keyword || undefined, enabled: methodQuery.enabled, current: methodCurrent.value, size: methodSize.value })
    methodRows.value = res.records
    methodTotal.value = res.total
  } catch (e) { ElMessage.error((e as { message?: string }).message ?? '认证方式加载失败') } finally { methodLoading.value = false }
}
const openMethod = (row?: AuthMethodVO) => {
  Object.assign(methodForm, row ?? { id: undefined, methodCode: '', methodName: '', methodType: '', compareMode: '', enabled: true, sortOrder: 0, description: '' })
  methodDialog.value = true
}
const submitMethod = async () => {
  try {
    if (methodForm.id) {
      await updateAuthMethod(methodForm.id, { methodName: methodForm.methodName, methodType: methodForm.methodType, compareMode: methodForm.compareMode, sortOrder: methodForm.sortOrder, description: methodForm.description })
    } else {
      await createAuthMethod({ methodCode: methodForm.methodCode, methodName: methodForm.methodName, methodType: methodForm.methodType, compareMode: methodForm.compareMode, enabled: methodForm.enabled, sortOrder: methodForm.sortOrder, description: methodForm.description })
    }
    methodDialog.value = false
    ElMessage.success('认证方式保存成功')
    await loadMethods()
  } catch (e) { ElMessage.error((e as { message?: string }).message ?? '认证方式保存失败') }
}

const loadPolicies = async () => {
  policyLoading.value = true
  try {
    const res = await queryAuthPolicies({ keyword: policyQuery.keyword || undefined, sceneType: policyQuery.sceneType || undefined, status: policyQuery.status || undefined, current: policyCurrent.value, size: policySize.value })
    policyRows.value = res.records
    policyTotal.value = res.total
  } catch (e) { ElMessage.error((e as { message?: string }).message ?? '认证策略加载失败') } finally { policyLoading.value = false }
}
const openPolicy = (row?: AuthPolicyVO) => {
  Object.assign(policyForm, row ?? { id: undefined, policyCode: '', policyName: '', sceneType: '', appResourceId: undefined, userScope: '', deviceScope: '', authChain: '', minTrustLevel: 1, sessionTtlSeconds: 1800, actionOnFail: 'DENY', status: 'ENABLED' })
  policyDialog.value = true
}
const submitPolicy = async () => {
  try {
    if (policyForm.id) {
      await updateAuthPolicy(policyForm.id, { policyName: policyForm.policyName, sceneType: policyForm.sceneType, appResourceId: policyForm.appResourceId, userScope: policyForm.userScope, deviceScope: policyForm.deviceScope, authChain: policyForm.authChain, minTrustLevel: policyForm.minTrustLevel, sessionTtlSeconds: policyForm.sessionTtlSeconds, actionOnFail: policyForm.actionOnFail, status: policyForm.status })
    } else {
      await createAuthPolicy({ policyCode: policyForm.policyCode, policyName: policyForm.policyName, sceneType: policyForm.sceneType, appResourceId: policyForm.appResourceId, userScope: policyForm.userScope, deviceScope: policyForm.deviceScope, authChain: policyForm.authChain, minTrustLevel: policyForm.minTrustLevel, sessionTtlSeconds: policyForm.sessionTtlSeconds, actionOnFail: policyForm.actionOnFail, status: policyForm.status })
    }
    policyDialog.value = false
    ElMessage.success('认证策略保存成功')
    await loadPolicies()
  } catch (e) { ElMessage.error((e as { message?: string }).message ?? '认证策略保存失败') }
}

const loadBaselines = async () => {
  baselineLoading.value = true
  try {
    const res = await queryBaselines({ keyword: baselineQuery.keyword || undefined, targetType: baselineQuery.targetType || undefined, status: baselineQuery.status || undefined, current: baselineCurrent.value, size: baselineSize.value })
    baselineRows.value = res.records
    baselineTotal.value = res.total
  } catch (e) { ElMessage.error((e as { message?: string }).message ?? '安全基线加载失败') } finally { baselineLoading.value = false }
}
const openBaseline = (row?: SecurityBaselineVO) => {
  Object.assign(baselineForm, row ?? { id: undefined, baselineCode: '', baselineName: '', targetType: '', targetScope: '', ruleConfig: '{}', riskLevel: 1, actionOnHit: 'SECOND_AUTH', status: 'ENABLED' })
  baselineDialog.value = true
}
const submitBaseline = async () => {
  try {
    if (baselineForm.id) {
      await updateBaseline(baselineForm.id, { baselineName: baselineForm.baselineName, targetType: baselineForm.targetType, targetScope: baselineForm.targetScope, ruleConfig: baselineForm.ruleConfig, riskLevel: baselineForm.riskLevel, actionOnHit: baselineForm.actionOnHit, status: baselineForm.status })
    } else {
      await createBaseline({ baselineCode: baselineForm.baselineCode, baselineName: baselineForm.baselineName, targetType: baselineForm.targetType, targetScope: baselineForm.targetScope, ruleConfig: baselineForm.ruleConfig, riskLevel: baselineForm.riskLevel, actionOnHit: baselineForm.actionOnHit, status: baselineForm.status })
    }
    baselineDialog.value = false
    ElMessage.success('安全基线保存成功')
    await loadBaselines()
  } catch (e) { ElMessage.error((e as { message?: string }).message ?? '安全基线保存失败') }
}

const loadPublishRecords = async () => {
  publishLoading.value = true
  try {
    const res = await queryPublishRecords({ policyType: publishQuery.policyType || undefined, publishStatus: publishQuery.publishStatus || undefined, current: publishCurrent.value, size: publishSize.value })
    publishRows.value = res.records
    publishTotal.value = res.total
  } catch (e) { ElMessage.error((e as { message?: string }).message ?? '发布记录加载失败') } finally { publishLoading.value = false }
}

const toggleStatus = async (type: 'app' | 'method' | 'policy' | 'baseline', row: any, target: any) => {
  try {
    if (type === 'app') await updateAppStatus(row.id, target)
    if (type === 'method') await updateAuthMethodStatus(row.id, target)
    if (type === 'policy') await updateAuthPolicyStatus(row.id, target)
    if (type === 'baseline') await updateBaselineStatus(row.id, target)
    ElMessage.success('状态更新成功')
    if (type === 'app') await loadApps()
    if (type === 'method') await loadMethods()
    if (type === 'policy') await loadPolicies()
    if (type === 'baseline') await loadBaselines()
  } catch (e) { ElMessage.error((e as { message?: string }).message ?? '状态更新失败') }
}

const doPublish = async (type: 'policy' | 'baseline', row: { id: number }) => {
  const targetType = await ElMessageBox.prompt('请输入 targetType（GLOBAL/APP/TERMINAL/USER）', '发布策略', { inputValue: 'GLOBAL' }).then(v => v.value)
  const targetIdText = await ElMessageBox.prompt('请输入 targetId（GLOBAL可留空）', '发布策略', { inputValue: '' }).then(v => v.value)
  const targetId = targetIdText ? Number(targetIdText) : undefined
  try {
    if (type === 'policy') await publishAuthPolicy(row.id, targetType, targetId)
    else await publishBaseline(row.id, targetType, targetId)
    ElMessage.success('发布成功')
    await loadPublishRecords()
  } catch (e) { ElMessage.error((e as { message?: string }).message ?? '发布失败') }
}

void loadApps()
void loadMethods()
void loadPolicies()
void loadBaselines()
void loadPublishRecords()
</script>

<template>
  <div class="dash-page">
    <header class="dash-header admin">
      <strong>面向多网融合的人机物电子认证服务软件管理后台</strong>
      <span>管理员 · 策略与应用资源</span>
    </header>
    <div class="dash-layout">
      <aside class="dash-aside">
        <router-link to="/admin/dashboard" class="menu-item">概览面板</router-link>
        <router-link to="/admin/login-logs" class="menu-item">登录日志</router-link>
        <router-link to="/admin/users" class="menu-item">用户管理</router-link>
        <router-link to="/admin/registrations" class="menu-item">身份注册</router-link>
        <router-link to="/admin/credentials" class="menu-item">生物凭据管理</router-link>
        <router-link to="/admin/device-binding" class="menu-item">设备与绑定</router-link>
        <div class="menu-item active">策略与应用</div>
        <router-link to="/admin/trust-risk" class="menu-item">信任与风险</router-link>
        <router-link to="/admin/audit-center" class="menu-item">审计中心</router-link>
        <router-link to="/admin/go-live" class="menu-item">系统联调</router-link>
        <div class="menu-item">数据资源管理</div>
      </aside>
      <main class="dash-content">
        <div class="crumb">管理后台 / 策略与应用资源管理</div>
        <el-tabs v-model="activeTab" type="border-card">
          <el-tab-pane label="应用资源" name="apps">
            <section class="toolbar">
              <el-form :inline="true">
                <el-form-item label="关键字"><el-input v-model.trim="appQuery.keyword" clearable /></el-form-item>
                <el-form-item label="状态"><el-select v-model="appQuery.status" clearable style="width:120px;"><el-option label="ENABLED" value="ENABLED"/><el-option label="DISABLED" value="DISABLED"/></el-select></el-form-item>
                <el-form-item><el-button type="primary" @click="appCurrent=1;loadApps()">查询</el-button></el-form-item>
                <el-form-item><el-button type="success" @click="openApp()">新增</el-button></el-form-item>
              </el-form>
            </section>
            <el-card class="panel-card"><el-table v-loading="appLoading" :data="appRows"><el-table-column prop="appCode" label="编码"/><el-table-column prop="appName" label="名称"/><el-table-column prop="securityLevel" label="安全级别" width="100"/><el-table-column prop="requiredTrustLevel" label="信任阈值" width="100"/><el-table-column prop="status" label="状态" width="100"/><el-table-column label="操作" width="220"><template #default="{row}"><el-button link @click="openApp(row)">编辑</el-button><el-button v-if="row.status!=='ENABLED'" link type="success" @click="toggleStatus('app',row,'ENABLED')">启用</el-button><el-button v-if="row.status!=='DISABLED'" link type="danger" @click="toggleStatus('app',row,'DISABLED')">禁用</el-button></template></el-table-column></el-table><div style="display:flex;justify-content:flex-end;margin-top:12px;"><el-pagination v-model:current-page="appCurrent" v-model:page-size="appSize" :total="appTotal" layout="total, sizes, prev, pager, next" :page-sizes="[10,20,50]" @current-change="loadApps" @size-change="appCurrent=1;loadApps()"/></div></el-card>
          </el-tab-pane>

          <el-tab-pane label="认证方式" name="methods">
            <section class="toolbar"><el-form :inline="true"><el-form-item label="关键字"><el-input v-model.trim="methodQuery.keyword" clearable/></el-form-item><el-form-item label="启用"><el-select v-model="methodQuery.enabled" clearable style="width:120px;"><el-option label="true" :value="true"/><el-option label="false" :value="false"/></el-select></el-form-item><el-form-item><el-button type="primary" @click="methodCurrent=1;loadMethods()">查询</el-button></el-form-item><el-form-item><el-button type="success" @click="openMethod()">新增</el-button></el-form-item></el-form></section>
            <el-card class="panel-card"><el-table v-loading="methodLoading" :data="methodRows"><el-table-column prop="methodCode" label="编码"/><el-table-column prop="methodName" label="名称"/><el-table-column prop="methodType" label="类型"/><el-table-column prop="enabled" label="启用" width="90"/><el-table-column label="操作" width="220"><template #default="{row}"><el-button link @click="openMethod(row)">编辑</el-button><el-button v-if="!row.enabled" link type="success" @click="toggleStatus('method',row,true)">启用</el-button><el-button v-if="row.enabled" link type="danger" @click="toggleStatus('method',row,false)">禁用</el-button></template></el-table-column></el-table><div style="display:flex;justify-content:flex-end;margin-top:12px;"><el-pagination v-model:current-page="methodCurrent" v-model:page-size="methodSize" :total="methodTotal" layout="total, sizes, prev, pager, next" :page-sizes="[10,20,50]" @current-change="loadMethods" @size-change="methodCurrent=1;loadMethods()"/></div></el-card>
          </el-tab-pane>

          <el-tab-pane label="认证策略" name="policies">
            <section class="toolbar"><el-form :inline="true"><el-form-item label="关键字"><el-input v-model.trim="policyQuery.keyword" clearable/></el-form-item><el-form-item label="场景"><el-input v-model.trim="policyQuery.sceneType" clearable/></el-form-item><el-form-item label="状态"><el-select v-model="policyQuery.status" clearable style="width:120px;"><el-option label="ENABLED" value="ENABLED"/><el-option label="DISABLED" value="DISABLED"/></el-select></el-form-item><el-form-item><el-button type="primary" @click="policyCurrent=1;loadPolicies()">查询</el-button></el-form-item><el-form-item><el-button type="success" @click="openPolicy()">新增</el-button></el-form-item></el-form></section>
            <el-card class="panel-card"><el-table v-loading="policyLoading" :data="policyRows"><el-table-column prop="policyCode" label="编码"/><el-table-column prop="policyName" label="名称"/><el-table-column prop="sceneType" label="场景"/><el-table-column prop="status" label="状态" width="100"/><el-table-column prop="versionNo" label="版本" width="80"/><el-table-column label="操作" width="300"><template #default="{row}"><el-button link @click="openPolicy(row)">编辑</el-button><el-button link type="primary" @click="doPublish('policy',row)">发布</el-button><el-button v-if="row.status!=='ENABLED'" link type="success" @click="toggleStatus('policy',row,'ENABLED')">启用</el-button><el-button v-if="row.status!=='DISABLED'" link type="danger" @click="toggleStatus('policy',row,'DISABLED')">禁用</el-button></template></el-table-column></el-table><div style="display:flex;justify-content:flex-end;margin-top:12px;"><el-pagination v-model:current-page="policyCurrent" v-model:page-size="policySize" :total="policyTotal" layout="total, sizes, prev, pager, next" :page-sizes="[10,20,50]" @current-change="loadPolicies" @size-change="policyCurrent=1;loadPolicies()"/></div></el-card>
          </el-tab-pane>

          <el-tab-pane label="安全基线" name="baselines">
            <section class="toolbar"><el-form :inline="true"><el-form-item label="关键字"><el-input v-model.trim="baselineQuery.keyword" clearable/></el-form-item><el-form-item label="目标类型"><el-input v-model.trim="baselineQuery.targetType" clearable/></el-form-item><el-form-item label="状态"><el-select v-model="baselineQuery.status" clearable style="width:120px;"><el-option label="ENABLED" value="ENABLED"/><el-option label="DISABLED" value="DISABLED"/></el-select></el-form-item><el-form-item><el-button type="primary" @click="baselineCurrent=1;loadBaselines()">查询</el-button></el-form-item><el-form-item><el-button type="success" @click="openBaseline()">新增</el-button></el-form-item></el-form></section>
            <el-card class="panel-card"><el-table v-loading="baselineLoading" :data="baselineRows"><el-table-column prop="baselineCode" label="编码"/><el-table-column prop="baselineName" label="名称"/><el-table-column prop="targetType" label="目标类型"/><el-table-column prop="riskLevel" label="风险级别" width="90"/><el-table-column prop="status" label="状态" width="100"/><el-table-column label="操作" width="300"><template #default="{row}"><el-button link @click="openBaseline(row)">编辑</el-button><el-button link type="primary" @click="doPublish('baseline',row)">发布</el-button><el-button v-if="row.status!=='ENABLED'" link type="success" @click="toggleStatus('baseline',row,'ENABLED')">启用</el-button><el-button v-if="row.status!=='DISABLED'" link type="danger" @click="toggleStatus('baseline',row,'DISABLED')">禁用</el-button></template></el-table-column></el-table><div style="display:flex;justify-content:flex-end;margin-top:12px;"><el-pagination v-model:current-page="baselineCurrent" v-model:page-size="baselineSize" :total="baselineTotal" layout="total, sizes, prev, pager, next" :page-sizes="[10,20,50]" @current-change="loadBaselines" @size-change="baselineCurrent=1;loadBaselines()"/></div></el-card>
          </el-tab-pane>

          <el-tab-pane label="发布记录" name="publish">
            <section class="toolbar"><el-form :inline="true"><el-form-item label="策略类型"><el-select v-model="publishQuery.policyType" clearable style="width:180px;"><el-option label="AUTH_POLICY" value="AUTH_POLICY"/><el-option label="SECURITY_BASELINE" value="SECURITY_BASELINE"/></el-select></el-form-item><el-form-item label="发布状态"><el-select v-model="publishQuery.publishStatus" clearable style="width:140px;"><el-option label="PENDING" value="PENDING"/><el-option label="DELIVERED" value="DELIVERED"/><el-option label="ACKED" value="ACKED"/><el-option label="FAILED" value="FAILED"/></el-select></el-form-item><el-form-item><el-button type="primary" @click="publishCurrent=1;loadPublishRecords()">查询</el-button></el-form-item></el-form></section>
            <el-card class="panel-card"><el-table v-loading="publishLoading" :data="publishRows"><el-table-column prop="id" label="ID" width="70"/><el-table-column prop="policyType" label="策略类型" width="150"/><el-table-column prop="policyId" label="策略ID" width="90"/><el-table-column prop="targetType" label="目标类型" width="120"/><el-table-column prop="targetId" label="目标ID" width="90"/><el-table-column prop="publishStatus" label="发布状态" width="110"/><el-table-column prop="executeResult" label="执行结果" width="110"/><el-table-column prop="publishedAt" label="发布时间" min-width="180"/></el-table><div style="display:flex;justify-content:flex-end;margin-top:12px;"><el-pagination v-model:current-page="publishCurrent" v-model:page-size="publishSize" :total="publishTotal" layout="total, sizes, prev, pager, next" :page-sizes="[10,20,50]" @current-change="loadPublishRecords" @size-change="publishCurrent=1;loadPublishRecords()"/></div></el-card>
          </el-tab-pane>
        </el-tabs>
      </main>
    </div>

    <el-dialog v-model="appDialog" :title="appForm.id ? '编辑应用' : '新增应用'" width="760px">
      <div class="detail-grid">
        <el-form-item label="appCode"><el-input v-model.trim="appForm.appCode" :disabled="Boolean(appForm.id)"/></el-form-item>
        <el-form-item label="appName"><el-input v-model.trim="appForm.appName"/></el-form-item>
        <el-form-item label="resourceType"><el-input v-model.trim="appForm.resourceType"/></el-form-item>
        <el-form-item label="accessUrl"><el-input v-model.trim="appForm.accessUrl"/></el-form-item>
        <el-form-item label="securityLevel"><el-input-number v-model="appForm.securityLevel" :min="0" style="width:100%"/></el-form-item>
        <el-form-item label="requiredTrustLevel"><el-input-number v-model="appForm.requiredTrustLevel" :min="0" style="width:100%"/></el-form-item>
      </div>
      <el-form-item label="status"><el-select v-model="appForm.status"><el-option label="ENABLED" value="ENABLED"/><el-option label="DISABLED" value="DISABLED"/></el-select></el-form-item>
      <el-form-item label="description"><el-input v-model="appForm.description" type="textarea" :rows="2"/></el-form-item>
      <el-form-item label="metadata"><el-input v-model="appForm.metadata" type="textarea" :rows="2"/></el-form-item>
      <template #footer><el-button @click="appDialog=false">取消</el-button><el-button type="primary" @click="submitApp">保存</el-button></template>
    </el-dialog>

    <el-dialog v-model="methodDialog" :title="methodForm.id ? '编辑认证方式' : '新增认证方式'" width="680px">
      <div class="detail-grid">
        <el-form-item label="methodCode"><el-input v-model.trim="methodForm.methodCode" :disabled="Boolean(methodForm.id)"/></el-form-item>
        <el-form-item label="methodName"><el-input v-model.trim="methodForm.methodName"/></el-form-item>
        <el-form-item label="methodType"><el-input v-model.trim="methodForm.methodType"/></el-form-item>
        <el-form-item label="compareMode"><el-input v-model.trim="methodForm.compareMode"/></el-form-item>
        <el-form-item label="sortOrder"><el-input-number v-model="methodForm.sortOrder" :min="0" style="width:100%"/></el-form-item>
      </div>
      <el-form-item label="description"><el-input v-model="methodForm.description" type="textarea" :rows="2"/></el-form-item>
      <template #footer><el-button @click="methodDialog=false">取消</el-button><el-button type="primary" @click="submitMethod">保存</el-button></template>
    </el-dialog>

    <el-dialog v-model="policyDialog" :title="policyForm.id ? '编辑认证策略' : '新增认证策略'" width="800px">
      <div class="detail-grid">
        <el-form-item label="policyCode"><el-input v-model.trim="policyForm.policyCode" :disabled="Boolean(policyForm.id)"/></el-form-item>
        <el-form-item label="policyName"><el-input v-model.trim="policyForm.policyName"/></el-form-item>
        <el-form-item label="sceneType"><el-input v-model.trim="policyForm.sceneType"/></el-form-item>
        <el-form-item label="appResourceId"><el-input-number v-model="policyForm.appResourceId" :min="1" style="width:100%"/></el-form-item>
        <el-form-item label="minTrustLevel"><el-input-number v-model="policyForm.minTrustLevel" :min="0" style="width:100%"/></el-form-item>
        <el-form-item label="sessionTtlSeconds"><el-input-number v-model="policyForm.sessionTtlSeconds" :min="0" style="width:100%"/></el-form-item>
      </div>
      <el-form-item label="authChain"><el-input v-model.trim="policyForm.authChain" placeholder="示例: USB_LOGIN,VOICEPRINT"/></el-form-item>
      <el-form-item label="userScope"><el-input v-model="policyForm.userScope" type="textarea" :rows="2"/></el-form-item>
      <el-form-item label="deviceScope"><el-input v-model="policyForm.deviceScope" type="textarea" :rows="2"/></el-form-item>
      <el-form-item label="actionOnFail"><el-input v-model.trim="policyForm.actionOnFail"/></el-form-item>
      <el-form-item label="status"><el-select v-model="policyForm.status"><el-option label="ENABLED" value="ENABLED"/><el-option label="DISABLED" value="DISABLED"/></el-select></el-form-item>
      <template #footer><el-button @click="policyDialog=false">取消</el-button><el-button type="primary" @click="submitPolicy">保存</el-button></template>
    </el-dialog>

    <el-dialog v-model="baselineDialog" :title="baselineForm.id ? '编辑安全基线' : '新增安全基线'" width="780px">
      <div class="detail-grid">
        <el-form-item label="baselineCode"><el-input v-model.trim="baselineForm.baselineCode" :disabled="Boolean(baselineForm.id)"/></el-form-item>
        <el-form-item label="baselineName"><el-input v-model.trim="baselineForm.baselineName"/></el-form-item>
        <el-form-item label="targetType"><el-input v-model.trim="baselineForm.targetType"/></el-form-item>
        <el-form-item label="riskLevel"><el-input-number v-model="baselineForm.riskLevel" :min="0" style="width:100%"/></el-form-item>
      </div>
      <el-form-item label="targetScope"><el-input v-model="baselineForm.targetScope" type="textarea" :rows="2"/></el-form-item>
      <el-form-item label="ruleConfig"><el-input v-model="baselineForm.ruleConfig" type="textarea" :rows="4"/></el-form-item>
      <el-form-item label="actionOnHit"><el-input v-model.trim="baselineForm.actionOnHit"/></el-form-item>
      <el-form-item label="status"><el-select v-model="baselineForm.status"><el-option label="ENABLED" value="ENABLED"/><el-option label="DISABLED" value="DISABLED"/></el-select></el-form-item>
      <template #footer><el-button @click="baselineDialog=false">取消</el-button><el-button type="primary" @click="submitBaseline">保存</el-button></template>
    </el-dialog>
  </div>
</template>
