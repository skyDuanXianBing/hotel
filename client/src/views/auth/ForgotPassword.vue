<template>
  <div class="forgot-password-container">
    <div class="forgot-password-layout">
      <div class="left-section">
        <img
          :src="forgotPasswordIllustration"
          alt=""
          aria-hidden="true"
          class="left-illustration"
        />
      </div>

      <div class="right-section">
        <div class="language-selector">
          <LanguageSwitcher variant="auth" />
        </div>

        <div class="form-shell">
          <div class="form-container">
            <h2 class="form-title">{{ t('pages.forgotPassword.title') }}</h2>
            <p class="form-subtitle">{{ t('pages.forgotPassword.subtitle') }}</p>

            <el-form
              ref="forgotFormRef"
              :model="forgotForm"
              :rules="forgotRules"
              class="forgot-form"
            >
              <div class="form-item">
                <label class="form-label">{{ t('common.email') }}</label>
                <el-form-item prop="email">
                  <el-input
                    v-model="forgotForm.email"
                    :placeholder="t('auth.login.emailPlaceholder')"
                    size="large"
                    :prefix-icon="Message"
                  />
                </el-form-item>
              </div>

              <div class="form-item">
                <label class="form-label">{{ t('common.verificationCode') }}</label>
                <el-form-item prop="verificationCode">
                  <div class="verification-code-field">
                    <el-input
                      v-model="forgotForm.verificationCode"
                      class="verification-code-input"
                      :placeholder="t('auth.login.codePlaceholder')"
                      size="large"
                      :prefix-icon="Key"
                    />
                    <el-button
                      class="send-code-button"
                      :disabled="countdown > 0"
                      @click="sendVerificationCode"
                    >
                      {{ countdown > 0 ? `${countdown}s` : t('auth.actions.sendCode') }}
                    </el-button>
                  </div>
                </el-form-item>
              </div>

              <div class="form-item">
                <label class="form-label">{{ t('pages.forgotPassword.newPasswordLabel') }}</label>
                <el-form-item prop="newPassword">
                  <el-input
                    v-model="forgotForm.newPassword"
                    type="password"
                    :placeholder="t('pages.forgotPassword.newPasswordPlaceholder')"
                    size="large"
                    :prefix-icon="Lock"
                    show-password
                  />
                </el-form-item>
              </div>

              <div class="form-item">
                <label class="form-label">{{ t('pages.forgotPassword.confirmPasswordLabel') }}</label>
                <el-form-item prop="confirmPassword">
                  <el-input
                    v-model="forgotForm.confirmPassword"
                    type="password"
                    :placeholder="t('pages.forgotPassword.confirmPasswordPlaceholder')"
                    size="large"
                    :prefix-icon="Lock"
                    show-password
                  />
                </el-form-item>
              </div>

              <el-button
                type="primary"
                size="large"
                class="submit-button"
                :loading="loading"
                @click="handleResetPassword"
              >
                {{ t('pages.forgotPassword.resetAction') }}
              </el-button>
            </el-form>

            <div class="form-footer">
              <div class="back-link">
                {{ t('pages.forgotPassword.backToLoginPrefix') }}
                <el-link type="primary" :underline="false" @click="goToLogin">
                  {{ t('pages.forgotPassword.backToLoginAction') }}
                </el-link>
              </div>
              <div class="support-link-row">
                <el-link type="primary" :underline="false" @click="goToTechnicalSupport">
                  {{ t('common.supportSite') }}
                </el-link>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Message, Lock, Key } from '@element-plus/icons-vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useI18n } from 'vue-i18n'
import {
  sendVerificationCode as sendVerificationCodeAPI,
  resetPassword as resetPasswordAPI,
} from '@/api/auth'
import LanguageSwitcher from '@/components/LanguageSwitcher.vue'
import forgotPasswordIllustration from '@/assets/auth/forgot-password-illustration.svg'

const router = useRouter()
const { t } = useI18n()

const countdown = ref(0)
const loading = ref(false)
const forgotFormRef = ref<FormInstance>()

const forgotForm = reactive({
  email: '',
  verificationCode: '',
  newPassword: '',
  confirmPassword: '',
})

const validateConfirmPassword = (rule: any, value: any, callback: any) => {
  if (value === '') {
    callback(new Error(t('pages.forgotPassword.validation.confirmPasswordRequired')))
  } else if (value !== forgotForm.newPassword) {
    callback(new Error(t('pages.forgotPassword.validation.confirmPasswordMismatch')))
  } else {
    callback()
  }
}

