<template>
  <div class="page">
    <div class="header">
      <div class="header-top">
        <div class="title">{{ t('guestRegistration') }}</div>
        <el-dropdown @command="changeLanguage" trigger="click">
          <el-button size="small">
            <el-icon><Setting /></el-icon>
            <span class="lang-btn-text">{{ t('changeLanguage') }}</span>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="en" :disabled="selectedLang === 'en'">English</el-dropdown-item>
              <el-dropdown-item command="ja" :disabled="selectedLang === 'ja'">日本語</el-dropdown-item>
              <el-dropdown-item command="zh" :disabled="selectedLang === 'zh'">中文</el-dropdown-item>
              <el-dropdown-item command="ko" :disabled="selectedLang === 'ko'">한국어</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
      <div v-if="loading" class="sub">{{ t('loading') }}</div>
      <div v-else class="sub">
        <div>{{ t('bookingNumber') }}: {{ data?.bookingKey }}</div>
        <div>{{ t('stay') }}: {{ data?.checkInDate }} ~ {{ data?.checkOutDate }}</div>
        <div v-if="data?.lastSavedAt" class="last-saved">{{ t('lastSaved') }}: {{ formatLastSaved(data.lastSavedAt) }}</div>
        <div>{{ t('status') }}: {{ statusLabel(data?.status || '') }}</div>
      </div>
    </div>

    <el-steps :active="stepsActive" finish-status="success" align-center class="steps">
      <el-step
        v-for="(s, idx) in stepItems"
        :key="idx"
        :title="s.title"
        :description="s.description"
      />
    </el-steps>

    <div v-if="error" class="error">{{ error }}</div>

    <div v-if="data" class="content">
      <!-- Guest steps -->

      <div v-show="isGuestStep" class="card">
        <div class="card-title">
          {{ t('guest') }} {{ activeGuestIndex + 1 }}
        </div>

        <div class="grid small-grid">
          <div class="kv"><div class="k">{{ t('checkIn') }}</div><div class="v">{{ data.checkInDate }}</div></div>
          <div class="kv"><div class="k">{{ t('checkOut') }}</div><div class="v">{{ data.checkOutDate }}</div></div>
        </div>

        <el-form label-position="top" class="form">
          <template v-if="activeGuest">
            <div class="mlabel">{{ t('residence') }} <span class="req">*</span></div>
            <el-radio-group v-model="activeGuest.residenceType" size="small">
              <el-radio-button label="JAPAN">{{ t('japan') }}</el-radio-button>
              <el-radio-button label="OTHER">{{ t('otherThanJapan') }}</el-radio-button>
            </el-radio-group>

            <div class="row">
              <el-form-item>
                <template #label>
                  <div class="mlabel">{{ t('firstName') }} <span class="req">*</span></div>
                </template>
                <el-input v-model="activeGuest.firstName" :placeholder="t('firstName')" />
              </el-form-item>
              <el-form-item>
                <template #label>
                  <div class="mlabel">{{ t('lastName') }} <span class="req">*</span></div>
                </template>
                <el-input v-model="activeGuest.lastName" :placeholder="t('lastName')" />
              </el-form-item>
            </div>

            <!-- Japan fields -->
            <template v-if="activeGuest.residenceType === 'JAPAN'">
              <div class="row">
                <el-form-item>
                  <template #label>
                    <div class="mlabel">{{ t('birthday') }} <span class="req">*</span></div>
                  </template>
                  <el-date-picker
                    v-model="activeGuest.birthday"
                    type="date"
                    value-format="YYYY-MM-DD"
                    placeholder="YYYY-MM-DD"
                    style="width: 100%"
                  />
                </el-form-item>
                <el-form-item>
                  <template #label>
                    <div class="mlabel">{{ t('phone') }} <span class="req">*</span></div>
                  </template>
                  <el-input v-model="activeGuest.phone" :placeholder="t('phone')" />
                </el-form-item>
              </div>

              <el-form-item>
                <template #label>
                  <div class="mlabel">{{ t('address') }} <span class="req">*</span></div>
                </template>
                <el-input v-model="activeGuest.address" type="textarea" :rows="3" :placeholder="t('address')" />
              </el-form-item>
            </template>

            <!-- Overseas fields -->
            <template v-else>
              <!-- Birthday + Phone -->
              <div class="row">
                <el-form-item>
                  <template #label>
                    <div class="mlabel">{{ t('birthday') }} <span class="req">*</span></div>
                  </template>
                  <el-date-picker
                    v-model="activeGuest.birthday"
                    type="date"
                    value-format="YYYY-MM-DD"
                    placeholder="YYYY-MM-DD"
                    style="width: 100%"
                  />
                </el-form-item>
                <el-form-item>
                  <template #label>
                    <div class="mlabel">{{ t('phone') }} <span class="req">*</span></div>
                  </template>
                  <el-input v-model="activeGuest.phone" :placeholder="t('phone')" />
                </el-form-item>
              </div>

              <!-- Address1 (full width) -->
              <el-form-item>
                <template #label>
                  <div class="mlabel">{{ t('address1') }} <span class="req">*</span></div>
                </template>
                <el-input v-model="activeGuest.address1" :placeholder="t('address1')" />
              </el-form-item>

              <!-- Address2 (full width) -->
              <el-form-item>
                <template #label>
                  <div class="mlabel">{{ t('address2') }}</div>
                </template>
                <el-input v-model="activeGuest.address2" :placeholder="t('address2Optional')" />
              </el-form-item>

              <!-- City + State -->
              <div class="row">
                <el-form-item>
                  <template #label>
                    <div class="mlabel">{{ t('city') }} <span class="req">*</span></div>
                  </template>
                  <el-input v-model="activeGuest.city" :placeholder="t('city')" />
                </el-form-item>
                <el-form-item>
                  <template #label>
                    <div class="mlabel">{{ t('state') }}</div>
                  </template>
                  <el-input v-model="activeGuest.state" :placeholder="t('stateOptional')" />
                </el-form-item>
              </div>

              <!-- Country (full width) -->
              <el-form-item>
                <template #label>
                  <div class="mlabel">{{ t('country') }} <span class="req">*</span></div>
                </template>
                <el-input v-model="activeGuest.country" :placeholder="t('country')" />
              </el-form-item>

              <!-- Previous location (full width) -->
              <el-form-item>
                <template #label>
                  <div class="mlabel">{{ t('priorStay') }}</div>
                </template>
                <el-input v-model="activeGuest.priorStay" :placeholder="t('optional')" />
              </el-form-item>

              <!-- Next destination (full width) -->
              <el-form-item>
                <template #label>
                  <div class="mlabel">{{ t('nextDestination') }}</div>
                </template>
                <el-input v-model="activeGuest.nextDestination" :placeholder="t('optional')" />
              </el-form-item>

              <!-- Passport number (full width) -->
              <!-- Passport number + Nationality -->
              <div class="row">
                <el-form-item>
                  <template #label>
                    <div class="mlabel">{{ t('passportNumber') }} <span class="req">*</span></div>
                  </template>
                  <el-input v-model="activeGuest.passportNumber" :placeholder="t('passportNumber')" />
                </el-form-item>
                <el-form-item>
                  <template #label>
                    <div class="mlabel">{{ t('nationality') }} <span class="req">*</span></div>
                  </template>
                  <el-input v-model="activeGuest.nationality" :placeholder="t('nationality')" />
                </el-form-item>
              </div>

              <!-- Passport photo (full width) -->
              <div class="upload">
                <div class="mlabel">{{ t('passportPhoto') }} <span class="req">*</span></div>
                <el-upload
                  :show-file-list="false"
                  :auto-upload="false"
                  accept="image/*"
                  :on-change="onPassportUploadChange.bind(null, activeGuest)"
                >
                  <el-button>{{ t('chooseImage') }}</el-button>
                </el-upload>
                <div class="upload-hint">
                  <span v-if="activeGuest.passportUploaded" class="ok">
                    {{ t('uploaded') }}{{ activeGuestPassportAttachment?.originalName ? `: ${activeGuestPassportAttachment.originalName}` : '' }}
                  </span>
                  <span v-else class="warn">{{ t('notUploadedYet') }}</span>
                </div>
              </div>

              <!-- Passport Image Preview (full width below) -->
              <div v-if="activeGuestPassportAttachment" class="passport-preview">
                <img 
                  :src="buildPassportAttachmentUrl(activeGuestPassportAttachment.id, true)" 
                  alt="Passport" 
                  class="passport-preview-image"
                />
              </div>
            </template>
          </template>
        </el-form>

        <div class="actions">
          <el-button v-if="canGoBack" @click="goPrev">{{ t('back') }}</el-button>
          <el-button type="primary" @click="goNext">{{ t('next') }}</el-button>
        </div>
      </div>

      <!-- Send -->
      <div v-show="isSendStep" class="card">
        <div class="card-title-row">
          <div class="card-title">{{ t('confirmSend') }}</div>
          <el-button @click="previewForm">{{ t('preview') }}</el-button>
        </div>
        <div class="hint">
          {{ t('reviewNotice') }}
        </div>
        <div v-if="data?.status === 'SUBMITTED'" class="hint" style="margin-top: 8px; color: #67c23a">
          {{ t('submittedAwaitingReview') }}
        </div>
        <div v-if="data?.status === 'APPROVED'" class="success-notice">
          <div class="success-title">{{ t('approvedTitle') }}</div>
          <div v-if="checkInGuideLink" class="checkin-guide">
            <div class="guide-label">{{ t('checkInGuideLabel') }}</div>
            <el-button type="primary" link @click="openCheckInGuide">{{ t('checkInGuide') }}</el-button>
          </div>
        </div>
        <div class="actions">
          <el-button v-if="canGoBack" @click="goPrev">{{ t('back') }}</el-button>
          <el-button type="success" :loading="submitting" :disabled="data?.status === 'SUBMITTED' || data?.status === 'APPROVED'" @click="submit">
            {{ t('send') }}
          </el-button>
        </div>
      </div>
    </div>
  </div>

  <el-dialog v-model="passportPreviewVisible" :title="passportPreviewTitle" width="760px" append-to-body destroy-on-close>
    <div class="passport-dialog-body">
      <img v-if="passportPreviewUrl" class="passport-preview-img" :src="passportPreviewUrl" alt="passport" />
    </div>
  </el-dialog>

  <el-dialog v-model="previewDialogVisible" :title="t('preview')" width="90%" class="preview-dialog" append-to-body destroy-on-close>
    <div class="preview-content">
      <div v-for="guest in previewData" :key="guest.guestIndex" class="preview-guest">
        <div class="preview-guest-title">{{ t('guest') }} {{ guest.guestIndex }}</div>
        <div class="preview-fields">
          <div v-for="(field, idx) in guest.fields" :key="idx" class="preview-field">
            <div class="preview-label">{{ field.label }}</div>
            <div class="preview-value">{{ field.value }}</div>
          </div>
        </div>
        <div v-if="guest.passportUrl" class="preview-passport">
          <div class="preview-passport-title">{{ t('passportPhoto') }}</div>
          <img :src="guest.passportUrl" alt="Passport" class="preview-passport-img" />
        </div>
      </div>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Setting } from '@element-plus/icons-vue'
