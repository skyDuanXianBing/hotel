<template>
  <div class="order-management">
    <!-- 左侧导航菜单 -->
    <div class="sidebar" :class="{ collapsed: isCollapsed }">
      <!-- 收起导航按钮 -->
      <div class="sidebar-header" @click="toggleSidebar">
        <el-icon class="sidebar-icon"><MenuIcon /></el-icon>
        <span v-if="!isCollapsed" class="sidebar-title">收起导航</span>
        <el-icon v-if="!isCollapsed" class="collapse-icon"><ArrowLeft /></el-icon>
        <el-icon v-else class="collapse-icon"><ArrowRight /></el-icon>
      </div>

      <el-menu class="sidebar-menu" :default-active="activeMenu" @select="handleMenuSelect" :collapse="isCollapsed">
        <el-menu-item index="order-management">
          <el-icon><Document /></el-icon>
          <span>住宿订单</span>
        </el-menu-item>
      </el-menu>
    </div>

    <!-- 主内容区域 -->
    <div class="main-content">
      <!-- 订单状态标签页 -->
      <div class="order-tabs">
        <el-tabs v-model="activeOrderTab" @tab-change="handleOrderTabChange">
          <el-tab-pane label="全部" name="all">
            <template #label>
              <span>全部</span>
            </template>
          </el-tab-pane>
          <el-tab-pane name="today-checkin">
            <template #label>
              <div class="tab-label-with-badge">
                <span>今日预抵</span>
                <span v-if="todayCheckinCount > 0" class="custom-badge">{{
                  todayCheckinCount
                }}</span>
              </div>
            </template>
          </el-tab-pane>
          <el-tab-pane name="today-checkout">
            <template #label>
              <div class="tab-label-with-badge">
                <span>今日预离</span>
                <span v-if="todayCheckoutCount > 0" class="custom-badge">{{
                  todayCheckoutCount
                }}</span>
              </div>
            </template>
          </el-tab-pane>
          <el-tab-pane name="today-new">
            <template #label>
              <div class="tab-label-with-badge">
                <span>今日新单</span>
                <span v-if="todayNewCount > 0" class="custom-badge">{{ todayNewCount }}</span>
              </div>
            </template>
          </el-tab-pane>
           <el-tab-pane name="unassigned">
             <template #label>
               <div class="tab-label-with-badge">
                 <span>未排房/未映射</span>
                 <span v-if="unassignedCount > 0" class="custom-badge">{{ unassignedCount }}</span>
               </div>
             </template>
           </el-tab-pane>
          <el-tab-pane label="已排房" name="assigned"></el-tab-pane>
          <el-tab-pane name="pending">
            <template #label>
              <div class="tab-label-with-badge">
                <span>待处理</span>
                <span v-if="pendingCount > 0" class="custom-badge">{{ pendingCount }}</span>
              </div>
            </template>
          </el-tab-pane>
          <el-tab-pane label="订单盒子" name="order-box"></el-tab-pane>
          <el-tab-pane label="房型/房间已删除" name="deleted-rooms"></el-tab-pane>
        </el-tabs>
      </div>

      <!-- 搜索和筛选区域 -->
      <div class="search-filter-section">
        <!-- 第一行：搜索框和主要筛选器 -->
        <div class="main-filter-row">
          <!-- 搜索框 -->
          <el-input
            v-model="searchQuery"
            placeholder="请输入订单号、渠道订单号、房间号、姓名、手机号查询"
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
            <span class="filter-label">渠道</span>
            <el-select v-model="filters.channel" placeholder="全部" @change="handleFilterChange">
              <el-option label="全部" value=""></el-option>
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
            <span class="filter-label">房型</span>
            <el-select v-model="filters.roomType" placeholder="全部" @change="handleFilterChange">
              <el-option label="全部" value=""></el-option>
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
            <span class="filter-label">创建时间</span>
            <el-date-picker
              v-model="dateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              format="YYYY-MM-DD"
              value-format="YYYY-MM-DD"
              @change="handleDateRangeChange"
            />
          </div>
        </div>

        <!-- 第二行：详细筛选器 -->
        <div class="filter-row">
          <div class="filter-group">
            <span class="filter-label">入住类型</span>
            <el-select
              v-model="filters.checkinType"
              placeholder="全部"
              @change="handleFilterChange"
            >
              <el-option label="全部" value=""></el-option>
              <el-option label="正常入住" value="normal"></el-option>
              <el-option label="提前入住" value="early"></el-option>
              <el-option label="延迟入住" value="late"></el-option>
            </el-select>
          </div>

          <div class="filter-group">
            <span class="filter-label">入住状态</span>
            <el-select v-model="filters.status" placeholder="请选择" @change="handleFilterChange">
              <el-option label="全部" value=""></el-option>
              <el-option label="已入住" value="checked-in"></el-option>
              <el-option label="未入住" value="not-checked-in"></el-option>
              <el-option label="已退房" value="checked-out"></el-option>
            </el-select>
          </div>

          <div class="filter-group">
            <span class="filter-label">结账状态</span>
            <el-select
              v-model="filters.paymentStatus"
              placeholder="全部"
              @change="handleFilterChange"
            >
              <el-option label="全部" value=""></el-option>
              <el-option label="已结账" value="paid"></el-option>
              <el-option label="未结账" value="unpaid"></el-option>
            </el-select>
          </div>

          <div class="filter-actions">
            <el-button @click="toggleFilters">
              {{ showAdvancedFilters ? '收起筛选' : '展开筛选' }}
              <el-icon>
              <component :is="showAdvancedFilters ? ArrowUp : ArrowDown" />
            </el-icon>
          </el-button>
        </div>
      </div>
      </div>

      <!-- 订单盒子提示信息 -->
      <div v-if="activeOrderTab === 'order-box'" class="order-box-notice">
        <el-alert
          title="订单盒子说明"
          type="info"
          :closable="false"
          show-icon
        >
          <template #default>
            <div class="notice-content">
              <p>1. 只有已预订房间可移入订单盒子</p>
              <p>2. 订单盒子中的房间不实际排房，不占库存，营业数据不进行统计</p>
              <p>3. 订单金额不计算移入订单盒子的房间，如房间已结账或挂账，移入后将被撤销</p>
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
          element-loading-text="加载中..."
        >
          <el-table-column
            prop="orderNumber"
            label="订单号/渠道订单号"
            min-width="280"
            fixed="left"
          >
            <template #default="scope">
              <div class="order-number">
                <el-button type="text" class="order-link" title="点击查看订单详情" @click="viewOrder(scope.row)">{{
                  scope.row.orderNumber
                }}</el-button>
                <el-button
                  type="text"
                  class="channel-order-link"
                  title="点击查看订单详情"
                  @click="viewOrder(scope.row)"
                >
                  {{ getDisplayChannelOrderNumber(scope.row) }}
                </el-button>
                <span class="order-detail-tip">点击订单号查看详情</span>
              </div>
            </template>
          </el-table-column>

          <el-table-column prop="channelName" label="渠道" width="150">
            <template #default="scope">
              <el-tag size="small" :type="getChannelTagType(scope.row.channelName)">
                {{ scope.row.channelName }}
              </el-tag>
            </template>
          </el-table-column>

          <el-table-column v-if="activeOrderTab !== 'all'" prop="status" label="状态" width="100">
            <template #default="scope">
              <el-tag size="small" :type="getReservationStatusTagType(scope.row.status)">
                {{ getReservationStatusText(scope.row.status) }}
              </el-tag>
            </template>
          </el-table-column>

          <el-table-column prop="guestName" label="联系人" width="150"></el-table-column>

          <el-table-column prop="phone" label="手机号" width="130">
            <template #default="scope">
              <span>{{ scope.row.phone || '-' }}</span>
            </template>
          </el-table-column>

          <el-table-column label="入住类型" width="100">
            <template #default="scope">
              <span>{{ getCheckinTypeText(scope.row) }}</span>
            </template>
          </el-table-column>

          <el-table-column prop="roomTypeName" label="房型" width="180"></el-table-column>

          <el-table-column v-if="activeOrderTab === 'all'" prop="pricePlan" label="价格计划" width="140">
            <template #default="scope">
              <span>{{ scope.row.pricePlan || '-' }}</span>
            </template>
          </el-table-column>

          <el-table-column v-if="activeOrderTab === 'unassigned'" prop="otaRoomId" label="渠道房型ID" width="160">
            <template #default="scope">
              <span>{{ scope.row.otaRoomId || '-' }}</span>
            </template>
          </el-table-column>

          <el-table-column v-if="activeOrderTab === 'unassigned'" prop="otaRoomTypeId" label="PMS房型ID" width="110">
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

          <el-table-column prop="roomNumber" label="房间号" width="100"></el-table-column>

          <el-table-column v-if="activeOrderTab === 'all'" label="房间数" width="90">
            <template #default>
              <span>1</span>
            </template>
          </el-table-column>

          <el-table-column v-if="activeOrderTab === 'unassigned' || activeOrderTab === 'assigned' || activeOrderTab === 'order-box'" label="排房状态" width="100">
            <template #default="scope">
              <el-tag size="small" :type="getAssignStatusTagType(scope.row)">
                {{ getAssignStatusText(scope.row) }}
              </el-tag>
            </template>
          </el-table-column>

          <el-table-column v-if="activeOrderTab === 'all'" prop="checkInDate" label="入住时间" width="170">
            <template #default="scope">
              <span>{{ formatStayDateTime(scope.row.checkInDate, 'checkIn') }}</span>
            </template>
          </el-table-column>

          <el-table-column v-if="activeOrderTab === 'all'" prop="checkOutDate" label="离店时间" width="170">
            <template #default="scope">
              <span>{{ formatStayDateTime(scope.row.checkOutDate, 'checkOut') }}</span>
            </template>
          </el-table-column>

          <el-table-column v-if="activeOrderTab === 'all'" label="间夜数" width="90">
            <template #default="scope">
              <span>{{ getNights(scope.row) }}</span>
            </template>
          </el-table-column>

          <el-table-column v-if="activeOrderTab === 'all'" label="入住人数" width="100">
            <template #default="scope">
              <span>{{ getTotalGuests(scope.row) }}</span>
            </template>
          </el-table-column>

          <el-table-column v-if="activeOrderTab === 'all'" label="成人数" width="100">
            <template #default="scope">
              <span>{{ scope.row.adults ?? '-' }}</span>
            </template>
          </el-table-column>

          <el-table-column v-if="activeOrderTab === 'all'" label="儿童数" width="100">
            <template #default="scope">
              <span>{{ scope.row.children ?? 0 }}</span>
            </template>
          </el-table-column>

          <el-table-column v-if="activeOrderTab === 'all'" label="入住状态" width="120">
            <template #default="scope">
              <span>{{ getReservationStatusText(scope.row.status) }}</span>
            </template>
          </el-table-column>

          <el-table-column prop="currentRoomPrice" label="房费" width="120">
            <template #default="scope">
              <span>{{ formatAmount(scope.row.currentRoomPrice) }}</span>
            </template>
          </el-table-column>

          <el-table-column v-if="activeOrderTab === 'all'" prop="totalAmount" label="住宿金额" width="140">
            <template #default="scope">
              <span>{{ formatAmount(scope.row.totalAmount) }}</span>
            </template>
          </el-table-column>

          <el-table-column v-if="activeOrderTab === 'all'" prop="totalAmount" label="住宿小计" width="140">
            <template #default="scope">
              <span>{{ formatAmount(scope.row.totalAmount) }}</span>
            </template>
          </el-table-column>

          <el-table-column v-if="activeOrderTab === 'all'" prop="commission" label="佣金" width="120">
            <template #default="scope">
              <span>{{ formatAmount(scope.row.commission) }}</span>
            </template>
          </el-table-column>

          <el-table-column v-if="activeOrderTab === 'all'" prop="paymentMethod" label="支付方式" width="150">
            <template #default="scope">
              <span>{{ scope.row.paymentMethod || '-' }}</span>
            </template>
          </el-table-column>

          <el-table-column v-if="activeOrderTab === 'all'" label="收银状态" width="120">
            <template #default="scope">
              <span>{{ getCashierStatusTextV2(scope.row) }}</span>
            </template>
          </el-table-column>

          <el-table-column label="结账状态" width="100">
            <template #default="scope">
              <el-select
                v-if="activeOrderTab === 'pending'"
                :model-value="getSettlementSelectValue(scope.row)"
                size="small"
                style="width: 96px"
                :disabled="settlementUpdatingOrderId === scope.row.id"
                @change="handleSettlementStatusChange(scope.row, $event)"
              >
                <el-option label="已结账" value="paid" />
                <el-option label="未结账" value="unpaid" />
              </el-select>
              <el-tag v-else size="small" :type="getSettlementTagTypeV2(scope.row)">
                {{ getSettlementStatusTextV2(scope.row) }}
              </el-tag>
            </template>
          </el-table-column>

          <el-table-column v-if="activeOrderTab !== 'all'" prop="checkInDate" label="入住时间" width="150"></el-table-column>

          <el-table-column v-if="activeOrderTab === 'all'" prop="notes" label="备注" width="200">
            <template #default="scope">
              <span>{{ scope.row.notes || '-' }}</span>
            </template>
          </el-table-column>

          <el-table-column v-if="activeOrderTab === 'all'" prop="createdBy" label="创建人" width="120">
            <template #default="scope">
              <span>{{ scope.row.createdBy || '-' }}</span>
            </template>
          </el-table-column>

          <el-table-column prop="createdAt" label="创建时间" width="180">
            <template #default="scope">
              <span>{{ formatDateTime(scope.row.createdAt) }}</span>
            </template>
          </el-table-column>

          <el-table-column
            v-if="activeOrderTab === 'unassigned' || activeOrderTab === 'assigned' || activeOrderTab === 'order-box'"
            label="操作"
            width="180"
            fixed="right"
          >
            <template #default="scope">
              <el-button
                v-if="scope.row.roomId"
                type="success"
                link
                @click="viewOrder(scope.row)"
              >
                已排房
              </el-button>
              <el-button
                type="primary"
                link
                :disabled="!canAssignRoom(scope.row)"
                @click="openAssignRoom(scope.row)"
              >
                {{ scope.row.roomId ? '编辑排房' : '进行排房' }}
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <!-- 分页 -->
        <div class="pagination-wrapper">
          <div class="pagination-info">
            <span>总计 {{ totalOrders }} 个订单</span>
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
      title="进行排房"
      width="860px"
      destroy-on-close
      @closed="handleAssignDialogClosed"
    >
      <div class="assign-room-panel">
        <h4 class="panel-title">排房（按订单日期范围过滤可用房间）</h4>

        <div class="assign-row">
          <div class="assign-label">房型</div>
          <el-select
            v-model="assignRoomTypeId"
            placeholder="请选择房型"
            filterable
            :loading="assignableRoomsLoading"
            style="width: 100%"
            @change="handleAssignRoomTypeChange"
          >
            <el-option
              v-for="roomType in assignableRoomTypes"
              :key="roomType.id"
              :label="`${roomType.name}（可用 ${roomType.availableRooms}）`"
              :value="roomType.id"
            />
          </el-select>
        </div>

        <div class="assign-row">
          <div class="assign-label">房间号</div>
          <el-select
            v-model="assignRoomId"
            placeholder="请选择房间"
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
            刷新可用房间
          </el-button>
          <el-button
            type="primary"
            :disabled="!assignRoomId"
            :loading="assignRoomSubmitting"
            @click="submitAssignRoom"
          >
            确认排房
          </el-button>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
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
  getReservationsByType,
  updateReservationSettlementStatus,
  type AssignableRoomDTO,
  type AssignableRoomTypeDTO,
  type ReservationDTO,
  type ReservationStatistics,
} from '@/api/reservation'
import { getAllRoomTypes } from '@/api/roomType'
import ReservationDetailDrawer from '@/components/reservation/ReservationDetailDrawer.vue'
import {
  getOrderBoxList,
  type OrderBoxItem,
} from '@/api/orderBox'

