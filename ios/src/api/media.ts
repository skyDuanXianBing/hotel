import request from '@/utils/request'
import type { ApiResponse } from '@/types/api'

export type MediaUploadScope =
  | 'store-logo'
  | 'store-desktop'
  | 'store-mobile'
  | 'room-type-desktop'
  | 'room-type-mobile'

export interface MediaUploadResponseDTO {
  url: string
  originalName: string
  contentType: string
  fileSize: number
}

export const uploadMedia = (scope: MediaUploadScope, file: File) => {
  const formData = new FormData()
  formData.append('scope', scope)
  formData.append('file', file)

  return request.post<ApiResponse<MediaUploadResponseDTO>>('/media/upload', formData)
}
