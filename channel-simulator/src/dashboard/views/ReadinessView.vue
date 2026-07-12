<script setup lang="ts">
import { useDashboardState } from '../state'

const {
  config,
  credentials,
  cleanerDisplayName,
  loading,
  readinessParts,
  statusText,
  bootstrapLocalEnvironment,
  clearCredentials,
  formatEntityList,
  hasAvailableTestSupportKey,
  hasManagedTestSupportKey,
  hasSessionCredentials,
  hasCleanerTestSession,
  loadConfig,
  loadReadiness,
} = useDashboardState()
</script>

<template>
  <section class="view-grid">
    <article class="panel">
      <div class="panel-header">
        <div>
          <h3>一键初始化本地模拟环境</h3>
          <p>自动调用 PMS setup-local，生成当前页面会话可用的 PMS token、门店和房间数据。</p>
        </div>
        <div class="actions">
          <button type="button" class="button secondary" @click="clearCredentials">清除会话</button>
          <button
            type="button"
            class="button primary"
            :disabled="loading.bootstrap || !hasAvailableTestSupportKey"
            @click="bootstrapLocalEnvironment"
          >
            初始化本地环境
          </button>
        </div>
      </div>
      <div class="form-grid single">
        <label v-if="!hasManagedTestSupportKey" class="field">
          <span>本地 E2E 管理密钥</span>
          <input
            v-model="credentials.testSupportKey"
            type="password"
            autocomplete="off"
            placeholder="与 PMS 本地 test-support 配置一致"
          />
        </label>
        <div v-else class="metric">
          <span>本地 E2E 管理密钥</span>
          <strong>后端已代管</strong>
        </div>
      </div>
      <p class="status-text">
        管理密钥来自 PMS 本地 `CHANNEL_E2E_TEST_SUPPORT_KEY`；simulator 后端已配置时不需要在页面输入。
      </p>
      <div class="metric-list">
        <div class="metric">
          <span>初始化状态</span>
          <strong>{{ hasSessionCredentials ? '当前页面已初始化' : '等待初始化' }}</strong>
        </div>
        <div class="metric">
          <span>门店上下文</span>
          <strong>{{ credentials.storeId || '-' }}</strong>
        </div>
        <div class="metric">
          <span>PMS Token</span>
          <strong>{{ credentials.token ? '已生成，仅当前页面内存' : '-' }}</strong>
        </div>
        <div class="metric">
          <span>保洁测试账号</span>
          <strong>{{ hasCleanerTestSession ? '已就绪' : '未就绪' }}</strong>
        </div>
        <div class="metric">
          <span>保洁测试账号名称</span>
          <strong>{{ cleanerDisplayName || '-' }}</strong>
        </div>
        <div class="metric">
          <span>Su 酒店 ID</span>
          <strong>{{ readinessParts.suHotelId || '-' }}</strong>
        </div>
      </div>
      <p class="status-text">{{ statusText.bootstrap || statusText.credentials }}</p>
    </article>

    <article class="panel">
      <div class="panel-header">
        <div>
          <h3>环境配置</h3>
          <p>来自 simulator 后端运行时环境。</p>
        </div>
        <button type="button" class="button secondary" :disabled="loading.config" @click="loadConfig">
          刷新配置
        </button>
      </div>
      <div class="metric-list">
        <div class="metric">
          <span>PMS 地址</span>
          <strong>{{ config?.pmsBaseUrl || '-' }}</strong>
        </div>
        <div class="metric">
          <span>端口</span>
          <strong>{{ config?.port || '-' }}</strong>
        </div>
        <div class="metric">
          <span>默认酒店 ID</span>
          <strong>{{ config?.defaultHotelId || '-' }}</strong>
        </div>
        <div class="metric">
          <span>后端 PMS Token</span>
          <strong>{{ config?.pmsAuth?.hasToken ? '已配置' : '未配置' }}</strong>
        </div>
        <div class="metric">
          <span>管理密钥</span>
          <strong>{{ config?.testSupportAuth?.hasKey ? '后端已代管' : '需页面输入' }}</strong>
        </div>
      </div>
      <p class="status-text">{{ statusText.config }}</p>
    </article>

    <article class="panel wide">
      <div class="panel-header">
        <div>
          <h3>就绪检查</h3>
          <p>初始化后会携带当前页面内存中的 PMS token 和门店上下文检查 PMS 前置条件。</p>
        </div>
        <button
          type="button"
          class="button primary"
          :disabled="loading.readiness || !hasSessionCredentials"
          @click="loadReadiness"
        >
          检查就绪状态
        </button>
      </div>
      <div class="summary-grid">
        <div class="summary-block">
          <span>就绪状态</span>
          <strong>{{ readinessParts.ready === true ? '已就绪' : '未就绪或未检查' }}</strong>
        </div>
        <div class="summary-block">
          <span>Su 酒店 ID</span>
          <strong>{{ readinessParts.suHotelId || '-' }}</strong>
        </div>
        <div class="summary-block">
          <span>渠道数</span>
          <strong>{{ readinessParts.channels.length }}</strong>
        </div>
        <div class="summary-block">
          <span>房间数</span>
          <strong>{{ readinessParts.rooms.length }}</strong>
        </div>
      </div>
      <div class="split-grid">
        <div>
          <h4>缺失条件</h4>
          <p v-if="readinessParts.missingRequirements.length === 0" class="muted">暂无缺失项。</p>
          <ul v-else class="plain-list">
            <li v-for="item in readinessParts.missingRequirements" :key="String(item)">
              {{ item }}
            </li>
          </ul>
        </div>
        <div>
          <h4>渠道与库存</h4>
          <p class="muted">{{ formatEntityList(readinessParts.channels, '渠道') }}</p>
          <p class="muted">{{ formatEntityList(readinessParts.roomTypes, '房型') }}</p>
        </div>
      </div>
      <p class="status-text">{{ statusText.readiness }}</p>
    </article>
  </section>
</template>
