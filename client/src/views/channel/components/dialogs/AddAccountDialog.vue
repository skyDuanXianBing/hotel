<script setup lang="ts">
defineProps<{
  modelValue: boolean
}>()

defineEmits<{
  'update:modelValue': [value: boolean]
  connect: []
}>()
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    title="Airbnb 直连"
    width="700px"
    @update:model-value="$emit('update:modelValue', $event)"
  >
    <div class="airbnb-connect-dialog">
      <div class="dialog-header-section">
        <div class="dialog-description">
          <p>点击<a href="#" class="link-text">相关内容</a>查看详情。注意：仅所有者账号可授权。</p>
        </div>
        <div class="dialog-logo">
          <img
            src="https://upload.wikimedia.org/wikipedia/commons/6/69/Airbnb_Logo_B%C3%A9lo.svg"
            alt="Airbnb"
            class="dialog-logo-img airbnb-logo"
          />
        </div>
      </div>

      <div class="agreement-section">
        <h3 class="agreement-title">Airbnb直连</h3>
        <div class="agreement-content">
          <p>Airbnb直连服务功能（"直连服务"）是指在酒店商户（"您"）在the-host系统中设置直连服务后，the-host系统和Airbnb系统自动建立直连。一旦您连接到Airbnb并完成授权，您可以在Airbnb平台上创建新的房源，管理价格和可用性，并自动将订单录入the-host系统，从而降低酒店运营成本并提高效率。</p>

          <p>在启用Airbnb直连服务之前，请务必仔细阅读并遵守本《用户须知》的条款内容，特别是免责或责任限制、纠纷解决和适用法律条款。如果您对协议有疑问或不同意内容，请立即停止使用服务。当您使用直连服务功能时，即表示您已充分阅读、理解并接受本"用户须知"的全部内容，此协议将在您在线确认后立即生效。协议条款如下：</p>

          <h4>1 功能介绍</h4>
          <p>1.1 在与Airbnb完成直连后，即可通过the-host PMS管理Airbnb的渠道房价、房态，渠道订单将自动落入the-host。</p>
          <p>1.2 1个the-host网络号可与多个Airbnb账号直连。点击"与Airbnb连接"后，将跳转至Airbnb页面进行授权。为确保授权账号无误，授权之前请确保您在Airbnb是未登录状态，完成授权后才可以关联房型。</p>
          <p>1.3 Airbnb房源类型为集中式公寓、度假民宿、客栈、农家乐、房车的房源可以直接关联整个房型（允许多个库存），其他类型的房源只能关联单个房间（只允许1个库存）。</p>
        </div>
      </div>
    </div>
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="$emit('update:modelValue', false)">取消</el-button>
        <el-button type="primary" @click="$emit('connect')">与Airbnb连接</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<style scoped>
.airbnb-connect-dialog {
  padding: 0;
}

.dialog-header-section {
  display: flex;
  gap: 20px;
  padding-bottom: 20px;
  border-bottom: 1px solid #e8e8e8;
  margin-bottom: 20px;
}

.dialog-description {
  flex: 1;
}

.dialog-description p {
  font-size: 13px;
  line-height: 1.8;
  color: #606266;
  margin: 0 0 12px 0;
}

.link-text {
  color: #409eff;
  text-decoration: none;
}

.link-text:hover {
  text-decoration: underline;
}

.dialog-logo {
  flex-shrink: 0;
  width: 100px;
  height: 50px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.dialog-logo-img {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
}

.agreement-section {
  max-height: 400px;
  overflow-y: auto;
}

.agreement-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 16px 0;
  text-align: center;
}

.agreement-content {
  font-size: 13px;
  line-height: 1.8;
  color: #606266;
}

.agreement-content p {
  margin: 0 0 16px 0;
  text-align: justify;
}

.agreement-content h4 {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin: 20px 0 12px 0;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
