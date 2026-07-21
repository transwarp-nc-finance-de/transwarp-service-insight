export interface EvaluationSummary {
  sampleCount: number
  gatePassed: boolean
  permissionLeakageRate: number
  citationErrorRate: number
  degradationPassRate: number
  recallAt5: number
  disclaimer: string
}

export interface EvaluationRun {
  taskId: string
  status: 'PENDING' | 'RUNNING' | 'SUCCEEDED' | 'FAILED'
  createdAt: string
  evaluationSetVersion: string
  summary: EvaluationSummary | null
  error: { code: string; message: string; retryable: boolean } | null
  mockData: true
}

export interface EvaluationFailure {
  caseId: string
  scenarioTags: string[]
  failedChecks: string[]
  failureCodes: string[]
  expected: Record<string, unknown>
  actual: Record<string, unknown>
  mockData: true
}

export interface Metrics {
  from: string
  to: string
  precheckCount: number
  successRate: number
  degradationRate: number
  averageRunCount: number
  informationSupplementRate: number
  evidenceHitRate: number
  adoptionRate: number
  continuationRate: number
  retrievalP95Ms: number
  embeddingP95Ms: number
  mockData: true
}
