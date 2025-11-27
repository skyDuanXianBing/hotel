<template>
  <div class="channel-management">
    <!-- 左侧导航菜单 -->
    <div class="sidebar" :class="{ collapsed: isCollapsed }">
      <!-- 收起导航按钮 -->
      <div class="sidebar-header" @click="toggleSidebar">
        <el-icon class="sidebar-icon"><MenuIcon /></el-icon>
        <span v-if="!isCollapsed" class="sidebar-title">收起导航</span>
        <el-icon v-if="!isCollapsed" class="collapse-icon"><ArrowLeft /></el-icon>
        <el-icon v-else class="collapse-icon"><ArrowRight /></el-icon>
      </div>

      <el-menu class="sidebar-menu" :default-active="activeMenu" @select="handleMenuSelect" :collapse="isCollapsed">
        <el-menu-item index="channel-list">
          <el-icon><List /></el-icon>
          <span>渠道列表</span>
        </el-menu-item>
        <el-menu-item index="price-ratio">
          <el-icon><Money /></el-icon>
          <span>价格比例</span>
        </el-menu-item>
      </el-menu>
    </div>

    <!-- 主内容区域 -->
    <div class="main-content" v-if="!showChannelSettings && !showPriceRatio && activeMenu === 'channel-list'">
      <div class="page-header">
        <h2>渠道管理</h2>
        <div class="header-tabs">
          <el-tabs v-model="activeTab" @tab-click="handleTabClick">
            <el-tab-pane label="OTA渠道" name="ota"></el-tab-pane>
            <el-tab-pane label="查看介绍" name="intro"></el-tab-pane>
          </el-tabs>
        </div>
      </div>

      <!-- OTA渠道内容 -->
      <div v-if="activeTab === 'ota'" class="channel-content">
        <!-- OTA渠道区域 -->
        <div class="channel-section">
          <h3 class="section-title">OTA渠道</h3>
          <div class="channel-grid">
            <div v-for="channel in otaChannels" :key="channel.id" class="channel-card">
              <div class="channel-logo-wrapper">
                <img :src="channel.logoUrl" :alt="channel.name" class="channel-logo-img" />
              </div>
              <h4 class="channel-name">{{ channel.name }}</h4>
              <div class="channel-status">
                <span class="status-dot" :class="channel.connected ? 'connected' : 'disconnected'"></span>
                <span class="status-text">{{ channel.connected ? '已设置' : '未设置' }}</span>
              </div>
              <el-button
                type="primary"
                size="small"
                class="config-btn"
                @click="openSettings(channel)"
              >
                配置
              </el-button>
            </div>
          </div>
        </div>

        <!-- 渠道管理区域 -->
        <div class="channel-section">
          <h3 class="section-title">渠道管理</h3>
          <div class="channel-grid">
            <div v-for="channel in managementChannels" :key="channel.id" class="channel-card">
              <div class="channel-logo-wrapper">
                <img :src="channel.logoUrl" :alt="channel.name" class="channel-logo-img" />
              </div>
              <h4 class="channel-name">{{ channel.name }}</h4>
              <div class="channel-status">
                <span class="status-dot" :class="channel.connected ? 'connected' : 'disconnected'"></span>
                <span class="status-text">{{ channel.connected ? '已设置' : '未设置' }}</span>
              </div>
              <el-button
                type="primary"
                size="small"
                class="config-btn"
                @click="openSettings(channel)"
              >
                配置
              </el-button>
            </div>
          </div>
        </div>
      </div>

      <!-- 查看介绍内容 -->
      <div v-if="activeTab === 'intro'" class="intro-content">
        <div class="intro-section">
          <h3>渠道管理功能介绍</h3>
          <p>通过渠道管理，您可以：</p>
          <ul>
            <li>连接各大OTA平台，实现房量同步</li>
            <li>统一管理多个渠道的房态信息</li>
            <li>自动化价格和库存更新</li>
            <li>查看各渠道的订单数据</li>
          </ul>
        </div>

        <div class="intro-section">
          <h3>支持的主要渠道</h3>
          <div class="channel-list">
            <div v-for="category in channelCategories" :key="category.name" class="category">
              <h4>{{ category.name }}</h4>
              <div class="channel-items">
                <span v-for="item in category.items" :key="item" class="channel-item">
                  {{ item }}
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 开通直连对话框 -->
    <el-dialog
      v-model="showConnectDialog"
      :title="`开始${selectedChannel?.name || ''}直连`"
      width="700px"
      :close-on-click-modal="false"
    >
      <div class="connect-dialog-content">
        <div class="connect-header">
          <p class="connect-description">
            完成{{ selectedChannel?.name || '' }}账号授权后，即可开始直连。<br />
            点击下一步，代表您已经阅读并同意《{{ selectedChannel?.name || '' }}直连须知》。
          </p>
          <div class="channel-logo-large" :class="selectedChannel?.logoClass">
            <span class="logo-text-large">{{ selectedChannel?.logoText }}</span>
          </div>
        </div>

        <!-- 直连须知内容 -->
        <div class="notice-section">
          <h3 class="notice-title">{{ selectedChannel?.name || '' }}直连须知</h3>
          <div class="notice-content">
            <div class="notice-text">
              <p>
                {{ selectedChannel?.name || '' }}直连功能（简称"直连"），是指酒店商户（"您"）在订单来了系统中设置直连后，由订单来了与{{
                  selectedChannel?.name || ''
                }}系统自动建立直接连接，实现您通过订单来了管理{{ selectedChannel?.name || ''
                }}系统中的订单。该功能旨在减少酒店运营的操作成本，提高酒店运营效率。
              </p>
              <p>
                在您开通{{ selectedChannel?.name || ''
                }}直连功能前，请务必认真阅读和遵守本《用户须知》的条款内容，特别是免除或者限制责任的条款、争议解决和法律适用条款。如您对协议有任何疑问或不认可，应立即停止使用服务。当您使用{{
                  selectedChannel?.name || ''
                }}直连功能时，即表明您已充分阅读、理解并接受本《用户须知》的全部内容，本协议经您在线确认后立即生效。协议条款如下：
              </p>

              <h4>1 功能介绍</h4>
              <p>
                1.1 在与{{ selectedChannel?.name || ''
                }}完成直连后，即可通过订单来了PMS管理{{ selectedChannel?.name || ''
                }}的渠道房价、房态，渠道订单将自动落入订单来了。
              </p>
              <p>
                1.2 1个订单来了网络号可与多个{{ selectedChannel?.name || ''
                }}账号直连。点击"开通直连"后，将跳转至{{ selectedChannel?.name || ''
                }}页面进行授权。为确保授权账号无误，授权之前请确保您在{{ selectedChannel?.name || ''
                }}是未登录状态，完成授权后才可以关联房型。
              </p>
              <p>
                1.3 {{ selectedChannel?.name || ''
                }}房源类型为集中式公寓、度假民宿、客栈、农家乐、房车的房源可以直接关联整个房型（允许多个库存），其他类型的房源只能关联单个房间（只允许1个库存）。
              </p>
              <p>
                1.4
                若要新增房源，请您在{{ selectedChannel?.name || ''
                }}后台新增并发布上架后，在订单来了系统中进行房型关联，建立直连关系。
              </p>
              <p>
                1.5 直连后，{{ selectedChannel?.name || ''
                }}订单将自动落入订单来了。闪订订单无需手动确认，普通订单请在渠道订单页面或{{
                  selectedChannel?.name || ''
                }}后台进行接受或拒绝操作。需要注意的是，{{ selectedChannel?.name || ''
                }}订单落单时，处于待付款状态，需等待客人付款后才会变成已预订状态。若您选择渠道订单不排房，将无法在日历房态中看到待付款订单。
              </p>
              <p>
                1.6 {{ selectedChannel?.name || '' }}客人只允许取消订单，不可修改订单。
              </p>

              <h4>2 本平台将可能不定期的修改本《用户须知》的有关条款，一旦条款及服务内容产生变动，本平台将会提示修改内容。如果您继续使用本平台的服务，则视为接受该《用户须知》的条款变动。</h4>

              <h4>3 免责声明</h4>
              <p>
                ●
                您完全理解并同意，本服务涉及到互联网及移动通讯等服务，可能会受到各个环节不稳定因素的影响。因此任何因不可抗力、计算机病毒或黑客攻击、系统不稳定、互联网络、通信线路等其他本公司无法预测或控制的原因，造成的服务中断、取消或终止的风险。您须自行承担以上风险，本公司对服务之及时性、安全性、准确性不做任何保证。
              </p>
              <p>
                ● 您完全理解并同意，您的{{ selectedChannel?.name || ''
                }}账号信息将被订单来了用于直连服务，本公司不会使用您的账号信息进行其他操作，若您的账号信息出现异常，与本公司无关。
              </p>
              <p>
                ●
                本公司需要定期或不定期地对提供网络服务的平台或相关的设备进行检修或者维护，如因此类情况而造成网络服务（包括收费网络服务）在合理时间内的中断，本公司无需为此承担任何责任。本公司保留不经事先通知为维修保养、升级或其他目的暂停全部或部分的网络服务的权利。
              </p>
              <p>
                ●
                您完全理解并同意，除本服务协议另有规定外，鉴于网络服务的特殊性，本公司有权随时变更、中断或终止部分或全部的网络服务，且无需通知您，也无需对您或任何第三方承担任何责任。
              </p>

              <h4>4 适用法律及管辖</h4>
              <p>
                本协议的订立、执行和解释及争议的解决均应适用中华人民共和国法律。如双方就本协议内容或其执行发生任何争议，双方应尽量友好协商解决；协商不成时，任何一方均可向本公司即湖州全都来了网络科技有限公司所在地的人民法院提起诉讼。
              </p>
            </div>
          </div>
        </div>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showConnectDialog = false">取消</el-button>
          <el-button type="primary" @click="startConnection">同意并开始授权</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 渠道设置页面 -->
    <div class="main-content channel-settings-view" v-if="showChannelSettings">
      <div class="settings-header">
        <div class="breadcrumb">
          <el-button type="text" @click="closeChannelSettings" class="back-btn">
            <el-icon><ArrowLeft /></el-icon>
            直连设置
          </el-button>
          <span class="breadcrumb-separator">&gt;</span>
          <span class="breadcrumb-item active">{{ currentSettingChannel?.name || '' }}渠道设置</span>
        </div>
      </div>

      <div class="settings-content">
        <!-- 渠道信息卡片 -->
        <div class="channel-info-card">
          <div class="channel-logo-section">
            <img
              :src="currentSettingChannel?.logoUrl"
              :alt="currentSettingChannel?.name"
              class="channel-logo-large-img"
            />
          </div>
          <div class="channel-desc">
            <h3 class="channel-title-highlight">{{ currentSettingChannel?.name || '' }}渠道设置</h3>
            <p>
              渠道连接是管理渠道房量、房价等信息的关键，一旦连接并匹配成功，Smart Order就能够获取渠道订单信息并允许在PMS中控制房量。如需详细的操作说明，请见帮助中心。
            </p>
          </div>
          <div class="channel-actions">
            <template v-if="!isAirbnbChannel">
              <el-button @click="showConnectionHistory">连接历史</el-button>
            </template>
            <el-button type="primary" @click="isAirbnbChannel ? (showAddAccountDialog = true) : (showAddHotelDialog = true)">
              {{ isAirbnbChannel ? '添加帐户' : '添加酒店' }}
            </el-button>
          </div>
        </div>

        <!-- 标签页 -->
        <div class="channel-tabs-section">
          <el-tabs v-model="channelSettingsTab" class="channel-settings-tabs">
            <!-- Airbnb: 帐户列表 -->
            <el-tab-pane v-if="isAirbnbChannel" label="帐户列表" name="accountList">
              <div class="account-table-section">
                <el-table :data="airbnbAccountList" border class="account-table">
                  <el-table-column prop="account" label="帐户" min-width="280" align="center" />
                  <el-table-column prop="accountCode" label="帐户代码" min-width="280" align="center" />
                  <el-table-column label="操作" min-width="180" align="center" fixed="right">
                    <template #default="{ row }">
                      <el-button type="danger" link size="small" @click="disconnectAirbnbAccount(row)">断开连接</el-button>
                    </template>
                  </el-table-column>
                </el-table>

                <!-- 空状态 -->
                <div v-if="airbnbAccountList.length === 0" class="empty-state">
                  <p class="empty-text">无数据</p>
                </div>
              </div>
            </el-tab-pane>

            <!-- Booking.com: 酒店列表 -->
            <el-tab-pane v-if="!isAirbnbChannel" label="酒店列表" name="hotelList">
              <!-- 酒店列表表格 -->
              <div class="hotel-table-section">
                <el-table :data="hotelList" border class="hotel-table">
                  <el-table-column prop="hotelCode" label="酒店代码" min-width="180" align="center" />
                  <el-table-column prop="hotelName" label="酒店名称" min-width="220" align="center" />
                  <el-table-column prop="priceMode" label="价格模式" min-width="180" align="center" />
                  <el-table-column prop="status" label="状态" min-width="120" align="center" />
                  <el-table-column label="操作" min-width="180" align="center" fixed="right">
                    <template #default="{ row }">
                      <el-button type="primary" link size="small" @click="editHotel(row)">编辑</el-button>
                      <el-button type="danger" link size="small" @click="disconnectHotel(row)">断开连接</el-button>
                    </template>
                  </el-table-column>
                </el-table>

                <!-- 空状态 -->
                <div v-if="hotelList.length === 0" class="empty-state">
                  <div class="empty-icon">
                    <svg
                      width="120"
                      height="120"
                      viewBox="0 0 120 120"
                      fill="none"
                      xmlns="http://www.w3.org/2000/svg"
                    >
                      <path
                        d="M60 110C87.6142 110 110 87.6142 110 60C110 32.3858 87.6142 10 60 10C32.3858 10 10 32.3858 10 60C10 87.6142 32.3858 110 60 110Z"
                        fill="#F0F2F5"
                      />
                      <path
                        d="M45 50C48.3137 50 51 47.3137 51 44C51 40.6863 48.3137 38 45 38C41.6863 38 39 40.6863 39 44C39 47.3137 41.6863 50 45 50Z"
                        fill="#D9D9D9"
                      />
                      <path
                        d="M75 50C78.3137 50 81 47.3137 81 44C81 40.6863 78.3137 38 75 38C71.6863 38 69 40.6863 69 44C69 47.3137 71.6863 50 75 50Z"
                        fill="#D9D9D9"
                      />
                      <path
                        d="M80 68C80 75.732 71.046 82 60 82C48.954 82 40 75.732 40 68"
                        stroke="#D9D9D9"
                        stroke-width="4"
                        stroke-linecap="round"
                      />
                    </svg>
                  </div>
                  <p class="empty-text">无数据</p>
                </div>
              </div>
            </el-tab-pane>
            <el-tab-pane label="映射" name="mapping">
              <div class="mapping-content">
                <!-- 映射筛选区域 -->
                <div class="mapping-filter-bar">
                  <div class="filter-left">
                    <div class="filter-item">
                      <span class="filter-label">直连状态</span>
                      <el-select v-model="mappingConnectionStatus" placeholder="请选择" style="width: 150px">
                        <el-option label="全部" value="all" />
                        <el-option label="已连接" value="connected" />
                        <el-option label="未连接" value="disconnected" />
                      </el-select>
                    </div>
                  </div>
                  <div class="filter-right">
                    <div class="filter-item">
                      <span class="filter-label">酒店</span>
                      <el-select v-model="selectedHotelId" placeholder="请选择酒店" style="width: 180px">
                        <el-option
                          v-for="hotel in hotelList"
                          :key="hotel.id"
                          :label="hotel.hotelName"
                          :value="hotel.id"
                        />
                      </el-select>
                    </div>
                    <el-button>导入未来订单</el-button>
                    <el-button type="primary" @click="refreshChannelInfo">
                      <el-icon><Refresh /></el-icon>
                      刷新渠道信息
                    </el-button>
                  </div>
                </div>

                <!-- 映射表格 -->
                <div class="mapping-table-section">
                  <el-table
                    :data="flattenedMappingData"
                    border
                    class="mapping-table"
                    :span-method="mappingSpanMethod"
                  >
                    <el-table-column :label="isAirbnbChannel ? 'Airbnb Room Type' : 'Booking.com Room Type'" min-width="180" align="left">
                      <template #default="{ row }">
                        <div class="room-type-cell">
                          <div class="room-name">{{ row.channelRoomType }}</div>
                          <div class="room-id">({{ row.channelRoomId }})</div>
                        </div>
                      </template>
                    </el-table-column>
                    <el-table-column label="PMS房型" min-width="160" align="center">
                      <template #default="{ row }">
                        <template v-if="editingRoomId === row.roomGroupId">
                          <el-select
                            v-model="row.selectedPmsRoom"
                            placeholder="请选择"
                            style="width: 140px"
                          >
                            <el-option
                              v-for="pms in pmsRoomOptions"
                              :key="pms.value"
                              :label="pms.label"
                              :value="pms.value"
                            />
                          </el-select>
                        </template>
                        <template v-else>
                          <div class="room-type-cell">
                            <div class="room-name">{{ row.pmsRoomType }}</div>
                          </div>
                        </template>
                      </template>
                    </el-table-column>
                    <el-table-column :label="isAirbnbChannel ? 'Airbnb 价格计划' : 'Booking.com 价格计划'" min-width="260" align="left">
                      <template #default="{ row }">
                        <div class="room-type-cell">
                          <div class="room-name">{{ row.channelPricePlan }}</div>
                          <div class="room-id">({{ row.channelPricePlanId }})</div>
                        </div>
                      </template>
                    </el-table-column>
                    <el-table-column label="PMS价格计划" min-width="160" align="center">
                      <template #default="{ row }">
                        <template v-if="editingRoomId === row.roomGroupId">
                          <el-select
                            v-model="row.selectedPmsPricePlan"
                            placeholder="请选择"
                            style="width: 140px"
                          >
                            <el-option
                              v-for="plan in pmsPricePlanOptions"
                              :key="plan.value"
                              :label="plan.label"
                              :value="plan.value"
                            />
                          </el-select>
                        </template>
                        <template v-else>
                          <span>{{ row.pmsPricePlan || '-' }}</span>
                        </template>
                      </template>
                    </el-table-column>
                    <el-table-column label="状态" min-width="100" align="center">
                      <template #default="{ row }">
                        <span :class="['mapping-status', getMappingStatusClass(row.status)]">
                          {{ getMappingStatusText(row.status) }}
                          <el-icon v-if="row.status === 'invalid'" class="status-icon"><QuestionFilled /></el-icon>
                        </span>
                      </template>
                    </el-table-column>
                    <el-table-column label="操作" min-width="140" align="center" fixed="right">
                      <template #default="{ row }">
                        <template v-if="isAirbnbChannel && row.isFirstInGroup">
                          <el-button type="danger" link size="small" @click="disconnectRoomMapping(row)">断开连接</el-button>
                          <el-button type="primary" link size="small" @click="manageRoomMapping(row)">管理</el-button>
                        </template>
                        <template v-else-if="!isAirbnbChannel">
                          <template v-if="editingRoomId === row.roomGroupId && row.isFirstInGroup">
                            <el-button link size="small" @click="cancelEditMapping">取消</el-button>
                            <el-button type="primary" size="small" @click="saveRoomMapping(row.roomGroupId)">保存</el-button>
                          </template>
                          <template v-else-if="!editingRoomId && row.isFirstInGroup">
                            <el-button type="primary" link size="small" @click="editRoomMapping(row)">编辑</el-button>
                          </template>
                        </template>
                      </template>
                    </el-table-column>
                  </el-table>

                  <!-- 空状态 -->
                  <div v-if="flattenedMappingData.length === 0" class="empty-state">
                    <p class="empty-text">暂无房型映射数据，请先添加酒店并完成授权</p>
                  </div>
                </div>
              </div>
            </el-tab-pane>
            <el-tab-pane label="日历" name="calendar">
              <div class="calendar-content">
                <!-- 日历筛选区域 -->
                <div class="calendar-filter-bar">
                  <div class="filter-left">
                    <div class="filter-item">
                      <span class="filter-label">选择日期</span>
                      <el-date-picker
                        v-model="calendarStartDate"
                        type="date"
                        placeholder="选择日期"
                        format="YYYY/MM/DD"
                        value-format="YYYY-MM-DD"
                        style="width: 160px"
                      />
                    </div>
                    <div class="filter-item">
                      <span class="filter-label">房型&房价</span>
                      <el-select v-model="calendarRoomType" placeholder="全部" style="width: 150px">
                        <el-option label="全部" value="all" />
                      </el-select>
                    </div>
                    <div class="filter-item">
                      <span class="filter-label">展示项</span>
                      <el-select v-model="calendarDisplayItem" placeholder="全部" style="width: 120px">
                        <el-option label="全部" value="all" />
                        <el-option label="价格" value="price" />
                        <el-option label="房量" value="inventory" />
                      </el-select>
                    </div>
                  </div>
                  <div class="filter-right">
                    <el-button @click="syncFromCalendar">从日历同步</el-button>
                    <el-button type="primary" @click="fullRefresh">全量刷新</el-button>
                    <div class="filter-item">
                      <span class="filter-label">酒店</span>
                      <el-select v-model="selectedHotelId" placeholder="请选择酒店" style="width: 180px">
                        <el-option
                          v-for="hotel in hotelList"
                          :key="hotel.id"
                          :label="hotel.hotelName"
                          :value="hotel.id"
                        />
                      </el-select>
                    </div>
                  </div>
                </div>

                <!-- 日历表格 -->
                <div class="calendar-table-section">
                  <el-table :data="calendarData" border class="calendar-table" :span-method="calendarSpanMethod">
                    <el-table-column label="日期" width="120" fixed="left" align="center">
                      <template #default="{ row }">
                        <span class="row-label">{{ row.label }}</span>
                      </template>
                    </el-table-column>
                    <el-table-column
                      v-for="date in calendarDates"
                      :key="date.value"
                      :label="date.label"
                      min-width="100"
                      align="center"
                    >
                      <template #header>
                        <div class="date-header">
                          <div class="date-day">{{ date.day }}</div>
                          <div class="date-weekday">{{ date.weekday }}</div>
                        </div>
                      </template>
                      <template #default="{ row }">
                        <template v-if="row.type === 'inventory'">
                          <span>{{ row.values[date.value] ?? 0 }}</span>
                        </template>
                        <template v-else-if="row.type === 'price'">
                          <span class="price-value">¥{{ row.values[date.value] ?? '-' }}</span>
                        </template>
                        <template v-else-if="row.type === 'checkbox'">
                          <el-checkbox v-model="row.values[date.value]" disabled />
                        </template>
                        <template v-else>
                          <span>{{ row.values[date.value] ?? '-' }}</span>
                        </template>
                      </template>
                    </el-table-column>
                  </el-table>

                  <!-- 空状态 -->
                  <div v-if="calendarData.length === 0" class="empty-state">
                    <p class="empty-text">暂无日历数据，请先选择酒店</p>
                  </div>
                </div>
              </div>
            </el-tab-pane>

            <!-- Airbnb: 房量设置 -->
            <el-tab-pane v-if="isAirbnbChannel" label="房量设置" name="roomSettings">
              <div class="room-settings-content">
                <!-- 日期选择器 -->
                <div class="room-settings-header">
                  <div class="date-selector">
                    <span class="selector-label">选择日期</span>
                    <el-date-picker
                      v-model="roomSettingsStartDate"
                      type="date"
                      placeholder="选择日期"
                      format="YYYY-MM-DD"
                      value-format="YYYY-MM-DD"
                      style="width: 160px"
                      @change="generateRoomSettingsData"
                    />
                  </div>
                </div>

                <!-- 房量设置表格 -->
                <div class="room-settings-table-section">
                  <el-table
                    :data="roomSettingsData"
                    border
                    class="room-settings-table"
                  >
                    <el-table-column label="Airbnb Room Type" width="250" fixed="left" align="left">
                      <template #default="{ row }">
                        <div class="room-type-info">
                          <div class="room-name">{{ row.airbnbRoomType }}</div>
                        </div>
                      </template>
                    </el-table-column>
                    <el-table-column label="PMS房型" width="180" fixed="left" align="center">
                      <template #default="{ row }">
                        <span>{{ row.pmsRoomType }}</span>
                      </template>
                    </el-table-column>
                    <el-table-column
                      v-for="date in roomSettingsDates"
                      :key="date.value"
                      :label="date.label"
                      min-width="100"
                      align="center"
                    >
                      <template #header>
                        <div class="date-header">
                          <div class="date-day">{{ date.day }}</div>
                          <div class="date-weekday">{{ date.weekday }}</div>
                        </div>
                      </template>
                      <template #default="{ row }">
                        <span>{{ row.values[date.value] ?? '' }}</span>
                      </template>
                    </el-table-column>
                  </el-table>

                  <!-- 空状态 -->
                  <div v-if="roomSettingsData.length === 0" class="empty-state">
                    <p class="empty-text">暂无房量设置数据</p>
                  </div>
                </div>
              </div>
            </el-tab-pane>
          </el-tabs>
        </div>
      </div>
    </div>

    <!-- 价格比例页面 -->
    <div class="main-content price-ratio-view" v-if="showPriceRatio">
      <!-- <div class="settings-header">
        <div class="breadcrumb">
          <el-button type="text" @click="closePriceRatio" class="back-btn">
            <el-icon><ArrowLeft /></el-icon>
            收起导航
          </el-button>
          <span class="breadcrumb-separator">/</span>
          <span class="breadcrumb-item active">价格比例</span>
        </div>
      </div> -->

      <div class="settings-content">
        <div class="price-ratio-table-container">
          <el-table :data="priceRatioData" border stripe class="price-ratio-table" v-loading="priceRatioLoading">
            <el-table-column prop="channel" label="渠道" min-width="150" align="center" />
            <el-table-column prop="ratio" label="价格比例" min-width="200" align="center" />
            <el-table-column label="操作" width="150" align="center">
              <template #default="{ row }">
                <el-button type="text" size="small" @click="editPriceRatio(row)">编辑</el-button>
              </template>
            </el-table-column>
          </el-table>
          <!-- 空状态 -->
          <div v-if="!priceRatioLoading && priceRatioData.length === 0" class="empty-state">
            <p class="empty-text">暂无渠道价格比例数据，请先在渠道管理中配置渠道</p>
          </div>
        </div>
      </div>
    </div>

    <!-- 编辑价格比例对话框 -->
    <el-dialog v-model="showEditRatioDialog" title="编辑价格比例" width="500px">
      <div class="edit-ratio-form" v-if="currentEditingRatio">
        <div class="form-item">
          <div class="form-label">OTA名称</div>
          <div class="form-value">{{ currentEditingRatio.channel }}</div>
        </div>
        <div class="form-item">
          <div class="form-label">价格调整</div>
          <div class="form-value adjustment-row">
            <el-select v-model="currentEditingRatio.adjustmentType" class="adjustment-type-select">
              <el-option label="更便宜" value="cheaper" />
              <el-option label="更贵" value="expensive" />
            </el-select>
            <el-input-number
              v-model="currentEditingRatio.adjustmentValue"
              :min="0"
              :max="currentEditingRatio.adjustmentUnit === '%' ? 100 : 999999"
              :controls="false"
              class="adjustment-value-input"
              placeholder="数值"
            />
            <el-select v-model="currentEditingRatio.adjustmentUnit" class="adjustment-unit-select">
              <el-option label="%" value="%" />
              <el-option label="¥" value="¥" />
            </el-select>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="showEditRatioDialog = false">取消</el-button>
        <el-button type="primary" @click="savePriceRatio">保存</el-button>
      </template>
    </el-dialog>

    <!-- 添加账号对话框 -->
    <el-dialog v-model="showAddAccountDialog" title="添加美团民宿账号" width="500px">
      <div class="add-account-form">
        <p>请输入美团民宿门店账号信息：</p>
        <el-form :model="newAccount" label-width="100px">
          <el-form-item label="账号名称">
            <el-input v-model="newAccount.name" placeholder="请输入账号名称"></el-input>
          </el-form-item>
          <el-form-item label="登录账号">
            <el-input v-model="newAccount.username" placeholder="请输入登录账号"></el-input>
          </el-form-item>
          <el-form-item label="密码">
            <el-input
              v-model="newAccount.password"
              type="password"
              placeholder="请输入密码"
            ></el-input>
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="showAddAccountDialog = false">取消</el-button>
        <el-button type="primary" @click="addAccount">添加</el-button>
      </template>
    </el-dialog>

    <!-- 添加酒店对话框 -->
    <el-dialog v-model="showAddHotelDialog" :title="`直连${currentSettingChannel?.name || ''}`" width="700px">
      <div class="add-hotel-dialog">
        <div class="dialog-header-section">
          <div class="dialog-description">
            <p>{{ currentSettingChannel?.name || '' }}账户授权完成后，可以启动直连，申请按钮表示您已阅读并同意{{ currentSettingChannel?.name || '' }}的直连说明。</p>
            <p class="notice-text">注意：如果你还未注册{{ currentSettingChannel?.name || '' }}，请先注册{{ currentSettingChannel?.name || '' }}</p>
          </div>
          <div class="dialog-logo">
            <img
              :src="currentSettingChannel?.logoUrl"
              :alt="currentSettingChannel?.name"
              class="dialog-logo-img"
            />
          </div>
        </div>

        <div class="agreement-section">
          <h3 class="agreement-title">{{ currentSettingChannel?.name || '' }}直连</h3>
          <div class="agreement-content">
            <p>{{ currentSettingChannel?.name || '' }}直连服务功能（"直连服务"）是指在适当许可（"您"）在SmartOrder系统中设置直连服务后，SmartOrder系统和{{ currentSettingChannel?.name || '' }}系统自动建立直连，一旦您连接到{{ currentSettingChannel?.name || '' }}并完成联系，您可以在{{ currentSettingChannel?.name || '' }}平台上创建新的房源，管理将格可用性，并自动将订单输入SmartOrder系统，从而降低运营成本并提高运营效率。</p>

            <p>在启用{{ currentSettingChannel?.name || '' }}直连服务之前，请务必仔细阅读并遵守本《用户须知》的条款内容，特别是免责条款理解纠纷解决和法律适用条款。如果您对协议有疑问或不同意内容，请立即停止使用服务。当您使用{{ currentSettingChannel?.name || '' }}直连时，协议表示在线确认后立即生效，协议以下：</p>

            <h4>1 功能介绍</h4>
            <p><!-- 功能介绍内容 --></p>
          </div>
        </div>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showAddHotelDialog = false">取消</el-button>
          <el-button type="primary" @click="startAuthorization">同意并开始授权</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 连接历史对话框 -->
    <el-dialog v-model="showHistoryDialog" title="连接历史" width="1000px">
      <div class="history-dialog">
        <el-table :data="connectionHistory" border class="history-table">
          <el-table-column prop="hotelCode" label="酒店代码" min-width="120" align="center" />
          <el-table-column prop="storeType" label="Agoda门店类型" min-width="150" align="center" />
          <el-table-column prop="hotelName" label="酒店名称" min-width="200" align="center" />
          <el-table-column prop="status" label="状态" min-width="100" align="center" />
          <el-table-column prop="note" label="备注" min-width="150" align="center" />
          <el-table-column prop="createTime" label="创建时间" min-width="180" align="center" />
        </el-table>

        <!-- 空状态 -->
        <div v-if="connectionHistory.length === 0" class="empty-state">
          <div class="empty-icon">
            <svg
              width="120"
              height="120"
              viewBox="0 0 120 120"
              fill="none"
              xmlns="http://www.w3.org/2000/svg"
            >
              <path
                d="M60 110C87.6142 110 110 87.6142 110 60C110 32.3858 87.6142 10 60 10C32.3858 10 10 32.3858 10 60C10 87.6142 32.3858 110 60 110Z"
                fill="#F0F2F5"
              />
              <path
                d="M45 50C48.3137 50 51 47.3137 51 44C51 40.6863 48.3137 38 45 38C41.6863 38 39 40.6863 39 44C39 47.3137 41.6863 50 45 50Z"
                fill="#D9D9D9"
              />
              <path
                d="M75 50C78.3137 50 81 47.3137 81 44C81 40.6863 78.3137 38 75 38C71.6863 38 69 40.6863 69 44C69 47.3137 71.6863 50 75 50Z"
                fill="#D9D9D9"
              />
              <path
                d="M80 68C80 75.732 71.046 82 60 82C48.954 82 40 75.732 40 68"
                stroke="#D9D9D9"
                stroke-width="4"
                stroke-linecap="round"
              />
            </svg>
          </div>
          <p class="empty-text">无数据</p>
        </div>
      </div>
    </el-dialog>

    <!-- 预定设置抽屉 -->
    <el-drawer
      v-model="showBookingSettingsDrawer"
      title="预定设置"
      direction="rtl"
      size="400px"
      :before-close="closeBookingSettingsDrawer"
    >
      <div class="booking-settings-form">
        <!-- 提前预订量 -->
        <div class="form-item">
          <div class="form-label">
            <span>提前预订量</span>
            <el-icon class="help-icon"><QuestionFilled /></el-icon>
          </div>
          <div class="form-desc">你需要客人提前多少小时预订?</div>
          <div class="form-control">
            <el-select v-model="bookingSettingsForm.advanceBookingHours" placeholder="请选择">
              <el-option :label="`${i} 小时`" :value="i" v-for="i in [1, 2, 3, 6, 12, 24, 48]" :key="i" />
            </el-select>
            <span class="control-unit">小时</span>
          </div>
        </div>

        <!-- 未在邀请时间完成 -->
        <div class="form-item">
          <div class="form-desc">未在您的邀请时间段完成的预订将变为"预订申请"。</div>
          <el-radio-group v-model="bookingSettingsForm.requireApproval">
            <el-radio :label="true">是</el-radio>
            <el-radio :label="false">否</el-radio>
          </el-radio-group>
        </div>

        <!-- 准备时间 -->
        <div class="form-item">
          <div class="form-label">
            <span>准备时间</span>
          </div>
          <div class="form-desc">预订日期和入住日之间隔最大天数。</div>
          <div class="form-control">
            <el-select v-model="bookingSettingsForm.preparationNights" placeholder="请选择">
              <el-option label="无" :value="0" />
              <el-option :label="`${i} 晚`" :value="i" v-for="i in [1, 2, 3, 5, 7]" :key="i" />
            </el-select>
            <span class="control-unit">晚</span>
          </div>
        </div>

        <!-- 预订开放期 -->
        <div class="form-item">
          <div class="form-label">
            <span>预订开放期</span>
            <el-icon class="help-icon"><QuestionFilled /></el-icon>
          </div>
          <div class="form-desc">预订日期和入住日之间隔最大天数。</div>
          <div class="form-control">
            <el-select v-model="bookingSettingsForm.bookingWindowDays" placeholder="请选择">
              <el-option :label="`${i} 天`" :value="i" v-for="i in [30, 60, 90, 180, 365, 730]" :key="i" />
            </el-select>
            <span class="control-unit">天</span>
          </div>
        </div>

        <!-- 入住窗口 -->
        <div class="form-item">
          <div class="form-label">
            <span>入住窗口</span>
          </div>

          <!-- 入住开始时间 -->
          <div class="form-subitem">
            <div class="form-desc">入住开始时间</div>
            <el-select v-model="bookingSettingsForm.checkInStartTime" placeholder="请选择">
              <el-option
                v-for="hour in 24"
                :key="hour"
                :label="`${String(hour - 1).padStart(2, '0')}:00`"
                :value="`${String(hour - 1).padStart(2, '0')}:00`"
              />
            </el-select>
          </div>

          <!-- 入住结束时间 -->
          <div class="form-subitem">
            <div class="form-desc">入住结束时间</div>
            <el-select v-model="bookingSettingsForm.checkInEndTime" placeholder="请选择">
              <el-option label="不限" value="" />
              <el-option
                v-for="hour in 24"
                :key="hour"
                :label="`${String(hour - 1).padStart(2, '0')}:00`"
                :value="`${String(hour - 1).padStart(2, '0')}:00`"
              />
            </el-select>
          </div>
        </div>

        <!-- 离店时间 -->
        <div class="form-item">
          <div class="form-label">
            <span>离店时间</span>
          </div>
          <div class="form-subitem">
            <div class="form-desc">之前离店</div>
            <el-select v-model="bookingSettingsForm.checkOutTime" placeholder="请选择">
              <el-option
                v-for="hour in 24"
                :key="hour"
                :label="`${String(hour - 1).padStart(2, '0')}:00`"
                :value="`${String(hour - 1).padStart(2, '0')}:00`"
              />
            </el-select>
          </div>
        </div>
      </div>

      <!-- 抽屉底部按钮 -->
      <template #footer>
        <div class="drawer-footer">
          <el-button @click="closeBookingSettingsDrawer">取消</el-button>
          <el-button type="primary" @click="saveBookingSettings">保存</el-button>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowLeft, ArrowRight, Setting, Connection, Remove, InfoFilled, Menu as MenuIcon, List, Money, Refresh, QuestionFilled } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getChannelPriceAdjustments,
  updateChannelPriceAdjustment,
  type ChannelPriceAdjustmentDTO,
  type PriceAdjustmentType,
} from '@/api/pricelabs'
import { getAllChannels, type ChannelDTO } from '@/api/channel'

