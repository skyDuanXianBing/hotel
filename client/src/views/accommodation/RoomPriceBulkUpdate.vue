<template>
  <div class="bulk-update-page">
    <!-- 顶部Tab -->
    <div class="page-tabs">
      <el-tabs v-model="activeTab" @tab-click="handleTabClick">
        <el-tab-pane label="日历" name="calendar"></el-tab-pane>
        <el-tab-pane label="批量更新" name="bulk-update"></el-tab-pane>
      </el-tabs>
    </div>

    <!-- 批量更新表单 -->
    <div class="bulk-update-form">
      <!-- 房型&价位选择 -->
      <el-form-item label="房型&价位" required class="form-item room-type-select-wrapper">
        <!-- 选中的标签展示区 -->
        <div class="selected-tags-area" @click="toggleTreeDropdown">
          <!-- 占位文本 -->
          <span v-if="selectedTags.length === 0" class="placeholder-text">请选择</span>

          <!-- 选中的标签 -->
          <el-tag
            v-for="tag in selectedTags"
            :key="tag.value"
            closable
            @close.stop="removeTag(tag.value)"
            class="selected-tag"
          >
            {{ tag.label }}
          </el-tag>

          <!-- 下拉箭头 -->
          <el-icon class="dropdown-arrow" :class="{ 'is-reverse': showTreeDropdown }">
            <ArrowDown />
          </el-icon>
        </div>

        <!-- 树形选择下拉框(绝对定位浮层) -->
        <transition name="dropdown">
          <div v-show="showTreeDropdown" class="tree-select-dropdown">
            <el-input
              v-model="filterText"
              placeholder="请输入搜索"
              size="default"
              class="filter-input"
            />
            <el-tree
              ref="treeRef"
              :data="treeData"
              show-checkbox
              node-key="value"
              :default-checked-keys="selectedPlanIds"
              :props="{ children: 'children', label: 'label' }"
              :filter-node-method="filterNode"
              @check="handleTreeCheck"
              class="selection-tree"
            />
          </div>
        </transition>
      </el-form-item>

      <!-- 日期范围 -->
      <el-form-item label="日期范围" required class="form-item">
        <div class="date-range-wrapper">
          <el-date-picker
            v-model="startDate"
            type="date"
            placeholder="开始日期"
            format="YYYY/MM/DD"
            value-format="YYYY-MM-DD"
            size="default"
          />
          <span class="date-separator">至</span>
          <el-date-picker
            v-model="endDate"
            type="date"
            placeholder="结束日期"
            format="YYYY/MM/DD"
            value-format="YYYY-MM-DD"
            size="default"
          />
        </div>
      </el-form-item>

      <!-- 星期几 -->
      <el-form-item label="星期几" required class="form-item">
        <el-checkbox-group v-model="selectedWeekdays" class="weekday-group" @change="handleWeekdayChange">
          <el-checkbox :label="0">全部</el-checkbox>
          <el-checkbox :label="1">周一</el-checkbox>
          <el-checkbox :label="2">周二</el-checkbox>
          <el-checkbox :label="3">周三</el-checkbox>
          <el-checkbox :label="4">周四</el-checkbox>
          <el-checkbox :label="5">周五</el-checkbox>
          <el-checkbox :label="6">周六</el-checkbox>
          <el-checkbox :label="7">周日</el-checkbox>
        </el-checkbox-group>
      </el-form-item>

      <!-- 设置 -->
      <el-form-item label="设置" required class="form-item">
        <el-select v-model="priceType" placeholder="请选择" style="width: 200px">
          <el-option label="价格" value="price" />
          <el-option label="最小入住天数" value="minStay" />
          <el-option label="最大入住天数" value="maxStay" />
        </el-select>
      </el-form-item>

      <!-- 价格 -->
      <el-form-item v-if="priceType === 'price'" label="价格" class="form-item">
        <div class="price-mode-wrapper">
          <el-radio-group v-model="priceMode" class="price-mode-group">
            <el-radio value="fixed" label="固定值" />
            <el-radio value="relative" label="基于当前价格" />
          </el-radio-group>

          <!-- 固定值模式 -->
          <div v-if="priceMode === 'fixed'" class="unified-price-input">
            <el-input
              v-model="unifiedPriceValue"
              placeholder="请输入价格"
              type="number"
              style="width: 300px"
            >
              <template #append>JPY</template>
            </el-input>
            <el-button type="primary" @click="applyUnifiedPrice" style="margin-left: 12px">
              批量更新
            </el-button>
          </div>

          <!-- 基于当前价格模式 -->
          <div v-else class="relative-price-section">
            <div class="relative-price-options">
              <el-radio-group v-model="relativeType" class="relative-type-group">
                <el-radio value="cheaper" label="比当前价格便宜" />
                <el-radio value="expensive" label="比当前价格贵" />
              </el-radio-group>
            </div>

            <div class="unified-price-input">
              <el-input
                v-model="unifiedPriceValue"
                placeholder="请输入价格"
                type="number"
                style="width: 300px"
              >
                <template #prepend>{{ relativeType === 'cheaper' ? '-' : '+' }}</template>
                <template #append>JPY</template>
              </el-input>
              <el-button type="primary" @click="applyUnifiedPrice" style="margin-left: 12px">
                批量更新
              </el-button>
            </div>
          </div>

          <div class="price-hint">
            应用于全部的价格值或修改百分比
          </div>
        </div>
      </el-form-item>

      <!-- 最小入住天数 -->
      <el-form-item v-if="priceType === 'minStay'" label="最小入住天数" class="form-item">
        <div class="stay-days-wrapper">
          <el-input
            v-model="minStayDays"
            placeholder="请输入最小入住天数（1-99天）"
            type="number"
            style="width: 300px"
            :min="1"
            :max="99"
          >
            <template #append>天</template>
          </el-input>
          <el-button type="primary" @click="applyStayDays" style="margin-left: 12px">
            批量更新
          </el-button>
        </div>
      </el-form-item>

      <!-- 最大入住天数 -->
      <el-form-item v-if="priceType === 'maxStay'" label="最大入住天数" class="form-item">
        <div class="stay-days-wrapper">
          <el-input
            v-model="maxStayDays"
            placeholder="请输入最大入住天数（1-99天）"
            type="number"
            style="width: 300px"
            :min="1"
            :max="99"
          >
            <template #append>天</template>
          </el-input>
          <el-button type="primary" @click="applyStayDays" style="margin-left: 12px">
            批量更新
          </el-button>
        </div>
      </el-form-item>

      <!-- 价格表格 -->
      <div class="price-table-section">
        <!-- 空状态提示 -->
        <div v-if="priceTableData.length === 0" class="empty-state">
          <el-icon :size="48" color="#c0c4cc">
            <Document />
          </el-icon>
          <p class="empty-text">请先选择房型&价位</p>
        </div>

        <!-- 价格表格 -->
        <table v-else class="price-input-table">
          <thead>
            <tr>
              <th class="col-room-type">房型</th>
              <th class="col-price-plan">价格计划</th>
              <th class="col-price">价格</th>
              <th class="col-currency">货币</th>
              <th class="col-action">操作</th>
            </tr>
          </thead>
          <tbody>
            <template v-for="(roomType, roomIndex) in priceTableData" :key="roomType.id">
              <template v-for="(plan, planIndex) in roomType.plans" :key="plan.id">
                <tr :class="{ 'first-row': planIndex === 0 }">
                  <td v-if="planIndex === 0" :rowspan="roomType.plans.length" class="room-type-cell">
                    <div class="room-type-info">
                      <div class="room-type-name">{{ roomType.name }}</div>
                      <div class="room-type-code">{{ roomType.code }}</div>
                    </div>
                  </td>
                  <td class="price-plan-cell">
                    {{ plan.name }}
                  </td>
                  <td class="price-input-cell">
                    <el-input
                      v-model="plan.price"
                      placeholder="请输入价格"
                      type="number"
                    >
                      <template v-if="priceMode === 'relative'" #prepend>
                        {{ relativeType === 'cheaper' ? '-' : '+' }}
                      </template>
                      <template #append>JPY</template>
                    </el-input>
                  </td>
                  <td class="currency-cell">
                    JPY
                  </td>
                  <td class="action-cell">
                    <el-button
                      type="danger"
                      link
                      @click="removePricePlan(roomType.id, plan.id)"
                    >
                      删除
                    </el-button>
                  </td>
                </tr>
              </template>
            </template>
          </tbody>
        </table>
      </div>
    </div>

    <!-- 底部操作按钮 -->
    <div class="footer-actions">
      <el-button @click="handleCancel" size="large">取消</el-button>
      <el-button type="primary" @click="handleSave" size="large" :loading="saving">
        保存
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowDown, Document } from '@element-plus/icons-vue'

