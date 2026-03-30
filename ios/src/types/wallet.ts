export interface WalletStats {
  balance: number
  withdrawing: number
  pending: number
  totalWithdrawn: number
}

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

export interface WalletPageParams {
  current: number
  size: number
}

export interface WalletPageResponse<T> {
  records: T[]
  total: number
  current: number
  size: number
}

export interface WithdrawOrderRequest {
  amount?: number
  accountType?: string
  accountInfo?: string
}
