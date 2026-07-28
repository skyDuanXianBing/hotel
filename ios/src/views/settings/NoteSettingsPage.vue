<template>
  <SettingsSortablePage
    class="settings-note-page"
    :back-href="ROUTE_PATHS.settings"
    :title="$t('settings.entries.noteSettings.0')"
    :hero-eyebrow="$t('settings.groups.finance')"
    :hero-title="$t('stage5UiAttributes.60')"
    :toolbar-action-label="$t('settingsStage4.roomGroup.addGroup')"
    :show-refresher="true"
    :refresher-pulling-text="$t('stage5UiAttributes.17')"
    :section-title="$t('stage5UiAttributes.30')"
    :loading="loading"
    :modal-open="editorOpen"
    :modal-title="editingCategoryId ? $t('settingsStage4.consumptionItems.dialog.editCategory') : $t('settingsStage4.consumptionItems.addCategory')"
    @toolbar-action="handleCreateCategory"
    @refresh="handleRefresh"
    @dismiss-editor="handleDismissEditor"
  >
    <template #controls>
      <ion-segment :value="activeType" @ionChange="handleTypeChange">
        <ion-segment-button value="income">
          <ion-label>{{ $t('stage5.statistics.notes.income') }}</ion-label>
        </ion-segment-button>
        <ion-segment-button value="expense">
          <ion-label>{{ $t('stage5.statistics.notes.expense') }}</ion-label>
        </ion-segment-button>
      </ion-segment>
    </template>

    <div v-if="currentCategories.length > 0" class="mobile-list settings-minimal-list">
      <article
        v-for="(category, index) in currentCategories"
        :key="category.id"
        class="settings-minimal-card note-category-card"
      >
        <div class="settings-minimal-card__header">
          <div class="settings-minimal-card__title-group">
            <strong>{{ category.name }}</strong>
            <p class="settings-minimal-card__summary">{{ $t('stage5DynamicUi.142') }} {{ index + 1 }}</p>
          </div>
          <span
            class="settings-minimal-card__badge"
            :class="category.type === 'income' ? 'settings-minimal-card__badge--success' : 'settings-minimal-card__badge--warning'"
          >
            {{ category.type === 'income' ? $t('stage5.statistics.notes.income') : $t('stage5.statistics.notes.expense') }}
          </span>
        </div>

        <div class="settings-minimal-card__actions settings-minimal-card__actions--dense">
          <ion-button size="small" fill="outline" @click="handleEditCategory(category)">{{ $t('accommodation.roomPrice.editTitle') }}</ion-button>
          <ion-button size="small" fill="outline" :disabled="index === 0" @click="handleMove(index, -1)">{{ $t('stage5SourceText.3') }}</ion-button>
          <ion-button
            size="small"
            fill="outline"
            :disabled="index === currentCategories.length - 1"
            @click="handleMove(index, 1)"
          >
            {{ $t('stage5SourceText.4') }}
          </ion-button>
          <ion-button size="small" color="danger" fill="clear" @click="handleDeleteCategory(category)">{{ $t('roomStatus.roomLock.actions.delete') }}</ion-button>
        </div>
      </article>
    </div>

    <p v-else-if="!loading" class="mobile-note note-settings-empty">{{ $t('stage5SourceText.56') }}</p>

    <template #sectionFooter>
      <div class="settings-form-actions settings-form-actions--section">
        <ion-button fill="outline" :disabled="loading || savingOrder" @click="loadPageData">{{ $t('accommodation.common.reset') }}</ion-button>
        <ion-button :disabled="loading || savingOrder || currentCategories.length === 0" @click="handleSaveOrder">
          {{ savingOrder ? $t('channel.mobile.common.saving') : $t('stage5DynamicUi.24') }}
        </ion-button>
      </div>
    </template>

    <template #modalContent>
      <div class="settings-form-grid">
        <label class="settings-form-field">
          <span>{{ $t('settingsStage4.consumptionItems.fields.categoryName') }}</span>
          <ion-input v-model="categoryForm.name" fill="outline" :placeholder="$t('settingsStage4.consumptionItems.placeholders.categoryName')" />
        </label>

        <label class="settings-form-field">
          <span>{{ $t('stage5SourceText.19') }}</span>
          <ion-select v-model="categoryForm.type" fill="outline" interface="action-sheet">
            <ion-select-option value="income">{{ $t('stage5.statistics.notes.income') }}</ion-select-option>
            <ion-select-option value="expense">{{ $t('stage5.statistics.notes.expense') }}</ion-select-option>
          </ion-select>
        </label>
      </div>
    </template>

    <template #modalActions>
      <ion-button fill="outline" @click="handleDismissEditor">{{ $t('accommodation.common.cancel') }}</ion-button>
      <ion-button :disabled="submitting" @click="handleSaveCategory">
        {{ submitting ? $t('iosStage5.cleaning.submitting') : $t('stage5DynamicUi.7') }}
      </ion-button>
    </template>
  </SettingsSortablePage>
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
  onIonViewWillEnter,
} from '@ionic/vue'
import { computed, ref } from 'vue'
import { createCategory, deleteCategory, getAllCategories, updateCategoriesOrder, updateCategory } from '@/api/noteCategory'
import SettingsSortablePage from '@/components/settings/families/SettingsSortablePage.vue'
import { ROUTE_PATHS } from '@/router/guards'
import type { NoteCategoryDTO, NoteCategoryType } from '@/types/settings'
import { showSuccessToast, showWarningToast } from '@/utils/notify'
import { isHandledRequestError } from '@/utils/request'
import { moveArrayItem } from '@/utils/settings'

