<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { bindCertificate, issueCertificate, queryCertificates, revokeCertificate } from '../../api/certificate'
import { queryBiometricAuthRecords, queryBiometricTemplates, reviewBiometricTemplate, revokeBiometricTemplate } from '../../api/biometric'
import type { UserCertificateVO } from '../../types/certificate'
import type { BiometricAuthRecordVO, BiometricTemplateVO } from '../../types/biometric'

const activeTab = ref('certificates')

const certLoading = ref(false)
const certRows = ref<UserCertificateVO[]>([])
const certTotal = ref(0)
const certCurrent = ref(1)
const certSize = ref(10)
const certQuery = reactive({ username: '', status: '' })
const certForm = reactive({
  issueUserId: undefined as number | undefined,
  issueUsername: '',
  publicKeyPem: '',
  bindUserId: undefined as number | undefined,
  bindUsername: '',
  serialNumber: '',
  fingerprintSha256: '',
  certificatePem: '',
  issuer: '',
  subjectDn: '',
})

const templateLoading = ref(false)
const templateRows = ref<BiometricTemplateVO[]>([])
const templateTotal = ref(0)
const templateCurrent = ref(1)
const templateSize = ref(10)
const templateQuery = reactive({
  userId: undefined as number | undefined,
  biometricType: '',
  auditStatus: '',
  templateStatus: '',
})

const recordLoading = ref(false)
const recordRows = ref<BiometricAuthRecordVO[]>([])
const recordTotal = ref(0)
const recordCurrent = ref(1)
const recordSize = ref(10)
const recordQuery = reactive({
  userId: undefined as number | undefined,
  biometricType: '',
  authResult: '',
})

const loadCertificates = async () => {
  certLoading.value = true
  try {
    const res = await queryCertificates({
      username: certQuery.username || undefined,
      status: certQuery.status || undefined,
      current: certCurrent.value,
      size: certSize.value,
    })
    certRows.value = res.records
    certTotal.value = res.total
  } catch (error) {
    ElMessage.error((error as { message?: string }).message ?? '证书列表加载失败')
  } finally {
    certLoading.value = false
  }
}

const submitIssue = async () => {
  try {
    await issueCertificate({
      userId: certForm.issueUserId,
      username: certForm.issueUsername || undefined,
      publicKeyPem: certForm.publicKeyPem,
    })
    ElMessage.success('证书签发成功')
    await loadCertificates()
  } catch (error) {
    ElMessage.error((error as { message?: string }).message ?? '证书签发失败')
  }
}

const submitBind = async () => {
  try {
    await bindCertificate({
      userId: certForm.bindUserId,
      username: certForm.bindUsername || undefined,
      serialNumber: certForm.serialNumber,
      fingerprintSha256: certForm.fingerprintSha256,
      certificatePem: certForm.certificatePem,
      issuer: certForm.issuer || undefined,
      subjectDn: certForm.subjectDn || undefined,
    })
    ElMessage.success('证书绑定成功')
    await loadCertificates()
  } catch (error) {
    ElMessage.error((error as { message?: string }).message ?? '证书绑定失败')
  }
}

const submitRevokeCertificate = async (row: UserCertificateVO) => {
  const revokeReason = await ElMessageBox.prompt('请输入吊销原因', '证书吊销', {
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    inputPattern: /\S+/,
    inputErrorMessage: '吊销原因不能为空',
  }).then((v) => v.value)
  if (!revokeReason) return
  try {
    await revokeCertificate(row.id, revokeReason)
    ElMessage.success('证书已吊销')
    await loadCertificates()
  } catch (error) {
    ElMessage.error((error as { message?: string }).message ?? '证书吊销失败')
  }
}

const loadTemplates = async () => {
  templateLoading.value = true
  try {
    const res = await queryBiometricTemplates({
      userId: templateQuery.userId,
      biometricType: templateQuery.biometricType || undefined,
      auditStatus: templateQuery.auditStatus || undefined,
      templateStatus: templateQuery.templateStatus || undefined,
      current: templateCurrent.value,
      size: templateSize.value,
    })
    templateRows.value = res.records
    templateTotal.value = res.total
  } catch (error) {
    ElMessage.error((error as { message?: string }).message ?? '生物模板列表加载失败')
  } finally {
    templateLoading.value = false
  }
}

