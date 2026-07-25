<template>
  <ion-page>
    <ion-header translucent>
      <ion-toolbar class="app-page-header__toolbar">
        <ion-buttons slot="start">
          <ion-back-button class="app-page-header__back-btn" :default-href="ROUTE_PATHS.rooms" />
        </ion-buttons>
        <ion-title class="app-page-header__title">{{ $t('routes.RoomsPricing') }}</ion-title>
        <ion-buttons slot="end">
          <ion-button class="room-price-page__history-button" @click="handleOpenHistory">
            {{ $t('accommodation.layout.priceChangeHistory') }}
          </ion-button>
        </ion-buttons>
      </ion-toolbar>
    </ion-header>

    <ion-content fullscreen class="mobile-page room-price-page">
      <ion-refresher slot="fixed" @ionRefresh="handleRefresh">
        <ion-refresher-content :pulling-text="$t('stage5UiAttributes.5')" refreshing-spinner="crescent" />
      </ion-refresher>

      <section class="mobile-hero room-price-page__hero">
        <p class="mobile-note room-price-page__eyebrow">{{ $t('stage5VisibleText.121') }}</p>
        <h1 class="mobile-title">{{ $t('routes.RoomsPricing') }}</h1>
        <p class="mobile-subtitle">{{ $t('stage5VisibleText.219') }}</p>
        <div class="mobile-chip-row">
          <span class="mobile-chip">{{ $t('stage5DynamicUi.136') }} {{ formatDate(selectedDate) }}</span>
          <span class="mobile-chip">{{ $t('stage5DynamicUi.137') }} {{ windowDays }} {{ $t('accommodation.common.dayUnit') }}</span>
        </div>
      </section>

      <div class="mobile-stack">
        <section class="mobile-card room-price-page__toolbar-card">
          <div class="room-price-page__toolbar-row">
            <ion-button fill="outline" size="small" @click="handleShiftDate(-7)">{{ $t('accommodation.roomPrice.previousWeek') }}</ion-button>
            <ion-button fill="outline" size="small" @click="handleShiftDate(-1)">{{ $t('accommodation.roomPrice.previousDay') }}</ion-button>
            <ion-button fill="outline" size="small" @click="handleGoToday">{{ $t('accommodation.common.today') }}</ion-button>
            <ion-button fill="outline" size="small" @click="handleShiftDate(1)">{{ $t('accommodation.roomPrice.nextDay') }}</ion-button>
            <ion-button fill="outline" size="small" @click="handleShiftDate(7)">{{ $t('accommodation.roomPrice.nextWeek') }}</ion-button>
          </div>

          <div class="room-price-page__toolbar-row room-price-page__toolbar-row--compact">
            <button
              v-for="preset in windowDayPresets"
              :key="preset"
              class="room-price-page__preset-chip"
              :class="{ 'is-active': windowDays === preset }"
              type="button"
              @click="handleChangeWindowDays(preset)"
            >
              {{ preset }} {{ $t('accommodation.common.dayUnit') }}
            </button>
          </div>

          <div class="room-price-page__filter-grid">
            <label class="room-price-page__field">
              <span>{{ $t('stage5VisibleText.232') }}</span>
              <input :value="selectedDate" type="date" @input="handleDateInput" />
            </label>

            <label class="room-price-page__field">
              <span>{{ $t('accommodation.roomPrice.roomTypeFilter') }}</span>
              <select :value="roomTypeSelectValue" @change="handleRoomTypeChange">
                <option value="">{{ $t('accommodation.priceHistory.allRoomTypes') }}</option>
                <option v-for="roomType in roomTypes" :key="roomType.id" :value="String(roomType.id)">
                  {{ roomType.name }}
                </option>
              </select>
            </label>

            <label class="room-price-page__field">
              <span>{{ $t('accommodation.roomPrice.groupFilter') }}</span>
              <select :value="roomGroupSelectValue" @change="handleRoomGroupChange">
                <option value="">{{ $t('channel.mobile.mapping.filters.allGroups') }}</option>
                <option v-for="group in roomGroups" :key="group.id" :value="String(group.id)">
                  {{ group.name }}
                </option>
              </select>
            </label>
          </div>

          <div class="room-price-page__toolbar-row room-price-page__toolbar-row--actions">
            <ion-button fill="outline" @click="handleExportCurrentView">{{ $t('stage5VisibleText.153') }}</ion-button>
            <ion-button @click="handleOpenBulkUpdate">{{ $t('accommodation.roomPrice.bulkUpdate') }}</ion-button>
          </div>

          <p v-if="errorMessage" class="mobile-note room-price-page__error">{{ errorMessage }}</p>
        </section>

        <section class="mobile-card">
          <div class="mobile-inline-row room-price-page__section-header">
            <div>
              <h2 class="mobile-section-title">{{ $t('stage5VisibleText.233') }}</h2>
              <p class="mobile-note">{{ $t('stage5DynamicUi.130') }} {{ windowDays }} {{ $t('stage5DynamicUi.106') }}</p>
            </div>
            <ion-spinner v-if="loading" name="crescent" />
          </div>

          <div class="mobile-chip-row room-price-page__summary-row">
            <span class="mobile-chip">{{ $t('accommodation.common.roomType') }} {{ summaryMetrics.roomTypeCount }} {{ $t('stage5DynamicUi.87') }}</span>
            <span class="mobile-chip">{{ $t('accommodation.roomPriceBulk.table.pricePlan') }} {{ summaryMetrics.planCount }} {{ $t('stage5DynamicUi.87') }}</span>
            <span class="mobile-chip">{{ $t('stage5DynamicUi.104') }} {{ summaryMetrics.unavailableCellCount }} {{ $t('accommodation.common.dayUnit') }}</span>
          </div>

          <div v-if="matrixRows.length > 0" class="room-price-page__matrix-shell">
            <div class="room-price-page__matrix-scroll">
              <div class="room-price-page__matrix-row room-price-page__matrix-row--header" :style="matrixGridStyle">
                <div class="room-price-page__matrix-leading room-price-page__matrix-leading--header">
                  <strong>{{ $t('stage5VisibleText.173') }}</strong>
                  <span>{{ $t('stage5VisibleText.214') }}</span>
                </div>
                <div
                  v-for="day in dateWindow"
                  :key="`matrix-header-${day.date}`"
                  class="room-price-page__matrix-header-cell"
                  :class="{ 'is-today': day.isToday, 'is-weekend': day.isWeekend }"
                >
                  <strong>{{ day.shortLabel }}</strong>
                  <span>{{ day.weekday }}</span>
                </div>
              </div>

              <div
                v-for="row in matrixRows"
                :key="row.key"
                class="room-price-page__matrix-row"
                :style="matrixGridStyle"
              >
                <div class="room-price-page__matrix-leading room-price-page__matrix-leading--body">
                  <strong>{{ row.roomTypeName }}</strong>
                  <span>{{ row.pricePlanName }}</span>
                  <small>{{ row.roomTypeCode || $t('stage5DynamicUi.51') }}</small>
                </div>

                <button
                  v-for="cell in row.cells"
                  :key="`${row.key}-${cell.date}`"
                  class="room-price-page__matrix-cell"
                  :class="getMatrixCellClass(cell.record)"
                  type="button"
                  @click="handleOpenEditor(row, cell)"
                >
                  <strong>{{ getDisplayPriceText(cell.record) }}</strong>
                  <span>{{ getAvailabilityCompactText(cell.record) }}</span>
                  <small>{{ getMatrixStatusText(cell.record) }}</small>
                </button>
              </div>
            </div>
          </div>

          <p v-else-if="!loading" class="mobile-note">{{ $t('stage5VisibleText.165') }}</p>
        </section>

        <section class="mobile-card">
          <h2 class="mobile-section-title">{{ $t('stage5VisibleText.220') }}</h2>
          <ul class="mobile-bullet-list">
            <li>{{ $t('stage5VisibleText.187') }}</li>
            <li>{{ $t('stage5VisibleText.190') }}</li>
            <li>{{ $t('stage5VisibleText.189') }}</li>
          </ul>
        </section>
      </div>

      <ion-modal :is-open="editorOpen" @didDismiss="handleCloseEditor">
        <ion-header>
          <ion-toolbar>
            <ion-title>{{ $t('settingsStage4.pricePlan.dialog.editRate') }}</ion-title>
            <ion-buttons slot="end">
              <ion-button @click="handleCloseEditor">{{ $t('home.section.close') }}</ion-button>
            </ion-buttons>
          </ion-toolbar>
        </ion-header>

        <ion-content class="mobile-page room-price-page__modal-page">
          <section class="mobile-card room-price-page__editor-card">
            <div class="room-price-page__editor-title">
              <strong>{{ editorForm.roomTypeName }}</strong>
              <span>{{ editorForm.pricePlanName }}</span>
            </div>

            <div v-if="editorRecord" class="room-price-page__editor-current">
              <div class="room-price-page__editor-current-header">
                <strong>{{ getDisplayPriceText(editorRecord) }}</strong>
                <span>{{ getAvailabilityText(editorRecord) }}</span>
              </div>
              <div class="room-price-page__tag-row">
                <span v-for="tag in getStatusTags(editorRecord)" :key="`editor-${tag}`">{{ tag }}</span>
              </div>
              <p class="mobile-note">
                {{ getPriceSourceText(editorRecord) }} · {{ getStayRestrictionText(editorRecord) }}
              </p>
              <p v-if="editorRecord.priceLabsBasePrice !== undefined && editorRecord.priceLabsBasePrice !== null" class="mobile-note">
                {{ $t('stage5DynamicUi.79') }} {{ formatMoney(editorRecord.priceLabsBasePrice) }}{{ $t('stage5DynamicUi.145') }} {{ formatDateTime(editorRecord.priceLabsUpdatedAt) }}
              </p>
              <p v-if="editorRecord.manualOverrideUntil" class="mobile-note">
                {{ $t('stage5DynamicUi.116') }} {{ formatDate(editorRecord.manualOverrideUntil) }}
              </p>
            </div>

            <div class="room-price-page__filter-grid">
              <label class="room-price-page__field">
                <span>{{ $t('accommodation.common.startDate') }}</span>
                <input v-model="editorForm.startDate" type="date" />
              </label>

              <label class="room-price-page__field">
                <span>{{ $t('accommodation.common.endDate') }}</span>
                <input v-model="editorForm.endDate" type="date" />
              </label>

              <label class="room-price-page__field room-price-page__field--full">
                <span>{{ $t('stage5VisibleText.223') }}</span>
                <select v-model="editorForm.settingType">
                  <option value="price">{{ $t('accommodation.roomPrice.settingType.price') }}</option>
                  <option value="minStay">{{ $t('accommodation.roomPrice.settingType.minStay') }}</option>
                  <option value="maxStay">{{ $t('accommodation.roomPrice.settingType.maxStay') }}</option>
                  <option value="closeRoom">{{ $t('accommodation.roomPrice.closeRoom') }}</option>
                  <option value="cta">CTA</option>
                  <option value="ctd">CTD</option>
                </select>
              </label>
            </div>

            <div class="room-price-page__weekday-section">
              <div class="room-price-page__weekday-header">
                <span>{{ $t('stage5VisibleText.236') }}</span>
                <div class="room-price-page__weekday-actions">
                  <ion-button fill="clear" size="small" @click="handleSelectAllWeekdays">{{ $t('roomStatus.filterDrawer.selectAll') }}</ion-button>
                  <ion-button fill="clear" size="small" @click="handleInvertWeekdays">{{ $t('accommodation.roomPrice.invertSelection') }}</ion-button>
                </div>
              </div>
              <div class="room-price-page__weekday-grid">
                <button
                  v-for="item in weekdayOptions"
                  :key="item.value"
                  class="room-price-page__weekday-button"
                  :class="{ 'is-active': isWeekdaySelected(item.value) }"
                  type="button"
                  @click="handleToggleWeekday(item.value)"
                >
                  {{ item.label }}
                </button>
              </div>
              <p class="mobile-note room-price-page__weekday-hint">{{ editorWeekdayHint }}</p>
            </div>

            <label v-if="editorForm.settingType === 'price'" class="room-price-page__field room-price-page__field--full">
              <span>{{ $t('accommodation.roomPrice.settingType.price') }}</span>
              <input v-model="editorForm.price" inputmode="decimal" :placeholder="$t('accommodation.roomPrice.inputPrice')" type="number" />
            </label>

            <label v-if="editorForm.settingType === 'minStay'" class="room-price-page__field room-price-page__field--full">
              <span>{{ $t('accommodation.roomPrice.settingType.minStay') }}</span>
              <input v-model="editorForm.minStay" inputmode="numeric" :placeholder="$t('accommodation.roomPrice.inputMinStay')" type="number" />
            </label>

            <label v-if="editorForm.settingType === 'maxStay'" class="room-price-page__field room-price-page__field--full">
              <span>{{ $t('accommodation.roomPrice.settingType.maxStay') }}</span>
              <input v-model="editorForm.maxStay" inputmode="numeric" :placeholder="$t('accommodation.roomPrice.inputMaxStay')" type="number" />
            </label>

            <div v-if="editorForm.settingType === 'closeRoom'" class="room-price-page__toggle-row">
              <div>
                <strong>{{ $t('accommodation.roomPrice.closeRoom') }}</strong>
                <p>{{ $t('stage5VisibleText.161') }}</p>
              </div>
              <ion-toggle v-model="editorForm.closeRoom" />
            </div>

            <div v-if="editorForm.settingType === 'cta'" class="room-price-page__toggle-row">
              <div>
                <strong>CTA</strong>
                <p>{{ $t('stage5VisibleText.182') }}</p>
              </div>
              <ion-toggle v-model="editorForm.cta" />
            </div>

            <div v-if="editorForm.settingType === 'ctd'" class="room-price-page__toggle-row">
              <div>
                <strong>CTD</strong>
                <p>{{ $t('stage5VisibleText.183') }}</p>
              </div>
              <ion-toggle v-model="editorForm.ctd" />
            </div>

            <div class="room-price-page__editor-actions">
              <ion-button fill="outline" @click="handleCloseEditor">{{ $t('accommodation.common.cancel') }}</ion-button>
              <ion-button :disabled="saving" @click="handleSaveEditor">
                {{ saving ? $t('channel.mobile.common.saving') : $t('roomStatus.bookingModal.submit.edit') }}
              </ion-button>
            </div>
          </section>
        </ion-content>
      </ion-modal>

      <ion-modal :is-open="bulkUpdateOpen" @didDismiss="handleCloseBulkUpdate">
        <ion-header>
          <ion-toolbar>
            <ion-title>{{ $t('accommodation.roomPrice.bulkUpdate') }}</ion-title>
            <ion-buttons slot="end">
              <ion-button @click="handleCloseBulkUpdate">{{ $t('home.section.close') }}</ion-button>
            </ion-buttons>
          </ion-toolbar>
        </ion-header>

        <ion-content class="mobile-page room-price-page__modal-page">
          <section class="mobile-card room-price-page__editor-card">
            <div class="mobile-chip-row">
              <span class="mobile-chip">{{ $t('accommodation.common.roomType') }} {{ bulkForm.roomTypeIds.length }} {{ $t('stage5DynamicUi.87') }}</span>
              <span class="mobile-chip">{{ $t('stage5DynamicUi.120') }} {{ bulkForm.dateRanges.length }} {{ $t('stage5DynamicUi.87') }}</span>
              <span class="mobile-chip">{{ $t('accommodation.common.weekday') }} {{ bulkSelectedWeekdayCountLabel }}</span>
            </div>

            <div class="room-price-page__bulk-section">
              <div class="room-price-page__bulk-section-header">
                <strong>{{ $t('stage5VisibleText.102') }}</strong>
                <div class="room-price-page__weekday-actions">
                  <ion-button fill="clear" size="small" @click="handleSelectAllBulkRoomTypes">{{ $t('roomStatus.filterDrawer.selectAll') }}</ion-button>
                  <ion-button fill="clear" size="small" @click="handleClearBulkRoomTypes">{{ $t('accommodation.roomPriceBulk.clear') }}</ion-button>
                </div>
              </div>

              <div class="room-price-page__selection-grid">
                <button
                  v-for="roomType in bulkSelectableRoomTypes"
                  :key="`bulk-room-type-${roomType.id}`"
                  class="room-price-page__selection-chip"
                  :class="{ 'is-active': bulkForm.roomTypeIds.includes(roomType.id) }"
                  type="button"
                  @click="handleToggleBulkRoomType(roomType.id)"
                >
                  {{ roomType.name }}
                </button>
              </div>
            </div>

            <div class="room-price-page__bulk-section">
              <div class="room-price-page__bulk-section-header">
                <strong>{{ $t('stage5VisibleText.103') }}</strong>
                <ion-button fill="clear" size="small" @click="handleAddBulkDateRange">{{ $t('stage5VisibleText.212') }}</ion-button>
              </div>

              <div class="room-price-page__bulk-range-list">
                <div
                  v-for="(range, index) in bulkForm.dateRanges"
                  :key="`bulk-range-${index}`"
                  class="room-price-page__bulk-range-item"
                >
                  <label class="room-price-page__field">
                    <span>{{ $t('accommodation.common.startDate') }}</span>
                    <input v-model="range.startDate" type="date" />
                  </label>
                  <label class="room-price-page__field">
                    <span>{{ $t('accommodation.common.endDate') }}</span>
                    <input v-model="range.endDate" type="date" />
                  </label>
                  <ion-button
                    v-if="bulkForm.dateRanges.length > 1"
                    fill="clear"
                    color="danger"
                    @click="handleRemoveBulkDateRange(index)"
                  >
                    {{ $t('roomStatus.roomLock.actions.delete') }}
                  </ion-button>
                </div>
              </div>
            </div>

            <div class="room-price-page__bulk-section">
              <div class="room-price-page__bulk-section-header">
                <strong>{{ $t('stage5VisibleText.104') }}</strong>
                <div class="room-price-page__weekday-actions">
                  <ion-button fill="clear" size="small" @click="handleSelectAllBulkWeekdays">{{ $t('roomStatus.filterDrawer.selectAll') }}</ion-button>
                  <ion-button fill="clear" size="small" @click="handleClearBulkWeekdays">{{ $t('accommodation.roomPriceBulk.clear') }}</ion-button>
                </div>
              </div>

              <div class="room-price-page__weekday-grid">
                <button
                  v-for="item in bulkWeekdayOptions"
                  :key="`bulk-weekday-${item.value}`"
                  class="room-price-page__weekday-button"
                  :class="{ 'is-active': bulkForm.weekdays.includes(item.value) }"
                  type="button"
                  @click="handleToggleBulkWeekday(item.value)"
                >
                  {{ item.label }}
                </button>
              </div>
            </div>

            <div class="room-price-page__bulk-section">
              <div class="room-price-page__toggle-row">
                <div>
                  <strong>{{ $t('stage5VisibleText.142') }}</strong>
                  <p>{{ $t('stage5VisibleText.160') }}</p>
                </div>
                <ion-toggle v-model="bulkForm.weekendDifferentiation" />
              </div>

              <div class="room-price-page__filter-grid">
                <label class="room-price-page__field">
                  <span>{{ $t('settingsStage4.roomTypeManagement.fields.weekdayPrice') }}</span>
                  <input v-model="bulkForm.weekdayPrice" inputmode="decimal" :placeholder="$t('stage5UiAttributes.75')" type="number" />
                </label>

                <label v-if="bulkForm.weekendDifferentiation" class="room-price-page__field">
                  <span>{{ $t('settingsStage4.roomTypeManagement.fields.weekendPrice') }}</span>
                  <input v-model="bulkForm.weekendPrice" inputmode="decimal" :placeholder="$t('stage5UiAttributes.68')" type="number" />
                </label>

                <label class="room-price-page__field room-price-page__field--full">
                  <span>{{ $t('accommodation.common.remarks') }}</span>
                  <textarea v-model="bulkForm.notes" rows="3" :placeholder="$t('stage5UiAttributes.36')"></textarea>
                </label>
              </div>
            </div>

            <div class="room-price-page__editor-actions">
              <ion-button fill="outline" @click="handleCloseBulkUpdate">{{ $t('accommodation.common.cancel') }}</ion-button>
              <ion-button :disabled="bulkSubmitting" @click="handleSubmitBulkUpdate">
                {{ bulkSubmitting ? $t('iosStage5.cleaning.submitting') : $t('stage5DynamicUi.31') }}
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
  IonBackButton,
  IonButton,
  IonButtons,
  IonContent,
  IonHeader,
  IonModal,
  IonPage,
  IonRefresher,
  IonRefresherContent,
  IonSpinner,
  IonTitle,
  IonToggle,
  IonToolbar,
} from '@ionic/vue'
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { onIonViewWillEnter } from '@ionic/vue'
import {
  bulkPriceChange,
  getRoomPriceManagementData,
  updatePriceByPlan,
  type BulkPriceChangeRequest,
  type RoomPriceManagementDTO,
  type UpdatePriceByPlanRequest,
} from '@/api/roomPrice'
import { getAllRoomGroups, getGroupMembers } from '@/api/roomGroup'
import { getAllRoomTypes, getAllRoomTypesWithRooms, type RoomTypeDTO } from '@/api/roomType'
import { ROUTE_PATHS } from '@/router/guards'
import { useStoreStore } from '@/stores/store'
import { useUserStore } from '@/stores/user'
import type { RoomGroupDTO } from '@/types/settings'
import {
  buildDateWindow,
  formatDate,
  formatDateTime,
  getTodayDate,
  resolveUserLabel,
  shiftDate,
} from '@/utils/accommodation'
import {
  compareLocalizedText,
  formatMoney as formatBusinessMoney,
} from '@/utils/formatters'
import { showSuccessToast, showWarningToast } from '@/utils/notify'
import { isHandledRequestError } from '@/utils/request'

