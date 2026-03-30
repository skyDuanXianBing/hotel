<template>
  <ion-page>
    <ion-header translucent>
      <ion-toolbar>
        <ion-buttons slot="start">
          <ion-back-button :default-href="ROUTE_PATHS.home" />
        </ion-buttons>
        <ion-title>消息</ion-title>
      </ion-toolbar>
    </ion-header>

    <ion-content fullscreen class="mobile-page messages-page">
      <ion-refresher slot="fixed" @ionRefresh="handleRefresh">
        <ion-refresher-content pulling-text="下拉刷新消息" refreshing-spinner="crescent" />
      </ion-refresher>

      <section class="mobile-hero messages-hero">
        <p class="mobile-note messages-hero__eyebrow">收件箱迁移</p>
        <h1 class="mobile-title">住客会话</h1>
        <p class="mobile-subtitle">移动端保留会话搜索、未读提醒与详情跳转，列表和会话详情拆为两级导航。</p>
        <div class="mobile-chip-row">
          <span class="mobile-chip">会话 {{ threads.length }}</span>
          <span class="mobile-chip">未读 {{ unreadThreadCount }}</span>
          <span class="mobile-chip">支持 AI 草稿</span>
        </div>
      </section>

      <div class="mobile-stack">
        <section class="mobile-card messages-toolbar-card">
          <ion-searchbar
            v-model="searchKeyword"
            :debounce="0"
            placeholder="搜索住客、渠道、订单号或会话号"
          />

          <div class="messages-toolbar-card__actions">
            <ion-button fill="outline" size="small" @click="loadThreads">刷新列表</ion-button>
          </div>

          <p v-if="loadNotice" class="mobile-note messages-toolbar-card__notice">{{ loadNotice }}</p>
        </section>

        <section class="mobile-card">
          <div class="mobile-inline-row">
            <div>
              <h2 class="mobile-section-title">会话列表</h2>
              <p class="mobile-note">点击会话进入详情页继续沟通。</p>
            </div>
            <ion-spinner v-if="loading" name="crescent" />
          </div>

          <div v-if="filteredThreads.length > 0" class="mobile-list messages-list">
            <article
              v-for="thread in filteredThreads"
              :key="thread.id"
              class="message-thread-card"
              @click="handleOpenThread(thread.id)"
            >
              <div class="message-thread-card__header">
                <div>
                  <strong>{{ formatThreadTitle(thread) }}</strong>
                  <p>{{ thread.channelName }} · 订单 {{ thread.bookingId || thread.threadId || '-' }}</p>
                </div>
                <div class="message-thread-card__meta">
                  <span v-if="thread.unreadCount > 0" class="message-thread-card__badge">{{ thread.unreadCount }}</span>
                  <span class="mobile-note">{{ formatRelativeTime(thread.lastActivity) }}</span>
                </div>
              </div>

              <div class="message-thread-card__footer">
                <span :class="thread.closed ? 'status-pill is-closed' : 'status-pill is-open'">
                  {{ thread.closed ? '已关闭' : '活跃中' }}
                </span>
                <span class="mobile-note message-thread-card__preview">
                  {{ thread.lastMessage || '暂无最新消息内容' }}
                </span>
              </div>
            </article>
          </div>

          <div v-else-if="!loading" class="messages-empty-state">
            <h3>{{ searchKeyword.trim() ? '没有匹配的会话' : '当前暂无会话' }}</h3>
            <p class="mobile-note">{{ searchKeyword.trim() ? '可以尝试更换关键词后重新搜索。' : '新会话出现后会自动在这里展示。' }}</p>
            <ion-button fill="outline" size="small" @click="loadThreads">重新加载</ion-button>
          </div>
        </section>
      </div>
    </ion-content>
  </ion-page>
</template>

<script setup lang="ts">
import {
  IonBackButton,
  IonButton,
  IonButtons,
  IonContent,
  IonHeader,
  IonPage,
  IonRefresher,
  IonRefresherContent,
  IonSearchbar,
  IonSpinner,
  IonTitle,
  IonToolbar,
  onIonViewWillEnter,
} from '@ionic/vue'
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getMessageThreads } from '@/api/message'
import { buildMessageDetailPath, ROUTE_PATHS } from '@/router/guards'
import type { MessageThreadDTO } from '@/types/message'
import { isHandledRequestError } from '@/utils/request'
import { showWarningToast } from '@/utils/notify'

const router = useRouter()

const loading = ref(false)
const searchKeyword = ref('')
const loadNotice = ref('')
const threads = ref<MessageThreadDTO[]>([])

