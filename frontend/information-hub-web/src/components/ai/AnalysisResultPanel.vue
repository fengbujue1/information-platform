<script setup lang="ts">
import { computed } from 'vue'
import { ElDescriptions, ElDescriptionsItem, ElTag } from 'element-plus'

import type { InformationAnalysis } from '@/types/analysis'
import { formatDateTime, formatNullableValue } from '@/utils/formatters'

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
      <h2>Analysis #{{ analysis.id }}</h2>
      <ElTag
        :type="analysis.status === 'SUCCEEDED' ? 'success' : analysis.status === 'FAILED' ? 'danger' : 'info'"
      >
        {{ analysis.status }}
      </ElTag>
    </div>

    <ElDescriptions :column="2" border>
      <ElDescriptionsItem label="Information">
        {{ analysis.informationId }}
      </ElDescriptionsItem>
      <ElDescriptionsItem label="Snapshot">
        {{ analysis.snapshotId }}
      </ElDescriptionsItem>
      <ElDescriptionsItem label="Prompt Version">
        {{ analysis.promptVersionId }}
      </ElDescriptionsItem>
      <ElDescriptionsItem label="相关度">
        {{ formatNullableValue(analysis.relevanceScore) }}
      </ElDescriptionsItem>
      <ElDescriptionsItem label="Estimated Total">
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

    <h3>Provider Invocation</h3>
    <p v-if="analysis.invocations.length === 0" class="ai-muted">
      尚无 Provider Invocation
    </p>
    <ElDescriptions
      v-for="invocation in analysis.invocations"
      v-else
      :key="invocation.id"
      :column="2"
      border
    >
      <ElDescriptionsItem label="Attempt">
        {{ invocation.attemptNo }} / {{ invocation.status }}
      </ElDescriptionsItem>
      <ElDescriptionsItem label="Provider / Model">
        {{ invocation.provider }} / {{ invocation.modelName }}
      </ElDescriptionsItem>
      <ElDescriptionsItem label="Actual Total">
        {{ formatNullableValue(invocation.totalTokens) }}
      </ElDescriptionsItem>
      <ElDescriptionsItem label="Usage Status">
        {{ invocation.usageStatus }}
      </ElDescriptionsItem>
    </ElDescriptions>
  </section>
</template>
