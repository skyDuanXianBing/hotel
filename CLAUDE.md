# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

这是一个酒店预订管理系统的全栈项目,包含前端Vue 3应用和后端Spring Boot API。主要功能包括房态管理、房型设置、渠道管理、订单处理、账号权限管理等。系统支持多门店管理(Multi-Store/Multi-Tenant架构)。

## 项目结构

```
├── client/          # Vue 3 前端 (TypeScript)
│   ├── src/
│   │   ├── views/      # 页面组件(按功能模块组织)
│   │   ├── stores/     # Pinia状态管理
│   │   ├── utils/      # 工具函数
│   │   ├── types/      # TypeScript类型定义
│   │   ├── api/        # API请求封装
│   │   └── router/     # 路由配置
├── server/          # Spring Boot 后端 (Java 17)
│   ├── src/main/java/server/demo/
│   │   ├── controller/        # REST控制器
│   │   ├── service/           # 业务逻辑服务
│   │   ├── repository/        # JPA数据访问层
│   │   ├── entity/            # JPA实体类
│   │   │   ├── base/          # 基础接口(如StoreScopedEntity)
│   │   │   └── listener/      # JPA实体监听器
│   │   ├── dto/               # 数据传输对象
│   │   ├── enums/             # 枚举类型
│   │   ├── config/            # 配置类
│   │   ├── context/           # 上下文管理(如StoreContextHolder)
│   │   ├── interceptor/       # 拦截器(如StoreContextInterceptor)
│   │   ├── annotation/        # 自定义注解(如@StoreScoped)
│   │   ├── exception/         # 异常类
│   │   └── util/              # 工具类
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
bun run lint:oxlint    # OxLint检查
bun run lint:eslint    # ESLint检查
bun run lint           # 运行所有linter
bun run format         # Prettier格式化
```

### 后端 (server目录)
```bash
# 启动开发服务器 (http://localhost:8092)
./mvnw spring-boot:run

# 编译项目
./mvnw compile

# 运行测试
./mvnw test

# 打包
./mvnw package
```

## 核心架构

### 多门店架构 (Store-Scoped Multi-Tenant)

系统采用门店隔离的多租户架构,允许用户管理多个门店,每个门店的数据完全隔离。

#### 核心组件

1. **@StoreScoped注解** ([annotation/StoreScoped.java](server/src/main/java/server/demo/annotation/StoreScoped.java))
   - 标记Controller或方法需要门店级上下文
   - `required = true`表示必须提供`X-Store-Id`请求头

2. **StoreContextInterceptor** ([interceptor/StoreContextInterceptor.java](server/src/main/java/server/demo/interceptor/StoreContextInterceptor.java))
   - 拦截带有`@StoreScoped`注解的请求
   - 验证用户对门店的访问权限(通过`store_users`表)
   - 将门店上下文存入`StoreContextHolder`

3. **StoreContextHolder** ([context/StoreContextHolder.java](server/src/main/java/server/demo/context/StoreContextHolder.java))
   - 基于ThreadLocal存储当前请求的门店上下文
   - 包含`userId`, `storeId`, `role`信息

4. **StoreScopedEntity接口** ([entity/base/StoreScopedEntity.java](server/src/main/java/server/demo/entity/base/StoreScopedEntity.java))
   - 门店级数据实体必须实现此接口
   - 提供`getStoreId()`和`setStoreId()`方法

5. **StoreScopedEntityListener** ([entity/listener/StoreScopedEntityListener.java](server/src/main/java/server/demo/entity/listener/StoreScopedEntityListener.java))
   - JPA实体监听器
   - 在`@PrePersist`和`@PreUpdate`时自动注入`storeId`
   - 使用方式: `@EntityListeners(StoreScopedEntityListener.class)`

#### 实现新的门店级功能

当添加新的门店级功能时,遵循以下步骤:

1. **实体类**: 实现`StoreScopedEntity`接口,添加`@EntityListeners(StoreScopedEntityListener.class)`
   ```java
   @Entity
   @EntityListeners(StoreScopedEntityListener.class)
   public class YourEntity implements StoreScopedEntity {
       @Column(name = "store_id")
       private Long storeId;

       // 如果从旧架构迁移,将userId标记为废弃
       @Deprecated
       @Column(nullable = true)
       private Long userId;

       @Override
       public Long getStoreId() { return storeId; }

       @Override
       public void setStoreId(Long storeId) { this.storeId = storeId; }
   }
   ```

2. **Repository**: 查询方法添加`storeId`参数,废弃旧的`userId`查询
   ```java
   List<YourEntity> findByStoreId(Long storeId);
   ```

