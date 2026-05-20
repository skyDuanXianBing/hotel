<template>
  <ion-page>
    <ion-content fullscreen class="mobile-page auth-page">
      <div class="mobile-stack">
        <section class="mobile-card auth-card">
          <div class="auth-card__header">
            <h2 class="mobile-section-title">保洁工作台登录</h2>
            <p class="mobile-note">使用邀请注册后的邮箱和密码登录，查看并处理当日保洁任务。</p>
          </div>

          <ion-list lines="none" class="auth-list">
            <p class="auth-field-label">邮箱</p>
            <ion-item :class="['auth-item', { 'auth-item--focused': focusedField === 'email' }]">
              <ion-input
                v-model="email"
                autocomplete="email"
                inputmode="email"
                placeholder="请输入登录邮箱"
                type="email"
                @ionBlur="handleInputBlur('email')"
                @ionFocus="handleInputFocus('email')"
              />
            </ion-item>

            <p class="auth-field-label">密码</p>
            <ion-item :class="['auth-item', { 'auth-item--focused': focusedField === 'password' }]">
              <ion-input
                v-model="password"
                autocomplete="current-password"
                placeholder="请输入密码"
                type="password"
                @ionBlur="handleInputBlur('password')"
                @ionFocus="handleInputFocus('password')"
              />
            </ion-item>
          </ion-list>

          <ion-button expand="block" class="auth-submit-button" :disabled="submitting" @click="handleLogin">
            <ion-spinner v-if="submitting" name="crescent" />
            <span v-else>登录</span>
          </ion-button>

          <div class="auth-footer">
            <div class="auth-helper-link auth-helper-link--secondary">
              <span>不是保洁人员？</span>
              <button class="auth-text-button auth-helper-link__button" type="button" @click="handleGoToAdminLogin">
                返回登录
              </button>
            </div>
          </div>
        </section>
      </div>
    </ion-content>
  </ion-page>
</template>

