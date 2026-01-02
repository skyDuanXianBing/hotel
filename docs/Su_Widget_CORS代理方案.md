# Su Widget 方案2：后端代理绕过 Su CORS（沙盒/生产通用）

## 背景（为什么会白屏）

Su Widget 的前端脚本（`https://static.otaswitch.com/JS/script.js` → `https://partner.su-api.com/...`）会在**浏览器内**直接请求 Su 的 Config 接口：

- `https://connect-sandbox.su-api.com/Config/...`（沙盒）
- `https://connect.su-api.com/Config/...`（生产）

但 Su 服务端通常不会对第三方域名返回 `Access-Control-Allow-Origin`，浏览器会拦截请求并报 CORS，Widget 就会“空白”。

## 方案说明（后端反向代理 + 前端重写请求）

本项目做法：

1. 前端在打开 Widget 弹窗期间，临时拦截 XHR/fetch：
   - 把 `connect(-sandbox).su-api.com/Config/...` 的请求改写到本系统后端
   - 自动注入本系统 `localStorage.token` 作为 `Authorization: Bearer ...`，用于访问后端 `/api/v1/**`
   - 如果 Su 的请求本身携带 `Authorization`，会搬运到 `X-Su-Authorization`，由后端再转回 Su（避免和本系统 JWT 冲突）

2. 后端提供受控代理接口（仅允许代理 Su Config 的 `jservice` 路径）：
   - `GET/POST /api/v1/su/config/sandbox/jservice/**` → `https://connect-sandbox.su-api.com/Config/jservice/**`
   - `GET/POST /api/v1/su/config/prod/jservice/**` → `https://connect.su-api.com/Config/jservice/**`

对应代码：
- 前端：`client/src/components/ConnectOtaWidget.vue`
- 后端：`server/src/main/java/server/demo/controller/SuConfigProxyController.java`

## 沙盒环境怎么测（你现在的情况）

前提：
- 你已经能拿到 Su Widget Token（后端日志显示 `Su Widget token generated successfully`）
- 前端 `VITE_API_BASE_URL` 指向可访问的后端（例如你现在的内网穿透/部署域名）
- 浏览器已登录系统（localStorage 有 `token`）

然后打开“渠道管理 → 配置”，Widget 内部的 Su Config 请求会自动走代理，不再触发 Su 的 CORS。

## 生产环境能不能用

可以用，但建议作为“兜底方案”：

- 优点：不依赖 Su 侧为你域名开 CORS，稳定可控
- 风险/成本：
  - 需要你维护代理转发（Su 接口/路径调整时可能要跟着改）
  - 必须避免成为通用开放代理（本项目已限制域名与路径）
  - 需要确认符合 Su 的集成规范/条款（建议同时让 Su 知会/确认）

如果 Su 愿意为你的生产域名开启 CORS（官方白名单），那是更“标准”的方案；否则方案2可长期使用。

