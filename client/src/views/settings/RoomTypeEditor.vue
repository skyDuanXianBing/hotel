<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Plus, Upload, Check, Delete, Search, Edit, Star } from '@element-plus/icons-vue'
import type {
  RoomType,
  RoomTypeCategory,
  BreakfastOption,
  AcceptanceSetting,
  CancellationPolicy,
  BedInfo,
  RoomLayout,
  Amenity,
  RoomTypeTag,
  SaleSettings,
} from '@/types/room'

interface RoomTypeEditForm {
  id: number
  name: string
  externalName: string
  description: string
  defaultPrice: number
  weekdayPrice?: number
  weekendPrice?: number
  images: string[]
  category: RoomTypeCategory
  area: number
  capacity: number
  roomLayout: RoomLayout
  bedInfo: BedInfo[]
  amenities: Amenity[]
  tags: RoomTypeTag[]
  saleSettings: SaleSettings
  description_rich?: string
}

const props = defineProps<{
  roomType: any
}>()

const emit = defineEmits<{
  back: []
  save: [data: RoomTypeEditForm]
}>()

// 表单数据
const editForm = ref<RoomTypeEditForm>({
  id: props.roomType.id,
  name: props.roomType.name,
  externalName: props.roomType.name,
  description: props.roomType.description,
  defaultPrice: props.roomType.defaultPrice,
  images: ['/room-images/sample.jpg'], // 示例图片
  category: 'suite' as RoomTypeCategory,
  area: 30,
  capacity: 2,
  roomLayout: {
    bedroom: 1,
    bathroom: 1,
    livingRoom: 0,
    kitchen: 0,
    study: 0,
    balcony: 0,
  },
  bedInfo: [
    {
      type: '大床',
      size: '2×1.8米',
      count: 1,
    },
  ],
  amenities: [
    { id: '1', name: '空调', category: '核心设施' },
    { id: '2', name: '无线网络', category: '核心设施' },
    { id: '3', name: '智能门锁', category: '核心设施' },
  ],
  tags: [],
  saleSettings: {
    includeBreakfast: 'none' as BreakfastOption,
    latestCheckInTime: '24:00',
    acceptanceSetting: 'auto' as AcceptanceSetting,
    cancellationPolicy: 'confirmed_no_cancel' as CancellationPolicy,
    freeCancelDays: undefined,
  },
  description_rich: '',
})

// 房型类型选项
const roomTypeOptions = [
  { label: '套房', value: 'suite' as RoomTypeCategory },
  { label: '营位', value: 'standard' as RoomTypeCategory },
  { label: '账篷', value: 'luxury' as RoomTypeCategory },
]

// 包含早餐选项
const breakfastOptions = [
  { label: '无早', value: 'none' as BreakfastOption },
  { label: '单早', value: 'single' as BreakfastOption },
  { label: '双早', value: 'double' as BreakfastOption },
]

// 接单设置选项
const acceptanceOptions = [
  { label: '手动接单', value: 'manual' as AcceptanceSetting },
  { label: '自动接单', value: 'auto' as AcceptanceSetting },
]

// 取消政策选项
const cancellationPolicyOptions = [
  { label: '一经确认，不可取消修改', value: 'confirmed_no_cancel' as CancellationPolicy },
  { label: '入住日前可免费取消', value: 'free_cancel_before_checkin' as CancellationPolicy },
]

// 可用设施列表（模拟数据）
const availableAmenities = ref<Amenity[]>([
  { id: '1', name: '空调', category: '核心设施' },
  { id: '2', name: '无线网络', category: '核心设施' },
  { id: '3', name: '智能门锁', category: '核心设施' },
  { id: '4', name: '电视', category: '娱乐设施' },
  { id: '5', name: '冰箱', category: '生活设施' },
  { id: '6', name: '热水器', category: '生活设施' },
  { id: '7', name: '洗衣机', category: '生活设施' },
  { id: '8', name: '阳台', category: '空间设施' },
])

