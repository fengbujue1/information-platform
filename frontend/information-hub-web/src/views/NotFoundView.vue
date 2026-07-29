<script setup lang="ts">
import { ElButton, ElResult } from 'element-plus'
import 'element-plus/es/components/button/style/css'
import 'element-plus/es/components/result/style/css'
</script>

<template>
  <ElResult
    icon="warning"
    title="404"
    sub-title="未找到请求的页面"
  >
    <template #extra>
      <RouterLink to="/jobs">
        <ElButton type="primary">返回职位入口</ElButton>
      </RouterLink>
    </template>
  </ElResult>
</template>
