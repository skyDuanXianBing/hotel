# Su Widget OTA集成测试步骤

## 📋 测试前准备清单

### 1. 环境配置检查

- [ ] 已在 `server/.env` 或环境变量中配置Su API凭证
- [ ] MySQL数据库已启动并可访问
- [ ] Redis服务已启动（如果使用）
- [ ] 后端服务可以正常启动
- [ ] 前端服务可以正常启动

### 2. 数据库准备

确认 `ota_integrations` 表中已存在Airbnb和Booking.com的记录：

```sql
-- 检查OTA记录是否存在
SELECT id, name, code, is_connected, logo_url
FROM ota_integrations
WHERE code IN ('AIRBNB', 'BOOKING');
```

**预期结果**：
- 应该有2条记录（Airbnb和Booking.com）
- `is_connected` 字段初始值应为 false 或 NULL

如果没有记录，需要先在数据库中创建：

```sql
-- 插入Airbnb记录（如果不存在）
INSERT INTO ota_integrations (store_id, name, code, logo_url, is_connected, enabled, created_at, updated_at)
VALUES (1, 'Airbnb', 'AIRBNB', 'https://example.com/airbnb-logo.png', false, true, NOW(), NOW());

-- 插入Booking.com记录（如果不存在）
INSERT INTO ota_integrations (store_id, name, code, logo_url, is_connected, enabled, created_at, updated_at)
VALUES (1, 'Booking.com', 'BOOKING', 'https://example.com/booking-logo.png', false, true, NOW(), NOW());
```

---

## 🧪 测试步骤

### 测试1：验证Su API凭证

**操作步骤**：
1. 打开PowerShell
2. 导航到项目根目录：
   ```powershell
   cd d:\project\住房管理jap
   ```
3. 运行测试脚本：
   ```powershell
   .\test-su-api.ps1
   ```

**检查要点**：
- [ ] 脚本能够成功运行
- [ ] Access Token生成成功（显示绿色✓）
- [ ] Access Token验证成功
- [ ] Widget Token生成成功

**预期结果**：
```
==================================
Su API 凭证测试
==================================

[测试 1/3] 生成Access Token...
✓ Access Token生成成功!
  Token类型: Bearer
  Token: sandbox_xxxxxxxxxxxxx...
  过期时间: 3600秒

[测试 2/3] 验证Access Token...
✓ Access Token验证成功!
  API响应正常

[测试 3/3] 生成Widget Token...
✓ Widget Token生成成功!
  Token ID: xxxxxxxxxxxxxxxx...
  Property ID: store_test_001
  PMS名称: HostHub
```

**如果失败**：
- 检查网络连接
- 确认Su API凭证是否正确
- 查看错误信息并提供给我

---

### 测试2：后端服务启动检查

**操作步骤**：
1. 打开新的终端窗口
2. 导航到server目录：
   ```bash
   cd d:\project\住房管理jap\server
   ```
3. 启动后端服务：
   ```bash
   ./mvnw.cmd spring-boot:run
   ```

**检查要点**：
- [ ] 服务成功启动，监听端口8092
- [ ] 日志中没有Su API配置错误
- [ ] 数据库连接成功
- [ ] 所有Bean初始化成功

**预期日志输出**：
```
... Started DemoApplication in X.XXX seconds
... Tomcat started on port(s): 8092 (http)
```

**关键配置加载日志**（可能在启动日志中看到）：
```
... SuApiConfig initialized with base URL: https://connect-sandbox.su-api.com
```

**如果失败**：
- 查看完整的错误堆栈
- 检查环境变量是否正确加载
- 确认端口8092未被占用

---

### 测试3：前端服务启动检查

**操作步骤**：
1. 打开新的终端窗口
2. 导航到client目录：
   ```bash
   cd d:\project\住房管理jap\client
   ```
3. 启动前端服务：
   ```bash
   bun run dev
   ```

**检查要点**：
- [ ] 服务成功启动
- [ ] 监听端口8091
- [ ] 没有TypeScript编译错误
- [ ] 没有依赖缺失错误

**预期输出**：
```
VITE vX.X.X  ready in XXX ms

➜  Local:   http://localhost:8091/
➜  Network: use --host to expose
```

**如果失败**：
- 运行 `bun install` 安装依赖
- 检查是否有TypeScript错误

---

### 测试4：登录系统并访问渠道管理

**操作步骤**：
1. 打开Chrome或Edge浏览器
2. 访问：`http://localhost:8091`
3. 使用测试账号登录（或注册新账号）
4. 登录成功后，从侧边栏进入"渠道管理"页面

**检查要点**：
- [ ] 页面正常加载
- [ ] 看到"OTA渠道"标签页
- [ ] 能看到Airbnb和Booking.com的渠道卡片
- [ ] 卡片显示"未设置"状态

