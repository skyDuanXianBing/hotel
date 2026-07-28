<template>
  <SettingsPageShell
    :back-href="ROUTE_PATHS.settings"
    :title="$t('settings.entries.consumptionItems.0')"
    :hero-eyebrow="$t('stage5SourceText.5')"
    :hero-title="$t('stage5UiAttributes.54')"
    :toolbar-action-label="$t('settingsStage4.roleManagement.actions.add')"
    :chips="[
      { label: `${$t('settings.entries.consumptionItems.0')} ${items.length}` },
      { label: `${$t('stage5.common.fields.category')} ${categories.length}` },
    ]"
    :show-refresher="true"
    :refresher-pulling-text="$t('stage5UiAttributes.15')"
    page-class="settings-consumption-page"
    content-class="settings-page-block mobile-page--dashboard settings-consumption-page__content"
    hero-class="settings-page-block__hero mobile-dashboard-surface settings-consumption-page__hero"
    eyebrow-class="settings-page-block__eyebrow"
    stack-class="settings-consumption-page__stack"
    @refresh="handleRefresh"
    @toolbar-action="handleOpenCreate"
  >
    <section class="mobile-card mobile-dashboard-surface settings-consumption-page__tabs-card">
      <ion-segment
        class="settings-consumption-page__segment"
        :value="activeSegment"
        @ionChange="handleSegmentChange"
      >
        <ion-segment-button value="items">
          <ion-label>{{ $t('settings.entries.consumptionItems.0') }}</ion-label>
        </ion-segment-button>
        <ion-segment-button value="categories">
          <ion-label>{{ $t('stage5.common.fields.category') }}</ion-label>
        </ion-segment-button>
      </ion-segment>
    </section>

    <SettingsSectionCard
      v-if="activeSegment === 'items'"
      :title="$t('settingsStage4.consumptionItems.tabs.list')"
      :loading="loading"
      card-class="mobile-dashboard-surface settings-consumption-page__list-card"
      header-class="settings-page-block__section-header"
    >
      <template #headerActions>
        <div class="settings-consumption-page__header-actions">
          <ion-spinner v-if="loading" name="crescent" />
          <ion-button class="settings-consumption-page__add-button" size="small" @click="handleOpenCreate">
            {{ $t('settingsStage4.consumptionItems.dialog.addItem') }}
          </ion-button>
        </div>
      </template>

      <div v-if="items.length > 0" class="mobile-list settings-minimal-list">
        <article v-for="item in items" :key="item.id" class="settings-minimal-card settings-consumption-item-card">
          <div class="settings-minimal-card__header">
            <div class="settings-minimal-card__title-group">
              <strong>{{ item.name }}</strong>
              <p class="settings-minimal-card__summary">{{ item.category }}</p>
            </div>
            <span class="settings-minimal-card__badge">{{ formatItemPrice(item.price) }}</span>
          </div>

          <div class="settings-minimal-card__meta">
            <span
              class="settings-minimal-card__meta-pill"
              :class="item.enabled ? 'settings-minimal-card__meta-pill--success' : 'settings-minimal-card__meta-pill--warning'"
            >
              {{ item.enabled ? $t('channel.managementData.statusActive') : $t('stage5DynamicUi.28') }}
            </span>
          </div>

          <div class="settings-minimal-card__actions">
            <ion-button size="small" fill="outline" @click="handleEditItem(item)">{{ $t('accommodation.roomPrice.editTitle') }}</ion-button>
            <ion-button size="small" fill="outline" @click="handleToggleItem(item)">
              {{ item.enabled ? $t('roomStatus.store.roomState.outOfOrder') : $t('settingsStage4.accountList.status.enabled') }}
            </ion-button>
            <ion-button size="small" color="danger" fill="clear" @click="handleDeleteItem(item)">{{ $t('roomStatus.roomLock.actions.delete') }}</ion-button>
          </div>
        </article>
      </div>

      <p v-else-if="!loading" class="mobile-note settings-empty-state">{{ $t('stage5SourceText.82') }}</p>
    </SettingsSectionCard>

    <SettingsSectionCard
      v-else
      :title="$t('stage5UiAttributes.30')"
      :loading="loading"
      card-class="mobile-dashboard-surface settings-consumption-page__list-card"
      header-class="settings-page-block__section-header"
    >
      <template #headerActions>
        <div class="settings-consumption-page__header-actions">
          <ion-spinner v-if="loading" name="crescent" />
          <ion-button class="settings-consumption-page__add-button" size="small" @click="handleOpenCreate">
            {{ $t('settingsStage4.consumptionItems.addCategory') }}
          </ion-button>
        </div>
      </template>

      <div v-if="categories.length > 0" class="mobile-list settings-minimal-list">
        <article
          v-for="category in categories"
          :key="category.id"
          class="settings-minimal-card settings-consumption-category-card"
        >
          <div class="settings-minimal-card__header">
            <div class="settings-minimal-card__title-group">
              <strong>{{ category.name }}</strong>
              <p class="settings-minimal-card__summary settings-minimal-card__summary--clamp-two">
                {{ category.description || $t('stage5DynamicUi.47') }}
              </p>
            </div>
            <span class="settings-minimal-card__badge">{{ $t('settings.entries.consumptionItems.0') }} {{ category.count || 0 }}</span>
          </div>

          <div class="settings-minimal-card__actions">
            <ion-button size="small" fill="outline" @click="handleEditCategory(category)">{{ $t('accommodation.roomPrice.editTitle') }}</ion-button>
            <ion-button size="small" color="danger" fill="clear" @click="handleDeleteCategory(category)">{{ $t('roomStatus.roomLock.actions.delete') }}</ion-button>
          </div>
        </article>
      </div>

      <p v-else-if="!loading" class="mobile-note settings-empty-state">{{ $t('stage5SourceText.83') }}</p>
    </SettingsSectionCard>

    <SettingsEditorModal
      :is-open="itemEditorOpen"
      :title="editingItemId ? $t('settingsStage4.consumptionItems.dialog.editItem') : $t('settingsStage4.consumptionItems.dialog.addItem')"
      @close="handleDismissItemEditor"
      @didDismiss="handleDismissItemEditor"
    >
      <div class="settings-form-grid">
        <label class="settings-form-field">
          <span>{{ $t('stage5.common.fields.category') }}</span>
          <ion-select v-model="itemForm.category" fill="outline" interface="modal">
            <ion-select-option v-for="category in categories" :key="category.id" :value="category.name">
              {{ category.name }}
            </ion-select-option>
          </ion-select>
        </label>

        <label class="settings-form-field">
          <span>{{ $t('roomStatus.roomLock.fields.name') }}</span>
          <ion-input v-model="itemForm.name" fill="outline" :placeholder="$t('settingsStage4.consumptionItems.placeholders.itemName')" />
        </label>

        <label class="settings-form-field">
          <span>{{ $t('accommodation.roomPrice.settingType.price') }}</span>
          <ion-input v-model="itemForm.price" fill="outline" inputmode="decimal" placeholder="0.00" />
        </label>

        <div class="settings-toggle-field">
          <div>
            <strong>{{ $t('stage5SourceText.35') }}</strong>
          </div>
          <ion-toggle v-model="itemForm.enabled" />
        </div>

        <label class="settings-form-field settings-form-field--full">
          <span>{{ $t('settingsStage4.consumptionItems.fields.description') }}</span>
          <ion-textarea v-model="itemForm.description" :rows="4" fill="outline" :placeholder="$t('stage5UiAttributes.80')" />
        </label>
      </div>

      <template #actions>
        <ion-button fill="outline" @click="handleDismissItemEditor">{{ $t('accommodation.common.cancel') }}</ion-button>
        <ion-button :disabled="submitting" @click="handleSaveItem">
          {{ submitting ? $t('iosStage5.cleaning.submitting') : $t('stage5DynamicUi.16') }}
        </ion-button>
      </template>
    </SettingsEditorModal>

    <SettingsEditorModal
      :is-open="categoryEditorOpen"
      :title="editingCategoryId ? $t('settingsStage4.consumptionItems.dialog.editCategory') : $t('settingsStage4.consumptionItems.addCategory')"
      @close="handleDismissCategoryEditor"
      @didDismiss="handleDismissCategoryEditor"
    >
      <div class="settings-form-grid">
        <label class="settings-form-field">
          <span>{{ $t('settingsStage4.consumptionItems.fields.categoryName') }}</span>
          <ion-input v-model="categoryForm.name" fill="outline" :placeholder="$t('settingsStage4.consumptionItems.placeholders.categoryName')" />
        </label>

        <label class="settings-form-field settings-form-field--full">
          <span>{{ $t('stage5SourceText.18') }}</span>
          <ion-textarea v-model="categoryForm.description" :rows="4" fill="outline" :placeholder="$t('stage5UiAttributes.64')" />
        </label>
      </div>

      <template #actions>
        <ion-button fill="outline" @click="handleDismissCategoryEditor">{{ $t('accommodation.common.cancel') }}</ion-button>
        <ion-button :disabled="submitting" @click="handleSaveCategory">
          {{ submitting ? $t('iosStage5.cleaning.submitting') : $t('stage5DynamicUi.7') }}
        </ion-button>
      </template>
    </SettingsEditorModal>
  </SettingsPageShell>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import {
  alertController,
  IonButton,
  IonInput,
  IonLabel,
  IonSegment,
  IonSegmentButton,
  IonSelect,
  IonSelectOption,
  IonSpinner,
  IonTextarea,
  IonToggle,
  onIonViewWillEnter,
} from '@ionic/vue'
import { computed, ref } from 'vue'
import {
  createConsumptionCategory,
  createConsumptionItem,
  deleteConsumptionCategory,
  deleteConsumptionItem,
  getAllConsumptionCategories,
  getAllConsumptionItems,
  updateConsumptionCategory,
  updateConsumptionItem,
  updateConsumptionItemEnabled,
  type ConsumptionCategoryDTO,
  type ConsumptionItemDTO,
} from '@/api/consumptionItem'
import SettingsEditorModal from '@/components/settings/base/SettingsEditorModal.vue'
import SettingsPageShell from '@/components/settings/base/SettingsPageShell.vue'
import SettingsSectionCard from '@/components/settings/base/SettingsSectionCard.vue'
import { ROUTE_PATHS } from '@/router/guards'
import { useStoreStore } from '@/stores/store'
import { formatMoney } from '@/utils/formatters'
import { showSuccessToast, showWarningToast } from '@/utils/notify'
import { isHandledRequestError } from '@/utils/request'

