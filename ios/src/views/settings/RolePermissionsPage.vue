<template>
  <ion-page>
    <ion-header translucent>
      <ion-toolbar>
        <ion-buttons slot="start">
          <ion-back-button :default-href="ROUTE_PATHS.settingsStoreMembers" />
        </ion-buttons>
        <ion-title>角色权限</ion-title>
      </ion-toolbar>
    </ion-header>

    <ion-content fullscreen class="mobile-page settings-page-block">
      <section class="mobile-hero settings-page-block__hero">
        <p class="mobile-note settings-page-block__eyebrow">账号与角色</p>
        <h1 class="mobile-title">{{ roleTitle }}</h1>
        <div class="mobile-chip-row">
          <span class="mobile-chip">权限 {{ selectedPermissionCount }}</span>
          <span class="mobile-chip">房型 {{ roomTypes.length }}</span>
        </div>
      </section>

      <div class="mobile-stack">
        <section v-for="tab in SETTINGS_PERMISSION_TABS" :key="tab.name" class="mobile-card">
          <h2 class="mobile-section-title">{{ tab.label }}</h2>
          <div class="mobile-list settings-card-list settings-card-list--compact">
            <article v-for="section in tab.sections" :key="section.title" class="settings-card-item">
              <strong>{{ section.title }}</strong>
              <div class="settings-permission-grid">
                <article v-for="item in section.items" :key="item.key" class="settings-permission-item">
                  <div class="settings-permission-item__header">
                    <div>
                      <strong>{{ item.label }}</strong>
                      <p>{{ permissionScopeSummary(item) }}</p>
                    </div>
                    <ion-checkbox :checked="hasPermission(item)" @ionChange="handlePermissionToggle(item, $event.detail.checked)" />
                  </div>

                  <label
                    v-if="hasPermission(item) && item.supportsRoomTypeScope"
                    class="settings-form-field settings-form-field--top"
                  >
                    <span>房型范围</span>
                    <ion-select :value="getPermissionScopeValue(item)" fill="outline" interface="modal" @ionChange="handleScopeChange(item, $event.detail.value)">
                      <ion-select-option value="ALL">全部房型</ion-select-option>
                      <ion-select-option v-for="roomType in roomTypes" :key="roomType.id" :value="String(roomType.id)">
                        {{ roomType.name }}
                      </ion-select-option>
                    </ion-select>
                  </label>
                </article>
              </div>
            </article>
          </div>
        </section>

        <section class="mobile-card">
          <div class="settings-form-actions">
            <ion-button fill="outline" :disabled="loading || saving" @click="loadPageData">重置</ion-button>
            <ion-button :disabled="loading || saving" @click="handleSave">
              {{ saving ? '保存中...' : '保存权限矩阵' }}
            </ion-button>
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
  IonCheckbox,
  IonContent,
  IonHeader,
  IonPage,
  IonSelect,
  IonSelectOption,
  IonTitle,
  IonToolbar,
  onIonViewWillEnter,
} from '@ionic/vue'
import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'
import { SETTINGS_PERMISSION_TABS, type PermissionItemConfig } from '@/constants/settings'
import { getRoleById, getRolePermissions, updateRolePermissions, type PermissionDTO } from '@/api/role'
import { getAllRoomTypes, type RoomTypeDTO } from '@/api/roomType'
import { ROUTE_PATHS } from '@/router/guards'
import { showSuccessToast, showWarningToast } from '@/utils/notify'
import { isHandledRequestError } from '@/utils/request'
import { isRoomTypeScopedPermission } from '@/utils/settings'

interface PermissionStateItem {
  enabled: boolean
  allRoomTypes: boolean
  roomTypeId: string
}

const route = useRoute()

const loading = ref(false)
const saving = ref(false)
const roleTitle = ref('角色权限')
const roomTypes = ref<RoomTypeDTO[]>([])
const permissionState = ref<Record<string, PermissionStateItem>>({})

const roleId = computed(() => Number(route.params.roleId || 0))
const selectedPermissionCount = computed(() => Object.values(permissionState.value).filter((item) => item.enabled).length)

function buildPermissionKey(module: string, action: string) {
  return `${module}:${action}`
}

function createPermissionState(enabled = false): PermissionStateItem {
  return {
    enabled,
    allRoomTypes: true,
    roomTypeId: '',
  }
}

function resolveWarningMessage(error: unknown, fallbackMessage: string) {
  if (error instanceof Error && error.message) {
    return error.message
  }
  return fallbackMessage
}

function ensurePermissionState(item: PermissionItemConfig) {
  const key = buildPermissionKey(item.module, item.action)
  if (!permissionState.value[key]) {
    permissionState.value[key] = createPermissionState(false)
  }
  return permissionState.value[key]
}

function hasPermission(item: PermissionItemConfig) {
  return ensurePermissionState(item).enabled
}

function getPermissionScopeValue(item: PermissionItemConfig) {
  const state = ensurePermissionState(item)
  if (state.allRoomTypes || !state.roomTypeId) {
    return 'ALL'
  }
  return state.roomTypeId
}

