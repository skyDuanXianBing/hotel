<template>
  <div class="page">
    <div class="header">
      <div class="title">Guest Registration / 宿泊者名簿 / 入住登记 / 투숙자 등록</div>
      <div v-if="loading" class="sub">Loading...</div>
      <div v-else class="sub">
        <div>Order: {{ data?.orderNumber }}</div>
        <div>Stay: {{ data?.checkInDate }} ~ {{ data?.checkOutDate }}</div>
        <div>Status: {{ data?.status }}</div>
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
      <!-- Step 0: Reservation -->
      <div v-show="step === 0" class="card">
        <div class="card-title">Reservation Information / 予約情報 / 预订信息 / 예약 정보</div>
        <div class="grid">
          <div class="kv"><div class="k">Guest</div><div class="v">{{ data.guestName }}</div></div>
          <div class="kv"><div class="k">Stay</div><div class="v">{{ data.checkInDate }} ~ {{ data.checkOutDate }}</div></div>
          <div class="kv"><div class="k">Max guests</div><div class="v">{{ data.maxGuests }}</div></div>
          <div class="kv"><div class="k">Last saved</div><div class="v">{{ data.lastSavedAt || '-' }}</div></div>
        </div>

        <div class="form">
          <el-form label-position="top">
            <el-form-item>
              <template #label>
                <div class="mlabel">Number of guests / 宿泊人数 / 入住人数 / 숙박 인원 <span class="req">*</span></div>
              </template>
              <el-select v-model="guestCount" style="width: 220px" @change="onGuestCountChange">
                <el-option v-for="n in guestCountOptions" :key="n" :label="String(n)" :value="n" />
              </el-select>
            </el-form-item>
          </el-form>
        </div>

        <div class="actions">
          <el-button type="primary" :disabled="model.guests.length === 0" @click="goNext">Next</el-button>
        </div>
      </div>

      <!-- Guest steps -->
      <div v-show="isGuestStep" class="card">
        <div class="card-title">
          Guest {{ activeGuestIndex + 1 }} / 宿泊者{{ activeGuestIndex + 1 }} / 入住人{{ activeGuestIndex + 1 }} /
          투숙자{{ activeGuestIndex + 1 }}
        </div>

        <div class="grid small-grid">
          <div class="kv"><div class="k">Check-in</div><div class="v">{{ data.checkInDate }}</div></div>
          <div class="kv"><div class="k">Check-out</div><div class="v">{{ data.checkOutDate }}</div></div>
        </div>

        <el-form label-position="top" class="form">
          <template v-if="activeGuest">
            <div class="mlabel">Residence / 居住地 / 居住地 / 거주지 <span class="req">*</span></div>
            <el-radio-group v-model="activeGuest.residenceType" size="small">
              <el-radio-button label="JAPAN">Japan / 日本</el-radio-button>
              <el-radio-button label="OTHER">Other than Japan / 海外</el-radio-button>
            </el-radio-group>

            <div class="row">
              <el-form-item>
                <template #label>
                  <div class="mlabel">First Name / 名 / 名 / 이름 <span class="req">*</span></div>
                </template>
                <el-input v-model="activeGuest.firstName" placeholder="First Name" />
              </el-form-item>
              <el-form-item>
                <template #label>
                  <div class="mlabel">Last Name / 姓 / 姓 / 성 <span class="req">*</span></div>
                </template>
                <el-input v-model="activeGuest.lastName" placeholder="Last Name" />
              </el-form-item>
            </div>

            <!-- Japan fields -->
            <template v-if="activeGuest.residenceType === 'JAPAN'">
              <el-form-item>
                <template #label>
                  <div class="mlabel">Home Address / 住所 / 住址 / 주소 <span class="req">*</span></div>
                </template>
                <el-input v-model="activeGuest.address" type="textarea" :rows="3" placeholder="Address line" />
              </el-form-item>

              <div class="row">
                <el-form-item>
                  <template #label>
                    <div class="mlabel">Phone number / 電話番号 / 电话号码 / 전화번호 <span class="req">*</span></div>
                  </template>
                  <el-input v-model="activeGuest.phone" placeholder="Phone number" />
                </el-form-item>
                <el-form-item>
                  <template #label>
                    <div class="mlabel">Date of Birth / 生年月日 / 出生日期 / 생년월일 <span class="req">*</span></div>
                  </template>
                  <el-date-picker
                    v-model="activeGuest.birthday"
                    type="date"
                    value-format="YYYY-MM-DD"
                    placeholder="YYYY-MM-DD"
                    style="width: 100%"
                  />
                </el-form-item>
              </div>
            </template>

            <!-- Overseas fields -->
            <template v-else>
              <div class="upload">
                <div class="mlabel">Passport photo / 旅券写真 / 护照照片 / 여권사진 <span class="req">*</span></div>
                <el-upload
                  :show-file-list="false"
                  :auto-upload="false"
                  accept="image/*"
                  :on-change="onPassportUploadChange.bind(null, activeGuest)"
                >
                  <el-button>Choose image</el-button>
                </el-upload>
                <div class="upload-hint">
                  <span v-if="activeGuest.passportUploaded" class="ok">
                    Uploaded{{ activeGuestPassportAttachment?.originalName ? `: ${activeGuestPassportAttachment.originalName}` : '' }}
                  </span>
                  <span v-else class="warn">Not uploaded yet</span>
                </div>

                <div v-if="activeGuestPassportAttachment" class="passport-preview">
                  <img
                    class="passport-thumb"
                    :src="buildPassportAttachmentUrl(activeGuestPassportAttachment.id, true)"
                    alt="passport"
                    @click="openPassportPreview(activeGuestPassportAttachment)"
                  />
                  <div class="passport-actions">
                    <el-button link type="primary" @click="openPassportPreview(activeGuestPassportAttachment)">预览</el-button>
                    <el-button link @click="downloadPassport(activeGuestPassportAttachment)">下载</el-button>
                    <span class="passport-tip">重新上传将覆盖原图片</span>
                  </div>
                </div>
              </div>

              <div class="row">
                <el-form-item>
                  <template #label>
                    <div class="mlabel">Passport number / 旅券番号 / 护照号 / 여권번호 <span class="req">*</span></div>
                  </template>
                  <el-input v-model="activeGuest.passportNumber" placeholder="Passport number" />
                </el-form-item>
                <el-form-item>
                  <template #label>
                    <div class="mlabel">Nationality / 国籍 / 国籍 / 국적 <span class="req">*</span></div>
                  </template>
                  <el-input v-model="activeGuest.nationality" placeholder="Nationality" />
                </el-form-item>
              </div>

              <div class="row">
                <el-form-item>
                  <template #label>
                    <div class="mlabel">Phone number / 電話番号 / 电话号码 / 전화번호 <span class="req">*</span></div>
                  </template>
                  <el-input v-model="activeGuest.phone" placeholder="Phone number" />
                </el-form-item>
                <el-form-item>
                  <template #label>
                    <div class="mlabel">Date of Birth / 生年月日 / 出生日期 / 생년월일 <span class="req">*</span></div>
                  </template>
                  <el-date-picker
                    v-model="activeGuest.birthday"
                    type="date"
                    value-format="YYYY-MM-DD"
                    placeholder="YYYY-MM-DD"
                    style="width: 100%"
                  />
                </el-form-item>
              </div>

              <div class="row">
                <el-form-item>
                  <template #label>
                    <div class="mlabel">Address1 / 住所1 / 地址1 / 주소1 <span class="req">*</span></div>
                  </template>
                  <el-input v-model="activeGuest.address1" placeholder="Address1" />
                </el-form-item>
                <el-form-item>
                  <template #label>
                    <div class="mlabel">Address2 / 住所2 / 地址2 / 주소2</div>
                  </template>
                  <el-input v-model="activeGuest.address2" placeholder="Address2 (Optional)" />
                </el-form-item>
              </div>

              <div class="row">
                <el-form-item>
                  <template #label>
                    <div class="mlabel">City / 市区町村 / 城市 / 도시 <span class="req">*</span></div>
                  </template>
                  <el-input v-model="activeGuest.city" placeholder="City" />
                </el-form-item>
                <el-form-item>
                  <template #label>
                    <div class="mlabel">State / 都道府県 / 州 / 주</div>
                  </template>
                  <el-input v-model="activeGuest.state" placeholder="State (Optional)" />
                </el-form-item>
              </div>

              <div class="row">
                <el-form-item>
                  <template #label>
                    <div class="mlabel">Country / 国 / 国家 / 국가 <span class="req">*</span></div>
                  </template>
                  <el-input v-model="activeGuest.country" placeholder="Country" />
                </el-form-item>
                <div />
              </div>

              <div class="row">
                <el-form-item>
                  <template #label>
                    <div class="mlabel">Previous location / 前泊地 / 前泊地 / 전 숙박지</div>
                  </template>
                  <el-input v-model="activeGuest.priorStay" placeholder="Optional" />
                </el-form-item>
                <el-form-item>
                  <template #label>
                    <div class="mlabel">Next destination / 行先 / 行先 / 다음 목적지</div>
                  </template>
                  <el-input v-model="activeGuest.nextDestination" placeholder="Optional" />
                </el-form-item>
              </div>
            </template>
          </template>
        </el-form>

        <div class="actions">
          <el-button @click="goPrev">Back</el-button>
          <el-button :loading="saving" @click="saveDraft">Save</el-button>
          <el-button type="primary" @click="goNext">Next</el-button>
        </div>
      </div>

      <!-- Send -->
      <div v-show="isSendStep" class="card">
        <div class="card-title">Confirm & Send / 確認 / 确认提交 / 제출</div>
        <div class="hint">
          Overseas guests (Residence=Other) must upload passport photo and fill passport number.
        </div>
        <div v-if="data?.status === 'SUBMITTED'" class="hint" style="margin-top: 8px; color: #67c23a">
          已提交，等待审查。
        </div>
        <div class="actions">
          <el-button @click="goPrev">Back</el-button>
          <el-button :loading="saving" @click="saveDraft">Save</el-button>
          <el-button type="success" :loading="submitting" :disabled="data?.status === 'SUBMITTED'" @click="submit">
            Send
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
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
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
  status: string
  checkInDate: string
  checkOutDate: string
  guestName: string
  adults: number
  children: number
  maxGuests: number
  guestCount: number
  lastSavedAt?: string
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
  const items: Array<{ title: string; description: string }> = [
    { title: 'Reservation', description: 'Reservation Information / 予約情報 / 预订信息 / 예약 정보' },
  ]
  for (let i = 0; i < guests; i++) {
    items.push({
      title: `Guest${i + 1}`,
      description: `Guest${i + 1} / 宿泊者${i + 1} / 入住人${i + 1} / 투숙자${i + 1}`,
    })
  }
  items.push({ title: 'Send', description: 'Send / 送信 / 发送 / 전송' })
  return items
})

