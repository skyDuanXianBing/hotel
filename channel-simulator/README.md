# Channel Simulator

本目录是本地 Su / SiteMinder 渠道模拟器，用于联调 PMS 的 Su 集成闭环：

- PMS -> Mock Su API：PMS 开启 `CHANNEL_E2E_LOCAL_ENABLED` 后会使用本地 mock 参数调用 `/SUAPI/...`。
- Simulator -> PMS：Dashboard 或 API 触发 webhook，发送到 PMS 的 Su webhook 接收端。
- Pull Reservation：模拟器先登记 pending reservation，再发 `reservation_notif`，PMS 回拉 `/SUAPI/jservice/Reservation` 后再 ack `/SUAPI/jservice/Reservation_notif`。

## 安装

```bash
cd channel-simulator
npm install
```

## 构建与运行

开发监听：

```bash
npm run dev
```

类型检查：

```bash
npm run type-check
```

编译构建：

```bash
npm run build
```

运行编译产物：

```bash
npm run start
```

本地全链路验收脚本（需先启动 PMS 与 simulator）：

```bash
PMS_BASE_URL=http://localhost:8092 \
SIMULATOR_BASE_URL=http://localhost:4000 \
CHANNEL_E2E_TEST_SUPPORT_KEY=local-e2e-key \
npm run verify:local-e2e
```

默认端口是 `4000`。启动后访问：

```text
http://localhost:4000/
```

## Simulator 环境变量

| 变量 | 默认值 | 说明 |
| --- | --- | --- |
| `SIM_PORT` / `PORT` | `4000` | 模拟器监听端口。 |
| `PMS_BASE_URL` | `http://localhost:8092` | webhook 发送目标 PMS 服务基础地址，可通过环境变量覆盖。 |
| `DEFAULT_HOTEL_ID` | `1` | Dashboard 和 webhook 场景默认 hotelid。 |
| `WEBHOOK_AUTH_USERNAME` | 空 | 发送 webhook 到 PMS 时使用的 Basic Auth 用户名。 |
| `WEBHOOK_AUTH_PASSWORD` | 空 | 发送 webhook 到 PMS 时使用的 Basic Auth 密码。 |
| `PMS_AUTH_TOKEN` | 空 | 调用 PMS test-support API 的默认 Bearer token；可被 `/api/e2e/*` 请求头覆盖。 |
| `PMS_STORE_ID` | 空 | 调用 PMS test-support API 的默认 `X-Store-Id`；可被 `/api/e2e/*` 请求头覆盖。 |
| `CHANNEL_E2E_TEST_SUPPORT_KEY` | 空 | 调用 PMS setup-local 与 test-support API 的本地 E2E 管理密钥；设置后 Dashboard 不需要输入该密钥。 |
| `SU_CLIENT_ID` | `mock-client-id` | Mock Su token 端点接受的 client id。 |
| `SU_CLIENT_SECRET` | `mock-client-secret` | Mock Su token 端点接受的 client secret。 |

示例：

```bash
SIM_PORT=4000 \
PMS_BASE_URL=http://localhost:8092 \
DEFAULT_HOTEL_ID=1 \
WEBHOOK_AUTH_USERNAME=su-webhook \
WEBHOOK_AUTH_PASSWORD=su-secret \
SU_CLIENT_ID=mock-client-id \
SU_CLIENT_SECRET=mock-client-secret \
npm run dev
```

未设置 `PMS_BASE_URL` 时，模拟器默认连接 `http://localhost:8092`。

## PMS server/.env 配置

在 PMS 后端 `server/.env` 中启用本地渠道 E2E。该统一开关会同时启用 PMS test-support gate 与 Su API 本地 mock；`CHANNEL_E2E_TEST_SUPPORT_KEY` 必须显式配置，不能留空：

```env
CHANNEL_E2E_LOCAL_ENABLED=true
CHANNEL_E2E_TEST_SUPPORT_KEY=local-e2e-key
```

本地 mock 默认连接 `http://localhost:4000`，默认 client id / secret 为 `mock-client-id` / `mock-client-secret`。如需改模拟器地址或 mock 凭据，可额外覆盖：

```env
SU_API_LOCAL_MOCK_BASE_URL=http://localhost:4000
SU_API_LOCAL_MOCK_CLIENT_ID=mock-client-id
SU_API_LOCAL_MOCK_CLIENT_SECRET=mock-client-secret
```

如果 PMS 开启 Su webhook Basic Auth，需要让 PMS 与模拟器两侧对齐：