function permissionScopeSummary(item: PermissionItemConfig) {
  const state = ensurePermissionState(item)
  if (!state.enabled) {
    return '未启用'
  }
  if (!item.supportsRoomTypeScope) {
    return '已启用'
  }
  if (state.allRoomTypes || !state.roomTypeId) {
    return '全部房型'
  }
  const roomType = roomTypes.value.find((currentItem) => String(currentItem.id) === state.roomTypeId)
  return roomType ? `仅 ${roomType.name}` : '指定房型'
}

async function loadPageData() {
  if (!roleId.value) {
    showWarningToast('缺少角色 ID')
    return
  }

  loading.value = true
  try {
    const [roleResponse, permissionResponse, roomTypeResponse] = await Promise.all([
      getRoleById(roleId.value),
      getRolePermissions(roleId.value),
      getAllRoomTypes(),
    ])
    if (!roleResponse.success || !roleResponse.data) {
      throw new Error(roleResponse.message || '加载角色失败')
    }
    if (!permissionResponse.success || !permissionResponse.data) {
      throw new Error(permissionResponse.message || '加载角色权限失败')
    }
    if (!roomTypeResponse.success || !roomTypeResponse.data) {
      throw new Error(roomTypeResponse.message || '加载房型失败')
    }

    roleTitle.value = roleResponse.data.name
    roomTypes.value = roomTypeResponse.data

    const nextState: Record<string, PermissionStateItem> = {}
    for (const tab of SETTINGS_PERMISSION_TABS) {
      for (const section of tab.sections) {
        for (const item of section.items) {
          nextState[buildPermissionKey(item.module, item.action)] = createPermissionState(false)
        }
      }
    }

    for (const permission of permissionResponse.data) {
      const key = buildPermissionKey(permission.module, permission.action)
      nextState[key] = {
        enabled: true,
        allRoomTypes: permission.allRoomTypes !== false && !permission.roomTypeId,
        roomTypeId: permission.roomTypeId ? String(permission.roomTypeId) : '',
      }
    }
    permissionState.value = nextState
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, '加载角色权限失败'))
    }
  } finally {
    loading.value = false
  }
}

function handlePermissionToggle(item: PermissionItemConfig, checked: boolean) {
  const state = ensurePermissionState(item)
  state.enabled = checked
  if (checked && item.supportsRoomTypeScope && !state.roomTypeId) {
    state.allRoomTypes = true
  }
}

function handleScopeChange(item: PermissionItemConfig, value: string) {
  const state = ensurePermissionState(item)
  if (value === 'ALL') {
    state.allRoomTypes = true
    state.roomTypeId = ''
    return
  }
  state.allRoomTypes = false
  state.roomTypeId = value
}

async function handleSave() {
  if (!roleId.value) {
    showWarningToast('缺少角色 ID')
    return
  }

  saving.value = true
  try {
    const permissions: PermissionDTO[] = []
    for (const tab of SETTINGS_PERMISSION_TABS) {
      for (const section of tab.sections) {
        for (const item of section.items) {
          const state = ensurePermissionState(item)
          if (!state.enabled) {
            continue
          }

          const permission: PermissionDTO = {
            module: item.module,
            action: item.action,
          }

          if (isRoomTypeScopedPermission(item.module, item.action)) {
            permission.allRoomTypes = state.allRoomTypes || !state.roomTypeId
            if (!permission.allRoomTypes && state.roomTypeId) {
              permission.roomTypeId = Number(state.roomTypeId)
            }
          }

          permissions.push(permission)
        }
      }
    }

    const response = await updateRolePermissions(roleId.value, permissions)
    if (!response.success) {
      throw new Error(response.message || '保存角色权限失败')
    }
    showSuccessToast('角色权限已保存')
    await loadPageData()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, '保存角色权限失败'))
    }
  } finally {
    saving.value = false
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

.settings-card-list {
  margin-top: 16px;
}

.settings-card-list--compact {
  gap: 12px;
}

.settings-card-item {
  display: grid;
  gap: 12px;
  padding: 14px;
  border-radius: 18px;
  border: 1px solid var(--app-border);
  background: rgba(255, 255, 255, 0.82);
}

.settings-permission-grid {
  display: grid;
  gap: 10px;
}

.settings-permission-item {
  padding: 12px;
  border-radius: 16px;
  background: var(--app-primary-soft);
}

.settings-permission-item__header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.settings-permission-item strong,
.settings-permission-item p {
  margin: 0;
}

.settings-permission-item p {
  margin-top: 6px;
  color: var(--app-muted);
  font-size: 12px;
}

.settings-form-field {
  display: grid;
  gap: 8px;
}

.settings-form-field--top {
  margin-top: 10px;
}

.settings-form-field span {
  color: var(--app-heading);
  font-size: 13px;
  font-weight: 600;
}

.settings-form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  flex-wrap: wrap;
}
</style>
