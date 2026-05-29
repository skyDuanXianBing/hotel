import { Request, Response } from 'express'
import express from 'express'
import chalk from 'chalk'
import { deflateSync } from 'node:zlib'

import tokenValidator from './tokenValidator'

const { v4: uuidv4 } = require('uuid') as { v4: () => string }

const router = express.Router()

export const BOOKING_CHANNEL_ID = '19'
export const AIRBNB_CHANNEL_ID = '244'
const DEFAULT_CHANNEL_ID = BOOKING_CHANNEL_ID
const KNOWN_CHANNEL_IDS = [BOOKING_CHANNEL_ID, AIRBNB_CHANNEL_ID]
const LOCAL_MOCK_CHANNEL_ID_HEADER = 'x-su-channel-id'
const DEFAULT_ROOM_ID = 'E2ELOCAL'
const DEFAULT_RATE_PLAN_ID = 'Local E2E Standard Rate'
const DEFAULT_HOTEL_ID = 'LOCALE2EHOTEL'
const DEFAULT_ROOM_NAME = 'Local E2E Standard Room'
const DEFAULT_RATE_PLAN_NAME = 'Local E2E Standard Rate'
const CHANNEL_CATEGORY_NAME = 'Online Travel Agents'
const LOCAL_BOOKING_ROOM_TYPE_ID = 'LOCAL-19-ROOM'
const LOCAL_BOOKING_RATE_PLAN_ID = 'LOCAL-19-STD'
const LOCAL_BOOKING_FLEX_RATE_PLAN_ID = 'LOCAL-19-FLEX'
const LOCAL_AIRBNB_LISTING_ID = 'LOCAL-244-LISTING'
const LOCAL_AIRBNB_RATE_PLAN_ID = 'LOCAL-244-STD'
const WIDGET_TOKEN_TTL_SECONDS = 3600
const DEFAULT_WIDGET_LANGUAGE = 'zn'

const ENGLISH_WIDGET_TRANSLATIONS = {
  channelMapping: 'Channel Mapping',
  channelList: 'Channel List',
  roomType: 'Room Type',
  ratePlan: 'Rate Plan',
  mappingStatus: 'Mapping Status',
  mapped: 'Mapped',
  unmapped: 'Unmapped',
  save: 'Save',
  cancel: 'Cancel',
  close: 'Close',
  continue: 'Continue',
  back: 'Back',
  search: 'Search',
  loading: 'Loading',
  noData: 'No data',
  success: 'Success',
  error: 'Error',
}

const CHINESE_WIDGET_TRANSLATIONS = {
  channelMapping: '渠道映射',
  channelList: '渠道列表',
  roomType: '房型',
  ratePlan: '价格计划',
  mappingStatus: '映射状态',
  mapped: '已映射',
  unmapped: '未映射',
  save: '保存',
  cancel: '取消',
  close: '关闭',
  continue: '继续',
  back: '返回',
  search: '搜索',
  loading: '加载中',
  noData: '暂无数据',
  success: '成功',
  error: '错误',
}

