<script setup lang="ts">
import { onMounted, ref } from 'vue'
import {
  createEvaluationRun,
  getMetrics,
  listEvaluationFailures,
  listEvaluationRuns,
} from '../features/evaluation-v2/api'
import type { EvaluationFailure, EvaluationRun, Metrics } from '../features/evaluation-v2/types'

const runs = ref<EvaluationRun[]>([])
const metrics = ref<Metrics>()
const failures = ref<EvaluationFailure[]>([])
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
    const latestCompleted = runs.value.find((run) => run.status === 'SUCCEEDED')
    failures.value = latestCompleted
      ? (await listEvaluationFailures(latestCompleted.taskId)).items
      : []
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
        <strong>{{ percent(metrics.informationSupplementRate) }}</strong
        ><span>信息补充率</span>
      </article>
      <article>
        <strong>{{ percent(metrics.evidenceHitRate) }}</strong
        ><span>Evidence 命中率</span>
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

    <section class="card run-history" data-test="evaluation-failures">
      <h2>最新运行失败案例（安全摘要）</h2>
      <p class="fallback">
        仅展示 caseId、场景、检查项和结构化计数/状态；不包含评估输入、Evidence ID、摘录或内部推理。
      </p>
      <p v-if="!failures.length" class="empty">最新已完成运行没有失败案例。</p>
      <article v-for="failure in failures" :key="failure.caseId" class="reference">
        <h3>{{ failure.caseId }} · {{ failure.failureCodes.join('、') }}</h3>
        <p>场景：{{ failure.scenarioTags.join('、') }}</p>
        <p>失败检查：{{ failure.failedChecks.join('、') }}</p>
        <p>期望摘要：{{ JSON.stringify(failure.expected) }}</p>
        <p>实际摘要：{{ JSON.stringify(failure.actual) }}</p>
      </article>
    </section>
  </main>
</template>
