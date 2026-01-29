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

    <el-steps :active="step" finish-status="success" align-center class="steps">
      <el-step title="Info" description="信息 / 情報 / 정보" />
      <el-step title="Guests" description="人员 / 宿泊者 / 투숙자" />
      <el-step title="Submit" description="提交 / 送信 / 제출" />
    </el-steps>

    <div v-if="error" class="error">{{ error }}</div>

    <div v-if="data" class="content">
      <div v-show="step === 0" class="card">
        <div class="card-title">Reservation Info / 予約情報 / 预订信息 / 예약 정보</div>
        <div class="grid">
          <div class="kv"><div class="k">Guest</div><div class="v">{{ data.guestName }}</div></div>
          <div class="kv"><div class="k">Adults</div><div class="v">{{ data.adults }}</div></div>
          <div class="kv"><div class="k">Children</div><div class="v">{{ data.children }}</div></div>
          <div class="kv"><div class="k">Last saved</div><div class="v">{{ data.lastSavedAt || '-' }}</div></div>
        </div>
        <div class="actions">
          <el-button type="primary" @click="step = 1">Next</el-button>
        </div>
      </div>

      <div v-show="step === 1" class="card">
        <div class="card-title">Guest Details / 宿泊者情報 / 入住人员信息 / 투숙자 정보</div>

        <el-form label-position="top" class="form">
          <div v-for="(g, idx) in model.guests" :key="g._key" class="guest-block">
            <div class="guest-title">#{{ idx + 1 }}</div>

            <div class="mlabel">居住地 / Residence / 居住地 / 거주지</div>
            <el-radio-group v-model="g.residenceType" size="small">
              <el-radio-button label="JAPAN">Japan</el-radio-button>
              <el-radio-button label="OTHER">Other</el-radio-button>
            </el-radio-group>

            <div class="row">
              <el-form-item>
                <template #label>
                  <div class="mlabel">姓 / Last name / 姓 / 성</div>
                </template>
                <el-input v-model="g.lastName" placeholder="Last name" />
              </el-form-item>
              <el-form-item>
                <template #label>
                  <div class="mlabel">名 / First name / 名 / 이름</div>
                </template>
                <el-input v-model="g.firstName" placeholder="First name" />
              </el-form-item>
            </div>

            <div class="row">
              <el-form-item>
                <template #label>
                  <div class="mlabel">国籍 / Nationality / 国籍 / 국적</div>
                </template>
                <el-input v-model="g.nationality" placeholder="Nationality" />
              </el-form-item>
              <el-form-item>
                <template #label>
                  <div class="mlabel">Passport number (Required if overseas) / 旅券番号 / 护照号 / 여권번호</div>
                </template>
                <el-input v-model="g.passportNumber" placeholder="Passport number" />
              </el-form-item>
            </div>

            <div v-if="g.residenceType === 'OTHER'" class="upload">
              <div class="mlabel">Passport photo / 旅券写真 / 护照照片 / 여권사진 (Required if overseas)</div>
              <el-upload
                :show-file-list="false"
                :auto-upload="false"
                accept="image/*"
                :on-change="onPassportUploadChange.bind(null, g)"
              >
                <el-button>Choose image</el-button>
              </el-upload>
              <div class="upload-hint">
                <span v-if="g.passportUploaded" class="ok">Uploaded</span>
                <span v-else class="warn">Not uploaded yet</span>
              </div>
            </div>

            <div class="row">
              <el-form-item>
                <template #label>
                  <div class="mlabel">前泊地 / Prior stay / 前泊地 / 전 숙박지 (Optional)</div>
                </template>
                <el-input v-model="g.priorStay" placeholder="Optional" />
              </el-form-item>
              <el-form-item>
                <template #label>
                  <div class="mlabel">行先 / Next destination / 行先 / 다음 목적지 (Optional)</div>
                </template>
                <el-input v-model="g.nextDestination" placeholder="Optional" />
              </el-form-item>
            </div>
          </div>
        </el-form>

        <div class="actions">
          <el-button @click="step = 0">Back</el-button>
          <el-button :loading="saving" @click="saveDraft">Save</el-button>
          <el-button type="primary" @click="step = 2">Next</el-button>
        </div>
      </div>

      <div v-show="step === 2" class="card">
        <div class="card-title">Confirm & Submit / 確認 / 确认提交 / 제출</div>
        <div class="hint">
          Overseas guests (Residence=Other) must fill Passport number and upload passport photo.
        </div>
        <div class="actions">
          <el-button @click="step = 1">Back</el-button>
          <el-button :loading="saving" @click="saveDraft">Save</el-button>
          <el-button type="success" :loading="submitting" @click="submit">Submit</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { UploadFile } from 'element-plus'
