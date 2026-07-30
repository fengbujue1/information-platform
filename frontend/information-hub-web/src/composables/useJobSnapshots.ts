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
import { getJobSnapshots } from '@/api/jobApi'
import type { JobSnapshot } from '@/types/job'
import {
  parseJobDetailId,
  resolveJobListReturnTarget,
} from '@/utils/jobDetailRoute'

interface UseJobSnapshotsResult {
  jobId: ComputedRef<number | null>
  snapshots: Ref<JobSnapshot[]>
  selectedSnapshot: ComputedRef<JobSnapshot | null>
  selectedSnapshotId: Ref<number | null>
  loading: Ref<boolean>
  error: Ref<ApiClientError | null>
  notFound: Ref<boolean>
  invalidId: ComputedRef<boolean>
  returnTarget: ComputedRef<string>
  selectSnapshot: (snapshotId: number) => void
  retry: () => void
}

/**
 * 集中处理历史快照加载、版本选择、请求取消和页面错误状态。
 */
export function useJobSnapshots(): UseJobSnapshotsResult {
  const route = useRoute()
  const jobId = computed(() => parseJobDetailId(route.params.id))
  const invalidId = computed(() => jobId.value === null)
  const returnTarget = computed(() =>
    resolveJobListReturnTarget(route.query.from),
  )
  const snapshots = shallowRef<JobSnapshot[]>([])
  const selectedSnapshotId = ref<number | null>(null)
  const selectedSnapshot = computed(
    () =>
      snapshots.value.find(
        (snapshot) => snapshot.id === selectedSnapshotId.value,
      ) ?? null,
  )
  const loading = ref(false)
  const error = ref<ApiClientError | null>(null)
  const notFound = ref(false)
  const retryVersion = ref(0)

  let abortController: AbortController | null = null
  let requestVersion = 0

  async function loadSnapshots(): Promise<void> {
    const currentRequestVersion = ++requestVersion
    abortController?.abort()
    abortController = null

    snapshots.value = []
    selectedSnapshotId.value = null
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
      const response = await getJobSnapshots(id, {
        signal: abortController.signal,
      })
      if (currentRequestVersion !== requestVersion) {
        return
      }

      // 后端已经冻结排序语义；前端直接保留顺序并默认选中第一项。
      snapshots.value = response
      selectedSnapshotId.value = response[0]?.id ?? null
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
      void loadSnapshots()
    },
    { immediate: true },
  )

  onBeforeUnmount(() => {
    requestVersion += 1
    abortController?.abort()
  })

  function selectSnapshot(snapshotId: number): void {
    if (snapshots.value.some((snapshot) => snapshot.id === snapshotId)) {
      selectedSnapshotId.value = snapshotId
    }
  }

  function retry(): void {
    if (!loading.value && !invalidId.value) {
      retryVersion.value += 1
    }
  }

  return {
    jobId,
    snapshots,
    selectedSnapshot,
    selectedSnapshotId,
    loading,
    error,
    notFound,
    invalidId,
    returnTarget,
    selectSnapshot,
    retry,
  }
}
