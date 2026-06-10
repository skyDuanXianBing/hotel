<template>
  <div ref="chartRef" class="occupancy-chart"></div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as echarts from 'echarts'
import type { EChartsOption } from 'echarts'
import { useI18n } from 'vue-i18n'

interface OccupancyData {
  date: string
  rate: number
}

interface Props {
  data: OccupancyData[]
}

const props = defineProps<Props>()
const { t } = useI18n()

const chartRef = ref<HTMLElement>()
let chartInstance: echarts.ECharts | null = null

const getRates = () => props.data.map((item) => Number(item.rate) || 0)
const getDates = () => props.data.map((item) => item.date)

const getYAxisMax = (rates: number[]) => {
  const maxRate = Math.max(...rates, 0)

  if (maxRate <= 1) {
    return 1
  }

  if (maxRate <= 10) {
    return 10
  }

  const steppedMax = Math.ceil(maxRate / 10) * 10
  return Math.min(100, steppedMax)
}

const formatAxisPercent = (value: number) => {
  if (value <= 1) {
    return value === 0 ? '0' : `${value}`
  }

  return `${Math.round(value)}%`
}

const buildOption = (): EChartsOption => {
  const rates = getRates()

  return {
    animationDuration: 500,
    grid: {
      left: 6,
      right: 4,
      top: 36,
      bottom: 18,
      containLabel: true,
    },
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255, 255, 255, 0.98)',
      borderColor: 'rgba(17, 24, 39, 0.06)',
      borderWidth: 1,
      padding: [10, 12],
      textStyle: {
        color: '#111111',
        fontSize: 12,
      },
      extraCssText: 'box-shadow: 0 14px 30px rgba(15, 23, 42, 0.08); border-radius: 12px;',
      formatter: (params: any) => {
        const param = params[0]
        return `${param.axisValue}<br/>${t('stage6.components.occupancyChart.tooltip', { value: param.value })}`
      },
    },
    xAxis: {
      type: 'category',
      data: getDates(),
      boundaryGap: true,
      axisTick: {
        show: false,
        alignWithLabel: false,
      },
      axisLine: {
        lineStyle: {
          color: 'rgba(0, 0, 0, 0.08)',
        },
      },
      axisLabel: {
        color: 'rgba(0, 0, 0, 0.46)',
        fontSize: 12,
        margin: 10,
      },
      splitLine: {
        show: true,
        lineStyle: {
          color: 'rgba(76, 111, 255, 0.12)',
          type: 'dashed',
        },
      },
    },
    yAxis: {
      type: 'value',
      name: t('stage6.components.occupancyChart.occupancyRate'),
      min: 0,
      max: getYAxisMax(rates),
      splitNumber: 5,
      nameTextStyle: {
        color: 'rgba(0, 0, 0, 0.56)',
        fontSize: 12,
        padding: [0, 0, 6, 0],
      },
      axisLabel: {
        color: 'rgba(0, 0, 0, 0.5)',
        fontSize: 12,
        formatter: (value: number) => formatAxisPercent(value),
      },
      axisLine: {
        show: false,
      },
      axisTick: {
        show: false,
      },
      splitLine: {
        lineStyle: {
          color: 'rgba(60, 195, 223, 0.18)',
          type: 'dashed',
        },
      },
    },
    series: [
      {
        type: 'line',
        data: rates,
        smooth: false,
        symbol: 'circle',
        symbolSize: 7,
        showSymbol: true,
        lineStyle: {
          color: '#3cc3df',
          width: 2,
        },
        itemStyle: {
          color: '#ffffff',
          borderColor: '#3cc3df',
          borderWidth: 1.5,
        },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              {
                offset: 0,
                color: 'rgba(60, 195, 223, 0.34)',
              },
              {
                offset: 1,
                color: 'rgba(228, 247, 252, 0.7)',
              },
            ],
          },
        },
        emphasis: {
          focus: 'series',
          itemStyle: {
            color: '#ffffff',
            borderColor: '#3cc3df',
            borderWidth: 2,
          },
        },
      },
    ],
  }
}

const initChart = () => {
  if (!chartRef.value) return

  chartInstance = echarts.init(chartRef.value)
  chartInstance.setOption(buildOption())
}

const updateChart = () => {
  if (!chartInstance) return
  chartInstance.setOption(buildOption(), true)
}

const handleResize = () => {
  chartInstance?.resize()
}

watch(
  () => t('stage6.components.occupancyChart.occupancyRate'),
  () => {
    updateChart()
  }
)

watch(
  () => props.data,
  () => {
    updateChart()
  },
  { deep: true }
)

onMounted(() => {
  initChart()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  chartInstance?.dispose()
})
</script>

<style scoped>
.occupancy-chart {
  width: 100%;
  height: 258px;
}
</style>
