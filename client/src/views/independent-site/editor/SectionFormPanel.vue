<script setup lang="ts">
import { computed } from 'vue'
import { ArrowDown, ArrowUp, Delete, Plus } from '@element-plus/icons-vue'
import type { IndependentSitePageSection } from '@/types/independentSite'
import {
  INDEPENDENT_SITE_MAX_GALLERY_IMAGES,
  INDEPENDENT_SITE_MAX_LIST_ITEMS,
  safeIndependentSiteImageUrl,
} from '../pageSchema'
import { INDEPENDENT_SITE_SECTION_TYPE_LABELS } from './constants'
import SectionImageUpload from './SectionImageUpload.vue'

// section 是编辑器内部响应式 schema 的直接引用，此处修改会触发父级 watch 并实时刷新预览
const props = defineProps<{
  section: IndependentSitePageSection
}>()

const ITEM_SECTION_TYPES = new Set(['HIGHLIGHTS', 'AMENITIES', 'HOUSE_RULES'])
const IMAGE_URL_SECTION_TYPES = new Set(['HERO', 'ABOUT'])
// body 仅对实际渲染它的类型展示（HIGHLIGHTS/AMENITIES 不渲染正文）
const BODY_SECTION_TYPES = new Set(['HERO', 'ABOUT', 'LOCATION', 'HOUSE_RULES', 'GALLERY', 'BOOKING'])

const typeLabel = computed(() => INDEPENDENT_SITE_SECTION_TYPE_LABELS[props.section.type])
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
      title="此区块展示订房入口"
      description="公开页面会将访客引导至页面底部的实时订房流程，不会产生第二套订房流程。"
    />

    <div class="field">
      <label>标题 <span class="field-required">必填</span></label>
      <el-input
        v-model="section.title"
        maxlength="120"
        show-word-limit
        autocomplete="off"
        placeholder="区块标题（为空时区块不会通过校验）"
      />
    </div>

    <div v-if="hasBody" class="field">
      <label>正文</label>
      <el-input
        v-model="section.body"
        type="textarea"
        :rows="4"
        maxlength="600"
        show-word-limit
        resize="vertical"
        placeholder="补充说明文字，可多行"
      />
    </div>

    <div class="field">
      <label>对齐方式</label>
      <el-radio-group v-model="section.alignment">
        <el-radio-button value="LEFT">左对齐</el-radio-button>
        <el-radio-button value="CENTER">居中</el-radio-button>
      </el-radio-group>
    </div>

    <div v-if="hasItems" class="field">
      <label>列表项 <span class="field-limit">至多 {{ INDEPENDENT_SITE_MAX_LIST_ITEMS }} 项</span></label>
      <div class="item-list">
        <div v-for="(item, index) in ensureItems()" :key="index" class="item-row">
          <el-input
            v-model="ensureItems()[index]"
            maxlength="100"
            autocomplete="off"
            placeholder="列表项内容"
          />
          <el-button
            :icon="ArrowUp"
            circle
            size="small"
            aria-label="上移列表项"
            :disabled="index === 0"
            @click="moveItem(index, -1)"
          />
          <el-button
            :icon="ArrowDown"
            circle
            size="small"
            aria-label="下移列表项"
            :disabled="index === ensureItems().length - 1"
            @click="moveItem(index, 1)"
          />
          <el-button
            :icon="Delete"
            circle
            size="small"
            type="danger"
            plain
            aria-label="删除列表项"
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
        添加一项
      </el-button>
    </div>

    <div v-if="hasImageUrl" class="field">
      <label>配图地址</label>
      <div class="image-url-row">
        <el-input
          v-model="section.imageUrl"
          maxlength="1500"
          autocomplete="off"
          placeholder="https://… 或 /media/… 相对地址"
        />
        <SectionImageUpload @uploaded="(url: string) => (section.imageUrl = url)" />
      </div>
      <img v-if="safeImageUrl" class="image-thumb" :src="safeImageUrl" alt="配图预览" />
      <p v-else-if="section.imageUrl" class="field-warning">
        地址格式不合法（仅支持 http(s) 或以 / 开头的相对地址），保存时会被忽略。
      </p>
    </div>

    <div v-if="isGallery" class="field">
      <label>
        图片列表
        <span class="field-limit">至多 {{ INDEPENDENT_SITE_MAX_GALLERY_IMAGES }} 张，至少 1 张才会展示</span>
      </label>
      <div class="gallery-list">
        <div v-for="(image, index) in ensureImages()" :key="index" class="gallery-row">
          <img v-if="galleryThumb(image.url)" class="gallery-thumb" :src="galleryThumb(image.url)" alt="" />
          <span v-else class="gallery-thumb gallery-thumb--empty">无图</span>
          <div class="gallery-fields">
            <el-input
              v-model="image.url"
              maxlength="1500"
              autocomplete="off"
              placeholder="图片地址"
            />
            <el-input
              v-model="image.alt"
              maxlength="100"
              autocomplete="off"
              placeholder="图片描述（可选，≤100 字）"
            />
          </div>
          <div class="gallery-actions">
            <el-button
              :icon="ArrowUp"
              circle
              size="small"
              aria-label="上移图片"
              :disabled="index === 0"
              @click="moveGalleryImage(index, -1)"
            />
            <el-button
              :icon="ArrowDown"
              circle
              size="small"
              aria-label="下移图片"
              :disabled="index === ensureImages().length - 1"
              @click="moveGalleryImage(index, 1)"
            />
            <el-button
              :icon="Delete"
              circle
              size="small"
              type="danger"
              plain
              aria-label="删除图片"
              @click="removeGalleryImage(index)"
            />
          </div>
        </div>
      </div>
      <div class="gallery-add-row">
        <SectionImageUpload
          button-text="上传图片"
          :disabled="ensureImages().length >= INDEPENDENT_SITE_MAX_GALLERY_IMAGES"
          @uploaded="(url: string) => addGalleryImage(url)"
        />
        <el-button
          :icon="Plus"
          plain
          :disabled="ensureImages().length >= INDEPENDENT_SITE_MAX_GALLERY_IMAGES"
          @click="addGalleryImage()"
        >
          手动添加地址
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
