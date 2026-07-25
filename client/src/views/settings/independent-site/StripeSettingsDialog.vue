<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { CopyDocument } from '@element-plus/icons-vue'
import { useI18n } from 'vue-i18n'
import {
  getIndependentSiteStripeSettings,
  updateIndependentSiteStripeSettings,
} from '@/api/independentSite'
import type {
  IndependentSiteStripeSettings,
  IndependentSiteStripeSettingsUpdateRequest,
} from '@/types/independentSite'

const props = defineProps<{
  modelValue: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  saved: []
}>()

const { t } = useI18n()

const loading = ref(false)
const saving = ref(false)
const settings = ref<IndependentSiteStripeSettings | null>(null)

const form = reactive({
  publishableKey: '',
  secretKey: '',
  webhookSecret: '',
})

// 公开 webhook 端点固定，客人支付结果由 Stripe 推送到这里
const webhookUrl = `${window.location.origin}/api/public/independent-sites/stripe/webhook`

const secretKeyPlaceholder = computed(() =>
  settings.value?.secretKeyConfigured
    ? t('independentSite.stripe.configuredPlaceholder', {
        last4: settings.value.secretKeyLast4 ?? '****',
      })
    : t('independentSite.stripe.secretKeyPlaceholder'),
)

const webhookSecretPlaceholder = computed(() =>
  settings.value?.webhookSecretConfigured
    ? t('independentSite.stripe.configuredPlaceholder', {
        last4: settings.value.webhookSecretLast4 ?? '****',
      })
    : t('independentSite.stripe.webhookSecretPlaceholder'),
)

const getErrorMessage = (error: unknown, fallback: string) => {
  if (error && typeof error === 'object') {
    const responseMessage = (error as { response?: { data?: { message?: unknown } } }).response
      ?.data?.message
    if (typeof responseMessage === 'string' && responseMessage.trim()) {
      return responseMessage
    }
  }
  if (error instanceof Error && error.message) {
    return error.message
  }
  return fallback
}

const loadSettings = async () => {
  loading.value = true
  form.publishableKey = ''
  form.secretKey = ''
  form.webhookSecret = ''
  try {
    const response = await getIndependentSiteStripeSettings()
    if (!response.success || !response.data) {
      throw new Error(response.message || t('independentSite.stripe.loadFailed'))
    }
    settings.value = response.data
    // sk/whsec 明文不回传，只回显 pk 与已配置状态
    form.publishableKey = response.data.publishableKey || ''
  } catch (error) {
    settings.value = null
    ElMessage.error(getErrorMessage(error, t('independentSite.stripe.loadFailed')))
  } finally {
    loading.value = false
  }
}

watch(
  () => props.modelValue,
  (visible) => {
    if (visible) {
      void loadSettings()
    }
  },
)

const copyWebhookUrl = async () => {
  try {
    await navigator.clipboard.writeText(webhookUrl)
    ElMessage.success(t('independentSite.stripe.copied'))
  } catch {
    ElMessage.error(t('independentSite.stripe.copyFailed'))
  }
}

// 轻校验仅提示不拦截，格式终验在服务端
const prefixHints = (): Array<{ value: () => string; prefix: string; label: string }> => [
  {
    value: () => form.publishableKey,
    prefix: 'pk_',
    label: t('independentSite.stripe.publishableKey'),
  },
  { value: () => form.secretKey, prefix: 'sk_', label: t('independentSite.stripe.secretKey') },
  {
    value: () => form.webhookSecret,
    prefix: 'whsec_',
    label: t('independentSite.stripe.webhookSecret'),
  },
]

const warnInvalidPrefixes = () => {
  const invalidLabels = prefixHints()
    .filter(
    (item) => item.value().trim() && !item.value().trim().startsWith(item.prefix),
    )
    .map((item) => t('independentSite.stripe.prefixHint', item))
  if (invalidLabels.length > 0) {
    ElMessage.warning(
      t('independentSite.stripe.invalidPrefixWarning', { labels: invalidLabels.join('、') }),
    )
  }
}