type SettingType = 'price' | 'minStay' | 'maxStay' | 'closeRoom' | 'cta' | 'ctd'

interface PriceCellView {
  date: string
  shortLabel: string
  weekday: string
  record: RoomPriceManagementDTO | null
}

interface PricePlanView {
  key: string
  pricePlanId: number
  pricePlanName: string
  cells: PriceCellView[]
}

interface RoomPriceGroupView {
  roomTypeId: number
  roomTypeName: string
  roomTypeCode: string
  plans: PricePlanView[]
}

interface MatrixRowView {
  key: string
  roomTypeId: number
  roomTypeName: string
  roomTypeCode: string
  pricePlanId: number
  pricePlanName: string
  cells: PriceCellView[]
}

interface RoomGroupOption extends RoomGroupDTO {
  id: number
}

interface SummaryMetrics {
  roomTypeCount: number
  planCount: number
  unavailableCellCount: number
}

interface EditorState {
  roomTypeId: number
  pricePlanId: number
  roomTypeName: string
  pricePlanName: string
  startDate: string
  endDate: string
  weekdays: number[]
  settingType: SettingType
  price: string
  minStay: string
  maxStay: string
  closeRoom: boolean
  cta: boolean
  ctd: boolean
}

interface BulkDateRangeItem {
  startDate: string
  endDate: string
}

