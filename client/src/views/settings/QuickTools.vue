<template>
  <div class="quick-tools-page">
    <div class="page-header">
      <div>
        <h2 class="page-title">{{ t('settings.quickTools.title') }}</h2>
        <p class="page-description">{{ t('settings.quickTools.description') }}</p>
      </div>
    </div>

    <div class="tools-grid">
      <section class="tool-panel tool-panel--memo">
        <div class="tool-panel-header">
          <span class="tool-icon memo-icon">
            <el-icon><Document /></el-icon>
          </span>
          <div class="tool-heading">
            <h3>{{ t('settings.quickTools.memo.title') }}</h3>
            <p>{{ t('settings.quickTools.memo.description') }}</p>
          </div>
        </div>

        <div class="memo-meta">
          <span v-if="memoStore.lastSavedAt">
            {{ memoStore.getFormattedSaveTime() }} {{ t('layout.memo.saved') }}
          </span>
          <span v-else>{{ t('settings.quickTools.memo.autoSave') }}</span>
        </div>

        <el-input
          v-model="memo"
          type="textarea"
          :rows="8"
          :placeholder="t('layout.memo.placeholder')"
          class="memo-input"
        />
      </section>

      <section class="tool-panel">
        <div class="tool-panel-header">
          <span class="tool-icon record-icon">
            <el-icon><EditPen /></el-icon>
          </span>
          <div class="tool-heading">
            <h3>{{ t('settings.quickTools.record.title') }}</h3>
            <p>{{ t('settings.quickTools.record.description') }}</p>
          </div>
        </div>

        <el-button type="primary" class="tool-action" @click="showRecordTransaction = true">
          {{ t('settings.quickTools.record.action') }}
        </el-button>
      </section>

      <section class="tool-panel">
        <div class="tool-panel-header">
          <span class="tool-icon support-icon">
            <el-icon><Headset /></el-icon>
          </span>
          <div class="tool-heading">
            <h3>{{ t('settings.quickTools.support.title') }}</h3>
            <p>{{ t('settings.quickTools.support.description') }}</p>
          </div>
        </div>

        <el-button class="tool-action" @click="showCustomerService = true">
          {{ t('settings.quickTools.support.action') }}
        </el-button>
      </section>
    </div>

    <RecordTransaction v-model="showRecordTransaction" />
    <CustomerService
      :visible="showCustomerService"
      @close="showCustomerService = false"
      @minimize="showCustomerService = false"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { Document, EditPen, Headset } from '@element-plus/icons-vue'
import CustomerService from '@/components/CustomerService.vue'
import RecordTransaction from '@/components/RecordTransaction.vue'
import { useMemoStore } from '@/stores/memo'

const { t } = useI18n()
const memoStore = useMemoStore()
const showRecordTransaction = ref(false)
const showCustomerService = ref(false)

const memo = computed({
  get: () => memoStore.memoContent,
  set: (value: string) => memoStore.saveMemoDebounced(value),
})

onMounted(() => {
  memoStore.loadMemo()
})
</script>

<style scoped>
.quick-tools-page {
  padding: 24px;
  background: #f5f7fa;
  min-height: calc(100vh - 100px);
}

.page-header {
  margin-bottom: 20px;
}

.page-title {
  margin: 0 0 6px;
  font-size: 20px;
  font-weight: 650;
  color: #24272d;
}

.page-description {
  margin: 0;
  font-size: 14px;
  color: #7b8492;
  line-height: 1.5;
}

.tools-grid {
  display: grid;
  grid-template-columns: minmax(420px, 1.4fr) minmax(240px, 0.8fr);
  gap: 18px;
  align-items: stretch;
}

.tool-panel {
  min-width: 0;
  padding: 20px;
  border: 1px solid #e7e9ef;
  border-radius: 8px;
  background: #ffffff;
  box-shadow: 0 8px 22px rgba(21, 27, 38, 0.05);
}

.tool-panel--memo {
  grid-row: span 2;
}

.tool-panel-header {
  display: flex;
  gap: 14px;
  align-items: flex-start;
  margin-bottom: 18px;
}

.tool-icon {
  width: 42px;
  height: 42px;
  border-radius: 8px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  flex: 0 0 auto;
}

.memo-icon {
  color: #2f7cf6;
  background: #edf4ff;
}

.record-icon {
  color: #9b6a10;
  background: #fff4d8;
}

.support-icon {
  color: #148766;
  background: #e8f8f1;
}

.tool-heading {
  min-width: 0;
}

.tool-heading h3 {
  margin: 0 0 6px;
  font-size: 16px;
  font-weight: 650;
  color: #24272d;
}

.tool-heading p {
  margin: 0;
  font-size: 13px;
  line-height: 1.45;
  color: #7b8492;
}

.memo-meta {
  min-height: 18px;
  margin-bottom: 10px;
  font-size: 12px;
  color: #9098a5;
  text-align: right;
}

.memo-input {
  width: 100%;
}

.memo-input :deep(.el-textarea__inner) {
  font-family: inherit;
  line-height: 1.6;
}

.tool-action {
  width: 100%;
  height: 38px;
  border-radius: 6px;
}

@media (max-width: 1100px) {
  .tools-grid {
    grid-template-columns: 1fr;
  }

  .tool-panel--memo {
    grid-row: auto;
  }
}
</style>
