import { readCsrfToken } from '../identity/useAuthSession'
import type { AdminReset } from './types'

export async function listAdminResets(): Promise<{ items: AdminReset[] }> {
  return request('/api/v2/admin/resets')
}

export async function createAdminReset(reason: string): Promise<AdminReset> {
  return request('/api/v2/admin/resets', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'X-CSRF-Token': readCsrfToken(),
      'Idempotency-Key': crypto.randomUUID(),
    },
    body: JSON.stringify({
      environmentCode: 'LOCAL',
      confirmationPhrase: 'RESET LOCAL MOCK DATA',
      reason,
    }),
  })
}

async function request<T>(url: string, init?: RequestInit): Promise<T> {
  const response = await fetch(url, init)
  if (response.ok) return (await response.json()) as T
  const error = (await response.json()) as { message?: string }
  throw new Error(error.message || '本地模拟数据重置请求失败')
}
