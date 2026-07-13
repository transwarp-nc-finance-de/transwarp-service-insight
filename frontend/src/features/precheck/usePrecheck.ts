import { ref } from 'vue'
import { recordFeedback, runFollowUp, runPrecheck } from './api'
import type { AdoptionStatus, ConversationTurn, PrecheckRequest, PrecheckResponse } from './types'

export function usePrecheck() {
  const loading = ref(false)
  const error = ref('')
  const result = ref<PrecheckResponse>()
  const followUpLoading = ref(false)
  const followUpError = ref('')
  const followUpDraft = ref('')
  const conversation = ref<ConversationTurn[]>([])
  const feedbackLoading = ref(false)
  const feedbackMessage = ref('')

  async function run(payload: PrecheckRequest) {
    error.value = ''
    result.value = undefined
    conversation.value = []
    followUpDraft.value = ''
    followUpError.value = ''
    feedbackMessage.value = ''
    loading.value = true
    try {
      result.value = await runPrecheck(payload)
    } catch (cause) {
      error.value = `${cause instanceof Error ? cause.message : '预诊失败'}；可以继续人工提交。`
    } finally {
      loading.value = false
    }
  }

  async function sendFeedback(adoptionStatus: AdoptionStatus, continuedSubmission = false) {
    if (!result.value || feedbackLoading.value) return
    feedbackLoading.value = true
    feedbackMessage.value = ''
    try {
      await recordFeedback({
        precheckId: result.value.precheckId,
        adoptionStatus,
        continuedSubmission,
      })
      feedbackMessage.value = continuedSubmission
        ? '已记录人工继续提交选择（模拟数据），提交内容仍由人工确认。'
        : '已记录本次反馈（模拟数据）。'
    } catch (cause) {
      feedbackMessage.value = `${cause instanceof Error ? cause.message : '反馈记录失败'}；不影响继续人工提交。`
    } finally {
      feedbackLoading.value = false
    }
  }

  async function sendFollowUp(message = followUpDraft.value) {
    const normalized = message.trim()
    if (!result.value || !normalized || followUpLoading.value) return
    followUpError.value = ''
    followUpLoading.value = true
    if (message !== followUpDraft.value) followUpDraft.value = message
    try {
      const response = await runFollowUp({
        precheckId: result.value.precheckId,
        sessionId: result.value.sessionId,
        message: normalized,
      })
      conversation.value.push({ message: normalized, response })
      followUpDraft.value = ''
    } catch (cause) {
      followUpError.value = `${cause instanceof Error ? cause.message : '追问失败'}；历史已保留，可以继续人工提交。`
    } finally {
      followUpLoading.value = false
    }
  }

  return {
    loading,
    error,
    result,
    run,
    followUpLoading,
    followUpError,
    followUpDraft,
    conversation,
    sendFollowUp,
    feedbackLoading,
    feedbackMessage,
    sendFeedback,
  }
}
