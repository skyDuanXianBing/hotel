<template>
  <StatisticsLayout>
    <div class="wrap">
      <div class="top">
        <div class="title">{{ t('stage5.dataCenter.detail.title') }}</div>
        <div class="actions">
          <el-button @click="back">{{ t('stage5.common.actions.back') }}</el-button>
          <el-button :loading="loading" @click="load">{{ t('stage5.common.actions.refresh') }}</el-button>
          <el-button type="primary" :disabled="!detail" @click="downloadPdf">{{ t('stage5.common.actions.downloadPdf') }}</el-button>
        </div>
      </div>

      <div v-if="!detail" class="empty">{{ t('stage5.common.empty.noData') }}</div>

      <div v-else class="card">
        <div class="meta">
          <div class="meta-actions">
            <el-button size="small" :disabled="!detail" @click="previewForm">
              {{ t('stage5.common.actions.preview') }}
            </el-button>
          </div>
          <div><b>{{ t('stage5.dataCenter.detail.metaOrderNumber') }}</b>{{ detail.channelOrderNumber || detail.orderNumber }}</div>
          <div><b>{{ t('stage5.dataCenter.detail.metaGuest') }}</b>{{ detail.guestName }}</div>
          <div><b>{{ t('stage5.dataCenter.detail.metaDate') }}</b>{{ detail.checkInDate }} ~ {{ detail.checkOutDate }}</div>
          <div><b>{{ t('stage5.dataCenter.detail.metaReservedRoomType') }}</b>{{ detail.roomTypeName || '-' }}</div>
          <div><b>{{ t('stage5.dataCenter.detail.metaRoomNumber') }}</b>{{ detail.roomNumber || '-' }}</div>
          <div><b>{{ t('stage5.dataCenter.detail.metaStatus') }}</b>{{ detail.status }}</div>
          <div v-if="detail.reviewNote"><b>{{ t('stage5.dataCenter.detail.metaNote') }}</b>{{ detail.reviewNote }}</div>
        </div>

        <div class="section">
          <div class="section-title">{{ t('stage5.dataCenter.detail.guestInfo') }}</div>
          <el-table :data="detail.guests" border stripe style="width: 100%">
            <el-table-column prop="sortOrder" label="#" width="60" />
            <el-table-column :label="t('stage5.dataCenter.detail.name')" min-width="160">
              <template #default="{ row }">{{ row.lastName }} {{ row.firstName }}</template>
            </el-table-column>
            <el-table-column prop="nationality" :label="t('stage5.dataCenter.detail.nationality')" min-width="120" />
            <el-table-column prop="residenceType" :label="t('stage5.dataCenter.detail.residence')" min-width="110" />
            <el-table-column prop="passportNumber" :label="t('stage5.dataCenter.detail.passport')" min-width="150" />
            <el-table-column prop="priorStay" :label="t('stage5.dataCenter.detail.priorStay')" min-width="120" />
            <el-table-column prop="nextDestination" :label="t('stage5.dataCenter.detail.nextDestination')" min-width="120" />
          </el-table>
        </div>

        <div class="section" v-if="detail.attachments && detail.attachments.length">
          <div class="section-title">{{ t('stage5.common.fields.attachments') }}</div>
          <el-table :data="detail.attachments" border stripe style="width: 100%">
            <el-table-column prop="id" :label="t('stage5.common.fields.id')" width="90" />
            <el-table-column :label="t('stage5.common.fields.guest')" min-width="120">
              <template #default="{ row }">{{ guestLabel(row.guestId) }}</template>
            </el-table-column>
            <el-table-column prop="type" :label="t('stage5.common.fields.type')" min-width="120" />
            <el-table-column prop="originalName" :label="t('stage5.common.fields.fileName')" min-width="220" />
            <el-table-column :label="t('stage5.common.fields.actions')" width="180">
              <template #default="{ row }">
                <el-button size="small" @click="previewAttachment(row)">{{ t('stage5.common.actions.preview') }}</el-button>
                <el-button size="small" type="primary" @click="downloadAttachment(row)">{{ t('stage5.common.actions.download') }}</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <div class="section">
          <div class="section-title">{{ t('stage5.dataCenter.detail.reviewAction') }}</div>
          <el-input v-model="note" type="textarea" :rows="3" :placeholder="t('stage5.dataCenter.detail.notePlaceholder')" />
          <div class="approve-message">
            <div class="approve-message__header">
              <div class="approve-message__label">{{ t('stage5.dataCenter.detail.approveMessageLabel') }}</div>
              <div class="approve-message__tools">
                <el-select
                  v-model="reviewQuickReplyId"
                  size="small"
                  filterable
                  clearable
                  :placeholder="t('stage5.dataCenter.detail.selectQuickReply')"
                  class="approve-message__quick-select"
                  :loading="quickReplyLoading"
                  @change="applyReviewQuickReply"
                >
                  <el-option v-for="q in quickReplies" :key="q.id" :label="q.title" :value="q.id" />
                </el-select>
                <el-tooltip :content="t('stage5.common.actions.refresh')" placement="top">
                  <el-button
                    size="small"
                    circle
                    :icon="Refresh"
                    :loading="quickReplyLoading"
                    @click="loadQuickReplies"
                  />
                </el-tooltip>
              </div>
            </div>
            <el-input
              v-model="approveMessage"
              type="textarea"
              :rows="4"
              :placeholder="t('stage5.dataCenter.detail.approveMessagePlaceholder')"
            />
          </div>
          <div class="btns">
            <el-button type="danger" :loading="acting" @click="reject">{{ t('stage5.common.actions.reject') }}</el-button>
            <el-button type="success" :loading="acting" @click="approve">{{ t('stage5.common.actions.approve') }}</el-button>
          </div>
        </div>

        <div class="section">
          <div class="section-title">{{ t('stage5.dataCenter.detail.reviewHistory') }}</div>
          <el-table :data="detail.reviewLogs" border stripe style="width: 100%">
            <el-table-column prop="createdAt" :label="t('stage5.common.fields.time')" min-width="170" />
            <el-table-column prop="action" :label="t('stage5.common.fields.actions')" min-width="110" />
            <el-table-column prop="operatorUserId" :label="t('stage5.common.fields.userId')" min-width="110" />
            <el-table-column prop="note" :label="t('stage5.common.fields.note')" min-width="220" />
          </el-table>
        </div>

        <div class="section" v-if="false">
          <div class="section-title">{{ t('stage5.dataCenter.detail.sendMessageToGuest') }}</div>
          <div class="msg-grid">
            <div class="msg-row">
              <div class="msg-label">{{ t('stage5.dataCenter.detail.messageType') }}</div>
              <el-select v-model="sendType" :placeholder="t('stage5.dataCenter.detail.selectMessageType')" style="width: 240px">
                <el-option :label="t('stage5.dataCenter.detail.messageTypes.approvedInfo')" value="APPROVED_INFO" />
                <el-option :label="t('stage5.dataCenter.detail.messageTypes.rejectRequest')" value="REJECT_REQUEST" />
                <el-option :label="t('stage5.dataCenter.detail.messageTypes.reminder')" value="REMINDER" />
              </el-select>
            </div>

            <div class="msg-row">
              <div class="msg-label">{{ t('stage5.dataCenter.detail.quickReply') }}</div>
              <el-select
                v-model="quickReplyId"
                filterable
                clearable
                :placeholder="t('stage5.dataCenter.detail.selectQuickReply')"
                style="width: 420px"
                :loading="quickReplyLoading"
                @change="applyQuickReply"
              >
                <el-option v-for="q in quickReplies" :key="q.id" :label="q.title" :value="q.id" />
              </el-select>
              <el-button @click="loadQuickReplies" :loading="quickReplyLoading">{{ t('stage5.common.actions.refresh') }}</el-button>
            </div>

            <div class="msg-row">
              <div class="msg-label">{{ t('stage5.dataCenter.detail.messageContent') }}</div>
              <el-input v-model="sendContent" type="textarea" :rows="5" :placeholder="t('stage5.dataCenter.detail.variablesPlaceholder')" />
            </div>

            <div class="msg-actions">
              <el-button :disabled="!detail" @click="fillDefaultTemplate">{{ t('stage5.dataCenter.detail.fillDefaultTemplate') }}</el-button>
              <el-button type="primary" :loading="sending" :disabled="!detail" @click="sendMessage">{{ t('stage5.common.actions.send') }}</el-button>
            </div>
            <div class="msg-hint">
              <div v-if="lastSendStatus" class="msg-status">{{ t('stage5.dataCenter.detail.lastSendResult', { status: lastSendStatus }) }}</div>
              <div class="msg-status-sub">{{ t('stage5.dataCenter.detail.waitStatusHint') }}</div>
            </div>
          </div>
        </div>

        <div class="section" v-if="detail.messageLogs && detail.messageLogs.length">
          <div class="section-title">{{ t('stage5.dataCenter.detail.messageHistory') }}</div>
          <el-table :data="detail.messageLogs" border stripe style="width: 100%">
            <el-table-column prop="createdAt" :label="t('stage5.common.fields.time')" min-width="170" />
            <el-table-column prop="type" :label="t('stage5.common.fields.type')" min-width="140" />
            <el-table-column prop="sendStatus" :label="t('stage5.common.fields.status')" min-width="150" />
            <el-table-column prop="errorMessage" :label="t('stage5.common.fields.errorReason')" min-width="260" />
            <el-table-column :label="t('stage5.common.fields.content')" min-width="360">
              <template #default="{ row }">
                <div class="msg-content">{{ row.content }}</div>
              </template>
            </el-table-column>
            <el-table-column :label="t('stage5.common.fields.actions')" width="120">
              <template #default="{ row }">
                <el-button size="small" @click="copyText(row.content)">{{ t('stage5.common.actions.copy') }}</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>
    </div>

    <el-dialog
      v-model="previewDialogVisible"
      :title="t('stage5.common.actions.preview')"
      width="90%"
      class="preview-dialog"
      append-to-body
      @closed="handlePreviewDialogClosed"
      destroy-on-close
    >
      <div class="preview-content">
        <div v-for="guest in previewData" :key="guest.guestIndex" class="preview-guest">
          <div class="preview-guest-title">{{ t('stage5.publicRegistration.common.guest') }} {{ guest.guestIndex }}</div>
          <div class="preview-fields">
            <div v-for="(field, idx) in guest.fields" :key="idx" class="preview-field">
              <div class="preview-label">{{ field.label }}</div>
              <div class="preview-value">{{ field.value }}</div>
            </div>
          </div>
          <div v-if="guest.passportAttachmentId" class="preview-passport">
            <div class="preview-passport-title">{{ t('stage5.publicRegistration.form.passportPhoto') }}</div>
            <img
              v-if="guest.passportUrl"
              :src="guest.passportUrl"
              :alt="t('stage5.publicRegistration.form.passportPhoto')"
              class="preview-passport-img"
            />
          </div>
        </div>
      </div>
    </el-dialog>
  </StatisticsLayout>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import request from '@/utils/request'