const { t } = useI18n()
const storeStore = useStoreStore()
const currentCurrency = computed(() => storeStore.currentStore?.currency || 'CNY')
const currentMoneyContext = computed(() => ({ country: storeStore.currentStore?.country }))

const formatItemPrice = (value: number) =>
  formatMoney(
    value,
    currentCurrency.value,
    { minimumFractionDigits: 2, maximumFractionDigits: 2 },
    currentMoneyContext.value,
  )

type ConsumptionSegment = 'items' | 'categories'

interface ItemFormState {
  category: string
  name: string
  price: string
  enabled: boolean
  description: string
}

interface CategoryFormState {
  name: string
  description: string
}

const activeSegment = ref<ConsumptionSegment>('items')
const loading = ref(false)
const submitting = ref(false)
const items = ref<ConsumptionItemDTO[]>([])
const categories = ref<ConsumptionCategoryDTO[]>([])
const itemEditorOpen = ref(false)
const categoryEditorOpen = ref(false)
const editingItemId = ref<number | null>(null)
const editingCategoryId = ref<number | null>(null)
const itemForm = ref<ItemFormState>(createEmptyItemForm())
const categoryForm = ref<CategoryFormState>(createEmptyCategoryForm())

function createEmptyItemForm(): ItemFormState {
  return {
    category: '',
    name: '',
    price: '0',
    enabled: true,
    description: '',
  }
}

