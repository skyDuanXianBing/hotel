<template>
  <div class="store-details-container">
    <!-- 门店卡片列表 -->
    <div v-if="!showConfigPage" class="stores-grid">
      <div
        v-for="store in stores"
        :key="store.id"
        class="store-card"
      >
        <div class="card-header">
          <el-icon class="collapse-icon"><Remove /></el-icon>
          <h3 class="store-name">{{ store.name }}</h3>
        </div>
        <el-button
          type="primary"
          class="config-button"
          @click="handleConfig(store)"
        >
          配置
        </el-button>
      </div>
    </div>

    <!-- 配置详情页面 -->
    <div v-else class="config-page">
      <!-- 面包屑导航 -->
      <div class="breadcrumb-nav">
        <el-breadcrumb separator="›">
          <el-breadcrumb-item>
            <a @click="handleBack">详情</a>
          </el-breadcrumb-item>
          <el-breadcrumb-item>{{ currentStore?.name }}</el-breadcrumb-item>
        </el-breadcrumb>
      </div>

      <!-- 标签页 -->
      <el-tabs v-model="activeTab" class="store-tabs">
        <el-tab-pane label="详情" name="details">
          <div class="details-content">
            <!-- 门店名称和编辑按钮 -->
            <div class="store-header">
              <h2 class="store-title">{{ currentStore?.name }}</h2>
              <el-button type="primary" @click="handleEdit">编辑</el-button>
            </div>

            <!-- 主要内容区域 -->
            <div class="main-content">
              <!-- 左侧图片 -->
              <div class="image-section">
                <div class="store-image">
                  <el-icon class="upload-icon"><UploadFilled /></el-icon>
                  <p class="upload-text">上传图片</p>
                </div>
              </div>

              <!-- 右侧信息网格 -->
              <div class="info-section">
                <div class="info-grid">
                  <div class="info-item">
                    <label class="info-label">联系电话</label>
                    <span class="info-value">{{ storeDetails.phone || '-' }}</span>
                  </div>
                  <div class="info-item">
                    <label class="info-label">邮箱地址</label>
                    <span class="info-value">{{ storeDetails.email || '-' }}</span>
                  </div>
                  <div class="info-item">
                    <label class="info-label">Wechat</label>
                    <span class="info-value">{{ storeDetails.wechat || '-' }}</span>
                  </div>
                  <div class="info-item">
                    <label class="info-label">WhatsApp</label>
                    <span class="info-value">{{ storeDetails.whatsapp || '-' }}</span>
                  </div>
                  <div class="info-item">
                    <label class="info-label">LINE</label>
                    <span class="info-value">{{ storeDetails.line || '-' }}</span>
                  </div>
                  <div class="info-item full-width">
                    <label class="info-label">地址</label>
                    <span class="info-value">{{ storeDetails.address || '-' }}</span>
                  </div>
                  <div class="info-item">
                    <label class="info-label">城市</label>
                    <span class="info-value">{{ storeDetails.city || '-' }}</span>
                  </div>
                  <div class="info-item">
                    <label class="info-label">州/省</label>
                    <span class="info-value">{{ storeDetails.state || '-' }}</span>
                  </div>
                  <div class="info-item">
                    <label class="info-label">国家和地区</label>
                    <span class="info-value">{{ storeDetails.country || '-' }}</span>
                  </div>
                  <div class="info-item full-width">
                    <label class="info-label">门店描述</label>
                    <span class="info-value">{{ storeDetails.description || '-' }}</span>
                  </div>
                </div>
              </div>
            </div>

            <!-- 地图区域 -->
            <div class="map-section">
              <div class="map-header">
                <el-radio-group v-model="mapType" size="small">
                  <el-radio-button label="map">Map</el-radio-button>
                  <el-radio-button label="satellite">Satellite</el-radio-button>
                </el-radio-group>
              </div>
              <div class="map-container">
                <div class="map-placeholder">
                  <p>地图区域</p>
                </div>
              </div>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane label="政策" name="policy">
          <div class="policy-content">
            <!-- 政策标题和编辑按钮 -->
            <div class="policy-header">
              <h2 class="policy-title">{{ currentStore?.name }}</h2>
              <el-button type="primary" @click="handleEditPolicy">编辑</el-button>
            </div>

            <!-- 主要内容区域 -->
            <div class="policy-main">
              <!-- 左侧图片 -->
              <div class="policy-image-section">
                <div class="policy-image">
                  <el-icon class="upload-icon"><UploadFilled /></el-icon>
                  <p class="upload-text">上传图片</p>
                </div>
              </div>

              <!-- 右侧政策信息 -->
              <div class="policy-info-section">
                <div class="policy-grid">
                  <div class="policy-item">
                    <label class="policy-label">入住时间</label>
                    <span class="policy-value">{{ policyDetails.checkinTime || '-' }}</span>
                  </div>
                  <div class="policy-item">
                    <label class="policy-label">退房时间</label>
                    <span class="policy-value">{{ policyDetails.checkoutTime || '-' }}</span>
                  </div>
                  <div class="policy-item full-width">
                    <label class="policy-label">儿童政策</label>
                    <span class="policy-value">{{ policyDetails.childPolicy || '-' }}</span>
                  </div>
                  <div class="policy-item">
                    <label class="policy-label">吸烟政策</label>
                    <span class="policy-value">{{ policyDetails.smokingPolicy || '-' }}</span>
                  </div>
                  <div class="policy-item">
                    <label class="policy-label">宠物政策</label>
                    <span class="policy-value">{{ policyDetails.petPolicy || '-' }}</span>
                  </div>
                  <div class="policy-item full-width">
                    <label class="policy-label">附加规则</label>
                    <span class="policy-value">{{ policyDetails.additionalRules || '-' }}</span>
                  </div>
                  <div class="policy-item full-width">
                    <label class="policy-label">酒店条款</label>
                    <span class="policy-value">{{ policyDetails.hotelTerms || '-' }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane label="设施" name="facilities">
          <div class="facilities-content">
            <!-- 设施标题和编辑按钮 -->
            <div class="facilities-header">
              <h2 class="facilities-title">{{ currentStore?.name }}</h2>
              <el-button type="primary" @click="handleEditFacilities">编辑</el-button>
            </div>

            <!-- 主要内容区域 -->
            <div class="facilities-main">
              <!-- 左侧图片 -->
              <div class="facilities-image-section">
                <div class="facilities-image">
                  <el-icon class="upload-icon"><UploadFilled /></el-icon>
                  <p class="upload-text">上传图片</p>
                </div>
              </div>

              <!-- 右侧设施信息 -->
              <div class="facilities-info-section">
                <div class="facilities-grid">
                  <div
                    v-for="facility in selectedFacilitiesList"
                    :key="facility"
                    class="facility-tag"
                  >
                    {{ facility }}
                  </div>
                  <div v-if="selectedFacilitiesList.length === 0" class="no-facilities">
                    暂无设施
                  </div>
                </div>
              </div>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane label="照片" name="photos">
          <div class="photos-content">
            <!-- 照片标题和编辑按钮 -->
            <div class="photos-header">
              <h2 class="photos-title">{{ currentStore?.name }}</h2>
              <el-button type="primary" @click="handleEditPhotos">编辑</el-button>
            </div>

            <!-- 展示在电脑上的照片 -->
            <div class="photo-section">
              <h3 class="photo-section-title">展示在电脑上的照片</h3>
              <div class="photos-grid">
                <div
                  v-for="(photo, index) in desktopPhotos"
                  :key="index"
                  class="photo-item"
                >
                  <img :src="photo" alt="电脑照片" class="photo-img" />
                </div>
                <div v-if="desktopPhotos.length === 0" class="no-photos">
                  暂无照片
                </div>
              </div>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- 编辑对话框 -->
    <el-dialog
      v-model="editDialogVisible"
      title="编辑"
      width="800px"
      :close-on-click-modal="false"
    >
      <el-form :model="editForm" :rules="formRules" ref="formRef" label-width="100px">
        <el-form-item label="翻译">
          <el-select v-model="editForm.language" style="width: 100%">
            <el-option label="English" value="English" />
            <el-option label="中文" value="中文" />
            <el-option label="日本語" value="日本語" />
          </el-select>
          <div class="form-hint" style="margin-top: 8px">
            您将以"English"保存该内容。如果您将语言设置为"English"，将显示此翻译。默认情况下，空白字段将输出为英语
          </div>
        </el-form-item>

        <el-form-item label="门店名称" prop="name">
          <div style="width: 100%">
            <div class="form-hint" style="margin-bottom: 8px">
              请填写展示给客人的门店名称
            </div>
            <el-input v-model="editForm.name" placeholder="请填写门店名称" />
          </div>
        </el-form-item>

        <el-form-item label="酒店Logo">
          <div class="upload-area-dialog">
            <el-icon class="upload-icon"><UploadFilled /></el-icon>
            <p class="upload-text">上传</p>
          </div>
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="联系电话" prop="phone">
              <el-input v-model="editForm.phone" placeholder="请输入联系电话" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="邮箱地址" prop="email">
              <el-input v-model="editForm.email" placeholder="请输入邮箱地址" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="Wechat">
              <el-input v-model="editForm.wechat" placeholder="请输入微信号" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="WhatsApp">
              <el-input v-model="editForm.whatsapp" placeholder="请输入WhatsApp" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="LINE">
              <el-input v-model="editForm.line" placeholder="请输入LINE" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="地址" prop="address">
          <el-input v-model="editForm.address" placeholder="请输入详细地址" />
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="城市" prop="city">
              <el-input v-model="editForm.city" placeholder="请输入城市" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="州/省">
              <el-input v-model="editForm.state" placeholder="请输入州/省" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="国家和地区" prop="country">
              <el-select v-model="editForm.country" placeholder="请选择国家" style="width: 100%">
                <el-option label="China" value="China" />
                <el-option label="Japan" value="Japan" />
                <el-option label="Korea" value="Korea" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="门店描述">
          <el-input
            v-model="editForm.description"
            type="textarea"
            :rows="4"
            placeholder="请输入门店描述内容，它将会显示在网站首页"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="handleCancelEdit">取消</el-button>
          <el-button type="primary" @click="handleSaveEdit">保存</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 政策编辑对话框 -->
    <el-dialog
      v-model="editPolicyDialogVisible"
      title="编辑政策"
      width="800px"
      :close-on-click-modal="false"
    >
      <el-form :model="editPolicyForm" ref="policyFormRef" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="入住时间">
              <el-input v-model="editPolicyForm.checkinTime" placeholder="例如:14:00 之后" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="退房时间">
              <el-input v-model="editPolicyForm.checkoutTime" placeholder="例如:12:00 之前" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="儿童政策">
          <el-input v-model="editPolicyForm.childPolicy" placeholder="请输入儿童政策" />
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="吸烟政策">
              <el-select v-model="editPolicyForm.smokingPolicy" placeholder="请选择吸烟政策" style="width: 100%">
                <el-option label="禁止吸烟" value="禁止吸烟" />
                <el-option label="允许吸烟" value="允许吸烟" />
                <el-option label="部分房间允许" value="部分房间允许" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="宠物政策">
              <el-select v-model="editPolicyForm.petPolicy" placeholder="请选择宠物政策" style="width: 100%">
                <el-option label="禁止携带宠物" value="禁止携带宠物" />
                <el-option label="允许携带宠物" value="允许携带宠物" />
                <el-option label="需额外收费" value="需额外收费" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="附加规则">
          <el-input
            v-model="editPolicyForm.additionalRules"
            type="textarea"
            :rows="3"
            placeholder="请输入附加规则"
          />
        </el-form-item>

        <el-form-item label="酒店条款">
          <el-input
            v-model="editPolicyForm.hotelTerms"
            type="textarea"
            :rows="3"
            placeholder="请输入酒店条款"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="handleCancelEditPolicy">取消</el-button>
          <el-button type="primary" @click="handleSaveEditPolicy">保存</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 设施编辑对话框 -->
    <el-dialog
      v-model="editFacilitiesDialogVisible"
      title="编辑"
      width="900px"
      :close-on-click-modal="false"
    >
      <div class="facilities-edit-grid">
        <el-checkbox
          v-for="facility in allFacilities"
          :key="facility"
          v-model="selectedFacilities"
          :label="facility"
          :value="facility"
        />
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="handleCancelEditFacilities">取消</el-button>
          <el-button type="primary" @click="handleSaveEditFacilities">保存</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 照片编辑对话框 -->
    <el-dialog
      v-model="editPhotosDialogVisible"
      title="编辑"
      width="900px"
      :close-on-click-modal="false"
    >
      <div class="photos-edit-container">
        <!-- 上传提示信息 -->
        <div class="upload-tip">
          <h4 class="upload-tip-title">请上传门店照片</h4>
          <p class="upload-tip-text">
            请注意如果您上传的照片非推荐尺寸，它们将被自动调整，建议遵照推荐尺寸，以免影响美观。为了更好地显示您的照片，建议您分别上传在电脑和手机上的照片
          </p>
        </div>

        <!-- 展示在电脑上的照片 -->
        <div class="upload-section">
          <h4 class="upload-section-title">展示在电脑上的照片 <span class="required">*</span></h4>
          <div class="upload-info">
            <p>最大数量: 40</p>
            <p>最大上传大小: 图片5MB, 视频20MB</p>
            <p>推荐尺寸: 1920px*768px</p>
          </div>
          <div class="upload-area-large">
            <el-icon class="upload-icon-large"><UploadFilled /></el-icon>
            <p class="upload-text-large">上传</p>
          </div>
        </div>

        <!-- 展示在手机上的照片 -->
        <div class="upload-section">
          <h4 class="upload-section-title">展示在手机上的照片 <span class="required">*</span></h4>
          <div class="upload-info">
            <p>最大数量: 40</p>
            <p>最大上传大小: 图片5MB, 视频20MB</p>
            <p>推荐尺寸: 750px*450px(5:3)</p>
          </div>
          <div class="upload-area-large">
            <el-icon class="upload-icon-large"><UploadFilled /></el-icon>
            <p class="upload-text-large">上传</p>
          </div>
        </div>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="handleCancelEditPhotos">取消</el-button>
          <el-button type="primary" @click="handleSaveEditPhotos">保存</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { Remove, UploadFilled } from '@element-plus/icons-vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import {
  getAllStores,
  getStoreById,
  updateStore,
  getStorePolicy,
  saveStorePolicy,
} from '@/api/store'

