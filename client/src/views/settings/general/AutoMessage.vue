<template>
  <div class="auto-message-container">
    <div class="page-header">
      <h2 class="page-title">{{ t('settings.autoMessage.title') }}</h2>
      <el-button type="primary" @click="handleCreate">{{ t('settings.autoMessage.create') }}</el-button>
    </div>

    <el-table :data="messages" border stripe v-loading="loading">
      <el-table-column prop="title" :label="t('settings.autoMessage.columns.title')" min-width="120" />
      <el-table-column prop="message" :label="t('settings.autoMessage.columns.message')" min-width="200">
        <template #default="{ row }">
          <span class="message-cell">{{ row.message }}</span>
        </template>
      </el-table-column>
      <el-table-column :label="t('settings.autoMessage.columns.automationRule')" min-width="220">
        <template #default="{ row }">
          <div class="automation-rule-cell">
            <div class="automation-rule-action">
              {{ t('settings.autoMessage.ruleAction', { action: row.automationRuleAction }) }}
            </div>
            <div class="automation-rule-timing">
              {{ t('settings.autoMessage.ruleTiming', { timing: row.automationRuleTiming }) }}
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="channel" :label="t('settings.autoMessage.columns.channel')" min-width="120" />
      <el-table-column prop="room" :label="t('settings.autoMessage.columns.room')" min-width="120" />
      <el-table-column :label="t('settings.autoMessage.columns.enabled')" width="80" align="center">
        <template #default="{ row }">
          <el-switch v-model="row.enabled" @change="handleToggle(row)" />
        </template>
      </el-table-column>
      <el-table-column :label="t('settings.common.actions')" width="180" align="center" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="handleEdit(row)">{{ t('settings.common.edit') }}</el-button>
          <el-button link type="primary" @click="handleCopy(row)">{{ t('settings.autoMessage.copy') }}</el-button>
          <el-button link type="danger" @click="handleDelete(row)">{{ t('settings.common.delete') }}</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 创建/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="700px"
      :close-on-click-modal="false"
    >
      <el-form :model="form" :rules="formRules" ref="formRef" label-position="top">
        <!-- 标题 -->
        <el-form-item :label="t('settings.autoMessage.fields.title')" prop="title" required>
          <el-input v-model="form.title" :placeholder="t('settings.autoMessage.placeholders.title')" />
        </el-form-item>

        <!-- 消息内容 -->
        <el-form-item :label="t('settings.autoMessage.fields.message')" prop="message" required>
          <el-input
            ref="messageInputRef"
            v-model="form.message"
            type="textarea"
            :rows="5"
            :placeholder="t('settings.autoMessage.placeholders.message')"
          />
        </el-form-item>

        <!-- 插入变量 -->
        <div class="variable-section">
          <div class="variable-title">{{ t('settings.autoMessage.fields.variables') }}</div>
          <div class="variable-desc">{{ t('settings.autoMessage.variableDesc') }}</div>
          <div class="variable-tags">
            <el-tag
              v-for="variable in messageVariables"
              :key="variable.code"
              class="variable-tag"
              effect="plain"
              @click="insertVariable(variable.code)"
            >
              {{ t(variable.label) }}
            </el-tag>
          </div>
        </div>

        <!-- 渠道（多选） -->
        <el-form-item :label="t('settings.autoMessage.fields.channel')" prop="selectedChannels" required>
          <el-select
            v-model="form.selectedChannels"
            multiple
            :placeholder="t('settings.autoMessage.placeholders.channel')"
            style="width: 100%"
          >
            <el-option
              v-for="channel in channels"
              :key="channel.id"
              :label="channel.name"
              :value="channel.id"
            />
          </el-select>
        </el-form-item>

        <!-- 过时补发 -->
        <el-form-item :label="t('settings.autoMessage.fields.resendOnExpire')">
          <el-switch v-model="form.resendOnExpire" />
        </el-form-item>

        <!-- 房间选择 -->
        <el-form-item :label="t('settings.autoMessage.fields.room')" prop="roomSelectionType" required>
          <div class="room-selection">
            <el-radio-group v-model="form.roomSelectionType" class="room-radio-group">
              <el-radio value="ALL_LOCAL">{{ t('settings.autoMessage.roomSelection.allLocal') }}</el-radio>
              <el-radio value="BY_ROOM_TYPE">{{ t('settings.autoMessage.roomSelection.byRoomType') }}</el-radio>
              <el-radio value="BY_GROUP">{{ t('settings.autoMessage.roomSelection.byGroup') }}</el-radio>
              <el-radio value="BY_ROOM">{{ t('settings.autoMessage.roomSelection.byRoom') }}</el-radio>
            </el-radio-group>
            <el-select
              v-if="form.roomSelectionType !== 'ALL_LOCAL'"
              v-model="form.selectedRooms"
              multiple
              :placeholder="t('settings.autoMessage.placeholders.select')"
              style="width: 100%; margin-top: 12px"
            >
              <!-- 根据房型 -->
              <template v-if="form.roomSelectionType === 'BY_ROOM_TYPE'">
                <el-option
                  v-for="roomType in roomTypes"
                  :key="roomType.id"
                  :label="roomType.name"
                  :value="roomType.id"
                />
              </template>
              <!-- 根据分组 -->
              <template v-else-if="form.roomSelectionType === 'BY_GROUP'">
                <el-option
                  v-for="group in roomGroups"
                  :key="group.id"
                  :label="group.name"
                  :value="group.id"
                />
              </template>
              <!-- 按房间 -->
              <template v-else-if="form.roomSelectionType === 'BY_ROOM'">
                <el-option
                  v-for="room in rooms"
                  :key="room.id"
                  :label="room.roomNumber"
                  :value="room.id"
                />
              </template>
            </el-select>
          </div>
        </el-form-item>

        <!-- 自动化规则 -->
        <div class="automation-section">
          <div class="automation-title">{{ t('settings.autoMessage.fields.automationRule') }}</div>
          <div class="automation-desc">{{ t('settings.autoMessage.automationDesc') }}</div>
        </div>

        <el-form-item :label="t('settings.autoMessage.fields.action')" prop="action" required>
          <el-select v-model="form.action" :placeholder="t('settings.autoMessage.placeholders.select')" style="width: 100%">
            <el-option :label="t('settings.autoMessage.actions.bookingConfirm')" value="BOOKING_CONFIRM" />
            <el-option :label="t('settings.autoMessage.actions.checkIn')" value="CHECK_IN" />
            <el-option :label="t('settings.autoMessage.actions.checkOut')" value="CHECK_OUT" />
          </el-select>
        </el-form-item>

        <!-- 入住/离店：按预订入住/离店日期 + 天数偏移 + 时间 -->
        <template v-if="form.action === 'CHECK_IN' || form.action === 'CHECK_OUT'">
          <el-form-item :label="t('settings.autoMessage.fields.day')" prop="day" required>
            <el-select v-model="form.day" :placeholder="t('settings.autoMessage.placeholders.select')" style="width: 100%">
              <el-option
                v-for="option in dayOptions"
                :key="option.value"
                :label="option.label"
                :value="option.value"
              />
            </el-select>
          </el-form-item>

          <el-form-item :label="t('settings.autoMessage.fields.time')" prop="time" required>
            <el-select v-model="form.time" :placeholder="t('settings.autoMessage.placeholders.select')" style="width: 100%">
              <el-option
                v-for="option in timeOptions"
                :key="option.value"
                :label="option.label"
                :value="option.value"
              />
            </el-select>
          </el-form-item>
        </template>

        <!-- 预订确认：按预订创建时间 + 延迟 -->
        <el-form-item v-else :label="t('settings.autoMessage.fields.sendTiming')" prop="sendTiming" required>
          <el-select v-model="form.sendTiming" :placeholder="t('settings.autoMessage.placeholders.select')" style="width: 100%">
            <el-option
              v-for="option in bookingTimingOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </el-form-item>

        <!-- 密码变量警告提示 -->
        <el-alert
          v-if="hasPasswordVariable"
          type="warning"
          :closable="false"
          show-icon
          class="password-warning"
        >
          <template #title>
            {{ t('settings.autoMessage.passwordWarning') }}
          </template>
        </el-alert>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">{{ t('settings.common.cancel') }}</el-button>
          <el-button type="primary" @click="handleSave">{{ t('settings.common.save') }}</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch, nextTick } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  getAllAutoMessages,
  createAutoMessage,
  updateAutoMessage,
  deleteAutoMessage,
  toggleAutoMessage,
  type AutoMessageDTO,
  type RoomSelectionType,
  type AutoMessageAction,
  type SendTiming,
} from '@/api/autoMessage'
import { getAllChannels, type ChannelDTO } from '@/api/channel'
import { getRooms, type RoomDTO } from '@/api/room'
import { getAllRoomTypes, type RoomTypeDTO } from '@/api/roomType'
import { getAllRoomGroups, type RoomGroupDTO } from '@/api/roomGroup'
import { useStoreStore } from '@/stores/store'

