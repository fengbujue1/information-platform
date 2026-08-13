<script setup lang="ts">
import { onMounted, ref } from 'vue'
import {
  ElButton,
  ElCard,
  ElInput,
  ElInputNumber,
  ElOption,
  ElSelect,
  ElSwitch,
  ElTimePicker,
} from 'element-plus'

import { toApiClientError } from '@/api/apiError'
import { listPromptProfiles } from '@/api/promptApi'
import {
  createAnalysisSchedule,
  listAnalysisSchedules,
  previewAnalysisSchedule,
  updateAnalysisSchedule,
  updateAnalysisScheduleStatus,
} from '@/api/scheduleApi'
import PageState from '@/components/common/PageState.vue'
import type { AnalysisPreview } from '@/types/batch'
import type { PromptProfile } from '@/types/prompt'
import type {
  AnalysisSchedule,
  SaveAnalysisScheduleRequest,
} from '@/types/schedule'
import { authState } from '@/stores/authSession'
import { formatDateTime, formatNullableValue } from '@/utils/formatters'
import {
  formatBatchStatus,
  formatReason,
} from '@/utils/aiDisplay'
import { showError, showSuccess, showWarning } from '@/utils/userFeedback'

const schedules = ref<AnalysisSchedule[]>([])
const profiles = ref<PromptProfile[]>([])
const editingId = ref<number | null>(null)
const form = ref<SaveAnalysisScheduleRequest>(defaultForm())
const preview = ref<AnalysisPreview | null>(null)
const loading = ref(true)
const saving = ref(false)
const error = ref<string | null>(null)

function defaultForm(): SaveAnalysisScheduleRequest {
  return {
    name: '',
    promptProfileId: 0,
    localTime: '02:00:00',
    timezone:
      authState.currentUser.value?.timezone || 'Asia/Shanghai',
    windowDays: 3,
    maxCandidates: 20,
    maxEstimatedTokens: 75_000,
    enabled: false,
  }
}

async function load(): Promise<void> {
  loading.value = true
  error.value = null
  try {
    const [scheduleList, profileList] = await Promise.all([
      listAnalysisSchedules(),
      listPromptProfiles(),
    ])
    schedules.value = scheduleList
    profiles.value = profileList
    if (!form.value.promptProfileId && profileList.length > 0) {
      form.value.promptProfileId = profileList[0]!.id
    }
  } catch (cause) {
    error.value = toApiClientError(cause).message
  } finally {
    loading.value = false
  }
}

function edit(schedule: AnalysisSchedule): void {
  editingId.value = schedule.id
  preview.value = null
  form.value = {
    name: schedule.name,
    promptProfileId: schedule.promptProfileId,
    localTime: schedule.localTime,
    timezone: schedule.timezone,
    windowDays: schedule.windowDays,
    maxCandidates: schedule.maxCandidates,
    maxEstimatedTokens: schedule.maxEstimatedTokens,
    enabled: schedule.enabled,
  }
}

function resetForm(): void {
  editingId.value = null
  preview.value = null
  form.value = defaultForm()
  if (profiles.value.length > 0) {
    form.value.promptProfileId = profiles.value[0]!.id
  }
}

async function save(): Promise<void> {
  if (!form.value.name.trim() || !form.value.promptProfileId) {
    showWarning('请输入名称并选择提示词方案')
    return
  }
  saving.value = true
  error.value = null
  try {
    if (editingId.value === null) {
      await createAnalysisSchedule({
        ...form.value,
        name: form.value.name.trim(),
        enabled: false,
      })
    } else {
      await updateAnalysisSchedule(editingId.value, {
        name: form.value.name.trim(),
        promptProfileId: form.value.promptProfileId,
        localTime: form.value.localTime,
        timezone: form.value.timezone,
        windowDays: form.value.windowDays,
        maxCandidates: form.value.maxCandidates,
        maxEstimatedTokens: form.value.maxEstimatedTokens,
      })
    }
    resetForm()
    await load()
    showSuccess('定时分析配置已保存')
  } catch (cause) {
    showError(cause)
  } finally {
    saving.value = false
  }
}

async function toggle(schedule: AnalysisSchedule, enabled: boolean): Promise<void> {
  saving.value = true
  error.value = null
  try {
    await updateAnalysisScheduleStatus(schedule.id, enabled)
    await load()
    showSuccess(enabled ? '定时分析已启用' : '定时分析已关闭')
  } catch (cause) {
    showError(cause)
    schedule.enabled = !enabled
  } finally {
    saving.value = false
  }
}

