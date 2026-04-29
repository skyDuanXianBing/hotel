<template>
  <ion-page>
    <ion-header translucent>
      <ion-toolbar class="app-page-header__toolbar">
        <ion-buttons slot="start">
          <ion-back-button class="app-page-header__back-btn" :default-href="ROUTE_PATHS.settings" />
        </ion-buttons>
        <ion-title class="app-page-header__title">门店成员</ion-title>
        <ion-buttons slot="end">
          <ion-button class="app-page-header__text-btn" fill="clear" @click="handleCreateMember">添加</ion-button>
        </ion-buttons>
      </ion-toolbar>
    </ion-header>

    <ion-content fullscreen class="mobile-page settings-members-page">
      <ion-refresher slot="fixed" @ionRefresh="handleRefresh">
        <ion-refresher-content pulling-text="下拉刷新成员设置" refreshing-spinner="crescent" />
      </ion-refresher>

      <section class="mobile-hero settings-members-hero">
        <p class="mobile-note settings-members-hero__eyebrow">账号与角色</p>
        <h1 class="mobile-title">{{ storeTitle }}</h1>
        <div class="settings-page-hero__summary">
          <div class="settings-page-hero__meta">
            <div class="settings-page-hero__meta-item">
              <span class="settings-page-hero__meta-label">当前视图</span>
              <strong>{{ activeSegmentLabel }}</strong>
            </div>
            <div class="settings-page-hero__meta-item">
              <span class="settings-page-hero__meta-label">成员数</span>
              <strong>{{ members.length }} 人</strong>
            </div>
            <div class="settings-page-hero__meta-item">
              <span class="settings-page-hero__meta-label">启用成员</span>
              <strong>{{ activeMembersCount }} 人</strong>
            </div>
            <div class="settings-page-hero__meta-item">
              <span class="settings-page-hero__meta-label">角色数</span>
              <strong>{{ roles.length }} 个</strong>
            </div>
          </div>
        </div>
      </section>

      <div class="mobile-stack">
        <section class="mobile-card">
          <ion-segment :value="activeSegment" @ionChange="handleSegmentChange">
            <ion-segment-button value="members">
              <ion-label>成员</ion-label>
            </ion-segment-button>
            <ion-segment-button value="roles">
              <ion-label>角色</ion-label>
            </ion-segment-button>
          </ion-segment>
        </section>

        <section v-if="activeSegment === 'members'" class="mobile-card">
          <div class="mobile-inline-row settings-members-page__section-header">
            <div class="settings-members-page__section-heading">
              <h2 class="mobile-section-title">门店成员</h2>
            </div>
            <ion-spinner v-if="loading" name="crescent" />
          </div>

          <div v-if="members.length > 0" class="mobile-list settings-members-list">
            <article v-for="member in members" :key="member.id" class="settings-member-card">
              <div class="settings-member-card__header">
                <div>
                  <strong>{{ resolveMemberName(member) }}</strong>
                  <p>{{ member.user.email }}</p>
                </div>
                <span class="settings-member-card__status" :class="member.isActive ? 'is-active' : 'is-inactive'">
                  {{ member.isActive ? '已激活' : '已停用' }}
                </span>
              </div>

              <div class="settings-member-card__meta">
                <span>基础角色：{{ resolveRoleLabel(member.role) }}</span>
                <span>角色数：{{ member.roles.length }}</span>
                <span>额外权限：{{ member.extraPermissions?.length || 0 }}</span>
                <span>加入时间：{{ formatDate(member.joinedAt) }}</span>
              </div>

              <p class="mobile-note settings-member-card__roles-note">
                {{ resolveAssignedRoles(member) }}
              </p>

              <div class="settings-member-card__actions">
                <ion-button
                  class="settings-member-card__action settings-member-card__action--primary"
                  size="small"
                  fill="solid"
                  @click="handleEditMember(member)"
                >
                  设置权限
                </ion-button>

                <div class="settings-member-card__secondary-actions">
                  <ion-button
                    size="small"
                    fill="solid"
                    class="settings-member-card__action settings-member-card__action--secondary"
                    @click="handleToggleMember(member)"
                  >
                    {{ member.isActive ? '停用' : '启用' }}
                  </ion-button>
                  <ion-button
                    v-if="member.role === 'owner'"
                    size="small"
                    fill="solid"
                    class="settings-member-card__action settings-member-card__action--secondary"
                    @click="handleOpenTransferOwner"
                  >
                    更换负责人
                  </ion-button>
                </div>

                <div class="settings-member-card__danger-zone">
                  <ion-button
                    size="small"
                    color="danger"
                    fill="clear"
                    class="settings-member-card__action settings-member-card__action--danger"
                    @click="handleRemoveMember(member)"
                  >
                    移除成员
                  </ion-button>
                </div>
              </div>
            </article>
          </div>

          <p v-else-if="!loading" class="mobile-note settings-members-page__empty-state">当前门店暂无成员。</p>
        </section>

        <section v-else class="mobile-card">
          <div class="mobile-inline-row settings-members-page__section-header">
            <div class="settings-members-page__section-heading">
              <h2 class="mobile-section-title">角色管理</h2>
            </div>
            <ion-button size="small" class="settings-role-create-button" @click="handleCreateRole">新增角色</ion-button>
          </div>

          <div v-if="roles.length > 0" class="mobile-list settings-roles-list">
            <article v-for="role in roles" :key="role.id" class="settings-role-card">
              <div>
                <strong>{{ role.name }}</strong>
                <p>{{ role.description || '未填写角色说明' }}</p>
              </div>

              <div class="settings-role-card__footer">
                <span>{{ role.isSystem ? '系统角色' : '自定义角色' }}</span>
                <div class="settings-role-card__actions">
                  <ion-button size="small" fill="outline" @click="handleOpenRolePermissions(role)">权限</ion-button>
                  <ion-button size="small" fill="outline" @click="handleEditRole(role)">编辑</ion-button>
                  <ion-button
                    size="small"
                    color="danger"
                    fill="clear"
                    :disabled="role.isSystem"
                    @click="handleDeleteRole(role)"
                  >
                    删除
                  </ion-button>
                </div>
              </div>
            </article>
          </div>

          <p v-else-if="!loading" class="mobile-note settings-members-page__empty-state">当前暂无角色数据。</p>
        </section>
      </div>

      <ion-modal :is-open="memberModalOpen" @didDismiss="handleDismissMemberModal">
        <ion-header>
          <ion-toolbar>
            <ion-title>{{ editingMemberId ? '编辑成员' : '添加成员' }}</ion-title>
            <ion-buttons slot="end">
              <ion-button @click="handleDismissMemberModal">关闭</ion-button>
            </ion-buttons>
          </ion-toolbar>
        </ion-header>

        <ion-content class="mobile-page settings-modal-page">
          <section class="mobile-card">
            <ion-segment :value="memberEditorSegment" @ionChange="handleMemberEditorSegmentChange">
              <ion-segment-button value="basic">
                <ion-label>基础信息</ion-label>
              </ion-segment-button>
              <ion-segment-button value="permissions">
                <ion-label>额外权限</ion-label>
              </ion-segment-button>
            </ion-segment>

            <div v-if="memberEditorSegment === 'basic'" class="settings-form-grid settings-form-grid--with-segment">
              <label class="settings-form-field">
                <span>邮箱</span>
                <ion-input
                  v-model="memberForm.email"
                  fill="outline"
                  placeholder="请输入成员邮箱"
                  :readonly="Boolean(editingMemberId)"
                />
              </label>

              <label class="settings-form-field">
                <span>基础角色</span>
                <ion-select v-model="memberForm.role" fill="outline" interface="action-sheet">
                  <ion-select-option value="owner">负责人</ion-select-option>
                  <ion-select-option value="admin">管理员</ion-select-option>
                  <ion-select-option value="member">成员</ion-select-option>
                </ion-select>
              </label>

              <label class="settings-form-field settings-form-field--full">
                <span>角色列表</span>
                <ion-select v-model="memberForm.roleIds" fill="outline" interface="modal" multiple>
                  <ion-select-option v-for="role in roles" :key="role.id" :value="role.id">
                    {{ role.name }}
                  </ion-select-option>
                </ion-select>
              </label>
            </div>

            <div v-else class="settings-form-grid settings-form-grid--with-segment">
              <div class="settings-member-permissions-note">
                <strong>权限状态</strong>
                <p>{{ permissionSummary }}</p>
              </div>

              <div v-for="tab in SETTINGS_PERMISSION_TABS" :key="tab.name" class="settings-member-permission-tab">
                <h3 class="mobile-section-title">{{ tab.label }}</h3>

                <article v-for="section in tab.sections" :key="`${tab.name}-${section.title}`" class="settings-member-permission-section">
                  <strong>{{ section.title }}</strong>

                  <div class="settings-permission-grid settings-permission-grid--member">
                    <article v-for="item in section.items" :key="item.key" class="settings-permission-item">
                      <div class="settings-permission-item__header">
                        <div>
                          <strong>{{ item.label }}</strong>
                          <p>{{ resolveMemberPermissionSummary(item) }}</p>
                        </div>
                        <ion-checkbox
                          :checked="isMemberPermissionChecked(item)"
                          :disabled="isMemberPermissionDisabled(item)"
                          @ionChange="handleMemberPermissionToggle(item, $event.detail.checked)"
                        />
                      </div>
                    </article>
                  </div>

                  <div v-if="showMemberRoomTypeScopeEditor(tab.name, section)" class="settings-member-room-scope">
                    <div class="settings-member-room-scope__header">
                      <strong>房型范围</strong>
                      <p>可单独设置“查看房态”的房型范围。</p>
                    </div>

                    <label class="settings-member-room-scope__all">
                      <ion-checkbox
                        :checked="hasAllRoomTypesPermission"
                        :disabled="roleRoomScope.allRoomTypes || !hasViewRoomStatusPermission"
                        @ionChange="handleRoomTypeAllToggle($event.detail.checked)"
                      />
                      <span>查看全部房型</span>
                    </label>

                    <p v-if="roomTypes.length === 0" class="mobile-note">当前门店暂无房型。</p>

                    <div v-else class="settings-member-room-type-grid">
                      <label v-for="roomType in roomTypes" :key="roomType.id" class="settings-member-room-type-item">
                        <ion-checkbox
                          :checked="isRoomTypeChecked(roomType.id)"
                          :disabled="isRoomTypeDisabled(roomType.id)"
                          @ionChange="handleRoomTypeToggle(roomType.id, $event.detail.checked)"
                        />
                        <span>{{ roomType.name }}</span>
                      </label>
                    </div>
                  </div>
                </article>
              </div>
            </div>

            <div class="settings-form-actions">
              <ion-button fill="outline" @click="handleDismissMemberModal">取消</ion-button>
              <ion-button :disabled="submitting" @click="handleSaveMember">
                {{ submitting ? '提交中...' : '保存成员' }}
              </ion-button>
            </div>
          </section>
        </ion-content>
      </ion-modal>

      <ion-modal :is-open="roleModalOpen" @didDismiss="handleDismissRoleModal">
        <ion-header>
          <ion-toolbar>
            <ion-title>{{ editingRoleId ? '编辑角色' : '新增角色' }}</ion-title>
            <ion-buttons slot="end">
              <ion-button @click="handleDismissRoleModal">关闭</ion-button>
            </ion-buttons>
          </ion-toolbar>
        </ion-header>

        <ion-content class="mobile-page settings-modal-page">
          <section class="mobile-card">
            <div class="settings-form-grid">
              <label class="settings-form-field">
                <span>角色名称</span>
                <ion-input v-model="roleForm.name" fill="outline" placeholder="请输入角色名称" />
              </label>

              <label class="settings-form-field settings-form-field--full">
                <span>角色说明</span>
                <ion-textarea v-model="roleForm.description" :rows="5" fill="outline" placeholder="请输入角色说明" />
              </label>
            </div>

            <div class="settings-form-actions">
              <ion-button fill="outline" @click="handleDismissRoleModal">取消</ion-button>
              <ion-button :disabled="submitting" @click="handleSaveRole">
                {{ submitting ? '提交中...' : '保存角色' }}
              </ion-button>
            </div>
          </section>
        </ion-content>
      </ion-modal>

      <ion-modal :is-open="transferOwnerModalOpen" @didDismiss="handleDismissTransferOwnerModal">
        <ion-header>
          <ion-toolbar>
            <ion-title>转移负责人</ion-title>
            <ion-buttons slot="end">
              <ion-button @click="handleDismissTransferOwnerModal">关闭</ion-button>
            </ion-buttons>
          </ion-toolbar>
        </ion-header>

        <ion-content class="mobile-page settings-modal-page">
          <section class="mobile-card">
            <label class="settings-form-field settings-form-field--full">
              <span>新负责人</span>
              <ion-select v-model="transferOwnerUserIdText" fill="outline" interface="modal">
                <ion-select-option v-for="member in ownerTransferCandidates" :key="member.user.id" :value="String(member.user.id)">
                  {{ resolveMemberName(member) }} · {{ member.user.email }}
                </ion-select-option>
              </ion-select>
            </label>

            <div class="settings-form-actions">
              <ion-button fill="outline" @click="handleDismissTransferOwnerModal">取消</ion-button>
              <ion-button :disabled="submitting" @click="handleConfirmTransferOwner">
                {{ submitting ? '提交中...' : '确认转移' }}
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
  IonCheckbox,
  IonContent,
  IonHeader,
  IonInput,
  IonLabel,
  IonModal,
  IonPage,
  IonRefresher,
  IonRefresherContent,
  IonSegment,
  IonSegmentButton,
  IonSelect,
  IonSelectOption,
  IonSpinner,
  IonTextarea,
  IonTitle,
  IonToolbar,
  onIonViewWillEnter,
} from '@ionic/vue'
import { computed, ref, watch } from 'vue'
import {
  addStoreMember,
  getStoreMemberDetail,
  getStoreMembers,
  removeStoreMember,
  transferStoreOwner,
  updateStoreMemberPermission,
  type AddStoreMemberRequest,
} from '@/api/store'
import {
  createRole,
  deleteRole,
  getAllRoles,
  getRolePermissions,
  PermissionAction,
  PermissionModule,
  updateRole,
  type PermissionDTO,
  type RoleDTO,
} from '@/api/role'
import { getAllRoomTypes, type RoomTypeDTO } from '@/api/roomType'
import {
  SETTINGS_PERMISSION_TABS,
  type PermissionItemConfig,
  type PermissionSectionConfig,
  type SettingsExtraPermissionKey,
} from '@/constants/settings'
import { buildSettingsRolePermissionsPath, ROUTE_PATHS } from '@/router/guards'
import { useStoreStore } from '@/stores/store'
import type { StoreMember } from '@/types/store'
import { showSuccessToast, showWarningToast } from '@/utils/notify'
import { isHandledRequestError } from '@/utils/request'
import { formatPermissionSummary, resolveRoomScope, type PermissionRoomScope } from '@/utils/settings'
import { useRouter } from 'vue-router'

