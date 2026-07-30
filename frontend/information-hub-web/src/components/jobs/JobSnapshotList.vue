<script setup lang="ts">
import type { JobSnapshot } from '@/types/job'
import {
  formatDateTime,
  formatNullableValue,
} from '@/utils/formatters'

defineProps<{
  snapshots: JobSnapshot[]
  selectedSnapshotId: number | null
}>()

const emit = defineEmits<{
  select: [snapshotId: number]
}>()
</script>

<template>
  <section class="snapshot-list-panel" aria-labelledby="snapshot-list-title">
    <div class="snapshot-section-heading">
      <h2 id="snapshot-list-title">历史版本</h2>
      <span>{{ snapshots.length }} 个版本</span>
    </div>

    <ul class="snapshot-list">
      <li v-for="snapshot in snapshots" :key="snapshot.id">
        <button
          type="button"
          class="snapshot-option"
          :class="{ 'is-selected': snapshot.id === selectedSnapshotId }"
          :aria-pressed="snapshot.id === selectedSnapshotId"
          :aria-current="
            snapshot.id === selectedSnapshotId ? 'true' : undefined
          "
          :data-test="`snapshot-option-${snapshot.versionNo}`"
          @click="emit('select', snapshot.id)"
        >
          <span class="snapshot-option-heading">
            <strong>版本 {{ snapshot.versionNo }}</strong>
            <span>{{ formatDateTime(snapshot.createdAt) }}</span>
          </span>
          <span class="snapshot-option-title">
            {{ formatNullableValue(snapshot.title) }}
          </span>
          <span class="snapshot-option-meta">
            采集于 {{ formatDateTime(snapshot.collectedAt) }}
          </span>
        </button>
      </li>
    </ul>
  </section>
</template>
