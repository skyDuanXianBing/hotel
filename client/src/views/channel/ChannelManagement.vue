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
        <el-menu-item index="channel-management">
          <el-icon><Setting /></el-icon>
          <span>渠道管理</span>
        </el-menu-item>
        <el-menu-item index="channel-list">
          <span>渠道列表</span>
        </el-menu-item>
        <el-menu-item index="channel-automation">
          <span>渠道自动化</span>
        </el-menu-item>
      </el-menu>
    </div>

    <!-- 主内容区域 -->
    <div class="main-content" v-if="!showChannelSettings">
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
      <div v-if="activeTab === 'ota'" class="channel-grid">
        <div v-for="channel in channels" :key="channel.id" class="channel-card">
          <div class="card-header">
            <div class="channel-logo" :class="channel.logoClass">
              <span class="logo-text">{{ channel.logoText }}</span>
            </div>
            <h3 class="channel-name">{{ channel.name }}</h3>
            <div class="channel-status">
              <el-tag v-if="channel.connected" type="success" size="small">
                <el-icon><Connection /></el-icon>
                直连中
              </el-tag>
              <el-tag v-else type="info" size="small">
                <el-icon><Remove /></el-icon>
                未直连
              </el-tag>
            </div>
          </div>

          <div class="card-content">
            <div v-if="channel.connected" class="connected-info">
              <p class="room-type-info">{{ channel.roomTypeText }}</p>
              <el-button type="text" size="small" @click="openSettings(channel)"> 设置 </el-button>
            </div>
            <div v-else class="connect-action">
              <el-button
                type="primary"
                @click="connectChannel(channel)"
                :class="channel.buttonClass"
              >
                {{ channel.buttonText }}
              </el-button>
            </div>
          </div>

          <!-- 推荐标签 -->
          <div v-if="channel.recommended" class="recommend-tag">推荐</div>
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
            收起导航
          </el-button>
          <span class="breadcrumb-separator">/</span>
          <span class="breadcrumb-item">直连设置</span>
          <span class="breadcrumb-separator">/</span>
          <span class="breadcrumb-item active">{{ currentSettingChannel?.name || '' }}渠道设置</span>
        </div>
      </div>

      <div class="settings-content">
        <!-- 渠道信息卡片 -->
        <div class="channel-info-card">
          <div class="channel-logo-section">
            <div class="channel-logo-large" :class="currentSettingChannel?.logoClass">
              <span class="logo-text-large">{{ currentSettingChannel?.logoText }}</span>
            </div>
          </div>
          <div class="channel-desc">
            <h3>{{ currentSettingChannel?.name || '' }}渠道直连</h3>
            <p>
              您已开通{{ currentSettingChannel?.name || '' }}直连，下一步请在下方列表
              <span class="highlight">关联房型</span>
              ，建立连接关系。
            </p>
            <p>
              直连后，渠道将自动同步PMS的房态、库存、价格，渠道订单将自动落入订单来了。注意，同步是
              <span class="highlight">单向</span>
              的，在渠道cbk修改的房态、库存、价格将不会同步到PMS，点击<a href="#" class="link"
                >查看操作指南</a
              >。
            </p>
          </div>
        </div>

        <!-- Tab 切换 -->
        <div class="settings-tabs">
          <el-tabs v-model="settingsTab">
            <el-tab-pane label="房源管理" name="房源管理">
              <div class="tab-content">
                <div class="filter-bar">
                  <div class="filter-item">
                    <label>房东账号</label>
                    <el-select v-model="selectedAccount" placeholder="请选择" style="width: 200px">
                      <el-option label="全部" value="all"></el-option>
                      <el-option
                        v-for="account in channelAccounts"
                        :key="account.id"
                        :label="account.name"
                        :value="account.id"
                      ></el-option>
                    </el-select>
                  </div>
                  <div class="filter-item">
                    <label>直连状态</label>
                    <el-select v-model="selectedStatus" placeholder="全部" style="width: 200px">
                      <el-option label="全部" value="all"></el-option>
                      <el-option label="已直连" value="connected"></el-option>
                      <el-option label="未直连" value="disconnected"></el-option>
                    </el-select>
                  </div>
                  <div class="filter-actions">
                    <el-button type="primary">刷新房源信息</el-button>
                  </div>
                </div>

                <!-- 表格 -->
                <el-table :data="[]" class="room-source-table" empty-text="暂无房源">
                  <el-table-column prop="channelRoom" label="渠道房源" />
                  <el-table-column prop="status" label="状态" />
                  <el-table-column prop="relatedRoom" label="关联房型/房间" />
                  <el-table-column prop="actions" label="操作" />
                </el-table>

                <!-- 空状态 -->
                <div class="empty-state">
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
                  <p class="empty-text">
                    暂无房源，请在{{ currentSettingChannel?.name || '' }}中添加房源信息，<br />
                    或尝试添加<a href="#" class="link">其他美团民宿房源乐账号</a>
                  </p>
                </div>
              </div>
            </el-tab-pane>
            <el-tab-pane label="账号管理" name="账号管理">
              <div class="tab-content">
                <!-- 账号说明 -->
                <div class="account-notice">
                  <el-icon><InfoFilled /></el-icon>
                  <span>您可以添加多个美团民宿门店（最多5个），房源统一管理。</span>
                </div>

                <!-- 添加账号按钮 -->
                <div class="account-actions">
                  <el-button type="primary" @click="showAddAccountDialog = true">添加账号</el-button>
                </div>

                <!-- 账号列表表格 -->
                <div class="account-table">
                  <el-table :data="channelAccounts" style="width: 100%">
                    <el-table-column prop="name" label="账号名" min-width="300"></el-table-column>
                    <el-table-column prop="roomCount" label="关联房源数" width="150">
                      <template #default="scope">
                        <span>{{ scope.row.roomCount || 0 }}</span>
                      </template>
                    </el-table-column>
                    <el-table-column label="操作" width="200">
                      <template #default="scope">
                        <el-button type="text" size="small" @click="viewAccountRooms(scope.row)">
                          查看房源
                        </el-button>
                        <el-button
                          type="text"
                          size="small"
                          @click="removeChannelAccount(scope.row)"
                          class="danger-link"
                        >
                          解除直连
                        </el-button>
                      </template>
                    </el-table-column>
                  </el-table>
                </div>
              </div>
            </el-tab-pane>
          </el-tabs>
        </div>
      </div>
    </div>

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
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowLeft, Setting, Connection, Remove, InfoFilled } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()

