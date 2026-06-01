<template>
  <div class="store-selection-page">
    <div class="top-nav">
      <div class="logo">
        <el-icon :size="28" color="#1890ff"><HomeFilled /></el-icon>
        <span class="logo-text">{{ t('app.name') }}</span>
      </div>
      <div class="user-info">
        <LanguageSwitcher />
        <span class="user-email">{{ userEmail }}</span>
        <el-button link type="danger" @click="handleLogout">{{ t('layout.logout') }}</el-button>
      </div>
    </div>

    <div class="main-content">
      <div class="toolbar">
        <el-input
          v-model="searchKeyword"
          :placeholder="t('stage6.components.storeSelection.searchPlaceholder')"
          class="search-input"
          :prefix-icon="Search"
          clearable
        />
        <el-button type="primary" size="large" @click="showCreateDialog">
          {{ t('stage6.components.storeSelection.createStore') }}
        </el-button>
      </div>

      <div v-if="loading" class="loading-container">
        <el-icon class="is-loading" :size="40">
          <Loading />
        </el-icon>
        <p>{{ t('stage6.components.storeSelection.loading') }}</p>
      </div>

      <div v-else class="store-grid">
        <div
          v-for="store in filteredStores"
          :key="store.id"
          class="store-card"
          @click="selectStore(store)"
        >
          <div class="store-header">
            <div class="store-icon">
              <el-icon :size="24"><HomeFilled /></el-icon>
            </div>
            <span class="store-name">{{ formatPropertyType(store.type) }}</span>
            <div class="store-actions">
              <span v-if="store.userRole === 'owner'" class="store-badge pro">Pro</span>
              <span v-else class="store-badge ess">Ess</span>
              <el-button
                v-if="store.userRole === 'owner'"
                link
                type="danger"
                size="small"
                :loading="deletingStoreId === store.id"
                @click.stop="handleDeleteStore(store)"
              >
                {{ t('stage6.common.actions.delete') }}
              </el-button>
            </div>
          </div>
          <div class="store-body">
            <h3 class="store-title">{{ store.name }}</h3>
            <p class="store-validity">
              {{ t('stage6.components.storeSelection.validUntil', { date: formatDate(store.updatedAt) }) }}
            </p>
          </div>
        </div>

        <div v-if="filteredStores.length === 0 && !loading" class="empty-state">
          <el-empty :description="t('stage6.components.storeSelection.emptyStores')">
            <el-button type="primary" @click="showCreateDialog">
              {{ t('stage6.components.storeSelection.createStore') }}
            </el-button>
          </el-empty>
        </div>
      </div>
    </div>

    <el-dialog
      v-model="createDialogVisible"
      :title="t('stage6.components.storeSelection.createStore')"
      width="600px"
      :close-on-click-modal="false"
    >
      <template #header>
        <div class="dialog-header">
          <el-icon :size="24" color="#1890ff"><HomeFilled /></el-icon>
          <span>{{ t('stage6.components.storeSelection.createStore') }}</span>
        </div>
      </template>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        class="create-form"
      >
        <el-form-item :label="t('stage6.components.storeSelection.form.storeName')" prop="name">
          <el-input
            v-model="form.name"
            :placeholder="t('stage6.components.storeSelection.form.storeNamePlaceholder')"
            size="large"
            maxlength="50"
          />
        </el-form-item>

        <el-form-item :label="t('stage6.components.storeSelection.form.propertyType')" prop="type">
          <el-select
            v-model="form.type"
            :placeholder="t('stage6.components.storeSelection.form.propertyTypePlaceholder')"
            size="large"
            style="width: 100%"
          >
            <el-option
              v-for="option in PROPERTY_TYPE_OPTIONS"
              :key="option.value"
              :label="getOptionLabel(PROPERTY_TYPE_OPTIONS, option.value)"
              :value="option.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item :label="t('stage6.components.storeSelection.form.country')" prop="country">
          <el-select
            v-model="form.country"
            :placeholder="t('stage6.components.storeSelection.form.countryPlaceholder')"
            size="large"
            filterable
            style="width: 100%"
          >
            <el-option
              v-for="option in COUNTRY_OPTIONS"
              :key="option.value"
              :label="getOptionLabel(COUNTRY_OPTIONS, option.value)"
              :value="option.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item :label="t('stage6.components.storeSelection.form.city')" prop="city">
          <el-input v-model="form.city" :placeholder="t('stage6.components.storeSelection.form.cityPlaceholder')" size="large" />
        </el-form-item>

        <el-form-item :label="t('stage6.components.storeSelection.form.address')" prop="address">
          <el-input
            v-model="form.address"
            :placeholder="t('stage6.components.storeSelection.form.addressPlaceholder')"
            size="large"
            type="textarea"
            :rows="2"
          />
        </el-form-item>

        <el-form-item :label="t('stage6.components.storeSelection.form.phone')" prop="phone">
          <el-input v-model="form.phone" :placeholder="t('stage6.components.storeSelection.form.phonePlaceholder')" size="large">
            <template #prepend>
              <el-select v-model="phonePrefix" style="width: 100px">
                <el-option
                  v-for="option in PHONE_PREFIX_OPTIONS"
                  :key="option.value"
                  :label="option.label"
                  :value="option.value"
                />
              </el-select>
            </template>
          </el-input>
        </el-form-item>

        <el-form-item :label="t('stage6.components.storeSelection.form.manager')" prop="manager">
          <el-input v-model="form.manager" :placeholder="t('stage6.components.storeSelection.form.managerPlaceholder')" size="large" />
        </el-form-item>

        <el-form-item :label="t('stage6.components.storeSelection.form.timezone')" prop="timezone">
          <el-select
            v-model="form.timezone"
            :placeholder="t('stage6.components.storeSelection.form.timezonePlaceholder')"
            size="large"
            filterable
            style="width: 100%"
          >
            <el-option
              v-for="option in TIMEZONE_OPTIONS"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item :label="t('stage6.components.storeSelection.form.currency')" prop="currency">
          <el-select
            v-model="form.currency"
            :placeholder="t('stage6.components.storeSelection.form.currencyPlaceholder')"
            size="large"
            style="width: 100%"
          >
            <el-option
              v-for="option in CURRENCY_OPTIONS"
              :key="option.value"
              :label="getOptionLabel(CURRENCY_OPTIONS, option.value)"
              :value="option.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item :label="t('stage6.components.storeSelection.form.language')" prop="language">
          <el-select
            v-model="form.language"
            :placeholder="t('stage6.components.storeSelection.form.languagePlaceholder')"
            size="large"
            style="width: 100%"
          >
            <el-option
              v-for="option in LANGUAGE_OPTIONS"
              :key="option.value"
              :label="getOptionLabel(LANGUAGE_OPTIONS, option.value)"
              :value="option.value"
            />
          </el-select>
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item :label="t('stage6.components.storeSelection.form.checkinTime')" prop="checkinTime">
              <el-time-select
                v-model="form.checkinTime"
                start="00:00"
                step="00:30"
                end="23:30"
                :placeholder="t('stage6.components.storeSelection.form.checkinTimePlaceholder')"
                size="large"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="t('stage6.components.storeSelection.form.checkoutTime')" prop="checkoutTime">
              <el-time-select
                v-model="form.checkoutTime"
                start="00:00"
                step="00:30"
                end="23:30"
                :placeholder="t('stage6.components.storeSelection.form.checkoutTimePlaceholder')"
                size="large"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item :label="t('stage6.components.storeSelection.form.channelConnection')" prop="createSuProperty">
          <el-checkbox v-model="form.createSuProperty">
            {{ t('stage6.components.storeSelection.form.createSuProperty') }}
          </el-checkbox>
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="createDialogVisible = false" size="large">
            {{ t('stage6.common.actions.cancel') }}
          </el-button>
          <el-button type="primary" @click="handleCreateStore" :loading="submitting" size="large">
            {{ t('stage6.components.storeSelection.createStore') }}
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  Loading,
  Search,
  HomeFilled,
} from '@element-plus/icons-vue'
import { useStoreStore } from '@/stores/store'
import { useUserStore } from '@/stores/user'
import LanguageSwitcher from '@/components/LanguageSwitcher.vue'
import type { StoreDTO, StoreRequest } from '@/api/store'
import {
  COUNTRY_OPTIONS,
  CURRENCY_OPTIONS,
  LANGUAGE_OPTIONS,
  PHONE_PREFIX_OPTIONS,
  PROPERTY_TYPE_OPTIONS,
  TIMEZONE_OPTIONS,
  type StoreOption,
  getStoreOptionLabel,
} from '@/constants/storeOptions'
import { DEFAULT_STORE_TIME_ZONE } from '@/utils/storeDateTime'

