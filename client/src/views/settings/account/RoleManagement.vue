<template>
  <div class="role-management-container">
    <div class="layout-wrapper">
      <!-- 左侧角色列表 -->
      <aside class="roles-sidebar">
        <div class="sidebar-header">
          <el-input
            v-model="roleSearchKeyword"
            placeholder="搜索角色名称"
            clearable
          >
            <template #suffix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </div>

        <el-button
          type="primary"
          :icon="CirclePlus"
          style="width: 100%; margin: 12px 0"
          @click="handleAddRole"
        >
          新增
        </el-button>

        <div class="roles-list">
          <div
            v-for="role in filteredRoles"
            :key="role.id"
            :class="['role-item', { active: selectedRole?.id === role.id }]"
            @click="handleSelectRole(role)"
          >
            <span class="role-name">{{ role.name }}</span>
            <div class="role-actions">
              <el-button
                link
                :icon="Edit"
                size="small"
                @click.stop="handleEditRole(role)"
              />
              <el-button
                link
                :icon="Delete"
                size="small"
                type="danger"
                @click.stop="handleDeleteRole(role)"
              />
            </div>
          </div>
        </div>
      </aside>

      <!-- 右侧权限设置 -->
      <main v-if="selectedRole" class="permissions-main">
        <div class="permissions-header">
          <h2 class="role-title">{{ selectedRole.name }}</h2>
          <el-button v-if="!isEditing" type="primary" @click="handleEditMode">编辑</el-button>
          <el-button v-else type="primary" @click="handleSavePermissions" :loading="saving">确定</el-button>
        </div>

        <el-tabs v-model="activeTab">
          <el-tab-pane label="住宿管理" name="accommodation">
            <div class="permission-section">
              <h3 class="section-title">房型权限</h3>
              <div class="permission-toggle">
                <el-switch
                  v-model="permissions.roomTypeAll"
                  :disabled="!isEditing || !permissions.viewRoomStatus"
                />
                <span class="toggle-label">给该员工所有房型权限</span>
                <el-icon class="help-icon"><QuestionFilled /></el-icon>
              </div>
              <div v-if="!permissions.roomTypeAll" class="permission-checkboxes">
                <el-checkbox
                  v-for="roomType in roomTypes"
                  :key="roomType.id"
                  v-model="roomType.checked"
                  :label="roomType.name"
                  :disabled="!isEditing || !permissions.viewRoomStatus"
                />
              </div>
            </div>

            <div class="permission-section">
              <h3 class="section-title">房态管理</h3>
              <div class="permission-checkboxes">
                <el-checkbox v-model="permissions.viewRoomStatus" label="查看房态" :disabled="!isEditing" />
                <el-checkbox v-model="permissions.editRoomStatus" label="修改房态" :disabled="!isEditing" />
                <!-- <el-checkbox v-model="permissions.viewRoomOperationLog" label="查看房态操作日志（暂未接入）" :disabled="true" /> -->
                <el-checkbox v-model="permissions.viewRoomInfo" label="查看房情表" :disabled="!isEditing" />
              </div>
            </div>

            <div class="permission-section">
              <h3 class="section-title">房价管理</h3>
              <div class="permission-checkboxes">
                <el-checkbox v-model="permissions.viewRoomPrice" label="查看房价" :disabled="!isEditing" />
                <el-checkbox v-model="permissions.editRoomPrice" label="修改房价" :disabled="!isEditing" />
                <el-checkbox v-model="permissions.viewPriceLog" label="查看改价记录" :disabled="!isEditing" />
                <el-checkbox v-model="permissions.batchChangePrice" label="批量改价" :disabled="!isEditing" />
              </div>
            </div>

            <div class="permission-section">
              <h3 class="section-title">保洁管理</h3>
              <div class="permission-checkboxes">
                <el-checkbox v-model="permissions.taskList" label="查看保洁任务" :disabled="!isEditing" />
              </div>
            </div>
          </el-tab-pane>

          <el-tab-pane label="订单管理" name="order">
            <div class="permission-section">
              <div class="permission-checkboxes">
                <el-checkbox v-model="permissions.viewOrders" label="查看订单" :disabled="!isEditing" />
                <el-checkbox v-model="permissions.modifyOrder" label="修改订单" :disabled="!isEditing" />
                <el-checkbox v-model="permissions.cancelOrder" label="取消订单" :disabled="!isEditing" />
              </div>
            </div>
          </el-tab-pane>

          <el-tab-pane label="渠道" name="channel">
            <div class="permission-section">
              <div class="permission-checkboxes">
                <el-checkbox v-model="permissions.viewChannels" label="查看渠道" :disabled="!isEditing" />
                <el-checkbox v-model="permissions.manageChannels" label="管理渠道" :disabled="!isEditing" />
              </div>
            </div>
          </el-tab-pane>

          <el-tab-pane label="统计分析" name="statistics">
            <div class="permission-section">
              <div class="permission-checkboxes">
                <el-checkbox v-model="permissions.viewStats" label="查看统计数据" :disabled="!isEditing" />
                <!-- <el-checkbox v-model="permissions.exportStats" label="导出报表（暂未接入）" :disabled="true" /> -->
              </div>
            </div>
          </el-tab-pane>

          <el-tab-pane label="设置" name="settings">
            <div class="permission-section">
              <div class="permission-checkboxes">
                <el-checkbox v-model="permissions.modifyStoreSettings" label="修改门店设置" :disabled="!isEditing" />
                <el-checkbox v-model="permissions.manageEmployeeAccounts" label="管理员工账号" :disabled="!isEditing" />
                <!-- <el-checkbox v-model="permissions.managePaymentMethods" label="管理支付方式（暂未接入）" :disabled="true" /> -->
              </div>
            </div>
          </el-tab-pane>

          <el-tab-pane label="敏感权限" name="sensitive">
            <div class="permission-section">
              <div class="permission-checkboxes">
                <el-checkbox v-model="permissions.viewFinancialData" label="查看财务数据" :disabled="!isEditing" />
                <el-checkbox v-model="permissions.deleteImportantData" label="删除重要数据" :disabled="!isEditing" />
              </div>
            </div>
          </el-tab-pane>
        </el-tabs>
      </main>

      <div v-else class="empty-state">
        <el-empty description="请选择一个角色查看权限设置" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import {
  Search,
  CirclePlus,
  Edit,
  Delete,
  QuestionFilled,
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getAllRoles,
  createRole,
  updateRole,
  deleteRole,
  getRolePermissions,
  updateRolePermissions,
  type RoleDTO,
  type PermissionDTO,
  type CreateRoleRequest,
  type UpdateRoleRequest,
  PermissionModule,
  PermissionAction,
} from '@/api/role'
import { getAllRoomTypes, type RoomTypeDTO } from '@/api/roomType'
import { useStoreStore } from '@/stores/store'