// 响应式数据
const activeMenu = ref('channel-management')
const activeTab = ref('ota')
const showConnectDialog = ref(false)
const selectedChannel = ref<any>(null)
const showChannelSettings = ref(false)
const currentSettingChannel = ref<any>(null)
const settingsTab = ref('房源管理')
const selectedAccount = ref('all')
const selectedStatus = ref('all')
const showAddAccountDialog = ref(false)

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

// 渠道数据
const channels = ref([
  {
    id: 1,
    name: '美团民宿',
    logoText: '美团',
    logoClass: 'meituan-logo',
    connected: true,
    roomTypeText: '尚未关联房型',
    buttonText: '',
    buttonClass: '',
    recommended: false,
  },
  {
    id: 2,
    name: '途家',
    logoText: '途家',
    logoClass: 'tujia-logo',
    connected: false,
    buttonText: '开通直连',
    buttonClass: 'connect-btn',
    recommended: false,
  },
  {
    id: 3,
    name: '小猪民宿',
    logoText: '小猪',
    logoClass: 'xiaozhu-logo',
    connected: false,
    buttonText: '开通直连',
    buttonClass: 'connect-btn',
    recommended: false,
  },
  {
    id: 4,
    name: '木鸟民宿',
    logoText: '木鸟',
    logoClass: 'muniao-logo',
    connected: false,
    buttonText: '开通直连',
    buttonClass: 'connect-btn',
    recommended: false,
  },
  {
    id: 5,
    name: '飞猪',
    logoText: '飞猪',
    logoClass: 'fliggy-logo',
    connected: false,
    buttonText: '立即订购',
    buttonClass: 'purchase-btn',
    recommended: true,
  },
  {
    id: 6,
    name: '携程',
    logoText: '携程',
    logoClass: 'ctrip-logo',
    connected: false,
    buttonText: '立即订购',
    buttonClass: 'purchase-btn',
    recommended: true,
  },
  {
    id: 7,
    name: '美团酒店',
    logoText: '美团',
    logoClass: 'meituan-hotel-logo',
    connected: false,
    buttonText: '立即订购',
    buttonClass: 'purchase-btn',
    recommended: true,
  },
  {
    id: 8,
    name: 'Agoda',
    logoText: 'A',
    logoClass: 'agoda-logo',
    connected: false,
    buttonText: '立即订购',
    buttonClass: 'purchase-btn',
    recommended: true,
  },
  {
    id: 9,
    name: 'Expedia',
    logoText: 'E',
    logoClass: 'expedia-logo',
    connected: false,
    buttonText: '立即订购',
    buttonClass: 'purchase-btn',
    recommended: true,
  },
  {
    id: 10,
    name: 'Booking.com',
    logoText: 'B',
    logoClass: 'booking-logo',
    connected: false,
    buttonText: '咨询开展开店',
    buttonClass: 'consult-btn',
    recommended: true,
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
  // 这里可以添加菜单切换逻辑
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

/* 渠道网格 */
.channel-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
  padding: 20px;
}

.channel-card {
  background: white;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  position: relative;
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease;
}

.channel-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
}

.card-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 16px;
}

