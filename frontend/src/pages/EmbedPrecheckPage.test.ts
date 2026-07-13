import { mount } from '@vue/test-utils'
import { afterEach, describe, expect, it, vi } from 'vitest'
import EmbedPrecheckPage from './EmbedPrecheckPage.vue'

afterEach(() => {
  vi.clearAllTimers()
  vi.useRealTimers()
})

describe('Embedded precheck page', () => {
  it('does not duplicate the AIOps SLA form', () => {
    vi.useFakeTimers()
    const wrapper = mount(EmbedPrecheckPage)
    expect(wrapper.find('form').exists()).toBe(false)
    expect(wrapper.text()).not.toContain('模拟 AIOps SLA 表单')
    expect(wrapper.text()).toContain('表单与最终提交由 AIOps 负责')
    wrapper.unmount()
  })
})