const route = useRoute()

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

// 映射订单页面标签页到统计类型
const mapTabToType = (tab: string) => {
  const tabMap: Record<string, string> = {
    'today-checkin': 'today-arrivals',
    'today-checkout': 'today-departures',
    'today-new': 'today-new',
    unassigned: 'unassigned',
    assigned: 'assigned',
    pending: 'pending',
  }
  return tabMap[tab]
}
const searchQuery = ref('')
const dateRange = ref<[string, string] | null>(null)
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
    // 检查当前标签页是否对应特定统计类型
    const statisticsType = mapTabToType(activeOrderTab.value)

    console.log('loadReservations - 当前标签页:', activeOrderTab.value)
    console.log('loadReservations - 映射统计类型:', statisticsType)

    if (statisticsType) {
      // 使用按类型过滤的 API
      console.log('loadReservations - 使用类型过滤 API:', statisticsType)
      const response = await getReservationsByType(statisticsType)
      console.log('loadReservations - API 响应:', response)
      if (response.success) {
        orders.value = response.data
        totalOrders.value = response.data.length
        console.log('loadReservations - 过滤后订单数量:', response.data.length)
      } else {
        ElMessage.error(response.message)
      }
    } else {
      // 使用通用分页 API
      console.log('loadReservations - 使用通用分页 API')
      const filterParams = {
        page: currentPage.value - 1, // 后端从0开始计数
        size: pageSize.value,
        searchKeyword: searchQuery.value || undefined,
        channel: filters.value.channel || undefined,
        roomType: filters.value.roomType || undefined,
        checkinType: filters.value.checkinType || undefined,
        status: filters.value.status || undefined,
        paymentStatus: filters.value.paymentStatus || undefined,
        startDate: dateRange.value?.[0] || undefined,
        endDate: dateRange.value?.[1] || undefined,
        orderType: activeOrderTab.value !== 'all' ? activeOrderTab.value : undefined,
      }

      const response = await getReservationsWithFilters(filterParams)
      if (response.success) {
        orders.value = response.data.content
        totalOrders.value = response.data.totalElements
      } else {
        ElMessage.error(response.message)
      }
    }
  } catch (error) {
    console.error('加载订单数据失败:', error)
    ElMessage.error('加载订单数据失败')
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
    ElMessage.error('加载统计数据失败')
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
    ElMessage.error('加载订单盒子失败')
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
          label: channel.name,
          value: channel.name,
        }))
    } else {
      ElMessage.warning(channelResponse.message || '加载渠道筛选项失败')
    }

    if (roomTypeResponse.success) {
      roomTypeOptions.value = roomTypeResponse.data
        .filter((roomType) => roomType.name && roomType.name.trim().length > 0)
        .map((roomType) => ({
          label: roomType.name,
          value: roomType.name,
        }))
    } else {
      ElMessage.warning(roomTypeResponse.message || '加载房型筛选项失败')
    }
  } catch (error) {
    console.error('加载筛选项失败:', error)
    ElMessage.warning('加载筛选项失败')
  }
}

