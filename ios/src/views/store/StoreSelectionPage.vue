<template>
  <ion-page>
    <ion-header translucent>
      <ion-toolbar class="app-page-header__toolbar">
        <ion-title class="app-page-header__title">选择门店</ion-title>
        <ion-buttons slot="end">
          <ion-button class="app-page-header__text-btn" fill="clear" :disabled="isLoggingOut" @click="handleLogout">
            <ion-icon slot="start" :icon="logOutOutline" />
            <span>退出</span>
          </ion-button>
        </ion-buttons>
      </ion-toolbar>
    </ion-header>

    <ion-content fullscreen class="mobile-page store-page">
      <ion-refresher slot="fixed" @ionRefresh="handlePullToRefresh">
        <ion-refresher-content pulling-text="下拉刷新门店" refreshing-spinner="crescent" />
      </ion-refresher>

      <section class="mobile-hero store-hero">
        <p class="mobile-note store-hero__eyebrow">当前账号</p>
        <h1 class="mobile-title">{{ accountTitle }}</h1>
        <p class="mobile-subtitle">
          {{ accountSubtitle }} · {{ storeCountLabel }}
        </p>
      </section>

      <div class="mobile-stack">
        <section class="mobile-card store-toolbar-card">
          <div class="mobile-inline-row store-toolbar-card__header">
            <div>
              <h2 class="mobile-section-title">门店列表</h2>
            </div>
            <div class="store-toolbar-card__actions">
              <ion-button class="store-create-button" size="small" @click="handleOpenCreateStoreModal">
                <ion-icon slot="start" :icon="addCircleOutline" />
                <span>创建门店</span>
              </ion-button>
              <ion-button fill="clear" size="small" :disabled="storeStore.loading" @click="handleRefreshButton">
                <ion-icon slot="icon-only" :icon="refreshOutline" />
              </ion-button>
            </div>
          </div>

          <ion-searchbar
            v-model="searchKeyword"
            animated
            class="store-searchbar custom-searchbar"
            placeholder="搜索门店、城市或国家"
            show-clear-button="focus"
          />

          <div v-if="showInlineError" class="store-inline-state store-inline-state--danger">
            <ion-icon :icon="alertCircleOutline" />
            <span>{{ loadError }}</span>
          </div>

          <div v-if="showInitialLoading" class="mobile-list">
            <div v-for="index in 3" :key="index" class="store-skeleton-card">
              <ion-skeleton-text animated class="store-skeleton-card__title" />
              <ion-skeleton-text animated class="store-skeleton-card__line" />
              <ion-skeleton-text animated class="store-skeleton-card__line store-skeleton-card__line--short" />
            </div>
          </div>

          <div v-else-if="showLoadErrorState" class="store-empty-state">
            <ion-icon :icon="alertCircleOutline" class="store-empty-state__icon" />
            <h3 class="store-empty-state__title">门店列表加载失败</h3>
            <p class="mobile-note">{{ loadError }}</p>
            <div class="store-empty-state__actions">
              <ion-button expand="block" class="primary-action-btn" @click="handleOpenCreateStoreModal">创建门店</ion-button>
              <ion-button expand="block" fill="outline" @click="handleRefreshButton">重新加载</ion-button>
            </div>
          </div>

          <div v-else-if="showEmptyState" class="store-empty-state">
            <ion-icon :icon="storefrontOutline" class="store-empty-state__icon" />
            <h3 class="store-empty-state__title">还没有门店</h3>
            <p class="mobile-note">现在即可在移动端创建首家门店，创建成功后会自动进入新门店首页。</p>
            <div class="store-empty-state__actions">
              <ion-button expand="block" class="primary-action-btn" @click="handleOpenCreateStoreModal">创建门店</ion-button>
              <ion-button expand="block" fill="outline" @click="handleRefreshButton">重新加载</ion-button>
            </div>
          </div>

          <div v-else-if="showSearchEmptyState" class="store-empty-state store-empty-state--compact">
            <ion-icon :icon="searchOutline" class="store-empty-state__icon" />
            <h3 class="store-empty-state__title">未找到匹配门店</h3>
            <p class="mobile-note">请尝试更换关键词，或清空搜索后查看全部门店。</p>
          </div>

          <div v-else class="mobile-list store-list">
            <ion-card v-for="store in filteredStores" :key="store.id" class="store-card">
              <ion-card-header>
                <div class="store-card__header-row">
                  <div>
                    <ion-card-title>{{ store.name }}</ion-card-title>
                    <ion-card-subtitle>{{ formatLocation(store) }}</ion-card-subtitle>
                  </div>
                  <ion-chip color="primary" outline>
                    <ion-label>{{ formatRoleLabel(store.userRole) }}</ion-label>
                  </ion-chip>
                </div>
              </ion-card-header>

              <ion-card-content>
                <div class="store-card__detail-row">
                  <span class="store-card__detail-label">联系人</span>
                  <span class="store-card__detail-value">{{ store.manager || '未设置' }}</span>
                </div>
                <div class="store-card__detail-row">
                  <span class="store-card__detail-label">更新时间</span>
                  <span class="store-card__detail-value">{{ formatDate(store.updatedAt) }}</span>
                </div>
                <div class="store-card__detail-row">
                  <span class="store-card__detail-label">地址</span>
                  <span class="store-card__detail-value">{{ formatAddress(store) }}</span>
                </div>

                <ion-button
                  expand="block"
                  class="store-card__button"
                  :disabled="selectingStoreId === store.id || deletingStoreId === store.id"
                  @click="handleSelectStore(store)"
                >
                  <ion-spinner v-if="selectingStoreId === store.id" name="crescent" />
                  <span v-else>进入该门店</span>
                </ion-button>

                <ion-button
                  v-if="store.userRole === 'owner'"
                  expand="block"
                  fill="outline"
                  color="danger"
                  class="store-card__button store-card__button--danger"
                  :disabled="selectingStoreId === store.id || deletingStoreId === store.id"
                  @click="handleDeleteStore(store)"
                >
                  <ion-spinner v-if="deletingStoreId === store.id" name="crescent" />
                  <span v-else>删除门店</span>
                </ion-button>
              </ion-card-content>
            </ion-card>
          </div>
        </section>
      </div>
    </ion-content>

    <ion-modal :is-open="createStoreModalOpen" :backdrop-dismiss="!creatingStore" @didDismiss="handleDismissCreateStoreModal">
      <ion-header>
        <ion-toolbar>
          <ion-title>创建门店</ion-title>
          <ion-buttons slot="end">
            <ion-button :disabled="creatingStore" @click="handleCloseCreateStoreModal">关闭</ion-button>
          </ion-buttons>
        </ion-toolbar>
      </ion-header>

      <ion-content class="mobile-page store-modal-page">
        <section class="mobile-card">
          <div class="store-create-panel">
            <h2 class="mobile-section-title">基础信息</h2>
            <p class="mobile-note">创建成功后会刷新门店列表、自动选中新门店并进入首页。</p>
          </div>

          <div class="store-form-grid">
            <label class="store-form-field">
              <span>门店名称</span>
              <ion-input
                v-model="createForm.name"
                fill="outline"
                :maxlength="STORE_NAME_MAX_LENGTH"
                placeholder="请输入门店名称"
              />
            </label>

            <label class="store-form-field">
              <span>房产类型</span>
              <ion-select v-model="createForm.type" fill="outline" interface="action-sheet" placeholder="请选择房产类型">
                <ion-select-option v-for="option in PROPERTY_TYPE_OPTIONS" :key="option.value" :value="option.value">
                  {{ option.label }}
                </ion-select-option>
              </ion-select>
            </label>

            <label class="store-form-field">
              <span>国家</span>
              <ion-select
                v-model="createForm.country"
                fill="outline"
                interface="action-sheet"
                placeholder="请选择国家"
              >
                <ion-select-option v-for="option in COUNTRY_OPTIONS" :key="option.value" :value="option.value">
                  {{ option.label }}
                </ion-select-option>
              </ion-select>
            </label>

            <label class="store-form-field">
              <span>城市</span>
              <ion-input v-model="createForm.city" fill="outline" placeholder="请输入城市" />
            </label>

            <label class="store-form-field store-form-field--full">
              <span>详细地址</span>
              <ion-textarea v-model="createForm.address" :rows="3" fill="outline" placeholder="请输入详细地址" />
            </label>

            <label class="store-form-field store-form-field--full">
              <span>联系电话</span>
              <div class="store-phone-row">
                <ion-select v-model="createForm.phonePrefix" class="store-phone-prefix" fill="outline" interface="action-sheet">
                  <ion-select-option v-for="option in PHONE_PREFIX_OPTIONS" :key="option.value" :value="option.value">
                    {{ option.label }}
                  </ion-select-option>
                </ion-select>
                <ion-input
                  v-model="createForm.phone"
                  class="store-phone-input"
                  fill="outline"
                  inputmode="tel"
                  placeholder="请输入联系电话"
                />
              </div>
            </label>

            <label class="store-form-field">
              <span>联系人姓名</span>
              <ion-input v-model="createForm.manager" fill="outline" placeholder="请输入联系人姓名" />
            </label>

            <label class="store-form-field">
              <span>时区</span>
              <ion-select v-model="createForm.timezone" fill="outline" interface="action-sheet" placeholder="请选择时区">
                <ion-select-option v-for="option in TIMEZONE_OPTIONS" :key="option.value" :value="option.value">
                  {{ option.label }}
                </ion-select-option>
              </ion-select>
            </label>

            <label class="store-form-field">
              <span>货币</span>
              <ion-select v-model="createForm.currency" fill="outline" interface="action-sheet" placeholder="请选择货币">
                <ion-select-option v-for="option in CURRENCY_OPTIONS" :key="option.value" :value="option.value">
                  {{ option.label }}
                </ion-select-option>
              </ion-select>
            </label>

            <label class="store-form-field">
              <span>语言</span>
              <ion-select v-model="createForm.language" fill="outline" interface="action-sheet" placeholder="请选择语言">
                <ion-select-option v-for="option in LANGUAGE_OPTIONS" :key="option.value" :value="option.value">
                  {{ option.label }}
                </ion-select-option>
              </ion-select>
            </label>

            <label class="store-form-field">
              <span>入住时间</span>
              <ion-select
                v-model="createForm.checkinTime"
                fill="outline"
                interface="action-sheet"
                placeholder="请选择入住时间"
              >
                <ion-select-option v-for="option in STORE_TIME_OPTIONS" :key="option.value" :value="option.value">
                  {{ option.label }}
                </ion-select-option>
              </ion-select>
            </label>

            <label class="store-form-field">
              <span>退房时间</span>
              <ion-select
                v-model="createForm.checkoutTime"
                fill="outline"
                interface="action-sheet"
                placeholder="请选择退房时间"
              >
                <ion-select-option v-for="option in STORE_TIME_OPTIONS" :key="option.value" :value="option.value">
                  {{ option.label }}
                </ion-select-option>
              </ion-select>
            </label>

            <label class="store-form-field store-form-field--full">
              <span>渠道直连</span>
              <div class="store-switch-row">
                <div>
                  <strong>同步创建渠道物业</strong>
                  <p class="mobile-note store-switch-row__note">创建门店后同步创建/覆盖渠道物业（用于后续打开授权 Widget）</p>
                </div>
                <ion-toggle v-model="createForm.createSuProperty" />
              </div>
            </label>
          </div>

          <div class="store-form-actions">
            <ion-button fill="outline" :disabled="creatingStore" @click="handleCloseCreateStoreModal">取消</ion-button>
            <ion-button :disabled="creatingStore" @click="handleCreateStore">
              {{ creatingStore ? '创建中...' : '创建门店' }}
            </ion-button>
          </div>
        </section>
      </ion-content>
    </ion-modal>
  </ion-page>
