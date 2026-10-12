# Faculty demonstration and acceptance checklist

These are tests to carry out on the real hosted application, not claims that they have already passed. Use a disposable demonstration account and mark entered numbers as test data.

1. Open the hosted website. Register account A with a unique email and a long password. Confirm no transactions and zero financial totals. Sign out, reject a wrong password, then sign in correctly.
2. Add income 10000.00 and expense 1200.50 dated today using matching categories. Confirm monthly income 10000.00, expense 1200.50, net savings and recorded balance 8799.50. Refresh the browser and confirm the same data.
3. Set this month's budget to 1000.00. Confirm the exceeded warning and remaining -200.50. Edit the expense to 600.50; confirm the warning clears and remaining becomes 399.50. Delete it and confirm expenses become zero.
4. Create a category, add a transaction using it, rename the category and confirm history updates. Attempt to delete the used category and confirm a helpful refusal. After reassigning/deleting the transaction, delete the unused category.
5. Enter clearly marked test transactions on 2024-02-29 and 2024-03-01. Select February 2024; verify only the leap-day entry is included. Move it to March and verify both months when opened. Remove these test entries after checking.
6. Create a goal target 2000.00 with a future deadline. Reserve 500.00. Confirm 25% progress, unchanged income/expense totals, and available balance reduced by 500.00. Reject contributions exceeding available balance or the goal target. Release the contribution and confirm restoration.
7. Open history. Search a description, filter type/category/date, change sort and test pagination after creating enough real test entries. Clear filters and verify all owned entries are returned.
8. Open the monthly report. Check dates, number of rows and totals against history. Download CSV and open it in a spreadsheet. Use Print / Save as PDF and review the print preview. These exports must describe the selected month, not every month.
9. In a separate private/incognito browser, register account B. Confirm none of A's categories/transactions/budgets/goals is exposed. With B's authenticated Postman session, attempt A's transaction/category/goal IDs; require 404/denial. The Java integration tests also exercise this directly.
10. Try empty descriptions, zero/negative amounts, 3 decimal places, impossible dates, future transaction dates and wrong category types. Require clear validation messages and no new stored records.
11. Restart only the Java application service, leaving MySQL and its volume intact. Sign in again. Confirm the records remain. MySQL persistence is not proven by browser refresh alone. Verify database backups separately.
12. Open the site in a narrow mobile browser and a desktop browser. Check dialogs, nav, tables, charts, keyboard focus, CSV, refresh and error screens. Stop the backend temporarily and verify the frontend displays an error, not false totals; restore it afterward.

## Evidence to retain

Save screenshots of the empty account, real entered transactions, budget recalculation, monthly report, two-account isolation, passing online CI result and MySQL persistence check. Do not include real credentials or another person's financial records in screenshots.

## Optional viva questions

Explain frontend vs backend, REST API, why SQL DECIMAL/Java BigDecimal are used, why dates use a half-open month interval, how user ownership is enforced, why BCrypt is not reversible encryption, what CSRF prevents, why goals are reservations rather than expenses, what Flyway changes, and why a React deployment alone is not full-stack.
