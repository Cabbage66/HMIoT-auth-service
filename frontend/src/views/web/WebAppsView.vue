<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { useAdminAuthStore } from '../../stores/auth'
import { issueTicket, keepAliveTicket, mockBusinessAppAccess, refreshTicket, revokeTicket, ssoAccess, ssoValidate, verifyTicket } from '../../api/web-access'
import type { IdentityTicketVO, SsoAccessResponse, SsoValidateResponse, TicketVerifyResponse } from '../../types/web-access'

const router = useRouter()
const authStore = useAdminAuthStore()

const apps = [
  { appCode: 'APP_A', appName: '应用A', targetSystem: 'SYSTEM_A' },
  { appCode: 'APP_B', appName: '应用B', targetSystem: 'SYSTEM_B' },
]

const selectedAppCode = ref('APP_A')
const audienceSystem = ref('HMIOT_AUTH_SERVICE')
const verifierSystem = ref('HMIOT_AUTH_SERVICE')
const targetSystem = ref('SYSTEM_B')
const requiredTrustLevel = ref<number | undefined>(undefined)
const loading = ref(false)
const ticket = ref<IdentityTicketVO | null>(null)
const ticketVerifyResult = ref<TicketVerifyResponse | null>(null)
const ssoAccessResult = ref<SsoAccessResponse | null>(null)
const ssoValidateResult = ref<SsoValidateResponse | null>(null)
const mockAccessResult = ref<SsoValidateResponse | null>(null)

const logout = async () => {
  authStore.clearUserSession()
  await router.push('/web/login')
}

const withLoading = async (fn: () => Promise<void>) => {
  loading.value = true
  try {
    await fn()
  } finally {
    loading.value = false
  }
}

const doIssueTicket = async () => {
  await withLoading(async () => {
    try {
      ticket.value = await issueTicket({
        appCode: selectedAppCode.value,
        audienceSystem: audienceSystem.value || undefined,
        trustLevel: requiredTrustLevel.value,
      })
      ElMessage.success('票据签发成功')
    } catch (e) {
      ElMessage.error((e as { message?: string }).message ?? '票据签发失败')
    }
  })
}

const doVerifyTicket = async () => {
  const currentTicket = ticket.value
  if (!currentTicket) return ElMessage.warning('请先签发票据')
  await withLoading(async () => {
    try {
      ticketVerifyResult.value = await verifyTicket({
        ticketId: currentTicket.ticketId,
        ticketPayload: currentTicket.ticketPayload,
        appCode: currentTicket.appCode,
        verifierSystem: verifierSystem.value,
        requiredTrustLevel: requiredTrustLevel.value,
      })
      ElMessage.success('票据校验请求已完成')
    } catch (e) {
      ElMessage.error((e as { message?: string }).message ?? '票据校验失败')
    }
  })
}

const doSsoAccess = async () => {
  await withLoading(async () => {
    try {
      ssoAccessResult.value = await ssoAccess({
        appCode: selectedAppCode.value,
        targetSystem: targetSystem.value,
      })
      ticket.value = ssoAccessResult.value.ticket
      ElMessage.success('SSO访问请求成功')
    } catch (e) {
      ElMessage.error((e as { message?: string }).message ?? 'SSO访问失败')
    }
  })
}

const doSsoValidate = async () => {
  const currentTicket = ticket.value
  if (!currentTicket) return ElMessage.warning('请先签发票据或执行SSO访问')
  await withLoading(async () => {
    try {
      ssoValidateResult.value = await ssoValidate({
        ticketId: currentTicket.ticketId,
        ticketPayload: currentTicket.ticketPayload,
        sourceSystem: currentTicket.issuerSystem,
        targetSystem: targetSystem.value,
        appCode: currentTicket.appCode,
        requiredTrustLevel: requiredTrustLevel.value,
      })
      ElMessage.success('SSO校验完成')
    } catch (e) {
      ElMessage.error((e as { message?: string }).message ?? 'SSO校验失败')
    }
  })
}

const doMockAppAccess = async () => {
  const currentTicket = ticket.value
  if (!currentTicket) return ElMessage.warning('请先签发票据或执行SSO访问')
  await withLoading(async () => {
    try {
      mockAccessResult.value = await mockBusinessAppAccess({
        ticketId: currentTicket.ticketId,
        ticketPayload: currentTicket.ticketPayload,
        sourceSystem: currentTicket.issuerSystem,
        targetSystem: targetSystem.value,
        appCode: currentTicket.appCode,
        requiredTrustLevel: requiredTrustLevel.value,
      })
      ElMessage.success('业务应用访问校验完成')
    } catch (e) {
      ElMessage.error((e as { message?: string }).message ?? '业务应用访问失败')
    }
  })
}

const doRefreshTicket = async () => {
  const currentTicket = ticket.value
  if (!currentTicket) return ElMessage.warning('暂无票据可刷新')
  await withLoading(async () => {
    try {
      ticket.value = await refreshTicket(currentTicket.ticketId)
      ElMessage.success('票据刷新成功')
    } catch (e) {
      ElMessage.error((e as { message?: string }).message ?? '票据刷新失败')
    }
  })
}

