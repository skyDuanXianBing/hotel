<template>
  <div class="order-management" :class="{ 'is-sidebar-collapsed': isCollapsed }" :style="shellStyle">
    <aside class="order-sidebar" :class="{ 'is-collapsed': isCollapsed }">
      <button type="button" class="sidebar-toggle" @click="toggleSidebar">
        <span class="sidebar-toggle-mark">
          <el-icon><MenuIcon /></el-icon>
        </span>
        <span v-if="!isCollapsed" class="sidebar-toggle-label">
          {{ t('order.sidebar.collapse') }}
        </span>
        <el-icon v-if="!isCollapsed" class="sidebar-toggle-arrow"><ArrowLeft /></el-icon>
        <el-icon v-else class="sidebar-toggle-arrow"><ArrowRight /></el-icon>
      </button>

      <nav class="sidebar-nav" aria-label="Order navigation">
        <button
          type="button"
          class="sidebar-parent"
          :class="{ 'is-active': activeMenu === 'order-management' }"
          :title="isCollapsed ? t('order.sidebar.accommodationOrders') : undefined"
          @click="handleMenuSelect('order-management')"
        >
          <span class="sidebar-parent-icon">
            <el-icon><Document /></el-icon>
          </span>
          <span v-if="!isCollapsed" class="sidebar-parent-label">
            {{ t('order.sidebar.accommodationOrders') }}
          </span>
        </button>
      </nav>
    </aside>

    <section class="order-panel">
      <header class="order-panel-header">
        <AppTopNav
          v-bind="topNavBindings.props.value"
          @store-select="topNavBindings.onStoreSelect"
          @manage-stores="topNavBindings.onManageStores"
          @menu-click="topNavBindings.onMenuClick"
          @wallet-click="topNavBindings.onWalletClick"
          @inbox-click="topNavBindings.onInboxClick"
          @support-chat="topNavBindings.onSupportChat"
          @system-notification="topNavBindings.onSystemNotification"
          @order-notification="topNavBindings.onOrderNotification"
          @profile-click="topNavBindings.onProfileClick"
          @logout="topNavBindings.onLogout"
        />
      </header>

      <main class="main-content">
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
          <el-tab-pane :label="t('order.tabs.assigned')" name="assigned">
            <template #label>
              <span>{{ t('order.tabs.assigned') }}</span>
            </template>
          </el-tab-pane>
          <el-tab-pane name="pending">
            <template #label>
              <div class="tab-label-with-badge">
                <span>{{ t('order.tabs.pending') }}</span>
                <span v-if="pendingCount > 0" class="custom-badge">{{ pendingCount }}</span>
              </div>
            </template>
          </el-tab-pane>
          <el-tab-pane :label="t('order.tabs.orderBox')" name="order-box">
            <template #label>
              <span>{{ t('order.tabs.orderBox') }}</span>
            </template>
          </el-tab-pane>
          <el-tab-pane :label="t('order.tabs.deletedRooms')" name="deleted-rooms">
            <template #label>
              <span>{{ t('order.tabs.deletedRooms') }}</span>
            </template>
          </el-tab-pane>
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
          <div class="filter-group filter-group--channel">
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
          <div class="filter-group filter-group--room">
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
              :clearable="false"
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
              :clearable="false"
              @change="handleFilterChange"
            />
          </div>
        </div>

        <!-- 第二行：详细筛选器 -->
        <div class="filter-row">
          <div class="filter-group filter-group--checkin">
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

          <div class="filter-group filter-group--status">
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

          <div class="filter-group filter-group--payment">
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
            width="220"
            fixed="left"
          >
            <template #default="scope">
              <div class="order-summary">
                <el-button
                  type="text"
                  class="order-link"
                  :title="t('order.table.detailTitle')"
                  @click="viewOrder(scope.row)"
                >
                  {{ getOrderPrimaryDisplay(scope.row) }}
                </el-button>
                <div class="order-summary-meta">
                  <span
                    v-for="item in getOrderSecondaryDisplayItems(scope.row)"
                    :key="item"
                    class="order-summary-meta-item"
                  >
                    {{ item }}
                  </span>
                </div>
                <el-button
                  v-if="getDisplayChannelOrderNumber(scope.row)"
                  type="text"
                  class="channel-order-link"
                  :title="t('order.table.detailTitle')"
                  @click="viewOrder(scope.row)"
                >
                  {{ t('order.table.channelOrderNumber') }}:
                  {{ getDisplayChannelOrderNumber(scope.row) }}
                </el-button>
                <span class="order-detail-tip">{{ t('order.table.detailTip') }}</span>
              </div>
            </template>
          </el-table-column>

          <el-table-column
            prop="channelName"
            :label="t('order.table.channel')"
            width="110"
            class-name="channel-column"
          >
            <template #default="scope">
              <el-tag
                size="small"
                class="channel-tag"
                :style="getChannelTagStyle(scope.row)"
              >
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
              <el-tag
                size="small"
                class="stay-status-tag"
                :class="getReservationStatusTagClass(scope.row.status)"
              >
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
          >
            <template #default="scope">
              <span>{{ getRoomTypeDisplay(scope.row) }}</span>
            </template>
          </el-table-column>

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
            :label="t('order.mapping.channelRoomType')"
            min-width="220"
          >
            <template #default="scope">
              <div class="room-mapping-cell">
                <div
                  v-for="item in getChannelRoomDisplayItems(scope.row)"
                  :key="item.label"
                  class="room-mapping-line"
                >
                  <span class="room-mapping-label">{{ item.label }}</span>
                  <span class="room-mapping-value">{{ item.value }}</span>
                </div>
                <el-tag size="small" :type="getAssignStatusTagType(scope.row)">
                  {{ getAssignStatusText(scope.row) }}
                </el-tag>
              </div>
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
              <el-tag
                size="small"
                class="stay-status-tag"
                :class="getReservationStatusTagClass(scope.row.status)"
              >
                {{ getReservationStatusText(scope.row.status) }}
              </el-tag>
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
      </main>
    </section>

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
import { ref, computed, inject, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import {
  ArrowLeft,
  ArrowRight,
  Document,
  Search,
  Menu as MenuIcon,
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getAllChannels, type ChannelDTO } from '@/api/channel'
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
import AppTopNav from '@/components/layout/AppTopNav.vue'
import { appTopNavBindingsKey } from '@/components/layout/appShellContext'
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
const topNavBindings = inject(appTopNavBindingsKey)
const storeStore = useStoreStore()
const currentStoreTimeZone = computed(() => resolveStoreTimeZone(storeStore.currentStore?.timezone))

if (!topNavBindings) {
  throw new Error('OrderManagement requires top navigation bindings')
}

const SIDEBAR_STORAGE_KEY = 'order-sidebar-collapsed'
const COLLAPSED_WIDTH = 84
const EXPANDED_WIDTH = 220

const isCollapsed = ref(
  typeof window === 'undefined'
    ? false
    : localStorage.getItem(SIDEBAR_STORAGE_KEY) === 'true',
)

const shellStyle = computed(() => ({
  '--sidebar-width': `${isCollapsed.value ? COLLAPSED_WIDTH : EXPANDED_WIDTH}px`,
}))

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
const getTodayDate = () => getStoreTodayYmd(currentStoreTimeZone.value)
const operationDate = ref<string | null>(getTodayDate())
const loading = ref(false)
type ChannelOption = Pick<ChannelDTO, 'id' | 'color'> & {
  label: string
  value: string
}

const channelOptions = ref<ChannelOption[]>([])
const roomTypeOptions = ref<Array<{ label: string; value: string }>>([])

const normalizeChannelKey = (value?: string | null) => (value || '').trim().toLowerCase()

const channelColorMap = computed(() => {
  const map = new Map<string, string>()

  channelOptions.value.forEach((channel) => {
    if (!channel.color) return

    map.set(`id:${channel.id}`, channel.color)

    const rawName = normalizeChannelKey(channel.value)
    if (rawName) {
      map.set(`name:${rawName}`, channel.color)
    }

    const displayName = normalizeChannelKey(channel.label)
    if (displayName) {
      map.set(`name:${displayName}`, channel.color)
    }
  })

  return map
})

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
  ['today-checkin', 'today-checkout', 'today-new'].includes(activeOrderTab.value),
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
    const normalizedSearchQuery = searchQuery.value.trim()
    const filterParams = {
      page: currentPage.value - 1, // 后端从0开始计数
      size: pageSize.value,
      searchKeyword: normalizedSearchQuery || undefined,
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
          id: channel.id,
          color: channel.color,
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

const getChannelTagStyle = (order: ReservationDTO) => {
  const rawName = order.channelName || ''
  const displayName = getChannelDisplayName(rawName)
  const color =
    channelColorMap.value.get(`id:${order.channelId}`) ||
    channelColorMap.value.get(`name:${normalizeChannelKey(rawName)}`) ||
    channelColorMap.value.get(`name:${normalizeChannelKey(displayName)}`) ||
    '#627083'

  return { '--channel-tag-bg': color, '--channel-tag-color': '#ffffff' }
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

const getReservationStatusTagClass = (status?: string) => {
  const normalized = (status || '').toUpperCase()
  switch (normalized) {
    case 'CHECKED_IN':
      return 'stay-status-tag--checked-in'
    case 'CHECKED_OUT':
    case 'CANCELLED':
      return 'stay-status-tag--checked-out'
    default:
      return 'stay-status-tag--not-checked-in'
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

const getTrimmedValue = (value?: string | null) => {
  return value?.trim() || ''
}

const getParsedOtaRoomNumber = (order: ReservationDTO) => {
  const otaRoomId = getTrimmedValue(order.otaRoomId)
  if (!otaRoomId) {
    return ''
  }

  const separatorIndex = otaRoomId.indexOf('-')
  if (separatorIndex < 0 || separatorIndex === otaRoomId.length - 1) {
    return ''
  }

  return otaRoomId.slice(separatorIndex + 1).trim()
}

const hasChannelRoomReference = (order: ReservationDTO) => {
  return Boolean(getTrimmedValue(order.otaRoomId) || order.otaRoomTypeId)
}

const getRoomTypeDisplay = (order: ReservationDTO) => {
  const roomTypeName = getTrimmedValue(order.roomTypeName)
  if (roomTypeName) {
    return roomTypeName
  }

  if (hasChannelRoomReference(order)) {
    return t('order.tabs.unassigned')
  }

  return '-'
}

const getChannelRoomDisplayItems = (order: ReservationDTO) => {
  const displayItems: Array<{ label: string; value: string }> = []

  if (hasChannelRoomReference(order)) {
    displayItems.push({
      label: t('order.mapping.channelRoomType'),
      value: t('order.tabs.unassigned'),
    })
  } else {
    displayItems.push({
      label: t('order.mapping.channelRoomType'),
      value: '-',
    })
  }

  const parsedRoomNumber = getParsedOtaRoomNumber(order)
  if (parsedRoomNumber) {
    displayItems.push({
      label: t('order.table.roomNumber'),
      value: parsedRoomNumber,
    })
  }

  const roomTypeName = getTrimmedValue(order.roomTypeName)
  if (roomTypeName) {
    displayItems.push({
      label: t('order.mapping.pmsRoomType'),
      value: roomTypeName,
    })
  }

  return displayItems
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
    direct: t('order.channelLabels.directGuest'),
    'direct guest': t('order.channelLabels.directGuest'),
    直营客: t('order.channelLabels.directGuest'),
    自来客: t('order.channelLabels.directGuest'),
    散客: t('order.channelLabels.directGuest'),
    直接来店客: t('order.channelLabels.directGuest'),
    meituan: t('order.channelLabels.meituan'),
    'meituan homestay': t('order.channelLabels.meituan'),
    美团民宿: t('order.channelLabels.meituan'),
    美團民宿: t('order.channelLabels.meituan'),
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

const isInternalOrderNumber = (value?: string | null) => {
  const normalized = getTrimmedValue(value)
  return /^RSV[A-Z0-9]{16,}$/i.test(normalized)
}

const getDisplayChannelOrderNumber = (order: ReservationDTO) => {
  const channelOrderNumber = getTrimmedValue(order.channelOrderNumber)
  if (channelOrderNumber && !isInternalOrderNumber(channelOrderNumber)) {
    return channelOrderNumber
  }

  const orderNumber = getTrimmedValue(order.orderNumber)
  const underscoreIndex = orderNumber.indexOf('_')
  if (underscoreIndex > 0) {
    const channelOrderCandidate = orderNumber.slice(0, underscoreIndex).trim()
    if (channelOrderCandidate && !isInternalOrderNumber(channelOrderCandidate)) {
      return channelOrderCandidate
    }
  }
  return ''
}

const formatOrderStayDateRange = (order: ReservationDTO) => {
  const checkInDate = normalizeYmdInput(order.checkInDate, '')
  const checkOutDate = normalizeYmdInput(order.checkOutDate, '')
  if (checkInDate && checkOutDate) {
    return `${checkInDate} - ${checkOutDate}`
  }
  return checkInDate || checkOutDate
}

const getOrderRoomDescription = (order: ReservationDTO) => {
  const roomTypeDisplay = getRoomTypeDisplay(order)
  const roomTypeName = roomTypeDisplay === '-' ? '' : roomTypeDisplay
  const roomNumber = getTrimmedValue(order.roomNumber)

  if (roomTypeName && roomNumber) {
    return `${roomTypeName} / ${roomNumber}`
  }
  return roomTypeName || roomNumber
}

const getReadableChannelName = (order: ReservationDTO) => {
  const channelName = getChannelDisplayName(order.channelName)
  if (channelName === '-') {
    return ''
  }
  return channelName
}

const getOrderPrimaryDisplay = (order: ReservationDTO) => {
  const guestName = getTrimmedValue(order.guestName)
  const stayDateRange = formatOrderStayDateRange(order)
  const channelName = getReadableChannelName(order)
  const roomDescription = getOrderRoomDescription(order)

  if (guestName && stayDateRange) {
    return `${guestName} · ${stayDateRange}`
  }
  if (guestName) {
    return guestName
  }
  if (channelName && stayDateRange) {
    return `${channelName} · ${stayDateRange}`
  }
  if (stayDateRange) {
    return stayDateRange
  }
  if (roomDescription) {
    return roomDescription
  }

  const channelOrderNumber = getDisplayChannelOrderNumber(order)
  if (channelOrderNumber) {
    return channelOrderNumber
  }
  return t('order.table.detailTitle')
}

const getOrderSecondaryDisplayItems = (order: ReservationDTO) => {
  const displayItems: string[] = []
  const channelName = getReadableChannelName(order)
  const roomDescription = getOrderRoomDescription(order)
  const stayDateRange = formatOrderStayDateRange(order)
  const primaryDisplay = getOrderPrimaryDisplay(order)

  if (channelName && !primaryDisplay.includes(channelName)) {
    displayItems.push(channelName)
  }
  if (roomDescription && !primaryDisplay.includes(roomDescription)) {
    displayItems.push(roomDescription)
  }

  if (stayDateRange && !primaryDisplay.includes(stayDateRange)) {
    displayItems.push(stayDateRange)
  }

  return displayItems
}

const handleReservationUpdated = async () => {
  await Promise.all([loadReservations(), loadStatistics()])
}

watch(
  isCollapsed,
  (collapsed) => {
    if (typeof window === 'undefined') return
    localStorage.setItem(SIDEBAR_STORAGE_KEY, String(collapsed))
  },
  { immediate: true },
)

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
  --sidebar-width: 84px;
  height: 100vh;
  display: grid;
  grid-template-columns: var(--sidebar-width) minmax(0, 1fr);
  background: #f5f5f5;
}

.order-sidebar {
  width: var(--sidebar-width);
  display: flex;
  flex-direction: column;
  min-height: 0;
  background: #ffffff;
  border-right: 1px solid #ecece7;
  transition: width 0.24s ease;
  overflow: hidden;
}

.sidebar-toggle {
  height: 76px;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 20px;
  border: none;
  border-bottom: 1px solid #f0f0ea;
  background: #ffffff;
  color: #1f2120;
  cursor: pointer;
}

.sidebar-toggle-mark {
  width: 20px;
  height: 20px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #2f7cf6;
  font-size: 20px;
  flex: 0 0 auto;
}

.sidebar-toggle-label {
  flex: 1;
  min-width: 0;
  text-align: left;
  font-size: 14px;
  font-weight: 600;
  color: #232421;
  white-space: nowrap;
}

.sidebar-toggle-arrow {
  color: #a8ada8;
  font-size: 14px;
  flex: 0 0 auto;
}

.sidebar-nav {
  flex: 1;
  min-height: 0;
  padding: 18px 0 24px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.sidebar-parent {
  position: relative;
  height: 44px;
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  padding: 0 18px 0 20px;
  border: none;
  border-radius: 0;
  background: transparent;
  color: #5f645f;
  cursor: pointer;
  text-align: left;
  transition:
    background-color 0.2s ease,
    color 0.2s ease;
}

.sidebar-parent:hover {
  background: #f6f7f3;
  color: #20211d;
}

.sidebar-parent.is-active {
  background: #eef5ff;
  color: #2f7cf6;
}

.sidebar-parent-icon {
  width: 18px;
  height: 18px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  flex: 0 0 auto;
}

.sidebar-parent-label {
  min-width: 0;
  font-size: 14px;
  font-weight: 600;
  white-space: nowrap;
}

.order-sidebar.is-collapsed .sidebar-toggle {
  justify-content: center;
  padding: 0;
}

.order-sidebar.is-collapsed .sidebar-parent {
  justify-content: center;
  padding: 0;
}

.order-panel {
  min-width: 0;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.order-panel-header {
  padding: 18px 32px 14px;
  background: #f5f5f5;
  overflow: hidden;
}

.order-panel-header :deep(.top-nav) {
  --nav-center-shift: calc(-56px + ((var(--sidebar-width) - 84px) / 6));
  --nav-right-shift: -28px;
}

.main-content {
  flex: 1;
  min-height: 0;
  overflow: auto;
  background: #f5f5f5;
  padding: 0 24px 24px;
}

.order-tabs {
  --order-tabs-center-shift: calc(-56px + ((var(--sidebar-width, 84px) - 84px) / 6) + 20px);
  box-sizing: border-box;
  display: flex;
  justify-content: center;
  min-width: 1120px;
  padding: 2px 0 16px;
  background: transparent;
  border-bottom: none;
}

.order-tabs :deep(.el-tabs) {
  display: flex;
  justify-content: center;
  width: max-content;
  transform: translateX(var(--order-tabs-center-shift));
  transition: transform 0.24s ease;
}

.order-tabs :deep(.el-tabs__header) {
  margin: 0;
}

.order-tabs :deep(.el-tabs__nav-wrap) {
  overflow: visible;
}

.order-tabs :deep(.el-tabs__nav-wrap::after),
.order-tabs :deep(.el-tabs__active-bar) {
  display: none;
}

.order-tabs :deep(.el-tabs__nav-scroll) {
  display: flex;
  justify-content: center;
  overflow: visible;
}

.order-tabs :deep(.el-tabs__nav) {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  float: none;
  height: 28px;
  padding: 2px;
  border-radius: 999px;
  background: #ffffff;
  box-shadow: 0 1px 8px rgba(30, 30, 30, 0.04);
}

.order-tabs :deep(.el-tabs__item) {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 78px;
  height: 24px;
  padding: 0 12px !important;
  border-radius: 999px;
  color: #252525;
  font-size: 12px;
  font-weight: 600;
  line-height: 1;
  white-space: nowrap;
  transition:
    background-color 0.2s ease,
    color 0.2s ease;
}

.order-tabs :deep(.el-tabs__item:hover:not(.is-active)) {
  background: #f2f2f2;
  color: #111111;
}

.order-tabs :deep(.el-tabs__item.is-active),
.order-tabs :deep(.el-tabs__item.is-active:hover) {
  background: #111111;
  color: #ffffff;
}

.tab-label-with-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  min-height: 24px;
}

.custom-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 14px;
  height: 14px;
  padding: 0 4px;
  border-radius: 999px;
  background: #ff6267;
  color: #ffffff;
  font-size: 10px;
  font-weight: 700;
  line-height: 14px;
  transform: translateY(-1px);
}

.search-filter-section {
  box-sizing: border-box;
  min-width: 1120px;
  display: flex;
  align-items: center;
  gap: 8px;
  overflow: visible;
  margin-bottom: 22px;
  padding: 12px 16px;
  border: none;
  border-radius: 0;
  background: #ffffff;
  box-shadow: none;
}

.search-input {
  order: 1;
  width: 188px;
  flex: 0 0 188px;
}

.filter-row,
.main-filter-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0;
  flex-wrap: nowrap;
  min-width: max-content;
  flex: 0 0 auto;
}

.filter-group {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 0 0 auto;
}

.date-filter-group {
  order: 2;
}

.filter-group--room {
  order: 3;
}

.filter-group--channel {
  order: 4;
}

.filter-group--checkin {
  order: 5;
}

.filter-group--status {
  order: 6;
}

.filter-group--payment {
  order: 7;
}

.filter-label {
  flex: 0 0 auto;
  font-size: 13px;
  color: #3f4347;
  white-space: nowrap;
  min-width: 0;
  line-height: 1;
  font-weight: 600;
}

.filter-group .el-select {
  width: 120px;
  flex: 0 0 120px;
}

.filter-row .filter-group .el-select {
  width: 108px;
  flex-basis: 108px;
}

.date-filter-group {
  display: flex;
  align-items: center;
  gap: 8px;
}

.date-filter-group .el-date-editor,
.date-filter-group :deep(.el-date-editor) {
  width: 242px;
  flex: 0 0 242px;
}

.single-date-filter .el-date-editor,
.single-date-filter :deep(.el-date-editor) {
  width: 152px;
  flex: 0 0 152px;
}

.search-filter-section :deep(.el-input__wrapper),
.search-filter-section :deep(.el-select__wrapper) {
  min-height: 34px;
  height: 34px;
  border-radius: 4px;
  box-shadow: 0 0 0 1px #dddddd inset;
  background: #ffffff;
}

.search-filter-section :deep(.el-input__wrapper:hover),
.search-filter-section :deep(.el-input__wrapper.is-focus),
.search-filter-section :deep(.el-select__wrapper:hover),
.search-filter-section :deep(.el-select__wrapper.is-focused) {
  box-shadow: 0 0 0 1px #bdbdbd inset;
}

.search-filter-section :deep(.el-input__inner),
.search-filter-section :deep(.el-select__placeholder),
.search-filter-section :deep(.el-select__selected-item) {
  color: #303030;
  font-size: 13px;
  font-weight: 500;
}

.search-filter-section :deep(.el-input__prefix) {
  color: #a8abb2;
}

.search-input {
  position: relative;
}

.search-input :deep(.el-input__wrapper) {
  padding-left: 36px;
}

.search-filter-section :deep(.search-input .el-input-group__append) {
  position: absolute;
  top: 0;
  left: 0;
  z-index: 2;
  width: 36px;
  height: 34px;
  padding: 0;
  border: none;
  background: transparent;
  box-shadow: none;
}

.search-filter-section :deep(.search-input .el-input-group__append .el-button) {
  width: 36px;
  height: 34px;
  margin: 0;
  padding: 0;
  border: none;
  color: #9aa0a6;
}

.search-filter-section :deep(.el-select__wrapper) {
  padding: 0 0 0 14px;
}

.search-filter-section :deep(.el-select__suffix) {
  align-self: stretch;
  width: 34px;
  margin-left: auto;
  padding: 0;
  border-left: none;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #a8abb2;
}

.search-filter-section :deep(.el-select__suffix .el-icon) {
  margin: 0;
}

.search-filter-section :deep(.el-date-editor.el-input__wrapper) {
  padding: 1px 1px 1px 6px;
  overflow: hidden;
  border-radius: 4px;
}

.search-filter-section :deep(.el-date-editor .el-input__prefix),
.search-filter-section :deep(.el-range-editor .el-range__icon) {
  color: #a8abb2;
}

.search-filter-section :deep(.el-range-editor .el-range__icon) {
  margin: 0 8px 0 2px;
  font-size: 16px;
}

.search-filter-section :deep(.el-range-editor .el-range-input) {
  height: 30px;
  line-height: 30px;
  padding: 0 4px;
  border-radius: 0;
  background: #f3f3f3;
  color: #2f2f2f;
  font-size: 13px;
  font-weight: 600;
}

.search-filter-section :deep(.el-range-editor .el-range-separator) {
  width: 26px;
  padding: 0;
  color: #909399;
  line-height: 32px;
  text-align: center;
}

.search-filter-section :deep(.single-date-filter .el-input__wrapper) {
  position: relative;
  padding: 0;
  overflow: hidden;
}

.search-filter-section :deep(.single-date-filter .el-input__wrapper::after) {
  position: absolute;
  top: 2px;
  right: 2px;
  bottom: 2px;
  z-index: 0;
  width: 94px;
  border-radius: 3px;
  background: #f3f3f3;
  content: '';
  pointer-events: none;
}

.search-filter-section :deep(.single-date-filter .el-input__prefix) {
  position: absolute;
  top: 0;
  left: 0;
  width: 38px;
  height: 100%;
  justify-content: center;
  pointer-events: none;
  z-index: 2;
}

.search-filter-section :deep(.single-date-filter .el-input__suffix) {
  display: none;
}

.search-filter-section :deep(.single-date-filter .el-input__inner) {
  position: relative;
  z-index: 1;
  width: 100%;
  height: 30px;
  margin: 2px 0;
  padding: 0 13px 0 42px;
  border-radius: 0;
  background: transparent;
  color: #2f2f2f;
  font-weight: 600;
  text-align: right;
  box-sizing: border-box;
}

.order-box-notice {
  min-width: 1120px;
  margin-bottom: 16px;
}

.notice-content {
  line-height: 1.8;
}

.notice-content p {
  margin: 4px 0;
}

.order-list {
  min-width: 1120px;
  background: #ffffff;
  padding: 0 0 10px;
  border: 1px solid #e5e5e5;
  border-radius: 0;
  box-shadow: none;
  overflow: hidden;
}

.order-list :deep(.el-table) {
  --el-table-border-color: #e5e5e5;
  --el-table-header-bg-color: #eef4ff;
  --el-table-row-hover-bg-color: #fafafa;
  color: #303030;
  font-size: 12px;
}

.order-list :deep(.el-table__inner-wrapper::before),
.order-list :deep(.el-table__border-left-patch),
.order-list :deep(.el-table__body-wrapper::before) {
  display: none;
}

.order-list :deep(.el-table__header th.el-table__cell) {
  height: 34px;
  padding: 0;
  border-color: #e1e5ef;
  background: #eef4ff !important;
  color: #252525 !important;
  font-size: 12px;
  font-weight: 700;
}

.order-list :deep(.el-table__header th.el-table__cell .cell) {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 8px;
  color: #252525;
  line-height: 1.25;
  white-space: nowrap;
}

.order-list :deep(.el-table__body td.el-table__cell) {
  height: 54px;
  padding: 2px 0;
  border-color: #eeeeee;
  background: #ffffff;
}

.order-list :deep(.el-table__body td.el-table__cell .cell) {
  padding: 0 8px;
  color: #303030;
  line-height: 1.3;
  text-align: center;
}

.order-list :deep(.el-table__body td.el-table__cell:first-child .cell) {
  padding: 4px 8px 4px 12px;
  line-height: 1.3;
}

.order-list :deep(.el-table__body td.channel-column .cell) {
  display: flex;
  align-items: center;
  justify-content: center;
  padding-left: 6px;
  padding-right: 6px;
  text-align: center;
}

.order-list :deep(.el-table__row:hover > td.el-table__cell) {
  background: #fbfbfb;
}

.order-list :deep(.el-table__fixed-right::before),
.order-list :deep(.el-table__fixed::before) {
  display: none;
}

.order-list :deep(.el-tag) {
  min-width: 48px;
  height: 20px;
  padding: 0 8px;
  border: none;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 700;
  line-height: 20px;
}

.order-list :deep(.el-tag--success) {
  background: #d9ffc8;
  color: #57b229;
}

.order-list :deep(.el-tag--warning) {
  background: #fff0c7;
  color: #d99b00;
}

.order-list :deep(.el-tag--danger) {
  background: #ffd9d9;
  color: #f05b61;
}

.order-list :deep(.el-tag--primary),
.order-list :deep(.el-tag--info) {
  background: #135fad;
  color: #ffffff;
}

.order-list :deep(.channel-tag) {
  min-width: 60px;
  background: var(--channel-tag-bg);
  color: var(--channel-tag-color);
}

.order-list :deep(.stay-status-tag) {
  min-width: 58px;
  height: 22px;
  padding: 0 10px;
  border-radius: 7px;
  font-size: 12px;
  font-weight: 500;
  line-height: 22px;
}

.order-list :deep(.stay-status-tag--not-checked-in) {
  background: #72bdf4;
  color: #1383d3;
}

.order-list :deep(.stay-status-tag--checked-in) {
  background: #ffd2d8;
  color: #f05f75;
}

.order-list :deep(.stay-status-tag--checked-out) {
  background: #dcdcdc;
  color: #9d9d9d;
}

.order-list :deep(.el-button.is-link) {
  font-size: 12px;
  font-weight: 600;
}

.order-summary {
  display: flex;
  flex-direction: column;
  gap: 3px;
  align-items: flex-start;
  width: 100%;
}

.order-link {
  color: #2f7cf6;
  padding: 0;
  font-size: 12px;
  font-weight: 600;
  text-align: left;
  white-space: normal;
  word-break: break-all;
  line-height: 1.3;
}

.order-link :deep(.el-button__text) {
  white-space: normal;
  word-break: break-all;
  text-align: left;
  display: block;
  width: 100%;
}

.order-link:hover :deep(.el-button__text) {
  color: #2f7cf6;
  text-decoration: underline;
}

.order-summary-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 4px 8px;
  max-width: 100%;
  font-size: 11px;
  line-height: 1.3;
  color: #777777;
}

.order-summary-meta-item {
  max-width: 100%;
  overflow-wrap: anywhere;
}

.channel-order-link {
  padding: 0;
  font-size: 11px;
  color: #777777;
  text-align: left;
  white-space: normal;
  word-break: break-all;
  line-height: 1.3;
}

.channel-order-link :deep(.el-button__text) {
  white-space: normal;
  word-break: break-all;
  text-align: left;
  display: block;
  width: 100%;
}

.channel-order-link:hover :deep(.el-button__text) {
  color: #2f7cf6;
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
  margin-top: 0;
  font-size: 11px;
  line-height: 1.3;
  color: #777777;
}

.room-mapping-cell {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 6px;
  line-height: 1.4;
}

.room-mapping-line {
  display: flex;
  gap: 6px;
  max-width: 100%;
  font-size: 12px;
}

.room-mapping-label {
  color: #909399;
  white-space: nowrap;
}

.room-mapping-value {
  color: #303133;
  overflow-wrap: anywhere;
}

.pagination-wrapper {
  display: flex;
  justify-content: space-between;
  align-items: center;
  min-height: 52px;
  margin-top: 0;
  padding: 0 14px;
  border-top: 1px solid #eeeeee;
  background: #ffffff;
}

.pagination-info {
  font-size: 12px;
  font-weight: 600;
  color: #252525;
}

.pagination-wrapper :deep(.el-pagination) {
  --el-pagination-button-width: 28px;
  --el-pagination-button-height: 28px;
  gap: 10px;
  font-size: 12px;
  font-weight: 600;
}

.pagination-wrapper :deep(.el-select .el-select__wrapper),
.pagination-wrapper :deep(.el-pagination__jump .el-input__wrapper) {
  min-height: 28px;
  height: 28px;
  border-radius: 4px;
  box-shadow: 0 0 0 1px #dddddd inset;
}

.pagination-wrapper :deep(.el-pagination button),
.pagination-wrapper :deep(.el-pager li) {
  min-width: 28px;
  height: 28px;
  color: #252525;
  background: transparent;
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

@media (max-width: 1280px) {
  .order-panel-header {
    padding-left: 24px;
    padding-right: 20px;
  }

  .main-content {
    padding-right: 20px;
    padding-bottom: 20px;
  }
}

@media (max-width: 768px) {
  .order-management {
    grid-template-columns: 84px minmax(0, 1fr);
  }

  .order-sidebar {
    width: 84px;
  }

  .order-sidebar .sidebar-toggle,
  .order-sidebar .sidebar-parent {
    justify-content: center;
    padding: 0;
  }

  .main-content {
    padding-left: 12px;
    padding-right: 12px;
  }

  .order-tabs,
  .search-filter-section,
  .order-list,
  .order-box-notice {
    min-width: 980px;
  }
}
</style>
