<template>
  <div class="channel-management">
    <!-- 主内容区域 -->
    <div class="main-container">
      <template v-if="activeSection === 'channel-list'">
        <ChannelSettingsPanel
          v-if="showChannelSettings"
          :channel="selectedChannel"
          :is-airbnb="isAirbnbChannel"
          :airbnb-accounts="airbnbAccountList"
          :hotels="hotelList"
          :flattened-mapping-data="flattenedMappingData"
          :selected-hotel-id="selectedHotelId"
          :pms-room-options="pmsRoomOptions"
          :pms-price-plan-options="pmsPricePlanOptions"
          :editing-room-id="editingRoomId"
          :calendar-data="calendarData"
          :calendar-dates="calendarDates"
          :room-settings-data="roomSettingsData"
          :room-settings-dates="roomSettingsDates"
          :room-settings-start-date="roomSettingsStartDate"
          @close="closeChannelSettings"
          @show-history="handleShowHistory"
          @add-account="handleAddAccount"
          @add-hotel="handleAddHotel"
          @disconnect-account="handleDisconnectAccount"
          @edit-hotel="handleEditHotel"
          @disconnect-hotel="handleDisconnectHotel"
          @update:selected-hotel-id="handleUpdateSelectedHotelId"
          @update:editing-room-id="editingRoomId = $event"
          @update:room-settings-start-date="handleRoomSettingsStartDateChange"
          @refresh-channel-info="handleRefreshChannelInfo"
          @import-orders="handleImportOrders"
          @edit-mapping="handleEditMapping"
          @save-mapping="handleSaveMapping"
          @cancel-edit-mapping="handleCancelEditMapping"
          @disconnect-mapping="handleDisconnectMapping"
          @manage-mapping="handleManageMapping"
          @sync-from-calendar="handleSyncFromCalendar"
          @full-refresh="handleFullRefresh"
        />
        <ChannelList
          v-else
          v-loading="channelsLoading"
          :channels="otaChannels"
          @connect="handleConnectChannel"
          @manage="openSettings"
        />
      </template>

      <PriceRatioPanel
        v-else-if="activeSection === 'price-ratio'"
        :loading="priceRatioLoading"
        v-loading="priceRatioLoading"
        :data="priceRatioData"
        @edit="handleEditPriceRatio"
      />
    </div>

    <!-- ──────────────── 弹窗 / 抽屉组件 ──────────────── -->

    <!-- 直连须知 -->
    <ConnectNoticeDialog
      v-model="showConnectNoticeDialog"
      :channel="selectedChannel"
      @confirm="handleConfirmNotice"
    />

    <!-- 添加账户（Airbnb） -->
    <AddAccountDialog v-model="showAddAccountDialog" @connect="handleAirbnbConnect" />

    <!-- 添加酒店（通用） -->
    <AddHotelDialog
      v-model="showAddHotelDialog"
      :channel="selectedChannel"
      @authorize="handleHotelAuthorize"
    />

    <!-- 价格比例编辑 -->
    <EditPriceRatioDialog
      v-model="showEditPriceRatioDialog"
      :edit-data="editingPriceRatio"
      @save="handleSavePriceRatio"
    />

    <!-- 连接历史 -->
    <ConnectionHistoryDialog v-model="showHistoryDialog" :history="historyList" />

    <!-- 预订设置（Airbnb 房量管理） -->
    <BookingSettingsDrawer
      v-model="showBookingSettingsDrawer"
      :settings="bookingSettings"
      @save="handleSaveBookingSettings"
    />

    <!-- Su Widget连接组件 -->
    <ConnectOtaWidget
      v-model="showWidgetDialog"
      :ota-id="selectedOtaId"
      :ota-name="selectedOtaName"
      @success="handleWidgetSuccess"
      @synced="handleWidgetSynced"
      @error="handleWidgetError"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'

