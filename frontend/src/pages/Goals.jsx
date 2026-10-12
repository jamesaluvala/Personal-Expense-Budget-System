import React, { useState } from 'react';
import { api } from '../lib/api.js';
import { useResource } from '../lib/hooks.js';
import { dateLabel, errorText, money, normalizeAmount, percent } from '../lib/format.js';
import { Alert, Badge, Empty, Field, Modal, Panel, Progress, ResourceState } from '../components/UI.jsx';
function GoalForm({ goal, meta, onClose, changed }) {
  const [form, setForm] = useState({ title: goal?.title || '', targetAmount: goal?.targetAmount || '', targetDate: goal?.targetDate || meta.today });
  const [busy, setBusy] = useState(false); const [error, setError] = useState('');
  function update(field, value) { setForm(old => ({ ...old, [field]: value })); }
  async function save(event) {
    event.preventDefault(); setBusy(true); setError('');
    try { await api(goal ? `/goals/${goal.id}` : '/goals', { method: goal ? 'PUT' : 'POST', data: { ...form, title: form.title.trim(), targetAmount: normalizeAmount(form.targetAmount) } }); changed('Savings goal saved.'); onClose(); }
    catch (err) { setError(errorText(err)); } finally { setBusy(false); }
  }
  return <Modal title={goal ? 'Edit savings goal' : 'Create a savings goal'} onClose={onClose} busy={busy}><form onSubmit={save}><Alert>{error}</Alert><Field label="Goal name"><input required maxLength="80" value={form.title} onChange={event => update('title', event.target.value)} disabled={busy} placeholder="What are you saving for?" /></Field><Field label={`Target amount (${meta.currency})`}><input required inputMode="decimal" maxLength="15" value={form.targetAmount} onChange={event => update('targetAmount', event.target.value)} disabled={busy} /></Field><Field label="Target date"><input required type="date" min={goal ? '2000-01-01' : meta.today} max="2100-12-31" value={form.targetDate} onChange={event => update('targetDate', event.target.value)} disabled={busy} /></Field><div className="form-actions"><button type="button" className="btn secondary" onClick={onClose} disabled={busy}>Cancel</button><button className="btn" disabled={busy}>{busy ? 'Saving...' : 'Save goal'}</button></div></form></Modal>;
}
function ContributionForm({ goal, meta, onClose, changed }) {
  const [revision, setRevision] = useState(0); const [amount, setAmount] = useState(''); const [date, setDate] = useState(meta.today);
  const [busy, setBusy] = useState(false); const [error, setError] = useState('');
  const resource = useResource(`/goals/${goal.id}/contributions`, revision);
  async function save(event) {
    event.preventDefault(); setBusy(true); setError('');
    try { await api(`/goals/${goal.id}/contributions`, { method: 'POST', data: { amount: normalizeAmount(amount), date } }); setAmount(''); setRevision(value => value + 1); changed('Savings reserved for your goal.'); }
    catch (err) { setError(errorText(err)); } finally { setBusy(false); }
  }
  async function release(entry) {
    if (!window.confirm(`Release ${money(entry.amount, meta.currency)} from this goal? Income and expense transactions stay unchanged.`)) return;
    setBusy(true); setError('');
    try { await api(`/goals/${goal.id}/contributions/${entry.id}`, { method: 'DELETE' }); setRevision(value => value + 1); changed('Contribution released.'); }
    catch (err) { setError(errorText(err)); } finally { setBusy(false); }
  }
  return <Modal title={goal.title} onClose={onClose} busy={busy}><p className="muted">Reserve part of your recorded balance. This does not transfer actual money or count as an expense.</p><form onSubmit={save}><Alert>{error}</Alert><div className="form-grid"><Field label={`Amount to reserve (${meta.currency})`}><input required inputMode="decimal" maxLength="15" value={amount} onChange={event => setAmount(event.target.value)} disabled={busy} /></Field><Field label="Date"><input required type="date" min="2000-01-01" max={meta.today} value={date} onChange={event => setDate(event.target.value)} disabled={busy} /></Field></div><button className="btn full" disabled={busy}>{busy ? 'Saving...' : 'Add savings contribution'}</button></form><h3 className="section-title">Contribution history</h3>{!resource.data || resource.error ? <ResourceState resource={resource} /> : resource.data.length ? <div className="contribution-list">{resource.data.map(entry => <div key={entry.id}><span>{dateLabel(entry.date)}</span><strong>{money(entry.amount, meta.currency)}</strong><button className="text-btn danger-text" onClick={() => release(entry)} disabled={busy}>Release</button></div>)}</div> : <p className="muted">No contributions yet.</p>}</Modal>;
}
export default function Goals({ revision, meta, changed }) {
  const resource = useResource('/goals', revision); const [dialog, setDialog] = useState(null);
  const [error, setError] = useState(''); const [busy, setBusy] = useState(false);
  async function remove(goal) {
    if (!window.confirm(`Delete the goal "${goal.title}" and its contribution history? Reserved money becomes unreserved; income and expenses are unchanged.`)) return;
    setBusy(true); setError('');
    try { await api(`/goals/${goal.id}`, { method: 'DELETE' }); changed('Goal deleted and its reservations released.'); }
    catch (err) { setError(errorText(err)); } finally { setBusy(false); }
  }
  return <div className="page-stack"><div className="intro-row"><div><h1>Give your savings a purpose.</h1><p>Create a goal and reserve money towards it, one contribution at a time.</p></div><button className="btn" onClick={() => setDialog({ kind: 'edit', goal: null })}>Create a goal</button></div><Alert>{error}</Alert><Alert type="info">Goal contributions are reservations within your recorded balance, not additional expenses. You cannot reserve more than your unreserved balance or the remaining goal target.</Alert>{!resource.data || resource.error ? <ResourceState resource={resource} /> : resource.data.length ? <div className="goals-grid">{resource.data.map(goal => <Panel key={goal.id} title={goal.title} subtitle={`Target date: ${dateLabel(goal.targetDate)}`} action={<Badge status={goal.status} />}><div className="goal-amount"><strong>{money(goal.savedAmount, meta.currency)}</strong><span>of {money(goal.targetAmount, meta.currency)}</span></div><Progress value={goal.progress} /><p className="muted">{percent(goal.progress)} of your goal reserved</p><div className="goal-actions"><button className="btn secondary small" onClick={() => setDialog({ kind: 'contribute', goal })} disabled={busy}>Savings &amp; history</button><button className="text-btn" onClick={() => setDialog({ kind: 'edit', goal })} disabled={busy}>Edit</button><button className="text-btn danger-text" onClick={() => remove(goal)} disabled={busy}>Delete</button></div></Panel>)}</div> : <Panel title="Your savings goals"><Empty title="Something worth saving for" action={<button className="btn" onClick={() => setDialog({ kind: 'edit', goal: null })}>Create your first goal</button>}>Start with a target, a date, and a name that means something to you.</Empty></Panel>}{dialog?.kind === 'edit' && <GoalForm goal={dialog.goal} meta={meta} onClose={() => setDialog(null)} changed={changed} />}{dialog?.kind === 'contribute' && <ContributionForm goal={dialog.goal} meta={meta} onClose={() => setDialog(null)} changed={changed} />}</div>;
}
