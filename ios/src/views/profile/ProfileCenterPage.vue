<template>
  <ion-page>
    <ion-header translucent>
      <ion-toolbar>
        <ion-buttons slot="start">
          <ion-back-button :default-href="ROUTE_PATHS.settings" />
        </ion-buttons>
        <ion-title>个人中心</ion-title>
      </ion-toolbar>
    </ion-header>

    <ion-content fullscreen class="mobile-page profile-page">
      <ion-refresher slot="fixed" @ionRefresh="handleRefresh">
        <ion-refresher-content pulling-text="下拉刷新个人资料" refreshing-spinner="crescent" />
      </ion-refresher>

      <section class="mobile-hero profile-hero">
        <p class="mobile-note profile-hero__eyebrow">账号中心迁移</p>
        <h1 class="mobile-title">{{ displayName }}</h1>
        <p class="mobile-subtitle">{{ userStore.currentUser?.email || '未恢复邮箱信息' }}</p>
        <div class="mobile-chip-row">
          <span class="mobile-chip">昵称可编辑</span>
          <span class="mobile-chip">支持改密</span>
          <span class="mobile-chip">与 Web 资料字段对齐</span>
        </div>
      </section>

      <div class="mobile-stack">
        <section class="mobile-card profile-avatar-card">
          <div class="profile-avatar-card__avatar">
            <img v-if="form.avatar.trim()" :src="form.avatar.trim()" alt="头像" />
            <span v-else>{{ avatarFallback }}</span>
          </div>
          <div>
            <h2 class="mobile-section-title">资料概览</h2>
            <p class="mobile-note">修改密码成功后会强制重新登录，保持与 Web 行为一致。</p>
          </div>
        </section>

        <section class="mobile-card profile-form-card">
          <div class="mobile-inline-row">
            <div>
              <h2 class="mobile-section-title">个人资料</h2>
              <p class="mobile-note">支持昵称、性别和头像地址维护。</p>
            </div>
            <ion-spinner v-if="loading || saving" name="crescent" />
          </div>

          <div class="profile-form-grid">
            <label class="profile-form-field">
              <span>昵称</span>
              <ion-input v-model="form.nickname" fill="outline" placeholder="请输入昵称" />
            </label>

            <label class="profile-form-field">
              <span>性别</span>
              <ion-select v-model="form.gender" fill="outline" interface="action-sheet">
                <ion-select-option value="male">男</ion-select-option>
                <ion-select-option value="female">女</ion-select-option>
                <ion-select-option value="private">保密</ion-select-option>
              </ion-select>
            </label>

            <label class="profile-form-field profile-form-field--full">
              <span>头像地址</span>
              <ion-input v-model="form.avatar" fill="outline" placeholder="请输入头像 URL，可选" />
            </label>
          </div>

          <div class="profile-form-actions">
            <ion-button fill="outline" :disabled="saving" @click="resetForm">重置</ion-button>
            <ion-button :disabled="saving" @click="handleSaveProfile">
              {{ saving ? '保存中...' : '保存资料' }}
            </ion-button>
          </div>
        </section>

        <section class="mobile-card profile-security-card">
          <h2 class="mobile-section-title">账户安全</h2>
          <p class="mobile-note">建议定期更新密码，确保新密码与旧密码不同。</p>
          <ion-button fill="outline" @click="passwordModalOpen = true">修改密码</ion-button>
        </section>
      </div>

      <ion-modal :is-open="passwordModalOpen" @didDismiss="handleDismissPasswordModal">
        <ion-header>
          <ion-toolbar>
            <ion-title>修改密码</ion-title>
            <ion-buttons slot="end">
              <ion-button @click="handleDismissPasswordModal">关闭</ion-button>
            </ion-buttons>
          </ion-toolbar>
        </ion-header>

        <ion-content class="mobile-page profile-modal-page">
          <section class="mobile-card profile-form-card">
            <div class="profile-form-grid">
              <label class="profile-form-field">
                <span>当前密码</span>
                <ion-input v-model="passwordForm.currentPassword" fill="outline" type="password" placeholder="请输入当前密码" />
              </label>

              <label class="profile-form-field">
                <span>新密码</span>
                <ion-input v-model="passwordForm.newPassword" fill="outline" type="password" placeholder="请输入新密码" />
              </label>

              <label class="profile-form-field">
                <span>确认新密码</span>
                <ion-input v-model="passwordForm.confirmPassword" fill="outline" type="password" placeholder="请再次输入新密码" />
              </label>
            </div>

            <div class="profile-form-actions">
              <ion-button fill="outline" :disabled="changingPassword" @click="handleDismissPasswordModal">
                取消
              </ion-button>
              <ion-button :disabled="changingPassword" @click="handleChangePassword">
                {{ changingPassword ? '提交中...' : '确认修改' }}
              </ion-button>
            </div>
          </section>
        </ion-content>
      </ion-modal>
    </ion-content>
  </ion-page>
</template>

