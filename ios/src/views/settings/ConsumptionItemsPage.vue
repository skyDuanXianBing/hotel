<template>
  <SettingsPageShell
    :back-href="ROUTE_PATHS.settings"
    title="消费项"
    hero-eyebrow="价格与商品"
    hero-title="消费项管理"
    :chips="[{ label: `消费项 ${items.length}` }, { label: `分类 ${categories.length}` }]"
    :show-refresher="true"
    refresher-pulling-text="下拉刷新消费项"
    content-class="settings-page-block"
    hero-class="settings-page-block__hero"
    eyebrow-class="settings-page-block__eyebrow"
    @refresh="handleRefresh"
  >
    <section class="mobile-card">
      <div class="mobile-inline-row settings-consumption-page__toolbar">
        <ion-segment :value="activeSegment" @ionChange="handleSegmentChange">
          <ion-segment-button value="items">
            <ion-label>消费项</ion-label>
          </ion-segment-button>
          <ion-segment-button value="categories">
            <ion-label>分类</ion-label>
          </ion-segment-button>
        </ion-segment>

        <ion-button size="small" @click="handleOpenCreate">
          {{ activeSegment === 'items' ? '新增消费项' : '新增分类' }}
        </ion-button>
      </div>
    </section>

    <SettingsSectionCard
      v-if="activeSegment === 'items'"
      title="消费项列表"
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
            <span class="settings-minimal-card__badge">¥{{ item.price.toFixed(2) }}</span>
          </div>

          <div class="settings-minimal-card__meta">
            <span
              class="settings-minimal-card__meta-pill"
              :class="item.enabled ? 'settings-minimal-card__meta-pill--success' : 'settings-minimal-card__meta-pill--warning'"
            >
              {{ item.enabled ? '已启用' : '已停用' }}
            </span>
          </div>

          <div class="settings-minimal-card__actions">
            <ion-button size="small" fill="outline" @click="handleEditItem(item)">编辑</ion-button>
            <ion-button size="small" fill="outline" @click="handleToggleItem(item)">
              {{ item.enabled ? '停用' : '启用' }}
            </ion-button>
            <ion-button size="small" color="danger" fill="clear" @click="handleDeleteItem(item)">删除</ion-button>
          </div>
        </article>
      </div>

      <p v-else-if="!loading" class="mobile-note settings-empty-state">当前暂无消费项。</p>
    </SettingsSectionCard>

    <SettingsSectionCard
      v-else
      title="分类列表"
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
                {{ category.description || '未填写分类说明' }}
              </p>
            </div>
            <span class="settings-minimal-card__badge">消费项 {{ category.count || 0 }}</span>
          </div>

          <div class="settings-minimal-card__actions">
            <ion-button size="small" fill="outline" @click="handleEditCategory(category)">编辑</ion-button>
            <ion-button size="small" color="danger" fill="clear" @click="handleDeleteCategory(category)">删除</ion-button>
          </div>
        </article>
      </div>

      <p v-else-if="!loading" class="mobile-note settings-empty-state">当前暂无消费项分类。</p>
    </SettingsSectionCard>

    <SettingsEditorModal
      :is-open="itemEditorOpen"
      :title="editingItemId ? '编辑消费项' : '新增消费项'"
      @close="handleDismissItemEditor"
      @didDismiss="handleDismissItemEditor"
    >
      <div class="settings-form-grid">
        <label class="settings-form-field">
          <span>分类</span>
          <ion-select v-model="itemForm.category" fill="outline" interface="modal">
            <ion-select-option v-for="category in categories" :key="category.id" :value="category.name">
              {{ category.name }}
            </ion-select-option>
          </ion-select>
        </label>

        <label class="settings-form-field">
          <span>名称</span>
          <ion-input v-model="itemForm.name" fill="outline" placeholder="请输入消费项名称" />
        </label>

        <label class="settings-form-field">
          <span>价格</span>
          <ion-input v-model="itemForm.price" fill="outline" inputmode="decimal" placeholder="0.00" />
        </label>

        <div class="settings-toggle-field">
          <div>
            <strong>启用状态</strong>
          </div>
          <ion-toggle v-model="itemForm.enabled" />
        </div>

        <label class="settings-form-field settings-form-field--full">
          <span>描述</span>
          <ion-textarea v-model="itemForm.description" :rows="4" fill="outline" placeholder="请输入描述" />
        </label>
      </div>

      <template #actions>
        <ion-button fill="outline" @click="handleDismissItemEditor">取消</ion-button>
        <ion-button :disabled="submitting" @click="handleSaveItem">
          {{ submitting ? '提交中...' : '保存消费项' }}
        </ion-button>
      </template>
    </SettingsEditorModal>

    <SettingsEditorModal
      :is-open="categoryEditorOpen"
      :title="editingCategoryId ? '编辑分类' : '新增分类'"
      @close="handleDismissCategoryEditor"
      @didDismiss="handleDismissCategoryEditor"
    >
      <div class="settings-form-grid">
        <label class="settings-form-field">
          <span>分类名称</span>
          <ion-input v-model="categoryForm.name" fill="outline" placeholder="请输入分类名称" />
        </label>

        <label class="settings-form-field settings-form-field--full">
          <span>分类描述</span>
          <ion-textarea v-model="categoryForm.description" :rows="4" fill="outline" placeholder="请输入分类说明" />
        </label>
      </div>

      <template #actions>
        <ion-button fill="outline" @click="handleDismissCategoryEditor">取消</ion-button>
        <ion-button :disabled="submitting" @click="handleSaveCategory">
          {{ submitting ? '提交中...' : '保存分类' }}
        </ion-button>
      </template>
    </SettingsEditorModal>
  </SettingsPageShell>
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
  IonTextarea,
  IonToggle,
  onIonViewWillEnter,
} from '@ionic/vue'
import { ref } from 'vue'
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
import { showSuccessToast, showWarningToast } from '@/utils/notify'
import { isHandledRequestError } from '@/utils/request'

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
        text: '取消',
        role: 'cancel',
      },
      {
        text: '确认删除',
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
      throw new Error(itemResponse.message || '加载消费项失败')
    }
    if (!categoryResponse.success || !categoryResponse.data) {
      throw new Error(categoryResponse.message || '加载分类失败')
    }

    items.value = itemResponse.data
    categories.value = categoryResponse.data
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, '加载消费项失败'))
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
    showWarningToast('请选择分类')
    return
  }
  if (!itemForm.value.name.trim()) {
    showWarningToast('请输入消费项名称')
    return
  }

  const price = Number(itemForm.value.price)
  if (!Number.isFinite(price) || price < 0) {
    showWarningToast('请输入有效价格')
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
        throw new Error(response.message || '更新消费项失败')
      }
      showSuccessToast('消费项已更新')
    } else {
      const response = await createConsumptionItem(payload)
      if (!response.success) {
        throw new Error(response.message || '创建消费项失败')
      }
      showSuccessToast('消费项已创建')
    }

    handleDismissItemEditor()
    await loadPageData()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, '保存消费项失败'))
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
      throw new Error(response.message || '更新消费项状态失败')
    }
    showSuccessToast(nextEnabled ? '消费项已启用' : '消费项已停用')
    await loadPageData()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, '更新消费项状态失败'))
    }
  }
}

