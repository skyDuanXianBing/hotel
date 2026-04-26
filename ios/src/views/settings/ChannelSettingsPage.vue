<template>
  <SettingsCrudPage
    :back-href="ROUTE_PATHS.settings"
    title="渠道设置"
    hero-eyebrow="通用设置"
    hero-title="渠道设置"
    toolbar-action-label="新增"
    section-title="渠道列表"
    :loading="loading"
    :modal-open="editorOpen"
    :modal-title="editingChannelId ? '编辑渠道' : '新增渠道'"
    @toolbar-action="handleCreateChannel"
    @dismiss-editor="handleDismissEditor"
  >
    <div v-if="channels.length > 0" class="mobile-list settings-channel-list">
      <article v-for="channel in channels" :key="channel.id" class="settings-channel-card">
        <div class="settings-channel-card__header">
          <div class="settings-channel-card__title-group">
            <div class="settings-channel-card__title-row">
              <span
                class="settings-channel-card__swatch"
                :style="{ backgroundColor: channel.color || CHANNEL_COLOR_OPTIONS[0].value }"
              />
              <strong>{{ channel.name }}</strong>
            </div>
            <p class="settings-channel-card__subtitle">
              {{ resolveChannelTypeLabel(channel.type) }} · 代码 {{ channel.code }}
            </p>
          </div>
          <span
            class="settings-channel-card__status"
            :class="channel.enabled ? 'is-active' : 'is-inactive'"
          >
            {{ channel.enabled ? '已启用' : '已停用' }}
          </span>
        </div>

        <p class="settings-channel-card__description">
          {{ channel.description || '未填写渠道说明' }}
        </p>

        <div class="settings-channel-card__meta-grid">
          <div class="settings-channel-card__meta-item">
            <span>渠道类型</span>
            <strong>{{ resolveChannelTypeLabel(channel.type) }}</strong>
          </div>
          <div class="settings-channel-card__meta-item">
            <span>渠道代码</span>
            <strong>{{ channel.code }}</strong>
          </div>
        </div>

        <div class="settings-channel-card__actions">
          <ion-button size="small" fill="outline" @click="handleEditChannel(channel)">编辑</ion-button>
          <ion-button size="small" fill="outline" @click="handleToggleChannel(channel)">
            {{ channel.enabled ? '停用' : '启用' }}
          </ion-button>
          <ion-button size="small" color="danger" fill="clear" @click="handleDeleteChannel(channel)">删除</ion-button>
        </div>
      </article>
    </div>

    <p v-else-if="!loading" class="mobile-note">当前暂无渠道。</p>

    <template #modalContent>
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
          <ion-select
            v-model="channelForm.color"
            fill="outline"
            interface="action-sheet"
            :selected-text="resolveChannelColorLabel(channelForm.color)"
          >
            <ion-select-option v-for="color in CHANNEL_COLOR_OPTIONS" :key="color.value" :value="color.value">
              {{ color.label }}
            </ion-select-option>
          </ion-select>
          <div class="settings-channel-color-preview">
            <span
              class="settings-channel-color-preview__swatch"
              :style="{ backgroundColor: channelForm.color || CHANNEL_COLOR_OPTIONS[0].value }"
            />
            <span>{{ resolveChannelColorLabel(channelForm.color) }}</span>
          </div>
        </label>

        <label class="settings-form-field settings-form-field--full">
          <span>说明</span>
          <ion-textarea v-model="channelForm.description" :rows="4" fill="outline" placeholder="请输入说明" />
        </label>

        <div class="settings-toggle-field">
          <div>
            <strong>启用状态</strong>
          </div>
          <ion-toggle v-model="channelForm.enabled" />
        </div>
      </div>
    </template>

    <template #modalActions>
      <ion-button fill="outline" @click="handleDismissEditor">取消</ion-button>
      <ion-button :disabled="submitting" @click="handleSaveChannel">
        {{ submitting ? '提交中...' : '保存渠道' }}
      </ion-button>
    </template>
  </SettingsCrudPage>
</template>

<script setup lang="ts">
import {
  alertController,
  IonButton,
  IonInput,
  IonSelect,
  IonSelectOption,
  IonTextarea,
  IonToggle,
  onIonViewWillEnter,
} from '@ionic/vue'
import { ref } from 'vue'
import { createChannel, deleteChannel, getAllChannels, toggleChannelStatus, updateChannel, type ChannelDTO } from '@/api/channel'
import SettingsCrudPage from '@/components/settings/families/SettingsCrudPage.vue'
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

function resolveChannelColorLabel(colorValue?: string) {
  const matched = CHANNEL_COLOR_OPTIONS.find((item) => item.value === colorValue)
  if (matched) {
    return matched.label
  }
  return '默认颜色'
}

