<template>
  <ion-page>
    <ion-header translucent>
      <ion-toolbar>
        <ion-title class="mobile-toolbar-title">设置</ion-title>
      </ion-toolbar>
    </ion-header>

    <ion-content fullscreen class="mobile-page settings-page">
      <section class="settings-hero">
        <div class="settings-hero__row">
          <h1 class="settings-hero__title">{{ storeTitle }}</h1>
          <span class="settings-hero__role">{{ storeRoleLabel }}</span>
        </div>
        <p class="settings-hero__user">{{ currentUserLabel }}</p>
      </section>

      <div class="mobile-stack">
        <section v-for="group in entryGroups" :key="group.key" class="settings-group">
          <h2 class="settings-group__title">{{ group.title }}</h2>

          <div class="settings-group__list">
            <button
              v-for="entry in group.items"
              :key="entry.key"
              type="button"
              class="settings-entry"
              @click="handleOpenEntry(entry.path)"
            >
              <div class="settings-entry__icon">
                <ion-icon :icon="entry.icon" />
              </div>
              <div class="settings-entry__body">
                <strong>{{ entry.title }}</strong>
                <p>{{ entry.description }}</p>
              </div>
              <span v-if="entry.badge" class="settings-entry__badge">{{ entry.badge }}</span>
              <ion-icon :icon="chevronForwardOutline" class="settings-entry__arrow" />
            </button>
          </div>
        </section>

        <section class="settings-group">
          <h2 class="settings-group__title">显示与主题</h2>
          <div class="settings-theme-panel">
            <div class="settings-theme-panel__header">
              <div>
                <strong>主题模式</strong>
                <p>可选择跟随系统，或固定浅色 / 深色显示。</p>
              </div>
              <span class="settings-entry__badge">{{ themePreferenceLabel }}</span>
            </div>

            <ion-segment :value="themePreference" @ionChange="handleThemePreferenceChange">
              <ion-segment-button value="system">
                <ion-label>跟随系统</ion-label>
              </ion-segment-button>
              <ion-segment-button value="light">
                <ion-label>浅色</ion-label>
              </ion-segment-button>
              <ion-segment-button value="dark">
                <ion-label>深色</ion-label>
              </ion-segment-button>
            </ion-segment>
          </div>
        </section>

        <section class="settings-group">
          <h2 class="settings-group__title">账号操作</h2>
          <div class="settings-group__list">
            <button type="button" class="settings-entry" @click="handleOpenProfile">
              <div class="settings-entry__icon settings-entry__icon--secondary">
                <ion-icon :icon="personOutline" />
              </div>
              <div class="settings-entry__body">
                <strong>个人中心</strong>
                <p>{{ currentUserLabel }}</p>
              </div>
              <ion-icon :icon="chevronForwardOutline" class="settings-entry__arrow" />
            </button>
            <button type="button" class="settings-entry" @click="handleOpenPassword">
              <div class="settings-entry__icon settings-entry__icon--secondary">
                <ion-icon :icon="lockClosedOutline" />
              </div>
              <div class="settings-entry__body">
                <strong>修改密码</strong>
              </div>
              <ion-icon :icon="chevronForwardOutline" class="settings-entry__arrow" />
            </button>
            <button type="button" class="settings-entry settings-entry--danger" @click="handleLogout">
              <div class="settings-entry__icon settings-entry__icon--danger">
                <ion-icon :icon="logOutOutline" />
              </div>
              <div class="settings-entry__body">
                <strong>退出登录</strong>
              </div>
            </button>
          </div>
        </section>

        <section class="settings-group">
          <h2 class="settings-group__title">快捷工具</h2>
          <div class="settings-tools-row">
            <button type="button" class="settings-tool-btn" @click="handleOpenMemoTool">
              <ion-icon :icon="documentTextOutline" />
              <span>备忘录</span>
            </button>
            <button type="button" class="settings-tool-btn" @click="handleOpenRecordTool">
              <ion-icon :icon="createOutline" />
              <span>记一笔</span>
            </button>
            <button type="button" class="settings-tool-btn" @click="handleOpenContactTool">
              <ion-icon :icon="headsetOutline" />
              <span>联系客服</span>
            </button>
          </div>
        </section>
      </div>
    </ion-content>
  </ion-page>
