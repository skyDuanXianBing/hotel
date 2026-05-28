<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete } from '@element-plus/icons-vue'
import {
  getAllChannels,
  createChannel,
  updateChannel,
  deleteChannel,
  type ChannelDTO,
  type CreateChannelRequest,
} from '@/api/channel'

const { t } = useI18n()
const loading = ref(false)
const channels = ref<ChannelDTO[]>([])
const selectedChannelIds = ref<number[]>([])
const selectAllChannels = ref(false)
const showChannelDialog = ref(false)
const channelForm = ref<CreateChannelRequest>({
  name: '',
  code: '',
  type: 'OTA',
  color: '#409EFF',
  enabled: true,
  description: '',
})
const editingChannelId = ref<number | null>(null)
const isEditing = ref(false)
const selectedCount = computed(() => selectedChannelIds.value.length)
const enabledChannels = computed(() => channels.value.filter((channel) => channel.enabled))
const disabledChannels = computed(() => channels.value.filter((channel) => !channel.enabled))

// 预定义颜色选项
const colorOptions = [
  '#409EFF',
  '#67C23A',
  '#E6A23C',
  '#F56C6C',
  '#909399',
  '#9B59B6',
  '#1ABC9C',
  '#F39C12',
  '#E74C3C',
  '#3498DB',
  '#2ECC71',
  '#95A5A6',
]

// 渠道类型选项
const typeOptions = computed(() => [
  { label: t('settings.channelSettings.types.OTA'), value: 'OTA' },
  { label: t('settings.channelSettings.types.DIRECT'), value: 'DIRECT' },
  { label: t('settings.channelSettings.types.TRAVEL_AGENCY'), value: 'TRAVEL_AGENCY' },
  { label: t('settings.channelSettings.types.CORPORATE'), value: 'CORPORATE' },
])

// 加载渠道列表
const loadChannels = async () => {
  try {
    loading.value = true
    const response = (await getAllChannels()) as any
    if (response.success && Array.isArray(response.data)) {
      channels.value = response.data
    } else {
      channels.value = []
      ElMessage.error(response.message || t('settings.channelSettings.messages.loadFailed'))
    }
  } catch (error) {
    console.error('加载渠道失败:', error)
    channels.value = []
    ElMessage.error(t('settings.channelSettings.messages.loadFailedWithHint'))
  } finally {
    const currentIds = new Set(channels.value.map((channel) => channel.id))
    selectedChannelIds.value = selectedChannelIds.value.filter((id) => currentIds.has(id))
    syncSelectAllState()
    loading.value = false
  }
}

const syncSelectAllState = () => {
  const total = channels.value.length
  if (!total) {
    selectAllChannels.value = false
    return
  }
  selectAllChannels.value = channels.value.every((channel) => selectedChannelIds.value.includes(channel.id))
}

const handleToggleSelectAll = (checked: boolean) => {
  if (checked) {
    selectedChannelIds.value = channels.value.map((channel) => channel.id)
    return
  }
  selectedChannelIds.value = []
}

const toggleChannelSelected = (channelId: number, checked: boolean) => {
  if (checked) {
    if (!selectedChannelIds.value.includes(channelId)) {
      selectedChannelIds.value.push(channelId)
    }
  } else {
    selectedChannelIds.value = selectedChannelIds.value.filter((id) => id !== channelId)
  }
  syncSelectAllState()
}

// 打开新增渠道对话框
const openAddDialog = () => {
  isEditing.value = false
  editingChannelId.value = null
  channelForm.value = {
    name: '',
    code: '',
    type: 'OTA',
    color: '#409EFF',
    enabled: true,
    description: '',
  }
  showChannelDialog.value = true
}

// 打开编辑渠道对话框
const openEditDialog = (channel: ChannelDTO) => {
  isEditing.value = true
  editingChannelId.value = channel.id
  channelForm.value = {
    name: channel.name,
    code: channel.code,
    type: channel.type,
    color: channel.color,
    enabled: channel.enabled,
    description: channel.description || '',
  }
  showChannelDialog.value = true
}

