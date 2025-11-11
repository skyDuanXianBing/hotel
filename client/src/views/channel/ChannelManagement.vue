<template>
  <div class="channel-management">
    <!-- 左侧导航菜单 -->
    <div class="sidebar">
      <div class="sidebar-header">
        <el-button type="text" @click="$router.go(-1)">
          <el-icon><ArrowLeft /></el-icon>
        </el-button>
        <span class="sidebar-title">收起导航</span>
      </div>

      <el-menu class="sidebar-menu" :default-active="activeMenu" @select="handleMenuSelect">
        <el-menu-item index="channel-list">
          <span>渠道列表</span>
        </el-menu-item>
        <el-menu-item index="price-ratio">
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
          <span class="breadcrumb-separator">/</span>
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
            <h3>{{ currentSettingChannel?.name || '' }}渠道设置</h3>
            <p>
              渠道连接是指管理理层面（"您"）在订单来了系统中设置直连后，由订单来了与{{ currentSettingChannel?.name || '' }}系统自动建立直接连接，一旦连接匹配成功，Smart Order将能够获取渠道订单信息，并支持酒店在PMS更新房量、房价和限制条件。Smart Order将会即吧这些信息自动同步给渠道。如需详细操作说明，请访问帮助中心。
            </p>
          </div>
          <div class="channel-actions">
            <el-button @click="showConnectionHistory">连接历史</el-button>
            <el-button type="primary" @click="showAddHotelDialog = true">添加酒店</el-button>
          </div>
        </div>

        <!-- 酒店列表表格 -->
        <div class="hotel-table-section">
          <el-table :data="hotelList" border class="hotel-table">
            <el-table-column prop="hotelCode" label="酒店代码" min-width="120" align="center" />
            <el-table-column prop="hotelName" label="酒店名称" min-width="200" align="center" />
            <el-table-column prop="storeType" label="Agoda门店类型" min-width="150" align="center" />
            <el-table-column prop="priceMode" label="价格模式" min-width="120" align="center" />
            <el-table-column prop="status" label="状态" min-width="100" align="center" />
            <el-table-column label="操作" min-width="100" align="center" fixed="right">
              <template #default="{ row }">
                <el-button type="text" size="small">编辑</el-button>
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
      </div>
    </div>

    <!-- 价格比例页面 -->
    <div class="main-content price-ratio-view" v-if="showPriceRatio">
      <div class="settings-header">
        <div class="breadcrumb">
          <el-button type="text" @click="closePriceRatio" class="back-btn">
            <el-icon><ArrowLeft /></el-icon>
            收起导航
          </el-button>
          <span class="breadcrumb-separator">/</span>
          <span class="breadcrumb-item active">价格比例</span>
        </div>
      </div>

      <div class="settings-content">
        <div class="price-ratio-table-container">
          <el-table :data="priceRatioData" border stripe class="price-ratio-table">
            <el-table-column prop="channel" label="渠道" min-width="150" align="center" />
            <el-table-column prop="ratio" label="价格比例" min-width="200" align="center" />
            <el-table-column label="操作" width="150" align="center">
              <template #default="{ row }">
                <el-button type="text" size="small" @click="editPriceRatio(row)">编辑</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>
    </div>

    <!-- 编辑价格比例对话框 -->
    <el-dialog v-model="showEditRatioDialog" title="编辑价格比例" width="500px">
      <div class="edit-ratio-form">
        <el-form v-if="currentEditingRatio" :model="currentEditingRatio" label-width="100px">
          <el-form-item label="渠道">
            <el-input v-model="currentEditingRatio.channel" disabled></el-input>
          </el-form-item>
          <el-form-item label="价格比例">
            <el-input v-model="currentEditingRatio.ratio" placeholder="请输入价格比例"></el-input>
          </el-form-item>
        </el-form>
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
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowLeft, Setting, Connection, Remove, InfoFilled } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()

// 响应式数据
const activeMenu = ref('channel-list')
const activeTab = ref('ota')
const showConnectDialog = ref(false)
const selectedChannel = ref<any>(null)
const showChannelSettings = ref(false)
const currentSettingChannel = ref<any>(null)
const settingsTab = ref('房源管理')
const selectedAccount = ref('all')
const selectedStatus = ref('all')
const showAddAccountDialog = ref(false)
const showEditRatioDialog = ref(false)
const currentEditingRatio = ref<any>(null)
const showPriceRatio = ref(false)
const showAddHotelDialog = ref(false)
const showHistoryDialog = ref(false)

// 酒店列表数据
const hotelList = ref<any[]>([])

// 连接历史数据
const connectionHistory = ref<any[]>([])

