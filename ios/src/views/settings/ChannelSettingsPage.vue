<template>
  <ion-page>
    <ion-header translucent>
      <ion-toolbar>
        <ion-buttons slot="start">
          <ion-back-button :default-href="ROUTE_PATHS.settings" />
        </ion-buttons>
        <ion-title>渠道设置</ion-title>
        <ion-buttons slot="end">
          <ion-button @click="handleCreateChannel">新增</ion-button>
        </ion-buttons>
      </ion-toolbar>
    </ion-header>

    <ion-content fullscreen class="mobile-page settings-page-block">
      <section class="mobile-hero settings-page-block__hero">
        <p class="mobile-note settings-page-block__eyebrow">通用设置</p>
        <h1 class="mobile-title">渠道设置</h1>
        <p class="mobile-subtitle">支持渠道新增、编辑、启停与删除。</p>
      </section>

      <div class="mobile-stack">
        <section class="mobile-card">
          <div class="mobile-inline-row settings-page-block__section-header">
            <div>
              <h2 class="mobile-section-title">渠道列表</h2>
              <p class="mobile-note">颜色与类型用于订单和自动消息的渠道展示。</p>
            </div>
            <ion-spinner v-if="loading" name="crescent" />
          </div>

          <div v-if="channels.length > 0" class="mobile-list settings-card-list">
            <article v-for="channel in channels" :key="channel.id" class="settings-card-item">
              <div class="settings-card-item__header">
                <div>
                  <strong>{{ channel.name }}</strong>
                  <p>{{ channel.code }} · {{ channel.type }}</p>
                </div>
                <span class="settings-card-item__color" :style="{ backgroundColor: channel.color || '#0f766e' }" />
              </div>

              <p class="mobile-note">{{ channel.description || '未填写渠道说明' }}</p>
              <p class="mobile-note">状态：{{ channel.enabled ? '已启用' : '已停用' }}</p>

              <div class="settings-card-item__actions">
                <ion-button size="small" fill="outline" @click="handleEditChannel(channel)">编辑</ion-button>
                <ion-button size="small" fill="outline" @click="handleToggleChannel(channel)">
                  {{ channel.enabled ? '停用' : '启用' }}
                </ion-button>
                <ion-button size="small" color="danger" fill="clear" @click="handleDeleteChannel(channel)">删除</ion-button>
              </div>
            </article>
          </div>

          <p v-else-if="!loading" class="mobile-note">当前暂无渠道。</p>
        </section>
      </div>

      <ion-modal :is-open="editorOpen" @didDismiss="handleDismissEditor">
        <ion-header>
          <ion-toolbar>
            <ion-title>{{ editingChannelId ? '编辑渠道' : '新增渠道' }}</ion-title>
            <ion-buttons slot="end">
              <ion-button @click="handleDismissEditor">关闭</ion-button>
            </ion-buttons>
          </ion-toolbar>
        </ion-header>

        <ion-content class="mobile-page settings-modal-page">
          <section class="mobile-card">
            <div class="settings-form-grid">
              <label class="settings-form-field">
                <span>渠道名称</span>
                <ion-input v-model="channelForm.name" fill="outline" placeholder="请输入渠道名称" />
              </label>

              <label class="settings-form-field">
                <span>渠道代码</span>
                <ion-input v-model="channelForm.code" fill="outline" placeholder="请输入渠道代码" />
              </label>

              <label class="settings-form-field">
                <span>渠道类型</span>
                <ion-select v-model="channelForm.type" fill="outline" interface="action-sheet">
                  <ion-select-option v-for="option in CHANNEL_TYPE_OPTIONS" :key="option.value" :value="option.value">
                    {{ option.label }}
                  </ion-select-option>
                </ion-select>
              </label>

              <label class="settings-form-field">
                <span>颜色</span>
                <ion-select v-model="channelForm.color" fill="outline" interface="action-sheet">
                  <ion-select-option v-for="color in CHANNEL_COLOR_OPTIONS" :key="color" :value="color">
                    {{ color }}
                  </ion-select-option>
                </ion-select>
              </label>

              <label class="settings-form-field settings-form-field--full">
                <span>说明</span>
                <ion-textarea v-model="channelForm.description" :rows="4" fill="outline" placeholder="请输入说明" />
              </label>

              <div class="settings-toggle-field">
                <div>
                  <strong>启用状态</strong>
                  <p>停用后渠道仍保留历史数据，但不再参与新增配置。</p>
                </div>
                <ion-toggle v-model="channelForm.enabled" />
              </div>
            </div>

            <div class="settings-form-actions">
              <ion-button fill="outline" @click="handleDismissEditor">取消</ion-button>
              <ion-button :disabled="submitting" @click="handleSaveChannel">
                {{ submitting ? '提交中...' : '保存渠道' }}
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
  IonSelect,
  IonSelectOption,
  IonSpinner,
  IonTextarea,
  IonTitle,
  IonToggle,
  IonToolbar,
  onIonViewWillEnter,
} from '@ionic/vue'
import { ref } from 'vue'
import { createChannel, deleteChannel, getAllChannels, toggleChannelStatus, updateChannel, type ChannelDTO } from '@/api/channel'
import { CHANNEL_COLOR_OPTIONS, CHANNEL_TYPE_OPTIONS } from '@/constants/settings'
import { ROUTE_PATHS } from '@/router/guards'
import type { CreateChannelRequest } from '@/types/settings'
import { showSuccessToast, showWarningToast } from '@/utils/notify'
import { isHandledRequestError } from '@/utils/request'

