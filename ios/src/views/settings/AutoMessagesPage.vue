<template>
  <ion-page>
    <ion-header translucent>
      <ion-toolbar class="app-page-header__toolbar">
        <ion-buttons slot="start">
          <ion-back-button class="app-page-header__back-btn" :default-href="ROUTE_PATHS.settings" />
        </ion-buttons>
        <ion-title class="app-page-header__title">自动消息</ion-title>
        <ion-buttons slot="end">
          <ion-button class="app-page-header__text-btn" fill="clear" @click="handleCreateMessage">新增</ion-button>
        </ion-buttons>
      </ion-toolbar>
    </ion-header>

    <ion-content fullscreen class="mobile-page settings-auto-messages-page">
      <ion-refresher slot="fixed" @ionRefresh="handleRefresh">
        <ion-refresher-content pulling-text="下拉刷新自动消息" refreshing-spinner="crescent" />
      </ion-refresher>

      <section class="mobile-hero settings-auto-messages-hero">
        <p class="mobile-note settings-auto-messages-hero__eyebrow">沟通与自动化</p>
        <h1 class="mobile-title">自动消息</h1>
        <div class="mobile-chip-row">
          <span class="mobile-chip">消息 {{ messages.length }}</span>
          <span class="mobile-chip">渠道 {{ channels.length }}</span>
          <span class="mobile-chip">房型 {{ roomTypes.length }}</span>
          <span class="mobile-chip">分组 {{ roomGroups.length }}</span>
        </div>
      </section>

      <div class="mobile-stack">
        <section class="mobile-card">
          <div class="mobile-inline-row settings-auto-messages-page__section-header">
            <div>
              <h2 class="mobile-section-title">消息列表</h2>
            </div>
            <ion-spinner v-if="loading" name="crescent" />
          </div>

          <div v-if="messages.length > 0" class="mobile-list settings-minimal-list settings-auto-messages-list">
            <article v-for="message in messages" :key="message.id" class="settings-minimal-card settings-auto-message-card">
              <div class="settings-minimal-card__header">
                <div class="settings-minimal-card__title-group">
                  <strong>{{ message.title }}</strong>
                  <p class="settings-minimal-card__summary settings-minimal-card__summary--clamp-two">
                    {{ message.message }}
                  </p>
                </div>
                <span
                  class="settings-minimal-card__badge"
                  :class="message.enabled ? 'settings-minimal-card__badge--success' : 'settings-minimal-card__badge--warning'"
                >
                  {{ message.enabled ? '已启用' : '已停用' }}
                </span>
              </div>

              <div class="settings-minimal-card__meta">
                <span class="settings-minimal-card__meta-pill">{{ formatAutomationSummary(message) }}</span>
                <span v-if="formatChannelTag(message.channels)" class="settings-minimal-card__meta-pill">
                  {{ formatChannelTag(message.channels) }}
                </span>
                <span class="settings-minimal-card__meta-pill">
                  {{ formatRoomSummary(message.roomSelectionType, message.roomSelection) }}
                </span>
              </div>

              <div class="settings-minimal-card__actions settings-minimal-card__actions--dense">
                <ion-button size="small" fill="outline" @click="handleEditMessage(message)">编辑</ion-button>
                <ion-button size="small" fill="outline" @click="handleCopyMessage(message)">复制</ion-button>
                <ion-button size="small" fill="outline" @click="handleToggleMessage(message)">
                  {{ message.enabled ? '停用' : '启用' }}
                </ion-button>
                <ion-button size="small" color="danger" fill="clear" @click="handleDeleteMessage(message)">
                  删除
                </ion-button>
              </div>
            </article>
          </div>

          <p v-else-if="!loading" class="mobile-note settings-auto-messages-page__empty-state">当前暂无自动消息。</p>
        </section>

      </div>

      <ion-modal :is-open="editorOpen" @didDismiss="handleDismissEditor">
        <ion-header>
          <ion-toolbar>
            <ion-title>{{ editingMessageId ? '编辑自动消息' : '新增自动消息' }}</ion-title>
            <ion-buttons slot="end">
              <ion-button @click="handleDismissEditor">关闭</ion-button>
            </ion-buttons>
          </ion-toolbar>
        </ion-header>

        <ion-content class="mobile-page settings-modal-page">
          <section class="mobile-card">
            <div class="settings-form-grid">
              <label class="settings-form-field">
                <span>标题</span>
                <ion-input v-model="messageForm.title" fill="outline" placeholder="请输入标题" />
              </label>

              <label class="settings-form-field settings-form-field--full">
                <span>消息内容</span>
                <ion-textarea v-model="messageForm.message" :rows="7" fill="outline" placeholder="请输入自动消息内容" />
              </label>

              <div class="settings-variable-panel">
                <h3>插入变量</h3>
                <div class="settings-variable-panel__list">
                  <button
                    v-for="variable in messageVariables"
                    :key="variable.code"
                    type="button"
                    class="settings-variable-chip"
                    @click="handleInsertMessageVariable(variable.code)"
                  >
                    {{ variable.label }}
                  </button>
                </div>
              </div>

              <label class="settings-form-field settings-form-field--full">
                <span>渠道</span>
                <ion-select v-model="messageForm.selectedChannels" fill="outline" interface="modal" multiple>
                  <ion-select-option v-for="channel in channels" :key="channel.id" :value="channel.id">
                    {{ channel.name }}
                  </ion-select-option>
                </ion-select>
              </label>

              <div class="settings-toggle-field">
                <div>
                  <strong>过时补发</strong>
                </div>
                <ion-toggle v-model="messageForm.resendOnExpire" />
              </div>

              <label class="settings-form-field">
                <span>房间范围</span>
                <ion-select v-model="messageForm.roomSelectionType" fill="outline" interface="action-sheet" @ionChange="handleRoomSelectionTypeChange">
                  <ion-select-option value="ALL_LOCAL">全部房型</ion-select-option>
                  <ion-select-option value="BY_ROOM_TYPE">按房型</ion-select-option>
                  <ion-select-option value="BY_GROUP">按分组</ion-select-option>
                  <ion-select-option value="BY_ROOM">按房间</ion-select-option>
                </ion-select>
              </label>

              <label v-if="messageForm.roomSelectionType === 'BY_ROOM_TYPE'" class="settings-form-field settings-form-field--full">
                <span>选择房型</span>
                <ion-select v-model="messageForm.selectedRoomTypeIds" fill="outline" interface="modal" multiple>
                  <ion-select-option v-for="roomType in roomTypes" :key="roomType.id" :value="roomType.id">
                    {{ roomType.name }}
                  </ion-select-option>
                </ion-select>
              </label>

              <label v-if="messageForm.roomSelectionType === 'BY_GROUP'" class="settings-form-field settings-form-field--full">
                <span>选择分组</span>
                <ion-select v-model="messageForm.selectedRoomTypeIds" fill="outline" interface="modal" multiple>
                  <ion-select-option v-for="group in roomGroups" :key="group.id" :value="group.id">
                    {{ group.name }}
                  </ion-select-option>
                </ion-select>
              </label>

              <label v-if="messageForm.roomSelectionType === 'BY_ROOM'" class="settings-form-field settings-form-field--full">
                <span>选择房间</span>
                <ion-select v-model="messageForm.selectedRoomTypeIds" fill="outline" interface="modal" multiple>
                  <ion-select-option v-for="room in rooms" :key="room.id" :value="room.id">
                    {{ room.roomNumber }} · {{ room.roomType.name }}
                  </ion-select-option>
                </ion-select>
              </label>

              <label class="settings-form-field">
                <span>触发动作</span>
                <ion-select v-model="messageForm.action" fill="outline" interface="action-sheet" @ionChange="handleActionChange">
                  <ion-select-option value="BOOKING_CONFIRM">预订确认</ion-select-option>
                  <ion-select-option value="CHECK_IN">入住</ion-select-option>
                  <ion-select-option value="CHECK_OUT">离店</ion-select-option>
                </ion-select>
              </label>

              <template v-if="messageForm.action === 'CHECK_IN' || messageForm.action === 'CHECK_OUT'">
                <label class="settings-form-field">
                  <span>天数偏移</span>
                  <ion-input v-model="messageForm.day" fill="outline" inputmode="numeric" placeholder="0 / -1 / 1" />
                </label>

                <label class="settings-form-field">
                  <span>发送时间</span>
                  <ion-input v-model="messageForm.time" fill="outline" placeholder="14:00" />
                </label>
              </template>

              <label v-else class="settings-form-field">
                <span>发送时机</span>
                <ion-select v-model="messageForm.sendTiming" fill="outline" interface="modal">
                  <ion-select-option v-for="option in sendTimingOptions" :key="option.value" :value="option.value">
                    {{ option.label }}
                  </ion-select-option>
                </ion-select>
              </label>

              <div class="settings-toggle-field">
                <div>
                  <strong>启用状态</strong>
                </div>
                <ion-toggle v-model="messageForm.enabled" />
              </div>
            </div>

            <div class="settings-form-actions">
              <ion-button fill="outline" @click="handleDismissEditor">取消</ion-button>
              <ion-button :disabled="submitting" @click="handleSaveMessage">
                {{ submitting ? '提交中...' : '保存自动消息' }}
              </ion-button>
            </div>
          </section>
        </ion-content>
      </ion-modal>
    </ion-content>
  </ion-page>