// 保存渠道
const saveChannel = async () => {
  try {
    loading.value = true
    const response = isEditing.value
      ? ((await updateChannel(editingChannelId.value!, channelForm.value)) as any)
      : ((await createChannel(channelForm.value)) as any)

    if (response.success) {
      ElMessage.success(
        isEditing.value
          ? t('settings.channelSettings.messages.updateSuccess')
          : t('settings.channelSettings.messages.createSuccess'),
      )
      showChannelDialog.value = false
      await loadChannels()
    } else {
      ElMessage.error(response.message || t('settings.common.operationFailed'))
    }
  } catch (error) {
    console.error('保存渠道失败:', error)
    ElMessage.error(t('settings.common.operationFailedRetry'))
  } finally {
    loading.value = false
  }
}

// 删除渠道
const handleDeleteChannel = async (channel: ChannelDTO) => {
  try {
    await ElMessageBox.confirm(t('settings.channelSettings.messages.deleteConfirm', { name: channel.name }), t('settings.common.deleteConfirmTitle'), {
      confirmButtonText: t('settings.common.confirmButton'),
      cancelButtonText: t('settings.common.cancelButton'),
      type: 'warning',
    })

    loading.value = true
    const response = (await deleteChannel(channel.id)) as any
    if (response.success) {
      selectedChannelIds.value = selectedChannelIds.value.filter((id) => id !== channel.id)
      ElMessage.success(t('settings.common.deleteSuccess'))
      await loadChannels()
    } else {
      ElMessage.error(response.message || t('settings.common.deleteFailed'))
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('删除渠道失败:', error)
      ElMessage.error(t('settings.common.operationFailedRetry'))
    }
  } finally {
    loading.value = false
  }
}

const handleBatchDelete = async () => {
  if (selectedCount.value === 0) {
    return
  }

  const selectedChannels = channels.value.filter((channel) => selectedChannelIds.value.includes(channel.id))
  try {
    await ElMessageBox.confirm(t('settings.channelSettings.messages.batchDeleteConfirm', { count: selectedChannels.length }), t('settings.common.batchDeleteConfirmTitle'), {
      confirmButtonText: t('settings.common.confirmButton'),
      cancelButtonText: t('settings.common.cancelButton'),
      type: 'warning',
    })

    loading.value = true
    const deleteResults = await Promise.allSettled(
      selectedChannels.map(async (channel) => {
        const response = await deleteChannel(channel.id)
        if (!response.success) {
          throw new Error(
            response.message ||
              t('settings.channelSettings.messages.deleteChannelFailed', { name: channel.name }),
          )
        }
        return channel.name
      })
    )

    const successCount = deleteResults.filter((item) => item.status === 'fulfilled').length
    const failedChannels: string[] = []
    deleteResults.forEach((item, index) => {
      if (item.status === 'rejected') {
        failedChannels.push(selectedChannels[index]?.name || t('settings.common.none'))
      }
    })

    await loadChannels()

    if (failedChannels.length === 0) {
      ElMessage.success(t('settings.channelSettings.messages.batchDeleteSuccess', { count: successCount }))
      return
    }

    ElMessage.warning(
      t('settings.channelSettings.messages.batchDeletePartial', {
        successCount,
        failedCount: failedChannels.length,
      }),
    )
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('批量删除渠道失败:', error)
      ElMessage.error(t('settings.channelSettings.messages.batchDeleteFailedRetry'))
    }
  } finally {
    loading.value = false
  }
}

// 获取类型显示文本
const getTypeText = (type: string) => {
  const typeKey = `settings.channelSettings.types.${type}`
  const translated = t(typeKey)
  return translated === typeKey ? type : translated
}

onMounted(() => {
  loadChannels()
})
</script>

