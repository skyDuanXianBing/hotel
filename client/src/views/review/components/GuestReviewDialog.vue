<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { WarningFilled } from '@element-plus/icons-vue'
import { useI18n } from 'vue-i18n'
import {
  REVIEW_ALLOWED_ACTION,
  reviewGuest,
  type GuestReviewCategory,
  type GuestReviewCategoryInput,
  type SuReviewDetailDTO,
} from '@/api/suReviews'
import {
  getReviewErrorMessage,
  hasAllowedReviewAction,
  isAirbnbReview,
} from '@/views/review/reviewPresentation'

interface CategoryForm {
  category: GuestReviewCategory
  rating: number
  comment: string
  reviewCategoryTags: string[]
}

interface GuestReviewForm {
  isRevieweeRecommended: boolean | null
  publicReview: string
  privateFeedback: string
  categories: CategoryForm[]
}

const props = defineProps<{
  modelValue: boolean
  review: SuReviewDetailDTO | null
}>()

const emit = defineEmits<{
  (event: 'update:modelValue', value: boolean): void
  (event: 'submitted'): void
}>()

const { t } = useI18n()
const submitting = ref(false)
const idempotencyKey = ref('')
const validationErrors = reactive({
  recommendation: '',
  ratings: '',
  publicReview: '',
  privateFeedback: '',
  categoryComments: {} as Record<GuestReviewCategory, string>,
})

const createCategoryForms = (): CategoryForm[] => [
  {
    category: 'cleanliness',
    rating: 0,
    comment: '',
    reviewCategoryTags: [],
  },
  {
    category: 'communication',
    rating: 0,
    comment: '',
    reviewCategoryTags: [],
  },
  {
    category: 'respect_house_rules',
    rating: 0,
    comment: '',
    reviewCategoryTags: [],
  },
]

const form = reactive<GuestReviewForm>({
  isRevieweeRecommended: null,
  publicReview: '',
  privateFeedback: '',
  categories: createCategoryForms(),
})

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value),
})

const actionUnavailableReason = computed(
  () => props.review?.actionReasons?.[REVIEW_ALLOWED_ACTION.GUEST_REVIEW] || '',
)

const requiresLowScoreContext = (category: CategoryForm) => {
  return category.rating > 0 && category.rating < 5
}

const clearValidation = () => {
  validationErrors.recommendation = ''
  validationErrors.ratings = ''
  validationErrors.publicReview = ''
  validationErrors.privateFeedback = ''
  validationErrors.categoryComments = {} as Record<GuestReviewCategory, string>
}

const reset = () => {
  form.isRevieweeRecommended = null
  form.publicReview = ''
  form.privateFeedback = ''
  form.categories = createCategoryForms()
  idempotencyKey.value = ''
  clearValidation()
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
  () => ({
    recommended: form.isRevieweeRecommended,
    publicReview: form.publicReview,
    privateFeedback: form.privateFeedback,
    categories: form.categories.map((category) => ({
      rating: category.rating,
      comment: category.comment,
      tags: [...category.reviewCategoryTags],
    })),
  }),
  () => {
    if (!submitting.value) {
      idempotencyKey.value = ''
    }
  },
  { deep: true },
)

const validate = () => {
  clearValidation()
  let valid = true

  if (form.isRevieweeRecommended === null) {
    validationErrors.recommendation = t('suReviews.validation.recommendationRequired')
    valid = false
  }

  if (form.categories.some((item) => item.rating < 1 || item.rating > 5)) {
    validationErrors.ratings = t('suReviews.validation.ratingsRequired')
    valid = false
  }

  const publicReview = form.publicReview.trim()
  if (!publicReview) {
    validationErrors.publicReview = t('suReviews.validation.publicReviewRequired')
    valid = false
  } else if (publicReview.length > 999) {
    validationErrors.publicReview = t('suReviews.validation.publicReviewTooLong')
    valid = false
  }

  if (form.privateFeedback.trim().length > 999) {
    validationErrors.privateFeedback = t('suReviews.validation.privateFeedbackTooLong')
    valid = false
  }

  form.categories.forEach((category) => {
    const comment = category.comment.trim()
    if (requiresLowScoreContext(category) && !comment) {
      validationErrors.categoryComments[category.category] = t(
        'suReviews.validation.lowScoreCommentRequired',
      )
      valid = false
      return
    }
    if (comment.length > 50) {
      validationErrors.categoryComments[category.category] = t(
        'suReviews.validation.lowScoreCommentTooLong',
      )
      valid = false
    }
  })

  return valid
}

