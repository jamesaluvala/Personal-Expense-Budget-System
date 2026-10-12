export function normalizeAmount(value) {
  const text = String(value).trim();
  if (!/^(0|[1-9][0-9]{0,11})(\.[0-9]{1,2})?$/.test(text)) {
    throw new Error('Enter an amount such as 250 or 250.50, with at most 12 digits before the decimal point.');
  }
  const [whole, fraction = ''] = text.split('.');
  const cents = BigInt(whole) * 100n + BigInt(fraction.padEnd(2, '0'));
  if (cents <= 0n) throw new Error('Amount must be greater than zero.');
  return `${whole}.${fraction.padEnd(2, '0')}`;
}
export function money(value, currency = 'INR') {
  if (value === null || value === undefined) return '--';
  return new Intl.NumberFormat('en-IN', { style: 'currency', currency, minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(Number(value));
}
export function dateLabel(value) {
  if (!value) return '--';
  const [year, month, day] = value.split('-').map(Number);
  return new Date(year, month - 1, day).toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' });
}
export function monthLabel(value, short = false) {
  const [year, month] = value.split('-').map(Number);
  return new Date(year, month - 1, 1).toLocaleDateString('en-GB', { month: short ? 'short' : 'long', year: 'numeric' });
}
export function monthBounds(value) {
  if (!/^[0-9]{4}-(0[1-9]|1[0-2])$/.test(value)) throw new Error('Invalid month.');
  const [year, month] = value.split('-').map(Number);
  return { from: `${value}-01`, to: `${value}-${String(new Date(Date.UTC(year, month, 0)).getUTCDate()).padStart(2, '0')}` };
}
export function percent(value) { return value == null ? '--' : `${Number(value).toFixed(1)}%`; }
export function clampPercent(value) { return Math.max(0, Math.min(100, Number(value) || 0)); }
export function errorText(error) {
  const fields = error?.fields ? Object.entries(error.fields).map(([field, message]) => `${field}: ${message}`).join('; ') : '';
  return fields || error?.message || 'Something went wrong. Please try again.';
}