**预期界面**：
- 渠道卡片显示OTA的logo
- 状态显示为"未设置"（灰色圆点）
- 有"配置"按钮

**打开浏览器开发者工具**：
- 按F12打开开发者工具
- 切换到"Console"标签
- 确保没有JavaScript错误

---

### 测试5：点击配置按钮查看须知对话框

**操作步骤**：
1. 在OTA渠道列表中，找到Airbnb卡片
2. 点击"配置"按钮

**检查要点**：
- [ ] 弹出"开始Airbnb直连"对话框
- [ ] 对话框显示完整的直连须知内容
- [ ] 对话框底部有"取消"和"同意并开始授权"按钮

**预期界面**：
```
┌─────────────────────────────────────────┐
│ 开始Airbnb直连                      [X] │
├─────────────────────────────────────────┤
│                                         │
│ 完成Airbnb账号授权后，即可开始直连。     │
│ 点击下一步，代表您已经阅读并同意         │
│ 《Airbnb直连须知》。                    │
│                                         │
│ [Airbnb Logo]                          │
│                                         │
│ ===== Airbnb直连须知 =====              │
│ (须知内容滚动显示)                      │
│                                         │
├─────────────────────────────────────────┤
│           [取消] [同意并开始授权]        │
└─────────────────────────────────────────┘
```

**如果失败**：
- 检查浏览器控制台是否有错误
- 确认Vue组件是否正确加载

---

### 测试6：启动Su Widget连接流程（核心测试）

**操作步骤**：
1. 在须知对话框中，点击"同意并开始授权"按钮

**检查要点**：
- [ ] 须知对话框关闭
- [ ] 弹出新的"连接到 Airbnb"对话框
- [ ] 显示"正在加载连接向导..."和loading动画
- [ ] Su Widget脚本开始加载

**预期界面变化**：

**阶段1 - Loading状态**：
```
┌─────────────────────────────────────────┐
│ 连接到 Airbnb                       [X] │
├─────────────────────────────────────────┤
│                                         │
│            [Loading图标]                │
│         正在加载连接向导...              │
│                                         │
├─────────────────────────────────────────┤
│                        [关闭]           │
└─────────────────────────────────────────┘
```

**阶段2 - Widget加载成功**：
```
┌─────────────────────────────────────────┐
│ 连接到 Airbnb                       [X] │
├─────────────────────────────────────────┤
│                                         │
│   [Su Widget渠道映射界面]               │
│   - 显示OTA渠道选择                     │
│   - 显示房型映射配置                    │
│                                         │
├─────────────────────────────────────────┤
│                        [关闭]           │
└─────────────────────────────────────────┘
```

**浏览器控制台检查**：
打开F12开发者工具，切换到Network标签，应该看到：

1. **API请求**：
   - `GET /api/v1/ota-integrations/{id}/su-widget-token`
   - 状态码：200
   - 响应格式：
     ```json
     {
       "success": true,
       "message": "获取Widget Token成功",
       "data": {
         "tokenId": "widget_token_here",
         "propertyId": "encrypted_property_id_here",
         "channelCode": "aM4JjiWOnUx5qS2IT8wHCbVmIWbA9tTD3PFcjnt8M-Y",
         "baseUrl": "https://static.otaswitch.com/JS/script.js"
       }
     }
     ```

2. **Widget脚本加载**：
   - `https://static.otaswitch.com/JS/script.js`
   - 状态码：200

**如果出现错误**：

**错误1：显示"获取连接配置失败"**
- 检查后端日志，查看Su API调用是否成功
- 确认环境变量配置是否正确
- 查看浏览器Network标签中的API请求详情

**错误2：显示"无法加载Su Widget脚本"**
- 检查网络连接
- 确认可以访问 `https://static.otaswitch.com`
- 查看浏览器控制台的CORS错误

**错误3：Widget脚本加载了但没有显示内容**
- 检查浏览器控制台的JavaScript错误
- 确认Widget配置对象是否正确
- 查看`window.SuOtaSwitch`对象是否已加载

---

### 测试7：数据库状态验证

在Widget加载成功后，检查数据库中的记录是否更新：

**操作步骤**：
1. 在数据库客户端中执行查询：
   ```sql
   SELECT
       id,
       name,
       code,
       is_connected,
       su_property_id,
       su_widget_token,
       su_token_expires_at,
       connected_at,
       updated_at
   FROM ota_integrations
   WHERE code = 'AIRBNB';
   ```

**检查要点**：
- [ ] `su_property_id` 已填写（默认：`STORE{storeId}`，例如 `STORE1`）
- [ ] `su_widget_token` 已填充（长字符串）
- [ ] `su_token_expires_at` 已设置（当前时间+1小时）
- [ ] `updated_at` 更新为最新时间

