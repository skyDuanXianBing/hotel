<template>
  <div class="auto-checkin-container">
    <!-- 门店卡片列表 -->
    <div v-if="!showConfigPage" class="container-with-notice">
      <!-- 顶部提示信息 -->
      <div class="notice-banner">
        <span>{{ t('settingsStage4.autoCheckin.notice.configureStoreFirst') }}</span>
      </div>

      <!-- 门店卡片网格 -->
      <div class="stores-grid">
        <div
          v-for="store in stores"
          :key="store.id"
          class="store-card"
        >
          <div class="card-header">
            <el-icon class="collapse-icon"><Minus /></el-icon>
            <h3 class="store-name">{{ store.name }}</h3>
          </div>
          <el-button
            type="primary"
            class="config-button"
            @click="handleConfig(store)"
          >
            {{ t('settingsStage4.autoCheckin.actions.configure') }}
          </el-button>
          <div class="status-row">
            <span class="status-label">{{ t('settingsStage4.autoCheckin.status.disabled') }}</span>
            <el-switch v-model="store.enabled" />
          </div>
        </div>
      </div>
    </div>

    <!-- 配置详情页面 -->
    <div v-else class="config-page">
      <!-- 面包屑导航 -->
      <div class="breadcrumb-nav">
        <el-breadcrumb separator="›">
          <el-breadcrumb-item>
            <a @click="handleBack">{{ t('settings.layout.fallbackTitle') }}</a>
          </el-breadcrumb-item>
          <el-breadcrumb-item>{{ currentStore?.name }}</el-breadcrumb-item>
        </el-breadcrumb>
      </div>

      <!-- 标签页 -->
      <el-tabs v-model="activeTab" class="config-tabs">
        <!-- 基本信息标签页 -->
        <el-tab-pane :label="t('settingsStage4.autoCheckin.tabs.basicInfo')" name="basic-info">
          <div class="tab-content">
            <!-- 自助入住开关 -->
            <div class="setting-item">
              <div class="setting-header">
                <h3 class="setting-title">{{ t('settingsStage4.autoCheckin.basic.selfCheckin') }}</h3>
                <el-switch v-model="basicConfig.selfCheckin" />
              </div>
              <p class="setting-desc">
                {{ t('settingsStage4.autoCheckin.basic.selfCheckinDescBefore') }}
                <a href="javascript:;" class="link-text">{{ t('settingsStage4.autoCheckin.basic.selfCheckinLink') }}</a>
                {{ t('settingsStage4.autoCheckin.basic.selfCheckinDescAfter') }}
              </p>
            </div>

            <!-- 通用入住链接 -->
            <div class="setting-item">
              <h3 class="setting-title">{{ t('settingsStage4.autoCheckin.basic.commonLink') }}</h3>
              <p class="setting-desc">
                {{ t('settingsStage4.autoCheckin.basic.commonLinkDesc') }}
              </p>
              <div class="link-row">
                <span class="link-text">{{ selfCheckinUrl }}</span>
                <el-button type="primary" size="small" @click="handleCopyLink">{{ t('settingsStage4.autoCheckin.actions.copy') }}</el-button>
              </div>
            </div>

            <!-- 门店二维码 -->
            <div class="setting-item">
              <h3 class="setting-title">{{ t('settingsStage4.autoCheckin.basic.storeQrCode') }}</h3>
              <p class="setting-desc">
                {{ t('settingsStage4.autoCheckin.basic.storeQrCodeDesc') }}
              </p>
              <div class="qrcode-section">
                <div class="qrcode-wrapper">
                  <div class="qrcode-placeholder">
                    <span>{{ t('settingsStage4.autoCheckin.status.loading') }}</span>
                  </div>
                </div>
                <el-button type="primary" size="small" @click="handleDownloadQR">{{ t('settingsStage4.autoCheckin.actions.download') }}</el-button>
              </div>
            </div>

            <!-- 文件上传 -->
            <div class="setting-item">
              <h3 class="setting-title">{{ t('settingsStage4.autoCheckin.basic.files') }}</h3>
              <p class="setting-desc">
                {{ t('settingsStage4.autoCheckin.basic.filesDesc') }}
              </p>
              <div class="upload-section">
                <el-button class="upload-button">
                  <el-icon><Upload /></el-icon>
                  {{ t('settingsStage4.autoCheckin.actions.upload') }}
                </el-button>
                <p class="upload-hint">{{ t('settingsStage4.autoCheckin.basic.uploadHint') }}</p>
              </div>
            </div>
          </div>
        </el-tab-pane>

        <!-- 入住登记标签页 -->
        <el-tab-pane :label="t('settingsStage4.autoCheckin.tabs.checkin')" name="checkin">
          <div class="tab-content">
            <!-- 入住表单配置 -->
            <div class="checkin-section">
              <div class="section-header-row">
                <h3 class="section-title">{{ t('settingsStage4.autoCheckin.checkin.guestRegistration') }}</h3>
                <div class="action-buttons">
                  <el-button type="text">{{ t('settingsStage4.common.applyToAll') }}</el-button>
                  <el-button type="primary">{{ t('settingsStage4.autoCheckin.actions.preview') }}</el-button>
                </div>
              </div>

              <div class="section-switch-row">
                <div class="switch-content">
                  <h4 class="switch-title">{{ t('settingsStage4.autoCheckin.checkin.form') }}</h4>
                  <p class="switch-desc">{{ t('settingsStage4.autoCheckin.checkin.formDesc') }}</p>
                </div>
                <el-switch v-model="checkinConfig.formEnabled" />
              </div>

              <!-- 表单字段列表 -->
              <div class="form-fields-list">
                <div
                  v-for="(field, index) in checkinConfig.formFields"
                  :key="field.id"
                  class="field-item"
                >
                  <div class="field-drag">
                    <el-icon><Menu /></el-icon>
                    <el-icon class="field-icon"><Document /></el-icon>
                    <span class="field-name">{{ getFieldName(field) }}</span>
                  </div>
                  <div class="field-actions">
                    <el-tag :type="field.required ? 'primary' : 'info'" size="small">
                      {{ field.required ? t('settingsStage4.autoCheckin.status.required') : t('settingsStage4.autoCheckin.status.optional') }}
                    </el-tag>
                    <el-button link type="danger" :icon="Delete" />
                  </div>
                </div>

                <!-- 模板库按钮 -->
                <div class="template-section">
                  <h4 class="template-title">{{ t('settingsStage4.autoCheckin.checkin.templateLibrary') }}</h4>
                  <div class="template-buttons">
                    <el-button
                      v-for="template in templateFields"
                      :key="template.id"
                      class="template-button"
                      @click="handleAddField(template)"
                    >
                      <el-icon><Document /></el-icon>
                      {{ getFieldName(template) }}
                      <el-icon class="add-icon"><Plus /></el-icon>
                    </el-button>
                  </div>
                </div>
              </div>
            </div>

            <!-- 身份验证 -->
            <div class="verification-section">
              <div class="section-switch-row">
                <div class="switch-content">
                  <h4 class="switch-title">{{ t('settingsStage4.autoCheckin.checkin.identityVerification') }}</h4>
                  <p class="switch-desc">{{ t('settingsStage4.autoCheckin.checkin.identityVerificationDesc') }}</p>
                </div>
                <el-switch v-model="checkinConfig.verificationEnabled" />
              </div>
            </div>
          </div>
        </el-tab-pane>

        <!-- 入住指引标签页 -->
        <el-tab-pane :label="t('settingsStage4.autoCheckin.tabs.guide')" name="guide">
          <div class="tab-content">
            <!-- 显示设置 -->
            <div class="guide-section">
              <div class="guide-header">
                <h3 class="section-title">{{ t('settingsStage4.autoCheckin.guide.displaySettings') }}</h3>
                <el-button type="text">{{ t('settingsStage4.common.applyToAll') }}</el-button>
              </div>

              <div class="display-time-row">
                <div>
                  <h4 class="setting-label">{{ t('settingsStage4.autoCheckin.guide.displayTime') }}</h4>
                  <p class="setting-desc">{{ t('settingsStage4.autoCheckin.guide.displayTimeDesc') }}</p>
                </div>
                <div class="time-input-group">
                  <el-input-number
                    v-model="guideConfig.displayHours"
                    :min="0"
                    :max="999"
                    controls-position="right"
                  />
                  <span class="time-unit">{{ t('settingsStage4.autoCheckin.guide.hours') }}</span>
                  <el-button type="primary">{{ t('settings.common.edit') }}</el-button>
                </div>
              </div>

              <div class="display-condition-row">
                <div>
                  <h4 class="setting-label">{{ t('settingsStage4.autoCheckin.guide.displayConditions') }}</h4>
                  <p class="setting-desc">{{ t('settingsStage4.autoCheckin.guide.displayConditionsDesc') }}</p>
                </div>
                <el-button type="primary">{{ t('settings.common.edit') }}</el-button>
              </div>

              <div class="conditions-list">
                <div class="condition-item">
                  <span class="condition-label">{{ t('settingsStage4.autoCheckin.guide.payment') }}</span>
                  <span class="condition-value">{{ t('settingsStage4.autoCheckin.guide.fullPaymentRequired') }}</span>
                </div>
                <div class="condition-item">
                  <span class="condition-label">{{ t('settingsStage4.autoCheckin.guide.guestForm') }}</span>
                  <span class="condition-value">{{ t('settingsStage4.autoCheckin.guide.allGuestsSubmitForm') }}</span>
                </div>
              </div>
            </div>

            <!-- 入住指引表格 -->
            <div class="guide-table-section">
              <h3 class="section-title">{{ t('settingsStage4.autoCheckin.tabs.guide') }}</h3>
              <el-table :data="guideConfig.guideList" border stripe>
                <el-table-column prop="roomType" :label="t('settingsStage4.cleaningSettings.fields.roomType')" min-width="150" />
                <el-table-column prop="room" :label="t('settingsStage4.autoCheckin.guide.room')" min-width="120" />
                <el-table-column prop="checkinDevice" :label="t('settingsStage4.autoCheckin.guide.checkinDevice')" min-width="150" />
                <el-table-column prop="checkinMethod" :label="t('settingsStage4.autoCheckin.guide.checkinMethod')" min-width="150" />
                <el-table-column :label="t('settingsStage4.accountList.columns.actions')" width="250" align="center" fixed="right">
                  <template #default="{ row }">
                    <el-button link type="primary" @click="handleEditGuide(row)">{{ t('settings.common.edit') }}</el-button>
                    <el-button link type="danger" @click="handleDeleteGuide(row)">{{ t('settingsStage4.autoCheckin.actions.clear') }}</el-button>
                    <el-button link type="primary" @click="handlePreviewGuide(row)">{{ t('settingsStage4.autoCheckin.actions.preview') }}</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { Minus, Upload, Menu, Document, Delete, Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'

interface Store {
  id: number
  name: string
  enabled: boolean
}

interface BasicConfig {
  selfCheckin: boolean
}

interface FormField {
  id: number
  name?: string
  nameKey?: string
  required: boolean
  type: string
}

interface CheckinConfig {
  formEnabled: boolean
  formFields: FormField[]
  verificationEnabled: boolean
}

interface GuideItem {
  id: number
  roomType: string
  room: string
  checkinDevice: string
  checkinMethod: string
}

interface GuideConfig {
  displayHours: number
  guideList: GuideItem[]
}

const { t } = useI18n()

const showConfigPage = ref(false)
const currentStore = ref<Store | null>(null)
const activeTab = ref('basic-info')

const stores = ref<Store[]>([
  { id: 1, name: '茶途木テル　池袋', enabled: true },
  { id: 2, name: '茶途木テル　東十条', enabled: true },
  { id: 3, name: 'Tanpopo Inn 北赤羽', enabled: false },
])

const basicConfig = ref<BasicConfig>({
  selfCheckin: false,
})

const selfCheckinUrl = ref('https://selfcheckin.the-host.ai/')

// 入住登记配置
const checkinConfig = ref<CheckinConfig>({
  formEnabled: true,
  formFields: [
    { id: 1, name: 'Name/姓名/名名前/성명', required: true, type: 'text' },
    { id: 2, name: 'Reservation site/预约平台/予約サイト/숙박 예약 사이트', required: true, type: 'select' },
    { id: 3, name: 'Check-in date', required: true, type: 'date' },
    { id: 4, nameKey: 'settingsStage4.autoCheckin.fields.checkinTime', required: true, type: 'time' },
    { id: 5, name: 'Passport/护照/パスポート写真', required: true, type: 'file' },
  ],
  verificationEnabled: false,
})

// 模板字段
const templateFields = ref<FormField[]>([
  { id: 101, nameKey: 'settingsStage4.autoCheckin.fields.givenName', required: false, type: 'text' },
  { id: 102, nameKey: 'settingsStage4.autoCheckin.fields.familyName', required: false, type: 'text' },
  { id: 103, nameKey: 'settingsStage4.autoCheckin.fields.nationality', required: false, type: 'select' },
  { id: 104, nameKey: 'settingsStage4.autoCheckin.fields.birthDate', required: false, type: 'date' },
  { id: 105, nameKey: 'settingsStage4.autoCheckin.fields.gender', required: false, type: 'select' },
  { id: 106, nameKey: 'settingsStage4.autoCheckin.fields.email', required: false, type: 'email' },
  { id: 107, nameKey: 'settingsStage4.autoCheckin.fields.phone', required: false, type: 'tel' },
  { id: 108, nameKey: 'settingsStage4.autoCheckin.fields.countryOfResidence', required: false, type: 'select' },
])

// 入住指引配置
const guideConfig = ref<GuideConfig>({
  displayHours: 168,
  guideList: [
    {
      id: 1,
      roomType: '茶途木テル　池袋',
      room: '201',
      checkinDevice: '',
      checkinMethod: '',
    },
    {
      id: 2,
      roomType: '茶途木テル　池袋',
      room: '401',
      checkinDevice: '',
      checkinMethod: '',
    },
    {
      id: 3,
      roomType: '茶途木テル　池袋',
      room: '403',
      checkinDevice: '',
      checkinMethod: '',
    },
  ],
})

const handleConfig = (store: Store) => {
  currentStore.value = store
  showConfigPage.value = true
  activeTab.value = 'basic-info'
}

const handleBack = () => {
  showConfigPage.value = false
  currentStore.value = null
}

const handleCopyLink = () => {
  navigator.clipboard.writeText(selfCheckinUrl.value)
  ElMessage.success(t('settingsStage4.autoCheckin.messages.linkCopied'))
}

const handleDownloadQR = () => {
  ElMessage.success(t('settingsStage4.autoCheckin.messages.qrDownloadComingSoon'))
  // TODO: 实现二维码下载
}

// 入住登记相关函数
const getFieldName = (field: FormField) => (field.nameKey ? t(field.nameKey) : field.name || '')

const handleAddField = (template: FormField) => {
  const newField = {
    ...template,
    id: Date.now(),
  }
  checkinConfig.value.formFields.push(newField)
  ElMessage.success(t('settingsStage4.autoCheckin.messages.fieldAdded', { name: getFieldName(template) }))
}

// 入住指引相关函数
const handleEditGuide = (row: GuideItem) => {
  ElMessage.info(t('settingsStage4.autoCheckin.messages.editGuideComingSoon'))
  // TODO: 实现编辑指引
}

const handleDeleteGuide = (row: GuideItem) => {
  ElMessageBox.confirm(t('settingsStage4.autoCheckin.messages.clearGuideConfirm', { room: row.room }), t('settingsStage4.autoCheckin.messages.clearGuideTitle'), {
    confirmButtonText: t('settings.common.confirm'),
    cancelButtonText: t('settings.common.cancel'),
    type: 'warning',
  })
    .then(() => {
      const index = guideConfig.value.guideList.findIndex((g) => g.id === row.id)
      if (index !== -1) {
        guideConfig.value.guideList[index].checkinDevice = ''
        guideConfig.value.guideList[index].checkinMethod = ''
        ElMessage.success(t('settingsStage4.autoCheckin.messages.clearSuccess'))
      }
    })
    .catch(() => {
      // 用户取消
    })
}

const handlePreviewGuide = (row: GuideItem) => {
  ElMessage.info(t('settingsStage4.autoCheckin.messages.previewGuideComingSoon'))
  // TODO: 实现预览指引
}
</script>

<style scoped>
.auto-checkin-container {
  background: #f5f7fa;
  min-height: calc(100vh - 100px);
}

/* 带通知的容器 */
.container-with-notice {
  padding: 20px;
}

/* 顶部提示信息 */
.notice-banner {
  background: #e6f7ff;
  border: 1px solid #91d5ff;
  border-radius: 4px;
  padding: 12px 16px;
  margin-bottom: 20px;
  font-size: 14px;
  color: #1890ff;
  line-height: 1.6;
}

/* 门店卡片网格 */
.stores-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 20px;
}