function createEmptyCategoryForm(): CategoryFormState {
  return {
    name: '',
    description: '',
  }
}

function resolveWarningMessage(error: unknown, fallbackMessage: string) {
  if (error instanceof Error && error.message) {
    return error.message
  }
  return fallbackMessage
}

async function confirmDelete(header: string, message: string) {
  const alert = await alertController.create({
    header,
    message,
    buttons: [
      {
        text: t('accommodation.common.cancel'),
        role: 'cancel',
      },
      {
        text: t('settingsStage4.roomSettings.messages.deleteTitle'),
        role: 'destructive',
      },
    ],
  })

  await alert.present()
  const result = await alert.onDidDismiss()
  return result.role === 'destructive'
}

async function loadPageData() {
  loading.value = true
  try {
    const [itemResponse, categoryResponse] = await Promise.all([
      getAllConsumptionItems(),
      getAllConsumptionCategories(),
    ])

    if (!itemResponse.success || !itemResponse.data) {
      throw new Error(itemResponse.message || t('settingsStage4.consumptionItems.messages.loadItemsFailed'))
    }
    if (!categoryResponse.success || !categoryResponse.data) {
      throw new Error(categoryResponse.message || t('settingsStage4.consumptionItems.messages.loadCategoriesFailed'))
    }

    items.value = itemResponse.data
    categories.value = categoryResponse.data
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, t('settingsStage4.consumptionItems.messages.loadItemsFailed')))
    }
  } finally {
    loading.value = false
  }
}