import axios from 'axios'

type Detail = {
  formId: number
  orderNumber: string
  channelOrderNumber?: string
  status: string
  reservationStatus?: string | null
  guestName: string
  checkInDate: string
  checkOutDate: string
  roomTypeName?: string
  roomNumber?: string
  reviewNote?: string
  guests: any[]
  attachments?: Array<{ id: number; guestId?: number; type: string; originalName?: string }>
  reviewLogs: any[]
  messageLogs: Array<{
    id: number
    type: string
    channel?: string
    toIdentifier?: string
    content: string
    sendStatus: string
    errorMessage?: string
    createdAt: string
  }>
}

type DetailGuest = {
  id?: number
  sortOrder?: number
  firstName?: string
  lastName?: string
  birthday?: string
  nationality?: string
  residenceType?: string
  address?: string
  phone?: string
  passportNumber?: string
  priorStay?: string
  nextDestination?: string
}

type QuickReplyItem = { id: number; title: string; message: string }
type RegistrationReviewRequest = { note: string; guestMessage?: string; senderName?: string }
type RegistrationReviewMessageLog = { sendStatus?: string; errorMessage?: string }
type RegistrationReviewResponse = {
  messageAttempted?: boolean
  messageLog?: RegistrationReviewMessageLog | null
  messageError?: string | null
}
type ReviewFeedbackOptions = {
  successKey: string
  successWithMessageKey: string
  messageFailedKey: string
  messageStatusKey: string
}
type PreviewGuestItem = {
  guestIndex: number
  fields: Array<{ label: string; value: string }>
  passportAttachmentId?: number
  passportUrl?: string
}

