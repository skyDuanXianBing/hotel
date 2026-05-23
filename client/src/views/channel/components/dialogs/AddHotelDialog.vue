<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  modelValue: boolean
  channel: { name: string; logoUrl?: string } | null
}>()

defineEmits<{
  'update:modelValue': [value: boolean]
  authorize: []
}>()

const channelName = computed(() => props.channel?.name || '该渠道')
const headerDescription = computed(() => {
  return `${channelName.value}直连接入需要先确认渠道账号、酒店资料和授权条件。继续操作表示您已了解接入说明，并准备发起后续授权或开通申请。`
})
const registrationNotice = computed(() => {
  return `注意：如果您还没有${channelName.value}商家账号，请先在${channelName.value}完成注册和酒店资料维护。`
})
const serviceDescription = computed(() => {
  return `${channelName.value}直连服务是指在获得渠道许可并完成账号授权后，由 THE HOST HUB 与${channelName.value}建立系统连接，用于同步酒店、房型、价格、房态和订单等信息。该能力需要依赖渠道侧审核、账号权限和酒店资料匹配结果。`
})
const enableNotice = computed(() => {
  return `在启用${channelName.value}直连服务前，请确认您有权管理对应渠道账号和酒店资源，并已了解直连后可能影响渠道房价、房态和订单同步。若您对接入条件或数据同步范围有疑问，请先联系运营或客服确认后再继续。`
})
const preparationNotice = computed(() => {
  return `请先在${channelName.value}后台完成酒店创建、资料审核和必要的房型/价格设置，并确保当前账号具备授权或管理权限。资料不完整、账号权限不足或渠道审核未通过时，直连可能无法开通。`
})
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    :title="`直连${channel?.name || ''}`"
    width="700px"
    @update:model-value="$emit('update:modelValue', $event)"
  >
    <div class="add-hotel-dialog">
      <div class="dialog-header-section">
        <div class="dialog-description">
          <p>{{ headerDescription }}</p>
          <p class="warning-text">{{ registrationNotice }}</p>
        </div>
        <div class="dialog-logo">
          <img :src="channel?.logoUrl" :alt="channel?.name" class="dialog-logo-img" />
        </div>
      </div>

      <div class="agreement-section">
        <h3 class="agreement-title">{{ channelName }}直连接入说明</h3>
        <div class="agreement-content">
          <p>{{ serviceDescription }}</p>

          <p>{{ enableNotice }}</p>

          <h4>1 功能范围</h4>
          <p>
            完成真实授权和资料匹配后，系统可按渠道支持范围同步酒店、房型、价格计划、库存、价格和订单。不同渠道开放能力不同，最终可用功能以渠道授权结果和系统实际接入状态为准。
          </p>

          <h4>2 开通前准备</h4>
          <p>{{ preparationNotice }}</p>

          <h4>3 下一步</h4>
          <p>
            点击“我已了解，继续申请授权”后，系统会关闭本说明并提示您进入真实授权或运营开通流程。当前页面不会伪造授权成功，也不会自动向真实酒店列表写入模拟酒店数据。
          </p>
        </div>
      </div>
    </div>
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="$emit('update:modelValue', false)">取消</el-button>
        <el-button type="primary" @click="$emit('authorize')">我已了解，继续申请授权</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<style scoped>
.add-hotel-dialog {
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

.warning-text {
  color: #f56c6c;
  font-weight: 500;
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
