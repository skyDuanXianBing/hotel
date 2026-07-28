<template>
  <ion-modal class="record-transaction-modal" :is-open="isOpen" @didDismiss="handleDismiss">
    <ion-header>
      <ion-toolbar>
        <ion-title>{{ t('tools.transaction.title') }}</ion-title>
        <ion-buttons slot="end">
          <ion-button :disabled="submitting" @click="handleDismiss">{{ t('tools.close') }}</ion-button>
        </ion-buttons>
      </ion-toolbar>
    </ion-header>

    <ion-content class="mobile-page mobile-page--dashboard record-modal-page">
      <div class="mobile-stack record-modal-stack">
        <section class="mobile-card mobile-dashboard-surface record-modal-card">
          <div class="record-modal-card__header">
            <h2 class="mobile-section-title">{{ t('tools.transaction.newEntry') }}</h2>
          </div>

          <div class="record-modal-type-row">
            <ion-segment class="record-modal-segment" :value="form.type" @ionChange="handleTypeChange">
              <ion-segment-button value="income">
                <ion-label>{{ t('tools.transaction.income') }}</ion-label>
              </ion-segment-button>
              <ion-segment-button value="expense">
                <ion-label>{{ t('tools.transaction.expense') }}</ion-label>
              </ion-segment-button>
            </ion-segment>
          </div>

          <label class="record-field">
            <span>{{ t('tools.transaction.category') }}</span>
            <ion-select
              v-model="form.category"
              fill="outline"
              interface="action-sheet"
              :placeholder="t('tools.transaction.categoryPlaceholder')"
            >
              <ion-select-option v-for="item in categoryOptions" :key="item.id" :value="item.name">
                {{ item.name }}
              </ion-select-option>
            </ion-select>
          </label>

          <label class="record-field">
            <span>{{ t('tools.transaction.paymentMethod') }}</span>
            <ion-select
              v-model="form.paymentMethod"
              fill="outline"
              interface="action-sheet"
              :placeholder="t('tools.transaction.paymentMethodPlaceholder')"
            >
              <ion-select-option v-for="item in paymentMethodOptions" :key="item.id" :value="item.name">
                {{ item.name }}
              </ion-select-option>
            </ion-select>
          </label>

          <label class="record-field">
            <span>{{ t('tools.transaction.amount') }}</span>
            <ion-input
              v-model="form.amount"
              fill="outline"
              inputmode="decimal"
              :placeholder="t('tools.transaction.amountPlaceholder')"
            />
          </label>

          <label class="record-field">
            <span>{{ t('tools.transaction.room') }}</span>
            <ion-select
              v-model="form.roomId"
              fill="outline"
              interface="action-sheet"
              :placeholder="t('tools.transaction.noRoom')"
            >
              <ion-select-option :value="null">{{ t('tools.transaction.noRoom') }}</ion-select-option>
              <ion-select-option v-for="item in roomOptions" :key="item.id" :value="item.id">
                {{ item.roomNumber }} - {{ item.roomType.name }}
              </ion-select-option>
            </ion-select>
          </label>

          <label class="record-field">
            <span>{{ t('tools.transaction.datetime') }}</span>
            <input v-model="form.datetime" class="record-field__native-input" type="datetime-local" />
          </label>

          <label class="record-field">
            <span>{{ t('tools.transaction.notes') }}</span>
            <ion-textarea
              v-model="form.notes"
              :rows="4"
              fill="outline"
              :placeholder="t('tools.transaction.notesPlaceholder')"
            />
          </label>

          <p v-if="loadNotice" class="mobile-note record-modal-card__notice">{{ loadNotice }}</p>

          <div class="record-modal-card__actions">
            <ion-button fill="outline" :disabled="submitting" @click="handleReset">
              {{ t('tools.transaction.reset') }}
            </ion-button>
            <ion-button :disabled="submitting || dependenciesLoading" @click="handleSubmit">
              {{ submitButtonText }}
            </ion-button>
          </div>
        </section>
      </div>
    </ion-content>
  </ion-modal>
</template>