const router = useRouter()

// 侧边栏折叠状态
const isCollapsed = ref(false)

const toggleSidebar = () => {
  isCollapsed.value = !isCollapsed.value
}

// 响应式数据
const activeMenu = ref('channel-list')
const activeTab = ref('ota')
const showConnectDialog = ref(false)
const selectedChannel = ref<any>(null)
const showChannelSettings = ref(false)
const currentSettingChannel = ref<any>(null)
const settingsTab = ref('房源管理')
const channelSettingsTab = ref('hotelList')
const selectedAccount = ref('all')
const selectedStatus = ref('all')
const showAddAccountDialog = ref(false)
const showEditRatioDialog = ref(false)
interface PriceRatioEdit {
  channelId: number
  channel: string
  ratio: string
  adjustmentType: 'cheaper' | 'expensive'
  adjustmentValue: number
  adjustmentUnit: '%' | '¥'
  autoSyncPrice: boolean
  backendAdjustmentType: PriceAdjustmentType
}
const currentEditingRatio = ref<PriceRatioEdit | null>(null)
const priceRatioLoading = ref(false)
const showPriceRatio = ref(false)
const showAddHotelDialog = ref(false)
const showHistoryDialog = ref(false)

// 酒店列表数据
const hotelList = ref<any[]>([])

