<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { requestUsbChallenge, verifyUsbLogin } from '../../api/auth'
import { useAdminAuthStore } from '../../stores/auth'
import type { UsbChallengeResponse } from '../../types/auth'

const router = useRouter()
const authStore = useAdminAuthStore()
const loadingChallenge = ref(false)
const loadingVerify = ref(false)
const challengeFormRef = ref<FormInstance>()
const verifyFormRef = ref<FormInstance>()
const challengeResult = ref<UsbChallengeResponse | null>(null)

const challengeForm = reactive({
  username: '',
  terminalCode: '',
  clientType: 'WEB',
})

const verifyForm = reactive({
  signatureB64: '',
})

const challengeRules: FormRules<typeof challengeForm> = {
  username: [{ required: true, message: '请输入用户账号', trigger: 'blur' }],
  terminalCode: [{ required: true, message: '请输入终端编码', trigger: 'blur' }],
}

const verifyRules: FormRules<typeof verifyForm> = {
  signatureB64: [{ required: true, message: '请输入USB模块签名结果', trigger: 'blur' }],
}

const getChallenge = async () => {
  if (!challengeFormRef.value) return
  const pass = await challengeFormRef.value.validate().catch(() => false)
  if (!pass) return
  loadingChallenge.value = true
  try {
    challengeResult.value = await requestUsbChallenge({
      username: challengeForm.username,
      terminalCode: challengeForm.terminalCode,
      clientType: challengeForm.clientType,
      userAgent: navigator.userAgent,
    })
    ElMessage.success('挑战码获取成功')
  } catch (error) {
    ElMessage.error((error as { message?: string }).message ?? '挑战码获取失败')
  } finally {
    loadingChallenge.value = false
  }
}

const submitVerify = async () => {
  if (!verifyFormRef.value || !challengeResult.value) return
  const pass = await verifyFormRef.value.validate().catch(() => false)
  if (!pass) return
  loadingVerify.value = true
  try {
    const data = await verifyUsbLogin({
      username: challengeForm.username,
      sessionId: challengeResult.value.sessionId,
      challengeId: challengeResult.value.challengeId,
      signatureB64: verifyForm.signatureB64,
      terminalCode: challengeForm.terminalCode,
    })
    authStore.setUserSession(data)
    ElMessage.success('网页登录成功')
    await router.push('/web/apps')
  } catch (error) {
    ElMessage.error((error as { message?: string }).message ?? '登录失败')
  } finally {
    loadingVerify.value = false
  }
}
</script>

<template>
  <div class="login-page web">
    <div class="login-shell">
      <section class="login-panel">
        <h1 class="panel-title">网页客户端登录</h1>
        <p class="panel-subtitle">USB认证模块 + PIN/签名验证流程</p>

        <el-form ref="challengeFormRef" :model="challengeForm" :rules="challengeRules" label-position="top" @submit.prevent>
          <el-form-item label="用户账号" prop="username">
            <el-input v-model.trim="challengeForm.username" placeholder="请输入用户账号" />
          </el-form-item>
          <el-form-item label="终端编码" prop="terminalCode">
            <el-input v-model.trim="challengeForm.terminalCode" placeholder="请输入终端编码" />
          </el-form-item>
          <el-button :loading="loadingChallenge" type="primary" style="width: 100%;" @click="getChallenge">1. 获取挑战码</el-button>
        </el-form>

        <el-divider />

        <el-form ref="verifyFormRef" :model="verifyForm" :rules="verifyRules" label-position="top" @submit.prevent>
          <el-form-item label="USB签名结果（Base64）" prop="signatureB64">
            <el-input v-model.trim="verifyForm.signatureB64" type="textarea" :rows="3" placeholder="输入USB模块返回的signatureB64" />
          </el-form-item>
          <el-button :loading="loadingVerify" :disabled="!challengeResult" type="success" style="width: 100%;" @click="submitVerify">2. 提交验签登录</el-button>
        </el-form>
      </section>

      <section class="login-panel">
        <h2 class="section-title">挑战会话信息</h2>
        <el-descriptions :column="1" border>
          <el-descriptions-item label="sessionId">{{ challengeResult?.sessionId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="challengeId">{{ challengeResult?.challengeId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="challenge">{{ challengeResult?.challenge || '-' }}</el-descriptions-item>
          <el-descriptions-item label="过期时间">{{ challengeResult?.expiresAt || '-' }}</el-descriptions-item>
        </el-descriptions>
      </section>
    </div>
  </div>
</template>
