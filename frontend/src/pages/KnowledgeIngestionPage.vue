<script setup lang="ts">
import { computed, ref } from 'vue'
import { readCsrfToken } from '../features/identity/useAuthSession'

interface ParseWarning {
  code: string
  message: string
}

interface ParseSummary {
  versionStatus: 'DRAFT' | 'IN_REVIEW' | 'APPROVED'
  submittedBy?: string | null
  parserVersion: string
  parseResultHash: string
  warnings: ParseWarning[]
}

interface KnowledgeVersion {
  versionId: string
  status: 'DRAFT' | 'IN_REVIEW' | 'APPROVED'
  submittedBy?: string | null
  approvedBy?: string | null
}

const props = defineProps<{ csrfToken?: string }>()
const title = ref('')
const productLineCode = ref('TDH')
const file = ref<File>()
const task = ref<Record<string, unknown>>()
const summary = ref<ParseSummary>()
const blocks = ref<Array<Record<string, unknown>>>([])
const chunks = ref<Array<Record<string, unknown>>>([])
const loading = ref(false)
const error = ref('')
const version = ref<KnowledgeVersion>()
const acknowledgedWarningCodes = ref<string[]>([])
const cleanedText = ref('')
const warningNote = ref('')
const returnReason = ref('')
const reviewVersionId = ref('')
const governanceLoading = ref(false)

function selectFile(event: Event) {
  file.value = (event.target as HTMLInputElement).files?.[0]
}

async function upload() {
  if (!file.value) return
  loading.value = true
  error.value = ''
  try {
    const metadata = {
      title: title.value,
      productLine: {
        code: productLineCode.value,
        displayName: `${productLineCode.value}（模拟数据）`,
      },
      sourceType: 'MOCK',
      mockData: true,
    }
    const body = new FormData()
    body.append('metadata', new Blob([JSON.stringify(metadata)], { type: 'application/json' }))
    body.append('file', file.value)
    const created = await request('/api/v2/knowledge-documents', {
      method: 'POST',
      headers: {
        'X-CSRF-Token': props.csrfToken || readCsrfToken(),
        'Idempotency-Key': crypto.randomUUID(),
      },
      body,
    })
    task.value = created.parseTask
    version.value = created.version as KnowledgeVersion
    await poll(created.parseTask.taskId, created.version.versionId)
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '知识解析暂时不可用'
  } finally {
    loading.value = false
  }
}

async function poll(taskId: string, versionId: string): Promise<void> {
  task.value = await request(`/api/v2/parse-tasks/${taskId}`)
  if (task.value?.status === 'PENDING' || task.value?.status === 'RUNNING') {
    await new Promise((resolve) => setTimeout(resolve, 500))
    return poll(taskId, versionId)
  }
  if (task.value?.status !== 'SUCCEEDED') return
  await loadPreview(versionId)
}

async function loadPreview(versionId: string) {
  const [preview, blockPage, chunkPage] = await Promise.all([
    request(`/api/v2/knowledge-versions/${versionId}/parse-preview`),
    request(`/api/v2/knowledge-versions/${versionId}/parse-preview/blocks`),
    request(`/api/v2/knowledge-versions/${versionId}/parse-preview/chunks`),
  ])
  summary.value = preview as ParseSummary
  blocks.value = blockPage.items
  chunks.value = chunkPage.items
  cleanedText.value = blocks.value.map((block) => String(block.text || '')).join('\n\n')
  acknowledgedWarningCodes.value = []
}

async function loadReviewVersion() {
  const versionId = reviewVersionId.value.trim()
  if (!versionId) return
  governanceLoading.value = true
  error.value = ''
  try {
    await loadPreview(versionId)
    version.value = {
      versionId,
      status: summary.value?.versionStatus ?? 'IN_REVIEW',
      submittedBy: summary.value?.submittedBy,
    }
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '待审核版本暂时不可用'
  } finally {
    governanceLoading.value = false
  }
}

async function submitReview() {
  if (!version.value || !summary.value) return
  await governanceCommand('review-submissions', {
    parseResultHash: summary.value.parseResultHash,
  })
}

async function approve() {
  if (!version.value || !summary.value || !allWarningsAcknowledged.value) return
  await governanceCommand('approvals', {
    parseResultHash: summary.value.parseResultHash,
    acknowledgedWarningCodes: acknowledgedWarningCodes.value,
  })
}

async function returnToDraft() {
  if (!version.value || !returnReason.value.trim()) return
  await governanceCommand('draft-returns', { reason: returnReason.value.trim() })
}

async function revise() {
  if (!version.value || !cleanedText.value.trim()) return
  governanceLoading.value = true
  error.value = ''
  try {
    const body = new FormData()
    body.append(
      'revision',
      new Blob(
        [
          JSON.stringify({
            title: title.value,
            productLine: {
              code: productLineCode.value,
              displayName: `${productLineCode.value}（模拟数据）`,
            },
            cleanedText: cleanedText.value,
            parseWarningNotes: warningNote.value.trim() ? [warningNote.value.trim()] : [],
          }),
        ],
        { type: 'application/json' },
      ),
    )
    const result = await request(
      `/api/v2/knowledge-versions/${version.value.versionId}/revisions`,
      writeRequest(body),
    )
    task.value = result.parseTask
    version.value.status = 'DRAFT'
    await poll(result.parseTask.taskId, version.value.versionId)
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '草稿修订暂时不可用'
  } finally {
    governanceLoading.value = false
  }
}

