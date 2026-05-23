import { Request, Response } from 'express'
import express from 'express'
import chalk from 'chalk'

import tokenValidator from './tokenValidator'

const router = express.Router()

/**
 * POST /SUAPI/jservice/availability
 *
 * 模拟 Su API 接收房态/库存（ARI）推送。
 * - Bearer Token 校验
 * - 打印请求体到控制台（带颜色）
 * - 固定返回 { Status: 'Success' }
 */
router.post('/SUAPI/jservice/availability', tokenValidator, (req: Request, res: Response) => {
  // eslint-disable-next-line no-console
  console.log(chalk.cyan('[mock-su-api/availability] received payload:'))
  // eslint-disable-next-line no-console
  console.log(chalk.gray(JSON.stringify(req.body, null, 2)))

  res.json({ Status: 'Success' })
})

export default router
