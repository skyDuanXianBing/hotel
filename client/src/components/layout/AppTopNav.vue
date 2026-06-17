<script setup lang="ts">
import { computed } from 'vue'
import {
  ArrowDown,
  HomeFilled,
  Message,
  User,
  Wallet,
} from '@element-plus/icons-vue'
import { useI18n } from 'vue-i18n'
import type { StoreDTO } from '@/api/store'
import LanguageSwitcher from '@/components/LanguageSwitcher.vue'

interface NavItem {
  labelKey: string
  path: string
}

const props = defineProps<{
  stores: StoreDTO[]
  currentStore: StoreDTO | null
  navItems: NavItem[]
  currentPath: string
  displayName: string
  userEmail: string
  userAvatar?: string
  canAccessWallet: boolean
  canAccessOrder: boolean
  chatUnreadCount: number
  formattedChatUnreadCount: string
  inboxUnreadCount: number
  formattedInboxUnreadCount: string
  hasSystemUnread: boolean
  hasOrderUnread: boolean
}>()

const emit = defineEmits<{
  (e: 'store-select', store: StoreDTO): void
  (e: 'manage-stores'): void
  (e: 'menu-click', path: string): void
  (e: 'wallet-click'): void
  (e: 'inbox-click'): void
  (e: 'support-chat'): void
  (e: 'system-notification'): void
  (e: 'order-notification'): void
  (e: 'profile-click'): void
  (e: 'logout'): void
}>()

const { t, locale } = useI18n()

const normalizePath = (path: string) => path.replace(/\/+$/, '') || '/'

const storeName = computed(() => props.currentStore?.name || t('app.shortName'))
const isWideCopyLocale = computed(() => String(locale.value).startsWith('en'))

const handleStoreCommand = (command: StoreDTO | 'manage') => {
  if (command === 'manage') {
    emit('manage-stores')
    return
  }

  emit('store-select', command)
}

const isNavItemActive = (path: string) => {
  const normalizedPath = normalizePath(path)
  const normalizedRoute = normalizePath(props.currentPath)

  if (normalizedPath === '/') {
    return normalizedRoute === '/'
  }

  return normalizedRoute === normalizedPath || normalizedRoute.startsWith(`${normalizedPath}/`)
}

const getStoreRoleBadge = (role?: string) => {
  if (role === 'owner') {
    return 'Pro'
  }
  if (role === 'admin') {
    return 'Adm'
  }
  return 'Ess'
}
</script>

