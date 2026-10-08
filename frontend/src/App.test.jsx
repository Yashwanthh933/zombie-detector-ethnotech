import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import App from './App'
import { installFakeBackend, installFakeEventSource } from './test/fakeBackend'

const ACCOUNTS = {
    'admin@example.com': { email: 'admin@example.com', password: 'adminpass1', role: 'ADMIN', token: 'tok-admin' },
    'user@example.com': { email: 'user@example.com', password: 'userpass12', role: 'USER', token: 'tok-user' }
}

let backend
let eventSources

beforeEach(() => {
    backend = installFakeBackend({ accounts: ACCOUNTS })
    eventSources = installFakeEventSource()
})

afterEach(() => {
    vi.unstubAllGlobals()
})

async function signIn(email, password) {
    const user = userEvent.setup()
    render(<App />)
    await user.type(await screen.findByLabelText('Email'), email)
    await user.type(screen.getByLabelText('Password'), password)
    await user.click(screen.getByRole('button', { name: 'Sign in' }))
    return user
}

describe('authentication', () => {
    it('shows the login screen when signed out and never calls protected endpoints', async () => {
        render(<App />)
        expect(await screen.findByRole('heading', { name: 'Sign in' })).toBeInTheDocument()
        expect(backend.calls.filter(c => c.path !== '/auth/login')).toHaveLength(0)
    })

    it('rejects a wrong password with the server message', async () => {
        await signIn('user@example.com', 'wrong-password')
        expect(await screen.findByRole('alert')).toHaveTextContent('Invalid email or password.')
        expect(localStorage.getItem('zd_token')).toBeNull()
    })

    it('signs in, stores the token, and sends it on every API call', async () => {
        await signIn('user@example.com', 'userpass12')
        expect(await screen.findByText('user@example.com')).toBeInTheDocument()
        expect(localStorage.getItem('zd_token')).toBe('tok-user')

        await waitFor(() => expect(backend.calls.some(c => c.path === '/nodes')).toBe(true))
        const protectedCalls = backend.calls.filter(c => c.path !== '/auth/login')
        expect(protectedCalls.length).toBeGreaterThan(0)
        expect(protectedCalls.every(c => c.auth === 'Bearer tok-user')).toBe(true)
    })

    it('restores the session after a reload from the stored token', async () => {
        localStorage.setItem('zd_token', 'tok-admin')
        render(<App />)
        expect(await screen.findByText('admin@example.com')).toBeInTheDocument()
        expect(screen.getByText('ADMIN')).toBeInTheDocument()
    })

    it('drops a stale stored token and shows the login screen', async () => {
        localStorage.setItem('zd_token', 'tok-garbage')
        render(<App />)
        expect(await screen.findByRole('heading', { name: 'Sign in' })).toBeInTheDocument()
        expect(localStorage.getItem('zd_token')).toBeNull()
    })

    it('returns to login with a notice when the session expires mid-use', async () => {
        await signIn('user@example.com', 'userpass12')
        await screen.findByText('user@example.com')

        backend.expireAllTokens = true // token expires server-side; the next 5s poll gets a 401
        await userEvent.setup().click(screen.getByRole('button', { name: /Resources/ }))

        expect(await screen.findByText('Your session expired. Please sign in again.', {}, { timeout: 7000 })).toBeInTheDocument()
        expect(localStorage.getItem('zd_token')).toBeNull()
    }, 10000)

    it('signs out', async () => {
        const user = await signIn('user@example.com', 'userpass12')
        await user.click(await screen.findByRole('button', { name: 'Sign out' }))
        expect(await screen.findByRole('heading', { name: 'Sign in' })).toBeInTheDocument()
        expect(localStorage.getItem('zd_token')).toBeNull()
    })
})

