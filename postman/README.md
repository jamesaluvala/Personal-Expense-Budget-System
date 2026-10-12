# Postman API tests

Import `Expense-Budget.postman_collection.json` and `Hosted.postman_environment.json` into Postman. Set `baseUrl` to your actual public HTTPS website origin, **without** a trailing `/api`. Choose the environment and keep the cookie jar enabled. The Postman web app with an available Cloud Agent can call a publicly hosted HTTPS application without a locally running backend; agent availability depends on your Postman account.

Run the collection in order. The collection-level pre-request script gets a fresh CSRF token for each write. Login uses form encoding. Test dates come from `/api/meta`, so they are not hardcoded to the ZIP creation date.

The collection registers a unique test account and actually saves clearly marked TEST records. It checks empty-start behavior, money totals, budget recalculation, filtering, negative amounts, goal allocations, exports and logout. It deletes the created transactions/goal/budget at the end. The test account and starter category labels remain; there is no account-deletion API in this scope. Use a disposable test database or accept this explicit test account remaining.

No administrator/default credentials are supplied. A random test password is generated at run time. Do not export populated environments or collection variables containing credentials into a public repository. Run the collection only against a database you are authorized to test.

Cross-user ID rejection, month boundaries and CSRF rejection are additionally covered by the authored Java integration suite and manual demo checklist. This collection was generated and its JSON parsed, but it has **not** been executed against a hosted API in the creation environment.
