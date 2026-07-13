import type { HostBridge, HostContext } from './HostBridge'
import {
  PROTOCOL_VERSION,
  type HostMessage,
  type HostMessageType,
  type PrecheckContext,
} from './protocol'

export class PostMessageHostBridge implements HostBridge {
  private context?: PrecheckContext
  private requestId = ''
  private readonly handled = new Set<string>()
  constructor(
    private readonly allowedOrigins: string[],
    private readonly target: Window = window.parent,
  ) {}
  initialize(): Promise<HostContext> {
    return new Promise((resolve, reject) => {
      const timeout = window.setTimeout(() => reject(new Error('等待 AIOps 初始化超时')), 10000)
      const listener = (event: MessageEvent) => {
        if (
          !this.allowedOrigins.includes(event.origin) ||
          JSON.stringify(event.data).length > 100_000
        )
          return
        const message = event.data as Partial<HostMessage<PrecheckContext>>
        if (
          message.type !== 'TSI_INIT' ||
          message.version !== PROTOCOL_VERSION ||
          !message.requestId ||
          !message.payload ||
          this.handled.has(message.requestId)
        )
          return
        this.handled.add(message.requestId)
        this.requestId = message.requestId
        this.context = message.payload
        window.clearTimeout(timeout)
        window.removeEventListener('message', listener)
        resolve({ requestId: this.requestId, debug: false })
      }
      window.addEventListener('message', listener)
    })
  }
  async getPrecheckContext() {
    if (!this.context) throw new Error('宿主上下文尚未初始化')
    return this.context
  }
  notifyReady() {
    this.send('TSI_READY', {})
  }
  notifyPrecheckStart(payload: unknown) {
    this.send('TSI_PRECHECK_START', payload)
  }
  notifyPrecheckResult(payload: unknown) {
    this.send('TSI_PRECHECK_RESULT', payload)
  }
  requestSupplementation(payload: unknown) {
    this.send('TSI_REQUEST_SUPPLEMENT', payload)
  }
  adoptChanges(payload: unknown) {
    this.send('TSI_ADOPT_CHANGES', payload)
  }
  continueSubmission(payload: unknown) {
    this.send('TSI_CONTINUE_SUBMISSION', payload)
  }
  openReference(payload: unknown) {
    this.send('TSI_REFERENCE_OPENED', payload)
  }
  close() {
    this.send('TSI_CLOSE', {})
  }
  reportError(payload: unknown) {
    this.send('TSI_ERROR', payload)
  }
  private send(type: HostMessageType, payload: unknown) {
    const message: HostMessage = {
      type,
      version: PROTOCOL_VERSION,
      requestId: this.requestId,
      timestamp: new Date().toISOString(),
      payload,
    }
    for (const origin of this.allowedOrigins) this.target.postMessage(message, origin)
  }
}
