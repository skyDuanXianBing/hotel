<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref, shallowRef } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  CircleCheck,
  Delete,
  Document,
  Download,
  Plus,
  Refresh,
  UploadFilled,
} from '@element-plus/icons-vue'
import {
  ElMessage,
  type FormInstance,
  type FormRules,
  type UploadFile,
  type UploadFiles,
  type UploadInstance,
  type UploadRawFile,
} from 'element-plus'
import {
  exportManagedOperationSettlement,
  getManagedOperationSettings,
  getManagedOperationStamp,
  previewManagedOperationSettlement,
  saveManagedOperationSettings,
  uploadManagedOperationStamp,
  type ManagedOperationDeduction,
  type ManagedOperationDocumentType,
  type ManagedOperationLineStatus,
  type ManagedOperationPreview,
  type ManagedOperationPreviewLine,
  type ManagedOperationRoom,
  type ManagedOperationRunRequest,
  type ManagedOperationSettings,
} from '@/api/managedOperationSettlement'
import { managedOperationFileFingerprint } from '@/api/managedOperationSettlementState'

const { t, locale } = useI18n()

const MAX_SHEET_SIZE = 7 * 1024 * 1024
const MAX_STAMP_SIZE = 2 * 1024 * 1024
const ACCEPTED_SHEET_EXTENSIONS = ['csv', 'xls', 'xlsx']
const ACCEPTED_STAMP_TYPES = ['image/png', 'image/jpeg']

const createDefaultSettings = (): ManagedOperationSettings => ({
  propertyName: '',
  selectedRoomIds: [],
  managementFeeRate: 0.1,
  taxRate: 0.1,
  cleaningFeeGross: 8000,
  registrationFeeNet: 2000,
  ownerCompanyName: '',
  ownerContactName: '',
  ownerPostalCode: '',
  ownerAddress: '',
  issuerCompanyName: '',
  issuerPostalCode: '',
  issuerAddress: '',
  issuerRegistrationNumber: '',
  issuerPhone: '',
  issuerEmail: '',
  bankName: '',
  bankBranch: '',
  bankAccountType: '',
  bankAccountNumber: '',
  bankAccountHolder: '',
  hasStamp: false,
})

