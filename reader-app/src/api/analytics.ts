import request, { type ApiResponse } from './request'

export function recordViewApi(contentId: number) {
  return request.post<ApiResponse<void>>('/reader/analytics/view', { contentId })
}

export function recordDurationApi(contentId: number, durationSeconds: number) {
  return request.post<ApiResponse<void>>('/reader/analytics/duration', { contentId, durationSeconds })
}
