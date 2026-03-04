import request, { type ApiResponse } from './request'

export type ContentType = 'COMIC' | 'NOVEL'

export interface ContentItem {
  id: number
  title: string
  contentType: ContentType
  coverUrl: string
  description: string
  isPaid: boolean
  price: number | null
  freeChapterCount: number
  publishedAt: string
}

export interface NovelChapterVO {
  id: number
  contentId: number
  chapterNumber: number
  chapterTitle: string
  chapterText: string | null
  chapterSummary: string | null
  price: number | null
  accessible: boolean
  chapterPrice: number | null
}

export interface PageData<T> {
  records: T[]
  total: number
  page: number
  size: number
  hasNext: boolean
}

export interface ContentListParams {
  page?: number
  size?: number
  type?: ContentType
}

export function getContentsApi(params: ContentListParams = {}) {
  return request.get<ApiResponse<PageData<ContentItem>>>('/reader/contents', { params })
}

export function getContentDetailApi(id: number) {
  return request.get<ApiResponse<unknown>>(`/reader/contents/${id}`)
}

export function searchContentsApi(keyword: string, page = 1, size = 10) {
  return request.get<ApiResponse<PageData<ContentItem>>>('/reader/contents/search', {
    params: { keyword, page, size },
  })
}

export function unlockChapterApi(chapterId: number) {
  return request.post<ApiResponse<void>>(`/reader/contents/chapters/${chapterId}/unlock`)
}
