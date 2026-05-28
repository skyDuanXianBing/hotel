import type { RoomTypeDTO } from '@/api/roomType'

export const getPriceByDate = (roomType: RoomTypeDTO, date: string): number => {
  const dateObj = new Date(date)
  const dayOfWeek = dateObj.getDay()

  switch (dayOfWeek) {
    case 1:
      return roomType.monPrice || 0
    case 2:
      return roomType.tuePrice || 0
    case 3:
      return roomType.wedPrice || 0
    case 4:
      return roomType.thuPrice || 0
    case 5:
      return roomType.friPrice || 0
    case 6:
      return roomType.satPrice || 0
    case 0:
      return roomType.sunPrice || 0
    default:
      return 0
  }
}

export const calculateTotalPriceByDates = (
  roomType: RoomTypeDTO,
  checkInDate: string,
  checkOutDate: string
): number => {
  const checkIn = new Date(checkInDate)
  const checkOut = new Date(checkOutDate)

  let totalPrice = 0
  let currentDate = new Date(checkIn)

  while (currentDate < checkOut) {
    const dateStr = currentDate.toISOString().split('T')[0]
    totalPrice += getPriceByDate(roomType, dateStr)
    currentDate.setDate(currentDate.getDate() + 1)
  }

  return totalPrice
}

const WEEKDAY_KEYS = [
  'stage6.common.weekdays.sun',
  'stage6.common.weekdays.mon',
  'stage6.common.weekdays.tue',
  'stage6.common.weekdays.wed',
  'stage6.common.weekdays.thu',
  'stage6.common.weekdays.fri',
  'stage6.common.weekdays.sat',
] as const

const DEFAULT_WEEKDAYS = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'] as const

export const getWeekdayName = (
  dayOfWeek: number,
  translate?: (key: string) => string,
): string => {
  if (translate) {
    const key = WEEKDAY_KEYS[dayOfWeek]
    return key ? translate(key) : ''
  }
  const weekdays = DEFAULT_WEEKDAYS
  return weekdays[dayOfWeek] || ''
}

export const getWeekdayNameByDate = (
  date: string,
  translate?: (key: string) => string,
): string => {
  const dateObj = new Date(date)
  return getWeekdayName(dateObj.getDay(), translate)
}
