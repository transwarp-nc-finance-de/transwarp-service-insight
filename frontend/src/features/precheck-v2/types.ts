export interface CatalogValue {
  code: string
  displayName: string
}

export interface PrecheckContext {
  sourceSystem: 'SANDBOX'
  hostRequestId: string
  formSchemaVersion: string
  issueType: CatalogValue
  productLine: CatalogValue
  product: CatalogValue
  component: CatalogValue
  version: string
  severity: CatalogValue
  serviceType: CatalogValue
  title: string
  descriptionPlainText: string
  additionalInformation: Array<{ fieldCode: string; displayName: string; value: string }>
  impactScope: string
  attachments: Array<never>
}

export interface PrecheckRun {
  runId: string
  sessionId: string
  sequence: number
  status: string
  contextSnapshot: PrecheckContext
  result: {
    summary: string
    recommendations: string[]
    completeness: {
      complete: boolean
      policyVersion: string
      missingFieldCodes: string[]
      reasons: string[]
    }
    confidence: string
    confidenceReasons: string[]
    humanInterventionAdvice: string[]
    missingInformation: string[]
    allowedActions: string[]
    disclaimer: string
    evidence: EvidenceReference[]
    retrieval: {
      mode: 'HYBRID' | 'FTS_ONLY' | 'UNAVAILABLE'
      degraded: boolean
      fts: RetrievalCapability
      embedding: RetrievalCapability
    }
    mockData: boolean
  }
}

export interface RetrievalCapability {
  available: boolean
  code: string
  message: string
}

export interface EvidenceReference {
  evidenceId: string
  title: string
  excerpt: string
  mockData: boolean
}

export interface Evidence {
  evidenceId: string
  document: { documentId: string; title: string }
  versionId: string
  chunkId: string
  excerpt: string
  contentHash: string
  mockData: boolean
}

export interface PrecheckSession {
  sessionId: string
  ownerUserCode: string
  status: string
  terminationReason?: string
  latestRun: PrecheckRun
  runCount: number
  maxRuns: number
  mockData: boolean
}

export interface SessionPage {
  items: PrecheckSession[]
}

export interface RunPage {
  items: PrecheckRun[]
}
