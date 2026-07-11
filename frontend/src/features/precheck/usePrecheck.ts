import { ref } from 'vue'
import { runPrecheck } from './api'
import type { PrecheckRequest, PrecheckResponse } from './types'

export function usePrecheck() {
  const loading = ref(false)
  const error = ref('')
  const result = ref<PrecheckResponse>()

  async function run(payload: PrecheckRequest) {
    error.value = ''
    result.value = undefined
    loading.value = true
    try {
      result.value = await runPrecheck(payload)
    } catch (cause) {
      error.value = `${cause instanceof Error ? cause.message : '预诊失败'}；可以继续人工提交。`
    } finally {
      loading.value = false
    }
  }

  return { loading, error, result, run }
}
