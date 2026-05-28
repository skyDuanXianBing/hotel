<template>
  <el-dialog
    v-model="visible"
    :title="t('stage6.components.recordTransaction.title')"
    width="500px"
    :before-close="handleClose"
    class="record-transaction-dialog"
  >
    <!-- Tip -->
    <el-alert
      :title="t('stage6.components.recordTransaction.alert')"
      type="warning"
      :closable="false"
      class="mb-4"
    />

    <el-form ref="formRef" :model="form" :rules="rules" label-width="80px" class="record-form">
      <!-- Type selection -->
      <el-form-item :label="t('stage6.common.labels.type')" prop="type" required>
        <el-radio-group v-model="form.type">
          <el-radio value="income">{{ t('stage6.components.recordTransaction.income') }}</el-radio>
          <el-radio value="expense">{{ t('stage6.components.recordTransaction.expense') }}</el-radio>
        </el-radio-group>
      </el-form-item>

      <!-- Category -->
      <el-form-item :label="t('stage6.common.labels.project')" prop="category" required>
        <el-select
          v-model="form.category"
          :placeholder="t('stage6.components.recordTransaction.selectPlaceholder')"
          style="width: 100%"
        >
          <el-option
            v-for="item in categoryOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>

      <!-- Payment method -->
      <el-form-item :label="t('stage6.common.labels.paymentMethod')" prop="paymentMethod" required>
        <el-select
          v-model="form.paymentMethod"
          :placeholder="t('stage6.components.recordTransaction.selectPlaceholder')"
          style="width: 100%"
        >
          <el-option
            v-for="item in paymentMethodOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>

      <!-- Amount -->
      <el-form-item :label="t('stage6.common.labels.amount')" prop="amount" required>
        <el-input
          v-model="form.amount"
          :placeholder="t('stage6.components.recordTransaction.amountPlaceholder')"
          type="number"
          style="width: 100%"
        >
          <template #prepend>¥</template>
        </el-input>
      </el-form-item>

      <!-- Related room -->
      <el-form-item :label="t('stage6.common.labels.relatedRoom')">
        <el-input
          v-model="selectedRoomDisplay"
          :placeholder="t('stage6.components.recordTransaction.selectRoomPlaceholder')"
          readonly
          @click="showRoomSelector = true"
          style="width: 100%; cursor: pointer"
        >
          <template #suffix>
            <el-icon><ArrowRight /></el-icon>
          </template>
        </el-input>
      </el-form-item>

      <!-- Time -->
      <el-form-item :label="t('stage6.common.labels.time')">
        <el-date-picker
          v-model="form.datetime"
          type="datetime"
          :placeholder="t('stage6.components.recordTransaction.datetimePlaceholder')"
          format="YYYY-MM-DD HH:mm:ss"
          value-format="YYYY-MM-DD HH:mm:ss"
          style="width: 100%"
        />
      </el-form-item>

      <!-- Voucher -->
      <el-form-item :label="t('stage6.common.labels.voucher')">
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
            <div class="upload-text">{{ t('stage6.components.recordTransaction.addImage') }}</div>
          </div>
          <template #tip>
            <div class="upload-tip">{{ t('stage6.components.recordTransaction.uploadTip') }}</div>
          </template>
        </el-upload>
      </el-form-item>

      <!-- Notes -->
      <el-form-item :label="t('stage6.common.labels.note')">
        <el-input
          v-model="form.notes"
          type="textarea"
          :placeholder="t('stage6.components.recordTransaction.notesPlaceholder')"
          :rows="3"
          maxlength="200"
          show-word-limit
          style="width: 100%"
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose">{{ t('stage6.common.actions.cancel') }}</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">
          {{ t('stage6.common.actions.done') }}
        </el-button>
      </div>
    </template>

    <!-- Room selector dialog -->
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
import { useI18n } from 'vue-i18n'
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
const { t } = useI18n()

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

// Form data interface
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

// Form data
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

// Form validation rules
const rules = computed<FormRules>(() => ({
  type: [{ required: true, message: t('stage6.components.recordTransaction.typeRequired'), trigger: 'change' }],
  category: [{ required: true, message: t('stage6.components.recordTransaction.categoryRequired'), trigger: 'change' }],
  paymentMethod: [
    {
      required: true,
      message: t('stage6.components.recordTransaction.paymentMethodRequired'),
      trigger: 'change',
    },
  ],
  amount: [
    { required: true, message: t('stage6.components.recordTransaction.amountRequired'), trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (!value || isNaN(Number(value)) || Number(value) <= 0) {
          callback(new Error(t('stage6.components.recordTransaction.amountInvalid')))
        } else {
          callback()
        }
      },
      trigger: 'blur',
    },
  ],
}))

