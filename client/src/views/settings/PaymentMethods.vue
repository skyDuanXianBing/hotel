<template>
  <div class="payment-methods">
    <!-- 信息提示 -->
    <el-alert
      type="info"
      :closable="false"
      class="info-alert"
    >
      <template #default>
        <div class="info-content">
          <p>1. 收款方式默认1个常用可用，您可拖动以调整收款方式为主次顺序。同时也可调整收款方式的排序。</p>
          <p>2. 系统预设预不可修改名称，其中通过扫码付、收款码、POS机等方式实现的在线收款，其收款金额将存入订单钱包（支付宝、微信支付官方需收取交易手续费，扫码付收取0.38%；收款码收取0.6%；POS机境内卡收取0.5%起；POS机境外卡收取1.5%起）。<a href="#" class="link">订单钱包</a> 页面可查看余额、操作规则。</p>
        </div>
      </template>
    </el-alert>

    <!-- 渠道设置标题栏 -->
    <div class="section-header">
      <h3 class="section-title">渠道设置</h3>
      <el-button type="primary" @click="handleAddPaymentMethod">新增收款方式</el-button>
    </div>

    <!-- 可用收款方式 -->
    <div class="payment-section">
      <h4 class="subsection-title">可用收款方式 <span class="subtitle-desc">即在订单中操作（付、收、退）时，可选中的收款方式。</span></h4>

      <div class="payment-grid">
        <div
          v-for="method in availablePaymentMethods"
          :key="method.id"
          class="payment-card"
        >
          <div v-if="method.isDefault && !method.isEditing" class="default-corner">默认</div>
          <div class="drag-handle">
            <el-icon><Grid /></el-icon>
          </div>
          <div class="card-content">
            <el-input
              v-if="method.isEditing"
              v-model="method.name"
              placeholder="请输入收款方式名称"
              size="small"
              @keyup.enter="handleSaveMethod(method)"
            />
            <span v-else class="method-name">{{ method.name }}</span>
          </div>
          <div class="card-actions">
            <el-icon v-if="method.isEditing" class="action-icon success" @click="handleSaveMethod(method)">
              <Check />
            </el-icon>
            <el-icon v-if="method.isEditing" class="action-icon danger" @click="handleCancelEdit(method)">
              <Close />
            </el-icon>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { Grid, Check, Close } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

interface PaymentMethod {
  id: number
  name: string
  isDefault: boolean
  isSystem: boolean
  isEditing: boolean
}

// 可用收款方式
const availablePaymentMethods = ref<PaymentMethod[]>([
  { id: 1, name: '携程代收', isDefault: true, isSystem: true, isEditing: false },
  { id: 2, name: '飞猪代收', isDefault: true, isSystem: true, isEditing: false },
  { id: 3, name: '美团酒店代收', isDefault: true, isSystem: true, isEditing: false },
  { id: 4, name: '美团民宿代收', isDefault: true, isSystem: true, isEditing: false },
  { id: 5, name: '爱彼迎代收', isDefault: true, isSystem: true, isEditing: false },
  { id: 6, name: 'Booking.com代收', isDefault: true, isSystem: true, isEditing: false },
  { id: 7, name: '小猪民宿代收', isDefault: true, isSystem: true, isEditing: false },
  { id: 8, name: '木鸟民宿代收', isDefault: true, isSystem: true, isEditing: false },
  { id: 9, name: 'Agoda代收', isDefault: true, isSystem: true, isEditing: false },
  { id: 10, name: 'Expedia代收', isDefault: true, isSystem: true, isEditing: false },
  { id: 11, name: '小红书代收', isDefault: true, isSystem: true, isEditing: false },
  { id: 12, name: '途家代收', isDefault: true, isSystem: true, isEditing: false },
  { id: 13, name: '抖音代收', isDefault: true, isSystem: true, isEditing: false },
  { id: 14, name: '现金', isDefault: true, isSystem: true, isEditing: false },
  { id: 15, name: '支付宝', isDefault: true, isSystem: true, isEditing: false },
  { id: 16, name: '微信', isDefault: true, isSystem: true, isEditing: false },
])

// 新增收款方式
const handleAddPaymentMethod = () => {
  const newMethod: PaymentMethod = {
    id: Date.now(),
    name: '',
    isDefault: false,
    isSystem: false,
    isEditing: true,
  }
  availablePaymentMethods.value.push(newMethod)
}

// 保存收款方式
const handleSaveMethod = (method: PaymentMethod) => {
  if (!method.name.trim()) {
    ElMessage.warning('请输入收款方式名称')
    return
  }
  method.isEditing = false
  ElMessage.success('保存成功')
}

// 取消编辑
const handleCancelEdit = (method: PaymentMethod) => {
  if (!method.name) {
    // 新增的空白项，直接删除
    const index = availablePaymentMethods.value.findIndex((m) => m.id === method.id)
    if (index > -1) {
      availablePaymentMethods.value.splice(index, 1)
    }
  } else {
    method.isEditing = false
  }
}
</script>

<style scoped>
.payment-methods {
  padding: 20px;
  background: white;
  min-height: 100%;
}

.info-alert {
  margin-bottom: 24px;
  background: #e6f7ff;
  border: 1px solid #91d5ff;
}

.info-content p {
  margin: 8px 0;
  font-size: 14px;
  line-height: 1.6;
  color: #333;
}

.info-content .link {
  color: #1890ff;
  text-decoration: none;
}

.info-content .link:hover {
  text-decoration: underline;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #e8e8e8;
}

.section-title {
  font-size: 18px;
  font-weight: 600;
  color: #333;
  margin: 0;
}

.payment-section {
  margin-bottom: 32px;
}

.subsection-title {
  font-size: 16px;
  font-weight: 500;
  color: #333;
  margin-bottom: 16px;
}

.subtitle-desc {
  font-size: 13px;
  font-weight: normal;
  color: #999;
  margin-left: 8px;
}

.payment-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 16px;
}

.payment-card {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: white;
  border: 1px solid #e8e8e8;
  border-radius: 4px;
  cursor: move;
  transition: all 0.2s ease;
  position: relative;
  overflow: hidden;
}

.payment-card:hover {
  border-color: #1890ff;
  box-shadow: 0 2px 8px rgba(24, 144, 255, 0.2);
}

.default-corner {
  position: absolute;
  top: 0;
  right: 0;
  width: 50px;
  height: 24px;
  background: #1890ff;
  color: white;
  font-size: 11px;
  font-weight: 500;
  display: flex;
  align-items: center;
  justify-content: center;
  transform-origin: top right;
  transform: rotate(45deg) translate(14px, -14px);
  z-index: 1;
}

.drag-handle {
  display: flex;
  align-items: center;
  color: #999;
  cursor: move;
}

.card-content {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 8px;
}

.method-name {
  font-size: 14px;
  color: #333;
}

.card-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.action-icon {
  font-size: 18px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.action-icon.success {
  color: #52c41a;
}

.action-icon.success:hover {
  color: #73d13d;
  transform: scale(1.1);
}

.action-icon.danger {
  color: #ff4d4f;
}

.action-icon.danger:hover {
  color: #ff7875;
  transform: scale(1.1);
}
</style>