</template>

<script setup lang="ts">
import {
  alertController,
  IonButton,
  IonButtons,
  IonCard,
  IonCardContent,
  IonCardHeader,
  IonCardSubtitle,
  IonCardTitle,
  IonChip,
  IonContent,
  IonHeader,
  IonIcon,
  IonInput,
  IonLabel,
  IonModal,
  IonPage,
  IonRefresher,
  IonRefresherContent,
  IonSearchbar,
  IonSelect,
  IonSelectOption,
  IonSkeletonText,
  IonSpinner,
  IonTextarea,
  IonTitle,
  IonToggle,
  IonToolbar,
} from '@ionic/vue'
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  addCircleOutline,
  alertCircleOutline,
  logOutOutline,
  refreshOutline,
  searchOutline,
  storefrontOutline,
} from 'ionicons/icons'
import {
  COUNTRY_OPTIONS,
  CREATE_STORE_DEFAULTS,
  CURRENCY_OPTIONS,
  LANGUAGE_OPTIONS,
  PHONE_PREFIX_OPTIONS,
  PROPERTY_TYPE_OPTIONS,
  STORE_NAME_MAX_LENGTH,
  STORE_PHONE_MAX_LENGTH,
  STORE_PHONE_MIN_LENGTH,
  STORE_TIME_OPTIONS,
  TIMEZONE_OPTIONS,
} from '@/constants/store'
import { ROUTE_PATHS } from '@/router/guards'
import { useStoreStore } from '@/stores/store'
import { useUserStore } from '@/stores/user'
import type { StoreDTO, StoreRequest } from '@/types/store'
import { showSuccessToast, showWarningToast } from '@/utils/notify'
import { isHandledRequestError } from '@/utils/request'