3. **Service**: 使用`StoreContextHolder.getContext().getStoreId()`获取当前门店ID

4. **Controller**: 添加`@StoreScoped`注解,移除`userId`参数
   ```java
   @StoreScoped
   @GetMapping
   public ApiResponse<List<YourDTO>> getList() {
       // 自动获取storeId,无需参数传递
   }
   ```

5. **前端API**: 无需传递`storeId`,通过请求拦截器自动添加`X-Store-Id`请求头

#### 重要注意事项

- **数据库架构变更**: Hibernate的`ddl-auto=update`不会自动将`NOT NULL`改为`NULL`。从旧架构迁移时,需要手动执行SQL:
  ```sql
  ALTER TABLE your_table MODIFY COLUMN user_id BIGINT NULL;
  ```

- **前端请求头**: [request.ts](client/src/utils/request.ts)请求拦截器会自动从`localStorage.currentStore`读取门店ID并添加到`X-Store-Id`请求头

### 后端架构

- **数据库**: MySQL,使用JPA/Hibernate进行ORM映射
- **API设计**: RESTful API,所有接口使用`/api/v1`前缀
- **响应格式**: 统一使用`ApiResponse<T>`包装所有API响应
  ```json
  {
    "success": true,
    "message": "操作描述",
    "data": "实际数据"
  }
  ```
- **安全认证**:
  - Spring Security + JWT进行身份验证
  - BCrypt密码加密(在`SecurityConfig`中配置`PasswordEncoder` Bean)
  - Token存储在Redis中
  - JWT拦截器注入`userId`到request attributes
  - `StoreContextInterceptor`基于`X-Store-Id`请求头建立门店上下文
- **集成服务**:
  - LangChain4j集成阿里千问(DashScope)
  - Spring Mail邮件服务
  - Redis缓存

### 前端架构

- **状态管理**: 使用Pinia进行全局状态管理
- **API请求**: 统一使用`src/utils/request.ts`进行HTTP请求封装
  - Base URL从环境变量`VITE_API_BASE_URL`读取
  - 自动添加JWT Token到`Authorization`请求头
  - 自动添加门店ID到`X-Store-Id`请求头(从localStorage读取)
  - 401响应自动跳转到登录页
- **路由**: Vue Router配置了嵌套路由,支持设置页面的子路由
- **UI组件**: Element Plus + 自定义组件
- **图标**: @element-plus/icons-vue

### 核心业务实体

#### 多门店相关
- **Store**: 门店实体,包含门店基本信息
- **StoreUser**: 门店-用户关联表,实现多用户协作管理同一门店
  - `role`: owner(所有者)、admin(管理员)、member(普通成员)
- **User**: 用户实体,支持多角色关联和账号状态管理
- **Role**: 角色实体,管理系统角色
- **RolePermission**: 角色权限关联,支持细粒度权限控制(模块+操作+房型)

#### 房态管理
- **RoomType**: 房型实体,定义不同类型的房间(门店级)
- **Room**: 房间实体,关联到具体房型(门店级)
- **RoomPrice**: 房价实体,管理房间价格(门店级)
- **PriceChangeHistory**: 价格变更历史(门店级)
- **PricePlan**: 价格计划(门店级)
- **RoomTypePricePlan**: 房型-价格计划关联(门店级)

#### 订单与渠道
- **Reservation**: 预订实体,处理订单和预订信息(门店级)
- **Channel**: 渠道实体,管理不同的预订渠道(门店级)

#### 其他门店级功能
- **AutoMessage**: 自动消息(门店级)
- **QuickReply**: 快捷回复(门店级)
- **CleaningSupply**: 保洁易耗品(门店级)
- **RoomGroup**: 房间分组(门店级)

### 权限管理架构

- **多对多关系**: User ↔ Role ↔ RolePermission
- **权限模块**: 住宿管理、订单管理、渠道、客户管理、统计分析、设置、数据中心、敏感权限
- **权限操作**: 每个模块包含具体操作权限(查看、编辑、删除等)
- **房型权限**: 支持全部房型或指定房型的细粒度控制

## 数据库配置

