<template>
  <div class="register-container">
    <div class="register-card">
      <!-- Logo -->
      <div class="logo-section">
        <div class="logo-icon">◆</div>
        <h1 class="logo-text">{{ t('stage5.cleaner.common.appName') }}</h1>
      </div>

      <!-- Title -->
      <h2 class="register-title">{{ t('stage5.cleaner.auth.registerTitle') }}</h2>

      <!-- Form -->
      <el-form
        ref="formRef"
        :model="registerForm"
        :rules="rules"
        label-width="0"
        class="register-form"
      >
        <el-form-item prop="name">
          <el-input
            v-model="registerForm.name"
            :placeholder="t('stage5.cleaner.auth.namePlaceholder')"
            size="large"
            :prefix-icon="User"
            :disabled="loading || Boolean(invitationInfo)"
          />
        </el-form-item>

        <el-form-item prop="email">
          <el-input
            v-model="registerForm.email"
            :placeholder="t('stage5.cleaner.auth.emailPlaceholder')"
            size="large"
            :prefix-icon="Message"
            :disabled="loading || Boolean(invitationInfo)"
          />
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="registerForm.password"
            type="password"
            :placeholder="t('stage5.cleaner.auth.passwordPlaceholder')"
            size="large"
            :prefix-icon="Lock"
            show-password
            :disabled="loading"
          />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            @click="handleRegister"
            class="register-button"
          >
            {{ t('stage5.cleaner.auth.register') }}
          </el-button>
        </el-form-item>

        <div class="login-link">
          {{ t('stage5.cleaner.auth.alreadyHaveAccount') }}
          <router-link to="/cleaner/login">{{ t('stage5.cleaner.auth.login') }}</router-link>
        </div>

        <div class="terms-text">
          {{ t('stage5.cleaner.auth.termsClickPrefix') }}
          <a href="#">{{ t('stage5.cleaner.auth.terms') }}</a>
          {{ t('stage5.cleaner.auth.and') }}
          <a href="#">{{ t('stage5.cleaner.auth.privacy') }}</a>
        </div>
      </el-form>

      <!-- Footer Logo -->
      <div class="footer-logo">
        <svg viewBox="0 0 200 40" xmlns="http://www.w3.org/2000/svg">
          <text x="100" y="25" text-anchor="middle" fill="#999" font-size="16">{{ t('app.name') }}</text>
        </svg>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { User, Message, Lock } from '@element-plus/icons-vue'
import {
  validateInvitationToken,
  registerCleaner,
  type CleanerInvitationInfo
} from '@/api/cleaning'

const route = useRoute()
const router = useRouter()
const { t } = useI18n()

// 表单引用
const formRef = ref<FormInstance>()

// 邀请信息
const invitationInfo = ref<CleanerInvitationInfo | null>(null)

// 注册表单
const registerForm = reactive({
  name: '',
  email: '',
  password: ''
})

// 表单验证规则
const rules: FormRules = {
  name: [{ required: true, message: t('stage5.cleaner.auth.nameRequired'), trigger: 'blur' }],
  email: [
    { required: true, message: t('stage5.cleaner.auth.emailRequired'), trigger: 'blur' },
    { type: 'email', message: t('stage5.cleaner.auth.emailCorrect'), trigger: 'blur' }
  ],
  password: [
    { required: true, message: t('stage5.cleaner.auth.passwordRequired'), trigger: 'blur' },
    { min: 6, message: t('stage5.cleaner.auth.passwordMin'), trigger: 'blur' }
  ]
}

// 加载状态
const loading = ref(false)

// 验证邀请token
const verifyToken = async () => {
  const token = route.query.token as string

  if (!token) {
    ElMessage.error(t('stage5.cleaner.auth.invalidInvitation'))
    router.push('/cleaner/login')
    return
  }

  try {
    loading.value = true
    const response = await validateInvitationToken(token)

    if (response.success && response.data) {
      invitationInfo.value = response.data
      // 预填充邮箱和姓名
      registerForm.email = response.data.email
      registerForm.name = response.data.name
    } else {
      ElMessage.error(response.message || t('stage5.cleaner.auth.invitationVerifyFailed'))
      router.push('/cleaner/login')
    }
  } catch (error: any) {
    console.error('Cleaner invitation verification failed:', error)
    ElMessage.error(error.message || t('stage5.cleaner.auth.invitationVerifyFailed'))
    router.push('/cleaner/login')
  } finally {
    loading.value = false
  }
}

// 注册
const handleRegister = async () => {
  if (!formRef.value) return

  try {
    await formRef.value.validate()

    const token = route.query.token as string

    if (!token) {
      ElMessage.error(t('stage5.cleaner.auth.invalidInvitation'))
      return
    }

    loading.value = true

    const response = await registerCleaner({
      token,
      name: registerForm.name,
      email: registerForm.email,
      password: registerForm.password
    })

    if (response.success) {
      ElMessage.success(t('stage5.cleaner.auth.registerSuccess'))
      setTimeout(() => {
        router.push('/cleaner/login')
      }, 1500)
    } else {
      ElMessage.error(response.message || t('stage5.cleaner.auth.registerFailed'))
    }
  } catch (error: any) {
    if (error.message) {
      // 验证错误
      return
    }
    console.error('Cleaner registration failed:', error)
    ElMessage.error(error.message || t('stage5.cleaner.auth.registerFailed'))
  } finally {
    loading.value = false
  }
}

// 页面加载时验证token
onMounted(() => {
  verifyToken()
})
</script>

<style scoped>
.register-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}

.register-card {
  background: white;
  border-radius: 12px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.1);
  padding: 40px;
  width: 100%;
  max-width: 440px;
}

.logo-section {
  text-align: center;
  margin-bottom: 30px;
}

.logo-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 60px;
  height: 60px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 12px;
  color: white;
  font-size: 32px;
  font-weight: bold;
  margin-bottom: 12px;
}

.logo-text {
  font-size: 24px;
  font-weight: 600;
  color: #333;
  margin: 0;
}

.register-title {
  font-size: 20px;
  font-weight: 600;
  color: #333;
  text-align: center;
  margin: 0 0 30px 0;
}

.register-form {
  margin-top: 20px;
}

.register-button {
  width: 100%;
  font-size: 16px;
  font-weight: 600;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
}

.register-button:hover {
  opacity: 0.9;
}

.login-link {
  text-align: center;
  margin-top: 20px;
  font-size: 14px;
  color: #666;
}

.login-link a {
  color: #667eea;
  text-decoration: none;
  font-weight: 600;
}

.login-link a:hover {
  text-decoration: underline;
}

.terms-text {
  text-align: center;
  margin-top: 20px;
  font-size: 12px;
  color: #999;
  line-height: 1.6;
}

.terms-text a {
  color: #667eea;
  text-decoration: none;
}

.terms-text a:hover {
  text-decoration: underline;
}

.footer-logo {
  margin-top: 40px;
  text-align: center;
  opacity: 0.5;
}

.footer-logo svg {
  height: 30px;
}

@media (max-width: 768px) {
  .register-card {
    padding: 30px 20px;
  }

  .register-title {
    font-size: 18px;
  }
}
</style>