// 价格比例数据
const priceRatioData = ref([
  { id: 1, channel: 'Agoda', ratio: '等同于基本价格' },
  { id: 2, channel: 'Airbnb', ratio: '等同于基本价格' },
  { id: 3, channel: 'Booking.com', ratio: '45 % 比基准价贵' },
  { id: 4, channel: 'Traveloka', ratio: '等同于基本价格' },
  { id: 5, channel: 'Trip.com', ratio: '等同于基本价格' },
  { id: 6, channel: 'Expedia', ratio: '等同于基本价格' },
  { id: 7, channel: 'Tiket.com', ratio: '等同于基本价格' },
  { id: 8, channel: 'Neppan', ratio: '等同于基本价格' },
  { id: 9, channel: 'HostelWorld', ratio: '等同于基本价格' },
  { id: 10, channel: 'TuJia', ratio: '等同于基本价格' }
])

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

// OTA渠道数据
const otaChannels = ref([
  {
    id: 1,
    name: 'Agoda',
    logoUrl: 'https://cdn.worldvectorlogo.com/logos/agoda-1.svg',
    connected: false,
  },
  {
    id: 2,
    name: 'Airbnb',
    logoUrl: 'https://upload.wikimedia.org/wikipedia/commons/6/69/Airbnb_Logo_B%C3%A9lo.svg',
    connected: false,
  },
  {
    id: 3,
    name: 'Booking.com',
    logoUrl: 'https://upload.wikimedia.org/wikipedia/commons/b/be/Booking.com_logo.svg',
    connected: false,
  },
  {
    id: 4,
    name: 'Traveloka',
    logoUrl: 'https://upload.wikimedia.org/wikipedia/commons/5/5b/Traveloka_logo.svg',
    connected: false,
  },
  {
    id: 5,
    name: 'Trip.com',
    logoUrl: 'https://ak-d.tripcdn.com/images/0ww5h12000c6vhxm53B87.png',
    connected: false,
  },
  {
    id: 6,
    name: 'Expedia',
    logoUrl: 'https://upload.wikimedia.org/wikipedia/commons/5/5b/Expedia_2012_logo.svg',
    connected: false,
  },
  {
    id: 7,
    name: 'Tiket.com',
    logoUrl: 'https://www.tiket.com/img/tiket-logo.svg',
    connected: false,
  },
  {
    id: 8,
    name: 'HostelWorld',
    logoUrl: 'https://a.hwstatic.com/image/upload/f_auto,q_auto,h_63/v1/propertyimages/0/8914/x5ecdkqgtrzfmcyiykfb',
    connected: false,
  },
  {
    id: 9,
    name: 'TuJia',
    logoUrl: 'https://pages.c-ctrip.com/hotels/wuhan/tujia-logo.png',
    connected: false,
  },
])

// 渠道管理数据
const managementChannels = ref([
  {
    id: 10,
    name: 'Neppan',
    logoUrl: 'https://via.placeholder.com/120x60/FF6B35/FFFFFF?text=Neppan',
    connected: false,
  },
])

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

// 方法
const handleMenuSelect = (index: string) => {
  activeMenu.value = index
  if (index === 'price-ratio') {
    showPriceRatio.value = true
    showChannelSettings.value = false
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
}

const closeChannelSettings = () => {
  showChannelSettings.value = false
  currentSettingChannel.value = null
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

// 编辑价格比例
const editPriceRatio = (row: any) => {
  currentEditingRatio.value = { ...row }
  showEditRatioDialog.value = true
}

// 保存价格比例
const savePriceRatio = () => {
  if (!currentEditingRatio.value) return

  const index = priceRatioData.value.findIndex(item => item.id === currentEditingRatio.value.id)
  if (index > -1) {
    priceRatioData.value[index] = { ...currentEditingRatio.value }
  }

  showEditRatioDialog.value = false
  currentEditingRatio.value = null
  ElMessage.success('价格比例已更新')
}

// 显示连接历史
const showConnectionHistory = () => {
  showHistoryDialog.value = true
}

// 开始授权
const startAuthorization = () => {
  showAddHotelDialog.value = false
  ElMessage.success('正在跳转到授权页面...')
  // TODO: 实现实际的授权逻辑
}

onMounted(() => {
  // 组件挂载后的初始化逻辑
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
}

.sidebar-header {
  padding: 16px;
  border-bottom: 1px solid #e8e8e8;
  display: flex;
  align-items: center;
  gap: 8px;
}

.sidebar-title {
  font-size: 14px;
  color: #666;
}

.sidebar-menu {
  flex: 1;
  border-right: none;
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

.hotel-table-section {
  background: white;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
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
  padding: 20px 0;
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
</style>
