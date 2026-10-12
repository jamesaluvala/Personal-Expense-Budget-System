import { test, expect } from '@playwright/test';
import { randomUUID } from 'node:crypto';
const password = 'Demo-test-only-Password!42';
async function register(page) {
  const email = `browser-${randomUUID()}@example.com`;
  await page.goto('/');
  await page.getByRole('button', { name: 'Create an account', exact: true }).click();
  await page.getByLabel('Your name').fill('Browser Test User');
  await page.getByLabel('Email address').fill(email);
  await page.getByLabel('Password', { exact: true }).fill(password);
  await page.getByLabel('Confirm password').fill(password);
  await page.getByRole('button', { name: 'Create account', exact: true }).click();
  await expect(page.getByRole('navigation', { name: 'Main navigation' })).toBeVisible();
  return email;
}
async function addEntry(page, type, amount, description) {
  await page.getByRole('button', { name: 'Add transaction', exact: true }).click();
  const dialog = page.getByRole('dialog');
  await dialog.getByLabel('Type', { exact: true }).selectOption(type);
  await dialog.getByLabel('Category', { exact: true }).selectOption({ label: type === 'INCOME' ? 'Salary' : 'Food' });
  await dialog.getByLabel(/Amount \(/).fill(amount);
  await dialog.getByLabel('Description', { exact: true }).fill(description);
  await dialog.getByRole('button', { name: 'Save transaction' }).click();
  await expect(dialog).toBeHidden();
}
async function summary(page) {
  const response = await page.request.get('/api/dashboard');
  expect(response.ok()).toBeTruthy(); return response.json();
}

test('real registration, transactions, budget updates, goals, exports and persistence', async ({ page }) => {
  await register(page);
  expect((await summary(page)).transactionCount).toBe(0);
  await addEntry(page, 'INCOME', '10000.00', 'BROWSER TEST income');
  await addEntry(page, 'EXPENSE', '1200.00', 'BROWSER TEST expense');
  expect((await summary(page)).netSavings).toBe('8800.00');
  await page.getByRole('button', { name: 'Monthly budget', exact: true }).click();
  await page.getByLabel(/Budget for/).fill('1000.00');
  await page.getByRole('button', { name: 'Create budget', exact: true }).click();
  await expect(page.getByText('Over budget', { exact: true })).toBeVisible();
  await page.getByRole('button', { name: 'Transactions', exact: true }).click();
  await page.getByLabel('Search', { exact: true }).fill('BROWSER TEST expense');
  const row = page.getByRole('row').filter({ hasText: 'BROWSER TEST expense' });
  await row.getByRole('button', { name: 'Edit', exact: true }).click();
  const dialog = page.getByRole('dialog');
  await dialog.getByLabel(/Amount \(/).fill('600.00');
  await dialog.getByRole('button', { name: 'Save transaction' }).click();
  await expect(dialog).toBeHidden();
  expect((await summary(page)).budget.spent).toBe('600.00');
  expect((await summary(page)).budget.status).toBe('ON_TRACK');
  await page.getByRole('button', { name: 'Savings goals', exact: true }).click();
  await page.getByRole('button', { name: 'Create a goal', exact: true }).click();
  await dialog.getByLabel('Goal name', { exact: true }).fill('BROWSER TEST goal');
  await dialog.getByLabel(/Target amount/).fill('2000.00');
  const meta = await (await page.request.get('/api/meta')).json();
  const future = new Date(meta.today + 'T12:00:00Z'); future.setUTCFullYear(future.getUTCFullYear() + 1);
  await dialog.getByLabel('Target date').fill(future.toISOString().slice(0, 10));
  await dialog.getByRole('button', { name: 'Save goal' }).click();
  await expect(dialog).toBeHidden();
  await page.getByRole('button', { name: 'Savings & history' }).click();
  await dialog.getByLabel(/Amount to reserve/).fill('500.00');
  await dialog.getByRole('button', { name: 'Add savings contribution' }).click();
  await expect(dialog.getByRole('button', { name: 'Release', exact: true })).toBeVisible();
  await dialog.getByRole('button', { name: 'Close dialog' }).click();
  const actual = await summary(page);
  expect(actual.expenses).toBe('600.00'); expect(actual.reservedForGoals).toBe('500.00');
  expect(actual.availableBalance).toBe('8900.00');
  await page.getByRole('button', { name: 'Reports', exact: true }).click();
  const downloadPromise = page.waitForEvent('download');
  await page.getByRole('button', { name: /Download CSV/ }).click();
  const download = await downloadPromise;
  expect(download.suggestedFilename()).toContain('financial-report-');
  await page.reload();
  await expect(page.getByRole('navigation', { name: 'Main navigation' })).toBeVisible();
  expect((await summary(page)).income).toBe('10000.00');
  // Browser reload proves server readback, not a database/server restart. The manual checklist tests that.
});

test('second user cannot see first user data; mobile layout and logout', async ({ page, browser }) => {
  await register(page); await addEntry(page, 'INCOME', '99.00', 'PRIVATE BROWSER TEST');
  const entries = await (await page.request.get('/api/transactions')).json();
  const contextB = await browser.newContext({ baseURL: process.env.APP_URL || 'http://127.0.0.1:8080', viewport: { width: 390, height: 844 } });
  const second = await contextB.newPage(); await register(second);
  expect((await summary(second)).transactionCount).toBe(0);
  const foreign = await second.request.get(`/api/transactions/INCOME/${entries.items[0].id}`);
  expect(foreign.status()).toBe(404);
  await expect(second.getByRole('button', { name: 'Add transaction', exact: true })).toBeVisible();
  await second.getByRole('button', { name: 'Sign out', exact: true }).click();
  await expect(second.getByRole('button', { name: 'Sign in', exact: true })).toBeVisible();
  expect((await second.request.get('/api/transactions')).status()).toBe(401);
  await contextB.close();
});
