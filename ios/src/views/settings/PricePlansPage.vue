<template>
  <SettingsCrudPage
    :back-href="ROUTE_PATHS.settings"
    :title="$t('accommodation.roomPriceBulk.table.pricePlan')"
    :hero-eyebrow="$t('stage5UiAttributes.23')"
    :hero-title="$t('stage5UiAttributes.22')"
    :chips="[{ label: `${$t('stage5VisibleText.117')} ${plans.length}` }]"
    :toolbar-action-label="$t('settingsStage4.roomGroup.addGroup')"
    :show-refresher="true"
    :refresher-pulling-text="$t('stage5UiAttributes.0')"
    :section-title="$t('stage5UiAttributes.21')"
    :loading="loading"
    :modal-open="editorOpen"
    :modal-title="editingPlanId ? $t('settingsStage4.pricePlan.dialog.editPlan') : $t('settingsStage4.pricePlan.actions.addPlan')"
    @toolbar-action="handleCreatePlan"
    @refresh="handleRefresh"
    @dismiss-editor="handleDismissEditor"
  >
    <div v-if="plans.length > 0" class="mobile-list settings-minimal-list settings-price-plans-list">
      <article v-for="plan in plans" :key="plan.id" class="settings-minimal-card settings-price-plan-card">
        <div class="settings-minimal-card__header">
          <div class="settings-minimal-card__title-group">
            <strong>{{ plan.name }}</strong>
            <p class="settings-minimal-card__summary">
              {{ $t('stage5DynamicUi.122') }} {{ plan.minNights }} {{ $t('stage5DynamicUi.121') }} {{ plan.maxNights || $t('channel.dialogs.bookingSettings.unlimited') }} {{ $t('channel.dialogs.bookingSettings.nightUnit') }}
            </p>
          </div>
          <span class="settings-minimal-card__badge">{{ plan.includeMeal ? $t('settingsStage4.pricePlan.columns.includeMeal') : $t('stage5DynamicUi.2') }}</span>
        </div>

        <div class="settings-minimal-card__meta">
          <span class="settings-minimal-card__meta-pill">{{ $t('stage5SourceText.52') }} {{ plan.roomTypeCount }}</span>
          <span class="settings-minimal-card__meta-pill">
            {{ plan.derivationType === 'derived' ? $t('stage5DynamicUi.55') : $t('stage5DynamicUi.57') }}
          </span>
        </div>

        <div class="settings-minimal-card__actions">
          <ion-button size="small" fill="solid" @click="handleOpenRates(plan)">{{ $t('routes.SettingsPricePlanRates') }}</ion-button>
          <ion-button size="small" fill="outline" @click="handleEditPlan(plan)">{{ $t('accommodation.roomPrice.editTitle') }}</ion-button>
          <ion-button size="small" color="danger" fill="clear" @click="handleDeletePlan(plan)">{{ $t('roomStatus.roomLock.actions.delete') }}</ion-button>
        </div>
      </article>
    </div>

    <div v-else-if="!loading" class="settings-price-plans-empty-state">
      <strong>{{ $t('stage5SourceText.74') }}</strong>
      <p>{{ $t('stage5SourceText.207') }}</p>
      <ion-button @click="handleCreatePlan">{{ $t('stage5SourceText.22') }}</ion-button>
    </div>
    <template #modalContent>
      <div class="settings-form-grid">
        <label class="settings-form-field">
          <span>{{ $t('stage5SourceText.197') }}</span>
          <ion-input v-model="planForm.name" fill="outline" :placeholder="$t('settingsStage4.pricePlan.placeholders.planName')" />
        </label>

        <label class="settings-form-field">
          <span>{{ $t('settingsStage4.roomTypeDetails.fields.englishName') }}</span>
          <ion-input v-model="planForm.nameEn" fill="outline" :placeholder="$t('stage5UiAttributes.91')" />
        </label>

        <label class="settings-form-field">
          <span>{{ $t('stage5SourceText.140') }}</span>
          <ion-input v-model="planForm.minNights" fill="outline" inputmode="numeric" placeholder="1" />
        </label>

        <label class="settings-form-field">
          <span>{{ $t('stage5SourceText.139') }}</span>
          <ion-input v-model="planForm.maxNights" fill="outline" inputmode="numeric" :placeholder="$t('stage5UiAttributes.84')" />
        </label>

        <label class="settings-form-field">
          <span>{{ $t('stage5SourceText.190') }}</span>
          <ion-select v-model="planForm.derivationType" fill="outline" interface="action-sheet">
            <ion-select-option value="independent">independent</ion-select-option>
            <ion-select-option value="derived">derived</ion-select-option>
          </ion-select>
        </label>

        <label v-if="planForm.derivationType === 'derived'" class="settings-form-field">
          <span>{{ $t('stage5SourceText.40') }}</span>
          <ion-select v-model="planForm.basePlanId" fill="outline" interface="modal">
            <ion-select-option v-for="plan in availableBasePlans" :key="plan.id" :value="plan.id">
              {{ plan.name }}
            </ion-select-option>
          </ion-select>
        </label>

        <div class="settings-toggle-field">
          <div>
            <strong>{{ $t('settingsStage4.pricePlan.columns.includeMeal') }}</strong>
          </div>
          <ion-toggle v-model="planForm.includeMeal" />
        </div>

        <label class="settings-form-field settings-form-field--full">
          <span>{{ $t('settingsStage4.consumptionItems.fields.description') }}</span>
          <ion-textarea v-model="planForm.description" :rows="4" fill="outline" :placeholder="$t('stage5UiAttributes.94')" />
        </label>

        <label class="settings-form-field settings-form-field--full">
          <span>{{ $t('settingsStage4.roomTypeDetails.fields.englishDescription') }}</span>
          <ion-textarea v-model="planForm.descriptionEn" :rows="4" fill="outline" :placeholder="$t('stage5UiAttributes.92')" />
        </label>

        <label v-if="planForm.derivationType === 'derived'" class="settings-form-field settings-form-field--full">
          <span>{{ $t('settingsStage4.pricePlan.columns.derivationRules') }}</span>
          <ion-textarea v-model="planForm.derivationRule" :rows="3" fill="outline" :placeholder="$t('stage5UiAttributes.86')" />
        </label>

        <label class="settings-form-field settings-form-field--full">
          <span>{{ $t('settingsStage4.roomTypeManagement.editor.fields.cancellationPolicy') }}</span>
          <ion-textarea v-model="planForm.cancellationPolicy" :rows="4" fill="outline" :placeholder="$t('stage5UiAttributes.66')" />
        </label>

        <label class="settings-form-field settings-form-field--full">
          <span>{{ $t('stage5SourceText.187') }}</span>
          <ion-textarea v-model="planForm.cancellationPolicyEn" :rows="4" fill="outline" :placeholder="$t('stage5UiAttributes.90')" />
        </label>
      </div>
    </template>

    <template #modalActions>
      <ion-button fill="outline" @click="handleDismissEditor">{{ $t('accommodation.common.cancel') }}</ion-button>
      <ion-button :disabled="submitting" @click="handleSavePlan">
        {{ submitting ? $t('iosStage5.cleaning.submitting') : $t('stage5DynamicUi.4') }}
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
  IonSelect,
  IonSelectOption,
  IonTextarea,
  IonToggle,
  onIonViewWillEnter,
} from '@ionic/vue'
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import SettingsCrudPage from '@/components/settings/families/SettingsCrudPage.vue'
import {
  countRoomTypesByPricePlan,
  createPricePlan,
  deletePricePlan,
  forceDeletePricePlan,
  getAllPricePlans,
  updatePricePlan,
  type PricePlanDTO,
} from '@/api/pricePlan'
import { buildSettingsPricePlanRatesPath, ROUTE_PATHS } from '@/router/guards'
import { useUserStore } from '@/stores/user'
import { showSuccessToast, showWarningToast } from '@/utils/notify'
import { isHandledRequestError } from '@/utils/request'

