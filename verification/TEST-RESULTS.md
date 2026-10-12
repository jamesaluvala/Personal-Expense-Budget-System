# Creation-time verification results

Prepared: 12 October 2026.

## Checks actually executed

| Check | Result | Evidence |
|---|---|---|
| Frontend utility tests using Node's built-in test runner | 13 tests passed, 0 failed | frontend-unit-tests.log |
| Java finance-rule checks, compiled and executed with JDK 21 | 22 checks passed | java-core-checks.log |
| Java source syntax parse | 16 main/test source files parsed | syntax-checks.log |
| JavaScript / JSX syntax and relative import-path checks | 19 files checked | syntax-checks.log |
| CSS syntax | 1 stylesheet parsed | syntax-checks.log |
| Postman embedded JavaScript syntax | 31 scripts parsed | syntax-checks.log |
| Configuration structure | JSON, YAML and Maven XML parsed | packaging-checks.log |
| Schema consistency | Reference SQL matches Flyway migration byte for byte | packaging-checks.log |
| ZIP integrity | Every archived file passed the ZIP CRC check | Verified by the packaging process |

The Java checks execute the real dependency-free FinanceRules class. Frontend utility tests execute the real formatting/validation helper functions. These are not a substitute for a dependency-resolved application build. Syntax parsing does not type-check external APIs or render the UI.

## Not executed / not verified in this environment

- npm dependency installation or a Vite production build.
- Maven dependency resolution, full Java compilation, JUnit or Spring startup.
- The Spring integration tests against H2 or MySQL.
- End-to-end browser tests against the real application.
- Postman requests against a live endpoint.
- Cloud Docker build, Railway deployment or Vercel deployment.
- MySQL data persistence after service restart, backups or restore.
- Browser layout, mobile interaction and Print / Save as PDF.

The creation environment had JDK 21 and Node 22.16 available, but not Maven, MySQL or Docker. Dependency-download attempts could not resolve the external package hosts. Dependencies are therefore not bundled, and no npm lockfile or built JAR/dist is presented as if it had been generated.

## How to finish verification online

The source contains `.github/workflows/verify.yml`, which builds React, runs Java tests first with H2 and then with MySQL 8.4, starts the actual application, and runs Playwright. Uploading this file is not evidence of a passing run: inspect the actual Actions result and fix any failures.

The root Dockerfile separately runs the frontend utility suite and Java/H2 suite during a real cloud build. Use the full hosted checklist in `docs/DEMO-CHECKLIST.md` before a faculty demonstration. No claim of a production-ready security audit or a working public URL is made by this package.
