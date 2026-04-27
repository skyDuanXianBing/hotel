<template>
  <ion-page>
    <ion-header translucent>
      <ion-toolbar class="app-page-header__toolbar">
        <ion-buttons slot="start">
          <ion-back-button class="app-page-header__back-btn" :default-href="ROUTE_PATHS.settings" />
        </ion-buttons>
        <ion-title class="app-page-header__title">房型设置</ion-title>
        <ion-buttons slot="end">
          <ion-button class="app-page-header__text-btn" fill="clear" @click="handleCreateRoomType">新增</ion-button>
        </ion-buttons>
      </ion-toolbar>
    </ion-header>

    <ion-content fullscreen class="mobile-page settings-room-types-page">
      <ion-refresher slot="fixed" @ionRefresh="handleRefresh">
        <ion-refresher-content pulling-text="下拉刷新房型" refreshing-spinner="crescent" />
      </ion-refresher>

      <section class="mobile-hero settings-room-types-hero">
        <p class="mobile-note settings-room-types-hero__eyebrow">价格与商品</p>
        <h1 class="mobile-title">房型管理</h1>
        <div class="mobile-chip-row">
          <span class="mobile-chip">房型 {{ roomTypes.length }}</span>
          <span class="mobile-chip">房间 {{ totalRooms }}</span>
          <span class="mobile-chip">移动端管理</span>
        </div>
      </section>

      <div class="mobile-stack">
        <section class="mobile-card">
          <div class="mobile-inline-row settings-room-types-page__section-header">
            <div>
              <h2 class="mobile-section-title">房型列表</h2>
            </div>
            <ion-spinner v-if="loading" name="crescent" />
          </div>

          <div v-if="roomTypes.length > 0" class="mobile-list settings-room-types-list">
            <article v-for="roomType in roomTypes" :key="roomType.id" class="settings-room-type-card">
              <div class="settings-room-type-card__header">
                <div class="settings-room-type-card__title-group">
                  <strong>{{ roomType.name }}</strong>
                  <p class="settings-room-type-card__summary">
                    简称 {{ roomType.shortName }} · 最多 {{ roomType.maxGuests }} 人
                    <span v-if="roomType.maxChildOccupancy > 0"> · 儿童 {{ roomType.maxChildOccupancy }} 人</span>
                  </p>
                </div>
                <span class="settings-room-type-card__badge">{{ roomType.roomCount }} 间</span>
              </div>

              <div class="settings-room-type-card__meta">
                <span class="settings-room-type-card__meta-pill">{{ getBasePriceText(roomType.source) }}</span>
                <span v-if="roomType.sizeText" class="settings-room-type-card__meta-pill">
                  {{ roomType.sizeText }}
                </span>
                <span v-else-if="roomType.source.code" class="settings-room-type-card__meta-pill">
                  代码 {{ roomType.source.code }}
                </span>
              </div>

              <div class="settings-room-type-card__actions">
                <ion-button
                  size="small"
                  fill="solid"
                  class="settings-room-type-card__action settings-room-type-card__action--primary"
                  @click="handleOpenDetails(roomType)"
                >
                  详情
                </ion-button>
                <ion-button
                  size="small"
                  fill="outline"
                  class="settings-room-type-card__action"
                  @click="handleEditRoomType(roomType)"
                >
                  编辑
                </ion-button>
                <ion-button
                  size="small"
                  color="danger"
                  fill="clear"
                  class="settings-room-type-card__action settings-room-type-card__action--danger"
                  @click="handleDeleteRoomType(roomType)"
                >
                  删除
                </ion-button>
              </div>
            </article>
          </div>

          <div v-else-if="!loading" class="settings-room-types-page__empty-state">
            <p class="mobile-note settings-room-types-page__empty-text">
              当前暂无房型。
            </p>
            <ion-button @click="handleCreateRoomType">添加房型</ion-button>
          </div>
        </section>
      </div>

      <ion-modal :is-open="editorOpen" :backdrop-dismiss="!submitting" @didDismiss="handleEditorDidDismiss">
        <ion-header>
          <ion-toolbar>
            <ion-title>{{ editingRoomType ? '编辑房型' : '新增房型' }}</ion-title>
            <ion-buttons slot="end">
              <ion-button :disabled="submitting" @click="handleCloseEditor">关闭</ion-button>
            </ion-buttons>
          </ion-toolbar>
        </ion-header>

        <ion-content class="mobile-page settings-modal-page">
          <section class="mobile-card settings-editor-card">
            <div class="settings-form-section">
              <div>
                <h2 class="mobile-section-title">基本信息</h2>
              </div>

              <div class="settings-form-grid">
                <label class="settings-form-field">
                  <span>房型名称</span>
                  <ion-input
                    v-model="roomTypeForm.name"
                    :disabled="submitting"
                    fill="outline"
                    placeholder="请输入房型名称"
                  />
                </label>

                <label class="settings-form-field">
                  <span>简称</span>
                  <ion-input
                    v-model="roomTypeForm.shortName"
                    :disabled="submitting"
                    fill="outline"
                    placeholder="请输入简称"
                  />
                </label>

                <label class="settings-form-field">
                  <span>最大入住人数</span>
                  <ion-input
                    v-model="roomTypeForm.maxGuests"
                    :disabled="submitting"
                    fill="outline"
                    inputmode="numeric"
                    placeholder="至少 1 人"
                  />
                </label>

                <label class="settings-form-field">
                  <span>儿童最大入住</span>
                  <ion-input
                    v-model="roomTypeForm.maxChildOccupancy"
                    :disabled="submitting"
                    fill="outline"
                    inputmode="numeric"
                    placeholder="选填"
                  />
                </label>

                <label class="settings-form-field settings-form-field--full">
                  <span>房型地址</span>
                  <ion-textarea
                    v-model="roomTypeForm.roomTypeAddress"
                    :disabled="submitting"
                    :rows="3"
                    fill="outline"
                    placeholder="请输入房型地址"
                  />
                </label>

                <label class="settings-form-field settings-form-field--full">
                  <span>附近车站</span>
                  <ion-input
                    v-model="roomTypeForm.nearbyStation"
                    :disabled="submitting"
                    fill="outline"
                    placeholder="请输入附近车站"
                  />
                </label>

                <label class="settings-form-field settings-form-field--full">
                  <span>入住指南链接</span>
                  <ion-input
                    v-model="roomTypeForm.checkInGuideLink"
                    :disabled="submitting"
                    fill="outline"
                    placeholder="请输入入住指南链接"
                  />
                </label>

                <label class="settings-form-field settings-form-field--full">
                  <span>房型描述</span>
                  <ion-textarea
                    v-model="roomTypeForm.roomDescription"
                    :disabled="submitting"
                    :rows="4"
                    fill="outline"
                    placeholder="请输入房型描述"
                  />
                </label>

                <label class="settings-form-field">
                  <span>房型类型代码</span>
                  <ion-input
                    v-model="roomTypeForm.suRoomType"
                    :disabled="submitting"
                    fill="outline"
                    placeholder="例如 DOUBLE / TWIN"
                  />
                </label>

                <label class="settings-form-field">
                  <span>面积</span>
                  <ion-input
                    v-model="roomTypeForm.sizeMeasurement"
                    :disabled="submitting"
                    fill="outline"
                    inputmode="decimal"
                    placeholder="请输入面积"
                  />
                </label>

                <label class="settings-form-field">
                  <span>面积单位</span>
                  <ion-select
                    v-model="roomTypeForm.sizeMeasurementUnit"
                    :disabled="submitting"
                    fill="outline"
                    interface="action-sheet"
                    placeholder="请选择面积单位"
                  >
                    <ion-select-option value="">未设置</ion-select-option>
                    <ion-select-option
                      v-for="option in ROOM_TYPE_SIZE_UNIT_OPTIONS"
                      :key="option.value"
                      :value="option.value"
                    >
                      {{ option.label }}
                    </ion-select-option>
                  </ion-select>
                </label>
              </div>
            </div>

            <div class="settings-form-section">
              <div class="settings-section-heading">
                <div>
                  <h2 class="mobile-section-title">价格信息</h2>
                </div>
                <ion-button size="small" fill="outline" :disabled="submitting" @click="handleApplyDefaultPrice">
                  默认价填充全周
                </ion-button>
              </div>

              <div class="settings-form-grid settings-price-grid">
                <label class="settings-form-field settings-price-grid__full">
                  <span>默认价格</span>
                  <ion-input
                    v-model="roomTypeForm.defaultPrice"
                    :disabled="submitting"
                    fill="outline"
                    inputmode="decimal"
                    placeholder="请输入默认价格"
                  />
                </label>

                <label
                  v-for="field in ROOM_TYPE_DAILY_PRICE_FIELDS"
                  :key="field.key"
                  class="settings-form-field"
                >
                  <span>{{ field.label }}</span>
                  <ion-input
                    v-model="roomTypeForm[field.key]"
                    :disabled="submitting"
                    fill="outline"
                    inputmode="decimal"
                    placeholder="请输入价格"
                  />
                </label>
              </div>
            </div>

            <div class="settings-form-section">
              <div>
                <h2 class="mobile-section-title">设施与图片</h2>
              </div>

              <div class="settings-form-grid">
                <label class="settings-form-field settings-form-field--full">
                  <span>设施信息</span>
                  <ion-textarea
                    v-model="roomTypeForm.facilitiesText"
                    :disabled="submitting"
                    :rows="5"
                    fill="outline"
                    placeholder="示例：WiFi、窗户\n卫浴: 干湿分离"
                  />
                </label>

                <label class="settings-form-field settings-form-field--full">
                  <span>桌面图片链接</span>
                  <ion-textarea
                    v-model="roomTypeForm.desktopPhotoUrlsText"
                    :disabled="submitting"
                    :rows="4"
                    fill="outline"
                    placeholder="每行一个链接"
                  />
                </label>

                <label class="settings-form-field settings-form-field--full">
                  <span>移动图片链接</span>
                  <ion-textarea
                    v-model="roomTypeForm.mobilePhotoUrlsText"
                    :disabled="submitting"
                    :rows="4"
                    fill="outline"
                    placeholder="每行一个链接"
                  />
                </label>
              </div>
            </div>

            <div class="settings-form-section">
              <div class="settings-room-numbers__header">
                <div>
                  <h2 class="mobile-section-title">房间维护</h2>
                </div>
                <ion-button size="small" fill="outline" :disabled="submitting" @click="handleAddRoom">
                  新增房间
                </ion-button>
              </div>

              <div class="settings-room-numbers__list">
                <div
                  v-for="(room, index) in roomTypeForm.rooms"
                  :key="index"
                  class="settings-room-entry"
                >
                  <div class="settings-room-entry__header">
                    <strong>房间 {{ index + 1 }}</strong>
                    <ion-button
                      size="small"
                      color="danger"
                      fill="clear"
                      :disabled="submitting"
                      @click="handleRemoveRoom(index)"
                    >
                      删除
                    </ion-button>
                  </div>

                  <div class="settings-room-entry__fields">
                    <label class="settings-form-field">
                      <span>房间号</span>
                      <ion-input
                        v-model="roomTypeForm.rooms[index].roomNumber"
                        :disabled="submitting"
                        fill="outline"
                        placeholder="请输入房间号"
                      />
                    </label>

                    <label class="settings-form-field">
                      <span>房间密码（可选）</span>
                      <ion-input
                        v-model="roomTypeForm.rooms[index].smartlockPasscode"
                        :disabled="submitting"
                        fill="outline"
                        placeholder="请输入房间密码"
                      />
                    </label>
                  </div>
                </div>
              </div>

            </div>

            <div class="settings-form-actions">
              <ion-button
                v-if="editingRoomType"
                fill="outline"
                :disabled="submitting"
                @click="handleRestoreEditor"
              >
                恢复原值
              </ion-button>
              <ion-button fill="outline" :disabled="submitting" @click="handleCloseEditor">取消</ion-button>
              <ion-button :disabled="submitting" @click="handleSaveRoomType">
                {{ submitting ? '保存中...' : '保存房型' }}
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
  IonToolbar,
  onIonViewWillEnter,
} from '@ionic/vue'
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  createRoomType,
  deleteRoomType,
  getAllRoomTypesWithRooms,
  updateRoomType,
  type CreateRoomTypeRequest,
  type RoomTypeDeleteBlockInfo,
  type RoomTypeWithRoomsDTO,
} from '@/api/roomType'
import { buildSettingsRoomTypeDetailPath, ROUTE_PATHS } from '@/router/guards'
import { useRoomStatusStore } from '@/stores/roomStatus'
import { showSuccessToast, showWarningToast } from '@/utils/notify'
import {
  buildExistingRoomTypePayload,
  buildRoomTypeLocationSummary,
  buildLocalizedContent,
  buildRoomTypeCode,
  buildRoomTypePriceSummary,
  extractRoomEntries,
  extractRoomNumbers,
  formatFacilitiesText,
  formatUrlTextList,
  getBasePriceText,
  mergeRoomTypePhotoUrls,
  normalizeHttpUrl,
  normalizeUrlTextList,
  parseFacilitiesText,
  resolveRoomTypeLongDescription,
  resolveRoomTypeShortName,
  ROOM_TYPE_DAILY_PRICE_FIELDS,
  ROOM_TYPE_SIZE_UNIT_OPTIONS,
} from '@/utils/roomType'
import { isHandledRequestError } from '@/utils/request'