const { t } = useI18n()

interface PricePlanView extends PricePlanDTO {
  id: number
  roomTypeCount: number
}

interface PricePlanFormState {
  name: string
  nameEn: string
  minNights: string
  maxNights: string
  includeMeal: boolean
  derivationType: string
  basePlanId: number | null
  derivationRule: string
  description: string
  descriptionEn: string
  cancellationPolicy: string
  cancellationPolicyEn: string
}

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const submitting = ref(false)
const plans = ref<PricePlanView[]>([])
const editorOpen = ref(false)
const editingPlanId = ref<number | null>(null)
const planForm = ref<PricePlanFormState>(createEmptyPlanForm())

const availableBasePlans = computed(() => {
  return plans.value.filter((item) => item.id !== editingPlanId.value)
})

function createEmptyPlanForm(): PricePlanFormState {
  return {
    name: '',
    nameEn: '',
    minNights: '1',
    maxNights: '',
    includeMeal: false,
    derivationType: 'independent',
    basePlanId: null,
    derivationRule: '',
    description: '',
    descriptionEn: '',
    cancellationPolicy: '',
    cancellationPolicyEn: '',
  }
}

function resolveWarningMessage(error: unknown, fallbackMessage: string) {
  if (error instanceof Error && error.message) {
    return error.message
  }
  return fallbackMessage
}

