<script setup lang="ts">
import { computed } from 'vue'
import { ElEmpty } from 'element-plus'
import 'element-plus/es/components/empty/style/css'
import { RouterLink, useRoute } from 'vue-router'

import {
  createJobDetailTarget,
  parseJobDetailId,
  resolveJobListReturnTarget,
} from '@/utils/jobDetailRoute'

const route = useRoute()
const jobId = computed(() => parseJobDetailId(route.params.id))
const returnTarget = computed(() =>
  resolveJobListReturnTarget(route.query.from),
)
const detailTarget = computed(() =>
  jobId.value === null
    ? returnTarget.value
    : createJobDetailTarget(jobId.value, returnTarget.value),
)
</script>

<template>
  <section aria-labelledby="job-snapshots-placeholder-title">
    <h1 id="job-snapshots-placeholder-title">历史快照</h1>
    <ElEmpty description="历史快照内容将在 TASK-016 中实现">
      <RouterLink :to="detailTarget">返回职位详情</RouterLink>
    </ElEmpty>
  </section>
</template>