</template>

<script setup lang="ts">
import {
  IonContent,
  IonHeader,
  IonIcon,
  IonLabel,
  IonPage,
  IonSegment,
  IonSegmentButton,
  IonTitle,
  IonToolbar,
} from '@ionic/vue'
import {
  bedOutline,
  buildOutline,
  cashOutline,
  chatboxEllipsesOutline,
  chevronForwardOutline,
  colorWandOutline,
  constructOutline,
  createOutline,
  documentTextOutline,
  headsetOutline,
  lockClosedOutline,
  logOutOutline,
  notificationsOutline,
  peopleOutline,
  personOutline,
  pricetagOutline,
  receiptOutline,
  settingsOutline,
  storefrontOutline,
  swapHorizontalOutline,
  walletOutline,
} from 'ionicons/icons'
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ROUTE_PATHS } from '@/router/guards'
import { useStoreStore } from '@/stores/store'
import { useUserStore } from '@/stores/user'
import { useVisibleToolsStore } from '@/stores/visibleTools'
import { showSuccessToast } from '@/utils/notify'
import {
  applyThemePreference,
  getStoredThemePreference,
  setStoredThemePreference,
  type ThemePreference,
} from '@/utils/theme'

interface SettingsEntry {
  key: string
  title: string
  description: string
  path: string
  icon: string
  badge?: string
}

interface SettingsGroup {
  key: string
  title: string
  description: string
  items: SettingsEntry[]
}

const router = useRouter()
const storeStore = useStoreStore()
const userStore = useUserStore()
const visibleToolsStore = useVisibleToolsStore()
const themePreference = ref<ThemePreference>(getStoredThemePreference())

const currentUserLabel = computed(() => {
  if (!userStore.currentUser) {
    return '未恢复用户信息'
  }

  return `${userStore.currentUser.nickname} · ${userStore.currentUser.email}`
})

const storeTitle = computed(() => {
  if (!storeStore.currentStore?.name) {
    return '当前门店'
  }

  return storeStore.currentStore.name
})

const storeRoleLabel = computed(() => {
  const role = storeStore.currentStore?.userRole
  if (role === 'owner') {
    return '负责人'
  }
  if (role === 'admin') {
    return '管理员'
  }
  if (role === 'member') {
    return '成员'
  }
  return '待确认'
})

const themePreferenceLabel = computed(() => {
  if (themePreference.value === 'light') {
    return '浅色'
  }

  if (themePreference.value === 'dark') {
    return '深色'
  }

  return '跟随系统'
})

