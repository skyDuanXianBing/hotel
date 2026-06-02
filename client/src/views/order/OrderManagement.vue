<template>
  <div class="order-management">
    <!-- 左侧导航菜单 -->
    <div class="sidebar" :class="{ collapsed: isCollapsed }">
      <!-- 收起导航按钮 -->
      <div class="sidebar-header" @click="toggleSidebar">
        <el-icon class="sidebar-icon"><MenuIcon /></el-icon>
        <span v-if="!isCollapsed" class="sidebar-title">{{ t('order.sidebar.collapse') }}</span>
        <el-icon v-if="!isCollapsed" class="collapse-icon"><ArrowLeft /></el-icon>
        <el-icon v-else class="collapse-icon"><ArrowRight /></el-icon>
      </div>

      <el-menu
        class="sidebar-menu"
        :default-active="activeMenu"
        @select="handleMenuSelect"
        :collapse="isCollapsed"
      >
        <el-menu-item index="order-management">
          <el-icon><Document /></el-icon>
          <span>{{ t('order.sidebar.accommodationOrders') }}</span>
        </el-menu-item>
      </el-menu>
    </div>

    <!-- 主内容区域 -->
    <div class="main-content">
      <!-- 订单状态标签页 -->
      <div class="order-tabs">
        <el-tabs v-model="activeOrderTab" @tab-change="handleOrderTabChange">
          <el-tab-pane :label="t('order.tabs.all')" name="all">
            <template #label>
              <span>{{ t('order.tabs.all') }}</span>
            </template>
          </el-tab-pane>
          <el-tab-pane name="today-checkin">
            <template #label>
              <div class="tab-label-with-badge">
                <span>{{ t('order.tabs.todayCheckin') }}</span>
                <span v-if="todayCheckinCount > 0" class="custom-badge">{{
                  todayCheckinCount
                }}</span>
              </div>
            </template>
          </el-tab-pane>
          <el-tab-pane name="today-checkout">
            <template #label>
              <div class="tab-label-with-badge">
                <span>{{ t('order.tabs.todayCheckout') }}</span>
                <span v-if="todayCheckoutCount > 0" class="custom-badge">{{
                  todayCheckoutCount
                }}</span>
              </div>
            </template>
          </el-tab-pane>
          <el-tab-pane name="today-new">
            <template #label>
              <div class="tab-label-with-badge">
                <span>{{ t('order.tabs.todayNew') }}</span>
                <span v-if="todayNewCount > 0" class="custom-badge">{{ todayNewCount }}</span>
              </div>
            </template>
          </el-tab-pane>
          <el-tab-pane name="unassigned">
            <template #label>
              <div class="tab-label-with-badge">
                <span>{{ t('order.tabs.unassigned') }}</span>
                <span v-if="unassignedCount > 0" class="custom-badge">{{ unassignedCount }}</span>
              </div>
            </template>
          </el-tab-pane>
          <el-tab-pane :label="t('order.tabs.assigned')" name="assigned"></el-tab-pane>
          <el-tab-pane name="pending">
            <template #label>
              <div class="tab-label-with-badge">
                <span>{{ t('order.tabs.pending') }}</span>
                <span v-if="pendingCount > 0" class="custom-badge">{{ pendingCount }}</span>
              </div>
            </template>
          </el-tab-pane>
          <el-tab-pane :label="t('order.tabs.orderBox')" name="order-box"></el-tab-pane>
          <el-tab-pane :label="t('order.tabs.deletedRooms')" name="deleted-rooms"></el-tab-pane>
        </el-tabs>
      </div>

      <!-- 搜索和筛选区域 -->
      <div class="search-filter-section">
        <!-- 第一行：搜索框和主要筛选器 -->
        <div class="main-filter-row">
          <!-- 搜索框 -->
          <el-input
            v-model="searchQuery"
            :placeholder="t('order.filters.searchPlaceholder')"
            class="search-input"
            @keyup.enter="handleSearch"
          >
            <template #append>
              <el-button @click="handleSearch">
                <el-icon><Search /></el-icon>
              </el-button>
            </template>
          </el-input>

          <!-- 渠道筛选 -->
          <div class="filter-group">
            <span class="filter-label">{{ t('order.filters.channel') }}</span>
            <el-select
              v-model="filters.channel"
              :placeholder="t('order.options.all')"
              @change="handleFilterChange"
            >
              <el-option :label="t('order.options.all')" value=""></el-option>
              <el-option
                v-for="channel in channelOptions"
                :key="channel.value"
                :label="channel.label"
                :value="channel.value"
              />
            </el-select>
          </div>

          <!-- 房型筛选 -->
          <div class="filter-group">
            <span class="filter-label">{{ t('order.filters.roomType') }}</span>
            <el-select
              v-model="filters.roomType"
              :placeholder="t('order.options.all')"
              @change="handleFilterChange"
            >
              <el-option :label="t('order.options.all')" value=""></el-option>
              <el-option
                v-for="roomType in roomTypeOptions"
                :key="roomType.value"
                :label="roomType.label"
                :value="roomType.value"
              />
            </el-select>
          </div>

          <!-- 日期选择器（仅在全部标签页显示） -->
          <div v-if="activeOrderTab === 'all'" class="filter-group date-filter-group">
            <span class="filter-label">{{ t('order.filters.createdAt') }}</span>
            <el-date-picker
              v-model="dateRange"
              type="daterange"
              range-separator="-"
              :start-placeholder="t('accommodation.common.startDate')"
              :end-placeholder="t('accommodation.common.endDate')"
              format="YYYY-MM-DD"
              value-format="YYYY-MM-DD"
              @change="handleDateRangeChange"
            />
          </div>

          <div v-else-if="isTodayOperationTab" class="filter-group date-filter-group single-date-filter">
            <span class="filter-label">{{ todayOperationDateLabel }}</span>
            <el-date-picker
              v-model="operationDate"
              type="date"
              :placeholder="todayOperationDateLabel"
              format="YYYY-MM-DD"
              value-format="YYYY-MM-DD"
              @change="handleOperationDateChange"
            />
          </div>
        </div>

        <!-- 第二行：详细筛选器 -->
        <div class="filter-row">
          <div class="filter-group">
            <span class="filter-label">{{ t('order.filters.checkinType') }}</span>
            <el-select
              v-model="filters.checkinType"
              :placeholder="t('order.options.all')"
              @change="handleFilterChange"
            >
              <el-option :label="t('order.options.all')" value=""></el-option>
              <el-option :label="t('order.options.normalCheckin')" value="normal"></el-option>
              <el-option :label="t('order.options.earlyCheckin')" value="early"></el-option>
              <el-option :label="t('order.options.lateCheckin')" value="late"></el-option>
            </el-select>
          </div>

          <div class="filter-group">
            <span class="filter-label">{{ t('order.filters.stayStatus') }}</span>
            <el-select
              v-model="filters.status"
              :placeholder="t('order.options.select')"
              @change="handleFilterChange"
            >
              <el-option :label="t('order.options.all')" value=""></el-option>
              <el-option :label="t('order.options.checkedIn')" value="checked-in"></el-option>
              <el-option
                :label="t('order.options.notCheckedIn')"
                value="not-checked-in"
              ></el-option>
              <el-option :label="t('order.options.checkedOut')" value="checked-out"></el-option>
            </el-select>
          </div>

          <div class="filter-group">
            <span class="filter-label">{{ t('order.filters.paymentStatus') }}</span>
            <el-select
              v-model="filters.paymentStatus"
              :placeholder="t('order.options.all')"
              @change="handleFilterChange"
            >
              <el-option :label="t('order.options.all')" value=""></el-option>
              <el-option :label="t('order.options.paid')" value="paid"></el-option>
              <el-option :label="t('order.options.unpaid')" value="unpaid"></el-option>
            </el-select>
          </div>

          <div class="filter-actions">
            <el-button @click="toggleFilters">
              {{ showAdvancedFilters ? t('order.filters.collapse') : t('order.filters.expand') }}
              <el-icon>
                <component :is="showAdvancedFilters ? ArrowUp : ArrowDown" />
              </el-icon>
            </el-button>
          </div>
        </div>
      </div>

      <!-- 订单盒子提示信息 -->
      <div v-if="activeOrderTab === 'order-box'" class="order-box-notice">
        <el-alert :title="t('order.orderBoxNotice.title')" type="info" :closable="false" show-icon>
          <template #default>
            <div class="notice-content">
              <p>{{ t('order.orderBoxNotice.line1') }}</p>
              <p>{{ t('order.orderBoxNotice.line2') }}</p>
              <p>{{ t('order.orderBoxNotice.line3') }}</p>
            </div>
          </template>
        </el-alert>
      </div>

      <!-- 订单列表 -->
      <div class="order-list">
        <el-table
          :data="filteredOrders"
          style="width: 100%"
          :header-cell-style="{ background: '#f5f7fa', color: '#606266' }"
          v-loading="loading"
          :element-loading-text="t('order.options.loading')"
        >
          <el-table-column
            prop="orderNumber"
            :label="t('order.table.orderNumbers')"
            min-width="280"
            fixed="left"
          >
            <template #default="scope">
              <div class="order-number">
                <el-button
                  type="text"
                  class="order-link"
                  :title="t('order.table.detailTitle')"
                  @click="viewOrder(scope.row)"
                  >{{ scope.row.orderNumber }}</el-button
                >
                <el-button
                  type="text"
                  class="channel-order-link"
                  :title="t('order.table.detailTitle')"
                  @click="viewOrder(scope.row)"
                >
                  {{ getDisplayChannelOrderNumber(scope.row) }}
                </el-button>
                <span class="order-detail-tip">{{ t('order.table.detailTip') }}</span>
              </div>
            </template>
          </el-table-column>

          <el-table-column prop="channelName" :label="t('order.table.channel')" width="150">
            <template #default="scope">
              <el-tag size="small" :type="getChannelTagType(scope.row.channelName)">
                {{ getChannelDisplayName(scope.row.channelName) }}
              </el-tag>
            </template>
          </el-table-column>

          <el-table-column
            v-if="activeOrderTab !== 'all'"
            prop="status"
            :label="t('order.table.status')"
            width="100"
          >
            <template #default="scope">
              <el-tag size="small" :type="getReservationStatusTagType(scope.row.status)">
                {{ getReservationStatusText(scope.row.status) }}
              </el-tag>
            </template>
          </el-table-column>

          <el-table-column
            prop="guestName"
            :label="t('order.table.guestName')"
            width="150"
          ></el-table-column>

          <el-table-column prop="phone" :label="t('order.table.phone')" width="130">
            <template #default="scope">
              <span>{{ scope.row.phone || '-' }}</span>
            </template>
          </el-table-column>

          <el-table-column :label="t('order.table.checkinType')" width="100">
            <template #default="scope">
              <span>{{ getCheckinTypeText(scope.row) }}</span>
            </template>
          </el-table-column>

          <el-table-column
            prop="roomTypeName"
            :label="t('order.table.roomType')"
            width="180"
          ></el-table-column>

          <el-table-column
            v-if="activeOrderTab === 'all'"
            prop="pricePlan"
            :label="t('order.table.pricePlan')"
            width="140"
          >
            <template #default="scope">
              <span>{{ scope.row.pricePlan || '-' }}</span>
            </template>
          </el-table-column>

          <el-table-column
            v-if="activeOrderTab === 'unassigned'"
            prop="otaRoomId"
            :label="t('order.table.otaRoomId')"
            width="160"
          >
            <template #default="scope">
              <span>{{ scope.row.otaRoomId || '-' }}</span>
            </template>
          </el-table-column>

          <el-table-column
            v-if="activeOrderTab === 'unassigned'"
            prop="otaRoomTypeId"
            :label="t('order.table.pmsRoomTypeId')"
            width="110"
          >
            <template #default="scope">
              <span>{{ scope.row.otaRoomTypeId ?? '-' }}</span>
            </template>
          </el-table-column>

          <el-table-column
            v-if="activeOrderTab === 'unassigned'"
            prop="reservationNotifId"
            label="reservation_notif_id"
            width="210"
          >
            <template #default="scope">
              <span>{{ scope.row.reservationNotifId || '-' }}</span>
            </template>
          </el-table-column>

          <el-table-column
            prop="roomNumber"
            :label="t('order.table.roomNumber')"
            width="100"
          ></el-table-column>

          <el-table-column
            v-if="activeOrderTab === 'all'"
            :label="t('order.table.roomCount')"
            width="90"
          >
            <template #default>
              <span>1</span>
            </template>
          </el-table-column>

          <el-table-column
            v-if="
              activeOrderTab === 'unassigned' ||
              activeOrderTab === 'assigned' ||
              activeOrderTab === 'order-box'
            "
            :label="t('order.table.assignStatus')"
            width="100"
          >
            <template #default="scope">
              <el-tag size="small" :type="getAssignStatusTagType(scope.row)">
                {{ getAssignStatusText(scope.row) }}
              </el-tag>
            </template>
          </el-table-column>

          <el-table-column
            v-if="activeOrderTab === 'all'"
            prop="checkInDate"
            :label="t('order.table.checkInTime')"
            width="170"
          >
            <template #default="scope">
              <span>{{ formatStayDateTime(scope.row.checkInDate, 'checkIn') }}</span>
            </template>
          </el-table-column>

          <el-table-column
            v-if="activeOrderTab === 'all'"
            prop="checkOutDate"
            :label="t('order.table.checkOutTime')"
            width="170"
          >
            <template #default="scope">
              <span>{{ formatStayDateTime(scope.row.checkOutDate, 'checkOut') }}</span>
            </template>
          </el-table-column>

          <el-table-column
            v-if="activeOrderTab === 'all'"
            :label="t('order.table.nights')"
            width="90"
          >
            <template #default="scope">
              <span>{{ getNights(scope.row) }}</span>
            </template>
          </el-table-column>

          <el-table-column
            v-if="activeOrderTab === 'all'"
            :label="t('order.table.totalGuests')"
            width="100"
          >
            <template #default="scope">
              <span>{{ getTotalGuests(scope.row) }}</span>
            </template>
          </el-table-column>

          <el-table-column
            v-if="activeOrderTab === 'all'"
            :label="t('order.table.adults')"
            width="100"
          >
            <template #default="scope">
              <span>{{ scope.row.adults ?? '-' }}</span>
            </template>
          </el-table-column>

          <el-table-column
            v-if="activeOrderTab === 'all'"
            :label="t('order.table.children')"
            width="100"
          >
            <template #default="scope">
              <span>{{ scope.row.children ?? 0 }}</span>
            </template>
          </el-table-column>

          <el-table-column
            v-if="activeOrderTab === 'all'"
            :label="t('order.table.stayStatus')"
            width="120"
          >
            <template #default="scope">
              <span>{{ getReservationStatusText(scope.row.status) }}</span>
            </template>
          </el-table-column>

          <el-table-column prop="currentRoomPrice" :label="t('order.table.roomFee')" width="120">
            <template #default="scope">
              <span>{{ formatAmount(scope.row.currentRoomPrice) }}</span>
            </template>
          </el-table-column>

          <el-table-column
            v-if="activeOrderTab === 'all'"
            prop="totalAmount"
            :label="t('order.table.accommodationAmount')"
            width="140"
          >
            <template #default="scope">
              <span>{{ formatAmount(scope.row.totalAmount) }}</span>
            </template>
          </el-table-column>

          <el-table-column
            v-if="activeOrderTab === 'all'"
            prop="totalAmount"
            :label="t('order.table.accommodationSubtotal')"
            width="140"
          >
            <template #default="scope">
              <span>{{ formatAmount(scope.row.totalAmount) }}</span>
            </template>
          </el-table-column>

          <el-table-column
            v-if="activeOrderTab === 'all'"
            prop="commission"
            :label="t('order.table.commission')"
            width="120"
          >
            <template #default="scope">
              <span>{{ formatAmount(scope.row.commission) }}</span>
            </template>
          </el-table-column>

          <el-table-column
            v-if="activeOrderTab === 'all'"
            prop="paymentMethod"
            :label="t('order.table.paymentMethod')"
            width="150"
          >
            <template #default="scope">
              <span>{{ scope.row.paymentMethod || '-' }}</span>
            </template>
          </el-table-column>

          <el-table-column
            v-if="activeOrderTab === 'all'"
            :label="t('order.table.cashierStatus')"
            width="120"
          >
            <template #default="scope">
              <span>{{ getCashierStatusTextV2(scope.row) }}</span>
            </template>
          </el-table-column>

          <el-table-column :label="t('order.table.settlementStatus')" width="100">
            <template #default="scope">
              <el-select
                v-if="activeOrderTab === 'pending'"
                :model-value="getSettlementSelectValue(scope.row)"
                size="small"
                style="width: 96px"
                :disabled="settlementUpdatingOrderId === scope.row.id"
                @change="handleSettlementStatusChange(scope.row, $event)"
              >
                <el-option :label="t('order.options.paid')" value="paid" />
                <el-option :label="t('order.options.unpaid')" value="unpaid" />
              </el-select>
              <el-tag v-else size="small" :type="getSettlementTagTypeV2(scope.row)">
                {{ getSettlementStatusTextV2(scope.row) }}
              </el-tag>
            </template>
          </el-table-column>

          <el-table-column
            v-if="activeOrderTab !== 'all'"
            prop="checkInDate"
            :label="t('order.table.checkInTime')"
            width="150"
          ></el-table-column>

          <el-table-column
            v-if="activeOrderTab === 'all'"
            prop="notes"
            :label="t('order.table.notes')"
            width="200"
          >
            <template #default="scope">
              <span>{{ scope.row.notes || '-' }}</span>
            </template>
          </el-table-column>

          <el-table-column
            v-if="activeOrderTab === 'all'"
            prop="createdBy"
            :label="t('order.table.createdBy')"
            width="120"
          >
            <template #default="scope">
              <span>{{ scope.row.createdBy || '-' }}</span>
            </template>
          </el-table-column>

          <el-table-column prop="createdAt" :label="t('order.table.createdAt')" width="180">
            <template #default="scope">
              <span>{{
                formatDateTime(scope.row.createdAt, scope.row.reservationTimestampStorageZone)
              }}</span>
            </template>
          </el-table-column>

          <el-table-column
            v-if="
              activeOrderTab === 'unassigned' ||
              activeOrderTab === 'assigned' ||
              activeOrderTab === 'order-box'
            "
            :label="t('order.table.actions')"
            width="180"
            fixed="right"
          >
            <template #default="scope">
              <el-button v-if="scope.row.roomId" type="success" link @click="viewOrder(scope.row)">
                {{ t('order.assignDialog.assigned') }}
              </el-button>
              <el-button
                type="primary"
                link
                :disabled="!canAssignRoom(scope.row)"
                @click="openAssignRoom(scope.row)"
              >
                {{
                  scope.row.roomId ? t('order.assignDialog.edit') : t('order.assignDialog.start')
                }}
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <!-- 分页 -->
        <div class="pagination-wrapper">
          <div class="pagination-info">
            <span>{{ t('order.pagination.total', { count: totalOrders }) }}</span>
          </div>
          <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :page-sizes="[10, 20, 50, 100]"
            layout="sizes, prev, pager, next, jumper"
            :total="totalOrders"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
          />
        </div>
      </div>
    </div>

    <ReservationDetailDrawer
      v-model="showOrderDetailDrawer"
      :reservation-id="selectedReservationId"
      :active-order-tab="activeOrderTab"
      @updated="handleReservationUpdated"
    />

    <el-dialog
      v-model="showAssignRoomDialog"
      :title="t('order.assignDialog.title')"
      width="860px"
      destroy-on-close
      @closed="handleAssignDialogClosed"
    >
      <div class="assign-room-panel">
        <h4 class="panel-title">{{ t('order.assignDialog.panelTitle') }}</h4>

        <div class="assign-row">
          <div class="assign-label">{{ t('order.assignDialog.roomType') }}</div>
          <el-select
            v-model="assignRoomTypeId"
            :placeholder="t('order.assignDialog.roomTypePlaceholder')"
            filterable
            :loading="assignableRoomsLoading"
            style="width: 100%"
            @change="handleAssignRoomTypeChange"
          >
            <el-option
              v-for="roomType in assignableRoomTypes"
              :key="roomType.id"
              :label="formatAssignableRoomTypeLabel(roomType)"
              :value="roomType.id"
            />
          </el-select>
        </div>

        <div class="assign-row">
          <div class="assign-label">{{ t('order.assignDialog.room') }}</div>
          <el-select
            v-model="assignRoomId"
            :placeholder="t('order.assignDialog.roomPlaceholder')"
            filterable
            :disabled="!assignRoomTypeId"
            :loading="assignableRoomsLoading"
            style="width: 100%"
          >
            <el-option
              v-for="room in assignableRooms"
              :key="room.id"
              :label="`${room.roomTypeName}-${room.roomNumber}`"
              :value="room.id"
            />
          </el-select>
        </div>

        <div class="assign-actions">
          <el-button :loading="assignableRoomsLoading" @click="refreshAssignableRoomTypes">
            {{ t('order.assignDialog.refresh') }}
          </el-button>
          <el-button
            type="primary"
            :disabled="!assignRoomId"
            :loading="assignRoomSubmitting"
            @click="submitAssignRoom"
          >
            {{ t('order.assignDialog.confirm') }}
          </el-button>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import {
  ArrowLeft,
  ArrowRight,
  Document,
  Search,
  ArrowDown,
  ArrowUp,
  Menu as MenuIcon,
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getAllChannels } from '@/api/channel'
import {
  assignReservationRoom,
  getAssignableRooms,
  getReservationsWithFilters,
  getReservationStatistics,
  updateReservationSettlementStatus,
  type AssignableRoomDTO,
  type AssignableRoomTypeDTO,
  type ReservationDTO,
  type ReservationStatistics,
} from '@/api/reservation'
import { getAllRoomTypes } from '@/api/roomType'
import ReservationDetailDrawer from '@/components/reservation/ReservationDetailDrawer.vue'
import { getOrderBoxList, type OrderBoxItem } from '@/api/orderBox'
import { useStoreStore } from '@/stores/store'
import {
  diffYmdDays,
  formatReservationTimestamp,
  getStoreTodayYmd,
  normalizeYmdInput,
  resolveStoreTimeZone,
} from '@/utils/storeDateTime'

