<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { searchAdminStores, type AdminStoreSearchItem } from '@/api/admin'
import { getAdminErrorMessage } from '@/utils/adminRequest'

/**
 * 管理端可搜索门店选择器（P5 修复，替代手写门店 ID 数字输入）：
 * 远程搜索（名称模糊 + ID 精确，limit 20），选中后显示“名称 (#id)”，提交仍用 id。
 * 支持外部回填 id（如对话框复用列表筛选值）：未知 id 会按 ID 精确搜索补拉名称。
 */
const props = withDefaults(
  defineProps<{
    modelValue: number | undefined
    placeholder?: string
    disabled?: boolean
  }>(),
  {
    placeholder: '',
    disabled: false,
  },
)

const emit = defineEmits<{
  (e: 'update:modelValue', value: number | undefined): void
  // el-form-item trigger:'change' 校验依赖自定义组件显式发出 change 事件
  (e: 'change', value: number | undefined): void
}>()

const { t } = useI18n()

const loading = ref(false)
const options = ref<AdminStoreSearchItem[]>([])
// 已拉取过的门店（id → 门店），保证选中项在搜索词变化后仍能展示“名称 (#id)”
const knownStores = new Map<number, AdminStoreSearchItem>()

const formatStoreLabel = (store: AdminStoreSearchItem) =>
  store.name ? `${store.name} (#${store.id})` : `#${store.id}`

const mergedOptions = computed(() => {
  const list = [...options.value]
  const current = props.modelValue !== undefined ? knownStores.get(props.modelValue) : undefined
  if (current && !list.some((item) => item.id === current.id)) {
    list.unshift(current)
  }
  return list
})

const search = async (keyword: string) => {
  loading.value = true
  try {
    const response = await searchAdminStores(keyword.trim() || undefined)
    if (response.success && Array.isArray(response.data)) {
      response.data.forEach((store) => knownStores.set(store.id, store))
      options.value = response.data
    }
  } catch (error) {
    ElMessage.error(getAdminErrorMessage(error))
  } finally {
    loading.value = false
  }
}

const handleChange = (value: number | string | undefined) => {
  const next = typeof value === 'number' ? value : undefined
  emit('update:modelValue', next)
  emit('change', next)
}

const handleVisibleChange = (visible: boolean) => {
  // 打开下拉时重置为默认前 20 家（远程搜索词随关闭被清空）
  if (visible) {
    void search('')
  }
}

// 外部回填 id（如人工开通弹窗默认带入列表筛选门店）：按 ID 精确搜索补拉名称
watch(
  () => props.modelValue,
  (value) => {
    if (value !== undefined && !knownStores.has(value)) {
      void search(String(value))
    }
  },
  { immediate: true },
)

onMounted(() => {
  void search('')
})
</script>

<template>
  <el-select
    :model-value="modelValue"
    filterable
    remote
    clearable
    :remote-method="search"
    :loading="loading"
    :placeholder="placeholder || t('admin.storeSearch.placeholder')"
    :disabled="disabled"
    class="admin-store-select"
    @update:model-value="handleChange"
    @visible-change="handleVisibleChange"
  >
    <el-option
      v-for="store in mergedOptions"
      :key="store.id"
      :value="store.id"
      :label="formatStoreLabel(store)"
    />
    <template #empty>
      <div class="store-select-empty">{{ t('admin.storeSearch.empty') }}</div>
    </template>
  </el-select>
</template>

<style scoped>
.store-select-empty {
  padding: 16px 0;
  color: #909399;
  font-size: 13px;
  text-align: center;
}
</style>