const router = useRouter()

const activeTab = ref('bulk-update')
const saving = ref(false)

// 控制树形选择器显示/隐藏
const showTreeDropdown = ref(false)

// 树形选择器的选中值
const selectedPlanIds = ref<string[]>([])

// 保存上一次的星期几选择值
let previousWeekdays: number[] = []

// 树形选择器数据
const treeData = ref([
  {
    value: 'all',
    label: '全选',
    children: [
      {
        value: '201',
        label: '豪华大床房201',
        children: [
          { value: '201-standard', label: 'Standard Rate' },
          { value: '201-rack', label: 'Rack Rate' }
        ]
      },
      {
        value: '401',
        label: '豪华双床房401',
        children: [
          { value: '401-standard', label: 'Standard Rate' },
          { value: '401-rack', label: 'Rack Rate' }
        ]
      }
    ]
  }
])

// 搜索过滤文本
const filterText = ref('')

// 树形选择器引用
const treeRef = ref()

// 计算选中的标签列表
interface Tag {
  value: string
  label: string
}

const selectedTags = computed<Tag[]>(() => {
  const labelMap: { [key: string]: string } = {
    '201-standard': '豪华大床房201_Standard Rate',
    '201-rack': '豪华大床房201_Rack Rate',
    '401-standard': '豪华双床房401_Standard Rate',
    '401-rack': '豪华双床房401_Rack Rate'
  }

  return selectedPlanIds.value
    .filter(id => id.includes('-'))
    .map(id => ({ value: id, label: labelMap[id] || id }))
})

