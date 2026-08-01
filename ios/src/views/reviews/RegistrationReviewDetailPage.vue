<template>
  <ion-page>
    <ion-header translucent>
      <ion-toolbar class="app-page-header__toolbar">
        <ion-buttons slot="start">
          <ion-back-button class="app-page-header__back-btn" :default-href="ROUTE_PATHS.reviews" />
        </ion-buttons>
        <ion-title class="app-page-header__title">{{ $t('routes.RegistrationReviewDetail') }}</ion-title>
      </ion-toolbar>
    </ion-header>

    <ion-content fullscreen class="mobile-page registration-review-detail-page">
      <div v-if="isLoading && !record" class="mobile-stack">
        <section class="mobile-card registration-review-detail-page__loading-state">
          <ion-spinner name="crescent" />
          <p class="mobile-note">{{ $t('stage5SourceText.157') }}</p>
        </section>
      </div>

      <template v-else-if="record">
        <section class="mobile-hero registration-review-detail-page__hero">
          <div class="registration-review-detail-page__hero-header">
            <div class="registration-review-detail-page__hero-copy">
              <p class="mobile-note registration-review-detail-page__eyebrow">{{ $t('stage5SourceText.169') }}</p>
              <h1 class="mobile-title">{{ record.guestName }}</h1>
            </div>
            <ion-button
              class="registration-review-detail-page__preview-trigger"
              fill="outline"
              size="small"
              :disabled="!hasGuestPreview"
              @click="handleOpenGuestPreview"
            >
              {{ $t('settingsStage4.autoCheckin.actions.preview') }}
            </ion-button>
          </div>
          <p class="mobile-subtitle">{{ record.roomLabel }} · {{ record.channelName }} · #{{ record.formId }}</p>
          <div class="mobile-chip-row">
            <span class="mobile-chip">{{ getReviewStatusLabel(record.status) }}</span>
            <span class="mobile-chip">{{ record.checkInDate }} {{ $t('roomStatus.action.checkIn') }}</span>
            <span class="mobile-chip">{{ record.checkOutDate }} {{ $t('roomStatus.hoverCard.checkOutDate') }}</span>
          </div>
        </section>

        <div class="mobile-stack">
          <section class="mobile-card">
            <h2 class="mobile-section-title">{{ $t('stage5SourceText.151') }}</h2>
            <div class="registration-review-detail-page__summary-grid">
              <article class="registration-review-detail-page__summary-card">
                <span>{{ $t('stage5SourceText.91') }}</span>
                <strong>{{ getReviewStatusLabel(record.status) }}</strong>
              </article>
              <article class="registration-review-detail-page__summary-card">
                <span>{{ $t('stage5.common.fields.submittedAt') }}</span>
                <strong>{{ record.submittedAt }}</strong>
              </article>
              <article class="registration-review-detail-page__summary-card">
                <span>{{ $t('stage5SourceText.227') }}</span>
                <strong>{{ record.attachments.length }} {{ $t('stage5DynamicUi.90') }}</strong>
              </article>
              <article class="registration-review-detail-page__summary-card">
                <span>{{ $t('stage5.dataCenter.detail.reviewHistory') }}</span>
                <strong>{{ record.history.length }} {{ $t('stage5DynamicUi.125') }}</strong>
              </article>
            </div>
          </section>

          <section class="mobile-card mobile-list">
            <article class="registration-review-detail-page__section-card">
              <div class="mobile-inline-row">
                <div>
                  <h2 class="mobile-section-title">{{ $t('settingsStage4.roomTypeManagement.actions.basicInfo') }}</h2>
                  <p class="mobile-note">{{ $t('stage5SourceText.198') }}</p>
                </div>
                <ion-button fill="outline" size="small" @click="handleOpenLinks">{{ $t('stage5.dataCenter.registrations.linkList') }}</ion-button>
              </div>

              <div class="registration-review-detail-page__detail-grid">
                <p><strong>{{ $t('stage5.dataCenter.detail.metaOrderNumber') }}</strong>{{ record.orderNumber || '—' }}</p>
                <p><strong>{{ $t('stage5SourceText.167') }}</strong>{{ record.channelOrderNumber || '—' }}</p>
                <p><strong>{{ $t('stage5SourceText.168') }}</strong>{{ record.channelName }}</p>
                <p><strong>{{ $t('stage5SourceText.104') }}</strong>{{ record.roomTypeName || '—' }}</p>
                <p><strong>{{ $t('stage5SourceText.99') }}</strong>{{ record.roomNumber || '—' }}</p>
                <p><strong>{{ $t('stage5SourceText.121') }}</strong>{{ record.submittedAt }}</p>
                <p><strong>{{ $t('stage5SourceText.138') }}</strong>{{ record.updatedAt }}</p>
                <p><strong>{{ $t('stage5SourceText.49') }}</strong>{{ record.reviewNote || '—' }}</p>
              </div>
            </article>

            <article class="registration-review-detail-page__section-card">
              <h2 class="mobile-section-title">{{ $t('stage5SourceText.13') }}</h2>
              <div class="mobile-list">
                <div v-for="guest in record.guests" :key="guest.id" class="registration-review-detail-page__list-item">
                  <div class="registration-review-detail-page__guest-card">
                    <strong>{{ guest.sortOrder }}. {{ guest.name }} · {{ guest.relation }}</strong>
                    <div class="registration-review-detail-page__detail-grid">
                      <p><strong>{{ guest.idType }}：</strong>{{ guest.idNumber }}</p>
                      <p><strong>{{ $t('stage5SourceText.110') }}</strong>{{ guest.phone }}</p>
                      <p><strong>{{ $t('stage5SourceText.37') }}</strong>{{ guest.nationality }}</p>
                      <p><strong>{{ $t('stage5SourceText.50') }}</strong>{{ guest.residenceType }}</p>
                      <p><strong>{{ $t('stage5SourceText.111') }}</strong>{{ guest.passportNumber }}</p>
                      <p><strong>{{ $t('stage5SourceText.24') }}</strong>{{ guest.priorStay }}</p>
                      <p><strong>{{ $t('stage5SourceText.189') }}</strong>{{ guest.nextDestination }}</p>
                    </div>
                  </div>
                </div>
                <p v-if="record.guests.length === 0" class="mobile-note">{{ $t('stage5SourceText.135') }}</p>
              </div>
            </article>

            <article class="registration-review-detail-page__section-card">
              <div class="mobile-inline-row">
                <div>
                  <h2 class="mobile-section-title">{{ $t('stage5SourceText.2') }}</h2>
                  <p class="mobile-note">{{ $t('stage5SourceText.28') }}</p>
                </div>
                <ion-button fill="outline" size="small" :disabled="isPdfDownloading" @click="handleDownloadPdf">
                  {{ isPdfDownloading ? $t('stage5DynamicUi.1') : $t('stage5DynamicUi.0') }}
                </ion-button>
              </div>

              <div class="mobile-list registration-review-detail-page__attachment-list">
                <div v-for="attachment in record.attachments" :key="attachment.id" class="registration-review-detail-page__list-item">
                  <div>
                    <strong>{{ attachment.name }}</strong>
                    <p>{{ attachment.typeLabel }} · {{ attachment.sizeLabel }}</p>
                    <p>{{ $t('stage5DynamicUi.91') }}{{ resolveGuestLabel(attachment.guestId) }}</p>
                  </div>
                  <div class="registration-review-detail-page__item-actions">
                    <ion-button fill="clear" size="small" :disabled="activeAttachmentId === attachment.id" @click="handlePreviewAttachment(attachment)">
                      {{ $t('settingsStage4.autoCheckin.actions.preview') }}
                    </ion-button>
                    <ion-button fill="clear" size="small" :disabled="activeAttachmentId === attachment.id" @click="handleDownloadAttachment(attachment)">
                      {{ $t('settingsStage4.autoCheckin.actions.download') }}
                    </ion-button>
                  </div>
                </div>
                <p v-if="record.attachments.length === 0" class="mobile-note">{{ $t('stage5SourceText.76') }}</p>
              </div>
            </article>
          </section>

          <section class="mobile-card registration-review-detail-page__note-card">
            <h2 class="mobile-section-title">{{ $t('stage5SourceText.48') }}</h2>
            <ion-textarea
              v-model="reviewNote"
              auto-grow
              fill="outline"
              :rows="5"
              :placeholder="$t('stage5UiAttributes.39')"
            />
          </section>

          <section class="mobile-card registration-review-detail-page__actions-card">
            <div>
              <h2 class="mobile-section-title">{{ $t('stage5SourceText.47') }}</h2>
              <p class="mobile-note">{{ $t('stage5SourceText.120') }}</p>
            </div>
            <div class="registration-review-detail-page__guest-message">
              <div class="mobile-inline-row">
                <strong>{{ $t('stage5.dataCenter.detail.approveMessageLabel') }}</strong>
                <ion-button
                  size="small"
                  fill="clear"
                  :disabled="quickReplyLoading"
                  @click="loadQuickReplies"
                >
                  {{ $t('stage5.common.actions.refresh') }}
                </ion-button>
              </div>
              <ion-select
                v-model="selectedQuickReplyId"
                fill="outline"
                interface="action-sheet"
                :disabled="quickReplyLoading || !canReview"
                :placeholder="$t('stage5.dataCenter.detail.selectQuickReply')"
                @ionChange="handleQuickReplyChange"
              >
                <ion-select-option
                  v-for="reply in quickReplies"
                  :key="reply.id"
                  :value="reply.id"
                >
                  {{ reply.title }}
                </ion-select-option>
              </ion-select>
              <ion-textarea
                v-model="guestMessage"
                auto-grow
                fill="outline"
                :rows="4"
                :disabled="!canReview || isSubmitting"
                :placeholder="$t('stage5.dataCenter.detail.approveMessagePlaceholder')"
              />
            </div>
            <div class="registration-review-detail-page__actions-grid">
              <ion-button color="success" :disabled="!canReview || isSubmitting" @click="handleApprove">{{ $t('stage5.common.actions.approve') }}</ion-button>
              <ion-button color="danger" fill="outline" :disabled="!canReview || isSubmitting" @click="handleReject">{{ $t('stage5.common.actions.reject') }}</ion-button>
            </div>
            <p v-if="!canReview" class="mobile-note">{{ $t('stage5SourceText.92') }}</p>
          </section>

          <section class="mobile-card">
            <h2 class="mobile-section-title">{{ $t('stage5.dataCenter.detail.reviewHistory') }}</h2>
            <div class="mobile-list">
              <article v-for="item in record.history" :key="item.id" class="registration-review-detail-page__history-item">
                <div class="mobile-inline-row">
                  <strong>{{ item.action }}</strong>
                  <span class="mobile-note">{{ item.timestamp }}</span>
                </div>
                <p class="mobile-note">{{ $t('stage5DynamicUi.117') }}{{ item.operator }}</p>
                <p class="mobile-note">{{ item.note }}</p>
              </article>
            </div>
          </section>
        </div>
      </template>

      <div v-else class="mobile-stack">
        <section class="mobile-card">
          <h2 class="mobile-section-title">{{ $t('stage5SourceText.45') }}</h2>
          <p class="mobile-note">{{ loadError || $t('stage5DynamicUi.33') }}</p>
          <ion-button fill="outline" @click="handleReload">{{ $t('storeSelection.reload') }}</ion-button>
          <ion-button expand="block" @click="handleBackToList">{{ $t('stage5SourceText.214') }}</ion-button>
        </section>
      </div>

      <ion-modal class="registration-review-detail-page__preview-modal" :is-open="guestPreviewOpen" @didDismiss="handleCloseGuestPreview">
        <ion-content class="registration-review-detail-page__preview-content">
          <div class="registration-review-detail-page__preview-shell">
            <div class="registration-review-detail-page__preview-topbar">
              <h2>{{ $t('stage5SourceText.14') }}</h2>
              <button
                class="registration-review-detail-page__preview-close"
                type="button"
                :aria-label="$t('stage5UiAttributes.29')"
                @click="handleCloseGuestPreview"
              >
                &times;
              </button>
            </div>

            <div class="mobile-list">
              <article
                v-for="guest in record?.guests || []"
                :key="`preview-${guest.id}`"
                class="registration-review-detail-page__preview-card"
              >
                <h3>{{ $t('stage5DynamicUi.93') }} {{ guest.sortOrder }}</h3>
                <div class="registration-review-detail-page__preview-fields">
                  <div class="registration-review-detail-page__preview-field">
                    <span>{{ $t('settingsStage4.autoCheckin.fields.givenName') }}</span>
                    <strong>{{ resolveGuestPreviewValue(guest.firstName) }}</strong>
                  </div>
                  <div class="registration-review-detail-page__preview-field">
                    <span>{{ $t('settingsStage4.autoCheckin.fields.familyName') }}</span>
                    <strong>{{ resolveGuestPreviewValue(guest.lastName) }}</strong>
                  </div>
                  <div class="registration-review-detail-page__preview-field">
                    <span>{{ $t('stage5.dataCenter.detail.residence') }}</span>
                    <strong>{{ resolveGuestPreviewValue(guest.residenceType) }}</strong>
                  </div>
                  <div class="registration-review-detail-page__preview-field">
                    <span>{{ $t('settingsStage4.autoCheckin.fields.birthDate') }}</span>
                    <strong>{{ resolveGuestPreviewValue(guest.birthday) }}</strong>
                  </div>
                  <div class="registration-review-detail-page__preview-field">
                    <span>{{ $t('roomStatus.sampleLogs.labels.phone') }}</span>
                    <strong>{{ resolveGuestPreviewValue(guest.phone) }}</strong>
                  </div>
                  <div class="registration-review-detail-page__preview-field">
                    <span>{{ $t('stage5.publicRegistration.form.passportNumber') }}</span>
                    <strong>{{ resolveGuestPreviewValue(guest.passportNumber) }}</strong>
                  </div>
                  <div class="registration-review-detail-page__preview-field">
                    <span>{{ $t('settingsStage4.autoCheckin.fields.nationality') }}</span>
                    <strong>{{ resolveGuestPreviewValue(guest.nationality) }}</strong>
                  </div>
                </div>

                <div v-if="findPassportAttachment(guest.id)" class="registration-review-detail-page__preview-passport">
                  <span class="registration-review-detail-page__preview-passport-title">
                    {{ $t('stage5.publicRegistration.form.passportPhoto') }}
                  </span>
                  <div
                    v-if="guestPassportStates[guest.id]?.loading"
                    class="registration-review-detail-page__preview-passport-state"
                  >
                    <ion-spinner name="crescent" />
                  </div>
                  <img
                    v-else-if="guestPassportStates[guest.id]?.url"
                    :src="guestPassportStates[guest.id]?.url"
                    :alt="$t('stage5.publicRegistration.form.passportPhoto')"
                    class="registration-review-detail-page__preview-passport-img"
                    @click="handleOpenGuestPassportViewer(guest)"
                  />
                  <p v-else-if="guestPassportStates[guest.id]?.failed" class="mobile-note">
                    {{ $t('stage5Final.review.previewAttachmentFailed') }}
                  </p>
                </div>
              </article>

              <section v-if="!hasGuestPreview" class="mobile-card">
                <p class="mobile-note">{{ $t('stage5SourceText.136') }}</p>
              </section>
            </div>
          </div>
        </ion-content>
      </ion-modal>

      <ImageViewerModal
        :open="imageViewerOpen"
        :src="imageViewerSrc"
        :title="imageViewerTitle"
        @close="handleCloseImageViewer"
      />
    </ion-content>
  </ion-page>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import {
  IonBackButton,
  IonButton,
  IonButtons,
  IonContent,
  IonHeader,
  IonModal,
  IonPage,
  IonSelect,
  IonSelectOption,
  IonSpinner,
  IonTextarea,
  IonTitle,
  IonToolbar,
  onIonViewWillEnter,
} from '@ionic/vue'
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  approveRegistrationReview,
  downloadRegistrationAttachment,
  downloadRegistrationPdf,
  getRegistrationReviewDetail,
  rejectRegistrationReview,
} from '@/api/review'
import { getAllQuickReplies, type QuickReplyDTO } from '@/api/quickReply'
import ImageViewerModal from '@/components/global/ImageViewerModal.vue'
import { getReviewStatusLabel, type ReviewAttachment, type ReviewGuest, type ReviewRecord } from '@/constants/reviews'
import { ROUTE_PATHS } from '@/router/guards'
import { useReviewStore } from '@/stores/reviews'
import { downloadBlobFile, openBlobPreview } from '@/utils/file'
import { showSuccessToast, showWarningToast } from '@/utils/notify'
import { showUnhandledRequestWarning } from '@/utils/requestError'

