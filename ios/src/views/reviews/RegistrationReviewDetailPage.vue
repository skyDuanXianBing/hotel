<template>
  <ion-page>
    <ion-header translucent>
      <ion-toolbar class="app-page-header__toolbar">
        <ion-buttons slot="start">
          <ion-back-button class="app-page-header__back-btn" :default-href="ROUTE_PATHS.reviews" />
        </ion-buttons>
        <ion-title class="app-page-header__title">审查详情</ion-title>
      </ion-toolbar>
    </ion-header>

    <ion-content fullscreen class="mobile-page registration-review-detail-page">
      <div v-if="isLoading && !record" class="mobile-stack">
        <section class="mobile-card registration-review-detail-page__loading-state">
          <ion-spinner name="crescent" />
          <p class="mobile-note">正在加载审查详情...</p>
        </section>
      </div>

      <template v-else-if="record">
        <section class="mobile-hero registration-review-detail-page__hero">
          <p class="mobile-note registration-review-detail-page__eyebrow">登记审查详情</p>
          <h1 class="mobile-title">{{ record.guestName }}</h1>
          <p class="mobile-subtitle">{{ record.roomLabel }} · {{ record.channelName }} · #{{ record.formId }}</p>
          <div class="mobile-chip-row">
            <span class="mobile-chip">{{ getReviewStatusLabel(record.status) }}</span>
            <span class="mobile-chip">{{ record.checkInDate }} 入住</span>
            <span class="mobile-chip">{{ record.checkOutDate }} 离店</span>
          </div>
        </section>

        <div class="mobile-stack">
          <section class="mobile-card">
            <h2 class="mobile-section-title">概览</h2>
            <div class="registration-review-detail-page__summary-grid">
              <article class="registration-review-detail-page__summary-card">
                <span>当前状态</span>
                <strong>{{ getReviewStatusLabel(record.status) }}</strong>
              </article>
              <article class="registration-review-detail-page__summary-card">
                <span>提交时间</span>
                <strong>{{ record.submittedAt }}</strong>
              </article>
              <article class="registration-review-detail-page__summary-card">
                <span>附件数量</span>
                <strong>{{ record.attachments.length }} 份</strong>
              </article>
              <article class="registration-review-detail-page__summary-card">
                <span>审核历史</span>
                <strong>{{ record.history.length }} 条</strong>
              </article>
            </div>
          </section>

          <section class="mobile-card mobile-list">
            <article class="registration-review-detail-page__section-card">
              <div class="mobile-inline-row">
                <div>
                  <h2 class="mobile-section-title">基础信息</h2>
                  <p class="mobile-note">订单、房间与审核备注按移动端卡片方式集中展示。</p>
                </div>
                <ion-button fill="outline" size="small" @click="handleOpenLinks">链接列表</ion-button>
              </div>

              <div class="registration-review-detail-page__detail-grid">
                <p><strong>订单号：</strong>{{ record.orderNumber || '—' }}</p>
                <p><strong>渠道订单号：</strong>{{ record.channelOrderNumber || '—' }}</p>
                <p><strong>渠道：</strong>{{ record.channelName }}</p>
                <p><strong>房型：</strong>{{ record.roomTypeName || '—' }}</p>
                <p><strong>房号：</strong>{{ record.roomNumber || '—' }}</p>
                <p><strong>提交时间：</strong>{{ record.submittedAt }}</p>
                <p><strong>更新时间：</strong>{{ record.updatedAt }}</p>
                <p><strong>审核备注：</strong>{{ record.reviewNote || '—' }}</p>
              </div>
            </article>

            <article class="registration-review-detail-page__section-card">
              <h2 class="mobile-section-title">入住人信息</h2>
              <div class="mobile-list">
                <div v-for="guest in record.guests" :key="guest.id" class="registration-review-detail-page__list-item">
                  <div class="registration-review-detail-page__guest-card">
                    <strong>{{ guest.sortOrder }}. {{ guest.name }} · {{ guest.relation }}</strong>
                    <div class="registration-review-detail-page__detail-grid">
                      <p><strong>{{ guest.idType }}：</strong>{{ guest.idNumber }}</p>
                      <p><strong>手机号：</strong>{{ guest.phone }}</p>
                      <p><strong>国籍：</strong>{{ guest.nationality }}</p>
                      <p><strong>居住地：</strong>{{ guest.residenceType }}</p>
                      <p><strong>Passport：</strong>{{ guest.passportNumber }}</p>
                      <p><strong>前泊地：</strong>{{ guest.priorStay }}</p>
                      <p><strong>行先：</strong>{{ guest.nextDestination }}</p>
                    </div>
                  </div>
                </div>
                <p v-if="record.guests.length === 0" class="mobile-note">暂无入住人详情。</p>
              </div>
            </article>

            <article class="registration-review-detail-page__section-card">
              <div class="mobile-inline-row">
                <div>
                  <h2 class="mobile-section-title">PDF 与附件</h2>
                  <p class="mobile-note">可下载登记 PDF，并查看或下载附件；如暂不支持预览，将直接下载。</p>
                </div>
                <ion-button fill="outline" size="small" :disabled="isPdfDownloading" @click="handleDownloadPdf">
                  {{ isPdfDownloading ? '下载中...' : '下载 PDF' }}
                </ion-button>
              </div>

              <div class="mobile-list registration-review-detail-page__attachment-list">
                <div v-for="attachment in record.attachments" :key="attachment.id" class="registration-review-detail-page__list-item">
                  <div>
                    <strong>{{ attachment.name }}</strong>
                    <p>{{ attachment.typeLabel }} · {{ attachment.sizeLabel }}</p>
                    <p>住客：{{ resolveGuestLabel(attachment.guestId) }}</p>
                  </div>
                  <div class="registration-review-detail-page__item-actions">
                    <ion-button fill="clear" size="small" :disabled="activeAttachmentId === attachment.id" @click="handlePreviewAttachment(attachment)">
                      预览
                    </ion-button>
                    <ion-button fill="clear" size="small" :disabled="activeAttachmentId === attachment.id" @click="handleDownloadAttachment(attachment)">
                      下载
                    </ion-button>
                  </div>
                </div>
                <p v-if="record.attachments.length === 0" class="mobile-note">当前暂无可查看附件。</p>
              </div>
            </article>
          </section>

          <section class="mobile-card registration-review-detail-page__note-card">
            <h2 class="mobile-section-title">审核备注</h2>
            <ion-textarea
              v-model="reviewNote"
              auto-grow
              fill="outline"
              :rows="5"
              placeholder="填写审核意见、补充要求或驳回原因"
            />
          </section>

          <section class="mobile-card registration-review-detail-page__actions-card">
            <div>
              <h2 class="mobile-section-title">审核动作</h2>
              <p class="mobile-note">提交处理结果后，会自动刷新详情与列表状态。</p>
            </div>
            <div class="registration-review-detail-page__actions-grid">
              <ion-button color="success" :disabled="!canReview || isSubmitting" @click="handleApprove">通过</ion-button>
              <ion-button color="danger" fill="outline" :disabled="!canReview || isSubmitting" @click="handleReject">驳回</ion-button>
            </div>
            <p v-if="!canReview" class="mobile-note">当前状态无需重复审核。</p>
          </section>

          <section class="mobile-card">
            <h2 class="mobile-section-title">审核历史</h2>
            <div class="mobile-list">
              <article v-for="item in record.history" :key="item.id" class="registration-review-detail-page__history-item">
                <div class="mobile-inline-row">
                  <strong>{{ item.action }}</strong>
                  <span class="mobile-note">{{ item.timestamp }}</span>
                </div>
                <p class="mobile-note">操作人：{{ item.operator }}</p>
                <p class="mobile-note">{{ item.note }}</p>
              </article>
            </div>
          </section>
        </div>
      </template>

      <div v-else class="mobile-stack">
        <section class="mobile-card">
          <h2 class="mobile-section-title">审查详情加载失败</h2>
          <p class="mobile-note">{{ loadError || '当前表单不存在或已失效，请返回审查列表重新选择。' }}</p>
          <ion-button fill="outline" @click="handleReload">重新加载</ion-button>
          <ion-button expand="block" @click="handleBackToList">返回审查列表</ion-button>
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
  IonPage,
  IonSpinner,
  IonTextarea,
  IonTitle,
  IonToolbar,
  onIonViewWillEnter,
} from '@ionic/vue'
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  approveRegistrationReview,
  downloadRegistrationAttachment,
  downloadRegistrationPdf,
  getRegistrationReviewDetail,
  rejectRegistrationReview,
} from '@/api/review'
import { getReviewStatusLabel, type ReviewAttachment, type ReviewRecord } from '@/constants/reviews'
import { ROUTE_PATHS } from '@/router/guards'
import { useReviewStore } from '@/stores/reviews'
import { downloadBlobFile, openBlobPreview } from '@/utils/file'
import { showSuccessToast, showWarningToast } from '@/utils/notify'
import { showUnhandledRequestWarning } from '@/utils/requestError'

