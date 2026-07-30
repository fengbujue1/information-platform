import type { RouteLocationRaw } from 'vue-router'

function firstString(value: unknown): string | undefined {
  if (typeof value === 'string') {
    return value
  }
  if (Array.isArray(value) && typeof value[0] === 'string') {
    return value[0]
  }
  return undefined
}

/**
 * 将不可信的详情路由参数收敛为平台正整数主键。
 */
export function parseJobDetailId(value: unknown): number | null {
  const candidate = firstString(value)?.trim()
  if (!candidate || !/^[1-9]\d*$/.test(candidate)) {
    return null
  }

  const id = Number(candidate)
  return Number.isSafeInteger(id) ? id : null
}

/**
 * 返回地址只允许是职位列表本身或带 Query 的职位列表，避免外部跳转。
 */
export function resolveJobListReturnTarget(value: unknown): string {
  const candidate = firstString(value)
  return candidate === '/jobs' || candidate?.startsWith('/jobs?')
    ? candidate
    : '/jobs'
}

export function createJobSnapshotsTarget(
  id: number,
  returnTarget: string,
): RouteLocationRaw {
  return {
    name: 'job-snapshots',
    params: { id: String(id) },
    query: { from: returnTarget },
  }
}

export function createJobDetailTarget(
  id: number,
  returnTarget: string,
): RouteLocationRaw {
  return {
    name: 'job-detail',
    params: { id: String(id) },
    query: { from: returnTarget },
  }
}
