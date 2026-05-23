export type JsonObject = Record<string, unknown>

export interface PmsApiEnvelope<T> {
  success?: boolean
  message?: string
  errorCode?: string
  code?: string | number
  data?: T
}

export interface PmsClientResult<T> {
  ok: boolean
  statusCode: number | null
  statusText: string | null
  data: T | null
  envelope: PmsApiEnvelope<T> | null
  error: string | null
  errorCode: string | null
  diagnostic: JsonObject
}

export interface PmsRequestContext {
  authorization?: string | null
  storeId?: string | null
  testSupportKey?: string | null
}

export interface PmsWebhookConfigSummary {
  configured: boolean
  serverBaseUrl: string | null
  reservationNotifWebhookUrl: string | null
}

export interface PmsChannelSummary {
  id: number
  name: string | null
  code: string | null
  type: string | null
  enabled: boolean | null
  active: boolean | null
  autoSyncPrice: boolean | null
  otaPropertyId: string | null
  defaultPricePlanId: number | null
}

export interface PmsRoomTypeSummary {
  id: number
  name: string | null
  code: string | null
  totalRooms: number | null
  maxGuests: number | null
  maxChildOccupancy: number | null
  defaultPrice: number | string | null
  suRoomType: string | null
}

export interface PmsRoomSummary {
  id: number
  roomNumber: string | null
  roomTypeId: number | null
  roomTypeName: string | null
  roomTypeCode: string | null
  floor: number | null
  status: string | null
}

export interface PmsPricePlanSummary {
  id: number
  name: string | null
  nameEn: string | null
  minNights: number | null
  maxNights: number | null
  includeMeal: boolean | null
  derivationType: string | null
  basePlanId: number | null
}

export interface PmsOtaIntegrationSummary {
  id: number
  name: string | null
  code: string | null
  enabled: boolean | null
  connected: boolean | null
  propertyId: string | null
  suPropertyId: string | null
  autoSyncPrice: boolean | null
  defaultPricePlanId: number | null
  hasApiKey: boolean
  hasApiSecret: boolean
  hasWidgetToken: boolean
}

export interface PmsReadinessData {
  storeId: number
  storeName: string | null
  suHotelId: string
  supportedChannelCodes: string[]
  ready: boolean
  missingRequirements: string[]
  webhookConfig: PmsWebhookConfigSummary
  channels: PmsChannelSummary[]
  roomTypes: PmsRoomTypeSummary[]
  rooms: PmsRoomSummary[]
  pricePlans: PmsPricePlanSummary[]
  otaIntegrations: PmsOtaIntegrationSummary[]
}

export interface PmsReservationLookupQuery {
  reservationNotifId?: string
  suReservationId?: string
  channelOrderNumber?: string
  externalBookingKey?: string
  orderNumber?: string
}

export interface PmsReservationSummary {
  id: number
  orderNumber: string | null
  channelOrderNumber: string | null
  externalBookingKey: string | null
  suHotelId: string | null
  suReservationId: string | null
  reservationNotifId: string | null
  roomReservationId: string | null
  status: string | null
  guestName: string | null
  guestPhone: string | null
  checkInDate: string | null
  checkOutDate: string | null
  adults: number | null
  children: number | null
  totalAmount: number | string | null
  currencyCode: string | null
  otaRoomId: string | null
  otaRoomTypeId: number | null
  otaRoomNumber: string | null
  channel: PmsChannelSummary | null
  room: PmsRoomSummary | null
  createdAt: string | null
  updatedAt: string | null
}

export interface PmsReservationLookupData {
  query: PmsReservationLookupQuery
  totalMatches: number
  reservations: PmsReservationSummary[]
}

export interface PmsWebhookEventLookupQuery {
  hotelId?: string
  reservationNotifId?: string
  status?: string
  eventType?: string
  limit?: number
}

export interface PmsWebhookEventSummary {
  id: number
  storeId: number
  hotelId: string | null
  reservationNotifId: string | null
  eventType: string | null
  status: string | null
  retryCount: number | null
  nextRetryAt: string | null
  lastError: string | null
  payloadJson: string | null
  createdAt: string | null
  updatedAt: string | null
}

export interface PmsWebhookEventLookupData {
  query: PmsWebhookEventLookupQuery
  totalMatches: number
  events: PmsWebhookEventSummary[]
}

export interface PmsSetupLocalRoomSummary {
  id: number
  roomNumber: string | null
  roomTypeId: number | null
  roomTypeName: string | null
  roomTypeCode: string | null
  floor: number | null
  status: string | null
}

export interface PmsSetupLocalData {
  token: string
  storeId: number
  suHotelId: string
  userEmail: string
  userId: number
  roomTypeId: number
  roomId: number
  rooms: PmsSetupLocalRoomSummary[]
  summary: JsonObject
  readiness: PmsReadinessData
}

export interface PmsMessagingLookupQuery {
  channelId?: number | string
  threadId?: string
  bookingId?: string
  externalMessageId?: string
  messageId?: string
  limit?: number
}

export interface PmsMessagingMessageSummary {
  id: number
  externalMessageId: string | null
  senderType: string | null
  senderName: string | null
  content: string | null
  sentAt: string | null
  isRead: boolean | null
  deliveryStatus: string | null
}

export interface PmsMessagingThreadSummary {
  id: number
  storeId: number
  suHotelId: string | null
  channelId: number | null
  threadKey: string | null
  threadId: string | null
  bookingId: string | null
  guestId: string | null
  listingId: string | null
  bookingFlag: string | null
  guestName: string | null
  listingName: string | null
  lastMessage: string | null
  lastActivity: string | null
  closed: boolean | null
  messages: PmsMessagingMessageSummary[]
}

export interface PmsMessagingLookupData {
  query: PmsMessagingLookupQuery
  totalMatches: number
  threads: PmsMessagingThreadSummary[]
}
