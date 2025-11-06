<template>
  <StatisticsLayout>
    <div class="channel-summary-content">

    <!-- 日期选择器 -->
      <div class="date-selector">
        <el-select v-model="dateRange" class="date-quick-select" placeholder="自定义">
          <el-option label="今天" value="today" />
          <el-option label="昨天" value="yesterday" />
          <el-option label="本周" value="this-week" />
          <el-option label="本月" value="this-month" />
          <el-option label="自定义" value="custom" />
        </el-select>
        <el-date-picker
          v-model="startDate"
          type="date"
          placeholder="开始日期"
          format="YYYY-MM-DD"
          value-format="YYYY-MM-DD"
          class="date-picker"
        />
        <span class="date-separator">至</span>
        <el-date-picker
          v-model="endDate"
          type="date"
          placeholder="结束日期"
          format="YYYY-MM-DD"
          value-format="YYYY-MM-DD"
          class="date-picker"
        />
      </div>

    <!-- 图表区域 -->
      <div class="charts-section">
        <!-- 渠道消费分布 -->
        <div class="chart-container">
          <div class="chart-header">
            <h3>渠道消费分布</h3>
            <span class="chart-info">统计说明 <el-icon><QuestionFilled /></el-icon></span>
          </div>
          <div class="chart-content">
            <div class="pie-chart" ref="pieChartRef"></div>
          </div>
        </div>

        <!-- 渠道消费趋势 -->
        <div class="chart-container">
          <div class="chart-header">
            <h3>渠道消费趋势</h3>
            <span class="chart-info">统计说明 <el-icon><QuestionFilled /></el-icon></span>
          </div>
          <div class="chart-content">
            <div class="line-chart" ref="lineChartRef"></div>
          </div>
        </div>
      </div>

    <!-- 渠道明细 -->
      <div class="channel-details">
        <div class="details-header">
          <h3>渠道明细</h3>
          <span class="details-period">({{ formatDateRange }})</span>
          <div class="details-actions">
            <el-button type="primary" @click="exportDetails">导出明细</el-button>
          </div>
        </div>

        <!-- 明细表格切换 -->
        <div class="details-tabs">
          <el-tabs v-model="activeDetailTab">
            <el-tab-pane label="消费明细" name="consumption"></el-tab-pane>
            <el-tab-pane label="间夜明细" name="nights"></el-tab-pane>
          </el-tabs>
        </div>

        <div class="details-table">
          <el-table
            :data="channelDetails"
            style="width: 100%"
            :header-cell-style="{ background: '#f5f7fa', color: '#606266' }"
            show-summary
            :summary-method="getSummaries"
            border
          >
            <el-table-column prop="channel" label="项目" width="120" fixed>
              <template #default="scope">
                <span class="channel-name">{{ scope.row.channel }}</span>
              </template>
            </el-table-column>

            <el-table-column prop="total" label="合计" width="120" align="right" fixed>
              <template #default="scope">
                <span class="amount-value">¥{{ scope.row.total }}</span>
              </template>
            </el-table-column>

            <el-table-column
              v-for="col in dateColumns"
              :key="col.prop"
              :prop="col.prop"
              :label="col.label"
              width="120"
              align="right"
            >
              <template #default="scope">
                <span class="date-amount">¥{{ scope.row[col.prop] }}</span>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>
    </div>
  </StatisticsLayout>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import {
  QuestionFilled,
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import StatisticsLayout from './StatisticsLayout.vue'

const router = useRouter()

// 响应式数据
const dateRange = ref('custom')
const startDate = ref('2025-09-22')
const endDate = ref('2025-09-30')
const activeDetailTab = ref('consumption')

// ECharts实例
const pieChartRef = ref<HTMLElement>()
const lineChartRef = ref<HTMLElement>()
let pieChart: echarts.ECharts | null = null
let lineChart: echarts.ECharts | null = null

// 渠道明细数据
const channelDetails = ref([
  {
    channel: '自来客',
    total: '12.00',
    date_0922: '0.00',
    date_0923: '0.00',
    date_0924: '0.00',
    date_0925: '0.00',
    date_0926: '0.00',
    date_0927: '0.00',
    date_0928: '0.00',
    date_0929: '0.00',
    date_0930: '12.00',
  },
])

// 动态生成日期列
const dateColumns = computed(() => {
  const columns = []
  const start = new Date(startDate.value)
  const end = new Date(endDate.value)

  for (let d = new Date(start); d <= end; d.setDate(d.getDate() + 1)) {
    const month = (d.getMonth() + 1).toString()
    const day = d.getDate().toString()
    columns.push({
      prop: `date_${month.padStart(2, '0')}${day.padStart(2, '0')}`,
      label: `${month}月${day}日`,
    })
  }

  return columns
})

// 计算属性
const formatDateRange = computed(() => {
  if (startDate.value === endDate.value) {
    return startDate.value
  }
  return `${startDate.value} 至 ${endDate.value}`
})


// 方法

// 初始化饼图
const initPieChart = () => {
  if (!pieChartRef.value) return

  pieChart = echarts.init(pieChartRef.value)

  const option = {
    title: [
      {
        text: '消费总计(元)',
        left: '28%',
        top: '43%',
        textAlign: 'center',
        textStyle: {
          fontSize: 12,
          color: '#999',
          fontWeight: 'normal'
        }
      },
      {
        text: '¥ 12.00',
        left: '28%',
        top: '50%',
        textAlign: 'center',
        textStyle: {
          fontSize: 18,
          color: '#333',
          fontWeight: 'bold'
        }
      }
    ],
    tooltip: {
      trigger: 'item',
      formatter: '{b}: ¥{c} ({d}%)'
    },
    legend: {
      orient: 'vertical',
      right: '15%',
      top: 'center',
      itemWidth: 12,
      itemHeight: 12,
      itemGap: 16,
      textStyle: {
        fontSize: 13,
        color: '#333',
        rich: {
          name: {
            width: 60
          },
          percent: {
            width: 50,
            align: 'right'
          }
        }
      },
      formatter: (name: string) => {
        return `{name|${name}}{percent|100%}`
      }
    },
    series: [
      {
        name: '渠道消费',
        type: 'pie',
        radius: ['45%', '65%'],
        center: ['28%', '50%'],
        avoidLabelOverlap: false,
        label: {
          show: false
        },
        emphasis: {
          label: {
            show: false
          },
          scale: true,
          scaleSize: 5
        },
        labelLine: {
          show: false
        },
        data: [
          { value: 12.00, name: '自来客', itemStyle: { color: '#5470c6' } }
        ]
      }
    ]
  }

  pieChart.setOption(option)
}

// 初始化折线图
const initLineChart = () => {
  if (!lineChartRef.value) return

  lineChart = echarts.init(lineChartRef.value)

  const option = {
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: '#e0e0e0',
      borderWidth: 1,
      textStyle: {
        color: '#333'
      }
    },
    legend: {
      data: ['自来客'],
      bottom: '5%',
      itemWidth: 12,
      itemHeight: 12,
      icon: 'circle',
      textStyle: {
        fontSize: 13,
        color: '#333'
      }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '15%',
      top: '10%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: ['9月22日', '9月24日', '9月26日', '9月28日', '9月30日'],
      axisLabel: {
        fontSize: 12,
        color: '#666'
      },
      axisLine: {
        lineStyle: {
          color: '#e0e0e0'
        }
      },
      axisTick: {
        show: false
      }
    },
    yAxis: {
      type: 'value',
      max: 12,
      axisLabel: {
        fontSize: 12,
        color: '#666'
      },
      axisLine: {
        show: false
      },
      splitLine: {
        lineStyle: {
          color: '#f0f0f0'
        }
      }
    },
    series: [
      {
        name: '自来客',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 8,
        showSymbol: true,
        itemStyle: {
          color: '#5470c6',
          borderColor: '#fff',
          borderWidth: 2
        },
        lineStyle: {
          color: '#5470c6',
          width: 3
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
                color: 'rgba(84, 112, 198, 0.3)'
              },
              {
                offset: 1,
                color: 'rgba(84, 112, 198, 0.05)'
              }
            ]
          }
        },
        data: [0, 0, 0, 12, 0]
      }
    ]
  }

  lineChart.setOption(option)
}

