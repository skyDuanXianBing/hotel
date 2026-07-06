<template>
  <div class="quick-reply-container">
    <section class="quick-reply-panel">
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
        <el-table :data="quickReplies" class="quick-reply-table" style="width: 100%" v-loading="loading">
          <el-table-column
            prop="title"
            :label="t('settings.quickReply.columns.title')"
            min-width="220"
            align="center"
            header-align="center"
            class-name="reply-title-column"
          >
            <template #default="{ row }">
              <span class="reply-title" :title="row.title">{{ row.title }}</span>
            </template>
          </el-table-column>
          <el-table-column
            prop="message"
            :label="t('settings.quickReply.columns.message')"
            min-width="560"
            align="center"
            header-align="center"
            class-name="reply-message-column"
          >
            <template #default="{ row }">
              <span class="reply-message" :title="row.message">{{ row.message }}</span>
            </template>
          </el-table-column>
          <el-table-column
            :label="t('settings.quickReply.columns.action')"
            width="150"
            align="center"
            header-align="center"
            class-name="reply-action-column"
          >
            <template #default="{ row }">
              <div class="reply-actions">
                <el-button link type="primary" @click="handleEdit(row)">
                  {{ t('settings.common.edit') }}
                </el-button>
                <el-button link type="danger" @click="handleDelete(row.id)">
                  {{ t('settings.common.delete') }}
                </el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </section>

    <!-- 创建/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="
        isEditing
          ? t('settings.quickReply.dialogTitle.edit')
          : t('settings.quickReply.dialogTitle.create')
      "
      width="500px"
      class="quick-reply-dialog"
      :close-on-click-modal="false"
    >
      <el-form :model="form" :rules="formRules" ref="formRef" label-position="top" class="quick-reply-form">
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
            :rows="4"
            :placeholder="t('settings.quickReply.placeholders.message')"
            maxlength="1500"
            show-word-limit
          />
        </el-form-item>

        <div class="variable-panel">
          <div class="variable-title">{{ t('settings.quickReply.fields.variables') }}</div>
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
        </div>
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
  min-height: 100%;
  background: transparent;
}

.quick-reply-panel {
  min-height: 540px;
  padding: 0 24px 24px;
  background: #ffffff;
  border-radius: 2px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  padding: 24px 0 22px;
  margin: 0;
  background: #ffffff;
}

.page-title {
  font-size: 16px;
  font-weight: 600;
  line-height: 22px;
  color: #1f2329;
  margin: 0 0 4px 0;
}

.page-description {
  font-size: 14px;
  line-height: 20px;
  color: #8f959e;
  margin: 0;
}

.page-header :deep(.el-button) {
  height: 28px;
  min-width: 48px;
  padding: 0 13px;
  border-radius: 4px;
  font-size: 13px;
  font-weight: 400;
}

.table-container {
  background: #ffffff;
}

:deep(.quick-reply-table.el-table) {
  --el-table-border-color: #eeeeee;
  --el-table-header-bg-color: #ffffff;
  --el-table-row-hover-bg-color: #fafafa;
  color: #8d929a;
  font-size: 12px;
}

:deep(.quick-reply-table.el-table .el-table__inner-wrapper::before) {
  height: 0;
}

:deep(.quick-reply-table.el-table th.el-table__cell) {
  height: 58px;
  padding: 0;
  background: #ffffff;
  border-bottom: 1px solid #eeeeee;
  color: #0f1115;
  font-size: 14px;
  font-weight: 600;
}

:deep(.quick-reply-table.el-table td.el-table__cell) {
  height: 58px;
  padding: 0;
  border-bottom: 1px solid #eeeeee;
}

:deep(.quick-reply-table.el-table .cell) {
  padding: 0 16px;
  line-height: 1.4;
}

:deep(.quick-reply-table.el-table .reply-title-column .cell) {
  padding: 0 22px;
}

:deep(.quick-reply-table.el-table .reply-message-column .cell) {
  padding: 0 24px;
}