interface Role {
  id: number
  name: string
}

interface RoomType extends RoomTypeDTO {
  checked: boolean
}

interface Permissions {
  roomTypeAll: boolean
  viewRoomStatus: boolean
  editRoomStatus: boolean
  viewRoomOperationLog: boolean
  viewRoomInfo: boolean
  viewRoomPrice: boolean
  editRoomPrice: boolean
  viewPriceLog: boolean
  batchChangePrice: boolean
  taskList: boolean
  viewOrders: boolean
  modifyOrder: boolean
  cancelOrder: boolean
  viewChannels: boolean
  manageChannels: boolean
  viewStats: boolean
  exportStats: boolean
  modifyStoreSettings: boolean
  manageEmployeeAccounts: boolean
  managePaymentMethods: boolean
  viewFinancialData: boolean
  deleteImportantData: boolean
}

const roleSearchKeyword = ref('')
const selectedRole = ref<Role | null>(null)
const activeTab = ref('accommodation')
const loading = ref(false)
const isEditing = ref(false)
const saving = ref(false)
const storeStore = useStoreStore()
const currentStoreId = computed(() => storeStore.currentStore?.id ?? null)

const roles = ref<Role[]>([])

// 加载角色列表
const loadRoles = async () => {
  try {
    loading.value = true
    const response = await getAllRoles(roleSearchKeyword.value)
    if (response.success && response.data) {
      roles.value = response.data.map((dto: RoleDTO) => ({
        id: dto.id,
        name: dto.name,
      }))
    } else {
      ElMessage.error(response.message || '加载角色列表失败')
    }
  } catch (error) {
    console.error('加载角色列表失败:', error)
    ElMessage.error('加载角色列表失败')
  } finally {
    loading.value = false
  }
}

// 加载房型列表
const loadRoomTypes = async () => {
  if (!currentStoreId.value) {
    roomTypes.value = []
    return
  }
  try {
    const response = await getAllRoomTypes()
    if (response.success && response.data) {
      roomTypes.value = response.data.map(rt => ({
        ...rt,
        checked: false
      }))
    } else {
      ElMessage.error(response.message || '加载房型列表失败')
    }
  } catch (error) {
    console.error('加载房型列表失败:', error)
    ElMessage.error('加载房型列表失败')
  }
}

