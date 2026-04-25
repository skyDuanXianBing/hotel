<template>
  <ion-page>
    <ion-header translucent>
      <ion-toolbar>
        <ion-title class="mobile-toolbar-title">设置</ion-title>
      </ion-toolbar>
    </ion-header>

    <ion-content fullscreen class="mobile-page settings-page">
      <section class="settings-hero">
        <div class="settings-hero__profile">
          <div class="settings-hero__logo-shell" aria-hidden="true">
            <div class="settings-hero__logo-frame">
              <img src="/settings-logo.png" alt="" class="settings-hero__logo" />
            </div>
          </div>

          <div class="settings-hero__body">
            <h1 class="settings-hero__title">{{ storeTitle }}</h1>
            <p class="settings-hero__user">{{ currentUserLabel }}</p>
          </div>
        </div>
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

    <MemoSheetModal :is-open="visibleToolsStore.memoOpen" @dismiss="visibleToolsStore.closeMemo" />

    <RecordTransactionModal
      :is-open="visibleToolsStore.recordOpen"
      @dismiss="visibleToolsStore.closeRecord"
      @success="handleRecordSuccess"
    />

    <ContactSupportModal :is-open="visibleToolsStore.contactOpen" @dismiss="visibleToolsStore.closeContact" />
  </ion-page>
</template>

