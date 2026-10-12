# Requirements and scope

## Exact baseline from the supplied one-page PDF

Reference: `DOC-20260914-WA0191.pdf`, page 1, titled Personal Expense & Budget Management System.

The abstract describes a web-based platform for managing personal income, expenses, budgets and financial records. It calls for recording income and daily expenses, categorizing transactions, setting monthly budgets, monitoring spending patterns and analyzing financial activity through an interactive dashboard.

The technology paragraph specifies a **React frontend**, **Spring Boot backend with RESTful APIs**, and **SQL storage** for users, income, expenses, categories, transactions and budgets. **Postman** is for API testing and **Eclipse** for backend development. The React interface includes transaction recording, category management, budgets, transaction history, dashboards and reports.

The final paragraph lists possible extensions: automated budget recommendations, expense predictions, financial reports, savings goals, notification services and advanced spending analytics. Financial reports appear in both the interface description and the extension list; this project includes basic monthly reports and exports, not a forecasting subsystem.

## Additional requirements supplied in the chat

Secure registration/login, independent records per user, budget warnings, savings goal progress, charts, transaction search/filter/sort, downloadable reports, useful in-app insights, real database calculations, validation, actual testing and a maintainable code structure.

## Decisions made for this implementation (not mandated by the PDF)

| Decision | Scope |
|---|---|
| Database | MySQL 8.4, following the user's preference |
| Authentication | Spring Security server-side sessions and HttpOnly cookies, not browser-stored JWTs |
| SQL access | Explicit named-parameter Spring JDBC queries, not JPA |
| Currency | One configured currency for the whole installation, default INR with two decimal places; do not change it after recording data without a conversion migration |
| Calendar | Default Asia/Kolkata; financial transaction dates are date-only, not UTC timestamps |
| Amounts | Positive values, maximum 999999999999.99, at most two decimal places |
| Dates | Transaction/contribution dates 2000-01-01 through the application's current date; report/budget months 2000-01 through 2100-12 |
| Budget | One overall expense budget per user and calendar month; not category sub-budgets or recurring transactions |
| Savings | Contributions reserve existing recorded balance and do not transfer money or create expenses |
| Reporting | Monthly statement and CSV; Print / Save as PDF uses the browser's printing feature |
| Notifications | Visible in-app messages derived when data is requested; not background push or email |
| Hosting | Cloud build on Railway, MySQL with a persistent volume; optional Vercel React hosting |

## Traceability

| Requirement | Main source |
|---|---|
| Registration and own records | `UserService`, `SecurityConfig`, owned SQL predicates |
| Income / expenses / categories | `LedgerService`, `Transactions.jsx`, `Categories.jsx` |
| Monthly budgets and warnings | `PlanningService`, `FinanceRules`, `Budgets.jsx` |
| Dashboard and patterns | `ReportService`, `Overview.jsx`, `Charts.jsx` |
| History, filters, sorting | `LedgerService.entries`, `Transactions.jsx` |
| Reports and exports | `ReportService.report/csv`, `Reports.jsx` |
| Savings goals | `PlanningService`, `Goals.jsx` |
| Permanent records | Flyway schema and MySQL; volume retained across app restarts |
| Verification | Java tests, frontend tests, Postman, Playwright, demo checklist |

No automation, prediction model, payment service, bank integration or email service is claimed. H2 is used only in tests. Ten starter category names are created per account; financial data starts empty.
