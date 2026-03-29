<template>
  <ion-page>
    <ion-header translucent>
      <ion-toolbar>
        <ion-buttons slot="start">
          <ion-back-button :default-href="ROUTE_PATHS.reviews" />
        </ion-buttons>
        <ion-title class="mobile-toolbar-title">链接列表</ion-title>
      </ion-toolbar>
    </ion-header>

    <ion-content fullscreen class="mobile-page registration-review-links-page">
      <section class="mobile-hero registration-review-links-page__hero">
        <p class="mobile-note registration-review-links-page__eyebrow">移动端替代视图</p>
        <h1 class="mobile-title">登记链接列表</h1>
        <p class="mobile-subtitle">查看待发送或可复用的登记链接，并在手机上快速复制继续处理。</p>
      </section>

      <div class="mobile-stack">
        <section class="mobile-card">
          <div class="mobile-inline-row">
            <div>
              <h2 class="mobile-section-title">链接记录</h2>
              <p class="mobile-note">共 {{ reviewStore.linkEntries.length }} 条，按创建时间展示，方便随时复制发送。</p>
            </div>
            <ion-button fill="outline" size="small" @click="handleReload">刷新</ion-button>
          </div>

          <div v-if="reviewStore.isLinkLoading" class="registration-review-links-page__state-block">
            <ion-spinner name="crescent" />
            <p class="mobile-note">正在加载链接列表...</p>
          </div>

          <div v-else-if="reviewStore.linkLoadError" class="registration-review-links-page__state-block">
            <p class="mobile-note">{{ reviewStore.linkLoadError }}</p>
            <ion-button fill="outline" size="small" @click="handleReload">重新加载</ion-button>
          </div>

          <div v-else-if="reviewStore.linkEntries.length > 0" class="mobile-list registration-review-links-page__list">
            <article v-for="entry in reviewStore.linkEntries" :key="entry.id" class="registration-review-links-page__item">
              <div class="mobile-inline-row registration-review-links-page__item-header">
                <div>
                  <strong>{{ entry.guestName }}</strong>
                  <p class="mobile-note">创建时间：{{ entry.createdAt }}</p>
                </div>
                <span class="mobile-chip">{{ entry.roomCount }} 间</span>
              </div>

              <div class="registration-review-links-page__meta-grid">
                <span>入住 {{ entry.checkInDate }}</span>
                <span>离店 {{ entry.checkOutDate }}</span>
                <span>BookingKey {{ entry.bookingKey }}</span>
              </div>

              <div class="registration-review-links-page__link-box">
                {{ entry.linkUrl || '暂无可复制链接' }}
              </div>

              <div class="registration-review-links-page__actions">
                <ion-button fill="outline" size="small" :disabled="!entry.linkUrl" @click="handleCopy(entry.linkUrl)">
                  复制链接
                </ion-button>
              </div>
            </article>
          </div>

          <p v-else class="mobile-note">当前没有可展示的登记链接。</p>
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
  IonSpinner,
  IonTitle,
  IonToolbar,
  onIonViewWillEnter,
} from '@ionic/vue'
import { useReviewStore } from '@/stores/reviews'
import { ROUTE_PATHS } from '@/router/guards'
import { showSuccessToast } from '@/utils/notify'
import { showUnhandledRequestWarning } from '@/utils/requestError'
import { copyTextToClipboard } from '@/utils/file'

const reviewStore = useReviewStore()

onIonViewWillEnter(async () => {
  await reviewStore.refreshLinks()
})

async function handleReload() {
  await reviewStore.refreshLinks()
}

async function handleCopy(linkUrl: string) {
  try {
    await copyTextToClipboard(linkUrl)
    showSuccessToast('链接已复制')
  } catch (error) {
    showUnhandledRequestWarning(error, '复制链接失败')
  }
}
</script>

<style scoped>
.registration-review-links-page__eyebrow {
  color: var(--ion-color-primary);
  font-weight: 700;
}

.registration-review-links-page__state-block {
  display: grid;
  justify-items: center;
  gap: 10px;
  padding: 24px 0;
}

.registration-review-links-page__item {
  padding: 14px;
  border-radius: 18px;
  border: 1px solid var(--app-border);
  background: rgba(255, 255, 255, 0.86);
}

.registration-review-links-page__item-header {
  align-items: flex-start;
}

.registration-review-links-page__meta-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  margin-top: 12px;
  color: var(--app-muted);
  font-size: 12px;
}

.registration-review-links-page__link-box {
  margin-top: 12px;
  padding: 12px;
  border-radius: 14px;
  background: rgba(16, 35, 63, 0.04);
  color: var(--app-muted);
  font-size: 13px;
  line-height: 1.6;
  word-break: break-all;
}

.registration-review-links-page__actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}

@media (max-width: 360px) {
  .registration-review-links-page__meta-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