interface BulkFormState {
  roomTypeIds: number[]
  dateRanges: BulkDateRangeItem[]
  weekdays: number[]
  weekendDifferentiation: boolean
  weekdayPrice: string
  weekendPrice: string
  notes: string
}

const DEFAULT_WINDOW_DAYS = 30
const WINDOW_DAY_PRESETS = [7, 14, 30]
const FULL_WEEKDAY_VALUES = [1, 2, 3, 4, 5, 6, 7]
const ALL_WEEKDAY_VALUES = [0, ...FULL_WEEKDAY_VALUES]
const BULK_WEEKDAY_VALUES = [1, 2, 3, 4, 5, 6, 0]
const MAX_STAY_LIMIT = 99
const MAX_BULK_DATE_RANGES = 10

const router = useRouter()
const storeStore = useStoreStore()
const userStore = useUserStore()
const { t } = useI18n()

const selectedDate = ref(getTodayDate())
const windowDays = ref(DEFAULT_WINDOW_DAYS)
const selectedRoomTypeId = ref<number | null>(null)
const selectedRoomGroupId = ref<number | null>(null)
const roomTypes = ref<RoomTypeDTO[]>([])
const roomGroups = ref<RoomGroupOption[]>([])
const roomGroupRoomTypeIdsMap = ref<Record<number, number[]>>({})
const records = ref<RoomPriceManagementDTO[]>([])
const loading = ref(false)
const saving = ref(false)
const editorOpen = ref(false)
const bulkUpdateOpen = ref(false)
const bulkSubmitting = ref(false)
const errorMessage = ref('')
const editorRecord = ref<RoomPriceManagementDTO | null>(null)
let hasLoadedReferenceData = false

const weekdayOptions = computed(() => [
  { label: t('home.section.all'), value: 0 },
  { label: t('accommodation.weekdays.short.1'), value: 1 },
  { label: t('accommodation.weekdays.short.2'), value: 2 },
  { label: t('accommodation.weekdays.short.3'), value: 3 },
  { label: t('accommodation.weekdays.short.4'), value: 4 },
  { label: t('accommodation.weekdays.short.5'), value: 5 },
  { label: t('accommodation.weekdays.short.6'), value: 6 },
  { label: t('accommodation.weekdays.short.7'), value: 7 },
])

