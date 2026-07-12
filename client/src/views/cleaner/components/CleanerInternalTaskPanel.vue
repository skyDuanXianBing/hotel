<template>
  <section class="internal-task-panel">
    <header class="panel-header">
      <div>
        <h2>{{ t('pages.home.workbench.internalTasks.title') }}</h2>
        <span class="assigned-badge">{{ pageData.assignedCount }}</span>
      </div>
      <el-button :icon="Refresh" circle :loading="loading" @click="handleRefresh" />
    </header>
    <el-tabs v-model="activeStatus" stretch @tab-change="handleStatusChange">
      <el-tab-pane
        :label="`${t('pages.home.workbench.internalTasks.assigned')} (${pageData.assignedCount})`"
        name="ASSIGNED"
      />
      <el-tab-pane
        :label="`${t('pages.home.workbench.internalTasks.completed')} (${pageData.completedCount})`"
        name="COMPLETED"
      />
    </el-tabs>
    <div v-loading="loading" class="task-list">
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
      <article v-for="task in pageData.items" :key="task.id" class="task-card">
        <div class="task-content">
          <strong>{{ task.title }}</strong>
          <p v-if="task.description">{{ task.description }}</p>
          <small>
            {{ t('pages.home.workbench.internalTasks.creator') }}：{{ task.createdByName || '-' }}
          </small>
          <small v-if="task.completedAt">
            {{ t('pages.home.workbench.internalTasks.completedAt') }}：{{ formatTime(task.completedAt) }}
          </small>
        </div>
        <el-button
          v-if="task.canComplete && task.status === 'ASSIGNED'"
          type="success"
          size="small"
          :loading="completingTaskId === task.id"
          @click="completeTask(task)"
        >
          {{ t('pages.home.workbench.internalTasks.markComplete') }}
        </el-button>
      </article>
    </div>
  </section>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useI18n } from 'vue-i18n'
import {
  completeInternalTask,
  getMyInternalTasks,
  type InternalTaskDTO,
  type InternalTaskPageDTO,
} from '@/api/internalTask'

const { t } = useI18n()
const emit = defineEmits<{ completed: [] }>()
const emptyPage = (): InternalTaskPageDTO => ({
  items: [], page: 0, size: 20, totalElements: 0, assignedCount: 0, completedCount: 0,
})
const activeStatus = ref<'ASSIGNED' | 'COMPLETED'>('ASSIGNED')
const pageData = ref<InternalTaskPageDTO>(emptyPage())
const loading = ref(false)
const completingTaskId = ref<number | null>(null)
const initialLoadError = ref('')
const hasSuccessfulLoad = ref(false)
let requestSeq = 0

const reset = () => {
  requestSeq += 1
  pageData.value = emptyPage()
  loading.value = false
  completingTaskId.value = null
  initialLoadError.value = ''
  hasSuccessfulLoad.value = false
}

const loadTasks = async () => {
  const seq = ++requestSeq
  loading.value = true
  try {
    const response = await getMyInternalTasks({ status: activeStatus.value, page: 0, size: 20 })
    if (seq !== requestSeq) return
    if (!response.success || !response.data) throw new Error(response.message)
    pageData.value = response.data
    hasSuccessfulLoad.value = true
    initialLoadError.value = ''
  } catch (error) {
    if (seq === requestSeq) {
      const message = error instanceof Error && error.message ? error.message : t('pages.home.workbench.internalTasks.loadFailed')
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

const completeTask = async (task: InternalTaskDTO) => {
  try {
    await ElMessageBox.confirm(
      t('pages.home.workbench.internalTasks.completeConfirm'),
      t('pages.home.workbench.internalTasks.completeConfirmTitle'),
      { type: 'success' },
    )
    completingTaskId.value = task.id
    const response = await completeInternalTask(task.id)
    if (!response.success) throw new Error(response.message)
    ElMessage.success(t('pages.home.workbench.internalTasks.completeSuccess'))
    await loadTasks()
    emit('completed')
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error instanceof Error && error.message ? error.message : t('pages.home.workbench.internalTasks.completeFailed'))
      await loadTasks()
    }
  } finally {
    completingTaskId.value = null
  }
}

const formatTime = (value: string) => new Date(value).toLocaleString()
const handleRefresh = () => void loadTasks()
const handleStatusChange = () => void loadTasks()
defineExpose({ loadTasks, reset })
</script>

<style scoped>
.internal-task-panel { margin: 12px 20px 0; padding: 16px 20px; border-radius: 8px; background: #fff; box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05); }
.panel-header { display: flex; align-items: center; justify-content: space-between; }
.panel-header > div { display: flex; align-items: center; gap: 8px; }
.panel-header h2 { margin: 0; font-size: 17px; color: #303133; }
.assigned-badge { min-width: 22px; height: 22px; padding: 0 6px; border-radius: 999px; color: #fff; background: #409eff; font-size: 12px; line-height: 22px; text-align: center; }
.task-list { min-height: 80px; max-height: 340px; overflow: auto; }
.task-card { display: flex; align-items: center; justify-content: space-between; gap: 12px; padding: 12px 0; border-bottom: 1px solid #ebeef5; }
.task-card:last-child { border-bottom: 0; }
.task-content { min-width: 0; display: flex; flex-direction: column; gap: 5px; }
.task-content strong { color: #303133; }
.task-content p { margin: 0; color: #606266; white-space: pre-wrap; }
.task-content small { color: #909399; }
@media (max-width: 480px) { .internal-task-panel { margin: 10px 12px 0; padding: 14px; } }
</style>
