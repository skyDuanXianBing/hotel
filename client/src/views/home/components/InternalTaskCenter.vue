<template>
  <el-drawer
    v-model="drawerVisible"
    :title="t('pages.home.workbench.internalTasks.title')"
    size="min(720px, 92vw)"
    @open="handleDrawerOpen"
  >
    <div class="task-center-toolbar">
      <el-tabs v-model="activeStatus" @tab-change="handleStatusChange">
        <el-tab-pane
          v-if="canManage"
          :label="t('pages.home.workbench.internalTasks.unassigned')"
          name="UNASSIGNED"
        />
        <el-tab-pane
          :label="`${t('pages.home.workbench.internalTasks.assigned')} (${pageData.assignedCount})`"
          name="ASSIGNED"
        />
        <el-tab-pane
          :label="`${t('pages.home.workbench.internalTasks.completed')} (${pageData.completedCount})`"
          name="COMPLETED"
        />
      </el-tabs>
      <el-button :icon="Refresh" circle :loading="loading" @click="handleRefresh" />
    </div>

    <div v-loading="loading" class="internal-task-list">
      <el-alert
        v-if="initialLoadError"
        type="error"
        :title="initialLoadError"
        :closable="false"
        show-icon
      />
      <el-empty
        v-else-if="!loading && pageData.items.length === 0"
        :description="t('pages.home.workbench.internalTasks.empty')"
      />
      <article v-for="task in pageData.items" :key="task.id" class="internal-task-card">
        <div class="task-card-main">
          <div class="task-card-title-row">
            <h4>{{ task.title }}</h4>
            <el-tag :type="statusTagType(task.status)" size="small">
              {{ statusLabel(task.status) }}
            </el-tag>
          </div>
          <p v-if="task.description">{{ task.description }}</p>
          <div class="task-card-meta">
            <span>{{ t('pages.home.workbench.internalTasks.creator') }}：{{ task.createdByName || '-' }}</span>
            <span>{{ t('pages.home.workbench.internalTasks.assignee') }}：{{ task.assigneeName || t('pages.home.workbench.internalTasks.notAssigned') }}</span>
            <span v-if="task.completedAt">
              {{ t('pages.home.workbench.internalTasks.completedAt') }}：{{ formatTime(task.completedAt) }}
            </span>
            <span v-if="task.completedByName">
              {{ t('pages.home.workbench.internalTasks.completedBy') }}：{{ task.completedByName }}
            </span>
          </div>
        </div>
        <div class="task-card-actions">
          <template v-if="canManage && task.status !== 'COMPLETED'">
            <el-select
              v-model="assigneeSelections[task.id]"
              clearable
              filterable
              size="small"
              :placeholder="t('pages.home.workbench.internalTasks.selectAssignee')"
              :loading="assigneesLoading"
            >
              <el-option
                v-for="assignee in assignees"
                :key="assignee.userId"
                :value="assignee.userId"
                :label="assigneeLabel(assignee)"
              />
            </el-select>
            <el-button
              size="small"
              type="primary"
              :loading="actionTaskId === task.id"
              @click="saveAssignee(task)"
            >
              {{ t('pages.home.workbench.internalTasks.saveAssignment') }}
            </el-button>
          </template>
          <el-button
            v-if="task.canComplete && task.status === 'ASSIGNED'"
            size="small"
            type="success"
            :loading="actionTaskId === task.id"
            @click="completeTask(task)"
          >
            {{ t('pages.home.workbench.internalTasks.markComplete') }}
          </el-button>
          <el-button
            v-if="canManage"
            size="small"
            type="danger"
            link
            :loading="actionTaskId === task.id"
            @click="archiveTask(task)"
          >
            {{ t('pages.home.workbench.internalTasks.archive') }}
          </el-button>
        </div>
      </article>
    </div>

    <el-pagination
      v-if="pageData.totalElements > pageData.size"
      layout="prev, pager, next"
      :page-size="pageData.size"
      :total="pageData.totalElements"
      :current-page="pageData.page + 1"
      @current-change="changePage"
    />
  </el-drawer>

  <el-dialog
    v-model="createVisible"
    :title="t('pages.home.workbench.internalTasks.createTitle')"
    width="min(520px, 92vw)"
    :close-on-click-modal="false"
    @open="ensureAssignees"
  >
    <el-form label-position="top" @submit.prevent>
      <el-form-item :label="t('pages.home.workbench.internalTasks.taskTitle')" required>
        <el-input v-model="createForm.title" maxlength="120" show-word-limit />
      </el-form-item>
      <el-form-item :label="t('pages.home.workbench.internalTasks.description')">
        <el-input v-model="createForm.description" type="textarea" :rows="4" maxlength="1000" show-word-limit />
      </el-form-item>
      <el-form-item :label="t('pages.home.workbench.internalTasks.assignee')">
        <el-select
          v-model="createForm.assigneeUserId"
          clearable
          filterable
          :loading="assigneesLoading"
          :placeholder="t('pages.home.workbench.internalTasks.optionalAssignee')"
          style="width: 100%"
        >
          <el-option
            v-for="assignee in assignees"
            :key="assignee.userId"
            :value="assignee.userId"
            :label="assigneeLabel(assignee)"
          />
        </el-select>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="createVisible = false">{{ t('pages.home.workbench.internalTasks.cancel') }}</el-button>
      <el-button type="primary" :loading="creating" @click="submitCreate">
        {{ t('pages.home.workbench.internalTasks.create') }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useI18n } from 'vue-i18n'
