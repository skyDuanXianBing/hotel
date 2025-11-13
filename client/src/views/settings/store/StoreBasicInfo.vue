<template>
  <div class="store-basic-info-container">
    <!-- 标题和编辑按钮 -->
    <div class="page-header">
      <h2 class="page-title">门店基本信息</h2>
      <el-button type="primary" @click="handleEdit">编辑</el-button>
    </div>

    <!-- 信息展示卡片 -->
    <div class="info-card">
      <div class="info-grid">
        <!-- 第一行 -->
        <div class="info-item">
          <span class="info-label">门店名称</span>
          <span class="info-value">{{ storeInfo.name }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">门店电话</span>
          <span class="info-value">{{ storeInfo.phone }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">门店类型</span>
          <span class="info-value">{{ storeInfo.type }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">时区</span>
          <span class="info-value">{{ storeInfo.timezone }}</span>
        </div>

        <!-- 第二行 -->
        <div class="info-item">
          <span class="info-label">门店ID</span>
          <span class="info-value">{{ storeInfo.id }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">负责人</span>
          <span class="info-value">{{ storeInfo.manager }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">Owner's Email</span>
          <span class="info-value">{{ storeInfo.ownerEmail }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">创建时间</span>
          <span class="info-value">{{ storeInfo.createdAt }}</span>
        </div>

        <!-- 第三行 -->
        <div class="info-item">
          <span class="info-label">门店地址</span>
          <span class="info-value">{{ storeInfo.address }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">城市</span>
          <span class="info-value">{{ storeInfo.city }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">国家和地区</span>
          <span class="info-value">{{ storeInfo.country }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">货币</span>
          <span class="info-value">{{ storeInfo.currency }}</span>
        </div>
      </div>
    </div>

    <!-- 编辑对话框 -->
    <el-dialog
      v-model="editDialogVisible"
      title="编辑门店基本信息"
      width="800px"
      :close-on-click-modal="false"
    >
      <el-form :model="editForm" :rules="formRules" ref="formRef" label-width="120px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="门店名称" prop="name">
              <el-input v-model="editForm.name" placeholder="请输入门店名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="门店电话" prop="phone">
              <el-input v-model="editForm.phone" placeholder="请输入门店电话" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="门店类型" prop="type">
              <el-select v-model="editForm.type" placeholder="请选择门店类型" style="width: 100%">
                <el-option label="日式旅馆" value="日式旅馆" />
                <el-option label="酒店" value="酒店" />
                <el-option label="民宿" value="民宿" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="时区" prop="timezone">
              <el-select v-model="editForm.timezone" placeholder="请选择时区" style="width: 100%">
                <el-option label="Asia/Tokyo" value="Asia/Tokyo" />
                <el-option label="Asia/Shanghai" value="Asia/Shanghai" />
                <el-option label="Asia/Hong_Kong" value="Asia/Hong_Kong" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="负责人" prop="manager">
              <el-input v-model="editForm.manager" placeholder="请输入负责人姓名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Owner's Email" prop="ownerEmail">
              <el-input v-model="editForm.ownerEmail" placeholder="请输入邮箱地址" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="门店地址" prop="address">
          <el-input v-model="editForm.address" placeholder="请输入门店地址" />
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="城市" prop="city">
              <el-input v-model="editForm.city" placeholder="请输入城市" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="国家和地区" prop="country">
              <el-select v-model="editForm.country" placeholder="请选择国家" style="width: 100%">
                <el-option label="Japan" value="Japan" />
                <el-option label="China" value="China" />
                <el-option label="Korea" value="Korea" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="货币" prop="currency">
          <el-select v-model="editForm.currency" placeholder="请选择货币" style="width: 100%">
            <el-option label="JPY" value="JPY" />
            <el-option label="CNY" value="CNY" />
            <el-option label="USD" value="USD" />
          </el-select>
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="handleCancelEdit">取消</el-button>
          <el-button type="primary" @click="handleSaveEdit">保存</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { getStoreById, updateStore, type StoreDTO } from '@/api/store'
import { useStoreStore } from '@/stores/store'

interface StoreInfo {
  id: string
  name: string
  phone: string
  type: string
  timezone: string
  manager: string
  ownerEmail: string
  createdAt: string
  address: string
  city: string
  country: string
  currency: string
}

const editDialogVisible = ref(false)
const formRef = ref<FormInstance>()
const loading = ref(false)

// 使用 Pinia store 获取当前门店
const storeStore = useStoreStore()
const currentStoreId = computed(() => storeStore.currentStore?.id)

const storeInfo = ref<StoreInfo>({
  id: '',
  name: '',
  phone: '',
  type: '',
  timezone: '',
  manager: '',
  ownerEmail: '',
  createdAt: '',
  address: '',
  city: '',
  country: '',
  currency: '',
})

const editForm = reactive<StoreInfo>({
  id: '',
  name: '',
  phone: '',
  type: '',
  timezone: '',
  manager: '',
  ownerEmail: '',
  createdAt: '',
  address: '',
  city: '',
  country: '',
  currency: '',
})

const formRules: FormRules = {
  name: [{ required: true, message: '请输入门店名称', trigger: 'blur' }],
  phone: [{ required: true, message: '请输入门店电话', trigger: 'blur' }],
  type: [{ required: true, message: '请选择门店类型', trigger: 'change' }],
  timezone: [{ required: true, message: '请选择时区', trigger: 'change' }],
  manager: [{ required: true, message: '请输入负责人姓名', trigger: 'blur' }],
  ownerEmail: [
    { required: true, message: '请输入邮箱地址', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' },
  ],
  address: [{ required: true, message: '请输入门店地址', trigger: 'blur' }],
  city: [{ required: true, message: '请输入城市', trigger: 'blur' }],
  country: [{ required: true, message: '请选择国家', trigger: 'change' }],
  currency: [{ required: true, message: '请选择货币', trigger: 'change' }],
}

const handleEdit = () => {
  Object.assign(editForm, storeInfo.value)
  editDialogVisible.value = true
}

const handleCancelEdit = () => {
  editDialogVisible.value = false
}

// 加载门店信息
const loadStoreInfo = async () => {
  if (!currentStoreId.value) {
    ElMessage.warning('请先选择门店')
    return
  }

  try {
    loading.value = true
    const response = await getStoreById(currentStoreId.value)
    if (response.success && response.data) {
      const data = response.data
      storeInfo.value = {
        id: data.id.toString(),
        name: data.name,
        phone: data.phone || '',
        type: data.type || '',
        timezone: data.timezone || '',
        manager: data.manager || '',
        ownerEmail: data.ownerEmail || '',
        createdAt: data.createdAt ? new Date(data.createdAt).toLocaleDateString('zh-CN') : '',
        address: data.address || '',
        city: data.city || '',
        country: data.country || '',
        currency: data.currency || '',
      }
    } else {
      ElMessage.error(response.message || '加载门店信息失败')
    }
  } catch (error) {
    console.error('加载门店信息失败:', error)
    ElMessage.error('加载门店信息失败')
  } finally {
    loading.value = false
  }
}

const handleSaveEdit = async () => {
  if (!currentStoreId.value) {
    ElMessage.warning('请先选择门店')
    return
  }

  try {
    const valid = await formRef.value?.validate()
    if (valid) {
      loading.value = true
      const response = await updateStore(currentStoreId.value, {
        name: editForm.name,
        phone: editForm.phone,
        type: editForm.type,
        timezone: editForm.timezone,
        manager: editForm.manager,
        ownerEmail: editForm.ownerEmail,
        address: editForm.address,
        city: editForm.city,
        country: editForm.country,
        currency: editForm.currency,
      })

      if (response.success) {
        ElMessage.success('保存成功')
        editDialogVisible.value = false
        // 重新加载数据
        await loadStoreInfo()
      } else {
        ElMessage.error(response.message || '保存失败')
      }
    }
  } catch (error) {
    console.error('保存失败:', error)
    ElMessage.error('保存失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadStoreInfo()
})
</script>

<style scoped>
.store-basic-info-container {
  padding: 20px;
  background: #f5f7fa;
  min-height: calc(100vh - 100px);
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

.info-card {
  background: #fff;
  border-radius: 8px;
  padding: 32px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 32px 24px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.info-label {
  font-size: 12px;
  color: #909399;
}

.info-value {
  font-size: 14px;
  color: #303133;
  font-weight: 500;
}

:deep(.el-form-item) {
  margin-bottom: 20px;
}
</style>