type MemberSegment = 'members' | 'roles'
type MemberEditorSegment = 'basic' | 'permissions'
type MemberBaseRole = 'owner' | 'admin' | 'member'

interface MemberFormState {
  email: string
  role: MemberBaseRole
  roleIds: number[]
}

interface RoleFormState {
  name: string
  description: string
}

interface ExtraPermissionFormState extends Record<SettingsExtraPermissionKey, boolean> {
  roomTypeAll: boolean
  roomTypeIds: number[]
}

const storeStore = useStoreStore()
const router = useRouter()

const permissionItems: PermissionItemConfig[] = []
for (const tab of SETTINGS_PERMISSION_TABS) {
  for (const section of tab.sections) {
    for (const item of section.items) {
      permissionItems.push(item)
    }
  }
}

const permissionItemMap = new Map<string, PermissionItemConfig>()
for (const item of permissionItems) {
  permissionItemMap.set(`${item.module}-${item.action}`, item)
}

const activeSegment = ref<MemberSegment>('members')
const memberEditorSegment = ref<MemberEditorSegment>('basic')
const loading = ref(false)
const submitting = ref(false)
const members = ref<StoreMember[]>([])
const roles = ref<RoleDTO[]>([])
const roomTypes = ref<RoomTypeDTO[]>([])
const memberModalOpen = ref(false)
const roleModalOpen = ref(false)
const transferOwnerModalOpen = ref(false)
const editingMemberId = ref<number | null>(null)
const editingRoleId = ref<number | null>(null)
const transferOwnerUserIdText = ref('')
const memberForm = ref<MemberFormState>(createEmptyMemberForm())
const roleForm = ref<RoleFormState>(createEmptyRoleForm())
const memberExtraPermissionForm = ref<ExtraPermissionFormState>(createEmptyExtraPermissionForm())
const rolePermissionsMap = ref<Map<number, PermissionDTO[]>>(new Map())
const rolePermissionRequestToken = ref(0)

