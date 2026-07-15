<script setup lang="ts">
import { ref } from 'vue'
import { readCsrfToken } from '../features/identity/useAuthSession'

interface ParseWarning {
  code: string
  message: string
}

interface ParseSummary {
  parserVersion: string
  parseResultHash: string
  warnings: ParseWarning[]
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
  const [preview, blockPage, chunkPage] = await Promise.all([
    request(`/api/v2/knowledge-versions/${versionId}/parse-preview`),
    request(`/api/v2/knowledge-versions/${versionId}/parse-preview/blocks`),
    request(`/api/v2/knowledge-versions/${versionId}/parse-preview/chunks`),
  ])
  summary.value = preview as ParseSummary
  blocks.value = blockPage.items
  chunks.value = chunkPage.items
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
    </div>
  </main>
</template>