// 设施搜索
const amenitySearchText = ref('')
const searchResults = ref<Amenity[]>([])
const showSearchResults = ref(false)

// 预览数据计算
const previewData = computed(() => {
  const today = new Date()
  const tomorrow = new Date(today)
  tomorrow.setDate(tomorrow.getDate() + 1)

  return {
    name: editForm.value.name || '房型名称',
    externalName: editForm.value.externalName || '房型名称',
    checkInDate: `${today.getMonth() + 1}月${today.getDate()}日`,
    checkOutDate: `${tomorrow.getMonth() + 1}月${tomorrow.getDate()}日`,
    price: editForm.value.defaultPrice || 0,
    weekdayLabel: '周一到五',
    weekendLabel: `${tomorrow.getMonth() + 1}月${tomorrow.getDate()}日 周末`,
    capacity: editForm.value.capacity,
    bedInfo:
      editForm.value.bedInfo.length > 0
        ? `${editForm.value.bedInfo[0].count}张${editForm.value.bedInfo[0].type} (${editForm.value.bedInfo[0].size})`
        : `1张大床 (${editForm.value.area || 30}平米)`,
    area: editForm.value.area,
    roomType: roomTypeOptions.find((opt) => opt.value === editForm.value.category)?.label || '套房',
    image: editForm.value.images[0] || '/room-images/sample.jpg',
  }
})

// 图片上传处理
const handleImageUpload = () => {
  // 模拟图片上传
  ElMessage.success('图片上传功能演示')
}

const addImage = () => {
  editForm.value.images.push('/room-images/sample2.jpg')
}

const removeImage = (index: number) => {
  editForm.value.images.splice(index, 1)
}

// 设置房型图片首图
const setAsMainImage = () => {
  ElMessage.success('已设置为首图')
}

// 床型管理
const addBedInfo = () => {
  editForm.value.bedInfo.push({
    type: '大床',
    size: '2×1.8米',
    count: 1,
  })
}

const removeBedInfo = (index: number) => {
  editForm.value.bedInfo.splice(index, 1)
}

// 设施搜索
const searchAmenities = () => {
  if (!amenitySearchText.value.trim()) {
    showSearchResults.value = false
    return
  }

  searchResults.value = availableAmenities.value.filter(
    (amenity) =>
      amenity.name.includes(amenitySearchText.value) &&
      !editForm.value.amenities.find((selected) => selected.id === amenity.id),
  )
  showSearchResults.value = true
}

const addAmenity = (amenity: Amenity) => {
  if (!editForm.value.amenities.find((selected) => selected.id === amenity.id)) {
    editForm.value.amenities.push(amenity)
    amenitySearchText.value = ''
    showSearchResults.value = false
  }
}

const removeAmenity = (amenityId: string) => {
  const index = editForm.value.amenities.findIndex((amenity) => amenity.id === amenityId)
  if (index !== -1) {
    editForm.value.amenities.splice(index, 1)
  }
}

// 标签管理
const newTagName = ref('')
const addTag = () => {
  if (!newTagName.value.trim()) return

  const newTag: RoomTypeTag = {
    id: Date.now().toString(),
    name: newTagName.value.trim(),
    color: '#1890ff',
  }

  editForm.value.tags.push(newTag)
  newTagName.value = ''
  ElMessage.success('添加标签成功')
}

const removeTag = (tagId: string) => {
  const index = editForm.value.tags.findIndex((tag) => tag.id === tagId)
  if (index !== -1) {
    editForm.value.tags.splice(index, 1)
  }
}

// 监听取消政策变化
watch(
  () => editForm.value.saleSettings.cancellationPolicy,
  (newPolicy) => {
    if (newPolicy === 'confirmed_no_cancel') {
      editForm.value.saleSettings.freeCancelDays = undefined
    } else if (newPolicy === 'free_cancel_before_checkin') {
      editForm.value.saleSettings.freeCancelDays = 0
    }
  },
)

