package com.college.budget.core;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;

/** Pure Java business rules. Money is never calculated using floating-point arithmetic. */
public final class FinanceRules {
    public static final BigDecimal ZERO = new BigDecimal("0.00");
    private static final BigDecimal MAX = new BigDecimal("999999999999.99");
    private FinanceRules() {}

    public static BigDecimal amount(BigDecimal value) {
        if (value == null || value.signum() <= 0 || value.compareTo(MAX) > 0)
            throw new IllegalArgumentException("Amount must be between 0.01 and 999999999999.99.");
        try { return value.setScale(2, RoundingMode.UNNECESSARY); }
        catch (ArithmeticException ex) { throw new IllegalArgumentException("Use at most two decimal places."); }
    }
    public static YearMonth month(String value) {
        try {
            if (value == null || !value.matches("[0-9]{4}-[0-9]{2}")) throw new IllegalArgumentException();
            YearMonth month = YearMonth.parse(value);
            if (month.getYear() < 2000 || month.getYear() > 2100) throw new IllegalArgumentException();
            return month;
        } catch (RuntimeException ex) { throw new IllegalArgumentException("Month must be YYYY-MM, between 2000 and 2100."); }
    }
    public static LocalDate pastDate(LocalDate date, Clock clock) {
        if (date == null || date.isBefore(LocalDate.of(2000, 1, 1)) || date.isAfter(LocalDate.now(clock)))
            throw new IllegalArgumentException("Transaction date must be between 2000-01-01 and today.");
        return date;
    }
    public static BigDecimal percentage(BigDecimal numerator, BigDecimal denominator) {
        return denominator.signum() <= 0 ? null : numerator.multiply(new BigDecimal("100"))
                .divide(denominator, 2, RoundingMode.HALF_UP);
    }
    public static String budgetStatus(BigDecimal spent, BigDecimal limit) {
        if (spent.compareTo(limit) > 0) return "EXCEEDED";
        if (spent.compareTo(limit) == 0) return "AT_LIMIT";
        return spent.compareTo(limit.multiply(new BigDecimal("0.80"))) >= 0 ? "NEAR_LIMIT" : "ON_TRACK";
    }
    /** Quote every cell; neutralize spreadsheet formula prefixes in user-controlled text. */
    public static String csvText(String value) {
        String safe = value == null ? "" : value;
        String stripped = safe.stripLeading();
        if (!stripped.isEmpty() && "=+-@".indexOf(stripped.charAt(0)) >= 0) safe = "'" + safe;
        return "\"" + safe.replace("\"", "\"\"") + "\"";
    }
}