interface CreateStoreFormState {
  name: string
  type: string
  country: string
  city: string
  address: string
  manager: string
  phonePrefix: string
  phone: string
  timezone: string
  currency: string
  language: string
  checkinTime: string
  checkoutTime: string
  createSuProperty: boolean
}

const ROLE_LABELS: Record<string, string> = {
  owner: '所有者',
  admin: '管理员',
  member: '成员',
}

const PHONE_NUMBER_PATTERN = /^\d+$/

const router = useRouter()
const storeStore = useStoreStore()
const userStore = useUserStore()

const searchKeyword = ref('')
const loadError = ref('')
const hasLoaded = ref(false)
const selectingStoreId = ref<number | null>(null)
const deletingStoreId = ref<number | null>(null)
const isLoggingOut = ref(false)
const createStoreModalOpen = ref(false)
const creatingStore = ref(false)
const createForm = ref<CreateStoreFormState>(createCreateStoreForm())

const accountTitle = computed(() => {
  if (userStore.currentUser?.nickname) {
    return userStore.currentUser.nickname
  }

  if (userStore.currentUser?.email) {
    return userStore.currentUser.email
  }

  return '请选择要进入的门店'
})

const accountSubtitle = computed(() => {
  if (userStore.currentUser?.email) {
    return userStore.currentUser.email
  }

  return '已完成登录'
})

