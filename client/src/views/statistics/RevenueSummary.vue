<template>
  <StatisticsLayout>
    <div class="legacy-redirect">
      <el-alert
        type="info"
        :title="t('stage5.statistics.revenue.redirectingToDataCenter')"
        :closable="false"
        show-icon
      />
    </div>
  </StatisticsLayout>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import StatisticsLayout from './StatisticsLayout.vue'

const router = useRouter()
const { t } = useI18n()

onMounted(() => {
  router.replace({
    path: '/data-center/overview',
    query: { tab: 'revenue' },
  })
})
</script>

<style scoped>
.legacy-redirect {
  min-height: 240px;
  padding: 24px;
  background: #ffffff;
}
</style>