// 表格汇总方法
const getSummaries = (param: any) => {
  const { columns, data } = param
  const sums: string[] = []
  columns.forEach((column: any, index: number) => {
    if (index === 0) {
      sums[index] = '合计'
      return
    }

    const prop = column.property
    if (prop === 'total' || prop.startsWith('date_')) {
      const values = data.map((item: any) => Number(item[prop]))
      if (!values.every((value: any) => Number.isNaN(value))) {
        const total = values.reduce((prev: number, curr: number) => {
          const value = Number(curr)
          if (!Number.isNaN(value)) {
            return prev + curr
          } else {
            return prev
          }
        }, 0)
        sums[index] = `¥${total.toFixed(2)}`
      } else {
        sums[index] = ''
      }
    } else {
      sums[index] = ''
    }
  })

  return sums
}

// 导出明细
const exportDetails = () => {
  ElMessage.success('渠道明细导出成功')
}

// 监听日期变化
watch(
  [startDate, endDate],
  () => {
    // 重新加载数据和更新图表
    nextTick(() => {
      if (pieChart) {
        pieChart.resize()
      }
      if (lineChart) {
        lineChart.resize()
      }
    })
  }
)

// 组件挂载
onMounted(() => {
  nextTick(() => {
    initPieChart()
    initLineChart()
  })
})