// 连接历史数据
const connectionHistory = ref<any[]>([])

// Airbnb 帐户列表数据
interface AirbnbAccount {
  id: number
  account: string
  accountCode: string
}
const airbnbAccountList = ref<AirbnbAccount[]>([])

// 预定设置抽屉
const showBookingSettingsDrawer = ref(false)
const currentManagingRoom = ref<FlattenedMappingItem | null>(null)

// 预定设置表单数据
interface BookingSettings {
  advanceBookingHours: number // 提前预订量（小时）
  requireApproval: boolean // 是否需要申请
  preparationNights: number // 准备时间（晚）
  bookingWindowDays: number // 预订开放期（天）
  checkInStartTime: string // 入住开始时间
  checkInEndTime: string // 入住结束时间（不限用空字符串）
  checkOutTime: string // 离店时间
}

const bookingSettingsForm = ref<BookingSettings>({
  advanceBookingHours: 2,
  requireApproval: true,
  preparationNights: 0,
  bookingWindowDays: 365,
  checkInStartTime: '16:00',
  checkInEndTime: '',
  checkOutTime: '10:00',
})

// 判断当前渠道类型
const isAirbnbChannel = computed(() => {
  return currentSettingChannel.value?.name === 'Airbnb'
})

const isBookingChannel = computed(() => {
  return currentSettingChannel.value?.name === 'Booking.com'
})