// 初始化
onMounted(() => {
  loadRoles()
})

const roomTypes = ref<RoomType[]>([])

watch(currentStoreId, async (storeId) => {
  if (!storeId) {
    roomTypes.value = []
    return
  }
  await loadRoomTypes()
  if (selectedRole.value) {
    await loadRolePermissions(selectedRole.value.id)
  }
}, { immediate: true })

const permissions = ref<Permissions>({
  roomTypeAll: false,
  viewRoomStatus: false,
  editRoomStatus: false,
  viewRoomOperationLog: false,
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
  exportStats: false,
  modifyStoreSettings: false,
  manageEmployeeAccounts: false,
  managePaymentMethods: false,
  viewFinancialData: false,
  deleteImportantData: false,
})

const filteredRoles = computed(() => {
  if (!roleSearchKeyword.value) {
    return roles.value
  }
  return roles.value.filter(role =>
    role.name.toLowerCase().includes(roleSearchKeyword.value.toLowerCase())
  )
})

const handleAddRole = async () => {
  try {
    const { value } = await ElMessageBox.prompt('请输入角色名称', '新增角色', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
    })

    if (value) {
      const request: CreateRoleRequest = { name: value }
      const response = await createRole(request)
      if (response.success) {
        ElMessage.success('添加成功')
        loadRoles()
      } else {
        ElMessage.error(response.message || '创建角色失败')
      }
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('创建角色失败:', error)
      ElMessage.error('创建角色失败')
    }
  }
}

// 加载角色权限并回显
const loadRolePermissions = async (roleId: number) => {
  try {
    loading.value = true
    const response = await getRolePermissions(roleId)

    if (response.success && response.data) {
      const permissionList: PermissionDTO[] = response.data

      // 重置所有权限
      permissions.value = {
        roomTypeAll: false,
        viewRoomStatus: false,
        editRoomStatus: false,
        viewRoomOperationLog: false,
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
        exportStats: false,
        modifyStoreSettings: false,
        manageEmployeeAccounts: false,
        managePaymentMethods: false,
        viewFinancialData: false,
        deleteImportantData: false,
      }

      // 重置房型选择
      roomTypes.value.forEach(rt => {
        rt.checked = false
      })

      // 房态查看权限（同时承载房型范围）
      const viewRoomStatusPermissions = permissionList.filter(
        p => p.module === PermissionModule.ACCOMMODATION && p.action === PermissionAction.VIEW_ROOM_STATUS
      )
      if (viewRoomStatusPermissions.length > 0) {
        permissions.value.viewRoomStatus = true
        const hasAllRoomTypes = viewRoomStatusPermissions.some(p => p.allRoomTypes)
        if (hasAllRoomTypes) {
          permissions.value.roomTypeAll = true
        } else {
          viewRoomStatusPermissions.forEach(p => {
            if (p.roomTypeId && p.roomTypeId > 0) {
              const roomType = roomTypes.value.find(rt => rt.id === p.roomTypeId)
              if (roomType) {
                roomType.checked = true
              }
            }
          })
        }
      }

      // 其它权限回显
      permissionList.forEach(permission => {
        switch (permission.action) {
          case 'VIEW_ORDERS':
            permissions.value.viewOrders = true
            break
          case 'MODIFY_ORDER':
            permissions.value.modifyOrder = true
            break
          case 'CANCEL_ORDER':
            permissions.value.cancelOrder = true
            break
          case 'VIEW_CHANNELS':
            permissions.value.viewChannels = true
            break
          case 'MANAGE_CHANNELS':
            permissions.value.manageChannels = true
            break
          case 'VIEW_STATS':
            permissions.value.viewStats = true
            break
          case 'MODIFY_STORE_SETTINGS':
            permissions.value.modifyStoreSettings = true
            break
          case 'MANAGE_EMPLOYEE_ACCOUNTS':
            permissions.value.manageEmployeeAccounts = true
            break
          case 'VIEW_FINANCIAL_DATA':
            permissions.value.viewFinancialData = true
            break
          case 'DELETE_IMPORTANT_DATA':
            permissions.value.deleteImportantData = true
            break
          case 'EDIT_ROOM_STATUS':
            permissions.value.editRoomStatus = true
            break
          case 'VIEW_ROOM_INFO':
            permissions.value.viewRoomInfo = true
            break
          case 'VIEW_ROOM_PRICE':
            permissions.value.viewRoomPrice = true
            break
          case 'EDIT_ROOM_PRICE':
            permissions.value.editRoomPrice = true
            break
          case 'VIEW_PRICE_LOG':
            permissions.value.viewPriceLog = true
            break
          case 'BATCH_CHANGE_PRICE':
            permissions.value.batchChangePrice = true
            break
          case 'TASK_LIST':
            permissions.value.taskList = true
            break
        }
      })
    } else {
      ElMessage.error(response.message || '加载权限失败')
    }
  } catch (error) {
    console.error('加载权限失败:', error)
    ElMessage.error('加载权限失败')
  } finally {
    loading.value = false
  }
}

