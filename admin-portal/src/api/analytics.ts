import request, { type ApiResponse } from './request'

export type ContentType = 'COMIC' | 'NOVEL'

// ---- Usage Analytics ----
export interface UsageQueryParams {
  startDate?: string
  endDate?: string
  contentType?: ContentType | ''
  isPaid?: boolean | ''
}

export interface ContentUsageVO {
  contentId: number
  title: string
  contentType: ContentType
  isPaid: boolean
  totalViews: number
  uniqueViewers: number
  averageDurationSeconds: number
}

export interface UsageAnalyticsVO {
  totalViews: number
  uniqueViewers: number
  averageDurationSeconds: number
  contentUsageList: ContentUsageVO[]
}

// ---- Token Cost Analytics ----
export interface TokenCostQueryParams {
  startDate?: string
  endDate?: string
  providerName?: string
  storylineId?: number | ''
}

export interface ProviderModelCostVO {
  providerName: string
  modelName: string | null
  inputTokens: number
  outputTokens: number
  estimatedCost: number
  callCount: number
}

export interface StorylineCostVO {
  storylineId: number
  storylineTitle: string
  inputTokens: number
  outputTokens: number
  estimatedCost: number
  callCount: number
}

export interface DailyTokenCostVO {
  date: string
  inputTokens: number
  outputTokens: number
  estimatedCost: number
  callCount: number
}

export interface TokenCostAnalyticsVO {
  totalInputTokens: number
  totalOutputTokens: number
  totalEstimatedCost: number
  providerModelCosts: ProviderModelCostVO[]
  storylineCosts: StorylineCostVO[]
  dailyTrend: DailyTokenCostVO[]
}

// ---- Recharge Analytics ----
export interface RechargeQueryParams {
  startDate?: string
  endDate?: string
}

export interface RechargeUserVO {
  userId: number
  nickname: string
  email: string
  rechargeCount: number
  totalRechargeAmount: number
  unlockCount: number
  totalSpent: number
}

export interface RechargeAnalyticsVO {
  totalRechargeCount: number
  totalRechargeAmount: number
  averageRechargeAmount: number
  rechargeUsers: RechargeUserVO[]
}

export const analyticsApi = {
  getUsage(params: UsageQueryParams): Promise<ApiResponse<UsageAnalyticsVO>> {
    return request.get('/analytics/usage', { params }).then(r => r.data)
  },

  getTokenCost(params: TokenCostQueryParams): Promise<ApiResponse<TokenCostAnalyticsVO>> {
    return request.get('/analytics/token-cost', { params }).then(r => r.data)
  },

  getRecharge(params: RechargeQueryParams): Promise<ApiResponse<RechargeAnalyticsVO>> {
    return request.get('/analytics/recharge', { params }).then(r => r.data)
  },
}