// 保存
const handleSave = () => {
  if (!editForm.value.externalName.trim()) {
    ElMessage.warning('请输入对外名称')
    return
  }

  emit('save', editForm.value)
  ElMessage.success('保存成功')
}

// 返回
const handleBack = () => {
  emit('back')
}
</script>

<template>
  <div class="room-type-editor">
    <!-- 顶部导航 -->
    <div class="editor-header">
      <div class="header-nav">
        <el-button text @click="handleBack" class="back-btn">
          <el-icon><ArrowLeft /></el-icon>
          返回
        </el-button>
        <div class="nav-tabs">
          <span class="nav-tab">房型房间设置</span>
          <span class="nav-tab active">编辑房型</span>
        </div>
      </div>
      <h2 class="editor-title">房型名称：{{ previewData.name }}</h2>
    </div>

    <!-- 主要内容区域 -->
    <div class="editor-content">
      <!-- 左侧预览卡片 -->
      <div class="preview-section">
        <div class="preview-card">
          <div class="card-image">
            <img :src="previewData.image" :alt="previewData.name" />
          </div>

          <div class="card-content">
            <h3 class="card-title">{{ previewData.externalName }}</h3>

            <div class="date-section">
              <div class="date-group">
                <div class="date-item">
                  <span class="date-label">入住日期</span>
                  <span class="date-value">{{ previewData.checkInDate }}</span>
                </div>
                <div class="date-separator">
                  <span class="separator-text">共1夜</span>
                </div>
                <div class="date-item">
                  <span class="date-label">退房</span>
                  <span class="date-value">{{ previewData.checkOutDate }}</span>
                </div>
              </div>
            </div>

            <div class="room-basic-info">
              <div class="info-grid">
                <div class="info-item">
                  <span class="info-label">可住</span>
                  <span class="info-value">{{ previewData.capacity }}人</span>
                </div>
                <div class="info-item">
                  <span class="info-label">面积</span>
                  <span class="info-value">{{ previewData.area }}m²</span>
                </div>
                <div class="info-item">
                  <span class="info-label">套餐</span>
                  <span class="info-value">无套餐</span>
                </div>
                <div class="info-item">
                  <span class="info-label">床型</span>
                  <span class="info-value">{{ previewData.bedInfo }}</span>
                </div>
              </div>
            </div>

            <div class="amenities-section">
              <h4>设施服务</h4>
              <div class="amenities-list">
                <div
                  v-for="amenity in editForm.amenities.slice(0, 4)"
                  :key="amenity.id"
                  class="amenity-item"
                >
                  <el-icon class="amenity-icon"><Check /></el-icon>
                  <span>{{ amenity.name }}</span>
                </div>
                <div v-if="editForm.amenities.length > 4" class="amenity-item">
                  <el-icon class="amenity-icon"><Check /></el-icon>
                  <span>更多设施</span>
                </div>
              </div>
            </div>

            <!-- 退订政策 -->
            <div class="cancellation-policy">
              <h4>退订政策</h4>
              <div class="policy-tag">
                <el-tag
                  :type="
                    editForm.saleSettings.cancellationPolicy === 'free_cancel_before_checkin'
                      ? 'warning'
                      : 'info'
                  "
                  size="small"
                  class="policy-badge"
                >
                  {{
                    editForm.saleSettings.cancellationPolicy === 'free_cancel_before_checkin'
                      ? '可变更'
                      : '订单确认，不可变更'
                  }}
                </el-tag>
              </div>
            </div>

            <!-- 相关说明 -->
            <div v-if="editForm.description_rich" class="description-section">
              <h4>相关说明</h4>
            </div>
          </div>

          <div class="card-footer">
            <div class="footer-actions">
              <el-button text class="note-btn">
                <el-icon><Edit /></el-icon>
                笔记
              </el-button>
              <el-button text class="subscribe-btn">
                <el-icon><Star /></el-icon>
                订阅
              </el-button>
            </div>
            <div class="price-section">
              <div class="price-display">
                <span class="currency">¥</span>
                <span class="price">{{ previewData.price.toFixed(0) }}</span>
              </div>
              <el-button type="primary" class="book-btn">预订</el-button>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧编辑表单 -->
      <div class="form-section">
        <el-form :model="editForm" label-width="100px" class="edit-form">
          <!-- 对外名称 -->
          <el-form-item label="对外名称" required>
            <el-input v-model="editForm.externalName" placeholder="请输入对外名称" />
            <div class="form-help">对外名称用于在C端展示给客户</div>
          </el-form-item>

          <!-- 房型图片 -->
          <el-form-item label="房型图片" required>
            <div class="image-upload-section">
              <div class="image-list">
                <div v-for="(image, index) in editForm.images" :key="index" class="image-item">
                  <img :src="image" :alt="`房型图片${index + 1}`" />
                  <div class="image-overlay">
                    <el-button text @click="removeImage(index)" class="remove-btn">删除</el-button>
                  </div>
                </div>
                <div class="image-upload-btn" @click="addImage">
                  <el-icon><Plus /></el-icon>
                  <span>添加图片</span>
                </div>
              </div>
              <div class="upload-help">
                请上传房型，至少1张，推荐宽高比5:3，支持JPG、JPEG、GIF格式，大小需不于5M，最多可上传20张
              </div>
              <el-button type="primary" link @click="setAsMainImage">
                设置房型图片首图跳转
              </el-button>
            </div>
          </el-form-item>

          <!-- 房型房间基础信息 -->
          <el-form-item label="房型房间基础信息" required>
            <div class="basic-info-section">
              <div class="info-row-form">
                <span class="info-label-form">房型类型</span>
                <el-radio-group v-model="editForm.category" class="room-type-radios">
                  <el-radio
                    v-for="option in roomTypeOptions"
                    :key="option.value"
                    :label="option.value"
                  >
                    {{ option.label }}
                  </el-radio>
                </el-radio-group>
              </div>

              <div class="info-row-form">
                <span class="info-label-form">面积</span>
                <el-input-number v-model="editForm.area" :min="1" class="area-input" />
                <span class="unit">m²</span>
              </div>

              <div class="info-row-form">
                <span class="info-label-form">可住</span>
                <el-input-number v-model="editForm.capacity" :min="1" class="capacity-input" />
                <span class="unit">人</span>
              </div>

              <!-- 户型设置 -->
              <div class="info-row-form">
                <span class="info-label-form">户型</span>
                <div class="room-layout-inputs">
                  <div class="layout-item">
                    <span class="layout-label">卧室</span>
                    <el-select v-model="editForm.roomLayout.bedroom" class="layout-select">
                      <el-option v-for="n in 10" :key="n - 1" :label="n - 1" :value="n - 1" />
                    </el-select>
                  </div>
                  <div class="layout-item">
                    <span class="layout-label">卫生间</span>
                    <el-select v-model="editForm.roomLayout.bathroom" class="layout-select">
                      <el-option v-for="n in 10" :key="n - 1" :label="n - 1" :value="n - 1" />
                    </el-select>
                  </div>
                  <div class="layout-item">
                    <span class="layout-label">客厅</span>
                    <el-select v-model="editForm.roomLayout.livingRoom" class="layout-select">
                      <el-option v-for="n in 10" :key="n - 1" :label="n - 1" :value="n - 1" />
                    </el-select>
                  </div>
                  <div class="layout-item">
                    <span class="layout-label">厨房</span>
                    <el-select v-model="editForm.roomLayout.kitchen" class="layout-select">
                      <el-option v-for="n in 10" :key="n - 1" :label="n - 1" :value="n - 1" />
                    </el-select>
                  </div>
                  <div class="layout-item">
                    <span class="layout-label">书房</span>
                    <el-select v-model="editForm.roomLayout.study" class="layout-select">
                      <el-option v-for="n in 10" :key="n - 1" :label="n - 1" :value="n - 1" />
                    </el-select>
                  </div>
                  <div class="layout-item">
                    <span class="layout-label">阳台</span>
                    <el-select v-model="editForm.roomLayout.balcony" class="layout-select">
                      <el-option v-for="n in 10" :key="n - 1" :label="n - 1" :value="n - 1" />
                    </el-select>
                  </div>
                </div>
              </div>

              <!-- 床型设置 -->
              <div class="info-row-form">
                <span class="info-label-form">床型</span>
                <div class="bed-info-list">
                  <div v-for="(bed, index) in editForm.bedInfo" :key="index" class="bed-info-item">
                    <el-input v-model="bed.type" placeholder="床型" class="bed-type-input" />
                    <el-input v-model="bed.size" placeholder="尺寸" class="bed-size-input" />
                    <el-input-number v-model="bed.count" :min="1" class="bed-count-input" />
                    <span class="unit">张</span>
                    <el-button
                      text
                      @click="removeBedInfo(index)"
                      v-if="editForm.bedInfo.length > 1"
                    >
                      <el-icon><Delete /></el-icon>
                    </el-button>
                  </div>
                  <el-button type="primary" link @click="addBedInfo" class="add-bed-btn">
                    <el-icon><Plus /></el-icon> 添加规格
                  </el-button>
                </div>
              </div>
            </div>
          </el-form-item>

          <!-- 销售设置&取消政策 -->
          <el-form-item label="销售设置&取消政策" required>
            <div class="sales-settings-section">
              <!-- 是否包含早餐 -->
              <div class="setting-row">
                <span class="setting-label">是否包含早餐</span>
                <el-radio-group
                  v-model="editForm.saleSettings.includeBreakfast"
                  class="breakfast-radios"
                >
                  <el-radio
                    v-for="option in breakfastOptions"
                    :key="option.value"
                    :label="option.value"
                  >
                    {{ option.label }}
                  </el-radio>
                </el-radio-group>
              </div>

              <!-- 当晚最晚预订时间 -->
              <div class="setting-row">
                <span class="setting-label">当晚最晚预订时间</span>
                <el-select v-model="editForm.saleSettings.latestCheckInTime" class="time-picker">
                  <el-option value="18:00" label="18:00" />
                  <el-option value="20:00" label="20:00" />
                  <el-option value="22:00" label="22:00" />
                  <el-option value="24:00" label="24:00" />
                </el-select>
              </div>

              <!-- 接单设置 -->
              <div class="setting-row">
                <span class="setting-label">接单设置</span>
                <el-radio-group
                  v-model="editForm.saleSettings.acceptanceSetting"
                  class="acceptance-radios"
                >
                  <el-radio
                    v-for="option in acceptanceOptions"
                    :key="option.value"
                    :label="option.value"
                  >
                    {{ option.label }}
                  </el-radio>
                </el-radio-group>
              </div>

              <!-- 取消政策 -->
              <div class="setting-row">
                <span class="setting-label">取消政策</span>
                <el-radio-group
                  v-model="editForm.saleSettings.cancellationPolicy"
                  class="cancellation-radios"
                >
                  <el-radio
                    v-for="option in cancellationPolicyOptions"
                    :key="option.value"
                    :label="option.value"
                  >
                    {{ option.label }}
                  </el-radio>
                </el-radio-group>
              </div>

              <!-- 入住日设置（仅在允许取消时显示） -->
              <div
                v-if="editForm.saleSettings.cancellationPolicy === 'free_cancel_before_checkin'"
                class="setting-row"
              >
                <div class="cancel-days-setting">
                  <span>入住日</span>
                  <el-input-number
                    v-model="editForm.saleSettings.freeCancelDays"
                    :min="0"
                    class="cancel-days-input"
                  />
                  <span>天</span>
                  <span class="setting-note">前可免费取消</span>
                </div>
              </div>
            </div>
          </el-form-item>

          <!-- 房间设施 -->
          <el-form-item label="房间设施">
            <div class="amenities-management">
              <!-- 搜索添加设施 -->
              <div class="amenity-search">
                <el-input
                  v-model="amenitySearchText"
                  placeholder="搜索已添加的设施名称"
                  class="search-input"
                  @input="searchAmenities"
                >
                  <template #suffix>
                    <el-icon><Search /></el-icon>
                  </template>
                </el-input>

                <!-- 搜索结果 -->
                <div v-if="showSearchResults" class="search-results">
                  <div class="results-header">
                    <span class="results-title">设施分类</span>
                    <span class="results-title">设施名称</span>
                    <span class="results-title">操作</span>
                  </div>
                  <div v-for="amenity in searchResults" :key="amenity.id" class="result-item">
                    <span class="result-category">{{ amenity.category }}</span>
                    <span class="result-name">{{ amenity.name }}</span>
                    <el-button text type="danger" @click="addAmenity(amenity)">添加</el-button>
                  </div>
                </div>
              </div>

              <!-- 已选设施列表 -->
              <div v-if="editForm.amenities.length" class="selected-amenities">
                <div class="amenities-header">
                  <span class="amenities-title">设施分类</span>
                  <span class="amenities-title">设施名称</span>
                  <span class="amenities-title">操作</span>
                </div>
                <div v-for="amenity in editForm.amenities" :key="amenity.id" class="amenity-row">
                  <span class="amenity-category">{{ amenity.category }}</span>
                  <span class="amenity-name">{{ amenity.name }}</span>
                  <el-button text type="danger" @click="removeAmenity(amenity.id)">删除</el-button>
                </div>
                <div class="pagination-info">共 {{ editForm.amenities.length }} 条</div>
              </div>
            </div>
          </el-form-item>

          <!-- 房型标签 -->
          <el-form-item label="房型标签">
            <div class="tags-management">
              <div class="tag-note">房型标签可在日房型商品列表展示，消费者可通过此标签筛选房型</div>

              <!-- 添加标签 -->
              <div class="add-tag-section">
                <el-input
                  v-model="newTagName"
                  placeholder="请输入标签名称"
                  class="tag-input"
                  @keyup.enter="addTag"
                />
                <el-button type="primary" link @click="addTag">
                  <el-icon><Plus /></el-icon> 添加标签
                </el-button>
              </div>

              <!-- 已添加的标签 -->
              <div v-if="editForm.tags.length" class="selected-tags">
                <el-tag
                  v-for="tag in editForm.tags"
                  :key="tag.id"
                  :color="tag.color"
                  closable
                  @close="removeTag(tag.id)"
                  class="tag-item"
                >
                  {{ tag.name }}
                </el-tag>
              </div>
            </div>
          </el-form-item>

          <!-- 相关说明 -->
          <el-form-item label="相关说明">
            <div class="description-section">
              <el-input
                v-model="editForm.description_rich"
                type="textarea"
                :rows="6"
                placeholder="请输入相关说明..."
                maxlength="5000"
                show-word-limit
                class="description-textarea"
              />
              <div class="description-note">0 / 5000</div>
            </div>
          </el-form-item>
        </el-form>

        <!-- 完成按钮 -->
        <div class="form-footer">
          <el-button type="primary" @click="handleSave" class="save-btn"> 完成 </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.room-type-editor {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f5f5;
}

