<template>
  <div class="store-selection-page">
    <div class="top-nav">
      <div class="logo">
        <el-icon :size="28" color="#1890ff"><HomeFilled /></el-icon>
        <span class="logo-text">房东智控中心（THE HOST HUB）</span>
      </div>
      <div class="user-info">
        <el-dropdown trigger="click">
          <div class="user-dropdown">
            <el-icon><Document /></el-icon>
            <span>简体中文</span>
            <el-icon class="arrow-down"><ArrowDown /></el-icon>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item>简体中文</el-dropdown-item>
              <el-dropdown-item>English</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
        <span class="user-email">{{ userEmail }}</span>
        <el-button link type="danger" @click="handleLogout">退出</el-button>
      </div>
    </div>

    <div class="main-content">
      <div class="toolbar">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索门店名称"
          class="search-input"
          :prefix-icon="Search"
          clearable
        />
        <el-button type="primary" size="large" @click="showCreateDialog">
          创建新门店
        </el-button>
      </div>

      <div v-if="loading" class="loading-container">
        <el-icon class="is-loading" :size="40">
          <Loading />
        </el-icon>
        <p>加载中...</p>
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
                删除
              </el-button>
            </div>
          </div>
          <div class="store-body">
            <h3 class="store-title">{{ store.name }}</h3>
            <p class="store-validity">有效期至: {{ formatDate(store.updatedAt) }}</p>
          </div>
        </div>

        <div v-if="filteredStores.length === 0 && !loading" class="empty-state">
          <el-empty description="暂无门店数据">
            <el-button type="primary" @click="showCreateDialog">创建新门店</el-button>
          </el-empty>
        </div>
      </div>
    </div>

    <el-dialog
      v-model="createDialogVisible"
      title="创建新门店"
      width="600px"
      :close-on-click-modal="false"
    >
      <template #header>
        <div class="dialog-header">
          <el-icon :size="24" color="#1890ff"><HomeFilled /></el-icon>
          <span>创建新门店</span>
        </div>
      </template>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        class="create-form"
      >
        <el-form-item label="新门店名称" prop="name">
          <el-input
            v-model="form.name"
            placeholder="请输入门店名称"
            size="large"
            maxlength="50"
          />
        </el-form-item>

        <el-form-item label="房产类型" prop="type">
          <el-select
            v-model="form.type"
            placeholder="请选择房产类型"
            size="large"
            style="width: 100%"
          >
            <el-option
              v-for="option in PROPERTY_TYPE_OPTIONS"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="国家" prop="country">
          <el-select
            v-model="form.country"
            placeholder="请选择国家"
            size="large"
            filterable
            style="width: 100%"
          >
            <el-option
              v-for="option in COUNTRY_OPTIONS"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="城市" prop="city">
          <el-input v-model="form.city" placeholder="请输入城市" size="large" />
        </el-form-item>

        <el-form-item label="详细地址" prop="address">
          <el-input
            v-model="form.address"
            placeholder="请输入详细地址"
            size="large"
            type="textarea"
            :rows="2"
          />
        </el-form-item>

        <el-form-item label="电话号码" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入电话号码" size="large">
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

        <el-form-item label="联系人姓名" prop="manager">
          <el-input v-model="form.manager" placeholder="请输入联系人姓名" size="large" />
        </el-form-item>

        <el-form-item label="时区" prop="timezone">
          <el-select
            v-model="form.timezone"
            placeholder="请选择时区"
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

        <el-form-item label="货币" prop="currency">
          <el-select
            v-model="form.currency"
            placeholder="请选择货币"
            size="large"
            style="width: 100%"
          >
            <el-option
              v-for="option in CURRENCY_OPTIONS"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="语言" prop="language">
          <el-select
            v-model="form.language"
            placeholder="请选择语言"
            size="large"
            style="width: 100%"
          >
            <el-option
              v-for="option in LANGUAGE_OPTIONS"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="入住时间" prop="checkinTime">
              <el-time-select
                v-model="form.checkinTime"
                start="00:00"
                step="00:30"
                end="23:30"
                placeholder="请选择入住时间"
                size="large"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="退房时间" prop="checkoutTime">
              <el-time-select
                v-model="form.checkoutTime"
                start="00:00"
                step="00:30"
                end="23:30"
                placeholder="请选择退房时间"
                size="large"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="渠道直连" prop="createSuProperty">
          <el-checkbox v-model="form.createSuProperty">
            创建门店后同步创建/覆盖渠道物业（用于后续打开授权 Widget）
          </el-checkbox>
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="createDialogVisible = false" size="large">取消</el-button>
          <el-button type="primary" @click="handleCreateStore" :loading="submitting" size="large">
            创建新门店
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  Loading,
  Search,
  Document,
  ArrowDown,
  HomeFilled,
} from '@element-plus/icons-vue'
import { useStoreStore } from '@/stores/store'
import { useUserStore } from '@/stores/user'
import type { StoreDTO, StoreRequest } from '@/api/store'
import {
  COUNTRY_OPTIONS,
  CURRENCY_OPTIONS,
  LANGUAGE_OPTIONS,
  PHONE_PREFIX_OPTIONS,
  PROPERTY_TYPE_OPTIONS,
  TIMEZONE_OPTIONS,
  getStoreOptionLabel,
} from '@/constants/storeOptions'

