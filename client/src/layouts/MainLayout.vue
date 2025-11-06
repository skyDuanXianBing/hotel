<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { House, Message, Bell, User, EditPen, Headset, Wallet } from '@element-plus/icons-vue'
import CustomerService from '@/components/CustomerService.vue'
import RecordTransaction from '@/components/RecordTransaction.vue'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const showCustomerService = ref(false)
const showRecordTransaction = ref(false)

const currentUser = computed(() => userStore.currentUser)
const displayName = computed(() => currentUser.value?.nickname || currentUser.value?.email || '')

onMounted(() => {
  if (localStorage.getItem('token')) {
    userStore.fetchCurrentUser().catch(() => {
      /* ignore init failure; auth interceptor handles expired tokens */
    })
  }
})

const handleMenuClick = (path: string) => {
  router.push(path)
}

const handleRecordClick = () => {
  showRecordTransaction.value = true
}

const handleServiceClick = () => {
  showCustomerService.value = !showCustomerService.value
}

const handleWalletClick = () => {
  router.push('/wallet')
}

const handleCustomerServiceClose = () => {
  showCustomerService.value = false
}

const handleCustomerServiceMinimize = () => {
  showCustomerService.value = false
}

const handleRecordTransactionSuccess = () => {
  // hook for post-save logic
}

const handleProfileClick = () => {
  router.push('/profile')
}

const handleLogout = async () => {
  try {
    await userStore.logout()
    ElMessage.success('已退出登录')
  } catch (error: unknown) {
    const message = error instanceof Error ? error.message : '退出登录失败'
    ElMessage.error(message)
  } finally {
    router.replace('/login')
  }
}
</script>

<template>
  <div class="main-layout">
    <header class="app-header">
      <div class="header-left">
        <div class="logo">
          <el-icon size="24"><House /></el-icon>
          <span class="logo-text">酒店管理系统</span>
        </div>
      </div>

      <nav class="header-nav">
        <el-menu
          mode="horizontal"
          :default-active="route.path"
          @select="handleMenuClick"
          class="nav-menu"
        >
          <el-menu-item index="/">首页</el-menu-item>
          <el-menu-item index="/accommodation">住宿</el-menu-item>
          <el-menu-item index="/channel">渠道</el-menu-item>
          <el-menu-item index="/order">订单</el-menu-item>
          <el-menu-item index="/statistics">统计</el-menu-item>
          <el-menu-item index="/chat">客服</el-menu-item>
          <el-menu-item index="/settings">设置</el-menu-item>
        </el-menu>
      </nav>

      <div class="header-right">
        <div class="user-actions">
          <el-button text>
            <el-icon><Message /></el-icon>
          </el-button>
          <el-button text>
            <el-icon><Bell /></el-icon>
          </el-button>
          <el-tooltip content="记一笔" placement="bottom">
            <el-button text class="action-icon" @click="handleRecordClick">
              <el-icon><EditPen /></el-icon>
            </el-button>
          </el-tooltip>
          <el-tooltip content="聊天测试" placement="bottom">
            <el-button text class="action-icon" @click="handleServiceClick">
              <el-icon><Headset /></el-icon>
            </el-button>
          </el-tooltip>
          <el-tooltip content="钱包" placement="bottom">
            <el-button text class="action-icon" @click="handleWalletClick">
              <el-icon><Wallet /></el-icon>
            </el-button>
          </el-tooltip>
          <span v-if="displayName" class="user-name">{{ displayName }}</span>
          <el-dropdown trigger="click">
            <el-button circle>
              <el-icon><User /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="handleProfileClick">个人中心</el-dropdown-item>
                <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </header>

    <main class="layout-main">
      <router-view />
    </main>

    <CustomerService
      :visible="showCustomerService"
      @close="handleCustomerServiceClose"
      @minimize="handleCustomerServiceMinimize"
    />

    <RecordTransaction
      v-model="showRecordTransaction"
      @success="handleRecordTransactionSuccess"
    />
  </div>
</template>

<style scoped>
.main-layout {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f5f5;
}

.app-header {
  height: 60px;
  background: #fff;
  border-bottom: 1px solid #e8e8e8;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  z-index: 1000;
}

.header-left {
  display: flex;
  align-items: center;
}

.logo {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 18px;
  font-weight: bold;
  color: #1890ff;
}

.logo-text {
  font-size: 16px;
}

.header-nav {
  flex: 1;
  display: flex;
  justify-content: center;
  min-width: 700px;
  overflow: visible;
}

.nav-menu {
  border-bottom: none;
  background: transparent;
  flex-wrap: nowrap;
  overflow: visible;
  width: 100%;
}

.nav-menu :deep(.el-menu--horizontal) {
  border-bottom: none;
  width: 100%;
  overflow: visible;
}

.nav-menu :deep(.el-menu--horizontal > .el-menu-item) {
  white-space: nowrap;
  flex-shrink: 0;
}

/* 强制隐藏溢出菜单的三个点 */
.nav-menu :deep(.el-menu--horizontal .el-sub-menu__title) {
  display: none !important;
}

.nav-menu :deep(.el-menu--horizontal .el-sub-menu) {
  display: none !important;
}

/* 确保所有菜单项都显示 */
.nav-menu :deep(.el-menu--horizontal .el-menu-item) {
  display: inline-block !important;
  visibility: visible !important;
}

.nav-menu .el-menu-item {
  border-bottom: 2px solid transparent;
}

.nav-menu .el-menu-item.is-active {
  color: #1890ff;
  border-bottom-color: #1890ff;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 20px;
}

.user-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.user-name {
  max-width: 140px;
  font-size: 14px;
  color: #333;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.action-icon {
  color: #666;
  transition: color 0.3s ease;
}

.action-icon:hover {
  color: #1890ff;
}

.layout-main {
  flex: 1;
  overflow: auto;
}
</style>