const forgotRules = computed<FormRules>(() => ({
  email: [
    { required: true, message: t('auth.login.validation.emailRequired'), trigger: 'blur' },
    { type: 'email', message: t('auth.login.validation.emailInvalid'), trigger: 'blur' },
  ],
  verificationCode: [
    { required: true, message: t('auth.login.validation.codeRequired'), trigger: 'blur' },
    { len: 6, message: t('auth.login.validation.codeLength'), trigger: 'blur' },
  ],
  newPassword: [
    {
      required: true,
      message: t('pages.forgotPassword.validation.newPasswordRequired'),
      trigger: 'blur',
    },
    {
      min: 6,
      max: 20,
      message: t('pages.forgotPassword.validation.newPasswordLength'),
      trigger: 'blur',
    },
  ],
  confirmPassword: [
    {
      required: true,
      message: t('pages.forgotPassword.validation.confirmPasswordRequired'),
      trigger: 'blur',
    },
    { validator: validateConfirmPassword, trigger: 'blur' },
  ],
}))

const sendVerificationCode = async () => {
  try {
    await forgotFormRef.value?.validateField('email')
  } catch {
    return
  }

  try {
    await sendVerificationCodeAPI({
      email: forgotForm.email,
      type: 'reset_password',
    })

    ElMessage.success(t('auth.login.codeSent'))

    countdown.value = 60
    const timer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) {
        clearInterval(timer)
      }
    }, 1000)
  } catch (error: any) {
    const message =
      error.response?.data?.message || error.message || t('pages.forgotPassword.sendCodeFailed')
    ElMessage.error(message)
  }
}

const handleResetPassword = async () => {
  try {
    await forgotFormRef.value?.validate()

    loading.value = true

    await resetPasswordAPI({
      email: forgotForm.email,
      verificationCode: forgotForm.verificationCode,
      newPassword: forgotForm.newPassword,
    })

    ElMessage.success(t('pages.forgotPassword.resetSuccess'))
    router.push('/login')
  } catch (error: any) {
    const message =
      error.response?.data?.message || error.message || t('pages.forgotPassword.resetFailed')
    ElMessage.error(message)
  } finally {
    loading.value = false
  }
}

const goToLogin = () => {
  router.push('/login')
}

const goToTechnicalSupport = () => {
  router.push('/legal/support')
}
</script>

<style scoped>
.forgot-password-container {
  width: 100%;
  min-height: 100vh;
  display: flex;
  background: #fff;
}

.forgot-password-layout {
  width: 100%;
  min-height: 100vh;
  display: grid;
  grid-template-columns: minmax(0, 55.8571428571%) minmax(0, 44.1428571429%);
}

.left-section {
  background: #eef3ff;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: clamp(44px, 5vh, 72px) clamp(28px, 3.2vw, 48px) clamp(34px, 4vh, 56px);
  overflow: hidden;
}

.left-illustration {
  width: clamp(620px, 46vw, 760px);
  max-width: 100%;
  height: auto;
  display: block;
  transform: translate(clamp(-12px, -0.8vw, 0px), clamp(0px, 0.6vh, 6px));
}

.right-section {
  background: #fff;
  display: flex;
  flex-direction: column;
  padding:
    clamp(18px, 2.5vh, 24px)
    clamp(40px, 4.8vw, 96px)
    clamp(24px, 3.8vh, 40px)
    clamp(44px, 5vw, 104px);
  overflow-y: auto;
}

.language-selector {
  display: flex;
  justify-content: flex-end;
}

.language-selector :deep(.language-trigger--auth) {
  color: #7b7b7b;
  font-size: 14px;
  line-height: 22px;
  gap: 8px;
}

.language-selector :deep(.language-trigger--auth .el-icon) {
  font-size: 14px;
}

.form-shell {
  flex: 1;
  display: flex;
  justify-content: center;
  min-height: 0;
  padding-top: clamp(12px, 2.2vh, 24px);
}

