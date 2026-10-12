package com.college.budget;
import tools.jackson.databind.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.*;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(ApplicationIntegrationTest.FixedClock.class)
class ApplicationIntegrationTest {
    @TestConfiguration static class FixedClock {
        @Bean @Primary Clock testClock() { return Clock.fixed(Instant.parse("2026-10-12T12:00:00Z"),ZoneOffset.UTC); }
    }
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    private static final String PASSWORD="College-test-password!42";
    private record Account(String email,MockHttpSession session) {}
    private JsonNode body(MvcResult result) throws Exception { return json.readTree(result.getResponse().getContentAsString()); }
    private Account account() throws Exception {
        String email="test-"+UUID.randomUUID()+"@example.com";
        mvc.perform(post("/api/auth/register").with(csrf()).contentType("application/json")
            .content(json.writeValueAsString(Map.of("name","Test account","email",email,"password",PASSWORD))))
            .andExpect(status().isCreated());
        MvcResult result=mvc.perform(post("/api/auth/login").with(csrf()).param("email",email).param("password",PASSWORD))
            .andExpect(status().isOk()).andReturn();
        return new Account(email,(MockHttpSession)result.getRequest().getSession(false));
    }
    private JsonNode read(Account a,String path) throws Exception {
        return body(mvc.perform(get("/api"+path).session(a.session())).andExpect(status().isOk()).andReturn());
    }
    private ResultActions send(Account a,String method,String path,Object value) throws Exception {
        var builder=switch(method) { case "PUT" -> put("/api"+path); case "DELETE" -> delete("/api"+path); default -> post("/api"+path); };
        builder.session(a.session()).with(csrf());
        if(value!=null) builder.contentType("application/json").content(json.writeValueAsString(value));
        return mvc.perform(builder);
    }
    private long category(Account a,String type) throws Exception {
        for(JsonNode c:read(a,"/categories")) if(c.get("type").stringValue().equals(type)) return c.get("id").longValue();
        throw new IllegalStateException("No test category");
    }
    private Map<String,Object> entry(Account a,String type,String amount,String date,String description) throws Exception {
        return Map.of("type",type,"categoryId",category(a,type),"amount",amount,"date",date,"description",description);
    }
    private long create(Account a,String type,String amount,String date,String description) throws Exception {
        return body(send(a,"POST","/transactions",entry(a,type,amount,date,description)).andExpect(status().isCreated()).andReturn()).get("id").longValue();
    }
    private void amount(String expected,JsonNode actual) { assertEquals(0,new BigDecimal(expected).compareTo(new BigDecimal(actual.stringValue()))); }