const entryGroups = computed<SettingsGroup[]>(() => {
  return [
    {
      key: 'store-account',
      title: '门店与账号',
      description: '门店基础资料、成员角色与权限。',
      items: [
        {
          key: 'store-profile',
          title: '门店资料',
          description: '基础资料、联系方式',
          path: ROUTE_PATHS.settingsStoreProfile,
          icon: storefrontOutline,
          badge: storeStore.currentStore?.city || '',
        },
        {
          key: 'store-details',
          title: '门店详情',
          description: '设施、照片、入住退房时间',
          path: ROUTE_PATHS.settingsStoreDetails,
          icon: buildOutline,
        },
        {
          key: 'store-members',
          title: '门店成员',
          description: '成员管理、角色分配',
          path: ROUTE_PATHS.settingsStoreMembers,
          icon: peopleOutline,
        },
      ],
    },
    {
      key: 'accommodation',
      title: '住宿设置',
      description: '房型、分组、价格计划与消费项。',
      items: [
        {
          key: 'room-types',
          title: '房型设置',
          description: '房型、房间号、价格',
          path: ROUTE_PATHS.settingsRoomTypes,
          icon: bedOutline,
        },
        {
          key: 'room-groups',
          title: '房间分组',
          description: '分组与成员管理',
          path: ROUTE_PATHS.settingsRoomGroups,
          icon: buildOutline,
        },
        {
          key: 'room-sort',
          title: '排序设置',
          description: '房型、房间显示顺序',
          path: ROUTE_PATHS.settingsRoomSort,
          icon: swapHorizontalOutline,
        },
        {
          key: 'price-plans',
          title: '价格计划',
          description: '价格计划与房型关联',
          path: ROUTE_PATHS.settingsPricePlans,
          icon: pricetagOutline,
        },
        {
          key: 'consumption-items',
          title: '消费项',
          description: '消费项维护与分类',
          path: ROUTE_PATHS.settingsConsumptionItems,
          icon: receiptOutline,
        },
      ],
    },
    {
      key: 'finance',
      title: '财务设置',
      description: '收款方式和记一笔分类。',
      items: [
        {
          key: 'payment-methods',
          title: '收款方式',
          description: '收款方式与启停',
          path: ROUTE_PATHS.settingsPaymentMethods,
          icon: walletOutline,
        },
        {
          key: 'note-settings',
          title: '记一笔设置',
          description: '收入/支出分类',
          path: ROUTE_PATHS.settingsNoteSettings,
          icon: cashOutline,
        },
      ],
    },
    {
      key: 'general',
      title: '通用设置',
      description: '通知、渠道、快捷回复与自动消息。',
      items: [
        {
          key: 'notification',
          title: '通知设置',
          description: '弹框与声音提醒',
          path: ROUTE_PATHS.settingsNotification,
          icon: notificationsOutline,
        },
        {
          key: 'channel-settings',
          title: '渠道设置',
          description: '渠道名称、颜色与状态',
          path: ROUTE_PATHS.settingsChannelSettings,
          icon: colorWandOutline,
        },
        {
          key: 'quick-replies',
          title: '快捷回复',
          description: '常用回复模板',
          path: ROUTE_PATHS.settingsQuickReplies,
          icon: chatboxEllipsesOutline,
        },
        {
          key: 'auto-messages',
          title: '自动消息',
          description: '触发消息与发送时机',
          path: ROUTE_PATHS.settingsAutoMessages,
          icon: settingsOutline,
        },
      ],
    },
    {
      key: 'cleaning',
      title: '保洁设置',
      description: '保洁时段、保洁员与易耗品。',
      items: [
        {
          key: 'cleaning-settings',
          title: '保洁设置',
          description: '时间窗口与保洁员',
          path: ROUTE_PATHS.settingsCleaningSettings,
          icon: constructOutline,
        },
        {
          key: 'cleaning-supplies',
          title: '易耗品',
          description: '按房型维护补给项',
          path: ROUTE_PATHS.settingsCleaningSupplies,
          icon: documentTextOutline,
        },
      ],
    },
    {
      key: 'third-party',
      title: '第三方集成',
      description: '定价工具与支付平台。',
      items: [
        {
          key: 'pricing-tools',
          title: '定价工具',
          description: 'PriceLabs 集成与同步',
          path: ROUTE_PATHS.settingsPricingTools,
          icon: pricetagOutline,
        },
        {
          key: 'payment-platforms',
          title: '支付平台',
          description: '平台状态与说明',
          path: ROUTE_PATHS.settingsPaymentPlatforms,
          icon: walletOutline,
        },
      ],
    },
  ]
})

const entryCount = computed(() => {
  let count = 0
  for (const group of entryGroups.value) {
    count += group.items.length
  }
  return count
})

async function handleOpenEntry(path: string) {
  await router.push(path)
}

async function handleLogout() {
  await userStore.logout()
  await router.replace(ROUTE_PATHS.login)
}

async function handleOpenProfile() {
  await router.push(ROUTE_PATHS.profile)
}

async function handleOpenPassword() {
  await router.push(ROUTE_PATHS.profile)
}

function handleOpenMemoTool() {
  visibleToolsStore.openMemo()
}

function handleOpenRecordTool() {
  visibleToolsStore.openRecord()
}

function handleOpenContactTool() {
  visibleToolsStore.openContact()
}

