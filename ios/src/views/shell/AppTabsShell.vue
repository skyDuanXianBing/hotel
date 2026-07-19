<template>
  <ion-page>
    <ion-tabs>
      <ion-router-outlet />
      <ion-tab-bar slot="bottom" class="mobile-tabbar">
        <ion-tab-button tab="home" :href="ROUTE_PATHS.home" aria-label="首页">
          <span class="mobile-tabbar__content">
            <span class="mobile-tabbar__icon-wrap" aria-hidden="true">
              <span class="mobile-tabbar__icon mobile-tabbar__icon--home" />
            </span>
            <span class="mobile-tabbar__label">首页</span>
          </span>
        </ion-tab-button>
        <ion-tab-button tab="rooms" :href="ROUTE_PATHS.rooms" aria-label="房态">
          <span class="mobile-tabbar__content">
            <span class="mobile-tabbar__icon-wrap" aria-hidden="true">
              <span class="mobile-tabbar__icon mobile-tabbar__icon--rooms" />
            </span>
            <span class="mobile-tabbar__label">房态</span>
          </span>
        </ion-tab-button>
        <ion-tab-button tab="messages" :href="ROUTE_PATHS.messages" aria-label="消息">
          <span class="mobile-tabbar__content">
            <span class="mobile-tabbar__icon-wrap" aria-hidden="true">
              <span class="mobile-tabbar__icon mobile-tabbar__icon--messages" />
              <span v-if="notificationCenterStore.unreadMessageCount > 0" class="mobile-tabbar__badge">
                {{ notificationCenterStore.unreadMessageCount }}
              </span>
            </span>
            <span class="mobile-tabbar__label">消息</span>
          </span>
        </ion-tab-button>
        <ion-tab-button tab="reviews" :href="ROUTE_PATHS.reviews" aria-label="审查">
          <span class="mobile-tabbar__content">
            <span class="mobile-tabbar__icon-wrap" aria-hidden="true">
              <span class="mobile-tabbar__icon mobile-tabbar__icon--reviews" />
            </span>
            <span class="mobile-tabbar__label">审查</span>
          </span>
        </ion-tab-button>
        <ion-tab-button tab="settings" :href="ROUTE_PATHS.settings" aria-label="设置">
          <span class="mobile-tabbar__content">
            <span class="mobile-tabbar__icon-wrap" aria-hidden="true">
              <span class="mobile-tabbar__icon mobile-tabbar__icon--settings" />
            </span>
            <span class="mobile-tabbar__label">设置</span>
          </span>
        </ion-tab-button>
      </ion-tab-bar>
    </ion-tabs>
  </ion-page>
</template>

<script setup lang="ts">
import { IonPage, IonRouterOutlet, IonTabBar, IonTabButton, IonTabs } from '@ionic/vue'
import { ROUTE_PATHS } from '@/router/guards'
import { useNotificationCenterStore } from '@/stores/notificationCenter'

const notificationCenterStore = useNotificationCenterStore()
</script>

<style scoped>
.mobile-tabbar {
  --tabbar-visual-height: 50px;
  --tabbar-button-height: 44px;
  box-sizing: border-box;
  height: calc(var(--tabbar-visual-height) + var(--app-safe-bottom));
  min-height: calc(var(--tabbar-visual-height) + var(--app-safe-bottom));
  padding: 3px 6px calc(3px + var(--app-safe-bottom));
  border-top: 1px solid rgba(164, 181, 216, 0.14);
  background: rgba(255, 255, 255, 0.88);
  box-shadow: var(--ios-pms-shadow-tabbar);
  backdrop-filter: blur(18px);
}

.mobile-tabbar :deep(ion-tab-button) {
  --color: #8c98b1;
  --color-selected: #3474f6;
  min-width: 0;
  min-height: var(--tabbar-button-height);
  padding: 0 2px;
  border-radius: 16px;
  position: relative;
  background: transparent;
  box-shadow: none;
}

.mobile-tabbar :deep(ion-tab-button::part(native)) {
  border-radius: 16px;
  background: transparent;
  box-shadow: none;
}

.mobile-tabbar :deep(ion-tab-button.tab-selected),
.mobile-tabbar :deep(ion-tab-button.tab-selected::part(native)) {
  background: transparent;
  box-shadow: none;
}

.mobile-tabbar__content {
  display: grid;
  grid-template-rows: 22px 12px;
  align-content: center;
  justify-items: center;
  width: 100%;
  height: 38px;
  row-gap: 2px;
  color: #8c98b1;
  transition: color 160ms ease;
}

.mobile-tabbar :deep(ion-tab-button.tab-selected) .mobile-tabbar__content {
  color: #3474f6;
}

.mobile-tabbar__icon-wrap {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  line-height: 1;
  flex-shrink: 0;
}

.mobile-tabbar__icon {
  display: block;
  width: 22px;
  height: 22px;
  background-color: currentColor;
  -webkit-mask-image: var(--tabbar-icon);
  mask-image: var(--tabbar-icon);
  -webkit-mask-position: center;
  mask-position: center;
  -webkit-mask-repeat: no-repeat;
  mask-repeat: no-repeat;
  -webkit-mask-size: auto var(--tabbar-icon-height, 24px);
  mask-size: auto var(--tabbar-icon-height, 24px);
}

.mobile-tabbar__icon--home {
  --tabbar-icon: url('/tabbar/home.png');
}

.mobile-tabbar__icon--rooms {
  --tabbar-icon: url('/home-shortcuts/rooms.png');
  --tabbar-icon-height: 26px;
}

.mobile-tabbar__icon--messages {
  --tabbar-icon: url('/home-shortcuts/messages.png');
  --tabbar-icon-height: 24.5px;
}

.mobile-tabbar__icon--reviews {
  --tabbar-icon: url('/tabbar/reviews.png');
}

.mobile-tabbar__icon--settings {
  --tabbar-icon: url('/home-shortcuts/settings.png');
}

.mobile-tabbar__badge {
  position: absolute;
  top: -4px;
  right: -9px;
  min-width: 16px;
  height: 16px;
  padding: 0 4px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: #ff4d4f;
  color: #fff;
  font-size: 10px;
  font-weight: 700;
  line-height: 1;
  box-shadow: 0 0 0 2px rgba(255, 255, 255, 0.92);
}

.mobile-tabbar__label {
  display: block;
  height: 12px;
  font-size: 10px;
  font-weight: 700;
  line-height: 12px;
  letter-spacing: 0;
  white-space: nowrap;
}
</style>
