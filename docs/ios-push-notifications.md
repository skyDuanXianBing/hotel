# iOS / Android 手机推送通知配置指南

本文档覆盖移动端推送的完整启用步骤。代码已全量就绪，只需补凭证即可真机使用。

## 功能范围

| 事件 | 触发点 | 推送内容 | 点击跳转 |
| --- | --- | --- | --- |
| 客人新聊天消息 | Su webhook 落库（`SuMessagingRealtimeGateway`） | 标题=客人名，正文=消息摘要（120 字截断） | 会话详情 `/tabs/messages/:threadId` |
| 新订单 / 更改订单 / 取消订单 | `OrderNotificationDispatchService` | 与 web 弹窗完全一致的标题+正文 | 订单详情 `/tabs/orders/reservations/:id` |
| 客人提交住宿者表格 | `PublicRegistrationService.submit` | TASK 通知标题+正文 | 表格审核详情 `/tabs/reviews/:formId` |

- App 未打开 / 在后台：APNs 直接弹系统通知（声音+横幅）。
- App 在前台：iOS 由系统按 `presentationOptions` 弹横幅；Android 用本地通知兜底弹横幅。
- 接收人遵循 App 内通知设置：聊天看"聊天弹框提醒"，订单/任务看"订单弹框提醒"（`notification_settings`）。
- 住宿者表格提交同时创建 TASK 类型站内通知（系统通知组可见），接收人为有表格审核权限的在职用户（无人有权限时回退全体在职用户）。

## 一、服务端配置（APNs）

需要 Apple Developer 账号的 **APNs Auth Key（.p8）**：Apple Developer → Certificates, Identifiers & Profiles → Keys → 新建 APNs Key。

在 `server/.env`（本地）或生产环境变量中配置：

```properties
PUSH_ENABLED=true
PUSH_APNS_ENABLED=true
PUSH_APNS_TEAM_ID=<Team ID，开发者账号右上角 10 位>
PUSH_APNS_KEY_ID=<.p8 文件的 Key ID，10 位>
# 二选一：文件路径，或文件内容 Base64（单行，适合 .env）
PUSH_APNS_AUTH_KEY_PATH=/secure/path/AuthKey_XXXXXXXXXX.p8
# base64 生成：base64 -i AuthKey_XXXXXXXXXX.p8 | tr -d '\n'
PUSH_APNS_AUTH_KEY_BASE64=
PUSH_APNS_TOPIC=jp.thehost.pms
# Xcode 直装 Debug 包 = false（sandbox）；TestFlight / App Store = true
PUSH_APNS_PRODUCTION=false
```

- 未配置或开关为 false 时推送整体跳过，不影响业务主流程。
- APNs 返回 BadDeviceToken/Unregistered 的设备会自动停用。
- `.p8` 文件不得入库；`.env` 已在 gitignore。

数据库：`V066__push_device_tokens.sql`（本地 `ddl-auto=update` 自动建表，生产跑 Flyway 迁移）。

## 二、iOS 真机运行

推送必须在**真机**测试（模拟器收不到 APNs）。

1. `cd ios && npm run build && npx cap sync ios`（或 `./rebuild-ios.command`）。
2. Xcode 打开 `ios/ios/App/App.xcodeproj`。
3. Signing & Capabilities：选择开发者 Team（推送需要付费账号的描述文件），确认已存在 **Push Notifications** 能力（工程已写入 `App.entitlements`，aps-environment=development；发布时 Xcode 会自动切 production）。
4. 真机运行 → 登录并选定门店 → 首次会弹系统通知授权 → 允许后 App 自动把设备令牌上传到 `POST /api/v1/push/devices`。
5. 验证：触发一条客人消息/订单/表格提交 → 锁屏状态应收到系统弹窗；前台打开 App 时也会弹系统横幅。

## 三、Android（预留，待启用）

安卓工程已生成（`ios/android/`），FCM 接入点已全部预留：

1. Firebase 控制台建项目 → 添加 Android 应用（包名 `jp.thehost.pms`）→ 下载 `google-services.json` 放到 `ios/android/app/`（已 gitignore，构建脚本检测到文件会自动应用 google-services 插件）。
2. 服务端实现 FCM 发送：`PushDispatchService` 中 `PushPlatform.ANDROID` 分支已标注"FCM 发送位"（接入 Firebase Admin SDK 或 FCM HTTP v1）。
3. App 端代码无需改动：注册/上传/前台横幅/点击路由均已按平台双端实现。

### 打 APK 的前提

本机需要 Android SDK（任选其一）：
- 装 Android Studio（推荐，含 SDK 管理器，后续签名/模拟器都方便）；
- 或 `brew install --cask android-commandlinetools` 后用 sdkmanager 装 platform/build-tools。

装好 SDK 后打 debug 包：

```bash
cd ios && ./rebuild-android.command   # 构建前端 + 同步
cd android && ./gradlew assembleDebug # 产物在 android/app/build/outputs/apk/debug/
```

命令行工具路线（无 Android Studio）：

```bash
brew install --cask android-commandlinetools
yes | sdkmanager --licenses
sdkmanager "platform-tools" "platforms;android-35" "build-tools;35.0.0"
# 在 ios/android/local.properties 写入：sdk.dir=/opt/homebrew/share/android-commandlinetools
```

debug 包可直接侧载安装测试；上架 Google Play 的 release/AAB 还需要配置签名 keystore。

## 四、本地联调

- 聊天消息：用 `channel-simulator/` 发 webhook（`npm run verify:local-e2e`，需 PMS 与模拟器已启动），客人消息落库即触发推送。
- 住宿者表格：通过公开链接 `/api/public/registration/{orderNumber}/submit` 提交后触发 TASK 通知+推送。
- 设备令牌注册/解绑：`POST/DELETE /api/v1/push/devices`（需登录态 + `X-Store-Id`）。

## 五、注意事项

- `PUSH_APNS_PRODUCTION` 必须与打包方式匹配，否则 APNs 返回 BadDeviceToken。
- App 通知设置里关掉"聊天弹框提醒"的用户不会收到聊天推送（订单/任务同理），与 web 行为一致。
- 已知版本警告：`@capacitor/core@8.5.0` 与 `@capacitor/ios@8.3.0` 次版本不一致（历史遗留），功能不受影响，升级 iOS 包时可一并拉齐。