const route = useRoute()
const { t } = useI18n()
const storeStore = useStoreStore()
const currentStoreTimeZone = computed(() => resolveStoreTimeZone(storeStore.currentStore?.timezone))

// Sidebar collapsed state
const isCollapsed = ref(false)

const toggleSidebar = () => {
  isCollapsed.value = !isCollapsed.value
}

// Reactive state
const activeMenu = ref('order-management')
const activeOrderTab = ref('all')

// 映射首页统计类型到订单页标签页
const mapTypeToTab = (type: string) => {
  const typeMap: Record<string, string> = {
    'today-arrivals': 'today-checkin',
    'today-departures': 'today-checkout',
    'today-new': 'today-new',
    unassigned: 'unassigned',
    assigned: 'assigned',
    pending: 'pending',
  }
  return typeMap[type] || 'all'
}

const searchQuery = ref('')
const dateRange = ref<[string, string] | null>(null)
const getTodayDate = () => {
  const today = new Date()
  return `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`
}
const operationDate = ref(getTodayDate())
const showAdvancedFilters = ref(true)
const loading = ref(false)
const channelOptions = ref<Array<{ label: string; value: string }>>([])
const roomTypeOptions = ref<Array<{ label: string; value: string }>>([])

// Filters
const filters = ref({
  channel: '',
  roomType: '',
  checkinType: '',
  status: '',
  paymentStatus: '',
})

