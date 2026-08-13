<script setup lang="ts">
import { reactive, watch } from 'vue'
import {
  ElButton,
  ElInputNumber,
  ElOption,
  ElSelect,
} from 'element-plus'

import type { PromptProfile } from '@/types/prompt'
import type {
  RecommendationProfile,
  RecommendationProfileRequest,
  RecommendationRemoteType,
} from '@/types/recommendation'

const props = defineProps<{
  profile: RecommendationProfile | null
  promptProfiles: PromptProfile[]
  saving: boolean
}>()

const emit = defineEmits<{
  save: [request: RecommendationProfileRequest]
}>()

interface ProfileFormState {
  analysisPromptProfileId: number | null
  windowDays: number
  topN: number
  targetRoles: string[]
  preferredSkills: string[]
  preferredCities: string[]
  preferredRemoteTypes: RecommendationRemoteType[]
  salaryMinMonthlyYuan: number | null
  excludedKeywords: string[]
}

const form = reactive<ProfileFormState>(emptyForm())

function emptyForm(): ProfileFormState {
  return {
    analysisPromptProfileId: null,
    windowDays: 7,
    topN: 50,
    targetRoles: [],
    preferredSkills: [],
    preferredCities: [],
    preferredRemoteTypes: [],
    salaryMinMonthlyYuan: null,
    excludedKeywords: [],
  }
}

function replaceForm(profile: RecommendationProfile | null): void {
  Object.assign(form, profile ? {
    analysisPromptProfileId: profile.analysisPromptProfileId,
    windowDays: profile.windowDays,
    topN: profile.topN,
    targetRoles: [...profile.targetRoles],
    preferredSkills: [...profile.preferredSkills],
    preferredCities: [...profile.preferredCities],
    preferredRemoteTypes: [...profile.preferredRemoteTypes],
    salaryMinMonthlyYuan: profile.salaryMinMonthlyYuan,
    excludedKeywords: [...profile.excludedKeywords],
  } : emptyForm())
}

function submit(): void {
  if (form.analysisPromptProfileId === null) return
  emit('save', {
    analysisPromptProfileId: form.analysisPromptProfileId,
    windowDays: form.windowDays,
    topN: form.topN,
    targetRoles: [...form.targetRoles],
    preferredSkills: [...form.preferredSkills],
    preferredCities: [...form.preferredCities],
    preferredRemoteTypes: [...form.preferredRemoteTypes],
    salaryMinMonthlyYuan: form.salaryMinMonthlyYuan,
    excludedKeywords: [...form.excludedKeywords],
  })
}

watch(() => props.profile, replaceForm, { immediate: true })
</script>

<template>
  <form class="recommendation-profile-form" @submit.prevent="submit">
    <div class="recommendation-field recommendation-field-wide">
      <label for="recommendation-prompt-profile">提示词方案</label>
      <ElSelect
        id="recommendation-prompt-profile"
        v-model="form.analysisPromptProfileId"
        placeholder="选择已启用且具有生效版本的提示词方案"
      >
        <ElOption
          v-for="prompt in promptProfiles"
          :key="prompt.id"
          :label="prompt.name"
          :value="prompt.id"
          :disabled="prompt.status !== 'ACTIVE' || prompt.activeVersionId === null"
        />
      </ElSelect>
      <small>推荐只使用该提示词方案已有的职位用户相关性分析。</small>
    </div>

    <div class="recommendation-field">
      <label for="recommendation-window-days">候选窗口（天）</label>
      <ElInputNumber
        id="recommendation-window-days"
        v-model="form.windowDays"
        :min="1"
        :max="30"
        controls-position="right"
      />
    </div>
    <div class="recommendation-field">
      <label for="recommendation-top-n">每轮推荐数量</label>
      <ElInputNumber
        id="recommendation-top-n"
        v-model="form.topN"
        :min="1"
        :max="100"
        controls-position="right"
      />
    </div>
    <div class="recommendation-field">
      <label for="recommendation-salary">最低月薪（元）</label>
      <ElInputNumber
        id="recommendation-salary"
        v-model="form.salaryMinMonthlyYuan"
        :min="0"
        :step="1000"
        controls-position="right"
        placeholder="不限"
      />
    </div>

    <div class="recommendation-field recommendation-field-wide">
      <label for="recommendation-target-roles">目标岗位</label>
      <ElSelect
        id="recommendation-target-roles"
        v-model="form.targetRoles"
        multiple
        filterable
        allow-create
        default-first-option
        placeholder="输入后按 Enter，例如 Java 后端"
      />
    </div>
    <div class="recommendation-field recommendation-field-wide">
      <label for="recommendation-skills">偏好技能</label>
      <ElSelect
        id="recommendation-skills"
        v-model="form.preferredSkills"
        multiple
        filterable
        allow-create
        default-first-option
        placeholder="输入后按 Enter，例如 Spring Boot"
      />
    </div>
    <div class="recommendation-field recommendation-field-wide">
      <label for="recommendation-cities">偏好城市</label>
      <ElSelect
        id="recommendation-cities"
        v-model="form.preferredCities"
        multiple
        filterable
        allow-create
        default-first-option
        placeholder="输入后按 Enter，例如 成都"
      />
    </div>
    <div class="recommendation-field recommendation-field-wide">
      <label for="recommendation-remote-types">办公方式</label>
      <ElSelect
        id="recommendation-remote-types"
        v-model="form.preferredRemoteTypes"
        multiple
        placeholder="不限"
      >
        <ElOption label="现场办公" value="ONSITE" />
        <ElOption label="混合办公" value="HYBRID" />
        <ElOption label="远程办公" value="REMOTE" />
      </ElSelect>
    </div>
    <div class="recommendation-field recommendation-field-wide">
      <label for="recommendation-excluded-keywords">排除关键词</label>
      <ElSelect
        id="recommendation-excluded-keywords"
        v-model="form.excludedKeywords"
        multiple
        filterable
        allow-create
        default-first-option
        placeholder="输入后按 Enter，例如 纯销售"
      />
      <small>命中职位标题、正文或公司名时会被硬排除。</small>
    </div>

    <div class="recommendation-form-actions">
      <ElButton
        native-type="submit"
        type="primary"
        :loading="saving"
        :disabled="form.analysisPromptProfileId === null"
      >
        保存推荐画像
      </ElButton>
      <span>保存画像不会自动运行分析或刷新推荐。</span>
    </div>
  </form>
</template>
