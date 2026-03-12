<template>
  <div class="store-details-container">
    <div class="config-page">
      <div class="page-header">
        <h2 class="page-title">门店详情</h2>
      </div>

      <el-tabs v-model="activeTab" class="store-tabs">
        <el-tab-pane label="详情" name="details">
          <div class="details-content">
            <div class="store-header">
              <h2 class="store-title">{{ storeDetails.name || '-' }}</h2>
              <el-button type="primary" :loading="loading" @click="handleEdit">编辑</el-button>
            </div>

            <div class="main-content">
              <div class="image-section">
                <div class="store-image">
                  <img
                    v-if="storeDetails.logo"
                    :src="storeDetails.logo"
                    alt="门店 Logo"
                    class="store-logo-image"
                  />
                  <template v-else>
                    <el-icon class="upload-icon"><UploadFilled /></el-icon>
                    <p class="upload-text">上传图片</p>
                  </template>
                </div>
              </div>

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

        <el-tab-pane label="设施" name="facilities">
          <div class="facilities-content">
            <div class="facilities-header">
              <h2 class="facilities-title">{{ storeDetails.name || '-' }}</h2>
              <el-button type="primary" :loading="loading" @click="handleEditFacilities">编辑</el-button>
            </div>

            <div class="facilities-main">
              <div class="facilities-image-section">
                <div class="facilities-image">
                  <img
                    v-if="storeDetails.logo"
                    :src="storeDetails.logo"
                    alt="门店 Logo"
                    class="store-logo-image"
                  />
                  <template v-else>
                    <el-icon class="upload-icon"><UploadFilled /></el-icon>
                    <p class="upload-text">上传图片</p>
                  </template>
                </div>
              </div>

              <div class="facilities-info-section">
                <div class="facilities-grid">
                  <div v-for="facility in selectedFacilitiesList" :key="facility" class="facility-tag">
                    {{ facility }}
                  </div>
                  <div v-if="selectedFacilitiesList.length === 0" class="no-facilities">暂无设施</div>
                </div>
              </div>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane label="照片" name="photos">
          <div class="photos-content">
            <div class="photos-header">
              <h2 class="photos-title">{{ storeDetails.name || '-' }}</h2>
              <el-button type="primary" :loading="loading" @click="handleEditPhotos">编辑</el-button>
            </div>

            <div class="photo-section">
              <h3 class="photo-section-title">照片</h3>
              <div class="photos-grid">
                <div v-for="photo in photos" :key="photo.uid || photo.url" class="photo-item">
                  <img v-if="photo.url" :src="photo.url" alt="门店照片" class="photo-img" />
                </div>
                <div v-if="photos.length === 0" class="no-photos">暂无照片</div>
              </div>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>

    <el-dialog
      v-model="editDialogVisible"
      title="编辑"
      width="800px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="editForm" :rules="formRules" label-width="100px">
        <el-form-item label="门店名称" prop="name">
          <div style="width: 100%">
            <div class="form-hint" style="margin-bottom: 8px">请填写展示给客人的门店名称</div>
            <el-input v-model="editForm.name" placeholder="请填写门店名称" />
          </div>
        </el-form-item>

        <el-form-item label="酒店Logo">
          <el-upload
            :show-file-list="false"
            :before-upload="beforeImageUpload"
            :http-request="handleLogoUpload"
          >
            <div class="upload-area-dialog">
              <img v-if="editForm.logo" :src="editForm.logo" alt="logo" class="store-logo-image" />
              <template v-else>
                <el-icon class="upload-icon"><UploadFilled /></el-icon>
                <p class="upload-text">上传</p>
              </template>
            </div>
          </el-upload>
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
              <el-input v-model="editForm.country" placeholder="请输入国家和地区" />
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
          <el-button type="primary" :loading="loading" @click="handleSaveEdit">保存</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog
      v-model="editFacilitiesDialogVisible"
      title="编辑"
      width="900px"
      :close-on-click-modal="false"
    >
      <div class="facilities-edit-grid">
        <el-checkbox
          v-for="facility in STORE_FACILITY_OPTIONS"
          :key="facility.label"
          v-model="selectedFacilities"
          :label="facility.label"
          :value="facility.label"
        />
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="handleCancelEditFacilities">取消</el-button>
          <el-button type="primary" :loading="loading" @click="handleSaveEditFacilities">保存</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog
      v-model="editPhotosDialogVisible"
      title="编辑"
      width="900px"
      :close-on-click-modal="false"
    >
      <div class="photos-edit-container">
        <div class="upload-tip">
          <h4 class="upload-tip-title">请上传门店照片</h4>
          <p class="upload-tip-text">
            保存后将同步当前门店图片到 Su。
          </p>
        </div>

        <div class="upload-section">
          <h4 class="upload-section-title">照片</h4>
          <div class="upload-info">
            <p>最大数量: 40</p>
            <p>最大上传大小: 图片 5MB</p>
            <p>推荐尺寸: 1200px 以上</p>
          </div>
          <el-upload
            v-model:file-list="photos"
            class="upload-area-large"
            list-type="picture-card"
            :before-upload="beforeImageUpload"
            :http-request="handlePhotoRequest"
          >
            <el-icon class="upload-icon-large"><UploadFilled /></el-icon>
            <p class="upload-text-large">上传</p>
          </el-upload>
        </div>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="handleCancelEditPhotos">取消</el-button>
          <el-button type="primary" :loading="loading" @click="handleSaveEditPhotos">保存</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { UploadFilled } from '@element-plus/icons-vue'
