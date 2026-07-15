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

  it('submits the current parse result and explicitly acknowledges warnings before approval', async () => {
    const warned = {
      ...summary(),
      versionStatus: 'IN_REVIEW',
      submittedBy: 'mock-knowledge-editor',
      warnings: [{ code: 'CONTENT_LOSS_SUSPECTED', message: '模拟数据：可能存在内容丢失' }],
    }
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(ok(created()))
      .mockResolvedValueOnce(ok({ ...created().parseTask, status: 'SUCCEEDED' }))
      .mockResolvedValueOnce(ok(warned))
      .mockResolvedValueOnce(ok(page('block')))
      .mockResolvedValueOnce(ok(page('chunk')))
      .mockResolvedValueOnce(ok(commandResult('IN_REVIEW')))
      .mockResolvedValueOnce(ok(warned))
      .mockResolvedValueOnce(ok(page('review block')))
      .mockResolvedValueOnce(ok(page('review chunk')))
      .mockResolvedValueOnce(ok(commandResult('APPROVED')))
    vi.stubGlobal('fetch', fetchMock)

    const editor = mount(KnowledgeIngestionPage, { props: { csrfToken: 'csrf-editor' } })
    await uploadFile(editor)
    expect(editor.get('[data-test="approve"]').attributes('disabled')).toBeDefined()
    await editor.get('[data-test="submit-review"]').trigger('click')
    await flushPromises()
    expect(fetchMock.mock.calls[5][0]).toContain('/review-submissions')
    expect(JSON.parse(fetchMock.mock.calls[5][1].body)).toEqual({
      parseResultHash: 'sha256:result',
    })
    editor.unmount()

    const reviewer = mount(KnowledgeIngestionPage, { props: { csrfToken: 'csrf-reviewer' } })
    await reviewer.get('[data-test="review-version-id"]').setValue(created().version.versionId)
    await reviewer.get('[data-test="load-review-version"]').trigger('click')
    await flushPromises()
    expect(fetchMock.mock.calls[6][0]).toContain('/parse-preview')
    await reviewer.get('[data-test="warning-CONTENT_LOSS_SUSPECTED"]').setValue(true)
    await reviewer.get('[data-test="approve"]').trigger('click')
    await flushPromises()
    expect(fetchMock.mock.calls[9][1].headers['X-CSRF-Token']).toBe('csrf-reviewer')
    expect(JSON.parse(fetchMock.mock.calls[9][1].body).acknowledgedWarningCodes).toEqual([
      'CONTENT_LOSS_SUSPECTED',
    ])
    expect(reviewer.text()).toContain('APPROVED')
  })

  it('returns an in-review version then creates a new immutable draft revision', async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(ok(created()))
      .mockResolvedValueOnce(ok({ ...created().parseTask, status: 'SUCCEEDED' }))
      .mockResolvedValueOnce(ok(summary()))
      .mockResolvedValueOnce(ok(page('block')))
      .mockResolvedValueOnce(ok(page('chunk')))
      .mockResolvedValueOnce(ok(commandResult('IN_REVIEW')))
      .mockResolvedValueOnce(
        ok({ ...summary(), versionStatus: 'IN_REVIEW', submittedBy: 'mock-knowledge-editor' }),
      )
      .mockResolvedValueOnce(ok(page('review block')))
      .mockResolvedValueOnce(ok(page('review chunk')))
      .mockResolvedValueOnce(ok(commandResult('DRAFT')))
      .mockResolvedValueOnce(ok(summary()))
      .mockResolvedValueOnce(ok(page('returned block')))
      .mockResolvedValueOnce(ok(page('returned chunk')))
      .mockResolvedValueOnce(
        ok({
          revision: { revisionNumber: 2, cleanedTextHash: 'sha256:new' },
          parseTask: { taskId: 'new-task', status: 'PENDING' },
        }),
      )
      .mockResolvedValueOnce(ok({ taskId: 'new-task', status: 'SUCCEEDED' }))
      .mockResolvedValueOnce(ok({ ...summary(), parseResultHash: 'sha256:new-result' }))
      .mockResolvedValueOnce(ok(page('new block')))
      .mockResolvedValueOnce(ok(page('new chunk')))
    vi.stubGlobal('fetch', fetchMock)

    const editor = mount(KnowledgeIngestionPage, { props: { csrfToken: 'csrf-editor' } })
    await uploadFile(editor)
    await editor.get('[data-test="submit-review"]').trigger('click')
    await flushPromises()
    editor.unmount()

    const reviewer = mount(KnowledgeIngestionPage, { props: { csrfToken: 'csrf-reviewer' } })
    await reviewer.get('[data-test="review-version-id"]').setValue(created().version.versionId)
    await reviewer.get('[data-test="load-review-version"]').trigger('click')
    await flushPromises()
    await reviewer.get('[data-test="return-reason"]').setValue('模拟数据：请补充说明')
    await reviewer.get('[data-test="return-draft"]').trigger('click')
    await flushPromises()
    expect(fetchMock.mock.calls[9][1].headers['X-CSRF-Token']).toBe('csrf-reviewer')
    reviewer.unmount()

    const revisingEditor = mount(KnowledgeIngestionPage, { props: { csrfToken: 'csrf-editor' } })
    await revisingEditor
      .get('[data-test="review-version-id"]')
      .setValue(created().version.versionId)
    await revisingEditor.get('[data-test="load-review-version"]').trigger('click')
    await flushPromises()
    await revisingEditor.get('[data-test="title"]').setValue('Revised mock guide')
    await revisingEditor.get('[data-test="cleaned-text"]').setValue('# 模拟数据\n\n修订正文')
    await revisingEditor.get('[data-test="revise"]').trigger('click')
    await flushPromises()

    expect(fetchMock.mock.calls[13][0]).toContain('/revisions')
    expect(fetchMock.mock.calls[13][1].body).toBeInstanceOf(FormData)
    expect(revisingEditor.text()).toContain('sha256:new-result')
    expect(revisingEditor.text()).toContain('new block mock data')
  })
})

async function uploadFile(wrapper: ReturnType<typeof mount>) {
  await wrapper.get('[data-test="title"]').setValue('Mock guide')
  const input = wrapper.get('[data-test="file"]')
  Object.defineProperty(input.element, 'files', {
    value: [new File(['# Mock data'], 'guide.md', { type: 'text/markdown' })],
  })
  await input.trigger('change')
  await wrapper.get('form').trigger('submit')
  await flushPromises()
}

function created() {
  return {
    version: { versionId: '33333333-3333-4333-8333-333333333333', status: 'DRAFT' },
    parseTask: { taskId: '44444444-4444-4444-8444-444444444444', status: 'PENDING' },
  }
}

function summary() {
  return {
    versionStatus: 'DRAFT',
    submittedBy: null,
    parseStatus: 'SUCCEEDED',
    parserVersion: 'text-structure-v1',
    parseResultHash: 'sha256:result',
    warnings: [],
  }
}

function page(kind: string) {
  return { items: [{ sequence: 1, text: `${kind} mock data` }], page: { page: 1, size: 20 } }
}

function commandResult(status: string) {
  return {
    version: {
      versionId: created().version.versionId,
      status,
      submittedBy: status === 'DRAFT' ? 'mock-knowledge-editor' : 'mock-knowledge-editor',
      approvedBy: status === 'APPROVED' ? 'mock-knowledge-reviewer' : null,
    },
    auditEventId: 'audit-event',
  }
}

function ok(body: object) {
  return { ok: true, status: 200, headers: new Headers(), json: async () => body }
}
