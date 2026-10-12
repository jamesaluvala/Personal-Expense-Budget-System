# Personal Expense & Budget Management System

**Ledgerly** is the interface name for this college project. The required project title remains Personal Expense & Budget Management System.

React frontend + Spring Boot REST API + persistent MySQL storage. Cloud-first instructions are in **START-HERE.md** and **docs/CLOUD-DEPLOYMENT.md**.

## Included functionality

| Area | Implementation |
|---|---|
| Accounts | Registration, password hashing, session login/logout, CSRF protection, per-user data access |
| Transactions | Income/expense create, edit, delete; categories; descriptions; dates; exact decimal amounts |
| History | Search, category/type/date filters, five sort choices and server pagination |
| Budgets | One overall monthly limit; actual spending; near-limit, at-limit and exceeded warnings |
| Dashboard | Monthly income, expense, net savings, all-time recorded balance, category chart, six-month trends, recent records |
| Savings goals | Targets, deadlines, progress, contributions and release history; reservations do not count as expenses |
| Reports | Monthly transaction report, actual totals, CSV download, browser Print / Save as PDF |
| Insights | In-app observations based on actual recorded data, not predictions or financial advice |
| Delivery | Dockerfile, optional local Compose file, Vercel proxy configuration, Postman collection, automated test source, cloud guide |

## Source basis

The uploaded one-page `DOC-20260914-WA0191.pdf` supplies the baseline: React, Spring Boot, SQL, Postman, Eclipse; users, transactions, categories, budgets, dashboard and reports. Secure login details, savings goals, exports and notifications are implemented in response to the user's additional requirements. The PDF does not specify library versions, a database schema, an authentication scheme, a hosting provider or a deployment URL. See **docs/REQUIREMENTS.md** for the mapping and decisions.

## Technology baseline

- Java 21; Spring Boot 4.0.8; Maven 3.9 in the cloud build.
- Spring MVC, Spring Security, Spring JDBC, Bean Validation, Flyway migrations, Jackson 3.
- MySQL 8.4; H2 is a test-only dependency, not application storage.
- React 19; Vite 7; Node 22.12 or later in the 22.x line is the recommended build runtime.
- Postman collection; optional Eclipse Maven import; Playwright browser tests and GitHub Actions.

Dependency versions are in `backend/pom.xml`, `frontend/package.json` and `e2e/package.json`. npm lockfiles could not be generated without network access. The online build uses `npm install`; commit its resulting lockfiles and change to `npm ci` after a successful verified build. No dependency directories or compiled binaries are bundled.

## Project layout

```text
Personal-Expense-Budget-System/
  START-HERE.md
  README.md
  Dockerfile                     Builds React + Java together in the cloud
  compose.yaml                   Optional local development only
  .github/workflows/verify.yml    Online build, MySQL and browser verification
  backend/
    pom.xml
    src/main/java/com/college/budget/
      api/                       HTTP endpoints and safe error responses
      config/                    Authentication, CSRF and JSON money formatting
      core/                      Money, calendar and CSV rules
      model/                     Validated request and response records
      repository/                Named-parameter SQL helper
      service/                   Business rules and owned database queries
    src/main/resources/db/migration/
    src/test/
  frontend/
    src/components/              Reusable forms, dialogs and charts
    src/pages/                   Seven working application screens
    src/lib/                     API client, data loading and formatting
    tests/                       Dependency-free utility tests
    vercel.json                  Replace the backend-domain placeholder
  database/                      Schema reference and explanation
  postman/                       Collection and environment templates
  e2e/                           Browser tests for the real application
  docs/                          Requirements, architecture, APIs, setup and demo
  verification/                  Actual creation-time test evidence
```

## Important limits

This package has not been live-deployed or fully integration-tested in the creation environment. Source code and authored tests are not evidence of a passing full-stack run. See **verification/TEST-RESULTS.md**.

This is a single-currency, single-app-instance college project, not banking software. There are no bank connections, payments, email delivery, password reset emails, automatic forecasts, multiple currencies, administrator console or receipt OCR. No empty buttons for these features are included.

Sessions and the basic login rate limiter are in application memory. A restart signs users out, but MySQL records remain when its volume is retained. Before real public production use, complete a security review, verified backups, dependency review, operational monitoring and a shared session/rate-limit design for multiple instances.