<template>
  <div class="top-nav" :class="{ 'top-nav--wide-copy': isWideCopyLocale }">
    <div class="nav-left">
      <el-dropdown
        class="store-dropdown"
        placement="bottom-start"
        trigger="click"
        @command="handleStoreCommand"
      >
        <button type="button" class="store-trigger">
          <span class="store-icon-shell">
            <el-icon><HomeFilled /></el-icon>
          </span>
          <span class="store-name">{{ storeName }}</span>
          <el-icon class="store-arrow"><ArrowDown /></el-icon>
        </button>

        <template #dropdown>
          <el-dropdown-menu class="store-menu">
            <el-dropdown-item
              v-for="store in stores"
              :key="store.id"
              :command="store"
              :class="{ 'is-active': currentStore?.id === store.id }"
            >
              <div class="store-menu-item">
                <span class="store-menu-name">{{ store.name }}</span>
                <span class="store-menu-badge">{{ getStoreRoleBadge(store.userRole) }}</span>
              </div>
            </el-dropdown-item>
            <el-dropdown-item divided command="manage">
              {{ t('layout.store.manage') }}
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>

    <div class="nav-center">
      <nav class="nav-pill-list" aria-label="Global navigation">
        <button
          v-for="item in navItems"
          :key="item.path"
          type="button"
          class="nav-pill"
          :class="{ 'is-active': isNavItemActive(item.path) }"
          @click="emit('menu-click', item.path)"
        >
          <span class="nav-pill-label">
            {{ t(item.labelKey) }}
            <template v-if="item.path === '/messages' && chatUnreadCount > 0">
              <span class="nav-pill-message-badge-count">{{ formattedChatUnreadCount }}</span>
            </template>
          </span>
        </button>
      </nav>
    </div>

    <div class="nav-right">
      <button
        v-if="canAccessWallet"
        type="button"
        class="action-button"
        :aria-label="t('layout.wallet.tooltip')"
        @click="emit('wallet-click')"
      >
        <el-icon><Wallet /></el-icon>
      </button>

      <el-dropdown placement="bottom-end" trigger="hover">
        <button
          type="button"
          class="action-button action-button--inbox"
          :aria-label="t('layout.inbox.title')"
        >
          <el-icon><Message /></el-icon>
          <span v-if="inboxUnreadCount > 0" class="action-badge">
            {{ formattedInboxUnreadCount }}
          </span>
        </button>

        <template #dropdown>
          <el-dropdown-menu class="message-menu">
            <el-dropdown-item class="message-menu-title-item" @click="emit('inbox-click')">
              <span class="message-menu-title">{{ t('layout.inbox.title') }}</span>
            </el-dropdown-item>
            <el-dropdown-item @click="emit('system-notification')">
              <span class="message-menu-item">
                <span v-if="hasSystemUnread" class="message-menu-indicator"></span>
                <span class="message-menu-label">{{ t('layout.inbox.system') }}</span>
              </span>
            </el-dropdown-item>
            <el-dropdown-item v-if="canAccessOrder" @click="emit('order-notification')">
              <span class="message-menu-item">
                <span v-if="hasOrderUnread" class="message-menu-indicator"></span>
                <span class="message-menu-label">{{ t('layout.inbox.order') }}</span>
              </span>
            </el-dropdown-item>
            <el-dropdown-item divided @click="emit('support-chat')">
              <span class="message-menu-item">
                {{ t('layout.support.customerService') }}
              </span>
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>

      <LanguageSwitcher display="icon" />

      <el-dropdown placement="bottom-end" trigger="click">
        <button type="button" class="profile-card">
          <span class="profile-avatar">
            <img v-if="userAvatar" :src="userAvatar" alt="" />
            <el-icon v-else><User /></el-icon>
          </span>
          <span class="profile-copy">
            <span class="profile-name">{{ displayName }}</span>
            <span class="profile-email">{{ userEmail }}</span>
          </span>
        </button>

        <template #dropdown>
          <el-dropdown-menu class="profile-menu">
            <el-dropdown-item @click="emit('profile-click')">
              {{ t('layout.profile') }}
            </el-dropdown-item>
            <el-dropdown-item divided @click="emit('logout')">
              {{ t('layout.logout') }}
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </div>
</template>

<style scoped>
.top-nav {
  --nav-gap: 18px;
  --nav-left-min-width: 336px;
  --nav-center-safe-width: 500px;
  --nav-right-min-width: 346px;
  --top-nav-min-width: calc(
    var(--nav-left-min-width) + var(--nav-center-safe-width) + var(--nav-right-min-width) +
      (var(--nav-gap) * 2)
  );
  --nav-center-shift: 0px;
  --nav-right-shift: 0px;
  display: grid;
  grid-template-columns:
    minmax(var(--nav-left-min-width), 1fr)
    auto
    minmax(var(--nav-right-min-width), 1fr);
  align-items: center;
  gap: var(--nav-gap);
  min-width: var(--top-nav-min-width);
  width: max(100%, var(--top-nav-min-width));
}

.top-nav--wide-copy {
  --nav-gap: 16px;
  --nav-left-min-width: 336px;
  --nav-center-safe-width: 604px;
  --nav-right-min-width: 346px;
}

.top-nav--wide-copy .nav-pill-list {
  gap: 2px;
}

.top-nav--wide-copy .nav-pill {
  min-width: 40px;
  padding: 0 12px;
}

.top-nav--wide-copy .nav-pill-label {
  font-size: 13px;
}

.nav-left,
.nav-right {
  display: flex;
  align-items: center;
  min-width: 0;
  flex-wrap: nowrap;
}

.nav-center {
  min-width: 0;
  display: flex;
  justify-content: center;
  justify-self: center;
  width: max-content;
  max-width: 100%;
  overflow: hidden;
  transform: translateX(var(--nav-center-shift));
  transition: transform 0.24s ease;
}

.nav-left {
  justify-self: start;
  min-width: var(--nav-left-min-width);
}

.nav-right {
  justify-self: end;
  justify-content: flex-end;
  min-width: var(--nav-right-min-width);
  transform: translateX(var(--nav-right-shift));
  transition: transform 0.24s ease;
}

.store-dropdown {
  display: inline-flex;
  max-width: 100%;
}

.store-trigger {
  max-width: min(100%, 396px);
  height: 44px;
  display: inline-flex;
  align-items: center;
  justify-content: flex-start;
  gap: 10px;
  padding: 0;
  border: none;
  border-radius: 0;
  background: transparent;
  color: #2f7cf6;
  cursor: pointer;
}