const route = useRoute()
const router = useRouter()
const reviewStore = useReviewStore()

const record = ref<ReviewRecord | null>(null)
const reviewNote = ref('')
const isLoading = ref(false)
const isSubmitting = ref(false)
const isPdfDownloading = ref(false)
const activeAttachmentId = ref('')
const loadError = ref('')

const formId = computed(() => {
  const rawFormId = Array.isArray(route.params.formId) ? route.params.formId[0] : route.params.formId
  const parsedValue = Number(rawFormId || '')

  if (Number.isFinite(parsedValue) && parsedValue > 0) {
    return parsedValue
  }

  return 0
})

const canReview = computed(() => {
  if (!record.value) {
    return false
  }

  return record.value.status === 'pending' || record.value.status === 'draft'
})

watch(
  formId,
  async (nextFormId, previousFormId) => {
    if (!nextFormId || nextFormId === previousFormId) {
      return
    }

    await loadRecordDetail()
  },
)

onIonViewWillEnter(async () => {
  await Promise.all([loadRecordDetail(), reviewStore.refreshRecords()])
})

function buildDecisionNote(fallbackText: string) {
  const nextNote = reviewNote.value.trim()
  if (nextNote) {
    return nextNote
  }

  return fallbackText
}

function buildPdfFileName() {
  if (!record.value) {
    return 'registration-review.pdf'
  }

  const suffix = record.value.orderNumber || record.value.formId
  return `registration-${suffix}.pdf`
}