interface RoomTypeView {
  id: number
  name: string
  shortName: string
  roomDescription: string
  locationSummary: string
  maxGuests: number
  maxChildOccupancy: number
  roomCount: number
  roomNumbers: string[]
  sizeText: string
  facilityCount: number
  photoCount: number
  hasGuideLink: boolean
  priceSummary: string
  source: RoomTypeWithRoomsDTO
}

interface RoomTypeFormState {
  name: string
  shortName: string
  roomDescription: string
  maxGuests: string
  maxChildOccupancy: string
  roomTypeAddress: string
  nearbyStation: string
  checkInGuideLink: string
  suRoomType: string
  sizeMeasurement: string
  sizeMeasurementUnit: string
  defaultPrice: string
  mondayPrice: string
  tuesdayPrice: string
  wednesdayPrice: string
  thursdayPrice: string
  fridayPrice: string
  saturdayPrice: string
  sundayPrice: string
  facilitiesText: string
  desktopPhotoUrlsText: string
  mobilePhotoUrlsText: string
  rooms: RoomFormItem[]
}

interface RoomFormItem {
  roomNumber: string
  smartlockPasscode: string
}

const router = useRouter()
const roomStatusStore = useRoomStatusStore()

const loading = ref(false)
const submitting = ref(false)
const editorOpen = ref(false)
const roomTypes = ref<RoomTypeView[]>([])
const editingRoomType = ref<RoomTypeWithRoomsDTO | null>(null)
const editorSnapshot = ref<RoomTypeFormState | null>(null)
const roomTypeForm = ref<RoomTypeFormState>(createEmptyRoomTypeForm())

