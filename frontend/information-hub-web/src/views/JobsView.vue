<script setup lang="ts">
import { computed } from 'vue'
import { ElAlert } from 'element-plus'
import 'element-plus/es/components/alert/style/css'

import JobFilterForm from '@/components/jobs/JobFilterForm.vue'
import JobListResults from '@/components/jobs/JobListResults.vue'
import { useJobList } from '@/composables/useJobList'
import {
  DEFAULT_JOB_LIST_QUERY,
  createJobListFilterDraft,
  type JobListFilterDraft,
} from '@/utils/jobListQuery'

const {
  queryState,
  hasInvalidParameters,
  result,
  loading,
  error,
  pushState,
  retry,
  openJob,
} = useJobList()

const filterState = computed(() =>
  createJobListFilterDraft(queryState.value),
)

function search(draft: JobListFilterDraft): void {
  void pushState({
    ...queryState.value,
    ...draft,
    page: 1,
  })
}

function reset(): void {
  void pushState({
    ...DEFAULT_JOB_LIST_QUERY,
    size: queryState.value.size,
  })
}

function changePage(page: number): void {
  void pushState({
    ...queryState.value,
    page,
  })
}

function changeSize(size: number): void {
  void pushState({
    ...queryState.value,
    page: 1,
    size,
  })
}
</script>

<template>
  <div class="jobs-view">
    <header class="page-heading">
      <div>
        <h1>职位浏览</h1>
        <p>浏览 Information Hub 中已标准化的职位信息。</p>
      </div>
    </header>

    <ElAlert
      v-if="hasInvalidParameters"
      title="地址中的部分参数无效，已使用安全默认值"
      type="warning"
      show-icon
      :closable="false"
    />

    <JobFilterForm
      :state="filterState"
      :loading="loading"
      @search="search"
      @reset="reset"
    />

    <JobListResults
      :items="result?.items ?? []"
      :loading="loading"
      :error-message="error?.message ?? null"
      :page="queryState.page"
      :size="queryState.size"
      :total="result?.total ?? 0"
      :total-pages="result?.totalPages ?? 0"
      @retry="retry"
      @reset="reset"
      @page-change="changePage"
      @size-change="changeSize"
      @select-job="openJob"
    />
  </div>
</template>
