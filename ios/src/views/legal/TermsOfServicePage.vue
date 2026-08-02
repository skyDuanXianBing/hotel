<template>
  <ion-page>
    <ion-content fullscreen class="legal-page">
      <div class="legal-page__shell">
        <button class="legal-page__back" type="button" @click="handleBack">
          <ion-icon :icon="chevronBackOutline" aria-hidden="true" />
          {{ t('legal.terms.title') }}
        </button>
        <LegalDocument document="terms" />
      </div>
    </ion-content>
  </ion-page>
</template>

<script setup lang="ts">
import { IonContent, IonIcon, IonPage } from '@ionic/vue'
import { chevronBackOutline } from 'ionicons/icons'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import LegalDocument from '@/components/legal/LegalDocument.vue'
import { ROUTE_PATHS } from '@/router/guards'

const { t } = useI18n()
const router = useRouter()

const handleBack = async () => {
  if (window.history.length > 1) {
    await router.back()
    return
  }

  await router.replace(ROUTE_PATHS.login)
}
</script>

<style scoped>
.legal-page__shell {
  max-width: 760px;
  margin: 0 auto;
  padding: calc(16px + env(safe-area-inset-top)) 20px 40px;
}

.legal-page__back {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin: 0 0 16px;
  padding: 0;
  border: 0;
  background: transparent;
  color: #1f2430;
  font: inherit;
  font-size: 17px;
  font-weight: 600;
  cursor: pointer;
}

.legal-page__back ion-icon {
  font-size: 20px;
}
</style>