const loading = ref(false)
const submitting = ref(false)
const editorOpen = ref(false)
const editingChannelId = ref<number | null>(null)
const channels = ref<ChannelDTO[]>([])
const channelForm = ref<CreateChannelRequest>(createEmptyForm())

function createEmptyForm(): CreateChannelRequest {
  return {
    name: '',
    code: '',
    type: 'DIRECT',
    color: '#0f766e',
    enabled: true,
    description: '',
  }
}

function resolveWarningMessage(error: unknown, fallbackMessage: string) {
  if (error instanceof Error && error.message) {
    return error.message
  }
  return fallbackMessage
}

async function confirmDelete(name: string) {
  const alert = await alertController.create({
    header: '删除渠道',
    message: `确认删除 ${name} 吗？`,
    buttons: [
      { text: '取消', role: 'cancel' },
      { text: '确认删除', role: 'destructive' },
    ],
  })
  await alert.present()
  const result = await alert.onDidDismiss()
  return result.role === 'destructive'
}

async function loadPageData() {
  loading.value = true
  try {
    const response = await getAllChannels()
    if (!response.success || !response.data) {
      throw new Error(response.message || '加载渠道失败')
    }
    channels.value = response.data
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, '加载渠道失败'))
    }
  } finally {
    loading.value = false
  }
}

function handleCreateChannel() {
  editingChannelId.value = null
  channelForm.value = createEmptyForm()
  editorOpen.value = true
}

function handleEditChannel(channel: ChannelDTO) {
  editingChannelId.value = channel.id
  channelForm.value = {
    name: channel.name,
    code: channel.code,
    type: channel.type,
    color: channel.color,
    enabled: channel.enabled,
    description: channel.description || '',
  }
  editorOpen.value = true
}

function handleDismissEditor() {
  editorOpen.value = false
  editingChannelId.value = null
  channelForm.value = createEmptyForm()
}

async function handleSaveChannel() {
  if (!channelForm.value.name.trim()) {
    showWarningToast('请输入渠道名称')
    return
  }
  if (!channelForm.value.code.trim()) {
    showWarningToast('请输入渠道代码')
    return
  }

  submitting.value = true
  try {
    if (editingChannelId.value) {
      const response = await updateChannel(editingChannelId.value, {
        ...channelForm.value,
        name: channelForm.value.name.trim(),
        code: channelForm.value.code.trim(),
        description: channelForm.value.description?.trim(),
      })
      if (!response.success) {
        throw new Error(response.message || '更新渠道失败')
      }
    } else {
      const response = await createChannel({
        ...channelForm.value,
        name: channelForm.value.name.trim(),
        code: channelForm.value.code.trim(),
        description: channelForm.value.description?.trim(),
      })
      if (!response.success) {
        throw new Error(response.message || '创建渠道失败')
      }
    }

    showSuccessToast('渠道已保存')
    handleDismissEditor()
    await loadPageData()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, '保存渠道失败'))
    }
  } finally {
    submitting.value = false
  }
}

async function handleToggleChannel(channel: ChannelDTO) {
  try {
    const response = await toggleChannelStatus(channel.id, !channel.enabled)
    if (!response.success) {
      throw new Error(response.message || '更新渠道状态失败')
    }
    showSuccessToast(response.data.enabled ? '渠道已启用' : '渠道已停用')
    await loadPageData()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, '更新渠道状态失败'))
    }
  }
}

async function handleDeleteChannel(channel: ChannelDTO) {
  const confirmed = await confirmDelete(channel.name)
  if (!confirmed) {
    return
  }
  try {
    const response = await deleteChannel(channel.id)
    if (!response.success) {
      throw new Error(response.message || '删除渠道失败')
    }
    showSuccessToast('渠道已删除')
    await loadPageData()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, '删除渠道失败'))
    }
  }
}

onIonViewWillEnter(async () => {
  await loadPageData()
})
</script>

<style scoped>
.settings-page-block {
  display: block;
}

.settings-page-block__hero {
  margin-top: 4px;
}

.settings-page-block__eyebrow {
  color: var(--ion-color-primary);
  font-weight: 700;
}

.settings-page-block__section-header {
  align-items: flex-start;
}

.settings-card-list {
  margin-top: 16px;
}

.settings-card-item {
  padding: 14px;
  border-radius: 18px;
  border: 1px solid var(--app-border);
  background: rgba(255, 255, 255, 0.82);
}

.settings-card-item__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.settings-card-item strong,
.settings-card-item p {
  margin: 0;
}

.settings-card-item p {
  margin-top: 6px;
  color: var(--app-muted);
  font-size: 13px;
}

.settings-card-item__color {
  width: 18px;
  height: 18px;
  border-radius: 999px;
}

.settings-card-item__actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 12px;
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

.settings-toggle-field {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 14px;
  border-radius: 18px;
  background: var(--app-primary-soft);
}

.settings-toggle-field strong,
.settings-toggle-field p {
  margin: 0;
}

.settings-toggle-field p {
  margin-top: 6px;
  color: var(--app-muted);
  font-size: 12px;
}

.settings-form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  flex-wrap: wrap;
  margin-top: 18px;
}
</style>
