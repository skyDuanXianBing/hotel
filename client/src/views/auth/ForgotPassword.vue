<template>
  <div class="forgot-password-container">
    <!-- 左侧蓝色背景区域 -->
    <div class="left-section">
      <div class="left-content">
        <h1 class="main-title">{{ t('auth.hero.titlePrimary') }}</h1>
        <h1 class="sub-title">{{ t('auth.hero.titleSecondary') }}</h1>

        <p class="description">{{ t('auth.hero.description') }}</p>

        <p class="trial-info">{{ t('auth.hero.trialInfo') }}</p>

        <!-- 装饰性浏览器窗口 -->
        <div class="browser-window">
          <div class="browser-header">
            <span class="dot red"></span>
            <span class="dot yellow"></span>
            <span class="dot green"></span>
          </div>
        </div>
      </div>
    </div>

    <!-- 右侧表单区域 -->
    <div class="right-section">
      <!-- 语言选择 -->
      <div class="language-selector">
        <LanguageSwitcher variant="auth" />
      </div>

      <div class="form-container">
        <!-- 标题 -->
        <h2 class="form-title">{{ t('pages.forgotPassword.title') }}</h2>
        <p class="form-subtitle">{{ t('pages.forgotPassword.subtitle') }}</p>

        <!-- 表单 -->
        <el-form
          ref="forgotFormRef"
          :model="forgotForm"
          :rules="forgotRules"
          class="forgot-form"
        >
          <!-- 邮箱 -->
          <div class="form-item">
            <label class="form-label">{{ t('common.email') }}</label>
            <el-form-item prop="email">
              <el-input
                v-model="forgotForm.email"
                :placeholder="t('auth.login.emailPlaceholder')"
                size="large"
                :prefix-icon="Message"
              />
            </el-form-item>
          </div>

          <!-- 验证码 -->
          <div class="form-item">
            <label class="form-label">{{ t('common.verificationCode') }}</label>
            <el-form-item prop="verificationCode">
              <el-input
                v-model="forgotForm.verificationCode"
                :placeholder="t('auth.login.codePlaceholder')"
                size="large"
                :prefix-icon="Key"
              >
                <template #append>
                  <el-button :disabled="countdown > 0" @click="sendVerificationCode">
                    {{ countdown > 0 ? `${countdown}s` : t('auth.actions.sendCode') }}
                  </el-button>
                </template>
              </el-input>
            </el-form-item>
          </div>

          <!-- 设置新密码 -->
          <div class="form-item">
            <label class="form-label">{{ t('pages.forgotPassword.newPasswordLabel') }}</label>
            <el-form-item prop="newPassword">
              <el-input
                v-model="forgotForm.newPassword"
                type="password"
                :placeholder="t('pages.forgotPassword.newPasswordPlaceholder')"
                size="large"
                :prefix-icon="Lock"
                show-password
              />
            </el-form-item>
          </div>

          <!-- 确认新密码 -->
          <div class="form-item">
            <label class="form-label">{{ t('pages.forgotPassword.confirmPasswordLabel') }}</label>
            <el-form-item prop="confirmPassword">
              <el-input
                v-model="forgotForm.confirmPassword"
                type="password"
                :placeholder="t('pages.forgotPassword.confirmPasswordPlaceholder')"
                size="large"
                :prefix-icon="Lock"
                show-password
              />
            </el-form-item>
          </div>

          <!-- 重置密码按钮 -->
          <el-button
            type="primary"
            size="large"
            class="submit-button"
            :loading="loading"
            @click="handleResetPassword"
          >
            {{ t('pages.forgotPassword.resetAction') }}
          </el-button>
        </el-form>

        <!-- 返回登录 -->
        <div class="back-link">
          {{ t('pages.forgotPassword.backToLoginPrefix') }}
          <el-link type="primary" :underline="false" @click="goToLogin">
            {{ t('pages.forgotPassword.backToLoginAction') }}
          </el-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Message, Lock, Key } from '@element-plus/icons-vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useI18n } from 'vue-i18n'
import {
  sendVerificationCode as sendVerificationCodeAPI,
  resetPassword as resetPasswordAPI,
} from '@/api/auth'
import LanguageSwitcher from '@/components/LanguageSwitcher.vue'

const router = useRouter()
const { t } = useI18n()

// 倒计时
const countdown = ref(0)

// 加载状态
const loading = ref(false)

// 表单引用
const forgotFormRef = ref<FormInstance>()

// 表单数据
const forgotForm = reactive({
  email: '',
  verificationCode: '',
  newPassword: '',
  confirmPassword: '',
})

// 自定义验证：确认密码
const validateConfirmPassword = (rule: any, value: any, callback: any) => {
  if (value === '') {
    callback(new Error(t('pages.forgotPassword.validation.confirmPasswordRequired')))
  } else if (value !== forgotForm.newPassword) {
    callback(new Error(t('pages.forgotPassword.validation.confirmPasswordMismatch')))
  } else {
    callback()
  }
}

