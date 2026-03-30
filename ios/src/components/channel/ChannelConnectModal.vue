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

    <ion-content>
      <section class="channel-connect-modal__hero">
        <strong>{{ channelName }}</strong>
      </section>

      <ion-list inset>
        <ion-item lines="none">
          <div class="channel-connect-modal__section">
            <h3>开始前请确认</h3>
            <ul>
              <li>请使用拥有渠道酒店 / 账号权限的正式账号授权。</li>
              <li>授权完成后，返回应用会自动刷新当前渠道状态。</li>
              <li>若显示“映射未完成”，请继续进入 Su 向导补齐房型与价盘映射。</li>
            </ul>
          </div>
        </ion-item>

      </ion-list>
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
  IonItem,
  IonList,
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
.channel-connect-modal__hero {
  margin: 16px;
  padding: 18px;
  border-radius: 20px;
  background: var(--app-primary-soft);
}

.channel-connect-modal__hero strong {
  display: block;
  font-size: 18px;
  color: var(--app-heading);
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
  padding: 6px 0;
}

.channel-connect-modal__section h3 {
  margin: 0;
  color: var(--app-heading);
  font-size: 15px;
}

.channel-connect-modal__section ul {
  margin: 10px 0 0;
  padding-left: 18px;
}

.channel-connect-modal__footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 0 16px;
}
</style>