const { t } = useI18n()

/** 消息变量定义 */
interface MessageVariable {
  label: string
  code: string
}

/** 消息变量列表 */
const messageVariables: MessageVariable[] = [
  { label: 'settings.autoMessage.variables.propertyName', code: '{{property_name}}' },
  { label: 'settings.autoMessage.variables.guestName', code: '{{guest_name}}' },
  { label: 'settings.autoMessage.variables.guestPhone', code: '{{guest_phone}}' },
  { label: 'settings.autoMessage.variables.checkInDate', code: '{{checkin_date}}' },
  { label: 'settings.autoMessage.variables.checkoutDate', code: '{{checkout_date}}' },
  { label: 'settings.autoMessage.variables.roomTypeName', code: '{{room_type_name}}' },
  { label: 'settings.autoMessage.variables.roomTypeAddress', code: '{{room_type_address}}' },
  { label: 'settings.autoMessage.variables.nearbyStation', code: '{{nearby_station}}' },
  { label: 'settings.autoMessage.variables.ratePlanName', code: '{{rate_plan_name}}' },
  { label: 'settings.autoMessage.variables.propertyAddress', code: '{{property_address}}' },
  { label: 'settings.autoMessage.variables.propertyCity', code: '{{property_city}}' },
  { label: 'settings.autoMessage.variables.propertyPhone', code: '{{property_phone}}' },
  { label: 'settings.autoMessage.variables.propertyEmail', code: '{{property_email}}' },
  { label: 'settings.autoMessage.variables.confirmationCode', code: '{{confirmation_code}}' },
  { label: 'settings.autoMessage.variables.registrationLink', code: '{{registration_link}}' },
  { label: 'settings.autoMessage.variables.checkInFormLink', code: '{{checkin_form_link}}' },
  { label: 'settings.autoMessage.variables.numberOfNights', code: '{{number_of_nights}}' },
  { label: 'settings.autoMessage.variables.checkInCode', code: '{{checkin_code}}' },
  { label: 'settings.autoMessage.variables.smartLockPasscode', code: '{{smartlock_passcode}}' },
  { label: 'settings.autoMessage.variables.roomNumber', code: '{{room_number}}' },
]

