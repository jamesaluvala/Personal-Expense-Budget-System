# Testing and verification

Read `verification/TEST-RESULTS.md` for what actually ran while this ZIP was created. Do not label authored tests as passing until they have run.

## Included layers

- `frontend/tests/format.test.js`: Node's built-in runner; exact decimal validation, amount bounds, month/leap-day boundaries, percentages and safe error formatting.
- `verification/CoreChecks.java`: dependency-free Java checks for amount precision/limits, date/month validation, budget thresholds and CSV escaping.
- `backend/src/test/java/com/college/budget/FinanceRulesTest.java`: JUnit versions of finance rule tests.
- `backend/src/test/java/com/college/budget/ApplicationIntegrationTest.java`: full Spring Security/MVC/database tests for registration, passwords, logout, CSRF, isolation, validation, persistence across requests, sums, month moves, leap days, categories, search, budgets, goals and CSV.
- `postman/`: sequential tests of the actual hosted REST API. This creates real, explicitly marked test records in a new test account.
- `e2e/tests/application.spec.js`: browser flows against the real backend, including creation/editing, budgets, goals, CSV, isolation, mobile-size navigation and logout. No mocked financial API.
- `docs/DEMO-CHECKLIST.md`: user acceptance tests, including app restart persistence and print-to-PDF inspection.

## Run online, not on your computer

Upload the source to GitHub and enable the workflow in `.github/workflows/verify.yml`. It installs dependencies, builds React, runs the backend with H2, repeats against a disposable MySQL 8.4 service, starts the real JAR and runs Chromium tests. Read the Actions output. It has not been run on your behalf.

The CI database/passwords in that YAML are disposable test-only values, not credentials for a live database. Never point TEST_DB_URL at a personal/live database. The test suite creates accounts and financial records. Browser and Java test accounts remain in the disposable CI database until the job ends.

The Docker cloud build also runs frontend utility tests and the backend H2 tests before producing the application image. MySQL-specific verification is handled by CI and your hosted acceptance check, not by pretending H2 is production.

After the first online dependency resolution, preserve the generated frontend/e2e package-lock.json files in the repository and switch installation commands to npm ci for repeatability. Review relevant dependency advisories before public deployment.

## Optional developer commands

From the project root (only for a developer who chooses to run locally):

```sh
cd frontend
npm test
npm install
npm run build
cd ../backend
mvn verify
```

The Java integration test profile uses H2 unless TEST_DB_URL, TEST_DB_USER and TEST_DB_PASSWORD are provided. Running unit utilities alone does not prove that React builds, Spring starts or MySQL persists data.

## Required acceptance gate

A usable faculty demonstration requires an actual successful build, hosted health check, two-account isolation check, monetary/date edge checks, CSV/print inspection, and persistence after application restart with the MySQL volume retained. Record failures and fix them before calling the project complete.
