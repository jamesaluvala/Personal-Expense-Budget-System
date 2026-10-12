package com.college.budget.repository;
import com.college.budget.api.ApiException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.*;
@Repository
public class Db {
    private final NamedParameterJdbcTemplate jdbc;
    public Db(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }
    public <T> List<T> list(String sql, Map<String,?> params, RowMapper<T> mapper) {
        return jdbc.query(sql, params, mapper);
    }
    public <T> T one(String sql, Map<String,?> params, RowMapper<T> mapper) {
        List<T> rows = list(sql, params, mapper);
        if (rows.isEmpty()) throw ApiException.missing();
        return rows.getFirst();
    }
    public int update(String sql, Map<String,?> params) { return jdbc.update(sql, params); }
    public long insert(String sql, Map<String,?> params) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(sql, new MapSqlParameterSource(params), keys, new String[]{"id"});
        return Objects.requireNonNull(keys.getKey(), "Database did not return a generated id").longValue();
    }
    public BigDecimal money(String sql, Map<String,?> params) {
        BigDecimal value = jdbc.queryForObject(sql, params, BigDecimal.class);
        return value == null ? new BigDecimal("0.00") : value.setScale(2);
    }
    public long count(String sql, Map<String,?> params) {
        Long value = jdbc.queryForObject(sql, params, Long.class);
        return value == null ? 0L : value;
    }
    // Serialize a user's writes, especially concurrent goal allocations.
    public void lockUser(long userId) {
        jdbc.queryForObject("SELECT id FROM app_users WHERE id=:uid FOR UPDATE", Map.of("uid", userId), Long.class);
    }
}