interface Store {
  id: number
  name: string
}

interface StoreDetails {
  language: string
  name: string
  description: string
  logo?: string
  phone: string
  email: string
  wechat: string
  whatsapp: string
  line: string
  address: string
  city: string
  state: string
  country: string
}

interface PolicyDetails {
  checkinTime: string
  checkoutTime: string
  childPolicy: string
  smokingPolicy: string
  petPolicy: string
  additionalRules: string
  hotelTerms: string
}

const showConfigPage = ref(false)
const currentStore = ref<Store | null>(null)
const activeTab = ref('details')
const editDialogVisible = ref(false)
const editPolicyDialogVisible = ref(false)
const editFacilitiesDialogVisible = ref(false)
const formRef = ref<FormInstance>()
const policyFormRef = ref<FormInstance>()
const mapType = ref('map')

// 所有可选设施列表
const allFacilities = ref([
  'WiFi', '停车场', '电梯', '行李寄存', '健身房', '游泳池', '室外游泳区',
  '餐厅', '休息室', '机场班车', '酒吧', '前台接待', '会议室', '洗衣服务',
  '桑拿', '水疗中心', '礼宾服务', '自动取款机', '外币兑换', '提供早餐', '礼品店',
  '打印机', '客房服务', '加床/婴儿床', '24小时前台', '禁止吸烟', '残疾人通道', '轮椅通道',
  '司机服务', '私人庭院', '加床/婴儿床', '多语言员工'
])