const storeTitle = computed(() => {
  return storeStore.currentStore?.name || '当前门店'
})

const activeMembersCount = computed(() => {
  return members.value.filter((member) => member.isActive).length
})

const activeSegmentLabel = computed(() => {
  if (activeSegment.value === 'roles') {
    return '角色管理视图'
  }
  return '成员管理视图'
})

const ownerTransferCandidates = computed(() => {
  return members.value.filter((member) => member.role !== 'owner' && member.isActive)
})

const aggregatedRolePermissions = computed<PermissionDTO[]>(() => {
  const mergedPermissionMap = new Map<string, PermissionDTO>()

  rolePermissionsMap.value.forEach((permissions) => {
    for (const permission of permissions) {
      const permissionKey = buildPermissionCacheKey(permission)
      mergedPermissionMap.set(permissionKey, permission)
    }
  })

  return Array.from(mergedPermissionMap.values())
})

const roleRoomScope = computed<PermissionRoomScope>(() => {
  return resolveRoomScope(aggregatedRolePermissions.value)
})

const hasRoleRoomStatusPermission = computed(() => {
  return roleRoomScope.value.allRoomTypes || roleRoomScope.value.roomTypeIds.length > 0
})

const hasViewRoomStatusPermission = computed(() => {
  return hasRoleRoomStatusPermission.value || memberExtraPermissionForm.value.viewRoomStatus
})