const { t } = useI18n()

const route = useRoute()
const router = useRouter()
const reviewStore = useReviewStore()

const record = ref<ReviewRecord | null>(null)
const reviewNote = ref('')
const isLoading = ref(false)
const isSubmitting = ref(false)
const isPdfDownloading = ref(false)
const activeAttachmentId = ref('')
const guestPreviewOpen = ref(false)
const loadError = ref('')
const guestMessage = ref('')
const quickReplies = ref<QuickReplyDTO[]>([])
const quickReplyLoading = ref(false)
const selectedQuickReplyId = ref<number | null>(null)

type GuestPassportState = {
  url?: string
  loading: boolean
  failed?: boolean
}

const IMAGE_FILE_PATTERN = /\.(avif|bmp|gif|hei[cf]|jpe?g|png|webp)$/i

const imageViewerOpen = ref(false)
const imageViewerSrc = ref('')
const imageViewerTitle = ref('')
// 附件预览的 blob URL 由本页面创建、关闭查看器时回收；客人护照图的 URL 归 guestPassportStates 管理
let imageViewerRevokeOnClose = false
const guestPassportStates = ref<Record<string, GuestPassportState>>({})
let guestPreviewRequestId = 0

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

const hasGuestPreview = computed(() => Boolean(record.value?.guests.length))

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
  await Promise.all([loadRecordDetail(), loadQuickReplies()])
})

