<template>
  <div class="forbidden-page">
    <el-result
      icon="warning"
      title="暂无权限查看"
      sub-title="当前账号在所选门店下没有访问此页面的权限，请联系管理员调整角色权限。"
    >
      <template #extra>
        <el-button type="primary" @click="goHome">返回首页</el-button>
      </template>
    </el-result>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()

const goHome = () => {
  router.push('/')
}
</script>

<style scoped>
.forbidden-page {
  min-height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px 24px;
  background: #fff;
  border-radius: 12px;
}
</style>
