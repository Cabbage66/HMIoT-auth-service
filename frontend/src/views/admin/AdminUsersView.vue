<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { getUserDetail, queryUsers, syncUserAttributes, updateUser } from '../../api/user'
import type { UserVO } from '../../types/user'

const loading = ref(false)
const users = ref<UserVO[]>([])
const total = ref(0)
const current = ref(1)
const size = ref(10)

const query = reactive({
  keyword: '',
  status: '',
})

const detailLoading = ref(false)
const selectedUser = ref<UserVO | null>(null)
const editDialog = ref(false)
const editFormRef = ref<FormInstance>()
const editLoading = ref(false)
const syncLoading = ref(false)

const editForm = reactive({
  realName: '',
  phone: '',
  email: '',
  idCardNo: '',
  status: 'ENABLED',
})

const rules: FormRules<typeof editForm> = {
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }],
}

const loadUsers = async () => {
  loading.value = true
  try {
    const res = await queryUsers({
      keyword: query.keyword || undefined,
      status: query.status || undefined,
      current: current.value,
      size: size.value,
    })
    users.value = res.records
    total.value = res.total
  } catch (error) {
    ElMessage.error((error as { message?: string }).message ?? '用户列表加载失败')
  } finally {
    loading.value = false
  }
}

const openDetail = async (userId: number) => {
  detailLoading.value = true
  try {
    selectedUser.value = await getUserDetail(userId)
    editForm.realName = selectedUser.value.realName || ''
    editForm.phone = selectedUser.value.phone || ''
    editForm.email = selectedUser.value.email || ''
    editForm.idCardNo = selectedUser.value.idCardNo || ''
    editForm.status = selectedUser.value.status || 'ENABLED'
    editDialog.value = true
  } catch (error) {
    ElMessage.error((error as { message?: string }).message ?? '用户详情加载失败')
  } finally {
    detailLoading.value = false
  }
}

const saveUser = async () => {
  if (!selectedUser.value || !editFormRef.value) return
  const pass = await editFormRef.value.validate().catch(() => false)
  if (!pass) return
  editLoading.value = true
  try {
    selectedUser.value = await updateUser(selectedUser.value.id, editForm)
    await loadUsers()
    ElMessage.success('用户信息已更新')
  } catch (error) {
    ElMessage.error((error as { message?: string }).message ?? '用户更新失败')
  } finally {
    editLoading.value = false
  }
}

const syncAttributes = async () => {
  if (!selectedUser.value) return
  syncLoading.value = true
  try {
    const res = await syncUserAttributes(selectedUser.value.id)
    selectedUser.value = res.user
    await loadUsers()
    ElMessage.success('属性同步成功')
  } catch (error) {
    ElMessage.error((error as { message?: string }).message ?? '属性同步失败')
  } finally {
    syncLoading.value = false
  }
}

void loadUsers()
</script>

<template>
  <div class="dash-page">
    <header class="dash-header admin">
      <strong>面向多网融合的人机物电子认证服务软件管理后台</strong>
      <span>管理员 · 用户管理</span>
    </header>

    <div class="dash-layout">
      <aside class="dash-aside">
        <router-link to="/admin/dashboard" class="menu-item">概览面板</router-link>
        <router-link to="/admin/login-logs" class="menu-item">登录日志</router-link>
        <div class="menu-item active">用户管理</div>
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
        <div class="crumb">管理后台 / 用户管理</div>

        <section class="toolbar">
          <el-form :inline="true">
            <el-form-item label="关键字">
              <el-input v-model.trim="query.keyword" placeholder="用户名/姓名/手机号" clearable />
            </el-form-item>
            <el-form-item label="状态">
              <el-select v-model="query.status" style="width: 140px;" clearable placeholder="全部">
                <el-option label="启用" value="ENABLED" />
                <el-option label="禁用" value="DISABLED" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="current = 1; loadUsers()">查询</el-button>
            </el-form-item>
          </el-form>
        </section>

        <el-card class="panel-card">
          <el-table v-loading="loading" :data="users">
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="username" label="账号" min-width="120" />
            <el-table-column prop="realName" label="姓名" min-width="100" />
            <el-table-column prop="phone" label="手机号" min-width="130" />
            <el-table-column prop="status" label="状态" width="110">
              <template #default="{ row }">
                <el-tag :type="row.status === 'ENABLED' ? 'success' : 'danger'">{{ row.status }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="registerSource" label="注册来源" min-width="120" />
            <el-table-column label="操作" width="120">
              <template #default="{ row }">
                <el-button :loading="detailLoading" link type="primary" @click="openDetail(row.id)">详情</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div style="display:flex;justify-content:flex-end;margin-top:12px;">
            <el-pagination
              v-model:current-page="current"
              v-model:page-size="size"
              :total="total"
              layout="total, sizes, prev, pager, next"
              :page-sizes="[10, 20, 50]"
              @current-change="loadUsers"
              @size-change="current = 1; loadUsers()"
            />
          </div>
        </el-card>

        <el-dialog v-model="editDialog" title="用户详情与编辑" width="980px">
          <div class="split">
            <el-card class="panel-card" header="基础信息编辑">
              <el-form ref="editFormRef" :model="editForm" :rules="rules" label-position="top">
                <el-form-item label="姓名" prop="realName">
                  <el-input v-model="editForm.realName" />
                </el-form-item>
                <el-form-item label="手机号">
                  <el-input v-model="editForm.phone" />
                </el-form-item>
                <el-form-item label="邮箱" prop="email">
                  <el-input v-model="editForm.email" />
                </el-form-item>
                <el-form-item label="证件号">
                  <el-input v-model="editForm.idCardNo" />
                </el-form-item>
                <el-form-item label="状态">
                  <el-select v-model="editForm.status">
                    <el-option label="启用" value="ENABLED" />
                    <el-option label="禁用" value="DISABLED" />
                  </el-select>
                </el-form-item>
                <el-space>
                  <el-button :loading="editLoading" type="primary" @click="saveUser">保存更新</el-button>
                  <el-button :loading="syncLoading" type="success" @click="syncAttributes">同步外部属性</el-button>
                </el-space>
              </el-form>
            </el-card>

            <el-card class="panel-card" header="属性信息">
              <div class="detail-grid">
                <div class="kv-item">
                  <div class="k">部门</div>
                  <div class="v">{{ selectedUser?.profile?.department || '-' }}</div>
                </div>
                <div class="kv-item">
                  <div class="k">岗位</div>
                  <div class="v">{{ selectedUser?.profile?.positionName || '-' }}</div>
                </div>
                <div class="kv-item">
                  <div class="k">属性版本</div>
                  <div class="v">{{ selectedUser?.profile?.attributeVersion || '-' }}</div>
                </div>
                <div class="kv-item">
                  <div class="k">外部来源</div>
                  <div class="v">{{ selectedUser?.profile?.externalSource || '-' }}</div>
                </div>
                <div class="kv-item">
                  <div class="k">外部用户ID</div>
                  <div class="v">{{ selectedUser?.profile?.externalUserId || '-' }}</div>
                </div>
                <div class="kv-item">
                  <div class="k">同步时间</div>
                  <div class="v">{{ selectedUser?.profile?.syncedAt || '-' }}</div>
                </div>
              </div>
              <el-divider />
              <div class="k">外部属性 JSON</div>
              <el-input :model-value="selectedUser?.profile?.externalAttributes || '-'" type="textarea" :rows="6" readonly />
            </el-card>
          </div>
        </el-dialog>
      </main>
    </div>
  </div>
</template>