// 已选择的设施
const selectedFacilities = ref<string[]>(['停车场', '休息室'])

// 照片数据
const desktopPhotos = ref<string[]>([])
const mobilePhotos = ref<string[]>([])
const editPhotosDialogVisible = ref(false)

const stores = ref<Store[]>([])
const loading = ref(false)

const storeDetails = ref<StoreDetails>({
  language: 'English',
  name: '',
  description: '',
  phone: '13512345678',
  email: '211890@qq.com',
  wechat: '',
  whatsapp: '',
  line: '',
  address: 'hhh',
  city: 'hhh',
  state: '-',
  country: 'China',
})

const editForm = reactive<StoreDetails>({
  language: 'English',
  name: '',
  description: '',
  phone: '',
  email: '',
  wechat: '',
  whatsapp: '',
  line: '',
  address: '',
  city: '',
  state: '',
  country: '',
})

const policyDetails = ref<PolicyDetails>({
  checkinTime: '14:00 之后',
  checkoutTime: '12:00 之前',
  childPolicy: '儿童年龄0-3可随同成人免费入住',
  smokingPolicy: '禁止吸烟',
  petPolicy: '禁止携带宠物',
  additionalRules: '-',
  hotelTerms: '-',
})

const editPolicyForm = reactive<PolicyDetails>({
  checkinTime: '',
  checkoutTime: '',
  childPolicy: '',
  smokingPolicy: '',
  petPolicy: '',
  additionalRules: '',
  hotelTerms: '',
})