async function confirmAction(
  header: string,
  message: string,
  destructive = false,
  confirmText = t('settingsResidual.common.confirm'),
) {
  const alert = await alertController.create({
    header,
    message,
    buttons: [
      {
        text: t('accommodation.common.cancel'),
        role: 'cancel',
      },
      {
        text: confirmText,
        role: destructive ? 'destructive' : 'confirm',
      },
    ],
  })

  await alert.present()
  const result = await alert.onDidDismiss()
  if (destructive) {
    return result.role === 'destructive'
  }
  return result.role === 'confirm'
}

async function loadPlans() {
  const userId = userStore.currentUser?.id
  if (!userId) {
    showWarningToast(t('stage5Pattern.setup'))
    return
  }

  loading.value = true
  try {
    const response = await getAllPricePlans(userId)
    if (!response.success || !response.data) {
      throw new Error(response.message || t('settingsStage4.pricePlan.messages.loadPricePlansFailed'))
    }

    const nextPlans: PricePlanView[] = []

    for (const plan of response.data) {
      let roomTypeCount = 0

      if (plan.id) {
        const countResponse = await countRoomTypesByPricePlan(plan.id)
        if (countResponse.success && typeof countResponse.data === 'number') {
          roomTypeCount = countResponse.data
        }
      }

      nextPlans.push({
        ...plan,
        id: Number(plan.id),
        roomTypeCount,
      })
    }

    plans.value = nextPlans
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, t('settingsStage4.pricePlan.messages.loadPricePlansFailed')))
    }
  } finally {
    loading.value = false
  }
}

function handleCreatePlan() {
  editingPlanId.value = null
  planForm.value = createEmptyPlanForm()
  editorOpen.value = true
}

function handleEditPlan(plan: PricePlanView) {
  editingPlanId.value = plan.id
  planForm.value = {
    name: plan.name,
    nameEn: plan.nameEn || '',
    minNights: String(plan.minNights || 1),
    maxNights: plan.maxNights ? String(plan.maxNights) : '',
    includeMeal: plan.includeMeal,
    derivationType: plan.derivationType || 'independent',
    basePlanId: plan.basePlanId ?? null,
    derivationRule: plan.derivationRule || '',
    description: plan.description || '',
    descriptionEn: plan.descriptionEn || '',
    cancellationPolicy: plan.cancellationPolicy || '',
    cancellationPolicyEn: plan.cancellationPolicyEn || '',
  }
  editorOpen.value = true
}

function handleDismissEditor() {
  editorOpen.value = false
  editingPlanId.value = null
  planForm.value = createEmptyPlanForm()
}

