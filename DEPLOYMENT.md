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
- Linux (Ubuntu 20.04+, CentOS 7+, Amazon Linux 2/2023)

### 必需软件
- **Java 17+** (后端运行环境)
- **Node.js 20.19.0+** 或 **22.12.0+** (前端构建工具)
- **Bun** (前端包管理器)
- **MySQL 8.0+** (数据库)
- **Git** (代码管理)

### 可选软件
- **Redis 6.0+** (会话管理和缓存,推荐但非必需)

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

#### Linux (CentOS 7/RHEL 7)
```bash
# CentOS 7 需要启用额外仓库
sudo yum install -y epel-release
sudo yum install -y java-17-openjdk-devel

# 验证安装
java -version
```

#### Linux (Amazon Linux 2)
```bash
# Amazon Linux 2 安装 Amazon Corretto 17
sudo amazon-linux-extras enable corretto17
sudo yum clean metadata
sudo yum install -y java-17-amazon-corretto-devel

# 验证安装
java -version
```

#### Linux (Amazon Linux 2023)
```bash
# 方式1: 安装 Amazon Corretto 17 (推荐)
sudo dnf install -y java-17-amazon-corretto-devel

# 验证安装
java -version

# 如果上面命令失败,尝试方式2:
# 添加 Corretto 仓库
sudo rpm --import https://yum.corretto.aws/corretto.key
sudo curl -L -o /etc/yum.repos.d/corretto.repo https://yum.corretto.aws/corretto.repo

# 安装
sudo dnf install -y java-17-amazon-corretto-devel

# 验证安装
java -version
```

#### 如何确认你的 Amazon Linux 版本
```bash
# 查看系统版本
cat /etc/os-release

# Amazon Linux 2 会显示: VERSION="2"
# Amazon Linux 2023 会显示: VERSION="2023"
```

---

### 2. 安装 Node.js

#### Linux (Ubuntu/Debian)
```bash
# 使用 NodeSource 安装 Node.js 20
curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
sudo apt-get install -y nodejs

# 验证安装
node -v
npm -v
```

#### Linux (Amazon Linux 2/2023 & CentOS/RHEL)
```bash
# 使用 NodeSource 安装 Node.js 20
curl -fsSL https://rpm.nodesource.com/setup_20.x | sudo bash -
sudo yum install -y nodejs

# 或使用 dnf (Amazon Linux 2023)
curl -fsSL https://rpm.nodesource.com/setup_20.x | sudo bash -
sudo dnf install -y nodejs

# 验证安装
node -v
npm -v
```

---

### 3. 安装 Git

#### Linux (Ubuntu/Debian)
```bash
sudo apt update
sudo apt install git -y

# 验证安装
git --version
```

#### Linux (CentOS 7/RHEL 7)
```bash
sudo yum install git -y

# 验证安装
git --version
```

#### Linux (Amazon Linux 2/2023)
```bash
# Amazon Linux 2
sudo yum install git -y

# 或 Amazon Linux 2023
sudo dnf install git -y

# 验证安装
git --version
```

#### macOS
```bash
# Git 通常已预装,如果没有:
xcode-select --install

# 或使用 Homebrew
brew install git

# 验证安装
git --version
```