const submitReview = async (row: BiometricTemplateVO, status: 'APPROVED' | 'REJECTED') => {
  let rejectReason = ''
  if (status === 'REJECTED') {
    rejectReason = await ElMessageBox.prompt('请输入驳回原因', '模板审核', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      inputPattern: /\S+/,
      inputErrorMessage: '驳回原因不能为空',
    }).then((v) => v.value)
    if (!rejectReason) return
  }
  try {
    await reviewBiometricTemplate(row.id, status, rejectReason || undefined)
    ElMessage.success(status === 'APPROVED' ? '审核通过' : '审核驳回成功')
    await loadTemplates()
  } catch (error) {
    ElMessage.error((error as { message?: string }).message ?? '模板审核失败')
  }
}

const submitRevokeTemplate = async (row: BiometricTemplateVO) => {
  const reason = await ElMessageBox.prompt('请输入模板撤销原因', '模板撤销', {
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    inputPattern: /\S+/,
    inputErrorMessage: '撤销原因不能为空',
  }).then((v) => v.value)
  if (!reason) return
  try {
    await revokeBiometricTemplate(row.id, reason)
    ElMessage.success('模板已撤销')
    await loadTemplates()
  } catch (error) {
    ElMessage.error((error as { message?: string }).message ?? '模板撤销失败')
  }
}

const loadRecords = async () => {
  recordLoading.value = true
  try {
    const res = await queryBiometricAuthRecords({
      userId: recordQuery.userId,
      biometricType: recordQuery.biometricType || undefined,
      authResult: recordQuery.authResult || undefined,
      current: recordCurrent.value,
      size: recordSize.value,
    })
    recordRows.value = res.records
    recordTotal.value = res.total
  } catch (error) {
    ElMessage.error((error as { message?: string }).message ?? '生物认证流水加载失败')
  } finally {
    recordLoading.value = false
  }
}

void loadCertificates()
void loadTemplates()
void loadRecords()
</script>

