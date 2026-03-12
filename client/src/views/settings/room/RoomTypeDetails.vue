<template>
  <div class="room-type-details-container">
    <div class="breadcrumb-section">
      <el-breadcrumb separator=">">
        <el-breadcrumb-item @click="handleBack" class="breadcrumb-link">房型</el-breadcrumb-item>
        <el-breadcrumb-item>详情</el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <div class="content-section">
      <el-tabs v-model="activeTab" class="room-tabs">
        <el-tab-pane label="基本信息" name="basic">
          <div class="tab-content">
            <div class="info-notice">以下名称与描述将作为同步到 Su 的英文内容</div>

            <el-form :model="formData" label-width="100px" class="detail-form">
              <el-form-item label="英文名称" required>
                <el-input
                  v-model="formData.name"
                  placeholder="请输入英文名称"
                  maxlength="100"
                  show-word-limit
                />
              </el-form-item>

              <el-form-item label="英文描述">
                <el-input
                  v-model="formData.description"
                  type="textarea"
                  :rows="5"
                  placeholder="请输入英文描述"
                  maxlength="2000"
                  show-word-limit
                />
              </el-form-item>
            </el-form>

            <div class="form-actions">
              <el-button @click="handleBack">取消</el-button>
              <el-button type="primary" :loading="savingBasic" @click="handleSaveBasic">保存</el-button>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane label="设施" name="facilities">
          <div class="tab-content">
            <div
              v-for="section in ROOM_FACILITY_SECTIONS"
              :key="section.title"
              class="facilities-section"
            >
              <h3 class="section-title">{{ section.title }}</h3>
              <div class="facility-grid">
                <el-checkbox
                  v-for="facility in section.items"
                  :key="facility.key"
                  v-model="facilityState[facility.key]"
                >
                  {{ facility.label }}
                </el-checkbox>
              </div>
            </div>

            <div class="form-actions">
              <el-button @click="handleBack">取消</el-button>
              <el-button type="primary" :loading="savingFacilities" @click="handleSaveFacilities">
                保存
              </el-button>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane label="照片" name="photos">
          <div class="tab-content">
            <div class="photo-section">
              <h3 class="section-title">照片</h3>
              <div class="photo-info">
                <p>最大数量: 40</p>
                <p>最大尺寸: 图片 5MB</p>
                <p>推荐尺寸: 1200px 以上</p>
              </div>
              <div class="upload-area">
                <el-upload
                  v-model:file-list="photos"
                  class="upload-box"
                  list-type="picture-card"
                  :show-file-list="true"
                  :before-upload="beforeUpload"
                  :http-request="handlePhotoRequest"
                >
                  <el-icon class="upload-icon"><Upload /></el-icon>
                  <div class="upload-text">上传</div>
                </el-upload>
              </div>
            </div>

            <div class="form-actions">
              <el-button @click="handleBack">取消</el-button>
              <el-button type="primary" :loading="savingPhotos" @click="handleSavePhotos">保存</el-button>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  ElMessage,
  type UploadRawFile,
  type UploadRequestOptions,
  type UploadUserFile,
} from 'element-plus'
import { Upload } from '@element-plus/icons-vue'
import { uploadMedia } from '@/api/media'
import {
  getAllRoomTypesWithRooms,
  getRoomTypeById,
  updateRoomType,
  type CreateRoomTypeRequest,
  type FacilityDTO,
  type RoomTypeDTO,
} from '@/api/roomType'
import { ROOM_FACILITY_SECTIONS } from '@/constants/suFacilities'

type UploadErrorParam = Parameters<NonNullable<UploadRequestOptions['onError']>>[0]

interface RoomTypeForm {
  id: number
  name: string
  description: string
  code: string
  totalRooms: number
  maxGuests: number
  checkInGuideLink: string
  defaultPrice?: number
  weekdayPrice?: number
  weekendPrice?: number
  mondayPrice?: number
  tuesdayPrice?: number
  wednesdayPrice?: number
  thursdayPrice?: number
  fridayPrice?: number
  saturdayPrice?: number
  sundayPrice?: number
  roomNumbers: string[]
}

interface RoomTypeWithRoomsItem {
  id: number
  rooms?: Array<{ roomNumber: string }>
}

