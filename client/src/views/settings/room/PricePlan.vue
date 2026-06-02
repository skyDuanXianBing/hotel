<template>
  <div class="price-plan-container">
    <!-- 标签页 -->
    <el-tabs v-model="activeTab" class="price-tabs">
      <el-tab-pane :label="t('settingsStage4.pricePlan.tabs.plan')" name="plan"></el-tab-pane>
      <el-tab-pane :label="t('settingsStage4.pricePlan.tabs.rate')" name="rate"></el-tab-pane>
    </el-tabs>

    <!-- 价格计划页面 -->
    <div v-if="activeTab === 'plan'">
      <!-- 搜索和按钮 -->
      <div class="toolbar">
        <el-input
          v-model="searchQuery"
          :placeholder="t('settingsStage4.pricePlan.placeholders.searchPlan')"
          :prefix-icon="Search"
          clearable
          style="width: 300px"
        />
        <el-button type="primary" @click="handleAddPlan">{{
          t('settingsStage4.pricePlan.actions.addPlan')
        }}</el-button>
      </div>

      <!-- 价格计划表格 -->
      <el-table :data="pricePlans" stripe border style="width: 100%">
        <el-table-column
          prop="name"
          :label="t('settingsStage4.pricePlan.columns.pricePlan')"
          min-width="150"
        />
        <el-table-column
          prop="minNights"
          :label="t('settingsStage4.pricePlan.columns.minNights')"
          width="120"
          align="center"
        />
        <el-table-column
          prop="maxNights"
          :label="t('settingsStage4.pricePlan.columns.maxNights')"
          width="120"
          align="center"
        />
        <el-table-column
          :label="t('settingsStage4.pricePlan.columns.includeMeal')"
          width="100"
          align="center"
        >
          <template #default="{ row }">
            <el-icon v-if="row.includeMeal" color="#67C23A">
              <CircleCheck />
            </el-icon>
          </template>
        </el-table-column>
        <el-table-column
          prop="rules"
          :label="t('settingsStage4.pricePlan.columns.derivationRules')"
          min-width="150"
        />
        <el-table-column
          :label="t('settingsStage4.pricePlan.columns.appliedRoomTypes')"
          min-width="200"
        >
          <template #default="{ row }">
            <el-link type="primary" :underline="false" @click="handleShowAppliedRoomTypes(row)">
              <el-icon><Link /></el-icon>
              {{ t('settingsStage4.pricePlan.roomTypesCount', { count: row.roomTypesCount }) }}
            </el-link>
          </template>
        </el-table-column>
        <el-table-column
          :label="t('settingsStage4.accountList.columns.actions')"
          width="200"
          align="center"
          fixed="right"
        >
          <template #default="{ row }">
            <el-button link type="primary" @click="handleEdit(row)">{{
              t('settings.common.edit')
            }}</el-button>
            <el-button link type="primary" @click="handleDetail(row)">{{
              t('settingsStage4.common.details')
            }}</el-button>
            <el-button link type="danger" @click="handleDelete(row)">{{
              t('settings.common.delete')
            }}</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 房费页面 -->
    <div v-else-if="activeTab === 'rate'">
      <!-- 搜索框 -->
      <div class="toolbar">
        <el-input
          v-model="roomTypeSearchQuery"
          :placeholder="t('settingsStage4.pricePlan.placeholders.searchRoomType')"
          :prefix-icon="Search"
          clearable
          style="width: 300px"
        />
      </div>

      <!-- 房型价格列表 -->
      <div v-for="roomType in roomTypes" :key="roomType.id" class="room-type-section">
        <div class="room-type-header">
          <div class="room-type-title">
            <el-icon><House /></el-icon>
            <span class="room-type-name">{{ roomType.name }}</span>
          </div>
          <el-button type="primary" link @click="handleAssignPricePlan(roomType)">
            + {{ t('settingsStage4.pricePlan.actions.assignPricePlan') }}
          </el-button>
        </div>

        <!-- 价格表格 -->
        <el-table :data="roomType.pricePlans" border class="rate-table">
          <el-table-column :label="t('settingsStage4.pricePlan.columns.pricePlan')" width="150">
            <template #default="{ row }">
              <div class="plan-name-cell">
                <el-icon class="arrow-icon"><ArrowLeft /></el-icon>
                <span>{{ row.name }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column
            :label="t('settingsStage4.pricePlan.columns.priceMode')"
            width="120"
            align="center"
          >
            <template #default>
              <el-tag type="primary" size="small">RBP</el-tag>
            </template>
          </el-table-column>
          <el-table-column
            :label="t('settingsStage4.pricePlan.columns.defaultPrice')"
            align="center"
          >
            <el-table-column
              :label="t('settingsStage4.weekdays.monShort')"
              width="100"
              align="center"
            >
              <template #default="{ row }">{{ formatPrice(row.prices.mon) }}</template>
            </el-table-column>
            <el-table-column
              :label="t('settingsStage4.weekdays.tueShort')"
              width="100"
              align="center"
            >
              <template #default="{ row }">{{ formatPrice(row.prices.tue) }}</template>
            </el-table-column>
            <el-table-column
              :label="t('settingsStage4.weekdays.wedShort')"
              width="100"
              align="center"
            >
              <template #default="{ row }">{{ formatPrice(row.prices.wed) }}</template>
            </el-table-column>
            <el-table-column
              :label="t('settingsStage4.weekdays.thuShort')"
              width="100"
              align="center"
            >
              <template #default="{ row }">{{ formatPrice(row.prices.thu) }}</template>
            </el-table-column>
            <el-table-column
              :label="t('settingsStage4.weekdays.friShort')"
              width="100"
              align="center"
            >
              <template #default="{ row }">{{ formatPrice(row.prices.fri) }}</template>
            </el-table-column>
            <el-table-column
              :label="t('settingsStage4.weekdays.satShort')"
              width="100"
              align="center"
            >
              <template #default="{ row }">{{ formatPrice(row.prices.sat) }}</template>
            </el-table-column>
            <el-table-column
              :label="t('settingsStage4.weekdays.sunShort')"
              width="100"
              align="center"
            >
              <template #default="{ row }">{{ formatPrice(row.prices.sun) }}</template>
            </el-table-column>
          </el-table-column>
          <el-table-column
            :label="t('settingsStage4.pricePlan.columns.maxGuests')"
            width="120"
            align="center"
            prop="maxGuests"
          />
          <el-table-column
            :label="t('settingsStage4.pricePlan.columns.includedGuests')"
            width="100"
            align="center"
            prop="includedGuests"
          />
          <el-table-column
            :label="t('settingsStage4.accountList.columns.actions')"
            width="150"
            align="center"
            fixed="right"
          >
            <template #default="{ row }">
              <el-button link type="primary" @click="handleEditRate(roomType, row)">
                {{ t('settings.common.edit') }}
              </el-button>
              <el-button link type="danger" @click="handleDeleteRate(roomType, row)">
                {{ t('settings.common.delete') }}
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <!-- 编辑价格计划对话框 -->
    <el-dialog
      v-model="editDialogVisible"
      :title="t('settingsStage4.pricePlan.dialog.editPlan')"
      width="600px"
      :close-on-click-modal="false"
    >
      <el-form :model="editForm" :rules="formRules" ref="editFormRef" label-width="140px">
        <el-form-item :label="t('settingsStage4.pricePlan.columns.pricePlan')" prop="name" required>
          <el-input
            v-model="editForm.name"
            :placeholder="t('settingsStage4.pricePlan.placeholders.planName')"
          />
        </el-form-item>

        <el-form-item
          :label="t('settingsStage4.pricePlan.fields.defaultMinNights')"
          prop="minNights"
          required
        >
          <el-input-number
            v-model="editForm.minNights"
            :min="1"
            :max="365"
            controls-position="right"
            class="night-count-input"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item
          :label="t('settingsStage4.pricePlan.fields.defaultMaxNights')"
          prop="maxNights"
        >
          <el-input-number
            v-model="editForm.maxNights"
            :min="1"
            :max="365"
            controls-position="right"
            class="night-count-input"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item
          :label="t('settingsStage4.pricePlan.fields.derivationType')"
          prop="derivationType"
          required
        >
          <div class="form-item-hint">
            {{ t('settingsStage4.pricePlan.hints.derivationType') }}
          </div>
          <el-radio-group v-model="editForm.derivationType">
            <el-radio value="independent">
              {{ t('settingsStage4.pricePlan.fields.manualDailyRate') }}
              <el-tooltip
                :content="t('settingsStage4.pricePlan.hints.independentPlan')"
                placement="top"
              >
                <el-icon><QuestionFilled /></el-icon>
              </el-tooltip>
            </el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item
          :label="t('settingsStage4.pricePlan.columns.includeMeal')"
          prop="includeMeal"
          required
          class="include-meal-item"
        >
          <el-radio-group v-model="editForm.includeMeal" class="include-meal-group">
            <el-radio :value="true">{{ t('settingsStage4.pricePlan.yes') }}</el-radio>
            <el-radio :value="false">{{ t('settingsStage4.pricePlan.no') }}</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="handleCancelEdit">{{ t('settings.common.cancel') }}</el-button>
          <el-button type="primary" @click="handleConfirmEdit">{{
            t('settings.common.confirm')
          }}</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 价格计划详情对话框 -->
    <el-dialog
      v-model="detailDialogVisible"
      :title="t('settingsStage4.pricePlan.dialog.details')"
      width="700px"
      :close-on-click-modal="false"
    >
      <div class="detail-hint">
        {{ t('settingsStage4.pricePlan.hints.details') }}
      </div>

      <el-form :model="detailForm" ref="detailFormRef" label-width="120px" class="detail-form">
        <el-form-item :label="t('settingsStage4.pricePlan.columns.pricePlan')" required>
          <div class="form-item-label">{{ t('settingsStage4.pricePlan.fields.english') }}</div>
          <el-input v-model="detailForm.name" maxlength="64" show-word-limit />
          <el-button link type="primary" class="localize-btn">
            <el-icon><ArrowLeft /></el-icon>
            {{ t('settingsStage4.pricePlan.actions.localize') }}
          </el-button>
        </el-form-item>

        <el-form-item :label="t('settingsStage4.pricePlan.fields.planDescription')" required>
          <div class="form-item-label">{{ t('settingsStage4.pricePlan.fields.english') }}</div>
          <el-input
            v-model="detailForm.description"
            type="textarea"
            :rows="4"
            maxlength="256"
            show-word-limit
          />
          <el-button link type="primary" class="localize-btn">
            <el-icon><ArrowLeft /></el-icon>
            {{ t('settingsStage4.pricePlan.actions.localize') }}
          </el-button>
        </el-form-item>

        <el-form-item :label="t('settingsStage4.pricePlan.fields.cancellationPolicy')" required>
          <div class="form-item-label">{{ t('settingsStage4.pricePlan.fields.english') }}</div>
          <el-input
            v-model="detailForm.cancellationPolicy"
            type="textarea"
            :rows="4"
            maxlength="256"
            show-word-limit
          />
          <el-button link type="primary" class="localize-btn">
            <el-icon><ArrowLeft /></el-icon>
            {{ t('settingsStage4.pricePlan.actions.localize') }}
          </el-button>
        </el-form-item>

        <el-form-item :label="t('settingsStage4.pricePlan.fields.breakfast')" required>
          <div class="form-item-label">{{ t('settingsStage4.pricePlan.fields.english') }}</div>
          <el-radio-group v-model="detailForm.includeMeal">
            <el-radio :value="true">{{ t('settingsStage4.pricePlan.fields.included') }}</el-radio>
            <el-radio :value="false">{{
              t('settingsStage4.pricePlan.fields.notIncluded')
            }}</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="handleCancelDetail">{{ t('settings.common.cancel') }}</el-button>
          <el-button type="primary" @click="handleConfirmDetail">{{
            t('settings.common.confirm')
          }}</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 编辑房价对话框 -->
    <el-dialog
      v-model="editRateDialogVisible"
      :title="t('settingsStage4.pricePlan.dialog.editRate')"
      width="900px"
      :close-on-click-modal="false"
    >
      <div class="edit-rate-content">
        <!-- 价格计划名称 -->
        <div class="rate-plan-name">{{ currentEditRate?.planName }}</div>

        <!-- 快速填充 -->
        <div class="quick-fill-section">
          <span class="section-label">{{ t('settingsStage4.pricePlan.fields.quickFill') }}</span>
          <el-input-number
            v-model="quickFillPrice"
            :min="0"
            :controls="false"
            :precision="0"
            class="quick-fill-input"
            placeholder="0.00"
          >
            <template #prefix>¥</template>
          </el-input-number>
        </div>

        <!-- 每日价格 -->
        <div class="daily-prices-section">
          <div class="daily-price-item" v-for="day in weekDays" :key="day.key">
            <div class="day-label">{{ day.label }}</div>
            <el-input-number
              v-model="dailyPrices[day.key as keyof typeof dailyPrices]"
              :min="0"
              :controls="false"
              :precision="0"
              class="price-input"
            >
              <template #prefix>¥</template>
            </el-input-number>
          </div>
        </div>

        <!-- 价格选项 -->
        <div class="price-options-section">
          <div class="section-label">{{ t('settingsStage4.pricePlan.fields.priceOptions') }}</div>
          <div class="price-option-buttons">
            <el-button
              :type="priceOption === 'unified' ? 'primary' : 'default'"
              @click="priceOption = 'unified'"
              class="option-button"
            >
              <div class="option-content">
                <el-icon v-if="priceOption === 'unified'" class="check-icon"><Check /></el-icon>
                <span>{{ t('settingsStage4.pricePlan.fields.unifiedPrice') }}</span>
              </div>
              <el-tooltip
                :content="t('settingsStage4.pricePlan.hints.unifiedPrice')"
                placement="top"
              >
                <el-icon class="help-icon"><QuestionFilled /></el-icon>
              </el-tooltip>
            </el-button>
            <el-button
              :type="priceOption === 'multiple' ? 'primary' : 'default'"
              @click="priceOption = 'multiple'"
              class="option-button"
            >
              <div class="option-content">
                <el-icon v-if="priceOption === 'multiple'" class="check-icon"><Check /></el-icon>
                <span>{{ t('settingsStage4.pricePlan.fields.extraCharge') }}</span>
              </div>
              <el-tooltip
                :content="t('settingsStage4.pricePlan.hints.extraCharge')"
                placement="top"
              >
                <el-icon class="help-icon"><QuestionFilled /></el-icon>
              </el-tooltip>
            </el-button>
          </div>
        </div>

        <!-- 包含人数 -->
        <div v-if="priceOption === 'multiple'" class="included-guests-section">
          <div class="section-label">
            {{ t('settingsStage4.pricePlan.fields.baseIncludedGuests') }}
          </div>
          <el-select v-model="includedGuestsCount" style="width: 300px">
            <el-option
              v-for="n in includedGuestsOptions"
              :key="n"
              :label="t('settingsStage4.pricePlan.personCount', { count: n })"
              :value="String(n)"
            />
          </el-select>
        </div>

        <!-- 额外成人/儿童加价 -->
        <div v-if="priceOption === 'multiple'" class="extra-guests-section">
          <div class="section-label">
            {{ t('settingsStage4.pricePlan.fields.extraGuestRates') }}
          </div>

          <div class="extra-rate-row">
            <div class="extra-rate-item">
              <div class="extra-rate-label">
                {{ t('settingsStage4.pricePlan.fields.extraAdultRate') }}
              </div>
              <el-input-number
                v-model="extraAdultRate"
                :min="0"
                :controls="false"
                :precision="0"
                class="extra-rate-input"
              >
                <template #prefix>+ ¥</template>
              </el-input-number>
            </div>

            <div class="extra-rate-item">
              <div class="extra-rate-label">
                {{ t('settingsStage4.pricePlan.fields.extraChildRate') }}
              </div>
              <el-input-number
                v-model="extraChildRate"
                :min="0"
                :controls="false"
                :precision="0"
                class="extra-rate-input"
              >
                <template #prefix>+ ¥</template>
              </el-input-number>
            </div>
          </div>
        </div>

        <div class="clear-overrides-section">
          <el-checkbox v-model="clearFutureOverridesOnSave">
            {{ t('settingsStage4.pricePlan.fields.clearFutureOverrides') }}
          </el-checkbox>
        </div>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="handleCancelEditRate">{{ t('settings.common.cancel') }}</el-button>
          <el-button type="primary" @click="handleConfirmEditRate">{{
            t('settings.common.confirm')
          }}</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 分配价格计划对话框 -->
    <el-dialog
      v-model="assignPricePlanDialogVisible"
      :title="t('settingsStage4.pricePlan.dialog.linkPricePlan')"
      width="800px"
      :close-on-click-modal="false"
    >
      <div class="assign-price-plan-content">
        <div class="selection-row">
          <div class="selection-item">
            <div class="selection-label">
              {{ t('settingsStage4.cleaningSettings.fields.roomType') }}
            </div>
            <el-input
              :model-value="`${currentAssignRoomType?.storeName} ${currentAssignRoomType?.name}`"
              disabled
              class="selection-input"
            >
              <template #suffix>
                <el-icon><Link /></el-icon>
              </template>
            </el-input>
          </div>

          <el-icon class="link-icon"><Link /></el-icon>

          <div class="selection-item">
            <div class="selection-label">{{ t('settingsStage4.pricePlan.columns.pricePlan') }}</div>
            <el-select
              v-model="selectedAssignPricePlanId"
              :placeholder="t('settingsStage4.pricePlan.placeholders.selectPricePlan')"
              class="selection-input"
              filterable
            >
              <el-option
                v-for="plan in filteredAssignPricePlans"
                :key="plan.id"
                :label="plan.name"
                :value="plan.id"
              >
                <div style="display: flex; align-items: center; justify-content: space-between">
                  <span>{{ plan.name }}</span>
                  <el-icon v-if="plan.isAssigned"><Link /></el-icon>
                </div>
              </el-option>
            </el-select>
          </div>
        </div>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="handleCancelAssignPricePlan">{{
            t('settings.common.cancel')
          }}</el-button>
          <el-button type="primary" @click="handleConfirmAssignPricePlan">{{
            t('settings.common.confirm')
          }}</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 应用房型对话框 -->
    <el-dialog
      v-model="appliedRoomTypesDialogVisible"
      :title="t('settingsStage4.pricePlan.dialog.appliedRoomTypes')"
      width="600px"
      :close-on-click-modal="false"
    >
      <div class="applied-room-types-content">
        <div class="price-plan-title">{{ currentPricePlan?.name }}</div>

        <div class="room-type-tags">
          <el-tag
            v-for="roomType in appliedRoomTypes"
            :key="roomType.id"
            closable
            @close="handleRemoveRoomType(roomType)"
            size="large"
            class="room-type-tag"
          >
            {{ roomType.name }} {{ roomType.code }}
          </el-tag>
        </div>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="appliedRoomTypesDialogVisible = false">{{
            t('settings.common.cancel')
          }}</el-button>
          <el-button type="primary" @click="handleShowLinkRoomTypes">{{
            t('settingsStage4.pricePlan.actions.linkRoomTypes')
          }}</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 关联房型对话框 -->
    <el-dialog
      v-model="linkRoomTypesDialogVisible"
      :title="t('settingsStage4.pricePlan.actions.linkRoomTypes')"
      width="800px"
      :close-on-click-modal="false"
    >
      <div class="link-room-types-content">
        <div class="selection-row">
          <div class="selection-item">
            <div class="selection-label">{{ t('settingsStage4.pricePlan.columns.pricePlan') }}</div>
            <el-select v-model="selectedPricePlanId" disabled class="selection-input">
              <el-option
                v-for="plan in pricePlans"
                :key="plan.id"
                :label="plan.name"
                :value="plan.id"
              />
            </el-select>
          </div>

          <el-icon class="link-icon"><Link /></el-icon>

          <div class="selection-item">
            <div class="selection-label">
              {{ t('settingsStage4.cleaningSettings.fields.roomType') }}
            </div>
            <el-select
              v-model="selectedRoomTypeIds"
              multiple
              collapse-tags
              collapse-tags-tooltip
              :placeholder="t('settingsStage4.pricePlan.placeholders.chooseRoomType')"
              class="selection-input"
              @visible-change="handleRoomTypeSelectVisible"
              @change="handleRoomTypeSelectionChange"
            >
              <template #header>
                <el-input
                  v-model="roomTypeFilterKeyword"
                  :placeholder="t('settingsStage4.pricePlan.placeholders.filterKeyword')"
                  clearable
                  @input="handleFilterRoomTypes"
                />
              </template>
              <el-option :label="t('settingsStage4.cleaningSettings.all')" value="all" />
              <el-option
                v-for="roomType in filteredAvailableRoomTypes"
                :key="roomType.id"
                :label="`${roomType.name}　${roomType.code}`"
                :value="roomType.id"
              >
                <div class="room-type-option">
                  <span>{{ roomType.name }}　{{ roomType.code }}</span>
                  <el-icon class="option-link-icon"><Link /></el-icon>
                </div>
              </el-option>
            </el-select>
          </div>
        </div>

        <!-- 默认市价区域 -->
        <div v-if="selectedRoomTypeIdsForPrices.length > 0" class="default-prices-section">
          <div class="section-title">
            {{ t('settingsStage4.pricePlan.fields.defaultMarketPrice') }}
          </div>

          <!-- 每个选中的房型显示价格输入区域 -->
          <div
            v-for="roomTypeId in selectedRoomTypeIdsForPrices"
            :key="roomTypeId"
            class="room-type-prices"
          >
            <div class="room-type-prices-header">
              {{ getRoomTypeName(roomTypeId) }} &lt;{{ selectedPricePlanName }}&gt;
            </div>

            <div class="price-input-section">
              <!-- 快速填充 -->
              <div class="quick-fill-section">
                <span class="section-label">{{
                  t('settingsStage4.pricePlan.fields.quickFill')
                }}</span>
                <el-input-number
                  v-model="quickFillPrices[roomTypeId]"
                  :min="0"
                  :controls="false"
                  placeholder="0"
                  class="quick-fill-input"
                  @change="handleQuickFill(roomTypeId)"
                >
                  <template #prefix>¥</template>
                </el-input-number>
              </div>

              <!-- 每日价格 -->
              <div class="daily-prices-grid">
                <div class="price-item">
                  <span class="weekday-label">{{ t('settingsStage4.weekdays.monShort') }}</span>
                  <el-input-number
                    v-model="linkRoomTypePrices[roomTypeId].mon"
                    :min="0"
                    :controls="false"
                    class="price-input-number"
                  >
                    <template #prefix>¥</template>
                  </el-input-number>
                </div>

                <div class="price-item">
                  <span class="weekday-label">{{ t('settingsStage4.weekdays.tueShort') }}</span>
                  <el-input-number
                    v-model="linkRoomTypePrices[roomTypeId].tue"
                    :min="0"
                    :controls="false"
                    class="price-input-number"
                  >
                    <template #prefix>¥</template>
                  </el-input-number>
                </div>

                <div class="price-item">
                  <span class="weekday-label">{{ t('settingsStage4.weekdays.wedShort') }}</span>
                  <el-input-number
                    v-model="linkRoomTypePrices[roomTypeId].wed"
                    :min="0"
                    :controls="false"
                    class="price-input-number"
                  >
                    <template #prefix>¥</template>
                  </el-input-number>
                </div>

                <div class="price-item">
                  <span class="weekday-label">{{ t('settingsStage4.weekdays.thuShort') }}</span>
                  <el-input-number
                    v-model="linkRoomTypePrices[roomTypeId].thu"
                    :min="0"
                    :controls="false"
                    class="price-input-number"
                  >
                    <template #prefix>¥</template>
                  </el-input-number>
                </div>

                <div class="price-item">
                  <span class="weekday-label">{{ t('settingsStage4.weekdays.friShort') }}</span>
                  <el-input-number
                    v-model="linkRoomTypePrices[roomTypeId].fri"
                    :min="0"
                    :controls="false"
                    class="price-input-number"
                  >
                    <template #prefix>¥</template>
                  </el-input-number>
                </div>

                <div class="price-item">
                  <span class="weekday-label">{{ t('settingsStage4.weekdays.satShort') }}</span>
                  <el-input-number
                    v-model="linkRoomTypePrices[roomTypeId].sat"
                    :min="0"
                    :controls="false"
                    class="price-input-number"
                  >
                    <template #prefix>¥</template>
                  </el-input-number>
                </div>

                <div class="price-item">
                  <span class="weekday-label">{{ t('settingsStage4.weekdays.sunShort') }}</span>
                  <el-input-number
                    v-model="linkRoomTypePrices[roomTypeId].sun"
                    :min="0"
                    :controls="false"
                    class="price-input-number"
                  >
                    <template #prefix>¥</template>
                  </el-input-number>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="handleCancelLinkRoomTypes">{{
            t('settings.common.cancel')
          }}</el-button>
          <el-button type="primary" @click="handleConfirmLinkRoomTypes">{{
            t('settings.common.confirm')
          }}</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  Search,
  CircleCheck,
  Link,
  QuestionFilled,
  House,
  ArrowLeft,
  Check,
  User,
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { useUserStore } from '@/stores/user'
import {
  getAllPricePlans,
  createPricePlan,
  updatePricePlan,
  deletePricePlan,
  forceDeletePricePlan,
  getPricePlansByRoomType,
  assignPricePlanToRoomType,
  updateRoomTypePricePlan,
  deleteRoomTypePricePlan,
  countRoomTypesByPricePlan,
  getRoomTypesByPricePlan,
  type PricePlanDTO,
  type RoomTypePricePlanDTO,
} from '@/api/pricePlan'
import { getAllRoomTypes, type RoomTypeDTO } from '@/api/roomType'
import { getUserStores, type StoreDTO } from '@/api/store'
import { getStoreTodayYmd } from '@/utils/storeDateTime'

const userStore = useUserStore()
const { t } = useI18n()

interface PricePlan {
  id: number
  name: string
  minNights: number
  maxNights: number
  includeMeal: boolean
  rules: string
  roomTypesCount: number
  derivationType?: string
  description?: string
  cancellationPolicy?: string
}

interface EditForm {
  name: string
  minNights: number
  maxNights: number
  includeMeal: boolean
  derivationType: string
}

interface DetailForm {
  name: string
  nameEn: string
  description: string
  descriptionEn: string
  cancellationPolicy: string
  cancellationPolicyEn: string
  includeMeal: boolean
}

interface RoomTypePricePlan {
  id: number
  name: string
  prices: {
    mon: number
    tue: number
    wed: number
    thu: number
    fri: number
    sat: number
    sun: number
  }
  maxGuests: number
  includedGuests: string
  includedGuestsValue?: number | null
  extraAdultRate?: number
  extraChildRate?: number
  pricePlanId?: number
  roomTypeId?: number
  priceMode?: string
}

interface RoomType {
  id: number
  name: string
  code: string
  storeName?: string
  pricePlans: RoomTypePricePlan[]
  roomTypePricePlanId?: number
}

const activeTab = ref('plan')
const searchQuery = ref('')
const roomTypeSearchQuery = ref('')
const editDialogVisible = ref(false)
const editFormRef = ref<FormInstance>()
const currentEditId = ref<number | null>(null)

// 详情对话框
const detailDialogVisible = ref(false)
const detailFormRef = ref<FormInstance>()
const detailForm = reactive<DetailForm>({
  name: '',
  nameEn: '',
  description: '',
  descriptionEn: '',
  cancellationPolicy: '',
  cancellationPolicyEn: '',
  includeMeal: false,
})
const currentDetailPlanId = ref<number | null>(null)

// 应用房型对话框
const appliedRoomTypesDialogVisible = ref(false)
const linkRoomTypesDialogVisible = ref(false)
const currentPricePlan = ref<PricePlan | null>(null)
const appliedRoomTypes = ref<RoomType[]>([])
const selectedPricePlanId = ref<number | null>(null)
type RoomTypeSelectValue = number | 'all'
const selectedRoomTypeIds = ref<RoomTypeSelectValue[]>([])
const roomTypeFilterKeyword = ref('')
const filteredAvailableRoomTypes = ref<RoomType[]>([])

// 关联房型时的价格数据
const linkRoomTypePrices = ref<
  Record<
    number,
    {
      mon: number
      tue: number
      wed: number
      thu: number
      fri: number
      sat: number
      sun: number
    }
  >
>({})

// 快速填充价格
const quickFillPrices = ref<Record<number, number>>({})

// 分配价格计划对话框
const assignPricePlanDialogVisible = ref(false)
const currentAssignRoomType = ref<RoomType | null>(null)
const selectedAssignPricePlanId = ref<number | null>(null)
const assignPricePlanFilterKeyword = ref('')

// 编辑房价对话框
const editRateDialogVisible = ref(false)
const currentEditRate = ref<{
  roomType: RoomType
  plan: RoomTypePricePlan
  planName: string
} | null>(null)
const quickFillPrice = ref(0)
const priceOption = ref<'unified' | 'multiple'>('unified')
const includedGuestsCount = ref('2')
const extraAdultRate = ref(0)
const extraChildRate = ref(0)
const clearFutureOverridesOnSave = ref(true)

const includedGuestsOptions = computed(() => {
  const maxGuests = currentEditRate.value?.plan?.maxGuests ?? 4
  const normalizedMax = Math.max(1, Number.isFinite(maxGuests) ? maxGuests : 4)
  return Array.from({ length: normalizedMax }, (_, i) => i + 1)
})

// 每日价格
const dailyPrices = reactive({
  mon: 0,
  tue: 0,
  wed: 0,
  thu: 0,
  fri: 0,
  sat: 0,
  sun: 0,
})

// 额外人数加价（第1-4人）
// 已移除“多人价阶梯表”相关状态：现在改为“额外成人/儿童加价（统一值）”

// 额外人数快速填充价格
// 星期配置
const weekDays = computed(() => [
  { key: 'mon', label: t('settingsStage4.weekdays.monShort') },
  { key: 'tue', label: t('settingsStage4.weekdays.tueShort') },
  { key: 'wed', label: t('settingsStage4.weekdays.wedShort') },
  { key: 'thu', label: t('settingsStage4.weekdays.thuShort') },
  { key: 'fri', label: t('settingsStage4.weekdays.friShort') },
  { key: 'sat', label: t('settingsStage4.weekdays.satShort') },
  { key: 'sun', label: t('settingsStage4.weekdays.sunShort') },
])

// 编辑表单数据
const editForm = reactive<EditForm>({
  name: '',
  minNights: 1,
  maxNights: 365,
  includeMeal: false,
  derivationType: 'independent',
})

// 表单验证规则
const formRules = computed<FormRules>(() => ({
  name: [
    {
      required: true,
      message: t('settingsStage4.pricePlan.validation.planNameRequired'),
      trigger: 'blur',
    },
  ],
  minNights: [
    {
      required: true,
      message: t('settingsStage4.pricePlan.validation.minNightsRequired'),
      trigger: 'blur',
    },
  ],
  derivationType: [
    {
      required: true,
      message: t('settingsStage4.pricePlan.validation.derivationTypeRequired'),
      trigger: 'change',
    },
  ],
  includeMeal: [
    {
      required: true,
      message: t('settingsStage4.pricePlan.validation.includeMealRequired'),
      trigger: 'change',
    },
  ],
}))

// 数据
const pricePlans = ref<PricePlan[]>([])
const roomTypes = ref<RoomType[]>([])
const allRoomTypes = ref<RoomTypeDTO[]>([])
const storeInfo = ref<StoreDTO | null>(null)
const loading = ref(false)

// 计算属性:过滤价格计划列表(用于分配价格计划弹窗)
const filteredAssignPricePlans = computed(() => {
  if (!currentAssignRoomType.value) return []

  // 获取当前房型已关联的价格计划ID
  const assignedPlanIds = currentAssignRoomType.value.pricePlans.map((pp) => pp.pricePlanId)

  return pricePlans.value.map((plan) => ({
    ...plan,
    isAssigned: assignedPlanIds.includes(plan.id),
  }))
})

// 计算属性:过滤出数字类型的房型ID(排除"all")
const selectedRoomTypeIdsForPrices = computed(() => {
  return selectedRoomTypeIds.value.filter((id) => typeof id === 'number') as number[]
})

// 计算属性:选中的价格计划名称
const selectedPricePlanName = computed(() => {
  const plan = pricePlans.value.find((p) => p.id === selectedPricePlanId.value)
  return plan?.name || ''
})

// 计算属性:需要显示的额外人数（超出包含人数的部分）
// 获取房型名称
const getRoomTypeName = (roomTypeId: number) => {
  const roomType = allRoomTypes.value.find((rt) => rt.id === roomTypeId)
  return roomType ? `${roomType.name} <${roomType.code}>` : ''
}

// 快速填充价格 - 将快速填充的价格应用到所有星期
const handleQuickFill = (roomTypeId: number) => {
  const fillPrice = quickFillPrices.value[roomTypeId]
  if (fillPrice !== undefined && fillPrice >= 0) {
    linkRoomTypePrices.value[roomTypeId] = {
      mon: fillPrice,
      tue: fillPrice,
      wed: fillPrice,
      thu: fillPrice,
      fri: fillPrice,
      sat: fillPrice,
      sun: fillPrice,
    }
  }
}

// 额外人数快速填充
// 加载价格计划列表
const loadPricePlans = async () => {
  if (!userStore.currentUser?.id) return

  try {
    loading.value = true
    const response = (await getAllPricePlans(userStore.currentUser.id)) as any
    if (!response?.success) {
      throw new Error(
        response?.message || t('settingsStage4.pricePlan.messages.loadPricePlansFailed'),
      )
    }
    const plans = Array.isArray(response.data) ? response.data : []

    // 加载每个价格计划的房型数量
    const plansWithCount = await Promise.all(
      plans.map(async (plan: PricePlanDTO) => {
        const countResponse = (await countRoomTypesByPricePlan(plan.id!)) as any
        const count = countResponse?.success ? countResponse.data || 0 : 0
        return {
          id: plan.id!,
          name: plan.name,
          minNights: plan.minNights,
          maxNights: plan.maxNights || 365,
          includeMeal: plan.includeMeal,
          rules: plan.derivationRule || '',
          roomTypesCount: count,
          derivationType: plan.derivationType,
          description: plan.description,
          cancellationPolicy: plan.cancellationPolicy,
        }
      }),
    )

    pricePlans.value = plansWithCount
  } catch (error) {
    ElMessage.error(t('settingsStage4.pricePlan.messages.loadPricePlansFailed'))
    console.error('加载价格计划失败:', error)
  } finally {
    loading.value = false
  }
}

// 加载所有房型
const loadAllRoomTypes = async () => {
  if (!userStore.currentUser?.id) return

  try {
    const response = (await getAllRoomTypes()) as any
    allRoomTypes.value = response.data || []
  } catch (error) {
    ElMessage.error(t('settingsStage4.pricePlan.messages.loadRoomTypesFailed'))
    console.error('加载房型失败:', error)
  }
}

// 加载门店信息
const loadStoreInfo = async () => {
  if (!userStore.currentUser?.id) return

  try {
    const response = (await getUserStores()) as any
    const stores = response.data || []
    // 获取第一个门店信息
    if (stores.length > 0) {
      storeInfo.value = stores[0]
    }
  } catch (error) {
    console.error('加载门店信息失败:', error)
  }
}

// 加载房型价格数据
const loadRoomTypePrices = async () => {
  if (!userStore.currentUser?.id) return

  try {
    loading.value = true

    // 加载所有房型
    const roomTypesResponse = (await getAllRoomTypes()) as any
    const roomTypesData: RoomTypeDTO[] = roomTypesResponse.data || []

    // 为每个房型加载关联的价格计划
    const roomTypesWithPrices = await Promise.all(
      roomTypesData.map(async (roomType: RoomTypeDTO) => {
        const pricePlansResponse = (await getPricePlansByRoomType(roomType.id!)) as any
        const pricePlansData: RoomTypePricePlanDTO[] = pricePlansResponse.data || []

        const pricePlans = pricePlansData.map((pp) => ({
          id: pp.id!,
          name: pp.pricePlan?.name || '',
          prices: {
            mon: pp.mondayPrice || roomType.monPrice || 0,
            tue: pp.tuesdayPrice || roomType.tuePrice || 0,
            wed: pp.wednesdayPrice || roomType.wedPrice || 0,
            thu: pp.thursdayPrice || roomType.thuPrice || 0,
            fri: pp.fridayPrice || roomType.friPrice || 0,
            sat: pp.saturdayPrice || roomType.satPrice || 0,
            sun: pp.sundayPrice || roomType.sunPrice || 0,
          },
          maxGuests: pp.maxGuests,
          includedGuests: pp.includedGuests?.toString() || '-',
          includedGuestsValue: pp.includedGuests ?? null,
          extraAdultRate: pp.extraAdultRate ?? 0,
          extraChildRate: pp.extraChildRate ?? 0,
          pricePlanId: pp.pricePlan?.id,
          roomTypeId: roomType.id,
          priceMode: pp.priceMode,
        }))

        return {
          id: roomType.id!,
          name: roomType.name,
          code: roomType.code || '',
          storeName: storeInfo.value?.name || '',
          pricePlans,
        }
      }),
    )

    roomTypes.value = roomTypesWithPrices
  } catch (error) {
    ElMessage.error(t('settingsStage4.pricePlan.messages.loadRoomPricesFailed'))
    console.error('加载房型价格失败:', error)
  } finally {
    loading.value = false
  }
}

// 初始化数据
onMounted(async () => {
  await loadStoreInfo()
  loadPricePlans()
  loadAllRoomTypes()
  loadRoomTypePrices()
})

const formatPrice = (price: number) => {
  return `¥${price.toLocaleString()}`
}

const handleAddPlan = () => {
  editForm.name = ''
  editForm.minNights = 1
  editForm.maxNights = 365
  editForm.includeMeal = false
  editForm.derivationType = 'independent'
  currentEditId.value = null
  editDialogVisible.value = true
}

const handleEdit = (row: PricePlan) => {
  editForm.name = row.name
  editForm.minNights = row.minNights
  editForm.maxNights = row.maxNights
  editForm.includeMeal = row.includeMeal
  editForm.derivationType = 'independent'
  currentEditId.value = row.id
  editDialogVisible.value = true
}

const handleDetail = (row: PricePlan) => {
  // 填充详情表单数据
  detailForm.name = row.name
  detailForm.nameEn = row.name
  detailForm.description = row.description || ''
  detailForm.descriptionEn = row.description || ''
  detailForm.cancellationPolicy = row.cancellationPolicy || ''
  detailForm.cancellationPolicyEn = row.cancellationPolicy || ''
  detailForm.includeMeal = row.includeMeal
  currentDetailPlanId.value = row.id

  detailDialogVisible.value = true
}

const handleDelete = async (row: PricePlan) => {
  if (!userStore.currentUser?.id) return

  try {
    await ElMessageBox.confirm(
      t('settingsStage4.pricePlan.messages.deletePlanConfirm', { name: row.name }),
      t('settingsStage4.pricePlan.messages.deletePlanTitle'),
      {
        confirmButtonText: t('settings.common.confirm'),
        cancelButtonText: t('settings.common.cancel'),
        type: 'warning',
      },
    )

    const resp: any = await deletePricePlan(row.id, userStore.currentUser.id)
    if (!resp?.success) {
      throw new Error(resp?.message || t('settingsStage4.pricePlan.messages.deletePlanFailed'))
    }
    ElMessage.success(resp?.message || t('settingsStage4.pricePlan.messages.deletePlanSuccess'))
    await loadPricePlans()
  } catch (error: any) {
    if (error !== 'cancel') {
      const msg = error?.message || t('settingsStage4.pricePlan.messages.deletePlanFailed')

      // 最小改动：仅在“渠道价格记录”阻塞时，引导用户走“彻底删除（只清理 channel_prices）”
      if (typeof msg === 'string' && msg.includes('渠道价格记录')) {
        try {
          await ElMessageBox.confirm(
            t('settingsStage4.pricePlan.messages.forceDeleteConfirm'),
            t('settingsStage4.pricePlan.messages.forceDeleteTitle'),
            {
              confirmButtonText: t('settingsStage4.pricePlan.actions.forceDelete'),
              cancelButtonText: t('settings.common.cancel'),
              type: 'warning',
            },
          )

          const forceResp: any = await forceDeletePricePlan(row.id, userStore.currentUser.id)
          if (!forceResp?.success) {
            throw new Error(
              forceResp?.message || t('settingsStage4.pricePlan.messages.forceDeleteFailed'),
            )
          }
          ElMessage.success(
            forceResp?.message || t('settingsStage4.pricePlan.messages.forceDeleteSuccess'),
          )
          await loadPricePlans()
          return
        } catch (forceError: any) {
          if (forceError !== 'cancel') {
            ElMessage.error(
              forceError?.message || t('settingsStage4.pricePlan.messages.forceDeleteFailed'),
            )
          }
          return
        }
      }
      ElMessage.error(error?.message || t('settingsStage4.pricePlan.messages.deletePlanFailed'))
      console.error('删除价格计划失败:', error)
    }
  }
}

const handleCancelEdit = () => {
  editDialogVisible.value = false
  editFormRef.value?.resetFields()
}

const handleConfirmEdit = async () => {
  if (!editFormRef.value || !userStore.currentUser?.id) return

  await editFormRef.value.validate(async (valid) => {
    if (valid) {
      try {
        if (currentEditId.value) {
          // 更新价格计划
          const resp: any = await updatePricePlan(currentEditId.value, userStore.currentUser!.id, {
            name: editForm.name,
            minNights: editForm.minNights,
            maxNights: editForm.maxNights,
            includeMeal: editForm.includeMeal,
            derivationType: editForm.derivationType,
          })
          if (!resp?.success) {
            throw new Error(
              resp?.message || t('settingsStage4.pricePlan.messages.updatePlanFailed'),
            )
          }
          ElMessage.success(t('settingsStage4.pricePlan.messages.updatePlanSuccess'))
        } else {
          // 创建价格计划
          const resp: any = await createPricePlan(userStore.currentUser!.id, {
            name: editForm.name,
            minNights: editForm.minNights,
            maxNights: editForm.maxNights,
            includeMeal: editForm.includeMeal,
            derivationType: editForm.derivationType,
          })
          if (!resp?.success) {
            throw new Error(
              resp?.message || t('settingsStage4.pricePlan.messages.createPlanFailed'),
            )
          }
          ElMessage.success(t('settingsStage4.pricePlan.messages.createPlanSuccess'))
        }

        // 重新加载数据
        await loadPricePlans()
        editDialogVisible.value = false
        editFormRef.value?.resetFields()
      } catch (error) {
        ElMessage.error(t('settingsStage4.storeBasic.messages.saveFailed'))
        console.error('保存价格计划失败:', error)
      }
    }
  })
}

const handleCancelDetail = () => {
  detailDialogVisible.value = false
  detailFormRef.value?.resetFields()
}

const handleConfirmDetail = async () => {
  if (!detailFormRef.value || !userStore.currentUser?.id || !currentDetailPlanId.value) return

  detailFormRef.value?.validate(async (valid) => {
    if (valid) {
      try {
        const resp: any = await updatePricePlan(
          currentDetailPlanId.value!,
          userStore.currentUser!.id,
          {
            name: detailForm.name,
            nameEn: detailForm.nameEn,
            description: detailForm.description,
            descriptionEn: detailForm.descriptionEn,
            cancellationPolicy: detailForm.cancellationPolicy,
            cancellationPolicyEn: detailForm.cancellationPolicyEn,
            includeMeal: detailForm.includeMeal,
            minNights: 1, // 使用默认值,因为详情表单中没有这些字段
            maxNights: 365,
          },
        )

        ElMessage.success(t('settingsStage4.pricePlan.messages.detailSaveSuccess'))
        await loadPricePlans()
        detailDialogVisible.value = false
      } catch (error) {
        ElMessage.error(t('settingsStage4.pricePlan.messages.detailSaveFailed'))
        console.error('保存详情失败:', error)
      }
    }
  })
}

const handleAssignPricePlan = (roomType: RoomType) => {
  currentAssignRoomType.value = roomType
  selectedAssignPricePlanId.value = null
  assignPricePlanFilterKeyword.value = ''
  assignPricePlanDialogVisible.value = true
}

const handleCancelAssignPricePlan = () => {
  assignPricePlanDialogVisible.value = false
  currentAssignRoomType.value = null
  selectedAssignPricePlanId.value = null
}

const handleConfirmAssignPricePlan = async () => {
  if (
    !selectedAssignPricePlanId.value ||
    !currentAssignRoomType.value ||
    !userStore.currentUser?.id
  ) {
    ElMessage.warning(t('settingsStage4.pricePlan.messages.selectPricePlan'))
    return
  }

  try {
    // 调用API分配价格计划到房型
    await assignPricePlanToRoomType(
      currentAssignRoomType.value.id,
      selectedAssignPricePlanId.value,
      userStore.currentUser.id,
      {
        maxGuests: 2, // 默认最大2人
        priceMode: 'unified', // 默认统一价
      },
    )

    ElMessage.success(t('settingsStage4.pricePlan.messages.assignPlanSuccess'))
    assignPricePlanDialogVisible.value = false

    // 重新加载房型价格数据
    await loadRoomTypePrices()
  } catch (error) {
    ElMessage.error(t('settingsStage4.pricePlan.messages.assignPlanFailed'))
    console.error('分配价格计划失败:', error)
  }
}

// 应用房型相关函数
const handleShowAppliedRoomTypes = async (plan: PricePlan) => {
  currentPricePlan.value = plan
  selectedPricePlanId.value = plan.id

  try {
    // 从后端获取该价格计划已关联的房型
    const response = (await getRoomTypesByPricePlan(plan.id)) as any
    const roomTypePricePlans: RoomTypePricePlanDTO[] = response.data || []

    appliedRoomTypes.value = roomTypePricePlans.map((rtp: RoomTypePricePlanDTO) => ({
      id: rtp.roomType!.id,
      name: rtp.roomType!.name,
      code: rtp.roomType!.nameEn || '',
      pricePlans: [],
      roomTypePricePlanId: rtp.id,
    }))

    appliedRoomTypesDialogVisible.value = true
  } catch (error) {
    ElMessage.error(t('settingsStage4.pricePlan.messages.loadLinkedRoomTypesFailed'))
    console.error('加载关联房型失败:', error)
  }
}

const confirmRemoveRelationOptions = async (roomTypeName: string, planName: string) => {
  try {
    await ElMessageBox.confirm(
      t('settingsStage4.pricePlan.messages.removeRelationConfirm', { roomTypeName, planName }),
      t('settingsStage4.pricePlan.messages.removeRelationTitle'),
      {
        confirmButtonText: t('settingsStage4.pricePlan.actions.unlinkAndClear'),
        cancelButtonText: t('settingsStage4.pricePlan.actions.unlinkOnly'),
        distinguishCancelAndClose: true,
        closeOnClickModal: false,
        closeOnPressEscape: false,
        type: 'warning',
      },
    )
    return true
  } catch (action: any) {
    if (action === 'cancel') {
      return false
    }
    return null
  }
}

const handleRemoveRoomType = async (roomType: RoomType) => {
  if (!userStore.currentUser?.id || !selectedPricePlanId.value) return

  const planName = currentPricePlan.value?.name || t('settingsStage4.pricePlan.currentPlan')
  const clearOverrides = await confirmRemoveRelationOptions(roomType.name, planName)
  if (clearOverrides === null) {
    return
  }

  try {
    let relationId = roomType.roomTypePricePlanId

    // 兜底：如果前端没拿到关联ID，则从后端再查一次
    if (!relationId) {
      const response = (await getRoomTypesByPricePlan(selectedPricePlanId.value)) as any
      const roomTypePricePlans: RoomTypePricePlanDTO[] = response.data || []
      const matched = roomTypePricePlans.find(
        (rtp) => rtp.roomType?.id === roomType.id && typeof rtp.id === 'number',
      )
      relationId = matched?.id
    }

    if (!relationId) {
      ElMessage.error(t('settingsStage4.pricePlan.messages.relationIdMissing'))
      return
    }

    const deleteResp: any = await deleteRoomTypePricePlan(
      relationId,
      userStore.currentUser.id,
      clearOverrides,
    )
    if (!deleteResp?.success) {
      throw new Error(
        deleteResp?.message || t('settingsStage4.pricePlan.messages.deleteRelationFailed'),
      )
    }

    appliedRoomTypes.value = appliedRoomTypes.value.filter((rt) => rt.id !== roomType.id)
    ElMessage.success(
      deleteResp?.message ||
        (clearOverrides
          ? t('settingsStage4.pricePlan.messages.removedRoomTypeAndCleared', {
              name: `${roomType.name} ${roomType.code}`,
            })
          : t('settingsStage4.pricePlan.messages.removedRoomTypeOnly', {
              name: `${roomType.name} ${roomType.code}`,
            })),
    )

    await loadPricePlans()
    await loadRoomTypePrices()
    if (currentPricePlan.value) {
      await handleShowAppliedRoomTypes(currentPricePlan.value)
    }
  } catch (error) {
    ElMessage.error(
      (error as any)?.message || t('settingsStage4.pricePlan.messages.removeRoomTypeFailed'),
    )
    console.error('移除房型失败:', error)
  }
}

const handleShowLinkRoomTypes = () => {
  // 初始化可用房型列表(排除已关联的)
  const appliedIds = appliedRoomTypes.value.map((rt) => rt.id)
  filteredAvailableRoomTypes.value = allRoomTypes.value
    .filter((rt) => !appliedIds.includes(rt.id!))
    .map((rt) => ({
      id: rt.id!,
      name: rt.name,
      code: rt.code || '',
      pricePlans: [],
    }))

  selectedRoomTypeIds.value = []
  roomTypeFilterKeyword.value = ''

  linkRoomTypesDialogVisible.value = true
}

const handleRoomTypeSelectVisible = (visible: boolean) => {
  if (!visible) {
    roomTypeFilterKeyword.value = ''
  }
}

const handleRoomTypeSelectionChange = (values: RoomTypeSelectValue[]) => {
  const roomTypeIds = filteredAvailableRoomTypes.value.map((roomType) => roomType.id)
  if (values.includes('all')) {
    const hasSelectedAll = roomTypeIds.length > 0 && roomTypeIds.every((id) => values.includes(id))
    selectedRoomTypeIds.value = hasSelectedAll ? [] : roomTypeIds
    return
  }
  selectedRoomTypeIds.value = values.filter((value): value is number => typeof value === 'number')
}

const handleFilterRoomTypes = () => {
  const appliedIds = appliedRoomTypes.value.map((rt) => rt.id)
  const keyword = roomTypeFilterKeyword.value.toLowerCase()

  filteredAvailableRoomTypes.value = allRoomTypes.value
    .filter((rt) => !appliedIds.includes(rt.id!))
    .filter((rt) => {
      if (!keyword) return true
      return (
        rt.name.toLowerCase().includes(keyword) ||
        (rt.code && rt.code.toLowerCase().includes(keyword))
      )
    })
    .map((rt) => ({
      id: rt.id!,
      name: rt.name,
      code: rt.code || '',
      pricePlans: [],
    }))
}

const handleCancelLinkRoomTypes = () => {
  linkRoomTypesDialogVisible.value = false
  selectedRoomTypeIds.value = []
  roomTypeFilterKeyword.value = ''
  linkRoomTypePrices.value = {}
  quickFillPrices.value = {}
}

const handleConfirmLinkRoomTypes = async () => {
  if (selectedRoomTypeIds.value.length === 0) {
    ElMessage.warning(t('settingsStage4.pricePlan.messages.selectRoomTypesToLink'))
    return
  }

  if (!userStore.currentUser?.id || !selectedPricePlanId.value) return

  // 验证所有房型都已填写价格
  const numericIds = selectedRoomTypeIds.value.filter((id) => typeof id === 'number') as number[]
  for (const roomTypeId of numericIds) {
    const prices = linkRoomTypePrices.value[roomTypeId]
    if (!prices) {
      ElMessage.warning(t('settingsStage4.pricePlan.messages.fillAllRoomTypePrices'))
      return
    }
    // 检查是否至少有一个价格大于0
    const hasPrice = Object.values(prices).some((p) => p > 0)
    if (!hasPrice) {
      ElMessage.warning(t('settingsStage4.pricePlan.messages.fillAtLeastOneDayPrice'))
      return
    }
  }

  try {
    // 为每个选中的房型分配价格计划
    await Promise.all(
      numericIds.map((roomTypeId) => {
        const prices = linkRoomTypePrices.value[roomTypeId]
        return assignPricePlanToRoomType(
          roomTypeId,
          selectedPricePlanId.value!,
          userStore.currentUser!.id,
          {
            mondayPrice: prices.mon,
            tuesdayPrice: prices.tue,
            wednesdayPrice: prices.wed,
            thursdayPrice: prices.thu,
            fridayPrice: prices.fri,
            saturdayPrice: prices.sat,
            sundayPrice: prices.sun,
            maxGuests: 4,
            priceMode: 'unified',
          },
        )
      }),
    )

    ElMessage.success(
      t('settingsStage4.pricePlan.messages.linkRoomTypesSuccess', { count: numericIds.length }),
    )

    // 重新加载应用的房型列表
    if (currentPricePlan.value) {
      await handleShowAppliedRoomTypes(currentPricePlan.value)
    }

    linkRoomTypesDialogVisible.value = false
    selectedRoomTypeIds.value = []
    linkRoomTypePrices.value = {}
    quickFillPrices.value = {}

    // 重新加载价格计划统计
    await loadPricePlans()
  } catch (error) {
    ElMessage.error(t('settingsStage4.pricePlan.messages.linkRoomTypesFailed'))
    console.error('关联房型失败:', error)
  }
}

const handleEditRate = (roomType: RoomType, plan: RoomTypePricePlan) => {
  // 填充数据
  currentEditRate.value = {
    roomType,
    plan,
    planName: plan.name,
  }

  // 填充每日价格
  dailyPrices.mon = plan.prices.mon
  dailyPrices.tue = plan.prices.tue
  dailyPrices.wed = plan.prices.wed
  dailyPrices.thu = plan.prices.thu
  dailyPrices.fri = plan.prices.fri
  dailyPrices.sat = plan.prices.sat
  dailyPrices.sun = plan.prices.sun

  // 重置其他字段
  quickFillPrice.value = 0
  priceOption.value = plan.priceMode === 'multiple' ? 'multiple' : 'unified'
  includedGuestsCount.value = String(plan.includedGuestsValue ?? 2)
  extraAdultRate.value = plan.extraAdultRate ?? 0
  extraChildRate.value = plan.extraChildRate ?? 0
  clearFutureOverridesOnSave.value = true

  editRateDialogVisible.value = true
}

const handleDeleteRate = async (roomType: RoomType, plan: RoomTypePricePlan) => {
  if (!userStore.currentUser?.id) return

  const clearOverrides = await confirmRemoveRelationOptions(roomType.name, plan.name)
  if (clearOverrides === null) {
    return
  }

  try {
    const deleteResp: any = await deleteRoomTypePricePlan(
      plan.id,
      userStore.currentUser!.id,
      clearOverrides,
    )
    if (!deleteResp?.success) {
      throw new Error(
        deleteResp?.message || t('settingsStage4.pricePlan.messages.deleteRelationFailed'),
      )
    }

    ElMessage.success(
      deleteResp?.message ||
        (clearOverrides
          ? t('settingsStage4.pricePlan.messages.deletedRateAndCleared', {
              roomType: roomType.name,
              plan: plan.name,
            })
          : t('settingsStage4.pricePlan.messages.deletedRateOnly', {
              roomType: roomType.name,
              plan: plan.name,
            })),
    )
    await loadRoomTypePrices()
  } catch (error) {
    ElMessage.error(
      (error as any)?.message || t('settingsStage4.pricePlan.messages.deleteRateFailed'),
    )
    console.error('删除房价失败:', error)
  }
}

const handleCancelEditRate = () => {
  editRateDialogVisible.value = false
  currentEditRate.value = null
}

const handleConfirmEditRate = async () => {
  if (!currentEditRate.value || !userStore.currentUser?.id) return

  try {
    const { plan } = currentEditRate.value

    // 构建基础数据
    const updateData: any = {
      mondayPrice: dailyPrices.mon,
      tuesdayPrice: dailyPrices.tue,
      wednesdayPrice: dailyPrices.wed,
      thursdayPrice: dailyPrices.thu,
      fridayPrice: dailyPrices.fri,
      saturdayPrice: dailyPrices.sat,
      sundayPrice: dailyPrices.sun,
      maxGuests: plan.maxGuests ?? 4,
      priceMode: priceOption.value,
      clearFutureOverrides: clearFutureOverridesOnSave.value,
    }

    if (clearFutureOverridesOnSave.value) {
      updateData.clearFromDate = getStoreTodayYmd()
    }

    if (priceOption.value === 'multiple') {
      const included = Number.parseInt(includedGuestsCount.value)
      updateData.includedGuests = Number.isFinite(included) ? included : 2
      updateData.extraAdultRate = extraAdultRate.value
      updateData.extraChildRate = extraChildRate.value
    } else {
      updateData.includedGuests = null
      updateData.extraAdultRate = 0
      updateData.extraChildRate = 0
    }

    await updateRoomTypePricePlan(plan.id, userStore.currentUser!.id, updateData)

    ElMessage.success(
      clearFutureOverridesOnSave.value
        ? t('settingsStage4.pricePlan.messages.rateUpdateSuccessCleared')
        : t('settingsStage4.pricePlan.messages.rateUpdateSuccess'),
    )
    await loadRoomTypePrices()
    editRateDialogVisible.value = false
    currentEditRate.value = null
  } catch (error) {
    ElMessage.error(t('settingsStage4.pricePlan.messages.updateRateFailed'))
    console.error('更新房价失败:', error)
  }
}

// 监听快速填充价格变化，统一修改所有日期的价格
watch(quickFillPrice, (newPrice) => {
  if (newPrice > 0) {
    dailyPrices.mon = newPrice
    dailyPrices.tue = newPrice
    dailyPrices.wed = newPrice
    dailyPrices.thu = newPrice
    dailyPrices.fri = newPrice
    dailyPrices.sat = newPrice
    dailyPrices.sun = newPrice
  }
})

// 监听选中房型变化,初始化价格数据
watch(selectedRoomTypeIds, (newIds) => {
  const numericIds = newIds.filter((id) => typeof id === 'number') as number[]

  // 为新增的房型初始化价格数据
  numericIds.forEach((roomTypeId) => {
    if (!linkRoomTypePrices.value[roomTypeId]) {
      linkRoomTypePrices.value[roomTypeId] = {
        mon: 0,
        tue: 0,
        wed: 0,
        thu: 0,
        fri: 0,
        sat: 0,
        sun: 0,
      }
    }
    // 初始化快速填充价格
    if (quickFillPrices.value[roomTypeId] === undefined) {
      quickFillPrices.value[roomTypeId] = 0
    }
  })

  // 移除取消选择的房型价格数据
  Object.keys(linkRoomTypePrices.value).forEach((key) => {
    const id = Number(key)
    if (!numericIds.includes(id)) {
      delete linkRoomTypePrices.value[id]
      delete quickFillPrices.value[id]
    }
  })
})
</script>

<style scoped>
.price-plan-container {
  padding: 20px;
  background: #fff;
  min-height: calc(100vh - 100px);
}

.price-tabs {
  margin-bottom: 20px;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

:deep(.el-table) {
  font-size: 14px;
}

:deep(.el-table th) {
  background-color: #f5f7fa;
  color: #606266;
  font-weight: 600;
}

:deep(.el-link) {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.form-item-hint {
  color: #909399;
  font-size: 13px;
  margin-bottom: 12px;
  line-height: 1.5;
}

.extra-rate-row {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}

.extra-rate-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.extra-rate-label {
  color: #606266;
  font-size: 13px;
}

.extra-rate-input {
  width: 220px;
}

.clear-overrides-section {
  margin-top: 16px;
}

:deep(.el-radio) {
  display: flex;
  align-items: flex-start;
  margin-bottom: 12px;
  white-space: normal;
}

:deep(.el-radio__label) {
  white-space: normal;
  line-height: 1.5;
}

:deep(.el-form-item__label) {
  font-weight: 500;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

/* 房费页面样式 */
.room-type-section {
  margin-bottom: 30px;
}

.room-type-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 20px;
  background: #f5f7fa;
  border: 1px solid #dcdfe6;
  border-bottom: none;
  border-radius: 4px 4px 0 0;
}

.room-type-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.room-type-name {
  color: #303133;
}

.room-type-code {
  color: #909399;
  font-weight: 400;
}

.rate-table {
  border-top: none;
  border-radius: 0 0 4px 4px;
}

.plan-name-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.arrow-icon {
  color: #909399;
  font-size: 14px;
}

:deep(.rate-table .el-table__header th) {
  background-color: #fafafa;
}

/* 编辑房价对话框样式 */
.edit-rate-content {
  padding: 10px 0;
}

.rate-plan-name {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 24px;
}

.quick-fill-section {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
  padding: 16px;
  background: #f5f7fa;
  border-radius: 4px;
}

.section-label {
  font-size: 14px;
  font-weight: 500;
  color: #606266;
  min-width: 80px;
}

.quick-fill-input {
  width: 200px;
}

.daily-prices-section {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.daily-price-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.day-label {
  font-size: 14px;
  color: #606266;
  font-weight: 500;
}

.price-input {
  width: 100%;
}

.price-options-section {
  margin-bottom: 24px;
}

.price-option-buttons {
  display: flex;
  gap: 12px;
  margin-top: 12px;
}

.option-button {
  flex: 1;
  height: 80px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  position: relative;
}

.help-icon {
  position: absolute;
  top: 8px;
  right: 8px;
  font-size: 16px;
  color: #909399;
  cursor: help;
}

.included-guests-section {
  padding: 16px;
  background: #f5f7fa;
  border-radius: 4px;
}

:deep(.el-input-number) {
  width: 100%;
}

:deep(.el-input-number .el-input__inner) {
  text-align: left;
  padding-left: 30px;
}

:deep(.el-input-number .el-input__prefix) {
  left: 11px;
}

:deep(.night-count-input .el-input__inner) {
  text-align: left !important;
  padding-left: 12px !important;
}

:deep(.include-meal-item .el-form-item__content) {
  padding-top: 6px;
}

:deep(.include-meal-group) {
  display: flex;
  align-items: center;
  gap: 24px;
}

:deep(.include-meal-group .el-radio) {
  display: inline-flex;
  align-items: center;
  margin-bottom: 0;
}

/* 详情对话框样式 */
.detail-hint {
  padding: 12px 16px;
  background: #e6f7ff;
  border: 1px solid #91d5ff;
  border-radius: 4px;
  color: #0050b3;
  font-size: 14px;
  margin-bottom: 24px;
  line-height: 1.5;
}

.detail-form {
  margin-top: 16px;
}

.form-item-label {
  font-size: 12px;
  color: #909399;
  margin-bottom: 8px;
}

.localize-btn {
  margin-top: 8px;
  padding: 0;
}

.localize-btn .el-icon {
  margin-right: 4px;
}

:deep(.detail-form .el-form-item__label) {
  font-weight: 500;
}

:deep(.detail-form .el-form-item) {
  margin-bottom: 24px;
}

/* 应用房型对话框样式 */
.applied-room-types-content {
  padding: 10px 0;
}

.price-plan-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 24px;
}

.room-type-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  min-height: 100px;
}

.room-type-tag {
  font-size: 14px;
}

/* 关联房型对话框样式 */
.link-room-types-content {
  padding: 10px 0;
}

.selection-row {
  display: flex;
  align-items: center;
  gap: 24px;
}

.selection-item {
  flex: 1;
}

.selection-label {
  font-size: 14px;
  font-weight: 500;
  color: #606266;
  margin-bottom: 12px;
}

.selection-input {
  width: 100%;
}

.link-icon {
  font-size: 20px;
  color: #409eff;
  flex-shrink: 0;
}

.room-type-option {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.option-link-icon {
  font-size: 14px;
  color: #909399;
}

:deep(.el-select-dropdown__header) {
  padding: 8px;
}

/* 日期范围选择区域 */
.date-range-section {
  margin-top: 24px;
  padding-top: 24px;
  border-top: 1px solid #dcdfe6;
}

.date-range-picker {
  width: 100%;
}

/* 默认市价区域样式 */
.default-prices-section {
  margin-top: 24px;
  padding-top: 24px;
  border-top: 1px solid #dcdfe6;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 16px;
}

.room-type-prices {
  margin-bottom: 24px;
  padding: 16px;
  background: #f5f7fa;
  border-radius: 8px;
}

.room-type-prices-header {
  font-size: 14px;
  font-weight: 500;
  color: #606266;
  margin-bottom: 12px;
}

.price-input-section {
  background: #fff;
  padding: 16px;
  border-radius: 4px;
}

/* 快速填充区域 */
.quick-fill-section {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #ebeef5;
}

.section-label {
  font-size: 14px;
  font-weight: 500;
  color: #606266;
  min-width: 70px;
}

.quick-fill-input {
  width: 200px;
}

/* 每日价格网格 - 每行3个 */
.daily-prices-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.price-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.weekday-label {
  font-size: 14px;
  color: #606266;
  min-width: 20px;
  text-align: center;
}

.price-input-number {
  flex: 1;
}

:deep(.price-input-number .el-input__inner) {
  text-align: left;
  padding-left: 30px;
}

:deep(.price-input-number .el-input__prefix) {
  left: 11px;
  color: #909399;
}

:deep(.quick-fill-input .el-input__inner) {
  text-align: left;
  padding-left: 30px;
}

:deep(.quick-fill-input .el-input__prefix) {
  left: 11px;
  color: #909399;
}
</style>
