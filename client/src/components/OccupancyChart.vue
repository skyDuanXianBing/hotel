<template>
  <div ref="chartRef" class="occupancy-chart"></div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, watch } from 'vue'
import * as echarts from 'echarts'
import type { EChartsOption } from 'echarts'

interface OccupancyData {
  date: string
  rate: number
}

interface Props {
  data: OccupancyData[]
}

const props = defineProps<Props>()

const chartRef = ref<HTMLElement>()
let chartInstance: echarts.ECharts | null = null

const initChart = () => {
  if (!chartRef.value) return

  // 初始化图表实例
  chartInstance = echarts.init(chartRef.value)

  // 配置图表选项
  const option: EChartsOption = {
    grid: {
      left: '5%',
      right: '5%',
      top: '15%',
      bottom: '15%',
      containLabel: true,
    },
    xAxis: {
      type: 'category',
      data: props.data.map((item) => item.date),
      axisLabel: {
        fontSize: 12,
        color: '#666',
        rotate: 0,
      },
      axisLine: {
        lineStyle: {
          color: '#e0e0e0',
        },
      },
    },
    yAxis: {
      type: 'value',
      name: '入住率',
      nameTextStyle: {
        color: '#666',
        fontSize: 12,
      },
      axisLabel: {
        formatter: '{value}%',
        fontSize: 12,
        color: '#666',
      },
      axisLine: {
        show: false,
      },
      splitLine: {
        lineStyle: {
          color: '#f0f0f0',
        },
      },
    },
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: '#e0e0e0',
      borderWidth: 1,
      textStyle: {
        color: '#333',
      },
      formatter: (params: any) => {
        const param = params[0]
        return `${param.axisValue}<br/>入住率: ${param.value}%`
      },
    },
    series: [
      {
        type: 'line',
        data: props.data.map((item) => item.rate),
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        lineStyle: {
          color: '#4285f4',
          width: 3,
        },
        itemStyle: {
          color: '#4285f4',
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
                color: 'rgba(66, 133, 244, 0.3)',
              },
              {
                offset: 1,
                color: 'rgba(66, 133, 244, 0.05)',
              },
            ],
          },
        },
        emphasis: {
          focus: 'series',
          itemStyle: {
            color: '#4285f4',
            borderColor: '#fff',
            borderWidth: 2,
          },
        },
      },
    ],
  }

  chartInstance.setOption(option)
}

const updateChart = () => {
  if (!chartInstance) return

  chartInstance.setOption({
    xAxis: {
      data: props.data.map((item) => item.date),
    },
    series: [
      {
        data: props.data.map((item) => item.rate),
      },
    ],
  })
}

// 监听数据变化
watch(
  () => props.data,
  () => {
    updateChart()
  },
  { deep: true }
)

// 监听窗口大小变化
const handleResize = () => {
  chartInstance?.resize()
}

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
  height: 300px;
}
</style>