</template>

<script setup lang="ts">
import {
  alertController,
  IonBackButton,
  IonButton,
  IonButtons,
  IonContent,
  IonHeader,
  IonInput,
  IonModal,
  IonPage,
  IonRefresher,
  IonRefresherContent,
  IonSelect,
  IonSelectOption,
  IonSpinner,
  IonTextarea,
  IonTitle,
  IonToggle,
  IonToolbar,
  onIonViewWillEnter,
} from '@ionic/vue'
import { ref } from 'vue'
import {
  createAutoMessage,
  deleteAutoMessage,
  getAllAutoMessages,
  toggleAutoMessage,
  updateAutoMessage,
  type AutoMessageAction,
  type AutoMessageDTO,
  type RoomSelectionType,
  type SendTiming,
} from '@/api/autoMessage'
import { getAllChannels, type ChannelDTO } from '@/api/channel'
import { getAllRoomGroups } from '@/api/roomGroup'
import { getRooms } from '@/api/rooms'
import { getAllRoomTypes, type RoomTypeDTO } from '@/api/roomType'
import { ROUTE_PATHS } from '@/router/guards'
import type { RoomDTO, RoomGroupDTO } from '@/types/settings'
import { showSuccessToast, showWarningToast } from '@/utils/notify'
import { isHandledRequestError } from '@/utils/request'