// 组件卸载时销毁图表
import { onBeforeUnmount } from 'vue'
onBeforeUnmount(() => {
  if (pieChart) {
    pieChart.dispose()
  }
  if (lineChart) {
    lineChart.dispose()
  }
})
</script>

<style scoped>
.channel-summary-content {
  width: 100%;
}

/* 日期选择器 */
.date-selector {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
  padding: 20px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.date-quick-select {
  width: 120px;
}

.date-picker {
  width: 180px;
}

.date-separator {
  font-size: 14px;
  color: #666;
}

/* 图表区域 */
.charts-section {
  display: grid;
  grid-template-columns: 500px 1fr;
  gap: 24px;
  margin-bottom: 32px;
}

.chart-container {
  background: white;
  border-radius: 8px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.chart-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.chart-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 500;
  color: #333;
}

.chart-info {
  font-size: 12px;
  color: #999;
  display: flex;
  align-items: center;
  gap: 4px;
  cursor: pointer;
}

.chart-content {
  min-height: 350px;
}

.pie-chart {
  width: 100%;
  height: 350px;
}

.line-chart {
  width: 100%;
  height: 300px;
}

/* 渠道明细 */
.channel-details {
  background: white;
  border-radius: 8px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.details-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
}

.details-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 500;
  color: #333;
}

.details-period {
  font-size: 14px;
  color: #666;
}

.details-actions {
  margin-left: auto;
}

.details-tabs {
  margin-bottom: 16px;
}

.details-table {
  margin-top: 16px;
  overflow-x: auto;
}

.details-table :deep(.el-table) {
  font-size: 13px;
}

.details-table :deep(.el-table__header-wrapper) {
  .cell {
    font-weight: 500;
  }
}

.channel-name {
  font-weight: 500;
  color: #333;
}

.amount-value,
.date-amount {
  font-weight: 500;
  color: #333;
}

/* 表格汇总行样式 */
.details-table :deep(.el-table__footer-wrapper) {
  .cell {
    font-weight: 500;
    color: #333;
  }
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .charts-section {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .channel-summary {
    flex-direction: column;
  }

  .sidebar {
    width: 100%;
    height: auto;
  }

  .main-content {
    padding: 16px;
  }

  .date-selector {
    flex-direction: column;
    align-items: stretch;
    gap: 8px;
  }

  .top-tabs {
    flex-wrap: wrap;
    gap: 16px;
  }
}
</style>