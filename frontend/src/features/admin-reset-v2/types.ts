export interface AdminResetError {
  code: string
  message: string
  retryable: boolean
}

export interface AdminReset {
  taskId: string
  resourceId: string
  status: 'PENDING' | 'RUNNING' | 'SUCCEEDED' | 'FAILED'
  attempt: number
  maxAttempts: 3
  error: AdminResetError | null
  nextRetryAt: string | null
  createdAt: string
  startedAt: string | null
  completedAt: string | null
  mockData: true
  environmentCode: 'LOCAL'
  confirmedBy: string
  auditEventId: string
}