interface AutoMessageForm {
  id: number
  title: string
  message: string
  selectedChannels: number[]
  resendOnExpire: boolean
  roomSelectionType: RoomSelectionType
  selectedRooms: number[]
  action: AutoMessageAction | ''
  sendTiming: SendTiming | ''
  /** 入住/离店：天数偏移（-14~14） */
  day: string
  /** 入住/离店：时间（HH:mm） */
  time: string
  enabled: boolean
}

/** 列表显示用的接口 */
interface AutoMessageListItem {
  id: number
  title: string
  message: string
  automationRule: string
  automationRuleAction: string
  automationRuleTiming: string
  channel: string
  room: string
  enabled: boolean
}

const storeStore = useStoreStore()
const loading = ref(false)
const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const messageInputRef = ref()
const editingId = ref<number | null>(null)

const messages = ref<AutoMessageListItem[]>([])
const channels = ref<ChannelDTO[]>([])
const rooms = ref<RoomDTO[]>([])
const roomTypes = ref<RoomTypeDTO[]>([])
const roomGroups = ref<RoomGroupDTO[]>([])

const form = reactive<AutoMessageForm>({
  id: 0,
  title: '',
  message: '',
  selectedChannels: [],
  resendOnExpire: false,
  roomSelectionType: 'ALL_LOCAL',
  selectedRooms: [],
  action: '',
  sendTiming: '',
  day: '',
  time: '',
  enabled: true,
})

let isPopulatingForm = false

// 检测消息中是否包含密码变量
const hasPasswordVariable = computed(() => {
  const passwordVars = ['{{smartlock_passcode}}', '{{checkin_code}}']
  return passwordVars.some((v) => form.message.includes(v))
})

interface SendTimingOption {
  label: string
  value: string
}

/** 天数选项（用于入住/离店） */
const dayOptions = computed<SendTimingOption[]>(() => {
  const options: SendTimingOption[] = []
  for (let day = -14; day <= 14; day++) {
    let label = ''
    if (day < 0) {
      label = t('settings.autoMessage.timing.dayBefore', { count: Math.abs(day) })
    } else if (day === 0) {
      label = t('settings.autoMessage.timing.sameDay')
    } else {
      label = t('settings.autoMessage.timing.dayAfter', { count: day })
    }
    options.push({ label, value: String(day) })
  }
  return options
})

