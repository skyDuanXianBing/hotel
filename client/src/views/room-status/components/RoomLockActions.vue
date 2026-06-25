<template>
  <section class="room-lock-actions" :class="{ compact }">
    <div class="lock-header">
      <div>
        <div class="lock-title">
          <el-icon><Lock /></el-icon>
          <span>{{ lockT('title') }}</span>
        </div>
        <div class="lock-subtitle">{{ targetLabel }}</div>
      </div>
      <el-button
        size="small"
        plain
        :icon="Refresh"
        :loading="isStatusLoading"
        :disabled="!normalizedTarget || (!canUseControlDevice && !statusError)"
        @click="refreshCurrentStatus"
      >
        {{ lockT('actions.refresh') }}
      </el-button>
    </div>

    <el-alert
      v-if="!normalizedTarget"
      :title="lockT('messages.missingTarget')"
      type="warning"
      :closable="false"
      show-icon
    />

    <template v-else>
      <div class="lock-status-panel" :class="statusClass">
        <div class="status-main">
          <span class="status-label">{{ statusLabel }}</span>
          <el-tag size="small" :type="statusTagType">{{ statusTagText }}</el-tag>
        </div>
        <div class="status-meta">
          <span>{{ providerText }}</span>
          <span>{{ controlDeviceText }}</span>
          <span>{{ passcodeDeviceText }}</span>
          <span>{{ onlineText }}</span>
          <span>{{ batteryText }}</span>
        </div>
        <div v-if="statusError" class="status-error">{{ statusError }}</div>
        <div v-else-if="controlUnavailableText" class="status-hint">
          {{ controlUnavailableText }}
        </div>
        <div v-else-if="pollingStatusText" class="status-polling">{{ pollingStatusText }}</div>
        <div v-else-if="statusUpdatedText" class="status-updated">
          {{ lockT('status.meta.updatedAt', { time: statusUpdatedText }) }}
        </div>
      </div>

      <div class="lock-action-row">
        <el-button
          type="success"
          plain
          :icon="Unlock"
          :loading="isActionBusy('UNLOCK')"
          :disabled="!canUseControlDevice || isCurrentControlBusy"
          @click="beginProtectedAction('UNLOCK')"
        >
          {{ lockT('actions.unlock') }}
        </el-button>
        <el-button
          type="warning"
          plain
          :icon="Lock"
          :loading="isActionBusy('LOCK')"
          :disabled="!canUseControlDevice || isCurrentControlBusy"
          @click="beginProtectedAction('LOCK')"
        >
          {{ lockT('actions.lock') }}
        </el-button>
        <el-button
          plain
          :icon="Key"
          :disabled="!canUsePasscodeDevice || isCurrentPasscodeBusy"
          @click="openPasscodeDialog"
        >
          {{ lockT('actions.setPasscode') }}
        </el-button>
      </div>

      <div class="passcode-section">
        <div class="passcode-header">
          <span>{{ lockT('passcodes.title') }}</span>
          <el-button
            link
            size="small"
            :loading="isPasscodesLoading"
            :disabled="!canUsePasscodeDevice || isCurrentPasscodeBusy"
            @click="reloadCurrentPasscodes"
          >
            {{ lockT('actions.refreshPasscodes') }}
          </el-button>
        </div>

        <div v-if="passcodeError" class="passcode-error">{{ passcodeError }}</div>
        <el-empty
          v-else-if="!isPasscodesLoading && currentPasscodes.length === 0"
          :description="passcodeEmptyDescription"
          :image-size="48"
        />
        <div v-else class="passcode-list" v-loading="isPasscodesLoading">
          <div v-for="passcode in currentPasscodes" :key="getPasscodeKey(passcode)" class="passcode-item">
            <div class="passcode-info">
              <div class="passcode-main">
                <span class="passcode-code">{{ getPasscodeDisplay(passcode) }}</span>
                <el-tag size="small" effect="plain">{{ getPasscodeStatusText(passcode) }}</el-tag>
              </div>
              <div class="passcode-meta">{{ getPasscodeValidityText(passcode) }}</div>
              <div v-if="passcode.passcodeName" class="passcode-name">
                {{ passcode.passcodeName }}
              </div>
            </div>
            <el-button
              link
              type="danger"
              :icon="Delete"
              :loading="isDeleteBusy(passcode)"
              :disabled="
                !canUsePasscodeDevice ||
                !getPasscodeRecordId(passcode) ||
                isPasscodeDeletePending(passcode)
              "
              @click="confirmDeletePasscode(passcode)"
            >
              {{ lockT('actions.delete') }}
            </el-button>
          </div>
        </div>
      </div>
    </template>

    <el-dialog
      v-model="confirmDialogVisible"
      :title="confirmDialogTitle"
      width="440px"
      append-to-body
      :close-on-click-modal="false"
      @close="resetConfirmationDialog"
    >
      <div v-if="pendingConfirmation" class="confirm-dialog-content">
        <el-alert
          :title="confirmDialogWarning"
          type="warning"
          show-icon
          :closable="false"
        />
        <div class="confirm-target">
          <span>{{ getTargetDisplay(pendingConfirmation.target) }}</span>
          <span v-if="pendingConfirmation.target.date">{{ pendingConfirmation.target.date }}</span>
          <span v-if="pendingConfirmation.target.guestName">
            {{ pendingConfirmation.target.guestName }}
          </span>
        </div>
        <el-input
          v-model="confirmationReason"
          type="textarea"
          :rows="3"
          maxlength="120"
          show-word-limit
          :placeholder="lockT('placeholders.reason')"
        />
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button :disabled="confirmSubmitting" @click="confirmDialogVisible = false">
            {{ lockT('actions.cancel') }}
          </el-button>
          <el-button
            type="primary"
            :loading="confirmSubmitting"
            :disabled="confirmSubmitting"
            @click="submitConfirmedAction"
          >
            {{ lockT('actions.confirmExecute') }}
          </el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog
      v-model="passcodeDialogVisible"
      :title="lockT('dialogs.passcode.title')"
      width="520px"
      append-to-body
      :close-on-click-modal="false"
      @close="resetPasscodeDialog"
    >
      <el-form :model="passcodeForm" label-width="112px" class="passcode-form">
        <el-form-item :label="lockT('fields.room')">
          <span>{{ passcodeTarget ? getTargetDisplay(passcodeTarget) : '-' }}</span>
        </el-form-item>
        <el-form-item :label="lockT('fields.passcodeMode')" required>
          <el-radio-group v-model="passcodeForm.generate">
            <el-radio :value="true">{{ lockT('passcodeModes.generated') }}</el-radio>
            <el-radio :value="false">{{ lockT('passcodeModes.manual') }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="!passcodeForm.generate" :label="lockT('fields.passcode')" required>
          <el-input
            v-model="passcodeForm.passcode"
            maxlength="12"
            show-word-limit
            :placeholder="lockT('placeholders.passcode')"
          />
          <div class="field-tip">{{ lockT('hints.passcodeLength') }}</div>
        </el-form-item>
        <el-form-item :label="lockT('fields.validity')" required>
          <div class="date-range-fields">
            <el-date-picker
              v-model="passcodeForm.validFrom"
              type="datetime"
              value-format="YYYY-MM-DDTHH:mm:ss"
              format="YYYY-MM-DD HH:mm:ss"
              :placeholder="lockT('placeholders.startDate')"
            />
            <span>{{ lockT('fields.validitySeparator') }}</span>
            <el-date-picker
              v-model="passcodeForm.validUntil"
              type="datetime"
              value-format="YYYY-MM-DDTHH:mm:ss"
              format="YYYY-MM-DD HH:mm:ss"
              :placeholder="lockT('placeholders.endDate')"
            />
          </div>
        </el-form-item>
        <el-form-item :label="lockT('fields.name')">
          <el-input
            v-model="passcodeForm.name"
            maxlength="40"
            :placeholder="lockT('placeholders.passcodeName')"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="passcodeDialogVisible = false">
            {{ lockT('actions.cancel') }}
          </el-button>
          <el-button
            type="primary"
            :loading="passcodeSubmitting"
            :disabled="passcodeSubmitting"
            @click="submitPasscode"
          >
            {{ lockT('actions.savePasscode') }}
          </el-button>
        </div>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Key, Lock, Refresh, Unlock } from '@element-plus/icons-vue'
