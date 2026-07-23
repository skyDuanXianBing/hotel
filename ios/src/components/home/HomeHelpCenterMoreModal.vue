<template>
  <ion-modal
    :is-open="isOpen"
    :breakpoints="[0, 0.45, 0.78]"
    :initial-breakpoint="0.45"
    @didDismiss="handleDismiss"
  >
    <ion-content class="mobile-page home-help-center-modal-page">
      <div class="mobile-stack">
        <section class="mobile-card home-help-center-modal-card">
          <div class="home-help-center-modal-card__header">
            <div>
              <h2 class="mobile-section-title">{{ t('home.section.moreHelp') }}</h2>
            </div>

            <ion-button fill="outline" size="small" @click="handleContactSupport">
              {{ t('home.section.contactSupport') }}
            </ion-button>
          </div>

          <button
            v-for="item in items"
            :key="item.key"
            type="button"
            class="home-help-center-modal-card__item"
            @click="handleSelect(item)"
          >
            <div>
              <strong>{{ item.title }}</strong>
            </div>

            <ion-icon :icon="chevronForwardOutline" />
          </button>

          <ion-button fill="outline" @click="handleDismiss">{{ t('home.section.close') }}</ion-button>
        </section>
      </div>
    </ion-content>
  </ion-modal>
</template>

<script setup lang="ts">
import { IonButton, IonContent, IonIcon, IonModal } from '@ionic/vue'
import { chevronForwardOutline } from 'ionicons/icons'
import { useI18n } from 'vue-i18n'
import type { HomeHelpCenterItem } from '@/components/home/HomeHelpCenter.vue'

const { t } = useI18n()

interface Props {
  isOpen: boolean
  items: HomeHelpCenterItem[]
}

defineProps<Props>()

const emit = defineEmits<{
  dismiss: []
  select: [item: HomeHelpCenterItem]
  contact: []
}>()

function handleDismiss() {
  emit('dismiss')
}

function handleSelect(item: HomeHelpCenterItem) {
  emit('select', item)
}

function handleContactSupport() {
  emit('contact')
}
</script>

<style scoped>
.home-help-center-modal-page {
  --padding-top: 16px;
  --padding-bottom: 24px;
  --padding-start: 16px;
  --padding-end: 16px;
}

.home-help-center-modal-card {
  display: grid;
  gap: 12px;
}

.home-help-center-modal-card__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.home-help-center-modal-card__item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  width: 100%;
  padding: 14px;
  border: 1px solid var(--app-border);
  border-radius: 18px;
  background: var(--app-surface);
  color: var(--app-heading);
  text-align: left;
  font: inherit;
}

.home-help-center-modal-card__item strong,
.home-help-center-modal-card__item p {
  margin: 0;
}

.home-help-center-modal-card__item p {
  margin-top: 6px;
  color: var(--app-muted);
  font-size: 13px;
  line-height: 1.5;
}

.home-help-center-modal-card__item ion-icon {
  flex-shrink: 0;
  font-size: 20px;
  color: var(--ion-color-primary);
}
</style>
