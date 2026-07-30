import {
  computed,
  onBeforeUnmount,
  ref,
  shallowRef,
  watch,
  type ComputedRef,
  type Ref,
} from 'vue'
import { useRoute } from 'vue-router'

import {
  toApiClientError,
  type ApiClientError,
} from '@/api/apiError'
import { getJobById } from '@/api/jobApi'
import type { JobDetail } from '@/types/job'
import {
  parseJobDetailId,
  resolveJobListReturnTarget,
} from '@/utils/jobDetailRoute'

interface UseJobDetailResult {
  jobId: ComputedRef<number | null>
  job: Ref<JobDetail | null>
  loading: Ref<boolean>
  error: Ref<ApiClientError | null>
  notFound: Ref<boolean>
  invalidId: ComputedRef<boolean>
  returnTarget: ComputedRef<string>
  retry: () => void
}

/**
 * 负责详情路由校验、只读加载、请求取消和错误状态，不承担展示逻辑。
 */
export function useJobDetail(): UseJobDetailResult {
  const route = useRoute()
  const jobId = computed(() => parseJobDetailId(route.params.id))
  const invalidId = computed(() => jobId.value === null)
  const returnTarget = computed(() =>
    resolveJobListReturnTarget(route.query.from),
  )
  const job = shallowRef<JobDetail | null>(null)
  const loading = ref(false)
  const error = ref<ApiClientError | null>(null)
  const notFound = ref(false)
  const retryVersion = ref(0)

  let abortController: AbortController | null = null
  let requestVersion = 0

  async function loadJob(): Promise<void> {
    const currentRequestVersion = ++requestVersion
    abortController?.abort()
    abortController = null

    job.value = null
    error.value = null
    notFound.value = false

    const id = jobId.value
    if (id === null) {
      loading.value = false
      return
    }

    abortController = new AbortController()
    loading.value = true

    try {
      const response = await getJobById(id, {
        signal: abortController.signal,
      })
      if (currentRequestVersion === requestVersion) {
        job.value = response
      }
    } catch (cause) {
      if (currentRequestVersion !== requestVersion) {
        return
      }

      const apiError = toApiClientError(cause)
      if (apiError.kind === 'cancelled') {
        return
      }
      if (apiError.code === 'JOB_NOT_FOUND' || apiError.status === 404) {
        notFound.value = true
        return
      }
      error.value = apiError
    } finally {
      if (currentRequestVersion === requestVersion) {
        loading.value = false
      }
    }
  }

  watch(
    [() => route.params.id, retryVersion],
    () => {
      void loadJob()
    },
    { immediate: true },
  )

  onBeforeUnmount(() => {
    requestVersion += 1
    abortController?.abort()
  })

  function retry(): void {
    if (!loading.value && !invalidId.value) {
      retryVersion.value += 1
    }
  }

  return {
    jobId,
    job,
    loading,
    error,
    notFound,
    invalidId,
    returnTarget,
    retry,
  }
}
