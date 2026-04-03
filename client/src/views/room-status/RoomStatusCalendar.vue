<template>
  <div class="room-status-calendar">
    <!-- 日历视图 -->
    <div class="calendar-view">
      <!-- 头部工具栏 -->
      <div class="header-toolbar">
        <div class="date-navigation">
          <el-button @click="previousWeek" :icon="ArrowLeft" circle />
          <el-date-picker
            v-model="visibleDateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            :clearable="false"
          />
          <el-button @click="goToToday">今日</el-button>
          <el-button @click="nextWeek" :icon="ArrowRight" circle />
        </div>

        <div class="toolbar-actions">
          <el-autocomplete
            v-model="searchKeyword"
            :fetch-suggestions="querySearchAsync"
            placeholder="房号、手机号、订单号、渠道订单号、房间号、客户"
            :prefix-icon="Search"
            clearable
            style="width: 400px; margin-right: 10px"
            @select="handleSearchSelect"
            @input="handleSearchInput"
            :trigger-on-focus="false"
            highlight-first-item
          >
            <template #default="{ item }">
              <div class="search-suggestion-item">
                <span class="guest-name">{{ item.guestName }}</span>
                <div class="status-tags">
                  <el-tag
                    size="small"
                    :style="{
                      backgroundColor:
                        getChannelByName(item.channelName || '自来客')?.color || '#409EFF',
                      borderColor:
                        getChannelByName(item.channelName || '自来客')?.color || '#409EFF',
                      color: 'white',
                    }"
                    >{{ item.channelName || '自来客' }}</el-tag
                  >
                  <el-tag :type="getStatusTagType(item.status || 'CONFIRMED')" size="small">
                    {{ getReservationStatusText(item.status || 'CONFIRMED') }}
                  </el-tag>
                </div>
              </div>
            </template>
          </el-autocomplete>

          <!-- 批量置脏/净下拉菜单 -->
          <el-dropdown @command="handleBatchCleanCommand">
            <el-button type="primary">
              批量脏/净 <el-icon><ArrowDown /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="clean">批量置净</el-dropdown-item>
                <el-dropdown-item command="dirty">批量置脏</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>

          <!-- 批量开/关房下拉菜单 -->
          <el-dropdown @command="handleBatchRoomCommand">
            <el-button type="primary">
              批量开/关房 <el-icon><ArrowDown /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="open">批量开房</el-dropdown-item>
                <el-dropdown-item command="close">批量关房</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>

          <el-button
            :type="showCellDefaultPrice ? 'primary' : 'default'"
            @click="showCellDefaultPrice = !showCellDefaultPrice"
          >
            {{ showCellDefaultPrice ? '隐藏价格' : '显示价格' }}
          </el-button>
          <el-select
            v-if="showCellDefaultPrice"
            v-model="cellPriceDisplaySource"
            class="cell-price-source-select"
            size="small"
            :loading="loadingCellPricePlanOptions"
            placeholder="请选择"
          >
            <el-option label="默认价格" value="default" />
            <el-option
              v-for="plan in calendarCellPricePlanOptions"
              :key="plan.id"
              :label="plan.name"
              :value="`plan:${plan.id}`"
            />
          </el-select>

        </div>
      </div>  

      <!-- 主要内容区域 -->
      <div class="calendar-content" v-loading="loading">
        <div class="calendar-container">
          <!-- 日期表头 -->
          <div class="date-header">
            <div class="header-cell clickable-header" @click="toggleFilterSidebar">
              筛选 <el-icon><ArrowDown /></el-icon>
            </div>
            <div class="header-cell clickable-header" @click="toggleRoomCollapse">
              {{ isRoomCollapsed ? '展开' : '收起' }}
              <el-icon><component :is="isRoomCollapsed ? ArrowDown : ArrowUp" /></el-icon>
            </div>
            <div
              v-for="date in dateColumns"
              :key="date.date"
              class="date-column"
              :class="{
                weekend: isWeekend(date.date),
                today: isToday(date.date),
              }"
            >
              <div class="date-info">
                <div class="month-day">{{ formatMonthDay(date.date) }}</div>
                <div class="weekday">{{ getWeekday(date.date) }}</div>
              </div>
              <div class="date-time">剩{{ getAvailableRoomsCount(date.date) }}间</div>
            </div>
          </div>

          <!-- 空状态提示 -->
          <div v-if="filteredRooms.length === 0" class="empty-state">
            <div class="empty-icon">
              <svg width="120" height="120" viewBox="0 0 120 120" fill="none" xmlns="http://www.w3.org/2000/svg">
                <rect x="20" y="30" width="80" height="60" rx="8" fill="#E8F4FF"/>
                <rect x="30" y="20" width="60" height="70" rx="6" fill="#B3D9FF"/>
                <circle cx="60" cy="50" r="3" fill="#4A5568"/>
                <circle cx="70" cy="50" r="3" fill="#4A5568"/>
                <path d="M55 65 Q60 70 65 65" stroke="#4A5568" stroke-width="2" stroke-linecap="round" fill="none"/>
              </svg>
            </div>
            <div class="empty-text">暂无可用房型房间</div>
            <el-button type="primary" @click="goToRoomTypeManagement">添加房型</el-button>
          </div>

          <!-- 房间状态网格 -->
          <div v-else class="rooms-grid">
            <div v-for="roomData in filteredRooms" :key="roomData.roomId" class="room-row">
              <!-- 房间信息列 -->
              <div class="room-info-cell clickable-cell">
                <div class="room-type">{{ roomData.roomType }}</div>
              </div>

              <!-- 房间号列 -->
              <div
                class="room-number-cell clickable-cell"
                @mouseenter="onRoomNumberHover($event, roomData)"
                @click="onRoomNumberClick($event, roomData)"
              >
                <!-- 脏房清理图标 -->
                <div
                  v-if="!isRoomCollapsed && getRoomNumberDirtyStatus(roomData.roomId)"
                  class="room-dirty-icon"
                >
                  <el-icon><Tools /></el-icon>
                </div>

                <!-- 房间号显示 -->
              <div
                class="room-number"
                :class="{ 'with-dirty-icon': !isRoomCollapsed && getRoomNumberDirtyStatus(roomData.roomId) }"
              >
                  {{ isRoomCollapsed ? '剩余' : roomData.roomNumber }}
                </div>
              </div>

              <!-- 日期状态格子 -->
              <div
                v-for="dailyStatus in roomData.dailyStatus"
                :key="dailyStatus.date"
                class="status-cell"
                :class="[
                  `status-${dailyStatus.status.toLowerCase()}`,
                  {
                    'batch-selected': isCellSelected(roomData.roomId, dailyStatus.date),
                    'multi-selection-start-cell': isSelectedStartCell(roomData.roomId, dailyStatus.date),
                    'multi-selection-middle-cell': isSelectedMiddleCell(roomData.roomId, dailyStatus.date),
                    'multi-selection-end-cell': isSelectedEndCell(roomData.roomId, dailyStatus.date),
                    'has-reservation': !!dailyStatus.reservation && !isRoomCollapsed,
                    'reservation-start-cell': isReservationStartCell(dailyStatus),
                    'reservation-middle-cell': isReservationMiddleCell(dailyStatus),
                    'reservation-end-cell': isReservationEndCell(dailyStatus),
                    'room-change-drop-target': isRoomChangeDropTarget(roomData.roomId, dailyStatus.date),
                  },
                ]"
                @mousedown.left.prevent="onCellMouseDown($event, roomData, dailyStatus)"
                @mouseup.left="onCellMouseUp($event, roomData, dailyStatus)"
                @click="onCellClick($event, roomData, dailyStatus)"
                @mouseenter="onCellMouseEnter($event, roomData, dailyStatus)"
                @dragover="onStatusCellDragOver($event, roomData, dailyStatus)"
                @drop="onStatusCellDrop($event, roomData, dailyStatus)"
                :title="getCellDefaultPriceTooltip(roomData, dailyStatus)"
              >
                <!-- 批量选择标识 -->
                <div
                  v-if="isCellSelected(roomData.roomId, dailyStatus.date)"
                  class="batch-selected-icon"
                >
                  <el-icon><Check /></el-icon>
                </div>

                <!-- 脏房清理图标 -->
                <div
                  v-if="getRoomExtraStatus(roomData.roomId, dailyStatus.date).isDirty && !isRoomCollapsed"
                  class="dirty-room-icon"
                >
                  <el-icon><Tools /></el-icon>
                </div>

                <!-- 停用房图标 -->
                <div
                  v-if="getRoomExtraStatus(roomData.roomId, dailyStatus.date).isClosed && !isRoomCollapsed"
                  class="closed-room-icon"
                >
                  <el-icon><Remove /></el-icon>
                </div>

                <div
                  v-if="hasReservationNotes(dailyStatus)"
                  class="reservation-note-indicator"
                  title="该订单有备注"
                />

                <div
                  v-if="shouldDisplayCellDefaultPrice(roomData, dailyStatus)"
                  class="cell-default-price"
                >
                  ¥{{ getCellDisplayPriceText(roomData, dailyStatus) }}
                </div>

                <div v-if="dailyStatus.reservation && !isRoomCollapsed" class="reservation-cell-info">
                  <div
                    class="reservation-ribbon"
                    :class="{ 'reservation-draggable': canDragReservationAtCell(dailyStatus) }"
                    :draggable="canDragReservationAtCell(dailyStatus)"
                    @mousedown.stop
                    @dragstart="onReservationDragStart($event, roomData, dailyStatus)"
                    @dragend="onReservationDragEnd"
                  >
                    <template v-if="isReservationStartCell(dailyStatus)">
                      <div class="reservation-guest-name">{{ dailyStatus.reservation.guestName }}</div>
                      <div
                        class="reservation-channel-badge"
                        :style="{
                          backgroundColor:
                            getChannelByName(dailyStatus.reservation.channel)?.color || '#409EFF',
                          color: 'white',
                        }"
                      >
                        {{ dailyStatus.reservation.channel }}
                      </div>
                    </template>
                  </div>
                </div>
                <div v-else class="empty-cell">
                  <template v-if="isRoomCollapsed">
                    <span
                      class="collapsed-status-text"
                      :class="{
                        full: getCollapsedCellStatus(roomData, dailyStatus).isFull,
                      }"
                    >
                      {{ getCollapsedCellStatus(roomData, dailyStatus).label }}
                    </span>
                  </template>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 快速操作菜单 -->
      <div
        v-show="showQuickActions"
        ref="quickActionsPopupRef"
        class="quick-actions-popup"
        :style="{
          left: quickActionPosition.x + 'px',
          top: quickActionPosition.y + 'px',
          transform: 'translateX(-50%)',
        }"
        @click.stop
      >
        <div class="popup-content">
          <!-- 选中指示器 -->
          <div class="selected-indicator">
            <el-icon class="check-icon"><Check /></el-icon>
          </div>

          <!-- 房间信息 -->
          <div class="room-info-title">
            {{ quickActionRoom?.roomType }}-{{ quickActionRoom?.roomNumber }}
          </div>

          <!-- 操作按钮 -->
          <div class="popup-actions">
            <!-- 今天显示两个主要按钮：预订和入住 -->
            <template v-if="isToday(quickActionDate)">
              <div class="today-primary-actions">
                <el-button
                  type="primary"
                  class="today-action-btn"
                  @click="handleQuickAction('book')"
                >
                  预订
                </el-button>

                <el-button
                  type="primary"
                  class="today-action-btn"
                  @click="handleQuickAction('check-in')"
                >
                  入住
                </el-button>
              </div>
            </template>

            <!-- 其他日期显示原来的逻辑 -->
            <template v-else>
              <el-button
                :type="getOrderButtonType(quickActionDate)"
                class="primary-action-btn"
                @click="handleQuickAction('book')"
              >
                {{ getOrderTypeText(quickActionDate) }}
              </el-button>
            </template>

            <div class="secondary-actions">
              <el-button class="secondary-action-btn" @click="handleQuickAction('close')">
                关房
              </el-button>
              <el-button class="secondary-action-btn" @click="handleQuickAction('cancel')">
                取消
              </el-button>
            </div>
          </div>
        </div>
      </div>

      <!-- 停用房操作弹窗 -->
      <div
        v-show="showClosedRoomActions"
        class="closed-room-actions-popup"
        :style="{
          left: closedRoomActionPosition.x + 'px',
          top: closedRoomActionPosition.y + 'px',
          transform: 'translateX(-50%)',
        }"
        @click.stop
      >
        <div class="popup-content">
          <!-- 停用标识 -->
          <div class="closed-indicator">
            <el-icon class="closed-icon"><Remove /></el-icon>
            <span class="status-text">停用</span>
          </div>

          <!-- 房间信息 -->
          <div class="room-info-title">
            {{ closedRoomActionData.room?.roomType }}-{{ closedRoomActionData.room?.roomNumber }}
          </div>

          <!-- 操作按钮 -->
          <div class="popup-actions">
            <el-button
              type="primary"
              class="primary-action-btn"
              @click="handleClosedRoomAction('open')"
            >
              开房
            </el-button>

            <div class="secondary-actions">
              <el-button class="secondary-action-btn" @click="handleClosedRoomAction('reserve')">
                转预订
              </el-button>
              <el-button class="secondary-action-btn" @click="handleClosedRoomAction('cancel')">
                取消
              </el-button>
            </div>
          </div>
        </div>
      </div>

      <!-- 悬停脏房提示 -->
      <div
        v-show="showDirtyHover"
        class="dirty-hover-tooltip"
        :style="{
          left: hoverCardPosition.x + 'px',
          top: hoverCardPosition.y + 'px',
        }"
      >
        {{ dirtyHoverActionText }}
      </div>

      <!-- 悬停预订信息卡片 -->
      <div
        v-show="showHoverCard && hoverReservation"
        ref="hoverCardRef"
        class="hover-info-card"
        :style="{
          left: hoverCardPosition.x + 'px',
          top: hoverCardPosition.y + 'px',
        }"
      >
        <div class="card-header">
          <div class="guest-info">
            <el-icon><User /></el-icon>
            {{ hoverReservation?.guestName }}{{ hoverReservation?.phone ? ' ' + hoverReservation.phone : '' }}
          </div>
          <div class="card-actions">
            <!-- 根据预订状态显示不同的按钮 -->
            <template v-if="isConfirmedStatus(hoverReservation?.status)">
              <el-button type="warning" size="small"> 已预订 </el-button>
            </template>

            <template v-else-if="isCheckedInStatus(hoverReservation?.status)">
              <el-button type="primary" size="small"> 已入住 </el-button>
            </template>

            <template v-else-if="isCheckedOutStatus(hoverReservation?.status)">
              <el-button type="info" size="small" disabled> 已退房 </el-button>
            </template>

            <template v-else-if="isCancelledStatus(hoverReservation?.status)">
              <el-button type="danger" size="small" disabled> 已取消 </el-button>
            </template>

            <template v-else-if="isNoShowStatus(hoverReservation?.status)">
              <el-button type="warning" size="small"> 未到店 </el-button>
            </template>

            <!-- 默认情况 -->
            <template v-else>
              <el-button type="primary" size="small">
                {{ getReservationStatusText(hoverReservation?.status) }}
              </el-button>
            </template>

            <el-button size="small" @click="hideHoverCard">关闭</el-button>
          </div>
        </div>

        <div class="stay-info">
          <el-icon><Calendar /></el-icon>
          {{ hoverReservation?.checkInDate }}入住 | {{ hoverReservation?.checkOutDate }}离店 | 共{{ getReservationStayNightsText(hoverReservation) }}
        </div>

        <div class="channel-info">
          <el-icon><Shop /></el-icon>
          • {{ hoverReservation?.channelName || hoverReservation?.channel }}
        </div>

        <!-- 状态信息 -->
        <div class="status-info" v-if="hoverReservation?.status">
          <span class="status-label">状态：</span>
          <el-tag :type="getStatusTagType(hoverReservation.status)" size="small">
            {{ getReservationStatusText(hoverReservation.status) }}
          </el-tag>
        </div>

        <div class="price-info">
          <span>订单总额: ¥{{ hoverReservation?.totalAmount || '0.00' }}</span>
          <span class="received">已收款: ¥{{ totalPayment.toFixed(2) }}</span>
        </div>

        <div class="notes">
          <div class="notes-label">备注：</div>
          <div v-if="getReservationNotesText(hoverReservation)" class="notes-content">
            {{ getReservationNotesText(hoverReservation) }}
          </div>
          <div v-else class="notes-content notes-content-empty">无</div>
        </div>
      </div>

      <!-- 筛选侧边栏 -->
      <el-drawer v-model="showFilterSidebar" title="房型筛选" direction="rtl" size="320px">
        <div class="filter-content">
          <div class="filter-section">
            <div class="section-header">
              <span>全选</span>
              <el-checkbox
                :model-value="
                  filterOptions.selectedRoomTypes.length === filterOptions.roomTypes.length
                "
                :indeterminate="
                  filterOptions.selectedRoomTypes.length > 0 &&
                  filterOptions.selectedRoomTypes.length < filterOptions.roomTypes.length
                "
                @change="handleSelectAll"
              />
            </div>

            <div class="room-type-list">
              <div
                v-for="roomType in filterOptions.roomTypes"
                :key="roomType"
                class="room-type-item"
              >
                <el-checkbox
                  v-model="filterOptions.selectedRoomTypes"
                  :label="roomType"
                  :value="roomType"
                >
                  {{ roomType }}
                </el-checkbox>
              </div>

              
            </div>
          </div>

          <div class="filter-section" v-if="filterOptions.roomGroups.length > 0">
            <div class="section-header">
              <span>分组筛选</span>
            </div>

            <div class="room-type-list">
              <div
                v-for="group in filterOptions.roomGroups"
                :key="group.id"
                class="room-type-item"
              >
                <el-checkbox
                  v-model="filterOptions.selectedRoomGroupIds"
                  :label="group.id"
                  :value="group.id"
                >
                  {{ group.name }}
                </el-checkbox>
              </div>
            </div>
          </div>

          <div class="filter-actions">
            <el-button @click="resetFilters">重置</el-button>
            <el-button type="primary" @click="applyFilters">确定</el-button>
          </div>
        </div>
      </el-drawer>

      <!-- 预订/修改订单/直接入住通用侧边栏 -->
      <el-drawer
        v-model="showBookingSidebar"
        :title="bookingMode === 'create' ? '全日房' : bookingMode === 'check-in' ? '直接入住' : '修改订单'"
        direction="rtl"
        size="700px"
        :show-close="false"
      >
        <div class="booking-container">
          <div class="booking-content">
            <!-- 基本信息 -->
            <div class="booking-section">
              <h3>基本信息</h3>
              <el-form :model="bookingForm" label-width="80px">
                <el-form-item label="姓名">
                  <el-input v-model="bookingForm.guestName" placeholder="请输入姓名" />
                </el-form-item>
                <el-form-item label="手机">
                  <el-input v-model="bookingForm.guestPhone" placeholder="请输入手机号" />
                </el-form-item>
                <el-form-item label="渠道">
                  <el-select
                    v-model="bookingForm.channelId"
                    placeholder="请选择渠道"
                    style="width: 100%"
                  >
                    <template #prefix v-if="selectedChannel">
                      <div
                        style="width: 12px; height: 12px; border-radius: 2px; margin-right: 4px"
                        :style="{ backgroundColor: selectedChannel.color }"
                      ></div>
                    </template>
                    <el-option
                      v-for="channel in channelOptions"
                      :key="channel.id"
                      :label="channel.name"
                      :value="channel.id"
                    >
                      <div style="display: flex; align-items: center; gap: 8px">
                        <div
                          style="width: 12px; height: 12px; border-radius: 2px"
                          :style="{ backgroundColor: channel.color }"
                        ></div>
                        <span>{{ channel.name }}</span>
                        <el-tag size="small" type="info" style="margin-left: auto; font-size: 10px">
                          {{
                            channel.type === 'OTA'
                              ? '在线旅行社'
                              : channel.type === 'DIRECT'
                                ? '直销'
                                : channel.type === 'TRAVEL_AGENCY'
                                  ? '旅行社'
                                  : '企业客户'
                          }}
                        </el-tag>
                      </div>
                    </el-option>
                  </el-select>
                </el-form-item>
              </el-form>
            </div>

            <!-- 房间信息 -->
            <div class="booking-section">
              <h3>房间信息</h3>

              <div class="room-selection">
                <div class="date-range">
                  <el-date-picker
                    v-model="bookingForm.checkInDate"
                    type="date"
                    placeholder="入住日期"
                    format="YYYY-MM-DD"
                    value-format="YYYY-MM-DD"
                    style="width: 48%"
                  />
                  <span>至</span>
                  <el-date-picker
                    v-model="bookingForm.checkOutDate"
                    type="date"
                    placeholder="离店日期"
                    format="YYYY-MM-DD"
                    value-format="YYYY-MM-DD"
                    style="width: 48%"
                  />
                </div>
                <div class="room-details">
                  <span>{{ getRoomDisplayText() }}</span>
                  <span class="price" v-if="isLoadingPrice">
                    <el-icon class="is-loading"><Loading /></el-icon>
                    计算中...
                  </span>
                  <span class="price" v-else>¥ {{ bookingForm.totalAmount.toFixed(2) || '0.00' }}</span>
                  <el-button link>入住</el-button>
                </div>
                <div class="manual-price-row">
                  <el-switch
                    v-model="isManualPrice"
                    active-text="手动改价"
                    inactive-text="自动计算"
                  />
                  <el-select
                    v-if="pricePlanOptions.length > 0"
                    v-model="selectedPricePlanId"
                    placeholder="请选择"
                    style="width: 180px"
                  >
                    <el-option
                      v-for="plan in pricePlanOptions"
                      :key="plan.id"
                      :label="plan.name"
                      :value="plan.id"
                    />
                  </el-select>
                  <el-input-number
                    v-model="bookingForm.totalAmount"
                    :min="0"
                    :precision="2"
                    :step="100"
                    :disabled="!isManualPrice"
                    controls-position="right"
                    style="width: 180px"
                  />
                </div>
              </div>
            </div>

            <!-- 消费信息 -->
            <div class="booking-section">
              <h3>消费信息</h3>
              <div v-for="item in consumptionItems" :key="item.id" class="info-row">
                <el-select
                  v-model="item.type"
                  placeholder="选择消费项目"
                  style="width: 120px"
                  :loading="enabledConsumptionItemsLoading"
                  filterable
                  @change="handleBookingConsumptionItemChange($event, item)"
                >
                  <el-option
                    v-for="option in enabledConsumptionItemOptions"
                    :key="option.value"
                    :label="option.label"
                    :value="option.value"
                  />
                </el-select>
                <el-input-number
                  v-model="item.quantity"
                  :min="1"
                  :max="100"
                  controls-position="right"
                  style="width: 110px"
                />
                <el-input
                  v-model="item.amount"
                  placeholder="金额"
                  type="number"
                  style="width: 100px"
                >
                  <template #prefix>-¥</template>
                </el-input>
                <el-date-picker
                  v-model="item.date"
                  type="date"
                  placeholder="日期"
                  format="MM-DD"
                  value-format="YYYY-MM-DD"
                  style="width: 100px"
                />
                <el-button link class="action-btn" @click="() => {}">备注</el-button>
                <el-button link class="action-btn danger" @click="removeConsumptionItem(item.id)">
                  <el-icon><Delete /></el-icon>
                </el-button>
              </div>
              <el-button link class="add-btn" @click="addConsumptionItem">
                <el-icon><Plus /></el-icon>
                添加消费
              </el-button>
            </div>

            <!-- 收款信息 -->
            <div class="booking-section">
              <h3>收款信息</h3>
              <div v-for="item in paymentItems" :key="item.id" class="info-row">
                <el-select v-model="item.type" placeholder="收款类型" style="width: 120px">
                  <el-option
                    v-for="option in paymentTypeOptions"
                    :key="option.value"
                    :label="option.label"
                    :value="option.value"
                  />
                </el-select>
                <el-select v-model="item.paymentMethod" placeholder="支付方式" style="width: 110px">
                  <el-option
                    v-for="option in paymentMethodOptions"
                    :key="option.value"
                    :label="option.label"
                    :value="option.value"
                  />
                </el-select>
                <el-input
                  v-model="item.amount"
                  placeholder="金额"
                  type="number"
                  style="width: 100px"
                >
                  <template #prefix>¥</template>
                </el-input>
                <el-date-picker
                  v-model="item.date"
                  type="date"
                  placeholder="日期"
                  format="MM-DD"
                  value-format="YYYY-MM-DD"
                  style="width: 100px"
                />
                <el-button link class="action-btn" @click="() => {}">备注</el-button>
                <el-button link class="action-btn danger" @click="removePaymentItem(item.id)">
                  <el-icon><Delete /></el-icon>
                </el-button>
              </div>
              <el-button link class="add-btn" @click="addPaymentItem">
                <el-icon><Plus /></el-icon>
                添加收款
              </el-button>
            </div>

            <!-- 备注信息 -->
            <div class="booking-section">
              <h3>备注信息</h3>
              <el-input
                v-model="bookingForm.notes"
                type="textarea"
                :rows="4"
                placeholder="请输入备注信息"
                maxlength="2048"
                show-word-limit
              />
            </div>
          </div>

          <!-- 底部操作区域 -->
          <div class="booking-footer">
            <div class="total-amount">
              订单金额：
              <span v-if="isLoadingPrice">
                <el-icon class="is-loading"><Loading /></el-icon>
                计算中...
              </span>
              <span v-else>¥{{ bookingForm.totalAmount.toFixed(2) || '0.00' }}</span>
            </div>

            <div class="actions">
              <el-button @click="showBookingSidebar = false">取消</el-button>
              <el-button type="primary" @click="submitBooking">
                {{ bookingMode === 'create' ? '提交订单' : bookingMode === 'check-in' ? '确认入住' : '保存' }}
              </el-button>
            </div>
          </div>
        </div>
      </el-drawer>

      <!-- 预订详情侧边栏 -->
      <el-drawer v-model="showBookingDetailSidebar" direction="rtl" size="1000px">
        <template #header>
          <div class="detail-header">
            <el-tabs v-model="activeDetailTab">
              <el-tab-pane label="详细信息" name="detail" />
              <el-tab-pane label="操作日志" name="log" />
              <el-tab-pane label="渠道信息" name="channel" />
            </el-tabs>
          </div>
        </template>

        <div class="booking-detail-content">
          <div v-if="activeDetailTab === 'detail'">
            <div class="guest-header">
              <div class="guest-main">
                <h3>{{ selectedReservation?.guestName || '客户姓名' }}</h3>
                <el-button
                  type="primary"
                  plain
                  size="small"
                  :disabled="!selectedReservation"
                  @click="goToMessages"
                >
                  去聊天
                </el-button>
              </div>
              <div class="status-tags">
                <el-tag :type="getStatusTagType(selectedReservation?.status || 'CONFIRMED')">
                  {{ getReservationStatusText(selectedReservation?.status || 'CONFIRMED') }}
                </el-tag>
                <el-tag
                  :style="{
                    backgroundColor:
                      getChannelByName(selectedReservation?.channelName || '自来客')?.color ||
                      '#409EFF',
                    borderColor:
                      getChannelByName(selectedReservation?.channelName || '自来客')?.color ||
                      '#409EFF',
                    color: 'white',
                  }"
                  >{{ selectedReservation?.channelName || '自来客' }}</el-tag
                >
              </div>
            </div>

            <div class="order-summary">
              <div class="amounts">
                <div class="amount-item">
                  <span>订单金额</span>
                  <span class="amount">{{ selectedReservation?.totalAmount || '0.00' }}</span>
                </div>
                <div
                  class="amount-item"
                  v-if="
                    selectedReservation?.currentRoomPrice &&
                    selectedReservation?.currentRoomPrice !== selectedReservation?.totalAmount
                  "
                >
                  <span>当前房价</span>
                  <span class="amount current-price">{{
                    selectedReservation?.currentRoomPrice || '0.00'
                  }}</span>
                </div>
                <div class="amount-item">
                  <span>已付金额</span>
                  <span class="amount">{{ totalPayment.toFixed(2) }}</span>
                </div>
                <div class="amount-item">
                  <span>还需付款</span>
                  <span class="amount" :class="{ red: remainingPayment > 0, green: remainingPayment < 0 }">
                    {{ remainingPayment >= 0 ? remainingPayment.toFixed(2) : '+' + Math.abs(remainingPayment).toFixed(2) }}
                  </span>
                </div>
              </div>
            </div>

            <div class="room-info-detail">
              <h4>房间信息：¥{{ selectedReservation?.totalAmount || '0.00' }} 排房</h4>
              <div class="room-card">
                <div class="room-header">
                  <span class="room-name"
                    >{{ selectedReservation?.roomTypeName }}-{{
                      selectedReservation?.roomNumber
                    }}</span
                  >
                  <span class="room-dates"
                    >{{ selectedReservation?.checkInDate }} 至
                    {{ selectedReservation?.checkOutDate }}，{{ getReservationStayNightsText(selectedReservation) }}</span
                  >
                  <div class="room-actions">
                    <el-button
                      v-if="isConfirmedStatus(selectedReservation?.status || '')"
                      size="small"
                      @click="handleCheckIn"
                      >办理入住</el-button
                    >
                    <el-button
                      v-if="isCheckedInStatus(selectedReservation?.status || '')"
                      size="small"
                      type="danger"
                      @click="handleCheckOut"
                      >办理退房</el-button
                    >
                    <span class="room-price">
                      ¥{{ selectedReservation?.totalAmount || '0.00' }}
                      <span
                        v-if="
                          selectedReservation?.currentRoomPrice &&
                          selectedReservation?.currentRoomPrice !== selectedReservation?.totalAmount
                        "
                        class="current-room-price"
                      >
                        (当前: ¥{{ selectedReservation?.currentRoomPrice }})
                      </span>
                    </span>
                    <el-button
                      size="small"
                      :type="getStatusTagType(selectedReservation?.status || 'CONFIRMED')"
                    >
                      {{ getReservationStatusText(selectedReservation?.status || 'CONFIRMED') }}
                    </el-button>
                  </div>
                </div>
              </div>
            </div>

            <div class="expandable-sections">
              <el-collapse v-model="activeCollapsePanels">
                <el-collapse-item name="1">
                  <template #title>
                    <div style="display: flex; justify-content: space-between; align-items: center; width: 100%">
                      <span>其他消费：+¥{{ Math.abs(totalConsumption).toFixed(2) }}</span>
                      <el-button link type="primary" @click.stop="openAddConsumptionSidebar">
                        <el-icon><Plus /></el-icon>
                        添加消费
                      </el-button>
                    </div>
                  </template>
                  <!-- 消费记录表格 -->
                  <el-table
                    v-if="consumptionList.length > 0"
                    :data="consumptionList"
                    border
                    style="width: 100%"
                    :header-cell-style="{ background: '#f5f7fa', color: '#606266' }"
                  >
                    <el-table-column prop="item" label="消费项目" width="120">
                      <template #default="{ row }">
                        {{ row.item }}×{{ row.quantity }}
                      </template>
                    </el-table-column>
                    <el-table-column prop="amount" label="消费金额" width="100" align="right">
                      <template #default="{ row }">
                        {{ row.amount }}
                      </template>
                    </el-table-column>
                    <el-table-column prop="date" label="消费日期" width="120" />
                    <el-table-column prop="createdBy" label="录人人" min-width="120" />
                    <el-table-column label="操作" width="100" align="center">
                      <template #default="{ row }">
                        <el-button link @click="handleDeleteConsumption(row.id)">
                          <el-icon color="#f56c6c"><Delete /></el-icon>
                        </el-button>
                        <el-button link>
                          <el-icon><Right /></el-icon>
                        </el-button>
                      </template>
                    </el-table-column>
                  </el-table>
                </el-collapse-item>
                <el-collapse-item name="2">
                  <template #title>
                    <div style="display: flex; justify-content: space-between; align-items: center; width: 100%">
                      <span>收款金额：¥{{ totalPayment.toFixed(2) }}</span>
                      <el-button link type="primary" @click.stop="openPaymentSidebar">
                        <el-icon><Plus /></el-icon>
                        收款/退款
                      </el-button>
                    </div>
                  </template>
                  <!-- 收款记录表格 -->
                  <el-table
                    v-if="paymentList.length > 0"
                    :data="paymentList"
                    border
                    style="width: 100%"
                    :header-cell-style="{ background: '#f5f7fa', color: '#606266' }"
                  >
                    <el-table-column prop="type" label="项目" width="100" />
                    <el-table-column prop="paymentMethod" label="支付方式" width="140" />
                    <el-table-column prop="amount" label="金额" width="100" align="right">
                      <template #default="{ row }">
                        ¥{{ row.amount }}
                      </template>
                    </el-table-column>
                    <el-table-column label="日期" width="100">
                      <template #default="{ row }">
                        {{ formatShortDate(row.date) }}
                      </template>
                    </el-table-column>
                    <el-table-column label="操作" min-width="100" align="center">
                      <template #default="{ row }">
                        <el-button link type="danger" @click="handleDeletePayment(row.id)">删除</el-button>
                        <el-button link>
                          <el-icon><Right /></el-icon>
                        </el-button>
                      </template>
                    </el-table-column>
                  </el-table>
                </el-collapse-item>
              </el-collapse>
            </div>

            <div class="order-info">
              <p><strong>订单号：</strong>{{ selectedReservation?.orderNumber || '无' }}</p>
              <p><strong>客人手机：</strong>{{ selectedReservation?.phone || '无' }}</p>
              <p><strong>备注：</strong>{{ getReservationNotesText(selectedReservation) || '无' }}</p>
              <div class="order-colors">
                <el-button size="small">订单颜色</el-button>
              </div>
            </div>

            <div class="more-actions">
              <el-dropdown @command="handleMoreActions">
                <el-button link>
                  更多操作 <el-icon><ArrowDown /></el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="moveToOrderBox">移入订单盒子</el-dropdown-item>
                    <el-dropdown-item command="cancelReservation">取消预约</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </div>

          <div v-if="activeDetailTab === 'log'">
            <div class="operation-logs" v-loading="operationLogsLoading">
              <!-- 日志类型筛选按钮 -->
              <div class="log-filter-buttons">
                <el-button
                  :type="logFilterType === 'all' ? 'primary' : 'default'"
                  size="small"
                  @click="logFilterType = 'all'"
                >
                  全部日志
                </el-button>
                <el-button
                  :type="logFilterType === 'order' ? 'primary' : 'default'"
                  size="small"
                  @click="logFilterType = 'order'"
                >
                  订单日志
                </el-button>
                <el-button
                  :type="logFilterType === 'billing' ? 'primary' : 'default'"
                  size="small"
                  @click="logFilterType = 'billing'"
                >
                  账单日志
                </el-button>
                <el-button
                  :type="logFilterType === 'compensation' ? 'primary' : 'default'"
                  size="small"
                  @click="handleOpenCompensationPanel"
                >
                  补偿/重试
                </el-button>
              </div>

              <div v-if="logFilterType === 'compensation'" class="compensation-panel" v-loading="suWebhookEventsLoading">
                <div class="compensation-toolbar">
                  <div class="toolbar-left">
                    <span class="toolbar-label">状态</span>
                    <el-select v-model="suWebhookStatusFilter" style="width: 220px">
                      <el-option
                        v-for="opt in suWebhookStatusOptions"
                        :key="opt.value"
                        :label="opt.label"
                        :value="opt.value"
                      />
                    </el-select>
                  </div>
                  <div class="toolbar-right">
                    <el-button @click="loadSuWebhookEvents">刷新</el-button>
                    <el-button type="primary" :loading="suWebhookEventsProcessing" @click="handleProcessSuWebhookEvents">
                      立即处理
                    </el-button>
                  </div>
                </div>

                <el-table :data="suWebhookEvents" border size="small">
                  <el-table-column prop="reservationNotifId" label="reservation_notif_id" min-width="200" />
                  <el-table-column prop="hotelId" label="hotel_id" min-width="120" />
                  <el-table-column prop="status" label="状态" width="120" align="center" />
                  <el-table-column prop="retryCount" label="重试次数" width="100" align="center" />
                  <el-table-column prop="nextRetryAt" label="下次重试" min-width="160" />
                  <el-table-column prop="updatedAt" label="更新时间" min-width="160" />
                  <el-table-column label="错误" min-width="260">
                    <template #default="{ row }">
                      <el-tooltip v-if="row.lastError" placement="top" :content="row.lastError">
                        <span class="error-ellipsis">{{ row.lastError }}</span>
                      </el-tooltip>
                      <span v-else>-</span>
                    </template>
                  </el-table-column>
                </el-table>

                <el-empty v-if="suWebhookEvents.length === 0" description="暂无补偿事件" />
              </div>

              <template v-else>
                <!-- 操作日志时间线 -->
                <el-timeline class="operation-timeline">
                  <el-timeline-item
                    v-for="log in filteredOperationLogs"
                    :key="log.id"
                    :timestamp="log.timestamp"
                    placement="top"
                  >
                    <div class="log-item">
                      <div class="log-header">
                        <span class="log-action">{{ log.action }}</span>
                        <span class="log-operator">操作人: {{ log.operator }}</span>
                      </div>
                      <div class="log-content" v-if="log.content">
                        {{ log.content }}
                      </div>
                      <div class="log-details" v-if="log.details">
                        <div v-for="(detail, index) in log.details" :key="index" class="detail-item">
                          <span class="detail-label">{{ detail.label }}:</span>
                          <span class="detail-value">{{ detail.value }}</span>
                        </div>
                      </div>
                    </div>
                  </el-timeline-item>
                </el-timeline>
              </template>

              <!-- 空状态 -->
              <el-empty v-if="logFilterType !== 'compensation' && filteredOperationLogs.length === 0" description="暂无操作日志" />
            </div>
          </div>

          <div v-if="activeDetailTab === 'channel'">
            <div class="channel-info-content" v-loading="channelInfoLoading">
              <!-- 渠道Logo和名称 -->
              <div class="channel-header">
                <div class="channel-logo-wrapper">
                  <div class="channel-logo">
                    <span class="logo-text">{{ channelLogoText }}</span>
                  </div>
                  <h3>{{ channelInfo?.channelName || selectedReservation?.channelName || '自来客' }}</h3>
                </div>
              </div>

              <!-- 订单详情 -->
              <div class="channel-section">
                <h4>订单详情</h4>
                <div class="info-grid">
                  <div class="info-item">
                    <span class="label">渠道订单号:</span>
                    <span class="value">{{ channelInfo?.channelOrderNumber || selectedReservation?.orderNumber || '-' }}</span>
                  </div>
                  <div class="info-item">
                    <span class="label">预定状态:</span>
                    <el-tag :type="getStatusTagType(selectedReservation?.status || 'CONFIRMED')">
                      {{ getReservationStatusText(selectedReservation?.status || 'CONFIRMED') }}
                    </el-tag>
                  </div>
                  <div class="info-item">
                    <span class="label">预定日期:</span>
                    <span class="value">{{ channelInfo?.bookingDate || selectedReservation?.createdAt || '-' }}</span>
                  </div>
                  <div class="info-item">
                    <span class="label">支付方式:</span>
                    <el-tag v-if="channelInfo?.paymentMethod" type="warning">
                      {{ channelInfo.paymentMethod }}
                    </el-tag>
                    <span v-else class="value">-</span>
                  </div>
                </div>
              </div>

              <!-- 价格信息 -->
              <div class="channel-section">
                <h4>价格信息</h4>
                <div class="price-table">
                  <el-table :data="channelPriceRows" border>
                    <el-table-column prop="label" label="" width="150" />
                    <el-table-column prop="value" label="" align="right" />
                  </el-table>
                </div>
              </div>

              <!-- 房间详情 -->
              <div class="channel-section">
                <h4>房间详情</h4>
                <div class="info-grid">
                  <div class="info-item">
                    <span class="label">房型:</span>
                    <span class="value">{{ selectedReservation?.roomTypeName || '-' }}</span>
                  </div>
                  <div class="info-item">
                    <span class="label">客人姓名:</span>
                    <span class="value">{{ selectedReservation?.guestName || '-' }} ({{ selectedReservation?.adults || 1 }}成人, {{ selectedReservation?.children || 0 }}儿童)</span>
                  </div>
                  <div class="info-item">
                    <span class="label">入离日期:</span>
                    <span class="value">{{ selectedReservation?.checkInDate }} ~ {{ selectedReservation?.checkOutDate }}</span>
                  </div>
                  <div class="info-item">
                    <span class="label">间夜数:</span>
                    <span class="value">{{ channelNightsText }}</span>
                  </div>
                  <div class="info-item">
                    <span class="label">价格计划:</span>
                    <span class="value">{{ channelInfo?.pricePlan || '-' }}</span>
                  </div>
                </div>
              </div>

              <!-- 特殊需求 -->
              <div class="channel-section">
                <h4>特殊需求</h4>
                <div class="special-requests">
                  <p v-if="channelInfo?.specialRequests || selectedReservation?.notes">
                    {{ channelInfo?.specialRequests || selectedReservation?.notes }}
                  </p>
                  <el-empty v-else description="无特殊需求" :image-size="60" />
                </div>
              </div>
            </div>
          </div>

          <div class="detail-footer">
            <el-button @click="showBookingDetailSidebar = false">打印</el-button>
            <el-button type="primary" @click="handleModifyOrder">修改订单</el-button>
            <el-button
              v-if="
                selectedDate &&
                (isToday(selectedDate) || isAfterToday(selectedDate)) &&
                !isCheckedInStatus(selectedReservation?.status || '')
              "
              type="success"
              @click="handleCheckIn"
            >
              办理入住
            </el-button>
          </div>
        </div>
      </el-drawer>

      <!-- 取消预约侧边栏 -->
      <el-drawer
        v-model="showCancelReservationSidebar"
        title="取消预约"
        direction="rtl"
        size="400px"
      >
        <div class="cancel-reservation-content">
          <div class="warning-info">
            <el-alert
              title="确认要取消此预约吗？"
              type="warning"
              show-icon
              :closable="false"
              style="margin-bottom: 20px"
            />

            <div class="reservation-info" v-if="selectedReservation">
              <div class="info-row">
                <span class="label">订单号：</span>
                <span class="value">{{ selectedReservation.orderNumber }}</span>
              </div>
              <div class="info-row">
                <span class="label">客人姓名：</span>
                <span class="value">{{ selectedReservation.guestName }}</span>
              </div>
              <div class="info-row">
                <span class="label">房间：</span>
                <span class="value"
                  >{{ selectedReservation.roomTypeName }}-{{ selectedReservation.roomNumber }}</span
                >
              </div>
              <div class="info-row">
                <span class="label">入住日期：</span>
                <span class="value">{{ selectedReservation.checkInDate }}</span>
              </div>
              <div class="info-row">
                <span class="label">离店日期：</span>
                <span class="value">{{ selectedReservation.checkOutDate }}</span>
              </div>
            </div>
          </div>

          <div class="cancel-form">
            <el-form :model="cancelForm" label-width="100px">
              <el-form-item label="取消原因">
                <el-select
                  v-model="cancelForm.reason"
                  placeholder="请选择取消原因"
                  style="width: 100%"
                >
                  <el-option label="客人主动取消" value="guest_cancel" />
                  <el-option label="房间问题" value="room_issue" />
                  <el-option label="系统错误" value="system_error" />
                  <el-option label="其他原因" value="other" />
                </el-select>
              </el-form-item>

              <el-form-item label="备注说明">
                <el-input
                  v-model="cancelForm.notes"
                  type="textarea"
                  :rows="3"
                  placeholder="请输入取消说明..."
                />
              </el-form-item>

              <el-form-item label="退款处理">
                <el-radio-group v-model="cancelForm.refundType">
                  <el-radio value="full">全额退款</el-radio>
                  <el-radio value="partial">部分退款</el-radio>
                  <el-radio value="none">不退款</el-radio>
                </el-radio-group>
              </el-form-item>

              <el-form-item v-if="cancelForm.refundType === 'partial'" label="退款金额">
                <el-input-number
                  v-model="cancelForm.refundAmount"
                  :min="0"
                  :max="selectedReservation?.totalAmount || 0"
                  :precision="2"
                  style="width: 100%"
                />
              </el-form-item>
            </el-form>
          </div>

          <div class="cancel-actions">
            <el-button @click="showCancelReservationSidebar = false">取消</el-button>
            <el-button type="danger" @click="confirmCancelReservation">确认取消预约</el-button>
          </div>
        </div>
      </el-drawer>

      <!-- 添加消费侧边栏 -->
      <el-drawer
        v-model="showAddConsumptionSidebar"
        direction="rtl"
        size="540px"
        :show-close="false"
      >
        <template #header>
          <div style="display: flex; align-items: center; justify-content: space-between; width: 100%">
            <div style="display: flex; align-items: center; gap: 12px">
              <el-button link @click="showAddConsumptionSidebar = false">
                <el-icon><ArrowLeft /></el-icon>
                返回
              </el-button>
              <span style="font-size: 16px; font-weight: 500">添加消费</span>
            </div>
            <el-button link @click="showAddConsumptionSidebar = false">
              <el-icon><Close /></el-icon>
            </el-button>
          </div>
        </template>

        <div style="padding: 20px">
            <el-form :model="consumptionForm" label-position="left" label-width="100px">
            <el-form-item label="消费项目" required>
              <el-select
                v-model="consumptionForm.item"
                placeholder="项目"
                style="width: 100%"
                :loading="enabledConsumptionItemsLoading"
                filterable
                @change="handleConsumptionFormItemChange"
              >
                <el-option
                  v-for="option in enabledConsumptionItemOptions"
                  :key="option.value"
                  :label="option.label"
                  :value="option.value"
                />
              </el-select>
            </el-form-item>

            <el-form-item label="数量" required>
              <el-input-number
                v-model="consumptionForm.quantity"
                :min="1"
                :max="100"
                controls-position="right"
                style="width: 100%"
              />
            </el-form-item>

            <el-form-item label="金额" required>
              <el-input v-model="consumptionForm.amount" placeholder="请输入金额" type="number">
                <template #prefix>¥</template>
              </el-input>
            </el-form-item>

            <el-form-item label="消费日期" required>
              <el-date-picker
                v-model="consumptionForm.date"
                type="date"
                placeholder="选择日期"
                style="width: 100%"
                format="YYYY-MM-DD"
                value-format="YYYY-MM-DD"
              />
            </el-form-item>

            <el-form-item label="备注">
              <el-input
                v-model="consumptionForm.remark"
                type="textarea"
                :rows="4"
                placeholder="备注信息"
                maxlength="200"
                show-word-limit
              />
            </el-form-item>
          </el-form>

          <div style="display: flex; justify-content: flex-end; gap: 12px; margin-top: 24px">
            <el-button @click="showAddConsumptionSidebar = false">取消</el-button>
            <el-button type="primary" @click="submitConsumption">确定</el-button>
          </div>
        </div>
      </el-drawer>

      <!-- 收款/退款侧边栏 -->
      <el-drawer
        v-model="showPaymentSidebar"
        direction="rtl"
        size="540px"
        :show-close="false"
      >
        <template #header>
          <div style="display: flex; align-items: center; justify-content: space-between; width: 100%">
            <div style="display: flex; align-items: center; gap: 12px">
              <el-button link @click="showPaymentSidebar = false">
                <el-icon><ArrowLeft /></el-icon>
                返回
              </el-button>
              <el-tabs v-model="activePaymentTab" style="margin: 0">
                <el-tab-pane label="收款" name="payment" />
                <el-tab-pane label="退款" name="refund" />
              </el-tabs>
            </div>
            <el-button link @click="showPaymentSidebar = false">
              <el-icon><Close /></el-icon>
            </el-button>
          </div>
        </template>

        <div style="padding: 20px">
          <!-- 收款内容 -->
          <div v-if="activePaymentTab === 'payment'">
            <!-- 金额信息卡片 -->
            <div style="display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; margin-bottom: 24px">
              <div style="border: 1px solid #e5e7eb; border-radius: 8px; padding: 16px">
                <div style="font-size: 12px; color: #6b7280; margin-bottom: 4px">订单金额</div>
                <div style="font-size: 18px; font-weight: 600; color: #1890ff">
                  ¥{{ Number(selectedReservation?.totalAmount || 0).toFixed(2) }}
                </div>
              </div>
              <div style="border: 1px solid #e5e7eb; border-radius: 8px; padding: 16px">
                <div style="font-size: 12px; color: #6b7280; margin-bottom: 4px">已收金额</div>
                <div style="font-size: 18px; font-weight: 600; color: #1890ff">¥{{ totalPayment.toFixed(2) }}</div>
              </div>
              <div style="border: 1px solid #e5e7eb; border-radius: 8px; padding: 16px">
                <div style="font-size: 12px; color: #6b7280; margin-bottom: 4px">还差付款</div>
                <div
                  style="font-size: 18px; font-weight: 600"
                  :style="{ color: remainingPayment >= 0 ? '#ef4444' : '#10b981' }"
                >
                  {{ remainingPayment >= 0 ? '-' : '+' }}¥{{ Math.abs(remainingPayment).toFixed(2) }}
                </div>
              </div>
            </div>

            <!-- 收款类型选择 -->
            <el-form :model="paymentForm" label-position="left">
              <el-form-item label="项目" required style="margin-bottom: 20px">
                <el-radio-group v-model="paymentForm.type">
                  <el-radio value="payment">收款</el-radio>
                  <el-radio value="deposit">收押金</el-radio>
                </el-radio-group>
              </el-form-item>

              <el-form-item label="支付方式" required>
                <el-select v-model="paymentForm.paymentMethod" placeholder="请选择" style="width: 100%">
                  <el-option
                    v-for="option in paymentMethodOptions"
                    :key="option.value"
                    :label="option.label"
                    :value="option.value"
                  />
                </el-select>
              </el-form-item>

              <el-form-item label="金额" required>
                <el-input v-model="paymentForm.amount" placeholder="金额" type="number">
                  <template #prefix>¥</template>
                </el-input>
              </el-form-item>

              <el-form-item label="日期" required>
                <el-date-picker
                  v-model="paymentForm.date"
                  type="date"
                  placeholder="选择日期"
                  style="width: 100%"
                  format="MM-DD"
                  value-format="YYYY-MM-DD"
                />
              </el-form-item>

              <el-form-item label="备注">
                <el-input
                  v-model="paymentForm.remark"
                  type="textarea"
                  :rows="4"
                  placeholder="备注信息"
                  maxlength="200"
                  show-word-limit
                />
              </el-form-item>
            </el-form>
          </div>

          <!-- 退款内容 -->
          <div v-if="activePaymentTab === 'refund'">
            <!-- 金额信息卡片 -->
            <div style="display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; margin-bottom: 24px">
              <div style="border: 1px solid #e5e7eb; border-radius: 8px; padding: 16px">
                <div style="font-size: 12px; color: #6b7280; margin-bottom: 4px">订单金额</div>
                <div style="font-size: 18px; font-weight: 600; color: #1890ff">
                  ¥{{ Number(selectedReservation?.totalAmount || 0).toFixed(2) }}
                </div>
              </div>
              <div style="border: 1px solid #e5e7eb; border-radius: 8px; padding: 16px">
                <div style="font-size: 12px; color: #6b7280; margin-bottom: 4px">已收金额</div>
                <div style="font-size: 18px; font-weight: 600; color: #1890ff">¥{{ totalPayment.toFixed(2) }}</div>
              </div>
              <div style="border: 1px solid #e5e7eb; border-radius: 8px; padding: 16px">
                <div style="font-size: 12px; color: #6b7280; margin-bottom: 4px">还差付款</div>
                <div
                  style="font-size: 18px; font-weight: 600"
                  :style="{ color: remainingPayment >= 0 ? '#ef4444' : '#10b981' }"
                >
                  {{ remainingPayment >= 0 ? '-' : '+' }}¥{{ Math.abs(remainingPayment).toFixed(2) }}
                </div>
              </div>
            </div>

            <!-- 退款方式选择 -->
            <div style="display: flex; gap: 12px; margin-bottom: 24px">
              <el-button
                :type="refundType === 'online' ? 'primary' : 'default'"
                style="flex: 1"
                @click="refundType = 'online'"
              >
                线上原路退款
              </el-button>
              <el-button
                :type="refundType === 'offline' ? 'primary' : 'default'"
                style="flex: 1"
                @click="refundType = 'offline'"
              >
                线下退款
              </el-button>
            </div>

            <!-- 提示文字 -->
            <div style="font-size: 14px; color: #6b7280; margin-bottom: 16px">请选择需要退款的流水</div>

            <!-- 退款记录表格 -->
            <el-table :data="[]" border style="width: 100%">
              <el-table-column prop="date" label="发生日期" width="100" />
              <el-table-column prop="amount" label="收款金额" width="100" />
              <el-table-column prop="remark" label="备注" />
              <el-table-column prop="refundable" label="可退金额" width="100" />
            </el-table>

            <el-empty description="暂无数据" style="padding: 40px 0" />
          </div>

          <!-- 底部按钮 -->
          <div style="display: flex; justify-content: flex-end; gap: 12px; margin-top: 24px">
            <el-button @click="showPaymentSidebar = false">取消</el-button>
            <el-button
              v-if="activePaymentTab === 'payment'"
              type="primary"
              @click="submitPayment"
            >
              下一步
            </el-button>
            <el-button v-if="activePaymentTab === 'refund'" type="primary" disabled>下一步</el-button>
          </div>
        </div>
      </el-drawer>

      <!-- 批量操作弹窗 -->
      <el-dialog
        v-model="showBatchDialog"
        :title="batchDialogTitle"
        width="800px"
        :close-on-click-modal="false"
      >
        <div class="batch-dialog-content">
          <div class="selection-area">
            <div class="batch-room-tree">
              <div class="selection-header">
                <el-checkbox
                  v-model="batchSelectAll"
                  :indeterminate="batchSelectIndeterminate"
                >
                  全部
                </el-checkbox>
                <span class="selection-count">{{ batchSelectedCount }}/{{ batchTotalCount }}</span>
              </div>

              <el-input
                v-model="batchSearchKeyword"
                placeholder="请输入搜索内容"
                clearable
                class="batch-search-input"
              />

              <div class="batch-tree-list">
                <div
                  v-for="group in batchDisplayGroups"
                  :key="group.roomType"
                  class="batch-type-block"
                >
                  <div class="batch-type-row">
                    <el-icon class="expand-icon" @click.stop="toggleBatchTypeExpanded(group.roomType)">
                      <component
                        :is="isBatchTypeExpanded(group.roomType) ? ArrowDown : ArrowRight"
                      />
                    </el-icon>
                    <el-checkbox
                      :model-value="isBatchTypeChecked(group.roomType)"
                      :indeterminate="isBatchTypeIndeterminate(group.roomType)"
                      @change="(checked: boolean) => handleBatchTypeCheck(group.roomType, checked)"
                    />
                    <span class="room-type-name">{{ group.roomType }}</span>
                  </div>

                  <div v-show="isBatchTypeExpanded(group.roomType)" class="batch-room-list">
                    <div
                      v-for="room in group.rooms"
                      :key="room.roomId"
                      class="batch-room-item"
                    >
                      <el-checkbox
                        :model-value="isBatchRoomChecked(room.roomId)"
                        @change="(checked: boolean) => handleBatchRoomCheck(room.roomId, checked)"
                      />
                      <span class="batch-room-number">{{ room.roomNumber }}</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <div class="selected-rooms-area">
              <div class="selection-header">
                <span class="selected-rooms-title">已选{{ batchSelectedCount }}间房</span>
              </div>

              <div v-if="batchSelectedRooms.length === 0" class="empty-selection">
                <p>请在左侧选择要操作的房型和房间</p>
              </div>
              <div v-else class="selected-room-list">
                <div
                  v-for="room in batchSelectedRooms"
                  :key="room.roomId"
                  class="selected-room-item"
                >
                  <span>{{ room.roomType }} {{ room.roomNumber }}</span>
                  <el-button link size="small" @click="removeBatchSelectedRoom(room.roomId)">
                    <el-icon><Close /></el-icon>
                  </el-button>
                </div>
              </div>
            </div>
          </div>
        </div>

        <template #footer>
          <div class="batch-dialog-footer">
            <el-button @click="cancelBatchOperation">取消</el-button>
            <el-button type="primary" @click="confirmBatchOperation">
              {{ batchAction === 'close' || batchAction === 'open' ? '下一步' : '确定' }}
            </el-button>
          </div>
        </template>
      </el-dialog>

      <!-- 关房弹窗 -->
      <el-dialog
        v-model="showCloseRoomDialog"
        title="关房"
        width="600px"
        :close-on-click-modal="false"
      >
        <div class="close-room-content">
          <el-alert
            title="房间转'停用房'或'维修房'后，将会影响入住率"
            type="info"
            show-icon
            :closable="false"
            style="margin-bottom: 20px"
          />

          <el-form :model="closeRoomForm" label-width="80px">
            <el-form-item label="关房类型">
              <el-radio-group v-model="closeRoomForm.type">
                <el-radio value="stop">停用房</el-radio>
                <el-radio value="maintenance">维修房</el-radio>
                <el-radio value="retain">保留房</el-radio>
              </el-radio-group>
            </el-form-item>

            <el-form-item label="时间">
              <div class="date-range-container">
                <el-date-picker
                  v-model="closeRoomForm.startDate"
                  type="date"
                  placeholder="2025-09-26"
                  format="YYYY-MM-DD"
                  value-format="YYYY-MM-DD"
                  style="width: 45%"
                />
                <span style="margin: 0 10px">至</span>
                <el-date-picker
                  v-model="closeRoomForm.endDate"
                  type="date"
                  placeholder="2025-09-26"
                  format="YYYY-MM-DD"
                  value-format="YYYY-MM-DD"
                  style="width: 45%"
                />
              </div>
            </el-form-item>

            <el-form-item label="备注">
              <el-input
                v-model="closeRoomForm.remark"
                type="textarea"
                placeholder="请输入内容（选填）"
                :rows="4"
                maxlength="200"
                show-word-limit
              />
            </el-form-item>
          </el-form>
        </div>

        <template #footer>
          <div class="dialog-footer">
            <el-button @click="cancelCloseRoom">取消</el-button>
            <el-button type="primary" @click="confirmCloseRoom">确定</el-button>
          </div>
        </template>
      </el-dialog>

      <!-- 批量关房详细设置弹窗 -->
      <el-dialog
        v-model="showBatchCloseRoomDialog"
        :title="batchAction === 'open' ? '批量开房' : '批量关房'"
        width="600px"
        :close-on-click-modal="false"
      >
        <div class="batch-close-room-content">
          <el-alert
            title="房间转'停用房'或'维修房'后，将会影响入住率"
            type="info"
            show-icon
            :closable="false"
            style="margin-bottom: 20px"
          />

          <el-form :model="batchCloseForm" label-width="80px">
            <el-form-item label="房间">
              <div class="selected-rooms-display">
                <div
                  v-for="room in batchCloseSelectedRooms"
                  :key="room.roomId"
                  class="batch-close-room-tag"
                >
                  <span>{{ room.roomNumber }}</span>
                  <el-button link size="small" @click="removeBatchCloseRoom(room.roomId)">
                    <el-icon><Close /></el-icon>
                  </el-button>
                </div>
                <span v-if="batchCloseSelectedRooms.length === 0" class="batch-close-empty-tip">
                  请先在上一步选择房间
                </span>
              </div>
            </el-form-item>

            <el-form-item label="日期">
              <div class="date-range-container">
                <el-date-picker
                  v-model="batchCloseForm.startDate"
                  type="date"
                  placeholder="开始日期"
                  format="YYYY-MM-DD"
                  value-format="YYYY-MM-DD"
                  style="width: 45%"
                />
                <span style="margin: 0 10px">至</span>
                <el-date-picker
                  v-model="batchCloseForm.endDate"
                  type="date"
                  placeholder="结束日期"
                  format="YYYY-MM-DD"
                  value-format="YYYY-MM-DD"
                  style="width: 45%"
                />
              </div>
              <div class="date-selector" style="margin-top: 10px">
                <el-dropdown @command="handleDateSelectorCommand">
                  <el-button link>
                    {{ batchDateSelectorText }} <el-icon><ArrowDown /></el-icon>
                  </el-button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item command="all">全部日期</el-dropdown-item>
                      <el-dropdown-item command="weekday">工作日</el-dropdown-item>
                      <el-dropdown-item command="weekend">周末</el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </div>
            </el-form-item>

            <el-form-item label="关房类型">
              <el-radio-group v-model="batchCloseForm.type">
                <el-radio value="stop">停用房</el-radio>
                <el-radio value="maintenance">维修房</el-radio>
                <el-radio value="retain">保留房</el-radio>
              </el-radio-group>
            </el-form-item>

            <el-form-item label="备注">
              <el-input
                v-model="batchCloseForm.remark"
                type="textarea"
                placeholder="请输入内容（选填）"
                :rows="4"
                maxlength="200"
                show-word-limit
              />
            </el-form-item>
          </el-form>
        </div>

        <template #footer>
          <div class="dialog-footer">
            <el-button @click="cancelBatchCloseRoom">上一步</el-button>
            <el-button type="primary" @click="confirmBatchCloseRoom">确定</el-button>
          </div>
        </template>
      </el-dialog>

      <el-dialog
        v-model="showRoomChangeConfirmDialog"
        title="确定要换房吗？"
        width="520px"
        :close-on-click-modal="false"
      >
        <div class="room-change-confirm-content">
          <p class="room-change-summary" v-if="pendingRoomChange">
            {{ pendingRoomChange.guestName || '-' }}：
            {{ pendingRoomChange.sourceRoomNumber || '-' }} -> {{ pendingRoomChange.targetRoomNumber || '-' }}
            （{{ pendingRoomChange.checkInDate }} 至 {{ pendingRoomChange.checkOutDate }}）
          </p>
          <el-checkbox v-model="roomChangeUpdatePrice" class="room-change-price-checkbox">
            换房同时更新房价
          </el-checkbox>
        </div>
        <template #footer>
          <div class="dialog-footer">
            <el-button @click="cancelRoomChangeConfirm">取消</el-button>
            <el-button type="primary" :loading="roomChangeSubmitting" @click="confirmRoomChange">
              确定
            </el-button>
          </div>
        </template>
      </el-dialog>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onActivated, onBeforeUnmount, computed, watch, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox, ElLoading } from 'element-plus'
