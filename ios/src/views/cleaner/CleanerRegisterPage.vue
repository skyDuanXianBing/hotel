<template>
  <ion-page>
    <ion-header translucent>
      <ion-toolbar class="app-page-header__toolbar">
        <ion-buttons slot="start">
          <ion-back-button class="app-page-header__back-btn" :default-href="ROUTE_PATHS.cleanerLogin" />
        </ion-buttons>
        <ion-title class="app-page-header__title">保洁员注册</ion-title>
      </ion-toolbar>
    </ion-header>

    <ion-content fullscreen class="mobile-page cleaner-auth-page">
      <section class="mobile-hero cleaner-auth-page__hero">
        <p class="mobile-note cleaner-auth-page__eyebrow">Cleaner Invitation</p>
        <h1 class="mobile-title">完成邀请注册</h1>
        <p class="mobile-subtitle">先校验邀请链接，再设置登录密码，注册成功后即可进入保洁员登录页。</p>
      </section>

      <div class="mobile-stack">
        <section class="mobile-card cleaner-auth-page__card">
          <div class="cleaner-auth-page__header">
            <h2 class="mobile-section-title">创建账户</h2>
            <p class="mobile-note">姓名和邮箱以邀请信息为准，密码长度至少 6 位。</p>
          </div>

          <p v-if="validating" class="mobile-note">正在校验邀请链接...</p>

          <ion-list lines="none" class="cleaner-auth-page__list">
            <ion-item class="cleaner-auth-page__item">
              <ion-input
                v-model="name"
                :disabled="validating || submitting || invitationLocked"
                label="姓名"
                label-placement="stacked"
                placeholder="请输入姓名"
                type="text"
              />
            </ion-item>

            <ion-item class="cleaner-auth-page__item">
              <ion-input
                v-model="email"
                :disabled="validating || submitting || invitationLocked"
                autocomplete="email"
                inputmode="email"
                label="邮箱"
                label-placement="stacked"
                placeholder="请输入邮箱"
                type="email"
              />
            </ion-item>

            <ion-item class="cleaner-auth-page__item">
              <ion-input
                v-model="password"
                :disabled="validating || submitting"
                autocomplete="new-password"
                label="密码"
                label-placement="stacked"
                placeholder="请设置密码"
                type="password"
              />
            </ion-item>
          </ion-list>

          <ion-button expand="block" class="cleaner-auth-page__submit" :disabled="submitting || validating" @click="handleRegister">
            <ion-spinner v-if="submitting" name="crescent" />
            <span v-else>提交注册</span>
          </ion-button>

          <div class="cleaner-auth-page__footer-link">
            <span>已经注册完成？</span>
            <button class="cleaner-auth-page__text-button" type="button" @click="handleGoToLogin">去登录</button>
          </div>
        </section>
      </div>
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
  IonItem,
  IonList,
  IonPage,
  IonSpinner,
  IonTitle,
  IonToolbar,
} from '@ionic/vue'
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { registerCleaner, validateInvitationToken } from '@/api/cleaning'
import { ROUTE_PATHS } from '@/router/guards'
import { showErrorToast, showSuccessToast, showWarningToast } from '@/utils/notify'
import { isHandledRequestError } from '@/utils/request'

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
const PASSWORD_MIN_LENGTH = 6

const route = useRoute()
const router = useRouter()

const name = ref('')
const email = ref('')
const password = ref('')
const validating = ref(false)
const submitting = ref(false)
const invitationLocked = ref(false)

const invitationToken = computed(() => {
  const tokenQuery = route.query.token
  if (typeof tokenQuery !== 'string') {
    return ''
  }

  return tokenQuery.trim()
})

function buildLoginLocation() {
  const normalizedEmail = email.value.trim()

  if (!normalizedEmail) {
    return {
      path: ROUTE_PATHS.cleanerLogin,
    }
  }

  return {
    path: ROUTE_PATHS.cleanerLogin,
    query: {
      email: normalizedEmail,
    },
  }
}

