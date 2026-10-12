import React, { useEffect, useId, useRef } from 'react';
import { clampPercent, dateLabel, money } from '../lib/format.js';
export function Icon({ name, size = 20 }) {
  const paths = {
    overview: 'M3 3h7v7H3z M14 3h7v11h-7z M3 14h7v7H3z M14 18h7v3h-7z',
    transactions: 'M4 7h16 M16 3l4 4-4 4 M20 17H4 M8 13l-4 4 4 4',
    budgets: 'M4 5h16v16H4z M4 10h16 M8 3v4 M16 3v4 M8 14h3 M8 17h7',
    goals: 'M12 3a9 9 0 1 0 9 9 M12 7a5 5 0 1 0 5 5 M12 12l9-9 M16 3h5v5',
    categories: 'M3 3h7v7H3z M14 3h7v7h-7z M3 14h7v7H3z M14 14h7v7h-7z',
    reports: 'M5 3h10l4 4v14H5z M14 3v5h5 M9 12h6 M9 16h6',
    plus: 'M12 5v14 M5 12h14',
    close: 'M6 6l12 12 M18 6L6 18',
    logout: 'M10 4H4v16h6 M8 12h12 M16 8l4 4-4 4',
    arrow: 'M5 12h14 M13 6l6 6-6 6',
    download: 'M12 3v12 M7 10l5 5 5-5 M4 17v4h16v-4',
    refresh: 'M20 6v5h-5 M4 18v-5h5 M6 7a7 7 0 0 1 12-1l2 3 M18 17a7 7 0 0 1-12 1l-2-3',
    shield: 'M12 3l8 3v6c0 5-8 9-8 9S4 17 4 12V6z M8 12l3 3 5-6',
  };
  return <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true"><path d={paths[name] || paths.overview} /></svg>;
}
export function Brand() { return <div className="brand"><span className="brand-mark">L<span /></span><span>Ledgerly<span className="brand-sub">PERSONAL FINANCE</span></span></div>; }
export function Field({ label, children, hint }) {
  const id = useId();
  return <label className="field" htmlFor={id}><span>{label}</span>{React.cloneElement(children, { id, 'aria-describedby': hint ? `${id}-hint` : undefined })}{hint && <small id={`${id}-hint`}>{hint}</small>}</label>;
}
export function Alert({ children, type = 'error' }) { return children ? <div className={`alert ${type}`} role={type === 'error' ? 'alert' : 'status'}>{children}</div> : null; }
export function Loading() { return <div className="loading" role="status"><span className="spinner" /> Loading your records...</div>; }
export function ResourceState({ resource }) {
  if (resource.error) return <div className="resource-error"><Alert>{resource.error}</Alert><button className="btn secondary" onClick={resource.reload}>Try again</button></div>;
  return <Loading />;
}
export function Empty({ title, children, action }) {
  return <div className="empty"><span className="empty-icon"><Icon name="transactions" size={26} /></span><h3>{title}</h3><p>{children}</p>{action}</div>;
}
export function Panel({ title, subtitle, action, children, className = '' }) {
  return <section className={`panel ${className}`}><div className="panel-heading"><div><h2>{title}</h2>{subtitle && <p>{subtitle}</p>}</div>{action}</div>{children}</section>;
}
export function Metric({ label, value, caption, accent, negative }) {
  return <section className={`metric ${accent ? 'accent' : ''}`}><p>{label}</p><strong className={negative ? 'negative' : ''}>{value}</strong><small>{caption}</small></section>;
}
export function Progress({ value, danger = false }) {
  return <div className={`progress ${danger ? 'danger' : ''}`} role="progressbar" aria-label="Progress" aria-valuenow={clampPercent(value)} aria-valuemin="0" aria-valuemax="100"><span style={{ width: `${clampPercent(value)}%` }} /></div>;
}
export function Badge({ status }) {
  const labels = { ON_TRACK: 'On track', NEAR_LIMIT: 'Near limit', AT_LIMIT: 'At limit', EXCEEDED: 'Over budget', COMPLETED: 'Completed', OVERDUE: 'Overdue', IN_PROGRESS: 'In progress' };
  return <span className={`badge ${['EXCEEDED', 'OVERDUE'].includes(status) ? 'bad' : ['NEAR_LIMIT', 'AT_LIMIT'].includes(status) ? 'warn' : ''}`}>{labels[status] || status}</span>;
}
export function Modal({ title, children, onClose, busy = false }) {
  const ref = useRef(null); const id = useId();
  useEffect(() => { const dialog = ref.current; dialog.showModal(); return () => dialog.close(); }, []);
  return <dialog ref={ref} className="modal" aria-labelledby={id} onCancel={event => { event.preventDefault(); if (!busy) onClose(); }} onClick={event => { if (event.target === ref.current && !busy) onClose(); }}><div className="modal-body"><div className="modal-heading"><h2 id={id}>{title}</h2><button type="button" className="icon-btn" aria-label="Close dialog" onClick={onClose} disabled={busy}><Icon name="close" /></button></div>{children}</div></dialog>;
}
export function EntryTable({ entries, currency, onEdit, onDelete, busyKey }) {
  return <div className="table-wrap"><table><thead><tr><th scope="col">Transaction</th><th scope="col">Category</th><th scope="col">Date</th><th scope="col" className="numeric">Amount</th>{(onEdit || onDelete) && <th scope="col" className="screen-only">Actions</th>}</tr></thead><tbody>{entries.map(entry => <tr key={`${entry.type}-${entry.id}`}><td><div className="transaction-name"><span className={`entry-mark ${entry.type.toLowerCase()}`}>{entry.type === 'INCOME' ? '+' : '-'}</span><div><strong>{entry.description}</strong><small>{entry.type === 'INCOME' ? 'Income' : 'Expense'}</small></div></div></td><td><span className="category-tag">{entry.categoryName}</span></td><td className="nowrap">{dateLabel(entry.date)}</td><td className={`numeric ${entry.type === 'INCOME' ? 'positive' : ''}`}>{entry.type === 'INCOME' ? '+' : '-'}{money(entry.amount, currency)}</td>{(onEdit || onDelete) && <td className="screen-only"><div className="row-actions">{onEdit && <button className="text-btn" onClick={() => onEdit(entry)} disabled={Boolean(busyKey)}>Edit</button>}{onDelete && <button className="text-btn danger-text" onClick={() => onDelete(entry)} disabled={Boolean(busyKey)}>{busyKey === `${entry.type}-${entry.id}` ? 'Deleting...' : 'Delete'}</button>}</div></td>}</tr>)}</tbody></table></div>;
}