import {
  createRoomLockConfirmation,
  createRoomLockPasscode,
  deleteRoomLockPasscode,
  getRoomLockPasscodes,
  getRoomLockStatus,
  lockRoomLock,
  refreshRoomLockStatus,
  unlockRoomLock,
  type CreateRoomLockPasscodeRequest,
  type SmartLockProvider,
  type RoomLockActionRequest,
  type RoomLockActionResultDTO,
  type RoomLockConfirmationDTO,
  type RoomLockOperationAction,
  type RoomLockOperationContext,
  type RoomLockPasscodeDTO,
  type RoomLockStatusDTO,
} from '@/api/roomLock'
import {
  addDaysToLocalDateTime,
  getStoreLocalDateTime,
  normalizeYmdInput,
} from '@/utils/storeDateTime'

const PASSCODE_MIN_LENGTH = 6
const PASSCODE_MAX_LENGTH = 12
const PASSCODE_DEFAULT_VALIDITY_DAYS = 1
const LOCK_STATUS_POLL_INTERVAL_MS = 1000
const LOCK_STATUS_POLL_MAX_ATTEMPTS = 10
const PASSCODE_CREATE_PENDING_STATUSES = new Set([
  'PENDING',
  'CREATE_PENDING',
  'PENDING_CREATE',
  'CREATE_REQUESTED',
  'CREATING',
  'PROCESSING',
])
const PASSCODE_DELETE_PENDING_STATUSES = new Set([
  'DELETE_PENDING',
  'PENDING_DELETE',
  'DELETE_REQUESTED',
  'DELETING',
])
const FAILED_STATUS = 'FAILED'

const props = defineProps<{
  target: RoomLockOperationContext | null
  compact?: boolean
}>()

const { t } = useI18n()
const lockT = (key: string, params?: Record<string, string | number>) => {
  const fullKey = `roomStatus.roomLock.${key}`
  return params ? t(fullKey, params) : t(fullKey)
}

interface PendingConfirmation {
  action: RoomLockOperationAction
  target: RoomLockOperationContext
  confirmation: RoomLockConfirmationDTO
  idempotencyKey: string
}

interface LockStatusPolling {
  action: RoomLockOperationAction
  attempts: number
  key: string
  roomId: number
  timerId: ReturnType<typeof setTimeout> | null
}

const statusMap = ref<Map<number, RoomLockStatusDTO | null>>(new Map())
const statusLoadingMap = ref<Map<number, boolean>>(new Map())
const statusErrorMap = ref<Map<number, string>>(new Map())
const passcodeMap = ref<Map<number, RoomLockPasscodeDTO[]>>(new Map())
const passcodesLoadingMap = ref<Map<number, boolean>>(new Map())
const passcodeErrorMap = ref<Map<number, string>>(new Map())
const actionLoadingMap = ref<Map<string, boolean>>(new Map())
const statusPolling = ref<LockStatusPolling | null>(null)

const confirmDialogVisible = ref(false)
const confirmSubmitting = ref(false)
const pendingConfirmation = ref<PendingConfirmation | null>(null)
const confirmationReason = ref('')

const passcodeDialogVisible = ref(false)
const passcodeSubmitting = ref(false)
const passcodeTarget = ref<RoomLockOperationContext | null>(null)
const passcodeForm = ref({
  generate: true,
  passcode: '',
  validFrom: '',
  validUntil: '',
  name: '',
})

const normalizedTarget = computed(() => normalizeTarget(props.target))
const currentRoomId = computed(() => normalizedTarget.value?.roomId || 0)
const currentStatus = computed(() => statusMap.value.get(currentRoomId.value) || null)
const statusError = computed(() => statusErrorMap.value.get(currentRoomId.value) || '')
const currentPasscodes = computed(() => passcodeMap.value.get(currentRoomId.value) || [])
const passcodeError = computed(() => passcodeErrorMap.value.get(currentRoomId.value) || '')
const isStatusLoading = computed(() => statusLoadingMap.value.get(currentRoomId.value) || false)
const isPasscodesLoading = computed(() => passcodesLoadingMap.value.get(currentRoomId.value) || false)
const currentStatusPolling = computed(() => {
  const polling = statusPolling.value
  if (!polling || polling.roomId !== currentRoomId.value) {
    return null
  }
  return polling
})

const targetLabel = computed(() => {
  if (!normalizedTarget.value) {
    return lockT('target.none')
  }
  return getTargetDisplay(normalizedTarget.value)
})

