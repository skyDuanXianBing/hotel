<template>
  <section class="memo-workbench mobile-dashboard-surface">
    <div class="memo-workbench__header">
      <h2>{{ t('tools.workbench.title') }}</h2>
      <button
        type="button"
        class="memo-workbench__refresh"
        :aria-label="t('tools.workbench.refresh')"
        :disabled="loading"
        @click="loadWorkbench"
      >
        <ion-spinner v-if="loading" name="crescent" />
        <ion-icon v-else :icon="refreshOutline" />
      </button>
    </div>

    <div class="memo-workbench__date-line">
      <span>{{ t('tools.workbench.taskDate') }}</span>
      <strong>{{ todayYmd }}</strong>
      <button v-if="activeType !== 'all'" type="button" @click="handleViewAll">
        {{ t('tools.workbench.viewAll') }}
        <ion-icon :icon="chevronForwardOutline" />
      </button>
    </div>

    <div class="memo-workbench__memo-heading">
      <h3>{{ t('tools.memo') }}</h3>
      <span :class="{ 'is-saving': memoAutoSaving }">{{ memoStatusText }}</span>
    </div>

    <div v-if="memoLoading && !memoValue" class="memo-workbench__memo-skeleton">
      <ion-skeleton-text animated />
    </div>
    <ion-textarea
      v-else
      :model-value="memoValue"
      class="memo-workbench__textarea"
      fill="outline"
      :placeholder="t('home.section.memoPlaceholder')"
      :rows="2"
      @ionInput="handleMemoInput"
    />

    <div class="memo-workbench__divider" />

    <div
      class="memo-workbench__types"
      role="tablist"
      :aria-label="t('tools.workbench.typeFilter')"
    >
      <button
        v-for="summary in typeSummaries"
        :key="summary.type"
        type="button"
        class="memo-workbench__type"
        :class="{ 'is-active': activeType === summary.type }"
        role="tab"
        :aria-selected="activeType === summary.type"
        @click="changeType(summary.type)"
      >
        <ion-icon :icon="getTypeIcon(summary.type)" aria-hidden="true" />
        <span>
          <strong>{{ summary.label }}</strong>
          <small>
            {{
              summary.connected
                ? summary.count
                : t('tools.workbench.notConnectedShort')
            }}
          </small>
        </span>
      </button>
    </div>

    <div
      v-if="selectedTypeSummary?.connected"
      class="memo-workbench__statuses"
      role="tablist"
      :aria-label="t('tools.workbench.statusFilter')"
    >
      <button
        v-for="summary in statusSummaries"
        :key="summary.status"
        type="button"
        :class="{ 'is-active': activeStatus === summary.status }"
        role="tab"
        :aria-selected="activeStatus === summary.status"
        @click="changeStatus(summary.status)"
      >
        {{ summary.label }} {{ summary.count }}
      </button>
    </div>

    <div class="memo-workbench__tasks" aria-live="polite">
      <div v-if="loading && tasks.length === 0" class="memo-workbench__state">
        <ion-spinner name="crescent" />
        <span>{{ t('tools.workbench.loading') }}</span>
      </div>

      <div v-else-if="loadError" class="memo-workbench__state memo-workbench__state--error">
        <span>{{ loadError }}</span>
        <button type="button" @click="loadWorkbench">{{ t('tools.workbench.retry') }}</button>
      </div>

      <div
        v-else-if="!selectedTypeSummary?.connected"
        class="memo-workbench__state"
      >
        <span>
          {{
            t('tools.workbench.notConnected', {
              source: selectedTypeSummary?.label || '',
            })
          }}
        </span>
      </div>

      <div v-else-if="tasks.length === 0" class="memo-workbench__state">
        <span>{{ t('tools.workbench.emptyToday') }}</span>
      </div>

      <article
        v-for="task in tasks"
        v-else
        :key="`${task.type}:${task.id}`"
        class="memo-workbench__task"
        :class="{ 'is-clickable': canNavigateTask(task) }"
        @click="handleOpenTask(task)"
      >
        <div class="memo-workbench__task-icon">
          <ion-icon :icon="getTypeIcon(task.type)" aria-hidden="true" />
        </div>

        <div class="memo-workbench__task-body">
          <div class="memo-workbench__task-copy">
            <strong>{{ task.title }}</strong>
            <p v-if="task.subtitle">{{ task.subtitle }}</p>
          </div>

          <span
            class="memo-workbench__task-status"
            :class="`is-${sanitizeStatus(task.statusGroup)}`"
          >
            {{ getTaskStatusLabel(task) }}
          </span>

          <div v-if="task.metaItems.length > 0" class="memo-workbench__task-meta">
            <span v-for="item in task.metaItems" :key="item">{{ item }}</span>
          </div>

          <div
            v-if="task.canAssignCleaner"
            class="memo-workbench__assign"
            @click.stop
          >
            <label>
              <span>{{ t('tools.workbench.assignEmployee') }}</span>
              <select
                v-model="assignSelections[task.sourceTaskId]"
                :disabled="cleanersLoading || assigningTaskId === task.sourceTaskId"
              >
                <option :value="undefined">
                  {{ t('tools.workbench.selectEmployee') }}
                </option>
                <option
                  v-for="cleaner in cleanerList"
                  :key="cleaner.id"
                  :value="cleaner.id"
                >
                  {{ cleaner.name }}
                </option>
              </select>
            </label>
            <button
              type="button"
              :disabled="assigningTaskId === task.sourceTaskId"
              @click="assignTask(task)"
            >
              <ion-spinner
                v-if="assigningTaskId === task.sourceTaskId"
                name="crescent"
              />
              <span v-else>{{ t('tools.workbench.assign') }}</span>
            </button>
          </div>

          <div v-if="task.canComplete" class="memo-workbench__task-actions" @click.stop>
            <button
              type="button"
              :disabled="completingTaskId === task.sourceTaskId"
              @click="handleCompleteTask(task)"
            >
              <ion-spinner v-if="completingTaskId === task.sourceTaskId" name="crescent" />
              <ion-icon v-else :icon="checkmarkCircleOutline" />
              <span>{{ t('tools.workbench.complete') }}</span>
            </button>
          </div>
        </div>
      </article>

      <div v-if="hasMore" class="memo-workbench__load-more">
        <button type="button" :disabled="loadingMore" @click="loadMore">
          <ion-spinner v-if="loadingMore" name="crescent" />
          <span v-else>{{ t('tools.workbench.loadMore') }}</span>
        </button>
        <p v-if="loadMoreError">{{ loadMoreError }}</p>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import {
  alertController,
  IonIcon,
  IonSkeletonText,
  IonSpinner,
  IonTextarea,
} from '@ionic/vue'
import {
  appsOutline,
  chatbubblesOutline,
  checkmarkCircleOutline,
  chevronForwardOutline,
  clipboardOutline,
  ellipsisHorizontalCircleOutline,
  receiptOutline,
  refreshOutline,
  sparklesOutline,
} from 'ionicons/icons'
import { onBeforeUnmount, watch } from 'vue'
import { useRouter, type RouteLocationRaw } from 'vue-router'
import { useI18n } from 'vue-i18n'
import type {
  HomeWorkbenchTargetDTO,
  HomeWorkbenchTaskTypeFilter,
} from '@/api/homeWorkbench'
import {
  useHomeTaskWorkbench,
  type MobileWorkbenchTask,
} from '@/composables/useHomeTaskWorkbench'
import { buildMessageDetailPath, ROUTE_PATHS } from '@/router/guards'
import { resolveWorkbenchMessageThreadId } from '@/utils/homeWorkbench'