const hasAllRoomTypesPermission = computed(() => {
  return roleRoomScope.value.allRoomTypes || (memberExtraPermissionForm.value.viewRoomStatus && memberExtraPermissionForm.value.roomTypeAll)
})

const availableExtraRoomTypes = computed(() => {
  if (roleRoomScope.value.allRoomTypes) {
    return []
  }

  const inheritedRoomTypeIds = new Set(roleRoomScope.value.roomTypeIds)
  return roomTypes.value.filter((roomType) => !inheritedRoomTypeIds.has(roomType.id))
})

const permissionSummary = computed(() => {
  return formatPermissionSummary(buildSelectedExtraPermissionCount())
})

watch(
  () => memberForm.value.roleIds.slice(),
  async (roleIds) => {
    await loadSelectedRolePermissions(roleIds)
  },
)

watch(
  () => memberExtraPermissionForm.value.viewRoomStatus,
  (enabled) => {
    if (enabled) {
      return
    }

    memberExtraPermissionForm.value.roomTypeAll = false
    memberExtraPermissionForm.value.roomTypeIds = []
  },
)

watch(
  () => memberExtraPermissionForm.value.roomTypeAll,
  (enabled) => {
    if (!enabled) {
      return
    }

    memberExtraPermissionForm.value.roomTypeIds = []
  },
)

watch(
  () => roleRoomScope.value.allRoomTypes,
  (enabled) => {
    if (!enabled) {
      return
    }

    memberExtraPermissionForm.value.viewRoomStatus = false
    memberExtraPermissionForm.value.roomTypeAll = false
    memberExtraPermissionForm.value.roomTypeIds = []
  },
)

watch(
  availableExtraRoomTypes,
  (roomTypeList) => {
    const availableRoomTypeIds = new Set(roomTypeList.map((roomType) => roomType.id))
    memberExtraPermissionForm.value.roomTypeIds = memberExtraPermissionForm.value.roomTypeIds.filter((roomTypeId) => {
      return availableRoomTypeIds.has(roomTypeId)
    })

    if (
      hasRoleRoomStatusPermission.value ||
      !memberExtraPermissionForm.value.viewRoomStatus ||
      memberExtraPermissionForm.value.roomTypeAll ||
      memberExtraPermissionForm.value.roomTypeIds.length > 0
    ) {
      return
    }

    memberExtraPermissionForm.value.viewRoomStatus = false
  },
)

function createEmptyMemberForm(): MemberFormState {
  return {
    email: '',
    role: 'member',
    roleIds: [],
  }
}

function createEmptyRoleForm(): RoleFormState {
  return {
    name: '',
    description: '',
  }
}

function createEmptyExtraPermissionForm(): ExtraPermissionFormState {
  return {
    viewRoomStatus: false,
    editRoomStatus: false,
    viewRoomInfo: false,
    viewRoomPrice: false,
    editRoomPrice: false,
    viewPriceLog: false,
    batchChangePrice: false,
    taskList: false,
    viewOrders: false,
    modifyOrder: false,
    cancelOrder: false,
    viewChannels: false,
    manageChannels: false,
    viewStats: false,
    modifyStoreSettings: false,
    manageEmployeeAccounts: false,
    viewFinancialData: false,
    deleteImportantData: false,
    roomTypeAll: false,
    roomTypeIds: [],
  }
}

function resetExtraPermissionForm() {
  memberExtraPermissionForm.value = createEmptyExtraPermissionForm()
}

function normalizeRoleIds(roleIds: Array<number | string>) {
  const normalizedRoleIds: number[] = []

  for (const roleId of roleIds) {
    const nextRoleId = Number(roleId)
    if (Number.isFinite(nextRoleId) && nextRoleId > 0) {
      normalizedRoleIds.push(nextRoleId)
    }
  }

  return normalizedRoleIds
}

function buildPermissionCacheKey(permission: PermissionDTO) {
  return `${permission.module}-${permission.action}-${permission.roomTypeId ?? 0}-${permission.allRoomTypes ? 1 : 0}`
}

function resolveWarningMessage(error: unknown, fallbackMessage: string) {
  if (error instanceof Error && error.message) {
    return error.message
  }
  return fallbackMessage
}

function resolveMemberName(member: StoreMember) {
  return member.user.nickname || member.user.username || member.user.email
}

function resolveAssignedRoles(member: StoreMember) {
  if (!member.roles || member.roles.length === 0) {
    return '未分配额外角色'
  }
  return `已分配角色：${member.roles.map((item) => item.name).join('、')}`
}

function resolveRoleLabel(role: string) {
  if (role === 'owner') {
    return '负责人'
  }
  if (role === 'admin') {
    return '管理员'
  }
  if (role === 'member') {
    return '成员'
  }
  return role || '未标注'
}

