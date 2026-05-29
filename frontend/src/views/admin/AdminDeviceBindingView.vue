<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  bindUserDynamicBiometric,
  bindUserTerminal,
  createDevice,
  createTerminal,
  queryBindings,
  queryDevices,
  queryTerminals,
  unbindRelation,
  updateDevice,
  updateDeviceStatus,
  updateTerminal,
  updateTerminalStatus,
} from '../../api/device-binding'
import type { BindingRelationVO, DeviceEntityVO, TerminalInfoVO } from '../../types/device-binding'

const activeTab = ref('devices')

const deviceLoading = ref(false)
const deviceRows = ref<DeviceEntityVO[]>([])
const deviceTotal = ref(0)
const deviceCurrent = ref(1)
const deviceSize = ref(10)
const deviceQuery = reactive({ keyword: '', deviceType: '', status: '' })
const deviceForm = reactive({
  id: undefined as number | undefined,
  entityId: undefined as number | undefined,
  deviceCode: '',
  deviceName: '',
  deviceType: '',
  deviceFingerprint: '',
  certSubject: '',
  status: 'ENABLED',
  sourceSystem: '',
  description: '',
  metadata: '',
})
const deviceDialog = ref(false)

const terminalLoading = ref(false)
const terminalRows = ref<TerminalInfoVO[]>([])
const terminalTotal = ref(0)
const terminalCurrent = ref(1)
const terminalSize = ref(10)
const terminalQuery = reactive({ keyword: '', clientType: '', status: '' })
const terminalForm = reactive({
  id: undefined as number | undefined,
  terminalCode: '',
  deviceEntityId: undefined as number | undefined,
  userAgent: '',
  osName: '',
  osVersion: '',
  clientType: '',
  clientVersion: '',
  ipAddress: '',
  status: 'ENABLED',
  metadata: '',
})
const terminalDialog = ref(false)

const bindingLoading = ref(false)
const bindingRows = ref<BindingRelationVO[]>([])
const bindingTotal = ref(0)
const bindingCurrent = ref(1)
const bindingSize = ref(10)
const bindingQuery = reactive({ subjectType: 'USER', targetType: '', relationType: '', status: '' })
const bindForm = reactive({
  userId: undefined as number | undefined,
  terminalId: undefined as number | undefined,
  terminalReason: '',
  dynamicUserId: undefined as number | undefined,
  externalSubjectId: '',
  biometricType: '',
  modelSource: '',
  dynamicReason: '',
})

const loadDevices = async () => {
  deviceLoading.value = true
  try {
    const res = await queryDevices({
      keyword: deviceQuery.keyword || undefined,
      deviceType: deviceQuery.deviceType || undefined,
      status: deviceQuery.status || undefined,
      current: deviceCurrent.value,
      size: deviceSize.value,
    })
    deviceRows.value = res.records
    deviceTotal.value = res.total
  } catch (error) {
    ElMessage.error((error as { message?: string }).message ?? '设备列表加载失败')
  } finally {
    deviceLoading.value = false
  }
}

const openDeviceCreate = () => {
  Object.assign(deviceForm, {
    id: undefined,
    entityId: undefined,
    deviceCode: '',
    deviceName: '',
    deviceType: '',
    deviceFingerprint: '',
    certSubject: '',
    status: 'ENABLED',
    sourceSystem: '',
    description: '',
    metadata: '',
  })
  deviceDialog.value = true
}

const openDeviceEdit = (row: DeviceEntityVO) => {
  Object.assign(deviceForm, row)
  deviceDialog.value = true
}

