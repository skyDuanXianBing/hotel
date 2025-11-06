<template>
  <el-dialog
    v-model="visible"
    title="记一笔"
    width="500px"
    :before-close="handleClose"
    class="record-transaction-dialog"
  >
    <!-- 提示信息 -->
    <el-alert title="记一笔账目不统计进订单消费" type="warning" :closable="false" class="mb-4" />

    <el-form ref="formRef" :model="form" :rules="rules" label-width="80px" class="record-form">
      <!-- 类型选择 -->
      <el-form-item label="类型" prop="type" required>
        <el-radio-group v-model="form.type">
          <el-radio value="income">收入</el-radio>
          <el-radio value="expense">支出</el-radio>
        </el-radio-group>
      </el-form-item>

      <!-- 项目 -->
      <el-form-item label="项目" prop="category" required>
        <el-select v-model="form.category" placeholder="请选择" style="width: 100%">
          <el-option
            v-for="item in categoryOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>

      <!-- 收款方式 -->
      <el-form-item label="收款方式" prop="paymentMethod" required>
        <el-select v-model="form.paymentMethod" placeholder="请选择" style="width: 100%">
          <el-option
            v-for="item in paymentMethodOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>

      <!-- 金额 -->
      <el-form-item label="金额" prop="amount" required>
        <el-input v-model="form.amount" placeholder="请输入金额" type="number" style="width: 100%">
          <template #prepend>¥</template>
        </el-input>
      </el-form-item>

      <!-- 关联房间 -->
      <el-form-item label="关联房间">
        <el-input
          v-model="selectedRoomDisplay"
          placeholder="去选择房间"
          readonly
          @click="showRoomSelector = true"
          style="width: 100%; cursor: pointer"
        >
          <template #suffix>
            <el-icon><ArrowRight /></el-icon>
          </template>
        </el-input>
      </el-form-item>

      <!-- 时间 -->
      <el-form-item label="时间">
        <el-date-picker
          v-model="form.datetime"
          type="datetime"
          placeholder="选择日期时间"
          format="YYYY-MM-DD HH:mm:ss"
          value-format="YYYY-MM-DD HH:mm:ss"
          style="width: 100%"
        />
      </el-form-item>

      <!-- 凭证 -->
      <el-form-item label="凭证">
        <el-upload
          ref="uploadRef"
          :file-list="fileList"
          :auto-upload="false"
          :on-change="handleFileChange"
          :on-remove="handleFileRemove"
          :before-upload="beforeUpload"
          list-type="picture-card"
          accept="image/*"
          class="voucher-upload"
        >
          <div class="upload-trigger">
            <el-icon><Plus /></el-icon>
            <div class="upload-text">添加图片</div>
          </div>
          <template #tip>
            <div class="upload-tip">支持批量上传图片（最多9张），单张图片最大不超过13M</div>
          </template>
        </el-upload>
      </el-form-item>

      <!-- 备注 -->
      <el-form-item label="备注">
        <el-input
          v-model="form.notes"
          type="textarea"
          placeholder="请输入内容"
          :rows="3"
          maxlength="200"
          show-word-limit
          style="width: 100%"
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting"> 完成 </el-button>
      </div>
    </template>

    <!-- 房间选择器弹窗 -->
    <RoomSelectorDialog
      v-model="showRoomSelector"
      :rooms="allRooms"
      :selectedRoomIds="form.roomId ? [form.roomId] : []"
      @confirm="handleRoomSelect"
    />
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import type { FormInstance, FormRules, UploadFile, UploadFiles } from 'element-plus'
import { ElMessage } from 'element-plus'
import { Plus, ArrowRight } from '@element-plus/icons-vue'
import RoomSelectorDialog from './RoomSelectorDialog.vue'
import { getRooms } from '@/api/room'
import type { RoomDTO } from '@/api/room'
import { createNote, type NoteType } from '@/api/notes'

interface Props {
  modelValue: boolean
}

interface Emits {
  (event: 'update:modelValue', value: boolean): void
  (event: 'success'): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value),
})

const formRef = ref<FormInstance>()
const uploadRef = ref()
const submitting = ref(false)
const fileList = ref<UploadFile[]>([])
const showRoomSelector = ref(false)
const allRooms = ref<RoomDTO[]>([])

// 表单数据接口
interface FormData {
  type: NoteType
  category: string
  paymentMethod: string
  amount: string
  roomId: number | null
  datetime: string
  vouchers: File[]
  notes: string
}

// 表单数据
const form = reactive<FormData>({
  type: 'income',
  category: '',
  paymentMethod: '',
  amount: '',
  roomId: null,
  datetime: new Date().toISOString().slice(0, 19).replace('T', ' '),
  vouchers: [],
  notes: '',
})

// 表单验证规则
const rules: FormRules = {
  type: [{ required: true, message: '请选择类型', trigger: 'change' }],
  category: [{ required: true, message: '请选择项目', trigger: 'change' }],
  paymentMethod: [{ required: true, message: '请选择收款方式', trigger: 'change' }],
  amount: [
    { required: true, message: '请输入金额', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (!value || isNaN(Number(value)) || Number(value) <= 0) {
          callback(new Error('请输入有效的金额'))
        } else {
          callback()
        }
      },
      trigger: 'blur',
    },
  ],
}

