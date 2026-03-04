import request, { type ApiResponse } from './request'

export interface UserProfile {
  id: number
  nickname: string
  email: string
  balance: number
  createdAt: string
}

export interface RechargeRecord {
  id: number
  amount: number
  createdAt: string
}

export interface UnlockResult {
  success: boolean
  newBalance: number
}

export function getUserProfileApi() {
  return request.get<ApiResponse<UserProfile>>('/reader/user/profile')
}

export function rechargeApi(amount: number) {
  return request.post<ApiResponse<{ balance: number }>>('/reader/user/recharge', { amount })
}

export function getRechargeRecordsApi() {
  return request.get<ApiResponse<RechargeRecord[]>>('/reader/user/recharge-records')
}

export function unlockContentApi(contentId: number) {
  return request.post<ApiResponse<UnlockResult>>(`/reader/contents/${contentId}/unlock`)
}

export interface PaymentOrderResult {
  orderNo: string
  qrcodeUrl: string
  payUrl: string
  amount: number
}

export interface PaymentStatusResult {
  orderNo: string
  status: 'PENDING' | 'PAID' | 'EXPIRED' | 'FAILED'
  amount: number
  paidAt: string | null
}

export function createPaymentOrderApi(amount: number) {
  return request.post<ApiResponse<PaymentOrderResult>>('/reader/payment/create', { amount })
}

export function getPaymentStatusApi(orderNo: string) {
  return request.get<ApiResponse<PaymentStatusResult>>(`/reader/payment/status/${orderNo}`)
}