const SENT_MESSAGE_STATUS = 'SENT'
const UNKNOWN_MESSAGE_STATUS = 'UNKNOWN'

const route = useRoute()
const router = useRouter()
const { t } = useI18n()

const detail = ref<Detail | null>(null)
const loading = ref(false)
const acting = ref(false)
const note = ref('')
const approveMessage = ref('')
const reviewQuickReplyId = ref<number | null>(null)
const isCancelledReservation = computed(() => detail.value?.reservationStatus === 'CANCELLED')
const previewDialogVisible = ref(false)
const previewData = ref<PreviewGuestItem[]>([])

const sending = ref(false)
const sendType = ref<'APPROVED_INFO' | 'REJECT_REQUEST' | 'REMINDER'>('APPROVED_INFO')
const sendContent = ref('')
const lastSendStatus = ref<string>('')

const quickReplies = ref<QuickReplyItem[]>([])
const quickReplyLoading = ref(false)
const quickReplyId = ref<number | null>(null)
let previewRequestId = 0

function formId(): string {
  return route.params.formId as string
}

async function load() {
  loading.value = true
  try {
    const resp = await request.get(`/registrations/${formId()}`)
    detail.value = resp.data as Detail
    note.value = detail.value?.reviewNote || ''
    // keep last status hint from newest message log
    lastSendStatus.value = detail.value?.messageLogs?.[0]?.sendStatus || ''
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e?.message || t('stage5.common.messages.dataLoadFailed'))
  } finally {
    loading.value = false
  }
}

