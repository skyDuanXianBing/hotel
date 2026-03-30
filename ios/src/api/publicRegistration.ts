import type { ApiResponse } from '@/types/api'
import type {
  PublicRegistrationAttachment,
  PublicRegistrationResponse,
  PublicRegistrationSaveRequest,
} from '@/types/publicRegistration'
import publicRequest, { getPublicRequestBaseUrl } from '@/utils/publicRequest'

export const getPublicRegistration = (orderNumber: string, token: string) => {
  return publicRequest.get<ApiResponse<PublicRegistrationResponse>>(
    `/public/registration/${encodeURIComponent(orderNumber)}`,
    {
      params: { t: token },
    },
  )
}

export const savePublicRegistration = (
  orderNumber: string,
  token: string,
  payload: PublicRegistrationSaveRequest,
) => {
  return publicRequest.put<ApiResponse<PublicRegistrationResponse>>(
    `/public/registration/${encodeURIComponent(orderNumber)}`,
    payload,
    {
      params: { t: token },
    },
  )
}

export const submitPublicRegistration = (orderNumber: string, token: string) => {
  return publicRequest.post<ApiResponse<PublicRegistrationResponse>>(
    `/public/registration/${encodeURIComponent(orderNumber)}/submit`,
    null,
    {
      params: { t: token },
    },
  )
}

export const uploadPublicRegistrationPassport = async (
  orderNumber: string,
  token: string,
  guestId: number,
  file: File,
) => {
  const formData = new FormData()
  formData.append('file', file)

  return publicRequest.post<ApiResponse<PublicRegistrationAttachment>>(
    `/public/registration/${encodeURIComponent(orderNumber)}/attachments/passport`,
    formData,
    {
      params: {
        t: token,
        guestId,
      },
    },
  )
}

export const buildPublicRegistrationAttachmentUrl = (
  orderNumber: string,
  attachmentId: number,
  token: string,
  inline: boolean,
) => {
  const baseUrl = getPublicRequestBaseUrl()
  const encodedOrderNumber = encodeURIComponent(orderNumber)
  const encodedToken = encodeURIComponent(token)
  const encodedAttachmentId = encodeURIComponent(String(attachmentId))

  return `${baseUrl}/public/registration/${encodedOrderNumber}/attachments/${encodedAttachmentId}?t=${encodedToken}&inline=${inline ? 'true' : 'false'}`
}

export const downloadPublicRegistrationAttachment = (
  orderNumber: string,
  attachmentId: number,
  token: string,
) => {
  return publicRequest.blob(
    `/public/registration/${encodeURIComponent(orderNumber)}/attachments/${encodeURIComponent(String(attachmentId))}`,
    {
      params: {
        t: token,
        inline: false,
      },
    },
  )
}
