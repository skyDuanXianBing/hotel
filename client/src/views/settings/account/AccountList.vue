<template>
  <div class="account-list-container">
    <div v-show="!isCollapsed" class="filter-section">
      <div class="filter-row">
        <div class="filter-item">
          <span class="filter-label">{{ t('settingsStage4.accountList.filters.search') }}</span>
          <el-input
            v-model="searchKeyword"
            :placeholder="t('settingsStage4.accountList.placeholders.searchAccount')"
            style="width: 300px"
            clearable
            @keyup.enter="loadAccounts"
            @clear="loadAccounts"
          >
            <template #suffix>
              <el-icon style="cursor: pointer" @click="loadAccounts">
                <Search />
              </el-icon>
            </template>
          </el-input>
        </div>

        <div class="filter-item">
          <span class="filter-label">{{ t('settingsStage4.accountList.filters.permissionRole') }}</span>
          <el-select
            v-model="selectedRole"
            :placeholder="t('settingsStage4.accountList.placeholders.all')"
            style="width: 200px"
            clearable
            @change="loadAccounts"
          >
            <el-option :label="t('settingsStage4.accountList.placeholders.all')" :value="''" />
            <el-option
              v-for="role in roleOptions"
              :key="role.id"
              :label="role.name"
              :value="role.id"
            />
          </el-select>
        </div>

        <div class="filter-item">
          <span class="filter-label">{{ t('settingsStage4.accountList.filters.status') }}</span>
          <el-select
            v-model="selectedStatus"
            :placeholder="t('settingsStage4.accountList.placeholders.all')"
            style="width: 200px"
            clearable
            @change="loadAccounts"
          >
            <el-option :label="t('settingsStage4.accountList.placeholders.all')" :value="''" />
            <el-option :label="t('settingsStage4.accountList.status.enabled')" :value="true" />
            <el-option :label="t('settingsStage4.accountList.status.disabled')" :value="false" />
          </el-select>
        </div>
      </div>
    </div>

    <div class="action-section">
      <div class="action-left">
        <el-button link @click="toggleCollapse">
          {{ isCollapsed ? t('settingsStage4.accountList.actions.expand') : t('settingsStage4.accountList.actions.collapse') }}
          <el-icon>
            <ArrowUp v-if="!isCollapsed" />
            <ArrowDown v-else />
          </el-icon>
        </el-button>
        <el-button @click="handleRoleManagement">{{ t('settingsStage4.accountList.actions.roleManagement') }}</el-button>
      </div>
      <el-button type="primary" @click="handleAdd">{{ t('settingsStage4.accountList.actions.addAccount') }}</el-button>
    </div>

    <el-table
      ref="tableRef"
      v-loading="loading"
      :data="pagedAccounts"
      border
      row-key="id"
      style="width: 100%"
      @selection-change="handleSelectionChange"
    >
      <el-table-column
        type="selection"
        width="55"
        :reserve-selection="true"
        :selectable="isBatchSelectable"
      />
      <el-table-column prop="email" :label="t('settingsStage4.accountList.columns.account')" min-width="220" />
      <el-table-column prop="name" :label="t('settingsStage4.accountList.columns.employeeName')" min-width="140" />
      <el-table-column :label="t('settingsStage4.accountList.columns.baseRole')" min-width="110">
        <template #default="{ row }">
          <el-tag v-if="row.role === 'owner'" type="danger" size="small">{{ t('settingsStage4.accountList.baseRoles.owner') }}</el-tag>
          <el-tag v-else-if="row.role === 'admin'" type="warning" size="small">{{ t('settingsStage4.accountList.baseRoles.admin') }}</el-tag>
          <el-tag v-else type="info" size="small">{{ t('settingsStage4.accountList.baseRoles.member') }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column :label="t('settingsStage4.accountList.columns.permissionRoles')" min-width="220">
        <template #default="{ row }">
          <el-tag
            v-for="role in row.roles"
            :key="role.id"
            size="small"
            style="margin-right: 4px"
          >
            {{ role.name }}
          </el-tag>
          <span v-if="!row.roles.length" class="muted">{{ t('settingsStage4.accountList.none') }}</span>
        </template>
      </el-table-column>
      <el-table-column :label="t('settingsStage4.accountList.columns.extraPermissions')" min-width="140">
        <template #default="{ row }">
          <span :class="row.extraPermissions.length ? 'primary-text' : 'muted'">
            {{ row.extraPermissions.length ? t('settingsStage4.accountList.extraConfigured', { count: row.extraPermissions.length }) : t('settingsStage4.accountList.none') }}
          </span>
        </template>
      </el-table-column>
      <el-table-column :label="t('settingsStage4.accountList.columns.status')" min-width="110">
        <template #default="{ row }">
          <span v-if="isOwnerAccount(row)" class="status-text">{{ t('settingsStage4.accountList.status.enabled') }}</span>
          <el-switch
            v-else
            v-model="row.isActive"
            :active-text="row.isActive ? t('settingsStage4.accountList.status.enabled') : t('settingsStage4.accountList.status.disabled')"
            @change="handleStatusChange(row)"
          />
        </template>
      </el-table-column>
      <el-table-column :label="t('settingsStage4.accountList.columns.actions')" min-width="200" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="handleDetail(row)">{{ t('settingsStage4.common.details') }}</el-button>
          <template v-if="isOwnerAccount(row)">
            <el-button
              v-if="canShowTransferOwner(row)"
              link
              type="primary"
              @click="openTransferOwnerDialog(row)"
            >
              {{ t('settingsStage4.accountList.actions.transferOwner') }}
            </el-button>
          </template>
          <template v-else>
            <el-button link type="primary" @click="handleSetPermission(row)">{{ t('settingsStage4.accountList.actions.setPermissions') }}</el-button>
            <el-button link type="danger" @click="handleDelete(row)">{{ t('settings.common.delete') }}</el-button>
          </template>
        </template>
      </el-table-column>
    </el-table>

    <div class="batch-action-section">
      <div class="action-left">
        <el-checkbox :model-value="isCurrentPageAllSelected" @change="handleSelectAll">
          {{ t('settingsStage4.accountList.actions.selectCurrentPage') }}
        </el-checkbox>
        <span class="selected-text">{{ t('settingsStage4.accountList.batch.selected', { count: selectedAccounts.length }) }}</span>
        <el-button size="small" @click="handleBatchEnable">{{ t('settingsStage4.accountList.status.enabled') }}</el-button>
        <el-button size="small" @click="handleBatchDisable">{{ t('settingsStage4.accountList.status.disabled') }}</el-button>
        <el-button size="small" @click="handleBatchChangeRole">{{ t('settingsStage4.accountList.actions.changeRole') }}</el-button>
        <el-button size="small" type="danger" @click="handleBatchDelete">{{ t('settings.common.delete') }}</el-button>
      </div>
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :page-sizes="[25, 50, 100]"
        :total="totalCount"
        layout="total, sizes, prev, pager, next"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>

    <el-dialog v-model="roleDialogVisible" :title="t('settingsStage4.accountList.dialog.changeRole')" width="600px">
      <el-alert
        :title="t('settingsStage4.accountList.dialog.changeRoleNotice')"
        type="info"
        :closable="false"
        show-icon
        style="margin-bottom: 20px"
      />
      <div class="role-checkboxes">
        <el-checkbox-group v-model="batchSelectedRoleIds">
          <el-checkbox v-for="role in roleOptions" :key="role.id" :label="role.id">
            {{ role.name }}
          </el-checkbox>
        </el-checkbox-group>
      </div>
      <template #footer>
        <el-button @click="roleDialogVisible = false">{{ t('settings.common.cancel') }}</el-button>
        <el-button type="primary" @click="handleConfirmRoleChange">{{ t('settings.common.save') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="transferOwnerDialogVisible"
      :title="t('settingsStage4.accountList.dialog.transferOwner')"
      width="520px"
      destroy-on-close
    >
      <div class="transfer-owner-body">
        <div class="tip-text">{{ t('settingsStage4.accountList.dialog.transferOwnerTip') }}</div>
        <el-select
          v-model="selectedNewOwnerId"
          :placeholder="t('settingsStage4.accountList.placeholders.newOwner')"
          style="width: 100%; margin-top: 16px"
        >
          <el-option
            v-for="account in transferableAccounts"
            :key="account.id"
            :label="formatTransferOwnerOption(account)"
            :value="account.id"
          />
        </el-select>
      </div>
      <template #footer>
        <el-button @click="closeTransferOwnerDialog">{{ t('settings.common.cancel') }}</el-button>
        <el-button type="primary" :loading="transferOwnerLoading" @click="submitTransferOwner">
          {{ t('settingsStage4.accountList.actions.confirmTransfer') }}
        </el-button>
      </template>
    </el-dialog>

    <el-drawer
      v-model="drawerVisible"
      :title="accountForm.id ? t('settingsStage4.accountList.dialog.setAccountPermissions') : t('settingsStage4.accountList.dialog.addAccount')"
      size="80%"
      direction="rtl"
    >
      <div class="drawer-body">
        <div class="section">
          <div class="section-title">{{ t('settingsStage4.accountList.sections.basicInfo') }}</div>
          <el-form :model="accountForm" label-width="100px">
            <el-form-item :label="t('settingsStage4.accountList.fields.email')" required>
              <el-input
                v-model="accountForm.email"
                style="width: 420px"
                :placeholder="t('settingsStage4.accountList.placeholders.email')"
                :disabled="Boolean(accountForm.id)"
              />
            </el-form-item>
            <el-form-item :label="t('settingsStage4.accountList.fields.employeeName')">
              <el-input
                v-model="accountForm.name"
                style="width: 420px"
                :placeholder="t('settingsStage4.accountList.placeholders.employeeName')"
                :disabled="Boolean(accountForm.id)"
              />
            </el-form-item>
          </el-form>
        </div>

        <div class="section">
          <div class="section-title">{{ t('settingsStage4.accountList.sections.permissionSettings') }}</div>
          <div class="sub-title">{{ t('settingsStage4.accountList.sections.rolePermissions') }}</div>
          <div class="role-checkboxes">
            <el-checkbox-group v-model="selectedAccountRoles">
              <el-checkbox v-for="role in roleOptions" :key="role.id" :label="role.id">
                {{ role.name }}
              </el-checkbox>
            </el-checkbox-group>
          </div>

          <el-alert
            :title="t('settingsStage4.accountList.alerts.extraPermissionOnly')"
            type="info"
            :closable="false"
            show-icon
            style="margin: 16px 0"
          />

          <el-tabs v-model="activePermissionTab">
            <el-tab-pane
              v-for="tab in ACCOUNT_PERMISSION_TABS"
              :key="tab.name"
              :label="t(tab.label)"
              :name="tab.name"
            >
              <div
                v-for="section in tab.sections"
                :key="`${tab.name}-${section.title}`"
                class="permission-card"
              >
                <div class="group-title">{{ t(section.title) }}</div>
                <div class="checkbox-grid">
                  <el-checkbox
                    v-for="item in section.items"
                    :key="`${tab.name}-${section.title}-${item.key}`"
                    :model-value="isPermissionChecked(item)"
                    :disabled="isPermissionDisabled(item)"
                    @change="handlePermissionToggle(item, !!$event)"
                  >
                    {{ t(item.label) }}
                  </el-checkbox>
                </div>

                <div v-if="showRoomTypeScopeEditor(tab.name, section)" class="room-scope-box">
                  <div class="sub-head">{{ t('settingsStage4.accountList.sections.roomTypeScope') }}</div>
                  <div class="permission-hint">
                    {{ t('settingsStage4.accountList.hints.roomTypeScope') }}
                  </div>

                  <el-checkbox
                    :model-value="hasAllRoomTypesPermission"
                    :disabled="roleRoomScope.allRoomTypes || !hasViewRoomStatusPermission"
                    @change="handleRoomTypeAllToggle(!!$event)"
                  >
                    {{ t('settingsStage4.accountList.actions.viewAllRoomTypes') }}
                  </el-checkbox>

                  <div v-if="!roomTypes.length" class="tip-text" style="margin-top: 12px">
                    {{ t('settingsStage4.accountList.hints.noRoomTypes') }}
                  </div>

                  <div v-else class="checkbox-grid room-type-grid">
                    <el-checkbox
                      v-for="roomType in roomTypes"
                      :key="roomType.id"
                      :model-value="isRoomTypeChecked(roomType.id)"
                      :disabled="isRoomTypeDisabled(roomType.id)"
                      @change="handleRoomTypeToggle(roomType.id, !!$event)"
                    >
                      {{ roomType.name }}
                    </el-checkbox>
                  </div>
                </div>
              </div>
            </el-tab-pane>
          </el-tabs>
        </div>

        <div class="drawer-footer">
          <el-button @click="closeDrawer">{{ t('settings.common.cancel') }}</el-button>
          <el-button type="primary" @click="submitMember">{{ t('settingsStage4.accountList.actions.finish') }}</el-button>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowDown, ArrowUp, Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useI18n } from 'vue-i18n'
import {
  addStoreMember,
  getStoreMemberDetail,
  getStoreMembers,
  removeStoreMember,
  transferStoreOwner,
  updateStoreMemberPermission,
  type AddStoreMemberRequest,
  type RoleDTO as StoreRoleDTO,
  type StoreMember,
} from '@/api/store'
import {
  getAllRoles,
  getRolePermissions,
  type PermissionDTO,
  type RoleDTO as PermissionRoleDTO,
  PermissionAction,
  PermissionModule,
} from '@/api/role'
import { getAllRoomTypes, type RoomTypeDTO } from '@/api/roomType'
import { useStoreStore } from '@/stores/store'
import { useUserStore } from '@/stores/user'
import {
  ACCOUNT_PERMISSION_TABS,
  type ExtraPermissionKey,
  type PermissionItemConfig,
} from './accountPermissionSchema'

interface Account {
  id: number
  storeUserId?: number
  email: string
  name: string
  role: string
  isActive: boolean
  roles: StoreRoleDTO[]
  extraPermissions: PermissionDTO[]
}

interface AccountForm {
  id?: number
  email: string
  name: string
}

interface RoomScope {
  allRoomTypes: boolean
  roomTypeIds: number[]
}

type ExtraForm = Record<ExtraPermissionKey, boolean> & {
  roomTypeAll: boolean
  roomTypeIds: number[]
}

const router = useRouter()
const storeStore = useStoreStore()
const userStore = useUserStore()
const { t } = useI18n()
const searchKeyword = ref('')
const selectedRole = ref<number | ''>('')
const selectedStatus = ref<boolean | ''>('')
const isCollapsed = ref(false)
const currentPage = ref(1)
const pageSize = ref(25)
const loading = ref(false)
const drawerVisible = ref(false)
const roleDialogVisible = ref(false)
const transferOwnerDialogVisible = ref(false)
const transferOwnerLoading = ref(false)
const activePermissionTab = ref('accommodation')
const tableRef = ref()
const roleOptions = ref<PermissionRoleDTO[]>([])
const roomTypes = ref<RoomTypeDTO[]>([])
const accounts = ref<Account[]>([])
const selectedAccounts = ref<Account[]>([])
const selectedAccountRoles = ref<number[]>([])
const batchSelectedRoleIds = ref<number[]>([])
const currentOwnerAccount = ref<Account | null>(null)
const selectedNewOwnerId = ref<number | null>(null)
const rolePermissionsMap = ref<Map<number, PermissionDTO[]>>(new Map())
const requestToken = ref(0)
const accountForm = ref<AccountForm>({ email: '', name: '' })
const currentStoreId = computed(() => storeStore.currentStore?.id ?? null)
const currentUserRole = computed(() => storeStore.currentStore?.userRole ?? '')
const currentUserEmail = computed(() => userStore.currentUser?.email?.trim().toLowerCase() ?? '')
const allItems = ACCOUNT_PERMISSION_TABS.flatMap((tab) =>
  tab.sections.flatMap((section) => section.items)
)
const itemMap = new Map(
  allItems.map((item) => [`${item.module}-${item.action}`, item] as const)
)
const extraForm = reactive<ExtraForm>(createExtraForm())

watch([searchKeyword, selectedRole, selectedStatus, pageSize], () => {
  currentPage.value = 1
})

watch(
  selectedAccountRoles,
  async (ids) => {
    rolePermissionsMap.value = new Map()
    if (!ids.length) {
      return
    }

    const token = ++requestToken.value
    const nextMap = new Map<number, PermissionDTO[]>()

    for (const id of ids) {
      try {
        const response = await getRolePermissions(id)
        if (token !== requestToken.value) {
          return
        }
        if (response.success && response.data) {
          nextMap.set(id, response.data)
        }
      } catch (error) {
        console.error(`加载角色 ${id} 的权限失败:`, error)
      }
    }

    if (token === requestToken.value) {
      rolePermissionsMap.value = nextMap
    }
  },
  { deep: true }
)

const aggregatedPermissions = computed(() => {
  const map = new Map<string, PermissionDTO>()
  rolePermissionsMap.value.forEach((list) => {
    list.forEach((permission) => {
      map.set(
        `${permission.module}-${permission.action}-${permission.roomTypeId ?? 0}-${permission.allRoomTypes ? 1 : 0}`,
        permission
      )
    })
  })
  return Array.from(map.values())
})

const roleRoomScope = computed<RoomScope>(() => resolveRoomScope(aggregatedPermissions.value))
const hasRoleRoomStatusPermission = computed(
  () => roleRoomScope.value.allRoomTypes || roleRoomScope.value.roomTypeIds.length > 0
)
const hasViewRoomStatusPermission = computed(
  () => hasRoleRoomStatusPermission.value || extraForm.viewRoomStatus
)
const hasAllRoomTypesPermission = computed(
  () => roleRoomScope.value.allRoomTypes || (extraForm.viewRoomStatus && extraForm.roomTypeAll)
)
const availableRoomTypes = computed(() => {
  if (roleRoomScope.value.allRoomTypes) {
    return []
  }
  const inherited = new Set(roleRoomScope.value.roomTypeIds)
  return roomTypes.value.filter((item) => !inherited.has(item.id))
})

watch(
  () => extraForm.viewRoomStatus,
  (value) => {
    if (!value) {
      extraForm.roomTypeAll = false
      extraForm.roomTypeIds = []
    }
  }
)

watch(
  () => extraForm.roomTypeAll,
  (value) => {
    if (value) {
      extraForm.roomTypeIds = []
    }
  }
)

watch(
  () => roleRoomScope.value.allRoomTypes,
  (value) => {
    if (value) {
      extraForm.viewRoomStatus = false
      extraForm.roomTypeAll = false
      extraForm.roomTypeIds = []
    }
  }
)

watch(
  availableRoomTypes,
  (list) => {
    const availableIds = new Set(list.map((item) => item.id))
    extraForm.roomTypeIds = extraForm.roomTypeIds.filter((id) => availableIds.has(id))
    if (
      !hasRoleRoomStatusPermission.value &&
      extraForm.viewRoomStatus &&
      !extraForm.roomTypeAll &&
      !extraForm.roomTypeIds.length
    ) {
      extraForm.viewRoomStatus = false
    }
  },
  { deep: true }
)

const filteredAccounts = computed(() => {
  const keyword = searchKeyword.value.trim().toLowerCase()
  return accounts.value.filter((account) => {
    const matchesKeyword =
      !keyword ||
      account.email.toLowerCase().includes(keyword) ||
      account.name.toLowerCase().includes(keyword)
    const matchesRole =
      selectedRole.value === '' || account.roles.some((role) => role.id === selectedRole.value)
    const matchesStatus =
      selectedStatus.value === '' || account.isActive === selectedStatus.value
    return matchesKeyword && matchesRole && matchesStatus
  })
})

const totalCount = computed(() => filteredAccounts.value.length)
const pagedAccounts = computed(() =>
  filteredAccounts.value.slice(
    (currentPage.value - 1) * pageSize.value,
    currentPage.value * pageSize.value
  )
)
const selectedIdSet = computed(() => new Set(selectedAccounts.value.map((item) => item.id)))
const isCurrentPageAllSelected = computed(
  () =>
    pagedAccounts.value.length > 0 &&
    pagedAccounts.value.every((item) => selectedIdSet.value.has(item.id))
)
const transferableAccounts = computed(() =>
  accounts.value.filter(
    (account) => !isOwnerAccount(account) && (!currentOwnerAccount.value || account.id !== currentOwnerAccount.value.id)
  )
)

function isOwnerAccount(account: Account) {
  return account.role === 'owner'
}

function isBatchSelectable(row: Account) {
  return !isOwnerAccount(row)
}

function canShowTransferOwner(account: Account) {
  if (!isOwnerAccount(account)) {
    return false
  }

  if (currentUserRole.value === 'owner') {
    return true
  }

  return Boolean(currentUserEmail.value) && currentUserEmail.value === account.email.trim().toLowerCase()
}

function formatTransferOwnerOption(account: Account) {
  const displayName = account.name || account.email
  return `${displayName}（${account.email}）`
}

function createExtraForm(): ExtraForm {
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

function resetExtraForm() {
  Object.assign(extraForm, createExtraForm())
}

function resolveRoomScope(permissions: PermissionDTO[]): RoomScope {
  const roomTypeIds = new Set<number>()
  permissions
    .filter(
      (item) =>
        item.module === PermissionModule.ACCOMMODATION &&
        item.action === PermissionAction.VIEW_ROOM_STATUS
    )
    .forEach((item) => {
      if (item.allRoomTypes || !item.roomTypeId || item.roomTypeId === 0) {
        roomTypeIds.clear()
        roomTypeIds.add(0)
        return
      }
      roomTypeIds.add(item.roomTypeId)
    })

  if (roomTypeIds.has(0)) {
    return { allRoomTypes: true, roomTypeIds: [] }
  }

  return { allRoomTypes: false, roomTypeIds: Array.from(roomTypeIds) }
}

function hasRolePermission(item: PermissionItemConfig) {
  if (item.action === PermissionAction.VIEW_ROOM_STATUS) {
    return hasRoleRoomStatusPermission.value
  }

  return aggregatedPermissions.value.some(
    (permission) => permission.module === item.module && permission.action === item.action
  )
}

function isPermissionChecked(item: PermissionItemConfig) {
  if (item.action === PermissionAction.VIEW_ROOM_STATUS) {
    return hasViewRoomStatusPermission.value
  }

  return hasRolePermission(item) || extraForm[item.key]
}

function isPermissionDisabled(item: PermissionItemConfig) {
  return hasRolePermission(item)
}

function handlePermissionToggle(item: PermissionItemConfig, checked: boolean) {
  if (item.action === PermissionAction.VIEW_ROOM_STATUS) {
    handleViewRoomStatusToggle(checked)
    return
  }

  if (hasRolePermission(item)) {
    return
  }

  extraForm[item.key] = checked
}

function showRoomTypeScopeEditor(
  tabName: string,
  section: { items: PermissionItemConfig[] }
) {
  return (
    tabName === 'accommodation' &&
    section.items.some((item) => item.action === PermissionAction.VIEW_ROOM_STATUS) &&
    hasViewRoomStatusPermission.value
  )
}

function handleViewRoomStatusToggle(checked: boolean) {
  if (hasRoleRoomStatusPermission.value) {
    return
  }

  extraForm.viewRoomStatus = checked

  if (!checked) {
    extraForm.roomTypeAll = false
    extraForm.roomTypeIds = []
  }
}

function handleRoomTypeAllToggle(checked: boolean) {
  if (roleRoomScope.value.allRoomTypes || !hasViewRoomStatusPermission.value) {
    return
  }

  extraForm.viewRoomStatus = true
  extraForm.roomTypeAll = checked

  if (checked) {
    extraForm.roomTypeIds = []
  }
}

function isRoomTypeChecked(roomTypeId: number) {
  if (roleRoomScope.value.allRoomTypes || extraForm.roomTypeAll) {
    return true
  }

  return (
    roleRoomScope.value.roomTypeIds.includes(roomTypeId) ||
    extraForm.roomTypeIds.includes(roomTypeId)
  )
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

  if (!extraForm.viewRoomStatus && checked) {
    extraForm.viewRoomStatus = true
  }

  if (checked) {
    if (!extraForm.roomTypeIds.includes(roomTypeId)) {
      extraForm.roomTypeIds.push(roomTypeId)
    }
    return
  }

  extraForm.roomTypeIds = extraForm.roomTypeIds.filter((id) => id !== roomTypeId)

  if (
    !hasRoleRoomStatusPermission.value &&
    !extraForm.roomTypeAll &&
    extraForm.roomTypeIds.length === 0
  ) {
    extraForm.viewRoomStatus = false
  }
}

function applyExtraPermissions(permissions: PermissionDTO[] = []) {
  resetExtraForm()

  permissions.forEach((permission) => {
    if (
      permission.module === PermissionModule.ACCOMMODATION &&
      permission.action === PermissionAction.VIEW_ROOM_STATUS
    ) {
      extraForm.viewRoomStatus = true
      if (permission.allRoomTypes || !permission.roomTypeId || permission.roomTypeId === 0) {
        extraForm.roomTypeAll = true
      } else if (!extraForm.roomTypeIds.includes(permission.roomTypeId)) {
        extraForm.roomTypeIds.push(permission.roomTypeId)
      }
      return
    }

    const item = itemMap.get(`${permission.module}-${permission.action}`)
    if (item) {
      extraForm[item.key] = true
    }
  })
}

function buildExtraPermissions(): PermissionDTO[] | null {
  const permissions: PermissionDTO[] = []

  if (!roleRoomScope.value.allRoomTypes && extraForm.viewRoomStatus) {
    if (extraForm.roomTypeAll) {
      permissions.push({
        module: PermissionModule.ACCOMMODATION,
        action: PermissionAction.VIEW_ROOM_STATUS,
        allRoomTypes: true,
      })
    } else {
      const roomTypeIds = extraForm.roomTypeIds.filter((id) =>
        availableRoomTypes.value.some((item) => item.id === id)
      )
      if (!roomTypeIds.length && !hasRoleRoomStatusPermission.value) {
        ElMessage.warning(t('settingsStage4.accountList.messages.selectRoomTypeScope'))
        return null
      }
      roomTypeIds.forEach((roomTypeId) => {
        permissions.push({
          module: PermissionModule.ACCOMMODATION,
          action: PermissionAction.VIEW_ROOM_STATUS,
          roomTypeId,
        })
      })
    }
  }

  allItems.forEach((item) => {
    if (item.action === PermissionAction.VIEW_ROOM_STATUS) {
      return
    }

    if (extraForm[item.key] && !hasRolePermission(item)) {
      permissions.push({ module: item.module, action: item.action })
    }
  })

  return permissions
}

function resetEditor() {
  accountForm.value = { email: '', name: '' }
  selectedAccountRoles.value = []
  resetExtraForm()
  activePermissionTab.value = 'accommodation'
}

async function loadAccounts(showWarning = true) {
  if (!currentStoreId.value) {
    if (showWarning) {
      ElMessage.warning(t('settingsStage4.accountList.messages.selectStore'))
    }
    accounts.value = []
    selectedAccounts.value = []
    return
  }

  try {
    loading.value = true
    const response = await getStoreMembers(currentStoreId.value)
    if (response.success && response.data) {
      accounts.value = response.data.map((member: StoreMember) => ({
        id: member.user.id,
        storeUserId: member.id,
        email: member.user.email,
        name: member.user.nickname || member.user.email,
        role: member.role,
        isActive: member.isActive,
        roles: member.roles || [],
        extraPermissions: member.extraPermissions || [],
      }))
      selectedAccounts.value = []
      return
    }
    ElMessage.error(response.message || t('settingsStage4.accountList.messages.loadAccountsFailed'))
  } catch (error) {
    console.error('加载账号列表失败:', error)
    ElMessage.error(t('settingsStage4.accountList.messages.loadAccountsFailed'))
  } finally {
    loading.value = false
  }
}

async function loadRoles() {
  try {
    const response = await getAllRoles()
    if (response.success && response.data) {
      roleOptions.value = response.data
    }
  } catch (error) {
    console.error('加载角色列表失败:', error)
  }
}

async function loadRoomTypes() {
  if (!currentStoreId.value) {
    roomTypes.value = []
    return
  }

  try {
    const response = await getAllRoomTypes()
    if (response.success && response.data) {
      roomTypes.value = response.data
    }
  } catch (error) {
    console.error('加载房型列表失败:', error)
  }
}

onMounted(() => {
  loadRoles()
})

watch(
  currentStoreId,
  async (storeId) => {
    if (!storeId) {
      accounts.value = []
      roomTypes.value = []
      return
    }

    try {
      await refreshCurrentStoreContext()
    } catch (error) {
      console.error('刷新当前门店上下文失败:', error)
    }

    await Promise.all([loadAccounts(false), loadRoomTypes()])
  },
  { immediate: true }
)

function toggleCollapse() {
  isCollapsed.value = !isCollapsed.value
}

function handleRoleManagement() {
  router.push('/settings/account/role-management')
}

function handleAdd() {
  resetEditor()
  drawerVisible.value = true
}

function closeDrawer() {
  drawerVisible.value = false
  resetEditor()
}

function openTransferOwnerDialog(row: Account) {
  if (!transferableAccounts.value.length) {
    ElMessage.warning(t('settingsStage4.accountList.messages.noTransferableMembers'))
    return
  }
  currentOwnerAccount.value = row
  selectedNewOwnerId.value = null
  transferOwnerDialogVisible.value = true
}

function closeTransferOwnerDialog() {
  transferOwnerDialogVisible.value = false
  currentOwnerAccount.value = null
  selectedNewOwnerId.value = null
}

function handleDetail(row: Account) {
  ElMessage.info(t('settingsStage4.accountList.messages.viewAccountDetail', { name: row.name }))
}

async function refreshCurrentStoreContext() {
  if (!currentStoreId.value) {
    return
  }

  await storeStore.fetchUserStores(true)
  const latestStore = await storeStore.fetchStoreById(currentStoreId.value)
  storeStore.setCurrentStore(latestStore)
}

async function submitTransferOwner() {
  if (!currentStoreId.value) {
    ElMessage.warning(t('settingsStage4.accountList.messages.selectStore'))
    return
  }

  if (!currentOwnerAccount.value) {
    ElMessage.warning(t('settingsStage4.accountList.messages.ownerNotFound'))
    return
  }

  if (!selectedNewOwnerId.value) {
    ElMessage.warning(t('settingsStage4.accountList.messages.selectNewOwner'))
    return
  }

  try {
    transferOwnerLoading.value = true
    const response = await transferStoreOwner(currentStoreId.value, {
      targetUserId: selectedNewOwnerId.value,
    })

    if (!response.success) {
      ElMessage.error(response.message || t('settingsStage4.accountList.messages.transferOwnerFailed'))
      return
    }

    ElMessage.success(t('settingsStage4.accountList.messages.transferOwnerSuccess'))
    closeTransferOwnerDialog()
    await Promise.all([loadAccounts(false), refreshCurrentStoreContext()])
  } catch (error: any) {
    console.error('更换负责人失败:', error)
    ElMessage.error(error?.response?.data?.message || t('settingsStage4.accountList.messages.transferOwnerFailed'))
  } finally {
    transferOwnerLoading.value = false
  }
}

async function submitMember() {
  if (!accountForm.value.email) {
    ElMessage.warning(t('settingsStage4.accountList.messages.emailRequired'))
    return
  }

  if (!storeStore.currentStore?.id) {
    ElMessage.warning(t('settingsStage4.accountList.messages.selectStore'))
    return
  }

  const extraPermissions = buildExtraPermissions()
  if (extraPermissions === null) {
    return
  }

  try {
    if (accountForm.value.id) {
      const response = await updateStoreMemberPermission(
        storeStore.currentStore.id,
        accountForm.value.id,
        {
          roleIds: selectedAccountRoles.value,
          extraPermissions,
        }
      )
      if (!response.success) {
        ElMessage.error(response.message || t('settingsStage4.accountList.messages.updateMemberPermissionsFailed'))
        return
      }
      ElMessage.success(t('settingsStage4.accountList.messages.updatePermissionsSuccess'))
    } else {
      const request: AddStoreMemberRequest = {
        email: accountForm.value.email,
        role: 'member',
        roleIds: selectedAccountRoles.value,
        extraPermissions,
      }
      const response = await addStoreMember(storeStore.currentStore.id, request)
      if (!response.success) {
        ElMessage.error(response.message || t('settingsStage4.accountList.messages.addMemberFailed'))
        return
      }
      ElMessage.success(t('settingsStage4.accountList.messages.addMemberSuccess'))
    }

    closeDrawer()
    await loadAccounts()
  } catch (error: any) {
    console.error('操作失败:', error)
    ElMessage.error(error?.response?.data?.message || t('settingsStage4.accountList.messages.operationFailed'))
  }
}

async function handleSetPermission(row: Account) {
  if (!storeStore.currentStore?.id) {
    ElMessage.warning(t('settingsStage4.accountList.messages.selectStore'))
    return
  }

  try {
    accountForm.value = { id: row.id, email: row.email, name: row.name }
    const response = await getStoreMemberDetail(storeStore.currentStore.id, row.id)
    if (!response.success || !response.data) {
      ElMessage.error(response.message || t('settingsStage4.accountList.messages.loadMemberFailed'))
      return
    }
    selectedAccountRoles.value = response.data.roles.map((role) => role.id)
    applyExtraPermissions(response.data.extraPermissions || [])
    drawerVisible.value = true
  } catch (error) {
    console.error('加载成员信息失败:', error)
    ElMessage.error(t('settingsStage4.accountList.messages.loadMemberFailed'))
  }
}

async function handleDelete(row: Account) {
  if (!storeStore.currentStore?.id) {
    ElMessage.warning(t('settingsStage4.accountList.messages.selectStore'))
    return
  }

  try {
    await ElMessageBox.confirm(t('settingsStage4.accountList.messages.removeConfirm', { name: row.name }), t('settingsStage4.accountList.messages.removeTitle'), {
      confirmButtonText: t('settings.common.confirm'),
      cancelButtonText: t('settings.common.cancel'),
      type: 'warning',
    })
    const response = await removeStoreMember(storeStore.currentStore.id, row.id)
    if (!response.success) {
      ElMessage.error(response.message || t('settingsStage4.accountList.messages.removeFailed'))
      return
    }
    ElMessage.success(t('settingsStage4.accountList.messages.removeSuccess'))
    await loadAccounts()
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('移除成员失败:', error)
      ElMessage.error(t('settingsStage4.accountList.messages.removeMemberFailed'))
    }
  }
}

async function handleStatusChange(row: Account) {
  if (isOwnerAccount(row)) {
    row.isActive = true
    return
  }

  if (!storeStore.currentStore?.id) {
    ElMessage.warning(t('settingsStage4.accountList.messages.selectStore'))
    return
  }

  try {
    const response = await updateStoreMemberPermission(storeStore.currentStore.id, row.id, {
      isActive: row.isActive,
    })
    if (!response.success) {
      ElMessage.error(response.message || t('settingsStage4.accountList.messages.updateStatusFailed'))
      row.isActive = !row.isActive
      return
    }
    ElMessage.success(
      t('settingsStage4.accountList.messages.statusUpdated', {
        action: t(row.isActive ? 'settingsStage4.accountList.status.enabled' : 'settingsStage4.accountList.status.disabled'),
        name: row.name,
      })
    )
  } catch (error) {
    console.error('更新成员状态失败:', error)
    ElMessage.error(t('settingsStage4.accountList.messages.updateStatusFailed'))
    row.isActive = !row.isActive
  }
}

function handleSelectionChange(selection: Account[]) {
  selectedAccounts.value = selection
}

function handleSelectAll(checked: boolean) {
  pagedAccounts.value.forEach((account) => {
    tableRef.value?.toggleRowSelection(account, checked)
  })
}

function handleBatchEnable() {
  handleBatchUpdateStatus(true)
}

function handleBatchDisable() {
  handleBatchUpdateStatus(false)
}

function handleBatchChangeRole() {
  if (!selectedAccounts.value.length) {
    ElMessage.warning(t('settingsStage4.accountList.messages.selectEmployees'))
    return
  }
  batchSelectedRoleIds.value = []
  roleDialogVisible.value = true
}

function handleConfirmRoleChange() {
  if (!selectedAccounts.value.length) {
    ElMessage.warning(t('settingsStage4.accountList.messages.selectEmployees'))
    return
  }
  if (!batchSelectedRoleIds.value.length) {
    ElMessage.warning(t('settingsStage4.accountList.messages.selectPermissionRole'))
    return
  }
  handleBatchUpdateRoles(batchSelectedRoleIds.value)
}

function handleBatchDelete() {
  handleBatchRemoveMembers()
}

function collectFailed(results: PromiseSettledResult<any>[]) {
  return results.flatMap((result, index) =>
    result.status === 'rejected' || !result.value?.success
      ? [selectedAccounts.value[index]?.name || String(selectedAccounts.value[index]?.id)]
      : []
  )
}

async function handleBatchUpdateStatus(isActive: boolean) {
  if (!storeStore.currentStore?.id) {
    ElMessage.warning(t('settingsStage4.accountList.messages.selectStore'))
    return
  }
  if (!selectedAccounts.value.length) {
    ElMessage.warning(t('settingsStage4.accountList.messages.selectEmployees'))
    return
  }

  try {
    await ElMessageBox.confirm(
      t('settingsStage4.accountList.messages.batchStatusConfirm', {
        action: t(isActive ? 'settingsStage4.accountList.status.enabled' : 'settingsStage4.accountList.status.disabled'),
        count: selectedAccounts.value.length,
      }),
      t('settingsStage4.accountList.messages.batchConfirmTitle'),
      { confirmButtonText: t('settings.common.confirm'), cancelButtonText: t('settings.common.cancel'), type: 'warning' }
    )
    loading.value = true
    const results = await Promise.allSettled(
      selectedAccounts.value.map((account) =>
        updateStoreMemberPermission(storeStore.currentStore!.id, account.id, { isActive })
      )
    )
    const failed = collectFailed(results)
    const successCount = selectedAccounts.value.length - failed.length
    if (failed.length) {
      ElMessage.warning(t('settingsStage4.accountList.messages.batchPartialFailed', { success: successCount, failed: failed.length }))
    } else {
      ElMessage.success(
        t('settingsStage4.accountList.messages.batchStatusSuccess', {
          action: t(isActive ? 'settingsStage4.accountList.status.enabled' : 'settingsStage4.accountList.status.disabled'),
          count: successCount,
        })
      )
    }
    await loadAccounts()
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('批量更新状态失败:', error)
      ElMessage.error(t('settingsStage4.accountList.messages.batchUpdateStatusFailed'))
    }
  } finally {
    loading.value = false
  }
}

async function handleBatchUpdateRoles(roleIds: number[]) {
  if (!storeStore.currentStore?.id) {
    ElMessage.warning(t('settingsStage4.accountList.messages.selectStore'))
    return
  }
  if (!selectedAccounts.value.length) {
    ElMessage.warning(t('settingsStage4.accountList.messages.selectEmployees'))
    return
  }

  try {
    await ElMessageBox.confirm(
      t('settingsStage4.accountList.messages.batchRoleConfirm', { count: selectedAccounts.value.length }),
      t('settingsStage4.accountList.messages.batchConfirmTitle'),
      { confirmButtonText: t('settings.common.confirm'), cancelButtonText: t('settings.common.cancel'), type: 'warning' }
    )
    loading.value = true
    const results = await Promise.allSettled(
      selectedAccounts.value.map((account) =>
        updateStoreMemberPermission(storeStore.currentStore!.id, account.id, { roleIds })
      )
    )
    const failed = collectFailed(results)
    const successCount = selectedAccounts.value.length - failed.length
    if (failed.length) {
      ElMessage.warning(t('settingsStage4.accountList.messages.batchRolePartialFailed', { success: successCount, failed: failed.length }))
    } else {
      ElMessage.success(t('settingsStage4.accountList.messages.batchRoleSuccess', { count: successCount }))
    }
    roleDialogVisible.value = false
    batchSelectedRoleIds.value = []
    await loadAccounts()
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('批量调整角色失败:', error)
      ElMessage.error(t('settingsStage4.accountList.messages.batchRoleFailed'))
    }
  } finally {
    loading.value = false
  }
}

