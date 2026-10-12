package com.college.budget.service;
import com.college.budget.core.FinanceRules;
import com.college.budget.model.Models.*;
import com.college.budget.repository.Db;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Service
public class ReportService {
    private final Db db;
    private final LedgerService ledger;
    private final PlanningService planning;
    public ReportService(Db db,LedgerService ledger,PlanningService planning) { this.db=db; this.ledger=ledger; this.planning=planning; }
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public Summary summary(long uid,String monthValue) {
        YearMonth month=FinanceRules.month(monthValue);
        BigDecimal income=planning.total(uid,Kind.INCOME,month), expenses=planning.total(uid,Kind.EXPENSE,month);
        BigDecimal balance=planning.balance(uid), reserved=planning.reserved(uid);
        Budget budget=planning.budget(uid,month);
        Map<String,Object> p=Map.of("uid",uid,"start",month.atDay(1),"end",month.plusMonths(1).atDay(1));
        long count=db.count("SELECT COUNT(*) FROM financial_transactions WHERE user_id=:uid AND transaction_date>=:start AND transaction_date<:end",p);
        List<CategoryTotal> categories=db.list("SELECT c.name,SUM(e.amount) AS amount FROM expenses e JOIN categories c ON c.id=e.category_id AND c.user_id=e.user_id WHERE e.user_id=:uid AND e.transaction_date>=:start AND e.transaction_date<:end GROUP BY c.id,c.name ORDER BY SUM(e.amount) DESC,c.name",p,
            (r,n)->new CategoryTotal(r.getString("name"),r.getBigDecimal("amount")));
        Map<String,MonthTotal> trend=new LinkedHashMap<>();
        for(int i=5;i>=0;i--) { String m=month.minusMonths(i).toString(); trend.put(m,new MonthTotal(m,FinanceRules.ZERO,FinanceRules.ZERO)); }
        List<MonthTotal> actual=db.list("SELECT EXTRACT(YEAR FROM transaction_date) AS yr,EXTRACT(MONTH FROM transaction_date) AS mo,SUM(CASE WHEN kind='INCOME' THEN amount ELSE 0 END) AS income,SUM(CASE WHEN kind='EXPENSE' THEN amount ELSE 0 END) AS expenses FROM financial_transactions WHERE user_id=:uid AND transaction_date>=:start AND transaction_date<:end GROUP BY EXTRACT(YEAR FROM transaction_date),EXTRACT(MONTH FROM transaction_date)",
            Map.of("uid",uid,"start",month.minusMonths(5).atDay(1),"end",month.plusMonths(1).atDay(1)),
            (r,n)->new MonthTotal(YearMonth.of(r.getInt("yr"),r.getInt("mo")).toString(),r.getBigDecimal("income"),r.getBigDecimal("expenses")));
        actual.forEach(row->trend.put(row.month(),row));
        List<String> insights=new ArrayList<>();
        if(count==0) insights.add("No transactions recorded for this month. Add your first income or expense to start.");
        if(budget==null) insights.add("No budget is set for this month.");
        else if(!budget.status().equals("ON_TRACK")) insights.add(switch(budget.status()) {
            case "EXCEEDED" -> "Your expenses exceed this month's budget.";
            case "AT_LIMIT" -> "You have reached this month's budget limit.";
            default -> "You have used at least 80% of this month's budget.";
        });
        if(expenses.compareTo(income)>0) insights.add("Expenses exceed income for the selected month.");
        if(!categories.isEmpty()) insights.add(categories.getFirst().category()+" is your largest expense category this month.");
        if(reserved.compareTo(balance)>0) insights.add("Recorded spending or income changes have left less balance than your goal reservations. Review and release contributions if necessary.");
        return new Summary(monthValue,income,expenses,income.subtract(expenses),balance,reserved,balance.subtract(reserved),
            FinanceRules.percentage(income.subtract(expenses),income),count,budget,categories,new ArrayList<>(trend.values()),ledger.monthEntries(uid,month,true),insights);
    }
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public Report report(long uid,String month) {
        return new Report(summary(uid,month),ledger.monthEntries(uid,FinanceRules.month(month),false));
    }
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public String csv(long uid,String month) {
        Report report=report(uid,month);
        StringBuilder csv=new StringBuilder("\ufeffRecord,Date,Type,Category,Description,Income,Expense,Net savings\r\n");
        for(Entry entry:report.transactions()) {
            csv.append("Transaction,").append(entry.date()).append(',').append(entry.type()).append(',')
                .append(FinanceRules.csvText(entry.categoryName())).append(',').append(FinanceRules.csvText(entry.description())).append(',')
                .append(entry.type()==Kind.INCOME?entry.amount().toPlainString():"0.00").append(',')
                .append(entry.type()==Kind.EXPENSE?entry.amount().toPlainString():"0.00").append(",\r\n");
        }
        Summary s=report.summary();
        csv.append("Monthly totals,").append(month).append(",,,,").append(s.income().toPlainString()).append(',')
            .append(s.expenses().toPlainString()).append(',').append(s.netSavings().toPlainString()).append("\r\n");
        return csv.toString();
    }
}
