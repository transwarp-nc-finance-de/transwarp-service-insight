import { readCsrfToken } from '../identity/useAuthSession'
import type {
  AdoptionStatus,
  Evidence,
  Feedback,
  Helpfulness,
  PrecheckContext,
  PrecheckRun,
  PrecheckSession,
  RunPage,
  SessionPage,
  SubmissionContinuation,
} from './types'

export async function listSessions(): Promise<SessionPage> {
  return request('/api/v2/precheck-sessions?size=20')
}

export async function listRuns(sessionId: string): Promise<RunPage> {
  return request(`/api/v2/precheck-sessions/${sessionId}/runs?size=20`)
}

export async function getEvidence(evidenceId: string): Promise<Evidence> {
  return request(`/api/v2/evidence/${evidenceId}`)
}

export async function createSession(context: PrecheckContext): Promise<PrecheckSession> {
  return write('/api/v2/precheck-sessions', { context })
}

export async function createRun(sessionId: string, context: PrecheckContext): Promise<PrecheckRun> {
  return write(`/api/v2/precheck-sessions/${sessionId}/runs`, { context }, commandKey())
}

export async function confirmSelfService(sessionId: string, reason: string) {
  return write(
    `/api/v2/precheck-sessions/${sessionId}/self-service-confirmations`,
    { confirmed: true, reason },
    commandKey(),
  )
}

export async function createFeedback(
  sessionId: string,
  runId: string,
  adoptionStatus: AdoptionStatus,
  helpfulness?: Helpfulness,
  reason?: string,
): Promise<Feedback> {
  return write(
    '/api/v2/feedback',
    compact({ sessionId, runId, adoptionStatus, helpfulness, reason }),
    commandKey(),
  )
}

export async function continueSubmission(
  sessionId: string,
  reason?: string,
): Promise<SubmissionContinuation> {
  return write(
    '/api/v2/submission-continuations',
    compact({ sessionId, confirmed: true, reason }),
    commandKey(),
  )
}

async function request<T>(url: string): Promise<T> {
  const response = await fetch(url, { credentials: 'include' })
  return decode<T>(response)
}

async function write<T>(url: string, body: object, idempotencyKey?: string): Promise<T> {
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    'X-CSRF-Token': readCsrfToken(),
  }
  if (idempotencyKey) headers['Idempotency-Key'] = idempotencyKey
  const response = await fetch(url, {
    method: 'POST',
    credentials: 'include',
    headers,
    body: JSON.stringify(body),
  })
  return decode<T>(response)
}

async function decode<T>(response: Response): Promise<T> {
  if (response.ok) return (await response.json()) as T
  const body = (await response.json().catch(() => ({}))) as { message?: string }
  throw new Error(body.message || '模拟数据：持久化预诊服务暂时不可用，仍可继续人工提交。')
}

function commandKey() {
  return `sandbox-${crypto.randomUUID()}`
}

function compact<T extends object>(value: T): T {
  return Object.fromEntries(Object.entries(value).filter(([, item]) => item !== undefined)) as T
}