function handleThemePreferenceChange(event: CustomEvent<{ value?: string | number }>) {
  const nextValue = event.detail.value
  if (nextValue !== 'system' && nextValue !== 'light' && nextValue !== 'dark') {
    return
  }

  themePreference.value = nextValue
  setStoredThemePreference(nextValue)
  applyThemePreference(nextValue)
  showSuccessToast('主题模式已更新')
}
</script>

<style scoped>
.settings-page {
  display: block;
}

.settings-hero {
  padding: 18px;
  border: 1px solid var(--app-border);
  border-radius: 20px;
  background:
    linear-gradient(135deg, rgba(var(--ion-color-primary-rgb), 0.08), transparent 60%),
    var(--app-surface-strong);
  box-shadow: var(--app-shadow);
}

.settings-hero__row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.settings-hero__title {
  margin: 0;
  color: var(--app-heading);
  font-size: 20px;
  font-weight: 700;
}

.settings-hero__role {
  flex-shrink: 0;
  padding: 4px 10px;
  border-radius: 999px;
  background: var(--app-primary-soft);
  color: var(--ion-color-primary);
  font-size: 11px;
  font-weight: 600;
}

.settings-hero__user {
  margin: 6px 0 0;
  color: var(--app-muted);
  font-size: 13px;
}

.settings-group {
  padding: 16px;
  border: 1px solid var(--app-border);
  border-radius: 20px;
  background: var(--app-surface);
  box-shadow: var(--app-shadow);
}

.settings-group__title {
  margin: 0 0 12px;
  color: var(--app-heading);
  font-size: 15px;
  font-weight: 700;
}

.settings-group__list {
  display: grid;
  gap: 2px;
}

.settings-theme-panel {
  display: grid;
  gap: 12px;
}

.settings-theme-panel__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.settings-theme-panel__header strong {
  display: block;
  color: var(--app-heading);
  font-size: 14px;
}

.settings-theme-panel__header p {
  margin: 4px 0 0;
  color: var(--app-muted);
  font-size: 12px;
  line-height: 1.5;
}

.settings-entry {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  padding: 12px;
  border: none;
  border-radius: 14px;
  background: transparent;
  font: inherit;
  text-align: left;
  transition: background 0.15s ease;
}

.settings-entry:active {
  background: var(--app-primary-soft);
}

.settings-entry__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  width: 38px;
  height: 38px;
  border-radius: 10px;
  background: var(--app-primary-soft);
  color: var(--ion-color-primary);
  font-size: 18px;
}

.settings-entry__icon--secondary {
  background: var(--app-secondary-soft);
  color: var(--ion-color-secondary);
}

.settings-entry__icon--danger {
  background: var(--app-danger-soft);
  color: var(--ion-color-danger);
}

.settings-entry__body {
  flex: 1;
  min-width: 0;
}

.settings-entry__body strong {
  display: block;
  color: var(--app-heading);
  font-size: 14px;
}

.settings-entry__body p {
  margin: 2px 0 0;
  color: var(--app-muted);
  font-size: 12px;
  line-height: 1.3;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.settings-entry__badge {
  flex-shrink: 0;
  padding: 3px 8px;
  border-radius: 999px;
  background: var(--app-primary-soft);
  color: var(--ion-color-primary);
  font-size: 11px;
  font-weight: 600;
}

.settings-entry__arrow {
  flex-shrink: 0;
  color: var(--app-muted);
  font-size: 16px;
  opacity: 0.4;
}

.settings-entry--danger .settings-entry__body strong {
  color: var(--ion-color-danger);
}

.settings-tools-row {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.settings-tool-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 16px 8px;
  border: 1px solid var(--app-border);
  border-radius: 16px;
  background: var(--app-surface-strong);
  font: inherit;
  transition: background 0.15s ease;
}

.settings-tool-btn:active {
  background: var(--app-primary-soft);
}

.settings-tool-btn ion-icon {
  font-size: 22px;
  color: var(--ion-color-primary);
}

.settings-tool-btn span {
  color: var(--app-heading);
  font-size: 13px;
  font-weight: 500;
}
</style>