<script setup lang="ts">
import {
  IonButton,
  IonButtons,
  IonContent,
  IonHeader,
  IonInput,
  IonLabel,
  IonModal,
  IonSegment,
  IonSegmentButton,
  IonSelect,
  IonSelectOption,
  IonTextarea,
  IonTitle,
  IonToolbar,
} from '@ionic/vue'
import { computed, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { getCategoriesByType } from '@/api/noteCategory'
import { createNote, type CreateNoteRequest, type NoteType } from '@/api/notes'
import { getAllPaymentMethods } from '@/api/paymentMethod'
import { getRooms } from '@/api/rooms'
import type { PaymentMethodDTO, NoteCategoryDTO, RoomDTO } from '@/types/settings'
import { isHandledRequestError } from '@/utils/request'
import { showSuccessToast, showWarningToast } from '@/utils/notify'
import {
  toStoreDatetimeLocalValue,
  toStoreServerDatetime,
} from '@/utils/storeBusinessDate'

interface Props {
  isOpen: boolean
}

interface RecordFormState {
  type: NoteType
  category: string
  paymentMethod: string
  amount: string
  roomId: number | null
  datetime: string
  notes: string
}

const props = defineProps<Props>()
const { t } = useI18n()

const emit = defineEmits<{
  dismiss: []
  success: []
}>()

const dependenciesLoading = ref(false)
const submitting = ref(false)
const loadNotice = ref('')
const roomOptions = ref<RoomDTO[]>([])
const categoryOptions = ref<NoteCategoryDTO[]>([])
const paymentMethodOptions = ref<PaymentMethodDTO[]>([])

const form = reactive<RecordFormState>(createDefaultForm())

function createDefaultForm(): RecordFormState {
  return {
    type: 'income',
    category: '',
    paymentMethod: '',
    amount: '',
    roomId: null,
    datetime: toStoreDatetimeLocalValue(),
    notes: '',
  }
}

function resolveWarningMessage(error: unknown, fallbackMessage: string) {
  if (error instanceof Error && error.message) {
    return error.message
  }

  return fallbackMessage
}

async function loadFormOptions() {
  dependenciesLoading.value = true
  loadNotice.value = ''

  try {
    const [categoryResponse, paymentResponse, roomResponse] = await Promise.all([
      getCategoriesByType(form.type),
      getAllPaymentMethods(),
      getRooms(),
    ])

    if (categoryResponse.success && categoryResponse.data) {
      categoryOptions.value = categoryResponse.data
    } else {
      categoryOptions.value = []
    }

    if (paymentResponse.success && paymentResponse.data) {
      const nextMethods: PaymentMethodDTO[] = []

      for (const item of paymentResponse.data) {
        if (item.enabled) {
          nextMethods.push(item)
        }
      }

      paymentMethodOptions.value = nextMethods
    } else {
      paymentMethodOptions.value = []
    }

    if (roomResponse.success && roomResponse.data) {
      roomOptions.value = roomResponse.data
    } else {
      roomOptions.value = []
    }

    ensureSelectableCategory()
  } catch (error) {
    loadNotice.value = resolveWarningMessage(error, t('tools.transaction.loadFailed'))
    if (!isHandledRequestError(error)) {
      showWarningToast(loadNotice.value)
    }
  } finally {
    dependenciesLoading.value = false
  }
}

function ensureSelectableCategory() {
  if (!form.category) {
    return
  }

  let exists = false

  for (const item of categoryOptions.value) {
    if (item.name === form.category) {
      exists = true
      break
    }
  }

  if (!exists) {
    form.category = ''
  }
}

function handleTypeChange(event: CustomEvent) {
  const nextType = event.detail.value as NoteType
  if (!nextType) {
    return
  }

  form.type = nextType
  form.category = ''
  void loadFormOptions()
}

function validateForm() {
  if (!form.category) {
    showWarningToast(t('tools.transaction.categoryRequired'))
    return false
  }

  if (!form.paymentMethod) {
    showWarningToast(t('tools.transaction.paymentMethodRequired'))
    return false
  }

  const amount = Number(form.amount)
  if (!Number.isFinite(amount) || amount <= 0) {
    showWarningToast(t('tools.transaction.amountInvalid'))
    return false
  }

  const datetime = toStoreServerDatetime(form.datetime)
  if (!datetime) {
    showWarningToast(t('tools.transaction.datetimeInvalid'))
    return false
  }

  return true
}

function buildSubmitPayload(): CreateNoteRequest {
  const payload: CreateNoteRequest = {
    type: form.type,
    category: form.category,
    paymentMethod: form.paymentMethod,
    amount: Number(form.amount),
    datetime: toStoreServerDatetime(form.datetime),
    notes: form.notes.trim() || undefined,
  }

  if (typeof form.roomId === 'number') {
    payload.roomId = form.roomId
  }

  return payload
}

function resetFormState() {
  const nextForm = createDefaultForm()
  form.type = nextForm.type
  form.category = nextForm.category
  form.paymentMethod = nextForm.paymentMethod
  form.amount = nextForm.amount
  form.roomId = nextForm.roomId
  form.datetime = nextForm.datetime
  form.notes = nextForm.notes
}

function handleReset() {
  resetFormState()
  void loadFormOptions()
}

function handleDismiss() {
  emit('dismiss')
}

async function handleSubmit() {
  if (!validateForm()) {
    return
  }

  submitting.value = true
  try {
    const response = await createNote(buildSubmitPayload())
    if (!response.success || !response.data) {
      throw new Error(response.message || t('tools.transaction.submitFailed'))
    }

    showSuccessToast(t('tools.transaction.saved'))
    resetFormState()
    emit('success')
    emit('dismiss')
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, t('tools.transaction.submitFailed')))
    }
  } finally {
    submitting.value = false
  }
}