const storeCountLabel = computed(() => {
  return `共 ${storeStore.stores.length} 家门店`
})

const filteredStores = computed(() => {
  const keyword = searchKeyword.value.trim().toLowerCase()

  if (!keyword) {
    return storeStore.stores
  }

  return storeStore.stores.filter((store) => {
    const searchPool = [store.name, store.city, store.country]

    return searchPool.some((value) => value && value.toLowerCase().includes(keyword))
  })
})

const showInitialLoading = computed(() => {
  return storeStore.loading && storeStore.stores.length === 0 && !hasLoaded.value
})

const showLoadErrorState = computed(() => {
  return !storeStore.loading && Boolean(loadError.value) && storeStore.stores.length === 0
})

const showInlineError = computed(() => {
  return !storeStore.loading && Boolean(loadError.value) && storeStore.stores.length > 0
})

const showEmptyState = computed(() => {
  return !storeStore.loading && !loadError.value && hasLoaded.value && storeStore.stores.length === 0
})

const showSearchEmptyState = computed(() => {
  return !storeStore.loading && storeStore.stores.length > 0 && filteredStores.value.length === 0
})

function createCreateStoreForm(): CreateStoreFormState {
  return {
    name: '',
    type: CREATE_STORE_DEFAULTS.type ?? '1',
    country: CREATE_STORE_DEFAULTS.country ?? 'China',
    city: '',
    address: '',
    manager: '',
    phonePrefix: PHONE_PREFIX_OPTIONS[0]?.value ?? '+86',
    phone: '',
    timezone: CREATE_STORE_DEFAULTS.timezone ?? 'Asia/Shanghai',
    currency: CREATE_STORE_DEFAULTS.currency ?? 'CNY',
    language: CREATE_STORE_DEFAULTS.language ?? 'en',
    checkinTime: CREATE_STORE_DEFAULTS.checkinTime ?? '15:00',
    checkoutTime: CREATE_STORE_DEFAULTS.checkoutTime ?? '11:00',
    createSuProperty: CREATE_STORE_DEFAULTS.createSuProperty ?? true,
  }
}

