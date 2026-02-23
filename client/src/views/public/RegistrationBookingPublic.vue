<template>
  <div class="page">
    <!-- Language Select -->
    <div v-if="!selectedLang" class="lang-select-container">
      <div class="lang-select-card">
        <div class="lang-select-title">Select Language / 言語を選択 / 选择语言 / 언어 선택</div>
        <div class="lang-options">
          <button class="lang-btn" @click="selectLanguage('en')">English</button>
          <button class="lang-btn" @click="selectLanguage('ja')">日本語</button>
          <button class="lang-btn" @click="selectLanguage('zh')">中文</button>
          <button class="lang-btn" @click="selectLanguage('ko')">한국어</button>
        </div>
      </div>
    </div>

    <template v-else>
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
          <div>Booking Number: {{ booking?.bookingKey }}</div>
          <div>{{ t('stay') }}: {{ booking?.checkInDate }} ~ {{ booking?.checkOutDate }}</div>
        </div>
      </div>

      <div class="notice">
        <div class="notice-title">{{ t('importantNotice') }}</div>
        <div class="notice-body">
          <div>{{ t('noticeText') }}</div>
        </div>
      </div>

      <div v-if="error" class="error">{{ error }}</div>

      <div v-if="booking" class="content">
        <div class="card">
          <div class="card-title">{{ t('bookingInfo') }}</div>
          <div class="grid">
            <div class="kv"><div class="k">{{ t('guest') }}</div><div class="v">{{ booking.guestName || '-' }}</div></div>
            <div class="kv"><div class="k">{{ t('stay') }}</div><div class="v">{{ booking.checkInDate }} ~ {{ booking.checkOutDate }}</div></div>
            <div class="kv"><div class="k">{{ t('rooms') }}</div><div class="v">{{ booking.rooms.length }}</div></div>
          </div>
        </div>

        <div v-for="room in booking.rooms" :key="room.orderNumber" class="room-card">
          <div class="room-header">
            <div class="room-title">
              <div class="room-title-main">
                {{ room.storeName || 'Store' }} / Room Number: {{ room.roomNumber || '-' }}
              </div>
              <div class="room-title-sub">Booking Number: {{ booking?.bookingKey }}</div>
            </div>

            <div class="status-area">
              <div v-if="room.lastSavedAt" class="last-saved">{{ t('lastSaved') }}: {{ formatLastSaved(room.lastSavedAt) }}</div>
              <el-tag :type="statusType(room.status)" effect="plain">{{ statusLabel(room.status) }}</el-tag>
            </div>
          </div>

          <div class="grid">
            <div class="kv"><div class="k">{{ t('stay') }}</div><div class="v">{{ room.checkInDate }} ~ {{ room.checkOutDate }}</div></div>
            <div class="kv"><div class="k">{{ t('maxGuests') }}</div><div class="v">{{ room.maxGuests }}</div></div>
          </div>

          <div class="form">
            <el-form label-position="top">
              <el-form-item>
                <template #label>
                  <div class="mlabel">{{ t('numberOfGuests') }} *</div>
                </template>
                <el-select
                  v-model="guestCountByOrder[room.orderNumber]"
                  style="width: 220px"
                  :disabled="savingOrder === room.orderNumber"
                >
                  <el-option v-for="n in guestCountOptions(room.maxGuests)" :key="n" :label="String(n)" :value="n" />
                </el-select>
              </el-form-item>
            </el-form>
          </div>

          <div class="actions">
            <el-button
              type="primary"
              :loading="savingOrder === room.orderNumber"
              @click="handleContinue(room.orderNumber, room.roomRegistrationLink)"
            >
              {{ t('continue') }}
            </el-button>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Setting } from '@element-plus/icons-vue'
import {
  getPublicRegistrationBooking,
  setRoomGuestCountFromBooking,
  type PublicRegistrationBookingResponse,
  type RegistrationFormStatus,
} from '@/api/publicRegistrationBooking'

const route = useRoute()

const bookingKey = computed(() => String(route.params.bookingKey || ''))
const token = computed(() => String(route.query.t || ''))

const loading = ref(false)
const error = ref('')
const booking = ref<PublicRegistrationBookingResponse | null>(null)

const savingOrder = ref<string | null>(null)
const guestCountByOrder = reactive<Record<string, number>>({})

// Initialize language from localStorage immediately to avoid showing language selection screen
const getInitialLanguage = (): string => {
  const savedLang = localStorage.getItem('registrationLang')
  if (savedLang) {
    return savedLang
  }
  // Auto-detect browser language
  const browserLang = navigator.language.toLowerCase()
  if (browserLang.startsWith('ja')) return 'ja'
  if (browserLang.startsWith('zh')) return 'zh'
  if (browserLang.startsWith('ko')) return 'ko'
  return 'en'
}

