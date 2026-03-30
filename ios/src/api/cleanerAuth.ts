import type { ApiResponse } from '@/types/api'
import type { CleanerLoginRequest, CleanerLoginResponse } from '@/types/auth'
import request from '@/utils/request'

export const cleanerLoginByPassword = (data: CleanerLoginRequest) => {
  return request<ApiResponse<CleanerLoginResponse>>({
    url: '/auth/cleaner/login/password',
    method: 'POST',
    data,
  })
}
