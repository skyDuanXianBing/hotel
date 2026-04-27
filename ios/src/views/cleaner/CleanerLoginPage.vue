<template>
  <ion-page>
    <ion-header translucent>
      <ion-toolbar class="app-page-header__toolbar">
        <ion-title class="app-page-header__title">保洁员登录</ion-title>
      </ion-toolbar>
    </ion-header>

    <ion-content fullscreen class="mobile-page cleaner-auth-page">
      <section class="mobile-hero cleaner-auth-page__hero">
        <p class="mobile-note cleaner-auth-page__eyebrow">Cleaner Workspace</p>
        <h1 class="mobile-title">保洁员工作台</h1>
        <p class="mobile-subtitle">使用邀请注册后的邮箱和密码登录，进入自己的任务工作台。</p>
      </section>

      <div class="mobile-stack">
        <section class="mobile-card cleaner-auth-page__card">
          <div class="cleaner-auth-page__header">
            <h2 class="mobile-section-title">欢迎回来</h2>
            <p class="mobile-note">登录后可查看月历任务、按日期查看详情并处理任务状态。</p>
          </div>

          <ion-list lines="none" class="cleaner-auth-page__list">
            <ion-item class="cleaner-auth-page__item">
              <ion-input
                v-model="email"
                autocomplete="email"
                inputmode="email"
                label="邮箱"
                label-placement="stacked"
                placeholder="请输入登录邮箱"
                type="email"
              />
            </ion-item>

            <ion-item class="cleaner-auth-page__item">
              <ion-input
                v-model="password"
                autocomplete="current-password"
                label="密码"
                label-placement="stacked"
                placeholder="请输入密码"
                type="password"
              />
            </ion-item>
          </ion-list>

          <ion-button expand="block" class="cleaner-auth-page__submit" :disabled="submitting" @click="handleLogin">
            <ion-spinner v-if="submitting" name="crescent" />
            <span v-else>登录并进入工作台</span>
          </ion-button>

          <div class="cleaner-auth-page__footer-link">
            <span>收到邀请链接？</span>
            <button class="cleaner-auth-page__text-button" type="button" @click="handleGoToRegister">
              去注册
            </button>
          </div>
        </section>
      </div>
    </ion-content>
  </ion-page>
</template>

<script setup lang="ts">
import {
  IonButton,
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
import { ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { cleanerLoginByPassword } from '@/api/cleanerAuth'
import { ROUTE_PATHS } from '@/router/guards'
import type { CleanerSessionUser } from '@/types/auth'
import { showErrorToast, showSuccessToast, showWarningToast } from '@/utils/notify'
import { isHandledRequestError } from '@/utils/request'
import { saveCleanerSession } from '@/utils/cleanerSession'

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
const PASSWORD_MIN_LENGTH = 6

const route = useRoute()
const router = useRouter()

const email = ref('')
const password = ref('')
const submitting = ref(false)

function applyEmailPrefill(value: unknown) {
  if (typeof value !== 'string') {
    return
  }

  const nextEmail = value.trim()
  if (!nextEmail) {
    return
  }

  email.value = nextEmail
}

function validateForm() {
  const normalizedEmail = email.value.trim()
  const normalizedPassword = password.value.trim()

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
    email: normalizedEmail,
    password: normalizedPassword,
  }
}

function buildCleanerSessionUser(cleaner: {
  id: number
  email: string
  name: string
  createdAt: string
  updatedAt: string
}): CleanerSessionUser {
  return {
    id: cleaner.id,
    email: cleaner.email,
    nickname: cleaner.name,
    gender: 'private',
    createdAt: cleaner.createdAt,
    updatedAt: cleaner.updatedAt,
    isCleaner: true,
  }
}

async function handleLogin() {
  if (submitting.value) {
    return
  }

  const payload = validateForm()
  if (!payload) {
    return
  }

  submitting.value = true

  try {
    const response = await cleanerLoginByPassword(payload)

    if (!response.success || !response.data?.token || !response.data.cleaner) {
      showErrorToast(response.message || '登录失败')
      return
    }

    const cleanerUser = buildCleanerSessionUser(response.data.cleaner)
    saveCleanerSession(response.data.token, cleanerUser, response.data.cleaner.storeId)
    showSuccessToast('登录成功')
    await router.replace(ROUTE_PATHS.cleanerDashboard)
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showErrorToast(error instanceof Error ? error.message : '登录失败')
    }
  } finally {
    submitting.value = false
  }
}

async function handleGoToRegister() {
  await router.push(ROUTE_PATHS.cleanerRegister)
}

watch(
  () => route.query.email,
  (nextEmail) => {
    applyEmailPrefill(nextEmail)
  },
  {
    immediate: true,
  },
)
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
