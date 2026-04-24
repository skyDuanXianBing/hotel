<template>
  <ion-page>
    <ion-header translucent>
      <ion-toolbar>
        <ion-buttons slot="start">
          <ion-back-button :default-href="ROUTE_PATHS.settingsPricePlans" />
        </ion-buttons>
        <ion-title>房型价格</ion-title>
        <ion-buttons slot="end">
          <ion-button @click="handleCreateRelation">新增关联</ion-button>
        </ion-buttons>
      </ion-toolbar>
    </ion-header>

    <ion-content fullscreen class="mobile-page settings-price-plan-rates-page">
      <ion-refresher slot="fixed" @ionRefresh="handleRefresh">
        <ion-refresher-content pulling-text="下拉刷新房型价格" refreshing-spinner="crescent" />
      </ion-refresher>

      <section class="mobile-hero settings-price-plan-rates-hero">
        <p class="mobile-note settings-price-plan-rates-hero__eyebrow">价格计划房型价格</p>
        <h1 class="mobile-title">{{ planTitle }}</h1>
        <div class="mobile-chip-row">
          <span class="mobile-chip">关联房型 {{ relations.length }}</span>
          <span class="mobile-chip">{{ includeMealLabel }}</span>
        </div>
      </section>

      <div class="mobile-stack">
        <section class="mobile-card">
          <div class="mobile-inline-row settings-price-plan-rates-page__section-header">
            <div>
              <h2 class="mobile-section-title">已关联房型</h2>
            </div>
            <ion-spinner v-if="loading" name="crescent" />
          </div>

          <div v-if="relations.length > 0" class="mobile-list settings-minimal-list settings-price-plan-rates-list">
            <article v-for="relation in relations" :key="relation.id" class="settings-minimal-card settings-price-plan-rate-card">
              <div class="settings-minimal-card__header">
                <div class="settings-minimal-card__title-group">
                  <strong>{{ relation.roomType?.name || '未命名房型' }}</strong>
                  <p class="settings-minimal-card__summary">
                    最大入住 {{ relation.maxGuests }} 人 · 包含 {{ relation.includedGuests ?? '-' }} 人
                  </p>
                </div>
                <span class="settings-minimal-card__badge">{{ formatPriceModeLabel(relation.priceMode) }}</span>
              </div>

              <div class="settings-minimal-card__meta">
                <span class="settings-minimal-card__meta-pill">{{ buildWeeklyPricePreview(relation) }}</span>
                <span class="settings-minimal-card__meta-pill">{{ buildExtraGuestPreview(relation) }}</span>
              </div>

              <div class="settings-minimal-card__actions">
                <ion-button size="small" fill="outline" @click="handleEditRelation(relation)">编辑</ion-button>
                <ion-button size="small" color="danger" fill="clear" @click="handleDeleteRelation(relation)">
                  删除
                </ion-button>
              </div>
            </article>
          </div>

          <div v-else-if="!loading" class="settings-price-plan-rates-empty-state">
            <strong>当前计划尚未关联任何房型</strong>
            <p>先新增首条关联，再继续填写本周房价与入住人数，完成价格计划闭环。</p>
            <ion-button @click="handleCreateRelation">新增首条关联</ion-button>
          </div>
        </section>

      </div>

      <ion-modal :is-open="editorOpen" @didDismiss="handleDismissEditor">
        <ion-header>
          <ion-toolbar>
            <ion-title>{{ editingRelationId ? '编辑房型价格' : '新增房型价格' }}</ion-title>
            <ion-buttons slot="end">
              <ion-button @click="handleDismissEditor">关闭</ion-button>
            </ion-buttons>
          </ion-toolbar>
        </ion-header>

        <ion-content class="mobile-page settings-modal-page">
          <section class="mobile-card">
            <div class="settings-form-grid">
              <label class="settings-form-field">
                <span>房型</span>
                <ion-select
                  v-model="relationForm.roomTypeId"
                  fill="outline"
                  interface="modal"
                  :disabled="Boolean(editingRelationId)"
                >
                  <ion-select-option v-for="roomType in availableRoomTypes" :key="roomType.id" :value="roomType.id">
                    {{ roomType.name }}
                  </ion-select-option>
                </ion-select>
              </label>

              <label class="settings-form-field">
                <span>价格模式</span>
                <ion-select v-model="relationForm.priceMode" fill="outline" interface="action-sheet">
                  <ion-select-option value="unified">unified</ion-select-option>
                  <ion-select-option value="multiple">multiple</ion-select-option>
                </ion-select>
              </label>

              <label class="settings-form-field">
                <span>最大入住人数</span>
                <ion-input v-model="relationForm.maxGuests" fill="outline" inputmode="numeric" placeholder="2" />
              </label>

              <label class="settings-form-field">
                <span>包含人数</span>
                <ion-input v-model="relationForm.includedGuests" fill="outline" inputmode="numeric" placeholder="2" />
              </label>

              <label v-for="day in weekFields" :key="day.key" class="settings-form-field">
                <span>{{ day.label }}</span>
                <ion-input v-model="relationForm[day.key]" fill="outline" inputmode="decimal" placeholder="0" />
              </label>

              <label class="settings-form-field">
                <span>额外成人加价</span>
                <ion-input v-model="relationForm.extraAdultRate" fill="outline" inputmode="decimal" placeholder="0" />
              </label>

              <label class="settings-form-field">
                <span>额外儿童加价</span>
                <ion-input v-model="relationForm.extraChildRate" fill="outline" inputmode="decimal" placeholder="0" />
              </label>

              <div v-if="editingRelationId" class="settings-price-plan-rates-page__toggle-card">
                <div>
                  <strong>保存时清理未来覆盖价</strong>
                </div>
                <ion-checkbox :checked="clearFutureOverridesOnSave" @ionChange="handleClearFutureOverridesChange" />
              </div>
            </div>

            <div class="settings-form-actions">
              <ion-button fill="outline" @click="handleDismissEditor">取消</ion-button>
              <ion-button :disabled="submitting" @click="handleSaveRelation">
                {{ submitting ? '提交中...' : '保存房型价格' }}
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
  IonCheckbox,
  IonContent,
  IonHeader,
  IonInput,
  IonModal,
  IonPage,
  IonRefresher,
  IonRefresherContent,
  IonSelect,
  IonSelectOption,
  IonSpinner,
  IonTitle,
  IonToolbar,
  onIonViewWillEnter,
} from '@ionic/vue'
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  assignPricePlanToRoomType,
  deleteRoomTypePricePlan,
  getPricePlanById,
  getRoomTypesByPricePlan,
  updateRoomTypePricePlan,
  type PricePlanDTO,
  type RoomTypePricePlanDTO,
} from '@/api/pricePlan'
import { getAllRoomTypes, type RoomTypeDTO } from '@/api/roomType'
import { ROUTE_PATHS } from '@/router/guards'
import { useUserStore } from '@/stores/user'
import { getTodayDate } from '@/utils/accommodation'
import { showSuccessToast, showWarningToast } from '@/utils/notify'
import { isHandledRequestError } from '@/utils/request'

