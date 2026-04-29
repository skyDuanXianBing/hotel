<template>
  <ion-page>
    <ion-header translucent>
      <ion-toolbar class="app-page-header__toolbar">
        <ion-buttons slot="start">
          <ion-button fill="clear" class="shortcut-manage-toolbar__back app-page-header__icon-btn" @click="handleBack">
            <ion-icon slot="icon-only" :icon="chevronBackOutline" />
          </ion-button>
        </ion-buttons>
        <ion-title class="app-page-header__title">管理</ion-title>
        <ion-buttons slot="end">
          <ion-button class="app-page-header__text-btn" fill="clear" :disabled="!hasChanges" @click="handleSave">保存</ion-button>
        </ion-buttons>
      </ion-toolbar>
    </ion-header>

    <ion-content fullscreen class="mobile-page shortcut-manage-page">
      <div class="shortcut-manage-shell">
        <section class="shortcut-manage-section">
          <h2 class="shortcut-manage-section__title">显示</h2>

          <div v-if="visibleItems.length > 0" class="shortcut-manage-grid">
            <button
              v-for="item in visibleItems"
              :key="item.key"
              type="button"
              class="shortcut-manage-item"
              :aria-label="`隐藏${item.title}`"
              @click="handleHide(item.key)"
            >
              <span class="shortcut-manage-item__action shortcut-manage-item__action--remove" aria-hidden="true">
                <ion-icon :icon="removeOutline" />
              </span>
              <div class="shortcut-manage-item__icon-shell">
                <div class="shortcut-manage-item__icon" :class="`shortcut-manage-item__icon--${item.tone}`">
                  <ion-icon :icon="item.icon" />
                </div>
              </div>
              <span class="shortcut-manage-item__label">{{ item.title }}</span>
            </button>
          </div>

          <p v-else class="mobile-note shortcut-manage-section__empty">当前未显示任何快捷方式。</p>
        </section>

        <section class="shortcut-manage-section">
          <h2 class="shortcut-manage-section__title">隐藏</h2>

          <div v-if="hiddenItems.length > 0" class="shortcut-manage-grid">
            <button
              v-for="item in hiddenItems"
              :key="item.key"
              type="button"
              class="shortcut-manage-item"
              :aria-label="`显示${item.title}`"
              @click="handleShow(item.key)"
            >
              <span class="shortcut-manage-item__action shortcut-manage-item__action--add" aria-hidden="true">
                <ion-icon :icon="addOutline" />
              </span>
              <div class="shortcut-manage-item__icon-shell">
                <div class="shortcut-manage-item__icon" :class="`shortcut-manage-item__icon--${item.tone}`">
                  <ion-icon :icon="item.icon" />
                </div>
              </div>
              <span class="shortcut-manage-item__label">{{ item.title }}</span>
            </button>
          </div>

          <p v-else class="mobile-note shortcut-manage-section__empty">当前没有已隐藏的快捷方式。</p>
        </section>
      </div>
    </ion-content>
  </ion-page>
</template>

<script setup lang="ts">
import {
  alertController,
  IonButton,
  IonButtons,
  IonContent,
  IonHeader,
  IonIcon,
  IonPage,
  IonTitle,
  IonToolbar,
  onIonViewWillEnter,
} from '@ionic/vue'
import { addOutline, chevronBackOutline, removeOutline } from 'ionicons/icons'
import { computed, ref } from 'vue'
import { onBeforeRouteLeave, useRouter } from 'vue-router'
import {
  HOME_QUICK_ACTION_DEFINITIONS,
  normalizeHomeQuickActionKeys,
} from '@/constants/homeQuickActions'
import { ROUTE_PATHS } from '@/router/guards'
import { useHomeShortcutsStore } from '@/stores/homeShortcuts'
import { showSuccessToast } from '@/utils/notify'

const router = useRouter()
const homeShortcutsStore = useHomeShortcutsStore()

const allowRouteLeave = ref(false)
const draftVisibleKeys = ref<string[]>([])

const normalizedDraftVisibleKeys = computed(() => {
  return normalizeHomeQuickActionKeys(draftVisibleKeys.value)
})

const visibleItems = computed(() => {
  const visibleKeySet = new Set(normalizedDraftVisibleKeys.value)
  return HOME_QUICK_ACTION_DEFINITIONS.filter((item) => visibleKeySet.has(item.key))
})

const hiddenItems = computed(() => {
  const visibleKeySet = new Set(normalizedDraftVisibleKeys.value)
  return HOME_QUICK_ACTION_DEFINITIONS.filter((item) => !visibleKeySet.has(item.key))
})

const hasChanges = computed(() => {
  return normalizedDraftVisibleKeys.value.join('|') !== homeShortcutsStore.visibleKeys.join('|')
})

