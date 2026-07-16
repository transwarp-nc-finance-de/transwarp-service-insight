import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, describe, expect, it, vi } from 'vitest'
import PersistentPrecheckPage from './PersistentPrecheckPage.vue'

afterEach(() => vi.unstubAllGlobals())

describe('persistent precheck v2 sandbox', () => {
  it('creates run one, reaches the three-run limit and explicitly terminates', async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(ok({ items: [] }))
      .mockResolvedValueOnce(ok(session(1)))
      .mockResolvedValueOnce(ok(run(2)))
      .mockResolvedValueOnce(ok({ items: [run(2), run(1)] }))
      .mockResolvedValueOnce(ok(run(3)))
      .mockResolvedValueOnce(ok({ items: [run(3), run(2), run(1)] }))
      .mockResolvedValueOnce(
        ok({
          sessionId: 'session-1',
          status: 'TERMINATED',
          reason: 'SELF_SERVICE_CONFIRMED',
          mockData: true,
        }),
      )
    vi.stubGlobal('fetch', fetchMock)

    const wrapper = mount(PersistentPrecheckPage)
    await flushPromises()
    await wrapper.get('button.primary').trigger('click')
    await flushPromises()
    expect(wrapper.text()).toContain('第 1 次')

    await wrapper.get('[data-test="supplement"]').trigger('click')
    await flushPromises()
    await wrapper.get('[data-test="supplement"]').trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('3/3 Runs')
    expect(wrapper.get('[data-test="supplement"]').attributes('disabled')).toBeDefined()
    await wrapper.get('[data-test="continue"]').trigger('click')
    expect(wrapper.text()).toContain('由人工确认 SLA 提交')
    await wrapper.get('[data-test="terminate"]').trigger('click')
    await flushPromises()

    const terminationBody = JSON.parse(fetchMock.mock.calls[6][1].body as string)
    expect(terminationBody).toEqual({
      confirmed: true,
      reason: '模拟数据：已由人工明确确认采用自助建议',
    })
    expect(wrapper.text()).toContain('历史 Run 保持只读')
  })

  it('restores an active session, supplements a run and keeps human submission available', async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(ok({ items: [session(1)] }))
      .mockResolvedValueOnce(ok({ items: [run(1)] }))
      .mockResolvedValueOnce(ok(run(2)))
      .mockResolvedValueOnce(ok({ items: [run(1), run(2)] }))
    vi.stubGlobal('fetch', fetchMock)

    const wrapper = mount(PersistentPrecheckPage)
    await flushPromises()
    expect(wrapper.text()).toContain('第 1 次')
    await wrapper.get('[data-test="title"]').setValue('模拟数据：补充错误信息')
    await wrapper.get('[data-test="supplement"]').trigger('click')
    await flushPromises()

    expect(fetchMock.mock.calls[2][0]).toContain('/runs')
    expect(fetchMock.mock.calls[2][1]).toEqual(
      expect.objectContaining({ method: 'POST', credentials: 'include' }),
    )
    expect(wrapper.text()).toContain('第 2 次')
    await wrapper.get('[data-test="continue"]').trigger('click')
    expect(wrapper.text()).toContain('由人工确认 SLA 提交')
  })
})

function run(sequence: number) {
  return {
    runId: `run-${sequence}`,
    sessionId: 'session-1',
    sequence,
    status: 'NEED_MORE_INFORMATION',
    contextSnapshot: {
      sourceSystem: 'SANDBOX',
      hostRequestId: 'mock-host',
      formSchemaVersion: 'sandbox-v2',
      issueType: { code: 'FUNCTIONAL_FAILURE', displayName: '功能故障（模拟数据）' },
      productLine: { code: 'TDH', displayName: 'TDH（模拟数据）' },
      product: { code: 'INCEPTOR', displayName: 'Inceptor（模拟数据）' },
      component: { code: 'SQL_ENGINE', displayName: 'SQL 引擎（模拟数据）' },
      version: '9.1.0-mock',
      severity: { code: 'S2', displayName: 'S2（模拟数据）' },
      serviceType: { code: 'CONSULTATION', displayName: '咨询（模拟数据）' },
      title: '模拟数据：查询失败',
      descriptionPlainText: '模拟数据：SQL 返回 MOCK-1001。',
      additionalInformation: [],
      impactScope: '模拟数据：单个测试任务',
      attachments: [],
    },
    result: {
      summary: '模拟数据：建议补充信息。',
      recommendations: ['人工核对'],
      completeness: {
        complete: false,
        policyVersion: 'mock-completeness-v1',
        missingFieldCodes: ['ERROR_MESSAGE'],
        reasons: ['模拟数据：缺少字段'],
      },
      confidence: 'LOW',
      confidenceReasons: ['未接入检索'],
      humanInterventionAdvice: ['人工核对'],
      missingInformation: ['ERROR_MESSAGE'],
      allowedActions: ['SUPPLEMENT_INFORMATION', 'CONTINUE_SUBMISSION'],
      disclaimer: '不是最终根因、最终方案或正式复盘结论。',
      retrieval: { mode: 'UNAVAILABLE', degraded: true },
      mockData: true,
    },
  }
}

function session(sequence: number) {
  return {
    sessionId: 'session-1',
    ownerUserCode: 'mock-precheck-tdh',
    status: 'ACTIVE',
    latestRun: run(sequence),
    runCount: sequence,
    maxRuns: 3,
    mockData: true,
  }
}

function ok(body: object) {
  return { ok: true, status: 200, json: async () => body }
}
