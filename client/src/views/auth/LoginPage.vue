<template>
  <div class="login-container">
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

    <!-- 右侧登录表单区域 -->
    <div class="right-section">
      <!-- 语言选择 -->
      <div class="language-selector">
        <LanguageSwitcher variant="auth" />
      </div>

      <div class="form-container">
        <!-- 标题 -->
        <h2 class="form-title">
          {{ loginMode === 'password' ? t('auth.login.title.password') : t('auth.login.title.code') }}
        </h2>
        <p class="form-subtitle">
          {{
            loginMode === 'password'
              ? t('auth.login.subtitle.password')
              : t('auth.login.subtitle.code')
          }}
        </p>

        <!-- 登录表单 -->
        <el-form ref="loginFormRef" :model="loginForm" :rules="loginRules" class="login-form">
          <!-- 邮箱 -->
          <div class="form-item">
            <label class="form-label">{{ t('common.email') }}</label>
            <el-form-item prop="email">
              <el-input
                v-model="loginForm.email"
                :placeholder="t('auth.login.emailPlaceholder')"
                size="large"
                :prefix-icon="Message"
              />
            </el-form-item>
          </div>

          <!-- 密码登录模式 -->
          <template v-if="loginMode === 'password'">
            <div class="form-item">
              <div class="label-row">
                <label class="form-label">{{ t('common.password') }}</label>
                <el-link
                  type="primary"
                  :underline="false"
                  @click="goToForgotPassword"
                  class="forgot-link"
                >
                  {{ t('auth.login.forgotPassword') }}
                </el-link>
              </div>
              <el-form-item prop="password">
                <el-input
                  v-model="loginForm.password"
                  type="password"
                  :placeholder="t('auth.login.passwordPlaceholder')"
                  size="large"
                  :prefix-icon="Lock"
                  show-password
                />
              </el-form-item>
            </div>

            <!-- 记住密码 -->
            <div class="options-row">
              <el-checkbox v-model="loginForm.rememberMe">{{ t('auth.login.rememberPassword') }}</el-checkbox>
              <el-link
                type="primary"
                :underline="false"
                @click="switchLoginMode"
                class="switch-mode-link"
              >
                {{ t('auth.login.switchToCode') }}
              </el-link>
            </div>
          </template>

          <!-- 验证码登录模式 -->
          <template v-else>
            <div class="form-item">
              <label class="form-label">{{ t('common.verificationCode') }}</label>
              <el-form-item prop="verificationCode">
                <el-input
                  v-model="loginForm.verificationCode"
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

            <!-- 记住登录状态 -->
            <div class="options-row">
              <el-checkbox v-model="loginForm.rememberMe">{{ t('auth.login.rememberSession') }}</el-checkbox>
              <el-link
                type="primary"
                :underline="false"
                @click="switchLoginMode"
                class="switch-mode-link"
              >
                {{ t('auth.login.switchToPassword') }}
              </el-link>
            </div>
          </template>

          <!-- 同意协议 -->
          <div class="agreement-row">
            <el-checkbox v-model="loginForm.agreeToTerms">
              {{ t('auth.login.agreementPrefix') }}
              <el-link
                type="primary"
                :underline="false"
                @click.stop.prevent="goToTermsOfService"
              >
                {{ t('common.termsOfService') }}
              </el-link>
              {{ t('auth.login.agreementJoiner') }}
              <el-link type="primary" :underline="false" @click.stop.prevent="goToPrivacyPolicy">
                {{ t('common.privacyPolicy') }}
              </el-link>
            </el-checkbox>
          </div>

          <!-- 登录按钮 -->
          <el-button
            type="primary"
            size="large"
            class="login-button"
            :loading="loading"
            @click="handleLogin"
          >
            {{ t('auth.login.submit') }}
          </el-button>
        </el-form>

        <!-- 注册链接 -->
        <div class="register-link">
          {{ t('auth.login.registerPrefix') }}
          <el-link type="primary" :underline="false" @click="goToRegister">{{ t('auth.login.registerAction') }}</el-link>
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
import { useUserStore } from '@/stores/user'
import { useStoreStore } from '@/stores/store'
import {
  loginByPassword as loginByPasswordAPI,
  loginByCode as loginByCodeAPI,
  sendVerificationCode as sendVerificationCodeAPI,
} from '@/api/auth'

const router = useRouter()
const userStore = useUserStore()
const storeStore = useStoreStore()
const { t } = useI18n()