function resolveChannelTypeLabel(typeValue: string) {
  const matched = CHANNEL_TYPE_OPTIONS.find((item) => item.value === typeValue)
  if (matched) {
    return matched.label
  }
  return typeValue || '未设置'
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
.settings-channel-list {
  margin-top: 16px;
  gap: 12px;
}

.settings-channel-card {
  position: relative;
  overflow: hidden;
  padding: 16px;
  border-radius: 22px;
  border: 1px solid rgba(116, 138, 185, 0.12);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.99), rgba(247, 250, 255, 0.95));
  box-shadow:
    0 14px 28px rgba(90, 111, 153, 0.06),
    inset 0 1px 0 rgba(255, 255, 255, 0.94);
}

.settings-channel-card::before {
  content: '';
  position: absolute;
  inset: 0 0 auto;
  height: 86px;
  background: linear-gradient(135deg, rgba(63, 124, 255, 0.08), rgba(255, 255, 255, 0));
  pointer-events: none;
}

.settings-channel-card > * {
  position: relative;
  z-index: 1;
}

.settings-channel-card__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.settings-channel-card__title-group {
  min-width: 0;
  display: grid;
  gap: 8px;
}

.settings-channel-card__title-row {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.settings-channel-card__title-row strong,
.settings-channel-card__subtitle,
.settings-channel-card__description,
.settings-channel-card__meta-item span,
.settings-channel-card__meta-item strong {
  margin: 0;
}

.settings-channel-card__title-row strong {
  color: var(--ios-pms-text-primary);
  font-size: 18px;
  font-weight: var(--ios-pms-weight-heavy);
  line-height: 1.1;
  letter-spacing: -0.03em;
  word-break: break-word;
}

.settings-channel-card__swatch,
.settings-channel-color-preview__swatch {
  flex: none;
  border-radius: 999px;
  border: 1px solid rgba(22, 35, 59, 0.08);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.58);
}

.settings-channel-card__swatch {
  width: 14px;
  height: 14px;
}

.settings-channel-card__subtitle {
  color: var(--ios-pms-text-muted);
  font-size: 12px;
  line-height: 1.5;
}

.settings-channel-card__status {
  display: inline-flex;
  flex: none;
  align-items: center;
  min-height: 28px;
  padding: 0 10px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: var(--ios-pms-weight-bold);
  letter-spacing: 0.01em;
}

.settings-channel-card__status.is-active {
  border: 1px solid rgba(15, 159, 110, 0.12);
  background: rgba(15, 159, 110, 0.1);
  color: var(--ion-color-success);
}

.settings-channel-card__status.is-inactive {
  border: 1px solid rgba(217, 119, 6, 0.12);
  background: rgba(217, 119, 6, 0.12);
  color: var(--ion-color-warning);
}

.settings-channel-card__description {
  margin-top: 14px;
  color: var(--ios-pms-text-secondary);
  font-size: 13px;
  line-height: 1.65;
  white-space: pre-wrap;
}

.settings-channel-card__meta-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin-top: 14px;
}

.settings-channel-card__meta-item {
  display: grid;
  gap: 6px;
  min-width: 0;
  padding: 12px;
  border: 1px solid rgba(116, 138, 185, 0.1);
  border-radius: 16px;
  background: rgba(243, 247, 255, 0.86);
}

.settings-channel-card__meta-item span {
  color: var(--ios-pms-text-soft);
  font-size: 10px;
  font-weight: var(--ios-pms-weight-bold);
  letter-spacing: 0.03em;
}

.settings-channel-card__meta-item strong {
  color: var(--ios-pms-text-primary);
  font-size: 13px;
  font-weight: var(--ios-pms-weight-bold);
  line-height: 1.35;
  word-break: break-word;
}

.settings-channel-card__actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px solid rgba(116, 138, 185, 0.08);
}

.settings-channel-card__actions ion-button {
  margin: 0;
  min-height: 34px;
  --padding-start: 14px;
  --padding-end: 14px;
  --padding-top: 0;
  --padding-bottom: 0;
  --border-radius: 999px;
  --box-shadow: none;
  font-size: 12px;
  font-weight: var(--ios-pms-weight-bold);
  letter-spacing: 0.01em;
}

.settings-channel-card__actions ion-button[fill='outline'] {
  --background: rgba(255, 255, 255, 0.82);
  --color: var(--ios-pms-text-secondary);
  --border-color: rgba(116, 138, 185, 0.14);
}

.settings-channel-card__actions ion-button[fill='clear'] {
  --padding-start: 8px;
  --padding-end: 8px;
}

.settings-channel-card__actions ion-button[color='danger'] {
  --color: #de5c5c;
}

.settings-channel-color-preview {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  color: var(--ios-pms-text-muted);
  font-size: 12px;
  font-weight: var(--ios-pms-weight-medium);
}

.settings-channel-color-preview__swatch {
  width: 16px;
  height: 16px;
}
</style>