function validateForm() {
  const normalizedName = name.value.trim()
  const normalizedEmail = email.value.trim()
  const normalizedPassword = password.value.trim()

  if (!invitationToken.value) {
    showWarningToast('邀请链接无效')
    return null
  }

  if (!normalizedName) {
    showWarningToast('请输入姓名')
    return null
  }

  if (!normalizedEmail) {
    showWarningToast('请输入邮箱')
    return null
  }

  if (!EMAIL_PATTERN.test(normalizedEmail)) {
    showWarningToast('请输入正确的邮箱格式')
    return null
  }

  if (!normalizedPassword) {
    showWarningToast('请输入密码')
    return null
  }

  if (normalizedPassword.length < PASSWORD_MIN_LENGTH) {
    showWarningToast(`密码长度至少为 ${PASSWORD_MIN_LENGTH} 位`)
    return null
  }

  return {
    token: invitationToken.value,
    name: normalizedName,
    email: normalizedEmail,
    password: normalizedPassword,
  }
}

async function redirectToLoginWithError(message: string) {
  showErrorToast(message)
  await router.replace(ROUTE_PATHS.cleanerLogin)
}

async function loadInvitation() {
  if (!invitationToken.value) {
    await redirectToLoginWithError('邀请链接无效')
    return
  }

  validating.value = true

  try {
    const response = await validateInvitationToken(invitationToken.value)
    if (!response.success || !response.data) {
      await redirectToLoginWithError(response.message || '邀请链接已失效')
      return
    }

    name.value = response.data.name
    email.value = response.data.email
    invitationLocked.value = true
  } catch (error) {
    const message = error instanceof Error ? error.message : '邀请校验失败'

    if (!isHandledRequestError(error)) {
      await redirectToLoginWithError(message)
      return
    }

    await redirectToLoginWithError(message)
  } finally {
    validating.value = false
  }
}

async function handleRegister() {
  if (submitting.value || validating.value) {
    return
  }

  const payload = validateForm()
  if (!payload) {
    return
  }

  submitting.value = true

  try {
    const response = await registerCleaner(payload)
    if (!response.success) {
      showErrorToast(response.message || '注册失败')
      return
    }

    showSuccessToast('注册成功，请登录')
    await router.replace(buildLoginLocation())
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showErrorToast(error instanceof Error ? error.message : '注册失败')
    }
  } finally {
    submitting.value = false
  }
}

async function handleGoToLogin() {
  await router.replace(buildLoginLocation())
}

onMounted(async () => {
  await loadInvitation()
})
</script>

<style scoped>
.cleaner-auth-page {
  display: block;
}

.cleaner-auth-page__hero {
  margin-top: 16px;
  text-align: center;
}

.cleaner-auth-page__eyebrow {
  color: var(--ion-color-primary);
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.cleaner-auth-page__card {
  display: grid;
  gap: 16px;
  padding: 24px 20px;
}

.cleaner-auth-page__header {
  display: grid;
  gap: 6px;
}

.cleaner-auth-page__list {
  margin: 0;
  background: transparent;
}

.cleaner-auth-page__item {
  --background: #f8fafc;
  --border-radius: 14px;
  --padding-start: 16px;
  --padding-end: 16px;
  --inner-padding-end: 0;
  margin-bottom: 14px;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
}

.cleaner-auth-page__submit {
  min-height: 52px;
  --border-radius: 14px;
  font-weight: 600;
}

.cleaner-auth-page__footer-link {
  display: flex;
  justify-content: center;
  gap: 6px;
  color: var(--app-muted);
  font-size: 14px;
}

.cleaner-auth-page__text-button {
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--ion-color-primary);
  font: inherit;
  font-weight: 600;
}

:deep(.cleaner-auth-page__item ion-input) {
  --padding-top: 10px;
  --padding-bottom: 10px;
}

:deep(.cleaner-auth-page__item .label-text-wrapper) {
  margin-bottom: 8px;
  color: var(--app-heading);
  font-weight: 600;
}
</style>
