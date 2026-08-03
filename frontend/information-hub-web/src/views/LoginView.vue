<script setup lang="ts">
import { ElButton, ElInput, ElMessage } from 'element-plus'
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { ApiClientError } from '@/api'
import { login } from '@/stores/authSession'
import { resolveSafeReturnPath } from '@/utils/authRedirect'

const route = useRoute()
const router = useRouter()
const submitting = ref(false)
const form = reactive({
  username: '',
  password: '',
})

async function submit(): Promise<void> {
  if (!form.username.trim() || !form.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  submitting.value = true
  try {
    await login({
      username: form.username,
      password: form.password,
    })
    await router.replace(resolveSafeReturnPath(route.query.redirect))
  } catch (error) {
    ElMessage.error(
      error instanceof ApiClientError
        ? error.message
        : '登录失败，请稍后重试',
    )
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <section class="login-view" aria-labelledby="login-title">
    <div class="login-card">
      <header class="page-heading">
        <h1 id="login-title">登录 Information Hub</h1>
        <p>使用服务端配置的账号继续访问职位信息。</p>
      </header>

      <form class="login-form" @submit.prevent="submit">
        <label for="login-username">用户名</label>
        <ElInput
          id="login-username"
          v-model="form.username"
          name="username"
          autocomplete="username"
          :disabled="submitting"
        />

        <label for="login-password">密码</label>
        <ElInput
          id="login-password"
          v-model="form.password"
          name="password"
          type="password"
          autocomplete="current-password"
          show-password
          :disabled="submitting"
        />

        <ElButton
          native-type="submit"
          type="primary"
          :loading="submitting"
        >
          登录
        </ElButton>
      </form>
    </div>
  </section>
</template>
