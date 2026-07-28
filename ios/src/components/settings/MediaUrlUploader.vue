<template>
  <div class="media-url-uploader">
    <div v-if="urls.length > 0" class="media-url-uploader__grid">
      <article v-for="(url, index) in urls" :key="`${url}-${index}`" class="media-url-uploader__item">
        <img :src="resolvePreviewUrl(url)" alt="" />
        <button
          type="button"
          class="media-url-uploader__remove"
          :aria-label="t('settings.common.delete')"
          :disabled="disabled || uploading"
          @click="removeUrl(index)"
        >
          <ion-icon :icon="closeCircle" />
        </button>
      </article>
    </div>

    <button
      type="button"
      class="media-url-uploader__add"
      :disabled="disabled || uploading || urls.length >= maxCount"
      @click="openFilePicker"
    >
      <ion-spinner v-if="uploading" name="crescent" />
      <ion-icon v-else :icon="cloudUploadOutline" />
      <span>
        {{
          uploading
            ? t('channel.mobile.common.saving')
            : t('settingsStage4.roomTypeDetails.photos.upload')
        }}
      </span>
    </button>

    <input
      ref="fileInput"
      class="media-url-uploader__input"
      type="file"
      accept="image/*"
      multiple
      :disabled="disabled || uploading"
      @change="handleFilesSelected"
    />
    <p class="media-url-uploader__hint">
      {{ t('settingsStage4.roomTypeDetails.photos.typeAndSize') }}
    </p>
  </div>
</template>

<script setup lang="ts">
import { IonIcon, IonSpinner } from '@ionic/vue'
import { closeCircle, cloudUploadOutline } from 'ionicons/icons'
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { uploadMedia, type MediaUploadScope } from '@/api/media'
import { API_BASE_URL } from '@/constants/api'
import { showSuccessToast, showWarningToast } from '@/utils/notify'
import { isHandledRequestError } from '@/utils/request'

const props = withDefaults(
  defineProps<{
    modelValue: string
    scope: MediaUploadScope
    disabled?: boolean
    maxCount?: number
  }>(),
  {
    disabled: false,
    maxCount: 10,
  },
)

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

const { t } = useI18n()
const fileInput = ref<HTMLInputElement | null>(null)
const uploading = ref(false)
const urls = computed(() =>
  props.modelValue
    .split(/\r?\n/)
    .map((item) => item.trim())
    .filter(Boolean),
)

function commitUrls(nextUrls: string[]) {
  emit('update:modelValue', nextUrls.join('\n'))
}

function resolvePreviewUrl(url: string) {
  if (/^https?:\/\//i.test(url)) {
    return url
  }

  try {
    const apiRoot = API_BASE_URL.replace(/\/api\/v1\/?$/i, '/')
    return new URL(url, apiRoot).toString()
  } catch {
    return url
  }
}

function openFilePicker() {
  fileInput.value?.click()
}

function removeUrl(index: number) {
  const nextUrls = [...urls.value]
  nextUrls.splice(index, 1)
  commitUrls(nextUrls)
}

function resolveUploadError(error: unknown) {
  if (error instanceof Error && error.message) {
    return error.message
  }
  return t('settingsStage4.roomTypeDetails.messages.uploadFailed')
}

async function handleFilesSelected(event: Event) {
  const input = event.target as HTMLInputElement
  const files = Array.from(input.files || [])
  input.value = ''
  if (files.length === 0) {
    return
  }

  const remainingCount = Math.max(0, props.maxCount - urls.value.length)
  const acceptedFiles = files.slice(0, remainingCount)
  if (acceptedFiles.length < files.length) {
    showWarningToast(t('settingsStage4.roomTypeDetails.photos.maxCount'))
  }

  const nextUrls = [...urls.value]
  let uploadedCount = 0
  uploading.value = true
  try {
    for (const file of acceptedFiles) {
      if (!file.type.startsWith('image/')) {
        showWarningToast(t('settingsStage4.roomTypeDetails.messages.onlyImages'))
        continue
      }
      if (file.size > 5 * 1024 * 1024) {
        showWarningToast(t('settingsStage4.roomTypeDetails.messages.imageTooLarge'))
        continue
      }

      const response = await uploadMedia(props.scope, file)
      if (!response.success || !response.data?.url) {
        throw new Error(
          response.message || t('settingsStage4.roomTypeDetails.messages.uploadFailed'),
        )
      }
      nextUrls.push(response.data.url)
      uploadedCount += 1
      commitUrls(nextUrls)
    }
    if (uploadedCount > 0) {
      showSuccessToast(t('settingsStage4.roomTypeDetails.messages.uploadSuccess'))
    }
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveUploadError(error))
    }
  } finally {
    uploading.value = false
  }
}
</script>

<style scoped>
.media-url-uploader {
  display: grid;
  gap: 10px;
}

.media-url-uploader__grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.media-url-uploader__item {
  position: relative;
  aspect-ratio: 1;
  min-width: 0;
  overflow: hidden;
  border: 1px solid var(--ios-pms-border-soft);
  border-radius: 8px;
  background: var(--ios-pms-surface-muted);
}

.media-url-uploader__item img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.media-url-uploader__remove {
  position: absolute;
  top: 4px;
  right: 4px;
  display: grid;
  place-items: center;
  width: 28px;
  height: 28px;
  padding: 0;
  border: 0;
  border-radius: 50%;
  background: rgba(17, 24, 39, 0.72);
  color: #fff;
}

.media-url-uploader__remove ion-icon {
  font-size: 20px;
}

.media-url-uploader__add {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-height: 48px;
  padding: 0 14px;
  border: 1px dashed var(--ios-pms-primary);
  border-radius: 8px;
  background: var(--ios-pms-primary-soft);
  color: var(--ios-pms-primary-strong);
  font: inherit;
  font-weight: 600;
}

.media-url-uploader__add:disabled {
  opacity: 0.56;
}

.media-url-uploader__add ion-icon,
.media-url-uploader__add ion-spinner {
  width: 19px;
  height: 19px;
  font-size: 19px;
}

.media-url-uploader__input {
  display: none;
}

.media-url-uploader__hint {
  margin: 0;
  color: var(--ios-pms-text-muted);
  font-size: 12px;
  line-height: 1.45;
}
</style>