// Foundational modules
import type {
  ChannelItem,
  HotelItem,
  AirbnbAccount,
  FlattenedMappingItem,
  RoomMappingGroup,
  CalendarDate,
  CalendarRow,
  RoomSettingsRow,
  BookingSettings,
  PriceRatioEdit,
} from './types'

import { useChannelData } from './composables/useChannelData'
import { getStoreTodayYmd } from '@/utils/storeDateTime'

// Dialogs & Drawers
import ConnectNoticeDialog from './components/dialogs/ConnectNoticeDialog.vue'
import AddAccountDialog from './components/dialogs/AddAccountDialog.vue'
import AddHotelDialog from './components/dialogs/AddHotelDialog.vue'
import EditPriceRatioDialog from './components/dialogs/EditPriceRatioDialog.vue'
import ConnectionHistoryDialog from './components/dialogs/ConnectionHistoryDialog.vue'
import BookingSettingsDrawer from './components/dialogs/BookingSettingsDrawer.vue'

// Panels
import ChannelList from './components/ChannelList.vue'
import ChannelSettingsPanel from './components/ChannelSettingsPanel.vue'
import PriceRatioPanel from './components/PriceRatioPanel.vue'

// ConnectOtaWidget (Original widget)
import ConnectOtaWidget from '@/components/ConnectOtaWidget.vue'

// Composable initialization
const {
  otaChannels,
  channelsLoading,
  loadChannels,
  pmsRoomOptions,
  pmsPricePlanOptions,
  loadPmsRoomOptions,
  loadPmsPricePlanOptions,
  priceRatioData,
  priceRatioLoading,
  loadPriceRatioData,
  savePriceRatio,
  flattenMappingData,
  buildConnectedChannelManagementData,
} = useChannelData()

const { t } = useI18n()
const route = useRoute()

// Navigation & panels status
const activeSection = computed<'channel-list' | 'price-ratio'>(() =>
  route.name === 'ChannelPriceRatioSettings' ? 'price-ratio' : 'channel-list',
)
const showChannelSettings = ref(false)
const selectedChannel = ref<ChannelItem | null>(null)

const isAirbnbChannel = computed(() => {
  return selectedChannel.value?.name === 'Airbnb'
})

// Sub-panel state properties
const airbnbAccountList = ref<AirbnbAccount[]>([])
const hotelList = ref<HotelItem[]>([])
const roomMappingData = ref<RoomMappingGroup[]>([])
const flattenedMappingData = ref<FlattenedMappingItem[]>([])
const selectedHotelId = ref<number | null>(null)
const editingRoomId = ref<string | null>(null)

// Calendar state properties
const calendarStartDate = ref(getStoreTodayYmd())
const calendarDates = ref<CalendarDate[]>([])
const calendarData = ref<CalendarRow[]>([])

// Airbnb Room Settings properties
const roomSettingsStartDate = ref(getStoreTodayYmd())
const roomSettingsDates = ref<CalendarDate[]>([])
const roomSettingsData = ref<RoomSettingsRow[]>([])

// Dialog visibility state properties
const showConnectNoticeDialog = ref(false)
const showAddAccountDialog = ref(false)
const showAddHotelDialog = ref(false)
const showEditPriceRatioDialog = ref(false)
const showHistoryDialog = ref(false)
const showBookingSettingsDrawer = ref(false)

// Edit state properties
const editingPriceRatio = ref<PriceRatioEdit | null>(null)
const historyList = ref<any[]>([])
const bookingSettings = ref<BookingSettings>({
  advanceBookingHours: 2,
  requireApproval: true,
  preparationNights: 0,
  bookingWindowDays: 365,
  checkInStartTime: '16:00',
  checkInEndTime: '',
  checkOutTime: '10:00',
})

// Su Widget connection state properties
const showWidgetDialog = ref(false)
const selectedOtaId = ref<number>(0)
const selectedOtaName = ref<string>('')

