<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { createAdminReset, listAdminResets } from '../features/admin-reset-v2/api'
import type { AdminReset } from '../features/admin-reset-v2/types'

const confirmation = ref('')
const reason = ref('')
const resets = ref<AdminReset[]>([])
const latest = ref<AdminReset>()
const error = ref('')
const submitting = ref(false)
const confirmed = computed(
  () => confirmation.value === 'RESET LOCAL MOCK DATA' && reason.value.trim().length > 0,
)

onMounted(refresh)

async function refresh() {
  error.value = ''
  try {
    resets.value = (await listAdminResets()).items
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '管理员重置历史加载失败'
  }
}

async function submit() {
  if (!confirmed.value) return
  submitting.value = true
  error.value = ''
  try {
    latest.value = await createAdminReset(reason.value.trim())
    resets.value = [
      latest.value,
      ...resets.value.filter((item) => item.taskId !== latest.value?.taskId),
    ]
    confirmation.value = ''
    reason.value = ''
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '本地模拟数据重置创建失败'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <main>
    <section class="intro">
      <p class="eyebrow">Admin Reset v2 · 模拟数据</p>
      <h1>本地模拟数据受控重置</h1>
      <p class="sandbox-warning">
        该操作仅供明确标识的 LOCAL 环境使用，不是备份、灾备或生产恢复，也不是生产删除能力。
        重置会清理数据库中的一期模拟业务状态与原始知识文件，并恢复版本化初始数据。
      </p>
      <p class="fallback">
        任务开始后会注销当前本地会话；请等待片刻、重新以本地管理员登录，再刷新查看最终结果。
        操作不会修改 Git 仓库、模型授权证据或任何外部系统。
      </p>
    </section>

    <section class="card">
      <h2>二次确认</h2>
      <label>
        重置原因（仅进入任务元数据，不复制被删除正文）
        <textarea v-model="reason" data-test="reset-reason" maxlength="1000" />
      </label>
      <label>
        输入固定确认短语 <code>RESET LOCAL MOCK DATA</code>
        <input v-model="confirmation" data-test="reset-confirmation" autocomplete="off" />
      </label>
      <button
        class="primary"
        data-test="reset-submit"
        :disabled="!confirmed || submitting"
        @click="submit"
      >
        {{ submitting ? '创建中…' : '创建异步重置任务' }}
      </button>
      <p v-if="error" class="error">{{ error }}</p>
      <article v-if="latest" class="reference" data-test="reset-latest">
        <h3>已受理 · {{ latest.status }}</h3>
        <p>{{ latest.taskId }} · attempt {{ latest.attempt }}/{{ latest.maxAttempts }}</p>
      </article>
    </section>

    <section class="card run-history">
      <h2>重置任务历史</h2>
      <button class="secondary" data-test="reset-refresh" @click="refresh">刷新</button>
      <p v-if="!resets.length" class="empty">暂无本地重置任务。</p>
      <article v-for="reset in resets" :key="reset.taskId" class="reference">
        <h3>{{ reset.environmentCode }} · {{ reset.status }}</h3>
        <p>{{ reset.createdAt }} · {{ reset.taskId }}</p>
        <p>确认人 {{ reset.confirmedBy }} · attempt {{ reset.attempt }}/{{ reset.maxAttempts }}</p>
        <p v-if="reset.error" class="error">{{ reset.error.code }}：{{ reset.error.message }}</p>
      </article>
    </section>
  </main>
</template>
