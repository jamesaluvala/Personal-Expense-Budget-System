import test from 'node:test';
import assert from 'node:assert/strict';
import { normalizeAmount, monthBounds, clampPercent, percent, money, dateLabel } from '../src/lib/format.js';
test('integer amounts retain exact cents', () => assert.equal(normalizeAmount('150'), '150.00'));
test('one decimal place is normalized', () => assert.equal(normalizeAmount(' 10.5 '), '10.50'));
test('small positive amounts are accepted', () => assert.equal(normalizeAmount('0.01'), '0.01'));
test('largest permitted amount is exact', () => assert.equal(normalizeAmount('999999999999.99'), '999999999999.99'));
test('invalid money is rejected', () => {
  for (const value of ['', '0', '-1', '0.00', '1.001', '1e3', 'NaN', 'Infinity', '1,000', '1000000000000', '01', '.5']) assert.throws(() => normalizeAmount(value), value);
});
test('ordinary February range', () => assert.deepEqual(monthBounds('2025-02'), { from: '2025-02-01', to: '2025-02-28' }));
test('leap-year February range', () => assert.deepEqual(monthBounds('2024-02'), { from: '2024-02-01', to: '2024-02-29' }));
test('December does not move into another month', () => assert.deepEqual(monthBounds('2026-12'), { from: '2026-12-01', to: '2026-12-31' }));
test('invalid month is rejected', () => { for (const value of ['2026-13', '2026-00', '2026-1']) assert.throws(() => monthBounds(value)); });
test('progress clamps for display, not finance arithmetic', () => { assert.equal(clampPercent(125), 100); assert.equal(clampPercent(-10), 0); });
test('percentage handles no-income case without infinity', () => { assert.equal(percent(null), '--'); assert.equal(percent('25.12'), '25.1%'); });
test('formatting preserves two displayed decimals', () => assert.match(money('1200.50', 'INR'), /1,200\.50/));
test('date formatting avoids UTC midnight shifts', () => assert.match(dateLabel('2024-02-29'), /29 Feb 2024/));