// Category options, shown based on type
const categoryOptions = computed(() => {
  if (form.type === 'income') {
    return [
      { label: t('stage6.components.recordTransaction.categories.catering'), value: 'catering' },
      { label: t('stage6.components.recordTransaction.categories.tobaccoAlcohol'), value: 'tobacco_alcohol' },
      { label: t('stage6.components.recordTransaction.categories.compensation'), value: 'compensation' },
      { label: t('stage6.components.recordTransaction.categories.ticket'), value: 'ticket' },
      { label: t('stage6.components.recordTransaction.categories.souvenir'), value: 'souvenir' },
      { label: t('stage6.components.recordTransaction.categories.other'), value: 'other' },
    ]
  } else {
    return [
      { label: t('stage6.components.recordTransaction.categories.utilities'), value: 'utilities' },
      { label: t('stage6.components.recordTransaction.categories.rentProperty'), value: 'rent_property' },
      { label: t('stage6.components.recordTransaction.categories.salary'), value: 'salary' },
      { label: t('stage6.components.recordTransaction.categories.maintenance'), value: 'maintenance' },
      {
        label: t('stage6.components.recordTransaction.categories.communicationTransport'),
        value: 'communication_transport',
      },
      { label: t('stage6.components.recordTransaction.categories.dailyMisc'), value: 'daily_misc' },
    ]
  }
})

// Payment method options
const paymentMethodOptions = computed(() => [
  { label: t('stage6.components.recordTransaction.paymentMethods.wechat'), value: 'wechat' },
  { label: t('stage6.components.recordTransaction.paymentMethods.alipay'), value: 'alipay' },
  { label: t('stage6.components.recordTransaction.paymentMethods.cash'), value: 'cash' },
])

// Selected room display text
const selectedRoomDisplay = computed(() => {
  if (!form.roomId) return ''

  const room = allRooms.value.find((r) => r.id === form.roomId)
  return room ? `${room.roomNumber} - ${room.roomType.name}` : ''
})

// Load room data
const loadRooms = async () => {
  try {
    const response = await getRooms()
    if (response.success) {
      allRooms.value = response.data
    } else {
      ElMessage.error(response.message || t('stage6.components.recordTransaction.loadRoomsFailed'))
    }
  } catch (error) {
    console.error('Failed to load room data:', error)
    ElMessage.error(t('stage6.components.recordTransaction.loadRoomsFailed'))
  }
}

// Handle room selection
const handleRoomSelect = (roomIds: number[]) => {
  // Single-select mode: use the first item only.
  form.roomId = roomIds.length > 0 ? roomIds[0] : null
}

// File upload handling
const handleFileChange = (file: UploadFile, fileList: UploadFiles) => {
  // Limit to 9 images.
  if (fileList.length > 9) {
    ElMessage.warning(t('stage6.components.recordTransaction.maxImages'))
    return false
  }

  // Update the file list.
  form.vouchers = fileList.map((item) => item.raw!).filter(Boolean)
}

const handleFileRemove = (file: UploadFile, fileList: UploadFiles) => {
  form.vouchers = fileList.map((item) => item.raw!).filter(Boolean)
}

const beforeUpload = (file: File) => {
  const isImage = file.type.startsWith('image/')
  const isLt13M = file.size / 1024 / 1024 < 13

  if (!isImage) {
    ElMessage.error(t('stage6.common.messages.uploadImagesOnly'))
    return false
  }
  if (!isLt13M) {
    ElMessage.error(t('stage6.components.recordTransaction.imageTooLarge'))
    return false
  }
  return false // Prevent automatic upload; this component controls it.
}

// Reset the form
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

// Close the dialog
const handleClose = () => {
  resetForm()
  visible.value = false
}

// Submit the form
const handleSubmit = async () => {
  if (!formRef.value) return

  try {
    await formRef.value.validate()
    submitting.value = true

    // Build submit data.
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

    // Submit data through the API.
    const response = await createNote(submitData)

    if (response.success) {
      ElMessage.success(t('stage6.components.recordTransaction.submitSuccess'))
      emit('success')
      handleClose()
    } else {
      ElMessage.error(response.message || t('stage6.components.recordTransaction.submitFailed'))
    }
  } catch (error) {
    console.error('Form validation failed:', error)
  } finally {
    submitting.value = false
  }
}

// Initialize on component mount.
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
