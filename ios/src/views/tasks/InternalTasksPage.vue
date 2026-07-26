<template>
  <ion-page>
    <ion-header translucent>
      <ion-toolbar>
        <ion-buttons slot="start">
          <ion-back-button :default-href="ROUTE_PATHS.home" />
        </ion-buttons>
        <ion-title>{{ t('routes.InternalTasks') }}</ion-title>
        <ion-buttons slot="end">
          <ion-button
            fill="clear"
            :aria-label="t('internalTasks.refresh')"
            :disabled="loading"
            @click="loadTasks(true)"
          >
            <ion-spinner v-if="loading" name="crescent" />
            <ion-icon v-else :icon="refreshOutline" />
          </ion-button>
        </ion-buttons>
      </ion-toolbar>
    </ion-header>

    <ion-content fullscreen class="mobile-page mobile-page--dashboard internal-tasks-page">
      <ion-refresher slot="fixed" @ionRefresh="handleRefresh">
        <ion-refresher-content :pulling-text="t('internalTasks.pullToRefresh')" />
      </ion-refresher>

      <div class="internal-tasks-page__shell">
        <header class="internal-tasks-page__summary">
          <p>{{ t('internalTasks.eyebrow') }}</p>
          <h1>{{ t('internalTasks.title') }}</h1>
          <span>{{ t('internalTasks.total', { count: total }) }}</span>
        </header>

        <div class="internal-tasks-page__filters" role="tablist" :aria-label="t('internalTasks.filterLabel')">
          <button
            v-for="filter in filters"
            :key="filter.value"
            type="button"
            role="tab"
            :aria-selected="activeStatus === filter.value"
            :class="{ 'is-active': activeStatus === filter.value }"
            @click="changeStatus(filter.value)"
          >
            {{ filter.label }}
            <span>{{ filter.count }}</span>
          </button>
        </div>

        <p v-if="loadError" class="internal-tasks-page__notice">
          {{ loadError }}
          <button type="button" @click="loadTasks(true)">{{ t('internalTasks.retry') }}</button>
        </p>

        <div v-if="loading && tasks.length === 0" class="internal-tasks-page__state">
          <ion-spinner name="crescent" />
          <span>{{ t('internalTasks.loading') }}</span>
        </div>

        <div v-else-if="tasks.length === 0 && !loadError" class="internal-tasks-page__state">
          <ion-icon :icon="checkmarkDoneCircleOutline" />
          <strong>{{ t('internalTasks.empty') }}</strong>
        </div>

        <div v-else class="internal-tasks-page__list">
          <article
            v-for="task in tasks"
            :key="task.id"
            class="internal-task-card"
            :class="{ 'is-focused': isFocusedTask(task) }"
          >
            <div class="internal-task-card__heading">
              <div>
                <strong>{{ task.title || t('internalTasks.untitled') }}</strong>
                <p v-if="task.subtitle">{{ task.subtitle }}</p>
              </div>
              <span :class="`is-${taskStatus(task)}`">{{ statusLabel(taskStatus(task)) }}</span>
            </div>

            <div v-if="formatMeta(task).length > 0" class="internal-task-card__meta">
              <span v-for="item in formatMeta(task)" :key="item">{{ item }}</span>
            </div>

            <button
              v-if="canComplete(task)"
              type="button"
              class="internal-task-card__complete"
              :disabled="completingTaskId === taskSourceId(task)"
              @click="confirmComplete(task)"
            >
              <ion-spinner v-if="completingTaskId === taskSourceId(task)" name="crescent" />
              <ion-icon v-else :icon="checkmarkCircleOutline" />
              <span>{{ t('internalTasks.complete') }}</span>
            </button>
          </article>
        </div>

        <button
          v-if="hasMore"
          type="button"
          class="internal-tasks-page__load-more"
          :disabled="loadingMore"
          @click="loadTasks(false)"
        >
          <ion-spinner v-if="loadingMore" name="crescent" />
          <span>{{ t('internalTasks.loadMore') }}</span>
        </button>
      </div>
    </ion-content>
  </ion-page>