// 映射页面数据
const mappingConnectionStatus = ref('all')
const selectedHotelId = ref<number | null>(null)

// 编辑状态
const editingRoomId = ref<string | null>(null)

// 房型映射数据 - 价格计划项
interface PricePlanMapping {
  id: string
  channelPricePlan: string
  channelPricePlanId: string
  pmsPricePlan: string | null
  selectedPmsPricePlan: string | null
  status: 'connected' | 'disconnected' | 'invalid'  // 已连接、未直连、无效
}

// 房型映射数据 - 房型组
interface RoomMappingGroup {
  roomGroupId: string
  channelRoomType: string
  channelRoomId: string
  pmsRoomType: string | null
  selectedPmsRoom: string | null
  pricePlans: PricePlanMapping[]
}

// 扁平化后的映射数据项（用于表格展示）
interface FlattenedMappingItem {
  id: string
  roomGroupId: string
  channelRoomType: string
  channelRoomId: string
  pmsRoomType: string | null
  selectedPmsRoom: string | null
  channelPricePlan: string
  channelPricePlanId: string
  pmsPricePlan: string | null
  selectedPmsPricePlan: string | null
  status: 'connected' | 'disconnected' | 'invalid'
  isFirstInGroup: boolean
  groupRowCount: number
}

const roomMappingData = ref<RoomMappingGroup[]>([])

// 扁平化的映射数据（用于表格展示）
const flattenedMappingData = ref<FlattenedMappingItem[]>([])

// PMS房型选项
const pmsRoomOptions = ref<{ value: string; label: string }[]>([
  { value: 'Tanpopo Inn101', label: 'Tanpopo Inn101' },
  { value: 'Tanpopo Inn102', label: 'Tanpopo Inn102' },
  { value: 'Tanpopo Inn103', label: 'Tanpopo Inn103' },
  { value: 'Tanpopo Inn104', label: 'Tanpopo Inn104' },
])

// PMS价格计划选项
const pmsPricePlanOptions = ref<{ value: string; label: string }[]>([
  { value: 'Rack Rate', label: 'Rack Rate' },
  { value: 'Standard Rate', label: 'Standard Rate' },
])

// 日历页面数据
const calendarStartDate = ref(new Date().toISOString().split('T')[0])
const calendarRoomType = ref('all')
const calendarDisplayItem = ref('all')

// 日历日期列表
interface CalendarDate {
  value: string
  label: string
  day: string
  weekday: string
}
const calendarDates = ref<CalendarDate[]>([])

// 日历表格数据
interface CalendarRow {
  id: string
  label: string
  type: 'title' | 'inventory' | 'price' | 'number' | 'checkbox'
  roomId?: string
  values: Record<string, any>
}
const calendarData = ref<CalendarRow[]>([])

// Airbnb 房量设置数据
const roomSettingsStartDate = ref(new Date().toISOString().split('T')[0])
const roomSettingsDates = ref<CalendarDate[]>([])

interface RoomSettingsRow {
  id: string
  airbnbRoomType: string
  pmsRoomType: string
  values: Record<string, any>
}
const roomSettingsData = ref<RoomSettingsRow[]>([])

// 价格比例数据
interface PriceRatioItem {
  channelId: number
  channel: string
  ratio: string
  adjustmentType: PriceAdjustmentType
  adjustmentValue: number | null
  autoSyncPrice: boolean
}
const priceRatioData = ref<PriceRatioItem[]>([])

// 账号数据
const channelAccounts = ref([
  {
    id: '1',
    name: 'iHQ895664016',
    roomCount: 0,
  },
])

// 新账号表单
const newAccount = ref({
  name: '',
  username: '',
  password: '',
})

// 渠道 Logo 映射表
const channelLogoMap: Record<string, string> = {
  'Agoda': 'https://cdn.worldvectorlogo.com/logos/agoda-1.svg',
  'Airbnb': 'https://upload.wikimedia.org/wikipedia/commons/6/69/Airbnb_Logo_B%C3%A9lo.svg',
  'Booking.com': 'https://upload.wikimedia.org/wikipedia/commons/b/be/Booking.com_logo.svg',
  'Traveloka': 'https://upload.wikimedia.org/wikipedia/commons/5/5b/Traveloka_logo.svg',
  'Trip.com': 'https://ak-d.tripcdn.com/images/0ww5h12000c6vhxm53B87.png',
  'Expedia': 'https://upload.wikimedia.org/wikipedia/commons/5/5b/Expedia_2012_logo.svg',
  'Tiket.com': 'https://www.tiket.com/img/tiket-logo.svg',
  'HostelWorld': 'https://a.hwstatic.com/image/upload/f_auto,q_auto,h_63/v1/propertyimages/0/8914/x5ecdkqgtrzfmcyiykfb',
  'TuJia': 'https://pages.c-ctrip.com/hotels/wuhan/tujia-logo.png',
  'Neppan': 'https://via.placeholder.com/120x60/FF6B35/FFFFFF?text=Neppan',
}

// OTA渠道数据（从后端加载）
interface ChannelItem {
  id: number
  name: string
  logoUrl: string
  connected: boolean
}
const otaChannels = ref<ChannelItem[]>([])

// 渠道管理数据（从后端加载）
const managementChannels = ref<ChannelItem[]>([])

// 加载状态
const channelsLoading = ref(false)

// 渠道分类
const channelCategories = ref([
  {
    name: '国内OTA',
    items: ['携程', '去哪儿', '美团', '飞猪', '途牛', '同程', '艺龙'],
  },
  {
    name: '民宿平台',
    items: ['途家', '小猪民宿', '木鸟民宿', '榛果民宿', 'Airbnb'],
  },
  {
    name: '国际OTA',
    items: ['Booking.com', 'Expedia', 'Agoda', 'Hotels.com', 'Priceline'],
  },
])

