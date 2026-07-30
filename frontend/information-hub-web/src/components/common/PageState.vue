<script setup lang="ts">
import { computed, useSlots } from 'vue'
import { ElResult, ElSkeleton } from 'element-plus'
import 'element-plus/es/components/result/style/css'
import 'element-plus/es/components/skeleton/style/css'

type PageStateKind =
  | 'loading'
  | 'empty'
  | 'error'
  | 'not-found'
  | 'invalid'

const props = withDefaults(
  defineProps<{
    kind: PageStateKind
    title: string
    description?: string
    rows?: number
  }>(),
  {
    description: '',
    rows: 8,
  },
)

const slots = useSlots()

const resultIcon = computed<'error' | 'info' | 'warning'>(() => {
  if (props.kind === 'error') {
    return 'error'
  }
  if (props.kind === 'invalid') {
    return 'warning'
  }
  return 'info'
})

const liveRole = computed(() =>
  props.kind === 'error' ? 'alert' : 'status',
)
</script>

<template>
  <section
    class="page-state"
    :class="`page-state-${kind}`"
    :role="liveRole"
    :aria-live="kind === 'error' ? 'assertive' : 'polite'"
    :aria-busy="kind === 'loading' ? 'true' : undefined"
    :aria-label="kind === 'loading' ? title : undefined"
    :data-state="kind"
  >
    <template v-if="kind === 'loading'">
      <p class="visually-hidden">{{ title }}</p>
      <ElSkeleton :rows="rows" animated aria-hidden="true" />
    </template>

    <ElResult
      v-else
      :icon="resultIcon"
      :title="title"
      :sub-title="description"
    >
      <template v-if="slots.default" #extra>
        <div class="page-state-actions">
          <slot />
        </div>
      </template>
    </ElResult>
  </section>
</template>
