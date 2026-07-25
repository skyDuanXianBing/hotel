<script setup lang="ts">
import { computed } from 'vue'
import { ArrowDown, ArrowUp, Delete, Plus } from '@element-plus/icons-vue'
import type {
  IndependentSitePageSection,
  IndependentSiteSectionType,
} from '@/types/independentSite'
import { INDEPENDENT_SITE_MAX_SECTIONS } from '../pageSchema'
import {
  INDEPENDENT_SITE_ADDABLE_SECTION_TYPES,
  INDEPENDENT_SITE_SECTION_TYPE_LABELS,
} from './constants'

const props = defineProps<{
  sections: IndependentSitePageSection[]
  selectedId: string
}>()

const emit = defineEmits<{
  select: [id: string]
  move: [id: string, offset: number]
  remove: [id: string]
  add: [type: IndependentSiteSectionType]
}>()

const existingTypes = computed(() => new Set(props.sections.map((section) => section.type)))
const reachedMax = computed(() => props.sections.length >= INDEPENDENT_SITE_MAX_SECTIONS)

const sectionKey = (section: IndependentSitePageSection, index: number) =>
  section.id || `${section.type}-${index}`

const isTypeDisabled = (type: IndependentSiteSectionType) => existingTypes.value.has(type)
</script>

<template>
  <aside class="section-list-panel">
    <div class="panel-heading">
      <h3>页面区块</h3>
      <span>{{ sections.length }}/{{ INDEPENDENT_SITE_MAX_SECTIONS }}</span>
    </div>

    <ul class="section-list">
      <li
        v-for="(section, index) in sections"
        :key="sectionKey(section, index)"
        class="section-item"
        :class="{ 'is-selected': section.id === selectedId }"
        @click="section.id && emit('select', section.id)"
      >
        <div class="section-item-main">
          <span class="section-type">{{ INDEPENDENT_SITE_SECTION_TYPE_LABELS[section.type] }}</span>
          <span class="section-title">{{ section.title || '（未填写标题）' }}</span>
        </div>
        <div class="section-item-actions" @click.stop>
          <el-button
            :icon="ArrowUp"
            circle
            size="small"
            aria-label="上移区块"
            :disabled="index === 0"
            @click="section.id && emit('move', section.id, -1)"
          />
          <el-button
            :icon="ArrowDown"
            circle
            size="small"
            aria-label="下移区块"
            :disabled="index === sections.length - 1"
            @click="section.id && emit('move', section.id, 1)"
          />
          <el-button
            :icon="Delete"
            circle
            size="small"
            type="danger"
            plain
            aria-label="删除区块"
            :disabled="section.type === 'HERO'"
            :title="section.type === 'HERO' ? '首屏横幅为必需区块，不能删除' : '删除区块'"
            @click="section.id && emit('remove', section.id)"
          />
        </div>
      </li>
    </ul>

    <el-dropdown
      class="add-section"
      trigger="click"
      :disabled="reachedMax"
      @command="(type: IndependentSiteSectionType) => emit('add', type)"
    >
      <el-button type="primary" plain :icon="Plus" :disabled="reachedMax" style="width: 100%">
        {{ reachedMax ? '已达区块数量上限' : '添加区块' }}
      </el-button>
      <template #dropdown>
        <el-dropdown-menu>
          <el-dropdown-item
            v-for="type in INDEPENDENT_SITE_ADDABLE_SECTION_TYPES"
            :key="type"
            :command="type"
            :disabled="isTypeDisabled(type)"
          >
            {{ INDEPENDENT_SITE_SECTION_TYPE_LABELS[type] }}
            <span v-if="isTypeDisabled(type)" class="add-disabled-hint">已存在</span>
          </el-dropdown-item>
        </el-dropdown-menu>
      </template>
    </el-dropdown>
    <p class="panel-hint">同类型区块每页至多一个；首屏横幅为必需区块。</p>
  </aside>
</template>

<style scoped>
.section-list-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
  width: 280px;
  min-width: 280px;
  padding: 16px;
  border: 1px solid #e4e9e7;
  border-radius: 14px;
  background: #fbfdfc;
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

.panel-heading span {
  color: #8b9592;
  font-size: 12px;
}

.section-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin: 0;
  padding: 0;
  list-style: none;
  overflow-y: auto;
}

.section-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 10px 12px;
  border: 1px solid #e2e8e5;
  border-radius: 11px;
  background: #fff;
  cursor: pointer;
  transition:
    border-color 0.2s,
    box-shadow 0.2s;
}

.section-item:hover {
  border-color: #4d9386;
}

.section-item.is-selected {
  border-color: #28695e;
  box-shadow: 0 0 0 2px rgba(40, 105, 94, 0.12);
}

.section-item-main {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
}

.section-type {
  color: #357d70;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.05em;
}

.section-title {
  overflow: hidden;
  color: #26302e;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.section-item-actions {
  display: flex;
  flex-shrink: 0;
  gap: 4px;
}

.section-item-actions .el-button + .el-button {
  margin-left: 0;
}

.add-section {
  width: 100%;
}

.add-disabled-hint {
  margin-left: 8px;
  color: #a6afac;
  font-size: 12px;
}

.panel-hint {
  margin: 0;
  color: #98a19e;
  font-size: 12px;
  line-height: 1.6;
}
</style>
