<template>
  <div class="account-list-container">
    <!-- 筛选区域 -->
    <div class="filter-section">
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
              <el-icon @click="loadAccounts" style="cursor: pointer"><Search /></el-icon>
            </template>
          </el-input>
        </div>

        <div class="filter-item">
          <span class="filter-label">员工角色</span>
          <el-select v-model="selectedRole" placeholder="全部" style="width: 200px" clearable @change="loadAccounts">
            <el-option label="全部" :value="''" />
            <el-option v-for="role in roleOptions" :key="role.id" :label="role.name" :value="role.id" />
          </el-select>
        </div>

        <div class="filter-item">
          <span class="filter-label">状态</span>
          <el-select v-model="selectedStatus" placeholder="全部" style="width: 200px" clearable @change="loadAccounts">
            <el-option label="全部" :value="''" />
            <el-option label="启用" :value="true" />
            <el-option label="停用" :value="false" />
          </el-select>
        </div>
      </div>
    </div>

    <!-- 操作按钮区域 -->
    <div class="action-section">
      <div class="action-left">
        <el-button link @click="toggleCollapse">
          收起 <el-icon><ArrowUp v-if="!isCollapsed" /><ArrowDown v-else /></el-icon>
        </el-button>
        <el-button @click="handleRoleManagement">角色管理</el-button>
      </div>
      <el-button type="primary" @click="handleAdd">添加</el-button>
    </div>

    <!-- 表格 -->
    <el-table :data="filteredAccounts" border style="width: 100%" v-loading="loading">
      <el-table-column type="selection" width="55" />
      <el-table-column prop="email" label="账号" min-width="200" />
      <el-table-column prop="name" label="员工姓名" min-width="120" />
      <el-table-column prop="role" label="基础角色" min-width="100">
        <template #default="{ row }">
          <el-tag v-if="row.role === 'owner'" type="danger" size="small">所有者</el-tag>
          <el-tag v-else-if="row.role === 'admin'" type="warning" size="small">管理员</el-tag>
          <el-tag v-else type="info" size="small">成员</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="权限角色" min-width="200">
        <template #default="{ row }">
          <el-tag
            v-for="role in row.roles"
            :key="role.id"
            size="small"
            style="margin-right: 4px"
          >
            {{ role.name }}
          </el-tag>
          <span v-if="!row.roles || row.roles.length === 0" style="color: #909399; font-size: 12px">
            无
          </span>
        </template>
      </el-table-column>
      <el-table-column label="状态" min-width="100">
        <template #default="{ row }">
          <el-switch
            v-model="row.isActive"
            :active-text="row.isActive ? '启用' : '停用'"
            @change="handleStatusChange(row)"
          />
        </template>
      </el-table-column>
      <el-table-column label="操作" min-width="200" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="handleDetail(row)">详情</el-button>
          <el-button link type="primary" @click="handleSetPermission(row)">设置权限</el-button>
          <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 底部批量操作 -->
    <div class="batch-action-section">
      <div class="batch-action-left">
        <el-checkbox v-model="selectAll" @change="handleSelectAll">全选</el-checkbox>
        <span class="selected-text">已选中0个员工</span>
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

    <!-- 调整角色对话框 -->
    <el-dialog v-model="roleDialogVisible" title="调整角色" width="600px">
      <el-alert
        title="批量调整角色后,调整后角色会替换原有角色。"
        type="info"
        :closable="false"
        show-icon
        style="margin-bottom: 20px"
      />

      <div style="margin-bottom: 20px">
        <div style="color: #606266; margin-bottom: 12px">当前共选中1名员工: 甜甜</div>
        <div style="color: #909399; font-size: 14px">员工角色</div>
      </div>

      <div style="display: flex; flex-wrap: wrap; gap: 12px">
        <el-checkbox v-model="selectedRoles.role11">11</el-checkbox>
        <el-checkbox v-model="selectedRoles.cleaner">清扫</el-checkbox>
        <el-checkbox v-model="selectedRoles.soPms">SO PMS</el-checkbox>
        <el-checkbox v-model="selectedRoles.admin">全权限</el-checkbox>
      </div>

      <template #footer>
        <el-button @click="roleDialogVisible = false" size="large">取消</el-button>
        <el-button type="primary" @click="handleConfirmRoleChange" size="large">保存</el-button>
      </template>
    </el-dialog>

    <!-- 添加账号抽屉 -->
    <el-drawer v-model="addDrawerVisible" title="" size="80%" direction="rtl">
      <div class="add-account-content">
        <!-- 标签页 -->
        <el-tabs v-model="activeTab" class="account-tabs">
          <el-tab-pane label="房态" name="room-status">
            <div class="tab-content">
              <!-- 基本信息 -->
              <div class="section">
                <h3 class="section-title">基本信息</h3>
                <el-form :model="accountForm" label-width="100px" class="account-form">
                  <el-form-item label="邮箱" required>
                    <el-input
                      v-model="accountForm.email"
                      placeholder="请输入邮箱地址"
                      style="width: 400px"
                    />
                  </el-form-item>
                  <el-form-item label="员工姓名" required>
                    <el-input
                      v-model="accountForm.name"
                      placeholder="请输入员工姓名"
                      style="width: 400px"
                    />
                  </el-form-item>
                </el-form>
              </div>

              <!-- 权限设置 -->
              <div class="section">
                <h3 class="section-title">权限设置</h3>

                <!-- 角色权限选择 -->
                <div class="role-permission-section">
                  <h4 class="subsection-title">角色权限</h4>
                  <div class="role-checkboxes">
                    <el-checkbox-group v-model="selectedAccountRoles">
                      <el-checkbox
                        v-for="role in roleOptions"
                        :key="role.id"
                        :label="role.id"
                      >
                        {{ role.name }}
                      </el-checkbox>
                    </el-checkbox-group>
                  </div>
                </div>

                <el-divider />

                <h4 class="subsection-title">
                  附加额外权限
                  <el-tag type="info" size="small" style="margin-left: 12px">预览模式</el-tag>
                </h4>
                <el-alert
                  title="以下权限是通过上方选中的角色自动汇总的,如需修改请在角色管理中调整角色权限"
                  type="info"
                  :closable="false"
                  show-icon
                  style="margin-bottom: 16px"
                />
                <el-tabs v-model="activePermissionTab" class="permission-tabs">
                  <!-- 生意管理 -->
                  <el-tab-pane label="住宿管理" name="business">
                    <div class="permission-content">
                      <div class="permission-group">
                        <h4 class="group-title">房型权限</h4>
                        <div class="permission-note">
                          房型权限在角色管理中配置,可以指定"所有房型"或特定房型
                        </div>
                      </div>

                      <div class="permission-group">
                        <h4 class="group-title">房态管理</h4>
                        <div class="checkbox-group">
                          <el-checkbox
                            :model-value="hasPermission(PermissionModule.ACCOMMODATION, PermissionAction.VIEW_ROOM_STATUS)"
                            disabled
                          >
                            查看房态
                          </el-checkbox>
                          <el-checkbox
                            :model-value="hasPermission(PermissionModule.ACCOMMODATION, PermissionAction.EDIT_ROOM_STATUS)"
                            disabled
                          >
                            修改房态
                          </el-checkbox>
                          <el-checkbox
                            :model-value="hasPermission(PermissionModule.ACCOMMODATION, PermissionAction.VIEW_ROOM_OPERATION_LOG)"
                            disabled
                          >
                            查看房态操作日志
                          </el-checkbox>
                          <el-checkbox
                            :model-value="hasPermission(PermissionModule.ACCOMMODATION, PermissionAction.VIEW_ROOM_INFO)"
                            disabled
                          >
                            查看房情表
                          </el-checkbox>
                        </div>
                      </div>

                      <div class="permission-group">
                        <h4 class="group-title">房价管理</h4>
                        <div class="checkbox-group">
                          <el-checkbox
                            :model-value="hasPermission(PermissionModule.ACCOMMODATION, PermissionAction.VIEW_ROOM_PRICE)"
                            disabled
                          >
                            查看房价
                          </el-checkbox>
                          <el-checkbox
                            :model-value="hasPermission(PermissionModule.ACCOMMODATION, PermissionAction.EDIT_ROOM_PRICE)"
                            disabled
                          >
                            修改房价
                          </el-checkbox>
                          <el-checkbox
                            :model-value="hasPermission(PermissionModule.ACCOMMODATION, PermissionAction.VIEW_PRICE_LOG)"
                            disabled
                          >
                            查看改价记录
                          </el-checkbox>
                          <el-checkbox
                            :model-value="hasPermission(PermissionModule.ACCOMMODATION, PermissionAction.BATCH_CHANGE_PRICE)"
                            disabled
                          >
                            批量改价
                          </el-checkbox>
                        </div>
                      </div>

                      <div class="permission-group">
                        <h4 class="group-title">保洁管理</h4>
                        <div class="checkbox-group">
                          <el-checkbox
                            :model-value="hasPermission(PermissionModule.ACCOMMODATION, PermissionAction.TASK_LIST)"
                            disabled
                          >
                            查看保洁任务
                          </el-checkbox>
                        </div>
                      </div>
                    </div>
                  </el-tab-pane>

                  <!-- 订单管理 -->
                  <el-tab-pane label="订单管理" name="order">
                    <div class="permission-content">
                      <div class="permission-group">
                        <div class="checkbox-group">
                          <el-checkbox
                            :model-value="hasPermission(PermissionModule.ORDER, PermissionAction.VIEW_ORDERS)"
                            disabled
                          >
                            查看订单
                          </el-checkbox>
                          <el-checkbox
                            :model-value="hasPermission(PermissionModule.ORDER, PermissionAction.MODIFY_ORDER)"
                            disabled
                          >
                            修改订单
                          </el-checkbox>
                          <el-checkbox
                            :model-value="hasPermission(PermissionModule.ORDER, PermissionAction.CANCEL_ORDER)"
                            disabled
                          >
                            取消订单
                          </el-checkbox>
                        </div>
                      </div>
                    </div>
                  </el-tab-pane>

                  <!-- 渠道 -->
                  <el-tab-pane label="渠道" name="channel">
                    <div class="permission-content">
                      <div class="permission-group">
                        <div class="checkbox-group">
                          <el-checkbox
                            :model-value="hasPermission(PermissionModule.CHANNEL, PermissionAction.VIEW_CHANNELS)"
                            disabled
                          >
                            查看渠道
                          </el-checkbox>
                          <el-checkbox
                            :model-value="hasPermission(PermissionModule.CHANNEL, PermissionAction.MANAGE_CHANNELS)"
                            disabled
                          >
                            管理渠道
                          </el-checkbox>
                        </div>
                      </div>
                    </div>
                  </el-tab-pane>

                  <!-- 统计分析 -->
                  <el-tab-pane label="统计分析" name="statistics">
                    <div class="permission-content">
                      <div class="permission-group">
                        <div class="checkbox-group">
                          <el-checkbox
                            :model-value="hasPermission(PermissionModule.STATISTICS, PermissionAction.VIEW_STATS)"
                            disabled
                          >
                            查看统计
                          </el-checkbox>
                          <el-checkbox
                            :model-value="hasPermission(PermissionModule.STATISTICS, PermissionAction.EXPORT_STATS)"
                            disabled
                          >
                            导出统计
                          </el-checkbox>
                        </div>
                      </div>
                    </div>
                  </el-tab-pane>

                  <!-- 设置 -->
                  <el-tab-pane label="设置" name="settings">
                    <div class="permission-content">
                      <div class="permission-group">
                        <div class="checkbox-group">
                          <el-checkbox
                            :model-value="hasPermission(PermissionModule.SETTINGS, PermissionAction.VIEW_SETTINGS)"
                            disabled
                          >
                            查看设置
                          </el-checkbox>
                          <el-checkbox
                            :model-value="hasPermission(PermissionModule.SETTINGS, PermissionAction.MODIFY_SETTINGS)"
                            disabled
                          >
                            修改设置
                          </el-checkbox>
                          <el-checkbox
                            :model-value="hasPermission(PermissionModule.SETTINGS, PermissionAction.MODIFY_STORE_SETTINGS)"
                            disabled
                          >
                            修改门店设置
                          </el-checkbox>
                          <el-checkbox
                            :model-value="hasPermission(PermissionModule.SETTINGS, PermissionAction.MANAGE_EMPLOYEE_ACCOUNTS)"
                            disabled
                          >
                            管理员工账号
                          </el-checkbox>
                          <el-checkbox
                            :model-value="hasPermission(PermissionModule.SETTINGS, PermissionAction.MANAGE_PAYMENT_METHODS)"
                            disabled
                          >
                            管理收款方式
                          </el-checkbox>
                        </div>
                      </div>
                    </div>
                  </el-tab-pane>

                  <!-- 数据中心 -->
                  <el-tab-pane label="数据中心" name="data-center">
                    <div class="permission-content">
                      <div class="permission-group">
                        <div class="checkbox-group">
                          <el-checkbox
                            :model-value="hasPermission(PermissionModule.DATA_CENTER, PermissionAction.VIEW_DATA)"
                            disabled
                          >
                            查看数据
                          </el-checkbox>
                          <el-checkbox
                            :model-value="hasPermission(PermissionModule.DATA_CENTER, PermissionAction.EXPORT_DATA)"
                            disabled
                          >
                            导出数据
                          </el-checkbox>
                        </div>
                      </div>
                    </div>
                  </el-tab-pane>
                </el-tabs>
              </div>
            </div>
          </el-tab-pane>
        </el-tabs>

        <!-- 底部按钮 -->
        <div class="drawer-footer">
          <el-button @click="handleCancelAdd" size="large">取消</el-button>
          <el-button type="primary" @click="handleConfirmAdd" size="large">完成</el-button>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { Search, ArrowUp, ArrowDown } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getStoreMembers,
  addStoreMember,
  getStoreMemberDetail,
  updateStoreMemberPermission,
  removeStoreMember,
  type StoreMember,
  type AddStoreMemberRequest,
} from '@/api/store'
import { searchUsers, type UserDTO } from '@/api/auth'
import {
  getAllRoles,
  getRolePermissions,
  type RoleDTO,
  type PermissionDTO,
  PermissionModule,
  PermissionAction
} from '@/api/role'
import { useStoreStore } from '@/stores/store'