type WeekFieldKey =
  | 'mondayPrice'
  | 'tuesdayPrice'
  | 'wednesdayPrice'
  | 'thursdayPrice'
  | 'fridayPrice'
  | 'saturdayPrice'
  | 'sundayPrice'

interface RelationFormState {
  roomTypeId: number | null
  priceMode: string
  maxGuests: string
  includedGuests: string
  mondayPrice: string
  tuesdayPrice: string
  wednesdayPrice: string
  thursdayPrice: string
  fridayPrice: string
  saturdayPrice: string
  sundayPrice: string
  extraAdultRate: string
  extraChildRate: string
}

const weekFields: Array<{ key: WeekFieldKey; label: string }> = [
  { key: 'mondayPrice', label: '周一价格' },
  { key: 'tuesdayPrice', label: '周二价格' },
  { key: 'wednesdayPrice', label: '周三价格' },
  { key: 'thursdayPrice', label: '周四价格' },
  { key: 'fridayPrice', label: '周五价格' },
  { key: 'saturdayPrice', label: '周六价格' },
  { key: 'sundayPrice', label: '周日价格' },
]

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const submitting = ref(false)
const editorOpen = ref(false)
const editingRelationId = ref<number | null>(null)
const plan = ref<PricePlanDTO | null>(null)
const relations = ref<RoomTypePricePlanDTO[]>([])
const allRoomTypes = ref<RoomTypeDTO[]>([])
const relationForm = ref<RelationFormState>(createEmptyRelationForm())
const clearFutureOverridesOnSave = ref(true)

