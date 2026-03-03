import request, { type ApiResponse } from './request'

export interface Storyline {
  id: number
  title: string
  genre: string
  contentType: 'COMIC' | 'NOVEL'
  status: 'ACTIVE' | 'INACTIVE'
  generatedCount: number
  createdAt: string
}

export interface StorylineDetail extends Storyline {
  characterSettings: string
  worldview: string
  plotOutline: string
  generationConfig?: GenerationConfig
}

export interface StorylineForm {
  title: string
  genre: string
  contentType: 'COMIC' | 'NOVEL'
  characterSettings: string
  worldview: string
  plotOutline: string
}

export interface GenerationConfig {
  id?: number
  textProvider: string
  textModel: string
  imageProvider: string
  imageModel: string
  textTemperature: number
  imageSize: string
  chaptersPerGeneration: number
}

export const storylineApi = {
  list(): Promise<ApiResponse<{ records: Storyline[]; total: number } | Storyline[]>> {
    return request.get('/storylines', { params: { page: 1, size: 100 } }).then(r => r.data)
  },

  create(data: StorylineForm): Promise<ApiResponse<StorylineDetail>> {
    return request.post('/storylines', data).then(r => r.data)
  },

  get(id: number): Promise<ApiResponse<StorylineDetail>> {
    return request.get(`/storylines/${id}`).then(r => r.data)
  },

  update(id: number, data: StorylineForm): Promise<ApiResponse<StorylineDetail>> {
    return request.put(`/storylines/${id}`, data).then(r => r.data)
  },

  toggleStatus(id: number, status: 'ACTIVE' | 'INACTIVE'): Promise<ApiResponse<void>> {
    return request.put(`/storylines/${id}/status`, { status }).then(r => r.data)
  },

  getGenerationConfig(id: number): Promise<ApiResponse<GenerationConfig>> {
    return request.get(`/storylines/${id}/generation-config`).then(r => r.data)
  },

  saveGenerationConfig(id: number, data: GenerationConfig): Promise<ApiResponse<GenerationConfig>> {
    return request.put(`/storylines/${id}/generation-config`, data).then(r => r.data)
  },

  generate(id: number): Promise<ApiResponse<void>> {
    return request.post(`/storylines/${id}/generate`).then(r => r.data)
  },
}
