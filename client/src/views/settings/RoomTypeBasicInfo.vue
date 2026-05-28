<script setup lang="ts">
import { ref, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import type { BaseRoomType } from '@/types/room'

interface RoomTypeBasicForm {
  id: number
  name: string
  shortName: string
  pricingType: 'fixed' | 'flexible'
  fixedPrice: number
  weekdayPrice: number
  weekendPrice: number
  roomCount: number
  roomNumbers: string[]
}

const props = defineProps<{
  roomType: BaseRoomType
}>()

const emit = defineEmits<{
  back: []
  save: [data: RoomTypeBasicForm]
}>()

const { t } = useI18n()

// 表单数据
const basicForm = ref<RoomTypeBasicForm>({
  id: props.roomType.id,
  name: props.roomType.name,
  shortName: props.roomType.description || props.roomType.name,
  pricingType: props.roomType.weekdayPrice || props.roomType.weekendPrice ? 'flexible' : 'fixed',
  fixedPrice: props.roomType.defaultPrice || 0.0,
  weekdayPrice: props.roomType.weekdayPrice || 0.0,
  weekendPrice: props.roomType.weekendPrice || 0.0,
  roomCount: props.roomType.roomNumbers?.length || props.roomType.totalRooms || 1,
  roomNumbers: props.roomType.roomNumbers || [],
})

// 字符计数
const nameCount = computed(() => basicForm.value.name.length)
const shortNameCount = computed(() => basicForm.value.shortName.length)

// 保存
const handleSave = () => {
  if (!basicForm.value.name.trim()) {
    ElMessage.warning(t('settingsStage4.roomTypeManagement.messages.nameRequired'))
    return
  }
  if (!basicForm.value.shortName.trim()) {
    ElMessage.warning(t('settingsStage4.roomTypeManagement.messages.shortNameRequired'))
    return
  }

  emit('save', basicForm.value)
}

// 保存并完善信息
const handleSaveAndComplete = () => {
  handleSave()
}

// 取消
const handleCancel = () => {
  emit('back')
}
</script>

<template>
  <div class="room-type-basic-info">
    <!-- 页面标题 -->
    <div class="page-header">
      <h2 class="page-title">{{ t('settingsStage4.roomTypeManagement.actions.editRoomType') }}</h2>
    </div>

    <!-- 表单内容 -->
    <div class="form-container">
      <el-form :model="basicForm" label-width="100px" class="basic-form">
        <!-- 房型名称 -->
        <el-form-item :label="t('settingsStage4.roomTypeManagement.fields.roomTypeName')" required>
          <div class="input-with-count">
            <el-input
              v-model="basicForm.name"
              :placeholder="t('settingsStage4.roomTypeManagement.placeholders.roomTypeName')"
              maxlength="30"
              class="name-input"
            />
            <span class="char-count">{{ nameCount }} / 30</span>
          </div>
        </el-form-item>

        <!-- 房型简称 -->
        <el-form-item :label="t('settingsStage4.roomTypeManagement.fields.shortName')" required>
          <div class="input-with-count">
            <el-input
              v-model="basicForm.shortName"
              :placeholder="t('settingsStage4.roomTypeManagement.placeholders.shortName')"
              maxlength="20"
              class="short-name-input"
            />
            <span class="char-count">{{ shortNameCount }} / 20</span>
          </div>
        </el-form-item>

        <!-- 房间默认价 -->
        <el-form-item :label="t('settingsStage4.roomTypeManagement.fields.defaultRoomPrice')" required>
          <div class="pricing-section">
            <el-radio-group v-model="basicForm.pricingType" class="pricing-radios">
              <el-radio label="fixed">{{ t('settingsStage4.roomTypeManagement.options.fixedDailyPrice') }}</el-radio>
              <el-radio label="flexible">{{ t('settingsStage4.roomTypeManagement.options.weekdayWeekendPrice') }}</el-radio>
            </el-radio-group>

            <div class="price-inputs">
              <div v-if="basicForm.pricingType === 'fixed'" class="fixed-price">
                <span class="currency">¥</span>
                <el-input-number
                  v-model="basicForm.fixedPrice"
                  :min="0"
                  :precision="2"
                  :step="0.01"
                  placeholder="0.00"
                  class="price-input"
                />
              </div>

              <div v-else class="flexible-price">
                <div class="price-group">
                  <span class="price-label">{{ t('settingsStage4.roomTypeManagement.fields.weekdayPrice') }}</span>
                  <span class="currency">¥</span>
                  <el-input-number
                    v-model="basicForm.weekdayPrice"
                    :min="0"
                    :precision="2"
                    :step="0.01"
                    placeholder="0.00"
                    class="price-input"
                  />
                </div>
                <div class="price-group">
                  <span class="price-label">{{ t('settingsStage4.roomTypeManagement.fields.weekendPrice') }}</span>
                  <span class="currency">¥</span>
                  <el-input-number
                    v-model="basicForm.weekendPrice"
                    :min="0"
                    :precision="2"
                    :step="0.01"
                    placeholder="0.00"
                    class="price-input"
                  />
                </div>
              </div>
            </div>
          </div>
        </el-form-item>

        <!-- 房间数量 -->
        <el-form-item :label="t('settingsStage4.roomTypeManagement.fields.roomCount')" required>
          <div class="room-count-section">
            <span class="count-text">
              {{ t('settingsStage4.roomTypeManagement.units.roomsCount', { count: basicForm.roomCount }) }}
            </span>
          </div>
        </el-form-item>

        <!-- 房间号 -->
        <el-form-item :label="t('settingsStage4.roomTypeManagement.fields.roomNumbers')">
          <div class="room-numbers-section">
            <div class="room-numbers-info">
              <span>{{ t('settingsStage4.roomTypeManagement.hints.roomNumbersManagePrefix') }}</span>
              <el-button type="primary" link class="manage-link">
                {{ t('settingsStage4.roomTypeManagement.hints.roomManagementLink') }}
              </el-button>
              <span>{{ t('settingsStage4.roomTypeManagement.hints.roomNumbersManageSuffix') }}</span>
            </div>
            <div class="room-number-display">
              <div
                v-for="(roomNumber, index) in basicForm.roomNumbers"
                :key="index"
                class="room-number-tag"
              >
                {{ roomNumber }}
              </div>
            </div>
          </div>
        </el-form-item>
      </el-form>

      <!-- 底部按钮 -->
      <div class="form-footer">
        <el-button @click="handleCancel" class="cancel-btn">{{ t('settings.common.cancel') }}</el-button>
        <el-button type="primary" @click="handleSave" class="save-btn">{{ t('settings.common.save') }}</el-button>
        <el-button type="primary" @click="handleSaveAndComplete" class="save-complete-btn"
          >{{ t('settingsStage4.roomTypeManagement.actions.saveAndComplete') }}</el-button
        >
      </div>
    </div>
  </div>
</template>

<style scoped>
.room-type-basic-info {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f5f5;
}

.page-header {
  background: white;
  padding: 20px;
  border-bottom: 1px solid #e8e8e8;
}

.page-title {
  font-size: 18px;
  font-weight: 600;
  color: #333;
  margin: 0;
}

.form-container {
  flex: 1;
  padding: 40px;
  overflow: auto;
  display: flex;
  flex-direction: column;
  max-width: 800px;
  margin: 0 auto;
  width: 100%;
}

.basic-form {
  flex: 1;
}

.basic-form .el-form-item {
  margin-bottom: 32px;
}

.basic-form .el-form-item__label {
  font-weight: 500;
  color: #333;
  font-size: 14px;
}

/* 输入框字符计数 */
.input-with-count {
  position: relative;
  display: flex;
  align-items: center;
  gap: 16px;
}

.name-input,
.short-name-input {
  flex: 1;
  max-width: 400px;
}

.char-count {
  font-size: 12px;
  color: #999;
  white-space: nowrap;
}

/* 定价区域 */
.pricing-section {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.pricing-radios {
  display: flex;
  gap: 32px;
}

.price-inputs {
  padding: 20px;
  background: #f8f9fa;
  border-radius: 6px;
}

.fixed-price {
  display: flex;
  align-items: center;
  gap: 8px;
}

.flexible-price {
  display: flex;
  gap: 40px;
}

.price-group {
  display: flex;
  align-items: center;
  gap: 8px;
}

.price-label {
  font-size: 14px;
  color: #666;
  min-width: 60px;
}

.currency {
  font-size: 16px;
  color: #333;
  font-weight: 500;
}

.price-input {
  width: 140px;
}

/* 房间数量 */
.room-count-section {
  display: flex;
  align-items: center;
}

.count-text {
  font-size: 14px;
  color: #333;
  padding: 8px 12px;
  background: #f8f9fa;
  border-radius: 4px;
}

/* 房间号区域 */
.room-numbers-section {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.room-numbers-info {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 14px;
  color: #666;
}

.manage-link {
  padding: 0;
  font-size: 14px;
}

.room-number-display {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.room-number-tag {
  padding: 8px 12px;
  background: #f0f0f0;
  border-radius: 4px;
  font-size: 14px;
  color: #333;
  border: 1px solid #e0e0e0;
}

/* 底部按钮 */
.form-footer {
  display: flex;
  justify-content: center;
  gap: 16px;
  padding-top: 40px;
  border-top: 1px solid #e8e8e8;
  margin-top: 20px;
}

.cancel-btn {
  min-width: 80px;
}

.save-btn,
.save-complete-btn {
  min-width: 120px;
}

.save-complete-btn {
  background: #1890ff;
  border-color: #1890ff;
}
</style>