const formRules: FormRules = {
  name: [{ required: true, message: '请输入门店名称', trigger: 'blur' }],
}

// 加载门店列表
const loadStores = async () => {
  try {
    loading.value = true
    const response = await getAllStores()
    if (response.success && response.data) {
      stores.value = response.data.map((store) => ({
        id: store.id,
        name: store.name,
      }))
    } else {
      ElMessage.error(response.message || '加载门店列表失败')
    }
  } catch (error) {
    console.error('加载门店列表失败:', error)
    ElMessage.error('加载门店列表失败')
  } finally {
    loading.value = false
  }
}

// 加载门店详情
const loadStoreDetails = async (storeId: number) => {
  try {
    loading.value = true
    const response = await getStoreById(storeId)
    if (response.success && response.data) {
      const data = response.data
      storeDetails.value = {
        language: data.language || 'English',
        name: data.name,
        description: data.description || '',
        logo: data.logo,
        phone: data.phone || '',
        email: data.email || '',
        wechat: data.wechat || '',
        whatsapp: data.whatsapp || '',
        line: data.line || '',
        address: data.address || '',
        city: data.city || '',
        state: data.state || '',
        country: data.country || '',
      }
    }
  } catch (error) {
    console.error('加载门店详情失败:', error)
    ElMessage.error('加载门店详情失败')
  } finally {
    loading.value = false
  }
}