.editor-header {
  background: white;
  padding: 16px 20px;
  border-bottom: 1px solid #e8e8e8;
}

.header-nav {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 12px;
}

.back-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #666;
  font-size: 14px;
}

.nav-tabs {
  display: flex;
  gap: 20px;
}

.nav-tab {
  font-size: 14px;
  color: #666;
  position: relative;
}

.nav-tab.active {
  color: #1890ff;
}

.nav-tab.active::after {
  content: '';
  position: absolute;
  bottom: -16px;
  left: 0;
  right: 0;
  height: 2px;
  background: #1890ff;
}

.editor-title {
  font-size: 16px;
  font-weight: 500;
  color: #333;
  margin: 0;
}

.editor-content {
  flex: 1;
  display: flex;
  gap: 20px;
  padding: 20px;
  min-height: 0;
}

/* 左侧预览卡片 */
.preview-section {
  width: 350px;
  flex-shrink: 0;
}

.preview-card {
  background: white;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  position: sticky;
  top: 20px;
}

.card-image {
  width: 100%;
  height: 200px;
  overflow: hidden;
}

.card-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.card-content {
  padding: 16px;
}

.card-title {
  font-size: 18px;
  font-weight: 600;
  color: #333;
  margin: 0 0 16px 0;
}

.date-section {
  margin-bottom: 16px;
}

