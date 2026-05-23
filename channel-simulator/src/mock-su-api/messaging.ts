import { Request, Response } from 'express'
import express from 'express'
import chalk from 'chalk'

import tokenValidator from './tokenValidator'

const router = express.Router()

/**
 * POST /SUAPI/jservice/messagingAB
 *
 * 模拟 Su API 接收消息（Guest Messaging）回复。
 * - Bearer Token 校验
 * - 打印消息回复内容到控制台
 * - 固定返回 { status: 'Success', response: 'Successfully posted' }
 */
router.post('/SUAPI/jservice/messagingAB', tokenValidator, (req: Request, res: Response) => {
  // eslint-disable-next-line no-console
  console.log(chalk.cyan('[mock-su-api/messagingAB] received message:'))
  // eslint-disable-next-line no-console
  console.log(chalk.gray(JSON.stringify(req.body, null, 2)))

  res.json({
    status: 'Success',
    response: 'Successfully posted',
  })
})

export default router