type FacilityState = Record<string, boolean>

const router = useRouter()
const route = useRoute()

const activeTab = ref('basic')
const savingBasic = ref(false)
const savingFacilities = ref(false)
const savingPhotos = ref(false)
const photos = ref<UploadUserFile[]>([])

const createFacilityState = (): FacilityState =>
  ROOM_FACILITY_SECTIONS.reduce<FacilityState>((state, section) => {
    section.items.forEach((item) => {
      state[item.key] = false
    })
    return state
  }, {})

const facilityState = reactive<FacilityState>(createFacilityState())

const formData = reactive<RoomTypeForm>({
  id: 0,
  name: '',
  description: '',
  code: '',
  totalRooms: 1,
  maxGuests: 1,
  checkInGuideLink: '',
  defaultPrice: undefined,
  weekdayPrice: undefined,
  weekendPrice: undefined,
  mondayPrice: undefined,
  tuesdayPrice: undefined,
  wednesdayPrice: undefined,
  thursdayPrice: undefined,
  fridayPrice: undefined,
  saturdayPrice: undefined,
  sundayPrice: undefined,
  roomNumbers: [],
})

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

const applyFacilities = (facilities: FacilityDTO[] = []) => {
  Object.keys(facilityState).forEach((key) => {
    facilityState[key] = false
  })

  for (const section of ROOM_FACILITY_SECTIONS) {
    for (const item of section.items) {
      facilityState[item.key] = facilities.some((facility) => {
        const group = facility.group?.trim() || 'Common'
        const name = facility.name?.trim()
        return (
          name === item.payload.name &&
          (group === (item.payload.group?.trim() || 'Common') || name === item.label)
        )
      })
    }
  }
}

const buildFacilities = (): FacilityDTO[] =>
  ROOM_FACILITY_SECTIONS.flatMap((section) =>
    section.items
      .filter((item) => facilityState[item.key])
      .map((item) => ({
        group: item.payload.group,
        name: item.payload.name,
      }))
  )

const buildPayload = (): CreateRoomTypeRequest => ({
  name: formData.name,
  code: formData.code,
  description: formData.description,
  totalRooms: formData.totalRooms,
  maxGuests: formData.maxGuests,
  checkInGuideLink: formData.checkInGuideLink,
  defaultPrice: formData.defaultPrice,
  weekdayPrice: formData.weekdayPrice,
  weekendPrice: formData.weekendPrice,
  mondayPrice: formData.mondayPrice,
  tuesdayPrice: formData.tuesdayPrice,
  wednesdayPrice: formData.wednesdayPrice,
  thursdayPrice: formData.thursdayPrice,
  fridayPrice: formData.fridayPrice,
  saturdayPrice: formData.saturdayPrice,
  sundayPrice: formData.sundayPrice,
  roomNumbers: [...formData.roomNumbers],
  facilities: buildFacilities(),
  desktopPhotoUrls: extractPhotoUrls(photos.value),
  mobilePhotoUrls: [],
  localizedContent: {
    'en-US': {
      name: formData.name,
      description: formData.description,
    },
  },
})

const loadRoomTypeDetails = async () => {
  const roomTypeId = Number(route.params.id)
  if (!roomTypeId) {
    ElMessage.error('房型 ID 不存在')
    handleBack()
    return
  }

  try {
    const [detailResponse, roomResponse] = await Promise.all([
      getRoomTypeById(roomTypeId),
      getAllRoomTypesWithRooms(),
    ])

    if (!detailResponse.success || !detailResponse.data) {
      ElMessage.error(detailResponse.message || '加载房型详情失败')
      return
    }

    const data: RoomTypeDTO = detailResponse.data
    const englishContent = data.localizedContent?.['en-US']

    formData.id = data.id
    formData.name = englishContent?.name || data.name || ''
    formData.description = englishContent?.description || data.description || ''
    formData.code = data.code || ''
    formData.totalRooms = data.totalRooms || 1
    formData.maxGuests = data.maxGuests || 1
    formData.checkInGuideLink = data.checkInGuideLink || ''
    formData.defaultPrice = data.defaultPrice
    formData.weekdayPrice = data.weekdayPrice
    formData.weekendPrice = data.weekendPrice
    formData.mondayPrice = data.mondayPrice
    formData.tuesdayPrice = data.tuesdayPrice
    formData.wednesdayPrice = data.wednesdayPrice
    formData.thursdayPrice = data.thursdayPrice
    formData.fridayPrice = data.fridayPrice
    formData.saturdayPrice = data.saturdayPrice
    formData.sundayPrice = data.sundayPrice

    photos.value = toUploadFiles(mergePhotoUrls(data.desktopPhotoUrls || [], data.mobilePhotoUrls || []))
    applyFacilities(data.facilities || [])

    if (roomResponse.success && roomResponse.data) {
      const matched = roomResponse.data.find(
        (item: RoomTypeWithRoomsItem) => item.id === roomTypeId
      )
      formData.roomNumbers = matched?.rooms?.map((room: { roomNumber: string }) => room.roomNumber) || []
    }
  } catch (error) {
    console.error('加载房型详情失败:', error)
    ElMessage.error('加载房型详情失败')
  }
}

