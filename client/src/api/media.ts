import { request } from '@/utils/request'

export interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
}

export interface MediaUploadResponseDTO {
  url: string
  originalName: string
  contentType: string
  fileSize: number
}

export const uploadMedia = async (
  scope: 'store-logo' | 'store-desktop' | 'store-mobile' | 'room-type-desktop' | 'room-type-mobile',
  file: File
): Promise<ApiResponse<MediaUploadResponseDTO>> => {
  const formData = new FormData()
  formData.append('scope', scope)
  formData.append('file', file)

  return await request.post('/media/upload', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  })
}