const bookingTimingOptions = computed<SendTimingOption[]>(() => [
  { label: t('settings.autoMessage.timing.immediately'), value: 'IMMEDIATELY' },
  { label: t('settings.autoMessage.timing.minutesAfter', { count: 5 }), value: '5_MIN' },
  { label: t('settings.autoMessage.timing.minutesAfter', { count: 10 }), value: '10_MIN' },
  { label: t('settings.autoMessage.timing.minutesAfter', { count: 15 }), value: '15_MIN' },
  { label: t('settings.autoMessage.timing.minutesAfter', { count: 30 }), value: '30_MIN' },
  { label: t('settings.autoMessage.timing.hoursAfter', { count: 1 }), value: '1_HOUR' },
  { label: t('settings.autoMessage.timing.hoursAfter', { count: 2 }), value: '2_HOUR' },
  { label: t('settings.autoMessage.timing.hoursAfter', { count: 4 }), value: '4_HOUR' },
  { label: t('settings.autoMessage.timing.hoursAfter', { count: 8 }), value: '8_HOUR' },
  { label: t('settings.autoMessage.timing.hoursAfter', { count: 16 }), value: '16_HOUR' },
  { label: t('settings.autoMessage.timing.hoursAfter', { count: 24 }), value: '24_HOUR' },
])

/** 时间选项（用于入住/离店） */
const timeOptions = computed<SendTimingOption[]>(() => {
  const options: SendTimingOption[] = []
  for (let hour = 0; hour < 24; hour++) {
    const value = `${hour.toString().padStart(2, '0')}:00`
    options.push({ label: value, value })
  }
  return options
})

const normalizeTimeValue = (value: string) => {
  if (!value) {
    return ''
  }
  const normalized = value.trim()
  const match = normalized.match(/^([01]\d|2[0-3]):([0-5]\d)(?::[0-5]\d)?$/)
  if (!match) {
    return normalized
  }
  return `${match[1]}:${match[2]}`
}

// 监听房间选择类型变化，清空已选房间
watch(
  () => form.roomSelectionType,
  () => {
    if (isPopulatingForm) {
      return
    }
    form.selectedRooms = []
  }
)

// 监听操作类型变化，清空发送时机相关字段
watch(
  () => form.action,
  () => {
    if (isPopulatingForm) {
      return
    }
    form.sendTiming = ''
    form.day = ''
    form.time = ''
  }
)

const parseDayAndTime = (sendTiming: string) => {
  if (!sendTiming || !sendTiming.startsWith('DAY_')) {
    return null
  }
  const decoded = decodeURIComponent(sendTiming)
  const exactMatch = decoded.match(/^DAY_(-?\d+)_([01]\d|2[0-3]):([0-5]\d)(?::[0-5]\d)?$/)
  if (exactMatch) {
    return {
      day: exactMatch[1],
      time: `${exactMatch[2]}:${exactMatch[3]}`,
    }
  }

  const parts = decoded.split('_')
  if (parts.length >= 3) {
    const day = parts[1]
    const time = normalizeTimeValue(parts.slice(2).join('_'))
    if (/^-?\d+$/.test(day) && /^([01]\d|2[0-3]):[0-5]\d$/.test(time)) {
      return { day, time }
    }
  }
  return null
}

const formRules: FormRules = {
  title: [{ required: true, message: t('settings.autoMessage.validation.titleRequired'), trigger: 'blur' }],
  message: [{ required: true, message: t('settings.autoMessage.validation.messageRequired'), trigger: 'blur' }],
  selectedChannels: [
    {
      required: true,
      message: t('settings.autoMessage.validation.channelRequired'),
      trigger: 'change',
      type: 'array',
      min: 1,
    },
  ],
  roomSelectionType: [
    { required: true, message: t('settings.autoMessage.validation.roomTypeRequired'), trigger: 'change' },
  ],
  action: [{ required: true, message: t('settings.autoMessage.validation.actionRequired'), trigger: 'change' }],
  sendTiming: [
    { required: true, message: t('settings.autoMessage.validation.sendTimingRequired'), trigger: 'change' },
  ],
  day: [{ required: true, message: t('settings.autoMessage.validation.dayRequired'), trigger: 'change' }],
  time: [{ required: true, message: t('settings.autoMessage.validation.timeRequired'), trigger: 'change' }],
}

