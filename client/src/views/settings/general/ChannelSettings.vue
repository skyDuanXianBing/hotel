<template>
  <div class="channel-settings-container" v-loading="loading">
    <!-- 顶部提示信息 -->
    <div class="notice-section">
      <div class="notice-item">
        <span class="notice-number">1.</span>
        <span class="notice-text">{{ t('settings.generalChannel.notices.defaultChannels') }}</span>
      </div>
      <div class="notice-item">
        <span class="notice-number">2.</span>
        <span class="notice-text">{{ t('settings.generalChannel.notices.presetProtected') }}</span>
      </div>
    </div>

    <!-- 可用渠道区域 -->
    <div class="section">
      <div class="section-header">
        <div class="section-title-wrapper">
          <h3 class="section-title">{{ t('settings.generalChannel.available') }}</h3>
          <span class="section-subtitle">{{ t('settings.generalChannel.availableDesc') }}</span>
        </div>
        <div class="section-actions">
          <el-checkbox :model-value="selectAllChannels" @change="handleToggleSelectAll">
            {{ t('settings.generalChannel.selectAll') }}
          </el-checkbox>
          <el-button type="danger" plain :disabled="selectedCount === 0" @click="handleDeleteSelected">
            {{ t('settings.generalChannel.deleteSelected', { count: selectedCount }) }}
          </el-button>
          <el-button type="primary" @click="handleAddChannel">
            {{ t('settings.generalChannel.add') }}
          </el-button>
        </div>
      </div>

      <div class="channels-grid">
        <!-- 新增渠道输入框 -->
        <div v-if="showChannelInput" class="channel-item editing">
          <el-input
            v-model="newChannelName"
            :placeholder="t('settings.generalChannel.namePlaceholder')"
            maxlength="20"
            @keyup.enter="handleSaveNewChannel"
          />
          <div class="item-actions">
            <el-button
              link
              type="success"
              :icon="Check"
              @click="handleSaveNewChannel"
            />
            <el-button
              link
              type="danger"
              :icon="Close"
              @click="handleCancelNewChannel"
            />
          </div>
        </div>

        <!-- 渠道卡片列表 -->
        <div
          v-for="channel in availableChannels"
          :key="channel.id"
          class="channel-item"
          :class="[`channel-${channel.color}`]"
        >
          <el-checkbox
            class="item-checkbox"
            :model-value="selectedChannelIds.includes(channel.id)"
            :disabled="isProtectedChannel(channel)"
            @change="(checked: boolean) => toggleChannelSelected(channel.id, checked)"
          />
          <el-button
            v-if="!isProtectedChannel(channel)"
            class="item-delete"
            type="danger"
            link
            :icon="Delete"
            @click="handleDeleteChannel(channel)"
          />
          <el-icon class="drag-icon"><Menu /></el-icon>
          <div class="channel-color-bar" :style="{ backgroundColor: channel.colorHex }"></div>
          <span class="channel-name">{{ channel.name }}</span>
        </div>
      </div>
    </div>

  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { Check, Close, Menu, Delete } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getAllChannels,
  createChannel,
  deleteChannel as deleteChannelApi,
  type ChannelDTO,
  type CreateChannelRequest,
} from '@/api/channel'

const { t } = useI18n()

interface Channel {
  id: number
  name: string
  code: string
  color: string
  colorHex: string
  enabled: boolean
}

// 新增渠道相关
const showChannelInput = ref(false)
const newChannelName = ref('')

// 可用渠道列表
const availableChannels = ref<Channel[]>([])

const selectedChannelIds = ref<number[]>([])
const selectAllChannels = ref(false)
const loading = ref(false)
const selectedCount = computed(() => selectedChannelIds.value.length)
const PROTECTED_CHANNEL_CODES = new Set(['AIRBNB', 'BOOKING', 'DIRECT'])

const isProtectedChannel = (channel: Channel): boolean => {
  return PROTECTED_CHANNEL_CODES.has((channel.code || '').toUpperCase())
}

const getAllChannelIds = (): number[] => {
  return availableChannels.value
    .filter((channel) => !isProtectedChannel(channel))
    .map((channel) => channel.id)
}

const syncSelectAllState = () => {
  const allIds = getAllChannelIds()
  if (allIds.length === 0) {
    selectAllChannels.value = false
    return
  }
  selectAllChannels.value = allIds.every((id) => selectedChannelIds.value.includes(id))
}

