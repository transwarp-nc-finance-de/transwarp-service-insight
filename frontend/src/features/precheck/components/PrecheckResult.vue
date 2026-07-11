<script setup lang="ts">
import type { PrecheckResponse } from '../types'

defineProps<{ loading: boolean; error: string; result?: PrecheckResponse }>()
</script>

<template>
  <aside class="card result">
    <h2>预诊结果</h2>
    <div v-if="loading" class="empty">正在调用本地 Mock API…</div>
    <div v-else-if="error" class="error">{{ error }}</div>
    <div v-else-if="result">
      <div class="meta"><span>置信度 {{ result.confidence }}</span><span>必须人工审核</span></div>
      <h3>辅助摘要</h3><p>{{ result.summary }}</p>
      <h3>建议</h3><ul><li v-for="item in result.recommendations" :key="item">{{ item }}</li></ul>
      <h3>依据来源</h3>
      <article v-for="item in result.references" :key="item.url" class="reference">
        <strong>{{ item.title }}</strong><p>{{ item.excerpt }}</p>
        <a :href="item.url" target="_blank" rel="noopener noreferrer">模拟链接</a>
      </article>
      <template v-if="result.missingInformation.length">
        <h3>待补充信息</h3><div class="chips"><span v-for="item in result.missingInformation" :key="item">{{ item }}</span></div>
      </template>
      <p class="fallback">{{ result.fallbackReason }}</p>
    </div>
    <div v-else class="empty">填写表单后点击“智能预诊”，结果将在这里展示。</div>
  </aside>
</template>
