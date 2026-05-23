import express from 'express'

import authRouter from './auth'
import availabilityRouter from './availability'
import channelMappingRouter from './channelMapping'
import messagingRouter from './messaging'
import ratePlanRouter from './ratePlan'
import reservationRouter, {
  buildReservationFromFixture,
  getNotifReservation,
  registerNotifReservation,
} from './reservation'
import roomRouter from './room'
export {
  ackPending,
  ackPendingReservations,
  clearReservationPendingState,
  findPendingReservation,
  getPendingReservation,
  listAckedReservations,
  listPending,
  listPendingReservations,
  registerPendingReservation,
  type AckPendingReservationResult,
  type JsonObject,
  type PendingReservation,
  type RegisterPendingReservationInput,
} from '../state/reservationPendingState'

const router = express.Router()

router.use(authRouter)
router.use(availabilityRouter)
router.use(channelMappingRouter)
router.use(messagingRouter)
router.use(ratePlanRouter)
router.use(reservationRouter)
router.use(roomRouter)

export {
  buildReservationFromFixture,
  getNotifReservation,
  registerNotifReservation,
}

export default router