import {
  ElMessage,
  type FormInstance,
  type FormRules,
  type UploadRawFile,
  type UploadRequestOptions,
  type UploadUserFile,
} from 'element-plus'
import {
  getStoreById,
  updateStore,
  type FacilityDTO,
  type StoreRequest,
} from '@/api/store'
import { uploadMedia } from '@/api/media'
import { STORE_FACILITY_OPTIONS } from '@/constants/suFacilities'
import { useStoreStore } from '@/stores/store'

type UploadErrorParam = Parameters<NonNullable<UploadRequestOptions['onError']>>[0]

interface StoreDetailsForm extends StoreRequest {
  language: string
}

const storeStore = useStoreStore()
const currentStoreId = computed(() => storeStore.currentStore?.id)

const activeTab = ref('details')
const editDialogVisible = ref(false)
const editFacilitiesDialogVisible = ref(false)
const editPhotosDialogVisible = ref(false)
const formRef = ref<FormInstance>()
const mapType = ref('map')
const selectedFacilities = ref<string[]>([])
const photos = ref<UploadUserFile[]>([])
const loading = ref(false)

const createEmptyStore = (): StoreDetailsForm => ({
  language: 'English',
  name: '',
  description: '',
  logo: '',
  phone: '',
  email: '',
  address: '',
  city: '',
  state: '',
  country: '',
  type: '',
  timezone: '',
  manager: '',
  currency: '',
  suHotelId: '',
  ownerEmail: '',
  facilities: [],
  desktopPhotoUrls: [],
  mobilePhotoUrls: [],
})

const storeDetails = reactive<StoreDetailsForm>(createEmptyStore())
const editForm = reactive<StoreDetailsForm>(createEmptyStore())

const formRules: FormRules = {
  name: [{ required: true, message: '请输入门店名称', trigger: 'blur' }],
  country: [{ required: true, message: '请输入国家和地区', trigger: 'blur' }],
}

const selectedFacilitiesList = computed(() => selectedFacilities.value)

const buildUploadAjaxError = (error: unknown): UploadErrorParam => {
  const normalizedError = error instanceof Error ? error : new Error(String(error))
  return {
    name: normalizedError.name || 'UploadError',
    message: normalizedError.message,
    status: 0,
    method: 'POST',
    url: '',
    stack: normalizedError.stack,
  } as UploadErrorParam
}

const toUploadFiles = (urls: string[]): UploadUserFile[] =>
  urls.map((url, index) => ({
    name: `image-${index + 1}`,
    url,
  }))

const mergePhotoUrls = (desktopUrls: string[] = [], mobileUrls: string[] = []): string[] => [
  ...new Set([...desktopUrls, ...mobileUrls].filter(Boolean)),
]

const extractPhotoUrls = (files: UploadUserFile[]): string[] =>
  files
    .map((file) => file.url)
    .filter((url): url is string => Boolean(url))

const buildFacilities = (): FacilityDTO[] =>
  selectedFacilities.value
    .map((label) => STORE_FACILITY_OPTIONS.find((item) => item.label === label)?.payload)
    .filter((facility): facility is FacilityDTO => Boolean(facility))

