import { Request, Response } from 'express'
import express from 'express'
import chalk from 'chalk'

import tokenValidator from './tokenValidator'

const router = express.Router()

/**
 * POST /SUAPI/jservice/OTA_HotelRatePlan
 *
 * 模拟 Su API 接收价格计划（Rate Plan）推送。
 * - Bearer Token 校验
 * - 打印请求体到控制台
 * - 固定返回 { Status: 'Success' }
 */
router.post('/SUAPI/jservice/OTA_HotelRatePlan', tokenValidator, (req: Request, res: Response) => {
  // eslint-disable-next-line no-console
  console.log(chalk.cyan('[mock-su-api/OTA_HotelRatePlan] received payload:'))
  // eslint-disable-next-line no-console
  console.log(chalk.gray(JSON.stringify(req.body, null, 2)))

  res.json({ Status: 'Success' })
})

export default router
