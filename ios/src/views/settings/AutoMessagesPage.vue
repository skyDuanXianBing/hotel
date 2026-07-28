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

    <ion-content fullscreen class="mobile-page mobile-page--dashboard settings-auto-messages-page">
      <ion-refresher slot="fixed" @ionRefresh="handleRefresh">
        <ion-refresher-content :pulling-text="$t('stage5UiAttributes.16')" refreshing-spinner="crescent" />
      </ion-refresher>

      <section class="mobile-hero mobile-dashboard-surface settings-auto-messages-hero">
        <h1 class="mobile-title">{{ $t('routes.SettingsAutoMessages') }}</h1>
        <div class="mobile-chip-row">
          <span class="mobile-chip">{{ $t('home.quick.messages.0') }} {{ messages.length }}</span>
          <span class="mobile-chip">{{ $t('home.quick.channels.0') }} {{ channels.length }}</span>
          <span class="mobile-chip">{{ $t('accommodation.common.roomType') }} {{ roomTypes.length }}</span>
          <span class="mobile-chip">{{ $t('channel.mobile.mapping.groups') }} {{ roomGroups.length }}</span>
        </div>
      </section>

      <div class="mobile-stack">
        <section class="mobile-card mobile-dashboard-surface settings-auto-messages-list-card">
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
                <ion-button class="settings-auto-message-card__primary-action" size="small" fill="solid" @click="handleEditMessage(message)">{{ $t('accommodation.roomPrice.editTitle') }}</ion-button>
                <ion-button class="settings-auto-message-card__secondary-action" size="small" fill="outline" @click="handleCopyMessage(message)">{{ $t('home.support.copy') }}</ion-button>
                <ion-button class="settings-auto-message-card__secondary-action" size="small" fill="outline" @click="handleToggleMessage(message)">
                  {{ message.enabled ? $t('roomStatus.store.roomState.outOfOrder') : $t('settingsStage4.accountList.status.enabled') }}
                </ion-button>
                <ion-button class="settings-auto-message-card__delete-action" size="small" fill="outline" @click="handleDeleteMessage(message)">
                  {{ $t('roomStatus.roomLock.actions.delete') }}
                </ion-button>
              </div>
            </article>
          </div>

          <p v-else-if="!loading" class="mobile-note settings-auto-messages-page__empty-state">{{ $t('stage5SourceText.86') }}</p>
        </section>

      </div>

      <SettingsEditorModal
        :is-open="editorOpen"
        :title="editingMessageId ? $t('stage5DynamicUi.68') : $t('stage5DynamicUi.41')"
        :backdrop-dismiss="!submitting"
        :close-disabled="submitting"
        modal-class="settings-auto-message-editor-modal"
        content-class="settings-auto-message-editor-page"
        card-class="settings-auto-message-editor-card"
        @close="handleDismissEditor"
        @didDismiss="handleDismissEditor"
      >
        <div class="settings-form-grid settings-auto-message-editor-form">
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

              <div class="settings-toggle-field settings-auto-message-editor-toggle">
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

              <div class="settings-toggle-field settings-auto-message-editor-toggle">
                <div>
                  <strong>{{ $t('stage5SourceText.35') }}</strong>
                </div>
                <ion-toggle v-model="messageForm.enabled" />
              </div>
        </div>

        <template #actions>
          <ion-button fill="outline" @click="handleDismissEditor">{{ $t('accommodation.common.cancel') }}</ion-button>
          <ion-button :disabled="submitting" @click="handleSaveMessage">
            {{ submitting ? $t('iosStage5.cleaning.submitting') : $t('stage5DynamicUi.18') }}
          </ion-button>
        </template>
      </SettingsEditorModal>
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
import SettingsEditorModal from '@/components/settings/base/SettingsEditorModal.vue'
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

