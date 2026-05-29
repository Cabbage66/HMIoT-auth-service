<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  approveRegistration,
  createOfflineBatch,
  createOfflineRegistration,
  queryAdminRegistrations,
  queryRegistrationProgress,
  rejectRegistration,
} from '../../api/registration'
import type { AdminRegistrationVO, RegistrationProgressVO } from '../../types/registration'

const activeTab = ref('review')

const reviewLoading = ref(false)
const reviewRows = ref<AdminRegistrationVO[]>([])
const reviewTotal = ref(0)
const reviewCurrent = ref(1)
const reviewSize = ref(10)
const reviewQuery = reactive({ keyword: '' })

const progressDialog = ref(false)
const progressLoading = ref(false)
const progressRows = ref<RegistrationProgressVO[]>([])

const manualSubmitting = ref(false)
const manualAutoApprove = ref(true)
const manualForm = reactive({
  username: '',
  realName: '',
  phone: '',
  email: '',
  idCardNo: '',
  password: '',
})

const batchSubmitting = ref(false)
const batchAutoApprove = ref(true)
const batchText = ref('')
const batchResults = ref<
  Array<{
    username: string
    status: 'SUCCESS' | 'FAIL'
    message: string
  }>
>([])

const randomPassword = () => `Temp#${Math.random().toString(36).slice(2, 10)}A1`

const loadPending = async () => {
  reviewLoading.value = true
  try {
    const res = await queryAdminRegistrations({
      keyword: reviewQuery.keyword || undefined,
      status: 'PENDING',
      current: reviewCurrent.value,
      size: reviewSize.value,
    })
    reviewRows.value = res.records
    reviewTotal.value = res.total
  } catch (e) {
    ElMessage.error((e as { message?: string }).message ?? '注册申请加载失败')
  } finally {
    reviewLoading.value = false
  }
}

const openProgress = async (username: string) => {
  progressLoading.value = true
  progressRows.value = []
  progressDialog.value = true
  try {
    const res = await queryRegistrationProgress(username, 1, 20)
    progressRows.value = res.records
  } catch (e) {
    ElMessage.error((e as { message?: string }).message ?? '注册进度加载失败')
  } finally {
    progressLoading.value = false
  }
}

const approve = async (row: AdminRegistrationVO) => {
  try {
    await approveRegistration(row.registrationId)
    ElMessage.success('审核通过')
    await loadPending()
  } catch (e) {
    ElMessage.error((e as { message?: string }).message ?? '审核失败')
  }
}

const reject = async (row: AdminRegistrationVO) => {
  const reason = await ElMessageBox.prompt('请输入驳回原因', '驳回申请', {
    inputPlaceholder: '驳回原因',
    inputValue: '',
  })
    .then((v) => v.value.trim())
    .catch(() => null)
  if (reason === null) return
  try {
    await rejectRegistration(row.registrationId, reason || '管理员驳回')
    ElMessage.success(`已驳回：${reason || '未填写原因'}`)
    await loadPending()
  } catch (e) {
    ElMessage.error((e as { message?: string }).message ?? '驳回失败')
  }
}

const submitManual = async () => {
  if (!manualForm.username || !manualForm.realName) {
    ElMessage.warning('请填写用户名与姓名')
    return
  }
  manualSubmitting.value = true
  try {
    await createOfflineRegistration({
      username: manualForm.username,
      password: manualForm.password || randomPassword(),
      realName: manualForm.realName,
      phone: manualForm.phone || undefined,
      email: manualForm.email || undefined,
      idCardNo: manualForm.idCardNo || undefined,
      autoApprove: manualAutoApprove.value,
    })
    ElMessage.success(manualAutoApprove.value ? '离线注册成功并已审核通过' : '离线注册成功，待审核')
    Object.assign(manualForm, { username: '', realName: '', phone: '', email: '', idCardNo: '', password: '' })
    await loadPending()
  } catch (e) {
    ElMessage.error((e as { message?: string }).message ?? '离线注册失败')
  } finally {
    manualSubmitting.value = false
  }
}