async function handleBatchRemoveMembers() {
  if (!storeStore.currentStore?.id) {
    ElMessage.warning(t('settingsStage4.accountList.messages.selectStore'))
    return
  }
  if (!selectedAccounts.value.length) {
    ElMessage.warning(t('settingsStage4.accountList.messages.selectEmployees'))
    return
  }

  try {
    await ElMessageBox.confirm(
      t('settingsStage4.accountList.messages.batchRemoveConfirm', { count: selectedAccounts.value.length }),
      t('settingsStage4.accountList.messages.batchRemoveTitle'),
      { confirmButtonText: t('settings.common.confirm'), cancelButtonText: t('settings.common.cancel'), type: 'warning' }
    )
    loading.value = true
    const results = await Promise.allSettled(
      selectedAccounts.value.map((account) =>
        removeStoreMember(storeStore.currentStore!.id, account.id)
      )
    )
    const failed = collectFailed(results)
    const successCount = selectedAccounts.value.length - failed.length
    if (failed.length) {
      ElMessage.warning(t('settingsStage4.accountList.messages.batchRemovePartialFailed', { success: successCount, failed: failed.length }))
    } else {
      ElMessage.success(t('settingsStage4.accountList.messages.batchRemoveSuccess', { count: successCount }))
    }
    await loadAccounts()
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('批量移除失败:', error)
      ElMessage.error(t('settingsStage4.accountList.messages.batchRemoveFailed'))
    }
  } finally {
    loading.value = false
  }
}

