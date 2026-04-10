<template>
  <div class="auto-message-container">
    <div class="page-header">
      <h2 class="page-title">自动化消息</h2>
      <el-button type="primary" @click="handleCreate">创建</el-button>
    </div>

    <el-table :data="messages" border stripe v-loading="loading">
      <el-table-column prop="title" label="标题" min-width="120" />
      <el-table-column prop="message" label="消息" min-width="200">
        <template #default="{ row }">
          <span class="message-cell">{{ row.message }}</span>
        </template>
      </el-table-column>
      <el-table-column label="自动化规则" min-width="220">
        <template #default="{ row }">
          <div class="automation-rule-cell">
            <div class="automation-rule-action">操作：{{ row.automationRuleAction }}</div>
            <div class="automation-rule-timing">何时发送：{{ row.automationRuleTiming }}</div>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="channel" label="渠道" min-width="120" />
      <el-table-column prop="room" label="房间" min-width="120" />
      <el-table-column label="允许" width="80" align="center">
        <template #default="{ row }">
          <el-switch v-model="row.enabled" @change="handleToggle(row)" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180" align="center" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
          <el-button link type="primary" @click="handleCopy(row)">复制</el-button>
          <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
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
        <el-form-item label="标题" prop="title" required>
          <el-input v-model="form.title" placeholder="请输入标题" />
        </el-form-item>

        <!-- 消息内容 -->
        <el-form-item label="消息" prop="message" required>
          <el-input
            ref="messageInputRef"
            v-model="form.message"
            type="textarea"
            :rows="5"
            placeholder="请输入消息内容"
          />
        </el-form-item>

        <!-- 插入变量 -->
        <div class="variable-section">
          <div class="variable-title">插入变量</div>
          <div class="variable-desc">选择一个短代码并将其添加到您的消息中。每次您使用该消息时，正确的详细信息都会自动填充。</div>
          <div class="variable-tags">
            <el-tag
              v-for="variable in messageVariables"
              :key="variable.code"
              class="variable-tag"
              effect="plain"
              @click="insertVariable(variable.code)"
            >
              {{ variable.label }}
            </el-tag>
          </div>
        </div>

        <!-- 渠道（多选） -->
        <el-form-item label="渠道" prop="selectedChannels" required>
          <el-select
            v-model="form.selectedChannels"
            multiple
            placeholder="请选择渠道"
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
        <el-form-item label="过时补发">
          <el-switch v-model="form.resendOnExpire" />
        </el-form-item>

        <!-- 房间选择 -->
        <el-form-item label="房间" prop="roomSelectionType" required>
          <div class="room-selection">
            <el-radio-group v-model="form.roomSelectionType" class="room-radio-group">
              <el-radio value="ALL_LOCAL">全部本地房型</el-radio>
              <el-radio value="BY_ROOM_TYPE">根据房型</el-radio>
              <el-radio value="BY_GROUP">根据分组</el-radio>
              <el-radio value="BY_ROOM">按房间</el-radio>
            </el-radio-group>
            <el-select
              v-if="form.roomSelectionType !== 'ALL_LOCAL'"
              v-model="form.selectedRooms"
              multiple
              placeholder="请选择"
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
          <div class="automation-title">自动化规则</div>
          <div class="automation-desc">选择将触发您的消息的操作以及在该操作之前或之后多久发送。</div>
        </div>

        <el-form-item label="操作" prop="action" required>
          <el-select v-model="form.action" placeholder="请选择" style="width: 100%">
            <el-option label="预订确认" value="BOOKING_CONFIRM" />
            <el-option label="入住" value="CHECK_IN" />
            <el-option label="离店" value="CHECK_OUT" />
          </el-select>
        </el-form-item>

        <!-- 入住/离店：按预订入住/离店日期 + 天数偏移 + 时间 -->
        <template v-if="form.action === 'CHECK_IN' || form.action === 'CHECK_OUT'">
          <el-form-item label="天" prop="day" required>
            <el-select v-model="form.day" placeholder="请选择" style="width: 100%">
              <el-option
                v-for="option in dayOptions"
                :key="option.value"
                :label="option.label"
                :value="option.value"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="时间" prop="time" required>
            <el-select v-model="form.time" placeholder="请选择" style="width: 100%">
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
        <el-form-item v-else label="何时发送" prop="sendTiming" required>
          <el-select v-model="form.sendTiming" placeholder="请选择" style="width: 100%">
            <el-option label="立即发送" value="IMMEDIATELY" />
            <el-option label="5分钟后" value="5_MIN" />
            <el-option label="10分钟后" value="10_MIN" />
            <el-option label="15分钟后" value="15_MIN" />
            <el-option label="30分钟后" value="30_MIN" />
            <el-option label="1小时后" value="1_HOUR" />
            <el-option label="2小时后" value="2_HOUR" />
            <el-option label="4小时后" value="4_HOUR" />
            <el-option label="8小时后" value="8_HOUR" />
            <el-option label="16小时后" value="16_HOUR" />
            <el-option label="24小时后" value="24_HOUR" />
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
            请注意：如果你增加了密码变量，请确保密码自动生成时间早于消息发送时间，否则消息将会发送失败。
          </template>
        </el-alert>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSave">保存</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch, nextTick } from 'vue'
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

