<template>
  <section class="memo-section">
    <div class="memo-section__header">
      <h2 class="memo-section__title">备忘录</h2>
      <span class="memo-status" :class="{ 'memo-status--saving': autoSaving }">
        {{ statusText }}
      </span>
    </div>

    <div v-if="loading && !modelValue" class="memo-skeleton">
      <ion-skeleton-text animated class="memo-skeleton__line" />
      <ion-skeleton-text animated class="memo-skeleton__line memo-skeleton__line--short" />
    </div>

    <template v-else>
      <ion-textarea
        v-model="textareaValue"
        auto-grow
        class="memo-textarea"
        fill="outline"
        placeholder="记录今日重点事项或待办…"
        :rows="4"
      />
    </template>
  </section>
</template>

<script setup lang="ts">
import { IonSkeletonText, IonTextarea } from '@ionic/vue'
import { computed } from 'vue'

interface Props {
  modelValue: string
  loading: boolean
  autoSaving: boolean
  statusText: string
}

const props = defineProps<Props>()

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

const textareaValue = computed({
  get: () => props.modelValue,
  set: (value: string | null | undefined) => {
    emit('update:modelValue', String(value ?? ''))
  },
})
</script>

<style scoped>
.memo-section {
  padding: 18px;
  border: 1px solid var(--app-border);
  border-radius: 20px;
  background: var(--app-surface);
  box-shadow: var(--app-shadow);
}

.memo-section__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.memo-section__title {
  margin: 0;
  color: var(--app-heading);
  font-size: 16px;
  font-weight: 700;
}

.memo-status {
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(var(--ion-color-primary-rgb), 0.06);
  color: var(--app-muted);
  font-size: 11px;
  font-weight: 600;
}

.memo-status--saving {
  background: var(--app-primary-soft-strong);
  color: var(--ion-color-primary);
}

.memo-textarea {
  --background: var(--app-surface-muted);
  --border-radius: 14px;
  --border-color: var(--app-border);
  --padding-start: 14px;
  --padding-end: 14px;
  min-height: 120px;
  font-size: 14px;
}

.memo-skeleton {
  padding: 14px;
  border: 1px solid var(--app-border);
  border-radius: 14px;
  background: var(--app-surface-muted);
}

.memo-skeleton__line {
  width: 100%;
  height: 14px;
}

.memo-skeleton__line + .memo-skeleton__line {
  margin-top: 10px;
}

.memo-skeleton__line--short {
  width: 66%;
}
</style>
