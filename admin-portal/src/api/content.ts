import request, { type ApiResponse } from './request'

export type ContentStatus = 'PENDING_REVIEW' | 'PENDING_PUBLISH' | 'REJECTED' | 'PUBLISHED' | 'OFFLINE'
export type ContentType = 'COMIC' | 'NOVEL'

export interface ContentItem {
  id: number
  title: string
  contentType: ContentType
  status: ContentStatus
  isPaid: boolean
  price: number | null
  coverUrl: string | null
  storylineId: number | null
  createdAt: string
}

export interface ComicPage {
  id: number
  pageNumber: number
  imageUrl: string
  dialogueText: string | null
}

export interface NovelChapter {
  id: number
  chapterNumber: number
  chapterTitle: string
  chapterText: string
}

export interface ContentDetail extends ContentItem {
  description: string | null
  publishedAt: string | null
  comicPages?: ComicPage[]
  novelChapters?: NovelChapter[]
}

export interface ContentListParams {
  page?: number
  size?: number
  status?: ContentStatus | ''
  contentType?: ContentType | ''
  storylineId?: number | ''
  isPaid?: boolean | ''
}

export interface ContentListResult {
  records: ContentItem[]
  total: number
  current: number
  size: number
}

export interface ReviewBody {
  action: 'approve' | 'reject'
  reason?: string
}

export interface PaidBody {
  isPaid: boolean
  price?: number
}

export interface BatchReviewBody {
  contentIds: number[]
  action: 'approve' | 'reject'
}

export interface BatchPaidBody {
  contentIds: number[]
  isPaid: boolean
  price?: number
}

export const contentApi = {
  list(params: ContentListParams): Promise<ApiResponse<ContentListResult>> {
    return request.get('/contents', { params }).then(r => r.data)
  },

  get(id: number): Promise<ApiResponse<ContentDetail>> {
    return request.get(`/contents/${id}`).then(r => r.data)
  },

  update(id: number, data: { title?: string; coverUrl?: string }): Promise<ApiResponse<ContentItem>> {
    return request.put(`/contents/${id}`, data).then(r => r.data)
  },

  review(id: number, body: ReviewBody): Promise<ApiResponse<void>> {
    return request.put(`/contents/${id}/review`, body).then(r => r.data)
  },

  publish(id: number): Promise<ApiResponse<void>> {
    return request.put(`/contents/${id}/status`, { action: 'online' }).then(r => r.data)
  },

  unpublish(id: number): Promise<ApiResponse<void>> {
    return request.put(`/contents/${id}/status`, { action: 'offline' }).then(r => r.data)
  },

  setPaid(id: number, body: PaidBody): Promise<ApiResponse<void>> {
    return request.put(`/contents/${id}/paid`, body).then(r => r.data)
  },

  batchReview(body: BatchReviewBody): Promise<ApiResponse<void>> {
    return request.post('/contents/batch-review', body).then(r => r.data)
  },

  batchPaid(body: BatchPaidBody): Promise<ApiResponse<void>> {
    return request.post('/contents/batch-paid', body).then(r => r.data)
  },
}
