import request from '@/utils/request'
import type { ApiResponse } from '@/types/api'

export type PushPlatform = 'IOS' | 'ANDROID'

export const registerPushDevice = (platform: PushPlatform, deviceToken: string, locale: string) => {
  return request.post<ApiResponse<void>>('/push/devices', { platform, deviceToken, locale })
}

export const unregisterPushDevice = (deviceToken: string) => {
  return request<ApiResponse<void>>({
    url: '/push/devices',
    method: 'DELETE',
    data: { deviceToken },
  })
}
