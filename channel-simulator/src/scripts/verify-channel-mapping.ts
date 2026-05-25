import assert from 'node:assert/strict'
import { inflateSync } from 'node:zlib'

import {
  AIRBNB_CHANNEL_ID,
  BOOKING_CHANNEL_ID,
  buildLanguageFileResponse,
  buildMasterDataResponse,
  buildOtaRatePlanPullResponse,
  resolveChannelId,
} from '../mock-su-api/channelMapping'

function assertChannelResolution() {
  assert.equal(
    resolveChannelId({ channel_code: AIRBNB_CHANNEL_ID }, BOOKING_CHANNEL_ID),
    BOOKING_CHANNEL_ID,
  )
  assert.equal(resolveChannelId({ channelid: AIRBNB_CHANNEL_ID }), AIRBNB_CHANNEL_ID)
  assert.equal(resolveChannelId({ channel_code: BOOKING_CHANNEL_ID }), BOOKING_CHANNEL_ID)
  assert.equal(resolveChannelId({ channel_code: '999' }), BOOKING_CHANNEL_ID)
}

function assertBookingPayload() {
  const masterData = buildMasterDataResponse({ proppmsid: 'HOTEL-1' }, BOOKING_CHANNEL_ID)
  const ratePlans = buildOtaRatePlanPullResponse({ channel_code: BOOKING_CHANNEL_ID })

  assert.equal(masterData.success, true)
  assert.equal(masterData.data.propertyinfo.proppmsid, 'HOTEL-1')
  assert.equal(masterData.data.channelinfo.id, BOOKING_CHANNEL_ID)
  assert.equal(masterData.data.channelinfo.name, 'Booking.com')
  assert.equal(masterData.data.mapping[0].roomtype_id, 'LOCAL-19-ROOM')
  assert.equal(masterData.data.mapping[0].rateplan_id, 'LOCAL-19-STD')
  assert.equal(ratePlans.success, true)
  assert.equal(ratePlans.data[0].mappingformula, 'LOCAL-19-ROOM####LOCAL-19-STD')
}

function assertAirbnbPayload() {
  const masterData = buildMasterDataResponse({ hotelid: 'HOTEL-2' }, AIRBNB_CHANNEL_ID)
  const ratePlans = buildOtaRatePlanPullResponse({ channel_code: AIRBNB_CHANNEL_ID })

  assert.equal(masterData.success, true)
  assert.equal(masterData.data.propertyinfo.proppmsid, 'HOTEL-2')
  assert.equal(masterData.data.channelinfo.id, AIRBNB_CHANNEL_ID)
  assert.equal(masterData.data.channelinfo.name, 'Airbnb')
  assert.equal(masterData.data.mapping[0].roomtype_id, 'LOCAL-244-LISTING')
  assert.equal(masterData.data.mapping[0].rateplan_id, 'LOCAL-244-STD')
  assert.equal(ratePlans.success, true)
  assert.equal(ratePlans.data[0].mappingformula, 'LOCAL-244-LISTING####LOCAL-244-STD')
}

function assertLanguageFilePayload() {
  const chinesePayload = buildLanguageFileResponse('zn')
  const englishPayload = buildLanguageFileResponse('en')
  const defaultPayload = buildLanguageFileResponse()
  const chineseTranslations = decodeLanguageFileData(chinesePayload.data)
  const englishTranslations = decodeLanguageFileData(englishPayload.data)
  const defaultTranslations = decodeLanguageFileData(defaultPayload.data)

  assert.equal(chinesePayload.success, true)
  assert.equal(chinesePayload.Status, 'Success')
  assert.equal(chinesePayload.language, 'zn')
  assert.equal(chinesePayload.data, chinesePayload.Data)
  assert.equal(chineseTranslations.channelMapping, '渠道映射')
  assert.equal(chineseTranslations.save, '保存')
  assert.equal(chineseTranslations.language, 'zn')

  assert.equal(englishPayload.success, true)
  assert.equal(englishPayload.language, 'en')
  assert.equal(englishTranslations.channelMapping, 'Channel Mapping')
  assert.equal(englishTranslations.save, 'Save')
  assert.equal(englishTranslations.language, 'en')

  assert.equal(defaultPayload.success, true)
  assert.equal(defaultPayload.language, 'zn')
  assert.equal(defaultTranslations.noData, '暂无数据')
}

function decodeLanguageFileData(data: string) {
  const inflated = inflateSync(Buffer.from(data, 'base64'))
  return JSON.parse(inflated.toString('utf8')) as Record<string, string>
}

assertChannelResolution()
assertBookingPayload()
assertAirbnbPayload()
assertLanguageFilePayload()

// eslint-disable-next-line no-console
console.log('channel mapping verification passed')
