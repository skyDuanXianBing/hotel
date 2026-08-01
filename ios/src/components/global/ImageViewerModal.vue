<template>
  <!-- PhotoSwipe 自行挂载 UI 到 body，本组件只负责生命周期桥接 -->
</template>

<script setup lang="ts">
import { onBeforeUnmount, watch } from 'vue'
import PhotoSwipeLightbox from 'photoswipe/lightbox'
import 'photoswipe/style.css'

const props = defineProps<{
  open: boolean
  src: string
  title?: string
}>()

const emit = defineEmits<{
  (event: 'close'): void
}>()

let lightbox: PhotoSwipeLightbox | null = null
let openRequestId = 0

function loadImageSize(src: string) {
  return new Promise<{ width: number; height: number }>((resolve, reject) => {
    const image = new Image()
    image.onload = () =>
      resolve({
        width: image.naturalWidth || 1200,
        height: image.naturalHeight || 800,
      })
    image.onerror = () => reject(new Error('loadImageFailed'))
    image.src = src
  })
}

function destroyLightbox() {
  if (lightbox) {
    lightbox.destroy()
    lightbox = null
  }
}

async function openViewer(requestId: number) {
  const src = props.src
  if (!src) {
    emit('close')
    return
  }

  try {
    // PhotoSwipe 要求预先提供图片尺寸；blob URL 已在内存中，加载是同步级速度
    const size = await loadImageSize(src)
    if (requestId !== openRequestId || !props.open) {
      return
    }

    destroyLightbox()
    const nextLightbox = new PhotoSwipeLightbox({
      dataSource: [{ src, width: size.width, height: size.height, alt: props.title || '' }],
      pswpModule: () => import('photoswipe'),
      showHideAnimationType: 'fade',
      wheelToZoom: true,
      padding: { top: 0, bottom: 0, left: 0, right: 0 },
    })

    if (props.title) {
      const title = props.title
      nextLightbox.on('uiRegister', () => {
        nextLightbox.pswp?.ui?.registerElement({
          name: 'image-viewer-caption',
          className: 'image-viewer-caption',
          appendTo: 'root',
          html: title,
        })
      })
    }

    nextLightbox.on('close', () => {
      emit('close')
    })

    lightbox = nextLightbox
    nextLightbox.init()
    nextLightbox.loadAndOpen(0)
  } catch {
    if (requestId === openRequestId) {
      emit('close')
    }
  }
}

watch(
  () => [props.open, props.src],
  () => {
    openRequestId += 1
    if (props.open) {
      void openViewer(openRequestId)
    } else {
      destroyLightbox()
    }
  },
)

onBeforeUnmount(() => {
  openRequestId += 1
  destroyLightbox()
})
</script>

<style>
/* PhotoSwipe 渲染在 body 下，标题样式必须非 scoped */
.image-viewer-caption {
  position: absolute;
  top: calc(10px + env(safe-area-inset-top));
  left: 56px;
  right: 56px;
  overflow: hidden;
  color: rgba(255, 255, 255, 0.92);
  font-size: 14px;
  font-weight: 600;
  line-height: 44px;
  text-align: center;
  text-overflow: ellipsis;
  white-space: nowrap;
  pointer-events: none;
}
</style>
