<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { UserFilled } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import type { UserDTO } from '@/api/auth'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const saving = ref(false)
const passwordDialogVisible = ref(false)
const passwordLoading = ref(false)

const profileFormRef = ref<FormInstance>()
const passwordFormRef = ref<FormInstance>()

const formData = reactive({
  nickname: '',
  gender: 'private' as 'male' | 'female' | 'private',
  avatar: '',
})

const passwordForm = reactive({
  currentPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const userInfo = computed(() => userStore.currentUser)

const normalizeGender = (value?: string | null) => {
  return value === 'male' || value === 'female' ? value : 'private'
}

const syncForm = (user: UserDTO) => {
  formData.nickname = user.nickname || user.email?.split('@')[0] || ''
  formData.gender = normalizeGender(user.gender)
  formData.avatar = user.avatar || ''
  profileFormRef.value?.clearValidate()
}

watch(
  userInfo,
  (user) => {
    if (user) {
      syncForm(user)
    }
  },
  { immediate: true },
)

onMounted(async () => {
  if (!userInfo.value && localStorage.getItem('token')) {
    loading.value = true
    try {
      await userStore.fetchCurrentUser(true)
    } catch (error: unknown) {
      const message = error instanceof Error ? error.message : '加载用户信息失败'
      ElMessage.error(message)
    } finally {
      loading.value = false
    }
  }
})

const isDirty = computed(() => {
  const user = userInfo.value
  if (!user) return false
  const baseNickname = user.nickname || user.email?.split('@')[0] || ''
  const baseGender = normalizeGender(user.gender)
  const baseAvatar = user.avatar || ''
  return (
    formData.nickname.trim() !== baseNickname ||
    formData.gender !== baseGender ||
    formData.avatar.trim() !== baseAvatar
  )
})

const handleSave = async () => {
  if (!formData.nickname.trim()) {
    ElMessage.warning('昵称不能为空')
    return
  }

  saving.value = true
  try {
    await userStore.updateProfile({
      nickname: formData.nickname.trim(),
      gender: formData.gender,
      avatar: formData.avatar.trim() || undefined,
    })
    ElMessage.success('个人资料已更新')
    if (userStore.currentUser) {
      syncForm(userStore.currentUser)
    }
  } catch (error: unknown) {
    const message = error instanceof Error ? error.message : '更新个人资料失败'
    ElMessage.error(message)
  } finally {
    saving.value = false
  }
}

const handleChangePassword = () => {
  passwordDialogVisible.value = true
}

const resetPasswordForm = () => {
  passwordForm.currentPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
  passwordFormRef.value?.clearValidate()
}

const passwordRules: FormRules = {
  currentPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 64, message: '密码长度需在6到64个字符之间', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value !== passwordForm.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur',
    },
  ],
}

const submitChangePassword = async () => {
  if (!passwordFormRef.value) return
  try {
    await passwordFormRef.value.validate()
  } catch {
    return
  }

  passwordLoading.value = true
  try {
    await userStore.changePassword({
      currentPassword: passwordForm.currentPassword,
      newPassword: passwordForm.newPassword,
      confirmPassword: passwordForm.confirmPassword,
    })
    ElMessage.success('密码修改成功，请重新登录')
    passwordDialogVisible.value = false
    resetPasswordForm()
    await userStore.logout()
    router.replace('/login')
  } catch (error: unknown) {
    const message = error instanceof Error ? error.message : '修改密码失败'
    ElMessage.error(message)
  } finally {
    passwordLoading.value = false
  }
}
</script>

<template>
  <div class="profile-page">
    <div class="profile-content" v-loading="loading">
      <el-card class="profile-card" :body-style="{ padding: '28px 32px' }">
        <div class="profile-header">
          <el-avatar
            :size="96"
            :icon="UserFilled"
            :src="userInfo?.avatar || undefined"
            class="avatar"
          />
          <div class="profile-header__detail">
            <div class="profile-email">{{ userInfo?.email || '—' }}</div>
            <el-link type="primary" :underline="false" @click="handleChangePassword">修改密码</el-link>
          </div>
        </div>

        <el-form ref="profileFormRef" :model="formData" class="profile-form" label-width="80px">
          <el-form-item label="昵称">
            <el-input v-model="formData.nickname" placeholder="请输入昵称" />
          </el-form-item>
          <el-form-item label="性别">
            <el-radio-group v-model="formData.gender" class="gender-group">
              <el-radio-button label="male">男</el-radio-button>
              <el-radio-button label="female">女</el-radio-button>
              <el-radio-button label="private">保密</el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="头像地址">
            <el-input v-model="formData.avatar" placeholder="请输入头像 URL（可选）" />
          </el-form-item>
          <div class="form-actions">
            <el-button
              type="primary"
              class="save-btn"
              :loading="saving"
              :disabled="!isDirty"
              @click="handleSave"
            >
              保存
            </el-button>
          </div>
        </el-form>
      </el-card>

      <el-card class="security-card" :body-style="{ padding: '24px 32px' }">
        <h3 class="card-title">账户安全</h3>
        <p class="card-subtitle">定期更新密码可以提升账户安全，请确保新密码与旧密码不同。</p>
        <el-button type="primary" plain @click="handleChangePassword">修改密码</el-button>
      </el-card>
    </div>

    <el-dialog
      v-model="passwordDialogVisible"
      title="修改密码"
      width="420px"
      @close="resetPasswordForm"
    >
      <el-form
        ref="passwordFormRef"
        :model="passwordForm"
        :rules="passwordRules"
        label-width="100px"
      >
        <el-form-item label="当前密码" prop="currentPassword">
          <el-input
            v-model="passwordForm.currentPassword"
            type="password"
            show-password
            placeholder="请输入当前密码"
          />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input
            v-model="passwordForm.newPassword"
            type="password"
            show-password
            placeholder="请输入新密码"
          />
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input
            v-model="passwordForm.confirmPassword"
            type="password"
            show-password
            placeholder="请再次输入新密码"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="passwordDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="passwordLoading" @click="submitChangePassword">
          确认修改
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.profile-page {
  min-height: calc(100vh - 60px);
  background: #f5f5f5;
  padding: 32px 24px;
  display: flex;
  justify-content: center;
}

.profile-content {
  width: 100%;
  max-width: 880px;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.profile-card,
.security-card {
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
}

.profile-header {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 24px;
}

.avatar {
  background: #b3d4fc;
}

.profile-header__detail {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.profile-email {
  font-size: 16px;
  font-weight: 500;
  color: #333;
}

.profile-form {
  max-width: 480px;
}

.gender-group {
  display: flex;
  gap: 12px;
}

.form-actions {
  margin-top: 12px;
}

.save-btn {
  width: 160px;
}

.security-card {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.card-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
}

.card-subtitle {
  margin: 0;
  font-size: 14px;
  color: #666;
  line-height: 1.6;
}
</style>