const totalRooms = computed(() => {
  let count = 0
  for (const roomType of roomTypes.value) {
    count += roomType.roomCount
  }
  return count
})

function createEmptyRoomTypeForm(): RoomTypeFormState {
  return {
    name: '',
    shortName: '',
    roomDescription: '',
    maxGuests: '2',
    maxChildOccupancy: '0',
    roomTypeAddress: '',
    nearbyStation: '',
    checkInGuideLink: '',
    suRoomType: '',
    sizeMeasurement: '',
    sizeMeasurementUnit: '',
    defaultPrice: '',
    mondayPrice: '',
    tuesdayPrice: '',
    wednesdayPrice: '',
    thursdayPrice: '',
    fridayPrice: '',
    saturdayPrice: '',
    sundayPrice: '',
    facilitiesText: '',
    desktopPhotoUrlsText: '',
    mobilePhotoUrlsText: '',
    rooms: [createEmptyRoomFormItem()],
  }
}

function createEmptyRoomFormItem(): RoomFormItem {
  return {
    roomNumber: '',
    smartlockPasscode: '',
  }
}

function cloneRoomTypeForm(form: RoomTypeFormState): RoomTypeFormState {
  return {
    ...form,
    rooms: form.rooms.map((room) => ({
      roomNumber: room.roomNumber,
      smartlockPasscode: room.smartlockPasscode,
    })),
  }
}