// 工具函数：判断是否为今天
const isToday = (dateStr: string) => {
  const date = new Date(dateStr)
  const today = new Date()
  return date.toDateString() === today.toDateString()
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

const toggleFilters = () => {
  showAdvancedFilters.value = !showAdvancedFilters.value
}

const getChannelTagType = (channel: string) => {
  switch (channel) {
    case '直营客':
      return 'primary'
    case '美团民宿':
      return 'warning'
    case '途家':
      return 'success'
    default:
      return 'info'
  }
}

const getReservationStatusText = (status?: string) => {
  const normalized = (status || '').toUpperCase()
  switch (normalized) {
    case 'CONFIRMED':
      return '已确认'
    case 'REQUESTED':
      return '待确认'
    case 'CHECKED_IN':
      return '已入住'
    case 'CHECKED_OUT':
      return '已退房'
    case 'CANCELLED':
      return '已取消'
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
    return '未排房'
  }
  return canAssignRoom(order) ? '已排房' : '已排房(不占房)'
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

const getCheckinTypeText = (_order: ReservationDTO) => '正常入住'

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
  const checkInDate = new Date(order.checkInDate)
  const checkOutDate = new Date(order.checkOutDate)
  const diffMs = checkOutDate.getTime() - checkInDate.getTime()
  if (!Number.isFinite(diffMs) || diffMs <= 0) {
    return '-'
  }
  return Math.floor(diffMs / (1000 * 60 * 60 * 24))
}

const formatStayDateTime = (dateStr?: string, type: 'checkIn' | 'checkOut' = 'checkIn') => {
  if (!dateStr) return '-'
  const defaultTime = type === 'checkIn' ? '16:00:00' : '10:00:00'
  const normalizedDate = dateStr.includes('T') ? dateStr.split('T')[0] : dateStr
  return `${normalizedDate} ${defaultTime}`
}

const getSettlementStatusText = (order: ReservationDTO) => {
  const paidAmount = Number(order.paidAmount ?? 0)
  const totalAmount = Number(order.totalAmount ?? 0)
  if (totalAmount > 0 && paidAmount >= totalAmount) {
    return '已结账'
  }
  if (paidAmount > 0 && paidAmount < totalAmount) {
    return '部分结账'
  }
  return '未结账'
}

const getSettlementTagType = (order: ReservationDTO) => {
  const settlementStatus = getSettlementStatusText(order)
  if (settlementStatus === '已结账') {
    return 'success'
  }
  if (settlementStatus === '部分结账') {
    return 'warning'
  }
  return 'danger'
}

const getCashierStatusText = (order: ReservationDTO) => {
  const paidAmount = Number(order.paidAmount ?? 0)
  const totalAmount = Number(order.totalAmount ?? 0)
  if (totalAmount > 0 && paidAmount >= totalAmount) {
    return '账目已平'
  }
  if (paidAmount > 0) {
    return '部分到账'
  }
  return '待收款'
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
    return '已结账'
  }
  const paidAmount = Number(order.paidAmount ?? 0)
  return paidAmount > 0 ? '部分结账' : '未结账'
}

