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

import type { JobSnapshot } from '@/types/job'
import {
  formatDateTime,
  formatNullableValue,
} from '@/utils/formatters'
import { formatJsonForDisplay } from '@/utils/jsonDisplay'

const props = defineProps<{
  snapshot: JobSnapshot
}>()

const standardizedJson = computed(() =>
  formatJsonForDisplay(props.snapshot.standardizedPayload),
)
</script>

<template>
  <article class="snapshot-content" aria-labelledby="snapshot-content-title">
    <header class="snapshot-content-heading">
      <div>
        <ElTag type="info">版本 {{ snapshot.versionNo }}</ElTag>
        <h2 id="snapshot-content-title">
          {{ formatNullableValue(snapshot.title) }}
        </h2>
      </div>
    </header>

    <ElCard shadow="never" class="snapshot-card">
      <template #header>
        <h3>版本信息</h3>
      </template>
      <ElDescriptions :column="2" border>
        <ElDescriptionsItem label="快照创建时间">
          <span :title="snapshot.createdAt">
            {{ formatDateTime(snapshot.createdAt) }}
          </span>
        </ElDescriptionsItem>
        <ElDescriptionsItem label="采集时间">
          <span :title="snapshot.collectedAt">
            {{ formatDateTime(snapshot.collectedAt) }}
          </span>
        </ElDescriptionsItem>
        <ElDescriptionsItem label="Collector">
          {{ formatNullableValue(snapshot.collectorId) }}
        </ElDescriptionsItem>
        <ElDescriptionsItem label="Collector 版本">
          {{ formatNullableValue(snapshot.collectorVersion) }}
        </ElDescriptionsItem>
        <ElDescriptionsItem label="内容 Hash" :span="2">
          <code class="snapshot-hash">{{ snapshot.contentHash }}</code>
        </ElDescriptionsItem>
      </ElDescriptions>
    </ElCard>

    <ElCard shadow="never" class="snapshot-card">
      <template #header>
        <h3>该版本职位描述</h3>
      </template>
      <div class="snapshot-description">
        {{ formatNullableValue(snapshot.content) }}
      </div>
    </ElCard>

    <ElCard shadow="never" class="snapshot-card">
      <template #header>
        <h3>标准化业务 JSON</h3>
      </template>
      <pre class="snapshot-json">{{ standardizedJson }}</pre>
    </ElCard>
  </article>
</template>