const submitDevice = async () => {
  try {
    if (deviceForm.id) {
      await updateDevice(deviceForm.id, {
        entityId: deviceForm.entityId,
        deviceName: deviceForm.deviceName,
        deviceType: deviceForm.deviceType,
        deviceFingerprint: deviceForm.deviceFingerprint || undefined,
        certSubject: deviceForm.certSubject || undefined,
        status: deviceForm.status,
        sourceSystem: deviceForm.sourceSystem || undefined,
        description: deviceForm.description || undefined,
        metadata: deviceForm.metadata || undefined,
      })
      ElMessage.success('设备已更新')
    } else {
      await createDevice({
        entityId: deviceForm.entityId,
        deviceCode: deviceForm.deviceCode,
        deviceName: deviceForm.deviceName,
        deviceType: deviceForm.deviceType,
        deviceFingerprint: deviceForm.deviceFingerprint || undefined,
        certSubject: deviceForm.certSubject || undefined,
        status: deviceForm.status,
        sourceSystem: deviceForm.sourceSystem || undefined,
        description: deviceForm.description || undefined,
        metadata: deviceForm.metadata || undefined,
      })
      ElMessage.success('设备已创建')
    }
    deviceDialog.value = false
    await loadDevices()
  } catch (error) {
    ElMessage.error((error as { message?: string }).message ?? '设备操作失败')
  }
}

const changeDeviceStatus = async (row: DeviceEntityVO, status: string) => {
  try {
    await updateDeviceStatus(row.id, status)
    ElMessage.success('设备状态已更新')
    await loadDevices()
  } catch (error) {
    ElMessage.error((error as { message?: string }).message ?? '状态更新失败')
  }
}

const loadTerminals = async () => {
  terminalLoading.value = true
  try {
    const res = await queryTerminals({
      keyword: terminalQuery.keyword || undefined,
      clientType: terminalQuery.clientType || undefined,
      status: terminalQuery.status || undefined,
      current: terminalCurrent.value,
      size: terminalSize.value,
    })
    terminalRows.value = res.records
    terminalTotal.value = res.total
  } catch (error) {
    ElMessage.error((error as { message?: string }).message ?? '终端列表加载失败')
  } finally {
    terminalLoading.value = false
  }
}

const openTerminalCreate = () => {
  Object.assign(terminalForm, {
    id: undefined,
    terminalCode: '',
    deviceEntityId: undefined,
    userAgent: '',
    osName: '',
    osVersion: '',
    clientType: '',
    clientVersion: '',
    ipAddress: '',
    status: 'ENABLED',
    metadata: '',
  })
  terminalDialog.value = true
}

const openTerminalEdit = (row: TerminalInfoVO) => {
  Object.assign(terminalForm, {
    id: row.id,
    terminalCode: row.terminalCode,
    deviceEntityId: row.deviceEntityId,
    userAgent: row.userAgent,
    osName: row.osName,
    osVersion: row.osVersion,
    clientType: row.clientType,
    clientVersion: row.clientVersion,
    ipAddress: row.ipAddress,
    status: row.status,
    metadata: row.metadata,
  })
  terminalDialog.value = true
}

const submitTerminal = async () => {
  try {
    if (terminalForm.id) {
      await updateTerminal(terminalForm.id, {
        deviceEntityId: terminalForm.deviceEntityId,
        userAgent: terminalForm.userAgent || undefined,
        osName: terminalForm.osName || undefined,
        osVersion: terminalForm.osVersion || undefined,
        clientType: terminalForm.clientType,
        clientVersion: terminalForm.clientVersion || undefined,
        ipAddress: terminalForm.ipAddress || undefined,
        status: terminalForm.status,
        metadata: terminalForm.metadata || undefined,
      })
      ElMessage.success('终端已更新')
    } else {
      await createTerminal({
        terminalCode: terminalForm.terminalCode,
        deviceEntityId: terminalForm.deviceEntityId,
        userAgent: terminalForm.userAgent || undefined,
        osName: terminalForm.osName || undefined,
        osVersion: terminalForm.osVersion || undefined,
        clientType: terminalForm.clientType,
        clientVersion: terminalForm.clientVersion || undefined,
        ipAddress: terminalForm.ipAddress || undefined,
        status: terminalForm.status,
        metadata: terminalForm.metadata || undefined,
      })
      ElMessage.success('终端已创建')
    }
    terminalDialog.value = false
    await loadTerminals()
  } catch (error) {
    ElMessage.error((error as { message?: string }).message ?? '终端操作失败')
  }
}

