<template>
  <ion-page>
    <ion-tabs>
      <ion-router-outlet />
      <ion-tab-bar slot="bottom" class="mobile-tabbar">
        <ion-tab-button tab="home" :href="ROUTE_PATHS.home">
          <ion-icon :icon="homeOutline" />
          <ion-label>首页</ion-label>
        </ion-tab-button>
        <ion-tab-button tab="rooms" :href="ROUTE_PATHS.rooms">
          <ion-icon :icon="bedOutline" />
          <ion-label>房态</ion-label>
        </ion-tab-button>
        <ion-tab-button tab="messages" :href="ROUTE_PATHS.messages">
          <ion-icon :icon="chatbubblesOutline" />
          <ion-label>消息</ion-label>
        </ion-tab-button>
        <ion-tab-button tab="reviews" :href="ROUTE_PATHS.reviews">
          <ion-icon :icon="clipboardOutline" />
          <ion-label>审查</ion-label>
        </ion-tab-button>
        <ion-tab-button tab="settings" :href="ROUTE_PATHS.settings">
          <ion-icon :icon="settingsOutline" />
          <ion-label>设置</ion-label>
        </ion-tab-button>
      </ion-tab-bar>
    </ion-tabs>
  </ion-page>
</template>

<script setup lang="ts">
import {
  IonIcon,
  IonLabel,
  IonPage,
  IonRouterOutlet,
  IonTabBar,
  IonTabButton,
  IonTabs,
} from '@ionic/vue'
import {
  bedOutline,
  chatbubblesOutline,
  clipboardOutline,
  homeOutline,
  settingsOutline,
} from 'ionicons/icons'
import { ROUTE_PATHS } from '@/router/guards'
</script>

<style scoped>
.mobile-tabbar {
  padding: 8px 6px calc(8px + env(safe-area-inset-bottom));
  border-top: 1px solid rgba(164, 181, 216, 0.14);
  background: rgba(255, 255, 255, 0.88);
  box-shadow: var(--ios-pms-shadow-tabbar);
  backdrop-filter: blur(18px);
}

.mobile-tabbar :deep(ion-tab-button) {
  --color: #8c98b1;
  --color-selected: #3474f6;
  min-width: 0;
  gap: 5px;
  padding: 8px 2px 10px;
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

.mobile-tabbar :deep(ion-icon) {
  font-size: 20px;
}

.mobile-tabbar :deep(ion-label) {
  font-size: 10px;
  font-weight: 700;
  letter-spacing: -0.01em;
}
</style>