const handleSave = async () => {
  const payload: IndependentSiteStripeSettingsUpdateRequest = {}
  const publishableKey = form.publishableKey.trim()
  const secretKey = form.secretKey.trim()
  const webhookSecret = form.webhookSecret.trim()
  if (publishableKey) {
    payload.publishableKey = publishableKey
  }
  if (secretKey) {
    payload.secretKey = secretKey
  }
  if (webhookSecret) {
    payload.webhookSecret = webhookSecret
  }
  if (Object.keys(payload).length === 0) {
    ElMessage.info(t('independentSite.stripe.noChanges'))
    return
  }

  warnInvalidPrefixes()
  saving.value = true
  try {
    const response = await updateIndependentSiteStripeSettings(payload)
    if (!response.success || !response.data) {
      throw new Error(response.message || t('independentSite.stripe.saveFailed'))
    }
    settings.value = response.data
    form.publishableKey = response.data.publishableKey || ''
    form.secretKey = ''
    form.webhookSecret = ''
    ElMessage.success(t('independentSite.stripe.saved'))
    emit('saved')
    emit('update:modelValue', false)
  } catch (error) {
    ElMessage.error(getErrorMessage(error, t('independentSite.stripe.saveFailed')))
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    :title="t('independentSite.stripe.title')"
    width="640px"
    :close-on-click-modal="!saving"
    @update:model-value="$emit('update:modelValue', $event)"
  >
    <div v-loading="loading" class="stripe-settings">
      <div class="status-row">
        <el-tag :type="settings?.configured ? 'success' : 'info'" effect="plain">
          {{
            settings?.configured
              ? t('independentSite.stripe.configured')
              : t('independentSite.stripe.notConfigured')
          }}
        </el-tag>
        <span class="status-hint">
          {{ t('independentSite.stripe.statusHint') }}
        </span>
      </div>

      <el-form label-position="top" :disabled="saving">
        <el-form-item :label="t('independentSite.stripe.publishableKey')">
          <el-input
            v-model="form.publishableKey"
            autocomplete="off"
            :placeholder="t('independentSite.stripe.publishableKeyPlaceholder')"
          />
        </el-form-item>
        <el-form-item :label="t('independentSite.stripe.secretKey')">
          <el-input
            v-model="form.secretKey"
            type="password"
            show-password
            autocomplete="new-password"
            :placeholder="secretKeyPlaceholder"
          />
        </el-form-item>
        <el-form-item :label="t('independentSite.stripe.webhookSecret')">
          <el-input
            v-model="form.webhookSecret"
            type="password"
            show-password
            autocomplete="new-password"
            :placeholder="webhookSecretPlaceholder"
          />
        </el-form-item>
      </el-form>

      <div class="webhook-block">
        <span class="webhook-label">{{ t('independentSite.stripe.webhookUrl') }}</span>
        <div class="webhook-url-row">
          <el-input :model-value="webhookUrl" readonly>
            <template #append>
              <el-button :icon="CopyDocument" @click="copyWebhookUrl">
                {{ t('independentSite.common.copy') }}
              </el-button>
            </template>
          </el-input>
        </div>
      </div>

      <div class="guide-block">
        <span class="guide-title">{{ t('independentSite.stripe.guideTitle') }}</span>
        <ol class="guide-list">
          <li>{{ t('independentSite.stripe.guideStepOne') }}</li>
          <li>{{ t('independentSite.stripe.guideStepTwo') }}</li>
          <li>{{ t('independentSite.stripe.guideStepThree') }}</li>
          <li>{{ t('independentSite.stripe.guideStepFour') }}</li>
        </ol>
      </div>
    </div>
    <template #footer>
      <el-button :disabled="saving" @click="$emit('update:modelValue', false)">
        {{ t('independentSite.common.cancel') }}
      </el-button>
      <el-button type="primary" :loading="saving" :disabled="loading" @click="handleSave">
        {{ t('independentSite.common.save') }}
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.stripe-settings {
  display: grid;
  gap: 4px;
}

.status-row {
  display: flex;
  gap: 10px;
  align-items: center;
  margin-bottom: 12px;
}

.status-hint {
  color: #929896;
  font-size: 12px;
  line-height: 1.5;
}

.webhook-block {
  margin-bottom: 4px;
}

.webhook-label,
.guide-title {
  display: block;
  margin-bottom: 6px;
  color: #606266;
  font-size: 14px;
}

.guide-block {
  padding: 10px 14px;
  border: 1px solid #e5e9e7;
  border-radius: 8px;
  background: #f7f8f7;
}

.guide-list {
  margin: 0;
  padding-left: 18px;
  color: #69716f;
  font-size: 12px;
  line-height: 1.8;
}

.guide-list code {
  padding: 1px 4px;
  border-radius: 4px;
  background: #ecefef;
  font-family: 'SFMono-Regular', Consolas, monospace;
  font-size: 11px;
}
</style>