// 分页数据
const currentPage = ref(1)
const pageSize = ref(25)
const totalOrders = ref(0)

// API数据
const orders = ref<ReservationDTO[]>([])
const orderBoxItems = ref<OrderBoxItem[]>([])
const statistics = ref<ReservationStatistics>({
  todayCheckinCount: 0,
  todayCheckoutCount: 0,
  todayNewCount: 0,
  unassignedCount: 0,
  pendingCount: 0,
  totalReservations: 0,
})

// 各类型订单数量统计（使用统计接口数据）
const todayCheckinCount = computed(() => {
  return statistics.value.todayCheckinCount
})

const todayCheckoutCount = computed(() => {
  return statistics.value.todayCheckoutCount
})

const todayNewCount = computed(() => {
  return statistics.value.todayNewCount
})

const unassignedCount = computed(() => {
  return statistics.value.unassignedCount
})

const pendingCount = computed(() => {
  return statistics.value.pendingCount
})

const isTodayOperationTab = computed(() =>
  ['today-checkin', 'today-checkout', 'today-new'].includes(activeOrderTab.value)
)

const todayOperationDateLabel = computed(() => {
  switch (activeOrderTab.value) {
    case 'today-checkin':
      return t('order.filters.todayCheckinDate')
    case 'today-checkout':
      return t('order.filters.todayCheckoutDate')
    case 'today-new':
      return t('order.filters.todayNewDate')
    default:
      return t('order.filters.operationDate')
  }
})