const parseBatchLines = () => {
  const lines = batchText.value
    .split('\n')
    .map((line) => line.trim())
    .filter(Boolean)
  return lines.map((line, index) => {
    const [username = '', realName = '', phone = '', email = '', idCardNo = '', password = ''] = line
      .split(',')
      .map((x) => x.trim())
    return { lineNo: index + 1, username, realName, phone, email, idCardNo, password, autoApprove: batchAutoApprove.value }
  })
}

const submitBatch = async () => {
  const rows = parseBatchLines()
  if (rows.length === 0) {
    ElMessage.warning('请填写批量数据')
    return
  }
  batchSubmitting.value = true
  batchResults.value = []
  try {
    const validRows = rows.filter((row) => row.username && row.realName)
    const invalidRows = rows.filter((row) => !row.username || !row.realName)
    for (const row of invalidRows) {
      batchResults.value.push({
        username: row.username || `line-${row.lineNo}`,
        status: 'FAIL',
        message: `第${row.lineNo}行缺少用户名或姓名`,
      })
    }
    if (validRows.length > 0) {
      const response = await createOfflineBatch(
        validRows.map((row) => ({
          username: row.username,
          password: row.password || randomPassword(),
          realName: row.realName,
          phone: row.phone || undefined,
          email: row.email || undefined,
          idCardNo: row.idCardNo || undefined,
          autoApprove: row.autoApprove,
        })),
      )
      for (const item of response.items) {
        batchResults.value.push({
          username: item.username,
          status: item.success ? 'SUCCESS' : 'FAIL',
          message: item.message,
        })
      }
    }
    const failCount = batchResults.value.filter((x) => x.status === 'FAIL').length
    if (failCount > 0) ElMessage.warning(`批量注册完成，失败 ${failCount} 条`)
    else ElMessage.success('批量注册完成')
    await loadPending()
  } finally {
    batchSubmitting.value = false
  }
}

void loadPending()
</script>