async function testPreview(scheduleId: number): Promise<void> {
  saving.value = true
  error.value = null
  preview.value = null
  try {
    preview.value = await previewAnalysisSchedule(scheduleId)
    editingId.value = scheduleId
    showSuccess('当前配置预览已生成')
  } catch (cause) {
    showError(cause)
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<template>
  <section class="ai-page">
    <header class="ai-page-header">
      <div>
        <h1>定时分析</h1>
        <p>定时分析新建时默认关闭；默认本地执行时间为 02:00。</p>
      </div>
      <ElButton @click="resetForm">新建定时分析</ElButton>
    </header>

    <PageState
      v-if="loading"
      kind="loading"
      title="正在加载定时分析"
    />
    <PageState
      v-else-if="error && schedules.length === 0"
      kind="error"
      title="定时分析加载失败"
      :description="error"
    >
      <ElButton type="primary" @click="load">重试</ElButton>
    </PageState>

    <template v-else>
      <p v-if="error" class="ai-error" role="alert">{{ error }}</p>
      <section class="ai-section">
        <h2>{{ editingId === null ? '新建定时分析' : `编辑定时分析 #${editingId}` }}</h2>
        <div class="ai-form-grid">
          <label class="ai-form-field">
            名称
            <ElInput v-model="form.name" maxlength="255" />
          </label>
          <label class="ai-form-field">
            提示词方案
            <ElSelect v-model="form.promptProfileId">
              <ElOption
                v-for="profile in profiles"
                :key="profile.id"
                :label="profile.name"
                :value="profile.id"
              />
            </ElSelect>
          </label>
          <label class="ai-form-field">
            本地时间
            <ElTimePicker
              v-model="form.localTime"
              format="HH:mm"
              value-format="HH:mm:ss"
              placeholder="02:00"
            />
          </label>
          <label class="ai-form-field">
            IANA 时区
            <ElInput v-model="form.timezone" placeholder="Asia/Shanghai" />
          </label>
          <label class="ai-form-field">
            候选时间范围（天）
            <ElInputNumber v-model="form.windowDays" :min="1" :max="14" />
          </label>
          <label class="ai-form-field">
            最大候选数量
            <ElInputNumber v-model="form.maxCandidates" :min="1" :max="50" />
          </label>
          <label class="ai-form-field">
            预估 Token 预算
            <ElInputNumber
              v-model="form.maxEstimatedTokens"
              :min="1"
              :max="200000"
            />
          </label>
        </div>
        <div class="ai-actions">
          <ElButton type="primary" :loading="saving" @click="save">
            保存配置
          </ElButton>
          <span class="ai-muted">
            启停状态通过下方独立开关修改，保存配置不会隐式启用。
          </span>
        </div>
      </section>

      <section class="ai-section">
        <h2>已保存的定时分析</h2>
        <p v-if="schedules.length === 0" class="ai-muted">
          尚无定时分析。
        </p>
        <div class="ai-card-list">
          <ElCard
            v-for="schedule in schedules"
            :key="schedule.id"
            shadow="never"
          >
            <template #header>
              <div class="ai-card-heading">
                <strong>{{ schedule.name }}</strong>
                <ElSwitch
                  v-model="schedule.enabled"
                  :loading="saving"
                  active-text="启用"
                  inactive-text="关闭"
                  @change="(value: string | number | boolean) => toggle(schedule, Boolean(value))"
                />
              </div>
            </template>
            <p>
              {{ schedule.localTime }} · {{ schedule.timezone }} ·
              最近 {{ schedule.windowDays }} 天 ·
              最大 {{ schedule.maxCandidates }} 条
            </p>
            <p class="ai-muted">
              上次：{{ schedule.lastRun ? `${formatBatchStatus(schedule.lastRun.status)} / ${formatDateTime(schedule.lastRun.scheduledFor)}` : '—' }}
              · 下次：{{ formatDateTime(schedule.nextRunAt) }}
              · 原因：{{ formatReason(schedule.lastRun?.skipReason) }}
            </p>
            <div class="ai-actions">
              <ElButton link @click="edit(schedule)">编辑</ElButton>
              <ElButton
                link
                :loading="saving"
                @click="testPreview(schedule.id)"
              >
                预览当前配置
              </ElButton>
              <RouterLink
                v-if="schedule.lastRun"
                :to="`/ai/batches/${schedule.lastRun.batchId}`"
              >
                查看上次分析批次
              </RouterLink>
            </div>
          </ElCard>
        </div>
      </section>

      <section v-if="preview" class="ai-section">
        <h2>定时分析预览</h2>
        <div class="ai-metric-grid">
          <div class="ai-metric">
            <span class="ai-metric-label">窗口内总数 / 符合条件</span>
            <strong class="ai-metric-value">
              {{ preview.totalInWindow }} / {{ preview.eligibleCount }}
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
          此预览不创建分析批次或模型调用，也不调用模型服务。
        </p>
      </section>
    </template>
  </section>
</template>