function buildDecisionNote(fallbackText: string) {
  const nextNote = reviewNote.value.trim()
  if (nextNote) {
    return nextNote
  }

  return fallbackText
}

function buildDecisionPayload(fallbackText: string) {
  const message = guestMessage.value.trim()
  return {
    note: buildDecisionNote(fallbackText),
    ...(message
      ? {
          guestMessage: message,
          senderName: t('stage5.dataCenter.detail.frontDesk'),
        }
      : {}),
  }
}

async function loadQuickReplies() {
  quickReplyLoading.value = true
  try {
    const response = await getAllQuickReplies()
    if (!response.success || !response.data) {
      throw new Error(response.message || t('stage5.common.messages.dataLoadFailed'))
    }
    quickReplies.value = response.data
  } catch (error) {
    quickReplies.value = []
    showUnhandledRequestWarning(error, t('stage5.common.messages.dataLoadFailed'))
  } finally {
    quickReplyLoading.value = false
  }
}

function handleQuickReplyChange(event: CustomEvent) {
  const selectedId = Number(event.detail.value || 0)
  const reply = quickReplies.value.find((item) => item.id === selectedId)
  selectedQuickReplyId.value = null
  if (!reply?.message) {
    return
  }

  const currentMessage = guestMessage.value.trim()
  guestMessage.value = currentMessage ? `${currentMessage}\n\n${reply.message}` : reply.message
}

