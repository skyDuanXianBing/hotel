# 房态管理系统 API 文档

## 1. 房型管理 API

### 1.1 获取房型列表
```
GET /api/v1/room-types
```

**响应示例:**
```json
{
  "success": true,
  "message": "操作成功",
  "data": [
    {
      "id": 1,
      "name": "大床房",
      "code": "DBF",
      "totalRooms": 10,
      "description": "舒适大床房"
    },
    {
      "id": 2,
      "name": "标准间",
      "code": "BZJ", 
      "totalRooms": 15,
      "description": "标准双人间"
    }
  ]
}
```

## 2. 房间管理 API

### 2.1 获取房间列表
```
GET /api/v1/rooms?roomTypeId={roomTypeId}&date={date}
```

**参数:**
- `roomTypeId` (可选): 房型ID
- `date` (可选): 查询日期 (YYYY-MM-DD)

**响应示例:**
```json
{
  "success": true,
  "message": "操作成功",
  "data": [
    {
      "id": 1,
      "roomNumber": "a01",
      "roomType": {
        "id": 1,
        "name": "大床房",
        "code": "DBF"
      },
      "floor": 1,
      "status": "AVAILABLE"
    }
  ]
}
```

## 3. 房态管理 API

### 3.1 获取房态日历数据
```
GET /api/v1/room-status/calendar?startDate={startDate}&endDate={endDate}
```

**参数:**
- `startDate`: 开始日期 (YYYY-MM-DD)
- `endDate`: 结束日期 (YYYY-MM-DD)

**响应示例:**
```json
{
  "success": true,
  "message": "操作成功",
  "data": {
    "dateRange": {
      "startDate": "2025-09-21",
      "endDate": "2025-10-04"
    },
    "rooms": [
      {
        "roomId": 1,
        "roomNumber": "a01",
        "roomType": "大床房",
        "dailyStatus": [
          {
            "date": "2025-09-21",
            "status": "AVAILABLE",
            "reservation": null
          },
          {
            "date": "2025-09-22",
            "status": "OCCUPIED",
            "reservation": {
              "id": 1,
              "guestName": "林",
              "channel": "自来客",
              "checkIn": "2025-09-22",
              "checkOut": "2025-09-24",
              "orderNumber": "aa-a01"
            }
          }
        ]
      }
    ]
  }
}
```

### 3.2 获取单日房态
```
GET /api/v1/room-status/daily?date={date}
```

**参数:**
- `date`: 查询日期 (YYYY-MM-DD)

**响应示例:**
```json
{
  "success": true,
  "message": "操作成功",
  "data": {
    "date": "2025-09-23",
    "roomTypes": [
      {
        "id": 1,
        "name": "大床房",
        "totalRooms": 5,
        "availableRooms": 3,
        "occupiedRooms": 1,
        "maintenanceRooms": 1,
        "rooms": [
          {
            "id": 1,
            "roomNumber": "a01",
            "status": "OCCUPIED",
            "guestName": "林",
            "channel": "自来客"
          }
        ]
      }
    ]
  }
}
```

### 3.3 更新房间状态
```
PUT /api/v1/room-status/{roomId}
```

**请求体:**
```json
{
  "date": "2025-09-23",
  "status": "MAINTENANCE",
  "reason": "清洁维护"
}
```

**房间状态枚举:**
- `AVAILABLE` - 可用
- `OCCUPIED` - 已入住  
- `RESERVED` - 已预订
- `MAINTENANCE` - 维修
- `OUT_OF_ORDER` - 停用

## 4. 预订管理 API

### 4.1 创建预订
```
POST /api/v1/reservations
```

**请求体:**
```json
{
  "roomId": 1,
  "guestName": "张三",
  "phone": "13800138000",
  "checkInDate": "2025-09-25",
  "checkOutDate": "2025-09-27",
  "channel": "OTA",
  "adults": 2,
  "children": 0,
  "totalAmount": 500.00,
  "notes": "晚到"
}
```

### 4.2 获取预订列表
```
GET /api/v1/reservations?startDate={startDate}&endDate={endDate}&status={status}
```

### 4.3 取消预订
```
DELETE /api/v1/reservations/{reservationId}
```

## 5. 渠道管理 API

### 5.1 获取渠道列表
```
GET /api/v1/channels
```

**响应示例:**
```json
{
  "success": true,
  "message": "操作成功",
  "data": [
    {
      "id": 1,
      "name": "自来客",
      "code": "DIRECT",
      "type": "DIRECT"
    },
    {
      "id": 2,
      "name": "携程",
      "code": "CTRIP",
      "type": "OTA"
    }
  ]
}
```

### 5.2 获取渠道房型映射
```
GET /api/v1/channels/{channelId}/room-types
```

### 5.3 批量修改渠道房态
```
PUT /api/v1/channels/{channelId}/room-status/batch
```

**请求体:**
```json
{
  "date": "2025-09-23",
  "roomTypeUpdates": [
    {
      "roomTypeId": 1,
      "availableRooms": 5,
      "price": 288.00
    }
  ]
}
```

## 6. 房态修改记录 API

### 6.1 获取修改记录
```
GET /api/v1/room-status/logs?startDate={startDate}&endDate={endDate}
```

**响应示例:**
```json
{
  "success": true,
  "message": "操作成功",
  "data": [
    {
      "id": 1,
      "roomId": 1,
      "roomNumber": "a01",
      "date": "2025-09-23",
      "oldStatus": "AVAILABLE",
      "newStatus": "MAINTENANCE",
      "reason": "清洁维护",
      "operatorName": "管理员",
      "operatedAt": "2025-09-23T10:30:00"
    }
  ]
}
```

## 数据模型说明

### 房间状态 (RoomStatus)
- `AVAILABLE` - 可用房
- `OCCUPIED` - 已入住
- `RESERVED` - 已预订待入住
- `MAINTENANCE` - 维修中
- `OUT_OF_ORDER` - 停用

### 渠道类型 (ChannelType)
- `DIRECT` - 直销
- `OTA` - 在线旅行社
- `TRAVEL_AGENCY` - 旅行社
- `CORPORATE` - 企业客户

### 预订状态 (ReservationStatus)
- `CONFIRMED` - 已确认
- `CHECKED_IN` - 已入住
- `CHECKED_OUT` - 已退房
- `CANCELLED` - 已取消
- `NO_SHOW` - 未到