import {
  AUTO_LOGIN_TEST_CONSTANTS,
  clearAutoLoginCredentials,
  hasStoredAutoLoginCredentials,
  loadRenewableAutoLoginCredentials,
  resolveTokenExpiresAt,
  saveAutoLoginCredentials,
  shouldRenewTokenSoon,
  touchAutoLoginActivity,
} from '@/utils/autoLogin'

const createFakeJwt = (expSeconds: number) => {
  const header = { alg: 'HS256', typ: 'JWT' }
  const payload = { exp: expSeconds }
  const encode = (value: Record<string, unknown>) => {
    return btoa(JSON.stringify(value)).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/g, '')
  }

  return `${encode(header)}.${encode(payload)}.signature`
}

describe('autoLogin', () => {
  beforeEach(() => {
    window.localStorage.clear()
  })

  it('can save and restore renewable credentials', async () => {
    const now = Date.now()
    const token = createFakeJwt(Math.floor((now + 60_000) / 1000))

    await saveAutoLoginCredentials({
      email: 'demo@example.com',
      password: 'password123',
      token,
    })

    const credentials = await loadRenewableAutoLoginCredentials(now + 1_000)

    expect(credentials).toEqual({
      email: 'demo@example.com',
      password: 'password123',
    })
  })

  it('persists the selected workspace for silent reauthentication', async () => {
    const now = Date.now()
    const token = createFakeJwt(Math.floor((now + 60_000) / 1000))

    await saveAutoLoginCredentials({
      email: 'cleaner@example.com',
      password: 'password123',
      token,
      preferredLoginTarget: 'CLEANER',
    })

    const credentials = await loadRenewableAutoLoginCredentials(now + 1_000)

    expect(credentials).toEqual({
      email: 'cleaner@example.com',
      password: 'password123',
      preferredLoginTarget: 'CLEANER',
    })
  })

  it('clears credentials after auto login window expires', async () => {
    const now = Date.now()
    const token = createFakeJwt(Math.floor((now + 60_000) / 1000))

    await saveAutoLoginCredentials({
      email: 'demo@example.com',
      password: 'password123',
      token,
    })

    const credentials = await loadRenewableAutoLoginCredentials(
      now + AUTO_LOGIN_TEST_CONSTANTS.AUTO_LOGIN_MAX_IDLE_MS + 1,
    )

    expect(credentials).toBeNull()
    expect(hasStoredAutoLoginCredentials()).toBe(false)
  })

  it('parses token expiration and renew window correctly', async () => {
    const now = Date.now()
    const expiringSoonToken = createFakeJwt(
      Math.floor((now + AUTO_LOGIN_TEST_CONSTANTS.AUTO_LOGIN_RENEW_WINDOW_MS - 1_000) / 1000),
    )
    const longLivedToken = createFakeJwt(
      Math.floor((now + AUTO_LOGIN_TEST_CONSTANTS.AUTO_LOGIN_RENEW_WINDOW_MS + 60_000) / 1000),
    )

    expect(resolveTokenExpiresAt(expiringSoonToken)).not.toBeNull()
    expect(shouldRenewTokenSoon(expiringSoonToken, now)).toBe(true)
    expect(shouldRenewTokenSoon(longLivedToken, now)).toBe(false)
  })

  it('keeps stored credentials after activity touch inside window', async () => {
    const now = Date.now()
    const token = createFakeJwt(Math.floor((now + 60_000) / 1000))

    await saveAutoLoginCredentials({
      email: 'demo@example.com',
      password: 'password123',
      token,
    })

    await touchAutoLoginActivity(now + 2 * 60_000)

    const credentials = await loadRenewableAutoLoginCredentials(
      now + AUTO_LOGIN_TEST_CONSTANTS.AUTO_LOGIN_MAX_IDLE_MS - 30_000,
    )

    expect(credentials).toEqual({
      email: 'demo@example.com',
      password: 'password123',
    })
  })

  afterEach(() => {
    clearAutoLoginCredentials()
  })
})
