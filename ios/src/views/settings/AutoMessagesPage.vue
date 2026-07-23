<template>
  <ion-page>
    <ion-header translucent>
      <ion-toolbar class="app-page-header__toolbar">
        <ion-buttons slot="start">
          <ion-back-button class="app-page-header__back-btn" :default-href="ROUTE_PATHS.settings" />
        </ion-buttons>
        <ion-title class="app-page-header__title">{{ $t('routes.SettingsAutoMessages') }}</ion-title>
        <ion-buttons slot="end">
          <ion-button class="app-page-header__text-btn" fill="clear" @click="handleCreateMessage">{{ $t('settingsStage4.roomGroup.addGroup') }}</ion-button>
        </ion-buttons>
      </ion-toolbar>
    </ion-header>

    <ion-content fullscreen class="mobile-page settings-auto-messages-page">
      <ion-refresher slot="fixed" @ionRefresh="handleRefresh">
        <ion-refresher-content :pulling-text="$t('stage5UiAttributes.16')" refreshing-spinner="crescent" />
      </ion-refresher>

      <section class="mobile-hero settings-auto-messages-hero">
        <p class="mobile-note settings-auto-messages-hero__eyebrow">{{ $t('stage5SourceText.159') }}</p>
        <h1 class="mobile-title">{{ $t('routes.SettingsAutoMessages') }}</h1>
        <div class="mobile-chip-row">
          <span class="mobile-chip">{{ $t('home.quick.messages.0') }} {{ messages.length }}</span>
          <span class="mobile-chip">{{ $t('home.quick.channels.0') }} {{ channels.length }}</span>
          <span class="mobile-chip">{{ $t('accommodation.common.roomType') }} {{ roomTypes.length }}</span>
          <span class="mobile-chip">{{ $t('channel.mobile.mapping.groups') }} {{ roomGroups.length }}</span>
        </div>
      </section>

      <div class="mobile-stack">
        <section class="mobile-card">
          <div class="mobile-inline-row settings-auto-messages-page__section-header">
            <div>
              <h2 class="mobile-section-title">{{ $t('stage5SourceText.161') }}</h2>
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
                  {{ message.enabled ? $t('channel.managementData.statusActive') : $t('stage5DynamicUi.28') }}
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
                <ion-button size="small" fill="outline" @click="handleEditMessage(message)">{{ $t('accommodation.roomPrice.editTitle') }}</ion-button>
                <ion-button size="small" fill="outline" @click="handleCopyMessage(message)">{{ $t('home.support.copy') }}</ion-button>
                <ion-button size="small" fill="outline" @click="handleToggleMessage(message)">
                  {{ message.enabled ? $t('roomStatus.store.roomState.outOfOrder') : $t('settingsStage4.accountList.status.enabled') }}
                </ion-button>
                <ion-button size="small" color="danger" fill="clear" @click="handleDeleteMessage(message)">
                  {{ $t('roomStatus.roomLock.actions.delete') }}
                </ion-button>
              </div>
            </article>
          </div>

          <p v-else-if="!loading" class="mobile-note settings-auto-messages-page__empty-state">{{ $t('stage5SourceText.86') }}</p>
        </section>

      </div>

      <ion-modal :is-open="editorOpen" @didDismiss="handleDismissEditor">
        <ion-header>
          <ion-toolbar>
            <ion-title>{{ editingMessageId ? $t('stage5DynamicUi.68') : $t('stage5DynamicUi.41') }}</ion-title>
            <ion-buttons slot="end">
              <ion-button @click="handleDismissEditor">{{ $t('home.section.close') }}</ion-button>
            </ion-buttons>
          </ion-toolbar>
        </ion-header>

        <ion-content class="mobile-page settings-modal-page">
          <section class="mobile-card">
            <div class="settings-form-grid">
              <label class="settings-form-field">
                <span>{{ $t('stage5SourceText.148') }}</span>
                <ion-input v-model="messageForm.title" fill="outline" :placeholder="$t('stage5UiAttributes.85')" />
              </label>

              <label class="settings-form-field settings-form-field--full">
                <span>{{ $t('stage5.dataCenter.detail.messageContent') }}</span>
                <ion-textarea v-model="messageForm.message" :rows="7" fill="outline" :placeholder="$t('stage5UiAttributes.89')" />
              </label>

              <div class="settings-variable-panel">
                <h3>{{ $t('stage5SourceText.122') }}</h3>
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
                <span>{{ $t('home.quick.channels.0') }}</span>
                <ion-select v-model="messageForm.selectedChannels" fill="outline" interface="modal" multiple>
                  <ion-select-option v-for="channel in channels" :key="channel.id" :value="channel.id">
                    {{ channel.name }}
                  </ion-select-option>
                </ion-select>
              </label>

              <div class="settings-toggle-field">
                <div>
                  <strong>{{ $t('stage5SourceText.213') }}</strong>
                </div>
                <ion-toggle v-model="messageForm.resendOnExpire" />
              </div>

              <label class="settings-form-field">
                <span>{{ $t('stage5SourceText.109') }}</span>
                <ion-select v-model="messageForm.roomSelectionType" fill="outline" interface="action-sheet" @ionChange="handleRoomSelectionTypeChange">
                  <ion-select-option value="ALL_LOCAL">{{ $t('accommodation.priceHistory.allRoomTypes') }}</ion-select-option>
                  <ion-select-option value="BY_ROOM_TYPE">{{ $t('stage5SourceText.115') }}</ion-select-option>
                  <ion-select-option value="BY_GROUP">{{ $t('stage5SourceText.114') }}</ion-select-option>
                  <ion-select-option value="BY_ROOM">{{ $t('stage5SourceText.116') }}</ion-select-option>
                </ion-select>
              </label>

              <label v-if="messageForm.roomSelectionType === 'BY_ROOM_TYPE'" class="settings-form-field settings-form-field--full">
                <span>{{ $t('stage5SourceText.222') }}</span>
                <ion-select v-model="messageForm.selectedRoomTypeIds" fill="outline" interface="modal" multiple>
                  <ion-select-option v-for="roomType in roomTypes" :key="roomType.id" :value="roomType.id">
                    {{ roomType.name }}
                  </ion-select-option>
                </ion-select>
              </label>

              <label v-if="messageForm.roomSelectionType === 'BY_GROUP'" class="settings-form-field settings-form-field--full">
                <span>{{ $t('channel.mobile.mapping.filters.selectGroup') }}</span>
                <ion-select v-model="messageForm.selectedRoomTypeIds" fill="outline" interface="modal" multiple>
                  <ion-select-option v-for="group in roomGroups" :key="group.id" :value="group.id">
                    {{ group.name }}
                  </ion-select-option>
                </ion-select>
              </label>

              <label v-if="messageForm.roomSelectionType === 'BY_ROOM'" class="settings-form-field settings-form-field--full">
                <span>{{ $t('stage5SourceText.223') }}</span>
                <ion-select v-model="messageForm.selectedRoomTypeIds" fill="outline" interface="modal" multiple>
                  <ion-select-option v-for="room in rooms" :key="room.id" :value="room.id">
                    {{ room.roomNumber }} · {{ room.roomType.name }}
                  </ion-select-option>
                </ion-select>
              </label>

              <label class="settings-form-field">
                <span>{{ $t('stage5SourceText.196') }}</span>
                <ion-select v-model="messageForm.action" fill="outline" interface="action-sheet" @ionChange="handleActionChange">
                  <ion-select-option value="BOOKING_CONFIRM">{{ $t('stage5SourceText.230') }}</ion-select-option>
                  <ion-select-option value="CHECK_IN">{{ $t('roomStatus.action.checkIn') }}</ion-select-option>
                  <ion-select-option value="CHECK_OUT">{{ $t('roomStatus.hoverCard.checkOutDate') }}</ion-select-option>
                </ion-select>
              </label>

              <template v-if="messageForm.action === 'CHECK_IN' || messageForm.action === 'CHECK_OUT'">
                <label class="settings-form-field">
                  <span>{{ $t('stage5SourceText.41') }}</span>
                  <ion-input v-model="messageForm.day" fill="outline" inputmode="numeric" placeholder="0 / -1 / 1" />
                </label>

                <label class="settings-form-field">
                  <span>{{ $t('stage5SourceText.27') }}</span>
                  <ion-input v-model="messageForm.time" fill="outline" placeholder="14:00" />
                </label>
              </template>

              <label v-else class="settings-form-field">
                <span>{{ $t('stage5SourceText.26') }}</span>
                <ion-select v-model="messageForm.sendTiming" fill="outline" interface="modal">
                  <ion-select-option v-for="option in sendTimingOptions" :key="option.value" :value="option.value">
                    {{ option.label }}
                  </ion-select-option>
                </ion-select>
              </label>

              <div class="settings-toggle-field">
                <div>
                  <strong>{{ $t('stage5SourceText.35') }}</strong>
                </div>
                <ion-toggle v-model="messageForm.enabled" />
              </div>
            </div>

            <div class="settings-form-actions">
              <ion-button fill="outline" @click="handleDismissEditor">{{ $t('accommodation.common.cancel') }}</ion-button>
              <ion-button :disabled="submitting" @click="handleSaveMessage">
                {{ submitting ? $t('iosStage5.cleaning.submitting') : $t('stage5DynamicUi.18') }}
              </ion-button>
            </div>
          </section>
        </ion-content>
      </ion-modal>
    </ion-content>
  </ion-page>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'
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
import { computed, ref } from 'vue'
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

