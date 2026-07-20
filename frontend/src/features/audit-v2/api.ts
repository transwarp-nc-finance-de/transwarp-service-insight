export interface AuditEvent {
  eventId: string
  actorUserCode: string
  action: string
  subjectType: string
  subjectId: string
  outcome: string
  metadata: Record<string, string | boolean | number>
  occurredAt: string
  mockData: boolean
}

export async function listAuditEvents(action = ''): Promise<{ items: AuditEvent[] }> {
  const query = new URLSearchParams({ size: '50', sortDirection: 'DESC' })
  if (action) query.set('action', action)
  const response = await fetch(`/api/v2/audit-events?${query}`, { credentials: 'include' })
  if (response.ok) return (await response.json()) as { items: AuditEvent[] }
  const body = (await response.json().catch(() => ({}))) as { message?: string }
  throw new Error(body.message || '模拟数据：结构化审计暂时不可用。')
}
