import axios from 'axios'

import type { ApiFailureResponse } from '@/types/api'
import { isApiFailureResponse } from '@/types/api'

export type ApiErrorKind =
  | 'business'
  | 'network'
  | 'timeout'
  | 'cancelled'
  | 'protocol'
  | 'unexpected'

const ERROR_MESSAGES: Readonly<Record<string, string>> = {
  INVALID_JOB_PAGE: '页码必须从 1 开始',
  INVALID_JOB_PAGE_SIZE: '每页数量必须在 1 到 100 之间',
  INVALID_JOB_SORT_FIELD: '选择的排序字段不受支持',
  INVALID_JOB_SORT_DIRECTION: '排序方向必须为升序或降序',
  INVALID_JOB_SALARY: '薪资条件不能小于 0',
  INVALID_JOB_SALARY_RANGE: '最低薪资不能高于最高薪资',
  INVALID_JOB_FILTER: '筛选文本过长，请缩短后重试',
  INVALID_JOB_ID: '职位 ID 必须为正整数',
  INVALID_REQUEST_PARAMETER: '请求参数格式不正确',
  VALIDATION_FAILED: '请求参数校验失败',
  JOB_NOT_FOUND: '职位不存在或已不可用',
  JOB_QUERY_FAILED: '职位信息暂时无法查询',
  AUTHENTICATION_FAILED: '用户名或密码错误',
  AUTHENTICATION_REQUIRED: '登录状态已失效，请重新登录',
  ACCESS_DENIED: '请求被拒绝，请刷新页面后重试',
  PROMPT_PROFILE_NAME_REQUIRED: '请输入提示词方案名称',
  PROMPT_PROFILE_NAME_TOO_LONG: '提示词方案名称不能超过 255 个字符',
  PROMPT_PROFILE_NAME_CONFLICT: '当前账号已存在同名提示词方案',
  PROMPT_PROFILE_NOT_FOUND: '提示词方案不存在或无权访问',
  PROMPT_VERSION_NOT_FOUND: '提示词版本不存在或不属于该方案',
  PROMPT_CONTENT_REQUIRED: '请输入用户提示词',
  PROMPT_CONTENT_TOO_LONG: '用户提示词不能超过 8,000 个字符',
  INVALID_PROMPT_PROFILE_STATUS: '提示词方案状态不受支持',
  UNSUPPORTED_ANALYSIS_DEFINITION: '当前分析定义不受支持',
  PROMPT_PERSISTENCE_FAILED: '提示词数据暂时无法保存，请稍后重试',
  ANALYSIS_NOT_FOUND: '分析结果不存在或无权访问',
  INFORMATION_ANALYSIS_NOT_FOUND: '分析结果不存在或无权访问',
  ANALYSIS_BATCH_NOT_FOUND: '分析批次不存在或无权访问',
  ANALYSIS_SCHEDULE_NOT_FOUND: '定时分析不存在或无权访问',
  ANALYSIS_PREVIEW_TOKEN_EXPIRED: '预览已过期，请重新预览',
  ANALYSIS_PREVIEW_TOKEN_INVALID: '预览已失效，请重新预览',
  ANALYSIS_PREVIEW_DRIFTED: '候选或配置已经变化，请重新预览',
  ANALYSIS_PROVIDER_DISABLED: 'AI 模型服务当前未启用',
  ANALYSIS_WORKER_DISABLED: '分析 Worker 当前未启用',
  ANALYSIS_CONFIGURATION_INVALID: '分析配置当前不可执行',
  ANALYSIS_SCHEDULE_NAME_CONFLICT: '当前账号已存在同名定时分析',
  RECOMMENDATION_PROFILE_NOT_FOUND: '尚未配置职位推荐画像',
  RECOMMENDATION_PROMPT_PROFILE_NOT_FOUND: '绑定的提示词方案不存在',
  RECOMMENDATION_PROMPT_PROFILE_INCOMPATIBLE: '请选择用于职位相关性分析的提示词方案',
  RECOMMENDATION_PROMPT_PROFILE_DISABLED: '绑定的提示词方案已停用',
  RECOMMENDATION_PROMPT_PROFILE_VERSION_REQUIRED: '绑定的提示词方案尚无生效版本',
  RECOMMENDATION_PROMPT_ACTIVE_VERSION_REQUIRED: '绑定的提示词方案必须启用并具有生效版本',
  RECOMMENDATION_RUN_IN_PROGRESS: '已有一次手动刷新正在执行，请等待完成',
  RECOMMENDATION_INFORMATION_TYPE_UNSUPPORTED: '当前只支持职位推荐',
  RECOMMENDATION_FEED_PAGE_INVALID: '推荐页码必须从 1 开始',
  RECOMMENDATION_FEED_PAGE_SIZE_INVALID: '推荐每页数量必须在 1 到 100 之间',
  RECOMMENDATION_INTERACTION_ATTRIBUTION_INVALID: '该推荐项已失效，请刷新页面后重试',
  RECOMMENDATION_PROFILE_PERSISTENCE_FAILED: '推荐画像暂时无法保存，请稍后重试',
  RECOMMENDATION_RUN_PERSISTENCE_FAILED: '推荐刷新暂时无法执行，请稍后重试',
  RECOMMENDATION_FEED_PERSISTENCE_FAILED: '推荐列表暂时无法读取，请稍后重试',
  RECOMMENDATION_INTERACTION_PERSISTENCE_FAILED: '推荐状态暂时无法保存，请稍后重试',
  INTERNAL_ERROR: '服务暂时不可用，请稍后重试',
}

