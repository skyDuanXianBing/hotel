<script setup lang="ts">
import { useDashboardState } from '../state'

const {
  config,
  loading,
  reversedLogs,
  statusText,
  clearLogs,
  formatDateTime,
  formatLogPreview,
  getLogKey,
  loadConfig,
  loadLogs,
  prettyJson,
} = useDashboardState()
</script>

<template>
  <section class="view-grid">
    <article class="panel wide">
      <div class="panel-header">
        <div>
          <h3>运行配置</h3>
          <p>敏感字段由后端脱敏展示。</p>
        </div>
        <button type="button" class="button secondary" :disabled="loading.config" @click="loadConfig">
          刷新配置
        </button>
      </div>
      <pre class="code-panel compact">{{ prettyJson(config) }}</pre>
    </article>

    <article class="panel wide">
      <div class="panel-header">
        <div>
          <h3>请求日志</h3>
          <p>包含 simulator API、webhook 发送和接收记录。</p>
        </div>
        <div class="actions">
          <button type="button" class="button secondary" :disabled="loading.logs" @click="loadLogs">
            刷新日志
          </button>
          <button type="button" class="button danger" :disabled="loading.logs" @click="clearLogs">
            清空日志
          </button>
        </div>
      </div>
      <div v-if="reversedLogs.length === 0" class="empty">暂无日志。</div>
      <div v-for="log in reversedLogs" :key="getLogKey(log)" class="log-row">
        <div class="log-head">
          <span class="status-pill">{{ log.type }}</span>
          <span class="status-pill">{{ log.method }}</span>
          <span class="status-pill">状态 {{ log.statusCode || '-' }}</span>
          <span>{{ formatDateTime(log.timestamp) }}</span>
        </div>
        <strong>{{ log.path }}</strong>
        <pre class="code-panel compact">{{ formatLogPreview(log) }}</pre>
      </div>
      <p class="status-text">{{ statusText.logs }}</p>
    </article>
  </section>
</template>