function handleSegmentChange(event: CustomEvent) {
  const nextValue = event.detail.value as ConsumptionSegment
  if (nextValue === 'categories') {
    activeSegment.value = 'categories'
    return
  }
  activeSegment.value = 'items'
}

function handleOpenCreate() {
  if (activeSegment.value === 'categories') {
    editingCategoryId.value = null
    categoryForm.value = createEmptyCategoryForm()
    categoryEditorOpen.value = true
    return
  }

  editingItemId.value = null
  itemForm.value = createEmptyItemForm()
  itemEditorOpen.value = true
}

function handleEditItem(item: ConsumptionItemDTO) {
  editingItemId.value = Number(item.id)
  itemForm.value = {
    category: item.category,
    name: item.name,
    price: String(item.price),
    enabled: item.enabled,
    description: item.description || '',
  }
  itemEditorOpen.value = true
}

function handleDismissItemEditor() {
  itemEditorOpen.value = false
  editingItemId.value = null
  itemForm.value = createEmptyItemForm()
}

async function handleSaveItem() {
  if (!itemForm.value.category) {
    showWarningToast(t('settingsStage4.consumptionItems.placeholders.selectCategory'))
    return
  }
  if (!itemForm.value.name.trim()) {
    showWarningToast(t('settingsStage4.consumptionItems.placeholders.itemName'))
    return
  }

  const price = Number(itemForm.value.price)
  if (!Number.isFinite(price) || price < 0) {
    showWarningToast(t('accommodation.roomPriceBulk.messages.invalidPrice'))
    return
  }

  submitting.value = true
  try {
    const payload: ConsumptionItemDTO = {
      category: itemForm.value.category,
      name: itemForm.value.name.trim(),
      price,
      enabled: itemForm.value.enabled,
      description: itemForm.value.description.trim(),
    }

    if (editingItemId.value) {
      const response = await updateConsumptionItem(editingItemId.value, payload)
      if (!response.success) {
        throw new Error(response.message || t('stage5Pattern.updateFailed'))
      }
      showSuccessToast(t('stage5Pattern.updateCompleted'))
    } else {
      const response = await createConsumptionItem(payload)
      if (!response.success) {
        throw new Error(response.message || t('stage5Pattern.createFailed'))
      }
      showSuccessToast(t('stage5Pattern.createCompleted'))
    }

    handleDismissItemEditor()
    await loadPageData()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, t('stage5Pattern.saveFailed')))
    }
  } finally {
    submitting.value = false
  }
}