const submitButtonText = computed(() => {
  if (submitting.value) {
    return t('tools.transaction.submitting')
  }

  if (dependenciesLoading.value) {
    return t('tools.transaction.loading')
  }

  return t('tools.transaction.complete')
})

watch(
  () => props.isOpen,
  async (nextOpen) => {
    if (nextOpen) {
      await loadFormOptions()
      return
    }

    resetFormState()
    loadNotice.value = ''
  },
)
</script>

<style scoped>
.record-transaction-modal {
  --background: var(--app-background);
}

.record-transaction-modal ion-toolbar {
  --background: rgba(255, 255, 255, 0.82);
  --border-color: transparent;
  --min-height: 62px;
}

.record-transaction-modal ion-title {
  color: var(--ios-pms-header-title-color);
  font-size: 23px;
  font-weight: 400;
  letter-spacing: 0;
}

.record-transaction-modal ion-header ion-button {
  font-weight: 400;
  letter-spacing: 0;
  --color: var(--ios-pms-header-control-color);
}

.record-modal-page {
  --background: var(--app-background);
  --padding-top: 16px;
  --padding-bottom: calc(28px + var(--app-safe-bottom));
  --padding-start: 16px;
  --padding-end: 16px;
  background: var(--app-background);
}

.record-modal-stack {
  min-height: 100%;
}

.record-modal-card {
  display: grid;
  gap: 18px;
  padding: 30px 16px 24px;
  border-radius: calc(var(--ios-pms-radius-card) + 6px);
  background: var(--ios-pms-dashboard-card-background);
  box-shadow: var(--ios-pms-dashboard-card-shadow);
}

.record-modal-card__header {
  min-width: 0;
}

.record-modal-card__header .mobile-section-title {
  margin: 0;
  color: var(--ios-pms-header-title-color);
  font-size: 22px;
  font-weight: var(--ios-pms-weight-medium);
  line-height: 1.25;
  letter-spacing: 0;
}

.record-modal-type-row {
  min-width: 0;
}

.record-modal-segment {
  width: 100%;
  height: 34px;
  min-height: 34px;
  padding: 0;
  overflow: hidden;
  border: 1px solid rgba(130, 143, 165, 0.18);
  border-radius: var(--ios-pms-radius-pill);
  background: rgba(255, 255, 255, 0.94);
}

.record-modal-segment ion-segment-button {
  --border-radius: var(--ios-pms-radius-pill);
  --color: #111111;
  --color-checked: #ffffff;
  --indicator-color: #343436;
  --indicator-box-shadow: none;
  --padding-start: 6px;
  --padding-end: 6px;
  width: 100%;
  min-width: 0;
  height: 100%;
  min-height: 100%;
  margin: 0;
  color: #111111;
  font-size: 16px;
  font-weight: var(--ios-pms-weight-medium);
  letter-spacing: 0;
}

.record-modal-segment ion-segment-button.segment-button-checked,
.record-modal-segment ion-segment-button[aria-selected='true'] {
  --color: #ffffff;
  --color-checked: #ffffff;
  color: #ffffff;
}

.record-modal-segment ion-segment-button.segment-button-checked ion-label,
.record-modal-segment ion-segment-button[aria-selected='true'] ion-label {
  color: #ffffff;
}

.record-modal-segment ion-segment-button::part(native) {
  min-height: 100%;
  padding: 0 2px;
  border-radius: var(--ios-pms-radius-pill);
}

.record-modal-segment ion-segment-button::part(indicator) {
  padding: 0;
}

.record-modal-segment ion-segment-button::part(indicator-background) {
  border-radius: var(--ios-pms-radius-pill);
  background: #343436;
  box-shadow: none;
}

.record-modal-segment ion-label {
  margin: 0;
  line-height: 1.2;
}