import type { UploadFile } from 'element-plus'
import publicRequest from '@/utils/publicRequest'

type ApiResponse<T> = {
  success: boolean
  message: string
  data: T
}

type ResidenceType = 'JAPAN' | 'OTHER'

type GuestModel = {
  id?: number
  _key: string
  sortOrder?: number

  lastName?: string
  firstName?: string
  residenceType?: ResidenceType

  // common required
  phone?: string
  birthday?: string

  // japan
  address?: string

  // overseas
  nationality?: string
  country?: string
  address1?: string
  address2?: string
  city?: string
  state?: string
  passportNumber?: string
  passportUploaded?: boolean

  // optional travel fields
  priorStay?: string
  nextDestination?: string
}

type AttachmentDTO = {
  id: number
  guestId?: number
  type: string
  originalName?: string
}

type PublicRegistrationResponse = {
  formId: number
  orderNumber: string
  bookingKey: string
  status: string
  checkInDate: string
  checkOutDate: string
  guestName: string
  adults: number
  children: number
  maxGuests: number
  guestCount: number
  lastSavedAt?: string
  checkInGuideLink?: string
  guests: Array<{
    id: number
    sortOrder: number
    lastName?: string
    firstName?: string
    residenceType?: ResidenceType
    phone?: string
    birthday?: string
    address?: string
    nationality?: string
    country?: string
    address1?: string
    address2?: string
    city?: string
    state?: string
    passportNumber?: string
    priorStay?: string
    nextDestination?: string
  }>
  attachments?: AttachmentDTO[]
}

