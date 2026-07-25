<template>
  <ion-modal :is-open="isOpen" @didDismiss="$emit('dismiss')">
    <ion-header translucent>
      <ion-toolbar>
        <ion-title>{{ title }}</ion-title>
        <ion-buttons slot="end">
          <ion-button @click="$emit('dismiss')">{{ $t('home.section.close') }}</ion-button>
        </ion-buttons>
      </ion-toolbar>
    </ion-header>

    <ion-content class="channel-connect-modal__content">
      <section class="channel-connect-modal__hero">
        <span class="channel-connect-modal__eyebrow">{{ $t('iosStage5.channel.prepareConnection') }}</span>
        <strong>{{ channelName }}</strong>
        <p>{{ $t('iosStage5.channel.connectionDescription') }}</p>
      </section>

      <section class="channel-connect-modal__section">
        <h3>{{ $t('iosStage5.channel.confirmBeforeStart') }}</h3>
        <ul>
          <li>{{ $t('iosStage5.channel.authorizedAccount') }}</li>
          <li>{{ $t('iosStage5.channel.refreshAfterAuthorization') }}</li>
          <li>{{ $t('iosStage5.channel.mappingReminder') }}</li>
        </ul>
      </section>
    </ion-content>

    <ion-footer>
      <ion-toolbar>
        <div class="channel-connect-modal__footer">
          <ion-button fill="outline" @click="$emit('dismiss')">{{ $t('iosStage5.channel.later') }}</ion-button>
          <ion-button @click="$emit('confirm')">{{ $t('channel.dialogs.connectNotice.agree') }}</ion-button>
        </div>
      </ion-toolbar>
    </ion-footer>
  </ion-modal>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'
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

const { t } = useI18n()

const props = defineProps<{
  isOpen: boolean
  channelName: string
}>()

defineEmits<{
  dismiss: []
  confirm: []
}>()

const title = computed(() =>
  t('stage5Final.channel.authorizationTitle', {
    name: props.channelName || t('home.quick.channels.0'),
  }),
)
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
