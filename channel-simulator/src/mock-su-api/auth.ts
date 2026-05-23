import { Request, Response } from 'express'
import express from 'express'
import chalk from 'chalk'

import config from '../config'

const { v4: uuidv4 } = require('uuid') as { v4: () => string }

const router = express.Router()

/**
 * GET/POST /SUAPI/jservice/auth/generate-access-token
 *
 * 模拟 Su API 的访问令牌生成接口。
 * - 从请求头读取 `client-id` 与 `client-secret`
 * - 与 config.suAuth 中的凭证进行对比；如果 config 中字段为空则跳过对应校验
 * - 校验通过返回 mock token（Bearer 形式）
 */
function handleGenerateAccessToken(req: Request, res: Response): void {
  const clientId = req.headers['client-id']
  const clientSecret = req.headers['client-secret']

  const expectedId = config.suAuth.clientId
  const expectedSecret = config.suAuth.clientSecret

  const idMismatch = expectedId && clientId !== expectedId
  const secretMismatch = expectedSecret && clientSecret !== expectedSecret

  if (idMismatch || secretMismatch) {
    // eslint-disable-next-line no-console
    console.log(
      chalk.red('[mock-su-api/auth] credential mismatch:'),
      chalk.gray(`client-id=${clientId} client-secret=${clientSecret ? '***' : ''}`),
    )
    res.status(401).json({
      success: false,
      message: 'Invalid credentials',
    })
    return
  }

  const token = `mock-access-token-${uuidv4()}`

  // eslint-disable-next-line no-console
  console.log(
    chalk.cyan('[mock-su-api/auth] issued token:'),
    chalk.yellow(token),
  )

  res.json({
    success: true,
    data: {
      token,
      token_type: 'Bearer',
      expire_in: '3600',
    },
  })
}

router.get('/SUAPI/jservice/auth/generate-access-token', handleGenerateAccessToken)
router.post('/SUAPI/jservice/auth/generate-access-token', handleGenerateAccessToken)

export default router
