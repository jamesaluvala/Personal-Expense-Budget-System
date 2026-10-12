package com.college.budget;
import com.college.budget.core.FinanceRules;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;
class FinanceRulesTest {
    @Test void exactMoney() {
        assertEquals(new BigDecimal("0.10"),FinanceRules.amount(new BigDecimal("0.1")));
        assertThrows(IllegalArgumentException.class,()->FinanceRules.amount(new BigDecimal("1.001")));
        assertThrows(IllegalArgumentException.class,()->FinanceRules.amount(BigDecimal.ZERO));
    }
    @Test void budgetBoundaries() {
        BigDecimal limit=new BigDecimal("100.00");
        assertEquals("ON_TRACK",FinanceRules.budgetStatus(new BigDecimal("79.99"),limit));
        assertEquals("NEAR_LIMIT",FinanceRules.budgetStatus(new BigDecimal("80.00"),limit));
        assertEquals("AT_LIMIT",FinanceRules.budgetStatus(new BigDecimal("100.00"),limit));
        assertEquals("EXCEEDED",FinanceRules.budgetStatus(new BigDecimal("100.01"),limit));
    }
    @Test void monthAndLeapDay() {
        assertEquals(LocalDate.of(2024,2,29),FinanceRules.month("2024-02").atEndOfMonth());
        assertThrows(IllegalArgumentException.class,()->FinanceRules.month("2026-13"));
    }
    @Test void percentagesAndCsv() {
        assertNull(FinanceRules.percentage(BigDecimal.ONE,BigDecimal.ZERO));
        assertEquals(new BigDecimal("33.33"),FinanceRules.percentage(BigDecimal.ONE,new BigDecimal("3")));
        assertEquals("\"'=1+1\"",FinanceRules.csvText("=1+1"));
        assertEquals("\"a,\"\"b\"\"\"",FinanceRules.csvText("a,\"b\""));
    }
}
