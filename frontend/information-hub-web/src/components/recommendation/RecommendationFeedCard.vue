<script setup lang="ts">
import { ElButton, ElCard, ElProgress, ElTag } from 'element-plus'

import type {
  JobDisposition,
  RecommendationFeedItem,
  RecommendationFeedbackState,
} from '@/types/recommendation'

defineProps<{
  item: RecommendationFeedItem
  busy: boolean
}>()

const emit = defineEmits<{
  feedback: [item: RecommendationFeedItem, state: RecommendationFeedbackState]
  disposition: [item: RecommendationFeedItem, state: JobDisposition]
  view: [item: RecommendationFeedItem]
}>()

function display(value: string | null): string {
  return value?.trim() || '—'
}
</script>

<template>
  <ElCard class="recommendation-card" shadow="never">
    <template #header>
      <div class="recommendation-card-heading">
        <div>
          <span class="recommendation-rank">#{{ item.rank }}</span>
          <h3>{{ item.job.title }}</h3>
          <p>{{ display(item.job.companyName) }}</p>
        </div>
        <div class="recommendation-score" :aria-label="`推荐分 ${item.finalScore}`">
          <strong>{{ item.finalScore.toFixed(1) }}</strong>
          <span>推荐分</span>
        </div>
      </div>
    </template>

    <div class="recommendation-job-meta">
      <span>{{ display(item.job.salaryText) }}</span>
      <span>{{ display(item.job.locationName) }}</span>
      <span>{{ display(item.job.remoteType) }}</span>
      <ElTag v-if="item.viewed" size="small" type="info">已查看</ElTag>
      <ElTag
        v-if="item.feedbackState === 'INTERESTED'"
        size="small"
        type="success"
      >
        感兴趣
      </ElTag>
      <ElTag
        v-if="item.jobDisposition === 'CONTACTED'"
        size="small"
        type="warning"
      >
        已联系
      </ElTag>
    </div>

    <div class="recommendation-breakdown" aria-label="评分明细">
      <div>
        <span>AI 相关度</span>
        <ElProgress :percentage="item.scoreBreakdown.aiRelevanceScore" :stroke-width="8" />
      </div>
      <div>
        <span>画像匹配</span>
        <ElProgress :percentage="item.scoreBreakdown.profileMatchScore" :stroke-width="8" />
      </div>
      <div>
        <span>新鲜度</span>
        <ElProgress :percentage="item.scoreBreakdown.freshnessScore" :stroke-width="8" />
      </div>
    </div>

    <ul class="recommendation-reasons" aria-label="推荐原因">
      <li v-for="reason in item.reasons" :key="reason">{{ reason }}</li>
    </ul>

    <div class="recommendation-card-actions">
      <a
        v-if="item.job.sourceUrl"
        class="el-button el-button--primary"
        :href="item.job.sourceUrl"
        target="_blank"
        rel="noopener noreferrer"
        @click="emit('view', item)"
      >
        打开来源 / BOSS
      </a>
      <ElButton
        :loading="busy"
        :type="item.feedbackState === 'INTERESTED' ? 'success' : 'default'"
        @click="emit('feedback', item, item.feedbackState === 'INTERESTED' ? 'NONE' : 'INTERESTED')"
      >
        {{ item.feedbackState === 'INTERESTED' ? '取消感兴趣' : '感兴趣' }}
      </ElButton>
      <ElButton
        :loading="busy"
        @click="emit('feedback', item, 'NOT_INTERESTED')"
      >
        不感兴趣
      </ElButton>
      <ElButton
        :loading="busy"
        :type="item.jobDisposition === 'CONTACTED' ? 'warning' : 'default'"
        @click="emit('disposition', item, item.jobDisposition === 'CONTACTED' ? 'NONE' : 'CONTACTED')"
      >
        {{ item.jobDisposition === 'CONTACTED' ? '取消已联系' : '标记已联系' }}
      </ElButton>
      <ElButton
        :loading="busy"
        type="danger"
        plain
        @click="emit('disposition', item, 'CONTACTED_NOT_SUITABLE')"
      >
        已联系且不合适
      </ElButton>
    </div>
  </ElCard>
</template>