</template>

<script setup lang="ts">
import {
  alertController,
  IonBackButton,
  IonButton,
  IonButtons,
  IonContent,
  IonHeader,
  IonIcon,
  IonPage,
  IonRefresher,
  IonRefresherContent,
  IonSpinner,
  IonTitle,
  IonToolbar,
  onIonViewWillEnter,
} from '@ionic/vue'
import {
  checkmarkCircleOutline,
  checkmarkDoneCircleOutline,
  refreshOutline,
} from 'ionicons/icons'
import { computed, onBeforeUnmount, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import {
  getHomeWorkbench,
  type HomeWorkbenchItemDTO,
  type HomeWorkbenchTaskTypeFilter,
} from '@/api/homeWorkbench'
import { completeInternalTask } from '@/api/internalTask'
import { ROUTE_PATHS } from '@/router/guards'
import {
  appendUniqueWorkbenchItems,
  formatWorkbenchMetaItem,
  getWorkbenchStatusLabelKey,
  hasWorkbenchAction,
  normalizeWorkbenchStatus,
} from '@/utils/homeWorkbench'
import { showSuccessToast, showWarningToast } from '@/utils/notify'
import { isHandledRequestError } from '@/utils/request'
import { getStoreTodayDate } from '@/utils/storeBusinessDate'

type TaskStatusFilter = 'all' | 'unassigned' | 'assigned' | 'completed'

const PAGE_SIZE = 50
const route = useRoute()
const { t } = useI18n()
const activeStatus = ref<TaskStatusFilter>('all')
const tasks = ref<HomeWorkbenchItemDTO[]>([])
const statusCounts = ref<Record<string, number>>({})
const total = ref(0)
const nextCursor = ref<string | null>(null)
const hasMore = ref(false)
const loading = ref(false)
const loadingMore = ref(false)
const loadError = ref('')
const completingTaskId = ref<number | null>(null)
let activeController: AbortController | null = null
let requestVersion = 0

const filters = computed(() =>
  (['all', 'unassigned', 'assigned', 'completed'] as const).map((value) => ({
    value,
    label: t(`internalTasks.statuses.${value}`),
    count: value === 'all' ? total.value : Number(statusCounts.value[value] || 0),
  })),
)

const focusedTaskId = computed(() => {
  const value = Array.isArray(route.query.taskId) ? route.query.taskId[0] : route.query.taskId
  return String(value || '').trim()
})

function taskSourceId(task: HomeWorkbenchItemDTO) {
  const value = Number(task.sourceId)
  return Number.isInteger(value) && value > 0 ? value : 0
}

function isFocusedTask(task: HomeWorkbenchItemDTO) {
  return Boolean(focusedTaskId.value && String(task.sourceId || '') === focusedTaskId.value)
}

function canComplete(task: HomeWorkbenchItemDTO) {
  return taskSourceId(task) > 0 && hasWorkbenchAction(task.actions, 'complete')
}

function taskStatus(task: HomeWorkbenchItemDTO) {
  return String(task.statusGroup || task.sourceStatus || 'unassigned').trim().toLowerCase()
}

function formatMeta(task: HomeWorkbenchItemDTO) {
  return (task.metaItems || [])
    .map(formatWorkbenchMetaItem)
    .filter((item): item is string => Boolean(item))
}

function statusLabel(status: string) {
  const normalized = normalizeWorkbenchStatus('other', status)
  const key = getWorkbenchStatusLabelKey('other', normalized)
  return key ? t(`tools.workbench.statuses.${key}`) : status.replace(/_/g, ' ')
}

function buildRequest(cursor?: string) {
  return {
    date: getStoreTodayDate(),
    size: PAGE_SIZE,
    type: 'other' as Exclude<HomeWorkbenchTaskTypeFilter, 'all'>,
    status: activeStatus.value === 'all' ? undefined : activeStatus.value,
    cursor,
    includeSummaries: !cursor,
  }
}

async function loadTasks(reset: boolean) {
  if (!reset && (!hasMore.value || !nextCursor.value || loadingMore.value)) {
    return
  }

  requestVersion += 1
  const version = requestVersion
  activeController?.abort()
  const controller = new AbortController()
  activeController = controller
  reset ? (loading.value = true) : (loadingMore.value = true)
  loadError.value = ''
  const cursor = reset ? undefined : nextCursor.value || undefined

  try {
    const response = await getHomeWorkbench(buildRequest(cursor), controller.signal)
    if (!response.success || !response.data) {
      throw new Error(response.message || t('internalTasks.loadFailed'))
    }
    if (activeController !== controller || version !== requestVersion) {
      return
    }

    tasks.value = reset
      ? response.data.items || []
      : appendUniqueWorkbenchItems(tasks.value, response.data.items || [])
    nextCursor.value = response.data.page?.nextCursor || null
    hasMore.value = Boolean(response.data.page?.hasMore && nextCursor.value)
    if (reset) {
      total.value = Number(response.data.page?.totalElements ?? response.data.total ?? tasks.value.length)
      statusCounts.value = Object.fromEntries(
        (response.data.statusSummaries || []).map((item) => [item.statusGroup, Number(item.count || 0)]),
      )
    }
  } catch (error) {
    if (error instanceof DOMException && error.name === 'AbortError') {
      return
    }
    loadError.value = error instanceof Error ? error.message : t('internalTasks.loadFailed')
    if (!isHandledRequestError(error)) {
      showWarningToast(loadError.value)
    }
  } finally {
    if (activeController === controller) {
      activeController = null
      loading.value = false
      loadingMore.value = false
    }
  }
}

async function changeStatus(status: TaskStatusFilter) {
  if (activeStatus.value === status) {
    return
  }
  activeStatus.value = status
  await loadTasks(true)
}

async function confirmComplete(task: HomeWorkbenchItemDTO) {
  const taskId = taskSourceId(task)
  if (!taskId || completingTaskId.value) {
    return
  }

  const alert = await alertController.create({
    header: t('internalTasks.complete'),
    message: t('internalTasks.confirmComplete', { task: task.title || t('internalTasks.untitled') }),
    buttons: [
      { text: t('order.mobile.actions.cancel'), role: 'cancel' },
      { text: t('internalTasks.complete'), role: 'confirm' },
    ],
  })
  await alert.present()
  const result = await alert.onDidDismiss()
  if (result.role !== 'confirm') {
    return
  }

  completingTaskId.value = taskId
  try {
    const response = await completeInternalTask(taskId)
    if (!response.success || !response.data) {
      throw new Error(response.message || t('internalTasks.completeFailed'))
    }
    showSuccessToast(t('internalTasks.completeSuccess'))
    await loadTasks(true)
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(error instanceof Error ? error.message : t('internalTasks.completeFailed'))
    }
  } finally {
    completingTaskId.value = null
  }
}