// API方法
const loadReservations = async () => {
  loading.value = true
  try {
    // 如果是订单盒子标签页，加载订单盒子数据
    if (activeOrderTab.value === 'order-box') {
      await loadOrderBox()
      loading.value = false
      return
    }
    const filterParams = {
      page: currentPage.value - 1, // 后端从0开始计数
      size: pageSize.value,
      searchKeyword: searchQuery.value || undefined,
      channel: filters.value.channel || undefined,
      roomType: filters.value.roomType || undefined,
      checkinType: filters.value.checkinType || undefined,
      status: filters.value.status || undefined,
      paymentStatus: filters.value.paymentStatus || undefined,
      startDate: activeOrderTab.value === 'all' ? dateRange.value?.[0] || undefined : undefined,
      endDate: activeOrderTab.value === 'all' ? dateRange.value?.[1] || undefined : undefined,
      orderType: activeOrderTab.value !== 'all' ? activeOrderTab.value : undefined,
      operationDate: isTodayOperationTab.value ? operationDate.value || getTodayDate() : undefined,
    }

    const response = await getReservationsWithFilters(filterParams)
    if (response.success) {
      orders.value = response.data.content
      totalOrders.value = response.data.totalElements
    } else {
      ElMessage.error(response.message)
    }
  } catch (error) {
    console.error('加载订单数据失败:', error)
    ElMessage.error(t('order.messages.loadOrdersFailed'))
  } finally {
    loading.value = false
  }
}