// 加载渠道数据
const loadChannels = async () => {
  channelsLoading.value = true
  try {
    const response = await getAllChannels()
    if (response.success && response.data) {
      // 将渠道数据转换为前端格式，并添加 logoUrl
      const channels = response.data.map((channel: ChannelDTO) => ({
        id: channel.id,
        name: channel.name,
        logoUrl: channelLogoMap[channel.name] || 'https://via.placeholder.com/120x60/409EFF/FFFFFF?text=' + encodeURIComponent(channel.name),
        connected: false, // TODO: 从后端获取连接状态
      }))

      // 根据渠道名称分类：Neppan 归入管理渠道，其他归入 OTA 渠道
      otaChannels.value = channels.filter((ch: ChannelItem) => ch.name !== 'Neppan')
      managementChannels.value = channels.filter((ch: ChannelItem) => ch.name === 'Neppan')
    }
  } catch (error) {
    console.error('加载渠道数据失败:', error)
    ElMessage.error('加载渠道数据失败')
  } finally {
    channelsLoading.value = false
  }
}

// 加载价格比例数据
const loadPriceRatioData = async () => {
  priceRatioLoading.value = true
  try {
    const response = await getChannelPriceAdjustments()
    if (response.success && response.data) {
      priceRatioData.value = response.data.map((item: ChannelPriceAdjustmentDTO) => {
        // 将后端数据转换为前端显示格式
        const adjustmentValue = item.adjustmentValue ?? 0
        let ratio: string
        if (adjustmentValue === 0) {
          ratio = '等同于基本价格'
        } else {
          const unit = item.adjustmentType === 'FIXED' ? '¥' : '%'
          const typeText = adjustmentValue > 0 ? '贵' : '便宜'
          ratio = `${Math.abs(adjustmentValue)} ${unit} 比基准价${typeText}`
        }
        return {
          channelId: item.channelId,
          channel: item.channelName,
          ratio: ratio,
          adjustmentType: item.adjustmentType,
          adjustmentValue: item.adjustmentValue,
          autoSyncPrice: item.autoSyncPrice,
        }
      })
    }
  } catch (error) {
    console.error('加载价格比例数据失败:', error)
    ElMessage.error('加载价格比例数据失败')
  } finally {
    priceRatioLoading.value = false
  }
}

// 方法
const handleMenuSelect = (index: string) => {
  activeMenu.value = index
  if (index === 'price-ratio') {
    showPriceRatio.value = true
    showChannelSettings.value = false
    // 加载价格比例数据
    loadPriceRatioData()
  } else if (index === 'channel-list') {
    showPriceRatio.value = false
    showChannelSettings.value = false
  }
}

const handleTabClick = (tab: any) => {
  activeTab.value = tab.name
}

const connectChannel = (channel: any) => {
  console.log('连接渠道:', channel.name)
  selectedChannel.value = channel
  showConnectDialog.value = true
}

const openSettings = (channel: any) => {
  console.log('打开设置:', channel.name)
  currentSettingChannel.value = channel
  showChannelSettings.value = true
  settingsTab.value = '房源管理'
  editingRoomId.value = null  // 重置编辑状态

  // 根据渠道类型设置不同的默认标签页和数据
  if (channel.name === 'Airbnb') {
    channelSettingsTab.value = 'accountList'
    // 加载 Airbnb 帐户数据
    airbnbAccountList.value = [
      { id: 1, account: 'chochoketsu@gmail.com', accountCode: '577845936' },
      { id: 2, account: '284033031@qq.com', accountCode: '718401313' },
    ]
    // 生成映射数据
    roomMappingData.value = generateAirbnbMappingData()
    updateFlattenedMappingData()
    // 生成房量设置数据
    generateRoomSettingsData()
    // 清空其他渠道数据
    hotelList.value = []
    calendarData.value = []
  } else {
    channelSettingsTab.value = 'hotelList'
    // 清空 Airbnb 数据
    airbnbAccountList.value = []

    // 加载静态模拟数据以便展示效果
    const mockHotels = generateMockHotelData(channel.name)
    hotelList.value = mockHotels

    // 自动选择第一个酒店并生成相关数据
    if (mockHotels.length > 0) {
      selectedHotelId.value = mockHotels[0].id

      // 生成映射数据
      roomMappingData.value = generateMockMappingData(mockHotels[0].hotelName)
      // 更新扁平化数据
      updateFlattenedMappingData()

      // 生成日历数据
      calendarDates.value = generateCalendarDates(calendarStartDate.value, 10)
      calendarData.value = generateMockCalendarData(mockHotels[0].hotelName, calendarDates.value)
    }
  }
}

const closeChannelSettings = () => {
  showChannelSettings.value = false
  currentSettingChannel.value = null
  hotelList.value = [] // 清空酒店列表
  airbnbAccountList.value = [] // 清空 Airbnb 帐户列表
  channelSettingsTab.value = 'hotelList' // 重置标签页
  roomMappingData.value = []
  flattenedMappingData.value = []
  calendarData.value = []
  roomSettingsData.value = []
  roomSettingsDates.value = []
}

// 酒店编辑
interface HotelItem {
  id: number
  hotelCode: string
  hotelName: string
  priceMode: string
  status: string
}

const editHotel = (row: HotelItem) => {
  ElMessage.info(`编辑酒店: ${row.hotelName}`)
  // TODO: 实现编辑酒店逻辑
}

