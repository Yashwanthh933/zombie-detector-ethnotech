import { vi } from 'vitest'

/**
 * A tiny in-memory stand-in for the Spring API, enforcing the same auth rules the real one
 * does (401 without a valid token, 403 for non-admins on admin routes).
 */
export function installFakeBackend({ accounts }) {
    const state = {
        policy: { cpuThreshold: 15, idleWindowMinutes: 2, minSamplesRequired: 5, recoveryStrikesRequired: 3, gracePeriodSeconds: 20 },
        nodes: [],
        calls: [],
        paused: false,
        expireAllTokens: false
    }

    const json = (status, body) => new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } })
    const text = (status, body) => new Response(body, { status })

    const fetchMock = vi.fn(async (url, init = {}) => {
        const path = String(url).replace(/^https?:\/\/[^/]+\/api/, '')
        const method = init.method || 'GET'
        const auth = (init.headers || {}).Authorization || ''
        const token = auth.replace('Bearer ', '')
        const account = state.expireAllTokens ? null : Object.values(accounts).find(a => a.token === token)
        state.calls.push({ method, path, auth })

        if (path === '/auth/login') {
            const body = JSON.parse(init.body)
            const a = accounts[body.email]
            if (!a || a.password !== body.password) return text(401, 'Invalid email or password.')
            return json(200, { token: a.token, email: body.email, role: a.role })
        }
        if (!account) return text(401, 'Authentication required.')

        if (path === '/auth/me') return json(200, { email: account.email, role: account.role })
        if (path === '/nodes' && method === 'GET') return json(200, state.nodes)
        if (path === '/nodes' && method === 'POST') {
            const body = JSON.parse(init.body)
            if (body.hourlyRate > 100) return text(400, 'hourlyRate must be between 0 and 100.')
            const node = { nodeId: 'user-abc12345', ...body, status: 'RUNNING', ownerEmail: account.email, avgCpuLoadLast15Min: 0 }
            state.nodes.push(node)
            return json(200, node)
        }
        if (path === '/savings') return json(200, { stoppedCount: 0, monthlySavings: 0 })
        if (path === '/savings/history') return json(200, [])
        if (path === '/scheduler/status') return json(200, { paused: state.paused })
        if (path === '/alerts' || path === '/audit') return json(200, [])
        if (path === '/policy' && method === 'GET') return json(200, state.policy)
        if (path === '/policy' && method === 'PUT') {
            if (account.role !== 'ADMIN') return text(403, '')
            const body = JSON.parse(init.body)
            if (body.cpuThreshold > 100) return text(400, 'cpuThreshold must be between 0 and 100.')
            state.policy = { ...body, updatedBy: account.email }
            return json(200, state.policy)
        }
        return text(404, 'not found')
    })

    vi.stubGlobal('fetch', fetchMock)
    return state
}

/** Records every EventSource the app opens so tests can push server events into it. */
export function installFakeEventSource() {
    const sources = []
    class FakeEventSource {
        static CLOSED = 2
        constructor(url) {
            this.url = url
            this.readyState = 1
            this.listeners = {}
            sources.push(this)
        }
        addEventListener(name, fn) { this.listeners[name] = fn }
        emit(name, data) { this.listeners[name]?.({ data: JSON.stringify(data) }) }
        close() { this.readyState = 2 }
    }
    vi.stubGlobal('EventSource', FakeEventSource)
    return sources
}
