<template>
  <div class="quick-reply-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <div>
        <h2 class="page-title">{{ t('settings.quickReply.title') }}</h2>
        <p class="page-description">{{ t('settings.quickReply.description') }}</p>
      </div>
      <el-button type="primary" @click="handleCreate">{{ t('settings.quickReply.create') }}</el-button>
    </div>

    <!-- 表格 -->
    <div class="table-container">
      <el-table :data="quickReplies" style="width: 100%" v-loading="loading">
        <el-table-column prop="title" :label="t('settings.quickReply.columns.title')" min-width="200" />
        <el-table-column prop="message" :label="t('settings.quickReply.columns.message')" min-width="400" show-overflow-tooltip />
        <el-table-column :label="t('settings.quickReply.columns.action')" width="200" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleEdit(row)">
              {{ t('settings.common.edit') }}
            </el-button>
            <el-button link type="danger" @click="handleDelete(row.id)">
              {{ t('settings.common.delete') }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 创建/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="
        isEditing
          ? t('settings.quickReply.dialogTitle.edit')
          : t('settings.quickReply.dialogTitle.create')
      "
      width="800px"
      :close-on-click-modal="false"
    >
      <el-form :model="form" :rules="formRules" ref="formRef" label-width="80px">
        <el-form-item :label="t('settings.quickReply.fields.title')" prop="title">
          <el-input
            v-model="form.title"
            :placeholder="t('settings.quickReply.placeholders.title')"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>

        <el-form-item :label="t('settings.quickReply.fields.message')" prop="message">
          <el-input
            v-model="form.message"
            type="textarea"
            :rows="6"
            :placeholder="t('settings.quickReply.placeholders.message')"
            maxlength="1500"
            show-word-limit
          />
        </el-form-item>

        <el-form-item :label="t('settings.quickReply.fields.variables')">
          <div class="variable-info">
            {{ t('settings.quickReply.variableInfo') }}
          </div>
          <div class="variable-buttons">
            <el-button
              v-for="variable in quickReplyVariables"
              :key="variable.value"
              size="small"
              @click="insertVariable(variable.value)"
            >
              {{ t(variable.labelKey) }}
            </el-button>
          </div>
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">{{ t('settings.common.cancel') }}</el-button>
          <el-button type="primary" @click="handleSave">{{ t('settings.common.save') }}</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  getAllQuickReplies,
  createQuickReply,
  updateQuickReply,
  deleteQuickReply,
  type QuickReplyDTO,
} from '@/api/quickReply'
import { useStoreStore } from '@/stores/store'

const { t } = useI18n()
const storeStore = useStoreStore()
const loading = ref(false)
const dialogVisible = ref(false)
const isEditing = ref(false)
const currentEditId = ref<number | null>(null)
const formRef = ref<FormInstance>()

const quickReplies = ref<QuickReplyDTO[]>([])

const quickReplyVariables = [
  { labelKey: 'settings.quickReply.variables.propertyName', value: 'Property name' },
  { labelKey: 'settings.quickReply.variables.guestName', value: "Guest's name" },
  { labelKey: 'settings.quickReply.variables.guestPhone', value: "Guest's phone number" },
  { labelKey: 'settings.quickReply.variables.checkInDate', value: 'Check-in date' },
  { labelKey: 'settings.quickReply.variables.checkoutDate', value: 'Checkout date' },
  { labelKey: 'settings.quickReply.variables.roomTypeName', value: 'Room type name' },
  { labelKey: 'settings.quickReply.variables.ratePlanName', value: 'Rate plan name' },
] as const

const form = reactive({
  title: '',
  message: '',
})

const formRules: FormRules = {
  title: [{ required: true, message: t('settings.quickReply.validation.titleRequired'), trigger: 'blur' }],
  message: [{ required: true, message: t('settings.quickReply.validation.messageRequired'), trigger: 'blur' }],
}

// 加载快捷回复列表
const loadQuickReplies = async () => {
  if (!storeStore.currentStore?.id) {
    ElMessage.warning(t('settings.quickReply.messages.selectStore'))
    quickReplies.value = []
    return
  }

  try {
    loading.value = true
    const response = await getAllQuickReplies()
    if (response.success && response.data) {
      quickReplies.value = response.data
    } else {
      ElMessage.error(response.message || t('settings.quickReply.messages.loadFailed'))
    }
  } catch (error) {
    console.error('加载快捷回复列表失败:', error)
    ElMessage.error(t('settings.quickReply.messages.loadFailed'))
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
      }

      let response
      if (isEditing.value && currentEditId.value) {
        response = await updateQuickReply(currentEditId.value, data)
      } else {
        response = await createQuickReply(data)
      }

      if (response.success) {
        ElMessage.success(
          isEditing.value
            ? t('settings.quickReply.messages.updateSuccess')
            : t('settings.quickReply.messages.createSuccess'),
        )
        dialogVisible.value = false
        await loadQuickReplies()
      } else {
        ElMessage.error(response.message || t('settings.quickReply.messages.saveFailed'))
      }
    }
  } catch (error) {
    console.error('保存失败:', error)
    ElMessage.error(t('settings.quickReply.messages.saveFailed'))
  } finally {
    loading.value = false
  }
}

// 删除快捷回复
const handleDelete = async (id: number) => {
  try {
    await ElMessageBox.confirm(t('settings.quickReply.messages.deleteConfirm'), t('settings.common.deleteConfirmTitle'), {
      confirmButtonText: t('settings.common.confirmButton'),
      cancelButtonText: t('settings.common.cancelButton'),
      type: 'warning',
    })

    loading.value = true
    const response = await deleteQuickReply(id)
    if (response.success) {
      ElMessage.success(t('settings.common.deleteSuccess'))
      await loadQuickReplies()
    } else {
      ElMessage.error(response.message || t('settings.common.deleteFailed'))
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
      ElMessage.error(t('settings.common.deleteFailed'))
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