interface ApiClientErrorOptions {
  kind: ApiErrorKind
  code: string
  message: string
  status?: number | null
}

export class ApiClientError extends Error {
  readonly kind: ApiErrorKind
  readonly code: string
  readonly status: number | null

  constructor(options: ApiClientErrorOptions) {
    super(options.message)
    this.name = 'ApiClientError'
    this.kind = options.kind
    this.code = options.code
    this.status = options.status ?? null
  }
}

export function getApiErrorMessage(code: string): string {
  return ERROR_MESSAGES[code] ?? '请求失败，请稍后重试'
}

export function createBusinessApiError(
  failure: ApiFailureResponse,
  status: number | null = null,
): ApiClientError {
  return new ApiClientError({
    kind: 'business',
    code: failure.code,
    message: getApiErrorMessage(failure.code),
    status,
  })
}

export function createInvalidApiResponseError(): ApiClientError {
  return new ApiClientError({
    kind: 'protocol',
    code: 'INVALID_API_RESPONSE',
    message: '服务返回了无法识别的响应',
  })
}

export function toApiClientError(error: unknown): ApiClientError {
  if (error instanceof ApiClientError) {
    return error
  }

  if (axios.isCancel(error)) {
    return new ApiClientError({
      kind: 'cancelled',
      code: 'REQUEST_CANCELLED',
      message: '请求已取消',
    })
  }

  if (axios.isAxiosError(error)) {
    if (error.code === 'ECONNABORTED' || error.code === 'ETIMEDOUT') {
      return new ApiClientError({
        kind: 'timeout',
        code: 'REQUEST_TIMEOUT',
        message: '请求超时，请稍后重试',
      })
    }

    if (error.response) {
      if (isApiFailureResponse(error.response.data)) {
        return createBusinessApiError(
          error.response.data,
          error.response.status,
        )
      }

      return new ApiClientError({
        kind: 'protocol',
        code: 'INVALID_API_RESPONSE',
        message: '服务返回了无法识别的错误响应',
        status: error.response.status,
      })
    }

    return new ApiClientError({
      kind: 'network',
      code: 'NETWORK_ERROR',
      message: '无法连接到服务，请检查网络后重试',
    })
  }

  return new ApiClientError({
    kind: 'unexpected',
    code: 'UNEXPECTED_ERROR',
    message: '发生未知错误，请稍后重试',
  })
}