<template>
  <div class="channel-settings">
    <div class="page-header">
      <div class="header-left">
        <h3>{{ t('settings.channelSettings.title') }}</h3>
        <div class="help-info">
          <p>1. {{ t('settings.channelSettings.tips.usage') }}</p>
          <p>2. {{ t('settings.channelSettings.tips.preset') }}</p>
        </div>
      </div>
      <div class="header-right">
        <el-checkbox v-model="selectAllChannels" @change="handleToggleSelectAll">
          {{ t('settings.channelSettings.selectAll') }}
        </el-checkbox>
        <el-button type="danger" plain :disabled="selectedCount === 0" @click="handleBatchDelete">
          {{ t('settings.channelSettings.deleteSelected', { count: selectedCount }) }}
        </el-button>
        <el-button type="primary" :icon="Plus" @click="openAddDialog">
          {{ t('settings.channelSettings.add') }}
        </el-button>
      </div>
    </div>

    <div class="content-area">
      <div class="section">
        <h4 class="section-title">{{ t('settings.channelSettings.available') }}</h4>
        <p class="section-desc">{{ t('settings.channelSettings.availableDesc') }}</p>

        <div class="channels-grid">
          <div
            v-for="channel in enabledChannels"
            :key="channel.id"
            class="channel-card"
            :style="{ borderColor: channel.color }"
          >
            <el-checkbox
              class="channel-checkbox"
              :model-value="selectedChannelIds.includes(channel.id)"
              @change="(checked: boolean) => toggleChannelSelected(channel.id, checked)"
            />
            <div class="channel-content">
              <div class="channel-color" :style="{ backgroundColor: channel.color }"></div>
              <div class="channel-name">{{ channel.name }}</div>
              <div class="channel-type">{{ getTypeText(channel.type) }}</div>
            </div>
            <div class="channel-actions">
              <el-button size="small" :icon="Edit" @click="openEditDialog(channel)" circle />
              <el-button
                size="small"
                :icon="Delete"
                type="danger"
                @click="handleDeleteChannel(channel)"
                circle
              />
            </div>
            <div class="channel-corner"></div>
          </div>
        </div>
      </div>

      <div class="section" v-if="disabledChannels.length > 0">
        <h4 class="section-title">{{ t('settings.channelSettings.disabled') }}</h4>
        <p class="section-desc">{{ t('settings.channelSettings.disabledDesc') }}</p>

        <div class="channels-grid">
          <div
            v-for="channel in disabledChannels"
            :key="channel.id"
            class="channel-card disabled"
            :style="{ borderColor: channel.color }"
          >
            <el-checkbox
              class="channel-checkbox"
              :model-value="selectedChannelIds.includes(channel.id)"
              @change="(checked: boolean) => toggleChannelSelected(channel.id, checked)"
            />
            <div class="channel-content">
              <div class="channel-color" :style="{ backgroundColor: channel.color }"></div>
              <div class="channel-name">{{ channel.name }}</div>
              <div class="channel-type">{{ getTypeText(channel.type) }}</div>
            </div>
            <div class="channel-actions">
              <el-button size="small" :icon="Edit" @click="openEditDialog(channel)" circle />
              <el-button
                size="small"
                :icon="Delete"
                type="danger"
                @click="handleDeleteChannel(channel)"
                circle
              />
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 渠道编辑对话框 -->
    <el-dialog
      v-model="showChannelDialog"
      :title="
        isEditing
          ? t('settings.channelSettings.dialogTitle.edit')
          : t('settings.channelSettings.dialogTitle.add')
      "
      width="500px"
    >
      <el-form :model="channelForm" label-width="80px">
        <el-form-item :label="t('settings.channelSettings.fields.name')" required>
          <el-input v-model="channelForm.name" :placeholder="t('settings.channelSettings.placeholders.name')" />
        </el-form-item>
        <el-form-item :label="t('settings.channelSettings.fields.code')" required>
          <el-input v-model="channelForm.code" :placeholder="t('settings.channelSettings.placeholders.code')" />
        </el-form-item>
        <el-form-item :label="t('settings.channelSettings.fields.type')" required>
          <el-select v-model="channelForm.type" :placeholder="t('settings.channelSettings.placeholders.type')">
            <el-option
              v-for="option in typeOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('settings.channelSettings.fields.color')" required>
          <div class="color-picker-container">
            <el-color-picker v-model="channelForm.color" />
            <div class="color-options">
              <div
                v-for="color in colorOptions"
                :key="color"
                class="color-option"
                :class="{ active: channelForm.color === color }"
                :style="{ backgroundColor: color }"
                @click="channelForm.color = color"
              ></div>
            </div>
          </div>
        </el-form-item>
        <el-form-item :label="t('settings.channelSettings.fields.status')">
          <el-switch
            v-model="channelForm.enabled"
            :active-text="t('settings.common.enabled')"
            :inactive-text="t('settings.common.disabled')"
          />
        </el-form-item>
        <el-form-item :label="t('settings.channelSettings.fields.remark')">
          <el-input
            v-model="channelForm.description"
            type="textarea"
            :placeholder="t('settings.channelSettings.placeholders.remark')"
            :rows="3"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showChannelDialog = false">{{ t('settings.common.cancel') }}</el-button>
          <el-button type="primary" @click="saveChannel" :loading="loading">
            {{ isEditing ? t('settings.common.update') : t('settings.common.create') }}
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.channel-settings {
  padding: 20px;
  background-color: #f5f5f5;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
  background: white;
  padding: 16px 20px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.header-left h3 {
  margin: 0 0 12px 0;
  color: #333;
  font-size: 20px;
  font-weight: 500;
}

.help-info {
  color: #666;
  font-size: 13px;
  line-height: 1.4;
}

.help-info p {
  margin: 4px 0;
}

.header-right {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 12px;
}

.content-area {
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.section {
  padding: 20px;
}

.section + .section {
  border-top: 1px solid #f0f0f0;
}

.section-title {
  margin: 0 0 8px 0;
  color: #333;
  font-size: 16px;
  font-weight: 500;
}

.section-desc {
  margin: 0 0 20px 0;
  color: #666;
  font-size: 13px;
}

.channels-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 16px;
}

.channel-card {
  position: relative;
  background: white;
  border: 2px solid;
  border-radius: 8px;
  padding: 16px;
  cursor: pointer;
  transition: all 0.3s ease;
  overflow: hidden;
}

.channel-checkbox {
  position: absolute;
  top: 10px;
  left: 10px;
  z-index: 2;
}

.channel-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.channel-card.disabled {
  opacity: 0.6;
  transform: skewX(-5deg);
}

.channel-card.disabled:hover {
  transform: skewX(-5deg) translateY(-2px);
}

.channel-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  margin-bottom: 12px;
}

