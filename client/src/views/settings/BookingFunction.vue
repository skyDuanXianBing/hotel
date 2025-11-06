<script setup lang="ts">
import { ref } from 'vue'
import { Notebook } from '@element-plus/icons-vue'

const loading = ref(false)
</script>

<template>
  <div class="booking-function">
    <div class="page-header">
      <h3>订单功能设置</h3>
      <p class="page-description">配置订单管理的相关功能</p>
    </div>

    <div class="content-area">
      <el-card>
        <div class="placeholder-content">
          <el-icon size="48" color="#d9d9d9"><Notebook /></el-icon>
          <p>订单功能设置开发中...</p>
        </div>
      </el-card>
    </div>
  </div>
</template>

<style scoped>
.booking-function {
  padding: 20px;
  height: 100%;
}

.page-header {
  margin-bottom: 20px;
}

.page-header h3 {
  margin: 0 0 8px 0;
  color: #333;
  font-size: 18px;
}

.page-description {
  margin: 0;
  color: #666;
  font-size: 14px;
}

.content-area {
  height: calc(100% - 80px);
}

.placeholder-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 200px;
  color: #999;
}
</style>