const syncDraftWithStore = () => {
  allowRouteLeave.value = false
  draftVisibleKeys.value = [...homeShortcutsStore.visibleKeys]
}

const confirmDiscardChanges = async () => {
  const alert = await alertController.create({
    header: '放弃修改',
    message: '当前快捷方式调整尚未保存，确定返回吗？',
    buttons: [
      { text: '继续编辑', role: 'cancel' },
      { text: '放弃修改', role: 'confirm' },
    ],
  })

  await alert.present()
  const result = await alert.onDidDismiss()
  return result.role === 'confirm'
}

const navigateBackToHome = async () => {
  allowRouteLeave.value = true
  await router.replace(ROUTE_PATHS.home)
}

const handleHide = (key: string) => {
  draftVisibleKeys.value = draftVisibleKeys.value.filter((item) => item !== key)
}

const handleShow = (key: string) => {
  draftVisibleKeys.value = normalizeHomeQuickActionKeys([...draftVisibleKeys.value, key])
}

const handleBack = async () => {
  if (hasChanges.value && !(await confirmDiscardChanges())) {
    return
  }

  await navigateBackToHome()
}

const handleSave = async () => {
  homeShortcutsStore.setVisibleKeys(normalizedDraftVisibleKeys.value)
  showSuccessToast('快捷方式已更新')
  await navigateBackToHome()
}

onIonViewWillEnter(() => {
  syncDraftWithStore()
})

onBeforeRouteLeave(async () => {
  if (allowRouteLeave.value || !hasChanges.value) {
    return true
  }

  return confirmDiscardChanges()
})
</script>

<style scoped>
.shortcut-manage-page {
  --background: var(--ios-pms-bg-page-plain);
}

.shortcut-manage-shell {
  display: grid;
  gap: 36px;
  padding-top: 22px;
}

.shortcut-manage-section {
  display: grid;
  gap: 18px;
}

.shortcut-manage-section__title {
  margin: 0;
  color: var(--ios-pms-text-primary);
  font-size: var(--ios-pms-font-title-lg-size);
  font-weight: var(--ios-pms-weight-heavy);
  letter-spacing: -0.03em;
}

.shortcut-manage-section__empty {
  padding: 4px 2px 0;
}

.shortcut-manage-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 28px 12px;
}

.shortcut-manage-item {
  position: relative;
  display: grid;
  justify-items: center;
  gap: 7px;
  width: 100%;
  min-width: 0;
  padding: 0;
  border: none;
  background: transparent;
  color: inherit;
  font: inherit;
  transition:
    transform 0.18s ease,
    opacity 0.18s ease;
}

.shortcut-manage-item:active {
  transform: scale(0.98);
  opacity: 0.82;
}

.shortcut-manage-item:focus-visible {
  outline: none;
}

.shortcut-manage-item__action {
  position: absolute;
  top: -8px;
  right: 4px;
  display: grid;
  place-items: center;
  width: 28px;
  height: 28px;
  border-radius: 999px;
  box-shadow: 0 6px 16px rgba(96, 117, 152, 0.12);
  font-size: 15px;
}

.shortcut-manage-item__action--remove {
  background: rgba(235, 239, 245, 0.96);
  color: #697586;
}

.shortcut-manage-item__action--add {
  background: rgba(52, 116, 246, 0.14);
  color: var(--ion-color-primary);
}

.shortcut-manage-item__icon-shell {
  display: grid;
  place-items: center;
  width: 54px;
  height: 54px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.56);
}

.shortcut-manage-item__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  border-radius: 16px;
  font-size: 28px;
}

.shortcut-manage-item__icon--primary {
  color: var(--ion-color-primary);
}

.shortcut-manage-item__icon--warning {
  color: var(--ion-color-warning);
}

.shortcut-manage-item__icon--secondary {
  color: var(--ion-color-secondary);
}

.shortcut-manage-item__icon--success {
  color: var(--ion-color-success);
}

.shortcut-manage-item__label {
  color: var(--ios-pms-text-secondary);
  font-size: 14px;
  font-weight: var(--ios-pms-weight-bold);
  line-height: 1.2;
  text-align: center;
}

@media (max-width: 374px) {
  .shortcut-manage-shell {
    gap: 32px;
    padding-top: 18px;
  }

  .shortcut-manage-grid {
    gap: 24px 10px;
  }

  .shortcut-manage-item__action {
    right: 0;
  }

  .shortcut-manage-item__icon-shell {
    width: 48px;
    height: 48px;
  }

  .shortcut-manage-item__icon {
    width: 42px;
    height: 42px;
    font-size: 24px;
  }

  .shortcut-manage-item__label {
    font-size: 12px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .shortcut-manage-item {
    transition: none;
  }
}
</style>
