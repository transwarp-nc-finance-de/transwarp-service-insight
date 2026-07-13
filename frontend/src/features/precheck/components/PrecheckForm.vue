<script setup lang="ts">
import { reactive, ref } from 'vue'
import type { PrecheckRequest } from '../types'

defineProps<{ loading: boolean }>()
const emit = defineEmits<{
  precheck: [payload: PrecheckRequest]
  continueSubmission: []
}>()
const submitted = ref(false)
const validationError = ref('')
const form = reactive<PrecheckRequest>({
  title: '模拟环境查询响应变慢',
  description: '模拟数据：上午开始查询响应时间增加，需要辅助梳理排查信息。',
  product: 'Inceptor',
  module: 'SQL Engine',
  version: '',
  severity: 'P2',
  impactScope: '单个模拟任务',
  attachmentsSummary: '',
})

function submitPrecheck() {
  validationError.value = ''
  if (!form.title.trim() || !form.description.trim()) {
    validationError.value = '请先填写标题和问题描述；仍可继续人工提交。'
    return
  }
  emit('precheck', { ...form })
}

function continueSubmission() {
  submitted.value = true
  emit('continueSubmission')
}
</script>

<template>
  <form class="card" @submit.prevent="submitPrecheck">
    <h2>模拟 AIOps SLA 表单</h2>
    <label>标题 *<input v-model="form.title" maxlength="200" /></label>
    <label>问题描述 *<textarea v-model="form.description" rows="5" maxlength="10000" /></label>
    <div class="grid">
      <label>产品<input v-model="form.product" maxlength="100" /></label>
      <label>模块<input v-model="form.module" maxlength="100" /></label>
      <label>版本<input v-model="form.version" maxlength="100" /></label>
      <label
        >紧急程度<select v-model="form.severity">
          <option value="">请选择</option>
          <option>P1</option>
          <option>P2</option>
          <option>P3</option>
        </select></label
      >
    </div>
    <label>影响范围<input v-model="form.impactScope" maxlength="2000" /></label>
    <label
      >附件摘要（仅限模拟或脱敏内容）<textarea
        v-model="form.attachmentsSummary"
        rows="2"
        maxlength="5000"
      />
    </label>
    <p v-if="validationError" class="error">{{ validationError }}</p>
    <div class="actions">
      <button class="primary" :disabled="loading">{{ loading ? '正在预诊…' : '智能预诊' }}</button>
      <button type="button" class="secondary" @click="continueSubmission">继续提交 SLA</button>
    </div>
    <p v-if="submitted" class="notice">已进入人工确认步骤（模拟），未调用真实提交接口。</p>
  </form>
</template>
