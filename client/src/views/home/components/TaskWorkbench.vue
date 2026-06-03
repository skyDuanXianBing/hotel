<template>
  <section class="task-workbench">
    <div class="workbench-header">
      <div class="title-block">
        <span class="eyebrow">{{ t('pages.home.workbench.eyebrow') }}</span>
        <h3>{{ t('pages.home.workbench.title') }}</h3>
      </div>
      <el-button
        circle
        :icon="Refresh"
        :loading="loading"
        :aria-label="t('pages.home.workbench.refresh')"
        @click="loadWorkbench"
      />
    </div>

    <div class="date-line">
      <span>{{ t('pages.home.workbench.todayLabel', { date: todayYmd }) }}</span>
      <el-button link type="primary" @click="goToTaskList">
        {{ t('pages.home.workbench.viewAll') }}
        <el-icon><ArrowRight /></el-icon>
      </el-button>
    </div>

    <div class="type-strip" role="tablist" :aria-label="t('pages.home.workbench.typeFilter')">
      <button
        v-for="summary in taskTypeSummaries"
        :key="summary.type"
        class="type-chip"
        :class="[`type-${summary.type}`, { active: activeType === summary.type }]"
        type="button"
        role="tab"
        :aria-selected="activeType === summary.type"
        @click="handleTypeChange(summary.type)"
      >
        <span class="chip-icon">
          <el-icon><component :is="getTypeIcon(summary.type)" /></el-icon>
        </span>
        <span class="chip-copy">
          <span class="chip-label">{{ summary.label }}</span>
          <span class="chip-count">
            {{ summary.connected ? summary.count : t('pages.home.workbench.notConnectedShort') }}
          </span>
        </span>
      </button>
    </div>

    <div
      v-if="selectedTypeIsConnected"
      class="status-strip"
      role="tablist"
      :aria-label="t('pages.home.workbench.statusFilter')"
    >
      <button
        v-for="summary in statusSummaries"
        :key="summary.status"
        class="status-chip"
        :class="{ active: activeStatus === summary.status }"
        type="button"
        role="tab"
        :aria-selected="activeStatus === summary.status"
        @click="activeStatus = summary.status"
      >
        <span>{{ summary.label }}</span>
        <strong>{{ summary.count }}</strong>
      </button>
    </div>

    <div v-loading="loading" class="task-list-shell">
      <el-empty
        v-if="!selectedTypeIsConnected"
        :description="
          t('pages.home.workbench.futureSourceEmpty', { source: selectedTypeSummary.label })
        "
      />
      <el-empty
        v-else-if="filteredTasks.length === 0"
        :description="t('pages.home.workbench.emptyToday')"
      />

      <div
        v-for="task in filteredTasks"
        v-else
        :key="task.id"
        class="task-row"
        :class="[`priority-${task.priority}`]"
      >
        <div class="task-marker">
          <el-icon><EditPen /></el-icon>
        </div>
        <div class="task-main">
          <div class="task-title-row">
            <div class="task-copy">
              <h4>{{ task.title }}</h4>
              <p>{{ task.subtitle }}</p>
            </div>
            <el-tag :type="getStatusTagType(task.status)" size="small">
              {{ getStatusLabel(task.status) }}
            </el-tag>
          </div>

          <div class="task-meta">
            <span v-for="item in task.metaItems" :key="item">{{ item }}</span>
          </div>

          <div v-if="task.status === 'pending'" class="assign-row">
            <el-select
              v-model="assignSelections[task.sourceTaskId]"
              :placeholder="t('pages.home.workbench.selectEmployee')"
              :loading="cleanersLoading"
              size="small"
              filterable
              class="assign-select"
            >
              <el-option
                v-for="cleaner in cleanerList"
                :key="cleaner.id"
                :label="cleaner.name"
                :value="cleaner.id"
              />
            </el-select>
            <el-button
              type="primary"
              size="small"
              :loading="assigningTaskId === task.sourceTaskId"
              @click="assignTask(task)"
            >
              {{ t('pages.home.workbench.assign') }}
            </el-button>
          </div>
        </div>
      </div>
    </div>

  </section>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import {
  ArrowRight,
  Document,
  EditPen,
  Message,
  Refresh,
  User,
} from '@element-plus/icons-vue'
import {
  useHomeTaskWorkbench,
  type WorkbenchStatusFilter,
  type WorkbenchTaskTypeFilter,
} from '@/views/home/composables/useHomeTaskWorkbench'

