<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { createEvaluationRun, getMetrics, listEvaluationRuns } from '../features/evaluation-v2/api'
import type { EvaluationRun, Metrics } from '../features/evaluation-v2/types'

const runs = ref<EvaluationRun[]>([])
const metrics = ref<Metrics>()
const error = ref('')
const running = ref(false)

onMounted(refresh)

async function refresh() {
  error.value = ''
  const to = new Date()
  const from = new Date(to.getTime() - 7 * 24 * 60 * 60 * 1000)
  try {
    ;[runs.value, metrics.value] = [
      (await listEvaluationRuns()).items,
      await getMetrics(from.toISOString(), to.toISOString()),
    ]
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '模拟数据：管理员评估页加载失败。'
  }
}

async function start() {
  running.value = true
  error.value = ''
  try {
    await createEvaluationRun()
    await refresh()
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '模拟数据：评估运行创建失败。'
  } finally {
    running.value = false
  }
}

function percent(value: number) {
  return `${(value * 100).toFixed(1)}%`
}
</script>

<template>
  <main>
    <section class="intro">
      <p class="eyebrow">Evaluation v2 · 模拟数据</p>
      <h1>管理员评估与指标</h1>
      <p class="sandbox-warning">
        仅 ADMIN 可运行固定评估集。小样本工程评估，不代表生产效果；指标不用于自动判责，SLA
        内容仍由人工确认。
      </p>
      <div class="actions">
        <button class="primary" data-test="evaluation-start" :disabled="running" @click="start">
          {{ running ? '创建中…' : '运行 mock-eval-v1' }}
        </button>
        <button class="secondary" data-test="evaluation-refresh" @click="refresh">刷新</button>
      </div>
      <p v-if="error" class="error">{{ error }}</p>
    </section>

    <section v-if="metrics" class="card metric-grid" data-test="metrics">
      <article>
        <strong>{{ metrics.precheckCount }}</strong
        ><span>预诊次数</span>
      </article>
      <article>
        <strong>{{ percent(metrics.successRate) }}</strong
        ><span>成功率</span>
      </article>
      <article>
        <strong>{{ percent(metrics.degradationRate) }}</strong
        ><span>降级率</span>
      </article>
      <article>
        <strong>{{ metrics.averageRunCount.toFixed(1) }}</strong
        ><span>平均 Run 数</span>
      </article>
      <article>
        <strong>{{ percent(metrics.adoptionRate) }}</strong
        ><span>采纳率</span>
      </article>
      <article>
        <strong>{{ percent(metrics.continuationRate) }}</strong
        ><span>继续提交率</span>
      </article>
      <article>
        <strong>{{ metrics.retrievalP95Ms }} ms</strong><span>检索 P95</span>
      </article>
      <article>
        <strong>{{ metrics.embeddingP95Ms }} ms</strong><span>Embedding P95</span>
      </article>
    </section>

    <section class="card run-history">
      <h2>固定评估历史</h2>
      <p v-if="!runs.length" class="empty">暂无评估运行。</p>
      <article v-for="run in runs" :key="run.taskId" class="reference" data-test="evaluation-run">
        <h3>{{ run.evaluationSetVersion }} · {{ run.status }}</h3>
        <p>{{ run.createdAt }} · {{ run.taskId }}</p>
        <template v-if="run.summary">
          <p>门禁：{{ run.summary.gatePassed ? 'PASS' : 'FAIL（任务仍正常完成）' }}</p>
          <p>
            权限泄漏 {{ percent(run.summary.permissionLeakageRate) }} · 引用错误
            {{ percent(run.summary.citationErrorRate) }} · 降级通过
            {{ percent(run.summary.degradationPassRate) }} · Recall@5
            {{ percent(run.summary.recallAt5) }}
          </p>
          <p class="fallback">{{ run.summary.disclaimer }}</p>
        </template>
      </article>
    </section>
  </main>
</template>