// 移除标签
const removeTag = (value: string) => {
  const index = selectedPlanIds.value.indexOf(value)
  if (index > -1) {
    selectedPlanIds.value.splice(index, 1)
    updatePriceTableData(selectedPlanIds.value)
  }
}

// 树形选择器勾选处理
const handleTreeCheck = () => {
  if (!treeRef.value) return

  const checkedNodes = treeRef.value.getCheckedKeys() as string[]
  selectedPlanIds.value = checkedNodes.filter((key: string) => key.includes('-'))
  updatePriceTableData(selectedPlanIds.value)
}

// 树形选择器过滤方法
const filterNode = (value: string, data: any) => {
  if (!value) return true
  return data.label.toLowerCase().includes(value.toLowerCase())
}

// 监听搜索文本变化
watch(filterText, (val) => {
  if (treeRef.value) {
    treeRef.value.filter(val)
  }
})

// 切换树形选择器显示/隐藏
const toggleTreeDropdown = () => {
  showTreeDropdown.value = !showTreeDropdown.value
}

// 日期范围
const startDate = ref('2025-11-21')
const endDate = ref('2025-12-19')

// 星期几
const selectedWeekdays = ref<number[]>([])

// 设置
const priceType = ref('price')

// 价格模式
const priceMode = ref('fixed')

// 相对价格类型(比当前价格便宜/贵)
const relativeType = ref<'cheaper' | 'expensive'>('cheaper')

// 统一价格值
const unifiedPriceValue = ref('')

// 最小入住天数
const minStayDays = ref('')

// 最大入住天数
const maxStayDays = ref('')

// 价格表格数据
interface PricePlan {
  id: string
  name: string
  price: string
}

interface RoomTypePriceData {
  id: string
  name: string
  code: string
  plans: PricePlan[]
}

const priceTableData = ref<RoomTypePriceData[]>([])

