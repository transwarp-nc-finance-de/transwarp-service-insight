import type { ApiError, PrecheckRequest, PrecheckResponse } from './types'

export async function runPrecheck(payload: PrecheckRequest): Promise<PrecheckResponse> {
  const response = await fetch('/api/v1/precheck', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  })
  if (!response.ok) {
    let message = '预诊服务暂时不可用'
    try {
      const body = (await response.json()) as ApiError
      message = body.message || message
    } catch {
      // 非 JSON 错误响应统一使用安全提示。
    }
    throw new Error(message)
  }
  return (await response.json()) as PrecheckResponse
}
