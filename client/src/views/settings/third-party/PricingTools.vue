<template>
  <div class="pricing-tools-container">
    <!-- 连接状态视图 -->
    <div v-if="!showConfigPage" class="connection-view">
      <div class="pricelabs-card">
        <div class="card-content">
          <div class="logo-section">
            <div class="pricelabs-logo">
              <div class="logo-icon">
                <svg viewBox="0 0 40 40" xmlns="http://www.w3.org/2000/svg">
                  <rect
                    x="10"
                    y="10"
                    width="20"
                    height="20"
                    fill="#E53935"
                    transform="rotate(45 20 20)"
                  />
                </svg>
              </div>
              <span class="logo-text">PriceLabs</span>
            </div>
          </div>
          <p class="description">{{ t('settingsStage4.pricingTools.intro.description') }}</p>
          <el-button type="primary" size="large" @click="handleConfigure">
            {{ t('settingsStage4.pricingTools.actions.configure') }}
          </el-button>
        </div>
      </div>
    </div>

    <!-- 配置列表视图 -->
    <div v-else class="config-view">
      <!-- 面包屑导航 -->
      <div class="breadcrumb-section">
        <el-breadcrumb separator=">">
          <el-breadcrumb-item>
            <el-button link @click="showConfigPage = false">{{
              t('settings.layout.groups.thirdParty')
            }}</el-button>
          </el-breadcrumb-item>
          <el-breadcrumb-item>PriceLabs</el-breadcrumb-item>
        </el-breadcrumb>
      </div>

      <!-- 标签页 -->
      <el-tabs v-model="activeTab" class="config-tabs" @tab-change="handleTabChange">
        <!-- 房源连接标签页 -->
        <el-tab-pane :label="t('settingsStage4.pricingTools.tabs.connections')" name="connections">
          <div class="connections-panel">
            <!-- 说明区域 -->
            <div class="instructions-banner">
              <div class="instruction-item">
                <span class="instruction-number">1.</span>
                <span class="instruction-text">{{
                  t('settingsStage4.pricingTools.instructions.beforeStore')
                }}</span>
              </div>
              <div class="instruction-item">
                <span class="instruction-number">2.</span>
                <span class="instruction-text">{{
                  t('settingsStage4.pricingTools.instructions.connectionDelay')
                }}</span>
              </div>
              <div class="instruction-item">
                <span class="instruction-number">3.</span>
                <span class="instruction-text">{{
                  t('settingsStage4.pricingTools.instructions.defaultSync')
                }}</span>
              </div>
            </div>

            <!-- 集成配置区域 -->
            <div class="integration-config-section">
              <div class="section-row">
                <div class="section-label">
                  <h3 class="label-title">
                    {{ t('settingsStage4.pricingTools.sections.accountConfig') }}
                  </h3>
                  <p class="label-desc">
                    {{ t('settingsStage4.pricingTools.sections.accountDesc') }}
                  </p>
                </div>
                <div class="config-form">
                  <el-select
                    v-model="defaultAccountId"
                    :placeholder="
                      t('settingsStage4.pricingTools.placeholders.selectDefaultAccount')
                    "
                    class="default-account-select"
                    :disabled="integration.isEnabled"
                  >
                    <el-option
                      v-for="account in accounts"
                      :key="account.id"
                      :label="`${account.accountName} (${account.priceLabsEmail})`"
                      :value="account.id"
                    />
                  </el-select>
                  <el-button
                    type="primary"
                    :disabled="!defaultAccountId || integration.isEnabled"
                    @click="handleSaveDefaultAccountConfig"
                  >
                    {{ t('settingsStage4.pricingTools.actions.saveConfig') }}
                  </el-button>
                  <el-text v-if="integration.isEnabled" type="info" size="small">
                    {{ t('settingsStage4.pricingTools.hints.disableBeforeEditEmail') }}
                  </el-text>
                </div>
              </div>
            </div>

            <!-- 集成状态区域 -->
            <div class="integration-status-section">
              <div class="section-row">
                <div class="section-label">
                  <h3 class="label-title">
                    {{ t('settingsStage4.pricingTools.sections.integrationStatus') }}
                  </h3>
                  <p class="label-desc">
                    {{
                      integration.isEnabled
                        ? t('settingsStage4.pricingTools.status.integrationEnabled')
                        : t('settingsStage4.pricingTools.status.integrationDisabled')
                    }}
                  </p>
                </div>
                <div class="status-actions">
                  <el-tag :type="integration.isEnabled ? 'success' : 'info'" size="large">
                    {{
                      integration.isEnabled
                        ? t('settingsStage4.pricingTools.status.enabled')
                        : t('settingsStage4.pricingTools.status.disabled')
                    }}
                  </el-tag>
                  <el-switch
                    v-model="integration.isEnabled"
                    :loading="toggleLoading"
                    @change="handleToggleIntegration"
                  />
                </div>
              </div>
              <div v-if="integration.isEnabled" class="sync-stats">
                <div class="stat-item">
                  <span class="stat-label">{{
                    t('settingsStage4.pricingTools.stats.connectedRoomTypes')
                  }}</span>
                  <span class="stat-value">{{ integration.connectedRoomTypeCount || 0 }}</span>
                </div>
                <div class="stat-item">
                  <span class="stat-label">{{
                    t('settingsStage4.pricingTools.stats.lastPriceSync')
                  }}</span>
                  <span class="stat-value">{{
                    formatDateTime(integration.lastPriceSyncAt) || '-'
                  }}</span>
                </div>
                <div class="stat-item">
                  <span class="stat-label">{{
                    t('settingsStage4.pricingTools.stats.syncSuccessRate')
                  }}</span>
                  <span class="stat-value">{{ calculateSuccessRate() }}</span>
                </div>
              </div>
            </div>

            <!-- 工具栏 -->
            <div class="integration-config-section">
              <div class="section-row">
                <div class="section-label">
                  <h3 class="label-title">
                    {{ t('settingsStage4.pricingTools.sections.multiAccount') }}
                  </h3>
                  <p class="label-desc">
                    {{ t('settingsStage4.pricingTools.sections.multiAccountDesc') }}
                  </p>
                </div>
                <div class="config-form">
                  <el-button type="primary" @click="openCreateAccountDialog">{{
                    t('settingsStage4.pricingTools.actions.addAccount')
                  }}</el-button>
                </div>
              </div>
              <el-table
                :data="accounts"
                size="small"
                class="config-table account-table"
                v-loading="accountsLoading"
              >
                <el-table-column
                  prop="accountName"
                  :label="t('settingsStage4.pricingTools.columns.accountName')"
                  min-width="180"
                />
                <el-table-column
                  prop="priceLabsEmail"
                  :label="t('settingsStage4.pricingTools.columns.priceLabsEmail')"
                  min-width="220"
                />
                <el-table-column :label="t('settings.common.status')" width="120" align="center">
                  <template #default="{ row }">
                    <el-tag :type="row.isEnabled ? 'success' : 'info'" size="small">
                      {{
                        row.isEnabled
                          ? t('settingsStage4.pricingTools.status.enabled')
                          : t('settingsStage4.pricingTools.status.disabled')
                      }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column
                  :label="t('settingsStage4.pricingTools.columns.linkedRoomTypes')"
                  width="120"
                  align="center"
                >
                  <template #default="{ row }">
                    {{ row.connectionCount ?? 0 }}
                  </template>
                </el-table-column>
                <el-table-column :label="t('settings.common.actions')" width="220" align="center">
                  <template #default="{ row }">
                    <el-button link type="primary" @click="selectAccount(row.id)">{{
                      t('settingsStage4.pricingTools.actions.viewRoomTypes')
                    }}</el-button>
                    <el-button link type="primary" @click="openEditAccountDialog(row)">{{
                      t('settings.common.edit')
                    }}</el-button>
                    <el-button
                      link
                      :type="row.isEnabled ? 'warning' : 'success'"
                      @click="handleToggleAccount(row)"
                    >
                      {{
                        row.isEnabled ? t('settings.common.disable') : t('settings.common.enable')
                      }}
                    </el-button>
                    <el-button link type="danger" @click="handleDeleteAccount(row)">{{
                      t('settings.common.delete')
                    }}</el-button>
                  </template>
                </el-table-column>
              </el-table>
              <div v-if="selectedAccountName" class="account-filter-tip">
                {{
                  t('settingsStage4.pricingTools.accountFilterTip', { name: selectedAccountName })
                }}
                <el-button link type="primary" @click="clearSelectedAccount">{{
                  t('settingsStage4.pricingTools.actions.viewAll')
                }}</el-button>
              </div>
            </div>

            <div class="toolbar">
              <div class="toolbar-left">
                <span class="filter-label">{{
                  t('settingsStage4.pricingTools.filters.connectionStatus')
                }}</span>
                <el-select
                  v-model="connectionFilter"
                  :placeholder="t('settingsStage4.pricingTools.placeholders.select')"
                  style="width: 120px"
                >
                  <el-option :label="t('settings.common.all')" value="all" />
                  <el-option
                    :label="t('settingsStage4.pricingTools.status.connected')"
                    value="connected"
                  />
                  <el-option
                    :label="t('settingsStage4.pricingTools.status.disconnected')"
                    value="disconnected"
                  />
                </el-select>
              </div>
              <el-button type="primary" @click="handleAddConnection">{{
                t('settingsStage4.pricingTools.actions.addConnection')
              }}</el-button>
            </div>

            <!-- 配置表格 -->
            <el-table
              :data="filteredConnections"
              class="config-table"
              v-loading="connectionsLoading"
            >
              <el-table-column
                prop="accountName"
                :label="t('settingsStage4.pricingTools.columns.account')"
                min-width="160"
              />
              <el-table-column
                prop="roomTypeName"
                :label="t('settingsStage4.cleaningSettings.fields.roomType')"
                min-width="180"
              />
              <el-table-column
                prop="pricePlanName"
                :label="t('settingsStage4.pricePlan.columns.pricePlan')"
                min-width="150"
              />
              <el-table-column
                prop="priceLabsListingId"
                :label="t('settingsStage4.pricingTools.columns.listingId')"
                min-width="200"
              />
              <el-table-column :label="t('settings.common.status')" width="120" align="center">
                <template #default="{ row }">
                  <el-tag
                    :type="
                      row.syncStatus === 'connected'
                        ? 'success'
                        : row.syncStatus === 'error'
                          ? 'danger'
                          : 'info'
                    "
                    size="small"
                  >
                    {{ getSyncStatusText(row.syncStatus) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column
                :label="t('settingsStage4.pricingTools.columns.recentSync')"
                width="180"
                align="center"
              >
                <template #default="{ row }">
                  {{ formatDateTime(row.lastSyncAt) || '-' }}
                </template>
              </el-table-column>
              <el-table-column
                :label="t('settings.common.actions')"
                width="180"
                align="center"
                fixed="right"
              >
                <template #default="{ row }">
                  <el-button
                    link
                    type="primary"
                    @click="handleOpenStatusDialog({ type: 'listing', id: row.priceLabsListingId })"
                  >
                    {{ t('settingsStage4.pricingTools.actions.statusQuery') }}
                  </el-button>
                  <el-button
                    link
                    type="primary"
                    :loading="syncingRoomTypeIds.includes(row.roomTypeId)"
                    @click="handleSyncRoomType(row)"
                  >
                    {{ t('settingsStage4.pricingTools.actions.sync') }}
                  </el-button>
                  <el-button
                    link
                    :type="row.isEnabled ? 'warning' : 'success'"
                    @click="handleToggleConnection(row)"
                  >
                    {{ row.isEnabled ? t('settings.common.disable') : t('settings.common.enable') }}
                  </el-button>
                  <el-button link type="danger" @click="handleDeleteConnection(row)">
                    {{ t('settings.common.delete') }}
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-tab-pane>

        <!-- 同步日志标签页 -->
        <el-tab-pane :label="t('settingsStage4.pricingTools.tabs.logs')" name="logs">
          <div class="toolbar">
            <div class="toolbar-left">
              <span class="filter-label">{{
                t('settingsStage4.pricingTools.sections.diagnostics')
              }}</span>
              <el-text type="info" size="small">{{
                t('settingsStage4.pricingTools.hints.diagnostics')
              }}</el-text>
            </div>
            <div>
              <el-button @click="handleOpenStatusDialog()">{{
                t('settingsStage4.pricingTools.actions.statusQuery')
              }}</el-button>
              <el-button type="primary" @click="handleOpenPushReservationsDialog()">{{
                t('settingsStage4.pricingTools.actions.pushReservations')
              }}</el-button>
            </div>
          </div>

          <el-table :data="syncLogs" border stripe class="config-table" v-loading="logsLoading">
            <el-table-column
              :label="t('settingsStage4.pricingTools.columns.time')"
              width="180"
              align="center"
            >
              <template #default="{ row }">
                {{ formatDateTime(row.createdAt) }}
              </template>
            </el-table-column>
            <el-table-column
              prop="syncTypeDisplay"
              :label="t('settingsStage4.pricingTools.columns.type')"
              width="120"
              align="center"
            />
            <el-table-column
              prop="directionDisplay"
              :label="t('settingsStage4.pricingTools.columns.direction')"
              width="100"
              align="center"
            >
              <template #default="{ row }">
                <el-tag :type="row.direction === 'INBOUND' ? 'success' : 'primary'" size="small">
                  {{ row.directionDisplay }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column
              prop="statusDisplay"
              :label="t('settings.common.status')"
              width="100"
              align="center"
            >
              <template #default="{ row }">
                <el-tag
                  :type="
                    row.status === 'SUCCESS'
                      ? 'success'
                      : row.status === 'FAILURE'
                        ? 'danger'
                        : 'warning'
                  "
                  size="small"
                >
                  {{ row.statusDisplay }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column
              prop="affectedCount"
              :label="t('settingsStage4.pricingTools.columns.affectedRecords')"
              width="100"
              align="center"
            />
            <el-table-column
              prop="errorMessage"
              :label="t('settingsStage4.pricingTools.columns.errorMessage')"
              min-width="200"
            >
              <template #default="{ row }">
                <span class="error-message">{{ row.errorMessage || '-' }}</span>
              </template>
            </el-table-column>
          </el-table>

          <!-- 分页 -->
          <div class="pagination-wrapper">
            <el-pagination
              v-model:current-page="logsPagination.page"
              v-model:page-size="logsPagination.size"
              :total="logsPagination.total"
              :page-sizes="[10, 20, 50]"
              layout="total, sizes, prev, pager, next"
              @size-change="loadSyncLogs"
              @current-change="loadSyncLogs"
            />
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- 添加连接对话框 -->
    <el-dialog
      v-model="showConnectionDialog"
      :title="t('settingsStage4.pricingTools.dialog.addConnection')"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-alert
        :title="t('settingsStage4.pricingTools.dialog.notice')"
        type="warning"
        :closable="false"
        show-icon
        style="margin-bottom: 12px"
      >
        <template #default>
          <div>{{ t('settingsStage4.pricingTools.hints.singleActiveConnection') }}</div>
          <div>{{ t('settingsStage4.pricingTools.hints.replaceConnection') }}</div>
        </template>
      </el-alert>
      <el-form :model="connectionForm" label-width="100px">
        <el-form-item :label="t('settingsStage4.pricingTools.columns.account')" required>
          <el-select
            v-model="connectionForm.accountId"
            :placeholder="t('settingsStage4.pricingTools.placeholders.selectAccount')"
            style="width: 100%"
          >
            <el-option
              v-for="account in enabledAccounts"
              :key="account.id"
              :label="account.accountName"
              :value="account.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('settingsStage4.cleaningSettings.fields.roomType')" required>
          <el-select
            v-model="connectionForm.roomTypeId"
            :placeholder="t('settingsStage4.pricingTools.placeholders.selectRoomType')"
            style="width: 100%"
            @change="handleRoomTypeChange"
          >
            <el-option v-for="rt in roomTypes" :key="rt.id" :label="rt.name" :value="rt.id" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('settingsStage4.pricePlan.columns.pricePlan')" required>
          <el-select
            v-model="connectionForm.pricePlanId"
            :placeholder="t('settingsStage4.pricingTools.placeholders.selectBoundPricePlan')"
            style="width: 100%"
            :disabled="!connectionForm.roomTypeId"
          >
            <el-option
              v-for="pp in roomTypeBoundPricePlans"
              :key="pp.id"
              :label="pp.name"
              :value="pp.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showConnectionDialog = false">{{
            t('settings.common.cancel')
          }}</el-button>
          <el-button
            type="primary"
            :loading="saveConnectionLoading"
            @click="handleSaveConnection"
            >{{ t('settings.common.save') }}</el-button
          >
        </div>
      </template>
    </el-dialog>

    <!-- 编辑价格调整对话框 -->
    <el-dialog
      v-model="showAccountDialog"
      :title="
        editingAccountId
          ? t('settingsStage4.pricingTools.dialog.editAccount')
          : t('settingsStage4.pricingTools.dialog.addAccount')
      "
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form :model="accountForm" label-width="110px">
        <el-form-item :label="t('settingsStage4.pricingTools.columns.accountName')" required>
          <el-input
            v-model="accountForm.accountName"
            :placeholder="t('settingsStage4.pricingTools.placeholders.accountNameExample')"
          />
        </el-form-item>
        <el-form-item :label="t('settingsStage4.pricingTools.columns.priceLabsEmail')" required>
          <el-input v-model="accountForm.priceLabsEmail" placeholder="example@email.com" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showAccountDialog = false">{{
            t('settings.common.cancel')
          }}</el-button>
          <el-button type="primary" :loading="saveAccountLoading" @click="handleSaveAccount">
            {{ t('settings.common.save') }}
          </el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog
      v-model="showAdjustmentDialog"
      :title="t('settingsStage4.pricingTools.dialog.editAdjustment')"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form :model="adjustmentForm" label-width="100px">
        <el-form-item :label="t('settingsStage4.pricingTools.columns.channel')">
          <el-input :model-value="adjustmentForm.channelName" disabled />
        </el-form-item>
        <el-form-item :label="t('settingsStage4.pricingTools.fields.adjustmentDirection')">
          <el-radio-group v-model="adjustmentDirection">
            <el-radio value="up">{{ t('settingsStage4.pricingTools.adjustment.up') }}</el-radio>
            <el-radio value="down">{{ t('settingsStage4.pricingTools.adjustment.down') }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item :label="t('settingsStage4.pricingTools.fields.adjustmentType')">
          <el-radio-group v-model="adjustmentForm.adjustmentType">
            <el-radio value="PERCENTAGE">{{
              t('settingsStage4.pricingTools.adjustment.percentage')
            }}</el-radio>
            <el-radio value="FIXED">{{
              t('settingsStage4.pricingTools.adjustment.fixed')
            }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item :label="t('settingsStage4.pricingTools.fields.adjustmentValue')">
          <el-input-number
            v-model="adjustmentFormValue"
            :min="0"
            :precision="adjustmentForm.adjustmentType === 'PERCENTAGE' ? 1 : 0"
            style="width: 200px"
          />
          <span class="unit-label">{{
            adjustmentForm.adjustmentType === 'PERCENTAGE'
              ? '%'
              : t('settingsStage4.pricingTools.units.currency')
          }}</span>
        </el-form-item>
        <el-form-item :label="t('settingsStage4.pricingTools.fields.autoSync')">
          <el-switch v-model="adjustmentForm.autoSyncPrice" />
          <span class="form-tip">{{ t('settingsStage4.pricingTools.hints.autoSync') }}</span>
        </el-form-item>
        <el-form-item :label="t('settingsStage4.pricingTools.fields.exampleCalculation')">
          <div class="example-preview">
            <span>{{
              t('settingsStage4.pricingTools.examplePreview', { price: calculatePreviewPrice() })
            }}</span>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showAdjustmentDialog = false">{{
            t('settings.common.cancel')
          }}</el-button>
          <el-button
            type="primary"
            :loading="saveAdjustmentLoading"
            @click="handleSaveAdjustment"
            >{{ t('settings.common.save') }}</el-button
          >
        </div>
      </template>
    </el-dialog>

    <!-- PriceLabs 状态查询（PMS -> PriceLabs /status） -->
    <el-dialog
      v-model="showStatusDialog"
      :title="t('settingsStage4.pricingTools.dialog.statusQuery')"
      width="760px"
      :close-on-click-modal="false"
    >
      <el-form :model="statusForm" label-width="110px">
        <el-form-item :label="t('settingsStage4.pricingTools.columns.type')" required>
          <el-select v-model="statusForm.type" style="width: 220px">
            <el-option
              :label="t('settingsStage4.pricingTools.statusTypes.listing')"
              value="listing"
            />
            <el-option
              :label="t('settingsStage4.pricingTools.statusTypes.reservation')"
              value="reservation"
            />
            <el-option
              :label="t('settingsStage4.pricingTools.statusTypes.calendar')"
              value="calendar"
            />
          </el-select>
        </el-form-item>

        <el-form-item
          v-if="statusForm.type === 'listing'"
          :label="t('settingsStage4.pricingTools.columns.listingId')"
        >
          <el-select
            v-model="statusListingId"
            filterable
            clearable
            :placeholder="t('settingsStage4.pricingTools.placeholders.selectFromConnections')"
            style="width: 520px"
            @change="handleSelectStatusListingId"
          >
            <el-option
              v-for="conn in connections"
              :key="conn.id"
              :label="`${conn.roomTypeName} - ${conn.pricePlanName} (${conn.priceLabsListingId})`"
              :value="conn.priceLabsListingId"
            />
          </el-select>
        </el-form-item>

        <el-form-item :label="t('settingsStage4.pricingTools.fields.id')" required>
          <el-input
            v-model="statusForm.id"
            :placeholder="t('settingsStage4.pricingTools.placeholders.statusId')"
            style="width: 520px"
          />
          <el-text type="info" size="small" style="margin-left: 10px">
            {{ t('settingsStage4.pricingTools.hints.reservationId') }}
          </el-text>
        </el-form-item>
      </el-form>

      <el-divider content-position="left">{{
        t('settingsStage4.pricingTools.result.title')
      }}</el-divider>
      <div v-if="statusResult" class="status-result-vertical">
        <!-- Success 区域 -->
        <div class="result-block">
          <div class="result-header">
            <span class="result-title">{{ t('settingsStage4.pricingTools.result.success') }}</span>
            <el-tag v-if="statusResult.success?.length" type="success" size="small">
              {{
                t('settingsStage4.pricingTools.result.count', {
                  count: statusResult.success.length,
                })
              }}
            </el-tag>
          </div>
          <el-scrollbar v-if="statusResult.success?.length" max-height="300">
            <div v-for="(item, idx) in statusResult.success" :key="idx" class="status-item">
              <template v-if="typeof item === 'object' && item !== null">
                <el-descriptions :column="2" size="small" border>
                  <el-descriptions-item
                    v-for="(value, key) in item"
                    :key="key"
                    :label="String(key)"
                  >
                    <template v-if="typeof value === 'boolean'">
                      <el-tag :type="value ? 'success' : 'info'" size="small">
                        {{ value ? t('settings.common.yes') : t('settings.common.no') }}
                      </el-tag>
                    </template>
                    <template v-else-if="value === null || value === undefined">
                      <span class="text-muted">-</span>
                    </template>
                    <template v-else>
                      {{ value }}
                    </template>
                  </el-descriptions-item>
                </el-descriptions>
              </template>
              <template v-else>
                <el-tag size="small">{{ item }}</el-tag>
              </template>
            </div>
          </el-scrollbar>
          <el-empty
            v-else
            :description="t('settingsStage4.pricingTools.result.noSuccess')"
            :image-size="50"
          />
        </div>

        <!-- Failure 区域 -->
        <div class="result-block">
          <div class="result-header">
            <span class="result-title">{{ t('settingsStage4.pricingTools.result.failure') }}</span>
            <el-tag v-if="statusResult.failure?.length" type="danger" size="small">
              {{
                t('settingsStage4.pricingTools.result.count', {
                  count: statusResult.failure.length,
                })
              }}
            </el-tag>
          </div>
          <el-scrollbar v-if="statusResult.failure?.length" max-height="200">
            <div v-for="(item, idx) in statusResult.failure" :key="idx" class="status-item">
              <template v-if="typeof item === 'object' && item !== null">
                <el-descriptions :column="2" size="small" border>
                  <el-descriptions-item
                    v-for="(value, key) in item"
                    :key="key"
                    :label="String(key)"
                  >
                    {{ value ?? '-' }}
                  </el-descriptions-item>
                </el-descriptions>
              </template>
              <template v-else>
                <el-tag type="danger" size="small">{{ item }}</el-tag>
              </template>
            </div>
          </el-scrollbar>
          <el-empty
            v-else
            :description="t('settingsStage4.pricingTools.result.noFailure')"
            :image-size="50"
          />
        </div>
      </div>
      <el-empty
        v-else
        :description="t('settingsStage4.pricingTools.result.queryEmpty')"
        :image-size="80"
      />

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showStatusDialog = false">{{ t('settings.common.close') }}</el-button>
          <el-button type="primary" :loading="statusLoading" @click="handleQueryStatus">{{
            t('settingsStage4.pricingTools.actions.query')
          }}</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- PriceLabs 预订推送（PMS -> PriceLabs /reservations） -->
    <el-dialog
      v-model="showPushReservationsDialog"
      :title="t('settingsStage4.pricingTools.dialog.pushReservations')"
      width="760px"
      :close-on-click-modal="false"
    >
      <el-form label-width="110px">
        <el-form-item :label="t('settingsStage4.pricingTools.fields.dateRange')">
          <el-date-picker
            v-model="pushReservationsRange"
            type="daterange"
            :range-separator="t('settingsStage4.pricingTools.fields.to')"
            :start-placeholder="t('settingsStage4.pricingTools.placeholders.startDate')"
            :end-placeholder="t('settingsStage4.pricingTools.placeholders.endDate')"
            value-format="YYYY-MM-DD"
          />
          <el-text type="info" size="small" style="margin-left: 10px">
            {{ t('settingsStage4.pricingTools.hints.defaultDateRange') }}
          </el-text>
        </el-form-item>
      </el-form>

      <el-divider content-position="left">{{
        t('settingsStage4.pricingTools.result.title')
      }}</el-divider>
      <div v-if="pushReservationsResult">
        <el-descriptions border :column="2" size="small">
          <el-descriptions-item :label="t('settingsStage4.pricingTools.result.listingCount')">{{
            pushReservationsResult.listingCount
          }}</el-descriptions-item>
          <el-descriptions-item :label="t('settingsStage4.pricingTools.result.reservationCount')">{{
            pushReservationsResult.reservationCount
          }}</el-descriptions-item>
          <el-descriptions-item :label="t('settingsStage4.pricingTools.result.successCount')">{{
            pushReservationsResult.successCount
          }}</el-descriptions-item>
          <el-descriptions-item :label="t('settingsStage4.pricingTools.result.failureCount')">{{
            pushReservationsResult.failureCount
          }}</el-descriptions-item>
        </el-descriptions>
        <div style="margin-top: 12px">
          <div class="result-title">{{ t('settingsStage4.pricingTools.result.failures') }}</div>
          <el-scrollbar height="220">
            <pre class="result-json">{{
              JSON.stringify(pushReservationsResult.failures ?? [], null, 2)
            }}</pre>
          </el-scrollbar>
        </div>
      </div>
      <el-empty
        v-else
        :description="t('settingsStage4.pricingTools.result.pushEmpty')"
        :image-size="80"
      />

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showPushReservationsDialog = false">{{
            t('settings.common.close')
          }}</el-button>
          <el-button
            type="primary"
            :loading="pushReservationsLoading"
            @click="handlePushReservations"
          >
            {{ t('settingsStage4.pricingTools.actions.push') }}
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as priceLabsApi from '@/api/pricelabs'
import { getAllRoomTypes, type RoomTypeDTO } from '@/api/roomType'
import {
  getPricePlansByRoomType,
  type PricePlanDTO,
  type RoomTypePricePlanDTO,
} from '@/api/pricePlan'
import {
  getAllOtaIntegrations,
  updatePriceAdjustment,
  type OtaIntegrationDTO,
  type PriceAdjustmentType,
} from '@/api/otaIntegration'
import { addDaysToYmd, formatBackendDateTime, getStoreTodayYmd } from '@/utils/storeDateTime'
import type {
  PriceLabsAccountDTO,
  PriceLabsIntegrationDTO,
  PriceLabsConnectionDTO,
  PriceLabsSyncLogDTO,
  PriceLabsStatusType,
  PriceLabsStatusResult,
  PriceLabsPushReservationsResult,
} from '@/api/pricelabs'

const { t } = useI18n()

// 视图状态
const showConfigPage = ref(false)
const activeTab = ref('connections')

// 集成配置
const integration = ref<PriceLabsIntegrationDTO>({
  isEnabled: false,
  connectedRoomTypeCount: 0,
  totalSyncCount: 0,
  successSyncCount: 0,
})
const toggleLoading = ref(false)

const accounts = ref<PriceLabsAccountDTO[]>([])
const accountsLoading = ref(false)
const defaultAccountId = ref<number | null>(null)
const selectedAccountId = ref<number | null>(null)

// 连接列表
const connections = ref<PriceLabsConnectionDTO[]>([])
const connectionsLoading = ref(false)
const syncingRoomTypeIds = ref<number[]>([])
const connectionFilter = ref('all')

// 渠道价格调整（使用 OTA 直连数据）
interface ChannelAdjustmentItem {
  channelId: number
  channelName: string
  channelCode: string
  adjustmentType: PriceAdjustmentType
  adjustmentValue: number | null
  autoSyncPrice: boolean
  exampleBasePrice: number
  exampleChannelPrice: number
}
const channelAdjustments = ref<ChannelAdjustmentItem[]>([])
const adjustmentsLoading = ref(false)

// 同步日志
const syncLogs = ref<PriceLabsSyncLogDTO[]>([])
const logsLoading = ref(false)
const logsPagination = reactive({
  page: 1,
  size: 20,
  total: 0,
})

// 诊断工具：/status & /reservations
const showStatusDialog = ref(false)
const statusLoading = ref(false)
const statusListingId = ref<string | null>(null)
const statusForm = reactive({
  type: 'listing' as PriceLabsStatusType,
  id: '',
})
const statusResult = ref<PriceLabsStatusResult | null>(null)

const showPushReservationsDialog = ref(false)
const pushReservationsLoading = ref(false)
const pushReservationsRange = ref<[string, string]>(['2020-01-01', ''])
const pushReservationsResult = ref<PriceLabsPushReservationsResult | null>(null)

// 房型和价格计划选项
const roomTypes = ref<RoomTypeDTO[]>([])
const roomTypeBoundPricePlans = ref<PricePlanDTO[]>([])

// 添加连接对话框
const showConnectionDialog = ref(false)
const saveConnectionLoading = ref(false)
const showAccountDialog = ref(false)
const saveAccountLoading = ref(false)
const editingAccountId = ref<number | null>(null)
const editingAccountOriginalEmail = ref('')
const accountForm = reactive({
  accountName: '',
  priceLabsEmail: '',
})
const connectionForm = reactive({
  accountId: null as number | null,
  roomTypeId: null as number | null,
  pricePlanId: null as number | null,
})

// 编辑价格调整对话框
const showAdjustmentDialog = ref(false)
const saveAdjustmentLoading = ref(false)
const adjustmentDirection = ref<'up' | 'down'>('up')
const adjustmentFormValue = ref(0)
const adjustmentForm = reactive({
  channelId: 0,
  channelName: '',
  adjustmentType: 'PERCENTAGE' as PriceAdjustmentType,
  adjustmentValue: 0 as number | null,
  autoSyncPrice: true,
})

// 过滤后的连接列表
const filteredConnections = computed(() => {
  let result = connections.value
  if (selectedAccountId.value !== null) {
    result = result.filter((conn) => conn.accountId === selectedAccountId.value)
  }
  if (connectionFilter.value === 'all') {
    return result
  }
  const isConnected = connectionFilter.value === 'connected'
  return result.filter((conn) =>
    isConnected ? conn.syncStatus === 'connected' : conn.syncStatus !== 'connected',
  )
})

const enabledAccounts = computed(() => accounts.value.filter((account) => account.isEnabled))

const selectedAccountName = computed(() => {
  if (selectedAccountId.value === null) {
    return ''
  }
  return accounts.value.find((account) => account.id === selectedAccountId.value)?.accountName || ''
})

const normalizeEmailValue = (email?: string | null) => email?.trim().toLowerCase() || ''

const syncDefaultAccountSelection = () => {
  const currentEmail = normalizeEmailValue(integration.value.priceLabsEmail)
  if (!currentEmail) {
    defaultAccountId.value = null
    return
  }

  defaultAccountId.value =
    accounts.value.find((account) => normalizeEmailValue(account.priceLabsEmail) === currentEmail)
      ?.id ?? null
}

const enabledConnectionByRoomTypeId = computed(() => {
  const map = new Map<number, PriceLabsConnectionDTO>()
  for (const conn of connections.value) {
    if (conn.isEnabled && conn.accountEnabled !== false) {
      map.set(conn.roomTypeId, conn)
    }
  }
  return map
})

const getConnectionAccountLabel = (connection: PriceLabsConnectionDTO): string => {
  const accountName = connection.accountName?.trim()
  const accountEmail = connection.accountEmail?.trim()

  if (accountName && accountEmail && accountName !== accountEmail) {
    return `${accountName} (${accountEmail})`
  }
  return accountName || accountEmail || t('settingsStage4.pricingTools.status.unknownAccount')
}

const findDuplicateConnection = (roomTypeId: number, pricePlanId: number) =>
  connections.value.find(
    (conn) => conn.roomTypeId === roomTypeId && conn.pricePlanId === pricePlanId,
  )

// 进入配置页面
const handleConfigure = () => {
  showConfigPage.value = true
  loadAllData()
}

// 加载所有数据
const loadAllData = async () => {
  await Promise.all([
    loadIntegration(),
    loadAccounts(),
    loadConnections(),
    loadChannelAdjustments(),
    loadRoomTypes(),
  ])
}

// 加载集成配置
const loadIntegration = async () => {
  try {
    const res = await priceLabsApi.getIntegration()
    if (res.success) {
      integration.value = res.data
      syncDefaultAccountSelection()
    }
  } catch (error) {
    console.error('加载集成配置失败:', error)
  }
}

// 加载连接列表
const loadAccounts = async () => {
  accountsLoading.value = true
  try {
    const res = await priceLabsApi.getAccounts()
    if (res.success) {
      accounts.value = res.data
      syncDefaultAccountSelection()
      if (
        selectedAccountId.value !== null &&
        !res.data.some((account) => account.id === selectedAccountId.value)
      ) {
        selectedAccountId.value = null
      }
    }
  } catch (error) {
    console.error('鍔犺浇 PriceLabs 璐﹀彿鍒楄〃澶辫触:', error)
  } finally {
    accountsLoading.value = false
  }
}

const loadConnections = async () => {
  connectionsLoading.value = true
  try {
    const res = await priceLabsApi.getConnections()
    if (res.success) {
      connections.value = res.data
    }
  } catch (error) {
    console.error('加载连接列表失败:', error)
  } finally {
    connectionsLoading.value = false
  }
}

// 加载渠道价格调整（从 OTA 直连表获取）
const loadChannelAdjustments = async () => {
  adjustmentsLoading.value = true
  try {
    const res = await getAllOtaIntegrations()
    if (res.success) {
      // 将 OTA 直连数据转换为价格调整格式
      channelAdjustments.value = res.data.map((ota: OtaIntegrationDTO) => {
        const basePrice = 1000
        const adjustmentValue = ota.priceAdjustmentValue ?? 0
        let channelPrice = basePrice
        if (ota.priceAdjustmentType === 'PERCENTAGE') {
          channelPrice = basePrice * (1 + adjustmentValue / 100)
        } else if (ota.priceAdjustmentType === 'FIXED') {
          channelPrice = basePrice + adjustmentValue
        }
        return {
          channelId: ota.id,
          channelName: ota.name,
          channelCode: ota.code,
          adjustmentType: ota.priceAdjustmentType ?? 'PERCENTAGE',
          adjustmentValue: ota.priceAdjustmentValue ?? null,
          autoSyncPrice: ota.autoSyncPrice ?? true,
          exampleBasePrice: basePrice,
          exampleChannelPrice: channelPrice,
        }
      })
    }
  } catch (error) {
    console.error('加载渠道价格调整失败:', error)
  } finally {
    adjustmentsLoading.value = false
  }
}

// 加载同步日志
const loadSyncLogs = async () => {
  logsLoading.value = true
  try {
    const res = await priceLabsApi.getSyncLogs(logsPagination.page - 1, logsPagination.size)
    if (res.success) {
      syncLogs.value = res.data.content
      logsPagination.total = res.data.totalElements
    }
  } catch (error) {
    console.error('加载同步日志失败:', error)
  } finally {
    logsLoading.value = false
  }
}

// 加载房型列表
const loadRoomTypes = async () => {
  try {
    const res = await getAllRoomTypes()
    if (res.success) {
      roomTypes.value = res.data
    }
  } catch (error) {
    console.error('加载房型列表失败:', error)
  }
}

// 加载房型已绑定的价格计划（来源：价格计划页的房型绑定关系）
const loadBoundPricePlansByRoomType = async (roomTypeId: number) => {
  try {
    const res = (await getPricePlansByRoomType(roomTypeId)) as unknown as {
      success?: boolean
      data?: RoomTypePricePlanDTO[]
    }
    const mappings = Array.isArray(res?.data) ? res.data : []
    const uniquePlans = new Map<number, PricePlanDTO>()
    for (const mapping of mappings) {
      const plan = mapping.pricePlan
      if (!plan?.id) {
        continue
      }
      if (uniquePlans.has(plan.id)) {
        continue
      }
      uniquePlans.set(plan.id, {
        id: plan.id,
        name: plan.name || `Plan ${plan.id}`,
        minNights: 1,
        includeMeal: false,
      })
    }
    roomTypeBoundPricePlans.value = Array.from(uniquePlans.values())
  } catch (error) {
    roomTypeBoundPricePlans.value = []
    console.error('加载房型已绑定价格计划失败:', error)
  }
}

const handleRoomTypeChange = async (roomTypeId: number | null) => {
  connectionForm.pricePlanId = null
  roomTypeBoundPricePlans.value = []
  if (!roomTypeId) {
    return
  }
  await loadBoundPricePlansByRoomType(roomTypeId)
}

// 保存配置
const selectAccount = (accountId: number) => {
  selectedAccountId.value = accountId
}

const clearSelectedAccount = () => {
  selectedAccountId.value = null
}

const openCreateAccountDialog = () => {
  editingAccountId.value = null
  editingAccountOriginalEmail.value = ''
  accountForm.accountName = ''
  accountForm.priceLabsEmail = ''
  showAccountDialog.value = true
}

const openEditAccountDialog = (account: PriceLabsAccountDTO) => {
  editingAccountId.value = account.id
  editingAccountOriginalEmail.value = account.priceLabsEmail
  accountForm.accountName = account.accountName
  accountForm.priceLabsEmail = account.priceLabsEmail
  showAccountDialog.value = true
}

const handleSaveAccount = async () => {
  if (!accountForm.accountName.trim() || !accountForm.priceLabsEmail.trim()) {
    ElMessage.warning(t('settingsStage4.pricingTools.messages.fillAccountNameAndEmail'))
    return
  }

  saveAccountLoading.value = true
  try {
    const payload = {
      accountName: accountForm.accountName.trim(),
      priceLabsEmail: accountForm.priceLabsEmail.trim(),
    }
    const isEditingCurrentDefault =
      editingAccountId.value !== null &&
      normalizeEmailValue(integration.value.priceLabsEmail) ===
        normalizeEmailValue(editingAccountOriginalEmail.value)
    const shouldInitializeDefault =
      editingAccountId.value === null && !normalizeEmailValue(integration.value.priceLabsEmail)

    const res = editingAccountId.value
      ? await priceLabsApi.updateAccount(editingAccountId.value, payload)
      : await priceLabsApi.createAccount(payload)

    if (res.success) {
      if (isEditingCurrentDefault || shouldInitializeDefault) {
        const updateRes = await priceLabsApi.updateIntegrationConfig({
          priceLabsEmail: payload.priceLabsEmail,
        })
        if (!updateRes.success) {
          ElMessage.error(
            updateRes.message ||
              t('settingsStage4.pricingTools.messages.updateDefaultAccountFailed'),
          )
          return
        }
        integration.value = updateRes.data
        defaultAccountId.value = res.data.id
      }
      ElMessage.success(
        editingAccountId.value
          ? t('settingsStage4.pricingTools.messages.accountUpdated')
          : t('settingsStage4.pricingTools.messages.accountCreated'),
      )
      showAccountDialog.value = false
      await loadAccounts()
      await loadIntegration()
    } else {
      ElMessage.error(res.message || t('settingsStage4.pricingTools.messages.saveAccountFailed'))
    }
  } catch (error) {
    console.error('保存 PriceLabs 账号失败:', error)
    ElMessage.error(t('settingsStage4.pricingTools.messages.saveAccountFailed'))
  } finally {
    saveAccountLoading.value = false
  }
}

const handleToggleAccount = async (account: PriceLabsAccountDTO) => {
  try {
    const res = await priceLabsApi.updateAccountStatus(account.id, !account.isEnabled)
    if (res.success) {
      if (!res.data.isEnabled && selectedAccountId.value === account.id) {
        selectedAccountId.value = account.id
      }
      ElMessage.success(
        res.data.isEnabled
          ? t('settingsStage4.pricingTools.messages.accountEnabled')
          : t('settingsStage4.pricingTools.messages.accountDisabled'),
      )
      await loadAccounts()
      await loadConnections()
    } else {
      ElMessage.error(
        res.message || t('settingsStage4.pricingTools.messages.updateAccountStatusFailed'),
      )
    }
  } catch (error) {
    console.error('更新 PriceLabs 账号状态失败:', error)
    ElMessage.error(t('settingsStage4.pricingTools.messages.updateAccountStatusFailed'))
  }
}

const handleDeleteAccount = async (account: PriceLabsAccountDTO) => {
  try {
    await ElMessageBox.confirm(
      t('settingsStage4.pricingTools.messages.deleteAccountConfirm', { name: account.accountName }),
      t('settings.common.deleteConfirmTitle'),
      {
        confirmButtonText: t('settings.common.confirm'),
        cancelButtonText: t('settings.common.cancel'),
        type: 'warning',
      },
    )
    const res = await priceLabsApi.deleteAccount(account.id)
    if (res.success) {
      if (selectedAccountId.value === account.id) {
        selectedAccountId.value = null
      }
      ElMessage.success(t('settingsStage4.pricingTools.messages.accountDeleted'))
      await loadAccounts()
      await loadConnections()
      await loadIntegration()
    } else {
      ElMessage.error(res.message || t('settingsStage4.pricingTools.messages.deleteAccountFailed'))
    }
  } catch (error) {
    if (error) {
      console.error('删除 PriceLabs 账号失败:', error)
    }
  }
}

const handleSaveDefaultAccountConfig = async () => {
  if (!defaultAccountId.value) {
    ElMessage.warning(t('settingsStage4.pricingTools.messages.selectDefaultAccount'))
    return
  }

  try {
    const account = accounts.value.find((item) => item.id === defaultAccountId.value)
    if (!account?.priceLabsEmail?.trim()) {
      ElMessage.warning(t('settingsStage4.pricingTools.messages.defaultAccountMissingEmail'))
      return
    }

    const res = await priceLabsApi.updateIntegrationConfig({
      priceLabsEmail: account.priceLabsEmail.trim(),
    })
    if (res.success) {
      integration.value = res.data
      defaultAccountId.value = account.id
      ElMessage.success(t('settings.common.saveSuccess'))
      return
    }
    ElMessage.error(res.message || t('settings.common.saveFailed'))
  } catch (error) {
    console.error('淇濆瓨閰嶇疆澶辫触:', error)
    ElMessage.error(t('settings.common.saveFailed'))
  }
}

const handleSaveConfig = async () => {
  if (!integration.value.priceLabsEmail) {
    ElMessage.warning(t('settingsStage4.pricingTools.messages.enterPriceLabsEmail'))
    return
  }

  try {
    const normalizedEmail = integration.value.priceLabsEmail.trim()
    const existingAccount = accounts.value.find(
      (account) => account.priceLabsEmail === normalizedEmail,
    )
    if (!existingAccount) {
      await priceLabsApi.createAccount({
        accountName: normalizedEmail,
        priceLabsEmail: normalizedEmail,
      })
      await loadAccounts()
    }

    const res = await priceLabsApi.updateIntegrationConfig({
      priceLabsEmail: normalizedEmail,
    })
    if (res.success) {
      integration.value = res.data
      ElMessage.success(t('settings.common.saveSuccess'))
    } else {
      ElMessage.error(res.message || t('settings.common.saveFailed'))
    }
  } catch (error) {
    console.error('保存配置失败:', error)
    ElMessage.error(t('settings.common.saveFailed'))
  }
}

const ensureIntegrationEmailForAccount = async (accountId?: number | null) => {
  const account =
    accounts.value.find((item) => item.id === accountId) ||
    accounts.value.find((item) => item.id === selectedAccountId.value) ||
    enabledAccounts.value[0]

  if (!account?.priceLabsEmail?.trim()) {
    return false
  }

  const nextEmail = account.priceLabsEmail.trim()
  if (integration.value.priceLabsEmail?.trim() === nextEmail) {
    defaultAccountId.value = account.id
    return true
  }

  const res = await priceLabsApi.updateIntegrationConfig({
    priceLabsEmail: nextEmail,
  })
  if (!res.success) {
    ElMessage.error(res.message || t('settingsStage4.pricingTools.messages.syncDefaultEmailFailed'))
    return false
  }

  integration.value = res.data
  defaultAccountId.value = account.id
  return true
}

// 切换集成状态
const handleToggleIntegration = async (enabled: boolean) => {
  if (enabled && enabledAccounts.value.length === 0) {
    ElMessage.warning(t('settingsStage4.pricingTools.messages.addAndEnableAccountFirst'))
    integration.value.isEnabled = false
    return
  }

  if (enabled) {
    const defaultAccount = accounts.value.find((item) => item.id === defaultAccountId.value)
    if (!defaultAccount) {
      ElMessage.warning(t('settingsStage4.pricingTools.messages.saveDefaultAccountFirst'))
      integration.value.isEnabled = false
      return
    }
    if (!defaultAccount.isEnabled) {
      ElMessage.warning(t('settingsStage4.pricingTools.messages.enableDefaultAccountFirst'))
      integration.value.isEnabled = false
      return
    }
    const synced = await ensureIntegrationEmailForAccount(defaultAccount.id)
    if (!synced) {
      ElMessage.warning(t('settingsStage4.pricingTools.messages.noAvailableAccountEmail'))
      integration.value.isEnabled = false
      return
    }
  }

  toggleLoading.value = true
  try {
    const res = await priceLabsApi.toggleIntegration(enabled)
    if (res.success) {
      integration.value = res.data
      ElMessage.success(
        enabled
          ? t('settingsStage4.pricingTools.messages.integrationEnabled')
          : t('settingsStage4.pricingTools.messages.integrationDisabled'),
      )
    } else {
      // 恢复状态
      integration.value.isEnabled = !enabled
      ElMessage.error(res.message || t('settings.common.operationFailed'))
    }
  } catch (error) {
    integration.value.isEnabled = !enabled
    ElMessage.error(t('settings.common.operationFailed'))
  } finally {
    toggleLoading.value = false
  }
}

// 添加连接
const handleAddConnection = () => {
  if (enabledAccounts.value.length === 0) {
    ElMessage.warning(t('settingsStage4.pricingTools.messages.addAndEnableAccountFirst'))
    return
  }
  connectionForm.accountId = selectedAccountId.value ?? enabledAccounts.value[0]?.id ?? null
  connectionForm.roomTypeId = null
  connectionForm.pricePlanId = null
  roomTypeBoundPricePlans.value = []
  showConnectionDialog.value = true
}

// 保存连接
const handleSaveConnection = async () => {
  if (!connectionForm.accountId) {
    ElMessage.warning(t('settingsStage4.pricingTools.messages.selectAccountToBind'))
    return
  }
  if (!connectionForm.roomTypeId) {
    ElMessage.warning(t('settingsStage4.pricingTools.messages.selectRoomType'))
    return
  }

  const existing = enabledConnectionByRoomTypeId.value.get(connectionForm.roomTypeId)
  if (existing) {
    const planName = existing.pricePlanName || t('settingsStage4.pricingTools.status.unknown')
    ElMessage.warning(
      t('settingsStage4.pricingTools.messages.roomTypeAlreadyConnected', { planName }),
    )
    return
  }

  if (!connectionForm.pricePlanId) {
    ElMessage.warning(t('settingsStage4.pricingTools.messages.selectPricePlan'))
    return
  }

  const duplicateConnection = findDuplicateConnection(
    connectionForm.roomTypeId,
    connectionForm.pricePlanId,
  )
  if (duplicateConnection) {
    ElMessage.warning(
      t('settingsStage4.pricingTools.messages.duplicateConnection', {
        account: getConnectionAccountLabel(duplicateConnection),
      }),
    )
    return
  }

  const isBoundPlan = roomTypeBoundPricePlans.value.some(
    (plan) => plan.id === connectionForm.pricePlanId,
  )
  if (!isBoundPlan) {
    ElMessage.warning(t('settingsStage4.pricingTools.messages.selectBoundPricePlan'))
    return
  }

  saveConnectionLoading.value = true
  try {
    if (!integration.value.isEnabled) {
      const synced = await ensureIntegrationEmailForAccount(connectionForm.accountId)
      if (!synced) {
        ElMessage.warning(t('settingsStage4.pricingTools.messages.selectedAccountMissingEmail'))
        return
      }

      const toggleRes = await priceLabsApi.toggleIntegration(true)
      if (toggleRes.success) {
        if (toggleRes.data) {
          integration.value = toggleRes.data
        } else {
          await loadIntegration()
        }
        ElMessage.success(t('settingsStage4.pricingTools.messages.integrationEnabled'))
      } else {
        ElMessage.error(
          toggleRes.message || t('settingsStage4.pricingTools.messages.enableIntegrationFailed'),
        )
        return
      }
    }

    const res = await priceLabsApi.createConnection(
      connectionForm.accountId,
      connectionForm.roomTypeId,
      connectionForm.pricePlanId,
    )
    if (res.success) {
      ElMessage.success(t('settingsStage4.pricingTools.messages.connectionAdded'))
      selectedAccountId.value = res.data.accountId ?? connectionForm.accountId
      showConnectionDialog.value = false
      await loadAccounts()
      await loadConnections()
      await loadIntegration()
      return
    }
    if (res.data) {
      ElMessage.warning(
        res.message || t('settingsStage4.pricingTools.messages.connectionSavedSyncFailed'),
      )
      selectedAccountId.value = res.data.accountId ?? connectionForm.accountId
      showConnectionDialog.value = false
      await loadAccounts()
      await loadConnections()
      await loadIntegration()
      return
    } else {
      ElMessage.error(res.message || t('settingsStage4.pricingTools.messages.addConnectionFailed'))
    }
  } catch (error) {
    ElMessage.error(t('settingsStage4.pricingTools.messages.addConnectionFailed'))
  } finally {
    saveConnectionLoading.value = false
  }
}

// 切换连接状态
const handleToggleConnection = async (row: PriceLabsConnectionDTO) => {
  try {
    const res = await priceLabsApi.updateConnectionStatus(row.id, !row.isEnabled)
    if (res.success) {
      row.isEnabled = !row.isEnabled
      ElMessage.success(
        row.isEnabled
          ? t('settingsStage4.pricingTools.messages.connectionEnabled')
          : t('settingsStage4.pricingTools.messages.connectionDisabled'),
      )
    } else {
      ElMessage.error(res.message || t('settings.common.operationFailed'))
    }
  } catch (error) {
    ElMessage.error(t('settings.common.operationFailed'))
  }
}

// 单房型同步
const handleSyncRoomType = async (row: PriceLabsConnectionDTO) => {
  if (!row.isEnabled) {
    ElMessage.warning(t('settingsStage4.pricingTools.messages.enableConnectionFirst'))
    return
  }
  if (syncingRoomTypeIds.value.includes(row.roomTypeId)) {
    return
  }

  syncingRoomTypeIds.value = [...syncingRoomTypeIds.value, row.roomTypeId]
  try {
    const res = await priceLabsApi.syncRoomType(row.roomTypeId)
    if (res.success) {
      ElMessage.success(t('settingsStage4.pricingTools.messages.syncTriggered'))
      await loadConnections()
      await loadIntegration()
    } else {
      ElMessage.error(res.message || t('settingsStage4.pricingTools.messages.syncFailed'))
    }
  } catch (error) {
    ElMessage.error(t('settingsStage4.pricingTools.messages.syncFailed'))
  } finally {
    syncingRoomTypeIds.value = syncingRoomTypeIds.value.filter((id) => id !== row.roomTypeId)
  }
}

// 删除连接
const handleDeleteConnection = async (row: PriceLabsConnectionDTO) => {
  try {
    await ElMessageBox.confirm(
      t('settingsStage4.pricingTools.messages.deleteConnectionConfirm', {
        roomType: row.roomTypeName,
        plan: row.pricePlanName,
      }),
      t('settings.common.deleteConfirmTitle'),
      {
        confirmButtonText: t('settings.common.confirm'),
        cancelButtonText: t('settings.common.cancel'),
        type: 'warning',
      },
    )

    const res = await priceLabsApi.deleteConnection(row.id)
    if (res.success) {
      ElMessage.success(t('settings.common.deleteSuccess'))
      await loadAccounts()
      await loadConnections()
      await loadIntegration()
    } else {
      ElMessage.error(res.message || t('settings.common.deleteFailed'))
    }
  } catch {
    // 用户取消
  }
}

// 编辑价格调整
const handleEditAdjustment = (row: ChannelAdjustmentItem) => {
  adjustmentForm.channelId = row.channelId
  adjustmentForm.channelName = row.channelName
  adjustmentForm.adjustmentType = row.adjustmentType || 'PERCENTAGE'
  adjustmentForm.autoSyncPrice = row.autoSyncPrice

  // 处理调整值和方向
  const value = row.adjustmentValue || 0
  adjustmentDirection.value = value >= 0 ? 'up' : 'down'
  adjustmentFormValue.value = Math.abs(value)

  showAdjustmentDialog.value = true
}

// 保存价格调整
const handleSaveAdjustment = async () => {
  saveAdjustmentLoading.value = true
  try {
    // 根据方向计算最终值
    const finalValue =
      adjustmentDirection.value === 'up' ? adjustmentFormValue.value : -adjustmentFormValue.value

    const res = await updatePriceAdjustment(adjustmentForm.channelId, {
      priceAdjustmentType: adjustmentForm.adjustmentType,
      priceAdjustmentValue: finalValue,
      autoSyncPrice: adjustmentForm.autoSyncPrice,
    })

    if (res.success) {
      ElMessage.success(t('settings.common.saveSuccess'))
      showAdjustmentDialog.value = false
      await loadChannelAdjustments()
    } else {
      ElMessage.error(res.message || t('settings.common.saveFailed'))
    }
  } catch (error) {
    ElMessage.error(t('settings.common.saveFailed'))
  } finally {
    saveAdjustmentLoading.value = false
  }
}

// 自动同步开关变化
const handleAutoSyncChange = async (row: ChannelAdjustmentItem) => {
  try {
    await updatePriceAdjustment(row.channelId, {
      priceAdjustmentType: row.adjustmentType,
      priceAdjustmentValue: row.adjustmentValue ?? 0,
      autoSyncPrice: row.autoSyncPrice,
    })
    ElMessage.success(
      row.autoSyncPrice
        ? t('settingsStage4.pricingTools.messages.autoSyncEnabled')
        : t('settingsStage4.pricingTools.messages.autoSyncDisabled'),
    )
  } catch (error) {
    row.autoSyncPrice = !row.autoSyncPrice
    ElMessage.error(t('settings.common.operationFailed'))
  }
}

// 计算预览价格
const calculatePreviewPrice = (): string => {
  const basePrice = 1000
  const value =
    adjustmentDirection.value === 'up' ? adjustmentFormValue.value : -adjustmentFormValue.value

  if (adjustmentForm.adjustmentType === 'PERCENTAGE') {
    return (basePrice * (1 + value / 100)).toFixed(0)
  } else {
    return (basePrice + value).toFixed(0)
  }
}

// 计算成功率
const calculateSuccessRate = (): string => {
  const total = integration.value.totalSyncCount || 0
  const success = integration.value.successSyncCount || 0
  if (total === 0) return '-'
  return ((success / total) * 100).toFixed(1) + '%'
}

// 格式化日期时间
const formatDateTime = (dateStr: string | undefined): string => {
  if (!dateStr) return ''
  return formatBackendDateTime(dateStr)
}

// 获取同步状态文本
const getSyncStatusText = (status: string): string => {
  const statusMap: Record<string, string> = {
    connected: t('settingsStage4.pricingTools.status.connected'),
    disconnected: t('settingsStage4.pricingTools.status.disconnected'),
    error: t('settingsStage4.pricingTools.status.error'),
  }
  return statusMap[status] || status
}

// 获取调整类型标签
const getAdjustmentTypeTag = (type: PriceAdjustmentType): string => {
  const tagMap: Record<string, string> = {
    COMMISSION: 'warning',
    PERCENTAGE: 'primary',
    FIXED: 'success',
  }
  return tagMap[type] || 'info'
}

// 获取调整类型文本
const getAdjustmentTypeText = (type: PriceAdjustmentType): string => {
  const textMap: Record<string, string> = {
    COMMISSION: t('settingsStage4.pricingTools.adjustment.commission'),
    PERCENTAGE: t('settingsStage4.pricingTools.adjustment.percentage'),
    FIXED: t('settingsStage4.pricingTools.adjustment.fixed'),
  }
  return textMap[type] || type
}

// 格式化调整值
const formatAdjustmentValue = (row: ChannelAdjustmentItem): string => {
  const value = row.adjustmentValue
  if (value === null || value === undefined) return '-'

  const prefix = value >= 0 ? '+' : ''
  if (row.adjustmentType === 'PERCENTAGE') {
    return `${prefix}${value}%`
  }
  return `${prefix}¥${value}`
}

// 获取调整值样式类
const getAdjustmentValueClass = (row: ChannelAdjustmentItem): string => {
  const value = row.adjustmentValue
  if (value === null || value === undefined) return ''
  return value >= 0 ? 'value-up' : 'value-down'
}

// 监听标签页切换
const handleTabChange = (tab: string) => {
  if (tab === 'logs' && syncLogs.value.length === 0) {
    loadSyncLogs()
  }
}

const handleOpenStatusDialog = (preset?: { type?: PriceLabsStatusType; id?: string }) => {
  statusForm.type = preset?.type || 'listing'
  statusForm.id = preset?.id || ''
  statusListingId.value = statusForm.type === 'listing' ? statusForm.id || null : null
  statusResult.value = null
  showStatusDialog.value = true
}

const handleSelectStatusListingId = (id: string) => {
  statusForm.id = id
}

const handleQueryStatus = async () => {
  const id = statusForm.id?.trim()
  if (!id) {
    ElMessage.warning(t('settingsStage4.pricingTools.messages.enterId'))
    return
  }

  statusLoading.value = true
  statusResult.value = null
  try {
    const res = await priceLabsApi.queryStatus([{ id, type: statusForm.type }])
    if (res.success) {
      statusResult.value = res.data
    } else {
      ElMessage.error(res.message || t('settingsStage4.pricingTools.messages.statusQueryFailed'))
    }
  } catch (error) {
    ElMessage.error(t('settingsStage4.pricingTools.messages.statusQueryFailed'))
  } finally {
    statusLoading.value = false
  }
}

const handleOpenPushReservationsDialog = () => {
  pushReservationsRange.value = ['2020-01-01', addDaysToYmd(getStoreTodayYmd(), 365)]
  pushReservationsResult.value = null
  showPushReservationsDialog.value = true
}

const handlePushReservations = async () => {
  pushReservationsLoading.value = true
  pushReservationsResult.value = null

  try {
    const [startDate, endDate] = pushReservationsRange.value || []
    const res = await priceLabsApi.pushReservations({
      startDate: startDate || undefined,
      endDate: endDate || undefined,
    })
    if (res.success) {
      pushReservationsResult.value = res.data
      ElMessage.success(t('settingsStage4.pricingTools.messages.reservationsPushed'))
      loadSyncLogs()
    } else {
      ElMessage.error(
        res.message || t('settingsStage4.pricingTools.messages.reservationsPushFailed'),
      )
    }
  } catch (error) {
    ElMessage.error(t('settingsStage4.pricingTools.messages.reservationsPushFailed'))
  } finally {
    pushReservationsLoading.value = false
  }
}
</script>

<style scoped>
.pricing-tools-container {
  --pricing-primary: #1685ff;
  --pricing-primary-gradient: linear-gradient(90deg, #81bfff 0%, #017cfe 100%);
  --pricing-primary-gradient-hover: linear-gradient(90deg, #8ec6ff 0%, #1489ff 100%);
  padding: 20px;
  background: #fff;
  min-height: calc(100vh - 100px);
  color: #111111;
  box-sizing: border-box;
}

/* 连接状态视图 */
.connection-view {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 400px;
  padding: 40px 20px;
}

.pricelabs-card {
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 60px 80px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
  text-align: center;
  max-width: 500px;
  width: 100%;
}

.logo-section {
  margin-bottom: 24px;
}

.pricelabs-logo {
  display: inline-flex;
  align-items: center;
  gap: 12px;
}

.logo-icon {
  width: 40px;
  height: 40px;
}

.logo-icon svg {
  width: 100%;
  height: 100%;
}

.logo-text {
  font-size: 28px;
  font-weight: 600;
  color: #303133;
}

.description {
  font-size: 16px;
  color: #606266;
  margin: 0 0 32px 0;
  line-height: 1.6;
}

/* 配置视图 */
.config-view {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* 面包屑 */
.breadcrumb-section {
  padding: 0;
  border-bottom: none;
}

.breadcrumb-section :deep(.el-breadcrumb) {
  display: inline-flex;
  align-items: center;
  min-height: 24px;
  padding: 0 12px;
  border-radius: 999px;
  background: #ffffff;
  font-size: 13px;
  line-height: 24px;
}

.breadcrumb-section :deep(.el-breadcrumb__separator) {
  margin: 0 6px;
  color: #222222;
  font-weight: 400;
}

.breadcrumb-section :deep(.el-button.is-link) {
  height: auto;
  padding: 0;
  color: #111111;
  font-size: 13px;
  font-weight: 500;
}

:deep(.el-breadcrumb__item:last-child .el-breadcrumb__inner) {
  color: #111111;
  font-weight: 500;
}

/* 标签页 */
.config-tabs {
  min-height: calc(100vh - 170px);
  margin-top: 0;
  padding: 18px 20px 28px;
  background: #ffffff;
  border-radius: 4px;
  box-sizing: border-box;
}

.config-tabs :deep(.el-tabs__header) {
  margin: 0 0 20px;
}

.config-tabs :deep(.el-tabs__nav-wrap::after),
.config-tabs :deep(.el-tabs__active-bar) {
  display: none;
}

.config-tabs :deep(.el-tabs__nav-scroll) {
  display: flex;
}

.config-tabs :deep(.el-tabs__nav) {
  display: inline-flex;
  gap: 20px;
  align-items: center;
}

.config-tabs :deep(.el-tabs__item) {
  height: 23px;
  padding: 0 18px !important;
  border-radius: 999px;
  background: #fbfbfb;
  color: #252525;
  font-size: 14px;
  font-weight: 500;
  line-height: 23px;
  letter-spacing: 0;
}

.config-tabs :deep(.el-tabs__item:hover) {
  color: #111111;
}

.config-tabs :deep(.el-tabs__item.is-active),
.config-tabs :deep(.el-tabs__item.is-active:hover) {
  background: #000000;
  color: #ffffff;
  font-weight: 600;
}

.config-tabs :deep(.el-tabs__content) {
  overflow: visible;
}

.connections-panel {
  color: #111111;
}

.connections-panel :deep(.el-button--primary) {
  --el-button-bg-color: var(--pricing-primary);
  --el-button-border-color: var(--pricing-primary);
  --el-button-hover-bg-color: #2f93ff;
  --el-button-hover-border-color: #2f93ff;
  --el-button-active-bg-color: #0d73e8;
  --el-button-active-border-color: #0d73e8;
  height: 32px;
  padding: 0 16px;
  border-radius: 4px;
  font-size: 14px;
  font-weight: 500;
}

.connections-panel :deep(.el-button.is-disabled) {
  opacity: 0.6;
}

.connections-panel :deep(.el-button.is-link) {
  height: auto;
  min-height: 20px;
  padding: 0;
  font-size: 13px;
  font-weight: 500;
}

.connections-panel :deep(.el-button + .el-button) {
  margin-left: 12px;
}

.connections-panel :deep(.el-select__wrapper) {
  min-height: 32px;
  border-radius: 6px;
  box-shadow: 0 0 0 1px #dedede inset;
}

.connections-panel :deep(.el-select__placeholder),
.connections-panel :deep(.el-select__selected-item) {
  color: #8c8c8c;
  font-size: 14px;
}

.config-form {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 8px 16px;
  min-width: 0;
}

.default-account-select {
  width: 276px;
}

.config-form :deep(.el-text) {
  color: #8c8c8c;
  font-size: 14px;
  line-height: 20px;
}

.integration-config-section {
  margin-bottom: 24px;
}

.integration-config-section + .toolbar {
  margin-top: 4px;
}

/* 说明区域 */
.instructions-banner {
  background: rgba(89, 126, 247, 0.15);
  border: 1px solid rgba(89, 126, 247, 0.15);
  border-radius: 4px;
  padding: 8px 18px;
  margin-bottom: 22px;
}

.instruction-item {
  display: flex;
  align-items: flex-start;
  line-height: 26px;
  color: rgba(89, 126, 247, 0.66);
  font-size: 14px;
  font-weight: 400;
}

.instruction-item + .instruction-item {
  margin-top: 4px;
}

.instruction-number {
  flex-shrink: 0;
  margin-right: 2px;
  font-weight: 400;
}

.instruction-text {
  flex: 1;
}

/* 集成状态区域 */
.integration-status-section {
  background: #fbfbfb;
  border: none;
  border-radius: 4px;
  padding: 14px 20px;
  margin-bottom: 24px;
}

.section-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 20px;
}

.section-label {
  flex: 1;
  min-width: 0;
}

.label-title {
  font-size: 16px;
  font-weight: 600;
  color: #111111;
  line-height: 22px;
  margin: 0 0 12px 0;
}

.label-desc {
  font-size: 14px;
  color: #8c8c8c;
  line-height: 20px;
  margin: 0;
}

.status-actions {
  display: flex;
  align-items: center;
  gap: 20px;
  min-height: 28px;
  padding-top: 5px;
}

.status-actions :deep(.el-tag) {
  height: 20px;
  padding: 0 12px;
  border: none;
  border-radius: 10px;
  background: #f4fff8;
  color: #27c571;
  font-size: 12px;
  font-weight: 600;
  line-height: 20px;
}

.status-actions :deep(.el-switch) {
  height: 24px;
}

.status-actions :deep(.el-switch__core) {
  width: 47px;
  min-width: 47px;
  height: 22px;
  border: none;
  background: #d7dce5;
}

.status-actions :deep(.el-switch.is-checked .el-switch__core) {
  background: var(--pricing-primary-gradient);
}

.status-actions :deep(.el-switch.is-checked:hover .el-switch__core) {
  background: var(--pricing-primary-gradient-hover);
}

.status-actions :deep(.el-switch__action) {
  top: 2px;
  left: 2px;
  width: 18px;
  height: 18px;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.18);
}

.status-actions :deep(.el-switch.is-checked .el-switch__action) {
  left: calc(100% - 20px);
}

.sync-stats {
  display: flex;
  gap: 82px;
  margin-top: 18px;
  padding-top: 0;
  border-top: none;
}

.stat-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.stat-label {
  font-size: 14px;
  color: #8c8c8c;
  line-height: 20px;
}

.stat-value {
  font-size: 14px;
  font-weight: 600;
  color: #111111;
  line-height: 20px;
}

/* 工具栏 */
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0;
  margin: 20px 0 10px;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.filter-label {
  font-size: 14px;
  color: #111111;
  font-weight: 600;
  line-height: 20px;
}

/* 表格 */
.config-table {
  width: 100%;
  margin-top: 10px;
  border-radius: 0;
  color: #8c8c8c;
  font-size: 12px;
}

.config-table :deep(.el-table__inner-wrapper::before),
.config-table :deep(.el-table__border-left-patch),
.config-table :deep(.el-table__border-right-patch),
.config-table :deep(.el-table__border-bottom-patch) {
  display: none;
}

.config-table :deep(.el-table__inner-wrapper) {
  border-radius: 0;
}

.config-table :deep(.el-table__cell) {
  border-right: none;
}

.config-table :deep(.el-table__header-wrapper th) {
  height: 29px;
  padding: 0;
  background: #f1f5ff;
  border-bottom: none;
  color: #111111;
  font-size: 12px;
  font-weight: 600;
}

.config-table :deep(.el-table__header-wrapper .cell) {
  line-height: 29px;
}

.config-table :deep(.el-table__body-wrapper td) {
  height: 54px;
  padding: 0;
  border-bottom: 1px solid #eeeeee;
  color: #8c8c8c;
  font-size: 12px;
  font-weight: 400;
}

.config-table :deep(.el-table__body-wrapper .cell) {
  line-height: 20px;
}

.config-table :deep(.el-table__row:hover > td.el-table__cell) {
  background: #fbfdff;
}

.config-table :deep(.el-tag) {
  height: 18px;
  padding: 0;
  border: none;
  background: transparent;
  color: #27c571;
  font-size: 12px;
  font-weight: 600;
  line-height: 18px;
}

.config-table :deep(.el-tag.el-tag--info) {
  color: #8c8c8c;
}

.config-table :deep(.el-tag.el-tag--danger) {
  color: #ff6b6b;
}

.config-table :deep(.el-tag.el-tag--warning) {
  color: #c49b1d;
}

.config-table :deep(.el-button.is-link.el-button--primary) {
  color: #1685ff;
}

.config-table :deep(.el-button.is-link.el-button--warning) {
  color: #c49b1d;
}

.config-table :deep(.el-button.is-link.el-button--danger) {
  color: #ff6b6b;
}

.account-table {
  overflow: hidden;
  margin-top: 10px;
  background: #fbfbfb;
}

.account-table :deep(.el-table__header-wrapper th),
.account-table :deep(.el-table__body-wrapper td) {
  background: #fbfbfb;
  border-bottom: none;
}

.account-table :deep(.el-table__header-wrapper th) {
  height: 28px;
}

.account-table :deep(.el-table__body-wrapper td) {
  height: 44px;
  color: #8c8c8c;
  font-size: 14px;
}

.account-table :deep(.el-table__header-wrapper .cell) {
  color: #111111;
  font-size: 14px;
  font-weight: 600;
}

:deep(.el-table th) {
  background-color: #f1f5ff;
  font-weight: 600;
}

/* 渠道信息 */
.channel-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.channel-name {
  font-weight: 500;
  color: #303133;
}

.channel-code {
  font-size: 12px;
  color: #909399;
}

/* 调整值样式 */
.value-up {
  color: #e6a23c;
  font-weight: 500;
}

.value-down {
  color: #67c23a;
  font-weight: 500;
}

/* 示例计算 */
.example-calc {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
}

.base-price {
  color: #909399;
}

.channel-price {
  color: #409eff;
  font-weight: 500;
}

/* 错误信息 */
.error-message {
  color: #f56c6c;
  font-size: 13px;
}

/* 分页 */
.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

/* 对话框 */
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.status-result-vertical {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.result-block {
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  padding: 12px;
  background: #fafafa;
}

.result-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.result-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.status-item {
  margin-bottom: 12px;
}

.status-item:last-child {
  margin-bottom: 0;
}

.status-item :deep(.el-descriptions) {
  background: #fff;
}

.status-item :deep(.el-descriptions__label) {
  min-width: 100px;
  font-weight: 500;
}

.text-muted {
  color: #909399;
}

.result-json {
  margin: 0;
  font-size: 12px;
  line-height: 1.5;
  color: #606266;
  white-space: pre-wrap;
  word-break: break-word;
}

.unit-label {
  margin-left: 8px;
  color: #606266;
}

.form-tip {
  margin-left: 12px;
  font-size: 12px;
  color: #909399;
}

.example-preview {
  padding: 12px 16px;
  background: #f5f7fa;
  border-radius: 4px;
  color: #606266;
}

.account-filter-tip {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin-top: 10px;
  color: #8c8c8c;
  font-size: 13px;
  line-height: 20px;
}

.pagination-wrapper {
  margin-top: 18px;
}

@media (max-width: 1100px) {
  .section-row {
    flex-direction: column;
    align-items: stretch;
  }

  .config-form,
  .status-actions {
    justify-content: flex-start;
  }

  .sync-stats {
    gap: 32px;
    flex-wrap: wrap;
  }

  .default-account-select {
    width: min(100%, 320px);
  }
}
</style>