interface Props {
  isOpen: boolean
  memoValue: string
  memoLoading: boolean
  memoAutoSaving: boolean
  memoStatusText: string
}

const props = defineProps<Props>()

const emit = defineEmits<{
  dismiss: []
  'update:memoValue': [value: string]
}>()

const router = useRouter()
const { t } = useI18n()
const {
  activeStatus,
  activeType,
  assigningTaskId,
  completingTaskId,
  assignSelections,
  cleanerList,
  cleanersLoading,
  hasMore,
  loadError,
  loadMoreError,
  loading,
  loadingMore,
  selectedTypeSummary,
  statusSummaries,
  tasks,
  todayYmd,
  typeSummaries,
  assignTask,
  completeTask,
  changeStatus,
  changeType,
  dispose,
  getStatusLabel,
  initialize,
  loadMore,
  loadWorkbench,
} = useHomeTaskWorkbench()

const typeIconMap: Record<HomeWorkbenchTaskTypeFilter, string> = {
  all: appsOutline,
  cleaning: sparklesOutline,
  review: clipboardOutline,
  order: receiptOutline,
  message: chatbubblesOutline,
  other: ellipsisHorizontalCircleOutline,
}

const getTypeIcon = (type: HomeWorkbenchTaskTypeFilter) => typeIconMap[type]