.channel-logo {
  width: 60px;
  height: 60px;
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f8f9fa;
  border-radius: 8px;
}

.channel-logo img {
  width: 40px;
  height: 40px;
  object-fit: contain;
}

.logo-text {
  font-size: 14px;
  font-weight: bold;
  color: white;
}

/* 不同渠道的Logo样式 */
.meituan-logo {
  background: linear-gradient(135deg, #ffd900, #ffb700);
}

.tujia-logo {
  background: linear-gradient(135deg, #ff6b35, #f7931e);
}

.xiaozhu-logo {
  background: linear-gradient(135deg, #ff1493, #ff69b4);
}

.muniao-logo {
  background: linear-gradient(135deg, #ff4500, #ff8c00);
}

.fliggy-logo {
  background: linear-gradient(135deg, #1890ff, #40a9ff);
}

.ctrip-logo {
  background: linear-gradient(135deg, #0066cc, #4096ff);
}

.meituan-hotel-logo {
  background: linear-gradient(135deg, #52c41a, #73d13d);
}

.agoda-logo {
  background: linear-gradient(135deg, #722ed1, #9254de);
}

.expedia-logo {
  background: linear-gradient(135deg, #faad14, #ffc53d);
}

.booking-logo {
  background: linear-gradient(135deg, #1890ff, #69c0ff);
}

.channel-name {
  font-size: 16px;
  font-weight: 500;
  margin: 0 0 8px 0;
  text-align: center;
}

.channel-status {
  text-align: center;
}

.card-content {
  text-align: center;
}

.connected-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.room-type-info {
  font-size: 12px;
  color: #666;
  margin: 0;
}

.connect-action {
  margin-top: 8px;
}

/* 按钮样式 */
.connect-btn {
  background: #1890ff;
  border-color: #1890ff;
  width: 100%;
}

.purchase-btn {
  background: #1890ff;
  border-color: #1890ff;
  width: 100%;
}

.consult-btn {
  background: #52c41a;
  border-color: #52c41a;
  width: 100%;
  font-size: 12px;
}

/* 推荐标签 */
.recommend-tag {
  position: absolute;
  top: -1px;
  right: -1px;
  background: linear-gradient(135deg, #ff4d4f, #ff7875);
  color: white;
  font-size: 12px;
  padding: 4px 12px;
  border-radius: 0 8px 0 12px;
  font-weight: 500;
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
}

.channel-desc h3 {
  font-size: 18px;
  font-weight: 500;
  color: #333;
  margin: 0 0 16px 0;
}

.channel-desc p {
  font-size: 14px;
  line-height: 1.8;
  color: #666;
  margin: 0 0 12px 0;
}

.channel-desc p:last-child {
  margin-bottom: 0;
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
</style>
