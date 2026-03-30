<template>
  <div class="quick-tools-root">
    <button class="quick-tools-trigger" type="button" @click="toolMenuOpen = true">
      <ion-icon :icon="addOutline" />
      <span>工具</span>
    </button>

    <ion-modal
      :is-open="toolMenuOpen"
      :breakpoints="[0, 0.4, 0.72]"
      :initial-breakpoint="0.4"
      @didDismiss="handleDismissToolMenu"
    >
      <ion-content class="mobile-page quick-tools-sheet-page">
        <div class="mobile-stack">
          <section class="mobile-card quick-tools-sheet-card">
            <h2 class="mobile-section-title">快捷工具</h2>

            <button class="quick-tools-sheet-item" type="button" @click="handleOpenMemo">
              <ion-icon :icon="documentTextOutline" />
              <strong>备忘录</strong>
            </button>

            <button class="quick-tools-sheet-item" type="button" @click="handleOpenRecord">
              <ion-icon :icon="createOutline" />
              <strong>记一笔</strong>
            </button>

            <button class="quick-tools-sheet-item" type="button" @click="handleOpenContact">
              <ion-icon :icon="headsetOutline" />
              <strong>联系客服</strong>
            </button>

            <ion-button fill="outline" @click="handleDismissToolMenu">收起</ion-button>
          </section>
        </div>
      </ion-content>
    </ion-modal>

    <MemoSheetModal :is-open="visibleToolsStore.memoOpen" @dismiss="visibleToolsStore.closeMemo" />

    <RecordTransactionModal
      :is-open="visibleToolsStore.recordOpen"
      @dismiss="visibleToolsStore.closeRecord"
      @success="handleRecordSuccess"
    />

    <ContactSupportModal :is-open="visibleToolsStore.contactOpen" @dismiss="visibleToolsStore.closeContact" />
  </div>
</template>

<script setup lang="ts">
import { IonButton, IonContent, IonIcon, IonModal } from '@ionic/vue'
import { addOutline, createOutline, documentTextOutline, headsetOutline } from 'ionicons/icons'
import { ref } from 'vue'
import ContactSupportModal from '@/components/global/ContactSupportModal.vue'
import MemoSheetModal from '@/components/global/MemoSheetModal.vue'
import RecordTransactionModal from '@/components/notes/RecordTransactionModal.vue'
import { useVisibleToolsStore } from '@/stores/visibleTools'
import { showSuccessToast } from '@/utils/notify'

const visibleToolsStore = useVisibleToolsStore()

const toolMenuOpen = ref(false)

const handleDismissToolMenu = () => {
  toolMenuOpen.value = false
}

const handleOpenMemo = () => {
  toolMenuOpen.value = false
  visibleToolsStore.openMemo()
}

const handleOpenRecord = () => {
  toolMenuOpen.value = false
  visibleToolsStore.openRecord()
}

const handleOpenContact = () => {
  toolMenuOpen.value = false
  visibleToolsStore.openContact()
}

const handleRecordSuccess = () => {
  showSuccessToast('已完成记一笔录入')
}
</script>

<style scoped>
.quick-tools-root {
  position: fixed;
  right: 18px;
  bottom: calc(env(safe-area-inset-bottom) + 82px);
  z-index: 1200;
}

.quick-tools-trigger {
  display: flex;
  align-items: center;
  gap: 8px;
  border: none;
  border-radius: 999px;
  padding: 12px 16px;
  background: linear-gradient(135deg, #2563eb, #1d4ed8);
  color: #fff;
  box-shadow: var(--app-floating-shadow);
  font: inherit;
  font-size: 14px;
  font-weight: 700;
}

.quick-tools-trigger ion-icon {
  font-size: 18px;
}

.quick-tools-sheet-page {
  --padding-top: 16px;
  --padding-bottom: 24px;
  --padding-start: 16px;
  --padding-end: 16px;
}

.quick-tools-sheet-card {
  display: grid;
  gap: 12px;
}

.quick-tools-sheet-item {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  padding: 14px;
  border: 1px solid var(--app-border);
  border-radius: 14px;
  background: var(--app-surface-strong);
  color: var(--app-heading);
  text-align: left;
  font: inherit;
  transition: background 0.15s ease;
}

.quick-tools-sheet-item:active {
  background: var(--app-primary-soft);
}

.quick-tools-sheet-item strong {
  margin: 0;
  font-size: 15px;
}

.quick-tools-sheet-item ion-icon {
  flex-shrink: 0;
  font-size: 22px;
  color: var(--ion-color-primary);
}

.quick-tools-sheet-item ion-icon {
  flex-shrink: 0;
  font-size: 22px;
  color: var(--ion-color-primary);
}
</style>
