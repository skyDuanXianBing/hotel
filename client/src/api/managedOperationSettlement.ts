import axios from 'axios'
import { request } from '@/utils/request'

export type ManagedOperationPlatform = 'AIRBNB' | 'BOOKING'
export type ManagedOperationLineStatus =
  | 'INCLUDED'
  | 'PERIOD_EXCLUDED'
  | 'UNMATCHED'
  | 'AMBIGUOUS'
  | 'ROOM_EXCLUDED'
  | 'CANCELLED'

export type ManagedOperationDocumentType = 'settlement' | 'invoice' | 'receipt' | 'all'
export type ManagedOperationFeeType = 'DEDUCTION' | 'CREDIT'

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
  id: number | null
  propertyName: string
  selectedRoomIds: number[]
  managementFeeRate: number
  taxRate: number
  cleaningFeeGross: number
  registrationFeeNet: number
  invoiceIssueDay: number
  receiptIssueDay: number
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

export interface ManagedOperationPropertySummary {
  id: number
  propertyName: string
  roomCount: number
  hasStamp: boolean
  updatedAt: string
}

export interface ManagedOperationSettingsResponse {
  settings: ManagedOperationSettings
  availableRooms: ManagedOperationRoom[]
  persisted: boolean
}

export interface ManagedOperationFee {
  feeType: ManagedOperationFeeType
  description: string
  amountGross: number
}

export interface ManagedOperationRunRequest {
  settlementMonth: string
  fees: ManagedOperationFee[]
  invoiceNumber: string
  invoiceDate: string
  paymentDueDate: string
  receiptNumber: string
  receiptDate: string
  note: string
}

export interface ManagedOperationMonthlyData extends ManagedOperationRunRequest {
  airbnbFileName: string
  bookingFileName: string
  persisted: boolean
}