async function loadQuickReplies() {
  quickReplyLoading.value = true
  try {
    const resp = await request.get('/quick-replies')
    quickReplies.value = (resp.data || []) as QuickReplyItem[]
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e?.message || t('stage5.common.messages.dataLoadFailed'))
  } finally {
    quickReplyLoading.value = false
  }
}

function applyQuickReply(id: number | null) {
  if (!id) return
  const found = quickReplies.value.find((q) => q.id === id)
  if (!found) return
  // append with a blank line separator
  sendContent.value = appendQuickReplyContent(sendContent.value, found.message || '')
  quickReplyId.value = null
}

function appendQuickReplyContent(currentContent: string, quickReplyContent: string) {
  if (!quickReplyContent) {
    return currentContent
  }
  const base = currentContent?.trim() ? `${currentContent.trim()}\n\n` : ''
  return base + quickReplyContent
}

function applyReviewQuickReply(id: number | null) {
  if (!id) return
  const found = quickReplies.value.find((q) => q.id === id)
  if (!found) return
  approveMessage.value = appendQuickReplyContent(approveMessage.value, found.message || '')
  reviewQuickReplyId.value = null
}

function fillDefaultTemplate() {
  if (!detail.value) return
  if (sendType.value === 'APPROVED_INFO') {
    sendContent.value = t('stage5.dataCenter.detail.defaultApprovedInfo')
  } else if (sendType.value === 'REJECT_REQUEST') {
    sendContent.value = t('stage5.dataCenter.detail.defaultRejectRequest')
  } else {
    sendContent.value = t('stage5.dataCenter.detail.defaultReminder')
  }
}