const pricePlanId = computed(() => {
  return Number(route.params.pricePlanId || 0)
})

const planTitle = computed(() => {
  return plan.value?.name || '房型价格'
})

const includeMealLabel = computed(() => {
  if (!plan.value) {
    return '计划信息加载中'
  }
  return plan.value.includeMeal ? '默认含餐' : '默认不含餐'
})

const availableRoomTypes = computed(() => {
  const assignedRoomTypeIds: number[] = []

  for (const relation of relations.value) {
    if (relation.roomType?.id && relation.id !== editingRelationId.value) {
      assignedRoomTypeIds.push(relation.roomType.id)
    }
  }

  return allRoomTypes.value.filter((roomType) => !assignedRoomTypeIds.includes(roomType.id))
})

function createEmptyRelationForm(): RelationFormState {
  return {
    roomTypeId: null,
    priceMode: 'unified',
    maxGuests: '2',
    includedGuests: '2',
    mondayPrice: '0',
    tuesdayPrice: '0',
    wednesdayPrice: '0',
    thursdayPrice: '0',
    fridayPrice: '0',
    saturdayPrice: '0',
    sundayPrice: '0',
    extraAdultRate: '0',
    extraChildRate: '0',
  }
}

function resolveWarningMessage(error: unknown, fallbackMessage: string) {
  if (error instanceof Error && error.message) {
    return error.message
  }
  return fallbackMessage
}

function parseOptionalNumber(value: string) {
  const normalized = value.trim()
  if (!normalized) {
    return undefined
  }

  const parsed = Number(normalized)
  if (!Number.isFinite(parsed)) {
    return undefined
  }

  return parsed
}

function formatPrice(value?: number) {
  if (value === undefined || value === null) {
    return '¥0'
  }
  return `¥${Number(value).toFixed(0)}`
}

function formatPriceModeLabel(value?: string) {
  return value === 'multiple' ? '多价' : '统一价'
}

function buildWeeklyPricePreview(relation: RoomTypePricePlanDTO) {
  const prices = [
    relation.mondayPrice,
    relation.tuesdayPrice,
    relation.wednesdayPrice,
    relation.thursdayPrice,
    relation.fridayPrice,
    relation.saturdayPrice,
    relation.sundayPrice,
  ].filter((value): value is number => value !== undefined && value !== null)

  if (prices.length === 0) {
    return '周价 ¥0'
  }

  const minPrice = Math.min(...prices)
  const maxPrice = Math.max(...prices)
  if (minPrice === maxPrice) {
    return `周价 ${formatPrice(minPrice)}`
  }
  return `周价 ${formatPrice(minPrice)} - ${formatPrice(maxPrice)}`
}

function buildExtraGuestPreview(relation: RoomTypePricePlanDTO) {
  return `加人 成人 ${formatPrice(relation.extraAdultRate)} / 儿童 ${formatPrice(relation.extraChildRate)}`
}

async function confirmDelete(roomTypeName: string) {
  const alert = await alertController.create({
    header: '删除房型价格',
    message: `请选择删除 ${roomTypeName} 价格关联的处理方式。`,
    buttons: [
      {
        text: '取消',
        role: 'cancel',
      },
      {
        text: '仅解绑',
        role: 'unbind',
      },
      {
        text: '解绑并清理覆盖价',
        role: 'destructive',
      },
    ],
  })

  await alert.present()
  const result = await alert.onDidDismiss()
  if (result.role === 'destructive') {
    return true
  }
  if (result.role === 'unbind') {
    return false
  }
  return null
}