const statusLabel = computed(() => getStatusLabel(currentStatus.value))
const statusTagText = computed(() => getStatusTagText(currentStatus.value))
const statusTagType = computed(() => getStatusTagType(currentStatus.value))
const statusClass = computed(() => getStatusClass(currentStatus.value))
const providerText = computed(() => getProviderText(currentStatus.value))
const controlDeviceText = computed(() => getControlDeviceText(currentStatus.value))
const passcodeDeviceText = computed(() => getPasscodeDeviceText(currentStatus.value))
const onlineText = computed(() => getOnlineText(currentStatus.value))
const batteryText = computed(() => getBatteryText(currentStatus.value))
const statusUpdatedText = computed(() => getStatusUpdatedText(currentStatus.value))
const canUseControlDevice = computed(() => canUseControlDeviceForStatus(currentStatus.value))
const canUsePasscodeDevice = computed(() => canUsePasscodeDeviceForStatus(currentStatus.value))
const controlUnavailableText = computed(() => {
  if (canUseControlDevice.value) {
    return ''
  }
  return getControlUnavailableMessage(currentStatus.value)
})
const passcodeEmptyDescription = computed(() => {
  if (canUsePasscodeDevice.value) {
    return lockT('passcodes.empty')
  }
  return lockT('passcodes.emptyNoPasscodeDevice')
})
const pollingStatusText = computed(() => {
  const polling = currentStatusPolling.value
  if (!polling) {
    return ''
  }
  const actionText =
    polling.action === 'UNLOCK' ? lockT('actions.unlock') : lockT('actions.lock')
  const attemptText =
    polling.attempts > 0
      ? lockT('polling.attempts', {
          current: polling.attempts,
          max: LOCK_STATUS_POLL_MAX_ATTEMPTS,
        })
      : lockT('polling.waiting')
  return lockT('polling.sentConfirming', {
    action: actionText,
    progress: attemptText,
  })
})

const isCurrentControlBusy = computed(() => {
  const roomId = currentRoomId.value
  if (!roomId) {
    return false
  }
  return isRoomActionBusy(roomId, ['LOCK', 'UNLOCK'])
})

const isCurrentPasscodeBusy = computed(() => {
  const roomId = currentRoomId.value
  if (!roomId) {
    return false
  }
  return isRoomActionPrefixBusy(roomId, 'PASSCODE_')
})

const confirmDialogTitle = computed(() => {
  const action = pendingConfirmation.value?.action
  if (action === 'UNLOCK') {
    return lockT('dialogs.confirm.title.unlock')
  }
  return lockT('dialogs.confirm.title.lock')
})

const confirmDialogWarning = computed(() => {
  const action = pendingConfirmation.value?.action
  if (action === 'UNLOCK') {
    return lockT('dialogs.confirm.warning.unlock')
  }
  return lockT('dialogs.confirm.warning.lock')
})

const normalizeTarget = (target: RoomLockOperationContext | null) => {
  if (!target || !target.roomId) {
    return null
  }
  return {
    ...target,
    roomId: Number(target.roomId),
    reservationId: target.reservationId ? Number(target.reservationId) : null,
    date: normalizeYmdInput(target.date || '', ''),
    checkInDate: normalizeYmdInput(target.checkInDate || '', ''),
    checkOutDate: normalizeYmdInput(target.checkOutDate || '', ''),
  }
}

const cloneCurrentTarget = () => {
  if (!normalizedTarget.value) {
    return null
  }
  return { ...normalizedTarget.value }
}

const getTargetDisplay = (target: RoomLockOperationContext) => {
  const roomType = target.roomType || ''
  const roomNumber = target.roomNumber || String(target.roomId)
  if (roomType) {
    return `${roomType}-${roomNumber}`
  }
  return roomNumber
}

const setStatusLoading = (roomId: number, loading: boolean) => {
  const next = new Map(statusLoadingMap.value)
  next.set(roomId, loading)
  statusLoadingMap.value = next
}

const setStatusError = (roomId: number, message: string) => {
  const next = new Map(statusErrorMap.value)
  if (message) {
    next.set(roomId, message)
  } else {
    next.delete(roomId)
  }
  statusErrorMap.value = next
}

const setRoomStatus = (roomId: number, status: RoomLockStatusDTO | null) => {
  const next = new Map(statusMap.value)
  next.set(roomId, status)
  statusMap.value = next
}

const setPasscodesLoading = (roomId: number, loading: boolean) => {
  const next = new Map(passcodesLoadingMap.value)
  next.set(roomId, loading)
  passcodesLoadingMap.value = next
}

const setPasscodeError = (roomId: number, message: string) => {
  const next = new Map(passcodeErrorMap.value)
  if (message) {
    next.set(roomId, message)
  } else {
    next.delete(roomId)
  }
  passcodeErrorMap.value = next
}

const setPasscodes = (roomId: number, passcodes: RoomLockPasscodeDTO[]) => {
  const next = new Map(passcodeMap.value)
  next.set(roomId, passcodes)
  passcodeMap.value = next
}

const getActionKey = (roomId: number, action: string) => `${roomId}:${action}`

const setActionLoading = (roomId: number, action: string, loading: boolean) => {
  const next = new Map(actionLoadingMap.value)
  const key = getActionKey(roomId, action)
  if (loading) {
    next.set(key, true)
  } else {
    next.delete(key)
  }
  actionLoadingMap.value = next
}

const isActionBusy = (action: string) => {
  const roomId = currentRoomId.value
  if (!roomId) {
    return false
  }
  return actionLoadingMap.value.get(getActionKey(roomId, action)) || false
}

const isRoomActionBusy = (roomId: number, actions: string[]) => {
  for (const action of actions) {
    if (actionLoadingMap.value.get(getActionKey(roomId, action))) {
      return true
    }
  }
  return false
}

const isRoomActionPrefixBusy = (roomId: number, actionPrefix: string) => {
  const keyPrefix = `${roomId}:${actionPrefix}`
  for (const [key, loading] of actionLoadingMap.value.entries()) {
    if (loading && key.startsWith(keyPrefix)) {
      return true
    }
  }
  return false
}

const isCurrentTargetRoom = (roomId: number) => {
  return normalizedTarget.value?.roomId === roomId
}