```env
SU_MESSAGING_WEBHOOK_USERNAME=su-webhook
SU_MESSAGING_WEBHOOK_PASSWORD=su-secret
```

同时启动模拟器时设置：

```bash
WEBHOOK_AUTH_USERNAME=su-webhook
WEBHOOK_AUTH_PASSWORD=su-secret
```

如果 PMS 的 `SU_MESSAGING_WEBHOOK_USERNAME` / `SU_MESSAGING_WEBHOOK_PASSWORD` 为空，PMS 侧不会校验 Basic Auth，模拟器也可以不设置 `WEBHOOK_AUTH_*`。

## Dashboard 使用流程

1. 启动 PMS 后端，并确认 PMS `server/.env` 中已配置 `CHANNEL_E2E_LOCAL_ENABLED=true` 与 `CHANNEL_E2E_TEST_SUPPORT_KEY=local-e2e-key`。
2. 启动 `channel-simulator`，打开 `http://localhost:4000/`。
3. 在 Dashboard 中确认配置：`PMS Base URL`、默认 `Hotel ID`、Su client 配置和 webhook auth 状态。
4. 选择场景，例如 `新预订 (Pull模式 - Booking.com)`。
5. 按需编辑 JSON payload，或勾选“使用服务端默认 payload”让模拟器使用内置 fixture。
6. 点击“发送到 PMS”，查看 PMS 响应。
7. 刷新日志，确认 webhook 发送、PMS 回拉 Mock Su API、Reservation_notif ack 等请求链路。

## E2E 控制台 UI

Dashboard 顶部的 E2E 控制台用于本地端到端检查，调用 simulator 侧 E2E API：

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `POST` | `/api/e2e/bootstrap` | 调用 PMS setup-local，初始化本地测试门店与当前页面会话凭据。 |
| `GET` | `/api/e2e/readiness` | 检查 PMS 当前门店的 Su 集成 readiness。 |
| `POST` | `/api/e2e/runs` | 按 mode、channel、scenario、房型/房间启动一次 E2E run。 |
| `GET` | `/api/e2e/runs/:runId` | 查询 E2E run 状态与 verifier 结果。 |
| `POST` | `/api/e2e/lifecycle` | 自动执行 new -> modification -> cancellation。 |
| `POST` | `/api/e2e/messaging` | 发送一条 OTA guest message webhook 并用 PMS test-support 验证消息入库。 |

使用流程：

1. 如果 simulator 后端没有设置 `CHANNEL_E2E_TEST_SUPPORT_KEY`，先在 `Environment / Readiness` 面板输入“本地 E2E 管理密钥”。该密钥只作为请求头传给 simulator，不会由后端 API 回传到页面。
2. 点击“初始化本地环境”，Dashboard 会调用 `POST /api/e2e/bootstrap`，由 simulator 后端请求 PMS `setup-local`，并把返回的 PMS token、`Store ID`、`suHotelId`、房型和房间写入当前页面内存。
3. 初始化成功后 readiness 会自动刷新；也可以点击“检查就绪状态”重新查看 `suHotelId`、ready 状态、缺失项、渠道、房型和房间摘要。
4. 在 `Builder` 面板选择 `PUSH` / `PULL`、`BOOKING` / `AIRBNB`、`NEW` / `MULTI_ROOM` / `AIRBNB_NEW`，并可选择 readiness 返回的房型和房间。
5. 点击“Run E2E”，Dashboard 会调用 `POST /api/e2e/runs`，请求体包含 `mode`、`channel`、`scenario`、可选 `roomTypeId` 和 `roomId`。`NEW` 是默认单房新单；`MULTI_ROOM` 会生成同一 reservation 下的多房新单；`AIRBNB_NEW` 会使用 Airbnb 新单字段。
6. `Run Steps`、`Verifier` 和 `E2E Logs` 面板会展示 runId、生成 ids、步骤、send response、reservation lookup 和 webhook event verifier。需要重新拉取时点击“刷新 Run”。
7. 点击“Run Lifecycle”会调用 `POST /api/e2e/lifecycle`，使用当前 `mode` 和 `channel` 自动执行 new -> modification -> cancellation，并在 `Lifecycle Steps / Verifier` 面板展示每一步的 run、steps 和 verifier。

`/api/e2e/readiness`、`POST /api/e2e/runs`、`GET /api/e2e/runs/:runId`、`POST /api/e2e/lifecycle` 调 PMS test-support API 时，认证与门店上下文优先级如下：

1. 当前请求头 `Authorization` 与 `X-Store-Id`。
2. simulator 环境变量 `PMS_AUTH_TOKEN` 与 `PMS_STORE_ID`。

