<template>
  <ion-page>
    <ion-header translucent>
      <ion-toolbar class="app-page-header__toolbar">
        <ion-buttons slot="start">
          <ion-back-button class="app-page-header__back-btn" :default-href="ROUTE_PATHS.settings" />
        </ion-buttons>
        <ion-title class="app-page-header__title">{{ $t('routes.SettingsRoomTypes') }}</ion-title>
        <ion-buttons slot="end">
          <ion-button class="app-page-header__text-btn" fill="clear" @click="handleCreateRoomType">{{ $t('settingsStage4.roomGroup.addGroup') }}</ion-button>
        </ion-buttons>
      </ion-toolbar>
    </ion-header>

    <ion-content fullscreen class="mobile-page settings-room-types-page">
      <ion-refresher slot="fixed" @ionRefresh="handleRefresh">
        <ion-refresher-content :pulling-text="$t('stage5UiAttributes.6')" refreshing-spinner="crescent" />
      </ion-refresher>

      <section class="mobile-hero settings-room-types-hero">
        <p class="mobile-note settings-room-types-hero__eyebrow">{{ $t('stage5SourceText.5') }}</p>
        <h1 class="mobile-title">{{ $t('routes.SettingsRoomTypes') }}</h1>
        <div class="mobile-chip-row">
          <span class="mobile-chip">{{ $t('accommodation.common.roomType') }} {{ roomTypes.length }}</span>
          <span class="mobile-chip">{{ $t('accommodation.common.room') }} {{ totalRooms }}</span>
          <span class="mobile-chip">{{ $t('stage5SourceText.173') }}</span>
        </div>
      </section>

      <div class="mobile-stack">
        <section class="mobile-card">
          <div class="mobile-inline-row settings-room-types-page__section-header">
            <div>
              <h2 class="mobile-section-title">{{ $t('stage5SourceText.101') }}</h2>
            </div>
            <ion-spinner v-if="loading" name="crescent" />
          </div>

          <div v-if="roomTypes.length > 0" class="mobile-list settings-room-types-list">
            <article v-for="roomType in roomTypes" :key="roomType.id" class="settings-room-type-card">
              <div class="settings-room-type-card__header">
                <div class="settings-room-type-card__title-group">
                  <strong>{{ roomType.name }}</strong>
                  <p class="settings-room-type-card__summary">
                    {{ $t('settingsStage4.roomSettings.columns.shortName') }} {{ roomType.shortName }} {{ $t('stage5DynamicUi.85') }} {{ roomType.maxGuests }} {{ $t('settingsStage4.roomTypeManagement.editor.units.people') }}
                    <span v-if="roomType.maxChildOccupancy > 0">
                      {{ $t('stage5DynamicUi.81') }} {{ roomType.maxChildOccupancy }} {{ $t('settingsStage4.roomTypeManagement.editor.units.people') }}
                    </span>
                  </p>
                </div>
                <span class="settings-room-type-card__badge">{{ roomType.roomCount }} {{ $t('settingsStage4.common.unitRooms') }}</span>
              </div>

              <div class="settings-room-type-card__meta">
                  <span class="settings-room-type-card__meta-pill">{{ getBasePriceText(roomType.source, roomTypeMoneyOptions) }}</span>
                <span v-if="roomType.sizeText" class="settings-room-type-card__meta-pill">
                  {{ roomType.sizeText }}
                </span>
                <span v-else-if="roomType.source.code" class="settings-room-type-card__meta-pill">
                  {{ $t('stage5DynamicUi.89') }} {{ roomType.source.code }}
                </span>
              </div>

              <div class="settings-room-type-card__actions">
                <ion-button
                  size="small"
                  fill="solid"
                  class="settings-room-type-card__action settings-room-type-card__action--primary"
                  @click="handleOpenDetails(roomType)"
                >
                  {{ $t('accommodation.cleaning.detail') }}
                </ion-button>
                <ion-button
                  size="small"
                  fill="outline"
                  class="settings-room-type-card__action"
                  @click="handleEditRoomType(roomType)"
                >
                  {{ $t('accommodation.roomPrice.editTitle') }}
                </ion-button>
                <ion-button
                  size="small"
                  color="danger"
                  fill="clear"
                  class="settings-room-type-card__action settings-room-type-card__action--danger"
                  @click="handleDeleteRoomType(roomType)"
                >
                  {{ $t('roomStatus.roomLock.actions.delete') }}
                </ion-button>
              </div>
            </article>
          </div>

          <div v-else-if="!loading" class="settings-room-types-page__empty-state">
            <p class="mobile-note settings-room-types-page__empty-text">
              {{ $t('stage5VisibleText.167') }}
            </p>
            <ion-button @click="handleCreateRoomType">{{ $t('roomStatus.common.addRoomType') }}</ion-button>
          </div>
        </section>
      </div>

      <ion-modal :is-open="editorOpen" :backdrop-dismiss="!submitting" @didDismiss="handleEditorDidDismiss">
        <ion-header>
          <ion-toolbar>
            <ion-title>{{ editingRoomType ? $t('settingsStage4.roomSettings.dialog.editTitle') : $t('settingsStage4.roomSettings.dialog.addTitle') }}</ion-title>
            <ion-buttons slot="end">
              <ion-button :disabled="submitting" @click="handleCloseEditor">{{ $t('home.section.close') }}</ion-button>
            </ion-buttons>
          </ion-toolbar>
        </ion-header>

        <ion-content class="mobile-page settings-modal-page">
          <section class="mobile-card settings-editor-card">
            <div class="settings-form-section">
              <div>
                <h2 class="mobile-section-title">{{ $t('accommodation.cleaning.basicInfo') }}</h2>
              </div>

              <div class="settings-form-grid">
                <label class="settings-form-field">
                  <span>{{ $t('accommodation.roomTable.columns.roomTypeName') }}</span>
                  <ion-input
                    v-model="roomTypeForm.name"
                    :disabled="submitting"
                    fill="outline"
                    :placeholder="$t('settingsStage4.roomSettings.placeholders.roomTypeName')"
                  />
                </label>

                <label class="settings-form-field">
                  <span>{{ $t('settingsStage4.roomSettings.columns.shortName') }}</span>
                  <ion-input
                    v-model="roomTypeForm.shortName"
                    :disabled="submitting"
                    fill="outline"
                    :placeholder="$t('settingsStage4.roomSettings.placeholders.shortName')"
                  />
                </label>

                <label class="settings-form-field">
                  <span>{{ $t('settingsStage4.roomSettings.fields.maxGuests') }}</span>
                  <ion-input
                    v-model="roomTypeForm.maxGuests"
                    :disabled="submitting"
                    fill="outline"
                    inputmode="numeric"
                    :placeholder="$t('stage5UiAttributes.59')"
                  />
                </label>

                <label class="settings-form-field">
                  <span>{{ $t('settingsStage4.roomTypeDetails.fields.maxChildren') }}</span>
                  <ion-input
                    v-model="roomTypeForm.maxChildOccupancy"
                    :disabled="submitting"
                    fill="outline"
                    inputmode="numeric"
                    :placeholder="$t('stage5UiAttributes.109')"
                  />
                </label>

                <label class="settings-form-field settings-form-field--full">
                  <span>{{ $t('settingsStage4.roomSettings.fields.roomTypeAddress') }}</span>
                  <ion-textarea
                    v-model="roomTypeForm.roomTypeAddress"
                    :disabled="submitting"
                    :rows="3"
                    fill="outline"
                    :placeholder="$t('stage5UiAttributes.77')"
                  />
                </label>

                <label class="settings-form-field settings-form-field--full">
                  <span>{{ $t('settingsStage4.roomSettings.fields.nearbyStation') }}</span>
                  <ion-input
                    v-model="roomTypeForm.nearbyStation"
                    :disabled="submitting"
                    fill="outline"
                    :placeholder="$t('stage5UiAttributes.103')"
                  />
                </label>

                <label class="settings-form-field settings-form-field--full">
                  <span>{{ $t('settingsStage4.roomSettings.fields.checkInGuideLink') }}</span>
                  <ion-input
                    v-model="roomTypeForm.checkInGuideLink"
                    :disabled="submitting"
                    fill="outline"
                    :placeholder="$t('stage5UiAttributes.63')"
                  />
                </label>

                <label class="settings-form-field settings-form-field--full">
                  <span>{{ $t('stage5SourceText.102') }}</span>
                  <ion-textarea
                    v-model="roomTypeForm.roomDescription"
                    :disabled="submitting"
                    :rows="4"
                    fill="outline"
                    :placeholder="$t('stage5UiAttributes.78')"
                  />
                </label>

                <label class="settings-form-field">
                  <span>{{ $t('stage5SourceText.103') }}</span>
                  <ion-input
                    v-model="roomTypeForm.suRoomType"
                    :disabled="submitting"
                    fill="outline"
                    :placeholder="$t('stage5UiAttributes.25')"
                  />
                </label>

                <label class="settings-form-field">
                  <span>{{ $t('settingsStage4.roomTypeManagement.editor.fields.area') }}</span>
                  <ion-input
                    v-model="roomTypeForm.sizeMeasurement"
                    :disabled="submitting"
                    fill="outline"
                    inputmode="decimal"
                    :placeholder="$t('stage5UiAttributes.104')"
                  />
                </label>

                <label class="settings-form-field">
                  <span>{{ $t('settingsStage4.roomTypeDetails.fields.sizeUnit') }}</span>
                  <ion-select
                    v-model="roomTypeForm.sizeMeasurementUnit"
                    :disabled="submitting"
                    fill="outline"
                    interface="action-sheet"
                    :placeholder="$t('settingsStage4.roomTypeDetails.placeholders.sizeUnit')"
                  >
                    <ion-select-option value="">{{ $t('channel.list.disconnected') }}</ion-select-option>
                    <ion-select-option
                      v-for="option in ROOM_TYPE_SIZE_UNIT_OPTIONS"
                      :key="option.value"
                      :value="option.value"
                    >
                      {{ $t(option.labelKey) }}
                    </ion-select-option>
                  </ion-select>
                </label>
              </div>
            </div>

            <div class="settings-form-section">
              <div class="settings-section-heading">
                <div>
                  <h2 class="mobile-section-title">{{ $t('roomStatus.detail.channelInfo.priceInfo') }}</h2>
                </div>
                <ion-button size="small" fill="outline" :disabled="submitting" @click="handleApplyDefaultPrice">
                  {{ $t('stage5VisibleText.244') }}
                </ion-button>
              </div>

              <div class="settings-form-grid settings-price-grid">
                <label class="settings-form-field settings-price-grid__full">
                  <span>{{ $t('roomStatus.common.defaultPrice') }}</span>
                  <ion-input
                    v-model="roomTypeForm.defaultPrice"
                    :disabled="submitting"
                    fill="outline"
                    inputmode="decimal"
                    :placeholder="$t('stage5UiAttributes.105')"
                  />
                </label>

                <label
                  v-for="field in ROOM_TYPE_DAILY_PRICE_FIELDS"
                  :key="field.key"
                  class="settings-form-field"
                >
                  <span>{{ $t(field.labelKey) }}</span>
                  <ion-input
                    v-model="roomTypeForm[field.key]"
                    :disabled="submitting"
                    fill="outline"
                    inputmode="decimal"
                    :placeholder="$t('accommodation.roomPrice.inputPrice')"
                  />
                </label>
              </div>
            </div>

            <div class="settings-form-section">
              <div>
                <h2 class="mobile-section-title">{{ $t('stage5SourceText.203') }}</h2>
              </div>

              <div class="settings-form-grid">
                <label class="settings-form-field settings-form-field--full">
                  <span>{{ $t('stage5SourceText.205') }}</span>
                  <ion-textarea
                    v-model="roomTypeForm.facilitiesText"
                    :disabled="submitting"
                    :rows="5"
                    fill="outline"
                    :placeholder="$t('stage5UiAttributes.56')"
                  />
                </label>

                <label class="settings-form-field settings-form-field--full">
                  <span>{{ $t('stage5SourceText.149') }}</span>
                  <ion-textarea
                    v-model="roomTypeForm.desktopPhotoUrlsText"
                    :disabled="submitting"
                    :rows="4"
                    fill="outline"
                    :placeholder="$t('stage5UiAttributes.53')"
                  />
                </label>

                <label class="settings-form-field settings-form-field--full">
                  <span>{{ $t('stage5SourceText.170') }}</span>
                  <ion-textarea
                    v-model="roomTypeForm.mobilePhotoUrlsText"
                    :disabled="submitting"
                    :rows="4"
                    fill="outline"
                    :placeholder="$t('stage5UiAttributes.53')"
                  />
                </label>
              </div>
            </div>

            <div class="settings-form-section">
              <div class="settings-room-numbers__header">
                <div>
                  <h2 class="mobile-section-title">{{ $t('stage5SourceText.108') }}</h2>
                </div>
                <ion-button size="small" fill="outline" :disabled="submitting" @click="handleAddRoom">
                  {{ $t('stage5VisibleText.192') }}
                </ion-button>
              </div>

              <div class="settings-room-numbers__list">
                <div
                  v-for="(room, index) in roomTypeForm.rooms"
                  :key="index"
                  class="settings-room-entry"
                >
                  <div class="settings-room-entry__header">
                    <strong>{{ $t('accommodation.common.room') }} {{ index + 1 }}</strong>
                    <ion-button
                      size="small"
                      color="danger"
                      fill="clear"
                      :disabled="submitting"
                      @click="handleRemoveRoom(index)"
                    >
                      {{ $t('roomStatus.roomLock.actions.delete') }}
                    </ion-button>
                  </div>

                  <div class="settings-room-entry__fields">
                    <label class="settings-form-field">
                      <span>{{ $t('accommodation.common.roomNumber') }}</span>
                      <ion-input
                        v-model="roomTypeForm.rooms[index].roomNumber"
                        :disabled="submitting"
                        fill="outline"
                        :placeholder="$t('settingsStage4.roomSettings.placeholders.roomNumber')"
                      />
                    </label>

                    <label class="settings-form-field">
                      <span>{{ $t('settingsStage4.roomSettings.placeholders.roomPassword') }}</span>
                      <ion-input
                        v-model="roomTypeForm.rooms[index].smartlockPasscode"
                        :disabled="submitting"
                        fill="outline"
                        :placeholder="$t('stage5UiAttributes.79')"
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
                {{ $t('stage5VisibleText.171') }}
              </ion-button>
              <ion-button fill="outline" :disabled="submitting" @click="handleCloseEditor">{{ $t('accommodation.common.cancel') }}</ion-button>
              <ion-button :disabled="submitting" @click="handleSaveRoomType">
                {{ submitting ? $t('channel.mobile.common.saving') : $t('stage5DynamicUi.11') }}
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
import { useStoreStore } from '@/stores/store'
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

