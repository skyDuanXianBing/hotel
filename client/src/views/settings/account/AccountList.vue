<template>
  <div class="account-list-container">
    <div v-show="!isCollapsed" class="filter-section">
      <div class="filter-row">
        <div class="filter-item">
          <span class="filter-label">搜索</span>
          <el-input
            v-model="searchKeyword"
            placeholder="搜索账号、员工姓名"
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
          <span class="filter-label">权限角色</span>
          <el-select
            v-model="selectedRole"
            placeholder="全部"
            style="width: 200px"
            clearable
            @change="loadAccounts"
          >
            <el-option label="全部" :value="''" />
            <el-option
              v-for="role in roleOptions"
              :key="role.id"
              :label="role.name"
              :value="role.id"
            />
          </el-select>
        </div>

        <div class="filter-item">
          <span class="filter-label">状态</span>
          <el-select
            v-model="selectedStatus"
            placeholder="全部"
            style="width: 200px"
            clearable
            @change="loadAccounts"
          >
            <el-option label="全部" :value="''" />
            <el-option label="启用" :value="true" />
            <el-option label="停用" :value="false" />
          </el-select>
        </div>
      </div>
    </div>

    <div class="action-section">
      <div class="action-left">
        <el-button link @click="toggleCollapse">
          {{ isCollapsed ? '展开' : '收起' }}
          <el-icon>
            <ArrowUp v-if="!isCollapsed" />
            <ArrowDown v-else />
          </el-icon>
        </el-button>
        <el-button @click="handleRoleManagement">角色管理</el-button>
      </div>
      <el-button type="primary" @click="handleAdd">添加账号</el-button>
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
      <el-table-column prop="email" label="账号" min-width="220" />
      <el-table-column prop="name" label="员工姓名" min-width="140" />
      <el-table-column label="基础角色" min-width="110">
        <template #default="{ row }">
          <el-tag v-if="row.role === 'owner'" type="danger" size="small">门店负责人</el-tag>
          <el-tag v-else-if="row.role === 'admin'" type="warning" size="small">管理员</el-tag>
          <el-tag v-else type="info" size="small">成员</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="权限角色" min-width="220">
        <template #default="{ row }">
          <el-tag
            v-for="role in row.roles"
            :key="role.id"
            size="small"
            style="margin-right: 4px"
          >
            {{ role.name }}
          </el-tag>
          <span v-if="!row.roles.length" class="muted">无</span>
        </template>
      </el-table-column>
      <el-table-column label="额外权限" min-width="140">
        <template #default="{ row }">
          <span :class="row.extraPermissions.length ? 'primary-text' : 'muted'">
            {{ row.extraPermissions.length ? `已配置 ${row.extraPermissions.length} 项` : '无' }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="状态" min-width="110">
        <template #default="{ row }">
          <span v-if="isOwnerAccount(row)" class="status-text">启用</span>
          <el-switch
            v-else
            v-model="row.isActive"
            :active-text="row.isActive ? '启用' : '停用'"
            @change="handleStatusChange(row)"
          />
        </template>
      </el-table-column>
      <el-table-column label="操作" min-width="200" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="handleDetail(row)">详情</el-button>
          <template v-if="isOwnerAccount(row)">
            <el-button
              v-if="canShowTransferOwner(row)"
              link
              type="primary"
              @click="openTransferOwnerDialog(row)"
            >
              更换负责人
            </el-button>
          </template>
          <template v-else>
            <el-button link type="primary" @click="handleSetPermission(row)">设置权限</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </template>
      </el-table-column>
    </el-table>

    <div class="batch-action-section">
      <div class="action-left">
        <el-checkbox :model-value="isCurrentPageAllSelected" @change="handleSelectAll">
          全选（当前页）
        </el-checkbox>
        <span class="selected-text">已选中 {{ selectedAccounts.length }} 名员工</span>
        <el-button size="small" @click="handleBatchEnable">启用</el-button>
        <el-button size="small" @click="handleBatchDisable">停用</el-button>
        <el-button size="small" @click="handleBatchChangeRole">调整角色</el-button>
        <el-button size="small" type="danger" @click="handleBatchDelete">删除</el-button>
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

    <el-dialog v-model="roleDialogVisible" title="调整角色" width="600px">
      <el-alert
        title="批量调整角色会直接替换当前角色，额外权限会保留。"
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
        <el-button @click="roleDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleConfirmRoleChange">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="transferOwnerDialogVisible"
      title="更换负责人"
      width="520px"
      destroy-on-close
    >
      <div class="transfer-owner-body">
        <div class="tip-text">请选择新的门店负责人。更换后，当前负责人会自动调整为管理员。</div>
        <el-select
          v-model="selectedNewOwnerId"
          placeholder="请选择新的负责人"
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
        <el-button @click="closeTransferOwnerDialog">取消</el-button>
        <el-button type="primary" :loading="transferOwnerLoading" @click="submitTransferOwner">
          确认更换
        </el-button>
      </template>
    </el-dialog>

    <el-drawer
      v-model="drawerVisible"
      :title="accountForm.id ? '设置账号权限' : '添加账号'"
      size="80%"
      direction="rtl"
    >
      <div class="drawer-body">
        <div class="section">
          <div class="section-title">基本信息</div>
          <el-form :model="accountForm" label-width="100px">
            <el-form-item label="邮箱" required>
              <el-input
                v-model="accountForm.email"
                style="width: 420px"
                placeholder="请输入邮箱地址"
                :disabled="Boolean(accountForm.id)"
              />
            </el-form-item>
            <el-form-item label="员工姓名">
              <el-input
                v-model="accountForm.name"
                style="width: 420px"
                placeholder="用于识别当前账号"
                :disabled="Boolean(accountForm.id)"
              />
            </el-form-item>
          </el-form>
        </div>

        <div class="section">
          <div class="section-title">权限设置</div>
          <div class="sub-title">角色权限</div>
          <div class="role-checkboxes">
            <el-checkbox-group v-model="selectedAccountRoles">
              <el-checkbox v-for="role in roleOptions" :key="role.id" :label="role.id">
                {{ role.name }}
              </el-checkbox>
            </el-checkbox-group>
          </div>

          <el-alert
            title="账号页只允许追加额外权限，不能在这里取消角色自带权限；如需调整角色默认权限，请前往角色管理。"
            type="info"
            :closable="false"
            show-icon
            style="margin: 16px 0"
          />

          <el-tabs v-model="activePermissionTab">
            <el-tab-pane
              v-for="tab in ACCOUNT_PERMISSION_TABS"
              :key="tab.name"
              :label="tab.label"
              :name="tab.name"
            >
              <div
                v-for="section in tab.sections"
                :key="`${tab.name}-${section.title}`"
                class="permission-card"
              >
                <div class="group-title">{{ section.title }}</div>
                <div class="checkbox-grid">
                  <el-checkbox
                    v-for="item in section.items"
                    :key="`${tab.name}-${section.title}-${item.key}`"
                    :model-value="isPermissionChecked(item)"
                    :disabled="isPermissionDisabled(item)"
                    @change="handlePermissionToggle(item, !!$event)"
                  >
                    {{ item.label }}
                  </el-checkbox>
                </div>

                <div v-if="showRoomTypeScopeEditor(tab.name, section)" class="room-scope-box">
                  <div class="sub-head">房型范围</div>
                  <div class="permission-hint">
                    勾选后表示该账号可查看对应房型房态；角色已拥有的房型会直接显示为已勾选。
                  </div>

                  <el-checkbox
                    :model-value="hasAllRoomTypesPermission"
                    :disabled="roleRoomScope.allRoomTypes || !hasViewRoomStatusPermission"
                    @change="handleRoomTypeAllToggle(!!$event)"
                  >
                    查看全部房型
                  </el-checkbox>

                  <div v-if="!roomTypes.length" class="tip-text" style="margin-top: 12px">
                    当前门店暂无房型
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
          <el-button @click="closeDrawer">取消</el-button>
          <el-button type="primary" @click="submitMember">完成</el-button>
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
        ElMessage.warning('请至少选择一个房型，或直接授予全部房型权限')
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
      ElMessage.warning('请先选择门店')
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
    ElMessage.error(response.message || '加载账号列表失败')
  } catch (error) {
    console.error('加载账号列表失败:', error)
    ElMessage.error('加载账号列表失败')
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
    ElMessage.warning('当前没有可接手负责人的成员')
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
  ElMessage.info(`查看账号详情：${row.name}`)
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
    ElMessage.warning('请先选择门店')
    return
  }

  if (!currentOwnerAccount.value) {
    ElMessage.warning('未找到当前负责人')
    return
  }

  if (!selectedNewOwnerId.value) {
    ElMessage.warning('请选择新的负责人')
    return
  }

  try {
    transferOwnerLoading.value = true
    const response = await transferStoreOwner(currentStoreId.value, {
      targetUserId: selectedNewOwnerId.value,
    })

    if (!response.success) {
      ElMessage.error(response.message || '更换负责人失败')
      return
    }

    ElMessage.success('更换负责人成功')
    closeTransferOwnerDialog()
    await Promise.all([loadAccounts(false), refreshCurrentStoreContext()])
  } catch (error: any) {
    console.error('更换负责人失败:', error)
    ElMessage.error(error?.response?.data?.message || '更换负责人失败')
  } finally {
    transferOwnerLoading.value = false
  }
}

