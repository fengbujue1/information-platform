import {
  computed,
  onBeforeUnmount,
  ref,
  watch,
  type ComputedRef,
  type Ref,
} from 'vue'
import {
  useRoute,
  useRouter,
  type LocationQueryRaw,
} from 'vue-router'

import { getJobs } from '@/api/jobApi'
import {
  toApiClientError,
  type ApiClientError,
} from '@/api/apiError'
import type { PageResponse } from '@/types/api'
import type { JobListItem } from '@/types/job'
import {
  parseJobListQuery,
  toJobListRouteQuery,
  toJobQueryParams,
  type JobListQueryState,
} from '@/utils/jobListQuery'

interface UseJobListResult {
  queryState: ComputedRef<JobListQueryState>
  hasInvalidParameters: ComputedRef<boolean>
  result: Ref<PageResponse<JobListItem> | null>
  loading: Ref<boolean>
  error: Ref<ApiClientError | null>
  pushState: (state: JobListQueryState) => Promise<void>
  retry: () => void
  openJob: (id: number) => Promise<void>
}

/**
 * 以路由 URL 为列表状态的唯一事实来源，并集中处理取消、乱序响应和越界分页。
 */
export function useJobList(): UseJobListResult {
  const route = useRoute()
  const router = useRouter()
  const parsedQuery = computed(() => parseJobListQuery(route.query))
  const queryState = computed(() => parsedQuery.value.state)
  const hasInvalidParameters = computed(
    () => parsedQuery.value.hasInvalidParameters,
  )
  const result = ref<PageResponse<JobListItem> | null>(null)
  const loading = ref(false)
  const error = ref<ApiClientError | null>(null)
  const retryVersion = ref(0)

  let abortController: AbortController | null = null
  let requestVersion = 0

  async function replaceWithLastValidPage(
    state: JobListQueryState,
    response: PageResponse<JobListItem>,
  ): Promise<boolean> {
    const lastValidPage = Math.max(response.totalPages, 1)
    if (state.page <= lastValidPage) {
      return false
    }

    await router.replace({
      name: 'jobs',
      query: toJobListRouteQuery({
        ...state,
        page: lastValidPage,
      }),
    })
    return true
  }

  async function loadJobs(): Promise<void> {
    const currentRequestVersion = ++requestVersion
    abortController?.abort()
    abortController = new AbortController()

    loading.value = true
    error.value = null
    result.value = null
    const state = queryState.value

    try {
      const response = await getJobs(toJobQueryParams(state), {
        signal: abortController.signal,
      })
      if (currentRequestVersion !== requestVersion) {
        return
      }

      // 后端数据变化后当前页可能越界；修正 URL，由路由监听器只触发一次新请求。
      if (await replaceWithLastValidPage(state, response)) {
        return
      }

      result.value = response
    } catch (cause) {
      if (currentRequestVersion !== requestVersion) {
        return
      }

      const apiError = toApiClientError(cause)
      if (apiError.kind !== 'cancelled') {
        error.value = apiError
      }
    } finally {
      if (currentRequestVersion === requestVersion) {
        loading.value = false
      }
    }
  }

  watch(
    [() => route.fullPath, retryVersion],
    () => {
      void loadJobs()
    },
    { immediate: true },
  )

  onBeforeUnmount(() => {
    requestVersion += 1
    abortController?.abort()
  })

  async function pushState(state: JobListQueryState): Promise<void> {
    await router.push({
      name: 'jobs',
      query: toJobListRouteQuery(state) as LocationQueryRaw,
    })
  }

  function retry(): void {
    if (!loading.value) {
      retryVersion.value += 1
    }
  }

  async function openJob(id: number): Promise<void> {
    await router.push({
      name: 'job-detail',
      params: { id: String(id) },
      query: { from: route.fullPath },
    })
  }

  return {
    queryState,
    hasInvalidParameters,
    result,
    loading,
    error,
    pushState,
    retry,
    openJob,
  }
}
