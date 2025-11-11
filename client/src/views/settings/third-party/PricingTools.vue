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

      <!-- 说明区域 -->
      <div class="instructions-banner">
        <div class="instruction-item">
          <span class="instruction-number">1.</span>
          <span class="instruction-text">访问您的 PriceLabs 账号，创建API密钥并保存</span>
        </div>
        <div class="instruction-item">
          <span class="instruction-number">2.</span>
          <span class="instruction-text">输入下方表单配置房型与PriceLabs对应的价格计划</span>
        </div>
        <div class="instruction-item">
          <span class="instruction-number">3.</span>
          <span class="instruction-text">完成配置后，我们将每小时自动同步价格</span>
        </div>
      </div>

      <!-- 邮箱验证区域 -->
      <div class="email-verification-section">
        <div class="section-row">
          <div class="section-label">
            <h3 class="label-title">邮箱验证</h3>
            <p class="label-desc">使用您的PriceLabs账号邮箱进行验证</p>
          </div>
          <div class="verification-input-group">
            <el-input
              v-model="emailAddress"
              placeholder="请输入邮箱地址"
              class="email-input"
              disabled
            />
            <el-button type="primary">验证账号</el-button>
          </div>
        </div>
      </div>

      <!-- 工具栏 -->
      <div class="toolbar">
        <div class="toolbar-left">
          <span class="filter-label">直连快去</span>
          <el-select v-model="connectionFilter" placeholder="请选择" style="width: 120px">
            <el-option label="全部" value="all" />
            <el-option label="已连接" value="connected" />
            <el-option label="未连接" value="disconnected" />
          </el-select>
        </div>
        <el-button type="primary" @click="handleAddConnection">添加</el-button>
      </div>

      <!-- 配置表格 -->
      <el-table :data="filteredConnections" border stripe class="config-table">
        <el-table-column prop="roomType" label="房型" min-width="200" />
        <el-table-column prop="pricePlan" label="价格计划" min-width="150" />
        <el-table-column label="状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="row.connected ? 'success' : 'info'" size="small">
              {{ row.connected ? '已连接' : '未连接' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lastSyncTime" label="最近同步时间" width="180" align="center" />
        <el-table-column label="操作" width="220" align="center" fixed="right">
          <template #default="{ row }">
            <el-button
              link
              type="primary"
              @click="handleSync(row)"
              :disabled="!row.connected"
            >
              同步
            </el-button>
            <el-button link type="primary" @click="handleEdit(row)">
              编辑
            </el-button>
            <el-button link type="danger" @click="handleDelete(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 添加/编辑连接对话框 -->
    <el-dialog
      v-model="showConnectionDialog"
      :title="editingConnection ? '编辑连接' : '添加连接'"
      width="600px"
      :close-on-click-modal="false"
    >
      <el-form :model="connectionForm" label-width="120px">
        <el-form-item label="房型">
          <el-select
            v-model="connectionForm.roomType"
            placeholder="请选择房型"
            style="width: 100%"
          >
            <el-option
              v-for="roomType in availableRoomTypes"
              :key="roomType"
              :label="roomType"
              :value="roomType"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="价格计划">
          <el-input
            v-model="connectionForm.pricePlan"
            placeholder="请输入价格计划名称"
          />
        </el-form-item>
        <el-form-item label="连接状态">
          <el-switch v-model="connectionForm.connected" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showConnectionDialog = false">取消</el-button>
          <el-button type="primary" @click="handleSaveConnection">保存</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

interface PriceConnection {
  id: number
  roomType: string
  pricePlan: string
  connected: boolean
  lastSyncTime: string
}

interface ConnectionForm {
  roomType: string
  pricePlan: string
  connected: boolean
}

// 视图状态
const showConfigPage = ref(false)
const showConnectionDialog = ref(false)
const editingConnection = ref<PriceConnection | null>(null)

// 邮箱验证
const emailAddress = ref('operations@the-host.jp')

// 筛选条件
const connectionFilter = ref('all')

// 可用房型列表
const availableRoomTypes = [
  '茶途木テル　池袋',
  '标准单人间',
  '标准双人间',
  '豪华套房',
  '家庭房',
]

// 连接列表数据
const connections = ref<PriceConnection[]>([
  {
    id: 1,
    roomType: '茶途木テル　池袋',
    pricePlan: 'Standard Rate',
    connected: true,
    lastSyncTime: '2025-11-08 14:30:00',
  },
  {
    id: 2,
    roomType: '茶途木テル　池袋',
    pricePlan: 'Standard Rate',
    connected: true,
    lastSyncTime: '2025-11-08 14:30:00',
  },
  {
    id: 3,
    roomType: '茶途木テル　池袋',
    pricePlan: 'Standard Rate',
    connected: true,
    lastSyncTime: '2025-11-08 14:30:00',
  },
  {
    id: 4,
    roomType: '茶途木テル　池袋',
    pricePlan: 'Standard Rate',
    connected: true,
    lastSyncTime: '2025-11-08 14:30:00',
  },
  {
    id: 5,
    roomType: '茶途木テル　池袋',
    pricePlan: 'Standard Rate',
    connected: true,
    lastSyncTime: '2025-11-08 14:30:00',
  },
])

// 表单数据
const connectionForm = reactive<ConnectionForm>({
  roomType: '',
  pricePlan: '',
  connected: false,
})

// 过滤后的连接列表
const filteredConnections = computed(() => {
  if (connectionFilter.value === 'all') {
    return connections.value
  }
  const isConnected = connectionFilter.value === 'connected'
  return connections.value.filter((conn) => conn.connected === isConnected)
})

// 进入配置页面
const handleConfigure = () => {
  showConfigPage.value = true
}

// 添加连接
const handleAddConnection = () => {
  editingConnection.value = null
  connectionForm.roomType = ''
  connectionForm.pricePlan = ''
  connectionForm.connected = false
  showConnectionDialog.value = true
}

// 编辑连接
const handleEdit = (row: PriceConnection) => {
  editingConnection.value = row
  connectionForm.roomType = row.roomType
  connectionForm.pricePlan = row.pricePlan
  connectionForm.connected = row.connected
  showConnectionDialog.value = true
}

// 保存连接
const handleSaveConnection = () => {
  if (!connectionForm.roomType.trim()) {
    ElMessage.warning('请选择房型')
    return
  }

  if (!connectionForm.pricePlan.trim()) {
    ElMessage.warning('请输入价格计划')
    return
  }

  if (editingConnection.value) {
    // 更新现有连接
    const connection = connections.value.find((c) => c.id === editingConnection.value!.id)
    if (connection) {
      connection.roomType = connectionForm.roomType
      connection.pricePlan = connectionForm.pricePlan
      connection.connected = connectionForm.connected
      connection.lastSyncTime = new Date().toLocaleString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit',
        hour12: false,
      })
      ElMessage.success('更新成功')
    }
  } else {
    // 添加新连接
    const newConnection: PriceConnection = {
      id: Date.now(),
      roomType: connectionForm.roomType,
      pricePlan: connectionForm.pricePlan,
      connected: connectionForm.connected,
      lastSyncTime: connectionForm.connected
        ? new Date().toLocaleString('zh-CN', {
            year: 'numeric',
            month: '2-digit',
            day: '2-digit',
            hour: '2-digit',
            minute: '2-digit',
            second: '2-digit',
            hour12: false,
          })
        : '-',
    }
    connections.value.push(newConnection)
    ElMessage.success('添加成功')
  }

  showConnectionDialog.value = false
}

