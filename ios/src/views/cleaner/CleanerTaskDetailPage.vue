<template>
  <ion-page>
    <ion-header translucent>
      <ion-toolbar>
        <ion-buttons slot="start">
          <ion-back-button :default-href="ROUTE_PATHS.cleanerDashboard" />
        </ion-buttons>
        <ion-title>任务详情</ion-title>
      </ion-toolbar>
    </ion-header>

    <ion-content fullscreen class="mobile-page cleaner-task-detail-page">
      <section class="mobile-hero cleaner-task-detail-page__hero">
        <p class="mobile-note cleaner-task-detail-page__eyebrow">Task Detail</p>
        <h1 class="mobile-title">{{ taskTitle }}</h1>
        <p class="mobile-subtitle">查看任务状态与详情，并按状态执行接受、拒绝或完成打扫。</p>
      </section>

      <div class="mobile-stack">
        <section class="mobile-card cleaner-task-detail-page__detail-card">
          <div class="cleaner-task-detail-page__detail-grid">
            <span>任务日期 {{ task?.taskDate || '-' }}</span>
            <span>房型 {{ task?.roomType || '-' }}</span>
            <span>房间 {{ task?.roomNumber || '-' }}</span>
            <span>任务类型 {{ taskTypeText }}</span>
            <span>任务状态 {{ taskStatusText }}</span>
            <span>预计时间 {{ task?.estimatedTime || '-' }}</span>
            <span>保洁员 {{ task?.cleanerName || cleanerDisplayName }}</span>
            <span>审批人 {{ task?.approverName || '-' }}</span>
            <span>开始时间 {{ task?.startTime || '-' }}</span>
            <span>完成时间 {{ task?.completeTime || '-' }}</span>
          </div>

          <p v-if="task?.notes" class="mobile-note">备注：{{ task.notes }}</p>
          <p v-if="errorMessage" class="mobile-note cleaner-task-detail-page__error">{{ errorMessage }}</p>
        </section>
      </div>

      <div class="cleaner-task-detail-page__footer">
        <ion-button
          v-if="task?.status === 'assigned'"
          fill="outline"
          color="danger"
          class="cleaner-task-detail-page__action"
          :disabled="submitting"
          @click="handleReject"
        >
          拒绝任务
        </ion-button>

        <ion-button
          v-if="task?.status === 'assigned'"
          class="cleaner-task-detail-page__action"
          :disabled="submitting"
          @click="handleAccept"
        >
          {{ submitting ? '提交中...' : '接受任务' }}
        </ion-button>

        <ion-button
          v-if="task?.status === 'in_progress'"
          class="cleaner-task-detail-page__action"
          :disabled="submitting"
          @click="handleComplete"
        >
          {{ submitting ? '提交中...' : '完成打扫' }}
        </ion-button>

        <ion-button v-if="!task || task.status === 'completed' || task.status === 'pending'" fill="outline" @click="handleBack">
          返回工作台
        </ion-button>
      </div>
    </ion-content>
  </ion-page>
</template>

<script setup lang="ts">
import {
  IonBackButton,
  IonButton,
  IonButtons,
  IonContent,
  IonHeader,
  IonPage,
  IonTitle,
  IonToolbar,
} from '@ionic/vue'
import { computed, ref } from 'vue'
import { onIonViewWillEnter } from '@ionic/vue'
import {
  acceptCleaningTask,
  completeCleaningTask,
  getCleaningTaskById,
  rejectCleaningTask,
  type CleaningTaskDTO,
} from '@/api/cleaning'
import { CLEANER_STATUS_LABELS, CLEANER_TASK_TYPE_LABELS } from '@/constants/cleaner'
import { ROUTE_PATHS } from '@/router/guards'
import { showErrorToast, showSuccessToast, showWarningToast } from '@/utils/notify'
import { isHandledRequestError } from '@/utils/request'
import { readCleanerUser } from '@/utils/cleanerSession'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

const cleanerUser = ref(readCleanerUser())
const task = ref<CleaningTaskDTO | null>(null)
const submitting = ref(false)
const errorMessage = ref('')

const cleanerDisplayName = computed(() => {
  if (cleanerUser.value?.nickname) {
    return cleanerUser.value.nickname
  }

  if (cleanerUser.value?.email) {
    return cleanerUser.value.email
  }

  return '保洁员'
})

const taskTitle = computed(() => {
  if (!task.value) {
    return '任务详情'
  }

  return `房间 ${task.value.roomNumber || '-'} · ${taskTypeText.value}`
})

