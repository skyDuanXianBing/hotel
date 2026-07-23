<template>
  <SettingsCrudPage
    :back-href="ROUTE_PATHS.settings"
    :title="$t('stage5.statistics.notes.paymentReceivedMethod')"
    :hero-eyebrow="$t('settings.groups.finance')"
    :hero-title="$t('stage5.statistics.notes.paymentReceivedMethod')"
    :toolbar-action-label="$t('settingsStage4.roomGroup.addGroup')"
    :show-refresher="true"
    :refresher-pulling-text="$t('stage5UiAttributes.12')"
    :section-title="$t('stage5UiAttributes.49')"
    :loading="loading"
    :modal-open="editorOpen"
    :modal-title="editingMethodId ? $t('stage5DynamicUi.66') : $t('stage5DynamicUi.38')"
    @toolbar-action="handleCreateMethod"
    @refresh="handleRefresh"
    @dismiss-editor="handleDismissEditor"
  >
    <div v-if="methods.length > 0" class="mobile-list settings-minimal-list">
      <article v-for="(method, index) in methods" :key="method.id" class="settings-minimal-card">
        <div class="settings-minimal-card__header">
          <div class="settings-minimal-card__title-group">
            <strong>{{ method.name }}</strong>
            <p class="settings-minimal-card__summary">
              {{ $t('stage5DynamicUi.142') }} {{ method.displayOrder }} · {{ method.enabled ? $t('channel.managementData.statusActive') : $t('stage5DynamicUi.28') }}
            </p>
          </div>
          <span
            class="settings-minimal-card__badge"
            :class="method.enabled ? 'settings-minimal-card__badge--success' : 'settings-minimal-card__badge--warning'"
          >
            {{ method.enabled ? $t('stage5DynamicUi.26') : $t('stage5DynamicUi.28') }}
          </span>
        </div>

        <div class="settings-minimal-card__actions settings-minimal-card__actions--dense">
          <ion-button size="small" fill="outline" @click="handleEditMethod(method)">{{ $t('accommodation.roomPrice.editTitle') }}</ion-button>
          <ion-button size="small" fill="outline" @click="handleToggleMethod(method)">
            {{ method.enabled ? $t('roomStatus.store.roomState.outOfOrder') : $t('settingsStage4.accountList.status.enabled') }}
          </ion-button>
          <ion-button size="small" fill="outline" :disabled="index === 0" @click="handleMove(index, -1)">{{ $t('stage5SourceText.3') }}</ion-button>
          <ion-button size="small" fill="outline" :disabled="index === methods.length - 1" @click="handleMove(index, 1)">{{ $t('stage5SourceText.4') }}</ion-button>
          <ion-button size="small" color="danger" fill="clear" @click="handleDeleteMethod(method)">{{ $t('roomStatus.roomLock.actions.delete') }}</ion-button>
        </div>
      </article>
    </div>

    <p v-else-if="!loading" class="mobile-note">{{ $t('stage5SourceText.80') }}</p>

    <template #sectionFooter>
      <div class="settings-form-actions settings-form-actions--section">
        <ion-button fill="outline" :disabled="loading || savingOrder" @click="loadPageData">{{ $t('accommodation.common.reset') }}</ion-button>
        <ion-button :disabled="loading || savingOrder || methods.length === 0" @click="handleSaveOrder">
          {{ savingOrder ? $t('channel.mobile.common.saving') : $t('stage5DynamicUi.24') }}
        </ion-button>
      </div>
    </template>

    <template #modalContent>
      <div class="settings-form-grid">
        <label class="settings-form-field">
          <span>{{ $t('roomStatus.roomLock.fields.name') }}</span>
          <ion-input v-model="methodForm.name" fill="outline" :placeholder="$t('stage5UiAttributes.81')" />
        </label>

        <div class="settings-toggle-field">
          <div>
            <strong>{{ $t('stage5SourceText.35') }}</strong>
          </div>
          <ion-toggle v-model="methodForm.enabled" />
        </div>
      </div>
    </template>

    <template #modalActions>
      <ion-button fill="outline" @click="handleDismissEditor">{{ $t('accommodation.common.cancel') }}</ion-button>
      <ion-button :disabled="submitting" @click="handleSaveMethod">
        {{ submitting ? $t('iosStage5.cleaning.submitting') : $t('stage5DynamicUi.13') }}
      </ion-button>
    </template>
  </SettingsCrudPage>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import {
  alertController,
  IonButton,
  IonInput,
  IonToggle,
  onIonViewWillEnter,
} from '@ionic/vue'
import { ref } from 'vue'
import {
  createPaymentMethod,
  deletePaymentMethod,
  getAllPaymentMethods,
  updatePaymentMethod,
  updatePaymentMethodEnabled,
  updatePaymentMethodsOrder,
} from '@/api/paymentMethod'
import SettingsCrudPage from '@/components/settings/families/SettingsCrudPage.vue'
import { ROUTE_PATHS } from '@/router/guards'
import type { PaymentMethodDTO } from '@/types/settings'
import { showSuccessToast, showWarningToast } from '@/utils/notify'
import { isHandledRequestError } from '@/utils/request'
import { moveArrayItem } from '@/utils/settings'

