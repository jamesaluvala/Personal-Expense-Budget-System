# Eclipse and local development (optional, not needed for cloud deployment)

The supplied PDF specifies Eclipse for Spring Boot development. The backend is a standard Maven Java project that can be imported into Eclipse. This package was generated outside Eclipse; it does not claim that Eclipse itself was used to execute or verify the application.

The user's current preference is cloud deployment without running the application on their computer. Follow CLOUD-DEPLOYMENT.md instead unless local development is specifically chosen later.

For a developer who already has the tools: use Java JDK 21, Eclipse with Maven integration, Node 22.12+ (22.x recommended), Maven 3.9 and MySQL 8.4. In Eclipse choose File > Import > Maven > Existing Maven Projects and select the exact `backend` folder containing `pom.xml`. Set the project's Java execution environment to JavaSE-21. Configure DB_URL, DB_USER, DB_PASSWORD, COOKIE_SECURE=false, APP_CURRENCY and APP_TIME_ZONE in the Java application's Run Configuration environment, then run `com.college.budget.BudgetApplication`.

The backend `.env.example` is documentation: Java does not automatically load it. Supply values through the IDE or hosting environment. Use Secure cookies for hosted HTTPS, and false only for deliberate local HTTP testing.

The frontend's development proxy is in `frontend/vite.config.js` and targets localhost:8080. In the `frontend` directory, `npm install` followed by `npm run dev` starts Vite; opening `index.html` as a local file does not run React.

An optional Docker Compose setup is included for developers who already use Docker. It requires explicit strong passwords in a copied `.env`. The app is bound to localhost; MySQL is not published on a host port. `docker compose up --build` builds both applications. `docker compose down` stops containers while retaining the named database volume; adding `-v` deletes that volume and its records and must not be done as a casual troubleshooting step.

Do not replace a user's existing project folder or run destructive database commands without first backing it up and explaining the change.