<script setup lang="ts">
import {
  IonContent,
  IonHeader,
  IonIcon,
  IonPage,
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
import { computed } from 'vue'
import ContactSupportModal from '@/components/global/ContactSupportModal.vue'
import MemoSheetModal from '@/components/global/MemoSheetModal.vue'
import RecordTransactionModal from '@/components/notes/RecordTransactionModal.vue'
import { useRouter } from 'vue-router'
import { ROUTE_PATHS } from '@/router/guards'
import { useStoreStore } from '@/stores/store'
import { useUserStore } from '@/stores/user'
import { useVisibleToolsStore } from '@/stores/visibleTools'
import { showSuccessToast } from '@/utils/notify'

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
          description: '设施、时区、币种',
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

function handleOpenMemoTool() {
  visibleToolsStore.openMemo()
}

function handleOpenRecordTool() {
  visibleToolsStore.openRecord()
}

function handleOpenContactTool() {
  visibleToolsStore.openContact()
}

function handleRecordSuccess() {
  showSuccessToast('已完成记一笔录入')
}

</script>

<style scoped>
.settings-page {
  display: block;
  --background: var(--ios-pms-bg-page);
}

.settings-hero {
  padding: var(--ios-pms-space-5);
  margin-bottom: var(--ios-pms-space-4);
  border: 1px solid rgba(210, 220, 238, 0.78);
  border-radius: var(--ios-pms-radius-card);
  background:
    radial-gradient(circle at top right, rgba(90, 144, 255, 0.08), transparent 42%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(250, 252, 255, 0.96)),
    var(--ios-pms-surface-strong);
  box-shadow:
    0 18px 40px rgba(132, 153, 191, 0.08),
    inset 0 1px 0 rgba(255, 255, 255, 0.78);
}

.settings-hero__profile {
  display: flex;
  align-items: center;
  gap: 18px;
}

.settings-hero__logo-shell {
  flex-shrink: 0;
}

.settings-hero__logo-frame {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 64px;
  height: 64px;
  padding: 1px;
  border: 1px solid rgba(216, 223, 234, 0.95);
  border-radius: 14px;
  background: #fff;
  box-shadow:
    0 8px 18px rgba(150, 165, 196, 0.05),
    inset 0 1px 0 rgba(255, 255, 255, 0.92);
}

.settings-hero__logo {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.settings-hero__body {
  display: flex;
  flex-direction: column;
  justify-content: center;
  min-width: 0;
  min-height: 64px;
}

.settings-hero__title {
  margin: 0;
  color: var(--ios-pms-text-primary);
  font-size: 17px;
  font-weight: var(--ios-pms-weight-heavy);
  line-height: 1.24;
  letter-spacing: -0.03em;
}

.settings-hero__user {
  margin: 6px 0 0;
  color: var(--ios-pms-text-muted);
  font-size: 14px;
  font-weight: 400;
  line-height: 1.24;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.settings-group {
  padding: calc(var(--ios-pms-space-5) - 2px) var(--ios-pms-space-5);
  border: 1px solid rgba(217, 226, 241, 0.82);
  border-radius: var(--ios-pms-radius-card);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(251, 252, 255, 0.96)),
    var(--ios-pms-surface);
  box-shadow: 0 16px 34px rgba(145, 164, 198, 0.07);
}

.settings-group__title {
  margin: 0 0 calc(var(--ios-pms-space-3) + 2px);
  color: var(--ios-pms-text-primary);
  font-size: var(--ios-pms-font-title-md-size);
  font-weight: var(--ios-pms-weight-bold);
  letter-spacing: -0.035em;
}

.settings-group__list {
  display: grid;
  gap: 0;
}

.settings-entry {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto auto;
  align-items: center;
  gap: 12px;
  width: 100%;
  padding: 15px 2px;
  border: none;
  border-radius: 0;
  background: transparent;
  font: inherit;
  text-align: left;
  transition:
    background-color 0.15s ease,
    transform 0.15s ease;
}

.settings-entry + .settings-entry {
  border-top: 1px solid rgba(225, 232, 243, 0.88);
}

.settings-entry:active {
  background: rgba(93, 145, 245, 0.08);
  transform: translateY(1px);
}

.settings-entry__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  width: 42px;
  height: 42px;
  border: 1px solid rgba(225, 232, 243, 0.88);
  border-radius: 14px;
  background: linear-gradient(180deg, rgba(247, 250, 255, 0.98), rgba(239, 244, 253, 0.95));
  color: var(--ios-pms-primary);
  font-size: 18px;
}

.settings-entry__icon--secondary {
  background: linear-gradient(180deg, rgba(247, 248, 255, 0.98), rgba(239, 242, 255, 0.95));
  color: var(--ion-color-secondary);
}

.settings-entry__icon--danger {
  background: linear-gradient(180deg, rgba(255, 248, 248, 0.98), rgba(255, 240, 240, 0.95));
  color: var(--ion-color-danger);
}

.settings-entry__body {
  flex: 1;
  min-width: 0;
}

.settings-entry__body strong {
  display: block;
  color: var(--ios-pms-text-primary);
  font-size: var(--ios-pms-font-title-sm-size);
  font-weight: var(--ios-pms-weight-bold);
  line-height: 1.25;
  letter-spacing: -0.025em;
}

.settings-entry__body p {
  margin: 4px 0 0;
  color: var(--ios-pms-text-secondary);
  font-size: var(--ios-pms-font-body-sm-size);
  line-height: 1.4;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.settings-entry__badge {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  min-height: 30px;
  padding: 0 11px;
  border: 1px solid rgba(221, 229, 242, 0.92);
  border-radius: var(--ios-pms-radius-pill);
  background: rgba(246, 248, 253, 0.96);
  color: var(--ios-pms-primary-strong);
  font-size: var(--ios-pms-font-body-sm-size);
  font-weight: var(--ios-pms-weight-bold);
}

.settings-entry__arrow {
  flex-shrink: 0;
  color: #b2bfd5;
  font-size: 17px;
  opacity: 1;
}

.settings-entry--danger .settings-entry__body strong {
  color: var(--ion-color-danger);
}

.settings-tools-row {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--ios-pms-space-3);
}

.settings-tool-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--ios-pms-space-2);
  padding: 15px 8px 13px;
  border: 1px solid rgba(223, 230, 242, 0.88);
  border-radius: 18px;
  background:
    linear-gradient(180deg, rgba(248, 250, 255, 0.98), rgba(243, 247, 255, 0.94)),
    var(--ios-pms-surface-strong);
  font: inherit;
  transition:
    background-color 0.15s ease,
    border-color 0.15s ease,
    transform 0.15s ease;
}

.settings-tool-btn:active {
  background: rgba(93, 145, 245, 0.08);
  border-color: rgba(179, 197, 232, 0.96);
  transform: translateY(1px);
}

.settings-tool-btn ion-icon {
  font-size: 21px;
  color: var(--ios-pms-primary);
}

.settings-tool-btn span {
  color: var(--ios-pms-text-primary);
  font-size: 13px;
  font-weight: var(--ios-pms-weight-medium);
}

@media (max-width: 374px) {
  .settings-hero {
    padding: var(--ios-pms-space-4);
  }

  .settings-hero__logo-frame {
    width: 60px;
    height: 60px;
    padding: 1px;
    border-radius: 12px;
  }

  .settings-hero__body {
    min-height: 60px;
  }

  .settings-group {
    padding: var(--ios-pms-space-4);
  }

  .settings-tools-row {
    grid-template-columns: 1fr;
  }
}
</style>
