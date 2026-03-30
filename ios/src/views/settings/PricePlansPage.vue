<template>
  <ion-page>
    <ion-header translucent>
      <ion-toolbar>
        <ion-buttons slot="start">
          <ion-back-button :default-href="ROUTE_PATHS.settings" />
        </ion-buttons>
        <ion-title>价格计划</ion-title>
        <ion-buttons slot="end">
          <ion-button @click="handleCreatePlan">新增</ion-button>
        </ion-buttons>
      </ion-toolbar>
    </ion-header>

    <ion-content fullscreen class="mobile-page settings-price-plans-page">
      <ion-refresher slot="fixed" @ionRefresh="handleRefresh">
        <ion-refresher-content pulling-text="下拉刷新价格计划" refreshing-spinner="crescent" />
      </ion-refresher>

      <section class="mobile-hero settings-price-plans-hero">
        <p class="mobile-note settings-price-plans-hero__eyebrow">住宿价格</p>
        <h1 class="mobile-title">价格计划管理</h1>
        <p class="mobile-subtitle">支持价格计划本体管理，并在空数据下引导继续进入房型价格配置。</p>
        <div class="mobile-chip-row">
          <span class="mobile-chip">计划 {{ plans.length }}</span>
          <span class="mobile-chip">继续保留 `userId` 契约</span>
        </div>
      </section>

      <div class="mobile-stack">
        <section class="mobile-card">
          <div class="mobile-inline-row settings-price-plans-page__section-header">
            <div>
              <h2 class="mobile-section-title">价格计划列表</h2>
              <p class="mobile-note">支持新增、编辑、删除，并进入对应房型价格页。</p>
            </div>
            <ion-spinner v-if="loading" name="crescent" />
          </div>

          <div v-if="plans.length > 0" class="mobile-list settings-price-plans-list">
            <article v-for="plan in plans" :key="plan.id" class="settings-price-plan-card">
              <div class="settings-price-plan-card__header">
                <div>
                  <strong>{{ plan.name }}</strong>
                  <p>{{ plan.description || '未填写计划描述' }}</p>
                </div>
                <span class="settings-price-plan-card__badge">{{ plan.includeMeal ? '含餐' : '不含餐' }}</span>
              </div>

              <div class="settings-price-plan-card__meta">
                <span>最少入住 {{ plan.minNights }} 晚</span>
                <span>最多入住 {{ plan.maxNights || '-' }} 晚</span>
                <span>已关联房型 {{ plan.roomTypeCount }}</span>
              </div>

              <div class="settings-price-plan-card__actions">
                <ion-button size="small" @click="handleOpenRates(plan)">房型价格</ion-button>
                <ion-button size="small" fill="outline" @click="handleEditPlan(plan)">编辑</ion-button>
                <ion-button size="small" color="danger" fill="clear" @click="handleDeletePlan(plan)">
                  删除
                </ion-button>
              </div>
            </article>
          </div>

          <div v-else-if="!loading" class="settings-price-plans-empty-state">
            <strong>当前暂无价格计划</strong>
            <p>请先创建一个价格计划，随后即可继续配置房型价格。</p>
            <ion-button @click="handleCreatePlan">创建价格计划</ion-button>
          </div>
        </section>

        <section class="mobile-card">
          <h2 class="mobile-section-title">首版说明</h2>
          <ul class="mobile-bullet-list">
            <li>已覆盖价格计划本体管理与房型价格入口。</li>
            <li>删除失败且被渠道价格阻塞时，会继续提供强制删除补救路径。</li>
            <li>复杂派生规则与营销详情编辑暂未在移动端完整迁移。</li>
          </ul>
        </section>
      </div>

      <ion-modal :is-open="editorOpen" @didDismiss="handleDismissEditor">
        <ion-header>
          <ion-toolbar>
            <ion-title>{{ editingPlanId ? '编辑价格计划' : '新增价格计划' }}</ion-title>
            <ion-buttons slot="end">
              <ion-button @click="handleDismissEditor">关闭</ion-button>
            </ion-buttons>
          </ion-toolbar>
        </ion-header>

        <ion-content class="mobile-page settings-modal-page">
          <section class="mobile-card">
            <div class="settings-form-grid">
              <label class="settings-form-field">
                <span>计划名称</span>
                <ion-input v-model="planForm.name" fill="outline" placeholder="请输入价格计划名称" />
              </label>

              <label class="settings-form-field">
                <span>英文名称</span>
                <ion-input v-model="planForm.nameEn" fill="outline" placeholder="可选" />
              </label>

              <label class="settings-form-field">
                <span>最少入住晚数</span>
                <ion-input v-model="planForm.minNights" fill="outline" inputmode="numeric" placeholder="1" />
              </label>

              <label class="settings-form-field">
                <span>最多入住晚数</span>
                <ion-input v-model="planForm.maxNights" fill="outline" inputmode="numeric" placeholder="可留空" />
              </label>

              <label class="settings-form-field">
                <span>衍生类型</span>
                <ion-select v-model="planForm.derivationType" fill="outline" interface="action-sheet">
                  <ion-select-option value="independent">independent</ion-select-option>
                  <ion-select-option value="derived">derived</ion-select-option>
                </ion-select>
              </label>

              <label v-if="planForm.derivationType === 'derived'" class="settings-form-field">
                <span>基础计划</span>
                <ion-select v-model="planForm.basePlanId" fill="outline" interface="modal">
                  <ion-select-option v-for="plan in availableBasePlans" :key="plan.id" :value="plan.id">
                    {{ plan.name }}
                  </ion-select-option>
                </ion-select>
              </label>

              <div class="settings-toggle-field">
                <div>
                  <strong>含餐</strong>
                  <p>保存当前价格计划是否默认包含早餐。</p>
                </div>
                <ion-toggle v-model="planForm.includeMeal" />
              </div>

              <label class="settings-form-field settings-form-field--full">
                <span>描述</span>
                <ion-textarea v-model="planForm.description" :rows="4" fill="outline" placeholder="可填写计划说明" />
              </label>

              <label class="settings-form-field settings-form-field--full">
                <span>英文描述</span>
                <ion-textarea v-model="planForm.descriptionEn" :rows="4" fill="outline" placeholder="可选" />
              </label>

              <label v-if="planForm.derivationType === 'derived'" class="settings-form-field settings-form-field--full">
                <span>派生规则</span>
                <ion-textarea v-model="planForm.derivationRule" :rows="3" fill="outline" placeholder="请输入派生规则摘要" />
              </label>

              <label class="settings-form-field settings-form-field--full">
                <span>取消政策</span>
                <ion-textarea v-model="planForm.cancellationPolicy" :rows="4" fill="outline" placeholder="可填写取消政策摘要" />
              </label>

              <label class="settings-form-field settings-form-field--full">
                <span>英文取消政策</span>
                <ion-textarea v-model="planForm.cancellationPolicyEn" :rows="4" fill="outline" placeholder="可选" />
              </label>
            </div>

            <div class="settings-form-actions">
              <ion-button fill="outline" @click="handleDismissEditor">取消</ion-button>
              <ion-button :disabled="submitting" @click="handleSavePlan">
                {{ submitting ? '提交中...' : '保存价格计划' }}
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
  IonModal,
  IonPage,
  IonRefresher,
  IonRefresherContent,
  IonSelect,
  IonSelectOption,
  IonSpinner,
  IonTextarea,
  IonTitle,
  IonToggle,
  IonToolbar,
  onIonViewWillEnter,
} from '@ionic/vue'
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
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

