<script setup lang="ts">
defineProps<{
  modelValue: boolean
  channel: { name: string; logoText?: string; logoClass?: string } | null
}>()

defineEmits<{
  'update:modelValue': [value: boolean]
  confirm: []
}>()
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    :title="`${channel?.name || ''}直连须知`"
    width="700px"
    :close-on-click-modal="false"
    @update:model-value="$emit('update:modelValue', $event)"
  >
    <div class="connect-dialog-content">
      <div class="connect-header">
        <p class="connect-description">
          完成{{ channel?.name || '' }}账号授权后，即可开始直连。<br />
          点击下一步，代表您已经阅读并同意《{{ channel?.name || '' }}直连须知》。
        </p>
        <div v-if="channel?.logoClass" class="channel-logo-large" :class="channel.logoClass">
          <span class="logo-text-large">{{ channel?.logoText }}</span>
        </div>
      </div>

      <!-- 直连须知内容 -->
      <div class="notice-section">
        <h3 class="notice-title">{{ channel?.name || '' }}直连须知</h3>
        <div class="notice-content">
          <div class="notice-text">
            <p>
              {{ channel?.name || '' }}直连功能（简称"直连"），是指酒店商户（"您"）在订单来了系统中设置直连后，由订单来了与{{
                channel?.name || ''
              }}系统自动建立直接连接，实现您通过订单来了管理{{ channel?.name || ''
              }}系统中的订单。该功能旨在减少酒店运营的操作成本，提高酒店运营效率。
            </p>
            <p>
              在您开通{{ channel?.name || ''
              }}直连功能前，请务必认真阅读和遵守本《用户须知》的条款内容，特别是免除或者限制责任的条款、争议解决和法律适用条款。如您对协议有任何疑问或不认可，应立即停止使用服务。当您使用{{
                channel?.name || ''
              }}直连功能时，即表明您已充分阅读、理解并接受本《用户须知》的全部内容，本协议经您在线确认后立即生效。协议条款如下：
            </p>

            <h4>1 功能介绍</h4>
            <p>
              1.1 在与{{ channel?.name || ''
              }}完成直连后，即可通过订单来了PMS管理{{ channel?.name || ''
              }}的渠道房价、房态，渠道订单将自动落入订单来了。
            </p>
            <p>
              1.2 1个订单来了网络号可与多个{{ channel?.name || ''
              }}账号直连。点击"开通直连"后，将跳转至{{ channel?.name || ''
              }}页面进行授权。为确保授权账号无误，授权之前请确保您在{{ channel?.name || ''
              }}是未登录状态，完成授权后才可以关联房型。
            </p>
            <p>
              1.3 {{ channel?.name || ''
              }}房源类型为集中式公寓、度假民宿、客栈、农家乐、房车的房源可以直接关联整个房型（允许多个库存），其他类型的房源只能关联单个房间（只允许1个库存）。
            </p>
            <p>
              1.4
              若要新增房源，请您在{{ channel?.name || ''
              }}后台新增并发布上架后，在订单来了系统中进行房型关联，建立直连关系。
            </p>
            <p>
              1.5 直连后，{{ channel?.name || ''
              }}订单将自动落入订单来了。闪订订单无需手动确认，普通订单请在渠道订单页面或{{
                channel?.name || ''
              }}后台进行接受或拒绝操作。需要注意的是，{{ channel?.name || ''
              }}订单落单时，处于待付款状态，需等待客人付款后才会变成已预订状态。若您选择渠道订单不排房，将无法在日历房态中看到待付款订单。
            </p>
            <p>
              1.6 {{ channel?.name || '' }}客人只允许取消订单，不可修改订单。
            </p>

            <h4>2 本平台将可能不定期的修改本《用户须知》的有关条款，一旦条款及服务内容产生变动，本平台将会提示修改内容。如果您继续使用本平台的服务，则视为接受该《用户须知》的条款变动。</h4>

            <h4>3 免责声明</h4>
            <p>
              ●
              您完全理解并同意，本服务涉及到互联网及移动通讯等服务，可能会受到各个环节不稳定因素的影响。因此任何因不可抗力、计算机病毒或黑客攻击、系统不稳定、互联网络、通信线路等其他本公司无法预测或控制的原因，造成的服务中断、取消或终止的风险。您须自行承担以上风险，本公司对服务之及时性、安全性、准确性不做任何保证。
            </p>
            <p>
              ● 您完全理解并同意，您的{{ channel?.name || ''
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
        <el-button @click="$emit('update:modelValue', false)">取消</el-button>
        <el-button type="primary" @click="$emit('confirm')">同意并开始授权</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<style scoped>
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
</style>
