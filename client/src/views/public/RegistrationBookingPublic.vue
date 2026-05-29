<template>
  <div class="page">
    <!-- Language Select -->
    <div v-if="!selectedLang" class="lang-select-container">
      <div class="lang-select-card">
        <div class="lang-select-title">{{ t('languageSelectTitle') }}</div>
        <div class="lang-options">
          <button
            v-for="option in languageOptions"
            :key="option.value"
            class="lang-btn"
            @click="selectLanguage(option.value)"
          >
            {{ t(option.labelKey) }}
          </button>
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
                <el-dropdown-item
                  v-for="option in languageOptions"
                  :key="option.value"
                  :command="option.value"
                  :disabled="selectedLang === option.value"
                >
                  {{ t(option.labelKey) }}
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
        <div v-if="loading" class="sub">{{ t('loading') }}</div>
        <div v-else class="sub">
          <div>{{ t('bookingNumber') }}: {{ booking?.bookingKey }}</div>
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
                {{ room.roomTypeName || t('roomType') }} / {{ t('roomNumber') }}: {{ room.roomNumber || '-' }}
              </div>
              <div class="room-title-sub">{{ t('bookingNumber') }}: {{ booking?.bookingKey }}</div>
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
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { Setting } from '@element-plus/icons-vue'
import type { SupportedLocale } from '@/locales'
import { useLanguageStore } from '@/stores/language'
import {
  getPublicRegistrationBooking,
  setRoomGuestCountFromBooking,
  type PublicRegistrationBookingResponse,
  type RegistrationFormStatus,
} from '@/api/publicRegistrationBooking'
import {
  normalizeRegistrationLocale,
  persistRegistrationLocale,
  resolvePublicRegistrationLocale,
  resolvePublicRegistrationErrorKey,
  shouldApplyLoadedRegistrationLocale,
  shouldPersistRegistrationLocale,
  type PublicRegistrationLocaleResolution,
  type PublicRegistrationLocaleSource,
  type PublicRegistrationErrorKey,
} from './registrationLocale'

const route = useRoute()
const languageStore = useLanguageStore()
const { t: i18nT, locale } = useI18n()

const bookingKey = computed(() => String(route.params.bookingKey || ''))
const token = computed(() => String(route.query.t || ''))

const loading = ref(false)
const error = ref('')
const loadErrorKey = ref<PublicRegistrationErrorKey | null>(null)
const booking = ref<PublicRegistrationBookingResponse | null>(null)

const savingOrder = ref<string | null>(null)
const guestCountByOrder = reactive<Record<string, number>>({})
const currentLocaleSource = ref<PublicRegistrationLocaleSource>('default')
const userSelectedLocale = ref(false)

const selectedLang = computed(() => languageStore.locale)

const registrationTextKeys: Record<string, string> = {
  languageSelectTitle: 'stage5.publicRegistration.language.selectTitle',
  english: 'stage5.publicRegistration.language.english',
  japanese: 'stage5.publicRegistration.language.japanese',
  simplifiedChinese: 'stage5.publicRegistration.language.simplifiedChinese',
  traditionalChinese: 'stage5.publicRegistration.language.traditionalChinese',
  guestRegistration: 'stage5.publicRegistration.common.guestRegistration',
  loading: 'stage5.publicRegistration.common.loading',
  bookingNumber: 'stage5.publicRegistration.common.bookingNumber',
  stay: 'stage5.publicRegistration.common.stay',
  guest: 'stage5.publicRegistration.common.guest',
  roomType: 'stage5.publicRegistration.common.roomType',
  roomNumber: 'stage5.publicRegistration.common.roomNumber',
  rooms: 'stage5.publicRegistration.common.rooms',
  maxGuests: 'stage5.publicRegistration.common.maxGuests',
  numberOfGuests: 'stage5.publicRegistration.common.numberOfGuests',
  continue: 'stage5.publicRegistration.common.continue',
  lastSaved: 'stage5.publicRegistration.common.lastSaved',
  changeLanguage: 'stage5.publicRegistration.common.changeLanguage',
  importantNotice: 'stage5.publicRegistration.booking.importantNotice',
  noticeText: 'stage5.publicRegistration.booking.noticeText',
  bookingInfo: 'stage5.publicRegistration.booking.bookingInfo',
  missingParams: 'stage5.publicRegistration.booking.missingParams',
  loadFailed: 'stage5.publicRegistration.booking.loadFailed',
  selectGuestCount: 'stage5.publicRegistration.booking.selectGuestCount',
  saveFailed: 'stage5.publicRegistration.booking.saveFailed',
  draft: 'stage5.publicRegistration.status.draft',
  submitted: 'stage5.publicRegistration.status.submitted',
  approved: 'stage5.publicRegistration.status.approved',
  rejected: 'stage5.publicRegistration.status.rejected',
}

const languageOptions: Array<{ value: SupportedLocale; labelKey: string }> = [
  { value: 'en', labelKey: 'english' },
  { value: 'ja', labelKey: 'japanese' },
  { value: 'zh-CN', labelKey: 'simplifiedChinese' },
  { value: 'zh-TW', labelKey: 'traditionalChinese' },
]

const getQueryLocale = (value: unknown): string | null => {
  if (Array.isArray(value)) {
    return typeof value[0] === 'string' ? value[0] : null
  }
  return typeof value === 'string' ? value : null
}

