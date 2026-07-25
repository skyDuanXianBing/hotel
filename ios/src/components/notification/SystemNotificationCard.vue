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
          :aria-label="$t('stage5Final.notification.moreActions')"
          @click="$emit('openActions')"
        >
          <ion-icon :icon="ellipsisHorizontal" />
        </button>
      </div>
    </div>

    <div class="system-notification-card__content-band">
      <span class="system-notification-card__content-label">{{ $t('stage5Final.notification.content') }}</span>
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
import { useI18n } from 'vue-i18n'
import { formatDateTime } from '@/components/order/orderUtils'
import type { NotificationMessageDTO } from '@/types/notification'

const props = defineProps<{
  notification: NotificationMessageDTO
}>()

defineEmits<{
  openActions: []
}>()

const { t } = useI18n()
const titleText = computed(
  () => props.notification.title?.trim() || t('stage5Final.notification.systemNotification'),
)
const contentText = computed(
  () => props.notification.content?.trim() || t('stage5Final.notification.emptyContent'),
)
const createdAtText = computed(() => formatDateTime(props.notification.createdAt))
const readAtText = computed(() => formatDateTime(props.notification.readAt))
const statusText = computed(() =>
  props.notification.isRead
    ? t('stage5Final.notification.read')
    : t('stage5Final.notification.unread'),
)
const statusClass = computed(() => (props.notification.isRead ? 'is-medium' : 'is-danger'))
const typeText = computed(() => formatTypeLabel(props.notification.notificationType))
const metaItems = computed(() => {
  const items = [
    typeText.value,
    props.notification.isRead
      ? t('stage5Final.notification.handled')
      : t('stage5Final.notification.pending'),
  ]

  if (props.notification.relatedId) {
    items.push(t('stage5Final.notification.related', { id: props.notification.relatedId }))
  }

  return items
})
const supportingText = computed(() => {
  if (props.notification.readAt) {
    return t('stage5Final.notification.readAt', { time: readAtText.value })
  }

  return t('stage5Final.notification.manageHint')
})

function formatTypeLabel(type: string) {
  if (type === 'SYSTEM') {
    return t('stage5Final.notification.systemNotification')
  }
  if (type === 'CLEANING') {
    return t('stage5Final.notification.cleaningNotification')
  }
  if (type === 'TASK') {
    return t('stage5Final.notification.taskNotification')
  }
  return type || t('stage5Final.notification.systemNotification')
}
</script>

<style scoped>
.system-notification-card {
  display: grid;
  gap: 10px;
  padding: 16px 16px 18px;
  border: 1px solid rgba(97, 124, 177, 0.06);
  border-radius: var(--ios-pms-radius-card-sm);
  background: linear-gradient(180deg, #ffffff 0%, rgba(250, 252, 255, 0.97) 100%);
  box-shadow: 0 8px 20px rgba(77, 98, 145, 0.08);
  transition:
    border-color 0.18s ease,
    box-shadow 0.18s ease,
    transform 0.18s ease,
    background 0.18s ease;
}

.system-notification-card.is-read {
  background: linear-gradient(180deg, #ffffff 0%, rgba(250, 251, 253, 0.96) 100%);
}

.system-notification-card__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.system-notification-card__identity {
  min-width: 0;
  display: flex;
  flex-direction: column;
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
.system-notification-card__timestamp,
.system-notification-card__support-line {
  margin: 0;
}

.system-notification-card__eyebrow {
  order: 2;
  color: var(--ios-pms-text-soft);
  font-size: 12px;
  font-weight: 400;
  line-height: 1.35;
  letter-spacing: 0;
}

.system-notification-card__title {
  order: 1;
  color: var(--ios-pms-text-primary);
  font-size: 20px;
  font-weight: var(--ios-pms-weight-heavy);
  line-height: 1.2;
  letter-spacing: 0;
  word-break: break-word;
}

.system-notification-card__status {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 26px;
  padding: 0 12px;
  border-radius: var(--ios-pms-radius-pill);
  background: var(--ios-pms-primary-soft);
  color: var(--ios-pms-primary-strong);
  font-size: 12px;
  font-weight: var(--ios-pms-weight-bold);
  white-space: nowrap;
}

.system-notification-card__status.is-danger {
  background: #ffe1e3;
  color: #ef454f;
}

.system-notification-card__status.is-medium {
  background: #eef2f7;
  color: #7f8999;
}

.system-notification-card__more-btn {
  display: grid;
  place-items: center;
  width: 28px;
  height: 28px;
  padding: 0;
  border: 0;
  border-radius: 999px;
  background: rgba(248, 249, 252, 0.94);
  color: #a1a8b4;
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
  gap: 8px;
  padding: 12px 14px 13px;
  border: 0;
  border-radius: 10px;
  background: #f1f6ff;
}

.system-notification-card__content-label {
  color: #3f8cf4;
  font-size: 14px;
  font-weight: var(--ios-pms-weight-medium);
  line-height: 1.2;
}

.system-notification-card__content {
  color: var(--ios-pms-text-primary);
  font-size: 15px;
  font-weight: 400;
  line-height: 1.48;
  white-space: pre-wrap;
  word-break: break-word;
  display: -webkit-box;
  overflow: hidden;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 4;
}

.system-notification-card__meta-strip {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.system-notification-card__meta-line {
  display: contents;
}

.system-notification-card__meta-item {
  flex: 0 0 auto;
  color: #a1a8b4;
  font-size: 11px;
  line-height: 1.35;
  white-space: nowrap;
}

.system-notification-card__meta-item + .system-notification-card__meta-item::before {
  content: none;
}

.system-notification-card__timestamp {
  min-width: 0;
  margin-left: auto;
  color: #a1a8b4;
  font-size: 11px;
  line-height: 1.35;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.system-notification-card__support-line {
  color: #a1a8b4;
  font-size: 12px;
  line-height: 1.4;
}
</style>
