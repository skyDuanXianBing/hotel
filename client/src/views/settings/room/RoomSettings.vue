<template>
  <div class="room-settings-container">
    <!-- 顶部信息栏 -->
    <div class="header-info">
      <div class="header-actions">
        <el-button @click="handleRoomOwnership">{{ t('settingsStage4.roomSettings.roomOwnership') }}</el-button>
        <el-button type="primary" @click="handleAdd">{{ t('settings.common.add') }}</el-button>
      </div>
    </div>

    <!-- 数据表格 -->
    <div class="table-wrapper">
      <el-table
        v-loading="loading"
        :data="paginatedData"
        border
        stripe
        class="room-settings-table"
        :element-loading-text="t('settings.common.loading')"
      >
        <el-table-column prop="name" :label="t('settingsStage4.roomSettings.columns.roomTypeName')" min-width="150" fixed />
        <el-table-column prop="shortName" :label="t('settingsStage4.roomSettings.columns.shortName')" min-width="120" />

        <!-- 默认价格标题 -->
        <el-table-column align="center" :label="t('settingsStage4.roomSettings.columns.defaultPrice')">
          <el-table-column prop="mondayPrice" :label="t('settingsStage4.weekdays.monShort')" min-width="100" align="center">
            <template #default="{ row }">
              <span>¥{{ formatPrice(row.mondayPrice) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="tuesdayPrice" :label="t('settingsStage4.weekdays.tueShort')" min-width="100" align="center">
            <template #default="{ row }">
              <span>¥{{ formatPrice(row.tuesdayPrice) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="wednesdayPrice" :label="t('settingsStage4.weekdays.wedShort')" min-width="100" align="center">
            <template #default="{ row }">
              <span>¥{{ formatPrice(row.wednesdayPrice) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="thursdayPrice" :label="t('settingsStage4.weekdays.thuShort')" min-width="100" align="center">
            <template #default="{ row }">
              <span>¥{{ formatPrice(row.thursdayPrice) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="fridayPrice" :label="t('settingsStage4.weekdays.friShort')" min-width="100" align="center">
            <template #default="{ row }">
              <span>¥{{ formatPrice(row.fridayPrice) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="saturdayPrice" :label="t('settingsStage4.weekdays.satShort')" min-width="100" align="center">
            <template #default="{ row }">
              <span>¥{{ formatPrice(row.saturdayPrice) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="sundayPrice" :label="t('settingsStage4.weekdays.sunShort')" min-width="100" align="center">
            <template #default="{ row }">
              <span>¥{{ formatPrice(row.sundayPrice) }}</span>
            </template>
          </el-table-column>
        </el-table-column>

        <el-table-column prop="roomCount" :label="t('settingsStage4.roomSettings.columns.roomCount')" min-width="100" align="center" />
        <el-table-column prop="roomNumbers" :label="t('settingsStage4.roomSettings.columns.roomNumbers')" min-width="150">
          <template #default="{ row }">
            {{ (row.roomNumbers || []).join(', ') }}
          </template>
        </el-table-column>

        <el-table-column :label="t('settings.common.operation')" width="280" fixed="right" align="center">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button link type="primary" @click="handleEdit(row)">{{ t('settings.common.edit') }}</el-button>
              <el-button link type="primary" @click="handleViewDetails(row)">{{ t('settingsStage4.common.details') }}</el-button>
              <el-button link type="primary" @click="handleSort(row)">{{ t('settings.layout.items.roomSort') }}</el-button>
              <el-button link type="danger" @click="handleDelete(row)">{{ t('settings.common.delete') }}</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-container">
        <div class="pagination-info">{{ t('settingsStage4.common.itemsTotal', { count: total }) }}</div>
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[25, 50, 100]"
          layout="sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </div>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="showDialog"
      :title="dialogTitle"
      width="800px"
      :close-on-click-modal="false"
    >
      <el-form :model="formData" label-width="120px">
        <el-form-item :label="t('settingsStage4.roomSettings.fields.roomTypeName')" required>
          <el-input v-model="formData.name" :placeholder="t('settingsStage4.roomSettings.placeholders.roomTypeName')" />
        </el-form-item>

        <el-form-item :label="t('settingsStage4.roomSettings.fields.shortName')" required>
          <el-input v-model="formData.shortName" :placeholder="t('settingsStage4.roomSettings.placeholders.shortName')" />
        </el-form-item>

        <el-form-item :label="t('settingsStage4.roomSettings.fields.maxGuests')">
          <el-input-number v-model="formData.maxGuests" :min="1" />
        </el-form-item>

        <el-form-item :label="t('settingsStage4.roomSettings.fields.defaultWalkInRate')">
          <div class="price-section">
            <!-- 快速填充 -->
            <div class="quick-fill-section">
              <span class="quick-fill-label">{{ t('settingsStage4.roomSettings.fields.quickFill') }}</span>
              <el-input-number
                v-model="quickFillPrice"
                :min="0"
                :precision="0"
                :placeholder="t('settingsStage4.roomSettings.placeholders.quickFillPrice')"
                class="quick-fill-input"
              />
              <el-button @click="handleQuickFill" class="quick-fill-btn">{{ t('settingsStage4.common.applyToAll') }}</el-button>
            </div>

            <!-- 价格网格 -->
            <div class="price-grid">
              <div class="price-item">
                <span>{{ t('settingsStage4.weekdays.monShort') }}</span>
                <el-input-number v-model="formData.mondayPrice" :min="0" :precision="0" />
              </div>
              <div class="price-item">
                <span>{{ t('settingsStage4.weekdays.tueShort') }}</span>
                <el-input-number v-model="formData.tuesdayPrice" :min="0" :precision="0" />
              </div>
              <div class="price-item">
                <span>{{ t('settingsStage4.weekdays.wedShort') }}</span>
                <el-input-number v-model="formData.wednesdayPrice" :min="0" :precision="0" />
              </div>
              <div class="price-item">
                <span>{{ t('settingsStage4.weekdays.thuShort') }}</span>
                <el-input-number v-model="formData.thursdayPrice" :min="0" :precision="0" />
              </div>
              <div class="price-item">
                <span>{{ t('settingsStage4.weekdays.friShort') }}</span>
                <el-input-number v-model="formData.fridayPrice" :min="0" :precision="0" />
              </div>
              <div class="price-item">
                <span>{{ t('settingsStage4.weekdays.satShort') }}</span>
                <el-input-number v-model="formData.saturdayPrice" :min="0" :precision="0" />
              </div>
              <div class="price-item">
                <span>{{ t('settingsStage4.weekdays.sunShort') }}</span>
                <el-input-number v-model="formData.sundayPrice" :min="0" :precision="0" />
              </div>
            </div>
          </div>
        </el-form-item>

        <el-form-item :label="t('settingsStage4.roomSettings.fields.roomCount')">
          <el-input-number v-model="formData.roomCount" :min="0" />
          <span class="unit">{{ t('settingsStage4.common.unitRooms') }}</span>
        </el-form-item>

        <el-form-item :label="t('settingsStage4.roomSettings.fields.roomTypeAddress')">
          <el-input
            v-model="formData.roomTypeAddress"
            :placeholder="t('settingsStage4.roomSettings.placeholders.roomTypeAddress')"
            clearable
          />
        </el-form-item>

        <el-form-item :label="t('settingsStage4.roomSettings.fields.nearbyStation')">
          <el-input
            v-model="formData.nearbyStation"
            :placeholder="t('settingsStage4.roomSettings.placeholders.nearbyStation')"
            clearable
          />
        </el-form-item>

        <el-form-item :label="t('settingsStage4.roomSettings.fields.checkInGuideLink')">
          <el-input 
            v-model="formData.checkInGuideLink" 
            :placeholder="t('settingsStage4.roomSettings.placeholders.checkInGuideLink')"
            clearable
          />
          <div class="field-hint">{{ t('settingsStage4.roomSettings.hints.guideVisibleAfterApproval') }}</div>
        </el-form-item>

        <el-form-item :label="t('settingsStage4.roomSettings.fields.roomNumberAndPassword')">
          <div class="room-numbers-container">
            <div
              v-if="formData.rooms.length === 0"
              class="empty-room-hint"
            >
              {{ t('settingsStage4.roomSettings.hints.emptyRooms') }}
            </div>
            <div class="room-numbers-list">
              <div
                v-for="(_, index) in formData.rooms"
                :key="index"
                class="room-number-item"
              >
                <el-input
                  v-model="formData.rooms[index].roomNumber"
                  :placeholder="t('settingsStage4.roomSettings.placeholders.roomNumber')"
                  class="room-number-input"
                />
                <el-input
                  v-model="formData.rooms[index].smartlockPasscode"
                  :placeholder="t('settingsStage4.roomSettings.placeholders.roomPassword')"
                  class="room-passcode-input"
                  clearable
                />
                <el-button
                  type="danger"
                  circle
                  size="small"
                  @click="removeRoom(index)"
                  class="remove-room-btn"
                >
                  <el-icon><Delete /></el-icon>
                </el-button>
              </div>
            </div>
            <el-button type="primary" link @click="addNewRoom" class="add-room-btn">
              <el-icon><Plus /></el-icon> {{ t('settings.common.add') }}
            </el-button>
          </div>
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showDialog = false">{{ t('settings.common.cancel') }}</el-button>
          <el-button type="primary" @click="handleSave">{{ t('settings.common.save') }}</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, h } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Delete } from '@element-plus/icons-vue'
import {
  getAllRoomTypesWithRooms,
  createRoomType,
  updateRoomType,
  deleteRoomType,
  type RoomTypeDeleteBlockInfo,
} from '@/api/roomType'

const router = useRouter()
const { t } = useI18n()

interface RoomTypeData {
  id?: number
  name: string
  code: string
  shortName: string
  maxGuests: number
  defaultPrice?: number
  mondayPrice: number
  tuesdayPrice: number
  wednesdayPrice: number
  thursdayPrice: number
  fridayPrice: number
  saturdayPrice: number
  sundayPrice: number
  roomCount: number
  rooms: Array<{ roomNumber: string; smartlockPasscode: string }>
  roomTypeAddress?: string
  nearbyStation?: string
  checkInGuideLink?: string
}

const loading = ref(false)
const showDialog = ref(false)
const isEdit = ref(false)
const currentPage = ref(1)
const pageSize = ref(25)
const total = ref(0)

// 快速填充价格 - 默认为null避免误操作
const quickFillPrice = ref<number | null>(null)

// 房型数据列表
const roomTypeList = ref<RoomTypeData[]>([])
const reservationStatusTextMap = computed<Record<string, string>>(() => ({
  REQUESTED: t('settingsStage4.reservationStatus.REQUESTED'),
  CONFIRMED: t('settingsStage4.reservationStatus.CONFIRMED'),
  CHECKED_IN: t('settingsStage4.reservationStatus.CHECKED_IN'),
  CHECKED_OUT: t('settingsStage4.reservationStatus.CHECKED_OUT'),
  CANCELLED: t('settingsStage4.reservationStatus.CANCELLED'),
}))

// 表单数据
const formData = ref<RoomTypeData>({
  name: '',
  code: '',
  shortName: '',
  maxGuests: 4,
  mondayPrice: 25000,
  tuesdayPrice: 25000,
  wednesdayPrice: 25000,
  thursdayPrice: 25000,
  fridayPrice: 28000,
  saturdayPrice: 28000,
  sundayPrice: 25000,
  roomCount: 1,
  rooms: [],
  roomTypeAddress: '',
  nearbyStation: '',
  checkInGuideLink: ''
})

// 对话框标题
const dialogTitle = computed(() =>
  isEdit.value
    ? t('settingsStage4.roomSettings.dialog.editTitle')
    : t('settingsStage4.roomSettings.dialog.addTitle'),
)

// 分页数据
const paginatedData = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  return roomTypeList.value.slice(start, end)
})

// 格式化价格
const formatPrice = (price: number | undefined): string => {
  if (price === undefined || price === null) return '0'
  return price.toString()
}

const normalizeCheckInGuideLink = (rawLink?: string): string | undefined => {
  const trimmed = (rawLink || '').trim()
  if (!trimmed) {
    return undefined
  }
  if (/^https?:\/\//i.test(trimmed)) {
    return trimmed
  }
  if (/^[a-zA-Z][a-zA-Z\d+\-.]*:/.test(trimmed)) {
    return undefined
  }
  return `https://${trimmed}`
}

// 添加新房间号
const addNewRoom = () => {
  formData.value.rooms.push({ roomNumber: '', smartlockPasscode: '' })
}

// 删除房间号
const removeRoom = (index: number) => {
  if (formData.value.rooms.length > 0) {
    formData.value.rooms.splice(index, 1)
  }
}

// 监听房间数量变化,调整房间号数组长度
watch(
  () => formData.value.roomCount,
  (newCount) => {
    const currentLength = formData.value.rooms.length

    if (newCount > currentLength) {
      // 增加房间号输入框
      const roomsToAdd = newCount - currentLength
      for (let i = 0; i < roomsToAdd; i++) {
        formData.value.rooms.push({ roomNumber: '', smartlockPasscode: '' })
      }
    } else if (newCount < currentLength) {
      // 减少房间号输入框
      formData.value.rooms = formData.value.rooms.slice(0, newCount)
    }
  },
)

// 监听房间号数组变化,同步更新房间数量
watch(
  () => formData.value.rooms.length,
  (newLength) => {
    formData.value.roomCount = newLength
  },
)

// 快速填充价格到所有天
const handleQuickFill = () => {
  if (quickFillPrice.value === null || quickFillPrice.value === undefined) {
    ElMessage.warning(t('settingsStage4.roomSettings.messages.quickFillPriceRequired'))
    return
  }

  if (quickFillPrice.value < 0) {
    ElMessage.warning(t('settingsStage4.roomSettings.messages.priceCannotBeNegative'))
    return
  }

  // 确认是否要应用到全部
  ElMessageBox.confirm(
    t('settingsStage4.roomSettings.messages.quickFillConfirm', { price: quickFillPrice.value }),
    t('settingsStage4.roomSettings.messages.quickFillTitle'),
    {
      confirmButtonText: t('settings.common.confirmButton'),
      cancelButtonText: t('settings.common.cancelButton'),
      type: 'warning',
    }
  )
    .then(() => {
      formData.value.mondayPrice = quickFillPrice.value!
      formData.value.tuesdayPrice = quickFillPrice.value!
      formData.value.wednesdayPrice = quickFillPrice.value!
      formData.value.thursdayPrice = quickFillPrice.value!
      formData.value.fridayPrice = quickFillPrice.value!
      formData.value.saturdayPrice = quickFillPrice.value!
      formData.value.sundayPrice = quickFillPrice.value!
      ElMessage.success(t('settingsStage4.roomSettings.messages.quickFillSuccess'))
    })
    .catch(() => {
      // 用户取消,不执行任何操作
    })
}

// 加载房型数据
const loadRoomTypes = async () => {
  try {
    loading.value = true
    const response = await getAllRoomTypesWithRooms()
    if (response.success && response.data) {
      console.log('🔍 后端返回的原始数据:', response.data)

      // 将后端数据转换为前端格式
      roomTypeList.value = response.data.map((item: any) => {
        // 从rooms数组中提取房间号
        const rooms = item.rooms
          ? item.rooms.map((room: any) => ({
              roomNumber: room.roomNumber ?? '',
              smartlockPasscode: room.smartlockPasscode ?? '',
            }))
          : []

        console.log(`📊 房型 ${item.name} 的价格数据:`, {
          原始数据: {
            mondayPrice: item.mondayPrice,
            tuesdayPrice: item.tuesdayPrice,
            wednesdayPrice: item.wednesdayPrice,
            thursdayPrice: item.thursdayPrice,
            fridayPrice: item.fridayPrice,
            saturdayPrice: item.saturdayPrice,
            sundayPrice: item.sundayPrice,
            defaultPrice: item.defaultPrice,
            weekdayPrice: item.weekdayPrice,
            weekendPrice: item.weekendPrice,
          }
        })

        const mappedData = {
          id: item.id,
          name: item.name,
          code: item.code ?? '',
          shortName: item.description || item.code,
          maxGuests: item.maxGuests ?? 4,
          roomTypeAddress: item.roomTypeAddress ?? '',
          nearbyStation: item.nearbyStation ?? '',
          checkInGuideLink: item.checkInGuideLink ?? '',
          defaultPrice: item.defaultPrice ?? 0, // 保存原始defaultPrice
          mondayPrice: item.mondayPrice ?? item.weekdayPrice ?? item.defaultPrice ?? 0,
          tuesdayPrice: item.tuesdayPrice ?? item.weekdayPrice ?? item.defaultPrice ?? 0,
          wednesdayPrice: item.wednesdayPrice ?? item.weekdayPrice ?? item.defaultPrice ?? 0,
          thursdayPrice: item.thursdayPrice ?? item.weekdayPrice ?? item.defaultPrice ?? 0,
          fridayPrice: item.fridayPrice ?? item.weekdayPrice ?? item.defaultPrice ?? 0,
          saturdayPrice: item.saturdayPrice ?? item.weekendPrice ?? item.defaultPrice ?? 0,
          sundayPrice: item.sundayPrice ?? item.weekendPrice ?? item.defaultPrice ?? 0,
          roomCount: item.totalRooms || rooms.length,
          rooms: rooms,
        }

        console.log(`✅ 转换后的价格数据:`, {
          defaultPrice: mappedData.defaultPrice,
          mondayPrice: mappedData.mondayPrice,
          tuesdayPrice: mappedData.tuesdayPrice,
          wednesdayPrice: mappedData.wednesdayPrice,
          thursdayPrice: mappedData.thursdayPrice,
          fridayPrice: mappedData.fridayPrice,
          saturdayPrice: mappedData.saturdayPrice,
          sundayPrice: mappedData.sundayPrice,
        })

        return mappedData
      })
      total.value = roomTypeList.value.length
    } else {
      ElMessage.error(response.message || t('settingsStage4.roomSettings.messages.loadFailed'))
    }
  } catch (error) {
    console.error('加载房型数据失败:', error)
    ElMessage.error(t('settingsStage4.roomSettings.messages.loadFailed'))
  } finally {
    loading.value = false
  }
}

// 房间归属
const handleRoomOwnership = () => {
  router.push('/settings/room/ownership')
}

// 新增
const handleAdd = () => {
  isEdit.value = false
  formData.value = {
    name: '',
    code: '',
    shortName: '',
    maxGuests: 4,
    defaultPrice: 25000,
    mondayPrice: 25000,
    tuesdayPrice: 25000,
    wednesdayPrice: 25000,
    thursdayPrice: 25000,
    fridayPrice: 28000,
    saturdayPrice: 28000,
    sundayPrice: 25000,
    roomCount: 1,
    rooms: [{ roomNumber: '', smartlockPasscode: '' }],
    roomTypeAddress: '',
    nearbyStation: '',
    checkInGuideLink: ''
  }
  showDialog.value = true
}

// 编辑
const handleEdit = (row: RoomTypeData) => {
  isEdit.value = true
  console.log('✏️ 编辑房型数据:', row)

  // 深拷贝数据,确保数组也是新的引用
  formData.value = {
    ...row,
    rooms: row.rooms && row.rooms.length > 0 ? row.rooms.map(r => ({ ...r })) : [{ roomNumber: '', smartlockPasscode: '' }],
    roomTypeAddress: row.roomTypeAddress || '',
    nearbyStation: row.nearbyStation || '',
    checkInGuideLink: row.checkInGuideLink || ''
  }

  console.log('📋 formData设置为:', formData.value)
  showDialog.value = true
}

// 查看详情
const handleViewDetails = (row: RoomTypeData) => {
  router.push(`/settings/room-type/${row.id}/details`)
}

// 排序
const handleSort = (_row?: RoomTypeData) => {
  router.push('/settings/room/room-sort')
}

// 删除
const handleDelete = (row: RoomTypeData) => {
  ElMessageBox.confirm(
    t('settingsStage4.roomSettings.messages.deleteConfirm', { name: row.name }),
    t('settingsStage4.roomSettings.messages.deleteTitle'),
    {
      confirmButtonText: t('settings.common.confirmButton'),
      cancelButtonText: t('settings.common.cancelButton'),
      type: 'warning',
    }
  )
    .then(async () => {
      try {
        loading.value = true
        if (row.id) {
          const response = await deleteRoomType(row.id)
          if (response.success) {
            ElMessage.success(t('settings.common.deleteSuccess'))
            await loadRoomTypes()
          } else {
            const blockInfo = response.data as RoomTypeDeleteBlockInfo | null
            if (blockInfo && blockInfo.totalBlockingReservations > 0) {
              const sampleCount = blockInfo.sample?.length || 0
              const header = t('settingsStage4.roomSettings.messages.deleteBlockedHeader', {
                count: blockInfo.totalBlockingReservations,
                sampleCount,
              })
              const body = sampleCount
                ? blockInfo.sample
                    .map((r, idx) => {
                      const orderNumber = r.orderNumber || '-'
                      const statusCode = r.status || '-'
                      const statusText = reservationStatusTextMap.value[statusCode] || statusCode
                      const roomNumber = r.roomNumber || '-'
                      const dateRange = `${r.checkInDate || '-'} ~ ${r.checkOutDate || '-'}`
                      return t('settingsStage4.roomSettings.messages.deleteBlockedSample', {
                        index: idx + 1,
                        orderNumber,
                        statusText,
                        statusCode,
                        roomNumber,
                        dateRange,
                      })
                    })
                    .join('\n')
                : t('settingsStage4.roomSettings.messages.deleteBlockedNoSample')
              const tip = t('settingsStage4.roomSettings.messages.deleteBlockedTip')

              await ElMessageBox.alert(
                h('div', { style: 'white-space: pre-wrap; line-height: 1.6;' }, header + body + tip),
                t('settingsStage4.roomSettings.messages.deleteBlockedTitle'),
                { type: 'warning', confirmButtonText: t('settingsStage4.common.gotIt') }
              )
              return
            }
            await ElMessageBox.alert(response.message || t('settings.common.deleteFailed'), t('settings.common.deleteFailed'), {
              type: 'error',
              confirmButtonText: t('settingsStage4.common.gotIt'),
            })
          }
        }
      } catch (error) {
        console.error('删除失败:', error)
        ElMessage.error(t('settings.common.deleteFailed'))
      } finally {
        loading.value = false
      }
    })
    .catch(() => {
      // 用户取消
    })
}

// 保存
const handleSave = async () => {
  if (!formData.value.name.trim()) {
    ElMessage.warning(t('settingsStage4.roomSettings.messages.nameRequired'))
    return
  }
  if (!formData.value.shortName.trim()) {
    ElMessage.warning(t('settingsStage4.roomSettings.messages.shortNameRequired'))
    return
  }
  if (isEdit.value && !formData.value.code.trim()) {
    ElMessage.error(t('settingsStage4.roomSettings.messages.codeMissing'))
    return
  }

  // 过滤掉空的房间号
  const validRooms = formData.value.rooms
    .map(item => ({
      roomNumber: (item.roomNumber || '').trim(),
      smartlockPasscode: (item.smartlockPasscode || '').trim(),
    }))
    .filter(item => item.roomNumber.length > 0)

  const validRoomNumbers = validRooms.map(item => item.roomNumber)

  if (validRoomNumbers.length === 0) {
    ElMessage.warning(t('settingsStage4.roomSettings.messages.roomNumberRequired'))
    return
  }

  // 检查房间号是否重复
  const uniqueRoomNumbers = new Set(validRoomNumbers)
  if (uniqueRoomNumbers.size !== validRoomNumbers.length) {
    ElMessage.warning(t('settingsStage4.roomSettings.messages.roomNumberDuplicate'))
    return
  }

  try {
    loading.value = true

    // 构建请求数据
    // 后端当前规则：同一门店下房间号全局唯一（不同房型也不能重复）。
    // 这里在前端先做一次冲突校验，避免保存后才失败。
    const occupiedRoomNumberToRoomType = new Map<string, RoomTypeData>()
    for (const rt of roomTypeList.value) {
      // 编辑时允许与自己原有房间号重复
      if (isEdit.value && formData.value.id && rt.id === formData.value.id) continue
      for (const rn of (rt.rooms || []).map(r => r.roomNumber) || []) {
        const normalized = (rn || '').trim()
        if (normalized) {
          occupiedRoomNumberToRoomType.set(normalized, rt)
        }
      }
    }

    const conflictRoomNumbers = validRoomNumbers.filter(rn => occupiedRoomNumberToRoomType.has(rn))
    if (conflictRoomNumbers.length > 0) {
      const first = conflictRoomNumbers[0]
      const conflictRoomType = occupiedRoomNumberToRoomType.get(first)
      const roomTypeName = conflictRoomType
        ? `${conflictRoomType.name} (${conflictRoomType.shortName})`
        : t('settingsStage4.common.otherRoomType')
      ElMessage.warning(
        t('settingsStage4.roomSettings.messages.roomNumberConflict', {
          roomNumber: first,
          roomTypeName,
        }),
      )
      return
    }

    const normalizedCheckInGuideLink = normalizeCheckInGuideLink(formData.value.checkInGuideLink)
    if (formData.value.checkInGuideLink?.trim() && !normalizedCheckInGuideLink) {
      ElMessage.warning(t('settingsStage4.roomSettings.messages.invalidGuideLink'))
      return
    }

    const requestData = {
      name: formData.value.name,
      code: isEdit.value ? formData.value.code : formData.value.shortName.substring(0, 3).toUpperCase(),
      description: formData.value.shortName,
      totalRooms: validRoomNumbers.length, // 使用实际房间号数量
      maxGuests: formData.value.maxGuests,
      roomTypeAddress: formData.value.roomTypeAddress?.trim() || undefined,
      nearbyStation: formData.value.nearbyStation?.trim() || undefined,
      checkInGuideLink: normalizedCheckInGuideLink ? normalizedCheckInGuideLink : undefined,
      // 编辑时保持原有defaultPrice,新增时使用周一价格作为默认价格
      defaultPrice: isEdit.value && formData.value.defaultPrice !== undefined
        ? formData.value.defaultPrice
        : formData.value.mondayPrice,
      mondayPrice: formData.value.mondayPrice,
      tuesdayPrice: formData.value.tuesdayPrice,
      wednesdayPrice: formData.value.wednesdayPrice,
      thursdayPrice: formData.value.thursdayPrice,
      fridayPrice: formData.value.fridayPrice,
      saturdayPrice: formData.value.saturdayPrice,
      sundayPrice: formData.value.sundayPrice,
      rooms: validRooms,
      roomNumbers: validRoomNumbers,
    }

    console.log('💾 准备保存的数据:', requestData)
    console.log('📝 编辑模式:', isEdit.value, '房型ID:', formData.value.id)

    let response
    if (isEdit.value && formData.value.id) {
      response = await updateRoomType(formData.value.id, requestData)
    } else {
      response = await createRoomType(requestData)
    }

    console.log('✅ 保存响应:', response)

    if (response.success) {
      ElMessage.success(
        isEdit.value
          ? t('settingsStage4.roomSettings.messages.updateSuccess')
          : t('settingsStage4.roomSettings.messages.createSuccess'),
      )
      showDialog.value = false
      await loadRoomTypes()
    } else {
      ElMessage.error(
        response.message ||
          (isEdit.value
            ? t('settingsStage4.roomSettings.messages.updateFailed')
            : t('settingsStage4.roomSettings.messages.createFailed')),
      )
    }
  } catch (error) {
    console.error('保存失败:', error)
    ElMessage.error(t('settingsStage4.roomSettings.messages.saveFailed'))
  } finally {
    loading.value = false
  }
}

// 分页处理
const handleSizeChange = (val: number) => {
  pageSize.value = val
  currentPage.value = 1
}

const handleCurrentChange = (val: number) => {
  currentPage.value = val
}

onMounted(() => {
  loadRoomTypes()
})
</script>

<style scoped>
.room-settings-container {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #fff;
}

/* 顶部信息栏 */
.header-info {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #e8e8e8;
  background: #f5f7fa;
}

.header-actions {
  display: flex;
  gap: 12px;
}

/* 表格容器 */
.table-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 20px;
  overflow: hidden;
}

.room-settings-table {
  flex: 1;
  overflow: auto;
}

/* 操作按钮 */
.action-buttons {
  display: flex;
  gap: 8px;
  justify-content: center;
  flex-wrap: wrap;
}

/* 分页容器 */
.pagination-container {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 0;
  border-top: 1px solid #e8e8e8;
  margin-top: 16px;
}

.pagination-info {
  font-size: 14px;
  color: #666;
}

/* 对话框 */
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

/* 价格部分容器 */
.price-section {
  width: 100%;
}

/* 快速填充部分 */
.quick-fill-section {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  padding: 12px;
  background: #f5f7fa;
  border-radius: 4px;
}

.quick-fill-label {
  font-size: 14px;
  color: #666;
  min-width: 70px;
}

.quick-fill-input {
  width: 200px;
}

.quick-fill-btn {
  flex-shrink: 0;
}

/* 价格网格 */
.price-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
  width: 100%;
}

.price-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.price-item span {
  min-width: 50px;
  font-size: 14px;
  color: #666;
}

/* 表格样式优化 */
:deep(.el-table) {
  font-size: 14px;
}

:deep(.el-table th) {
  background: #fafafa;
  color: #333;
  font-weight: 600;
}

:deep(.el-table td) {
  padding: 12px 0;
}

:deep(.el-table__header-wrapper .el-table__cell) {
  background: #fafafa;
}

/* 输入框样式 */
:deep(.el-input-number) {
  width: 100%;
}

/* 房间号管理样式 */
.unit {
  margin-left: 8px;
  color: #606266;
}

.field-hint {
  margin-top: 8px;
  font-size: 12px;
  color: #909399;
  line-height: 1.5;
}

.room-numbers-container {
  width: 100%;
}

.empty-room-hint {
  color: #909399;
  font-size: 12px;
  padding: 8px 0;
  line-height: 1.5;
  margin-bottom: 12px;
}

.room-numbers-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 12px;
}

.room-number-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.room-number-input {
  flex: 1;
  max-width: 400px;
}

.room-passcode-input {
  flex: 1;
  max-width: 400px;
}

.remove-room-btn {
  flex-shrink: 0;
}

.add-room-btn {
  font-size: 14px;
  padding: 0;
}
</style>
