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
        <p class="order-notification-card__eyebrow">
          {{ notification.relatedId ? `关联订单 #${notification.relatedId}` : '订单提醒' }}
        </p>
        <h3 class="order-notification-card__title">{{ titleText }}</h3>
      </div>

      <div class="order-notification-card__header-side">
        <span class="order-notification-card__status" :class="statusClass">{{ statusText }}</span>
        <button
          type="button"
          class="order-notification-card__more-btn"
          aria-label="更多通知操作"
          @click.stop="$emit('openActions')"
        >
          <ion-icon :icon="ellipsisHorizontal" />
        </button>
      </div>
    </div>

    <div class="order-notification-card__content-band">
      <span class="order-notification-card__content-label">通知内容</span>
      <p class="order-notification-card__content">{{ contentText }}</p>
    </div>

    <div class="order-notification-card__meta-strip">
      <p class="order-notification-card__meta-line">
        <span v-for="item in metaItems" :key="item" class="order-notification-card__meta-item">
          {{ item }}
        </span>
      </p>
      <p class="order-notification-card__timestamp">{{ createdAtText }}</p>
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
import { formatDateTime } from '@/components/order/orderUtils'
import type { NotificationMessageDTO } from '@/types/notification'

const props = defineProps<{
  notification: NotificationMessageDTO
}>()

defineEmits<{
  openDetail: []
  openActions: []
}>()

const titleText = computed(() => props.notification.title?.trim() || '订单提醒')
const contentText = computed(() => props.notification.content?.trim() || '暂无通知内容')
const createdAtText = computed(() => formatDateTime(props.notification.createdAt))
const readAtText = computed(() => formatDateTime(props.notification.readAt))
const statusText = computed(() => (props.notification.isRead ? '已读' : '未读'))
const statusClass = computed(() => (props.notification.isRead ? 'is-medium' : 'is-danger'))
const canOpenDetail = computed(() => Boolean(props.notification.relatedId))
const metaItems = computed(() => {
  const items = ['订单通知']

  if (props.notification.relatedId) {
    items.push(`订单 ${props.notification.relatedId}`)
  }

  items.push(props.notification.isRead ? '已处理' : '待处理')
  return items
})
const supportingText = computed(() => {
  if (props.notification.readAt) {
    return `已读时间 · ${readAtText.value}`
  }

  if (props.notification.relatedId) {
    return '点击卡片可查看关联订单详情'
  }

  return '可在右上角菜单中标记已读或删除通知'
})
</script>

<style scoped>
.order-notification-card {
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

.order-notification-card.is-clickable:active {
  border-color: rgba(var(--ion-color-primary-rgb), 0.14);
  background: linear-gradient(180deg, #ffffff 0%, rgba(243, 248, 255, 0.96) 100%);
  box-shadow: 0 8px 20px rgba(77, 98, 145, 0.06);
  transform: translateY(1px);
}

.order-notification-card.is-read {
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.96) 0%, rgba(250, 251, 253, 0.9) 100%);
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
.order-notification-card__meta-line,
.order-notification-card__timestamp,
.order-notification-card__support-line {
  margin: 0;
}

.order-notification-card__eyebrow {
  color: var(--ios-pms-text-soft);
  font-size: 11px;
  font-weight: var(--ios-pms-weight-bold);
  letter-spacing: 0.04em;
  text-transform: uppercase;
}

.order-notification-card__title {
  color: var(--ios-pms-text-primary);
  font-size: 18px;
  font-weight: var(--ios-pms-weight-heavy);
  line-height: 1.24;
  letter-spacing: -0.02em;
  word-break: break-word;
}

.order-notification-card__status {
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

.order-notification-card__status.is-danger {
  background: rgba(var(--ion-color-danger-rgb), 0.1);
  color: var(--ion-color-danger);
}

.order-notification-card__status.is-medium {
  background: rgba(116, 138, 185, 0.1);
  color: var(--ios-pms-text-muted);
}

.order-notification-card__more-btn {
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

.order-notification-card__more-btn ion-icon {
  font-size: 16px;
}

.order-notification-card__more-btn:active {
  background: var(--ios-pms-primary-soft);
  color: var(--ios-pms-primary-strong);
}

.order-notification-card__content-band {
  display: grid;
  gap: 6px;
  padding: 10px 12px;
  border: 1px solid rgba(var(--ion-color-primary-rgb), 0.08);
  border-radius: 14px;
  background: var(--ios-pms-primary-soft);
}

.order-notification-card__content-label {
  color: var(--ios-pms-primary-strong);
  font-size: 11px;
  font-weight: var(--ios-pms-weight-bold);
  line-height: 1;
}

.order-notification-card__content {
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

.order-notification-card__meta-strip {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: baseline;
  gap: 10px;
}

.order-notification-card__meta-line {
  min-width: 0;
  color: var(--ios-pms-text-muted);
  font-size: 12px;
  line-height: 1.45;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.order-notification-card__meta-item {
  display: inline;
}

.order-notification-card__meta-item + .order-notification-card__meta-item::before {
  content: '·';
  margin: 0 6px;
  color: var(--ios-pms-text-disabled);
}

.order-notification-card__timestamp {
  color: var(--ios-pms-text-soft);
  font-size: 11px;
  line-height: 1.3;
  white-space: nowrap;
}

.order-notification-card__support-line {
  color: var(--ios-pms-text-muted);
  font-size: 12px;
  line-height: 1.45;
}
</style>