const { t } = useI18n()
const storeStore = useStoreStore()
const roomTypeMoneyOptions = computed(() => ({
  currency: storeStore.currentStore?.currency || 'CNY',
  context: { country: storeStore.currentStore?.country },
}))

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
    return t('settingsResidual.roomType.area', { value: roomType.sizeMeasurement })
  }

  return t('settingsResidual.roomType.area', {
    value: `${roomType.sizeMeasurement} ${unit}`,
  })
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
    priceSummary: buildRoomTypePriceSummary(roomType, roomTypeMoneyOptions.value),
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
    return t('settingsResidual.roomType.roomCodeConflict', { message })
  }

  if (
    message.includes('房型名称') ||
    (normalizedMessage.includes('name') &&
      (normalizedMessage.includes('exist') || normalizedMessage.includes('duplicate')))
  ) {
    return t('settingsResidual.roomType.nameExists')
  }

  if (normalizedMessage.includes('maxchildoccupancy') || message.includes('儿童')) {
    return t('settingsResidual.roomType.childLimit')
  }

  if (
    normalizedMessage.includes('sizemeasurementunit') ||
    message.includes('面积单位') ||
    normalizedMessage.includes('sqm') ||
    normalizedMessage.includes('sqft')
  ) {
    return t('settingsResidual.roomType.sizeUnit')
  }

  if (
    message.includes('房型代码') ||
    (normalizedMessage.includes('code') &&
      (normalizedMessage.includes('conflict') || normalizedMessage.includes('exist')))
  ) {
    return t('settingsResidual.roomType.codeConflict')
  }

  return message
}

