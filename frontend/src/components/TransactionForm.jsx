import React, { useState } from 'react';
import { api } from '../lib/api.js';
import { errorText, normalizeAmount } from '../lib/format.js';
import { Alert, Field, Modal } from './UI.jsx';
export default function TransactionForm({ entry, initialType = 'EXPENSE', categories, meta, onClose, onSaved }) {
  const [form, setForm] = useState({ type: entry?.type || initialType, categoryId: entry?.categoryId || '', amount: entry?.amount || '', date: entry?.date || meta.today, description: entry?.description || '' });
  const [busy, setBusy] = useState(false); const [error, setError] = useState('');
  const choices = categories.filter(category => category.type === form.type);
  function update(field, value) { setForm(old => ({ ...old, [field]: value, ...(field === 'type' ? { categoryId: '' } : {}) })); }
  async function submit(event) {
    event.preventDefault(); setError(''); setBusy(true);
    try {
      if (!form.categoryId) throw new Error('Please choose a category.');
      const data = { ...form, categoryId: Number(form.categoryId), amount: normalizeAmount(form.amount), description: form.description.trim() };
      await api(entry ? `/transactions/${entry.type}/${entry.id}` : '/transactions', { method: entry ? 'PUT' : 'POST', data });
      onSaved(entry ? 'Transaction updated.' : 'Transaction saved.'); onClose();
    } catch (err) { setError(errorText(err)); } finally { setBusy(false); }
  }
  return <Modal title={entry ? 'Edit transaction' : 'Add a transaction'} onClose={onClose} busy={busy}><form onSubmit={submit}><Alert>{error}</Alert><div className="form-grid"><Field label="Type"><select value={form.type} onChange={event => update('type', event.target.value)} disabled={busy || Boolean(entry)}><option value="EXPENSE">Expense</option><option value="INCOME">Income</option></select></Field><Field label="Category"><select required value={form.categoryId} onChange={event => update('categoryId', event.target.value)} disabled={busy}><option value="">Choose a category</option>{choices.map(category => <option key={category.id} value={category.id}>{category.name}</option>)}</select></Field><Field label={`Amount (${meta.currency})`} hint="Use up to two decimal places."><input required inputMode="decimal" maxLength="15" value={form.amount} onChange={event => update('amount', event.target.value)} disabled={busy} placeholder="0.00" /></Field><Field label="Transaction date"><input required type="date" min="2000-01-01" max={meta.today} value={form.date} onChange={event => update('date', event.target.value)} disabled={busy} /></Field></div><Field label="Description"><input required maxLength="250" value={form.description} onChange={event => update('description', event.target.value)} disabled={busy} placeholder="What was this transaction for?" /></Field>{choices.length === 0 && <Alert type="info">Create a matching category in Categories first.</Alert>}<div className="form-actions"><button className="btn secondary" type="button" onClick={onClose} disabled={busy}>Cancel</button><button className="btn" disabled={busy || choices.length === 0}>{busy ? 'Saving...' : 'Save transaction'}</button></div></form></Modal>;
}