const router = useRouter()
const storeStore = useStoreStore()
const userStore = useUserStore()

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
  timezone: 'Asia/Shanghai',
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

const rules: FormRules = {
  name: [{ required: true, message: '请输入门店名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择房产类型', trigger: 'change' }],
  country: [{ required: true, message: '请选择国家', trigger: 'change' }],
  city: [{ required: true, message: '请输入城市', trigger: 'blur' }],
  address: [{ required: true, message: '请输入详细地址', trigger: 'blur' }],
  phone: [{ required: true, message: '请输入电话号码', trigger: 'blur' }],
  manager: [{ required: true, message: '请输入联系人姓名', trigger: 'blur' }],
  timezone: [{ required: true, message: '请选择时区', trigger: 'change' }],
  currency: [{ required: true, message: '请选择货币', trigger: 'change' }],
  language: [{ required: true, message: '请选择语言', trigger: 'change' }],
  checkinTime: [{ required: true, message: '请选择入住时间', trigger: 'change' }],
  checkoutTime: [{ required: true, message: '请选择退房时间', trigger: 'change' }],
}

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
    ElMessage.error(error.message || '加载门店列表失败')
  } finally {
    loading.value = false
  }
}

const handleDeleteStore = async (store: StoreDTO) => {
  try {
    await ElMessageBox.confirm(
      `确定删除门店“${store.name}”吗？删除后该门店将从列表中移除。`,
      '删除门店',
      {
        type: 'warning',
        confirmButtonText: '删除',
        cancelButtonText: '取消',
      }
    )
  } catch {
    return
  }

  deletingStoreId.value = store.id
  try {
    await storeStore.deleteStore(store.id)
    ElMessage.success('门店已删除')
    await loadStores()
  } catch (error: any) {
    if (error?.code === '953') {
      await ElMessageBox.alert(
        'Su 返回错误码 953：该 Property 仍与渠道存在映射，请先在 Su 侧解绑渠道映射后再重试删除门店。',
        '无法删除门店',
        { type: 'warning', confirmButtonText: '知道了' }
      )
      return
    }
    ElMessage.error(error.message || '删除门店失败')
  } finally {
    deletingStoreId.value = null
  }
}

const selectStore = (store: StoreDTO) => {
  storeStore.setCurrentStore(store)
  ElMessage.success(`已切换到门店: ${store.name}`)
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
    const message = result.message || '门店创建成功'

    if (message.includes('失败')) {
      ElMessage.warning(message)
    } else {
      ElMessage.success(message)
    }

    createDialogVisible.value = false
    await loadStores()
    selectStore(newStore)
  } catch (error: any) {
    ElMessage.error(error.message || '创建门店失败')
  } finally {
    submitting.value = false
  }
}

const formatPropertyType = (value?: string) => getStoreOptionLabel(PROPERTY_TYPE_OPTIONS, value)

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
