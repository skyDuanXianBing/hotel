<script setup lang="ts">
import { computed } from 'vue'
import { ArrowDown, ArrowUp, Delete, Plus } from '@element-plus/icons-vue'
import { useI18n } from 'vue-i18n'
import type { IndependentSitePageSection } from '@/types/independentSite'
import {
  INDEPENDENT_SITE_MAX_GALLERY_IMAGES,
  INDEPENDENT_SITE_MAX_LIST_ITEMS,
  safeIndependentSiteImageUrl,
} from '../pageSchema'
import { getIndependentSiteSectionTypeLabel } from './constants'
import SectionImageUpload from './SectionImageUpload.vue'

// section 是编辑器内部响应式 schema 的直接引用，此处修改会触发父级 watch 并实时刷新预览
const props = defineProps<{
  section: IndependentSitePageSection
}>()

const { t } = useI18n()

const ITEM_SECTION_TYPES = new Set(['HIGHLIGHTS', 'AMENITIES', 'HOUSE_RULES'])
const IMAGE_URL_SECTION_TYPES = new Set(['HERO', 'ABOUT'])
// body 仅对实际渲染它的类型展示（HIGHLIGHTS/AMENITIES 不渲染正文）
const BODY_SECTION_TYPES = new Set(['HERO', 'ABOUT', 'LOCATION', 'HOUSE_RULES', 'GALLERY', 'BOOKING'])

const typeLabel = computed(() => getIndependentSiteSectionTypeLabel(t, props.section.type))
const hasItems = computed(() => ITEM_SECTION_TYPES.has(props.section.type))
const hasBody = computed(() => BODY_SECTION_TYPES.has(props.section.type))
const hasImageUrl = computed(() => IMAGE_URL_SECTION_TYPES.has(props.section.type))
const isGallery = computed(() => props.section.type === 'GALLERY')
const isBooking = computed(() => props.section.type === 'BOOKING')

const safeImageUrl = computed(() => safeIndependentSiteImageUrl(props.section.imageUrl))

const ensureItems = (): string[] => {
  if (!Array.isArray(props.section.items)) {
    props.section.items = []
  }
  return props.section.items
}

const addItem = () => {
  const items = ensureItems()
  if (items.length < INDEPENDENT_SITE_MAX_LIST_ITEMS) {
    items.push('')
  }
}

const removeItem = (index: number) => {
  ensureItems().splice(index, 1)
}

const moveItem = (index: number, offset: number) => {
  const items = ensureItems()
  const target = index + offset
  if (target < 0 || target >= items.length) {
    return
  }
  const [item] = items.splice(index, 1)
  items.splice(target, 0, item)
}

const ensureImages = () => {
  if (!Array.isArray(props.section.images)) {
    props.section.images = []
  }
  return props.section.images
}

const addGalleryImage = (url = '') => {
  const images = ensureImages()
  if (images.length < INDEPENDENT_SITE_MAX_GALLERY_IMAGES) {
    images.push({ url, alt: '' })
  }
}

const removeGalleryImage = (index: number) => {
  ensureImages().splice(index, 1)
}

const moveGalleryImage = (index: number, offset: number) => {
  const images = ensureImages()
  const target = index + offset
  if (target < 0 || target >= images.length) {
    return
  }
  const [image] = images.splice(index, 1)
  images.splice(target, 0, image)
}

const galleryThumb = (url: string) => safeIndependentSiteImageUrl(url)
</script>

