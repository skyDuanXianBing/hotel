<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage, type UploadRequestOptions } from 'element-plus'
import { Upload } from '@element-plus/icons-vue'
import { uploadMedia } from '@/api/media'
import { safeIndependentSiteImageUrl } from '../pageSchema'
import { INDEPENDENT_SITE_IMAGE_MAX_BYTES } from './constants'

const props = withDefaults(
  defineProps<{
    disabled?: boolean
    buttonText?: string
  }>(),
  {
    disabled: false,
    buttonText: '上传图片',
  },
)

const emit = defineEmits<{
  uploaded: [url: string]
}>()

const uploading = ref(false)

const beforeUpload = (file: File) => {
  if (!file.type.startsWith('image/')) {
    ElMessage.warning('仅支持图片文件')
    return false
  }
  if (file.size > INDEPENDENT_SITE_IMAGE_MAX_BYTES) {
    ElMessage.warning('图片大小不能超过 5MB')
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
      throw new Error(response?.message || '图片上传失败')
    }
    emit('uploaded', url)
    ElMessage.success('图片已上传')
  } catch (error) {
    const message =
      error && typeof error === 'object'
        ? ((error as { response?: { data?: { message?: unknown } } }).response?.data
            ?.message as string) || (error instanceof Error ? error.message : '')
        : ''
    ElMessage.error(message || '图片上传失败，请重试')
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
      {{ props.buttonText }}
    </el-button>
  </el-upload>
</template>