const changeTerminalStatus = async (row: TerminalInfoVO, status: string) => {
  try {
    await updateTerminalStatus(row.id, status)
    ElMessage.success('终端状态已更新')
    await loadTerminals()
  } catch (error) {
    ElMessage.error((error as { message?: string }).message ?? '状态更新失败')
  }
}

const loadBindings = async () => {
  bindingLoading.value = true
  try {
    const res = await queryBindings({
      subjectType: bindingQuery.subjectType || undefined,
      targetType: bindingQuery.targetType || undefined,
      relationType: bindingQuery.relationType || undefined,
      status: bindingQuery.status || undefined,
      current: bindingCurrent.value,
      size: bindingSize.value,
    })
    bindingRows.value = res.records
    bindingTotal.value = res.total
  } catch (error) {
    ElMessage.error((error as { message?: string }).message ?? '绑定关系加载失败')
  } finally {
    bindingLoading.value = false
  }
}

const submitBindUserTerminal = async () => {
  if (!bindForm.userId || !bindForm.terminalId) {
    ElMessage.warning('请填写 userId 和 terminalId')
    return
  }
  try {
    await bindUserTerminal({
      userId: bindForm.userId,
      terminalId: bindForm.terminalId,
      reason: bindForm.terminalReason || undefined,
    })
    ElMessage.success('用户-终端绑定成功')
    await loadBindings()
  } catch (error) {
    ElMessage.error((error as { message?: string }).message ?? '绑定失败')
  }
}

const submitBindDynamic = async () => {
  if (!bindForm.dynamicUserId || !bindForm.externalSubjectId) {
    ElMessage.warning('请填写 userId 和 externalSubjectId')
    return
  }
  try {
    await bindUserDynamicBiometric({
      userId: bindForm.dynamicUserId,
      externalSubjectId: bindForm.externalSubjectId,
      biometricType: bindForm.biometricType || undefined,
      modelSource: bindForm.modelSource || undefined,
      reason: bindForm.dynamicReason || undefined,
    })
    ElMessage.success('用户-动态生物绑定成功')
    await loadBindings()
  } catch (error) {
    ElMessage.error((error as { message?: string }).message ?? '绑定失败')
  }
}

const submitUnbind = async (row: BindingRelationVO) => {
  const reason = await ElMessageBox.prompt('请输入解绑原因（可选）', '解绑', {
    confirmButtonText: '确认',
    cancelButtonText: '取消',
  }).then((v) => v.value)
  try {
    await unbindRelation(row.id, reason || undefined)
    ElMessage.success('解绑成功')
    await loadBindings()
  } catch (error) {
    ElMessage.error((error as { message?: string }).message ?? '解绑失败')
  }
}

void loadDevices()
void loadTerminals()
void loadBindings()
</script>