<template>
  <div class="dash-page">
    <header class="dash-header admin">
      <strong>面向多网融合的人机物电子认证服务软件管理后台</strong>
      <span>管理员 · 身份注册</span>
    </header>
    <div class="dash-layout">
      <aside class="dash-aside">
        <router-link to="/admin/dashboard" class="menu-item">概览面板</router-link>
        <router-link to="/admin/login-logs" class="menu-item">登录日志</router-link>
        <router-link to="/admin/users" class="menu-item">用户管理</router-link>
        <div class="menu-item active">身份注册</div>
        <router-link to="/admin/credentials" class="menu-item">生物凭据管理</router-link>
        <router-link to="/admin/device-binding" class="menu-item">设备与绑定</router-link>
        <router-link to="/admin/policy-app" class="menu-item">策略与应用</router-link>
        <router-link to="/admin/trust-risk" class="menu-item">信任与风险</router-link>
        <router-link to="/admin/audit-center" class="menu-item">审计中心</router-link>
        <router-link to="/admin/go-live" class="menu-item">系统联调</router-link>
        <div class="menu-item">数据资源管理</div>
      </aside>
      <main class="dash-content">
        <div class="crumb">管理后台 / 身份注册</div>
        <el-tabs v-model="activeTab" type="border-card">
          <el-tab-pane label="注册申请与审核" name="review">
            <section class="toolbar">
              <el-form :inline="true">
                <el-form-item label="关键字">
                  <el-input v-model.trim="reviewQuery.keyword" placeholder="用户名/姓名/手机号" clearable />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" @click="reviewCurrent = 1; loadPending()">查询待审核</el-button>
                </el-form-item>
              </el-form>
            </section>
            <el-card class="panel-card">
              <el-table v-loading="reviewLoading" :data="reviewRows">
                <el-table-column prop="registrationId" label="申请ID" width="90" />
                <el-table-column prop="userId" label="用户ID" width="90" />
                <el-table-column prop="username" label="账号" min-width="130" />
                <el-table-column prop="realName" label="姓名" width="120" />
                <el-table-column prop="phone" label="手机号" width="140" />
                <el-table-column prop="email" label="邮箱" min-width="180" />
                <el-table-column prop="registerSource" label="来源" width="100" />
                <el-table-column prop="createdAt" label="申请时间" min-width="170" />
                <el-table-column label="操作" width="260">
                  <template #default="{ row }">
                    <el-space>
                      <el-button link type="primary" @click="openProgress(row.username)">进度</el-button>
                      <el-button link type="success" @click="approve(row)">通过</el-button>
                      <el-button link type="danger" @click="reject(row)">驳回</el-button>
                    </el-space>
                  </template>
                </el-table-column>
              </el-table>
              <div style="display:flex;justify-content:flex-end;margin-top:12px;">
                <el-pagination
                  v-model:current-page="reviewCurrent"
                  v-model:page-size="reviewSize"
                  :total="reviewTotal"
                  layout="total, sizes, prev, pager, next"
                  :page-sizes="[10,20,50]"
                  @current-change="loadPending"
                  @size-change="reviewCurrent = 1; loadPending()"
                />
              </div>
            </el-card>
          </el-tab-pane>

          <el-tab-pane label="离线注册" name="offline">
            <div class="split">
              <el-card class="panel-card" header="手动新增">
                <el-form label-position="top">
                  <div class="detail-grid">
                    <el-form-item label="用户名">
                      <el-input v-model.trim="manualForm.username" placeholder="4-64字符" />
                    </el-form-item>
                    <el-form-item label="姓名">
                      <el-input v-model.trim="manualForm.realName" />
                    </el-form-item>
                    <el-form-item label="手机号">
                      <el-input v-model.trim="manualForm.phone" />
                    </el-form-item>
                    <el-form-item label="邮箱">
                      <el-input v-model.trim="manualForm.email" />
                    </el-form-item>
                    <el-form-item label="证件号">
                      <el-input v-model.trim="manualForm.idCardNo" />
                    </el-form-item>
                    <el-form-item label="初始密码（可选）">
                      <el-input v-model.trim="manualForm.password" show-password />
                    </el-form-item>
                  </div>
                  <el-form-item>
                    <el-checkbox v-model="manualAutoApprove">提交后自动审核通过</el-checkbox>
                  </el-form-item>
                  <el-button type="primary" :loading="manualSubmitting" @click="submitManual">提交离线注册</el-button>
                </el-form>
              </el-card>

              <el-card class="panel-card" header="批量导入">
                <el-form label-position="top">
                  <el-form-item label="导入内容（每行：username,realName,phone,email,idCardNo,password）">
                    <el-input
                      v-model="batchText"
                      type="textarea"
                      :rows="10"
                      placeholder="usera,张三,13800138000,usera@test.com,ID001,Pass#1234"
                    />
                  </el-form-item>
                  <el-form-item>
                    <el-checkbox v-model="batchAutoApprove">导入后自动审核通过</el-checkbox>
                  </el-form-item>
                  <el-button type="primary" :loading="batchSubmitting" @click="submitBatch">执行批量导入</el-button>
                </el-form>
                <el-divider />
                <el-table :data="batchResults" max-height="240">
                  <el-table-column prop="username" label="用户名" min-width="140" />
                  <el-table-column prop="status" label="状态" width="100" />
                  <el-table-column prop="message" label="结果" min-width="200" />
                </el-table>
              </el-card>
            </div>
          </el-tab-pane>
        </el-tabs>
      </main>
    </div>
  </div>

  <el-dialog v-model="progressDialog" title="注册进度" width="860px">
    <el-table v-loading="progressLoading" :data="progressRows" max-height="420">
      <el-table-column prop="registrationId" label="申请ID" width="100" />
      <el-table-column prop="username" label="账号" width="140" />
      <el-table-column prop="status" label="状态" width="100" />
      <el-table-column prop="rejectReason" label="驳回原因" min-width="180" />
      <el-table-column prop="reviewedAt" label="审核时间" min-width="170" />
      <el-table-column prop="createdAt" label="申请时间" min-width="170" />
    </el-table>
  </el-dialog>
</template>