const loadStatistics = async () => {
  try {
    const response = await getReservationStatistics()
    if (response.success) {
      statistics.value = response.data
    } else {
      ElMessage.error(response.message)
    }
  } catch (error) {
    console.error('加载统计数据失败:', error)
    ElMessage.error(t('order.messages.loadStatisticsFailed'))
  }
}

// 加载订单盒子数据
const loadOrderBox = async () => {
  try {
    const response = await getOrderBoxList()
    if (response.success) {
      orderBoxItems.value = response.data
      totalOrders.value = response.data.length
    } else {
      ElMessage.error(response.message)
    }
  } catch (error) {
    console.error('加载订单盒子失败:', error)
    ElMessage.error(t('order.messages.loadOrderBoxFailed'))
  }
}

const loadFilterOptions = async () => {
  try {
    const [channelResponse, roomTypeResponse] = await Promise.all([
      getAllChannels(),
      getAllRoomTypes(),
    ])

    if (channelResponse.success) {
      channelOptions.value = channelResponse.data
        .filter((channel) => channel.name && channel.name.trim().length > 0)
        .map((channel) => ({
          label: getChannelDisplayName(channel.name),
          value: channel.name,
        }))
    } else {
      ElMessage.warning(channelResponse.message || t('order.messages.loadChannelFiltersFailed'))
    }

    if (roomTypeResponse.success) {
      roomTypeOptions.value = roomTypeResponse.data
        .filter((roomType) => roomType.name && roomType.name.trim().length > 0)
        .map((roomType) => ({
          label: roomType.name,
          value: roomType.name,
        }))
    } else {
      ElMessage.warning(roomTypeResponse.message || t('order.messages.loadRoomTypeFiltersFailed'))
    }
  } catch (error) {
    console.error('加载筛选项失败:', error)
    ElMessage.warning(t('order.messages.loadFiltersFailed'))
  }
}

// 工具函数：判断是否为今天
const isToday = (dateStr: string) => {
  return dateStr === getStoreTodayYmd(currentStoreTimeZone.value)
}

// 筛选由后端处理，这里直接返回数据
const filteredOrders = computed(() => {
  if (activeOrderTab.value === 'order-box') {
    // 订单盒子标签显示订单盒子内的订单
    return orderBoxItems.value.map((item) => item.reservation)
  }
  return orders.value
})

// 页面交互方法
const handleMenuSelect = (index: string) => {
  activeMenu.value = index
}

const handleOrderTabChange = (tabName: string) => {
  console.log('切换标签页:', tabName)
  activeOrderTab.value = tabName
  console.log('设置 activeOrderTab 后:', activeOrderTab.value)
  if (isTodayOperationTab.value) {
    operationDate.value = getTodayDate()
  }
  currentPage.value = 1
  loadReservations()
}

const handleSearch = () => {
  currentPage.value = 1
  loadReservations()
}

const handleDateChange = (dates: [string, string] | null) => {
  if (dates) {
    dateRange.value = dates
  }
  currentPage.value = 1
  loadReservations()
}

const handleFilterChange = () => {
  currentPage.value = 1
  loadReservations()
}

