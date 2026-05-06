<template>
  <StatisticsLayout>
    <div class="wrap">
      <div class="header">
        <div class="title">人员信息审查</div>
        <div class="actions">
          <el-select v-model="status" placeholder="状态" clearable style="width: 160px" @change="load">
            <el-option label="草稿" value="DRAFT" />
            <el-option label="已提交" value="SUBMITTED" />
            <el-option label="已通过" value="APPROVED" />
            <el-option label="已驳回" value="REJECTED" />
          </el-select>
          <el-button @click="openLinkDrawer">链接列表</el-button>
          <el-button :loading="loading" @click="load">刷新</el-button>
        </div>
      </div>

      <div class="filters">
        <el-select
          v-model="channelId"
          filterable
          clearable
          placeholder="平台"
          style="width: 200px"
        >
          <el-option v-for="c in channels" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
        <el-select
          v-model="reservationStatus"
          clearable
          placeholder="订单状态"
          style="width: 180px"
        >
          <el-option
            v-for="option in reservationStatusOptions"
            :key="option.value"
            :label="option.label"
            :value="option.value"
          />
        </el-select>
        <el-date-picker
          v-model="checkInDate"
          type="date"
          value-format="YYYY-MM-DD"
          clearable
          placeholder="入住日期"
          style="width: 160px"
        />
        <el-date-picker
          v-model="checkOutDate"
          type="date"
          value-format="YYYY-MM-DD"
          clearable
          placeholder="离店日期"
          style="width: 160px"
        />
        <el-button type="primary" :loading="loading" @click="load">搜索</el-button>
        <el-button :disabled="loading" @click="resetFilters">重置</el-button>
      </div>

      <el-table :data="rows" border stripe style="width: 100%" @row-click="go">
        <el-table-column prop="channelOrderNumber" label="渠道订单号" min-width="160" />
        <el-table-column prop="channelName" label="渠道" min-width="140" />
        <el-table-column prop="guestName" label="客人" min-width="120" />
        <el-table-column prop="checkInDate" label="入住" min-width="110" />
        <el-table-column prop="checkOutDate" label="离店" min-width="110" />
        <el-table-column prop="status" label="状态" min-width="110" />
        <el-table-column prop="submittedAt" label="提交时间" min-width="170" />
        <el-table-column prop="updatedAt" label="更新时间" min-width="170" />
      </el-table>
    </div>

    <el-drawer v-model="linkDrawerVisible" title="链接列表" size="80%">
      <div class="drawer-actions">
        <el-select
          v-model="linkReservationStatus"
          clearable
          placeholder="预订状态"
          style="width: 160px"
          @change="loadLinks"
        >
          <el-option
            v-for="option in linkReservationStatusOptions"
            :key="option.value"
            :label="option.label"
            :value="option.value"
          />
        </el-select>
        <el-button :loading="linkLoading" @click="loadLinks">刷新</el-button>
      </div>

      <el-table :data="linkRows" border stripe style="width: 100%" v-loading="linkLoading">
        <el-table-column prop="createdAt" label="时间" min-width="170" />
        <el-table-column prop="guestName" label="客人" min-width="110" />
        <el-table-column prop="checkInDate" label="入住" min-width="110" />
        <el-table-column prop="checkOutDate" label="离店" min-width="110" />
        <el-table-column label="预订状态" min-width="120">
          <template #default="{ row }">
            {{ getReservationStatusLabel(row.reservationStatus) }}
          </template>
        </el-table-column>
        <el-table-column prop="roomCount" label="房间数" width="90" />
        <el-table-column label="链接" min-width="240">
          <template #default="{ row }">
            <div class="link-cell">
              <el-input :model-value="row.linkUrl" readonly size="small" />
              <el-button size="small" @click="copy(row.linkUrl)">复制</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-drawer>
  </StatisticsLayout>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import { getAllChannels, type ChannelDTO } from '@/api/channel'
import { getRegistrationLinkInbox, type RegistrationLinkInboxItemDTO } from '@/api/registrationLinkInbox'

type Row = {
  formId: number
  orderNumber: string
  channelOrderNumber?: string | null
  channelName?: string | null
  guestName: string
  checkInDate: string
  checkOutDate: string
  reservationStatus?: string | null
  status: string
  submittedAt?: string
  updatedAt?: string
}

const router = useRouter()
const rows = ref<Row[]>([])
const loading = ref(false)
const status = ref<string | null>(null)
const channels = ref<ChannelDTO[]>([])
const channelId = ref<number | null>(null)
const reservationStatus = ref<string | null>(null)
const checkInDate = ref<string | null>(null)
const checkOutDate = ref<string | null>(null)
const linkDrawerVisible = ref(false)
const linkLoading = ref(false)
const linkRows = ref<RegistrationLinkInboxItemDTO[]>([])
const linkReservationStatus = ref<string | null>(null)

const reservationStatusOptions = [
  { label: '待确认', value: 'REQUESTED' },
  { label: '已预订', value: 'CONFIRMED' },
  { label: '已入住', value: 'CHECKED_IN' },
  { label: '已退房', value: 'CHECKED_OUT' },
  { label: '已取消', value: 'CANCELLED' },
  { label: '未到店', value: 'NO_SHOW' },
]