interface AutoMessageFormState {
  title: string
  message: string
  selectedChannels: number[]
  resendOnExpire: boolean
  roomSelectionType: RoomSelectionType
  selectedRoomTypeIds: number[]
  action: AutoMessageAction | ''
  sendTiming: SendTiming | ''
  day: string
  time: string
  enabled: boolean
}

const sendTimingOptions: Array<{ label: string; value: SendTiming }> = [
  { label: '立即发送', value: 'IMMEDIATELY' },
  { label: '5 分钟后', value: '5_MIN' },
  { label: '10 分钟后', value: '10_MIN' },
  { label: '15 分钟后', value: '15_MIN' },
  { label: '30 分钟后', value: '30_MIN' },
  { label: '1 小时后', value: '1_HOUR' },
  { label: '2 小时后', value: '2_HOUR' },
  { label: '4 小时后', value: '4_HOUR' },
  { label: '8 小时后', value: '8_HOUR' },
  { label: '16 小时后', value: '16_HOUR' },
  { label: '24 小时后', value: '24_HOUR' },
]

const messageVariables = [
  { label: 'Property name', code: '{{property_name}}' },
  { label: "Guest's name", code: '{{guest_name}}' },
  { label: 'Check-in date', code: '{{checkin_date}}' },
  { label: 'Checkout date', code: '{{checkout_date}}' },
  { label: 'Room number', code: '{{room_number}}' },
  { label: 'Check-in code', code: '{{checkin_code}}' },
]

