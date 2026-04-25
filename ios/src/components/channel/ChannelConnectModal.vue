<template>
  <ion-modal :is-open="isOpen" @didDismiss="$emit('dismiss')">
    <ion-header translucent>
      <ion-toolbar>
        <ion-title>{{ title }}</ion-title>
        <ion-buttons slot="end">
          <ion-button @click="$emit('dismiss')">关闭</ion-button>
        </ion-buttons>
      </ion-toolbar>
    </ion-header>

    <ion-content class="channel-connect-modal__content">
      <section class="channel-connect-modal__hero">
        <span class="channel-connect-modal__eyebrow">准备连接</span>
        <strong>{{ channelName }}</strong>
        <p>授权完成后会自动回到当前页面，并刷新当前渠道的状态。</p>
      </section>

      <section class="channel-connect-modal__section">
        <h3>开始前请确认</h3>
        <ul>
          <li>请使用拥有渠道酒店 / 账号权限的正式账号授权。</li>
          <li>授权完成后，返回应用会自动刷新当前渠道状态。</li>
          <li>若显示“映射未完成”，请继续进入 Su 向导补齐房型与价盘映射。</li>
        </ul>
      </section>
    </ion-content>

    <ion-footer>
      <ion-toolbar>
        <div class="channel-connect-modal__footer">
          <ion-button fill="outline" @click="$emit('dismiss')">稍后再说</ion-button>
          <ion-button @click="$emit('confirm')">同意并开始授权</ion-button>
        </div>
      </ion-toolbar>
    </ion-footer>
  </ion-modal>
</template>

<script setup lang="ts">
import {
  IonButton,
  IonButtons,
  IonContent,
  IonFooter,
  IonHeader,
  IonModal,
  IonTitle,
  IonToolbar,
} from '@ionic/vue'
import { computed } from 'vue'

const props = defineProps<{
  isOpen: boolean
  channelName: string
}>()

defineEmits<{
  dismiss: []
  confirm: []
}>()

const title = computed(() => `${props.channelName || '渠道'}授权说明`)
</script>

<style scoped>
.channel-connect-modal__content {
  --padding-top: 10px;
  --padding-bottom: calc(20px + var(--app-safe-bottom));
  --padding-start: 16px;
  --padding-end: 16px;
}

.channel-connect-modal__hero {
  padding: 18px 18px 16px;
  border-radius: 24px;
  border: 1px solid var(--app-border);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(246, 249, 255, 0.94));
}

.channel-connect-modal__eyebrow {
  display: inline-flex;
  margin-bottom: 10px;
  color: var(--ion-color-primary);
  font-size: 12px;
  font-weight: 700;
}

.channel-connect-modal__hero strong {
  display: block;
  font-size: 20px;
  color: var(--app-heading);
  letter-spacing: -0.02em;
}

.channel-connect-modal__hero p,
.channel-connect-modal__section p,
.channel-connect-modal__section li {
  margin: 8px 0 0;
  color: var(--app-muted);
  font-size: 14px;
  line-height: 1.6;
}

.channel-connect-modal__section {
  margin-top: 14px;
  padding: 18px;
  border: 1px solid var(--app-border);
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.96);
}

.channel-connect-modal__section h3 {
  margin: 0;
  color: var(--app-heading);
  font-size: 16px;
}

.channel-connect-modal__section ul {
  margin: 10px 0 0;
  padding-left: 18px;
}

.channel-connect-modal__footer {
  display: flex;
  gap: 10px;
  padding: 0 16px calc(4px + var(--app-safe-bottom));
}

.channel-connect-modal__footer ion-button {
  flex: 1;
  --box-shadow: none;
}
</style>