function buildAttachmentFileName(attachment: ReviewAttachment) {
  if (attachment.originalName) {
    return attachment.originalName
  }

  return `attachment-${attachment.attachmentNumericId}`
}

function resolveGuestLabel(guestId: string) {
  if (!record.value || !guestId) {
    return '—'
  }

  const targetGuest = record.value.guests.find((guest) => guest.id === guestId)

  if (!targetGuest) {
    return guestId
  }

  return `${targetGuest.sortOrder}. ${targetGuest.name}`
}

async function handleDownloadPdf() {
  if (!record.value || isPdfDownloading.value) {
    return
  }

  isPdfDownloading.value = true

  try {
    const pdfBlob = await downloadRegistrationPdf(record.value.formNumericId)
    downloadBlobFile(pdfBlob, buildPdfFileName())
    showSuccessToast('PDF 已开始下载')
  } catch (error) {
    showUnhandledRequestWarning(error, '下载 PDF 失败')
  } finally {
    isPdfDownloading.value = false
  }
}

async function handlePreviewAttachment(attachment: ReviewAttachment) {
  if (!record.value || activeAttachmentId.value) {
    return
  }

  if (attachment.attachmentNumericId <= 0) {
    showWarningToast('附件缺少有效标识，暂无法预览')
    return
  }

  activeAttachmentId.value = attachment.id

  try {
    const attachmentBlob = await downloadRegistrationAttachment(
      record.value.formNumericId,
      attachment.attachmentNumericId,
    )
    const didOpenPreview = openBlobPreview(attachmentBlob)

    if (didOpenPreview) {
      showSuccessToast('已打开附件预览')
    } else {
      downloadBlobFile(attachmentBlob, buildAttachmentFileName(attachment))
      showWarningToast('当前环境未能直接预览，已改为下载附件')
    }
  } catch (error) {
    showUnhandledRequestWarning(error, '预览附件失败')
  } finally {
    activeAttachmentId.value = ''
  }
}