async function submitMember() {
  if (!accountForm.value.email) {
    ElMessage.warning('请输入邮箱地址')
    return
  }

  if (!storeStore.currentStore?.id) {
    ElMessage.warning('请先选择门店')
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
        ElMessage.error(response.message || '更新成员权限失败')
        return
      }
      ElMessage.success('权限更新成功')
    } else {
      const request: AddStoreMemberRequest = {
        email: accountForm.value.email,
        role: 'member',
        roleIds: selectedAccountRoles.value,
        extraPermissions,
      }
      const response = await addStoreMember(storeStore.currentStore.id, request)
      if (!response.success) {
        ElMessage.error(response.message || '添加成员失败')
        return
      }
      ElMessage.success('成员添加成功')
    }

    closeDrawer()
    await loadAccounts()
  } catch (error: any) {
    console.error('操作失败:', error)
    ElMessage.error(error?.response?.data?.message || '操作失败')
  }
}

async function handleSetPermission(row: Account) {
  if (!storeStore.currentStore?.id) {
    ElMessage.warning('请先选择门店')
    return
  }

  try {
    accountForm.value = { id: row.id, email: row.email, name: row.name }
    const response = await getStoreMemberDetail(storeStore.currentStore.id, row.id)
    if (!response.success || !response.data) {
      ElMessage.error(response.message || '加载成员信息失败')
      return
    }
    selectedAccountRoles.value = response.data.roles.map((role) => role.id)
    applyExtraPermissions(response.data.extraPermissions || [])
    drawerVisible.value = true
  } catch (error) {
    console.error('加载成员信息失败:', error)
    ElMessage.error('加载成员信息失败')
  }
}