const handleDateRangeChange = (value: [string, string] | null) => {
  // 处理日期范围变化
  dateRange.value = value
  currentPage.value = 1
  loadReservations()
}

const handleOperationDateChange = (value: string | null) => {
  operationDate.value = value || getTodayDate()
  currentPage.value = 1
  loadReservations()
}

const toggleFilters = () => {
  showAdvancedFilters.value = !showAdvancedFilters.value
}

const getChannelTagType = (channel: string) => {
  switch (channel) {
    case '直营客':
    case 'Direct Guest':
      return 'primary'
    case '美团民宿':
    case 'Meituan Homestay':
      return 'warning'
    case '途家':
    case 'Tujia':
      return 'success'
    default:
      return 'info'
  }
}

const getReservationStatusText = (status?: string) => {
  const normalized = (status || '').toUpperCase()
  switch (normalized) {
    case 'CONFIRMED':
      return t('order.status.confirmed')
    case 'REQUESTED':
      return t('order.status.requested')
    case 'CHECKED_IN':
      return t('order.status.checkedIn')
    case 'CHECKED_OUT':
      return t('order.status.checkedOut')
    case 'CANCELLED':
      return t('order.status.cancelled')
    default:
      return status || '-'
  }
}

const getReservationStatusTagType = (status?: string) => {
  const normalized = (status || '').toUpperCase()
  switch (normalized) {
    case 'CHECKED_IN':
      return 'success'
    case 'CANCELLED':
      return 'danger'
    case 'REQUESTED':
      return 'warning'
    default:
      return 'info'
  }
}

const canAssignRoom = (order: ReservationDTO) => {
  const normalized = (order.status || '').toUpperCase()
  return normalized === 'CONFIRMED' || normalized === 'REQUESTED' || normalized === 'CHECKED_IN'
}

const getAssignStatusText = (order: ReservationDTO) => {
  if (!order.roomId) {
    return t('order.assignStatus.unassigned')
  }
  return canAssignRoom(order)
    ? t('order.assignStatus.assigned')
    : t('order.assignStatus.assignedNoInventory')
}

const getAssignStatusTagType = (order: ReservationDTO) => {
  if (!order.roomId) {
    return 'warning'
  }
  return canAssignRoom(order) ? 'success' : 'info'
}

const formatAmount = (value?: number) => {
  const numericValue = Number(value ?? 0)
  if (!Number.isFinite(numericValue)) {
    return '¥0.00'
  }
  return `¥${numericValue.toLocaleString('en-US', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })}`
}

const getCheckinTypeText = (_order: ReservationDTO) => t('order.options.normalCheckin')

const getTotalGuests = (order: ReservationDTO) => {
  const adults = order.adults ?? 0
  const children = order.children ?? 0
  const total = adults + children
  return total > 0 ? total : '-'
}

const getNights = (order: ReservationDTO) => {
  if (!order.checkInDate || !order.checkOutDate) {
    return '-'
  }
  const checkInDate = normalizeYmdInput(order.checkInDate, '')
  const checkOutDate = normalizeYmdInput(order.checkOutDate, '')
  const nights = diffYmdDays(checkInDate, checkOutDate)
  if (nights <= 0) {
    return '-'
  }
  return nights
}

const formatStayDateTime = (dateStr?: string, type: 'checkIn' | 'checkOut' = 'checkIn') => {
  if (!dateStr) return '-'
  const defaultTime = type === 'checkIn' ? '16:00:00' : '10:00:00'
  const normalizedDate = normalizeYmdInput(dateStr, '')
  return `${normalizedDate} ${defaultTime}`
}

const getSettlementStatusText = (order: ReservationDTO) => {
  const paidAmount = Number(order.paidAmount ?? 0)
  const totalAmount = Number(order.totalAmount ?? 0)
  if (totalAmount > 0 && paidAmount >= totalAmount) {
    return t('order.settlement.settled')
  }
  if (paidAmount > 0 && paidAmount < totalAmount) {
    return t('order.settlement.partiallySettled')
  }
  return t('order.settlement.unsettled')
}

const getSettlementTagType = (order: ReservationDTO) => {
  const settlementStatus = getSettlementStatusText(order)
  if (settlementStatus === t('order.settlement.settled')) {
    return 'success'
  }
  if (settlementStatus === t('order.settlement.partiallySettled')) {
    return 'warning'
  }
  return 'danger'
}

const getCashierStatusText = (order: ReservationDTO) => {
  const paidAmount = Number(order.paidAmount ?? 0)
  const totalAmount = Number(order.totalAmount ?? 0)
  if (totalAmount > 0 && paidAmount >= totalAmount) {
    return t('order.settlement.balanced')
  }
  if (paidAmount > 0) {
    return t('order.settlement.partialPaid')
  }
  return t('order.settlement.pendingPayment')
}

const isOrderSettled = (order: ReservationDTO) => {
  const totalAmount = Number(order.totalAmount ?? 0)
  const paidAmount = Number(order.paidAmount ?? 0)
  const status = (order.status || '').toUpperCase()
  const hasSuSource = Boolean(order.suReservationId?.trim())
  const checkedInOrOut = status === 'CHECKED_IN' || status === 'CHECKED_OUT'
  const amountSettled = totalAmount > 0 && paidAmount >= totalAmount
  return Boolean(order.settled) || hasSuSource || checkedInOrOut || amountSettled
}

const getSettlementStatusTextV2 = (order: ReservationDTO) => {
  if (isOrderSettled(order)) {
    return t('order.settlement.settled')
  }
  const paidAmount = Number(order.paidAmount ?? 0)
  return paidAmount > 0 ? t('order.settlement.partiallySettled') : t('order.settlement.unsettled')
}

const getSettlementTagTypeV2 = (order: ReservationDTO) => {
  const settlementStatus = getSettlementStatusTextV2(order)
  if (settlementStatus === t('order.settlement.settled')) {
    return 'success'
  }
  if (settlementStatus === t('order.settlement.partiallySettled')) {
    return 'warning'
  }
  return 'danger'
}