async function handleDownloadAttachment(attachment: ReviewAttachment) {
  if (!record.value || activeAttachmentId.value) {
    return
  }

  if (attachment.attachmentNumericId <= 0) {
    showWarningToast('附件缺少有效标识，暂无法下载')
    return
  }

  activeAttachmentId.value = attachment.id

  try {
    const attachmentBlob = await downloadRegistrationAttachment(
      record.value.formNumericId,
      attachment.attachmentNumericId,
    )
    downloadBlobFile(attachmentBlob, buildAttachmentFileName(attachment))
    showSuccessToast('附件已开始下载')
  } catch (error) {
    showUnhandledRequestWarning(error, '下载附件失败')
  } finally {
    activeAttachmentId.value = ''
  }
}

async function loadRecordDetail() {
  if (!formId.value) {
    record.value = null
    loadError.value = '缺少有效表单编号，请返回审查列表后重试。'
    return false
  }

  isLoading.value = true
  loadError.value = ''

  try {
    const detail = await getRegistrationReviewDetail(formId.value)
    record.value = detail
    reviewNote.value = detail.reviewNote || ''
    return true
  } catch (error) {
    record.value = null
    loadError.value = showUnhandledRequestWarning(error, '加载审查详情失败')
    return false
  } finally {
    isLoading.value = false
  }
}

async function handleApprove() {
  if (!record.value) {
    return
  }

  isSubmitting.value = true

  try {
    await approveRegistrationReview(record.value.formNumericId, buildDecisionNote('资料完整，审核通过。'))
    showSuccessToast('已审核通过')
    await Promise.all([loadRecordDetail(), reviewStore.refreshRecords()])
  } catch (error) {
    showUnhandledRequestWarning(error, '审核通过失败')
  } finally {
    isSubmitting.value = false
  }
}

async function handleReject() {
  if (!record.value) {
    return
  }

  isSubmitting.value = true

  try {
    await rejectRegistrationReview(record.value.formNumericId, buildDecisionNote('资料不完整，请补充后重新提交。'))
    showSuccessToast('已驳回并记录备注')
    await Promise.all([loadRecordDetail(), reviewStore.refreshRecords()])
  } catch (error) {
    showUnhandledRequestWarning(error, '驳回失败')
  } finally {
    isSubmitting.value = false
  }
}

async function handleReload() {
  await Promise.all([loadRecordDetail(), reviewStore.refreshRecords()])
}

async function handleBackToList() {
  await router.push(ROUTE_PATHS.reviews)
}

async function handleOpenLinks() {
  await router.push({
    name: 'RegistrationReviewLinks',
  })
}
</script>

<style scoped>
.registration-review-detail-page__eyebrow {
  color: var(--ion-color-primary);
  font-weight: 700;
}

.registration-review-detail-page__summary-grid,
.registration-review-detail-page__actions-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.registration-review-detail-page__summary-card,
.registration-review-detail-page__section-card,
.registration-review-detail-page__list-item,
.registration-review-detail-page__history-item {
  padding: 14px;
  border-radius: 18px;
  border: 1px solid var(--app-border);
  background: rgba(255, 255, 255, 0.86);
}

.registration-review-detail-page__summary-card span {
  display: block;
  color: var(--app-muted);
  font-size: 12px;
}

.registration-review-detail-page__summary-card strong {
  display: block;
  margin-top: 8px;
}

.registration-review-detail-page__detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px 14px;
  margin-top: 12px;
}

.registration-review-detail-page__detail-grid p {
  margin: 0;
  color: var(--app-muted);
  line-height: 1.6;
}

.registration-review-detail-page__guest-card {
  display: grid;
  gap: 10px;
}

.registration-review-detail-page__list-item p,
.registration-review-detail-page__history-item p {
  margin: 6px 0 0;
}

.registration-review-detail-page__attachment-list,
.registration-review-detail-page__note-card,
.registration-review-detail-page__actions-card {
  margin-top: 12px;
}

.registration-review-detail-page__item-actions {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.registration-review-detail-page__actions-card {
  display: grid;
  gap: 12px;
}

.registration-review-detail-page__loading-state {
  display: grid;
  justify-items: center;
  gap: 10px;
}

@media (max-width: 360px) {
  .registration-review-detail-page__summary-grid,
  .registration-review-detail-page__actions-grid,
  .registration-review-detail-page__detail-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