function resolveWarningMessage(error: unknown, fallbackMessage: string) {
  if (error instanceof Error && error.message) {
    return error.message
  }
  return fallbackMessage
}

function formatNumberText(value?: number) {
  if (value === undefined || value === null) {
    return ''
  }
  return String(value)
}

function formatSizeText(roomType: RoomTypeWithRoomsDTO) {
  if (roomType.sizeMeasurement === undefined || roomType.sizeMeasurement === null) {
    return ''
  }

  const unit = roomType.sizeMeasurementUnit?.trim()
  if (!unit) {
    return `面积 ${roomType.sizeMeasurement}`
  }

  return `面积 ${roomType.sizeMeasurement} ${unit}`
}

function createFormFromRoomType(roomType: RoomTypeWithRoomsDTO): RoomTypeFormState {
  const roomEntries = extractRoomEntries(roomType)
  const rooms = roomEntries.map((room) => ({
    roomNumber: room.roomNumber,
    smartlockPasscode: room.smartlockPasscode || '',
  }))

  return {
    name: roomType.name,
    shortName: resolveRoomTypeShortName(roomType),
    roomDescription: resolveRoomTypeLongDescription(roomType),
    maxGuests: formatNumberText(roomType.maxGuests || 1),
    maxChildOccupancy: formatNumberText(roomType.maxChildOccupancy ?? 0),
    roomTypeAddress: roomType.roomTypeAddress || '',
    nearbyStation: roomType.nearbyStation || '',
    checkInGuideLink: roomType.checkInGuideLink || '',
    suRoomType: roomType.suRoomType || '',
    sizeMeasurement: formatNumberText(roomType.sizeMeasurement),
    sizeMeasurementUnit: roomType.sizeMeasurementUnit || '',
    defaultPrice: formatNumberText(roomType.defaultPrice),
    mondayPrice: formatNumberText(roomType.mondayPrice),
    tuesdayPrice: formatNumberText(roomType.tuesdayPrice),
    wednesdayPrice: formatNumberText(roomType.wednesdayPrice),
    thursdayPrice: formatNumberText(roomType.thursdayPrice),
    fridayPrice: formatNumberText(roomType.fridayPrice),
    saturdayPrice: formatNumberText(roomType.saturdayPrice),
    sundayPrice: formatNumberText(roomType.sundayPrice),
    facilitiesText: formatFacilitiesText(roomType.facilities),
    desktopPhotoUrlsText: formatUrlTextList(roomType.desktopPhotoUrls),
    mobilePhotoUrlsText: formatUrlTextList(roomType.mobilePhotoUrls),
    rooms: rooms.length > 0 ? rooms : [createEmptyRoomFormItem()],
  }
}

function buildRoomTypeView(roomType: RoomTypeWithRoomsDTO): RoomTypeView {
  const roomNumbers = extractRoomNumbers(roomType)

  return {
    id: roomType.id,
    name: roomType.name,
    shortName: resolveRoomTypeShortName(roomType),
    roomDescription: resolveRoomTypeLongDescription(roomType),
    locationSummary: buildRoomTypeLocationSummary(roomType),
    maxGuests: roomType.maxGuests || 1,
    maxChildOccupancy: roomType.maxChildOccupancy ?? 0,
    roomCount: roomType.totalRooms || roomNumbers.length,
    roomNumbers,
    sizeText: formatSizeText(roomType),
    facilityCount: roomType.facilities?.length || 0,
    photoCount: mergeRoomTypePhotoUrls(roomType).length,
    hasGuideLink: Boolean(roomType.checkInGuideLink),
    priceSummary: buildRoomTypePriceSummary(roomType),
    source: roomType,
  }
}

function resetEditorState() {
  editingRoomType.value = null
  editorSnapshot.value = null
  roomTypeForm.value = createEmptyRoomTypeForm()
}

function openEditor(form: RoomTypeFormState, roomType: RoomTypeWithRoomsDTO | null) {
  editingRoomType.value = roomType
  roomTypeForm.value = cloneRoomTypeForm(form)
  editorSnapshot.value = cloneRoomTypeForm(form)
  editorOpen.value = true
}

