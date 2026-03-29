<template>
  <StatisticsReportLayout :definition="reportDefinition" />
</template>

<script setup lang="ts">
import StatisticsReportLayout from '@/components/statistics/StatisticsReportLayout.vue'
import { STATISTICS_REPORTS } from '@/constants/statistics'

const reportDefinition = STATISTICS_REPORTS.accommodationReport
</script>
