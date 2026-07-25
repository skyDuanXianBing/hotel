<template>
  <article
    class="order-notification-card"
    :class="{ 'is-read': notification.isRead, 'is-clickable': canOpenDetail }"
    :role="canOpenDetail ? 'button' : undefined"
    :tabindex="canOpenDetail ? 0 : undefined"
    @click="$emit('openDetail')"
    @keydown.enter.prevent="$emit('openDetail')"
    @keydown.space.prevent="$emit('openDetail')"
  >
    <div class="order-notification-card__header">
      <div class="order-notification-card__identity">
        <h3 class="order-notification-card__title">{{ titleText }}</h3>
        <p class="order-notification-card__eyebrow">
          {{
            notification.relatedId
              ? $t('stage5Final.notification.relatedOrder', { id: notification.relatedId })
              : $t('iosStage5.roomStatus.reservationAlerts')
          }}
        </p>
      </div>

      <div class="order-notification-card__header-side">
        <span class="order-notification-card__status" :class="statusClass">{{ statusText }}</span>
        <button
          type="button"
          class="order-notification-card__more-btn"
          :aria-label="$t('stage5Final.notification.moreActions')"
          @click.stop="$emit('openActions')"
        >
          <ion-icon :icon="ellipsisHorizontal" />
        </button>
      </div>
    </div>

    <div class="order-notification-card__content-band">
      <span class="order-notification-card__content-label">{{ $t('stage5Final.notification.content') }}</span>
      <p class="order-notification-card__content">{{ contentText }}</p>
    </div>

    <div class="order-notification-card__meta-strip">
      <span v-for="item in metaItems" :key="item" class="order-notification-card__meta-item">
        {{ item }}
      </span>
      <time class="order-notification-card__timestamp">{{ createdAtText }}</time>
    </div>

    <p v-if="supportingText" class="order-notification-card__support-line">
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
  openDetail: []
  openActions: []
}>()

const { t } = useI18n()
const titleText = computed(
  () => props.notification.title?.trim() || t('iosStage5.roomStatus.reservationAlerts'),
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
const canOpenDetail = computed(() => Boolean(props.notification.relatedId))
const metaItems = computed(() => {
  const items = [t('stage5Final.notification.orderNotification')]

  if (props.notification.relatedId) {
    items.push(t('stage5Final.notification.related', { id: props.notification.relatedId }))
  }

  items.push(
    props.notification.isRead
      ? t('stage5Final.notification.handled')
      : t('stage5Final.notification.pending'),
  )
  return items
})
const supportingText = computed(() => {
  if (props.notification.readAt) {
    return t('stage5Final.notification.readAt', { time: readAtText.value })
  }

  if (props.notification.relatedId) {
    return t('stage5Final.notification.openOrderHint')
  }

  return t('stage5Final.notification.manageHint')
})
</script>

<style scoped>
.order-notification-card {
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

.order-notification-card.is-clickable:active {
  border-color: rgba(var(--ion-color-primary-rgb), 0.14);
  background: linear-gradient(180deg, #ffffff 0%, rgba(247, 250, 255, 0.98) 100%);
  box-shadow: 0 6px 16px rgba(77, 98, 145, 0.08);
  transform: translateY(1px);
}

.order-notification-card.is-read {
  background: linear-gradient(180deg, #ffffff 0%, rgba(250, 251, 253, 0.96) 100%);
}

.order-notification-card__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.order-notification-card__identity {
  min-width: 0;
  display: grid;
  gap: 4px;
}

.order-notification-card__header-side {
  display: flex;
  align-items: center;
  gap: 8px;
}

.order-notification-card__eyebrow,
.order-notification-card__title,
.order-notification-card__content,
.order-notification-card__timestamp,
.order-notification-card__support-line {
  margin: 0;
}

.order-notification-card__eyebrow {
  color: var(--ios-pms-text-soft);
  font-size: 12px;
  font-weight: 400;
  line-height: 1.35;
  letter-spacing: 0;
}

.order-notification-card__title {
  color: var(--ios-pms-text-primary);
  font-size: 20px;
  font-weight: var(--ios-pms-weight-heavy);
  line-height: 1.2;
  letter-spacing: 0;
  word-break: break-word;
}

.order-notification-card__status {
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

.order-notification-card__status.is-danger {
  background: #ffe1e3;
  color: #ef454f;
}

.order-notification-card__status.is-medium {
  background: #eef2f7;
  color: #7f8999;
}

.order-notification-card__more-btn {
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

.order-notification-card__more-btn ion-icon {
  font-size: 16px;
}

.order-notification-card__more-btn:active {
  background: var(--ios-pms-primary-soft);
  color: var(--ios-pms-primary-strong);
}

.order-notification-card__content-band {
  display: grid;
  gap: 8px;
  padding: 12px 14px 13px;
  border: 0;
  border-radius: 10px;
  background: #f1f6ff;
}

.order-notification-card__content-label {
  color: #3f8cf4;
  font-size: 14px;
  font-weight: var(--ios-pms-weight-medium);
  line-height: 1.2;
}

.order-notification-card__content {
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

.order-notification-card__meta-strip {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.order-notification-card__meta-item {
  flex: 0 0 auto;
  color: #a1a8b4;
  font-size: 11px;
  line-height: 1.35;
  white-space: nowrap;
}

.order-notification-card__timestamp {
  min-width: 0;
  margin-left: auto;
  color: #a1a8b4;
  font-size: 11px;
  line-height: 1.35;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.order-notification-card__support-line {
  color: #a1a8b4;
  font-size: 12px;
  line-height: 1.4;
}
</style>
