<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { UserFilled } from '@element-plus/icons-vue'
import { useI18n } from 'vue-i18n'
import { useUserStore } from '@/stores/user'
import type { UserDTO } from '@/api/auth'

const router = useRouter()
const userStore = useUserStore()
const { t } = useI18n()

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
      const message =
        error instanceof Error ? error.message : t('stage6.common.messages.fetchUserInfoFailed')
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
    ElMessage.warning(t('pages.profile.nicknameRequired'))
    return
  }

  saving.value = true
  try {
    await userStore.updateProfile({
      nickname: formData.nickname.trim(),
      gender: formData.gender,
      avatar: formData.avatar.trim() || undefined,
    })
    ElMessage.success(t('pages.profile.saveSuccess'))
    if (userStore.currentUser) {
      syncForm(userStore.currentUser)
    }
  } catch (error: unknown) {
    const message =
      error instanceof Error ? error.message : t('stage6.common.messages.updateProfileFailed')
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

const passwordRules = computed<FormRules>(() => ({
  currentPassword: [
    {
      required: true,
      message: t('pages.profile.validation.currentPasswordRequired'),
      trigger: 'blur',
    },
  ],
  newPassword: [
    {
      required: true,
      message: t('pages.profile.validation.newPasswordRequired'),
      trigger: 'blur',
    },
    {
      min: 6,
      max: 64,
      message: t('pages.profile.validation.newPasswordLength'),
      trigger: 'blur',
    },
  ],
  confirmPassword: [
    {
      required: true,
      message: t('pages.profile.validation.confirmPasswordRequired'),
      trigger: 'blur',
    },
    {
      validator: (_rule, value, callback) => {
        if (value !== passwordForm.newPassword) {
          callback(new Error(t('pages.profile.validation.confirmPasswordMismatch')))
        } else {
          callback()
        }
      },
      trigger: 'blur',
    },
  ],
}))

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
    ElMessage.success(t('pages.profile.changePasswordSuccess'))
    passwordDialogVisible.value = false
    resetPasswordForm()
    await userStore.logout()
    router.replace('/login')
  } catch (error: unknown) {
    const message =
      error instanceof Error ? error.message : t('stage6.common.messages.changePasswordFailed')
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
            <div class="profile-email">{{ userInfo?.email || t('pages.profile.emailFallback') }}</div>
            <el-link type="primary" :underline="false" @click="handleChangePassword">
              {{ t('pages.profile.changePassword') }}
            </el-link>
          </div>
        </div>

        <el-form ref="profileFormRef" :model="formData" class="profile-form" label-width="80px">
          <el-form-item :label="t('pages.profile.nickname')">
            <el-input v-model="formData.nickname" :placeholder="t('pages.profile.nicknamePlaceholder')" />
          </el-form-item>
          <el-form-item :label="t('pages.profile.gender')">
            <el-radio-group v-model="formData.gender" class="gender-group">
              <el-radio-button label="male">{{ t('pages.profile.genderOptions.male') }}</el-radio-button>
              <el-radio-button label="female">{{ t('pages.profile.genderOptions.female') }}</el-radio-button>
              <el-radio-button label="private">{{ t('pages.profile.genderOptions.private') }}</el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item :label="t('pages.profile.avatar')">
            <el-input v-model="formData.avatar" :placeholder="t('pages.profile.avatarPlaceholder')" />
          </el-form-item>
          <div class="form-actions">
            <el-button
              type="primary"
              class="save-btn"
              :loading="saving"
              :disabled="!isDirty"
              @click="handleSave"
            >
              {{ t('pages.profile.save') }}
            </el-button>
          </div>
        </el-form>
      </el-card>

      <el-card class="security-card" :body-style="{ padding: '24px 32px' }">
        <h3 class="card-title">{{ t('pages.profile.securityTitle') }}</h3>
        <p class="card-subtitle">{{ t('pages.profile.securitySubtitle') }}</p>
        <el-button type="primary" plain @click="handleChangePassword">
          {{ t('pages.profile.changePassword') }}
        </el-button>
      </el-card>
    </div>

    <el-dialog
      v-model="passwordDialogVisible"
      :title="t('pages.profile.passwordDialogTitle')"
      width="420px"
      @close="resetPasswordForm"
    >
      <el-form
        ref="passwordFormRef"
        :model="passwordForm"
        :rules="passwordRules"
        label-width="100px"
      >
        <el-form-item :label="t('pages.profile.currentPassword')" prop="currentPassword">
          <el-input
            v-model="passwordForm.currentPassword"
            type="password"
            show-password
            :placeholder="t('pages.profile.currentPasswordPlaceholder')"
          />
        </el-form-item>
        <el-form-item :label="t('pages.profile.newPassword')" prop="newPassword">
          <el-input
            v-model="passwordForm.newPassword"
            type="password"
            show-password
            :placeholder="t('pages.profile.newPasswordPlaceholder')"
          />
        </el-form-item>
        <el-form-item :label="t('pages.profile.confirmPassword')" prop="confirmPassword">
          <el-input
            v-model="passwordForm.confirmPassword"
            type="password"
            show-password
            :placeholder="t('pages.profile.confirmPasswordPlaceholder')"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="passwordDialogVisible = false">{{ t('stage6.common.actions.cancel') }}</el-button>
        <el-button type="primary" :loading="passwordLoading" @click="submitChangePassword">
          {{ t('stage6.common.actions.confirm') }}
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
