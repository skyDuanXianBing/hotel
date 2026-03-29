<template>
  <ion-page>
    <ion-header translucent>
      <ion-toolbar>
        <ion-buttons slot="start">
          <ion-back-button :default-href="ROUTE_PATHS.settings" />
        </ion-buttons>
        <ion-title>快捷回复</ion-title>
        <ion-buttons slot="end">
          <ion-button @click="handleCreateReply">新增</ion-button>
        </ion-buttons>
      </ion-toolbar>
    </ion-header>

    <ion-content fullscreen class="mobile-page settings-quick-replies-page">
      <ion-refresher slot="fixed" @ionRefresh="handleRefresh">
        <ion-refresher-content pulling-text="下拉刷新快捷回复" refreshing-spinner="crescent" />
      </ion-refresher>

      <section class="mobile-hero settings-quick-replies-hero">
        <p class="mobile-note settings-quick-replies-hero__eyebrow">沟通与自动化</p>
        <h1 class="mobile-title">快捷回复</h1>
        <p class="mobile-subtitle">保留回复模板管理与变量插入入口，方便在移动端快速维护话术。</p>
        <div class="mobile-chip-row">
          <span class="mobile-chip">模板 {{ replies.length }}</span>
          <span class="mobile-chip">支持变量插入</span>
        </div>
      </section>

      <div class="mobile-stack">
        <section class="mobile-card">
          <div class="mobile-inline-row settings-quick-replies-page__section-header">
            <div>
              <h2 class="mobile-section-title">模板列表</h2>
              <p class="mobile-note">支持新增、编辑、删除和消息内容维护。</p>
            </div>
            <ion-spinner v-if="loading" name="crescent" />
          </div>

          <div v-if="replies.length > 0" class="mobile-list settings-quick-replies-list">
            <article v-for="reply in replies" :key="reply.id" class="settings-quick-reply-card">
              <div>
                <strong>{{ reply.title }}</strong>
                <p>{{ reply.message }}</p>
              </div>

              <div class="settings-quick-reply-card__actions">
                <ion-button size="small" fill="outline" @click="handleEditReply(reply)">编辑</ion-button>
                <ion-button size="small" color="danger" fill="clear" @click="handleDeleteReply(reply)">
                  删除
                </ion-button>
              </div>
            </article>
          </div>

          <p v-else-if="!loading" class="mobile-note settings-quick-replies-page__empty-state">当前暂无快捷回复。</p>
        </section>
      </div>

      <ion-modal :is-open="editorOpen" @didDismiss="handleDismissEditor">
        <ion-header>
          <ion-toolbar>
            <ion-title>{{ editingReplyId ? '编辑快捷回复' : '新增快捷回复' }}</ion-title>
            <ion-buttons slot="end">
              <ion-button @click="handleDismissEditor">关闭</ion-button>
            </ion-buttons>
          </ion-toolbar>
        </ion-header>

        <ion-content class="mobile-page settings-modal-page">
          <section class="mobile-card">
            <div class="settings-form-grid">
              <label class="settings-form-field">
                <span>标题</span>
                <ion-input v-model="replyForm.title" fill="outline" placeholder="请输入标题" />
              </label>

              <label class="settings-form-field settings-form-field--full">
                <span>消息内容</span>
                <ion-textarea v-model="replyForm.message" :rows="7" fill="outline" placeholder="请输入回复内容" />
              </label>
            </div>

            <div class="settings-variable-panel">
              <h3>插入变量</h3>
              <p class="mobile-note">点击变量后会追加到消息尾部，首版不复刻桌面光标定位增强。</p>
              <div class="settings-variable-panel__list">
                <button
                  v-for="variable in messageVariables"
                  :key="variable.code"
                  type="button"
                  class="settings-variable-chip"
                  @click="handleInsertVariable(variable.code)"
                >
                  {{ variable.label }}
                </button>
              </div>
            </div>

            <div class="settings-form-actions">
              <ion-button fill="outline" @click="handleDismissEditor">取消</ion-button>
              <ion-button :disabled="submitting" @click="handleSaveReply">
                {{ submitting ? '提交中...' : '保存快捷回复' }}
              </ion-button>
            </div>
          </section>
        </ion-content>
      </ion-modal>
    </ion-content>
  </ion-page>
</template>

<script setup lang="ts">
import {
  alertController,
  IonBackButton,
  IonButton,
  IonButtons,
  IonContent,
  IonHeader,
  IonInput,
  IonModal,
  IonPage,
  IonRefresher,
  IonRefresherContent,
  IonSpinner,
  IonTextarea,
  IonTitle,
  IonToolbar,
  onIonViewWillEnter,
} from '@ionic/vue'
import { ref } from 'vue'
import {
  createQuickReply,
  deleteQuickReply,
  getAllQuickReplies,
  updateQuickReply,
  type QuickReplyDTO,
} from '@/api/quickReply'
import { ROUTE_PATHS } from '@/router/guards'
import { showSuccessToast, showWarningToast } from '@/utils/notify'
import { isHandledRequestError } from '@/utils/request'

interface QuickReplyFormState {
  title: string
  message: string
}

const messageVariables = [
  { label: 'Property name', code: '{{property_name}}' },
  { label: "Guest's name", code: '{{guest_name}}' },
  { label: 'Check-in date', code: '{{checkin_date}}' },
  { label: 'Checkout date', code: '{{checkout_date}}' },
  { label: 'Room type name', code: '{{room_type_name}}' },
  { label: 'Rate plan name', code: '{{rate_plan_name}}' },
]

