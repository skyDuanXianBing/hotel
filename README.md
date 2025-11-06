# Java Vue 模板项目

这是一个全栈 Java Spring Boot + Vue 3 模板项目，集成了Spring Data JPA进行数据库操作。

## 项目结构

```
├── client/          # Vue 3 前端应用
├── server/          # Spring Boot 后端API
└── database/        # 数据库脚本（可选，支持JPA自动建表）
```

## 技术栈

### 后端
- **Spring Boot 3.5.5** - 基础框架
- **Spring Data JPA** - 数据访问层
- **Hibernate** - JPA实现
- **MySQL** - 生产环境数据库
- **Maven** - 依赖管理

### 前端
- **Vue 3** - 渐进式JavaScript框架
- **TypeScript** - 类型安全
- **Vite** - 构建工具
- **Element Plus** - UI组件库
- **Pinia** - 状态管理

## 功能特性

- ✅ **Code First开发** - JPA自动创建数据库表结构
- ✅ **RESTful API** - 标准化API设计
- ✅ **数据验证** - 基于Jakarta Validation
- ✅ **统一响应格式** - ApiResponse<T>封装
- ✅ **CORS配置** - 支持跨域请求
- ✅ **热重载开发** - 前后端开发服务器

## 环境配置

### 数据库配置

在 `server/src/main/resources/application.properties` 中配置数据库连接：

```properties
# 数据库配置
spring.datasource.url=${DB_URL:jdbc:mysql://localhost:3306/template_db?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=GMT%2B8&allowPublicKeyRetrieval=true&createDatabaseIfNotExist=true}
spring.datasource.username=${DB_USERNAME:root}
spring.datasource.password=${DB_PASSWORD:root123}

# JPA配置 - 支持Code First开发
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
```

### 环境变量配置

您可以通过环境变量覆盖默认配置：

```bash
export DB_URL="jdbc:mysql://localhost:3306/your_db?createDatabaseIfNotExist=true"
export DB_USERNAME="your_username"  
export DB_PASSWORD="your_password"
export SERVER_PORT="8092"
```

### 前端配置

在 `client` 目录下创建 `.env` 文件：

```env
VITE_API_BASE_URL=http://localhost:8092
```

## 快速开始

### 1. 启动数据库（推荐使用Podman/Docker）

```bash
# 使用Podman启动MySQL
podman run -d \
  --name mysql-container \
  -e MYSQL_ROOT_PASSWORD=root123 \
  -p 3306:3306 \
  mysql:latest
```

### 2. 启动后端

```bash
cd server
./mvnw spring-boot:run
```

后端将在 `http://localhost:8092` 启动，JPA会自动创建数据库和表结构。

### 3. 启动前端

```bash
cd client
bun install
bun run dev
```

前端将在 `http://localhost:8091` 启动。

## API 接口

### 系统健康检查

#### GET /api/v1/health

返回系统健康状态

**响应示例：**
```json
{
  "success": true,
  "message": "操作成功",
  "data": {
    "status": "健康运行中"
  }
}
```

### 用户管理API

#### GET /api/v1/users
获取所有用户列表

#### POST /api/v1/users
创建新用户

**请求体：**
```json
{
  "username": "testuser",
  "email": "test@example.com",
  "name": "测试用户"
}
```

#### GET /api/v1/users/{id}
根据ID获取单个用户

#### PUT /api/v1/users/{id}
更新用户信息

#### DELETE /api/v1/users/{id}
删除用户

#### GET /api/v1/users/search?keyword=关键词
搜索用户

#### GET /api/v1/users/recent?days=7
获取最近N天注册的用户

**统一响应格式：**
```json
{
  "success": true,
  "message": "操作描述",
  "data": "实际数据"
}
```

## 数据模型

### User 实体

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键，自增 |
| username | String | 用户名，唯一 |
| email | String | 邮箱，唯一 |
| name | String | 姓名 |
| createdAt | LocalDateTime | 创建时间 |
| updatedAt | LocalDateTime | 更新时间 |

## 开发指南

### 后端开发
1. **实体类** - 在 `server.demo.entity` 包下定义
2. **Repository** - 继承 `JpaRepository` 接口
3. **Service** - 业务逻辑层，调用Repository
4. **Controller** - REST接口层，统一使用 `ApiResponse<T>` 响应格式

### 前端开发
1. **API调用** - 使用 `src/utils/request.ts` 统一封装
2. **状态管理** - 使用 Pinia 管理全局状态
3. **路由** - Vue Router 配置在 `src/router`
4. **组件** - Element Plus UI组件库

### 开发规范
- 后端严格按照RESTful API设计
- 使用Jakarta Validation进行数据验证
- JPA支持Code First，实体变更会自动更新表结构
- 前后端统一使用UTF-8编码
- API接口统一添加 `/api/v1` 前缀