.record-field {
  display: grid;
  gap: 6px;
  min-width: 0;
}

.record-field span {
  color: var(--ios-pms-text-secondary);
  font-size: 14px;
  font-weight: 400;
  line-height: 1.4;
  letter-spacing: 0;
}

.record-field :deep(ion-input),
.record-field :deep(ion-select),
.record-field :deep(ion-textarea) {
  --background: var(--ios-pms-dashboard-card-background);
  --border-color: #d8d8dc;
  --border-radius: 11px;
  --color: var(--ios-pms-text-primary);
  --highlight-color-focused: rgba(var(--ion-color-primary-rgb), 0.42);
  --highlight-color-valid: rgba(var(--ion-color-primary-rgb), 0.42);
  --highlight-color-invalid: var(--ion-color-danger);
  --placeholder-color: rgba(115, 130, 157, 0.78);
  --placeholder-opacity: 1;
  --padding-start: 12px;
  --padding-end: 12px;
  --padding-top: 0;
  --padding-bottom: 0;
  box-sizing: border-box;
  display: block;
  width: 100%;
  min-height: 44px;
  margin: 0;
  border: 1px solid #d8d8dc;
  border-radius: 11px;
  background: var(--ios-pms-dashboard-card-background);
  box-shadow: none;
  overflow: visible;
  font-size: 14px;
  font-weight: 400;
}

.record-field :deep(ion-input.ion-focused),
.record-field :deep(ion-select.select-expanded),
.record-field :deep(ion-textarea.ion-focused) {
  border-color: rgba(var(--ion-color-primary-rgb), 0.42);
  box-shadow: 0 0 0 3px rgba(var(--ion-color-primary-rgb), 0.07);
}

.record-field :deep(ion-textarea) {
  min-height: 72px;
  --padding-top: 10px;
  --padding-bottom: 10px;
}

.record-field__native-input {
  box-sizing: border-box;
  width: 100%;
  min-height: 44px;
  margin: 0;
  padding: 0 12px;
  border: 1px solid #d8d8dc;
  border-radius: 11px;
  outline: none;
  background: var(--ios-pms-dashboard-card-background);
  color: var(--ios-pms-text-primary);
  font: inherit;
  font-size: 14px;
  font-weight: var(--ios-pms-weight-medium);
  line-height: 1.4;
  text-align: center;
  transition:
    border-color 0.2s ease,
    box-shadow 0.2s ease;
}

.record-field__native-input:focus {
  border-color: rgba(var(--ion-color-primary-rgb), 0.42);
  box-shadow: 0 0 0 3px rgba(var(--ion-color-primary-rgb), 0.07);
}

.record-modal-card__notice {
  margin: 0;
  color: var(--ion-color-warning);
  font-size: 12px;
  line-height: 1.5;
}

.record-modal-card__actions {
  display: grid;
  grid-template-columns: minmax(0, 0.92fr) minmax(0, 1.08fr);
  gap: 12px;
  margin-top: 2px;
}

.record-modal-card__actions ion-button {
  width: 100%;
  min-width: 0;
  min-height: 30px;
  height: 30px;
  margin: 0;
  font-size: 13px;
  font-weight: 500;
  letter-spacing: 0;
  --border-radius: 6px;
  --box-shadow: none;
  --padding-top: 0;
  --padding-bottom: 0;
  --padding-start: 10px;
  --padding-end: 10px;
}

.record-modal-card__actions ion-button[fill='outline'] {
  --background: rgba(255, 255, 255, 0.96);
  --border-color: rgba(193, 204, 220, 0.95);
  --border-width: 1px;
  --color: #8a96a8;
}

.record-modal-card__actions ion-button:not([fill='outline']) {
  --background: linear-gradient(180deg, #3191ff 0%, #2687f7 100%);
  --background-hover: #2687f7;
  --background-activated: #1f7eea;
  --box-shadow: 0 4px 10px rgba(52, 116, 246, 0.1);
  --color: #ffffff;
}

.record-modal-card__actions ion-button:not([fill='outline'])::part(native) {
  border: none;
}

@media (max-width: 374px) {
  .record-modal-page {
    --padding-start: 12px;
    --padding-end: 12px;
  }

  .record-modal-card {
    gap: 16px;
    padding-right: 14px;
    padding-left: 14px;
  }

  .record-transaction-modal ion-title {
    font-size: 21px;
  }

  .record-modal-card__header .mobile-section-title {
    font-size: 20px;
  }

  .record-modal-segment ion-segment-button {
    font-size: 14px;
  }
}
</style>
