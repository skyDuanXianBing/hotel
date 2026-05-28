<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

const props = defineProps<{
  modelValue: boolean
  channel: { name: string; logoUrl?: string } | null
}>()

defineEmits<{
  'update:modelValue': [value: boolean]
  authorize: []
}>()

const { t } = useI18n()
const channelName = computed(() => props.channel?.name || t('channel.dialogs.addHotel.fallbackChannel'))
const headerDescription = computed(() => {
  return t('channel.dialogs.addHotel.headerDescription', { name: channelName.value })
})
const registrationNotice = computed(() => {
  return t('channel.dialogs.addHotel.registrationNotice', { name: channelName.value })
})
const serviceDescription = computed(() => {
  return t('channel.dialogs.addHotel.serviceDescription', { name: channelName.value })
})
const enableNotice = computed(() => {
  return t('channel.dialogs.addHotel.enableNotice', { name: channelName.value })
})
const preparationNotice = computed(() => {
  return t('channel.dialogs.addHotel.preparation', { name: channelName.value })
})
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    :title="t('channel.dialogs.addHotel.title', { name: channelName })"
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
        <h3 class="agreement-title">
          {{ t('channel.dialogs.addHotel.agreementTitle', { name: channelName }) }}
        </h3>
        <div class="agreement-content">
          <p>{{ serviceDescription }}</p>

          <p>{{ enableNotice }}</p>

          <h4>{{ t('channel.dialogs.addHotel.functionScopeTitle') }}</h4>
          <p>{{ t('channel.dialogs.addHotel.functionScope') }}</p>

          <h4>{{ t('channel.dialogs.addHotel.preparationTitle') }}</h4>
          <p>{{ preparationNotice }}</p>

          <h4>{{ t('channel.dialogs.addHotel.nextStepTitle') }}</h4>
          <p>{{ t('channel.dialogs.addHotel.nextStep') }}</p>
        </div>
      </div>
    </div>
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="$emit('update:modelValue', false)">{{ t('channel.dialogs.common.cancel') }}</el-button>
        <el-button type="primary" @click="$emit('authorize')">
          {{ t('channel.dialogs.common.continue') }}
        </el-button>
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