function resetCreateStoreForm() {
  createForm.value = createCreateStoreForm()
}

function normalizePhoneNumber(value: string) {
  return value.replace(/[\s-]+/g, '')
}

function buildCreateStorePayload(): StoreRequest {
  return {
    name: createForm.value.name.trim(),
    type: createForm.value.type,
    country: createForm.value.country.trim(),
    city: createForm.value.city.trim(),
    address: createForm.value.address.trim(),
    manager: createForm.value.manager.trim(),
    phoneTechType: CREATE_STORE_DEFAULTS.phoneTechType,
    phone: `${createForm.value.phonePrefix} ${normalizePhoneNumber(createForm.value.phone)}`,
    timezone: createForm.value.timezone,
    currency: createForm.value.currency,
    language: createForm.value.language,
    checkinTime: createForm.value.checkinTime,
    checkoutTime: createForm.value.checkoutTime,
    createSuProperty: createForm.value.createSuProperty,
  }
}

function validateCreateStoreForm() {
  const name = createForm.value.name.trim()
  const type = createForm.value.type
  const country = createForm.value.country.trim()
  const city = createForm.value.city.trim()
  const address = createForm.value.address.trim()
  const manager = createForm.value.manager.trim()
  const phone = normalizePhoneNumber(createForm.value.phone)
  const timezone = createForm.value.timezone
  const currency = createForm.value.currency
  const language = createForm.value.language
  const checkinTime = createForm.value.checkinTime
  const checkoutTime = createForm.value.checkoutTime

  if (!name) {
    showWarningToast('请输入门店名称')
    return false
  }

  if (name.length > STORE_NAME_MAX_LENGTH) {
    showWarningToast(`门店名称长度不能超过 ${STORE_NAME_MAX_LENGTH} 个字符`)
    return false
  }

  if (!type) {
    showWarningToast('请选择房产类型')
    return false
  }

  if (!country) {
    showWarningToast('请选择国家')
    return false
  }

  if (!city) {
    showWarningToast('请输入城市')
    return false
  }

  if (!address) {
    showWarningToast('请输入详细地址')
    return false
  }

  if (!manager) {
    showWarningToast('请输入联系人姓名')
    return false
  }

  if (!createForm.value.phonePrefix) {
    showWarningToast('请选择电话区号')
    return false
  }

  if (!phone) {
    showWarningToast('请输入联系电话')
    return false
  }

  if (!PHONE_NUMBER_PATTERN.test(phone)) {
    showWarningToast('联系电话仅支持数字、空格或短横线')
    return false
  }

  if (phone.length < STORE_PHONE_MIN_LENGTH || phone.length > STORE_PHONE_MAX_LENGTH) {
    showWarningToast(`联系电话长度需为 ${STORE_PHONE_MIN_LENGTH}-${STORE_PHONE_MAX_LENGTH} 位`)
    return false
  }

  if (!timezone) {
    showWarningToast('请选择时区')
    return false
  }

  if (!currency) {
    showWarningToast('请选择货币')
    return false
  }

  if (!language) {
    showWarningToast('请选择语言')
    return false
  }

  if (!checkinTime) {
    showWarningToast('请选择入住时间')
    return false
  }

  if (!checkoutTime) {
    showWarningToast('请选择退房时间')
    return false
  }

  return true
}

