<template>
  <ion-modal :is-open="isOpen" @didDismiss="handleDismiss">
    <ion-header>
      <ion-toolbar>
        <ion-title>{{ t('tools.memo') }}</ion-title>
        <ion-buttons slot="end">
          <ion-button @click="handleDismiss">{{ t('tools.close') }}</ion-button>
        </ion-buttons>
      </ion-toolbar>
    </ion-header>

    <ion-content class="mobile-page memo-sheet-page">
      <div class="mobile-stack">
        <HomeMemoCard
          v-model="memoValue"
          :auto-saving="memoStore.autoSaving"
          :loading="memoStore.loading"
          :status-text="memoStore.saveStatusText"
        />
      </div>
    </ion-content>
  </ion-modal>
</template>

<script setup lang="ts">
import {
  IonButton,
  IonButtons,
  IonContent,
  IonHeader,
  IonModal,
  IonTitle,
  IonToolbar,
} from '@ionic/vue'
import { computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import HomeMemoCard from '@/components/home/HomeMemoCard.vue'
import { useMemoStore } from '@/stores/memo'

interface Props {
  isOpen: boolean
}

const props = defineProps<Props>()

const emit = defineEmits<{
  dismiss: []
}>()

const memoStore = useMemoStore()
const { t } = useI18n()

const memoValue = computed({
  get: () => memoStore.memoContent,
  set: (value: string) => {
    memoStore.saveMemoDebounced(value)
  },
})

const handleDismiss = () => {
  emit('dismiss')
}

watch(
  () => props.isOpen,
  async (nextOpen) => {
    if (!nextOpen) {
      return
    }

    if (!memoStore.hasLoaded) {
      try {
        await memoStore.loadMemo(false)
      } catch {
        return
      }
    }
  },
)
</script>

<style scoped>
.memo-sheet-page {
  --padding-top: 16px;
  --padding-bottom: 24px;
  --padding-start: 16px;
  --padding-end: 16px;
}
</style>
