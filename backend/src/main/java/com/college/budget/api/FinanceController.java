package com.college.budget.api;
import com.college.budget.core.FinanceRules;
import com.college.budget.model.Models.*;
import com.college.budget.repository.Db;
import com.college.budget.service.*;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import java.time.*;
import java.util.*;

@RestController
@RequestMapping("/api")
public class FinanceController {
    private final UserService users;
    private final LedgerService ledger;
    private final PlanningService planning;
    private final ReportService reports;
    private final Db db;
    private final Clock clock;
    private final String currency;
    public FinanceController(UserService users, LedgerService ledger, PlanningService planning, ReportService reports,
            Db db, Clock clock,@Value("${app.currency}") String currency) {
        this.users=users; this.ledger=ledger; this.planning=planning; this.reports=reports; this.db=db; this.clock=clock;
        this.currency=Currency.getInstance(currency).getCurrencyCode();
    }
    private long uid(Authentication auth) { return users.profile(auth).id(); }
    private String month(String value) { return value==null?YearMonth.now(clock).toString():FinanceRules.month(value).toString(); }
    @GetMapping("/health") public Map<String,String> health() {
        db.count("SELECT 1",Map.of()); return Map.of("status","UP");
    }
    @GetMapping("/meta") public Map<String,String> metadata() {
        return Map.of("currency",currency,"today",LocalDate.now(clock).toString(),"timeZone",clock.getZone().getId());
    }
    @GetMapping("/auth/csrf") public CsrfToken csrf(CsrfToken token) { return token; }
    @PostMapping("/auth/register") public ResponseEntity<Profile> register(@Valid @RequestBody Register input) {
        return ResponseEntity.status(201).body(users.register(input));
    }
    @GetMapping("/auth/me") public Profile me(Authentication auth) { return users.profile(auth); }
    @GetMapping("/categories") public List<Category> categories(Authentication auth) { return ledger.categories(uid(auth)); }
    @PostMapping("/categories") @ResponseStatus(HttpStatus.CREATED)
    public Category createCategory(Authentication auth,@Valid @RequestBody CategoryInput input) { return ledger.saveCategory(uid(auth),null,input); }
    @PutMapping("/categories/{id}") public Category editCategory(Authentication auth,@PathVariable long id,@Valid @RequestBody CategoryInput input) {
        return ledger.saveCategory(uid(auth),id,input);
    }
    @DeleteMapping("/categories/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCategory(Authentication auth,@PathVariable long id) { ledger.deleteCategory(uid(auth),id); }
    @GetMapping("/transactions") public EntryPage transactions(Authentication auth,
        @RequestParam(required=false) Kind type,@RequestParam(required=false) Long categoryId,
        @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to,
        @RequestParam(defaultValue="") String search,@RequestParam(defaultValue="date_desc") String sort,
        @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) {
        return ledger.entries(uid(auth),type,categoryId,from,to,search,sort,page,size);
    }
    @GetMapping("/transactions/{type}/{id}") public Entry transaction(Authentication auth,@PathVariable Kind type,@PathVariable long id) {
        return ledger.entry(uid(auth),type,id);
    }
    @PostMapping("/transactions") @ResponseStatus(HttpStatus.CREATED)
    public Entry createEntry(Authentication auth,@Valid @RequestBody EntryInput input) { return ledger.saveEntry(uid(auth),null,null,input); }
    @PutMapping("/transactions/{type}/{id}") public Entry editEntry(Authentication auth,@PathVariable Kind type,@PathVariable long id,@Valid @RequestBody EntryInput input) {
        return ledger.saveEntry(uid(auth),type,id,input);
    }
    @DeleteMapping("/transactions/{type}/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteEntry(Authentication auth,@PathVariable Kind type,@PathVariable long id) { ledger.deleteEntry(uid(auth),type,id); }
    @GetMapping("/income") public EntryPage income(Authentication auth,@RequestParam(defaultValue="0") int page) {
        return ledger.entries(uid(auth),Kind.INCOME,null,null,null,"","date_desc",page,20);
    }
    @GetMapping("/expenses") public EntryPage expenses(Authentication auth,@RequestParam(defaultValue="0") int page) {
        return ledger.entries(uid(auth),Kind.EXPENSE,null,null,null,"","date_desc",page,20);
    }
    @PostMapping("/income") @ResponseStatus(HttpStatus.CREATED)
    public Entry addIncome(Authentication auth,@Valid @RequestBody EntryInput input) {
        if(input.type()!=Kind.INCOME) throw new IllegalArgumentException("Use INCOME type for this endpoint.");
        return ledger.saveEntry(uid(auth),null,null,input);
    }
    @PostMapping("/expenses") @ResponseStatus(HttpStatus.CREATED)
    public Entry addExpense(Authentication auth,@Valid @RequestBody EntryInput input) {
        if(input.type()!=Kind.EXPENSE) throw new IllegalArgumentException("Use EXPENSE type for this endpoint.");
        return ledger.saveEntry(uid(auth),null,null,input);
    }
    @GetMapping("/budgets") public Budget budget(Authentication auth,@RequestParam(required=false) String month) {
        return planning.budget(uid(auth),FinanceRules.month(month(month)));
    }
    @PutMapping("/budgets") public Budget saveBudget(Authentication auth,@Valid @RequestBody BudgetInput input) {
        return planning.saveBudget(uid(auth),input);
    }
    @DeleteMapping("/budgets/{month}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBudget(Authentication auth,@PathVariable String month) { planning.deleteBudget(uid(auth),month); }
    @GetMapping("/goals") public List<Goal> goals(Authentication auth) { return planning.goals(uid(auth)); }
    @PostMapping("/goals") @ResponseStatus(HttpStatus.CREATED)
    public Goal addGoal(Authentication auth,@Valid @RequestBody GoalInput input) { return planning.saveGoal(uid(auth),null,input); }
    @PutMapping("/goals/{id}") public Goal editGoal(Authentication auth,@PathVariable long id,@Valid @RequestBody GoalInput input) {
        return planning.saveGoal(uid(auth),id,input);
    }
    @DeleteMapping("/goals/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteGoal(Authentication auth,@PathVariable long id) { planning.deleteGoal(uid(auth),id); }
    @GetMapping("/goals/{id}/contributions") public List<Contribution> contributions(Authentication auth,@PathVariable long id) {
        return planning.contributions(uid(auth),id);
    }
    @PostMapping("/goals/{id}/contributions") @ResponseStatus(HttpStatus.CREATED)
    public Goal contribute(Authentication auth,@PathVariable long id,@Valid @RequestBody ContributionInput input) {
        return planning.contribute(uid(auth),id,input);
    }
    @DeleteMapping("/goals/{goalId}/contributions/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void release(Authentication auth,@PathVariable long goalId,@PathVariable long id) { planning.deleteContribution(uid(auth),goalId,id); }
    @GetMapping("/dashboard") public Summary dashboard(Authentication auth,@RequestParam(required=false) String month) {
        return reports.summary(uid(auth),month(month));
    }
    @GetMapping("/reports") public Report report(Authentication auth,@RequestParam(required=false) String month) { return reports.report(uid(auth),month(month)); }
    @GetMapping(value="/reports/export.csv",produces="text/csv;charset=UTF-8")
    public ResponseEntity<String> csv(Authentication auth,@RequestParam(required=false) String month) {
        String selected=month(month);
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\"financial-report-"+selected+".csv\"")
            .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8")).body(reports.csv(uid(auth),selected));
    }
}