const handleSelectRole = async (role: Role) => {
  selectedRole.value = role
  isEditing.value = false
  // 加载该角色的权限
  await loadRolePermissions(role.id)
}

const handleEditRole = async (role: Role) => {
  try {
    const { value } = await ElMessageBox.prompt('请输入新的角色名称', '编辑角色', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputValue: role.name,
    })

    if (value) {
      const request: UpdateRoleRequest = { name: value }
      const response = await updateRole(role.id, request)
      if (response.success) {
        ElMessage.success('修改成功')
        loadRoles()
        // 如果是当前选中的角色,更新显示
        if (selectedRole.value?.id === role.id) {
          selectedRole.value.name = value
        }
      } else {
        ElMessage.error(response.message || '更新角色失败')
      }
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('更新角色失败:', error)
      ElMessage.error('更新角色失败')
    }
  }
}

const handleDeleteRole = async (role: Role) => {
  try {
    await ElMessageBox.confirm(
      '删除后，正在使用该角色的员工将同步删除该角色，员工权限将更新。',
      '确定要删除该角色吗？',
      {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
    })

    const response = await deleteRole(role.id)
    if (response.success) {
      ElMessage.success('删除成功')
      if (selectedRole.value?.id === role.id) {
        selectedRole.value = null
      }
      loadRoles()
    } else {
      ElMessage.error(response.message || '删除角色失败')
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('删除角色失败:', error)
      ElMessage.error('删除角色失败')
    }
  }
}

const handleEditMode = () => {
  isEditing.value = true
}