const { t } = useI18n()

interface CategoryFormState {
  name: string
  type: NoteCategoryType
}

const activeType = ref<NoteCategoryType>('income')
const loading = ref(false)
const submitting = ref(false)
const savingOrder = ref(false)
const editorOpen = ref(false)
const editingCategoryId = ref<number | null>(null)
const categories = ref<NoteCategoryDTO[]>([])
const categoryForm = ref<CategoryFormState>(createEmptyForm())

const currentCategories = computed(() => {
  return categories.value.filter((item) => item.type === activeType.value)
})

function createEmptyForm(): CategoryFormState {
  return {
    name: '',
    type: 'income',
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
    const response = await getAllCategories()
    if (!response.success || !response.data) {
      throw new Error(response.message || t('settingsStage4.consumptionItems.messages.loadCategoriesFailed'))
    }
    categories.value = [...response.data].sort((left, right) => left.displayOrder - right.displayOrder)
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, t('settingsStage4.consumptionItems.messages.loadCategoriesFailed')))
    }
  } finally {
    loading.value = false
  }
}

function handleTypeChange(event: CustomEvent) {
  activeType.value = event.detail.value as NoteCategoryType
}

function handleCreateCategory() {
  editingCategoryId.value = null
  categoryForm.value = { name: '', type: activeType.value }
  editorOpen.value = true
}

function handleEditCategory(category: NoteCategoryDTO) {
  editingCategoryId.value = category.id
  categoryForm.value = { name: category.name, type: category.type }
  editorOpen.value = true
}

function handleDismissEditor() {
  editorOpen.value = false
  editingCategoryId.value = null
  categoryForm.value = createEmptyForm()
}

