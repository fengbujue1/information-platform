<script setup lang="ts">
import { computed } from 'vue'
import { ElButton } from 'element-plus'
import 'element-plus/es/components/button/style/css'
import { RouterLink } from 'vue-router'

import PageState from '@/components/common/PageState.vue'
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
  <section class="snapshots-view" aria-label="职位历史快照">
    <PageState
      v-if="loading"
      kind="loading"
      title="正在加载历史快照"
      :rows="12"
    />

    <PageState
      v-else-if="invalidId"
      kind="invalid"
      title="职位地址无效"
      description="职位 ID 必须为正整数"
    >
      <RouterLink :to="returnTarget" class="internal-link">
        返回职位列表
      </RouterLink>
    </PageState>

    <PageState
      v-else-if="notFound"
      kind="not-found"
      title="职位不存在"
      description="无法查看该职位的历史快照"
    >
      <RouterLink :to="returnTarget" class="internal-link">
        返回职位列表
      </RouterLink>
    </PageState>

    <PageState
      v-else-if="error"
      kind="error"
      title="历史快照加载失败"
      :description="error.message"
    >
      <ElButton
        type="primary"
        data-test="retry-snapshots"
        @click="retry"
      >
        重试
      </ElButton>
      <RouterLink :to="detailTarget" class="internal-link">
        返回职位详情
      </RouterLink>
    </PageState>

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

      <PageState
        v-if="snapshots.length === 0"
        kind="empty"
        title="当前职位暂无历史版本"
        description="职位存在，但尚未形成可查看的历史快照"
      >
        <RouterLink :to="detailTarget" class="internal-link">
          返回当前职位详情
        </RouterLink>
      </PageState>

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
