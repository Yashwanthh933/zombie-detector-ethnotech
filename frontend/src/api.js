// Single place that knows where the backend lives and how to talk to it.
// Set VITE_API_URL in Vercel (e.g. https://your-service.onrender.com/api); falls back to local dev.
export const API = (import.meta.env.VITE_API_URL || 'http://localhost:8080/api').replace(/\/+$/, '')

const TOKEN_KEY = 'zd_token'

export function getToken() {
    try { return localStorage.getItem(TOKEN_KEY) } catch { return null }
}

export function setToken(token) {
    try { localStorage.setItem(TOKEN_KEY, token) } catch { /* storage unavailable: session lasts until reload */ }
}

export function clearToken() {
    try { localStorage.removeItem(TOKEN_KEY) } catch { /* ignore */ }
}

/**
 * fetch() wrapper: attaches the JWT, sets JSON content-type for bodies, and on a 401
 * clears the stored token and broadcasts "auth:expired" so the app returns to the login
 * screen. Pass { skipAuthRedirect: true } for calls where 401 is an expected answer
 * (e.g. a wrong password on the login form).
 */
export async function apiFetch(path, options = {}) {
    const { skipAuthRedirect, ...init } = options
    const headers = { ...(init.headers || {}) }
    const token = getToken()
    if (token) headers.Authorization = `Bearer ${token}`
    if (init.body && !headers['Content-Type']) headers['Content-Type'] = 'application/json'

    const res = await fetch(`${API}${path}`, { ...init, headers })

    if (res.status === 401 && !skipAuthRedirect) {
        clearToken()
        window.dispatchEvent(new Event('auth:expired'))
    }
    return res
}

/** Pulls a human-readable message out of an error response (plain text or JSON). */
export async function readError(res, fallback = 'Something went wrong.') {
    try {
        const text = (await res.text()).trim()
        if (!text) return fallback
        if (text.startsWith('{')) {
            const data = JSON.parse(text)
            return data.message || data.error || fallback
        }
        return text
    } catch {
        return fallback
    }
}