<template>
  <div class="dash-page">
    <header class="dash-header admin">
      <strong>面向多网融合的人机物电子认证服务软件管理后台</strong>
      <span>管理员 · 设备与绑定管理</span>
    </header>

    <div class="dash-layout">
      <aside class="dash-aside">
        <router-link to="/admin/dashboard" class="menu-item">概览面板</router-link>
        <router-link to="/admin/login-logs" class="menu-item">登录日志</router-link>
        <router-link to="/admin/users" class="menu-item">用户管理</router-link>
        <router-link to="/admin/registrations" class="menu-item">身份注册</router-link>
        <router-link to="/admin/credentials" class="menu-item">生物凭据管理</router-link>
        <div class="menu-item active">设备与绑定</div>
        <router-link to="/admin/policy-app" class="menu-item">策略与应用</router-link>
        <router-link to="/admin/trust-risk" class="menu-item">信任与风险</router-link>
        <router-link to="/admin/audit-center" class="menu-item">审计中心</router-link>
        <router-link to="/admin/go-live" class="menu-item">系统联调</router-link>
        <div class="menu-item">数据资源管理</div>
      </aside>

      <main class="dash-content">
        <div class="crumb">管理后台 / 设备与绑定管理</div>
        <el-tabs v-model="activeTab" type="border-card">
          <el-tab-pane label="设备实体管理" name="devices">
            <section class="toolbar">
              <el-form :inline="true">
                <el-form-item label="关键字"><el-input v-model.trim="deviceQuery.keyword" placeholder="编码/名称" clearable /></el-form-item>
                <el-form-item label="类型"><el-input v-model.trim="deviceQuery.deviceType" clearable /></el-form-item>
                <el-form-item label="状态">
                  <el-select v-model="deviceQuery.status" clearable style="width: 130px;">
                    <el-option label="ENABLED" value="ENABLED" />
                    <el-option label="DISABLED" value="DISABLED" />
                  </el-select>
                </el-form-item>
                <el-form-item><el-button type="primary" @click="deviceCurrent = 1; loadDevices()">查询</el-button></el-form-item>
                <el-form-item><el-button type="success" @click="openDeviceCreate">新增设备</el-button></el-form-item>
              </el-form>
            </section>
            <el-card class="panel-card">
              <el-table v-loading="deviceLoading" :data="deviceRows">
                <el-table-column prop="id" label="ID" width="70" />
                <el-table-column prop="deviceCode" label="设备编码" min-width="130" />
                <el-table-column prop="deviceName" label="设备名称" min-width="120" />
                <el-table-column prop="deviceType" label="设备类型" min-width="120" />
                <el-table-column prop="status" label="状态" width="110" />
                <el-table-column label="操作" width="220">
                  <template #default="{ row }">
                    <el-space>
                      <el-button link type="primary" @click="openDeviceEdit(row)">编辑</el-button>
                      <el-button v-if="row.status !== 'ENABLED'" link type="success" @click="changeDeviceStatus(row, 'ENABLED')">启用</el-button>
                      <el-button v-if="row.status !== 'DISABLED'" link type="danger" @click="changeDeviceStatus(row, 'DISABLED')">禁用</el-button>
                    </el-space>
                  </template>
                </el-table-column>
              </el-table>
              <div style="display:flex;justify-content:flex-end;margin-top:12px;">
                <el-pagination v-model:current-page="deviceCurrent" v-model:page-size="deviceSize" :total="deviceTotal" layout="total, sizes, prev, pager, next" :page-sizes="[10,20,50]" @current-change="loadDevices" @size-change="deviceCurrent = 1; loadDevices()" />
              </div>
            </el-card>
          </el-tab-pane>

          <el-tab-pane label="终端管理" name="terminals">
            <section class="toolbar">
              <el-form :inline="true">
                <el-form-item label="关键字"><el-input v-model.trim="terminalQuery.keyword" placeholder="terminalCode/IP" clearable /></el-form-item>
                <el-form-item label="客户端类型"><el-input v-model.trim="terminalQuery.clientType" clearable /></el-form-item>
                <el-form-item label="状态">
                  <el-select v-model="terminalQuery.status" clearable style="width:130px;">
                    <el-option label="ENABLED" value="ENABLED" />
                    <el-option label="DISABLED" value="DISABLED" />
                  </el-select>
                </el-form-item>
                <el-form-item><el-button type="primary" @click="terminalCurrent = 1; loadTerminals()">查询</el-button></el-form-item>
                <el-form-item><el-button type="success" @click="openTerminalCreate">新增终端</el-button></el-form-item>
              </el-form>
            </section>
            <el-card class="panel-card">
              <el-table v-loading="terminalLoading" :data="terminalRows">
                <el-table-column prop="id" label="ID" width="70" />
                <el-table-column prop="terminalCode" label="终端编码" min-width="130" />
                <el-table-column prop="clientType" label="客户端类型" min-width="120" />
                <el-table-column prop="deviceEntityId" label="设备实体ID" width="120" />
                <el-table-column prop="status" label="状态" width="110" />
                <el-table-column label="操作" width="220">
                  <template #default="{ row }">
                    <el-space>
                      <el-button link type="primary" @click="openTerminalEdit(row)">编辑</el-button>
                      <el-button v-if="row.status !== 'ENABLED'" link type="success" @click="changeTerminalStatus(row, 'ENABLED')">启用</el-button>
                      <el-button v-if="row.status !== 'DISABLED'" link type="danger" @click="changeTerminalStatus(row, 'DISABLED')">禁用</el-button>
                    </el-space>
                  </template>
                </el-table-column>
              </el-table>
              <div style="display:flex;justify-content:flex-end;margin-top:12px;">
                <el-pagination v-model:current-page="terminalCurrent" v-model:page-size="terminalSize" :total="terminalTotal" layout="total, sizes, prev, pager, next" :page-sizes="[10,20,50]" @current-change="loadTerminals" @size-change="terminalCurrent = 1; loadTerminals()" />
              </div>
            </el-card>
          </el-tab-pane>

          <el-tab-pane label="绑定关系管理" name="bindings">
            <section class="toolbar">
              <el-form :inline="true">
                <el-form-item label="主体类型"><el-input v-model.trim="bindingQuery.subjectType" /></el-form-item>
                <el-form-item label="目标类型"><el-input v-model.trim="bindingQuery.targetType" clearable /></el-form-item>
                <el-form-item label="关系类型"><el-input v-model.trim="bindingQuery.relationType" clearable /></el-form-item>
                <el-form-item label="状态"><el-select v-model="bindingQuery.status" clearable style="width:130px;"><el-option label="ACTIVE" value="ACTIVE" /><el-option label="INACTIVE" value="INACTIVE" /></el-select></el-form-item>
                <el-form-item><el-button type="primary" @click="bindingCurrent = 1; loadBindings()">查询</el-button></el-form-item>
              </el-form>
            </section>

            <div class="split">
              <el-card class="panel-card" header="用户-终端绑定">
                <el-form label-position="top">
                  <el-form-item label="用户ID"><el-input-number v-model="bindForm.userId" :min="1" style="width:100%;" /></el-form-item>
                  <el-form-item label="终端ID"><el-input-number v-model="bindForm.terminalId" :min="1" style="width:100%;" /></el-form-item>
                  <el-form-item label="原因"><el-input v-model.trim="bindForm.terminalReason" /></el-form-item>
                  <el-button type="success" @click="submitBindUserTerminal">执行绑定</el-button>
                </el-form>
              </el-card>
              <el-card class="panel-card" header="用户-动态生物绑定">
                <el-form label-position="top">
                  <el-form-item label="用户ID"><el-input-number v-model="bindForm.dynamicUserId" :min="1" style="width:100%;" /></el-form-item>
                  <el-form-item label="externalSubjectId"><el-input v-model.trim="bindForm.externalSubjectId" /></el-form-item>
                  <el-form-item label="biometricType"><el-input v-model.trim="bindForm.biometricType" placeholder="可选" /></el-form-item>
                  <el-form-item label="modelSource"><el-input v-model.trim="bindForm.modelSource" placeholder="可选" /></el-form-item>
                  <el-form-item label="原因"><el-input v-model.trim="bindForm.dynamicReason" /></el-form-item>
                  <el-button type="success" @click="submitBindDynamic">执行绑定</el-button>
                </el-form>
              </el-card>
            </div>

            <el-card class="panel-card" style="margin-top:12px;">
              <el-table v-loading="bindingLoading" :data="bindingRows">
                <el-table-column prop="id" label="ID" width="70" />
                <el-table-column prop="subjectType" label="主体类型" width="90" />
                <el-table-column prop="subjectId" label="主体ID" width="90" />
                <el-table-column prop="targetType" label="目标类型" width="150" />
                <el-table-column prop="targetId" label="目标ID" width="120" />
                <el-table-column prop="relationType" label="关系类型" min-width="170" />
                <el-table-column prop="status" label="状态" width="100" />
                <el-table-column label="操作" width="100">
                  <template #default="{ row }">
                    <el-button v-if="row.status === 'ACTIVE'" link type="danger" @click="submitUnbind(row)">解绑</el-button>
                  </template>
                </el-table-column>
              </el-table>
              <div style="display:flex;justify-content:flex-end;margin-top:12px;">
                <el-pagination v-model:current-page="bindingCurrent" v-model:page-size="bindingSize" :total="bindingTotal" layout="total, sizes, prev, pager, next" :page-sizes="[10,20,50]" @current-change="loadBindings" @size-change="bindingCurrent = 1; loadBindings()" />
              </div>
            </el-card>
          </el-tab-pane>
        </el-tabs>
      </main>
    </div>

    <el-dialog v-model="deviceDialog" :title="deviceForm.id ? '编辑设备' : '新增设备'" width="760px">
      <div class="detail-grid">
        <el-form-item label="entityId"><el-input-number v-model="deviceForm.entityId" :min="1" style="width:100%;" /></el-form-item>
        <el-form-item label="设备编码"><el-input v-model.trim="deviceForm.deviceCode" :disabled="Boolean(deviceForm.id)" /></el-form-item>
        <el-form-item label="设备名称"><el-input v-model.trim="deviceForm.deviceName" /></el-form-item>
        <el-form-item label="设备类型"><el-input v-model.trim="deviceForm.deviceType" /></el-form-item>
        <el-form-item label="设备指纹"><el-input v-model.trim="deviceForm.deviceFingerprint" /></el-form-item>
        <el-form-item label="证书主体"><el-input v-model.trim="deviceForm.certSubject" /></el-form-item>
        <el-form-item label="状态"><el-select v-model="deviceForm.status"><el-option label="ENABLED" value="ENABLED" /><el-option label="DISABLED" value="DISABLED" /></el-select></el-form-item>
        <el-form-item label="来源系统"><el-input v-model.trim="deviceForm.sourceSystem" /></el-form-item>
      </div>
      <el-form-item label="描述"><el-input v-model="deviceForm.description" type="textarea" :rows="2" /></el-form-item>
      <el-form-item label="元数据"><el-input v-model="deviceForm.metadata" type="textarea" :rows="3" /></el-form-item>
      <template #footer><el-button @click="deviceDialog = false">取消</el-button><el-button type="primary" @click="submitDevice">保存</el-button></template>
    </el-dialog>

    <el-dialog v-model="terminalDialog" :title="terminalForm.id ? '编辑终端' : '新增终端'" width="760px">
      <div class="detail-grid">
        <el-form-item label="终端编码"><el-input v-model.trim="terminalForm.terminalCode" :disabled="Boolean(terminalForm.id)" /></el-form-item>
        <el-form-item label="deviceEntityId"><el-input-number v-model="terminalForm.deviceEntityId" :min="1" style="width:100%;" /></el-form-item>
        <el-form-item label="clientType"><el-input v-model.trim="terminalForm.clientType" /></el-form-item>
        <el-form-item label="clientVersion"><el-input v-model.trim="terminalForm.clientVersion" /></el-form-item>
        <el-form-item label="osName"><el-input v-model.trim="terminalForm.osName" /></el-form-item>
        <el-form-item label="osVersion"><el-input v-model.trim="terminalForm.osVersion" /></el-form-item>
        <el-form-item label="ipAddress"><el-input v-model.trim="terminalForm.ipAddress" /></el-form-item>
        <el-form-item label="状态"><el-select v-model="terminalForm.status"><el-option label="ENABLED" value="ENABLED" /><el-option label="DISABLED" value="DISABLED" /></el-select></el-form-item>
      </div>
      <el-form-item label="userAgent"><el-input v-model="terminalForm.userAgent" type="textarea" :rows="2" /></el-form-item>
      <el-form-item label="metadata"><el-input v-model="terminalForm.metadata" type="textarea" :rows="3" /></el-form-item>
      <template #footer><el-button @click="terminalDialog = false">取消</el-button><el-button type="primary" @click="submitTerminal">保存</el-button></template>
    </el-dialog>
  </div>
</template>