// 断开酒店连接
const disconnectHotel = async (row: HotelItem) => {
  try {
    await ElMessageBox.confirm(
      `确定要断开与酒店 "${row.hotelName}" 的连接吗？断开后将无法同步该酒店的订单和房态。`,
      '断开连接',
      {
        confirmButtonText: '确定断开',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )

    // 从列表中移除
    const index = hotelList.value.findIndex(item => item.id === row.id)
    if (index > -1) {
      hotelList.value.splice(index, 1)
    }
    ElMessage.success(`已断开与酒店 "${row.hotelName}" 的连接`)
  } catch {
    // 用户取消操作
  }
}

// 断开 Airbnb 帐户连接
const disconnectAirbnbAccount = async (row: AirbnbAccount) => {
  try {
    await ElMessageBox.confirm(
      `确定要断开与帐户 "${row.account}" 的连接吗？`,
      '断开连接',
      {
        confirmButtonText: '确定断开',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )

    // 从列表中移除
    const index = airbnbAccountList.value.findIndex(item => item.id === row.id)
    if (index > -1) {
      airbnbAccountList.value.splice(index, 1)
    }
    ElMessage.success(`已断开与帐户 "${row.account}" 的连接`)
  } catch {
    // 用户取消操作
  }
}

// 断开房型映射
const disconnectRoomMapping = async (row: FlattenedMappingItem) => {
  try {
    await ElMessageBox.confirm(
      `确定要断开房型 "${row.channelRoomType}" 的映射吗？`,
      '断开连接',
      {
        confirmButtonText: '确定断开',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )
    // 从映射数据中移除
    const groupIndex = roomMappingData.value.findIndex(g => g.roomGroupId === row.roomGroupId)
    if (groupIndex > -1) {
      roomMappingData.value.splice(groupIndex, 1)
      updateFlattenedMappingData()
    }
    ElMessage.success('映射已断开')
  } catch {
    // 用户取消操作
  }
}

// 管理房型映射
const manageRoomMapping = (row: FlattenedMappingItem) => {
  currentManagingRoom.value = row
  showBookingSettingsDrawer.value = true
}

// 保存预定设置
const saveBookingSettings = () => {
  ElMessage.success('预定设置已保存')
  showBookingSettingsDrawer.value = false
}

// 关闭预定设置抽屉
const closeBookingSettingsDrawer = () => {
  showBookingSettingsDrawer.value = false
  currentManagingRoom.value = null
}

const closePriceRatio = () => {
  showPriceRatio.value = false
  activeMenu.value = 'channel-list'
}

const startConnection = () => {
  if (selectedChannel.value) {
    // 模拟连接过程
    showConnectDialog.value = false
    ElMessage.success(`正在连接${selectedChannel.value.name}，请稍后...`)

    // 模拟连接成功后更新状态
    setTimeout(() => {
      selectedChannel.value.connected = true
      selectedChannel.value.roomTypeText = '尚未关联房型'
      ElMessage.success(`${selectedChannel.value.name}连接成功！`)
      selectedChannel.value = null
    }, 2000)
  }
}

// 账号管理方法
const viewAccountRooms = (account: any) => {
  selectedAccount.value = account.id
  settingsTab.value = '房源管理'
  ElMessage.info(`查看账号 ${account.name} 的房源`)
}

const removeChannelAccount = async (account: any) => {
  try {
    await ElMessageBox.confirm(`确定要解除与账号 ${account.name} 的直连吗？`, '确认操作', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })

    // 移除账号
    const index = channelAccounts.value.findIndex((a) => a.id === account.id)
    if (index > -1) {
      channelAccounts.value.splice(index, 1)
    }

    ElMessage.success('账号已解除直连')
  } catch {
    // 用户取消操作
  }
}

const addAccount = () => {
  if (!newAccount.value.name || !newAccount.value.username || !newAccount.value.password) {
    ElMessage.warning('请填写完整的账号信息')
    return
  }

  // 添加新账号
  const newId = (channelAccounts.value.length + 1).toString()
  channelAccounts.value.push({
    id: newId,
    name: newAccount.value.name,
    roomCount: 0,
  })

  // 清空表单
  newAccount.value = {
    name: '',
    username: '',
    password: '',
  }

  showAddAccountDialog.value = false
  ElMessage.success('账号添加成功')
}

// 解析现有的 ratio 字符串到新数据结构
const parseRatioString = (ratio: string): { type: 'cheaper' | 'expensive', value: number, unit: '%' | '¥' } => {
  // 默认值
  const defaultResult = { type: 'cheaper' as const, value: 0, unit: '%' as const }

  if (ratio === '等同于基本价格') {
    return defaultResult
  }

  // 匹配格式如 "45 % 比基准价贵" 或 "10 ¥ 比基准价便宜"
  const match = ratio.match(/(\d+)\s*([%¥])\s*比基准价(贵|便宜)/)
  if (match) {
    return {
      type: match[3] === '贵' ? 'expensive' : 'cheaper',
      value: parseInt(match[1], 10),
      unit: match[2] as '%' | '¥'
    }
  }

  return defaultResult
}

// 将新数据结构转换为显示字符串
const formatRatioString = (type: 'cheaper' | 'expensive', value: number, unit: '%' | '¥'): string => {
  if (value === 0) {
    return '等同于基本价格'
  }
  const typeText = type === 'expensive' ? '贵' : '便宜'
  return `${value} ${unit} 比基准价${typeText}`
}

// 编辑价格比例
const editPriceRatio = (row: PriceRatioItem) => {
  const parsed = parseRatioString(row.ratio)
  currentEditingRatio.value = {
    channelId: row.channelId,
    channel: row.channel,
    ratio: row.ratio,
    adjustmentType: parsed.type,
    adjustmentValue: parsed.value,
    adjustmentUnit: parsed.unit,
    autoSyncPrice: row.autoSyncPrice,
    backendAdjustmentType: row.adjustmentType,
  }
  showEditRatioDialog.value = true
}

// 保存价格比例
const savePriceRatio = async () => {
  if (!currentEditingRatio.value) return

  const { channelId, adjustmentType, adjustmentValue, adjustmentUnit, autoSyncPrice } = currentEditingRatio.value

  // 将前端的调整类型转换为后端的类型
  let backendAdjustmentType: PriceAdjustmentType
  let backendAdjustmentValue: number | null = adjustmentValue

  if (adjustmentValue === 0) {
    // 等同于基本价格
    backendAdjustmentType = 'PERCENTAGE'
    backendAdjustmentValue = 0
  } else if (adjustmentUnit === '%') {
    backendAdjustmentType = 'PERCENTAGE'
    // 如果是"更便宜"，则使用负值
    backendAdjustmentValue = adjustmentType === 'cheaper' ? -adjustmentValue : adjustmentValue
  } else {
    backendAdjustmentType = 'FIXED'
    backendAdjustmentValue = adjustmentType === 'cheaper' ? -adjustmentValue : adjustmentValue
  }

  try {
    const response = await updateChannelPriceAdjustment(channelId, {
      adjustmentType: backendAdjustmentType,
      adjustmentValue: backendAdjustmentValue,
      autoSyncPrice: autoSyncPrice,
    })

    if (response.success) {
      // 更新本地数据
      const newRatio = formatRatioString(adjustmentType, adjustmentValue, adjustmentUnit)
      const index = priceRatioData.value.findIndex(item => item.channelId === channelId)
      if (index > -1) {
        priceRatioData.value[index] = {
          channelId: channelId,
          channel: currentEditingRatio.value.channel,
          ratio: newRatio,
          adjustmentType: backendAdjustmentType,
          adjustmentValue: backendAdjustmentValue,
          autoSyncPrice: autoSyncPrice,
        }
      }
      showEditRatioDialog.value = false
      currentEditingRatio.value = null
      ElMessage.success('价格比例已更新')
    } else {
      ElMessage.error(response.message || '更新失败')
    }
  } catch (error) {
    console.error('更新价格比例失败:', error)
    ElMessage.error('更新价格比例失败，请重试')
  }
}

// 显示连接历史
const showConnectionHistory = () => {
  showHistoryDialog.value = true
}

// 刷新渠道信息
const refreshChannelInfo = () => {
  ElMessage.success('正在刷新渠道信息...')
  // 模拟刷新
  setTimeout(() => {
    ElMessage.success('渠道信息已刷新')
  }, 1000)
}

// 映射表格合并方法
const mappingSpanMethod = ({ row, columnIndex }: { row: FlattenedMappingItem; column: any; rowIndex: number; columnIndex: number }) => {
  // 第一列（Booking.com Room Type）和第二列（PMS房型）需要合并
  if (columnIndex === 0 || columnIndex === 1) {
    if (row.isFirstInGroup) {
      return { rowspan: row.groupRowCount, colspan: 1 }
    } else {
      return { rowspan: 0, colspan: 0 }
    }
  }
  // 操作列也需要合并
  if (columnIndex === 5) {
    if (row.isFirstInGroup) {
      return { rowspan: row.groupRowCount, colspan: 1 }
    } else {
      return { rowspan: 0, colspan: 0 }
    }
  }
  return { rowspan: 1, colspan: 1 }
}

// 获取映射状态样式类
const getMappingStatusClass = (status: 'connected' | 'disconnected' | 'invalid'): string => {
  switch (status) {
    case 'connected':
      return 'status-connected'
    case 'disconnected':
      return 'status-disconnected'
    case 'invalid':
      return 'status-invalid'
    default:
      return ''
  }
}

// 获取映射状态文本
const getMappingStatusText = (status: 'connected' | 'disconnected' | 'invalid'): string => {
  switch (status) {
    case 'connected':
      return '已连接'
    case 'disconnected':
      return '未直连'
    case 'invalid':
      return '无效'
    default:
      return '-'
  }
}

// 编辑房型映射
const editRoomMapping = (row: FlattenedMappingItem) => {
  editingRoomId.value = row.roomGroupId
}

// 取消编辑映射
const cancelEditMapping = () => {
  editingRoomId.value = null
  // 重新生成扁平化数据以恢复原始值
  updateFlattenedMappingData()
}

// 保存房型映射
const saveRoomMapping = (roomGroupId: string) => {
  // 找到对应的房型组
  const group = roomMappingData.value.find(g => g.roomGroupId === roomGroupId)
  if (group) {
    // 更新扁平化数据中的选择值到源数据
    const flatItems = flattenedMappingData.value.filter(item => item.roomGroupId === roomGroupId)
    if (flatItems.length > 0) {
      group.pmsRoomType = flatItems[0].selectedPmsRoom
      group.selectedPmsRoom = flatItems[0].selectedPmsRoom
      flatItems.forEach(flatItem => {
        const pricePlan = group.pricePlans.find(p => p.id === flatItem.id)
        if (pricePlan) {
          pricePlan.pmsPricePlan = flatItem.selectedPmsPricePlan
          pricePlan.selectedPmsPricePlan = flatItem.selectedPmsPricePlan
          // 如果选择了PMS价格计划，状态变为已连接
          if (flatItem.selectedPmsPricePlan) {
            pricePlan.status = 'connected'
          }
        }
      })
    }
  }
  editingRoomId.value = null
  updateFlattenedMappingData()
  ElMessage.success('保存成功')
}

// 更新扁平化映射数据
const updateFlattenedMappingData = () => {
  const flattened: FlattenedMappingItem[] = []
  roomMappingData.value.forEach(group => {
    const groupRowCount = group.pricePlans.length
    group.pricePlans.forEach((plan, index) => {
      flattened.push({
        id: plan.id,
        roomGroupId: group.roomGroupId,
        channelRoomType: group.channelRoomType,
        channelRoomId: group.channelRoomId,
        pmsRoomType: group.pmsRoomType,
        selectedPmsRoom: group.selectedPmsRoom,
        channelPricePlan: plan.channelPricePlan,
        channelPricePlanId: plan.channelPricePlanId,
        pmsPricePlan: plan.pmsPricePlan,
        selectedPmsPricePlan: plan.selectedPmsPricePlan,
        status: plan.status,
        isFirstInGroup: index === 0,
        groupRowCount: groupRowCount,
      })
    })
  })
  flattenedMappingData.value = flattened
}

// 从日历同步
const syncFromCalendar = () => {
  ElMessage.success('正在从日历同步...')
  setTimeout(() => {
    ElMessage.success('同步完成')
  }, 1000)
}

// 全量刷新
const fullRefresh = () => {
  ElMessage.success('正在全量刷新...')
  setTimeout(() => {
    ElMessage.success('刷新完成')
  }, 1500)
}

// 日历表格合并方法
const calendarSpanMethod = ({ rowIndex, columnIndex }: { row: CalendarRow; column: any; rowIndex: number; columnIndex: number }) => {
  // 标题行合并所有列
  if (calendarData.value[rowIndex]?.type === 'title' && columnIndex === 0) {
    return { rowspan: 1, colspan: calendarDates.value.length + 1 }
  }
  return { rowspan: 1, colspan: 1 }
}

// 生成日历日期
const generateCalendarDates = (startDate: string, days: number = 10): CalendarDate[] => {
  const weekdays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
  const dates: CalendarDate[] = []
  const start = new Date(startDate)

  for (let i = 0; i < days; i++) {
    const date = new Date(start)
    date.setDate(start.getDate() + i)
    const month = date.getMonth() + 1
    const day = date.getDate()
    dates.push({
      value: date.toISOString().split('T')[0],
      label: `${month}月${day}日`,
      day: `${month}月${day.toString().padStart(2, '0')}日`,
      weekday: weekdays[date.getDay()],
    })
  }
  return dates
}

// 生成映射模拟数据
const generateMockMappingData = (hotelName: string): RoomMappingGroup[] => {
  return [
    {
      roomGroupId: 'room_1454031803',
      channelRoomType: 'Economy Double Room',
      channelRoomId: '1454031803',
      pmsRoomType: `${hotelName}103`,
      selectedPmsRoom: `${hotelName}103`,
      pricePlans: [
        {
          id: 'plan_57653133',
          channelPricePlan: 'Fully flexible(non-manageable)',
          channelPricePlanId: '57653133',
          pmsPricePlan: null,
          selectedPmsPricePlan: null,
          status: 'invalid',
        },
        {
          id: 'plan_57652901',
          channelPricePlan: 'Non-refundable(non-manageable)',
          channelPricePlanId: '57652901',
          pmsPricePlan: null,
          selectedPmsPricePlan: null,
          status: 'invalid',
        },
        {
          id: 'plan_56459960',
          channelPricePlan: 'Standard Rate',
          channelPricePlanId: '56459960',
          pmsPricePlan: 'Standard Rate',
          selectedPmsPricePlan: 'Standard Rate',
          status: 'connected',
        },
        {
          id: 'plan_57593856',
          channelPricePlan: 'Weekly rate(non-manageable)',
          channelPricePlanId: '57593856',
          pmsPricePlan: null,
          selectedPmsPricePlan: null,
          status: 'disconnected',
        },
        {
          id: 'plan_57715884',
          channelPricePlan: 'Fully flexible(non-manageable)',
          channelPricePlanId: '57715884',
          pmsPricePlan: null,
          selectedPmsPricePlan: null,
          status: 'disconnected',
        },
        {
          id: 'plan_57715921',
          channelPricePlan: 'Weekly rate(non-manageable)',
          channelPricePlanId: '57715921',
          pmsPricePlan: null,
          selectedPmsPricePlan: null,
          status: 'disconnected',
        },
        {
          id: 'plan_57716084',
          channelPricePlan: 'Early booker rate plan (90+ days)(non-manageable)',
          channelPricePlanId: '57716084',
          pmsPricePlan: null,
          selectedPmsPricePlan: null,
          status: 'disconnected',
        },
        {
          id: 'plan_57715907',
          channelPricePlan: 'Non-refundable(non-manageable)',
          channelPricePlanId: '57715907',
          pmsPricePlan: null,
          selectedPmsPricePlan: null,
          status: 'disconnected',
        },
      ],
    },
    {
      roomGroupId: 'room_1454031804',
      channelRoomType: 'Standard Double Room',
      channelRoomId: '1454031804',
      pmsRoomType: `${hotelName}104`,
      selectedPmsRoom: `${hotelName}104`,
      pricePlans: [
        {
          id: 'plan_56459961',
          channelPricePlan: 'Standard Rate',
          channelPricePlanId: '56459961',
          pmsPricePlan: 'Standard Rate',
          selectedPmsPricePlan: 'Standard Rate',
          status: 'connected',
        },
      ],
    },
  ]
}

// 生成日历模拟数据
const generateMockCalendarData = (hotelName: string, dates: CalendarDate[]): CalendarRow[] => {
  const rooms = [`${hotelName}103`, `${hotelName}104`]
  const rows: CalendarRow[] = []

  rooms.forEach((room, roomIndex) => {
    const roomId = `room_${roomIndex}`
    // 房型标题行
    rows.push({
      id: `${roomId}_title`,
      label: `${room}(Economy Double Room-145403180${3 + roomIndex})`,
      type: 'title',
      roomId,
      values: {},
    })
    // 房量行
    const inventoryValues: Record<string, number> = {}
    dates.forEach(d => { inventoryValues[d.value] = 0 })
    rows.push({
      id: `${roomId}_inventory`,
      label: '房量',
      type: 'inventory',
      roomId,
      values: inventoryValues,
    })
    // Standard Rate 标题（使用特殊标记）
    rows.push({
      id: `${roomId}_rate_title`,
      label: '▼ Standard Rate',
      type: 'title',
      roomId,
      values: {},
    })
    // 价格行
    const priceValues: Record<string, number> = {}
    dates.forEach(d => {
      const basePrice = 11420.2
      const dayOfWeek = new Date(d.value).getDay()
      // 周末价格稍高
      priceValues[d.value] = dayOfWeek === 0 || dayOfWeek === 6 ? basePrice * 1.1 : basePrice
    })
    rows.push({
      id: `${roomId}_price`,
      label: '价格',
      type: 'price',
      roomId,
      values: priceValues,
    })
    // 2人价格
    const price2Values: Record<string, string> = {}
    dates.forEach(d => { price2Values[d.value] = '-' })
    rows.push({
      id: `${roomId}_price2`,
      label: '2人价格',
      type: 'number',
      roomId,
      values: price2Values,
    })
    // 最小入住天数
    const minStayValues: Record<string, number> = {}
    dates.forEach(d => { minStayValues[d.value] = 1 })
    rows.push({
      id: `${roomId}_minstay`,
      label: '最小入住天数',
      type: 'number',
      roomId,
      values: minStayValues,
    })
    // 最大入住天数
    const maxStayValues: Record<string, number> = {}
    dates.forEach(d => { maxStayValues[d.value] = 365 })
    rows.push({
      id: `${roomId}_maxstay`,
      label: '最大入住天数',
      type: 'number',
      roomId,
      values: maxStayValues,
    })
    // 关房
    const closeValues: Record<string, boolean> = {}
    dates.forEach(d => { closeValues[d.value] = false })
    rows.push({
      id: `${roomId}_close`,
      label: '关房',
      type: 'checkbox',
      roomId,
      values: closeValues,
    })
    // CTA
    const ctaValues: Record<string, boolean> = {}
    dates.forEach(d => { ctaValues[d.value] = false })
    rows.push({
      id: `${roomId}_cta`,
      label: 'CTA',
      type: 'checkbox',
      roomId,
      values: ctaValues,
    })
    // CTD
    const ctdValues: Record<string, boolean> = {}
    dates.forEach(d => { ctdValues[d.value] = false })
    rows.push({
      id: `${roomId}_ctd`,
      label: 'CTD',
      type: 'checkbox',
      roomId,
      values: ctdValues,
    })
  })

  return rows
}

// 模拟酒店数据生成器（根据渠道不同生成不同的数据）
const generateMockHotelData = (channelName: string): HotelItem[] => {
  const baseId = Date.now()

  // 根据渠道生成不同的模拟数据
  const mockDataByChannel: Record<string, HotelItem[]> = {
    'Booking.com': [
      { id: baseId, hotelCode: '14540318', hotelName: 'Tanpopo Inn', priceMode: 'Occupancy Based Price', status: 'Active' },
      { id: baseId + 1, hotelCode: '14844797', hotelName: '楽途ホテル 東十条', priceMode: 'Occupancy Based Price', status: 'Active' },
      { id: baseId + 2, hotelCode: '14850604', hotelName: '楽途ホテル 池袋', priceMode: 'Occupancy Based Price', status: 'Active' },
    ],
    'Agoda': [
      { id: baseId, hotelCode: 'AGD001', hotelName: 'Tokyo Central Hotel', priceMode: 'Per Night', status: 'Active' },
      { id: baseId + 1, hotelCode: 'AGD002', hotelName: 'Osaka Bay Resort', priceMode: 'Per Night', status: 'Active' },
    ],
    'Airbnb': [
      { id: baseId, hotelCode: 'ABB001', hotelName: 'Cozy Tokyo Apartment', priceMode: 'Per Night', status: 'Active' },
      { id: baseId + 1, hotelCode: 'ABB002', hotelName: 'Shibuya Modern Studio', priceMode: 'Per Night', status: 'Active' },
    ],
    'Trip.com': [
      { id: baseId, hotelCode: 'TRIP001', hotelName: '東京スカイホテル', priceMode: 'Room Rate', status: 'Active' },
    ],
    'Expedia': [
      { id: baseId, hotelCode: 'EXP001', hotelName: 'Grand Tokyo Hotel', priceMode: 'Per Room Per Night', status: 'Active' },
    ],
  }

  // 返回对应渠道的数据，如果没有则返回通用数据
  return mockDataByChannel[channelName] || [
    { id: baseId, hotelCode: `${channelName.substring(0, 3).toUpperCase()}001`, hotelName: `${channelName} Hotel 1`, priceMode: 'Standard', status: 'Active' },
  ]
}

// 生成 Airbnb 映射数据
const generateAirbnbMappingData = (): RoomMappingGroup[] => {
  return [
    {
      roomGroupId: 'airbnb_room_1',
      channelRoomType: '串十条丁目2F(LS012)  higashijujo 2F ・《Rakuto Higashijujo 2F》Good Access to Ikebukuro',
      channelRoomId: '115771838797582817A',
      pmsRoomType: '楽途ホテル  串十条2F',
      selectedPmsRoom: '楽途ホテル  串十条2F',
      pricePlans: [
        {
          id: 'airbnb_plan_1',
          channelPricePlan: 'Standard Rate',
          channelPricePlanId: 'STANDARD',
          pmsPricePlan: 'Standard Rate',
          selectedPmsPricePlan: 'Standard Rate',
          status: 'connected',
        },
      ],
    },
    {
      roomGroupId: 'airbnb_room_2',
      channelRoomType: '串十条丁目3・4F(LS013)  higashijujo1 3-4F ・《Rakuto Higashijujo 2F》Good Access to Ikebukuro',
      channelRoomId: '116520872962299543I',
      pmsRoomType: '楽途ホテル  串十条3/4F',
      selectedPmsRoom: '楽途ホテル  串十条3/4F',
      pricePlans: [
        {
          id: 'airbnb_plan_2',
          channelPricePlan: 'Standard Rate',
          channelPricePlanId: 'STANDARD',
          pmsPricePlan: 'Standard Rate',
          selectedPmsPricePlan: 'Standard Rate',
          status: 'connected',
        },
      ],
    },
    {
      roomGroupId: 'airbnb_room_3',
      channelRoomType: '楽途ホテル  池袋・《Rakuto Hotel Ikebukuro》Direct to Shinjuku',
      channelRoomId: '131261457580594907',
      pmsRoomType: '楽途ホテル  池袋201',
      selectedPmsRoom: '楽途ホテル  池袋201',
      pricePlans: [
        {
          id: 'airbnb_plan_3',
          channelPricePlan: 'Standard Rate',
          channelPricePlanId: 'STANDARD',
          pmsPricePlan: 'Standard Rate',
          selectedPmsPricePlan: 'Standard Rate',
          status: 'connected',
        },
      ],
    },
    {
      roomGroupId: 'airbnb_room_4',
      channelRoomType: '串十条丁目1F  higashi jujo 1 1F ・《Rakuto Higashijujo 1F》Good Access to Ikebukuro',
      channelRoomId: '141957367786915011A',
      pmsRoomType: '楽途ホテル  串十条1F',
      selectedPmsRoom: '楽途ホテル  串十条1F',
      pricePlans: [
        {
          id: 'airbnb_plan_4',
          channelPricePlan: 'Standard Rate',
          channelPricePlanId: 'STANDARD',
          pmsPricePlan: 'Standard Rate',
          selectedPmsPricePlan: 'Standard Rate',
          status: 'connected',
        },
      ],
    },
  ]
}

// 生成 Airbnb 房量设置数据
const generateRoomSettingsData = () => {
  // 生成日期列表（从选择的日期开始，12天）
  roomSettingsDates.value = generateCalendarDates(roomSettingsStartDate.value, 12)

  // 生成房型行数据
  const airbnbRooms = [
    { airbnbRoomType: '串十条丁目2F(LS012)  higashijujo 2F ・《Rakuto Higashijujo 2F》Good Access to Ikebukuro (115771838797582817A)', pmsRoomType: '楽途ホテル  串十条2F' },
    { airbnbRoomType: '串十条丁目3・4F(LS013)  higashijujo1 3-4F ・《Rakuto Higashijujo 2F》Good Access to Ikebukuro (116520872962299543I)', pmsRoomType: '楽途ホテル  串十条3/4F' },
    { airbnbRoomType: '楽途ホテル  池袋・《Rakuto Hotel Ikebukuro》Direct to Shinjuku (131261457580594907)', pmsRoomType: '楽途ホテル  池袋201' },
    { airbnbRoomType: '串十条丁目1F  higashi jujo 1 1F ・《Rakuto Higashijujo 1F》Good Access to Ikebukuro (141957367786915011A)', pmsRoomType: '楽途ホテル  串十条1F' },
  ]

  roomSettingsData.value = airbnbRooms.map((room, index) => {
    const values: Record<string, any> = {}
    roomSettingsDates.value.forEach(date => {
      values[date.value] = '' // 空白单元格
    })
    return {
      id: `room_${index}`,
      airbnbRoomType: room.airbnbRoomType,
      pmsRoomType: room.pmsRoomType,
      values,
    }
  })
}

// 开始授权
const startAuthorization = () => {
  showAddHotelDialog.value = false
  ElMessage.success('正在进行授权...')

  // 模拟授权过程
  setTimeout(() => {
    if (currentSettingChannel.value) {
      // 授权成功，添加模拟酒店数据
      const mockHotels = generateMockHotelData(currentSettingChannel.value.name)
      hotelList.value = [...hotelList.value, ...mockHotels]

      // 自动选择第一个酒店
      if (mockHotels.length > 0) {
        selectedHotelId.value = mockHotels[0].id

        // 生成映射数据
        roomMappingData.value = generateMockMappingData(mockHotels[0].hotelName)

        // 生成日历数据
        calendarDates.value = generateCalendarDates(calendarStartDate.value, 10)
        calendarData.value = generateMockCalendarData(mockHotels[0].hotelName, calendarDates.value)
      }

      ElMessage.success(`${currentSettingChannel.value.name} 授权成功！已添加 ${mockHotels.length} 个酒店`)
    }
  }, 1500)
}

onMounted(() => {
  // 组件挂载后的初始化逻辑
  loadChannels()
})
</script>

<style scoped>
.channel-management {
  display: flex;
  height: 100vh;
  background: #f5f5f5;
}

/* 左侧导航 */
.sidebar {
  width: 200px;
  background: white;
  border-right: 1px solid #e8e8e8;
  display: flex;
  flex-direction: column;
  transition: width 0.3s ease;
}

.sidebar.collapsed {
  width: 64px;
}

.sidebar-header {
  height: 56px;
  display: flex;
  align-items: center;
  padding: 0 16px;
  border-bottom: 1px solid #e8e8e8;
  cursor: pointer;
  transition: background-color 0.3s;
  flex-shrink: 0;
}

.sidebar-header:hover {
  background-color: #f5f5f5;
}

.sidebar-icon {
  font-size: 20px;
  color: #1890ff;
  flex-shrink: 0;
}

.sidebar-title {
  flex: 1;
  margin-left: 12px;
  font-size: 14px;
  font-weight: 500;
  color: #333;
}

.collapse-icon {
  font-size: 16px;
  color: #999;
}

.sidebar-menu {
  flex: 1;
  border-right: none;
  overflow-y: auto;
}

/* 主内容区域 */
.main-content {
  flex: 1;
  overflow: auto;
  background: #f5f5f5;
}

.page-header {
  background: white;
  padding: 20px;
  border-bottom: 1px solid #e8e8e8;
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0 0 16px 0;
  font-size: 20px;
  font-weight: 500;
}

.header-tabs :deep(.el-tabs__header) {
  margin: 0;
}

/* 渠道内容区域 */
.channel-content {
  padding: 20px;
}

.channel-section {
  margin-bottom: 40px;
}

.section-title {
  font-size: 16px;
  font-weight: 500;
  color: #606266;
  margin: 0 0 20px 0;
  padding-bottom: 12px;
  border-bottom: 1px solid #e8e8e8;
}

/* 渠道网格 */
.channel-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 20px;
}

.channel-card {
  background: white;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  padding: 24px 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  transition: all 0.3s ease;
}

.channel-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  border-color: #409eff;
}

