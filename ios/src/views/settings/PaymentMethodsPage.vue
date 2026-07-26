<template>
  <SettingsCrudPage
    class="settings-payment-methods-page"
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
      <article
        v-for="(method, index) in methods"
        :key="method.id"
        class="settings-minimal-card payment-method-card"
      >
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
          <ion-button size="small" fill="solid" @click="handleEditMethod(method)">
            {{ $t('accommodation.roomPrice.editTitle') }}
          </ion-button>
          <ion-button size="small" fill="solid" @click="handleToggleMethod(method)">
            {{ method.enabled ? $t('roomStatus.store.roomState.outOfOrder') : $t('settingsStage4.accountList.status.enabled') }}
          </ion-button>
          <ion-button size="small" fill="outline" :disabled="index === 0" @click="handleMove(index, -1)">{{ $t('stage5SourceText.3') }}</ion-button>
          <ion-button size="small" fill="outline" :disabled="index === methods.length - 1" @click="handleMove(index, 1)">{{ $t('stage5SourceText.4') }}</ion-button>
          <ion-button
            class="payment-method-card__delete-button"
            size="small"
            fill="outline"
            @click="handleDeleteMethod(method)"
          >
            {{ $t('roomStatus.roomLock.actions.delete') }}
          </ion-button>
        </div>
      </article>
    </div>

    <p v-else-if="!loading" class="mobile-note payment-methods-empty">{{ $t('stage5SourceText.80') }}</p>

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

<style scoped>
.settings-payment-methods-page :deep(.settings-crud-page) {
  display: block;
  --background: var(--ios-pms-dashboard-page-background);
  --padding-top: 12px;
  --padding-bottom: calc(30px + var(--app-safe-bottom));
  --padding-start: 16px;
  --padding-end: 16px;
}

.settings-payment-methods-page :deep(.app-page-header__title) {
  color: #333333;
  font-size: 20px;
  font-weight: 500;
  letter-spacing: 0;
}

.settings-payment-methods-page :deep(.app-page-header__text-btn) {
  --color: #333333;
  font-size: 16px;
  font-weight: 400;
  letter-spacing: 0;
}

.settings-payment-methods-page :deep(.settings-page-block__hero) {
  display: none;
}

.settings-payment-methods-page :deep(.settings-page-shell__stack) {
  gap: 0;
  margin-top: 0;
  padding-bottom: 6px;
}

.settings-payment-methods-page :deep(.settings-page-shell__stack > .mobile-card) {
  padding: 18px 16px 22px;
  border: 1px solid var(--ios-pms-dashboard-card-border);
  border-radius: var(--ios-pms-radius-card);
  background: var(--ios-pms-dashboard-card-background);
  box-shadow: var(--ios-pms-dashboard-card-shadow);
}

.settings-payment-methods-page :deep(.settings-page-block__section-header) {
  align-items: center;
  min-height: 32px;
}

.settings-payment-methods-page :deep(.settings-page-block__section-header .mobile-section-title) {
  margin: 0;
  color: #333333;
  font-size: 22px;
  font-weight: var(--ios-pms-weight-medium);
  line-height: 1.25;
  letter-spacing: 0;
}

.settings-payment-methods-page :deep(.settings-page-block__section-header ion-spinner) {
  width: 18px;
  height: 18px;
  color: rgba(var(--ion-color-primary-rgb), 0.78);
}

.settings-payment-methods-page :deep(.settings-minimal-list) {
  gap: 14px;
  margin-top: 18px;
}

.settings-payment-methods-page :deep(.payment-method-card) {
  padding: 16px 14px 18px;
  border: 1px solid rgba(130, 143, 165, 0.2);
  border-radius: var(--ios-pms-radius-input);
  background: rgba(255, 255, 255, 0.86);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.9),
    0 6px 16px rgba(77, 98, 145, 0.035);
}

.settings-payment-methods-page :deep(.payment-method-card::before) {
  display: none;
}

.settings-payment-methods-page :deep(.payment-method-card .settings-minimal-card__header) {
  flex-wrap: nowrap;
  align-items: flex-start;
  gap: 12px;
}

.settings-payment-methods-page :deep(.payment-method-card .settings-minimal-card__title-group) {
  flex: 1;
  gap: 8px;
}

.settings-payment-methods-page :deep(.payment-method-card .settings-minimal-card__title-group strong) {
  color: #333333;
  font-size: 19px;
  font-weight: 500;
  line-height: 1.3;
  letter-spacing: 0;
}

.settings-payment-methods-page :deep(.payment-method-card .settings-minimal-card__summary) {
  color: #999999;
  font-size: 13px;
  font-weight: 400;
  line-height: 1.45;
  letter-spacing: 0;
}

.settings-payment-methods-page :deep(.payment-method-card .settings-minimal-card__badge) {
  min-height: 28px;
  padding: 2px 10px;
  border: 0;
  border-radius: var(--ios-pms-radius-pill);
  font-size: 13px;
  font-weight: 400;
  line-height: 1.2;
  letter-spacing: 0;
}