async function loadPageData() {
  const userId = userStore.currentUser?.id
  if (!pricePlanId.value) {
    await handleInvalidPricePlan()
    return
  }

  if (!userId) {
    showWarningToast('请先恢复当前用户信息')
    return
  }

  loading.value = true
  try {
    const planResponse = await getPricePlanById(pricePlanId.value, userId)
    if (!planResponse.success || !planResponse.data) {
      await handleInvalidPricePlan(planResponse.message)
      return
    }

    const [relationResponse, roomTypeResponse] = await Promise.all([
      getRoomTypesByPricePlan(pricePlanId.value),
      getAllRoomTypes(),
    ])

    if (!relationResponse.success || !relationResponse.data) {
      throw new Error(relationResponse.message || '加载房型价格失败')
    }
    if (!roomTypeResponse.success || !roomTypeResponse.data) {
      throw new Error(roomTypeResponse.message || '加载房型列表失败')
    }

    plan.value = planResponse.data
    relations.value = relationResponse.data
    allRoomTypes.value = roomTypeResponse.data
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, '加载房型价格失败'))
    }
  } finally {
    loading.value = false
  }
}

async function handleInvalidPricePlan(message?: string) {
  showWarningToast(message || '价格计划不存在或已被删除')
  await router.replace(ROUTE_PATHS.settingsPricePlans)
}

function handleCreateRelation() {
  editingRelationId.value = null
  relationForm.value = createEmptyRelationForm()
  clearFutureOverridesOnSave.value = true
  editorOpen.value = true
}

function handleEditRelation(relation: RoomTypePricePlanDTO) {
  editingRelationId.value = Number(relation.id)
  relationForm.value = {
    roomTypeId: relation.roomType?.id || null,
    priceMode: relation.priceMode || 'unified',
    maxGuests: String(relation.maxGuests || 2),
    includedGuests: relation.includedGuests !== undefined ? String(relation.includedGuests) : '2',
    mondayPrice: String(relation.mondayPrice ?? 0),
    tuesdayPrice: String(relation.tuesdayPrice ?? 0),
    wednesdayPrice: String(relation.wednesdayPrice ?? 0),
    thursdayPrice: String(relation.thursdayPrice ?? 0),
    fridayPrice: String(relation.fridayPrice ?? 0),
    saturdayPrice: String(relation.saturdayPrice ?? 0),
    sundayPrice: String(relation.sundayPrice ?? 0),
    extraAdultRate: String(relation.extraAdultRate ?? 0),
    extraChildRate: String(relation.extraChildRate ?? 0),
  }
  clearFutureOverridesOnSave.value = true
  editorOpen.value = true
}

function handleDismissEditor() {
  editorOpen.value = false
  editingRelationId.value = null
  relationForm.value = createEmptyRelationForm()
  clearFutureOverridesOnSave.value = true
}

function handleClearFutureOverridesChange(event: CustomEvent) {
  clearFutureOverridesOnSave.value = Boolean(event.detail.checked)
}

