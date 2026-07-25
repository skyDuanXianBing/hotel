<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { CopyDocument } from '@element-plus/icons-vue'
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
    ? `已配置（尾号 ${settings.value.secretKeyLast4 ?? '****'}），留空保持不变`
    : '粘贴 Stripe Secret key（sk_...）',
)

const webhookSecretPlaceholder = computed(() =>
  settings.value?.webhookSecretConfigured
    ? `已配置（尾号 ${settings.value.webhookSecretLast4 ?? '****'}），留空保持不变`
    : '粘贴 Webhook Signing secret（whsec_...）',
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
      throw new Error(response.message || '加载 Stripe 设置失败')
    }
    settings.value = response.data
    // sk/whsec 明文不回传，只回显 pk 与已配置状态
    form.publishableKey = response.data.publishableKey || ''
  } catch (error) {
    settings.value = null
    ElMessage.error(getErrorMessage(error, '加载 Stripe 设置失败'))
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
    ElMessage.success('Webhook URL 已复制')
  } catch {
    ElMessage.error('复制失败，请手动复制')
  }
}

// 轻校验仅提示不拦截，格式终验在服务端
const PREFIX_HINTS: Array<{ value: () => string; prefix: string; label: string }> = [
  { value: () => form.publishableKey, prefix: 'pk_', label: 'Publishable Key' },
  { value: () => form.secretKey, prefix: 'sk_', label: 'Secret Key' },
  { value: () => form.webhookSecret, prefix: 'whsec_', label: 'Webhook Secret' },
]

const warnInvalidPrefixes = () => {
  const invalidLabels = PREFIX_HINTS.filter(
    (item) => item.value().trim() && !item.value().trim().startsWith(item.prefix),
  ).map((item) => `${item.label}（应以 ${item.prefix} 开头）`)
  if (invalidLabels.length > 0) {
    ElMessage.warning(`${invalidLabels.join('、')} 格式可能不正确，仍以服务端校验结果为准`)
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
    ElMessage.info('没有需要保存的变更，留空表示保持不变')
    return
  }

  warnInvalidPrefixes()
  saving.value = true
  try {
    const response = await updateIndependentSiteStripeSettings(payload)
    if (!response.success || !response.data) {
      throw new Error(response.message || '保存 Stripe 设置失败')
    }
    settings.value = response.data
    form.publishableKey = response.data.publishableKey || ''
    form.secretKey = ''
    form.webhookSecret = ''
    ElMessage.success('Stripe 设置已保存')
    emit('saved')
    emit('update:modelValue', false)
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '保存 Stripe 设置失败'))
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    title="Stripe 设置"
    width="640px"
    :close-on-click-modal="!saving"
    @update:model-value="$emit('update:modelValue', $event)"
  >
    <div v-loading="loading" class="stripe-settings">
      <div class="status-row">
        <el-tag :type="settings?.configured ? 'success' : 'info'" effect="plain">
          {{ settings?.configured ? '门店密钥已配齐' : '门店密钥未配齐' }}
        </el-tag>
        <span class="status-hint">
          密钥按门店保存并加密存储，该门店所有独立站共享；保存后不再回显明文。
        </span>
      </div>

      <el-form label-position="top" :disabled="saving">
        <el-form-item label="Publishable Key">
          <el-input
            v-model="form.publishableKey"
            autocomplete="off"
            placeholder="粘贴 Stripe Publishable key（pk_...）"
          />
        </el-form-item>
        <el-form-item label="Secret Key">
          <el-input
            v-model="form.secretKey"
            type="password"
            show-password
            autocomplete="new-password"
            :placeholder="secretKeyPlaceholder"
          />
        </el-form-item>
        <el-form-item label="Webhook Secret">
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
        <span class="webhook-label">Webhook URL</span>
        <div class="webhook-url-row">
          <el-input :model-value="webhookUrl" readonly>
            <template #append>
              <el-button :icon="CopyDocument" @click="copyWebhookUrl">复制</el-button>
            </template>
          </el-input>
        </div>
      </div>

      <div class="guide-block">
        <span class="guide-title">配置指引</span>
        <ol class="guide-list">
          <li>打开 Stripe Dashboard → Developers → Webhooks → Add endpoint，填写上方 Webhook URL。</li>
          <li>订阅事件 <code>payment_intent.succeeded</code> 与 <code>payment_intent.payment_failed</code>。</li>
          <li>把该 endpoint 的 Signing secret（<code>whsec_...</code>）填回上方「Webhook Secret」。</li>
          <li>测试卡号 <code>4242 4242 4242 4242</code>，有效期填任意未来日期，CVC 任意填写。</li>
        </ol>
      </div>
    </div>
    <template #footer>
      <el-button :disabled="saving" @click="$emit('update:modelValue', false)">取消</el-button>
      <el-button type="primary" :loading="saving" :disabled="loading" @click="handleSave">
        保存
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