const router = useRouter()
const storeStore = useStoreStore()

interface Account {
  id: number
  storeUserId?: number // StoreUser的ID，用于删除和更新
  email: string
  name: string
  role: string
  isActive: boolean
  roles?: RoleDTO[]
}

const searchKeyword = ref('')
const selectedRole = ref<number | ''>('')
const selectedStatus = ref<boolean | ''>('')
const isCollapsed = ref(false)
const selectAll = ref(false)
const currentPage = ref(1)
const pageSize = ref(25)
const loading = ref(false)
const roleOptions = ref<RoleDTO[]>([])

// 添加账号相关
const addDrawerVisible = ref(false)
const activeTab = ref('room-status')
const activePermissionTab = ref('business')

// 调整角色相关
const roleDialogVisible = ref(false)
const selectedRoles = ref({
  role11: false,
  cleaner: false,
  soPms: false,
  admin: false,
})

// 账号表单
const accountForm = ref<{
  id?: number
  email: string
  name: string
}>({
  email: '',
  name: '',
})

// 当前账号选择的角色(用于设置权限)
const selectedAccountRoles = ref<number[]>([])

// 存储选中角色的所有权限数据
const rolePermissionsMap = ref<Map<number, PermissionDTO[]>>(new Map())

// 监听选中角色变化,加载对应的权限
watch(selectedAccountRoles, async (newRoleIds) => {
  // 清空旧的权限数据
  rolePermissionsMap.value.clear()

  if (!newRoleIds || newRoleIds.length === 0) {
    return
  }

  // 加载每个角色的权限
  for (const roleId of newRoleIds) {
    try {
      const response = await getRolePermissions(roleId)
      if (response.success && response.data) {
        rolePermissionsMap.value.set(roleId, response.data)
      }
    } catch (error) {
      console.error(`加载角色 ${roleId} 的权限失败:`, error)
    }
  }
})

