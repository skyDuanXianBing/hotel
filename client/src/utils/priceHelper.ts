/**
 * 价格辅助函数
 * 用于根据日期的星期几获取对应的房型价格
 */

import type { RoomTypeDTO } from '@/api/roomType'

/**
 * 根据日期获取房型的对应价格
 * @param roomType 房型数据
 * @param date 日期字符串 (YYYY-MM-DD)
 * @returns 对应星期的价格
 */
export const getPriceByDate = (roomType: RoomTypeDTO, date: string): number => {
  const dateObj = new Date(date)
  const dayOfWeek = dateObj.getDay() // 0=周日, 1=周一, ..., 6=周六

  // 根据星期几返回对应的价格
  switch (dayOfWeek) {
    case 1: // 周一
      return roomType.monPrice || 0
    case 2: // 周二
      return roomType.tuePrice || 0
    case 3: // 周三
      return roomType.wedPrice || 0
    case 4: // 周四
      return roomType.thuPrice || 0
    case 5: // 周五
      return roomType.friPrice || 0
    case 6: // 周六
      return roomType.satPrice || 0
    case 0: // 周日
      return roomType.sunPrice || 0
    default:
      return 0
  }
}

/**
 * 计算多天住宿的总价格
 * @param roomType 房型数据
 * @param checkInDate 入住日期 (YYYY-MM-DD)
 * @param checkOutDate 离店日期 (YYYY-MM-DD)
 * @returns 总价格
 */
export const calculateTotalPriceByDates = (
  roomType: RoomTypeDTO,
  checkInDate: string,
  checkOutDate: string
): number => {
  const checkIn = new Date(checkInDate)
  const checkOut = new Date(checkOutDate)

  let totalPrice = 0
  let currentDate = new Date(checkIn)

  // 遍历每一天,累加对应星期的价格
  while (currentDate < checkOut) {
    const dateStr = currentDate.toISOString().split('T')[0]
    totalPrice += getPriceByDate(roomType, dateStr)
    currentDate.setDate(currentDate.getDate() + 1)
  }

  return totalPrice
}

/**
 * 获取星期几的中文名称
 * @param dayOfWeek 星期几 (0-6, 0=周日)
 * @returns 中文名称
 */
export const getWeekdayName = (dayOfWeek: number): string => {
  const weekdays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
  return weekdays[dayOfWeek] || ''
}

/**
 * 获取日期的星期几中文名称
 * @param date 日期字符串 (YYYY-MM-DD)
 * @returns 星期几的中文名称
 */
export const getWeekdayNameByDate = (date: string): string => {
  const dateObj = new Date(date)
  return getWeekdayName(dateObj.getDay())
}