const loading = ref(false)
const submitting = ref(false)
const editorOpen = ref(false)
const editingMessageId = ref<number | null>(null)
const messages = ref<AutoMessageDTO[]>([])
const channels = ref<ChannelDTO[]>([])
const roomTypes = ref<RoomTypeDTO[]>([])
const roomGroups = ref<RoomGroupDTO[]>([])
const rooms = ref<RoomDTO[]>([])
const messageForm = ref<AutoMessageFormState>(createEmptyMessageForm())

function createEmptyMessageForm(): AutoMessageFormState {
  return {
    title: '',
    message: '',
    selectedChannels: [],
    resendOnExpire: false,
    roomSelectionType: 'ALL_LOCAL',
    selectedRoomTypeIds: [],
    action: 'BOOKING_CONFIRM',
    sendTiming: 'IMMEDIATELY',
    day: '0',
    time: '14:00',
    enabled: true,
  }
}

function resolveWarningMessage(error: unknown, fallbackMessage: string) {
  if (error instanceof Error && error.message) {
    return error.message
  }
  return fallbackMessage
}

function parseNumberList(rawValue: string) {
  if (!rawValue) {
    return []
  }

  try {
    const parsed = JSON.parse(rawValue) as unknown[]
    const numbers: number[] = []
    for (const item of parsed) {
      const value = Number(item)
      if (Number.isFinite(value)) {
        numbers.push(value)
      }
    }
    return numbers
  } catch {
    return []
  }
}

function normalizeTimeValue(value: string) {
  const match = value.trim().match(/^([01]\d|2[0-3]):([0-5]\d)(?::[0-5]\d)?$/)
  if (!match) {
    return ''
  }
  return `${match[1]}:${match[2]}`
}

function parseDayTiming(value: string) {
  const match = decodeURIComponent(value).match(/^DAY_(-?\d+)_([01]\d|2[0-3]):([0-5]\d)(?::[0-5]\d)?$/)
  if (!match) {
    return null
  }
  return {
    day: match[1],
    time: `${match[2]}:${match[3]}`,
  }
}

function formatActionLabel(action: AutoMessageAction) {
  if (action === 'CHECK_IN') {
    return '入住'
  }
  if (action === 'CHECK_OUT') {
    return '离店'
  }
  return '预订确认'
}

function formatTimingLabel(value: string) {
  const option = sendTimingOptions.find((item) => item.value === value)
  if (option) {
    return option.label
  }

  const parsed = parseDayTiming(value)
  if (parsed) {
    return `${parsed.day} 天偏移 · ${parsed.time}`
  }

  return value || '未设置'
}

function formatAutomationSummary(message: AutoMessageDTO) {
  return `${formatActionLabel(message.action)} · ${formatTimingLabel(message.sendTiming)}`
}

function formatChannelSummary(rawValue: string) {
  const channelIds = parseNumberList(rawValue)
  if (channelIds.length === 0) {
    return '全部渠道'
  }

  const names: string[] = []
  for (const channelId of channelIds) {
    const matched = channels.value.find((item) => item.id === channelId)
    if (matched) {
      names.push(matched.name)
    }
  }

  if (names.length === 0) {
    return '全部渠道'
  }
  return names.join('、')
}