const route = useRoute()

// Language support
// Initialize from localStorage to avoid flicker, will be overridden by query param in onMounted if present
const selectedLang = ref<string>(localStorage.getItem('registrationLang') || '')
const checkInGuideLink = ref<string>('')

type LangCode = 'en' | 'ja' | 'zh' | 'ko'

const translations: Record<LangCode, Record<string, string>> = {
  en: {
    guestRegistration: 'Guest Registration',
    loading: 'Loading...',
    bookingNumber: 'Booking Number',
    stay: 'Stay',
    status: 'Status',
    lastSaved: 'Last saved',
    guest: 'Guest',
    checkIn: 'Check-in',
    checkOut: 'Check-out',
    residence: 'Residence',
    japan: 'Japan',
    otherThanJapan: 'Other than Japan',
    firstName: 'First Name',
    lastName: 'Last Name',
    birthday: 'Date of Birth',
    phone: 'Phone number',
    address: 'Home Address',
    passportNumber: 'Passport number',
    nationality: 'Nationality',
    passportPhoto: 'Passport photo',
    chooseImage: 'Choose image',
    uploaded: 'Uploaded',
    notUploadedYet: 'Not uploaded yet',
    clickToView: 'Click to view full size',
    address1: 'Address1',
    address2: 'Address2',
    address2Optional: 'Address2 (Optional)',
    city: 'City',
    state: 'State',
    stateOptional: 'State (Optional)',
    country: 'Country',
    priorStay: 'Previous location',
    nextDestination: 'Next destination',
    optional: 'Optional',
    back: 'Back',
    next: 'Next',
    confirmSend: 'Confirm & Send',
    preview: 'Preview',
    send: 'Send',
    reviewNotice: 'After review, we will automatically send you the check-in guide.',
    submittedAwaitingReview: 'Submitted, awaiting review.',
    approvedTitle: 'Approved',
    checkInGuideLabel: 'Here is the check-in guide:',
    checkInGuide: 'View Check-in Guide',
    draft: 'Draft',
    submitted: 'Submitted',
    approved: 'Approved',
    rejected: 'Rejected',
    changeLanguage: 'Language'
  },
  ja: {
    guestRegistration: '宿泊者名簿',
    loading: '読み込み中...',
    bookingNumber: '予約番号',
    stay: '宿泊期間',
    status: '状態',
    lastSaved: '最終保存',
    guest: '宿泊者',
    checkIn: 'チェックイン',
    checkOut: 'チェックアウト',
    residence: '居住地',
    japan: '日本',
    otherThanJapan: '海外',
    firstName: '名',
    lastName: '姓',
    birthday: '生年月日',
    phone: '電話番号',
    address: '住所',
    passportNumber: '旅券番号',
    nationality: '国籍',
    passportPhoto: '旅券写真',
    chooseImage: '画像を選択',
    uploaded: 'アップロード済み',
    notUploadedYet: '未アップロード',
    clickToView: 'クリックして拡大表示',
    address1: '住所1',
    address2: '住所2',
    address2Optional: '住所2（任意）',
    city: '市区町村',
    state: '都道府県',
    stateOptional: '都道府県（任意）',
    country: '国',
    priorStay: '前泊地',
    nextDestination: '行先',
    optional: '任意',
    back: '戻る',
    next: '次へ',
    confirmSend: '確認して送信',
    preview: 'プレビュー',
    send: '送信',
    reviewNotice: '審査後、自動的に入住指南をお送りします。',
    submittedAwaitingReview: '提出済み、審査をお待ちください。',
    approvedTitle: '承認済み',
    checkInGuideLabel: '以下は入住指南です：',
    checkInGuide: '入住指南を見る',
    draft: '未提出',
    submitted: '提出済み',
    approved: '承認済み',
    rejected: '要再提出',
    changeLanguage: '言語'
  },
  zh: {
    guestRegistration: '入住登记',
    loading: '加载中...',
    bookingNumber: '预订号',
    stay: '入住期间',
    status: '状态',
    lastSaved: '最后保存',
    guest: '入住人',
    checkIn: '入住',
    checkOut: '退房',
    residence: '居住地',
    japan: '日本',
    otherThanJapan: '海外',
    firstName: '名',
    lastName: '姓',
    birthday: '出生日期',
    phone: '电话号码',
    address: '住址',
    passportNumber: '护照号',
    nationality: '国籍',
    passportPhoto: '护照照片',
    chooseImage: '选择图片',
    uploaded: '已上传',
    notUploadedYet: '尚未上传',
    clickToView: '点击查看大图',
    address1: '地址1',
    address2: '地址2',
    address2Optional: '地址2（可选）',
    city: '城市',
    state: '州',
    stateOptional: '州（可选）',
    country: '国家',
    priorStay: '前泊地',
    nextDestination: '行先',
    optional: '可选',
    back: '返回',
    next: '下一步',
    confirmSend: '确认并发送',
    preview: '预览',
    send: '发送',
    reviewNotice: '我们审查後会自動給您发入住指南。',
    submittedAwaitingReview: '已提交，等待审查。',
    approvedTitle: '已通过审查',
    checkInGuideLabel: '下面是入住指南：',
    checkInGuide: '查看入住指南',
    draft: '未提交',
    submitted: '已提交',
    approved: '已通过',
    rejected: '需重填',
    changeLanguage: '语言'
  },
  ko: {
    guestRegistration: '투숙자 등록',
    loading: '로딩 중...',
    bookingNumber: '예약 번호',
    stay: '숙박 기간',
    status: '상태',
    lastSaved: '최종 저장',
    guest: '투숙자',
    checkIn: '체크인',
    checkOut: '체크아웃',
    residence: '거주지',
    japan: '일본',
    otherThanJapan: '해외',
    firstName: '이름',
    lastName: '성',
    birthday: '생년월일',
    phone: '전화번호',
    address: '주소',
    passportNumber: '여권번호',
    nationality: '국적',
    passportPhoto: '여권사진',
    chooseImage: '이미지 선택',
    uploaded: '업로드됨',
    notUploadedYet: '미업로드',
    clickToView: '클릭하여 크게 보기',
    address1: '주소1',
    address2: '주소2',
    address2Optional: '주소2（선택）',
    city: '도시',
    state: '주',
    stateOptional: '주（선택）',
    country: '국가',
    priorStay: '전 숙박지',
    nextDestination: '다음 목적지',
    optional: '선택',
    back: '뒤로',
    next: '다음',
    confirmSend: '확인 및 전송',
    preview: '미리보기',
    send: '전송',
    reviewNotice: '검토 후 자동으로 체크인 가이드를 보내드립니다.',
    submittedAwaitingReview: '제출됨, 검토 대기 중.',
    approvedTitle: '승인됨',
    checkInGuideLabel: '아래는 체크인 가이드입니다:',
    checkInGuide: '체크인 가이드 보기',
    draft: '미제출',
    submitted: '제출됨',
    approved: '승인됨',
    rejected: '재작성 필요',
    changeLanguage: '언어'
  }
}

