<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { adminLogin } from '@/api/admin'
import { ADMIN_DASHBOARD_PATH, saveAdminSession } from '@/utils/adminSession'
import { getAdminErrorMessage } from '@/utils/adminRequest'

/**
 * 平台管理端登录页（公开页，不走 MainLayout；独立 adminToken 会话）。
 */
const route = useRoute()
const router = useRouter()
const { t } = useI18n()

const formRef = ref<FormInstance>()
const submitting = ref(false)
const form = reactive({
  username: '',
  password: '',
})

const rules: FormRules = {
  username: [
    { required: true, message: t('admin.login.validation.usernameRequired'), trigger: 'blur' },
  ],
  password: [
    { required: true, message: t('admin.login.validation.passwordRequired'), trigger: 'blur' },
  ],
}

const resolveRedirect = () => {
  const redirect = route.query.redirect
  if (typeof redirect === 'string' && redirect.startsWith('/admin')) {
    return redirect
  }
  return ADMIN_DASHBOARD_PATH
}

const handleSubmit = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }

  submitting.value = true
  try {
    const response = await adminLogin(form.username.trim(), form.password)
    if (!response.success || !response.data?.token) {
      throw new Error(response.message || t('admin.login.failed'))
    }
    saveAdminSession(response.data.token, {
      username: response.data.username,
      role: response.data.role,
    })
    ElMessage.success(t('admin.login.success'))
    router.replace(resolveRedirect())
  } catch (error) {
    ElMessage.error(getAdminErrorMessage(error, 'admin.login.failed'))
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="admin-login-page">
    <el-card class="admin-login-card">
      <div class="login-brand">
        <h1 class="login-title">{{ t('admin.login.title') }}</h1>
        <p class="login-subtitle">{{ t('admin.login.subtitle') }}</p>
      </div>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        @submit.prevent="handleSubmit"
      >
        <el-form-item :label="t('admin.login.username')" prop="username">
          <el-input
            v-model.trim="form.username"
            autocomplete="username"
            :placeholder="t('admin.login.usernamePlaceholder')"
            :disabled="submitting"
          />
        </el-form-item>
        <el-form-item :label="t('admin.login.password')" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            show-password
            autocomplete="current-password"
            :placeholder="t('admin.login.passwordPlaceholder')"
            :disabled="submitting"
            @keyup.enter="handleSubmit"
          />
        </el-form-item>
        <el-button class="login-submit" type="primary" native-type="submit" :loading="submitting">
          {{ t('admin.login.submit') }}
        </el-button>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped>
.admin-login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: linear-gradient(135deg, #1f2d3d 0%, #2f4358 100%);
}

.admin-login-card {
  width: min(400px, 100%);
}

.login-brand {
  margin-bottom: 20px;
  text-align: center;
}

.login-title {
  margin: 0 0 6px;
  color: #1f2d3d;
  font-size: 20px;
}

.login-subtitle {
  margin: 0;
  color: #909399;
  font-size: 13px;
}

.login-submit {
  width: 100%;
  margin-top: 8px;
}
</style>