const linkReservationStatusOptions = [
  { label: '已预订', value: 'CONFIRMED' },
  { label: '已取消', value: 'CANCELLED' },
]

function toDayNumber(dateValue?: string | null) {
  if (!dateValue) {
    return null
  }

  const match = dateValue.trim().match(/^(\d{4})-(\d{2})-(\d{2})$/)
  if (!match) {
    return null
  }

  const [, year, month, day] = match
  return Math.floor(Date.UTC(Number(year), Number(month) - 1, Number(day)) / 86400000)
}

function getTodayDayNumber() {
  const now = new Date()
  return Math.floor(Date.UTC(now.getFullYear(), now.getMonth(), now.getDate()) / 86400000)
}

function compareRowsByCheckInPriority(left: Row, right: Row) {
  const today = getTodayDayNumber()
  const leftCheckIn = toDayNumber(left.checkInDate)
  const rightCheckIn = toDayNumber(right.checkInDate)

  const getPriority = (dayNumber: number | null) => {
    if (dayNumber == null) {
      return { bucket: 2, distance: Number.MAX_SAFE_INTEGER }
    }
    if (dayNumber >= today) {
      return { bucket: 0, distance: dayNumber - today }
    }
    return { bucket: 1, distance: today - dayNumber }
  }

  const leftPriority = getPriority(leftCheckIn)
  const rightPriority = getPriority(rightCheckIn)

  if (leftPriority.bucket !== rightPriority.bucket) {
    return leftPriority.bucket - rightPriority.bucket
  }
  if (leftPriority.distance !== rightPriority.distance) {
    return leftPriority.distance - rightPriority.distance
  }

  const leftUpdatedAt = left.updatedAt || left.submittedAt || ''
  const rightUpdatedAt = right.updatedAt || right.submittedAt || ''
  return rightUpdatedAt.localeCompare(leftUpdatedAt)
}

async function loadChannels() {
  try {
    const resp = await getAllChannels()
    channels.value = resp.success ? (resp.data || []) : []
    if (!resp.success) {
      ElMessage.error(resp.message || '加载渠道失败')
    }
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e?.message || '加载渠道失败')
  }
}

function resetFilters() {
  channelId.value = null
  reservationStatus.value = null
  checkInDate.value = null
  checkOutDate.value = null
  load()
}

async function load() {
  loading.value = true
  try {
    const params: Record<string, string | number> = {}
    if (status.value) params.status = status.value
    if (channelId.value) params.channelId = channelId.value
    if (reservationStatus.value) params.reservationStatus = reservationStatus.value
    if (checkInDate.value) params.checkInDate = checkInDate.value
    if (checkOutDate.value) params.checkOutDate = checkOutDate.value

    const resp = (await request.get('/registrations', { params })) as {
      success: boolean
      message?: string
      data?: Row[]
    }
    if (resp.success) {
      rows.value = ((resp.data || []) as Row[])
        .map((r) => ({
          ...r,
          channelOrderNumber: r.channelOrderNumber || '-',
          channelName: r.channelName || '-',
        }))
        .sort(compareRowsByCheckInPriority)
    } else {
      rows.value = []
      ElMessage.error(resp.message || '加载失败')
    }
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

function go(row: Row) {
  router.push({ name: 'DataCenterRegistrationDetail', params: { formId: row.formId } })
}

async function loadLinks() {
  linkLoading.value = true
  try {
    const resp = await getRegistrationLinkInbox(linkReservationStatus.value)
    linkRows.value = resp.success ? (resp.data || []) : []
    if (!resp.success) {
      ElMessage.error(resp.message || '加载失败')
    }
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e?.message || '加载失败')
  } finally {
    linkLoading.value = false
  }
}

function openLinkDrawer() {
  linkDrawerVisible.value = true
  loadLinks()
}

function getReservationStatusLabel(status?: string | null) {
  switch (status) {
    case 'CONFIRMED':
      return '已预订'
    case 'CANCELLED':
      return '已取消'
    case 'CHECKED_IN':
      return '已入住'
    case 'CHECKED_OUT':
      return '已退房'
    case 'REQUESTED':
      return '待确认'
    case 'NO_SHOW':
      return '未到店'
    default:
      return '-'
  }
}

async function copy(text: string) {
  if (!text) {
    return
  }
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success('已复制')
  } catch {
    const textarea = document.createElement('textarea')
    textarea.value = text
    textarea.style.position = 'fixed'
    textarea.style.left = '-9999px'
    document.body.appendChild(textarea)
    textarea.select()
    document.execCommand('copy')
    document.body.removeChild(textarea)
    ElMessage.success('已复制')
  }
}

onMounted(() => {
  load()
  loadChannels()
})
</script>

<style scoped>
.wrap {
  padding: 16px;
}
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}
.filters {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
  margin-bottom: 12px;
}
.title {
  font-size: 16px;
  font-weight: 700;
}
.actions {
  display: flex;
  gap: 10px;
  align-items: center;
}
.drawer-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-bottom: 12px;
}
.link-cell {
  display: flex;
  gap: 8px;
  align-items: center;
}
</style>
