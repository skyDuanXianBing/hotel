/**
 * channel-simulator 运行时配置
 * 所有字段都支持通过环境变量覆盖，方便在不同环境下调整而无需修改代码。
 */
export interface SimulatorConfig {
  port: number
  pmsBaseUrl: string
  pmsAuth: {
    token: string
    storeId: string
  }
  webhookAuth: {
    username: string
    password: string
  }
  suAuth: {
    clientId: string
    clientSecret: string
  }
  defaultHotelId: string
}

const config = {
  // Mock API 服务监听端口
  port: parseInt(process.env.SIM_PORT || process.env.PORT || '4000', 10),

  // PMS 服务器基础地址（用于 webhook 推送目标）
  pmsBaseUrl: process.env.PMS_BASE_URL || 'http://localhost:8092',

  // 调用 PMS test-support API 时使用的认证与门店上下文
  pmsAuth: {
    token: process.env.PMS_AUTH_TOKEN || '',
    storeId: process.env.PMS_STORE_ID || '',
  },

  // 发送 webhook 到 PMS 时使用的 Basic Auth 凭证（可选）
  webhookAuth: {
    username: process.env.WEBHOOK_AUTH_USERNAME || '',
    password: process.env.WEBHOOK_AUTH_PASSWORD || '',
  },

  // Mock Su API 端侧接受的 OAuth/Client 凭证
  suAuth: {
    clientId: process.env.SU_CLIENT_ID || 'mock-client-id',
    clientSecret: process.env.SU_CLIENT_SECRET || 'mock-client-secret',
  },

  // 默认门店 ID（多门店场景下的回退值）
  defaultHotelId: process.env.DEFAULT_HOTEL_ID || '1',
} satisfies SimulatorConfig

export default config