const router = useRouter()
const storeStore = useStoreStore()
const userStore = useUserStore()
const { t } = useI18n()

const loading = ref(false)
const stores = ref<StoreDTO[]>([])
const searchKeyword = ref('')
const createDialogVisible = ref(false)
const submitting = ref(false)
const deletingStoreId = ref<number | null>(null)
const formRef = ref<FormInstance>()
const phonePrefix = ref('+86')

const userEmail = computed(() => userStore.currentUser?.email || '')

const createDefaultForm = (): StoreRequest => ({
  name: '',
  phone: '',
  phoneTechType: '5',
  type: '1',
  timezone: DEFAULT_STORE_TIME_ZONE,
  manager: '',
  country: 'China',
  city: '',
  state: '',
  address: '',
  currency: 'CNY',
  language: 'en',
  checkinTime: '15:00',
  checkoutTime: '11:00',
  createSuProperty: true,
})

const form = ref<StoreRequest>(createDefaultForm())

const rules = computed<FormRules>(() => ({
  name: [{ required: true, message: t('stage6.components.storeSelection.validation.storeName'), trigger: 'blur' }],
  type: [{ required: true, message: t('stage6.components.storeSelection.validation.propertyType'), trigger: 'change' }],
  country: [{ required: true, message: t('stage6.components.storeSelection.validation.country'), trigger: 'change' }],
  city: [{ required: true, message: t('stage6.components.storeSelection.validation.city'), trigger: 'blur' }],
  address: [{ required: true, message: t('stage6.components.storeSelection.validation.address'), trigger: 'blur' }],
  phone: [{ required: true, message: t('stage6.components.storeSelection.validation.phone'), trigger: 'blur' }],
  manager: [{ required: true, message: t('stage6.components.storeSelection.validation.manager'), trigger: 'blur' }],
  timezone: [{ required: true, message: t('stage6.components.storeSelection.validation.timezone'), trigger: 'change' }],
  currency: [{ required: true, message: t('stage6.components.storeSelection.validation.currency'), trigger: 'change' }],
  language: [{ required: true, message: t('stage6.components.storeSelection.validation.language'), trigger: 'change' }],
  checkinTime: [{ required: true, message: t('stage6.components.storeSelection.validation.checkinTime'), trigger: 'change' }],
  checkoutTime: [{ required: true, message: t('stage6.components.storeSelection.validation.checkoutTime'), trigger: 'change' }],
}))

