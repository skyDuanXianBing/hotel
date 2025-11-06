<template>
  <div class="housekeeper-list">
    <!-- 提示信息栏 -->
    <div v-if="showInfoBanner" class="info-banner">
      <div class="info-content">
        <div class="info-item">
          <span class="info-number">1.</span>
          <span class="info-text">「保洁员列表」为独立功能，和PMS中已有「账号列表」功能无关。</span>
        </div>
        <div class="info-item">
          <span class="info-number">2.</span>
          <span class="info-text">保洁员可通过关注微信公众号「PMS房务管家」，接收分派的保洁任务。</span>
        </div>
      </div>
      <el-button type="text" class="close-btn" @click="closeInfoBanner">
        <el-icon><Close /></el-icon>
      </el-button>
    </div>

    <!-- 搜索和操作栏 -->
    <div class="search-toolbar">
      <el-input
        v-model="searchQuery"
        placeholder="搜索保洁员姓名、保洁员手机号"
        class="search-input"
        clearable
      >
        <template #suffix>
          <el-icon class="search-icon"><Search /></el-icon>
        </template>
      </el-input>

      <el-button type="primary" @click="handleAdd">添加保洁员</el-button>
    </div>

    <!-- 保洁员表格 -->
    <el-table :data="housekeeperList" style="width: 100%" empty-text="暂无数据">
      <el-table-column prop="name" label="保洁员姓名" width="180" />
      <el-table-column prop="phone" label="保洁员手机号" width="180" />
      <el-table-column prop="associatedRooms" label="关联房间" min-width="200" />
      <el-table-column prop="wechatNickname" label="微信昵称" width="150" />
      <el-table-column label="状态" width="120" align="center">
        <template #default="{ row }">
          <el-switch
            v-model="row.enabled"
            active-text="启用"
            @change="handleStatusChange(row)"
          />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150" align="center">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
          <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div class="pagination-container">
      <el-pagination
        v-model:current-page="pagination.current"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        :page-sizes="[25, 50, 100]"
        layout="total, sizes, prev, pager, next"
        @current-change="handlePageChange"
        @size-change="handleSizeChange"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Close } from '@element-plus/icons-vue'

// 提示信息显示状态
const showInfoBanner = ref(true)

// 搜索关键字
const searchQuery = ref('')

// 保洁员列表数据
const housekeeperList = ref<any[]>([
  {
    id: 1,
    name: '小林',
    phone: '12345678901',
    associatedRooms: '大床房-a01',
    wechatNickname: '昵',
    enabled: true
  },
  {
    id: 2,
    name: 'll',
    phone: '13423456782',
    associatedRooms: '大床房-a01',
    wechatNickname: '绑定微信',
    enabled: true
  }
])

// 分页
const pagination = ref({
  current: 1,
  size: 25,
  total: 2
})

// 关闭提示信息
const closeInfoBanner = () => {
  showInfoBanner.value = false
}

// 添加保洁员
const handleAdd = () => {
  ElMessage.info('添加保洁员功能')
}

// 编辑保洁员
const handleEdit = (row: any) => {
  ElMessage.info(`编辑保洁员：${row.name}`)
}

// 删除保洁员
const handleDelete = async (row: any) => {
  try {
    await ElMessageBox.confirm(`确定要删除保洁员"${row.name}"吗？`, '确认删除', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    ElMessage.success('删除成功')
  } catch {
    // 取消删除
  }
}

// 状态变更
const handleStatusChange = (row: any) => {
  const status = row.enabled ? '启用' : '禁用'
  ElMessage.success(`已${status}保洁员：${row.name}`)
}

// 分页事件
const handlePageChange = (page: number) => {
  pagination.value.current = page
}

const handleSizeChange = (size: number) => {
  pagination.value.size = size
}
</script>

<style scoped>
.housekeeper-list {
  padding: 20px;
  background: #fff;
}

/* 提示信息栏 */
.info-banner {
  background: #e6f4ff;
  border: 1px solid #91caff;
  border-radius: 4px;
  padding: 16px 20px;
  margin-bottom: 20px;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.info-content {
  flex: 1;
}

.info-item {
  display: flex;
  margin-bottom: 8px;
  font-size: 14px;
  color: #333;
}

.info-item:last-child {
  margin-bottom: 0;
}

.info-number {
  margin-right: 4px;
  font-weight: 500;
}

.info-text {
  flex: 1;
}

.close-btn {
  padding: 0;
  min-height: auto;
  color: #666;
}

.close-btn:hover {
  color: #333;
}

/* 搜索和操作栏 */
.search-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.search-input {
  width: 400px;
}

/* 分页容器 */
.pagination-container {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

/* 响应式 */
@media (max-width: 768px) {
  .search-toolbar {
    flex-direction: column;
    gap: 12px;
    align-items: stretch;
  }

  .search-input {
    width: 100%;
  }
}
</style>