// 计算汇总的权限(用于显示)
const aggregatedPermissions = computed(() => {
  const allPermissions: PermissionDTO[] = []
  rolePermissionsMap.value.forEach(permissions => {
    allPermissions.push(...permissions)
  })
  return allPermissions
})

// 检查是否拥有指定权限
const hasPermission = (module: PermissionModule, action: PermissionAction): boolean => {
  return aggregatedPermissions.value.some(
    p => p.module === module && p.action === action
  )
}

// 账号数据
const accounts = ref<Account[]>([])

const totalCount = computed(() => accounts.value.length)

const filteredAccounts = computed(() => {
  return accounts.value
})

// 加载账号列表
const loadAccounts = async () => {
  // 检查当前门店
  if (!storeStore.currentStore?.id) {
    ElMessage.warning('请先选择门店')
    accounts.value = []
    return
  }

  try {
    loading.value = true
    // 使用门店成员API获取当前门店的成员列表
    const response = await getStoreMembers(storeStore.currentStore.id)
    if (response.success && response.data) {
      // 转换门店成员数据为账号格式
      accounts.value = response.data.map((member: StoreMember) => ({
        id: member.user.id,
        storeUserId: member.id, // 保存StoreUser的ID，用于删除和更新
        email: member.user.email,
        name: member.user.nickname || member.user.email,
        role: member.role,
        isActive: member.isActive,
        roles: member.roles || [], // 使用返回的权限角色列表
      }))

      // 应用本地筛选
      if (searchKeyword.value) {
        const keyword = searchKeyword.value.toLowerCase()
        accounts.value = accounts.value.filter(
          (account) =>
            (account.email?.toLowerCase().includes(keyword) || false) ||
            (account.name?.toLowerCase().includes(keyword) || false)
        )
      }
      if (selectedRole.value !== '') {
        accounts.value = accounts.value.filter((account) => account.role === selectedRole.value)
      }
      if (selectedStatus.value !== '') {
        accounts.value = accounts.value.filter((account) => account.isActive === selectedStatus.value)
      }
    } else {
      ElMessage.error(response.message || '加载账号列表失败')
    }
  } catch (error) {
    console.error('加载账号列表失败:', error)
    ElMessage.error('加载账号列表失败')
  } finally {
    loading.value = false
  }
}

