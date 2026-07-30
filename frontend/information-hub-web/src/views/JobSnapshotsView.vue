<script setup lang="ts">
import { computed } from 'vue'
import {
  ElButton,
  ElEmpty,
  ElResult,
  ElSkeleton,
} from 'element-plus'
import 'element-plus/es/components/button/style/css'
import 'element-plus/es/components/empty/style/css'
import 'element-plus/es/components/result/style/css'
import 'element-plus/es/components/skeleton/style/css'
import { RouterLink } from 'vue-router'

import JobSnapshotContent from '@/components/jobs/JobSnapshotContent.vue'
import JobSnapshotList from '@/components/jobs/JobSnapshotList.vue'
import { useJobSnapshots } from '@/composables/useJobSnapshots'
import '@/styles/jobSnapshots.css'
import { createJobDetailTarget } from '@/utils/jobDetailRoute'

const {
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
} = useJobSnapshots()

const detailTarget = computed(() =>
  jobId.value === null
    ? returnTarget.value
    : createJobDetailTarget(jobId.value, returnTarget.value),
)
</script>

<template>
  <section class="snapshots-view" aria-labelledby="snapshots-page-title">
    <ElSkeleton
      v-if="loading"
      :rows="12"
      animated
      aria-label="正在加载历史快照"
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
      sub-title="无法查看该职位的历史快照"
    >
      <template #extra>
        <RouterLink :to="returnTarget">返回职位列表</RouterLink>
      </template>
    </ElResult>

    <ElResult
      v-else-if="error"
      icon="error"
      title="历史快照加载失败"
      :sub-title="error.message"
    >
      <template #extra>
        <ElButton
          type="primary"
          data-test="retry-snapshots"
          @click="retry"
        >
          重试
        </ElButton>
        <RouterLink :to="detailTarget" class="result-back-link">
          返回职位详情
        </RouterLink>
      </template>
    </ElResult>

    <template v-else>
      <header class="snapshots-page-heading">
        <div>
          <RouterLink :to="detailTarget" class="internal-link">
            ← 返回职位详情
          </RouterLink>
          <h1 id="snapshots-page-title">历史快照</h1>
          <p>按归档顺序查看职位的标准化历史版本。</p>
        </div>
        <RouterLink :to="returnTarget" class="internal-link">
          返回职位列表
        </RouterLink>
      </header>

      <ElEmpty
        v-if="snapshots.length === 0"
        description="当前职位暂无历史版本"
      >
        <RouterLink :to="detailTarget">返回当前职位详情</RouterLink>
      </ElEmpty>

      <div v-else class="snapshots-layout">
        <JobSnapshotList
          :snapshots="snapshots"
          :selected-snapshot-id="selectedSnapshotId"
          @select="selectSnapshot"
        />
        <JobSnapshotContent
          v-if="selectedSnapshot"
          :snapshot="selectedSnapshot"
        />
      </div>
    </template>
  </section>
</template>
