import type { PrecheckRequest } from '../../features/precheck/types'

export const PROTOCOL_VERSION = '1.0'
export const HOST_MESSAGE_TYPES = [
  'TSI_INIT',
  'TSI_READY',
  'TSI_PRECHECK_START',
  'TSI_PRECHECK_UPDATE',
  'TSI_PRECHECK_RESULT',
  'TSI_REQUEST_SUPPLEMENT',
  'TSI_ADOPT_CHANGES',
  'TSI_CONTINUE_SUBMISSION',
  'TSI_REFERENCE_OPENED',
  'TSI_CLOSE',
  'TSI_ERROR',
] as const
export type HostMessageType = (typeof HOST_MESSAGE_TYPES)[number]

export interface CodeNameValue {
  code?: string
  name?: string
}
export interface AttachmentReference {
  attachmentId?: string
  fileName: string
  mimeType?: string
  size?: number
  contentAvailable: boolean
}
export interface PrecheckContext {
  sourceSystem: 'AIOPS' | 'SANDBOX'
  hostRequestId: string
  formSchemaVersion: string
  issueType?: CodeNameValue
  productLine?: CodeNameValue
  product?: CodeNameValue
  component?: CodeNameValue
  version?: CodeNameValue
  issueLevel?: CodeNameValue
  serviceType?: CodeNameValue
  title: string
  descriptionPlainText: string
  additionalInformation?: string
  impactScope?: string
  attachments?: AttachmentReference[]
}
export interface HostMessage<T = unknown> {
  type: HostMessageType
  version: string
  requestId: string
  timestamp: string
  payload: T
}
export function toPrecheckRequest(context: PrecheckContext): PrecheckRequest {
  return {
    title: context.title,
    description: context.descriptionPlainText,
    product: context.product?.name,
    module: context.component?.name,
    version: context.version?.name,
    severity: context.issueLevel?.name,
    impactScope: context.impactScope,
    attachmentsSummary: context.attachments?.map((item) => item.fileName).join('、'),
    context: {
      sourceSystem: context.sourceSystem,
      hostRequestId: context.hostRequestId,
      formSchemaVersion: context.formSchemaVersion,
    },
  }
}