const handleMemoInput = (event: CustomEvent<{ value?: string | null }>) => {
  emit('update:memoValue', String(event.detail.value || ''))
}

const handleCompleteTask = async (task: MobileWorkbenchTask) => {
  const alert = await alertController.create({
    header: t('tools.workbench.complete'),
    message: t('tools.workbench.confirmComplete', { task: task.title }),
    buttons: [
      { text: t('order.mobile.actions.cancel'), role: 'cancel' },
      {
        text: t('tools.workbench.complete'),
        role: 'confirm',
        handler: () => {
          void completeTask(task)
        },
      },
    ],
  })
  await alert.present()
}

const sanitizeStatus = (status: string) => {
  return status.replace(/[^a-zA-Z0-9_-]/g, '_')
}

const getTaskStatusLabel = (task: MobileWorkbenchTask) => {
  if (task.type === 'order' && task.statusGroup === 'pending') {
    return task.sourceStatus.toUpperCase() === 'UNASSIGNED'
      ? t('tools.workbench.statuses.orderUnassigned')
      : t('tools.workbench.statuses.awaitingCheckIn')
  }
  return getStatusLabel(task.statusGroup, task.type)
}

const normalizeRouteRecord = (
  source?: Record<string, string | number | boolean | null | undefined>,
) => {
  const result: Record<string, string> = {}
  for (const [key, value] of Object.entries(source || {})) {
    if (value !== undefined && value !== null && value !== '') {
      result[key] = String(value)
    }
  }
  return result
}

const routeNameMap: Record<string, string> = {
  CleaningTaskList: 'RoomsCleaningTasks',
  DataCenterRegistrations: 'RegistrationReviews',
  DataCenterRegistrationDetail: 'RegistrationReviewDetail',
  Order: 'Orders',
  Messages: 'Messages',
}

const pathMap: Record<string, string> = {
  '/accommodation/cleaning/task-list': ROUTE_PATHS.roomsCleaningTasks,
  '/data-center/registrations': ROUTE_PATHS.reviews,
  '/order': ROUTE_PATHS.orders,
  '/messages': ROUTE_PATHS.messages,
  '/internal-tasks': ROUTE_PATHS.internalTasks,
}