const t = (key: string): string => {
  const lang = selectedLang.value as LangCode
  return translations[lang]?.[key] || key
}

const step = ref(0)
const loading = ref(true)
const saving = ref(false)
const submitting = ref(false)
const error = ref<string | null>(null)
const data = ref<PublicRegistrationResponse | null>(null)

const model = reactive<{ guests: GuestModel[] }>({ guests: [] })
const guestCount = ref<number>(1)

const passportPreviewVisible = ref(false)
const passportPreviewUrl = ref('')
const passportPreviewTitle = ref('Passport photo')

const previewDialogVisible = ref(false)
const previewData = ref<Array<{
  guestIndex: number
  fields: Array<{ label: string; value: string }>
  passportUrl?: string
}>>([])

const stepsActive = computed(() => {
  const status = String(data.value?.status || '')
  if (status === 'SUBMITTED' || status === 'APPROVED') {
    return stepItems.value.length
  }
  return step.value
})

function token(): string {
  return (route.query.t as string) || ''
}

function orderNumber(): string {
  return route.params.orderNumber as string
}

const guestCountOptions = computed<number[]>(() => {
  const max = data.value?.maxGuests ?? 1
  const safeMax = Math.max(1, max)
  return Array.from({ length: safeMax }, (_, i) => i + 1)
})

