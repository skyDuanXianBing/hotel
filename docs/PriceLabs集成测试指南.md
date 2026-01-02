# PriceLabs 集成测试指南

## 目录
1. [测试环境准备](#测试环境准备)
2. [配置测试](#配置测试)
3. [API 客户端测试](#api-客户端测试)
4. [Webhook 测试](#webhook-测试)
5. [签名验证测试](#签名验证测试)
6. [端到端集成测试](#端到端集成测试)
7. [故障排查](#故障排查)

---

## 重要说明

### OTA集成架构变更

从 2025-11-27 起，系统采用独立的 OTA 集成架构：

- **独立数据表**: OTA 直连（Airbnb、Booking.com）使用独立的 `ota_integrations` 表，与普通渠道的 `channels` 表完全分离
- **自动初始化**: 每个门店在首次访问 OTA 集成 API 时，会自动创建 Airbnb 和 Booking.com 两条记录
- **价格调整**: OTA 渠道的价格调整配置存储在 `ota_integrations` 表中，支持 `PERCENTAGE`（百分比）和 `FIXED`（固定金额）两种调整类型
- **API 端点**:
  - 获取 OTA 集成：`GET /api/v1/ota-integrations`
  - 更新价格调整：`PUT /api/v1/ota-integrations/{id}/price-adjustment`

**重要提示**: 本文档已更新以反映新的 OTA 集成架构。所有涉及渠道价格调整的测试用例均使用 `ota_integrations` 表。

---

## 测试环境准备

### 1. 环境变量配置

在 `server/src/main/resources/application.yml` 或 `application.properties` 中添加以下配置：

```yaml
# PriceLabs API 配置
pricelabs:
  api:
    base-url: https://api.pricelabs.co/v1/integration/api
    integration-name: thehosthub
    integration-token: z7FgFEQn42hKzDNza76dpY0hQHz6P6mfEM/0xtT78paBIOmvTmYti/2n3l1K7Ac3

# 服务器基础 URL（用于生成 webhook URLs）
server:
  base-url: http://13.112.235.194:8092
```

**注意**:
- `integration-token` 是 PriceLabs 提供的认证令牌
- `server.base-url` 必须是外网可访问的 URL，PriceLabs 才能调用 webhook

### 2. 数据库准备

确保以下数据表已创建：
- `pricelabs_integration` - 集成配置表
- `pricelabs_connection` - 房型与价格计划连接表
- `channel_price` - 渠道价格表
- `pricelabs_sync_log` - 同步日志表
- `ota_integrations` - OTA直连配置表（自动初始化Airbnb和Booking.com）

**注意**：`ota_integrations` 表会在首次访问时自动为每个门店初始化默认的 Airbnb 和 Booking.com 记录，无需手动运行初始化脚本。

### 3. 启动服务

```bash
cd server
./mvnw spring-boot:run
```

确认服务启动成功，访问 http://localhost:8092/api/v1/health

---

## 配置测试

### 测试用例 1: 获取集成配置

**请求**:
```bash
curl -X GET "http://localhost:8092/api/v1/pricelabs/integration" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "X-Store-Id: 1"
```

**预期响应**:
```json
{
  "success": true,
  "message": "获取集成配置成功",
  "data": {
    "id": 1,
    "storeId": 1,
    "isEnabled": false,
    "syncUrl": "http://13.112.235.194:8092/api/v1/pricelabs/webhook/sync",
    "calendarTriggerUrl": "http://13.112.235.194:8092/api/v1/pricelabs/webhook/calendar-trigger",
    "hookUrl": "http://13.112.235.194:8092/api/v1/pricelabs/webhook/hook",
    "connectedRoomTypeCount": 0,
    "totalSyncCount": 0,
    "successSyncCount": 0
  }
}
```

**验证点**:
- ✅ 响应状态码为 200
- ✅ `success` 为 `true`
- ✅ webhook URLs 格式正确
- ✅ `isEnabled` 默认为 `false`

---

### 测试用例 2: 启用集成

**请求**:
```bash
curl -X PATCH "http://localhost:8092/api/v1/pricelabs/integration/toggle?enabled=true" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "X-Store-Id: 1"
```

**预期响应**:
```json
{
  "success": true,
  "message": "集成状态更新成功",
  "data": {
    "id": 1,
    "storeId": 1,
    "isEnabled": true,
    ...
  }
}
```

**验证点**:
- ✅ 响应状态码为 200
- ✅ `isEnabled` 变为 `true`
- ✅ 数据库中 `pricelabs_integration.is_enabled` 更新为 1

---

### 测试用例 3: 创建房型连接

**前提条件**:
- 门店中至少有一个房型（RoomType）
- 门店中至少有一个价格计划（PricePlan）

**请求**:
```bash
curl -X POST "http://localhost:8092/api/v1/pricelabs/connections" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "X-Store-Id: 1" \
  -H "Content-Type: application/json" \
  -d '{
    "roomTypeId": 1,
    "pricePlanId": 1
  }'
```

**预期响应**:
```json
{
  "success": true,
  "message": "连接创建成功",
  "data": {
    "id": 1,
    "storeId": 1,
    "roomTypeId": 1,
    "roomTypeName": "标准双人间",
    "pricePlanId": 1,
    "pricePlanName": "基础定价",
    "priceLabsListingId": "1_1",
    "isEnabled": true,
    "syncStatus": "pending"
  }
}
```

**验证点**:
- ✅ 响应状态码为 200
- ✅ `priceLabsListingId` 格式为 `{roomTypeId}_{pricePlanId}`
- ✅ `isEnabled` 默认为 `true`
- ✅ 数据库中创建了记录

---

## API 客户端测试

### 测试用例 4: 配置 PriceLabs 集成

**代码测试** (在 Spring Boot 环境中):

```java
@Autowired
private PriceLabsApiClient priceLabsApiClient;

@Test
public void testConfigureIntegration() {
    Map<String, Object> result = priceLabsApiClient.configureIntegration(false);

    // 验证返回结果
    assertNotNull(result);
    assertTrue((Boolean) result.get("success"));

    // 检查返回的 token（如果 regenerate_token=true）
    System.out.println("Integration configured: " + result);
}
```

**预期结果**:
- ✅ PriceLabs API 返回成功响应
- ✅ Webhook URLs 在 PriceLabs 系统中注册成功
- ✅ 如果 `regenerate_token=true`，返回新的 token

**手动测试** (使用 CURL):

```bash
curl -X POST "https://api.pricelabs.co/v1/integration/api/integration" \
  -H "X-INTEGRATION-NAME: thehosthub" \
  -H "X-INTEGRATION-TOKEN: z7FgFEQn42hKzDNza76dpY0hQHz6P6mfEM/0xtT78paBIOmvTmYti/2n3l1K7Ac3" \
  -H "Content-Type: application/json" \
  -d '{
    "sync_url": "http://13.112.235.194:8092/api/v1/pricelabs/webhook/sync",
    "calendar_trigger_url": "http://13.112.235.194:8092/api/v1/pricelabs/webhook/calendar-trigger",
    "hook_url": "http://13.112.235.194:8092/api/v1/pricelabs/webhook/hook",
    "regenerate_token": false,
    "delta_push": true
  }'
```

---

### 测试用例 5: 推送房源到 PriceLabs

**代码测试**:

```java
@Test
public void testPushListings() {
    // 构造房源数据
    Map<String, Object> listingData = new HashMap<>();
    List<Map<String, Object>> listings = new ArrayList<>();

    Map<String, Object> listing = new HashMap<>();
    listing.put("id", "1_1");  // listing_id = roomTypeId_pricePlanId
    listing.put("name", "标准双人间 - 基础定价");
    listing.put("active", true);
    listing.put("currency", "CNY");
    listing.put("timezone", "Asia/Shanghai");

    listings.add(listing);
    listingData.put("listings", listings);

    // 推送到 PriceLabs
    Map<String, Object> result = priceLabsApiClient.pushListings(listingData);

    // 验证
    assertNotNull(result);
    System.out.println("Push listings result: " + result);
}
```

**预期结果**:
- ✅ PriceLabs 接收并存储房源信息
- ✅ 返回成功状态

---

### 测试用例 6: 推送价格计划到 PriceLabs

**代码测试**:

```java
@Test
public void testPushRatePlans() {
    Map<String, Object> ratePlanData = new HashMap<>();
    List<Map<String, Object>> ratePlans = new ArrayList<>();

    Map<String, Object> ratePlan = new HashMap<>();
    ratePlan.put("id", "rp_1");
    ratePlan.put("listing_id", "1_1");
    ratePlan.put("name", "基础定价");
    ratePlan.put("active", true);

    ratePlans.add(ratePlan);
    ratePlanData.put("rate_plans", ratePlans);

    Map<String, Object> result = priceLabsApiClient.pushRatePlans(ratePlanData);

    assertNotNull(result);
    System.out.println("Push rate plans result: " + result);
}
```

---

## Webhook 测试

### 测试用例 7: 接收价格更新 Webhook

**模拟 PriceLabs 推送价格更新**:

```bash
# 1. 生成签名
# 使用 Python 脚本生成签名（见下方签名生成脚本）

# 2. 发送 webhook 请求
curl -X POST "http://13.112.235.194:8092/api/v1/pricelabs/webhook/sync" \
  -H "Content-Type: application/json" \
  -H "X-Signature: BASE64_ENCODED_SIGNATURE" \
  -d '{
    "type": "price_update",
    "timestamp": "2025-11-27T12:00:00Z",
    "data": [
      {
        "listingId": "1_1",
        "ratePlanId": "rp_1",
        "calendar": [
          {
            "date": "2025-12-01",
            "price": 500.00,
            "minStay": 1,
            "maxStay": 30,
            "closedToArrival": false,
            "closedToDeparture": false
          },
          {
            "date": "2025-12-02",
            "price": 550.00,
            "minStay": 1,
            "maxStay": 30,
            "closedToArrival": false,
            "closedToDeparture": false
          }
        ]
      }
    ]
  }'
```

**预期响应**:
```json
{
  "success": true,
  "message": "价格更新已接收",
  "processed_count": 1
}
```

**验证点**:
- ✅ 响应状态码为 200
- ✅ `success` 为 `true`
- ✅ `processed_count` 等于发送的房源数量
- ✅ 数据库中 `channel_price` 表新增了价格记录
- ✅ `pricelabs_sync_log` 表记录了同步日志
- ✅ `pricelabs_connection.last_sync_at` 更新为当前时间
- ✅ `pricelabs_integration.last_price_sync_at` 更新为当前时间

**数据库验证**:
```sql
-- 检查价格是否保存成功
SELECT * FROM channel_price
WHERE room_type_id = 1
  AND price_date IN ('2025-12-01', '2025-12-02')
ORDER BY price_date, channel_id;

-- 检查同步日志
SELECT * FROM pricelabs_sync_log
WHERE store_id = 1
ORDER BY created_at DESC
LIMIT 5;

-- 检查OTA渠道价格计算是否正确
-- 假设 Airbnb 价格上调 15%
-- 基础价 500，渠道价应为 500 * (1 + 0.15) = 575.00
SELECT
  ota.name AS channel_name,
  cp.price_date,
  cp.base_price,
  cp.channel_price,
  ota.price_adjustment_type,
  ota.price_adjustment_value
FROM channel_price cp
JOIN ota_integrations ota ON cp.channel_id = ota.id
WHERE cp.room_type_id = 1
  AND cp.price_date = '2025-12-01';
```

---

### 测试用例 8: 接收日历刷新请求

**请求**:
```bash
curl -X POST "http://13.112.235.194:8092/api/v1/pricelabs/webhook/calendar-trigger" \
  -H "Content-Type: application/json" \
  -H "X-Signature: BASE64_ENCODED_SIGNATURE" \
  -d '{
    "type": "calendar_request",
    "timestamp": "2025-11-27T12:00:00Z",
    "listing_ids": ["1_1", "1_2"]
  }'
```

**预期响应**:
```json
{
  "success": true,
  "message": "日历刷新请求已接收"
}
```

**验证点**:
- ✅ 响应状态码为 200
- ✅ 控制台输出日志信息
- ✅ TODO: 未来应触发日历数据同步到 PriceLabs

---

### 测试用例 9: 接收错误通知

**请求**:
```bash
curl -X POST "http://13.112.235.194:8092/api/v1/pricelabs/webhook/hook" \
  -H "Content-Type: application/json" \
  -H "X-Signature: BASE64_ENCODED_SIGNATURE" \
  -d '{
    "type": "error",
    "timestamp": "2025-11-27T12:00:00Z",
    "error_type": "sync_failed",
    "message": "无法同步房源 1_1",
    "details": {
      "listing_id": "1_1",
      "reason": "Invalid data format"
    }
  }'
```

**预期响应**:
```json
{
  "success": true,
  "message": "错误通知已接收"
}
```

**验证点**:
- ✅ 响应状态码为 200
- ✅ 控制台错误日志输出错误信息
- ✅ TODO: 未来应记录到错误日志表

---

## 签名验证测试

### 测试用例 10: 验证正确的签名

**Python 签名生成脚本**:
```python
import hmac
import hashlib
import base64
import json

# PriceLabs Integration Token
token = "z7FgFEQn42hKzDNza76dpY0hQHz6P6mfEM/0xtT78paBIOmvTmYti/2n3l1K7Ac3"

# Webhook 请求体
request_body = {
    "type": "price_update",
    "timestamp": "2025-11-27T12:00:00Z",
    "data": [
        {
            "listingId": "1_1",
            "ratePlanId": "rp_1",
            "calendar": [
                {
                    "date": "2025-12-01",
                    "price": 500.00,
                    "minStay": 1,
                    "maxStay": 30
                }
            ]
        }
    ]
}

# 转换为 JSON 字符串（无空格）
body_str = json.dumps(request_body, separators=(',', ':'))

# 生成 HMAC SHA256 签名
signature = hmac.new(
    token.encode('utf-8'),
    body_str.encode('utf-8'),
    hashlib.sha256
).digest()

# Base64 编码
signature_b64 = base64.b64encode(signature).decode('utf-8')

print("Request Body:")
print(body_str)
print("\nSignature:")
print(signature_b64)
```

**测试步骤**:
1. 运行 Python 脚本生成签名
2. 使用生成的签名发送 webhook 请求
3. 服务器应接受请求并处理

**预期结果**:
- ✅ 签名验证通过
- ✅ Webhook 数据被成功处理
- ✅ 返回 200 状态码

---

### 测试用例 11: 验证错误的签名

**请求**:
```bash
curl -X POST "http://13.112.235.194:8092/api/v1/pricelabs/webhook/sync" \
  -H "Content-Type: application/json" \
  -H "X-Signature: INVALID_SIGNATURE_HERE" \
  -d '{
    "type": "price_update",
    "timestamp": "2025-11-27T12:00:00Z",
    "data": []
  }'
```

**预期响应**:
```json
{
  "success": false,
  "message": "签名验证失败"
}
```

**验证点**:
- ✅ 响应状态码为 401 (Unauthorized)
- ✅ 请求被拒绝
- ✅ 数据未被处理

---

### 测试用例 12: 无签名头的请求

**请求**:
```bash
curl -X POST "http://13.112.235.194:8092/api/v1/pricelabs/webhook/sync" \
  -H "Content-Type: application/json" \
  -d '{
    "type": "price_update",
    "timestamp": "2025-11-27T12:00:00Z",
    "data": []
  }'
```

**预期行为**:
- 当前实现：如果 `X-Signature` 为 null，会跳过签名验证（开发环境）
- 生产环境：应该要求必须提供签名

**建议**: 生产环境中设置 `required = true`:
```java
@RequestHeader(value = "X-Signature", required = true) String signature
```

---

## 端到端集成测试

### 测试用例 13: 完整价格同步流程

**测试流程**:

#### 步骤 1: 启用集成
```bash
curl -X PATCH "http://localhost:8092/api/v1/pricelabs/integration/toggle?enabled=true" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "X-Store-Id: 1"
```

#### 步骤 2: 创建房型连接
```bash
curl -X POST "http://localhost:8092/api/v1/pricelabs/connections" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "X-Store-Id: 1" \
  -H "Content-Type: application/json" \
  -d '{
    "roomTypeId": 1,
    "pricePlanId": 1
  }'
```

#### 步骤 3: 配置OTA价格调整

**注意**：首先需要获取 OTA 集成的 ID
```bash
# 获取所有OTA集成配置
curl -X GET "http://localhost:8092/api/v1/ota-integrations" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "X-Store-Id: 1"

# 响应示例：
# {
#   "success": true,
#   "data": [
#     {"id": 1, "name": "Airbnb", "code": "AIRBNB", ...},
#     {"id": 2, "name": "Booking.com", "code": "BOOKING", ...}
#   ]
# }
```

使用获取的ID配置价格调整：

```bash
# 设置 Airbnb 价格上调 15%
curl -X PUT "http://localhost:8092/api/v1/ota-integrations/1/price-adjustment" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "X-Store-Id: 1" \
  -H "Content-Type: application/json" \
  -d '{
    "priceAdjustmentType": "PERCENTAGE",
    "priceAdjustmentValue": 15.0,
    "autoSyncPrice": true
  }'

# 设置 Booking.com 价格上调 17%
curl -X PUT "http://localhost:8092/api/v1/ota-integrations/2/price-adjustment" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "X-Store-Id: 1" \
  -H "Content-Type: application/json" \
  -d '{
    "priceAdjustmentType": "PERCENTAGE",
    "priceAdjustmentValue": 17.0,
    "autoSyncPrice": true
  }'
```

#### 步骤 4: 配置 PriceLabs 集成（推送 Webhook URLs）
```bash
curl -X POST "https://api.pricelabs.co/v1/integration/api/integration" \
  -H "X-INTEGRATION-NAME: thehosthub" \
  -H "X-INTEGRATION-TOKEN: z7FgFEQn42hKzDNza76dpY0hQHz6P6mfEM/0xtT78paBIOmvTmYti/2n3l1K7Ac3" \
  -H "Content-Type: application/json" \
  -d '{
    "sync_url": "http://13.112.235.194:8092/api/v1/pricelabs/webhook/sync",
    "calendar_trigger_url": "http://13.112.235.194:8092/api/v1/pricelabs/webhook/calendar-trigger",
    "hook_url": "http://13.112.235.194:8092/api/v1/pricelabs/webhook/hook",
    "regenerate_token": false,
    "delta_push": true
  }'
```

#### 步骤 5: 推送房源到 PriceLabs
```bash
curl -X POST "https://api.pricelabs.co/v1/integration/api/listings" \
  -H "X-INTEGRATION-NAME: thehosthub" \
  -H "X-INTEGRATION-TOKEN: z7FgFEQn42hKzDNza76dpY0hQHz6P6mfEM/0xtT78paBIOmvTmYti/2n3l1K7Ac3" \
  -H "Content-Type: application/json" \
  -d '{
    "listings": [
      {
        "id": "1_1",
        "name": "标准双人间 - 基础定价",
        "active": true,
        "currency": "CNY",
        "timezone": "Asia/Shanghai"
      }
    ]
  }'
```

#### 步骤 6: 等待 PriceLabs 推送价格更新
- PriceLabs 根据其定价算法计算价格
- 通过 webhook 推送价格到系统

#### 步骤 7: 模拟接收价格更新（如果 PriceLabs 尚未主动推送）
```bash
# 使用 Python 脚本生成签名后执行
curl -X POST "http://13.112.235.194:8092/api/v1/pricelabs/webhook/sync" \
  -H "Content-Type: application/json" \
  -H "X-Signature: GENERATED_SIGNATURE" \
  -d '{
    "type": "price_update",
    "timestamp": "2025-11-27T12:00:00Z",
    "data": [
      {
        "listingId": "1_1",
        "ratePlanId": "rp_1",
        "calendar": [
          {
            "date": "2025-12-01",
            "price": 500.00,
            "minStay": 1,
            "maxStay": 30,
            "closedToArrival": false,
            "closedToDeparture": false
          }
        ]
      }
    ]
  }'
```

#### 步骤 8: 查询渠道价格
```bash
curl -X GET "http://localhost:8092/api/v1/pricelabs/channel-prices?roomTypeId=1&startDate=2025-12-01&endDate=2025-12-31" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "X-Store-Id: 1"
```

**预期结果**:
```json
{
  "success": true,
  "message": "获取渠道价格成功",
  "data": [
    {
      "id": 1,
      "roomTypeId": 1,
      "roomTypeName": "标准双人间",
      "channelId": 1,
      "channelName": "Airbnb",
      "channelCode": "AIRBNB",
      "priceDate": "2025-12-01",
      "basePrice": 500.00,
      "channelPrice": 575.00,  // 500 * (1 + 0.15)
      "minStay": 1,
      "maxStay": 30,
      "isSyncedToOta": false,
      "priceLabsUpdatedAt": "2025-11-27T12:00:00"
    },
    {
      "id": 2,
      "roomTypeId": 1,
      "roomTypeName": "标准双人间",
      "channelId": 2,
      "channelName": "Booking.com",
      "channelCode": "BOOKING",
      "priceDate": "2025-12-01",
      "basePrice": 500.00,
      "channelPrice": 585.00,  // 500 * (1 + 0.17)
      "minStay": 1,
      "maxStay": 30,
      "isSyncedToOta": false,
      "priceLabsUpdatedAt": "2025-11-27T12:00:00"
    }
  ]
}
```

**验证点**:
- ✅ 价格已保存到数据库
- ✅ 每个OTA渠道的价格根据调整规则正确计算
- ✅ Airbnb 渠道价 = 500 * (1 + 0.15) = 575.00
- ✅ Booking.com 渠道价 = 500 * (1 + 0.17) = 585.00
- ✅ 最小/最大入住天数正确保存
- ✅ `isSyncedToOta` 为 `false`（需要推送到 OTA）
- ✅ `priceLabsUpdatedAt` 记录了更新时间

---

## 故障排查

### 问题 1: Webhook 签名验证失败

**症状**:
- 返回 401 状态码
- 错误消息: "签名验证失败"

**解决方案**:
1. 检查 `integration-token` 是否正确配置
2. 确认请求体与签名计算时使用的内容完全一致（包括空格、换行）
3. 使用 Python 脚本重新生成签名
4. 开发环境可临时移除签名验证进行测试

---

### 问题 2: 价格未保存到数据库

**症状**:
- Webhook 返回成功，但数据库中没有价格记录

**排查步骤**:
1. 检查 `pricelabs_connection` 表中是否存在对应的 `listing_id` 记录
```sql
SELECT * FROM pricelabs_connection
WHERE pricelabs_listing_id = '1_1';
```

2. 检查OTA集成是否启用且开启了自动同步
```sql
SELECT id, name, code, enabled, auto_sync_price
FROM ota_integrations
WHERE store_id = 1;
```

3. 查看同步日志中的错误信息
```sql
SELECT * FROM pricelabs_sync_log
WHERE status = 'FAILED'
ORDER BY created_at DESC
LIMIT 10;
```

4. 检查服务器日志中的异常信息

---

### 问题 3: 渠道价格计算不正确

**症状**:
- 渠道价格与预期不符

**排查步骤**:
1. 检查OTA集成的价格调整设置
```sql
SELECT id, name, code, price_adjustment_type, price_adjustment_value
FROM ota_integrations
WHERE store_id = 1;
```

2. 验证计算公式:
   - **PERCENTAGE** (百分比模式): `channelPrice = basePrice * (1 + adjustmentValue / 100)`
   - **FIXED** (固定金额): `channelPrice = basePrice + adjustmentValue`

3. 手动计算验证

---

### 问题 4: PriceLabs API 调用失败

**症状**:
- 推送房源/价格计划失败
- 错误: "Unauthorized" 或 "Invalid token"

**解决方案**:
1. 确认 `integration-name` 和 `integration-token` 正确
2. 检查请求头是否包含:
   - `X-INTEGRATION-NAME: thehosthub`
   - `X-INTEGRATION-TOKEN: [your-token]`
3. 确认 PriceLabs API 端点 URL 正确
4. 联系 PriceLabs 支持确认 token 是否有效

---

### 问题 5: Webhook URL 无法访问

**症状**:
- PriceLabs 推送失败
- PriceLabs 显示 "Webhook URL unreachable"

**解决方案**:
1. 确认 `server.base-url` 配置的是外网可访问的 URL
2. 检查服务器防火墙是否允许外部访问 8092 端口
3. 使用外部工具测试 webhook URL 的可达性:
```bash
curl -X POST "http://13.112.235.194:8092/api/v1/pricelabs/webhook/sync" \
  -H "Content-Type: application/json" \
  -d '{"type":"test"}'
```
4. 考虑使用 ngrok 等工具进行本地测试

---

## 测试总结

### 测试清单

#### 配置测试
- [ ] 获取集成配置
- [ ] 启用/禁用集成
- [ ] 创建房型连接
- [ ] 更新渠道价格调整

#### API 客户端测试
- [ ] 配置 PriceLabs 集成
- [ ] 推送房源
- [ ] 推送价格计划
- [ ] 推送日历数据
- [ ] 推送预订

#### Webhook 测试
- [ ] 接收价格更新
- [ ] 接收日历刷新请求
- [ ] 接收错误通知

#### 签名验证测试
- [ ] 验证正确签名
- [ ] 拒绝错误签名
- [ ] 处理无签名请求

#### 端到端测试
- [ ] 完整价格同步流程
- [ ] 多渠道价格计算
- [ ] 同步日志记录

---

## 附录

### A. 测试数据准备脚本

```sql
-- 创建测试门店
INSERT INTO store (name, created_at, updated_at)
VALUES ('测试酒店', NOW(), NOW());

-- 创建测试用户
INSERT INTO user (username, password, email, created_at, updated_at)
VALUES ('test', '$2a$10$encrypted_password', 'test@example.com', NOW(), NOW());

-- 关联用户和门店
INSERT INTO store_user (store_id, user_id, role, created_at, updated_at)
VALUES (1, 1, 'owner', NOW(), NOW());

-- 创建房型
INSERT INTO room_type (store_id, name, created_at, updated_at)
VALUES (1, '标准双人间', NOW(), NOW());

-- 创建价格计划
INSERT INTO price_plan (store_id, name, created_at, updated_at)
VALUES (1, '基础定价', NOW(), NOW());

-- OTA集成数据会在首次访问 /api/v1/ota-integrations 时自动初始化
-- 无需手动插入 Airbnb 和 Booking.com 记录
```

### B. Python 签名验证工具

保存为 `generate_signature.py`:

```python
#!/usr/bin/env python3
import hmac
import hashlib
import base64
import json
import sys

def generate_signature(token, request_body):
    """生成 HMAC SHA256 签名"""
    # 如果 request_body 是字典，转换为 JSON 字符串
    if isinstance(request_body, dict):
        body_str = json.dumps(request_body, separators=(',', ':'))
    else:
        body_str = request_body

    # 生成签名
    signature = hmac.new(
        token.encode('utf-8'),
        body_str.encode('utf-8'),
        hashlib.sha256
    ).digest()

    # Base64 编码
    return base64.b64encode(signature).decode('utf-8')

if __name__ == '__main__':
    token = "z7FgFEQn42hKzDNza76dpY0hQHz6P6mfEM/0xtT78paBIOmvTmYti/2n3l1K7Ac3"

    # 示例请求体
    request_body = {
        "type": "price_update",
        "timestamp": "2025-11-27T12:00:00Z",
        "data": [
            {
                "listingId": "1_1",
                "ratePlanId": "rp_1",
                "calendar": [
                    {
                        "date": "2025-12-01",
                        "price": 500.00,
                        "minStay": 1,
                        "maxStay": 30,
                        "closedToArrival": False,
                        "closedToDeparture": False
                    }
                ]
            }
        ]
    }

    signature = generate_signature(token, request_body)

    print("Request Body:")
    print(json.dumps(request_body, indent=2))
    print("\nSignature:")
    print(signature)
```

---

**文档版本**: 1.1
**最后更新**: 2025-11-27
**作者**: Claude Code
**更新说明**:
- v1.1 (2025-11-27): 更新为独立 OTA 集成架构，使用 `ota_integrations` 表替代 `channels` 表
- v1.0 (2025-11-27): 初始版本
