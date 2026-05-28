<script setup lang="ts">
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { Cpu } from '@element-plus/icons-vue'

const { t } = useI18n()
const loading = ref(false)
</script>

<template>
  <div class="automation">
    <div class="page-header">
      <h3>{{ t('settings.placeholders.automation.title') }}</h3>
      <p class="page-description">{{ t('settings.placeholders.automation.description') }}</p>
    </div>

    <div class="content-area">
      <el-card>
        <div class="placeholder-content">
          <el-icon size="48" color="#d9d9d9"><Cpu /></el-icon>
          <p>
            {{
              t('settings.common.inDevelopment', {
                name: t('settings.placeholders.automation.title'),
              })
            }}
          </p>
        </div>
      </el-card>
    </div>
  </div>
</template>

<style scoped>
.automation {
  padding: 20px;
  height: 100%;
}

.page-header {
  margin-bottom: 20px;
}

.page-header h3 {
  margin: 0 0 8px 0;
  color: #333;
  font-size: 18px;
}

.page-description {
  margin: 0;
  color: #666;
  font-size: 14px;
}

.content-area {
  height: calc(100% - 80px);
}

.placeholder-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 200px;
  color: #999;
}
</style>
