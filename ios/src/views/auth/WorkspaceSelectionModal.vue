<template>
  <ion-modal
    :is-open="open"
    :backdrop-dismiss="false"
    class="workspace-selection-modal"
    @did-dismiss="emit('dismiss')"
  >
    <ion-header>
      <ion-toolbar>
        <ion-title>{{ t('auth.workspace.title') }}</ion-title>
      </ion-toolbar>
    </ion-header>

    <ion-content class="ion-padding">
      <p class="workspace-selection__description">
        {{ t('auth.workspace.description') }}
      </p>

      <ul class="workspace-selection__options">
        <li v-for="target in targets" :key="target">
          <button
            class="workspace-selection__option"
            type="button"
            :disabled="loading"
            @click="emit('select', target)"
          >
            <strong>{{ getWorkspaceTitle(target) }}</strong>
            <span>{{ getWorkspaceDescription(target) }}</span>
          </button>
        </li>
      </ul>

      <ion-button
        expand="block"
        fill="clear"
        class="workspace-selection__back"
        :disabled="loading"
        @click="emit('dismiss')"
      >
        {{ t('auth.workspace.back') }}
      </ion-button>
    </ion-content>
  </ion-modal>
</template>

<script setup lang="ts">
import { IonButton, IonContent, IonHeader, IonModal, IonTitle, IonToolbar } from '@ionic/vue'
import { useI18n } from 'vue-i18n'
import type { LoginTarget } from '@/types/auth'

const { t } = useI18n()

defineProps<{
  open: boolean
  targets: LoginTarget[]
  loading: boolean
}>()

const emit = defineEmits<{
  (event: 'select', target: LoginTarget): void
  (event: 'dismiss'): void
}>()

const getWorkspaceTitle = (target: LoginTarget) => {
  return target === 'CLEANER' ? t('auth.workspace.cleaner') : t('auth.workspace.pms')
}

const getWorkspaceDescription = (target: LoginTarget) => {
  return target === 'CLEANER'
    ? t('auth.workspace.cleanerDescription')
    : t('auth.workspace.pmsDescription')
}
</script>

<style scoped>
.workspace-selection__description {
  margin: 4px 0 20px;
  color: #6f7279;
  font-size: 15px;
  line-height: 1.55;
}

.workspace-selection__options {
  display: grid;
  gap: 12px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.workspace-selection__option {
  display: flex;
  min-height: 88px;
  flex-direction: column;
  justify-content: center;
  gap: 7px;
  width: 100%;
  padding: 16px;
  border: 1px solid #d7d9dd;
  border-radius: 12px;
  background: #ffffff;
  color: #2b2d33;
  text-align: left;
}

.workspace-selection__option:focus-visible {
  border-color: #078ff4;
  outline: 3px solid rgba(7, 143, 244, 0.2);
  outline-offset: 2px;
}

.workspace-selection__option:disabled {
  opacity: 0.55;
}

.workspace-selection__option strong {
  overflow-wrap: anywhere;
  font-size: 17px;
  font-weight: 600;
}

.workspace-selection__option span {
  overflow-wrap: anywhere;
  color: #74777e;
  font-size: 14px;
  line-height: 1.45;
}

.workspace-selection__back {
  margin-top: 16px;
}
</style>