const bulkWeekdayOptions = computed(() => [
  { label: t('accommodation.weekdays.short.1'), value: 1 },
  { label: t('accommodation.weekdays.short.2'), value: 2 },
  { label: t('accommodation.weekdays.short.3'), value: 3 },
  { label: t('accommodation.weekdays.short.4'), value: 4 },
  { label: t('accommodation.weekdays.short.5'), value: 5 },
  { label: t('accommodation.weekdays.short.6'), value: 6 },
  { label: t('accommodation.weekdays.short.7'), value: 0 },
])

const editorForm = ref<EditorState>({
  roomTypeId: 0,
  pricePlanId: 0,
  roomTypeName: '',
  pricePlanName: '',
  startDate: getTodayDate(),
  endDate: getTodayDate(),
  weekdays: [],
  settingType: 'price',
  price: '0',
  minStay: '1',
  maxStay: '',
  closeRoom: false,
  cta: false,
  ctd: false,
})

const createDefaultBulkForm = (): BulkFormState => ({
  roomTypeIds: [],
  dateRanges: [
    {
      startDate: selectedDate.value,
      endDate: selectedDate.value,
    },
  ],
  weekdays: [...BULK_WEEKDAY_VALUES],
  weekendDifferentiation: false,
  weekdayPrice: '',
  weekendPrice: '',
  notes: '',
})

const bulkForm = ref<BulkFormState>(createDefaultBulkForm())
const currentCurrency = computed(() => storeStore.currentStore?.currency || 'CNY')
const currentMoneyContext = computed(() => ({ country: storeStore.currentStore?.country }))

const dateWindow = computed(() => {
  return buildDateWindow(selectedDate.value, windowDays.value)
})

const windowDayPresets = WINDOW_DAY_PRESETS

const roomTypeSelectValue = computed(() => {
  if (!selectedRoomTypeId.value) {
    return ''
  }
  return String(selectedRoomTypeId.value)
})

const roomGroupSelectValue = computed(() => {
  if (!selectedRoomGroupId.value) {
    return ''
  }
  return String(selectedRoomGroupId.value)
})

const matrixGridStyle = computed(() => {
  return {
    gridTemplateColumns: `168px repeat(${dateWindow.value.length}, minmax(88px, 1fr))`,
  }
})

const filteredRecords = computed(() => {
  if (!selectedRoomGroupId.value) {
    return records.value
  }

  const roomTypeIds = roomGroupRoomTypeIdsMap.value[selectedRoomGroupId.value] || []
  if (roomTypeIds.length === 0) {
    return []
  }

  const allowedRoomTypeIds = new Set(roomTypeIds)
  return records.value.filter((record) => allowedRoomTypeIds.has(record.roomTypeId))
})

const filteredRecordMap = computed(() => {
  const map = new Map<string, RoomPriceManagementDTO>()

  for (const record of filteredRecords.value) {
    const recordPricePlanId = record.pricePlanId ?? 0
    map.set(`${record.roomTypeId}-${recordPricePlanId}-${record.priceDate}`, record)
  }

  return map
})

const groupedRecords = computed<RoomPriceGroupView[]>(() => {
  const roomMap = new Map<number, RoomPriceGroupView>()

  for (const record of filteredRecords.value) {
    const roomTypeId = record.roomTypeId
    const pricePlanId = record.pricePlanId ?? 0
    const pricePlanName = record.pricePlanName || t('accommodation.roomPrice.unnamedPricePlan')
    const roomKey = roomTypeId
    const planKey = `${roomTypeId}-${pricePlanId}`

    if (!roomMap.has(roomKey)) {
      roomMap.set(roomKey, {
        roomTypeId,
        roomTypeName: record.roomTypeName,
        roomTypeCode: record.roomTypeCode,
        plans: [],
      })
    }

    const roomGroup = roomMap.get(roomKey)
    if (!roomGroup) {
      continue
    }

    let planGroup = roomGroup.plans.find((item) => item.key === planKey)
    if (!planGroup) {
      planGroup = {
        key: planKey,
        pricePlanId,
        pricePlanName,
        cells: [],
      }
      roomGroup.plans.push(planGroup)
    }
  }

  const groups = Array.from(roomMap.values())

  for (const group of groups) {
    for (const plan of group.plans) {
      const cells: PriceCellView[] = []
      for (const day of dateWindow.value) {
        const matchedRecord = filteredRecordMap.value.get(
          `${group.roomTypeId}-${plan.pricePlanId}-${day.date}`,
        )

        cells.push({
          date: day.date,
          shortLabel: day.shortLabel,
          weekday: day.weekday,
          record: matchedRecord || null,
        })
      }
      plan.cells = cells
    }
  }

  groups.sort((left, right) => compareLocalizedText(left.roomTypeName, right.roomTypeName))
  return groups
})

const matrixRows = computed<MatrixRowView[]>(() => {
  const rows: MatrixRowView[] = []

  for (const group of groupedRecords.value) {
    for (const plan of group.plans) {
      rows.push({
        key: plan.key,
        roomTypeId: group.roomTypeId,
        roomTypeName: group.roomTypeName,
        roomTypeCode: group.roomTypeCode,
        pricePlanId: plan.pricePlanId,
        pricePlanName: plan.pricePlanName,
        cells: plan.cells,
      })
    }
  }

  return rows
})

const bulkSelectableRoomTypes = computed(() => {
  const groupRoomTypeIds = selectedRoomGroupId.value
    ? roomGroupRoomTypeIdsMap.value[selectedRoomGroupId.value] || []
    : []
  const allowedByGroup = selectedRoomGroupId.value ? new Set(groupRoomTypeIds) : null

  return roomTypes.value
    .filter((roomType) => {
      if (selectedRoomTypeId.value && roomType.id !== selectedRoomTypeId.value) {
        return false
      }

      if (allowedByGroup && !allowedByGroup.has(roomType.id)) {
        return false
      }

      return true
    })
    .sort((left, right) => compareLocalizedText(left.name, right.name))
})

const bulkSelectedWeekdayCountLabel = computed(() => {
  if (bulkForm.value.weekdays.length === BULK_WEEKDAY_VALUES.length) {
    return t('home.section.all')
  }

  return `${bulkForm.value.weekdays.length} ${t('accommodation.common.dayUnit')}`
})

const summaryMetrics = computed<SummaryMetrics>(() => {
  let planCount = 0
  let unavailableCellCount = 0

  for (const group of groupedRecords.value) {
    planCount += group.plans.length
    for (const plan of group.plans) {
      for (const cell of plan.cells) {
        if (isUnavailableRecord(cell.record)) {
          unavailableCellCount += 1
        }
      }
    }
  }

  return {
    roomTypeCount: groupedRecords.value.length,
    planCount,
    unavailableCellCount,
  }
})

const editorWeekdayHint = computed(() => {
  if (!editorForm.value.startDate || !editorForm.value.endDate) {
    return t('stage5Final.roomPrice.multiDayHint')
  }

  const normalizedRange = normalizeDateRange(editorForm.value.startDate, editorForm.value.endDate)
  if (normalizedRange.startDate === normalizedRange.endDate) {
    return t('stage5Final.roomPrice.singleDayHint')
  }

  return t('stage5Final.roomPrice.multiDayHint')
})

function resolveWarningMessage(error: unknown, fallbackMessage: string) {
  if (error instanceof Error && error.message) {
    return error.message
  }
  return fallbackMessage
}

function normalizeDateRange(startDate: string, endDate: string) {
  if (startDate <= endDate) {
    return { startDate, endDate }
  }

  return {
    startDate: endDate,
    endDate: startDate,
  }
}

function parseYmdToLocalDate(value: string) {
  const [year, month, day] = value.split('-').map(Number)
  return new Date(year, (month || 1) - 1, day || 1)
}

function getWeekdayValue(date: string) {
  const weekday = parseYmdToLocalDate(date).getDay()
  if (weekday === 0) {
    return 7
  }
  return weekday
}

function buildWeekdaySelection(values: number[]) {
  const nextValues = values.filter((value) => FULL_WEEKDAY_VALUES.includes(value))
  nextValues.sort((left, right) => left - right)

  const hasFullWeek = FULL_WEEKDAY_VALUES.every((value) => nextValues.includes(value))
  if (hasFullWeek) {
    return [...ALL_WEEKDAY_VALUES]
  }

  return nextValues
}

function isUnavailableRecord(record: RoomPriceManagementDTO | null) {
  if (!record) {
    return false
  }

  if (record.closeRoom) {
    return true
  }

  if (typeof record.availableRooms === 'number' && record.availableRooms <= 0) {
    return true
  }

  return false
}