function resolveSaveFailureMessage(message: string) {
  const normalizedMessage = message.toLowerCase()

  if (
    message.includes('房间号') ||
    normalizedMessage.includes('room number') ||
    normalizedMessage.includes('roomnumber')
  ) {
    return `房间号冲突：${message}`
  }

  if (
    message.includes('房型名称') ||
    (normalizedMessage.includes('name') &&
      (normalizedMessage.includes('exist') || normalizedMessage.includes('duplicate')))
  ) {
    return '房型名称已存在，请更换后再保存'
  }

  if (normalizedMessage.includes('maxchildoccupancy') || message.includes('儿童')) {
    return '儿童最大入住人数需在 0 到最大入住人数之间'
  }

  if (
    normalizedMessage.includes('sizemeasurementunit') ||
    message.includes('面积单位') ||
    normalizedMessage.includes('sqm') ||
    normalizedMessage.includes('sqft')
  ) {
    return '面积单位仅支持 sqm 或 sqft'
  }

  if (
    message.includes('房型代码') ||
    (normalizedMessage.includes('code') &&
      (normalizedMessage.includes('conflict') || normalizedMessage.includes('exist')))
  ) {
    return '房型代码冲突，请调整简称后重试'
  }

  return message
}

async function presentAlert(header: string, message: string) {
  const alert = await alertController.create({
    header,
    message,
    buttons: ['知道了'],
  })

  await alert.present()
  await alert.onDidDismiss()
}

async function confirmDeleteRoomType(name: string) {
  const alert = await alertController.create({
    header: '删除房型',
    message: `确认删除 ${name} 吗？`,
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

function buildDeleteFailureMessage(message: string, blockInfo: RoomTypeDeleteBlockInfo | null) {
  if (!blockInfo || blockInfo.totalBlockingReservations <= 0) {
    return message || '房型删除失败'
  }

  const sampleTexts: string[] = []
  for (const item of blockInfo.sample || []) {
    const orderNumber = item.orderNumber || '-'
    const status = item.status || '-'
    const roomNumber = item.roomNumber || '-'
    const dateRange = `${item.checkInDate || '-'} ~ ${item.checkOutDate || '-'}`
    sampleTexts.push(`订单 ${orderNumber}（${status}）/ 房间 ${roomNumber} / ${dateRange}`)
  }

  let result = message || '房型删除失败'
  result += `。当前共有 ${blockInfo.totalBlockingReservations} 笔订单阻塞删除`
  if (sampleTexts.length > 0) {
    result += `：${sampleTexts.join('；')}`
  }
  result += '。请先处理相关订单后再重试。'
  return result
}

async function syncRoomStatus() {
  try {
    await roomStatusStore.refreshAll()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, '房态刷新失败，可稍后在房态页下拉重试'))
    }
  }
}

async function loadRoomTypes() {
  loading.value = true
  try {
    const response = await getAllRoomTypesWithRooms()
    if (!response.success || !response.data) {
      throw new Error(response.message || '加载房型失败')
    }

    const nextRoomTypes: RoomTypeView[] = []
    for (const item of response.data) {
      nextRoomTypes.push(buildRoomTypeView(item))
    }

    roomTypes.value = nextRoomTypes
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, '加载房型失败'))
    }
  } finally {
    loading.value = false
  }
}

function handleCreateRoomType() {
  openEditor(createEmptyRoomTypeForm(), null)
}

function handleEditRoomType(roomType: RoomTypeView) {
  const nextForm = createFormFromRoomType(roomType.source)
  openEditor(nextForm, roomType.source)
}

function handleRestoreEditor() {
  if (!editorSnapshot.value) {
    return
  }
  roomTypeForm.value = cloneRoomTypeForm(editorSnapshot.value)
  showSuccessToast('已恢复为打开编辑时的原始内容')
}

function handleEditorDidDismiss() {
  if (editorOpen.value) {
    return
  }
  resetEditorState()
}

function handleCloseEditor() {
  if (submitting.value) {
    return
  }
  editorOpen.value = false
  resetEditorState()
}

function handleAddRoom() {
  roomTypeForm.value.rooms.push(createEmptyRoomFormItem())
}

function handleRemoveRoom(index: number) {
  if (roomTypeForm.value.rooms.length <= 1) {
    roomTypeForm.value.rooms[0] = createEmptyRoomFormItem()
    return
  }

  roomTypeForm.value.rooms.splice(index, 1)
}

function handleApplyDefaultPrice() {
  if (!roomTypeForm.value.defaultPrice.trim()) {
    showWarningToast('请先填写默认价格')
    return
  }

  for (const field of ROOM_TYPE_DAILY_PRICE_FIELDS) {
    roomTypeForm.value[field.key] = roomTypeForm.value.defaultPrice
  }

  showSuccessToast('已用默认价填充周一到周日')
}

function parseRequiredPositiveInteger(rawValue: string, fieldLabel: string) {
  const value = Number(rawValue)
  if (!Number.isInteger(value) || value < 1) {
    showWarningToast(`${fieldLabel}需填写大于等于 1 的整数`)
    return null
  }
  return value
}

function parseOptionalNonNegativeInteger(rawValue: string, fieldLabel: string) {
  const trimmedValue = rawValue.trim()
  if (!trimmedValue) {
    return undefined
  }

  const value = Number(trimmedValue)
  if (!Number.isInteger(value) || value < 0) {
    showWarningToast(`${fieldLabel}需填写大于等于 0 的整数`)
    return null
  }
  return value
}

