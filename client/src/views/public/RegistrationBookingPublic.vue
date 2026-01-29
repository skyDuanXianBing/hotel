<template>
  <div class="page">
    <div class="header">
      <div class="title">Guest Registration / 宿泊者名簿 / 入住登记 / 투숙자 등록</div>
      <div v-if="loading" class="sub">Loading...</div>
      <div v-else class="sub">
        <div>Booking: {{ booking?.bookingKey }}</div>
        <div>Stay: {{ booking?.checkInDate }} ~ {{ booking?.checkOutDate }}</div>
      </div>
    </div>

    <div class="notice">
      <div class="notice-title">Important Notice</div>
      <div class="notice-body">
        <div>
          Under Japanese law, overseas travelers must submit all guests' personal information and passports to the
          accommodation. Thank you for your cooperation.
        </div>
        <div>日本の法律により、海外からの宿泊者は全員の個人情報およびパスポートの提出が必要です。ご協力お願いします。</div>
        <div>根据日本法律，海外旅客需向住宿方提交所有入住者的个人信息及护照。感谢您的配合。</div>
        <div>일본 법에 따라 해외 투숙객은 모든 투숙객의 개인정보 및 여권 제출이 필요합니다. 협조 부탁드립니다.</div>
      </div>
    </div>

    <div v-if="error" class="error">{{ error }}</div>

    <div v-if="booking" class="content">
      <div class="card">
        <div class="card-title">Booking Information / 予約情報 / 预订信息 / 예약 정보</div>
        <div class="grid">
          <div class="kv"><div class="k">Guest</div><div class="v">{{ booking.guestName || '-' }}</div></div>
          <div class="kv"><div class="k">Stay</div><div class="v">{{ booking.checkInDate }} ~ {{ booking.checkOutDate }}</div></div>
          <div class="kv"><div class="k">Rooms</div><div class="v">{{ booking.rooms.length }}</div></div>
        </div>
      </div>

      <div v-for="room in booking.rooms" :key="room.orderNumber" class="room-card">
        <div class="room-header">
          <div class="room-title">
            <div class="room-title-main">
              {{ room.roomTypeName || 'Room' }} <span v-if="room.roomNumber">/ {{ room.roomNumber }}</span>
            </div>
            <div class="room-title-sub">Order: {{ room.orderNumber }}</div>
          </div>

          <el-tag :type="statusType(room.status)" effect="plain">{{ statusLabel(room.status) }}</el-tag>
        </div>

        <div class="grid">
          <div class="kv"><div class="k">Stay</div><div class="v">{{ room.checkInDate }} ~ {{ room.checkOutDate }}</div></div>
          <div class="kv"><div class="k">Max guests</div><div class="v">{{ room.maxGuests }}</div></div>
          <div class="kv"><div class="k">Last saved</div><div class="v">{{ room.lastSavedAt || '-' }}</div></div>
        </div>

        <div class="form">
          <el-form label-position="top">
            <el-form-item>
              <template #label>
                <div class="mlabel">Number of guests / 宿泊人数 / 入住人数 / 숙박 인원 *</div>
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
            继续填写 / Continue
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
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

const guestCountOptions = (maxGuests: number) => {
  const safeMax = Math.max(1, Number(maxGuests || 1))
  return Array.from({ length: safeMax }, (_, i) => i + 1)
}

const statusLabel = (status: RegistrationFormStatus) => {
  switch (status) {
    case 'DRAFT':
      return '未提交 / Draft'
    case 'SUBMITTED':
      return '已提交 / Submitted'
    case 'APPROVED':
      return '已通过 / Approved'
    case 'REJECTED':
      return '需重填 / Rejected'
    default:
      return String(status)
  }
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
    window.location.assign(roomLink)
  } catch (e) {
    console.error(e)
    ElMessage.error('保存失败，请重试')
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

.room-title-main {
  font-weight: 700;
}

.room-title-sub {
  font-size: 12px;
  color: #909399;
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