// ──────────── Component Actions ────────────

/** Flatten mapping data helper */
const updateFlattenedMappingData = () => {
  flattenedMappingData.value = flattenMappingData(roomMappingData.value)
}

const resetChannelManagementData = () => {
  airbnbAccountList.value = []
  hotelList.value = []
  selectedHotelId.value = null
  roomMappingData.value = []
  flattenedMappingData.value = []
  calendarDates.value = []
  calendarData.value = []
  roomSettingsData.value = []
  roomSettingsDates.value = []
}

const loadChannelManagementData = async (
  channel: ChannelItem,
  startDate = roomSettingsStartDate.value,
) => {
  resetChannelManagementData()
  try {
    const data = await buildConnectedChannelManagementData(channel, startDate)
    hotelList.value = data.hotels
    airbnbAccountList.value = data.airbnbAccounts
    selectedHotelId.value = data.selectedHotelId
    roomMappingData.value = data.roomMappings
    flattenedMappingData.value = flattenMappingData(data.roomMappings)
    calendarDates.value = data.calendarDates
    calendarData.value = data.calendarData
    roomSettingsDates.value = data.roomSettingsDates
    roomSettingsData.value = data.roomSettingsData
  } catch (error) {
    console.error('加载渠道管理数据失败:', error)
    ElMessage.error(t('channel.messages.loadManagementDataFailed'))
  }
}

/** Open channel settings details panel */
const openSettings = async (channel: ChannelItem) => {
  selectedChannel.value = channel
  showChannelSettings.value = true
  editingRoomId.value = null
  await loadChannelManagementData(channel)
}

/** Close channel settings details panel */
const closeChannelSettings = () => {
  showChannelSettings.value = false
  selectedChannel.value = null
  resetChannelManagementData()
}

/** Switch selected hotel */
const handleUpdateSelectedHotelId = (val: number | null) => {
  selectedHotelId.value = val
}

/** Handle date settings change on Airbnb settings */
const handleRoomSettingsStartDateChange = (val: string) => {
  roomSettingsStartDate.value = val
  if (selectedChannel.value) {
    loadChannelManagementData(selectedChannel.value, val)
  }
}

// ──────────── Hotel Actions ────────────

const handleEditHotel = (row: HotelItem) => {
  ElMessage.info(t('channel.messages.editHotel', { name: row.hotelName }))
}

const showChannelWriteNotReady = () => {
  ElMessage.warning(t('channel.messages.channelWriteNotReady'))
}

const handleDisconnectHotel = (_row: HotelItem) => {
  showChannelWriteNotReady()
}

const handleDisconnectAccount = (_row: AirbnbAccount) => {
  showChannelWriteNotReady()
}

// ──────────── Mapping Actions ────────────

const handleEditMapping = (_row: FlattenedMappingItem) => {
  showChannelWriteNotReady()
}

const handleCancelEditMapping = () => {
  editingRoomId.value = null
  updateFlattenedMappingData()
}

const handleSaveMapping = (_roomGroupId: string) => {
  showChannelWriteNotReady()
}

const handleDisconnectMapping = (_row: FlattenedMappingItem) => {
  showChannelWriteNotReady()
}

const handleManageMapping = (_row: FlattenedMappingItem) => {
  showChannelWriteNotReady()
}

const handleSaveBookingSettings = (_newSettings: BookingSettings) => {
  showChannelWriteNotReady()
}

// ──────────── Dialog Triggers ────────────

const handleShowHistory = () => {
  historyList.value = []
  showHistoryDialog.value = true
}

const handleAddAccount = () => {
  showAddAccountDialog.value = true
}

const handleAddHotel = () => {
  showAddHotelDialog.value = true
}

const handleAirbnbConnect = () => {
  showAddAccountDialog.value = false
  ElMessageBox.alert(
    t('channel.messages.airbnbAuthNotice'),
    t('channel.dialogs.addAccount.title'),
    {
      confirmButtonText: t('channel.dialogs.common.understood'),
      type: 'info',
    },
  )
}