const taskStatusText = computed(() => {
  if (!task.value?.status) {
    return '-'
  }

  return CLEANER_STATUS_LABELS[task.value.status] || task.value.status
})

const taskTypeText = computed(() => {
  if (!task.value?.taskType) {
    return '-'
  }

  return CLEANER_TASK_TYPE_LABELS[task.value.taskType] || task.value.taskType
})

function resolveTaskId() {
  const taskIdParam = route.params.id
  const taskIdText = Array.isArray(taskIdParam) ? taskIdParam[0] : taskIdParam
  const taskId = Number(taskIdText)

  if (!taskId) {
    return 0
  }

  return taskId
}

async function loadTaskDetail() {
  const taskId = resolveTaskId()
  if (!taskId) {
    showWarningToast('任务 ID 无效')
    await router.replace(ROUTE_PATHS.cleanerDashboard)
    return
  }

  errorMessage.value = ''

  try {
    const response = await getCleaningTaskById(taskId)
    if (!response.success || !response.data) {
      throw new Error(response.message || '加载任务详情失败')
    }

    task.value = response.data
  } catch (error) {
    task.value = null
    errorMessage.value = error instanceof Error ? error.message : '加载任务详情失败'

    if (!isHandledRequestError(error)) {
      showWarningToast(errorMessage.value)
    }
  }
}

async function handleAccept() {
  if (!task.value || submitting.value) {
    return
  }

  submitting.value = true

  try {
    const response = await acceptCleaningTask(task.value.id)
    if (!response.success) {
      throw new Error(response.message || '接受任务失败')
    }

    showSuccessToast('已接受任务')
    await loadTaskDetail()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showErrorToast(error instanceof Error ? error.message : '接受任务失败')
    }
  } finally {
    submitting.value = false
  }
}

async function handleReject() {
  if (!task.value || submitting.value) {
    return
  }

  if (typeof window !== 'undefined') {
    const confirmed = window.confirm('确定要拒绝这个任务吗？')
    if (!confirmed) {
      return
    }
  }

  submitting.value = true

  try {
    const response = await rejectCleaningTask(task.value.id)
    if (!response.success) {
      throw new Error(response.message || '拒绝任务失败')
    }

    showSuccessToast('已拒绝任务')
    await router.replace(ROUTE_PATHS.cleanerDashboard)
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showErrorToast(error instanceof Error ? error.message : '拒绝任务失败')
    }
  } finally {
    submitting.value = false
  }
}

async function handleComplete() {
  if (!task.value || submitting.value) {
    return
  }

  const cleanerId = cleanerUser.value?.id
  if (!cleanerId) {
    showWarningToast('未获取到当前保洁员信息')
    return
  }

  if (typeof window !== 'undefined') {
    const confirmed = window.confirm('确认已完成打扫吗？')
    if (!confirmed) {
      return
    }
  }

  submitting.value = true

  try {
    const response = await completeCleaningTask(task.value.id, cleanerId)
    if (!response.success) {
      throw new Error(response.message || '完成任务失败')
    }

    showSuccessToast('任务已完成')
    await router.replace(ROUTE_PATHS.cleanerDashboard)
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showErrorToast(error instanceof Error ? error.message : '完成任务失败')
    }
  } finally {
    submitting.value = false
  }
}

async function handleBack() {
  await router.replace(ROUTE_PATHS.cleanerDashboard)
}

onIonViewWillEnter(async () => {
  cleanerUser.value = readCleanerUser()

  if (!cleanerUser.value) {
    await router.replace(ROUTE_PATHS.cleanerLogin)
    return
  }

  await loadTaskDetail()
})
</script>

<style scoped>
.cleaner-task-detail-page {
  display: block;
  --padding-bottom: 100px;
}

.cleaner-task-detail-page__hero {
  margin-top: 4px;
}

.cleaner-task-detail-page__eyebrow {
  color: var(--ion-color-primary);
  font-weight: 700;
}

.cleaner-task-detail-page__detail-card {
  display: grid;
  gap: 14px;
}

.cleaner-task-detail-page__detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  color: var(--app-heading);
  font-size: 14px;
}

.cleaner-task-detail-page__error {
  color: var(--ion-color-danger);
}

.cleaner-task-detail-page__footer {
  position: sticky;
  bottom: 0;
  display: flex;
  gap: 10px;
  padding: 12px 16px calc(12px + env(safe-area-inset-bottom));
  background: rgba(255, 255, 255, 0.96);
  border-top: 1px solid var(--app-border);
}

.cleaner-task-detail-page__action {
  flex: 1;
}

@media (max-width: 520px) {
  .cleaner-task-detail-page__detail-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
