<template>
  <div class="forbidden-page">
    <el-result
      icon="warning"
      :title="t('pages.forbidden.title')"
      :sub-title="t('pages.forbidden.subtitle')"
    >
      <template #extra>
        <el-button type="primary" @click="goHome">{{ t('pages.forbidden.backHome') }}</el-button>
      </template>
    </el-result>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'

const router = useRouter()
const { t } = useI18n()

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
