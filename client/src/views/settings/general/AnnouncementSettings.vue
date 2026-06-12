<template>
  <div class="announcement-settings-container">
    <div class="page-header">
      <div>
        <h2 class="page-title">{{ t('settings.announcementSettings.title') }}</h2>
        <p class="page-desc">{{ t('settings.announcementSettings.description') }}</p>
      </div>
      <div class="header-actions">
        <el-button :loading="loading" @click="loadAnnouncements">
          {{ t('settings.announcementSettings.refresh') }}
        </el-button>
        <el-button type="primary" @click="handleCreate">
          {{ t('settings.announcementSettings.create') }}
        </el-button>
      </div>
    </div>

    <el-alert
      v-if="errorMessage"
      class="page-alert"
      type="error"
      show-icon
      :closable="false"
      :title="errorMessage"
    />

    <el-table
      v-loading="loading"
      :data="announcements"
      border
      stripe
      class="announcement-table"
    >
      <el-table-column
        prop="title"
        :label="t('settings.announcementSettings.columns.title')"
        min-width="180"
        show-overflow-tooltip
      />
      <el-table-column
        prop="content"
        :label="t('settings.announcementSettings.columns.content')"
        min-width="260"
        show-overflow-tooltip
      />
      <el-table-column :label="t('settings.announcementSettings.columns.locale')" width="120">
        <template #default="{ row }">
          {{ getLocaleLabel(row.locale) }}
        </template>
      </el-table-column>
      <el-table-column :label="t('settings.announcementSettings.columns.type')" width="130">
        <template #default="{ row }">
          {{ getOptionLabel(typeOptions, row.type) }}
        </template>
      </el-table-column>
      <el-table-column :label="t('settings.announcementSettings.columns.severity')" width="120">
        <template #default="{ row }">
          <el-tag :type="getSeverityTagType(row.severity)" effect="plain">
            {{ getOptionLabel(severityOptions, row.severity) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column
        prop="sortOrder"
        :label="t('settings.announcementSettings.columns.sortOrder')"
        width="100"
        align="right"
      />
      <el-table-column :label="t('settings.common.status')" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="getActiveTagType(row.active)" effect="light">
            {{ row.active ? t('settings.common.enabled') : t('settings.common.disabled') }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column :label="t('settings.announcementSettings.columns.period')" min-width="190">
        <template #default="{ row }">
          <div class="period-cell">
            <span>{{ formatDateTime(row.startsAt) }}</span>
            <span>{{ t('settings.announcementSettings.periodSeparator') }}</span>
            <span>{{ formatDateTime(row.endsAt) }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column :label="t('settings.common.actions')" width="170" align="center" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="() => handleEdit(row)">
            {{ t('settings.common.edit') }}
          </el-button>
          <el-button
            v-if="row.active"
            link
            type="danger"
            :loading="actionLoadingId === row.id"
            @click="() => handleDisable(row)"
          >
            {{ t('settings.common.disable') }}
          </el-button>
          <el-button
            v-else
            link
            type="primary"
            :loading="actionLoadingId === row.id"
            @click="() => handleEnable(row)"
          >
            {{ t('settings.common.enable') }}
          </el-button>
        </template>
      </el-table-column>

      <template #empty>
        <el-empty :description="t('settings.announcementSettings.empty')" />
      </template>
    </el-table>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="720px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="formRules" label-position="top">
        <el-form-item :label="t('settings.announcementSettings.fields.title')" prop="title" required>
          <el-input
            v-model="form.title"
            :maxlength="TITLE_MAX_LENGTH"
            show-word-limit
            :placeholder="t('settings.announcementSettings.placeholders.title')"
          />
        </el-form-item>

        <el-form-item
          :label="t('settings.announcementSettings.fields.content')"
          prop="content"
          required
        >
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="5"
            :placeholder="t('settings.announcementSettings.placeholders.content')"
          />
        </el-form-item>

        <div class="form-grid">
          <el-form-item :label="t('settings.announcementSettings.fields.locale')" prop="locale">
            <el-select
              v-model="form.locale"
              clearable
              :placeholder="t('settings.announcementSettings.placeholders.locale')"
              style="width: 100%"
            >
              <el-option
                v-for="option in localeOptions"
                :key="option.value"
                :label="t(option.labelKey)"
                :value="option.value"
              />
            </el-select>
          </el-form-item>

          <el-form-item :label="t('settings.announcementSettings.fields.type')" prop="type" required>
            <el-select v-model="form.type" style="width: 100%">
              <el-option
                v-for="option in typeOptions"
                :key="option.value"
                :label="t(option.labelKey)"
                :value="option.value"
              />
            </el-select>
          </el-form-item>

          <el-form-item
            :label="t('settings.announcementSettings.fields.severity')"
            prop="severity"
            required
          >
            <el-select v-model="form.severity" style="width: 100%">
              <el-option
                v-for="option in severityOptions"
                :key="option.value"
                :label="t(option.labelKey)"
                :value="option.value"
              />
            </el-select>
          </el-form-item>

          <el-form-item
            :label="t('settings.announcementSettings.fields.sortOrder')"
            prop="sortOrder"
            required
          >
            <el-input-number
              v-model="form.sortOrder"
              :min="MIN_SORT_ORDER"
              :max="MAX_SORT_ORDER"
              :step="SORT_ORDER_STEP"
              controls-position="right"
              style="width: 100%"
            />
          </el-form-item>
        </div>

        <div class="form-grid">
          <el-form-item :label="t('settings.announcementSettings.fields.startsAt')" prop="startsAt">
            <el-date-picker
              v-model="form.startsAt"
              type="datetime"
              format="YYYY-MM-DD HH:mm"
              value-format="YYYY-MM-DDTHH:mm:ss"
              :placeholder="t('settings.announcementSettings.placeholders.startsAt')"
              style="width: 100%"
            />
          </el-form-item>

          <el-form-item :label="t('settings.announcementSettings.fields.endsAt')" prop="endsAt">
            <el-date-picker
              v-model="form.endsAt"
              type="datetime"
              format="YYYY-MM-DD HH:mm"
              value-format="YYYY-MM-DDTHH:mm:ss"
              :placeholder="t('settings.announcementSettings.placeholders.endsAt')"
              style="width: 100%"
            />
          </el-form-item>
        </div>

        <el-form-item :label="t('settings.announcementSettings.fields.active')" prop="active">
          <el-switch
            v-model="form.active"
            :active-text="t('settings.common.enabled')"
            :inactive-text="t('settings.common.disabled')"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">
            {{ t('settings.common.cancel') }}
          </el-button>
          <el-button type="primary" :loading="submitLoading" @click="handleSave">
            {{ t('settings.common.save') }}
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  createStoreAnnouncement,
  disableStoreAnnouncement,
  listStoreAnnouncements,
  updateStoreAnnouncement,
  type AnnouncementManagementDTO,
  type AnnouncementMutationPayload,
} from '@/api/announcements'

interface SelectOption {
  value: string
  labelKey: string
}

interface AnnouncementForm {
  title: string
  content: string
  locale: string | null
  type: string
  severity: string
  sortOrder: number
  startsAt: string | null
  endsAt: string | null
  active: boolean
}

const DEFAULT_TYPE = 'GENERAL'
const DEFAULT_SEVERITY = 'INFO'
const DEFAULT_SORT_ORDER = 0
const MIN_SORT_ORDER = -9999
const MAX_SORT_ORDER = 9999
const SORT_ORDER_STEP = 1
const TITLE_MAX_LENGTH = 160
const DATE_TIME_DISPLAY_LENGTH = 16
const DATE_TIME_SEPARATOR = 'T'
const EMPTY_DISPLAY = '-'

const typeOptions: SelectOption[] = [
  { value: 'GENERAL', labelKey: 'settings.announcementSettings.types.general' },
  { value: 'MAINTENANCE', labelKey: 'settings.announcementSettings.types.maintenance' },
  { value: 'PROMOTION', labelKey: 'settings.announcementSettings.types.promotion' },
  { value: 'POLICY', labelKey: 'settings.announcementSettings.types.policy' },
]

const severityOptions: SelectOption[] = [
  { value: 'INFO', labelKey: 'settings.announcementSettings.severities.info' },
  { value: 'SUCCESS', labelKey: 'settings.announcementSettings.severities.success' },
  { value: 'WARNING', labelKey: 'settings.announcementSettings.severities.warning' },
  { value: 'ERROR', labelKey: 'settings.announcementSettings.severities.error' },
]

const localeOptions: SelectOption[] = [
  { value: 'zh-CN', labelKey: 'language.option.zh-CN' },
  { value: 'zh-TW', labelKey: 'language.option.zh-TW' },
  { value: 'en', labelKey: 'language.option.en' },
  { value: 'ja', labelKey: 'language.option.ja' },
]

const { t } = useI18n()
const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const actionLoadingId = ref<number | null>(null)
const errorMessage = ref('')
const announcements = ref<AnnouncementManagementDTO[]>([])
const formRef = ref<FormInstance>()

const createEmptyForm = (): AnnouncementForm => ({
  title: '',
  content: '',
  locale: null,
  type: DEFAULT_TYPE,
  severity: DEFAULT_SEVERITY,
  sortOrder: DEFAULT_SORT_ORDER,
  startsAt: null,
  endsAt: null,
  active: true,
})

const form = ref<AnnouncementForm>(createEmptyForm())

const validateTitle = (_rule: unknown, value: string, callback: (error?: Error) => void) => {
  if (!value || !value.trim()) {
    callback(new Error(t('settings.announcementSettings.validation.titleRequired')))
    return
  }
  callback()
}

const validateContent = (_rule: unknown, value: string, callback: (error?: Error) => void) => {
  if (!value || !value.trim()) {
    callback(new Error(t('settings.announcementSettings.validation.contentRequired')))
    return
  }
  callback()
}

const validateDateRange = (_rule: unknown, value: string | null, callback: (error?: Error) => void) => {
  if (form.value.startsAt && value && form.value.startsAt > value) {
    callback(new Error(t('settings.announcementSettings.validation.invalidPeriod')))
    return
  }
  callback()
}

const formRules: FormRules = {
  title: [{ validator: validateTitle, trigger: 'blur' }],
  content: [{ validator: validateContent, trigger: 'blur' }],
  type: [{ required: true, message: t('settings.announcementSettings.validation.typeRequired'), trigger: 'change' }],
  severity: [
    {
      required: true,
      message: t('settings.announcementSettings.validation.severityRequired'),
      trigger: 'change',
    },
  ],
  sortOrder: [
    {
      required: true,
      type: 'number',
      message: t('settings.announcementSettings.validation.sortOrderRequired'),
      trigger: 'change',
    },
  ],
  endsAt: [{ validator: validateDateRange, trigger: 'change' }],
}

const dialogTitle = computed(() => {
  if (editingId.value) {
    return t('settings.announcementSettings.dialogTitle.edit')
  }
  return t('settings.announcementSettings.dialogTitle.create')
})

const getOptionLabel = (options: SelectOption[], value?: string | null) => {
  for (const option of options) {
    if (option.value === value) {
      return t(option.labelKey)
    }
  }
  return value || EMPTY_DISPLAY
}

const getLocaleLabel = (locale?: string | null) => {
  if (!locale) {
    return t('settings.announcementSettings.locales.all')
  }
  return getOptionLabel(localeOptions, locale)
}

const getSeverityTagType = (severity?: string | null) => {
  if (severity === 'SUCCESS') {
    return 'success'
  }
  if (severity === 'WARNING') {
    return 'warning'
  }
  if (severity === 'ERROR') {
    return 'danger'
  }
  return 'info'
}

const getActiveTagType = (active: boolean) => {
  if (active) {
    return 'success'
  }
  return 'info'
}

const formatDateTime = (value?: string | null) => {
  if (!value) {
    return EMPTY_DISPLAY
  }
  return value.replace(DATE_TIME_SEPARATOR, ' ').slice(0, DATE_TIME_DISPLAY_LENGTH)
}

const normalizeOptionalText = (value: string | null) => {
  if (!value || !value.trim()) {
    return null
  }
  return value.trim()
}

const buildPayload = (source: AnnouncementForm): AnnouncementMutationPayload => ({
  title: source.title.trim(),
  content: source.content.trim(),
  locale: normalizeOptionalText(source.locale),
  type: source.type,
  severity: source.severity,
  active: source.active,
  sortOrder: Number(source.sortOrder),
  startsAt: source.startsAt || null,
  endsAt: source.endsAt || null,
})

const buildPayloadFromAnnouncement = (
  announcement: AnnouncementManagementDTO,
  active: boolean,
): AnnouncementMutationPayload => ({
  title: announcement.title,
  content: announcement.content,
  locale: announcement.locale,
  type: announcement.type || DEFAULT_TYPE,
  severity: announcement.severity || DEFAULT_SEVERITY,
  active,
  sortOrder: announcement.sortOrder ?? DEFAULT_SORT_ORDER,
  startsAt: announcement.startsAt || null,
  endsAt: announcement.endsAt || null,
})

const loadAnnouncements = async () => {
  try {
    loading.value = true
    errorMessage.value = ''
    const response = await listStoreAnnouncements()
    if (response.success && response.data) {
      announcements.value = response.data
      return
    }
    errorMessage.value = response.message || t('settings.announcementSettings.messages.loadFailed')
  } catch (error) {
    console.error('加载公告列表失败:', error)
    errorMessage.value = t('settings.announcementSettings.messages.loadFailed')
  } finally {
    loading.value = false
  }
}

const resetForm = () => {
  form.value = createEmptyForm()
  formRef.value?.clearValidate()
}

const handleCreate = () => {
  editingId.value = null
  resetForm()
  dialogVisible.value = true
}

const handleEdit = (announcement: AnnouncementManagementDTO) => {
  editingId.value = announcement.id
  form.value = {
    title: announcement.title || '',
    content: announcement.content || '',
    locale: announcement.locale || null,
    type: announcement.type || DEFAULT_TYPE,
    severity: announcement.severity || DEFAULT_SEVERITY,
    sortOrder: announcement.sortOrder ?? DEFAULT_SORT_ORDER,
    startsAt: announcement.startsAt || null,
    endsAt: announcement.endsAt || null,
    active: announcement.active,
  }
  formRef.value?.clearValidate()
  dialogVisible.value = true
}

const handleSave = async () => {
  if (!formRef.value) {
    return
  }

  try {
    await formRef.value.validate()
  } catch {
    return
  }

  try {
    submitLoading.value = true
    const payload = buildPayload(form.value)
    let response
    if (editingId.value) {
      response = await updateStoreAnnouncement(editingId.value, payload)
    } else {
      response = await createStoreAnnouncement(payload)
    }

    if (response.success) {
      ElMessage.success(
        editingId.value
          ? t('settings.announcementSettings.messages.updateSuccess')
          : t('settings.announcementSettings.messages.createSuccess'),
      )
      dialogVisible.value = false
      await loadAnnouncements()
      return
    }
    ElMessage.error(response.message || t('settings.announcementSettings.messages.saveFailed'))
  } catch (error) {
    console.error('保存公告失败:', error)
    ElMessage.error(t('settings.announcementSettings.messages.saveFailed'))
  } finally {
    submitLoading.value = false
  }
}

const handleDisable = async (announcement: AnnouncementManagementDTO) => {
  try {
    await ElMessageBox.confirm(
      t('settings.announcementSettings.messages.disableConfirm', {
        title: announcement.title,
      }),
      t('settings.announcementSettings.messages.disableConfirmTitle'),
      {
        confirmButtonText: t('settings.common.confirmButton'),
        cancelButtonText: t('settings.common.cancelButton'),
        type: 'warning',
      },
    )
  } catch {
    return
  }

  try {
    actionLoadingId.value = announcement.id
    const response = await disableStoreAnnouncement(announcement.id)
    if (response.success) {
      ElMessage.success(t('settings.announcementSettings.messages.disableSuccess'))
      await loadAnnouncements()
      return
    }
    ElMessage.error(response.message || t('settings.announcementSettings.messages.disableFailed'))
  } catch (error) {
    console.error('停用公告失败:', error)
    ElMessage.error(t('settings.announcementSettings.messages.disableFailed'))
  } finally {
    actionLoadingId.value = null
  }
}

const handleEnable = async (announcement: AnnouncementManagementDTO) => {
  try {
    actionLoadingId.value = announcement.id
    const response = await updateStoreAnnouncement(
      announcement.id,
      buildPayloadFromAnnouncement(announcement, true),
    )
    if (response.success) {
      ElMessage.success(t('settings.announcementSettings.messages.enableSuccess'))
      await loadAnnouncements()
      return
    }
    ElMessage.error(response.message || t('settings.announcementSettings.messages.enableFailed'))
  } catch (error) {
    console.error('启用公告失败:', error)
    ElMessage.error(t('settings.announcementSettings.messages.enableFailed'))
  } finally {
    actionLoadingId.value = null
  }
}

onMounted(() => {
  void loadAnnouncements()
})
</script>

<style scoped>
.announcement-settings-container {
  padding: 24px;
  background: #fff;
  min-height: calc(100vh - 100px);
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 20px;
}

.page-title {
  font-size: 20px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

.page-desc {
  margin: 8px 0 0;
  color: #606266;
  font-size: 14px;
  line-height: 1.5;
}

.header-actions {
  display: flex;
  gap: 12px;
  flex-shrink: 0;
}

.page-alert {
  margin-bottom: 16px;
}

.announcement-table {
  width: 100%;
}

:deep(.el-table th) {
  background-color: #f5f7fa;
  color: #606266;
  font-weight: 600;
}

.period-cell {
  display: flex;
  flex-direction: column;
  gap: 2px;
  line-height: 1.35;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 16px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
  }

  .header-actions {
    width: 100%;
    justify-content: flex-start;
    flex-wrap: wrap;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