<template>
  <div class="dash-page">
    <header class="dash-header admin">
      <strong>面向多网融合的人机物电子认证服务软件管理后台</strong>
      <span>管理员 · 生物凭据管理</span>
    </header>

    <div class="dash-layout">
      <aside class="dash-aside">
        <router-link to="/admin/dashboard" class="menu-item">概览面板</router-link>
        <router-link to="/admin/login-logs" class="menu-item">登录日志</router-link>
        <router-link to="/admin/users" class="menu-item">用户管理</router-link>
        <router-link to="/admin/registrations" class="menu-item">身份注册</router-link>
        <div class="menu-item active">生物凭据管理</div>
        <router-link to="/admin/device-binding" class="menu-item">设备与绑定</router-link>
        <router-link to="/admin/policy-app" class="menu-item">策略与应用</router-link>
        <router-link to="/admin/trust-risk" class="menu-item">信任与风险</router-link>
        <router-link to="/admin/audit-center" class="menu-item">审计中心</router-link>
        <router-link to="/admin/go-live" class="menu-item">系统联调</router-link>
        <div class="menu-item">数据资源管理</div>
      </aside>

      <main class="dash-content">
        <div class="crumb">管理后台 / 生物凭据管理</div>

        <el-tabs v-model="activeTab" type="border-card">
          <el-tab-pane label="证书管理" name="certificates">
            <section class="toolbar">
              <el-form :inline="true">
                <el-form-item label="用户名">
                  <el-input v-model.trim="certQuery.username" placeholder="按用户名筛选" clearable />
                </el-form-item>
                <el-form-item label="状态">
                  <el-select v-model="certQuery.status" clearable placeholder="全部" style="width: 140px;">
                    <el-option label="VALID" value="VALID" />
                    <el-option label="REVOKED" value="REVOKED" />
                  </el-select>
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" @click="certCurrent = 1; loadCertificates()">查询</el-button>
                </el-form-item>
              </el-form>
            </section>

            <div class="split">
              <el-card class="panel-card" header="证书签发">
                <el-form label-position="top">
                  <el-form-item label="用户ID">
                    <el-input-number v-model="certForm.issueUserId" :min="1" style="width: 100%;" />
                  </el-form-item>
                  <el-form-item label="用户名（与用户ID二选一）">
                    <el-input v-model.trim="certForm.issueUsername" />
                  </el-form-item>
                  <el-form-item label="公钥PEM">
                    <el-input v-model="certForm.publicKeyPem" type="textarea" :rows="4" />
                  </el-form-item>
                  <el-button type="primary" @click="submitIssue">签发证书</el-button>
                </el-form>
              </el-card>

              <el-card class="panel-card" header="证书绑定">
                <el-form label-position="top">
                  <el-form-item label="用户ID">
                    <el-input-number v-model="certForm.bindUserId" :min="1" style="width: 100%;" />
                  </el-form-item>
                  <el-form-item label="用户名（与用户ID二选一）">
                    <el-input v-model.trim="certForm.bindUsername" />
                  </el-form-item>
                  <el-form-item label="序列号">
                    <el-input v-model.trim="certForm.serialNumber" />
                  </el-form-item>
                  <el-form-item label="指纹SHA256">
                    <el-input v-model.trim="certForm.fingerprintSha256" />
                  </el-form-item>
                  <el-form-item label="证书PEM">
                    <el-input v-model="certForm.certificatePem" type="textarea" :rows="3" />
                  </el-form-item>
                  <el-form-item label="Issuer">
                    <el-input v-model.trim="certForm.issuer" />
                  </el-form-item>
                  <el-form-item label="Subject DN">
                    <el-input v-model.trim="certForm.subjectDn" />
                  </el-form-item>
                  <el-button type="success" @click="submitBind">绑定证书</el-button>
                </el-form>
              </el-card>
            </div>

            <el-card class="panel-card" style="margin-top: 12px;">
              <el-table v-loading="certLoading" :data="certRows">
                <el-table-column prop="id" label="ID" width="70" />
                <el-table-column prop="username" label="用户名" width="120" />
                <el-table-column prop="serialNumber" label="序列号" min-width="200" />
                <el-table-column prop="status" label="状态" width="110">
                  <template #default="{ row }">
                    <el-tag :type="row.status === 'VALID' ? 'success' : 'danger'">{{ row.status }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="issuedAt" label="签发时间" min-width="180" />
                <el-table-column label="操作" width="120">
                  <template #default="{ row }">
                    <el-button v-if="row.status === 'VALID'" link type="danger" @click="submitRevokeCertificate(row)">吊销</el-button>
                  </template>
                </el-table-column>
              </el-table>
              <div style="display:flex;justify-content:flex-end;margin-top:12px;">
                <el-pagination
                  v-model:current-page="certCurrent"
                  v-model:page-size="certSize"
                  :total="certTotal"
                  layout="total, sizes, prev, pager, next"
                  :page-sizes="[10, 20, 50]"
                  @current-change="loadCertificates"
                  @size-change="certCurrent = 1; loadCertificates()"
                />
              </div>
            </el-card>
          </el-tab-pane>

          <el-tab-pane label="生物模板审核" name="templates">
            <section class="toolbar">
              <el-form :inline="true">
                <el-form-item label="用户ID">
                  <el-input-number v-model="templateQuery.userId" :min="1" />
                </el-form-item>
                <el-form-item label="类型">
                  <el-select v-model="templateQuery.biometricType" clearable style="width: 150px;">
                    <el-option label="VOICEPRINT" value="VOICEPRINT" />
                    <el-option label="DYNAMIC_BIOMETRIC" value="DYNAMIC_BIOMETRIC" />
                  </el-select>
                </el-form-item>
                <el-form-item label="审核状态">
                  <el-select v-model="templateQuery.auditStatus" clearable style="width: 140px;">
                    <el-option label="PENDING" value="PENDING" />
                    <el-option label="APPROVED" value="APPROVED" />
                    <el-option label="REJECTED" value="REJECTED" />
                  </el-select>
                </el-form-item>
                <el-form-item label="模板状态">
                  <el-select v-model="templateQuery.templateStatus" clearable style="width: 140px;">
                    <el-option label="ACTIVE" value="ACTIVE" />
                    <el-option label="REVOKED" value="REVOKED" />
                    <el-option label="REPLACED" value="REPLACED" />
                  </el-select>
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" @click="templateCurrent = 1; loadTemplates()">查询</el-button>
                </el-form-item>
              </el-form>
            </section>

            <el-card class="panel-card">
              <el-table v-loading="templateLoading" :data="templateRows">
                <el-table-column prop="id" label="ID" width="70" />
                <el-table-column prop="username" label="用户名" width="120" />
                <el-table-column prop="biometricType" label="类型" width="170" />
                <el-table-column prop="auditStatus" label="审核状态" width="120" />
                <el-table-column prop="templateStatus" label="模板状态" width="120" />
                <el-table-column prop="versionNo" label="版本" width="80" />
                <el-table-column prop="submittedAt" label="提交时间" min-width="180" />
                <el-table-column label="操作" width="220">
                  <template #default="{ row }">
                    <el-space>
                      <el-button v-if="row.auditStatus === 'PENDING'" link type="success" @click="submitReview(row, 'APPROVED')">通过</el-button>
                      <el-button v-if="row.auditStatus === 'PENDING'" link type="warning" @click="submitReview(row, 'REJECTED')">驳回</el-button>
                      <el-button v-if="row.templateStatus !== 'REVOKED'" link type="danger" @click="submitRevokeTemplate(row)">撤销</el-button>
                    </el-space>
                  </template>
                </el-table-column>
              </el-table>
              <div style="display:flex;justify-content:flex-end;margin-top:12px;">
                <el-pagination
                  v-model:current-page="templateCurrent"
                  v-model:page-size="templateSize"
                  :total="templateTotal"
                  layout="total, sizes, prev, pager, next"
                  :page-sizes="[10, 20, 50]"
                  @current-change="loadTemplates"
                  @size-change="templateCurrent = 1; loadTemplates()"
                />
              </div>
            </el-card>
          </el-tab-pane>

          <el-tab-pane label="生物认证流水" name="records">
            <section class="toolbar">
              <el-form :inline="true">
                <el-form-item label="用户ID">
                  <el-input-number v-model="recordQuery.userId" :min="1" />
                </el-form-item>
                <el-form-item label="类型">
                  <el-select v-model="recordQuery.biometricType" clearable style="width: 150px;">
                    <el-option label="VOICEPRINT" value="VOICEPRINT" />
                    <el-option label="DYNAMIC_BIOMETRIC" value="DYNAMIC_BIOMETRIC" />
                  </el-select>
                </el-form-item>
                <el-form-item label="结果">
                  <el-select v-model="recordQuery.authResult" clearable style="width: 120px;">
                    <el-option label="SUCCESS" value="SUCCESS" />
                    <el-option label="FAIL" value="FAIL" />
                  </el-select>
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" @click="recordCurrent = 1; loadRecords()">查询</el-button>
                </el-form-item>
              </el-form>
            </section>

            <el-card class="panel-card">
              <el-table v-loading="recordLoading" :data="recordRows">
                <el-table-column prop="id" label="ID" width="70" />
                <el-table-column prop="username" label="用户名" width="120" />
                <el-table-column prop="biometricType" label="类型" width="170" />
                <el-table-column prop="score" label="分数" width="100" />
                <el-table-column prop="threshold" label="阈值" width="100" />
                <el-table-column prop="authResult" label="结果" width="100" />
                <el-table-column prop="failReason" label="失败原因" min-width="180" />
                <el-table-column prop="createdAt" label="时间" min-width="180" />
              </el-table>
              <div style="display:flex;justify-content:flex-end;margin-top:12px;">
                <el-pagination
                  v-model:current-page="recordCurrent"
                  v-model:page-size="recordSize"
                  :total="recordTotal"
                  layout="total, sizes, prev, pager, next"
                  :page-sizes="[10, 20, 50]"
                  @current-change="loadRecords"
                  @size-change="recordCurrent = 1; loadRecords()"
                />
              </div>
            </el-card>
          </el-tab-pane>
        </el-tabs>
      </main>
    </div>
  </div>
</template>
