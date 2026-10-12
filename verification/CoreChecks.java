import com.college.budget.core.FinanceRules;
import java.math.BigDecimal;
import java.time.*;
/** Dependency-free checks that can run with Java 21 even when Maven downloads are unavailable. */
public class CoreChecks {
  static int checks=0;
  static void check(boolean result,String message) { checks++; if(!result) throw new AssertionError(message); }
  static void rejects(Runnable action) { boolean rejected=false; try { action.run(); } catch(IllegalArgumentException ex) { rejected=true; } check(rejected,"Invalid value was accepted"); }
  public static void main(String[] args) {
    check(FinanceRules.amount(new BigDecimal("0.1")).toPlainString().equals("0.10"),"Decimal normalization");
    for(String value:new String[]{"0","-1","0.001","1000000000000.00"}) rejects(()->FinanceRules.amount(new BigDecimal(value)));
    check(FinanceRules.amount(new BigDecimal("999999999999.99")).toPlainString().equals("999999999999.99"),"Maximum exact amount");
    check(FinanceRules.month("2024-02").atEndOfMonth().equals(LocalDate.of(2024,2,29)),"Leap day");
    for(String value:new String[]{"2026-13","2026-1","1999-01","2101-01"}) rejects(()->FinanceRules.month(value));
    BigDecimal hundred=new BigDecimal("100");
    check(FinanceRules.budgetStatus(new BigDecimal("79.99"),hundred).equals("ON_TRACK"),"79.99 threshold");
    check(FinanceRules.budgetStatus(new BigDecimal("80"),hundred).equals("NEAR_LIMIT"),"80 percent threshold");
    check(FinanceRules.budgetStatus(hundred,hundred).equals("AT_LIMIT"),"Exact budget");
    check(FinanceRules.budgetStatus(new BigDecimal("100.01"),hundred).equals("EXCEEDED"),"Exceeded budget");
    check(FinanceRules.percentage(BigDecimal.ONE,BigDecimal.ZERO)==null,"No division by zero");
    check(FinanceRules.percentage(BigDecimal.ONE,new BigDecimal("3")).toPlainString().equals("33.33"),"Percentage rounding");
    check(FinanceRules.csvText("=1+1").startsWith("\"'="),"CSV formula escaped");
    check(FinanceRules.csvText("a\"b").equals("\"a\"\"b\""),"CSV quote escaped");
    Clock clock=Clock.fixed(Instant.parse("2026-10-12T00:00:00Z"),ZoneOffset.UTC);
    check(FinanceRules.pastDate(LocalDate.of(2026,10,12),clock).equals(LocalDate.of(2026,10,12)),"Today accepted");
    rejects(()->FinanceRules.pastDate(LocalDate.of(2026,10,13),clock));
    rejects(()->FinanceRules.pastDate(LocalDate.of(1999,12,31),clock));
    System.out.println("PASS: "+checks+" Java money, date, budget and CSV checks.");
  }
}