function formatChannelTag(rawValue: string) {
  const summary = formatChannelSummary(rawValue)
  if (summary === '全部渠道') {
    return ''
  }
  return `渠道 ${summary}`
}

function formatRoomSummary(roomSelectionType: RoomSelectionType, rawValue: string) {
  if (roomSelectionType === 'ALL_LOCAL') {
    return '全部房型'
  }

  if (roomSelectionType === 'BY_ROOM_TYPE') {
    const roomTypeIds = parseNumberList(rawValue)
    const names: string[] = []

    for (const roomTypeId of roomTypeIds) {
      const matched = roomTypes.value.find((item) => item.id === roomTypeId)
      if (matched) {
        names.push(matched.name)
      }
    }

    if (names.length === 0) {
      return '按房型'
    }

    return names.join('、')
  }

  if (roomSelectionType === 'BY_GROUP') {
    const groupIds = parseNumberList(rawValue)
    const names: string[] = []

    for (const groupId of groupIds) {
      const matched = roomGroups.value.find((item: RoomGroupDTO) => Number(item.id) === groupId)
      if (matched) {
        names.push(matched.name)
      }
    }

    return names.length > 0 ? names.join('、') : '按分组'
  }

  if (roomSelectionType === 'BY_ROOM') {
    const roomIds = parseNumberList(rawValue)
    const names: string[] = []

    for (const roomId of roomIds) {
      const matched = rooms.value.find((item) => item.id === roomId)
      if (matched) {
        names.push(matched.roomNumber)
      }
    }

    return names.length > 0 ? names.join('、') : '按房间'
  }

  return '未设置'
}

function handleRoomSelectionTypeChange() {
  messageForm.value.selectedRoomTypeIds = []
}

function handleActionChange() {
  if (messageForm.value.action === 'CHECK_IN' || messageForm.value.action === 'CHECK_OUT') {
    messageForm.value.sendTiming = ''
    return
  }

  messageForm.value.day = '0'
  messageForm.value.time = '14:00'
  messageForm.value.sendTiming = 'IMMEDIATELY'
}

function handleInsertMessageVariable(code: string) {
  if (!messageForm.value.message.trim()) {
    messageForm.value.message = code
    return
  }

  messageForm.value.message = `${messageForm.value.message} ${code}`
}

async function confirmDelete(title: string) {
  const alert = await alertController.create({
    header: '删除自动消息',
    message: `确认删除 ${title} 吗？`,
    buttons: [
      {
        text: '取消',
        role: 'cancel',
      },
      {
        text: '确认删除',
        role: 'destructive',
      },
    ],
  })

  await alert.present()
  const result = await alert.onDidDismiss()
  return result.role === 'destructive'
}

async function loadPageData() {
  loading.value = true
  try {
    const [messageResponse, channelResponse, roomTypeResponse, roomGroupResponse, roomResponse] = await Promise.all([
      getAllAutoMessages(),
      getAllChannels(),
      getAllRoomTypes(),
      getAllRoomGroups(),
      getRooms(),
    ])

    if (!messageResponse.success || !messageResponse.data) {
      throw new Error(messageResponse.message || '加载自动消息失败')
    }
    if (!channelResponse.success || !channelResponse.data) {
      throw new Error(channelResponse.message || '加载渠道失败')
    }
    if (!roomTypeResponse.success || !roomTypeResponse.data) {
      throw new Error(roomTypeResponse.message || '加载房型失败')
    }
    if (!roomGroupResponse.success || !roomGroupResponse.data) {
      throw new Error(roomGroupResponse.message || '加载分组失败')
    }
    if (!roomResponse.success || !roomResponse.data) {
      throw new Error(roomResponse.message || '加载房间失败')
    }

    messages.value = messageResponse.data
    channels.value = channelResponse.data
    roomTypes.value = roomTypeResponse.data
    roomGroups.value = roomGroupResponse.data
    rooms.value = roomResponse.data
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, '加载自动消息失败'))
    }
  } finally {
    loading.value = false
  }
}