const messageVariables = computed(() => [
  { label: t('settingsResidual.messageVariables.propertyName'), code: '{{property_name}}' },
  { label: t('settingsResidual.messageVariables.guestName'), code: '{{guest_name}}' },
  { label: t('settingsResidual.messageVariables.checkInDate'), code: '{{checkin_date}}' },
  { label: t('settingsResidual.messageVariables.checkOutDate'), code: '{{checkout_date}}' },
  { label: t('settingsResidual.messageVariables.roomNumber'), code: '{{room_number}}' },
  { label: t('settingsResidual.messageVariables.checkInCode'), code: '{{checkin_code}}' },
])

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
    return t('settingsResidual.autoMessages.checkIn')
  }
  if (action === 'CHECK_OUT') {
    return t('settingsResidual.autoMessages.checkOut')
  }
  return t('settingsResidual.autoMessages.bookingConfirm')
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
  return names.join(t('settingsResidual.common.listSeparator'))
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

    return names.join(t('settingsResidual.common.listSeparator'))
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

    return names.length > 0
      ? names.join(t('settingsResidual.common.listSeparator'))
      : t('settingsResidual.autoMessages.byGroup')
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

    return names.length > 0
      ? names.join(t('settingsResidual.common.listSeparator'))
      : t('settingsResidual.autoMessages.byRoom')
  }

  return t('settingsResidual.common.unset')
}

function formatRoomPayloadSummary(roomSelectionType: RoomSelectionType, rawValue: string) {
  if (roomSelectionType === 'ALL_LOCAL') {
    return t('settingsResidual.autoMessages.allRoomTypes')
  }

  const selectedIds = parseNumberList(rawValue)
  if (roomSelectionType === 'BY_ROOM_TYPE') {
    const names = selectedIds
      .map((id) => roomTypes.value.find((item) => item.id === id)?.name)
      .filter((name): name is string => Boolean(name))
    return names.length > 0
      ? names.join(t('settingsResidual.common.listSeparator'))
      : t('settingsResidual.autoMessages.byRoomType')
  }
  if (roomSelectionType === 'BY_GROUP') {
    const names = selectedIds
      .map((id) => roomGroups.value.find((item) => Number(item.id) === id)?.name)
      .filter((name): name is string => Boolean(name))
    return names.length > 0
      ? names.join(t('settingsResidual.common.listSeparator'))
      : t('settingsResidual.autoMessages.byGroup')
  }
  if (roomSelectionType === 'BY_ROOM') {
    const names = selectedIds
      .map((id) => rooms.value.find((item) => item.id === id)?.roomNumber)
      .filter((name): name is string => Boolean(name))
    return names.length > 0
      ? names.join(t('settingsResidual.common.listSeparator'))
      : t('settingsResidual.autoMessages.byRoom')
  }
  return t('settingsResidual.common.unset')
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
      channel: channelNames.join(t('settingsResidual.common.listSeparator')),
      room:
        messageForm.value.roomSelectionType === 'ALL_LOCAL'
          ? t('settingsResidual.autoMessages.allRoomTypes')
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
  --background: var(--ios-pms-dashboard-page-background);
  --padding-top: 12px;
  --padding-bottom: calc(30px + var(--app-safe-bottom));
  --padding-start: 16px;
  --padding-end: 16px;
}

.app-page-header__title {
  color: #333333;
  font-size: 20px;
  font-weight: 500;
  letter-spacing: 0;
}

.app-page-header__text-btn {
  --color: #333333;
  font-size: 16px;
  font-weight: 400;
  letter-spacing: 0;
}

.settings-auto-messages-hero {
  margin-top: 10px;
  padding: 18px 16px 20px;
  border: 1px solid var(--ios-pms-dashboard-card-border);
  border-radius: var(--ios-pms-radius-card);
  background: var(--ios-pms-dashboard-card-background);
  box-shadow: var(--ios-pms-dashboard-card-shadow);
}

.settings-auto-messages-hero::before {
  display: none;
}

.settings-auto-messages-hero .mobile-title {
  margin: 0;
  color: #333333;
  font-size: 22px;
  font-weight: 600;
  line-height: 1.24;
  letter-spacing: 0;
}

.settings-auto-messages-hero .mobile-chip-row {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
  margin-top: 14px;
}

.settings-auto-messages-hero .mobile-chip {
  min-height: 23px;
  padding: 3px 10px;
  border: 0;
  border-radius: var(--ios-pms-radius-pill);
  background: rgba(var(--ion-color-primary-rgb), 0.1);
  color: #1d73ff;
  font-size: 13px;
  font-weight: 500;
  line-height: 1.2;
  letter-spacing: 0;
}

.settings-auto-messages-page > .mobile-stack {
  gap: 0;
  margin-top: 20px;
  padding-bottom: 6px;
}