// 项目选项（根据类型动态显示）
const categoryOptions = computed(() => {
  if (form.type === 'income') {
    return [
      { label: '餐饮美食', value: 'catering' },
      { label: '烟酒饮料', value: 'tobacco_alcohol' },
      { label: '物品赔付', value: 'compensation' },
      { label: '景点门票', value: 'ticket' },
      { label: '特色纪念品', value: 'souvenir' },
      { label: '其他杂项', value: 'other' },
    ]
  } else {
    return [
      { label: '水电燃气', value: 'utilities' },
      { label: '房租物业费', value: 'rent_property' },
      { label: '支付工资', value: 'salary' },
      { label: '房间维修', value: 'maintenance' },
      { label: '通讯交通', value: 'communication_transport' },
      { label: '日常杂项', value: 'daily_misc' },
    ]
  }
})

// 收款方式选项
const paymentMethodOptions = [
  { label: '微信', value: 'wechat' },
  { label: '支付宝', value: 'alipay' },
  { label: '现金', value: 'cash' },
]

// 选中的房间显示文本
const selectedRoomDisplay = computed(() => {
  if (!form.roomId) return ''

  const room = allRooms.value.find((r) => r.id === form.roomId)
  return room ? `${room.roomNumber} - ${room.roomType.name}` : ''
})

// 加载房间数据
const loadRooms = async () => {
  try {
    const response = await getRooms()
    if (response.success) {
      allRooms.value = response.data
    } else {
      ElMessage.error(response.message || '获取房间列表失败')
    }
  } catch (error) {
    console.error('加载房间数据失败:', error)
    ElMessage.error('加载房间数据失败')
  }
}

// 处理房间选择
const handleRoomSelect = (roomIds: number[]) => {
  // 单选模式,只取第一个
  form.roomId = roomIds.length > 0 ? roomIds[0] : null
}

// 文件上传处理
const handleFileChange = (file: UploadFile, fileList: UploadFiles) => {
  // 限制最多9张图片
  if (fileList.length > 9) {
    ElMessage.warning('最多只能上传9张图片')
    return false
  }

  // 更新文件列表
  form.vouchers = fileList.map((item) => item.raw!).filter(Boolean)
}

const handleFileRemove = (file: UploadFile, fileList: UploadFiles) => {
  form.vouchers = fileList.map((item) => item.raw!).filter(Boolean)
}

const beforeUpload = (file: File) => {
  const isImage = file.type.startsWith('image/')
  const isLt13M = file.size / 1024 / 1024 < 13

  if (!isImage) {
    ElMessage.error('只能上传图片文件!')
    return false
  }
  if (!isLt13M) {
    ElMessage.error('图片大小不能超过 13MB!')
    return false
  }
  return false // 阻止自动上传，由组件控制
}

// 重置表单
const resetForm = () => {
  form.type = 'income'
  form.category = ''
  form.paymentMethod = ''
  form.amount = ''
  form.roomId = null
  form.datetime = new Date().toISOString().slice(0, 19).replace('T', ' ')
  form.vouchers = []
  form.notes = ''
  fileList.value = []

  if (formRef.value) {
    formRef.value.resetFields()
  }
}

// 关闭对话框
const handleClose = () => {
  resetForm()
  visible.value = false
}

// 提交表单
const handleSubmit = async () => {
  if (!formRef.value) return

  try {
    await formRef.value.validate()
    submitting.value = true

    // 构建提交数据
    const submitData = {
      type: form.type,
      category: form.category,
      paymentMethod: form.paymentMethod,
      amount: Number(form.amount),
      roomId: form.roomId ?? undefined,
      datetime: form.datetime,
      notes: form.notes,
      vouchers: form.vouchers,
    }

    console.log('提交记账数据:', submitData)

    // 调用API提交数据
    const response = await createNote(submitData)

    if (response.success) {
      ElMessage.success('记账成功')
      emit('success')
      handleClose()
    } else {
      ElMessage.error(response.message || '记账失败')
    }
  } catch (error) {
    console.error('表单验证失败:', error)
  } finally {
    submitting.value = false
  }
}

// 组件挂载时初始化
onMounted(() => {
  loadRooms()
})
</script>

<style scoped>
.record-transaction-dialog {
  .record-form {
    .el-form-item {
      margin-bottom: 18px;
    }
  }

  .voucher-upload {
    .upload-trigger {
      width: 80px;
      height: 80px;
      border: 1px dashed #d9d9d9;
      border-radius: 6px;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      cursor: pointer;
      transition: border-color 0.3s;

      &:hover {
        border-color: #409eff;
      }

      .upload-text {
        margin-top: 4px;
        font-size: 12px;
        color: #666;
      }
    }

    .upload-tip {
      margin-top: 8px;
      font-size: 12px;
      color: #999;
      line-height: 1.4;
    }
  }

  .dialog-footer {
    text-align: right;
  }
}

.mb-4 {
  margin-bottom: 16px;
}
</style>