function formatDate(rawValue: string) {
  if (!rawValue) {
    return '未知'
  }

  const date = new Date(rawValue)
  if (Number.isNaN(date.getTime())) {
    return rawValue
  }

  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function buildSelectedExtraPermissionCount() {
  let count = 0

  if (!hasRoleRoomStatusPermission.value && memberExtraPermissionForm.value.viewRoomStatus) {
    count += 1
  }

  if (
    hasRoleRoomStatusPermission.value &&
    (memberExtraPermissionForm.value.roomTypeAll || memberExtraPermissionForm.value.roomTypeIds.length > 0)
  ) {
    count += 1
  }

  for (const item of permissionItems) {
    if (item.supportsRoomTypeScope) {
      continue
    }

    if (memberExtraPermissionForm.value[item.key] && !hasInheritedPermission(item)) {
      count += 1
    }
  }

  return count
}

function hasInheritedPermission(item: PermissionItemConfig) {
  if (item.supportsRoomTypeScope) {
    return hasRoleRoomStatusPermission.value
  }

  return aggregatedRolePermissions.value.some((permission) => {
    return permission.module === item.module && permission.action === item.action
  })
}

function isMemberPermissionChecked(item: PermissionItemConfig) {
  if (item.supportsRoomTypeScope) {
    return hasViewRoomStatusPermission.value
  }

  return hasInheritedPermission(item) || memberExtraPermissionForm.value[item.key]
}

function isMemberPermissionDisabled(item: PermissionItemConfig) {
  if (item.supportsRoomTypeScope) {
    return hasRoleRoomStatusPermission.value
  }

  return hasInheritedPermission(item)
}

function resolveRoomStatusPermissionSummary() {
  if (!hasViewRoomStatusPermission.value) {
    return '未启用'
  }

  if (roleRoomScope.value.allRoomTypes) {
    return '角色默认：全部房型'
  }

  if (memberExtraPermissionForm.value.roomTypeAll) {
    return '成员追加：全部房型'
  }

  const coveredRoomTypeIds = new Set<number>()
  for (const roomTypeId of roleRoomScope.value.roomTypeIds) {
    coveredRoomTypeIds.add(roomTypeId)
  }
  for (const roomTypeId of memberExtraPermissionForm.value.roomTypeIds) {
    coveredRoomTypeIds.add(roomTypeId)
  }

  if (coveredRoomTypeIds.size === 0) {
    return '已启用'
  }

  return `已覆盖 ${coveredRoomTypeIds.size} 个房型`
}

function resolveMemberPermissionSummary(item: PermissionItemConfig) {
  if (item.supportsRoomTypeScope) {
    return resolveRoomStatusPermissionSummary()
  }

  if (hasInheritedPermission(item)) {
    return '角色默认已授予'
  }

  if (memberExtraPermissionForm.value[item.key]) {
    return '成员额外已启用'
  }

  return '未启用'
}

async function confirmAction(header: string, message: string, destructive = false) {
  const alert = await alertController.create({
    header,
    message,
    buttons: [
      {
        text: '取消',
        role: 'cancel',
      },
      {
        text: destructive ? '确认删除' : '确认',
        role: destructive ? 'destructive' : 'confirm',
      },
    ],
  })

  await alert.present()
  const result = await alert.onDidDismiss()

  if (destructive) {
    return result.role === 'destructive'
  }

  return result.role === 'confirm'
}

async function loadSelectedRolePermissions(roleIds: Array<number | string>) {
  const normalizedRoleIds = normalizeRoleIds(roleIds)
  rolePermissionsMap.value = new Map()

  if (normalizedRoleIds.length === 0) {
    return
  }

  const requestToken = rolePermissionRequestToken.value + 1
  rolePermissionRequestToken.value = requestToken

  const nextRolePermissionsMap = new Map<number, PermissionDTO[]>()

  for (const roleId of normalizedRoleIds) {
    try {
      const response = await getRolePermissions(roleId)
      if (requestToken !== rolePermissionRequestToken.value) {
        return
      }

      if (response.success && response.data) {
        nextRolePermissionsMap.set(roleId, response.data)
      }
    } catch (error) {
      console.warn(`加载角色 ${roleId} 权限失败`, error)
    }
  }

  if (requestToken === rolePermissionRequestToken.value) {
    rolePermissionsMap.value = nextRolePermissionsMap
  }
}

function handleMemberEditorSegmentChange(event: CustomEvent) {
  const nextSegment = event.detail.value
  if (nextSegment === 'permissions') {
    memberEditorSegment.value = 'permissions'
    return
  }

  memberEditorSegment.value = 'basic'
}

function handleMemberPermissionToggle(item: PermissionItemConfig, checked: boolean) {
  if (item.supportsRoomTypeScope) {
    handleViewRoomStatusToggle(checked)
    return
  }

  if (hasInheritedPermission(item)) {
    return
  }

  memberExtraPermissionForm.value[item.key] = checked
}

function showMemberRoomTypeScopeEditor(tabName: string, section: PermissionSectionConfig) {
  return tabName === 'accommodation' && section.items.some((item) => item.supportsRoomTypeScope) && hasViewRoomStatusPermission.value
}

function handleViewRoomStatusToggle(checked: boolean) {
  if (hasRoleRoomStatusPermission.value) {
    return
  }

  memberExtraPermissionForm.value.viewRoomStatus = checked

  if (!checked) {
    memberExtraPermissionForm.value.roomTypeAll = false
    memberExtraPermissionForm.value.roomTypeIds = []
  }
}

function handleRoomTypeAllToggle(checked: boolean) {
  if (roleRoomScope.value.allRoomTypes || !hasViewRoomStatusPermission.value) {
    return
  }

  memberExtraPermissionForm.value.viewRoomStatus = true
  memberExtraPermissionForm.value.roomTypeAll = checked

  if (checked) {
    memberExtraPermissionForm.value.roomTypeIds = []
  }
}

function isRoomTypeChecked(roomTypeId: number) {
  if (roleRoomScope.value.allRoomTypes || memberExtraPermissionForm.value.roomTypeAll) {
    return true
  }

  return roleRoomScope.value.roomTypeIds.includes(roomTypeId) || memberExtraPermissionForm.value.roomTypeIds.includes(roomTypeId)
}

function isRoomTypeDisabled(roomTypeId: number) {
  if (!hasViewRoomStatusPermission.value) {
    return true
  }

  if (roleRoomScope.value.allRoomTypes) {
    return true
  }

  if (roleRoomScope.value.roomTypeIds.includes(roomTypeId)) {
    return true
  }

  return hasAllRoomTypesPermission.value
}

function handleRoomTypeToggle(roomTypeId: number, checked: boolean) {
  if (roleRoomScope.value.allRoomTypes || roleRoomScope.value.roomTypeIds.includes(roomTypeId)) {
    return
  }

  if (!memberExtraPermissionForm.value.viewRoomStatus && checked) {
    memberExtraPermissionForm.value.viewRoomStatus = true
  }

  if (checked) {
    if (!memberExtraPermissionForm.value.roomTypeIds.includes(roomTypeId)) {
      memberExtraPermissionForm.value.roomTypeIds.push(roomTypeId)
    }
    return
  }

  memberExtraPermissionForm.value.roomTypeIds = memberExtraPermissionForm.value.roomTypeIds.filter((currentRoomTypeId) => {
    return currentRoomTypeId !== roomTypeId
  })

  if (
    hasRoleRoomStatusPermission.value ||
    memberExtraPermissionForm.value.roomTypeAll ||
    memberExtraPermissionForm.value.roomTypeIds.length > 0
  ) {
    return
  }

  memberExtraPermissionForm.value.viewRoomStatus = false
}

function applyExtraPermissions(permissions: PermissionDTO[] = []) {
  resetExtraPermissionForm()

  for (const permission of permissions) {
    if (
      permission.module === PermissionModule.ACCOMMODATION &&
      permission.action === PermissionAction.VIEW_ROOM_STATUS
    ) {
      memberExtraPermissionForm.value.viewRoomStatus = true

      if (permission.allRoomTypes || !permission.roomTypeId || permission.roomTypeId === 0) {
        memberExtraPermissionForm.value.roomTypeAll = true
      } else if (!memberExtraPermissionForm.value.roomTypeIds.includes(permission.roomTypeId)) {
        memberExtraPermissionForm.value.roomTypeIds.push(permission.roomTypeId)
      }

      continue
    }

    const matchedItem = permissionItemMap.get(`${permission.module}-${permission.action}`)
    if (matchedItem) {
      memberExtraPermissionForm.value[matchedItem.key] = true
    }
  }
}

function buildExtraPermissions() {
  const nextPermissions: PermissionDTO[] = []

  if (!roleRoomScope.value.allRoomTypes && memberExtraPermissionForm.value.viewRoomStatus) {
    if (memberExtraPermissionForm.value.roomTypeAll) {
      nextPermissions.push({
        module: PermissionModule.ACCOMMODATION,
        action: PermissionAction.VIEW_ROOM_STATUS,
        allRoomTypes: true,
      })
    } else {
      const availableRoomTypeIdSet = new Set(availableExtraRoomTypes.value.map((roomType) => roomType.id))
      const selectedRoomTypeIds = memberExtraPermissionForm.value.roomTypeIds.filter((roomTypeId) => {
        return availableRoomTypeIdSet.has(roomTypeId)
      })

      if (selectedRoomTypeIds.length === 0 && !hasRoleRoomStatusPermission.value) {
        showWarningToast('请至少选择一个房型，或直接授予全部房型权限')
        return null
      }

      for (const roomTypeId of selectedRoomTypeIds) {
        nextPermissions.push({
          module: PermissionModule.ACCOMMODATION,
          action: PermissionAction.VIEW_ROOM_STATUS,
          roomTypeId,
        })
      }
    }
  }

  for (const item of permissionItems) {
    if (item.supportsRoomTypeScope) {
      continue
    }

    if (memberExtraPermissionForm.value[item.key] && !hasInheritedPermission(item)) {
      nextPermissions.push({
        module: item.module,
        action: item.action,
      })
    }
  }

  return nextPermissions
}

async function loadPageData() {
  const storeId = storeStore.currentStore?.id
  if (!storeId) {
    showWarningToast('请先选择门店')
    return
  }

  loading.value = true
  try {
    const [membersResponse, rolesResponse, roomTypesResponse] = await Promise.all([
      getStoreMembers(storeId),
      getAllRoles(),
      getAllRoomTypes(),
    ])

    if (!membersResponse.success || !membersResponse.data) {
      throw new Error(membersResponse.message || '加载门店成员失败')
    }
    if (!rolesResponse.success || !rolesResponse.data) {
      throw new Error(rolesResponse.message || '加载角色列表失败')
    }
    if (!roomTypesResponse.success || !roomTypesResponse.data) {
      throw new Error(roomTypesResponse.message || '加载房型列表失败')
    }

    members.value = membersResponse.data
    roles.value = rolesResponse.data
    roomTypes.value = roomTypesResponse.data
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, '加载门店成员失败'))
    }
  } finally {
    loading.value = false
  }
}

