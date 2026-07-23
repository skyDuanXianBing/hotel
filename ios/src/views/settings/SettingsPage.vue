<template>
  <ion-page>
    <ion-header translucent>
      <ion-toolbar class="app-page-header__toolbar">
        <ion-buttons slot="start">
          <ion-back-button class="app-page-header__back-btn" :default-href="ROUTE_PATHS.home" />
        </ion-buttons>
        <ion-title class="app-page-header__title">{{ t('settings.title') }}</ion-title>
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
          <h2 class="settings-group__title">{{ t('settings.groups.account') }}</h2>
          <div class="settings-group__list">
            <button type="button" class="settings-entry" @click="handleChangeLanguage">
              <div class="settings-entry__icon">
                <ion-icon :icon="languageOutline" />
              </div>
              <div class="settings-entry__body">
                <strong>{{ t('language.title') }}</strong>
                <p>{{ t(`language.option.${languageStore.locale}`) }}</p>
              </div>
              <ion-icon :icon="chevronForwardOutline" class="settings-entry__arrow" />
            </button>
            <button type="button" class="settings-entry" @click="handleOpenProfile">
              <div class="settings-entry__icon settings-entry__icon--secondary">
                <ion-icon :icon="personOutline" />
              </div>
              <div class="settings-entry__body">
                <strong>{{ t('settings.profile') }}</strong>
                <p>{{ currentUserLabel }}</p>
              </div>
              <ion-icon :icon="chevronForwardOutline" class="settings-entry__arrow" />
            </button>
            <button type="button" class="settings-entry settings-entry--danger" @click="handleLogout">
              <div class="settings-entry__icon settings-entry__icon--danger">
                <ion-icon :icon="logOutOutline" />
              </div>
              <div class="settings-entry__body">
                <strong>{{ t('settings.logout') }}</strong>
              </div>
            </button>
          </div>
        </section>

        <section class="settings-group">
          <h2 class="settings-group__title">{{ t('settings.groups.tools') }}</h2>
          <div class="settings-tools-row">
            <button type="button" class="settings-tool-btn" @click="handleOpenMemoTool">
              <ion-icon :icon="documentTextOutline" />
              <span>{{ t('settings.memo') }}</span>
            </button>
            <button type="button" class="settings-tool-btn" @click="handleOpenRecordTool">
              <ion-icon :icon="createOutline" />
              <span>{{ t('settings.record') }}</span>
            </button>
            <button type="button" class="settings-tool-btn" @click="handleOpenContactTool">
              <ion-icon :icon="headsetOutline" />
              <span>{{ t('settings.support') }}</span>
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
  actionSheetController,
  alertController,
  IonBackButton,
  IonButtons,
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
  languageOutline,
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
import { useI18n } from 'vue-i18n'
import ContactSupportModal from '@/components/global/ContactSupportModal.vue'
import MemoSheetModal from '@/components/global/MemoSheetModal.vue'
import RecordTransactionModal from '@/components/notes/RecordTransactionModal.vue'
import { useRouter } from 'vue-router'
import { ROUTE_PATHS } from '@/router/guards'
import { SUPPORTED_LOCALES, type SupportedLocale } from '@/locales'
import { useLanguageStore } from '@/stores/language'
import { useStoreStore } from '@/stores/store'
import { useUserStore } from '@/stores/user'
import { useVisibleToolsStore } from '@/stores/visibleTools'
import { showSuccessToast } from '@/utils/notify'

type SettingsEntryKey =
  | 'storeProfile'
  | 'storeDetails'
  | 'storeMembers'
  | 'roomTypes'
  | 'roomGroups'
  | 'roomSort'
  | 'pricePlans'
  | 'consumptionItems'
  | 'paymentMethods'
  | 'noteSettings'
  | 'notification'
  | 'channelSettings'
  | 'quickReplies'
  | 'autoMessages'
  | 'cleaningSettings'
  | 'cleaningSupplies'
  | 'pricingTools'
  | 'paymentPlatforms'