async function handleSavePlan() {
  const userId = userStore.currentUser?.id
  if (!userId) {
    showWarningToast(t('stage5Pattern.setup'))
    return
  }

  if (!planForm.value.name.trim()) {
    showWarningToast(t('settingsStage4.pricePlan.placeholders.planName'))
    return
  }

  const minNights = Number(planForm.value.minNights)
  if (!Number.isFinite(minNights) || minNights <= 0) {
    showWarningToast(t('stage5Pattern.enter'))
    return
  }

  let maxNights: number | undefined
  if (planForm.value.maxNights.trim()) {
    maxNights = Number(planForm.value.maxNights)
    if (!Number.isFinite(maxNights) || maxNights <= 0) {
      showWarningToast(t('stage5Pattern.enter'))
      return
    }
  }

  if (planForm.value.derivationType === 'derived' && !planForm.value.basePlanId) {
    showWarningToast(t('stage5Pattern.select'))
    return
  }

  submitting.value = true
  try {
    const payload = {
      name: planForm.value.name.trim(),
      nameEn: planForm.value.nameEn.trim() || undefined,
      minNights,
      maxNights,
      includeMeal: planForm.value.includeMeal,
      derivationType: planForm.value.derivationType,
      basePlanId: planForm.value.derivationType === 'derived' ? planForm.value.basePlanId || undefined : undefined,
      derivationRule: planForm.value.derivationType === 'derived' ? planForm.value.derivationRule.trim() || undefined : undefined,
      description: planForm.value.description.trim(),
      descriptionEn: planForm.value.descriptionEn.trim() || undefined,
      cancellationPolicy: planForm.value.cancellationPolicy.trim(),
      cancellationPolicyEn: planForm.value.cancellationPolicyEn.trim() || undefined,
    }

    if (editingPlanId.value) {
      const response = await updatePricePlan(editingPlanId.value, userId, payload)
      if (!response.success) {
        throw new Error(response.message || t('settingsStage4.pricePlan.messages.updatePlanFailed'))
      }
      showSuccessToast(t('settingsStage4.pricePlan.messages.updatePlanSuccess'))
    } else {
      const response = await createPricePlan(userId, payload)
      if (!response.success) {
        throw new Error(response.message || t('settingsStage4.pricePlan.messages.createPlanFailed'))
      }
      const createdPlanId = response.data?.id ? Number(response.data.id) : 0
      showSuccessToast(
        createdPlanId
          ? t('settingsResidual.pricePlans.createdAndOpening')
          : t('settingsResidual.pricePlans.created'),
      )
      handleDismissEditor()
      await loadPlans()

      if (createdPlanId > 0) {
        await router.push(buildSettingsPricePlanRatesPath(createdPlanId))
        return
      }

      return
    }

    handleDismissEditor()
    await loadPlans()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, t('stage5Pattern.saveFailed')))
    }
  } finally {
    submitting.value = false
  }
}

async function handleDeletePlan(plan: PricePlanView) {
  const userId = userStore.currentUser?.id
  if (!userId) {
    return
  }

  const confirmed = await confirmAction(
    t('settingsResidual.pricePlans.deleteTitle'),
    t('settingsResidual.pricePlans.confirmDelete', { name: plan.name }),
    true,
    t('settingsResidual.common.deleteConfirm'),
  )
  if (!confirmed) {
    return
  }

  try {
    const response = await deletePricePlan(plan.id, userId)
    if (!response.success) {
      throw new Error(response.message || t('settingsStage4.pricePlan.messages.deletePlanFailed'))
    }
    showSuccessToast(t('settingsStage4.pricePlan.messages.deletePlanSuccess'))
    await loadPlans()
  } catch (error) {
    const message = resolveWarningMessage(error, t('settingsStage4.pricePlan.messages.deletePlanFailed'))
    if (message.includes('渠道价格记录') || message.includes('channel_prices')) {
      const forceConfirmed = await confirmAction(
        t('settingsResidual.pricePlans.forceDeleteTitle'),
        t('settingsResidual.pricePlans.forceDeleteMessage'),
        true,
        t('settingsResidual.pricePlans.forceDelete'),
      )

      if (!forceConfirmed) {
        return
      }

      try {
        const forceResponse = await forceDeletePricePlan(plan.id, userId)
        if (!forceResponse.success) {
          throw new Error(forceResponse.message || t('settingsStage4.pricePlan.messages.forceDeleteFailed'))
        }
        showSuccessToast(t('settingsStage4.pricePlan.messages.forceDeleteSuccess'))
        await loadPlans()
      } catch (forceError) {
        if (!isHandledRequestError(forceError)) {
          showWarningToast(resolveWarningMessage(forceError, t('settingsStage4.pricePlan.messages.forceDeleteFailed')))
        }
      }

      return
    }

    if (!isHandledRequestError(error)) {
      showWarningToast(message)
    }
  }
}

async function handleOpenRates(plan: PricePlanView) {
  await router.push({
    name: 'SettingsPricePlanRates',
    params: { pricePlanId: plan.id },
  })
}

async function handleRefresh(event: CustomEvent) {
  await loadPlans()
  event.detail.complete()
}

onIonViewWillEnter(async () => {
  await loadPlans()
})
</script>

<style scoped>
.settings-price-plans-empty-state {
  display: grid;
  gap: 10px;
  padding-top: 16px;
}

.settings-price-plans-empty-state strong,
.settings-price-plans-empty-state p {
  margin: 0;
}

.settings-price-plans-empty-state p {
  color: var(--app-muted);
  font-size: 13px;
  line-height: 1.6;
}
</style>
