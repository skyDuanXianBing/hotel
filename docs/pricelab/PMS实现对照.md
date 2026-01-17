# PriceLabs Connector（PMS 实现对照）

本文用于把 PriceLabs SwaggerHub 文档中的 **PMS -> PriceLabs** 接口与本 PMS 的实现对应起来，便于填写认证清单与自测。

## 1) get_prices（pull_sync）

- PriceLabs 接口：`POST /get_prices`（SwaggerHub `pull_sync`）
- 本 PMS 调用点：
  - 出站客户端：`server/src/main/java/server/demo/service/PriceLabsApiClient.java` 的 `pullSyncGetPrices`
  - “定价工具-立即同步”入口：`POST /api/v1/pricelabs/sync/manual`
  - 实际拉价落库：`server/src/main/java/server/demo/service/PriceLabsSyncService.java` 的 `pullPricesForNextDaysPullSync(365)`
- 策略（稳定优先）：
  - `delta_only` 固定为 `false`（全量更稳）
  - 按“启用的 PriceLabsConnection”逐个 `listing_id` 调用
  - 将返回的 `data[]` 转换为 webhook 统一落库逻辑（房价、渠道价、改价记录）
  - `check_in / check_out` 映射为 PMS 内部字段：`closedToArrival = !check_in`，`closedToDeparture = !check_out`

## 2) reservations

- PriceLabs 接口：`POST /reservations`
- 本 PMS 调用点：
  - 出站客户端：`server/src/main/java/server/demo/service/PriceLabsApiClient.java` 的 `pushReservations`
  - 手动推送入口：`POST /api/v1/pricelabs/reservations/push`
    - `startDate/endDate` 可选；默认 `2020-01-01 ~ today+365`
  - 自动推送触发：
    - `server/src/main/java/server/demo/service/ReservationService.java` 在创建/修改/入住/退房/取消后 **事务提交后** 调用 `PriceLabsReservationSyncService.pushReservationById`
- 约束：
  - 仅推送“房型存在启用的 PriceLabsConnection”的 reservations（避免推送到未连接 listing）
  - `reservation_id` 使用 `Reservation.orderNumber`（系统唯一）

## 3) status

- PriceLabs 接口：`POST /status`
- 本 PMS 调用点：
  - 出站客户端：`server/src/main/java/server/demo/service/PriceLabsApiClient.java` 的 `queryStatus`
  - 手动查询入口：`POST /api/v1/pricelabs/status/query`（Body：`{ "statuses": [ { "id": "...", "type": "listing" } ] }`）

