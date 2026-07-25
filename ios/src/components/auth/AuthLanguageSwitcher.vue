<template>
  <button
    class="auth-language-switcher"
    type="button"
    :aria-label="t('language.title')"
    :disabled="languageStore.isLoading"
    @click="handleOpenLanguageSheet"
  >
    <ion-icon :icon="languageOutline" aria-hidden="true" />
    <span>{{ currentLocaleLabel }}</span>
    <ion-icon
      :icon="chevronDownOutline"
      class="auth-language-switcher__chevron"
      aria-hidden="true"
    />
  </button>
</template>

<script setup lang="ts">
import { actionSheetController, IonIcon } from '@ionic/vue'
import { chevronDownOutline, languageOutline } from 'ionicons/icons'
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { SUPPORTED_LOCALES, type SupportedLocale } from '@/locales'
import { useLanguageStore } from '@/stores/language'

const localeShortLabels: Record<SupportedLocale, string> = {
  en: 'EN',
  ja: '日本語',
  'zh-CN': '简中',
  'zh-TW': '繁中',
}

const { t } = useI18n()
const languageStore = useLanguageStore()

const currentLocaleLabel = computed(() => localeShortLabels[languageStore.locale])

const handleOpenLanguageSheet = async () => {
  if (languageStore.isLoading) {
    return
  }

  const actionSheet = await actionSheetController.create({
    header: t('language.title'),
    subHeader: t('language.description'),
    cssClass: 'auth-language-action-sheet',
    buttons: [
      ...SUPPORTED_LOCALES.map((locale) => ({
        text: t(`language.option.${locale}`),
        role: languageStore.locale === locale ? 'selected' : undefined,
        handler: async () => {
          await languageStore.setLocale(locale)
        },
      })),
      {
        text: t('common.cancel'),
        role: 'cancel',
      },
    ],
  })

  await actionSheet.present()
}
</script>

<style scoped>
.auth-language-switcher {
  position: absolute;
  z-index: 3;
  top: calc(16px + var(--app-safe-top));
  right: 16px;
  display: inline-grid;
  grid-template-columns: 18px auto 12px;
  align-items: center;
  gap: 6px;
  min-width: 78px;
  height: 36px;
  padding: 0 9px;
  border: 1px solid rgba(7, 143, 244, 0.16);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.72);
  box-shadow: 0 4px 14px rgba(36, 112, 166, 0.08);
  color: #247bb6;
  font: inherit;
  font-size: 13px;
  font-weight: 600;
  line-height: 1;
  letter-spacing: 0;
  backdrop-filter: blur(12px);
  transition:
    border-color 0.18s ease,
    background-color 0.18s ease,
    transform 0.18s ease;
}

.auth-language-switcher ion-icon {
  font-size: 18px;
}

.auth-language-switcher__chevron {
  color: #70a3c7;
  font-size: 12px !important;
}

.auth-language-switcher:active {
  transform: scale(0.97);
}

.auth-language-switcher:focus-visible {
  outline: 3px solid rgba(7, 143, 244, 0.2);
  outline-offset: 2px;
}

.auth-language-switcher:disabled {
  cursor: wait;
  opacity: 0.58;
}

@media (max-width: 360px) {
  .auth-language-switcher {
    top: calc(12px + var(--app-safe-top));
    right: 12px;
    min-width: 74px;
  }
}
</style>
