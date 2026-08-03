import { readonly, ref } from 'vue'

import {
  clearCsrfToken,
  getCurrentUser,
  login as loginRequest,
  logout as logoutRequest,
} from '@/api/authApi'
import { ApiClientError } from '@/api/apiError'
import type { CurrentUser, LoginRequest } from '@/types/identity'

const currentUser = ref<CurrentUser | null>(null)
const initialized = ref(false)
let initialization: Promise<CurrentUser | null> | null = null

export const authState = {
  currentUser: readonly(currentUser),
  initialized: readonly(initialized),
}

export async function ensureCurrentUser(): Promise<CurrentUser | null> {
  if (initialized.value) {
    return currentUser.value
  }
  if (initialization) {
    return initialization
  }
  initialization = getCurrentUser()
    .then((user) => {
      currentUser.value = user
      return user
    })
    .catch((error: unknown) => {
      if (error instanceof ApiClientError && error.status === 401) {
        currentUser.value = null
        return null
      }
      throw error
    })
    .finally(() => {
      initialized.value = true
      initialization = null
    })
  return initialization
}

export async function login(request: LoginRequest): Promise<CurrentUser> {
  const user = await loginRequest(request)
  currentUser.value = user
  initialized.value = true
  return user
}

export async function logout(): Promise<void> {
  try {
    await logoutRequest()
  } finally {
    clearAuthentication()
  }
}

export function clearAuthentication(): void {
  currentUser.value = null
  initialized.value = true
  clearCsrfToken()
}

/** 只供隔离的自动化测试重置模块级 Session 状态。 */
export function resetAuthenticationForTest(): void {
  currentUser.value = null
  initialized.value = false
  initialization = null
  clearCsrfToken()
}

/** 只供组件测试建立与路由 resolver 一致的显示状态。 */
export function setAuthenticatedUserForTest(user: CurrentUser): void {
  currentUser.value = user
  initialized.value = true
}