interface SettingsEntry {
  key: SettingsEntryKey
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
const { t } = useI18n()
const languageStore = useLanguageStore()
const storeStore = useStoreStore()
const userStore = useUserStore()
const visibleToolsStore = useVisibleToolsStore()

const currentUserLabel = computed(() => {
  if (!userStore.currentUser) {
    return t('common.unavailableUser')
  }

  return `${userStore.currentUser.nickname} · ${userStore.currentUser.email}`
})

const storeTitle = computed(() => {
  if (!storeStore.currentStore?.name) {
    return t('common.currentStore')
  }

  return storeStore.currentStore.name
})

const storeRoleLabel = computed(() => {
  const role = storeStore.currentStore?.userRole
  if (role === 'owner') {
    return t('settings.role.owner')
  }
  if (role === 'admin') {
    return t('settings.role.admin')
  }
  if (role === 'member') {
    return t('settings.role.member')
  }
  return t('settings.role.unknown')
})

const entryGroups = computed<SettingsGroup[]>(() => {
  const entry = (
    key: SettingsEntryKey,
    path: string,
    icon: string,
    badge?: string,
  ): SettingsEntry => ({
    key,
    title: t(`settings.entries.${key}.0`),
    description: t(`settings.entries.${key}.1`),
    path,
    icon,
    badge,
  })

  return [
    {
      key: 'store-account',
      title: t('settings.groups.storeAccount'),
      description: '',
      items: [
        entry('storeProfile', ROUTE_PATHS.settingsStoreProfile, storefrontOutline, storeStore.currentStore?.city || ''),
        entry('storeDetails', ROUTE_PATHS.settingsStoreDetails, buildOutline),
        entry('storeMembers', ROUTE_PATHS.settingsStoreMembers, peopleOutline),
      ],
    },
    {
      key: 'accommodation',
      title: t('settings.groups.accommodation'),
      description: '',
      items: [
        entry('roomTypes', ROUTE_PATHS.settingsRoomTypes, bedOutline),
        entry('roomGroups', ROUTE_PATHS.settingsRoomGroups, buildOutline),
        entry('roomSort', ROUTE_PATHS.settingsRoomSort, swapHorizontalOutline),
        entry('pricePlans', ROUTE_PATHS.settingsPricePlans, pricetagOutline),
        entry('consumptionItems', ROUTE_PATHS.settingsConsumptionItems, receiptOutline),
      ],
    },
    {
      key: 'finance',
      title: t('settings.groups.finance'),
      description: '',
      items: [
        entry('paymentMethods', ROUTE_PATHS.settingsPaymentMethods, walletOutline),
        entry('noteSettings', ROUTE_PATHS.settingsNoteSettings, cashOutline),
      ],
    },
    {
      key: 'general',
      title: t('settings.groups.general'),
      description: '',
      items: [
        entry('notification', ROUTE_PATHS.settingsNotification, notificationsOutline),
        entry('channelSettings', ROUTE_PATHS.settingsChannelSettings, colorWandOutline),
        entry('quickReplies', ROUTE_PATHS.settingsQuickReplies, chatboxEllipsesOutline),
        entry('autoMessages', ROUTE_PATHS.settingsAutoMessages, settingsOutline),
      ],
    },
    {
      key: 'cleaning',
      title: t('settings.groups.cleaning'),
      description: '',
      items: [
        entry('cleaningSettings', ROUTE_PATHS.settingsCleaningSettings, constructOutline),
        entry('cleaningSupplies', ROUTE_PATHS.settingsCleaningSupplies, documentTextOutline),
      ],
    },
    {
      key: 'third-party',
      title: t('settings.groups.integrations'),
      description: '',
      items: [
        entry('pricingTools', ROUTE_PATHS.settingsPricingTools, pricetagOutline),
        entry('paymentPlatforms', ROUTE_PATHS.settingsPaymentPlatforms, walletOutline),
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

async function confirmLogout() {
  const alert = await alertController.create({
    header: t('settings.logout'),
    message: t('settings.logoutConfirm'),
    buttons: [
      {
        text: t('common.cancel'),
        role: 'cancel',
      },
      {
        text: t('common.confirm'),
        role: 'destructive',
      },
    ],
  })

  await alert.present()
  const result = await alert.onDidDismiss()
  return result.role === 'destructive'
}

async function handleChangeLanguage() {
  const actionSheet = await actionSheetController.create({
    header: t('language.title'),
    subHeader: t('language.description'),
    buttons: [
      ...SUPPORTED_LOCALES.map((locale) => ({
        text: t(`language.option.${locale}`),
        role: languageStore.locale === locale ? 'selected' : undefined,
        handler: async () => {
          const changed = await languageStore.setLocale(locale as SupportedLocale)
          if (changed) {
            showSuccessToast(t('language.changed'))
          }
        },
      })),
      {
        text: t('common.cancel'),
        role: 'cancel',
      },
    ],
  })

  await actionSheet.present()
}

async function handleLogout() {
  const confirmed = await confirmLogout()
  if (!confirmed) {
    return
  }

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
  showSuccessToast(t('settings.recordSuccess'))
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
  white-space: normal;
  overflow-wrap: anywhere;
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
  white-space: normal;
  overflow-wrap: anywhere;
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