async function presentAlert(header: string, message: string) {
  const alert = await alertController.create({
    header,
    message,
    buttons: [t('settingsResidual.roomType.acknowledged')],
  })

  await alert.present()
  await alert.onDidDismiss()
}

async function confirmDeleteRoomType(name: string) {
  const alert = await alertController.create({
    header: t('settingsResidual.common.confirm'),
    message: t('settingsResidual.roomType.deleteConfirm', { name }),
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

function buildDeleteFailureMessage(message: string, blockInfo: RoomTypeDeleteBlockInfo | null) {
  if (!blockInfo || blockInfo.totalBlockingReservations <= 0) {
    return message || t('settingsResidual.roomType.deleteFailure')
  }

  const sampleTexts: string[] = []
  for (const item of blockInfo.sample || []) {
    const orderNumber = item.orderNumber || '-'
    const status = item.status || '-'
    const roomNumber = item.roomNumber || '-'
    const dateRange = `${item.checkInDate || '-'} ~ ${item.checkOutDate || '-'}`
    sampleTexts.push(
      t('settingsResidual.roomType.orderSample', {
        order: orderNumber,
        status,
        room: roomNumber,
        dates: dateRange,
      }),
    )
  }

  let result = message || t('settingsResidual.roomType.deleteFailure')
  result += `。${t('settingsResidual.roomType.blockingOrders', { count: blockInfo.totalBlockingReservations })}`
  if (sampleTexts.length > 0) {
    result += `：${sampleTexts.join('；')}`
  }
  result += `。${t('settingsResidual.roomType.retryAfterOrders')}`
  return result
}

async function syncRoomStatus() {
  try {
    await roomStatusStore.refreshAll()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, t('stage5Pattern.refreshFailed')))
    }
  }
}

