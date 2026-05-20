<template>
  <div v-if="notificationCenterStore.items.length > 0" class="notif-overlay">
    <article
      v-for="item in notificationCenterStore.items"
      :key="item.id"
      class="notif-banner"
      role="button"
      tabindex="0"
      @click="handleOpenNotification(item.id, item.targetPath)"
      @keydown.enter.prevent="handleOpenNotification(item.id, item.targetPath)"
      @keydown.space.prevent="handleOpenNotification(item.id, item.targetPath)"
    >
      <div class="notif-banner__icon" :class="item.type === 'order' ? 'is-order' : 'is-message'">
        <ion-icon :icon="item.type === 'order' ? receiptOutline : chatbubbleOutline" />
      </div>

      <div class="notif-banner__body">
        <div class="notif-banner__title-row">
          <strong>{{ item.title }}</strong>
          <span class="notif-banner__type">{{ item.type === 'order' ? '订单' : '消息' }}</span>
        </div>
        <p class="notif-banner__content">{{ item.content }}</p>
      </div>

      <button class="notif-banner__close" type="button" @click.stop="handleDismiss(item.id)">
        <ion-icon :icon="closeOutline" />
      </button>
    </article>
  </div>
</template>

<script setup lang="ts">
import { IonIcon } from '@ionic/vue'
import { chatbubbleOutline, closeOutline, receiptOutline } from 'ionicons/icons'
import { onBeforeUnmount, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useNotificationCenterStore } from '@/stores/notificationCenter'
import { useStoreStore } from '@/stores/store'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const authStore = useAuthStore()
const notificationCenterStore = useNotificationCenterStore()
const userStore = useUserStore()
const storeStore = useStoreStore()

const handleDismiss = (id: string) => {
  notificationCenterStore.dismiss(id)
}

const handleOpenNotification = async (id: string, targetPath: string) => {
  notificationCenterStore.dismiss(id)
  await router.push(targetPath)
}

watch(
  [() => authStore.token, () => userStore.currentUser?.id, () => storeStore.currentStore?.id],
  async ([token, userId, storeId]) => {
    if (!token || !userId || !storeId) {
      notificationCenterStore.stop()
      return
    }

    notificationCenterStore.stop()

    try {
      await notificationCenterStore.start(userId)
    } catch {
      return
    }
  },
  { immediate: true },
)

onBeforeUnmount(() => {
  notificationCenterStore.stop()
})
</script>

<style scoped>
.notif-overlay {
  position: fixed;
  top: calc(env(safe-area-inset-top) + 10px);
  left: 12px;
  right: 12px;
  z-index: 1500;
  display: grid;
  gap: 8px;
}

.notif-banner {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 14px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.96);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  box-shadow:
    0 4px 24px rgba(0, 0, 0, 0.10),
    0 0 0 1px rgba(0, 0, 0, 0.04);
  text-align: left;
  font: inherit;
  cursor: pointer;
  animation: notif-slide-in 0.3s ease-out;
}

@keyframes notif-slide-in {
  from {
    opacity: 0;
    transform: translateY(-12px) scale(0.96);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.notif-banner__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  width: 36px;
  height: 36px;
  border-radius: 10px;
  font-size: 18px;
}

.notif-banner__icon.is-order {
  background: var(--app-warning-soft);
  color: var(--ion-color-warning);
}

.notif-banner__icon.is-message {
  background: var(--app-primary-soft);
  color: var(--ion-color-primary);
}

.notif-banner__body {
  flex: 1;
  min-width: 0;
}

.notif-banner__title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.notif-banner__title-row strong {
  color: var(--app-heading);
  font-size: 14px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.notif-banner__type {
  flex-shrink: 0;
  padding: 2px 8px;
  border-radius: 6px;
  font-size: 10px;
  font-weight: 700;
}

.notif-banner__icon.is-order ~ .notif-banner__body .notif-banner__type {
  background: var(--app-warning-soft);
  color: var(--ion-color-warning);
}

.notif-banner__icon.is-message ~ .notif-banner__body .notif-banner__type {
  background: var(--app-primary-soft);
  color: var(--ion-color-primary);
}

.notif-banner__content {
  margin: 4px 0 0;
  color: var(--app-muted);
  font-size: 13px;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.notif-banner__close {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  margin: -4px -4px 0 0;
  border: none;
  border-radius: 999px;
  background: rgba(0, 0, 0, 0.06);
  color: var(--app-muted);
  font-size: 16px;
}

.notif-banner__close:active {
  background: rgba(0, 0, 0, 0.12);
}
</style>
