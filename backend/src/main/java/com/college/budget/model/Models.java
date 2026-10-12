package com.college.budget.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Request/response records keep database credentials and password hashes out of API responses. */
public final class Models {
    private Models() {}
    public enum Kind { INCOME, EXPENSE }
    public record User(long id, String name, String email, @JsonIgnore String passwordHash) {}
    public record Profile(long id, String name, String email) {}
    public record Register(@NotBlank @Size(max=80) String name,
                           @NotBlank @Email @Size(max=190) String email,
                           @NotNull @Size(min=12, max=72) String password) {}
    public record Category(long id, String name, Kind type) {}
    public record CategoryInput(@NotBlank @Size(max=60) String name, @NotNull Kind type) {}
    public record Entry(long id, Kind type, long categoryId, String categoryName,
                        BigDecimal amount, LocalDate date, String description) {}
    public record EntryInput(@NotNull Kind type, @NotNull @Positive Long categoryId,
                             @NotNull BigDecimal amount, @NotNull LocalDate date,
                             @NotBlank @Size(max=250) String description) {}
    public record EntryPage(List<Entry> items, long total, int page, int size) {}
    public record Budget(long id, String month, BigDecimal amount, BigDecimal spent,
                         BigDecimal remaining, BigDecimal usedPercent, String status) {}
    public record BudgetInput(@NotBlank String month, @NotNull BigDecimal amount) {}
    public record Goal(long id, String title, BigDecimal targetAmount, BigDecimal savedAmount,
                       LocalDate targetDate, BigDecimal progress, String status) {}
    public record GoalInput(@NotBlank @Size(max=80) String title, @NotNull BigDecimal targetAmount,
                            @NotNull LocalDate targetDate) {}
    public record Contribution(long id, BigDecimal amount, LocalDate date) {}
    public record ContributionInput(@NotNull BigDecimal amount, @NotNull LocalDate date) {}
    public record CategoryTotal(String category, BigDecimal amount) {}
    public record MonthTotal(String month, BigDecimal income, BigDecimal expenses) {}
    public record Summary(String month, BigDecimal income, BigDecimal expenses, BigDecimal netSavings,
                          BigDecimal balance, BigDecimal reservedForGoals, BigDecimal availableBalance,
                          BigDecimal savingsRate, long transactionCount, Budget budget,
                          List<CategoryTotal> categories, List<MonthTotal> trend,
                          List<Entry> recent, List<String> insights) {}
    public record Report(Summary summary, List<Entry> transactions) {}
}