async function sendMessage() {
  if (!detail.value) return
  const content = sendContent.value?.trim()
  if (!content) {
    ElMessage.warning(t('stage5.dataCenter.detail.contentRequired'))
    return
  }
  sending.value = true
  try {
    const resp = await request.post(`/registrations/${detail.value.formId}/messages/send`, {
      type: sendType.value,
      content,
      senderName: t('stage5.dataCenter.detail.frontDesk'),
    })
    const dto = resp.data
    lastSendStatus.value = dto?.sendStatus || ''
    ElMessage.success(t('stage5.dataCenter.detail.messageSubmitted', { status: dto?.sendStatus || 'OK' }))
    await load()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e?.message || t('stage5.dataCenter.detail.sendFailed'))
  } finally {
    sending.value = false
  }
}

async function copyText(text: string) {
  try {
    await navigator.clipboard.writeText(text || '')
    ElMessage.success(t('stage5.common.messages.copied'))
  } catch {
    // fallback
    const ta = document.createElement('textarea')
    ta.value = text || ''
    document.body.appendChild(ta)
    ta.select()
    document.execCommand('copy')
    ta.remove()
    ElMessage.success(t('stage5.common.messages.copied'))
  }
}

function back() {
  router.back()
}

function buildReviewRequestPayload(messageContent: string): RegistrationReviewRequest {
  const payload: RegistrationReviewRequest = { note: note.value }
  if (messageContent) {
    payload.guestMessage = messageContent
    payload.senderName = t('stage5.dataCenter.detail.frontDesk')
  }
  return payload
}

function normalizeReviewMessageStatus(status?: string | null): string {
  if (!status) {
    return ''
  }
  return status.trim()
}

function buildReviewMessageWarning(
  response: RegistrationReviewResponse | undefined,
  options: ReviewFeedbackOptions,
): string {
  if (!response?.messageAttempted) {
    return ''
  }

  const messageError = response.messageError?.trim()
  if (messageError) {
    return t(options.messageFailedKey, { message: messageError })
  }

  const logError = response.messageLog?.errorMessage?.trim()
  if (logError) {
    return t(options.messageFailedKey, { message: logError })
  }

  const sendStatus = normalizeReviewMessageStatus(response.messageLog?.sendStatus)
  if (!sendStatus) {
    return t(options.messageStatusKey, { status: UNKNOWN_MESSAGE_STATUS })
  }
  if (sendStatus !== SENT_MESSAGE_STATUS) {
    return t(options.messageStatusKey, { status: sendStatus })
  }
  return ''
}

function showReviewFeedback(
  response: RegistrationReviewResponse | undefined,
  messageContent: string,
  options: ReviewFeedbackOptions,
) {
  if (!messageContent) {
    ElMessage.success(t(options.successKey))
    return
  }

  const warning = buildReviewMessageWarning(response, options)
  if (warning) {
    ElMessage.warning(warning)
    return
  }

  ElMessage.success(t(options.successWithMessageKey))
}

async function approve() {
  if (!detail.value) return
  if (isCancelledReservation.value) {
    ElMessage.warning(t('stage5.dataCenter.registrations.cancelled'))
    return
  }
  acting.value = true
  const messageContent = approveMessage.value.trim()
  try {
    const resp = await request.post(
      `/registrations/${detail.value.formId}/approve`,
      buildReviewRequestPayload(messageContent),
    )
    const reviewResponse = resp.data as RegistrationReviewResponse | undefined
    if (messageContent) {
      approveMessage.value = ''
    }
    showReviewFeedback(reviewResponse, messageContent, {
      successKey: 'stage5.dataCenter.detail.approveSuccess',
      successWithMessageKey: 'stage5.dataCenter.detail.approveWithMessageSuccess',
      messageFailedKey: 'stage5.dataCenter.detail.approveMessageFailed',
      messageStatusKey: 'stage5.dataCenter.detail.approveMessageStatus',
    })
    await load()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e?.message || t('stage5.dataCenter.detail.approveFailed'))
  } finally {
    acting.value = false
  }
}

