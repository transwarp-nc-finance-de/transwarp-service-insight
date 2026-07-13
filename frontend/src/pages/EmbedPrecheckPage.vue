<script setup lang="ts">
import { onMounted, ref } from 'vue'
import PrecheckPanel from '../features/precheck/components/PrecheckPanel.vue'
import { usePrecheck } from '../features/precheck/usePrecheck'
import { PostMessageHostBridge } from '../integration/aiops/PostMessageHostBridge'
import { toPrecheckRequest } from '../integration/aiops/protocol'
const origins = (import.meta.env.VITE_AIOPS_ALLOWED_ORIGINS ?? window.location.origin)
  .split(',')
  .map((item: string) => item.trim())
const bridge = new PostMessageHostBridge(origins)
const state = usePrecheck()
const hostError = ref('')
onMounted(async () => {
  try {
    await bridge.initialize()
    bridge.notifyReady()
    bridge.notifyPrecheckStart({ source: 'AIOPS' })
    await state.run(toPrecheckRequest(await bridge.getPrecheckContext()))
    if (state.error.value) bridge.reportError({ message: state.error.value })
    else
      bridge.notifyPrecheckResult({
        precheckId: state.result.value?.precheckId,
        status: state.result.value?.status,
      })
  } catch (cause) {
    hostError.value = cause instanceof Error ? cause.message : '宿主初始化失败'
    bridge.reportError({ message: hostError.value })
  }
})
function continueSubmission() {
  bridge.continueSubmission({ precheckId: state.result.value?.precheckId })
}
</script>
<template>
  <main class="embed-page">
    <section class="intro">
      <p class="eyebrow">Embedded Precheck UI</p>
      <h1>智能预诊</h1>
      <p>表单与最终提交由 AIOps 负责；预诊失败不阻断人工继续提交。</p>
    </section>
    <PrecheckPanel
      v-model:draft="state.followUpDraft.value"
      :loading="state.loading.value"
      :error="hostError || state.error.value"
      :result="state.result.value"
      :follow-up-loading="state.followUpLoading.value"
      :follow-up-error="state.followUpError.value"
      :conversation="state.conversation.value"
      :feedback-loading="state.feedbackLoading.value"
      :feedback-message="state.feedbackMessage.value"
      empty-message="等待 AIOps 宿主传入未提交表单快照…"
      @send-follow-up="state.sendFollowUp"
      @feedback="state.sendFeedback"
      @continue-submission="continueSubmission"
    />
  </main>
</template>
