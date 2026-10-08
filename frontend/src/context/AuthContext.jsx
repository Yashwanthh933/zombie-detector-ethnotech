import { useCallback, useEffect, useMemo, useState } from 'react'
import { apiFetch, clearToken, getToken, readError, setToken } from '../api'
import { AuthContext } from './auth'

export function AuthProvider({ children }) {
    const [user, setUser] = useState(null)
    const [ready, setReady] = useState(false)
    const [notice, setNotice] = useState('')

    // On load: if a token is stored, ask the backend who it belongs to (also proves it's still valid).
    useEffect(() => {
        let cancelled = false
        async function restore() {
            if (!getToken()) { setReady(true); return }
            try {
                const res = await apiFetch('/auth/me', { skipAuthRedirect: true })
                if (cancelled) return
                if (res.ok) {
                    const me = await res.json()
                    setUser({ email: me.email, role: me.role })
                } else {
                    clearToken()
                }
            } catch {
                // Backend unreachable: keep the token so a refresh can succeed later, but show login.
            }
            if (!cancelled) setReady(true)
        }
        restore()
        return () => { cancelled = true }
    }, [])

    // Any 401 anywhere in the app lands here (see apiFetch).
    useEffect(() => {
        const onExpired = () => {
            setUser(null)
            setNotice('Your session expired. Please sign in again.')
        }
        window.addEventListener('auth:expired', onExpired)
        return () => window.removeEventListener('auth:expired', onExpired)
    }, [])

    const authenticate = useCallback(async (path, email, password) => {
        try {
            const res = await apiFetch(path, {
                method: 'POST',
                body: JSON.stringify({ email, password }),
                skipAuthRedirect: true
            })
            if (!res.ok) return { ok: false, error: await readError(res, 'Request failed.') }
            const data = await res.json()
            setToken(data.token)
            setNotice('')
            setUser({ email: data.email, role: data.role })
            return { ok: true }
        } catch {
            return { ok: false, error: 'Cannot reach the server. Check your connection and try again.' }
        }
    }, [])

    const login = useCallback((email, password) => authenticate('/auth/login', email, password), [authenticate])
    const register = useCallback((email, password) => authenticate('/auth/register', email, password), [authenticate])

    const logout = useCallback(() => {
        clearToken()
        setUser(null)
        setNotice('')
    }, [])

    const value = useMemo(
        () => ({ user, ready, notice, login, register, logout }),
        [user, ready, notice, login, register, logout]
    )

    return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