**预期数据**：
```
id: 1
name: Airbnb
code: AIRBNB
is_connected: false (首次加载Widget时还未完成连接)
su_property_id: STORE1
su_widget_token: (长字符串token)
su_token_expires_at: 2025-11-29 17:00:00
connected_at: NULL (完成Widget配置后才会设置)
updated_at: 2025-11-29 16:00:00
```

---

### 测试8：Widget交互测试（可选）

**注意**：这一步取决于Su Widget的实际功能。根据Su API文档，Widget应该提供渠道映射界面。

**操作步骤**（如果Widget成功加载）：
1. 在Widget界面中浏览可用选项
2. 如果有房型映射选项，尝试配置
3. 如果有"完成"或"保存"按钮，点击完成配置

**检查要点**：
- [ ] Widget界面响应正常
- [ ] 可以进行交互操作
- [ ] 完成配置后收到成功反馈

**预期行为**：
- Widget显示渠道配置选项
- 可以选择和映射房型
- 完成后可能触发回调事件

---

## 📊 测试记录表

请在测试过程中填写以下表格：

| 测试项 | 状态 | 备注 |
|-------|------|------|
| 1. Su API凭证验证 | ⬜ 通过 / ⬜ 失败 |  |
| 2. 后端服务启动 | ⬜ 通过 / ⬜ 失败 |  |
| 3. 前端服务启动 | ⬜ 通过 / ⬜ 失败 |  |
| 4. 渠道管理页面访问 | ⬜ 通过 / ⬜ 失败 |  |
| 5. 须知对话框显示 | ⬜ 通过 / ⬜ 失败 |  |
| 6. Widget对话框加载 | ⬜ 通过 / ⬜ 失败 |  |
| 7. Su Widget脚本加载 | ⬜ 通过 / ⬜ 失败 |  |
| 8. Widget Token获取 | ⬜ 通过 / ⬜ 失败 |  |
| 9. 数据库记录更新 | ⬜ 通过 / ⬜ 失败 |  |
| 10. Widget界面显示 | ⬜ 通过 / ⬜ 失败 |  |

---

## 🐛 常见问题排查

### 问题1：后端API调用返回401错误

**现象**：
- 浏览器Network标签显示 `401 Unauthorized`
- 后端日志显示JWT验证失败

**排查步骤**：
1. 确认用户已登录
2. 检查localStorage中是否有有效的JWT token
3. 确认请求头中包含 `Authorization: Bearer {token}`

### 问题2：后端API调用返回500错误

**现象**：
- 浏览器Network标签显示 `500 Internal Server Error`
- 后端日志显示异常堆栈

**排查步骤**：
1. 查看完整的后端错误日志
2. 检查Su API凭证是否正确
3. 确认Su API服务是否可访问
4. 检查数据库连接是否正常

### 问题3：Widget Token获取成功但Widget不显示

**现象**：
- API调用成功返回200
- 控制台没有错误
- Widget容器是空的

**排查步骤**：
1. 检查`window.SuOtaSwitch`对象是否存在
2. 确认Widget配置对象是否正确
3. 查看浏览器控制台是否有JavaScript错误
4. 检查Widget容器的DOM结构

### 问题4：CORS跨域错误

**现象**：
- 浏览器控制台显示CORS错误
- Network标签显示preflight请求失败

**排查步骤**：
1. 检查后端CORS配置
2. 确认Su Widget脚本URL是否正确
3. 查看Su API文档关于CORS的说明

---

## 📝 测试报告模板

完成测试后，请提供以下信息：

### 1. 测试环境
- 操作系统：Windows 10/11
- 浏览器：Chrome/Edge 版本号
- 后端Java版本：
- 前端Node/Bun版本：

### 2. 测试结果概述
- 总测试项：10
- 通过项：X
- 失败项：X

### 3. 失败项详情
（如有失败，请提供）
- 测试项名称：
- 错误现象：
- 错误日志：
- 截图：

### 4. 浏览器控制台截图
- Network标签截图（显示API请求）
- Console标签截图（显示任何错误）

### 5. 数据库查询结果
```sql
-- 执行查询后的结果
```

---

## ✅ 测试完成标准

所有以下条件都满足，表示集成测试通过：

- ✅ Su API凭证验证通过
- ✅ 后端服务正常启动，没有配置错误
- ✅ 前端服务正常启动，没有编译错误
- ✅ 渠道管理页面正常显示
- ✅ 点击"配置"按钮弹出须知对话框
- ✅ 点击"同意并开始授权"后弹出Widget对话框
- ✅ Widget Token API成功调用并返回正确数据
- ✅ Su Widget脚本成功加载（Network标签显示200状态）
- ✅ 数据库中 `su_property_id` 和 `su_widget_token` 字段被正确填充
- ✅ 没有JavaScript错误或异常

如果所有测试通过，恭喜！Su Widget集成功能已经成功实现！🎉