const loading = ref(false)
const submitting = ref(false)
const replies = ref<QuickReplyDTO[]>([])
const editorOpen = ref(false)
const editingReplyId = ref<number | null>(null)
const replyForm = ref<QuickReplyFormState>(createEmptyReplyForm())

function createEmptyReplyForm(): QuickReplyFormState {
  return {
    title: '',
    message: '',
  }
}

function resolveWarningMessage(error: unknown, fallbackMessage: string) {
  if (error instanceof Error && error.message) {
    return error.message
  }
  return fallbackMessage
}

async function confirmDelete(title: string) {
  const alert = await alertController.create({
    header: '删除快捷回复',
    message: `确认删除 ${title} 吗？`,
    buttons: [
      {
        text: '取消',
        role: 'cancel',
      },
      {
        text: '确认删除',
        role: 'destructive',
      },
    ],
  })

  await alert.present()
  const result = await alert.onDidDismiss()
  return result.role === 'destructive'
}

async function loadReplies() {
  loading.value = true
  try {
    const response = await getAllQuickReplies()
    if (!response.success || !response.data) {
      throw new Error(response.message || '加载快捷回复失败')
    }
    replies.value = response.data
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, '加载快捷回复失败'))
    }
  } finally {
    loading.value = false
  }
}

function handleCreateReply() {
  editingReplyId.value = null
  replyForm.value = createEmptyReplyForm()
  editorOpen.value = true
}

function handleEditReply(reply: QuickReplyDTO) {
  editingReplyId.value = reply.id
  replyForm.value = {
    title: reply.title,
    message: reply.message,
  }
  editorOpen.value = true
}

function handleDismissEditor() {
  editorOpen.value = false
  editingReplyId.value = null
  replyForm.value = createEmptyReplyForm()
}

function handleInsertVariable(code: string) {
  if (!replyForm.value.message.trim()) {
    replyForm.value.message = code
    return
  }

  replyForm.value.message = `${replyForm.value.message} ${code}`
}

async function handleSaveReply() {
  if (!replyForm.value.title.trim()) {
    showWarningToast('请输入标题')
    return
  }
  if (!replyForm.value.message.trim()) {
    showWarningToast('请输入消息内容')
    return
  }

  submitting.value = true
  try {
    const payload = {
      title: replyForm.value.title.trim(),
      message: replyForm.value.message.trim(),
    }

    if (editingReplyId.value) {
      const response = await updateQuickReply(editingReplyId.value, payload)
      if (!response.success) {
        throw new Error(response.message || '更新快捷回复失败')
      }
      showSuccessToast('快捷回复已更新')
    } else {
      const response = await createQuickReply(payload)
      if (!response.success) {
        throw new Error(response.message || '创建快捷回复失败')
      }
      showSuccessToast('快捷回复已创建')
    }

    handleDismissEditor()
    await loadReplies()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, '保存快捷回复失败'))
    }
  } finally {
    submitting.value = false
  }
}

async function handleDeleteReply(reply: QuickReplyDTO) {
  const confirmed = await confirmDelete(reply.title)
  if (!confirmed) {
    return
  }

  try {
    const response = await deleteQuickReply(reply.id)
    if (!response.success) {
      throw new Error(response.message || '删除快捷回复失败')
    }
    showSuccessToast('快捷回复已删除')
    await loadReplies()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, '删除快捷回复失败'))
    }
  }
}

async function handleRefresh(event: CustomEvent) {
  await loadReplies()
  event.detail.complete()
}

onIonViewWillEnter(async () => {
  await loadReplies()
})
</script>

<style scoped>
.settings-quick-replies-page {
  display: block;
}

.settings-quick-replies-hero {
  margin-top: 4px;
}

.settings-quick-replies-hero__eyebrow {
  color: var(--ion-color-primary);
  font-weight: 700;
}

.settings-quick-replies-page__section-header {
  align-items: flex-start;
}

.settings-quick-replies-list {
  margin-top: 16px;
}

.settings-quick-reply-card {
  padding: 14px;
  border-radius: 18px;
  border: 1px solid var(--app-border);
  background: rgba(255, 255, 255, 0.82);
}

.settings-quick-reply-card strong,
.settings-quick-reply-card p {
  margin: 0;
}

.settings-quick-reply-card p {
  margin-top: 8px;
  color: var(--app-muted);
  font-size: 13px;
  line-height: 1.6;
  white-space: pre-wrap;
}

.settings-quick-reply-card__actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 14px;
}

.settings-quick-replies-page__empty-state {
  padding-top: 16px;
}

.settings-modal-page {
  --padding-top: 16px;
  --padding-bottom: 24px;
  --padding-start: 16px;
  --padding-end: 16px;
}

.settings-form-grid {
  display: grid;
  gap: 14px;
}

.settings-form-field {
  display: grid;
  gap: 8px;
}

.settings-form-field span {
  color: var(--app-heading);
  font-size: 13px;
  font-weight: 600;
}

.settings-form-field--full {
  grid-column: 1 / -1;
}

.settings-variable-panel {
  margin-top: 18px;
  padding: 14px;
  border-radius: 18px;
  background: var(--app-primary-soft);
}

.settings-variable-panel h3,
.settings-variable-panel p {
  margin: 0;
}

.settings-variable-panel h3 {
  color: var(--app-heading);
  font-size: 15px;
}

.settings-variable-panel p {
  margin-top: 8px;
}

.settings-variable-panel__list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.settings-variable-chip {
  padding: 8px 12px;
  border: 1px solid var(--app-primary-soft-strong);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.92);
  color: var(--ion-color-primary);
  font-size: 12px;
  font-weight: 600;
}

.settings-form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  flex-wrap: wrap;
  margin-top: 18px;
}
</style>