function handleSizeChange(size: number) {
  pageSize.value = size
}

function handleCurrentChange(page: number) {
  currentPage.value = page
}
</script>

<style scoped>
.account-list-container {
  padding: 20px;
  background: #fff;
  min-height: calc(100vh - 100px);
}

.filter-section {
  margin-bottom: 20px;
}

.filter-row,
.action-left,
.row-inline {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}

.filter-item {
  display: flex;
  gap: 8px;
  align-items: center;
}

.filter-label,
.selected-text,
.tip-text,
.muted,
.permission-hint {
  font-size: 14px;
  color: #606266;
}

.muted {
  color: #909399;
}

.primary-text {
  color: #409eff;
  font-size: 14px;
}

.status-text {
  color: #303133;
  font-size: 14px;
}

.action-section,
.batch-action-section,
.drawer-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.action-section {
  margin-bottom: 16px;
}

.batch-action-section {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #ebeef5;
}

.drawer-body {
  height: 100%;
  padding: 20px;
  display: flex;
  flex-direction: column;
}

.section {
  margin-bottom: 28px;
}

.section-title {
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #ebeef5;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.sub-title,
.sub-head,
.group-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 12px;
}

.role-checkboxes,
.permission-card,
.room-scope-box {
  padding: 16px;
  border-radius: 8px;
  background: #f5f7fa;
}

.permission-card {
  margin-bottom: 16px;
}

.room-scope-box {
  margin-top: 16px;
  background: #fff;
}

.permission-hint {
  margin-bottom: 12px;
  line-height: 1.6;
}

.checkbox-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 12px;
}

.room-type-grid {
  margin-top: 12px;
}

.transfer-owner-body {
  display: flex;
  flex-direction: column;
}

.drawer-footer {
  margin-top: auto;
  padding-top: 16px;
  border-top: 1px solid #ebeef5;
}

:deep(.el-drawer__body) {
  padding: 0;
}

:deep(.el-drawer__header) {
  margin-bottom: 0;
  padding: 20px;
  border-bottom: 1px solid #ebeef5;
}
</style>
