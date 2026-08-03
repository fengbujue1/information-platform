<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { ElButton } from 'element-plus'

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
