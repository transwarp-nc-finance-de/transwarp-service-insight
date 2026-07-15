import { onMounted, ref } from 'vue'
import { createAuthSession, deleteCurrentAuthSession, getCurrentAuthSession } from './api'
import { AuthApiError, type AuthSession } from './types'

let sharedCsrfToken = ''

export function readCsrfToken() {
  return sharedCsrfToken
}

export function useAuthSession() {
  const session = ref<AuthSession>()
  const csrfToken = ref('')
  const loading = ref(false)
  const error = ref('')

  async function restore() {
    try {
      accept(await getCurrentAuthSession())
    } catch (cause) {
      if (!(cause instanceof AuthApiError && cause.status === 401)) setError(cause)
    }
  }

  async function login(userCode: string) {
    loading.value = true
    error.value = ''
    try {
      accept(await createAuthSession(userCode))
    } catch (cause) {
      setError(cause)
    } finally {
      loading.value = false
    }
  }

  async function logout() {
    if (!session.value || !csrfToken.value) return
    loading.value = true
    error.value = ''
    try {
      await deleteCurrentAuthSession(csrfToken.value)
      clear()
    } catch (cause) {
      setError(cause)
    } finally {
      loading.value = false
    }
  }

  function accept(result: { session: AuthSession; csrfToken: string }) {
    session.value = result.session
    csrfToken.value = result.csrfToken
    sharedCsrfToken = result.csrfToken
  }

  function clear() {
    session.value = undefined
    csrfToken.value = ''
    sharedCsrfToken = ''
  }

  function setError(cause: unknown) {
    error.value = cause instanceof Error ? cause.message : '本地模拟身份操作失败'
  }

  onMounted(restore)
  return { session, loading, error, login, logout }
}