/** 插入变量到消息内容 */
const insertVariable = (code: string) => {
  const textarea = messageInputRef.value?.$el?.querySelector('textarea')
  if (textarea) {
    const start = textarea.selectionStart
    const end = textarea.selectionEnd
    const text = form.message
    form.message = text.substring(0, start) + code + text.substring(end)
    // 设置光标位置
    setTimeout(() => {
      textarea.focus()
      textarea.setSelectionRange(start + code.length, start + code.length)
    }, 0)
  } else {
    form.message += code
  }
}

const dialogTitle = computed(() => {
  return editingId.value
    ? t('settings.autoMessage.dialogTitle.edit')
    : t('settings.autoMessage.dialogTitle.create')
})

// 加载渠道列表
const loadChannels = async () => {
  try {
    const response = await getAllChannels()
    if (response.success && response.data) {
      channels.value = response.data
    } else {
      ElMessage.error(response.message || t('settings.autoMessage.messages.loadChannelsFailed'))
    }
  } catch (error) {
    console.error('加载渠道列表失败:', error)
    ElMessage.error(t('settings.autoMessage.messages.loadChannelsFailed'))
  }
}

// 加载房间列表
const loadRooms = async () => {
  try {
    const response = await getRooms()
    if (response.success && response.data) {
      rooms.value = response.data
    } else {
      ElMessage.error(response.message || t('settings.autoMessage.messages.loadRoomsFailed'))
    }
  } catch (error) {
    console.error('加载房间列表失败:', error)
    ElMessage.error(t('settings.autoMessage.messages.loadRoomsFailed'))
  }
}

// 加载房型列表
const loadRoomTypes = async () => {
  try {
    const response = await getAllRoomTypes()
    if (response.success && response.data) {
      roomTypes.value = response.data
    } else {
      ElMessage.error(response.message || t('settings.autoMessage.messages.loadRoomTypesFailed'))
    }
  } catch (error) {
    console.error('加载房型列表失败:', error)
    ElMessage.error(t('settings.autoMessage.messages.loadRoomTypesFailed'))
  }
}

// 加载房间分组列表
const loadRoomGroups = async () => {
  try {
    const response = await getAllRoomGroups()
    if (response.success && response.data) {
      roomGroups.value = response.data
    } else {
      ElMessage.error(response.message || t('settings.autoMessage.messages.loadRoomGroupsFailed'))
    }
  } catch (error) {
    console.error('加载房间分组列表失败:', error)
    ElMessage.error(t('settings.autoMessage.messages.loadRoomGroupsFailed'))
  }
}

/** 操作类型显示映射 */
const actionLabelMap: Record<string, string> = {
  BOOKING_CONFIRM: t('settings.autoMessage.actions.bookingConfirm'),
  CHECK_IN: t('settings.autoMessage.actions.checkIn'),
  CHECK_OUT: t('settings.autoMessage.actions.checkOut'),
}

const bookingTimingLabelMap: Record<string, string> = {
  IMMEDIATELY: t('settings.autoMessage.timing.immediately'),
  '5_MIN': t('settings.autoMessage.timing.minutesAfter', { count: 5 }),
  '10_MIN': t('settings.autoMessage.timing.minutesAfter', { count: 10 }),
  '15_MIN': t('settings.autoMessage.timing.minutesAfter', { count: 15 }),
  '30_MIN': t('settings.autoMessage.timing.minutesAfter', { count: 30 }),
  '1_HOUR': t('settings.autoMessage.timing.hoursAfter', { count: 1 }),
  '2_HOUR': t('settings.autoMessage.timing.hoursAfter', { count: 2 }),
  '4_HOUR': t('settings.autoMessage.timing.hoursAfter', { count: 4 }),
  '8_HOUR': t('settings.autoMessage.timing.hoursAfter', { count: 8 }),
  '16_HOUR': t('settings.autoMessage.timing.hoursAfter', { count: 16 }),
  '24_HOUR': t('settings.autoMessage.timing.hoursAfter', { count: 24 }),
}

const formatSendTimingLabel = (action: AutoMessageAction | '', sendTiming: string | undefined) => {
  if (!sendTiming) {
    return '-'
  }

  if (action === 'CHECK_IN' || action === 'CHECK_OUT') {
    const parsed = parseDayAndTime(sendTiming)
    if (!parsed) {
      return sendTiming
    }
    const day = Number(parsed.day)
    const dayText =
      day < 0
        ? t('settings.autoMessage.timing.dayBefore', { count: Math.abs(day) })
        : day === 0
          ? t('settings.autoMessage.timing.sameDay')
          : t('settings.autoMessage.timing.dayAfter', { count: day })
    const anchor =
      action === 'CHECK_IN'
        ? t('settings.autoMessage.timing.checkInAnchor')
        : t('settings.autoMessage.timing.checkOutAnchor')
    return `${anchor}${dayText} ${parsed.time}`
  }

  return bookingTimingLabelMap[sendTiming] || sendTiming
}

