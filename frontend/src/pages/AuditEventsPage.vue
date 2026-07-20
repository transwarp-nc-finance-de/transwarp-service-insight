<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { listAuditEvents, type AuditEvent } from '../features/audit-v2/api'

const items = ref<AuditEvent[]>([])
const action = ref('')
const error = ref('')

onMounted(load)

async function load() {
  error.value = ''
  try {
    items.value = (await listAuditEvents(action.value)).items
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '模拟数据：审计查询失败。'
  }
}
</script>

<template>
  <main>
    <section class="intro">
      <p class="eyebrow">Audit v2 · 模拟数据</p>
      <h1>管理员结构化审计</h1>
      <p class="sandbox-warning">
        仅 ADMIN 可读，结果按授权产品线过滤；不展示标题、描述、附件、反馈正文或内部错误细节。
      </p>
    </section>
    <section class="card">
      <label>动作筛选<input v-model="action" data-test="audit-action" /></label>
      <button class="secondary" data-test="audit-refresh" @click="load">查询</button>
      <p v-if="error" class="error">{{ error }}</p>
      <p v-if="!error && !items.length" class="empty">暂无可见审计事件。</p>
      <article v-for="item in items" :key="item.eventId" class="reference" data-test="audit-event">
        <h2>{{ item.action }} · {{ item.outcome }}</h2>
        <p>操作者：{{ item.actorUserCode }}</p>
        <p>对象：{{ item.subjectType }} / {{ item.subjectId }}</p>
        <p>发生时间：{{ item.occurredAt }}</p>
        <p>脱敏元数据：{{ item.metadata }}</p>
        <p class="fallback">模拟数据 · 只读不可变审计</p>
      </article>
    </section>
  </main>
</template>