async function loadRoomTypes() {
  loading.value = true
  try {
    const response = await getAllRoomTypesWithRooms()
    if (!response.success || !response.data) {
      throw new Error(response.message || t('settingsStage4.roomSort.messages.loadRoomTypesFailed'))
    }

    const nextRoomTypes: RoomTypeView[] = []
    for (const item of response.data) {
      nextRoomTypes.push(buildRoomTypeView(item))
    }

    roomTypes.value = nextRoomTypes
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, t('settingsStage4.roomSort.messages.loadRoomTypesFailed')))
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
  showSuccessToast(t('stage5Pattern.operationCompleted'))
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
    showWarningToast(t('stage5Pattern.setup'))
    return
  }

  for (const field of ROOM_TYPE_DAILY_PRICE_FIELDS) {
    roomTypeForm.value[field.key] = roomTypeForm.value.defaultPrice
  }

  showSuccessToast(t('stage5Pattern.operationCompleted'))
}

function parseRequiredPositiveInteger(rawValue: string, fieldLabel: string) {
  const value = Number(rawValue)
  if (!Number.isInteger(value) || value < 1) {
    showWarningToast(
      t('settingsResidual.roomType.integerAtLeast', {
        field: fieldLabel,
        min: 1,
      }),
    )
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
    showWarningToast(
      t('settingsResidual.roomType.integerAtLeast', {
        field: fieldLabel,
        min: 0,
      }),
    )
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
    showWarningToast(
      t('settingsResidual.roomType.numberAtLeast', {
        field: fieldLabel,
        min: 0,
      }),
    )
    return null
  }

  return Number(value.toFixed(2))
}

