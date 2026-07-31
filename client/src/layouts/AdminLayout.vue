<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { Coin, DataAnalysis, Goods, Key, List, Notebook, SwitchButton } from '@element-plus/icons-vue'
import AdminChangePasswordDialog from '@/components/admin/AdminChangePasswordDialog.vue'
import LanguageSwitcher from '@/components/LanguageSwitcher.vue'
import { ADMIN_LOGIN_PATH, clearAdminSession, readAdminProfile } from '@/utils/adminSession'

/**
 * 平台管理端布局：简洁顶栏 + 侧栏。刻意不复用 MainLayout（与门店/RBAC 深耦合）。
 * P10：头部加语言切换（与租户端同一语言偏好，即时生效）与「修改密码」入口。
 */
const route = useRoute()
const router = useRouter()
const { t } = useI18n()

const profile = computed(() => readAdminProfile())

const changePasswordVisible = ref(false)

const menuItems = computed(() => [
  { path: '/admin/dashboard', label: t('admin.layout.menu.dashboard'), icon: DataAnalysis },
  { path: '/admin/packages', label: t('admin.layout.menu.packages'), icon: Goods },
  { path: '/admin/features', label: t('admin.layout.menu.features'), icon: Notebook },
  { path: '/admin/subscriptions', label: t('admin.layout.menu.subscriptions'), icon: List },
  { path: '/admin/quota', label: t('admin.layout.menu.quota'), icon: Coin },
])

const activeMenu = computed(() => route.path)

const handleLogout = () => {
  clearAdminSession()
  router.replace(ADMIN_LOGIN_PATH)
}

// 改密成功后强制重新登录（后端不吊销已签发 token，重新登录以新密码建立会话）
const handlePasswordChanged = () => {
  clearAdminSession()
  router.replace(ADMIN_LOGIN_PATH)
}
</script>

<template>
  <div class="admin-layout">
    <header class="admin-header">
      <div class="admin-brand">
        <span class="brand-title">{{ t('admin.layout.brand') }}</span>
        <span class="brand-sub">{{ t('admin.layout.brandSub') }}</span>
      </div>
      <div class="admin-header-right">
        <LanguageSwitcher display="icon" class="admin-language-switcher" />
        <template v-if="profile">
          <span class="admin-username">{{ profile.username }}</span>
          <el-tag size="small" effect="plain">{{ profile.role }}</el-tag>
        </template>
        <el-button :icon="Key" size="small" @click="changePasswordVisible = true">
          {{ t('admin.layout.changePassword') }}
        </el-button>
        <el-button :icon="SwitchButton" size="small" @click="handleLogout">
          {{ t('admin.layout.logout') }}
        </el-button>
      </div>
    </header>

    <AdminChangePasswordDialog
      v-model:visible="changePasswordVisible"
      @success="handlePasswordChanged"
    />

    <div class="admin-body">
      <aside class="admin-sidebar">
        <el-menu :default-active="activeMenu" router class="admin-menu">
          <el-menu-item v-for="item in menuItems" :key="item.path" :index="item.path">
            <el-icon><component :is="item.icon" /></el-icon>
            <span>{{ item.label }}</span>
          </el-menu-item>
        </el-menu>
      </aside>

      <main class="admin-main">
        <router-view />
      </main>
    </div>
  </div>
</template>

<style scoped>
.admin-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f7fa;
}

.admin-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 56px;
  padding: 0 20px;
  background: #1f2d3d;
  color: #fff;
}

.admin-brand {
  display: flex;
  align-items: baseline;
  gap: 10px;
}

.brand-title {
  font-size: 16px;
  font-weight: 700;
}

.brand-sub {
  color: rgba(255, 255, 255, 0.65);
  font-size: 12px;
}

.admin-header-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.admin-username {
  font-size: 13px;
  font-weight: 600;
}

.admin-body {
  flex: 1;
  display: flex;
  min-height: 0;
}

.admin-sidebar {
  width: 200px;
  flex: 0 0 auto;
  background: #fff;
  border-right: 1px solid #e4e7ed;
}

.admin-menu {
  border-right: none;
}

.admin-main {
  flex: 1;
  min-width: 0;
  padding: 20px;
  overflow: auto;
}

@media (max-width: 768px) {
  .admin-sidebar {
    width: 56px;
  }

  .admin-menu :deep(.el-menu-item span) {
    display: none;
  }
}
</style>
