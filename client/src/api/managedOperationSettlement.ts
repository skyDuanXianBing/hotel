import axios from 'axios'
import { request } from '@/utils/request'
import { resolveManagedOperationSettingsPersisted } from './managedOperationSettlementState'

export type ManagedOperationPlatform = 'AIRBNB' | 'BOOKING'
export type ManagedOperationLineStatus =
  | 'INCLUDED'
  | 'PERIOD_EXCLUDED'
  | 'UNMATCHED'
  | 'AMBIGUOUS'
  | 'ROOM_EXCLUDED'
  | 'CANCELLED'

export type ManagedOperationDocumentType = 'settlement' | 'invoice' | 'receipt' | 'all'

export interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
}

export interface ManagedOperationRoom {
  id: number
  roomNumber: string
  roomTypeName?: string
}

export interface ManagedOperationSettings {
  propertyName: string
  selectedRoomIds: number[]
  managementFeeRate: number
  taxRate: number
  cleaningFeeGross: number
  registrationFeeNet: number
  ownerCompanyName: string
  ownerContactName: string
  ownerPostalCode: string
  ownerAddress: string
  issuerCompanyName: string
  issuerPostalCode: string
  issuerAddress: string
  issuerRegistrationNumber: string
  issuerPhone: string
  issuerEmail: string
  bankName: string
  bankBranch: string
  bankAccountType: string
  bankAccountNumber: string
  bankAccountHolder: string
  hasStamp: boolean
}

export interface ManagedOperationSettingsResponse {
  settings: ManagedOperationSettings
  availableRooms: ManagedOperationRoom[]
  persisted: boolean
}

type ManagedOperationSettingsPayload =
  | ManagedOperationSettings
  | {
      settings?: ManagedOperationSettings | null
      availableRooms?: ManagedOperationRoom[]
      persisted?: boolean
    }

interface ManagedOperationSettingsResponsePayload {
  settings?: ManagedOperationSettings | null
  availableRooms?: ManagedOperationRoom[]
  persisted?: boolean
}

const isManagedOperationSettingsResponsePayload = (
  value: ManagedOperationSettingsPayload,
): value is ManagedOperationSettingsResponsePayload =>
  'settings' in value || 'availableRooms' in value || 'persisted' in value

export interface ManagedOperationDeduction {
  description: string
  amountGross: number
}

export interface ManagedOperationRunRequest {
  settlementMonth: string
  deductions: ManagedOperationDeduction[]
  invoiceNumber: string
  invoiceDate: string
  paymentDueDate: string
  receiptNumber: string
  receiptDate: string
  note: string
}

export interface ManagedOperationPreviewLine {
  platform: ManagedOperationPlatform | string
  sourceRowNumber: number
  bookingKey: string
  checkInDate: string
  checkOutDate: string
  guestName: string
  roomNumber?: string | null
  currency: string
  grossSales: number
  otaServiceFee: number
  payoutFee: number
  cleaningFeeNet: number
  receivedAmount: number | null
  managementFee: number | null
  scheduledTransfer: number | null
  payoutDate?: string | null
  payoutReference?: string | null
  status: ManagedOperationLineStatus | string
  warnings: string[]
}

export interface ManagedOperationPreviewStats {
  airbnbRows: number
  bookingRows: number
  statusCounts: Partial<Record<ManagedOperationLineStatus, number>>
}

export interface ManagedOperationSummary {
  includedReservationCount: number
  selectedRoomCount: number
  totalReceived: number
  managementFeeNet: number
  cleaningFeeNetUnit: number
  cleaningFeeNetTotal: number
  cleaningTax: number
  managementTax: number
  settlementSubtotal: number
  registrationFeeNet: number
  registrationFeeGross: number
  otherDeductionsGross: number
  finalTransfer: number
  invoiceSubtotalNet: number
  invoiceTax: number
  invoiceTotalGross: number
}

export interface ManagedOperationPreview {
  lines: ManagedOperationPreviewLine[]
  stats: ManagedOperationPreviewStats
  summary: ManagedOperationSummary
  exportAllowed: boolean
  blockingReasons: string[]
}

export interface ManagedOperationStampResponse {
  hasStamp: boolean
}

export interface ManagedOperationDownload {
  blob: Blob
  fileName: string
}

const BASE_PATH = '/managed-operation-settlement'

const fallbackSettings = (): ManagedOperationSettings => ({
  propertyName: '',
  selectedRoomIds: [],
  managementFeeRate: 0.1,
  taxRate: 0.1,
  cleaningFeeGross: 8000,
  registrationFeeNet: 2000,
  ownerCompanyName: '',
  ownerContactName: '',
  ownerPostalCode: '',
  ownerAddress: '',
  issuerCompanyName: '',
  issuerPostalCode: '',
  issuerAddress: '',
  issuerRegistrationNumber: '',
  issuerPhone: '',
  issuerEmail: '',
  bankName: '',
  bankBranch: '',
  bankAccountType: '',
  bankAccountNumber: '',
  bankAccountHolder: '',
  hasStamp: false,
})

const normalizeRoom = (room: ManagedOperationRoom & { roomId?: number }): ManagedOperationRoom => ({
  id: Number(room.id ?? room.roomId),
  roomNumber: String(room.roomNumber ?? ''),
  roomTypeName: room.roomTypeName,
})

