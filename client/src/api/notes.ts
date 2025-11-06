import { request } from '@/utils/request'

// API响应格式
export interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
}

// 记一笔类型
export type NoteType = 'income' | 'expense'

// 记一笔DTO
export interface NoteDTO {
  id: number
  type: NoteType
  category: string
  paymentMethod: string
  amount: number
  roomId?: number
  roomNumber?: string
  datetime: string
  voucherCount: number
  notes?: string
  createdAt: string
  updatedAt: string
}

// 创建记一笔请求
export interface CreateNoteRequest {
  type: NoteType
  category: string
  paymentMethod: string
  amount: number
  roomId?: number
  datetime: string
  notes?: string
  vouchers?: File[]
}

// 记一笔统计数据
export interface NotesStatisticsDTO {
  netIncome: number
  totalIncome: number
  totalExpense: number
  incomeByProject: CategoryStatistic[]
  expenseByProject: CategoryStatistic[]
  incomeByPayment: PaymentStatistic[]
  expenseByPayment: PaymentStatistic[]
}

// 分类统计
export interface CategoryStatistic {
  name: string
  value: number
}

// 支付方式统计
export interface PaymentStatistic {
  name: string
  value: number
}

// 记一笔查询参数
export interface NotesQueryParams {
  startDate: string
  endDate: string
  type?: NoteType
  category?: string
  paymentMethod?: string
  roomId?: number
}

// 获取记一笔统计数据
export const getNotesStatistics = async (
  params: NotesQueryParams
): Promise<ApiResponse<NotesStatisticsDTO>> => {
  return await request.get('/notes/statistics', { params })
}

// 获取记一笔列表
export const getNotesList = async (params: NotesQueryParams): Promise<ApiResponse<NoteDTO[]>> => {
  return await request.get('/notes', { params })
}

// 创建记一笔
export const createNote = async (data: CreateNoteRequest): Promise<ApiResponse<NoteDTO>> => {
  // 如果有凭证文件，使用 FormData
  if (data.vouchers && data.vouchers.length > 0) {
    const formData = new FormData()
    formData.append('type', data.type)
    formData.append('category', data.category)
    formData.append('paymentMethod', data.paymentMethod)
    formData.append('amount', data.amount.toString())
    if (data.roomId) {
      formData.append('roomId', data.roomId.toString())
    }
    formData.append('datetime', data.datetime)
    if (data.notes) {
      formData.append('notes', data.notes)
    }
    data.vouchers.forEach((file) => {
      formData.append('vouchers', file)
    })

    return await request.post('/notes', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    })
  }

  return await request.post('/notes', data)
}

// 删除记一笔
export const deleteNote = async (id: number): Promise<ApiResponse<void>> => {
  return await request.delete(`/notes/${id}`)
}

// 更新记一笔
export const updateNote = async (
  id: number,
  data: Partial<CreateNoteRequest>
): Promise<ApiResponse<NoteDTO>> => {
  return await request.put(`/notes/${id}`, data)
}

// 获取记一笔详情
export const getNoteDetail = async (id: number): Promise<ApiResponse<NoteDTO>> => {
  return await request.get(`/notes/${id}`)
}

// 导出记一笔报表
export const exportNotesReport = async (params: NotesQueryParams): Promise<Blob> => {
  const response = await request.get('/notes/export', {
    params,
    responseType: 'blob',
  })
  return response as unknown as Blob
}