// 加载门店政策
const loadStorePolicy = async (storeId: number) => {
  try {
    const response = await getStorePolicy(storeId)
    if (response.success && response.data) {
      const data = response.data
      policyDetails.value = {
        checkinTime: data.checkinTime || '',
        checkoutTime: data.checkoutTime || '',
        childPolicy: data.childPolicy || '',
        smokingPolicy: data.smokingPolicy || '',
        petPolicy: data.petPolicy || '',
        additionalRules: data.additionalRules || '',
        hotelTerms: data.hotelTerms || '',
      }
    }
  } catch (error) {
    console.error('加载门店政策失败:', error)
  }
}

const handleConfig = async (store: Store) => {
  currentStore.value = store
  showConfigPage.value = true
  await loadStoreDetails(store.id)
  await loadStorePolicy(store.id)
}

const handleBack = () => {
  showConfigPage.value = false
  currentStore.value = null
}

const handleEdit = () => {
  Object.assign(editForm, storeDetails.value)
  editDialogVisible.value = true
}

const handleCancelEdit = () => {
  editDialogVisible.value = false
}

const handleSaveEdit = async () => {
  try {
    const valid = await formRef.value?.validate()
    if (valid && currentStore.value) {
      loading.value = true
      const response = await updateStore(currentStore.value.id, editForm)
      if (response.success) {
        ElMessage.success('保存成功')
        editDialogVisible.value = false
        await loadStoreDetails(currentStore.value.id)
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

const handleEditPolicy = () => {
  Object.assign(editPolicyForm, policyDetails.value)
  editPolicyDialogVisible.value = true
}

const handleCancelEditPolicy = () => {
  editPolicyDialogVisible.value = false
}

const handleSaveEditPolicy = async () => {
  if (!currentStore.value) return

  try {
    loading.value = true
    const response = await saveStorePolicy(currentStore.value.id, editPolicyForm)
    if (response.success) {
      ElMessage.success('保存成功')
      editPolicyDialogVisible.value = false
      await loadStorePolicy(currentStore.value.id)
    } else {
      ElMessage.error(response.message || '保存失败')
    }
  } catch (error) {
    console.error('保存失败:', error)
    ElMessage.error('保存失败')
  } finally {
    loading.value = false
  }
}

const selectedFacilitiesList = computed(() => {
  return selectedFacilities.value
})

const handleEditFacilities = () => {
  editFacilitiesDialogVisible.value = true
}

const handleCancelEditFacilities = () => {
  editFacilitiesDialogVisible.value = false
}

const handleSaveEditFacilities = () => {
  ElMessage.success('保存成功')
  editFacilitiesDialogVisible.value = false
}

const handleEditPhotos = () => {
  editPhotosDialogVisible.value = true
}

const handleCancelEditPhotos = () => {
  editPhotosDialogVisible.value = false
}

const handleSaveEditPhotos = () => {
  ElMessage.success('保存成功')
  editPhotosDialogVisible.value = false
}

onMounted(() => {
  loadStores()
})
</script>

<style scoped>
.store-details-container {
  padding: 20px;
  background: #f5f7fa;
  min-height: calc(100vh - 100px);
}

.stores-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
  gap: 20px;
}

.store-card {
  background: #fff;
  border-radius: 8px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  transition: all 0.3s;
  position: relative;
}

.store-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
  transform: translateY(-2px);
}

.card-header {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 20px;
}

.collapse-icon {
  flex-shrink: 0;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #ecf5ff;
  border-radius: 4px;
  color: #409eff;
  font-size: 16px;
  margin-top: 2px;
}

.store-name {
  flex: 1;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin: 0;
  line-height: 1.6;
}

.config-button {
  width: 100%;
}

/* 配置页面样式 */
.config-page {
  background: #fff;
  border-radius: 8px;
  padding: 24px;
  min-height: calc(100vh - 140px);
}

.breadcrumb-nav {
  margin-bottom: 20px;
}

:deep(.el-breadcrumb__item) {
  font-size: 14px;
}

:deep(.el-breadcrumb__item a) {
  color: #606266;
  cursor: pointer;
}

:deep(.el-breadcrumb__item a:hover) {
  color: #409eff;
}

.store-tabs {
  margin-top: 20px;
}

.details-content {
  padding: 20px 0;
}

/* 门店名称和编辑按钮 */
.store-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid #ebeef5;
}

.store-title {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

/* 主要内容区域 */
.main-content {
  display: flex;
  gap: 32px;
  margin-bottom: 32px;
}

/* 左侧图片区域 */
.image-section {
  flex-shrink: 0;
  width: 200px;
}

.store-image {
  width: 200px;
  height: 200px;
  border: 1px dashed #dcdfe6;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s;
  background: #f5f7fa;
}

.store-image:hover {
  border-color: #409eff;
  background: #ecf5ff;
}

.upload-icon {
  font-size: 48px;
  color: #909399;
  margin-bottom: 8px;
}

.upload-text {
  font-size: 14px;
  color: #606266;
  margin: 0;
}

/* 右侧信息网格 */
.info-section {
  flex: 1;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px 32px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.info-item.full-width {
  grid-column: 1 / -1;
}

.info-label {
  font-size: 12px;
  color: #909399;
  font-weight: 500;
}

.info-value {
  font-size: 14px;
  color: #303133;
  word-break: break-word;
}

/* 地图区域 */
.map-section {
  margin-top: 32px;
  padding-top: 32px;
  border-top: 1px solid #ebeef5;
}

/* 政策页面样式 */
.policy-content {
  padding: 20px 0;
}

.policy-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid #ebeef5;
}

.policy-title {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

.policy-main {
  display: flex;
  gap: 32px;
  margin-bottom: 32px;
}

.policy-image-section {
  flex-shrink: 0;
  width: 200px;
}

.policy-image {
  width: 200px;
  height: 200px;
  border: 1px dashed #dcdfe6;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s;
  background: #f5f7fa;
}

.policy-image:hover {
  border-color: #409eff;
  background: #ecf5ff;
}

.policy-info-section {
  flex: 1;
}

.policy-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px 32px;
}

.policy-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.policy-item.full-width {
  grid-column: 1 / -1;
}

.policy-label {
  font-size: 12px;
  color: #909399;
  font-weight: 500;
}

.policy-value {
  font-size: 14px;
  color: #303133;
  word-break: break-word;
}

.map-header {
  margin-bottom: 16px;
}

.map-container {
  width: 100%;
  height: 400px;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  overflow: hidden;
}

.map-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
  color: #909399;
  font-size: 16px;
}

.upload-area-dialog {
  width: 120px;
  height: 120px;
  border: 1px dashed #dcdfe6;
  border-radius: 4px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s;
}

.upload-area-dialog:hover {
  border-color: #409eff;
}

.form-hint {
  font-size: 12px;
  color: #909399;
  line-height: 1.5;
}

.empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 400px;
}