/** 将 DTO 转换为列表显示项 */
const convertToListItem = (dto: AutoMessageDTO): AutoMessageListItem => {
  // 解析渠道
  let channelDisplay = dto.channel || ''
  if (dto.channels) {
    try {
      const channelIds = JSON.parse(dto.channels) as number[]
      const channelNames = channelIds
        .map((id) => channels.value.find((c) => c.id === id)?.name)
        .filter(Boolean)
      channelDisplay = channelNames.join(', ') || t('settings.autoMessage.display.allChannels')
    } catch {
      channelDisplay = dto.channel || t('settings.autoMessage.display.allChannels')
    }
  }

  // 解析房间
  let roomDisplay = dto.room || ''
  if (dto.roomSelectionType) {
    switch (dto.roomSelectionType) {
      case 'ALL_LOCAL':
        roomDisplay = t('settings.autoMessage.roomSelection.allLocal')
        break
      case 'BY_ROOM_TYPE':
        if (dto.roomSelection) {
          try {
            const roomTypeIds = JSON.parse(dto.roomSelection) as number[]
            const names = roomTypeIds
              .map((id) => roomTypes.value.find((rt) => rt.id === id)?.name)
              .filter(Boolean)
            roomDisplay = names.join(', ') || t('settings.autoMessage.display.byRoomType')
          } catch {
            roomDisplay = t('settings.autoMessage.display.byRoomType')
          }
        }
        break
      case 'BY_GROUP':
        if (dto.roomSelection) {
          try {
            const groupIds = JSON.parse(dto.roomSelection) as number[]
            const names = groupIds
              .map((id) => roomGroups.value.find((g) => g.id === id)?.name)
              .filter(Boolean)
            roomDisplay = names.join(', ') || t('settings.autoMessage.display.byGroup')
          } catch {
            roomDisplay = t('settings.autoMessage.display.byGroup')
          }
        }
        break
      case 'BY_ROOM':
        if (dto.roomSelection) {
          try {
            const roomIds = JSON.parse(dto.roomSelection) as number[]
            const names = roomIds
              .map((id) => rooms.value.find((r) => r.id === id)?.roomNumber)
              .filter(Boolean)
            roomDisplay = names.join(', ') || t('settings.autoMessage.display.byRoom')
          } catch {
            roomDisplay = t('settings.autoMessage.display.byRoom')
          }
        }
        break
    }
  }

  const actionText = dto.action ? actionLabelMap[dto.action] || dto.automationRule : dto.automationRule || '-'
  const sendTimingText = formatSendTimingLabel(dto.action || '', dto.sendTiming)

  return {
    id: dto.id,
    title: dto.title,
    message: dto.message,
    automationRule: actionText,
    automationRuleAction: actionText,
    automationRuleTiming: sendTimingText,
    channel: channelDisplay,
    room: roomDisplay,
    enabled: dto.enabled,
  }
}

// 加载自动化消息列表
const loadAutoMessages = async () => {
  if (!storeStore.currentStore?.id) {
    ElMessage.warning(t('settings.autoMessage.messages.selectStore'))
    messages.value = []
    return
  }

  try {
    loading.value = true
    const response = await getAllAutoMessages()
    if (response.success && response.data) {
      messages.value = response.data.map(convertToListItem)
    } else {
      ElMessage.error(response.message || t('settings.autoMessage.messages.loadAutoMessagesFailed'))
    }
  } catch (error) {
    console.error('加载自动化消息列表失败:', error)
    ElMessage.error(t('settings.autoMessage.messages.loadAutoMessagesFailed'))
  } finally {
    loading.value = false
  }
}

const resetForm = () => {
  form.id = 0
  form.title = ''
  form.message = ''
  form.selectedChannels = []
  form.resendOnExpire = false
  form.roomSelectionType = 'ALL_LOCAL'
  form.selectedRooms = []
  form.action = ''
  form.sendTiming = ''
  form.day = ''
  form.time = ''
  form.enabled = true
  formRef.value?.clearValidate()
}

