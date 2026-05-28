<template>
  <div class="legal-page">
    <div class="legal-container">
      <h1 class="legal-title">{{ t('stage5.legal.privacy.title') }}</h1>
      <p class="legal-meta">{{ t('stage5.legal.common.effectiveDate') }}</p>
      <p class="legal-intro">
        {{ t('stage5.legal.privacy.intro') }}
      </p>

      <section v-for="section in sections" :key="section.title" class="legal-section">
        <h2>{{ section.title }}</h2>
        <p v-for="paragraph in section.paragraphs" :key="paragraph">{{ paragraph }}</p>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

type LegalSection = {
  title: string
  paragraphs: string[]
}

const { t, tm } = useI18n()

const sections = computed(() => tm('stage5.legal.privacy.sections') as LegalSection[])
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