.store-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.card-header {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.collapse-icon {
  flex-shrink: 0;
  font-size: 20px;
  color: #606266;
  margin-top: 4px;
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

.status-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 12px;
  border-top: 1px solid #ebeef5;
}

.status-label {
  font-size: 14px;
  color: #606266;
}

/* 配置页面样式 */
.config-page {
  background: #fff;
  border-radius: 8px;
  padding: 24px;
  margin: 20px;
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

.config-tabs {
  margin-top: 20px;
}

/* 标签页内容 */
.tab-content {
  padding: 20px 0;
}

.setting-item {
  margin-bottom: 32px;
  padding-bottom: 32px;
  border-bottom: 1px solid #ebeef5;
}

.setting-item:last-child {
  border-bottom: none;
}

.setting-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.setting-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 8px 0;
}

.setting-desc {
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
  margin: 0 0 16px 0;
}

.link-text {
  color: #409eff;
  text-decoration: none;
}

.link-text:hover {
  text-decoration: underline;
}

.link-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: #f5f7fa;
  border-radius: 4px;
}

.link-row .link-text {
  flex: 1;
  font-size: 14px;
  color: #303133;
  word-break: break-all;
}

.qrcode-section {
  display: flex;
  align-items: flex-start;
  gap: 16px;
}