async function handleToggleItem(item: ConsumptionItemDTO) {
  if (!item.id) {
    return
  }

  const nextEnabled = !item.enabled
  try {
    const response = await updateConsumptionItemEnabled(item.id, nextEnabled)
    if (!response.success) {
      throw new Error(response.message || t('stage5Pattern.updateFailed'))
    }
    showSuccessToast(
      nextEnabled
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

async function handleDeleteItem(item: ConsumptionItemDTO) {
  if (!item.id) {
    return
  }

  const confirmed = await confirmDelete(
    t('settingsResidual.common.confirm'),
    t('settingsResidual.common.confirmDelete', { name: item.name }),
  )
  if (!confirmed) {
    return
  }

  try {
    const response = await deleteConsumptionItem(item.id)
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

function handleEditCategory(category: ConsumptionCategoryDTO) {
  editingCategoryId.value = Number(category.id)
  categoryForm.value = {
    name: category.name,
    description: category.description || '',
  }
  categoryEditorOpen.value = true
}

function handleDismissCategoryEditor() {
  categoryEditorOpen.value = false
  editingCategoryId.value = null
  categoryForm.value = createEmptyCategoryForm()
}

async function handleSaveCategory() {
  if (!categoryForm.value.name.trim()) {
    showWarningToast(t('settingsStage4.consumptionItems.placeholders.categoryName'))
    return
  }

  submitting.value = true
  try {
    const payload: ConsumptionCategoryDTO = {
      name: categoryForm.value.name.trim(),
      description: categoryForm.value.description.trim(),
    }

    if (editingCategoryId.value) {
      const response = await updateConsumptionCategory(editingCategoryId.value, payload)
      if (!response.success) {
        throw new Error(response.message || t('stage5Pattern.updateFailed'))
      }
      showSuccessToast(t('stage5Pattern.updateCompleted'))
    } else {
      const response = await createConsumptionCategory(payload)
      if (!response.success) {
        throw new Error(response.message || t('stage5Pattern.createFailed'))
      }
      showSuccessToast(t('stage5Pattern.createCompleted'))
    }

    handleDismissCategoryEditor()
    await loadPageData()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, t('stage5Pattern.saveFailed')))
    }
  } finally {
    submitting.value = false
  }
}

async function handleDeleteCategory(category: ConsumptionCategoryDTO) {
  if (!category.id) {
    return
  }

  const confirmed = await confirmDelete(
    t('settingsResidual.common.confirm'),
    t('settingsResidual.common.confirmDelete', { name: category.name }),
  )
  if (!confirmed) {
    return
  }

  try {
    const response = await deleteConsumptionCategory(category.id)
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
.settings-consumption-page :deep(.settings-consumption-page__content) {
  display: block;
  --background: var(--ios-pms-dashboard-page-background);
  --padding-top: 12px;
  --padding-bottom: calc(32px + var(--app-safe-bottom));
  --padding-start: 16px;
  --padding-end: 16px;
}

.settings-consumption-page :deep(.app-page-header__text-btn) {
  --color: #333333;
  font-size: 16px;
  font-weight: 400;
  letter-spacing: 0;
}

.settings-consumption-page :deep(.settings-consumption-page__hero) {
  margin: 0;
  padding: 18px 16px 24px;
  border-radius: var(--ios-pms-radius-card);
}

.settings-consumption-page :deep(.settings-consumption-page__hero::before) {
  display: none;
}

.settings-consumption-page :deep(.settings-page-block__eyebrow) {
  display: none;
}

.settings-consumption-page :deep(.settings-consumption-page__hero .mobile-title) {
  margin: 0;
  color: var(--ios-pms-text-primary);
  font-size: 22px;
  font-weight: var(--ios-pms-weight-medium);
  line-height: 1.25;
  letter-spacing: 0;
}

.settings-consumption-page :deep(.settings-consumption-page__hero .mobile-chip-row) {
  gap: 8px;
  margin-top: 12px;
}

.settings-consumption-page :deep(.settings-consumption-page__hero .mobile-chip) {
  min-height: 26px;
  padding: 2px 10px;
  border-color: rgba(var(--ion-color-primary-rgb), 0.1);
  background: rgba(var(--ion-color-primary-rgb), 0.07);
  color: rgba(var(--ion-color-primary-rgb), 0.82);
  font-size: 13px;
  font-weight: 400;
  line-height: 1.2;
  letter-spacing: 0;
}

.settings-consumption-page :deep(.settings-consumption-page__stack) {
  gap: 18px;
  margin-top: 16px;
  padding-bottom: 6px;
}

.settings-consumption-page__tabs-card {
  padding: 2px;
  overflow: hidden;
  border-radius: var(--ios-pms-radius-pill);
}

.settings-consumption-page__segment {
  width: 100%;
  height: 34px;
  min-height: 34px;
  padding: 0;
  overflow: hidden;
  border: 0;
  border-radius: var(--ios-pms-radius-pill);
  background: transparent;
}

.settings-consumption-page__segment ion-segment-button {
  --border-radius: var(--ios-pms-radius-pill);
  --color: #242529;
  --color-checked: #ffffff;
  --indicator-color: #343436;
  --indicator-box-shadow: none;
  --padding-start: 6px;
  --padding-end: 6px;
  min-width: 0;
  min-height: 34px;
  height: 34px;
  margin: 0;
  font-size: 15px;
  font-weight: 500;
  letter-spacing: 0;
}

.settings-consumption-page__segment ion-segment-button::part(native) {
  min-height: 34px;
  padding: 0 2px;
  border-radius: var(--ios-pms-radius-pill);
}

.settings-consumption-page__segment ion-segment-button::part(indicator) {
  padding: 0;
}

.settings-consumption-page__segment ion-segment-button::part(indicator-background) {
  border-radius: var(--ios-pms-radius-pill);
  background: #343436;
  box-shadow: none;
}

.settings-consumption-page__segment ion-label {
  margin: 0;
  line-height: 1.2;
}

.settings-consumption-page :deep(.settings-consumption-page__list-card) {
  padding: 18px 16px 30px;
  border-radius: var(--ios-pms-radius-card);
}

.settings-consumption-page :deep(.settings-page-block__section-header) {
  align-items: center;
  gap: 10px;
}

.settings-consumption-page :deep(.settings-page-block__section-header > div:first-child) {
  min-width: 0;
}

.settings-consumption-page :deep(.settings-page-block__section-header .mobile-section-title) {
  margin: 0;
  color: var(--ios-pms-text-primary);
  font-size: 22px;
  font-weight: var(--ios-pms-weight-medium);
  line-height: 1.25;
  letter-spacing: 0;
  overflow-wrap: anywhere;
}

.settings-consumption-page__header-actions {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  gap: 6px;
}

.settings-consumption-page__header-actions ion-spinner {
  width: 18px;
  height: 18px;
  color: rgba(var(--ion-color-primary-rgb), 0.78);
}

.settings-consumption-page__add-button {
  height: 28px;
  min-height: 28px;
  margin: 0;
  --background: #266eff;
  --background-activated: #1f5fe2;
  --border-radius: var(--ios-pms-radius-pill);
  --box-shadow: none;
  --padding-start: 12px;
  --padding-end: 12px;
  --padding-top: 0;
  --padding-bottom: 0;
  font-size: 14px;
  font-weight: 400;
  letter-spacing: 0;
}

.settings-consumption-page :deep(.settings-minimal-list) {
  gap: 12px;
  margin-top: 18px;
}

.settings-consumption-page :deep(.settings-consumption-item-card),
.settings-consumption-page :deep(.settings-consumption-category-card) {
  padding: 14px;
  border: 1px solid rgba(130, 143, 165, 0.2);
  border-radius: var(--ios-pms-radius-input);
  background: rgba(255, 255, 255, 0.86);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.9),
    0 6px 16px rgba(77, 98, 145, 0.035);
}

.settings-consumption-page :deep(.settings-consumption-item-card::before),
.settings-consumption-page :deep(.settings-consumption-category-card::before) {
  display: none;
}

.settings-consumption-page :deep(.settings-minimal-card__title-group) {
  gap: 5px;
}

.settings-consumption-page :deep(.settings-minimal-card__title-group strong) {
  color: #333333;
  font-size: 17px;
  font-weight: 500;
  line-height: 1.3;
  letter-spacing: 0;
}

.settings-consumption-page :deep(.settings-minimal-card__summary) {
  color: var(--ios-pms-text-muted);
  font-size: 13px;
  font-weight: 400;
  line-height: 1.5;
}

.settings-consumption-page :deep(.settings-minimal-card__badge),
.settings-consumption-page :deep(.settings-minimal-card__meta-pill) {
  min-height: 26px;
  padding: 2px 9px;
  border-color: rgba(var(--ion-color-primary-rgb), 0.1);
  background: rgba(var(--ion-color-primary-rgb), 0.07);
  color: rgba(var(--ion-color-primary-rgb), 0.82);
  font-size: 13px;
  font-weight: 400;
  line-height: 1.2;
  letter-spacing: 0;
}

.settings-consumption-page :deep(.settings-minimal-card__meta) {
  margin-top: 12px;
}

.settings-consumption-page :deep(.settings-minimal-card__actions) {
  gap: 6px;
  margin-top: 14px;
  padding-top: 12px;
  border-top-color: rgba(130, 143, 165, 0.12);
}

.settings-consumption-page :deep(.settings-minimal-card__actions ion-button) {
  min-height: 32px;
  --padding-start: 12px;
  --padding-end: 12px;
  font-size: 12px;
  font-weight: 400;
  letter-spacing: 0;
}

.settings-consumption-page :deep(.settings-empty-state) {
  margin: 18px 0 0;
  color: #ff0000;
  font-size: 15px;
  font-weight: 400;
  line-height: 1.55;
}

@media (max-width: 374px) {
  .settings-consumption-page :deep(.settings-consumption-page__content) {
    --padding-start: 12px;
    --padding-end: 12px;
  }

  .settings-consumption-page :deep(.settings-consumption-page__hero) {
    padding: 16px 14px 22px;
  }

  .settings-consumption-page :deep(.settings-consumption-page__list-card) {
    padding-right: 14px;
    padding-left: 14px;
  }

  .settings-consumption-page :deep(.settings-page-block__section-header .mobile-section-title) {
    font-size: 20px;
  }

  .settings-consumption-page__add-button {
    --padding-start: 10px;
    --padding-end: 10px;
    font-size: 13px;
  }
}
</style>
