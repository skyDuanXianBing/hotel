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
          {{ notification.relatedId ? `关联订单 #${notification.relatedId}` : '订单提醒' }}
        </p>
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
    items.push(`订单${props.notification.relatedId}`)
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