.date-group {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px;
  background: #f8f9fa;
  border-radius: 6px;
}

.date-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  flex: 1;
}

.date-separator {
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 16px;
}

.separator-text {
  font-size: 12px;
  color: #666;
  background: white;
  padding: 2px 8px;
  border-radius: 12px;
}

.date-label {
  font-size: 12px;
  color: #666;
}

.date-value {
  font-size: 14px;
  color: #333;
  font-weight: 500;
}

.room-basic-info {
  margin-bottom: 16px;
}

.info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px 16px;
}

.info-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 0;
}

.info-label {
  font-size: 14px;
  color: #666;
}

.info-value {
  font-size: 14px;
  color: #333;
}

.amenities-section h4,
.tags-section h4,
.cancellation-policy h4,
.description-section h4 {
  font-size: 14px;
  color: #333;
  margin: 0 0 8px 0;
}

.amenities-list {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px;
  margin-bottom: 16px;
}

.amenity-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #666;
}

.amenity-icon {
  font-size: 12px;
  color: #52c41a;
}

.more-amenities {
  grid-column: 1 / -1;
  text-align: center;
  font-size: 12px;
  color: #1890ff;
  padding: 4px;
  border: 1px dashed #1890ff;
  border-radius: 4px;
  background: #f0f7ff;
}