const applyFacilities = (facilities: FacilityDTO[] = []) => {
  selectedFacilities.value = STORE_FACILITY_OPTIONS.filter((option) =>
    facilities.some(
      (facility) =>
        facility.name === option.payload.name &&
        (facility.group || 'Common') === (option.payload.group || 'Common')
    )
  ).map((option) => option.label)
}

const buildStorePayload = (source: StoreDetailsForm): StoreRequest => ({
  name: source.name,
  phone: source.phone,
  type: source.type,
  timezone: source.timezone,
  manager: source.manager,
  country: source.country,
  city: source.city,
  state: source.state,
  address: source.address,
  currency: source.currency,
  suHotelId: source.suHotelId,
  ownerEmail: source.ownerEmail,
  language: 'English',
  description: source.description,
  logo: source.logo,
  email: source.email,
  facilities: buildFacilities(),
  desktopPhotoUrls: extractPhotoUrls(photos.value),
  mobilePhotoUrls: [],
})

const loadStoreDetails = async () => {
  if (!currentStoreId.value) {
    ElMessage.warning('请先选择门店')
    return
  }

  try {
    loading.value = true
    const response = await getStoreById(currentStoreId.value)
    if (!response.success || !response.data) {
      ElMessage.error(response.message || '加载门店详情失败')
      return
    }

    const data = response.data
    Object.assign(storeDetails, createEmptyStore(), {
      language: data.language || 'English',
      name: data.name || '',
      description: data.description || '',
      logo: data.logo || '',
      phone: data.phone || '',
      email: data.email || '',
      address: data.address || '',
      city: data.city || '',
      state: data.state || '',
      country: data.country || '',
      type: data.type || '',
      timezone: data.timezone || '',
      manager: data.manager || '',
      currency: data.currency || '',
      suHotelId: data.suHotelId || '',
      ownerEmail: data.ownerEmail || '',
      facilities: data.facilities || [],
      desktopPhotoUrls: mergePhotoUrls(data.desktopPhotoUrls || [], data.mobilePhotoUrls || []),
      mobilePhotoUrls: [],
    })

    photos.value = toUploadFiles(mergePhotoUrls(data.desktopPhotoUrls || [], data.mobilePhotoUrls || []))
    applyFacilities(data.facilities || [])
  } catch (error) {
    console.error('加载门店详情失败:', error)
    ElMessage.error('加载门店详情失败')
  } finally {
    loading.value = false
  }
}

const saveStore = async (payload: StoreRequest, successMessage: string) => {
  if (!currentStoreId.value) {
    ElMessage.warning('请先选择门店')
    return false
  }

  const response = await updateStore(currentStoreId.value, payload)
  if (!response.success) {
    ElMessage.error(response.message || '保存失败')
    return false
  }

  ElMessage.success(successMessage)
  await loadStoreDetails()
  return true
}

const handleEdit = () => {
  Object.assign(editForm, storeDetails)
  editDialogVisible.value = true
}

const handleCancelEdit = () => {
  editDialogVisible.value = false
}

const handleSaveEdit = async () => {
  try {
    const valid = await formRef.value?.validate()
    if (!valid) {
      return
    }
    loading.value = true
    const saved = await saveStore(buildStorePayload(editForm), '保存成功')
    if (saved) {
      editDialogVisible.value = false
    }
  } catch (error) {
    console.error('保存门店失败:', error)
    ElMessage.error('保存门店失败')
  } finally {
    loading.value = false
  }
}

const handleEditFacilities = () => {
  editFacilitiesDialogVisible.value = true
}

const handleCancelEditFacilities = () => {
  editFacilitiesDialogVisible.value = false
}

const handleSaveEditFacilities = async () => {
  try {
    loading.value = true
    const saved = await saveStore(buildStorePayload(storeDetails), '设施保存成功')
    if (saved) {
      editFacilitiesDialogVisible.value = false
    }
  } catch (error) {
    console.error('保存设施失败:', error)
    ElMessage.error('保存设施失败')
  } finally {
    loading.value = false
  }
}

const handleEditPhotos = () => {
  editPhotosDialogVisible.value = true
}

const handleCancelEditPhotos = () => {
  editPhotosDialogVisible.value = false
}

