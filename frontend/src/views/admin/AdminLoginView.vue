<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { loginAdmin } from '../../api/auth'
import { useAdminAuthStore } from '../../stores/auth'

const router = useRouter()
const authStore = useAdminAuthStore()
const loading = ref(false)
const formRef = ref<FormInstance>()
const form = reactive({
  username: '',
  password: '',
})

const rules: FormRules<typeof form> = {
  username: [{ required: true, message: '请输入管理员账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

const submit = async () => {
  if (!formRef.value) return
  const pass = await formRef.value.validate().catch(() => false)
  if (!pass) return
  loading.value = true
  try {
    const data = await loginAdmin(form)
    authStore.setAdminSession(data)
    ElMessage.success('登录成功')
    await router.push('/admin/dashboard')
  } catch (error) {
    const message = (error as { message?: string }).message ?? '登录失败'
    ElMessage.error(message)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page admin">
    <div class="login-shell login-shell-single">
      <section class="login-panel">
        <h1 class="panel-title">管理端登录</h1>
        <p class="panel-subtitle">多网融合人机物电子认证服务后台</p>
        <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent>
          <el-form-item label="管理员账号" prop="username">
            <el-input v-model.trim="form.username" clearable placeholder="请输入管理员账号" />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" />
          </el-form-item>
          <el-button :loading="loading" type="primary" style="width: 100%;" @click="submit">登录</el-button>
        </el-form>
      </section>
    </div>
  </div>
</template>
