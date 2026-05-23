import type { JsonObject, MessagingForm, RunForm, ScenarioSummary } from './types'

export function buildRunBody(runForm: RunForm): JsonObject {
  const body: JsonObject = {
    mode: runForm.mode,
    channel: runForm.channel,
    scenario: runForm.scenario,
  }
  if (runForm.scenario === 'AIRBNB_NEW') {
    body.channel = 'AIRBNB'
  }
  if (runForm.scenario === 'MULTI_ROOM') {
    body.channel = 'BOOKING'
  }
  if (runForm.roomTypeId) {
    body.roomTypeId = Number(runForm.roomTypeId)
  }
  if (runForm.roomId) {
    body.roomId = Number(runForm.roomId)
  }
  if (runForm.stayStartDays) {
    body.stayStartDays = Number(runForm.stayStartDays)
  }
  return body
}

export function buildMessagingBody(messagingForm: MessagingForm): JsonObject {
  const body: JsonObject = {
    channel: messagingForm.channel,
    message: messagingForm.message,
  }
  if (messagingForm.bookingId.trim()) {
    body.bookingId = messagingForm.bookingId.trim()
  }
  if (messagingForm.threadId.trim()) {
    body.threadId = messagingForm.threadId.trim()
  }
  return body
}

export function buildScenarioExample(
  scenario: ScenarioSummary | null,
  hotelId: string,
  defaultHotelId: string,
): JsonObject {
  const currentHotelId = hotelId.trim() || defaultHotelId
  if (!scenario) {
    return { hotelid: currentHotelId }
  }
  if (scenario.type === 'messaging') {
    return {
      hotelid: currentHotelId,
      messages: [
        {
          message_id: `MSG-${Date.now()}`,
          thread_id: `THREAD-${Date.now()}`,
          sender: 'guest',
          body: '请问可以提前入住吗？',
          sent_at: new Date().toISOString(),
        },
      ],
    }
  }
  if (scenario.mode === 'pull') {
    return {
      hotelid: currentHotelId,
      reservation_notif: {
        reservation_notif_id: [`NOTIF-${Date.now()}`],
      },
    }
  }
  return {
    hotelid: currentHotelId,
    reservations: [],
  }
}