const pad = (value: number) => String(value).padStart(2, '0')
const toDateValue = (date: Date) =>
  `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
const today = new Date()

const settingsFormRef = ref<FormInstance>()
const loading = ref(false)
const saving = ref(false)
const stampUploading = ref(false)
const previewing = ref(false)
const exportingType = ref<ManagedOperationDocumentType | null>(null)
const availableRooms = ref<ManagedOperationRoom[]>([])
const settings = reactive<ManagedOperationSettings>(createDefaultSettings())
const savedSettingsSnapshot = ref('')
const settingsPersisted = ref(false)

const airbnbUploadRef = ref<UploadInstance>()
const bookingUploadRef = ref<UploadInstance>()
const stampUploadRef = ref<UploadInstance>()
const airbnbFile = shallowRef<File | null>(null)
const bookingFile = shallowRef<File | null>(null)
const airbnbFileRevision = ref(0)
const bookingFileRevision = ref(0)
const stampPreviewUrl = ref('')

const runRequest = reactive<ManagedOperationRunRequest>({
  settlementMonth: `${today.getFullYear()}-${pad(today.getMonth() + 1)}`,
  deductions: [],
  invoiceNumber: '',
  invoiceDate: toDateValue(today),
  paymentDueDate: '',
  receiptNumber: '',
  receiptDate: toDateValue(today),
  note: '',
})

const preview = ref<ManagedOperationPreview | null>(null)
const previewFingerprint = ref('')

const settingsRules: FormRules<ManagedOperationSettings> = {
  propertyName: [
    {
      required: true,
      message: () => t('managedOperation.validation.propertyName'),
      trigger: 'blur',
    },
  ],
  selectedRoomIds: [
    {
      type: 'array',
      required: true,
      min: 1,
      message: () => t('managedOperation.validation.rooms'),
      trigger: 'change',
    },
  ],
  ownerCompanyName: [
    {
      required: true,
      message: () => t('managedOperation.validation.ownerCompany'),
      trigger: 'blur',
    },
  ],
  issuerCompanyName: [
    {
      required: true,
      message: () => t('managedOperation.validation.issuerCompany'),
      trigger: 'blur',
    },
  ],
}

const managementFeePercent = computed({
  get: () => Number((settings.managementFeeRate * 100).toFixed(4)),
  set: (value: number | undefined) => {
    settings.managementFeeRate = Number(value ?? 0) / 100
  },
})

const taxPercent = computed({
  get: () => Number((settings.taxRate * 100).toFixed(4)),
  set: (value: number | undefined) => {
    settings.taxRate = Number(value ?? 0) / 100
  },
})

const serializeSettings = () =>
  JSON.stringify({
    ...settings,
    selectedRoomIds: [...settings.selectedRoomIds].sort((a, b) => a - b),
  })

const settingsDirty = computed(
  () => Boolean(savedSettingsSnapshot.value) && serializeSettings() !== savedSettingsSnapshot.value,
)
const settingsUnsaved = computed(() => !settingsPersisted.value || settingsDirty.value)

const currentFingerprint = computed(() =>
  JSON.stringify({
    settings: serializeSettings(),
    runRequest,
    airbnb: managedOperationFileFingerprint(airbnbFile.value, airbnbFileRevision.value),
    booking: managedOperationFileFingerprint(bookingFile.value, bookingFileRevision.value),
  }),
)

const previewStale = computed(
  () => Boolean(preview.value) && previewFingerprint.value !== currentFingerprint.value,
)

const missingFiles = computed(() => !airbnbFile.value || !bookingFile.value)
const exportDisabled = computed(
  () =>
    !preview.value?.exportAllowed ||
    settingsUnsaved.value ||
    previewStale.value ||
    missingFiles.value ||
    Boolean(exportingType.value),
)

const selectedRoomCount = computed(() => settings.selectedRoomIds.length)
const registrationFeeGross = computed(() =>
  Math.round(selectedRoomCount.value * settings.registrationFeeNet * (1 + settings.taxRate)),
)

const statusOrder: ManagedOperationLineStatus[] = [
  'INCLUDED',
  'UNMATCHED',
  'AMBIGUOUS',
  'ROOM_EXCLUDED',
  'PERIOD_EXCLUDED',
  'CANCELLED',
]

const statusCounts = computed(() => {
  const counts = Object.fromEntries(statusOrder.map((status) => [status, 0])) as Record<
    ManagedOperationLineStatus,
    number
  >
  for (const line of preview.value?.lines ?? []) {
    if (statusOrder.includes(line.status as ManagedOperationLineStatus)) {
      counts[line.status as ManagedOperationLineStatus] += 1
    }
  }
  return counts
})

const blockingReasons = computed(() => preview.value?.blockingReasons ?? [])

const statusType = (status: string) => {
  const types: Record<string, 'success' | 'warning' | 'danger' | 'info'> = {
    INCLUDED: 'success',
    PERIOD_EXCLUDED: 'info',
    UNMATCHED: 'danger',
    AMBIGUOUS: 'danger',
    ROOM_EXCLUDED: 'warning',
    CANCELLED: 'info',
  }
  return types[status] ?? 'info'
}

const statusLabel = (status: string) => {
  const key = `managedOperation.status.${status}`
  const translated = t(key)
  return translated === key ? status : translated
}

const moneyFormatter = computed(
  () =>
    new Intl.NumberFormat(locale.value, {
      style: 'currency',
      currency: 'JPY',
      maximumFractionDigits: 0,
    }),
)
const formatMoney = (value?: number | null) => moneyFormatter.value.format(Number(value ?? 0))
const formatOptionalMoney = (value?: number | null) => (value == null ? '—' : formatMoney(value))
const formatRate = (value: number) => `${(Number(value || 0) * 100).toFixed(2)}%`

const replaceSettings = (value: ManagedOperationSettings, persisted: boolean) => {
  Object.assign(settings, createDefaultSettings(), value)
  savedSettingsSnapshot.value = serializeSettings()
  settingsPersisted.value = persisted
}

const revokeStampPreview = () => {
  if (stampPreviewUrl.value) {
    URL.revokeObjectURL(stampPreviewUrl.value)
    stampPreviewUrl.value = ''
  }
}

const loadStampPreview = async () => {
  revokeStampPreview()
  if (!settings.hasStamp) return
  try {
    const blob = await getManagedOperationStamp()
    if (blob instanceof Blob && blob.size > 0) {
      stampPreviewUrl.value = URL.createObjectURL(blob)
    }
  } catch {
    // The optional preview must not prevent the rest of the settings page from loading.
  }
}

const loadSettings = async () => {
  loading.value = true
  try {
    const response = await getManagedOperationSettings()
    availableRooms.value = response.availableRooms
    replaceSettings(response.settings, response.persisted)
    await loadStampPreview()
  } catch (error) {
    console.error('Failed to load managed operation settings:', error)
    ElMessage.error(t('managedOperation.messages.loadFailed'))
  } finally {
    loading.value = false
  }
}

const handleSaveSettings = async () => {
  if (!(await settingsFormRef.value?.validate().catch(() => false))) return
  saving.value = true
  try {
    const response = await saveManagedOperationSettings({
      ...settings,
      selectedRoomIds: [...settings.selectedRoomIds],
    })
    if (!response.persisted) throw new Error('SETTINGS_NOT_PERSISTED')
    if (response.availableRooms.length) availableRooms.value = response.availableRooms
    replaceSettings(response.settings, true)
    ElMessage.success(t('managedOperation.messages.saved'))
  } catch (error) {
    console.error('Failed to save managed operation settings:', error)
    ElMessage.error(t('managedOperation.messages.saveFailed'))
  } finally {
    saving.value = false
  }
}

const spreadsheetValidationError = (file: File) => {
  const extension = file.name.split('.').pop()?.toLowerCase() ?? ''
  if (!ACCEPTED_SHEET_EXTENSIONS.includes(extension)) {
    return t('managedOperation.validation.fileType')
  }
  if (!file.size) return t('managedOperation.validation.emptyFile')
  if (file.size > MAX_SHEET_SIZE) return t('managedOperation.validation.fileSize')
  return ''
}

const validateAndKeepSheet = (kind: 'airbnb' | 'booking', raw: UploadRawFile) => {
  const message = spreadsheetValidationError(raw)
  if (message) {
    ElMessage.warning(message)
    if (kind === 'airbnb') {
      if (airbnbFile.value) {
        airbnbFile.value = null
        airbnbFileRevision.value += 1
      }
      airbnbUploadRef.value?.clearFiles()
    } else {
      if (bookingFile.value) {
        bookingFile.value = null
        bookingFileRevision.value += 1
      }
      bookingUploadRef.value?.clearFiles()
    }
    return false
  }
  if (kind === 'airbnb') {
    airbnbFile.value = raw
    airbnbFileRevision.value += 1
  } else {
    bookingFile.value = raw
    bookingFileRevision.value += 1
  }
  return true
}

const handleSheetChange = (kind: 'airbnb' | 'booking', file: UploadFile, _files: UploadFiles) => {
  if (file.raw) validateAndKeepSheet(kind, file.raw)
}

const handleSheetExceed = (kind: 'airbnb' | 'booking', files: File[]) => {
  const raw = files[0] as UploadRawFile | undefined
  if (!raw || spreadsheetValidationError(raw)) {
    if (raw) ElMessage.warning(spreadsheetValidationError(raw))
    return
  }
  const instance = kind === 'airbnb' ? airbnbUploadRef.value : bookingUploadRef.value
  instance?.clearFiles()
  raw.uid = Date.now()
  instance?.handleStart(raw)
}

const handleSheetRemove = (kind: 'airbnb' | 'booking') => {
  if (kind === 'airbnb' && airbnbFile.value) {
    airbnbFile.value = null
    airbnbFileRevision.value += 1
  } else if (kind === 'booking' && bookingFile.value) {
    bookingFile.value = null
    bookingFileRevision.value += 1
  }
}

const handleStampChange = async (file: UploadFile) => {
  const raw = file.raw
  if (!raw) return
  const extension = raw.name.split('.').pop()?.toLowerCase()
  if (
    !ACCEPTED_STAMP_TYPES.includes(raw.type) ||
    !['png', 'jpg', 'jpeg'].includes(extension ?? '')
  ) {
    ElMessage.warning(t('managedOperation.validation.stampType'))
    stampUploadRef.value?.clearFiles()
    return
  }
  if (!raw.size || raw.size > MAX_STAMP_SIZE) {
    ElMessage.warning(t('managedOperation.validation.stampSize'))
    stampUploadRef.value?.clearFiles()
    return
  }

  stampUploading.value = true
  try {
    const result = await uploadManagedOperationStamp(raw)
    settings.hasStamp = result.hasStamp
    const savedSettings = JSON.parse(
      savedSettingsSnapshot.value || '{}',
    ) as Partial<ManagedOperationSettings>
    savedSettingsSnapshot.value = JSON.stringify({ ...savedSettings, hasStamp: result.hasStamp })
    await loadStampPreview()
    ElMessage.success(t('managedOperation.messages.stampUploaded'))
  } catch (error) {
    console.error('Failed to upload managed operation stamp:', error)
    ElMessage.error(t('managedOperation.messages.stampFailed'))
  } finally {
    stampUploading.value = false
    stampUploadRef.value?.clearFiles()
  }
}

const addDeduction = () => {
  runRequest.deductions.push({ description: '', amountGross: 0 })
}

const removeDeduction = (index: number) => {
  runRequest.deductions.splice(index, 1)
}

const validateRunRequest = () => {
  if (settingsUnsaved.value) {
    ElMessage.warning(t('managedOperation.messages.saveBeforePreview'))
    return false
  }
  if (!runRequest.settlementMonth) {
    ElMessage.warning(t('managedOperation.validation.month'))
    return false
  }
  if (!airbnbFile.value || !bookingFile.value) {
    ElMessage.warning(t('managedOperation.validation.bothFiles'))
    return false
  }
  const invalidDeduction = runRequest.deductions.some(
    (item) => !item.description.trim() || Number(item.amountGross) < 0,
  )
  if (invalidDeduction) {
    ElMessage.warning(t('managedOperation.validation.deduction'))
    return false
  }
  return true
}

const cleanRunRequest = (): ManagedOperationRunRequest => ({
  ...runRequest,
  deductions: runRequest.deductions.map((item) => ({
    description: item.description.trim(),
    amountGross: Number(item.amountGross),
  })),
})

const handlePreview = async () => {
  if (!validateRunRequest() || !airbnbFile.value || !bookingFile.value) return
  previewing.value = true
  try {
    const result = await previewManagedOperationSettlement(
      airbnbFile.value,
      bookingFile.value,
      cleanRunRequest(),
    )
    preview.value = result
    previewFingerprint.value = currentFingerprint.value
    ElMessage.success(t('managedOperation.messages.previewReady'))
  } catch (error) {
    console.error('Failed to preview managed operation settlement:', error)
    ElMessage.error(t('managedOperation.messages.previewFailed'))
  } finally {
    previewing.value = false
  }
}

const saveDownload = (blob: Blob, fileName: string) => {
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  document.body.appendChild(link)
  link.click()
  link.remove()
  URL.revokeObjectURL(url)
}

const handleExport = async (documentType: ManagedOperationDocumentType) => {
  if (exportDisabled.value || !airbnbFile.value || !bookingFile.value) {
    ElMessage.warning(
      previewStale.value
        ? t('managedOperation.messages.previewStale')
        : t('managedOperation.messages.exportBlocked'),
    )
    return
  }
  exportingType.value = documentType
  try {
    const download = await exportManagedOperationSettlement(
      documentType,
      airbnbFile.value,
      bookingFile.value,
      cleanRunRequest(),
    )
    saveDownload(download.blob, download.fileName)
    ElMessage.success(t('managedOperation.messages.downloadStarted'))
  } catch (error) {
    const message =
      error instanceof Error && error.message !== 'DOWNLOAD_FAILED' ? error.message : ''
    ElMessage.error(message || t('managedOperation.messages.exportFailed'))
  } finally {
    exportingType.value = null
  }
}

const rowClassName = ({ row }: { row: ManagedOperationPreviewLine }) =>
  row.status === 'INCLUDED' ? '' : 'excluded-row'

onMounted(loadSettings)
onBeforeUnmount(revokeStampPreview)
</script>

<template>
  <div v-loading="loading" class="managed-operation-page">
    <header class="page-header">
      <div>
        <div class="eyebrow">{{ t('managedOperation.eyebrow') }}</div>
        <h1>{{ t('managedOperation.title') }}</h1>
        <p>{{ t('managedOperation.description') }}</p>
      </div>
      <el-tag v-if="settingsUnsaved" type="warning" effect="plain">
        {{ t('managedOperation.unsaved') }}
      </el-tag>
      <el-tag v-else type="success" effect="plain">
        <el-icon><CircleCheck /></el-icon>
        {{ t('managedOperation.saved') }}
      </el-tag>
    </header>

    <el-alert
      v-if="settingsUnsaved"
      class="state-alert"
      type="warning"
      :title="t('managedOperation.messages.unsavedConfig')"
      :closable="false"
      show-icon
    />

    <el-form
      ref="settingsFormRef"
      :model="settings"
      :rules="settingsRules"
      label-position="top"
      status-icon
    >
      <el-card shadow="never" class="section-card">
        <template #header>
          <div class="card-heading">
            <div>
              <h2>{{ t('managedOperation.sections.configuration') }}</h2>
              <p>{{ t('managedOperation.sections.configurationHint') }}</p>
            </div>
            <el-button type="primary" :loading="saving" @click="handleSaveSettings">
              {{ t('managedOperation.actions.saveConfiguration') }}
            </el-button>
          </div>
        </template>

        <div class="form-grid form-grid--four">
          <el-form-item prop="propertyName" :label="t('managedOperation.fields.propertyName')">
            <el-input
              v-model="settings.propertyName"
              maxlength="100"
              show-word-limit
              :placeholder="t('managedOperation.placeholders.propertyName')"
            />
          </el-form-item>
          <el-form-item
            prop="selectedRoomIds"
            class="span-two"
            :label="t('managedOperation.fields.rooms')"
          >
            <el-select
              v-model="settings.selectedRoomIds"
              multiple
              filterable
              collapse-tags
              collapse-tags-tooltip
              :placeholder="t('managedOperation.placeholders.rooms')"
            >
              <el-option
                v-for="room in availableRooms"
                :key="room.id"
                :label="
                  room.roomTypeName ? `${room.roomNumber} · ${room.roomTypeName}` : room.roomNumber
                "
                :value="room.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item :label="t('managedOperation.fields.managementFeeRate')">
            <el-input-number
              v-model="managementFeePercent"
              :min="0"
              :max="100"
              :precision="2"
              :step="0.1"
            />
            <span class="field-suffix">%</span>
          </el-form-item>
          <el-form-item :label="t('managedOperation.fields.taxRate')">
            <el-input-number v-model="taxPercent" :min="0" :max="100" :precision="2" :step="0.1" />
            <span class="field-suffix">%</span>
          </el-form-item>
          <el-form-item :label="t('managedOperation.fields.cleaningFeeGross')">
            <el-input-number
              v-model="settings.cleaningFeeGross"
              :min="0"
              :precision="0"
              :controls="false"
            />
            <span class="field-suffix">JPY</span>
          </el-form-item>
          <el-form-item :label="t('managedOperation.fields.registrationFeeNet')">
            <el-input-number
              v-model="settings.registrationFeeNet"
              :min="0"
              :precision="0"
              :controls="false"
            />
            <span class="field-suffix">JPY</span>
          </el-form-item>
        </div>

        <el-divider />

        <div class="info-columns">
          <section class="info-panel">
            <h3>{{ t('managedOperation.sections.owner') }}</h3>
            <el-form-item prop="ownerCompanyName" :label="t('managedOperation.fields.companyName')">
              <el-input v-model="settings.ownerCompanyName" maxlength="100" />
            </el-form-item>
            <div class="form-grid form-grid--two">
              <el-form-item :label="t('managedOperation.fields.contactName')">
                <el-input v-model="settings.ownerContactName" maxlength="100" />
              </el-form-item>
              <el-form-item :label="t('managedOperation.fields.postalCode')">
                <el-input v-model="settings.ownerPostalCode" maxlength="20" />
              </el-form-item>
            </div>
            <el-form-item :label="t('managedOperation.fields.address')">
              <el-input v-model="settings.ownerAddress" maxlength="255" />
            </el-form-item>
          </section>

          <section class="info-panel">
            <h3>{{ t('managedOperation.sections.issuer') }}</h3>
            <el-form-item
              prop="issuerCompanyName"
              :label="t('managedOperation.fields.companyName')"
            >
              <el-input v-model="settings.issuerCompanyName" maxlength="100" />
            </el-form-item>
            <div class="form-grid form-grid--two">
              <el-form-item :label="t('managedOperation.fields.postalCode')">
                <el-input v-model="settings.issuerPostalCode" maxlength="20" />
              </el-form-item>
              <el-form-item :label="t('managedOperation.fields.registrationNumber')">
                <el-input v-model="settings.issuerRegistrationNumber" maxlength="30" />
              </el-form-item>
            </div>
            <el-form-item :label="t('managedOperation.fields.address')">
              <el-input v-model="settings.issuerAddress" maxlength="255" />
            </el-form-item>
            <div class="form-grid form-grid--two">
              <el-form-item :label="t('managedOperation.fields.phone')">
                <el-input v-model="settings.issuerPhone" maxlength="40" />
              </el-form-item>
              <el-form-item :label="t('managedOperation.fields.email')">
                <el-input v-model="settings.issuerEmail" maxlength="120" />
              </el-form-item>
            </div>
          </section>
        </div>

        <el-divider />

        <div class="info-columns">
          <section class="info-panel">
            <h3>{{ t('managedOperation.sections.bank') }}</h3>
            <div class="form-grid form-grid--two">
              <el-form-item :label="t('managedOperation.fields.bankName')">
                <el-input v-model="settings.bankName" maxlength="100" />
              </el-form-item>
              <el-form-item :label="t('managedOperation.fields.bankBranch')">
                <el-input v-model="settings.bankBranch" maxlength="100" />
              </el-form-item>
              <el-form-item :label="t('managedOperation.fields.bankAccountType')">
                <el-input v-model="settings.bankAccountType" maxlength="30" />
              </el-form-item>
              <el-form-item :label="t('managedOperation.fields.bankAccountNumber')">
                <el-input v-model="settings.bankAccountNumber" maxlength="40" />
              </el-form-item>
            </div>
            <el-form-item :label="t('managedOperation.fields.bankAccountHolder')">
              <el-input v-model="settings.bankAccountHolder" maxlength="100" />
            </el-form-item>
          </section>

          <section class="info-panel stamp-panel">
            <h3>{{ t('managedOperation.sections.stamp') }}</h3>
            <p>{{ t('managedOperation.sections.stampHint') }}</p>
            <div class="stamp-row">
              <div class="stamp-preview" :class="{ 'has-image': stampPreviewUrl }">
                <img
                  v-if="stampPreviewUrl"
                  :src="stampPreviewUrl"
                  :alt="t('managedOperation.fields.stamp')"
                />
                <span v-else>{{ t('managedOperation.noStamp') }}</span>
              </div>
              <el-upload
                ref="stampUploadRef"
                action="#"
                accept=".png,.jpg,.jpeg,image/png,image/jpeg"
                :auto-upload="false"
                :show-file-list="false"
                :disabled="stampUploading"
                :on-change="handleStampChange"
              >
                <el-button :loading="stampUploading">
                  <el-icon><UploadFilled /></el-icon>
                  {{ t('managedOperation.actions.uploadStamp') }}
                </el-button>
              </el-upload>
            </div>
          </section>
        </div>
      </el-card>
    </el-form>

    <el-card shadow="never" class="section-card">
      <template #header>
        <div class="card-heading">
          <div>
            <h2>{{ t('managedOperation.sections.currentSettlement') }}</h2>
            <p>{{ t('managedOperation.sections.currentSettlementHint') }}</p>
          </div>
        </div>
      </template>

      <div class="form-grid form-grid--four run-fields">
        <el-form-item :label="t('managedOperation.fields.settlementMonth')" required>
          <el-date-picker
            v-model="runRequest.settlementMonth"
            type="month"
            value-format="YYYY-MM"
            :placeholder="t('managedOperation.placeholders.month')"
          />
        </el-form-item>
        <el-form-item :label="t('managedOperation.fields.invoiceNumber')">
          <el-input v-model="runRequest.invoiceNumber" maxlength="50" />
        </el-form-item>
        <el-form-item :label="t('managedOperation.fields.invoiceDate')">
          <el-date-picker v-model="runRequest.invoiceDate" type="date" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item :label="t('managedOperation.fields.paymentDueDate')">
          <el-date-picker
            v-model="runRequest.paymentDueDate"
            type="date"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>
        <el-form-item :label="t('managedOperation.fields.receiptNumber')">
          <el-input v-model="runRequest.receiptNumber" maxlength="50" />
        </el-form-item>
        <el-form-item :label="t('managedOperation.fields.receiptDate')">
          <el-date-picker v-model="runRequest.receiptDate" type="date" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item class="span-two" :label="t('managedOperation.fields.note')">
          <el-input v-model="runRequest.note" maxlength="500" show-word-limit />
        </el-form-item>
      </div>

      <div class="upload-grid">
        <div class="upload-panel">
          <div class="upload-title">
            <span class="platform-mark platform-mark--airbnb">A</span>
            <div>
              <strong>{{ t('managedOperation.upload.airbnb') }}</strong>
              <span>{{ t('managedOperation.upload.formats') }}</span>
            </div>
          </div>
          <el-upload
            ref="airbnbUploadRef"
            drag
            action="#"
            accept=".csv,.xls,.xlsx"
            :auto-upload="false"
            :limit="1"
            :on-change="
              (file: UploadFile, files: UploadFiles) => handleSheetChange('airbnb', file, files)
            "
            :on-exceed="(files: File[]) => handleSheetExceed('airbnb', files)"
            :on-remove="() => handleSheetRemove('airbnb')"
          >
            <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
            <div class="el-upload__text">{{ t('managedOperation.upload.drop') }}</div>
          </el-upload>
        </div>

        <div class="upload-panel">
          <div class="upload-title">
            <span class="platform-mark platform-mark--booking">B</span>
            <div>
              <strong>{{ t('managedOperation.upload.booking') }}</strong>
              <span>{{ t('managedOperation.upload.formats') }}</span>
            </div>
          </div>
          <el-upload
            ref="bookingUploadRef"
            drag
            action="#"
            accept=".csv,.xls,.xlsx"
            :auto-upload="false"
            :limit="1"
            :on-change="
              (file: UploadFile, files: UploadFiles) => handleSheetChange('booking', file, files)
            "
            :on-exceed="(files: File[]) => handleSheetExceed('booking', files)"
            :on-remove="() => handleSheetRemove('booking')"
          >
            <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
            <div class="el-upload__text">{{ t('managedOperation.upload.drop') }}</div>
          </el-upload>
        </div>
      </div>
    </el-card>

    <el-card shadow="never" class="section-card">
      <template #header>
        <div class="card-heading">
          <div>
            <h2>{{ t('managedOperation.sections.deductions') }}</h2>
            <p>{{ t('managedOperation.sections.deductionsHint') }}</p>
          </div>
          <el-button :icon="Plus" @click="addDeduction">
            {{ t('managedOperation.actions.addDeduction') }}
          </el-button>
        </div>
      </template>

      <div class="automatic-fee">
        <div>
          <strong>{{ t('managedOperation.registrationFee') }}</strong>
          <span>
            {{ selectedRoomCount }} × {{ formatMoney(settings.registrationFeeNet) }} ·
            {{ t('managedOperation.taxIncluded') }}
          </span>
        </div>
        <b>{{ formatMoney(registrationFeeGross) }}</b>
      </div>

      <div v-if="runRequest.deductions.length" class="deduction-list">
        <div v-for="(item, index) in runRequest.deductions" :key="index" class="deduction-row">
          <el-input
            v-model="item.description"
            maxlength="100"
            :placeholder="t('managedOperation.placeholders.deductionDescription')"
          />
          <el-input-number
            v-model="item.amountGross"
            :min="0"
            :precision="0"
            :controls="false"
            :placeholder="t('managedOperation.placeholders.amount')"
          />
          <span>JPY</span>
          <el-button
            text
            type="danger"
            :icon="Delete"
            :aria-label="t('managedOperation.actions.removeDeduction')"
            @click="removeDeduction(index)"
          />
        </div>
      </div>
      <el-empty v-else :image-size="64" :description="t('managedOperation.noDeductions')" />

      <div class="preview-action">
        <div>
          <strong>{{ t('managedOperation.preview.checkTitle') }}</strong>
          <span>{{ t('managedOperation.preview.checkHint') }}</span>
        </div>
        <el-button
          type="primary"
          size="large"
          :icon="Refresh"
          :loading="previewing"
          :disabled="settingsUnsaved || missingFiles"
          @click="handlePreview"
        >
          {{
            preview
              ? t('managedOperation.actions.recalculate')
              : t('managedOperation.actions.preview')
          }}
        </el-button>
      </div>
    </el-card>

    <template v-if="preview">
      <el-alert
        v-if="previewStale"
        class="state-alert"
        type="warning"
        :title="t('managedOperation.messages.previewStale')"
        :closable="false"
        show-icon
      />
      <el-alert
        v-if="blockingReasons.length"
        class="state-alert"
        type="error"
        :title="t('managedOperation.preview.blocked')"
        :closable="false"
        show-icon
      >
        <ul class="blocking-list">
          <li v-for="reason in blockingReasons" :key="reason">{{ reason }}</li>
        </ul>
      </el-alert>

      <el-card shadow="never" class="section-card preview-card">
        <template #header>
          <div class="card-heading">
            <div>
              <h2>{{ t('managedOperation.sections.preview') }}</h2>
              <p>{{ t('managedOperation.sections.previewHint') }}</p>
            </div>
            <el-tag :type="preview.exportAllowed ? 'success' : 'danger'" effect="dark">
              {{
                preview.exportAllowed
                  ? t('managedOperation.preview.exportReady')
                  : t('managedOperation.preview.exportNotReady')
              }}
            </el-tag>
          </div>
        </template>

        <div class="status-strip">
          <div v-for="status in statusOrder" :key="status" class="status-stat">
            <span>{{ statusLabel(status) }}</span>
            <strong>{{ statusCounts[status] }}</strong>
          </div>
        </div>

        <el-table
          :data="preview.lines"
          max-height="540"
          stripe
          :row-class-name="rowClassName"
          empty-text="—"
        >
          <el-table-column
            prop="platform"
            :label="t('managedOperation.table.platform')"
            width="100"
            fixed
          />
          <el-table-column :label="t('managedOperation.table.status')" width="132" fixed>
            <template #default="{ row }">
              <el-tag :type="statusType(row.status)" size="small">
                {{ statusLabel(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column
            prop="sourceRowNumber"
            :label="t('managedOperation.table.row')"
            width="74"
          />
          <el-table-column
            prop="bookingKey"
            :label="t('managedOperation.table.bookingKey')"
            min-width="150"
          />
          <el-table-column
            prop="guestName"
            :label="t('managedOperation.table.guest')"
            min-width="140"
            show-overflow-tooltip
          />
          <el-table-column
            prop="roomNumber"
            :label="t('managedOperation.table.room')"
            width="100"
          />
          <el-table-column
            prop="checkInDate"
            :label="t('managedOperation.table.checkIn')"
            width="112"
          />
          <el-table-column
            prop="checkOutDate"
            :label="t('managedOperation.table.checkOut')"
            width="112"
          />
          <el-table-column
            :label="t('managedOperation.table.grossSales')"
            width="126"
            align="right"
          >
            <template #default="{ row }">{{ formatMoney(row.grossSales) }}</template>
          </el-table-column>
          <el-table-column :label="t('managedOperation.table.otaFee')" width="116" align="right">
            <template #default="{ row }">{{ formatMoney(row.otaServiceFee) }}</template>
          </el-table-column>
          <el-table-column :label="t('managedOperation.table.received')" width="126" align="right">
            <template #default="{ row }">{{ formatOptionalMoney(row.receivedAmount) }}</template>
          </el-table-column>
          <el-table-column
            :label="t('managedOperation.table.managementFee')"
            width="126"
            align="right"
          >
            <template #default="{ row }">{{ formatOptionalMoney(row.managementFee) }}</template>
          </el-table-column>
          <el-table-column :label="t('managedOperation.table.transfer')" width="126" align="right">
            <template #default="{ row }">{{ formatOptionalMoney(row.scheduledTransfer) }}</template>
          </el-table-column>
          <el-table-column :label="t('managedOperation.table.warnings')" min-width="220">
            <template #default="{ row }">
              <span v-if="row.warnings?.length" class="warning-copy">{{
                row.warnings.join(' · ')
              }}</span>
              <span v-else>—</span>
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <section class="summary-section">
        <div class="summary-heading">
          <div>
            <span>{{ t('managedOperation.summary.includedCount') }}</span>
            <strong>{{ preview.summary.includedReservationCount }}</strong>
          </div>
          <div>
            <span>{{ t('managedOperation.summary.selectedRooms') }}</span>
            <strong>{{ preview.summary.selectedRoomCount }}</strong>
          </div>
          <div>
            <span>{{ t('managedOperation.summary.managementRate') }}</span>
            <strong>{{ formatRate(settings.managementFeeRate) }}</strong>
          </div>
        </div>
        <div class="money-grid">
          <article>
            <span>{{ t('managedOperation.summary.totalReceived') }}</span>
            <strong>{{ formatMoney(preview.summary.totalReceived) }}</strong>
          </article>
          <article>
            <span>{{ t('managedOperation.summary.managementFee') }}</span>
            <strong>{{ formatMoney(preview.summary.managementFeeNet) }}</strong>
          </article>
          <article>
            <span>{{ t('managedOperation.summary.cleaningTax') }}</span>
            <strong>{{ formatMoney(preview.summary.cleaningTax) }}</strong>
          </article>
          <article>
            <span>{{ t('managedOperation.summary.managementTax') }}</span>
            <strong>{{ formatMoney(preview.summary.managementTax) }}</strong>
          </article>
          <article>
            <span>{{ t('managedOperation.summary.deductions') }}</span>
            <strong>
              {{
                formatMoney(
                  preview.summary.registrationFeeGross + preview.summary.otherDeductionsGross,
                )
              }}
            </strong>
          </article>
          <article class="money-card--accent">
            <span>{{ t('managedOperation.summary.finalTransfer') }}</span>
            <strong>{{ formatMoney(preview.summary.finalTransfer) }}</strong>
          </article>
          <article class="money-card--invoice">
            <span>{{ t('managedOperation.summary.invoiceTotal') }}</span>
            <strong>{{ formatMoney(preview.summary.invoiceTotalGross) }}</strong>
          </article>
        </div>
      </section>

      <el-card shadow="never" class="section-card download-card">
        <div class="download-copy">
          <el-icon><Document /></el-icon>
          <div>
            <h2>{{ t('managedOperation.sections.download') }}</h2>
            <p>{{ t('managedOperation.sections.downloadHint') }}</p>
          </div>
        </div>
        <div class="download-actions">
          <el-button
            :loading="exportingType === 'settlement'"
            :disabled="exportDisabled"
            @click="handleExport('settlement')"
          >
            {{ t('managedOperation.actions.downloadSettlement') }}
          </el-button>
          <el-button
            :loading="exportingType === 'invoice'"
            :disabled="exportDisabled"
            @click="handleExport('invoice')"
          >
            {{ t('managedOperation.actions.downloadInvoice') }}
          </el-button>
          <el-button
            :loading="exportingType === 'receipt'"
            :disabled="exportDisabled"
            @click="handleExport('receipt')"
          >
            {{ t('managedOperation.actions.downloadReceipt') }}
          </el-button>
          <el-button
            type="primary"
            :icon="Download"
            :loading="exportingType === 'all'"
            :disabled="exportDisabled"
            @click="handleExport('all')"
          >
            {{ t('managedOperation.actions.downloadAll') }}
          </el-button>
        </div>
      </el-card>
    </template>
  </div>
</template>

<style scoped>
.managed-operation-page {
  min-height: 100%;
  padding: 4px 24px 40px;
  color: #1f2937;
}

.page-header,
.card-heading,
.preview-action,
.download-card :deep(.el-card__body) {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
}

.page-header {
  padding: 20px 0 24px;
}

.page-header h1,
.card-heading h2,
.download-copy h2 {
  margin: 0;
  color: #111827;
}

.page-header h1 {
  font-size: 25px;
  line-height: 1.3;
}

.page-header p,
.card-heading p,
.download-copy p,
.stamp-panel > p {
  margin: 6px 0 0;
  color: #6b7280;
  font-size: 13px;
  line-height: 1.6;
}

.eyebrow {
  margin-bottom: 5px;
  color: #2563eb;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.page-header .el-tag {
  display: inline-flex;
  gap: 5px;
  flex-shrink: 0;
}

.state-alert {
  margin-bottom: 16px;
}

.section-card {
  margin-bottom: 18px;
  border-color: #e5e7eb;
  border-radius: 12px;
}

.section-card :deep(.el-card__header) {
  padding: 18px 20px;
  border-bottom-color: #edf0f3;
}

.section-card :deep(.el-card__body) {
  padding: 20px;
}

.card-heading h2,
.download-copy h2 {
  font-size: 16px;
  font-weight: 700;
}

.form-grid {
  display: grid;
  gap: 0 18px;
}

.form-grid--four {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.form-grid--two {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.span-two {
  grid-column: span 2;
}

:deep(.el-form-item__label) {
  color: #374151;
  font-size: 13px;
  font-weight: 600;
}

:deep(.el-select),
:deep(.el-input-number),
:deep(.el-date-editor) {
  width: 100%;
}

.field-suffix {
  position: absolute;
  right: 12px;
  bottom: 1px;
  z-index: 2;
  color: #9ca3af;
  font-size: 12px;
  line-height: 30px;
  pointer-events: none;
}

.field-suffix + * {
  display: none;
}

.info-columns,
.upload-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 20px;
}

.info-panel {
  padding: 18px;
  border: 1px solid #edf0f3;
  border-radius: 10px;
  background: #fbfcfd;
}

.info-panel h3 {
  margin: 0 0 16px;
  color: #111827;
  font-size: 14px;
}

.stamp-row {
  display: flex;
  align-items: center;
  gap: 18px;
  margin-top: 18px;
}

.stamp-preview {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 110px;
  height: 86px;
  overflow: hidden;
  border: 1px dashed #cbd5e1;
  border-radius: 10px;
  background: #fff;
  color: #94a3b8;
  font-size: 12px;
}

.stamp-preview.has-image {
  border-style: solid;
}

.stamp-preview img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.run-fields {
  margin-bottom: 18px;
}

.upload-panel {
  padding: 16px;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  background: #fafafa;
}

.upload-title {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}

.upload-title div {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.upload-title strong {
  color: #111827;
  font-size: 14px;
}

.upload-title span:not(.platform-mark) {
  color: #9ca3af;
  font-size: 12px;
}

.platform-mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  border-radius: 8px;
  color: #fff;
  font-weight: 800;
}

.platform-mark--airbnb {
  background: #ff385c;
}

.platform-mark--booking {
  background: #003b95;
}

.upload-panel :deep(.el-upload),
.upload-panel :deep(.el-upload-dragger) {
  width: 100%;
}

.upload-panel :deep(.el-upload-dragger) {
  padding: 22px 12px;
  border-radius: 8px;
}

.upload-panel :deep(.el-icon--upload) {
  margin-bottom: 6px;
  font-size: 28px;
}

.automatic-fee,
.deduction-row,
.preview-action,
.summary-heading {
  display: flex;
  align-items: center;
}

.automatic-fee {
  justify-content: space-between;
  padding: 14px 16px;
  border-radius: 9px;
  background: #eff6ff;
}

.automatic-fee div,
.preview-action > div {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.automatic-fee strong,
.preview-action strong {
  color: #1e3a5f;
  font-size: 13px;
}

.automatic-fee span,
.preview-action span {
  color: #64748b;
  font-size: 12px;
}

.automatic-fee b {
  color: #1d4ed8;
  font-size: 17px;
}

.deduction-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 14px;
}

.deduction-row {
  gap: 10px;
}

.deduction-row .el-input {
  flex: 1;
}

.deduction-row .el-input-number {
  width: 180px;
}

.deduction-row > span {
  color: #9ca3af;
  font-size: 12px;
}

.preview-action {
  margin-top: 20px;
  padding-top: 18px;
  border-top: 1px solid #edf0f3;
}

.blocking-list {
  margin: 8px 0 0;
  padding-left: 18px;
}

.status-strip {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 8px;
  margin-bottom: 16px;
}

.status-stat {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
}

.status-stat span {
  overflow: hidden;
  color: #6b7280;
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.status-stat strong {
  color: #111827;
  font-size: 17px;
}

.preview-card :deep(.excluded-row) {
  color: #6b7280;
  background: #fafafa;
}

.warning-copy {
  color: #b45309;
  font-size: 12px;
}

.summary-section {
  margin-bottom: 18px;
  overflow: hidden;
  border: 1px solid #dbe4f0;
  border-radius: 12px;
  background: #f8fafc;
}

.summary-heading {
  gap: 38px;
  padding: 14px 20px;
  border-bottom: 1px solid #dbe4f0;
}

.summary-heading > div {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.summary-heading span,
.money-grid span {
  color: #64748b;
  font-size: 12px;
}

.summary-heading strong {
  color: #0f172a;
  font-size: 15px;
}

.money-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 1px;
  background: #e2e8f0;
}

.money-grid article {
  display: flex;
  flex-direction: column;
  gap: 7px;
  min-height: 84px;
  padding: 15px 18px;
  background: #fff;
}

.money-grid strong {
  color: #111827;
  font-size: 19px;
  font-variant-numeric: tabular-nums;
}

.money-grid .money-card--accent {
  background: #172554;
}

.money-grid .money-card--accent span,
.money-grid .money-card--accent strong {
  color: #fff;
}

.money-grid .money-card--invoice {
  background: #eff6ff;
}

.money-grid .money-card--invoice strong {
  color: #1d4ed8;
}

.download-copy {
  display: flex;
  align-items: center;
  gap: 14px;
}

.download-copy > .el-icon {
  width: 42px;
  height: 42px;
  border-radius: 10px;
  background: #eff6ff;
  color: #2563eb;
  font-size: 22px;
}

.download-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.download-actions .el-button + .el-button {
  margin-left: 0;
}

@media (max-width: 1100px) {
  .form-grid--four,
  .money-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .status-strip {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .managed-operation-page {
    padding: 4px 12px 28px;
  }

  .page-header,
  .card-heading,
  .preview-action,
  .download-card :deep(.el-card__body) {
    align-items: flex-start;
    flex-direction: column;
  }

  .info-columns,
  .upload-grid,
  .form-grid--four,
  .form-grid--two,
  .money-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .span-two {
    grid-column: auto;
  }

  .status-strip {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .deduction-row {
    align-items: stretch;
    flex-wrap: wrap;
  }

  .deduction-row .el-input,
  .deduction-row .el-input-number {
    width: 100%;
  }

  .summary-heading {
    align-items: flex-start;
    flex-direction: column;
    gap: 8px;
  }

  .download-actions {
    justify-content: flex-start;
  }
}
</style>
