<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import type {
  IndependentSitePageSchema,
  IndependentSitePageSection,
  IndependentSiteSectionType,
} from '@/types/independentSite'
import IndependentSitePageRenderer from '../components/IndependentSitePageRenderer.vue'
import {
  createEmptyIndependentSiteSchema,
  createIndependentSiteSection,
  INDEPENDENT_SITE_MAX_SECTIONS,
  normalizeIndependentSiteSchema,
} from '../pageSchema'
import SectionFormPanel from './SectionFormPanel.vue'
import SectionListPanel from './SectionListPanel.vue'

const props = withDefaults(
  defineProps<{
    schema: IndependentSitePageSchema | null
    themeKey?: string
  }>(),
  {
    themeKey: 'classic',
  },
)

const { t } = useI18n()

const emit = defineEmits<{
  change: [schema: IndependentSitePageSchema | null]
}>()

const cloneSchema = (value: unknown): IndependentSitePageSchema =>
  normalizeIndependentSiteSchema(value) ?? createEmptyIndependentSiteSchema(t)

const editableSchema = ref<IndependentSitePageSchema>(cloneSchema(props.schema))
const selectedId = ref(editableSchema.value.sections[0]?.id ?? '')
const previewSchema = ref<IndependentSitePageSchema | null>(
  normalizeIndependentSiteSchema(editableSchema.value),
)
const previewScrollRef = ref<HTMLElement | null>(null)
// 记录最近一次向外发出的内容，避免父组件回写同名 prop 时触发无谓的内部重置
let lastEmittedJson = JSON.stringify(previewSchema.value)

const sections = computed(() => editableSchema.value.sections)

const selectedSection = computed<IndependentSitePageSection | null>(
  () => sections.value.find((section) => section.id === selectedId.value) ?? null,
)

const ensureSelection = () => {
  if (!selectedSection.value) {
    selectedId.value = sections.value[0]?.id ?? ''
  }
}

watch(
  () => props.schema,
  (next) => {
    const nextJson = JSON.stringify(next ?? null)
    if (nextJson === lastEmittedJson) {
      return
    }
    editableSchema.value = cloneSchema(next)
    lastEmittedJson = JSON.stringify(normalizeIndependentSiteSchema(editableSchema.value))
    ensureSelection()
  },
)

watch(
  editableSchema,
  (value) => {
    const normalized = normalizeIndependentSiteSchema(value)
    previewSchema.value = normalized
    lastEmittedJson = JSON.stringify(normalized)
    emit('change', normalized)
  },
  { deep: true },
)

const findSectionIndex = (id: string) =>
  sections.value.findIndex((section) => section.id === id)

const handleSelect = (id: string) => {
  selectedId.value = id
}

const handleMove = (id: string, offset: number) => {
  const index = findSectionIndex(id)
  const target = index + offset
  if (index < 0 || target < 0 || target >= sections.value.length) {
    return
  }
  const list = sections.value
  const [section] = list.splice(index, 1)
  list.splice(target, 0, section)
}

const handleRemove = (id: string) => {
  const index = findSectionIndex(id)
  if (index < 0) {
    return
  }
  const section = sections.value[index]
  if (section.type === 'HERO') {
    return
  }
  sections.value.splice(index, 1)
  if (selectedId.value === id) {
    const fallback = sections.value[Math.min(index, sections.value.length - 1)]
    selectedId.value = fallback?.id ?? ''
  }
}

const handleAdd = (type: IndependentSiteSectionType) => {
  if (sections.value.length >= INDEPENDENT_SITE_MAX_SECTIONS) {
    return
  }
  if (sections.value.some((section) => section.type === type)) {
    return
  }
  const section = createIndependentSiteSection(type, t)
  sections.value.push(section)
  selectedId.value = section.id ?? ''
}

const scrollPreviewToBottom = () => {
  const container = previewScrollRef.value
  if (container) {
    container.scrollTo({ top: container.scrollHeight, behavior: 'smooth' })
  }
}
</script>

<template>
  <div class="page-editor">
    <SectionListPanel
      :sections="sections"
      :selected-id="selectedId"
      @select="handleSelect"
      @move="handleMove"
      @remove="handleRemove"
      @add="handleAdd"
    />

    <SectionFormPanel v-if="selectedSection" :key="selectedSection.id" :section="selectedSection" />
    <div v-else class="form-placeholder">{{ t('independentSite.editor.selectBlock') }}</div>

    <div class="editor-preview">
      <div class="preview-toolbar">
        <span>{{ t('independentSite.editor.livePreview') }}</span>
        <span class="preview-tip">{{ t('independentSite.editor.livePreviewTip') }}</span>
      </div>
      <div ref="previewScrollRef" class="preview-scroll">
        <IndependentSitePageRenderer
          v-if="previewSchema"
          :schema="previewSchema"
          :theme-key="themeKey"
          preview
          @booking-request="scrollPreviewToBottom"
        />
        <el-empty
          v-else
          :description="t('independentSite.editor.invalidPreview')"
        />
      </div>
    </div>
  </div>
</template>

<style scoped>
.page-editor {
  display: flex;
  gap: 14px;
  align-items: stretch;
  min-height: calc(100vh - 220px);
}

.form-placeholder {
  display: grid;
  flex: 0 0 380px;
  place-items: center;
  border: 1px dashed #d7dedb;
  border-radius: 14px;
  color: #98a19e;
  font-size: 13px;
}

.editor-preview {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-width: 0;
  border: 1px solid #e4e9e7;
  border-radius: 14px;
  background: #f5f6f5;
  overflow: hidden;
}

.preview-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 14px;
  border-bottom: 1px solid #e4e9e7;
  color: #244c45;
  font-size: 13px;
  font-weight: 700;
}

.preview-tip {
  color: #98a19e;
  font-size: 12px;
  font-weight: 400;
}

.preview-scroll {
  flex: 1;
  max-height: calc(100vh - 260px);
  padding: 14px;
  overflow-y: auto;
}

@media (max-width: 1200px) {
  .page-editor {
    flex-wrap: wrap;
  }

  .editor-preview {
    flex-basis: 100%;
  }
}
</style>