// 加载角色列表
const loadRoles = async () => {
  try {
    const response = await getAllRoles()
    if (response.success && response.data) {
      roleOptions.value = response.data
    }
  } catch (error) {
    console.error('加载角色列表失败:', error)
  }
}

// 初始化
onMounted(() => {
  loadAccounts()
  loadRoles()
})

const toggleCollapse = () => {
  isCollapsed.value = !isCollapsed.value
}

const handleRoleManagement = () => {
  router.push('/settings/account/role-management')
}

const handleAdd = () => {
  // 重置表单
  accountForm.value = {
    email: '',
    name: '',
  }
  selectedAccountRoles.value = []
  addDrawerVisible.value = true
}

// 取消添加
const handleCancelAdd = () => {
  addDrawerVisible.value = false
  // 重置表单
  accountForm.value = {
    email: '',
    name: '',
  }
}

// 确认添加
const handleConfirmAdd = async () => {
  if (!accountForm.value.email) {
    ElMessage.warning('请输入邮箱地址')
    return
  }
  if (!storeStore.currentStore?.id) {
    ElMessage.warning('请先选择门店')
    return
  }

  try {
    let response

    if (accountForm.value.id) {
      // 更新成员权限
      response = await updateStoreMemberPermission(storeStore.currentStore.id, accountForm.value.id, {
        roleIds: selectedAccountRoles.value,
      })
      if (response.success) {
        ElMessage.success('权限更新成功')
      } else {
        ElMessage.error(response.message || '更新权限失败')
        return
      }
    } else {
      // 添加新成员
      const addRequest: AddStoreMemberRequest = {
        email: accountForm.value.email,
        role: 'member', // 默认为普通成员
        roleIds: selectedAccountRoles.value,
      }
      response = await addStoreMember(storeStore.currentStore.id, addRequest)
      if (response.success) {
        ElMessage.success('成员添加成功')
      } else {
        ElMessage.error(response.message || '添加成员失败')
        return
      }
    }

    // 成功后关闭抽屉并重置
    addDrawerVisible.value = false
    accountForm.value = {
      email: '',
      name: '',
    }
    selectedAccountRoles.value = []
    loadAccounts()
  } catch (error: any) {
    console.error('操作失败:', error)
    if (error.response?.data?.message) {
      ElMessage.error(error.response.data.message)
    } else {
      ElMessage.error('操作失败')
    }
  }
}

