<template>
  <div class="order-management">
    <!-- 左侧导航菜单 -->
    <div class="sidebar">
      <div class="sidebar-header">
        <el-button type="text" @click="$router.go(-1)">
          <el-icon><ArrowLeft /></el-icon>
        </el-button>
        <span class="sidebar-title">收起导航</span>
      </div>

      <el-menu class="sidebar-menu" :default-active="activeMenu" @select="handleMenuSelect">
        <el-menu-item index="order-management">
          <el-icon><Document /></el-icon>
          <span>订单管理</span>
        </el-menu-item>
        <el-menu-item index="accommodation-orders">
          <span>住宿订单</span>
        </el-menu-item>
        <el-menu-item index="order-collection">
          <el-icon><Collection /></el-icon>
          <span>订单牌收款</span>
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
                <span>今日新办</span>
                <span v-if="todayNewCount > 0" class="custom-badge">{{ todayNewCount }}</span>
              </div>
            </template>
          </el-tab-pane>
          <el-tab-pane name="unassigned">
            <template #label>
              <div class="tab-label-with-badge">
                <span>未排房</span>
                <span v-if="unassignedCount > 0" class="custom-badge">{{ unassignedCount }}</span>
              </div>
            </template>
          </el-tab-pane>
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
              <el-option label="直营客" value="direct"></el-option>
              <el-option label="美团民宿" value="meituan"></el-option>
              <el-option label="途家" value="tujia"></el-option>
            </el-select>
          </div>

          <!-- 房型筛选 -->
          <div class="filter-group">
            <span class="filter-label">房型</span>
            <el-select v-model="filters.roomType" placeholder="全部" @change="handleFilterChange">
              <el-option label="全部" value=""></el-option>
              <el-option label="aa" value="aa"></el-option>
              <el-option label="大床房" value="big-bed"></el-option>
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

          <div class="filter-group">
            <span class="filter-label">是否包栋</span>
            <el-select v-model="filters.isPackage" placeholder="全部" @change="handleFilterChange">
              <el-option label="全部" value=""></el-option>
              <el-option label="是" value="yes"></el-option>
              <el-option label="否" value="no"></el-option>
            </el-select>
          </div>

          <div class="filter-actions">
            <el-button @click="toggleFilters">
              {{ showAdvancedFilters ? '收起筛选' : '展开筛选' }}
              <el-icon>
                <component :is="showAdvancedFilters ? 'ArrowUp' : 'ArrowDown'" />
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
              <p>2. 订单盒子中的房间不实际排房,不占库存,营业数据不进行统计</p>
              <p>3. 订单金额不计算移入订单盒子的房间,如果房间被结账/挂账,移入后将被撤销</p>
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
          <el-table-column prop="orderNumber" label="订单号/渠道订单号" width="180">
            <template #default="scope">
              <div class="order-number">
                <el-button type="text" class="order-link">{{ scope.row.orderNumber }}</el-button>
                <div class="channel-order" v-if="scope.row.channelOrderNumber">
                  {{ scope.row.channelOrderNumber }}
                </div>
              </div>
            </template>
          </el-table-column>

          <el-table-column prop="channelName" label="渠道" width="100">
            <template #default="scope">
              <el-tag size="small" :type="getChannelTagType(scope.row.channelName)">
                {{ scope.row.channelName }}
              </el-tag>
            </template>
          </el-table-column>

          <el-table-column prop="guestName" label="联系人" width="100"></el-table-column>

          <el-table-column prop="phone" label="手机号" width="130">
            <template #default="scope">
              <span>{{ scope.row.phone || '-' }}</span>
            </template>
          </el-table-column>

          <el-table-column label="入住类型" width="100">
            <template #default="scope">
              <span>正常入住</span>
            </template>
          </el-table-column>

          <el-table-column prop="roomTypeName" label="房型" width="100"></el-table-column>

          <el-table-column prop="roomNumber" label="房间" width="80"></el-table-column>

          <el-table-column prop="currentRoomPrice" label="房费" width="100">
            <template #default="scope">
              <span>¥ {{ scope.row.currentRoomPrice || 0 }}</span>
            </template>
          </el-table-column>

          <el-table-column prop="totalAmount" label="订单总金额" width="120">
            <template #default="scope">
              <span>¥ {{ scope.row.totalAmount || 0 }}</span>
            </template>
          </el-table-column>

          <el-table-column label="结账状态" width="100">
            <template #default="scope">
              <el-tag size="small" type="danger"> 未结账 </el-tag>
            </template>
          </el-table-column>

          <el-table-column prop="checkInDate" label="入住时间" width="150"></el-table-column>

          <el-table-column prop="createdAt" label="创建时间" width="180">
            <template #default="scope">
              <span>{{ formatDateTime(scope.row.createdAt) }}</span>
            </template>
          </el-table-column>

          <el-table-column label="操作" width="200" fixed="right">
            <template #default="scope">
              <el-button type="text" size="small" @click="viewOrder(scope.row)">查看</el-button>
              <el-button type="text" size="small" @click="editOrder(scope.row)">编辑</el-button>
              <el-dropdown @command="(command: string) => handleOrderAction(command, scope.row)">
                <el-button type="text" size="small">
                  更多<el-icon><ArrowDown /></el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item v-if="activeOrderTab === 'order-box'" command="moveOut">
                      移出订单盒子
                    </el-dropdown-item>
                    <el-dropdown-item v-else command="checkIn">办理入住</el-dropdown-item>
                    <el-dropdown-item command="checkOut">办理退房</el-dropdown-item>
                    <el-dropdown-item command="cancel">取消订单</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </template>
          </el-table-column>
        </el-table>

        <!-- 分页 -->
        <div class="pagination-wrapper">
          <div class="pagination-info">
            <span>总计{{ totalOrders }}个订单</span>
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
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import {
  ArrowLeft,
  Document,
  Collection,
  Search,
  ArrowDown,
  ArrowUp,
} from '@element-plus/icons-vue'
import { ElMessage, ElLoading } from 'element-plus'
import {
  getReservationsWithFilters,
  getReservationStatistics,
  getReservationsByType,
  checkInReservation,
  checkOutReservation,
  cancelReservation,
  type ReservationDTO,
  type PagedReservationResponse,
  type ReservationStatistics,
} from '@/api/reservation'
import {
  getOrderBoxList,
  moveOutOrderBox,
  type OrderBoxItem,
} from '@/api/orderBox'

