<script setup lang="ts">
import { useDashboardState } from '../state'

const {
  customPayloadText,
  hotelId,
  loading,
  scenarioSendResult,
  scenarios,
  selectedScenarioName,
  statusText,
  useDefaultPayload,
  loadScenarios,
  prettyJson,
  resetScenarioPayload,
  selectScenario,
  sendScenario,
} = useDashboardState()
</script>

<template>
  <section class="view-grid">
    <article class="panel">
      <div class="panel-header">
        <div>
          <h3>场景列表</h3>
          <p>手动发送预设 webhook 场景，不携带 test-support key。</p>
        </div>
        <button
          type="button"
          class="button secondary"
          :disabled="loading.scenarios"
          @click="loadScenarios"
        >
          刷新场景
        </button>
      </div>
      <label class="field">
        <span>酒店 ID</span>
        <input v-model="hotelId" placeholder="例如 1" @change="resetScenarioPayload" />
      </label>
      <div class="scenario-list">
        <button
          v-for="scenario in scenarios"
          :key="scenario.name"
          type="button"
          class="scenario-item"
          :class="{ active: scenario.name === selectedScenarioName }"
          @click="selectScenario(scenario.name)"
        >
          <strong>{{ scenario.label }}</strong>
          <span>{{ scenario.description }}</span>
        </button>
      </div>
      <p class="status-text">{{ statusText.scenarios }}</p>
    </article>

    <article class="panel">
      <div class="panel-header">
        <div>
          <h3>请求内容</h3>
          <p>使用服务端默认夹具时不会读取编辑器内容。</p>
        </div>
        <div class="actions">
          <button
            type="button"
            class="button secondary"
            :disabled="useDefaultPayload"
            @click="resetScenarioPayload"
          >
            重置示例
          </button>
          <button
            type="button"
            class="button primary"
            :disabled="loading.sendScenario"
            @click="sendScenario"
          >
            发送场景
          </button>
        </div>
      </div>
      <label class="toggle">
        <input v-model="useDefaultPayload" type="checkbox" />
        <span>使用服务端默认请求内容</span>
      </label>
      <textarea
        v-model="customPayloadText"
        class="json-editor"
        :disabled="useDefaultPayload"
        spellcheck="false"
      />
      <p class="status-text">{{ statusText.sendScenario }}</p>
      <pre class="code-panel compact">{{ prettyJson(scenarioSendResult) }}</pre>
    </article>
  </section>
</template>
