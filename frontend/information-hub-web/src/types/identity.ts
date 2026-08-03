export interface CurrentUser {
  id: number
  username: string
  displayName: string | null
  timezone: string
}

export interface LoginRequest {
  username: string
  password: string
}

export interface CsrfToken {
  headerName: string
  parameterName: string
  token: string
}
