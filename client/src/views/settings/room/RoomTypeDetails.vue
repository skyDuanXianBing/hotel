<template>
  <div class="room-type-details-container">
    <!-- 面包屑导航 -->
    <div class="breadcrumb-section">
      <el-breadcrumb separator=">">
        <el-breadcrumb-item @click="handleBack" class="breadcrumb-link">房型</el-breadcrumb-item>
        <el-breadcrumb-item>详情</el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <!-- 标签页 -->
    <div class="content-section">
      <el-tabs v-model="activeTab" class="room-tabs">
        <!-- 基本信息标签 -->
        <el-tab-pane label="基本信息" name="basic">
          <div class="tab-content">
            <div class="info-notice">
              在此设置显示在您客源网站上的信息内容
            </div>

            <el-form :model="formData" label-width="100px" class="detail-form">
              <el-form-item label="房间名称" required>
                <el-input v-model="formData.name" placeholder="请输入房间名称" maxlength="64" show-word-limit />
                <div class="form-tip">英文 *</div>
                <el-input v-model="formData.nameEn" placeholder="请输入英文名称" class="mt-8" />
                <div class="add-btn-wrapper">
                  <el-button link type="primary">+ 本地化</el-button>
                </div>
              </el-form-item>

              <el-form-item label="房间描述" required>
                <el-input
                  v-model="formData.description"
                  type="textarea"
                  :rows="5"
                  placeholder="请输入房间描述"
                  maxlength="1200"
                  show-word-limit
                />
                <div class="form-tip">英文 *</div>
                <el-input
                  v-model="formData.descriptionEn"
                  type="textarea"
                  :rows="5"
                  placeholder="请输入英文描述"
                  class="mt-8"
                  maxlength="1200"
                  show-word-limit
                />
                <div class="add-btn-wrapper">
                  <el-button link type="primary">+ 本地化</el-button>
                </div>
              </el-form-item>

              <el-form-item label="房间大小" required>
                <el-input v-model="formData.size" type="number" style="width: 200px">
                  <template #suffix>m²</template>
                </el-input>
              </el-form-item>

              <el-form-item label="空间">
                <div class="space-config">
                  <div class="space-item">
                    <label>卧室</label>
                    <el-select v-model="formData.bedrooms" style="width: 100px">
                      <el-option :value="1" label="1" />
                      <el-option :value="2" label="2" />
                      <el-option :value="3" label="3" />
                    </el-select>
                  </div>
                  <div class="space-item">
                    <label>浴室空间</label>
                    <el-select v-model="formData.bathrooms" style="width: 100px">
                      <el-option :value="1" label="1" />
                      <el-option :value="2" label="2" />
                    </el-select>
                  </div>
                  <div class="space-item">
                    <label>客厅</label>
                    <el-select v-model="formData.livingRooms" style="width: 100px">
                      <el-option :value="0" label="0" />
                      <el-option :value="1" label="1" />
                    </el-select>
                  </div>
                  <div class="space-item">
                    <label>厨房</label>
                    <el-select v-model="formData.kitchens" style="width: 100px">
                      <el-option :value="0" label="0" />
                      <el-option :value="1" label="1" />
                    </el-select>
                  </div>
                  <div class="space-item">
                    <label>餐台</label>
                    <el-select v-model="formData.diningAreas" style="width: 100px">
                      <el-option :value="0" label="0" />
                      <el-option :value="1" label="1" />
                    </el-select>
                  </div>
                </div>
              </el-form-item>

              <el-form-item label="睡眠安排">
                <div class="sleep-config">
                  <div class="sleep-item">
                    <label>床类型</label>
                    <el-select v-model="formData.bedType" style="width: 150px">
                      <el-option value="double" label="双人床" />
                      <el-option value="single" label="单人床" />
                    </el-select>
                  </div>
                  <div class="sleep-item">
                    <label>床数量</label>
                    <el-input-number v-model="formData.bedCount" :min="1" style="width: 100px" />
                  </div>
                  <div class="add-btn-wrapper">
                    <el-button link type="primary">+ 添加</el-button>
                  </div>
                </div>
              </el-form-item>
            </el-form>

            <div class="form-actions">
              <el-button @click="handleBack">取消</el-button>
              <el-button type="primary" @click="handleSave">保存</el-button>
            </div>
          </div>
        </el-tab-pane>

        <!-- 设施标签 -->
        <el-tab-pane label="设施" name="facilities">
          <div class="tab-content">
            <div class="facilities-section">
              <h3 class="section-title">热门设施</h3>
              <div class="facility-grid">
                <el-checkbox v-model="facilities.airConditioning">空调</el-checkbox>
                <el-checkbox v-model="facilities.heating">暖气</el-checkbox>
                <el-checkbox v-model="facilities.noSmoking">禁止吸烟</el-checkbox>
                <el-checkbox v-model="facilities.tv">电视</el-checkbox>
                <el-checkbox v-model="facilities.wifi">WiFi</el-checkbox>
              </div>
            </div>

            <div class="facilities-section">
              <h3 class="section-title">浴室设施</h3>
              <div class="facility-grid">
                <el-checkbox v-model="facilities.bathtub">浴缸</el-checkbox>
                <el-checkbox v-model="facilities.toiletries">清洁用品</el-checkbox>
                <el-checkbox v-model="facilities.towel">毛巾</el-checkbox>
                <el-checkbox v-model="facilities.shower">淋浴器</el-checkbox>
                <el-checkbox v-model="facilities.hairDryer">吹风机</el-checkbox>
                <el-checkbox v-model="facilities.slippers">拖鞋</el-checkbox>
                <el-checkbox v-model="facilities.bathtub2">浴缸</el-checkbox>
                <el-checkbox v-model="facilities.separateShower">独立淋浴和浴缸</el-checkbox>
                <el-checkbox v-model="facilities.toiletries2">洗漱用品</el-checkbox>
              </div>
            </div>

            <div class="facilities-section">
              <h3 class="section-title">餐饮设施</h3>
              <div class="facility-grid">
                <el-checkbox v-model="facilities.teaCoffee">茶/咖啡设施</el-checkbox>
                <el-checkbox v-model="facilities.tableware">餐具</el-checkbox>
                <el-checkbox v-model="facilities.dinnerware">餐具和器皿</el-checkbox>
                <el-checkbox v-model="facilities.iceBox">冰箱</el-checkbox>
                <el-checkbox v-model="facilities.grill">烤箱</el-checkbox>
                <el-checkbox v-model="facilities.wine">酒杯</el-checkbox>
                <el-checkbox v-model="facilities.service24h">24小时客房服务</el-checkbox>
                <el-checkbox v-model="facilities.hotWater">电热水壶</el-checkbox>
                <el-checkbox v-model="facilities.microwave">迷你吧</el-checkbox>
                <el-checkbox v-model="facilities.coffeeService">咖啡和茶设施</el-checkbox>
                <el-checkbox v-model="facilities.stove">炉灶</el-checkbox>
                <el-checkbox v-model="facilities.cutlery">卫星频道</el-checkbox>
              </div>
            </div>

            <div class="facilities-section">
              <h3 class="section-title">娱乐设施</h3>
              <div class="facility-grid">
                <el-checkbox v-model="facilities.newspaper">报纸</el-checkbox>
                <el-checkbox v-model="facilities.cableTv">付费电视</el-checkbox>
                <el-checkbox v-model="facilities.satellite">卫星频道</el-checkbox>
                <el-checkbox v-model="facilities.phone">电话</el-checkbox>
                <el-checkbox v-model="facilities.radio">收音机</el-checkbox>
              </div>
            </div>

            <div class="facilities-section">
              <h3 class="section-title">更多设施</h3>
              <div class="facility-grid">
                <el-checkbox v-model="facilities.wardrobe">衣物储存</el-checkbox>
                <el-checkbox v-model="facilities.dryer">烘干机</el-checkbox>
                <el-checkbox v-model="facilities.rack">衣架</el-checkbox>
                <el-checkbox v-model="facilities.mosquito">蚊帐</el-checkbox>
                <el-checkbox v-model="facilities.washer">洗衣机</el-checkbox>
                <el-checkbox v-model="facilities.curtain">遮光窗帘</el-checkbox>
                <el-checkbox v-model="facilities.toys">提供婴儿床</el-checkbox>
                <el-checkbox v-model="facilities.board">婴斗和婴衣板</el-checkbox>
              </div>
            </div>

            <div class="form-actions">
              <el-button @click="handleBack">取消</el-button>
              <el-button type="primary" @click="handleSaveFacilities">保存</el-button>
            </div>
          </div>
        </el-tab-pane>

        <!-- 照片标签 -->
        <el-tab-pane label="照片" name="photos">
          <div class="tab-content">
            <div class="photo-section">
              <h3 class="section-title">展示在电脑上的照片</h3>
              <div class="photo-info">
                <p>最大数量: 40</p>
                <p>最大尺寸: 图片5MB、视频40MB</p>
                <p>推荐尺寸: 900px×600px (3:2)</p>
              </div>
              <div class="upload-area">
                <el-upload
                  class="upload-box"
                  drag
                  :action="uploadUrl"
                  :on-success="handleUploadSuccess"
                  :before-upload="beforeUpload"
                  list-type="picture-card"
                  :file-list="desktopPhotos"
                >
                  <el-icon class="upload-icon"><Upload /></el-icon>
                  <div class="upload-text">上传</div>
                </el-upload>
              </div>
            </div>

            <div class="photo-section">
              <h3 class="section-title">展示在手机上的照片</h3>
              <div class="photo-info">
                <p>最大数量: 40</p>
                <p>最大尺寸: 图片5MB、视频40MB</p>
                <p>推荐尺寸: 750px×450px (5:3)</p>
              </div>
              <div class="upload-area">
                <el-upload
                  class="upload-box"
                  drag
                  :action="uploadUrl"
                  :on-success="handleUploadSuccess"
                  :before-upload="beforeUpload"
                  list-type="picture-card"
                  :file-list="mobilePhotos"
                >
                  <el-icon class="upload-icon"><Upload /></el-icon>
                  <div class="upload-text">上传</div>
                </el-upload>
              </div>
            </div>

            <div class="form-actions">
              <el-button @click="handleBack">取消</el-button>
              <el-button type="primary" @click="handleSavePhotos">保存</el-button>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Upload } from '@element-plus/icons-vue'
