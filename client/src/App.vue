<script setup lang="ts">
import { computed, watch } from 'vue'
import { ElConfigProvider } from 'element-plus'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { getElementPlusLocale } from '@/locales'
import { useLanguageStore } from '@/stores/language'

const languageStore = useLanguageStore()
const route = useRoute()
const { t } = useI18n()

const elementLocale = computed(() => getElementPlusLocale(languageStore.locale))

const updateDocumentTitle = () => {
  const appTitle = t('app.name')
  const titleKey = typeof route.meta.titleKey === 'string' ? route.meta.titleKey : ''
  const routeTitle = titleKey ? t(titleKey) : typeof route.meta.title === 'string' ? route.meta.title : ''

  document.title = routeTitle ? `${routeTitle} - ${appTitle}` : appTitle
}

watch([() => route.fullPath, () => languageStore.locale], updateDocumentTitle, { immediate: true })
</script>

<template>
  <el-config-provider :locale="elementLocale">
    <router-view />
  </el-config-provider>
</template>

<style scoped>
/* Global styles live in layout components. */
</style>
