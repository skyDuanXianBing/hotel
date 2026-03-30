<template>
  <ion-modal
    :is-open="isOpen"
    :breakpoints="[0, 0.8, 1]"
    :initial-breakpoint="0.8"
    @didDismiss="$emit('dismiss')"
  >
    <ion-header translucent>
      <ion-toolbar>
        <ion-title>编辑价格比例</ion-title>
        <ion-buttons slot="end">
          <ion-button @click="$emit('dismiss')">关闭</ion-button>
        </ion-buttons>
      </ion-toolbar>
    </ion-header>

    <ion-content>
      <section class="channel-price-sheet__hero" v-if="form">
        <strong>{{ form.channelName }}</strong>
      </section>

      <ion-list inset v-if="form">
        <ion-item>
          <ion-select
            :value="form.direction"
            interface="action-sheet"
            label="价格方向"
            label-placement="stacked"
            @ionChange="handleDirectionChange"
          >
            <ion-select-option value="expensive">更贵</ion-select-option>
            <ion-select-option value="cheaper">更便宜</ion-select-option>
          </ion-select>
        </ion-item>

        <ion-item>
          <ion-input
            :value="String(form.value)"
            type="number"
            inputmode="decimal"
            label="调整数值"
            label-placement="stacked"
            @ionInput="handleValueInput"
          />
        </ion-item>

        <ion-item>
          <ion-select
            :value="form.unit"
            interface="action-sheet"
            label="单位"
            label-placement="stacked"
            @ionChange="handleUnitChange"
          >
            <ion-select-option value="%">百分比 %</ion-select-option>
            <ion-select-option value="¥">固定金额 ¥</ion-select-option>
          </ion-select>
        </ion-item>

        <ion-item>
          <ion-toggle :checked="form.autoSyncPrice" @ionChange="handleAutoSyncChange">
            自动同步价格到此渠道
          </ion-toggle>
        </ion-item>
      </ion-list>
    </ion-content>

    <ion-footer>
      <ion-toolbar>
        <div class="channel-price-sheet__footer">
          <ion-button fill="outline" @click="$emit('dismiss')">取消</ion-button>
          <ion-button :disabled="!form || submitting" @click="handleSave">
            {{ submitting ? '保存中...' : '保存' }}
          </ion-button>
        </div>
      </ion-toolbar>
    </ion-footer>
  </ion-modal>
</template>

<script setup lang="ts">
import {
  IonButton,
  IonButtons,
  IonContent,
  IonFooter,
  IonHeader,
  IonInput,
  IonItem,
  IonList,
  IonModal,
  IonSelect,
  IonSelectOption,
  IonTitle,
  IonToggle,
  IonToolbar,
} from '@ionic/vue'
import { computed, ref, watch } from 'vue'
import type {
  PriceAdjustmentDirection,
  PriceAdjustmentEditorValue,
  PriceAdjustmentUnit,
} from '@/components/channel/channelUtils'

const props = defineProps<{
  isOpen: boolean
  editor: PriceAdjustmentEditorValue | null
  submitting: boolean
}>()

const emit = defineEmits<{
  dismiss: []
  save: [value: PriceAdjustmentEditorValue]
}>()

const form = ref<PriceAdjustmentEditorValue | null>(null)

const previewText = computed(() => {
  if (!form.value) {
    return ''
  }
  if (form.value.value === 0) {
    return '当前为等同于基准价'
  }

  const directionText = form.value.direction === 'expensive' ? '更贵' : '更便宜'
  const unitText = form.value.unit === '%' ? '%' : '元'
  return `保存后将变为比基准价${directionText} ${form.value.value}${unitText}`
})

function resetForm() {
  if (!props.editor) {
    form.value = null
    return
  }

  form.value = {
    channelId: props.editor.channelId,
    channelName: props.editor.channelName,
    channelCode: props.editor.channelCode,
    direction: props.editor.direction,
    value: props.editor.value,
    unit: props.editor.unit,
    autoSyncPrice: props.editor.autoSyncPrice,
  }
}

function handleDirectionChange(event: CustomEvent) {
  if (!form.value) {
    return
  }
  form.value.direction = event.detail.value as PriceAdjustmentDirection
}

function handleUnitChange(event: CustomEvent) {
  if (!form.value) {
    return
  }
  form.value.unit = event.detail.value as PriceAdjustmentUnit
}

function handleValueInput(event: CustomEvent) {
  if (!form.value) {
    return
  }

  const nextValue = Number(event.detail.value || 0)
  if (Number.isNaN(nextValue) || nextValue < 0) {
    form.value.value = 0
    return
  }

  form.value.value = nextValue
}

function handleAutoSyncChange(event: CustomEvent) {
  if (!form.value) {
    return
  }
  form.value.autoSyncPrice = Boolean(event.detail.checked)
}

function handleSave() {
  if (!form.value) {
    return
  }
  emit('save', form.value)
}

watch(
  () => props.isOpen,
  (isOpen) => {
    if (isOpen) {
      resetForm()
    }
  },
)
</script>

<style scoped>
.channel-price-sheet__hero {
  margin: 16px;
  padding: 18px;
  border-radius: 20px;
  background: var(--app-primary-soft);
}

.channel-price-sheet__hero strong {
  display: block;
  color: var(--app-heading);
  font-size: 18px;
}

.channel-price-sheet__hero p {
  margin: 8px 0 0;
  color: var(--app-muted);
  font-size: 14px;
  line-height: 1.6;
}

.channel-price-sheet__footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 0 16px;
}
</style>
