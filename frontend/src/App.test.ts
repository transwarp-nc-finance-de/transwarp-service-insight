import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, describe, expect, it, vi } from 'vitest'
import App from './App.vue'
import type {
  FeedbackResponse,
  FollowUpResponse,
  PrecheckResponse,
} from './features/precheck/types'

const response: PrecheckResponse = {
  precheckId: '123e4567-e89b-12d3-a456-426614174000',
  sessionId: '523e4567-e89b-12d3-a456-426614174000',
  summary: '模拟预诊辅助摘要，不是最终根因。',
  recommendations: ['请人工核对影响范围。'],
  references: [
    {
      sourceType: 'PRODUCT_MANUAL',
      title: '产品手册（模拟数据）',
      excerpt: '模拟来源摘要',
      url: 'https://example.com/mock',
      mockData: true,
    },
  ],
  confidence: 'MEDIUM',
  confidenceReason: '仍缺少 1 项可核验信息。',
  humanReviewRequired: true,
  missingInformation: ['版本'],
  fallbackReason: '未调用真实 RAG 或模型。',
  nextAction: 'NEED_MORE_INFORMATION',
  nextActionReason: '仍有信息需要人工补充。',
  allowedActions: ['SUPPLEMENT_INFORMATION', 'CONTINUE_SUBMISSION'],
  status: 'NEED_MORE_INFORMATION',
  policyVersion: 'mock-policy-v1',
  modelVersion: 'not-applicable-deterministic-mock',
  promptVersion: 'mock-rule-v1',
  indexVersion: 'not-applicable-no-index',
}

const followUpResponse: FollowUpResponse = {
  followUpId: '223e4567-e89b-12d3-a456-426614174000',
  precheckId: response.precheckId,
  reply: '模拟数据：已记录日志线索，不是最终根因。',
  recommendations: ['请人工核对错误时间。'],
  references: response.references,
  confidence: 'MEDIUM',
  confidenceReason: '仅命中确定性关键词规则。',
  humanReviewRequired: true,
  missingInformation: ['完整错误码'],
  fallbackReason: '模拟数据：未调用真实模型。',
  nextAction: 'MANUAL_REVIEW_REQUIRED',
  nextActionReason: '已形成模拟辅助方向，仍需人工审核。',
  allowedActions: ['SUPPLEMENT_INFORMATION', 'CONTINUE_SUBMISSION'],
  status: 'COMPLETED',
  policyVersion: 'mock-policy-v1',
  modelVersion: 'not-applicable-deterministic-mock',
  promptVersion: 'mock-rule-v1',
  indexVersion: 'not-applicable-no-index',
}

const feedbackResponse: FeedbackResponse = {
  feedbackId: '423e4567-e89b-12d3-a456-426614174000',
  precheckId: response.precheckId,
  adoptionStatus: 'ADOPTED',
  continuedSubmission: false,
  policyVersion: 'mock-policy-v1',
  recorded: true,
  mockData: true,
}

afterEach(() => vi.unstubAllGlobals())

