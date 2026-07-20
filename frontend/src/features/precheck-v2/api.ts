import { readCsrfToken } from '../identity/useAuthSession'
import type {
  Evidence,
  PrecheckContext,
  PrecheckRun,
  PrecheckSession,
  RunPage,
  SessionPage,
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
