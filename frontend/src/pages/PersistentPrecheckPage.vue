<script setup lang="ts">
import { onMounted, ref } from 'vue'
import {
  confirmSelfService,
  createRun,
  createSession,
  listRuns,
  listSessions,
} from '../features/precheck-v2/api'
import type { PrecheckContext, PrecheckRun, PrecheckSession } from '../features/precheck-v2/types'

const session = ref<PrecheckSession>()
const runs = ref<PrecheckRun[]>([])
const title = ref('模拟数据：SQL 查询返回 MOCK-1001')
const description = ref('模拟数据：执行脱敏 SQL 时返回错误码 MOCK-1001。')
const impactScope = ref('模拟数据：单个测试任务')
const loading = ref(false)
const error = ref('')
const notice = ref('')

onMounted(restore)

async function restore() {
  try {
    const page = await listSessions()
    const active = page.items.find((item) => item.status === 'ACTIVE')
    if (!active) return
    session.value = active
    apply(active.latestRun.contextSnapshot)
    runs.value = (await listRuns(active.sessionId)).items
  } catch (cause) {
    setError(cause)
  }
}

async function start() {
  await execute(async () => {
    const created = await createSession(context(crypto.randomUUID()))
    session.value = created
    runs.value = [created.latestRun]
  })
}

async function supplement() {
  if (!session.value) return
  await execute(async () => {
    const run = await createRun(
      session.value!.sessionId,
      context(session.value!.latestRun.contextSnapshot.hostRequestId),
    )
    session.value = {
      ...session.value!,
      latestRun: run,
      runCount: run.sequence,
    }
    runs.value = (await listRuns(session.value.sessionId)).items
  })
}

async function terminate() {
  if (!session.value) return
  await execute(async () => {
    await confirmSelfService(session.value!.sessionId, '模拟数据：已由人工明确确认采用自助建议')
    session.value = {
      ...session.value!,
      status: 'TERMINATED',
      terminationReason: 'SELF_SERVICE_CONFIRMED',
    }
    notice.value = '模拟数据：Session 已由当前负责人明确结束，历史 Run 保持只读。'
  })
}

function continueSubmission() {
  notice.value = '模拟数据：已跳过建议，后续仍由人工确认 SLA 提交；本页未持久化提交记录。'
}

async function execute(action: () => Promise<void>) {
  loading.value = true
  error.value = ''
  notice.value = ''
  try {
    await action()
  } catch (cause) {
    setError(cause)
  } finally {
    loading.value = false
  }
}

function setError(cause: unknown) {
  error.value = cause instanceof Error ? cause.message : '模拟数据：操作失败。'
}

function apply(value: PrecheckContext) {
  title.value = value.title
  description.value = value.descriptionPlainText
  impactScope.value = value.impactScope
}

function context(hostRequestId: string): PrecheckContext {
  return {
    sourceSystem: 'SANDBOX',
    hostRequestId,
    formSchemaVersion: 'sandbox-v2',
    issueType: { code: 'FUNCTIONAL_FAILURE', displayName: '功能故障（模拟数据）' },
    productLine: { code: 'TDH', displayName: 'TDH（模拟数据）' },
    product: { code: 'INCEPTOR', displayName: 'Inceptor（模拟数据）' },
    component: { code: 'SQL_ENGINE', displayName: 'SQL 引擎（模拟数据）' },
    version: '9.1.0-mock',
    severity: { code: 'S2', displayName: 'S2（模拟数据）' },
    serviceType: { code: 'CONSULTATION', displayName: '咨询（模拟数据）' },
    title: title.value,
    descriptionPlainText: description.value,
    additionalInformation: [],
    impactScope: impactScope.value,
    attachments: [],
  }
}
</script>

<template>
  <main>
    <section class="intro">
      <p class="eyebrow">Persistent Precheck v2 · 模拟数据</p>
      <h1>可恢复的多轮预诊 Sandbox</h1>
      <p class="sandbox-warning">
        仅使用模拟数据；结果包含确定性完整度与降级说明，不是最终根因、最终方案或正式复盘结论。
      </p>
    </section>
    <div class="layout">
      <section class="card">
        <h2>人工维护的上下文</h2>
        <label>标题<input v-model="title" data-test="title" /></label>
        <label>描述<textarea v-model="description" rows="6" /></label>
        <label>影响范围<input v-model="impactScope" /></label>
        <div class="actions">
          <button v-if="!session" class="primary" :disabled="loading" @click="start">
            创建 Session
          </button>
          <button
            v-else
            class="primary"
            data-test="supplement"
            :disabled="
              loading || session.status !== 'ACTIVE' || session.runCount >= session.maxRuns
            "
            @click="supplement"
          >
            补充并再次预诊
          </button>
          <button class="secondary" data-test="continue" @click="continueSubmission">
            跳过建议并继续人工提交
          </button>
          <button
            v-if="session?.status === 'ACTIVE'"
            class="secondary"
            data-test="terminate"
            :disabled="loading"
            @click="terminate"
          >
            明确确认自助结束
          </button>
        </div>
        <p v-if="error" class="error">{{ error }}</p>
        <p v-if="notice" class="notice">{{ notice }}</p>
      </section>
      <section class="card result">
        <h2>持久化 Run 历史</h2>
        <p v-if="!session" class="empty">登录预诊用户后创建，或自动恢复自己的 ACTIVE Session。</p>
        <template v-else>
          <p class="meta">
            <span>{{ session.status }}</span
            ><span>{{ session.runCount }}/{{ session.maxRuns }} Runs</span>
          </p>
          <article v-for="run in runs" :key="run.runId" class="reference">
            <h3>第 {{ run.sequence }} 次 · {{ run.status }}</h3>
            <p>{{ run.result.summary }}</p>
            <p>完整度策略：{{ run.result.completeness.policyVersion }}</p>
            <p>
              置信度：{{ run.result.confidence }}（{{ run.result.confidenceReasons.join('；') }}）
            </p>
            <p>待补充：{{ run.result.missingInformation.join('、') || '无' }}</p>
            <p>人工介入：{{ run.result.humanInterventionAdvice.join('；') }}</p>
            <p>检索降级：{{ run.result.retrieval.mode }}</p>
            <p class="fallback">{{ run.result.disclaimer }}</p>
          </article>
        </template>
      </section>
    </div>
  </main>
</template>