const stepItems = computed(() => {
  const guests = model.guests.length
  const items: Array<{ title: string; description: string }> = []
  for (let i = 0; i < guests; i++) {
    items.push({
      title: `${t('guest')}${i + 1}`,
      description: `${t('guest')} ${i + 1}`,
    })
  }
  items.push({ title: t('send'), description: t('confirmSend') })
  return items
})

const isGuestStep = computed(() => step.value >= 1 && step.value && step.value <= model.guests.length)
const isSendStep = computed(() => step.value === model.guests.length + 1 || step.value > model.guests.length)
const activeGuestIndex = computed(() => Math.max(0, step.value - 1))
const activeGuest = computed(() => model.guests[activeGuestIndex.value] || null)
const canGoBack = computed(() => {
  return data.value?.status !== 'SUBMITTED' && data.value?.status !== 'APPROVED'
})

function formatLastSaved(dateStr: string): string {
  if (!dateStr) return '-'
  try {
    const date = new Date(dateStr)
    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    const hours = String(date.getHours()).padStart(2, '0')
    const minutes = String(date.getMinutes()).padStart(2, '0')
    return `${year}-${month}-${day} ${hours}:${minutes}`
  } catch {
    return dateStr
  }
}

function statusLabel(status: string): string {
  const statusKey = status.toLowerCase()
  return t(statusKey)
}

function previewForm() {
  // Prepare structured data for preview
  previewData.value = model.guests.map((g, idx) => {
    const fields: Array<{ label: string; value: string }> = []
    fields.push({ label: t('firstName'), value: g.firstName || '-' })
    fields.push({ label: t('lastName'), value: g.lastName || '-' })
    fields.push({ label: t('residence'), value: g.residenceType === 'JAPAN' ? t('japan') : t('otherThanJapan') })
    fields.push({ label: t('birthday'), value: g.birthday || '-' })
    fields.push({ label: t('phone'), value: g.phone || '-' })
    
    if (g.residenceType === 'JAPAN') {
      fields.push({ label: t('address'), value: g.address || '-' })
    } else {
      fields.push({ label: t('passportNumber'), value: g.passportNumber || '-' })
      fields.push({ label: t('nationality'), value: g.nationality || '-' })
      fields.push({ label: t('address1'), value: g.address1 || '-' })
      fields.push({ label: t('address2'), value: g.address2 || '-' })
      fields.push({ label: t('city'), value: g.city || '-' })
      fields.push({ label: t('state'), value: g.state || '-' })
      fields.push({ label: t('country'), value: g.country || '-' })
      fields.push({ label: t('priorStay'), value: g.priorStay || '-' })
      fields.push({ label: t('nextDestination'), value: g.nextDestination || '-' })
    }
    
    const guestItem: any = {
      guestIndex: idx + 1,
      fields
    }
    
    // Add passport image URL if uploaded
    if (g.residenceType !== 'JAPAN' && g.passportUploaded) {
      const attachment = getPassportAttachment(g.id)
      if (attachment) {
        guestItem.passportUrl = buildPassportAttachmentUrl(attachment.id, true)
      }
    }
    
    return guestItem
  })
  
  previewDialogVisible.value = true
}

function changeLanguage(lang: string) {
  selectedLang.value = lang
  localStorage.setItem('registrationLang', lang)
}

const isGuestStep_old = computed(() => step.value >= 0 && step.value <= model.guests.length)

function isBlank(v?: string): boolean {
  return !v || String(v).trim().length === 0
}

function validateReservationStep(): string[] {
  const errs: string[] = []
  if (!guestCount.value || guestCount.value < 1) {
    errs.push('Please select number of guests / 宿泊人数を選択してください')
  }
  if (!model.guests.length) {
    errs.push('Guest list not ready, please wait / 宿泊者情報の準備中です')
  }
  return errs
}

function validateGuest(g: GuestModel): string[] {
  const errs: string[] = []
  const residence = g.residenceType || 'JAPAN'

  if (isBlank(g.firstName)) errs.push(t('firstName') + ' is required / は必須です')
  if (isBlank(g.lastName)) errs.push(t('lastName') + ' is required / は必須です')
  if (isBlank(g.phone)) errs.push(t('phone') + ' is required / は必須です')
  if (isBlank(g.birthday)) errs.push(t('birthday') + ' is required / は必須です')

  if (residence === 'JAPAN') {
    if (isBlank(g.address)) errs.push(t('address') + ' is required / は必須です')
  } else {
    if (!g.passportUploaded) errs.push(t('passportPhoto') + ' is required / は必須です')
    if (isBlank(g.passportNumber)) errs.push(t('passportNumber') + ' is required / は必須です')
    if (isBlank(g.nationality)) errs.push(t('nationality') + ' is required / は必須です')
    if (isBlank(g.address1)) errs.push(t('address1') + ' is required / は必須です')
    if (isBlank(g.city)) errs.push(t('city') + ' is required / は必須です')
    if (isBlank(g.country)) errs.push(t('country') + ' is required / は必須です')
  }

  return errs
}

function validateCurrentStep(): string[] {
  if (isGuestStep.value) {
    if (!activeGuest.value) return [t('guestNotFound') || 'Guest not found']
    return validateGuest(activeGuest.value)
  }
  return []
}

