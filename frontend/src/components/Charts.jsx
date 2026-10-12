import React, { useState } from 'react';
import { money, monthLabel } from '../lib/format.js';
export function TrendChart({ data, currency }) {
  const [selected, setSelected] = useState(null);
  const max = Math.max(1, ...data.flatMap(row => [Number(row.income), Number(row.expenses)]));
  const active = data.find(row => row.month === selected);
  return <div><div className="chart-legend"><span className="legend-income">Income</span><span className="legend-expense">Expenses</span></div><div className="trend-chart">{data.map(row => <button type="button" key={row.month} className={`chart-month ${selected === row.month ? 'selected' : ''}`} onClick={() => setSelected(row.month)} aria-pressed={selected === row.month} aria-label={`${monthLabel(row.month)}: income ${money(row.income, currency)}, expenses ${money(row.expenses, currency)}`}><span className="chart-columns"><span className="chart-income" style={{ height: `${Number(row.income) / max * 100}%` }} /><span className="chart-expense" style={{ height: `${Number(row.expenses) / max * 100}%` }} /></span><span className="chart-label">{monthLabel(row.month, true).split(' ')[0]}</span></button>)}</div><div className="chart-detail" aria-live="polite">{active ? `${monthLabel(active.month)}: income ${money(active.income, currency)} / expenses ${money(active.expenses, currency)}` : 'Select a month to see the exact amounts.'}</div></div>;
}
export function CategoryChart({ data, total, currency }) {
  const [selected, setSelected] = useState('');
  const denominator = Number(total);
  return <div className="category-chart">{data.map((row, index) => { const share = denominator > 0 ? Number(row.amount) / denominator * 100 : 0; return <button type="button" className={`category-bar ${selected === row.category ? 'selected' : ''}`} key={row.category} onClick={() => setSelected(selected === row.category ? '' : row.category)} aria-pressed={selected === row.category}><span className="bar-label"><span>{row.category}</span><strong>{money(row.amount, currency)}</strong></span><span className="bar-track"><span style={{ width: `${share}%`, opacity: Math.max(0.45, 1 - index * 0.08) }} /></span>{selected === row.category && <small>{share.toFixed(1)}% of this month's expenses</small>}</button>; })}</div>;
}
