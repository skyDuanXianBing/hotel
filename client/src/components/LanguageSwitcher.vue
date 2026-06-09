<script setup lang="ts">
import { computed } from 'vue'
import { ArrowDown } from '@element-plus/icons-vue'
import { useI18n } from 'vue-i18n'
import { SUPPORTED_LOCALES, type SupportedLocale } from '@/locales'
import { useLanguageStore } from '@/stores/language'

interface Props {
  variant?: 'header' | 'auth'
}

const props = withDefaults(defineProps<Props>(), {
  variant: 'header',
})

const { t } = useI18n()
const languageStore = useLanguageStore()

const options = SUPPORTED_LOCALES.map((locale) => ({
  value: locale,
  labelKey: `language.option.${locale}`,
}))

const currentLabel = computed(() =>
  props.variant === 'auth'
    ? t(`language.option.${languageStore.locale}`)
    : t(`language.short.${languageStore.locale}`)
)

const dropdownPopperClass = computed(() =>
  props.variant === 'auth' ? 'language-dropdown language-dropdown--auth' : ''
)

const handleCommand = (locale: SupportedLocale) => {
  languageStore.setLocale(locale)
}
</script>

<template>
  <el-dropdown
    :popper-class="dropdownPopperClass"
    @command="handleCommand"
  >
    <button
      :class="['language-trigger', `language-trigger--${variant}`]"
      type="button"
    >
      <span>{{ currentLabel }}</span>
      <el-icon><ArrowDown /></el-icon>
    </button>
    <template #dropdown>
      <el-dropdown-menu>
        <el-dropdown-item
          v-for="option in options"
          :key="option.value"
          :command="option.value"
          :class="{ 'is-active': languageStore.locale === option.value }"
        >
          {{ t(option.labelKey) }}
        </el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
</template>

<style scoped>
.language-trigger {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  border: none;
  background: transparent;
  cursor: pointer;
  padding: 0;
}

.language-trigger--header {
  height: 32px;
  padding: 0 8px;
  border-radius: 8px;
  color: #606266;
  font-size: 13px;
  transition: background-color 0.2s ease, color 0.2s ease;
}

.language-trigger--header:hover {
  background: #f5f7fa;
  color: #409eff;
}

.language-trigger--auth {
  color: #666;
  font-size: 14px;
}

:global(.language-dropdown--auth .el-dropdown-menu__item) {
  color: #4b5563;
  transition: background-color 0.2s ease, color 0.2s ease;
}

:global(.language-dropdown--auth .el-dropdown-menu__item:hover),
:global(.language-dropdown--auth .el-dropdown-menu__item:focus) {
  background: rgba(89, 126, 247, 0.08);
  color: #597ef7;
}

:global(.language-dropdown--auth .el-dropdown-menu__item.is-active) {
  background: rgba(89, 126, 247, 0.08);
  color: #597ef7;
  font-weight: 600;
}
</style>
