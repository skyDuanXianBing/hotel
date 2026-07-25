<template>
  <SettingsPageShell
    :back-href="ROUTE_PATHS.settings"
    :title="$t('settings.entries.consumptionItems.0')"
    :hero-eyebrow="$t('stage5SourceText.5')"
    :hero-title="$t('stage5UiAttributes.54')"
    :chips="[
      { label: `${$t('settings.entries.consumptionItems.0')} ${items.length}` },
      { label: `${$t('stage5.common.fields.category')} ${categories.length}` },
    ]"
    :show-refresher="true"
    :refresher-pulling-text="$t('stage5UiAttributes.15')"
    content-class="settings-page-block"
    hero-class="settings-page-block__hero"
    eyebrow-class="settings-page-block__eyebrow"
    @refresh="handleRefresh"
  >
    <section class="mobile-card">
      <div class="mobile-inline-row settings-consumption-page__toolbar">
        <ion-segment :value="activeSegment" @ionChange="handleSegmentChange">
          <ion-segment-button value="items">
            <ion-label>{{ $t('settings.entries.consumptionItems.0') }}</ion-label>
          </ion-segment-button>
          <ion-segment-button value="categories">
            <ion-label>{{ $t('stage5.common.fields.category') }}</ion-label>
          </ion-segment-button>
        </ion-segment>

        <ion-button size="small" @click="handleOpenCreate">
          {{ activeSegment === 'items' ? $t('settingsStage4.consumptionItems.dialog.addItem') : $t('settingsStage4.consumptionItems.addCategory') }}
        </ion-button>
      </div>
    </section>

    <SettingsSectionCard
      v-if="activeSegment === 'items'"
      :title="$t('settingsStage4.consumptionItems.tabs.list')"
      :loading="loading"
      header-class="settings-page-block__section-header"
    >
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
      header-class="settings-page-block__section-header"
    >
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
.settings-consumption-page__toolbar {
  align-items: flex-start;
}
</style>
