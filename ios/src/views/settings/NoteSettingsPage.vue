<template>
  <SettingsSortablePage
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
      <article v-for="(category, index) in currentCategories" :key="category.id" class="settings-minimal-card">
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

    <p v-else-if="!loading" class="mobile-note">{{ $t('stage5SourceText.56') }}</p>

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
