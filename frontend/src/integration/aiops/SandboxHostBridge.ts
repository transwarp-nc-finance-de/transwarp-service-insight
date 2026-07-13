import type { HostBridge, HostContext } from './HostBridge'
import type { PrecheckContext } from './protocol'

export class SandboxHostBridge implements HostBridge {
  readonly events: Array<{ type: string; payload?: unknown }> = []
  constructor(private context: PrecheckContext) {}
  setContext(context: PrecheckContext) {
    this.context = context
  }
  async initialize(): Promise<HostContext> {
    return { requestId: this.context.hostRequestId, debug: true }
  }
  async getPrecheckContext() {
    return this.context
  }
  notifyReady() {
    this.events.push({ type: 'TSI_READY' })
  }
  notifyPrecheckStart(payload: unknown) {
    this.events.push({ type: 'TSI_PRECHECK_START', payload })
  }
  notifyPrecheckResult(payload: unknown) {
    this.events.push({ type: 'TSI_PRECHECK_RESULT', payload })
  }
  requestSupplementation(payload: unknown) {
    this.events.push({ type: 'TSI_REQUEST_SUPPLEMENT', payload })
  }
  adoptChanges(payload: unknown) {
    this.events.push({ type: 'TSI_ADOPT_CHANGES', payload })
  }
  continueSubmission(payload: unknown) {
    this.events.push({ type: 'TSI_CONTINUE_SUBMISSION', payload })
  }
  openReference(payload: unknown) {
    this.events.push({ type: 'TSI_REFERENCE_OPENED', payload })
  }
  close() {
    this.events.push({ type: 'TSI_CLOSE' })
  }
  reportError(payload: unknown) {
    this.events.push({ type: 'TSI_ERROR', payload })
  }
}
