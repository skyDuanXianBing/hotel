<template>
  <SettingsSortablePage
    :back-href="ROUTE_PATHS.settings"
    title="记一笔设置"
    hero-eyebrow="财务设置"
    hero-title="记一笔分类"
    toolbar-action-label="新增"
    :show-refresher="true"
    refresher-pulling-text="下拉刷新记一笔设置"
    section-title="分类列表"
    :loading="loading"
    :modal-open="editorOpen"
    :modal-title="editingCategoryId ? '编辑分类' : '新增分类'"
    @toolbar-action="handleCreateCategory"
    @refresh="handleRefresh"
    @dismiss-editor="handleDismissEditor"
  >
    <template #controls>
      <ion-segment :value="activeType" @ionChange="handleTypeChange">
        <ion-segment-button value="income">
          <ion-label>收入</ion-label>
        </ion-segment-button>
        <ion-segment-button value="expense">
          <ion-label>支出</ion-label>
        </ion-segment-button>
      </ion-segment>
    </template>

    <div v-if="currentCategories.length > 0" class="mobile-list settings-minimal-list">
      <article v-for="(category, index) in currentCategories" :key="category.id" class="settings-minimal-card">
        <div class="settings-minimal-card__header">
          <div class="settings-minimal-card__title-group">
            <strong>{{ category.name }}</strong>
            <p class="settings-minimal-card__summary">顺序 {{ index + 1 }}</p>
          </div>
          <span
            class="settings-minimal-card__badge"
            :class="category.type === 'income' ? 'settings-minimal-card__badge--success' : 'settings-minimal-card__badge--warning'"
          >
            {{ category.type === 'income' ? '收入' : '支出' }}
          </span>
        </div>

        <div class="settings-minimal-card__actions settings-minimal-card__actions--dense">
          <ion-button size="small" fill="outline" @click="handleEditCategory(category)">编辑</ion-button>
          <ion-button size="small" fill="outline" :disabled="index === 0" @click="handleMove(index, -1)">上移</ion-button>
          <ion-button
            size="small"
            fill="outline"
            :disabled="index === currentCategories.length - 1"
            @click="handleMove(index, 1)"
          >
            下移
          </ion-button>
          <ion-button size="small" color="danger" fill="clear" @click="handleDeleteCategory(category)">删除</ion-button>
        </div>
      </article>
    </div>

    <p v-else-if="!loading" class="mobile-note">当前分类为空。</p>

    <template #sectionFooter>
      <div class="settings-form-actions settings-form-actions--section">
        <ion-button fill="outline" :disabled="loading || savingOrder" @click="loadPageData">重置</ion-button>
        <ion-button :disabled="loading || savingOrder || currentCategories.length === 0" @click="handleSaveOrder">
          {{ savingOrder ? '保存中...' : '保存顺序' }}
        </ion-button>
      </div>
    </template>

    <template #modalContent>
      <div class="settings-form-grid">
        <label class="settings-form-field">
          <span>分类名称</span>
          <ion-input v-model="categoryForm.name" fill="outline" placeholder="请输入分类名称" />
        </label>

        <label class="settings-form-field">
          <span>分类类型</span>
          <ion-select v-model="categoryForm.type" fill="outline" interface="action-sheet">
            <ion-select-option value="income">收入</ion-select-option>
            <ion-select-option value="expense">支出</ion-select-option>
          </ion-select>
        </label>
      </div>
    </template>

    <template #modalActions>
      <ion-button fill="outline" @click="handleDismissEditor">取消</ion-button>
      <ion-button :disabled="submitting" @click="handleSaveCategory">
        {{ submitting ? '提交中...' : '保存分类' }}
      </ion-button>
    </template>
  </SettingsSortablePage>
</template>

<script setup lang="ts">
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
    header: '删除分类',
    message: `确认删除 ${name} 吗？`,
    buttons: [
      { text: '取消', role: 'cancel' },
      { text: '确认删除', role: 'destructive' },
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
      throw new Error(response.message || '加载分类失败')
    }
    categories.value = [...response.data].sort((left, right) => left.displayOrder - right.displayOrder)
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, '加载分类失败'))
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
    showWarningToast('请输入分类名称')
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
        throw new Error(response.message || '更新分类失败')
      }
    } else {
      const response = await createCategory({
        name: categoryForm.value.name.trim(),
        type: categoryForm.value.type,
      })
      if (!response.success) {
        throw new Error(response.message || '创建分类失败')
      }
    }

    showSuccessToast('分类已保存')
    handleDismissEditor()
    await loadPageData()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, '保存分类失败'))
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
      throw new Error(response.message || '保存顺序失败')
    }
    showSuccessToast('分类顺序已保存')
    await loadPageData()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, '保存顺序失败'))
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
      throw new Error(response.message || '删除分类失败')
    }
    showSuccessToast('分类已删除')
    await loadPageData()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, '删除分类失败'))
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