describe('SLA precheck flow', () => {
  it('renders summary, source and missing information after success', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(ok(response)))
    const wrapper = mount(App)
    await wrapper.get('form').trigger('submit')
    await flushPromises()
    expect(wrapper.text()).toContain(response.summary)
    expect(wrapper.text()).toContain('产品手册（模拟数据）')
    expect(wrapper.text()).toContain('待补充信息')
    expect(wrapper.text()).toContain('版本')
    expect(wrapper.text()).toContain('建议补充信息')
    expect(wrapper.text()).toContain('继续人工提交')
    expect(wrapper.text()).toContain(response.confidenceReason)
    expect(wrapper.text()).toContain('mock-rule-v1')
  })

  it('shows loading and disables only the precheck action', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(() => new Promise(() => {})),
    )
    const wrapper = mount(App)
    await wrapper.get('form').trigger('submit')
    expect(wrapper.text()).toContain('正在调用本地 Mock API')
    expect(wrapper.get('button.primary').attributes('disabled')).toBeDefined()
    expect(wrapper.get('button.secondary').attributes('disabled')).toBeUndefined()
  })

  it('validates required input without blocking manual submission', async () => {
    const wrapper = mount(App)
    await wrapper.get('input').setValue('')
    await wrapper.get('form').trigger('submit')
    expect(wrapper.text()).toContain('请先填写标题和问题描述')
    await wrapper.get('button.secondary').trigger('click')
    expect(wrapper.text()).toContain('未调用真实提交接口')
  })

  it.each([
    [
      '400 response',
      vi.fn().mockResolvedValue({ ok: false, json: async () => ({ message: '请求参数校验失败' }) }),
      '请求参数校验失败',
    ],
    ['network failure', vi.fn().mockRejectedValue(new Error('网络不可用')), '网络不可用'],
  ])('keeps manual submission after %s', async (_name, fetchMock, message) => {
    vi.stubGlobal('fetch', fetchMock)
    const wrapper = mount(App)
    await wrapper.get('form').trigger('submit')
    await flushPromises()
    expect(wrapper.text()).toContain(message)
    expect(wrapper.text()).toContain('可以继续人工提交')
    await wrapper.get('button.secondary').trigger('click')
    expect(wrapper.text()).toContain('未调用真实提交接口')
  })

  it('clears the old result when precheck is repeated', async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(ok(response))
      .mockImplementationOnce(() => new Promise(() => {}))
    vi.stubGlobal('fetch', fetchMock)
    const wrapper = mount(App)
    await wrapper.get('form').trigger('submit')
    await flushPromises()
    expect(wrapper.text()).toContain(response.summary)
    await wrapper.get('form').trigger('submit')
    expect(wrapper.text()).not.toContain(response.summary)
    expect(wrapper.text()).toContain('正在调用本地 Mock API')
  })

  it('supports quick and free follow-ups while preserving ordered history', async () => {
    const second = {
      ...followUpResponse,
      followUpId: '323e4567-e89b-12d3-a456-426614174000',
      reply: '模拟数据：第二轮回复。',
    }
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(ok(response))
      .mockResolvedValueOnce(ok(followUpResponse))
      .mockResolvedValueOnce(ok(second))
    vi.stubGlobal('fetch', fetchMock)
    const wrapper = mount(App)
    await wrapper.get('form').trigger('submit')
    await flushPromises()
    const quick = wrapper.findAll('.conversation .quick-actions button')[0]
    await quick.trigger('click')
    await flushPromises()
    await wrapper.get('.follow-up-form textarea').setValue('补充一个脱敏现象')
    await wrapper.get('.follow-up-form').trigger('submit')
    await flushPromises()
    expect(fetchMock.mock.calls[1][0]).toBe('/api/v1/precheck/follow-up')
    const turns = wrapper.findAll('.turn')
    expect(turns).toHaveLength(2)
    expect(turns[0].text()).toContain('补充日志现象')
    expect(turns[1].text()).toContain('补充一个脱敏现象')
    expect(wrapper.text()).toContain('完整错误码')
  })

  it('blocks empty follow-up and disables only follow-up controls while sending', async () => {
    const followUpPending = new Promise(() => {})
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(ok(response))
      .mockReturnValueOnce(followUpPending)
    vi.stubGlobal('fetch', fetchMock)
    const wrapper = mount(App)
    await wrapper.get('form').trigger('submit')
    await flushPromises()
    const sendButton = wrapper.get('.follow-up-form button')
    expect(sendButton.attributes('disabled')).toBeDefined()
    await wrapper.get('.follow-up-form').trigger('submit')
    expect(fetchMock).toHaveBeenCalledTimes(1)
    await wrapper.get('.follow-up-form textarea').setValue('补充权限信息')
    await wrapper.get('.follow-up-form').trigger('submit')
    expect(wrapper.text()).toContain('发送中')
    expect(wrapper.get('button.secondary').attributes('disabled')).toBeUndefined()
    expect(
      wrapper
        .findAll('.conversation .quick-actions button')
        .every((button) => button.attributes('disabled') !== undefined),
    ).toBe(true)
  })

  it('keeps history and draft after a follow-up failure, then clears both on new precheck', async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(ok(response))
      .mockResolvedValueOnce(ok(followUpResponse))
      .mockRejectedValueOnce(new Error('追问网络失败'))
      .mockResolvedValueOnce(ok(response))
    vi.stubGlobal('fetch', fetchMock)
    const wrapper = mount(App)
    await wrapper.get('form').trigger('submit')
    await flushPromises()
    await wrapper.findAll('.conversation .quick-actions button')[0].trigger('click')
    await flushPromises()
    await wrapper.get('.follow-up-form textarea').setValue('需要保留的输入')
    await wrapper.get('.follow-up-form').trigger('submit')
    await flushPromises()
    expect(wrapper.text()).toContain('追问网络失败')
    expect(wrapper.get<HTMLTextAreaElement>('.follow-up-form textarea').element.value).toBe(
      '需要保留的输入',
    )
    expect(wrapper.findAll('.turn')).toHaveLength(1)
    expect(wrapper.get('button.secondary').attributes('disabled')).toBeUndefined()
    await wrapper.get('form').trigger('submit')
    await flushPromises()
    expect(wrapper.findAll('.turn')).toHaveLength(0)
    expect(wrapper.get<HTMLTextAreaElement>('.follow-up-form textarea').element.value).toBe('')
  })

  it('records adoption feedback and manual continuation without blocking the form', async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(ok(response))
      .mockResolvedValueOnce(ok(feedbackResponse))
      .mockResolvedValueOnce(
        ok({ ...feedbackResponse, adoptionStatus: 'IGNORED', continuedSubmission: true }),
      )
    vi.stubGlobal('fetch', fetchMock)
    const wrapper = mount(App)
    await wrapper.get('form').trigger('submit')
    await flushPromises()
    await wrapper.findAll('.feedback button')[0].trigger('click')
    await flushPromises()
    expect(fetchMock.mock.calls[1][0]).toBe('/api/v1/precheck/feedback')
    expect(wrapper.text()).toContain('已记录本次反馈（模拟数据）')
    await wrapper.get('button.secondary').trigger('click')
    await flushPromises()
    expect(fetchMock.mock.calls[2][0]).toBe('/api/v1/precheck/feedback')
    expect(wrapper.text()).toContain('提交内容仍由人工确认')
    expect(wrapper.text()).toContain('未调用真实提交接口')
  })
})

function ok(body: PrecheckResponse | FollowUpResponse | FeedbackResponse) {
  return { ok: true, json: async () => body }
}