const resolveTargetRoute = (
  target?: string | HomeWorkbenchTargetDTO | null,
): RouteLocationRaw | null => {
  if (!target) {
    return null
  }

  if (typeof target === 'string') {
    const mappedName = routeNameMap[target]
    if (mappedName) {
      return { name: mappedName }
    }
    const mappedPath = pathMap[target]
    if (mappedPath) {
      return { path: mappedPath }
    }
    if (target.startsWith('/data-center/registrations/')) {
      return {
        name: 'RegistrationReviewDetail',
        params: { formId: target.split('/').filter(Boolean).at(-1) || '' },
      }
    }
    return null
  }

  const routeName = target.routeName || target.name || ''
  const mappedName = routeNameMap[routeName]
  const query = {
    ...normalizeRouteRecord(target.query),
    ...normalizeRouteRecord({
      reservationId: target.reservationId,
      orderNumber: target.orderNumber,
      channelOrderNumber: target.channelOrderNumber,
      guestName: target.guestName,
      suThreadId: target.suThreadId,
    }),
  }
  const messageThreadId = resolveWorkbenchMessageThreadId(target)
  if (messageThreadId) {
    return { path: buildMessageDetailPath(messageThreadId) }
  }
  if (mappedName) {
    return {
      name: mappedName,
      params: normalizeRouteRecord(target.params),
      query,
    }
  }

  const routePath = target.routePath || target.path || ''
  const mappedPath = pathMap[routePath]
  return mappedPath ? { path: mappedPath, query } : null
}

const resolveTaskRoute = (task: MobileWorkbenchTask): RouteLocationRaw | null => {
  if (task.type === 'message') {
    const messageThreadId = resolveWorkbenchMessageThreadId(task.target, task.sourceId)
    if (messageThreadId) {
      return { path: buildMessageDetailPath(messageThreadId) }
    }
  }

  const targetRoute = resolveTargetRoute(task.target)
  if (targetRoute) {
    return targetRoute
  }
  if (task.type === 'cleaning') {
    return { path: ROUTE_PATHS.roomsCleaningTasks }
  }
  if (task.type === 'review') {
    return task.sourceTaskId > 0
      ? {
          name: 'RegistrationReviewDetail',
          params: { formId: String(task.sourceTaskId) },
        }
      : { path: ROUTE_PATHS.reviews }
  }
  if (task.type === 'order') {
    return {
      path: ROUTE_PATHS.orders,
      query: {
        type: task.sourceStatus.toUpperCase() === 'UNASSIGNED' ? 'unassigned' : 'pending',
      },
    }
  }
  if (task.type === 'message') {
    return { path: ROUTE_PATHS.messages }
  }
  return null
}

const canNavigateTask = (task: MobileWorkbenchTask) => Boolean(resolveTaskRoute(task))

const navigateAfterDismiss = (route: RouteLocationRaw) => {
  emit('dismiss')
  window.setTimeout(() => {
    void router.push(route)
  }, 180)
}

const handleOpenTask = (task: MobileWorkbenchTask) => {
  const route = resolveTaskRoute(task)
  if (route) {
    navigateAfterDismiss(route)
  }
}

const handleViewAll = () => {
  if (activeType.value === 'review') {
    navigateAfterDismiss({ path: ROUTE_PATHS.reviews })
    return
  }
  if (activeType.value === 'order') {
    navigateAfterDismiss({ path: ROUTE_PATHS.orders })
    return
  }
  if (activeType.value === 'message') {
    navigateAfterDismiss({ path: ROUTE_PATHS.messages })
    return
  }
  if (activeType.value === 'other') {
    navigateAfterDismiss({ path: ROUTE_PATHS.internalTasks })
    return
  }
  navigateAfterDismiss({ path: ROUTE_PATHS.roomsCleaningTasks })
}

watch(
  () => props.isOpen,
  (isOpen) => {
    if (isOpen) {
      void initialize()
    } else {
      dispose()
    }
  },
  { immediate: true },
)

onBeforeUnmount(dispose)
</script>

<style scoped>
.memo-workbench {
  box-sizing: border-box;
  width: 100%;
  padding: var(--ios-pms-space-5);
  border-radius: var(--ios-pms-radius-card);
  background: rgba(255, 255, 255, 0.9);
  box-shadow: var(--ios-pms-shadow-card-strong);
}

