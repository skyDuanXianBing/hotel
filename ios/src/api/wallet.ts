import request from '@/utils/request'
import type { ApiResponse, RequestConfig } from '@/types/api'
import type {
  BalanceRecord,
  DepositRecord,
  WalletPageParams,
  WalletPageResponse,
  WalletStats,
  WithdrawOrderRequest,
  WithdrawRecord,
} from '@/types/wallet'

type WalletRequestOptions = Pick<RequestConfig, 'suppressErrorStatuses'>

export const getWalletStats = (options?: WalletRequestOptions) => {
  return request<ApiResponse<WalletStats>>({
    ...options,
    url: '/wallet/stats',
    method: 'GET',
  })
}

export const getBalanceRecords = (
  params: WalletPageParams & {
    searchText?: string
    startDate?: string
    endDate?: string
    type?: string
    paymentMethod?: string
    orderCategory?: string
    status?: string
  },
  options?: WalletRequestOptions,
) => {
  const requestParams: Record<string, string | number | boolean | null | undefined> = {
    ...params,
  }

  return request<ApiResponse<WalletPageResponse<BalanceRecord>>>({
    ...options,
    url: '/wallet/balance-records',
    method: 'GET',
    params: requestParams,
  })
}

export const getWithdrawRecords = (
  params: WalletPageParams & {
    startDate?: string
    endDate?: string
    accountType?: string
    status?: string
  },
  options?: WalletRequestOptions,
) => {
  const requestParams: Record<string, string | number | boolean | null | undefined> = {
    ...params,
  }

  return request<ApiResponse<WalletPageResponse<WithdrawRecord>>>({
    ...options,
    url: '/wallet/withdraw-records',
    method: 'GET',
    params: requestParams,
  })
}

export const getDepositRecords = (
  params: WalletPageParams & {
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
    depositType?: 'deposited' | 'pending'
  },
  options?: WalletRequestOptions,
) => {
  const requestParams: Record<string, string | number | boolean | null | undefined> = {
    ...params,
  }

  return request<ApiResponse<WalletPageResponse<DepositRecord>>>({
    ...options,
    url: '/wallet/deposit-records',
    method: 'GET',
    params: requestParams,
  })
}

export const withdrawOrder = (data: WithdrawOrderRequest, options?: WalletRequestOptions) => {
  return request<ApiResponse<string>>({
    ...options,
    url: '/wallet/withdraw',
    method: 'POST',
    data,
  })
}
