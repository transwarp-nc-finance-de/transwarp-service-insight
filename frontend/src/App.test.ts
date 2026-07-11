import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, describe, expect, it, vi } from 'vitest'
import App from './App.vue'
import type { PrecheckResponse } from './features/precheck/types'

const response: PrecheckResponse = {
  precheckId: '123e4567-e89b-12d3-a456-426614174000',
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
  humanReviewRequired: true,
  missingInformation: ['版本'],
  fallbackReason: '未调用真实 RAG 或模型。',
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
})

function ok(body: PrecheckResponse) {
  return { ok: true, json: async () => body }
}
