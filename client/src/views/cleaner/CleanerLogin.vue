<template>
  <div class="cleaner-login-container">
    <div class="login-card">
      <!-- Logo和标题 -->
      <div class="header">
        <div class="logo">
          <span class="logo-icon">◆</span>
           房东智控中心（THE HOST HUB）
        </div>
        <h1 class="title">保洁平台</h1>
        <p class="subtitle">登录您的账户以继续操作</p>
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
            placeholder="邮箱"
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
            placeholder="密码"
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
            登录
          </el-button>
        </el-form-item>
      </el-form>

      <!-- 底部链接 -->
      <div class="footer">
        <p class="footer-text">
          您确认或接受
          <a href="#" class="link">服务条款</a>和
          <a href="#" class="link">隐私政策</a>
        </p>
        <div class="footer-logo">
          <el-icon><Pointer /></el-icon>
         房东智控中心（THE HOST HUB）
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { User, Lock, Pointer } from '@element-plus/icons-vue'
import { cleanerLoginByPassword } from '@/api/cleanerAuth'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const formRef = ref<FormInstance>()
const loading = ref(false)

const loginForm = reactive({
  email: '',
  password: '',
})

const rules: FormRules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入有效的邮箱地址', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度至少6位', trigger: 'blur' },
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
      // 保存token到localStorage
      if (response.data.token) {
        localStorage.setItem('token', response.data.token)
      }

      // 保存保洁员信息,转换为用户格式
      const cleanerData = response.data.cleaner
      const userInfo = {
        id: cleanerData.id,
        email: cleanerData.email,
        nickname: cleanerData.name,
        avatar: undefined,
        gender: 'private' as const,
        createdAt: cleanerData.createdAt,
        updatedAt: cleanerData.updatedAt,
        isCleaner: true, // 标记为保洁员
      }
      localStorage.setItem('user', JSON.stringify(userInfo))
      userStore.setUser(userInfo)
      if (cleanerData.storeId) {
        localStorage.setItem('currentStore', JSON.stringify({ id: cleanerData.storeId }))
      }

      ElMessage.success('登录成功')

      // 跳转到保洁员工作台
      router.push('/cleaner/dashboard')
    } else {
      ElMessage.error(response.message || '登录失败')
    }
  } catch (error) {
    console.error('登录失败:', error)
    ElMessage.error('登录失败,请检查您的账号和密码')
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
