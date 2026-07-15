import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, describe, expect, it, vi } from 'vitest'
import AuthSessionPanel from './AuthSessionPanel.vue'

const editorSession = {
  sessionId: '11111111-1111-4111-8111-111111111111',
  userCode: 'mock-knowledge-editor',
  displayName: '知识编辑人员（模拟数据）',
  roles: ['KNOWLEDGE_EDITOR'],
  productLineCodes: ['STREAMING', 'TDH'],
  expiresAt: '2026-07-15T08:00:00Z',
  mockData: true,
}

afterEach(() => vi.unstubAllGlobals())

describe('local mock identity flow', () => {
  it('logs in, exposes confirmed authorization and logs out with the in-memory CSRF token', async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(errorResponse(401, 'UNAUTHENTICATED'))
      .mockResolvedValueOnce(ok(editorSession, 'csrf-editor'))
      .mockResolvedValueOnce({ ok: true, status: 204, headers: new Headers() })
    vi.stubGlobal('fetch', fetchMock)

    const wrapper = mount(AuthSessionPanel)
    await flushPromises()
    expect(wrapper.text()).toContain('本地模拟身份（模拟数据）')

    await wrapper.get('select').setValue('mock-knowledge-editor')
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(fetchMock.mock.calls[1]).toEqual([
      '/api/v2/auth-sessions',
      expect.objectContaining({
        method: 'POST',
        credentials: 'include',
        body: JSON.stringify({ userCode: 'mock-knowledge-editor', mockData: true }),
      }),
    ])
    expect(wrapper.text()).toContain('知识编辑人员（模拟数据）')
    expect(wrapper.text()).toContain('KNOWLEDGE_EDITOR')
    expect(wrapper.text()).toContain('STREAMING')
    expect(wrapper.text()).toContain('TDH')

    await wrapper.get('[data-test="logout"]').trigger('click')
    await flushPromises()
    expect(fetchMock.mock.calls[2]).toEqual([
      '/api/v2/auth-sessions/current',
      expect.objectContaining({
        method: 'DELETE',
        credentials: 'include',
        headers: { 'X-CSRF-Token': 'csrf-editor' },
      }),
    ])
    expect(wrapper.text()).toContain('请选择模拟身份')
  })

  it('restores a session after refresh and rotates visible identity on switch', async () => {
    const reviewer = {
      ...editorSession,
      sessionId: '22222222-2222-4222-8222-222222222222',
      userCode: 'mock-knowledge-reviewer',
      displayName: '知识审核人员（模拟数据）',
      roles: ['KNOWLEDGE_REVIEWER'],
    }
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(ok(editorSession, 'csrf-editor'))
      .mockResolvedValueOnce(ok(reviewer, 'csrf-reviewer'))
    vi.stubGlobal('fetch', fetchMock)

    const wrapper = mount(AuthSessionPanel)
    await flushPromises()
    expect(fetchMock.mock.calls[0][0]).toBe('/api/v2/auth-sessions/current')
    expect(fetchMock.mock.calls[0][1]).toEqual(expect.objectContaining({ credentials: 'include' }))
    expect(wrapper.text()).toContain('知识编辑人员（模拟数据）')

    await wrapper.get('select').setValue('mock-knowledge-reviewer')
    await wrapper.get('form').trigger('submit')
    await flushPromises()
    expect(wrapper.text()).toContain('知识审核人员（模拟数据）')
    expect(wrapper.text()).toContain('KNOWLEDGE_REVIEWER')
  })
})

function ok(body: object, csrfToken: string) {
  return {
    ok: true,
    status: 200,
    headers: new Headers({ 'X-CSRF-Token': csrfToken }),
    json: async () => body,
  }
}

function errorResponse(status: number, code: string) {
  return {
    ok: false,
    status,
    headers: new Headers(),
    json: async () => ({ code, message: '未登录', safeDetails: { mockData: true } }),
  }
}