.settings-payment-methods-page :deep(.payment-method-card .settings-minimal-card__badge--success) {
  background: rgba(var(--ion-color-success-rgb), 0.74);
  color: #ffffff;
}

.settings-payment-methods-page :deep(.payment-method-card .settings-minimal-card__badge--warning) {
  background: rgba(var(--ion-color-warning-rgb), 0.13);
  color: var(--ion-color-warning);
}

.settings-payment-methods-page :deep(.payment-method-card .settings-minimal-card__actions) {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 7px;
  margin-top: 14px;
  padding-top: 12px;
  border-top-color: rgba(130, 143, 165, 0.2);
}

.settings-payment-methods-page :deep(.payment-method-card .settings-minimal-card__actions ion-button) {
  flex: 0 0 auto;
  width: auto;
  min-width: 0;
  height: 26px;
  min-height: 26px;
  margin: 0;
  --padding-start: 11px;
  --padding-end: 11px;
  --padding-top: 0;
  --padding-bottom: 0;
  --border-color: #d9d9d9;
  --border-style: solid;
  --border-width: 1px;
  --border-radius: 10px;
  --box-shadow: none;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0;
}

.settings-payment-methods-page
  :deep(.payment-method-card .settings-minimal-card__actions ion-button::part(native)) {
  min-height: 26px;
  padding-top: 2px;
  padding-bottom: 2px;
}

.settings-payment-methods-page
  :deep(.payment-method-card .settings-minimal-card__actions ion-button[fill='solid']) {
  --background: var(--ios-pms-primary);
  --background-activated: var(--ion-color-primary-shade);
  --border-color: #d9d9d9;
  --color: #ffffff;
}

.settings-payment-methods-page
  :deep(.payment-method-card .settings-minimal-card__actions ion-button[fill='outline']) {
  --background: rgba(255, 255, 255, 0.72);
  --border-color: #d9d9d9;
  --border-width: 1px;
  --color: #2346ff;
}

.settings-payment-methods-page
  :deep(.payment-method-card .settings-minimal-card__actions .payment-method-card__delete-button) {
  color: #ff0000;
  --background: rgba(255, 255, 255, 0.72);
  --background-activated: rgba(255, 0, 0, 0.05);
  --background-activated-opacity: 1;
  --background-focused: rgba(255, 0, 0, 0.04);
  --background-hover: rgba(255, 0, 0, 0.03);
  --border-color: #d9d9d9;
  --color: #ff0000;
  --color-activated: #ff0000;
  --color-focused: #ff0000;
  --color-hover: #ff0000;
}

.settings-payment-methods-page
  :deep(
    .payment-method-card
      .settings-minimal-card__actions
      .payment-method-card__delete-button::part(native)
  ) {
  border-color: #d9d9d9;
  color: #ff0000;
}

.settings-payment-methods-page
  :deep(.payment-method-card .settings-minimal-card__actions ion-button.button-disabled) {
  opacity: 0.42;
}

.settings-payment-methods-page :deep(.settings-section-card__footer) {
  margin-top: 20px;
}

.settings-payment-methods-page :deep(.settings-form-actions--section) {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 36px;
  margin-top: 0;
  padding-top: 14px;
  border-top: 0;
}

.settings-payment-methods-page :deep(.settings-form-actions--section ion-button) {
  width: 100%;
  min-height: 34px;
  height: 34px;
  margin: 0;
  --padding-top: 0;
  --padding-bottom: 0;
  --border-radius: 6px;
  --box-shadow: none;
  font-size: 14px;
  font-weight: 400;
  letter-spacing: 0;
}

.settings-payment-methods-page :deep(.settings-form-actions--section ion-button:first-child) {
  --background: #ffffff;
  --border-color: #d9d9d9;
  --border-width: 1px;
  --color: #999999;
}

.settings-payment-methods-page :deep(.settings-form-actions--section ion-button:last-child) {
  --background: #1890ff;
  --background-activated: #1683e6;
  --color: #ffffff;
}

.settings-payment-methods-page :deep(.payment-methods-empty) {
  margin: 18px 0 0;
  padding: 24px 12px;
  border: 1px dashed rgba(130, 143, 165, 0.24);
  border-radius: var(--ios-pms-radius-input);
  color: var(--ios-pms-text-muted);
  text-align: center;
}

@media (max-width: 374px) {
  .settings-payment-methods-page :deep(.settings-crud-page) {
    --padding-start: 12px;
    --padding-end: 12px;
  }

  .settings-payment-methods-page :deep(.settings-page-shell__stack > .mobile-card) {
    padding-right: 14px;
    padding-left: 14px;
  }

  .settings-payment-methods-page :deep(.settings-page-block__section-header .mobile-section-title) {
    font-size: 20px;
  }

  .settings-payment-methods-page :deep(.payment-method-card) {
    padding-right: 12px;
    padding-left: 12px;
  }

  .settings-payment-methods-page :deep(.payment-method-card .settings-minimal-card__title-group strong) {
    font-size: 17px;
  }

}
</style>