const handleHotelAuthorize = () => {
  showAddHotelDialog.value = false
  ElMessageBox.alert(
    t('channel.messages.channelAuthNotice'),
    t('channel.dialogs.addHotel.title', {
      name: selectedChannel.value?.name || t('channel.dialogs.addHotel.fallbackChannel'),
    }),
    {
      confirmButtonText: t('channel.dialogs.common.understood'),
      type: 'info',
    },
  )
}

// ──────────── Price Ratio Actions ────────────

const handleEditPriceRatio = (row: any) => {
  const adjustmentValue = row.adjustmentValue ?? 0
  editingPriceRatio.value = {
    channelId: row.channelId,
    channel: row.channel,
    ratio: row.ratio,
    adjustmentType: adjustmentValue > 0 ? 'expensive' : 'cheaper',
    adjustmentValue: Math.abs(adjustmentValue),
    adjustmentUnit: row.adjustmentType === 'FIXED' ? '¥' : '%',
    autoSyncPrice: row.autoSyncPrice,
    backendAdjustmentType: row.adjustmentType,
  }
  showEditPriceRatioDialog.value = true
}

const handleSavePriceRatio = async (editData: PriceRatioEdit) => {
  const success = await savePriceRatio(editData)
  if (success) {
    showEditPriceRatioDialog.value = false
    editingPriceRatio.value = null
  }
}

// ──────────── Connection Widgets ────────────

const handleConnectChannel = (channel: ChannelItem) => {
  selectedChannel.value = channel
  if (channel.connected) {
    selectedOtaId.value = channel.id
    selectedOtaName.value = channel.name
    showWidgetDialog.value = true
    return
  }
  showConnectNoticeDialog.value = true
}

const handleConfirmNotice = () => {
  showConnectNoticeDialog.value = false
  if (selectedChannel.value) {
    selectedOtaId.value = selectedChannel.value.id
    selectedOtaName.value = selectedChannel.value.name
    showWidgetDialog.value = true
  }
}

const handleWidgetSuccess = () => {
  if (selectedChannel.value) {
    ElMessage.success(t('channel.messages.widgetConnected', { name: selectedChannel.value.name }))
    loadChannels()
  }
  showWidgetDialog.value = false
  selectedChannel.value = null
}

const handleWidgetSynced = () => {
  loadChannels()
}

const handleWidgetError = (error: string) => {
  console.error(t('channel.messages.widgetError'), error)
}

// ──────────── Extra Tab Buttons ────────────

const handleRefreshChannelInfo = () => {
  ElMessage.warning(t('channel.messages.refreshNotReady'))
}

const handleImportOrders = () => {
  ElMessage.warning(t('channel.messages.importNotReady'))
}

const handleSyncFromCalendar = () => {
  ElMessage.warning(t('channel.messages.calendarSyncNotReady'))
}

const handleFullRefresh = () => {
  ElMessage.warning(t('channel.messages.fullRefreshNotReady'))
}

onMounted(() => {
  loadChannels()
  loadPmsRoomOptions()
  loadPmsPricePlanOptions()
  if (activeSection.value === 'price-ratio') {
    loadPriceRatioData()
  }
})

watch(activeSection, (section) => {
  showChannelSettings.value = false
  if (section === 'price-ratio') {
    loadPriceRatioData()
  }
})

watch(
  () => t('channel.settings.directSettings'),
  () => {
    if (showChannelSettings.value && selectedChannel.value) {
      loadChannelManagementData(selectedChannel.value)
    }
  },
)
</script>

<style scoped>
.channel-management {
  display: flex;
  height: 100%;
  background: #f5f5f5;
  overflow: hidden;
}

/* 主内容区域 */
.main-container {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  background: #f5f5f5;
}
</style>
