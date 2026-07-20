import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, describe, expect, it, vi } from 'vitest'
import AuditEventsPage from './AuditEventsPage.vue'

afterEach(() => vi.unstubAllGlobals())

describe('admin structured audit page', () => {
  it('shows only structured redacted metadata and supports action filtering', async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      ok({
        items: [
          {
            eventId: 'event-1',
            actorUserCode: 'mock-precheck-tdh',
            action: 'FEEDBACK_RECORDED',
            subjectType: 'Feedback',
            subjectId: 'feedback-1',
            outcome: 'SUCCEEDED',
            metadata: { productLineCode: 'TDH', adoptionStatus: 'PARTIALLY_ADOPTED' },
            occurredAt: '2026-07-20T10:00:00Z',
            mockData: true,
          },
        ],
      }),
    )
    vi.stubGlobal('fetch', fetchMock)

    const wrapper = mount(AuditEventsPage)
    await flushPromises()
    expect(wrapper.get('[data-test="audit-event"]').text()).toContain('FEEDBACK_RECORDED')
    expect(wrapper.text()).not.toContain('反馈正文秘密')

    await wrapper.get('[data-test="audit-action"]').setValue('SUBMISSION_CONTINUED')
    await wrapper.get('[data-test="audit-refresh"]').trigger('click')
    await flushPromises()
    expect(fetchMock.mock.calls[1][0]).toContain('action=SUBMISSION_CONTINUED')
    expect(fetchMock.mock.calls[1][1]).toEqual({ credentials: 'include' })
  })
})

function ok(body: object) {
  return { ok: true, status: 200, json: async () => body }
}