const handleToggleSelectAll = (checked: boolean | string | number) => {
  if (checked === true) {
    selectedChannelIds.value = getAllChannelIds()
    return
  }
  selectedChannelIds.value = []
}

const toggleChannelSelected = (channelId: number, checked: boolean | string | number) => {
  if (checked === true) {
    if (!selectedChannelIds.value.includes(channelId)) {
      selectedChannelIds.value.push(channelId)
    }
  } else {
    selectedChannelIds.value = selectedChannelIds.value.filter((id) => id !== channelId)
  }
  syncSelectAllState()
}

// 加载渠道数据
const loadChannels = async () => {
  try {
    loading.value = true
    const response = await getAllChannels()
    if (response.success && response.data) {
      // 将后端数据映射到前端格式
      const channels = response.data.map((channel: ChannelDTO) => ({
        id: channel.id,
        name: channel.name,
        code: channel.code,
        color: getColorName(channel.color),
        colorHex: channel.color,
        enabled: channel.enabled,
      }))

      // 移除“购藏渠道”后统一在同一列表展示
      availableChannels.value = channels
      const idSet = new Set(getAllChannelIds())
      selectedChannelIds.value = selectedChannelIds.value.filter((id) => idSet.has(id))
      syncSelectAllState()
    }
  } catch (error) {
    console.error('加载渠道数据失败:', error)
    ElMessage.error(t('settings.generalChannel.messages.loadFailed'))
  } finally {
    loading.value = false
  }
}

// 根据颜色值获取颜色名称
const getColorName = (colorHex: string): string => {
  const colorMap: Record<string, string> = {
    '#409EFF': 'blue',
    '#1890FF': 'blue',
    '#F56C6C': 'red',
    '#E6A23C': 'orange',
    '#303133': 'navy',
    '#003580': 'navy',
    '#00C1DE': 'cyan',
    '#67C23A': 'green',
    '#FFB800': 'orange',
    '#FF6A00': 'orange',
  }
  return colorMap[colorHex] || 'blue'
}

// 新增渠道
const handleAddChannel = () => {
  showChannelInput.value = true
  newChannelName.value = ''
}

// 保存新增渠道
const handleSaveNewChannel = async () => {
  if (!newChannelName.value.trim()) {
    ElMessage.warning(t('settings.generalChannel.messages.nameRequired'))
    return
  }

  try {
    const requestData: CreateChannelRequest = {
      name: newChannelName.value.trim(),
      code: newChannelName.value.trim().toUpperCase().replace(/\s+/g, '_'),
      type: 'OTA',
      color: '#409EFF',
      enabled: true,
      description: '',
    }

    const response = await createChannel(requestData)
    if (response.success && response.data) {
      // 重新加载渠道列表
      await loadChannels()
      showChannelInput.value = false
      newChannelName.value = ''
      ElMessage.success(t('settings.generalChannel.messages.addSuccess'))
    }
  } catch (error) {
    console.error('新增渠道失败:', error)
    ElMessage.error(t('settings.generalChannel.messages.addFailed'))
  }
}

// 取消新增渠道
const handleCancelNewChannel = () => {
  showChannelInput.value = false
  newChannelName.value = ''
}

const handleDeleteChannel = async (channel: Channel) => {
  if (isProtectedChannel(channel)) {
    ElMessage.warning(t('settings.generalChannel.messages.protectedCannotDelete'))
    return
  }

  try {
    await ElMessageBox.confirm(
      t('settings.generalChannel.messages.deleteConfirm', { name: channel.name }),
      t('settings.common.deleteConfirmTitle'),
      {
        confirmButtonText: t('settings.common.confirmButton'),
        cancelButtonText: t('settings.common.cancelButton'),
        type: 'warning',
      },
    )

    const response = await deleteChannelApi(channel.id)
    if (!response.success) {
      ElMessage.error(response.message || t('settings.generalChannel.messages.deleteFailed'))
      return
    }

    selectedChannelIds.value = selectedChannelIds.value.filter((id) => id !== channel.id)
    ElMessage.success(t('settings.generalChannel.messages.deleteSuccess'))
    await loadChannels()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除渠道失败:', error)
      ElMessage.error(t('settings.generalChannel.messages.deleteFailed'))
    }
  }
}