const getCashierStatusTextV2 = (order: ReservationDTO) => {
  if (isOrderSettled(order)) {
    return t('order.settlement.balanced')
  }
  const paidAmount = Number(order.paidAmount ?? 0)
  return paidAmount > 0 ? t('order.settlement.partialPaid') : t('order.settlement.pendingPayment')
}

const getSettlementSelectValue = (order: ReservationDTO) => {
  return isOrderSettled(order) ? 'paid' : 'unpaid'
}

const formatDateTime = (dateStr: string, sourceTimeZone?: string) => {
  if (!dateStr) return '-'
  return formatReservationTimestamp(dateStr, sourceTimeZone, currentStoreTimeZone.value, true)
}

const getChannelDisplayName = (value?: string) => {
  const normalized = value?.trim().toLowerCase() || ''
  const channelMap: Record<string, string> = {
    direct: t('pages.home.guest.directGuest'),
    'direct guest': t('pages.home.guest.directGuest'),
    直营客: t('pages.home.guest.directGuest'),
    自来客: t('pages.home.guest.directGuest'),
    散客: t('pages.home.guest.directGuest'),
    直接来店客: t('pages.home.guest.directGuest'),
    meituan: t('pages.home.roomStatusChannel.channels.meituan'),
    'meituan homestay': t('pages.home.roomStatusChannel.channels.meituan'),
    美团民宿: t('pages.home.roomStatusChannel.channels.meituan'),
    美團民宿: t('pages.home.roomStatusChannel.channels.meituan'),
    tujia: 'Tujia',
    途家: 'Tujia',
  }
  return channelMap[normalized] || value || '-'
}

const getCheckinTypeDisplayName = (value: string) => {
  const typeMap: Record<string, string> = {
    normal: t('order.options.normalCheckin'),
    early: t('order.options.earlyCheckin'),
    late: t('order.options.lateCheckin'),
  }
  return typeMap[value] || value
}

const formatAssignableRoomTypeLabel = (roomType: AssignableRoomTypeDTO) =>
  t('order.assignDialog.roomTypeAvailable', {
    name: roomType.name,
    count: roomType.availableRooms,
  })

// 分页方法
const handleSizeChange = (size: number) => {
  pageSize.value = size
  currentPage.value = 1
  loadReservations()
}

const handleCurrentChange = (page: number) => {
  currentPage.value = page
  loadReservations()
}

const showOrderDetailDrawer = ref(false)
const selectedReservationId = ref<number | null>(null)

const showAssignRoomDialog = ref(false)
const assignTargetReservationId = ref<number | null>(null)
const assignableRoomsLoading = ref(false)
const assignRoomSubmitting = ref(false)
const assignRoomTypeId = ref<number | null>(null)
const assignRoomId = ref<number | null>(null)
const assignableRoomTypes = ref<AssignableRoomTypeDTO[]>([])
const assignableRooms = ref<AssignableRoomDTO[]>([])
const settlementUpdatingOrderId = ref<number | null>(null)

// 订单操作方法
const handleSettlementStatusChange = async (order: ReservationDTO, value: string) => {
  if (!order?.id) return
  settlementUpdatingOrderId.value = order.id
  try {
    const response = await updateReservationSettlementStatus(order.id, {
      settled: value === 'paid',
    })
    if (!response.success) {
      ElMessage.error(response.message || t('order.messages.updateSettlementFailed'))
      return
    }
    Object.assign(order, response.data)
    ElMessage.success(
      value === 'paid' ? t('order.settlement.updatedPaid') : t('order.settlement.updatedUnpaid'),
    )
    if (filters.value.paymentStatus) {
      await loadReservations()
    }
  } catch (error) {
    console.error('更新结账状态失败:', error)
    ElMessage.error(t('order.messages.updateSettlementFailed'))
  } finally {
    settlementUpdatingOrderId.value = null
  }
}

const viewOrder = (order: ReservationDTO) => {
  selectedReservationId.value = order.id
  showOrderDetailDrawer.value = true
}

const resetAssignRoomState = () => {
  assignRoomTypeId.value = null
  assignRoomId.value = null
  assignableRoomTypes.value = []
  assignableRooms.value = []
}

const handleAssignDialogClosed = () => {
  assignTargetReservationId.value = null
  resetAssignRoomState()
}

const refreshAssignableRoomTypes = async () => {
  const reservationId = assignTargetReservationId.value
  if (!reservationId) return
  assignableRoomsLoading.value = true
  try {
    const res = await getAssignableRooms(reservationId)
    if (!res.success) {
      ElMessage.error(res.message || t('order.messages.loadAssignableRoomTypesFailed'))
      return
    }
    assignableRoomTypes.value = res.data.roomTypes || []
    assignRoomTypeId.value = null
    assignRoomId.value = null
    assignableRooms.value = []
  } catch (error) {
    console.error('加载可用房型失败:', error)
    ElMessage.error(t('order.messages.loadAssignableRoomTypesFailed'))
  } finally {
    assignableRoomsLoading.value = false
  }
}

const handleAssignRoomTypeChange = async (roomTypeId: number) => {
  const reservationId = assignTargetReservationId.value
  if (!reservationId) return
  assignRoomId.value = null
  assignableRoomsLoading.value = true
  try {
    const res = await getAssignableRooms(reservationId, roomTypeId)
    if (!res.success) {
      ElMessage.error(res.message || t('order.messages.loadAssignableRoomsFailed'))
      return
    }
    assignableRooms.value = res.data.rooms || []
  } catch (error) {
    console.error('加载可用房间失败:', error)
    ElMessage.error(t('order.messages.loadAssignableRoomsFailed'))
  } finally {
    assignableRoomsLoading.value = false
  }
}

const submitAssignRoom = async () => {
  const reservationId = assignTargetReservationId.value
  const roomId = assignRoomId.value
  if (!reservationId || !roomId) return
  assignRoomSubmitting.value = true
  try {
    const res = await assignReservationRoom(reservationId, roomId)
    if (!res.success) {
      ElMessage.error(res.message || t('order.messages.assignFailed'))
      return
    }
    ElMessage.success(t('order.messages.assignSuccess'))
    showAssignRoomDialog.value = false
    await handleReservationUpdated()
  } catch (error) {
    console.error('排房失败:', error)
    ElMessage.error(t('order.messages.assignFailed'))
  } finally {
    assignRoomSubmitting.value = false
  }
}

