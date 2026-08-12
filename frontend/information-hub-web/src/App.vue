<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { ElButton } from 'element-plus'

import 'element-plus/dist/index.css'
import '@/styles/ai.css'
import '@/styles/recommendation.css'

import '@/styles/task017.css'
import '@/styles/task017-overflow.css'
import { authState, logout } from '@/stores/authSession'

const applicationTitle = import.meta.env.VITE_APP_TITLE ?? 'Information Hub'
const router = useRouter()
const userLabel = computed(
  () =>
    authState.currentUser.value?.displayName ||
    authState.currentUser.value?.username ||
    '',
)

async function signOut(): Promise<void> {
  await logout()
  await router.replace('/login')
}
</script>

<template>
  <div class="app-shell">
    <a class="skip-link" href="#main-content">跳到主要内容</a>

    <header class="app-header">
      <div class="app-header-inner">
        <RouterLink to="/jobs" class="app-title">
          {{ applicationTitle }}
        </RouterLink>
        <nav class="app-navigation" aria-label="主要导航">
          <RouterLink
            v-if="authState.currentUser.value"
            to="/jobs"
            class="app-navigation-link"
          >
            职位浏览
          </RouterLink>
          <RouterLink
            v-if="authState.currentUser.value"
            to="/recommendations"
            class="app-navigation-link"
          >
            职位推荐
          </RouterLink>
          <RouterLink
            v-if="authState.currentUser.value"
            to="/ai/prompts"
            class="app-navigation-link"
          >
            提示词方案
          </RouterLink>
          <RouterLink
            v-if="authState.currentUser.value"
            to="/ai/analyze"
            class="app-navigation-link"
          >
            手动分析
          </RouterLink>
          <RouterLink
            v-if="authState.currentUser.value"
            to="/ai/batches"
            class="app-navigation-link"
          >
            分析批次
          </RouterLink>
          <RouterLink
            v-if="authState.currentUser.value"
            to="/ai/schedules"
            class="app-navigation-link"
          >
            定时分析
          </RouterLink>
          <RouterLink
            v-if="authState.currentUser.value"
            to="/ai/usage"
            class="app-navigation-link"
          >
            用量统计
          </RouterLink>
          <span v-if="authState.currentUser.value" class="app-user">
            {{ userLabel }}
          </span>
          <ElButton
            v-if="authState.currentUser.value"
            class="app-logout"
            link
            @click="signOut"
          >
            退出登录
          </ElButton>
        </nav>
      </div>
    </header>

    <main id="main-content" class="app-content" tabindex="-1">
      <RouterView />
    </main>
  </div>
</template>