export const normalizeManagedOperationSettingsResponse = (
  value: ManagedOperationSettingsResponsePayload,
): ManagedOperationSettingsResponse => {
  const responseSettings = value.settings ?? null
  const normalized = { ...fallbackSettings(), ...(responseSettings ?? {}) }
  return {
    settings: {
      ...normalized,
      selectedRoomIds: (normalized.selectedRoomIds ?? []).map(Number),
      managementFeeRate: Number(normalized.managementFeeRate),
      taxRate: Number(normalized.taxRate),
      cleaningFeeGross: Number(normalized.cleaningFeeGross),
      registrationFeeNet: Number(normalized.registrationFeeNet),
    },
    availableRooms: (value.availableRooms ?? []).map(normalizeRoom),
    persisted: resolveManagedOperationSettingsPersisted(value.persisted, responseSettings),
  }
}

export const getManagedOperationSettings = async (): Promise<ManagedOperationSettingsResponse> => {
  const response = await request.get<unknown, ApiResponse<ManagedOperationSettingsResponsePayload>>(
    `${BASE_PATH}/settings`,
  )
  return normalizeManagedOperationSettingsResponse(response.data)
}

export const saveManagedOperationSettings = async (
  settings: ManagedOperationSettings,
): Promise<ManagedOperationSettingsResponse> => {
  const { hasStamp: _hasStamp, ...requestBody } = settings
  const response = await request.put<unknown, ApiResponse<ManagedOperationSettingsPayload>>(
    `${BASE_PATH}/settings`,
    requestBody,
  )
  const payload = response.data
  return normalizeManagedOperationSettingsResponse(
    isManagedOperationSettingsResponsePayload(payload)
      ? payload
      : {
          settings: payload,
          availableRooms: [],
          persisted: true,
        },
  )
}

export const uploadManagedOperationStamp = async (
  file: File,
): Promise<ManagedOperationStampResponse> => {
  const formData = new FormData()
  formData.append('file', file)
  const response = await request.post<unknown, ApiResponse<ManagedOperationStampResponse>>(
    `${BASE_PATH}/stamp`,
    formData,
    {
      headers: { 'Content-Type': 'multipart/form-data' },
    },
  )
  return response.data
}

export const getManagedOperationStamp = async (): Promise<Blob> => {
  return await request.get(`${BASE_PATH}/stamp`, {
    responseType: 'blob',
    suppressErrorToast: true,
  })
}

const buildRunFormData = (
  airbnbFile: File,
  bookingFile: File,
  runRequest: ManagedOperationRunRequest,
) => {
  const formData = new FormData()
  formData.append('airbnbFile', airbnbFile)
  formData.append('bookingFile', bookingFile)
  formData.append('request', new Blob([JSON.stringify(runRequest)], { type: 'application/json' }))
  return formData
}

const extractBlobErrorMessage = async (error: unknown): Promise<string | null> => {
  if (!axios.isAxiosError(error)) {
    return error instanceof Error ? error.message : null
  }

  const data: unknown = error.response?.data
  if (!(data instanceof Blob)) {
    if (data && typeof data === 'object' && 'message' in data) {
      return String((data as { message?: unknown }).message ?? '') || null
    }
    return error.message || null
  }

  try {
    const text = await data.text()
    if (!text) return null
    const parsed = JSON.parse(text) as { message?: unknown; error?: unknown }
    return String(parsed.message ?? parsed.error ?? '') || null
  } catch {
    return null
  }
}

export const previewManagedOperationSettlement = async (
  airbnbFile: File,
  bookingFile: File,
  runRequest: ManagedOperationRunRequest,
): Promise<ManagedOperationPreview> => {
  const response = await request.post<unknown, ApiResponse<ManagedOperationPreview>>(
    `${BASE_PATH}/preview`,
    buildRunFormData(airbnbFile, bookingFile, runRequest),
    {
      headers: { 'Content-Type': 'multipart/form-data' },
    },
  )
  return response.data
}

const parseDownloadFileName = (contentDisposition: unknown, fallback: string) => {
  if (typeof contentDisposition !== 'string') return fallback
  const encodedMatch = contentDisposition.match(/filename\*=UTF-8''([^;]+)/i)
  if (encodedMatch?.[1]) {
    try {
      return decodeURIComponent(encodedMatch[1])
    } catch {
      return encodedMatch[1]
    }
  }
  const plainMatch = contentDisposition.match(/filename="?([^";]+)"?/i)
  return plainMatch?.[1] || fallback
}

export const exportManagedOperationSettlement = async (
  documentType: ManagedOperationDocumentType,
  airbnbFile: File,
  bookingFile: File,
  runRequest: ManagedOperationRunRequest,
): Promise<ManagedOperationDownload> => {
  try {
    let contentDisposition = ''
    const response = await request.post<unknown, Blob>(
      `${BASE_PATH}/export/${documentType}`,
      buildRunFormData(airbnbFile, bookingFile, runRequest),
      {
        headers: { 'Content-Type': 'multipart/form-data' },
        responseType: 'blob',
        suppressErrorToast: true,
        onDownloadProgress: (progressEvent) => {
          const event = (progressEvent as { event?: ProgressEvent }).event
          const target = event?.target as XMLHttpRequest | null
          contentDisposition =
            target?.getResponseHeader('content-disposition') ?? contentDisposition
        },
      },
    )
    const fallback = `managed-operation-${documentType}.${documentType === 'all' ? 'zip' : 'pdf'}`
    return {
      blob: response,
      fileName: parseDownloadFileName(contentDisposition, fallback),
    }
  } catch (error) {
    const message = await extractBlobErrorMessage(error)
    throw new Error(message || 'DOWNLOAD_FAILED')
  }
}