// Tab切换
const handleTabClick = (tab: any) => {
  if (tab.props.name === 'calendar') {
    router.push('/accommodation/room-price-management')
  }
}

// 更新价格表格数据
const updatePriceTableData = (selectedIds: string[]) => {
  const newData: RoomTypePriceData[] = []

  if (selectedIds.includes('201-standard') || selectedIds.includes('201-rack')) {
    const plans: PricePlan[] = []
    if (selectedIds.includes('201-standard')) {
      plans.push({ id: 'standard', name: 'Standard Rate', price: '' })
    }
    if (selectedIds.includes('201-rack')) {
      plans.push({ id: 'rack', name: 'Rack Rate', price: '' })
    }
    if (plans.length > 0) {
      newData.push({
        id: '201',
        name: '豪华大床房',
        code: '地铁201',
        plans
      })
    }
  }

  if (selectedIds.includes('401-standard') || selectedIds.includes('401-rack')) {
    const plans: PricePlan[] = []
    if (selectedIds.includes('401-standard')) {
      plans.push({ id: 'standard', name: 'Standard Rate', price: '' })
    }
    if (selectedIds.includes('401-rack')) {
      plans.push({ id: 'rack', name: 'Rack Rate', price: '' })
    }
    if (plans.length > 0) {
      newData.push({
        id: '401',
        name: '豪华双床房',
        code: '地铁401',
        plans
      })
    }
  }

  priceTableData.value = newData
}


// 处理星期几选择变化
const handleWeekdayChange = (values: number[]) => {
  // 如果新选中了"全部"
  if (values.includes(0) && !previousWeekdays.includes(0)) {
    // 选中全部,同时选中周一到周日
    selectedWeekdays.value = [0, 1, 2, 3, 4, 5, 6, 7]
  }
  // 如果取消了"全部"
  else if (!values.includes(0) && previousWeekdays.includes(0)) {
    // 取消全部,同时取消所有
    selectedWeekdays.value = []
  }
  // 如果已经选中了全部,但取消了某个具体的星期
  else if (values.includes(0) && previousWeekdays.includes(0) && values.length < previousWeekdays.length) {
    // 取消全部,只保留剩余的具体星期
    selectedWeekdays.value = values.filter(v => v !== 0)
  }
  // 如果选中了周一到周日所有具体的星期(不包括全部)
  else if (!values.includes(0) && values.length === 7 && [1, 2, 3, 4, 5, 6, 7].every(v => values.includes(v))) {
    // 自动选中全部
    selectedWeekdays.value = [0, 1, 2, 3, 4, 5, 6, 7]
  }

  // 更新上一次的值
  nextTick(() => {
    previousWeekdays = [...selectedWeekdays.value]
  })
}

// 应用统一价格
const applyUnifiedPrice = () => {
  if (!unifiedPriceValue.value) {
    ElMessage.warning('请输入价格')
    return
  }

  const changeAmount = parseFloat(unifiedPriceValue.value)
  if (isNaN(changeAmount) || changeAmount <= 0) {
    ElMessage.warning('请输入有效的价格')
    return
  }

  // 将统一价格应用到所有价格计划
  priceTableData.value.forEach(roomType => {
    roomType.plans.forEach(plan => {
      if (priceMode.value === 'fixed') {
        // 固定值模式:直接设置价格
        plan.price = unifiedPriceValue.value
      } else {
        // 基于当前价格模式
        if (plan.price) {
          const currentPrice = parseFloat(plan.price)
          if (relativeType.value === 'cheaper') {
            // 比当前价格便宜:减去指定金额
            plan.price = Math.max(0, currentPrice - changeAmount).toString()
          } else {
            // 比当前价格贵:加上指定金额
            plan.price = (currentPrice + changeAmount).toString()
          }
        }
      }
    })
  })

  ElMessage.success('批量更新价格成功')
}