export interface ManagedOperationNumberSuggestion {
  invoiceNumber: string
  receiptNumber: string
  invoiceDate: string
  receiptDate: string
  invoiceIssueDay: number
  receiptIssueDay: number
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

export const createDefaultManagedOperationSettings = (): ManagedOperationSettings => ({
  id: null,
  propertyName: '',
  selectedRoomIds: [],
  managementFeeRate: 0.1,
  taxRate: 0.1,
  cleaningFeeGross: 8000,
  registrationFeeNet: 2000,
  invoiceIssueDay: 9,
  receiptIssueDay: 10,
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

const normalizeSettings = (value?: Partial<ManagedOperationSettings> | null): ManagedOperationSettings => {
  const normalized = { ...createDefaultManagedOperationSettings(), ...value }
  return {
    ...normalized,
    id: normalized.id == null ? null : Number(normalized.id),
    selectedRoomIds: (normalized.selectedRoomIds ?? []).map(Number),
    managementFeeRate: Number(normalized.managementFeeRate),
    taxRate: Number(normalized.taxRate),
    cleaningFeeGross: Number(normalized.cleaningFeeGross),
    registrationFeeNet: Number(normalized.registrationFeeNet),
    invoiceIssueDay: Number(normalized.invoiceIssueDay ?? 9),
    receiptIssueDay: Number(normalized.receiptIssueDay ?? 10),
  }
}

const normalizeSettingsResponse = (value: {
  settings?: Partial<ManagedOperationSettings> | null
  availableRooms?: ManagedOperationRoom[]
  persisted?: boolean
}): ManagedOperationSettingsResponse => ({
  settings: normalizeSettings(value.settings),
  availableRooms: (value.availableRooms ?? []).map(normalizeRoom),
  persisted: value.persisted === true,
})

export const listManagedOperationProperties = async (): Promise<
  ManagedOperationPropertySummary[]
> => {
  const response = await request.get<unknown, ApiResponse<ManagedOperationPropertySummary[]>>(
    `${BASE_PATH}/properties`,
  )
  return (response.data ?? []).map((item) => ({
    id: Number(item.id),
    propertyName: String(item.propertyName ?? ''),
    roomCount: Number(item.roomCount ?? 0),
    hasStamp: item.hasStamp === true,
    updatedAt: String(item.updatedAt ?? ''),
  }))
}

export const createManagedOperationProperty = async (
  propertyName: string,
): Promise<ManagedOperationSettingsResponse> => {
  const response = await request.post<unknown, ApiResponse<ManagedOperationSettingsResponse>>(
    `${BASE_PATH}/properties`,
    { propertyName },
  )
  return normalizeSettingsResponse(response.data)
}

export const getManagedOperationSettings = async (
  settingsId: number,
): Promise<ManagedOperationSettingsResponse> => {
  const response = await request.get<unknown, ApiResponse<ManagedOperationSettingsResponse>>(
    `${BASE_PATH}/properties/${settingsId}`,
  )
  return normalizeSettingsResponse(response.data)
}

export const saveManagedOperationSettings = async (
  settingsId: number,
  settings: ManagedOperationSettings,
): Promise<ManagedOperationSettingsResponse> => {
  const { hasStamp: _hasStamp, id: _id, ...requestBody } = settings
  const response = await request.put<unknown, ApiResponse<ManagedOperationSettingsResponse>>(
    `${BASE_PATH}/properties/${settingsId}`,
    requestBody,
  )
  return normalizeSettingsResponse(response.data)
}

export const updateManagedOperationIssueDay = async (
  settingsId: number,
  invoiceIssueDay: number,
  receiptIssueDay: number,
): Promise<ManagedOperationSettingsResponse> => {
  const response = await request.patch<unknown, ApiResponse<ManagedOperationSettingsResponse>>(
    `${BASE_PATH}/properties/${settingsId}/issue-day`,
    { invoiceIssueDay, receiptIssueDay },
  )
  return normalizeSettingsResponse(response.data)
}

export const deleteManagedOperationProperty = async (settingsId: number): Promise<void> => {
  await request.delete(`${BASE_PATH}/properties/${settingsId}`)
}

export const uploadManagedOperationStamp = async (
  settingsId: number,
  file: File,
): Promise<ManagedOperationStampResponse> => {
  const formData = new FormData()
  formData.append('file', file)
  const response = await request.post<unknown, ApiResponse<ManagedOperationStampResponse>>(
    `${BASE_PATH}/properties/${settingsId}/stamp`,
    formData,
    {
      headers: { 'Content-Type': 'multipart/form-data' },
    },
  )
  return response.data
}

export const getManagedOperationStamp = async (settingsId: number): Promise<Blob> => {
  return await request.get(`${BASE_PATH}/properties/${settingsId}/stamp`, {
    responseType: 'blob',
    suppressErrorToast: true,
  })
}

const normalizeFees = (fees?: ManagedOperationFee[] | null): ManagedOperationFee[] =>
  (fees ?? []).map((fee) => ({
    feeType: fee.feeType === 'CREDIT' ? 'CREDIT' : 'DEDUCTION',
    description: String(fee.description ?? ''),
    amountGross: Number(fee.amountGross ?? 0),
  }))

const normalizeMonthlyData = (value: Partial<ManagedOperationMonthlyData>): ManagedOperationMonthlyData => ({
  settlementMonth: String(value.settlementMonth ?? ''),
  fees: normalizeFees(value.fees),
  invoiceNumber: String(value.invoiceNumber ?? ''),
  invoiceDate: value.invoiceDate ? String(value.invoiceDate) : '',
  paymentDueDate: value.paymentDueDate ? String(value.paymentDueDate) : '',
  receiptNumber: String(value.receiptNumber ?? ''),
  receiptDate: value.receiptDate ? String(value.receiptDate) : '',
  note: String(value.note ?? ''),
  airbnbFileName: String(value.airbnbFileName ?? ''),
  bookingFileName: String(value.bookingFileName ?? ''),
  persisted: value.persisted === true,
})

export const getManagedOperationMonthlyData = async (
  settingsId: number,
  month: string,
): Promise<ManagedOperationMonthlyData> => {
  const response = await request.get<unknown, ApiResponse<Partial<ManagedOperationMonthlyData>>>(
    `${BASE_PATH}/properties/${settingsId}/monthly`,
    { params: { month } },
  )
  return normalizeMonthlyData(response.data)
}

export const saveManagedOperationMonthlyData = async (
  settingsId: number,
  data: ManagedOperationRunRequest,
  airbnbFile?: File | null,
  bookingFile?: File | null,
): Promise<ManagedOperationMonthlyData> => {
  const formData = new FormData()
  if (airbnbFile) formData.append('airbnbFile', airbnbFile)
  if (bookingFile) formData.append('bookingFile', bookingFile)
  formData.append('request', new Blob([JSON.stringify(data)], { type: 'application/json' }))
  const response = await request.post<unknown, ApiResponse<Partial<ManagedOperationMonthlyData>>>(
    `${BASE_PATH}/properties/${settingsId}/monthly`,
    formData,
    {
      headers: { 'Content-Type': 'multipart/form-data' },
    },
  )
  return normalizeMonthlyData(response.data)
}

export const getManagedOperationDocumentNumbers = async (
  settingsId: number,
  month: string,
): Promise<ManagedOperationNumberSuggestion> => {
  const response = await request.get<unknown, ApiResponse<ManagedOperationNumberSuggestion>>(
    `${BASE_PATH}/properties/${settingsId}/document-numbers`,
    { params: { month } },
  )
  return response.data
}

const buildRunFormData = (
  runRequest: ManagedOperationRunRequest,
  airbnbFile?: File | null,
  bookingFile?: File | null,
) => {
  const formData = new FormData()
  if (airbnbFile) formData.append('airbnbFile', airbnbFile)
  if (bookingFile) formData.append('bookingFile', bookingFile)
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
  settingsId: number,
  runRequest: ManagedOperationRunRequest,
  airbnbFile?: File | null,
  bookingFile?: File | null,
): Promise<ManagedOperationPreview> => {
  const response = await request.post<unknown, ApiResponse<ManagedOperationPreview>>(
    `${BASE_PATH}/properties/${settingsId}/preview`,
    buildRunFormData(runRequest, airbnbFile, bookingFile),
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
  settingsId: number,
  documentType: ManagedOperationDocumentType,
  runRequest: ManagedOperationRunRequest,
  airbnbFile?: File | null,
  bookingFile?: File | null,
): Promise<ManagedOperationDownload> => {
  try {
    let contentDisposition = ''
    const response = await request.post<unknown, Blob>(
      `${BASE_PATH}/properties/${settingsId}/export/${documentType}`,
      buildRunFormData(runRequest, airbnbFile, bookingFile),
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
