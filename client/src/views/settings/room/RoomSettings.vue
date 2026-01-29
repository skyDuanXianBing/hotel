<template>
  <div class="room-settings-container">
    <!-- 顶部信息栏 -->
    <div class="header-info">
      <div class="system-period">
        <el-icon><InfoFilled /></el-icon>
        <span>当前系统周日期: {{ systemPeriod }}</span>
      </div>
      <div class="header-actions">
        <el-button @click="handleRoomOwnership">房间归属</el-button>
        <el-button @click="handleImport">导入房型</el-button>
        <el-button type="primary" @click="handleAdd">新增</el-button>
      </div>
    </div>

    <!-- 数据表格 -->
    <div class="table-wrapper">
      <el-table
        v-loading="loading"
        :data="paginatedData"
        border
        stripe
        class="room-settings-table"
        element-loading-text="加载中..."
      >
        <el-table-column prop="name" label="房型名称" min-width="150" fixed />
        <el-table-column prop="shortName" label="简称" min-width="120" />

        <!-- 默认价格标题 -->
        <el-table-column align="center" label="默认价格">
          <el-table-column prop="mondayPrice" label="一" min-width="100" align="center">
            <template #default="{ row }">
              <span>¥{{ formatPrice(row.mondayPrice) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="tuesdayPrice" label="二" min-width="100" align="center">
            <template #default="{ row }">
              <span>¥{{ formatPrice(row.tuesdayPrice) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="wednesdayPrice" label="三" min-width="100" align="center">
            <template #default="{ row }">
              <span>¥{{ formatPrice(row.wednesdayPrice) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="thursdayPrice" label="四" min-width="100" align="center">
            <template #default="{ row }">
              <span>¥{{ formatPrice(row.thursdayPrice) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="fridayPrice" label="五" min-width="100" align="center">
            <template #default="{ row }">
              <span>¥{{ formatPrice(row.fridayPrice) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="saturdayPrice" label="六" min-width="100" align="center">
            <template #default="{ row }">
              <span>¥{{ formatPrice(row.saturdayPrice) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="sundayPrice" label="日" min-width="100" align="center">
            <template #default="{ row }">
              <span>¥{{ formatPrice(row.sundayPrice) }}</span>
            </template>
          </el-table-column>
        </el-table-column>

        <el-table-column prop="roomCount" label="房间数" min-width="100" align="center" />
        <el-table-column prop="roomNumbers" label="房间号" min-width="150">
          <template #default="{ row }">
            {{ (row.roomNumbers || []).join(', ') }}
          </template>
        </el-table-column>

        <el-table-column label="操作" width="280" fixed="right" align="center">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
              <el-button link type="primary" @click="handleViewDetails(row)">详情</el-button>
              <el-button link type="primary" @click="handleSort(row)">排序</el-button>
              <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-container">
        <div class="pagination-info">共 {{ total }} 条</div>
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[25, 50, 100]"
          layout="sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </div>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="showDialog"
      :title="dialogTitle"
      width="800px"
      :close-on-click-modal="false"
    >
      <el-form :model="formData" label-width="120px">
        <el-form-item label="房型名称" required>
          <el-input v-model="formData.name" placeholder="请输入房型名称" />
        </el-form-item>

        <el-form-item label="简称" required>
          <el-input v-model="formData.shortName" placeholder="请输入简称" />
        </el-form-item>

        <el-form-item label="最大入住人数">
          <el-input-number v-model="formData.maxGuests" :min="1" />
        </el-form-item>

        <el-form-item label="默认门市价">
          <div class="price-section">
            <!-- 快速填充 -->
            <div class="quick-fill-section">
              <span class="quick-fill-label">快速填充</span>
              <el-input-number
                v-model="quickFillPrice"
                :min="0"
                :precision="0"
                placeholder="输入价格后点击应用"
                class="quick-fill-input"
              />
              <el-button @click="handleQuickFill" class="quick-fill-btn">应用到全部</el-button>
            </div>

            <!-- 价格网格 -->
            <div class="price-grid">
              <div class="price-item">
                <span>一</span>
                <el-input-number v-model="formData.mondayPrice" :min="0" :precision="0" />
              </div>
              <div class="price-item">
                <span>二</span>
                <el-input-number v-model="formData.tuesdayPrice" :min="0" :precision="0" />
              </div>
              <div class="price-item">
                <span>三</span>
                <el-input-number v-model="formData.wednesdayPrice" :min="0" :precision="0" />
              </div>
              <div class="price-item">
                <span>四</span>
                <el-input-number v-model="formData.thursdayPrice" :min="0" :precision="0" />
              </div>
              <div class="price-item">
                <span>五</span>
                <el-input-number v-model="formData.fridayPrice" :min="0" :precision="0" />
              </div>
              <div class="price-item">
                <span>六</span>
                <el-input-number v-model="formData.saturdayPrice" :min="0" :precision="0" />
              </div>
              <div class="price-item">
                <span>日</span>
                <el-input-number v-model="formData.sundayPrice" :min="0" :precision="0" />
              </div>
            </div>
          </div>
        </el-form-item>

        <el-form-item label="房间数">
          <el-input-number v-model="formData.roomCount" :min="0" />
          <span class="unit">间</span>
        </el-form-item>

        <el-form-item label="房间号">
          <div class="room-numbers-container">
            <div
              v-if="formData.roomNumbers.length === 0"
              class="empty-room-hint"
            >
              房间删除后,将不对关联的订单也将无法操作"撤销退房"和"恢复预订"。
            </div>
            <div class="room-numbers-list">
              <div
                v-for="(_, index) in formData.roomNumbers"
                :key="index"
                class="room-number-item"
              >
                <el-input
                  v-model="formData.roomNumbers[index]"
                  placeholder="请输入房间号"
                  class="room-number-input"
                />
                <el-button
                  type="danger"
                  circle
                  size="small"
                  @click="removeRoom(index)"
                  class="remove-room-btn"
                >
                  <el-icon><Delete /></el-icon>
                </el-button>
              </div>
            </div>
            <el-button type="primary" link @click="addNewRoom" class="add-room-btn">
              <el-icon><Plus /></el-icon> 新增
            </el-button>
          </div>
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showDialog = false">取消</el-button>
          <el-button type="primary" @click="handleSave">保存</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { InfoFilled, Plus, Delete } from '@element-plus/icons-vue'
import { getAllRoomTypesWithRooms, createRoomType, updateRoomType, deleteRoomType } from '@/api/roomType'

const router = useRouter()

interface RoomTypeData {
  id?: number
  name: string
  shortName: string
  maxGuests: number
  defaultPrice?: number
  mondayPrice: number
  tuesdayPrice: number
  wednesdayPrice: number
  thursdayPrice: number
  fridayPrice: number
  saturdayPrice: number
  sundayPrice: number
  roomCount: number
  roomNumbers: string[]
}

const loading = ref(false)
const showDialog = ref(false)
const isEdit = ref(false)
const currentPage = ref(1)
const pageSize = ref(25)
const total = ref(0)

// 系统周期日期
const systemPeriod = ref('12/12 ( 2025/11/04 ~ 2026/07/18 )')

// 快速填充价格 - 默认为null避免误操作
const quickFillPrice = ref<number | null>(null)

// 房型数据列表
const roomTypeList = ref<RoomTypeData[]>([])

// 表单数据
const formData = ref<RoomTypeData>({
  name: '',
  shortName: '',
  maxGuests: 4,
  mondayPrice: 25000,
  tuesdayPrice: 25000,
  wednesdayPrice: 25000,
  thursdayPrice: 25000,
  fridayPrice: 28000,
  saturdayPrice: 28000,
  sundayPrice: 25000,
  roomCount: 1,
  roomNumbers: [],
})

// 对话框标题
const dialogTitle = computed(() => (isEdit.value ? '编辑房型' : '新增房型'))

// 分页数据
const paginatedData = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  return roomTypeList.value.slice(start, end)
})

// 格式化价格
const formatPrice = (price: number | undefined): string => {
  if (price === undefined || price === null) return '0'
  return price.toString()
}

// 添加新房间号
const addNewRoom = () => {
  formData.value.roomNumbers.push('')
}

// 删除房间号
const removeRoom = (index: number) => {
  if (formData.value.roomNumbers.length > 0) {
    formData.value.roomNumbers.splice(index, 1)
  }
}

// 监听房间数量变化,调整房间号数组长度
watch(
  () => formData.value.roomCount,
  (newCount) => {
    const currentLength = formData.value.roomNumbers.length

    if (newCount > currentLength) {
      // 增加房间号输入框
      const roomsToAdd = newCount - currentLength
      for (let i = 0; i < roomsToAdd; i++) {
        formData.value.roomNumbers.push('')
      }
    } else if (newCount < currentLength) {
      // 减少房间号输入框
      formData.value.roomNumbers = formData.value.roomNumbers.slice(0, newCount)
    }
  },
)

// 监听房间号数组变化,同步更新房间数量
watch(
  () => formData.value.roomNumbers.length,
  (newLength) => {
    formData.value.roomCount = newLength
  },
)

// 快速填充价格到所有天
const handleQuickFill = () => {
  if (quickFillPrice.value === null || quickFillPrice.value === undefined) {
    ElMessage.warning('请先输入快速填充价格')
    return
  }

  if (quickFillPrice.value < 0) {
    ElMessage.warning('价格不能为负数')
    return
  }

  // 确认是否要应用到全部
  ElMessageBox.confirm(
    `确定要将价格 ¥${quickFillPrice.value} 应用到周一至周日吗?此操作将覆盖当前所有价格设置。`,
    '确认快速填充',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    }
  )
    .then(() => {
      formData.value.mondayPrice = quickFillPrice.value!
      formData.value.tuesdayPrice = quickFillPrice.value!
      formData.value.wednesdayPrice = quickFillPrice.value!
      formData.value.thursdayPrice = quickFillPrice.value!
      formData.value.fridayPrice = quickFillPrice.value!
      formData.value.saturdayPrice = quickFillPrice.value!
      formData.value.sundayPrice = quickFillPrice.value!
      ElMessage.success('价格已应用到全部')
    })
    .catch(() => {
      // 用户取消,不执行任何操作
    })
}

// 加载房型数据
const loadRoomTypes = async () => {
  try {
    loading.value = true
    const response = await getAllRoomTypesWithRooms()
    if (response.success && response.data) {
      console.log('🔍 后端返回的原始数据:', response.data)

      // 将后端数据转换为前端格式
      roomTypeList.value = response.data.map((item: any) => {
        // 从rooms数组中提取房间号
        const roomNumbers = item.rooms ? item.rooms.map((room: any) => room.roomNumber) : []

        console.log(`📊 房型 ${item.name} 的价格数据:`, {
          原始数据: {
            mondayPrice: item.mondayPrice,
            tuesdayPrice: item.tuesdayPrice,
            wednesdayPrice: item.wednesdayPrice,
            thursdayPrice: item.thursdayPrice,
            fridayPrice: item.fridayPrice,
            saturdayPrice: item.saturdayPrice,
            sundayPrice: item.sundayPrice,
            defaultPrice: item.defaultPrice,
            weekdayPrice: item.weekdayPrice,
            weekendPrice: item.weekendPrice,
          }
        })

        const mappedData = {
          id: item.id,
          name: item.name,
          shortName: item.description || item.code,
          maxGuests: item.maxGuests ?? 4,
          defaultPrice: item.defaultPrice ?? 0, // 保存原始defaultPrice
          mondayPrice: item.mondayPrice ?? item.weekdayPrice ?? item.defaultPrice ?? 0,
          tuesdayPrice: item.tuesdayPrice ?? item.weekdayPrice ?? item.defaultPrice ?? 0,
          wednesdayPrice: item.wednesdayPrice ?? item.weekdayPrice ?? item.defaultPrice ?? 0,
          thursdayPrice: item.thursdayPrice ?? item.weekdayPrice ?? item.defaultPrice ?? 0,
          fridayPrice: item.fridayPrice ?? item.weekdayPrice ?? item.defaultPrice ?? 0,
          saturdayPrice: item.saturdayPrice ?? item.weekendPrice ?? item.defaultPrice ?? 0,
          sundayPrice: item.sundayPrice ?? item.weekendPrice ?? item.defaultPrice ?? 0,
          roomCount: item.totalRooms || roomNumbers.length,
          roomNumbers: roomNumbers, // 从rooms数组提取房间号
        }

        console.log(`✅ 转换后的价格数据:`, {
          defaultPrice: mappedData.defaultPrice,
          mondayPrice: mappedData.mondayPrice,
          tuesdayPrice: mappedData.tuesdayPrice,
          wednesdayPrice: mappedData.wednesdayPrice,
          thursdayPrice: mappedData.thursdayPrice,
          fridayPrice: mappedData.fridayPrice,
          saturdayPrice: mappedData.saturdayPrice,
          sundayPrice: mappedData.sundayPrice,
        })

        return mappedData
      })
      total.value = roomTypeList.value.length
    } else {
      ElMessage.error(response.message || '加载房型数据失败')
    }
  } catch (error) {
    console.error('加载房型数据失败:', error)
    ElMessage.error('加载房型数据失败')
  } finally {
    loading.value = false
  }
}

// 房间归属
const handleRoomOwnership = () => {
  router.push('/settings/room/ownership')
}

// 导入房型
const handleImport = () => {
  ElMessage.info('导入房型功能开发中')
}

// 新增
const handleAdd = () => {
  isEdit.value = false
  formData.value = {
    name: '',
    shortName: '',
    maxGuests: 4,
    defaultPrice: 25000,
    mondayPrice: 25000,
    tuesdayPrice: 25000,
    wednesdayPrice: 25000,
    thursdayPrice: 25000,
    fridayPrice: 28000,
    saturdayPrice: 28000,
    sundayPrice: 25000,
    roomCount: 1,
    roomNumbers: [''], // 初始化至少一个空输入框
  }
  showDialog.value = true
}

// 编辑
const handleEdit = (row: RoomTypeData) => {
  isEdit.value = true
  console.log('✏️ 编辑房型数据:', row)

  // 深拷贝数据,确保数组也是新的引用
  formData.value = {
    ...row,
    roomNumbers: row.roomNumbers && row.roomNumbers.length > 0 ? [...row.roomNumbers] : ['']
  }

  console.log('📋 formData设置为:', formData.value)
  showDialog.value = true
}

// 查看详情
const handleViewDetails = (row: RoomTypeData) => {
  router.push(`/settings/room-type/${row.id}/details`)
}

// 排序
const handleSort = (row: RoomTypeData) => {
  ElMessage.info(`排序: ${row.name}`)
}

// 删除
const handleDelete = (row: RoomTypeData) => {
  ElMessageBox.confirm(
    `确定要删除房型"${row.name}"吗?`,
    '确认删除',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    }
  )
    .then(async () => {
      try {
        loading.value = true
        if (row.id) {
          const response = await deleteRoomType(row.id)
          if (response.success) {
            ElMessage.success('删除成功')
            await loadRoomTypes()
          } else {
            ElMessage.error(response.message || '删除失败')
          }
        }
      } catch (error) {
        console.error('删除失败:', error)
        ElMessage.error('删除失败')
      } finally {
        loading.value = false
      }
    })
    .catch(() => {
      // 用户取消
    })
}

// 保存
const handleSave = async () => {
  if (!formData.value.name.trim()) {
    ElMessage.warning('请输入房型名称')
    return
  }
  if (!formData.value.shortName.trim()) {
    ElMessage.warning('请输入简称')
    return
  }

  // 过滤掉空的房间号
  const validRoomNumbers = formData.value.roomNumbers
    .map(num => (num || '').trim())
    .filter(num => num.length > 0)

  if (validRoomNumbers.length === 0) {
    ElMessage.warning('请至少输入一个房间号')
    return
  }

  // 检查房间号是否重复
  const uniqueRoomNumbers = new Set(validRoomNumbers)
  if (uniqueRoomNumbers.size !== validRoomNumbers.length) {
    ElMessage.warning('房间号不能重复')
    return
  }

  try {
    loading.value = true

    // 构建请求数据
    // 后端当前规则：同一门店下房间号全局唯一（不同房型也不能重复）。
    // 这里在前端先做一次冲突校验，避免保存后才失败。
    const occupiedRoomNumberToRoomType = new Map<string, RoomTypeData>()
    for (const rt of roomTypeList.value) {
      // 编辑时允许与自己原有房间号重复
      if (isEdit.value && formData.value.id && rt.id === formData.value.id) continue
      for (const rn of rt.roomNumbers || []) {
        const normalized = (rn || '').trim()
        if (normalized) {
          occupiedRoomNumberToRoomType.set(normalized, rt)
        }
      }
    }

    const conflictRoomNumbers = validRoomNumbers.filter(rn => occupiedRoomNumberToRoomType.has(rn))
    if (conflictRoomNumbers.length > 0) {
      const first = conflictRoomNumbers[0]
      const conflictRoomType = occupiedRoomNumberToRoomType.get(first)
      const roomTypeName = conflictRoomType ? `${conflictRoomType.name}（${conflictRoomType.shortName}）` : '其他房型'
      ElMessage.warning(`房间号 ${first} 已存在于房型 ${roomTypeName}，请先删除/更换后再保存`)
      return
    }

    const requestData = {
      name: formData.value.name,
      code: formData.value.shortName.substring(0, 3).toUpperCase(),
      description: formData.value.shortName,
      totalRooms: validRoomNumbers.length, // 使用实际房间号数量
      maxGuests: formData.value.maxGuests,
      // 编辑时保持原有defaultPrice,新增时使用周一价格作为默认价格
      defaultPrice: isEdit.value && formData.value.defaultPrice !== undefined
        ? formData.value.defaultPrice
        : formData.value.mondayPrice,
      mondayPrice: formData.value.mondayPrice,
      tuesdayPrice: formData.value.tuesdayPrice,
      wednesdayPrice: formData.value.wednesdayPrice,
      thursdayPrice: formData.value.thursdayPrice,
      fridayPrice: formData.value.fridayPrice,
      saturdayPrice: formData.value.saturdayPrice,
      sundayPrice: formData.value.sundayPrice,
      roomNumbers: validRoomNumbers, // 添加房间号列表
    }

    console.log('💾 准备保存的数据:', requestData)
    console.log('📝 编辑模式:', isEdit.value, '房型ID:', formData.value.id)

    let response
    if (isEdit.value && formData.value.id) {
      response = await updateRoomType(formData.value.id, requestData)
    } else {
      response = await createRoomType(requestData)
    }

    console.log('✅ 保存响应:', response)

    if (response.success) {
      ElMessage.success(isEdit.value ? '更新成功' : '新增成功')
      showDialog.value = false
      await loadRoomTypes()
    } else {
      ElMessage.error(response.message || (isEdit.value ? '更新失败' : '新增失败'))
    }
  } catch (error) {
    console.error('保存失败:', error)
    ElMessage.error('保存失败')
  } finally {
    loading.value = false
  }
}

// 分页处理
const handleSizeChange = (val: number) => {
  pageSize.value = val
  currentPage.value = 1
}

const handleCurrentChange = (val: number) => {
  currentPage.value = val
}

onMounted(() => {
  loadRoomTypes()
})
</script>

<style scoped>
.room-settings-container {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #fff;
}

/* 顶部信息栏 */
.header-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #e8e8e8;
  background: #f5f7fa;
}

.system-period {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #1890ff;
}

.header-actions {
  display: flex;
  gap: 12px;
}

/* 表格容器 */
.table-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 20px;
  overflow: hidden;
}

.room-settings-table {
  flex: 1;
  overflow: auto;
}

/* 操作按钮 */
.action-buttons {
  display: flex;
  gap: 8px;
  justify-content: center;
  flex-wrap: wrap;
}

/* 分页容器 */
.pagination-container {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 0;
  border-top: 1px solid #e8e8e8;
  margin-top: 16px;
}

.pagination-info {
  font-size: 14px;
  color: #666;
}

/* 对话框 */
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

/* 价格部分容器 */
.price-section {
  width: 100%;
}

/* 快速填充部分 */
.quick-fill-section {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  padding: 12px;
  background: #f5f7fa;
  border-radius: 4px;
}

.quick-fill-label {
  font-size: 14px;
  color: #666;
  min-width: 70px;
}

.quick-fill-input {
  width: 200px;
}

.quick-fill-btn {
  flex-shrink: 0;
}

/* 价格网格 */
.price-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
  width: 100%;
}

.price-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.price-item span {
  min-width: 50px;
  font-size: 14px;
  color: #666;
}

/* 表格样式优化 */
:deep(.el-table) {
  font-size: 14px;
}

:deep(.el-table th) {
  background: #fafafa;
  color: #333;
  font-weight: 600;
}

:deep(.el-table td) {
  padding: 12px 0;
}

:deep(.el-table__header-wrapper .el-table__cell) {
  background: #fafafa;
}

/* 输入框样式 */
:deep(.el-input-number) {
  width: 100%;
}

/* 房间号管理样式 */
.unit {
  margin-left: 8px;
  color: #606266;
}

.room-numbers-container {
  width: 100%;
}

.empty-room-hint {
  color: #909399;
  font-size: 12px;
  padding: 8px 0;
  line-height: 1.5;
  margin-bottom: 12px;
}

.room-numbers-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 12px;
}

.room-number-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.room-number-input {
  flex: 1;
  max-width: 400px;
}

.remove-room-btn {
  flex-shrink: 0;
}

.add-room-btn {
  font-size: 14px;
  padding: 0;
}
</style>
