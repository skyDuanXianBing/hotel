<script setup lang="ts">
// 定价工具页面 - 占位组件
</script>

<template>
  <div class="settings-page">
    <div class="page-header">
      <h2>定价工具</h2>
      <p class="page-description">集成第三方定价工具和服务</p>
    </div>
    <div class="page-content">
      <el-empty description="该功能正在开发中,敬请期待" />
    </div>
  </div>
</template>

<style scoped>
.settings-page {
  padding: 24px;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.page-header {
  margin-bottom: 24px;
}

.page-header h2 {
  font-size: 20px;
  font-weight: 600;
  color: #333;
  margin: 0 0 8px 0;
}

.page-description {
  font-size: 14px;
  color: #666;
  margin: 0;
}

.page-content {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fafafa;
  border-radius: 8px;
}
</style>
