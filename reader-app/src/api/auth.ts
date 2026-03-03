import request, { type ApiResponse } from './request'

export interface LoginParams {
  email: string
  password: string
}

export interface RegisterParams {
  email: string
  password: string
  nickname: string
}

export interface UserInfo {
  id: number
  email: string
  nickname: string
  balance: number
}

export interface LoginResult {
  token: string
  user: UserInfo
}

export function loginApi(params: LoginParams) {
  return request.post<ApiResponse<LoginResult>>('/reader/auth/login', params)
}

export function registerApi(params: RegisterParams) {
  return request.post<ApiResponse<UserInfo>>('/reader/auth/register', params)
}

export function logoutApi() {
  return request.post<ApiResponse<void>>('/reader/auth/logout')
}

export function getProfileApi() {
  return request.get<ApiResponse<UserInfo>>('/reader/user/profile')
}