// 应用入住天数
const applyStayDays = () => {
  const daysValue = priceType.value === 'minStay' ? minStayDays.value : maxStayDays.value
  const label = priceType.value === 'minStay' ? '最小入住天数' : '最大入住天数'

  if (!daysValue) {
    ElMessage.warning(`请输入${label}`)
    return
  }

  const days = parseInt(daysValue)
  if (isNaN(days) || days <= 0) {
    ElMessage.warning(`请输入有效的${label}`)
    return
  }

  // TODO: 实现批量更新入住天数的逻辑
  // 需要调用后端API保存最小/最大入住天数
  ElMessage.success(`批量更新${label}成功`)
}

// 移除价格计划行
const removePricePlan = (roomTypeId: string, planId: string) => {
  const roomType = priceTableData.value.find(rt => rt.id === roomTypeId)
  if (roomType) {
    const planIndex = roomType.plans.findIndex(p => p.id === planId)
    if (planIndex > -1) {
      roomType.plans.splice(planIndex, 1)

      // 如果该房型没有价格计划了,移除整个房型
      if (roomType.plans.length === 0) {
        const roomTypeIndex = priceTableData.value.findIndex(rt => rt.id === roomTypeId)
        if (roomTypeIndex > -1) {
          priceTableData.value.splice(roomTypeIndex, 1)
        }
      }
    }
  }
}

// 取消
const handleCancel = () => {
  router.push('/accommodation/room-price-management')
}

