import { useEffect, useRef } from 'react'
import { API, apiFetch, getToken } from '../api'

/**
 * Subscribes to the backend's policy-change stream (Server-Sent Events) and calls
 * onUpdate(policy) whenever an admin saves new policy settings.
 *
 * EventSource can't send an Authorization header, so the JWT goes in ?token=.
 * The browser auto-reconnects on network blips; if the connection is closed for good
 * (e.g. the token expired -> 401) we verify the session and either log out (apiFetch
 * handles that) or reconnect.
 */
export default function usePolicyStream(enabled, onUpdate) {
    const callback = useRef(onUpdate)
    useEffect(() => { callback.current = onUpdate })

    useEffect(() => {
        if (!enabled) return undefined
        let source = null
        let retryTimer = null
        let stopped = false

        const connect = () => {
            const token = getToken()
            if (!token || stopped) return
            source = new EventSource(`${API}/policy/stream?token=${encodeURIComponent(token)}`)

            source.addEventListener('policy-updated', (event) => {
                try { callback.current(JSON.parse(event.data)) } catch { /* ignore malformed event */ }
            })

            source.onerror = async () => {
                if (source.readyState !== EventSource.CLOSED || stopped) return
                source.close()
                try {
                    const res = await apiFetch('/auth/me')
                    if (res.ok && !stopped) retryTimer = setTimeout(connect, 5000)
                } catch {
                    if (!stopped) retryTimer = setTimeout(connect, 10000)
                }
            }
        }

        connect()
        return () => {
            stopped = true
            clearTimeout(retryTimer)
            if (source) source.close()
        }
    }, [enabled])
}
