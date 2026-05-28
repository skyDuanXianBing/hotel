<template>
  <div class="channel-settings-panel">
    <!-- 面包屑导航 -->
    <div class="settings-header">
      <div class="breadcrumb">
        <el-button type="text" class="back-btn" @click="$emit('close')">
          <el-icon><ArrowLeft /></el-icon>
          {{ t('channel.settings.directSettings') }}
        </el-button>
        <span class="breadcrumb-separator">&gt;</span>
        <span class="breadcrumb-item active">{{ channelSettingsTitle }}</span>
      </div>
    </div>

    <div class="settings-content">
      <!-- 渠道信息卡片 -->
      <div class="channel-info-card">
        <div class="channel-logo-section">
          <img :src="channel?.logoUrl" :alt="channel?.name" class="channel-logo-large-img" />
        </div>
        <div class="channel-desc">
          <h3 class="channel-title-highlight">{{ channelSettingsTitle }}</h3>
          <p>{{ t('channel.settings.description') }}</p>
        </div>
        <div class="channel-actions">
          <template v-if="!isAirbnb">
            <el-button @click="$emit('showHistory')">{{ t('channel.settings.history') }}</el-button>
          </template>
          <el-button
            type="primary"
            @click="isAirbnb ? $emit('addAccount') : $emit('addHotel')"
          >
            {{ isAirbnb ? t('channel.settings.addAccount') : t('channel.settings.addHotel') }}
          </el-button>
        </div>
      </div>

      <!-- 标签页区域 -->
      <div class="channel-tabs-section">
        <el-tabs v-model="currentTab" class="channel-settings-tabs">
          <!-- Airbnb: 帐户列表 -->
          <el-tab-pane v-if="isAirbnb" :label="t('channel.settings.tabs.accountList')" name="accountList">
            <AccountListTab
              :accounts="airbnbAccounts"
              @disconnect="(row) => $emit('disconnectAccount', row)"
            />
          </el-tab-pane>

          <!-- 酒店列表（非 Airbnb） -->
          <el-tab-pane v-if="!isAirbnb" :label="t('channel.settings.tabs.hotelList')" name="hotelList">
            <HotelListTab
              :hotels="hotels"
              @edit="(row) => $emit('editHotel', row)"
              @disconnect="(row) => $emit('disconnectHotel', row)"
            />
          </el-tab-pane>

          <!-- 映射 -->
          <el-tab-pane :label="t('channel.settings.tabs.mapping')" name="mapping">
            <MappingTab
              :is-airbnb="isAirbnb"
              :mapping-data="flattenedMappingData"
              :hotels="hotels"
              :selected-hotel-id="selectedHotelId"
              :connection-status="mappingConnectionStatus"
              :pms-room-options="pmsRoomOptions"
              :pms-price-plan-options="pmsPricePlanOptions"
              :editing-room-id="editingRoomId"
              @update:selected-hotel-id="(v) => $emit('update:selectedHotelId', v)"
              @update:connection-status="mappingConnectionStatus = $event"
              @update:editing-room-id="(v) => $emit('update:editingRoomId', v)"
              @refresh="$emit('refreshChannelInfo')"
              @import-orders="$emit('importOrders')"
              @edit="(row) => $emit('editMapping', row)"
              @save="(id) => $emit('saveMapping', id)"
              @cancel-edit="$emit('cancelEditMapping')"
              @disconnect="(row) => $emit('disconnectMapping', row)"
              @manage="(row) => $emit('manageMapping', row)"
            />
          </el-tab-pane>

          <!-- 日历 -->
          <el-tab-pane :label="t('channel.settings.tabs.calendar')" name="calendar">
            <CalendarTab
              :calendar-data="calendarData"
              :calendar-dates="calendarDates"
              :hotels="hotels"
              :selected-hotel-id="selectedHotelId"
              :start-date="calendarStartDate"
              :room-type="calendarRoomType"
              :display-item="calendarDisplayItem"
              @update:selected-hotel-id="(v) => $emit('update:selectedHotelId', v)"
              @update:start-date="calendarStartDate = $event"
              @update:room-type="calendarRoomType = $event"
              @update:display-item="calendarDisplayItem = $event"
              @sync-from-calendar="$emit('syncFromCalendar')"
              @full-refresh="$emit('fullRefresh')"
            />
          </el-tab-pane>

          <!-- Airbnb: 房量设置 -->
          <el-tab-pane v-if="isAirbnb" :label="t('channel.settings.tabs.roomSettings')" name="roomSettings">
            <RoomSettingsTab
              :data="roomSettingsData"
              :dates="roomSettingsDates"
              :start-date="roomSettingsStartDate"
              @update:start-date="(v) => $emit('update:roomSettingsStartDate', v)"
            />
          </el-tab-pane>
        </el-tabs>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ArrowLeft } from '@element-plus/icons-vue'