<script setup lang="ts">
import {
  IonBackButton,
  IonButton,
  IonButtons,
  IonContent,
  IonHeader,
  IonInput,
  IonModal,
  IonPage,
  IonRefresher,
  IonRefresherContent,
  IonSelect,
  IonSelectOption,
  IonSpinner,
  IonTitle,
  IonToolbar,
  onIonViewWillEnter,
} from '@ionic/vue'
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { changePassword, updateProfile } from '@/api/auth'
import { ROUTE_PATHS } from '@/router/guards'
import { useUserStore } from '@/stores/user'
import { isHandledRequestError } from '@/utils/request'
import { showSuccessToast, showWarningToast } from '@/utils/notify'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const saving = ref(false)
const changingPassword = ref(false)
const passwordModalOpen = ref(false)

const form = reactive({
  nickname: '',
  gender: 'private' as 'male' | 'female' | 'private',
  avatar: '',
})

const passwordForm = reactive({
  currentPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const displayName = computed(() => {
  if (form.nickname.trim()) {
    return form.nickname.trim()
  }

  if (userStore.currentUser?.email) {
    return userStore.currentUser.email.split('@')[0]
  }

  return '个人中心'
})

const avatarFallback = computed(() => {
  const value = displayName.value.trim()
  if (!value) {
    return '我'
  }
  return value.slice(0, 1)
})

function normalizeGender(value?: string | null) {
  if (value === 'male' || value === 'female') {
    return value
  }
  return 'private'
}

function resolveWarningMessage(error: unknown, fallbackMessage: string) {
  if (error instanceof Error && error.message) {
    return error.message
  }

  return fallbackMessage
}

function syncForm() {
  form.nickname = userStore.currentUser?.nickname || userStore.currentUser?.email?.split('@')[0] || ''
  form.gender = normalizeGender(userStore.currentUser?.gender)
  form.avatar = userStore.currentUser?.avatar || ''
}

function resetPasswordForm() {
  passwordForm.currentPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
}

async function loadProfile(force = false) {
  loading.value = true
  try {
    await userStore.fetchCurrentUser(force)
    syncForm()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, '加载个人资料失败'))
    }
  } finally {
    loading.value = false
  }
}

function resetForm() {
  syncForm()
}

async function handleSaveProfile() {
  if (!form.nickname.trim()) {
    showWarningToast('请输入昵称')
    return
  }

  saving.value = true
  try {
    const response = await updateProfile({
      nickname: form.nickname.trim(),
      gender: form.gender,
      avatar: form.avatar.trim() || undefined,
    })
    if (!response.success || !response.data) {
      throw new Error(response.message || '更新个人资料失败')
    }

    userStore.setUser(response.data)
    syncForm()
    showSuccessToast('个人资料已更新')
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, '更新个人资料失败'))
    }
  } finally {
    saving.value = false
  }
}

function handleDismissPasswordModal() {
  passwordModalOpen.value = false
  resetPasswordForm()
}

async function handleChangePassword() {
  if (!passwordForm.currentPassword.trim()) {
    showWarningToast('请输入当前密码')
    return
  }

  if (passwordForm.newPassword.trim().length < 6) {
    showWarningToast('新密码长度至少为 6 位')
    return
  }

  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    showWarningToast('两次输入的新密码不一致')
    return
  }

  changingPassword.value = true
  try {
    const response = await changePassword({
      currentPassword: passwordForm.currentPassword,
      newPassword: passwordForm.newPassword,
      confirmPassword: passwordForm.confirmPassword,
    })
    if (!response.success) {
      throw new Error(response.message || '修改密码失败')
    }

    showSuccessToast('密码修改成功，请重新登录')
    handleDismissPasswordModal()
    await userStore.logout()
    await router.replace(ROUTE_PATHS.login)
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, '修改密码失败'))
    }
  } finally {
    changingPassword.value = false
  }
}

async function handleRefresh(event: CustomEvent) {
  try {
    await loadProfile(true)
  } finally {
    event.detail.complete()
  }
}

onIonViewWillEnter(async () => {
  await loadProfile(false)
})
</script>

<style scoped>
.profile-page {
  display: block;
}

.profile-hero {
  margin-top: 4px;
}

.profile-hero__eyebrow {
  color: var(--ion-color-primary);
  font-weight: 700;
}

.profile-avatar-card {
  display: grid;
  grid-template-columns: 68px minmax(0, 1fr);
  gap: 14px;
  align-items: center;
}

.profile-avatar-card__avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 68px;
  height: 68px;
  border-radius: 50%;
  background: var(--app-primary-soft-strong);
  color: var(--ion-color-primary);
  font-size: 28px;
  font-weight: 700;
  overflow: hidden;
}

.profile-avatar-card__avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.profile-form-card,
.profile-security-card {
  display: grid;
  gap: 14px;
}

.profile-form-grid {
  display: grid;
  gap: 14px;
}

.profile-form-field {
  display: grid;
  gap: 8px;
}

.profile-form-field span {
  color: var(--app-heading);
  font-size: 13px;
  font-weight: 600;
}

.profile-form-field--full {
  grid-column: 1 / -1;
}

.profile-form-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.profile-modal-page {
  --padding-top: 16px;
  --padding-bottom: 24px;
  --padding-start: 16px;
  --padding-end: 16px;
}
</style>