const handleSavePermissions = async () => {
  if (!selectedRole.value) return

  try {
    saving.value = true

    // 构建权限数据
    const permissionDTOs: PermissionDTO[] = []

    // 房态查看权限（同时配置房型范围）
    if (permissions.value.viewRoomStatus) {
      if (permissions.value.roomTypeAll) {
        permissionDTOs.push({
          module: PermissionModule.ACCOMMODATION,
          action: PermissionAction.VIEW_ROOM_STATUS,
          allRoomTypes: true,
        })
      } else {
        const selectedRoomTypes = roomTypes.value.filter(rt => rt.checked)
        if (selectedRoomTypes.length === 0) {
          ElMessage.warning('请至少选择一个房型，或开启“所有房型权限”')
          return
        }
        selectedRoomTypes.forEach(rt => {
          permissionDTOs.push({
            module: PermissionModule.ACCOMMODATION,
            action: PermissionAction.VIEW_ROOM_STATUS,
            roomTypeId: rt.id,
          })
        })
      }
    }

    // 房态管理其它权限
    if (permissions.value.editRoomStatus) {
      permissionDTOs.push({ module: PermissionModule.ACCOMMODATION, action: PermissionAction.EDIT_ROOM_STATUS })
    }
    if (permissions.value.viewRoomInfo) {
      permissionDTOs.push({ module: PermissionModule.ACCOMMODATION, action: PermissionAction.VIEW_ROOM_INFO })
    }

    // 房价管理权限
    if (permissions.value.viewRoomPrice) {
      permissionDTOs.push({ module: PermissionModule.ACCOMMODATION, action: PermissionAction.VIEW_ROOM_PRICE })
    }
    if (permissions.value.editRoomPrice) {
      permissionDTOs.push({ module: PermissionModule.ACCOMMODATION, action: PermissionAction.EDIT_ROOM_PRICE })
    }
    if (permissions.value.viewPriceLog) {
      permissionDTOs.push({ module: PermissionModule.ACCOMMODATION, action: PermissionAction.VIEW_PRICE_LOG })
    }
    if (permissions.value.batchChangePrice) {
      permissionDTOs.push({ module: PermissionModule.ACCOMMODATION, action: PermissionAction.BATCH_CHANGE_PRICE })
    }

    // 保洁管理权限
    if (permissions.value.taskList) {
      permissionDTOs.push({ module: PermissionModule.ACCOMMODATION, action: PermissionAction.TASK_LIST })
    }

    // 订单管理
    if (permissions.value.viewOrders) {
      permissionDTOs.push({ module: PermissionModule.ORDER, action: PermissionAction.VIEW_ORDERS })
    }
    if (permissions.value.modifyOrder) {
      permissionDTOs.push({ module: PermissionModule.ORDER, action: PermissionAction.MODIFY_ORDER })
    }
    if (permissions.value.cancelOrder) {
      permissionDTOs.push({ module: PermissionModule.ORDER, action: PermissionAction.CANCEL_ORDER })
    }

    // 渠道
    if (permissions.value.viewChannels) {
      permissionDTOs.push({ module: PermissionModule.CHANNEL, action: PermissionAction.VIEW_CHANNELS })
    }
    if (permissions.value.manageChannels) {
      permissionDTOs.push({ module: PermissionModule.CHANNEL, action: PermissionAction.MANAGE_CHANNELS })
    }

    // 统计分析
    if (permissions.value.viewStats) {
      permissionDTOs.push({ module: PermissionModule.STATISTICS, action: PermissionAction.VIEW_STATS })
    }

    // 设置
    if (permissions.value.modifyStoreSettings) {
      permissionDTOs.push({ module: PermissionModule.SETTINGS, action: PermissionAction.MODIFY_STORE_SETTINGS })
    }
    if (permissions.value.manageEmployeeAccounts) {
      permissionDTOs.push({ module: PermissionModule.SETTINGS, action: PermissionAction.MANAGE_EMPLOYEE_ACCOUNTS })
    }

    // 敏感权限
    if (permissions.value.viewFinancialData) {
      permissionDTOs.push({ module: PermissionModule.SENSITIVE, action: PermissionAction.VIEW_FINANCIAL_DATA })
    }
    if (permissions.value.deleteImportantData) {
      permissionDTOs.push({ module: PermissionModule.SENSITIVE, action: PermissionAction.DELETE_IMPORTANT_DATA })
    }

    // 调用 API 保存权限
    const response = await updateRolePermissions(selectedRole.value.id, permissionDTOs)
    if (response.success) {
      ElMessage.success('权限保存成功')
      isEditing.value = false
      // 重新加载权限以显示最新保存的数据
      await loadRolePermissions(selectedRole.value.id)
    } else {
      ElMessage.error(response.message || '保存权限失败')
    }
  } catch (error) {
    console.error('保存权限失败:', error)
    ElMessage.error('保存权限失败')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.role-management-container {
  padding: 20px;
  background: #fff;
  min-height: calc(100vh - 100px);
}

.layout-wrapper {
  display: flex;
  gap: 24px;
  height: calc(100vh - 140px);
}

/* 左侧角色列表 */
.roles-sidebar {
  width: 240px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
}

.sidebar-header {
  margin-bottom: 12px;
}

.roles-list {
  flex: 1;
  overflow-y: auto;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
}

.role-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #ebeef5;
  cursor: pointer;
  transition: background-color 0.2s;
}

.role-item:hover {
  background-color: #f5f7fa;
}

.role-item.active {
  background-color: #ecf5ff;
  color: #409eff;
}

.role-item:last-child {
  border-bottom: none;
}

.role-name {
  font-size: 14px;
}

.role-actions {
  display: flex;
  gap: 4px;
  opacity: 0;
  transition: opacity 0.2s;
}

.role-item:hover .role-actions {
  opacity: 1;
}

/* 右侧权限设置 */
.permissions-main {
  flex: 1;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  padding: 24px;
  overflow-y: auto;
}

.permissions-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 2px solid #ebeef5;
}

.role-title {
  font-size: 20px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

.permission-section {
  margin-bottom: 32px;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: #606266;
  margin: 0 0 16px 0;
  padding-left: 8px;
  border-left: 3px solid #409eff;
}

.permission-toggle {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
}

.toggle-label {
  font-size: 14px;
  color: #303133;
}

.help-icon {
  color: #909399;
  cursor: help;
}

.permission-checkboxes {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 16px;
  padding: 12px;
  background: #f5f7fa;
  border-radius: 4px;
}

:deep(.el-checkbox) {
  margin-right: 0;
}

.empty-state {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

:deep(.el-tabs__nav-wrap::after) {
  display: none;
}

:deep(.el-tabs__header) {
  margin-bottom: 24px;
}
</style>