async function confirmAction(header: string, message: string, destructive = false, confirmText = '确认') {
  const alert = await alertController.create({
    header,
    message,
    buttons: [
      {
        text: '取消',
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
    showWarningToast('请先恢复当前用户信息')
    return
  }

  loading.value = true
  try {
    const response = await getAllPricePlans(userId)
    if (!response.success || !response.data) {
      throw new Error(response.message || '加载价格计划失败')
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
      showWarningToast(resolveWarningMessage(error, '加载价格计划失败'))
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
    showWarningToast('请先恢复当前用户信息')
    return
  }

  if (!planForm.value.name.trim()) {
    showWarningToast('请输入价格计划名称')
    return
  }

  const minNights = Number(planForm.value.minNights)
  if (!Number.isFinite(minNights) || minNights <= 0) {
    showWarningToast('请输入有效的最少入住晚数')
    return
  }

  let maxNights: number | undefined
  if (planForm.value.maxNights.trim()) {
    maxNights = Number(planForm.value.maxNights)
    if (!Number.isFinite(maxNights) || maxNights <= 0) {
      showWarningToast('请输入有效的最多入住晚数')
      return
    }
  }

  if (planForm.value.derivationType === 'derived' && !planForm.value.basePlanId) {
    showWarningToast('请选择基础计划')
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
        throw new Error(response.message || '更新价格计划失败')
      }
      showSuccessToast('价格计划已更新')
    } else {
      const response = await createPricePlan(userId, payload)
      if (!response.success) {
        throw new Error(response.message || '创建价格计划失败')
      }
      const createdPlanId = response.data?.id ? Number(response.data.id) : 0
      showSuccessToast(createdPlanId ? '价格计划已创建，正在进入房型价格配置' : '价格计划已创建')
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
      showWarningToast(resolveWarningMessage(error, '保存价格计划失败'))
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

  const confirmed = await confirmAction('删除价格计划', `确认删除 ${plan.name} 吗？`, true, '确认删除')
  if (!confirmed) {
    return
  }

  try {
    const response = await deletePricePlan(plan.id, userId)
    if (!response.success) {
      throw new Error(response.message || '删除价格计划失败')
    }
    showSuccessToast('价格计划已删除')
    await loadPlans()
  } catch (error) {
    const message = resolveWarningMessage(error, '删除价格计划失败')
    if (message.includes('渠道价格记录') || message.includes('channel_prices')) {
      const forceConfirmed = await confirmAction(
        '彻底删除价格计划',
        '当前计划存在渠道价格记录阻塞，是否继续执行彻底删除？',
        true,
        '彻底删除',
      )

      if (!forceConfirmed) {
        return
      }

      try {
        const forceResponse = await forceDeletePricePlan(plan.id, userId)
        if (!forceResponse.success) {
          throw new Error(forceResponse.message || '彻底删除失败')
        }
        showSuccessToast('价格计划已彻底删除')
        await loadPlans()
      } catch (forceError) {
        if (!isHandledRequestError(forceError)) {
          showWarningToast(resolveWarningMessage(forceError, '彻底删除失败'))
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
.settings-price-plans-page {
  display: block;
}

.settings-price-plans-hero {
  margin-top: 4px;
}

.settings-price-plans-hero__eyebrow {
  color: var(--ion-color-primary);
  font-weight: 700;
}

.settings-price-plans-page__section-header {
  align-items: flex-start;
}

.settings-price-plans-list {
  margin-top: 16px;
}

.settings-price-plan-card {
  padding: 14px;
  border-radius: 18px;
  border: 1px solid var(--app-border);
  background: rgba(255, 255, 255, 0.82);
}

.settings-price-plan-card__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.settings-price-plan-card__header strong,
.settings-price-plan-card__header p {
  margin: 0;
}

.settings-price-plan-card__header p {
  margin-top: 6px;
  color: var(--app-muted);
  font-size: 13px;
}

.settings-price-plan-card__badge {
  padding: 4px 10px;
  border-radius: 999px;
  background: var(--app-primary-soft);
  color: var(--ion-color-primary);
  font-size: 12px;
  font-weight: 600;
}

.settings-price-plan-card__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 14px;
  margin-top: 12px;
  color: var(--app-muted);
  font-size: 12px;
}

.settings-price-plan-card__actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 14px;
}

.settings-price-plans-page__empty-state {
  padding-top: 16px;
}

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

.settings-form-field--full {
  grid-column: 1 / -1;
}

.settings-toggle-field {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 14px;
  border-radius: 18px;
  background: var(--app-primary-soft);
}

.settings-toggle-field strong,
.settings-toggle-field p {
  margin: 0;
}

.settings-toggle-field p {
  margin-top: 6px;
  color: var(--app-muted);
  font-size: 12px;
}

.settings-form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  flex-wrap: wrap;
  margin-top: 18px;
}
</style>
