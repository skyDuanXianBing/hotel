<script setup lang="ts">
import { ref, onMounted } from 'vue'
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

const loading = ref(false)
const channels = ref<ChannelDTO[]>([])
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
const typeOptions = [
  { label: '在线旅行社', value: 'OTA' },
  { label: '直销', value: 'DIRECT' },
  { label: '旅行社', value: 'TRAVEL_AGENCY' },
  { label: '企业客户', value: 'CORPORATE' },
]

// 获取模拟数据
const getMockChannels = (): ChannelDTO[] => {
  return [
    {
      id: 1,
      name: '自来客',
      code: 'DIRECT',
      type: 'DIRECT',
      color: '#409EFF',
      enabled: true,
      description: '直接预订客户',
      createdAt: '2025-01-01',
      updatedAt: '2025-01-01',
    },
    {
      id: 2,
      name: '携程',
      code: 'CTRIP',
      type: 'OTA',
      color: '#1890FF',
      enabled: true,
      description: '携程网',
      createdAt: '2025-01-01',
      updatedAt: '2025-01-01',
    },
    {
      id: 3,
      name: '美团',
      code: 'MEITUAN',
      type: 'OTA',
      color: '#FFB800',
      enabled: true,
      description: '美团',
      createdAt: '2025-01-01',
      updatedAt: '2025-01-01',
    },
    {
      id: 4,
      name: '飞猪',
      code: 'FLIGGY',
      type: 'OTA',
      color: '#FF6A00',
      enabled: true,
      description: '飞猪旅行',
      createdAt: '2025-01-01',
      updatedAt: '2025-01-01',
    },
    {
      id: 5,
      name: '去哪儿',
      code: 'QUNAR',
      type: 'OTA',
      color: '#00C1DE',
      enabled: true,
      description: '去哪儿网',
      createdAt: '2025-01-01',
      updatedAt: '2025-01-01',
    },
    {
      id: 6,
      name: 'Booking.com',
      code: 'BOOKING',
      type: 'OTA',
      color: '#003580',
      enabled: true,
      description: 'Booking.com',
      createdAt: '2025-01-01',
      updatedAt: '2025-01-01',
    },
    {
      id: 7,
      name: 'Expedia',
      code: 'EXPEDIA',
      type: 'OTA',
      color: '#FFE135',
      enabled: true,
      description: 'Expedia',
      createdAt: '2025-01-01',
      updatedAt: '2025-01-01',
    },
    {
      id: 8,
      name: 'Agoda',
      code: 'AGODA',
      type: 'OTA',
      color: '#D7A441',
      enabled: true,
      description: 'Agoda',
      createdAt: '2025-01-01',
      updatedAt: '2025-01-01',
    },
    {
      id: 9,
      name: '艺龙',
      code: 'ELONG',
      type: 'OTA',
      color: '#7B68EE',
      enabled: true,
      description: '艺龙网',
      createdAt: '2025-01-01',
      updatedAt: '2025-01-01',
    },
    {
      id: 10,
      name: '马蜂窝',
      code: 'MAFENGWO',
      type: 'OTA',
      color: '#FFD700',
      enabled: true,
      description: '马蜂窝',
      createdAt: '2025-01-01',
      updatedAt: '2025-01-01',
    },
  ]
}

// 加载渠道列表
const loadChannels = async () => {
  try {
    loading.value = true
    const response = (await getAllChannels()) as any
    if (response.success) {
      channels.value = response.data
    } else {
      channels.value = getMockChannels()
    }
  } catch (error) {
    console.error('加载渠道失败:', error)
    channels.value = getMockChannels()
  } finally {
    loading.value = false
  }
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
      ElMessage.success(isEditing.value ? '渠道更新成功' : '渠道创建成功')
      showChannelDialog.value = false
      await loadChannels()
    } else {
      ElMessage.error(response.message || '操作失败')
    }
  } catch (error) {
    console.error('保存渠道失败:', error)
    ElMessage.error('操作失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

// 删除渠道
const handleDeleteChannel = async (channel: ChannelDTO) => {
  try {
    await ElMessageBox.confirm(`确认删除渠道"${channel.name}"吗？`, '删除确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })

    loading.value = true
    const response = (await deleteChannel(channel.id)) as any
    if (response.success) {
      ElMessage.success('删除成功')
      await loadChannels()
    } else {
      ElMessage.error(response.message || '删除失败')
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('删除渠道失败:', error)
      ElMessage.error('删除失败，请稍后重试')
    }
  } finally {
    loading.value = false
  }
}

// 获取类型显示文本
const getTypeText = (type: string) => {
  const typeMap: Record<string, string> = {
    OTA: '在线旅行社',
    DIRECT: '直销',
    TRAVEL_AGENCY: '旅行社',
    CORPORATE: '企业客户',
  }
  return typeMap[type] || type
}

onMounted(() => {
  loadChannels()
})
</script>

<template>
  <div class="channel-settings">
    <div class="page-header">
      <div class="header-left">
        <h3>渠道设置</h3>
        <div class="help-info">
          <p>1. 渠道设为全渠道用，须在制售酒饭店中准确为出来。同时中可以添加各渠道的基础。</p>
          <p>2. 系统预设最不可修改名称，仅支持修改各类型及颜色。</p>
        </div>
      </div>
      <div class="header-right">
        <el-button type="primary" :icon="Plus" @click="openAddDialog"> 新增渠道 </el-button>
      </div>
    </div>

    <div class="content-area">
      <div class="section">
        <h4 class="section-title">可用渠道</h4>
        <p class="section-desc">明亮显示：可用、倾斜：禁用。可定义的颜色。</p>

        <div class="channels-grid">
          <div
            v-for="channel in channels.filter((c) => c.enabled)"
            :key="channel.id"
            class="channel-card"
            :style="{ borderColor: channel.color }"
          >
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

      <div class="section" v-if="channels.filter((c) => !c.enabled).length > 0">
        <h4 class="section-title">停用渠道</h4>
        <p class="section-desc">明亮显示：可用、倾斜：禁用。不影响下单卖房。</p>

        <div class="channels-grid">
          <div
            v-for="channel in channels.filter((c) => !c.enabled)"
            :key="channel.id"
            class="channel-card disabled"
            :style="{ borderColor: channel.color }"
          >
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
      :title="isEditing ? '编辑渠道' : '新增渠道'"
      width="500px"
    >
      <el-form :model="channelForm" label-width="80px">
        <el-form-item label="渠道名称" required>
          <el-input v-model="channelForm.name" placeholder="请输入渠道名称" />
        </el-form-item>
        <el-form-item label="渠道代码" required>
          <el-input v-model="channelForm.code" placeholder="请输入渠道代码" />
        </el-form-item>
        <el-form-item label="渠道类型" required>
          <el-select v-model="channelForm.type" placeholder="请选择渠道类型">
            <el-option
              v-for="option in typeOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="显示颜色" required>
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
        <el-form-item label="状态">
          <el-switch v-model="channelForm.enabled" active-text="启用" inactive-text="禁用" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input
            v-model="channelForm.description"
            type="textarea"
            placeholder="请输入备注信息"
            :rows="3"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showChannelDialog = false">取消</el-button>
          <el-button type="primary" @click="saveChannel" :loading="loading">
            {{ isEditing ? '更新' : '创建' }}
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