function getDisplayPriceText(record: RoomPriceManagementDTO | null) {
  if (!record) {
    return t('stage5DynamicUi.52')
  }

  return formatMoney(record.price)
}

function formatMoney(value?: number | null) {
  return formatBusinessMoney(Number(value || 0), currentCurrency.value, {
    maximumFractionDigits: 0,
  }, currentMoneyContext.value)
}

function getAvailabilityText(record: RoomPriceManagementDTO | null) {
  if (!record) {
    return t('stage5Final.roomPrice.noInventory')
  }

  if (record.closeRoom) {
    return t('stage5Final.roomPrice.closed')
  }

  if (typeof record.availableRooms === 'number') {
    if (record.availableRooms <= 0) {
      return t('stage5Final.roomPrice.soldOut')
    }
    return `${t('accommodation.roomTable.future.available')} ${record.availableRooms} ${t('settingsStage4.common.unitRooms')}`
  }

  return t('stage5Final.roomPrice.inventoryPending')
}

function getAvailabilityCompactText(record: RoomPriceManagementDTO | null) {
  if (!record) {
    return t('stage5Final.roomPrice.notConfigured')
  }

  if (record.closeRoom) {
    return t('accommodation.roomPrice.closeRoom')
  }

  if (typeof record.availableRooms === 'number') {
    if (record.availableRooms <= 0) {
      return t('stage5Final.roomPrice.soldOut')
    }
    return `${t('stage5DynamicUi.92')} ${record.availableRooms}`
  }

  return t('stage5Final.roomPrice.pending')
}

function getStayRestrictionText(record: RoomPriceManagementDTO | null) {
  if (!record) {
    return t('stage5Final.roomPrice.noRestriction')
  }

  const minStay = record.minStay ?? 1
  if (record.maxStay) {
    return `${t('roomStatus.action.checkIn')} ${minStay}-${record.maxStay} ${t('channel.dialogs.bookingSettings.nightUnit')}`
  }

  return `${t('stage5DynamicUi.122')} ${minStay} ${t('channel.dialogs.bookingSettings.nightUnit')}`
}

function getPriceSourceText(record: RoomPriceManagementDTO | null) {
  if (!record?.priceSource) {
    return t('stage5Final.roomPrice.sourceDefault')
  }

  if (record.priceSource === 'MANUAL') {
    return t('stage5Final.roomPrice.sourceManual')
  }

  if (record.priceSource === 'PRICELABS') {
    return t('stage5Final.roomPrice.sourcePriceLabs')
  }

  if (record.priceSource === 'SYSTEM') {
    return t('stage5Final.roomPrice.sourceSystem')
  }

  return `${t('stage5DynamicUi.129')}${record.priceSource}`
}

function getStatusTags(record: RoomPriceManagementDTO | null) {
  const tags: string[] = []

  if (!record) {
    return tags
  }

  if (typeof record.availableRooms === 'number') {
    if (record.availableRooms <= 0) {
      tags.push(t('stage5Final.roomPrice.soldOut'))
    } else {
      tags.push(t('stage5Final.roomPrice.remainingRooms', { count: record.availableRooms }))
    }
  }

  if (record.closeRoom) {
    tags.push(t('accommodation.roomPrice.closeRoom'))
  }
  if (record.cta) {
    tags.push('CTA')
  }
  if (record.ctd) {
    tags.push('CTD')
  }
  if (record.manualOverride) {
    tags.push(t('stage5Final.roomPrice.manualOverride'))
  }
  if (record.priceLabsBasePrice !== undefined && record.priceLabsBasePrice !== null) {
    tags.push('PriceLabs')
  }

  return tags
}

function getMatrixStatusText(record: RoomPriceManagementDTO | null) {
  if (!record) {
    return t('stage5DynamicUi.52')
  }

  const tags = getStatusTags(record).filter(
    (tag) => tag !== t('stage5Final.roomPrice.soldOut') && tag !== 'PriceLabs',
  )
  if (tags.length > 0) {
    return tags.slice(0, 2).join(' · ')
  }

  if (record.manualOverride) {
    return t('stage5Final.roomPrice.manualOverride')
  }

  return record.minStay && record.minStay > 1
    ? t('stage5Final.roomPrice.minimumStay', { count: record.minStay })
    : t('stage5Final.roomPrice.defaultStatus')
}

function getDateCardClass(record: RoomPriceManagementDTO | null) {
  return {
    'is-unavailable': isUnavailableRecord(record),
    'is-closed': Boolean(record?.closeRoom),
    'is-manual': Boolean(record?.manualOverride),
  }
}

function getMatrixCellClass(record: RoomPriceManagementDTO | null) {
  return {
    'is-unavailable': isUnavailableRecord(record),
    'is-closed': Boolean(record?.closeRoom),
    'is-manual': Boolean(record?.manualOverride),
    'is-empty': !record,
  }
}