const filteredStores = computed(() => {
  if (!searchKeyword.value) {
    return stores.value
  }
  return stores.value.filter((store) =>
    store.name.toLowerCase().includes(searchKeyword.value.toLowerCase())
  )
})

const resetCreateForm = () => {
  form.value = createDefaultForm()
}

const loadStores = async () => {
  loading.value = true
  try {
    stores.value = await storeStore.fetchUserStores(true)
  } catch (error: any) {
    ElMessage.error(error.message || t('stage6.components.storeSelection.messages.loadStoresFailed'))
  } finally {
    loading.value = false
  }
}

const handleDeleteStore = async (store: StoreDTO) => {
  try {
    await ElMessageBox.confirm(
      t('stage6.components.storeSelection.messages.deleteConfirm', { name: store.name }),
      t('stage6.components.storeSelection.messages.deleteTitle'),
      {
        type: 'warning',
        confirmButtonText: t('stage6.common.actions.delete'),
        cancelButtonText: t('stage6.common.actions.cancel'),
      }
    )
  } catch {
    return
  }

  deletingStoreId.value = store.id
  try {
    await storeStore.deleteStore(store.id)
    ElMessage.success(t('stage6.components.storeSelection.messages.deleteSuccess'))
    await loadStores()
  } catch (error: any) {
    if (error?.code === '953') {
      await ElMessageBox.alert(
        t('stage6.components.storeSelection.messages.suDeleteBlocked'),
        t('stage6.components.storeSelection.messages.deleteBlockedTitle'),
        { type: 'warning', confirmButtonText: t('stage6.components.storeSelection.messages.gotIt') }
      )
      return
    }
    ElMessage.error(error.message || t('stage6.components.storeSelection.messages.deleteFailed'))
  } finally {
    deletingStoreId.value = null
  }
}

const selectStore = (store: StoreDTO) => {
  storeStore.setCurrentStore(store)
  ElMessage.success(t('layout.store.switched', { name: store.name }))
  router.push('/')
}

const showCreateDialog = () => {
  createDialogVisible.value = true
  resetCreateForm()
  formRef.value?.clearValidate()
}