import {
  archiveInternalTask,
  assignInternalTask,
  completeInternalTask,
  createInternalTask,
  getInternalTaskAssignees,
  getInternalTasks,
  getMyInternalTasks,
  type InternalTaskAssigneeDTO,
  type InternalTaskDTO,
  type InternalTaskPageDTO,
  type InternalTaskStatus,
} from '@/api/internalTask'

const props = defineProps<{ canManage: boolean }>()
const emit = defineEmits<{ updated: [] }>()
const { t } = useI18n()

const emptyPage = (): InternalTaskPageDTO => ({
  items: [],
  page: 0,
  size: 20,
  totalElements: 0,
  assignedCount: 0,
  completedCount: 0,
})

const drawerVisible = ref(false)
const createVisible = ref(false)
const activeStatus = ref<InternalTaskStatus>(props.canManage ? 'UNASSIGNED' : 'ASSIGNED')
const pageData = ref<InternalTaskPageDTO>(emptyPage())
const loading = ref(false)
const creating = ref(false)
const actionTaskId = ref<number | null>(null)
const assigneesLoading = ref(false)
const assignees = ref<InternalTaskAssigneeDTO[]>([])
const assigneeSelections = ref<Record<number, number | undefined>>({})
const initialLoadError = ref('')
const hasSuccessfulLoad = ref(false)
let requestSeq = 0

const createForm = reactive<{ title: string; description: string; assigneeUserId?: number }>({
  title: '',
  description: '',
  assigneeUserId: undefined,
})

const openDrawer = () => {
  drawerVisible.value = true
}

const openCreate = () => {
  if (!props.canManage) return
  createVisible.value = true
}

const reset = () => {
  requestSeq += 1
  drawerVisible.value = false
  createVisible.value = false
  pageData.value = emptyPage()
  assignees.value = []
  assigneeSelections.value = {}
  initialLoadError.value = ''
  hasSuccessfulLoad.value = false
  activeStatus.value = props.canManage ? 'UNASSIGNED' : 'ASSIGNED'
}

const loadCurrentTab = async (page: unknown = 0) => {
  const normalizedPage =
    typeof page === 'number' && Number.isInteger(page) && page >= 0 ? page : 0
  const seq = ++requestSeq
  loading.value = true
  try {
    const params = { status: activeStatus.value, page: normalizedPage, size: 20 }
    const response = props.canManage
      ? await getInternalTasks(params)
      : await getMyInternalTasks(params)
    if (seq !== requestSeq) return
    if (!response.success || !response.data) {
      throw new Error(response.message || t('pages.home.workbench.internalTasks.loadFailed'))
    }
    pageData.value = response.data
    hasSuccessfulLoad.value = true
    initialLoadError.value = ''
    assigneeSelections.value = Object.fromEntries(
      response.data.items.map((task) => [task.id, task.assigneeUserId || undefined]),
    )
    if (props.canManage && activeStatus.value !== 'COMPLETED') void ensureAssignees()
  } catch (error) {
    if (seq === requestSeq) {
      const message = error instanceof Error ? error.message : t('pages.home.workbench.internalTasks.loadFailed')
      if (!hasSuccessfulLoad.value) {
        initialLoadError.value = message
      } else {
        ElMessage.error(message)
      }
    }
  } finally {
    if (seq === requestSeq) loading.value = false
  }
}

const ensureAssignees = async () => {
  if (!props.canManage || assignees.value.length || assigneesLoading.value) return
  assigneesLoading.value = true
  try {
    const response = await getInternalTaskAssignees()
    if (!response.success) throw new Error(response.message)
    assignees.value = response.data || []
  } catch (error) {
    ElMessage.error(error instanceof Error && error.message ? error.message : t('pages.home.workbench.internalTasks.assigneesFailed'))
  } finally {
    assigneesLoading.value = false
  }
}

const submitCreate = async () => {
  const title = createForm.title.trim()
  if (!title) {
    ElMessage.warning(t('pages.home.workbench.internalTasks.titleRequired'))
    return
  }
  creating.value = true
  try {
    const response = await createInternalTask({
      title,
      description: createForm.description.trim() || undefined,
      assigneeUserId: createForm.assigneeUserId,
    })
    if (!response.success) throw new Error(response.message)
    createVisible.value = false
    createForm.title = ''
    createForm.description = ''
    createForm.assigneeUserId = undefined
    ElMessage.success(t('pages.home.workbench.internalTasks.createSuccess'))
    emit('updated')
    if (drawerVisible.value) await loadCurrentTab()
  } catch (error) {
    ElMessage.error(error instanceof Error && error.message ? error.message : t('pages.home.workbench.internalTasks.createFailed'))
  } finally {
    creating.value = false
  }
}