<script setup lang="ts">
import { IonButton, IonContent, IonInput, IonItem, IonList, IonPage, IonSpinner } from '@ionic/vue'
import { ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { cleanerLoginByPassword } from '@/api/cleanerAuth'
import { ROUTE_PATHS } from '@/router/guards'
import type { CleanerSessionUser } from '@/types/auth'
import { saveCleanerSession } from '@/utils/cleanerSession'
import { showErrorToast, showSuccessToast, showWarningToast } from '@/utils/notify'
import { isHandledRequestError } from '@/utils/request'

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
const PASSWORD_MIN_LENGTH = 6

type FocusedField = 'email' | 'password' | null

const route = useRoute()
const router = useRouter()

const email = ref('')
const password = ref('')
const submitting = ref(false)
const focusedField = ref<FocusedField>(null)

function handleInputFocus(field: Exclude<FocusedField, null>) {
  focusedField.value = field
}

function handleInputBlur(field: Exclude<FocusedField, null>) {
  if (focusedField.value === field) {
    focusedField.value = null
  }
}

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

async function handleGoToAdminLogin() {
  const normalizedEmail = email.value.trim()

  if (!normalizedEmail) {
    await router.replace(ROUTE_PATHS.login)
    return
  }

  await router.replace({
    path: ROUTE_PATHS.login,
    query: { email: normalizedEmail },
  })
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
.auth-page {
  --auth-control-radius: 13px;
  --background: #fcfcfc;
  --padding-top: calc(44px + var(--app-safe-top));
  --padding-bottom: calc(28px + var(--app-safe-bottom));
  --padding-start: 24px;
  --padding-end: 24px;
  display: block;
  color: #1f2128;
}

.mobile-stack {
  min-height: calc(100vh - var(--app-safe-top) - var(--app-safe-bottom) - 72px);
}

.auth-card {
  width: 100%;
  max-width: 560px;
  margin: 0;
  padding: 74px 0 0;
  border-radius: 0;
  box-shadow: none;
  background: transparent;
  border: 0;
  min-height: calc(100vh - var(--app-safe-top) - var(--app-safe-bottom) - 74px);
  display: flex;
  flex-direction: column;
}

.auth-card__header {
  margin: 0 0 40px;
}

.auth-card__header .mobile-note {
  display: none;
}

.mobile-section-title {
  margin: 0;
  color: #1f2128;
  font-size: clamp(33px, 7.8vw, 37px);
  font-weight: 400;
  line-height: 1.18;
  letter-spacing: 0;
}

.mobile-section-title::before {
  content: '🧼 ';
}

.auth-list {
  margin: 0;
  background: transparent;
}

.auth-field-label {
  margin: 0 0 10px 2px;
  color: #666b74;
  font-size: clamp(16px, 4.9vw, 19px);
  font-weight: 400;
  line-height: 1.32;
}

.auth-item {
  --auth-input-height: clamp(54px, 13.5vw, 64px);
  --background: #fcfcfc;
  --border-radius: var(--auth-control-radius);
  --padding-start: 20px;
  --padding-end: 20px;
  --inner-padding-end: 0;
  --inner-padding-top: 0;
  --inner-padding-bottom: 0;
  --highlight-height: 0;
  margin-bottom: 24px;
  --min-height: var(--auth-input-height);
  height: var(--auth-input-height);
  min-height: var(--auth-input-height);
  border: 2px solid #d6d7dc;
  border-radius: var(--auth-control-radius);
  box-shadow: none;
  transition: border-color 0.2s ease;
  position: relative;
}

.auth-item::part(native) {
  min-height: var(--auth-input-height);
  height: var(--auth-input-height);
  padding-top: 0;
  padding-bottom: 0;
  display: flex;
  align-items: center;
}

.auth-list > .auth-item:first-of-type::after {
  content: '✓';
  position: absolute;
  right: 18px;
  top: 50%;
  transform: translateY(-50%);
  color: #1f2128;
  font-size: clamp(18px, 4.6vw, 22px);
  line-height: 1;
  pointer-events: none;
}

.auth-item.item-has-focus,
.auth-item--focused {
  border-color: #3484ea;
  box-shadow: none;
}

.auth-submit-button {
  margin-top: 0;
  min-height: clamp(54px, 13.5vw, 64px);
  font-size: clamp(21px, 5.8vw, 24px);
  font-weight: 400;
  letter-spacing: 0;
  --border-radius: var(--auth-control-radius);
  --background: #3484ea;
  --background-activated: #2e77d4;
  --background-focused: #2e77d4;
  --background-hover: #2e77d4;
  --box-shadow: none;
  --color: #ffffff;
}

.auth-footer {
  margin-top: auto;
  margin-bottom: 2px;
  padding-top: 64px;
  display: grid;
  gap: 12px;
}

.auth-helper-link {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  color: #5d626b;
  font-size: clamp(14px, 4.1vw, 16px);
  line-height: 1.24;
}

.auth-helper-link__button {
  color: #3484ea;
}

.auth-helper-link--secondary {
  color: #5d626b;
}

.auth-text-button {
  padding: 0;
  border: 0;
  background: transparent;
  font: inherit;
}

::deep(.auth-item ion-input) {
  width: 100%;
  height: 100%;
  min-height: 100%;
  display: flex;
  align-items: center;
  --padding-top: 0;
  --padding-bottom: 0;
  --placeholder-color: #bdbfc4;
  --placeholder-opacity: 1;
}

::deep(.auth-item ion-input::part(native)) {
  height: auto;
  min-height: 0;
  line-height: 1.2;
  padding-top: 0;
  padding-bottom: 0;
}

::deep(.auth-item .input-wrapper),
::deep(.auth-item .native-wrapper) {
  height: 100%;
  min-height: 100%;
  display: flex;
  align-items: center;
}

::deep(.auth-list > .auth-item:first-of-type ion-input) {
  --padding-end: 22px;
}

::deep(.auth-item .native-input),
::deep(.auth-item input) {
  color: #1f2128;
  font-size: clamp(16px, 5.1vw, 20px);
  font-weight: 400;
  height: auto;
  min-height: 0;
  box-sizing: border-box;
  margin: 0;
  padding-top: 0;
  padding-bottom: 0;
  line-height: 1.2;
}

::deep(.auth-item .native-input::placeholder),
::deep(.auth-item input::placeholder) {
  color: #bdbfc4;
}

::deep(.auth-submit-button .button-native) {
  padding: 0 14px;
}

@media (min-width: 768px) {
  .auth-page {
    --padding-top: calc(78px + var(--app-safe-top));
    --padding-start: 56px;
    --padding-end: 56px;
  }

  .auth-card {
    margin: 0 auto;
    min-height: calc(100vh - var(--app-safe-top) - var(--app-safe-bottom) - 102px);
  }

  .mobile-section-title {
    font-size: 35px;
  }

  .auth-field-label,
  :deep(.auth-item .native-input),
  :deep(.auth-item input) {
    font-size: 20px;
  }
}

@media (max-width: 480px) {
  .auth-page {
    --padding-top: calc(38px + var(--app-safe-top));
    --padding-start: 24px;
    --padding-end: 24px;
  }
}
</style>