.store-icon-shell {
  width: 30px;
  height: 30px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  color: #2f7cf6;
  font-size: 28px;
  flex: 0 0 auto;
}

.store-name {
  flex: 0 1 auto;
  min-width: 0;
  max-width: 338px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 17px;
  font-weight: 700;
  line-height: 1;
}

.store-arrow {
  font-size: 12px;
  color: #a6acb5;
  flex: 0 0 auto;
  margin-left: 2px;
}

.nav-pill-list {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px;
  border-radius: 999px;
  background: #ffffff;
  min-width: max-content;
  white-space: nowrap;
}

.nav-pill {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 44px;
  height: 34px;
  padding: 0 14px;
  border: none;
  border-radius: 999px;
  background: transparent;
  color: #20211d;
  cursor: pointer;
  transition: background-color 0.2s ease, color 0.2s ease;
}

.nav-pill:hover {
  background: #f3f4ef;
}

.nav-pill.is-active {
  background: #121212;
  color: #ffffff;
}

.nav-pill-label {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  font-weight: 600;
  line-height: 1;
}

.action-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 16px;
  height: 16px;
  padding: 0 4px;
  border-radius: 999px;
  background: #ff5a5f;
  color: #ffffff;
  font-size: 10px;
  font-weight: 700;
  line-height: 1;
}

.nav-pill-message-badge-count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 16px;
  height: 16px;
  padding: 0 4px;
  border-radius: 999px;
  background: #ff5a5f;
  color: #ffffff;
  font-size: 10px;
  font-weight: 700;
  line-height: 1;
}

.nav-right {
  gap: 8px;
}

.action-button,
.profile-card {
  border: 1px solid #ededeb;
  background: #ffffff;
}

.action-button {
  position: relative;
  width: 36px;
  height: 36px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  color: #5d6461;
  cursor: pointer;
  transition:
    border-color 0.2s ease,
    background-color 0.2s ease,
    color 0.2s ease;
}

.action-button:hover,
.action-button:focus-visible,
.action-button[aria-expanded='true'] {
  border-color: #121212;
  color: #20211d;
  outline: none;
}

.profile-card {
  transition:
    border-color 0.2s ease,
    background-color 0.2s ease;
}

.profile-card:hover {
  border-color: #dfdfda;
}

.action-button--inbox {
  overflow: visible;
}

.action-badge {
  position: absolute;
  top: -3px;
  right: -2px;
}

.profile-card {
  min-width: 214px;
  max-width: 214px;
  height: 42px;
  display: inline-flex;
  align-items: center;
  justify-content: flex-start;
  gap: 12px;
  padding: 0 14px 0 0;
  border-radius: 999px;
  cursor: pointer;
  overflow: hidden;
  text-align: left;
}

.profile-avatar {
  width: 42px;
  height: 42px;
  border-radius: 999px;
  overflow: hidden;
  background: #d8e67b;
  color: #20211d;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
}

.profile-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.profile-copy {
  flex: 1 1 auto;
  min-width: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: flex-start;
  gap: 2px;
  text-align: left;
}

.profile-name,
.profile-email {
  width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.profile-name {
  font-size: 13px;
  line-height: 1.15;
  color: #1c1d1a;
  font-weight: 600;
}

.profile-email {
  font-size: 11px;
  line-height: 1.1;
  color: #979d97;
}

.store-menu-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  width: 100%;
}

.message-menu-item {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: 8px;
  width: max-content;
}

.message-menu-label {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.store-menu-name {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.store-menu-badge {
  flex: 0 0 auto;
  padding: 2px 6px;
  border-radius: 999px;
  background: #f1f2ec;
  color: #6a716c;
  font-size: 11px;
  line-height: 1;
}

.message-menu-title {
  font-size: 13px;
  font-weight: 600;
  color: #20211d;
}

:deep(.message-menu .message-menu-title-item) {
  padding-top: 8px;
  padding-bottom: 10px;
  border-bottom: 1px solid #efefea;
}

:deep(.message-menu) {
  min-width: 0;
}

:deep(.message-menu .el-dropdown-menu__item) {
  padding-left: 12px;
  padding-right: 12px;
}

.message-menu-indicator {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #ff5a5f;
  flex: 0 0 auto;
}

:deep(.store-menu .el-dropdown-menu__item.is-active) {
  background: #f5f8ff;
  color: #2f7cf6;
}

@media (max-width: 1480px) {
  .top-nav {
    gap: 16px;
  }

  .nav-pill-list {
    overflow: visible;
  }
}
</style>
