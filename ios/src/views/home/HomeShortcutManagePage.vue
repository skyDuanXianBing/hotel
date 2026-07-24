<template>
  <ion-page>
    <ion-header translucent>
      <ion-toolbar class="app-page-header__toolbar">
        <ion-buttons slot="start">
          <ion-button fill="clear" class="shortcut-manage-toolbar__back app-page-header__text-btn" @click="handleBack">
            <ion-icon slot="start" :icon="chevronBackOutline" aria-hidden="true" />
            {{ t('common.back') }}
          </ion-button>
        </ion-buttons>
        <ion-title class="app-page-header__title">{{ t('home.manage.title') }}</ion-title>
        <ion-buttons slot="end">
          <ion-button
            class="shortcut-manage-toolbar__save app-page-header__text-btn"
            fill="clear"
            :disabled="!hasChanges"
            @click="handleSave"
          >
            {{ t('home.manage.save') }}
          </ion-button>
        </ion-buttons>
      </ion-toolbar>
    </ion-header>

    <ion-content fullscreen class="mobile-page mobile-page--dashboard shortcut-manage-page">
      <div class="shortcut-manage-shell">
        <section class="shortcut-manage-card mobile-dashboard-surface">
          <section class="shortcut-manage-section">
            <h2 class="shortcut-manage-section__title">{{ t('home.manage.visible') }}</h2>

            <div v-if="visibleItems.length > 0" class="shortcut-manage-grid">
              <button
                v-for="item in visibleItems"
                :key="item.key"
                type="button"
                class="shortcut-manage-item"
                :aria-label="t('home.manage.hideItem', { title: item.title })"
                @click="handleHide(item.key)"
              >
                <span class="shortcut-manage-item__action shortcut-manage-item__action--remove" aria-hidden="true">
                  <ion-icon :icon="removeOutline" />
                </span>
                <div class="shortcut-manage-item__icon-shell">
                  <img
                    class="shortcut-manage-item__icon"
                    :src="item.iconSrc"
                    alt=""
                    draggable="false"
                  />
                </div>
                <span class="shortcut-manage-item__label">{{ item.title }}</span>
              </button>
            </div>

            <p v-else class="mobile-note shortcut-manage-section__empty">{{ t('home.manage.noVisible') }}</p>
          </section>

          <section class="shortcut-manage-section shortcut-manage-section--hidden">
            <h2 class="shortcut-manage-section__title">{{ t('home.manage.hidden') }}</h2>

            <div v-if="hiddenItems.length > 0" class="shortcut-manage-grid">
              <button
                v-for="item in hiddenItems"
                :key="item.key"
                type="button"
                class="shortcut-manage-item"
                :aria-label="t('home.manage.showItem', { title: item.title })"
                @click="handleShow(item.key)"
              >
                <span class="shortcut-manage-item__action shortcut-manage-item__action--add" aria-hidden="true">
                  <ion-icon :icon="addOutline" />
                </span>
                <div class="shortcut-manage-item__icon-shell">
                  <img
                    class="shortcut-manage-item__icon"
                    :src="item.iconSrc"
                    alt=""
                    draggable="false"
                  />
                </div>
                <span class="shortcut-manage-item__label">{{ item.title }}</span>
              </button>
            </div>

            <p v-else class="mobile-note shortcut-manage-section__empty">{{ t('home.manage.noHidden') }}</p>
          </section>
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
import { useI18n } from 'vue-i18n'
import { onBeforeRouteLeave, useRouter } from 'vue-router'
import {
  HOME_QUICK_ACTION_DEFINITIONS,
  localizeHomeQuickAction,
  normalizeHomeQuickActionKeys,
} from '@/constants/homeQuickActions'
import { ROUTE_PATHS } from '@/router/guards'
import { useHomeShortcutsStore } from '@/stores/homeShortcuts'
import { showSuccessToast } from '@/utils/notify'

const router = useRouter()
const { t } = useI18n()
const homeShortcutsStore = useHomeShortcutsStore()

const allowRouteLeave = ref(false)
const draftVisibleKeys = ref<string[]>([])

const normalizedDraftVisibleKeys = computed(() => {
  return normalizeHomeQuickActionKeys(draftVisibleKeys.value)
})

const visibleItems = computed(() => {
  const visibleKeySet = new Set(normalizedDraftVisibleKeys.value)
  return HOME_QUICK_ACTION_DEFINITIONS
    .filter((item) => visibleKeySet.has(item.key))
    .map((item) => localizeHomeQuickAction(item, t))
})