    @Test void registrationLoginPasswordsAndLogout() throws Exception {
        Account a=account();
        assertEquals(a.email(),read(a,"/auth/me").get("email").stringValue());
        String hash=jdbc.queryForObject("SELECT password_hash FROM app_users WHERE email=?",String.class,a.email());
        assertNotEquals(PASSWORD,hash); assertTrue(hash.startsWith("$2"));
        assertFalse(read(a,"/auth/me").has("passwordHash"));
        mvc.perform(post("/api/auth/login").with(csrf()).param("email",a.email()).param("password","wrong"))
            .andExpect(status().isUnauthorized());
        send(a,"POST","/auth/logout",null).andExpect(status().isNoContent());
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }
    @Test void emptyAccountsContainNoFakeTransactions() throws Exception {
        Account a=account(); JsonNode data=read(a,"/dashboard?month=2026-10");
        amount("0.00",data.get("income")); amount("0.00",data.get("expenses")); assertEquals(0,data.get("transactionCount").intValue());
        assertEquals(10,read(a,"/categories").size()); assertTrue(data.get("savingsRate").isNull());
    }
    @Test void csrfAndUnauthenticatedAccessAreRejected() throws Exception {
        mvc.perform(get("/api/transactions")).andExpect(status().isUnauthorized());
        Account a=account();
        mvc.perform(post("/api/categories").session(a.session()).contentType("application/json")
            .content("{\"name\":\"No csrf\",\"type\":\"EXPENSE\"}")).andExpect(status().isForbidden());
    }
    @Test void persistenceAndDecimalCalculationsAcrossRequests() throws Exception {
        Account a=account(); create(a,"INCOME","1000.10","2026-10-01","Salary"); create(a,"EXPENSE","250.05","2026-10-02","Food");
        JsonNode data=read(a,"/dashboard?month=2026-10");
        amount("1000.10",data.get("income")); amount("250.05",data.get("expenses")); amount("750.05",data.get("netSavings"));
        amount("750.05",data.get("balance")); assertEquals(2,read(a,"/transactions").get("total").intValue());
        assertEquals(2,read(a,"/reports?month=2026-10").get("transactions").size());
    }
    @Test void financialInputsAreValidated() throws Exception {
        Account a=account();
        for(String invalid:List.of("0","-1","1.001","1000000000000.00"))
            send(a,"POST","/transactions",entry(a,"EXPENSE",invalid,"2026-10-01","Invalid amount")).andExpect(status().isBadRequest());
        send(a,"POST","/transactions",entry(a,"EXPENSE","1","2026-10-13","Future date")).andExpect(status().isBadRequest());
        send(a,"POST","/transactions",entry(a,"EXPENSE","1","2026-02-30","Invalid date")).andExpect(status().isBadRequest());
        send(a,"POST","/transactions",entry(a,"EXPENSE","1","2026-10-01"," ")).andExpect(status().isBadRequest());
        send(a,"POST","/transactions",Map.of("type","EXPENSE","categoryId",category(a,"INCOME"),"amount","1","date","2026-10-01","description","Wrong kind"))
            .andExpect(status().isBadRequest());
    }
    @Test void oneUserCannotReadEditDeleteOrUseAnotherUsersRecords() throws Exception {
        Account alice=account(),bob=account(); long id=create(alice,"EXPENSE","20","2026-10-01","Private item");
        mvc.perform(get("/api/transactions/EXPENSE/"+id).session(bob.session())).andExpect(status().isNotFound());
        send(bob,"PUT","/transactions/EXPENSE/"+id,entry(bob,"EXPENSE","2","2026-10-01","Hijack")).andExpect(status().isNotFound());
        send(bob,"DELETE","/transactions/EXPENSE/"+id,null).andExpect(status().isNotFound());
        send(bob,"POST","/transactions",entry(alice,"EXPENSE","1","2026-10-01","Foreign category")).andExpect(status().isNotFound());
        mvc.perform(get("/api/transactions?categoryId="+category(alice,"EXPENSE")).session(bob.session())).andExpect(status().isNotFound());
        assertEquals(0,read(bob,"/transactions").get("total").intValue());
        amount("0",read(bob,"/reports?month=2026-10").get("summary").get("expenses"));
        assertEquals(1,read(alice,"/transactions").get("total").intValue());
    }
    @Test void changingAndDeletingExpenseRecalculatesBothMonths() throws Exception {
        Account a=account();
        send(a,"PUT","/budgets",Map.of("month","2026-02","amount","100")).andExpect(status().isOk());
        long id=create(a,"EXPENSE","110","2026-02-28","Old month");
        assertEquals("EXCEEDED",read(a,"/budgets?month=2026-02").get("status").stringValue());
        send(a,"PUT","/transactions/EXPENSE/"+id,entry(a,"EXPENSE","70","2026-03-01","Moved month")).andExpect(status().isOk());
        amount("0",read(a,"/budgets?month=2026-02").get("spent"));
        amount("70",read(a,"/dashboard?month=2026-03").get("expenses"));
        send(a,"DELETE","/transactions/EXPENSE/"+id,null).andExpect(status().isNoContent());
        amount("0",read(a,"/dashboard?month=2026-03").get("expenses"));
        send(a,"DELETE","/budgets/2026-02",null).andExpect(status().isNoContent());
    }
    @Test void leapDayIsIncludedAndNextMonthExcluded() throws Exception {
        Account a=account(); create(a,"EXPENSE","12.34","2024-02-29","Leap day"); create(a,"EXPENSE","90","2024-03-01","Next month");
        JsonNode report=read(a,"/reports?month=2024-02");
        assertEquals(1,report.get("transactions").size()); amount("12.34",report.get("summary").get("expenses"));
    }
    @Test void categoriesCannotBeDeletedWhileReferenced() throws Exception {
        Account a=account(); long id=category(a,"INCOME"); create(a,"INCOME","12","2026-10-01","Category reference");
        send(a,"DELETE","/categories/"+id,null).andExpect(status().isConflict());
        send(a,"PUT","/categories/"+id,Map.of("name","Renamed income","type","INCOME")).andExpect(status().isOk());
        assertEquals("Renamed income",read(a,"/income").get("items").get(0).get("categoryName").stringValue());
        send(a,"PUT","/categories/"+id,Map.of("name","Wrong kind","type","EXPENSE")).andExpect(status().isBadRequest());
    }
    @Test void searchSortAndPaginationUseSafeParameters() throws Exception {
        Account a=account(); create(a,"EXPENSE","10","2026-10-01","100% genuine"); create(a,"EXPENSE","20","2026-10-01","Other item");
        assertEquals(1,body(mvc.perform(get("/api/transactions").param("search","100%").session(a.session())).andExpect(status().isOk()).andReturn()).get("total").intValue());
        JsonNode results=read(a,"/transactions?sort=amount_desc&size=1&page=0");
        assertEquals(2,results.get("total").intValue()); amount("20",results.get("items").get(0).get("amount"));
        assertEquals(1,read(a,"/transactions?size=1&page=1").get("items").size());
        mvc.perform(get("/api/transactions?sort=DROP_TABLE").session(a.session())).andExpect(status().isBadRequest());
    }
    @Test void savingsDoNotDoubleCountExpensesAndRespectAvailableBalance() throws Exception {
        Account a=account(); create(a,"INCOME","1000","2026-10-01","Savings source"); create(a,"EXPENSE","200","2026-10-02","Spending");
        JsonNode goal=body(send(a,"POST","/goals",Map.of("title","Emergency fund","targetAmount","500","targetDate","2027-01-01"))
            .andExpect(status().isCreated()).andReturn()); long id=goal.get("id").longValue();
        send(a,"POST","/goals/"+id+"/contributions",Map.of("amount","200","date","2026-10-03")).andExpect(status().isCreated());
        JsonNode s=read(a,"/dashboard?month=2026-10"); amount("800",s.get("balance")); amount("200",s.get("reservedForGoals")); amount("600",s.get("availableBalance")); amount("200",s.get("expenses"));
        send(a,"POST","/goals/"+id+"/contributions",Map.of("amount","301","date","2026-10-03")).andExpect(status().isConflict());
        send(a,"PUT","/goals/"+id,Map.of("title","Reduced goal","targetAmount","100","targetDate","2027-01-01")).andExpect(status().isConflict());
        long contribution=read(a,"/goals/"+id+"/contributions").get(0).get("id").longValue();
        send(a,"DELETE","/goals/"+id+"/contributions/"+contribution,null).andExpect(status().isNoContent());
        amount("0",read(a,"/goals").get(0).get("savedAmount"));
        send(a,"DELETE","/goals/"+id,null).andExpect(status().isNoContent());
    }
    @Test void goalsAndBudgetsArePrivate() throws Exception {
        Account a=account(), b=account();
        long id=body(send(a,"POST","/goals",Map.of("title","Private goal","targetAmount","50","targetDate","2027-01-01"))
            .andExpect(status().isCreated()).andReturn()).get("id").longValue();
        mvc.perform(get("/api/goals/"+id+"/contributions").session(b.session())).andExpect(status().isNotFound());
        send(b,"POST","/goals/"+id+"/contributions",Map.of("amount","1","date","2026-10-01")).andExpect(status().isNotFound());
        send(b,"DELETE","/goals/"+id,null).andExpect(status().isNotFound());
        send(a,"PUT","/budgets",Map.of("month","2026-10","amount","100")).andExpect(status().isOk());
        assertTrue(read(b,"/dashboard?month=2026-10").get("budget").isNull());
    }
    @Test void exportedCsvEscapesUntrustedTextAndContainsActualTotals() throws Exception {
        Account a=account(); create(a,"EXPENSE","10.01","2026-10-01","=HYPERLINK(\"example\")");
        var result=mvc.perform(get("/api/reports/export.csv?month=2026-10").session(a.session()))
            .andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store, private")).andReturn();
        String csv=result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertTrue(csv.contains("'=HYPERLINK")); assertTrue(csv.contains("Monthly totals,2026-10,,,,0.00,10.01,-10.01"));
    }
}
