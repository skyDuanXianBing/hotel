<template>
  <ion-modal :is-open="isOpen" @didDismiss="handleDismiss">
    <ion-header>
      <ion-toolbar>
        <ion-title>联系客服</ion-title>
        <ion-buttons slot="end">
          <ion-button @click="handleDismiss">关闭</ion-button>
        </ion-buttons>
      </ion-toolbar>
    </ion-header>

    <ion-content class="mobile-page support-modal-page">
      <div class="mobile-stack">
        <section class="support-card">
          <article class="support-item">
            <div class="support-item__icon">📧</div>
            <div class="support-item__body">
              <strong>客服邮箱</strong>
              <p>{{ supportContact.email }}</p>
            </div>
            <div class="support-item__actions">
              <button type="button" class="support-btn" @click="handleCopyEmail">复制</button>
              <button type="button" class="support-btn support-btn--primary" @click="handleSendEmail">发邮件</button>
            </div>
          </article>

          <article class="support-item">
            <div class="support-item__icon">📞</div>
            <div class="support-item__body">
              <strong>客服电话</strong>
              <p>{{ supportContact.phone }}</p>
              <span class="support-item__hours">服务时间: {{ supportContact.serviceHours }}</span>
            </div>
            <div class="support-item__actions">
              <button type="button" class="support-btn" @click="handleCopyPhone">复制</button>
              <button type="button" class="support-btn support-btn--primary" @click="handleCallPhone">拨打</button>
            </div>
          </article>
        </section>
      </div>
    </ion-content>
  </ion-modal>
</template>

<script setup lang="ts">
import {
  IonButton,
  IonButtons,
  IonContent,
  IonHeader,
  IonModal,
  IonTitle,
  IonToolbar,
} from '@ionic/vue'
import { supportContact } from '@/constants/support'
import { copyText } from '@/utils/clipboard'
import { showSuccessToast, showWarningToast } from '@/utils/notify'

interface Props {
  isOpen: boolean
}

defineProps<Props>()

const emit = defineEmits<{
  dismiss: []
}>()

const handleDismiss = () => {
  emit('dismiss')
}

const handleCopyEmail = async () => {
  const copied = await copyText(supportContact.email)
  if (copied) {
    showSuccessToast('邮箱已复制')
    return
  }

  showWarningToast('当前环境暂不支持复制邮箱')
}

const handleCopyPhone = async () => {
  const copied = await copyText(supportContact.phone)
  if (copied) {
    showSuccessToast('电话已复制')
    return
  }

  showWarningToast('当前环境暂不支持复制电话')
}

const handleSendEmail = () => {
  if (typeof window === 'undefined') {
    return
  }

  window.location.href = `mailto:${supportContact.email}`
}

const handleCallPhone = () => {
  if (typeof window === 'undefined') {
    return
  }

  const phoneLink = supportContact.phone.replace(/\s+/g, '')
  window.location.href = `tel:${phoneLink}`
}
</script>

<style scoped>
.support-modal-page {
  --padding-top: 16px;
  --padding-bottom: 24px;
  --padding-start: 16px;
  --padding-end: 16px;
}

.support-card {
  display: grid;
  gap: 10px;
}

.support-item {
  display: grid;
  grid-template-columns: 40px 1fr auto;
  align-items: center;
  gap: 12px;
  padding: 16px;
  border-radius: 16px;
  border: 1px solid var(--app-border);
  background: var(--app-surface-strong);
}

.support-item__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: 12px;
  background: var(--app-primary-soft);
  font-size: 18px;
}

.support-item__body strong {
  display: block;
  color: var(--app-heading);
  font-size: 14px;
}

.support-item__body p {
  margin: 3px 0 0;
  color: var(--app-heading);
  font-size: 15px;
  font-weight: 600;
}

.support-item__hours {
  display: block;
  margin-top: 2px;
  color: var(--app-muted);
  font-size: 11px;
}

.support-item__actions {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.support-btn {
  padding: 8px 14px;
  border: 1px solid var(--app-border);
  border-radius: 10px;
  background: transparent;
  color: var(--app-heading);
  font: inherit;
  font-size: 13px;
  font-weight: 600;
  white-space: nowrap;
}

.support-btn--primary {
  border-color: rgba(var(--ion-color-primary-rgb), 0.2);
  background: var(--app-primary-soft);
  color: var(--ion-color-primary);
}

.support-btn:active {
  background: var(--app-primary-soft);
}
</style>
