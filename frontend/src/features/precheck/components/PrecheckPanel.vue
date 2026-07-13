<script setup lang="ts">
import PrecheckResult from './PrecheckResult.vue'
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
  debug?: boolean
  emptyMessage?: string
}>()
const draft = defineModel<string>('draft', { required: true })
defineEmits<{
  sendFollowUp: [message?: string]
  feedback: [status: 'ADOPTED' | 'PARTIALLY_ADOPTED' | 'IGNORED']
  continueSubmission: []
}>()
</script>
<template>
  <PrecheckResult
    v-model:draft="draft"
    :loading="props.loading"
    :error="props.error"
    :result="props.result"
    :follow-up-loading="props.followUpLoading"
    :follow-up-error="props.followUpError"
    :conversation="props.conversation"
    :feedback-loading="props.feedbackLoading"
    :feedback-message="props.feedbackMessage"
    :debug="props.debug"
    :empty-message="props.emptyMessage"
    @send-follow-up="$emit('sendFollowUp', $event)"
    @feedback="$emit('feedback', $event)"
  /><button class="secondary continue-host" type="button" @click="$emit('continueSubmission')">
    忽略建议并继续 AIOps 原有提交
  </button>
</template>
