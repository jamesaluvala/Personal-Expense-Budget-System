import React, { useState } from 'react';
import { exportCsv } from '../lib/api.js';
import { useResource } from '../lib/hooks.js';
import { errorText, money, monthLabel } from '../lib/format.js';
import { Alert, Empty, EntryTable, Icon, Metric, Panel, ResourceState } from '../components/UI.jsx';
export default function Reports({ month, revision, meta, user }) {
  const resource = useResource(`/reports?month=${month}`, revision); const [busy, setBusy] = useState(false); const [error, setError] = useState('');
  async function download() {
    setBusy(true); setError('');
    try { await exportCsv(month); } catch (err) { setError(errorText(err)); } finally { setBusy(false); }
  }
  if (!resource.data || resource.error) return <ResourceState resource={resource} />;
  const { summary, transactions } = resource.data;
  return <div className="page-stack report-root"><div className="intro-row"><div><span className="eyebrow">PERSONAL FINANCIAL REPORT</span><h1>{monthLabel(month)}</h1><p>{user.name} / All amounts in {meta.currency}</p></div><div className="row-actions screen-only"><button className="btn secondary" onClick={() => window.print()} disabled={resource.loading}>Print / Save as PDF</button><button className="btn" onClick={download} disabled={busy || resource.loading}><Icon name="download" />{busy ? 'Preparing...' : 'Download CSV'}</button></div></div><Alert>{error}</Alert><div className="metrics report-metrics"><Metric label="Total income" value={money(summary.income, meta.currency)} caption={monthLabel(month)} /><Metric label="Total expenses" value={money(summary.expenses, meta.currency)} caption={`${summary.transactionCount} total transactions in period`} /><Metric label="Net savings" value={money(summary.netSavings, meta.currency)} caption="Income minus expenses for this month" accent /></div><Panel title="Monthly transaction record" subtitle="Every transaction dated within the selected month. No pagination or hidden rows in this report.">{transactions.length ? <EntryTable entries={transactions} currency={meta.currency} /> : <Empty title="No records for this month">There are no transactions to include in this report.</Empty>}</Panel>{summary.categories.length > 0 && <Panel title="Category-wise expenses"><div className="report-categories">{summary.categories.map(category => <div key={category.category}><span>{category.category}</span><strong>{money(category.amount, meta.currency)}</strong></div>)}</div></Panel>}<p className="page-note">This report is calculated from your saved records. It is not a bank statement. Goal reservations are not counted as expenses. Transactions include dates from the first day of the month up to, but not including, the next month.</p></div>;
}
