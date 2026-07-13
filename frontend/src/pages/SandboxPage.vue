<script setup lang="ts">
import PrecheckForm from '../features/precheck/components/PrecheckForm.vue'
import PrecheckPanel from '../features/precheck/components/PrecheckPanel.vue'
import { usePrecheck } from '../features/precheck/usePrecheck'
import { SandboxHostBridge } from '../integration/aiops/SandboxHostBridge'
import { toPrecheckRequest, type PrecheckContext } from '../integration/aiops/protocol'
import type { PrecheckRequest } from '../features/precheck/types'
import { ref } from 'vue'
const bridge = new SandboxHostBridge({
  sourceSystem: 'SANDBOX',
  hostRequestId: crypto.randomUUID(),
  formSchemaVersion: 'sandbox-v1',
  title: '',
  descriptionPlainText: '',
})
const state = usePrecheck()
const submissionMessage = ref('')
const simulateFailure = ref(false)
async function runSandbox(payload: PrecheckRequest) {
  const context: PrecheckContext = {
    sourceSystem: 'SANDBOX',
    hostRequestId: crypto.randomUUID(),
    formSchemaVersion: 'sandbox-v1',
    title: payload.title,
    descriptionPlainText: payload.description,
    product: payload.product ? { name: payload.product } : undefined,
    component: payload.module ? { name: payload.module } : undefined,
    version: payload.version ? { name: payload.version } : undefined,
    issueLevel: payload.severity ? { name: payload.severity } : undefined,
    impactScope: payload.impactScope,
  }
  bridge.setContext(context)
  bridge.notifyPrecheckStart({ source: 'SANDBOX' })
  if (simulateFailure.value) {
    state.result.value = undefined
    state.conversation.value = []
    state.followUpDraft.value = ''
    state.error.value = '模拟数据：预诊后端不可用；仍可继续 AIOps 原有人工提交。'
    bridge.reportError({ code: 'SANDBOX_SIMULATED_FAILURE' })
    return
  }
  await state.run(toPrecheckRequest(context))
  bridge.notifyPrecheckResult({
    precheckId: state.result.value?.precheckId,
    status: state.result.value?.status,
  })
}
function continueSubmission() {
  bridge.continueSubmission({ precheckId: state.result.value?.precheckId })
  submissionMessage.value = '模拟数据：已交还 Mock AIOps 人工确认，未调用真实提交接口。'
}
</script>
<template>
  <main>
    <section class="intro">
      <p class="eyebrow">Mock AIOps Sandbox</p>
      <h1>AIOps 宿主联调与预诊演示</h1>
      <p class="sandbox-warning">本页面仅用于本地开发、联调和演示，不是正式 SLA 提交入口。</p>
    </section>
    <label class="sandbox-toggle">
      <input v-model="simulateFailure" type="checkbox" /> 模拟预诊后端失败
    </label>
    <div class="layout">
      <PrecheckForm
        :loading="state.loading.value"
        @precheck="runSandbox"
        @continue-submission="continueSubmission"
      /><PrecheckPanel
        v-model:draft="state.followUpDraft.value"
        debug
        :loading="state.loading.value"
        :error="state.error.value"
        :result="state.result.value"
        :follow-up-loading="state.followUpLoading.value"
        :follow-up-error="state.followUpError.value"
        :conversation="state.conversation.value"
        :feedback-loading="state.feedbackLoading.value"
        :feedback-message="state.feedbackMessage.value"
        @send-follow-up="state.sendFollowUp"
        @feedback="state.sendFeedback"
        @continue-submission="continueSubmission"
      />
    </div>
    <p v-if="submissionMessage" class="notice">{{ submissionMessage }}</p>
  </main>
</template>