const doKeepAlive = async () => {
  const currentTicket = ticket.value
  if (!currentTicket) return ElMessage.warning('暂无票据可保活')
  await withLoading(async () => {
    try {
      ticket.value = await keepAliveTicket(currentTicket.ticketId)
      ElMessage.success('票据保活成功')
    } catch (e) {
      ElMessage.error((e as { message?: string }).message ?? '票据保活失败')
    }
  })
}

const doRevokeTicket = async () => {
  const currentTicket = ticket.value
  if (!currentTicket) return ElMessage.warning('暂无票据可失效')
  await withLoading(async () => {
    try {
      ticket.value = await revokeTicket(currentTicket.ticketId, 'WEB_CLIENT_USER_REVOKE')
      ElMessage.success('票据已失效')
    } catch (e) {
      ElMessage.error((e as { message?: string }).message ?? '票据失效失败')
    }
  })
}
</script>

<template>
  <div class="dash-page">
    <header class="dash-header web">
      <strong>网页客户端应用访问与SSO</strong>
      <el-space>
        <span>{{ authStore.userProfile?.realName || authStore.userProfile?.username }}</span>
        <el-button link style="color:#fff;" @click="logout">退出</el-button>
      </el-space>
    </header>

    <div class="dash-layout">
      <aside class="dash-aside">
        <div class="menu-item active">应用访问</div>
        <div class="menu-item">票据与SSO</div>
        <div class="menu-item">验证结果</div>
      </aside>

      <main class="dash-content">
        <div class="crumb">网页客户端 / 应用访问与SSO</div>

        <section class="toolbar">
          <el-form :inline="true">
            <el-form-item label="应用">
              <el-select v-model="selectedAppCode" style="width: 180px;">
                <el-option v-for="app in apps" :key="app.appCode" :label="`${app.appName} (${app.appCode})`" :value="app.appCode" />
              </el-select>
            </el-form-item>
            <el-form-item label="票据受众">
              <el-input v-model="audienceSystem" style="width: 200px;" />
            </el-form-item>
            <el-form-item label="验证系统">
              <el-input v-model="verifierSystem" style="width: 200px;" />
            </el-form-item>
            <el-form-item label="目标系统">
              <el-input v-model="targetSystem" style="width: 160px;" />
            </el-form-item>
            <el-form-item label="信任阈值">
              <el-input-number v-model="requiredTrustLevel" :min="0" :step="1" />
            </el-form-item>
          </el-form>
        </section>

        <div class="row panels">
          <el-card class="panel-card" header="票据流程">
            <el-space wrap>
              <el-button :loading="loading" type="primary" @click="doIssueTicket">签发票据</el-button>
              <el-button :loading="loading" @click="doVerifyTicket">校验票据</el-button>
              <el-button :loading="loading" type="success" @click="doRefreshTicket">刷新票据</el-button>
              <el-button :loading="loading" @click="doKeepAlive">票据保活</el-button>
              <el-button :loading="loading" type="danger" @click="doRevokeTicket">票据失效</el-button>
            </el-space>
            <el-divider />
            <el-descriptions :column="1" border>
              <el-descriptions-item label="ticketId">{{ ticket?.ticketId || '-' }}</el-descriptions-item>
              <el-descriptions-item label="appCode">{{ ticket?.appCode || '-' }}</el-descriptions-item>
              <el-descriptions-item label="audienceSystem">{{ ticket?.audienceSystem || '-' }}</el-descriptions-item>
              <el-descriptions-item label="trustLevel">{{ ticket?.trustLevel ?? '-' }}</el-descriptions-item>
              <el-descriptions-item label="expiresAt">{{ ticket?.expiresAt || '-' }}</el-descriptions-item>
            </el-descriptions>
          </el-card>

          <el-card class="panel-card" header="SSO与业务系统访问">
            <el-space wrap>
              <el-button :loading="loading" type="primary" @click="doSsoAccess">SSO Access</el-button>
              <el-button :loading="loading" @click="doSsoValidate">SSO Validate</el-button>
              <el-button :loading="loading" type="success" @click="doMockAppAccess">Mock业务应用访问</el-button>
            </el-space>
            <el-divider />
            <el-descriptions :column="1" border>
              <el-descriptions-item label="SSO Access">{{ ssoAccessResult?.message || '-' }}</el-descriptions-item>
              <el-descriptions-item label="SSO Validate">{{ ssoValidateResult?.message || '-' }}</el-descriptions-item>
              <el-descriptions-item label="业务应用访问">{{ mockAccessResult?.message || '-' }}</el-descriptions-item>
              <el-descriptions-item label="票据校验结果">{{ ticketVerifyResult?.message || '-' }}</el-descriptions-item>
            </el-descriptions>
          </el-card>
        </div>
      </main>
    </div>
  </div>
</template>