.qrcode-wrapper {
  flex-shrink: 0;
}

.qrcode-placeholder {
  width: 150px;
  height: 150px;
  border: 1px dashed #dcdfe6;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fafafa;
  color: #909399;
  font-size: 14px;
}

.upload-section {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.upload-button {
  width: fit-content;
  display: flex;
  align-items: center;
  gap: 8px;
}

.upload-hint {
  font-size: 12px;
  color: #909399;
  margin: 0;
}

:deep(.el-tabs__nav-wrap::after) {
  display: none;
}

/* 入住登记页面样式 */
.checkin-section {
  margin-bottom: 32px;
  padding-bottom: 32px;
  border-bottom: 1px solid #ebeef5;
}

.section-header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.action-buttons {
  display: flex;
  gap: 12px;
}

.section-switch-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 20px;
  background: #f5f7fa;
  border-radius: 8px;
  margin-bottom: 20px;
}

.switch-content {
  flex: 1;
}

.switch-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 8px 0;
}

.switch-desc {
  font-size: 13px;
  color: #606266;
  line-height: 1.6;
  margin: 0;
}

.form-fields-list {
  margin-top: 20px;
}

.field-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  background: #fafafa;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  margin-bottom: 12px;
  transition: all 0.2s;
}

.field-item:hover {
  background: #f5f7fa;
  border-color: #409eff;
}

