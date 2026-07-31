<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { changeAdminPassword } from '@/api/admin'
import { getAdminErrorMessage } from '@/utils/adminRequest'

/**
 * 管理端修改密码对话框（P10）：旧密码/新密码/确认新密码 + 强度提示。
 * 密码规则与后端 ChangePasswordRequest 一致（8-64 位）。
 * 成功后 emit success，由布局层强制重新登录。
 */
const props = defineProps<{
  visible: boolean
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  success: []
}>()

const { t } = useI18n()

const formRef = ref<FormInstance>()
const submitting = ref(false)
const form = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const rules: FormRules = {
  oldPassword: [
    {
      required: true,
      message: t('admin.changePassword.validation.oldRequired'),
      trigger: 'blur',
    },
  ],
  newPassword: [
    {
      required: true,
      message: t('admin.changePassword.validation.newRequired'),
      trigger: 'blur',
    },
    {
      min: 8,
      max: 64,
      message: t('admin.changePassword.validation.newLength'),
      trigger: 'blur',
    },
  ],
  confirmPassword: [
    {
      required: true,
      message: t('admin.changePassword.validation.confirmRequired'),
      trigger: 'blur',
    },
    {
      validator: (_rule, value, callback) => {
        if (value && value !== form.newPassword) {
          callback(new Error(t('admin.changePassword.validation.mismatch')))
          return
        }
        callback()
      },
      trigger: 'blur',
    },
  ],
}

const close = () => {
  emit('update:visible', false)
}

// 每次打开重置表单与校验态，避免陈旧输入/红字残留
watch(
  () => props.visible,
  (visible) => {
    if (!visible) {
      return
    }
    form.oldPassword = ''
    form.newPassword = ''
    form.confirmPassword = ''
    formRef.value?.clearValidate()
  },
)

const handleSubmit = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }

  submitting.value = true
  try {
    const response = await changeAdminPassword({
      oldPassword: form.oldPassword,
      newPassword: form.newPassword,
    })
    if (!response.success) {
      throw new Error(response.message || t('admin.changePassword.failed'))
    }
    ElMessage.success(t('admin.changePassword.success'))
    close()
    emit('success')
  } catch (error) {
    ElMessage.error(getAdminErrorMessage(error, 'admin.changePassword.failed'))
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <el-dialog
    :model-value="visible"
    :title="t('admin.changePassword.title')"
    width="min(440px, 92vw)"
    :close-on-click-modal="false"
    @update:model-value="emit('update:visible', $event)"
    @closed="formRef?.clearValidate()"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
      <el-form-item :label="t('admin.changePassword.oldPassword')" prop="oldPassword">
        <el-input
          v-model="form.oldPassword"
          type="password"
          show-password
          autocomplete="current-password"
          :placeholder="t('admin.changePassword.oldPasswordPlaceholder')"
          :disabled="submitting"
        />
      </el-form-item>
      <el-form-item :label="t('admin.changePassword.newPassword')" prop="newPassword">
        <el-input
          v-model="form.newPassword"
          type="password"
          show-password
          autocomplete="new-password"
          :placeholder="t('admin.changePassword.newPasswordPlaceholder')"
          :disabled="submitting"
        />
        <p class="strength-hint">{{ t('admin.changePassword.strengthHint') }}</p>
      </el-form-item>
      <el-form-item :label="t('admin.changePassword.confirmPassword')" prop="confirmPassword">
        <el-input
          v-model="form.confirmPassword"
          type="password"
          show-password
          autocomplete="new-password"
          :placeholder="t('admin.changePassword.confirmPasswordPlaceholder')"
          :disabled="submitting"
          @keyup.enter="handleSubmit"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button :disabled="submitting" @click="close">
        {{ t('admin.common.cancel') }}
      </el-button>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">
        {{ t('admin.common.confirm') }}
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.strength-hint {
  margin: 4px 0 0;
  color: #909399;
  font-size: 12px;
  line-height: 1.5;
}
</style>
