import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, describe, expect, it, vi } from 'vitest'
import AdminResetPage from './AdminResetPage.vue'

afterEach(() => vi.unstubAllGlobals())

describe('admin reset page', () => {
  it('requires the fixed second confirmation and explains the local-only boundary', async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(ok({ items: [reset('SUCCEEDED')] }))
      .mockResolvedValueOnce(ok(reset('PENDING'), { 'Idempotency-Replayed': 'false' }))
    vi.stubGlobal('fetch', fetchMock)
    vi.stubGlobal('crypto', { randomUUID: () => 'reset-command-1' })

    const wrapper = mount(AdminResetPage)
    await flushPromises()
    expect(wrapper.text()).toContain('不是备份、灾备或生产恢复')
    expect(wrapper.text()).toContain('会注销当前本地会话')
    expect(wrapper.get('[data-test="reset-submit"]').attributes('disabled')).toBeDefined()

    await wrapper.get('[data-test="reset-reason"]').setValue('模拟数据：恢复演示初始状态')
    await wrapper.get('[data-test="reset-confirmation"]').setValue('RESET LOCAL MOCK DATA')
    expect(wrapper.get('[data-test="reset-submit"]').attributes('disabled')).toBeUndefined()
    await wrapper.get('[data-test="reset-submit"]').trigger('click')
    await flushPromises()

    expect(fetchMock.mock.calls[1][0]).toBe('/api/v2/admin/resets')
    expect(fetchMock.mock.calls[1][1].method).toBe('POST')
    expect(fetchMock.mock.calls[1][1].headers['Idempotency-Key']).toBe('reset-command-1')
    expect(JSON.parse(fetchMock.mock.calls[1][1].body)).toEqual({
      environmentCode: 'LOCAL',
      confirmationPhrase: 'RESET LOCAL MOCK DATA',
      reason: '模拟数据：恢复演示初始状态',
    })
    expect(wrapper.get('[data-test="reset-latest"]').text()).toContain('PENDING')
  })
})

function reset(status: string) {
  return {
    taskId: '11111111-1111-4111-8111-111111111111',
    resourceId: '11111111-1111-4111-8111-111111111111',
    status,
    attempt: status === 'PENDING' ? 0 : 1,
    maxAttempts: 3,
    error: null,
    nextRetryAt: null,
    createdAt: '2026-07-21T09:00:00Z',
    startedAt: null,
    completedAt: status === 'SUCCEEDED' ? '2026-07-21T09:00:10Z' : null,
    mockData: true,
    environmentCode: 'LOCAL',
    confirmedBy: 'mock-admin',
    auditEventId: '22222222-2222-4222-8222-222222222222',
  }
}

function ok(value: object, headers: Record<string, string> = {}) {
  return { ok: true, json: async () => value, headers: new Headers(headers) } as Response
}
