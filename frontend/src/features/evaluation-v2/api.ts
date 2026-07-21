import { readCsrfToken } from '../identity/useAuthSession'
import type { EvaluationRun, Metrics } from './types'

async function body<T>(response: Response): Promise<T> {
  if (response.ok) return (await response.json()) as T
  const error = (await response.json().catch(() => ({}))) as { message?: string }
  throw new Error(error.message || '模拟数据：评估与指标服务暂时不可用。')
}

export async function createEvaluationRun(): Promise<EvaluationRun> {
  return body(
    await fetch('/api/v2/evaluation-runs', {
      method: 'POST',
      credentials: 'include',
      headers: {
        'Content-Type': 'application/json',
        'X-CSRF-Token': readCsrfToken(),
        'Idempotency-Key': `evaluation-${crypto.randomUUID()}`,
      },
      body: JSON.stringify({
        evaluationSetVersion: 'mock-eval-v1',
        note: '模拟数据：管理员手动回归',
      }),
    }),
  )
}

export async function listEvaluationRuns(): Promise<{ items: EvaluationRun[] }> {
  return body(
    await fetch('/api/v2/evaluation-runs?size=20&sortBy=createdAt&sortDirection=desc', {
      credentials: 'include',
    }),
  )
}

export async function getMetrics(from: string, to: string): Promise<Metrics> {
  const query = new URLSearchParams({ from, to })
  return body(await fetch(`/api/v2/metrics?${query}`, { credentials: 'include' }))
}