项目使用MySQL数据库,配置通过环境变量控制:
- `DB_URL`: 数据库连接URL (默认: jdbc:mysql://localhost:3306/booking_system_db)
- `DB_USERNAME`: 数据库用户名 (默认: root)
- `DB_PASSWORD`: 数据库密码 (默认: 空)
- `SERVER_PORT`: 服务器端口 (默认: 8092)

JPA设置为`hibernate.ddl-auto=update`,会自动创建和更新数据库表结构。

**重要**: 从用户级架构迁移到门店级架构时,Hibernate不会自动将`user_id NOT NULL`改为`user_id NULL`,需要手动执行ALTER TABLE命令。

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

所有后端API都使用`/api/v1`前缀。

### 主要API模块

- `/api/v1/health` - 健康检查
- `/api/v1/stores` - 门店管理
- `/api/v1/users` - 用户管理
- `/api/v1/accounts` - 账号管理(支持筛选、批量操作)
- `/api/v1/roles` - 角色管理(支持权限配置)
- `/api/v1/room-types` - 房型管理(门店级)
- `/api/v1/rooms` - 房间管理(门店级)
- `/api/v1/channels` - 渠道管理(门店级)
- `/api/v1/reservations` - 订单管理(门店级)
- `/api/v1/room-prices` - 房价管理(门店级)
- `/api/v1/price-plans` - 价格计划(门店级)

## 设置页面架构

设置页面采用嵌套路由结构,主要模块包括:

### 门店设置 (`/settings/store/`)
- 基本信息、门店详情

### 通用设置 (`/settings/general/`)
- 通知设置、渠道设置、快捷回复、自动化消息

### 账号设置 (`/settings/account/`)
- 账号列表、角色管理

### 房间设置 (`/settings/room/`)
- 房型详情、房间归属、消费项目、房间排序、房间分组、价格计划

### 保洁设置 (`/settings/cleaning/`)
- 设置、易耗品

### 自动入住 (`/settings/auto-checkin/`)
- 自动入住设置

### 第三方集成 (`/settings/third-party/`)
- 定价工具、支付平台

## 开发约定

### 前端

- 使用TypeScript进行类型安全,禁止使用JavaScript
- 所有API请求必须通过`src/utils/request.ts`进行
- 状态管理使用Pinia stores
- 组件按功能模块组织在`views`目录下
- 禁止硬编码API地址,使用环境变量
- 使用`bun`和`bunx`进行依赖管理
- 门店级功能的API调用无需传递`storeId`参数,由请求拦截器自动添加

### 后端

- 遵循Spring Boot标准项目结构
- 使用Jakarta Validation进行数据验证
- Repository继承`JpaRepository`接口
- Service层处理业务逻辑,使用`StoreContextHolder.getContext()`获取门店上下文
- Controller层提供REST API接口,统一返回`ApiResponse<T>`
- 门店级Controller/方法必须添加`@StoreScoped`注解
- 门店级实体必须实现`StoreScopedEntity`接口并使用`@EntityListeners(StoreScopedEntityListener.class)`
- 敏感信息通过环境变量配置
- 密码加密使用`PasswordEncoder` Bean(在`SecurityConfig`中已配置)

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
SERVER_PORT=8092
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
VITE_API_BASE_URL=http://localhost:8092/api/v1
```

## 开发环境启动

1. 启动MySQL数据库 (使用podman或docker)
2. 启动Redis (可选,用于会话管理)
3. 后端: `cd server && ./mvnw spring-boot:run`
4. 前端: `cd client && bun run dev`

## 重要注意事项

- 每次代码修改后必须运行编译检查: 前端`bun run type-check`,后端`./mvnw compile`
- 测试由用户执行,开发时提供清晰的测试步骤说明
- 使用中文与用户沟通
- 复杂任务需要先制定计划并获得用户确认
- 增量开发,每完成一个小步骤都要验证
- 新增依赖需要向用户说明原因并获得批准
- 禁止启动开发服务器,由用户自行启动

## 特殊技术要点

### Spring Security配置

- `SecurityConfig`中已配置`PasswordEncoder` Bean,所有密码加密使用BCrypt
- 当Service层需要注入`PasswordEncoder`时,Spring会自动提供此Bean

### JPA实体关系

- User与Role是多对多关系,通过`user_roles`中间表关联
- Store与User通过`store_users`关联表实现多对多关系
- Role与RolePermission是一对多关系
- 所有实体使用`@PrePersist`和`@PreUpdate`自动管理时间戳
- 门店级实体使用`StoreScopedEntityListener`自动注入`storeId`

### 权限系统设计

- 权限分为模块(PermissionModule)和操作(PermissionAction)两个维度
- 支持房型级别的细粒度权限控制
- 系统角色(`isSystem=true`)不允许删除和修改名称

### 门店级数据隔离

- 所有门店级数据必须包含`store_id`字段
- 前端通过`X-Store-Id`请求头传递当前门店ID
- 后端通过`StoreContextInterceptor`验证权限并建立上下文
- Service层通过`StoreContextHolder`获取当前门店上下文
- JPA实体监听器自动注入`storeId`,避免手动赋值遗漏