function resolveErrorMessage(error: unknown, fallbackMessage: string) {
  if (error instanceof Error && error.message) {
    return error.message
  }

  return fallbackMessage
}

function resolveDeleteStoreCode(error: unknown) {
  if (!error || typeof error !== 'object') {
    return ''
  }

  const code = Reflect.get(error, 'code')
  if (typeof code === 'string' || typeof code === 'number') {
    return String(code)
  }

  if (error instanceof Error && error.message.includes('953')) {
    return '953'
  }

  return ''
}

function formatRoleLabel(role: string) {
  if (ROLE_LABELS[role]) {
    return ROLE_LABELS[role]
  }

  if (!role) {
    return '未标注角色'
  }

  return role
}

function formatLocation(store: StoreDTO) {
  const parts = [store.city, store.country].filter(Boolean)

  if (parts.length === 0) {
    return '未设置地区'
  }

  return parts.join(' · ')
}

function formatAddress(store: StoreDTO) {
  const parts = [store.address, store.city, store.country].filter(Boolean)

  if (parts.length === 0) {
    return '未设置地址'
  }

  return parts.join(' · ')
}

function formatDate(rawValue: string) {
  if (!rawValue) {
    return '未知'
  }

  const date = new Date(rawValue)
  if (Number.isNaN(date.getTime())) {
    return rawValue
  }

  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')

  return `${year}-${month}-${day}`
}

async function loadStores(force: boolean) {
  loadError.value = ''

  try {
    await storeStore.fetchUserStores(force)
  } catch (error) {
    loadError.value = resolveErrorMessage(error, '获取门店列表失败')

    if (!isHandledRequestError(error)) {
      showWarningToast(loadError.value)
    }
  } finally {
    hasLoaded.value = true
  }
}

async function confirmDeleteStore(store: StoreDTO) {
  const alert = await alertController.create({
    header: '删除门店',
    message: `确认删除 ${store.name} 吗？删除后该门店将从列表中移除。`,
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

async function showDeleteStoreBlockedAlert() {
  const alert = await alertController.create({
    header: '无法删除门店',
    message: '该 Property 仍与渠道存在映射，请先解绑后再删除门店。',
    buttons: ['知道了'],
  })

  await alert.present()
}

async function handleRefreshButton() {
  await loadStores(true)
}

async function handlePullToRefresh(event: CustomEvent) {
  await loadStores(true)
  event.detail.complete()
}

function handleOpenCreateStoreModal() {
  resetCreateStoreForm()
  createStoreModalOpen.value = true
}

function handleCloseCreateStoreModal() {
  if (creatingStore.value) {
    return
  }

  createStoreModalOpen.value = false
}

function handleDismissCreateStoreModal() {
  if (creatingStore.value) {
    return
  }

  createStoreModalOpen.value = false
  resetCreateStoreForm()
}

async function handleCreateStore() {
  if (creatingStore.value) {
    return
  }

  if (!validateCreateStoreForm()) {
    return
  }

  let createdStore: StoreDTO | null = null
  let successMessage = '门店创建成功'

  creatingStore.value = true

  try {
    const result = await storeStore.createStore(buildCreateStorePayload())
    createdStore = result.store
    successMessage = result.message || '门店创建成功'
    createStoreModalOpen.value = false
    loadError.value = ''
    hasLoaded.value = true
    searchKeyword.value = ''
    resetCreateStoreForm()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveErrorMessage(error, '创建门店失败'))
    }
    return
  } finally {
    creatingStore.value = false
  }

  if (successMessage.includes('失败')) {
    showWarningToast(successMessage)
  } else {
    showSuccessToast(successMessage)
  }

  try {
    await router.replace(ROUTE_PATHS.home)
  } catch (error) {
    if (!isHandledRequestError(error)) {
      const fallbackMessage = createdStore ? `门店已创建，请手动进入 ${createdStore.name}` : '门店已创建，请手动进入首页'
      showWarningToast(resolveErrorMessage(error, fallbackMessage))
    }
  }
}

async function handleSelectStore(store: StoreDTO) {
  if (selectingStoreId.value !== null) {
    return
  }

  selectingStoreId.value = store.id

  try {
    storeStore.setCurrentStore(store)
    showSuccessToast(`已进入 ${store.name}`)
    await router.replace(ROUTE_PATHS.home)
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveErrorMessage(error, '进入门店失败'))
    }
  } finally {
    selectingStoreId.value = null
  }
}

