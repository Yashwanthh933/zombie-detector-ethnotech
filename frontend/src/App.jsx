import { useState, useEffect, useCallback } from 'react'
import './index.css'

const API = 'http://localhost:8080/api'

function StatCard({ label, value, tone }) {
  return (
    <div className={`stat-card ${tone || ''}`}>
      <p className="stat-label">{label}</p>
      <p className="stat-value">{value}</p>
    </div>
  )
}

function StatusBadge({ status }) {
  const toneMap = { RUNNING: 'success', FLAGGED: 'warning', STOPPED: 'danger' }
  return <span className={`badge ${toneMap[status] || ''}`}>{status.toLowerCase()}</span>
}

function Dashboard() {
  const [nodes, setNodes] = useState([])
  const [savings, setSavings] = useState({ stoppedCount: 0, monthlySavings: 0 })
  const [schedulerPaused, setSchedulerPaused] = useState(false)

  const refresh = useCallback(async () => {
    const [nodesRes, savingsRes, schedRes] = await Promise.all([
      fetch(`${API}/nodes`), fetch(`${API}/savings`), fetch(`${API}/scheduler/status`)
    ])
    setNodes(await nodesRes.json())
    setSavings(await savingsRes.json())
    setSchedulerPaused((await schedRes.json()).paused)
  }, [])

  useEffect(() => {
    refresh()
    const id = setInterval(refresh, 5000)
    return () => clearInterval(id)
  }, [refresh])

  const toggleScheduler = async () => {
    await fetch(`${API}/scheduler/${schedulerPaused ? 'resume' : 'pause'}`, { method: 'POST' })
    refresh()
  }

  const override = async (nodeId) => {
    await fetch(`${API}/nodes/${nodeId}/override`, { method: 'POST' })
    refresh()
  }

  const running = nodes.filter(n => n.status === 'RUNNING').length
  const flagged = nodes.filter(n => n.status === 'FLAGGED').length

  return (
    <div>
      <div className="stat-grid">
        <StatCard label="Monthly savings" value={`$${savings.monthlySavings.toFixed(2)}`} tone="success" />
        <StatCard label="Running" value={running} />
        <StatCard label="Needs review" value={flagged} tone="warning" />
        <StatCard label="Stopped" value={savings.stoppedCount} tone="danger" />
      </div>

      <div className="panel-header">
        <p className="panel-title">Nodes</p>
        <button onClick={toggleScheduler}>
          {schedulerPaused ? 'Resume scheduler' : 'Pause scheduler'}
        </button>
      </div>

      <div className="card">
        {nodes.map(n => (
          <div key={n.nodeId} className="row">
            <div className="row-main">
              <span className="node-id">{n.nodeId}</span>
              {n.environment === 'PROD' && <span className="tag">prod</span>}
              <span className="owner">{n.ownerEmail}</span>
            </div>
            <div className="row-side">
              <span className="cpu">{n.avgCpuLoadLast15Min?.toFixed(1)}% cpu</span>
              <StatusBadge status={n.status} />
              {n.status === 'FLAGGED' && (
                <button className="small" onClick={() => override(n.nodeId)}>Override</button>
              )}
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}

function Alerts() {
  const [alerts, setAlerts] = useState([])

  const refresh = useCallback(async () => {
    setAlerts(await (await fetch(`${API}/alerts`)).json())
  }, [])

  useEffect(() => {
    refresh()
    const id = setInterval(refresh, 5000)
    return () => clearInterval(id)
  }, [refresh])

  const ack = async (id) => {
    await fetch(`${API}/alerts/${id}/ack`, { method: 'POST' })
    refresh()
  }

  const active = alerts.filter(a => !a.acknowledged)
  const acknowledged = alerts.filter(a => a.acknowledged)

  return (
    <div>
      <p className="panel-title">Active</p>
      <div className="card">
        {active.length === 0 && <p className="empty">No active alerts</p>}
        {active.map(a => (
          <div key={a.id} className={`alert alert-${a.severity.toLowerCase()}`}>
            <div>
              <p className="alert-title">{a.title}</p>
              <p className="alert-message">{a.message}</p>
            </div>
            <button className="small" onClick={() => ack(a.id)}>Acknowledge</button>
          </div>
        ))}
      </div>

      {acknowledged.length > 0 && (
        <>
          <p className="panel-title" style={{ marginTop: '1.5rem' }}>Acknowledged</p>
          <div className="card">
            {acknowledged.map(a => (
              <div key={a.id} className="alert dim">
                <div>
                  <p className="alert-title">{a.title}</p>
                  <p className="alert-message">{a.message}</p>
                </div>
              </div>
            ))}
          </div>
        </>
      )}
    </div>
  )
}

function AuditTrail() {
  const [events, setEvents] = useState([])

  useEffect(() => {
    fetch(`${API}/audit`).then(r => r.json()).then(setEvents)
    const id = setInterval(() => {
      fetch(`${API}/audit`).then(r => r.json()).then(setEvents)
    }, 5000)
    return () => clearInterval(id)
  }, [])

  return (
    <div className="card">
      {events.map(e => (
        <div key={e.id} className="row">
          <div className="row-main">
            <span className="timestamp">{e.timestamp?.replace('T', ' ').slice(0, 19)}</span>
            <span className="node-id">{e.action}</span>
            <span className="owner">{e.targetNodeId}</span>
          </div>
          <span className="details">{e.details}</span>
        </div>
      ))}
    </div>
  )
}

function Policies() {
  const [policy, setPolicy] = useState(null)
  const [saved, setSaved] = useState(false)

  useEffect(() => {
    fetch(`${API}/policy`).then(r => r.json()).then(setPolicy)
  }, [])

  if (!policy) return <p className="empty">Loading...</p>

  const update = (field, value) => setPolicy({ ...policy, [field]: value })

  const save = async () => {
    await fetch(`${API}/policy`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(policy)
    })
    setSaved(true)
    setTimeout(() => setSaved(false), 2000)
  }

  return (
    <div className="card" style={{ padding: '1.25rem' }}>
      <label className="field">
        <span>CPU threshold (%)</span>
        <input type="number" value={policy.cpuThreshold}
          onChange={e => update('cpuThreshold', parseFloat(e.target.value))} />
      </label>
      <label className="field">
        <span>Idle window (minutes)</span>
        <input type="number" value={policy.idleWindowMinutes}
          onChange={e => update('idleWindowMinutes', parseInt(e.target.value))} />
      </label>
      <label className="field">
        <span>Minimum samples required</span>
        <input type="number" value={policy.minSamplesRequired}
          onChange={e => update('minSamplesRequired', parseInt(e.target.value))} />
      </label>
      <label className="field">
        <span>Recovery strikes required</span>
        <input type="number" value={policy.recoveryStrikesRequired}
          onChange={e => update('recoveryStrikesRequired', parseInt(e.target.value))} />
      </label>
      <label className="field">
        <span>Grace period (seconds)</span>
        <input type="number" value={policy.gracePeriodSeconds}
          onChange={e => update('gracePeriodSeconds', parseInt(e.target.value))} />
      </label>
      <button onClick={save}>{saved ? 'Saved' : 'Save policy'}</button>
    </div>
  )
}

export default function App() {
  const [tab, setTab] = useState('dashboard')
  const tabs = [
    ['dashboard', 'Dashboard'], ['alerts', 'Alerts'],
    ['audit', 'Audit trail'], ['policies', 'Policies']
  ]

  return (
    <div className="app">
      <header className="header">
        <span className="brand">Zombie detector</span>
        <nav className="tabs">
          {tabs.map(([key, label]) => (
            <button key={key} className={tab === key ? 'active' : ''} onClick={() => setTab(key)}>
              {label}
            </button>
          ))}
        </nav>
      </header>
      <main>
        {tab === 'dashboard' && <Dashboard />}
        {tab === 'alerts' && <Alerts />}
        {tab === 'audit' && <AuditTrail />}
        {tab === 'policies' && <Policies />}
      </main>
    </div>
  )
}