#### Windows
1. 下载 [Git for Windows](https://git-scm.com/download/win)
2. 运行安装程序
3. 保持默认设置完成安装
4. 验证: `git --version`

---

### 4. 安装 Bun

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

### 5. 安装 MySQL

#### Linux (Ubuntu/Debian)
```bash
# 安装 MySQL Server
sudo apt update
sudo apt install mysql-server -y

# 启动 MySQL 服务
sudo systemctl start mysql
sudo systemctl enable mysql

# 安全配置
sudo mysql_secure_installation
```

#### Linux (CentOS 7/RHEL 7)
```bash
# 下载并安装 MySQL Yum 仓库
sudo yum install -y https://dev.mysql.com/get/mysql80-community-release-el7-3.noarch.rpm

# 安装 MySQL Server
sudo yum install -y mysql-community-server

# 启动 MySQL 服务
sudo systemctl start mysqld
sudo systemctl enable mysqld

# 获取临时 root 密码
sudo grep 'temporary password' /var/log/mysqld.log

# 安全配置(使用上面获取的临时密码)
sudo mysql_secure_installation
```

#### Linux (Amazon Linux 2)
```bash
# 下载并安装 MySQL Yum 仓库
sudo yum install -y https://dev.mysql.com/get/mysql80-community-release-el7-5.noarch.rpm

# 安装 MySQL Server
sudo yum install -y mysql-community-server

# 启动 MySQL 服务
sudo systemctl start mysqld
sudo systemctl enable mysqld

# 获取临时 root 密码
sudo grep 'temporary password' /var/log/mysqld.log

# 安全配置
sudo mysql_secure_installation
```

#### Linux (Amazon Linux 2023)
```bash
# 安装 MariaDB (兼容 MySQL)
sudo dnf install -y mariadb105-server

# 启动服务
sudo systemctl start mariadb
sudo systemctl enable mariadb

# 安全配置
sudo mysql_secure_installation
```

#### macOS
```bash
# 使用 Homebrew 安装
brew install mysql@8.0

# 启动服务
brew services start mysql@8.0

# 安全配置
mysql_secure_installation
```

#### Windows
1. 下载 [MySQL Installer](https://dev.mysql.com/downloads/installer/)
2. 运行安装程序,选择 "Server only" 或 "Full"
3. 设置 root 密码
4. 完成安装后,MySQL 服务会自动启动

#### 创建数据库
```bash
# 登录 MySQL (如果是 MariaDB 使用相同命令)
mysql -u root -p
```

在 MySQL 控制台中执行:
```sql
-- 创建数据库
CREATE DATABASE booking_system_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 创建专用用户(可选但推荐)
CREATE USER 'booking_user'@'localhost' IDENTIFIED BY 'your_secure_password';
GRANT ALL PRIVILEGES ON booking_system_db.* TO 'booking_user'@'localhost';
FLUSH PRIVILEGES;

-- 退出
EXIT;
```

#### 验证 MySQL 安装
```bash
# 检查 MySQL 服务状态
sudo systemctl status mysqld
# 或 (Ubuntu/Debian)
sudo systemctl status mysql
# 或 (MariaDB on Amazon Linux 2023)
sudo systemctl status mariadb

# 测试连接
mysql -u root -p -e "SELECT VERSION();"
```

---

### 6. 安装 Redis (可选)

> **注意**: Redis 用于会话管理和缓存。如果不安装 Redis,需要在后端配置文件中注释掉 Redis 相关配置。

#### Linux (Ubuntu/Debian)
```bash
# 安装 Redis
sudo apt update
sudo apt install redis-server -y

# 启动服务
sudo systemctl start redis-server
sudo systemctl enable redis-server

# 验证安装
redis-cli ping
# 应该返回: PONG
```

#### Linux (CentOS 7/RHEL 7)
```bash
# 安装 EPEL 仓库
sudo yum install -y epel-release

# 安装 Redis
sudo yum install -y redis

# 启动服务
sudo systemctl start redis
sudo systemctl enable redis

# 验证安装
redis-cli ping
```

#### Linux (Amazon Linux 2)
```bash
# 安装 Redis
sudo amazon-linux-extras install -y redis6
sudo yum install -y redis

# 启动服务
sudo systemctl start redis
sudo systemctl enable redis

# 验证安装
redis-cli ping
```

#### Linux (Amazon Linux 2023)
```bash
# 安装 Redis 6
sudo dnf install -y redis6

# 启动服务 (注意服务名是 redis6)
sudo systemctl start redis6
sudo systemctl enable redis6

# 检查服务状态
sudo systemctl status redis6

# 验证安装
redis6-cli ping
# 应该返回: PONG
```

#### macOS
```bash
# 使用 Homebrew 安装
brew install redis

# 启动服务
brew services start redis

# 验证安装
redis-cli ping
```

#### Windows
1. 从 [Redis for Windows](https://github.com/tporadowski/redis/releases) 下载最新版本
2. 解压到目录 (例如 `C:\Redis`)
3. 运行 `redis-server.exe`
4. 打开新终端运行 `redis-cli.exe ping` 验证

---

## 数据库配置

数据库和用户已在上面的 MySQL 安装步骤中创建。如果还没创建,请参考上面"创建数据库"部分。

### 验证数据库连接
```bash
# 使用创建的用户连接
mysql -u booking_user -p -D booking_system_db

# 或使用 root 用户
mysql -u root -p -D booking_system_db

# 查看数据库
SHOW DATABASES;
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
./mvnw spring-boot:run
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

#### 1. 确认服务运行状态
```bash
# 检查 MySQL/MariaDB 服务
sudo systemctl status mysqld        # CentOS/RHEL/Amazon Linux 2
# 或
sudo systemctl status mysql         # Ubuntu/Debian
# 或
sudo systemctl status mariadb       # Amazon Linux 2023

# 检查 Redis 服务(如果安装了)
sudo systemctl status redis         # CentOS/RHEL/Amazon Linux 2
# 或
sudo systemctl status redis-server  # Ubuntu/Debian
# 或
sudo systemctl status redis6        # Amazon Linux 2023

# 如果服务未运行,启动它们
sudo systemctl start mysqld    # 或 mysql 或 mariadb
sudo systemctl start redis     # 或 redis-server 或 redis6
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

### 7. mvnw 权限被拒绝
**问题**: `Permission denied: ./mvnw`

**解决方案**:
```bash
# 方式1: 添加执行权限
chmod +x mvnw
./mvnw clean compile

# 方式2: 使用 bash 执行
bash mvnw clean compile

# 方式3: 使用系统 Maven (如果已安装)
mvn clean compile
```

### 8. Redis 连接失败 (如果配置了)
**解决方案**:
1. 如果不需要 Redis,可以在 `application.properties` 中注释掉 Redis 配置
2. 或启动 Redis 服务:
```bash
# Amazon Linux 2023
sudo systemctl start redis6
redis6-cli ping

# 其他系统
redis-server
# 或
sudo systemctl start redis
redis-cli ping
```

---

## 外部访问配置

### 1. 配置服务器防火墙和安全组

如果你的服务器是云服务器(如 AWS EC2),需要配置安全组规则:

#### AWS EC2 安全组配置
1. 登录 AWS 控制台
2. 进入 EC2 → 实例 → 选择你的实例
3. 点击 "安全" 标签页 → 点击安全组
4. 点击 "编辑入站规则" → "添加规则"
5. 添加以下规则:

| 类型 | 协议 | 端口范围 | 源 | 描述 |
|-----|------|---------|-----|------|
| 自定义 TCP | TCP | 8082 | 0.0.0.0/0 | 后端 API 访问 |
| 自定义 TCP | TCP | 8091 | 0.0.0.0/0 | 前端应用访问 |
| SSH | TCP | 22 | 你的 IP/0.0.0.0/0 | SSH 访问 |

> **注意**: 生产环境建议限制源 IP 地址,避免使用 `0.0.0.0/0`

#### Linux 防火墙配置 (如果使用 firewalld)
```bash
# 检查防火墙状态
sudo systemctl status firewalld

# 如果防火墙启用,添加端口规则
sudo firewall-cmd --permanent --add-port=8082/tcp
sudo firewall-cmd --permanent --add-port=8091/tcp
sudo firewall-cmd --reload

# 验证规则
sudo firewall-cmd --list-ports
```

### 2. 使用外部 IP 访问应用

配置完成后,你可以通过以下地址访问:

- **前端应用**: `http://13.112.235.194:8091`
- **后端 API**: `http://13.112.235.194:8082/api/v1/health`

### 3. 配置说明

前端和后端的 CORS 配置已经包含外部 IP 访问支持:

- **前端配置** ([client/vite.config.ts](client/vite.config.ts:13-22)): 已启用 `host: true` 允许外部访问
- **后端配置** ([server/src/main/java/server/demo/config/CorsConfig.java](server/src/main/java/server/demo/config/CorsConfig.java:17-30)): 已添加外部 IP 到 CORS 白名单

---

## Git 更新和同步

### 从远程仓库拉取最新代码

```bash
# 方式 1: 拉取并合并 (推荐)
cd /path/to/project
git pull origin main

# 方式 2: 拉取但不自动合并
git fetch origin
git merge origin/main

# 方式 3: 强制覆盖本地修改 (谨慎使用)
git fetch origin
git reset --hard origin/main
```

### 查看本地修改状态

```bash
# 查看当前分支和状态
git status

# 查看本地与远程的差异
git diff origin/main

# 查看提交历史
git log --oneline -10
```

### 保存本地修改后再拉取

```bash
# 1. 暂存当前修改
git stash save "临时保存的修改"

# 2. 拉取最新代码
git pull origin main

# 3. 恢复之前的修改
git stash pop
```

### 拉取最新代码后重启服务

```bash
# 1. 拉取最新代码
cd ~/project
git pull origin main

# 2. 重新编译后端
cd server
./mvnw clean compile

# 3. 重启后端服务 (如果在运行,先停止)
# 按 Ctrl+C 停止当前运行的服务,然后:
./mvnw spring-boot:run &

# 4. 重新构建前端 (如果需要)
cd ../client
bun install  # 如果有新依赖
bun run build  # 生产构建

# 或直接启动开发服务器
bun run dev
```

### 常见 Git 问题解决

#### 问题 1: 本地有未提交的修改
```bash
# 解决方案 1: 提交本地修改
git add .
git commit -m "本地修改"
git pull origin main

# 解决方案 2: 暂存本地修改
git stash
git pull origin main
git stash pop
```

#### 问题 2: 拉取时出现冲突
```bash
# 1. 查看冲突文件
git status

# 2. 手动编辑冲突文件,解决冲突标记
# <<<<<<< HEAD
# 你的修改
# =======
# 远程修改
# >>>>>>> origin/main

# 3. 标记冲突已解决
git add <冲突文件>

# 4. 完成合并
git commit -m "解决合并冲突"
```

#### 问题 3: 权限不足
```bash
# 配置 Git 认证
git config --global user.name "Your Name"
git config --global user.email "your@email.com"

# 如果使用 HTTPS,可能需要输入 GitHub 用户名和 Personal Access Token
# 或配置 SSH 密钥
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
