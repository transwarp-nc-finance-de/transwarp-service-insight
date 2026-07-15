import { AuthApiError, type AuthResult, type AuthSession } from './types'

export async function createAuthSession(userCode: string): Promise<AuthResult> {
  const response = await fetch('/api/v2/auth-sessions', {
    method: 'POST',
    credentials: 'include',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ userCode, mockData: true }),
  })
  return readSession(response)
}

export async function getCurrentAuthSession(): Promise<AuthResult> {
  const response = await fetch('/api/v2/auth-sessions/current', { credentials: 'include' })
  return readSession(response)
}

export async function deleteCurrentAuthSession(csrfToken: string): Promise<void> {
  const response = await fetch('/api/v2/auth-sessions/current', {
    method: 'DELETE',
    credentials: 'include',
    headers: { 'X-CSRF-Token': csrfToken },
  })
  await ensureOk(response)
}

async function readSession(response: Response): Promise<AuthResult> {
  await ensureOk(response)
  const csrfToken = response.headers.get('X-CSRF-Token')
  if (!csrfToken) throw new Error('本地会话响应缺少 CSRF Token')
  return { session: (await response.json()) as AuthSession, csrfToken }
}

async function ensureOk(response: Response): Promise<void> {
  if (response.ok) return
  let message = '本地模拟身份服务暂时不可用'
  try {
    const body = (await response.json()) as { message?: string }
    if (body.message) message = body.message
  } catch {
    // 非 JSON 响应只显示本地安全提示。
  }
  throw new AuthApiError(response.status, message)
}
