# Su API 集成配置指南

## 沙盒账号信息

### 基本信息
- **PMS**: SaitoShoji Co Ltd
- **产品**: Su
- **用户名**: chochoketsu01@gmail.com
- **沙盒环境**: https://connect-sandbox.su-api.com
- **API文档**: [Su API Documentation](https://suissu.gitbook.io/su-api-documentation)

### API凭证
- **客户端ID**: `c2FpdG9zaG9qaWNvbHRkLnN1aXNzdS5jb20=`
- **客户端密钥**: `NVI4NWo1Yzc6WWl5OHY2OVc`
- **访问密钥**: `b040ZkdWQlI6RDdWY2xYelc`

## 环境变量配置

### 方式一：创建 `.env` 文件（推荐）

在 `server` 目录下创建 `.env` 文件：

```bash
# Su Channel Manager API配置
SU_API_BASE_URL=https://connect-sandbox.su-api.com
SU_CLIENT_ID=c2FpdG9zaG9qaWNvbHRkLnN1aXNzdS5jb20=
SU_CLIENT_SECRET=NVI4NWo1Yzc6WWl5OHY2OVc
SU_PMS_NAME=HostHub
```

### 方式二：系统环境变量配置

**Windows (PowerShell)**：
```powershell
$env:SU_API_BASE_URL="https://connect-sandbox.su-api.com"
$env:SU_CLIENT_ID="c2FpdG9zaG9qaWNvbHRkLnN1aXNzdS5jb20="
$env:SU_CLIENT_SECRET="NVI4NWo1Yzc6WWl5OHY2OVc"
$env:SU_PMS_NAME="HostHub"
```

**Linux/macOS (Bash)**：
```bash
export SU_API_BASE_URL="https://connect-sandbox.su-api.com"
export SU_CLIENT_ID="c2FpdG9zaG9qaWNvbHRkLnN1aXNzdS5jb20="
export SU_CLIENT_SECRET="NVI4NWo1Yzc6WWl5OHY2OVc"
export SU_PMS_NAME="HostHub"
```

## 验证配置

### 1. 启动后端服务

```bash
cd server
./mvnw.cmd spring-boot:run
```

### 2. 检查日志

启动时应该能看到Su API配置被成功加载。

### 3. 测试API连接

可以使用以下curl命令测试Su API连接：

```bash
curl -X POST "https://connect-sandbox.su-api.com/SUAPI/jservice/auth/generate-access-token" \
  -H "client-id: c2FpdG9zaG9qaWNvbHRkLnN1aXNzdS5jb20=" \
  -H "client-secret: NVI4NWo1Yzc6WWl5OHY2OVc" \
  -H "Content-Type: application/json"
```

预期响应：
```json
{
  "status": "SUCCESS",
  "data": {
    "token": "sandbox_xxxxxxxxxxxxxxxx",
    "expiresIn": 3600
  }
}
```

## Widget集成测试流程

### 前置条件
1. ✅ 后端服务已启动
2. ✅ 前端服务已启动 (`cd client && bun run dev`)
3. ✅ 环境变量已配置
4. ✅ 数据库已初始化（OTA Integration表已有Airbnb和Booking.com记录）

### 测试步骤

1. **访问渠道管理页面**
   - 打开浏览器访问：`http://localhost:8091`
   - 登录系统
   - 进入"渠道管理"页面

2. **选择OTA渠道**
   - 在OTA渠道列表中，点击 Airbnb 或 Booking.com 的"配置"按钮

3. **同意并开始授权**
   - 在弹出的"直连须知"对话框中
   - 阅读须知内容
   - 点击"同意并开始授权"按钮

4. **加载Su Widget**
   - 应该弹出新的对话框"连接到 [Airbnb/Booking.com]"
   - 显示"正在加载连接向导..."的loading动画
   - Widget脚本加载成功后，显示Su的渠道映射界面

5. **完成渠道映射**
   - 在Widget中选择要连接的OTA账号
   - 映射房型到OTA房源
   - 完成配置

6. **验证结果**
   - 连接成功后，应该看到成功提示消息
   - OTA渠道卡片的状态变为"已设置"
   - 数据库 `ota_integrations` 表中的相关记录被更新：
     - `is_connected` = true
     - `su_property_id` = "store_{storeId}"
     - `su_widget_token` 已填充
     - `connected_at` 时间戳已记录

## 常见问题排查

### 问题1：Widget加载失败
**现象**：显示"无法加载Su Widget脚本"

**解决方案**：
1. 检查网络连接
2. 确认可以访问 `https://static.otaswitch.com/JS/script.js`
3. 检查浏览器控制台是否有CORS错误

### 问题2：获取Widget Token失败
**现象**：显示"获取连接配置失败"

**解决方案**：
1. 检查环境变量是否正确配置
2. 查看后端日志，确认Su API调用是否成功
3. 验证客户端ID和密钥是否正确

### 问题3：Widget初始化失败
**现象**：Widget脚本加载成功，但没有显示内容

**解决方案**：
1. 检查浏览器控制台是否有JavaScript错误
2. 确认Widget配置对象是否正确
3. 验证propertyId、token、channelCode是否有效

## API端点说明

### 获取Widget Token
```
GET /api/v1/ota-integrations/{id}/su-widget-token
```

**请求头**：
- `Authorization: Bearer {jwt-token}`
- `X-Store-Id: {store-id}`

**响应**：
```json
{
  "success": true,
  "message": "获取Widget Token成功",
  "data": {
    "tokenId": "widget_token_here",
    "propertyId": "encrypted_property_id",
    "channelCode": "encrypted_channel_code",
    "baseUrl": "https://static.otaswitch.com/JS/script.js"
  }
}
```

## 迁移到生产环境

完成开发和测试后，需要进行以下操作：

1. **认证流程**
   - 联系Su安排认证
   - 完成两步认证测试

2. **生产环境配置**
   - 更新 `SU_API_BASE_URL` 为生产环境地址
   - Su会提供生产环境的客户端ID和密钥
   - 更新环境变量

3. **验证**
   - 在生产环境重新测试完整流程
   - 确认实际OTA连接正常工作

## 支持与文档

- **Su API文档**: https://suissu.gitbook.io/su-api-documentation
- **Su外联网**: 使用提供的用户名和密码登录
- **技术支持**: 通过Su外联网提交支持请求