function showFirstError(errs: string[]) {
  if (!errs.length) return
  ElMessage.error(errs[0])
}

function getPassportAttachment(guestId?: number): AttachmentDTO | null {
  if (!guestId || !data.value?.attachments || !Array.isArray(data.value.attachments)) {
    return null
  }
  const list = data.value.attachments
  for (let i = list.length - 1; i >= 0; i--) {
    const a = list[i]
    if (a && a.type === 'PASSPORT' && a.guestId === guestId) {
      return a
    }
  }
  return null
}

const activeGuestPassportAttachment = computed(() => getPassportAttachment(activeGuest.value?.id))

function publicBaseUrl(): string {
  const base = String((publicRequest.defaults as any).baseURL || '')
  return base.endsWith('/') ? base.slice(0, -1) : base
}

function buildPassportAttachmentUrl(attachmentId: number, inline: boolean): string {
  const encodedOrder = encodeURIComponent(orderNumber())
  const encodedToken = encodeURIComponent(token())
  const base = publicBaseUrl()
  return `${base}/public/registration/${encodedOrder}/attachments/${attachmentId}?t=${encodedToken}&inline=${inline ? 'true' : 'false'}`
}

function openPassportPreview(att: AttachmentDTO) {
  passportPreviewTitle.value = att.originalName ? `Passport photo - ${att.originalName}` : 'Passport photo'
  passportPreviewUrl.value = buildPassportAttachmentUrl(att.id, true)
  passportPreviewVisible.value = true
}

function downloadPassport(att: AttachmentDTO) {
  const url = buildPassportAttachmentUrl(att.id, false)
  window.open(url, '_blank', 'noopener,noreferrer')
}

function hydrate(resp: PublicRegistrationResponse) {
  data.value = resp
  checkInGuideLink.value = resp.checkInGuideLink || ''
  const attachments = resp.attachments || []
  model.guests = (resp.guests || []).map((g) => ({
    id: g.id,
    _key: String(g.id),
    sortOrder: g.sortOrder,
    lastName: g.lastName,
    firstName: g.firstName,
    residenceType: g.residenceType || 'JAPAN',
    phone: g.phone,
    birthday: g.birthday,
    address: g.address,
    nationality: g.nationality,
    country: g.country,
    address1: g.address1,
    address2: g.address2,
    city: g.city,
    state: g.state,
    passportNumber: g.passportNumber,
    priorStay: g.priorStay,
    nextDestination: g.nextDestination,
    passportUploaded: attachments.some((a) => a.type === 'PASSPORT' && a.guestId === g.id),
  }))
  guestCount.value = resp.guestCount || model.guests.length || 1

  // Set initial step to 1 (first guest) instead of 0
  if (step.value === 0) {
    step.value = 1
  }

  if (resp.status === 'SUBMITTED' || resp.status === 'APPROVED') {
    step.value = model.guests.length + 1
  }
}

async function compressPassportImage(file: File): Promise<File> {
  if (!file.type?.startsWith('image/')) {
    return file
  }

  // Keep originals if already small enough
  const maxBytes = 900 * 1024
  if (file.size <= maxBytes) {
    return file
  }

  const dataUrl: string = await new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(String(reader.result || ''))
    reader.onerror = () => reject(new Error('Read image failed'))
    reader.readAsDataURL(file)
  })

  const img: HTMLImageElement = await new Promise((resolve, reject) => {
    const image = new Image()
    image.onload = () => resolve(image)
    image.onerror = () => reject(new Error('Load image failed'))
    image.src = dataUrl
  })

  const maxSide = 2000
  const scale = Math.min(1, maxSide / Math.max(img.width || 1, img.height || 1))
  const targetW = Math.max(1, Math.round((img.width || 1) * scale))
  const targetH = Math.max(1, Math.round((img.height || 1) * scale))

  const canvas = document.createElement('canvas')
  canvas.width = targetW
  canvas.height = targetH
  const ctx = canvas.getContext('2d')
  if (!ctx) {
    return file
  }
  ctx.drawImage(img, 0, 0, targetW, targetH)

  const blob: Blob = await new Promise((resolve, reject) => {
    canvas.toBlob(
      (b) => {
        if (b) resolve(b)
        else reject(new Error('Compress failed'))
      },
      'image/jpeg',
      0.85,
    )
  })

  const name = file.name?.replace(/\.(png|webp|jpeg|jpg)$/i, '') || 'passport'
  return new File([blob], `${name}.jpg`, { type: 'image/jpeg' })
}

async function uploadPassport(guest: GuestModel, file: File) {
  if (!guest.id) {
    throw new Error('Missing guestId')
  }
  const safeFile = await compressPassportImage(file)
  const form = new FormData()
  form.append('file', safeFile)

  const resp = (await publicRequest.post(`/public/registration/${orderNumber()}/attachments/passport`, form, {
    params: { t: token(), guestId: guest.id },
  })) as any
  if (!resp?.success) {
    throw new Error(resp?.message || 'Upload failed')
  }
  guest.passportUploaded = true

  if (data.value) {
    const current = Array.isArray((data.value as any).attachments) ? ([...(data.value as any).attachments] as AttachmentDTO[]) : ([] as AttachmentDTO[])
    const dto = resp.data as AttachmentDTO
    const next = current.filter((a) => !(a.type === 'PASSPORT' && a.guestId === guest.id))
    next.push(dto)
    ;(data.value as any).attachments = next
  }
}

