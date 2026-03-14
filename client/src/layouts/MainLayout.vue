<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { House, Message, Bell, User, EditPen, Headset, Wallet, Document, Phone, ArrowDown, HomeFilled } from '@element-plus/icons-vue'
import CustomerService from '@/components/CustomerService.vue'
import RecordTransaction from '@/components/RecordTransaction.vue'
import NotificationPopup from '@/components/NotificationPopup.vue'
import { useUserStore } from '@/stores/user'
import { useMemoStore } from '@/stores/memo'
import { useStoreStore } from '@/stores/store'
import { useNotificationCenterStore } from '@/stores/notificationCenter'
import type { StoreDTO } from '@/api/store'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const memoStore = useMemoStore()
const storeStore = useStoreStore()
const notificationCenterStore = useNotificationCenterStore()

const showCustomerService = ref(false)
const stores = ref<StoreDTO[]>([])
const notificationPopupRef = ref<InstanceType<typeof NotificationPopup> | null>(null)

// 当前选中的门店
const currentStore = computed(() => storeStore.currentStore)

// 加载门店列表
const loadStores = async () => {
  try {
    stores.value = await storeStore.fetchUserStores()
  } catch (error) {
    console.error('加载门店列表失败', error)
  }
}

// 切换门店
const handleStoreSelect = (store: StoreDTO) => {
  storeStore.setCurrentStore(store)
  ElMessage.success(`已切换到门店: ${store.name}`)
  // 刷新当前页面数据
  router.go(0)
}

// 跳转到门店选择/创建页面
const goToStoreSelection = () => {
  router.push('/store/selection')
}
const showRecordTransaction = ref(false)
const showMemoDialog = ref(false)
const showContactDialog = ref(false)

const currentUser = computed(() => userStore.currentUser)
const displayName = computed(() => currentUser.value?.nickname || currentUser.value?.email || '')

onMounted(() => {
  if (localStorage.getItem('token')) {
    userStore.fetchCurrentUser().catch(() => {
      /* ignore init failure; auth interceptor handles expired tokens */
    })
    // 加载备忘录
    memoStore.loadMemo()
    // 加载门店列表
    loadStores()
  }
})

watch(
  notificationPopupRef,
  (popup) => {
    notificationCenterStore.bindPopupController(
      popup as unknown as { addNotification: (notification: unknown) => void } | null
    )
  },
  { immediate: true },
)

watch(
  [() => userStore.currentUser?.id, () => currentStore.value?.id],
  ([userId, storeId]) => {
    if (!userId) {
      notificationCenterStore.stop()
      return
    }
    void notificationCenterStore.start({
      userId,
      storeId: typeof storeId === 'number' ? storeId : undefined,
      onOrderClick: () => router.push('/notifications/order'),
      onChatClick: () => router.push('/messages'),
    })
  },
  { immediate: true },
)

onUnmounted(() => {
  notificationCenterStore.stop()
  notificationCenterStore.bindPopupController(null)
})

const handleMenuClick = (path: string) => {
  router.push(path)
}

const handleRecordClick = () => {
  showRecordTransaction.value = true
}

const handleServiceClick = () => {
  showContactDialog.value = true
}

