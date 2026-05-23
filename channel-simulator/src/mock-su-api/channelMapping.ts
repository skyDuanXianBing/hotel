import { Request, Response } from 'express'
import express from 'express'
import chalk from 'chalk'

import tokenValidator from './tokenValidator'

const { v4: uuidv4 } = require('uuid') as { v4: () => string }

const router = express.Router()

const BOOKING_CHANNEL_ID = '19'
const AIRBNB_CHANNEL_ID = '244'
const DEFAULT_CHANNEL_ID = BOOKING_CHANNEL_ID
const KNOWN_CHANNEL_IDS = [BOOKING_CHANNEL_ID, AIRBNB_CHANNEL_ID]
const DEFAULT_ROOM_ID = 'LOCAL-E2E-ROOM'
const DEFAULT_RATE_PLAN_ID = 'LOCAL-E2E-RATE'
const WIDGET_TOKEN_TTL_SECONDS = 3600

interface SuMappingRequestBody {
  hotelid?: string
  channelid?: string | number
}

function normalizeText(value: unknown): string | null {
  if (typeof value !== 'string' && typeof value !== 'number') {
    return null
  }

  const text = String(value).trim()
  if (!text) {
    return null
  }

  return text
}

function buildMappingItem(channelId: string) {
  return {
    Status: 'Active',
    ChannelID: channelId,
    RoomIDs: [DEFAULT_ROOM_ID],
    Rateplans: [
      {
        RatePlanID: DEFAULT_RATE_PLAN_ID,
        MappingStatus: 'Active',
      },
    ],
  }
}

function buildMappingsResponse(channelId: string | null) {
  if (!channelId) {
    const response: Record<string, unknown> = {
      Status: 'Success',
    }

    for (const knownChannelId of KNOWN_CHANNEL_IDS) {
      response[knownChannelId] = [buildMappingItem(knownChannelId)]
    }

    return response
  }

  const resolvedChannelId = channelId || DEFAULT_CHANNEL_ID
  return {
    Status: 'Success',
    [resolvedChannelId]: [buildMappingItem(resolvedChannelId)],
  }
}

/**
 * POST /SUAPI/jservice/mappings
 *
 * 模拟 Su API 已保存的渠道映射。
 * PMS 后端只要求返回的 channel 数组里存在 RoomIDs 且至少一个 Active Rateplans。
 */
router.post('/SUAPI/jservice/mappings', tokenValidator, (req: Request, res: Response) => {
  const body = req.body as SuMappingRequestBody
  const hotelId = normalizeText(body.hotelid)
  const channelId = normalizeText(body.channelid)

  // eslint-disable-next-line no-console
  console.log(chalk.cyan('[mock-su-api/mappings] received payload:'))
  // eslint-disable-next-line no-console
  console.log(chalk.gray(JSON.stringify(req.body, null, 2)))

  if (!hotelId) {
    res.status(400).json({
      Status: 'Fail',
      Errors: {
        ShortText: 'hotelid is required',
      },
    })
    return
  }

  res.json(buildMappingsResponse(channelId))
})

/**
 * POST /SUAPI/jservice/widget/getWidgetAccessToken
 *
 * 模拟 Su Channel Mapping Widget Token 生成。
 * 响应字段兼容 PMS 后端 SuWidgetTokenResponse 的 Data.token_id / Data.proppmsid。
 */
router.post('/SUAPI/jservice/widget/getWidgetAccessToken', tokenValidator, (req: Request, res: Response) => {
  const body = req.body as SuMappingRequestBody
  const hotelId = normalizeText(body.hotelid)

  // eslint-disable-next-line no-console
  console.log(chalk.cyan('[mock-su-api/widget/getWidgetAccessToken] received payload:'))
  // eslint-disable-next-line no-console
  console.log(chalk.gray(JSON.stringify(req.body, null, 2)))

  if (!hotelId) {
    res.status(400).json({
      Status: 'Fail',
      Message: 'hotelid is required',
    })
    return
  }

  res.json({
    Status: 'Success',
    Data: {
      msg: 'Success',
      token_id: `mock-widget-token-${uuidv4()}`,
      user_name: 'Local Channel E2E',
      pms_name: 'TheHostHub PMS',
      proppmsid: hotelId,
      expire_in: WIDGET_TOKEN_TTL_SECONDS,
    },
  })
})

export default router