.channel-color {
  width: 24px;
  height: 24px;
  border-radius: 4px;
  margin-bottom: 8px;
}

.channel-name {
  font-size: 16px;
  font-weight: 500;
  color: #333;
  margin-bottom: 4px;
}

.channel-type {
  font-size: 12px;
  color: #666;
}

.channel-actions {
  display: flex;
  justify-content: center;
  gap: 8px;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.channel-card:hover .channel-actions {
  opacity: 1;
}

.channel-corner {
  position: absolute;
  top: -1px;
  right: -1px;
  width: 0;
  height: 0;
  border-left: 20px solid transparent;
  border-top: 20px solid #409eff;
}

.channel-corner::after {
  content: '';
  position: absolute;
  top: -18px;
  right: -1px;
  width: 0;
  height: 0;
  border-left: 16px solid transparent;
  border-top: 16px solid white;
}

.color-picker-container {
  display: flex;
  align-items: flex-start;
  gap: 16px;
}

.color-options {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 8px;
}

.color-option {
  width: 24px;
  height: 24px;
  border-radius: 4px;
  cursor: pointer;
  border: 2px solid transparent;
  transition: all 0.2s ease;
}

.color-option:hover {
  transform: scale(1.1);
}

.color-option.active {
  border-color: #409eff;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2);
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