const readStringField = (value: unknown, fieldName: string) => {
  if (!value || typeof value !== 'object') {
    return ''
  }
  const message = (value as Record<string, unknown>)[fieldName]
  if (typeof message !== 'string') {
    return ''
  }
  return message.trim()
}

const readMessageField = (value: unknown) => readStringField(value, 'message')

const getResponseMessage = (response: unknown, fallbackKey: string) => {
  const message = readMessageField(response)
  if (message) {
    return message
  }
  return lockT(fallbackKey)
}

const isFailedStatus = (status: unknown) => {
  return String(status || '').trim().toUpperCase() === FAILED_STATUS
}

const getActionFailureMessage = (result: RoomLockActionResultDTO | undefined, fallbackKey: string) => {
  const errorMessage = readStringField(result, 'errorMessage')
  if (errorMessage) {
    return errorMessage
  }
  const resultMessage = readStringField(result, 'resultMessage')
  if (resultMessage) {
    return resultMessage
  }
  return lockT(fallbackKey)
}

const getPasscodeFailureMessage = (result: RoomLockPasscodeDTO | undefined, fallbackKey: string) => {
  const lastError = readStringField(result, 'lastError')
  if (lastError) {
    return lastError
  }
  return lockT(fallbackKey)
}

const getErrorMessage = (error: unknown, fallbackKey: string) => {
  const directMessage = readMessageField(error)
  if (directMessage) {
    return directMessage
  }
  if (error && typeof error === 'object') {
    const response = (error as { response?: { data?: unknown } }).response
    const responseMessage = readMessageField(response?.data)
    if (responseMessage) {
      return responseMessage
    }
  }
  return lockT(fallbackKey)
}

const loadStatus = async (roomId: number, force: boolean) => {
  if (!roomId) {
    return
  }
  setStatusLoading(roomId, true)
  setStatusError(roomId, '')
  try {
    const response = force ? await refreshRoomLockStatus(roomId) : await getRoomLockStatus(roomId)
    if (!response.success) {
      setRoomStatus(roomId, null)
      setStatusError(roomId, getResponseMessage(response, 'messages.statusLoadFailed'))
      return
    }
    const nextStatus = response.data || null
    setRoomStatus(roomId, nextStatus)
    if (!canUsePasscodeDeviceForStatus(nextStatus)) {
      setPasscodes(roomId, [])
      setPasscodeError(roomId, '')
    }
  } catch (error) {
    setRoomStatus(roomId, null)
    setStatusError(roomId, getErrorMessage(error, 'messages.statusLoadFailed'))
  } finally {
    setStatusLoading(roomId, false)
  }
}

const loadPasscodes = async (roomId: number) => {
  if (!roomId) {
    return false
  }
  setPasscodesLoading(roomId, true)
  setPasscodeError(roomId, '')
  try {
    const response = await getRoomLockPasscodes(roomId)
    if (!response.success) {
      setPasscodes(roomId, [])
      setPasscodeError(roomId, getResponseMessage(response, 'messages.passcodesLoadFailed'))
      return false
    }
    setPasscodes(roomId, Array.isArray(response.data) ? response.data : [])
    return true
  } catch (error) {
    setPasscodes(roomId, [])
    setPasscodeError(roomId, getErrorMessage(error, 'messages.passcodesLoadFailed'))
    return false
  } finally {
    setPasscodesLoading(roomId, false)
  }
}

const refreshCurrentStatus = async () => {
  const target = cloneCurrentTarget()
  if (!target) {
    ElMessage.warning(lockT('messages.missingRoom'))
    return
  }
  const cachedStatus = statusMap.value.get(target.roomId) || null
  if (!canUseControlDeviceForStatus(cachedStatus) && !statusErrorMap.value.get(target.roomId)) {
    ElMessage.warning(getControlUnavailableMessage(cachedStatus))
    return
  }
  await loadStatus(target.roomId, true)
  if (!statusErrorMap.value.get(target.roomId)) {
    const pollingResolved = completePollingIfExpectedState(target.roomId)
    if (!pollingResolved) {
      ElMessage.success(lockT('messages.statusRefreshed'))
    }
  }
}

const reloadCurrentPasscodes = async () => {
  const target = cloneCurrentTarget()
  if (!target) {
    return
  }
  const cachedStatus = statusMap.value.get(target.roomId) || null
  if (!canUsePasscodeDeviceForStatus(cachedStatus)) {
    ElMessage.warning(getPasscodeUnavailableMessage(cachedStatus))
    return
  }
  await loadPasscodes(target.roomId)
}

const createIdempotencyKey = (action: string, target: RoomLockOperationContext) => {
  const reservationId = target.reservationId || 'no-reservation'
  const date = target.date || 'no-date'
  const randomPart = Math.random().toString(36).slice(2, 10)
  return `${action.toLowerCase()}-${target.roomId}-${reservationId}-${date}-${Date.now()}-${randomPart}`
}

const beginProtectedAction = async (action: RoomLockOperationAction) => {
  const target = cloneCurrentTarget()
  if (!target) {
    ElMessage.warning(lockT('messages.missingRoom'))
    return
  }
  if (!canUseControlDevice.value) {
    ElMessage.warning(getControlUnavailableMessage(currentStatus.value))
    return
  }

  const idempotencyKey = createIdempotencyKey(action, target)
  setActionLoading(target.roomId, action, true)
  try {
    const response = await createRoomLockConfirmation(target.roomId, {
      action,
      bindingId: currentStatus.value?.bindingId,
    })
    if (!response.success || !response.data) {
      ElMessage.error(getResponseMessage(response, 'messages.confirmFailed'))
      return
    }
    pendingConfirmation.value = {
      action,
      target,
      confirmation: response.data,
      idempotencyKey,
    }
    confirmationReason.value = ''
    confirmDialogVisible.value = true
  } catch (error) {
    ElMessage.error(getErrorMessage(error, 'messages.confirmFailed'))
  } finally {
    setActionLoading(target.roomId, action, false)
  }
}

const buildActionRequest = (pending: PendingConfirmation): RoomLockActionRequest => {
  const reason = confirmationReason.value.trim()
  return {
    confirm: true,
    confirmToken: pending.confirmation.confirmToken,
    bindingId: pending.confirmation.bindingId,
    idempotencyKey: pending.idempotencyKey,
    reason: reason || undefined,
  }
}