function handleCreateMessage() {
  editingMessageId.value = null
  messageForm.value = createEmptyMessageForm()
  editorOpen.value = true
}

function fillMessageForm(message: AutoMessageDTO, copyMode: boolean) {
  const parsedTiming = parseDayTiming(message.sendTiming || '')
  const nextAction = message.action || 'BOOKING_CONFIRM'
  const nextTitle = copyMode ? `${message.title} (副本)` : message.title

  messageForm.value = {
    title: nextTitle,
    message: message.message,
    selectedChannels: parseNumberList(message.channels),
    resendOnExpire: message.resendOnExpire,
    roomSelectionType: message.roomSelectionType || 'ALL_LOCAL',
    selectedRoomTypeIds: parseNumberList(message.roomSelection),
    action: nextAction,
    sendTiming: nextAction === 'BOOKING_CONFIRM' ? message.sendTiming : '',
    day: parsedTiming?.day || '0',
    time: parsedTiming?.time || '14:00',
    enabled: message.enabled,
  }
}

function handleEditMessage(message: AutoMessageDTO) {
  editingMessageId.value = message.id
  fillMessageForm(message, false)
  editorOpen.value = true
}

function handleCopyMessage(message: AutoMessageDTO) {
  editingMessageId.value = null
  fillMessageForm(message, true)
  editorOpen.value = true
}

function handleDismissEditor() {
  editorOpen.value = false
  editingMessageId.value = null
  messageForm.value = createEmptyMessageForm()
}

async function handleSaveMessage() {
  if (!messageForm.value.title.trim()) {
    showWarningToast('请输入标题')
    return
  }
  if (!messageForm.value.message.trim()) {
    showWarningToast('请输入消息内容')
    return
  }
  if (messageForm.value.selectedChannels.length === 0) {
    showWarningToast('请至少选择一个渠道')
    return
  }
  if (messageForm.value.roomSelectionType !== 'ALL_LOCAL' && messageForm.value.selectedRoomTypeIds.length === 0) {
    showWarningToast('请选择至少一个范围项')
    return
  }

  let sendTiming: SendTiming
  if (messageForm.value.action === 'CHECK_IN' || messageForm.value.action === 'CHECK_OUT') {
    if (!/^-?\d+$/.test(messageForm.value.day.trim())) {
      showWarningToast('请输入有效的天数偏移')
      return
    }

    const normalizedTime = normalizeTimeValue(messageForm.value.time)
    if (!normalizedTime) {
      showWarningToast('请输入有效的发送时间，例如 14:00')
      return
    }

    sendTiming = `DAY_${messageForm.value.day.trim()}_${normalizedTime}` as SendTiming
  } else {
    if (!messageForm.value.sendTiming) {
      showWarningToast('请选择发送时机')
      return
    }

    sendTiming = messageForm.value.sendTiming
  }

  submitting.value = true
  try {
    const channelNames: string[] = []
    for (const channelId of messageForm.value.selectedChannels) {
      const matched = channels.value.find((item) => item.id === channelId)
      if (matched) {
        channelNames.push(matched.name)
      }
    }

    const payload = {
      title: messageForm.value.title.trim(),
      message: messageForm.value.message.trim(),
      channels: JSON.stringify(messageForm.value.selectedChannels),
      resendOnExpire: messageForm.value.resendOnExpire,
      roomSelectionType: messageForm.value.roomSelectionType,
      roomSelection:
        messageForm.value.roomSelectionType === 'ALL_LOCAL'
          ? ''
          : JSON.stringify(messageForm.value.selectedRoomTypeIds),
      action: messageForm.value.action || 'BOOKING_CONFIRM',
      sendTiming,
      enabled: messageForm.value.enabled,
      automationRule: formatActionLabel((messageForm.value.action || 'BOOKING_CONFIRM') as AutoMessageAction),
      channel: channelNames.join('、'),
      room:
        messageForm.value.roomSelectionType === 'ALL_LOCAL'
          ? '全部房型'
          : formatRoomSummary(messageForm.value.roomSelectionType, JSON.stringify(messageForm.value.selectedRoomTypeIds)),
    }

    if (editingMessageId.value) {
      const response = await updateAutoMessage(editingMessageId.value, payload)
      if (!response.success) {
        throw new Error(response.message || '更新自动消息失败')
      }
      showSuccessToast('自动消息已更新')
    } else {
      const response = await createAutoMessage(payload)
      if (!response.success) {
        throw new Error(response.message || '创建自动消息失败')
      }
      showSuccessToast('自动消息已创建')
    }

    handleDismissEditor()
    await loadPageData()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, '保存自动消息失败'))
    }
  } finally {
    submitting.value = false
  }
}