async function handleDelete(row: Account) {
  if (!storeStore.currentStore?.id) {
    ElMessage.warning('请先选择门店')
    return
  }

  try {
    await ElMessageBox.confirm(`确定要将“${row.name}”从门店中移除吗？`, '移除确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    const response = await removeStoreMember(storeStore.currentStore.id, row.id)
    if (!response.success) {
      ElMessage.error(response.message || '移除失败')
      return
    }
    ElMessage.success('移除成功')
    await loadAccounts()
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('移除成员失败:', error)
      ElMessage.error('移除成员失败')
    }
  }
}

async function handleStatusChange(row: Account) {
  if (isOwnerAccount(row)) {
    row.isActive = true
    return
  }

  if (!storeStore.currentStore?.id) {
    ElMessage.warning('请先选择门店')
    return
  }

  try {
    const response = await updateStoreMemberPermission(storeStore.currentStore.id, row.id, {
      isActive: row.isActive,
    })
    if (!response.success) {
      ElMessage.error(response.message || '更新状态失败')
      row.isActive = !row.isActive
      return
    }
    ElMessage.success(`已${row.isActive ? '启用' : '停用'}成员：${row.name}`)
  } catch (error) {
    console.error('更新成员状态失败:', error)
    ElMessage.error('更新成员状态失败')
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
    ElMessage.warning('请先选择员工')
    return
  }
  batchSelectedRoleIds.value = []
  roleDialogVisible.value = true
}

function handleConfirmRoleChange() {
  if (!selectedAccounts.value.length) {
    ElMessage.warning('请先选择员工')
    return
  }
  if (!batchSelectedRoleIds.value.length) {
    ElMessage.warning('请至少选择一个权限角色')
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
    ElMessage.warning('请先选择门店')
    return
  }
  if (!selectedAccounts.value.length) {
    ElMessage.warning('请先选择员工')
    return
  }

  try {
    await ElMessageBox.confirm(
      `确定要批量${isActive ? '启用' : '停用'}选中的 ${selectedAccounts.value.length} 名员工吗？`,
      '批量确认',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
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
      ElMessage.warning(`部分处理失败：成功 ${successCount} 名，失败 ${failed.length} 名`)
    } else {
      ElMessage.success(`批量${isActive ? '启用' : '停用'}成功，共 ${successCount} 名`)
    }
    await loadAccounts()
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('批量更新状态失败:', error)
      ElMessage.error('批量更新状态失败')
    }
  } finally {
    loading.value = false
  }
}

async function handleBatchUpdateRoles(roleIds: number[]) {
  if (!storeStore.currentStore?.id) {
    ElMessage.warning('请先选择门店')
    return
  }
  if (!selectedAccounts.value.length) {
    ElMessage.warning('请先选择员工')
    return
  }

  try {
    await ElMessageBox.confirm(
      `确定要为选中的 ${selectedAccounts.value.length} 名员工批量调整角色吗？`,
      '批量确认',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
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
      ElMessage.warning(`部分调整失败：成功 ${successCount} 名，失败 ${failed.length} 名`)
    } else {
      ElMessage.success(`批量调整角色成功，共 ${successCount} 名`)
    }
    roleDialogVisible.value = false
    batchSelectedRoleIds.value = []
    await loadAccounts()
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('批量调整角色失败:', error)
      ElMessage.error('批量调整角色失败')
    }
  } finally {
    loading.value = false
  }
}

async function handleBatchRemoveMembers() {
  if (!storeStore.currentStore?.id) {
    ElMessage.warning('请先选择门店')
    return
  }
  if (!selectedAccounts.value.length) {
    ElMessage.warning('请先选择员工')
    return
  }

  try {
    await ElMessageBox.confirm(
      `确定要将选中的 ${selectedAccounts.value.length} 名员工从门店中移除吗？`,
      '批量移除确认',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
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
      ElMessage.warning(`部分移除失败：成功 ${successCount} 名，失败 ${failed.length} 名`)
    } else {
      ElMessage.success(`批量移除成功，共 ${successCount} 名`)
    }
    await loadAccounts()
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('批量移除失败:', error)
      ElMessage.error('批量移除失败')
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
