# Cloud build: both frontend and backend are compiled on the hosting provider, not your laptop.
FROM node:22-bookworm-slim AS frontend
WORKDIR /web
COPY frontend/package*.json ./
RUN npm install --no-audit --no-fund
COPY frontend/ ./
RUN npm test && npm run build

FROM maven:3.9-eclipse-temurin-21 AS backend
WORKDIR /build
COPY backend/pom.xml ./
RUN mvn -B -ntp dependency:go-offline
COPY backend/src ./src
COPY --from=frontend /web/dist ./src/main/resources/static
# Integration tests here use H2 in MySQL mode. CI separately runs them against MySQL 8.4.
RUN mvn -B -ntp verify

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN groupadd --system ledger && useradd --system --gid ledger --home-dir /app ledger
COPY --from=backend --chown=ledger:ledger /build/target/expense-budget.jar /app/app.jar
USER ledger
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