.tags-section {
  margin-bottom: 16px;
}

.preview-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.preview-tag {
  margin-right: 0;
}

/* 退订政策样式 */
.cancellation-policy {
  margin-bottom: 16px;
}

.policy-tag {
  display: flex;
  align-items: flex-start;
}

.policy-badge {
  font-size: 12px;
  border-radius: 4px;
  padding: 2px 6px;
}

/* 相关说明样式 */
.description-section {
  margin-bottom: 16px;
}

.pricing-section {
  font-size: 12px;
  color: #999;
  line-height: 1.4;
}

.card-footer {
  padding: 16px;
  border-top: 1px solid #f0f0f0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.footer-actions {
  display: flex;
  gap: 16px;
}

.note-btn,
.subscribe-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #666;
  padding: 0;
}

.price-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.price-display {
  display: flex;
  align-items: baseline;
  gap: 2px;
}

.currency {
  font-size: 16px;
  color: #ff4d4f;
  font-weight: 500;
}

.price {
  font-size: 24px;
  color: #ff4d4f;
  font-weight: 600;
}

.book-btn {
  padding: 8px 24px;
}

/* 右侧表单区域 */
.form-section {
  flex: 1;
  background: white;
  border-radius: 8px;
  padding: 24px;
  overflow: auto;
}

