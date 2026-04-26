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
  left: 18px;
  bottom: calc(env(safe-area-inset-bottom) + 84px);
  z-index: 1200;
}

.quick-tools-trigger {
  display: flex;
  align-items: center;
  gap: 8px;
  border: 1px solid rgba(255, 255, 255, 0.26);
  border-radius: 999px;
  padding: 13px 18px;
  background: linear-gradient(135deg, #2d73ff, #4d8bff);
  color: #fff;
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.28),
    0 16px 28px rgba(68, 116, 238, 0.24);
  font: inherit;
  font-size: 14px;
  font-weight: 700;
}

.quick-tools-trigger ion-icon {
  font-size: 18px;
}

.quick-tools-sheet-page {
  --padding-top: 20px;
  --padding-bottom: 24px;
  --padding-start: 16px;
  --padding-end: 16px;
}

.quick-tools-sheet-card {
  display: grid;
  gap: 14px;
}

.quick-tools-sheet-item {
  display: flex;
  align-items: center;
  gap: 14px;
  width: 100%;
  padding: 16px;
  border: 1px solid rgba(168, 183, 216, 0.16);
  border-radius: 20px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(247, 250, 255, 0.98));
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.82),
    0 10px 24px rgba(121, 146, 190, 0.08);
  color: #171f34;
  text-align: left;
  font: inherit;
  transition: transform 0.18s ease, background 0.18s ease;
}

.quick-tools-sheet-item:active {
  transform: scale(0.985);
  background: rgba(115, 164, 255, 0.06);
}

.quick-tools-sheet-item strong {
  margin: 0;
  font-size: 15px;
  font-weight: 700;
}

.quick-tools-sheet-item ion-icon {
  flex-shrink: 0;
  font-size: 22px;
  color: #3474f6;
}
</style>