const submitConfirmedAction = async () => {
  if (confirmSubmitting.value) {
    return
  }

  const pending = pendingConfirmation.value
  if (!pending) {
    return
  }

  confirmSubmitting.value = true
  setActionLoading(pending.target.roomId, pending.action, true)
  let pollingStarted = false
  try {
    const requestData = buildActionRequest(pending)
    const response =
      pending.action === 'UNLOCK'
        ? await unlockRoomLock(pending.target.roomId, requestData)
        : await lockRoomLock(pending.target.roomId, requestData)

    if (!response.success) {
      ElMessage.error(getResponseMessage(response, 'messages.actionFailed'))
      return
    }
    if (isFailedStatus(response.data?.status)) {
      ElMessage.error(getActionFailureMessage(response.data, 'messages.actionFailed'))
      return
    }

    ElMessage.success(getActionSentMessage(pending.action))
    confirmDialogVisible.value = false
    pendingConfirmation.value = null
    confirmationReason.value = ''
    if (isCurrentTargetRoom(pending.target.roomId)) {
      startActionStatusPolling(pending.target.roomId, pending.action)
      pollingStarted = true
    }
  } catch (error) {
    ElMessage.error(getErrorMessage(error, 'messages.actionFailed'))
  } finally {
    confirmSubmitting.value = false
    if (!pollingStarted) {
      setActionLoading(pending.target.roomId, pending.action, false)
    }
  }
}

const stopStatusPolling = () => {
  const polling = statusPolling.value
  if (!polling) {
    return
  }
  if (polling.timerId) {
    clearTimeout(polling.timerId)
  }
  statusPolling.value = null
  setActionLoading(polling.roomId, polling.action, false)
}

const startActionStatusPolling = (roomId: number, action: RoomLockOperationAction) => {
  stopStatusPolling()
  const key = `${roomId}:${action}:${Date.now()}`
  statusPolling.value = {
    action,
    attempts: 0,
    key,
    roomId,
    timerId: null,
  }
  setActionLoading(roomId, action, true)
  void runStatusPollingAttempt(key)
}

const runStatusPollingAttempt = async (pollingKey: string) => {
  const polling = statusPolling.value
  if (!polling || polling.key !== pollingKey) {
    return
  }
  if (!isCurrentTargetRoom(polling.roomId)) {
    stopStatusPolling()
    return
  }

  const currentAttempt = polling.attempts + 1
  statusPolling.value = {
    ...polling,
    attempts: currentAttempt,
    timerId: null,
  }

  await loadStatus(polling.roomId, true)

  const activePolling = statusPolling.value
  if (!activePolling || activePolling.key !== pollingKey) {
    return
  }
  if (!isCurrentTargetRoom(activePolling.roomId)) {
    stopStatusPolling()
    return
  }
  if (isExpectedActionState(activePolling.action, getCurrentLockState(activePolling.roomId))) {
    ElMessage.success(getActionConfirmedMessage(activePolling.action))
    stopStatusPolling()
    return
  }
  if (currentAttempt >= LOCK_STATUS_POLL_MAX_ATTEMPTS) {
    const message = getActionTimeoutMessage(activePolling.action)
    setStatusError(activePolling.roomId, message)
    ElMessage.warning(message)
    stopStatusPolling()
    return
  }

  const timerId = setTimeout(() => {
    void runStatusPollingAttempt(pollingKey)
  }, LOCK_STATUS_POLL_INTERVAL_MS)
  statusPolling.value = {
    ...activePolling,
    timerId,
  }
}

const getCurrentLockState = (roomId: number) => {
  return normalizeLockState(statusMap.value.get(roomId) || null)
}

const completePollingIfExpectedState = (roomId: number) => {
  const polling = statusPolling.value
  if (!polling || polling.roomId !== roomId) {
    return false
  }
  if (!isExpectedActionState(polling.action, getCurrentLockState(roomId))) {
    return false
  }
  ElMessage.success(getActionConfirmedMessage(polling.action))
  stopStatusPolling()
  return true
}

const resetConfirmationDialog = () => {
  if (confirmSubmitting.value) {
    return
  }
  pendingConfirmation.value = null
  confirmationReason.value = ''
}

const openPasscodeDialog = () => {
  const target = cloneCurrentTarget()
  if (!target) {
    ElMessage.warning(lockT('messages.missingRoom'))
    return
  }
  if (!canUsePasscodeDevice.value) {
    ElMessage.warning(getPasscodeUnavailableMessage(currentStatus.value))
    return
  }

  passcodeTarget.value = target
  const defaultStart = getStoreLocalDateTime()
  const defaultUntil = addDaysToLocalDateTime(defaultStart, PASSCODE_DEFAULT_VALIDITY_DAYS)
  passcodeForm.value = {
    generate: true,
    passcode: '',
    validFrom: defaultStart,
    validUntil: defaultUntil,
    name: target.guestName
      ? lockT('passcodes.guestDefaultName', { guestName: target.guestName })
      : lockT('passcodes.defaultName'),
  }
  passcodeDialogVisible.value = true
}

const resetPasscodeDialog = () => {
  if (passcodeSubmitting.value) {
    return
  }
  passcodeTarget.value = null
  passcodeForm.value = {
    generate: true,
    passcode: '',
    validFrom: '',
    validUntil: '',
    name: '',
  }
}

const isManualPasscodeValid = () => {
  const passcode = passcodeForm.value.passcode.trim()
  if (passcodeForm.value.generate) {
    return true
  }
  if (!passcode) {
    ElMessage.warning(lockT('messages.passcodeRequired'))
    return false
  }
  if (!/^\d+$/.test(passcode)) {
    ElMessage.warning(lockT('messages.passcodeDigitsOnly'))
    return false
  }
  if (passcode.length < PASSCODE_MIN_LENGTH || passcode.length > PASSCODE_MAX_LENGTH) {
    ElMessage.warning(lockT('messages.passcodeLengthInvalid'))
    return false
  }
  return true
}

const validatePasscodeForm = () => {
  if (!passcodeTarget.value) {
    ElMessage.warning(lockT('messages.missingRoom'))
    return false
  }
  const cachedStatus = statusMap.value.get(passcodeTarget.value.roomId) || null
  if (!canUsePasscodeDeviceForStatus(cachedStatus)) {
    ElMessage.warning(getPasscodeUnavailableMessage(cachedStatus))
    return false
  }
  if (!isManualPasscodeValid()) {
    return false
  }
  if (!passcodeForm.value.validFrom || !passcodeForm.value.validUntil) {
    ElMessage.warning(lockT('messages.validityRequired'))
    return false
  }
  if (passcodeForm.value.validFrom >= passcodeForm.value.validUntil) {
    ElMessage.warning(lockT('messages.validityInvalid'))
    return false
  }
  return true
}