async function handleDeleteStore(store: StoreDTO) {
  if (deletingStoreId.value !== null) {
    return
  }

  const confirmed = await confirmDeleteStore(store)
  if (!confirmed) {
    return
  }

  deletingStoreId.value = store.id

  try {
    const successMessage = await storeStore.deleteStore(store.id)
    await loadStores(true)
    showSuccessToast(successMessage || '门店已删除')
  } catch (error) {
    const errorCode = resolveDeleteStoreCode(error)

    if (errorCode === '953') {
      await showDeleteStoreBlockedAlert()
      return
    }

    if (!isHandledRequestError(error)) {
      showWarningToast(resolveErrorMessage(error, '删除门店失败'))
    }
  } finally {
    deletingStoreId.value = null
  }
}

async function handleLogout() {
  if (isLoggingOut.value) {
    return
  }

  isLoggingOut.value = true

  try {
    await userStore.logout()
    showSuccessToast('已退出登录')
    await router.replace(ROUTE_PATHS.login)
  } finally {
    isLoggingOut.value = false
  }
}

onMounted(() => {
  hasLoaded.value = storeStore.stores.length > 0

  if (!hasLoaded.value) {
    void loadStores(true)
  }
})
</script>

<style scoped>
.store-page {
  display: block;
  /* ion-header translucent + fullscreen already offsets safe-area;
     override mobile-page padding-top to avoid double-counting */
  --padding-top: 8px;
}

.store-hero {
  margin-top: 4px;
  margin-bottom: 16px;
  text-align: center;
}

.store-hero__eyebrow {
  color: var(--ion-color-primary);
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  font-size: 13px;
  margin-bottom: 8px;
}

.mobile-title {
  font-size: 30px;
  font-weight: 800;
  margin-bottom: 12px;
  color: var(--app-heading);
}

.mobile-subtitle {
  font-size: 15px;
  color: var(--app-muted);
  line-height: 1.6;
}

.store-toolbar-card {
  padding: 24px 20px;
  border-radius: 24px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.04);
  background: #ffffff;
  border: 1px solid rgba(0, 0, 0, 0.02);
}

.store-toolbar-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.store-toolbar-card__header h2 {
  margin: 0;
}

.store-toolbar-card__actions {
  display: flex;
  align-items: center;
  gap: 4px;
}

.store-create-button {
  --border-radius: 14px;
  font-weight: 600;
}

.custom-searchbar {
  --border-radius: 12px;
  --box-shadow: none;
  --background: #f8fafc;
  padding-inline: 0;
  margin-bottom: 16px;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
}

.store-inline-state {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
  padding: 12px 14px;
  border-radius: 16px;
  font-size: 13px;
}

.store-inline-state--danger {
  background: rgba(220, 38, 38, 0.08);
  color: var(--ion-color-danger);
}

