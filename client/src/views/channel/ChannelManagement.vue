<template>
  <div class="channel-management">
    <!-- 左侧导航菜单 -->
    <div class="sidebar" :class="{ collapsed: isCollapsed }">
      <!-- 收起导航按钮 -->
      <div class="sidebar-header" @click="toggleSidebar">
        <el-icon class="sidebar-icon"><MenuIcon /></el-icon>
        <span v-if="!isCollapsed" class="sidebar-title">收起导航</span>
        <el-icon v-if="!isCollapsed" class="collapse-icon"><ArrowLeft /></el-icon>
        <el-icon v-else class="collapse-icon"><ArrowRight /></el-icon>
      </div>

      <el-menu
        class="sidebar-menu"
        :default-active="activeMenu"
        @select="handleMenuSelect"
        :collapse="isCollapsed"
      >
        <el-menu-item index="channel-list">
          <el-icon><List /></el-icon>
          <span>渠道列表</span>
        </el-menu-item>
        <el-menu-item index="price-ratio">
          <el-icon><Money /></el-icon>
          <span>价格比例</span>
        </el-menu-item>
      </el-menu>
    </div>

    <!-- 主内容区域 -->
    <div class="main-container">
      <template v-if="activeMenu === 'channel-list'">
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
        v-else-if="activeMenu === 'price-ratio'"
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
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Menu as MenuIcon, ArrowLeft, ArrowRight, List, Money } from '@element-plus/icons-vue'

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
  priceRatioData,
  priceRatioLoading,
  loadPriceRatioData,
  parseRatioString,
  savePriceRatio,
  flattenMappingData,
} = useChannelData()

// Sidebar folding status
const isCollapsed = ref(false)
const toggleSidebar = () => {
  isCollapsed.value = !isCollapsed.value
}

// Navigation & panels status
const activeMenu = ref('channel-list')
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
const calendarStartDate = ref(new Date().toISOString().split('T')[0])
const calendarDates = ref<CalendarDate[]>([])
const calendarData = ref<CalendarRow[]>([])

// Airbnb Room Settings properties
const roomSettingsStartDate = ref(new Date().toISOString().split('T')[0])
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
const currentManagingRoom = ref<FlattenedMappingItem | null>(null)
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

/** Menu selection routing */
const handleMenuSelect = (index: string) => {
  activeMenu.value = index
  if (index === 'price-ratio') {
    showChannelSettings.value = false
    loadPriceRatioData()
  } else if (index === 'channel-list') {
    showChannelSettings.value = false
  }
}

/** Flatten mapping data helper */
const updateFlattenedMappingData = () => {
  flattenedMappingData.value = flattenMappingData(roomMappingData.value)
}

/** Reset non-Airbnb real-flow data until backed by real APIs. */
const resetNonAirbnbChannelData = () => {
  hotelList.value = []
  selectedHotelId.value = null
  roomMappingData.value = []
  flattenedMappingData.value = []
  calendarDates.value = []
  calendarData.value = []
  roomSettingsData.value = []
  roomSettingsDates.value = []
}

/** Open channel settings details panel */
const openSettings = (channel: ChannelItem) => {
  selectedChannel.value = channel
  showChannelSettings.value = true
  editingRoomId.value = null

  if (channel.name === 'Airbnb') {
    airbnbAccountList.value = []
    hotelList.value = []
    selectedHotelId.value = null
    roomMappingData.value = []
    flattenedMappingData.value = []
    calendarDates.value = []
    calendarData.value = []
    roomSettingsData.value = []
    roomSettingsDates.value = []
  } else {
    airbnbAccountList.value = []
    resetNonAirbnbChannelData()
  }
}

/** Close channel settings details panel */
const closeChannelSettings = () => {
  showChannelSettings.value = false
  selectedChannel.value = null
  hotelList.value = []
  airbnbAccountList.value = []
  roomMappingData.value = []
  flattenedMappingData.value = []
  calendarData.value = []
  roomSettingsData.value = []
  roomSettingsDates.value = []
}