const handleCreate = () => {
  resetForm()
  editingId.value = null
  dialogVisible.value = true
}

/** 从 DTO 填充表单 */
const fillFormFromDTO = async (id: number) => {
  try {
    isPopulatingForm = true
    const response = await getAllAutoMessages()
    if (response.success && response.data) {
      const dto = response.data.find((m) => m.id === id)
      if (dto) {
        form.id = dto.id
        form.title = dto.title
        form.message = dto.message
        form.enabled = dto.enabled
        form.resendOnExpire = dto.resendOnExpire || false

        // 解析渠道
        if (dto.channels) {
          try {
            form.selectedChannels = JSON.parse(dto.channels) as number[]
          } catch {
            form.selectedChannels = []
          }
        } else {
          form.selectedChannels = []
        }

        // 解析房间选择
        form.roomSelectionType = dto.roomSelectionType || 'ALL_LOCAL'
        if (dto.roomSelection) {
          try {
            form.selectedRooms = JSON.parse(dto.roomSelection) as number[]
          } catch {
            form.selectedRooms = []
          }
        } else {
          form.selectedRooms = []
        }

        // 操作类型
        form.action = dto.action || ''
        // 发送时机（入住/离店：拆分 DAY_{day}_{time}）
        if (dto.action === 'CHECK_IN' || dto.action === 'CHECK_OUT') {
          const parsed = parseDayAndTime(dto.sendTiming || '')
          if (parsed) {
            form.day = parsed.day
            form.time = parsed.time
          } else {
            form.day = ''
            form.time = ''
          }
          form.sendTiming = ''
        } else {
          form.sendTiming = dto.sendTiming || ''
          form.day = ''
          form.time = ''
        }
      }
    }
  } catch (error) {
    console.error('获取自动化消息详情失败:', error)
  } finally {
    await nextTick()
    isPopulatingForm = false
  }
}

const handleEdit = async (row: AutoMessageListItem) => {
  resetForm()
  await fillFormFromDTO(row.id)
  editingId.value = row.id
  dialogVisible.value = true
}

const handleCopy = async (row: AutoMessageListItem) => {
  resetForm()
  await fillFormFromDTO(row.id)
  form.id = 0
  form.title = form.title + t('settings.autoMessage.display.duplicateSuffix')
  editingId.value = null
  dialogVisible.value = true
}

const handleDelete = async (row: AutoMessageListItem) => {
  try {
    await ElMessageBox.confirm(
      t('settings.autoMessage.messages.deleteConfirm', { title: row.title }),
      t('settings.common.deleteConfirmTitle'),
      {
        confirmButtonText: t('settings.common.confirmButton'),
        cancelButtonText: t('settings.common.cancelButton'),
        type: 'warning',
      },
    )

    loading.value = true
    const response = await deleteAutoMessage(row.id)
    if (response.success) {
      ElMessage.success(t('settings.common.deleteSuccess'))
      await loadAutoMessages()
    } else {
      ElMessage.error(response.message || t('settings.common.deleteFailed'))
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
      ElMessage.error(t('settings.common.deleteFailed'))
    }
  } finally {
    loading.value = false
  }
}

const handleToggle = async (row: AutoMessageListItem) => {
  try {
    loading.value = true
    const response = await toggleAutoMessage(row.id)
    if (response.success) {
      ElMessage.success(
        response.data.enabled
          ? t('settings.autoMessage.messages.enabled')
          : t('settings.autoMessage.messages.disabled'),
      )
      await loadAutoMessages()
    } else {
      ElMessage.error(response.message || t('settings.autoMessage.messages.toggleFailed'))
      // 恢复原状态
      row.enabled = !row.enabled
    }
  } catch (error) {
    console.error('切换状态失败:', error)
    ElMessage.error(t('settings.autoMessage.messages.toggleFailed'))
    // 恢复原状态
    row.enabled = !row.enabled
  } finally {
    loading.value = false
  }
}

