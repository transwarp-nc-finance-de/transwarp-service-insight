<script setup lang="ts">
import type { ConversationTurn, PrecheckResponse } from '../types'

const props = defineProps<{
  loading: boolean
  error: string
  result?: PrecheckResponse
  followUpLoading: boolean
  followUpError: string
  conversation: ConversationTurn[]
  feedbackLoading: boolean
  feedbackMessage: string
}>()
const draft = defineModel<string>('draft', { required: true })
const emit = defineEmits<{
  sendFollowUp: [message?: string]
  feedback: [status: 'ADOPTED' | 'PARTIALLY_ADOPTED' | 'IGNORED']
}>()

const quickActions = ['补充日志现象', '补充权限信息', '补充影响范围', '补充近期变更']

function send() {
  if (draft.value.trim() && !props.followUpLoading) emit('sendFollowUp')
}

const actionLabels = {
  SELF_SERVICE_SUGGESTED: '建议自助核对',
  NEED_MORE_INFORMATION: '建议补充信息',
  SUBMISSION_RECOMMENDED: '建议人工确认后提交',
  MANUAL_REVIEW_REQUIRED: '需要人工审核',
} as const
</script>

<template>
  <aside class="card result">
    <h2>预诊结果</h2>
    <div v-if="loading" class="empty">正在调用本地 Mock API…</div>
    <div v-else-if="error" class="error">{{ error }}</div>
    <div v-else-if="result">
      <div class="meta">
        <span>置信度 {{ result.confidence }}</span
        ><span>必须人工审核</span>
      </div>
      <p class="conversation-note">置信度依据：{{ result.confidenceReason }}</p>
      <p class="conversation-note">
        会话 {{ result.sessionId }} · 策略 {{ result.policyVersion }} · 规则
        {{ result.promptVersion }} · 模型 {{ result.modelVersion }} · 索引 {{ result.indexVersion }}
      </p>
      <h3>辅助摘要</h3>
      <p>{{ result.summary }}</p>
      <section class="next-action" aria-label="建议下一步">
        <h3>建议下一步</h3>
        <strong>{{ actionLabels[result.nextAction] }}</strong>
        <p>{{ result.nextActionReason }}</p>
        <p>允许操作：补充信息、继续人工提交</p>
      </section>
      <h3>建议</h3>
      <ul>
        <li v-for="item in result.recommendations" :key="item">{{ item }}</li>
      </ul>
      <h3>依据来源</h3>
      <article v-for="item in result.references" :key="item.url" class="reference">
        <strong>{{ item.title }}</strong>
        <p>{{ item.excerpt }}</p>
        <a :href="item.url" target="_blank" rel="noopener noreferrer">模拟链接</a>
      </article>
      <template v-if="result.missingInformation.length">
        <h3>待补充信息</h3>
        <div class="chips">
          <span v-for="item in result.missingInformation" :key="item">{{ item }}</span>
        </div>
      </template>
      <p class="fallback">{{ result.fallbackReason }}</p>
      <section class="feedback" aria-label="预诊反馈">
        <h3>反馈（模拟数据）</h3>
        <div class="quick-actions">
          <button type="button" :disabled="feedbackLoading" @click="emit('feedback', 'ADOPTED')">
            已采纳
          </button>
          <button
            type="button"
            :disabled="feedbackLoading"
            @click="emit('feedback', 'PARTIALLY_ADOPTED')"
          >
            部分采纳
          </button>
          <button type="button" :disabled="feedbackLoading" @click="emit('feedback', 'IGNORED')">
            未采纳
          </button>
        </div>
        <p v-if="feedbackMessage" class="notice">{{ feedbackMessage }}</p>
      </section>

      <section class="conversation" aria-label="预诊追问会话">
        <h3>继续补充问题</h3>
        <p class="conversation-note">
          会话仅保留在当前页面，不会回填 SLA 表单。请仅输入模拟、公开或脱敏内容。
        </p>
        <div class="quick-actions">
          <button
            v-for="action in quickActions"
            :key="action"
            type="button"
            :disabled="followUpLoading"
            @click="emit('sendFollowUp', action)"
          >
            {{ action }}
          </button>
        </div>
        <div v-for="turn in conversation" :key="turn.response.followUpId" class="turn">
          <p class="user-message"><strong>人工补充</strong>{{ turn.message }}</p>
          <div class="assistant-message">
            <div class="meta">
              <span>模拟回复</span><span>置信度 {{ turn.response.confidence }}</span
              ><span>必须人工审核</span>
            </div>
            <p class="conversation-note">置信度依据：{{ turn.response.confidenceReason }}</p>
            <p class="conversation-note">
              策略 {{ turn.response.policyVersion }} · 规则 {{ turn.response.promptVersion }} · 模型
              {{ turn.response.modelVersion }} · 索引 {{ turn.response.indexVersion }}
            </p>
            <p>{{ turn.response.reply }}</p>
            <strong>建议下一步</strong>
            <p>
              {{ actionLabels[turn.response.nextAction] }}：{{ turn.response.nextActionReason }}
            </p>
            <p>允许操作：补充信息、继续人工提交</p>
            <strong>建议</strong>
            <ul>
              <li v-for="item in turn.response.recommendations" :key="item">{{ item }}</li>
            </ul>
            <strong>模拟依据来源</strong>
            <p v-for="item in turn.response.references" :key="item.url" class="turn-reference">
              {{ item.title }}：{{ item.excerpt }}
            </p>
            <strong>待补充信息</strong>
            <div class="chips">
              <span v-for="item in turn.response.missingInformation" :key="item">{{ item }}</span>
            </div>
            <p class="fallback">{{ turn.response.fallbackReason }}</p>
          </div>
        </div>
        <div v-if="followUpError" class="error follow-up-error">{{ followUpError }}</div>
        <form class="follow-up-form" @submit.prevent="send">
          <textarea
            v-model="draft"
            maxlength="5000"
            rows="3"
            placeholder="补充脱敏日志、影响范围、权限信息或近期变更…"
          ></textarea>
          <button class="primary" :disabled="followUpLoading || !draft.trim()">
            {{ followUpLoading ? '发送中…' : '发送追问' }}
          </button>
        </form>
      </section>
    </div>
    <div v-else class="empty">填写表单后点击“智能预诊”，结果将在这里展示。</div>
  </aside>
</template>