// 登录模式：password（密码登录）或 code（验证码登录）
const loginMode = ref<'password' | 'code'>('password')

// 倒计时
const countdown = ref(0)

// 加载状态
const loading = ref(false)

// 表单引用
const loginFormRef = ref<FormInstance>()

// 登录表单数据
const loginForm = reactive({
  email: '',
  password: '',
  verificationCode: '',
  rememberMe: false,
  agreeToTerms: false,
})

// 表单验证规则
const loginRules = computed<FormRules>(() => ({
  email: [
    { required: true, message: t('auth.login.validation.emailRequired'), trigger: 'blur' },
    { type: 'email', message: t('auth.login.validation.emailInvalid'), trigger: 'blur' },
  ],
  password: [
    { required: true, message: t('auth.login.validation.passwordRequired'), trigger: 'blur' },
    { min: 6, message: t('auth.login.validation.passwordMin'), trigger: 'blur' },
  ],
  verificationCode: [
    { required: true, message: t('auth.login.validation.codeRequired'), trigger: 'blur' },
    { len: 6, message: t('auth.login.validation.codeLength'), trigger: 'blur' },
  ],
}))

// 切换登录模式
const switchLoginMode = () => {
  loginMode.value = loginMode.value === 'password' ? 'code' : 'password'
  // 清空表单验证
  loginFormRef.value?.clearValidate()
}

// 发送验证码
const sendVerificationCode = async () => {
  // 先验证邮箱
  try {
    await loginFormRef.value?.validateField('email')
  } catch {
    return
  }

  try {
    // 调用发送验证码API
    await sendVerificationCodeAPI({
      email: loginForm.email,
      type: 'login',
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
    const message = error.response?.data?.message || error.message || t('auth.login.failed')
    ElMessage.error(message)
  }
}

// 登录处理
const handleLogin = async () => {
  if (!loginForm.agreeToTerms) {
    ElMessage.warning(t('auth.login.agreementRequired'))
    return
  }

  try {
    await loginFormRef.value?.validate()

    loading.value = true

    // 调用登录API
    let response: any
    if (loginMode.value === 'password') {
      response = await loginByPasswordAPI({
        email: loginForm.email,
        password: loginForm.password,
        rememberMe: loginForm.rememberMe,
      })
    } else {
      response = await loginByCodeAPI({
        email: loginForm.email,
        verificationCode: loginForm.verificationCode,
        rememberMe: loginForm.rememberMe,
      })
    }

    // 检查响应是否成功
    if (!response.success || !response.data) {
      ElMessage.error(response.message || t('auth.login.failed'))
      loading.value = false
      return
    }

    // 保存token和用户信息
    const { token, user, stores } = response.data
    localStorage.setItem('token', token)
    localStorage.setItem('user', JSON.stringify(user))
    userStore.setUser(user)

    // 保存门店列表
    if (stores && stores.length > 0) {
      storeStore.setStores(stores)
    }

    ElMessage.success(t('auth.login.success'))

    // 登录成功后始终跳转到门店选择页面,让用户自己选择门店
    router.push('/store/selection')

    loading.value = false
  } catch (error: any) {
    loading.value = false
    const message = error.response?.data?.message || error.message || t('auth.login.failed')
    ElMessage.error(message)
  }
}

// 跳转到忘记密码页面
const goToForgotPassword = () => {
  router.push('/forgot-password')
}

// 跳转到注册页面
const goToRegister = () => {
  router.push('/register')
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
.login-container {
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

.login-form {
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

.label-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.forgot-link {
  font-size: 14px;
}

.options-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.switch-mode-link {
  font-size: 14px;
}

.agreement-row {
  margin-bottom: 24px;
  font-size: 14px;
  color: #666;
}

.login-button {
  width: 100%;
  height: 48px;
  font-size: 16px;
  font-weight: 500;
  margin-bottom: 24px;
}

.register-link {
  text-align: center;
  font-size: 14px;
  color: #666;
}

.support-link-row {
  margin-top: 12px;
  text-align: center;
}

/* 表单样式调整 */
.login-form :deep(.el-form-item) {
  margin-bottom: 0;
}

.login-form :deep(.el-input__wrapper) {
  box-shadow: 0 0 0 1px #e0e0e0 inset;
}

.login-form :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #409eff inset;
}

.login-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #409eff inset;
}

.login-form :deep(.el-checkbox__label) {
  font-size: 14px;
  color: #666;
}
</style>