import { getRoomTypeById, type RoomTypeDTO } from '@/api/roomType'

const router = useRouter()
const route = useRoute()

const activeTab = ref('basic')
const uploadUrl = ref('/api/v1/upload') // 上传接口URL

// 基本信息表单
const formData = reactive({
  id: 0,
  name: '',
  nameEn: '',
  description: '',
  descriptionEn: '',
  size: 0,
  bedrooms: 1,
  bathrooms: 1,
  livingRooms: 0,
  kitchens: 0,
  diningAreas: 0,
  bedType: 'double',
  bedCount: 1,
})

// 设施配置
const facilities = reactive({
  // 热门设施
  airConditioning: false,
  heating: false,
  noSmoking: false,
  tv: false,
  wifi: false,
  // 浴室设施
  bathtub: false,
  toiletries: false,
  towel: false,
  shower: false,
  hairDryer: false,
  slippers: false,
  bathtub2: false,
  separateShower: false,
  toiletries2: false,
  // 餐饮设施
  teaCoffee: false,
  tableware: false,
  dinnerware: false,
  iceBox: false,
  grill: false,
  wine: false,
  service24h: false,
  hotWater: false,
  microwave: false,
  coffeeService: false,
  stove: false,
  cutlery: false,
  // 娱乐设施
  newspaper: false,
  cableTv: false,
  satellite: false,
  phone: false,
  radio: false,
  // 更多设施
  wardrobe: false,
  dryer: false,
  rack: false,
  mosquito: false,
  washer: false,
  curtain: false,
  toys: false,
  board: false,
})

