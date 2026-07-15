export type Role = 'PRECHECK_USER' | 'KNOWLEDGE_EDITOR' | 'KNOWLEDGE_REVIEWER' | 'ADMIN'

export interface AuthSession {
  sessionId: string
  userCode: string
  displayName: string
  roles: Role[]
  productLineCodes: string[]
  expiresAt: string
  mockData: true
}

export interface AuthResult {
  session: AuthSession
  csrfToken: string
}

export class AuthApiError extends Error {
  constructor(
    public readonly status: number,
    message: string,
  ) {
    super(message)
  }
}