const getNormalizedPasscodeStatus = (passcode?: RoomLockPasscodeDTO | null) => {
  return String(passcode?.status || '').trim().toUpperCase()
}

const isPasscodePendingStatus = (status: string) => {
  return PASSCODE_CREATE_PENDING_STATUSES.has(status)
}

const isPasscodeDeletePendingStatus = (status: string) => {
  return PASSCODE_DELETE_PENDING_STATUSES.has(status)
}

const isPasscodeDeletePending = (passcode: RoomLockPasscodeDTO) => {
  return isPasscodeDeletePendingStatus(getNormalizedPasscodeStatus(passcode))
}

const submitPasscode = async () => {
  if (passcodeSubmitting.value) {
    return
  }

  if (!validatePasscodeForm() || !passcodeTarget.value) {
    return
  }

  const target = { ...passcodeTarget.value }
  const cachedStatus = statusMap.value.get(target.roomId) || null
  if (!canUsePasscodeDeviceForStatus(cachedStatus)) {
    ElMessage.warning(getPasscodeUnavailableMessage(cachedStatus))
    return
  }
  const passcodeName = passcodeForm.value.name.trim() || undefined
  const idempotencyKey = createIdempotencyKey('passcode-create', target)
  passcodeSubmitting.value = true
  setActionLoading(target.roomId, 'PASSCODE_CREATE', true)
  try {
    const requestData: CreateRoomLockPasscodeRequest = {
      passcodeName,
      validFrom: passcodeForm.value.validFrom,
      validUntil: passcodeForm.value.validUntil,
      idempotencyKey,
    }
    if (!passcodeForm.value.generate) {
      requestData.passcode = passcodeForm.value.passcode.trim()
    }
    const response = await createRoomLockPasscode(target.roomId, requestData)
    if (!response.success) {
      ElMessage.error(getResponseMessage(response, 'messages.passcodeCreateFailed'))
      return
    }
    if (isFailedStatus(response.data?.status)) {
      ElMessage.error(getPasscodeFailureMessage(response.data, 'messages.passcodeCreateFailed'))
      return
    }
    const createdCode = response.data?.oneTimePasscode || ''
    const createdStatus = getNormalizedPasscodeStatus(response.data)
    if (isPasscodePendingStatus(createdStatus)) {
      ElMessage.success(lockT('messages.passcodeCreatePending'))
    } else if (createdCode) {
      ElMessage.success(lockT('messages.passcodeCreatedWithCode', { code: createdCode }))
    } else {
      ElMessage.success(lockT('messages.passcodeCreated'))
    }
    passcodeDialogVisible.value = false
    await loadPasscodes(target.roomId)
  } catch (error) {
    ElMessage.error(getErrorMessage(error, 'messages.passcodeCreateFailed'))
  } finally {
    passcodeSubmitting.value = false
    setActionLoading(target.roomId, 'PASSCODE_CREATE', false)
  }
}

const getPasscodeRecordId = (passcode: RoomLockPasscodeDTO) => {
  const recordId = Number(passcode.id || 0)
  return Number.isFinite(recordId) && recordId > 0 ? recordId : 0
}

const getPasscodeKey = (passcode: RoomLockPasscodeDTO) => {
  const recordId = getPasscodeRecordId(passcode)
  if (recordId) {
    return recordId
  }
  return `${passcode.providerTaskId || 'task'}-${passcode.validFrom || ''}-${passcode.validUntil || ''}`
}

const isDeleteBusy = (passcode: RoomLockPasscodeDTO) => {
  const roomId = currentRoomId.value
  const recordId = getPasscodeRecordId(passcode)
  if (!roomId || !recordId) {
    return false
  }
  return actionLoadingMap.value.get(getActionKey(roomId, `PASSCODE_DELETE_${recordId}`)) || false
}

const confirmDeletePasscode = async (passcode: RoomLockPasscodeDTO) => {
  const target = cloneCurrentTarget()
  const recordId = getPasscodeRecordId(passcode)
  if (!target || !recordId) {
    ElMessage.warning(lockT('messages.deleteMissing'))
    return
  }
  const cachedStatus = statusMap.value.get(target.roomId) || null
  if (!canUsePasscodeDeviceForStatus(cachedStatus)) {
    ElMessage.warning(getPasscodeUnavailableMessage(cachedStatus))
    return
  }
  if (passcode.roomId && Number(passcode.roomId) !== target.roomId) {
    ElMessage.warning(lockT('messages.passcodeRoomMismatch'))
    return
  }
  if (isDeleteBusy(passcode) || isPasscodeDeletePending(passcode)) {
    return
  }

  try {
    await ElMessageBox.confirm(
      lockT('dialogs.deletePasscode.message'),
      lockT('dialogs.deletePasscode.title'),
      {
        confirmButtonText: lockT('dialogs.deletePasscode.confirm'),
        cancelButtonText: lockT('actions.cancel'),
        type: 'warning',
      },
    )
  } catch (error) {
    return
  }

  const actionKey = `PASSCODE_DELETE_${recordId}`
  if (actionLoadingMap.value.get(getActionKey(target.roomId, actionKey))) {
    return
  }

  setActionLoading(target.roomId, actionKey, true)
  try {
    const response = await deleteRoomLockPasscode(recordId)
    if (!response.success) {
      ElMessage.error(getResponseMessage(response, 'messages.deleteFailed'))
      return
    }
    if (isFailedStatus(response.data?.status)) {
      ElMessage.error(getPasscodeFailureMessage(response.data, 'messages.deleteFailed'))
      await loadPasscodes(target.roomId)
      return
    }
    const deleteStatus = getNormalizedPasscodeStatus(response.data)
    if (isPasscodeDeletePendingStatus(deleteStatus) || isPasscodePendingStatus(deleteStatus)) {
      ElMessage.success(lockT('messages.deletePending'))
    } else {
      ElMessage.success(lockT('messages.deleteSuccess'))
    }
    await loadPasscodes(target.roomId)
  } catch (error) {
    ElMessage.error(getErrorMessage(error, 'messages.deleteFailed'))
  } finally {
    setActionLoading(target.roomId, actionKey, false)
  }
}