// 照片列表
const desktopPhotos = ref<any[]>([])
const mobilePhotos = ref<any[]>([])

// 加载房型详情
const loadRoomTypeDetails = async () => {
  const roomTypeId = route.params.id as string
  if (!roomTypeId) {
    ElMessage.error('房型ID不存在')
    handleBack()
    return
  }

  try {
    const response = await getRoomTypeById(Number(roomTypeId))
    if (response.success && response.data) {
      const data = response.data
      formData.id = data.id
      formData.name = data.name
      formData.description = data.description || ''
      // TODO: 加载其他字段
    } else {
      ElMessage.error(response.message || '加载房型详情失败')
    }
  } catch (error) {
    console.error('加载房型详情失败:', error)
    ElMessage.error('加载房型详情失败')
  }
}

// 返回
const handleBack = () => {
  router.back()
}

// 保存基本信息
const handleSave = () => {
  ElMessage.success('保存成功')
}

// 保存设施信息
const handleSaveFacilities = () => {
  ElMessage.success('设施信息保存成功')
}

// 保存照片
const handleSavePhotos = () => {
  ElMessage.success('照片保存成功')
}

// 上传成功回调
const handleUploadSuccess = (response: any) => {
  ElMessage.success('上传成功')
}

// 上传前验证
const beforeUpload = (file: File) => {
  const isImage = file.type.startsWith('image/')
  const isVideo = file.type.startsWith('video/')
  const isLt5M = file.size / 1024 / 1024 < 5
  const isLt40M = file.size / 1024 / 1024 < 40

  if (!isImage && !isVideo) {
    ElMessage.error('只能上传图片或视频文件!')
    return false
  }

  if (isImage && !isLt5M) {
    ElMessage.error('图片大小不能超过 5MB!')
    return false
  }

  if (isVideo && !isLt40M) {
    ElMessage.error('视频大小不能超过 40MB!')
    return false
  }

  return true
}

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

/* 面包屑 */
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

/* 内容区 */
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

/* 信息提示 */
.info-notice {
  background: #e6f7ff;
  border: 1px solid #91d5ff;
  border-radius: 4px;
  padding: 12px 16px;
  margin-bottom: 24px;
  color: #0050b3;
  font-size: 14px;
}

/* 表单 */
.detail-form {
  max-width: 800px;
}

.form-tip {
  font-size: 12px;
  color: #666;
  margin-top: 4px;
}

.mt-8 {
  margin-top: 8px;
}

.add-btn-wrapper {
  margin-top: 8px;
}

/* 空间配置 */
.space-config {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
}

.space-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.space-item label {
  font-size: 14px;
  color: #666;
  min-width: 80px;
}

/* 睡眠配置 */
.sleep-config {
  display: flex;
  align-items: center;
  gap: 16px;
}

.sleep-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.sleep-item label {
  font-size: 14px;
  color: #666;
}

/* 设施部分 */
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

/* 照片部分 */
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
  width: 148px;
  height: 148px;
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

/* 表单操作按钮 */
.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 24px;
  border-top: 1px solid #e8e8e8;
  margin-top: 32px;
}

/* Element Plus 样式覆盖 */
:deep(.el-tabs__content) {
  height: calc(100% - 55px);
  overflow: auto;
}

:deep(.el-checkbox) {
  margin-right: 0;
}
</style>
