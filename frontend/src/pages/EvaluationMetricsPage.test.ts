import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, describe, expect, it, vi } from 'vitest'
import EvaluationMetricsPage from './EvaluationMetricsPage.vue'

afterEach(() => vi.unstubAllGlobals())

describe('evaluation metrics page', () => {
  it('shows persisted gate summaries and starts the fixed evaluation set', async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(ok({ items: [run()] }))
      .mockResolvedValueOnce(ok(metrics()))
      .mockResolvedValueOnce(ok({ items: [failure()] }))
      .mockResolvedValueOnce(ok({ ...run(), status: 'PENDING', summary: null }))
      .mockResolvedValueOnce(ok({ items: [run()] }))
      .mockResolvedValueOnce(ok(metrics()))
      .mockResolvedValueOnce(ok({ items: [failure()] }))
    vi.stubGlobal('fetch', fetchMock)
    vi.stubGlobal('crypto', { randomUUID: () => 'command-1' })

    const wrapper = mount(EvaluationMetricsPage)
    await flushPromises()
    expect(wrapper.get('[data-test="evaluation-run"]').text()).toContain('Recall@5 86.0%')
    expect(wrapper.get('[data-test="metrics"]').text()).toContain('Evidence 命中率')
    expect(wrapper.get('[data-test="evaluation-failures"]').text()).toContain(
      'MISSING_INFORMATION_MISMATCH',
    )
    expect(wrapper.text()).toContain('小样本工程评估，不代表生产效果')

    await wrapper.get('[data-test="evaluation-start"]').trigger('click')
    await flushPromises()
    expect(fetchMock.mock.calls[3][0]).toBe('/api/v2/evaluation-runs')
    expect(fetchMock.mock.calls[3][1].method).toBe('POST')
  })
})

function run() {
  return {
    taskId: 'task-1',
    status: 'SUCCEEDED',
    createdAt: '2026-07-20T10:00:00Z',
    evaluationSetVersion: 'mock-eval-v1',
    summary: {
      sampleCount: 30,
      gatePassed: true,
      permissionLeakageRate: 0,
      citationErrorRate: 0,
      degradationPassRate: 1,
      recallAt5: 0.86,
      disclaimer: '小样本工程评估，不代表生产效果',
    },
    error: null,
    mockData: true,
  }
}

function failure() {
  return {
    caseId: 'eval-001',
    scenarioTags: ['EXACT_TERM'],
    failedChecks: ['MISSING_INFORMATION'],
    failureCodes: ['MISSING_INFORMATION_MISMATCH'],
    expected: { evidenceCount: 1 },
    actual: { evidenceCount: 1 },
    mockData: true,
  }
}
function metrics() {
  return {
    from: '2026-07-13T00:00:00Z',
    to: '2026-07-20T00:00:00Z',
    precheckCount: 12,
    successRate: 0.9,
    degradationRate: 0.1,
    averageRunCount: 1.2,
    informationSupplementRate: 0.2,
    evidenceHitRate: 0.8,
    adoptionRate: 0.5,
    continuationRate: 0.4,
    retrievalP95Ms: 120,
    embeddingP95Ms: 80,
    mockData: true,
  }
}
function ok(value: object) {
  return { ok: true, json: async () => value } as Response
}
