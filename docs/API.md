# API guide

All routes below start with `/api`. Use the same HTTPS origin as the website. Financial routes require an authenticated session. Monetary fields are decimal strings in JSON, for example `"250.05"`. Request bodies also use decimal strings. Dates are `yyyy-MM-dd`; months are `yyyy-MM`.

## Authentication sequence

GET `/auth/csrf` obtains `token`, `headerName` and `parameterName` and creates/uses a session. Include the supplied header name/token for every POST, PUT or DELETE, including registration, login and logout. Keep the cookie jar enabled. Get a fresh token after login/logout session changes; the included clients do this automatically.

POST `/auth/register` accepts JSON:
```json
{"name":"Demo user","email":"your-unique-email@example.com","password":"use-your-own-long-password"}
```
It returns 201 and a profile. Registration does not independently log in; the frontend then calls login.

POST `/auth/login` is **application/x-www-form-urlencoded**, not JSON, with `email` and `password`. Successful login returns a profile and the HttpOnly session cookie. POST `/auth/logout` returns 204. GET `/auth/me` returns the profile, or 401 when signed out. Do not store cookie values or passwords in frontend localStorage.

## Routes

| Method | Path | Purpose |
|---|---|---|
| GET | /health | Public database-aware health status |
| GET | /meta | Public currency, current date and time zone |
| GET | /categories | List owned categories |
| POST | /categories | Create category: `name`, `type` |
| PUT | /categories/{id} | Rename owned category; type remains unchanged |
| DELETE | /categories/{id} | Delete unused owned category |
| GET | /transactions | Filtered, paginated owned history |
| POST | /transactions | Create income or expense |
| GET | /transactions/{type}/{id} | Read one owned transaction |
| PUT | /transactions/{type}/{id} | Edit owned transaction; immutable type |
| DELETE | /transactions/{type}/{id} | Delete owned transaction |
| GET/POST | /income | Convenience INCOME list/create routes |
| GET/POST | /expenses | Convenience EXPENSE list/create routes |
| GET | /budgets?month=2026-10 | Budget for month, or no body when none is set |
| PUT | /budgets | Create/update month limit: `month`, `amount` |
| DELETE | /budgets/{month} | Remove month's budget without deleting transactions |
| GET | /goals | List owned savings goals and progress |
| POST | /goals | Create goal: `title`, `targetAmount`, `targetDate` |
| PUT | /goals/{id} | Update owned goal |
| DELETE | /goals/{id} | Delete goal and release its reservations |
| GET | /goals/{id}/contributions | Owned goal contribution history |
| POST | /goals/{id}/contributions | Reserve funds: `amount`, `date` |
| DELETE | /goals/{goalId}/contributions/{id} | Release contribution |
| GET | /dashboard?month=2026-10 | Monthly summary, current/all-time balances and charts |
| GET | /reports?month=2026-10 | Summary plus all transactions in the month |
| GET | /reports/export.csv?month=2026-10 | CSV attachment with monthly entries and totals |

Transaction payload example (replace categoryId with an owned category of the same type):
```json
{"type":"EXPENSE","categoryId":1,"amount":"250.50","date":"2026-10-01","description":"Food purchase"}
```

`type` is uppercase INCOME or EXPENSE. Income and expense IDs are separate sequences. The type/id pair identifies an entry. `/income` and `/expenses` use the same full payload with a matching type.

History query parameters: optional `type`, `categoryId`, `from`, `to`; `search` (up to 100 characters); `sort` = date_desc, date_asc, amount_desc, amount_asc, description_asc; `page` starts at 0; `size` is 1-100, default 20. From/to dates are inclusive. Response: `items`, `total`, `page`, `size`. Encode query values normally; literal `%` and `_` are not wildcard search controls.

## Failure responses

400 invalid amount/date/fields; 401 login required or credentials incorrect; 403 missing/invalid security token; 404 record not found or owned by another user; 409 duplicate or business-rule conflict; 429 too many attempts; 500 unexpected server error without a stack trace. Empty bodies are expected for successful 204 deletion/logout.

Validation responses include `message` and an `errors` object keyed by field. No API allows a client-supplied `userId` to change ownership. Extra JSON fields are rejected.

See `postman/README.md` for a ready-to-import collection. Its financial examples create clearly marked test records in a unique test account, not fabricated production dashboard data.
