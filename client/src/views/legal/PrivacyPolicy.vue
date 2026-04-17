<template>
  <div class="legal-page">
    <div class="legal-container">
      <h1 class="legal-title">隐私政策</h1>
      <p class="legal-meta">生效日期：2026-04-17</p>
      <p class="legal-intro">
        本隐私政策用于说明房东智控中心（THE HOST HUB，以下简称“本系统”）如何收集、使用、存储、共享和保护您的个人信息，以及您如何行使相关权利。
      </p>

      <section class="legal-section">
        <h2>一、我们收集的信息</h2>
        <p>1. 账号信息：如邮箱、密码（加密存储）、登录凭证、账号标识。</p>
        <p>2. 业务信息：如订单信息、房态信息、渠道配置、门店信息、操作日志。</p>
        <p>3. 设备与日志信息：如 IP 地址、浏览器类型、访问时间、错误日志、设备标识符。</p>
        <p>4. 您主动提交的信息：如客服咨询内容、问题反馈信息、工单记录。</p>
      </section>

      <section class="legal-section">
        <h2>二、我们如何使用信息</h2>
        <p>1. 提供并维护系统核心功能，包括身份验证、业务处理、数据同步和消息通知。</p>
        <p>2. 保障账号与系统安全，包括风险识别、故障排查、审计追踪和异常预警。</p>
        <p>3. 改进产品体验与服务质量，包括功能优化、性能分析和使用统计。</p>
        <p>4. 履行法定义务及配合有权机关依法提出的要求。</p>
      </section>

      <section class="legal-section">
        <h2>三、信息共享、转让与公开披露</h2>
        <p>1. 我们不会向无关第三方出售您的个人信息。</p>
        <p>2. 在以下场景中，我们可能共享必要信息：</p>
        <p>（1）为实现服务功能，与经授权的技术服务提供方共享必要数据。</p>
        <p>（2）应法律法规、司法或行政机关要求进行披露。</p>
        <p>（3）在发生合并、分立、资产转让时，依法告知并保障信息接收方继续履行保护义务。</p>
      </section>

      <section class="legal-section">
        <h2>四、Cookie 与同类技术</h2>
        <p>
          为保障登录状态与服务体验，本系统可能使用 Cookie 或本地存储（如 localStorage）保存必要会话信息。您可通过浏览器设置管理相关数据，但可能影响部分功能的正常使用。
        </p>
      </section>

      <section class="legal-section">
        <h2>五、信息存储与安全保护</h2>
        <p>1. 我们在中国境内存储和处理个人信息，法律法规另有要求的除外。</p>
        <p>2. 我们采取访问控制、权限隔离、传输加密、日志审计、备份恢复等措施保护信息安全。</p>
        <p>3. 尽管我们采取合理措施，互联网并非绝对安全环境，请您妥善保管账号与密码。</p>
      </section>

      <section class="legal-section">
        <h2>六、您的权利</h2>
        <p>在符合法律法规的前提下，您有权访问、更正、删除您的个人信息，并可申请撤回授权、注销账号或获取相关副本。</p>
        <p>如您提出请求，我们将在核实身份后于合理期限内处理并反馈结果。</p>
      </section>

      <section class="legal-section">
        <h2>七、未成年人保护</h2>
        <p>
          本系统主要面向具备完全民事行为能力的经营主体及其授权人员。若您为未成年人，请在监护人指导下使用本系统，并确保已取得监护人同意。
        </p>
      </section>

      <section class="legal-section">
        <h2>八、政策更新</h2>
        <p>
          我们可能根据业务变化、法律法规要求对本政策进行更新。更新后将通过系统公告或页面提示方式告知。重大变更将以更显著方式提示。
        </p>
      </section>

      <section class="legal-section">
        <h2>九、联系我们</h2>
        <p>如您对本隐私政策有疑问、意见或投诉建议，可通过系统内客服渠道或运营方公示方式与我们联系。</p>
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