.field-drag {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
  cursor: move;
}

.field-icon {
  font-size: 18px;
  color: #606266;
}

.field-name {
  font-size: 14px;
  color: #303133;
  font-weight: 500;
}

.field-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.template-section {
  margin-top: 32px;
  padding: 20px;
  background: #fff;
  border: 1px dashed #dcdfe6;
  border-radius: 8px;
}

.template-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 16px 0;
}

.template-buttons {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 12px;
}

.template-button {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  background: #f5f7fa;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  transition: all 0.2s;
  justify-content: space-between;
}

.template-button:hover {
  background: #ecf5ff;
  border-color: #409eff;
  color: #409eff;
}

.add-icon {
  color: #409eff;
  font-size: 16px;
}

.verification-section {
  margin-bottom: 32px;
}

/* 入住指引页面样式 */
.guide-section {
  margin-bottom: 32px;
  padding-bottom: 32px;
  border-bottom: 1px solid #ebeef5;
}

.guide-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.display-time-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  background: #f5f7fa;
  border-radius: 8px;
  margin-bottom: 20px;
}

.setting-label {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 8px 0;
}

.setting-desc {
  font-size: 13px;
  color: #606266;
  line-height: 1.6;
  margin: 0;
}

.time-input-group {
  display: flex;
  align-items: center;
  gap: 12px;
}

.time-unit {
  font-size: 14px;
  color: #606266;
}

.display-condition-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  background: #f5f7fa;
  border-radius: 8px;
  margin-bottom: 16px;
}

.conditions-list {
  padding: 0 20px;
}

.condition-item {
  display: flex;
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
}

.condition-item:last-child {
  border-bottom: none;
}

.condition-label {
  min-width: 120px;
  font-size: 14px;
  color: #606266;
}

.condition-value {
  flex: 1;
  font-size: 14px;
  color: #303133;
}

.guide-table-section {
  margin-top: 32px;
}

.guide-table-section .section-title {
  margin-bottom: 16px;
}

:deep(.el-input-number) {
  width: 120px;
}

:deep(.el-table) {
  font-size: 14px;
}

:deep(.el-table th) {
  background-color: #f5f7fa;
  color: #606266;
  font-weight: 600;
}
</style>