const createIdempotencyKey = () => {
  const uuid =
    typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function'
      ? crypto.randomUUID()
      : `${Date.now()}-${Math.random().toString(36).slice(2)}`
  return `guest-review-${props.review?.id || 'unknown'}-${uuid}`
}

const buildCategoryRatings = (): GuestReviewCategoryInput[] =>
  form.categories.map((category) => {
    const result: GuestReviewCategoryInput = {
      category: category.category,
      rating: category.rating,
    }
    const comment = category.comment.trim()
    if (comment) {
      result.comment = comment
    }
    if (category.reviewCategoryTags.length) {
      result.reviewCategoryTags = [...category.reviewCategoryTags]
    }
    return result
  })

const handleSubmit = async () => {
  if (
    !props.review ||
    !isAirbnbReview(props.review) ||
    !hasAllowedReviewAction(props.review, REVIEW_ALLOWED_ACTION.GUEST_REVIEW)
  ) {
    ElMessage.error(actionUnavailableReason.value || t('suReviews.eligibility.none'))
    return
  }

  if (!validate() || form.isRevieweeRecommended === null) {
    return
  }

  try {
    await ElMessageBox.confirm(
      t('suReviews.guestDialog.confirmMessage'),
      t('suReviews.guestDialog.confirmTitle'),
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
    const privateFeedback = form.privateFeedback.trim()
    const response = await reviewGuest(props.review.id, {
      idempotencyKey: idempotencyKey.value,
      confirmed: true,
      isRevieweeRecommended: form.isRevieweeRecommended,
      publicReview: form.publicReview.trim(),
      ...(privateFeedback ? { privateFeedback } : {}),
      categoryRatings: buildCategoryRatings(),
    })

    if (!response.success) {
      throw new Error(response.message || t('suReviews.errors.action'))
    }

    ElMessage.success(response.message || t('suReviews.guestDialog.success'))
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
    class="guest-review-dialog"
    width="min(760px, calc(100vw - 32px))"
    :close-on-click-modal="!submitting"
    :close-on-press-escape="!submitting"
    :show-close="!submitting"
    destroy-on-close
  >
    <template #header>
      <div class="dialog-heading">
        <span class="airbnb-mark">Airbnb</span>
        <h2>{{ t('suReviews.guestDialog.title') }}</h2>
        <p>{{ t('suReviews.guestDialog.description') }}</p>
      </div>
    </template>

    <el-alert type="warning" :closable="false" show-icon class="impact-alert">
      <template #icon>
        <el-icon><WarningFilled /></el-icon>
      </template>
      <template #title>
        {{ t('suReviews.guestDialog.sideEffectTitle') }}
      </template>
      <p class="impact-copy">{{ t('suReviews.guestDialog.sideEffectDescription') }}</p>
    </el-alert>

    <div class="guest-review-form">
      <section class="form-section">
        <label class="section-label" id="guest-recommendation-label">
          {{ t('suReviews.guestDialog.recommendation') }}
        </label>
        <el-radio-group
          v-model="form.isRevieweeRecommended"
          :disabled="submitting"
          aria-labelledby="guest-recommendation-label"
        >
          <el-radio-button :value="true">
            {{ t('suReviews.guestDialog.recommend') }}
          </el-radio-button>
          <el-radio-button :value="false">
            {{ t('suReviews.guestDialog.notRecommend') }}
          </el-radio-button>
        </el-radio-group>
        <p v-if="validationErrors.recommendation" class="field-error" role="alert">
          {{ validationErrors.recommendation }}
        </p>
      </section>

      <section class="form-section">
        <h3>{{ t('suReviews.guestDialog.ratingSection') }}</h3>
        <p v-if="validationErrors.ratings" class="field-error" role="alert">
          {{ validationErrors.ratings }}
        </p>

        <div class="rating-grid">
          <article v-for="category in form.categories" :key="category.category" class="rating-card">
            <div class="rating-card-heading">
              <label :id="`rating-label-${category.category}`">
                {{ t(`suReviews.categories.${category.category}`) }}
              </label>
              <span v-if="category.rating" class="rating-value">{{ category.rating }}/5</span>
            </div>
            <el-rate
              v-model="category.rating"
              :disabled="submitting"
              :aria-labelledby="`rating-label-${category.category}`"
            />

            <div v-if="requiresLowScoreContext(category)" class="low-score-field">
              <label :for="`comment-${category.category}`">
                {{ t('suReviews.guestDialog.lowScoreComment') }}
              </label>
              <el-input
                :id="`comment-${category.category}`"
                v-model="category.comment"
                maxlength="50"
                show-word-limit
                :disabled="submitting"
                :placeholder="t('suReviews.guestDialog.lowScorePlaceholder')"
              />
              <p
                v-if="validationErrors.categoryComments[category.category]"
                class="field-error"
                role="alert"
              >
                {{ validationErrors.categoryComments[category.category] }}
              </p>
            </div>
          </article>
        </div>
      </section>

      <section class="form-section form-section--text">
        <label for="guest-public-review" class="section-label">
          {{ t('suReviews.guestDialog.publicReview') }}
        </label>
        <el-input
          id="guest-public-review"
          v-model="form.publicReview"
          type="textarea"
          :rows="5"
          maxlength="999"
          show-word-limit
          resize="vertical"
          :disabled="submitting"
          :placeholder="t('suReviews.guestDialog.publicPlaceholder')"
        />
        <p v-if="validationErrors.publicReview" class="field-error" role="alert">
          {{ validationErrors.publicReview }}
        </p>
      </section>

      <section class="form-section form-section--text">
        <label for="guest-private-feedback" class="section-label">
          {{ t('suReviews.guestDialog.privateFeedback') }}
        </label>
        <el-input
          id="guest-private-feedback"
          v-model="form.privateFeedback"
          type="textarea"
          :rows="4"
          maxlength="999"
          show-word-limit
          resize="vertical"
          :disabled="submitting"
          :placeholder="t('suReviews.guestDialog.privatePlaceholder')"
        />
        <p v-if="validationErrors.privateFeedback" class="field-error" role="alert">
          {{ validationErrors.privateFeedback }}
        </p>
      </section>
    </div>

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

.airbnb-mark {
  width: max-content;
  padding: 4px 9px;
  border-radius: 999px;
  background: #fff0f3;
  color: #e31c5f;
  font-size: 12px;
  font-weight: 800;
}

.dialog-heading h2 {
  margin: 0;
  color: #1d211e;
  font-size: 22px;
  line-height: 1.25;
}

.dialog-heading p,
.impact-copy {
  margin: 0;
  color: #747a75;
  font-size: 13px;
  line-height: 1.55;
}

.impact-alert {
  margin-bottom: 20px;
}

.impact-copy {
  margin-top: 5px;
  color: inherit;
}

.guest-review-form {
  display: grid;
  gap: 22px;
}

.form-section {
  display: grid;
  gap: 10px;
}

.form-section h3,
.section-label {
  margin: 0;
  color: #262a27;
  font-size: 14px;
  font-weight: 700;
}

.rating-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.rating-card {
  min-width: 0;
  padding: 16px;
  border: 1px solid #e8eae6;
  border-radius: 14px;
  background: #fafbf8;
}

.rating-card-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 10px;
}

.rating-card-heading label {
  color: #343936;
  font-size: 13px;
  font-weight: 700;
}

.rating-value {
  color: #2f7cf6;
  font-size: 12px;
  font-weight: 700;
}

.low-score-field {
  display: grid;
  gap: 6px;
  margin-top: 12px;
}

.low-score-field label {
  color: #696f6a;
  font-size: 12px;
  font-weight: 600;
}

.field-error {
  margin: 0;
  color: var(--el-color-danger);
  font-size: 12px;
  line-height: 1.4;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

:deep(.el-dialog__body) {
  padding-top: 8px;
}

@media (max-width: 720px) {
  .rating-grid {
    grid-template-columns: 1fr;
  }
}
</style>
