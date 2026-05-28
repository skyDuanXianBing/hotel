<template>
  <div class="housekeeper-list">
    <div v-if="showInfoBanner" class="info-banner">
      <div class="info-content">
        <div class="info-item">
          <span class="info-number">1.</span>
          <span class="info-text">{{ t('pages.housekeeperList.info.first') }}</span>
        </div>
        <div class="info-item">
          <span class="info-number">2.</span>
          <span class="info-text">{{ t('pages.housekeeperList.info.second') }}</span>
        </div>
      </div>
      <el-button type="text" class="close-btn" @click="closeInfoBanner">
        <el-icon><Close /></el-icon>
      </el-button>
    </div>

    <div class="search-toolbar">
      <el-input
        v-model="searchQuery"
        :placeholder="t('pages.housekeeperList.searchPlaceholder')"
        class="search-input"
        clearable
      >
        <template #suffix>
          <el-icon class="search-icon"><Search /></el-icon>
        </template>
      </el-input>

      <el-button type="primary" @click="handleAdd">
        {{ t('pages.housekeeperList.add') }}
      </el-button>
    </div>

    <el-table
      :data="housekeeperList"
      style="width: 100%"
      :empty-text="t('pages.housekeeperList.empty')"
    >
      <el-table-column prop="name" :label="t('pages.housekeeperList.columns.name')" width="180" />
      <el-table-column prop="phone" :label="t('pages.housekeeperList.columns.phone')" width="180" />
      <el-table-column
        prop="associatedRooms"
        :label="t('pages.housekeeperList.columns.associatedRooms')"
        min-width="200"
      />
      <el-table-column
        prop="wechatNickname"
        :label="t('pages.housekeeperList.columns.wechatNickname')"
        width="150"
      />
      <el-table-column :label="t('pages.housekeeperList.columns.status')" width="120" align="center">
        <template #default="{ row }">
          <el-switch
            v-model="row.enabled"
            :active-text="t('pages.housekeeperList.status.enabled')"
            @change="handleStatusChange(row)"
          />
        </template>
      </el-table-column>
      <el-table-column :label="t('pages.housekeeperList.columns.actions')" width="150" align="center">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="handleEdit(row)">
            {{ t('pages.housekeeperList.actions.edit') }}
          </el-button>
          <el-button link type="danger" size="small" @click="handleDelete(row)">
            {{ t('pages.housekeeperList.actions.delete') }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>

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
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Close, Search } from '@element-plus/icons-vue'

const { t } = useI18n()

const showInfoBanner = ref(true)
const searchQuery = ref('')

const housekeeperList = ref<any[]>([
  {
    id: 1,
    name: t('pages.housekeeperList.mock.nameA'),
    phone: '12345678901',
    associatedRooms: t('pages.housekeeperList.mock.associatedRooms'),
    wechatNickname: t('pages.housekeeperList.mock.nicknameUnknown'),
    enabled: true,
  },
  {
    id: 2,
    name: t('pages.housekeeperList.mock.nameB'),
    phone: '13423456782',
    associatedRooms: t('pages.housekeeperList.mock.associatedRooms'),
    wechatNickname: t('pages.housekeeperList.mock.nicknameBound'),
    enabled: true,
  },
])

const pagination = ref({
  current: 1,
  size: 25,
  total: 2,
})

const closeInfoBanner = () => {
  showInfoBanner.value = false
}

const handleAdd = () => {
  ElMessage.info(t('pages.housekeeperList.messages.addPending'))
}

const handleEdit = (row: any) => {
  ElMessage.info(t('pages.housekeeperList.messages.editPending', { name: row.name }))
}

const handleDelete = async (row: any) => {
  try {
    await ElMessageBox.confirm(
      t('pages.housekeeperList.messages.deleteConfirm', { name: row.name }),
      t('pages.housekeeperList.messages.deleteTitle'),
      {
        confirmButtonText: t('pages.housekeeperList.messages.confirm'),
        cancelButtonText: t('pages.housekeeperList.messages.cancel'),
        type: 'warning',
      },
    )
    ElMessage.success(t('pages.housekeeperList.messages.deleteSuccess'))
  } catch {
    // no-op
  }
}

const handleStatusChange = (row: any) => {
  const status = row.enabled
    ? t('pages.housekeeperList.status.enabled')
    : t('pages.housekeeperList.status.disabled')
  ElMessage.success(t('pages.housekeeperList.messages.statusChanged', { name: row.name, status }))
}

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

.search-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.search-input {
  width: 400px;
}

.pagination-container {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

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
