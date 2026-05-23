<script setup lang="ts">
import { useDashboardState } from '../state'

const {
  lifecycleData,
  lifecycleResult,
  loading,
  messagingForm,
  messagingResult,
  statusText,
  getLifecycleSteps,
  isObject,
  labelForEntity,
  prettyJson,
  runLifecycle,
  runMessaging,
  statusLabel,
} = useDashboardState()
</script>

<template>
  <section class="view-grid">
    <article class="panel">
      <div class="panel-header">
        <div>
          <h3>生命周期测试</h3>
          <p>自动执行新预订、修改、取消三个步骤。</p>
        </div>
        <button
          type="button"
          class="button primary"
          :disabled="loading.lifecycle"
          @click="runLifecycle"
        >
          运行生命周期
        </button>
      </div>
      <div v-if="getLifecycleSteps(lifecycleData).length === 0" class="empty">
        尚未运行生命周期测试。
      </div>
      <div v-for="step in getLifecycleSteps(lifecycleData)" :key="prettyJson(step)" class="step-row">
        <strong>{{ labelForEntity(step, '步骤') }}</strong>
        <span>{{ statusLabel(isObject(step) && isObject(step.run) ? step.run.status : '') }}</span>
      </div>
      <p class="status-text">{{ statusText.lifecycle }}</p>
      <pre class="code-panel compact">{{ prettyJson(lifecycleResult) }}</pre>
    </article>

    <article class="panel">
      <div class="panel-header">
        <div>
          <h3>消息测试</h3>
          <p>发送客人消息 webhook，并通过 PMS test-support 查询验证。</p>
        </div>
        <button
          type="button"
          class="button primary"
          :disabled="loading.messaging"
          @click="runMessaging"
        >
          运行消息测试
        </button>
      </div>
      <div class="form-grid single">
        <label class="field">
          <span>渠道</span>
          <select v-model="messagingForm.channel">
            <option value="AIRBNB">AIRBNB</option>
            <option value="BOOKING">BOOKING</option>
          </select>
        </label>
        <label class="field">
          <span>消息内容</span>
          <input v-model="messagingForm.message" placeholder="输入客人消息" />
        </label>
        <label class="field">
          <span>Booking ID</span>
          <input v-model="messagingForm.bookingId" placeholder="留空自动生成" />
        </label>
        <label class="field">
          <span>Thread ID</span>
          <input v-model="messagingForm.threadId" placeholder="留空自动生成" />
        </label>
      </div>
      <p class="status-text">{{ statusText.messaging }}</p>
      <pre class="code-panel compact">{{ prettyJson(messagingResult) }}</pre>
    </article>
  </section>
</template>