// 保存
const handleSave = async () => {
  // 验证
  if (selectedPlanIds.value.length === 0) {
    ElMessage.warning('请选择房型&价位')
    return
  }

  if (!startDate.value || !endDate.value) {
    ElMessage.warning('请选择日期范围')
    return
  }

  // 验证价格是否都已填写
  let hasEmptyPrice = false
  for (const roomType of priceTableData.value) {
    for (const plan of roomType.plans) {
      if (!plan.price || plan.price === '') {
        hasEmptyPrice = true
        break
      }
    }
    if (hasEmptyPrice) break
  }

  if (hasEmptyPrice) {
    ElMessage.warning('请填写所有价格')
    return
  }

  try {
    saving.value = true

    // 模拟保存
    await new Promise(resolve => setTimeout(resolve, 1000))

    ElMessage.success('批量更新成功')
    router.push('/accommodation/room-price-management')
  } catch (error) {
    console.error('保存失败:', error)
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.bulk-update-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #fff;
}

/* Tab样式 */
.page-tabs {
  border-bottom: 1px solid #e4e7ed;
  padding: 0 24px;
}

.page-tabs :deep(.el-tabs__header) {
  margin: 0;
}

.page-tabs :deep(.el-tabs__nav-wrap::after) {
  display: none;
}

/* 表单区域 */
.bulk-update-form {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
}

.form-item {
  margin-bottom: 24px;
}

.form-item :deep(.el-form-item__label) {
  font-weight: 500;
  color: #303133;
  font-size: 14px;
}

/* 房型选择器包装器 - 需要相对定位 */
.room-type-select-wrapper {
  position: relative;
}

.room-type-select-wrapper :deep(.el-form-item__content) {
  position: relative;
}

/* 选中标签区域 */
.selected-tags-area {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  gap: 10px;
  padding: 8px 40px 8px 12px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  min-height: 40px;
  background: #fff;
  cursor: pointer;
  position: relative;
  transition: border-color 0.3s;
}

.selected-tags-area:hover {
  border-color: #409eff;
}

/* 占位文本 */
.placeholder-text {
  color: #c0c4cc;
  font-size: 14px;
  line-height: 24px;
  user-select: none;
}

.selected-tag {
  height: 32px;
  line-height: 30px;
  font-size: 13px;
}

/* 下拉箭头 */
.dropdown-arrow {
  position: absolute;
  right: 12px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 14px;
  color: #909399;
  transition: transform 0.3s;
}

.dropdown-arrow.is-reverse {
  transform: translateY(-50%) rotate(180deg);
}

/* 树形选择下拉框 - 绝对定位浮层 */
.tree-select-dropdown {
  position: absolute;
  top: calc(100% + 4px);
  left: 0;
  right: 0;
  z-index: 1000;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  padding: 12px;
  background: #fff;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

/* 下拉动画 */
.dropdown-enter-active {
  transition: all 0.3s ease-out;
}

.dropdown-leave-active {
  transition: all 0.2s ease-in;
}

.dropdown-enter-from,
.dropdown-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

.filter-input {
  margin-bottom: 12px;
}

.selection-tree {
  height: 240px;
  overflow-y: auto;
  overflow-x: hidden;
  border: 1px solid #f0f0f0;
  border-radius: 4px;
  padding: 8px;
}

.selection-tree :deep(.el-tree-node__content) {
  height: 36px;
  line-height: 36px;
  font-size: 14px;
}

.selection-tree :deep(.el-tree-node__content:hover) {
  background-color: #f5f7fa;
}

/* 日期范围 */
.date-range-wrapper {
  display: flex;
  align-items: center;
  gap: 12px;
}

.date-separator {
  color: #606266;
  font-size: 14px;
}

/* 星期几 */
.weekday-group {
  display: flex;
  gap: 16px;
}

.weekday-group :deep(.el-checkbox) {
  margin-right: 0;
}

/* 价格模式 */
.price-mode-wrapper {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.price-mode-group {
  display: flex;
  gap: 24px;
}

.unified-price-input {
  display: flex;
  align-items: center;
  margin-top: 12px;
}

/* 基于当前价格的选项区域 */
.relative-price-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.relative-price-options {
  display: flex;
  align-items: center;
  margin-top: 8px;
}

.relative-type-group {
  display: flex;
  gap: 24px;
}

.price-hint {
  color: #909399;
  font-size: 12px;
  margin-top: 8px;
}

.currency {
  font-weight: 500;
  color: #606266;
}

/* 价格表格 */
.price-table-section {
  margin-top: 24px;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  overflow: hidden;
}

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  background: #fafafa;
}

.empty-text {
  margin-top: 16px;
  font-size: 14px;
  color: #909399;
}

.price-input-table {
  width: 100%;
  border-collapse: collapse;
}

.price-input-table thead {
  background: #f5f7fa;
}

.price-input-table th {
  padding: 12px 16px;
  text-align: left;
  font-weight: 500;
  color: #606266;
  font-size: 14px;
  border-bottom: 1px solid #e4e7ed;
}

.price-input-table td {
  padding: 12px 16px;
  border-bottom: 1px solid #e4e7ed;
  font-size: 14px;
  color: #606266;
}

.price-input-table tbody tr:last-child td {
  border-bottom: none;
}

.col-room-type {
  width: 200px;
}

.col-price-plan {
  width: 300px;
}

.col-price {
  width: 400px;
}

.col-currency {
  width: 100px;
}

.col-action {
  width: 100px;
}

/* 房型单元格 */
.room-type-cell {
  vertical-align: top;
  background: #fafafa;
  border-right: 1px solid #e4e7ed;
}

.room-type-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.room-type-name {
  font-weight: 500;
  color: #303133;
  font-size: 14px;
}

.room-type-code {
  color: #909399;
  font-size: 12px;
}

/* 价格计划单元格 */
.price-plan-cell {
  color: #303133;
  font-weight: 400;
}

/* 价格输入单元格 */
.price-input-cell {
  padding: 8px 16px;
}

.price-input-cell :deep(.el-input__wrapper) {
  box-shadow: 0 0 0 1px #dcdfe6 inset;
}

/* 货币单元格 */
.currency-cell {
  color: #606266;
  font-weight: 500;
}

/* 操作单元格 */
.action-cell {
  text-align: center;
}

/* 底部操作按钮 */
.footer-actions {
  display: flex;
  justify-content: center;
  gap: 16px;
  padding: 16px 24px;
  border-top: 1px solid #e4e7ed;
  background: #fff;
}

.footer-actions .el-button {
  min-width: 100px;
}

/* 响应式 */
@media (max-width: 768px) {
  .bulk-update-form {
    padding: 16px;
  }

  .date-range-wrapper {
    flex-direction: column;
    align-items: stretch;
  }

  .weekday-group {
    flex-wrap: wrap;
  }

  .price-input-table {
    font-size: 12px;
  }

  .price-input-table th,
  .price-input-table td {
    padding: 8px;
  }
}
</style>
