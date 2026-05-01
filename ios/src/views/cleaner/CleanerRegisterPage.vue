<template>
  <ion-page>
    <ion-content fullscreen class="mobile-page auth-page">
      <div class="mobile-stack">
        <section class="mobile-card auth-card">
          <button class="auth-back-button" type="button" @click="handleGoToLogin">
            <span class="auth-back-button__icon">&larr;</span>
          </button>

          <div class="auth-card__header auth-card__header--details">
            <h2 class="mobile-section-title">完成注册</h2>
            <p class="mobile-note auth-card__subtitle">校验邀请信息后，设置密码即可登录保洁工作台。</p>
          </div>

          <p v-if="validating" class="auth-status-note">正在校验邀请链接...</p>

          <ion-list lines="none" class="auth-list">
            <p class="auth-field-label">姓名</p>
            <ion-item
              :class="[
                'auth-item',
                {
                  'auth-item--readonly': invitationLocked,
                  'auth-item--focused': focusedField === 'name',
                },
              ]"
            >
              <ion-input
                v-model="name"
                :disabled="validating || submitting"
                :readonly="invitationLocked"
                placeholder="请输入姓名"
                type="text"
                @ionBlur="handleInputBlur('name')"
                @ionFocus="handleInputFocus('name')"
              />
            </ion-item>

            <p class="auth-field-label">邮箱</p>
            <ion-item
              :class="[
                'auth-item',
                {
                  'auth-item--readonly': invitationLocked,
                  'auth-item--focused': focusedField === 'email',
                },
              ]"
            >
              <ion-input
                v-model="email"
                :disabled="validating || submitting"
                :readonly="invitationLocked"
                autocomplete="email"
                inputmode="email"
                placeholder="请输入邮箱"
                type="email"
                @ionBlur="handleInputBlur('email')"
                @ionFocus="handleInputFocus('email')"
              />
            </ion-item>

            <p class="auth-field-label">密码</p>
            <ion-item :class="['auth-item', { 'auth-item--focused': focusedField === 'password' }]">
              <ion-input
                v-model="password"
                :disabled="validating || submitting"
                autocomplete="new-password"
                placeholder="请设置密码"
                type="password"
                @ionBlur="handleInputBlur('password')"
                @ionFocus="handleInputFocus('password')"
              />
            </ion-item>
          </ion-list>

          <ion-button expand="block" class="auth-submit-button" :disabled="submitting || validating" @click="handleRegister">
            <ion-spinner v-if="submitting" name="crescent" />
            <span v-else>注册</span>
          </ion-button>

          <div class="auth-footer-link">
            <button class="auth-text-button auth-footer-link__button" type="button" @click="handleGoToLogin">
              已有账号？登录
            </button>
          </div>
        </section>
      </div>
    </ion-content>
  </ion-page>
</template>

<script setup lang="ts">
import { IonButton, IonContent, IonInput, IonItem, IonList, IonPage, IonSpinner } from '@ionic/vue'
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { registerCleaner, validateInvitationToken } from '@/api/cleaning'
import { ROUTE_PATHS } from '@/router/guards'
import { showErrorToast, showSuccessToast, showWarningToast } from '@/utils/notify'
import { isHandledRequestError } from '@/utils/request'

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
const PASSWORD_MIN_LENGTH = 6

type FocusedField = 'name' | 'email' | 'password' | null

const route = useRoute()
const router = useRouter()

const name = ref('')
const email = ref('')
const password = ref('')
const validating = ref(false)
const submitting = ref(false)
const invitationLocked = ref(false)
const focusedField = ref<FocusedField>(null)

const invitationToken = computed(() => {
  const tokenQuery = route.query.token
  if (typeof tokenQuery !== 'string') {
    return ''
  }

  return tokenQuery.trim()
})

function handleInputFocus(field: Exclude<FocusedField, null>) {
  focusedField.value = field
}

function handleInputBlur(field: Exclude<FocusedField, null>) {
  if (focusedField.value === field) {
    focusedField.value = null
  }
}

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
.auth-page {
  --auth-control-radius: 13px;
  --background: #fcfcfc;
  --padding-top: calc(38px + var(--app-safe-top));
  --padding-bottom: calc(28px + var(--app-safe-bottom));
  --padding-start: 24px;
  --padding-end: 24px;
  display: block;
  color: #1f2128;
}

.mobile-stack {
  min-height: calc(100vh - var(--app-safe-top) - var(--app-safe-bottom) - 72px);
  overflow: hidden;
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
  height: calc(100vh - var(--app-safe-top) - var(--app-safe-bottom) - 74px);
  display: flex;
  flex-direction: column;
  position: relative;
  overflow: hidden;
}

.auth-back-button {
  position: absolute;
  top: 4px;
  left: -2px;
  width: 36px;
  height: 36px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 0;
  padding: 0;
  background: transparent;
  color: #1f2128;
}

.auth-back-button__icon {
  font-size: 30px;
  line-height: 1;
  transform: translateY(-1px);
}

.auth-card__header {
  margin: 0 0 28px;
}

.auth-card__header--details {
  margin-top: 8px;
}

.mobile-section-title {
  margin: 0;
  color: #1f2128;
  font-size: clamp(29px, 9vw, 35px);
  font-weight: 400;
  line-height: 1.18;
}

.mobile-section-title::before {
  content: '👋 ';
}

.auth-card__subtitle,
.auth-status-note {
  margin: 12px 0 0;
  color: #8f949e;
  font-size: clamp(16px, 4.8vw, 18px);
  line-height: 1.4;
}

.auth-status-note {
  margin: -8px 0 18px;
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
  margin-bottom: 18px;
  --min-height: var(--auth-input-height);
  height: var(--auth-input-height);
  min-height: var(--auth-input-height);
  border: 2px solid #dde5e8;
  border-radius: var(--auth-control-radius);
  box-shadow: none;
  transition: border-color 0.2s ease;
  position: relative;
}

.auth-item--readonly {
  background: #f7f8fa;
}

.auth-item::part(native) {
  min-height: var(--auth-input-height);
  height: var(--auth-input-height);
  padding-top: 0;
  padding-bottom: 0;
  display: flex;
  align-items: center;
}

.auth-item.item-has-focus,
.auth-item--focused {
  border-color: #3484ea;
}

.auth-submit-button {
  margin-top: 10px;
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

.auth-footer-link {
  margin-top: auto;
  margin-bottom: 2px;
  padding-top: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.auth-footer-link__button {
  color: #3484ea;
  font-size: clamp(16px, 4.8vw, 18px);
  font-weight: 400;
  line-height: 1.24;
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
  --placeholder-color: #c4c8cf;
  --placeholder-opacity: 1;
}

::deep(.auth-item ion-input::part(native)) {
  height: auto;
  min-height: 0;
  line-height: 1.2;
}

::deep(.auth-item .input-wrapper),
::deep(.auth-item .native-wrapper) {
  height: 100%;
  min-height: 100%;
  display: flex;
  align-items: center;
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

::deep(.auth-item--readonly .native-input),
::deep(.auth-item--readonly input) {
  color: #b8bcc4;
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
}

@media (max-width: 480px) {
  .auth-card {
    padding-top: 52px;
  }

  .auth-back-button {
    top: 0;
  }

  .auth-card__header--details {
    margin-top: 4px;
  }

  .auth-footer-link {
    padding-top: 20px;
  }
}
</style>