function showDecisionFeedback(
  response: Awaited<ReturnType<typeof approveRegistrationReview>>,
  successKey: string,
  successWithMessageKey: string,
) {
  const message = guestMessage.value.trim()
  if (!message) {
    showSuccessToast(t(successKey))
    return
  }

  const sendStatus = response?.messageLog?.sendStatus?.trim() || ''
  if (!response?.messageAttempted || sendStatus !== 'SENT') {
    showWarningToast(
      response?.messageError ||
        response?.messageLog?.errorMessage ||
        t('stage5.dataCenter.detail.sendFailed'),
    )
    return
  }

  guestMessage.value = ''
  showSuccessToast(t(successWithMessageKey))
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

function resolveGuestPreviewValue(value?: string) {
  const nextValue = String(value || '').trim()

  if (!nextValue || nextValue === '—' || nextValue === t('runtime.review.notProvided')) {
    return '-'
  }

  return nextValue
}

function isImageAttachment(blob: Blob, attachment: ReviewAttachment) {
  const mimeType = (blob.type || '').toLowerCase()

  if (mimeType.startsWith('image/')) {
    return true
  }

  // 服务端仅在未记录 Content-Type 时回退 octet-stream，此时按文件名扩展名兜底判断
  if (mimeType && mimeType !== 'application/octet-stream') {
    return false
  }

  return IMAGE_FILE_PATTERN.test(attachment.originalName || attachment.name || '')
}

function findPassportAttachment(guestId: string) {
  if (!record.value || !guestId) {
    return undefined
  }

  return record.value.attachments.find(
    (attachment) => attachment.guestId === guestId && attachment.type?.toUpperCase().includes('PASSPORT'),
  )
}

function revokeGuestPassportImages() {
  Object.values(guestPassportStates.value).forEach((state) => {
    if (state.url?.startsWith('blob:')) {
      URL.revokeObjectURL(state.url)
    }
  })
}

async function loadGuestPassportImages(requestId: number) {
  const currentRecord = record.value
  if (!currentRecord) {
    return
  }

  await Promise.all(
    currentRecord.guests.map(async (guest) => {
      const attachment = findPassportAttachment(guest.id)
      if (!attachment || attachment.attachmentNumericId <= 0) {
        return
      }

      guestPassportStates.value[guest.id] = { loading: true }

      try {
        const blob = await downloadRegistrationAttachment(
          currentRecord.formNumericId,
          attachment.attachmentNumericId,
        )

        if (requestId !== guestPreviewRequestId || !guestPreviewOpen.value) {
          return
        }

        if (!isImageAttachment(blob, attachment)) {
          guestPassportStates.value[guest.id] = { loading: false, failed: true }
          return
        }

        guestPassportStates.value[guest.id] = { loading: false, url: URL.createObjectURL(blob) }
      } catch {
        if (requestId !== guestPreviewRequestId) {
          return
        }
        guestPassportStates.value[guest.id] = { loading: false, failed: true }
      }
    }),
  )
}

function handleOpenGuestPreview() {
  if (!hasGuestPreview.value) {
    return
  }

  guestPreviewRequestId += 1
  revokeGuestPassportImages()
  guestPassportStates.value = {}
  guestPreviewOpen.value = true
  void loadGuestPassportImages(guestPreviewRequestId)
}

function handleCloseGuestPreview() {
  guestPreviewOpen.value = false
  guestPreviewRequestId += 1
  revokeGuestPassportImages()
  guestPassportStates.value = {}
}

function revokeImageViewerUrl() {
  if (imageViewerRevokeOnClose && imageViewerSrc.value.startsWith('blob:')) {
    URL.revokeObjectURL(imageViewerSrc.value)
  }
  imageViewerRevokeOnClose = false
  imageViewerSrc.value = ''
}

function openImageViewer(src: string, title: string, revokeOnClose: boolean) {
  revokeImageViewerUrl()
  imageViewerSrc.value = src
  imageViewerTitle.value = title
  imageViewerRevokeOnClose = revokeOnClose
  imageViewerOpen.value = true
}

function handleCloseImageViewer() {
  imageViewerOpen.value = false
  revokeImageViewerUrl()
}

function openAttachmentImagePreview(blob: Blob, attachment: ReviewAttachment) {
  openImageViewer(URL.createObjectURL(blob), attachment.name || attachment.originalName, true)
}

function handleOpenGuestPassportViewer(guest: ReviewGuest) {
  const url = guestPassportStates.value[guest.id]?.url
  if (!url) {
    return
  }
  openImageViewer(url, `${guest.name} · ${t('stage5.publicRegistration.form.passportPhoto')}`, false)
}

onBeforeUnmount(() => {
  guestPreviewRequestId += 1
  revokeGuestPassportImages()
  revokeImageViewerUrl()
})

async function handleDownloadPdf() {
  if (!record.value || isPdfDownloading.value) {
    return
  }

  isPdfDownloading.value = true

  try {
    const pdfBlob = await downloadRegistrationPdf(record.value.formNumericId)
    downloadBlobFile(pdfBlob, buildPdfFileName())
    showSuccessToast(t('stage5Final.review.pdfDownloadStarted'))
  } catch (error) {
    showUnhandledRequestWarning(error, t('stage5Final.review.downloadPdfFailed'))
  } finally {
    isPdfDownloading.value = false
  }
}

async function handlePreviewAttachment(attachment: ReviewAttachment) {
  if (!record.value || activeAttachmentId.value) {
    return
  }

  if (attachment.attachmentNumericId <= 0) {
    showWarningToast(t('stage5Final.review.attachmentPreviewUnavailable'))
    return
  }

  activeAttachmentId.value = attachment.id

  try {
    const attachmentBlob = await downloadRegistrationAttachment(
      record.value.formNumericId,
      attachment.attachmentNumericId,
    )

    // WKWebView 内 window.open(blobUrl) 与 <a download> 均不可用，图片改为页内弹窗预览（对齐 web 端内联展示）
    if (isImageAttachment(attachmentBlob, attachment)) {
      openAttachmentImagePreview(attachmentBlob, attachment)
      return
    }

    const didOpenPreview = openBlobPreview(attachmentBlob)

    if (didOpenPreview) {
      showSuccessToast(t('stage5Final.review.attachmentPreviewOpened'))
    } else {
      downloadBlobFile(attachmentBlob, buildAttachmentFileName(attachment))
      showWarningToast(t('stage5Final.review.previewFallbackDownload'))
    }
  } catch (error) {
    showUnhandledRequestWarning(error, t('stage5Final.review.previewAttachmentFailed'))
  } finally {
    activeAttachmentId.value = ''
  }
}

async function handleDownloadAttachment(attachment: ReviewAttachment) {
  if (!record.value || activeAttachmentId.value) {
    return
  }

  if (attachment.attachmentNumericId <= 0) {
    showWarningToast(t('stage5Final.review.attachmentDownloadUnavailable'))
    return
  }

  activeAttachmentId.value = attachment.id

  try {
    const attachmentBlob = await downloadRegistrationAttachment(
      record.value.formNumericId,
      attachment.attachmentNumericId,
    )
    downloadBlobFile(attachmentBlob, buildAttachmentFileName(attachment))
    showSuccessToast(t('stage5Final.review.attachmentDownloadStarted'))
  } catch (error) {
    showUnhandledRequestWarning(error, t('stage5Final.review.downloadAttachmentFailed'))
  } finally {
    activeAttachmentId.value = ''
  }
}

async function loadRecordDetail() {
  if (!formId.value) {
    record.value = null
    loadError.value = t('stage5Final.review.missingFormId')
    return false
  }

  isLoading.value = true
  loadError.value = ''

  try {
    const detail = await getRegistrationReviewDetail(formId.value)
    record.value = detail
    reviewNote.value = detail.reviewNote || ''
    // 就地同步列表缓存，代替回列表页前的全量刷新
    reviewStore.syncRecord(detail)
    return true
  } catch (error) {
    record.value = null
    loadError.value = showUnhandledRequestWarning(error, t('stage5Final.review.loadDetailFailed'))
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
    const response = await approveRegistrationReview(
      record.value.formNumericId,
      buildDecisionPayload(t('stage5Final.review.approveNote')),
    )
    showDecisionFeedback(
      response,
      'stage5Final.review.approved',
      'stage5.dataCenter.detail.approveWithMessageSuccess',
    )
    await loadRecordDetail()
  } catch (error) {
    showUnhandledRequestWarning(error, t('stage5Final.review.approveFailed'))
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
    const response = await rejectRegistrationReview(
      record.value.formNumericId,
      buildDecisionPayload(t('stage5Final.review.rejectNote')),
    )
    showDecisionFeedback(
      response,
      'stage5Final.review.rejected',
      'stage5.dataCenter.detail.rejectWithMessageSuccess',
    )
    await loadRecordDetail()
  } catch (error) {
    showUnhandledRequestWarning(error, t('stage5Final.review.rejectFailed'))
  } finally {
    isSubmitting.value = false
  }
}

async function handleReload() {
  await loadRecordDetail()
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

.registration-review-detail-page__hero {
  display: grid;
  gap: 14px;
}

.registration-review-detail-page__hero-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.registration-review-detail-page__hero-copy {
  min-width: 0;
}

.registration-review-detail-page__hero-copy .mobile-title {
  margin-top: 4px;
}

.registration-review-detail-page__preview-trigger {
  flex-shrink: 0;
  --background: rgba(255, 255, 255, 0.76);
  --border-color: rgba(58, 120, 255, 0.24);
  --border-radius: 999px;
  --color: var(--ion-color-primary);
  font-weight: 600;
  backdrop-filter: blur(18px);
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

.registration-review-detail-page__guest-message {
  display: grid;
  gap: 10px;
  padding: 12px;
  border: 1px solid var(--app-border);
  border-radius: 10px;
  background: rgba(248, 250, 252, 0.82);
}

.registration-review-detail-page__guest-message ion-select,
.registration-review-detail-page__guest-message ion-textarea {
  --background: #fff;
  --border-radius: 8px;
}

.registration-review-detail-page__loading-state {
  display: grid;
  justify-items: center;
  gap: 10px;
}

:global(ion-modal.registration-review-detail-page__preview-modal) {
  --border-radius: 28px 28px 0 0;
  --backdrop-opacity: 0.24;
  --box-shadow: 0 -28px 56px rgba(15, 23, 42, 0.2);
}

.registration-review-detail-page__preview-content {
  --background: linear-gradient(180deg, #f8fafc 0%, #eef2f7 100%);
}

.registration-review-detail-page__preview-shell {
  min-height: 100%;
  padding: 18px 16px calc(28px + var(--app-safe-bottom));
  display: grid;
  gap: 14px;
}

.registration-review-detail-page__preview-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.registration-review-detail-page__preview-topbar h2 {
  margin: 0;
  color: var(--ios-pms-text-primary);
  font-size: 24px;
  font-weight: var(--ios-pms-weight-bold);
  letter-spacing: -0.03em;
}

.registration-review-detail-page__preview-close {
  width: 40px;
  height: 40px;
  border: 0;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.9);
  color: #7b8798;
  font-size: 28px;
  line-height: 1;
  cursor: pointer;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.08);
}

.registration-review-detail-page__preview-card {
  padding: 18px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(148, 163, 184, 0.16);
  box-shadow: 0 18px 34px rgba(148, 163, 184, 0.12);
}

.registration-review-detail-page__preview-card h3 {
  margin: 0 0 14px;
  color: var(--ios-pms-text-primary);
  font-size: 16px;
  font-weight: 700;
}

.registration-review-detail-page__preview-fields {
  display: grid;
}

.registration-review-detail-page__preview-field {
  display: grid;
  gap: 6px;
  padding: 14px 0;
  border-bottom: 1px solid rgba(148, 163, 184, 0.2);
}

.registration-review-detail-page__preview-field:first-child {
  padding-top: 0;
}

.registration-review-detail-page__preview-field:last-child {
  padding-bottom: 0;
  border-bottom: 0;
}

.registration-review-detail-page__preview-field span {
  color: var(--ios-pms-text-muted);
  font-size: 13px;
  font-weight: 600;
}

.registration-review-detail-page__preview-field strong {
  color: var(--ios-pms-text-primary);
  font-size: 15px;
  font-weight: 600;
  line-height: 1.45;
}

.registration-review-detail-page__preview-passport {
  display: grid;
  gap: 10px;
  margin-top: 16px;
}

.registration-review-detail-page__preview-passport-title {
  color: var(--ios-pms-text-muted);
  font-size: 13px;
  font-weight: 600;
}

.registration-review-detail-page__preview-passport-state {
  display: grid;
  justify-items: center;
  padding: 18px 0;
}

.registration-review-detail-page__preview-passport-img {
  display: block;
  width: 100%;
  border: 1px solid rgba(148, 163, 184, 0.24);
  border-radius: 16px;
  background: #ffffff;
  cursor: zoom-in;
}

@media (max-width: 360px) {
  .registration-review-detail-page__hero-header {
    flex-direction: column;
    align-items: stretch;
  }

  .registration-review-detail-page__summary-grid,
  .registration-review-detail-page__actions-grid,
  .registration-review-detail-page__detail-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
