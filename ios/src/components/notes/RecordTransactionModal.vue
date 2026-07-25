<template>
  <ion-modal :is-open="isOpen" @didDismiss="handleDismiss">
    <ion-header>
      <ion-toolbar>
        <ion-title>{{ t('tools.transaction.title') }}</ion-title>
        <ion-buttons slot="end">
          <ion-button :disabled="submitting" @click="handleDismiss">{{ t('tools.close') }}</ion-button>
        </ion-buttons>
      </ion-toolbar>
    </ion-header>

    <ion-content class="mobile-page record-modal-page">
      <div class="mobile-stack">
        <section class="mobile-card record-modal-card">
          <div>
            <h2 class="mobile-section-title">{{ t('tools.transaction.newEntry') }}</h2>
          </div>

          <div class="record-modal-type-row">
            <ion-segment :value="form.type" @ionChange="handleTypeChange">
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
.record-modal-page {
  --padding-top: 16px;
  --padding-bottom: 24px;
  --padding-start: 16px;
  --padding-end: 16px;
}

.record-modal-card {
  display: grid;
  gap: 14px;
}

.record-modal-type-row {
  display: grid;
  gap: 8px;
}

.record-field {
  display: grid;
  gap: 8px;
}

.record-field span {
  color: var(--app-heading);
  font-size: 13px;
  font-weight: 600;
}

.record-field__native-input {
  width: 100%;
  min-height: 44px;
  padding: 12px 14px;
  border: 1px solid var(--app-border);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.88);
  color: var(--app-heading);
  font: inherit;
}

.record-modal-card__notice {
  color: var(--ion-color-warning);
}

.record-modal-card__actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}
</style>