function parseOptionalNonNegativeNumber(rawValue: string, fieldLabel: string) {
  const trimmedValue = rawValue.trim()
  if (!trimmedValue) {
    return undefined
  }

  const value = Number(trimmedValue)
  if (!Number.isFinite(value) || value < 0) {
    showWarningToast(`${fieldLabel}需填写大于等于 0 的数字`)
    return null
  }

  return Number(value.toFixed(2))
}

function buildValidatedRooms() {
  if (roomTypeForm.value.rooms.length === 0) {
    showWarningToast('请至少输入一个房间')
    return null
  }

  const rooms: Array<{ roomNumber: string; smartlockPasscode?: string }> = []
  const normalizedRoomNumbers = new Set<string>()

  for (let index = 0; index < roomTypeForm.value.rooms.length; index += 1) {
    const room = roomTypeForm.value.rooms[index]
    const trimmedRoomNumber = room.roomNumber.trim()
    if (!trimmedRoomNumber) {
      showWarningToast(`第 ${index + 1} 个房间号不能为空`)
      return null
    }

    const normalizedRoomNumber = trimmedRoomNumber.toUpperCase()
    if (normalizedRoomNumbers.has(normalizedRoomNumber)) {
      showWarningToast(`房间号 ${trimmedRoomNumber} 在当前房型内重复，请检查后重试`)
      return null
    }

    normalizedRoomNumbers.add(normalizedRoomNumber)
    const trimmedSmartlockPasscode = room.smartlockPasscode.trim()
    rooms.push({
      roomNumber: trimmedRoomNumber,
      smartlockPasscode: trimmedSmartlockPasscode || undefined,
    })
  }

  const occupiedRoomMap = new Map<string, RoomTypeView>()
  for (const roomType of roomTypes.value) {
    if (editingRoomType.value && roomType.id === editingRoomType.value.id) {
      continue
    }

    for (const roomNumber of roomType.roomNumbers) {
      occupiedRoomMap.set(roomNumber.trim().toUpperCase(), roomType)
    }
  }

  for (const room of rooms) {
    const roomNumber = room.roomNumber
    const conflictRoomType = occupiedRoomMap.get(roomNumber.toUpperCase())
    if (conflictRoomType) {
      showWarningToast(
        `房间号 ${roomNumber} 已存在于房型 ${conflictRoomType.name}（${conflictRoomType.shortName}），请更换后再保存`,
      )
      return null
    }
  }

  return rooms
}

function validateNameConflict(name: string) {
  for (const roomType of roomTypes.value) {
    if (editingRoomType.value && roomType.id === editingRoomType.value.id) {
      continue
    }

    if (roomType.name.trim() === name) {
      showWarningToast(`房型名称 ${name} 已存在，请更换后再保存`)
      return false
    }
  }

  return true
}

function normalizeOptionalText(rawValue: string) {
  const trimmedValue = rawValue.trim()
  if (!trimmedValue) {
    return undefined
  }

  return trimmedValue
}