Dashboard 输入框只负责为当前页面请求设置上述请求头，不会持久化 token。PMS base URL、webhook Basic Auth、Su client id/secret 等运行配置仍由 simulator 环境变量提供。

## 典型测试场景

### Pull 新预订

1. Dashboard 选择 `new-booking-pull`。
2. 使用默认 payload 或编辑 `reservation_notif.reservation_notif_id`。
3. 发送 webhook。
4. PMS 收到 notif 后回拉 `/SUAPI/jservice/Reservation`。
5. PMS 处理成功后调用 `/SUAPI/jservice/Reservation_notif` ack。
6. Dashboard 日志中应看到 webhook 发送、Reservation pull、Reservation_notif ack。

### Push 新预订 / 修改 / 取消

1. Dashboard 选择 `new-booking-push`、`modification-push` 或 `cancellation-push`。
2. 建议勾选“使用服务端默认 payload”，使用内置 reservation fixture。
3. 发送 webhook。
4. PMS 直接从 webhook body 中解析 reservation，不需要回拉 Mock Su。

### 批量 Pull

1. Dashboard 选择 `batch-pull`。
2. 使用默认 payload 或保留编辑器中 25 个 notif id。
3. 发送 webhook。
4. 用于验证 PMS 对超过 20 条 notif 的异步处理路径。

### Messaging webhook

1. Dashboard 选择 `guest-message`，可手工发送消息 webhook。
2. 更严格的本地入库验证使用 E2E API：`POST /api/e2e/messaging`，请求头带 PMS token 和 `X-Store-Id`。
3. 模拟器会生成 run-scoped `messageid` / `threadid` / `bookingid`，发送到 PMS `/api/v1/su/webhook/messaging`，再调用 PMS test-support messaging 查询接口验证 `su_message_threads` / `su_messages` 已写入本地数据库。

示例：

```bash
curl -X POST http://localhost:4000/api/e2e/messaging \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Bearer <token>' \
  -H 'X-Store-Id: <storeId>' \
  -d '{"channel":"AIRBNB"}'
```

## Dashboard 功能

- 场景列表与选择。
- Hotel ID 输入。
- JSON payload 编辑器。
- 可切换使用服务端内置 fixture。
- 发送 webhook 到 PMS。
- PMS 响应展示。
- 当前模拟器配置展示。
- E2E readiness 检查、scenario run builder、lifecycle、run steps、verifier 与 E2E logs 展示。
- 内存日志刷新与清空。

## API 清单

### Dashboard / 管理 API

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/api/config` | 查看模拟器运行配置，敏感字段会脱敏。 |
| `GET` | `/api/logs` | 查看内存日志。 |
| `DELETE` | `/api/logs` | 清空内存日志。 |
| `GET` | `/api/webhooks/scenarios` | 查看可发送场景。 |
| `POST` | `/api/webhooks/scenarios/:name/send` | 按场景发送 webhook。 |
| `POST` | `/api/webhooks/reservation/send` | 发送自定义 reservation webhook。 |
| `POST` | `/api/webhooks/messaging/send` | 发送自定义 messaging webhook。 |

### E2E API

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `POST` | `/api/e2e/bootstrap` | 初始化本地测试门店并返回当前页面会话所需的 PMS token、门店、酒店、房型、房间和 readiness。 |
| `GET` | `/api/e2e/readiness` | 检查 PMS 当前门店的 E2E readiness。 |
| `POST` | `/api/e2e/runs` | 启动单次 E2E run，`scenario` 支持 `NEW`、`MULTI_ROOM`、`AIRBNB_NEW`。 |
| `GET` | `/api/e2e/runs/:runId` | 查询 run 状态与 verifier。 |
| `POST` | `/api/e2e/lifecycle` | 执行 new -> modification -> cancellation 生命周期。 |
| `POST` | `/api/e2e/messaging` | 执行 OTA guest message webhook 并验证本地消息入库。 |

兼容入口：

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/scenarios` | 查看可发送场景。 |
| `POST` | `/scenarios/:name/send` | 按场景发送 webhook。 |
| `POST` | `/webhook/send` | 发送自定义 reservation 或 messaging webhook。 |

### Mock Su API

Mock Su API 直接挂载在根路径。PMS 开启统一本地 E2E 开关后默认连接本服务：

```env
CHANNEL_E2E_LOCAL_ENABLED=true
CHANNEL_E2E_TEST_SUPPORT_KEY=local-e2e-key
```

如果模拟器不在默认地址，可用 `SU_API_LOCAL_MOCK_BASE_URL` 覆盖。

