<template>
  <ion-modal
    :is-open="isOpen"
    :class="{ 'settings-memo-modal': showWorkbench }"
    @didDismiss="handleDismiss"
  >
    <ion-header translucent :class="{ 'settings-memo-header': showWorkbench }">
      <ion-toolbar :class="{ 'settings-memo-toolbar': showWorkbench }">
        <ion-buttons v-if="showWorkbench" slot="start">
          <ion-button class="settings-memo-back" @click="handleDismiss">
            <ion-icon :icon="chevronBackOutline" />
            <span>{{ t('tools.workbench.back') }}</span>
          </ion-button>
        </ion-buttons>
        <ion-title :class="{ 'settings-memo-title': showWorkbench }">
          {{ t('tools.memo') }}
        </ion-title>
        <ion-buttons v-if="!showWorkbench" slot="end">
          <ion-button @click="handleDismiss">{{ t('tools.close') }}</ion-button>
        </ion-buttons>
      </ion-toolbar>
    </ion-header>

    <ion-content
      class="mobile-page memo-sheet-page"
      :class="{ 'memo-sheet-page--workbench': showWorkbench }"
    >
      <div class="mobile-stack">
        <SettingsMemoWorkbench
          v-if="showWorkbench"
          :is-open="isOpen"
          :memo-auto-saving="memoStore.autoSaving"
          :memo-loading="memoStore.loading"
          :memo-status-text="memoStore.saveStatusText"
          :memo-value="memoValue"
          @dismiss="handleDismiss"
          @update:memo-value="handleMemoUpdate"
        />
        <HomeMemoCard
          v-else
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
  IonIcon,
  IonModal,
  IonTitle,
  IonToolbar,
} from '@ionic/vue'
import { chevronBackOutline } from 'ionicons/icons'
import { computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import HomeMemoCard from '@/components/home/HomeMemoCard.vue'
import SettingsMemoWorkbench from '@/components/global/SettingsMemoWorkbench.vue'
import { useMemoStore } from '@/stores/memo'

interface Props {
  isOpen: boolean
  showWorkbench?: boolean
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

const handleMemoUpdate = (value: string) => {
  memoStore.saveMemoDebounced(value)
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

.memo-sheet-page--workbench {
  --background: var(--ios-pms-bg-page);
  --padding-top: 12px;
  --padding-bottom: calc(30px + var(--app-safe-bottom));
  background: var(--ios-pms-bg-page);
}

:global(ion-modal.settings-memo-modal) {
  --width: 100%;
  --height: 100%;
  --border-radius: 0;
}

.settings-memo-header {
  box-shadow: none;
}

.settings-memo-toolbar {
  --background: rgba(255, 255, 255, 0.93);
  --border-color: transparent;
  --min-height: 58px;
}

.settings-memo-title {
  color: var(--ios-pms-header-title-color);
  font-size: 20px;
  font-weight: var(--ios-pms-weight-medium);
  letter-spacing: 0;
}

.settings-memo-back {
  --color: var(--ios-pms-header-control-color);
  --padding-start: 4px;
  --padding-end: 4px;
  font-size: 15px;
}

.settings-memo-back ion-icon {
  margin-right: 1px;
  font-size: 22px;
}
</style>
