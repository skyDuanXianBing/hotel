import type { ApiResponse } from '@/types/api'
import { AUTH_LOGIN_FAILURE_STATUSES } from '@/constants/auth'
import type { CleanerLoginRequest, CleanerLoginResponse } from '@/types/auth'
import request from '@/utils/request'

export const cleanerLoginByPassword = (data: CleanerLoginRequest) => {
  return request<ApiResponse<CleanerLoginResponse>>({
    url: '/auth/cleaner/login/password',
    method: 'POST',
    data,
    suppressErrorStatuses: [...AUTH_LOGIN_FAILURE_STATUSES],
  })
}