const hiddenItems = computed(() => {
  const visibleKeySet = new Set(normalizedDraftVisibleKeys.value)
  return HOME_QUICK_ACTION_DEFINITIONS
    .filter((item) => !visibleKeySet.has(item.key))
    .map((item) => localizeHomeQuickAction(item, t))
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
    header: t('home.manage.discardTitle'),
    message: t('home.manage.discardMessage'),
    buttons: [
      { text: t('home.manage.continueEditing'), role: 'cancel' },
      { text: t('home.manage.discard'), role: 'confirm' },
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
  showSuccessToast(t('home.manage.saved'))
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
  --background: var(--ios-pms-dashboard-page-background);
}

.shortcut-manage-toolbar__back,
.shortcut-manage-toolbar__save {
  --color: var(--ios-pms-header-title-color);
  align-self: center;
  height: 34px;
  min-width: 52px;
  min-height: 34px;
  margin: 0;
  color: var(--ios-pms-header-title-color);
  font-size: var(--ios-pms-font-title-md-size);
  font-weight: 400;
  line-height: 1;
}

.shortcut-manage-toolbar__back::part(native),
.shortcut-manage-toolbar__save::part(native) {
  display: flex;
  align-items: center;
  height: 34px;
  min-height: 34px;
  line-height: 1;
}

.shortcut-manage-toolbar__back {
  justify-content: flex-start;
}

.shortcut-manage-toolbar__back::part(native) {
  justify-content: flex-start;
}

.shortcut-manage-toolbar__back ion-icon {
  margin-inline-end: 0;
  color: inherit;
  font-size: 25px;
}

.shortcut-manage-toolbar__save {
  --color: #000000;
  justify-content: flex-end;
  color: #000000;
}

.shortcut-manage-toolbar__save::part(native) {
  justify-content: flex-end;
}

.shortcut-manage-shell {
  display: grid;
  padding-top: var(--ios-pms-space-3);
  padding-bottom: var(--ios-pms-space-6);
}

.shortcut-manage-card {
  display: grid;
  gap: var(--ios-pms-space-6);
  padding: 24px 18px 28px;
  border-radius: var(--ios-pms-radius-card);
  background: var(--ios-pms-dashboard-card-background);
}

.shortcut-manage-section {
  display: grid;
  gap: var(--ios-pms-space-4);
}

.shortcut-manage-section--hidden {
  padding-top: var(--ios-pms-space-1);
}

.shortcut-manage-section__title {
  margin: 0;
  color: var(--ios-pms-text-primary);
  font-size: var(--ios-pms-font-title-lg-size);
  font-weight: 400;
  line-height: 1.15;
  letter-spacing: 0;
}

.shortcut-manage-section__empty {
  padding: 0 2px;
}

.shortcut-manage-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 22px 8px;
}

.shortcut-manage-item {
  position: relative;
  display: grid;
  justify-items: center;
  gap: 5px;
  width: 100%;
  min-height: 70px;
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
  top: -5px;
  right: 8px;
  display: grid;
  place-items: center;
  width: 18px;
  height: 18px;
  border-radius: 999px;
  box-shadow: 0 5px 12px rgba(96, 117, 152, 0.1);
  font-size: 11px;
}

.shortcut-manage-item__action--remove {
  background: rgba(223, 228, 236, 0.96);
  color: #697586;
}

.shortcut-manage-item__action--add {
  background: rgba(52, 116, 246, 0.16);
  color: var(--ion-color-primary);
}

.shortcut-manage-item__icon-shell {
  display: grid;
  place-items: center;
  width: 40px;
  height: 40px;
  border-radius: var(--ios-pms-radius-icon);
  background: transparent;
}

.shortcut-manage-item__icon {
  display: block;
  width: 36px;
  height: 36px;
  object-fit: contain;
  user-select: none;
  -webkit-user-drag: none;
}

.shortcut-manage-item__label {
  color: var(--ios-pms-text-secondary);
  font-size: var(--ios-pms-font-body-md-size);
  font-weight: 400;
  line-height: 1.15;
  text-align: center;
  letter-spacing: 0;
}

@media (max-width: 374px) {
  .shortcut-manage-shell {
    padding-top: var(--ios-pms-space-2);
  }

  .shortcut-manage-card {
    gap: var(--ios-pms-space-5);
    padding: 22px 14px 24px;
  }

  .shortcut-manage-grid {
    gap: 20px 6px;
  }

  .shortcut-manage-item__action {
    right: 6px;
    width: 17px;
    height: 17px;
    font-size: 10px;
  }

  .shortcut-manage-item__icon-shell {
    width: 37px;
    height: 37px;
  }

  .shortcut-manage-item__icon {
    width: 34px;
    height: 34px;
  }

  .shortcut-manage-item__label {
    font-size: var(--ios-pms-font-body-sm-size);
  }
}

@media (prefers-reduced-motion: reduce) {
  .shortcut-manage-item {
    transition: none;
  }
}
</style>