.memo-workbench__header,
.memo-workbench__date-line,
.memo-workbench__memo-heading {
  display: flex;
  align-items: center;
}

.memo-workbench__header {
  justify-content: space-between;
  gap: var(--ios-pms-space-3);
}

.memo-workbench__header h2,
.memo-workbench__memo-heading h3 {
  margin: 0;
  color: var(--ios-pms-text-primary);
  letter-spacing: 0;
}

.memo-workbench__header h2 {
  font-size: var(--ios-pms-font-title-xl-size);
  font-weight: var(--ios-pms-weight-bold);
}

.memo-workbench__refresh {
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  padding: 0;
  border: 1px solid rgba(116, 138, 185, 0.18);
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.78);
  color: var(--ios-pms-text-muted);
  font-size: 21px;
}

.memo-workbench__refresh ion-spinner {
  width: 18px;
  height: 18px;
}

.memo-workbench__date-line {
  gap: var(--ios-pms-space-3);
  margin-top: var(--ios-pms-space-5);
  color: var(--ios-pms-text-soft);
  font-size: 14px;
}

.memo-workbench__date-line strong {
  color: var(--ios-pms-text-muted);
  font-weight: 400;
}

.memo-workbench__date-line button {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  margin-left: auto;
  padding: 4px 0;
  border: 0;
  background: transparent;
  color: var(--ios-pms-text-muted);
  font: inherit;
}

.memo-workbench__memo-heading {
  gap: var(--ios-pms-space-2);
  margin-top: var(--ios-pms-space-5);
  margin-bottom: var(--ios-pms-space-2);
}

.memo-workbench__memo-heading h3 {
  font-size: var(--ios-pms-font-title-md-size);
  font-weight: var(--ios-pms-weight-bold);
}

.memo-workbench__memo-heading span {
  min-width: 0;
  color: var(--ios-pms-text-soft);
  font-size: var(--ios-pms-font-note-size);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.memo-workbench__memo-heading span.is-saving {
  color: var(--ios-pms-primary);
}

.memo-workbench__textarea {
  --background: rgba(251, 253, 255, 0.96);
  --border-color: transparent;
  --border-radius: var(--ios-pms-radius-input);
  --border-width: 0;
  --highlight-color-focused: transparent;
  --padding-start: 14px;
  --padding-end: 14px;
  --padding-top: 12px;
  --padding-bottom: 12px;
  display: block;
  min-height: 76px;
  border: 1px solid rgba(96, 120, 168, 0.48);
  border-radius: var(--ios-pms-radius-input);
  background: rgba(251, 253, 255, 0.96);
  box-shadow: 0 1px 2px rgba(48, 69, 112, 0.04);
  color: var(--ios-pms-text-secondary);
  font-size: 14px;
  transition:
    border-color 0.18s ease,
    box-shadow 0.18s ease;
}

.memo-workbench__textarea:focus-within {
  border-color: var(--ios-pms-primary);
  box-shadow: 0 0 0 2px rgba(52, 116, 246, 0.12);
}

.memo-workbench__textarea::part(native) {
  border: 0;
  box-shadow: none;
}

.memo-workbench__memo-skeleton {
  height: 76px;
  padding: 16px;
  border: 1px solid var(--ios-pms-border-soft);
  border-radius: var(--ios-pms-radius-input);
  background: var(--ios-pms-surface-strong);
}

.memo-workbench__divider {
  height: 1px;
  margin: var(--ios-pms-space-5) 0;
  background: rgba(116, 138, 185, 0.18);
}

.memo-workbench__types {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--ios-pms-space-2);
}

.memo-workbench__type {
  display: grid;
  grid-template-columns: 24px minmax(0, 1fr);
  align-items: center;
  gap: 6px;
  min-width: 0;
  min-height: 52px;
  padding: 6px 7px;
  border: 1px solid transparent;
  border-radius: 10px;
  background: rgba(244, 248, 254, 0.9);
  color: var(--ios-pms-text-primary);
  font: inherit;
  text-align: left;
}

