<script setup lang="ts">
import { computed } from 'vue'
import { ElDescriptions, ElDescriptionsItem, ElTag } from 'element-plus'

import type { InformationAnalysis } from '@/types/analysis'
import { formatDateTime, formatNullableValue } from '@/utils/formatters'
import {
  formatAnalysisStatus,
  formatExecutionStatus,
  formatUsageStatus,
} from '@/utils/aiDisplay'

const props = defineProps<{
  analysis: InformationAnalysis
}>()

const resultText = computed(() =>
  props.analysis.resultJson === null
    ? ''
    : JSON.stringify(props.analysis.resultJson, null, 2),
)
</script>

<template>
  <section class="ai-section">
    <div class="ai-card-heading">
      <h2>分析 #{{ analysis.id }}</h2>
      <ElTag
        :type="analysis.status === 'SUCCEEDED' ? 'success' : analysis.status === 'FAILED' ? 'danger' : 'info'"
      >
        {{ formatAnalysisStatus(analysis.status) }}
      </ElTag>
    </div>

    <ElDescriptions :column="2" border>
      <ElDescriptionsItem label="信息 ID">
        {{ analysis.informationId }}
      </ElDescriptionsItem>
      <ElDescriptionsItem label="快照 ID">
        {{ analysis.snapshotId }}
      </ElDescriptionsItem>
      <ElDescriptionsItem label="提示词版本 ID">
        {{ analysis.promptVersionId }}
      </ElDescriptionsItem>
      <ElDescriptionsItem label="相关度">
        {{ formatNullableValue(analysis.relevanceScore) }}
      </ElDescriptionsItem>
      <ElDescriptionsItem label="预估 Token 总数">
        {{ formatNullableValue(analysis.estimatedTotalTokens) }}
      </ElDescriptionsItem>
      <ElDescriptionsItem label="完成时间">
        {{ formatDateTime(analysis.completedAt) }}
      </ElDescriptionsItem>
    </ElDescriptions>

    <h3>摘要</h3>
    <p>{{ formatNullableValue(analysis.summary) }}</p>

    <template v-if="analysis.failureCode || analysis.failureMessage">
      <h3>失败信息</h3>
      <p class="ai-error">
        {{ formatNullableValue(analysis.failureCode) }}：
        {{ formatNullableValue(analysis.failureMessage) }}
      </p>
    </template>

    <template v-if="resultText">
      <h3>结构化结果</h3>
      <pre class="ai-json">{{ resultText }}</pre>
    </template>

    <h3>模型调用记录</h3>
    <p v-if="analysis.invocations.length === 0" class="ai-muted">
      尚无模型调用记录
    </p>
    <ElDescriptions
      v-for="invocation in analysis.invocations"
      v-else
      :key="invocation.id"
      :column="2"
      border
    >
      <ElDescriptionsItem label="尝试次数 / 状态">
        {{ invocation.attemptNo }} / {{ formatExecutionStatus(invocation.status) }}
      </ElDescriptionsItem>
      <ElDescriptionsItem label="模型服务 / 模型">
        {{ invocation.provider }} / {{ invocation.modelName }}
      </ElDescriptionsItem>
      <ElDescriptionsItem label="实际 Token 总数">
        {{ formatNullableValue(invocation.totalTokens) }}
      </ElDescriptionsItem>
      <ElDescriptionsItem label="用量报告状态">
        {{ formatUsageStatus(invocation.usageStatus) }}
      </ElDescriptionsItem>
    </ElDescriptions>
  </section>
</template>
