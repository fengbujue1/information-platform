<script setup lang="ts">
import type { AnalysisUsagePeriod } from '@/types/usage'
import { formatNullableValue } from '@/utils/formatters'

defineProps<{
  title: string
  usage: AnalysisUsagePeriod
}>()
</script>

<template>
  <section class="ai-section">
    <h2>{{ title }}</h2>
    <div class="ai-metric-grid">
      <div class="ai-metric">
        <span class="ai-metric-label">实际 Token 总数</span>
        <strong class="ai-metric-value">
          {{ formatNullableValue(usage.actualTotalTokens) }}
        </strong>
      </div>
      <div class="ai-metric">
        <span class="ai-metric-label">实际输入 Token</span>
        <strong class="ai-metric-value">
          {{ formatNullableValue(usage.actualInputTokens) }}
        </strong>
      </div>
      <div class="ai-metric">
        <span class="ai-metric-label">实际输出 Token</span>
        <strong class="ai-metric-value">
          {{ formatNullableValue(usage.actualOutputTokens) }}
        </strong>
      </div>
      <div class="ai-metric">
        <span class="ai-metric-label">调用次数</span>
        <strong class="ai-metric-value">{{ usage.invocationCount }}</strong>
        <small>
          已报告 {{ usage.reportedInvocationCount }} /
          未报告 {{ usage.unavailableInvocationCount }}
        </small>
      </div>
    </div>
  </section>
</template>
