import React, { useEffect, useState } from 'react';
import { api } from '../lib/api.js';
import { useResource } from '../lib/hooks.js';
import { errorText, money, monthLabel, normalizeAmount, percent } from '../lib/format.js';
import { Alert, Badge, Field, Panel, Progress, ResourceState } from '../components/UI.jsx';
export default function Budgets({ month, revision, meta, changed }) {
  const resource = useResource(`/dashboard?month=${month}`, revision);
  const [amount, setAmount] = useState(''); const [busy, setBusy] = useState(false); const [error, setError] = useState('');
  const data = resource.data; const budget = data?.budget;
  useEffect(() => { setAmount(budget?.amount || ''); setError(''); }, [month, budget?.amount]);
  async function save(event) {
    event.preventDefault(); setBusy(true); setError('');
    try { await api('/budgets', { method: 'PUT', data: { month, amount: normalizeAmount(amount) } }); changed('Monthly budget saved.'); }
    catch (err) { setError(errorText(err)); } finally { setBusy(false); }
  }
  async function remove() {
    if (!window.confirm(`Remove the budget for ${monthLabel(month)}? Your transactions will not be deleted.`)) return;
    setBusy(true); setError('');
    try { await api(`/budgets/${month}`, { method: 'DELETE' }); changed('Budget removed. Transactions are unchanged.'); }
    catch (err) { setError(errorText(err)); } finally { setBusy(false); }
  }
  if (!data || resource.error) return <ResourceState resource={resource} />;
  return <div className="page-stack"><div className="intro-row"><div><h1>A plan for your month.</h1><p>Set a spending limit for {monthLabel(month)}. Your spending updates whenever transactions change.</p></div></div><div className="dashboard-columns"><Panel title={budget ? 'Manage your limit' : 'Set a monthly budget'} subtitle="One overall expense limit per user, per calendar month."><form onSubmit={save}><Alert>{error}</Alert><Field label={`Budget for ${monthLabel(month)} (${meta.currency})`} hint="The budget is a spending limit, not a transaction."><input required inputMode="decimal" maxLength="15" value={amount} onChange={event => setAmount(event.target.value)} disabled={busy} placeholder="0.00" /></Field><div className="form-actions">{budget && <button type="button" className="btn secondary danger-text" onClick={remove} disabled={busy}>Remove budget</button>}<button className="btn" disabled={busy}>{busy ? 'Saving...' : budget ? 'Update budget' : 'Create budget'}</button></div></form></Panel><Panel title="Your budget progress" subtitle={monthLabel(month)}>{budget ? <div className="budget-detail"><Badge status={budget.status} /><strong className="budget-main">{money(budget.spent, meta.currency)}</strong><p>spent out of {money(budget.amount, meta.currency)}</p><Progress value={budget.usedPercent} danger={budget.status === 'EXCEEDED'} /><div className="budget-detail-footer"><span>{percent(budget.usedPercent)} used</span><strong>{money(budget.remaining, meta.currency)} remaining</strong></div>{budget.status === 'EXCEEDED' && <Alert>You have exceeded your monthly limit. Recording expenses is still allowed so your records remain accurate.</Alert>}{['AT_LIMIT', 'NEAR_LIMIT'].includes(budget.status) && <Alert type="info">{budget.status === 'AT_LIMIT' ? 'You have reached your budget limit.' : 'You have used at least 80% of your monthly budget.'}</Alert>}</div> : <div className="budget-detail"><strong className="budget-main">{money(data.expenses, meta.currency)}</strong><p>recorded expenses this month. Create a budget to compare this spending with a limit.</p></div>}</Panel></div><Panel title="How it works"><div className="explain-grid"><div><strong>01 / Choose a month</strong><p>Use the month selector above to create or review any monthly budget.</p></div><div><strong>02 / Record your expenses</strong><p>Only expenses dated in that month count against its limit.</p></div><div><strong>03 / Stay informed</strong><p>Warnings appear at 80%, at the limit, and when expenses exceed it.</p></div></div></Panel></div>;
}