.channel-logo-wrapper {
  width: 120px;
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff;
  border-radius: 4px;
  padding: 8px;
}

.channel-logo-img {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
}

.channel-name {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
  margin: 4px 0;
  text-align: center;
}

.channel-status {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #909399;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.status-dot.connected {
  background: #67c23a;
}

.status-dot.disconnected {
  background: #d9d9d9;
}

.status-text {
  font-size: 13px;
}

.config-btn {
  width: 100%;
  margin-top: 4px;
}

/* 介绍内容 */
.intro-content {
  padding: 20px;
  background: white;
  margin: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.intro-section {
  margin-bottom: 32px;
}

.intro-section h3 {
  font-size: 18px;
  font-weight: 500;
  margin: 0 0 16px 0;
  color: #333;
}

.intro-section p {
  font-size: 14px;
  color: #666;
  margin: 0 0 12px 0;
}

.intro-section ul {
  margin: 0;
  padding-left: 20px;
}

.intro-section li {
  font-size: 14px;
  color: #666;
  margin-bottom: 8px;
}

.channel-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.category h4 {
  font-size: 16px;
  font-weight: 500;
  margin: 0 0 12px 0;
  color: #333;
}

.channel-items {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.channel-item {
  background: #f0f7ff;
  color: #1890ff;
  padding: 4px 12px;
  border-radius: 16px;
  font-size: 12px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .channel-management {
    flex-direction: column;
  }

  .sidebar {
    width: 100%;
    height: auto;
  }

  .channel-grid {
    grid-template-columns: 1fr;
    padding: 16px;
  }
}

/* 开通直连对话框样式 */
.connect-dialog-content {
  padding: 0;
}

.connect-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  padding: 0 20px 20px;
  border-bottom: 1px solid #f0f0f0;
}

.connect-description {
  flex: 1;
  font-size: 14px;
  color: #666;
  line-height: 1.6;
  margin: 0;
}

.channel-logo-large {
  width: 80px;
  height: 80px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.logo-text-large {
  font-size: 18px;
  font-weight: bold;
  color: white;
}

/* 须知内容区域样式 */
.notice-section {
  padding: 0 20px;
}

.notice-title {
  font-size: 16px;
  font-weight: 500;
  color: #333;
  margin: 20px 0 16px;
  text-align: center;
}

.notice-content {
  max-height: 400px;
  overflow-y: auto;
  padding-right: 8px;
}

/* 自定义滚动条样式 */
.notice-content::-webkit-scrollbar {
  width: 6px;
}

.notice-content::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 3px;
}

.notice-content::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
}

