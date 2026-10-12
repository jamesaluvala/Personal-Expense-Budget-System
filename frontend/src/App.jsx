import React, { useCallback, useEffect, useState } from 'react';
import { api } from './lib/api.js';
import { useResource } from './lib/hooks.js';
import { errorText } from './lib/format.js';
import { Alert, Brand, Icon, Loading, ResourceState } from './components/UI.jsx';
import TransactionForm from './components/TransactionForm.jsx';
import Auth from './pages/Auth.jsx';
import Overview from './pages/Overview.jsx';
import Transactions from './pages/Transactions.jsx';
import Budgets from './pages/Budgets.jsx';
import Goals from './pages/Goals.jsx';
import Categories from './pages/Categories.jsx';
import Reports from './pages/Reports.jsx';
const PAGES = { overview: 'Overview', transactions: 'Transactions', budgets: 'Monthly budget', goals: 'Savings goals', categories: 'Categories', reports: 'Reports' };
function pageFromHash() { const page = window.location.hash.slice(1); return Object.hasOwn(PAGES, page) ? page : 'overview'; }
function Workspace({ user, meta, onLogout }) {
  const [page, setPage] = useState(pageFromHash); const [month, setMonth] = useState(meta.today.slice(0, 7));
  const [revision, setRevision] = useState(0); const [toast, setToast] = useState(''); const [entryDialog, setEntryDialog] = useState(null);
  const [loggingOut, setLoggingOut] = useState(false); const [error, setError] = useState('');
  const categories = useResource('/categories', revision);
  useEffect(() => { const change = () => setPage(pageFromHash()); window.addEventListener('hashchange', change); return () => window.removeEventListener('hashchange', change); }, []);
  useEffect(() => { if (!toast) return; const timer = setTimeout(() => setToast(''), 5000); return () => clearTimeout(timer); }, [toast]);
  function navigate(destination) { window.location.hash = destination; setPage(destination); }
  const changed = useCallback(message => { setRevision(value => value + 1); if (message) setToast(message); }, []);
  function openEntry(entry = null, type = 'EXPENSE') { setEntryDialog({ entry, type }); }
  async function logout() {
    setLoggingOut(true); setError('');
    try { await api('/auth/logout', { method: 'POST' }); onLogout(); }
    catch (err) { setError(errorText(err)); } finally { setLoggingOut(false); }
  }
  const props = { user, meta, month, revision, changed, categories: categories.data || [], navigate, openEntry };
  const screens = { overview: Overview, transactions: Transactions, budgets: Budgets, goals: Goals, categories: Categories, reports: Reports };
  const Page = screens[page];
  return <div className="app-layout"><aside className="sidebar"><Brand /><div className="sidebar-label">WORKSPACE</div><nav aria-label="Main navigation">{Object.entries(PAGES).map(([key, label]) => <button key={key} className={`nav-item ${page === key ? 'active' : ''}`} aria-current={page === key ? 'page' : undefined} onClick={() => navigate(key)}><Icon name={key} /><span>{label}</span>{page === key && <span className="nav-dot" />}</button>)}</nav><div className="sidebar-tip"><Icon name="shield" size={25} /><h3>A habit worth keeping.</h3><p>Small, regular updates help you understand your money.</p></div><div className="account"><span className="avatar">{user.name.charAt(0).toUpperCase()}</span><div><strong>{user.name}</strong><small>Personal workspace</small></div><button className="icon-btn" title="Sign out" aria-label="Sign out" onClick={logout} disabled={loggingOut}><Icon name="logout" /></button></div></aside><main className="main"><header className="topbar"><div className="breadcrumb">Workspace <span>/</span> <strong>{PAGES[page]}</strong></div><div className="topbar-actions">{!['goals', 'categories'].includes(page) && <label className="month-control"><span>Viewing</span><input type="month" aria-label="Selected month" min="2000-01" max="2100-12" value={month} onChange={event => { const value = event.target.value; if (/^(20[0-9]{2}|2100)-(0[1-9]|1[0-2])$/.test(value)) setMonth(value); }} /></label>}<button className="icon-btn" title="Refresh records" aria-label="Refresh records" onClick={() => changed()}><Icon name="refresh" /></button><button className="btn small" onClick={() => openEntry()} disabled={!categories.data || Boolean(categories.error)}><Icon name="plus" />Add transaction</button></div></header><div className="page"><Alert>{error}</Alert>{!categories.data || categories.error ? <ResourceState resource={categories} /> : <Page {...props} />}</div><footer className="main-footer">Ledgerly / Personal Expense &amp; Budget Management System <span>Currency: {meta.currency} / Date zone: {meta.timeZone}</span></footer></main>{toast && <div className="toast" role="status"><span>{toast}</span><button className="icon-btn" aria-label="Dismiss notification" onClick={() => setToast('')}><Icon name="close" size={16} /></button></div>}{entryDialog && categories.data && <TransactionForm entry={entryDialog.entry} initialType={entryDialog.type} categories={categories.data} meta={meta} onClose={() => setEntryDialog(null)} onSaved={changed} />}</div>;
}
export default function App() {
  const [meta, setMeta] = useState(null); const [user, setUser] = useState(null); const [loading, setLoading] = useState(true);
  const [error, setError] = useState(''); const [notice, setNotice] = useState(''); const [attempt, setAttempt] = useState(0);
  useEffect(() => {
    const controller = new AbortController(); let alive = true;
    setLoading(true); setError('');
    (async () => {
      const config = await api('/meta', { signal: controller.signal });
      let profile = null;
      try { profile = await api('/auth/me', { signal: controller.signal }); }
      catch (err) { if (err.status !== 401) throw err; }
      if (alive) { setMeta(config); setUser(profile); }
    })().catch(err => { if (alive && err.name !== 'AbortError') setError(errorText(err)); }).finally(() => { if (alive) setLoading(false); });
    return () => { alive = false; controller.abort(); };
  }, [attempt]);
  useEffect(() => {
    function expired() { setUser(null); setNotice('Your session has expired. Sign in again to continue.'); }
    window.addEventListener('session-expired', expired); return () => window.removeEventListener('session-expired', expired);
  }, []);
  if (loading) return <div className="boot-screen"><Brand /><Loading /></div>;
  if (error || !meta) return <div className="boot-screen"><Brand /><div className="boot-error"><h1>Let's connect your workspace.</h1><Alert>{error || 'Backend configuration is not available.'}</Alert><p>This app needs the Spring Boot backend and MySQL database. Deploying only the React files is not enough. See START-HERE.md in the project ZIP.</p><button className="btn" onClick={() => setAttempt(value => value + 1)}>Retry connection</button></div></div>;
  return user ? <Workspace key={user.id} user={user} meta={meta} onLogout={() => { setUser(null); setNotice('You have signed out.'); }} /> : <Auth notice={notice} onLogin={profile => { setUser(profile); setNotice(''); }} />;
}