<template>
  <div class="section-form-panel">
    <div class="panel-heading">
      <h3>{{ typeLabel }}</h3>
      <span class="type-tag">{{ section.type }}</span>
    </div>

    <el-alert
      v-if="isBooking"
      class="booking-note"
      type="info"
      :closable="false"
      show-icon
      :title="t('independentSite.editor.bookingBlockTitle')"
      :description="t('independentSite.editor.bookingBlockDescription')"
    />

    <div class="field">
      <label>{{ t('independentSite.editor.title') }} <span class="field-required">{{ t('independentSite.editor.required') }}</span></label>
      <el-input
        v-model="section.title"
        maxlength="120"
        show-word-limit
        autocomplete="off"
        :placeholder="t('independentSite.editor.titlePlaceholder')"
      />
    </div>

    <div v-if="hasBody" class="field">
      <label>{{ t('independentSite.editor.body') }}</label>
      <el-input
        v-model="section.body"
        type="textarea"
        :rows="4"
        maxlength="600"
        show-word-limit
        resize="vertical"
        :placeholder="t('independentSite.editor.bodyPlaceholder')"
      />
    </div>

    <div class="field">
      <label>{{ t('independentSite.editor.alignment') }}</label>
      <el-radio-group v-model="section.alignment">
        <el-radio-button value="LEFT">{{ t('independentSite.editor.alignLeft') }}</el-radio-button>
        <el-radio-button value="CENTER">{{ t('independentSite.editor.alignCenter') }}</el-radio-button>
      </el-radio-group>
    </div>

    <div v-if="hasItems" class="field">
      <label>
        {{ t('independentSite.editor.listItems') }}
        <span class="field-limit">{{ t('independentSite.editor.maxItems', { count: INDEPENDENT_SITE_MAX_LIST_ITEMS }) }}</span>
      </label>
      <div class="item-list">
        <div v-for="(item, index) in ensureItems()" :key="index" class="item-row">
          <el-input
            v-model="ensureItems()[index]"
            maxlength="100"
            autocomplete="off"
            :placeholder="t('independentSite.editor.listItemPlaceholder')"
          />
          <el-button
            :icon="ArrowUp"
            circle
            size="small"
            :aria-label="t('independentSite.editor.moveItemUp')"
            :disabled="index === 0"
            @click="moveItem(index, -1)"
          />
          <el-button
            :icon="ArrowDown"
            circle
            size="small"
            :aria-label="t('independentSite.editor.moveItemDown')"
            :disabled="index === ensureItems().length - 1"
            @click="moveItem(index, 1)"
          />
          <el-button
            :icon="Delete"
            circle
            size="small"
            type="danger"
            plain
            :aria-label="t('independentSite.editor.deleteItem')"
            @click="removeItem(index)"
          />
        </div>
      </div>
      <el-button
        :icon="Plus"
        plain
        size="small"
        :disabled="ensureItems().length >= INDEPENDENT_SITE_MAX_LIST_ITEMS"
        @click="addItem"
      >
        {{ t('independentSite.editor.addItem') }}
      </el-button>
    </div>

    <div v-if="hasImageUrl" class="field">
      <label>{{ t('independentSite.editor.imageUrl') }}</label>
      <div class="image-url-row">
        <el-input
          v-model="section.imageUrl"
          maxlength="1500"
          autocomplete="off"
          :placeholder="t('independentSite.editor.imageUrlPlaceholder')"
        />
        <SectionImageUpload @uploaded="(url: string) => (section.imageUrl = url)" />
      </div>
      <img
        v-if="safeImageUrl"
        class="image-thumb"
        :src="safeImageUrl"
        :alt="t('independentSite.editor.imagePreview')"
      />
      <p v-else-if="section.imageUrl" class="field-warning">
        {{ t('independentSite.editor.invalidImageUrl') }}
      </p>
    </div>

    <div v-if="isGallery" class="field">
      <label>
        {{ t('independentSite.editor.images') }}
        <span class="field-limit">
          {{ t('independentSite.editor.imagesLimit', { count: INDEPENDENT_SITE_MAX_GALLERY_IMAGES }) }}
        </span>
      </label>
      <div class="gallery-list">
        <div v-for="(image, index) in ensureImages()" :key="index" class="gallery-row">
          <img v-if="galleryThumb(image.url)" class="gallery-thumb" :src="galleryThumb(image.url)" alt="" />
          <span v-else class="gallery-thumb gallery-thumb--empty">{{ t('independentSite.editor.noImage') }}</span>
          <div class="gallery-fields">
            <el-input
              v-model="image.url"
              maxlength="1500"
              autocomplete="off"
              :placeholder="t('independentSite.editor.imageUrl')"
            />
            <el-input
              v-model="image.alt"
              maxlength="100"
              autocomplete="off"
              :placeholder="t('independentSite.editor.imageDescription')"
            />
          </div>
          <div class="gallery-actions">
            <el-button
              :icon="ArrowUp"
              circle
              size="small"
              :aria-label="t('independentSite.editor.moveImageUp')"
              :disabled="index === 0"
              @click="moveGalleryImage(index, -1)"
            />
            <el-button
              :icon="ArrowDown"
              circle
              size="small"
              :aria-label="t('independentSite.editor.moveImageDown')"
              :disabled="index === ensureImages().length - 1"
              @click="moveGalleryImage(index, 1)"
            />
            <el-button
              :icon="Delete"
              circle
              size="small"
              type="danger"
              plain
              :aria-label="t('independentSite.editor.deleteImage')"
              @click="removeGalleryImage(index)"
            />
          </div>
        </div>
      </div>
      <div class="gallery-add-row">
        <SectionImageUpload
          :button-text="t('independentSite.common.uploadImage')"
          :disabled="ensureImages().length >= INDEPENDENT_SITE_MAX_GALLERY_IMAGES"
          @uploaded="(url: string) => addGalleryImage(url)"
        />
        <el-button
          :icon="Plus"
          plain
          :disabled="ensureImages().length >= INDEPENDENT_SITE_MAX_GALLERY_IMAGES"
          @click="addGalleryImage()"
        >
          {{ t('independentSite.editor.addImageUrl') }}
        </el-button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.section-form-panel {
  display: flex;
  flex: 0 0 380px;
  flex-direction: column;
  gap: 18px;
  padding: 16px;
  border: 1px solid #e4e9e7;
  border-radius: 14px;
  background: #fff;
  overflow-y: auto;
}