async function handleToggleMessage(message: AutoMessageDTO) {
  try {
    const response = await toggleAutoMessage(message.id)
    if (!response.success) {
      throw new Error(response.message || '更新自动消息状态失败')
    }
    showSuccessToast(response.data.enabled ? '自动消息已启用' : '自动消息已停用')
    await loadPageData()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, '更新自动消息状态失败'))
    }
  }
}

async function handleDeleteMessage(message: AutoMessageDTO) {
  const confirmed = await confirmDelete(message.title)
  if (!confirmed) {
    return
  }

  try {
    const response = await deleteAutoMessage(message.id)
    if (!response.success) {
      throw new Error(response.message || '删除自动消息失败')
    }
    showSuccessToast('自动消息已删除')
    await loadPageData()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, '删除自动消息失败'))
    }
  }
}

async function handleRefresh(event: CustomEvent) {
  await loadPageData()
  event.detail.complete()
}

onIonViewWillEnter(async () => {
  await loadPageData()
})
</script>

<style scoped>
.settings-auto-messages-page {
  display: block;
}

.settings-auto-messages-hero {
  margin-top: 4px;
}

.settings-auto-messages-hero__eyebrow {
  color: var(--ion-color-primary);
  font-weight: 700;
}

.settings-auto-messages-page > .mobile-stack {
  margin-top: var(--ios-pms-space-4);
}

.settings-auto-messages-page__section-header {
  align-items: flex-start;
}

.settings-auto-messages-page__empty-state {
  padding-top: 16px;
}

.settings-modal-page {
  --padding-top: 16px;
  --padding-bottom: 24px;
  --padding-start: 16px;
  --padding-end: 16px;
}

.settings-form-grid {
  display: grid;
  gap: 14px;
}

.settings-form-field {
  display: grid;
  gap: 8px;
}

.settings-form-field span {
  color: var(--app-heading);
  font-size: 13px;
  font-weight: 600;
}

.settings-form-field--full {
  grid-column: 1 / -1;
}

.settings-toggle-field {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 14px;
  border-radius: 18px;
  background: var(--app-primary-soft);
}

.settings-toggle-field strong,
.settings-toggle-field p {
  margin: 0;
}

.settings-toggle-field p {
  margin-top: 6px;
  color: var(--app-muted);
  font-size: 12px;
}

.settings-variable-panel {
  padding: 14px;
  border-radius: 18px;
  background: var(--app-primary-soft);
}

.settings-variable-panel h3,
.settings-variable-panel p {
  margin: 0;
}

.settings-variable-panel h3 {
  color: var(--app-heading);
  font-size: 15px;
}

.settings-variable-panel p {
  margin-top: 8px;
}

.settings-variable-panel__list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.settings-variable-chip {
  padding: 8px 12px;
  border: 1px solid var(--app-primary-soft-strong);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.92);
  color: var(--ion-color-primary);
  font-size: 12px;
  font-weight: 600;
}

.settings-form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  flex-wrap: wrap;
  margin-top: 18px;
}
</style>