const handleDetail = (row: Account) => {
  ElMessage.info(`查看账号详情: ${row.name}`)
}

const handleSetPermission = async (row: Account) => {
  if (!storeStore.currentStore?.id) {
    ElMessage.warning('请先选择门店')
    return
  }

  try {
    // 加载账号信息到表单
    accountForm.value = {
      id: row.id,
      email: row.email,
      name: row.name,
    }

    // 从API加载该成员的详细信息(包含角色)
    const response = await getStoreMemberDetail(storeStore.currentStore.id, row.id)
    if (response.success && response.data) {
      // 提取成员的权限角色ID列表
      selectedAccountRoles.value = response.data.roles.map(role => role.id)
    }

    // 打开抽屉
    addDrawerVisible.value = true
  } catch (error) {
    console.error('加载成员信息失败:', error)
    ElMessage.error('加载成员信息失败')
  }
}

const handleDelete = async (row: Account) => {
  if (!storeStore.currentStore?.id) {
    ElMessage.warning('请先选择门店')
    return
  }

  try {
    await ElMessageBox.confirm(`确定要将 "${row.name}" 从门店中移除吗?`, '移除确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })

    // 使用storeUserId作为成员ID进行删除
    const memberId = row.storeUserId || row.id
    const response = await removeStoreMember(storeStore.currentStore.id, memberId)
    if (response.success) {
      ElMessage.success('移除成功')
      loadAccounts()
    } else {
      ElMessage.error(response.message || '移除失败')
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('移除成员失败:', error)
      ElMessage.error('移除成员失败')
    }
  }
}

const handleStatusChange = async (row: Account) => {
  if (!storeStore.currentStore?.id) {
    ElMessage.warning('请先选择门店')
    return
  }

  try {
    const response = await updateStoreMemberPermission(storeStore.currentStore.id, row.id, {
      isActive: row.isActive,
    })
    if (response.success) {
      ElMessage.success(`已${row.isActive ? '启用' : '停用'}成员: ${row.name}`)
    } else {
      ElMessage.error(response.message || '更新状态失败')
      // 恢复原状态
      row.isActive = !row.isActive
    }
  } catch (error) {
    console.error('更新成员状态失败:', error)
    ElMessage.error('更新成员状态失败')
    // 恢复原状态
    row.isActive = !row.isActive
  }
}

const handleSelectAll = (checked: boolean) => {
  // 实现全选逻辑
}

const handleBatchEnable = () => {
  ElMessage.info('批量启用')
}

const handleBatchDisable = () => {
  ElMessage.info('批量停用')
}

const handleBatchChangeRole = () => {
  // 打开调整角色对话框
  roleDialogVisible.value = true
}

const handleConfirmRoleChange = () => {
  // TODO: 调用API批量更新角色
  ElMessage.success('角色调整成功')
  roleDialogVisible.value = false
  // 重置选择
  selectedRoles.value = {
    role11: false,
    cleaner: false,
    soPms: false,
    admin: false,
  }
}

const handleBatchDelete = () => {
  ElMessage.info('批量删除')
}

const handleSizeChange = (size: number) => {
  pageSize.value = size
}

const handleCurrentChange = (page: number) => {
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

.filter-row {
  display: flex;
  gap: 20px;
  flex-wrap: wrap;
}

.filter-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.filter-label {
  font-size: 14px;
  color: #606266;
  white-space: nowrap;
}

.action-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.action-left {
  display: flex;
  gap: 8px;
  align-items: center;
}

.batch-action-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #ebeef5;
}

.batch-action-left {
  display: flex;
  gap: 12px;
  align-items: center;
}

.selected-text {
  font-size: 14px;
  color: #606266;
}

:deep(.el-table) {
  font-size: 14px;
}

:deep(.el-table th) {
  background-color: #f5f7fa;
  font-weight: 600;
}

:deep(.el-switch__label) {
  margin-left: 8px;
}

/* 添加账号抽屉样式 */
.add-account-content {
  padding: 20px;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.account-tabs {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.account-tabs :deep(.el-tabs__content) {
  flex: 1;
  overflow-y: auto;
}

.tab-content {
  padding: 20px 0;
}

.section {
  margin-bottom: 40px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 20px;
  padding-bottom: 12px;
  border-bottom: 1px solid #ebeef5;
}

.account-form {
  margin-top: 20px;
}

.permission-tabs {
  margin-top: 20px;
}

.permission-content {
  padding: 20px 0;
}

.permission-group {
  margin-bottom: 30px;
  padding: 20px;
  background-color: #f5f7fa;
  border-radius: 8px;
}

.permission-note {
  font-size: 12px;
  color: #909399;
  margin: 8px 0 16px 24px;
}

.group-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 16px;
}

.checkbox-group {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 12px;
  margin-left: 24px;
}

.drawer-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 20px 0;
  border-top: 1px solid #ebeef5;
  margin-top: auto;
}

:deep(.el-drawer__header) {
  margin-bottom: 0;
  padding: 20px;
  border-bottom: 1px solid #ebeef5;
}

:deep(.el-drawer__body) {
  padding: 0;
}

.role-permission-section {
  margin-bottom: 24px;
}

.subsection-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 16px 0;
}

.role-checkboxes {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  padding: 16px;
  background-color: #f5f7fa;
  border-radius: 4px;
}
</style>