function onPassportUploadChange(guest: GuestModel, file: UploadFile) {
  onPassportFileChange(guest, (file as any).raw as File | undefined)
}

async function onPassportFileChange(guest: GuestModel, raw?: File) {
  if (!raw) return
  try {
    await uploadPassport(guest, raw)
    ElMessage.success('Passport uploaded')
  } catch (e: any) {
    const status = e?.response?.status
    if (status === 413) {
      ElMessage.error('图片过大，已超出服务器限制。请更换更小的图片后重试。')
      return
    }
    ElMessage.error(e?.response?.data?.message || e?.message || 'Upload failed')
  }
}

async function load() {
  loading.value = true
  error.value = null
  try {
    const resp = (await publicRequest.get(`/public/registration/${orderNumber()}`, { params: { t: token() } })) as ApiResponse<PublicRegistrationResponse>
    if (!resp?.success) {
      throw new Error(resp?.message || 'Failed to load')
    }
    hydrate(resp.data as PublicRegistrationResponse)
  } catch (e: any) {
    error.value = e?.response?.data?.message || e?.message || 'Failed to load'
  } finally {
    loading.value = false
  }
}

async function saveDraft() {
  if (!data.value) return
  saving.value = true
  try {
    const payload = {
      guestCount: guestCount.value,
      guests: model.guests.map((g, idx) => ({
        id: g.id,
        sortOrder: idx + 1,
        lastName: g.lastName,
        firstName: g.firstName,
        residenceType: g.residenceType,
        phone: g.phone,
        birthday: g.birthday,
        address: g.address,
        nationality: g.nationality,
        country: g.country,
        address1: g.address1,
        address2: g.address2,
        city: g.city,
        state: g.state,
        passportNumber: g.passportNumber,
        priorStay: g.priorStay,
        nextDestination: g.nextDestination,
      })),
    }
    const resp = (await publicRequest.put(`/public/registration/${orderNumber()}`, payload, { params: { t: token() } })) as ApiResponse<PublicRegistrationResponse>
    if (!resp?.success) {
      throw new Error(resp?.message || 'Save failed')
    }
    hydrate(resp.data as PublicRegistrationResponse)
    ElMessage.success('Saved')
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e?.message || 'Save failed')
  } finally {
    saving.value = false
  }
}

async function submit() {
  submitting.value = true
  try {
    // Validate all guests before submit
    for (let i = 0; i < model.guests.length; i++) {
      const errs = validateGuest(model.guests[i])
      if (errs.length) {
        step.value = i + 1
        showFirstError(errs)
        return
      }
    }
    await saveDraft()
    const resp = (await publicRequest.post(`/public/registration/${orderNumber()}/submit`, null, { params: { t: token() } })) as ApiResponse<PublicRegistrationResponse>
    if (!resp?.success) {
      throw new Error(resp?.message || 'Submit failed')
    }
    hydrate(resp.data as PublicRegistrationResponse)
    ElMessage.success('Submitted')
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e?.message || 'Submit failed')
  } finally {
    submitting.value = false
  }
}

function openCheckInGuide() {
  const rawLink = (checkInGuideLink.value || '').trim()
  if (!rawLink) {
    return
  }
  let finalLink = rawLink
  if (!/^https?:\/\//i.test(finalLink)) {
    if (/^[a-zA-Z][a-zA-Z\d+\-.]*:/.test(finalLink)) {
      ElMessage.warning('Invalid check-in guide link')
      return
    }
    finalLink = `https://${finalLink}`
  }
  window.open(finalLink, '_blank', 'noopener,noreferrer')
}

async function onGuestCountChange(next: number) {
  const current = model.guests.length || 0
  if (next === current) return

  if (next < current) {
    try {
      await ElMessageBox.confirm(
        `将宿泊人数从 ${current} 改为 ${next}，将删除多余 Guest 的已填信息，是否继续？`,
        '确认修改',
        { confirmButtonText: '继续', cancelButtonText: '取消', type: 'warning' },
      )
    } catch {
      guestCount.value = current
      return
    }
  }

  await saveDraft()

  // Keep step within range after guest count change
  const maxStep = model.guests.length + 1
  if (step.value > maxStep) {
    step.value = maxStep
  }
}

function goNext() {
  const errs = validateCurrentStep()
  if (errs.length) {
    showFirstError(errs)
    return
  }
  const maxStep = model.guests.length + 1
  step.value = Math.min(step.value + 1, maxStep)
}

function goPrev() {
  step.value = Math.max(step.value - 1, 1)
}

onMounted(() => {
  // Get language from query or localStorage
  const langFromQuery = route.query.lang as string
  const langFromStorage = localStorage.getItem('registrationLang')
  
  if (langFromQuery) {
    // Query parameter takes priority
    selectedLang.value = langFromQuery
    localStorage.setItem('registrationLang', langFromQuery)
  } else if (langFromStorage) {
    // Use saved language preference
    selectedLang.value = langFromStorage
  } else {
    // First time visit: auto-detect browser language
    const browserLang = navigator.language.toLowerCase()
    if (browserLang.startsWith('ja')) {
      selectedLang.value = 'ja'
    } else if (browserLang.startsWith('zh')) {
      selectedLang.value = 'zh'
    } else if (browserLang.startsWith('ko')) {
      selectedLang.value = 'ko'
    } else {
      selectedLang.value = 'en'
    }
    localStorage.setItem('registrationLang', selectedLang.value)
  }
  
  load()
})
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: #f6f7fb;
  padding: 16px;
}
.header {
  margin-bottom: 12px;
}
.header-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}
.lang-btn-text {
  margin-left: 4px;
  font-size: 13px;
}

