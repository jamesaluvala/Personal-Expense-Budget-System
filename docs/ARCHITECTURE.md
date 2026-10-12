# Architecture and calculation rules

## Request flow

The browser runs React. React calls same-origin `/api` URLs. Spring Security validates the login session and, for writes, a CSRF token. Controllers validate request fields and obtain the current user's ID from the authenticated session. Services apply finance rules. Spring JDBC uses named parameters to read/write MySQL. Responses return only that user's data.

For the simplest hosted demonstration, the Docker build copies React's compiled output into Spring Boot's static resources. A single HTTPS domain serves the website and API. In the optional split setup, Vercel serves React and rewrites `/api/*` to the Railway application, preserving the same-origin browser contract. No database password or API secret is sent to React.

## Data and exact money

Money is stored in MySQL `DECIMAL(14,2)` and calculated in Java `BigDecimal`. API monetary values are JSON decimal strings such as `"250.05"`. The browser validates decimal text without float arithmetic. Visual chart dimensions and formatted display use numeric conversions; authoritative totals always come from SQL/Java.

Monthly totals use `date >= first day` and `date < first day of next month`. Leap days are therefore included correctly. Income/expense dates are `LocalDate` and MySQL `DATE`. The configured application clock controls what counts as today. The browser obtains today from `/api/meta`.

- Monthly income = income dated within the selected month.
- Monthly expenses = expenses dated within the selected month.
- Monthly net savings = monthly income minus monthly expenses. It may be negative.
- Recorded balance = all-time income minus all-time expenses. This is not a bank balance and there is no separate opening-balance field.
- Reserved for goals = all current contribution records.
- Available balance = recorded balance minus reservations.
- Savings rate = monthly net savings / monthly income x 100; displayed as unavailable when income is zero.
- Budget spent = monthly expenses. Contributions and the budget limit itself are not expenses.
- Budget remaining = limit minus spent. Negative remaining is retained rather than hidden.

Reports label monthly numbers separately from all-time/current balance numbers. Goal reservations are current, not a reconstructed historical balance at month end.

## Budget and goal consistency

Budgets contain a limit, not a stale stored spending total. Spending is re-queried when the dashboard or budget is loaded. Add, edit, delete and date changes invalidate the frontend data, including both affected months on their next load.

Budget warnings: below 80% ON_TRACK, 80% to below 100% NEAR_LIMIT, exactly 100% AT_LIMIT, above 100% EXCEEDED. Expenses are not blocked just because the budget is exceeded.

A goal contribution cannot exceed its remaining target or the user's currently unreserved recorded balance. User writes lock the user row inside a database transaction to serialize conflicting contributions. Editing/deleting transactions may legitimately reduce recorded balance below existing reservations. Such corrections remain allowed; an insight asks the user to review/release reservations. Goal deletion releases reservations but does not modify income or expenses.

## Security boundaries

Password hashing uses BCrypt at cost 12. Registration enforces a minimum of 12 characters and a maximum of 72 UTF-8 bytes. Password hashes are never returned by public profile responses. Use a unique password; email verification and recovery are not included.

Spring Security manages session fixation protection, authentication persistence and logout. The cookie is HttpOnly and SameSite=Lax, and Secure defaults to true. CSRF remains enabled for registration, login, all writes and logout. React retrieves a fresh token before mutations, including after login rotation. Financial/API responses use Cache-Control no-store.

Each row lookup, update and delete is constrained by the session user's ID. The client cannot select another user by adding a user ID. Foreign category IDs are rejected. Composite foreign keys also enforce category ownership and type. SQL values are bound parameters; sort and table names come from fixed allowlists.

The basic in-memory limiter restricts repeated login and registration attempts. It is not a distributed anti-abuse system. Keep one application replica for this demonstration. Public production needs shared sessions, robust edge limits, audited TLS/proxy settings, verified backups and full security testing.

## Repository structure

Controllers handle HTTP; request records define validation; services own finance logic; `Db` centralizes SQL execution; React pages focus on screens. No secret is stored in `VITE_*` variables or frontend source. Environment variables configure the server and are not committed.