async function reject() {
  if (!detail.value) return
  if (isCancelledReservation.value) {
    ElMessage.warning(t('stage5.dataCenter.registrations.cancelled'))
    return
  }
  acting.value = true
  const messageContent = approveMessage.value.trim()
  try {
    const resp = await request.post(
      `/registrations/${detail.value.formId}/reject`,
      buildReviewRequestPayload(messageContent),
    )
    const reviewResponse = resp.data as RegistrationReviewResponse | undefined
    if (messageContent) {
      approveMessage.value = ''
    }
    showReviewFeedback(reviewResponse, messageContent, {
      successKey: 'stage5.dataCenter.detail.rejectSuccess',
      successWithMessageKey: 'stage5.dataCenter.detail.rejectWithMessageSuccess',
      messageFailedKey: 'stage5.dataCenter.detail.rejectMessageFailed',
      messageStatusKey: 'stage5.dataCenter.detail.rejectMessageStatus',
    })
    await load()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e?.message || t('stage5.common.messages.operationFailed'))
  } finally {
    acting.value = false
  }
}

function downloadPdf() {
  if (!detail.value) return
  const base = (import.meta.env.VITE_API_BASE_URL as string) || '/api/v1'
  const url = `${base}/registrations/${detail.value.formId}/pdf`

  const token = localStorage.getItem('token')
  const currentStore = localStorage.getItem('currentStore')
  let storeId: string | undefined
  if (currentStore) {
    try {
      const parsed = JSON.parse(currentStore)
      if (parsed?.id) storeId = String(parsed.id)
    } catch {
      // ignore
    }
  }

  axios
    .get(url, {
      responseType: 'blob',
      headers: {
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...(storeId ? { 'X-Store-Id': storeId } : {}),
      },
    })
    .then((res) => {
      const blobUrl = URL.createObjectURL(res.data)
      const a = document.createElement('a')
      a.href = blobUrl
      a.download = `registration-${detail.value?.orderNumber || detail.value?.formId}.pdf`
      document.body.appendChild(a)
      a.click()
      a.remove()
      URL.revokeObjectURL(blobUrl)
    })
    .catch((e) => {
      ElMessage.error(e?.response?.data?.message || e?.message || t('stage5.common.messages.downloadFailed'))
    })
}

function guestLabel(guestId?: number): string {
  if (!detail.value || !guestId) return '-'
  const idx = (detail.value.guests || []).findIndex((g: any) => g.id === guestId)
  return idx >= 0 ? `#${idx + 1}` : String(guestId)
}

function authHeaders(): Record<string, string> {
  const token = localStorage.getItem('token')
  const currentStore = localStorage.getItem('currentStore')
  let storeId: string | undefined
  if (currentStore) {
    try {
      const parsed = JSON.parse(currentStore)
      if (parsed?.id) storeId = String(parsed.id)
    } catch {
      // ignore
    }
  }
  return {
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...(storeId ? { 'X-Store-Id': storeId } : {}),
  }
}

function attachmentUrl(attId: number): string {
  const base = (import.meta.env.VITE_API_BASE_URL as string) || '/api/v1'
  return `${base}/registrations/${formId()}/attachments/${attId}`
}

function passportAttachmentId(guestId?: number) {
  if (!detail.value || !guestId) {
    return undefined
  }
  const attachment = detail.value.attachments?.find(
    (att) => att.guestId === guestId && att.type?.toUpperCase().includes('PASSPORT'),
  )
  return attachment?.id
}