export interface SuMappingRequestBody {
  hotelid?: string
  channelid?: string | number
  proppmsid?: string
  channel_code?: string | number
  cmid?: string | number
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

function getChannelName(channelId: string) {
  if (channelId === AIRBNB_CHANNEL_ID) {
    return 'Airbnb'
  }

  return 'Booking.com'
}

function getChannelRoomTypeId(channelId: string) {
  if (channelId === AIRBNB_CHANNEL_ID) {
    return LOCAL_AIRBNB_LISTING_ID
  }

  return LOCAL_BOOKING_ROOM_TYPE_ID
}

function getChannelRatePlanId(channelId: string) {
  if (channelId === AIRBNB_CHANNEL_ID) {
    return LOCAL_AIRBNB_RATE_PLAN_ID
  }

  return LOCAL_BOOKING_RATE_PLAN_ID
}

function buildChannelRatePlanCombo(channelId: string, ratePlanId?: string) {
  const channelRoomTypeId = getChannelRoomTypeId(channelId)
  const channelRatePlanId = ratePlanId || getChannelRatePlanId(channelId)

  return `${channelRoomTypeId}####${channelRatePlanId}`
}

function buildChannelCard(channelId: string, status: boolean) {
  const channelName = getChannelName(channelId)

  return {
    channel_display_name: channelName,
    channelCode: channelId,
    channelCode_enc: channelId,
    channel_code: channelId,
    id: channelId,
    name: channelName,
    status,
    ismetaChannel: false,
    child_channels: [],
    flagsBoxes: ['23', '24'],
    integrationLoginNames: ['11'],
    authtype: 'P',
    ratePricing: [1],
  }
}

function resolveWidgetLanguage(lang?: unknown): string {
  const normalizedLang = normalizeText(lang)?.toLowerCase()
  if (normalizedLang === 'en') {
    return 'en'
  }

  return DEFAULT_WIDGET_LANGUAGE
}

export function buildLanguageFileResponse(lang?: unknown) {
  const resolvedLang = resolveWidgetLanguage(lang)
  const translations =
    resolvedLang === 'en' ? ENGLISH_WIDGET_TRANSLATIONS : CHINESE_WIDGET_TRANSLATIONS
  const encodedData = encodeLanguageFilePayload({
    ...translations,
    language: resolvedLang,
  })

  return {
    success: true,
    Status: 'Success',
    message: 'Local Su Widget language file fetched successfully',
    language: resolvedLang,
    lang: resolvedLang,
    data: encodedData,
    Data: encodedData,
  }
}

function encodeLanguageFilePayload(payload: Record<string, string>): string {
  const json = JSON.stringify(payload)
  return Buffer.from(deflateSync(Buffer.from(json, 'utf8'))).toString('base64')
}

export function resolveChannelId(body: SuMappingRequestBody, headerChannelId?: unknown): string {
  const candidates = [
    normalizeText(headerChannelId),
    normalizeText(body.channelid),
    normalizeText(body.channel_code),
  ]

  for (const channelId of candidates) {
    if (channelId && KNOWN_CHANNEL_IDS.includes(channelId)) {
      return channelId
    }
  }

  return DEFAULT_CHANNEL_ID
}

export function resolveHotelId(body: SuMappingRequestBody): string {
  return normalizeText(body.hotelid) || normalizeText(body.proppmsid) || DEFAULT_HOTEL_ID
}

export function buildMasterDataResponse(body: SuMappingRequestBody, headerChannelId?: unknown) {
  const hotelId = resolveHotelId(body)
  const channelId = resolveChannelId(body, headerChannelId)
  const channelName = getChannelName(channelId)
  const channelRoomTypeId = getChannelRoomTypeId(channelId)
  const channelRatePlanId = getChannelRatePlanId(channelId)
  const channelRatePlanCombo = buildChannelRatePlanCombo(channelId)

  return {
    success: true,
    message: 'Local channel mapping master data',
    data: {
      propertyinfo: {
        propid: hotelId,
        proppmsid: hotelId,
        propname: 'Local Channel E2E Hotel',
        currencycode: 'USD',
        proptype: '1',
      },
      pmsinfo: {
        id: '1003',
        name: 'TheHostHub PMS',
        pms_status: '2',
        supportMultiConnection: true,
        pms_flags: [],
      },
      channelinfo: {
        id: channelId,
        cmid: `local-${channelId}`,
        channelCode: channelId,
        channel_display_name: channelName,
        name: channelName,
        mapping: 'D',
        channelmappingstring: 'roomtype_id####rateplan_id',
        status: true,
        flagsBoxes: ['23', '24'],
        integrationLoginNames: ['11'],
        authtype: 'P',
        ratePricing: [1],
        getota: false,
        v2only: false,
      },
      roomtypeinfo: {
        [DEFAULT_ROOM_ID]: {
          pmsroomid: DEFAULT_ROOM_ID,
          roomid: DEFAULT_ROOM_ID,
          roomname: DEFAULT_ROOM_NAME,
          rates: [DEFAULT_RATE_PLAN_ID],
        },
      },
      rateplaninfo: {
        [DEFAULT_RATE_PLAN_ID]: {
          pmsrateid: DEFAULT_RATE_PLAN_ID,
          rateid: DEFAULT_RATE_PLAN_ID,
          ratename: DEFAULT_RATE_PLAN_NAME,
        },
      },
      mapping: [
        {
          type: 'manageable',
          mappedname: `${channelName} Local Room - Standard Rate`,
          roomtypename: `${channelName} Local Room`,
          roomtype_id: channelRoomTypeId,
          rateplanname: 'Standard Rate',
          rateplan_id: channelRatePlanId,
          mappingformula: channelRatePlanCombo,
          rateplan_code: 'STD',
          mapping_rateplan: channelRatePlanId,
          fixed_occupany: '2',
          is_child: '',
        },
      ],
    },
  }
}

function buildChannelPasswordResponse(body: SuMappingRequestBody, headerChannelId?: unknown) {
  const channelId = resolveChannelId(body, headerChannelId)
  const channelName = getChannelName(channelId)
  const channelRoomTypeId = getChannelRoomTypeId(channelId)
  const channelRatePlanId = getChannelRatePlanId(channelId)
  const channelRatePlanCombo = buildChannelRatePlanCombo(channelId)
  const flexibleRatePlanId =
    channelId === AIRBNB_CHANNEL_ID ? LOCAL_AIRBNB_RATE_PLAN_ID : LOCAL_BOOKING_FLEX_RATE_PLAN_ID

  return {
    success: true,
    message: 'Local channel mapping password data',
    data: [
      {
        cmid: `local-${channelId}`,
        channel_code: channelId,
        hotel_id: `LOCAL-${channelId}-HOTEL`,
        status: true,
        roomids: [DEFAULT_ROOM_ID],
        mapping: [
          {
            type: 'manageable',
            mappedname: `${channelName} Local Room - Standard Rate`,
            roomtypename: `${channelName} Local Room`,
            roomtype_id: channelRoomTypeId,
            rateplanname: 'Standard Rate',
            rateplan_id: channelRatePlanId,
            mappingformula: channelRatePlanCombo,
            rateplan_code: 'STD',
            mapping_rateplan: channelRatePlanId,
            fixed_occupany: '2',
            is_child: '',
          },
          {
            type: 'nonmanageable',
            mappedname: `${channelName} Local Room - Flexible Rate`,
            roomtypename: `${channelName} Local Room`,
            roomtype_id: channelRoomTypeId,
            rateplanname: 'Flexible Rate',
            rateplan_id: flexibleRatePlanId,
            mappingformula: buildChannelRatePlanCombo(channelId, flexibleRatePlanId),
            rateplan_code: 'FLEX',
            mapping_rateplan: flexibleRatePlanId,
            fixed_occupany: '2',
            is_child: '',
          },
        ],
        room_map: [
          {
            rmid: `local-rmid-${channelId}`,
            staahid: `local-staah-${channelId}`,
            derivedRPs: [],
            extrainfo: {
              fromdt: new Date().toISOString(),
              todate: null,
            },
            roomid: DEFAULT_ROOM_ID,
            rateid: DEFAULT_RATE_PLAN_ID,
            pmsroomid: DEFAULT_ROOM_ID,
            pmsrateid: DEFAULT_RATE_PLAN_ID,
            channelrateplancombo: channelRatePlanCombo,
            croomid: channelRoomTypeId,
            crateid: channelRatePlanId,
            ratePricing: 1,
            multiplier: '1',
            multiplier1: '',
            surcharge: 0,
            surcharge1: '',
            occupancyBasedRates: [],
            fixminstay: false,
            minstay: 0,
            disabledFunction: '',
            so: 0,
            anog: '2',
            anogmult: 1,
            anogplus: 0,
            anog1: '',
            anogmult1: 0,
            anogplus1: 0,
            eacb: 0,
            eacbadult: 0,
            eacbchild: 0,
            advance_purchase_days: '',
            addDerived: 0,
            clonedRPS: null,
            vrbo_listing_id: null,
            isactive: true,
            otacontent: null,
            disable_availability: false,
            disable_rate: false,
            sync_time_limit: null,
          },
        ],
      },
    ],
  }
}

function buildChannelListResponse() {
  return {
    success: true,
    message: 'Local channel list fetched successfully',
    data: {
      property_name: 'Local Channel E2E Hotel',
      channelDetails: {
        [CHANNEL_CATEGORY_NAME]: [
          buildChannelCard(BOOKING_CHANNEL_ID, true),
          buildChannelCard(AIRBNB_CHANNEL_ID, true),
        ],
      },
    },
  }
}

export function buildOtaRatePlanPullResponse(body: SuMappingRequestBody, headerChannelId?: unknown) {
  const channelId = resolveChannelId(body, headerChannelId)
  const channelName = getChannelName(channelId)
  const channelRoomTypeId = getChannelRoomTypeId(channelId)
  const channelRatePlanId = getChannelRatePlanId(channelId)
  const flexibleRatePlanId =
    channelId === AIRBNB_CHANNEL_ID ? LOCAL_AIRBNB_RATE_PLAN_ID : LOCAL_BOOKING_FLEX_RATE_PLAN_ID

  return {
    success: true,
    message: 'Local OTA rate plans fetched successfully',
    data: [
      {
        type: 'manageable',
        mappedname: `${channelName} Local Room - Standard Rate`,
        roomtypename: `${channelName} Local Room`,
        roomtype_id: channelRoomTypeId,
        rateplanname: 'Standard Rate',
        rateplan_id: channelRatePlanId,
        mappingformula: buildChannelRatePlanCombo(channelId, channelRatePlanId),
        rateplan_code: 'STD',
        mapping_rateplan: channelRatePlanId,
        fixed_occupany: '2',
        is_child: '',
      },
      {
        type: 'nonmanageable',
        mappedname: `${channelName} Local Room - Flexible Rate`,
        roomtypename: `${channelName} Local Room`,
        roomtype_id: channelRoomTypeId,
        rateplanname: 'Flexible Rate',
        rateplan_id: flexibleRatePlanId,
        mappingformula: buildChannelRatePlanCombo(channelId, flexibleRatePlanId),
        rateplan_code: 'FLEX',
        mapping_rateplan: flexibleRatePlanId,
        fixed_occupany: '2',
        is_child: '',
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

router.post('/Config/jservice/channelmap/getMappingMasterData', (req: Request, res: Response) => {
  // eslint-disable-next-line no-console
  console.log(chalk.cyan('[mock-su-api/Config/channelmap/getMappingMasterData] received payload:'))
  // eslint-disable-next-line no-console
  console.log(chalk.gray(JSON.stringify(req.body, null, 2)))

  res.json(buildMasterDataResponse(req.body as SuMappingRequestBody, req.get(LOCAL_MOCK_CHANNEL_ID_HEADER)))
})

router.post('/Config/jservice/channelmap/getChannelPasswordData', (req: Request, res: Response) => {
  // eslint-disable-next-line no-console
  console.log(chalk.cyan('[mock-su-api/Config/channelmap/getChannelPasswordData] received payload:'))
  // eslint-disable-next-line no-console
  console.log(chalk.gray(JSON.stringify(req.body, null, 2)))

  res.json(buildChannelPasswordResponse(req.body as SuMappingRequestBody, req.get(LOCAL_MOCK_CHANNEL_ID_HEADER)))
})

router.post('/Config/jservice/channelmap/getChannelList', (req: Request, res: Response) => {
  // eslint-disable-next-line no-console
  console.log(chalk.cyan('[mock-su-api/Config/channelmap/getChannelList] received payload:'))
  // eslint-disable-next-line no-console
  console.log(chalk.gray(JSON.stringify(req.body, null, 2)))

  res.json(buildChannelListResponse())
})

router.post('/Config/jservice/channelmap/getChannelListView', (req: Request, res: Response) => {
  // eslint-disable-next-line no-console
  console.log(chalk.cyan('[mock-su-api/Config/channelmap/getChannelListView] received payload:'))
  // eslint-disable-next-line no-console
  console.log(chalk.gray(JSON.stringify(req.body, null, 2)))

  res.json(buildChannelListResponse())
})

router.all('/Config/jservice/getLanguageFile/:lang', (req: Request, res: Response) => {
  // eslint-disable-next-line no-console
  console.log(chalk.cyan('[mock-su-api/Config/getLanguageFile] received payload:'))
  // eslint-disable-next-line no-console
  console.log(chalk.gray(JSON.stringify({ lang: req.params.lang, body: req.body }, null, 2)))

  res.json(buildLanguageFileResponse(req.params.lang))
})

router.all('/Config/jservice/getLanguageFile', (req: Request, res: Response) => {
  // eslint-disable-next-line no-console
  console.log(chalk.cyan('[mock-su-api/Config/getLanguageFile] received payload:'))
  // eslint-disable-next-line no-console
  console.log(chalk.gray(JSON.stringify(req.body, null, 2)))

  res.json(buildLanguageFileResponse())
})

router.post('/Config/jservice/OTARateplanPull', (req: Request, res: Response) => {
  // eslint-disable-next-line no-console
  console.log(chalk.cyan('[mock-su-api/Config/OTARateplanPull] received payload:'))
  // eslint-disable-next-line no-console
  console.log(chalk.gray(JSON.stringify(req.body, null, 2)))

  res.json(buildOtaRatePlanPullResponse(req.body as SuMappingRequestBody, req.get(LOCAL_MOCK_CHANNEL_ID_HEADER)))
})

router.post('/Config/jservice/getOTARateplanPull', (req: Request, res: Response) => {
  // eslint-disable-next-line no-console
  console.log(chalk.cyan('[mock-su-api/Config/getOTARateplanPull] received payload:'))
  // eslint-disable-next-line no-console
  console.log(chalk.gray(JSON.stringify(req.body, null, 2)))

  res.json(buildOtaRatePlanPullResponse(req.body as SuMappingRequestBody, req.get(LOCAL_MOCK_CHANNEL_ID_HEADER)))
})

export default router