function escapeCsvValue(value: string | number | boolean | null | undefined) {
  const text = value === null || value === undefined ? '' : String(value)
  const escapedText = text.replace(/"/g, '""')
  return `"${escapedText}"`
}

function downloadCsvFile(filename: string, content: string) {
  const blob = new Blob([`\uFEFF${content}`], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}

function handleExportCurrentView() {
  if (matrixRows.value.length === 0) {
    showWarningToast(t('stage5Final.roomPrice.noExportData'))
    return
  }

  const header = [
    t('stage5Final.roomPrice.csv.roomType'),
    t('stage5Final.roomPrice.csv.roomTypeCode'),
    t('stage5Final.roomPrice.csv.pricePlan'),
    t('stage5Final.roomPrice.csv.date'),
    t('stage5Final.roomPrice.csv.price'),
    t('stage5Final.roomPrice.csv.availableRooms'),
    t('stage5Final.roomPrice.csv.minStay'),
    t('stage5Final.roomPrice.csv.maxStay'),
    t('stage5Final.roomPrice.csv.closeRoom'),
    'CTA',
    'CTD',
    t('stage5Final.roomPrice.csv.priceSource'),
    t('stage5Final.roomPrice.csv.notes'),
  ]

  const rows: string[] = [header.map((item) => escapeCsvValue(item)).join(',')]

  for (const row of matrixRows.value) {
    for (const cell of row.cells) {
      rows.push(
        [
          row.roomTypeName,
          row.roomTypeCode || '',
          row.pricePlanName,
          cell.date,
          cell.record?.price ?? '',
          cell.record?.availableRooms ?? '',
          cell.record?.minStay ?? '',
          cell.record?.maxStay ?? '',
          cell.record?.closeRoom
            ? t('stage5Final.roomPrice.csv.yes')
            : t('stage5Final.roomPrice.csv.no'),
          cell.record?.cta
            ? t('stage5Final.roomPrice.csv.yes')
            : t('stage5Final.roomPrice.csv.no'),
          cell.record?.ctd
            ? t('stage5Final.roomPrice.csv.yes')
            : t('stage5Final.roomPrice.csv.no'),
          cell.record?.priceSource || '',
          cell.record?.notes || '',
        ]
          .map((item) => escapeCsvValue(item))
          .join(','),
      )
    }
  }

  downloadCsvFile(`room-price-${selectedDate.value}-${windowDays.value}d.csv`, rows.join('\n'))
  showSuccessToast(t('stage5Final.roomPrice.exportCompleted'))
}

async function loadReferenceData() {
  const [roomTypeResponse, roomTypeWithRoomsResponse, roomGroupResponse] = await Promise.all([
    getAllRoomTypes(),
    getAllRoomTypesWithRooms(),
    getAllRoomGroups(),
  ])

  if (!roomTypeResponse.success || !roomTypeResponse.data) {
    throw new Error(roomTypeResponse.message || t('settingsStage4.roomSort.messages.loadRoomTypesFailed'))
  }

  roomTypes.value = roomTypeResponse.data

  if (!roomGroupResponse.success || !roomGroupResponse.data) {
    roomGroups.value = []
    roomGroupRoomTypeIdsMap.value = {}
    hasLoadedReferenceData = true
    return
  }

  roomGroups.value = roomGroupResponse.data.filter(
    (group): group is RoomGroupOption => typeof group.id === 'number',
  )

  if (!roomTypeWithRoomsResponse.success || !roomTypeWithRoomsResponse.data) {
    roomGroupRoomTypeIdsMap.value = {}
    hasLoadedReferenceData = true
    return
  }

  const roomIdToRoomTypeId = new Map<number, number>()
  for (const roomType of roomTypeWithRoomsResponse.data) {
    for (const room of roomType.rooms || []) {
      roomIdToRoomTypeId.set(room.id, roomType.id)
    }
  }

  const groupMemberResults = await Promise.all(
    roomGroups.value.map(async (group) => {
      try {
        const memberResponse = await getGroupMembers(group.id)
        if (!memberResponse.success || !memberResponse.data) {
          return { groupId: group.id, roomTypeIds: [] as number[] }
        }

        const roomTypeIds = Array.from(
          new Set(
            memberResponse.data
              .map((member) => roomIdToRoomTypeId.get(member.roomId))
              .filter((roomTypeId): roomTypeId is number => typeof roomTypeId === 'number'),
          ),
        )

        return { groupId: group.id, roomTypeIds }
      } catch {
        return { groupId: group.id, roomTypeIds: [] as number[] }
      }
    }),
  )

  const nextMap: Record<number, number[]> = {}
  for (const result of groupMemberResults) {
    nextMap[result.groupId] = result.roomTypeIds
  }

  roomGroupRoomTypeIdsMap.value = nextMap
  hasLoadedReferenceData = true
}

async function loadPriceData() {
  const endDate = shiftDate(selectedDate.value, windowDays.value - 1)
  const response = await getRoomPriceManagementData(
    selectedDate.value,
    endDate,
    selectedRoomTypeId.value || undefined,
  )

  if (!response.success || !response.data) {
    throw new Error(response.message || t('stage5Pattern.loadFailed'))
  }

  records.value = response.data
}

async function loadPageData(forceReloadReferences = false) {
  loading.value = true
  errorMessage.value = ''

  try {
    const tasks = [loadPriceData()]
    if (forceReloadReferences || !hasLoadedReferenceData) {
      tasks.push(loadReferenceData())
    }
    await Promise.all(tasks)
  } catch (error) {
    const message = resolveWarningMessage(error, t('stage5Pattern.loadFailed'))
    errorMessage.value = message
    if (!isHandledRequestError(error)) {
      showWarningToast(message)
    }
  } finally {
    loading.value = false
  }
}

function resetEditor() {
  editorForm.value = {
    roomTypeId: 0,
    pricePlanId: 0,
    roomTypeName: '',
    pricePlanName: '',
    startDate: getTodayDate(),
    endDate: getTodayDate(),
    weekdays: [],
    settingType: 'price',
    price: '0',
    minStay: '1',
    maxStay: '',
    closeRoom: false,
    cta: false,
    ctd: false,
  }
  editorRecord.value = null
}

function isWeekdaySelected(value: number) {
  return editorForm.value.weekdays.includes(value)
}

function handleToggleWeekday(value: number) {
  if (value === 0) {
    if (editorForm.value.weekdays.includes(0)) {
      editorForm.value.weekdays = []
      return
    }

    editorForm.value.weekdays = [...ALL_WEEKDAY_VALUES]
    return
  }

  const nextValues = editorForm.value.weekdays.filter((item) => item !== 0 && item !== value)
  if (!editorForm.value.weekdays.includes(value)) {
    nextValues.push(value)
  }

  editorForm.value.weekdays = buildWeekdaySelection(nextValues)
}

function handleSelectAllWeekdays() {
  editorForm.value.weekdays = [...ALL_WEEKDAY_VALUES]
}

function handleInvertWeekdays() {
  const explicitSelected = editorForm.value.weekdays.filter((value) => value !== 0)
  const invertedValues = FULL_WEEKDAY_VALUES.filter((value) => !explicitSelected.includes(value))
  editorForm.value.weekdays = buildWeekdaySelection(invertedValues)
}

function handleOpenEditor(row: MatrixRowView, cell: PriceCellView) {
  editorRecord.value = cell.record
  editorForm.value = {
    roomTypeId: row.roomTypeId,
    pricePlanId: row.pricePlanId,
    roomTypeName: row.roomTypeName,
    pricePlanName: row.pricePlanName,
    startDate: cell.date,
    endDate: cell.date,
    weekdays: [],
    settingType: 'price',
    price: cell.record?.price !== undefined ? String(cell.record.price) : '0',
    minStay: cell.record?.minStay !== undefined ? String(cell.record.minStay) : '1',
    maxStay: cell.record?.maxStay !== undefined ? String(cell.record.maxStay) : '',
    closeRoom: Boolean(cell.record?.closeRoom),
    cta: Boolean(cell.record?.cta),
    ctd: Boolean(cell.record?.ctd),
  }
  editorOpen.value = true
}

function handleCloseEditor() {
  editorOpen.value = false
  resetEditor()
}

function createBulkDateRange(startDate = selectedDate.value, endDate = selectedDate.value): BulkDateRangeItem {
  return {
    startDate,
    endDate,
  }
}

function syncBulkFormWithCurrentView() {
  const nextRoomTypeIds = bulkSelectableRoomTypes.value.map((roomType) => roomType.id)
  bulkForm.value = {
    ...createDefaultBulkForm(),
    roomTypeIds: nextRoomTypeIds,
    dateRanges: [createBulkDateRange(selectedDate.value, selectedDate.value)],
  }
}

function handleOpenBulkUpdate() {
  if (bulkSelectableRoomTypes.value.length === 0) {
    showWarningToast(t('stage5Final.roomPrice.noBulkRoomTypes'))
    return
  }

  syncBulkFormWithCurrentView()
  bulkUpdateOpen.value = true
}

function handleCloseBulkUpdate() {
  bulkUpdateOpen.value = false
  bulkForm.value = createDefaultBulkForm()
}

function handleSelectAllBulkRoomTypes() {
  bulkForm.value.roomTypeIds = bulkSelectableRoomTypes.value.map((roomType) => roomType.id)
}

function handleClearBulkRoomTypes() {
  bulkForm.value.roomTypeIds = []
}

function handleToggleBulkRoomType(roomTypeId: number) {
  if (bulkForm.value.roomTypeIds.includes(roomTypeId)) {
    bulkForm.value.roomTypeIds = bulkForm.value.roomTypeIds.filter((item) => item !== roomTypeId)
    return
  }

  bulkForm.value.roomTypeIds = [...bulkForm.value.roomTypeIds, roomTypeId]
}

function handleAddBulkDateRange() {
  if (bulkForm.value.dateRanges.length >= MAX_BULK_DATE_RANGES) {
    showWarningToast(t('roomStatus.roomPrice.bulkDateRangeLimit', { count: MAX_BULK_DATE_RANGES }))
    return
  }

  bulkForm.value.dateRanges = [...bulkForm.value.dateRanges, createBulkDateRange()]
}

function handleRemoveBulkDateRange(index: number) {
  bulkForm.value.dateRanges = bulkForm.value.dateRanges.filter((_, currentIndex) => currentIndex !== index)
}

function handleSelectAllBulkWeekdays() {
  bulkForm.value.weekdays = [...BULK_WEEKDAY_VALUES]
}

function handleClearBulkWeekdays() {
  bulkForm.value.weekdays = []
}

function handleToggleBulkWeekday(weekday: number) {
  if (bulkForm.value.weekdays.includes(weekday)) {
    bulkForm.value.weekdays = bulkForm.value.weekdays.filter((item) => item !== weekday)
    return
  }

  bulkForm.value.weekdays = [...bulkForm.value.weekdays, weekday].sort((left, right) => {
    return BULK_WEEKDAY_VALUES.indexOf(left) - BULK_WEEKDAY_VALUES.indexOf(right)
  })
}

async function handleSubmitBulkUpdate() {
  if (bulkForm.value.roomTypeIds.length === 0) {
    showWarningToast(t('stage5Pattern.atLeast'))
    return
  }

  const normalizedDateRanges = bulkForm.value.dateRanges
    .filter((range) => range.startDate && range.endDate)
    .map((range) => normalizeDateRange(range.startDate, range.endDate))

  if (normalizedDateRanges.length === 0) {
    showWarningToast(t('stage5Pattern.atLeast'))
    return
  }

  if (bulkForm.value.weekdays.length === 0) {
    showWarningToast(t('stage5Pattern.atLeast'))
    return
  }

  const weekdayPrice = Number(bulkForm.value.weekdayPrice)
  if (!Number.isFinite(weekdayPrice) || weekdayPrice < 0) {
    showWarningToast(t('stage5Pattern.enter'))
    return
  }

  let weekendPrice: number | undefined
  if (bulkForm.value.weekendDifferentiation) {
    weekendPrice = Number(bulkForm.value.weekendPrice)
    if (!Number.isFinite(weekendPrice) || weekendPrice < 0) {
      showWarningToast(t('stage5Pattern.enter'))
      return
    }
  }

  const requestData: BulkPriceChangeRequest = {
    roomTypeIds: [...bulkForm.value.roomTypeIds],
    dateRanges: normalizedDateRanges,
    weekdays:
      bulkForm.value.weekdays.length === BULK_WEEKDAY_VALUES.length
        ? undefined
        : [...bulkForm.value.weekdays],
    weekendDifferentiation: bulkForm.value.weekendDifferentiation,
    weekdayPrice,
    weekendPrice,
    notes: bulkForm.value.notes.trim() || undefined,
  }

  bulkSubmitting.value = true
  try {
    const response = await bulkPriceChange(requestData)
    if (!response.success) {
      throw new Error(response.message || t('stage5Pattern.updateFailed'))
    }

    showSuccessToast(response.message || t('roomStatus.roomPrice.bulkUpdateSuccess'))
    bulkUpdateOpen.value = false
    await loadPageData()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, t('stage5Pattern.updateFailed')))
    }
  } finally {
    bulkSubmitting.value = false
  }
}

