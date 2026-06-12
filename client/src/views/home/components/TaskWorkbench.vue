<template>
  <section class="task-workbench">
    <div class="workbench-header">
      <div class="title-block">
        <h3>{{ t('pages.home.workbench.title') }}</h3>
      </div>
      <el-button
        circle
        class="refresh-btn"
        :icon="Refresh"
        :loading="loading"
        :aria-label="t('pages.home.workbench.refresh')"
        @click="loadWorkbench"
      />
    </div>

    <div class="date-line">
      <span>{{ t('pages.home.workbench.todayLabel', { date: todayYmd }) }}</span>
      <el-button link type="primary" class="view-all-btn" @click="goToTaskList">
        {{ t('pages.home.workbench.viewAll') }}
        <el-icon><ArrowRight /></el-icon>
      </el-button>
    </div>

    <div class="type-strip" role="tablist" :aria-label="t('pages.home.workbench.typeFilter')">
      <button
        v-for="summary in taskTypeSummaries"
        :key="summary.type"
        class="type-chip"
        :class="{ active: activeType === summary.type }"
        type="button"
        role="tab"
        :aria-selected="activeType === summary.type"
        @click="handleTypeChange(summary.type)"
      >
        <span class="chip-icon">
          <img :src="getTypeIcon(summary.type)" :alt="summary.label" />
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
          <img :src="getTaskIcon(task.type)" :alt="task.title" />
        </div>

        <div class="task-main">
          <div class="task-copy">
            <h4>{{ task.title }}</h4>
            <p>{{ task.subtitle }}</p>
          </div>

          <div class="task-meta">
            <span v-for="item in task.metaItems" :key="item">{{ item }}</span>
          </div>

          <span class="task-status" :class="`status-${task.status}`">
            {{ getStatusLabel(task.status) }}
          </span>

          <el-select
            v-if="task.status === 'pending'"
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
            v-if="task.status === 'pending'"
            type="primary"
            size="small"
            class="assign-btn"
            :loading="assigningTaskId === task.sourceTaskId"
            @click="assignTask(task)"
          >
            {{ t('pages.home.workbench.assign') }}
          </el-button>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ArrowRight, Refresh } from '@element-plus/icons-vue'
import workbenchAllIcon from '@/assets/home/workbench-all.svg'
import workbenchCleanIcon from '@/assets/home/workbench-clean.svg'
import workbenchMessageIcon from '@/assets/home/workbench-message.svg'
import workbenchOrderIcon from '@/assets/home/workbench-order.svg'
import workbenchOtherIcon from '@/assets/home/workbench-other.svg'
import workbenchReviewIcon from '@/assets/home/workbench-review.svg'
import {
  useHomeTaskWorkbench,
  type WorkbenchStatusFilter,
  type WorkbenchTaskType,
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

const typeIconMap: Record<WorkbenchTaskTypeFilter, string> = {
  all: workbenchAllIcon,
  cleaning: workbenchCleanIcon,
  review: workbenchReviewIcon,
  order: workbenchOrderIcon,
  message: workbenchMessageIcon,
  other: workbenchOtherIcon,
}

const getTypeIcon = (type: WorkbenchTaskTypeFilter) => typeIconMap[type]

const getTaskIcon = (type: WorkbenchTaskType) => typeIconMap[type]

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
  --workbench-border: rgba(15, 23, 42, 0.06);
  --workbench-ink: #111111;
  --workbench-muted: rgba(0, 0, 0, 0.42);
  background: rgba(255, 255, 255, 0.94);
  border: 1px solid var(--workbench-border);
  border-radius: 20px;
  box-shadow:
    0 16px 36px rgba(15, 23, 42, 0.04),
    0 4px 10px rgba(15, 23, 42, 0.03);
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
  min-width: 0;
  width: 100%;
  height: 100%;
  min-height: 100%;
  padding: 16px 16px 14px;
  overflow: hidden;
}

.workbench-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.title-block {
  min-width: 0;
}

.title-block h3 {
  margin: 0;
  color: var(--workbench-ink);
  font-size: 19px;
  font-weight: 800;
  line-height: 1.2;
}

.refresh-btn {
  --el-fill-color-blank: #ffffff;
  --el-border-color: rgba(15, 23, 42, 0.08);
  --el-text-color-regular: rgba(0, 0, 0, 0.34);
  width: 28px;
  height: 28px;
  box-shadow: none;
}

.refresh-btn:hover {
  --el-border-color: rgba(76, 111, 255, 0.28);
  --el-text-color-regular: #4c6fff;
}

.date-line {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 10px;
  color: rgba(0, 0, 0, 0.38);
  font-size: 14px;
  line-height: 1.4;
}

.view-all-btn {
  padding-right: 0;
  color: rgba(0, 0, 0, 0.42);
  font-size: 14px;
  font-weight: 500;
}

.view-all-btn:hover {
  color: #4c6fff;
}

.type-strip {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 6px;
  margin-top: 16px;
}

.type-chip {
  display: flex;
  align-items: center;
  gap: 12px;
  min-height: 44px;
  padding: 10px 12px;
  background: #f7f8fb;
  border: 1px solid transparent;
  border-radius: 10px;
  text-align: left;
  cursor: pointer;
  transition:
    transform 0.2s ease,
    border-color 0.2s ease,
    background-color 0.2s ease,
    box-shadow 0.2s ease;
}

.type-chip:hover {
  transform: translateY(-1px);
  background: #f3f6ff;
}

.type-chip.active {
  background: rgba(76, 111, 255, 0.12);
  border-color: rgba(76, 111, 255, 0.72);
  box-shadow: inset 0 0 0 1px rgba(76, 111, 255, 0.08);
}