function buildValidatedPayload() {
  const name = roomTypeForm.value.name.trim()
  if (!name) {
    showWarningToast('请输入房型名称')
    return null
  }

  const shortName = roomTypeForm.value.shortName.trim()
  if (!shortName) {
    showWarningToast('请输入简称')
    return null
  }

  if (!validateNameConflict(name)) {
    return null
  }

  const maxGuests = parseRequiredPositiveInteger(roomTypeForm.value.maxGuests, '最大入住人数')
  if (maxGuests === null) {
    return null
  }

  const maxChildOccupancy = parseOptionalNonNegativeInteger(
    roomTypeForm.value.maxChildOccupancy,
    '儿童最大入住',
  )
  if (maxChildOccupancy === null) {
    return null
  }
  if (maxChildOccupancy !== undefined && maxChildOccupancy > maxGuests) {
    showWarningToast('儿童最大入住人数不能超过最大入住人数')
    return null
  }

  const sizeMeasurement = parseOptionalNonNegativeNumber(roomTypeForm.value.sizeMeasurement, '面积')
  if (sizeMeasurement === null) {
    return null
  }

  const sizeMeasurementUnit = roomTypeForm.value.sizeMeasurementUnit.trim()
  if (sizeMeasurement !== undefined && sizeMeasurementUnit !== 'sqm' && sizeMeasurementUnit !== 'sqft') {
    showWarningToast('面积单位仅支持 sqm 或 sqft')
    return null
  }

  const defaultPrice = parseOptionalNonNegativeNumber(roomTypeForm.value.defaultPrice, '默认价格')
  if (defaultPrice === null) {
    return null
  }

  const mondayPrice = parseOptionalNonNegativeNumber(roomTypeForm.value.mondayPrice, '周一价格')
  if (mondayPrice === null) {
    return null
  }
  const tuesdayPrice = parseOptionalNonNegativeNumber(roomTypeForm.value.tuesdayPrice, '周二价格')
  if (tuesdayPrice === null) {
    return null
  }
  const wednesdayPrice = parseOptionalNonNegativeNumber(roomTypeForm.value.wednesdayPrice, '周三价格')
  if (wednesdayPrice === null) {
    return null
  }
  const thursdayPrice = parseOptionalNonNegativeNumber(roomTypeForm.value.thursdayPrice, '周四价格')
  if (thursdayPrice === null) {
    return null
  }
  const fridayPrice = parseOptionalNonNegativeNumber(roomTypeForm.value.fridayPrice, '周五价格')
  if (fridayPrice === null) {
    return null
  }
  const saturdayPrice = parseOptionalNonNegativeNumber(roomTypeForm.value.saturdayPrice, '周六价格')
  if (saturdayPrice === null) {
    return null
  }
  const sundayPrice = parseOptionalNonNegativeNumber(roomTypeForm.value.sundayPrice, '周日价格')
  if (sundayPrice === null) {
    return null
  }

  const normalizedGuideLink = normalizeHttpUrl(roomTypeForm.value.checkInGuideLink)
  if (roomTypeForm.value.checkInGuideLink.trim() && !normalizedGuideLink) {
    showWarningToast('入住指南链接仅支持 http:// 或 https://')
    return null
  }

  const desktopPhotoResult = normalizeUrlTextList(roomTypeForm.value.desktopPhotoUrlsText)
  if (desktopPhotoResult.invalidValue) {
    showWarningToast(`桌面图片链接格式不正确：${desktopPhotoResult.invalidValue}`)
    return null
  }

  const mobilePhotoResult = normalizeUrlTextList(roomTypeForm.value.mobilePhotoUrlsText)
  if (mobilePhotoResult.invalidValue) {
    showWarningToast(`移动图片链接格式不正确：${mobilePhotoResult.invalidValue}`)
    return null
  }

  const rooms = buildValidatedRooms()
  if (!rooms) {
    return null
  }

  const roomNumbers = rooms.map((room) => room.roomNumber)

  const code = buildRoomTypeCode(shortName, editingRoomType.value?.code)
  if (!code) {
    showWarningToast('简称不可为空')
    return null
  }

  const existingPayload = editingRoomType.value ? buildExistingRoomTypePayload(editingRoomType.value) : null
  const roomDescription = roomTypeForm.value.roomDescription.trim()
  const payload: CreateRoomTypeRequest = {
    ...(existingPayload || {}),
    name,
    code,
    description: shortName,
    totalRooms: rooms.length,
    maxGuests,
    maxChildOccupancy,
    roomTypeAddress: normalizeOptionalText(roomTypeForm.value.roomTypeAddress),
    nearbyStation: normalizeOptionalText(roomTypeForm.value.nearbyStation),
    checkInGuideLink: normalizedGuideLink || undefined,
    suRoomType: roomTypeForm.value.suRoomType.trim() || undefined,
    sizeMeasurement,
    sizeMeasurementUnit: sizeMeasurement !== undefined ? sizeMeasurementUnit : undefined,
    defaultPrice: defaultPrice ?? mondayPrice ?? existingPayload?.defaultPrice,
    mondayPrice,
    tuesdayPrice,
    wednesdayPrice,
    thursdayPrice,
    fridayPrice,
    saturdayPrice,
    sundayPrice,
    rooms,
    roomNumbers,
    facilities: parseFacilitiesText(roomTypeForm.value.facilitiesText),
    desktopPhotoUrls: desktopPhotoResult.urls,
    mobilePhotoUrls: mobilePhotoResult.urls,
    localizedContent: buildLocalizedContent(existingPayload?.localizedContent, name, roomDescription),
  }

  return payload
}

async function handleSaveRoomType() {
  const payload = buildValidatedPayload()
  if (!payload) {
    return
  }

  submitting.value = true
  try {
    if (editingRoomType.value) {
      const response = await updateRoomType(editingRoomType.value.id, payload)
      if (!response.success) {
        throw new Error(resolveSaveFailureMessage(response.message || '更新房型失败'))
      }
      showSuccessToast('房型已更新')
    } else {
      const response = await createRoomType(payload)
      if (!response.success) {
        throw new Error(resolveSaveFailureMessage(response.message || '创建房型失败'))
      }
      showSuccessToast('房型已创建')
    }

    editorOpen.value = false
    resetEditorState()
    await loadRoomTypes()
    await syncRoomStatus()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      const fallbackMessage = editingRoomType.value ? '更新房型失败' : '创建房型失败'
      const message = resolveWarningMessage(error, fallbackMessage)
      showWarningToast(resolveSaveFailureMessage(message))
    }
  } finally {
    submitting.value = false
  }
}

async function handleDeleteRoomType(roomType: RoomTypeView) {
  const confirmed = await confirmDeleteRoomType(roomType.name)
  if (!confirmed) {
    return
  }

  try {
    const response = await deleteRoomType(roomType.id)
    if (!response.success) {
      const message = buildDeleteFailureMessage(response.message || '删除房型失败', response.data)
      await presentAlert('无法删除房型', message)
      return
    }

    showSuccessToast('房型已删除')
    await loadRoomTypes()
    await syncRoomStatus()
  } catch (error) {
    const message = resolveWarningMessage(error, '删除房型失败')
    await presentAlert('删除失败', message)
  }
}

async function handleOpenDetails(roomType: RoomTypeView) {
  await router.push(buildSettingsRoomTypeDetailPath(roomType.id))
}

async function handleRefresh(event: CustomEvent) {
  try {
    await loadRoomTypes()
  } finally {
    event.detail.complete()
  }
}

onIonViewWillEnter(async () => {
  await loadRoomTypes()
})
</script>

<style scoped>
.settings-room-types-page {
  display: block;
}

.settings-room-types-hero {
  margin-top: 4px;
}

