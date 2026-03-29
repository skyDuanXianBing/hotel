<template>
  <ion-page>
    <ion-header translucent>
      <ion-toolbar>
        <ion-buttons slot="start">
          <ion-back-button :default-href="ROUTE_PATHS.settings" />
        </ion-buttons>
        <ion-title>支付平台</ion-title>
      </ion-toolbar>
    </ion-header>

    <ion-content fullscreen class="mobile-page settings-page-block">
      <section class="mobile-hero settings-page-block__hero">
        <p class="mobile-note settings-page-block__eyebrow">第三方集成</p>
        <h1 class="mobile-title">支付平台</h1>
        <p class="mobile-subtitle">本轮先提供状态页与风险提示，保证移动端入口完整可见。</p>
      </section>

      <div class="mobile-stack">
        <section class="mobile-card">
          <h2 class="mobile-section-title">当前状态</h2>
          <ul class="mobile-bullet-list">
            <li v-for="item in PAYMENT_PLATFORM_STATUS_ITEMS" :key="item">{{ item }}</li>
          </ul>
        </section>

        <section class="mobile-card payment-platforms-page__placeholder">
          <strong>暂无可直接操作的平台配置</strong>
          <p>如需开通、签约或联调，请继续在桌面端完成；移动端当前负责能力可见与风险提示。</p>
        </section>
      </div>
    </ion-content>
  </ion-page>
</template>

<script setup lang="ts">
import { IonBackButton, IonButtons, IonContent, IonHeader, IonPage, IonTitle, IonToolbar } from '@ionic/vue'
import { PAYMENT_PLATFORM_STATUS_ITEMS } from '@/constants/settings'
import { ROUTE_PATHS } from '@/router/guards'
</script>

<style scoped>
.settings-page-block {
  display: block;
}

.settings-page-block__hero {
  margin-top: 4px;
}

.settings-page-block__eyebrow {
  color: var(--ion-color-primary);
  font-weight: 700;
}

.payment-platforms-page__placeholder {
  display: grid;
  gap: 8px;
}

.payment-platforms-page__placeholder strong,
.payment-platforms-page__placeholder p {
  margin: 0;
}

.payment-platforms-page__placeholder p {
  color: var(--app-muted);
  line-height: 1.6;
}
</style>
