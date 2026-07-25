<template>
  <ion-modal
    :is-open="isOpen"
    :breakpoints="[0, 0.8, 1]"
    :initial-breakpoint="0.8"
    class="channel-price-sheet"
    @didDismiss="$emit('dismiss')"
  >
    <ion-header translucent>
      <ion-toolbar class="channel-price-sheet__toolbar">
        <ion-title>{{ $t('channel.dialogs.editPriceRatio.title') }}</ion-title>
        <ion-buttons slot="end">
          <ion-button @click="$emit('dismiss')">{{ $t('home.section.close') }}</ion-button>
        </ion-buttons>
      </ion-toolbar>
    </ion-header>

    <ion-content class="channel-price-sheet__content">
      <section class="channel-price-sheet__hero" v-if="form">
        <strong>{{ form.channelName }}</strong>
      </section>

      <ion-list inset class="channel-price-sheet__list" v-if="form">
        <ion-item class="channel-price-sheet__item">
          <ion-select
            :value="form.direction"
            interface="action-sheet"
            :label="$t('iosStage5.channel.priceDirection')"
            label-placement="stacked"
            @ionChange="handleDirectionChange"
          >
            <ion-select-option value="expensive">{{ $t('channel.dialogs.editPriceRatio.expensive') }}</ion-select-option>
            <ion-select-option value="cheaper">{{ $t('channel.dialogs.editPriceRatio.cheaper') }}</ion-select-option>
          </ion-select>
        </ion-item>

        <ion-item class="channel-price-sheet__item channel-price-sheet__item--number">
          <ion-input
            :value="String(form.value)"
            type="number"
            inputmode="decimal"
            :label="$t('iosStage5.channel.adjustmentValue')"
            label-placement="stacked"
            @ionInput="handleValueInput"
          />
        </ion-item>

        <ion-item class="channel-price-sheet__item">
          <ion-select
            :value="form.unit"
            interface="action-sheet"
            :label="$t('iosStage5.channel.unit')"
            label-placement="stacked"
            @ionChange="handleUnitChange"
          >
            <ion-select-option value="PERCENTAGE">{{ $t('settingsStage4.pricingTools.adjustment.percentage') }} %</ion-select-option>
            <ion-select-option value="FIXED">
              {{ $t('settingsStage4.pricingTools.adjustment.fixed') }} {{ currencySymbol }}
            </ion-select-option>
          </ion-select>
        </ion-item>

        <ion-item class="channel-price-sheet__item channel-price-sheet__item--toggle">
          <ion-toggle :checked="form.autoSyncPrice" @ionChange="handleAutoSyncChange">
            {{ $t('iosStage5.channel.syncPrice') }}
          </ion-toggle>
        </ion-item>
      </ion-list>
    </ion-content>

    <ion-footer>
      <ion-toolbar class="channel-price-sheet__footer-toolbar">
        <div class="channel-price-sheet__footer">
          <ion-button fill="outline" @click="$emit('dismiss')">{{ $t('accommodation.common.cancel') }}</ion-button>
          <ion-button :disabled="!form || submitting" @click="handleSave">
            {{ submitting ? $t('channel.mobile.common.saving') : $t('home.manage.save') }}
          </ion-button>
        </div>
      </ion-toolbar>
    </ion-footer>
  </ion-modal>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'
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
import { useStoreStore } from '@/stores/store'
import { formatMoney, getCurrencySymbol } from '@/utils/formatters'
import type {
  PriceAdjustmentDirection,
  PriceAdjustmentEditorValue,
  PriceAdjustmentUnit,
} from '@/components/channel/channelUtils'

const { t } = useI18n()

const props = defineProps<{
  isOpen: boolean
  editor: PriceAdjustmentEditorValue | null
  submitting: boolean
}>()

const emit = defineEmits<{
  dismiss: []
  save: [value: PriceAdjustmentEditorValue]
}>()

const storeStore = useStoreStore()
const form = ref<PriceAdjustmentEditorValue | null>(null)
const currentCurrency = computed(() => storeStore.currentStore?.currency || 'CNY')
const currentMoneyContext = computed(() => ({ country: storeStore.currentStore?.country }))
const currencySymbol = computed(() =>
  getCurrencySymbol(currentCurrency.value, currentMoneyContext.value),
)