async function handleDeleteItem(item: ConsumptionItemDTO) {
  if (!item.id) {
    return
  }

  const confirmed = await confirmDelete('删除消费项', `确认删除 ${item.name} 吗？`)
  if (!confirmed) {
    return
  }

  try {
    const response = await deleteConsumptionItem(item.id)
    if (!response.success) {
      throw new Error(response.message || '删除消费项失败')
    }
    showSuccessToast('消费项已删除')
    await loadPageData()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, '删除消费项失败'))
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
    showWarningToast('请输入分类名称')
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
        throw new Error(response.message || '更新分类失败')
      }
      showSuccessToast('分类已更新')
    } else {
      const response = await createConsumptionCategory(payload)
      if (!response.success) {
        throw new Error(response.message || '创建分类失败')
      }
      showSuccessToast('分类已创建')
    }

    handleDismissCategoryEditor()
    await loadPageData()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, '保存分类失败'))
    }
  } finally {
    submitting.value = false
  }
}

async function handleDeleteCategory(category: ConsumptionCategoryDTO) {
  if (!category.id) {
    return
  }

  const confirmed = await confirmDelete('删除分类', `确认删除 ${category.name} 吗？`)
  if (!confirmed) {
    return
  }

  try {
    const response = await deleteConsumptionCategory(category.id)
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
.settings-consumption-page__toolbar {
  align-items: flex-start;
}
</style>
