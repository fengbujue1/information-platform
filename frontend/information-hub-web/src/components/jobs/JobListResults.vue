<script setup lang="ts">
import { computed } from 'vue'
import {
  ElButton,
  ElPagination,
  ElTable,
  ElTableColumn,
  ElTag,
  ElTooltip,
} from 'element-plus'
import 'element-plus/es/components/button/style/css'
import 'element-plus/es/components/pagination/style/css'
import 'element-plus/es/components/table/style/css'
import 'element-plus/es/components/tag/style/css'
import 'element-plus/es/components/tooltip/style/css'

import PageState from '@/components/common/PageState.vue'
import type { JobListItem } from '@/types/job'
import {
  formatDateTime,
  formatNullableValue,
  formatSalary,
} from '@/utils/formatters'

const props = defineProps<{
  items: JobListItem[]
  loading: boolean
  errorMessage: string | null
  page: number
  size: number
  total: number
  totalPages: number
}>()

const emit = defineEmits<{
  retry: []
  reset: []
  pageChange: [page: number]
  sizeChange: [size: number]
  selectJob: [id: number]
}>()

const pageSizes = computed(() =>
  Array.from(new Set([10, 20, 50, 100, props.size])).sort(
    (left, right) => left - right,
  ),
)

function salary(job: JobListItem): string {
  return formatSalary(job)
}

function tableJob(row: unknown): JobListItem {
  return row as JobListItem
}

function location(job: JobListItem): string {
  return formatNullableValue(job.locationName ?? job.cityName)
}

function experienceAndEducation(job: JobListItem): string {
  const values = [job.experienceText, job.educationText]
    .map((value) => value?.trim())
    .filter((value): value is string => Boolean(value))
  return values.length > 0 ? values.join(' · ') : '—'
}

function statusTagType(
  status: string,
): 'success' | 'info' | 'warning' {
  const normalized = status.trim().toUpperCase()
  if (normalized === 'ACTIVE' || normalized === 'OPEN') {
    return 'success'
  }
  if (normalized === 'CLOSED' || normalized === 'OFFLINE') {
    return 'info'
  }
  return 'warning'
}
</script>

<template>
  <section class="job-results" aria-labelledby="job-results-title">
    <div class="section-heading job-results-heading">
      <div>
        <h2 id="job-results-title">职位结果</h2>
        <p v-if="!loading && !errorMessage">
          共 {{ total }} 条，{{ totalPages }} 页
        </p>
      </div>
    </div>

    <PageState
      v-if="loading"
      kind="loading"
      title="正在加载职位列表"
      :rows="8"
    />

    <PageState
      v-else-if="errorMessage"
      kind="error"
      title="职位列表加载失败"
      :description="errorMessage"
    >
      <ElButton type="primary" data-test="retry-jobs" @click="emit('retry')">
        重试
      </ElButton>
    </PageState>

    <PageState
      v-else-if="items.length === 0"
      kind="empty"
      title="没有符合条件的职位"
      description="可以调整筛选条件后重新查询"
    >
      <ElButton type="primary" plain @click="emit('reset')">
        重置筛选
      </ElButton>
    </PageState>

    <template v-else>
      <div class="job-table">
        <ElTable :data="items" stripe>
          <ElTableColumn label="职位" min-width="220">
            <template #default="{ row }">
              <ElButton
                link
                type="primary"
                class="job-title-link"
                @click="emit('selectJob', row.id)"
              >
                {{ row.title }}
              </ElButton>
              <div class="job-secondary">
                {{ formatNullableValue(row.companyName) }}
              </div>
            </template>
          </ElTableColumn>

          <ElTableColumn label="薪资" min-width="150">
            <template #default="{ row }">
              {{ salary(tableJob(row)) }}
            </template>
          </ElTableColumn>

          <ElTableColumn label="地点与要求" min-width="170">
            <template #default="{ row }">
              <div>{{ location(tableJob(row)) }}</div>
              <div class="job-secondary">
                {{ experienceAndEducation(tableJob(row)) }}
              </div>
            </template>
          </ElTableColumn>

          <ElTableColumn label="来源/状态" min-width="150">
            <template #default="{ row }">
              <div>{{ row.source }}</div>
              <ElTag :type="statusTagType(row.jobStatus)" size="small">
                {{ row.jobStatus }}
              </ElTag>
              <ElTag type="info" size="small" class="job-status-tag">
                {{ row.remoteType }}
              </ElTag>
            </template>
          </ElTableColumn>

          <ElTableColumn label="发现时间" min-width="205">
            <template #default="{ row }">
              <ElTooltip :content="row.firstSeenTime" placement="top">
                <span>首次：{{ formatDateTime(row.firstSeenTime) }}</span>
              </ElTooltip>
              <br>
              <ElTooltip :content="row.lastSeenTime" placement="top">
                <span>最近：{{ formatDateTime(row.lastSeenTime) }}</span>
              </ElTooltip>
              <div class="job-secondary">
                版本 {{ row.currentVersionNo }}
              </div>
            </template>
          </ElTableColumn>
        </ElTable>
      </div>

      <div class="job-cards" aria-label="职位列表">
        <button
          v-for="job in items"
          :key="job.id"
          type="button"
          class="job-card"
          @click="emit('selectJob', job.id)"
        >
          <span class="job-card-heading">
            <strong>{{ job.title }}</strong>
            <ElTag :type="statusTagType(job.jobStatus)" size="small">
              {{ job.jobStatus }}
            </ElTag>
          </span>
          <span>{{ formatNullableValue(job.companyName) }}</span>
          <span class="job-card-salary">{{ salary(job) }}</span>
          <span>{{ location(job) }} · {{ experienceAndEducation(job) }}</span>
          <span>办公方式：{{ job.remoteType }}</span>
          <span class="job-secondary">
            {{ job.source }} · 首次 {{ formatDateTime(job.firstSeenTime) }}
          </span>
          <span class="job-secondary">
            最近 {{ formatDateTime(job.lastSeenTime) }} · 版本
            {{ job.currentVersionNo }}
          </span>
        </button>
      </div>

      <nav
        class="jobs-pagination jobs-pagination-desktop"
        aria-label="职位结果分页"
      >
        <ElPagination
          background
          :current-page="page"
          :page-size="size"
          :page-sizes="pageSizes"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="emit('pageChange', $event)"
          @size-change="emit('sizeChange', $event)"
        />
      </nav>

      <nav
        class="jobs-pagination jobs-pagination-mobile"
        aria-label="职位结果紧凑分页"
      >
        <ElPagination
          size="small"
          background
          :current-page="page"
          :page-size="size"
          :total="total"
          :pager-count="5"
          layout="prev, pager, next"
          @current-change="emit('pageChange', $event)"
        />
      </nav>
    </template>
  </section>
</template>