const handleChatTest = () => {
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

const handleMemoClick = () => {
  showMemoDialog.value = true
}

const handleMemoChange = (value: string) => {
  memoStore.saveMemoDebounced(value)
}

const handleSystemNotification = () => {
  router.push('/notifications/system')
}

const handleOrderNotification = () => {
  router.push('/notifications/order')
}

const handleInboxClick = () => {
  router.push('/messages')
}

const handleProfileClick = () => {
  router.push('/profile')
}

const handleHelpCenter = () => {
  ElMessage.info('帮助中心功能开发中...')
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
        <el-dropdown trigger="click" placement="bottom-start" class="store-dropdown" @command="handleStoreSelect">
          <div class="logo">
            <el-icon size="24" color="#1890ff"><HomeFilled /></el-icon>
            <span class="logo-text">{{ currentStore?.name || '房东智控中心' }}</span>
            <el-icon class="arrow-icon"><ArrowDown /></el-icon>
          </div>
          <template #dropdown>
            <el-dropdown-menu class="store-dropdown-menu">
              <div class="dropdown-header">
                <el-icon size="20" color="#1890ff"><HomeFilled /></el-icon>
                <span>PMS</span>
              </div>
              <el-dropdown-item
                v-for="store in stores"
                :key="store.id"
                :command="store"
                :class="{ 'is-active': currentStore?.id === store.id }"
              >
                <div class="store-item">
                  <span class="store-name">{{ store.name }}</span>
                  <span v-if="store.userRole === 'owner'" class="store-badge pro">Pro</span>
                  <span v-else class="store-badge ess">Ess</span>
                </div>
              </el-dropdown-item>
              <div class="dropdown-divider"></div>
              <el-dropdown-item divided @click.stop="goToStoreSelection">
                <div class="create-store-item">
                  <span>创建门店</span>
                  <el-icon><ArrowDown style="transform: rotate(-90deg)" /></el-icon>
                </div>
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>

      <nav class="header-nav">
        <el-menu
          mode="horizontal"
          :default-active="route.path"
          @select="handleMenuClick"
          class="nav-menu"
        >
          <el-menu-item index="/">首页</el-menu-item>
          <el-menu-item index="/accommodation">房态</el-menu-item>
          <el-menu-item index="/channel">渠道</el-menu-item>
          <el-menu-item index="/order">订单</el-menu-item>
          <el-menu-item index="/data-center/overview">统计</el-menu-item>
          <el-menu-item index="/data-center/registrations">审查</el-menu-item>
          <el-menu-item index="/settings">设置</el-menu-item>
        </el-menu>
      </nav>

      <div class="header-right">
        <div class="user-actions">
          <el-dropdown trigger="hover" placement="bottom">
            <el-button text>
              <el-icon><Message /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <div class="inbox-header" @click="handleInboxClick">收件箱</div>
                <el-dropdown-item @click="handleSystemNotification">
                  <span class="inbox-item">
                    <span class="inbox-dot system"></span>
                    系统通知
                  </span>
                </el-dropdown-item>
                <el-dropdown-item @click="handleOrderNotification">
                  <span class="inbox-item">
                    <span class="inbox-dot order"></span>
                    订单通知
                  </span>
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <el-tooltip content="备忘录" placement="bottom">
            <el-button text class="action-icon" @click="handleMemoClick">
              <el-icon><Document /></el-icon>
            </el-button>
          </el-tooltip>

          <el-tooltip content="记一笔" placement="bottom">
            <el-button text class="action-icon" @click="handleRecordClick">
              <el-icon><EditPen /></el-icon>
            </el-button>
          </el-tooltip>
          <el-dropdown trigger="hover" placement="bottom">
            <el-button text class="action-icon">
              <el-icon><Headset /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="handleServiceClick">
                  客户服务
                </el-dropdown-item>
                <el-dropdown-item @click="handleHelpCenter">
                  帮助中心
                </el-dropdown-item>
                <el-dropdown-item @click="handleChatTest">
                  测试聊天
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
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

    <!-- 备忘录弹窗 -->
    <el-dialog
      v-model="showMemoDialog"
      title="备忘录"
      width="600px"
      :close-on-click-modal="false"
    >
      <div class="memo-dialog-content">
        <div v-if="memoStore.lastSavedAt" class="memo-save-time">
          {{ memoStore.getFormattedSaveTime() }} 自动保存成功
        </div>
        <el-input
          :model-value="memoStore.memoContent"
          @update:model-value="handleMemoChange"
          type="textarea"
          :rows="12"
          placeholder="请输入备忘录内容"
          class="memo-textarea"
        />
      </div>
      <template #footer>
        <el-button @click="showMemoDialog = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 联系客服弹窗 -->
    <el-dialog
      v-model="showContactDialog"
      title="联系客服"
      width="600px"
      :close-on-click-modal="false"
      class="contact-dialog"
    >
      <div class="contact-content">
        <div class="contact-illustration">
          <img src="data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 400 300'%3E%3Ccircle cx='150' cy='150' r='100' fill='%23e8f4ff'/%3E%3Ccircle cx='250' cy='150' r='80' fill='%23ffe8f0'/%3E%3Ccircle cx='200' cy='80' r='60' fill='%23f0e8ff'/%3E%3C/svg%3E" alt="客服" class="illustration-bg" />
          <div class="illustration-people">
            <div class="person person-left">
              <div class="person-head"></div>
              <div class="person-body"></div>
              <div class="question-bubble">?</div>
            </div>
            <div class="person person-right">
              <div class="person-head"></div>
              <div class="person-body"></div>
            </div>
          </div>
        </div>

        <div class="contact-info">
          <h3>如果您需要帮助，请通过以下方式联系我们</h3>

          <div class="contact-item">
            <el-icon class="contact-icon" color="#409eff"><Message /></el-icon>
            <span class="contact-text">pms.support@the-host-jp.com</span>
          </div>

          <div class="contact-item">
            <el-icon class="contact-icon" color="#409eff"><Phone /></el-icon>
            <div class="contact-phone">
              <span class="contact-text">+81 03-6459-4606</span>
              <span class="contact-time">服务时间：周一至周五, 10:00-19:00(UTC+9)</span>
            </div>
          </div>
        </div>
      </div>
    </el-dialog>

    <NotificationPopup ref="notificationPopupRef" />
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

.store-dropdown {
  cursor: pointer;
}

.logo {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 18px;
  font-weight: bold;
  color: #1890ff;
  padding: 8px 12px;
  border-radius: 8px;
  transition: background 0.3s;
}

.logo:hover {
  background: #f5f7fa;
}

.logo-text {
  font-size: 16px;
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.arrow-icon {
  font-size: 12px;
  color: #999;
  margin-left: 4px;
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

/* 备忘录弹窗样式 */
.memo-dialog-content {
  padding: 0;
}

.memo-save-time {
  font-size: 12px;
  color: #909399;
  margin-bottom: 12px;
  text-align: right;
}

.memo-textarea {
  width: 100%;
}

.memo-textarea :deep(.el-textarea__inner) {
  font-family: inherit;
  line-height: 1.6;
}

/* 收件箱下拉菜单样式 */
.inbox-header {
  padding: 8px 16px;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  border-bottom: 1px solid #e8e8e8;
  margin-bottom: 4px;
  cursor: pointer;
  transition: background-color 0.3s ease;
}

.inbox-header:hover {
  background-color: #f5f7fa;
  color: #409eff;
}

.inbox-item {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
}

.inbox-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.inbox-dot.system {
  background: #ff4d4f;
}

.inbox-dot.order {
  background: #ff4d4f;
}

/* 联系客服弹窗样式 */
.contact-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20px 0;
}

.contact-illustration {
  position: relative;
  width: 300px;
  height: 200px;
  margin-bottom: 30px;
}

.illustration-bg {
  width: 100%;
  height: 100%;
  opacity: 0.3;
}

.illustration-people {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  display: flex;
  gap: 60px;
  align-items: center;
}

.person {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.person-head {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  margin-bottom: 5px;
}

.person-left .person-head {
  background: #5b8ff9;
}

.person-right .person-head {
  background: #5ad8a6;
}

.person-body {
  width: 60px;
  height: 70px;
  border-radius: 30px 30px 10px 10px;
}

.person-left .person-body {
  background: #5b8ff9;
}

.person-right .person-body {
  background: #5ad8a6;
}

.question-bubble {
  position: absolute;
  top: -10px;
  right: -30px;
  width: 35px;
  height: 35px;
  background: #5b8ff9;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 20px;
  font-weight: bold;
}

.contact-info {
  width: 100%;
}

.contact-info h3 {
  font-size: 16px;
  color: #333;
  margin: 0 0 30px 0;
  text-align: center;
  font-weight: 500;
}

.contact-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 12px 20px;
  margin-bottom: 16px;
}

.contact-icon {
  font-size: 20px;
  margin-top: 2px;
}

.contact-text {
  font-size: 15px;
  color: #333;
  font-weight: 500;
}

.contact-phone {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.contact-time {
  font-size: 13px;
  color: #999;
}

/* 门店下拉菜单样式 */
:deep(.store-dropdown-menu) {
  min-width: 280px;
  padding: 8px 0;
}

:deep(.store-dropdown-menu .dropdown-header) {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  font-size: 14px;
  font-weight: 500;
  color: #333;
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 4px;
}

:deep(.store-dropdown-menu .el-dropdown-menu__item) {
  padding: 10px 16px;
}

:deep(.store-dropdown-menu .el-dropdown-menu__item.is-active) {
  background: #e6f4ff;
  color: #1890ff;
}

.store-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  gap: 12px;
}

.store-item .store-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.store-badge {
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 500;
  flex-shrink: 0;
}

.store-badge.pro {
  background: #fff3e0;
  color: #e67e22;
}

.store-badge.ess {
  background: #e8f5e9;
  color: #27ae60;
}

:deep(.store-dropdown-menu .dropdown-divider) {
  height: 1px;
  background: #f0f0f0;
  margin: 4px 0;
}

.create-store-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  color: #666;
}

.create-store-item:hover {
  color: #1890ff;
}
</style>
