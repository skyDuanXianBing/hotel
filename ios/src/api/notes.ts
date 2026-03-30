import type { ApiResponse } from '@/types/api'
import request from '@/utils/request'

export type NoteType = 'income' | 'expense'

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

export interface CategoryStatistic {
  name: string
  value: number
}

export interface PaymentStatistic {
  name: string
  value: number
}

export interface NotesStatisticsDTO {
  netIncome: number
  totalIncome: number
  totalExpense: number
  incomeByProject: CategoryStatistic[]
  expenseByProject: CategoryStatistic[]
  incomeByPayment: PaymentStatistic[]
  expenseByPayment: PaymentStatistic[]
}

export interface NotesQueryParams {
  startDate: string
  endDate: string
  type?: NoteType
  category?: string
  paymentMethod?: string
  roomId?: number
}

export interface CreateNoteRequest {
  type: NoteType
  category: string
  paymentMethod: string
  amount: number
  roomId?: number
  datetime: string
  notes?: string
}

export const createNote = (data: CreateNoteRequest) => {
  return request.post<ApiResponse<NoteDTO>>('/notes', data)
}

export const getNotesStatistics = (params: NotesQueryParams) => {
  return request.get<ApiResponse<NotesStatisticsDTO>>('/notes/statistics', {
    params: { ...params },
  })
}

export const getNotesList = (params: NotesQueryParams) => {
  return request.get<ApiResponse<NoteDTO[]>>('/notes', {
    params: { ...params },
  })
}

const notesApi = {
  createNote,
  getNotesStatistics,
  getNotesList,
}

export default notesApi