const saveRoomType = async (successMessage: string) => {
  if (!formData.id) {
    ElMessage.error('房型 ID 不存在')
    return false
  }

  const response = await updateRoomType(formData.id, buildPayload())
  if (!response.success) {
    ElMessage.error(response.message || '保存失败')
    return false
  }

  ElMessage.success(successMessage)
  await loadRoomTypeDetails()
  return true
}

const handleBack = () => {
  router.back()
}

const handleSaveBasic = async () => {
  try {
    savingBasic.value = true
    await saveRoomType('保存成功')
  } catch (error) {
    console.error('保存房型失败:', error)
    ElMessage.error('保存房型失败')
  } finally {
    savingBasic.value = false
  }
}

const handleSaveFacilities = async () => {
  try {
    savingFacilities.value = true
    await saveRoomType('设施信息保存成功')
  } catch (error) {
    console.error('保存设施失败:', error)
    ElMessage.error('保存设施失败')
  } finally {
    savingFacilities.value = false
  }
}

const handleSavePhotos = async () => {
  try {
    savingPhotos.value = true
    await saveRoomType('照片保存成功')
  } catch (error) {
    console.error('保存照片失败:', error)
    ElMessage.error('保存照片失败')
  } finally {
    savingPhotos.value = false
  }
}

const beforeUpload = (file: UploadRawFile) => {
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

const handlePhotoUpload = async (options: UploadRequestOptions) => {
  try {
    const response = await uploadMedia('room-type-desktop', options.file as File)
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
  loadRoomTypeDetails()
})
</script>

<style scoped>
.room-type-details-container {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #fff;
}

.breadcrumb-section {
  padding: 16px 20px;
  border-bottom: 1px solid #e8e8e8;
}

.breadcrumb-link {
  cursor: pointer;
  color: #1890ff;
}

.breadcrumb-link:hover {
  text-decoration: underline;
}

.content-section {
  flex: 1;
  padding: 20px;
  overflow: auto;
}

.room-tabs {
  height: 100%;
}

.tab-content {
  padding: 20px 0;
}

.info-notice {
  background: #e6f7ff;
  border: 1px solid #91d5ff;
  border-radius: 4px;
  padding: 12px 16px;
  margin-bottom: 24px;
  color: #0050b3;
  font-size: 14px;
}

.detail-form {
  max-width: 800px;
}

.facilities-section {
  margin-bottom: 32px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin-bottom: 16px;
}

.facility-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.photo-section {
  margin-bottom: 32px;
}

.photo-info {
  background: #f5f5f5;
  padding: 12px 16px;
  border-radius: 4px;
  margin-bottom: 16px;
}

.photo-info p {
  margin: 4px 0;
  font-size: 14px;
  color: #666;
}

.upload-area {
  margin-top: 16px;
}

.upload-box {
  width: 100%;
}

.upload-icon {
  font-size: 32px;
  color: #999;
}

.upload-text {
  margin-top: 8px;
  font-size: 14px;
  color: #666;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 24px;
  border-top: 1px solid #e8e8e8;
  margin-top: 32px;
}

:deep(.el-tabs__content) {
  height: calc(100% - 55px);
  overflow: auto;
}

:deep(.el-checkbox) {
  margin-right: 0;
}
</style>
