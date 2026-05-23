<script setup lang="ts">
import { useDashboardState } from '../state'

const {
  loading,
  runData,
  runForm,
  runResult,
  selectedRooms,
  selectedRoomTypes,
  statusText,
  getRunSteps,
  isObject,
  labelForEntity,
  prettyJson,
  refreshRun,
  runE2E,
  statusLabel,
  valueForEntity,
} = useDashboardState()
</script>

<template>
  <section class="view-grid">
    <article class="panel">
      <div class="panel-header">
        <div>
          <h3>单次 E2E Run</h3>
          <p>构造一次预订并发送到 PMS，再读取验证结果。</p>
        </div>
      </div>
      <div class="form-grid single">
        <label class="field">
          <span>模式</span>
          <select v-model="runForm.mode">
            <option value="PUSH">推送模式</option>
            <option value="PULL">拉取模式</option>
          </select>
        </label>
        <label class="field">
          <span>渠道</span>
          <select v-model="runForm.channel">
            <option value="BOOKING">BOOKING</option>
            <option value="AIRBNB">AIRBNB</option>
          </select>
        </label>
        <label class="field">
          <span>场景</span>
          <select v-model="runForm.scenario">
            <option value="NEW">单房新预订</option>
            <option value="MULTI_ROOM">多房新预订</option>
            <option value="AIRBNB_NEW">Airbnb 新预订</option>
          </select>
        </label>
        <label class="field">
          <span>房型</span>
          <select v-model="runForm.roomTypeId">
            <option value="">自动选择</option>
            <option
              v-for="roomType in selectedRoomTypes"
              :key="valueForEntity(roomType, ['id'])"
              :value="valueForEntity(roomType, ['id'])"
            >
              {{ labelForEntity(roomType, '房型') }}
            </option>
          </select>
        </label>
        <label class="field">
          <span>房间</span>
          <select v-model="runForm.roomId">
            <option value="">自动选择</option>
            <option
              v-for="room in selectedRooms"
              :key="valueForEntity(room, ['id'])"
              :value="valueForEntity(room, ['id'])"
            >
              {{ labelForEntity(room, '房间') }}
            </option>
          </select>
        </label>
        <label class="field">
          <span>入住起始天数</span>
          <input v-model="runForm.stayStartDays" inputmode="numeric" placeholder="留空为自动生成" />
        </label>
      </div>
      <div class="actions">
        <button type="button" class="button primary" :disabled="loading.run" @click="runE2E">
          运行 E2E Run
        </button>
        <button type="button" class="button secondary" :disabled="loading.run" @click="refreshRun">
          刷新结果
        </button>
      </div>
      <p class="status-text">{{ statusText.run }}</p>
    </article>

    <article class="panel">
      <div class="panel-header">
        <div>
          <h3>Run 结果</h3>
          <p>发送结果、入库查询和 webhook 验证会在此汇总。</p>
        </div>
      </div>
      <div>
        <h4 style="font-size: 14px; font-weight: 700; margin-bottom: 8px; color: var(--text);">执行步骤</h4>
        <div v-if="getRunSteps(runData).length === 0" class="empty">尚未运行。</div>
        <div v-for="step in getRunSteps(runData)" :key="prettyJson(step)" class="step-row">
          <strong>{{ labelForEntity(step, '步骤') }}</strong>
          <span>{{ statusLabel(isObject(step) ? step.status : '') }}</span>
        </div>
      </div>
      <div>
        <h4 style="font-size: 14px; font-weight: 700; margin-bottom: 8px; color: var(--text);">原始响应</h4>
        <pre class="code-panel">{{ prettyJson(runResult) }}</pre>
      </div>
    </article>
  </section>
</template>