const isGuestStep = computed(() => step.value >= 1 && step.value <= model.guests.length)
const isSendStep = computed(() => step.value === model.guests.length + 1)
const activeGuestIndex = computed(() => Math.max(0, step.value - 1))
const activeGuest = computed(() => model.guests[activeGuestIndex.value] || null)

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

  if (isBlank(g.firstName)) errs.push('First Name is required / 名は必須です')
  if (isBlank(g.lastName)) errs.push('Last Name is required / 姓は必須です')
  if (isBlank(g.phone)) errs.push('Phone number is required / 電話番号は必須です')
  if (isBlank(g.birthday)) errs.push('Date of Birth is required / 生年月日は必須です')

  if (residence === 'JAPAN') {
    if (isBlank(g.address)) errs.push('Home Address is required / 住所は必須です')
  } else {
    if (!g.passportUploaded) errs.push('Passport photo is required / パスポート写真のアップロードが必要です')
    if (isBlank(g.passportNumber)) errs.push('Passport number is required / 旅券番号は必須です')
    if (isBlank(g.nationality)) errs.push('Nationality is required / 国籍は必須です')
    if (isBlank(g.address1)) errs.push('Address1 is required / 住所1は必須です')
    if (isBlank(g.city)) errs.push('City is required / 市区町村は必須です')
    if (isBlank(g.country)) errs.push('Country is required / 国は必須です')
  }

  return errs
}

function validateCurrentStep(): string[] {
  if (step.value === 0) return validateReservationStep()
  if (isGuestStep.value) {
    if (!activeGuest.value) return ['Guest not found / 宿泊者が見つかりません']
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
  step.value = Math.max(step.value - 1, 0)
}

onMounted(load)
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
  grid-template-columns: 1fr;
  gap: 10px;
}
@media (min-width: 720px) {
  .row {
    grid-template-columns: 1fr 1fr;
  }
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
  align-items: flex-start;
  gap: 12px;
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
</style>
