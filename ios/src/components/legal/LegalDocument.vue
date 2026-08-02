<template>
  <article class="legal-document">
    <h1 class="legal-document__title">{{ t(`legal.${document}.title`) }}</h1>
    <p class="legal-document__meta">{{ t('legal.common.effectiveDate') }}</p>
    <p class="legal-document__intro">{{ t(`legal.${document}.intro`) }}</p>

    <section v-for="section in sections" :key="section.title" class="legal-document__section">
      <h2 class="legal-document__section-title">{{ section.title }}</h2>
      <p
        v-for="paragraph in section.paragraphs"
        :key="paragraph"
        class="legal-document__paragraph"
      >
        {{ paragraph }}
      </p>
    </section>
  </article>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

type LegalSection = {
  title: string
  paragraphs: string[]
}

const props = defineProps<{
  document: 'terms' | 'privacy'
}>()

const { t, tm } = useI18n()

const sections = computed(() => {
  const value = tm(`legal.${props.document}.sections`)
  return Array.isArray(value) ? (value as LegalSection[]) : []
})
</script>

<style scoped>
.legal-document {
  max-width: 720px;
  margin: 0 auto;
  padding: 8px 4px 32px;
}

.legal-document__title {
  margin: 0;
  color: #1f2430;
  font-size: 24px;
  font-weight: 700;
  line-height: 1.3;
}

.legal-document__meta {
  margin: 10px 0 0;
  color: #8a8f99;
  font-size: 13px;
}

.legal-document__intro {
  margin: 18px 0 0;
  color: #3c414c;
  font-size: 14px;
  line-height: 1.8;
}

.legal-document__section {
  margin-top: 22px;
}

.legal-document__section-title {
  margin: 0 0 8px;
  color: #1f2430;
  font-size: 16px;
  font-weight: 600;
  line-height: 1.5;
}

.legal-document__paragraph {
  margin: 8px 0 0;
  color: #3c414c;
  font-size: 14px;
  line-height: 1.8;
}
</style>
