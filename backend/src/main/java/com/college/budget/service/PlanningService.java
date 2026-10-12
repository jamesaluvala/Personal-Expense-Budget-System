package com.college.budget.service;
import com.college.budget.api.ApiException;
import com.college.budget.core.FinanceRules;
import com.college.budget.model.Models.*;
import com.college.budget.repository.Db;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Service
public class PlanningService {
    private final Db db;
    private final Clock clock;
    public PlanningService(Db db, Clock clock) { this.db=db; this.clock=clock; }
    public BigDecimal total(long uid, Kind kind, YearMonth month) {
        Map<String,Object> p = new HashMap<>(Map.of("uid",uid,"kind",kind.name()));
        String range="";
        if (month != null) { range=" AND transaction_date>=:start AND transaction_date<:end";
            p.put("start",month.atDay(1)); p.put("end",month.plusMonths(1).atDay(1)); }
        return db.money("SELECT COALESCE(SUM(amount),0) FROM financial_transactions WHERE user_id=:uid AND kind=:kind"+range,p);
    }
    public BigDecimal balance(long uid) { return total(uid,Kind.INCOME,null).subtract(total(uid,Kind.EXPENSE,null)); }
    public BigDecimal reserved(long uid) {
        return db.money("SELECT COALESCE(SUM(amount),0) FROM goal_contributions WHERE user_id=:uid",Map.of("uid",uid));
    }
    public Budget budget(long uid, YearMonth month) {
        var found=db.list("SELECT id,amount FROM budgets WHERE user_id=:uid AND month_start=:month",
            Map.of("uid",uid,"month",month.atDay(1)),(r,n) -> Map.entry(r.getLong("id"),r.getBigDecimal("amount")));
        if (found.isEmpty()) return null;
        var row=found.getFirst(); BigDecimal spent=total(uid,Kind.EXPENSE,month);
        return new Budget(row.getKey(),month.toString(),row.getValue(),spent,row.getValue().subtract(spent),
            FinanceRules.percentage(spent,row.getValue()),FinanceRules.budgetStatus(spent,row.getValue()));
    }
    @Transactional public Budget saveBudget(long uid, BudgetInput input) {
        db.lockUser(uid); YearMonth month=FinanceRules.month(input.month()); BigDecimal amount=FinanceRules.amount(input.amount());
        Map<String,Object> p=Map.of("uid",uid,"month",month.atDay(1),"amount",amount);
        if (budget(uid,month)==null) db.update("INSERT INTO budgets(user_id,month_start,amount) VALUES(:uid,:month,:amount)",p);
        else db.update("UPDATE budgets SET amount=:amount WHERE user_id=:uid AND month_start=:month",p);
        return budget(uid,month);
    }
    @Transactional public void deleteBudget(long uid, String monthValue) {
        db.lockUser(uid); YearMonth month=FinanceRules.month(monthValue);
        if (db.update("DELETE FROM budgets WHERE user_id=:uid AND month_start=:month",Map.of("uid",uid,"month",month.atDay(1)))==0) throw ApiException.missing();
    }
    private final RowMapper<Goal> goalMapper=this::mapGoal;
    private Goal mapGoal(java.sql.ResultSet r, int n) throws java.sql.SQLException {
        BigDecimal target=r.getBigDecimal("target_amount"), saved=r.getBigDecimal("saved");
        LocalDate date=r.getDate("target_date").toLocalDate();
        String status=saved.compareTo(target)>=0?"COMPLETED":date.isBefore(LocalDate.now(clock))?"OVERDUE":"IN_PROGRESS";
        return new Goal(r.getLong("id"),r.getString("title"),target,saved,date,FinanceRules.percentage(saved,target),status);
    }
    private static final String GOALS="SELECT g.*, COALESCE((SELECT SUM(c.amount) FROM goal_contributions c WHERE c.user_id=g.user_id AND c.goal_id=g.id),0) AS saved FROM savings_goals g ";
    public List<Goal> goals(long uid) { return db.list(GOALS+"WHERE g.user_id=:uid ORDER BY g.target_date,g.id",Map.of("uid",uid),goalMapper); }
    public Goal goal(long uid,long id) { return db.one(GOALS+"WHERE g.user_id=:uid AND g.id=:id",Map.of("uid",uid,"id",id),goalMapper); }
    @Transactional public Goal saveGoal(long uid,Long id,GoalInput input) {
        db.lockUser(uid); BigDecimal target=FinanceRules.amount(input.targetAmount());
        if (input.targetDate().isBefore(LocalDate.of(2000,1,1)) || input.targetDate().isAfter(LocalDate.of(2100,12,31)))
            throw new IllegalArgumentException("Goal date must be between 2000 and 2100.");
        if (id==null && input.targetDate().isBefore(LocalDate.now(clock))) throw new IllegalArgumentException("A new goal's target date cannot be in the past.");
        if (id!=null && goal(uid,id).savedAmount().compareTo(target)>0) throw ApiException.conflict("Target cannot be lower than the amount already reserved. Release contributions first.");
        Map<String,Object> p=new HashMap<>(Map.of("uid",uid,"title",input.title().trim(),"amount",target,"date",input.targetDate()));
        if(id==null) id=db.insert("INSERT INTO savings_goals(user_id,title,target_amount,target_date) VALUES(:uid,:title,:amount,:date)",p);
        else { p.put("id",id); db.update("UPDATE savings_goals SET title=:title,target_amount=:amount,target_date=:date WHERE user_id=:uid AND id=:id",p); }
        return goal(uid,id);
    }
    @Transactional public void deleteGoal(long uid,long id) {
        db.lockUser(uid); goal(uid,id);
        db.update("DELETE FROM savings_goals WHERE user_id=:uid AND id=:id",Map.of("uid",uid,"id",id));
    }
    public List<Contribution> contributions(long uid,long goalId) {
        goal(uid,goalId);
        return db.list("SELECT id,amount,contribution_date FROM goal_contributions WHERE user_id=:uid AND goal_id=:goal ORDER BY contribution_date DESC,id DESC",
            Map.of("uid",uid,"goal",goalId),(r,n)->new Contribution(r.getLong("id"),r.getBigDecimal("amount"),r.getDate("contribution_date").toLocalDate()));
    }
    @Transactional public Goal contribute(long uid,long goalId,ContributionInput input) {
        db.lockUser(uid); Goal goal=goal(uid,goalId); BigDecimal amount=FinanceRules.amount(input.amount()); FinanceRules.pastDate(input.date(),clock);
        if(amount.compareTo(goal.targetAmount().subtract(goal.savedAmount()))>0) throw ApiException.conflict("Contribution exceeds the remaining goal target.");
        if(amount.compareTo(balance(uid).subtract(reserved(uid)))>0) throw ApiException.conflict("Contribution exceeds your unreserved recorded balance.");
        db.update("INSERT INTO goal_contributions(user_id,goal_id,amount,contribution_date) VALUES(:uid,:goal,:amount,:date)",
            Map.of("uid",uid,"goal",goalId,"amount",amount,"date",input.date()));
        return goal(uid,goalId);
    }
    @Transactional public void deleteContribution(long uid,long goalId,long id) {
        db.lockUser(uid); goal(uid,goalId);
        if(db.update("DELETE FROM goal_contributions WHERE user_id=:uid AND goal_id=:goal AND id=:id",Map.of("uid",uid,"goal",goalId,"id",id))==0) throw ApiException.missing();
    }
}