// 表单验证规则
const forgotRules = computed<FormRules>(() => ({
  email: [
    { required: true, message: t('auth.login.validation.emailRequired'), trigger: 'blur' },
    { type: 'email', message: t('auth.login.validation.emailInvalid'), trigger: 'blur' },
  ],
  verificationCode: [
    { required: true, message: t('auth.login.validation.codeRequired'), trigger: 'blur' },
    { len: 6, message: t('auth.login.validation.codeLength'), trigger: 'blur' },
  ],
  newPassword: [
    {
      required: true,
      message: t('pages.forgotPassword.validation.newPasswordRequired'),
      trigger: 'blur',
    },
    {
      min: 6,
      max: 20,
      message: t('pages.forgotPassword.validation.newPasswordLength'),
      trigger: 'blur',
    },
  ],
  confirmPassword: [
    {
      required: true,
      message: t('pages.forgotPassword.validation.confirmPasswordRequired'),
      trigger: 'blur',
    },
    { validator: validateConfirmPassword, trigger: 'blur' },
  ],
}))

// 发送验证码
const sendVerificationCode = async () => {
  // 先验证邮箱
  try {
    await forgotFormRef.value?.validateField('email')
  } catch {
    return
  }

  try {
    // 调用发送验证码API
    await sendVerificationCodeAPI({
      email: forgotForm.email,
      type: 'reset_password',
    })

    ElMessage.success(t('auth.login.codeSent'))

    // 开始倒计时
    countdown.value = 60
    const timer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) {
        clearInterval(timer)
      }
    }, 1000)
  } catch (error: any) {
    const message = error.response?.data?.message || error.message || t('pages.forgotPassword.sendCodeFailed')
    ElMessage.error(message)
  }
}

// 重置密码处理
const handleResetPassword = async () => {
  try {
    await forgotFormRef.value?.validate()

    loading.value = true

    // 调用重置密码API
    await resetPasswordAPI({
      email: forgotForm.email,
      verificationCode: forgotForm.verificationCode,
      newPassword: forgotForm.newPassword,
    })

    ElMessage.success(t('pages.forgotPassword.resetSuccess'))
    router.push('/login')
  } catch (error: any) {
    const message = error.response?.data?.message || error.message || t('pages.forgotPassword.resetFailed')
    ElMessage.error(message)
  } finally {
    loading.value = false
  }
}

// 返回登录页面
const goToLogin = () => {
  router.push('/login')
}
</script>

<style scoped>
.forgot-password-container {
  display: flex;
  height: 100vh;
  overflow: hidden;
}

/* 左侧蓝色区域 */
.left-section {
  flex: 1;
  background: linear-gradient(135deg, #1e88e5 0%, #1976d2 100%);
  padding: 60px 80px;
  color: white;
  position: relative;
  overflow: hidden;
}

.left-section::before {
  content: '';
  position: absolute;
  width: 600px;
  height: 600px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 50%;
  top: -200px;
  right: -200px;
}

.left-section::after {
  content: '';
  position: absolute;
  width: 400px;
  height: 400px;
  background: rgba(255, 255, 255, 0.08);
  border-radius: 50%;
  bottom: -100px;
  left: -100px;
}

.left-content {
  position: relative;
  z-index: 1;
}

.main-title {
  font-size: 48px;
  font-weight: bold;
  margin: 0 0 10px 0;
  line-height: 1.2;
}

.sub-title {
  font-size: 48px;
  font-weight: bold;
  margin: 0 0 40px 0;
  line-height: 1.2;
}

.description {
  font-size: 16px;
  line-height: 1.8;
  margin-bottom: 30px;
  opacity: 0.95;
}

.trial-info {
  font-size: 16px;
  line-height: 1.8;
  margin-bottom: 60px;
  opacity: 0.95;
}

.browser-window {
  width: 600px;
  height: 200px;
  background: rgba(255, 255, 255, 0.15);
  border-radius: 12px;
  backdrop-filter: blur(10px);
  padding: 12px;
}

.browser-header {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}

.dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
}

.dot.red {
  background: #ff5f57;
}

.dot.yellow {
  background: #ffbd2e;
}

.dot.green {
  background: #28ca42;
}

/* 右侧表单区域 */
.right-section {
  width: 520px;
  background: white;
  padding: 40px 60px;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
}

.language-selector {
  text-align: right;
  margin-bottom: 40px;
}

.form-container {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.form-title {
  font-size: 32px;
  font-weight: bold;
  color: #333;
  margin: 0 0 12px 0;
}

.form-subtitle {
  font-size: 14px;
  color: #999;
  margin: 0 0 40px 0;
}

.forgot-form {
  flex: 1;
}

.form-item {
  margin-bottom: 24px;
}

.form-label {
  display: block;
  font-size: 14px;
  color: #333;
  margin-bottom: 8px;
  font-weight: 500;
}

.submit-button {
  width: 100%;
  height: 48px;
  font-size: 16px;
  font-weight: 500;
  margin-bottom: 24px;
}

.back-link {
  text-align: center;
  font-size: 14px;
  color: #666;
}

/* 表单样式调整 */
.forgot-form :deep(.el-form-item) {
  margin-bottom: 0;
}

.forgot-form :deep(.el-input__wrapper) {
  box-shadow: 0 0 0 1px #e0e0e0 inset;
}

.forgot-form :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #409eff inset;
}

.forgot-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #409eff inset;
}
</style>
