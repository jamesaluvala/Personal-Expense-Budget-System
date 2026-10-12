import React, { useState } from 'react';
import { api } from '../lib/api.js';
import { errorText } from '../lib/format.js';
import { Alert, Empty, Field, Modal, Panel } from '../components/UI.jsx';
function CategoryForm({ category, onClose, changed }) {
  const [name, setName] = useState(category?.name || ''); const [type, setType] = useState(category?.type || 'EXPENSE');
  const [busy, setBusy] = useState(false); const [error, setError] = useState('');
  async function save(event) {
    event.preventDefault(); setBusy(true); setError('');
    try { await api(category ? `/categories/${category.id}` : '/categories', { method: category ? 'PUT' : 'POST', data: { name: name.trim(), type } }); changed('Category saved.'); onClose(); }
    catch (err) { setError(errorText(err)); } finally { setBusy(false); }
  }
  return <Modal title={category ? 'Rename category' : 'Create category'} onClose={onClose} busy={busy}><form onSubmit={save}><Alert>{error}</Alert><Field label="Name"><input required maxLength="60" value={name} onChange={event => setName(event.target.value)} disabled={busy} /></Field><Field label="Type" hint={category ? 'A category type cannot be changed after creation.' : undefined}><select value={type} onChange={event => setType(event.target.value)} disabled={busy || Boolean(category)}><option value="EXPENSE">Expense</option><option value="INCOME">Income</option></select></Field><div className="form-actions"><button type="button" className="btn secondary" onClick={onClose} disabled={busy}>Cancel</button><button className="btn" disabled={busy}>{busy ? 'Saving...' : 'Save category'}</button></div></form></Modal>;
}
export default function Categories({ categories, changed }) {
  const [dialog, setDialog] = useState(null); const [error, setError] = useState(''); const [busy, setBusy] = useState(false);
  async function remove(category) {
    if (!window.confirm(`Delete category "${category.name}"? This is only possible when no transactions use it.`)) return;
    setBusy(true); setError('');
    try { await api(`/categories/${category.id}`, { method: 'DELETE' }); changed('Category deleted.'); }
    catch (err) { setError(errorText(err)); } finally { setBusy(false); }
  }
  return <div className="page-stack"><div className="intro-row"><div><h1>Organized your way.</h1><p>Keep your income and expenses in categories that make sense to you.</p></div><button className="btn" onClick={() => setDialog({ category: null })}>Create category</button></div><Alert>{error}</Alert><div className="dashboard-columns">{['EXPENSE', 'INCOME'].map(type => <Panel key={type} title={type === 'EXPENSE' ? 'Expense categories' : 'Income categories'} subtitle="These labels belong only to your account.">{categories.filter(category => category.type === type).length ? <div className="category-list">{categories.filter(category => category.type === type).map(category => <div key={category.id}><span className={`entry-mark ${type.toLowerCase()}`}>{category.name.charAt(0).toUpperCase()}</span><strong>{category.name}</strong><div className="row-actions"><button className="text-btn" onClick={() => setDialog({ category })} disabled={busy}>Rename</button><button className="text-btn danger-text" onClick={() => remove(category)} disabled={busy}>Delete</button></div></div>)}</div> : <Empty title="No categories yet">Create a category to start organizing transactions.</Empty>}</Panel>)}</div><p className="page-note">Renaming a category updates its label in existing transactions and reports. Categories used by transactions cannot be deleted.</p>{dialog && <CategoryForm category={dialog.category} onClose={() => setDialog(null)} changed={changed} />}</div>;
}