// 同步价格
const handleSync = (row: PriceConnection) => {
  ElMessage.info(`正在同步 "${row.roomType}" 的价格...`)
  // 模拟同步
  setTimeout(() => {
    row.lastSyncTime = new Date().toLocaleString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
      hour12: false,
    })
    ElMessage.success('同步成功')
  }, 1000)
}

// 删除连接
const handleDelete = (row: PriceConnection) => {
  ElMessageBox.confirm(`确定要删除 "${row.roomType}" 的连接配置吗?`, '删除确认', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  })
    .then(() => {
      const index = connections.value.findIndex((c) => c.id === row.id)
      if (index !== -1) {
        connections.value.splice(index, 1)
        ElMessage.success('删除成功')
      }
    })
    .catch(() => {
      // 取消删除
    })
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

/* 说明区域 */
.instructions-banner {
  background: #e6f7ff;
  border: 1px solid #91d5ff;
  border-radius: 4px;
  padding: 16px 20px;
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

/* 邮箱验证区域 */
.email-verification-section {
  background: #f5f7fa;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  padding: 20px 24px;
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

.verification-input-group {
  display: flex;
  gap: 12px;
  align-items: center;
  flex: 1;
  max-width: 500px;
}

.email-input {
  flex: 1;
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

:deep(.el-button + .el-button) {
  margin-left: 8px;
}

/* 对话框 */
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