import {
  ArrowLeft,
  ArrowRight,
  ArrowDown,
  ArrowUp,
  Search,
  Check,
  Right,
  User,
  Calendar,
  Shop,
  Tools,
  Remove,
  Delete,
  Close,
  Plus,
  Loading,
} from '@element-plus/icons-vue'
import { useRoomStatusStore } from '@/stores/roomStatus'
import { useUserStore } from '@/stores/user'
import {
  createReservation,
  checkInReservation,
  cancelReservation,
  checkOutReservation,
  getReservationById,
  getReservationChannelInfo,
  assignReservationRoom,
  updateReservation,
  searchReservations,
  type ReservationDTO,
  type ReservationChannelInfoDTO,
} from '@/api/reservation'
import { closeRoomBlockouts, getRoomStatusCalendar, openRoomBlockouts } from '@/api/roomStatus'
import type { CreateReservationRequest } from '@/api/reservation'
import { RoomStatus, ReservationStatus } from '@/types/room'
import type { CalendarRoomData, DailyRoomStatus } from '@/types/room'
import { request } from '@/utils/request'
import { getAllChannels, type ChannelDTO } from '@/api/channel'
import { getRoomTypeByRoomId, getRoomCurrentPrice, getEffectiveRoomPrice, type RoomTypeDTO } from '@/api/roomType'
import { getPricePlansByRoomType, type RoomTypePricePlanDTO } from '@/api/pricePlan'
import { getRoomPriceManagementData, type RoomPriceManagementDTO } from '@/api/roomPrice'
import { getSortOrderMap } from '@/api/sortConfig'
import { getAllRoomGroups, getGroupMembers, type RoomGroupDTO, type RoomGroupMemberDTO } from '@/api/roomGroup'
import { calculateTotalPriceByDates } from '@/utils/priceHelper'
import { createConsumption, getConsumptionsByReservationId, deleteConsumption, getTotalConsumption, type ConsumptionDTO } from '@/api/consumption'
import { createPayment, getPaymentsByReservationId, deletePayment, getTotalPayment, type PaymentDTO } from '@/api/payment'
import { getOperationLogsByReservationId, type OperationLogDTO } from '@/api/operationLog'
import { getConsumptionItemsByEnabled, type ConsumptionItemDTO } from '@/api/consumptionItem'
import { getEnabledPaymentMethods, type PaymentMethodDTO } from '@/api/paymentMethod'
import {
  getSuWebhookEvents,
  processSuWebhookEvents,
  type SuReservationWebhookEventDTO,
  type SuWebhookEventStatus,
} from '@/api/suWebhookEvents'