const { t } = useI18n()

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

const sendTimingOptions = computed<Array<{ label: string; value: SendTiming }>>(() => [
  { label: t('settingsResidual.autoMessages.sendImmediately'), value: 'IMMEDIATELY' },
  { label: t('settingsResidual.autoMessages.minutesAfter', { value: 5 }), value: '5_MIN' },
  { label: t('settingsResidual.autoMessages.minutesAfter', { value: 10 }), value: '10_MIN' },
  { label: t('settingsResidual.autoMessages.minutesAfter', { value: 15 }), value: '15_MIN' },
  { label: t('settingsResidual.autoMessages.minutesAfter', { value: 30 }), value: '30_MIN' },
  { label: t('settingsResidual.autoMessages.hoursAfter', { value: 1 }), value: '1_HOUR' },
  { label: t('settingsResidual.autoMessages.hoursAfter', { value: 2 }), value: '2_HOUR' },
  { label: t('settingsResidual.autoMessages.hoursAfter', { value: 4 }), value: '4_HOUR' },
  { label: t('settingsResidual.autoMessages.hoursAfter', { value: 8 }), value: '8_HOUR' },
  { label: t('settingsResidual.autoMessages.hoursAfter', { value: 16 }), value: '16_HOUR' },
  { label: t('settingsResidual.autoMessages.hoursAfter', { value: 24 }), value: '24_HOUR' },
])

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
    return t('settingsResidual.autoMessages.checkIn')
  }
  if (action === 'CHECK_OUT') {
    return t('settingsResidual.autoMessages.checkOut')
  }
  return t('settingsResidual.autoMessages.bookingConfirm')
}