const selectedLang = ref<string>(getInitialLanguage())

type LangCode = 'en' | 'ja' | 'zh' | 'ko'

const translations: Record<LangCode, Record<string, string>> = {
  en: {
    guestRegistration: 'Guest Registration',
    loading: 'Loading...',
    stay: 'Stay',
    importantNotice: 'Important Notice',
    noticeText: 'Under Japanese law, overseas travelers must submit all guests\' personal information and passports to the accommodation. Thank you for your cooperation.',
    bookingInfo: 'Booking Information',
    guest: 'Guest',
    rooms: 'Rooms',
    maxGuests: 'Max guests',
    numberOfGuests: 'Number of guests',
    continue: 'Continue',
    lastSaved: 'Last saved',
    draft: 'Draft',
    submitted: 'Submitted',
    approved: 'Approved',
    rejected: 'Rejected',
    changeLanguage: 'Language'
  },
  ja: {
    guestRegistration: '宿泊者名簿',
    loading: '読み込み中...',
    stay: '宿泊期間',
    importantNotice: '重要なお知らせ',
    noticeText: '日本の法律により、海外からの宿泊者は全員の個人情報およびパスポートの提出が必要です。ご協力お願いします。',
    bookingInfo: '予約情報',
    guest: '宿泊者',
    rooms: '部屋数',
    maxGuests: '最大人数',
    numberOfGuests: '宿泊人数',
    continue: '続ける',
    lastSaved: '最終保存',
    draft: '未提出',
    submitted: '提出済み',
    approved: '承認済み',
    rejected: '要再提出',
    changeLanguage: '言語'
  },
  zh: {
    guestRegistration: '入住登记',
    loading: '加载中...',
    stay: '入住期间',
    importantNotice: '重要提示',
    noticeText: '根据日本法律，海外旅客需向住宿方提交所有入住者的个人信息及护照。感谢您的配合。',
    bookingInfo: '预订信息',
    guest: '客人',
    rooms: '房间数',
    maxGuests: '最多人数',
    numberOfGuests: '入住人数',
    continue: '继续填写',
    lastSaved: '最后保存',
    draft: '未提交',
    submitted: '已提交',
    approved: '已通过',
    rejected: '需重填',
    changeLanguage: '语言'
  },
  ko: {
    guestRegistration: '투숙자 등록',
    loading: '로딩 중...',
    stay: '숙박 기간',
    importantNotice: '중요 공지',
    noticeText: '일본 법에 따라 해외 투숙객은 모든 투숙객의 개인정보 및 여권 제출이 필요합니다. 협조 부탁드립니다.',
    bookingInfo: '예약 정보',
    guest: '투숙객',
    rooms: '객실 수',
    maxGuests: '최대 인원',
    numberOfGuests: '숙박 인원',
    continue: '계속하기',
    lastSaved: '최종 저장',
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

const selectLanguage = (lang: string) => {
  selectedLang.value = lang
  localStorage.setItem('registrationLang', lang)
  if (!booking.value) {
    load()
  }
}

const changeLanguage = (lang: string) => {
  selectedLang.value = lang
  localStorage.setItem('registrationLang', lang)
}

const guestCountOptions = (maxGuests: number) => {
  const safeMax = Math.max(1, Number(maxGuests || 1))
  return Array.from({ length: safeMax }, (_, i) => i + 1)
}

const formatLastSaved = (dateStr: string): string => {
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

const statusLabel = (status: RegistrationFormStatus) => {
  const statusKey = status.toLowerCase()
  return t(statusKey)
}

const statusType = (status: RegistrationFormStatus) => {
  switch (status) {
    case 'APPROVED':
      return 'success'
    case 'SUBMITTED':
      return 'warning'
    case 'REJECTED':
      return 'danger'
    default:
      return 'info'
  }
}

const load = async () => {
  error.value = ''
  if (!bookingKey.value || !token.value) {
    error.value = '缺少参数：bookingKey 或 t'
    return
  }

  loading.value = true
  try {
    const resp = await getPublicRegistrationBooking(bookingKey.value, token.value)
    if (!resp.success) {
      error.value = resp.message || '加载失败'
      return
    }

    booking.value = resp.data
    for (const room of resp.data.rooms || []) {
      if (room?.orderNumber) {
        guestCountByOrder[room.orderNumber] = Number(room.guestCount || 1)
      }
    }
  } catch (e) {
    console.error(e)
    error.value = '加载失败'
  } finally {
    loading.value = false
  }
}

const handleContinue = async (orderNumber: string, roomLink: string) => {
  if (!booking.value) {
    return
  }
  const guestCount = guestCountByOrder[orderNumber]
  if (!guestCount || guestCount < 1) {
    ElMessage.error('请选择入住人数')
    return
  }

  savingOrder.value = orderNumber
  try {
    await setRoomGuestCountFromBooking(bookingKey.value, orderNumber, token.value, guestCount)
    // Save selected language to pass to form page
    window.location.assign(roomLink + (roomLink.includes('?') ? '&' : '?') + 'lang=' + selectedLang.value)
  } catch (e) {
    console.error(e)
    ElMessage.error('保存失败，请重试')
  } finally {
    savingOrder.value = null
  }
}

onMounted(() => {
  // Ensure language is saved to localStorage
  if (selectedLang.value && !localStorage.getItem('registrationLang')) {
    localStorage.setItem('registrationLang', selectedLang.value)
  }
  load()
})
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: #f5f7fa;
  padding: 16px;
}

.header {
  background: #fff;
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 12px;
}

.title {
  font-size: 18px;
  font-weight: 700;
  margin-bottom: 6px;
}

.sub {
  font-size: 12px;
  color: #606266;
  display: grid;
  gap: 2px;
}

.notice {
  background: #fff;
  border-radius: 12px;
  padding: 14px 16px;
  margin-bottom: 12px;
  border: 1px solid #ebeef5;
}

.notice-title {
  font-weight: 700;
  margin-bottom: 6px;
}

.notice-body {
  font-size: 12px;
  color: #606266;
  display: grid;
  gap: 4px;
}

.content {
  display: grid;
  gap: 12px;
}

.card,
.room-card {
  background: #fff;
  border-radius: 12px;
  padding: 16px;
  border: 1px solid #ebeef5;
}

.card-title {
  font-weight: 700;
  margin-bottom: 10px;
}

.grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px 12px;
}

.kv {
  display: grid;
  gap: 2px;
}

.k {
  font-size: 12px;
  color: #909399;
}

.v {
  font-size: 13px;
  color: #303133;
}

.room-header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
  margin-bottom: 10px;
}