const router = useRouter()
const route = useRoute()
const roomStatusStore = useRoomStatusStore()
const userStore = useUserStore()

// 响应式数据
const searchKeyword = ref('')
const searchResults = ref<ReservationDTO[]>([])
const searchTimeout = ref<number | null>(null)
// 移除固定的日期范围，改为动态计算
// const dateRange = ref<[string, string]>(['2025-09-21', '2025-10-04'])

// 添加当前基准日期，用于控制日历显示的起始位置
const currentBaseDate = ref<string>(new Date().toISOString().split('T')[0])

const parseYmdDate = (value: string) => {
  const [year, month, day] = value.split('-').map((part) => Number(part))
  return new Date(year, (month || 1) - 1, day || 1)
}

const formatYmdDate = (date: Date) => {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const shiftYmdDate = (value: string, days: number) => {
  const date = parseYmdDate(value)
  date.setDate(date.getDate() + days)
  return formatYmdDate(date)
}

// 渠道数据
const channels = ref<ChannelDTO[]>([])
const channelMap = ref<Map<string, ChannelDTO>>(new Map())
const showFilterSidebar = ref(false)
const selectedRoom = ref<CalendarRoomData | null>(null)
const selectedDate = ref('')
const selectedDailyStatus = ref<DailyRoomStatus | null>(null)

// 快速操作菜单
const showQuickActions = ref(false)
const quickActionPosition = ref({ x: 0, y: 0 })
const quickActionRoom = ref<CalendarRoomData | null>(null)
const quickActionDate = ref('')
const quickActionsPopupRef = ref<HTMLElement | null>(null)

// 预订相关侧边栏（支持新建和编辑两种模式）
const showBookingSidebar = ref(false)
const bookingMode = ref<'create' | 'edit' | 'check-in'>('create')
const showBookingDetailSidebar = ref(false)
const selectedReservation = ref<any>(null)
const activeDetailTab = ref('detail')

interface ReservationDragContext {
  reservationId: number
  sourceRoomId: number
  sourceRoomNumber: string
  guestName: string
  checkInDate: string
  checkOutDate: string
}

interface PendingRoomChange {
  reservationId: number
  targetRoomId: number
  sourceRoomNumber: string
  targetRoomNumber: string
  guestName: string
  checkInDate: string
  checkOutDate: string
}

const draggingReservationContext = ref<ReservationDragContext | null>(null)
const roomChangeDropTargetKey = ref('')
const showRoomChangeConfirmDialog = ref(false)
const roomChangeUpdatePrice = ref(false)
const roomChangeSubmitting = ref(false)
const pendingRoomChange = ref<PendingRoomChange | null>(null)

const currentOperatorName = computed(() => {
  return userStore.currentUser?.nickname || userStore.currentUser?.email || '系统'
})

// 操作日志相关
const logFilterType = ref<'all' | 'order' | 'billing' | 'compensation'>('all')
const operationLogs = ref<OperationLogDTO[]>([])
const operationLogsLoading = ref(false)

const channelInfo = ref<ReservationChannelInfoDTO | null>(null)
const channelInfoLoading = ref(false)

// 操作日志接口
interface OperationLogSample {
  id: string
  action: string
  operator: string
  timestamp: string
  type: 'order' | 'billing'
  content?: string
  details?: Array<{ label: string; value: string }>
}

// 操作日志数据（示例数据）
const operationLogSamples = ref<OperationLogSample[]>([
  {
    id: '1',
    action: '办理入住',
    operator: '齐藤 广志',
    timestamp: '2026/01/31 16:34:20',
    type: 'order',
    content: '入住房间: Tanpopo Inn304-304',
  },
  {
    id: '2',
    action: '新增预订订单',
    operator: '系统',
    timestamp: '2025/11/24 19:30:52',
    type: 'order',
    details: [
      { label: '联系人', value: 'KAZUKO KIMURA' },
      { label: '手机号', value: '+81 9051835283' },
      { label: '邮箱', value: 'kkimur.786937@guest.booking.com' },
      { label: '渠道', value: 'Booking.com' },
      { label: '渠道订单号', value: '6538219044' },
      { label: '房间', value: '北赤羽304-304' },
      { label: '入住类型', value: '正常入住' },
      { label: '入离时间', value: '2026-01-31 16:00 至 2026-02-01 10:00' },
      { label: '成人数', value: '2' },
      { label: '房费', value: '¥6,018.46' },
      { label: '订单金额', value: '¥6,018.46' },
    ],
  },
  {
    id: '3',
    action: '收银',
    operator: '系统',
    timestamp: '2025/11/24 19:30:52',
    type: 'billing',
    details: [
      { label: '类型', value: '收款-订单金额' },
      { label: '金额', value: '¥14,018.46' },
      { label: '归属营业日', value: '2025-11-24' },
      { label: '支付方式', value: 'Booking代收' },
      { label: '备注', value: '-' },
    ],
  },
])

// 过滤后的操作日志
const filteredOperationLogs = computed(() => {
  if (logFilterType.value === 'compensation') {
    return []
  }
  if (logFilterType.value === 'all') {
    return operationLogs.value
  }
  return operationLogs.value.filter(log => log.type === logFilterType.value)
})

// SU webhook 补偿/重试
const suWebhookStatusFilter = ref<SuWebhookEventStatus>('FAILED')
const suWebhookEvents = ref<SuReservationWebhookEventDTO[]>([])
const suWebhookEventsLoading = ref(false)
const suWebhookEventsProcessing = ref(false)

const suWebhookStatusOptions: Array<{ label: string; value: SuWebhookEventStatus }> = [
  { label: '失败（FAILED）', value: 'FAILED' },
  { label: '已进入死信（DEAD）', value: 'DEAD' },
  { label: '已接收（RECEIVED）', value: 'RECEIVED' },
  { label: '处理中（PROCESSING）', value: 'PROCESSING' },
  { label: '已处理（PROCESSED）', value: 'PROCESSED' },
]

const loadSuWebhookEvents = async () => {
  suWebhookEventsLoading.value = true
  try {
    const res = await getSuWebhookEvents({ status: suWebhookStatusFilter.value, size: 50 })
    if (res.success) {
      suWebhookEvents.value = res.data || []
    } else {
      suWebhookEvents.value = []
    }
  } catch (error) {
    console.error('加载补偿事件失败:', error)
    suWebhookEvents.value = []
  } finally {
    suWebhookEventsLoading.value = false
  }
}

const handleProcessSuWebhookEvents = async () => {
  if (suWebhookEventsProcessing.value) return
  suWebhookEventsProcessing.value = true
  try {
    const res = await processSuWebhookEvents({ limit: 50 })
    if (res.success) {
      ElMessage.success(`已触发处理：${res.data ?? 0} 条`)
    } else {
      ElMessage.error(res.message || '触发处理失败')
    }
    await loadSuWebhookEvents()
  } catch (error) {
    console.error('触发补偿处理失败:', error)
    ElMessage.error('触发处理失败，请稍后再试')
  } finally {
    suWebhookEventsProcessing.value = false
  }
}

const handleOpenCompensationPanel = async () => {
  logFilterType.value = 'compensation'
  await loadSuWebhookEvents()
}

watch(suWebhookStatusFilter, async () => {
  if (logFilterType.value !== 'compensation') return
  await loadSuWebhookEvents()
})

const loadOperationLogs = async (reservationId: number) => {
  operationLogsLoading.value = true
  operationLogs.value = []
  try {
    const res = await getOperationLogsByReservationId(reservationId)
    if (res.success) {
      operationLogs.value = res.data || []
    } else {
      operationLogs.value = []
    }
  } catch (error) {
    console.error('加载操作日志失败:', error)
    operationLogs.value = []
  } finally {
    operationLogsLoading.value = false
  }
}

const loadChannelInfo = async (reservationId: number) => {
  channelInfoLoading.value = true
  channelInfo.value = null
  try {
    const res = await getReservationChannelInfo(reservationId)
    if (res.success) {
      channelInfo.value = res.data || null
    } else {
      channelInfo.value = null
    }
  } catch (error) {
    console.error('加载渠道信息失败:', error)
    channelInfo.value = null
  } finally {
    channelInfoLoading.value = false
  }
}

const formatMoney = (amount?: number | null) => {
  if (amount === null || amount === undefined) return '-'
  const num = Number(amount)
  if (Number.isNaN(num)) return '-'
  return `¥${num.toFixed(2)}`
}

const channelLogoText = computed(() => {
  const name = channelInfo.value?.channelName || selectedReservation.value?.channelName || ''
  const normalized = (name || '').replace(/\.com$/i, '').trim()
  if (!normalized) return 'OTA'
  return normalized.length > 10 ? normalized.slice(0, 10) : normalized
})

const getReservationDateValue = (
  reservation: Record<string, any> | null | undefined,
  key: 'checkIn' | 'checkOut',
) => {
  if (!reservation) return ''
  return reservation[`${key}Date`] || reservation[key] || ''
}

const getReservationDateOnly = (value: string) => normalizeDateOnly(value)

const getReservationStayEndDate = (reservation: Record<string, any> | null | undefined) => {
  const checkOutDate = getReservationDateOnly(getReservationDateValue(reservation, 'checkOut'))
  if (!checkOutDate) return ''
  return addDays(checkOutDate, -1)
}

const isReservationStartCell = (dailyStatus: DailyRoomStatus) => {
  if (!dailyStatus.reservation) return false
  const checkInDate = getReservationDateOnly(getReservationDateValue(dailyStatus.reservation, 'checkIn'))
  return !!checkInDate && normalizeDateOnly(dailyStatus.date) === checkInDate
}

const isReservationEndCell = (dailyStatus: DailyRoomStatus) => {
  if (!dailyStatus.reservation) return false
  const stayEndDate = getReservationStayEndDate(dailyStatus.reservation)
  return !!stayEndDate && normalizeDateOnly(dailyStatus.date) === stayEndDate
}

const isReservationMiddleCell = (dailyStatus: DailyRoomStatus) => {
  return !!dailyStatus.reservation && !isReservationStartCell(dailyStatus) && !isReservationEndCell(dailyStatus)
}

const getReservationDateDiffNights = (reservation: Record<string, any> | null | undefined) => {
  if (!reservation) return null

  const checkIn = getReservationDateValue(reservation, 'checkIn')
  const checkOut = getReservationDateValue(reservation, 'checkOut')
  if (!checkIn || !checkOut) return null

  const start = new Date(checkIn)
  const end = new Date(checkOut)
  if (Number.isNaN(start.getTime()) || Number.isNaN(end.getTime())) return null

  const diff = end.getTime() - start.getTime()
  const nights = Math.round(diff / (1000 * 60 * 60 * 24))
  return nights > 0 ? nights : null
}

const getReservationRoomCount = (reservation: Record<string, any> | null | undefined) => {
  if (!reservation || !calendarData.value?.rooms?.length) return 1

  const explicitRoomCount = Number(reservation.roomCount ?? reservation.reservationCount)
  if (Number.isFinite(explicitRoomCount) && explicitRoomCount > 0) {
    return explicitRoomCount
  }

  const groupOrderNo = reservation.groupOrderNo
  const orderNumber = reservation.orderNumber
  const guestName = reservation.guestName
  const checkIn = getReservationDateValue(reservation, 'checkIn')
  const checkOut = getReservationDateValue(reservation, 'checkOut')
  const matchedRoomIds = new Set<number>()

  calendarData.value.rooms.forEach((room) => {
    const hasMatchedReservation = room.dailyStatus.some((daily) => {
      const currentReservation = daily.reservation as Record<string, any> | null | undefined
      if (!currentReservation) return false

      const currentGroupOrderNo = currentReservation.groupOrderNo
      const currentOrderNumber = currentReservation.orderNumber
      const currentGuestName = currentReservation.guestName
      const currentCheckIn = getReservationDateValue(currentReservation, 'checkIn')
      const currentCheckOut = getReservationDateValue(currentReservation, 'checkOut')

      if (groupOrderNo && currentGroupOrderNo) {
        return currentGroupOrderNo === groupOrderNo
      }

      if (orderNumber && currentOrderNumber === orderNumber) {
        return true
      }

      return !!guestName && guestName === currentGuestName && checkIn === currentCheckIn && checkOut === currentCheckOut
    })

    if (hasMatchedReservation) {
      matchedRoomIds.add(room.roomId)
    }
  })

  return matchedRoomIds.size > 0 ? matchedRoomIds.size : 1
}

const getReservationStayNights = (reservation: Record<string, any> | null | undefined) => {
  if (!reservation) return 1

  const explicitNights = Number(reservation.nights)
  const dateDiffNights = getReservationDateDiffNights(reservation) ?? 0
  const normalizedExplicitNights =
    Number.isFinite(explicitNights) && explicitNights > 0 ? explicitNights : 0
  const normalizedStayNights = Math.max(normalizedExplicitNights, dateDiffNights, 1)

  return normalizedStayNights * getReservationRoomCount(reservation)
}

const getReservationStayNightsText = (reservation: Record<string, any> | null | undefined) => {
  return `${getReservationStayNights(reservation)}晚`
}

const channelNights = computed(() => {
  const explicitNights = Number(channelInfo.value?.nights)
  const channelDateNights = getReservationDateDiffNights(channelInfo.value)
  const reservationRoomNights = getReservationStayNights(selectedReservation.value)
  const candidates = [
    Number.isFinite(explicitNights) && explicitNights > 0 ? explicitNights : 0,
    channelDateNights ?? 0,
    reservationRoomNights,
  ].filter((value) => value > 0)

  if (candidates.length === 0) return null
  return Math.max(...candidates)
})

const channelNightsText = computed(() => {
  return channelNights.value ? `${channelNights.value} 晚` : '-'
})

const channelPriceRows = computed(() => {
  const total = channelInfo.value?.totalAmount ?? selectedReservation.value?.totalAmount
  return [
    { label: '总价', value: formatMoney(total) },
    { label: '佣金', value: formatMoney(channelInfo.value?.commission) },
    { label: '其他附加费', value: formatMoney(channelInfo.value?.otherFees) },
  ]
})

// 取消预约侧边栏
const showCancelReservationSidebar = ref(false)
const cancelForm = ref({
  reason: '',
  notes: '',
  refundType: 'full',
  refundAmount: 0,
})

// 添加消费侧边栏
const showAddConsumptionSidebar = ref(false)
const consumptionForm = ref({
  item: '',
  quantity: 1,
  amount: 0,
  date: '',
  remark: '',
})

// 收款/退款侧边栏
const showPaymentSidebar = ref(false)
const activePaymentTab = ref('payment') // 'payment' | 'refund'
const paymentForm = ref({
  type: 'payment', // 'payment' | 'deposit'
  paymentMethod: '',
  amount: 0,
  date: '',
  remark: '',
})

// 退款表单
const refundType = ref('online') // 'online' | 'offline'
const selectedRefundRecords = ref<any[]>([]) // 选中的退款记录

// 消费和收款数据
const consumptionList = ref<ConsumptionDTO[]>([])
const paymentList = ref<PaymentDTO[]>([])
const totalConsumption = ref(0)
const totalPayment = ref(0)

// 计算还差付款金额
const remainingPayment = computed(() => {
  const total = Number(selectedReservation.value?.totalAmount || 0)
  const consumption = Number(totalConsumption.value || 0)
  const payment = Number(totalPayment.value || 0)
  // 还需付款 = 订单金额 - 已收金额 - 其他消费
  return total - payment - consumption
})

// 折叠面板展开状态（控制订单详情中的消费、收款、提醒面板）
const activeCollapsePanels = ref<string[]>(['1', '2', '3'])

// 渠道选项 - 从动态渠道数据计算
const channelOptions = computed(() => {
  return channels.value
    .filter((channel) => channel.enabled)
    .map((channel) => ({
      id: channel.id,
      name: channel.name,
      color: channel.color,
      type: channel.type,
    }))
})

// 获取当前选中的渠道信息
const selectedChannel = computed(() => {
  if (!bookingForm.value.channelId) return null
  return channelOptions.value.find((channel) => channel.id === bookingForm.value.channelId) || null
})

// 预订表单数据（支持新建和编辑模式）
const bookingForm = ref({
  id: null as number | null, // 编辑模式时的预订ID
  guestName: '',
  guestPhone: '',
  guestIdCard: '',
  channelId: null as number | null,
  checkInDate: '',
  checkOutDate: '',
  roomId: null as number | null,
  roomNumber: '',
  roomTypeName: '',
  adults: 1,
  children: 0,
  totalAmount: 0,
  pricePlan: '',
  notes: '',
  hasSpecialColor: false,
})

// 消费项目接口
interface ConsumptionItem {
  id: string
  type: string
  quantity: number
  amount: number
  date: string
  remark: string
}

// 收款记录接口
interface PaymentItem {
  id: string
  type: string
  paymentMethod: string
  amount: number
  date: string
  remark: string
}

// 消费项目列表
const consumptionItems = ref<ConsumptionItem[]>([])

// 收款记录列表
const paymentItems = ref<PaymentItem[]>([])

const enabledConsumptionItems = ref<ConsumptionItemDTO[]>([])
const enabledConsumptionItemsLoading = ref(false)

const getConsumptionItemKey = (item: ConsumptionItemDTO) => `${item.category} - ${item.name}`

const enabledConsumptionItemOptions = computed(() => {
  return enabledConsumptionItems.value.map((item) => ({
    label: getConsumptionItemKey(item),
    value: getConsumptionItemKey(item),
  }))
})

const getEnabledConsumptionItemPrice = (key: string) => {
  const found = enabledConsumptionItems.value.find((item) => getConsumptionItemKey(item) === key)
  return found ? Number(found.price) : null
}

const loadEnabledConsumptionItems = async () => {
  enabledConsumptionItemsLoading.value = true
  try {
    const res = await getConsumptionItemsByEnabled(true)
    if (res.success) {
      enabledConsumptionItems.value = res.data || []
    } else {
      enabledConsumptionItems.value = []
    }
  } catch (error) {
    console.error('加载消费项目列表失败:', error)
    enabledConsumptionItems.value = []
  } finally {
    enabledConsumptionItemsLoading.value = false
  }
}

const handleBookingConsumptionItemChange = (key: string, item: ConsumptionItem) => {
  const price = getEnabledConsumptionItemPrice(key)
  if (price !== null && (!item.amount || item.amount <= 0)) {
    item.amount = price
  }
}

const handleConsumptionFormItemChange = (key: string) => {
  const price = getEnabledConsumptionItemPrice(key)
  if (price !== null && (!consumptionForm.value.amount || consumptionForm.value.amount <= 0)) {
    consumptionForm.value.amount = price
  }
}

// 收款类型选项
const paymentTypeOptions = [
  { label: '收押金', value: '收押金' },
  { label: '收房费', value: '收房费' },
  { label: '收消费', value: '收消费' },
  { label: '其他', value: '其他' },
]

// 支付方式选项
const FALLBACK_PAYMENT_METHOD_OPTIONS = [
  { label: '微信', value: '微信' },
  { label: '支付宝', value: '支付宝' },
  { label: '现金', value: '现金' },
  { label: '银行卡', value: '银行卡' },
]
const paymentMethodOptions = ref<{ label: string; value: string }[]>([...FALLBACK_PAYMENT_METHOD_OPTIONS])

const getDefaultPaymentMethodValue = () => {
  return paymentMethodOptions.value[0]?.value || ''
}

const loadPaymentMethodOptions = async () => {
  try {
    const response = await getEnabledPaymentMethods()
    if (response.success && Array.isArray(response.data) && response.data.length > 0) {
      paymentMethodOptions.value = response.data.map((item: PaymentMethodDTO) => ({
        label: item.name,
        value: item.name,
      }))
      return
    }
    paymentMethodOptions.value = [...FALLBACK_PAYMENT_METHOD_OPTIONS]
  } catch (error) {
    console.error('加载收款方式失败:', error)
    paymentMethodOptions.value = [...FALLBACK_PAYMENT_METHOD_OPTIONS]
  }
}

// 当前选中房间的房型信息
const currentRoomType = ref<RoomTypeDTO | null>(null)
const isLoadingPrice = ref(false)
const isManualPrice = ref(false)
const roomTypePricePlanMappings = ref<RoomTypePricePlanDTO[]>([])
const selectedPricePlanId = ref<number | null>(null)

const pricePlanOptions = computed(() => {
  const unique = new Map<number, { id: number; name: string }>()
  roomTypePricePlanMappings.value.forEach((mapping) => {
    const planId = mapping.pricePlan?.id
    if (!planId || unique.has(planId)) {
      return
    }
    unique.set(planId, {
      id: planId,
      name: mapping.pricePlan?.name || `计划${planId}`,
    })
  })
  return Array.from(unique.values())
})

// 悬停信息卡片
const showHoverCard = ref(false)
const showDirtyHover = ref(false)
const hoverCardPosition = ref({ x: 0, y: 0 })
const hoverReservation = ref<any>(null)
const hoverDirtyRoomId = ref<number | null>(null)
const hoverCardRef = ref<HTMLElement | null>(null)
const hoverCardAnchorRect = ref<DOMRect | null>(null)

// 停用房操作弹窗
const showClosedRoomActions = ref(false)
const closedRoomActionPosition = ref({ x: 0, y: 0 })
const closedRoomActionData = ref<{ room: CalendarRoomData | null; date: string }>({
  room: null,
  date: '',
})

// 筛选相关
const filterOptions = ref({
  roomTypes: [] as string[],
  selectedRoomTypes: [] as string[],
  roomGroups: [] as Array<{ id: number; name: string }>,
  selectedRoomGroupIds: [] as number[],
})
const showCellDefaultPrice = ref(false)
const cellPriceDisplaySource = ref('default')
const roomTypeDefaultPriceMap = ref<Map<string, number>>(new Map())
const calendarRoomTypePricePlanMappings = ref<RoomTypePricePlanDTO[]>([])
const loadingCellPricePlanOptions = ref(false)
const loadingCalendarManagementPrices = ref(false)
const loadedCellPricePlanRoomTypeIdsSignature = ref('')
const loadedCalendarManagementPriceRangeSignature = ref('')
const calendarManagementPriceMap = ref<Map<string, number>>(new Map())
const DEFAULT_SORT_ORDER = 999999
const roomTypeIdMap = ref<Map<string, number>>(new Map())
const roomTypeSortOrderMap = ref<Record<number, number>>({})
const roomSortOrderMap = ref<Record<number, number>>({})
const roomGroupSortOrderMap = ref<Record<number, number>>({})
const roomToGroupSortOrderMap = ref<Map<number, number>>(new Map())
const roomIdToGroupIdsMap = ref<Map<number, number[]>>(new Map())
const CELL_PRICE_VISIBLE_STORAGE_KEY = 'room-status-calendar.show-cell-default-price'
const CELL_PRICE_SOURCE_STORAGE_KEY = 'room-status-calendar.cell-price-display-source'

// 批量操作相关
const showBatchDialog = ref(false)
const batchDialogTitle = ref('')
const batchAction = ref('')
const batchMode = ref(false)
const selectedCells = ref<Set<string>>(new Set()) // 存储选中的单元格key: roomId-date
const quickActionDateRange = ref<{ startDate: string; endDate: string } | null>(null)
const isDraggingCellSelection = ref(false)
const dragSelectionRoomId = ref<number | null>(null)
const dragSelectionStartDate = ref('')
const dragSelectionEndDate = ref('')
const dragSelectionRoom = ref<CalendarRoomData | null>(null)
const dragSelectionTriggerRect = ref<DOMRect | null>(null)
const dragSelectionOriginCells = ref<Set<string>>(new Set())
const dragSelectionMoved = ref(false)
const suppressNextCellClick = ref(false)

type BatchActionType = 'dirty' | 'clean' | 'close' | 'open' | ''
type BatchWeekMode = 'all' | 'weekday' | 'weekend'

interface BatchRoomItem {
  roomId: number
  roomNumber: string
  roomType: string
}

interface BatchRoomGroup {
  roomType: string
  rooms: BatchRoomItem[]
}

const batchSearchKeyword = ref('')
const batchSelectedRoomIds = ref<number[]>([])
const batchExpandedRoomTypes = ref<string[]>([])

// 房间折叠相关
const isRoomCollapsed = ref(false)

// 关房弹窗相关
const showCloseRoomDialog = ref(false)
const closeRoomData = ref<{ room: CalendarRoomData | null; date: string }>({ room: null, date: '' })
const closeRoomForm = ref({
  type: 'stop', // stop: 停用房, maintenance: 维修房, retain: 保留房
  startDate: '',
  endDate: '',
  remark: '',
})

// 批量关房详细设置弹窗相关
const showBatchCloseRoomDialog = ref(false)
const batchCloseSelectedRoomIds = ref<number[]>([])
const batchCloseForm = ref({
  type: 'stop', // stop: 停用房, maintenance: 维修房, retain: 保留房
  startDate: '',
  endDate: '',
  remark: '',
  weekMode: 'all' as BatchWeekMode,
})

// 房间状态扩展（脏房、停用等）
const roomExtraStatus = ref<
  Map<
    string,
    {
      isDirty: boolean
      isClosed: boolean
      closeType: string
    }
  >
>(new Map())

// 房间号脏房状态（整个房间的脏房状态）
const roomNumberDirtyStatus = ref<Map<number, boolean>>(new Map())

const getRoomTypeSortOrder = (roomTypeName: string) => {
  const roomTypeId = roomTypeIdMap.value.get(roomTypeName)
  if (!roomTypeId) {
    return DEFAULT_SORT_ORDER
  }
  return roomTypeSortOrderMap.value[roomTypeId] ?? DEFAULT_SORT_ORDER
}

const getRoomSortOrder = (roomId: number) => {
  return roomSortOrderMap.value[roomId] ?? DEFAULT_SORT_ORDER
}

const getRoomGroupSortOrder = (roomId: number) => {
  return roomToGroupSortOrderMap.value.get(roomId) ?? DEFAULT_SORT_ORDER
}

const getRoomTypeDefaultPriceValue = (roomTypeName: string) => {
  return roomTypeDefaultPriceMap.value.get(roomTypeName) ?? 0
}

const calendarCellPricePlanOptions = computed(() => {
  const unique = new Map<number, { id: number; name: string }>()
  calendarRoomTypePricePlanMappings.value.forEach((mapping) => {
    const planId = Number(mapping.pricePlanId || mapping.pricePlan?.id || 0)
    if (!planId || unique.has(planId)) {
      return
    }
    unique.set(planId, {
      id: planId,
      name: mapping.pricePlan?.name || `计划${planId}`,
    })
  })
  return Array.from(unique.values()).sort((a, b) => a.name.localeCompare(b.name, 'zh-CN'))
})

const calendarCellPricePlanMappingMap = computed(() => {
  const mappingMap = new Map<string, RoomTypePricePlanDTO>()
  calendarRoomTypePricePlanMappings.value.forEach((mapping) => {
    const roomTypeId = Number(mapping.roomTypeId || mapping.roomType?.id || 0)
    const planId = Number(mapping.pricePlanId || mapping.pricePlan?.id || 0)
    if (!roomTypeId || !planId) {
      return
    }
    const key = `${roomTypeId}-${planId}`
    if (!mappingMap.has(key)) {
      mappingMap.set(key, mapping)
    }
  })
  return mappingMap
})

const getSelectedCellPricePlanId = () => {
  if (!cellPriceDisplaySource.value.startsWith('plan:')) {
    return null
  }
  const planId = Number(cellPriceDisplaySource.value.slice(5))
  return Number.isFinite(planId) && planId > 0 ? planId : null
}

const getCellPriceSourceLabel = () => {
  if (cellPriceDisplaySource.value === 'default') {
    return '默认价格'
  }
  const selectedPlanId = getSelectedCellPricePlanId()
  if (!selectedPlanId) {
    return '价格计划'
  }
  const selectedPlan = calendarCellPricePlanOptions.value.find((item) => item.id === selectedPlanId)
  return selectedPlan?.name || '价格计划'
}

const getRoomTypeIdsSignature = () => {
  const roomTypeIds = Array.from(new Set(Array.from(roomTypeIdMap.value.values())))
    .map((id) => Number(id))
    .filter((id) => id > 0)
    .sort((a, b) => a - b)
  return roomTypeIds.join(',')
}

const buildCalendarManagementPriceKey = (roomTypeId: number, pricePlanId: number, date: string) => {
  return `${roomTypeId}-${pricePlanId}-${date}`
}

const resetCalendarManagementPriceCache = () => {
  calendarManagementPriceMap.value = new Map()
  loadedCalendarManagementPriceRangeSignature.value = ''
}

const loadCalendarManagementPrices = async (force = false) => {
  const selectedPlanId = getSelectedCellPricePlanId()
  if (!showCellDefaultPrice.value || !selectedPlanId) {
    resetCalendarManagementPriceCache()
    return
  }

  const roomTypeIds = Array.from(new Set(Array.from(roomTypeIdMap.value.values())))
    .map((id) => Number(id))
    .filter((id) => id > 0)
  if (roomTypeIds.length === 0) {
    resetCalendarManagementPriceCache()
    return
  }

  const [startDate, endDate] = dateRange.value
  if (!startDate || !endDate) {
    resetCalendarManagementPriceCache()
    return
  }

  const roomTypeIdsSignature = getRoomTypeIdsSignature()
  const rangeSignature = `${roomTypeIdsSignature}|${selectedPlanId}|${startDate}|${endDate}`
  if (!force && loadedCalendarManagementPriceRangeSignature.value === rangeSignature) {
    return
  }

  loadingCalendarManagementPrices.value = true
  try {
    const response = await getRoomPriceManagementData(startDate, endDate)
    const managementRows = Array.isArray(response)
      ? response
      : Array.isArray((response as { data?: unknown }).data)
        ? ((response as { data: RoomPriceManagementDTO[] }).data ?? [])
        : []

    const roomTypeIdSet = new Set(roomTypeIds)
    const nextPriceMap = new Map<string, number>()
    managementRows.forEach((item) => {
      const roomTypeId = Number(item.roomTypeId || 0)
      const planId = Number(item.pricePlanId || 0)
      const priceDate = String(item.priceDate || '')
      const price = Number(item.price || 0)

      if (!roomTypeIdSet.has(roomTypeId) || !planId || !priceDate) {
        return
      }
      if (planId !== selectedPlanId) {
        return
      }
      if (!Number.isFinite(price) || price <= 0) {
        return
      }

      nextPriceMap.set(buildCalendarManagementPriceKey(roomTypeId, planId, priceDate), price)
    })

    calendarManagementPriceMap.value = nextPriceMap
    loadedCalendarManagementPriceRangeSignature.value = rangeSignature
  } catch (error) {
    console.error('加载房价管理价格失败:', error)
    resetCalendarManagementPriceCache()
  } finally {
    loadingCalendarManagementPrices.value = false
  }
}

const loadCalendarPricePlanOptions = async (force = false) => {
  const roomTypeIds = Array.from(new Set(Array.from(roomTypeIdMap.value.values())))
    .map((id) => Number(id))
    .filter((id) => id > 0)

  if (roomTypeIds.length === 0) {
    calendarRoomTypePricePlanMappings.value = []
    loadedCellPricePlanRoomTypeIdsSignature.value = ''
    resetCalendarManagementPriceCache()
    return
  }

  const roomTypeIdsSignature = getRoomTypeIdsSignature()
  if (!force && loadedCellPricePlanRoomTypeIdsSignature.value === roomTypeIdsSignature) {
    return
  }

  loadingCellPricePlanOptions.value = true
  try {
    const responses = await Promise.all(
      roomTypeIds.map(async (roomTypeId) => {
        try {
          const response = await getPricePlansByRoomType(roomTypeId)
          const mappings = Array.isArray(response)
            ? response
            : Array.isArray((response as { data?: unknown }).data)
              ? ((response as { data: RoomTypePricePlanDTO[] }).data ?? [])
              : []
          return mappings.map((item) => ({
            ...item,
            roomTypeId: Number(item.roomTypeId || item.roomType?.id || roomTypeId),
          }))
        } catch (error) {
          console.error(`加载房型${roomTypeId}价格计划失败:`, error)
          return []
        }
      }),
    )

    const uniqueMappingMap = new Map<string, RoomTypePricePlanDTO>()
    responses.flat().forEach((mapping) => {
      const roomTypeId = Number(mapping.roomTypeId || mapping.roomType?.id || 0)
      const planId = Number(mapping.pricePlanId || mapping.pricePlan?.id || 0)
      if (!roomTypeId || !planId) {
        return
      }
      const key = `${roomTypeId}-${planId}`
      if (!uniqueMappingMap.has(key)) {
        uniqueMappingMap.set(key, mapping)
      }
    })

    calendarRoomTypePricePlanMappings.value = Array.from(uniqueMappingMap.values())
    loadedCellPricePlanRoomTypeIdsSignature.value = roomTypeIdsSignature
  } finally {
    loadingCellPricePlanOptions.value = false
  }
}

const getCellDisplayPriceValue = (roomData: CalendarRoomData, dailyStatus: DailyRoomStatus) => {
  if (!showCellDefaultPrice.value || isRoomCollapsed.value) {
    return null
  }
  if (dailyStatus.reservation) {
    return null
  }
  const roomStatus = getRoomExtraStatus(roomData.roomId, dailyStatus.date)
  if (roomStatus.isClosed) {
    return null
  }

  if (cellPriceDisplaySource.value === 'default') {
    const defaultPrice = getRoomTypeDefaultPriceValue(roomData.roomType)
    return defaultPrice > 0 ? defaultPrice : null
  }

  const selectedPlanId = getSelectedCellPricePlanId()
  if (!selectedPlanId) {
    return null
  }
  const roomTypeId = roomTypeIdMap.value.get(roomData.roomType)
  if (!roomTypeId) {
    return null
  }
  const selectedMapping = calendarCellPricePlanMappingMap.value.get(`${roomTypeId}-${selectedPlanId}`)
  if (!selectedMapping) {
    return null
  }
  const managementPrice = calendarManagementPriceMap.value.get(
    buildCalendarManagementPriceKey(roomTypeId, selectedPlanId, dailyStatus.date),
  )
  if (managementPrice == null) {
    return null
  }
  return Number.isFinite(Number(managementPrice)) && Number(managementPrice) > 0
    ? Number(managementPrice)
    : null
}

const getCellDisplayPriceText = (roomData: CalendarRoomData, dailyStatus: DailyRoomStatus) => {
  const priceValue = getCellDisplayPriceValue(roomData, dailyStatus)
  if (priceValue === null) {
    return ''
  }
  return Number(priceValue).toFixed(2)
}

const shouldDisplayCellDefaultPrice = (roomData: CalendarRoomData, dailyStatus: DailyRoomStatus) => {
  return getCellDisplayPriceValue(roomData, dailyStatus) !== null
}

const getCellDefaultPriceTooltip = (roomData: CalendarRoomData, dailyStatus: DailyRoomStatus) => {
  const priceValue = getCellDisplayPriceValue(roomData, dailyStatus)
  if (priceValue === null) {
    return ''
  }
  return `${getCellPriceSourceLabel()}: ¥${Number(priceValue).toFixed(2)}`
}

const sortCalendarRooms = (rooms: CalendarRoomData[]) => {
  return [...rooms].sort((a, b) => {
    const roomGroupOrderDiff = getRoomGroupSortOrder(a.roomId) - getRoomGroupSortOrder(b.roomId)
    if (roomGroupOrderDiff !== 0) {
      return roomGroupOrderDiff
    }

    const roomTypeOrderDiff = getRoomTypeSortOrder(a.roomType) - getRoomTypeSortOrder(b.roomType)
    if (roomTypeOrderDiff !== 0) {
      return roomTypeOrderDiff
    }

    const roomOrderDiff = getRoomSortOrder(a.roomId) - getRoomSortOrder(b.roomId)
    if (roomOrderDiff !== 0) {
      return roomOrderDiff
    }

    const roomTypeNameDiff = a.roomType.localeCompare(b.roomType, 'zh-CN')
    if (roomTypeNameDiff !== 0) {
      return roomTypeNameDiff
    }
    return a.roomNumber.localeCompare(b.roomNumber, 'zh-CN')
  })
}

const applyCalendarRoomSorting = () => {
  if (!calendarData.value.rooms || calendarData.value.rooms.length === 0) {
    return
  }
  calendarData.value.rooms = sortCalendarRooms(calendarData.value.rooms)
}

const loadSortOrderMaps = async () => {
  if (!userStore.currentUser?.id) {
    roomTypeSortOrderMap.value = {}
    roomSortOrderMap.value = {}
    roomGroupSortOrderMap.value = {}
    roomToGroupSortOrderMap.value = new Map()
    return
  }

  try {
    const [roomTypeSortResponse, roomSortResponse, roomGroupSortResponse] = await Promise.all([
      getSortOrderMap(userStore.currentUser.id, 'ROOM_TYPE'),
      getSortOrderMap(userStore.currentUser.id, 'ROOM'),
      getSortOrderMap(userStore.currentUser.id, 'GROUP'),
    ])

    roomTypeSortOrderMap.value =
      roomTypeSortResponse.success && roomTypeSortResponse.data ? roomTypeSortResponse.data : {}
    roomSortOrderMap.value = roomSortResponse.success && roomSortResponse.data ? roomSortResponse.data : {}
    roomGroupSortOrderMap.value =
      roomGroupSortResponse.success && roomGroupSortResponse.data ? roomGroupSortResponse.data : {}

    const roomToGroupSortOrder = new Map<number, number>()
    const roomIdToGroupIds = new Map<number, number[]>()
    const roomGroupsResponse = await getAllRoomGroups()
    if (roomGroupsResponse.success && Array.isArray(roomGroupsResponse.data) && roomGroupsResponse.data.length > 0) {
      const roomGroups = roomGroupsResponse.data.filter(
        (group: RoomGroupDTO) => typeof group.id === 'number',
      ) as Array<RoomGroupDTO & { id: number }>
      filterOptions.value.roomGroups = roomGroups.map((group) => ({
        id: group.id,
        name: group.name,
      }))

      const groupMemberPairs = await Promise.all(
        roomGroups.map(async (group) => {
          try {
            const membersResponse = await getGroupMembers(group.id)
            if (!membersResponse.success || !Array.isArray(membersResponse.data)) {
              return {
                groupId: group.id,
                members: [] as RoomGroupMemberDTO[],
              }
            }
            return {
              groupId: group.id,
              members: membersResponse.data,
            }
          } catch (error) {
            console.error(`加载分组成员失败, groupId=${group.id}:`, error)
            return {
              groupId: group.id,
              members: [] as RoomGroupMemberDTO[],
            }
          }
        }),
      )

      groupMemberPairs.forEach(({ groupId, members }) => {
        const currentGroupSortOrder = roomGroupSortOrderMap.value[groupId] ?? DEFAULT_SORT_ORDER
        members.forEach((member) => {
          const roomId = Number(member.roomId)
          if (!roomId) {
            return
          }
          const currentGroupIds = roomIdToGroupIds.get(roomId) || []
          if (!currentGroupIds.includes(groupId)) {
            currentGroupIds.push(groupId)
            roomIdToGroupIds.set(roomId, currentGroupIds)
          }
          const previousSortOrder = roomToGroupSortOrder.get(roomId)
          if (previousSortOrder === undefined || currentGroupSortOrder < previousSortOrder) {
            roomToGroupSortOrder.set(roomId, currentGroupSortOrder)
          }
        })
      })
    } else {
      filterOptions.value.roomGroups = []
    }
    roomToGroupSortOrderMap.value = roomToGroupSortOrder
    roomIdToGroupIdsMap.value = roomIdToGroupIds
  } catch (error) {
    console.error('加载排序配置失败:', error)
    roomTypeSortOrderMap.value = {}
    roomSortOrderMap.value = {}
    roomGroupSortOrderMap.value = {}
    roomToGroupSortOrderMap.value = new Map()
    roomIdToGroupIdsMap.value = new Map()
    filterOptions.value.roomGroups = []
  }
}

const batchAllRooms = computed<BatchRoomItem[]>(() => {
  const roomMap = new Map<number, BatchRoomItem>()
  filteredRooms.value.forEach((room) => {
    roomMap.set(room.roomId, {
      roomId: room.roomId,
      roomNumber: room.roomNumber,
      roomType: room.roomType,
    })
  })
  return Array.from(roomMap.values())
})

const batchRoomsByType = computed(() => {
  const typeMap = new Map<string, BatchRoomItem[]>()
  batchAllRooms.value.forEach((room) => {
    if (!typeMap.has(room.roomType)) {
      typeMap.set(room.roomType, [])
    }
    typeMap.get(room.roomType)?.push(room)
  })
  return typeMap
})

const batchRoomLookup = computed(() => {
  const roomMap = new Map<number, BatchRoomItem>()
  calendarData.value.rooms.forEach((room) => {
    roomMap.set(room.roomId, {
      roomId: room.roomId,
      roomNumber: room.roomNumber,
      roomType: room.roomType,
    })
  })
  return roomMap
})

const batchSelectedRoomIdSet = computed(() => new Set(batchSelectedRoomIds.value))

const batchRoomGroups = computed<BatchRoomGroup[]>(() => {
  return Array.from(batchRoomsByType.value.entries()).map(([roomType, rooms]) => ({
    roomType,
    rooms,
  }))
})

const batchDisplayGroups = computed<BatchRoomGroup[]>(() => {
  const keyword = batchSearchKeyword.value.trim().toLowerCase()
  if (!keyword) {
    return batchRoomGroups.value
  }

  return batchRoomGroups.value
    .map((group) => {
      const roomTypeMatched = group.roomType.toLowerCase().includes(keyword)
      const rooms = roomTypeMatched
        ? group.rooms
        : group.rooms.filter((room) => room.roomNumber.toLowerCase().includes(keyword))
      if (rooms.length === 0) {
        return null
      }
      return {
        roomType: group.roomType,
        rooms,
      }
    })
    .filter((group): group is BatchRoomGroup => !!group)
})

const batchSelectedRooms = computed(() => {
  return batchAllRooms.value.filter((room) => batchSelectedRoomIdSet.value.has(room.roomId))
})

const batchCloseSelectedRooms = computed(() => {
  return batchCloseSelectedRoomIds.value
    .map((roomId) => batchRoomLookup.value.get(roomId))
    .filter((room): room is BatchRoomItem => !!room)
})

const batchTotalCount = computed(() => batchAllRooms.value.length)
const batchSelectedCount = computed(() => batchSelectedRoomIds.value.length)

const batchSelectAll = computed({
  get: () => batchTotalCount.value > 0 && batchSelectedCount.value === batchTotalCount.value,
  set: (checked: boolean) => {
    if (checked) {
      batchSelectedRoomIds.value = batchAllRooms.value.map((room) => room.roomId)
      return
    }
    batchSelectedRoomIds.value = []
  },
})

const batchSelectIndeterminate = computed(() => {
  return batchSelectedCount.value > 0 && batchSelectedCount.value < batchTotalCount.value
})

const isBatchRoomAction = computed(() => batchAction.value === 'close' || batchAction.value === 'open')

const batchDateSelectorText = computed(() => {
  const modeTextMap: Record<BatchWeekMode, string> = {
    all: '全部日期',
    weekday: '适用工作日',
    weekend: '适用周末',
  }
  return modeTextMap[batchCloseForm.value.weekMode]
})

// 计算属性
const loading = ref(false)
const calendarData = ref<{
  dateRange: {
    startDate: string
    endDate: string
  }
  rooms: CalendarRoomData[]
}>({
  dateRange: {
    startDate: '',
    endDate: '',
  },
  rooms: [],
})

const CALENDAR_DAYS_BEFORE_BASE = 2
const CALENDAR_VISIBLE_MONTHS = 2
const CALENDAR_NAVIGATION_STEP_DAYS = 30

const buildVisibleDateRangeFromBase = (baseDateValue: string): [string, string] => {
  const baseDate = parseYmdDate(baseDateValue)

  // 计算起始日期：基准日前2天
  const startDate = new Date(baseDate)
  startDate.setDate(startDate.getDate() - CALENDAR_DAYS_BEFORE_BASE)

  // 计算结束日期：从起始日期起连续两个月
  const endDate = new Date(startDate)
  endDate.setMonth(endDate.getMonth() + CALENDAR_VISIBLE_MONTHS)
  endDate.setDate(endDate.getDate() - 1)

  return [formatYmdDate(startDate), formatYmdDate(endDate)]
}

const visibleDateRange = ref<[string, string]>(buildVisibleDateRangeFromBase(currentBaseDate.value))

// 根据当前可视日期范围生成日期列
const dateColumns = computed(() => {
  const dates = []

  const startDate = parseYmdDate(visibleDateRange.value[0])
  const endDate = parseYmdDate(visibleDateRange.value[1])

  const totalDays = Math.floor(
    (endDate.getTime() - startDate.getTime()) / (1000 * 60 * 60 * 24),
  ) + 1

  for (let i = 0; i < totalDays; i++) {
    const current = new Date(startDate)
    current.setDate(startDate.getDate() + i)
    dates.push({
      date: formatYmdDate(current),
    })
  }

  return dates
})

// 计算当前日期范围（用于API调用）
const dateRange = computed(() => {
  return visibleDateRange.value
})

// 方法

const reloadCalendarForVisibleRange = async () => {
  // 先按当前日期范围重建房间日历骨架，再叠加后端房态
  await loadRoomTypesData()
  await loadRoomStatusCalendarData()
}

const previousWeek = () => {
  visibleDateRange.value = [
    shiftYmdDate(visibleDateRange.value[0], -CALENDAR_NAVIGATION_STEP_DAYS),
    shiftYmdDate(visibleDateRange.value[1], -CALENDAR_NAVIGATION_STEP_DAYS),
  ]
}

const nextWeek = () => {
  visibleDateRange.value = [
    shiftYmdDate(visibleDateRange.value[0], CALENDAR_NAVIGATION_STEP_DAYS),
    shiftYmdDate(visibleDateRange.value[1], CALENDAR_NAVIGATION_STEP_DAYS),
  ]
}

const goToToday = () => {
  const now = new Date()
  const year = now.getFullYear()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  const today = `${year}-${month}-${day}`
  visibleDateRange.value = buildVisibleDateRangeFromBase(today)
}


// 跳转到房型管理页面
const goToRoomTypeManagement = () => {
  router.push('/settings/room-type')
}

const goToMessages = async () => {
  const reservation = selectedReservation.value
  if (!reservation) {
    ElMessage.warning('订单数据加载中，请稍后重试')
    return
  }

  const query: Record<string, string> = {}
  if (reservation.id) {
    query.reservationId = String(reservation.id)
  }

  const orderNumber = String(reservation.orderNumber || '').trim()
  if (orderNumber) {
    query.orderNumber = orderNumber
  }

  const channelOrderNumber = String(reservation.channelOrderNumber || '').trim()
  if (channelOrderNumber) {
    query.channelOrderNumber = channelOrderNumber
  }

  const guestName = String(reservation.guestName || '').trim()
  if (guestName) {
    query.guestName = guestName
  }

  try {
    await router.push({
      name: 'Messages',
      query,
    })
  } catch (error) {
    console.error('跳转聊天页失败:', error)
    ElMessage.error('跳转聊天页失败')
  }
}

const formatMonthDay = (date: string) => {
  // 如果是今天，显示"今天"
  if (isToday(date)) {
    return '今天'
  }

  const d = new Date(date)
  return `${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

const getWeekday = (date: string) => {
  const weekdays = ['日', '一', '二', '三', '四', '五', '六']
  return weekdays[new Date(date).getDay()]
}

const getDayOfWeek = (date: string) => {
  return new Date(date).getDay() + 1
}

const getAvailableRoomsCount = (date: string) => {
  if (!calendarData.value || !calendarData.value.rooms) return 0

  // 计算当天可用房间数量
  let availableCount = 0
  calendarData.value.rooms.forEach((room) => {
    const dailyStatus = room.dailyStatus.find((ds) => ds.date === date)
    if (dailyStatus && dailyStatus.status === 'AVAILABLE') {
      availableCount++
    }
  })
  return availableCount
}

const isWeekend = (date: string) => {
  const day = new Date(date).getDay()
  return day === 0 || day === 6
}

const isToday = (date: string) => {
  return date === new Date().toISOString().split('T')[0]
}

// 判断日期是否在今天之前
const isBeforeToday = (date: string) => {
  const today = new Date().toISOString().split('T')[0]
  return date < today
}

// 判断日期是否在今天之后
const isAfterToday = (date: string) => {
  const today = new Date().toISOString().split('T')[0]
  return date > today
}

// 获取订单类型文本
const getOrderTypeText = (date: string) => {
  if (isBeforeToday(date)) {
    return '补录'
  } else if (isAfterToday(date)) {
    return '预订'
  } else {
    return '入住'
  }
}

// 获取订单按钮类型
const getOrderButtonType = (date: string) => {
  if (isBeforeToday(date)) {
    return 'info' // 蓝色，表示补录
  } else if (isAfterToday(date)) {
    return 'primary' // 主色，表示预订
  } else {
    return 'success' // 绿色，表示可入住
  }
}

// 获取预订状态文本
const getReservationStatusText = (status: string) => {
  const statusMap: Record<string, string> = {
    CONFIRMED: '已预订',
    CHECKED_IN: '已入住',
    CHECKED_OUT: '已退房',
    CANCELLED: '已取消',
    NO_SHOW: '未到店',
    // 添加可能的其他格式
    confirmed: '已预订',
    checked_in: '已入住',
    checked_out: '已退房',
    cancelled: '已取消',
    no_show: '未到店',
  }

  return statusMap[status] || '已预订'
}

// 获取状态标签类型
const getStatusTagType = (status: string) => {
  const tagTypeMap: Record<string, string> = {
    CONFIRMED: 'warning', // 橙色 - 已预订
    CHECKED_IN: 'success', // 绿色 - 已入住
    CHECKED_OUT: 'info', // 灰色 - 已退房
    CANCELLED: 'danger', // 红色 - 已取消
    NO_SHOW: 'warning', // 橙色 - 未到店
    // 添加可能的其他格式
    confirmed: 'warning',
    checked_in: 'success',
    checked_out: 'info',
    cancelled: 'danger',
    no_show: 'warning',
  }
  return tagTypeMap[status] || 'info'
}

// 状态判断辅助函数
const isConfirmedStatus = (status: string) => {
  return status === 'CONFIRMED' || status === 'confirmed'
}

const isCheckedInStatus = (status: string) => {
  return status === 'CHECKED_IN' || status === 'checked_in'
}

const isCheckedOutStatus = (status: string) => {
  return status === 'CHECKED_OUT' || status === 'checked_out'
}

const isCancelledStatus = (status: string) => {
  return status === 'CANCELLED' || status === 'cancelled'
}

const isNoShowStatus = (status: string) => {
  return status === 'NO_SHOW' || status === 'no_show'
}

const loadCalendarData = async () => {
  await loadSortOrderMaps()

  // 加载房型数据
  await loadRoomTypesData()

  // 加载真实的房态日历数据
  await loadRoomStatusCalendarData()

  applyCalendarRoomSorting()

  // 初始化筛选选项
  initFilterOptions()
}

// 加载真实的房态日历数据
const loadRoomStatusCalendarData = async () => {
  try {
    loading.value = true

    const response = await getRoomStatusCalendar(dateRange.value[0], dateRange.value[1])
    console.log('loadRoomStatusCalendarData - 后端API响应:', response)
    console.log('loadRoomStatusCalendarData - response.data.rooms:', response.data?.rooms)

    if (response.success && response.data) {
      // 转换后端数据格式为前端需要的格式
      const transformedData = {
        dateRange: response.data.dateRange,
        rooms: response.data.rooms.map((room) => ({
          roomId: room.roomId,
          roomNumber: room.roomNumber,
          roomType: room.roomType,
          dailyStatus: room.dailyStatus.map((daily) => ({
            date: daily.date,
            status: daily.status as RoomStatus,
            closed: daily.closed || false,
            closeType: daily.closeType || '',
            closeRemark: daily.closeRemark || '',
                reservation: daily.reservation
                  ? {
                      id: daily.reservation.id,
                      guestName: daily.reservation.guestName,
                      channel: daily.reservation.channel,
                      checkIn: daily.reservation.checkIn || (daily.reservation as any).checkInDate,
                      checkOut: daily.reservation.checkOut || (daily.reservation as any).checkOutDate,
                      orderNumber: daily.reservation.orderNumber,
                      groupOrderNo: (daily.reservation as any).groupOrderNo || '',
                      specialRequests: (daily.reservation as any).specialRequests || '',
                      notes:
                        daily.reservation.notes ||
                        (daily.reservation as any).remark ||
                        '',
                      adults: 1,
                      children: 0,
                      totalAmount: 0,
                      status: ReservationStatus.CONFIRMED,
                    }
              : null,
          })),
        })),
      }

      // 只有在返回的房间数据不为空时才覆盖
      if (transformedData.rooms && transformedData.rooms.length > 0) {
        calendarData.value = transformedData
        applyCalendarRoomSorting()

        // 同步后端落库的关房信息到 UI overlay（roomExtraStatus）
        transformedData.rooms.forEach((room) => {
          room.dailyStatus.forEach((daily) => {
            const key = `${room.roomId}-${daily.date}`
            const current = roomExtraStatus.value.get(key) || {
              isDirty: false,
              isClosed: false,
              closeType: '',
            }
            roomExtraStatus.value.set(key, {
              ...current,
              isClosed: !!daily.closed,
              closeType: daily.closed ? daily.closeType || '' : '',
            })
          })
        })

        console.log('房态日历数据加载成功:', transformedData)
        console.log('transformedData.rooms数量:', transformedData.rooms?.length)
        console.log('calendarData.value.rooms数量:', calendarData.value.rooms?.length)
      } else {
        console.warn('后端返回的房间数据为空,保留已有的房型数据')
        console.log('当前calendarData.value.rooms数量:', calendarData.value.rooms?.length)
        // 不覆盖calendarData,保持使用loadRoomTypesData()加载的数据
      }
    } else {
      console.warn('API调用失败,保留已有数据。API返回:', response)
      // 如果API调用失败,保持使用已有数据
    }
  } catch (error) {
    console.error('加载房态日历数据失败:', error)
    ElMessage.warning('加载房态数据失败，使用模拟数据')
    // 发生错误时保持使用模拟数据
  } finally {
    loading.value = false
  }
}

const loadRoomTypesData = async () => {
  loading.value = true
  try {
    const response = (await request.get('/room-types/with-rooms')) as any
    if (response.success && response.data && response.data.length > 0) {
      // 将房型数据转换为房态日历格式
      const roomsData: any[] = []
      let roomIdCounter = 1
      const nextRoomTypeIdMap = new Map<string, number>()
      const nextRoomTypeDefaultPriceMap = new Map<string, number>()

      response.data.forEach((roomType: any) => {
        if (roomType?.name && roomType?.id) {
          nextRoomTypeIdMap.set(roomType.name, Number(roomType.id))
        }
        if (roomType?.name && roomType?.defaultPrice != null && !Number.isNaN(Number(roomType.defaultPrice))) {
          nextRoomTypeDefaultPriceMap.set(roomType.name, Number(roomType.defaultPrice))
        }
        if (roomType.rooms && roomType.rooms.length > 0) {
          roomType.rooms.forEach((room: any) => {
            // 生成日期状态数据
            const dailyStatus = []
            const startDate = new Date(dateRange.value[0])
            const endDate = new Date(dateRange.value[1])
            const current = new Date(startDate)

            while (current <= endDate) {
              const dateStr = current.toISOString().split('T')[0]
              dailyStatus.push({
                date: dateStr,
                status: RoomStatus.AVAILABLE,
                reservation: null,
              })
              current.setDate(current.getDate() + 1)
            }

            roomsData.push({
              roomId: room?.id ? Number(room.id) : roomIdCounter++,
              roomNumber: room.roomNumber,
              roomType: roomType.name,
              dailyStatus,
            })
          })
        }
      })
      roomTypeIdMap.value = nextRoomTypeIdMap
      roomTypeDefaultPriceMap.value = nextRoomTypeDefaultPriceMap
      loadedCellPricePlanRoomTypeIdsSignature.value = ''
      resetCalendarManagementPriceCache()
      if (showCellDefaultPrice.value) {
        void loadCalendarPricePlanOptions(true)
        if (cellPriceDisplaySource.value !== 'default') {
          void loadCalendarManagementPrices(true)
        }
      }

      // 更新房态数据
      calendarData.value = {
        dateRange: {
          startDate: dateRange.value[0],
          endDate: dateRange.value[1],
        },
        rooms: roomsData,
      }
      applyCalendarRoomSorting()

      console.log('加载房型数据成功，生成房间数:', roomsData.length)
    } else {
      console.log('暂无房型数据，使用模拟数据')
      // 如果没有房型数据，可以保持原有的模拟数据
    }
  } catch (error) {
    console.error('加载房型数据失败:', error)
    ElMessage.error('加载房态数据失败')
  } finally {
    loading.value = false
  }
}

const initFilterOptions = () => {
  // 获取所有房型
  console.log('initFilterOptions - calendarData.value.rooms:', calendarData.value.rooms)
  console.log('initFilterOptions - rooms数量:', calendarData.value.rooms?.length)
  const roomTypes = [...new Set(sortCalendarRooms(calendarData.value.rooms).map((room) => room.roomType))]
  console.log('initFilterOptions - 提取的房型:', roomTypes)
  filterOptions.value.roomTypes = roomTypes
  filterOptions.value.selectedRoomTypes = [...roomTypes] // 默认全选
  filterOptions.value.selectedRoomGroupIds = []
  console.log('initFilterOptions - selectedRoomTypes:', filterOptions.value.selectedRoomTypes)
}

const toggleFilterSidebar = () => {
  showFilterSidebar.value = !showFilterSidebar.value
}

const applyFilters = () => {
  // 这里可以添加筛选逻辑
  console.log('应用筛选:', filterOptions.value.selectedRoomTypes)
  showFilterSidebar.value = false
}

const resetFilters = () => {
  filterOptions.value.selectedRoomTypes = [...filterOptions.value.roomTypes]
  filterOptions.value.selectedRoomGroupIds = []
}

const handleSelectAll = (checked: boolean) => {
  if (checked) {
    filterOptions.value.selectedRoomTypes = [...filterOptions.value.roomTypes]
  } else {
    filterOptions.value.selectedRoomTypes = []
  }
}

// 计算筛选后的房间数据
const filteredRooms = computed(() => {
  console.log('filteredRooms computed - calendarData.value.rooms数量:', calendarData.value.rooms?.length)
  console.log('filteredRooms computed - selectedRoomTypes:', filterOptions.value.selectedRoomTypes)

  const hasRoomTypeFilter = filterOptions.value.selectedRoomTypes.length > 0
  const hasRoomGroupFilter = filterOptions.value.selectedRoomGroupIds.length > 0

  const filtered = calendarData.value.rooms.filter((room) => {
    const matchedRoomType = !hasRoomTypeFilter || filterOptions.value.selectedRoomTypes.includes(room.roomType)
    const matchedRoomGroup =
      !hasRoomGroupFilter ||
      (roomIdToGroupIdsMap.value.get(room.roomId) || []).some((groupId) =>
        filterOptions.value.selectedRoomGroupIds.includes(groupId),
      )
    return matchedRoomType && matchedRoomGroup
  })
  console.log('filteredRooms computed - 筛选后房间数量:', filtered.length)
  return sortCalendarRooms(filtered)
})

// 批量操作相关方法
// 处理批量置脏/净命令
const handleBatchCleanCommand = (command: string) => {
  batchDialogTitle.value = command === 'dirty' ? '批量置脏' : '批量置净'
  batchAction.value = command as BatchActionType
  batchMode.value = true
  batchSearchKeyword.value = ''
  batchSelectedRoomIds.value = []
  batchExpandedRoomTypes.value = batchRoomGroups.value.map((group) => group.roomType)
  selectedCells.value.clear()
  showBatchDialog.value = true
}

// 处理批量开关房命令
const handleBatchRoomCommand = (command: string) => {
  batchDialogTitle.value = command === 'open' ? '批量开房' : '批量关房'
  batchAction.value = command as BatchActionType
  batchMode.value = true
  batchSearchKeyword.value = ''
  batchSelectedRoomIds.value = []
  batchExpandedRoomTypes.value = batchRoomGroups.value.map((group) => group.roomType)
  selectedCells.value.clear()
  showBatchDialog.value = true
}

// 检查是否被选中
const isCellSelected = (roomId: number, date: string) => {
  return selectedCells.value.has(getCellKey(roomId, date))
}

const normalizeDateOnly = (value: string) => (value ? value.split('T')[0] : '')

const getDateRangeOrdered = (startDate: string, endDate: string) => {
  const start = parseDateOnly(startDate)
  const end = parseDateOnly(endDate)
  return start <= end ? { start, end } : { start: end, end: start }
}

const getDateRangeStrings = (startDate: string, endDate: string) => {
  const { start, end } = getDateRangeOrdered(startDate, endDate)
  const result: string[] = []
  const cursor = new Date(start)
  while (cursor <= end) {
    result.push(formatDateOnly(cursor))
    cursor.setDate(cursor.getDate() + 1)
  }
  return result
}

const addDays = (date: string, days: number) => {
  const parsedDate = parseDateOnly(date)
  parsedDate.setDate(parsedDate.getDate() + days)
  return formatDateOnly(parsedDate)
}

const getCellKey = (roomId: number, date: string) => `${roomId}-${date}`

const parseCellKey = (cellKey: string) => {
  const [roomIdText, ...dateParts] = cellKey.split('-')
  return {
    roomId: Number(roomIdText),
    date: dateParts.join('-'),
  }
}

const getSelectedDatesByRoomId = (roomId: number) => {
  const selectedDates: string[] = []
  selectedCells.value.forEach((cellKey) => {
    const parsed = parseCellKey(cellKey)
    if (parsed.roomId === roomId && parsed.date) {
      selectedDates.push(parsed.date)
    }
  })
  return selectedDates.sort()
}

const isDatesContinuous = (dates: string[]) => {
  if (dates.length <= 1) return true
  for (let index = 1; index < dates.length; index += 1) {
    if (addDays(dates[index - 1], 1) !== dates[index]) {
      return false
    }
  }
  return true
}

const isSelectionContinuousForRoom = (roomId: number) => {
  return isDatesContinuous(getSelectedDatesByRoomId(roomId))
}

const isSelectedStartCell = (roomId: number, date: string) => {
  if (!isCellSelected(roomId, date)) return false
  return !isCellSelected(roomId, addDays(date, -1))
}

const isSelectedEndCell = (roomId: number, date: string) => {
  if (!isCellSelected(roomId, date)) return false
  return !isCellSelected(roomId, addDays(date, 1))
}

const isSelectedMiddleCell = (roomId: number, date: string) => {
  return (
    isCellSelected(roomId, date) &&
    !isSelectedStartCell(roomId, date) &&
    !isSelectedEndCell(roomId, date)
  )
}

const clearDragSelection = () => {
  if (batchMode.value) return
  selectedCells.value.clear()
  quickActionDateRange.value = null
  dragSelectionRoomId.value = null
  dragSelectionStartDate.value = ''
  dragSelectionEndDate.value = ''
  dragSelectionRoom.value = null
  dragSelectionTriggerRect.value = null
  dragSelectionOriginCells.value = new Set()
  dragSelectionMoved.value = false
}

const clearRoomChangeDragState = () => {
  draggingReservationContext.value = null
  roomChangeDropTargetKey.value = ''
}

const canDragReservationAtCell = (dailyStatus: DailyRoomStatus) => {
  if (isRoomCollapsed.value || batchMode.value) {
    return false
  }
  if (!dailyStatus.reservation || !isReservationStartCell(dailyStatus)) {
    return false
  }
  const reservation = dailyStatus.reservation as Record<string, any>
  const reservationId = Number(reservation.id || 0)
  if (!reservationId) {
    return false
  }
  const status = String(reservation.status || '').toUpperCase()
  return (
    status === 'CONFIRMED' ||
    status === 'REQUESTED' ||
    status === 'CHECKED_IN' ||
    status === 'RESERVED' ||
    status === 'NEW'
  )
}

const isDateInReservationStayRange = (targetDate: string, checkInDate: string, checkOutDate: string) => {
  if (!targetDate || !checkInDate || !checkOutDate) {
    return false
  }
  const target = parseDateOnly(targetDate).getTime()
  const start = parseDateOnly(checkInDate).getTime()
  const end = parseDateOnly(checkOutDate).getTime()
  return target >= start && target < end
}

const canDropReservationToCell = (roomData: CalendarRoomData, dailyStatus: DailyRoomStatus) => {
  const context = draggingReservationContext.value
  if (!context || isRoomCollapsed.value || batchMode.value) {
    return false
  }
  if (roomData.roomId === context.sourceRoomId) {
    return false
  }
  if (dailyStatus.reservation) {
    return false
  }
  const roomStatus = getRoomExtraStatus(roomData.roomId, dailyStatus.date)
  if (roomStatus.isClosed) {
    return false
  }
  return isDateInReservationStayRange(
    normalizeDateOnly(dailyStatus.date),
    context.checkInDate,
    context.checkOutDate,
  )
}

const isRoomChangeDropTarget = (roomId: number, date: string) => {
  return roomChangeDropTargetKey.value === getCellKey(roomId, date)
}

const onReservationDragStart = (
  event: DragEvent,
  roomData: CalendarRoomData,
  dailyStatus: DailyRoomStatus,
) => {
  if (!canDragReservationAtCell(dailyStatus)) {
    event.preventDefault()
    return
  }
  const reservation = dailyStatus.reservation as Record<string, any>
  const reservationId = Number(reservation.id || 0)
  const checkInDate = normalizeDateOnly(getReservationDateValue(reservation, 'checkIn'))
  const checkOutDate = normalizeDateOnly(getReservationDateValue(reservation, 'checkOut'))
  if (!reservationId || !checkInDate || !checkOutDate) {
    event.preventDefault()
    return
  }

  draggingReservationContext.value = {
    reservationId,
    sourceRoomId: roomData.roomId,
    sourceRoomNumber: roomData.roomNumber || String(roomData.roomId),
    guestName: String(reservation.guestName || ''),
    checkInDate,
    checkOutDate,
  }
  roomChangeDropTargetKey.value = ''
  suppressNextCellClick.value = true

  if (event.dataTransfer) {
    event.dataTransfer.effectAllowed = 'move'
    event.dataTransfer.setData('text/plain', String(reservationId))
  }
}

const onReservationDragEnd = () => {
  clearRoomChangeDragState()
}

const onStatusCellDragOver = (
  event: DragEvent,
  roomData: CalendarRoomData,
  dailyStatus: DailyRoomStatus,
) => {
  if (!draggingReservationContext.value) {
    return
  }
  if (!canDropReservationToCell(roomData, dailyStatus)) {
    roomChangeDropTargetKey.value = ''
    return
  }
  event.preventDefault()
  if (event.dataTransfer) {
    event.dataTransfer.dropEffect = 'move'
  }
  roomChangeDropTargetKey.value = getCellKey(roomData.roomId, dailyStatus.date)
}

const onStatusCellDrop = (
  event: DragEvent,
  roomData: CalendarRoomData,
  dailyStatus: DailyRoomStatus,
) => {
  event.preventDefault()
  suppressNextCellClick.value = true
  const context = draggingReservationContext.value
  if (!context) {
    return
  }

  if (!canDropReservationToCell(roomData, dailyStatus)) {
    clearRoomChangeDragState()
    return
  }

  pendingRoomChange.value = {
    reservationId: context.reservationId,
    targetRoomId: roomData.roomId,
    sourceRoomNumber: context.sourceRoomNumber,
    targetRoomNumber: roomData.roomNumber || String(roomData.roomId),
    guestName: context.guestName,
    checkInDate: context.checkInDate,
    checkOutDate: context.checkOutDate,
  }
  roomChangeUpdatePrice.value = false
  showRoomChangeConfirmDialog.value = true
  clearRoomChangeDragState()
}

const cancelRoomChangeConfirm = () => {
  showRoomChangeConfirmDialog.value = false
  roomChangeUpdatePrice.value = false
  pendingRoomChange.value = null
}

const buildReservationUpdateRequest = (
  reservation: ReservationDTO,
  roomId: number,
  totalAmount: number,
): CreateReservationRequest => {
  return {
    guestName: reservation.guestName || '',
    guestPhone: reservation.phone || '',
    guestIdCard: (reservation as any).guestIdCard || '',
    roomId,
    channelId: Number(reservation.channelId || 0),
    checkInDate: reservation.checkInDate,
    checkOutDate: reservation.checkOutDate,
    adults: Number((reservation as any).adults || 1),
    children: Number((reservation as any).children || 0),
    totalAmount,
    pricePlan: reservation.pricePlan || undefined,
    notes: reservation.notes || '',
  }
}

const updateReservationPriceAfterRoomChange = async (reservationId: number, targetRoomId: number) => {
  const reservationResp = await getReservationById(reservationId)
  if (!reservationResp.success || !reservationResp.data) {
    throw new Error(reservationResp.message || '获取最新订单信息失败')
  }
  const latestReservation = reservationResp.data
  if (!latestReservation.checkInDate || !latestReservation.checkOutDate) {
    throw new Error('订单缺少入住和退房日期，无法更新房价')
  }

  const roomTypeResp = await getRoomTypeByRoomId(targetRoomId)
  if (!roomTypeResp.success || !roomTypeResp.data) {
    throw new Error(roomTypeResp.message || '获取目标房型失败')
  }

  const newTotalAmount = calculateTotalPriceByDates(
    roomTypeResp.data,
    latestReservation.checkInDate,
    latestReservation.checkOutDate,
  )

  const updatePayload = buildReservationUpdateRequest(
    latestReservation,
    targetRoomId,
    Number(newTotalAmount || 0),
  )
  const updateResp = await updateReservation(reservationId, updatePayload)
  if (!updateResp.success) {
    throw new Error(updateResp.message || '更新房价失败')
  }
}

const confirmRoomChange = async () => {
  const pending = pendingRoomChange.value
  if (!pending) {
    return
  }

  roomChangeSubmitting.value = true
  try {
    const assignResp = await assignReservationRoom(pending.reservationId, pending.targetRoomId)
    if (!assignResp.success) {
      ElMessage.error(assignResp.message || '换房失败')
      return
    }

    let priceUpdated = false
    if (roomChangeUpdatePrice.value) {
      try {
        await updateReservationPriceAfterRoomChange(pending.reservationId, pending.targetRoomId)
        priceUpdated = true
      } catch (error: any) {
        console.error('换房后更新房价失败:', error)
        ElMessage.warning(error?.message || '换房成功，但房价更新失败')
      }
    }

    showRoomChangeConfirmDialog.value = false
    pendingRoomChange.value = null
    await loadRoomStatusCalendarData()

    if (roomChangeUpdatePrice.value) {
      ElMessage.success(priceUpdated ? '换房成功，房价已更新' : '换房成功')
      return
    }
    ElMessage.success('换房成功')
  } catch (error: any) {
    console.error('换房失败:', error)
    ElMessage.error(error?.message || '换房失败，请稍后重试')
  } finally {
    roomChangeSubmitting.value = false
    roomChangeUpdatePrice.value = false
    clearRoomChangeDragState()
  }
}

const canDragCreateReservation = (roomData: CalendarRoomData, dailyStatus: DailyRoomStatus) => {
  if (isRoomCollapsed.value || batchMode.value) return false
  if (dailyStatus.reservation) return false
  const roomStatus = getRoomExtraStatus(roomData.roomId, dailyStatus.date)
  return !roomStatus.isClosed
}

const updateDragSelectionRange = (
  roomData: CalendarRoomData,
  startDate: string,
  endDate: string,
) => {
  const dateStrings = getDateRangeStrings(startDate, endDate)
  const roomStatusByDate = new Map(roomData.dailyStatus.map((item) => [item.date, item]))
  const nextSelectedCells = new Set<string>()

  for (const dateString of dateStrings) {
    const daily = roomStatusByDate.get(dateString)
    if (!daily || !canDragCreateReservation(roomData, daily)) {
      break
    }
    nextSelectedCells.add(getCellKey(roomData.roomId, dateString))
  }

  selectedCells.value = nextSelectedCells

  const selectedDates = Array.from(nextSelectedCells)
    .map((cellKey) => cellKey.split('-').slice(1).join('-'))
    .sort()

  dragSelectionStartDate.value = selectedDates[0] || ''
  dragSelectionEndDate.value = selectedDates[selectedDates.length - 1] || ''
}

const beginDragSelection = (
  event: MouseEvent,
  roomData: CalendarRoomData,
  dailyStatus: DailyRoomStatus,
) => {
  if (!canDragCreateReservation(roomData, dailyStatus)) return

  closeAllPopups()
  quickActionDateRange.value = null
  isDraggingCellSelection.value = true
  dragSelectionMoved.value = false
  dragSelectionOriginCells.value = new Set(selectedCells.value)
  dragSelectionRoomId.value = roomData.roomId
  dragSelectionRoom.value = roomData
  dragSelectionStartDate.value = dailyStatus.date
  dragSelectionEndDate.value = dailyStatus.date
  dragSelectionTriggerRect.value = (event.currentTarget as HTMLElement).getBoundingClientRect()
  updateDragSelectionRange(roomData, dailyStatus.date, dailyStatus.date)
}

const updateDragSelection = (
  event: MouseEvent,
  roomData: CalendarRoomData,
  dailyStatus: DailyRoomStatus,
) => {
  if (!isDraggingCellSelection.value) return
  if (dragSelectionRoomId.value !== roomData.roomId) return

  if (dailyStatus.date !== dragSelectionEndDate.value) {
    dragSelectionMoved.value = true
  }
  dragSelectionTriggerRect.value = (event.currentTarget as HTMLElement).getBoundingClientRect()
  updateDragSelectionRange(roomData, dragSelectionStartDate.value || dailyStatus.date, dailyStatus.date)
}

const finishDragSelection = () => {
  if (!isDraggingCellSelection.value) return

  isDraggingCellSelection.value = false

  if (!dragSelectionRoom.value || !dragSelectionStartDate.value || !dragSelectionEndDate.value) {
    clearDragSelection()
    return
  }

  if (selectedCells.value.size === 0) {
    closeAllPopups()
    clearDragSelection()
    suppressNextCellClick.value = true
    return
  }

  // 单点不拖动：支持逐格追加/取消选择（同一房间）
  if (!dragSelectionMoved.value) {
    const roomId = dragSelectionRoomId.value
    if (roomId === null) {
      clearDragSelection()
      return
    }
    const clickedDate = dragSelectionStartDate.value
    const baseSet = new Set(dragSelectionOriginCells.value)
    const clickedCellKey = getCellKey(roomId, clickedDate)

    if (baseSet.size > 0) {
      const selectedRoomIds = new Set<number>()
      baseSet.forEach((cellKey) => {
        selectedRoomIds.add(parseCellKey(cellKey).roomId)
      })
      if (selectedRoomIds.size !== 1 || !selectedRoomIds.has(roomId)) {
        baseSet.clear()
      }
    }

    if (baseSet.has(clickedCellKey)) {
      baseSet.delete(clickedCellKey)
    } else {
      baseSet.add(clickedCellKey)
    }

    selectedCells.value = baseSet
    if (selectedCells.value.size === 0) {
      closeAllPopups()
      clearDragSelection()
      suppressNextCellClick.value = true
      return
    }

    const selectedDates = getSelectedDatesByRoomId(roomId)
    dragSelectionStartDate.value = selectedDates[0] || ''
    dragSelectionEndDate.value = selectedDates[selectedDates.length - 1] || ''
  }

  quickActionRoom.value = dragSelectionRoom.value
  quickActionDate.value = dragSelectionStartDate.value
  quickActionDateRange.value =
    dragSelectionStartDate.value && dragSelectionEndDate.value
      ? {
          startDate: dragSelectionStartDate.value,
          endDate: dragSelectionEndDate.value,
        }
      : null
  showQuickActions.value = true
  suppressNextCellClick.value = true

  const triggerRect = dragSelectionTriggerRect.value
  if (triggerRect) {
    void nextTick(() => {
      updateQuickActionPopupPosition(triggerRect)
    })
  }

  dragSelectionOriginCells.value = new Set()
  dragSelectionMoved.value = false
}

const onCellMouseDown = (
  event: MouseEvent,
  roomData: CalendarRoomData,
  dailyStatus: DailyRoomStatus,
) => {
  beginDragSelection(event, roomData, dailyStatus)
}

const onCellMouseUp = (
  event: MouseEvent,
  roomData: CalendarRoomData,
  dailyStatus: DailyRoomStatus,
) => {
  if (!isDraggingCellSelection.value) return
  updateDragSelection(event, roomData, dailyStatus)
  finishDragSelection()
}

const onCellMouseEnter = (
  event: MouseEvent,
  roomData: CalendarRoomData,
  dailyStatus: DailyRoomStatus,
) => {
  if (batchMode.value) return
  if (isDraggingCellSelection.value) {
    updateDragSelection(event, roomData, dailyStatus)
    return
  }
  onCellHover(event, dailyStatus, roomData)
}

const handleWindowMouseUp = () => {
  if (!isDraggingCellSelection.value) return
  finishDragSelection()
}

const getReservationNotesText = (reservation: Record<string, any> | null | undefined) => {
  if (!reservation) return ''
  const notesValue = reservation.notes ?? reservation.remark ?? ''
  return typeof notesValue === 'string' ? notesValue.trim() : ''
}

const hasReservationNotes = (dailyStatus: DailyRoomStatus) => {
  const reservation = dailyStatus.reservation as Record<string, any> | null | undefined
  return getReservationNotesText(reservation).length > 0
}

const isBatchRoomChecked = (roomId: number) => batchSelectedRoomIdSet.value.has(roomId)

const handleBatchRoomCheck = (roomId: number, checked: boolean) => {
  const selectedRoomSet = new Set(batchSelectedRoomIds.value)
  if (checked) {
    selectedRoomSet.add(roomId)
  } else {
    selectedRoomSet.delete(roomId)
  }
  batchSelectedRoomIds.value = Array.from(selectedRoomSet)
}

const isBatchTypeExpanded = (roomType: string) => {
  if (batchSearchKeyword.value.trim()) {
    return true
  }
  return batchExpandedRoomTypes.value.includes(roomType)
}

const toggleBatchTypeExpanded = (roomType: string) => {
  if (batchSearchKeyword.value.trim()) {
    return
  }
  if (batchExpandedRoomTypes.value.includes(roomType)) {
    batchExpandedRoomTypes.value = batchExpandedRoomTypes.value.filter((type) => type !== roomType)
    return
  }
  batchExpandedRoomTypes.value = [...batchExpandedRoomTypes.value, roomType]
}

const isBatchTypeChecked = (roomType: string) => {
  const rooms = batchRoomsByType.value.get(roomType) || []
  if (rooms.length === 0) {
    return false
  }
  return rooms.every((room) => batchSelectedRoomIdSet.value.has(room.roomId))
}

const isBatchTypeIndeterminate = (roomType: string) => {
  const rooms = batchRoomsByType.value.get(roomType) || []
  if (rooms.length === 0) {
    return false
  }
  const selectedCount = rooms.filter((room) => batchSelectedRoomIdSet.value.has(room.roomId)).length
  return selectedCount > 0 && selectedCount < rooms.length
}

const handleBatchTypeCheck = (roomType: string, checked: boolean) => {
  const rooms = batchRoomsByType.value.get(roomType) || []
  const selectedRoomSet = new Set(batchSelectedRoomIds.value)
  rooms.forEach((room) => {
    if (checked) {
      selectedRoomSet.add(room.roomId)
    } else {
      selectedRoomSet.delete(room.roomId)
    }
  })
  batchSelectedRoomIds.value = Array.from(selectedRoomSet)
}

const removeBatchSelectedRoom = (roomId: number) => {
  batchSelectedRoomIds.value = batchSelectedRoomIds.value.filter((id) => id !== roomId)
}

// 确认批量操作
const confirmBatchOperation = () => {
  if (batchSelectedRoomIds.value.length === 0) {
    ElMessage.warning('请先选择要操作的房间')
    return
  }

  if (batchAction.value === 'dirty' || batchAction.value === 'clean') {
    executeBatchCleanOperation(batchSelectedRoomIds.value)
  } else if (isBatchRoomAction.value) {
    batchCloseSelectedRoomIds.value = [...batchSelectedRoomIds.value]
    const today = new Date().toISOString().split('T')[0]
    batchCloseForm.value.type = 'stop'
    batchCloseForm.value.startDate = today
    batchCloseForm.value.endDate = today
    batchCloseForm.value.weekMode = 'all'
    batchCloseForm.value.remark = ''
    showBatchDialog.value = false
    showBatchCloseRoomDialog.value = true
    return
  }

  ElMessage.success(`${batchDialogTitle.value}操作已完成`)
  showBatchDialog.value = false
  batchMode.value = false
  selectedCells.value.clear()
  batchSelectedRoomIds.value = []
  batchExpandedRoomTypes.value = []
}

// 执行批量置脏/净操作
const executeBatchCleanOperation = (selectedRoomIds: number[]) => {
  const isDirty = batchAction.value === 'dirty'
  const nextStatusMap = new Map(roomNumberDirtyStatus.value)
  selectedRoomIds.forEach((roomId) => {
    if (isDirty) {
      nextStatusMap.set(roomId, true)
      return
    }
    nextStatusMap.delete(roomId)
  })
  roomNumberDirtyStatus.value = nextStatusMap
}

const removeBatchCloseRoom = (roomId: number) => {
  batchCloseSelectedRoomIds.value = batchCloseSelectedRoomIds.value.filter((id) => id !== roomId)
  batchSelectedRoomIds.value = [...batchCloseSelectedRoomIds.value]
}

// 取消批量操作
const cancelBatchOperation = () => {
  showBatchDialog.value = false
  batchMode.value = false
  selectedCells.value.clear()
  batchSelectedRoomIds.value = []
  batchExpandedRoomTypes.value = []
  batchSearchKeyword.value = ''
}

// 房间折叠功能
const toggleRoomCollapse = () => {
  isRoomCollapsed.value = !isRoomCollapsed.value
}

// 获取房间扩展状态
const getRoomExtraStatus = (roomId: number, date: string) => {
  const key = `${roomId}-${date}`
  return (
    roomExtraStatus.value.get(key) || {
      isDirty: false,
      isClosed: false,
      closeType: '',
    }
  )
}

// 获取房间号脏房状态
const getRoomNumberDirtyStatus = (roomId: number) => {
  return roomNumberDirtyStatus.value.get(roomId) || false
}

const dirtyHoverActionText = computed(() => {
  if (hoverDirtyRoomId.value === null) {
    return '转脏房'
  }
  return getRoomNumberDirtyStatus(hoverDirtyRoomId.value) ? '转净房' : '转脏房'
})

const getCollapsedCellStatus = (roomData: CalendarRoomData, dailyStatus: DailyRoomStatus) => {
  const roomStatus = getRoomExtraStatus(roomData.roomId, dailyStatus.date)
  const isFull =
    roomStatus.isClosed ||
    !!dailyStatus.reservation ||
    dailyStatus.status !== RoomStatus.AVAILABLE

  return {
    label: isFull ? '满房' : '1',
    isFull,
  }
}

const getFloatingSafePosition = (rect: DOMRect, panelWidth: number, panelHeight: number) => {
  const gap = 10
  const viewportPadding = 8
  const viewportWidth = window.innerWidth
  const viewportHeight = window.innerHeight

  let x = rect.right + gap
  if (x + panelWidth + viewportPadding > viewportWidth) {
    x = rect.left - panelWidth - gap
  }

  let y = rect.top
  if (y + panelHeight + viewportPadding > viewportHeight) {
    y = viewportHeight - panelHeight - viewportPadding
  }

  x = Math.max(viewportPadding, Math.min(x, viewportWidth - panelWidth - viewportPadding))
  y = Math.max(viewportPadding, Math.min(y, viewportHeight - panelHeight - viewportPadding))

  return { x, y }
}

const updateDirtyTooltipPosition = (rect: DOMRect) => {
  hoverCardPosition.value = getFloatingSafePosition(rect, 112, 40)
}

const updateHoverCardPositionByRect = (rect: DOMRect) => {
  const panelWidth = hoverCardRef.value?.offsetWidth ?? 340
  const panelHeight = hoverCardRef.value?.offsetHeight ?? 360
  hoverCardPosition.value = getFloatingSafePosition(rect, panelWidth, panelHeight)
}

const adjustHoverCardPosition = () => {
  if (!hoverCardAnchorRect.value) return
  updateHoverCardPositionByRect(hoverCardAnchorRect.value)
}

// 房间号悬停处理
const onRoomNumberHover = (event: MouseEvent, roomData: CalendarRoomData) => {
  // 只在展开状态下显示脏房提示
  if (!isRoomCollapsed.value) {
    hoverDirtyRoomId.value = roomData.roomId
    const target = (event.currentTarget as HTMLElement) || (event.target as HTMLElement)
    const rect = target.getBoundingClientRect()
    updateDirtyTooltipPosition(rect)
    showDirtyHover.value = true
    showHoverCard.value = false
  }
}

// 房间号点击处理
const onRoomNumberClick = (event: MouseEvent, roomData: CalendarRoomData) => {
  // 只在展开状态下处理点击
  if (!isRoomCollapsed.value) {
    hoverDirtyRoomId.value = roomData.roomId
    const currentStatus = getRoomNumberDirtyStatus(roomData.roomId)
    const nextStatusMap = new Map(roomNumberDirtyStatus.value)
    if (currentStatus) {
      nextStatusMap.delete(roomData.roomId)
    } else {
      nextStatusMap.set(roomData.roomId, true)
    }
    roomNumberDirtyStatus.value = nextStatusMap
    ElMessage.success(currentStatus ? '已置净房' : '已置脏房')
  }
}

// 修改悬停处理函数
const onCellHover = (
  event: MouseEvent,
  dailyStatus: DailyRoomStatus,
  roomData: CalendarRoomData,
) => {
  // 如果有其他弹窗显示，不处理悬停
  if (showQuickActions.value || showClosedRoomActions.value) {
    return
  }

  // 先清除其他可能的悬停状态
  showDirtyHover.value = false

  const target = (event.currentTarget as HTMLElement) || (event.target as HTMLElement)
  const rect = target.getBoundingClientRect()
  hoverCardAnchorRect.value = rect
  updateHoverCardPositionByRect(rect)

  // 显示预订信息卡片（悬停展示）
  if (dailyStatus.reservation) {
    // 先显示基本信息
    hoverReservation.value = dailyStatus.reservation
    showHoverCard.value = true
    void nextTick(() => {
      adjustHoverCardPosition()
    })

    // 异步获取完整的预订详情（如果有ID）
    if (dailyStatus.reservation.id) {
      loadReservationDetails(dailyStatus.reservation.id)
    } else {
      console.log('预订信息缺少ID，使用基本信息:', dailyStatus.reservation)
    }
  } else {
    hoverReservation.value = null
    showHoverCard.value = false
  }
}

// 隐藏悬停卡片
const hideHoverCard = () => {
  showHoverCard.value = false
  showDirtyHover.value = false
  hoverReservation.value = null
  hoverDirtyRoomId.value = null
  hoverCardAnchorRect.value = null
}

// 加载预订详情
const loadReservationDetails = async (reservationId: number) => {
  try {
    console.log('加载预订详情，ID:', reservationId)
    const response = await getReservationById(reservationId)

    if (response.success && response.data) {
      // 更新预订信息
      const reservationData = {
        ...response.data,
        // 确保关键字段存在
        status: response.data.status,
        guestName: response.data.guestName,
        phone: response.data.phone,
        totalAmount: response.data.totalAmount,
        currentRoomPrice: response.data.currentRoomPrice, // 当前房价
        checkInDate: response.data.checkInDate,
        checkOutDate: response.data.checkOutDate,
        channelName: response.data.channelName,
        roomNumber: response.data.roomNumber,
        roomTypeName: response.data.roomTypeName,
        notes: response.data.notes,
      }

      // 同时更新悬停卡片和选中的预订信息
      hoverReservation.value = { ...hoverReservation.value, ...reservationData }
      selectedReservation.value = reservationData
      void nextTick(() => {
        adjustHoverCardPosition()
      })

      console.log('预订详情加载成功:', reservationData)

      // 加载消费和收款数据
      await loadConsumptionAndPaymentData()

      // 加载操作日志与渠道信息
      void loadOperationLogs(reservationId)
      void loadChannelInfo(reservationId)
    } else {
      console.error('加载预订详情失败:', response.message)
    }
  } catch (error: any) {
    console.error('加载预订详情异常:', error)
    // 不显示错误提示，以免干扰用户体验
  }
}

// 隐藏停用房操作弹窗
const hideClosedRoomActions = () => {
  showClosedRoomActions.value = false
}

// 处理停用房操作
const handleClosedRoomAction = (action: string) => {
  if (!closedRoomActionData.value.room) return

  const roomId = closedRoomActionData.value.room.roomId
  const date = closedRoomActionData.value.date
  const key = `${roomId}-${date}`

  if (action === 'open') {
    ;(async () => {
      try {
        const resp = await openRoomBlockouts({
          roomIds: [roomId],
          startDate: date,
          endDate: date,
        })
        if (!resp.success) {
          ElMessage.error(resp.message || '开房失败')
          return
        }
        const currentStatus = roomExtraStatus.value.get(key) || {
          isDirty: false,
          isClosed: false,
          closeType: '',
        }
        roomExtraStatus.value.set(key, {
          ...currentStatus,
          isClosed: false,
          closeType: '',
        })
        ElMessage.success('已开房')
        hideClosedRoomActions()
        await loadRoomStatusCalendarData()
      } catch (error: any) {
        ElMessage.error(error?.message || '开房失败')
      }
    })()
  } else if (action === 'reserve') {
    // 转预订操作
    console.log('转预订操作')
    // TODO: 实现转预订逻辑
    ElMessage.info('转预订功能开发中')
    hideClosedRoomActions() // 转预订后关闭弹窗
  } else if (action === 'cancel') {
    // 取消操作 - 只关闭弹窗
    hideClosedRoomActions()
  }
}

const hideQuickActions = () => {
  showQuickActions.value = false
  clearDragSelection()
  // 隐藏快速操作菜单时，不影响悬停卡片的显示
}

// 添加消费项目
const addConsumptionItem = () => {
  const today = new Date()
  const year = today.getFullYear()
  const month = String(today.getMonth() + 1).padStart(2, '0')
  const day = String(today.getDate()).padStart(2, '0')
  const dateStr = `${year}-${month}-${day}`

  consumptionItems.value.push({
    id: Date.now().toString(),
    type: '',
    quantity: 1,
    amount: 0,
    date: dateStr,
    remark: '',
  })
}

// 删除消费项目
const removeConsumptionItem = (id: string) => {
  consumptionItems.value = consumptionItems.value.filter((item) => item.id !== id)
}

// 添加收款记录
const addPaymentItem = () => {
  const today = new Date()
  const year = today.getFullYear()
  const month = String(today.getMonth() + 1).padStart(2, '0')
  const day = String(today.getDate()).padStart(2, '0')
  const dateStr = `${year}-${month}-${day}`

  paymentItems.value.push({
    id: Date.now().toString(),
    type: '收押金',
    paymentMethod: getDefaultPaymentMethodValue(),
    amount: 0,
    date: dateStr,
    remark: '',
  })
}

// 删除收款记录
const removePaymentItem = (id: string) => {
  paymentItems.value = paymentItems.value.filter((item) => item.id !== id)
}

// 提交预订
const submitBooking = async () => {
  try {
    // 验证表单数据
    if (!bookingForm.value.guestName) {
      ElMessage.warning('请输入客人姓名')
      return
    }
    if (!bookingForm.value.channelId) {
      ElMessage.warning('请选择渠道')
      return
    }
    if (!bookingForm.value.checkInDate || !bookingForm.value.checkOutDate) {
      ElMessage.warning('请选择入住和离店日期')
      return
    }

    // 新建模式需要选择房间，编辑模式使用现有房间
    if (bookingMode.value === 'create' && !selectedRoom.value) {
      ElMessage.warning('请选择房间')
      return
    }

    const requestData: CreateReservationRequest = {
      guestName: bookingForm.value.guestName,
      guestPhone: bookingForm.value.guestPhone,
      guestIdCard: bookingForm.value.guestIdCard,
      roomId:
        bookingMode.value === 'edit' ? bookingForm.value.roomId! : selectedRoom.value?.roomId || 0,
      channelId: parseInt(bookingForm.value.channelId.toString()),
      checkInDate: bookingForm.value.checkInDate,
      checkOutDate: bookingForm.value.checkOutDate,
      adults: bookingForm.value.adults || 1,
      children: bookingForm.value.children || 0,
      totalAmount: parseFloat(String(bookingForm.value.totalAmount || 100.0)),
      pricePlan: bookingForm.value.pricePlan || undefined,
      notes: bookingForm.value.notes,
      directCheckIn: bookingMode.value === 'check-in' ? true : false,
    }

    console.log(`${bookingMode.value === 'create' ? '提交预订' : bookingMode.value === 'check-in' ? '直接入住' : '更新订单'}数据:`, requestData)

    let response
    if (bookingMode.value === 'create') {
      // 创建新预订
      response = await createReservation(requestData)
    } else if (bookingMode.value === 'check-in') {
      // 直接创建入住状态的订单
      response = await createReservation(requestData)
    } else {
      // 更新现有预订
      response = await updateReservation(bookingForm.value.id!, requestData)
    }

    if (response.success) {
      const message =
        bookingMode.value === 'create'
          ? `预订创建成功！订单号：${response.data.orderNumber}`
          : bookingMode.value === 'check-in'
            ? `入住成功！订单号：${response.data.orderNumber}`
            : '订单修改成功！'
      ElMessage.success(message)

      // 如果是创建或直接入住模式，保存消费和收款信息
      if (bookingMode.value === 'create' || bookingMode.value === 'check-in') {
        const reservationId = response.data.id
        const hasConsumption = consumptionItems.value.length > 0
        const hasPayment = paymentItems.value.length > 0

        if (hasConsumption || hasPayment) {
          console.log('开始保存消费和收款记录，预订ID:', reservationId)

          try {
            const promises = []

            // 保存消费记录
            if (hasConsumption) {
              const consumptionPromises = consumptionItems.value
                .filter(item => item.type && item.amount && item.amount > 0)
                .map(item => {
                  const consumptionData: ConsumptionDTO = {
                    reservationId: reservationId,
                    item: item.type,
                    quantity: item.quantity || 1,
                    amount: item.amount,
                    date: item.date,
                    remark: item.remark || '',
                    createdBy: currentOperatorName.value,
                  }
                  return createConsumption(consumptionData)
                })
              promises.push(...consumptionPromises)
            }

            // 保存收款记录
            if (hasPayment) {
              const paymentPromises = paymentItems.value
                .filter(item => item.amount && item.amount > 0)
                .map(item => {
                  const paymentData: PaymentDTO = {
                    reservationId: reservationId,
                    type: item.type,
                    paymentMethod: item.paymentMethod,
                    amount: item.amount,
                    date: item.date,
                    remark: item.remark || '',
                    createdBy: currentOperatorName.value,
                  }
                  return createPayment(paymentData)
                })
              promises.push(...paymentPromises)
            }

            // 并发保存所有记录
            const results = await Promise.all(promises)
            const successCount = results.filter(r => r.success).length
            const failCount = results.length - successCount

            if (successCount > 0) {
              console.log(`成功保存 ${successCount} 条消费/收款记录`)
              if (failCount > 0) {
                ElMessage.warning(`${successCount} 条记录保存成功，${failCount} 条失败`)
              }
            }
            if (failCount === results.length && results.length > 0) {
              ElMessage.error('消费/收款记录保存失败')
            }
          } catch (error) {
            console.error('保存消费/收款记录时出错:', error)
            ElMessage.warning('订单创建成功，但消费/收款记录保存失败')
          }
        }
      }

      // 关闭侧边栏并重置表单
      showBookingSidebar.value = false
      showBookingDetailSidebar.value = false // 同时关闭详情侧边栏
      resetBookingForm()

      // 刷新日历数据
      await loadRoomStatusCalendarData()
    } else {
      const message =
        bookingMode.value === 'create'
          ? `预订创建失败：${response.message}`
          : `订单修改失败：${response.message}`
      ElMessage.error(message)
    }
  } catch (error: any) {
    console.error(`${bookingMode.value === 'create' ? '预订创建' : '订单修改'}失败:`, error)
    const errorMessage =
      error?.response?.data?.message ||
      error?.message ||
      `${bookingMode.value === 'create' ? '预订创建' : '订单修改'}失败，请稍后重试`
    ElMessage.error(errorMessage)
  }
}

// 重置预订表单
const resetBookingForm = () => {
  isManualPrice.value = false
  // 重置为新建模式
  bookingMode.value = 'create'

  bookingForm.value = {
    id: null,
    guestName: '',
    guestPhone: '',
    guestIdCard: '',
    channelId: null,
    checkInDate: '',
    checkOutDate: '',
    roomId: null,
    roomNumber: '',
    roomTypeName: '',
    adults: 1,
    children: 0,
    totalAmount: 0,
    pricePlan: '',
    notes: '',
    hasSpecialColor: false,
  }

  selectedPricePlanId.value = null
  roomTypePricePlanMappings.value = []

  // 清空收款和消费记录
  paymentItems.value = []
  consumptionItems.value = []
}

// 处理修改订单
const handleModifyOrder = () => {
  isManualPrice.value = false
  if (!selectedReservation.value) {
    ElMessage.warning('未找到订单信息')
    return
  }

  // 设置为编辑模式
  bookingMode.value = 'edit'

  // 填充预订表单数据
  bookingForm.value = {
    id: selectedReservation.value.id,
    guestName: selectedReservation.value.guestName || '',
    guestPhone: selectedReservation.value.phone || '',
    guestIdCard: selectedReservation.value.guestIdCard || '',
    channelId: selectedReservation.value.channelId || 2, // 默认渠道ID
    checkInDate: selectedReservation.value.checkInDate || selectedReservation.value.checkIn || '',
    checkOutDate:
      selectedReservation.value.checkOutDate || selectedReservation.value.checkOut || '',
    roomId: selectedReservation.value.roomId || selectedRoom.value?.roomId || null,
    roomNumber: selectedReservation.value.roomNumber || selectedRoom.value?.roomNumber || '',
    roomTypeName: selectedReservation.value.roomTypeName || selectedRoom.value?.roomType || '',
    totalAmount: selectedReservation.value.totalAmount || 100,
    pricePlan: selectedReservation.value.pricePlan || '',
    adults: (selectedReservation.value as any).adults || 1,
    children: (selectedReservation.value as any).children || 0,
    notes: selectedReservation.value.notes || '',
    hasSpecialColor: false,
  }

  // 显示统一的预订侧边栏（编辑模式）
  showBookingSidebar.value = true
  if (bookingForm.value.roomId) {
    fetchRoomTypePrice(bookingForm.value.roomId)
  }
}

// 打开添加消费侧边栏
const openAddConsumptionSidebar = () => {
  // 重置表单
  consumptionForm.value = {
    item: '',
    quantity: 1,
    amount: 0,
    date: new Date().toISOString().split('T')[0],
    remark: '',
  }
  showAddConsumptionSidebar.value = true
}

// 打开收款/退款侧边栏
const openPaymentSidebar = () => {
  // 重置表单
  activePaymentTab.value = 'payment'
  paymentForm.value = {
    type: 'payment',
    paymentMethod: getDefaultPaymentMethodValue(),
    amount: 0,
    date: new Date().toISOString().split('T')[0],
    remark: '',
  }
  showPaymentSidebar.value = true
}

// 提交添加消费
const submitConsumption = async () => {
  if (!consumptionForm.value.item) {
    ElMessage.warning('请选择消费项目')
    return
  }
  if (!consumptionForm.value.amount || consumptionForm.value.amount <= 0) {
    ElMessage.warning('请输入有效金额')
    return
  }
  if (!selectedReservation.value?.id) {
    ElMessage.warning('未找到订单信息')
    return
  }

  try {
    const consumptionData: ConsumptionDTO = {
      reservationId: selectedReservation.value.id,
      item: consumptionForm.value.item,
      quantity: consumptionForm.value.quantity,
      amount: Math.abs(consumptionForm.value.amount), // 后端会自动转为负数
      date: consumptionForm.value.date,
      remark: consumptionForm.value.remark,
      createdBy: currentOperatorName.value,
    }

    const response = await createConsumption(consumptionData)
    if (response.success) {
      ElMessage.success('消费记录添加成功')
      showAddConsumptionSidebar.value = false
      // 刷新消费记录列表
      await loadConsumptionAndPaymentData()
      // 确保消费面板展开
      if (!activeCollapsePanels.value.includes('1')) {
        activeCollapsePanels.value.push('1')
      }
      console.log('消费记录添加成功，当前消费列表:', consumptionList.value)
    } else {
      ElMessage.error(response.message || '添加消费记录失败')
    }
  } catch (error: any) {
    console.error('添加消费记录失败:', error)
    ElMessage.error('网络错误，添加消费记录失败')
  }
}

// 提交收款
const submitPayment = async () => {
  if (!paymentForm.value.paymentMethod) {
    ElMessage.warning('请选择支付方式')
    return
  }
  if (!paymentForm.value.amount || paymentForm.value.amount <= 0) {
    ElMessage.warning('请输入有效金额')
    return
  }
  if (!selectedReservation.value?.id) {
    ElMessage.warning('未找到订单信息')
    return
  }

  try {
    const paymentData: PaymentDTO = {
      reservationId: selectedReservation.value.id,
      type: paymentForm.value.type === 'payment' ? '收款' : '收押金',
      paymentMethod: paymentForm.value.paymentMethod,
      amount: paymentForm.value.amount,
      date: paymentForm.value.date,
      remark: paymentForm.value.remark,
      createdBy: currentOperatorName.value,
    }

    const response = await createPayment(paymentData)
    if (response.success) {
      ElMessage.success('收款记录添加成功')
      showPaymentSidebar.value = false
      // 刷新收款记录列表
      await loadConsumptionAndPaymentData()
      // 确保收款面板展开
      if (!activeCollapsePanels.value.includes('2')) {
        activeCollapsePanels.value.push('2')
      }
      console.log('收款记录添加成功，当前收款列表:', paymentList.value)
    } else {
      ElMessage.error(response.message || '添加收款记录失败')
    }
  } catch (error: any) {
    console.error('添加收款记录失败:', error)
    ElMessage.error('网络错误，添加收款记录失败')
  }
}

// 格式化短日期（去掉日期前导零）
const formatShortDate = (dateStr: string) => {
  if (!dateStr) return ''
  // 输入格式：2025-09-30
  // 输出格式：2025-09-3
  const parts = dateStr.split('-')
  if (parts.length === 3) {
    const year = parts[0]
    const month = parts[1]
    const day = parseInt(parts[2], 10).toString() // 去掉前导零
    return `${year}-${month}-${day}`
  }
  return dateStr
}

// 加载消费和收款数据
const loadConsumptionAndPaymentData = async () => {
  if (!selectedReservation.value?.id) {
    console.log('loadConsumptionAndPaymentData: 没有选中的预订')
    return
  }

  console.log('loadConsumptionAndPaymentData: 开始加载预订ID:', selectedReservation.value.id)

  try {
    // 并发加载消费和收款数据
    const [consumptionRes, paymentRes, totalConsumptionRes, totalPaymentRes] = await Promise.all([
      getConsumptionsByReservationId(selectedReservation.value.id),
      getPaymentsByReservationId(selectedReservation.value.id),
      getTotalConsumption(selectedReservation.value.id),
      getTotalPayment(selectedReservation.value.id),
    ])

    console.log('消费记录响应:', consumptionRes)
    console.log('收款记录响应:', paymentRes)
    console.log('总消费金额响应:', totalConsumptionRes)
    console.log('总收款金额响应:', totalPaymentRes)

    if (consumptionRes.success) {
      consumptionList.value = consumptionRes.data
      console.log('消费列表已更新，数量:', consumptionList.value.length)
    }
    if (paymentRes.success) {
      paymentList.value = paymentRes.data
      console.log('收款列表已更新，数量:', paymentList.value.length)
    }
    if (totalConsumptionRes.success) {
      totalConsumption.value = totalConsumptionRes.data
      console.log('总消费金额已更新:', totalConsumption.value)
    }
    if (totalPaymentRes.success) {
      totalPayment.value = totalPaymentRes.data
      console.log('总收款金额已更新:', totalPayment.value)
    }
  } catch (error) {
    console.error('加载消费和收款数据失败:', error)
  }
}

// 删除消费记录
const handleDeleteConsumption = async (id: number) => {
  try {
    await ElMessageBox.confirm('确认删除此消费记录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })

    const response = await deleteConsumption(id)
    if (response.success) {
      ElMessage.success('删除成功')
      await loadConsumptionAndPaymentData()
    } else {
      ElMessage.error(response.message || '删除失败')
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('删除消费记录失败:', error)
      ElMessage.error('删除失败')
    }
  }
}

// 删除收款记录
const handleDeletePayment = async (id: number) => {
  try {
    await ElMessageBox.confirm('确认删除此收款记录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })

    const response = await deletePayment(id)
    if (response.success) {
      ElMessage.success('删除成功')
      await loadConsumptionAndPaymentData()
    } else {
      ElMessage.error(response.message || '删除失败')
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('删除收款记录失败:', error)
      ElMessage.error('删除失败')
    }
  }
}

// 处理更多操作
const handleMoreActions = (command: string) => {
  switch (command) {
    case 'moveToOrderBox':
      handleMoveToOrderBox()
      break
    case 'cancelReservation':
      handleShowCancelReservation()
      break
    default:
      console.log('未知操作:', command)
  }
}

// 移入订单盒子
const handleMoveToOrderBox = async () => {
  if (!selectedReservation.value) {
    ElMessage.warning('未找到订单信息')
    return
  }

  // 检查订单状态
  const status = selectedReservation.value.status
  if (status !== 'CONFIRMED' && status !== 'RESERVED') {
    ElMessage.warning('只有已预订的房间可以移入订单盒子')
    return
  }

  try {
    const { moveToOrderBox } = await import('@/api/orderBox')

    ElMessageBox.confirm(
      '移入订单盒子后，该房间将不实际排房，不占库存，营业数据不进行统计。是否继续?',
      '确认移入订单盒子',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )
      .then(async () => {
        const loadingInstance = ElLoading.service({ text: '正在移入订单盒子...' })
        try {
          const response = await moveToOrderBox({
            reservationId: selectedReservation.value!.id,
          })
          if (response.success) {
            ElMessage.success('已移入订单盒子')
            // 关闭侧边栏并刷新数据
            showBookingDetailSidebar.value = false
            loadRoomStatusCalendarData()
          } else {
            ElMessage.error(response.message)
          }
        } catch (error) {
          console.error('移入订单盒子失败:', error)
          ElMessage.error('移入订单盒子失败')
        } finally {
          loadingInstance.close()
        }
      })
      .catch(() => {
        // 用户取消操作
      })
  } catch (error) {
    console.error('加载订单盒子模块失败:', error)
    ElMessage.error('功能加载失败')
  }
}

// 显示取消预约侧边栏
const handleShowCancelReservation = () => {
  if (!selectedReservation.value) {
    ElMessage.warning('未找到订单信息')
    return
  }

  // 重置取消表单
  cancelForm.value = {
    reason: '',
    notes: '',
    refundType: 'full',
    refundAmount: 0,
  }

  showCancelReservationSidebar.value = true
}

// 确认取消预约
const confirmCancelReservation = async () => {
  try {
    if (!selectedReservation.value) {
      ElMessage.warning('未找到订单信息')
      return
    }

    if (!cancelForm.value.reason) {
      ElMessage.warning('请选择取消原因')
      return
    }

    // 确认对话框
    await ElMessageBox.confirm(`确认要取消此预约吗？此操作不可撤销。`, '取消预约确认', {
      confirmButtonText: '确认取消',
      cancelButtonText: '返回',
      type: 'warning',
    })

    // 调用后端API取消预约
    const response = await cancelReservation(selectedReservation.value.id)

    if (response.success) {
      ElMessage.success('预约已取消')
    } else {
      ElMessage.error('取消预约失败：' + response.message)
      return
    }

    // 关闭侧边栏并刷新数据
    showCancelReservationSidebar.value = false
    showBookingDetailSidebar.value = false
    await loadRoomStatusCalendarData()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('取消预约失败:', error)
      ElMessage.error('取消预约失败，请稍后重试')
    }
  }
}

// 处理办理入住
const handleCheckIn = async () => {
  try {
    if (!selectedReservation.value) {
      ElMessage.warning('未找到订单信息')
      return
    }

    // 确认对话框
    await ElMessageBox.confirm(
      `确认为客户 ${selectedReservation.value.guestName} 办理入住手续吗？`,
      '办理入住',
      {
        confirmButtonText: '确认入住',
        cancelButtonText: '取消',
        type: 'success',
      },
    )

    // 调用后端API办理入住
    const response = await checkInReservation(selectedReservation.value.id)

    if (response.success) {
      ElMessage.success('入住办理成功！')

      // 关闭侧边栏并刷新数据
      showBookingDetailSidebar.value = false
      await loadRoomStatusCalendarData()
    } else {
      ElMessage.error('办理入住失败：' + response.message)
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('办理入住失败:', error)
      const errorMessage =
        error?.response?.data?.message || error?.message || '办理入住失败，请稍后重试'
      ElMessage.error(errorMessage)
    }
  }
}

// 处理办理退房
const handleCheckOut = async () => {
  try {
    if (!selectedReservation.value) {
      ElMessage.warning('未找到订单信息')
      return
    }

    // 确认对话框
    await ElMessageBox.confirm(
      `确认为客户 ${selectedReservation.value.guestName} 办理退房手续吗？`,
      '办理退房',
      {
        confirmButtonText: '确认退房',
        cancelButtonText: '取消',
        type: 'warning',
      },
    )

    // 调用后端API办理退房
    const response = await checkOutReservation(selectedReservation.value.id)

    if (response.success) {
      ElMessage.success('退房办理成功！')

      // 关闭侧边栏并刷新数据
      showBookingDetailSidebar.value = false
      await loadRoomStatusCalendarData()
    } else {
      ElMessage.error('办理退房失败：' + response.message)
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('办理退房失败:', error)
      const errorMessage =
        error?.response?.data?.message || error?.message || '办理退房失败，请稍后重试'
      ElMessage.error(errorMessage)
    }
  }
}

// 获取房间显示文本
const getRoomDisplayText = () => {
  if (!selectedRoom.value || !bookingForm.value.checkInDate || !bookingForm.value.checkOutDate) {
    return '请选择房间和日期'
  }

  const checkIn = new Date(bookingForm.value.checkInDate)
  const checkOut = new Date(bookingForm.value.checkOutDate)
  const nights = Math.ceil((checkOut.getTime() - checkIn.getTime()) / (1000 * 60 * 60 * 24))

  return `${nights}晚 ${selectedRoom.value.roomNumber}/${selectedRoom.value.roomType}`
}

// 获取房间价格信息
const fetchRoomTypePrice = async (roomId: number) => {
  if (!roomId) return
  
  try {
    isLoadingPrice.value = true
    const response = await getRoomTypeByRoomId(roomId)
    
    if (response.success) {
      currentRoomType.value = response.data
      await loadRoomTypePricePlans(response.data.id)
      updateBookingPrice()
    } else {
      console.error('获取房型价格失败:', response.message)
      ElMessage.error('获取房型价格失败')
    }
  } catch (error) {
    console.error('获取房型价格出错:', error)
    ElMessage.error('获取房型价格出错')
  } finally {
    isLoadingPrice.value = false
  }
}

const loadRoomTypePricePlans = async (roomTypeId: number) => {
  const previousPricePlanName = bookingForm.value.pricePlan
  roomTypePricePlanMappings.value = []
  selectedPricePlanId.value = null

  if (!roomTypeId) {
    bookingForm.value.pricePlan = ''
    return
  }

  try {
    const res = (await getPricePlansByRoomType(roomTypeId)) as unknown as {
      success?: boolean
      data?: RoomTypePricePlanDTO[]
    }
    const mappings = Array.isArray(res?.data) ? res.data : []
    roomTypePricePlanMappings.value = mappings

    if (!mappings.length) {
      bookingForm.value.pricePlan = ''
      return
    }

    const matched = mappings.find(
      (item) => item.pricePlan?.name && item.pricePlan.name === previousPricePlanName
    )
    selectedPricePlanId.value = matched?.pricePlan?.id || mappings[0].pricePlan?.id || null

    const selected = pricePlanOptions.value.find((item) => item.id === selectedPricePlanId.value)
    bookingForm.value.pricePlan = selected?.name || ''
  } catch (error) {
    console.error('加载房型价格计划失败:', error)
    roomTypePricePlanMappings.value = []
    selectedPricePlanId.value = null
    bookingForm.value.pricePlan = ''
  }
}

const resolvePlanPriceByDate = (mapping: RoomTypePricePlanDTO, date: Date): number => {
  const dayOfWeek = date.getDay()
  switch (dayOfWeek) {
    case 1:
      return Number(mapping.mondayPrice || 0)
    case 2:
      return Number(mapping.tuesdayPrice || 0)
    case 3:
      return Number(mapping.wednesdayPrice || 0)
    case 4:
      return Number(mapping.thursdayPrice || 0)
    case 5:
      return Number(mapping.fridayPrice || 0)
    case 6:
      return Number(mapping.saturdayPrice || 0)
    case 0:
      return Number(mapping.sundayPrice || 0)
    default:
      return 0
  }
}

const calculateTotalPriceByPlan = (
  mapping: RoomTypePricePlanDTO,
  checkInDate: string,
  checkOutDate: string
): number => {
  const checkIn = new Date(checkInDate)
  const checkOut = new Date(checkOutDate)
  let currentDate = new Date(checkIn)
  let total = 0

  while (currentDate < checkOut) {
    total += resolvePlanPriceByDate(mapping, currentDate)
    currentDate.setDate(currentDate.getDate() + 1)
  }

  return total
}

const normalizeBookingTotalAmount = () => {
  const amount = Number(bookingForm.value.totalAmount)
  bookingForm.value.totalAmount = Number.isFinite(amount) && amount >= 0 ? amount : 0
}

// 更新预订价格
const updateBookingPrice = async () => {
  if (isManualPrice.value) {
    normalizeBookingTotalAmount()
    return
  }

  if (!currentRoomType.value || !bookingForm.value.checkInDate || !bookingForm.value.checkOutDate) {
    bookingForm.value.totalAmount = 0
    return
  }

  const checkIn = new Date(bookingForm.value.checkInDate)
  const checkOut = new Date(bookingForm.value.checkOutDate)
  const nights = Math.ceil((checkOut.getTime() - checkIn.getTime()) / (1000 * 60 * 60 * 24))

  if (nights <= 0) {
    bookingForm.value.totalAmount = 0
    return
  }

  try {
    isLoadingPrice.value = true

    const selectedPlanMapping = roomTypePricePlanMappings.value.find(
      (item) => item.pricePlan?.id === selectedPricePlanId.value
    )
    if (selectedPlanMapping) {
      bookingForm.value.totalAmount = calculateTotalPriceByPlan(
        selectedPlanMapping,
        bookingForm.value.checkInDate,
        bookingForm.value.checkOutDate
      )
      return
    }

    // 直接使用基于星期的价格计算方式
    console.log('当前房型价格信息:', {
      monPrice: currentRoomType.value.monPrice,
      tuePrice: currentRoomType.value.tuePrice,
      wedPrice: currentRoomType.value.wedPrice,
      thuPrice: currentRoomType.value.thuPrice,
      friPrice: currentRoomType.value.friPrice,
      satPrice: currentRoomType.value.satPrice,
      sunPrice: currentRoomType.value.sunPrice,
    })

    const totalPrice = calculateTotalPriceByDates(
      currentRoomType.value,
      bookingForm.value.checkInDate,
      bookingForm.value.checkOutDate
    )

    console.log('计算的总价格:', totalPrice)
    bookingForm.value.totalAmount = totalPrice
  } catch (error) {
    console.error('计算价格失败:', error)
    bookingForm.value.totalAmount = 0
  } finally {
    isLoadingPrice.value = false
  }
}

// 关闭所有弹窗
const closeAllPopups = () => {
  showQuickActions.value = false
  showClosedRoomActions.value = false
  showHoverCard.value = false
  showDirtyHover.value = false
}

// 修改单元格点击处理
const onCellClick = (
  event: MouseEvent,
  roomData: CalendarRoomData,
  dailyStatus: DailyRoomStatus,
) => {
  if (suppressNextCellClick.value) {
    suppressNextCellClick.value = false
    return
  }

  // 如果处于批量模式，切换单元格选择状态
  if (batchMode.value) {
    const cellKey = `${roomData.roomId}-${dailyStatus.date}`
    if (selectedCells.value.has(cellKey)) {
      selectedCells.value.delete(cellKey)
    } else {
      selectedCells.value.add(cellKey)
    }
    return
  }

  // 兜底：点击已选中的空白格子时，直接取消该格子选中
  const clickedCellKey = getCellKey(roomData.roomId, dailyStatus.date)
  if (selectedCells.value.has(clickedCellKey)) {
    selectedCells.value.delete(clickedCellKey)
    if (selectedCells.value.size === 0) {
      clearDragSelection()
    }
    closeAllPopups()
    return
  }

  // 先关闭所有弹窗，确保同时只有一个弹窗
  closeAllPopups()
  clearDragSelection()

  // 检查是否有停用图标，如果有则优先显示停用房操作弹窗
  const roomStatus = getRoomExtraStatus(roomData.roomId, dailyStatus.date)
  if (roomStatus.isClosed) {
    const rect = (event.target as HTMLElement).getBoundingClientRect()
    closedRoomActionPosition.value = {
      x: rect.left + rect.width / 2,
      y: rect.bottom + 10,
    }

    closedRoomActionData.value = {
      room: roomData,
      date: dailyStatus.date,
    }

    showClosedRoomActions.value = true
    return
  }

  // 如果格子有预订信息，打开预订详情侧边栏
  if (dailyStatus.reservation) {
    selectedReservation.value = dailyStatus.reservation
    selectedRoom.value = roomData
    selectedDate.value = dailyStatus.date
    showBookingDetailSidebar.value = true

    // 加载完整的预订详情（包含当前房价）
    if (dailyStatus.reservation.id) {
      loadReservationDetails(dailyStatus.reservation.id)
    }
    return
  }

  // 如果是空白格子，显示快速操作菜单（根据视窗自动调整位置）
  const target = (event.currentTarget as HTMLElement) || (event.target as HTMLElement)
  const rect = target.getBoundingClientRect()
  quickActionRoom.value = roomData
  quickActionDate.value = dailyStatus.date
  quickActionDateRange.value = null
  showQuickActions.value = true
  void nextTick(() => {
    updateQuickActionPopupPosition(rect)
  })
}

const updateQuickActionPopupPosition = (triggerRect: DOMRect) => {
  const popup = quickActionsPopupRef.value
  const popupWidth = popup?.offsetWidth || 280
  const popupHeight = popup?.offsetHeight || 260
  const viewportPadding = 12
  const popupGap = 8

  let centerX = triggerRect.left + triggerRect.width / 2
  const minCenterX = viewportPadding + popupWidth / 2
  const maxCenterX = window.innerWidth - viewportPadding - popupWidth / 2
  centerX = Math.min(maxCenterX, Math.max(minCenterX, centerX))

  let topY = triggerRect.bottom + popupGap
  const maxTopY = window.innerHeight - viewportPadding - popupHeight

  if (topY > maxTopY) {
    topY = triggerRect.top - popupHeight - popupGap
  }
  if (topY < viewportPadding) {
    topY = viewportPadding
  }

  quickActionPosition.value = {
    x: centerX,
    y: topY,
  }
}

const ensureContinuousQuickActionSelection = () => {
  if (!quickActionRoom.value) return true
  if (selectedCells.value.size <= 1) return true
  if (isSelectionContinuousForRoom(quickActionRoom.value.roomId)) return true

  ElMessage.warning('当前点选包含不连续日期，请补齐中间日期或改用拖动连续选择')
  return false
}

// 修改快速操作处理
const handleQuickAction = (action: string) => {
  if (action === 'book') {
    if (!ensureContinuousQuickActionSelection()) return

    // 设置为创建模式
    bookingMode.value = 'create'

    // 打开预订侧边栏
    selectedRoom.value = quickActionRoom.value
    selectedDate.value = quickActionDate.value

    // 自动填充房间和日期信息（支持拖动连续选择）
    if (quickActionRoom.value && quickActionDate.value) {
      const startDate = quickActionDateRange.value?.startDate || quickActionDate.value
      const endDate = quickActionDateRange.value?.endDate || quickActionDate.value
      bookingForm.value.checkInDate = startDate
      bookingForm.value.checkOutDate = addDays(endDate, 1)

      // 获取房型价格信息
      if (quickActionRoom.value.roomId) {
        fetchRoomTypePrice(quickActionRoom.value.roomId)
      }
    }

    showBookingSidebar.value = true
  } else if (action === 'check-in') {
    // 直接入住操作
    handleDirectCheckIn()
  } else if (action === 'close') {
    if (!ensureContinuousQuickActionSelection()) return

    // 打开关房弹窗
    const closeStartDate = quickActionDateRange.value?.startDate || quickActionDate.value
    const closeEndDate = quickActionDateRange.value?.endDate || quickActionDate.value
    closeRoomData.value = {
      room: quickActionRoom.value,
      date: quickActionDate.value,
    }
    closeRoomForm.value.startDate = closeStartDate
    closeRoomForm.value.endDate = closeEndDate
    showCloseRoomDialog.value = true
  } else if (action === 'cancel') {
    // 取消操作
    console.log('取消操作')
  }

  hideQuickActions()
}

// 处理直接入住（今天的空房间）
const handleDirectCheckIn = () => {
  if (!quickActionRoom.value || !quickActionDate.value) {
    ElMessage.warning('房间或日期信息缺失')
    return
  }

  // 设置为直接入住模式
  bookingMode.value = 'check-in'

  // 打开侧边栏
  selectedRoom.value = quickActionRoom.value
  selectedDate.value = quickActionDate.value

  // 自动填充房间和日期信息
  bookingForm.value.checkInDate = quickActionDate.value
  // 默认离店日期为入住日期的下一天
  const checkInDate = new Date(quickActionDate.value)
  checkInDate.setDate(checkInDate.getDate() + 1)
  bookingForm.value.checkOutDate = checkInDate.toISOString().split('T')[0]

  // 获取房型价格信息
  if (quickActionRoom.value.roomId) {
    fetchRoomTypePrice(quickActionRoom.value.roomId)
  }

  showBookingSidebar.value = true
}

// 处理悬停卡片的快速入住
const handleQuickCheckIn = async () => {
  try {
    if (!hoverReservation.value) {
      ElMessage.warning('未找到预订信息')
      return
    }

    // 确认对话框
    await ElMessageBox.confirm(
      `确认为客户 ${hoverReservation.value.guestName} 办理入住手续吗？`,
      '快速入住',
      {
        confirmButtonText: '确认入住',
        cancelButtonText: '取消',
        type: 'success',
      },
    )

    // 调用后端API办理入住
    const response = await checkInReservation(hoverReservation.value.id)

    if (response.success) {
      ElMessage.success('入住办理成功！')

      // 隐藏悬停卡片并刷新数据
      hideHoverCard()
      await loadRoomStatusCalendarData()
    } else {
      ElMessage.error('办理入住失败：' + response.message)
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('快速入住失败:', error)
      const errorMessage =
        error?.response?.data?.message || error?.message || '办理入住失败，请稍后重试'
      ElMessage.error(errorMessage)
    }
  }
}

// 处理悬停卡片的快速退房
const handleQuickCheckOut = async () => {
  try {
    if (!hoverReservation.value) {
      ElMessage.warning('未找到预订信息')
      return
    }

    // 确认对话框
    await ElMessageBox.confirm(
      `确认为客户 ${hoverReservation.value.guestName} 办理退房手续吗？`,
      '快速退房',
      {
        confirmButtonText: '确认退房',
        cancelButtonText: '取消',
        type: 'warning',
      },
    )

    // 调用后端API办理退房
    const response = await checkOutReservation(hoverReservation.value.id)

    if (response.success) {
      ElMessage.success('退房办理成功！')

      // 隐藏悬停卡片并刷新数据
      hideHoverCard()
      await loadRoomStatusCalendarData()
    } else {
      ElMessage.error('办理退房失败：' + response.message)
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('快速退房失败:', error)
      ElMessage.error('办理退房失败，请稍后重试')
    }
  }
}

// 关房弹窗相关方法
const cancelCloseRoom = () => {
  showCloseRoomDialog.value = false
  closeRoomForm.value = {
    type: 'stop',
    startDate: '',
    endDate: '',
    remark: '',
  }
}

const confirmCloseRoom = async () => {
  if (!closeRoomData.value.room || !closeRoomForm.value.startDate || !closeRoomForm.value.endDate) {
    ElMessage.warning('请填写完整信息')
    return
  }

  try {
    const roomId = closeRoomData.value.room.roomId
    const resp = await closeRoomBlockouts({
      roomIds: [roomId],
      startDate: closeRoomForm.value.startDate,
      endDate: closeRoomForm.value.endDate,
      type: closeRoomForm.value.type as 'stop' | 'maintenance' | 'retain',
      remark: closeRoomForm.value.remark,
    })

    if (!resp.success) {
      ElMessage.error(resp.message || '关房失败')
      return
    }

    ElMessage.success(`关房已保存（${resp.data?.affectedDays ?? 0} 天）`)
    showCloseRoomDialog.value = false
    cancelCloseRoom()
    await loadRoomStatusCalendarData()
  } catch (error: any) {
    ElMessage.error(error?.message || '关房失败')
  }
}

// 批量关房相关处理函数
const handleDateSelectorCommand = (command: string) => {
  if (command === 'all' || command === 'weekday' || command === 'weekend') {
    batchCloseForm.value.weekMode = command
  }
}

const cancelBatchCloseRoom = () => {
  batchSelectedRoomIds.value = [...batchCloseSelectedRoomIds.value]
  showBatchCloseRoomDialog.value = false
  showBatchDialog.value = true
}

const parseDateOnly = (value: string) => {
  const [year, month, day] = value.split('-').map((part) => Number(part))
  return new Date(year, (month || 1) - 1, day || 1)
}

const formatDateOnly = (date: Date) => {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const isWeekendDay = (date: Date) => {
  const day = date.getDay()
  return day === 0 || day === 6
}

const matchWeekMode = (date: Date, weekMode: BatchWeekMode) => {
  if (weekMode === 'all') {
    return true
  }
  if (weekMode === 'weekday') {
    return !isWeekendDay(date)
  }
  return isWeekendDay(date)
}

const buildBatchDateRanges = (startDate: string, endDate: string, weekMode: BatchWeekMode) => {
  if (weekMode === 'all') {
    return [{ startDate, endDate }]
  }

  const ranges: Array<{ startDate: string; endDate: string }> = []
  const end = parseDateOnly(endDate)
  const cursor = parseDateOnly(startDate)
  let currentRangeStart: Date | null = null

  while (cursor <= end) {
    const matched = matchWeekMode(cursor, weekMode)
    if (matched && !currentRangeStart) {
      currentRangeStart = new Date(cursor)
    }
    if (!matched && currentRangeStart) {
      const previousDate = new Date(cursor)
      previousDate.setDate(previousDate.getDate() - 1)
      ranges.push({
        startDate: formatDateOnly(currentRangeStart),
        endDate: formatDateOnly(previousDate),
      })
      currentRangeStart = null
    }
    cursor.setDate(cursor.getDate() + 1)
  }

  if (currentRangeStart) {
    const finalDate = new Date(end)
    ranges.push({
      startDate: formatDateOnly(currentRangeStart),
      endDate: formatDateOnly(finalDate),
    })
  }

  return ranges
}

const confirmBatchCloseRoom = async () => {
  if (!batchCloseForm.value.startDate || !batchCloseForm.value.endDate) {
    ElMessage.warning('请填写完整信息')
    return
  }
  if (batchCloseForm.value.startDate > batchCloseForm.value.endDate) {
    ElMessage.warning('开始日期不能晚于结束日期')
    return
  }
  if (batchCloseSelectedRoomIds.value.length === 0) {
    ElMessage.warning('请先选择要操作的房间')
    return
  }

  try {
    const dateRanges = buildBatchDateRanges(
      batchCloseForm.value.startDate,
      batchCloseForm.value.endDate,
      batchCloseForm.value.weekMode,
    )
    if (dateRanges.length === 0) {
      ElMessage.warning('当前日期范围内没有可应用的日期')
      return
    }

    let totalAffectedDays = 0

    if (batchAction.value === 'close') {
      for (const range of dateRanges) {
        const resp = await closeRoomBlockouts({
          roomIds: batchCloseSelectedRoomIds.value,
          startDate: range.startDate,
          endDate: range.endDate,
          type: batchCloseForm.value.type as 'stop' | 'maintenance' | 'retain',
          remark: batchCloseForm.value.remark,
        })
        if (!resp.success) {
          ElMessage.error(resp.message || '批量关房失败')
          return
        }
        totalAffectedDays += resp.data?.affectedDays ?? 0
      }
      ElMessage.success(`批量关房已保存（${totalAffectedDays} 天）`)
    } else {
      for (const range of dateRanges) {
        const resp = await openRoomBlockouts({
          roomIds: batchCloseSelectedRoomIds.value,
          startDate: range.startDate,
          endDate: range.endDate,
        })
        if (!resp.success) {
          ElMessage.error(resp.message || '批量开房失败')
          return
        }
        totalAffectedDays += resp.data?.affectedDays ?? 0
      }
      ElMessage.success(`批量开房已保存（${totalAffectedDays} 天）`)
    }

    showBatchCloseRoomDialog.value = false
    batchMode.value = false
    selectedCells.value.clear()
    batchSelectedRoomIds.value = []
    batchExpandedRoomTypes.value = []
    batchSearchKeyword.value = ''
    batchCloseSelectedRoomIds.value = []

    batchCloseForm.value = {
      type: 'stop',
      startDate: '',
      endDate: '',
      remark: '',
      weekMode: 'all',
    }

    await loadRoomStatusCalendarData()
  } catch (error: any) {
    ElMessage.error(error?.message || `${batchDialogTitle.value}失败`)
  }
}

// 搜索相关方法
const querySearchAsync = (
  queryString: string,
  callback: (suggestions: ReservationDTO[]) => void,
) => {
  if (!queryString || queryString.trim().length < 2) {
    callback([])
    return
  }

  // 清除之前的搜索定时器
  if (searchTimeout.value) {
    clearTimeout(searchTimeout.value)
  }

  // 设置新的搜索定时器，防抖300ms
  searchTimeout.value = setTimeout(async () => {
    try {
      const response = await searchReservations(queryString.trim())
      if (response.success && response.data) {
        searchResults.value = response.data
        callback(response.data)
      } else {
        callback([])
      }
    } catch (error) {
      console.error('搜索预订失败:', error)
      callback([])
    }
  }, 300)
}

const handleSearchInput = (value: string) => {
  // 输入变化时的处理，这里可以添加额外的逻辑
  if (!value || value.trim().length === 0) {
    searchResults.value = []
  }
}

const handleSearchSelect = (reservation: ReservationDTO) => {
  // 选择搜索结果时显示订单详情侧边栏
  selectedReservation.value = reservation

  // 需要构建房间数据和日期信息以供侧边栏使用
  selectedRoom.value = {
    roomId: reservation.roomId || 0,
    roomNumber: reservation.roomNumber || '',
    roomType: reservation.roomTypeName || '',
    dailyStatus: [], // 这里可以留空，因为我们主要关心预订信息
  }
  selectedDate.value = reservation.checkInDate

  // 显示预订详情侧边栏
  showBookingDetailSidebar.value = true

  // 加载完整的预订详情
  if (reservation.id) {
    loadReservationDetails(reservation.id)
  }

  // 清空搜索框
  searchKeyword.value = ''
  searchResults.value = []
}

// 加载渠道数据
const loadChannels = async () => {
  try {
    const response = (await getAllChannels()) as any
    if (response.success && response.data) {
      channels.value = response.data
      // 构建渠道映射表，方便根据名称快速查找
      const map = new Map<string, ChannelDTO>()
      response.data.forEach((channel: ChannelDTO) => {
        map.set(channel.name, channel)
        map.set(channel.code, channel)
      })
      channelMap.value = map
    } else {
      // 使用默认渠道数据
      const defaultChannels = [
        {
          id: 1,
          name: '自来客',
          code: 'DIRECT',
          type: 'DIRECT',
          color: '#409EFF',
          enabled: true,
          description: '',
          createdAt: '',
          updatedAt: '',
        },
        {
          id: 2,
          name: '携程',
          code: 'CTRIP',
          type: 'OTA',
          color: '#1890FF',
          enabled: true,
          description: '',
          createdAt: '',
          updatedAt: '',
        },
        {
          id: 3,
          name: '美团',
          code: 'MEITUAN',
          type: 'OTA',
          color: '#FFB800',
          enabled: true,
          description: '',
          createdAt: '',
          updatedAt: '',
        },
        {
          id: 4,
          name: '飞猪',
          code: 'FLIGGY',
          type: 'OTA',
          color: '#FF6A00',
          enabled: true,
          description: '',
          createdAt: '',
          updatedAt: '',
        },
        {
          id: 5,
          name: '去哪儿',
          code: 'QUNAR',
          type: 'OTA',
          color: '#00C1DE',
          enabled: true,
          description: '',
          createdAt: '',
          updatedAt: '',
        },
      ]
      channels.value = defaultChannels
      const map = new Map<string, ChannelDTO>()
      defaultChannels.forEach((channel) => {
        map.set(channel.name, channel)
        map.set(channel.code, channel)
      })
      channelMap.value = map
    }
  } catch (error) {
    console.error('加载渠道数据失败:', error)
  }
}

// 根据渠道名称获取渠道信息
const getChannelByName = (channelName: string): ChannelDTO | null => {
  return channelMap.value.get(channelName) || null
}

const loadCellPriceDisplayPreferences = () => {
  try {
    const savedVisible = localStorage.getItem(CELL_PRICE_VISIBLE_STORAGE_KEY)
    showCellDefaultPrice.value = savedVisible === 'true'

    const savedSource = localStorage.getItem(CELL_PRICE_SOURCE_STORAGE_KEY)
    if (savedSource) {
      cellPriceDisplaySource.value = savedSource
    }
  } catch (error) {
    console.error('读取房态价格展示偏好失败:', error)
  }
}

const persistCellPriceDisplayPreferences = () => {
  try {
    localStorage.setItem(CELL_PRICE_VISIBLE_STORAGE_KEY, String(showCellDefaultPrice.value))
    localStorage.setItem(CELL_PRICE_SOURCE_STORAGE_KEY, cellPriceDisplaySource.value)
  } catch (error) {
    console.error('保存房态价格展示偏好失败:', error)
  }
}

// 监听日期变化，更新价格
watch(
  () => [bookingForm.value.checkInDate, bookingForm.value.checkOutDate],
  () => {
    if (currentRoomType.value) {
      updateBookingPrice()
    }
  }
)

// 监听选中房间变化，获取房型价格
watch(
  () => selectedRoom.value?.roomId,
  (newRoomId) => {
    if (newRoomId && showBookingSidebar.value) {
      fetchRoomTypePrice(newRoomId)
    }
  }
)

// 监听价格计划选择变化，联动价格和提交字段
watch(selectedPricePlanId, (newPlanId) => {
  const selected = pricePlanOptions.value.find((item) => item.id === newPlanId)
  bookingForm.value.pricePlan = selected?.name || ''
  if (!isManualPrice.value) {
    updateBookingPrice()
  }
})

// 监听手动改价开关，切回自动时按房型/价格计划重算
watch(isManualPrice, (manual) => {
  if (manual) {
    normalizeBookingTotalAmount()
    return
  }
  if (currentRoomType.value) {
    updateBookingPrice()
  }
})

// 侧边栏关闭时清理价格计划状态，避免下次打开带入旧值
watch(showBookingSidebar, (visible) => {
  if (visible) {
    return
  }
  isManualPrice.value = false
  selectedPricePlanId.value = null
  roomTypePricePlanMappings.value = []
  bookingForm.value.pricePlan = ''
})

watch(showCellDefaultPrice, (visible) => {
  persistCellPriceDisplayPreferences()
  if (!visible) {
    resetCalendarManagementPriceCache()
    return
  }
  void (async () => {
    await loadCalendarPricePlanOptions()
    if (cellPriceDisplaySource.value !== 'default') {
      await loadCalendarManagementPrices(true)
    }
  })()
})

watch(cellPriceDisplaySource, (source) => {
  persistCellPriceDisplayPreferences()
  if (source === 'default') {
    resetCalendarManagementPriceCache()
    return
  }
  void (async () => {
    await loadCalendarPricePlanOptions()
    await loadCalendarManagementPrices(true)
  })()
})

watch(calendarCellPricePlanOptions, (options) => {
  if (cellPriceDisplaySource.value === 'default') {
    return
  }
  if (loadingCellPricePlanOptions.value) {
    return
  }
  // 页面切换回来时，价格计划选项可能会短暂为空，避免把用户已选计划误重置为默认
  if (options.length === 0) {
    return
  }
  const selectedPlanId = getSelectedCellPricePlanId()
  const hasSelectedPlan = !!selectedPlanId && options.some((item) => item.id === selectedPlanId)
  if (!hasSelectedPlan) {
    cellPriceDisplaySource.value = 'default'
  }
})

watch(
  visibleDateRange,
  (newRange, oldRange) => {
    if (!newRange || newRange.length !== 2) {
      return
    }

    if (oldRange && oldRange[0] === newRange[0] && oldRange[1] === newRange[1]) {
      return
    }

    // 日期输入和左右切换统一从这里触发，避免 @change 在某些场景不触发
    currentBaseDate.value = shiftYmdDate(newRange[0], CALENDAR_DAYS_BEFORE_BASE)
    void reloadCalendarForVisibleRange()
  },
)

// 生命周期
onMounted(async () => {
  loadCellPriceDisplayPreferences()
  window.addEventListener('mouseup', handleWindowMouseUp)
  await loadChannels()
  await loadPaymentMethodOptions()
  await loadEnabledConsumptionItems()
  await loadCalendarData()
  if (showCellDefaultPrice.value) {
    await loadCalendarPricePlanOptions()
    if (cellPriceDisplaySource.value !== 'default') {
      await loadCalendarManagementPrices(true)
    }
  }
})

onBeforeUnmount(() => {
  window.removeEventListener('mouseup', handleWindowMouseUp)
})

onActivated(async () => {
  loadCellPriceDisplayPreferences()
  await loadPaymentMethodOptions()
  await loadCalendarData()
  if (showCellDefaultPrice.value) {
    await loadCalendarPricePlanOptions()
    if (cellPriceDisplaySource.value !== 'default') {
      await loadCalendarManagementPrices(true)
    }
  }
})
</script>

<style scoped>
.room-status-calendar {
  background: #f5f5f5;
  min-height: 100vh;
}

/* 视图容器样式 */
.calendar-view {
  padding: 20px;
}

.daily-content,
.channel-content {
  background: white;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.header-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding: 15px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.date-navigation {
  display: flex;
  align-items: center;
  gap: 10px;
}

.toolbar-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.cell-price-source-select {
  width: 180px;
}

.calendar-content {
  background: white;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.calendar-container {
  overflow-x: auto;
}

.date-header {
  display: flex;
  background: #f8f9fa;
  border-bottom: 1px solid #e9ecef;
  width: fit-content;
  min-width: 100%;
}

.header-cell {
  width: 120px;
  flex-shrink: 0;
  padding: 15px 10px;
  border-right: 1px solid #e9ecef;
  text-align: center;
  font-weight: bold;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
}

.clickable-header {
  cursor: pointer;
  transition: all 0.2s ease;
  background: white;
  border: 1px solid #e9ecef;
  border-radius: 4px;
  position: relative;
  margin: 2px;
  min-height: 60px;
}

.clickable-header:hover {
  background-color: #f0f7ff;
  border-color: #91d5ff;
  box-shadow: 0 2px 8px rgba(24, 144, 255, 0.15);
  transform: translateY(-1px);
}

.clickable-header:active {
  background-color: #e6f7ff;
  transform: translateY(0);
  box-shadow: 0 1px 4px rgba(24, 144, 255, 0.2);
}

.date-column {
  width: 120px;
  flex-shrink: 0;
  padding: 10px;
  border-right: 1px solid #e9ecef;
  text-align: center;
}

.date-column.weekend {
  background: #fff7e6;
}

.date-column.today {
  background: #e6f7ff;
  font-weight: bold;
}

.date-info {
  font-weight: bold;
}

.month-day {
  font-size: 14px;
}

.weekday {
  font-size: 12px;
  color: #666;
}

.date-time {
  font-size: 10px;
  color: #999;
  margin-top: 5px;
}

/* 空状态样式 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 100px 20px;
  min-height: 400px;
  background: #fafafa;
  border-radius: 8px;
  margin: 20px;
}

.empty-icon {
  margin-bottom: 24px;
  opacity: 0.8;
}

.empty-text {
  font-size: 16px;
  color: #909399;
  margin-bottom: 24px;
  font-weight: 500;
}

.rooms-grid {
  width: fit-content;
  min-width: 100%;
}

.room-row {
  display: flex;
  border-bottom: 1px solid #e9ecef;
}

.room-info-cell,
.room-number-cell {
  width: 120px;
  flex-shrink: 0;
  padding: 20px 10px;
  border-right: 1px solid #e9ecef;
  display: flex;
  align-items: center;
  justify-content: center;
}

.clickable-cell {
  cursor: pointer;
  transition: all 0.2s ease;
  background: white;
  border: 1px solid #e9ecef;
  border-radius: 4px;
  margin: 2px;
  min-height: 76px;
  position: relative;
}

.clickable-cell:hover {
  background-color: #f0f7ff;
  border-color: #91d5ff;
  box-shadow: 0 2px 8px rgba(24, 144, 255, 0.15);
  transform: translateY(-1px);
}

.clickable-cell:active {
  background-color: #e6f7ff;
  transform: translateY(0);
  box-shadow: 0 1px 4px rgba(24, 144, 255, 0.2);
}

.room-type {
  font-size: 12px;
  color: #666;
}

.room-number {
  font-weight: bold;
  font-size: 14px;
  position: relative;
}

.room-number.with-dirty-icon {
  padding-right: 24px; /* 为右上角的脏房图标预留空间 */
}

.status-cell {
  width: 120px;
  flex-shrink: 0;
  min-height: 80px;
  border-right: 1px solid #e9ecef;
  padding: 4px;
  cursor: pointer;
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.status-cell:hover {
  background: #f0f0f0;
}

.status-cell.room-change-drop-target {
  border-color: #409eff !important;
  background: #e6f4ff !important;
  box-shadow: inset 0 0 0 2px #409eff;
}

.status-available {
  background: #f6ffed;
  border: 1px solid #b7eb8f;
}

.status-occupied {
  background: #fff2e8;
  border: 1px solid #ffbb96;
}

.status-reserved {
  background: #e6f7ff;
  border: 1px solid #91d5ff;
}

.status-cell.has-reservation {
  background: #f7e1cc;
  border-color: #e6b78b;
  padding: 0;
  overflow: hidden;
}

.status-cell.reservation-middle-cell,
.status-cell.reservation-end-cell {
  border-left-color: transparent;
  border-left-width: 0;
}

.status-cell.reservation-start-cell,
.status-cell.reservation-middle-cell {
  border-right-color: transparent;
  border-right-width: 0;
}

.status-cell.reservation-start-cell {
  border-top-left-radius: 6px;
  border-bottom-left-radius: 6px;
}

.status-cell.reservation-middle-cell {
  border-radius: 0;
}

.status-cell.reservation-end-cell {
  border-top-right-radius: 6px;
  border-bottom-right-radius: 6px;
}

.status-maintenance {
  background: #fff1f0;
  border: 1px solid #ffa39e;
}

.status-out_of_order {
  background: #f6f6f6;
  border: 1px solid #d9d9d9;
}

.reservation-cell-info {
  text-align: left;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: stretch;
}

.reservation-ribbon {
  width: 100%;
  min-height: 100%;
  height: 100%;
  border-radius: 0;
  background: transparent;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 6px 8px;
}

.reservation-ribbon.reservation-draggable {
  cursor: grab;
  user-select: none;
}

.reservation-ribbon.reservation-draggable:active {
  cursor: grabbing;
}

.status-cell.reservation-start-cell .reservation-ribbon {
  border-top-left-radius: 6px;
  border-bottom-left-radius: 6px;
}

.status-cell.reservation-end-cell .reservation-ribbon {
  border-top-right-radius: 6px;
  border-bottom-right-radius: 6px;
}

.reservation-note-indicator {
  position: absolute;
  top: 2px;
  right: 2px;
  width: 0;
  height: 0;
  border-top: 16px solid #f5222d;
  border-left: 16px solid transparent;
  z-index: 12;
  color: #fff;
  font-size: 10px;
  font-weight: 700;
  line-height: 1;
}

.reservation-note-indicator::after {
  content: '!';
  position: absolute;
  top: -14px;
  left: -6px;
  color: #fff;
}

.empty-cell {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  min-height: 28px;
}

.collapsed-status-text {
  font-size: 20px;
  font-weight: 500;
  color: #1f2937;
}

.collapsed-status-text.full {
  color: #f5222d;
}

.reservation-guest-name {
  font-weight: bold;
  font-size: 12px;
  margin-bottom: 4px;
  color: #6a3b12;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.reservation-channel-badge {
  background: #52c41a;
  color: white;
  padding: 2px 6px;
  border-radius: 2px;
  font-size: 10px;
  margin-bottom: 2px;
  width: fit-content;
  max-width: 100%;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.order-number {
  font-size: 10px;
  color: #666;
}

.status-dialog-content {
  padding: 10px 0;
}

.room-info {
  margin-bottom: 20px;
  padding: 15px;
  background: #f8f9fa;
  border-radius: 4px;
}

.room-info p {
  margin: 5px 0;
  color: #333;
}

/* 筛选侧边栏样式 */
.filter-content {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.filter-section {
  flex: 1;
  overflow-y: auto;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 0;
  border-bottom: 1px solid #e9ecef;
  margin-bottom: 15px;
  font-weight: bold;
}

.room-type-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.room-type-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 0;
}

.room-type-name {
  color: #333;
}

.filter-actions {
  padding: 20px 0;
  border-top: 1px solid #e9ecef;
  display: flex;
  gap: 10px;
  justify-content: flex-end;
}

.filter-actions .el-button {
  flex: 1;
}

/* 快速操作弹窗样式 */
.quick-actions-popup {
  position: fixed;
  background: white;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
  z-index: 2500;
  min-width: 280px;
}

.popup-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 0;
}

.selected-indicator {
  width: 100%;
  height: 40px;
  background: #4096ff;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px 6px 0 0;
}

.check-icon {
  color: white;
  font-size: 18px;
  font-weight: bold;
}

.room-info-title {
  font-weight: 500;
  font-size: 14px;
  color: #333;
  padding: 12px 16px;
  text-align: center;
}

.popup-actions {
  width: 100%;
  padding: 0 16px 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.primary-action-btn {
  width: 100%;
  height: 36px;
  border-radius: 4px;
  font-weight: 500;
}

/* 今天的主要操作按钮容器 */
.today-primary-actions {
  display: flex;
  gap: 8px;
  width: 100%;
}

/* 今天的操作按钮样式 */
.today-action-btn {
  flex: 1;
  height: 36px;
  border-radius: 4px;
  font-weight: 500;
}

.secondary-actions {
  display: flex;
  gap: 8px;
}

.secondary-action-btn {
  flex: 1;
  height: 32px;
  border-radius: 4px;
  background: white;
  border: 1px solid #d9d9d9;
  color: #666;
  font-size: 13px;
}

.secondary-action-btn:hover {
  border-color: #4096ff;
  color: #4096ff;
}

/* 停用房操作弹窗样式 */
.closed-room-actions-popup {
  position: fixed;
  background: white;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
  z-index: 2500;
  min-width: 280px;
}

.closed-indicator {
  width: 100%;
  height: 40px;
  background: #ff4d4f;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border-radius: 6px 6px 0 0;
}

.closed-icon {
  color: white;
  font-size: 16px;
  font-weight: bold;
}

.status-text {
  color: white;
  font-size: 14px;
  font-weight: 500;
}

/* 悬停信息卡片样式 */
.hover-info-card {
  position: fixed;
  background: white;
  border: 1px solid #e9ecef;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  z-index: 3000;
  padding: 15px;
  min-width: 320px;
  max-width: 360px;
  max-height: min(78vh, 560px);
  overflow: hidden;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.card-actions {
  display: flex;
  gap: 8px;
  align-items: center;
}

.guest-info {
  display: flex;
  align-items: center;
  gap: 5px;
  font-weight: bold;
}

.stay-info,
.channel-info,
.status-info {
  display: flex;
  align-items: center;
  gap: 5px;
  margin: 8px 0;
  color: #666;
  font-size: 14px;
  line-height: 1.45;
  word-break: break-word;
}

.status-info .status-label {
  margin-right: 5px;
  font-weight: 500;
}

.price-info {
  display: flex;
  justify-content: space-between;
  margin: 8px 0;
  font-size: 14px;
}

.received {
  color: #52c41a;
}

.notes {
  margin-top: 10px;
  border-top: 1px dashed #e5e7eb;
  padding-top: 8px;
}

.notes-label {
  color: #111827;
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 4px;
}

.notes-content {
  color: #666;
  font-size: 14px;
  line-height: 1.5;
  max-height: 140px;
  overflow-y: auto;
  white-space: pre-wrap;
  word-break: break-word;
  overflow-wrap: anywhere;
  padding-right: 2px;
}

.notes-content-empty {
  color: #9ca3af;
}

/* 预订侧边栏样式 */
.booking-content {
  padding: 20px;
}

.booking-section {
  margin-bottom: 30px;
}

.booking-section h3 {
  margin: 0 0 15px 0;
  color: #333;
  font-size: 16px;
}

/* 消费和收款信息行样式 */
.info-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  padding: 8px;
  border-radius: 4px;
  transition: background-color 0.2s;
}

.info-row:hover {
  background-color: #f8f9fa;
}

.info-row .el-select,
.info-row .el-input-number,
.info-row .el-input {
  flex-shrink: 0;
}

.action-btn {
  padding: 4px 8px;
  font-size: 14px;
  color: #1890ff;
}

.action-btn.danger {
  color: #ff4d4f;
}

.action-btn:hover {
  opacity: 0.8;
}

.add-btn {
  margin-top: 8px;
  color: #1890ff;
  font-size: 14px;
}

.add-btn:hover {
  opacity: 0.8;
}

.room-selection {
  border: 1px solid #e9ecef;
  border-radius: 6px;
  padding: 15px;
}

.date-range {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.room-details {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.manual-price-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 12px;
  flex-wrap: wrap;
}

.price {
  font-weight: bold;
  color: #1890ff;
}

.additional-options {
  margin-top: 15px;
  display: flex;
  gap: 10px;
}

.booking-footer {
  border-top: 1px solid #e9ecef;
  padding-top: 20px;
  margin-top: 20px;
}

.total-amount {
  font-size: 16px;
  font-weight: bold;
  text-align: right;
  margin-bottom: 15px;
}

.booking-footer .actions {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
}

/* 预订详情侧边栏样式 */
.booking-detail-content {
  padding: 0;
}

.booking-detail-content > div[v-if] {
  padding: 20px;
}

.guest-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding: 20px 20px 0 20px;
}

.guest-main {
  display: flex;
  align-items: center;
  gap: 12px;
}

.guest-main h3 {
  margin: 0;
}

/* 取消预约侧边栏样式 */
.cancel-reservation-content {
  padding: 20px;
}

.warning-info {
  margin-bottom: 30px;
}

.reservation-info {
  margin-top: 20px;
  padding: 15px;
  background: #f5f7fa;
  border-radius: 6px;
}

.info-row {
  display: flex;
  margin-bottom: 8px;
}

.info-row .label {
  width: 80px;
  color: #666;
  font-weight: 500;
}

.info-row .value {
  color: #333;
  flex: 1;
}

.cancel-form {
  margin-bottom: 30px;
}

.cancel-actions {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
  padding-top: 20px;
  border-top: 1px solid #ebeef5;
}

/* 当前房价样式 */
.current-price {
  color: #f56c6c !important;
  font-weight: bold;
}

.current-room-price {
  color: #f56c6c;
  font-size: 12px;
  margin-left: 8px;
}

/* 修改订单侧边栏样式 */
/* 修改订单侧边栏样式 */
.modify-order-container {
  display: flex;
  flex-direction: column;
  height: 100vh;
}

.modify-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #ebeef5;
  background: #fff;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.back-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #666;
  font-size: 14px;
}

.title {
  font-size: 16px;
  font-weight: 500;
  color: #333;
}

.close-btn {
  color: #666;
}

.modify-content {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
}

.info-section {
  margin-bottom: 24px;
}

.section-title {
  font-size: 16px;
  font-weight: 500;
  color: #333;
  margin: 0 0 16px 0;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.room-summary {
  font-size: 14px;
  color: #666;
}

.info-row {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.field-group {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.field-group.full-width {
  flex: none;
  width: 100%;
}

.field-group label {
  font-size: 14px;
  color: #333;
  font-weight: 500;
}

.room-detail-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: #f8f9fa;
  border-radius: 6px;
  margin-bottom: 16px;
  font-size: 14px;
}

.date-inputs {
  display: flex;
  align-items: center;
  gap: 8px;
}

.date-separator {
  color: #666;
  margin: 0 4px;
}

.room-select {
  display: flex;
  align-items: center;
  gap: 8px;
}

.nights {
  color: #666;
  font-size: 14px;
}

.price-guest {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-left: auto;
}

.price {
  font-weight: 600;
  color: #409eff;
}

.guest-selector {
  display: flex;
  align-items: center;
  gap: 4px;
}

.guest-icon {
  color: #666;
}

.add-options {
  display: flex;
  gap: 16px;
}

.add-btn {
  color: #409eff;
  font-size: 14px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.notes-section {
  margin-bottom: 20px;
}

.special-color-section {
  margin-bottom: 20px;
}

.modify-footer {
  border-top: 1px solid #ebeef5;
  padding: 16px 20px;
  background: #fff;
}

.amount-summary {
  margin-bottom: 16px;
}

.amount-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  font-size: 14px;
}

.amount-row:last-child {
  margin-bottom: 0;
}

.amount-row .label {
  color: #666;
}

.amount-row .amount {
  font-weight: 500;
}

.amount-row .amount.primary {
  color: #409eff;
  font-size: 16px;
  font-weight: 600;
}

.amount-row .amount.outstanding {
  color: #f56c6c;
  font-weight: 600;
}

.footer-actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
}

.status-tags {
  display: flex;
  gap: 5px;
}

.order-summary {
  background: #f8f9fa;
  padding: 15px;
  border-radius: 6px;
  margin-bottom: 20px;
}

.amounts {
  display: flex;
  justify-content: space-between;
}

.amount-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 5px;
}

.amount {
  font-weight: bold;
  font-size: 16px;
}

.amount.red {
  color: #ff4d4f;
}

.amount.green {
  color: #10b981;
}

.room-info-detail h4 {
  margin: 0 0 10px 0;
  color: #333;
}

.room-card {
  border: 1px solid #e9ecef;
  border-radius: 6px;
  padding: 15px;
}

.room-header {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.room-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.expandable-sections {
  margin: 20px 0;
}

.order-info {
  margin: 20px 0;
}

.order-info p {
  margin: 5px 0;
  color: #666;
}

.order-colors {
  margin-top: 10px;
}

.more-actions {
  margin: 20px 0;
}

.detail-footer {
  border-top: 1px solid #e9ecef;
  padding-top: 20px;
  display: flex;
  gap: 10px;
  justify-content: flex-end;
}

/* 批量选择效果样式 */
.status-cell.batch-selected {
  background: #b7c8f4 !important;
  border-color: #8ca8eb !important;
  z-index: 5;
}

.status-cell.batch-selected:hover {
  background: #a9bef3 !important;
}

.status-cell.multi-selection-middle-cell,
.status-cell.multi-selection-end-cell {
  border-left-color: transparent !important;
}

.status-cell.multi-selection-start-cell {
  border-top-left-radius: 6px;
  border-bottom-left-radius: 6px;
}

.status-cell.multi-selection-end-cell {
  border-top-right-radius: 6px;
  border-bottom-right-radius: 6px;
}

.batch-selected-icon {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  color: #fff;
  font-size: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 8;
  pointer-events: none;
}

/* 批量操作弹窗样式 */
.batch-dialog-content {
  min-height: 400px;
}

.selection-area {
  display: flex;
  height: 400px;
  gap: 16px;
}

.batch-room-tree {
  flex: 1;
  border: 1px solid #e9ecef;
  border-radius: 8px;
  padding: 0 12px 12px;
  overflow: hidden;
}

.selection-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 46px;
  border-bottom: 1px solid #e9ecef;
}

.selection-count {
  color: #666;
  font-size: 13px;
}

.batch-search-input {
  margin: 12px 0;
}

.batch-tree-list {
  max-height: 320px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding-right: 4px;
}

.batch-type-block {
  border: 1px solid #eef0f3;
  border-radius: 6px;
  padding: 8px;
}

.batch-type-row {
  display: flex;
  align-items: center;
  gap: 8px;
  min-height: 32px;
}

.expand-icon {
  color: #606266;
  font-size: 14px;
  cursor: pointer;
}

.batch-room-list {
  margin-left: 22px;
  padding-top: 6px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.batch-room-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.batch-room-number {
  font-size: 13px;
  color: #303133;
}

.room-type-name {
  color: #333;
  font-weight: 500;
  font-size: 14px;
  flex: 1;
}

.selected-rooms-area {
  flex: 1;
  border: 1px solid #e9ecef;
  border-radius: 8px;
  padding: 0 12px 12px;
  display: flex;
  flex-direction: column;
}

.empty-selection {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
  color: #999;
}

.selected-room-list {
  flex: 1;
  width: 100%;
  overflow-y: auto;
  padding-top: 8px;
}

.selected-room-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 34px;
  padding: 4px 0;
  border-bottom: 1px solid #f2f3f5;
  font-size: 13px;
}

.selected-rooms-title {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
}

.batch-dialog-footer {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
}

/* 脏房悬停提示样式 */
.dirty-hover-tooltip {
  position: fixed;
  background: rgba(0, 0, 0, 0.8);
  color: white;
  padding: 6px 12px;
  border-radius: 4px;
  font-size: 12px;
  z-index: 3100;
  pointer-events: none;
}

/* 脏房清理图标样式 */
.dirty-room-icon {
  position: absolute;
  top: 4px;
  left: 4px;
  background: #faad14;
  color: white;
  border-radius: 50%;
  width: 16px;
  height: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 10px;
}

/* 房间号脏房图标样式 */
.room-dirty-icon {
  position: absolute;
  top: 4px;
  right: 4px;
  background: #faad14;
  color: white;
  border-radius: 50%;
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  box-shadow: 0 2px 4px rgba(250, 173, 20, 0.3);
  z-index: 10;
}

/* 停用房图标样式 */
.closed-room-icon {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  background: #ff4d4f;
  color: white;
  border-radius: 50%;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  z-index: 10;
}

.cell-default-price {
  font-size: 12px;
  color: #fa8c16;
  font-weight: 600;
  line-height: 1;
}

/* 关房弹窗样式 */
.close-room-content {
  padding: 10px 0;
}

.date-range-container {
  display: flex;
  align-items: center;
  width: 100%;
}

.dialog-footer {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
}

.room-change-confirm-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 8px 0;
}

.room-change-summary {
  margin: 0;
  color: #303133;
  line-height: 1.6;
}

.room-change-price-checkbox {
  margin-top: 4px;
}

/* 批量关房详细设置弹窗样式 */
.batch-close-room-content {
  padding: 10px 0;
}

.selected-rooms-display {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  min-height: 32px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  padding: 8px 12px;
  background-color: #fff;
}

.selected-rooms-display .el-tag {
  margin-right: 8px;
  margin-bottom: 4px;
}

.batch-close-room-tag {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 2px 8px;
  margin-right: 8px;
  margin-bottom: 6px;
  border-radius: 4px;
  background: #f5f7fa;
  border: 1px solid #dcdfe6;
}

.batch-close-empty-tip {
  color: #909399;
  font-size: 13px;
}

.date-selector {
  color: #409eff;
  font-size: 14px;
}

/* 搜索建议样式 */
.search-suggestion-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 12px;
  border-bottom: 1px solid #f0f0f0;
  transition: background-color 0.2s;
}

.search-suggestion-item:hover {
  background-color: #f8f9fa;
}

.search-suggestion-item:last-child {
  border-bottom: none;
}

.guest-name {
  font-weight: 500;
  color: #333;
  font-size: 14px;
  flex: 1;
}

.status-tags {
  display: flex;
  gap: 6px;
  align-items: center;
}

/* 操作日志样式 */
.operation-logs {
  padding: 20px 20px 20px 20px;
}

.log-filter-buttons {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  padding-bottom: 12px;
  border-bottom: 1px solid #e4e7ed;
}

.operation-timeline {
  margin-top: 0;
}

.log-item {
  background: #f5f7fa;
  padding: 16px;
  border-radius: 8px;
  border: 1px solid #e4e7ed;
}

.log-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.log-action {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.log-operator {
  font-size: 13px;
  color: #909399;
}

.log-content {
  font-size: 14px;
  color: #606266;
  margin-bottom: 8px;
  line-height: 1.6;
}

.log-details {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #dcdfe6;
}

.detail-item {
  font-size: 13px;
  color: #606266;
  display: flex;
  gap: 8px;
}

.detail-label {
  color: #909399;
  min-width: 80px;
}

.detail-value {
  color: #303133;
  font-weight: 500;
}

/* 补偿/重试样式 */
.compensation-panel {
  padding: 0;
}

.compensation-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.toolbar-label {
  font-size: 13px;
  color: #606266;
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.error-ellipsis {
  display: inline-block;
  max-width: 240px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 渠道信息样式 */
.channel-info-content {
  padding: 0;
}

.channel-header {
  padding: 32px 24px;
  background: linear-gradient(135deg, #7B68EE 0%, #9370DB 100%);
  margin-bottom: 0;
}

.channel-logo-wrapper {
  display: flex;
  align-items: center;
  gap: 20px;
}

.channel-logo {
  width: 100px;
  height: 100px;
  background: #003580;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
}

.logo-text {
  font-size: 20px;
  font-weight: 700;
  color: white;
  font-family: Arial, sans-serif;
  letter-spacing: -0.5px;
}

.channel-header h3 {
  font-size: 28px;
  font-weight: 600;
  margin: 0;
  color: white;
}

.channel-section {
  margin-bottom: 0;
  padding: 24px;
  background: white;
  border-radius: 0;
  border: none;
  border-bottom: 1px solid #e4e7ed;
}

.channel-section:last-of-type {
  border-bottom: none;
}

.channel-section h4 {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 20px 0;
  padding-bottom: 0;
  border-bottom: none;
}

.info-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 16px;
}

.info-item {
  display: grid;
  grid-template-columns: 120px 1fr;
  gap: 16px;
  font-size: 14px;
  align-items: start;
}

.info-item .label {
  color: #606266;
  font-weight: 400;
}

.info-item .value {
  color: #303133;
  font-weight: 400;
}

.price-table {
  margin-top: 0;
}

.price-table :deep(.el-table) {
  border: 1px solid #e4e7ed;
}

.price-table :deep(.el-table th) {
  background: #f5f7fa;
  color: #606266;
  font-weight: 600;
}

.price-table :deep(.el-table td) {
  color: #303133;
}

.special-requests {
  padding: 0;
  background: transparent;
  border-radius: 0;
  min-height: auto;
}

.special-requests p {
  font-size: 14px;
  color: #606266;
  line-height: 1.8;
  margin: 0;
}

.special-requests ul {
  list-style: disc;
  padding-left: 20px;
  margin: 0;
}

.special-requests li {
  font-size: 14px;
  color: #606266;
  line-height: 1.8;
  margin-bottom: 8px;
}

</style>
