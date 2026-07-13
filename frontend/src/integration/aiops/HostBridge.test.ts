import { describe, expect, it, vi } from 'vitest'
import { SandboxHostBridge } from './SandboxHostBridge'
import { PostMessageHostBridge } from './PostMessageHostBridge'
import { PROTOCOL_VERSION, type PrecheckContext } from './protocol'
import { parseInitMessage } from './validation'

const context: PrecheckContext = {
  sourceSystem: 'SANDBOX',
  hostRequestId: 'request-1',
  formSchemaVersion: 'v1',
  title: '模拟问题',
  descriptionPlainText: '模拟描述',
}

describe('AIOps HostBridge protocol', () => {
  it('records sandbox continuation independently', () => {
    const bridge = new SandboxHostBridge(context)
    bridge.continueSubmission({ reason: 'manual' })
    expect(bridge.events).toEqual([
      { type: 'TSI_CONTINUE_SUBMISSION', payload: { reason: 'manual' } },
    ])
  })

  it('publishes precheck lifecycle events through the bridge', () => {
    const bridge = new SandboxHostBridge(context)
    bridge.notifyPrecheckStart({ source: 'SANDBOX' })
    bridge.notifyPrecheckResult({ status: 'COMPLETED' })
    expect(bridge.events.map((event) => event.type)).toEqual([
      'TSI_PRECHECK_START',
      'TSI_PRECHECK_RESULT',
    ])
  })

  it('accepts matching origin and version then emits ready', async () => {
    const target = { postMessage: vi.fn() } as unknown as Window
    const bridge = new PostMessageHostBridge(['https://aiops.example'], target)
    const initialization = bridge.initialize()
    window.dispatchEvent(
      new MessageEvent('message', {
        origin: 'https://aiops.example',
        data: {
          type: 'TSI_INIT',
          version: PROTOCOL_VERSION,
          requestId: 'request-1',
          timestamp: new Date().toISOString(),
          payload: context,
        },
      }),
    )
    await expect(initialization).resolves.toEqual({ requestId: 'request-1', debug: false })
    bridge.notifyReady()
    expect(target.postMessage).toHaveBeenCalledWith(
      expect.objectContaining({ type: 'TSI_READY', requestId: 'request-1' }),
      'https://aiops.example',
    )
  })

  it('ignores a wrong origin before accepting the allowed host', async () => {
    const bridge = new PostMessageHostBridge(['https://aiops.example'], {
      postMessage: vi.fn(),
    } as unknown as Window)
    const initialization = bridge.initialize()
    window.dispatchEvent(
      new MessageEvent('message', {
        origin: 'https://evil.example',
        data: {
          type: 'TSI_INIT',
          version: PROTOCOL_VERSION,
          requestId: 'bad',
          timestamp: new Date().toISOString(),
          payload: context,
        },
      }),
    )
    window.dispatchEvent(
      new MessageEvent('message', {
        origin: 'https://aiops.example',
        data: {
          type: 'TSI_INIT',
          version: PROTOCOL_VERSION,
          requestId: 'good',
          timestamp: new Date().toISOString(),
          payload: context,
        },
      }),
    )
    await expect(initialization).resolves.toEqual({ requestId: 'good', debug: false })
  })

  it('rejects a mismatched protocol version', async () => {
    const bridge = new PostMessageHostBridge(['https://aiops.example'], {
      postMessage: vi.fn(),
    } as unknown as Window)
    const initialization = bridge.initialize()
    window.dispatchEvent(
      new MessageEvent('message', {
        origin: 'https://aiops.example',
        data: {
          type: 'TSI_INIT',
          version: '2.0',
          requestId: 'wrong-version',
          timestamp: new Date().toISOString(),
          payload: context,
        },
      }),
    )
    window.dispatchEvent(
      new MessageEvent('message', {
        origin: 'https://aiops.example',
        data: {
          type: 'TSI_INIT',
          version: PROTOCOL_VERSION,
          requestId: 'valid-version',
          timestamp: new Date().toISOString(),
          payload: context,
        },
      }),
    )
    await expect(initialization).resolves.toEqual({ requestId: 'valid-version', debug: false })
  })

  it('rejects malformed or oversized host context', () => {
    expect(
      parseInitMessage({
        type: 'TSI_INIT',
        version: PROTOCOL_VERSION,
        requestId: 'request-1',
        timestamp: new Date().toISOString(),
        payload: { ...context, title: '' },
      }),
    ).toBeUndefined()
    expect(
      parseInitMessage({
        type: 'TSI_INIT',
        version: PROTOCOL_VERSION,
        requestId: 'request-1',
        timestamp: new Date().toISOString(),
        payload: { ...context, descriptionPlainText: 'x'.repeat(100_001) },
      }),
    ).toBeUndefined()
  })
})