.panel-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.panel-heading h3 {
  margin: 0;
  color: #244c45;
  font-size: 15px;
}

.type-tag {
  padding: 2px 8px;
  border-radius: 999px;
  color: #357d70;
  background: #e7f2ef;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.06em;
}

.booking-note {
  margin-bottom: 2px;
}

.field > label {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-bottom: 7px;
  color: #33403d;
  font-size: 13px;
  font-weight: 700;
}

.field-required {
  color: #b47b46;
  font-size: 11px;
  font-weight: 600;
}

.field-limit {
  color: #98a19e;
  font-size: 11px;
  font-weight: 400;
}

.field-warning {
  margin: 6px 0 0;
  color: #c2782f;
  font-size: 12px;
  line-height: 1.5;
}

.item-list,
.gallery-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 10px;
}

.item-row {
  display: flex;
  gap: 6px;
  align-items: center;
}

.item-row .el-button,
.gallery-actions .el-button {
  flex-shrink: 0;
}

.item-row .el-button + .el-button,
.gallery-actions .el-button + .el-button {
  margin-left: 0;
}

.image-url-row {
  display: flex;
  gap: 8px;
  align-items: flex-start;
}

.image-thumb {
  display: block;
  width: 100%;
  max-height: 180px;
  margin-top: 10px;
  border-radius: 10px;
  object-fit: cover;
  box-shadow: 0 8px 22px rgba(31, 42, 40, 0.12);
}

.gallery-row {
  display: flex;
  gap: 10px;
  align-items: center;
  padding: 10px;
  border: 1px solid #e7ecea;
  border-radius: 11px;
  background: #fbfdfc;
}

.gallery-thumb {
  flex-shrink: 0;
  width: 64px;
  height: 48px;
  border-radius: 8px;
  object-fit: cover;
  background: #eef2f0;
}

.gallery-thumb--empty {
  display: grid;
  place-items: center;
  color: #a6afac;
  font-size: 11px;
}

.gallery-fields {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}

.gallery-actions {
  display: flex;
  flex-shrink: 0;
  gap: 4px;
}

.gallery-add-row {
  display: flex;
  gap: 10px;
  align-items: center;
}
</style>