const { t } = useI18n()

interface PaymentMethodFormState {
  name: string
  enabled: boolean
}

const loading = ref(false)
const submitting = ref(false)
const savingOrder = ref(false)
const editorOpen = ref(false)
const editingMethodId = ref<number | null>(null)
const methods = ref<PaymentMethodDTO[]>([])
const methodForm = ref<PaymentMethodFormState>(createEmptyForm())

function createEmptyForm(): PaymentMethodFormState {
  return {
    name: '',
    enabled: true,
  }
}

function resolveWarningMessage(error: unknown, fallbackMessage: string) {
  if (error instanceof Error && error.message) {
    return error.message
  }
  return fallbackMessage
}

async function confirmDelete(name: string) {
  const alert = await alertController.create({
    header: t('settingsResidual.common.confirm'),
    message: t('settingsResidual.common.confirmDelete', { name }),
    buttons: [
      { text: t('accommodation.common.cancel'), role: 'cancel' },
      { text: t('settingsStage4.roomSettings.messages.deleteTitle'), role: 'destructive' },
    ],
  })
  await alert.present()
  const result = await alert.onDidDismiss()
  return result.role === 'destructive'
}

async function loadPageData() {
  loading.value = true
  try {
    const response = await getAllPaymentMethods()
    if (!response.success || !response.data) {
      throw new Error(response.message || t('stage5Pattern.loadFailed'))
    }
    methods.value = [...response.data].sort((left, right) => left.displayOrder - right.displayOrder)
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, t('stage5Pattern.loadFailed')))
    }
  } finally {
    loading.value = false
  }
}

function handleCreateMethod() {
  editingMethodId.value = null
  methodForm.value = createEmptyForm()
  editorOpen.value = true
}

function handleEditMethod(method: PaymentMethodDTO) {
  editingMethodId.value = method.id
  methodForm.value = {
    name: method.name,
    enabled: method.enabled,
  }
  editorOpen.value = true
}

function handleDismissEditor() {
  editorOpen.value = false
  editingMethodId.value = null
  methodForm.value = createEmptyForm()
}

async function handleSaveMethod() {
  if (!methodForm.value.name.trim()) {
    showWarningToast(t('stage5UiAttributes.81'))
    return
  }

  submitting.value = true
  try {
    if (editingMethodId.value) {
      const response = await updatePaymentMethod(editingMethodId.value, {
        name: methodForm.value.name.trim(),
        enabled: methodForm.value.enabled,
      })
      if (!response.success) {
        throw new Error(response.message || t('stage5Pattern.updateFailed'))
      }
    } else {
      const response = await createPaymentMethod({
        name: methodForm.value.name.trim(),
        enabled: methodForm.value.enabled,
      })
      if (!response.success) {
        throw new Error(response.message || t('stage5Pattern.createFailed'))
      }
    }

    showSuccessToast(t('stage5Pattern.saveCompleted'))
    handleDismissEditor()
    await loadPageData()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, t('stage5Pattern.saveFailed')))
    }
  } finally {
    submitting.value = false
  }
}

async function handleToggleMethod(method: PaymentMethodDTO) {
  try {
    const response = await updatePaymentMethodEnabled(method.id, !method.enabled)
    if (!response.success) {
      throw new Error(response.message || t('settingsStage4.consumptionItems.messages.statusUpdateFailed'))
    }
    showSuccessToast(
      response.data.enabled
        ? t('settingsResidual.common.enabled')
        : t('settingsResidual.common.disabled'),
    )
    await loadPageData()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, t('stage5Pattern.updateFailed')))
    }
  }
}

function handleMove(index: number, delta: number) {
  methods.value = moveArrayItem(methods.value, index, index + delta)
}

async function handleSaveOrder() {
  savingOrder.value = true
  try {
    const payload = methods.value.map((item, index) => ({
      id: item.id,
      displayOrder: index + 1,
    }))
    const response = await updatePaymentMethodsOrder(payload)
    if (!response.success) {
      throw new Error(response.message || t('stage5Pattern.saveFailed'))
    }
    showSuccessToast(t('stage5Pattern.saveCompleted'))
    await loadPageData()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, t('stage5Pattern.saveFailed')))
    }
  } finally {
    savingOrder.value = false
  }
}

async function handleDeleteMethod(method: PaymentMethodDTO) {
  const confirmed = await confirmDelete(method.name)
  if (!confirmed) {
    return
  }

  try {
    const response = await deletePaymentMethod(method.id)
    if (!response.success) {
      throw new Error(response.message || t('stage5Pattern.deleteFailed'))
    }
    showSuccessToast(t('stage5Pattern.deleteCompleted'))
    await loadPageData()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, t('stage5Pattern.deleteFailed')))
    }
  }
}

async function handleRefresh(event: CustomEvent) {
  await loadPageData()
  event.detail.complete()
}

onIonViewWillEnter(async () => {
  await loadPageData()
})
</script>
