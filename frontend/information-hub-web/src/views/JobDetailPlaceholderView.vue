<script setup lang="ts">
import { computed } from 'vue'
import { ElEmpty } from 'element-plus'
import 'element-plus/es/components/empty/style/css'
import { RouterLink, useRoute } from 'vue-router'

const route = useRoute()

const backTarget = computed(() => {
  const candidate = Array.isArray(route.query.from)
    ? route.query.from[0]
    : route.query.from
  return candidate === '/jobs' || candidate?.startsWith('/jobs?')
    ? candidate
    : '/jobs'
})
</script>

<template>
  <section aria-labelledby="job-detail-placeholder-title">
    <h1 id="job-detail-placeholder-title">职位详情</h1>
    <ElEmpty description="职位详情内容将在 TASK-015 中实现">
      <RouterLink :to="backTarget">返回职位列表</RouterLink>
    </ElEmpty>
  </section>
</template>