function hasMatchedWeekdayInRange(startDate: string, endDate: string, weekdays: number[]) {
  if (weekdays.length === 0 || weekdays.includes(0)) {
    return true
  }

  const weekdaySet = new Set(weekdays)
  const start = parseYmdToLocalDate(startDate)
  const end = parseYmdToLocalDate(endDate)
  const cursor = new Date(start)

  while (cursor <= end) {
    const currentWeekday = cursor.getDay() === 0 ? 7 : cursor.getDay()
    if (weekdaySet.has(currentWeekday)) {
      return true
    }
    cursor.setDate(cursor.getDate() + 1)
  }

  return false
}

function buildSaveSuccessMessage(settingType: SettingType) {
  if (settingType === 'price') {
    return t('roomStatus.roomPrice.saveSuccess.price')
  }
  if (settingType === 'minStay') {
    return t('roomStatus.roomPrice.saveSuccess.minStay')
  }
  if (settingType === 'maxStay') {
    return t('roomStatus.roomPrice.saveSuccess.maxStay')
  }
  if (settingType === 'closeRoom') {
    return t('roomStatus.roomPrice.saveSuccess.closeRoom')
  }
  if (settingType === 'cta') {
    return t('roomStatus.roomPrice.saveSuccess.cta')
  }
  return t('roomStatus.roomPrice.saveSuccess.ctd')
}

async function handleSaveEditor() {
  if (!editorForm.value.roomTypeId || !editorForm.value.pricePlanId) {
    showWarningToast(t('stage5Pattern.unavailable'))
    return
  }

  if (!editorForm.value.startDate || !editorForm.value.endDate) {
    showWarningToast(t('stage5Pattern.select'))
    return
  }

  const normalizedRange = normalizeDateRange(editorForm.value.startDate, editorForm.value.endDate)
  const normalizedWeekdays = [...editorForm.value.weekdays]
  const isSingleDayRange = normalizedRange.startDate === normalizedRange.endDate
  const hasExplicitWeekdaySelection = normalizedWeekdays.length > 0
  const applyWeekdaysInRange = hasExplicitWeekdaySelection ? !isSingleDayRange : true

  let payloadWeekdays: number[] | undefined
  if (hasExplicitWeekdaySelection) {
    const containsAllWeekdays = normalizedWeekdays.includes(0)
    if (!(applyWeekdaysInRange && containsAllWeekdays)) {
      payloadWeekdays = normalizedWeekdays
    }
  }

  if (
    applyWeekdaysInRange &&
    payloadWeekdays &&
    !payloadWeekdays.includes(0) &&
    !hasMatchedWeekdayInRange(normalizedRange.startDate, normalizedRange.endDate, payloadWeekdays)
  ) {
    showWarningToast(t('stage5Final.roomPrice.noMatchingWeekday'))
    return
  }

  const requestData: UpdatePriceByPlanRequest = {
    roomTypeId: editorForm.value.roomTypeId,
    pricePlanId: editorForm.value.pricePlanId,
    startDate: normalizedRange.startDate,
    endDate: normalizedRange.endDate,
    weekdays: payloadWeekdays,
    applyWeekdaysInRange,
  }

  if (editorForm.value.settingType === 'price') {
    const price = Number(editorForm.value.price)
    if (!Number.isFinite(price) || price < 0) {
      showWarningToast(t('accommodation.roomPriceBulk.messages.invalidPrice'))
      return
    }
    requestData.price = price
  }

  if (editorForm.value.settingType === 'minStay') {
    const minStay = Number(editorForm.value.minStay)
    if (!Number.isFinite(minStay) || minStay <= 0 || minStay > MAX_STAY_LIMIT) {
      showWarningToast(t('stage5Pattern.enter'))
      return
    }
    if (typeof editorRecord.value?.maxStay === 'number' && minStay > editorRecord.value.maxStay) {
      showWarningToast(t('stage5Pattern.unsupported'))
      return
    }
    requestData.minStay = minStay
  }

  if (editorForm.value.settingType === 'maxStay') {
    const maxStay = Number(editorForm.value.maxStay)
    if (!Number.isFinite(maxStay) || maxStay <= 0 || maxStay > MAX_STAY_LIMIT) {
      showWarningToast(t('stage5Pattern.enter'))
      return
    }
    const currentMinStay = editorRecord.value?.minStay ?? 1
    if (maxStay < currentMinStay) {
      showWarningToast(t('stage5Pattern.unsupported'))
      return
    }
    requestData.maxStay = maxStay
  }

  if (editorForm.value.settingType === 'closeRoom') {
    requestData.closeRoom = editorForm.value.closeRoom
  }

  if (editorForm.value.settingType === 'cta') {
    requestData.cta = editorForm.value.cta
  }

  if (editorForm.value.settingType === 'ctd') {
    requestData.ctd = editorForm.value.ctd
  }

  const operator = resolveUserLabel(userStore.currentUser?.nickname, userStore.currentUser?.email)

  saving.value = true
  try {
    const response = await updatePriceByPlan(requestData, operator)
    if (!response.success) {
      throw new Error(response.message || t('stage5Pattern.saveFailed'))
    }

    const successMessage = response.message || buildSaveSuccessMessage(editorForm.value.settingType)
    showSuccessToast(successMessage)
    handleCloseEditor()
    await loadPageData()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, t('stage5Pattern.saveFailed')))
    }
  } finally {
    saving.value = false
  }
}

async function handleShiftDate(offsetDays: number) {
  selectedDate.value = shiftDate(selectedDate.value, offsetDays)
  await loadPageData()
}

async function handleChangeWindowDays(days: number) {
  if (windowDays.value === days) {
    return
  }

  windowDays.value = days
  await loadPageData()
}

async function handleGoToday() {
  selectedDate.value = getTodayDate()
  await loadPageData()
}

async function handleDateInput(event: Event) {
  const target = event.target as HTMLInputElement | null
  if (!target?.value) {
    return
  }

  selectedDate.value = target.value
  await loadPageData()
}

async function handleRoomTypeChange(event: Event) {
  const target = event.target as HTMLSelectElement | null
  if (!target) {
    return
  }

  if (!target.value) {
    selectedRoomTypeId.value = null
  } else {
    selectedRoomTypeId.value = Number(target.value)
  }

  await loadPageData()
}

function handleRoomGroupChange(event: Event) {
  const target = event.target as HTMLSelectElement | null
  if (!target) {
    return
  }

  if (!target.value) {
    selectedRoomGroupId.value = null
    return
  }

  selectedRoomGroupId.value = Number(target.value)
}

async function handleRefresh(event: CustomEvent) {
  await loadPageData(true)
  event.detail.complete()
}

async function handleOpenHistory() {
  await router.push(ROUTE_PATHS.roomsPricingHistory)
}

onIonViewWillEnter(async () => {
  await loadPageData(true)
})
</script>

<style scoped>
.room-price-page {
  display: block;
}

.room-price-page__history-button {
  --color: var(--ios-pms-header-control-color);
}

.room-price-page__hero {
  margin-top: 4px;
}

.room-price-page__eyebrow {
  color: var(--ion-color-primary);
  font-weight: 700;
}

.room-price-page__toolbar-card,
.room-price-page__editor-card {
  display: grid;
  gap: 14px;
}

.room-price-page__toolbar-row {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.room-price-page__toolbar-row--compact,
.room-price-page__toolbar-row--actions {
  align-items: center;
}

.room-price-page__preset-chip {
  min-height: 38px;
  padding: 0 14px;
  border: 1px solid rgba(15, 23, 42, 0.12);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.92);
  color: var(--app-muted);
  font: inherit;
}

.room-price-page__preset-chip.is-active {
  border-color: var(--ion-color-primary);
  background: rgba(59, 130, 246, 0.14);
  color: var(--ion-color-primary);
  font-weight: 700;
}

