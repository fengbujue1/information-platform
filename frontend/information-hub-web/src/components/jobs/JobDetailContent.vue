<script setup lang="ts">
import { computed } from 'vue'
import {
  ElCard,
  ElDescriptions,
  ElDescriptionsItem,
  ElTag,
} from 'element-plus'
import 'element-plus/es/components/card/style/css'
import 'element-plus/es/components/descriptions/style/css'
import 'element-plus/es/components/tag/style/css'
import { RouterLink, type RouteLocationRaw } from 'vue-router'

import '@/styles/jobDetail.css'

import type { JobDetail } from '@/types/job'
import { resolveSafeExternalUrl } from '@/utils/externalLinks'
import {
  formatDateTime,
  formatDateTimeOrText,
  formatJsonValueList,
  formatNullableValue,
  formatSalary,
} from '@/utils/formatters'

const props = defineProps<{
  job: JobDetail
  returnTarget: string
  snapshotsTarget: RouteLocationRaw
}>()

const sourceUrl = computed(() =>
  resolveSafeExternalUrl(props.job.sourceUrl),
)
const companyUrl = computed(() =>
  resolveSafeExternalUrl(props.job.companyUrl),
)
const sourceTags = computed(() =>
  formatJsonValueList(props.job.sourceTags),
)
const sourceSkillTags = computed(() =>
  formatJsonValueList(props.job.sourceSkillTags),
)
const welfare = computed(() => formatJsonValueList(props.job.welfare))

function localDate(value: string | null): string {
  return formatDateTime(value)
}
</script>

