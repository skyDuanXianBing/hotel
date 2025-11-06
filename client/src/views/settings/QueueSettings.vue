<script setup lang="ts">
import { ref } from 'vue'
import { Sort } from '@element-plus/icons-vue'

const loading = ref(false)
</script>

<template>
  <div class="queue-settings">
    <div class="page-header">
      <h3>排序设置</h3>
      <p class="page-description">配置房间和订单的排序规则</p>
    </div>

    <div class="content-area">
      <el-card>
        <div class="placeholder-content">
          <el-icon size="48" color="#d9d9d9"><Sort /></el-icon>
          <p>排序设置功能开发中...</p>
        </div>
      </el-card>
    </div>
  </div>
</template>

<style scoped>
.queue-settings {
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
