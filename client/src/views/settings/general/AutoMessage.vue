<template>
  <div class="auto-message-container">
    <div class="page-header">
      <h2 class="page-title">自动化消息</h2>
      <el-button type="primary" @click="handleCreate">创建</el-button>
    </div>

    <el-table :data="messages" border stripe v-loading="loading">
      <el-table-column prop="title" label="标题" min-width="120" />
      <el-table-column prop="message" label="消息" min-width="200" show-overflow-tooltip />
      <el-table-column prop="automationRule" label="自动化规则" min-width="150" />
      <el-table-column prop="channel" label="渠道" min-width="120" />
      <el-table-column prop="room" label="房间" min-width="120" />
      <el-table-column label="允许" width="80" align="center">
        <template #default="{ row }">
          <el-switch v-model="row.enabled" @change="handleToggle(row)" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180" align="center" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
          <el-button link type="primary" @click="handleCopy(row)">复制</el-button>
          <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 创建/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="700px"
      :close-on-click-modal="false"
    >
      <el-form :model="form" :rules="formRules" ref="formRef" label-width="120px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入标题" />
        </el-form-item>

        <el-form-item label="消息内容" prop="message">
          <el-input
            v-model="form.message"
            type="textarea"
            :rows="4"
            placeholder="请输入消息内容"
          />
        </el-form-item>

        <el-form-item label="自动化规则" prop="automationRule">
          <el-select v-model="form.automationRule" placeholder="请选择自动化规则" style="width: 100%">
            <el-option label="订单确认时" value="订单确认时" />
            <el-option label="入住前24小时" value="入住前24小时" />
            <el-option label="入住当天" value="入住当天" />
            <el-option label="退房当天" value="退房当天" />
            <el-option label="退房后" value="退房后" />
          </el-select>
        </el-form-item>

        <el-form-item label="渠道" prop="channel">
          <el-select v-model="form.channel" placeholder="请选择渠道" style="width: 100%">
            <el-option label="全部渠道" value="全部渠道" />
            <el-option label="Booking.com" value="Booking.com" />
            <el-option label="Airbnb" value="Airbnb" />
            <el-option label="Agoda" value="Agoda" />
          </el-select>
        </el-form-item>

        <el-form-item label="房间" prop="room">
          <el-select v-model="form.room" placeholder="请选择房间" style="width: 100%">
            <el-option label="全部房间" value="全部房间" />
            <el-option label="要町201" value="要町201" />
            <el-option label="要町401" value="要町401" />
            <el-option label="東十条1F" value="東十条1F" />
          </el-select>
        </el-form-item>

        <el-form-item label="启用">
          <el-switch v-model="form.enabled" />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSave">保存</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  getAutoMessagesByUserId,
  createAutoMessage,
  updateAutoMessage,
  deleteAutoMessage,
  toggleAutoMessage,
  type AutoMessageDTO,
} from '@/api/autoMessage'

interface AutoMessage {
  id: number
  title: string
  message: string
  automationRule: string
  channel: string
  room: string
  enabled: boolean
}

const loading = ref(false)
const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const editingId = ref<number | null>(null)

const messages = ref<AutoMessage[]>([])

const form = reactive<AutoMessage>({
  id: 0,
  title: '',
  message: '',
  automationRule: '',
  channel: '',
  room: '',
  enabled: true,
})

const formRules: FormRules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  message: [{ required: true, message: '请输入消息内容', trigger: 'blur' }],
  automationRule: [{ required: true, message: '请选择自动化规则', trigger: 'change' }],
  channel: [{ required: true, message: '请选择渠道', trigger: 'change' }],
  room: [{ required: true, message: '请选择房间', trigger: 'change' }],
}

const dialogTitle = computed(() => {
  return editingId.value ? '编辑自动化消息' : '创建自动化消息'
})

// 加载自动化消息列表
const loadAutoMessages = async () => {
  try {
    loading.value = true
    const response = await getAutoMessagesByUserId(1) // 默认用户ID为1
    if (response.success && response.data) {
      messages.value = response.data
    } else {
      ElMessage.error(response.message || '加载自动化消息列表失败')
    }
  } catch (error) {
    console.error('加载自动化消息列表失败:', error)
    ElMessage.error('加载自动化消息列表失败')
  } finally {
    loading.value = false
  }
}

const resetForm = () => {
  form.id = 0
  form.title = ''
  form.message = ''
  form.automationRule = ''
  form.channel = ''
  form.room = ''
  form.enabled = true
  formRef.value?.clearValidate()
}

const handleCreate = () => {
  resetForm()
  editingId.value = null
  dialogVisible.value = true
}

const handleEdit = (row: AutoMessage) => {
  Object.assign(form, row)
  editingId.value = row.id
  dialogVisible.value = true
}

const handleCopy = (row: AutoMessage) => {
  Object.assign(form, { ...row, id: 0, title: row.title + ' (副本)' })
  editingId.value = null
  dialogVisible.value = true
}

const handleDelete = async (row: AutoMessage) => {
  try {
    await ElMessageBox.confirm(`确定要删除消息 "${row.title}" 吗?`, '删除确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })

    loading.value = true
    const response = await deleteAutoMessage(row.id)
    if (response.success) {
      ElMessage.success('删除成功')
      await loadAutoMessages()
    } else {
      ElMessage.error(response.message || '删除失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
      ElMessage.error('删除失败')
    }
  } finally {
    loading.value = false
  }
}

const handleToggle = async (row: AutoMessage) => {
  try {
    loading.value = true
    const response = await toggleAutoMessage(row.id)
    if (response.success) {
      ElMessage.success(response.data.enabled ? '已启用' : '已禁用')
      await loadAutoMessages()
    } else {
      ElMessage.error(response.message || '切换状态失败')
      // 恢复原状态
      row.enabled = !row.enabled
    }
  } catch (error) {
    console.error('切换状态失败:', error)
    ElMessage.error('切换状态失败')
    // 恢复原状态
    row.enabled = !row.enabled
  } finally {
    loading.value = false
  }
}

const handleSave = async () => {
  try {
    const valid = await formRef.value?.validate()
    if (valid) {
      loading.value = true
      const data = {
        title: form.title,
        message: form.message,
        automationRule: form.automationRule,
        channel: form.channel,
        room: form.room,
        enabled: form.enabled,
        userId: 1, // 默认用户ID为1
      }

      let response
      if (editingId.value) {
        response = await updateAutoMessage(editingId.value, data)
      } else {
        response = await createAutoMessage(data)
      }

      if (response.success) {
        ElMessage.success(editingId.value ? '更新成功' : '创建成功')
        dialogVisible.value = false
        await loadAutoMessages()
      } else {
        ElMessage.error(response.message || '保存失败')
      }
    }
  } catch (error) {
    console.error('保存失败:', error)
    ElMessage.error('保存失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadAutoMessages()
})
</script>

<style scoped>
.auto-message-container {
  padding: 24px;
  background: #fff;
  min-height: calc(100vh - 100px);
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-title {
  font-size: 20px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

:deep(.el-table) {
  font-size: 14px;
}

:deep(.el-table th) {
  background-color: #f5f7fa;
  color: #606266;
  font-weight: 600;
}
</style>
