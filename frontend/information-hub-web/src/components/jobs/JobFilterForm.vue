<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import {
  ElAlert,
  ElButton,
  ElInput,
  ElInputNumber,
  ElOption,
  ElSelect,
} from 'element-plus'
import 'element-plus/es/components/alert/style/css'
import 'element-plus/es/components/button/style/css'
import 'element-plus/es/components/input/style/css'
import 'element-plus/es/components/input-number/style/css'
import 'element-plus/es/components/option/style/css'
import 'element-plus/es/components/select/style/css'

import {
  JOB_SORT_OPTIONS,
  type JobListFilterDraft,
} from '@/utils/jobListQuery'

const props = defineProps<{
  state: JobListFilterDraft
  loading: boolean
}>()

const emit = defineEmits<{
  search: [draft: JobListFilterDraft]
  reset: []
}>()

const draft = reactive<JobListFilterDraft>({ ...props.state })
const validationMessage = ref('')

watch(
  () => props.state,
  (state) => {
    Object.assign(draft, state)
    validationMessage.value = ''
  },
  { deep: true },
)

function submitSearch(): void {
  if (
    draft.salaryMin !== null &&
    draft.salaryMax !== null &&
    draft.salaryMin > draft.salaryMax
  ) {
    validationMessage.value = '最低薪资不能高于最高薪资'
    return
  }

  validationMessage.value = ''
  emit('search', { ...draft })
}
</script>

<template>
  <section class="job-filter-panel" aria-labelledby="job-filter-title">
    <div class="section-heading">
      <div>
        <h2 id="job-filter-title">筛选职位</h2>
        <p>筛选条件会写入地址栏，可复制链接或使用浏览器前进、后退。</p>
      </div>
    </div>

    <ElAlert
      v-if="validationMessage"
      :title="validationMessage"
      type="warning"
      show-icon
      :closable="false"
      class="filter-validation"
    />

    <form class="job-filter-form" aria-label="职位筛选条件" @submit.prevent="submitSearch">
      <label class="filter-field">
        <span>关键词</span>
        <ElInput
          v-model="draft.keyword"
          placeholder="职位、公司或正文"
          clearable
          :disabled="loading"
        />
      </label>

      <label class="filter-field">
        <span>公司</span>
        <ElInput
          v-model="draft.company"
          placeholder="公司名称"
          clearable
          :disabled="loading"
        />
      </label>

      <label class="filter-field">
        <span>城市</span>
        <ElInput
          v-model="draft.city"
          placeholder="精确城市名称"
          clearable
          :disabled="loading"
        />
      </label>

      <label class="filter-field">
        <span>最低月薪（元）</span>
        <ElInputNumber
          v-model="draft.salaryMin"
          :min="0"
          :precision="0"
          :controls="false"
          placeholder="不限"
          :disabled="loading"
        />
      </label>

      <label class="filter-field">
        <span>最高月薪（元）</span>
        <ElInputNumber
          v-model="draft.salaryMax"
          :min="0"
          :precision="0"
          :controls="false"
          placeholder="不限"
          :disabled="loading"
        />
      </label>

      <label class="filter-field">
        <span>来源</span>
        <ElInput
          v-model="draft.source"
          placeholder="例如 BOSS"
          clearable
          :disabled="loading"
        />
      </label>

      <label class="filter-field">
        <span>职位状态</span>
        <ElInput
          v-model="draft.jobStatus"
          placeholder="按后端实际值精确匹配"
          clearable
          :disabled="loading"
        />
      </label>

      <label class="filter-field">
        <span>办公方式</span>
        <ElInput
          v-model="draft.remoteType"
          placeholder="按后端实际值精确匹配"
          clearable
          :disabled="loading"
        />
      </label>

      <label class="filter-field">
        <span>排序字段</span>
        <ElSelect v-model="draft.sortBy" :disabled="loading">
          <ElOption
            v-for="option in JOB_SORT_OPTIONS"
            :key="option.value"
            :label="option.label"
            :value="option.value"
          />
        </ElSelect>
      </label>

      <label class="filter-field">
        <span>排序方向</span>
        <ElSelect v-model="draft.sortDirection" :disabled="loading">
          <ElOption label="降序" value="desc" />
          <ElOption label="升序" value="asc" />
        </ElSelect>
      </label>

      <div class="filter-actions">
        <ElButton
          type="primary"
          native-type="submit"
          :loading="loading"
          :disabled="loading"
        >
          搜索
        </ElButton>
        <ElButton :disabled="loading" @click="emit('reset')">重置</ElButton>
      </div>
    </form>
  </section>
</template>