const router = useRouter()
const { t } = useI18n()

const {
  activeStatus,
  activeType,
  assignSelections,
  assigningTaskId,
  cleanerList,
  cleanersLoading,
  filteredTasks,
  loadWorkbench,
  loading,
  selectedTypeIsConnected,
  selectedTypeSummary,
  statusSummaries,
  taskTypeSummaries,
  todayYmd,
  assignTask,
} = useHomeTaskWorkbench()

const getTypeIcon = (type: WorkbenchTaskTypeFilter) => {
  const iconMap = {
    all: Document,
    cleaning: EditPen,
    review: Document,
    order: Document,
    message: Message,
    other: User,
  }
  return iconMap[type]
}

const getStatusLabel = (status: string) => {
  const statusMap: Record<string, string> = {
    expired: t('pages.home.workbench.statuses.expired'),
    pending: t('pages.home.workbench.statuses.pending'),
    assigned: t('pages.home.workbench.statuses.assigned'),
    in_progress: t('pages.home.workbench.statuses.inProgress'),
    completed: t('pages.home.workbench.statuses.completed'),
  }
  return statusMap[status] || status
}

const getStatusTagType = (status: string) => {
  const typeMap: Record<string, 'success' | 'warning' | 'info' | 'primary' | 'danger'> = {
    expired: 'info',
    pending: 'warning',
    assigned: 'primary',
    in_progress: 'primary',
    completed: 'success',
  }
  return typeMap[status] || 'info'
}

const handleTypeChange = (type: WorkbenchTaskTypeFilter) => {
  activeType.value = type
  activeStatus.value = 'all' as WorkbenchStatusFilter
}

const goToTaskList = () => {
  router.push('/accommodation/cleaning/task-list')
}

onMounted(() => {
  loadWorkbench()
})
</script>

<style scoped>
.task-workbench {
  --workbench-border: #e5e7eb;
  --workbench-muted: #6b7280;
  --workbench-ink: #1f2937;
  --workbench-soft: #f8fafc;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.08);
  display: flex;
  flex-direction: column;
  height: 550px;
  min-width: 0;
  padding: 18px;
}

