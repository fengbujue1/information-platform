<script setup lang="ts">
import { RouterLink } from 'vue-router'

import '@/styles/task017.css'
import '@/styles/task017-overflow.css'

const applicationTitle = import.meta.env.VITE_APP_TITLE ?? 'Information Hub'
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
          <RouterLink to="/jobs" class="app-navigation-link">
            职位浏览
          </RouterLink>
        </nav>
      </div>
    </header>

    <main id="main-content" class="app-content" tabindex="-1">
      <RouterView />
    </main>
  </div>
</template>
