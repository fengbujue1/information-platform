<script setup lang="ts">
import { ElButton } from 'element-plus'
import 'element-plus/es/components/button/style/css'
import { RouterLink } from 'vue-router'

import PageState from '@/components/common/PageState.vue'
</script>

<template>
  <PageState
    kind="not-found"
    title="404"
    description="未找到请求的页面"
  >
    <RouterLink v-slot="{ navigate }" to="/jobs" custom>
      <ElButton type="primary" @click="navigate">
        返回职位入口
      </ElButton>
    </RouterLink>
  </PageState>
</template>