<template>
  <article class="job-detail" aria-labelledby="job-detail-title">
    <header class="job-detail-heading">
      <div>
        <RouterLink :to="returnTarget" class="internal-link">
          ← 返回职位列表
        </RouterLink>
        <h1 id="job-detail-title">{{ job.title }}</h1>
        <div class="job-detail-statuses">
          <ElTag>{{ job.source }}</ElTag>
          <ElTag type="success">{{ job.jobStatus }}</ElTag>
          <ElTag type="info">{{ job.detailStatus }}</ElTag>
          <ElTag type="warning">{{ job.remoteType }}</ElTag>
        </div>
      </div>

      <div class="job-detail-actions">
        <a
          v-if="sourceUrl"
          :href="sourceUrl"
          target="_blank"
          rel="noopener noreferrer"
          class="external-link"
        >
          查看来源职位
        </a>
        <RouterLink :to="snapshotsTarget" class="internal-link">
          查看历史快照
        </RouterLink>
        <RouterLink
          :to="{ path: '/ai/analyze', query: { informationId: job.id } }"
          class="internal-link"
        >
          分析当前职位
        </RouterLink>
      </div>
    </header>

    <div class="job-detail-grid">
      <ElCard shadow="never" class="job-detail-card job-detail-card-wide">
        <template #header>
          <h2>职位概况</h2>
        </template>
        <ElDescriptions :column="2" border>
          <ElDescriptionsItem label="公司">
            {{ formatNullableValue(job.companyName) }}
          </ElDescriptionsItem>
          <ElDescriptionsItem label="薪资">
            {{ formatSalary(job) }}
          </ElDescriptionsItem>
          <ElDescriptionsItem label="工作地点">
            {{ formatNullableValue(job.locationName) }}
          </ElDescriptionsItem>
          <ElDescriptionsItem label="城市 / 区域">
            {{ formatNullableValue(job.cityName) }} /
            {{ formatNullableValue(job.areaName) }}
          </ElDescriptionsItem>
          <ElDescriptionsItem label="商圈">
            {{ formatNullableValue(job.businessDistrictName) }}
          </ElDescriptionsItem>
          <ElDescriptionsItem label="办公方式">
            {{ formatNullableValue(job.remoteType) }}
          </ElDescriptionsItem>
          <ElDescriptionsItem label="经验要求">
            {{ formatNullableValue(job.experienceText) }}
          </ElDescriptionsItem>
          <ElDescriptionsItem label="学历要求">
            {{ formatNullableValue(job.educationText) }}
          </ElDescriptionsItem>
          <ElDescriptionsItem label="薪资来源">
            {{ formatNullableValue(job.salarySource) }}
          </ElDescriptionsItem>
          <ElDescriptionsItem label="当前版本">
            {{ job.currentVersionNo }}
          </ElDescriptionsItem>
        </ElDescriptions>
      </ElCard>

      <ElCard shadow="never" class="job-detail-card">
        <template #header>
          <h2>公司信息</h2>
        </template>
        <dl class="detail-list">
          <div>
            <dt>公司名称</dt>
            <dd>{{ formatNullableValue(job.companyName) }}</dd>
          </div>
          <div>
            <dt>公司规模</dt>
            <dd>{{ formatNullableValue(job.companyScaleText) }}</dd>
          </div>
          <div>
            <dt>融资阶段</dt>
            <dd>{{ formatNullableValue(job.companyStageText) }}</dd>
          </div>
          <div>
            <dt>所属行业</dt>
            <dd>{{ formatNullableValue(job.companyIndustryText) }}</dd>
          </div>
          <div>
            <dt>公司主页</dt>
            <dd>
              <a
                v-if="companyUrl"
                :href="companyUrl"
                target="_blank"
                rel="noopener noreferrer"
                class="external-link"
              >
                打开来源公司页面
              </a>
              <span v-else>—</span>
            </dd>
          </div>
        </dl>
      </ElCard>

      <ElCard shadow="never" class="job-detail-card">
        <template #header>
          <h2>招聘者</h2>
        </template>
        <dl class="detail-list">
          <div>
            <dt>招聘者</dt>
            <dd>{{ formatNullableValue(job.recruiterName) }}</dd>
          </div>
          <div>
            <dt>职位</dt>
            <dd>{{ formatNullableValue(job.recruiterTitle) }}</dd>
          </div>
          <div>
            <dt>最近在线观测</dt>
            <dd :title="job.recruiterActiveText ?? undefined">
              {{ formatDateTimeOrText(job.recruiterActiveText) }}
            </dd>
          </div>
          <div>
            <dt>来源招聘者 ID</dt>
            <dd>{{ formatNullableValue(job.sourceRecruiterId) }}</dd>
          </div>
        </dl>
      </ElCard>

      <ElCard shadow="never" class="job-detail-card job-detail-card-wide">
        <template #header>
          <h2>标签与福利</h2>
        </template>
        <div class="detail-tag-group">
          <h3>来源标签</h3>
          <div v-if="sourceTags.length > 0" class="detail-tags">
            <ElTag v-for="tag in sourceTags" :key="tag" type="info">
              {{ tag }}
            </ElTag>
          </div>
          <span v-else>—</span>
        </div>
        <div class="detail-tag-group">
          <h3>技能标签</h3>
          <div v-if="sourceSkillTags.length > 0" class="detail-tags">
            <ElTag v-for="tag in sourceSkillTags" :key="tag">
              {{ tag }}
            </ElTag>
          </div>
          <span v-else>—</span>
        </div>
        <div class="detail-tag-group">
          <h3>福利</h3>
          <div v-if="welfare.length > 0" class="detail-tags">
            <ElTag v-for="item in welfare" :key="item" type="success">
              {{ item }}
            </ElTag>
          </div>
          <span v-else>—</span>
        </div>
      </ElCard>

      <ElCard shadow="never" class="job-detail-card job-detail-card-wide">
        <template #header>
          <h2>职位描述</h2>
        </template>
        <div class="job-description">
          {{ formatNullableValue(job.content) }}
        </div>
      </ElCard>

      <ElCard shadow="never" class="job-detail-card job-detail-card-wide">
        <template #header>
          <h2>来源与采集信息</h2>
        </template>
        <ElDescriptions :column="2" border>
          <ElDescriptionsItem label="来源职位 ID">
            {{ formatNullableValue(job.sourceItemId) }}
          </ElDescriptionsItem>
          <ElDescriptionsItem label="来源公司 ID">
            {{ formatNullableValue(job.sourceCompanyId) }}
          </ElDescriptionsItem>
          <ElDescriptionsItem label="发布时间">
            <span :title="job.publishTime ?? undefined">
              {{ localDate(job.publishTime) }}
            </span>
          </ElDescriptionsItem>
          <ElDescriptionsItem label="最近采集时间">
            <span :title="job.collectedAt">
              {{ localDate(job.collectedAt) }}
            </span>
          </ElDescriptionsItem>
          <ElDescriptionsItem label="首次发现时间">
            <span :title="job.firstSeenTime">
              {{ localDate(job.firstSeenTime) }}
            </span>
          </ElDescriptionsItem>
          <ElDescriptionsItem label="最近发现时间">
            <span :title="job.lastSeenTime">
              {{ localDate(job.lastSeenTime) }}
            </span>
          </ElDescriptionsItem>
          <ElDescriptionsItem label="详情采集时间">
            <span :title="job.detailCollectedAt ?? undefined">
              {{ localDate(job.detailCollectedAt) }}
            </span>
          </ElDescriptionsItem>
          <ElDescriptionsItem label="详情状态">
            {{ formatNullableValue(job.detailStatus) }}
          </ElDescriptionsItem>
          <ElDescriptionsItem label="Collector">
            {{ formatNullableValue(job.collectorId) }}
          </ElDescriptionsItem>
          <ElDescriptionsItem label="Collector 版本">
            {{ formatNullableValue(job.collectorVersion) }}
          </ElDescriptionsItem>
        </ElDescriptions>
      </ElCard>
    </div>
  </article>
</template>
