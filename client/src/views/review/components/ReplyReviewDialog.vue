<script setup lang="ts">
import { computed, nextTick, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { WarningFilled } from '@element-plus/icons-vue'
import { useI18n } from 'vue-i18n'
import { REVIEW_ALLOWED_ACTION, replyToReview, type SuReviewDetailDTO } from '@/api/suReviews'
import {
  getReviewErrorMessage,
  hasAllowedReviewAction,
  normalizeChannelCode,
} from '@/views/review/reviewPresentation'

const props = defineProps<{
  modelValue: boolean
  review: SuReviewDetailDTO | null
}>()

const emit = defineEmits<{
  (event: 'update:modelValue', value: boolean): void
  (event: 'submitted'): void
}>()

const { t, te } = useI18n()
const formRef = ref<FormInstance>()
const submitting = ref(false)
const idempotencyKey = ref('')
const form = reactive({
  reviewReply: '',
})

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value),
})

const channelLabel = computed(() => {
  const code = normalizeChannelCode(props.review?.channelCode)
  const key = `suReviews.channels.${code}`
  return te(key) ? t(key) : props.review?.channelCode || t('suReviews.labels.notProvided')
})

const actionUnavailableReason = computed(
  () => props.review?.actionReasons?.[REVIEW_ALLOWED_ACTION.REPLY] || '',
)

const rules = computed<FormRules>(() => ({
  reviewReply: [
    {
      validator: (_rule, value: string, callback) => {
        const content = String(value || '').trim()
        if (!content) {
          callback(new Error(t('suReviews.validation.replyRequired')))
          return
        }
        if (content.length > 5000) {
          callback(new Error(t('suReviews.validation.replyTooLong')))
          return
        }
        callback()
      },
      trigger: ['blur', 'change'],
    },
  ],
}))

const createIdempotencyKey = () => {
  const uuid =
    typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function'
      ? crypto.randomUUID()
      : `${Date.now()}-${Math.random().toString(36).slice(2)}`
  return `review-reply-${props.review?.id || 'unknown'}-${uuid}`
}

const reset = () => {
  form.reviewReply = ''
  idempotencyKey.value = ''
  nextTick(() => formRef.value?.clearValidate())
}

watch(
  () => [props.modelValue, props.review?.id] as const,
  ([open]) => {
    if (open) {
      reset()
    }
  },
)

watch(
  () => form.reviewReply,
  () => {
    if (!submitting.value) {
      idempotencyKey.value = ''
    }
  },
)

const handleSubmit = async () => {
  if (!props.review || !hasAllowedReviewAction(props.review, REVIEW_ALLOWED_ACTION.REPLY)) {
    ElMessage.error(actionUnavailableReason.value || t('suReviews.eligibility.none'))
    return
  }

  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }

  try {
    await ElMessageBox.confirm(
      t('suReviews.replyDialog.confirmMessage'),
      t('suReviews.replyDialog.confirmTitle'),
      {
        confirmButtonText: t('suReviews.actions.confirmSubmit'),
        cancelButtonText: t('suReviews.actions.cancel'),
        type: 'warning',
        confirmButtonClass: 'review-confirm-button',
      },
    )
  } catch {
    return
  }

  if (!idempotencyKey.value) {
    idempotencyKey.value = createIdempotencyKey()
  }

  submitting.value = true
  try {
    const response = await replyToReview(props.review.id, {
      reviewReply: form.reviewReply.trim(),
      idempotencyKey: idempotencyKey.value,
    })
    if (!response.success) {
      throw new Error(response.message || t('suReviews.errors.action'))
    }

    ElMessage.success(response.message || t('suReviews.replyDialog.success'))
    visible.value = false
    emit('submitted')
  } catch (error) {
    ElMessage.error(getReviewErrorMessage(error, t('suReviews.errors.action')))
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <el-dialog
    v-model="visible"
    class="review-action-dialog"
    width="min(620px, calc(100vw - 32px))"
    :close-on-click-modal="!submitting"
    :close-on-press-escape="!submitting"
    :show-close="!submitting"
    destroy-on-close
  >
    <template #header>
      <div class="dialog-heading">
        <span class="dialog-eyebrow">{{ channelLabel }}</span>
        <h2>{{ t('suReviews.replyDialog.title') }}</h2>
        <p>{{ t('suReviews.replyDialog.description', { channel: channelLabel }) }}</p>
      </div>
    </template>

    <el-alert
      :title="t('suReviews.replyDialog.publicImpact')"
      type="warning"
      :closable="false"
      show-icon
      class="impact-alert"
    >
      <template #icon>
        <el-icon><WarningFilled /></el-icon>
      </template>
    </el-alert>

    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-position="top"
      @submit.prevent="handleSubmit"
    >
      <el-form-item :label="t('suReviews.replyDialog.label')" prop="reviewReply">
        <el-input
          v-model="form.reviewReply"
          type="textarea"
          :rows="8"
          maxlength="5000"
          show-word-limit
          resize="vertical"
          :placeholder="t('suReviews.replyDialog.placeholder')"
          :disabled="submitting"
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="dialog-footer">
        <el-button :disabled="submitting" @click="visible = false">
          {{ t('suReviews.actions.cancel') }}
        </el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          {{ t('suReviews.actions.submit') }}
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<style scoped>
.dialog-heading {
  display: grid;
  gap: 5px;
  padding-right: 36px;
}

.dialog-eyebrow {
  color: #2f7cf6;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.dialog-heading h2 {
  margin: 0;
  color: #1d211e;
  font-size: 22px;
  line-height: 1.25;
}

.dialog-heading p {
  margin: 0;
  color: #747a75;
  font-size: 13px;
  line-height: 1.55;
}

.impact-alert {
  margin-bottom: 20px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

:deep(.el-dialog__body) {
  padding-top: 8px;
}

:deep(.el-textarea__inner) {
  line-height: 1.65;
}
</style>
