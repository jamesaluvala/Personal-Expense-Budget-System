package com.college.budget.service;
import com.college.budget.api.ApiException;
import com.college.budget.model.Models.*;
import com.college.budget.repository.Db;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.util.*;
@Service
public class UserService {
    private final Db db;
    private final PasswordEncoder passwords;
    public UserService(Db db, PasswordEncoder passwords) { this.db=db; this.passwords=passwords; }
    public Optional<User> find(String email) {
        return db.list("SELECT id,name,email,password_hash FROM app_users WHERE email=:email",
            Map.of("email", email.trim().toLowerCase(Locale.ROOT)), (rs,n) -> new User(rs.getLong("id"),
            rs.getString("name"),rs.getString("email"),rs.getString("password_hash"))).stream().findFirst();
    }
    public Profile profile(Authentication authentication) {
        User user = find(authentication.getName()).orElseThrow(ApiException::missing);
        return new Profile(user.id(), user.name(), user.email());
    }
    @Transactional
    public Profile register(Register input) {
        if (input.password().isBlank() || input.password().getBytes(StandardCharsets.UTF_8).length > 72)
            throw new IllegalArgumentException("Password must contain 12 or more characters and at most 72 UTF-8 bytes.");
        String email = input.email().trim().toLowerCase(Locale.ROOT);
        if (find(email).isPresent()) throw ApiException.conflict("Registration could not be completed with this email.");
        long id = db.insert("INSERT INTO app_users(name,email,password_hash) VALUES(:name,:email,:password)",
            Map.of("name", input.name().trim(), "email",email,"password",passwords.encode(input.password())));
        // Starter labels are not financial records. Every new user's balances start at zero.
        for (String name : List.of("Salary", "Freelance", "Other income")) addCategory(id, name, "INCOME");
        for (String name : List.of("Food", "Transport", "Housing", "Shopping", "Health", "Education", "Other expense"))
            addCategory(id, name, "EXPENSE");
        return new Profile(id, input.name().trim(), email);
    }
    private void addCategory(long id, String name, String kind) {
        db.update("INSERT INTO categories(user_id,name,kind) VALUES(:uid,:name,:kind)", Map.of("uid",id,"name",name,"kind",kind));
    }
}
