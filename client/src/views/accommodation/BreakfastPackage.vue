<template>
  <div class="breakfast-package">
    <div class="page-header">
      <h2>早餐套销</h2>
      <p>早餐套餐销售管理</p>
    </div>

    <!-- 功能开发中提示 -->
    <div class="under-development">
      <el-empty :image-size="200" description="此功能正在开发中，敬请期待..." />
    </div>
  </div>
</template>

<script setup lang="ts">
// 早餐套销功能实现
</script>

<style scoped>
.breakfast-package {
  padding: 24px;
  background: #fff;
  min-height: 100%;
}

.page-header {
  margin-bottom: 24px;
}

.page-header h2 {
  margin: 0 0 8px 0;
  font-size: 20px;
  font-weight: 500;
  color: #333;
}

.page-header p {
  margin: 0;
  color: #666;
  font-size: 14px;
}

.under-development {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 400px;
}
</style>