:deep(.el-tabs__nav-wrap::after) {
  display: none;
}

:deep(.el-select) {
  width: 100%;
}

:deep(.el-textarea__inner) {
  font-family: inherit;
}

/* 设施页面样式 */
.facilities-content {
  padding: 20px 0;
}

.facilities-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid #ebeef5;
}

.facilities-title {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

.facilities-main {
  display: flex;
  gap: 32px;
  margin-bottom: 32px;
}

.facilities-image-section {
  flex-shrink: 0;
  width: 200px;
}

.facilities-image {
  width: 200px;
  height: 200px;
  border: 1px dashed #dcdfe6;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s;
  background: #f5f7fa;
}

.facilities-image:hover {
  border-color: #409eff;
  background: #ecf5ff;
}

.facilities-info-section {
  flex: 1;
}

.facilities-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.facility-tag {
  padding: 8px 16px;
  background: #ecf5ff;
  color: #409eff;
  border-radius: 4px;
  font-size: 14px;
}

.no-facilities {
  color: #909399;
  font-size: 14px;
}

/* 设施编辑对话框 */
.facilities-edit-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 16px;
  padding: 20px 0;
}

.facilities-edit-grid :deep(.el-checkbox) {
  margin-right: 0;
}

.facilities-edit-grid :deep(.el-checkbox__label) {
  font-size: 14px;
}

