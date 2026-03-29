<template>
  <ion-app class="mobile-app-root">
    <div class="mobile-app-background" aria-hidden="true"></div>
    <ion-router-outlet id="app-router-outlet" />
    <div id="app-overlay-root"></div>
    <ion-loading :is-open="appStore.isRestoring" message="正在恢复会话..." />
    <ion-toast
      :is-open="appStore.toastOpen"
      :message="appStore.toastMessage"
      :duration="appStore.toastDuration"
      :color="appStore.toastColor"
      position="top"
      @didDismiss="appStore.dismissToast"
    />
    <AppNotificationOverlay />
  </ion-app>
</template>

<script setup lang="ts">
import { IonApp, IonLoading, IonRouterOutlet, IonToast } from '@ionic/vue'
import { onBeforeUnmount, onMounted } from 'vue'
import AppNotificationOverlay from '@/components/global/AppNotificationOverlay.vue'
import { useAppStore } from '@/stores/app'
import { APP_TOAST_EVENT_NAME, type AppToastEventDetail } from '@/utils/notify'

const appStore = useAppStore()

const handleToastEvent: EventListener = (event) => {
  const toastEvent = event as CustomEvent<AppToastEventDetail>
  appStore.showToast(toastEvent.detail)
}

onMounted(() => {
  void appStore.restoreRuntimeState()

  if (typeof window === 'undefined') {
    return
  }

  window.addEventListener(APP_TOAST_EVENT_NAME, handleToastEvent)
})

onBeforeUnmount(() => {
  if (typeof window === 'undefined') {
    return
  }

  window.removeEventListener(APP_TOAST_EVENT_NAME, handleToastEvent)
})
</script>

<style scoped>
.mobile-app-root {
  position: relative;
  background: var(--app-background);
}

.mobile-app-background {
  position: fixed;
  inset: 0;
  z-index: 0;
  pointer-events: none;
  background:
    radial-gradient(circle at top right, rgba(37, 99, 235, 0.14), transparent 36%),
    radial-gradient(circle at top left, rgba(96, 165, 250, 0.1), transparent 30%),
    var(--app-background);
}

#app-router-outlet,
#app-overlay-root {
  position: relative;
  z-index: 1;
}
</style>
