<script setup lang="ts">
import PrecheckForm from './features/precheck/components/PrecheckForm.vue'
import PrecheckResult from './features/precheck/components/PrecheckResult.vue'
import { usePrecheck } from './features/precheck/usePrecheck'

const { loading, error, result, run } = usePrecheck()
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
      <PrecheckForm :loading="loading" @precheck="run" />
      <PrecheckResult :loading="loading" :error="error" :result="result" />
    </div>
  </main>
</template>