function buildValidatedRooms() {
  if (roomTypeForm.value.rooms.length === 0) {
    showWarningToast(t('stage5Pattern.atLeast'))
    return null
  }

  const rooms: Array<{ roomNumber: string; smartlockPasscode?: string }> = []
  const normalizedRoomNumbers = new Set<string>()

  for (let index = 0; index < roomTypeForm.value.rooms.length; index += 1) {
    const room = roomTypeForm.value.rooms[index]
    const trimmedRoomNumber = room.roomNumber.trim()
    if (!trimmedRoomNumber) {
      showWarningToast(t('settingsResidual.roomType.roomNumberEmpty', { index: index + 1 }))
      return null
    }

    const normalizedRoomNumber = trimmedRoomNumber.toUpperCase()
    if (normalizedRoomNumbers.has(normalizedRoomNumber)) {
      showWarningToast(
        t('settingsResidual.roomType.roomNumberDuplicate', { value: trimmedRoomNumber }),
      )
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
        t('settingsResidual.roomType.roomNumberConflict', {
          room: roomNumber,
          name: conflictRoomType.name,
          shortName: conflictRoomType.shortName,
        }),
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
      showWarningToast(t('settingsResidual.roomType.nameConflict', { name }))
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
    showWarningToast(t('settingsStage4.roomSettings.placeholders.roomTypeName'))
    return null
  }

  const shortName = roomTypeForm.value.shortName.trim()
  if (!shortName) {
    showWarningToast(t('settingsStage4.roomSettings.placeholders.shortName'))
    return null
  }

  if (!validateNameConflict(name)) {
    return null
  }

  const maxGuests = parseRequiredPositiveInteger(
    roomTypeForm.value.maxGuests,
    t('settingsStage4.roomSettings.fields.maxGuests'),
  )
  if (maxGuests === null) {
    return null
  }

  const maxChildOccupancy = parseOptionalNonNegativeInteger(
    roomTypeForm.value.maxChildOccupancy,
    t('settingsStage4.roomTypeDetails.fields.maxChildren'),
  )
  if (maxChildOccupancy === null) {
    return null
  }
  if (maxChildOccupancy !== undefined && maxChildOccupancy > maxGuests) {
    showWarningToast(t('stage5Pattern.unsupported'))
    return null
  }

  const sizeMeasurement = parseOptionalNonNegativeNumber(
    roomTypeForm.value.sizeMeasurement,
    t('settingsStage4.roomTypeManagement.editor.fields.area'),
  )
  if (sizeMeasurement === null) {
    return null
  }

  const sizeMeasurementUnit = roomTypeForm.value.sizeMeasurementUnit.trim()
  if (sizeMeasurement !== undefined && sizeMeasurementUnit !== 'sqm' && sizeMeasurementUnit !== 'sqft') {
    showWarningToast(t('stage5Pattern.unsupported'))
    return null
  }

  const defaultPrice = parseOptionalNonNegativeNumber(
    roomTypeForm.value.defaultPrice,
    t('settingsStage4.roomSettings.columns.defaultPrice'),
  )
  if (defaultPrice === null) {
    return null
  }

  const mondayPrice = parseOptionalNonNegativeNumber(
    roomTypeForm.value.mondayPrice,
    t('settingsResidual.priceRates.monday'),
  )
  if (mondayPrice === null) {
    return null
  }
  const tuesdayPrice = parseOptionalNonNegativeNumber(
    roomTypeForm.value.tuesdayPrice,
    t('settingsResidual.priceRates.tuesday'),
  )
  if (tuesdayPrice === null) {
    return null
  }
  const wednesdayPrice = parseOptionalNonNegativeNumber(
    roomTypeForm.value.wednesdayPrice,
    t('settingsResidual.priceRates.wednesday'),
  )
  if (wednesdayPrice === null) {
    return null
  }
  const thursdayPrice = parseOptionalNonNegativeNumber(
    roomTypeForm.value.thursdayPrice,
    t('settingsResidual.priceRates.thursday'),
  )
  if (thursdayPrice === null) {
    return null
  }
  const fridayPrice = parseOptionalNonNegativeNumber(
    roomTypeForm.value.fridayPrice,
    t('settingsResidual.priceRates.friday'),
  )
  if (fridayPrice === null) {
    return null
  }
  const saturdayPrice = parseOptionalNonNegativeNumber(
    roomTypeForm.value.saturdayPrice,
    t('settingsResidual.priceRates.saturday'),
  )
  if (saturdayPrice === null) {
    return null
  }
  const sundayPrice = parseOptionalNonNegativeNumber(
    roomTypeForm.value.sundayPrice,
    t('settingsResidual.priceRates.sunday'),
  )
  if (sundayPrice === null) {
    return null
  }

  const normalizedGuideLink = normalizeHttpUrl(roomTypeForm.value.checkInGuideLink)
  if (roomTypeForm.value.checkInGuideLink.trim() && !normalizedGuideLink) {
    showWarningToast(t('settingsStage4.roomSettings.messages.invalidGuideLink'))
    return null
  }

  const desktopPhotoResult = normalizeUrlTextList(roomTypeForm.value.desktopPhotoUrlsText)
  if (desktopPhotoResult.invalidValue) {
    showWarningToast(
      t('settingsResidual.roomType.imageUrlInvalid', {
        device: t('settingsResidual.roomType.desktop'),
        value: desktopPhotoResult.invalidValue,
      }),
    )
    return null
  }

  const mobilePhotoResult = normalizeUrlTextList(roomTypeForm.value.mobilePhotoUrlsText)
  if (mobilePhotoResult.invalidValue) {
    showWarningToast(
      t('settingsResidual.roomType.imageUrlInvalid', {
        device: t('settingsResidual.roomType.mobile'),
        value: mobilePhotoResult.invalidValue,
      }),
    )
    return null
  }

  const rooms = buildValidatedRooms()
  if (!rooms) {
    return null
  }

  const roomNumbers = rooms.map((room) => room.roomNumber)

  const code = buildRoomTypeCode(shortName, editingRoomType.value?.code)
  if (!code) {
    showWarningToast(t('stage5Pattern.unsupported'))
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
        throw new Error(resolveSaveFailureMessage(response.message || t('stage5Pattern.updateFailed')))
      }
      showSuccessToast(t('stage5Pattern.updateCompleted'))
    } else {
      const response = await createRoomType(payload)
      if (!response.success) {
        throw new Error(resolveSaveFailureMessage(response.message || t('stage5Pattern.createFailed')))
      }
      showSuccessToast(t('stage5Pattern.createCompleted'))
    }

    editorOpen.value = false
    resetEditorState()
    await loadRoomTypes()
    await syncRoomStatus()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      const fallbackMessage = editingRoomType.value
        ? t('stage5Pattern.updateFailed')
        : t('stage5Pattern.createFailed')
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
      const message = buildDeleteFailureMessage(response.message || t('stage5Pattern.deleteFailed'), response.data)
      await presentAlert(t('settingsResidual.roomType.deleteUnavailable'), message)
      return
    }

    showSuccessToast(t('stage5Pattern.deleteCompleted'))
    await loadRoomTypes()
    await syncRoomStatus()
  } catch (error) {
    const message = resolveWarningMessage(error, t('stage5Pattern.deleteFailed'))
    await presentAlert(t('stage5Pattern.deleteFailed'), message)
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
