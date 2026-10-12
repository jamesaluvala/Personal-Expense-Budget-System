# Start here - your project ZIP

This is the **source-code package**, not an already deployed website. It contains the React frontend, Java Spring Boot backend, MySQL schema, tests, and deployment instructions. There are no default login credentials and no preloaded financial transactions.

## Use it online without installing Java, Node, MySQL or Eclipse on your Mac

Open **docs/CLOUD-DEPLOYMENT.md**. The recommended first deployment uses one Railway application service plus a MySQL database. The application's Dockerfile builds React and Spring Boot in the cloud and serves them together. After that works, the guide explains the optional Vercel frontend.

You need your own hosting accounts and a GitHub repository. Extracting/uploading the source is a file operation, not running the app on your computer. No hosting accounts, paid resources or live website have been created by this ZIP. Review provider charges before deploying.

**Do not upload only the ZIP file to Vercel.** The extracted frontend can be built there, but the Java backend and MySQL must also be hosted. Uploading only React will show a backend-connection error rather than invented balances.

## Before your faculty demonstration

Read **verification/TEST-RESULTS.md**. Dependency-free Java and JavaScript checks were run; a full dependency-resolved application build, MySQL integration run, browser run and cloud deployment were NOT possible in the creation environment. The package includes those tests and a GitHub Actions workflow to run them online. Complete **docs/DEMO-CHECKLIST.md** before declaring the project finished.

The next action is to read the cloud guide, not install software on your Mac.
