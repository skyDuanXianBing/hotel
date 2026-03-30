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
        <ion-tab-button tab="orders" :href="ROUTE_PATHS.orders">
          <ion-icon :icon="receiptOutline" />
          <ion-label>订单</ion-label>
        </ion-tab-button>
        <ion-tab-button tab="channels" :href="ROUTE_PATHS.channels">
          <ion-icon :icon="gitNetworkOutline" />
          <ion-label>渠道</ion-label>
        </ion-tab-button>
        <ion-tab-button tab="statistics" :href="ROUTE_PATHS.statistics">
          <ion-icon :icon="statsChartOutline" />
          <ion-label>统计</ion-label>
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
    <GlobalQuickTools />
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
  clipboardOutline,
  gitNetworkOutline,
  homeOutline,
  receiptOutline,
  settingsOutline,
  statsChartOutline,
} from 'ionicons/icons'
import GlobalQuickTools from '@/components/global/GlobalQuickTools.vue'
import { ROUTE_PATHS } from '@/router/guards'
</script>

<style scoped>
.mobile-tabbar {
  padding-inline: 6px;
}

.mobile-tabbar :deep(ion-tab-button) {
  min-width: 0;
  gap: 4px;
  padding-inline: 4px;
}

.mobile-tabbar :deep(ion-icon) {
  font-size: 18px;
}

.mobile-tabbar :deep(ion-label) {
  font-size: 10px;
  font-weight: 600;
  letter-spacing: -0.01em;
}
</style>