const getSettlementTagTypeV2 = (order: ReservationDTO) => {
  const settlementStatus = getSettlementStatusTextV2(order)
  if (settlementStatus === '已结账') {
    return 'success'
  }
  if (settlementStatus === '部分结账') {
    return 'warning'
  }
  return 'danger'
}

const getCashierStatusTextV2 = (order: ReservationDTO) => {
  if (isOrderSettled(order)) {
    return '账目已平'
  }
  const paidAmount = Number(order.paidAmount ?? 0)
  return paidAmount > 0 ? '部分到账' : '待收款'
}

const getSettlementSelectValue = (order: ReservationDTO) => {
  return isOrderSettled(order) ? 'paid' : 'unpaid'
}

const formatDateTime = (dateStr: string) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}:${String(date.getSeconds()).padStart(2, '0')}`
}

const getChannelDisplayName = (value: string) => {
  const channelMap: Record<string, string> = {
    direct: '直营客',
    meituan: '美团民宿',
    tujia: '途家',
  }
  return channelMap[value] || value
}

const getCheckinTypeDisplayName = (value: string) => {
  const typeMap: Record<string, string> = {
    normal: '正常入住',
    early: '提前入住',
    late: '延迟入住',
  }
  return typeMap[value] || value
}

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
      ElMessage.error(response.message || '更新结账状态失败')
      return
    }
    Object.assign(order, response.data)
    ElMessage.success(value === 'paid' ? '已更新为已结账' : '已更新为未结账')
    if (filters.value.paymentStatus) {
      await loadReservations()
    }
  } catch (error) {
    console.error('更新结账状态失败:', error)
    ElMessage.error('更新结账状态失败')
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
      ElMessage.error(res.message || '加载可用房型失败')
      return
    }
    assignableRoomTypes.value = res.data.roomTypes || []
    assignRoomTypeId.value = null
    assignRoomId.value = null
    assignableRooms.value = []
  } catch (error) {
    console.error('加载可用房型失败:', error)
    ElMessage.error('加载可用房型失败')
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
      ElMessage.error(res.message || '加载可用房间失败')
      return
    }
    assignableRooms.value = res.data.rooms || []
  } catch (error) {
    console.error('加载可用房间失败:', error)
    ElMessage.error('加载可用房间失败')
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
      ElMessage.error(res.message || '排房失败')
      return
    }
    ElMessage.success('排房成功，房态日历刷新后可查看占房')
    showAssignRoomDialog.value = false
    await handleReservationUpdated()
  } catch (error) {
    console.error('排房失败:', error)
    ElMessage.error('排房失败')
  } finally {
    assignRoomSubmitting.value = false
  }
}

const openAssignRoom = async (order: ReservationDTO) => {
  if (!canAssignRoom(order)) {
    ElMessage.warning('当前订单状态不支持排房（仅已确认/待确认/已入住可排房）')
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
