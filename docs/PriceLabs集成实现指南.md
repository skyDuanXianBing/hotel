# PriceLabs 动态定价 API 集成实现指南

> **基于当前项目架构设计，贴合现有实体结构和多门店架构**

## 重要架构说明

### OTA 集成架构变更 (2025-11-27)

**本文档部分内容已过时**。系统已采用独立的 OTA 集成架构:

- **OTA 直连渠道** (Airbnb, Booking.com): 使用独立的 `ota_integrations` 表
  - 价格调整类型: `PERCENTAGE` (百分比) 和 `FIXED` (固定金额)
  - API 端点: `/api/v1/ota-integrations`
  - 详见 [PriceLabs集成测试指南.md](./PriceLabs集成测试指南.md)

- **其他渠道** (携程、美团等): 可能仍使用 `channels` 表 (待实现)

**注意**: 本文档中关于 `Channel` 表和 `COMMISSION` 价格调整类型的内容仅供参考,实际 OTA 集成请参考最新的测试指南文档。

---

## 目录

1. [概述](#1-概述)
2. [认证配置](#2-认证配置)
3. [工作流程架构](#3-工作流程架构)
4. [API 端点详解](#4-api-端点详解)
5. [Webhook 实现](#5-webhook-实现)
6. [数据模型定义](#6-数据模型定义)
7. [贴合项目的实现方案](#7-贴合项目的实现方案)
8. [错误处理](#8-错误处理)
9. [安全考虑](#9-安全考虑)
10. [测试指南](#10-测试指南)

---

## 1. 概述

### 1.1 集成目标

本文档描述如何将 PMS（酒店预订管理系统）与 PriceLabs 动态定价服务集成，实现以下功能：

1. **房源导入** - 将 PMS 中的房型/房间数据推送到 PriceLabs
2. **动态价格接收** - 从 PriceLabs 获取智能定价建议
3. **价格同步** - 将更新后的价格推送至关联的 OTA 平台
4. **数据反馈** - 将预订及房态数据同步回 PriceLabs 用于分析优化

### 1.2 API 基本信息

| 项目 | 值 |
|------|-----|
| **Base URL** | `https://api.pricelabs.co/v1/integration/api` |
| **协议** | HTTPS |
| **数据格式** | JSON |
| **速率限制** | 300 请求/分钟 |
| **API 版本** | 1.0.0 |

### 1.3 集成凭证

```
集成名称 (Integration Name): thehosthub
集成令牌 (Integration Token): z7FgFEQn42hKzDNza76dpY0hQHz6P6mfEM/0xtT78paBIOmvTmYti/2n3l1K7Ac3
```

> ⚠️ **重要提示**: 令牌应存储在环境变量中，禁止硬编码到代码中。

---

## 2. 认证配置

### 2.1 请求头认证

所有 API 请求必须包含以下请求头：

```http
X-INTEGRATION-NAME: thehosthub
X-INTEGRATION-TOKEN: <your_token>
Content-Type: application/json
```

### 2.2 环境变量配置

**后端 (application.yml)**:
```yaml
pricelabs:
  api:
    base-url: https://api.pricelabs.co/v1/integration/api
    integration-name: ${PRICELABS_INTEGRATION_NAME:thehosthub}
    integration-token: ${PRICELABS_INTEGRATION_TOKEN}
```

**前端 (.env)**:
```bash
# PriceLabs 配置（如需前端直接调用）
VITE_PRICELABS_API_URL=https://api.pricelabs.co/v1/integration/api
```

### 2.3 响应签名验证

PriceLabs 的 Webhook 回调会包含签名头，用于验证请求来源：

```http
X-Signature: <sha256_hmac_signature>
```

验证方式：使用集成令牌对请求体进行 SHA256 HMAC 签名，与 `X-Signature` 比对。

---

## 3. 工作流程架构

### 3.1 整体数据流

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                              完整集成工作流                                       │
└─────────────────────────────────────────────────────────────────────────────────┘

                           ② 动态价格推送 (sync_url Webhook)
┌─────────┐  ① 房源导入   ┌────────────┐                      ┌─────────────────┐
│         │ ───────────> │            │                      │   OTA 平台       │
│   PMS   │              │ PriceLabs  │                      │ ┌─────────────┐ │
│  系统   │ <─────────── │   服务器    │                      │ │ Booking.com │ │
│         │              │            │                      │ │ Airbnb      │ │
└────┬────┘              └────────────┘                      │ │ 携程        │ │
     │                         ↑                             │ │ 美团        │ │
     │  ③ 渠道价格计算          │                             │ └─────────────┘ │
     │  (基础价格 + 价格调整)   │                             └────────┬────────┘
     │                         │                                      │
     ├─────────────────────────┼──────────────────────────────────────┤
     │                         │                                      │
     │  ④ 推送到各 OTA         │  ⑤ 预订/入住率反馈                    │
     └─────────────────────────┴──────────────────────────────────────┘
```

### 3.2 详细步骤说明

| 步骤 | 方向 | 说明 | 相关代码 |
|------|------|------|----------|
| ① 房源导入 | PMS → PriceLabs | 将 `RoomType` 推送到 PriceLabs `/listings` | `RoomType` 实体 |
| ② 动态价格推送 | PriceLabs → PMS | PriceLabs 通过 `sync_url` Webhook 推送价格 | 新增 `PriceLabsWebhookController` |
| ③ 渠道价格计算 | PMS 内部 | 基于 `Channel.commissionRate` 计算各渠道售价 | `Channel` 实体 |
| ④ OTA 同步 | PMS → OTA | 将调整后的价格推送到各 OTA 平台 API | 新增 `OtaSyncService` |
| ⑤ 数据反馈 | PMS → PriceLabs | 同步 `Reservation` 预订数据供 PriceLabs 分析 | `Reservation` 实体 |

### 3.3 渠道价格计算逻辑

PriceLabs 推送的是**基础定价**，需要根据各渠道的价格调整配置进行调整后再推送到 OTA。

**OTA 直连渠道** (使用 `ota_integrations` 表):

```
计算公式:
- PERCENTAGE 模式: 渠道售价 = 基础价格 × (1 + 调整值 / 100)
- FIXED 模式: 渠道售价 = 基础价格 + 调整值

示例 (PERCENTAGE 模式):
- PriceLabs 推送价格: ¥500
- Airbnb 价格上调 15%    → 售价 = 500 × (1 + 0.15) = ¥575
- Booking.com 价格上调 17% → 售价 = 500 × (1 + 0.17) = ¥585

示例 (FIXED 模式):
- PriceLabs 推送价格: ¥500
- Airbnb 固定加价 ¥50   → 售价 = 500 + 50 = ¥550
```

> **注意**: OTA 直连 (Airbnb, Booking.com) 使用独立的 `ota_integrations` 表管理价格调整配置,详见 [PriceLabs集成测试指南.md](./PriceLabs集成测试指南.md)。其他渠道的实现待定。

### 3.4 Webhook URL 配置

必须通过 `/integration` 端点配置三个回调 URL：

| URL 类型 | 用途 | 示例 |
|----------|------|------|
| `sync_url` | 接收价格/限制更新 | `https://your-pms.com/api/v1/pricelabs/sync` |
| `calendar_trigger_url` | 触发日历刷新请求 | `https://your-pms.com/api/v1/pricelabs/calendar-trigger` |
| `hook_url` | 接收错误通知 | `https://your-pms.com/api/v1/pricelabs/hook` |

---

## 4. API 端点详解

### 4.1 配置集成 - POST /integration

**用途**: 配置集成设置，包括 Webhook URL 和功能开关

**请求体**:
```json
{
  "sync_url": "https://your-pms.com/api/v1/pricelabs/sync",
  "calendar_trigger_url": "https://your-pms.com/api/v1/pricelabs/calendar-trigger",
  "hook_url": "https://your-pms.com/api/v1/pricelabs/hook",
  "regenerate_token": false,
  "delta_push": true
}
```

**参数说明**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `sync_url` | string | 是 | 接收价格更新的 URL |
| `calendar_trigger_url` | string | 是 | 接收日历刷新请求的 URL |
| `hook_url` | string | 是 | 接收错误通知的 URL |
| `regenerate_token` | boolean | 否 | 是否重新生成令牌（默认 false） |
| `delta_push` | boolean | 否 | 是否启用增量推送（默认 true） |

**响应体**:
```json
{
  "success": true,
  "message": "Integration settings updated successfully",
  "data": {
    "sync_url": "https://your-pms.com/api/v1/pricelabs/sync",
    "calendar_trigger_url": "https://your-pms.com/api/v1/pricelabs/calendar-trigger",
    "hook_url": "https://your-pms.com/api/v1/pricelabs/hook",
    "new_token": null
  }
}
```

> ⚠️ **注意**: 若 `regenerate_token=true`，响应中 `new_token` 将返回新令牌，后续所有 API 调用必须使用新令牌。

---

### 4.2 推送房源 - POST /listings

**用途**: 创建或更新房源（房型）信息

**请求体**:
```json
{
  "listings": [
    {
      "listing_id": "room_type_101",
      "name": "豪华大床房",
      "address": "北京市朝阳区XXX路XX号",
      "city": "北京",
      "country": "CN",
      "latitude": 39.9042,
      "longitude": 116.4074,
      "bedrooms": 1,
      "bathrooms": 1,
      "accommodates": 2,
      "property_type": "hotel_room",
      "currency": "CNY",
      "base_price": 500,
      "min_price": 300,
      "max_price": 1200,
      "active": true,
      "multi_unit": true,
      "multi_unit_count": 5
    }
  ]
}
```

**参数说明**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `listing_id` | string | 是 | 唯一标识，仅允许字母数字和下划线 |
| `name` | string | 是 | 房源名称 |
| `address` | string | 是 | 完整地址 |
| `city` | string | 是 | 城市 |
| `country` | string | 是 | 国家代码（ISO 3166-1 alpha-2） |
| `latitude` | number | 是 | 纬度 |
| `longitude` | number | 是 | 经度 |
| `bedrooms` | integer | 是 | 卧室数量 |
| `bathrooms` | number | 是 | 浴室数量 |
| `accommodates` | integer | 是 | 最大入住人数 |
| `property_type` | string | 是 | 房产类型 |
| `currency` | string | 是 | 货币代码（ISO 4217） |
| `base_price` | number | 是 | 基础价格 |
| `min_price` | number | 否 | 最低价格限制 |
| `max_price` | number | 否 | 最高价格限制 |
| `active` | boolean | 否 | 是否激活（默认 true） |
| `multi_unit` | boolean | 否 | 是否多单元房型 |
| `multi_unit_count` | integer | 否 | 单元数量（房间数） |

**响应体**:
```json
{
  "success": ["room_type_101"],
  "failure": []
}
```

---

### 4.3 推送价格计划 - POST /rate_plans

**用途**: 创建或更新房源的价格计划

**请求体**:
```json
{
  "rate_plans": [
    {
      "listing_id": "room_type_101",
      "rate_plan_id": "standard",
      "name": "标准价格",
      "is_default": true,
      "occupancy_based": false
    },
    {
      "listing_id": "room_type_101",
      "rate_plan_id": "weekend",
      "name": "周末特惠",
      "is_default": false,
      "occupancy_based": false
    }
  ]
}
```

**参数说明**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `listing_id` | string | 是 | 关联的房源ID |
| `rate_plan_id` | string | 是 | 价格计划唯一标识 |
| `name` | string | 是 | 价格计划名称 |
| `is_default` | boolean | 是 | 是否默认计划（每个房源必须有且仅有一个） |
| `occupancy_based` | boolean | 否 | 是否基于入住人数定价 |

**响应体**:
```json
{
  "success": [
    {"listing_id": "room_type_101", "rate_plan_id": "standard"},
    {"listing_id": "room_type_101", "rate_plan_id": "weekend"}
  ],
  "failure": []
}
```

---

### 4.4 更新日历 - POST /calendar

**用途**: 更新房源的价格和可用性

**请求体**:
```json
{
  "calendars": [
    {
      "listing_id": "room_type_101",
      "rate_plan_id": "standard",
      "calendar": [
        {
          "date": "2025-01-15",
          "price": 580,
          "available": true,
          "min_stay": 1,
          "available_units": 3
        },
        {
          "date": "2025-01-16",
          "price": 620,
          "available": true,
          "min_stay": 2,
          "available_units": 5
        }
      ]
    }
  ]
}
```

**参数说明**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `listing_id` | string | 是 | 房源ID |
| `rate_plan_id` | string | 否 | 价格计划ID（默认为 default） |
| `calendar` | array | 是 | 日历数据数组 |
| `calendar[].date` | string | 是 | 日期（YYYY-MM-DD） |
| `calendar[].price` | number | 是 | 当日价格 |
| `calendar[].available` | boolean | 是 | 是否可用 |
| `calendar[].min_stay` | integer | 否 | 最小入住天数 |
| `calendar[].available_units` | integer | 否 | 可用单元数（多单元房型） |

**响应体**:
```json
{
  "success": ["room_type_101"],
  "failure": []
}
```

---

### 4.5 获取价格 - POST /get_prices

**用途**: 从 PriceLabs 获取动态定价建议

**请求体**:
```json
{
  "listing_ids": ["room_type_101", "room_type_102"],
  "start_date": "2025-01-15",
  "end_date": "2025-03-15",
  "delta_only": false
}
```

**参数说明**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `listing_ids` | array | 是 | 房源ID列表 |
| `start_date` | string | 否 | 开始日期（默认今天） |
| `end_date` | string | 否 | 结束日期（默认365天后） |
| `delta_only` | boolean | 否 | 仅返回变更的数据 |

**响应体**:
```json
{
  "success": true,
  "data": [
    {
      "listing_id": "room_type_101",
      "rate_plan_id": "standard",
      "currency": "CNY",
      "calendar": [
        {
          "date": "2025-01-15",
          "price": 650,
          "min_stay": 1,
          "max_stay": 30,
          "closed_to_arrival": false,
          "closed_to_departure": false
        }
      ]
    }
  ],
  "failure": []
}
```

---

### 4.6 推送预订 - POST /reservations

**用途**: 同步预订数据到 PriceLabs

**请求体**:
```json
{
  "reservations": [
    {
      "reservation_id": "RES_20250115_001",
      "listing_id": "room_type_101",
      "rate_plan_id": "standard",
      "check_in": "2025-01-20",
      "check_out": "2025-01-23",
      "status": "confirmed",
      "total_price": 1860,
      "currency": "CNY",
      "guests": 2,
      "source": "booking.com",
      "booked_at": "2025-01-10T14:30:00Z",
      "units_booked": 1
    }
  ]
}
```

**参数说明**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `reservation_id` | string | 是 | 预订唯一标识 |
| `listing_id` | string | 是 | 房源ID |
| `rate_plan_id` | string | 否 | 价格计划ID |
| `check_in` | string | 是 | 入住日期 |
| `check_out` | string | 是 | 退房日期 |
| `status` | string | 是 | 状态：confirmed/cancelled/pending |
| `total_price` | number | 是 | 总价 |
| `currency` | string | 是 | 货币代码 |
| `guests` | integer | 否 | 入住人数 |
| `source` | string | 否 | 预订来源（渠道） |
| `booked_at` | string | 否 | 预订时间（ISO 8601） |
| `units_booked` | integer | 否 | 预订单元数 |

**响应体**:
```json
{
  "success": ["RES_20250115_001"],
  "failure": []
}
```

---

### 4.7 查询状态 - POST /status

**用途**: 查询房源或预订的处理状态

**请求体**:
```json
{
  "type": "listing",
  "ids": ["room_type_101", "room_type_102"]
}
```

**参数说明**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `type` | string | 是 | 查询类型：listing/reservation |
| `ids` | array | 是 | ID列表 |

**响应体**:
```json
{
  "success": true,
  "data": [
    {
      "id": "room_type_101",
      "status": "active",
      "last_sync": "2025-01-15T10:30:00Z",
      "pricing_enabled": true
    }
  ]
}
```

---

## 5. Webhook 实现

### 5.1 价格同步回调 - sync_url

**PriceLabs 调用此端点推送价格更新**

**请求方式**: POST

**请求头**:
```http
Content-Type: application/json
X-Signature: <sha256_hmac_signature>
```

**请求体**:
```json
{
  "type": "price_update",
  "timestamp": "2025-01-15T10:30:00Z",
  "data": [
    {
      "listing_id": "room_type_101",
      "rate_plan_id": "standard",
      "calendar": [
        {
          "date": "2025-01-20",
          "price": 680,
          "min_stay": 1,
          "max_stay": 30,
          "closed_to_arrival": false,
          "closed_to_departure": false
        }
      ]
    }
  ]
}
```

**PMS 响应**:
```json
{
  "success": true,
  "message": "Price update received",
  "processed_count": 1
}
```

### 5.2 日历触发回调 - calendar_trigger_url

**PriceLabs 调用此端点请求完整日历数据**

**请求体**:
```json
{
  "type": "calendar_request",
  "timestamp": "2025-01-15T10:30:00Z",
  "listing_ids": ["room_type_101", "room_type_102"],
  "start_date": "2025-01-15",
  "end_date": "2025-04-15"
}
```

**PMS 响应**:
```json
{
  "success": true,
  "message": "Calendar refresh triggered"
}
```

> 收到此回调后，PMS 应调用 `/calendar` 端点推送完整日历数据。

### 5.3 错误通知回调 - hook_url

**PriceLabs 调用此端点通知错误信息**

**请求体**:
```json
{
  "type": "error",
  "timestamp": "2025-01-15T10:30:00Z",
  "error_code": "LISTING_NOT_FOUND",
  "error_message": "Listing room_type_999 not found in PriceLabs",
  "related_ids": ["room_type_999"]
}
```

**PMS 响应**:
```json
{
  "success": true,
  "message": "Error notification received"
}
```

### 5.4 签名验证实现

```java
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

public class PriceLabsSignatureVerifier {

    private final String integrationToken;

    public PriceLabsSignatureVerifier(String integrationToken) {
        this.integrationToken = integrationToken;
    }

    /**
     * 验证 PriceLabs Webhook 签名
     * @param requestBody 请求体原始字符串
     * @param signature X-Signature 请求头的值
     * @return 签名是否有效
     */
    public boolean verifySignature(String requestBody, String signature) {
        try {
            Mac sha256Hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(
                integrationToken.getBytes("UTF-8"),
                "HmacSHA256"
            );
            sha256Hmac.init(secretKey);

            byte[] hash = sha256Hmac.doFinal(requestBody.getBytes("UTF-8"));
            String calculatedSignature = Base64.getEncoder().encodeToString(hash);

            return calculatedSignature.equals(signature);
        } catch (Exception e) {
            return false;
        }
    }
}
```

---

## 6. 数据模型定义

### 6.1 房源映射关系

```
PMS 实体                    PriceLabs 字段
─────────────────────────────────────────────
RoomType.id            →    listing_id
RoomType.name          →    name
Store.address          →    address
Store.city             →    city
Store.country          →    country
RoomType.maxGuests     →    accommodates
RoomType.basePrice     →    base_price
Room.count             →    multi_unit_count
```

### 6.2 价格计划映射

```
PMS 实体                    PriceLabs 字段
─────────────────────────────────────────────
PricePlan.id           →    rate_plan_id
PricePlan.name         →    name
PricePlan.isDefault    →    is_default
```

### 6.3 预订映射

```
PMS 实体                    PriceLabs 字段
─────────────────────────────────────────────
Reservation.id         →    reservation_id
Reservation.roomTypeId →    listing_id
Reservation.checkIn    →    check_in
Reservation.checkOut   →    check_out
Reservation.status     →    status
Reservation.totalPrice →    total_price
Channel.name           →    source
```

---

## 7. 贴合项目的实现方案

> 基于当前项目的多门店架构（StoreScopedEntity）和现有实体结构设计

### 7.1 现有实体映射关系

#### RoomType → PriceLabs Listing

| PMS 字段 | PriceLabs 字段 | 说明 |
|----------|----------------|------|
| `RoomType.id` | `listing_id` | 格式：`store_{storeId}_room_type_{id}` |
| `RoomType.name` | `name` | 房型名称 |
| `Store.address` | `address` | 从关联门店获取 |
| `Store.city` | `city` | 城市 |
| `Store.country` | `country` | 国家代码 |
| `RoomType.totalRooms` | `multi_unit_count` | 房间总数 |
| `RoomType.defaultPrice` | `base_price` | 基础价格 |
| `Store.currency` | `currency` | 货币 |

#### PricePlan → PriceLabs Rate Plan

| PMS 字段 | PriceLabs 字段 | 说明 |
|----------|----------------|------|
| `PricePlan.id` | `rate_plan_id` | 格式：`plan_{id}` |
| `PricePlan.name` | `name` | 计划名称 |
| `derivationType == "independent"` | `is_default` | 独立计划默认 |

#### Reservation → PriceLabs Reservation

| PMS 字段 | PriceLabs 字段 | 说明 |
|----------|----------------|------|
| `Reservation.orderNumber` | `reservation_id` | 订单号 |
| `Reservation.room.roomType.id` | `listing_id` | 房型 ID |
| `Reservation.checkInDate` | `check_in` | 入住日期 |
| `Reservation.checkOutDate` | `check_out` | 退房日期 |
| `Reservation.status` | `status` | 状态映射 |
| `Reservation.totalAmount` | `total_price` | 总金额 |
| `Channel.name` | `source` | 渠道来源 |

### 7.2 新增实体设计

#### 7.2.1 PriceLabsIntegration（门店级配置）

```java
// server/src/main/java/server/demo/entity/PriceLabsIntegration.java
@Entity
@Table(name = "pricelabs_integrations")
@EntityListeners(StoreScopedEntityListener.class)
public class PriceLabsIntegration implements StoreScopedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "is_enabled")
    private Boolean isEnabled = false;

    @Column(name = "sync_url", length = 500)
    private String syncUrl;

    @Column(name = "calendar_trigger_url", length = 500)
    private String calendarTriggerUrl;

    @Column(name = "hook_url", length = 500)
    private String hookUrl;

    @Column(name = "last_listing_sync_at")
    private LocalDateTime lastListingSyncAt;

    @Column(name = "last_price_sync_at")
    private LocalDateTime lastPriceSyncAt;

    @Column(name = "last_reservation_sync_at")
    private LocalDateTime lastReservationSyncAt;

    // getters/setters...
}
```

#### 7.2.2 ChannelPrice（渠道价格表）

```java
// server/src/main/java/server/demo/entity/ChannelPrice.java
@Entity
@Table(name = "channel_prices",
       uniqueConstraints = @UniqueConstraint(
           columnNames = {"store_id", "room_type_id", "channel_id", "price_date"}))
@EntityListeners(StoreScopedEntityListener.class)
public class ChannelPrice implements StoreScopedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_type_id", nullable = false)
    private RoomType roomType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "channel_id", nullable = false)
    private Channel channel;

    @Column(name = "price_date", nullable = false)
    private LocalDate priceDate;

    // PriceLabs 推送的基础价格
    @Column(name = "base_price", precision = 10, scale = 2)
    private BigDecimal basePrice;

    // 渠道调整后的售价 = basePrice * (1 + channel.commissionRate)
    @Column(name = "channel_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal channelPrice;

    @Column(name = "min_stay")
    private Integer minStay;

    @Column(name = "max_stay")
    private Integer maxStay;

    @Column(name = "is_synced_to_ota")
    private Boolean isSyncedToOta = false;

    @Column(name = "ota_sync_at")
    private LocalDateTime otaSyncAt;

    @Column(name = "pricelabs_updated_at")
    private LocalDateTime priceLabsUpdatedAt;

    // getters/setters...
}
```

#### 7.2.3 PriceLabsSyncLog（同步日志）

```java
// server/src/main/java/server/demo/entity/PriceLabsSyncLog.java
@Entity
@Table(name = "pricelabs_sync_logs")
@EntityListeners(StoreScopedEntityListener.class)
public class PriceLabsSyncLog implements StoreScopedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "sync_type", nullable = false)
    private SyncType syncType; // LISTING, RATE_PLAN, CALENDAR, RESERVATION, PRICE_UPDATE

    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false)
    private SyncDirection direction; // OUTBOUND, INBOUND

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SyncStatus status; // SUCCESS, FAILURE, PARTIAL

    @Column(name = "affected_count")
    private Integer affectedCount;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "request_data", columnDefinition = "JSON")
    private String requestData;

    @Column(name = "response_data", columnDefinition = "JSON")
    private String responseData;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    // getters/setters...
}
```

### 7.3 扩展 Channel 实体 (仅供参考)

> **重要**: 本节内容已过时。OTA 直连 (Airbnb, Booking.com) 现在使用独立的 `ota_integrations` 表,不使用 `Channel` 表。本节内容仅作为其他渠道(如携程、美团等)的参考实现。

如需为其他非 OTA 直连渠道添加配置,可参考以下字段设计:

```java
// 在 Channel.java 中添加以下字段(仅供参考)

// 渠道 API 配置
@Column(name = "api_url", length = 500)
private String apiUrl;

@Column(name = "api_key", length = 255)
private String apiKey;

@Column(name = "api_secret", length = 255)
private String apiSecret;

@Column(name = "property_id", length = 100)
private String propertyId;

// 价格调整方式: PERCENTAGE（百分比）, FIXED（固定金额）
// 注意: 已移除 COMMISSION 类型,统一使用 PERCENTAGE 模式
@Enumerated(EnumType.STRING)
@Column(name = "price_adjustment_type")
private PriceAdjustmentType priceAdjustmentType = PriceAdjustmentType.PERCENTAGE;

// 价格调整值（根据 adjustmentType 解释）
@Column(name = "price_adjustment_value", precision = 10, scale = 2)
private BigDecimal priceAdjustmentValue;

// 是否自动同步价格到此渠道
@Column(name = "auto_sync_price")
private Boolean autoSyncPrice = true;
```

### 7.4 后端实现任务清单

#### 第一阶段：基础设施（优先级：高）

```
server/src/main/java/server/demo/
├── config/
│   └── PriceLabsConfig.java              # API 配置类
├── dto/pricelabs/
│   ├── PriceLabsListingDTO.java          # 房源 DTO
│   ├── PriceLabsCalendarDTO.java         # 日历 DTO
│   ├── PriceLabsReservationDTO.java      # 预订 DTO
│   ├── PriceLabsPriceUpdateDTO.java      # 价格更新 DTO（Webhook 接收）
│   └── PriceLabsIntegrationDTO.java      # 集成配置 DTO
├── entity/
│   ├── PriceLabsIntegration.java         # 集成配置实体
│   ├── ChannelPrice.java                 # 渠道价格实体
│   └── PriceLabsSyncLog.java             # 同步日志实体
├── enums/
│   ├── SyncType.java                     # 同步类型枚举
│   ├── SyncDirection.java                # 同步方向枚举
│   ├── SyncStatus.java                   # 同步状态枚举
│   └── PriceAdjustmentType.java          # 价格调整类型枚举
├── repository/
│   ├── PriceLabsIntegrationRepository.java
│   ├── ChannelPriceRepository.java
│   └── PriceLabsSyncLogRepository.java
└── service/
    ├── PriceLabsApiClient.java           # API 客户端
    ├── PriceLabsIntegrationService.java  # 集成服务
    ├── PriceLabsDataMapper.java          # 数据转换
    ├── ChannelPriceService.java          # 渠道价格服务
    └── OtaSyncService.java               # OTA 同步服务
```

#### 第二阶段：核心服务实现

**PriceLabsIntegrationService.java** 核心方法：

```java
@Service
@StoreScoped
public class PriceLabsIntegrationService {

    /**
     * 配置 PriceLabs 集成（设置 Webhook URL）
     */
    public void configureIntegration(PriceLabsIntegrationDTO config);

    /**
     * 同步房型到 PriceLabs
     * 将 RoomType 转换为 PriceLabs Listing 格式并推送
     */
    public SyncResult syncListings();

    /**
     * 同步价格计划到 PriceLabs
     */
    public SyncResult syncRatePlans();

    /**
     * 同步日历数据到 PriceLabs
     * 包括可用房间数、已有价格等
     */
    public SyncResult syncCalendar(LocalDate startDate, LocalDate endDate);

    /**
     * 同步预订数据到 PriceLabs
     * 用于 PriceLabs 分析入住率
     */
    public SyncResult syncReservations(LocalDate startDate, LocalDate endDate);

    /**
     * 主动获取 PriceLabs 价格建议
     */
    public List<PriceLabsPriceUpdateDTO> fetchPrices(List<Long> roomTypeIds);
}
```

**ChannelPriceService.java** 核心方法：

```java
@Service
@StoreScoped
public class ChannelPriceService {

    /**
     * 处理 PriceLabs 推送的价格更新
     * 1. 保存基础价格到 RoomPrice
     * 2. 计算各渠道价格并保存到 ChannelPrice
     */
    public void handlePriceUpdate(PriceLabsPriceUpdateDTO priceUpdate);

    /**
     * 根据渠道配置计算调整后的价格
     */
    public BigDecimal calculateChannelPrice(BigDecimal basePrice, Channel channel);

    /**
     * 获取指定房型在各渠道的价格
     */
    public List<ChannelPriceDTO> getChannelPrices(Long roomTypeId, LocalDate date);

    /**
     * 触发渠道价格同步到 OTA
     */
    public void triggerOtaSync(Long roomTypeId, LocalDate startDate, LocalDate endDate);
}
```

#### 第三阶段：Webhook 控制器

```java
// server/src/main/java/server/demo/controller/PriceLabsWebhookController.java
@RestController
@RequestMapping("/api/v1/pricelabs/webhook")
public class PriceLabsWebhookController {

    /**
     * 接收 PriceLabs 价格更新推送
     * POST /api/v1/pricelabs/webhook/sync
     */
    @PostMapping("/sync")
    public ResponseEntity<Map<String, Object>> handlePriceSync(
            @RequestHeader("X-Signature") String signature,
            @RequestBody String rawBody) {
        // 1. 验证签名
        // 2. 解析价格数据
        // 3. 更新本地价格
        // 4. 计算渠道价格
        // 5. 触发 OTA 同步
    }

    /**
     * 接收日历刷新请求
     * POST /api/v1/pricelabs/webhook/calendar-trigger
     */
    @PostMapping("/calendar-trigger")
    public ResponseEntity<Map<String, Object>> handleCalendarTrigger(...);

    /**
     * 接收错误通知
     * POST /api/v1/pricelabs/webhook/hook
     */
    @PostMapping("/hook")
    public ResponseEntity<Map<String, Object>> handleHook(...);
}
```

#### 第四阶段：管理端点

```java
// server/src/main/java/server/demo/controller/PriceLabsController.java
@RestController
@RequestMapping("/api/v1/pricelabs")
@StoreScoped
public class PriceLabsController {

    // 获取集成状态
    @GetMapping("/status")
    public ApiResponse<PriceLabsIntegrationDTO> getIntegrationStatus();

    // 配置集成
    @PostMapping("/configure")
    public ApiResponse<Void> configureIntegration(@RequestBody ConfigureRequest request);

    // 手动同步房源
    @PostMapping("/sync/listings")
    public ApiResponse<SyncResult> syncListings();

    // 手动同步预订
    @PostMapping("/sync/reservations")
    public ApiResponse<SyncResult> syncReservations(@RequestBody DateRangeRequest request);

    // 手动获取价格
    @PostMapping("/fetch-prices")
    public ApiResponse<List<PriceLabsPriceUpdateDTO>> fetchPrices(@RequestBody FetchPricesRequest request);

    // 查看同步日志
    @GetMapping("/logs")
    public ApiResponse<Page<PriceLabsSyncLogDTO>> getSyncLogs(Pageable pageable);

    // 获取渠道价格
    @GetMapping("/channel-prices")
    public ApiResponse<List<ChannelPriceDTO>> getChannelPrices(
            @RequestParam Long roomTypeId,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate);
}
```

### 7.5 前端实现方案（基于现有 PricingTools.vue）

> 现有页面路径：`client/src/views/settings/third-party/PricingTools.vue`
> 菜单位置：设置 → 第三方集成 → 定价工具

#### 7.5.1 现有页面结构分析

当前 `PricingTools.vue` 已有以下功能：
- ✅ 连接状态视图（首页卡片）
- ✅ 配置列表视图（表格展示）
- ✅ 添加/编辑连接对话框
- ❌ **使用模拟数据，需改为真实 API 调用**
- ❌ **未关联实际的房型和价格计划**

#### 7.5.2 改造任务清单

**文件结构：**

```
client/src/
├── api/
│   └── pricelabs.ts                      # 新增：PriceLabs API 封装
├── types/
│   └── pricelabs.ts                      # 新增：类型定义
└── views/settings/third-party/
    └── PricingTools.vue                  # 改造：对接真实数据
```

#### 7.5.3 新增 API 模块

**文件：`client/src/api/pricelabs.ts`**

```typescript
import request from '@/utils/request'
import type {
  PriceLabsIntegrationDTO,
  PriceLabsConnectionDTO,
  PriceLabsSyncLogDTO,
  ChannelPriceDTO,
  SyncResult,
} from '@/types/pricelabs'

/**
 * 获取 PriceLabs 集成状态
 */
export const getIntegrationStatus = () => {
  return request<PriceLabsIntegrationDTO>({
    url: '/pricelabs/status',
    method: 'GET',
  })
}

/**
 * 配置 PriceLabs 集成
 */
export const configureIntegration = (data: {
  syncUrl?: string
  calendarTriggerUrl?: string
  hookUrl?: string
  isEnabled?: boolean
}) => {
  return request<void>({
    url: '/pricelabs/configure',
    method: 'POST',
    data,
  })
}

/**
 * 获取房型-价格计划连接列表
 */
export const getConnections = () => {
  return request<PriceLabsConnectionDTO[]>({
    url: '/pricelabs/connections',
    method: 'GET',
  })
}

/**
 * 创建连接（房型 + 价格计划 → PriceLabs Listing）
 */
export const createConnection = (data: {
  roomTypeId: number
  pricePlanId: number
  priceLabsListingId?: string
}) => {
  return request<PriceLabsConnectionDTO>({
    url: '/pricelabs/connections',
    method: 'POST',
    data,
  })
}

/**
 * 更新连接
 */
export const updateConnection = (id: number, data: {
  roomTypeId?: number
  pricePlanId?: number
  priceLabsListingId?: string
  isEnabled?: boolean
}) => {
  return request<PriceLabsConnectionDTO>({
    url: `/pricelabs/connections/${id}`,
    method: 'PUT',
    data,
  })
}

/**
 * 删除连接
 */
export const deleteConnection = (id: number) => {
  return request<void>({
    url: `/pricelabs/connections/${id}`,
    method: 'DELETE',
  })
}

/**
 * 手动同步房源到 PriceLabs
 */
export const syncListings = () => {
  return request<SyncResult>({
    url: '/pricelabs/sync/listings',
    method: 'POST',
  })
}

/**
 * 手动同步预订到 PriceLabs
 */
export const syncReservations = (startDate: string, endDate: string) => {
  return request<SyncResult>({
    url: '/pricelabs/sync/reservations',
    method: 'POST',
    data: { startDate, endDate },
  })
}

/**
 * 手动触发获取价格
 */
export const fetchPrices = (roomTypeIds?: number[]) => {
  return request<SyncResult>({
    url: '/pricelabs/fetch-prices',
    method: 'POST',
    data: { roomTypeIds },
  })
}

/**
 * 获取同步日志
 */
export const getSyncLogs = (params: {
  page?: number
  size?: number
  syncType?: string
}) => {
  return request<{
    content: PriceLabsSyncLogDTO[]
    totalElements: number
    totalPages: number
  }>({
    url: '/pricelabs/logs',
    method: 'GET',
    params,
  })
}

/**
 * 获取渠道价格预览
 */
export const getChannelPrices = (
  roomTypeId: number,
  startDate: string,
  endDate: string
) => {
  return request<ChannelPriceDTO[]>({
    url: '/pricelabs/channel-prices',
    method: 'GET',
    params: { roomTypeId, startDate, endDate },
  })
}
```

#### 7.5.4 新增类型定义

**文件：`client/src/types/pricelabs.ts`**

```typescript
/**
 * PriceLabs 集成配置
 */
export interface PriceLabsIntegrationDTO {
  id: number
  storeId: number
  isEnabled: boolean
  syncUrl?: string
  calendarTriggerUrl?: string
  hookUrl?: string
  lastListingSyncAt?: string
  lastPriceSyncAt?: string
  lastReservationSyncAt?: string
  createdAt: string
  updatedAt: string
}

/**
 * 房型-价格计划连接（映射到 PriceLabs Listing）
 */
export interface PriceLabsConnectionDTO {
  id: number
  roomTypeId: number
  roomTypeName: string
  roomTypeCode: string
  pricePlanId: number
  pricePlanName: string
  priceLabsListingId?: string  // PriceLabs 中的 listing_id
  isEnabled: boolean
  lastSyncAt?: string
  syncStatus: 'connected' | 'disconnected' | 'error'
  errorMessage?: string
  createdAt: string
  updatedAt: string
}

/**
 * 同步日志
 */
export interface PriceLabsSyncLogDTO {
  id: number
  syncType: 'LISTING' | 'RATE_PLAN' | 'CALENDAR' | 'RESERVATION' | 'PRICE_UPDATE'
  direction: 'OUTBOUND' | 'INBOUND'
  status: 'SUCCESS' | 'FAILURE' | 'PARTIAL'
  affectedCount?: number
  errorMessage?: string
  createdAt: string
}

/**
 * 渠道价格
 */
export interface ChannelPriceDTO {
  id: number
  roomTypeId: number
  roomTypeName: string
  channelId: number
  channelName: string
  channelCode: string
  priceDate: string
  basePrice: number      // PriceLabs 推送的基础价格
  channelPrice: number   // 渠道调整后的售价
  minStay?: number
  maxStay?: number
  isSyncedToOta: boolean
  otaSyncAt?: string
}

/**
 * 同步结果
 */
export interface SyncResult {
  success: boolean
  message: string
  successCount: number
  failureCount: number
  errors?: string[]
}

/**
 * 连接表单
 */
export interface ConnectionForm {
  roomTypeId: number | null
  pricePlanId: number | null
  priceLabsListingId: string
  isEnabled: boolean
}
```

#### 7.5.5 PricingTools.vue 改造要点

**关键改动：**

1. **引入真实 API 和数据**
```typescript
import { getAllRoomTypes, type RoomTypeDTO } from '@/api/roomType'
import { getAllPricePlans, getPricePlansByRoomType, type PricePlanDTO } from '@/api/pricePlan'
import * as priceLabsApi from '@/api/pricelabs'
import type { PriceLabsConnectionDTO, ConnectionForm } from '@/types/pricelabs'
```

2. **替换模拟数据为真实数据**
```typescript
// 真实数据
const roomTypes = ref<RoomTypeDTO[]>([])
const pricePlans = ref<PricePlanDTO[]>([])
const connections = ref<PriceLabsConnectionDTO[]>([])
const integrationStatus = ref<PriceLabsIntegrationDTO | null>(null)

// 加载数据
const loadData = async () => {
  try {
    const [roomTypesRes, pricePlansRes, connectionsRes, statusRes] = await Promise.all([
      getAllRoomTypes(),
      getAllPricePlans(userId.value),
      priceLabsApi.getConnections(),
      priceLabsApi.getIntegrationStatus(),
    ])

    if (roomTypesRes.success) roomTypes.value = roomTypesRes.data
    if (pricePlansRes.success) pricePlans.value = pricePlansRes.data
    if (connectionsRes.success) connections.value = connectionsRes.data
    if (statusRes.success) integrationStatus.value = statusRes.data
  } catch (error) {
    ElMessage.error('加载数据失败')
  }
}
```

3. **对话框改造：房型选择后自动加载关联的价格计划**
```typescript
// 当房型改变时，加载该房型的价格计划
const handleRoomTypeChange = async (roomTypeId: number) => {
  if (!roomTypeId) {
    availablePricePlans.value = []
    return
  }

  try {
    const res = await getPricePlansByRoomType(roomTypeId)
    if (res.success) {
      availablePricePlans.value = res.data.map(item => item.pricePlan!)
    }
  } catch (error) {
    ElMessage.error('加载价格计划失败')
  }
}
```

4. **表格列调整**
```vue
<el-table :data="filteredConnections" border stripe>
  <el-table-column prop="roomTypeName" label="房型" min-width="150" />
  <el-table-column prop="pricePlanName" label="价格计划" min-width="150" />
  <el-table-column prop="priceLabsListingId" label="PriceLabs ID" width="180" />
  <el-table-column label="状态" width="100" align="center">
    <template #default="{ row }">
      <el-tag :type="getStatusType(row.syncStatus)" size="small">
        {{ getStatusLabel(row.syncStatus) }}
      </el-tag>
    </template>
  </el-table-column>
  <el-table-column prop="lastSyncAt" label="最近同步" width="180" align="center">
    <template #default="{ row }">
      {{ row.lastSyncAt ? formatDateTime(row.lastSyncAt) : '-' }}
    </template>
  </el-table-column>
  <el-table-column label="操作" width="200" align="center" fixed="right">
    <!-- 操作按钮 -->
  </el-table-column>
</el-table>
```

5. **添加同步日志 Tab**
```vue
<el-tabs v-model="activeTab">
  <el-tab-pane label="连接配置" name="connections">
    <!-- 现有配置表格 -->
  </el-tab-pane>
  <el-tab-pane label="同步日志" name="logs">
    <SyncLogList />
  </el-tab-pane>
  <el-tab-pane label="渠道价格预览" name="channel-prices">
    <ChannelPricePreview />
  </el-tab-pane>
</el-tabs>
```

#### 7.5.6 新增子组件

**组件目录：`client/src/views/settings/third-party/components/`**

| 组件 | 功能 |
|------|------|
| `SyncLogList.vue` | 显示同步日志列表，支持筛选和分页 |
| `ChannelPricePreview.vue` | 预览各渠道的调整后价格 |
| `IntegrationStatusCard.vue` | 集成状态卡片，显示最后同步时间等 |

#### 7.5.7 完整改造后的页面功能

```
┌─────────────────────────────────────────────────────────────────────────┐
│  PriceLabs 定价工具                                                      │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │ 集成状态：✅ 已连接    最后同步：2025-01-15 14:30:00              │   │
│  │ [同步房源] [同步预订] [获取价格]                                    │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                                                                         │
│  ┌───────────────┬───────────────┬───────────────────┐                 │
│  │ 连接配置      │ 同步日志      │ 渠道价格预览        │                 │
│  └───────────────┴───────────────┴───────────────────┘                 │
│                                                                         │
│  筛选：[全部 ▼]                                    [添加连接]           │
│  ┌────────────────────────────────────────────────────────────────┐    │
│  │ 房型          │ 价格计划      │ PriceLabs ID │ 状态  │ 操作    │    │
│  ├────────────────────────────────────────────────────────────────┤    │
│  │ 豪华大床房    │ Standard Rate │ store_1_rt_1 │ ✅已连接 │ ...   │    │
│  │ 标准双人间    │ Weekend Rate  │ store_1_rt_2 │ ✅已连接 │ ...   │    │
│  │ 家庭套房      │ Standard Rate │ -            │ ⚠️未连接 │ ...   │    │
│  └────────────────────────────────────────────────────────────────┘    │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

### 7.6 数据库变更脚本

```sql
-- =====================================================
-- PriceLabs 集成相关表
-- =====================================================

-- 1. 集成配置表（门店级）
CREATE TABLE pricelabs_integrations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    store_id BIGINT NOT NULL,
    is_enabled BOOLEAN DEFAULT FALSE,
    sync_url VARCHAR(500),
    calendar_trigger_url VARCHAR(500),
    hook_url VARCHAR(500),
    last_listing_sync_at DATETIME,
    last_price_sync_at DATETIME,
    last_reservation_sync_at DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_store (store_id),
    FOREIGN KEY (store_id) REFERENCES stores(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. 渠道价格表（存储各渠道的调整后价格）
CREATE TABLE channel_prices (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    store_id BIGINT NOT NULL,
    room_type_id BIGINT NOT NULL,
    channel_id BIGINT NOT NULL,
    price_date DATE NOT NULL,
    base_price DECIMAL(10, 2) COMMENT 'PriceLabs 推送的基础价格',
    channel_price DECIMAL(10, 2) NOT NULL COMMENT '渠道调整后的售价',
    min_stay INT,
    max_stay INT,
    is_synced_to_ota BOOLEAN DEFAULT FALSE,
    ota_sync_at DATETIME,
    pricelabs_updated_at DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_channel_price (store_id, room_type_id, channel_id, price_date),
    INDEX idx_room_type_date (room_type_id, price_date),
    INDEX idx_channel_date (channel_id, price_date),
    INDEX idx_sync_status (is_synced_to_ota),
    FOREIGN KEY (store_id) REFERENCES stores(id) ON DELETE CASCADE,
    FOREIGN KEY (room_type_id) REFERENCES room_types(id) ON DELETE CASCADE,
    FOREIGN KEY (channel_id) REFERENCES channels(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. 同步日志表
CREATE TABLE pricelabs_sync_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    store_id BIGINT NOT NULL,
    sync_type ENUM('LISTING', 'RATE_PLAN', 'CALENDAR', 'RESERVATION', 'PRICE_UPDATE') NOT NULL,
    direction ENUM('OUTBOUND', 'INBOUND') NOT NULL,
    status ENUM('SUCCESS', 'FAILURE', 'PARTIAL') NOT NULL,
    affected_count INT,
    error_message TEXT,
    request_data JSON,
    response_data JSON,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_store_type (store_id, sync_type),
    INDEX idx_created_at (created_at),
    FOREIGN KEY (store_id) REFERENCES stores(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4. 扩展 Channel 表（添加 API 配置）【仅供参考】
-- 注意: OTA 直连 (Airbnb, Booking.com) 使用 ota_integrations 表,不需要此扩展
-- 此脚本仅供其他渠道(携程、美团等)参考
ALTER TABLE channels
    ADD COLUMN api_url VARCHAR(500) AFTER notes,
    ADD COLUMN api_key VARCHAR(255) AFTER api_url,
    ADD COLUMN api_secret VARCHAR(255) AFTER api_key,
    ADD COLUMN property_id VARCHAR(100) AFTER api_secret,
    ADD COLUMN price_adjustment_type ENUM('PERCENTAGE', 'FIXED') DEFAULT 'PERCENTAGE' AFTER property_id,
    ADD COLUMN price_adjustment_value DECIMAL(10, 2) AFTER price_adjustment_type,
    ADD COLUMN auto_sync_price BOOLEAN DEFAULT TRUE AFTER price_adjustment_value;

-- 5. 扩展 RoomPrice 表（添加 PriceLabs 来源标记）
ALTER TABLE room_prices
    ADD COLUMN price_source ENUM('MANUAL', 'PRICELABS', 'IMPORTED') DEFAULT 'MANUAL' AFTER notes,
    ADD COLUMN pricelabs_updated_at DATETIME AFTER price_source;

-- 6. 扩展 PriceChangeHistory 表（添加来源标记）
ALTER TABLE price_change_histories
    ADD COLUMN source ENUM('MANUAL', 'PRICELABS', 'OTA_SYNC') DEFAULT 'MANUAL' AFTER operator;
```

### 7.7 价格同步完整流程

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        PriceLabs 价格同步完整流程                            │
└─────────────────────────────────────────────────────────────────────────────┘

1. PriceLabs 推送价格
   │
   ▼
2. PriceLabsWebhookController 接收
   │
   ├─► 验证签名 (X-Signature)
   │
   ▼
3. ChannelPriceService.handlePriceUpdate()
   │
   ├─► 更新 RoomPrice 表（基础价格）
   │   └─ price_source = 'PRICELABS'
   │
   ├─► 遍历所有启用的 Channel
   │   │
   │   ├─► 计算渠道价格
   │   │   channelPrice = basePrice × (1 + commissionRate)
   │   │
   │   └─► 保存到 ChannelPrice 表
   │
   └─► 记录到 PriceLabsSyncLog
   │
   ▼
4. OtaSyncService.syncToChannels()
   │
   ├─► 遍历 auto_sync_price=true 的渠道
   │   │
   │   ├─► 调用 OTA API 更新价格
   │   │   └─ 使用 channel.ota_api_url, ota_api_key 等
   │   │
   │   └─► 更新 ChannelPrice.is_synced_to_ota = true
   │
   └─► 记录到 PriceLabsSyncLog
   │
   ▼
5. 完成
```

---

## 8. 错误处理

### 8.1 API 错误码

| 错误码 | HTTP 状态 | 说明 | 处理方式 |
|--------|-----------|------|----------|
| `INVALID_TOKEN` | 401 | 令牌无效 | 检查配置或重新获取令牌 |
| `RATE_LIMIT_EXCEEDED` | 429 | 超出速率限制 | 等待 Retry-After 后重试 |
| `LISTING_NOT_FOUND` | 404 | 房源不存在 | 先调用 /listings 创建 |
| `INVALID_DATE_RANGE` | 400 | 日期范围无效 | 检查日期格式和范围 |
| `DUPLICATE_LISTING_ID` | 409 | 房源ID重复 | 使用唯一标识 |

### 8.2 重试策略

```java
@Configuration
public class RetryConfig {

    @Bean
    public RetryTemplate priceLabsRetryTemplate() {
        RetryTemplate template = new RetryTemplate();

        // 指数退避：初始1秒，最大30秒
        ExponentialBackOffPolicy backOff = new ExponentialBackOffPolicy();
        backOff.setInitialInterval(1000);
        backOff.setMaxInterval(30000);
        backOff.setMultiplier(2.0);
        template.setBackOffPolicy(backOff);

        // 最多重试3次
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(3);
        template.setRetryPolicy(retryPolicy);

        return template;
    }
}
```

### 8.3 速率限制处理

```java
public class RateLimitHandler {

    private final AtomicInteger requestCount = new AtomicInteger(0);
    private volatile long windowStart = System.currentTimeMillis();
    private static final int MAX_REQUESTS_PER_MINUTE = 300;

    public synchronized boolean canMakeRequest() {
        long now = System.currentTimeMillis();
        if (now - windowStart > 60000) {
            // 重置窗口
            windowStart = now;
            requestCount.set(0);
        }
        return requestCount.incrementAndGet() <= MAX_REQUESTS_PER_MINUTE;
    }

    public void handleRateLimitResponse(HttpResponse response) {
        String retryAfter = response.getHeader("Retry-After");
        if (retryAfter != null) {
            int waitSeconds = Integer.parseInt(retryAfter);
            // 等待指定时间后重试
        }
    }
}
```

---

## 9. 安全考虑

### 9.1 敏感信息管理

- 集成令牌必须存储在环境变量中
- 禁止在日志中输出完整令牌
- 数据库中存储加密后的令牌

### 9.2 Webhook 安全

- 必须验证所有 Webhook 请求的签名
- 使用 HTTPS 端点
- 实现请求去重（基于 timestamp）

### 9.3 数据验证

- 验证所有输入数据的格式和范围
- 对 listing_id 进行字符过滤（仅允许字母数字下划线）
- 验证日期范围合理性

---

## 10. 测试指南

### 10.1 集成配置测试

**测试步骤**:
1. 启动后端服务
2. 调用配置接口设置 Webhook URL
3. 检查 PriceLabs 端是否收到配置

**预期结果**:
- 返回成功响应
- Webhook URL 正确保存

### 10.2 房源同步测试

**测试步骤**:
1. 在 PMS 中创建房型
2. 调用房源同步接口
3. 登录 PriceLabs 检查房源列表

**预期结果**:
- PriceLabs 中显示同步的房源
- 房源信息正确

### 10.3 价格接收测试

**测试步骤**:
1. 在 PriceLabs 中设置房源价格
2. 等待价格推送或手动触发
3. 检查 PMS 中的房价是否更新

**预期结果**:
- PMS 房价表更新为 PriceLabs 推送的价格
- 同步日志记录正确

### 10.4 预订同步测试

**测试步骤**:
1. 在 PMS 中创建预订
2. 调用预订同步接口
3. 检查 PriceLabs 中的预订数据

**预期结果**:
- PriceLabs 显示预订信息
- 入住率数据更新

---

## 附录

### A. API 请求示例（cURL）

**配置集成**:
```bash
curl -X POST "https://api.pricelabs.co/v1/integration/api/integration" \
  -H "Content-Type: application/json" \
  -H "X-INTEGRATION-NAME: thehosthub" \
  -H "X-INTEGRATION-TOKEN: z7FgFEQn42hKzDNza76dpY0hQHz6P6mfEM/0xtT78paBIOmvTmYti/2n3l1K7Ac3" \
  -d '{
    "sync_url": "https://your-pms.com/api/v1/pricelabs/sync",
    "calendar_trigger_url": "https://your-pms.com/api/v1/pricelabs/calendar-trigger",
    "hook_url": "https://your-pms.com/api/v1/pricelabs/hook",
    "regenerate_token": false
  }'
```

**推送房源**:
```bash
curl -X POST "https://api.pricelabs.co/v1/integration/api/listings" \
  -H "Content-Type: application/json" \
  -H "X-INTEGRATION-NAME: thehosthub" \
  -H "X-INTEGRATION-TOKEN: z7FgFEQn42hKzDNza76dpY0hQHz6P6mfEM/0xtT78paBIOmvTmYti/2n3l1K7Ac3" \
  -d '{
    "listings": [
      {
        "listing_id": "room_type_101",
        "name": "豪华大床房",
        "address": "北京市朝阳区XXX路XX号",
        "city": "北京",
        "country": "CN",
        "latitude": 39.9042,
        "longitude": 116.4074,
        "bedrooms": 1,
        "bathrooms": 1,
        "accommodates": 2,
        "property_type": "hotel_room",
        "currency": "CNY",
        "base_price": 500,
        "active": true
      }
    ]
  }'
```

### B. 参考链接

- [PriceLabs API 文档](https://help.pricelabs.co/portal/en/kb/articles/building-an-integration-with-pricelabs)
- [PriceLabs Swagger 规范](https://app.swaggerhub.com/apis/PriceLabs/price-labs_connector/1.0.0)
- [PriceLabs Postman Collection](https://documenter.getpostman.com/view/507656/SVSEurQC)

### C. 版本历史

| 版本 | 日期 | 变更说明 |
|------|------|----------|
| 1.0.0 | 2025-01-15 | 初始版本 |