const saveAssignee = async (task: InternalTaskDTO) => {
  actionTaskId.value = task.id
  try {
    const response = await assignInternalTask(task.id, {
      assigneeUserId: assigneeSelections.value[task.id],
      version: task.version,
    })
    if (!response.success) throw new Error(response.message)
    ElMessage.success(t('pages.home.workbench.internalTasks.assignSuccess'))
    emit('updated')
    await loadCurrentTab()
  } catch (error) {
    ElMessage.error(error instanceof Error && error.message ? error.message : t('pages.home.workbench.internalTasks.assignFailed'))
  } finally {
    actionTaskId.value = null
  }
}

const completeTask = async (task: InternalTaskDTO) => {
  try {
    await ElMessageBox.confirm(
      t('pages.home.workbench.internalTasks.completeConfirm'),
      t('pages.home.workbench.internalTasks.completeConfirmTitle'),
      { type: 'success' },
    )
    actionTaskId.value = task.id
    const response = await completeInternalTask(task.id)
    if (!response.success) throw new Error(response.message)
    ElMessage.success(t('pages.home.workbench.internalTasks.completeSuccess'))
    emit('updated')
    await loadCurrentTab()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error instanceof Error && error.message ? error.message : t('pages.home.workbench.internalTasks.completeFailed'))
    }
  } finally {
    actionTaskId.value = null
  }
}

const archiveTask = async (task: InternalTaskDTO) => {
  try {
    await ElMessageBox.confirm(
      t('pages.home.workbench.internalTasks.archiveConfirm'),
      t('pages.home.workbench.internalTasks.archive'),
      { type: 'warning' },
    )
    actionTaskId.value = task.id
    const response = await archiveInternalTask(task.id)
    if (!response.success) throw new Error(response.message)
    ElMessage.success(t('pages.home.workbench.internalTasks.archiveSuccess'))
    emit('updated')
    await loadCurrentTab()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error instanceof Error && error.message ? error.message : t('pages.home.workbench.internalTasks.archiveFailed'))
    }
  } finally {
    actionTaskId.value = null
  }
}

const changePage = (page: number) => void loadCurrentTab(page - 1)
const handleDrawerOpen = () => void loadCurrentTab(0)
const handleRefresh = () => void loadCurrentTab(0)
const handleStatusChange = () => void loadCurrentTab(0)
const formatTime = (value: string) => new Date(value).toLocaleString()
const statusLabel = (status: InternalTaskStatus) =>
  t(`pages.home.workbench.internalTasks.status.${status.toLowerCase()}`)
const statusTagType = (status: InternalTaskStatus): 'success' | 'warning' | 'info' =>
  status === 'COMPLETED' ? 'success' : status === 'ASSIGNED' ? 'warning' : 'info'
const assigneeLabel = (assignee: InternalTaskAssigneeDTO) => {
  const type = assignee.employeeType === 'CLEANER'
    ? t('pages.home.workbench.internalTasks.cleaner')
    : assignee.employeeType === 'BOTH'
      ? t('pages.home.workbench.internalTasks.both')
      : t('pages.home.workbench.internalTasks.employee')
  return `${assignee.displayName} · ${type}`
}

watch(
  () => props.canManage,
  (canManage) => {
    activeStatus.value = canManage ? 'UNASSIGNED' : 'ASSIGNED'
    pageData.value = emptyPage()
  },
)

defineExpose({ openDrawer, openCreate, reset })
</script>

<style scoped>
.task-center-toolbar { display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; }
.task-center-toolbar :deep(.el-tabs) { flex: 1; }
.internal-task-list { min-height: 240px; }
.internal-task-card { display: flex; gap: 16px; justify-content: space-between; padding: 16px; margin-bottom: 12px; border: 1px solid #e4e7ed; border-radius: 12px; background: #fff; }
.task-card-main { min-width: 0; flex: 1; }
.task-card-title-row { display: flex; align-items: center; gap: 10px; }
.task-card-title-row h4 { margin: 0; font-size: 15px; color: #303133; }
.task-card-main p { margin: 8px 0; color: #606266; white-space: pre-wrap; }
.task-card-meta { display: flex; flex-wrap: wrap; gap: 6px 16px; color: #909399; font-size: 12px; }
.task-card-actions { display: flex; align-items: center; justify-content: flex-end; flex-wrap: wrap; gap: 8px; max-width: 310px; }
.task-card-actions :deep(.el-select) { width: 180px; }
@media (max-width: 640px) { .internal-task-card { flex-direction: column; } .task-card-actions { max-width: none; justify-content: flex-start; } }
</style>