const handleSaveEditPhotos = async () => {
  try {
    loading.value = true
    const saved = await saveStore(buildStorePayload(storeDetails), '照片保存成功')
    if (saved) {
      editPhotosDialogVisible.value = false
    }
  } catch (error) {
    console.error('保存照片失败:', error)
    ElMessage.error('保存照片失败')
  } finally {
    loading.value = false
  }
}

const beforeImageUpload = (file: UploadRawFile) => {
  const isImage = file.type.startsWith('image/')
  const isLt5M = file.size / 1024 / 1024 < 5

  if (!isImage) {
    ElMessage.error('只能上传图片文件')
    return false
  }

  if (!isLt5M) {
    ElMessage.error('图片大小不能超过 5MB')
    return false
  }

  return true
}

const handleLogoUpload = async (options: UploadRequestOptions) => {
  try {
    const response = await uploadMedia('store-logo', options.file as File)
    if (!response.success || !response.data) {
      throw new Error(response.message || '上传失败')
    }

    editForm.logo = response.data.url
    options.onSuccess?.(response)
    ElMessage.success('Logo 上传成功')
  } catch (error) {
    console.error('Logo 上传失败:', error)
    ElMessage.error('Logo 上传失败')
    options.onError?.(buildUploadAjaxError(error))
  }
}

const handlePhotoUpload = async (options: UploadRequestOptions) => {
  try {
    const response = await uploadMedia('store-desktop', options.file as File)
    if (!response.success || !response.data) {
      throw new Error(response.message || '上传失败')
    }

    const matchedFile = photos.value.find((file) => file.uid === options.file.uid)

    if (matchedFile) {
      matchedFile.name = options.file.name
      matchedFile.url = response.data.url
      matchedFile.status = 'success'
    } else {
      photos.value.push({
        uid: options.file.uid,
        name: options.file.name,
        url: response.data.url,
        status: 'success',
      })
    }

    options.onSuccess?.(response)
    ElMessage.success('上传成功')
  } catch (error) {
    console.error('上传失败:', error)
    ElMessage.error('上传失败')
    options.onError?.(buildUploadAjaxError(error))
  }
}

const handlePhotoRequest = (options: UploadRequestOptions) => handlePhotoUpload(options)

onMounted(() => {
  loadStoreDetails()
})
</script>

<style scoped>
.store-details-container {
  padding: 20px;
  background: #f5f7fa;
  min-height: calc(100vh - 100px);
}

.config-page {
  background: #fff;
  border-radius: 8px;
  padding: 24px;
  min-height: calc(100vh - 140px);
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #ebeef5;
}

.page-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

.store-tabs {
  margin-top: 20px;
}

.details-content,
.facilities-content,
.photos-content {
  padding: 20px 0;
}

.store-header,
.facilities-header,
.photos-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid #ebeef5;
}

.store-title,
.facilities-title,
.photos-title {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

.main-content,
.facilities-main {
  display: flex;
  gap: 32px;
  margin-bottom: 32px;
}

.image-section,
.facilities-image-section {
  flex-shrink: 0;
  width: 200px;
}

.store-image,
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
  overflow: hidden;
}

.store-image:hover,
.facilities-image:hover {
  border-color: #409eff;
  background: #ecf5ff;
}

.store-logo-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
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

.info-section,
.facilities-info-section {
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

.map-section {
  margin-top: 32px;
  padding-top: 32px;
  border-top: 1px solid #ebeef5;
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
  aspect-ratio: 16 / 9;
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
  overflow: hidden;
}

.upload-area-dialog:hover {
  border-color: #409eff;
}

.form-hint {
  font-size: 12px;
  color: #909399;
  line-height: 1.5;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

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

.upload-info {
  margin-bottom: 16px;
}

.upload-info p {
  font-size: 13px;
  color: #909399;
  margin: 4px 0;
}

.upload-area-large {
  display: block;
}

.upload-area-large :deep(.el-upload) {
  width: 100%;
}

.upload-area-large :deep(.el-upload--picture-card) {
  width: 200px;
  height: 200px;
  border: 2px dashed #dcdfe6;
  border-radius: 8px;
  background: #fafafa;
}

.upload-area-large :deep(.el-upload-list--picture-card) {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
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

:deep(.el-tabs__nav-wrap::after) {
  display: none;
}

:deep(.el-textarea__inner) {
  font-family: inherit;
}
</style>