.status-area {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 6px;
}

.last-saved {
  font-size: 11px;
  color: #909399;
  white-space: nowrap;
}

/* 响应式布局：手机端 */
@media (max-width: 640px) {
  .header-top {
    flex-wrap: wrap;
    gap: 8px;
  }

  .header-top .el-button {
    white-space: nowrap;
  }

  .room-header {
    flex-direction: column;
    gap: 8px;
  }
  
  .status-area {
    width: 100%;
    flex-direction: row;
    justify-content: space-between;
    align-items: center;
  }
  
  .last-saved {
    order: 1;
  }
  
  .grid {
    grid-template-columns: 1fr;
  }
}

.room-title-main {
  font-weight: 700;
}

.room-title-sub {
  font-size: 12px;
  color: #909399;
}

.lang-select-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.lang-select-card {
  background: #fff;
  border-radius: 24px;
  padding: 48px 40px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  max-width: 560px;
  width: 100%;
  animation: fadeIn 0.5s ease-out;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(-20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.lang-select-title {
  font-size: 28px;
  font-weight: 700;
  text-align: center;
  margin-bottom: 32px;
  color: #1a1a1a;
  line-height: 1.4;
}

.lang-options {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

@media (max-width: 480px) {
  .lang-options {
    grid-template-columns: 1fr;
  }
  .lang-select-card {
    padding: 36px 24px;
  }
  .lang-select-title {
    font-size: 24px;
  }
}

.lang-btn {
  padding: 20px;
  font-size: 18px;
  font-weight: 600;
  background: linear-gradient(135deg, #f5f7fa 0%, #f0f2f5 100%);
  border: 2px solid #e4e7ed;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  color: #303133;
  position: relative;
  overflow: hidden;
}

.lang-btn::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.6), transparent);
  transition: left 0.5s;
}

.lang-btn:hover::before {
  left: 100%;
}

.lang-btn:hover {
  background: linear-gradient(135deg, #e8f4ff 0%, #d4e9ff 100%);
  border-color: #409eff;
  color: #409eff;
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(64, 158, 255, 0.3);
}

.lang-btn:active {
  transform: translateY(0);
  box-shadow: 0 4px 10px rgba(64, 158, 255, 0.2);
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

.form {
  margin-top: 12px;
}

.actions {
  margin-top: 8px;
  display: flex;
  justify-content: flex-end;
}

.mlabel {
  font-weight: 600;
}

.error {
  color: #f56c6c;
  background: #fff;
  border-radius: 12px;
  padding: 12px 16px;
  border: 1px solid #fde2e2;
}
</style>