.settings-room-types-hero__eyebrow {
  color: var(--ion-color-primary);
  font-weight: 700;
}

.settings-room-types-page > .mobile-stack {
  margin-top: var(--ios-pms-space-4);
}

.settings-room-types-page__section-header {
  align-items: flex-start;
}

.settings-room-types-list {
  margin-top: 16px;
  gap: 12px;
}

.settings-room-type-card {
  position: relative;
  overflow: hidden;
  padding: 16px;
  border-radius: 20px;
  border: 1px solid rgba(116, 138, 185, 0.12);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.99), rgba(247, 250, 255, 0.94));
  box-shadow:
    0 14px 28px rgba(90, 111, 153, 0.06),
    inset 0 1px 0 rgba(255, 255, 255, 0.92);
}

.settings-room-type-card::before {
  content: '';
  position: absolute;
  inset: 0 0 auto;
  height: 72px;
  background: linear-gradient(135deg, rgba(63, 124, 255, 0.08), rgba(255, 255, 255, 0));
  pointer-events: none;
}

.settings-room-type-card__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
  position: relative;
  z-index: 1;
}

.settings-room-type-card__title-group {
  min-width: 0;
  display: grid;
  gap: 6px;
}

.settings-room-type-card__header strong,
.settings-room-type-card__summary {
  margin: 0;
}

.settings-room-type-card__header strong {
  color: var(--ios-pms-text-primary);
  font-size: 18px;
  font-weight: var(--ios-pms-weight-heavy);
  line-height: 1.1;
  letter-spacing: -0.03em;
}

.settings-room-type-card__summary {
  color: var(--ios-pms-text-muted);
  font-size: 12px;
  line-height: 1.5;
}

.settings-room-type-card__badge {
  display: inline-flex;
  flex: none;
  align-items: center;
  min-height: 28px;
  padding: 0 10px;
  border-radius: 999px;
  border: 1px solid rgba(116, 163, 251, 0.12);
  background: rgba(115, 164, 255, 0.09);
  color: var(--ios-pms-primary-strong);
  font-size: 11px;
  font-weight: var(--ios-pms-weight-bold);
  letter-spacing: 0.01em;
}

.settings-room-type-card__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 14px;
  position: relative;
  z-index: 1;
}

.settings-room-type-card__meta-pill {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 10px;
  border-radius: 999px;
  border: 1px solid rgba(116, 138, 185, 0.1);
  background: rgba(243, 247, 255, 0.92);
  color: var(--ios-pms-text-secondary);
  font-size: 11px;
  font-weight: var(--ios-pms-weight-medium);
  line-height: 1;
}

.settings-room-type-card__actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px solid rgba(116, 138, 185, 0.08);
  position: relative;
  z-index: 1;
}

.settings-room-type-card__action {
  margin: 0;
  min-height: 34px;
  --padding-start: 14px;
  --padding-end: 14px;
  --padding-top: 0;
  --padding-bottom: 0;
  --border-radius: 999px;
  --box-shadow: none;
  font-size: 12px;
  font-weight: var(--ios-pms-weight-bold);
  letter-spacing: 0.01em;
}

.settings-room-type-card__action--primary {
  --background: rgba(52, 116, 246, 0.1);
  --background-hover: rgba(52, 116, 246, 0.14);
  --background-activated: rgba(52, 116, 246, 0.16);
  --color: var(--ios-pms-primary-strong);
}

.settings-room-type-card__action[fill='outline'] {
  --background: rgba(255, 255, 255, 0.82);
  --color: var(--ios-pms-text-secondary);
  --border-color: rgba(116, 138, 185, 0.14);
}

.settings-room-type-card__action--danger {
  --color: #de5c5c;
}

.settings-room-types-page__empty-state {
  display: grid;
  gap: 12px;
  justify-items: flex-start;
  padding-top: 16px;
}

.settings-room-types-page__empty-text {
  margin: 0;
}

.settings-modal-page {
  --padding-top: 16px;
  --padding-bottom: 24px;
  --padding-start: 16px;
  --padding-end: 16px;
}

.settings-editor-card,
.settings-form-section,
.settings-form-grid {
  display: grid;
}

.settings-editor-card {
  gap: 0;
}

.settings-form-section {
  gap: 14px;
}

.settings-form-section + .settings-form-section,
.settings-form-section + .settings-form-actions {
  border-top: 1px solid var(--app-border);
  margin-top: 12px;
  padding-top: 16px;
}

.settings-form-grid {
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

.settings-form-field--full,
.settings-price-grid__full {
  grid-column: 1 / -1;
}

.settings-section-heading,
.settings-room-numbers__header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}

.settings-price-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.settings-room-numbers__list {
  display: grid;
  gap: 10px;
}

.settings-room-entry {
  display: grid;
  gap: 12px;
  padding: 12px;
  border: 1px solid var(--app-border);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.88);
}

.settings-room-entry__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.settings-room-entry__header strong {
  color: var(--app-heading);
  font-size: 14px;
}

.settings-room-entry__fields {
  display: grid;
  gap: 12px;
}

.settings-room-numbers__hint {
  margin: 0;
}

.settings-form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  flex-wrap: wrap;
}

@media (max-width: 680px) {
  .settings-price-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .settings-section-heading,
  .settings-room-numbers__header,
  .settings-room-entry__header {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
