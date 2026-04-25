<template>
  <article class="system-notification-card" :class="{ 'is-read': notification.isRead }">
    <div class="system-notification-card__header">
      <div class="system-notification-card__identity">
        <p class="system-notification-card__eyebrow">{{ typeText }}</p>
        <h3 class="system-notification-card__title">{{ titleText }}</h3>
      </div>

      <div class="system-notification-card__header-side">
        <span class="system-notification-card__status" :class="statusClass">{{ statusText }}</span>
        <button
          type="button"
          class="system-notification-card__more-btn"
          aria-label="更多通知操作"
          @click="$emit('openActions')"
        >
          <ion-icon :icon="ellipsisHorizontal" />
        </button>
      </div>
    </div>

    <div class="system-notification-card__content-band">
      <span class="system-notification-card__content-label">通知内容</span>
      <p class="system-notification-card__content">{{ contentText }}</p>
    </div>

    <div class="system-notification-card__meta-strip">
      <p class="system-notification-card__meta-line">
        <span v-for="item in metaItems" :key="item" class="system-notification-card__meta-item">
          {{ item }}
        </span>
      </p>
      <p class="system-notification-card__timestamp">{{ createdAtText }}</p>
    </div>

    <p v-if="supportingText" class="system-notification-card__support-line">
      {{ supportingText }}
    </p>
  </article>
</template>

<script setup lang="ts">
import { IonIcon } from '@ionic/vue'
import { ellipsisHorizontal } from 'ionicons/icons'
import { computed } from 'vue'
import { formatDateTime } from '@/components/order/orderUtils'
import type { NotificationMessageDTO } from '@/types/notification'

const props = defineProps<{
  notification: NotificationMessageDTO
}>()

defineEmits<{
  openActions: []
}>()

const titleText = computed(() => props.notification.title?.trim() || '系统通知')
const contentText = computed(() => props.notification.content?.trim() || '暂无通知内容')
const createdAtText = computed(() => formatDateTime(props.notification.createdAt))
const readAtText = computed(() => formatDateTime(props.notification.readAt))
const statusText = computed(() => (props.notification.isRead ? '已读' : '未读'))
const statusClass = computed(() => (props.notification.isRead ? 'is-medium' : 'is-danger'))
const typeText = computed(() => formatTypeLabel(props.notification.notificationType))
const metaItems = computed(() => {
  const items = [typeText.value, props.notification.isRead ? '已处理' : '待处理']

  if (props.notification.relatedId) {
    items.push(`关联 ${props.notification.relatedId}`)
  }

  return items
})
const supportingText = computed(() => {
  if (props.notification.readAt) {
    return `已读时间 · ${readAtText.value}`
  }

  return '可在右上角菜单中标记已读或删除通知'
})

function formatTypeLabel(type: string) {
  if (type === 'SYSTEM') {
    return '系统通知'
  }
  if (type === 'CLEANING') {
    return '保洁通知'
  }
  if (type === 'TASK') {
    return '任务通知'
  }
  return type || '系统通知'
}
</script>

<style scoped>
.system-notification-card {
  display: grid;
  gap: var(--ios-pms-space-3);
  padding: 16px;
  border: 1px solid var(--ios-pms-border-soft);
  border-radius: var(--ios-pms-radius-card-sm);
  background: linear-gradient(180deg, var(--ios-pms-surface-strong) 0%, rgba(248, 251, 255, 0.92) 100%);
  box-shadow: var(--ios-pms-shadow-card);
  transition:
    border-color 0.18s ease,
    box-shadow 0.18s ease,
    transform 0.18s ease,
    background 0.18s ease;
}

.system-notification-card.is-read {
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.96) 0%, rgba(250, 251, 253, 0.9) 100%);
}

.system-notification-card__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.system-notification-card__identity {
  min-width: 0;
  display: grid;
  gap: 4px;
}

.system-notification-card__header-side {
  display: flex;
  align-items: center;
  gap: 8px;
}

.system-notification-card__eyebrow,
.system-notification-card__title,
.system-notification-card__content,
.system-notification-card__meta-line,
.system-notification-card__timestamp,
.system-notification-card__support-line {
  margin: 0;
}

.system-notification-card__eyebrow {
  color: var(--ios-pms-text-soft);
  font-size: 11px;
  font-weight: var(--ios-pms-weight-bold);
  letter-spacing: 0.04em;
  text-transform: uppercase;
}

.system-notification-card__title {
  color: var(--ios-pms-text-primary);
  font-size: 18px;
  font-weight: var(--ios-pms-weight-heavy);
  line-height: 1.24;
  letter-spacing: -0.02em;
  word-break: break-word;
}

.system-notification-card__status {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 26px;
  padding: 0 10px;
  border-radius: var(--ios-pms-radius-pill);
  background: var(--ios-pms-primary-soft);
  color: var(--ios-pms-primary-strong);
  font-size: 12px;
  font-weight: var(--ios-pms-weight-bold);
  white-space: nowrap;
}

.system-notification-card__status.is-danger {
  background: rgba(var(--ion-color-danger-rgb), 0.1);
  color: var(--ion-color-danger);
}

.system-notification-card__status.is-medium {
  background: rgba(116, 138, 185, 0.1);
  color: var(--ios-pms-text-muted);
}

.system-notification-card__more-btn {
  display: grid;
  place-items: center;
  width: 28px;
  height: 28px;
  padding: 0;
  border: 1px solid var(--ios-pms-border-faint);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.78);
  color: var(--ios-pms-text-muted);
  font: inherit;
}

.system-notification-card__more-btn ion-icon {
  font-size: 16px;
}

.system-notification-card__more-btn:active {
  background: var(--ios-pms-primary-soft);
  color: var(--ios-pms-primary-strong);
}

.system-notification-card__content-band {
  display: grid;
  gap: 6px;
  padding: 10px 12px;
  border: 1px solid rgba(var(--ion-color-primary-rgb), 0.08);
  border-radius: 14px;
  background: var(--ios-pms-primary-soft);
}

.system-notification-card__content-label {
  color: var(--ios-pms-primary-strong);
  font-size: 11px;
  font-weight: var(--ios-pms-weight-bold);
  line-height: 1;
}

.system-notification-card__content {
  color: var(--ios-pms-text-primary);
  font-size: 15px;
  font-weight: var(--ios-pms-weight-medium);
  line-height: 1.55;
  white-space: pre-wrap;
  word-break: break-word;
  display: -webkit-box;
  overflow: hidden;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 3;
}

.system-notification-card__meta-strip {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: baseline;
  gap: 10px;
}

.system-notification-card__meta-line {
  min-width: 0;
  color: var(--ios-pms-text-muted);
  font-size: 12px;
  line-height: 1.45;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.system-notification-card__meta-item {
  display: inline;
}

.system-notification-card__meta-item + .system-notification-card__meta-item::before {
  content: '·';
  margin: 0 6px;
  color: var(--ios-pms-text-disabled);
}

.system-notification-card__timestamp {
  color: var(--ios-pms-text-soft);
  font-size: 11px;
  line-height: 1.3;
  white-space: nowrap;
}

.system-notification-card__support-line {
  color: var(--ios-pms-text-muted);
  font-size: 12px;
  line-height: 1.45;
}
</style>