.edit-form .el-form-item {
  margin-bottom: 24px;
}

.form-help {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}

/* 图片上传区域 */
.image-upload-section {
  width: 100%;
}

.image-list {
  display: flex;
  gap: 12px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.image-item {
  position: relative;
  width: 100px;
  height: 75px;
  border-radius: 6px;
  overflow: hidden;
  border: 1px solid #e8e8e8;
}

.image-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.image-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s;
}

.image-item:hover .image-overlay {
  opacity: 1;
}

.remove-btn {
  color: white;
  font-size: 12px;
}

.image-upload-btn {
  width: 100px;
  height: 75px;
  border: 2px dashed #d9d9d9;
  border-radius: 6px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  cursor: pointer;
  transition: border-color 0.3s;
}

.image-upload-btn:hover {
  border-color: #1890ff;
  color: #1890ff;
}

.image-upload-btn span {
  font-size: 12px;
}

.upload-help {
  font-size: 12px;
  color: #999;
  line-height: 1.4;
  margin-bottom: 8px;
}

/* 基础信息区域 */
.basic-info-section {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 16px;
  background: #f8f9fa;
  border-radius: 6px;
}

.info-row-form {
  display: flex;
  align-items: center;
  gap: 12px;
}

.info-label-form {
  font-size: 14px;
  color: #333;
  min-width: 80px;
}