async function handleSaveCategory() {
  if (!categoryForm.value.name.trim()) {
    showWarningToast(t('settingsStage4.consumptionItems.placeholders.categoryName'))
    return
  }

  submitting.value = true
  try {
    if (editingCategoryId.value) {
      const response = await updateCategory(editingCategoryId.value, {
        name: categoryForm.value.name.trim(),
        type: categoryForm.value.type,
      })
      if (!response.success) {
        throw new Error(response.message || t('stage5Pattern.updateFailed'))
      }
    } else {
      const response = await createCategory({
        name: categoryForm.value.name.trim(),
        type: categoryForm.value.type,
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

function handleMove(index: number, delta: number) {
  const nextList = moveArrayItem(currentCategories.value, index, index + delta)
  const nextCategories: NoteCategoryDTO[] = []
  let inserted = false

  for (const category of categories.value) {
    if (category.type === activeType.value) {
      if (!inserted) {
        nextCategories.push(...nextList)
        inserted = true
      }
      continue
    }
    nextCategories.push(category)
  }

  if (!inserted) {
    nextCategories.push(...nextList)
  }

  categories.value = nextCategories
}

async function handleSaveOrder() {
  savingOrder.value = true
  try {
    const payload = currentCategories.value.map((item, index) => ({ id: item.id, displayOrder: index + 1 }))
    const response = await updateCategoriesOrder(payload)
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

async function handleDeleteCategory(category: NoteCategoryDTO) {
  const confirmed = await confirmDelete(category.name)
  if (!confirmed) {
    return
  }

  try {
    const response = await deleteCategory(category.id)
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
.settings-note-page :deep(.settings-sortable-page) {
  display: block;
  --background: var(--ios-pms-dashboard-page-background);
  --padding-top: 12px;
  --padding-bottom: calc(30px + var(--app-safe-bottom));
  --padding-start: 16px;
  --padding-end: 16px;
}

.settings-note-page :deep(.app-page-header__title) {
  color: #333333;
  font-size: 20px;
  font-weight: 500;
  letter-spacing: 0;
}

.settings-note-page :deep(.app-page-header__text-btn) {
  --color: #333333;
  font-size: 16px;
  font-weight: 400;
  letter-spacing: 0;
}

.settings-note-page :deep(.settings-page-block__hero) {
  display: none;
}

.settings-note-page :deep(.settings-page-shell__stack) {
  gap: 14px;
  margin-top: 0;
  padding-bottom: 6px;
}

.settings-note-page :deep(.settings-sortable-page__controls-card) {
  padding: 0;
  border: 1px solid rgba(130, 143, 165, 0.18);
  border-radius: var(--ios-pms-radius-pill);
  background: rgba(255, 255, 255, 0.88);
  box-shadow: 0 4px 12px rgba(77, 98, 145, 0.035);
}

.settings-note-page :deep(.settings-sortable-page__controls-card ion-segment) {
  min-height: 36px;
  padding: 0;
  border-radius: var(--ios-pms-radius-pill);
  background: transparent;
}

.settings-note-page :deep(.settings-sortable-page__controls-card ion-segment-button) {
  min-width: 0;
  min-height: 36px;
  margin: 0;
  --border-radius: var(--ios-pms-radius-pill);
  --color: var(--ios-pms-text-primary);
  --color-checked: #ffffff;
  --indicator-color: #343436;
  --indicator-box-shadow: none;
  font-size: 16px;
  font-weight: 500;
  letter-spacing: 0;
  text-transform: none;
}

.settings-note-page
  :deep(.settings-sortable-page__controls-card ion-segment-button::part(indicator-background)) {
  border-radius: var(--ios-pms-radius-pill);
}

.settings-note-page :deep(.settings-page-shell__stack > .mobile-card:not(.settings-sortable-page__controls-card)) {
  padding: 18px 16px 22px;
  border: 1px solid var(--ios-pms-dashboard-card-border);
  border-radius: var(--ios-pms-radius-card);
  background: var(--ios-pms-dashboard-card-background);
  box-shadow: var(--ios-pms-dashboard-card-shadow);
}

.settings-note-page :deep(.settings-page-block__section-header) {
  align-items: center;
  min-height: 32px;
}

.settings-note-page :deep(.settings-page-block__section-header .mobile-section-title) {
  margin: 0;
  color: #333333;
  font-size: 22px;
  font-weight: 500;
  line-height: 1.25;
  letter-spacing: 0;
}

.settings-note-page :deep(.settings-page-block__section-header ion-spinner) {
  width: 18px;
  height: 18px;
  color: rgba(var(--ion-color-primary-rgb), 0.78);
}

.settings-note-page :deep(.settings-minimal-list) {
  gap: 14px;
  margin-top: 18px;
}

.settings-note-page :deep(.note-category-card) {
  padding: 16px 14px 14px;
  border: 1px solid rgba(130, 143, 165, 0.2);
  border-radius: var(--ios-pms-radius-input);
  background: rgba(255, 255, 255, 0.86);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.9),
    0 6px 16px rgba(77, 98, 145, 0.035);
}

.settings-note-page :deep(.note-category-card::before) {
  display: none;
}

.settings-note-page :deep(.note-category-card .settings-minimal-card__header) {
  flex-wrap: nowrap;
  align-items: flex-start;
  gap: 12px;
}

.settings-note-page :deep(.note-category-card .settings-minimal-card__title-group) {
  flex: 1;
  gap: 8px;
}

.settings-note-page :deep(.note-category-card .settings-minimal-card__title-group strong) {
  color: #333333;
  font-size: 19px;
  font-weight: 500;
  line-height: 1.3;
  letter-spacing: 0;
}

.settings-note-page :deep(.note-category-card .settings-minimal-card__summary) {
  color: #999999;
  font-size: 13px;
  font-weight: 400;
  line-height: 1.45;
  letter-spacing: 0;
}

.settings-note-page :deep(.note-category-card .settings-minimal-card__badge) {
  box-sizing: border-box;
  height: 26px;
  min-height: 26px;
  padding: 0 9px;
  border: 0;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 400;
  line-height: 26px;
  letter-spacing: 0;
}

.settings-note-page :deep(.note-category-card .settings-minimal-card__badge--success) {
  background: #e6f7ff;
  color: #1890ff;
}

.settings-note-page :deep(.note-category-card .settings-minimal-card__badge--warning) {
  background: #fff2e8;
  color: #fa8c16;
}

.settings-note-page :deep(.note-category-card .settings-minimal-card__actions) {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-start;
  gap: 8px;
  margin-top: 14px;
  padding-top: 10px;
  border-top-color: rgba(130, 143, 165, 0.2);
}

.settings-note-page :deep(.note-category-card .settings-minimal-card__actions ion-button) {
  width: auto;
  min-width: 62px;
  min-height: 28px;
  height: 28px;
  margin: 0;
  --padding-start: 15px;
  --padding-end: 15px;
  --padding-top: 0;
  --padding-bottom: 0;
  --background: #ffffff;
  --background-activated: #f7f7f7;
  --border-color: #d9d9d9;
  --border-style: solid;
  --border-width: 1px;
  --border-radius: 11px;
  --box-shadow: none;
  --color: #2346ff;
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 0;
}

.settings-note-page
  :deep(.note-category-card .settings-minimal-card__actions ion-button[fill='outline']) {
  --background: #ffffff;
  --border-color: #d9d9d9;
  --border-width: 1px;
  --color: #2346ff;
}

.settings-note-page
  :deep(.note-category-card .settings-minimal-card__actions ion-button[color='danger']) {
  --background: #ffffff;
  --border-color: #d9d9d9;
  --border-style: solid;
  --border-width: 1px;
  --color: #ff0000;
}

.settings-note-page
  :deep(.note-category-card .settings-minimal-card__actions ion-button:first-child) {
  --background: #266eff;
  --background-activated: #1f5edb;
  --border-color: #266eff;
  --color: #ffffff;
}

.settings-note-page
  :deep(.note-category-card .settings-minimal-card__actions ion-button.button-disabled) {
  opacity: 0.42;
}

.settings-note-page :deep(.settings-section-card__footer) {
  margin-top: 20px;
}

.settings-note-page :deep(.settings-form-actions--section) {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 36px;
  margin-top: 0;
  padding-top: 14px;
  border-top: 0;
}

.settings-note-page :deep(.settings-form-actions--section ion-button) {
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

.settings-note-page :deep(.settings-form-actions--section ion-button:first-child) {
  --background: #ffffff;
  --border-color: #d9d9d9;
  --border-width: 1px;
  --color: #999999;
}

.settings-note-page :deep(.settings-form-actions--section ion-button:last-child) {
  --background: #1890ff;
  --background-activated: #1683e6;
  --color: #ffffff;
}

.settings-note-page :deep(.note-settings-empty) {
  margin: 18px 0 0;
  padding: 24px 12px;
  border: 1px dashed rgba(130, 143, 165, 0.24);
  border-radius: var(--ios-pms-radius-input);
  color: var(--ios-pms-text-muted);
  text-align: center;
}

@media (max-width: 374px) {
  .settings-note-page :deep(.settings-sortable-page) {
    --padding-start: 12px;
    --padding-end: 12px;
  }

  .settings-note-page
    :deep(.settings-page-shell__stack > .mobile-card:not(.settings-sortable-page__controls-card)) {
    padding-right: 14px;
    padding-left: 14px;
  }

  .settings-note-page :deep(.settings-page-block__section-header .mobile-section-title) {
    font-size: 20px;
  }

  .settings-note-page :deep(.note-category-card) {
    padding-right: 12px;
    padding-left: 12px;
  }

  .settings-note-page :deep(.note-category-card .settings-minimal-card__title-group strong) {
    font-size: 17px;
  }
}
</style>
