<template>
  <div class="legal-page">
    <div class="legal-container">
      <h1 class="legal-title">技术支持网站</h1>
      <p class="legal-meta">生效日期：2026-04-25</p>
      <p class="legal-intro">
        本页面用于说明房东智控中心（THE HOST HUB）为用户提供的技术支持方式、服务范围、响应流程及联系渠道，帮助您更高效地提交问题并获得支持。
      </p>

      <section class="legal-section">
        <h2>一、支持范围</h2>
        <p>1. 账号登录、注册、验证码、密码找回等基础访问问题。</p>
        <p>2. 门店管理、订单处理、房态维护、渠道配置等系统功能使用问题。</p>
        <p>3. 页面异常、接口报错、数据展示异常、权限异常等故障排查协助。</p>
        <p>4. 产品使用建议、流程咨询、问题反馈与服务改进建议。</p>
      </section>

      <section class="legal-section">
        <h2>二、建议您在反馈时提供的信息</h2>
        <p>1. 您的账号邮箱、所属门店或相关业务场景说明。</p>
        <p>2. 问题发生时间、操作步骤、报错提示或异常现象描述。</p>
        <p>3. 如条件允许，建议附上截图、录屏或相关订单编号，以便更快定位问题。</p>
      </section>

      <section class="legal-section">
        <h2>三、支持方式</h2>
        <p>1. 您可优先通过系统内已有客服或反馈渠道提交问题。</p>
        <p>2. 如需人工协助，也可通过下方公示联系方式与技术支持团队联系。</p>
      </section>

      <section class="legal-section">
        <h2>四、联系方式</h2>
        <p>1. 支持邮箱：saitohiroshi01@gmail.com</p>
        <p>2. 服务时间：周一至周五 10:00-19:00（UTC+9）</p>
      </section>

      <section class="legal-section">
        <h2>五、处理说明</h2>
        <p>1. 我们将在收到反馈后尽快进行分析与处理，并根据问题复杂度安排响应。</p>
        <p>2. 如问题涉及第三方平台、网络环境或账号权限配置，处理时间可能相应延长。</p>
        <p>3. 对于影响业务使用的紧急问题，建议在反馈中明确标注影响范围和紧急程度。</p>
      </section>

      <section class="legal-section">
        <h2>六、其他说明</h2>
        <p>1. 技术支持服务仅用于协助定位和处理系统使用相关问题，不构成额外承诺或保证。</p>
        <p>2. 如本页面内容发生调整，我们将通过系统页面更新相关信息。</p>
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