const hasLegacyControlStatus = (status: RoomLockStatusDTO | null) => {
  if (!status) {
    return false
  }
  return Boolean(
    status.deviceId ||
      status.providerLockId ||
      status.lockName ||
      status.provider ||
      status.lockStatus,
  )
}

const hasAnyLockBinding = (status: RoomLockStatusDTO | null) => {
  if (!status) {
    return false
  }
  return Boolean(
    status.bindingId ||
      status.controlAvailable ||
      status.passcodeAvailable ||
      status.controlDeviceId ||
      status.controlProviderLockId ||
      status.controlDeviceName ||
      status.controlLockName ||
      status.passcodeDeviceId ||
      status.passcodeProviderLockId ||
      status.passcodeDeviceName ||
      status.passcodeLockName ||
      hasLegacyControlStatus(status),
  )
}

const canUseControlDeviceForStatus = (status: RoomLockStatusDTO | null) => {
  if (!status) {
    return false
  }
  if (typeof status.controlAvailable === 'boolean') {
    return status.controlAvailable
  }
  return hasLegacyControlStatus(status)
}

const canUsePasscodeDeviceForStatus = (status: RoomLockStatusDTO | null) => {
  if (!status) {
    return false
  }
  if (typeof status.passcodeAvailable === 'boolean') {
    return status.passcodeAvailable
  }
  return hasLegacyControlStatus(status)
}

const getControlUnavailableMessage = (status: RoomLockStatusDTO | null) => {
  if (!hasAnyLockBinding(status)) {
    return lockT('messages.unboundRoom')
  }
  return lockT('messages.controlUnavailable')
}

const getPasscodeUnavailableMessage = (status: RoomLockStatusDTO | null) => {
  if (!hasAnyLockBinding(status)) {
    return lockT('messages.unboundRoom')
  }
  return lockT('messages.passcodeUnavailable')
}

const normalizeLockState = (status: RoomLockStatusDTO | null) => {
  if (!status) {
    return 'UNBOUND'
  }
  if (!hasAnyLockBinding(status)) {
    return 'UNBOUND'
  }
  if (!canUseControlDeviceForStatus(status)) {
    return 'CONTROL_MISSING'
  }
  const rawStatus = String(status.lockStatus || '').trim()
  if (!rawStatus) {
    return 'UNKNOWN'
  }

  const normalizedStatus = rawStatus.toLowerCase()
  if (
    normalizedStatus === 'unlock' ||
    normalizedStatus === 'unlocked' ||
    normalizedStatus === 'open' ||
    normalizedStatus === 'opened'
  ) {
    return 'UNLOCKED'
  }
  if (
    normalizedStatus === 'lock' ||
    normalizedStatus === 'locked' ||
    normalizedStatus === 'close' ||
    normalizedStatus === 'closed' ||
    normalizedStatus === 'latchboltlocked'
  ) {
    return 'LOCKED'
  }
  if (normalizedStatus.includes('jam')) {
    return 'JAMMED'
  }
  if (normalizedStatus.includes('unlock') || normalizedStatus.includes('open')) {
    return 'UNLOCKED'
  }
  if (normalizedStatus.includes('lock') || normalizedStatus.includes('close')) {
    return 'LOCKED'
  }

  return 'UNKNOWN'
}

const getStatusLabel = (status: RoomLockStatusDTO | null) => {
  const state = normalizeLockState(status)
  if (state === 'LOCKED') {
    return lockT('status.labels.locked')
  }
  if (state === 'UNLOCKED') {
    return lockT('status.labels.unlocked')
  }
  if (state === 'UNBOUND') {
    return lockT('status.labels.unbound')
  }
  if (state === 'CONTROL_MISSING') {
    return lockT('status.labels.controlMissing')
  }
  if (state === 'JAMMED') {
    return lockT('status.labels.jammed')
  }
  return lockT('status.labels.unknown')
}

const getStatusTagText = (status: RoomLockStatusDTO | null) => {
  const state = normalizeLockState(status)
  if (state === 'LOCKED') {
    return lockT('status.badges.safe')
  }
  if (state === 'UNLOCKED') {
    return lockT('status.badges.attention')
  }
  if (state === 'UNBOUND') {
    return lockT('status.badges.unbound')
  }
  if (state === 'CONTROL_MISSING') {
    return lockT('status.badges.controlMissing')
  }
  if (state === 'JAMMED') {
    return lockT('status.badges.abnormal')
  }
  return lockT('status.badges.unknown')
}

const getStatusTagType = (status: RoomLockStatusDTO | null) => {
  const state = normalizeLockState(status)
  if (state === 'LOCKED') {
    return 'success'
  }
  if (state === 'UNLOCKED') {
    return 'warning'
  }
  if (state === 'UNBOUND' || state === 'CONTROL_MISSING') {
    return 'info'
  }
  return 'danger'
}

const getActionSentMessage = (action: RoomLockOperationAction) => {
  if (action === 'UNLOCK') {
    return lockT('messages.actionSentUnlock')
  }
  return lockT('messages.actionSentLock')
}

const getActionConfirmedMessage = (action: RoomLockOperationAction) => {
  if (action === 'UNLOCK') {
    return lockT('polling.confirmedUnlocked')
  }
  return lockT('polling.confirmedLocked')
}

const getActionTimeoutMessage = (action: RoomLockOperationAction) => {
  if (action === 'UNLOCK') {
    return lockT('polling.timeoutUnlock')
  }
  return lockT('polling.timeoutLock')
}

const isExpectedActionState = (action: RoomLockOperationAction, state: string) => {
  if (action === 'UNLOCK') {
    return state === 'UNLOCKED'
  }
  return state === 'LOCKED'
}

const getStatusClass = (status: RoomLockStatusDTO | null) => {
  const state = normalizeLockState(status)
  return `status-${state.toLowerCase()}`
}

const getProviderText = (status: RoomLockStatusDTO | null) => {
  const provider = getProviderName(status?.controlProvider || status?.provider)
  return lockT('status.meta.provider', { provider })
}

const getProviderName = (provider?: SmartLockProvider) => {
  if (!provider) {
    return '-'
  }
  if (provider === 'SWITCHBOT') {
    return 'SwitchBot'
  }
  return 'TTLock'
}

const getRoleDeviceFallback = (available: boolean) => {
  return available ? lockT('roles.available') : lockT('roles.unavailable')
}