.room-price-page__filter-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.room-price-page__field {
  display: grid;
  gap: 8px;
}

.room-price-page__field--full {
  grid-column: 1 / -1;
}

.room-price-page__field span,
.room-price-page__weekday-section span {
  color: var(--app-heading);
  font-size: 13px;
  font-weight: 600;
}

.room-price-page__field input,
.room-price-page__field select,
.room-price-page__field textarea {
  box-sizing: border-box;
  width: 100%;
  min-height: 44px;
  padding: 10px 8px;
  border: 1px solid rgba(15, 23, 42, 0.12);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.92);
  color: var(--app-heading);
  font: inherit;
  text-align: center;
}

.room-price-page__field textarea {
  min-height: 96px;
  resize: vertical;
  text-align: left;
}

.room-price-page__error {
  color: var(--ion-color-danger);
}

.room-price-page__section-header {
  align-items: flex-start;
}

.room-price-page__summary-row {
  margin-top: 12px;
}

.room-price-page__matrix-shell {
  margin-top: 16px;
  border: 1px solid var(--app-border);
  border-radius: 20px;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.88);
}

.room-price-page__matrix-scroll {
  overflow-x: auto;
  overflow-y: hidden;
}

.room-price-page__matrix-row {
  display: grid;
  min-width: max-content;
}

.room-price-page__matrix-row--header {
  position: sticky;
  top: 0;
  z-index: 4;
}

.room-price-page__matrix-leading,
.room-price-page__matrix-header-cell,
.room-price-page__matrix-cell {
  min-height: 88px;
  padding: 10px;
  border-right: 1px solid rgba(15, 23, 42, 0.08);
  border-bottom: 1px solid rgba(15, 23, 42, 0.08);
}

.room-price-page__matrix-leading {
  position: sticky;
  left: 0;
  z-index: 3;
  display: grid;
  align-content: center;
  gap: 6px;
  background: rgba(248, 250, 252, 0.98);
}

.room-price-page__matrix-leading--header {
  z-index: 5;
}

.room-price-page__matrix-leading strong,
.room-price-page__matrix-header-cell strong,
.room-price-page__matrix-cell strong {
  color: var(--app-heading);
  font-size: 13px;
}

.room-price-page__matrix-leading span,
.room-price-page__matrix-leading small,
.room-price-page__matrix-header-cell span,
.room-price-page__matrix-cell span,
.room-price-page__matrix-cell small {
  color: var(--app-muted);
  font-size: 11px;
  line-height: 1.4;
}

.room-price-page__matrix-header-cell {
  display: grid;
  align-content: center;
  justify-items: center;
  gap: 4px;
  background: rgba(248, 250, 252, 0.98);
}

.room-price-page__matrix-header-cell.is-today {
  background: rgba(59, 130, 246, 0.12);
}

.room-price-page__matrix-header-cell.is-weekend {
  color: var(--ion-color-primary);
}

.room-price-page__matrix-cell {
  appearance: none;
  border-top: none;
  border-left: none;
  border-bottom: 1px solid rgba(15, 23, 42, 0.08);
  border-right: 1px solid rgba(15, 23, 42, 0.08);
  background: rgba(255, 255, 255, 0.96);
  display: grid;
  align-content: center;
  gap: 6px;
  text-align: left;
}

.room-price-page__matrix-cell.is-empty {
  background: rgba(248, 250, 252, 0.74);
}

.room-price-page__matrix-cell.is-unavailable {
  background: rgba(244, 63, 94, 0.08);
}

.room-price-page__matrix-cell.is-closed {
  box-shadow: inset 0 0 0 1px rgba(244, 63, 94, 0.18);
}

.room-price-page__matrix-cell.is-manual {
  box-shadow: inset 0 0 0 1px rgba(37, 99, 235, 0.18);
}

.room-price-page__matrix-cell strong {
  color: var(--ion-color-primary);
  font-size: 14px;
}

.room-price-page__group-list {
  margin-top: 16px;
}

.room-price-page__group-card,
.room-price-page__plan-card {
  display: grid;
  gap: 12px;
  padding: 14px;
  border-radius: 18px;
  border: 1px solid var(--app-border);
  background: rgba(255, 255, 255, 0.82);
}

.room-price-page__group-header strong,
.room-price-page__group-header p,
.room-price-page__plan-header strong,
.room-price-page__plan-header p {
  margin: 0;
}

.room-price-page__group-header p,
.room-price-page__plan-header p {
  margin-top: 6px;
  color: var(--app-muted);
  font-size: 13px;
}

.room-price-page__date-strip {
  display: flex;
  gap: 10px;
  overflow-x: auto;
  padding-bottom: 4px;
}

.room-price-page__date-card {
  flex: 0 0 112px;
  display: grid;
  gap: 6px;
  padding: 12px;
  border: none;
  border-radius: 16px;
  background: rgba(16, 35, 63, 0.04);
  color: var(--app-muted);
  text-align: left;
}

.room-price-page__date-card.is-unavailable {
  background: rgba(244, 63, 94, 0.08);
}

.room-price-page__date-card.is-closed {
  border: 1px solid rgba(244, 63, 94, 0.18);
}

.room-price-page__date-card.is-manual {
  box-shadow: inset 0 0 0 1px rgba(37, 99, 235, 0.16);
}

.room-price-page__date-card strong,
.room-price-page__editor-title strong {
  color: var(--app-heading);
  font-size: 14px;
}

.room-price-page__date-card b {
  color: var(--ion-color-primary);
  font-size: 16px;
}

.room-price-page__date-card small {
  line-height: 1.5;
}

.room-price-page__modal-page {
  --padding-top: 16px;
  --padding-bottom: 24px;
  --padding-start: 16px;
  --padding-end: 16px;
}

.room-price-page__editor-title {
  display: grid;
  gap: 6px;
}

.room-price-page__editor-title span {
  color: var(--app-muted);
  font-size: 13px;
}

.room-price-page__editor-current {
  display: grid;
  gap: 8px;
  padding: 12px 14px;
  border-radius: 18px;
  background: rgba(16, 35, 63, 0.04);
}

.room-price-page__editor-current-header,
.room-price-page__weekday-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.room-price-page__editor-current-header span {
  color: var(--app-muted);
  font-size: 12px;
}

.room-price-page__bulk-section {
  display: grid;
  gap: 12px;
}

.room-price-page__bulk-section-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.room-price-page__selection-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.room-price-page__selection-chip {
  min-height: 40px;
  padding: 0 14px;
  border: 1px solid rgba(15, 23, 42, 0.12);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.96);
  color: var(--app-muted);
  font: inherit;
}

.room-price-page__selection-chip.is-active {
  border-color: var(--ion-color-primary);
  background: rgba(59, 130, 246, 0.14);
  color: var(--ion-color-primary);
  font-weight: 700;
}

.room-price-page__bulk-range-list {
  display: grid;
  gap: 12px;
}

.room-price-page__bulk-range-item {
  display: grid;
  gap: 10px;
  padding: 12px;
  border-radius: 18px;
  background: rgba(248, 250, 252, 0.82);
}

.room-price-page__weekday-section {
  display: grid;
  gap: 10px;
}

.room-price-page__weekday-actions,
.room-price-page__tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.room-price-page__tag-row span {
  padding: 2px 8px;
  border-radius: 999px;
  background: var(--app-primary-soft);
  color: var(--ion-color-primary);
  font-size: 11px;
  font-weight: 600;
}

.room-price-page__weekday-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.room-price-page__weekday-button {
  min-height: 40px;
  padding: 0 14px;
  border: 1px solid rgba(15, 23, 42, 0.12);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.92);
  color: var(--app-muted);
  font: inherit;
}

.room-price-page__weekday-button.is-active {
  border-color: var(--app-border-strong);
  background: var(--app-primary-soft-strong);
  color: var(--ion-color-primary);
  font-weight: 700;
}

.room-price-page__weekday-hint {
  margin: 0;
}

.room-price-page__toggle-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 14px;
  border-radius: 18px;
  background: var(--app-primary-soft);
}

.room-price-page__toggle-row strong,
.room-price-page__toggle-row p {
  margin: 0;
}

.room-price-page__toggle-row p {
  margin-top: 6px;
  color: var(--app-muted);
  font-size: 12px;
}

.room-price-page__editor-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  flex-wrap: wrap;
}

@media (max-width: 520px) {
  .room-price-page__filter-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .room-price-page__matrix-leading,
  .room-price-page__matrix-header-cell,
  .room-price-page__matrix-cell {
    min-height: 84px;
    padding: 8px;
  }
}
</style>