.memo-workbench__type.is-active {
  border-color: rgba(52, 116, 246, 0.72);
  background: rgba(52, 116, 246, 0.12);
  box-shadow: inset 0 0 0 1px rgba(52, 116, 246, 0.06);
}

.memo-workbench__type > ion-icon {
  display: block;
  width: 24px;
  height: 24px;
  color: var(--ios-pms-primary-strong);
}

.memo-workbench__type > span {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.memo-workbench__type strong,
.memo-workbench__type small {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.memo-workbench__type strong {
  font-size: 13px;
  font-weight: var(--ios-pms-weight-bold);
}

.memo-workbench__type small {
  margin-top: 3px;
  color: var(--ios-pms-text-soft);
  font-size: 11px;
}

.memo-workbench__statuses {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
  margin-top: var(--ios-pms-space-4);
}

.memo-workbench__statuses button {
  min-height: 29px;
  padding: 0 10px;
  border: 1px solid rgba(116, 138, 185, 0.24);
  border-radius: var(--ios-pms-radius-pill);
  background: rgba(255, 255, 255, 0.76);
  color: var(--ios-pms-text-muted);
  font: inherit;
  font-size: 12px;
  white-space: nowrap;
}

.memo-workbench__statuses button.is-active {
  border-color: rgba(52, 116, 246, 0.1);
  background: rgba(52, 116, 246, 0.1);
  color: var(--ios-pms-primary);
}

.memo-workbench__tasks {
  display: grid;
  gap: var(--ios-pms-space-3);
  margin-top: var(--ios-pms-space-5);
}

.memo-workbench__task {
  display: grid;
  grid-template-columns: 34px minmax(0, 1fr);
  gap: var(--ios-pms-space-3);
  padding: var(--ios-pms-space-3);
  border: 1px solid rgba(116, 138, 185, 0.22);
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.8);
}

.memo-workbench__task.is-clickable:active {
  background: var(--ios-pms-primary-soft);
}

.memo-workbench__task-icon {
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  border-radius: 8px;
  background: rgba(246, 248, 254, 0.96);
}

.memo-workbench__task-icon ion-icon {
  display: block;
  width: 24px;
  height: 24px;
  color: var(--ios-pms-primary-strong);
}

.memo-workbench__task-body {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  grid-template-areas:
    'copy status'
    'meta meta'
    'assign assign';
  gap: 7px var(--ios-pms-space-2);
  min-width: 0;
}

.memo-workbench__task-copy {
  grid-area: copy;
  min-width: 0;
}

.memo-workbench__task-copy strong,
.memo-workbench__task-copy p {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.memo-workbench__task-copy strong {
  display: block;
  color: #111827;
  font-size: 14px;
  font-weight: var(--ios-pms-weight-heavy);
  line-height: 1.3;
}

.memo-workbench__task-copy p {
  margin: 3px 0 0;
  color: var(--ios-pms-text-soft);
  font-size: 11px;
  line-height: 1.35;
}

.memo-workbench__task-status {
  grid-area: status;
  align-self: start;
  padding: 4px 8px;
  border-radius: var(--ios-pms-radius-pill);
  background: rgba(0, 0, 0, 0.05);
  color: var(--ios-pms-text-secondary);
  font-size: 11px;
  font-weight: var(--ios-pms-weight-bold);
  white-space: nowrap;
}

.memo-workbench__task-status.is-pending {
  background: rgba(217, 92, 92, 0.09);
  color: var(--ion-color-danger);
}

.memo-workbench__task-status.is-awaiting_review,
.memo-workbench__task-status.is-awaiting_reply,
.memo-workbench__task-status.is-unassigned {
  background: rgba(227, 139, 24, 0.12);
  color: var(--ion-color-warning-shade);
}

.memo-workbench__task-status.is-assigned {
  background: rgba(23, 166, 115, 0.1);
  color: var(--ion-color-success);
}

.memo-workbench__task-status.is-in_progress {
  background: rgba(180, 145, 40, 0.11);
  color: #9a7919;
}

.memo-workbench__task-status.is-completed {
  background: rgba(138, 152, 176, 0.13);
  color: var(--ios-pms-text-muted);
}

.memo-workbench__task-meta {
  grid-area: meta;
  display: flex;
  flex-wrap: wrap;
  gap: 4px 10px;
  min-width: 0;
  color: var(--ios-pms-text-soft);
  font-size: 11px;
  line-height: 1.4;
}

.memo-workbench__assign {
  grid-area: assign;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 60px;
  gap: var(--ios-pms-space-2);
  align-items: end;
}

.memo-workbench__assign label {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  align-items: center;
  gap: 6px;
  min-width: 0;
  color: var(--ios-pms-text-muted);
  font-size: 12px;
}

.memo-workbench__assign select {
  width: 100%;
  min-width: 0;
  height: 30px;
  padding: 0 7px;
  border: 1px solid rgba(116, 138, 185, 0.28);
  border-radius: 7px;
  background: #fff;
  color: var(--ios-pms-text-primary);
  font: inherit;
  font-size: 12px;
}

.memo-workbench__assign > button,
.memo-workbench__load-more > button,
.memo-workbench__state button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 0;
  border-radius: 8px;
  background: var(--ios-pms-primary);
  color: #fff;
  font: inherit;
  font-weight: var(--ios-pms-weight-bold);
}

.memo-workbench__assign > button {
  height: 30px;
  padding: 0 9px;
  border-radius: 7px;
  font-size: 12px;
}

.memo-workbench__assign ion-spinner {
  width: 16px;
  height: 16px;
}

.memo-workbench__task-actions {
  grid-area: assign;
  display: flex;
  justify-content: flex-end;
}

.memo-workbench__task-actions button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  min-height: 32px;
  padding: 0 11px;
  border: 0;
  border-radius: 7px;
  background: var(--ios-pms-primary);
  color: #fff;
  font: inherit;
  font-size: 12px;
  font-weight: var(--ios-pms-weight-bold);
}