.settings-auto-messages-list-card {
  padding: 24px 16px 22px;
  border: 1px solid var(--ios-pms-dashboard-card-border);
  border-radius: var(--ios-pms-radius-card);
  background: var(--ios-pms-dashboard-card-background);
  box-shadow: var(--ios-pms-dashboard-card-shadow);
}

.settings-auto-messages-page__section-header {
  align-items: center;
  min-height: 32px;
}

.settings-auto-messages-page__section-header .mobile-section-title {
  margin: 0;
  color: #333333;
  font-size: 22px;
  font-weight: 600;
  line-height: 1.25;
  letter-spacing: 0;
}

.settings-auto-messages-page__section-header ion-spinner {
  width: 18px;
  height: 18px;
  color: rgba(var(--ion-color-primary-rgb), 0.78);
}

.settings-auto-messages-list {
  gap: 14px;
  margin-top: 18px;
}

.settings-auto-message-card {
  padding: 16px 14px 18px;
  border: 1px solid rgba(130, 143, 165, 0.2);
  border-radius: var(--ios-pms-radius-input);
  background: rgba(255, 255, 255, 0.9);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.9),
    0 6px 16px rgba(77, 98, 145, 0.035);
}

.settings-auto-message-card::before {
  display: none;
}

.settings-auto-message-card .settings-minimal-card__header {
  flex-wrap: nowrap;
  align-items: flex-start;
  gap: 12px;
}

.settings-auto-message-card .settings-minimal-card__title-group {
  flex: 1;
  gap: 6px;
}

.settings-auto-message-card .settings-minimal-card__title-group strong {
  color: #333333;
  font-size: 19px;
  font-weight: 600;
  line-height: 1.25;
  letter-spacing: 0;
}

.settings-auto-message-card .settings-minimal-card__summary {
  color: #8f8f8f;
  font-size: 13px;
  font-weight: 400;
  line-height: 1.35;
  letter-spacing: 0;
}

.settings-auto-message-card .settings-minimal-card__badge {
  min-height: 28px;
  padding: 2px 10px;
  border: 0;
  border-radius: var(--ios-pms-radius-pill);
  font-size: 13px;
  font-weight: 400;
  line-height: 1.2;
  letter-spacing: 0;
}

.settings-auto-message-card .settings-minimal-card__badge--success {
  background: rgba(var(--ion-color-success-rgb), 0.74);
  color: #ffffff;
}

.settings-auto-message-card .settings-minimal-card__badge--warning {
  background: rgba(var(--ion-color-warning-rgb), 0.13);
  color: var(--ion-color-warning);
}

.settings-auto-message-card .settings-minimal-card__meta {
  gap: 8px;
  margin-top: 14px;
}

.settings-auto-message-card .settings-minimal-card__meta-pill {
  min-height: 24px;
  padding: 2px 10px;
  border: 1px solid #d9d9d9;
  border-radius: 11px;
  background: rgba(255, 255, 255, 0.78);
  color: #444444;
  font-size: 13px;
  font-weight: 400;
  line-height: 1.2;
  letter-spacing: 0;
}

.settings-auto-message-card .settings-minimal-card__actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 14px;
  padding-top: 0;
  border-top: 0;
}

.settings-auto-message-card .settings-minimal-card__actions ion-button {
  flex: 0 0 auto;
  width: auto;
  min-width: 0;
  height: 30px;
  min-height: 30px;
  margin: 0;
  --padding-start: 13px;
  --padding-end: 13px;
  --padding-top: 0;
  --padding-bottom: 0;
  --border-color: #d9d9d9;
  --border-style: solid;
  --border-width: 1px;
  --border-radius: 11px;
  --box-shadow: none;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 0;
}

.settings-auto-message-card .settings-minimal-card__actions ion-button::part(native) {
  min-height: 30px;
  padding-top: 2px;
  padding-bottom: 2px;
}

.settings-auto-message-card__primary-action {
  --background: #266eff;
  --background-activated: #1f5fe0;
  --border-color: #266eff;
  --color: #ffffff;
}

.settings-auto-message-card__secondary-action {
  --background: rgba(255, 255, 255, 0.72);
  --background-activated: rgba(52, 116, 246, 0.05);
  --border-color: #d9d9d9;
  --color: #2346ff;
}

