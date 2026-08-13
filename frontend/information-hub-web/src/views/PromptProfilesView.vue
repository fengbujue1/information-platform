<script setup lang="ts">
import { onMounted, ref } from 'vue'
import {
  ElButton,
  ElCard,
  ElInput,
  ElSelect,
  ElOption,
  ElTag,
} from 'element-plus'

import {
  activatePromptVersion,
  createPromptProfile,
  createPromptVersion,
  listPromptProfiles,
  listPromptVersions,
  updatePromptProfileStatus,
} from '@/api/promptApi'
import { toApiClientError } from '@/api/apiError'
import PageState from '@/components/common/PageState.vue'
import type { PromptProfile, PromptVersion } from '@/types/prompt'
import { formatDateTime } from '@/utils/formatters'
import { formatPromptProfileStatus } from '@/utils/aiDisplay'
import { showError, showSuccess, showWarning } from '@/utils/userFeedback'

const profiles = ref<PromptProfile[]>([])
const selectedProfileId = ref<number | null>(null)
const versions = ref<PromptVersion[]>([])
const profileName = ref('')
const versionContent = ref('')
const loading = ref(true)
const saving = ref(false)
const error = ref<string | null>(null)

async function loadProfiles(): Promise<void> {
  loading.value = true
  error.value = null
  try {
    profiles.value = await listPromptProfiles()
    if (
      selectedProfileId.value === null &&
      profiles.value.length > 0
    ) {
      selectedProfileId.value = profiles.value[0]!.id
    }
    await loadVersions()
  } catch (cause) {
    error.value = toApiClientError(cause).message
  } finally {
    loading.value = false
  }
}

async function loadVersions(): Promise<void> {
  if (selectedProfileId.value === null) {
    versions.value = []
    return
  }
  try {
    versions.value = await listPromptVersions(selectedProfileId.value)
  } catch (cause) {
    error.value = toApiClientError(cause).message
  }
}

async function addProfile(): Promise<void> {
  if (!profileName.value.trim()) {
    showWarning('请输入提示词方案名称')
    return
  }
  saving.value = true
  error.value = null
  try {
    const created = await createPromptProfile({
      name: profileName.value.trim(),
      analysisDefinitionKey: 'JOB_USER_RELEVANCE',
    })
    profiles.value = [created, ...profiles.value]
    selectedProfileId.value = created.id
    versions.value = []
    profileName.value = ''
    showSuccess('提示词方案已创建')
  } catch (cause) {
    showError(cause)
  } finally {
    saving.value = false
  }
}

async function addVersion(): Promise<void> {
  if (selectedProfileId.value === null || !versionContent.value.trim()) {
    showWarning('请选择提示词方案并输入用户提示词')
    return
  }
  saving.value = true
  error.value = null
  try {
    const version = await createPromptVersion(
      selectedProfileId.value,
      { content: versionContent.value.trim() },
    )
    await activatePromptVersion(selectedProfileId.value, version.id)
    versionContent.value = ''
    await loadProfiles()
    showSuccess('新版本已创建并设为生效版本')
  } catch (cause) {
    showError(cause)
  } finally {
    saving.value = false
  }
}

async function activate(versionId: number): Promise<void> {
  if (selectedProfileId.value === null) return
  saving.value = true
  error.value = null
  try {
    await activatePromptVersion(selectedProfileId.value, versionId)
    await loadProfiles()
    showSuccess('生效版本已更新')
  } catch (cause) {
    showError(cause)
  } finally {
    saving.value = false
  }
}

async function toggleStatus(profile: PromptProfile): Promise<void> {
  saving.value = true
  error.value = null
  try {
    await updatePromptProfileStatus(
      profile.id,
      profile.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE',
    )
    await loadProfiles()
    showSuccess(profile.status === 'ACTIVE' ? '提示词方案已停用' : '提示词方案已启用')
  } catch (cause) {
    showError(cause)
  } finally {
    saving.value = false
  }
}

function currentProfile(): PromptProfile | undefined {
  return profiles.value.find(
    (profile) => profile.id === selectedProfileId.value,
  )
}

onMounted(loadProfiles)
</script>

<template>
  <section class="ai-page">
    <header class="ai-page-header">
      <div>
        <h1>提示词方案</h1>
        <p>每次修改都会创建不可变版本，历史正文不会被覆盖。</p>
      </div>
    </header>

    <PageState
      v-if="loading"
      kind="loading"
      title="正在加载提示词方案"
    />
    <PageState
      v-else-if="error && profiles.length === 0"
      kind="error"
      title="提示词方案加载失败"
      :description="error"
    >
      <ElButton type="primary" @click="loadProfiles">重试</ElButton>
    </PageState>

    <template v-else>
      <p v-if="error" class="ai-error" role="alert">{{ error }}</p>

      <section class="ai-section">
        <h2>新建提示词方案</h2>
        <div class="ai-toolbar">
          <ElInput
            v-model="profileName"
            maxlength="255"
            placeholder="例如：Java 后端职位"
          />
          <ElButton type="primary" :loading="saving" @click="addProfile">
            创建
          </ElButton>
        </div>
      </section>

      <section class="ai-section">
        <h2>提示词方案与版本</h2>
        <p v-if="profiles.length === 0" class="ai-muted">
          尚无提示词方案，请先创建。
        </p>
        <template v-else>
          <div class="ai-toolbar">
            <ElSelect
              v-model="selectedProfileId"
              aria-label="选择提示词方案"
              @change="loadVersions"
            >
              <ElOption
                v-for="profile in profiles"
                :key="profile.id"
                :label="profile.name"
                :value="profile.id"
              />
            </ElSelect>
            <ElTag :type="currentProfile()?.status === 'ACTIVE' ? 'success' : 'info'">
              {{ formatPromptProfileStatus(currentProfile()?.status) }}
            </ElTag>
            <ElButton
              :loading="saving"
              @click="currentProfile() && toggleStatus(currentProfile()!)"
            >
              {{ currentProfile()?.status === 'ACTIVE' ? '停用' : '启用' }}
            </ElButton>
          </div>

          <div class="ai-form-field ai-form-field-wide">
            <label for="prompt-content">创建新版本</label>
            <ElInput
              id="prompt-content"
              v-model="versionContent"
              type="textarea"
              :rows="6"
              maxlength="8000"
              show-word-limit
              placeholder="只填写用户提示词；系统提示词与输出结构由平台控制"
            />
            <ElButton
              type="primary"
              :loading="saving"
              @click="addVersion"
            >
              创建并启用新版本
            </ElButton>
          </div>

          <div class="ai-card-list">
            <ElCard v-for="version in versions" :key="version.id" shadow="never">
              <template #header>
                <div class="ai-card-heading">
                  <strong>版本 {{ version.versionNo }}</strong>
                  <ElTag
                    v-if="currentProfile()?.activeVersionId === version.id"
                    type="success"
                  >
                    当前生效
                  </ElTag>
                  <ElButton
                    v-else
                    link
                    :loading="saving"
                    @click="activate(version.id)"
                  >
                    设为生效版本
                  </ElButton>
                </div>
              </template>
              <p>{{ version.content }}</p>
              <small>
                创建：{{ formatDateTime(version.createdAt) }} ·
                Hash：{{ version.contentHash }}
              </small>
            </ElCard>
            <p v-if="versions.length === 0" class="ai-muted">
              当前提示词方案尚无版本，不能用于分析。
            </p>
          </div>
        </template>
      </section>
    </template>
  </section>
</template>