async function handleSaveRelation() {
  const userId = userStore.currentUser?.id
  if (!userId || !pricePlanId.value) {
    showWarningToast('请先恢复当前用户信息')
    return
  }

  if (!relationForm.value.roomTypeId) {
    showWarningToast('请选择房型')
    return
  }

  const maxGuests = Number(relationForm.value.maxGuests)
  if (!Number.isFinite(maxGuests) || maxGuests <= 0) {
    showWarningToast('请输入有效的最大入住人数')
    return
  }

  submitting.value = true
  try {
    const payload = {
      mondayPrice: parseOptionalNumber(relationForm.value.mondayPrice),
      tuesdayPrice: parseOptionalNumber(relationForm.value.tuesdayPrice),
      wednesdayPrice: parseOptionalNumber(relationForm.value.wednesdayPrice),
      thursdayPrice: parseOptionalNumber(relationForm.value.thursdayPrice),
      fridayPrice: parseOptionalNumber(relationForm.value.fridayPrice),
      saturdayPrice: parseOptionalNumber(relationForm.value.saturdayPrice),
      sundayPrice: parseOptionalNumber(relationForm.value.sundayPrice),
      maxGuests,
      includedGuests: parseOptionalNumber(relationForm.value.includedGuests),
      extraAdultRate: parseOptionalNumber(relationForm.value.extraAdultRate),
      extraChildRate: parseOptionalNumber(relationForm.value.extraChildRate),
      priceMode: relationForm.value.priceMode,
      clearFutureOverrides: editingRelationId.value ? clearFutureOverridesOnSave.value : undefined,
      clearFromDate: editingRelationId.value && clearFutureOverridesOnSave.value ? getTodayDate() : undefined,
    }

    if (editingRelationId.value) {
      const response = await updateRoomTypePricePlan(editingRelationId.value, userId, payload)
      if (!response.success) {
        throw new Error(response.message || '更新房型价格失败')
      }
      if (clearFutureOverridesOnSave.value) {
        showSuccessToast('房型价格已更新，已清理未来按日期覆盖价')
      } else {
        showSuccessToast('房型价格已更新，保留未来按日期覆盖价')
      }
    } else {
      const response = await assignPricePlanToRoomType(
        relationForm.value.roomTypeId,
        pricePlanId.value,
        userId,
        payload,
      )
      if (!response.success) {
        throw new Error(response.message || '新增房型价格失败')
      }
      showSuccessToast('房型价格已新增')
    }

    handleDismissEditor()
    await loadPageData()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, '保存房型价格失败'))
    }
  } finally {
    submitting.value = false
  }
}

async function handleDeleteRelation(relation: RoomTypePricePlanDTO) {
  const userId = userStore.currentUser?.id
  const relationId = relation.id
  if (!userId || !relationId) {
    return
  }

  const clearOverrides = await confirmDelete(relation.roomType?.name || '该房型')
  if (clearOverrides === null) {
    return
  }

  try {
    const response = await deleteRoomTypePricePlan(relationId, userId, clearOverrides)
    if (!response.success) {
      throw new Error(response.message || '删除房型价格失败')
    }
    if (response.message) {
      showSuccessToast(response.message)
    } else if (clearOverrides) {
      showSuccessToast('房型价格已删除，已清理按日期覆盖价')
    } else {
      showSuccessToast('房型价格已解绑，保留按日期覆盖价')
    }
    await loadPageData()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, '删除房型价格失败'))
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
.settings-price-plan-rates-page {
  display: block;
}

.settings-price-plan-rates-hero {
  margin-top: 4px;
}

.settings-price-plan-rates-hero__eyebrow {
  color: var(--ion-color-primary);
  font-weight: 700;
}

.settings-price-plan-rates-page__section-header {
  align-items: flex-start;
}

.settings-price-plan-rates-page__empty-state {
  padding-top: 16px;
}

.settings-price-plan-rates-empty-state {
  display: grid;
  gap: 10px;
  padding-top: 16px;
}

.settings-price-plan-rates-empty-state strong,
.settings-price-plan-rates-empty-state p {
  margin: 0;
}

.settings-price-plan-rates-empty-state p {
  color: var(--app-muted);
  font-size: 13px;
  line-height: 1.6;
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

.settings-price-plan-rates-page__toggle-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 14px;
  border-radius: 18px;
  background: var(--app-primary-soft);
}

.settings-price-plan-rates-page__toggle-card strong,
.settings-price-plan-rates-page__toggle-card p {
  margin: 0;
}

.settings-price-plan-rates-page__toggle-card p {
  margin-top: 6px;
  color: var(--app-muted);
  font-size: 12px;
}
</style>
