export interface PrecheckRequest {
  title: string
  description: string
  product?: string
  module?: string
  version?: string
  severity?: string
  impactScope?: string
  attachmentsSummary?: string
}

export interface ReferenceItem {
  sourceType: 'PRODUCT_MANUAL' | 'HISTORICAL_SLA'
  title: string
  excerpt: string
  url: string
  mockData: boolean
}

export interface PrecheckResponse {
  precheckId: string
  sessionId: string
  summary: string
  recommendations: string[]
  references: ReferenceItem[]
  confidence: 'HIGH' | 'MEDIUM' | 'LOW'
  confidenceReason: string
  humanReviewRequired: boolean
  missingInformation: string[]
  fallbackReason: string
  nextAction: NextAction
  nextActionReason: string
  allowedActions: AllowedAction[]
  status: PrecheckStatus
  policyVersion: string
  modelVersion: string
  promptVersion: string
  indexVersion: string
}

export interface FollowUpRequest {
  precheckId: string
  message: string
}

export interface FollowUpResponse {
  followUpId: string
  precheckId: string
  reply: string
  recommendations: string[]
  references: ReferenceItem[]
  confidence: 'HIGH' | 'MEDIUM' | 'LOW'
  confidenceReason: string
  humanReviewRequired: boolean
  missingInformation: string[]
  fallbackReason: string
  nextAction: NextAction
  nextActionReason: string
  allowedActions: AllowedAction[]
  status: PrecheckStatus
  policyVersion: string
  modelVersion: string
  promptVersion: string
  indexVersion: string
}

export type NextAction =
  | 'SELF_SERVICE_SUGGESTED'
  | 'NEED_MORE_INFORMATION'
  | 'SUBMISSION_RECOMMENDED'
  | 'MANUAL_REVIEW_REQUIRED'

export type AllowedAction = 'SUPPLEMENT_INFORMATION' | 'CONTINUE_SUBMISSION'

export type PrecheckStatus =
  | 'RECEIVED'
  | 'VALIDATED'
  | 'COMPLETED'
  | 'NEED_MORE_INFORMATION'
  | 'DEGRADED'
  | 'FAILED'
  | 'CANCELLED'

export interface ConversationTurn {
  message: string
  response: FollowUpResponse
}

export type AdoptionStatus = 'ADOPTED' | 'PARTIALLY_ADOPTED' | 'IGNORED'

export interface FeedbackRequest {
  precheckId: string
  adoptionStatus: AdoptionStatus
  feedbackReason?: string
  continuedSubmission: boolean
}

export interface FeedbackResponse {
  feedbackId: string
  precheckId: string
  adoptionStatus: AdoptionStatus
  continuedSubmission: boolean
  policyVersion: string
  recorded: boolean
  mockData: boolean
}

export interface ApiError {
  code: string
  message: string
  fieldErrors: Record<string, string>
  timestamp: string
  traceId: string
}
