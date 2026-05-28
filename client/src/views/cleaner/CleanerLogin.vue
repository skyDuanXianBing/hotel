<template>
  <div class="cleaner-login-container">
    <div class="login-card">
      <!-- Logo和标题 -->
      <div class="header">
        <div class="logo">
          <span class="logo-icon">◆</span>
          {{ t('app.name') }}
        </div>
        <h1 class="title">{{ t('stage5.cleaner.common.appName') }}</h1>
        <p class="subtitle">{{ t('stage5.cleaner.auth.subtitle') }}</p>
      </div>

      <!-- 登录表单 -->
      <el-form
        ref="formRef"
        :model="loginForm"
        :rules="rules"
        class="login-form"
        @submit.prevent="handleLogin"
      >
        <el-form-item prop="email">
          <el-input
            v-model="loginForm.email"
            :placeholder="t('stage5.cleaner.auth.emailPlaceholder')"
            size="large"
            clearable
          >
            <template #prefix>
              <el-icon><User /></el-icon>
            </template>
          </el-input>
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            :placeholder="t('stage5.cleaner.auth.passwordPlaceholder')"
            size="large"
            show-password
          >
            <template #prefix>
              <el-icon><Lock /></el-icon>
            </template>
          </el-input>
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            size="large"
            class="login-button"
            :loading="loading"
            @click="handleLogin"
          >
            {{ t('stage5.cleaner.auth.login') }}
          </el-button>
        </el-form-item>
      </el-form>

      <!-- 底部链接 -->
      <div class="footer">
        <p class="footer-text">
          {{ t('stage5.cleaner.auth.agreementPrefix') }}
          <a href="#" class="link">{{ t('stage5.cleaner.auth.terms') }}</a>
          {{ t('stage5.cleaner.auth.and') }}
          <a href="#" class="link">{{ t('stage5.cleaner.auth.privacy') }}</a>
        </p>
        <div class="footer-logo">
          <el-icon><Pointer /></el-icon>
          {{ t('app.name') }}
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { User, Lock, Pointer } from '@element-plus/icons-vue'
import { cleanerLoginByPassword } from '@/api/cleanerAuth'
import { saveCleanerSession, type CleanerSessionUser } from '@/utils/cleanerSession'

const router = useRouter()
const { t } = useI18n()

const formRef = ref<FormInstance>()
const loading = ref(false)

const loginForm = reactive({
  email: '',
  password: '',
})

const rules: FormRules = {
  email: [
    { required: true, message: t('stage5.cleaner.auth.emailRequired'), trigger: 'blur' },
    { type: 'email', message: t('stage5.cleaner.auth.emailInvalid'), trigger: 'blur' },
  ],
  password: [
    { required: true, message: t('stage5.cleaner.auth.passwordRequired'), trigger: 'blur' },
    { min: 6, message: t('stage5.cleaner.auth.passwordMin'), trigger: 'blur' },
  ],
}

const handleLogin = async () => {
  if (!formRef.value) return

  try {
    await formRef.value.validate()
    loading.value = true

    const response = await cleanerLoginByPassword({
      email: loginForm.email,
      password: loginForm.password,
    }) as any

    if (response.success && response.data) {
      // 保存保洁员独立会话，不覆盖PMS会话
      if (!response.data.token) {
        ElMessage.error(t('stage5.cleaner.auth.missingToken'))
        return
      }
      const cleanerData = response.data.cleaner
      if (!cleanerData?.userId || !cleanerData?.id) {
        ElMessage.error(t('stage5.cleaner.auth.incompleteIdentity'))
        return
      }
      const userInfo: CleanerSessionUser = {
        userId: cleanerData.userId,
        cleanerId: cleanerData.id,
        email: cleanerData.email,
        nickname: cleanerData.name,
        avatar: undefined,
        gender: 'private' as const,
        createdAt: cleanerData.createdAt,
        updatedAt: cleanerData.updatedAt,
        isCleaner: true,
      }
      saveCleanerSession(response.data.token, userInfo, cleanerData.storeId)

      ElMessage.success(t('stage5.cleaner.auth.success'))

      // 跳转到保洁员工作台
      router.push('/cleaner/dashboard')
    } else {
      ElMessage.error(response.message || t('stage5.cleaner.auth.failed'))
    }
  } catch (error) {
    console.error('Cleaner login failed:', error)
    ElMessage.error(t('stage5.cleaner.auth.failedWithHint'))
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.cleaner-login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}

.login-card {
  width: 100%;
  max-width: 440px;
  background: white;
  border-radius: 16px;
  padding: 48px 40px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
}

.header {
  text-align: center;
  margin-bottom: 40px;
}

.logo {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  font-weight: 700;
  color: #667eea;
  margin-bottom: 16px;
}

.logo-icon {
  width: 40px;
  height: 40px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
  margin-right: 12px;
  font-size: 20px;
}

.title {
  font-size: 28px;
  font-weight: 700;
  color: #1a1a1a;
  margin: 0 0 8px 0;
}

.subtitle {
  font-size: 14px;
  color: #666;
  margin: 0;
}

.login-form {
  margin-bottom: 24px;
}

.login-form :deep(.el-form-item) {
  margin-bottom: 20px;
}

.login-form :deep(.el-input__wrapper) {
  border-radius: 8px;
  padding: 12px 16px;
}

.login-button {
  width: 100%;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 600;
  height: 48px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
}

.login-button:hover {
  opacity: 0.9;
}

.footer {
  text-align: center;
  padding-top: 24px;
  border-top: 1px solid #eee;
}

.footer-text {
  font-size: 13px;
  color: #666;
  margin: 0 0 16px 0;
  line-height: 1.6;
}

.link {
  color: #667eea;
  text-decoration: none;
  font-weight: 500;
}

.link:hover {
  text-decoration: underline;
}

.footer-logo {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #999;
  font-weight: 500;
}

.footer-logo :deep(.el-icon) {
  font-size: 16px;
}
</style>
