<script setup lang="ts">
import { computed, onMounted } from 'vue'

import { useDashboardState } from './state'
import LifecycleMessagingView from './views/LifecycleMessagingView.vue'
import LogsConfigView from './views/LogsConfigView.vue'
import ReadinessView from './views/ReadinessView.vue'
import RunView from './views/RunView.vue'
import ScenariosView from './views/ScenariosView.vue'

const {
  activeView,
  viewItems,
  readinessBadge,
  loadConfig,
  loadLogs,
  loadScenarios,
  selectView,
} = useDashboardState()

const activeViewTitle = computed(() => {
  for (const item of viewItems) {
    if (item.id === activeView.value) {
      return item.title
    }
  }
  return ''
})

onMounted(async () => {
  await loadConfig()
  await loadScenarios()
  await loadLogs()
})
</script>

<template>
  <main class="app-shell">
    <aside class="sidebar">
      <div class="brand">
        <div class="brand-mark">SU</div>
        <div>
          <h1>渠道模拟器</h1>
          <p>独立本地测试控制台</p>
        </div>
      </div>
      <nav class="nav-list" aria-label="模拟器视图">
        <button
          v-for="item in viewItems"
          :key="item.id"
          type="button"
          class="nav-item"
          :class="{ active: activeView === item.id }"
          @click="selectView(item.id)"
        >
          <span>{{ item.title }}</span>
          <small>{{ item.description }}</small>
        </button>
      </nav>
      <div class="sidebar-note">
        <strong>隔离边界</strong>
        <span>此控制台只调用 simulator API，不修改真实 client 流程。</span>
      </div>
    </aside>

    <section class="workspace">
      <header class="workspace-header">
        <div>
          <p class="eyebrow">Channel Simulator</p>
          <h2>{{ activeViewTitle }}</h2>
        </div>
        <div class="status-strip">
          <span class="status-pill" :class="readinessBadge.tone">
            {{ readinessBadge.text }}
          </span>
        </div>
      </header>

      <ReadinessView v-if="activeView === 'readiness'" />
      <RunView v-else-if="activeView === 'run'" />
      <LifecycleMessagingView v-else-if="activeView === 'lifecycle'" />
      <ScenariosView v-else-if="activeView === 'scenarios'" />
      <LogsConfigView v-else />
    </section>
  </main>
</template>
