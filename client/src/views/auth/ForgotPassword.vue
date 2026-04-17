<template>
  <div class="forgot-password-container">
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
        <h2 class="form-title">忘记密码</h2>
        <p class="form-subtitle">请输入您的邮箱并设置新密码，我们将发送验证码进行验证</p>

        <!-- 表单 -->
        <el-form
          ref="forgotFormRef"
          :model="forgotForm"
          :rules="forgotRules"
          class="forgot-form"
        >
          <!-- 邮箱 -->
          <div class="form-item">
            <label class="form-label">邮箱</label>
            <el-form-item prop="email">
              <el-input
                v-model="forgotForm.email"
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
                v-model="forgotForm.verificationCode"
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

          <!-- 设置新密码 -->
          <div class="form-item">
            <label class="form-label">设置新密码</label>
            <el-form-item prop="newPassword">
              <el-input
                v-model="forgotForm.newPassword"
                type="password"
                placeholder="请设置新密码（6-20位字符）"
                size="large"
                :prefix-icon="Lock"
                show-password
              />
            </el-form-item>
          </div>

          <!-- 确认新密码 -->
          <div class="form-item">
            <label class="form-label">确认新密码</label>
            <el-form-item prop="confirmPassword">
              <el-input
                v-model="forgotForm.confirmPassword"
                type="password"
                placeholder="请再次输入新密码"
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
            重置密码
          </el-button>
        </el-form>

        <!-- 返回登录 -->
        <div class="back-link">
          想起密码了？
          <el-link type="primary" :underline="false" @click="goToLogin">返回登录</el-link>
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
  resetPassword as resetPasswordAPI,
} from '@/api/auth'

const router = useRouter()

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
    callback(new Error('请再次输入新密码'))
  } else if (value !== forgotForm.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

// 表单验证规则
const forgotRules: FormRules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' },
  ],
  verificationCode: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { len: 6, message: '验证码为6位数字', trigger: 'blur' },
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度为6-20位字符', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' },
  ],
}

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

    ElMessage.success('密码重置成功，请使用新密码登录')
    router.push('/login')
  } catch (error: any) {
    const message = error.response?.data?.message || error.message || '密码重置失败'
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