.memo-workbench__task-actions ion-icon,
.memo-workbench__task-actions ion-spinner {
  width: 16px;
  height: 16px;
}

.memo-workbench__state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--ios-pms-space-3);
  min-height: 110px;
  padding: var(--ios-pms-space-5);
  color: var(--ios-pms-text-muted);
  font-size: 13px;
  line-height: 1.5;
  text-align: center;
}

.memo-workbench__state button {
  min-height: 32px;
  padding: 0 14px;
}

.memo-workbench__state--error {
  color: var(--ion-color-danger);
}

.memo-workbench__load-more {
  display: grid;
  justify-items: center;
  gap: var(--ios-pms-space-2);
}

.memo-workbench__load-more > button {
  min-height: 34px;
  padding: 0 16px;
  background: var(--ios-pms-primary-soft);
  color: var(--ios-pms-primary);
}

.memo-workbench__load-more p {
  margin: 0;
  color: var(--ion-color-danger);
  font-size: 11px;
}

@media (max-width: 374px) {
  .memo-workbench {
    padding: var(--ios-pms-space-4);
  }

  .memo-workbench__type {
    grid-template-columns: 22px minmax(0, 1fr);
    gap: 5px;
    min-height: 48px;
    padding: 5px 6px;
  }

  .memo-workbench__type > ion-icon {
    width: 22px;
    height: 22px;
  }

  .memo-workbench__assign {
    grid-template-columns: 1fr;
  }

  .memo-workbench__assign > button {
    justify-self: end;
    width: 60px;
  }
}
</style>