.room-type-radios {
  display: flex;
  gap: 20px;
}

.area-input,
.capacity-input {
  width: 120px;
}

.unit {
  font-size: 14px;
  color: #666;
}

/* 户型输入样式 */
.room-layout-inputs {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  width: 100%;
}

.layout-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.layout-label {
  font-size: 14px;
  color: #333;
  min-width: 50px;
}

.layout-select {
  width: 80px;
}

/* 床型信息样式 */
.bed-info-list {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.bed-info-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.bed-type-input {
  width: 100px;
}

.bed-size-input {
  width: 120px;
}

.bed-count-input {
  width: 80px;
}

.add-bed-btn {
  align-self: flex-start;
}

/* 销售设置样式 */
.sales-settings-section {
  display: flex;
  flex-direction: column;
  gap: 20px;
  padding: 16px;
  background: #f8f9fa;
  border-radius: 6px;
  width: 100%;
}

.setting-row {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.setting-label {
  font-size: 14px;
  color: #333;
  font-weight: 500;
}

.breakfast-radios,
.acceptance-radios,
.cancellation-radios {
  display: flex;
  gap: 20px;
}

.time-picker {
  width: 150px;
}

.cancel-days-setting {
  display: flex;
  align-items: center;
  gap: 8px;
}

.cancel-days-input {
  width: 80px;
}

.setting-note {
  font-size: 12px;
  color: #666;
}

/* 设施管理样式 */
.amenities-management {
  width: 100%;
}

.amenity-search {
  margin-bottom: 16px;
  position: relative;
}

.search-input {
  width: 100%;
}

.search-results {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  background: white;
  border: 1px solid #e8e8e8;
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  z-index: 10;
  max-height: 200px;
  overflow-y: auto;
}

.results-header,
.amenities-header {
  display: grid;
  grid-template-columns: 1fr 1fr 100px;
  padding: 12px 16px;
  background: #f8f9fa;
  border-bottom: 1px solid #e8e8e8;
  font-size: 12px;
  color: #666;
  font-weight: 500;
}

.result-item,
.amenity-row {
  display: grid;
  grid-template-columns: 1fr 1fr 100px;
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
  align-items: center;
}

.result-category,
.amenity-category {
  font-size: 14px;
  color: #666;
}

.result-name,
.amenity-name {
  font-size: 14px;
  color: #333;
}

.selected-amenities {
  border: 1px solid #e8e8e8;
  border-radius: 4px;
  overflow: hidden;
}

.pagination-info {
  padding: 8px 16px;
  font-size: 12px;
  color: #666;
  background: #f8f9fa;
}

/* 标签管理样式 */
.tags-management {
  width: 100%;
}

.tag-note {
  font-size: 12px;
  color: #666;
  margin-bottom: 16px;
  line-height: 1.4;
}

.add-tag-section {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.tag-input {
  width: 200px;
}

.selected-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tag-item {
  margin-right: 0;
}

/* 相关说明样式 */
.description-section {
  width: 100%;
}

.description-textarea {
  width: 100%;
}

.description-note {
  text-align: right;
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}

.form-footer {
  padding-top: 24px;
  border-top: 1px solid #e8e8e8;
  display: flex;
  justify-content: center;
}

.save-btn {
  min-width: 120px;
  padding: 10px 24px;
}
</style>