/* 确保按钮内容在所有屏幕正确显示 */
:deep(.el-button) {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
}

.title {
  font-weight: 700;
  font-size: 16px;
}
.sub {
  margin-top: 6px;
  color: #555;
  font-size: 12px;
  line-height: 18px;
}
.steps {
  margin: 12px 0 16px;
}
.error {
  background: #fff;
  border: 1px solid #f2b8b5;
  color: #b42318;
  padding: 10px 12px;
  border-radius: 10px;
}
.content {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.card {
  background: #fff;
  border-radius: 12px;
  padding: 14px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
}
.card-title {
  font-weight: 700;
  margin-bottom: 10px;
}
.card-title-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}
.success-notice {
  background: #f0f9ff;
  border: 1px solid #7dd3fc;
  border-radius: 8px;
  padding: 12px;
  margin-top: 12px;
}
.success-title {
  font-weight: 700;
  color: #067647;
  margin-bottom: 8px;
}
.checkin-guide {
  margin-top: 8px;
}
.guide-link {
  display: inline-block;
  padding: 8px 16px;
  background: #3b82f6;
  color: white;
  text-decoration: none;
  border-radius: 6px;
  font-size: 14px;
  transition: background 0.2s;
}
.guide-link:hover {
  background: #2563eb;
}
.last-saved {
  font-size: 11px;
  color: #888;
  margin-top: 2px;
}
.grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}
.small-grid {
  margin-bottom: 8px;
}
.kv .k {
  font-size: 11px;
  color: #666;
}
.kv .v {
  font-size: 13px;
  color: #111;
}
.actions {
  margin-top: 12px;
  display: flex;
  gap: 10px;
  justify-content: flex-end;
}
.form {
  margin-top: 6px;
}
.row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}
.mlabel {
  font-weight: 600;
  font-size: 12px;
  margin: 8px 0 6px;
}
.req {
  color: var(--el-color-danger);
}
.upload {
  margin-top: 8px;
}
.upload-hint {
  margin-top: 6px;
  font-size: 12px;
}
.passport-preview {
  margin-top: 8px;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 8px;
}
.passport-preview-image {
  max-width: 100%;
  max-height: 300px;
  border-radius: 8px;
  border: 2px solid #e5e7eb;
  object-fit: contain;
}
.passport-thumb {
  width: 140px;
  height: 100px;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  object-fit: cover;
  cursor: pointer;
}
.passport-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.passport-tip {
  font-size: 12px;
  color: #909399;
}
.passport-dialog-body {
  display: flex;
  justify-content: center;
  align-items: center;
}
.passport-preview-img {
  max-width: 100%;
  max-height: 70vh;
  object-fit: contain;
}
.ok {
  color: #067647;
}
.warn {
  color: #b42318;
}
.hint {
  color: #555;
  font-size: 12px;
}

/* Preview Dialog Styles */
:deep(.preview-dialog) {
  max-width: 800px;
}

:deep(.preview-dialog .el-dialog__body) {
  padding: 20px;
  max-height: 70vh;
  overflow-y: auto;
}

.preview-content {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.preview-guest {
  background: #f6f7fb;
  border-radius: 8px;
  padding: 16px;
}

.preview-guest-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 16px;
  padding-bottom: 8px;
  border-bottom: 2px solid #409eff;
}

.preview-fields {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.preview-field {
  display: flex;
  flex-direction: row;
  gap: 12px;
  padding: 8px 0;
  border-bottom: 1px solid #e4e7ed;
}

.preview-label {
  font-weight: 600;
  color: #606266;
  min-width: 140px;
  flex-shrink: 0;
}

.preview-value {
  color: #303133;
  flex: 1;
  word-break: break-word;
}

.preview-passport {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #dcdfe6;
}

.preview-passport-title {
  font-weight: 600;
  color: #606266;
  margin-bottom: 12px;
  font-size: 15px;
}

.preview-passport-img {
  max-width: 100%;
  height: auto;
  border-radius: 4px;
  border: 1px solid #dcdfe6;
  display: block;
}

/* Mobile Responsive */
@media (max-width: 640px) {
  .header-top {
    flex-wrap: wrap;
    gap: 8px;
  }

  .header-top .el-button {
    white-space: nowrap;
  }

  :deep(.preview-dialog) {
    width: 95% !important;
  }

  :deep(.preview-dialog .el-dialog__body) {
    padding: 12px;
  }

  .preview-guest {
    padding: 12px;
  }

  .preview-guest-title {
    font-size: 16px;
    margin-bottom: 12px;
  }

  .preview-field {
    flex-direction: column;
    gap: 4px;
    padding: 8px 0;
  }

  .preview-label {
    min-width: auto;
    font-size: 13px;
  }

  .preview-value {
    font-size: 14px;
    padding-left: 0;
  }

  .preview-passport-img {
    max-width: 100%;
  }
}
</style>