function formatActionPayloadLabel(action: AutoMessageAction) {
  if (action === 'CHECK_IN') {
    return '入住'
  }
  if (action === 'CHECK_OUT') {
    return '离店'
  }
  return '预订确认'
}

function formatTimingLabel(value: string) {
  const option = sendTimingOptions.value.find((item) => item.value === value)
  if (option) {
    return option.label
  }

  const parsed = parseDayTiming(value)
  if (parsed) {
    return t('settingsResidual.autoMessages.dayOffset', parsed)
  }

  return value || t('settingsResidual.common.unset')
}

function formatAutomationSummary(message: AutoMessageDTO) {
  return `${formatActionLabel(message.action)} · ${formatTimingLabel(message.sendTiming)}`
}

function formatChannelSummary(rawValue: string) {
  const channelIds = parseNumberList(rawValue)
  if (channelIds.length === 0) {
    return t('settingsResidual.autoMessages.allChannels')
  }

  const names: string[] = []
  for (const channelId of channelIds) {
    const matched = channels.value.find((item) => item.id === channelId)
    if (matched) {
      names.push(matched.name)
    }
  }

  if (names.length === 0) {
    return t('settingsResidual.autoMessages.allChannels')
  }
  return names.join('、')
}

function formatChannelTag(rawValue: string) {
  const summary = formatChannelSummary(rawValue)
  if (parseNumberList(rawValue).length === 0) {
    return ''
  }
  return t('settingsResidual.autoMessages.channel', { value: summary })
}

function formatRoomSummary(roomSelectionType: RoomSelectionType, rawValue: string) {
  if (roomSelectionType === 'ALL_LOCAL') {
    return t('settingsResidual.autoMessages.allRoomTypes')
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
      return t('settingsResidual.autoMessages.byRoomType')
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

    return names.length > 0 ? names.join('、') : t('settingsResidual.autoMessages.byGroup')
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

    return names.length > 0 ? names.join('、') : t('settingsResidual.autoMessages.byRoom')
  }

  return t('settingsResidual.common.unset')
}