const handleDeleteSelected = async () => {
  if (selectedChannelIds.value.length === 0) {
    return
  }

  const selectedChannels = availableChannels.value.filter((channel) =>
    selectedChannelIds.value.includes(channel.id),
  )
  const deletableChannels = selectedChannels.filter((channel) => !isProtectedChannel(channel))

  if (deletableChannels.length === 0) {
    ElMessage.warning(t('settings.generalChannel.messages.selectedProtectedCannotDelete'))
    return
  }

  try {
    await ElMessageBox.confirm(
      t('settings.generalChannel.messages.batchDeleteConfirm', { count: deletableChannels.length }),
      t('settings.common.batchDeleteConfirmTitle'),
      {
        confirmButtonText: t('settings.common.confirmButton'),
        cancelButtonText: t('settings.common.cancelButton'),
        type: 'warning',
      },
    )

    const results = await Promise.allSettled(
      deletableChannels.map(async (channel) => {
        const response = await deleteChannelApi(channel.id)
        if (!response.success) {
          throw new Error(
            response.message ||
              t('settings.generalChannel.messages.batchDeleteItemFailed', { name: channel.name }),
          )
        }
        return channel.name
      }),
    )

    const successCount = results.filter((item) => item.status === 'fulfilled').length
    const failedCount = results.length - successCount

    await loadChannels()

    if (failedCount === 0) {
      ElMessage.success(t('settings.generalChannel.messages.batchDeleteSuccess', { count: successCount }))
    } else {
      ElMessage.warning(
        t('settings.generalChannel.messages.batchDeletePartial', { successCount, failedCount }),
      )
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('批量删除渠道失败:', error)
      ElMessage.error(t('settings.generalChannel.messages.batchDeleteFailed'))
    }
  }
}

// 组件挂载时加载数据
onMounted(() => {
  loadChannels()
})
</script>

<style scoped>
.channel-settings-container {
  padding: 20px;
  background: #fff;
  min-height: calc(100vh - 100px);
}

/* 顶部提示信息 */
.notice-section {
  background: #e6f7ff;
  border: 1px solid #91d5ff;
  border-radius: 4px;
  padding: 12px 16px;
  margin-bottom: 24px;
}

.notice-item {
  display: flex;
  align-items: flex-start;
  line-height: 1.6;
  color: #1890ff;
  font-size: 14px;
}

.notice-item + .notice-item {
  margin-top: 4px;
}

.notice-number {
  flex-shrink: 0;
  margin-right: 4px;
}

.notice-text {
  flex: 1;
}

/* 区块样式 */
.section {
  margin-bottom: 32px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.section-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.section-title-wrapper {
  display: flex;
  align-items: baseline;
  gap: 12px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

.section-subtitle {
  font-size: 13px;
  color: #909399;
}

/* 渠道网格 */
.channels-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 16px;
}

.channel-item {
  position: relative;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  padding-left: 38px;
  padding-right: 40px;
  background: #f5f7fa;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  cursor: move;
  transition: all 0.2s;
  min-height: 48px;
}

.channel-item:hover {
  border-color: #409eff;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.1);
}

.channel-item.editing {
  background: #fff;
  padding: 8px 12px;
  cursor: default;
}

.drag-icon {
  flex-shrink: 0;
  font-size: 18px;
  color: #909399;
  cursor: move;
}

.item-checkbox {
  position: absolute;
  left: 10px;
  top: 50%;
  transform: translateY(-50%);
}

.item-delete {
  position: absolute;
  right: 8px;
  top: 50%;
  transform: translateY(-50%);
}

.channel-color-bar {
  flex-shrink: 0;
  width: 4px;
  height: 24px;
  border-radius: 2px;
}

.channel-name {
  flex: 1;
  font-size: 14px;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.item-actions {
  display: flex;
  gap: 4px;
  flex-shrink: 0;
}

.empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100px;
  background: #fafafa;
  border: 1px dashed #d9d9d9;
  border-radius: 4px;
}

.empty-text {
  font-size: 14px;
  color: #909399;
  margin: 0;
}

/* 按钮样式调整 */
:deep(.el-button.is-link) {
  padding: 4px;
  font-size: 18px;
}

:deep(.el-input__wrapper) {
  padding: 8px 12px;
}

.channel-item.editing :deep(.el-input) {
  flex: 1;
}

/* 渠道颜色变体 */
.channel-blue {
  border-left: 3px solid #409eff;
}

.channel-red {
  border-left: 3px solid #f56c6c;
}

.channel-orange {
  border-left: 3px solid #e6a23c;
}

.channel-navy {
  border-left: 3px solid #303133;
}

.channel-cyan {
  border-left: 3px solid #409eff;
}

.channel-green {
  border-left: 3px solid #67c23a;
}
</style>