.settings-auto-message-card__delete-action {
  --background: rgba(255, 255, 255, 0.72);
  --background-activated: rgba(255, 0, 0, 0.05);
  --background-activated-opacity: 1;
  --background-focused: rgba(255, 0, 0, 0.04);
  --background-hover: rgba(255, 0, 0, 0.03);
  --border-color: #d9d9d9;
  --color: #ff0000;
  --color-activated: #ff0000;
  --color-focused: #ff0000;
  --color-hover: #ff0000;
  color: #ff0000;
}

.settings-auto-messages-page__empty-state {
  margin: 18px 0 0;
  padding: 24px 12px;
  border: 1px dashed rgba(130, 143, 165, 0.24);
  border-radius: var(--ios-pms-radius-input);
  color: var(--ios-pms-text-muted);
  text-align: center;
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

:global(.settings-auto-message-editor-modal) {
  --width: 100%;
  --height: 100%;
  --border-radius: 0;
  --background: #eef6ff;
}

:global(.settings-auto-message-editor-modal ion-header) {
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
}

:global(.settings-auto-message-editor-modal ion-header::after) {
  display: none;
}

:global(.settings-auto-message-editor-modal ion-toolbar) {
  --background: rgba(255, 255, 255, 0.94);
  --border-color: transparent;
  --min-height: 64px;
  --padding-start: 16px;
  --padding-end: 16px;
}

:global(.settings-auto-message-editor-modal ion-title) {
  color: #333333;
  font-size: 23px;
  font-weight: 400;
  letter-spacing: 0;
}

:global(.settings-auto-message-editor-close) {
  max-width: min(42vw, 150px);
  min-height: 36px;
  margin: 0;
  --padding-start: 0;
  --padding-end: 0;
  --background: transparent;
  --background-activated: transparent;
  --box-shadow: none;
  --color: #777777;
  color: #777777;
  font-size: 20px;
  font-weight: 400;
  letter-spacing: 0;
}

:global(.settings-auto-message-editor-close::part(native)) {
  white-space: normal;
}

:global(ion-content.settings-auto-message-editor-page) {
  --background: #eef6ff;
  --padding-top: 34px;
  --padding-bottom: calc(90px + var(--app-safe-bottom));
  --padding-start: 16px;
  --padding-end: 16px;
  background: #eef6ff;
}

:global(.settings-auto-message-editor-card) {
  width: 100%;
  margin: 0 auto;
  padding: 30px 16px 24px;
  border: 0;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 8px 18px rgba(67, 92, 132, 0.08);
}

:global(.settings-auto-message-editor-form) {
  gap: 18px;
}

:global(.settings-auto-message-editor-card .settings-form-field) {
  display: grid;
  gap: 6px;
  padding: 0;
  border: 0;
  border-radius: 0;
  background: transparent;
  box-shadow: none;
  transform: none;
  transition: none;
}

:global(.settings-auto-message-editor-card .settings-form-field span),
:global(.settings-auto-message-editor-card .settings-variable-panel h3),
:global(.settings-auto-message-editor-card .settings-auto-message-editor-toggle strong) {
  color: #333333;
  font-weight: 400;
  line-height: 1.25;
  letter-spacing: 0;
}

:global(.settings-auto-message-editor-card .settings-form-field span),
:global(.settings-auto-message-editor-card .settings-variable-panel h3) {
  font-size: 20px;
}

:global(.settings-auto-message-editor-card .settings-auto-message-editor-toggle strong) {
  font-size: 18px;
}

:global(.settings-auto-message-editor-card .settings-form-field ion-input),
:global(.settings-auto-message-editor-card .settings-form-field ion-select),
:global(.settings-auto-message-editor-card .settings-form-field ion-textarea) {
  box-sizing: border-box;
  display: block;
  width: 100%;
  min-height: 44px;
  height: 44px;
  margin: 0;
  overflow: visible;
  border: 1px solid #d8d8dc;
  border-radius: 11px;
  background: rgba(255, 255, 255, 0.62);
  box-shadow: none;
  color: #333333;
  font-size: 18px;
  font-weight: 400;
  --background: rgba(255, 255, 255, 0.62);
  --border-color: #d8d8dc;
  --border-radius: 11px;
  --color: #333333;
  --highlight-color-focused: #d8d8dc;
  --highlight-color-valid: #d8d8dc;
  --highlight-color-invalid: var(--ion-color-danger);
  --placeholder-color: #666666;
  --placeholder-opacity: 1;
  --padding-start: 18px;
  --padding-end: 18px;
  --padding-top: 0;
  --padding-bottom: 0;
}

:global(.settings-auto-message-editor-card .settings-form-field ion-input::part(native)),
:global(.settings-auto-message-editor-card .settings-form-field ion-textarea::part(native)) {
  color: #333333;
  font-size: 18px;
  font-weight: 400;
  line-height: 1.35;
}

:global(.settings-auto-message-editor-card .settings-form-field ion-textarea[rows='7']) {
  height: auto;
  min-height: 132px;
  --padding-top: 12px;
  --padding-bottom: 12px;
}

:global(.settings-auto-message-editor-card .settings-form-field ion-textarea[rows='7']::part(native)) {
  min-height: 108px;
}

:global(.settings-auto-message-editor-card .settings-variable-panel) {
  display: grid;
  gap: 12px;
  margin-top: 0;
  padding: 0;
  border-radius: 0;
  background: transparent;
}

:global(.settings-auto-message-editor-card .settings-variable-panel h3) {
  margin: 0;
}

:global(.settings-auto-message-editor-card .settings-variable-panel__list) {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 10px;
  min-height: 56px;
  margin-top: 0;
  padding: 10px 18px;
  border: 1px solid #d8d8dc;
  border-radius: 11px;
  background: rgba(255, 255, 255, 0.62);
}

:global(.settings-auto-message-editor-card .settings-variable-chip) {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 0;
  max-width: 100%;
  min-height: 28px;
  height: auto;
  margin: 0;
  padding: 5px 10px;
  border: 0;
  border-radius: var(--ios-pms-radius-pill);
  background: #e1f0ff;
  color: #017cfe;
  font-size: 12px;
  font-weight: 400;
  line-height: 1.2;
  letter-spacing: 0;
  text-align: center;
  white-space: normal;
  overflow-wrap: anywhere;
}

:global(.settings-auto-message-editor-toggle) {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 14px;
  min-height: 41px;
  margin-top: 14px;
  padding: 8px 16px;
  border: 0;
  border-radius: 9px;
  background: #dceaff;
  box-shadow: none;
}

:global(.settings-auto-message-editor-toggle ion-toggle) {
  width: 44px;
  min-width: 44px;
  height: 22px;
  min-height: 22px;
  contain: layout size style;
  --track-background: #d9d9d9;
  --track-background-checked: linear-gradient(90deg, #81bfff 0%, #017cfe 100%);
  --handle-background: #ffffff;
  --handle-background-checked: #ffffff;
  --handle-width: 22px;
  --handle-height: 22px;
  --handle-spacing: 0;
  --handle-box-shadow: none;
}

:global(.settings-auto-message-editor-toggle ion-toggle::part(track)) {
  width: 44px;
  height: 22px;
  border-radius: var(--ios-pms-radius-pill);
}

:global(.settings-auto-message-editor-toggle ion-toggle::part(handle)) {
  top: 0;
  width: 22px;
  height: 22px;
  margin: 0;
  border-radius: 50%;
  box-shadow: none;
}

:global(.settings-auto-message-editor-card .settings-form-actions) {
  display: grid;
  grid-template-columns: minmax(0, 0.92fr) minmax(0, 1.08fr);
  gap: 12px;
  margin-top: 24px;
  padding-top: 0;
  border-top: 0;
}

:global(.settings-auto-message-editor-card .settings-form-actions ion-button) {
  width: 100%;
  min-height: 30px;
  height: 30px;
  margin: 0;
  --padding-top: 0;
  --padding-bottom: 0;
  --padding-start: 10px;
  --padding-end: 10px;
  --border-radius: 6px;
  --box-shadow: none;
  font-size: 13px;
  font-weight: 500;
  line-height: 1.2;
  letter-spacing: 0;
}

:global(.settings-auto-message-editor-card .settings-form-actions ion-button::part(native)) {
  min-height: 30px;
  padding-top: 0;
  padding-bottom: 0;
  white-space: normal;
}

:global(.settings-auto-message-editor-card .settings-form-actions ion-button[fill='outline']) {
  --background: rgba(255, 255, 255, 0.96);
  --background-activated: #f7f7f7;
  --border-color: rgba(193, 204, 220, 0.95);
  --border-width: 1px;
  --color: #8a96a8;
}

:global(.settings-auto-message-editor-card .settings-form-actions ion-button:not([fill='outline'])) {
  --background: #2687f7;
  --background-activated: #1f78df;
  --color: #ffffff;
}
</style>