const allWarningsAcknowledged = computed(() => {
  const required = summary.value?.warnings.map((warning) => warning.code) ?? []
  return required.every((code) => acknowledgedWarningCodes.value.includes(code))
})

async function governanceCommand(path: string, body: object) {
  if (!version.value) return
  governanceLoading.value = true
  error.value = ''
  try {
    const result = await request(`/api/v2/knowledge-versions/${version.value.versionId}/${path}`, {
      ...writeRequest(JSON.stringify(body)),
      headers: {
        ...writeHeaders(),
        'Content-Type': 'application/json',
      },
    })
    version.value = result.version as KnowledgeVersion
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '知识审核命令暂时不可用'
  } finally {
    governanceLoading.value = false
  }
}

function writeHeaders() {
  return {
    'X-CSRF-Token': props.csrfToken || readCsrfToken(),
    'Idempotency-Key': crypto.randomUUID(),
  }
}

function writeRequest(body: string | FormData) {
  return { method: 'POST', headers: writeHeaders(), body }
}

async function request(path: string, init: Parameters<typeof fetch>[1] = {}) {
  const response = await fetch(path, { ...init, credentials: 'include' })
  if (!response.ok) {
    const body = await response.json().catch(() => ({}))
    throw new Error(body.message || `请求失败（${response.status}）`)
  }
  return response.json()
}
</script>

<template>
  <main>
    <section class="intro">
      <p class="eyebrow">Knowledge ingestion · 模拟数据</p>
      <h1>知识文件解析预览</h1>
      <p>仅用于本地模拟 Markdown、TXT 与文本型 PDF 的上传和解析，不连接真实知识源。</p>
    </section>
    <div class="layout">
      <form class="card" @submit.prevent="upload">
        <h2>上传模拟文件</h2>
        <label>标题<input data-test="title" v-model="title" required /></label>
        <label>
          产品线
          <select v-model="productLineCode">
            <option>TDH</option>
            <option>STREAMING</option>
          </select>
        </label>
        <label>
          文件
          <input
            data-test="file"
            type="file"
            accept=".md,.txt,.pdf,text/markdown,text/plain,application/pdf"
            required
            @change="selectFile"
          />
        </label>
        <button class="primary" :disabled="loading">
          {{ loading ? '处理中…' : '上传并解析' }}
        </button>
        <p v-if="error" class="error">{{ error }}</p>
        <p v-if="task" class="notice">任务状态：{{ task.status }}</p>
      </form>
      <section class="card">
        <h2>加载待审核版本（模拟数据）</h2>
        <label>
          Version ID
          <input data-test="review-version-id" v-model="reviewVersionId" />
        </label>
        <button
          data-test="load-review-version"
          :disabled="governanceLoading || !reviewVersionId.trim()"
          @click="loadReviewVersion"
        >
          加载审核预览
        </button>
      </section>
      <section class="card result">
        <h2>解析结果（模拟数据）</h2>
        <template v-if="summary">
          <p>解析器：{{ summary.parserVersion }}</p>
          <p>结果哈希：{{ summary.parseResultHash }}</p>
          <h3>安全警告</h3>
          <p v-if="!summary.warnings.length">无</p>
          <ul v-else>
            <li v-for="warning in summary.warnings" :key="warning.code">
              {{ warning.code }}：{{ warning.message }}
            </li>
          </ul>
          <h3>Blocks</h3>
          <ol>
            <li v-for="block in blocks" :key="String(block.sequence)">{{ block.text }}</li>
          </ol>
          <h3>Chunks</h3>
          <ol>
            <li v-for="chunk in chunks" :key="String(chunk.sequence)">{{ chunk.text }}</li>
          </ol>
        </template>
        <p v-else class="empty">上传后显示解析摘要、Block 和 Chunk。</p>
      </section>
      <section v-if="summary && version" class="card governance">
        <h2>知识草稿与审核（模拟数据）</h2>
        <p class="notice">治理状态：{{ version.status }}</p>
        <button
          data-test="submit-review"
          :disabled="governanceLoading || version.status !== 'DRAFT'"
          @click="submitReview"
        >
          提交审核
        </button>
        <fieldset>
          <legend>审核警告确认</legend>
          <p v-if="!summary.warnings.length">当前解析无警告</p>
          <label v-for="warning in summary.warnings" :key="warning.code">
            <input
              v-model="acknowledgedWarningCodes"
              type="checkbox"
              :value="warning.code"
              :data-test="`warning-${warning.code}`"
            />
            {{ warning.code }}：{{ warning.message }}
          </label>
        </fieldset>
        <button
          data-test="approve"
          :disabled="
            governanceLoading || version.status !== 'IN_REVIEW' || !allWarningsAcknowledged
          "
          @click="approve"
        >
          批准
        </button>
        <label
          >退回原因<input data-test="return-reason" v-model="returnReason" maxlength="1000"
        /></label>
        <button
          data-test="return-draft"
          :disabled="governanceLoading || version.status !== 'IN_REVIEW' || !returnReason.trim()"
          @click="returnToDraft"
        >
          退回草稿
        </button>
        <label
          >清洗文本<textarea
            data-test="cleaned-text"
            v-model="cleanedText"
            maxlength="5000000"
          ></textarea>
        </label>
        <label>警告说明<input v-model="warningNote" maxlength="1000" /></label>
        <button
          data-test="revise"
          :disabled="governanceLoading || version.status !== 'DRAFT' || !cleanedText.trim()"
          @click="revise"
        >
          创建不可变草稿修订并重新解析
        </button>
      </section>
    </div>
  </main>
</template>