.form-container {
  width: 100%;
  max-width: clamp(465px, 74%, 560px);
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.form-title {
  margin: 0;
  color: #597ef7;
  font-size: 36px;
  font-weight: 600;
  line-height: 1.08;
}

.form-subtitle {
  margin: 14px 0 0;
  color: #ababab;
  font-size: 14px;
  line-height: 1.5;
}

.forgot-form {
  margin-top: clamp(20px, 2.8vh, 30px);
  display: flex;
  flex-direction: column;
}

.form-item {
  margin-bottom: 24px;
}

.form-label {
  display: block;
  margin-bottom: 11px;
  color: #111827;
  font-size: 14px;
  font-weight: 600;
  line-height: 1.2;
}

.submit-button {
  width: 314px;
  max-width: 100%;
  height: 32px;
  margin: clamp(24px, 3vh, 34px) auto 0;
  border-radius: 2px;
  border-color: #597ef7;
  background: #597ef7;
  font-size: 14px;
  font-weight: 600;
  line-height: 1;
  letter-spacing: 0;
  box-shadow: none;
}

.submit-button:hover,
.submit-button:focus-visible,
.submit-button:active {
  border-color: #597ef7;
  background: #597ef7;
}

.form-footer {
  margin-top: auto;
  padding-top: clamp(18px, 2.4vh, 24px);
  padding-bottom: clamp(44px, 7vh, 96px);
}

.back-link {
  color: #4b5563;
  text-align: center;
  font-size: 14px;
  line-height: 1.6;
}

.support-link-row {
  margin-top: 14px;
  text-align: center;
}

.forgot-form :deep(.el-form-item) {
  margin-bottom: 0;
}

.forgot-form :deep(.el-form-item__content) {
  line-height: 1;
}

.forgot-form :deep(.el-input__wrapper) {
  min-height: 37px;
  padding: 0 12px;
  border-radius: 3px;
  background: #fff;
  box-shadow: inset 0 0 0 1px #d9dee8;
}

.forgot-form :deep(.el-input__wrapper:hover) {
  box-shadow: inset 0 0 0 1px #597ef7;
}

.forgot-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: inset 0 0 0 1px #597ef7;
}

.forgot-form :deep(.el-input__inner) {
  height: 100%;
  font-size: 13px;
  color: #111827;
}

.forgot-form :deep(.el-input__inner::placeholder) {
  color: #b7bdc9;
}

.forgot-form :deep(.el-input__inner:-webkit-autofill),
.forgot-form :deep(.el-input__inner:-webkit-autofill:hover),
.forgot-form :deep(.el-input__inner:-webkit-autofill:focus),
.forgot-form :deep(.el-input__inner:-webkit-autofill:active) {
  -webkit-text-fill-color: #111827;
  caret-color: #111827;
  background-color: transparent;
  -webkit-background-clip: text;
  transition: background-color 9999s ease-out 0s;
}

.forgot-form :deep(.el-input__prefix-inner),
.forgot-form :deep(.el-input__suffix-inner) {
  color: #4b5563;
  font-size: 14px;
}

.forgot-form :deep(.el-input__prefix-inner) {
  margin-right: 12px;
}

.forgot-form :deep(.el-input__suffix-inner) {
  margin-left: 12px;
}

.verification-code-field {
  position: relative;
  width: 100%;
}

.verification-code-field :deep(.verification-code-input) {
  width: 100%;
}

.verification-code-field :deep(.verification-code-input .el-input__wrapper) {
  padding-right: 175px;
}

.verification-code-field :deep(.send-code-button) {
  position: absolute;
  top: 1px;
  right: 35px;
  z-index: 2;
  width: auto;
  min-width: 84px;
  height: 35px;
  min-height: 35px;
  padding: 0 5px;
  border: 0;
  border-radius: 0;
  background: #f1f1f1;
  color: #b3b3b3;
  display: inline-flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 500;
  white-space: nowrap;
  opacity: 1;
  box-shadow: none;
}

.verification-code-field :deep(.send-code-button:hover),
.verification-code-field :deep(.send-code-button:focus-visible),
.verification-code-field :deep(.send-code-button:active),
.verification-code-field :deep(.send-code-button.is-disabled),
.verification-code-field :deep(.send-code-button.is-disabled:hover),
.verification-code-field :deep(.send-code-button.is-disabled:focus-visible),
.verification-code-field :deep(.send-code-button.is-disabled:active) {
  background: #f1f1f1;
  color: #b3b3b3;
  opacity: 1;
}

.forgot-form :deep(.el-link),
.back-link :deep(.el-link),
.support-link-row :deep(.el-link) {
  color: #597ef7;
}

.back-link :deep(.el-link),
.support-link-row :deep(.el-link) {
  font-weight: 600;
}

@media (max-width: 1100px) {
  .forgot-password-layout {
    grid-template-columns: 1fr;
  }

  .left-section {
    display: none;
  }

  .right-section {
    padding: 18px clamp(24px, 8vw, 72px) 34px;
  }

  .form-shell {
    padding-top: 24px;
  }

  .form-container {
    margin: 0 auto;
  }
}

@media (max-width: 640px) {
  .right-section {
    padding: 18px 20px 28px;
  }

  .form-shell {
    padding-top: 20px;
  }

  .form-title {
    font-size: 32px;
  }

  .forgot-form {
    margin-top: 24px;
  }

  .submit-button {
    width: 100%;
    margin-top: 30px;
  }

  .form-footer {
    padding-top: 20px;
  }
}
</style>