.reply-title,
.reply-message {
  display: block;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.reply-title {
  color: #8b9098;
  font-size: 13px;
  font-weight: 400;
}

.reply-message {
  max-width: 100%;
  margin: 0 auto;
  color: #8d929a;
  font-size: 11px;
  line-height: 18px;
  text-align: center;
}

.reply-actions {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  white-space: nowrap;
}

.reply-actions :deep(.el-button) {
  height: 20px;
  padding: 0;
  font-size: 12px;
  font-weight: 400;
}

.reply-actions :deep(.el-button + .el-button) {
  margin-left: 0;
}

.reply-actions :deep(.el-button--primary.is-link) {
  color: #1688d9;
}

.reply-actions :deep(.el-button--danger.is-link) {
  color: #ff6b7a;
}

.quick-reply-form {
  margin: 0;
}

.quick-reply-form :deep(.el-form-item) {
  margin-bottom: 18px;
}

.quick-reply-form :deep(.el-form-item__label) {
  height: auto;
  margin-bottom: 8px;
  padding: 0;
  color: #1f2329;
  font-size: 14px;
  font-weight: 600;
  line-height: 18px;
}

.quick-reply-form :deep(.el-form-item__content) {
  line-height: normal;
}

.quick-reply-form :deep(.el-input__wrapper) {
  min-height: 40px;
  padding: 0 16px;
  border-radius: 4px;
  box-shadow: 0 0 0 1px #d8dce3 inset;
}

.quick-reply-form :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #c7ccd4 inset;
}

.quick-reply-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #1688d9 inset;
}

.quick-reply-form :deep(.el-input__inner) {
  height: 38px;
  color: #1f2329;
  font-size: 13px;
}

.quick-reply-form :deep(.el-input__inner::placeholder),
.quick-reply-form :deep(.el-textarea__inner::placeholder) {
  color: #a8abb2;
}

.quick-reply-form :deep(.el-textarea__inner) {
  min-height: 90px !important;
  padding: 13px 18px;
  border-radius: 4px;
  color: #1f2329;
  font-family: inherit;
  font-size: 13px;
  line-height: 20px;
  resize: none;
  box-shadow: 0 0 0 1px #d8dce3 inset;
}

.quick-reply-form :deep(.el-textarea__inner:hover) {
  box-shadow: 0 0 0 1px #c7ccd4 inset;
}

.quick-reply-form :deep(.el-textarea__inner:focus) {
  box-shadow: 0 0 0 1px #1688d9 inset;
}

.quick-reply-form :deep(.el-input__count) {
  color: #a8abb2;
  font-size: 10px;
  background: transparent;
}

.variable-panel {
  margin-top: 2px;
  padding: 12px 18px 13px;
  background: #fbfbfc;
  border-radius: 2px;
}

.variable-title {
  margin-bottom: 5px;
  color: #1f2329;
  font-size: 14px;
  font-weight: 600;
  line-height: 18px;
}

.variable-info {
  margin-bottom: 8px;
  color: #a0a4ab;
  font-size: 11px;
  line-height: 16px;
}

.variable-buttons {
  display: flex;
  flex-wrap: wrap;
  gap: 5px 4px;
}

.variable-buttons :deep(.el-button) {
  height: 20px;
  margin: 0;
  padding: 0 7px;
  border-color: #dcdfe6;
  border-radius: 3px;
  background: #ffffff;
  color: #8f959e;
  font-size: 11px;
  font-weight: 400;
  line-height: 18px;
}

.variable-buttons :deep(.el-button + .el-button) {
  margin-left: 0;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 10px;
}

.dialog-footer :deep(.el-button) {
  min-width: 52px;
  height: 32px;
  margin-left: 0;
  padding: 0 14px;
  border-radius: 4px;
  font-size: 14px;
  font-weight: 400;
}

.dialog-footer :deep(.el-button--primary) {
  border-color: #1688d9;
  background: #1688d9;
}

:global(.quick-reply-dialog.el-dialog) {
  border-radius: 2px;
}

:global(.quick-reply-dialog .el-dialog__header) {
  padding: 20px 18px 12px;
  margin-right: 0;
}

:global(.quick-reply-dialog .el-dialog__title) {
  color: #1f2329;
  font-size: 16px;
  font-weight: 600;
  line-height: 22px;
}

:global(.quick-reply-dialog .el-dialog__headerbtn) {
  top: 15px;
  right: 10px;
  width: 32px;
  height: 32px;
}

:global(.quick-reply-dialog .el-dialog__close) {
  color: #8f959e;
  font-size: 18px;
}

:global(.quick-reply-dialog .el-dialog__body) {
  padding: 4px 18px 0;
}

:global(.quick-reply-dialog .el-dialog__footer) {
  padding: 12px 18px 20px;
}

@media (max-width: 560px) {
  :global(.quick-reply-dialog.el-dialog) {
    width: calc(100vw - 32px) !important;
  }
}
</style>
