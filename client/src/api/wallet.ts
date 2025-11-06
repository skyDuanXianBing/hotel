import { request } from '@/utils/request'

// API响应格式
export interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
}

// 钱包统计数据
export interface WalletStats {
  balance: number // 余额
  withdrawing: number // 提现中
  pending: number // 待入账
  totalWithdrawn: number // 累计提现
}

// 余额变动记录
export interface BalanceRecord {
  id: number
  time: string
  type: string
  amount: number
  paymentMethod: string
  orderNo: string
  channel: string
  transactionNo: string
  thirdPartyNo?: string
}

// 提现记录
export interface WithdrawRecord {
  id: number
  applyTime: string
  amount: number
  accountType: string
  accountInfo: string
  applicant: string
  status: string
  processTime?: string
}

// 入账明细
export interface DepositRecord {
  id: number
  occurTime: string
  type: string
  orderNo: string
  channel: string
  orderType: string
  settlementAmount: number
  withdrawStatus: string
  withdrawTime?: string
}

// 分页请求参数
export interface PageParams {
  current: number
  size: number
}

// 分页响应
export interface PageResponse<T> {
  records: T[]
  total: number
  current: number
  size: number
}

// 获取钱包统计数据
export const getWalletStats = async (): Promise<ApiResponse<WalletStats>> => {
  return await request.get('/wallet/stats')
}

// 获取余额变动记录
export const getBalanceRecords = async (
  params: PageParams & {
    searchText?: string
    startDate?: string
    endDate?: string
    type?: string
    paymentMethod?: string
    orderCategory?: string
    status?: string
  }
): Promise<ApiResponse<PageResponse<BalanceRecord>>> => {
  return await request.get('/wallet/balance-records', { params })
}

// 获取提现记录
export const getWithdrawRecords = async (
  params: PageParams & {
    startDate?: string
    endDate?: string
    accountType?: string
    status?: string
  }
): Promise<ApiResponse<PageResponse<WithdrawRecord>>> => {
  return await request.get('/wallet/withdraw-records', { params })
}

// 获取入账明细
export const getDepositRecords = async (
  params: PageParams & {
    searchText?: string
    occurStartDate?: string
    occurEndDate?: string
    type?: string
    channel?: string
    orderType?: string
    withdrawStatus?: string
    paymentMethod?: string
    withdrawStartDate?: string
    withdrawEndDate?: string
    depositType?: 'deposited' | 'pending' // 已入账 or 待入账
  }
): Promise<ApiResponse<PageResponse<DepositRecord>>> => {
  return await request.get('/wallet/deposit-records', { params })
}

// 订单提现
export const withdrawOrder = async (data: {
  amount?: number
  accountType?: string
  accountInfo?: string
}): Promise<ApiResponse<string>> => {
  return await request.post('/wallet/withdraw', data)
}

// 导出余额变动记录
export const exportBalanceRecords = async (params: any): Promise<Blob> => {
  return await request.get('/wallet/balance-records/export', {
    params,
    responseType: 'blob'
  })
}

// 导出提现记录
export const exportWithdrawRecords = async (params: any): Promise<Blob> => {
  return await request.get('/wallet/withdraw-records/export', {
    params,
    responseType: 'blob'
  })
}

// 导出入账明细
export const exportDepositRecords = async (params: any): Promise<Blob> => {
  return await request.get('/wallet/deposit-records/export', {
    params,
    responseType: 'blob'
  })
}