const getControlDeviceText = (status: RoomLockStatusDTO | null) => {
  const device =
    status?.controlDeviceName ||
    status?.controlLockName ||
    status?.lockName ||
    getRoleDeviceFallback(canUseControlDeviceForStatus(status))
  return lockT('status.meta.controlDevice', { device })
}

const getPasscodeDeviceText = (status: RoomLockStatusDTO | null) => {
  const device =
    status?.passcodeDeviceName ||
    status?.passcodeLockName ||
    getRoleDeviceFallback(canUsePasscodeDeviceForStatus(status))
  return lockT('status.meta.passcodeDevice', { device })
}

const getOnlineText = (status: RoomLockStatusDTO | null) => {
  let value = lockT('status.online.unknown')
  if (!status || status.online === undefined) {
    return lockT('status.meta.online', { value })
  }
  value = status.online ? lockT('status.online.yes') : lockT('status.online.no')
  return lockT('status.meta.online', { value })
}

const getBatteryText = (status: RoomLockStatusDTO | null) => {
  const battery = Number(status?.battery)
  let value = '-'
  if (!Number.isFinite(battery)) {
    return lockT('status.meta.battery', { value })
  }
  value = `${battery}%`
  return lockT('status.meta.battery', { value })
}

const getStatusUpdatedText = (status: RoomLockStatusDTO | null) => {
  return formatDateTime(status?.lastStatusAt || '')
}

const formatDateTime = (value: string | undefined) => {
  if (!value) {
    return ''
  }
  return String(value).replace('T', ' ').slice(0, 16)
}

const getPasscodeDisplay = (passcode: RoomLockPasscodeDTO) => {
  return passcode.oneTimePasscode || passcode.passcodeMasked || lockT('passcodes.generatedHidden')
}

const getPasscodeStatusText = (passcode: RoomLockPasscodeDTO) => {
  const status = getNormalizedPasscodeStatus(passcode)
  if (!status) {
    return lockT('passcodes.status.active')
  }
  if (status === 'ACTIVE' || status === 'SUCCESS') {
    return lockT('passcodes.status.active')
  }
  if (status === 'PENDING') {
    return lockT('passcodes.status.pending')
  }
  if (isPasscodeDeletePendingStatus(status)) {
    return lockT('passcodes.status.deletePending')
  }
  if (status === 'EXPIRED') {
    return lockT('passcodes.status.expired')
  }
  if (status === 'DELETED') {
    return lockT('passcodes.status.deleted')
  }
  if (status === 'FAILED') {
    return lockT('passcodes.status.failed')
  }
  return lockT('passcodes.status.unknown')
}

const getPasscodeValidityText = (passcode: RoomLockPasscodeDTO) => {
  const validFrom = formatDateTime(passcode.validFrom)
  const validUntil = formatDateTime(passcode.validUntil)
  if (!validFrom && !validUntil) {
    return lockT('passcodes.validityMissing')
  }
  return lockT('passcodes.validityRange', {
    from: validFrom || '-',
    until: validUntil || '-',
  })
}

watch(
  () => normalizedTarget.value,
  async (target) => {
    stopStatusPolling()
    if (!target) {
      return
    }
    await loadStatus(target.roomId, false)
    if (canUsePasscodeDeviceForStatus(statusMap.value.get(target.roomId) || null)) {
      await loadPasscodes(target.roomId)
    } else {
      setPasscodes(target.roomId, [])
      setPasscodeError(target.roomId, '')
    }
  },
  { immediate: true },
)

onBeforeUnmount(() => {
  stopStatusPolling()
})
</script>

<style scoped>
.room-lock-actions {
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  padding: 12px;
  background: #fff;
}

.room-lock-actions.compact {
  width: 100%;
  border-color: #eef0f3;
  background: #fbfcfe;
}

.lock-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}

.lock-title {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.lock-subtitle {
  margin-top: 2px;
  font-size: 12px;
  color: #606266;
}

.lock-status-panel {
  border-radius: 6px;
  padding: 10px;
  border: 1px solid #e5e7eb;
  background: #f8fafc;
}

.lock-status-panel.status-locked {
  border-color: #cdebd1;
  background: #f2fbf3;
}

.lock-status-panel.status-unlocked {
  border-color: #f5dab1;
  background: #fff8ed;
}

.lock-status-panel.status-unknown,
.lock-status-panel.status-unbound {
  border-color: #e5e7eb;
  background: #f8fafc;
}

.lock-status-panel.status-jammed {
  border-color: #f3b8b8;
  background: #fff5f5;
}

.status-main {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.status-label {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.status-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 12px;
  margin-top: 8px;
  font-size: 12px;
  color: #606266;
}

.status-error,
.passcode-error {
  margin-top: 8px;
  font-size: 12px;
  color: #c45656;
}

.status-hint {
  margin-top: 8px;
  font-size: 12px;
  color: #909399;
}

.status-polling {
  margin-top: 8px;
  font-size: 12px;
  color: #409eff;
}

.status-updated {
  margin-top: 8px;
  font-size: 12px;
  color: #909399;
}

.lock-action-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;
}

.lock-action-row .el-button {
  margin-left: 0;
}

.passcode-section {
  margin-top: 12px;
  border-top: 1px solid #eef0f3;
  padding-top: 10px;
}

.passcode-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
  font-size: 13px;
  font-weight: 600;
  color: #303133;
}

.passcode-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-height: 40px;
}

.passcode-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 8px 0;
  border-bottom: 1px solid #f0f2f5;
}

.passcode-item:last-child {
  border-bottom: none;
}

.passcode-info {
  min-width: 0;
  flex: 1;
}

.passcode-main {
  display: flex;
  align-items: center;
  gap: 8px;
}

.passcode-code {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  overflow-wrap: anywhere;
}

.passcode-meta,
.passcode-name {
  margin-top: 4px;
  font-size: 12px;
  color: #606266;
  overflow-wrap: anywhere;
}

.confirm-dialog-content {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.confirm-target {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 12px;
  font-size: 13px;
  color: #303133;
}

.passcode-form {
  padding-right: 8px;
}

.date-range-fields {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
}

.date-range-fields .el-date-editor {
  flex: 1 1 0;
  min-width: 0;
  width: 100%;
}

.field-tip {
  margin-top: 4px;
  font-size: 12px;
  color: #909399;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