.workbench-header {
  align-items: flex-start;
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.title-block {
  min-width: 0;
}

.eyebrow {
  color: #0f766e;
  display: block;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0;
  line-height: 1.2;
  margin-bottom: 5px;
}

.title-block h3 {
  color: var(--workbench-ink);
  font-size: 18px;
  font-weight: 700;
  line-height: 1.25;
  margin: 0;
}

.date-line {
  align-items: center;
  color: var(--workbench-muted);
  display: flex;
  font-size: 13px;
  justify-content: space-between;
  margin-top: 12px;
}

.date-line :deep(.el-button) {
  align-items: center;
  display: inline-flex;
  gap: 2px;
  padding-right: 0;
}

.type-strip {
  display: grid;
  gap: 8px;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  margin-top: 16px;
}

.type-chip {
  align-items: center;
  background: #f8fafc;
  border: 1px solid transparent;
  border-radius: 8px;
  color: var(--workbench-ink);
  cursor: pointer;
  display: flex;
  gap: 8px;
  min-height: 52px;
  min-width: 0;
  padding: 9px;
  text-align: left;
  transition:
    background-color 0.2s ease,
    border-color 0.2s ease,
    transform 0.2s ease;
}

.type-chip:hover {
  background: #f1f5f9;
  transform: translateY(-1px);
}

.type-chip.active {
  background: #eefdf8;
  border-color: #5eead4;
}

.chip-icon {
  align-items: center;
  border-radius: 8px;
  color: #0f766e;
  display: flex;
  flex: 0 0 28px;
  height: 28px;
  justify-content: center;
}

.type-order .chip-icon {
  color: #b45309;
}

.type-message .chip-icon {
  color: #2563eb;
}

.type-review .chip-icon {
  color: #7c3aed;
}

.type-other .chip-icon {
  color: #64748b;
}

.chip-copy {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.chip-label,
.chip-count {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.chip-label {
  font-size: 13px;
  font-weight: 600;
}

.chip-count {
  color: var(--workbench-muted);
  font-size: 12px;
}

.status-strip {
  display: flex;
  gap: 6px;
  margin-top: 14px;
  overflow-x: auto;
  padding-bottom: 2px;
}

.status-chip {
  align-items: center;
  background: transparent;
  border: 1px solid var(--workbench-border);
  border-radius: 999px;
  color: #4b5563;
  cursor: pointer;
  display: inline-flex;
  flex: 0 0 auto;
  font-size: 12px;
  gap: 6px;
  height: 30px;
  padding: 0 10px;
}

.status-chip strong {
  color: var(--workbench-ink);
  font-size: 12px;
}

.status-chip.active {
  background: #111827;
  border-color: #111827;
  color: #fff;
}

.status-chip.active strong {
  color: #fff;
}

.task-list-shell {
  border-top: 1px solid var(--workbench-border);
  flex: 1;
  margin-top: 14px;
  min-height: 0;
  overflow-y: auto;
  padding-top: 12px;
}

.task-list-shell :deep(.el-empty) {
  padding: 36px 0;
}

.task-row {
  border: 1px solid var(--workbench-border);
  border-radius: 8px;
  display: flex;
  gap: 10px;
  margin-bottom: 10px;
  min-width: 0;
  padding: 12px;
}

.task-row.priority-warning {
  border-left: 4px solid #f59e0b;
}

.task-row.priority-danger {
  border-left: 4px solid #94a3b8;
}

.task-row.priority-normal {
  border-left: 4px solid #3b82f6;
}

.task-row.priority-success {
  border-left: 4px solid #22c55e;
}

.task-marker {
  align-items: center;
  background: #ecfeff;
  border-radius: 8px;
  color: #0f766e;
  display: flex;
  flex: 0 0 34px;
  height: 34px;
  justify-content: center;
}

.task-main {
  flex: 1;
  min-width: 0;
}

.task-title-row {
  align-items: flex-start;
  display: flex;
  gap: 8px;
  justify-content: space-between;
}

.task-copy {
  min-width: 0;
}

.task-copy h4,
.task-copy p {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.task-copy h4 {
  color: var(--workbench-ink);
  font-size: 14px;
  font-weight: 700;
  line-height: 1.35;
  margin: 0;
}

.task-copy p {
  color: var(--workbench-muted);
  font-size: 12px;
  line-height: 1.35;
  margin: 3px 0 0;
}

.task-meta {
  color: var(--workbench-muted);
  display: flex;
  flex-wrap: wrap;
  font-size: 12px;
  gap: 6px 10px;
  margin-top: 8px;
}

.task-meta span {
  min-width: 0;
}

.assign-row {
  display: grid;
  gap: 8px;
  grid-template-columns: minmax(0, 1fr) auto;
  margin-top: 10px;
}

.assign-select {
  min-width: 0;
  width: 100%;
}

@media (max-width: 768px) {
  .task-workbench {
    height: auto;
    min-height: 520px;
  }

  .type-strip {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 480px) {
  .type-strip {
    grid-template-columns: 1fr;
  }

  .assign-row {
    grid-template-columns: 1fr;
  }
}
</style>
