<template>
  <div class="legal-page">
    <div class="legal-container">
      <h1 class="legal-title">用户服务协议</h1>
      <p class="legal-meta">生效日期：2026-04-17</p>
      <p class="legal-intro">
        欢迎使用房东智控中心（THE HOST HUB，以下简称“本系统”）。本协议由平台运营方（以下简称“我们”）与您（包括酒店、民宿、公寓等经营主体及其授权使用人）共同签署并具有法律效力。
      </p>

      <section class="legal-section">
        <h2>一、协议适用范围</h2>
        <p>
          本协议适用于您访问、注册、登录及使用本系统提供的门店管理、房态管理、订单处理、渠道管理、账号权限管理、消息通知及其他相关功能与服务。
        </p>
      </section>

      <section class="legal-section">
        <h2>二、账号注册与使用</h2>
        <p>1. 您应当提供真实、准确、完整的注册信息，并及时更新。</p>
        <p>2. 您应妥善保管账号、密码、验证码及其他登录凭证，并对该账号下全部操作承担责任。</p>
        <p>3. 未经我们书面许可，您不得出租、出借、转让账号，不得通过任何方式非法获取或使用他人账号。</p>
      </section>

      <section class="legal-section">
        <h2>三、服务内容与使用规范</h2>
        <p>1. 您可在服务有效期内使用本系统提供的功能，具体以实际开通模块为准。</p>
        <p>
          2.
          您在使用过程中应遵守中华人民共和国法律法规，不得利用本系统从事任何违法违规活动，包括但不限于发布违法信息、恶意攻击系统、干扰正常网络秩序等。
        </p>
        <p>3. 您不得逆向工程、反编译、破解、抓取或以其他方式破坏本系统的安全性与稳定性。</p>
      </section>

      <section class="legal-section">
        <h2>四、门店与数据管理</h2>
        <p>1. 您通过本系统录入、上传、维护的数据（包括订单、房态、客户信息、渠道信息等）由您自行管理并对其合法性负责。</p>
        <p>2. 您应确保已依法取得处理相关信息的必要授权，并建立内部权限控制与审计机制。</p>
        <p>3. 为保障系统运行，我们将采取合理技术措施进行数据隔离、备份与恢复，但不承诺绝对无中断或无错误。</p>
      </section>

      <section class="legal-section">
        <h2>五、费用与服务变更</h2>
        <p>1. 如涉及付费服务，具体价格、计费规则及开票方式以您与我们另行约定或页面公示为准。</p>
        <p>2. 我们有权基于业务发展对功能、接口、计费模式进行优化或调整，并将以合理方式提前通知。</p>
      </section>

      <section class="legal-section">
        <h2>六、知识产权</h2>
        <p>1. 本系统及其相关软件、页面、文档、商标、标识等知识产权归我们或相关权利人所有。</p>
        <p>2. 未经许可，您不得以复制、传播、改编、出售、许可等方式使用上述内容。</p>
      </section>

      <section class="legal-section">
        <h2>七、免责与责任限制</h2>
        <p>1. 因不可抗力、网络故障、第三方服务异常、监管要求等非我们可控原因导致的服务中断或数据异常，我们将在合理范围内协助处理。</p>
        <p>2. 对于因您自身原因（如账号泄露、误操作、违规使用）造成的损失，由您自行承担。</p>
        <p>3. 在法律允许范围内，我们承担责任的总额以您在相关争议发生前十二个月内实际支付的服务费用为上限。</p>
      </section>

      <section class="legal-section">
        <h2>八、协议终止</h2>
        <p>1. 若您严重违反本协议或相关法律法规，我们有权暂停或终止向您提供全部或部分服务。</p>
        <p>2. 服务终止后，除法律法规另有规定外，我们将根据数据保留策略处理相关数据。</p>
      </section>

      <section class="legal-section">
        <h2>九、法律适用与争议解决</h2>
        <p>1. 本协议的订立、履行与解释适用中华人民共和国法律。</p>
        <p>2. 因本协议引起的争议，双方应先友好协商；协商不成的，任一方可向平台运营方所在地有管辖权的人民法院提起诉讼。</p>
      </section>

      <section class="legal-section">
        <h2>十、联系我们</h2>
        <p>如您对本协议有疑问，可通过系统内客服渠道或运营方公示联系方式与我们联系。</p>
      </section>
    </div>
  </div>
</template>

<style scoped>
.legal-page {
  min-height: 100vh;
  background: #f5f7fa;
  padding: 32px 16px;
}

.legal-container {
  max-width: 920px;
  margin: 0 auto;
  background: #fff;
  border-radius: 12px;
  padding: 28px 24px;
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.06);
}

.legal-title {
  margin: 0;
  font-size: 30px;
  line-height: 1.3;
  color: #1f2d3d;
}

.legal-meta {
  margin: 12px 0 20px;
  color: #606266;
  font-size: 14px;
}

.legal-intro {
  margin: 0 0 20px;
  color: #303133;
  line-height: 1.8;
}

.legal-section {
  margin-top: 18px;
}

.legal-section h2 {
  margin: 0 0 8px;
  font-size: 18px;
  color: #1f2d3d;
}

.legal-section p {
  margin: 0 0 8px;
  color: #303133;
  line-height: 1.8;
}

@media (max-width: 768px) {
  .legal-container {
    padding: 22px 16px;
  }

  .legal-title {
    font-size: 24px;
  }
}
</style>