.notice-content::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}

.notice-text {
  font-size: 13px;
  line-height: 1.8;
  color: #333;
}

.notice-text p {
  margin: 0 0 16px 0;
  text-indent: 0;
  text-align: justify;
}

.notice-text h4 {
  font-size: 14px;
  font-weight: 500;
  color: #333;
  margin: 24px 0 12px 0;
  line-height: 1.6;
}

.notice-text h4:first-of-type {
  margin-top: 0;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

/* 动态标题 */
:deep(.el-dialog__header) {
  padding: 20px 20px 0;
}

:deep(.el-dialog__title) {
  font-size: 18px;
  font-weight: 500;
}

/* 渠道设置页面样式 */
.channel-settings-view {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.settings-header {
  background: white;
  padding: 16px 20px;
  border-bottom: 1px solid #e8e8e8;
}

.breadcrumb {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #666;
}

.back-btn {
  padding: 0;
  display: flex;
  align-items: center;
  gap: 4px;
  color: #666;
}

.back-btn:hover {
  color: #1890ff;
}

.breadcrumb-separator {
  color: #d9d9d9;
}

.breadcrumb-item {
  color: #666;
}

.breadcrumb-item.active {
  color: #333;
  font-weight: 500;
}

.settings-content {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
}

.channel-info-card {
  background: white;
  border-radius: 8px;
  padding: 24px;
  margin-bottom: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  display: flex;
  gap: 24px;
  align-items: flex-start;
}

.channel-logo-section {
  flex-shrink: 0;
  width: 100px;
  height: 50px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.channel-logo-large-img {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
}

.channel-desc {
  flex: 1;
}

.channel-desc h3 {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 12px 0;
}

.channel-desc h3.channel-title-highlight {
  color: #ff385c;
}

/* Airbnb 帐户表格样式 */
.account-table-section {
  margin-top: 0;
}

.account-table {
  width: 100%;
}

/* 房量设置样式 */
.room-settings-content {
  min-height: 200px;
  display: flex;
  flex-direction: column;
}

.room-settings-header {
  padding: 16px 20px;
  background: #fafafa;
  border: 1px solid #e8e8e8;
  border-radius: 4px;
  margin-bottom: 16px;
}

.date-selector {
  display: flex;
  align-items: center;
  gap: 8px;
}

.selector-label {
  font-size: 14px;
  color: #606266;
  white-space: nowrap;
}

.room-settings-table-section {
  flex: 1;
}

.room-settings-table {
  width: 100%;
}

.room-type-info {
  text-align: left;
}

.room-type-info .room-name {
  font-size: 13px;
  color: #303133;
  line-height: 1.5;
}

.settings-description {
  color: #606266;
  margin-bottom: 20px;
}

.channel-desc p {
  font-size: 13px;
  line-height: 1.8;
  color: #606266;
  margin: 0;
}

.channel-actions {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  flex-shrink: 0;
}

/* 渠道设置标签页 */
.channel-tabs-section {
  background: white;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.channel-settings-tabs :deep(.el-tabs__header) {
  margin-bottom: 20px;
}

.channel-settings-tabs :deep(.el-tabs__item) {
  font-size: 14px;
  padding: 0 20px;
}

.channel-settings-tabs :deep(.el-tabs__item.is-active) {
  font-weight: 600;
}

.mapping-content,
.calendar-content {
  min-height: 200px;
  display: flex;
  flex-direction: column;
}

/* 映射页面筛选栏 */
.mapping-filter-bar,
.calendar-filter-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  background: #fafafa;
  border: 1px solid #e8e8e8;
  border-radius: 4px;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 12px;
}

.mapping-filter-bar .filter-left,
.mapping-filter-bar .filter-right,
.calendar-filter-bar .filter-left,
.calendar-filter-bar .filter-right {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.mapping-filter-bar .filter-item,
.calendar-filter-bar .filter-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.mapping-filter-bar .filter-label,
.calendar-filter-bar .filter-label {
  font-size: 14px;
  color: #606266;
  white-space: nowrap;
}

/* 映射表格区域 */
.mapping-table-section,
.calendar-table-section {
  flex: 1;
}

.mapping-table {
  width: 100%;
}

.room-type-cell {
  text-align: left;
}

.room-type-cell .room-name {
  font-weight: 500;
  color: #303133;
}

.room-type-cell .room-id {
  font-size: 12px;
  color: #909399;
}

.status-tag {
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
}

.status-tag.connected {
  color: #67c23a;
  background: #f0f9eb;
}

.status-tag.disconnected {
  color: #909399;
  background: #f4f4f5;
}

/* 映射状态样式 */
.mapping-status {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
}

.mapping-status.status-connected {
  color: #1890ff;
}

.mapping-status.status-disconnected {
  color: #f5222d;
}

.mapping-status.status-invalid {
  color: #909399;
}

.mapping-status .status-icon {
  font-size: 14px;
}

/* 日历表格样式 */
.calendar-table {
  width: 100%;
}

.calendar-table :deep(.el-table__header th) {
  padding: 8px 0;
}

.date-header {
  text-align: center;
}

.date-header .date-day {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
}

.date-header .date-weekday {
  font-size: 12px;
  color: #909399;
}

.row-label {
  font-size: 13px;
  color: #606266;
}

.price-value {
  color: #409eff;
  font-weight: 500;
}

.hotel-table-section {
  margin-top: 0;
}

.hotel-table {
  width: 100%;
}

.highlight {
  color: #1890ff;
  font-weight: 500;
}

.link {
  color: #1890ff;
  text-decoration: none;
}

.link:hover {
  text-decoration: underline;
}

.settings-tabs {
  background: white;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.tab-content {
  padding-top: 20px;
}

.filter-bar {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.filter-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.filter-item label {
  font-size: 14px;
  color: #666;
  white-space: nowrap;
}

.filter-actions {
  margin-left: auto;
}

.room-source-table {
  margin-bottom: 20px;
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
}

.empty-icon {
  display: flex;
  justify-content: center;
  margin-bottom: 20px;
}

.empty-text {
  font-size: 14px;
  color: #999;
  line-height: 1.8;
  margin: 0;
}

/* 账号管理样式 */
.account-notice {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: #e6f7ff;
  border: 1px solid #91d5ff;
  border-radius: 6px;
  margin-bottom: 20px;
}

.account-notice span {
  font-size: 14px;
  color: #1890ff;
}

.account-actions {
  margin-bottom: 20px;
}

.account-table {
  margin-top: 20px;
}

.danger-link {
  color: #ff4d4f !important;
}

.add-account-form {
  padding: 20px 0;
}

.add-account-form p {
  margin: 0 0 20px 0;
  font-size: 14px;
  color: #666;
}

/* 价格比例页面样式 */
.price-ratio-view {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.price-ratio-table-container {
  background: white;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.price-ratio-table {
  width: 100%;
}

.edit-ratio-form {
  padding: 20px;
}

.edit-ratio-form .form-item {
  margin-bottom: 20px;
}

.edit-ratio-form .form-item:last-child {
  margin-bottom: 0;
}

.edit-ratio-form .form-label {
  font-size: 14px;
  color: #606266;
  margin-bottom: 8px;
}

.edit-ratio-form .form-value {
  font-size: 14px;
  color: #303133;
}

.edit-ratio-form .adjustment-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.edit-ratio-form .adjustment-type-select {
  width: 120px;
}

.edit-ratio-form .adjustment-value-input {
  width: 100px;
}

.edit-ratio-form .adjustment-value-input :deep(.el-input__inner) {
  text-align: center;
}

.edit-ratio-form .adjustment-unit-select {
  width: 80px;
}

/* 添加酒店对话框样式 */
.add-hotel-dialog {
  padding: 0;
}

.dialog-header-section {
  display: flex;
  gap: 20px;
  padding-bottom: 20px;
  border-bottom: 1px solid #e8e8e8;
  margin-bottom: 20px;
}

.dialog-description {
  flex: 1;
}

.dialog-description p {
  font-size: 13px;
  line-height: 1.8;
  color: #606266;
  margin: 0 0 12px 0;
}

.notice-text {
  color: #f56c6c;
  font-weight: 500;
}

.dialog-logo {
  flex-shrink: 0;
  width: 100px;
  height: 50px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.dialog-logo-img {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
}

.agreement-section {
  max-height: 400px;
  overflow-y: auto;
}

.agreement-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 16px 0;
  text-align: center;
}

.agreement-content {
  font-size: 13px;
  line-height: 1.8;
  color: #606266;
}

.agreement-content p {
  margin: 0 0 16px 0;
  text-align: justify;
}

.agreement-content h4 {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin: 20px 0 12px 0;
}

/* 连接历史对话框样式 */
.history-dialog {
  padding: 0;
}

.history-table {
  width: 100%;
  margin-bottom: 20px;
}

/* 预定设置抽屉样式 */
.booking-settings-form {
  padding: 0 20px;
}

.form-item {
  margin-bottom: 28px;
}

.form-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
}

.help-icon {
  font-size: 16px;
  color: #909399;
  cursor: help;
}

.form-desc {
  font-size: 13px;
  color: #606266;
  margin-bottom: 12px;
  line-height: 1.5;
}

.form-control {
  display: flex;
  align-items: center;
  gap: 8px;
}

.form-control .el-select {
  flex: 1;
}

.control-unit {
  font-size: 14px;
  color: #606266;
  white-space: nowrap;
}

.form-subitem {
  margin-top: 16px;
}

.form-subitem:first-child {
  margin-top: 0;
}

.form-subitem .form-desc {
  margin-bottom: 8px;
  font-size: 13px;
}

.form-subitem .el-select {
  width: 100%;
}

.drawer-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 12px 20px;
  border-top: 1px solid #e4e7ed;
}
</style>