/** Switch selected hotel */
const handleUpdateSelectedHotelId = (val: number | null) => {
  selectedHotelId.value = val
  roomMappingData.value = []
  flattenedMappingData.value = []
  calendarDates.value = []
  calendarData.value = []
}

/** Handle date settings change on Airbnb settings */
const handleRoomSettingsStartDateChange = (val: string) => {
  roomSettingsStartDate.value = val
  roomSettingsDates.value = []
  roomSettingsData.value = []
}

// ──────────── Hotel Actions ────────────

const handleEditHotel = (row: HotelItem) => {
  ElMessage.info(`编辑酒店: ${row.hotelName}`)
}

const handleDisconnectHotel = async (row: HotelItem) => {
  try {
    await ElMessageBox.confirm(
      `确定要断开与酒店 "${row.hotelName}" 的连接吗？断开后将无法同步该酒店的订单和房态。`,
      '断开连接',
      {
        confirmButtonText: '确定断开',
        cancelButtonText: '取消',
        type: 'warning',
      },
    )

    const index = hotelList.value.findIndex((item) => item.id === row.id)
    if (index > -1) {
      hotelList.value.splice(index, 1)
    }
    ElMessage.success(`已断开与酒店 "${row.hotelName}" 的连接`)
  } catch {
    // Cancelled
  }
}

const handleDisconnectAccount = async (row: AirbnbAccount) => {
  try {
    await ElMessageBox.confirm(`确定要断开与帐户 "${row.account}" 的连接吗？`, '断开连接', {
      confirmButtonText: '确定断开',
      cancelButtonText: '取消',
      type: 'warning',
    })

    const index = airbnbAccountList.value.findIndex((item) => item.id === row.id)
    if (index > -1) {
      airbnbAccountList.value.splice(index, 1)
    }
    ElMessage.success(`已断开与帐户 "${row.account}" 的连接`)
  } catch {
    // Cancelled
  }
}

// ──────────── Mapping Actions ────────────

const handleEditMapping = (row: FlattenedMappingItem) => {
  editingRoomId.value = row.roomGroupId
}

const handleCancelEditMapping = () => {
  editingRoomId.value = null
  updateFlattenedMappingData()
}

const handleSaveMapping = (roomGroupId: string) => {
  const group = roomMappingData.value.find((g) => g.roomGroupId === roomGroupId)
  if (group) {
    const flatItems = flattenedMappingData.value.filter((item) => item.roomGroupId === roomGroupId)
    if (flatItems.length > 0) {
      group.pmsRoomType = flatItems[0].selectedPmsRoom
      group.selectedPmsRoom = flatItems[0].selectedPmsRoom
      flatItems.forEach((flatItem) => {
        const pricePlan = group.pricePlans.find((p) => p.id === flatItem.id)
        if (pricePlan) {
          pricePlan.pmsPricePlan = flatItem.selectedPmsPricePlan
          pricePlan.selectedPmsPricePlan = flatItem.selectedPmsPricePlan
          if (flatItem.selectedPmsPricePlan) {
            pricePlan.status = 'connected'
          }
        }
      })
    }
  }
  editingRoomId.value = null
  updateFlattenedMappingData()
  ElMessage.success('保存成功')
}

const handleDisconnectMapping = async (row: FlattenedMappingItem) => {
  try {
    await ElMessageBox.confirm(`确定要断开房型 "${row.channelRoomType}" 的映射吗？`, '断开连接', {
      confirmButtonText: '确定断开',
      cancelButtonText: '取消',
      type: 'warning',
    })
    const groupIndex = roomMappingData.value.findIndex((g) => g.roomGroupId === row.roomGroupId)
    if (groupIndex > -1) {
      roomMappingData.value.splice(groupIndex, 1)
      updateFlattenedMappingData()
    }
    ElMessage.success('映射已断开')
  } catch {
    // Cancelled
  }
}

const handleManageMapping = (row: FlattenedMappingItem) => {
  currentManagingRoom.value = row
  showBookingSettingsDrawer.value = true
}

