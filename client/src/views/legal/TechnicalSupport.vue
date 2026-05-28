<template>
  <div class="legal-page">
    <div class="legal-container">
      <h1 class="legal-title">{{ t('stage5.legal.support.title') }}</h1>
      <p class="legal-meta">{{ t('stage5.legal.common.effectiveDate') }}</p>
      <p class="legal-intro">
        {{ t('stage5.legal.support.intro') }}
      </p>

      <section class="legal-section">
        <h2>{{ t('stage5.legal.support.sections.scope') }}</h2>
        <p v-for="item in supportScopeItems" :key="item">{{ item }}</p>
      </section>

      <section class="legal-section">
        <h2>{{ t('stage5.legal.support.sections.contact') }}</h2>
        <p>{{ t('stage5.legal.support.contactItems.email') }}: saitohiroshi01@gmail.com</p>
        <p>{{ t('stage5.legal.support.contactItems.inApp') }}: {{ t('stage5.legal.support.contactItems.inAppValue') }}</p>
      </section>

      <section class="legal-section">
        <h2>{{ t('stage5.legal.support.sections.hours') }}</h2>
        <p>{{ t('stage5.legal.support.supportHours') }}</p>
      </section>

      <section class="legal-section">
        <p>{{ t('stage5.legal.support.footer') }}</p>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

const { t, tm } = useI18n()

const supportScopeItems = computed(() => tm('stage5.legal.support.scopeItems') as string[])
</script>

<style scoped>
.legal-page {
  min-height: 100vh;
  background: #f5f7fa;
  padding: 32px 16px;
}

.legal-container {
  max-width: 920px;
  margin: 0 auto;
  background: #fff;
  border-radius: 12px;
  padding: 28px 24px;
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.06);
}

.legal-title {
  margin: 0;
  font-size: 30px;
  line-height: 1.3;
  color: #1f2d3d;
}

.legal-meta {
  margin: 12px 0 20px;
  color: #606266;
  font-size: 14px;
}

.legal-intro {
  margin: 0 0 20px;
  color: #303133;
  line-height: 1.8;
}

.legal-section {
  margin-top: 18px;
}

.legal-section h2 {
  margin: 0 0 8px;
  font-size: 18px;
  color: #1f2d3d;
}

.legal-section p {
  margin: 0 0 8px;
  color: #303133;
  line-height: 1.8;
}

@media (max-width: 768px) {
  .legal-container {
    padding: 22px 16px;
  }

  .legal-title {
    font-size: 24px;
  }
}
</style>
