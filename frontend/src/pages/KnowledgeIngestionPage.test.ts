import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, describe, expect, it, vi } from 'vitest'
import KnowledgeIngestionPage from './KnowledgeIngestionPage.vue'

afterEach(() => vi.unstubAllGlobals())

describe('knowledge ingestion vertical flow', () => {
  it('uploads, polls and renders summary, blocks and chunks as mock data', async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(ok(created()))
      .mockResolvedValueOnce(ok({ ...created().parseTask, status: 'SUCCEEDED' }))
      .mockResolvedValueOnce(ok(summary()))
      .mockResolvedValueOnce(ok(page('block')))
      .mockResolvedValueOnce(ok(page('chunk')))
    vi.stubGlobal('fetch', fetchMock)

    const wrapper = mount(KnowledgeIngestionPage, { props: { csrfToken: 'csrf-editor' } })
    await wrapper.get('[data-test="title"]').setValue('Mock guide')
    const input = wrapper.get('[data-test="file"]')
    Object.defineProperty(input.element, 'files', {
      value: [new File(['# Mock data'], 'guide.md', { type: 'text/markdown' })],
    })
    await input.trigger('change')
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(fetchMock.mock.calls[0][0]).toBe('/api/v2/knowledge-documents')
    expect(fetchMock.mock.calls[0][1]).toEqual(
      expect.objectContaining({ method: 'POST', credentials: 'include' }),
    )
    expect(wrapper.text()).toContain('SUCCEEDED')
    expect(wrapper.text()).toContain('sha256:result')
    expect(wrapper.text()).toContain('block mock data')
    expect(wrapper.text()).toContain('chunk mock data')
    expect(wrapper.text()).toContain('模拟数据')
  })
})

function created() {
  return {
    version: { versionId: '33333333-3333-4333-8333-333333333333' },
    parseTask: { taskId: '44444444-4444-4444-8444-444444444444', status: 'PENDING' },
  }
}

function summary() {
  return {
    parseStatus: 'SUCCEEDED',
    parserVersion: 'text-structure-v1',
    parseResultHash: 'sha256:result',
    warnings: [],
  }
}

function page(kind: string) {
  return { items: [{ sequence: 1, text: `${kind} mock data` }], page: { page: 1, size: 20 } }
}

function ok(body: object) {
  return { ok: true, status: 200, headers: new Headers(), json: async () => body }
}
