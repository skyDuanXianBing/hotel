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
  padding: 20px 18px;
  border: 1px solid rgba(97, 124, 177, 0.08);
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 12px 30px rgba(77, 98, 145, 0.08);
}

.memo-section__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.memo-section__title {
  margin: 0;
  color: #16233b;
  font-size: 18px;
  font-weight: 800;
  letter-spacing: -0.03em;
}

.memo-status {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 12px;
  border: 1px solid rgba(116, 163, 251, 0.1);
  border-radius: 999px;
  background: rgba(115, 164, 255, 0.08);
  color: #8c98b1;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.78);
  font-size: 11px;
  font-weight: 700;
}

.memo-status--saving {
  background: rgba(115, 164, 255, 0.14);
  color: #3f7cff;
}

.memo-editor,
.memo-skeleton {
  padding: 12px;
  border: 1px solid rgba(115, 139, 188, 0.1);
  border-radius: 20px;
  background: linear-gradient(180deg, rgba(248, 250, 255, 0.98), rgba(255, 255, 255, 0.94));
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.82);
}

.memo-textarea {
  --background: transparent;
  --border-radius: 16px;
  --border-color: transparent;
  --padding-start: 4px;
  --padding-end: 4px;
  min-height: 120px;
  color: #16233b;
  font-size: 14px;
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