/** 消息变量定义 */
interface MessageVariable {
  label: string
  code: string
}

/** 消息变量列表 */
const messageVariables: MessageVariable[] = [
  { label: 'Property name', code: '{{property_name}}' },
  { label: "Guest's name", code: '{{guest_name}}' },
  { label: "Guest's phone number", code: '{{guest_phone}}' },
  { label: 'Check-in date', code: '{{checkin_date}}' },
  { label: 'Checkout date', code: '{{checkout_date}}' },
  { label: 'Room type name', code: '{{room_type_name}}' },
  { label: 'Room type address', code: '{{room_type_address}}' },
  { label: 'Nearby station', code: '{{nearby_station}}' },
  { label: 'Rate plan name', code: '{{rate_plan_name}}' },
  { label: 'Property address', code: '{{property_address}}' },
  { label: 'Property city', code: '{{property_city}}' },
  { label: 'Property phone', code: '{{property_phone}}' },
  { label: 'Property Email', code: '{{property_email}}' },
  { label: 'Confirmation code', code: '{{confirmation_code}}' },
  { label: 'Registration link', code: '{{registration_link}}' },
  { label: 'Check-in form link', code: '{{checkin_form_link}}' },
  { label: 'Number of nights', code: '{{number_of_nights}}' },
  { label: 'Check-in code', code: '{{checkin_code}}' },
  { label: 'SmartLock Passcode', code: '{{smartlock_passcode}}' },
  { label: 'Room Number', code: '{{room_number}}' },
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
      label = `${Math.abs(day)}天前`
    } else if (day === 0) {
      label = '当天'
    } else {
      label = `${day}天后`
    }
    options.push({ label, value: String(day) })
  }
  return options
})

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
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  message: [{ required: true, message: '请输入消息内容', trigger: 'blur' }],
  selectedChannels: [{ required: true, message: '请选择渠道', trigger: 'change', type: 'array', min: 1 }],
  roomSelectionType: [{ required: true, message: '请选择房间类型', trigger: 'change' }],
  action: [{ required: true, message: '请选择操作', trigger: 'change' }],
  sendTiming: [{ required: true, message: '请选择发送时机', trigger: 'change' }],
  day: [{ required: true, message: '请选择天', trigger: 'change' }],
  time: [{ required: true, message: '请选择时间', trigger: 'change' }],
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
  return editingId.value ? '编辑自动化消息' : '创建消息模板'
})

// 加载渠道列表
const loadChannels = async () => {
  try {
    const response = await getAllChannels()
    if (response.success && response.data) {
      channels.value = response.data
    } else {
      ElMessage.error(response.message || '加载渠道列表失败')
    }
  } catch (error) {
    console.error('加载渠道列表失败:', error)
    ElMessage.error('加载渠道列表失败')
  }
}

// 加载房间列表
const loadRooms = async () => {
  try {
    const response = await getRooms()
    if (response.success && response.data) {
      rooms.value = response.data
    } else {
      ElMessage.error(response.message || '加载房间列表失败')
    }
  } catch (error) {
    console.error('加载房间列表失败:', error)
    ElMessage.error('加载房间列表失败')
  }
}

// 加载房型列表
const loadRoomTypes = async () => {
  try {
    const response = await getAllRoomTypes()
    if (response.success && response.data) {
      roomTypes.value = response.data
    } else {
      ElMessage.error(response.message || '加载房型列表失败')
    }
  } catch (error) {
    console.error('加载房型列表失败:', error)
    ElMessage.error('加载房型列表失败')
  }
}

