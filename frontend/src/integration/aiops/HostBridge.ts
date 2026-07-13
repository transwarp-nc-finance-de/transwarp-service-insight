import type { PrecheckContext } from './protocol'

export interface HostContext {
  requestId: string
  debug: boolean
}
export interface HostBridge {
  initialize(): Promise<HostContext>
  getPrecheckContext(): Promise<PrecheckContext>
  notifyReady(): void
  notifyPrecheckStart(payload: unknown): void
  notifyPrecheckResult(payload: unknown): void
  requestSupplementation(payload: unknown): void
  adoptChanges(payload: unknown): void
  continueSubmission(payload: unknown): void
  openReference(payload: unknown): void
  close(): void
  reportError(payload: unknown): void
}