常用端点：

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` / `POST` | `/SUAPI/jservice/auth/generate-access-token` | 获取 mock access token。 |
| `POST` | `/SUAPI/jservice/Reservation` | PMS Pull reservation。 |
| `POST` | `/SUAPI/jservice/Reservation_notif` | PMS ack reservation notif。 |
| `POST` | `/SUAPI/jservice/availability` | PMS 推送 ARI availability/rates。 |
| `POST` | `/SUAPI/jservice/OTA_HotelRoom` | PMS 推送 room content。 |
| `POST` | `/SUAPI/jservice/OTA_HotelRatePlan` | PMS 推送 rate plan content。 |
| `POST` | `/SUAPI/jservice/messagingAB` | PMS 调用 messaging 相关能力。 |

## 直接调用示例

列出场景：

```bash
curl http://localhost:4000/api/webhooks/scenarios
```

发送默认 Pull 新预订场景：

```bash
curl -X POST http://localhost:4000/api/webhooks/scenarios/new-booking-pull/send \
  -H 'Content-Type: application/json' \
  -d '{"hotelId":"1"}'
```

发送自定义 payload：

```bash
curl -X POST http://localhost:4000/api/webhooks/scenarios/new-booking-pull/send \
  -H 'Content-Type: application/json' \
  -d '{
    "hotelId": "1",
    "customPayload": {
      "hotelid": "1",
      "reservation_notif": {
        "reservation_notif_id": ["notif-local-001"]
      }
    }
}'
```

检查 E2E readiness：

```bash
curl http://localhost:4000/api/e2e/readiness \
  -H 'Authorization: Bearer <token>' \
  -H 'X-Store-Id: <storeId>'
```

启动默认单房新单 E2E：

```bash
curl -X POST http://localhost:4000/api/e2e/runs \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Bearer <token>' \
  -H 'X-Store-Id: <storeId>' \
  -d '{"mode":"PULL","channel":"BOOKING","scenario":"NEW"}'
```

启动多房新单 E2E：

```bash
curl -X POST http://localhost:4000/api/e2e/runs \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Bearer <token>' \
  -H 'X-Store-Id: <storeId>' \
  -d '{"mode":"PUSH","channel":"BOOKING","scenario":"MULTI_ROOM"}'
```

启动 Airbnb 新单 E2E：

```bash
curl -X POST http://localhost:4000/api/e2e/runs \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Bearer <token>' \
  -H 'X-Store-Id: <storeId>' \
  -d '{"mode":"PULL","channel":"AIRBNB","scenario":"AIRBNB_NEW"}'
```

执行 lifecycle：

```bash
curl -X POST http://localhost:4000/api/e2e/lifecycle \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Bearer <token>' \
  -H 'X-Store-Id: <storeId>' \
  -d '{"mode":"PUSH","channel":"BOOKING"}'
```

执行本地全链路验收脚本：

```bash
PMS_BASE_URL=http://localhost:8092 \
SIMULATOR_BASE_URL=http://localhost:4000 \
CHANNEL_E2E_TEST_SUPPORT_KEY=local-e2e-key \
npm run verify:local-e2e
```

脚本会依次执行：

- PMS `setup-local` 创建/复用本地门店、房型、房间、BOOKING/AIRBNB 渠道与 OTA integration。
- Simulator readiness。
- Booking Pull 新单。
- Booking Push 多房新单。
- Airbnb Push 新单。
- Booking Push/Pull lifecycle：new -> modification -> cancellation。
- Airbnb guest messaging webhook 入库验证。
- PMS 人工回复消息，验证 Mock Su `/SUAPI/jservice/messagingAB` 接收成功。

## 限制说明

- 这是本地测试模拟器，不是 Su 官方沙盒。
- 内存日志和 pending reservation queue 都是进程内状态，重启后清空。
- Dashboard 和 E2E 脚本会调用 PMS test-support API；这些接口默认受 JWT + `X-Store-Id` 保护，`setup-local` 额外要求 `CHANNEL_E2E_LOCAL_ENABLED=true` 和 `X-Test-Support-Key`。
- Dashboard 的编辑器示例请求内容是最小可用示例；如需完整业务字段，优先勾选“使用服务端默认请求内容”使用内置 fixture。
- Pull reservation 只有已登记到 pending queue 的 notif 才会返回 reservation；未命中时返回空 reservations 和诊断信息。
- PMS 仍需单独启动；本地门店、房型、房间、渠道与 OTA integration 可由 `setup-local` 或 `npm run verify:local-e2e` 自动创建/复用。
