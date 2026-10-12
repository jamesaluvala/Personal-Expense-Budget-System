import React, { useEffect, useState } from 'react';
import { api } from '../lib/api.js';
import { useDebounce, useResource } from '../lib/hooks.js';
import { errorText, monthBounds } from '../lib/format.js';
import { Alert, Empty, EntryTable, Field, Panel, ResourceState } from '../components/UI.jsx';
export default function Transactions({ month, revision, meta, categories, openEntry, changed }) {
  const [filters, setFilters] = useState({ ...monthBounds(month), type: '', categoryId: '', search: '', sort: 'date_desc' });
  const [page, setPage] = useState(0); const [busy, setBusy] = useState(''); const [error, setError] = useState('');
  const search = useDebounce(filters.search);
  useEffect(() => { setFilters(old => ({ ...old, ...monthBounds(month) })); setPage(0); }, [month]);
  function update(field, value) { setFilters(old => ({ ...old, [field]: value, ...(field === 'type' ? { categoryId: '' } : {}) })); setPage(0); }
  const params = new URLSearchParams({ page: String(page), size: '20', sort: filters.sort });
  for (const [key, value] of Object.entries({ ...filters, search })) if (value) params.set(key, value);
  const resource = useResource(`/transactions?${params}`, revision);
  async function remove(entry) {
    if (!window.confirm(`Delete "${entry.description}"? This permanently removes the transaction and updates balances, reports and budgets.`)) return;
    setBusy(`${entry.type}-${entry.id}`); setError('');
    try {
      await api(`/transactions/${entry.type}/${entry.id}`, { method: 'DELETE' });
      if (resource.data?.items.length === 1 && page > 0) setPage(value => value - 1);
      changed('Transaction deleted.');
    } catch (err) { setError(errorText(err)); } finally { setBusy(''); }
  }
  function reset() { setFilters({ ...monthBounds(month), type: '', categoryId: '', search: '', sort: 'date_desc' }); setPage(0); }
  const data = resource.data;
  return <div className="page-stack"><div className="intro-row"><div><h1>Every transaction, organized.</h1><p>Search your records, review spending, or correct an entry.</p></div><div className="row-actions"><button className="btn secondary" onClick={() => openEntry(null, 'INCOME')}>Add income</button><button className="btn" onClick={() => openEntry(null, 'EXPENSE')}>Add expense</button></div></div><Panel title="Transaction history" subtitle="Dates default to the selected month. Clear them to search all history." action={<button className="text-btn" onClick={reset}>Reset filters</button>}><div className="filters"><Field label="Search"><input type="search" maxLength="100" placeholder="Description or category" value={filters.search} onChange={event => update('search', event.target.value)} /></Field><Field label="Type"><select value={filters.type} onChange={event => update('type', event.target.value)}><option value="">All types</option><option value="INCOME">Income</option><option value="EXPENSE">Expense</option></select></Field><Field label="Category"><select value={filters.categoryId} onChange={event => update('categoryId', event.target.value)}><option value="">All categories</option>{categories.filter(category => !filters.type || category.type === filters.type).map(category => <option key={category.id} value={category.id}>{category.name} ({category.type.toLowerCase()})</option>)}</select></Field><Field label="From"><input type="date" min="2000-01-01" max="2100-12-31" value={filters.from} onChange={event => update('from', event.target.value)} /></Field><Field label="To"><input type="date" min="2000-01-01" max="2100-12-31" value={filters.to} onChange={event => update('to', event.target.value)} /></Field><Field label="Sort"><select value={filters.sort} onChange={event => update('sort', event.target.value)}><option value="date_desc">Newest first</option><option value="date_asc">Oldest first</option><option value="amount_desc">Highest amount</option><option value="amount_asc">Lowest amount</option><option value="description_asc">Description A-Z</option></select></Field></div><Alert>{error}</Alert>{filters.from && filters.to && filters.from > filters.to ? <Alert>From date cannot be after to date.</Alert> : !data || resource.error ? <ResourceState resource={resource} /> : <div aria-busy={resource.loading}>{data.items.length ? <EntryTable entries={data.items} currency={meta.currency} onEdit={entry => openEntry(entry)} onDelete={remove} busyKey={busy} /> : <Empty title="No matching transactions">Try changing your filters, or add a new transaction.</Empty>}<div className="pagination"><span>{data.total} matching records / Page {page + 1} of {Math.max(1, Math.ceil(data.total / 20))}</span><div className="row-actions"><button className="btn secondary small" disabled={page === 0 || resource.loading} onClick={() => setPage(value => value - 1)}>Previous</button><button className="btn secondary small" disabled={(page + 1) * 20 >= data.total || resource.loading} onClick={() => setPage(value => value + 1)}>Next</button></div></div></div>}</Panel></div>;
}
