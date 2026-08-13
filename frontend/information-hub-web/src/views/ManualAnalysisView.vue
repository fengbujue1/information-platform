<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  ElButton,
  ElInputNumber,
  ElOption,
  ElSelect,
} from 'element-plus'

import { executeAnalysis } from '@/api/analysisApi'
import {
  confirmAnalysisBatch,
  createAnalysisPreview,
} from '@/api/batchApi'
import { toApiClientError } from '@/api/apiError'
import { listPromptProfiles } from '@/api/promptApi'
import AnalysisResultPanel from '@/components/ai/AnalysisResultPanel.vue'
import PageState from '@/components/common/PageState.vue'
import type { InformationAnalysis } from '@/types/analysis'
import type { AnalysisPreview } from '@/types/batch'
import type { PromptProfile } from '@/types/prompt'
import { formatDateTime } from '@/utils/formatters'
import { showError, showSuccess, showWarning } from '@/utils/userFeedback'

const route = useRoute()
const router = useRouter()
const profiles = ref<PromptProfile[]>([])
const promptProfileId = ref<number | null>(null)
const informationId = ref<number | null>(readPositiveNumber(route.query.informationId))
const windowDays = ref(3)
const maxCandidates = ref(20)
const maxEstimatedTokens = ref(75_000)
const preview = ref<AnalysisPreview | null>(null)
const analysis = ref<InformationAnalysis | null>(null)
const loading = ref(true)
const submitting = ref(false)
const error = ref<string | null>(null)

const executableProfiles = computed(() =>
  profiles.value.filter(
    (profile) =>
      profile.status === 'ACTIVE' && profile.activeVersionId !== null,
  ),
)

function readPositiveNumber(value: unknown): number | null {
  const raw = Array.isArray(value) ? value[0] : value
  const parsed = typeof raw === 'string' ? Number(raw) : Number.NaN
  return Number.isSafeInteger(parsed) && parsed > 0 ? parsed : null
}

async function load(): Promise<void> {
  loading.value = true
  error.value = null
  try {
    profiles.value = await listPromptProfiles()
    promptProfileId.value = executableProfiles.value[0]?.id ?? null
  } catch (cause) {
    error.value = toApiClientError(cause).message
  } finally {
    loading.value = false
  }
}

async function runPreview(): Promise<void> {
  if (promptProfileId.value === null) {
    showWarning('请先创建并启用提示词版本')
    return
  }
  submitting.value = true
  error.value = null
  preview.value = null
  try {
    preview.value = await createAnalysisPreview({
      promptProfileId: promptProfileId.value,
      windowDays: windowDays.value,
      maxCandidates: maxCandidates.value,
      maxEstimatedTokens: maxEstimatedTokens.value,
    })
    showSuccess('批次预览已生成')
  } catch (cause) {
    showError(cause)
  } finally {
    submitting.value = false
  }
}

async function confirmBatch(): Promise<void> {
  if (!preview.value) return
  submitting.value = true
  error.value = null
  try {
    const batch = await confirmAnalysisBatch(preview.value.previewToken)
    await router.push(`/ai/batches/${batch.id}`)
  } catch (cause) {
    error.value = toApiClientError(cause).message
  } finally {
    submitting.value = false
  }
}

async function runSingle(): Promise<void> {
  if (promptProfileId.value === null || informationId.value === null) {
    showWarning('请输入有效的信息 ID 并选择可执行的提示词方案')
    return
  }
  submitting.value = true
  error.value = null
  analysis.value = null
  try {
    analysis.value = await executeAnalysis({
      informationId: informationId.value,
      promptProfileId: promptProfileId.value,
    })
    showSuccess('单条分析已完成')
  } catch (cause) {
    showError(cause)
  } finally {
    submitting.value = false
  }
}

onMounted(load)
</script>

<template>
  <section class="ai-page">
    <header class="ai-page-header">
      <div>
        <h1>手动分析</h1>
        <p>预览只读取候选并估算 Token，不调用模型服务。</p>
      </div>
    </header>

    <PageState
      v-if="loading"
      kind="loading"
      title="正在加载可用提示词方案"
    />
    <PageState
      v-else-if="error && profiles.length === 0"
      kind="error"
      title="提示词方案加载失败"
      :description="error"
    >
      <ElButton type="primary" @click="load">重试</ElButton>
    </PageState>

    <template v-else>
      <p v-if="error" class="ai-error" role="alert">{{ error }}</p>
      <PageState
        v-if="executableProfiles.length === 0"
        kind="empty"
        title="没有可执行的提示词方案"
        description="请先创建提示词版本、设为生效版本，并确保提示词方案已启用。"
      >
        <RouterLink to="/ai/prompts">管理提示词方案</RouterLink>
      </PageState>

      <template v-else>
        <section class="ai-section">
          <h2>最近 N 天分析批次</h2>
          <div class="ai-form-grid">
            <label class="ai-form-field">
              提示词方案
              <ElSelect v-model="promptProfileId">
                <ElOption
                  v-for="profile in executableProfiles"
                  :key="profile.id"
                  :label="profile.name"
                  :value="profile.id"
                />
              </ElSelect>
            </label>
            <label class="ai-form-field">
              候选时间范围（天）
              <ElInputNumber v-model="windowDays" :min="1" :max="14" />
            </label>
            <label class="ai-form-field">
              最大候选数量
              <ElInputNumber v-model="maxCandidates" :min="1" :max="50" />
            </label>
            <label class="ai-form-field">
              预估 Token 预算
              <ElInputNumber
                v-model="maxEstimatedTokens"
                :min="1"
                :max="200000"
              />
            </label>
          </div>
          <div class="ai-actions">
            <ElButton
              type="primary"
              :loading="submitting"
              @click="runPreview"
            >
              预览
            </ElButton>
          </div>
        </section>

        <section v-if="preview" class="ai-section">
          <h2>预览结果</h2>
          <div class="ai-metric-grid">
            <div class="ai-metric">
              <span class="ai-metric-label">窗口内总数 / 符合条件</span>
              <strong class="ai-metric-value">
                {{ preview.totalInWindow }} / {{ preview.eligibleCount }}
              </strong>
            </div>
            <div class="ai-metric">
              <span class="ai-metric-label">待分析 / 已分析</span>
              <strong class="ai-metric-value">
                {{ preview.pendingCount }} / {{ preview.alreadyAnalyzedCount }}
              </strong>
            </div>
            <div class="ai-metric">
              <span class="ai-metric-label">已选择</span>
              <strong class="ai-metric-value">{{ preview.selectedCount }}</strong>
            </div>
            <div class="ai-metric">
              <span class="ai-metric-label">预估 Token 总数</span>
              <strong class="ai-metric-value">
                {{ preview.estimatedTotalTokens }}
              </strong>
            </div>
          </div>
          <p class="ai-muted">
            Token 到期：{{ formatDateTime(preview.expiresAt) }}。
            预览不产生实际 Token。
          </p>
          <ElButton
            type="success"
            :disabled="preview.selectedCount === 0"
            :loading="submitting"
            @click="confirmBatch"
          >
            确认并创建分析批次
          </ElButton>
        </section>

        <section class="ai-section">
          <h2>单条信息分析</h2>
          <div class="ai-toolbar">
            <ElInputNumber
              v-model="informationId"
              :min="1"
              placeholder="信息 ID"
            />
            <ElButton
              type="primary"
              :loading="submitting"
              @click="runSingle"
            >
              执行单条分析
            </ElButton>
          </div>
        </section>

        <AnalysisResultPanel v-if="analysis" :analysis="analysis" />
      </template>
    </template>
  </section>
</template>
