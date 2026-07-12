<script setup lang="ts">
import PrecheckForm from './features/precheck/components/PrecheckForm.vue'
import PrecheckResult from './features/precheck/components/PrecheckResult.vue'
import { usePrecheck } from './features/precheck/usePrecheck'

const {
  loading,
  error,
  result,
  run,
  followUpLoading,
  followUpError,
  followUpDraft,
  conversation,
  sendFollowUp,
  feedbackLoading,
  feedbackMessage,
  sendFeedback,
} = usePrecheck()
</script>

<template>
  <header>
    <div><span class="brand">TRANSWARP</span><strong>Service Insight</strong></div>
    <span class="badge">技术 MVP · 模拟数据</span>
  </header>
  <main>
    <section class="intro">
      <p class="eyebrow">SLA 智能预诊助手</p>
      <h1>在提交前，把问题描述得更完整</h1>
      <p>Mock API 仅生成辅助建议，不代表最终根因或处理结论；SLA 是否提交由人工确认。</p>
    </section>
    <div class="layout">
      <PrecheckForm
        :loading="loading"
        @precheck="run"
        @continue-submission="sendFeedback('IGNORED', true)"
      />
      <PrecheckResult
        v-model:draft="followUpDraft"
        :loading="loading"
        :error="error"
        :result="result"
        :follow-up-loading="followUpLoading"
        :follow-up-error="followUpError"
        :conversation="conversation"
        :feedback-loading="feedbackLoading"
        :feedback-message="feedbackMessage"
        @send-follow-up="sendFollowUp"
        @feedback="sendFeedback"
      />
    </div>
  </main>
</template>
