<script setup lang="ts">
import { computed } from 'vue'
import {
  ElButton,
  ElResult,
  ElSkeleton,
} from 'element-plus'
import 'element-plus/es/components/button/style/css'
import 'element-plus/es/components/result/style/css'
import 'element-plus/es/components/skeleton/style/css'
import { RouterLink } from 'vue-router'

import JobDetailContent from '@/components/jobs/JobDetailContent.vue'
import { useJobDetail } from '@/composables/useJobDetail'
import { createJobSnapshotsTarget } from '@/utils/jobDetailRoute'

const {
  jobId,
  job,
  loading,
  error,
  notFound,
  invalidId,
  returnTarget,
  retry,
} = useJobDetail()

const snapshotsTarget = computed(() =>
  createJobSnapshotsTarget(jobId.value ?? 1, returnTarget.value),
)
</script>

<template>
  <section aria-label="职位详情页面">
    <ElSkeleton
      v-if="loading"
      :rows="12"
      animated
      aria-label="正在加载职位详情"
    />

    <ElResult
      v-else-if="invalidId"
      icon="warning"
      title="职位地址无效"
      sub-title="职位 ID 必须为正整数"
    >
      <template #extra>
        <RouterLink :to="returnTarget">返回职位列表</RouterLink>
      </template>
    </ElResult>

    <ElResult
      v-else-if="notFound"
      icon="info"
      title="职位不存在"
      sub-title="该职位可能尚未归档或已经不可用"
    >
      <template #extra>
        <RouterLink :to="returnTarget">返回职位列表</RouterLink>
      </template>
    </ElResult>

    <ElResult
      v-else-if="error"
      icon="error"
      title="职位详情加载失败"
      :sub-title="error.message"
    >
      <template #extra>
        <ElButton
          type="primary"
          data-test="retry-detail"
          @click="retry"
        >
          重试
        </ElButton>
        <RouterLink :to="returnTarget" class="result-back-link">
          返回职位列表
        </RouterLink>
      </template>
    </ElResult>

    <JobDetailContent
      v-else-if="job"
      :job="job"
      :return-target="returnTarget"
      :snapshots-target="snapshotsTarget"
    />
  </section>
</template>
