import type {
  ApiError,
  FeedbackRequest,
  FeedbackResponse,
  FollowUpRequest,
  FollowUpResponse,
  PrecheckRequest,
  PrecheckResponse,
} from './types'

export async function runPrecheck(payload: PrecheckRequest): Promise<PrecheckResponse> {
  const response = await fetch('/api/v1/precheck', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  })
  await ensureOk(response)
  return (await response.json()) as PrecheckResponse
}

export async function runFollowUp(payload: FollowUpRequest): Promise<FollowUpResponse> {
  const response = await fetch('/api/v1/precheck/follow-up', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  })
  await ensureOk(response)
  return (await response.json()) as FollowUpResponse
}

export async function recordFeedback(payload: FeedbackRequest): Promise<FeedbackResponse> {
  const response = await fetch('/api/v1/precheck/feedback', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  })
  await ensureOk(response)
  return (await response.json()) as FeedbackResponse
}

async function ensureOk(response: Response) {
  if (response.ok) return
  let message = '预诊服务暂时不可用'
  try {
    const body = (await response.json()) as ApiError
    message = body.message || message
  } catch {
    // 非 JSON 错误响应统一使用安全提示。
  }
  throw new Error(message)
}