function formatRoomPayloadSummary(roomSelectionType: RoomSelectionType, rawValue: string) {
  if (roomSelectionType === 'ALL_LOCAL') {
    return '全部房型'
  }

  const selectedIds = parseNumberList(rawValue)
  if (roomSelectionType === 'BY_ROOM_TYPE') {
    const names = selectedIds
      .map((id) => roomTypes.value.find((item) => item.id === id)?.name)
      .filter((name): name is string => Boolean(name))
    return names.length > 0 ? names.join('、') : '按房型'
  }
  if (roomSelectionType === 'BY_GROUP') {
    const names = selectedIds
      .map((id) => roomGroups.value.find((item) => Number(item.id) === id)?.name)
      .filter((name): name is string => Boolean(name))
    return names.length > 0 ? names.join('、') : '按分组'
  }
  if (roomSelectionType === 'BY_ROOM') {
    const names = selectedIds
      .map((id) => rooms.value.find((item) => item.id === id)?.roomNumber)
      .filter((name): name is string => Boolean(name))
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
    header: t('settingsResidual.common.confirm'),
    message: t('settingsResidual.common.confirmDelete', { name: title }),
    buttons: [
      {
        text: t('accommodation.common.cancel'),
        role: 'cancel',
      },
      {
        text: t('settingsStage4.roomSettings.messages.deleteTitle'),
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
      throw new Error(messageResponse.message || t('stage5Pattern.loadFailed'))
    }
    if (!channelResponse.success || !channelResponse.data) {
      throw new Error(channelResponse.message || t('stage5.common.messages.loadChannelsFailed'))
    }
    if (!roomTypeResponse.success || !roomTypeResponse.data) {
      throw new Error(roomTypeResponse.message || t('settingsStage4.roomSort.messages.loadRoomTypesFailed'))
    }
    if (!roomGroupResponse.success || !roomGroupResponse.data) {
      throw new Error(roomGroupResponse.message || t('settingsStage4.roomSort.messages.loadGroupsFailed'))
    }
    if (!roomResponse.success || !roomResponse.data) {
      throw new Error(roomResponse.message || t('settingsStage4.roomSort.messages.loadRoomsFailed'))
    }

    messages.value = messageResponse.data
    channels.value = channelResponse.data
    roomTypes.value = roomTypeResponse.data
    roomGroups.value = roomGroupResponse.data
    rooms.value = roomResponse.data
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, t('stage5Pattern.loadFailed')))
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
  const nextTitle = copyMode
    ? `${message.title} (${t('settingsResidual.common.copied')})`
    : message.title

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
    showWarningToast(t('stage5UiAttributes.85'))
    return
  }
  if (!messageForm.value.message.trim()) {
    showWarningToast(t('stage5Pattern.enter'))
    return
  }
  if (messageForm.value.selectedChannels.length === 0) {
    showWarningToast(t('stage5Pattern.atLeast'))
    return
  }
  if (messageForm.value.roomSelectionType !== 'ALL_LOCAL' && messageForm.value.selectedRoomTypeIds.length === 0) {
    showWarningToast(t('stage5Pattern.select'))
    return
  }

  let sendTiming: SendTiming
  if (messageForm.value.action === 'CHECK_IN' || messageForm.value.action === 'CHECK_OUT') {
    if (!/^-?\d+$/.test(messageForm.value.day.trim())) {
      showWarningToast(t('stage5Pattern.enter'))
      return
    }

    const normalizedTime = normalizeTimeValue(messageForm.value.time)
    if (!normalizedTime) {
      showWarningToast(t('stage5Pattern.enter'))
      return
    }

    sendTiming = `DAY_${messageForm.value.day.trim()}_${normalizedTime}` as SendTiming
  } else {
    if (!messageForm.value.sendTiming) {
      showWarningToast(t('stage5Pattern.select'))
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
      automationRule: formatActionPayloadLabel((messageForm.value.action || 'BOOKING_CONFIRM') as AutoMessageAction),
      channel: channelNames.join('、'),
      room:
        messageForm.value.roomSelectionType === 'ALL_LOCAL'
          ? '全部房型'
          : formatRoomPayloadSummary(
              messageForm.value.roomSelectionType,
              JSON.stringify(messageForm.value.selectedRoomTypeIds),
            ),
    }

    if (editingMessageId.value) {
      const response = await updateAutoMessage(editingMessageId.value, payload)
      if (!response.success) {
        throw new Error(response.message || t('stage5Pattern.updateFailed'))
      }
      showSuccessToast(t('stage5Pattern.updateCompleted'))
    } else {
      const response = await createAutoMessage(payload)
      if (!response.success) {
        throw new Error(response.message || t('stage5Pattern.createFailed'))
      }
      showSuccessToast(t('stage5Pattern.createCompleted'))
    }

    handleDismissEditor()
    await loadPageData()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, t('stage5Pattern.saveFailed')))
    }
  } finally {
    submitting.value = false
  }
}

async function handleToggleMessage(message: AutoMessageDTO) {
  try {
    const response = await toggleAutoMessage(message.id)
    if (!response.success) {
      throw new Error(response.message || t('stage5Pattern.updateFailed'))
    }
    showSuccessToast(
      response.data.enabled
        ? t('settingsResidual.common.enabled')
        : t('settingsResidual.common.disabled'),
    )
    await loadPageData()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, t('stage5Pattern.updateFailed')))
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
      throw new Error(response.message || t('stage5Pattern.deleteFailed'))
    }
    showSuccessToast(t('stage5Pattern.deleteCompleted'))
    await loadPageData()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, t('stage5Pattern.deleteFailed')))
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