const applyRegistrationLocaleResolution = (
  resolution: PublicRegistrationLocaleResolution,
  persist = shouldPersistRegistrationLocale(resolution),
): SupportedLocale => {
  locale.value = resolution.locale
  languageStore.setLocale(resolution.locale)
  currentLocaleSource.value = resolution.source
  if (persist) {
    persistRegistrationLocale(resolution.locale)
  }
  return resolution.locale
}

const applyRegistrationLocale = (value?: string | null): SupportedLocale => {
  return applyRegistrationLocaleResolution(
    {
      locale: normalizeRegistrationLocale(value),
      source: 'registrationCache',
    },
    true,
  )
}

applyRegistrationLocaleResolution(
  resolvePublicRegistrationLocale({ queryLocale: getQueryLocale(route.query.lang) }),
)

const t = (key: string, params: Record<string, string | number> = {}): string => {
  return String(i18nT(registrationTextKeys[key] || key, params))
}

const publicRegistrationErrorText = (key: PublicRegistrationErrorKey): string => {
  return String(i18nT(`stage5.publicRegistration.errors.${key}`))
}

const setPublicRegistrationError = (
  message?: string | null,
  fallback: PublicRegistrationErrorKey = 'loadFailed',
) => {
  const nextErrorKey = resolvePublicRegistrationErrorKey(message, fallback)
  loadErrorKey.value = nextErrorKey
  error.value = publicRegistrationErrorText(nextErrorKey)
}

const clearPublicRegistrationError = () => {
  loadErrorKey.value = null
  error.value = ''
}

const refreshPublicRegistrationError = () => {
  if (loadErrorKey.value) {
    error.value = publicRegistrationErrorText(loadErrorKey.value)
  }
}

const selectLanguage = (lang: SupportedLocale) => {
  userSelectedLocale.value = true
  applyRegistrationLocale(lang)
  refreshPublicRegistrationError()
  if (!booking.value) {
    load()
  }
}

const changeLanguage = (lang: string) => {
  userSelectedLocale.value = true
  applyRegistrationLocale(lang)
  refreshPublicRegistrationError()
}

const guestCountOptions = (maxGuests: number) => {
  const safeMax = Math.max(1, Number(maxGuests || 1))
  return Array.from({ length: safeMax }, (_, i) => i + 1)
}

const buildRoomRegistrationLink = (roomLink: string): string => {
  const lang = selectedLang.value
  return roomLink + (roomLink.includes('?') ? '&' : '?') + 'lang=' + encodeURIComponent(lang)
}

const resolveLoadedRegistrationLocale = (
  resp: PublicRegistrationBookingResponse,
): PublicRegistrationLocaleResolution => {
  return resolvePublicRegistrationLocale({
    // The public booking API does not expose guest region fields yet.
    // Add country/region/phoneCountryCode here when backend support is available.
    backendHint: null,
    guestName: resp.guestName,
    storedRegistrationLocale: null,
    storedAppLocale: null,
    browserLocales: [],
  })
}

const applyLoadedRegistrationLocale = (resp: PublicRegistrationBookingResponse) => {
  if (userSelectedLocale.value) {
    return
  }

  const resolution = resolveLoadedRegistrationLocale(resp)
  if (!shouldApplyLoadedRegistrationLocale(currentLocaleSource.value, resolution)) {
    return
  }

  applyRegistrationLocaleResolution(resolution)
  refreshPublicRegistrationError()
}

const shouldAutoRedirectToRegistrationForm = (
  resp: PublicRegistrationBookingResponse
): boolean => {
  const rooms = resp.rooms || []
  return rooms.length > 0 && rooms.every((room) => room.status === 'APPROVED')
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
  clearPublicRegistrationError()
  if (!bookingKey.value || !token.value) {
    error.value = t('missingParams')
    return
  }

  loading.value = true
  try {
    const resp = await getPublicRegistrationBooking(bookingKey.value, token.value)
    if (!resp.success) {
      setPublicRegistrationError(resp.message)
      return
    }

    booking.value = resp.data
    applyLoadedRegistrationLocale(resp.data)

    if (shouldAutoRedirectToRegistrationForm(resp.data)) {
      const firstRoomLink = (resp.data.rooms || []).find((room) => !!room.roomRegistrationLink)
      if (firstRoomLink?.roomRegistrationLink) {
        window.location.replace(buildRoomRegistrationLink(firstRoomLink.roomRegistrationLink))
        return
      }
    }

    for (const room of resp.data.rooms || []) {
      if (room?.orderNumber) {
        guestCountByOrder[room.orderNumber] = Number(room.guestCount || 1)
      }
    }
  } catch (e) {
    console.error(e)
    error.value = t('loadFailed')
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
    ElMessage.error(t('selectGuestCount'))
    return
  }

  savingOrder.value = orderNumber
  try {
    await setRoomGuestCountFromBooking(bookingKey.value, orderNumber, token.value, guestCount)
    // Save selected language to pass to form page
    window.location.assign(buildRoomRegistrationLink(roomLink))
  } catch (e) {
    console.error(e)
    ElMessage.error(t('saveFailed'))
  } finally {
    savingOrder.value = null
  }
}

onMounted(() => {
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