import publicRequest from '@/utils/publicRequest'

type ResidenceType = 'JAPAN' | 'OTHER'

type GuestModel = {
  id?: number
  _key: string
  sortOrder?: number
  lastName?: string
  firstName?: string
  nationality?: string
  residenceType?: ResidenceType
  passportNumber?: string
  priorStay?: string
  nextDestination?: string
  passportUploaded?: boolean
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
  lastSavedAt?: string
  guests: Array<{
    id: number
    sortOrder: number
    lastName?: string
    firstName?: string
    nationality?: string
    residenceType?: ResidenceType
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

function token(): string {
  return (route.query.t as string) || ''
}

function orderNumber(): string {
  return route.params.orderNumber as string
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
    nationality: g.nationality,
    residenceType: g.residenceType,
    passportNumber: g.passportNumber,
    priorStay: g.priorStay,
    nextDestination: g.nextDestination,
    passportUploaded: attachments.some((a) => a.type === 'PASSPORT' && a.guestId === g.id),
  }))
}

async function uploadPassport(guest: GuestModel, file: File) {
  if (!guest.id) {
    throw new Error('Missing guestId')
  }
  const form = new FormData()
  form.append('file', file)

  const resp = (await publicRequest.post(`/public/registration/${orderNumber()}/attachments/passport`, form, {
    params: { t: token(), guestId: guest.id },
  })) as any
  if (!resp?.success) {
    throw new Error(resp?.message || 'Upload failed')
  }
  guest.passportUploaded = true

  // keep attachment state in sync (avoid full reload to not lose in-progress edits)
  if (data.value) {
    const next = Array.isArray((data.value as any).attachments) ? ([...(data.value as any).attachments] as AttachmentDTO[]) : ([] as AttachmentDTO[])
    const dto = resp.data as AttachmentDTO
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
    ElMessage.error(e?.message || 'Upload failed')
  }
}

async function load() {
  loading.value = true
  error.value = null
  try {
    const resp = await publicRequest.get(`/public/registration/${orderNumber()}`, { params: { t: token() } })
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
      guests: model.guests.map((g, idx) => ({
        id: g.id,
        sortOrder: idx + 1,
        lastName: g.lastName,
        firstName: g.firstName,
        nationality: g.nationality,
        residenceType: g.residenceType,
        passportNumber: g.passportNumber,
        priorStay: g.priorStay,
        nextDestination: g.nextDestination,
      })),
    }
    const resp = await publicRequest.put(`/public/registration/${orderNumber()}`, payload, { params: { t: token() } })
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
    await saveDraft()
    const resp = await publicRequest.post(`/public/registration/${orderNumber()}/submit`, null, { params: { t: token() } })
    hydrate(resp.data as PublicRegistrationResponse)
    ElMessage.success('Submitted')
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e?.message || 'Submit failed')
  } finally {
    submitting.value = false
  }
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
.guest-block {
  border: 1px solid #eee;
  border-radius: 12px;
  padding: 12px;
  margin-bottom: 10px;
}
.guest-title {
  font-weight: 700;
  margin-bottom: 8px;
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
.upload {
  margin-top: 8px;
}
.upload-hint {
  margin-top: 6px;
  font-size: 12px;
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
