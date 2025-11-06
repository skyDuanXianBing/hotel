# 项目部署文档

本文档提供完整的环境配置和部署指南,帮助你在任何电脑上快速启动这个酒店预订系统。

## 目录
- [系统要求](#系统要求)
- [环境准备](#环境准备)
- [数据库配置](#数据库配置)
- [后端配置](#后端配置)
- [前端配置](#前端配置)
- [启动项目](#启动项目)
- [常见问题](#常见问题)

---

## 系统要求

### 操作系统
- Windows 10/11
- macOS 10.15+
- Linux (Ubuntu 20.04+, CentOS 7+)

### 必需软件
- **Java 17+** (后端运行环境)
- **Node.js 20.19.0+** 或 **22.12.0+** (前端构建工具)
- **Bun** (前端包管理器)
- **MySQL 8.0+** (数据库)
- **Git** (代码管理)

### 可选软件
- **Redis 6.0+** (会话管理和缓存,推荐但非必需)
- **Podman** 或 **Docker** (容器化运行数据库,推荐)

---

## 环境准备

### 1. 安装 Java 17



#### Linux (Ubuntu/Debian)
```bash
# 安装 OpenJDK 17
sudo apt update
sudo apt install openjdk-17-jdk -y

# 验证安装
java -version
```

#### Linux (CentOS/RHEL)
```bash
# 安装 OpenJDK 17
sudo yum install java-17-openjdk-devel -y

# 验证安装
java -version
```

---

### 2. 安装 Node.js


#### Linux
```bash
# 使用 NodeSource 安装 Node.js 20
curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
sudo apt-get install -y nodejs

# 验证安装
node -v
npm -v
```

---

### 3. 安装 Bun

#### Windows/macOS/Linux
```bash
# 使用官方安装脚本
curl -fsSL https://bun.sh/install | bash

# 或使用 npm 安装
npm install -g bun

# 验证安装
bun -v
```

#### Windows (PowerShell)
```powershell
# 使用 PowerShell 安装
powershell -c "irm bun.sh/install.ps1 | iex"

# 验证安装
bun -v
```

---

### 4. 安装 MySQL

#### 使用 Podman/Docker (推荐)

**安装 Podman (Windows/macOS/Linux):**
- Windows: 下载 [Podman Desktop](https://podman-desktop.io/downloads)
- macOS: `brew install podman`
- Linux: `sudo apt install podman` 或 `sudo yum install podman`

**启动 MySQL 容器:**
```bash
# 使用 Podman
podman run -d \
  --name mysql-booking-system \
  -e MYSQL_ROOT_PASSWORD=your_password \
  -e MYSQL_DATABASE=booking_system_db \
  -p 3306:3306 \
  mysql:8.0

# 使用 Docker (如果没有 Podman)
docker run -d \
  --name mysql-booking-system \
  -e MYSQL_ROOT_PASSWORD=your_password \
  -e MYSQL_DATABASE=booking_system_db \
  -p 3306:3306 \
  mysql:8.0
```

**查看容器状态:**
```bash
podman ps
# 或
docker ps
```

#### 直接安装 MySQL

**Windows:**
1. 下载 [MySQL Installer](https://dev.mysql.com/downloads/installer/)
2. 运行安装程序,选择 "Server only" 或 "Full"
3. 设置 root 密码
4. 完成安装

**macOS:**
```bash
brew install mysql@8.0
brew services start mysql@8.0
mysql_secure_installation
```

**Linux (Ubuntu/Debian):**
```bash
sudo apt update
sudo apt install mysql-server -y
sudo systemctl start mysql
sudo mysql_secure_installation
```

**创建数据库:**
```bash
mysql -u root -p
```
```sql
CREATE DATABASE booking_system_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
EXIT;
```

---

### 5. 安装 Redis (可选)

#### 使用 Podman/Docker (推荐)
```bash
# 使用 Podman
podman run -d \
  --name redis-booking-system \
  -p 6379:6379 \
  redis:7-alpine

# 使用 Docker
docker run -d \
  --name redis-booking-system \
  -p 6379:6379 \
  redis:7-alpine
```

#### 直接安装 Redis

**macOS:**
```bash
brew install redis
brew services start redis
```

**Linux (Ubuntu/Debian):**
```bash
sudo apt update
sudo apt install redis-server -y
sudo systemctl start redis-server
sudo systemctl enable redis-server
```

**Windows:**
1. 从 [Redis for Windows](https://github.com/microsoftarchive/redis/releases) 下载
2. 解压并运行 `redis-server.exe`

---

## 数据库配置

### 创建数据库用户 (可选,推荐)
```sql
-- 以 root 用户登录 MySQL
mysql -u root -p

-- 创建专用用户
CREATE USER 'booking_user'@'localhost' IDENTIFIED BY 'booking_password';
GRANT ALL PRIVILEGES ON booking_system_db.* TO 'booking_user'@'localhost';
FLUSH PRIVILEGES;
EXIT;
```

### 验证数据库连接
```bash
mysql -u booking_user -p -D booking_system_db
# 或使用 root
mysql -u root -p -D booking_system_db
```

---

## 后端配置

### 1. 克隆项目
```bash
# 克隆仓库
git clone <your-repository-url>
cd 住房管理jap/server
```

### 2. 配置环境变量

创建 `server/.env` 文件或设置系统环境变量:

**Windows (PowerShell):**
```powershell
$env:DB_URL="jdbc:mysql://localhost:3306/booking_system_db?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true"
$env:DB_USERNAME="root"
$env:DB_PASSWORD="your_password"
$env:SERVER_PORT="8082"
$env:JWT_SECRET="your-256-bit-secret-key-for-jwt-token-please-change-in-production"
$env:REDIS_HOST="localhost"
$env:REDIS_PORT="6379"
```

**macOS/Linux (Bash/Zsh):**
```bash
export DB_URL="jdbc:mysql://localhost:3306/booking_system_db?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true"
export DB_USERNAME="root"
export DB_PASSWORD="your_password"
export SERVER_PORT="8082"
export JWT_SECRET="your-256-bit-secret-key-for-jwt-token-please-change-in-production"
export REDIS_HOST="localhost"
export REDIS_PORT="6379"
```

**或者直接编辑 `server/src/main/resources/application.properties`:**
```properties
# 数据库配置
spring.datasource.url=jdbc:mysql://localhost:3306/booking_system_db?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=your_password

# 服务器端口
server.port=8082

# JWT配置
jwt.secret=your-256-bit-secret-key-for-jwt-token-please-change-in-production

# Redis配置
spring.data.redis.host=localhost
spring.data.redis.port=6379
```

### 3. 邮件配置 (可选)

如果需要邮件功能,配置邮件服务器:
```properties
# QQ邮箱示例
spring.mail.host=smtp.qq.com
spring.mail.port=587
spring.mail.username=your_email@qq.com
spring.mail.password=your_authorization_code
```

> **注意**: QQ邮箱需要使用授权码而不是密码。在QQ邮箱设置中开启SMTP服务获取授权码。

### 4. 编译和测试后端
```bash
cd server

# Windows
.\mvnw.cmd clean compile

# macOS/Linux
./mvnw clean compile

# 运行测试
./mvnw test
```

---

## 前端配置

### 1. 进入前端目录
```bash
cd ../client
```

### 2. 创建环境变量文件

创建 `client/.env` 文件:
```env
VITE_API_BASE_URL=http://localhost:8082/api/v1
```

**开发环境 (`client/.env.development`):**
```env
VITE_API_BASE_URL=http://localhost:8082/api/v1
```

**生产环境 (`client/.env.production`):**
```env
VITE_API_BASE_URL=https://your-production-domain.com/api/v1
```

### 3. 安装依赖
```bash
# 使用 Bun 安装依赖
bun install

# 如果遇到问题,可以清理后重新安装
rm -rf node_modules bun.lockb
bun install
```

### 4. 验证前端构建
```bash
# 类型检查
bun run type-check

# 代码检查
bun run lint
```

---

## 启动项目

### 方式一: 分别启动 (推荐用于开发)

#### 1. 启动数据库和 Redis (如果使用容器)
```bash
# 启动 MySQL
podman start mysql-booking-system

# 启动 Redis (可选)
podman start redis-booking-system

# 查看容器状态
podman ps
```

#### 2. 启动后端
```bash
cd server

# Windows
.\mvnw.cmd spring-boot:run

# macOS/Linux
./mvnw spring-boot:run
```

**后端启动成功标志:**
- 控制台输出: `Started DemoApplication in X.XXX seconds`
- 访问健康检查: http://localhost:8082/api/v1/health

#### 3. 启动前端 (新开一个终端)
```bash
cd client
bun run dev
```

**前端启动成功标志:**
- 控制台输出: `Local: http://localhost:8091/`
- 访问前端: http://localhost:8091

---

### 方式二: 使用脚本启动

#### Windows (PowerShell)
创建 `start.ps1`:
```powershell
# 启动后端
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd server; .\mvnw.cmd spring-boot:run"

# 等待后端启动
Start-Sleep -Seconds 10

# 启动前端
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd client; bun run dev"

Write-Host "项目启动中..."
Write-Host "后端: http://localhost:8082/api/v1/health"
Write-Host "前端: http://localhost:8091"
```

运行:
```powershell
.\start.ps1
```

#### macOS/Linux (Bash)
创建 `start.sh`:
```bash
#!/bin/bash

# 启动后端
cd server
./mvnw spring-boot:run &
BACKEND_PID=$!

# 等待后端启动
echo "等待后端启动..."
sleep 10

# 启动前端
cd ../client
bun run dev &
FRONTEND_PID=$!

echo "项目启动中..."
echo "后端: http://localhost:8082/api/v1/health"
echo "前端: http://localhost:8091"
echo "后端进程ID: $BACKEND_PID"
echo "前端进程ID: $FRONTEND_PID"

# 等待进程
wait
```

运行:
```bash
chmod +x start.sh
./start.sh
```

---

## 验证部署

### 1. 检查后端
```bash
# 健康检查
curl http://localhost:8082/api/v1/health

# 预期响应
{
  "success": true,
  "message": "操作成功",
  "data": {
    "status": "健康运行中"
  }
}
```

### 2. 检查前端
打开浏览器访问: http://localhost:8091

应该能看到登录页面

### 3. 检查数据库连接
查看后端日志,应该有类似输出:
```
Hibernate: create table if not exists ...
```

### 4. 检查 Redis (如果配置)
```bash
redis-cli ping
# 应该返回: PONG
```

---

## 生产环境部署

### 1. 构建前端
```bash
cd client
bun run build
```

构建产物在 `client/dist/` 目录

### 2. 打包后端
```bash
cd server
./mvnw clean package -DskipTests
```

生成的 JAR 文件在 `server/target/demo-0.0.1-SNAPSHOT.jar`

### 3. 运行生产版本

**后端:**
```bash
java -jar server/target/demo-0.0.1-SNAPSHOT.jar
```

**前端:**
使用 Nginx 或其他 Web 服务器托管 `client/dist/` 目录

**Nginx 配置示例:**
```nginx
server {
    listen 80;
    server_name your-domain.com;

    # 前端静态文件
    location / {
        root /path/to/client/dist;
        try_files $uri $uri/ /index.html;
    }

    # 后端 API 代理
    location /api/ {
        proxy_pass http://localhost:8082;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

---

## 常见问题

### 1. Maven 下载依赖失败
**问题**: Maven 下载速度慢或失败

**解决方案**: 配置国内镜像源
编辑 `~/.m2/settings.xml` (如果不存在则创建):
```xml
<settings>
  <mirrors>
    <mirror>
      <id>aliyun</id>
      <mirrorOf>central</mirrorOf>
      <name>Aliyun Maven</name>
      <url>https://maven.aliyun.com/repository/central</url>
    </mirror>
  </mirrors>
</settings>
```

### 2. 端口被占用
**问题**: `Address already in use: bind`

**解决方案**:
```bash
# Windows - 查找占用端口的进程
netstat -ano | findstr :8082
taskkill /PID <进程ID> /F

# macOS/Linux
lsof -i :8082
kill -9 <进程ID>

# 或修改端口
export SERVER_PORT=8083  # 后端
# 修改 client/vite.config.ts 中的 server.port
```

### 3. 数据库连接失败
**问题**: `Communications link failure`

**解决方案**:
1. 确认 MySQL 服务正在运行
2. 检查数据库用户名和密码
3. 确认数据库已创建
4. 检查防火墙设置
5. 尝试添加 `allowPublicKeyRetrieval=true` 到连接字符串

### 4. 前端无法访问后端 API
**问题**: CORS 错误或 Network Error

**解决方案**:
1. 确认后端已启动 (访问 http://localhost:8082/api/v1/health)
2. 检查 `client/.env` 中的 `VITE_API_BASE_URL` 配置
3. 确认后端 CORS 配置正确 (查看 `server/src/main/java/server/demo/config/CorsConfig.java`)

### 5. Bun 安装失败
**问题**: Bun 命令不存在

**解决方案**:
```bash
# 使用 npm 作为替代
cd client
npm install
npm run dev
```

### 6. Java 版本不匹配
**问题**: `Unsupported class file major version`

**解决方案**: 确保 Java 版本为 17+
```bash
java -version
# 如果版本不对,重新安装 JDK 17 并设置 JAVA_HOME
```

### 7. Redis 连接失败 (如果配置了)
**解决方案**:
1. 如果不需要 Redis,可以在 `application.properties` 中注释掉 Redis 配置
2. 或启动 Redis 服务:
```bash
redis-server
# 或使用容器
podman start redis-booking-system
```

---

## 技术支持

如果遇到其他问题:
1. 查看后端日志输出
2. 查看浏览器控制台 (F12)
3. 检查 `application.properties` 配置
4. 确认所有环境变量正确设置
5. 验证网络连接和防火墙设置

---

## 快速检查清单

部署前确认:
- [ ] Java 17+ 已安装并配置
- [ ] Node.js 20+ 已安装
- [ ] Bun 已安装
- [ ] MySQL 已安装并运行
- [ ] 数据库 `booking_system_db` 已创建
- [ ] Redis 已安装并运行 (可选)
- [ ] 环境变量已配置
- [ ] 后端依赖已下载 (`./mvnw compile`)
- [ ] 前端依赖已安装 (`bun install`)
- [ ] 端口 8082 和 8091 未被占用

祝部署顺利! 🚀
