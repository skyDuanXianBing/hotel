<template>
  <ion-page>
    <ion-header translucent>
      <ion-toolbar>
        <ion-buttons slot="start">
          <ion-back-button :default-href="ROUTE_PATHS.settings" />
        </ion-buttons>
        <ion-title>记一笔设置</ion-title>
        <ion-buttons slot="end">
          <ion-button @click="handleCreateCategory">新增</ion-button>
        </ion-buttons>
      </ion-toolbar>
    </ion-header>

    <ion-content fullscreen class="mobile-page settings-page-block">
      <ion-refresher slot="fixed" @ionRefresh="handleRefresh">
        <ion-refresher-content pulling-text="下拉刷新记一笔设置" refreshing-spinner="crescent" />
      </ion-refresher>

      <section class="mobile-hero settings-page-block__hero">
        <p class="mobile-note settings-page-block__eyebrow">财务设置</p>
        <h1 class="mobile-title">记一笔分类</h1>
        <p class="mobile-subtitle">按收入 / 支出两类维护分类，并支持顺序调整。</p>
      </section>

      <div class="mobile-stack">
        <section class="mobile-card">
          <ion-segment :value="activeType" @ionChange="handleTypeChange">
            <ion-segment-button value="income">
              <ion-label>收入</ion-label>
            </ion-segment-button>
            <ion-segment-button value="expense">
              <ion-label>支出</ion-label>
            </ion-segment-button>
          </ion-segment>
        </section>

        <section class="mobile-card">
          <div class="mobile-inline-row settings-page-block__section-header">
            <div>
              <h2 class="mobile-section-title">分类列表</h2>
              <p class="mobile-note">顺序保存后会影响记一笔页的展示优先级。</p>
            </div>
            <ion-spinner v-if="loading" name="crescent" />
          </div>

          <div v-if="currentCategories.length > 0" class="mobile-list settings-card-list">
            <article v-for="(category, index) in currentCategories" :key="category.id" class="settings-card-item">
              <div>
                <strong>{{ category.name }}</strong>
                <p>顺序 {{ index + 1 }} · {{ category.type === 'income' ? '收入' : '支出' }}</p>
              </div>

              <div class="settings-card-item__actions">
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

          <div class="settings-form-actions settings-form-actions--section">
            <ion-button fill="outline" :disabled="loading || savingOrder" @click="loadPageData">重置</ion-button>
            <ion-button :disabled="loading || savingOrder || currentCategories.length === 0" @click="handleSaveOrder">
              {{ savingOrder ? '保存中...' : '保存顺序' }}
            </ion-button>
          </div>
        </section>
      </div>

      <ion-modal :is-open="editorOpen" @didDismiss="handleDismissEditor">
        <ion-header>
          <ion-toolbar>
            <ion-title>{{ editingCategoryId ? '编辑分类' : '新增分类' }}</ion-title>
            <ion-buttons slot="end">
              <ion-button @click="handleDismissEditor">关闭</ion-button>
            </ion-buttons>
          </ion-toolbar>
        </ion-header>

        <ion-content class="mobile-page settings-modal-page">
          <section class="mobile-card">
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

            <div class="settings-form-actions">
              <ion-button fill="outline" @click="handleDismissEditor">取消</ion-button>
              <ion-button :disabled="submitting" @click="handleSaveCategory">
                {{ submitting ? '提交中...' : '保存分类' }}
              </ion-button>
            </div>
          </section>
        </ion-content>
      </ion-modal>
    </ion-content>
  </ion-page>
</template>

<script setup lang="ts">
import {
  alertController,
  IonBackButton,
  IonButton,
  IonButtons,
  IonContent,
  IonHeader,
  IonInput,
  IonLabel,
  IonModal,
  IonPage,
  IonRefresher,
  IonRefresherContent,
  IonSegment,
  IonSegmentButton,
  IonSelect,
  IonSelectOption,
  IonSpinner,
  IonTitle,
  IonToolbar,
  onIonViewWillEnter,
} from '@ionic/vue'
import { computed, ref } from 'vue'
import { createCategory, deleteCategory, getAllCategories, updateCategoriesOrder, updateCategory } from '@/api/noteCategory'
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

<style scoped>
.settings-page-block {
  display: block;
}

.settings-page-block__hero {
  margin-top: 4px;
}

.settings-page-block__eyebrow {
  color: var(--ion-color-primary);
  font-weight: 700;
}

.settings-page-block__section-header {
  align-items: flex-start;
}

.settings-card-list {
  margin-top: 16px;
}

.settings-card-item {
  padding: 14px;
  border-radius: 18px;
  border: 1px solid var(--app-border);
  background: rgba(255, 255, 255, 0.82);
}

.settings-card-item strong,
.settings-card-item p {
  margin: 0;
}

.settings-card-item p {
  margin-top: 6px;
  color: var(--app-muted);
  font-size: 13px;
}

.settings-card-item__actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 12px;
}

.settings-modal-page {
  --padding-top: 16px;
  --padding-bottom: 24px;
  --padding-start: 16px;
  --padding-end: 16px;
}

.settings-form-grid {
  display: grid;
  gap: 14px;
}

.settings-form-field {
  display: grid;
  gap: 8px;
}

.settings-form-field span {
  color: var(--app-heading);
  font-size: 13px;
  font-weight: 600;
}

.settings-form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  flex-wrap: wrap;
  margin-top: 18px;
}

.settings-form-actions--section {
  margin-top: 18px;
}
</style>