const openAssignRoom = async (order: ReservationDTO) => {
  if (!canAssignRoom(order)) {
    ElMessage.warning(t('order.messages.unsupportedAssignStatus'))
    return
  }
  assignTargetReservationId.value = order.id
  resetAssignRoomState()
  showAssignRoomDialog.value = true
  await refreshAssignableRoomTypes()
}

const getDisplayChannelOrderNumber = (order: ReservationDTO) => {
  const channelOrderNumber = order.channelOrderNumber?.trim()
  if (channelOrderNumber) {
    return channelOrderNumber
  }

  const orderNumber = order.orderNumber?.trim() || ''
  const underscoreIndex = orderNumber.indexOf('_')
  if (underscoreIndex > 0) {
    return orderNumber.slice(0, underscoreIndex)
  }
  return '-'
}

const handleReservationUpdated = async () => {
  await Promise.all([loadReservations(), loadStatistics()])
}

onMounted(() => {
  // 处理路由参数，设置初始标签页
  const type = route.query.type as string
  if (type) {
    const mappedTab = mapTypeToTab(type)
    if (mappedTab) {
      activeOrderTab.value = mappedTab
    }
  }

  loadFilterOptions()
  loadReservations()
  loadStatistics()
})
</script>

<style scoped>
.order-management {
  display: flex;
  height: 100vh;
  background: #f5f5f5;
}

/* 左侧导航 */
.sidebar {
  width: 200px;
  background: white;
  border-right: 1px solid #e8e8e8;
  display: flex;
  flex-direction: column;
  transition: width 0.3s ease;
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
.main-content {
  flex: 1;
  overflow: auto;
  background: #f5f5f5;
}

/* 订单状态标签页 */
.order-tabs {
  background: white;
  border-bottom: 1px solid #e8e8e8;
  padding: 0 20px;
}

.order-tabs :deep(.el-tabs__header) {
  margin: 0;
}

.tab-label-with-badge {
  display: flex;
  align-items: center;
  gap: 6px;
}

.custom-badge {
  display: inline-block;
  min-width: 16px;
  height: 16px;
  line-height: 16px;
  text-align: center;
  font-size: 11px;
  color: white;
  background-color: #409eff;
  border-radius: 8px;
  padding: 0 4px;
  font-weight: 500;
  margin-left: 4px;
}

/* 搜索和筛选区域 */
.search-filter-section {
  background: white;
  padding: 20px;
  margin-bottom: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.search-bar {
  margin-bottom: 16px;
}

.search-input {
  max-width: 600px;
}

.date-filter {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.filter-row {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.main-filter-row {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.filter-group {
  display: flex;
  align-items: center;
  gap: 8px;
}

.filter-label {
  font-size: 14px;
  color: #666;
  white-space: nowrap;
  min-width: 60px;
}

.filter-group .el-select {
  width: 120px;
}

.date-filter-group {
  display: flex;
  align-items: center;
  gap: 8px;
}

.date-filter-group .el-date-editor {
  width: 280px;
}

.single-date-filter .el-date-editor {
  width: 160px;
}

.filter-actions {
  margin-left: auto;
}

/* 订单列表 */
/* 订单盒子提示 */
.order-box-notice {
  margin-bottom: 16px;
}

.notice-content {
  line-height: 1.8;
}

.notice-content p {
  margin: 4px 0;
}

.order-list {
  background: white;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.order-number {
  display: flex;
  flex-direction: column;
  gap: 4px;
  align-items: flex-start;
  width: 100%;
}

.order-link {
  color: #1890ff;
  padding: 0;
  font-size: 14px;
  text-align: left;
  white-space: normal;
  word-break: break-all;
  line-height: 1.35;
}
.order-link :deep(.el-button__text) {
  white-space: normal;
  word-break: break-all;
  text-align: left;
  display: block;
  width: 100%;
}
.order-link:hover :deep(.el-button__text) {
  color: #409eff;
  text-decoration: underline;
}

.channel-order {
  font-size: 12px;
  color: #999;
}

.channel-order-link {
  padding: 0;
  font-size: 12px;
  color: #999;
  text-align: left;
  white-space: normal;
  word-break: break-all;
  line-height: 1.35;
}
.channel-order-link :deep(.el-button__text) {
  white-space: normal;
  word-break: break-all;
  text-align: left;
  display: block;
  width: 100%;
}
.channel-order-link:hover :deep(.el-button__text) {
  color: #409eff;
  text-decoration: underline;
}

.order-link,
.channel-order-link {
  width: 100%;
  margin: 0 !important;
  justify-content: flex-start !important;
  height: auto;
}

.order-detail-tip {
  margin-top: 2px;
  font-size: 11px;
  color: #8c8c8c;
}

/* 分页 */
.pagination-wrapper {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid #e8e8e8;
}

.pagination-info {
  font-size: 14px;
  color: #666;
}

.assign-room-panel {
  padding: 20px;
  border: 1px solid #dcdfe6;
  border-radius: 12px;
  background: #f7f8fa;
}

.panel-title {
  margin: 0 0 18px 0;
  font-size: 18px;
  font-weight: 700;
  line-height: 1.2;
}

.assign-row {
  display: flex;
  align-items: center;
  gap: 18px;
  margin-bottom: 20px;
}

.assign-label {
  width: 120px;
  font-size: 16px;
  color: #606266;
}

.assign-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 10px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .order-management {
    flex-direction: column;
  }

  .sidebar {
    width: 100%;
    height: auto;
  }

  .filter-row {
    flex-direction: column;
    align-items: stretch;
    gap: 12px;
  }

  .filter-group {
    justify-content: space-between;
  }

  .filter-group .el-select {
    width: auto;
    min-width: 120px;
  }
}
</style>
