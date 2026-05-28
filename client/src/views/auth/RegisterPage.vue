<template>
  <div class="register-container">
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
        <h2 class="form-title">{{ t('auth.register.title') }}</h2>
        <p class="form-subtitle">{{ t('auth.register.subtitle') }}</p>

        <!-- 注册表单 -->
        <el-form
          ref="registerFormRef"
          :model="registerForm"
          :rules="registerRules"
          class="register-form"
        >
          <!-- 邮箱 -->
          <div class="form-item">
            <label class="form-label">{{ t('common.email') }}</label>
            <el-form-item prop="email">
              <el-input
                v-model="registerForm.email"
                :placeholder="t('auth.register.emailPlaceholder')"
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
                v-model="registerForm.verificationCode"
                :placeholder="t('auth.register.codePlaceholder')"
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

          <!-- 设置密码 -->
          <div class="form-item">
            <label class="form-label">{{ t('auth.register.passwordLabel') }}</label>
            <el-form-item prop="password">
              <el-input
                v-model="registerForm.password"
                type="password"
                :placeholder="t('auth.register.passwordPlaceholder')"
                size="large"
                :prefix-icon="Lock"
                show-password
              />
            </el-form-item>
          </div>

          <!-- 确认密码 -->
          <div class="form-item">
            <label class="form-label">{{ t('auth.register.confirmPasswordLabel') }}</label>
            <el-form-item prop="confirmPassword">
              <el-input
                v-model="registerForm.confirmPassword"
                type="password"
                :placeholder="t('auth.register.confirmPasswordPlaceholder')"
                size="large"
                :prefix-icon="Lock"
                show-password
              />
            </el-form-item>
          </div>

          <!-- 同意协议 -->
          <div class="agreement-row">
            <el-checkbox v-model="registerForm.agreeToTerms">
              {{ t('auth.register.agreementPrefix') }}
              <el-link
                type="primary"
                :underline="false"
                @click.stop.prevent="goToTermsOfService"
              >
                {{ t('common.termsOfService') }}
              </el-link>
              {{ t('auth.register.agreementComma') }}
              <el-link type="primary" :underline="false" @click.stop.prevent="goToPrivacyPolicy">
                {{ t('common.privacyPolicy') }}
              </el-link>
              {{ t('auth.register.agreementJoiner') }}
              <el-link type="primary" :underline="false">{{ t('common.memberTerms') }}</el-link>
            </el-checkbox>
          </div>

          <!-- 注册按钮 -->
          <el-button
            type="primary"
            size="large"
            class="register-button"
            :loading="loading"
            @click="handleRegister"
          >
            {{ t('auth.register.submit') }}
          </el-button>
        </el-form>

        <!-- 登录链接 -->
        <div class="login-link">
          {{ t('auth.register.loginPrefix') }}
          <el-link type="primary" :underline="false" @click="goToLogin">{{ t('auth.register.loginAction') }}</el-link>
        </div>
        <div class="support-link-row">
          <el-link type="primary" :underline="false" @click="goToTechnicalSupport">
            {{ t('common.supportSite') }}
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
import LanguageSwitcher from '@/components/LanguageSwitcher.vue'
import {
  sendVerificationCode as sendVerificationCodeAPI,
  register as registerAPI,
} from '@/api/auth'

const router = useRouter()
const { t } = useI18n()

// 倒计时
const countdown = ref(0)

// 加载状态
const loading = ref(false)

// 表单引用
const registerFormRef = ref<FormInstance>()

// 表单数据
const registerForm = reactive({
  email: '',
  verificationCode: '',
  password: '',
  confirmPassword: '',
  agreeToTerms: false,
})

// 自定义验证：确认密码
const validateConfirmPassword = (rule: any, value: any, callback: any) => {
  if (value === '') {
    callback(new Error(t('auth.register.validation.confirmPasswordRequired')))
  } else if (value !== registerForm.password) {
    callback(new Error(t('auth.register.validation.confirmPasswordMismatch')))
  } else {
    callback()
  }
}

// 表单验证规则
const registerRules = computed<FormRules>(() => ({
  email: [
    { required: true, message: t('auth.register.validation.emailRequired'), trigger: 'blur' },
    { type: 'email', message: t('auth.register.validation.emailInvalid'), trigger: 'blur' },
  ],
  verificationCode: [
    { required: true, message: t('auth.register.validation.codeRequired'), trigger: 'blur' },
    { len: 6, message: t('auth.register.validation.codeLength'), trigger: 'blur' },
  ],
  password: [
    { required: true, message: t('auth.register.validation.passwordRequired'), trigger: 'blur' },
    { min: 6, max: 20, message: t('auth.register.validation.passwordLength'), trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: t('auth.register.validation.confirmPasswordRequired'), trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' },
  ],
}))

// 发送验证码
const sendVerificationCode = async () => {
  // 先验证邮箱
  try {
    await registerFormRef.value?.validateField('email')
  } catch {
    return
  }

  try {
    // 调用发送验证码API
    await sendVerificationCodeAPI({
      email: registerForm.email,
      type: 'register',
    })

    ElMessage.success(t('auth.register.codeSent'))

    // 开始倒计时
    countdown.value = 60
    const timer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) {
        clearInterval(timer)
      }
    }, 1000)
  } catch (error: any) {
    ElMessage.error(error.message || t('auth.register.failed'))
  }
}

// 注册处理
const handleRegister = async () => {
  if (!registerForm.agreeToTerms) {
    ElMessage.warning(t('auth.register.agreementRequired'))
    return
  }

  try {
    await registerFormRef.value?.validate()

    loading.value = true

    // 调用注册API
    await registerAPI({
      email: registerForm.email,
      verificationCode: registerForm.verificationCode,
      password: registerForm.password,
    })

    ElMessage.success(t('auth.register.success'))
    router.push('/login')
    loading.value = false
  } catch (error: any) {
    loading.value = false
    const message = error.response?.data?.message || error.message || t('auth.register.failed')
    ElMessage.error(message)
  }
}

// 跳转到登录页面
const goToLogin = () => {
  router.push('/login')
}

// 跳转到用户服务协议
const goToTermsOfService = () => {
  router.push('/legal/terms')
}

// 跳转到隐私政策
const goToPrivacyPolicy = () => {
  router.push('/legal/privacy')
}
// 跳转到技术支持网站
const goToTechnicalSupport = () => {
  router.push('/legal/support')
}
</script>

<style scoped>
.register-container {
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
  white-space: pre-line;
}

.trial-info {
  font-size: 16px;
  line-height: 1.8;
  margin-bottom: 60px;
  opacity: 0.95;
  white-space: pre-line;
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

.language-text {
  cursor: pointer;
  color: #666;
  font-size: 14px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
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

.register-form {
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

.agreement-row {
  margin-bottom: 24px;
  font-size: 14px;
  color: #666;
}

.register-button {
  width: 100%;
  height: 48px;
  font-size: 16px;
  font-weight: 500;
  margin-bottom: 24px;
}

.login-link {
  text-align: center;
  font-size: 14px;
  color: #666;
}

.support-link-row {
  margin-top: 12px;
  text-align: center;
}

/* 表单样式调整 */
.register-form :deep(.el-form-item) {
  margin-bottom: 0;
}

.register-form :deep(.el-input__wrapper) {
  box-shadow: 0 0 0 1px #e0e0e0 inset;
}

.register-form :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #409eff inset;
}

.register-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #409eff inset;
}

.register-form :deep(.el-checkbox__label) {
  font-size: 14px;
  color: #666;
}
</style>
