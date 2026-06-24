<template>
  <div class="door-locks-page" v-loading="pageLoading">
    <header class="page-header">
      <div>
        <h1>{{ t('settings.doorLocks.title') }}</h1>
        <p>{{ t('settings.doorLocks.subtitle') }}</p>
      </div>
      <div class="header-actions">
        <el-tag v-if="currentStoreName" type="info" effect="plain">
          {{ t('settings.doorLocks.currentStore', { name: currentStoreName }) }}
        </el-tag>
        <el-button :icon="Refresh" :loading="pageLoading" @click="handleReload">
          {{ t('settings.doorLocks.actions.reload') }}
        </el-button>
      </div>
    </header>

    <el-alert
      v-if="!currentStoreId"
      :title="t('settings.doorLocks.noStoreSelected')"
      type="warning"
      show-icon
      :closable="false"
      class="page-alert"
    />
    <el-alert
      v-if="errorMessage"
      :title="errorMessage"
      type="error"
      show-icon
      class="page-alert"
      @close="clearErrorMessage"
    />

    <el-tabs v-model="activeProvider" class="provider-tabs" @tab-change="handleProviderTabChange">
      <el-tab-pane
        v-for="provider in providerOptions"
        :key="provider.value"
        :name="provider.value"
      >
        <template #label>
          <span class="provider-tab-label">
            <span>{{ t(provider.labelKey) }}</span>
            <el-tag size="small" :type="getIntegrationTagType(provider.value)" effect="plain">
              {{ getIntegrationStatusLabel(provider.value) }}
            </el-tag>
          </span>
        </template>
      </el-tab-pane>
    </el-tabs>

    <section class="door-lock-section">
      <div class="section-header">
        <div>
          <h2>{{ t('settings.doorLocks.sections.configTitle') }}</h2>
          <p>{{ t('settings.doorLocks.sections.configDesc') }}</p>
        </div>
        <el-tag :type="activeIntegration ? 'success' : 'info'" effect="plain">
          {{ activeIntegration ? t('settings.doorLocks.status.configured') : t('settings.doorLocks.status.notConfigured') }}
        </el-tag>
      </div>

      <el-form
        ref="configFormRef"
        :model="activeConfigForm"
        :rules="configRules"
        label-width="140px"
        class="config-form"
      >
        <div class="form-grid">
          <el-form-item :label="t('settings.doorLocks.fields.enabled')" prop="enabled">
            <el-switch
              v-model="configForms[activeProvider].enabled"
              :active-text="t('settings.common.enabled')"
              :inactive-text="t('settings.common.disabled')"
            />
          </el-form-item>
          <el-form-item :label="t('settings.doorLocks.fields.accountName')" prop="name">
            <el-input
              v-model="configForms[activeProvider].name"
              :placeholder="t('settings.doorLocks.placeholders.accountName')"
              clearable
            />
          </el-form-item>

          <template v-if="activeProvider === PROVIDER_SWITCHBOT">
            <el-form-item
              :label="t('settings.doorLocks.fields.token')"
              prop="token"
              :required="isNewIntegration"
            >
              <el-input
                v-model="configForms.SWITCHBOT.token"
                :placeholder="t('settings.doorLocks.placeholders.switchBotToken')"
                show-password
                clearable
              />
            </el-form-item>
            <el-form-item
              :label="t('settings.doorLocks.fields.secret')"
              prop="secret"
              :required="isNewIntegration"
            >
              <el-input
                v-model="configForms.SWITCHBOT.secret"
                :placeholder="t('settings.doorLocks.placeholders.switchBotSecret')"
                show-password
                clearable
              />
            </el-form-item>
          </template>

          <template v-if="activeProvider === PROVIDER_TTLOCK">
            <el-form-item
              :label="t('settings.doorLocks.fields.clientId')"
              prop="clientId"
              :required="isNewIntegration"
            >
              <el-input
                v-model="configForms.TTLOCK.clientId"
                :placeholder="t('settings.doorLocks.placeholders.clientId')"
                clearable
              />
            </el-form-item>
            <el-form-item
              :label="t('settings.doorLocks.fields.clientSecret')"
              prop="clientSecret"
              :required="isNewIntegration"
            >
              <el-input
                v-model="configForms.TTLOCK.clientSecret"
                :placeholder="t('settings.doorLocks.placeholders.clientSecret')"
                show-password
                clearable
              />
            </el-form-item>
            <el-form-item
              :label="t('settings.doorLocks.fields.ttLockUsername')"
              prop="username"
              :required="isNewIntegration"
            >
              <el-input
                v-model="configForms.TTLOCK.username"
                :placeholder="t('settings.doorLocks.placeholders.ttLockUsername')"
                clearable
              />
            </el-form-item>
            <el-form-item
              :label="t('settings.doorLocks.fields.ttLockPassword')"
              prop="password"
              :required="isNewIntegration"
            >
              <el-input
                v-model="configForms.TTLOCK.password"
                :placeholder="t('settings.doorLocks.placeholders.ttLockPassword')"
                show-password
                clearable
              />
            </el-form-item>
          </template>
        </div>

        <div v-if="maskedCredentialList.length > 0" class="masked-credentials">
          <span v-for="item in maskedCredentialList" :key="item.label">
            {{ item.label }}: {{ item.value }}
          </span>
        </div>

        <div class="form-actions">
          <el-button
            type="primary"
            :icon="Link"
            :loading="savingConfig"
            :disabled="!currentStoreId"
            @click="handleSaveConfig"
          >
            {{ t('settings.doorLocks.actions.saveConfig') }}
          </el-button>
          <el-button
            :icon="Connection"
            :loading="testingConnection"
            :disabled="!activeIntegration || !currentStoreId"
            @click="handleTestConnection"
          >
            {{ t('settings.doorLocks.actions.testConnection') }}
          </el-button>
          <el-button
            v-if="activeProvider === PROVIDER_TTLOCK"
            :icon="Refresh"
            :loading="refreshingToken"
            :disabled="!activeIntegration || !currentStoreId"
            @click="handleRefreshToken"
          >
            {{ t('settings.doorLocks.actions.refreshToken') }}
          </el-button>
        </div>

        <p v-if="!activeIntegration" class="inline-hint">
          {{ t('settings.doorLocks.hints.saveBeforeTest') }}
        </p>
        <p v-if="activeProvider === PROVIDER_TTLOCK" class="inline-hint">
          {{ t('settings.doorLocks.hints.ttLockRefresh') }}
        </p>
      </el-form>

      <div v-if="activeIntegration" class="config-meta">
        <span>{{ t('settings.doorLocks.fields.lastTestAt') }}: {{ formatDateTime(activeIntegration.lastTestAt) }}</span>
        <span>{{ t('settings.doorLocks.fields.lastSyncedAt') }}: {{ formatDateTime(activeIntegration.lastSyncAt) }}</span>
        <span v-if="activeProvider === PROVIDER_TTLOCK">
          {{ t('settings.doorLocks.fields.tokenExpiresAt') }}: {{ formatDateTime(activeIntegration.tokenExpiresAt) }}
        </span>
      </div>
    </section>

    <section class="door-lock-section">
      <div class="section-header">
        <div>
          <h2>{{ t('settings.doorLocks.sections.devicesTitle') }}</h2>
          <p>{{ t('settings.doorLocks.sections.devicesDesc') }}</p>
        </div>
        <el-button
          type="primary"
          plain
          :icon="Refresh"
          :loading="syncingDevices"
          :disabled="!activeIntegration || !currentStoreId"
          @click="handleSyncDevices"
        >
          {{ t('settings.doorLocks.actions.syncDevices') }}
        </el-button>
      </div>

      <el-table
        :data="devices"
        border
        class="data-table"
        v-loading="resourceLoading"
      >
        <el-table-column :label="t('settings.doorLocks.fields.device')" min-width="220">
          <template #default="{ row }">
            <div class="primary-cell">
              <span>{{ getDeviceDisplayName(row) }}</span>
              <el-tag v-if="getDeviceBoundRoomLabel(row)" size="small" type="warning" effect="plain">
                {{ getDeviceBoundRoomLabel(row) }}
              </el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column :label="t('settings.doorLocks.fields.status')" width="150">
          <template #default="{ row }">
            <div class="status-cell">
              <el-tag :type="getDeviceStatusTagType(row)" effect="plain">
                {{ getDeviceStatusLabel(row) }}
              </el-tag>
              <span v-if="getDeviceStatusSourceLabel(row)" class="status-source">
                {{ getDeviceStatusSourceLabel(row) }}
              </span>
            </div>
          </template>
        </el-table-column>
        <el-table-column :label="t('settings.doorLocks.fields.capabilities')" min-width="180">
          <template #default="{ row }">
            <div class="tag-list">
              <el-tag v-for="capability in getDeviceCapabilityLabels(row)" :key="capability" size="small">
                {{ capability }}
              </el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column :label="t('settings.doorLocks.fields.battery')" width="110">
          <template #default="{ row }">
            {{ getDeviceBatteryLabel(row) }}
          </template>
        </el-table-column>
        <el-table-column :label="t('settings.doorLocks.fields.lastSyncedAt')" min-width="160">
          <template #default="{ row }">
            {{ formatDateTime(row.lastSyncedAt || row.updatedAt) }}
          </template>
        </el-table-column>
        <template #empty>
          <el-empty :description="t('settings.doorLocks.empty.devices')" />
        </template>
      </el-table>
    </section>

    <section class="door-lock-section">
      <div class="section-header">
        <div>
          <h2>{{ t('settings.doorLocks.sections.bindingsTitle') }}</h2>
          <p>{{ t('settings.doorLocks.sections.bindingsDesc') }}</p>
        </div>
        <div class="binding-toolbar">
          <el-select
            v-model="roomTypeFilter"
            :placeholder="t('settings.doorLocks.placeholders.roomType')"
            clearable
            filterable
            @change="handleRoomTypeChange"
            @clear="handleClearRoomTypeFilter"
          >
            <el-option :label="t('settings.common.all')" :value="EMPTY_ROOM_TYPE_FILTER" />
            <el-option
              v-for="roomType in roomTypeOptions"
              :key="roomType.id"
              :label="roomType.name"
              :value="roomType.id"
            />
          </el-select>
        </div>
      </div>

      <el-alert
        :title="t('settings.doorLocks.hints.roomIdBinding')"
        type="info"
        show-icon
        :closable="false"
        class="section-alert"
      />
      <el-alert
        :title="t('settings.doorLocks.hints.roleDeviceReuse')"
        type="warning"
        show-icon
        :closable="false"
        class="section-alert"
      />

      <el-table
        :data="filteredRooms"
        border
        class="data-table"
        v-loading="resourceLoading"
      >
        <el-table-column :label="t('settings.doorLocks.fields.room')" min-width="140">
          <template #default="{ row }">
            <span class="primary-text">{{ getRoomDisplayName(row) }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('settings.doorLocks.fields.roomType')" min-width="160">
          <template #default="{ row }">
            {{ getRoomTypeName(row) }}
          </template>
        </el-table-column>
        <el-table-column :label="t('settings.doorLocks.fields.currentBinding')" min-width="230">
          <template #default="{ row }">
            <div class="binding-summary">
              <div class="role-line">
                <span class="role-label">
                  {{ t('settings.doorLocks.fields.currentControlDevice') }}
                </span>
                <span class="role-value">{{ getRoomBoundRoleDeviceName(row, ROLE_CONTROL) }}</span>
              </div>
              <div class="role-line">
                <span class="role-label">
                  {{ t('settings.doorLocks.fields.currentPasscodeDevice') }}
                </span>
                <span class="role-value">{{ getRoomBoundRoleDeviceName(row, ROLE_PASSCODE) }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column :label="t('settings.doorLocks.fields.bindingDevices')" min-width="320">
          <template #default="{ row }">
            <div class="role-selection-list">
              <div class="role-selection-row">
                <span class="role-label">{{ t('settings.doorLocks.fields.controlDevice') }}</span>
                <el-select
                  :model-value="getSelectedRoleDeviceKey(row, ROLE_CONTROL)"
                  :placeholder="t('settings.doorLocks.placeholders.selectControlDevice')"
                  clearable
                  filterable
                  class="device-select"
                  @update:model-value="handleControlDeviceChange(row, $event)"
                >
                  <el-option
                    v-for="device in getRoleDeviceOptions(ROLE_CONTROL)"
                    :key="getDeviceKey(device)"
                    :label="getDeviceDisplayName(device)"
                    :value="getDeviceKey(device)"
                    :disabled="isRoleDeviceOptionDisabled(device, row, ROLE_CONTROL)"
                  />
                </el-select>
              </div>
              <div class="role-selection-row">
                <span class="role-label">{{ t('settings.doorLocks.fields.passcodeDevice') }}</span>
                <el-select
                  :model-value="getSelectedRoleDeviceKey(row, ROLE_PASSCODE)"
                  :placeholder="t('settings.doorLocks.placeholders.selectPasscodeDevice')"
                  clearable
                  filterable
                  class="device-select"
                  @update:model-value="handlePasscodeDeviceChange(row, $event)"
                >
                  <el-option
                    v-for="device in getRoleDeviceOptions(ROLE_PASSCODE)"
                    :key="getDeviceKey(device)"
                    :label="getDeviceDisplayName(device)"
                    :value="getDeviceKey(device)"
                    :disabled="isRoleDeviceOptionDisabled(device, row, ROLE_PASSCODE)"
                  />
                </el-select>
              </div>
              <div v-if="getRoomBindingHintMessages(row).length" class="binding-hints">
                <p v-for="message in getRoomBindingHintMessages(row)" :key="message">
                  {{ message }}
                </p>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column :label="t('settings.common.actions')" width="190" fixed="right">
          <template #default="{ row }">
            <el-button
              link
              type="primary"
              :loading="isSavingRoom(row)"
              :disabled="!canSaveRoomBinding(row)"
              @click="handleSaveBinding(row)"
            >
              {{ t('settings.doorLocks.actions.saveBinding') }}
            </el-button>
            <el-button
              link
              type="danger"
              :loading="isUnbindingRoom(row)"
              :disabled="!getRoomBinding(row)"
              @click="handleUnbind(row)"
            >
              {{ t('settings.doorLocks.actions.unbind') }}
            </el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty :description="t('settings.doorLocks.empty.rooms')" />
        </template>
      </el-table>
    </section>
  </div>
</template>

<script setup lang="ts">
import { Connection, Link, Refresh } from '@element-plus/icons-vue'
import { useDoorLockSettings } from './door-locks/useDoorLockSettings'

const {
  PROVIDER_SWITCHBOT,
  PROVIDER_TTLOCK,
  EMPTY_ROOM_TYPE_FILTER,
  ROLE_CONTROL,
  ROLE_PASSCODE,
  t,
  providerOptions,
  activeProvider,
  pageLoading,
  resourceLoading,
  savingConfig,
  testingConnection,
  refreshingToken,
  syncingDevices,
  errorMessage,
  roomTypeFilter,
  devices,
  configForms,
  currentStoreId,
  currentStoreName,
  activeIntegration,
  activeConfigForm,
  isNewIntegration,
  configRules,
  roomTypeOptions,
  filteredRooms,
  maskedCredentialList,
  configFormRef,
  clearErrorMessage,
  handleSaveConfig,
  handleTestConnection,
  handleRefreshToken,
  handleSyncDevices,
  handleSaveBinding,
  handleUnbind,
  handleControlDeviceChange,
  handlePasscodeDeviceChange,
  handleProviderTabChange,
  handleRoomTypeChange,
  handleClearRoomTypeFilter,
  handleReload,
  getIntegrationTagType,
  getIntegrationStatusLabel,
  getDeviceKey,
  getDeviceDisplayName,
  getDeviceStatusLabel,
  getDeviceStatusTagType,
  getDeviceCapabilityLabels,
  getDeviceStatusSourceLabel,
  getRoomDisplayName,
  getRoomTypeName,
  getRoomBinding,
  getRoomBoundRoleDeviceName,
  getSelectedRoleDeviceKey,
  getRoleDeviceOptions,
  isRoleDeviceOptionDisabled,
  getRoomBindingHintMessages,
  getDeviceBoundRoomLabel,
  isSavingRoom,
  isUnbindingRoom,
  canSaveRoomBinding,
  getDeviceBatteryLabel,
  formatDateTime,
} = useDoorLockSettings()
</script>

<style scoped>
.door-locks-page {
  min-height: calc(100vh - 120px);
  padding: 20px;
  background: #f6f8fb;
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.page-header h1 {
  margin: 0;
  color: #1f2937;
  font-size: 24px;
  font-weight: 600;
}

.page-header p {
  margin: 8px 0 0;
  color: #667085;
  font-size: 14px;
}

.header-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  flex-wrap: wrap;
}

.page-alert {
  margin-bottom: 16px;
}

.provider-tabs {
  margin-bottom: 12px;
}

.provider-tab-label {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.door-lock-section {
  padding: 18px;
  margin-bottom: 16px;
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
}

.section-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.section-header h2 {
  margin: 0;
  color: #1f2937;
  font-size: 18px;
  font-weight: 600;
}

.section-header p {
  margin: 6px 0 0;
  color: #667085;
  font-size: 13px;
  line-height: 1.5;
}

.config-form {
  max-width: 980px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(280px, 1fr));
  column-gap: 24px;
}

.masked-credentials {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  padding: 10px 14px;
  margin: 4px 0 16px 140px;
  color: #475467;
  font-size: 13px;
  background: #f8fafc;
  border: 1px dashed #cbd5e1;
  border-radius: 6px;
}

.form-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-left: 140px;
}

.inline-hint {
  margin: 10px 0 0 140px;
  color: #667085;
  font-size: 13px;
}

.config-meta {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
  padding-top: 14px;
  margin-top: 16px;
  color: #667085;
  font-size: 13px;
  border-top: 1px solid #eef2f7;
}

.binding-toolbar {
  min-width: 220px;
}

.section-alert {
  margin-bottom: 10px;
}

.data-table {
  width: 100%;
}

.primary-cell {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.primary-cell span,
.primary-text {
  overflow: hidden;
  color: #1f2937;
  font-weight: 500;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tag-list {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.status-cell {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
}

.status-source {
  color: #667085;
  font-size: 12px;
  line-height: 1.3;
}

.binding-summary,
.role-selection-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.role-line,
.role-selection-row {
  display: grid;
  grid-template-columns: 92px minmax(0, 1fr);
  gap: 8px;
  align-items: center;
}

.role-label {
  color: #667085;
  font-size: 12px;
  line-height: 1.4;
}

.role-value {
  min-width: 0;
  overflow: hidden;
  color: #1f2937;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.binding-hints {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding-top: 2px;
}

.binding-hints p {
  margin: 0;
  color: #b54708;
  font-size: 12px;
  line-height: 1.4;
}

.device-select {
  width: 100%;
}

@media (max-width: 900px) {
  .page-header,
  .section-header {
    flex-direction: column;
  }

  .header-actions,
  .binding-toolbar {
    width: 100%;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }

  .masked-credentials,
  .form-actions,
  .inline-hint {
    margin-left: 0;
  }

  .role-line,
  .role-selection-row {
    grid-template-columns: 1fr;
    gap: 4px;
  }
}
</style>
