# 千问模型配置说明

## 获取API密钥

1. 访问阿里云DashScope控制台：https://dashscope.console.aliyun.com/
2. 登录并创建API Key
3. 复制您的API密钥

## 配置方法

### 方法一：环境变量（推荐）

设置环境变量：
```bash
# Windows
set DASH_SCOPE_API_KEY=your-api-key-here

# Linux/macOS
export DASH_SCOPE_API_KEY=your-api-key-here
```

### 方法二：配置文件

在 `src/main/resources/application.properties` 中取消注释并设置：
```properties
dashscope.api-key=your-api-key-here
```

## 可用模型

- `qwen-turbo` (默认) - 快速响应，适合对话
- `qwen-plus` - 更强能力，响应稍慢
- `qwen-max` - 最强能力

可通过环境变量 `DASH_SCOPE_MODEL` 或配置文件修改模型名称。

## 启动服务

配置完成后，重新启动服务：
```bash
./mvnw spring-boot:run
```

如果配置正确，您将看到日志中显示使用千问模型，而不是Mock实现。

## 故障排除

如果遇到问题：
1. 检查API密钥是否正确设置
2. 确认网络连接正常
3. 查看控制台日志获取详细错误信息
4. 如果API密钥未配置，系统会自动回退到Mock实现