function handleSegmentChange(event: CustomEvent) {
  const nextSegment = event.detail.value as MemberSegment
  if (nextSegment === 'roles') {
    activeSegment.value = 'roles'
    return
  }
  activeSegment.value = 'members'
}

function handleCreateMember() {
  editingMemberId.value = null
  memberEditorSegment.value = 'basic'
  memberForm.value = createEmptyMemberForm()
  rolePermissionsMap.value = new Map()
  resetExtraPermissionForm()
  memberModalOpen.value = true
}

async function handleEditMember(member: StoreMember) {
  const storeId = storeStore.currentStore?.id
  if (!storeId) {
    showWarningToast('请先选择门店')
    return
  }

  try {
    const response = await getStoreMemberDetail(storeId, member.id)
    if (!response.success || !response.data) {
      throw new Error(response.message || '加载成员权限失败')
    }

    editingMemberId.value = member.id
    memberEditorSegment.value = 'basic'
    memberForm.value = {
      email: response.data.user.email,
      role: (response.data.role as MemberBaseRole) || 'member',
      roleIds: normalizeRoleIds(response.data.roles.map((item) => item.id)),
    }
    applyExtraPermissions(response.data.extraPermissions || [])
    memberModalOpen.value = true
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, '加载成员权限失败'))
    }
  }
}