const handleSave = async () => {
  try {
    if (!formRef.value) {
      return
    }

    // 入住/离店 与 预订确认 的校验字段不同：
    // - 入住/离店：day + time（sendTiming 不参与校验）
    // - 预订确认：sendTiming
    const fieldsToValidate = ['title', 'message', 'selectedChannels', 'roomSelectionType', 'action']
    if (form.action === 'CHECK_IN' || form.action === 'CHECK_OUT') {
      fieldsToValidate.push('day', 'time')
    } else {
      fieldsToValidate.push('sendTiming')
    }

    let valid = true
    try {
      await formRef.value.validateField(fieldsToValidate)
    } catch {
      valid = false
    }

    if (valid && (form.action === 'CHECK_IN' || form.action === 'CHECK_OUT')) {
      if (!form.day) {
        ElMessage.error(t('settings.autoMessage.validation.dayRequired'))
        valid = false
      } else {
        form.time = normalizeTimeValue(form.time)
      }

      if (!form.time || !/^([01]\d|2[0-3]):[0-5]\d$/.test(form.time)) {
        ElMessage.error(t('settings.autoMessage.validation.invalidTime'))
        valid = false
      }
    }

    if (valid) {
      loading.value = true

      // 根据操作类型决定 sendTiming 的值
      let sendTimingValue: SendTiming
      if (form.action === 'CHECK_IN' || form.action === 'CHECK_OUT') {
        sendTimingValue = `DAY_${form.day}_${form.time}` as SendTiming
      } else {
        sendTimingValue = form.sendTiming as SendTiming
      }

      // 构建请求数据
      const data = {
        title: form.title,
        message: form.message,
        // 新字段
        channels: JSON.stringify(form.selectedChannels),
        resendOnExpire: form.resendOnExpire,
        roomSelectionType: form.roomSelectionType,
        roomSelection: form.roomSelectionType === 'ALL_LOCAL' ? '' : JSON.stringify(form.selectedRooms),
        action: form.action as AutoMessageAction,
        sendTiming: sendTimingValue,
        enabled: form.enabled,
        // 兼容旧字段（用于显示）
        automationRule: form.action ? actionLabelMap[form.action] : '',
        channel: form.selectedChannels
          .map((id) => channels.value.find((c) => c.id === id)?.name)
          .filter(Boolean)
          .join(', '),
        room: form.roomSelectionType === 'ALL_LOCAL' ? t('settings.autoMessage.roomSelection.allLocal') : '',
      }

      let response
      if (editingId.value) {
        response = await updateAutoMessage(editingId.value, data)
      } else {
        response = await createAutoMessage(data)
      }

      if (response.success) {
        ElMessage.success(
          editingId.value
            ? t('settings.autoMessage.messages.updateSuccess')
            : t('settings.autoMessage.messages.createSuccess'),
        )
        dialogVisible.value = false
        await loadAutoMessages()
      } else {
        ElMessage.error(response.message || t('settings.autoMessage.messages.saveFailed'))
      }
    }
  } catch (error) {
    console.error('保存失败:', error)
    ElMessage.error(t('settings.autoMessage.messages.saveFailed'))
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadChannels()
  loadRooms()
  loadRoomTypes()
  loadRoomGroups()
  loadAutoMessages()
})
</script>

<style scoped>
.auto-message-container {
  padding: 24px;
  background: #fff;
  min-height: calc(100vh - 100px);
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-title {
  font-size: 20px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

:deep(.el-table) {
  font-size: 14px;
}

:deep(.el-table th) {
  background-color: #f5f7fa;
  color: #606266;
  font-weight: 600;
}

.message-cell {
  display: inline-block;
  width: 100%;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.automation-rule-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
  line-height: 1.4;
}

.automation-rule-action {
  color: #303133;
  font-weight: 500;
}

.automation-rule-timing {
  color: #606266;
  font-size: 12px;
}

/* 插入变量区域样式 */
.variable-section {
  margin-bottom: 22px;
  padding: 16px;
  background: #f9fafb;
  border-radius: 8px;
}

.variable-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
}

.variable-desc {
  font-size: 12px;
  color: #909399;
  margin-bottom: 12px;
}

.variable-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.variable-tag {
  cursor: pointer;
  border: 1px solid #dcdfe6;
  background: #fff;
  transition: all 0.2s;
}

.variable-tag:hover {
  border-color: #409eff;
  color: #409eff;
}

/* 自动化规则区域样式 */
.automation-section {
  margin-bottom: 16px;
  padding-bottom: 8px;
  border-bottom: 1px solid #ebeef5;
}

.automation-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
}

.automation-desc {
  font-size: 12px;
  color: #909399;
}

/* 房间选择样式 */
.room-selection {
  width: 100%;
}

.room-radio-group {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
}

/* 密码变量警告 */
.password-warning {
  margin-top: 16px;
}

/* 对话框表单样式优化 */
:deep(.el-form-item__label) {
  font-weight: 500;
  color: #303133;
}

:deep(.el-dialog__body) {
  max-height: 70vh;
  overflow-y: auto;
}
</style>