// 加载房间分组列表
const loadRoomGroups = async () => {
  try {
    const response = await getAllRoomGroups()
    if (response.success && response.data) {
      roomGroups.value = response.data
    } else {
      ElMessage.error(response.message || '加载房间分组列表失败')
    }
  } catch (error) {
    console.error('加载房间分组列表失败:', error)
    ElMessage.error('加载房间分组列表失败')
  }
}

/** 操作类型显示映射 */
const actionLabelMap: Record<string, string> = {
  BOOKING_CONFIRM: '预订确认',
  CHECK_IN: '入住',
  CHECK_OUT: '离店',
}

const bookingTimingLabelMap: Record<string, string> = {
  IMMEDIATELY: '立即发送',
  '5_MIN': '5分钟后',
  '10_MIN': '10分钟后',
  '15_MIN': '15分钟后',
  '30_MIN': '30分钟后',
  '1_HOUR': '1小时后',
  '2_HOUR': '2小时后',
  '4_HOUR': '4小时后',
  '8_HOUR': '8小时后',
  '16_HOUR': '16小时后',
  '24_HOUR': '24小时后',
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
    const dayText = day < 0 ? `${Math.abs(day)}天前` : day === 0 ? '当天' : `${day}天后`
    const anchor = action === 'CHECK_IN' ? '入住' : '离店'
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
      channelDisplay = channelNames.join(', ') || '全部渠道'
    } catch {
      channelDisplay = dto.channel || '全部渠道'
    }
  }

  // 解析房间
  let roomDisplay = dto.room || ''
  if (dto.roomSelectionType) {
    switch (dto.roomSelectionType) {
      case 'ALL_LOCAL':
        roomDisplay = '全部本地房型'
        break
      case 'BY_ROOM_TYPE':
        if (dto.roomSelection) {
          try {
            const roomTypeIds = JSON.parse(dto.roomSelection) as number[]
            const names = roomTypeIds
              .map((id) => roomTypes.value.find((rt) => rt.id === id)?.name)
              .filter(Boolean)
            roomDisplay = names.join(', ') || '按房型'
          } catch {
            roomDisplay = '按房型'
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
            roomDisplay = names.join(', ') || '按分组'
          } catch {
            roomDisplay = '按分组'
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
            roomDisplay = names.join(', ') || '按房间'
          } catch {
            roomDisplay = '按房间'
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
    ElMessage.warning('请先选择门店')
    messages.value = []
    return
  }

  try {
    loading.value = true
    const response = await getAllAutoMessages()
    if (response.success && response.data) {
      messages.value = response.data.map(convertToListItem)
    } else {
      ElMessage.error(response.message || '加载自动化消息列表失败')
    }
  } catch (error) {
    console.error('加载自动化消息列表失败:', error)
    ElMessage.error('加载自动化消息列表失败')
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
  form.title = form.title + ' (副本)'
  editingId.value = null
  dialogVisible.value = true
}

const handleDelete = async (row: AutoMessageListItem) => {
  try {
    await ElMessageBox.confirm(`确定要删除消息 "${row.title}" 吗?`, '删除确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })

    loading.value = true
    const response = await deleteAutoMessage(row.id)
    if (response.success) {
      ElMessage.success('删除成功')
      await loadAutoMessages()
    } else {
      ElMessage.error(response.message || '删除失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
      ElMessage.error('删除失败')
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
      ElMessage.success(response.data.enabled ? '已启用' : '已禁用')
      await loadAutoMessages()
    } else {
      ElMessage.error(response.message || '切换状态失败')
      // 恢复原状态
      row.enabled = !row.enabled
    }
  } catch (error) {
    console.error('切换状态失败:', error)
    ElMessage.error('切换状态失败')
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
        ElMessage.error('请选择天')
        valid = false
      } else {
        form.time = normalizeTimeValue(form.time)
      }

      if (!form.time || !/^([01]\d|2[0-3]):[0-5]\d$/.test(form.time)) {
        ElMessage.error('请选择时间（例如 14:00）')
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
        room: form.roomSelectionType === 'ALL_LOCAL' ? '全部本地房型' : '',
      }

      let response
      if (editingId.value) {
        response = await updateAutoMessage(editingId.value, data)
      } else {
        response = await createAutoMessage(data)
      }

      if (response.success) {
        ElMessage.success(editingId.value ? '更新成功' : '创建成功')
        dialogVisible.value = false
        await loadAutoMessages()
      } else {
        ElMessage.error(response.message || '保存失败')
      }
    }
  } catch (error) {
    console.error('保存失败:', error)
    ElMessage.error('保存失败')
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
