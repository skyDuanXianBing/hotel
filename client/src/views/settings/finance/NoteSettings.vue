<template>
  <div class="note-settings-container">
    <!-- 收入项部分 -->
    <div class="section">
      <div class="section-header">
        <h3 class="section-title">收入项</h3>
        <el-button type="primary" @click="handleAddIncome">新增收入项</el-button>
      </div>

      <div class="items-grid">
        <div
          v-for="item in incomeItems"
          :key="item.id"
          class="grid-item"
        >
          <el-icon class="drag-icon"><Menu /></el-icon>
          <span class="item-name">{{ item.name }}</span>
        </div>

        <!-- 新增项的输入框 -->
        <div v-if="showIncomeInput" class="grid-item editing">
          <el-input
            v-model="newIncomeName"
            placeholder=""
            maxlength="20"
            @keyup.enter="handleSaveNewIncome"
          />
          <div class="item-actions">
            <el-button
              link
              type="success"
              :icon="Check"
              @click="handleSaveNewIncome"
            />
            <el-button
              link
              type="danger"
              :icon="Close"
              @click="handleCancelNewIncome"
            />
          </div>
        </div>
      </div>
    </div>

    <!-- 支出项部分 -->
    <div class="section">
      <div class="section-header">
        <h3 class="section-title">支出项</h3>
        <el-button type="primary" @click="handleAddExpense">新增支出项</el-button>
      </div>

      <div class="items-grid">
        <div
          v-for="item in expenseItems"
          :key="item.id"
          class="grid-item"
        >
          <el-icon class="drag-icon"><Menu /></el-icon>
          <span class="item-name">{{ item.name }}</span>
        </div>

        <!-- 新增项的输入框 -->
        <div v-if="showExpenseInput" class="grid-item editing">
          <el-input
            v-model="newExpenseName"
            placeholder=""
            maxlength="20"
            @keyup.enter="handleSaveNewExpense"
          />
          <div class="item-actions">
            <el-button
              link
              type="success"
              :icon="Check"
              @click="handleSaveNewExpense"
            />
            <el-button
              link
              type="danger"
              :icon="Close"
              @click="handleCancelNewExpense"
            />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { Check, Close, Menu } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

interface NoteItem {
  id: number
  name: string
  type: 'income' | 'expense'
}

// 收入项数据
const incomeItems = ref<NoteItem[]>([])

// 支出项数据
const expenseItems = ref<NoteItem[]>([])

// 控制输入框显示
const showIncomeInput = ref(false)
const showExpenseInput = ref(false)

// 新增项的名称
const newIncomeName = ref('')
const newExpenseName = ref('')

// 添加收入项
const handleAddIncome = () => {
  showIncomeInput.value = true
  newIncomeName.value = ''
}

// 保存新的收入项
const handleSaveNewIncome = () => {
  if (!newIncomeName.value.trim()) {
    ElMessage.warning('请输入收入项名称')
    return
  }

  const newItem: NoteItem = {
    id: Date.now(),
    name: newIncomeName.value,
    type: 'income',
  }

  incomeItems.value.push(newItem)
  ElMessage.success('添加成功')
  showIncomeInput.value = false
  newIncomeName.value = ''
}

// 取消新增收入项
const handleCancelNewIncome = () => {
  showIncomeInput.value = false
  newIncomeName.value = ''
}

// 添加支出项
const handleAddExpense = () => {
  showExpenseInput.value = true
  newExpenseName.value = ''
}

// 保存新的支出项
const handleSaveNewExpense = () => {
  if (!newExpenseName.value.trim()) {
    ElMessage.warning('请输入支出项名称')
    return
  }

  const newItem: NoteItem = {
    id: Date.now(),
    name: newExpenseName.value,
    type: 'expense',
  }

  expenseItems.value.push(newItem)
  ElMessage.success('添加成功')
  showExpenseInput.value = false
  newExpenseName.value = ''
}

// 取消新增支出项
const handleCancelNewExpense = () => {
  showExpenseInput.value = false
  newExpenseName.value = ''
}

</script>

<style scoped>
.note-settings-container {
  padding: 20px;
  background: #fff;
  min-height: calc(100vh - 100px);
}

.section {
  margin-bottom: 40px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 12px;
  border-bottom: 2px solid #e4e7ed;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

.items-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 16px;
}

.grid-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 16px;
  background: #f5f7fa;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s;
  min-height: 56px;
}

.grid-item:hover {
  border-color: #409eff;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.1);
}

.grid-item.editing {
  background: #fff;
  padding: 8px 12px;
  cursor: default;
}

.drag-icon {
  flex-shrink: 0;
  font-size: 16px;
  color: #909399;
}

.item-name {
  flex: 1;
  font-size: 14px;
  color: #303133;
}

.item-actions {
  display: flex;
  gap: 4px;
  flex-shrink: 0;
}

:deep(.el-button.is-link) {
  padding: 4px;
  font-size: 16px;
}

:deep(.el-input__wrapper) {
  padding: 8px 12px;
}

.grid-item.editing :deep(.el-input) {
  flex: 1;
}
</style>