import type {
  ChannelItem,
  HotelItem,
  AirbnbAccount,
  FlattenedMappingItem,
  CalendarDate,
  CalendarRow,
  RoomSettingsRow,
  SelectOption,
} from '../types'
import AccountListTab from './tabs/AccountListTab.vue'
import HotelListTab from './tabs/HotelListTab.vue'
import MappingTab from './tabs/MappingTab.vue'
import CalendarTab from './tabs/CalendarTab.vue'
import RoomSettingsTab from './tabs/RoomSettingsTab.vue'

const props = defineProps<{
  channel: ChannelItem | null
  isAirbnb: boolean
  // 帐户 / 酒店
  airbnbAccounts: AirbnbAccount[]
  hotels: HotelItem[]
  // 映射
  flattenedMappingData: FlattenedMappingItem[]
  selectedHotelId: number | null
  pmsRoomOptions: SelectOption[]
  pmsPricePlanOptions: SelectOption[]
  editingRoomId: string | null
  // 日历
  calendarData: CalendarRow[]
  calendarDates: CalendarDate[]
  // 房量设置
  roomSettingsData: RoomSettingsRow[]
  roomSettingsDates: CalendarDate[]
  roomSettingsStartDate: string
}>()

defineEmits<{
  close: []
  showHistory: []
  addAccount: []
  addHotel: []
  disconnectAccount: [row: AirbnbAccount]
  editHotel: [row: HotelItem]
  disconnectHotel: [row: HotelItem]
  'update:selectedHotelId': [value: number | null]
  'update:editingRoomId': [value: string | null]
  'update:roomSettingsStartDate': [value: string]
  refreshChannelInfo: []
  importOrders: []
  editMapping: [row: FlattenedMappingItem]
  saveMapping: [roomGroupId: string]
  cancelEditMapping: []
  disconnectMapping: [row: FlattenedMappingItem]
  manageMapping: [row: FlattenedMappingItem]
  syncFromCalendar: []
  fullRefresh: []
}>()

// 标签页状态
const { t } = useI18n()
const currentTab = ref(props.isAirbnb ? 'accountList' : 'hotelList')
const channelSettingsTitle = computed(() =>
  t('channel.settings.channelSettings', { name: props.channel?.name || '' }),
)

// 筛选状态（本地管理）
const mappingConnectionStatus = ref('all')
const calendarStartDate = ref(new Date().toISOString().split('T')[0])
const calendarRoomType = ref('all')
const calendarDisplayItem = ref('all')
</script>

<style scoped>
.channel-settings-panel {
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.settings-header {
  background: white;
  padding: 16px 24px;
  border-bottom: 1px solid #e8e8e8;
  flex-shrink: 0;
}

.breadcrumb {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #606266;
}

.back-btn {
  padding: 0;
  display: flex;
  align-items: center;
  gap: 4px;
  color: #606266;
}

.back-btn:hover {
  color: #409eff;
}

.breadcrumb-separator {
  color: #d9d9d9;
}

.breadcrumb-item.active {
  color: #1d2129;
  font-weight: 500;
}

.settings-content {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
}

/* 渠道信息卡片 */
.channel-info-card {
  background: white;
  border-radius: 8px;
  padding: 24px;
  margin-bottom: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  display: flex;
  flex-wrap: wrap;
  gap: 24px;
  align-items: flex-start;
}

.channel-logo-section {
  flex-shrink: 0;
  width: 100px;
  height: 50px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.channel-logo-large-img {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
}

.channel-desc {
  flex: 1;
}

.channel-desc h3 {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 12px 0;
}

.channel-title-highlight {
  color: #ff385c !important;
}

.channel-desc p {
  font-size: 13px;
  line-height: 1.8;
  color: #606266;
  margin: 0;
}

.channel-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: flex-start;
  flex-shrink: 0;
}

.channel-actions .el-button {
  white-space: normal;
}

/* 标签页区域 */
.channel-tabs-section {
  background: white;
  border-radius: 8px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.channel-settings-tabs :deep(.el-tabs__header) {
  margin-bottom: 20px;
}

.channel-settings-tabs :deep(.el-tabs__item) {
  font-size: 14px;
  padding: 0 20px;
}

.channel-settings-tabs :deep(.el-tabs__item.is-active) {
  font-weight: 600;
}
</style>