function handleDismissMemberModal() {
  memberModalOpen.value = false
  editingMemberId.value = null
  memberEditorSegment.value = 'basic'
  memberForm.value = createEmptyMemberForm()
  rolePermissionsMap.value = new Map()
  resetExtraPermissionForm()
}

async function handleSaveMember() {
  const storeId = storeStore.currentStore?.id
  if (!storeId) {
    showWarningToast('请先选择门店')
    return
  }

  if (!memberForm.value.email.trim()) {
    showWarningToast('请输入成员邮箱')
    return
  }

  const extraPermissions = buildExtraPermissions()
  if (extraPermissions === null) {
    return
  }

  const roleIds = normalizeRoleIds(memberForm.value.roleIds)

  submitting.value = true
  try {
    if (editingMemberId.value) {
      const response = await updateStoreMemberPermission(storeId, editingMemberId.value, {
        role: memberForm.value.role,
        roleIds,
        extraPermissions,
      })
      if (!response.success) {
        throw new Error(response.message || '更新成员失败')
      }
      showSuccessToast('成员权限已更新')
    } else {
      const payload: AddStoreMemberRequest = {
        email: memberForm.value.email.trim(),
        role: memberForm.value.role,
        roleIds,
        extraPermissions,
      }
      const response = await addStoreMember(storeId, payload)
      if (!response.success) {
        throw new Error(response.message || '添加成员失败')
      }
      showSuccessToast('成员已添加')
    }

    handleDismissMemberModal()
    await loadPageData()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, '保存成员失败'))
    }
  } finally {
    submitting.value = false
  }
}

async function handleToggleMember(member: StoreMember) {
  const storeId = storeStore.currentStore?.id
  if (!storeId) {
    return
  }

  const nextStatus = !member.isActive
  const confirmed = await confirmAction(
    nextStatus ? '启用成员' : '停用成员',
    `${nextStatus ? '确认启用' : '确认停用'} ${resolveMemberName(member)} 吗？`,
  )
  if (!confirmed) {
    return
  }

  try {
    const response = await updateStoreMemberPermission(storeId, member.id, {
      isActive: nextStatus,
      role: (member.role as MemberBaseRole) || 'member',
      roleIds: normalizeRoleIds(member.roles.map((item) => item.id)),
      extraPermissions: member.extraPermissions || [],
    })
    if (!response.success) {
      throw new Error(response.message || '更新成员状态失败')
    }
    showSuccessToast(nextStatus ? '成员已启用' : '成员已停用')
    await loadPageData()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, '更新成员状态失败'))
    }
  }
}

async function handleRemoveMember(member: StoreMember) {
  const storeId = storeStore.currentStore?.id
  if (!storeId) {
    return
  }

  const confirmed = await confirmAction('移除成员', `确认移除 ${resolveMemberName(member)} 吗？`, true)
  if (!confirmed) {
    return
  }

  try {
    const response = await removeStoreMember(storeId, member.id)
    if (!response.success) {
      throw new Error(response.message || '移除成员失败')
    }
    showSuccessToast('成员已移除')
    await loadPageData()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, '移除成员失败'))
    }
  }
}

function handleCreateRole() {
  editingRoleId.value = null
  roleForm.value = createEmptyRoleForm()
  roleModalOpen.value = true
}

function handleEditRole(role: RoleDTO) {
  editingRoleId.value = role.id
  roleForm.value = {
    name: role.name,
    description: role.description || '',
  }
  roleModalOpen.value = true
}

function handleDismissRoleModal() {
  roleModalOpen.value = false
  editingRoleId.value = null
  roleForm.value = createEmptyRoleForm()
}

function handleOpenTransferOwner() {
  if (ownerTransferCandidates.value.length === 0) {
    showWarningToast('当前没有可接手负责人的成员')
    return
  }

  transferOwnerUserIdText.value = ''
  transferOwnerModalOpen.value = true
}

function handleDismissTransferOwnerModal() {
  transferOwnerModalOpen.value = false
  transferOwnerUserIdText.value = ''
}

async function handleConfirmTransferOwner() {
  const storeId = storeStore.currentStore?.id
  if (!storeId) {
    showWarningToast('请先选择门店')
    return
  }
  if (!transferOwnerUserIdText.value) {
    showWarningToast('请选择新负责人')
    return
  }

  submitting.value = true
  try {
    const response = await transferStoreOwner(storeId, {
      targetUserId: Number(transferOwnerUserIdText.value),
    })
    if (!response.success) {
      throw new Error(response.message || '转移负责人失败')
    }
    showSuccessToast('负责人已转移')
    handleDismissTransferOwnerModal()
    await loadPageData()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, '转移负责人失败'))
    }
  } finally {
    submitting.value = false
  }
}

async function handleOpenRolePermissions(role: RoleDTO) {
  await router.push(buildSettingsRolePermissionsPath(role.id))
}

async function handleSaveRole() {
  if (!roleForm.value.name.trim()) {
    showWarningToast('请输入角色名称')
    return
  }

  submitting.value = true
  try {
    if (editingRoleId.value) {
      const response = await updateRole(editingRoleId.value, {
        name: roleForm.value.name.trim(),
        description: roleForm.value.description.trim(),
      })
      if (!response.success) {
        throw new Error(response.message || '更新角色失败')
      }
      showSuccessToast('角色已更新')
    } else {
      const response = await createRole({
        name: roleForm.value.name.trim(),
        description: roleForm.value.description.trim(),
      })
      if (!response.success) {
        throw new Error(response.message || '创建角色失败')
      }
      showSuccessToast('角色已创建')
    }

    handleDismissRoleModal()
    await loadPageData()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, '保存角色失败'))
    }
  } finally {
    submitting.value = false
  }
}

async function handleDeleteRole(role: RoleDTO) {
  const confirmed = await confirmAction('删除角色', `确认删除角色 ${role.name} 吗？`, true)
  if (!confirmed) {
    return
  }

  try {
    const response = await deleteRole(role.id)
    if (!response.success) {
      throw new Error(response.message || '删除角色失败')
    }
    showSuccessToast('角色已删除')
    await loadPageData()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, '删除角色失败'))
    }
  }
}