const unreadThreadCount = computed(() => {
  let count = 0

  for (const thread of threads.value) {
    if (thread.unreadCount > 0) {
      count += 1
    }
  }

  return count
})

const filteredThreads = computed(() => {
  const keyword = searchKeyword.value.trim().toLowerCase()
  if (!keyword) {
    return threads.value
  }

  const nextItems: MessageThreadDTO[] = []

  for (const thread of threads.value) {
    const title = formatThreadTitle(thread).toLowerCase()
    const bookingId = (thread.bookingId || '').toLowerCase()
    const threadCode = (thread.threadId || '').toLowerCase()
    const channelName = (thread.channelName || '').toLowerCase()

    if (
      title.includes(keyword) ||
      bookingId.includes(keyword) ||
      threadCode.includes(keyword) ||
      channelName.includes(keyword)
    ) {
      nextItems.push(thread)
    }
  }

  return nextItems
})

function resolveWarningMessage(error: unknown, fallbackMessage: string) {
  if (error instanceof Error && error.message) {
    return error.message
  }

  return fallbackMessage
}

function formatThreadTitle(thread: MessageThreadDTO) {
  if (thread.guestName) {
    return thread.guestName
  }

  if (thread.listingName) {
    return thread.listingName
  }

  return thread.channelName || '未命名会话'
}

function formatRelativeTime(value: string) {
  const date = new Date(value)
  const time = date.getTime()
  if (Number.isNaN(time)) {
    return '时间未知'
  }

  const now = Date.now()
  const diff = now - time
  const oneMinute = 60 * 1000
  const oneHour = 60 * oneMinute
  const oneDay = 24 * oneHour

  if (diff < oneHour) {
    const minutes = Math.max(1, Math.floor(diff / oneMinute))
    return `${minutes} 分钟前`
  }

  if (diff < oneDay) {
    const hours = Math.max(1, Math.floor(diff / oneHour))
    return `${hours} 小时前`
  }

  if (diff < oneDay * 2) {
    return '昨天'
  }

  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${month}-${day}`
}

async function loadThreads() {
  loading.value = true
  loadNotice.value = ''

  try {
    const response = await getMessageThreads()
    if (!response.success || !response.data) {
      throw new Error(response.message || '加载会话失败')
    }

    threads.value = response.data
  } catch (error) {
    loadNotice.value = resolveWarningMessage(error, '加载会话失败')
    if (!isHandledRequestError(error)) {
      showWarningToast(loadNotice.value)
    }
  } finally {
    loading.value = false
  }
}

async function handleOpenThread(threadId: number) {
  await router.push(buildMessageDetailPath(threadId))
}

async function handleRefresh(event: CustomEvent) {
  try {
    await loadThreads()
  } finally {
    event.detail.complete()
  }
}

onIonViewWillEnter(async () => {
  await loadThreads()
})
</script>

<style scoped>
.messages-page {
  display: block;
}

.messages-hero {
  margin-top: 4px;
}

.messages-hero__eyebrow {
  color: var(--ion-color-primary);
  font-weight: 700;
}

.messages-toolbar-card {
  display: grid;
  gap: 10px;
}

.messages-toolbar-card__actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.messages-toolbar-card__notice {
  color: var(--ion-color-warning);
}

.messages-list {
  margin-top: 16px;
}

.message-thread-card {
  padding: 14px;
  border-radius: 20px;
  border: 1px solid var(--app-border);
  background: rgba(255, 255, 255, 0.84);
}

.message-thread-card__header,
.message-thread-card__footer {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.message-thread-card__header strong,
.message-thread-card__header p,
.message-thread-card__footer span {
  margin: 0;
}

.message-thread-card__header p {
  margin-top: 6px;
  color: var(--app-muted);
  font-size: 13px;
}

.message-thread-card__meta {
  display: grid;
  justify-items: end;
  gap: 8px;
}

.message-thread-card__badge {
  min-width: 22px;
  padding: 4px 7px;
  border-radius: 999px;
  background: var(--ion-color-danger);
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  text-align: center;
}

.message-thread-card__footer {
  align-items: center;
  margin-top: 14px;
}

.message-thread-card__preview {
  max-width: 62%;
  text-align: right;
}

.status-pill {
  padding: 6px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
}

.status-pill.is-open {
  background: rgba(15, 159, 110, 0.12);
  color: var(--ion-color-success);
}

.status-pill.is-closed {
  background: rgba(100, 116, 139, 0.14);
  color: var(--app-muted);
}

.messages-empty-state {
  display: grid;
  gap: 10px;
  justify-items: flex-start;
  padding-top: 18px;
}

.messages-empty-state h3,
.messages-empty-state p {
  margin: 0;
}
</style>
