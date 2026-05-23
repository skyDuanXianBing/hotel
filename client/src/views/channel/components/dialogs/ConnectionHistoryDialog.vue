<script setup lang="ts">
defineProps<{
  modelValue: boolean
  history: any[]
}>()

defineEmits<{
  'update:modelValue': [value: boolean]
}>()
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    title="连接历史"
    width="1000px"
    @update:model-value="$emit('update:modelValue', $event)"
  >
    <div class="history-dialog">
      <el-table :data="history" border class="history-table" empty-text="暂无真实连接历史">
        <el-table-column prop="hotelCode" label="酒店代码" min-width="120" align="center" />
        <el-table-column prop="storeType" label="Agoda门店类型" min-width="150" align="center" />
        <el-table-column prop="hotelName" label="酒店名称" min-width="200" align="center" />
        <el-table-column prop="status" label="状态" min-width="100" align="center" />
        <el-table-column prop="note" label="备注" min-width="150" align="center" />
        <el-table-column prop="createTime" label="创建时间" min-width="180" align="center" />
      </el-table>

      <!-- 空状态 -->
      <div v-if="history.length === 0" class="empty-state">
        <div class="empty-icon">
          <svg
            width="120"
            height="120"
            viewBox="0 0 120 120"
            fill="none"
            xmlns="http://www.w3.org/2000/svg"
          >
            <path
              d="M60 110C87.6142 110 110 87.6142 110 60C110 32.3858 87.6142 10 60 10C32.3858 10 10 32.3858 10 60C10 87.6142 32.3858 110 60 110Z"
              fill="#F0F2F5"
            />
            <path
              d="M45 50C48.3137 50 51 47.3137 51 44C51 40.6863 48.3137 38 45 38C41.6863 38 39 40.6863 39 44C39 47.3137 41.6863 50 45 50Z"
              fill="#D9D9D9"
            />
            <path
              d="M75 50C78.3137 50 81 47.3137 81 44C81 40.6863 78.3137 38 75 38C71.6863 38 69 40.6863 69 44C69 47.3137 71.6863 50 75 50Z"
              fill="#D9D9D9"
            />
            <path
              d="M80 68C80 75.732 71.046 82 60 82C48.954 82 40 75.732 40 68"
              stroke="#D9D9D9"
              stroke-width="4"
              stroke-linecap="round"
            />
          </svg>
        </div>
        <p class="empty-text">暂无真实连接历史</p>
        <p class="empty-tip">模拟连接流程请在独立 channel-simulator 中验证。</p>
      </div>
    </div>
  </el-dialog>
</template>

<style scoped>
.history-dialog {
  padding: 0;
}

.history-table {
  width: 100%;
  margin-bottom: 20px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 0;
}

.empty-icon {
  margin-bottom: 16px;
}

.empty-text {
  font-size: 14px;
  color: #909399;
  margin: 0;
}

.empty-tip {
  font-size: 13px;
  color: #a8abb2;
  margin: 8px 0 0;
}
</style>
