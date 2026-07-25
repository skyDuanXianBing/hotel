<template>
  <ion-page>
    <ion-header translucent>
      <ion-toolbar class="app-page-header__toolbar">
        <ion-buttons slot="start">
          <ion-back-button class="app-page-header__back-btn" :default-href="ROUTE_PATHS.cleanerDashboard" />
        </ion-buttons>
        <ion-title class="app-page-header__title">{{ $t('routes.CleanerTaskDetail') }}</ion-title>
      </ion-toolbar>
    </ion-header>

    <ion-content fullscreen class="mobile-page cleaner-task-detail-page">
      <section class="mobile-hero cleaner-task-detail-page__hero">
        <p class="mobile-note cleaner-task-detail-page__eyebrow">{{ $t('iosStage5.cleaning.taskDetails') }}</p>
        <h1 class="mobile-title">{{ taskTitle }}</h1>
        <p class="mobile-subtitle">{{ $t('iosStage5.cleanerWorkspace.detailSubtitle') }}</p>
      </section>

      <div class="mobile-stack">
        <section class="mobile-card cleaner-task-detail-page__detail-card">
          <div class="cleaner-task-detail-page__detail-grid">
            <span>{{ $t('iosStage5.cleanerWorkspace.taskDate') }} {{ task?.taskDate || '-' }}</span>
            <span>{{ $t('iosStage5.cleanerWorkspace.roomType') }} {{ task?.roomType || '-' }}</span>
            <span>{{ $t('iosStage5.cleanerWorkspace.room') }} {{ task?.roomNumber || '-' }}</span>
            <span>{{ $t('iosStage5.cleanerWorkspace.taskType') }} {{ taskTypeText }}</span>
            <span>{{ $t('iosStage5.cleanerWorkspace.taskStatus') }} {{ taskStatusText }}</span>
            <span>{{ $t('iosStage5.cleanerWorkspace.estimatedTime') }} {{ task?.estimatedTime || '-' }}</span>
            <span>{{ $t('iosStage5.cleanerWorkspace.cleaner') }} {{ task?.cleanerName || cleanerDisplayName }}</span>
            <span>{{ $t('iosStage5.cleanerWorkspace.approver') }} {{ task?.approverName || '-' }}</span>
            <span>{{ $t('iosStage5.cleanerWorkspace.startTime') }} {{ task?.startTime || '-' }}</span>
            <span>{{ $t('iosStage5.cleanerWorkspace.completeTime') }} {{ task?.completeTime || '-' }}</span>
          </div>

          <p v-if="task?.notes" class="mobile-note">{{ $t('iosStage5.cleanerWorkspace.notes') }}{{ task.notes }}</p>
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
          {{ $t('iosStage5.cleanerWorkspace.rejectTask') }}
        </ion-button>

        <ion-button
          v-if="task?.status === 'assigned'"
          class="cleaner-task-detail-page__action"
          :disabled="submitting"
          @click="handleAccept"
        >
          {{ submitting ? $t('iosStage5.cleaning.submitting') : $t('iosStage5.cleanerWorkspace.acceptTask') }}
        </ion-button>

        <ion-button
          v-if="task?.status === 'in_progress'"
          class="cleaner-task-detail-page__action"
          :disabled="submitting"
          @click="handleComplete"
        >
          {{ submitting ? $t('iosStage5.cleaning.submitting') : $t('iosStage5.cleanerWorkspace.completeCleaning') }}
        </ion-button>

        <ion-button v-if="!task || task.status === 'completed' || task.status === 'pending'" fill="outline" @click="handleBack">
          {{ $t('iosStage5.cleanerWorkspace.backToWorkspace') }}
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
import { useI18n } from 'vue-i18n'
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
const { t } = useI18n()

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

  return t('iosStage5.cleanerWorkspace.cleaner')
})

const taskTitle = computed(() => {
  if (!task.value) {
    return t('routes.CleanerTaskDetail')
  }

  return `${t('iosStage5.cleanerWorkspace.room')} ${task.value.roomNumber || '-'} · ${taskTypeText.value}`
})

const taskStatusText = computed(() => {
  if (!task.value?.status) {
    return '-'
  }

  const labelKey = CLEANER_STATUS_LABELS[task.value.status]
  return (labelKey ? t(labelKey) : '') || task.value.status
})

const taskTypeText = computed(() => {
  if (!task.value?.taskType) {
    return '-'
  }

  const labelKey = CLEANER_TASK_TYPE_LABELS[task.value.taskType]
  return (labelKey ? t(labelKey) : '') || task.value.taskType
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
    showWarningToast(t('iosStage5.cleanerWorkspace.invalidTaskId'))
    await router.replace(ROUTE_PATHS.cleanerDashboard)
    return
  }

  errorMessage.value = ''

  try {
    const response = await getCleaningTaskById(taskId)
    if (!response.success || !response.data) {
      throw new Error(response.message || t('iosStage5.cleanerWorkspace.detailLoadFailed'))
    }

    task.value = response.data
  } catch (error) {
    task.value = null
    errorMessage.value = error instanceof Error ? error.message : t('iosStage5.cleanerWorkspace.detailLoadFailed')

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
      throw new Error(response.message || t('iosStage5.cleanerWorkspace.acceptFailed'))
    }

    showSuccessToast(t('iosStage5.cleanerWorkspace.accepted'))
    await loadTaskDetail()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showErrorToast(error instanceof Error ? error.message : t('iosStage5.cleanerWorkspace.acceptFailed'))
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
    const confirmed = window.confirm(t('iosStage5.cleanerWorkspace.rejectConfirm'))
    if (!confirmed) {
      return
    }
  }

  submitting.value = true

  try {
    const response = await rejectCleaningTask(task.value.id)
    if (!response.success) {
      throw new Error(response.message || t('iosStage5.cleanerWorkspace.rejectFailed'))
    }

    showSuccessToast(t('iosStage5.cleanerWorkspace.rejected'))
    await router.replace(ROUTE_PATHS.cleanerDashboard)
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showErrorToast(error instanceof Error ? error.message : t('iosStage5.cleanerWorkspace.rejectFailed'))
    }
  } finally {
    submitting.value = false
  }
}

async function handleComplete() {
  if (!task.value || submitting.value) {
    return
  }

  const cleanerId = cleanerUser.value?.cleanerId
  if (!cleanerId) {
    showWarningToast(t('iosStage5.cleanerWorkspace.cleanerMissing'))
    return
  }

  if (typeof window !== 'undefined') {
    const confirmed = window.confirm(t('iosStage5.cleanerWorkspace.completeConfirm'))
    if (!confirmed) {
      return
    }
  }

  submitting.value = true

  try {
    const response = await completeCleaningTask(task.value.id, cleanerId)
    if (!response.success) {
      throw new Error(response.message || t('iosStage5.cleanerWorkspace.completeFailed'))
    }

    showSuccessToast(t('iosStage5.cleanerWorkspace.completed'))
    await router.replace(ROUTE_PATHS.cleanerDashboard)
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showErrorToast(error instanceof Error ? error.message : t('iosStage5.cleanerWorkspace.completeFailed'))
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
    await router.replace(ROUTE_PATHS.login)
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
  flex-wrap: wrap;
}

.cleaner-task-detail-page__action {
  flex: 1;
  min-width: min(150px, 100%);
}

@media (max-width: 520px) {
  .cleaner-task-detail-page__detail-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