/* 照片页面样式 */
.photos-content {
  padding: 20px 0;
}

.photos-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid #ebeef5;
}

.photos-title {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

.photo-section {
  margin-bottom: 32px;
}

.photo-section-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 16px 0;
}

.photos-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 16px;
}

.photo-item {
  position: relative;
  width: 100%;
  aspect-ratio: 16/9;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #dcdfe6;
}

.photo-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.no-photos {
  grid-column: 1 / -1;
  text-align: center;
  padding: 40px;
  color: #909399;
  font-size: 14px;
}

/* 照片编辑对话框样式 */
.photos-edit-container {
  padding: 20px 0;
}

.upload-tip {
  margin-bottom: 24px;
  padding: 16px;
  background: #f5f7fa;
  border-radius: 4px;
}

.upload-tip-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 8px 0;
}

.upload-tip-text {
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
  margin: 0;
}

.upload-section {
  margin-bottom: 32px;
}

.upload-section-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 12px 0;
}

.required {
  color: #f56c6c;
}

.upload-info {
  margin-bottom: 16px;
}

.upload-info p {
  font-size: 13px;
  color: #909399;
  margin: 4px 0;
}

.upload-area-large {
  width: 200px;
  height: 200px;
  border: 2px dashed #dcdfe6;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s;
  background: #fafafa;
}

.upload-area-large:hover {
  border-color: #409eff;
  background: #ecf5ff;
}

.upload-icon-large {
  font-size: 64px;
  color: #909399;
  margin-bottom: 12px;
}

.upload-text-large {
  font-size: 16px;
  color: #606266;
  margin: 0;
}
</style>
