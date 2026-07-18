<template>
  <section class="memo-section mobile-dashboard-surface">
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
      <div class="memo-editor">
        <ion-textarea
          v-model="textareaValue"
          auto-grow
          class="memo-textarea"
          fill="outline"
          placeholder="记录今日重点事项或待办"
          :rows="4"
        />
      </div>
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
  padding: 18px 16px 20px;
  border-radius: var(--ios-pms-radius-card);
}

.memo-section__header {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: 8px;
  margin-bottom: 14px;
}

.memo-section__title {
  margin: 0;
  color: var(--ios-pms-text-primary);
  font-size: 20px;
  font-weight: var(--ios-pms-weight-medium);
  letter-spacing: 0;
}

.memo-status {
  display: inline-flex;
  align-items: center;
  min-width: 0;
  padding: 0;
  border: none;
  background: transparent;
  color: var(--ios-pms-text-soft);
  box-shadow: none;
  font-size: 13px;
  font-weight: 400;
}

.memo-status--saving {
  background: transparent;
  color: #3f7cff;
}

.memo-status::before {
  content: '（';
}

.memo-status::after {
  content: '）';
}

.memo-editor,
.memo-skeleton {
  padding: 0;
  border: 1px solid rgba(116, 138, 185, 0.06);
  border-radius: 16px;
  background: #ffffff;
  box-shadow: 0 2px 10px rgba(77, 98, 145, 0.035);
  overflow: hidden;
}

.memo-textarea {
  --background: #ffffff;
  --border-radius: 16px;
  --border-color: transparent;
  --border-width: 0;
  --highlight-color-focused: transparent;
  --padding-start: 16px;
  --padding-end: 16px;
  --padding-top: 15px;
  --padding-bottom: 15px;
  min-height: 144px;
  color: var(--ios-pms-text-primary);
  font-size: 15px;
  line-height: 1.5;
}

.memo-textarea::part(native) {
  border: none;
  box-shadow: none;
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
