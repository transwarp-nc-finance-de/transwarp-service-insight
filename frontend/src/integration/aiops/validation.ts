import { PROTOCOL_VERSION, type HostMessage, type PrecheckContext } from './protocol'

export const MAX_HOST_MESSAGE_BYTES = 100_000

export function parseInitMessage(data: unknown): HostMessage<PrecheckContext> | undefined {
  if (!isRecord(data) || safeSize(data) > MAX_HOST_MESSAGE_BYTES) return undefined
  if (data.type !== 'TSI_INIT' || data.version !== PROTOCOL_VERSION) return undefined
  if (!isText(data.requestId, 1, 100) || !isText(data.timestamp, 1, 64)) return undefined
  if (!isPrecheckContext(data.payload)) return undefined
  return data as unknown as HostMessage<PrecheckContext>
}

function isPrecheckContext(value: unknown): value is PrecheckContext {
  if (!isRecord(value)) return false
  if (value.sourceSystem !== 'AIOPS' && value.sourceSystem !== 'SANDBOX') return false
  return (
    isText(value.hostRequestId, 1, 100) &&
    isText(value.formSchemaVersion, 1, 32) &&
    isText(value.title, 1, 200) &&
    isText(value.descriptionPlainText, 1, 10_000) &&
    (!('attachments' in value) ||
      (Array.isArray(value.attachments) && value.attachments.length <= 100))
  )
}

function safeSize(value: unknown) {
  try {
    return new TextEncoder().encode(JSON.stringify(value)).length
  } catch {
    return Number.POSITIVE_INFINITY
  }
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null
}

function isText(value: unknown, minimum: number, maximum: number): value is string {
  return typeof value === 'string' && value.trim().length >= minimum && value.length <= maximum
}