.chip-icon {
  flex: 0 0 24px;
  width: 26px;
  height: 26px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: visible;
}

.chip-icon img {
  width: 26px;
  height: 26px;
  object-fit: contain;
  display: block;
}

.chip-copy {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.chip-label,
.chip-count {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.chip-label {
  color: #111111;
  font-size: 14px;
  font-weight: 700;
  line-height: 1.25;
}

.chip-count {
  color: rgba(0, 0, 0, 0.42);
  font-size: 12px;
  line-height: 1.2;
}

.status-strip {
  display: flex;
  gap: 8px;
  margin-top: 16px;
  padding-bottom: 2px;
  overflow-x: auto;
}

.status-strip::-webkit-scrollbar {
  display: none;
}

.status-chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  flex: 0 0 auto;
  height: 30px;
  padding: 0 12px;
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 999px;
  background: #ffffff;
  color: rgba(0, 0, 0, 0.48);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition:
    color 0.2s ease,
    border-color 0.2s ease,
    background-color 0.2s ease,
    box-shadow 0.2s ease;
}

.status-chip span,
.status-chip strong {
  display: inline-flex;
  align-items: center;
  line-height: 1;
}

.status-chip strong {
  color: #111111;
  font-size: 13px;
  font-weight: 700;
}

.status-chip.active {
  color: #ffffff;
  background: #597ef7;
  border-color: #597ef7;
  box-shadow: none;
}

.status-chip.active strong {
  color: #ffffff;
}

.task-list-shell {
  flex: 1;
  min-height: 0;
  margin-top: 14px;
  padding-top: 2px;
  overflow-y: auto;
  overflow-x: hidden;
}

.task-list-shell :deep(.el-empty) {
  padding: 44px 0;
}

.task-list-shell :deep(.el-empty__description p) {
  color: rgba(0, 0, 0, 0.38);
}

.task-row {
  position: relative;
  display: flex;
  gap: 12px;
  padding: 8px 12px 12px 0;
  margin-bottom: 10px;
  border-radius: 12px;
  background: #ffffff;
  border: 1px solid rgba(15, 23, 42, 0.08);
  overflow: hidden;
}

.task-row::before {
  content: '';
  position: absolute;
  left: 0;
  top: 12px;
  bottom: 12px;
  width: 2px;
  border-radius: 0 999px 999px 0;
  background: #4973fe;
}

.task-marker {
  flex: 0 0 40px;
  width: 40px;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding-top: 12px;
  overflow: visible;
}

.task-marker img {
  width: 24px;
  height: 24px;
  object-fit: contain;
  display: block;
}

.task-main {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 64px;
  grid-template-areas:
    'copy copy'
    'meta status'
    'select button';
  align-items: start;
  column-gap: 8px;
  row-gap: 8px;
  flex: 1;
  min-width: 0;
  padding-top: 8px;
  padding-right: 12px;
}

.task-copy {
  grid-area: copy;
  min-width: 0;
}

.task-copy h4,
.task-copy p {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.task-copy h4 {
  margin: 0;
  color: #111111;
  font-size: 15px;
  font-weight: 700;
  line-height: 1.35;
}

.task-copy p {
  margin: 2px 0 0;
  color: rgba(0, 0, 0, 0.42);
  font-size: 13px;
  line-height: 1.4;
}

.task-status {
  grid-area: status;
  justify-self: center;
  align-self: start;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 58px;
  max-width: 100%;
  padding: 5px 10px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 700;
  line-height: 1;
  text-align: center;
  white-space: nowrap;
}

.task-status.status-pending {
  color: #f80e0e;
  background: rgba(248, 14, 14, 0.05);
}

.task-status.status-assigned {
  color: #57b78d;
  background: rgba(236, 255, 247, 0.9);
}

.task-status.status-in_progress {
  color: #b99a3b;
  background: rgba(185, 154, 59, 0.12);
}

.task-status.status-completed {
  color: #959292;
  background: #e9e9e9;
}

.task-status.status-expired {
  color: #111111;
  background: rgba(0, 0, 0, 0.05);
}

.task-meta {
  grid-area: meta;
  display: flex;
  flex-wrap: wrap;
  gap: 8px 14px;
  color: rgba(0, 0, 0, 0.42);
  font-size: 13px;
  line-height: 1.5;
  min-width: 0;
}

.assign-select {
  grid-area: select;
  width: 100%;
}

.assign-select :deep(.el-select__wrapper) {
  min-height: 32px;
  border-radius: 8px;
  box-shadow: 0 0 0 1px rgba(15, 23, 42, 0.08) inset;
}

.assign-select :deep(.el-select__wrapper.is-focused) {
  box-shadow: 0 0 0 1px rgba(94, 127, 255, 0.44) inset;
}

.assign-select :deep(.el-select__placeholder) {
  color: rgba(0, 0, 0, 0.28);
}

.assign-btn {
  grid-area: button;
  width: 100%;
  min-width: 56px;
  border-radius: 8px;
  border: 0;
  background: #597ef7;
  box-shadow: none;
}

.assign-btn:hover,
.assign-btn:focus-visible {
  background: #4f73eb;
}

@media (max-width: 768px) {
  .task-workbench {
    padding: 16px 14px 14px;
  }

  .type-strip {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 480px) {
  .type-strip {
    grid-template-columns: 1fr;
  }

  .task-main {
    grid-template-columns: 1fr;
    grid-template-areas:
      'copy'
      'meta'
      'status'
      'select'
      'button';
  }

  .date-line {
    flex-direction: column;
    align-items: flex-start;
  }

  .task-status,
  .assign-btn {
    justify-self: start;
    width: auto;
  }
}
</style>
