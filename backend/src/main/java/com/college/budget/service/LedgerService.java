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
public class LedgerService {
    private final Db db;
    private final Clock clock;
    public LedgerService(Db db, Clock clock) { this.db=db; this.clock=clock; }
    private static final RowMapper<Category> CATEGORY = (r,n) -> new Category(r.getLong("id"), r.getString("name"), Kind.valueOf(r.getString("kind")));
    private static final RowMapper<Entry> ENTRY = (r,n) -> new Entry(r.getLong("id"), Kind.valueOf(r.getString("kind")),
        r.getLong("category_id"),r.getString("category_name"),r.getBigDecimal("amount"),r.getDate("transaction_date").toLocalDate(),r.getString("description"));
    private static final String SELECT_ENTRIES = "SELECT t.*, c.name AS category_name FROM financial_transactions t JOIN categories c ON c.id=t.category_id AND c.user_id=t.user_id ";
    public List<Category> categories(long uid) {
        return db.list("SELECT id,name,kind FROM categories WHERE user_id=:uid ORDER BY kind,name", Map.of("uid",uid), CATEGORY);
    }
    public Category category(long uid, long id) {
        return db.one("SELECT id,name,kind FROM categories WHERE user_id=:uid AND id=:id",Map.of("uid",uid,"id",id),CATEGORY);
    }
    @Transactional public Category saveCategory(long uid, Long id, CategoryInput input) {
        db.lockUser(uid);
        String name = input.name().trim();
        if (id != null && category(uid,id).type() != input.type())
            throw new IllegalArgumentException("Category type cannot be changed. Create a new category instead.");
        Map<String,Object> p = new HashMap<>(Map.of("uid",uid,"name",name,"kind",input.type().name()));
        if (id == null) id = db.insert("INSERT INTO categories(user_id,name,kind) VALUES(:uid,:name,:kind)",p);
        else { p.put("id",id); db.update("UPDATE categories SET name=:name WHERE id=:id AND user_id=:uid",p); }
        return category(uid,id);
    }
    @Transactional public void deleteCategory(long uid, long id) {
        db.lockUser(uid); category(uid,id);
        if (db.count("SELECT COUNT(*) FROM financial_transactions WHERE user_id=:uid AND category_id=:id",Map.of("uid",uid,"id",id)) > 0)
            throw ApiException.conflict("This category is used by transactions. Reassign those transactions before deleting it.");
        db.update("DELETE FROM categories WHERE user_id=:uid AND id=:id",Map.of("uid",uid,"id",id));
    }
    private String table(Kind kind) { return kind == Kind.INCOME ? "income" : "expenses"; }
    public Entry entry(long uid, Kind type, long id) {
        return db.one(SELECT_ENTRIES+"WHERE t.user_id=:uid AND t.kind=:kind AND t.id=:id",
            Map.of("uid",uid,"kind",type.name(),"id",id),ENTRY);
    }
    @Transactional public Entry saveEntry(long uid, Kind existingType, Long id, EntryInput input) {
        db.lockUser(uid);
        BigDecimal amount = FinanceRules.amount(input.amount());
        FinanceRules.pastDate(input.date(),clock);
        if (id != null) {
            entry(uid,existingType,id);
            if (input.type() != existingType) throw new IllegalArgumentException("Transaction type cannot be changed during editing.");
        }
        if (category(uid,input.categoryId()).type() != input.type())
            throw new IllegalArgumentException("Select a category matching the transaction type.");
        Map<String,Object> p = new HashMap<>(Map.of("uid",uid,"category",input.categoryId(),"kind",input.type().name(),
            "amount",amount,"date",input.date(),"description",input.description().trim()));
        String table = table(input.type()); // Enum whitelist; never a user-supplied SQL identifier.
        if (id == null) id = db.insert("INSERT INTO "+table+"(user_id,category_id,category_kind,amount,transaction_date,description) VALUES(:uid,:category,:kind,:amount,:date,:description)",p);
        else {
            p.put("id",id);
            db.update("UPDATE "+table+" SET category_id=:category,amount=:amount,transaction_date=:date,description=:description WHERE user_id=:uid AND id=:id",p);
        }
        return entry(uid,input.type(),id);
    }
    @Transactional public void deleteEntry(long uid, Kind type, long id) {
        db.lockUser(uid); entry(uid,type,id);
        db.update("DELETE FROM "+table(type)+" WHERE user_id=:uid AND id=:id",Map.of("uid",uid,"id",id));
    }
    public EntryPage entries(long uid, Kind type, Long categoryId, LocalDate from, LocalDate to,
                             String search, String sort, int page, int size) {
        if (page < 0 || page > 1000000 || size < 1 || size > 100) throw new IllegalArgumentException("Invalid page or size (1-100).");
        if (from != null && to != null && from.isAfter(to)) throw new IllegalArgumentException("From date cannot be after to date.");
        if (search.length() > 100) throw new IllegalArgumentException("Search must be at most 100 characters.");
        Map<String,String> sorts = Map.of("date_desc","t.transaction_date DESC,t.id DESC,t.kind ASC",
            "date_asc","t.transaction_date ASC,t.id ASC,t.kind ASC","amount_desc","t.amount DESC,t.id DESC,t.kind ASC",
            "amount_asc","t.amount ASC,t.id ASC,t.kind ASC","description_asc","t.description ASC,t.id ASC,t.kind ASC");
        if (!sorts.containsKey(sort)) throw new IllegalArgumentException("Invalid sort option.");
        Map<String,Object> p = new HashMap<>(Map.of("uid",uid));
        StringBuilder where = new StringBuilder("WHERE t.user_id=:uid ");
        if (type != null) { where.append("AND t.kind=:kind "); p.put("kind",type.name()); }
        if (categoryId != null) { category(uid,categoryId); where.append("AND t.category_id=:category "); p.put("category",categoryId); }
        if (from != null) { where.append("AND t.transaction_date>=:from "); p.put("from",from); }
        if (to != null) { where.append("AND t.transaction_date<=:to "); p.put("to",to); }
        if (!search.isBlank()) {
            where.append("AND (LOWER(t.description) LIKE :search ESCAPE '!' OR LOWER(c.name) LIKE :search ESCAPE '!') ");
            String literal=search.trim().toLowerCase(Locale.ROOT).replace("!","!!").replace("%","!%").replace("_","!_");
            p.put("search","%"+literal+"%");
        }
        long count = db.count("SELECT COUNT(*) FROM financial_transactions t JOIN categories c ON c.id=t.category_id AND c.user_id=t.user_id "+where,p);
        p.put("size",size); p.put("offset",(long)page*size);
        List<Entry> items=db.list(SELECT_ENTRIES+where+"ORDER BY "+sorts.get(sort)+" LIMIT :size OFFSET :offset",p,ENTRY);
        return new EntryPage(items,count,page,size);
    }
    public List<Entry> monthEntries(long uid, YearMonth month, boolean recent) {
        return db.list(SELECT_ENTRIES+"WHERE t.user_id=:uid AND t.transaction_date>=:start AND t.transaction_date<:end ORDER BY t.transaction_date DESC,t.id DESC,t.kind"+(recent?" LIMIT 8":""),
            Map.of("uid",uid,"start",month.atDay(1),"end",month.plusMonths(1).atDay(1)),ENTRY);
    }
}