function displayPreviewValue(value?: string | null) {
  const normalizedValue = typeof value === 'string' ? value.trim() : ''
  return normalizedValue || '-'
}

function getResidenceTypeLabel(residenceType?: string | null) {
  if (!residenceType) {
    return '-'
  }
  return residenceType === 'JAPAN'
    ? t('stage5.publicRegistration.form.japan')
    : t('stage5.publicRegistration.form.otherThanJapan')
}

function revokePreviewPassportUrls(items: PreviewGuestItem[] = previewData.value) {
  items.forEach((guest) => {
    if (guest.passportUrl?.startsWith('blob:')) {
      URL.revokeObjectURL(guest.passportUrl)
    }
    guest.passportUrl = undefined
  })
}

function handlePreviewDialogClosed() {
  previewRequestId += 1
  revokePreviewPassportUrls()
  previewData.value = []
}

async function loadPreviewPassportImages(requestId: number) {
  await Promise.all(
    previewData.value.map(async (guest, index) => {
      if (!guest.passportAttachmentId) {
        return
      }

      try {
        const res = await axios.get(attachmentUrl(guest.passportAttachmentId), {
          responseType: 'blob',
          headers: authHeaders(),
        })

        if (requestId !== previewRequestId || !previewDialogVisible.value) {
          return
        }

        previewData.value[index].passportUrl = URL.createObjectURL(res.data)
      } catch {
        if (requestId !== previewRequestId) {
          return
        }
        previewData.value[index].passportUrl = undefined
      }
    }),
  )
}

async function previewForm() {
  if (!detail.value) {
    return
  }

  previewRequestId += 1
  const requestId = previewRequestId
  revokePreviewPassportUrls()

  const items = (detail.value.guests || []).map((guest: DetailGuest, idx) => {
    const fields: Array<{ label: string; value: string }> = []
    fields.push({ label: t('stage5.publicRegistration.form.firstName'), value: displayPreviewValue(guest.firstName) })
    fields.push({ label: t('stage5.publicRegistration.form.lastName'), value: displayPreviewValue(guest.lastName) })
    fields.push({
      label: t('stage5.publicRegistration.form.residence'),
      value: getResidenceTypeLabel(guest.residenceType),
    })
    fields.push({ label: t('stage5.publicRegistration.form.birthday'), value: displayPreviewValue(guest.birthday) })
    fields.push({ label: t('stage5.publicRegistration.form.phone'), value: displayPreviewValue(guest.phone) })

    if (guest.residenceType === 'JAPAN') {
      fields.push({ label: t('stage5.publicRegistration.form.address'), value: displayPreviewValue(guest.address) })
    } else {
      fields.push({
        label: t('stage5.publicRegistration.form.passportNumber'),
        value: displayPreviewValue(guest.passportNumber),
      })
      fields.push({
        label: t('stage5.publicRegistration.form.nationality'),
        value: displayPreviewValue(guest.nationality),
      })
      fields.push({ label: t('stage5.publicRegistration.form.address'), value: displayPreviewValue(guest.address) })
      fields.push({ label: t('stage5.publicRegistration.form.priorStay'), value: displayPreviewValue(guest.priorStay) })
      fields.push({
        label: t('stage5.publicRegistration.form.nextDestination'),
        value: displayPreviewValue(guest.nextDestination),
      })
    }

    return {
      guestIndex: idx + 1,
      fields,
      passportAttachmentId: passportAttachmentId(guest.id),
      passportUrl: undefined,
    }
  })

  previewData.value = items
  previewDialogVisible.value = true
  await loadPreviewPassportImages(requestId)
}

