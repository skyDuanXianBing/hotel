export type JsonObject = Record<string, any>

export interface PendingReservation {
  notifId: string
  hotelId: string
  reservation: JsonObject
  fixtureName?: string
  registeredAt: string
  ackedAt?: string
}

export interface RegisterPendingReservationInput {
  notifId: string
  hotelId: string
  reservation: JsonObject
  fixtureName?: string
}

export interface AckPendingReservationResult {
  notifId: string
  hotelId?: string
  acked: boolean
  reason?: string
}

const pendingReservations = new Map<string, PendingReservation>()
const ackedReservations = new Map<string, PendingReservation>()

function normalizeText(value: unknown): string | null {
  if (value === null || value === undefined) {
    return null
  }
  const text = String(value).trim()
  return text.length > 0 ? text : null
}

function cloneJson<T>(value: T): T {
  return JSON.parse(JSON.stringify(value))
}

function buildPendingKey(hotelId: string, notifId: string): string {
  return `${hotelId}::${notifId}`
}

function toPublicPendingReservation(pending: PendingReservation): PendingReservation {
  return {
    ...pending,
    reservation: cloneJson(pending.reservation),
  }
}

export function registerPendingReservation(input: RegisterPendingReservationInput): PendingReservation | null {
  const notifId = normalizeText(input.notifId)
  const hotelId = normalizeText(input.hotelId)

  if (!notifId || !hotelId || !input.reservation) {
    return null
  }

  const pending: PendingReservation = {
    notifId,
    hotelId,
    reservation: cloneJson(input.reservation),
    fixtureName: normalizeText(input.fixtureName) || undefined,
    registeredAt: new Date().toISOString(),
  }

  const key = buildPendingKey(hotelId, notifId)
  pendingReservations.set(key, pending)
  ackedReservations.delete(key)

  return toPublicPendingReservation(pending)
}

export function listPendingReservations(hotelId?: string): PendingReservation[] {
  const normalizedHotelId = normalizeText(hotelId)
  const result: PendingReservation[] = []

  for (const pending of pendingReservations.values()) {
    if (normalizedHotelId && pending.hotelId !== normalizedHotelId) {
      continue
    }
    result.push(toPublicPendingReservation(pending))
  }

  return result
}

export function listPending(hotelId?: string): PendingReservation[] {
  return listPendingReservations(hotelId)
}

export function listAckedReservations(hotelId?: string): PendingReservation[] {
  const normalizedHotelId = normalizeText(hotelId)
  const result: PendingReservation[] = []

  for (const acked of ackedReservations.values()) {
    if (normalizedHotelId && acked.hotelId !== normalizedHotelId) {
      continue
    }
    result.push(toPublicPendingReservation(acked))
  }

  return result
}

export function findPendingReservation(hotelId: string, notifId?: string): PendingReservation | null {
  const normalizedHotelId = normalizeText(hotelId)
  const normalizedNotifId = normalizeText(notifId)

  if (!normalizedHotelId) {
    return null
  }

  if (normalizedNotifId) {
    const pending = pendingReservations.get(buildPendingKey(normalizedHotelId, normalizedNotifId))
    return pending ? toPublicPendingReservation(pending) : null
  }

  for (const pending of pendingReservations.values()) {
    if (pending.hotelId === normalizedHotelId) {
      return toPublicPendingReservation(pending)
    }
  }

  return null
}

export function getPendingReservation(notifId: string, hotelId?: string): PendingReservation | null {
  const normalizedNotifId = normalizeText(notifId)
  const normalizedHotelId = normalizeText(hotelId)

  if (!normalizedNotifId) {
    return null
  }

  if (normalizedHotelId) {
    const pending = pendingReservations.get(buildPendingKey(normalizedHotelId, normalizedNotifId))
    return pending ? toPublicPendingReservation(pending) : null
  }

  for (const pending of pendingReservations.values()) {
    if (pending.notifId === normalizedNotifId) {
      return toPublicPendingReservation(pending)
    }
  }

  return null
}

export function ackPendingReservations(hotelId: string, notifIds: string[]): AckPendingReservationResult[] {
  const normalizedHotelId = normalizeText(hotelId)
  const results: AckPendingReservationResult[] = []

  for (const notifIdValue of notifIds) {
    const notifId = normalizeText(notifIdValue)
    if (!notifId) {
      results.push({
        notifId: '',
        hotelId: normalizedHotelId || undefined,
        acked: false,
        reason: 'missing reservation_notif_id',
      })
      continue
    }

    if (!normalizedHotelId) {
      results.push({
        notifId,
        acked: false,
        reason: 'missing hotelid',
      })
      continue
    }

    const key = buildPendingKey(normalizedHotelId, notifId)
    const pending = pendingReservations.get(key)
    if (!pending) {
      results.push({
        notifId,
        hotelId: normalizedHotelId,
        acked: false,
        reason: 'pending reservation not found',
      })
      continue
    }

    pendingReservations.delete(key)
    const acked: PendingReservation = {
      ...pending,
      ackedAt: new Date().toISOString(),
    }
    ackedReservations.set(key, acked)

    results.push({
      notifId,
      hotelId: normalizedHotelId,
      acked: true,
    })
  }

  return results
}

export function ackPending(hotelId: string, notifIds: string[]): AckPendingReservationResult[] {
  return ackPendingReservations(hotelId, notifIds)
}

export function clearReservationPendingState(): void {
  pendingReservations.clear()
  ackedReservations.clear()
}