.store-skeleton-card {
  padding: 18px;
  border: 1px solid var(--app-border);
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.76);
  margin-bottom: 12px;
}

.store-skeleton-card__title {
  width: 52%;
  height: 20px;
}

.store-skeleton-card__line {
  width: 88%;
  height: 14px;
  margin-top: 8px;
}

.store-skeleton-card__line--short {
  width: 66%;
}

.store-empty-state {
  display: grid;
  gap: 12px;
  justify-items: center;
  padding: 32px 8px 16px;
  text-align: center;
}

.store-empty-state--compact {
  padding-bottom: 2px;
}

.store-empty-state__icon {
  font-size: 42px;
  color: var(--ion-color-primary);
  opacity: 0.8;
}

.store-empty-state__title {
  margin: 0;
  color: var(--app-heading);
  font-size: 17px;
  font-weight: 700;
}

.store-empty-state__actions {
  display: grid;
  gap: 8px;
  width: 100%;
  margin-top: 12px;
}

.primary-action-btn {
  --border-radius: 14px;
  min-height: 48px;
  font-weight: 600;
  --box-shadow: 0 4px 12px rgba(var(--ion-color-primary-rgb), 0.3);
}

.store-list {
  margin-top: 8px;
}

.store-card {
  margin: 0 0 16px 0;
  border: 1px solid rgba(0, 0, 0, 0.04);
  border-radius: 20px;
  background: #ffffff;
  box-shadow: 0 4px 16px rgba(16, 35, 63, 0.04);
  transition: transform 0.2s ease;
}

.store-card:active {
  transform: scale(0.98);
}

.store-card__header-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.store-card__detail-row {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 10px 0;
  border-bottom: 1px solid #f1f5f9;
}

.store-card__detail-row:last-of-type {
  border-bottom: 0;
}

.store-card__detail-label {
  color: var(--app-muted);
  font-size: 13px;
}

.store-card__detail-value {
  flex: 1;
  color: var(--app-heading);
  font-size: 13px;
  font-weight: 500;
  text-align: right;
}

.store-card__button {
  margin-top: 16px;
  --border-radius: 12px;
  min-height: 44px;
  font-weight: 600;
}

.store-card__button--danger {
  margin-top: 10px;
}

.store-modal-page {
  --padding-top: 16px;
  --padding-bottom: 24px;
  --padding-start: 16px;
  --padding-end: 16px;
}

.store-create-panel {
  margin-bottom: 16px;
}

.store-create-panel h2,
.store-create-panel p {
  margin: 0;
}

.store-create-panel p {
  margin-top: 8px;
}

.store-form-grid {
  display: grid;
  gap: 14px;
}

.store-form-field {
  display: grid;
  gap: 8px;
}

.store-form-field span {
  color: var(--app-heading);
  font-size: 13px;
  font-weight: 600;
}

.store-form-field--full {
  grid-column: 1 / -1;
}

.store-phone-row {
  display: grid;
  grid-template-columns: 120px minmax(0, 1fr);
  gap: 10px;
}

.store-phone-prefix,
.store-phone-input {
  width: 100%;
}

.store-switch-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 16px;
  border-radius: 16px;
  border: 1px solid var(--app-border);
  background: rgba(15, 23, 42, 0.02);
}

.store-switch-row strong {
  display: block;
  color: var(--app-heading);
  font-size: 14px;
}

.store-switch-row__note {
  margin: 6px 0 0;
}

.store-form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  flex-wrap: wrap;
  margin-top: 18px;
}

:deep(.store-card ion-card-header) {
  padding-bottom: 12px;
}

:deep(.store-card ion-card-title) {
  color: var(--app-heading);
  font-size: 19px;
  font-weight: 700;
}

:deep(.store-card ion-card-subtitle) {
  margin-top: 4px;
  color: var(--app-muted);
  font-size: 13px;
}

@media (max-width: 480px) {
  .store-phone-row {
    grid-template-columns: 1fr;
  }

  .store-switch-row {
    align-items: flex-start;
  }
}
</style>