const previewText = computed(() => {
  if (!form.value) {
    return ''
  }
  if (form.value.value === 0) {
    return t('stage5Final.channel.equalBaseline')
  }

  const directionText = form.value.direction === 'expensive' ? t('channel.dialogs.editPriceRatio.expensive') : t('channel.dialogs.editPriceRatio.cheaper')
  const unitText =
    form.value.unit === 'PERCENTAGE'
      ? '%'
        : formatMoney(
            form.value.value,
            currentCurrency.value,
            { maximumFractionDigits: 2 },
            currentMoneyContext.value,
          )
  return t('stage5Final.channel.adjustmentPreview', {
    direction: directionText,
    value: form.value.unit === 'PERCENTAGE' ? form.value.value : '',
    unit: unitText,
  })
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
.channel-price-sheet {
  --border-radius: 24px 24px 0 0;
}

.channel-price-sheet__toolbar,
.channel-price-sheet__footer-toolbar {
  --background: rgba(255, 255, 255, 0.82);
  --border-color: transparent;
}

.channel-price-sheet__content {
  --background: var(--ios-pms-bg-page-plain);
  --padding-top: 8px;
  --padding-bottom: calc(18px + var(--app-safe-bottom));
}

.channel-price-sheet__hero {
  margin: 12px 16px 10px;
  padding: 16px 18px;
  border: 1px solid var(--app-border);
  border-radius: 22px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(52, 116, 246, 0.08));
  box-shadow: var(--app-shadow);
}

.channel-price-sheet__hero strong {
  display: block;
  color: var(--app-heading);
  font-size: 17px;
  font-weight: var(--ios-pms-weight-bold);
  letter-spacing: -0.01em;
}

.channel-price-sheet__list {
  margin: 0 16px;
  padding: 6px 0;
  border: 1px solid var(--app-border);
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: var(--app-shadow);
  overflow: hidden;
}

.channel-price-sheet__item {
  --background: transparent;
  --padding-start: 16px;
  --padding-end: 16px;
  --inner-padding-end: 0;
  --min-height: 74px;
  --border-color: var(--app-border);
}

.channel-price-sheet__item ion-input,
.channel-price-sheet__item ion-select {
  color: var(--app-heading);
}

.channel-price-sheet__item ion-input::part(label),
.channel-price-sheet__item ion-select::part(label) {
  margin-bottom: 8px;
  color: var(--app-muted);
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.02em;
}

.channel-price-sheet__item ion-input::part(native),
.channel-price-sheet__item ion-select::part(text) {
  color: var(--app-heading);
  font-size: 16px;
  overflow-wrap: anywhere;
  white-space: normal;
}

.channel-price-sheet__item--number ion-input::part(native) {
  font-variant-numeric: tabular-nums;
  line-height: 1.2;
  padding-top: 0;
  padding-bottom: 0;
}

.channel-price-sheet__item--number ion-input {
  width: 100%;
  min-height: 100%;
  display: flex;
  align-items: flex-start;
  --padding-top: 0;
  --padding-bottom: 0;
}

:deep(.channel-price-sheet__item--number .input-wrapper),
:deep(.channel-price-sheet__item--number .native-wrapper) {
  min-height: 100%;
  display: flex;
  align-items: flex-start;
}

:deep(.channel-price-sheet__item--number .native-input),
:deep(.channel-price-sheet__item--number input) {
  margin: 0;
  padding-top: 0;
  padding-bottom: 0;
  line-height: 1.2;
}

.channel-price-sheet__item--toggle {
  --min-height: 78px;
  --inner-padding-end: 4px;
}

.channel-price-sheet__item--toggle ion-toggle {
  width: 100%;
  color: var(--app-heading);
  font-size: 15px;
  --track-background: rgba(116, 138, 185, 0.22);
  --track-background-checked: var(--ion-color-primary);
  --handle-background: #ffffff;
  --handle-background-checked: #ffffff;
}

.channel-price-sheet__footer {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  padding: 10px 16px calc(6px + var(--app-safe-bottom));
}

.channel-price-sheet__footer ion-button {
  width: 100%;
  margin: 0;
  --border-radius: 14px;
  --box-shadow: none;
}
</style>
