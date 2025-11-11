<template>
  <div class="quick-reply-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <div>
        <h2 class="page-title">快捷回复</h2>
        <p class="page-description">设置常用的快捷回复模板</p>
      </div>
      <el-button type="primary" @click="handleCreate">创建</el-button>
    </div>

    <!-- 表格 -->
    <div class="table-container">
      <el-table :data="quickReplies" style="width: 100%" v-loading="loading">
        <el-table-column prop="title" label="标题" min-width="200" />
        <el-table-column prop="message" label="消息" min-width="400" show-overflow-tooltip />
        <el-table-column label="Action" width="200" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 创建/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEditing ? '编辑快捷回复' : '创建快捷回复'"
      width="800px"
      :close-on-click-modal="false"
    >
      <el-form :model="form" :rules="formRules" ref="formRef" label-width="80px">
        <el-form-item label="标题" prop="title">
          <el-input
            v-model="form.title"
            placeholder="这不会展示给客人"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="消息" prop="message">
          <el-input
            v-model="form.message"
            type="textarea"
            :rows="6"
            placeholder="请输入快捷回复的内容"
            maxlength="1500"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="插入变量">
          <div class="variable-info">
            选择一个短代码并将其添加到您的消息中。 每次您使用消息时，
            正确的详细信息都会自动填充。
          </div>
          <div class="variable-buttons">
            <el-button size="small" @click="insertVariable('Property name')">
              Property name
            </el-button>
            <el-button size="small" @click="insertVariable(`Guest's name`)">
              Guest's name
            </el-button>
            <el-button size="small" @click="insertVariable(`Guest's phone number`)">
              Guest's phone number
            </el-button>
            <el-button size="small" @click="insertVariable('Check-in date')">
              Check-in date
            </el-button>
            <el-button size="small" @click="insertVariable('Checkout date')">
              Checkout date
            </el-button>
            <el-button size="small" @click="insertVariable('Room type name')">
              Room type name
            </el-button>
            <el-button size="small" @click="insertVariable('Rate plan name')">
              Rate plan name
            </el-button>
          </div>
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
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  getAllQuickReplies,
  createQuickReply,
  updateQuickReply,
  deleteQuickReply,
  type QuickReplyDTO,
} from '@/api/quickReply'

const loading = ref(false)
const dialogVisible = ref(false)
const isEditing = ref(false)
const currentEditId = ref<number | null>(null)
const formRef = ref<FormInstance>()

const quickReplies = ref<QuickReplyDTO[]>([])

const form = reactive({
  title: '',
  message: '',
})

const formRules: FormRules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  message: [{ required: true, message: '请输入消息内容', trigger: 'blur' }],
}

// 加载快捷回复列表
const loadQuickReplies = async () => {
  try {
    loading.value = true
    const response = await getAllQuickReplies()
    if (response.success && response.data) {
      quickReplies.value = response.data
    } else {
      ElMessage.error(response.message || '加载快捷回复列表失败')
    }
  } catch (error) {
    console.error('加载快捷回复列表失败:', error)
    ElMessage.error('加载快捷回复列表失败')
  } finally {
    loading.value = false
  }
}

// 创建快捷回复
const handleCreate = () => {
  isEditing.value = false
  currentEditId.value = null
  form.title = ''
  form.message = ''
  dialogVisible.value = true
}

// 编辑快捷回复
const handleEdit = (row: QuickReplyDTO) => {
  isEditing.value = true
  currentEditId.value = row.id
  form.title = row.title
  form.message = row.message
  dialogVisible.value = true
}

// 保存快捷回复
const handleSave = async () => {
  try {
    const valid = await formRef.value?.validate()
    if (valid) {
      loading.value = true
      const data = {
        title: form.title,
        message: form.message,
        userId: 1, // 默认用户ID为1
      }

      let response
      if (isEditing.value && currentEditId.value) {
        response = await updateQuickReply(currentEditId.value, data)
      } else {
        response = await createQuickReply(data)
      }

      if (response.success) {
        ElMessage.success(isEditing.value ? '更新成功' : '创建成功')
        dialogVisible.value = false
        await loadQuickReplies()
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

// 删除快捷回复
const handleDelete = async (id: number) => {
  try {
    await ElMessageBox.confirm('确定要删除这条快捷回复吗?', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })

    loading.value = true
    const response = await deleteQuickReply(id)
    if (response.success) {
      ElMessage.success('删除成功')
      await loadQuickReplies()
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

// 插入变量
const insertVariable = (variable: string) => {
  const variableText = `{${variable}}`
  const textarea = document.querySelector('.el-textarea__inner') as HTMLTextAreaElement
  if (textarea) {
    const start = textarea.selectionStart
    const end = textarea.selectionEnd
    const text = form.message
    form.message = text.substring(0, start) + variableText + text.substring(end)

    // 设置光标位置到插入变量之后
    setTimeout(() => {
      textarea.focus()
      const newPosition = start + variableText.length
      textarea.setSelectionRange(newPosition, newPosition)
    }, 0)
  } else {
    form.message += variableText
  }
}

onMounted(() => {
  loadQuickReplies()
})
</script>

<style scoped>
.quick-reply-container {
  padding: 20px;
  background: #f5f7fa;
  min-height: calc(100vh - 100px);
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 4px 0;
}

.page-description {
  font-size: 14px;
  color: #909399;
  margin: 0;
}

.table-container {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.variable-info {
  font-size: 13px;
  color: #606266;
  margin-bottom: 12px;
  line-height: 1.6;
}

.variable-buttons {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

:deep(.el-form-item) {
  margin-bottom: 20px;
}

:deep(.el-textarea__inner) {
  font-family: inherit;
}
</style>
