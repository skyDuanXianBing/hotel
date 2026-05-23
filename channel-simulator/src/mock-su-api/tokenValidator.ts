/**
 * Mock Su API 共享的 Bearer Token 验证中间件。
 *
 * 校验规则：
 * - 请求头 `Authorization` 必须存在
 * - 必须以 `Bearer mock-access-token-` 开头
 *
 * 通过则继续；否则返回 401。
 */
import { RequestHandler } from 'express'

const tokenValidator: RequestHandler = (req, res, next) => {
  const authHeader = req.headers['authorization'] || req.headers['Authorization']

  if (!authHeader || typeof authHeader !== 'string') {
    res.status(401).json({
      success: false,
      message: 'Unauthorized - invalid or missing token',
    })
    return
  }

  if (!authHeader.startsWith('Bearer mock-access-token-')) {
    res.status(401).json({
      success: false,
      message: 'Unauthorized - invalid or missing token',
    })
    return
  }

  next()
}

export default tokenValidator