const handleSaveBookingSettings = (newSettings: BookingSettings) => {
  bookingSettings.value = newSettings
  ElMessage.success('预定设置已保存')
  showBookingSettingsDrawer.value = false
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
    '当前生产渠道管理页尚未配置 Airbnb 账号 OAuth 授权入口，因此不会跳转占位授权地址，也不会伪造授权成功或写入模拟账号。需要验证完整链路时，请在独立 channel-simulator 中执行全流程模拟。',
    'Airbnb 授权提示',
    {
      confirmButtonText: '我知道了',
      type: 'info',
    },
  )
}

const handleHotelAuthorize = () => {
  showAddHotelDialog.value = false
  ElMessageBox.alert(
    '当前生产渠道管理页不会执行模拟授权，也不会向真实酒店列表写入模拟酒店。需要验证完整链路时，请在独立 channel-simulator 中执行全流程模拟。',
    `${selectedChannel.value?.name || '该渠道'}接入提示`,
    {
      confirmButtonText: '我知道了',
      type: 'info',
    },
  )
}

// ──────────── Price Ratio Actions ────────────

const handleEditPriceRatio = (row: any) => {
  const parsed = parseRatioString(row.ratio)
  editingPriceRatio.value = {
    channelId: row.channelId,
    channel: row.channel,
    ratio: row.ratio,
    adjustmentType: parsed.type,
    adjustmentValue: parsed.value,
    adjustmentUnit: parsed.unit,
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
    ElMessage.success(`${selectedChannel.value.name}连接成功！`)
    loadChannels()
  }
  showWidgetDialog.value = false
  selectedChannel.value = null
}

const handleWidgetSynced = () => {
  loadChannels()
}

const handleWidgetError = (error: string) => {
  console.error('Widget连接错误:', error)
}

// ──────────── Extra Tab Buttons ────────────

const handleRefreshChannelInfo = () => {
  ElMessage.warning('真实渠道刷新接口尚未接入；模拟流程请在独立 channel-simulator 中验证。')
}

const handleImportOrders = () => {
  ElMessage.warning('真实订单导入接口尚未接入；模拟流程请在独立 channel-simulator 中验证。')
}

const handleSyncFromCalendar = () => {
  ElMessage.warning('真实日历同步接口尚未接入；模拟流程请在独立 channel-simulator 中验证。')
}

const handleFullRefresh = () => {
  ElMessage.warning('真实全量刷新接口尚未接入；模拟流程请在独立 channel-simulator 中验证。')
}

onMounted(() => {
  loadChannels()
  loadPmsRoomOptions()
})
</script>

<style scoped>
.channel-management {
  display: flex;
  height: 100vh;
  background: #f5f5f5;
  overflow: hidden;
}

/* 左侧导航 */
.sidebar {
  width: 200px;
  background: white;
  border-right: 1px solid #e8e8e8;
  display: flex;
  flex-direction: column;
  transition: width 0.3s ease;
  flex-shrink: 0;
}

.sidebar.collapsed {
  width: 64px;
}

.sidebar-header {
  height: 56px;
  display: flex;
  align-items: center;
  padding: 0 16px;
  border-bottom: 1px solid #e8e8e8;
  cursor: pointer;
  transition: background-color 0.3s;
  flex-shrink: 0;
}

.sidebar-header:hover {
  background-color: #f5f5f5;
}

.sidebar-icon {
  font-size: 20px;
  color: #1890ff;
  flex-shrink: 0;
}

.sidebar-title {
  flex: 1;
  margin-left: 12px;
  font-size: 14px;
  font-weight: 500;
  color: #333;
}

.collapse-icon {
  font-size: 16px;
  color: #999;
}

.sidebar-menu {
  flex: 1;
  border-right: none;
  overflow-y: auto;
}

/* 主内容区域 */
.main-container {
  flex: 1;
  overflow: hidden;
  background: #f5f5f5;
}

/* 移动端适配 */
@media (max-width: 768px) {
  .channel-management {
    flex-direction: column;
  }

  .sidebar {
    width: 100%;
    height: auto;
  }

  .sidebar.collapsed {
    width: 100%;
  }

  .sidebar-header {
    display: none;
  }
}
</style>
