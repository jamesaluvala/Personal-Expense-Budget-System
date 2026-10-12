-- MySQL 8.4. Flyway applies this migration once, then records its checksum.
-- No users, passwords, income, expenses or budgets are seeded here.
CREATE TABLE app_users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(80) NOT NULL,
    email VARCHAR(190) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(60) NOT NULL,
    kind VARCHAR(7) NOT NULL,
    CONSTRAINT ck_category_kind CHECK (kind IN ('INCOME', 'EXPENSE')),
    CONSTRAINT uq_category_name UNIQUE (user_id, name, kind),
    CONSTRAINT uq_category_owner_kind UNIQUE (id, user_id, kind),
    CONSTRAINT fk_category_user FOREIGN KEY (user_id) REFERENCES app_users(id)
);
CREATE TABLE income (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    category_kind VARCHAR(7) NOT NULL DEFAULT 'INCOME',
    amount DECIMAL(14,2) NOT NULL,
    transaction_date DATE NOT NULL,
    description VARCHAR(250) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_income_amount CHECK (amount > 0),
    CONSTRAINT ck_income_kind CHECK (category_kind = 'INCOME'),
    CONSTRAINT fk_income_user FOREIGN KEY (user_id) REFERENCES app_users(id),
    CONSTRAINT fk_income_category FOREIGN KEY (category_id, user_id, category_kind)
        REFERENCES categories(id, user_id, kind)
);
CREATE INDEX ix_income_user_date ON income(user_id, transaction_date);
CREATE TABLE expenses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    category_kind VARCHAR(7) NOT NULL DEFAULT 'EXPENSE',
    amount DECIMAL(14,2) NOT NULL,
    transaction_date DATE NOT NULL,
    description VARCHAR(250) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_expense_amount CHECK (amount > 0),
    CONSTRAINT ck_expense_kind CHECK (category_kind = 'EXPENSE'),
    CONSTRAINT fk_expense_user FOREIGN KEY (user_id) REFERENCES app_users(id),
    CONSTRAINT fk_expense_category FOREIGN KEY (category_id, user_id, category_kind)
        REFERENCES categories(id, user_id, kind)
);
CREATE INDEX ix_expense_user_date ON expenses(user_id, transaction_date);
CREATE TABLE budgets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    month_start DATE NOT NULL,
    amount DECIMAL(14,2) NOT NULL,
    CONSTRAINT uq_budget_month UNIQUE (user_id, month_start),
    CONSTRAINT ck_budget_amount CHECK (amount > 0),
    CONSTRAINT fk_budget_user FOREIGN KEY (user_id) REFERENCES app_users(id)
);
CREATE TABLE savings_goals (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(80) NOT NULL,
    target_amount DECIMAL(14,2) NOT NULL,
    target_date DATE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_goal_amount CHECK (target_amount > 0),
    CONSTRAINT uq_goal_owner UNIQUE (id, user_id),
    CONSTRAINT fk_goal_user FOREIGN KEY (user_id) REFERENCES app_users(id)
);
CREATE TABLE goal_contributions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    goal_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    amount DECIMAL(14,2) NOT NULL,
    contribution_date DATE NOT NULL,
    CONSTRAINT ck_contribution_amount CHECK (amount > 0),
    CONSTRAINT fk_contribution_goal FOREIGN KEY (goal_id, user_id)
        REFERENCES savings_goals(id, user_id) ON DELETE CASCADE
);
CREATE INDEX ix_contribution_user ON goal_contributions(user_id);
-- One read-only transaction history; each entry still lives in its own source table.
CREATE VIEW financial_transactions AS
SELECT id, user_id, category_id, amount, transaction_date, description, 'INCOME' AS kind FROM income
UNION ALL
SELECT id, user_id, category_id, amount, transaction_date, description, 'EXPENSE' AS kind FROM expenses;