async function handleRefresh(event: CustomEvent) {
  await loadPageData()
  event.detail.complete()
}

onIonViewWillEnter(async () => {
  await loadPageData()
})
</script>

<style scoped>
.settings-members-page {
  display: block;
}

.settings-members-hero {
  margin-top: 4px;
  margin-bottom: 12px;
}

.settings-members-hero__eyebrow {
  color: var(--ion-color-primary);
  font-weight: 700;
}

.settings-members-hero .mobile-title {
  max-width: 12ch;
  line-height: 1.08;
}

.settings-members-page__section-header {
  align-items: center;
  gap: 12px;
}

.settings-members-page__section-heading {
  min-width: 0;
}

.settings-members-page__section-heading .mobile-section-title {
  margin: 0;
}

.settings-role-create-button {
  margin: 0;
  min-height: 36px;
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.01em;
  --padding-start: 14px;
  --padding-end: 14px;
  --border-radius: 12px;
  --background: rgba(47, 107, 255, 0.1);
  --background-hover: rgba(47, 107, 255, 0.14);
  --color: #2452c8;
  --box-shadow: none;
}

.settings-members-list,
.settings-roles-list {
  margin-top: 16px;
}

.settings-member-card,
.settings-role-card {
  padding: 18px;
  border-radius: 22px;
  border: 1px solid var(--app-border);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(248, 250, 255, 0.92));
  box-shadow: 0 18px 32px rgba(15, 23, 42, 0.05);
}

.settings-member-card__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.settings-member-card__header strong,
.settings-member-card__header p,
.settings-role-card strong,
.settings-role-card p {
  margin: 0;
}

.settings-member-card__header p,
.settings-role-card p {
  margin-top: 6px;
  color: var(--app-muted);
  font-size: 13px;
}

.settings-member-card__status {
  padding: 5px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
}

.settings-member-card__status.is-active {
  background: rgba(15, 159, 110, 0.12);
  color: var(--ion-color-success);
}

.settings-member-card__status.is-inactive {
  background: rgba(217, 119, 6, 0.12);
  color: var(--ion-color-warning);
}

.settings-member-card__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 14px;
  margin-top: 12px;
  color: var(--app-muted);
  font-size: 12px;
}

.settings-role-card__actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.settings-member-card__actions {
  display: grid;
  gap: 10px;
  margin-top: 16px;
  padding-top: 14px;
  border-top: 1px solid rgba(148, 163, 184, 0.18);
}

.settings-member-card__roles-note {
  margin-top: 12px;
  padding: 10px 12px;
  border-radius: 14px;
  background: rgba(15, 23, 42, 0.035);
  color: var(--app-muted);
}

.settings-member-card__action {
  margin: 0;
  min-height: 40px;
  font-size: 14px;
  font-weight: 600;
  letter-spacing: 0.01em;
  --border-radius: 14px;
  --box-shadow: none;
}

.settings-member-card__action--primary {
  width: 100%;
  --background: linear-gradient(135deg, #2f6bff, #4f8cff);
  --background-hover: linear-gradient(135deg, #2b62eb, #467fe8);
  --color: #fff;
}

.settings-member-card__secondary-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.settings-member-card__secondary-actions .settings-member-card__action {
  flex: 1 1 132px;
  --background: rgba(47, 107, 255, 0.08);
  --background-hover: rgba(47, 107, 255, 0.12);
  --color: #2452c8;
}

.settings-member-card__danger-zone {
  display: flex;
  align-items: center;
  padding-top: 2px;
  border-top: 1px solid rgba(239, 68, 68, 0.08);
}

.settings-member-card__action--danger {
  width: auto;
  min-height: 28px;
  font-size: 13px;
  font-weight: 600;
  --color: #dc2626;
  --padding-start: 0;
  --padding-end: 0;
}

.settings-role-card__footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-top: 12px;
  color: var(--app-muted);
  font-size: 12px;
}

.settings-members-page__empty-state {
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

.settings-form-grid--with-segment {
  margin-top: 16px;
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

.settings-form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  flex-wrap: wrap;
  margin-top: 18px;
}

.settings-member-permissions-note,
.settings-member-permission-section {
  padding: 14px;
  border-radius: 18px;
  border: 1px solid var(--app-border);
  background: rgba(255, 255, 255, 0.82);
}

.settings-member-permissions-note strong,
.settings-member-permissions-note p,
.settings-member-permission-section strong {
  margin: 0;
}

.settings-member-permissions-note p {
  margin-top: 8px;
  color: var(--app-muted);
  font-size: 13px;
  line-height: 1.6;
}

.settings-member-permission-tab {
  display: grid;
  gap: 12px;
}

.settings-permission-grid {
  display: grid;
  gap: 10px;
}

.settings-member-permission-section {
  display: grid;
  gap: 12px;
}

.settings-permission-grid--member {
  gap: 10px;
}

.settings-permission-item {
  padding: 12px;
  border-radius: 16px;
  background: var(--app-primary-soft);
}

.settings-permission-item__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.settings-permission-item__header strong,
.settings-permission-item__header p {
  margin: 0;
}

.settings-permission-item__header p {
  margin-top: 6px;
  color: var(--app-muted);
  font-size: 12px;
}

.settings-member-room-scope {
  display: grid;
  gap: 12px;
  padding: 12px;
  border-radius: 16px;
  background: var(--app-primary-soft);
}

.settings-member-room-scope__header strong,
.settings-member-room-scope__header p {
  margin: 0;
}

.settings-member-room-scope__header p {
  margin-top: 6px;
  color: var(--app-muted);
  font-size: 12px;
  line-height: 1.6;
}

.settings-member-room-scope__all,
.settings-member-room-type-item {
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--app-heading);
  font-size: 13px;
}

.settings-member-room-type-grid {
  display: grid;
  gap: 10px;
}
</style>
