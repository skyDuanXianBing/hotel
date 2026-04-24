<template>
  <SettingsTogglePage
    :back-href="ROUTE_PATHS.settings"
    title="支付平台"
    hero-eyebrow="第三方集成"
    hero-title="支付平台"
  >
    <section class="mobile-card payment-platforms-page__placeholder">
      <strong>移动端暂不支持配置支付平台</strong>
    </section>
  </SettingsTogglePage>
</template>

<script setup lang="ts">
import SettingsTogglePage from '@/components/settings/families/SettingsTogglePage.vue'
import { ROUTE_PATHS } from '@/router/guards'
</script>

<style scoped>
.payment-platforms-page__placeholder {
  display: grid;
  gap: 8px;
}

.payment-platforms-page__placeholder strong,
.payment-platforms-page__placeholder p {
  margin: 0;
}

.payment-platforms-page__placeholder p {
  color: var(--app-muted);
  line-height: 1.6;
}
</style>