async function handleRefresh(event: CustomEvent) {
  await loadTasks(true)
  const target = event.target as HTMLIonRefresherElement | null
  await target?.complete()
}

onIonViewWillEnter(() => {
  void loadTasks(true)
})

onBeforeUnmount(() => {
  requestVersion += 1
  activeController?.abort()
})
</script>

<style scoped>
.internal-tasks-page {
  --padding-start: 0;
  --padding-end: 0;
  --padding-top: 0;
}

.internal-tasks-page__shell {
  display: grid;
  gap: 14px;
  width: min(100%, 760px);
  margin: 0 auto;
  padding: 18px 16px calc(34px + var(--app-safe-bottom));
}

.internal-tasks-page__summary {
  display: grid;
  gap: 6px;
  padding: 4px 2px 10px;
}

.internal-tasks-page__summary p,
.internal-tasks-page__summary h1 {
  margin: 0;
}

.internal-tasks-page__summary p {
  color: var(--ios-pms-primary-strong);
  font-size: 12px;
  font-weight: 700;
  text-transform: uppercase;
}

.internal-tasks-page__summary h1 {
  font-size: 24px;
  line-height: 1.2;
}

.internal-tasks-page__summary span {
  color: var(--ios-pms-text-muted);
  font-size: 13px;
}

