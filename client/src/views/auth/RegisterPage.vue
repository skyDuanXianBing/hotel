<template>
  <div class="register-container">
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

    <!-- 右侧表单区域 -->
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
        <h2 class="form-title">立即注册</h2>
        <p class="form-subtitle">创建账户，并开始使用房东智控中心（THE HOST HUB）</p>

        <!-- 注册表单 -->
        <el-form
          ref="registerFormRef"
          :model="registerForm"
          :rules="registerRules"
          class="register-form"
        >
          <!-- 邮箱 -->
          <div class="form-item">
            <label class="form-label">邮箱</label>
            <el-form-item prop="email">
              <el-input
                v-model="registerForm.email"
                placeholder="请输入您的邮箱"
                size="large"
                :prefix-icon="Message"
              />
            </el-form-item>
          </div>

          <!-- 验证码 -->
          <div class="form-item">
            <label class="form-label">验证码</label>
            <el-form-item prop="verificationCode">
              <el-input
                v-model="registerForm.verificationCode"
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

          <!-- 设置密码 -->
          <div class="form-item">
            <label class="form-label">设置密码</label>
            <el-form-item prop="password">
              <el-input
                v-model="registerForm.password"
                type="password"
                placeholder="请设置密码（6-20位字符）"
                size="large"
                :prefix-icon="Lock"
                show-password
              />
            </el-form-item>
          </div>

          <!-- 确认密码 -->
          <div class="form-item">
            <label class="form-label">确认密码</label>
            <el-form-item prop="confirmPassword">
              <el-input
                v-model="registerForm.confirmPassword"
                type="password"
                placeholder="请再次输入密码"
                size="large"
                :prefix-icon="Lock"
                show-password
              />
            </el-form-item>
          </div>

          <!-- 同意协议 -->
          <div class="agreement-row">
            <el-checkbox v-model="registerForm.agreeToTerms">
              我已阅读并同意
              <el-link
                type="primary"
                :underline="false"
                @click.stop.prevent="goToTermsOfService"
              >
                《用户服务协议》
              </el-link>
              、
              <el-link type="primary" :underline="false" @click.stop.prevent="goToPrivacyPolicy">
                《隐私政策》
              </el-link>
              和
              <el-link type="primary" :underline="false">《会员服务条款》</el-link>
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
            立即注册
          </el-button>
        </el-form>

        <!-- 登录链接 -->
        <div class="login-link">
          已有账户？
          <el-link type="primary" :underline="false" @click="goToLogin">立即登录</el-link>
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
import {
  sendVerificationCode as sendVerificationCodeAPI,
  register as registerAPI,
} from '@/api/auth'

const router = useRouter()

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
    callback(new Error('请再次输入密码'))
  } else if (value !== registerForm.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

// 表单验证规则
const registerRules: FormRules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' },
  ],
  verificationCode: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { len: 6, message: '验证码为6位数字', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度为6-20位字符', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' },
  ],
}

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

    ElMessage.success('验证码已发送,请查收邮箱')

    // 开始倒计时
    countdown.value = 60
    const timer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) {
        clearInterval(timer)
      }
    }, 1000)
  } catch (error: any) {
    ElMessage.error(error.message || '验证码发送失败')
  }
}

// 注册处理
const handleRegister = async () => {
  if (!registerForm.agreeToTerms) {
    ElMessage.warning('请先阅读并同意用户服务协议、隐私政策和会员服务条款')
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

    ElMessage.success('注册成功，请登录')
    router.push('/login')
    loading.value = false
  } catch (error: any) {
    loading.value = false
    const message = error.response?.data?.message || error.message || '注册失败'
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
