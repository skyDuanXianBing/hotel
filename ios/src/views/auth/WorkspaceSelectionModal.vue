<template>
  <ion-modal
    :is-open="open"
    :backdrop-dismiss="false"
    class="workspace-selection-modal"
    @did-dismiss="emit('dismiss')"
  >
    <ion-header>
      <ion-toolbar>
        <ion-title>选择工作台</ion-title>
      </ion-toolbar>
    </ion-header>

    <ion-content class="ion-padding">
      <p class="workspace-selection__description">
        该账号可以进入多个工作台，请选择本次要进入的工作空间。
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
        返回登录
      </ion-button>
    </ion-content>
  </ion-modal>
</template>

<script setup lang="ts">
import { IonButton, IonContent, IonHeader, IonModal, IonTitle, IonToolbar } from '@ionic/vue'
import type { LoginTarget } from '@/types/auth'

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
  return target === 'CLEANER' ? '保洁工作台' : '酒店管理工作台'
}

const getWorkspaceDescription = (target: LoginTarget) => {
  return target === 'CLEANER'
    ? '查看保洁任务与房间清洁安排'
    : '管理门店、房态、订单与经营数据'
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
  font-size: 17px;
  font-weight: 600;
}

.workspace-selection__option span {
  color: #74777e;
  font-size: 14px;
  line-height: 1.45;
}

.workspace-selection__back {
  margin-top: 16px;
}
</style>