const handleCreateStore = async () => {
  if (!formRef.value) {
    return
  }

  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }

  submitting.value = true
  try {
    const phoneWithPrefix = `${phonePrefix.value} ${form.value.phone}`
    const requestData = {
      ...form.value,
      phone: phoneWithPrefix,
      phoneTechType: '5',
    }

    const result = await storeStore.createStore(requestData)
    const newStore = result.store
    const message = result.message || t('stage6.components.storeSelection.messages.createSuccess')

    if (message.includes('\u5931\u8d25') || message.toLowerCase().includes('fail')) {
      ElMessage.warning(message)
    } else {
      ElMessage.success(message)
    }

    createDialogVisible.value = false
    await loadStores()
    selectStore(newStore)
  } catch (error: any) {
    ElMessage.error(error.message || t('stage6.components.storeSelection.messages.createFailed'))
  } finally {
    submitting.value = false
  }
}

const getOptionLabel = (options: StoreOption[], value?: string) => getStoreOptionLabel(options, value, t)

const formatPropertyType = (value?: string) => getOptionLabel(PROPERTY_TYPE_OPTIONS, value)

const handleLogout = async () => {
  await userStore.logout()
  storeStore.clearStoreData()
  router.push('/login')
}

const formatDate = (dateStr: string) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}/${month}/${day}`
}

onMounted(() => {
  loadStores()
})
</script>

<style scoped lang="scss">
.store-selection-page {
  min-height: 100vh;
  background: #f5f7fa;
}

.top-nav {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 40px;
  background: white;
  border-bottom: 1px solid #e8ecef;

  .logo {
    display: flex;
    align-items: center;
    gap: 12px;

    .logo-text {
      font-size: 20px;
      font-weight: 600;
      color: #1890ff;
    }
  }

  .user-info {
    display: flex;
    align-items: center;
    gap: 24px;

    .user-dropdown {
      display: flex;
      align-items: center;
      gap: 6px;
      cursor: pointer;
      padding: 6px 12px;
      border-radius: 6px;
      transition: background 0.3s;

      &:hover {
        background: #f5f7fa;
      }

      .arrow-down {
        font-size: 12px;
      }
    }

    .user-email {
      color: #666;
      font-size: 14px;
    }
  }
}

.main-content {
  max-width: 1400px;
  margin: 0 auto;
  padding: 40px 40px 60px;
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 32px;

  .search-input {
    max-width: 400px;
  }

  .el-button {
    margin-left: auto;
  }
}

.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 100px 0;
  color: #999;

  .el-icon {
    margin-bottom: 16px;
    color: #1890ff;
  }
}

.store-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 24px;
}

.store-card {
  background: white;
  border-radius: 12px;
  padding: 24px;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 1px solid #e8ecef;

  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
    border-color: #1890ff;
  }

  .store-header {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 20px;

    .store-icon {
      width: 48px;
      height: 48px;
      background: #e6f4ff;
      border-radius: 8px;
      display: flex;
      align-items: center;
      justify-content: center;
      color: #1890ff;
    }

    .store-name {
      font-size: 14px;
      color: #666;
    }

    .store-actions {
      margin-left: auto;
      display: flex;
      align-items: center;
      gap: 8px;
    }

    .store-badge {
      padding: 4px 12px;
      border-radius: 4px;
      font-size: 12px;
      font-weight: 500;

      &.pro {
        background: #fff3e0;
        color: #e67e22;
      }

      &.ess {
        background: #e8f5e9;
        color: #27ae60;
      }
    }
  }

  .store-body {
    .store-title {
      font-size: 18px;
      font-weight: 600;
      color: #2c3e50;
      margin: 0 0 12px 0;
    }

    .store-validity {
      font-size: 13px;
      color: #999;
      margin: 0;
    }
  }
}

.empty-state {
  grid-column: 1 / -1;
  padding: 60px 0;
}

.dialog-header {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 18px;
  font-weight: 600;
  color: #2c3e50;
}

.create-form {
  :deep(.el-form-item__label) {
    font-weight: 500;
    color: #2c3e50;
    margin-bottom: 8px;
  }

  :deep(.el-input__inner),
  :deep(.el-textarea__inner) {
    border-radius: 6px;
  }
}

.dialog-footer {
  display: flex;
  justify-content: center;
  gap: 16px;

  .el-button {
    min-width: 120px;
  }
}
</style>
