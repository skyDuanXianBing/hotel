<template>
  <div class="login-container">
    <!-- 左侧蓝色背景区域 -->
    <div class="left-section">
      <div class="left-content">
        <h1 class="main-title">房东智控中心</h1>
        <h1 class="sub-title">THE HOST HUB</h1>

        <p class="description">
          Trusted by over 100,000 properties worldwide.<br />
          Streamline your operations with our powerful order<br />
          management system.
        </p>

        <p class="trial-info">
          Register now to get a 30-day free trial with full<br />
          access to all features. No credit card required.
        </p>

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
        <el-dropdown>
          <span class="language-text">
            简体中文 <el-icon class="el-icon--right"><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item>简体中文</el-dropdown-item>
              <el-dropdown-item>English</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>

      <div class="form-container">
        <!-- 标题 -->
        <h2 class="form-title">{{ loginMode === 'password' ? '登录' : '验证码登录' }}</h2>
        <p class="form-subtitle">
          {{ loginMode === 'password' ? '欢迎回来，请登录您的账户' : '使用邮箱验证码快速登录您的账户' }}
        </p>

        <!-- 登录表单 -->
        <el-form ref="loginFormRef" :model="loginForm" :rules="loginRules" class="login-form">
          <!-- 邮箱 -->
          <div class="form-item">
            <label class="form-label">邮箱</label>
            <el-form-item prop="email">
              <el-input
                v-model="loginForm.email"
                placeholder="请输入您的邮箱"
                size="large"
                :prefix-icon="Message"
              />
            </el-form-item>
          </div>

          <!-- 密码登录模式 -->
          <template v-if="loginMode === 'password'">
            <div class="form-item">
              <div class="label-row">
                <label class="form-label">密码</label>
                <el-link
                  type="primary"
                  :underline="false"
                  @click="goToForgotPassword"
                  class="forgot-link"
                >
                  忘记密码?
                </el-link>
              </div>
              <el-form-item prop="password">
                <el-input
                  v-model="loginForm.password"
                  type="password"
                  placeholder="请输入您的密码"
                  size="large"
                  :prefix-icon="Lock"
                  show-password
                />
              </el-form-item>
            </div>

            <!-- 记住密码 -->
            <div class="options-row">
              <el-checkbox v-model="loginForm.rememberMe">记住密码</el-checkbox>
              <el-link
                type="primary"
                :underline="false"
                @click="switchLoginMode"
                class="switch-mode-link"
              >
                验证码登录
              </el-link>
            </div>
          </template>

          <!-- 验证码登录模式 -->
          <template v-else>
            <div class="form-item">
              <label class="form-label">验证码</label>
              <el-form-item prop="verificationCode">
                <el-input
                  v-model="loginForm.verificationCode"
                  placeholder="请输入验证码"
                  size="large"
                  :prefix-icon="Key"
                >
                  <template #append>
                    <el-button :disabled="countdown > 0" @click="sendVerificationCode">
                      {{ countdown > 0 ? `${countdown}s` : '获取验证码' }}
                    </el-button>
                  </template>
                </el-input>
              </el-form-item>
            </div>

            <!-- 记住登录状态 -->
            <div class="options-row">
              <el-checkbox v-model="loginForm.rememberMe">记住登录状态</el-checkbox>
              <el-link
                type="primary"
                :underline="false"
                @click="switchLoginMode"
                class="switch-mode-link"
              >
                密码登录
              </el-link>
            </div>
          </template>

          <!-- 同意协议 -->
          <div class="agreement-row">
            <el-checkbox v-model="loginForm.agreeToTerms">
              我已阅读并同意
              <el-link
                type="primary"
                :underline="false"
                @click.stop.prevent="goToTermsOfService"
              >
                《用户服务协议》
              </el-link>
              和
              <el-link type="primary" :underline="false" @click.stop.prevent="goToPrivacyPolicy">
                《隐私政策》
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
            登录
          </el-button>
        </el-form>

        <!-- 注册链接 -->
        <div class="register-link">
          还没有账户？
          <el-link type="primary" :underline="false" @click="goToRegister">立即注册</el-link>
        </div>
        <div class="support-link-row">
          <el-link type="primary" :underline="false" @click="goToTechnicalSupport">
            技术支持网站
          </el-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Message, Lock, Key, ArrowDown } from '@element-plus/icons-vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
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
const loginRules: FormRules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度至少为6位', trigger: 'blur' },
  ],
  verificationCode: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { len: 6, message: '验证码为6位数字', trigger: 'blur' },
  ],
}

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

    ElMessage.success('验证码已发送，请查收邮箱')

    // 开始倒计时
    countdown.value = 60
    const timer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) {
        clearInterval(timer)
      }
    }, 1000)
  } catch (error: any) {
    const message = error.response?.data?.message || error.message || '验证码发送失败'
    ElMessage.error(message)
  }
}

// 登录处理
const handleLogin = async () => {
  if (!loginForm.agreeToTerms) {
    ElMessage.warning('请先阅读并同意用户服务协议和隐私政策')
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
      ElMessage.error(response.message || '登录失败')
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

    ElMessage.success('登录成功')

    // 登录成功后始终跳转到门店选择页面,让用户自己选择门店
    router.push('/store/selection')

    loading.value = false
  } catch (error: any) {
    loading.value = false
    const message = error.response?.data?.message || error.message || '登录失败'
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