function previewAttachment(att: { id: number; originalName?: string }) {
  if (!detail.value) return
  axios
    .get(attachmentUrl(att.id), {
      responseType: 'blob',
      headers: authHeaders(),
    })
    .then((res) => {
      const blobUrl = URL.createObjectURL(res.data)
      window.open(blobUrl, '_blank')
      // best-effort revoke later
      setTimeout(() => URL.revokeObjectURL(blobUrl), 30_000)
    })
    .catch((e) => {
      ElMessage.error(e?.response?.data?.message || e?.message || t('stage5.common.messages.previewFailed'))
    })
}

function downloadAttachment(att: { id: number; originalName?: string }) {
  if (!detail.value) return
  axios
    .get(attachmentUrl(att.id), {
      responseType: 'blob',
      headers: authHeaders(),
    })
    .then((res) => {
      const blobUrl = URL.createObjectURL(res.data)
      const a = document.createElement('a')
      a.href = blobUrl
      a.download = att.originalName || `attachment-${att.id}`
      document.body.appendChild(a)
      a.click()
      a.remove()
      URL.revokeObjectURL(blobUrl)
    })
    .catch((e) => {
      ElMessage.error(e?.response?.data?.message || e?.message || t('stage5.common.messages.downloadFailed'))
    })
}

onMounted(load)
onMounted(loadQuickReplies)
onBeforeUnmount(handlePreviewDialogClosed)
</script>

<style scoped>
.wrap {
  padding: 16px;
}
.top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.title {
  font-size: 16px;
  font-weight: 700;
}
.actions {
  display: flex;
  gap: 10px;
}
.card {
  background: #fff;
  border-radius: 12px;
  padding: 16px;
}
.meta {
  position: relative;
  display: grid;
  grid-template-columns: 1fr;
  gap: 6px;
  margin-bottom: 12px;
}
.meta-actions {
  position: absolute;
  top: 0;
  right: 0;
}
.section {
  margin-top: 14px;
}
.section-title {
  font-weight: 700;
  margin-bottom: 8px;
}
.btns {
  margin-top: 10px;
  display: flex;
  gap: 10px;
  justify-content: flex-end;
}
.approve-message {
  margin-top: 12px;
}

.approve-message__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.approve-message__label {
  margin-bottom: 8px;
  color: #333;
  font-weight: 600;
}

.approve-message__tools {
  display: flex;
  align-items: center;
  gap: 8px;
}

.approve-message__quick-select {
  width: 260px;
}
.empty {
  padding: 20px;
  color: #666;
}

.msg-grid {
  display: grid;
  gap: 10px;
}

.msg-row {
  display: grid;
  grid-template-columns: 90px 1fr;
  gap: 10px;
  align-items: start;
}

.msg-label {
  color: #333;
  padding-top: 6px;
}

.msg-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.msg-hint {
  color: #666;
  font-size: 12px;
  line-height: 1.4;
}

.msg-status {
  color: #333;
  margin-bottom: 4px;
}

.msg-status-sub {
  color: #888;
}

.msg-content {
  white-space: pre-wrap;
}

:deep(.preview-dialog .el-dialog__body) {
  max-height: 70vh;
  overflow-y: auto;
}

.preview-content {
  display: grid;
  gap: 16px;
}

.preview-guest {
  padding: 16px;
  border: 1px solid #ebeef5;
  border-radius: 10px;
  background: #fafafa;
}

.preview-guest-title {
  margin-bottom: 12px;
  font-size: 15px;
  font-weight: 700;
  color: #303133;
}

.preview-fields {
  display: grid;
  gap: 10px;
}

.preview-field {
  display: grid;
  grid-template-columns: 180px 1fr;
  gap: 12px;
  align-items: start;
}

.preview-label {
  color: #606266;
  font-weight: 600;
}

.preview-value {
  color: #303133;
  word-break: break-word;
}

.preview-passport {
  margin-top: 16px;
}

.preview-passport-title {
  margin-bottom: 8px;
  color: #606266;
  font-weight: 600;
}

.preview-passport-img {
  display: block;
  max-width: 100%;
  border-radius: 8px;
  border: 1px solid #dcdfe6;
}
</style>
