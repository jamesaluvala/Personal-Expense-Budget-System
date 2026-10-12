import React, { useState } from 'react';
import { api } from '../lib/api.js';
import { errorText } from '../lib/format.js';
import { Alert, Brand, Field, Icon } from '../components/UI.jsx';
export default function Auth({ onLogin, notice }) {
  const [register, setRegister] = useState(false); const [busy, setBusy] = useState(false); const [error, setError] = useState('');
  const [form, setForm] = useState({ name: '', email: '', password: '', confirm: '' });
  function update(field, value) { setForm(old => ({ ...old, [field]: value })); }
  async function submit(event) {
    event.preventDefault(); setBusy(true); setError('');
    try {
      if (register) {
        if (form.password !== form.confirm) throw new Error('Passwords do not match.');
        if (new TextEncoder().encode(form.password).length > 72) throw new Error('Password must be at most 72 UTF-8 bytes.');
        await api('/auth/register', { method: 'POST', data: { name: form.name.trim(), email: form.email.trim(), password: form.password } });
      }
      onLogin(await api('/auth/login', { method: 'POST', form: true, data: { email: form.email.trim(), password: form.password } }));
      setForm({ name: '', email: '', password: '', confirm: '' });
    } catch (err) { setError(errorText(err)); } finally { setBusy(false); }
  }
  return <div className="auth-layout"><aside className="auth-story"><Brand /><div className="auth-story-content"><div className="eyebrow">LESS GUESSWORK. MORE CLARITY.</div><h1>Make room for<br />what matters.</h1><p>A simple home for your income, everyday expenses, and the goals you're working towards.</p><div className="auth-illustration" aria-hidden="true"><div className="illustration-card first"><span className="illustration-symbol">+</span><div><strong>Know what comes in</strong><small>Organize your income</small></div></div><div className="illustration-card second"><span className="illustration-symbol">-</span><div><strong>See where it goes</strong><small>Understand your spending</small></div></div><div className="illustration-card third"><Icon name="goals" size={28} /><div><strong>Plan what comes next</strong><small>Build your savings goals</small></div></div></div></div><div className="auth-footer"><Icon name="shield" /> Your financial records, in your own workspace.</div></aside><main className="auth-main"><div className="auth-form"><span className="eyebrow">YOUR PERSONAL FINANCE WORKSPACE</span><h2>{register ? 'Start with a clean slate.' : 'Welcome back.'}</h2><p>{register ? 'Create an account. Your records will start empty.' : 'Sign in to see the bigger picture.'}</p><Alert type="info">{notice}</Alert><Alert>{error}</Alert><form onSubmit={submit}>{register && <Field label="Your name"><input required autoComplete="name" maxLength="80" value={form.name} onChange={event => update('name', event.target.value)} disabled={busy} /></Field>}<Field label="Email address"><input required type="email" autoComplete="username" maxLength="190" value={form.email} onChange={event => update('email', event.target.value)} disabled={busy} placeholder="you@example.com" /></Field><Field label="Password" hint={register ? 'At least 12 characters; use a unique password.' : undefined}><input required type="password" autoComplete={register ? 'new-password' : 'current-password'} minLength={register ? 12 : undefined} maxLength="72" value={form.password} onChange={event => update('password', event.target.value)} disabled={busy} /></Field>{register && <Field label="Confirm password"><input required type="password" autoComplete="new-password" value={form.confirm} onChange={event => update('confirm', event.target.value)} disabled={busy} /></Field>}<button className="btn full" disabled={busy}>{busy ? 'Please wait...' : register ? 'Create account' : 'Sign in'}<Icon name="arrow" /></button></form><p className="auth-switch">{register ? 'Already have an account?' : 'New to Ledgerly?'} <button className="text-btn" disabled={busy} onClick={() => { setRegister(value => !value); setError(''); setForm(old => ({ ...old, password: '', confirm: '' })); }}>{register ? 'Sign in' : 'Create an account'}</button></p><div className="auth-footnote">Personal Expense &amp; Budget Management System<br />React + Spring Boot + MySQL</div></div></main></div>;
}
