# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

这是一个酒店预订系统的全栈项目,包含前端Vue 3应用和后端Spring Boot API。主要功能包括房态管理、房型设置、渠道管理和订单处理。

## 项目结构

```
├── client/          # Vue 3 前端 (TypeScript)
│   ├── src/
│   │   ├── views/      # 页面组件
│   │   ├── stores/     # Pinia状态管理
│   │   ├── utils/      # 工具函数
│   │   ├── types/      # TypeScript类型定义
│   │   └── router/     # 路由配置
├── server/          # Spring Boot 后端 (Java 17)
│   ├── src/main/java/server/demo/
│   │   ├── controller/ # REST控制器
│   │   ├── service/    # 业务逻辑服务
│   │   ├── repository/ # JPA数据访问层
│   │   ├── entity/     # JPA实体类
│   │   ├── dto/        # 数据传输对象
│   │   ├── enums/      # 枚举类型
│   │   └── config/     # 配置类
└── server/src/main/resources/ # 配置文件
```

## 开发命令

### 前端 (client目录)
```bash
# 开发服务器 (http://localhost:8091)
bun run dev

# 类型检查
bun run type-check

# 构建生产版本
bun run build

# 代码检查和格式化
bun run lint          # 运行所有linter (oxlint + eslint)
bun run format        # 格式化代码
bun run lint:oxlint   # 仅运行oxlint
bun run lint:eslint   # 仅运行eslint
```

### 后端 (server目录)
```bash
# 启动开发服务器 (http://localhost:8082)
./mvnw spring-boot:run

# 编译项目
./mvnw compile

# 运行测试
./mvnw test

# 打包
./mvnw package
```

## 核心架构

### 后端架构
- **数据库**: MySQL,使用JPA/Hibernate进行ORM映射
- **API设计**: RESTful API,所有接口使用`/api/v1`前缀
- **响应格式**: 统一使用`ApiResponse<T>`包装所有API响应
- **数据初始化**: `DataInitializer`组件负责系统启动时的数据初始化
- **CORS配置**: `CorsConfig`允许跨域请求
- **安全认证**:
  - 使用Spring Security + JWT进行身份验证
  - BCrypt密码加密
  - Token存储在Redis中
- **集成服务**:
  - LangChain4j集成阿里千问(DashScope)
  - Spring Mail邮件服务
  - Redis缓存

### 前端架构
- **状态管理**: 使用Pinia进行全局状态管理
- **API请求**: 统一使用`src/utils/request.ts`进行HTTP请求封装
  - Base URL从环境变量`VITE_API_BASE_URL`读取
  - 自动添加JWT Token到请求头
  - 401响应自动跳转到登录页
- **路由**: Vue Router配置了嵌套路由,支持设置页面的子路由
- **UI组件**: Element Plus + 自定义组件
- **图标**: @element-plus/icons-vue

### 核心业务实体
- **User**: 用户实体
- **RoomType**: 房型实体,定义不同类型的房间
- **Room**: 房间实体,关联到具体房型
- **Channel**: 渠道实体,管理不同的预订渠道
- **Reservation**: 预订实体,处理订单和预订信息

## 数据库配置

项目使用MySQL数据库,配置通过环境变量控制:
- `DB_URL`: 数据库连接URL (默认: jdbc:mysql://localhost:3306/booking_system_db)
- `DB_USERNAME`: 数据库用户名 (默认: root)
- `DB_PASSWORD`: 数据库密码 (默认: 空)
- `SERVER_PORT`: 服务器端口 (默认: 8082)

JPA设置为`hibernate.ddl-auto=update`,会自动创建和更新数据库表结构。

## API规范

### 统一响应格式
所有API响应都使用`ApiResponse<T>`格式:
```json
{
  "success": true,
  "message": "操作描述",
  "data": "实际数据"
}
```

### API前缀
所有后端API都使用`/api/v1`前缀,例如:
- `/api/v1/health` - 健康检查
- `/api/v1/users` - 用户管理
- `/api/v1/room-types` - 房型管理
- `/api/v1/rooms` - 房间管理

## 设置页面架构

设置页面采用嵌套路由结构,主要模块包括:

### 门店设置 (`/settings/store/`)
- 基本信息: `/settings/store/basic-info`
- 门店详情: `/settings/store/details`

### 通用设置 (`/settings/general/`)
- 通知设置: `/settings/general/notification`
- 渠道设置: `/settings/general/channel`
- 快捷回复: `/settings/general/quick-reply`
- 自动化消息: `/settings/general/auto-message`

### 保洁设置 (`/settings/cleaning/`)
- 设置: `/settings/cleaning/settings`
- 易耗品: `/settings/cleaning/supplies`

### 自动入住 (`/settings/auto-checkin/`)
- 自动入住设置: `/settings/auto-checkin/settings`

### 第三方集成 (`/settings/third-party/`)
- 定价工具: `/settings/third-party/pricing-tools`
- 支付平台: `/settings/third-party/payment-platforms`

## 开发约定

### 前端
- 使用TypeScript进行类型安全,禁止使用JavaScript
- 所有API请求必须通过`src/utils/request.ts`进行
- 状态管理使用Pinia stores
- 组件按功能模块组织在`views`目录下
- 禁止硬编码API地址,使用环境变量
- 使用`bun`和`bunx`进行依赖管理

### 后端
- 遵循Spring Boot标准项目结构
- 使用Jakarta Validation进行数据验证
- Repository继承`JpaRepository`接口
- Service层处理业务逻辑
- Controller层提供REST API接口,统一返回`ApiResponse<T>`
- 敏感信息通过环境变量配置

### 代码风格
- 前端使用ESLint + Prettier + OxLint进行代码检查
- 后端遵循Java标准命名约定
- 数据库字段使用下划线命名,Java实体使用驼峰命名
- 变量/函数使用camelCase
- 常量使用UPPER_SNAKE_CASE
- 类/组件使用PascalCase

## 环境变量配置

### 后端环境变量
```bash
DB_URL=jdbc:mysql://localhost:3306/booking_system_db
DB_USERNAME=root
DB_PASSWORD=
SERVER_PORT=8082
JWT_SECRET=your-secret-key
DASH_SCOPE_API_KEY=your-api-key
REDIS_HOST=localhost
REDIS_PORT=6379
MAIL_HOST=smtp.qq.com
MAIL_USERNAME=your-email
MAIL_PASSWORD=your-password
```

### 前端环境变量 (client/.env)
```bash
VITE_API_BASE_URL=http://localhost:8082/api/v1
```

## 开发环境启动

1. 启动MySQL数据库 (使用podman或docker)
2. 启动Redis (可选,用于会话管理)
3. 后端: `cd server && ./mvnw spring-boot:run`
4. 前端: `cd client && bun run dev`

## 重要注意事项

- 每次代码修改后必须运行类型检查: `bun run type-check`
- 测试由用户执行,开发时提供清晰的测试步骤说明
- 使用中文与用户沟通
- 复杂任务需要先制定计划并获得用户确认
- 增量开发,每完成一个小步骤都要验证
