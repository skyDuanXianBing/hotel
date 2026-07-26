<script setup lang="ts">
import { computed, ref } from 'vue'
import { ElMessage, type UploadRequestOptions } from 'element-plus'
import { Upload } from '@element-plus/icons-vue'
import { useI18n } from 'vue-i18n'
import { uploadMedia } from '@/api/media'
import { safeIndependentSiteImageUrl } from '../pageSchema'
import { INDEPENDENT_SITE_IMAGE_MAX_BYTES } from './constants'

const props = defineProps<{
  disabled?: boolean
  buttonText?: string
}>()

const { t } = useI18n()

const emit = defineEmits<{
  uploaded: [url: string]
}>()

const uploading = ref(false)
const resolvedButtonText = computed(() => props.buttonText || t('independentSite.common.uploadImage'))

const beforeUpload = (file: File) => {
  if (!file.type.startsWith('image/')) {
    ElMessage.warning(t('independentSite.editor.imageOnly'))
    return false
  }
  if (file.size > INDEPENDENT_SITE_IMAGE_MAX_BYTES) {
    ElMessage.warning(t('independentSite.editor.imageTooLarge'))
    return false
  }
  return true
}

const handleUpload = async (options: UploadRequestOptions) => {
  uploading.value = true
  try {
    const response = await uploadMedia('independent-site', options.file as File)
    const url = safeIndependentSiteImageUrl(response?.data?.url)
    if (!response?.success || !url) {
      throw new Error(response?.message || t('independentSite.editor.imageUploadFailed'))
    }
    emit('uploaded', url)
    ElMessage.success(t('independentSite.editor.imageUploaded'))
  } catch (error) {
    const message =
      error && typeof error === 'object'
        ? ((error as { response?: { data?: { message?: unknown } } }).response?.data
            ?.message as string) || (error instanceof Error ? error.message : '')
        : ''
    ElMessage.error(message || t('independentSite.editor.imageUploadRetry'))
  } finally {
    uploading.value = false
  }
}
</script>

<template>
  <el-upload
    :show-file-list="false"
    accept="image/*"
    :before-upload="beforeUpload"
    :http-request="handleUpload"
    :disabled="uploading || props.disabled"
  >
    <el-button :icon="Upload" :loading="uploading" :disabled="props.disabled">
      {{ resolvedButtonText }}
    </el-button>
  </el-upload>
</template>
