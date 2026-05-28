<script setup lang="ts">
import { useI18n } from 'vue-i18n'

defineProps<{
  modelValue: boolean
}>()

defineEmits<{
  'update:modelValue': [value: boolean]
  connect: []
}>()

const { t } = useI18n()
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    :title="t('channel.dialogs.addAccount.title')"
    width="700px"
    @update:model-value="$emit('update:modelValue', $event)"
  >
    <div class="airbnb-connect-dialog">
      <div class="dialog-header-section">
        <div class="dialog-description">
          <p>
            {{ t('channel.dialogs.addAccount.descriptionPrefix') }}
            <a href="#" class="link-text">{{ t('channel.dialogs.addAccount.descriptionLink') }}</a>
            {{ t('channel.dialogs.addAccount.descriptionSuffix') }}
          </p>
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
        <h3 class="agreement-title">{{ t('channel.dialogs.addAccount.agreementTitle') }}</h3>
        <div class="agreement-content">
          <p>{{ t('channel.dialogs.addAccount.paragraph1') }}</p>

          <p>{{ t('channel.dialogs.addAccount.paragraph2') }}</p>

          <h4>{{ t('channel.dialogs.addAccount.featureTitle') }}</h4>
          <p>{{ t('channel.dialogs.addAccount.feature1') }}</p>
          <p>{{ t('channel.dialogs.addAccount.feature2') }}</p>
          <p>{{ t('channel.dialogs.addAccount.feature3') }}</p>
        </div>
      </div>
    </div>
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="$emit('update:modelValue', false)">{{ t('channel.dialogs.common.cancel') }}</el-button>
        <el-button type="primary" @click="$emit('connect')">{{ t('channel.dialogs.addAccount.connect') }}</el-button>
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
