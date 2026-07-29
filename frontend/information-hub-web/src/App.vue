<script setup lang="ts">
const applicationTitle = import.meta.env.VITE_APP_TITLE ?? 'Information Hub'
</script>

<template>
  <div class="app-shell">
    <header class="app-header">
      <span class="app-title">{{ applicationTitle }}</span>
    </header>

    <main class="app-content">
      <RouterView />
    </main>
  </div>
</template>
