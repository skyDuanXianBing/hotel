<template>
  <div class="pricing-tools-container">
    <!-- 连接状态视图 -->
    <div v-if="!showConfigPage" class="connection-view">
      <div class="pricelabs-card">
        <div class="card-content">
          <div class="logo-section">
            <div class="pricelabs-logo">
              <div class="logo-icon">
                <svg viewBox="0 0 40 40" xmlns="http://www.w3.org/2000/svg">
                  <rect x="10" y="10" width="20" height="20" fill="#E53935" transform="rotate(45 20 20)" />
                </svg>
              </div>
              <span class="logo-text">PriceLabs</span>
            </div>
          </div>
          <p class="description">连接PriceLabs，实现动态定价</p>
          <el-button type="primary" size="large" @click="handleConfigure">
            配置
          </el-button>
        </div>
      </div>
    </div>

    <!-- 配置列表视图 -->
    <div v-else class="config-view">
      <!-- 面包屑导航 -->
      <div class="breadcrumb-section">
        <el-breadcrumb separator=">">
          <el-breadcrumb-item>
            <el-button link @click="showConfigPage = false">三方集成</el-button>
          </el-breadcrumb-item>
          <el-breadcrumb-item>PriceLabs</el-breadcrumb-item>
        </el-breadcrumb>
      </div>

      <!-- 标签页 -->
      <el-tabs v-model="activeTab" class="config-tabs" @tab-change="handleTabChange">
        <!-- 房源连接标签页 -->
        <el-tab-pane label="房源连接" name="connections">
          <!-- 说明区域 -->
          <div class="instructions-banner">
            <div class="instruction-item">
              <span class="instruction-number">1.</span>
              <span class="instruction-text">在添加房源之前，请先去设置门店资料</span>
            </div>
            <div class="instruction-item">
              <span class="instruction-number">2.</span>
              <span class="instruction-text">添加房源后，需要一些时间与PriceLabs建立连接，如果长时间未连接成功，可以点击"同步"按钮再次尝试</span>
            </div>
            <div class="instruction-item">
              <span class="instruction-number">3.</span>
              <span class="instruction-text">默认情况下，PriceLabs每24小时同步一次价格给PMS，如果你需要立即同步或调整同步频率，请去PriceLabs进行对应的操作</span>
            </div>
          </div>

          <!-- 集成配置区域 -->
          <div class="integration-config-section">
            <div class="section-row">
              <div class="section-label">
                <h3 class="label-title">账户配置</h3>
                <p class="label-desc">请输入您在 PriceLabs 注册的邮箱地址</p>
              </div>
              <div class="config-form">
                <el-input
                  v-model="integration.priceLabsEmail"
                  placeholder="example@email.com"
                  style="width: 300px; margin-right: 12px"
                  :disabled="integration.isEnabled"
                />
                <el-button
                  type="primary"
                  :disabled="!integration.priceLabsEmail || integration.isEnabled"
                  @click="handleSaveConfig"
                >
                  保存配置
                </el-button>
                <el-text v-if="integration.isEnabled" type="info" size="small">
                  (需先禁用集成才能修改邮箱)
                </el-text>
              </div>
            </div>
          </div>

          <!-- 集成状态区域 -->
          <div class="integration-status-section">
            <div class="section-row">
              <div class="section-label">
                <h3 class="label-title">集成状态</h3>
                <p class="label-desc">{{ integration.isEnabled ? '已启用 PriceLabs 集成' : '未启用 PriceLabs 集成' }}</p>
              </div>
              <div class="status-actions">
                <el-tag :type="integration.isEnabled ? 'success' : 'info'" size="large">
                  {{ integration.isEnabled ? '已启用' : '未启用' }}
                </el-tag>
                <el-switch
                  v-model="integration.isEnabled"
                  :loading="toggleLoading"
                  @change="handleToggleIntegration"
                />
              </div>
            </div>
            <div v-if="integration.isEnabled" class="sync-stats">
              <div class="stat-item">
                <span class="stat-label">已连接房型</span>
                <span class="stat-value">{{ integration.connectedRoomTypeCount || 0 }}</span>
              </div>
              <div class="stat-item">
                <span class="stat-label">最后价格同步</span>
                <span class="stat-value">{{ formatDateTime(integration.lastPriceSyncAt) || '-' }}</span>
              </div>
              <div class="stat-item">
                <span class="stat-label">同步成功率</span>
                <span class="stat-value">{{ calculateSuccessRate() }}</span>
              </div>
            </div>
          </div>

          <!-- 工具栏 -->
          <div class="toolbar">
            <div class="toolbar-left">
              <span class="filter-label">连接状态</span>
              <el-select v-model="connectionFilter" placeholder="请选择" style="width: 120px">
                <el-option label="全部" value="all" />
                <el-option label="已连接" value="connected" />
                <el-option label="未连接" value="disconnected" />
              </el-select>
            </div>
            <el-button type="primary" @click="handleAddConnection">添加连接</el-button>
          </div>

          <!-- 配置表格 -->
          <el-table :data="filteredConnections" border stripe class="config-table" v-loading="connectionsLoading">
            <el-table-column prop="roomTypeName" label="房型" min-width="180" />
            <el-table-column prop="pricePlanName" label="价格计划" min-width="150" />
            <el-table-column prop="priceLabsListingId" label="Listing ID" min-width="200" />
            <el-table-column label="状态" width="120" align="center">
              <template #default="{ row }">
                <el-tag :type="row.syncStatus === 'connected' ? 'success' : row.syncStatus === 'error' ? 'danger' : 'info'" size="small">
                  {{ getSyncStatusText(row.syncStatus) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="最近同步" width="180" align="center">
              <template #default="{ row }">
                {{ formatDateTime(row.lastSyncAt) || '-' }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="180" align="center" fixed="right">
              <template #default="{ row }">
                <el-button
                  link
                  type="primary"
                  @click="handleOpenStatusDialog({ type: 'listing', id: row.priceLabsListingId })"
                >
                  状态查询
                </el-button>
                <el-button
                  link
                  type="primary"
                  :loading="syncingRoomTypeIds.includes(row.roomTypeId)"
                  @click="handleSyncRoomType(row)"
                >
                  同步
                </el-button>
                <el-button
                  link
                  :type="row.isEnabled ? 'warning' : 'success'"
                  @click="handleToggleConnection(row)"
                >
                  {{ row.isEnabled ? '禁用' : '启用' }}
                </el-button>
                <el-button link type="danger" @click="handleDeleteConnection(row)">
                  删除
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- 同步日志标签页 -->
        <el-tab-pane label="同步日志" name="logs">
          <div class="toolbar">
            <div class="toolbar-left">
              <span class="filter-label">诊断工具</span>
              <el-text type="info" size="small">用于认证/排查：查询 /status，手动推送 /reservations</el-text>
            </div>
            <div>
              <el-button @click="handleOpenStatusDialog()">状态查询</el-button>
              <el-button type="primary" @click="handleOpenPushReservationsDialog()">推送预订</el-button>
            </div>
          </div>

          <el-table :data="syncLogs" border stripe class="config-table" v-loading="logsLoading">
            <el-table-column label="时间" width="180" align="center">
              <template #default="{ row }">
                {{ formatDateTime(row.createdAt) }}
              </template>
            </el-table-column>
            <el-table-column prop="syncTypeDisplay" label="类型" width="120" align="center" />
            <el-table-column prop="directionDisplay" label="方向" width="100" align="center">
              <template #default="{ row }">
                <el-tag :type="row.direction === 'INBOUND' ? 'success' : 'primary'" size="small">
                  {{ row.directionDisplay }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="statusDisplay" label="状态" width="100" align="center">
              <template #default="{ row }">
                <el-tag :type="row.status === 'SUCCESS' ? 'success' : row.status === 'FAILURE' ? 'danger' : 'warning'" size="small">
                  {{ row.statusDisplay }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="affectedCount" label="影响记录" width="100" align="center" />
            <el-table-column prop="errorMessage" label="错误信息" min-width="200">
              <template #default="{ row }">
                <span class="error-message">{{ row.errorMessage || '-' }}</span>
              </template>
            </el-table-column>
          </el-table>

          <!-- 分页 -->
          <div class="pagination-wrapper">
            <el-pagination
              v-model:current-page="logsPagination.page"
              v-model:page-size="logsPagination.size"
              :total="logsPagination.total"
              :page-sizes="[10, 20, 50]"
              layout="total, sizes, prev, pager, next"
              @size-change="loadSyncLogs"
              @current-change="loadSyncLogs"
            />
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- 添加连接对话框 -->
    <el-dialog
      v-model="showConnectionDialog"
      title="添加连接"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-alert
        title="提示"
        type="warning"
        :closable="false"
        show-icon
        style="margin-bottom: 12px"
      >
        <template #default>
          <div>为保证推价稳定性，同一房型仅允许绑定一个启用的价格计划连接。</div>
          <div>如需更换价格计划，请先禁用或删除该房型的原连接后再添加。</div>
        </template>
      </el-alert>
      <el-form :model="connectionForm" label-width="100px">
        <el-form-item label="房型" required>
          <el-select
            v-model="connectionForm.roomTypeId"
            placeholder="请选择房型"
            style="width: 100%"
            @change="handleRoomTypeChange"
          >
            <el-option
              v-for="rt in roomTypes"
              :key="rt.id"
              :label="rt.name"
              :value="rt.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="价格计划" required>
          <el-select
            v-model="connectionForm.pricePlanId"
            placeholder="请选择已绑定价格计划"
            style="width: 100%"
            :disabled="!connectionForm.roomTypeId"
          >
            <el-option
              v-for="pp in roomTypeBoundPricePlans"
              :key="pp.id"
              :label="pp.name"
              :value="pp.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showConnectionDialog = false">取消</el-button>
          <el-button type="primary" :loading="saveConnectionLoading" @click="handleSaveConnection">保存</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 编辑价格调整对话框 -->
    <el-dialog
      v-model="showAdjustmentDialog"
      title="编辑渠道价格调整"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form :model="adjustmentForm" label-width="100px">
        <el-form-item label="渠道">
          <el-input :model-value="adjustmentForm.channelName" disabled />
        </el-form-item>
        <el-form-item label="调整方向">
          <el-radio-group v-model="adjustmentDirection">
            <el-radio value="up">加价（更贵）</el-radio>
            <el-radio value="down">减价（更便宜）</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="调整类型">
          <el-radio-group v-model="adjustmentForm.adjustmentType">
            <el-radio value="PERCENTAGE">百分比</el-radio>
            <el-radio value="FIXED">固定金额</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="调整值">
          <el-input-number
            v-model="adjustmentFormValue"
            :min="0"
            :precision="adjustmentForm.adjustmentType === 'PERCENTAGE' ? 1 : 0"
            style="width: 200px"
          />
          <span class="unit-label">{{ adjustmentForm.adjustmentType === 'PERCENTAGE' ? '%' : '¥' }}</span>
        </el-form-item>
        <el-form-item label="自动同步">
          <el-switch v-model="adjustmentForm.autoSyncPrice" />
          <span class="form-tip">开启后将自动同步价格到OTA</span>
        </el-form-item>
        <el-form-item label="示例计算">
          <div class="example-preview">
            <span>基础价格 ¥1000 → 渠道价格 ¥{{ calculatePreviewPrice() }}</span>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showAdjustmentDialog = false">取消</el-button>
          <el-button type="primary" :loading="saveAdjustmentLoading" @click="handleSaveAdjustment">保存</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- PriceLabs 状态查询（PMS -> PriceLabs /status） -->
    <el-dialog
      v-model="showStatusDialog"
      title="PriceLabs 状态查询（/status）"
      width="760px"
      :close-on-click-modal="false"
    >
      <el-form :model="statusForm" label-width="110px">
        <el-form-item label="类型" required>
          <el-select v-model="statusForm.type" style="width: 220px">
            <el-option label="listing" value="listing" />
            <el-option label="reservation" value="reservation" />
            <el-option label="calendar" value="calendar" />
          </el-select>
        </el-form-item>

        <el-form-item v-if="statusForm.type === 'listing'" label="Listing ID">
          <el-select
            v-model="statusListingId"
            filterable
            clearable
            placeholder="从房源连接选择"
            style="width: 520px"
            @change="handleSelectStatusListingId"
          >
            <el-option
              v-for="conn in connections"
              :key="conn.id"
              :label="`${conn.roomTypeName} - ${conn.pricePlanName} (${conn.priceLabsListingId})`"
              :value="conn.priceLabsListingId"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="ID" required>
          <el-input
            v-model="statusForm.id"
            placeholder="listing_id / reservation_id / calendar id"
            style="width: 520px"
          />
          <el-text type="info" size="small" style="margin-left: 10px">
            reservation 通常用预订单号(orderNumber)
          </el-text>
        </el-form-item>
      </el-form>

      <el-divider content-position="left">结果</el-divider>
      <div v-if="statusResult" class="status-result-vertical">
        <!-- Success 区域 -->
        <div class="result-block">
          <div class="result-header">
            <span class="result-title">Success</span>
            <el-tag v-if="statusResult.success?.length" type="success" size="small">
              {{ statusResult.success.length }} 条
            </el-tag>
          </div>
          <el-scrollbar v-if="statusResult.success?.length" max-height="300">
            <div
              v-for="(item, idx) in statusResult.success"
              :key="idx"
              class="status-item"
            >
              <template v-if="typeof item === 'object' && item !== null">
                <el-descriptions :column="2" size="small" border>
                  <el-descriptions-item
                    v-for="(value, key) in item"
                    :key="key"
                    :label="String(key)"
                  >
                    <template v-if="typeof value === 'boolean'">
                      <el-tag :type="value ? 'success' : 'info'" size="small">
                        {{ value ? '是' : '否' }}
                      </el-tag>
                    </template>
                    <template v-else-if="value === null || value === undefined">
                      <span class="text-muted">-</span>
                    </template>
                    <template v-else>
                      {{ value }}
                    </template>
                  </el-descriptions-item>
                </el-descriptions>
              </template>
              <template v-else>
                <el-tag size="small">{{ item }}</el-tag>
              </template>
            </div>
          </el-scrollbar>
          <el-empty v-else description="无成功记录" :image-size="50" />
        </div>

        <!-- Failure 区域 -->
        <div class="result-block">
          <div class="result-header">
            <span class="result-title">Failure</span>
            <el-tag v-if="statusResult.failure?.length" type="danger" size="small">
              {{ statusResult.failure.length }} 条
            </el-tag>
          </div>
          <el-scrollbar v-if="statusResult.failure?.length" max-height="200">
            <div
              v-for="(item, idx) in statusResult.failure"
              :key="idx"
              class="status-item"
            >
              <template v-if="typeof item === 'object' && item !== null">
                <el-descriptions :column="2" size="small" border>
                  <el-descriptions-item
                    v-for="(value, key) in item"
                    :key="key"
                    :label="String(key)"
                  >
                    {{ value ?? '-' }}
                  </el-descriptions-item>
                </el-descriptions>
              </template>
              <template v-else>
                <el-tag type="danger" size="small">{{ item }}</el-tag>
              </template>
            </div>
          </el-scrollbar>
          <el-empty v-else description="无失败记录" :image-size="50" />
        </div>
      </div>
      <el-empty v-else description="点击查询后显示结果" :image-size="80" />

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showStatusDialog = false">关闭</el-button>
          <el-button type="primary" :loading="statusLoading" @click="handleQueryStatus">查询</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- PriceLabs 预订推送（PMS -> PriceLabs /reservations） -->
    <el-dialog
      v-model="showPushReservationsDialog"
      title="PriceLabs 预订推送（/reservations）"
      width="760px"
      :close-on-click-modal="false"
    >
      <el-form label-width="110px">
        <el-form-item label="日期范围">
          <el-date-picker
            v-model="pushReservationsRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
          />
          <el-text type="info" size="small" style="margin-left: 10px">
            默认：2020-01-01 ~ today+365
          </el-text>
        </el-form-item>
      </el-form>

      <el-divider content-position="left">结果</el-divider>
      <div v-if="pushReservationsResult">
        <el-descriptions border :column="2" size="small">
          <el-descriptions-item label="listingCount">{{ pushReservationsResult.listingCount }}</el-descriptions-item>
          <el-descriptions-item label="reservationCount">{{ pushReservationsResult.reservationCount }}</el-descriptions-item>
          <el-descriptions-item label="successCount">{{ pushReservationsResult.successCount }}</el-descriptions-item>
          <el-descriptions-item label="failureCount">{{ pushReservationsResult.failureCount }}</el-descriptions-item>
        </el-descriptions>
        <div style="margin-top: 12px">
          <div class="result-title">Failures</div>
          <el-scrollbar height="220">
            <pre class="result-json">{{ JSON.stringify(pushReservationsResult.failures ?? [], null, 2) }}</pre>
          </el-scrollbar>
        </div>
      </div>
      <el-empty v-else description="点击“推送”后显示结果" :image-size="80" />

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showPushReservationsDialog = false">关闭</el-button>
          <el-button type="primary" :loading="pushReservationsLoading" @click="handlePushReservations">
            推送
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Right } from '@element-plus/icons-vue'
import * as priceLabsApi from '@/api/pricelabs'
import { getAllRoomTypes, type RoomTypeDTO } from '@/api/roomType'
import {
  getPricePlansByRoomType,
  type PricePlanDTO,
  type RoomTypePricePlanDTO,
} from '@/api/pricePlan'
import {
  getAllOtaIntegrations,
  updatePriceAdjustment,
  type OtaIntegrationDTO,
  type PriceAdjustmentType,
} from '@/api/otaIntegration'
import type {
  PriceLabsIntegrationDTO,
  PriceLabsConnectionDTO,
  PriceLabsSyncLogDTO,
  PriceLabsStatusType,
  PriceLabsStatusResult,
  PriceLabsPushReservationsResult,
} from '@/api/pricelabs'

// 视图状态
const showConfigPage = ref(false)
const activeTab = ref('connections')

// 集成配置
const integration = ref<PriceLabsIntegrationDTO>({
  isEnabled: false,
  connectedRoomTypeCount: 0,
  totalSyncCount: 0,
  successSyncCount: 0,
})
const toggleLoading = ref(false)

// 连接列表
const connections = ref<PriceLabsConnectionDTO[]>([])
const connectionsLoading = ref(false)
const syncingRoomTypeIds = ref<number[]>([])
const connectionFilter = ref('all')

// 渠道价格调整（使用 OTA 直连数据）
interface ChannelAdjustmentItem {
  channelId: number
  channelName: string
  channelCode: string
  adjustmentType: PriceAdjustmentType
  adjustmentValue: number | null
  autoSyncPrice: boolean
  exampleBasePrice: number
  exampleChannelPrice: number
}
const channelAdjustments = ref<ChannelAdjustmentItem[]>([])
const adjustmentsLoading = ref(false)

// 同步日志
const syncLogs = ref<PriceLabsSyncLogDTO[]>([])
const logsLoading = ref(false)
const logsPagination = reactive({
  page: 1,
  size: 20,
  total: 0,
})

// 诊断工具：/status & /reservations
const showStatusDialog = ref(false)
const statusLoading = ref(false)
const statusListingId = ref<string | null>(null)
const statusForm = reactive({
  type: 'listing' as PriceLabsStatusType,
  id: '',
})
const statusResult = ref<PriceLabsStatusResult | null>(null)

const showPushReservationsDialog = ref(false)
const pushReservationsLoading = ref(false)
const pushReservationsRange = ref<[string, string]>(['2020-01-01', ''])
const pushReservationsResult = ref<PriceLabsPushReservationsResult | null>(null)

// 房型和价格计划选项
const roomTypes = ref<RoomTypeDTO[]>([])
const roomTypeBoundPricePlans = ref<PricePlanDTO[]>([])

// 添加连接对话框
const showConnectionDialog = ref(false)
const saveConnectionLoading = ref(false)
const connectionForm = reactive({
  roomTypeId: null as number | null,
  pricePlanId: null as number | null,
})

// 编辑价格调整对话框
const showAdjustmentDialog = ref(false)
const saveAdjustmentLoading = ref(false)
const adjustmentDirection = ref<'up' | 'down'>('up')
const adjustmentFormValue = ref(0)
const adjustmentForm = reactive({
  channelId: 0,
  channelName: '',
  adjustmentType: 'PERCENTAGE' as PriceAdjustmentType,
  adjustmentValue: 0 as number | null,
  autoSyncPrice: true,
})

// 过滤后的连接列表
const filteredConnections = computed(() => {
  if (connectionFilter.value === 'all') {
    return connections.value
  }
  const isConnected = connectionFilter.value === 'connected'
  return connections.value.filter((conn) =>
    isConnected ? conn.syncStatus === 'connected' : conn.syncStatus !== 'connected'
  )
})

const enabledConnectionByRoomTypeId = computed(() => {
  const map = new Map<number, PriceLabsConnectionDTO>()
  for (const conn of connections.value) {
    if (conn.isEnabled) {
      map.set(conn.roomTypeId, conn)
    }
  }
  return map
})

// 进入配置页面
const handleConfigure = () => {
  showConfigPage.value = true
  loadAllData()
}

const formatDateYYYYMMDD = (date: Date): string => {
  const yyyy = date.getFullYear()
  const mm = String(date.getMonth() + 1).padStart(2, '0')
  const dd = String(date.getDate()).padStart(2, '0')
  return `${yyyy}-${mm}-${dd}`
}

const addDays = (date: Date, days: number): Date => {
  const d = new Date(date)
  d.setDate(d.getDate() + days)
  return d
}

// 加载所有数据
const loadAllData = async () => {
  await Promise.all([
    loadIntegration(),
    loadConnections(),
    loadChannelAdjustments(),
    loadRoomTypes(),
  ])
}

// 加载集成配置
const loadIntegration = async () => {
  try {
    const res = await priceLabsApi.getIntegration()
    if (res.success) {
      integration.value = res.data
    }
  } catch (error) {
    console.error('加载集成配置失败:', error)
  }
}

// 加载连接列表
const loadConnections = async () => {
  connectionsLoading.value = true
  try {
    const res = await priceLabsApi.getConnections()
    if (res.success) {
      connections.value = res.data
    }
  } catch (error) {
    console.error('加载连接列表失败:', error)
  } finally {
    connectionsLoading.value = false
  }
}

// 加载渠道价格调整（从 OTA 直连表获取）
const loadChannelAdjustments = async () => {
  adjustmentsLoading.value = true
  try {
    const res = await getAllOtaIntegrations()
    if (res.success) {
      // 将 OTA 直连数据转换为价格调整格式
      channelAdjustments.value = res.data.map((ota: OtaIntegrationDTO) => {
        const basePrice = 1000
        const adjustmentValue = ota.priceAdjustmentValue ?? 0
        let channelPrice = basePrice
        if (ota.priceAdjustmentType === 'PERCENTAGE') {
          channelPrice = basePrice * (1 + adjustmentValue / 100)
        } else if (ota.priceAdjustmentType === 'FIXED') {
          channelPrice = basePrice + adjustmentValue
        }
        return {
          channelId: ota.id,
          channelName: ota.name,
          channelCode: ota.code,
          adjustmentType: ota.priceAdjustmentType ?? 'PERCENTAGE',
          adjustmentValue: ota.priceAdjustmentValue ?? null,
          autoSyncPrice: ota.autoSyncPrice ?? true,
          exampleBasePrice: basePrice,
          exampleChannelPrice: channelPrice,
        }
      })
    }
  } catch (error) {
    console.error('加载渠道价格调整失败:', error)
  } finally {
    adjustmentsLoading.value = false
  }
}

// 加载同步日志
const loadSyncLogs = async () => {
  logsLoading.value = true
  try {
    const res = await priceLabsApi.getSyncLogs(logsPagination.page - 1, logsPagination.size)
    if (res.success) {
      syncLogs.value = res.data.content
      logsPagination.total = res.data.totalElements
    }
  } catch (error) {
    console.error('加载同步日志失败:', error)
  } finally {
    logsLoading.value = false
  }
}

// 加载房型列表
const loadRoomTypes = async () => {
  try {
    const res = await getAllRoomTypes()
    if (res.success) {
      roomTypes.value = res.data
    }
  } catch (error) {
    console.error('加载房型列表失败:', error)
  }
}

// 加载房型已绑定的价格计划（来源：价格计划页的房型绑定关系）
const loadBoundPricePlansByRoomType = async (roomTypeId: number) => {
  try {
    const res = (await getPricePlansByRoomType(roomTypeId)) as unknown as {
      success?: boolean
      data?: RoomTypePricePlanDTO[]
    }
    const mappings = Array.isArray(res?.data) ? res.data : []
    const uniquePlans = new Map<number, PricePlanDTO>()
    for (const mapping of mappings) {
      const plan = mapping.pricePlan
      if (!plan?.id) {
        continue
      }
      if (uniquePlans.has(plan.id)) {
        continue
      }
      uniquePlans.set(plan.id, {
        id: plan.id,
        name: plan.name || `Plan ${plan.id}`,
        minNights: 1,
        includeMeal: false,
      })
    }
    roomTypeBoundPricePlans.value = Array.from(uniquePlans.values())
  } catch (error) {
    roomTypeBoundPricePlans.value = []
    console.error('加载房型已绑定价格计划失败:', error)
  }
}

const handleRoomTypeChange = async (roomTypeId: number | null) => {
  connectionForm.pricePlanId = null
  roomTypeBoundPricePlans.value = []
  if (!roomTypeId) {
    return
  }
  await loadBoundPricePlansByRoomType(roomTypeId)
}

// 保存配置
const handleSaveConfig = async () => {
  if (!integration.value.priceLabsEmail) {
    ElMessage.warning('请输入 PriceLabs 邮箱地址')
    return
  }

  try {
    const res = await priceLabsApi.updateIntegrationConfig({
      priceLabsEmail: integration.value.priceLabsEmail,
    })
    if (res.success) {
      integration.value = res.data
      ElMessage.success('配置保存成功')
    } else {
      ElMessage.error(res.message || '保存失败')
    }
  } catch (error) {
    console.error('保存配置失败:', error)
    ElMessage.error('保存失败')
  }
}

// 切换集成状态
const handleToggleIntegration = async (enabled: boolean) => {
  // 启用前检查是否已配置邮箱
  if (enabled && (!integration.value.priceLabsEmail || !integration.value.priceLabsEmail.trim())) {
    ElMessage.warning('请先配置 PriceLabs 邮箱地址')
    integration.value.isEnabled = false
    return
  }

  toggleLoading.value = true
  try {
    const res = await priceLabsApi.toggleIntegration(enabled)
    if (res.success) {
      integration.value = res.data
      ElMessage.success(enabled ? '已启用 PriceLabs 集成' : '已禁用 PriceLabs 集成')
    } else {
      // 恢复状态
      integration.value.isEnabled = !enabled
      ElMessage.error(res.message || '操作失败')
    }
  } catch (error) {
    integration.value.isEnabled = !enabled
    ElMessage.error('操作失败')
  } finally {
    toggleLoading.value = false
  }
}

// 添加连接
const handleAddConnection = () => {
  connectionForm.roomTypeId = null
  connectionForm.pricePlanId = null
  roomTypeBoundPricePlans.value = []
  showConnectionDialog.value = true
}

// 保存连接
const handleSaveConnection = async () => {
  if (!connectionForm.roomTypeId) {
    ElMessage.warning('请选择房型')
    return
  }

  const existing = enabledConnectionByRoomTypeId.value.get(connectionForm.roomTypeId)
  if (existing) {
    const planName = existing.pricePlanName || '（未知）'
    ElMessage.warning(`该房型已绑定价格计划：${planName}。如需更换，请先禁用或删除原连接后再添加。`)
    return
  }

  if (!connectionForm.pricePlanId) {
    ElMessage.warning('请选择价格计划')
    return
  }
  const isBoundPlan = roomTypeBoundPricePlans.value.some((plan) => plan.id === connectionForm.pricePlanId)
  if (!isBoundPlan) {
    ElMessage.warning('请选择该房型在“价格计划-应用房型”中已绑定的价格计划')
    return
  }

  saveConnectionLoading.value = true
  try {
    if (!integration.value.isEnabled) {
      if (!integration.value.priceLabsEmail || !integration.value.priceLabsEmail.trim()) {
        ElMessage.warning('请先配置 PriceLabs 邮箱地址')
        return
      }

      const toggleRes = await priceLabsApi.toggleIntegration(true)
      if (toggleRes.success) {
        if (toggleRes.data) {
          integration.value = toggleRes.data
        } else {
          await loadIntegration()
        }
        ElMessage.success('已启用 PriceLabs 集成')
      } else {
        ElMessage.error(toggleRes.message || '启用 PriceLabs 集成失败')
        return
      }
    }

    const res = await priceLabsApi.createConnection(connectionForm.roomTypeId, connectionForm.pricePlanId)
    if (res.success) {
      ElMessage.success('添加连接成功')
      showConnectionDialog.value = false
      await loadConnections()
      await loadIntegration()
      return
    }
    if (res.data) {
      ElMessage.warning(res.message || '连接已保存，但同步失败')
      showConnectionDialog.value = false
      await loadConnections()
      await loadIntegration()
      return
    } else {
      ElMessage.error(res.message || '添加连接失败')
    }
  } catch (error) {
    ElMessage.error('添加连接失败')
  } finally {
    saveConnectionLoading.value = false
  }
}

// 切换连接状态
const handleToggleConnection = async (row: PriceLabsConnectionDTO) => {
  try {
    const res = await priceLabsApi.updateConnectionStatus(row.id, !row.isEnabled)
    if (res.success) {
      row.isEnabled = !row.isEnabled
      ElMessage.success(row.isEnabled ? '已启用连接' : '已禁用连接')
    } else {
      ElMessage.error(res.message || '操作失败')
    }
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

// 单房型同步
const handleSyncRoomType = async (row: PriceLabsConnectionDTO) => {
  if (!row.isEnabled) {
    ElMessage.warning('请先启用连接')
    return
  }
  if (syncingRoomTypeIds.value.includes(row.roomTypeId)) {
    return
  }

  syncingRoomTypeIds.value = [...syncingRoomTypeIds.value, row.roomTypeId]
  try {
    const res = await priceLabsApi.syncRoomType(row.roomTypeId)
    if (res.success) {
      ElMessage.success('同步已触发')
      await loadConnections()
      await loadIntegration()
    } else {
      ElMessage.error(res.message || '同步失败')
    }
  } catch (error) {
    ElMessage.error('同步失败')
  } finally {
    syncingRoomTypeIds.value = syncingRoomTypeIds.value.filter((id) => id !== row.roomTypeId)
  }
}

// 删除连接
const handleDeleteConnection = async (row: PriceLabsConnectionDTO) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除 "${row.roomTypeName} - ${row.pricePlanName}" 的连接配置吗?`,
      '删除确认',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )

    const res = await priceLabsApi.deleteConnection(row.id)
    if (res.success) {
      ElMessage.success('删除成功')
      await loadConnections()
      await loadIntegration()
    } else {
      ElMessage.error(res.message || '删除失败')
    }
  } catch {
    // 用户取消
  }
}

// 编辑价格调整
const handleEditAdjustment = (row: ChannelAdjustmentItem) => {
  adjustmentForm.channelId = row.channelId
  adjustmentForm.channelName = row.channelName
  adjustmentForm.adjustmentType = row.adjustmentType || 'PERCENTAGE'
  adjustmentForm.autoSyncPrice = row.autoSyncPrice

  // 处理调整值和方向
  const value = row.adjustmentValue || 0
  adjustmentDirection.value = value >= 0 ? 'up' : 'down'
  adjustmentFormValue.value = Math.abs(value)

  showAdjustmentDialog.value = true
}

// 保存价格调整
const handleSaveAdjustment = async () => {
  saveAdjustmentLoading.value = true
  try {
    // 根据方向计算最终值
    const finalValue = adjustmentDirection.value === 'up' ? adjustmentFormValue.value : -adjustmentFormValue.value

    const res = await updatePriceAdjustment(adjustmentForm.channelId, {
      priceAdjustmentType: adjustmentForm.adjustmentType,
      priceAdjustmentValue: finalValue,
      autoSyncPrice: adjustmentForm.autoSyncPrice,
    })

    if (res.success) {
      ElMessage.success('保存成功')
      showAdjustmentDialog.value = false
      await loadChannelAdjustments()
    } else {
      ElMessage.error(res.message || '保存失败')
    }
  } catch (error) {
    ElMessage.error('保存失败')
  } finally {
    saveAdjustmentLoading.value = false
  }
}

// 自动同步开关变化
const handleAutoSyncChange = async (row: ChannelAdjustmentItem) => {
  try {
    await updatePriceAdjustment(row.channelId, {
      priceAdjustmentType: row.adjustmentType,
      priceAdjustmentValue: row.adjustmentValue ?? 0,
      autoSyncPrice: row.autoSyncPrice,
    })
    ElMessage.success(row.autoSyncPrice ? '已开启自动同步' : '已关闭自动同步')
  } catch (error) {
    row.autoSyncPrice = !row.autoSyncPrice
    ElMessage.error('操作失败')
  }
}

// 计算预览价格
const calculatePreviewPrice = (): string => {
  const basePrice = 1000
  const value = adjustmentDirection.value === 'up' ? adjustmentFormValue.value : -adjustmentFormValue.value

  if (adjustmentForm.adjustmentType === 'PERCENTAGE') {
    return (basePrice * (1 + value / 100)).toFixed(0)
  } else {
    return (basePrice + value).toFixed(0)
  }
}

// 计算成功率
const calculateSuccessRate = (): string => {
  const total = integration.value.totalSyncCount || 0
  const success = integration.value.successSyncCount || 0
  if (total === 0) return '-'
  return ((success / total) * 100).toFixed(1) + '%'
}

// 格式化日期时间
const formatDateTime = (dateStr: string | undefined): string => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

// 获取同步状态文本
const getSyncStatusText = (status: string): string => {
  const statusMap: Record<string, string> = {
    connected: '已连接',
    disconnected: '未连接',
    error: '错误',
  }
  return statusMap[status] || status
}

// 获取调整类型标签
const getAdjustmentTypeTag = (type: PriceAdjustmentType): string => {
  const tagMap: Record<string, string> = {
    COMMISSION: 'warning',
    PERCENTAGE: 'primary',
    FIXED: 'success',
  }
  return tagMap[type] || 'info'
}

// 获取调整类型文本
const getAdjustmentTypeText = (type: PriceAdjustmentType): string => {
  const textMap: Record<string, string> = {
    COMMISSION: '佣金',
    PERCENTAGE: '百分比',
    FIXED: '固定',
  }
  return textMap[type] || type
}

// 格式化调整值
const formatAdjustmentValue = (row: ChannelAdjustmentItem): string => {
  const value = row.adjustmentValue
  if (value === null || value === undefined) return '-'

  const prefix = value >= 0 ? '+' : ''
  if (row.adjustmentType === 'PERCENTAGE') {
    return `${prefix}${value}%`
  }
  return `${prefix}¥${value}`
}

// 获取调整值样式类
const getAdjustmentValueClass = (row: ChannelAdjustmentItem): string => {
  const value = row.adjustmentValue
  if (value === null || value === undefined) return ''
  return value >= 0 ? 'value-up' : 'value-down'
}

// 监听标签页切换
const handleTabChange = (tab: string) => {
  if (tab === 'logs' && syncLogs.value.length === 0) {
    loadSyncLogs()
  }
}

const handleOpenStatusDialog = (preset?: { type?: PriceLabsStatusType; id?: string }) => {
  statusForm.type = preset?.type || 'listing'
  statusForm.id = preset?.id || ''
  statusListingId.value = statusForm.type === 'listing' ? statusForm.id || null : null
  statusResult.value = null
  showStatusDialog.value = true
}

const handleSelectStatusListingId = (id: string) => {
  statusForm.id = id
}

const handleQueryStatus = async () => {
  const id = statusForm.id?.trim()
  if (!id) {
    ElMessage.warning('请输入 ID')
    return
  }

  statusLoading.value = true
  statusResult.value = null
  try {
    const res = await priceLabsApi.queryStatus([{ id, type: statusForm.type }])
    if (res.success) {
      statusResult.value = res.data
    } else {
      ElMessage.error(res.message || 'status 查询失败')
    }
  } catch (error) {
    ElMessage.error('status 查询失败')
  } finally {
    statusLoading.value = false
  }
}

const handleOpenPushReservationsDialog = () => {
  pushReservationsRange.value = ['2020-01-01', formatDateYYYYMMDD(addDays(new Date(), 365))]
  pushReservationsResult.value = null
  showPushReservationsDialog.value = true
}

const handlePushReservations = async () => {
  pushReservationsLoading.value = true
  pushReservationsResult.value = null

  try {
    const [startDate, endDate] = pushReservationsRange.value || []
    const res = await priceLabsApi.pushReservations({
      startDate: startDate || undefined,
      endDate: endDate || undefined,
    })
    if (res.success) {
      pushReservationsResult.value = res.data
      ElMessage.success('预订推送完成')
      loadSyncLogs()
    } else {
      ElMessage.error(res.message || 'reservations 推送失败')
    }
  } catch (error) {
    ElMessage.error('reservations 推送失败')
  } finally {
    pushReservationsLoading.value = false
  }
}
</script>

<style scoped>
.pricing-tools-container {
  padding: 20px;
  background: #fff;
  min-height: calc(100vh - 100px);
}

/* 连接状态视图 */
.connection-view {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 400px;
  padding: 40px 20px;
}

.pricelabs-card {
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 60px 80px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
  text-align: center;
  max-width: 500px;
  width: 100%;
}

.logo-section {
  margin-bottom: 24px;
}

.pricelabs-logo {
  display: inline-flex;
  align-items: center;
  gap: 12px;
}

.logo-icon {
  width: 40px;
  height: 40px;
}

.logo-icon svg {
  width: 100%;
  height: 100%;
}

.logo-text {
  font-size: 28px;
  font-weight: 600;
  color: #303133;
}

.description {
  font-size: 16px;
  color: #606266;
  margin: 0 0 32px 0;
  line-height: 1.6;
}

/* 配置视图 */
.config-view {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* 面包屑 */
.breadcrumb-section {
  padding-bottom: 16px;
  border-bottom: 1px solid #e4e7ed;
}

:deep(.el-breadcrumb__item:last-child .el-breadcrumb__inner) {
  color: #303133;
  font-weight: 500;
}

/* 标签页 */
.config-tabs {
  margin-top: 16px;
}

/* 说明区域 */
.instructions-banner {
  background: #e6f7ff;
  border: 1px solid #91d5ff;
  border-radius: 4px;
  padding: 16px 20px;
  margin-bottom: 20px;
}

.instruction-item {
  display: flex;
  align-items: flex-start;
  line-height: 1.8;
  color: #1890ff;
  font-size: 14px;
}

.instruction-item + .instruction-item {
  margin-top: 4px;
}

.instruction-number {
  flex-shrink: 0;
  margin-right: 6px;
  font-weight: 500;
}

.instruction-text {
  flex: 1;
}

/* 集成状态区域 */
.integration-status-section {
  background: #f5f7fa;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  padding: 20px 24px;
  margin-bottom: 20px;
}

.section-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 24px;
}

.section-label {
  flex-shrink: 0;
}

.label-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 4px 0;
}

.label-desc {
  font-size: 13px;
  color: #909399;
  margin: 0;
}

.status-actions {
  display: flex;
  align-items: center;
  gap: 16px;
}

.sync-stats {
  display: flex;
  gap: 40px;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #e4e7ed;
}

.stat-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.stat-label {
  font-size: 13px;
  color: #909399;
}

.stat-value {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

/* 工具栏 */
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 0;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.filter-label {
  font-size: 14px;
  color: #606266;
  font-weight: 500;
}

/* 表格 */
.config-table {
  margin-top: 8px;
}

:deep(.el-table th) {
  background-color: #fafafa;
  font-weight: 600;
}

/* 渠道信息 */
.channel-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.channel-name {
  font-weight: 500;
  color: #303133;
}

.channel-code {
  font-size: 12px;
  color: #909399;
}

/* 调整值样式 */
.value-up {
  color: #e6a23c;
  font-weight: 500;
}

.value-down {
  color: #67c23a;
  font-weight: 500;
}

/* 示例计算 */
.example-calc {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
}

.base-price {
  color: #909399;
}

.channel-price {
  color: #409eff;
  font-weight: 500;
}

/* 错误信息 */
.error-message {
  color: #f56c6c;
  font-size: 13px;
}

/* 分页 */
.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

/* 对话框 */
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.status-result-vertical {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.result-block {
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  padding: 12px;
  background: #fafafa;
}

.result-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.result-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.status-item {
  margin-bottom: 12px;
}

.status-item:last-child {
  margin-bottom: 0;
}

.status-item :deep(.el-descriptions) {
  background: #fff;
}

.status-item :deep(.el-descriptions__label) {
  min-width: 100px;
  font-weight: 500;
}

.text-muted {
  color: #909399;
}

.result-json {
  margin: 0;
  font-size: 12px;
  line-height: 1.5;
  color: #606266;
  white-space: pre-wrap;
  word-break: break-word;
}

.unit-label {
  margin-left: 8px;
  color: #606266;
}

.form-tip {
  margin-left: 12px;
  font-size: 12px;
  color: #909399;
}

.example-preview {
  padding: 12px 16px;
  background: #f5f7fa;
  border-radius: 4px;
  color: #606266;
}
</style>