describe('role-based UI', () => {
    it('regular users get a read-only policy page and no scheduler kill switch', async () => {
        const user = await signIn('user@example.com', 'userpass12')
        await screen.findByText('user@example.com')

        expect(await screen.findByText('Automation active')).toBeInTheDocument()
        expect(screen.queryByRole('button', { name: /Pause Scheduler/ })).not.toBeInTheDocument()

        await user.click(screen.getByRole('button', { name: /Policies/ }))
        expect(await screen.findByText(/can only be changed by an admin/)).toBeInTheDocument()
        expect(screen.queryByRole('button', { name: /Save Policy Settings/ })).not.toBeInTheDocument()
        screen.getAllByRole('spinbutton').forEach(input => expect(input).toBeDisabled())
    })

    it('admins see the scheduler control and can edit the policy', async () => {
        const user = await signIn('admin@example.com', 'adminpass1')
        expect(await screen.findByRole('button', { name: /Pause Scheduler/ })).toBeInTheDocument()

        await user.click(screen.getByRole('button', { name: /Policies/ }))
        const save = await screen.findByRole('button', { name: /Save Policy Settings/ })
        expect(save).toBeDisabled() // nothing changed yet

        const cpu = screen.getAllByRole('spinbutton')[0]
        await user.clear(cpu)
        await user.type(cpu, '25')
        await user.click(save)

        expect(await screen.findByText(/Policy Saved Successfully/)).toBeInTheDocument()
        expect(backend.policy.cpuThreshold).toBe(25)
        expect(backend.policy.updatedBy).toBe('admin@example.com')
    })

    it('shows the server validation message instead of a fake success', async () => {
        const user = await signIn('admin@example.com', 'adminpass1')
        await user.click(await screen.findByRole('button', { name: /Policies/ }))
        const cpu = (await screen.findAllByRole('spinbutton'))[0]
        await user.clear(cpu)
        await user.type(cpu, '150')
        await user.click(screen.getByRole('button', { name: /Save Policy Settings/ }))

        expect(await screen.findByRole('alert')).toHaveTextContent('cpuThreshold must be between 0 and 100.')
        expect(screen.queryByText(/Policy Saved Successfully/)).not.toBeInTheDocument()
    })
})

describe('policy-change notifications', () => {
    it('shows a toast and reloads the policy when the server pushes an update', async () => {
        const user = await signIn('user@example.com', 'userpass12')
        await screen.findByText('user@example.com')

        await waitFor(() => expect(eventSources).toHaveLength(1))
        expect(eventSources[0].url).toContain('/policy/stream?token=tok-user')

        await user.click(screen.getByRole('button', { name: /Policies/ }))
        await screen.findByText('Governance Policies')
        const policyGets = () => backend.calls.filter(c => c.method === 'GET' && c.path === '/policy').length
        const before = policyGets()

        backend.policy = { ...backend.policy, cpuThreshold: 40, updatedBy: 'admin@example.com' }
        eventSources[0].emit('policy-updated', { ...backend.policy, cpuThreshold: 40, updatedBy: 'admin@example.com' })

        const toast = await screen.findByRole('status')
        expect(within(toast).getByText('Detection policy updated')).toBeInTheDocument()
        expect(toast).toHaveTextContent('admin@example.com')
        await waitFor(() => expect(policyGets()).toBeGreaterThan(before))
    })

    it('closes the stream on sign out', async () => {
        const user = await signIn('user@example.com', 'userpass12')
        await waitFor(() => expect(eventSources).toHaveLength(1))
        await user.click(screen.getByRole('button', { name: 'Sign out' }))
        await waitFor(() => expect(eventSources[0].readyState).toBe(2))
    })
})

describe('resources', () => {
    it('adds a resource and surfaces server validation errors', async () => {
        const user = await signIn('user@example.com', 'userpass12')
        await user.click(await screen.findByRole('button', { name: /Resources/ }))

        await user.click(await screen.findByRole('button', { name: 'Add resource' }))
        expect(await screen.findByText(/Added user-abc12345/)).toBeInTheDocument()
        expect(backend.nodes).toHaveLength(1)

        const rate = screen.getByLabelText('Hourly rate (USD)')
        await user.clear(rate)
        await user.type(rate, '500')
        await user.click(screen.getByRole('button', { name: 'Add resource' }))
        expect(await screen.findByRole('alert')).toHaveTextContent('hourlyRate must be between 0 and 100.')
    })
})