const router = useRouter()
const route = useRoute()

// 响应式数据
const activeMenu = ref('order-management')
const activeOrderTab = ref('all')

// 映射首页统计类型到订单页面标签页
const mapTypeToTab = (type: string) => {
  const typeMap: Record<string, string> = {
    'today-arrivals': 'today-checkin',
    'today-departures': 'today-checkout',
    'today-new': 'today-new',
    unassigned: 'unassigned',
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
    pending: 'pending',
  }
  return tabMap[tab]
}
const searchQuery = ref('')
const dateRange = ref<[string, string] | null>(null)
const showAdvancedFilters = ref(true)
const loading = ref(false)

// 筛选条件
const filters = ref({
  channel: '',
  roomType: '',
  checkinType: '',
  status: '',
  paymentStatus: '',
  isPackage: '',
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

// 各类型订单数量计算 - 使用统计数据
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

    // 检查当前标签页是否对应特定的统计类型
    const statisticsType = mapTabToType(activeOrderTab.value)

    console.log('loadReservations - 当前标签页:', activeOrderTab.value)
    console.log('loadReservations - 映射的统计类型:', statisticsType)

    if (statisticsType) {
      // 使用按类型过滤的API
      console.log('loadReservations - 使用类型过滤API:', statisticsType)
      const response = await getReservationsByType(statisticsType)
      console.log('loadReservations - API响应:', response)
      if (response.success) {
        orders.value = response.data
        totalOrders.value = response.data.length
        console.log('loadReservations - 过滤后的订单数量:', response.data.length)
      } else {
        ElMessage.error(response.message)
      }
    } else {
      // 使用通用的分页API
      console.log('loadReservations - 使用通用分页API')
      const filterParams = {
        page: currentPage.value - 1, // 后端从0开始计数
        size: pageSize.value,
        searchKeyword: searchQuery.value || undefined,
        channel: filters.value.channel || undefined,
        roomType: filters.value.roomType || undefined,
        checkinType: filters.value.checkinType || undefined,
        status: filters.value.status || undefined,
        paymentStatus: filters.value.paymentStatus || undefined,
        isPackage: filters.value.isPackage || undefined,
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

// 工具函数：判断是否为今日
const isToday = (dateStr: string) => {
  const date = new Date(dateStr)
  const today = new Date()
  return date.toDateString() === today.toDateString()
}

// 因为筛选现在由后端处理，所以直接返回orders
const filteredOrders = computed(() => {
  if (activeOrderTab.value === 'order-box') {
    // 订单盒子显示订单盒子中的订单
    return orderBoxItems.value.map((item) => item.reservation)
  }
  return orders.value
})

// 方法
const handleMenuSelect = (index: string) => {
  activeMenu.value = index
}

const handleOrderTabChange = (tabName: string) => {
  console.log('切换标签页:', tabName)
  activeOrderTab.value = tabName
  console.log('设置activeOrderTab为:', activeOrderTab.value)
  // 重置分页到第一页
  currentPage.value = 1
  // 重新加载数据
  loadReservations()
}

const handleSearch = () => {
  // 重置分页到第一页
  currentPage.value = 1
  // 重新加载数据
  loadReservations()
}

const handleDateChange = (dates: [string, string] | null) => {
  if (dates) {
    dateRange.value = dates
  }
  // 重置分页到第一页
  currentPage.value = 1
  // 重新加载数据
  loadReservations()
}

const handleFilterChange = () => {
  // 重置分页到第一页
  currentPage.value = 1
  // 重新加载数据
  loadReservations()
}

const handleDateRangeChange = (value: [string, string] | null) => {
  // 处理日期范围变化
  // 重置分页到第一页
  currentPage.value = 1
  // 重新加载数据
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

const getPaymentStatusTagType = (status: string) => {
  switch (status) {
    case 'paid':
      return 'success'
    case 'unpaid':
      return 'danger'
    case 'partial':
      return 'warning'
    default:
      return 'info'
  }
}

const getPaymentStatusText = (status: string) => {
  switch (status) {
    case 'paid':
      return '已结账'
    case 'unpaid':
      return '欠款'
    case 'partial':
      return '部分结账'
    default:
      return '未知'
  }
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
  currentPage.value = 1 // 重置到第一页
  loadReservations()
}

const handleCurrentChange = (page: number) => {
  currentPage.value = page
  loadReservations()
}

// 订单操作方法
const viewOrder = (order: ReservationDTO) => {
  ElMessage.info(`查看订单: ${order.orderNumber}`)
}

const editOrder = (order: ReservationDTO) => {
  ElMessage.info(`编辑订单: ${order.orderNumber}`)
}

const checkInOrder = async (order: ReservationDTO) => {
  const loadingInstance = ElLoading.service({ text: '正在办理入住...' })
  try {
    const response = await checkInReservation(order.id)
    if (response.success) {
      ElMessage.success(`办理入住成功: ${order.orderNumber}`)
      // 重新加载数据
      await loadReservations()
      await loadStatistics()
    } else {
      ElMessage.error(response.message)
    }
  } catch (error) {
    console.error('办理入住失败:', error)
    ElMessage.error('办理入住失败')
  } finally {
    loadingInstance.close()
  }
}

const checkOutOrder = async (order: ReservationDTO) => {
  const loadingInstance = ElLoading.service({ text: '正在办理退房...' })
  try {
    const response = await checkOutReservation(order.id)
    if (response.success) {
      ElMessage.success(`办理退房成功: ${order.orderNumber}`)
      // 重新加载数据
      await loadReservations()
      await loadStatistics()
    } else {
      ElMessage.error(response.message)
    }
  } catch (error) {
    console.error('办理退房失败:', error)
    ElMessage.error('办理退房失败')
  } finally {
    loadingInstance.close()
  }
}

const cancelOrder = async (order: ReservationDTO) => {
  const loadingInstance = ElLoading.service({ text: '正在取消订单...' })
  try {
    const response = await cancelReservation(order.id)
    if (response.success) {
      ElMessage.success(`取消订单成功: ${order.orderNumber}`)
      // 重新加载数据
      await loadReservations()
      await loadStatistics()
    } else {
      ElMessage.error(response.message)
    }
  } catch (error) {
    console.error('取消订单失败:', error)
    ElMessage.error('取消订单失败')
  } finally {
    loadingInstance.close()
  }
}

// 处理订单操作
const handleOrderAction = async (command: string, order: ReservationDTO) => {
  switch (command) {
    case 'checkIn':
      await checkInOrder(order)
      break
    case 'checkOut':
      await checkOutOrder(order)
      break
    case 'cancel':
      await cancelOrder(order)
      break
    case 'moveOut':
      await handleMoveOutOrderBox(order)
      break
    default:
      ElMessage.info('功能开发中...')
  }
}

// 移出订单盒子
const handleMoveOutOrderBox = async (order: ReservationDTO) => {
  // 找到对应的订单盒子项
  const boxItem = orderBoxItems.value.find((item) => item.reservation.id === order.id)
  if (!boxItem) {
    ElMessage.warning('未找到订单盒子记录')
    return
  }

  const loadingInstance = ElLoading.service({ text: '正在移出订单盒子...' })
  try {
    const response = await moveOutOrderBox({ orderBoxItemId: boxItem.id })
    if (response.success) {
      ElMessage.success('已移出订单盒子')
      // 重新加载订单盒子数据
      await loadOrderBox()
    } else {
      ElMessage.error(response.message)
    }
  } catch (error) {
    console.error('移出订单盒子失败:', error)
    ElMessage.error('移出订单盒子失败')
  } finally {
    loadingInstance.close()
  }
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

  // 初始化加载数据
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
}

.sidebar-header {
  padding: 16px;
  border-bottom: 1px solid #e8e8e8;
  display: flex;
  align-items: center;
  gap: 8px;
}

.sidebar-title {
  font-size: 14px;
  color: #666;
}

.sidebar-menu {
  flex: 1;
  border-right: none;
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
}

.order-link {
  color: #1890ff;
  padding: 0;
  font-size: 14px;
}

.channel-order {
  font-size: 12px;
  color: #999;
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