.internal-tasks-page__filters {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  overflow: hidden;
  border: 1px solid var(--ios-pms-border-soft);
  border-radius: 8px;
  background: var(--ios-pms-surface);
}

.internal-tasks-page__filters button {
  display: grid;
  gap: 2px;
  min-width: 0;
  min-height: 50px;
  padding: 7px 4px;
  border: 0;
  border-right: 1px solid var(--ios-pms-border-soft);
  background: transparent;
  color: var(--ios-pms-text-muted);
  font: inherit;
  font-size: 12px;
}

.internal-tasks-page__filters button:last-child {
  border-right: 0;
}

.internal-tasks-page__filters button.is-active {
  background: var(--ios-pms-primary-soft);
  color: var(--ios-pms-primary-strong);
  font-weight: 700;
}

.internal-tasks-page__filters span {
  font-size: 11px;
}

.internal-tasks-page__list {
  display: grid;
  gap: 10px;
}

.internal-task-card {
  display: grid;
  gap: 12px;
  padding: 14px;
  border: 1px solid var(--ios-pms-border-soft);
  border-radius: 8px;
  background: var(--ios-pms-surface);
  box-shadow: var(--ios-pms-shadow-card);
}

.internal-task-card.is-focused {
  border-color: var(--ios-pms-primary);
  box-shadow: inset 0 0 0 1px var(--ios-pms-primary);
}

.internal-task-card__heading {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 12px;
  align-items: start;
}

.internal-task-card__heading strong,
.internal-task-card__heading p {
  overflow-wrap: anywhere;
}

.internal-task-card__heading p {
  margin: 5px 0 0;
  color: var(--ios-pms-text-muted);
  font-size: 13px;
  line-height: 1.5;
}

.internal-task-card__heading > span {
  padding: 4px 7px;
  border-radius: 6px;
  background: var(--ios-pms-surface-muted);
  color: var(--ios-pms-text-muted);
  font-size: 11px;
  white-space: nowrap;
}

.internal-task-card__heading > span.is-assigned {
  background: var(--ios-pms-primary-soft);
  color: var(--ios-pms-primary-strong);
}

.internal-task-card__heading > span.is-completed {
  background: rgba(34, 197, 94, 0.12);
  color: #167543;
}

.internal-task-card__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.internal-task-card__meta span {
  padding: 4px 7px;
  border-radius: 6px;
  background: var(--ios-pms-surface-muted);
  color: var(--ios-pms-text-muted);
  font-size: 11px;
}

.internal-task-card__complete,
.internal-tasks-page__load-more,
.internal-tasks-page__notice button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  min-height: 40px;
  padding: 0 14px;
  border: 0;
  border-radius: 8px;
  background: var(--ios-pms-primary);
  color: #fff;
  font: inherit;
  font-weight: 700;
}

.internal-task-card__complete {
  justify-self: end;
}

.internal-task-card__complete ion-icon,
.internal-task-card__complete ion-spinner {
  width: 18px;
  height: 18px;
}

.internal-tasks-page__load-more {
  justify-self: center;
  min-width: 150px;
}

.internal-tasks-page__state,
.internal-tasks-page__notice {
  display: grid;
  justify-items: center;
  gap: 10px;
  padding: 32px 16px;
  color: var(--ios-pms-text-muted);
  text-align: center;
}

.internal-tasks-page__state ion-icon {
  width: 34px;
  height: 34px;
  color: var(--ios-pms-primary);
}

.internal-tasks-page__notice button {
  margin-top: 8px;
}

@media (max-width: 380px) {
  .internal-tasks-page__filters button {
    font-size: 11px;
  }
}
</style>
