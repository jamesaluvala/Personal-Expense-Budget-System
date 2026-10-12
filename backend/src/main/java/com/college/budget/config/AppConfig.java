package com.college.budget.config;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.module.SimpleModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import java.math.BigDecimal;
import java.time.*;
@Configuration
public class AppConfig {
    @Bean Clock clock(@Value("${app.time-zone}") String zone) { return Clock.system(ZoneId.of(zone)); }
    // Decimal strings preserve exact cents in transit. Charts convert values only for display.
    @Bean SimpleModule moneyModule() {
        SimpleModule module = new SimpleModule();
        module.addSerializer(BigDecimal.class, new ValueSerializer<BigDecimal>() {
            @Override public void serialize(BigDecimal value, JsonGenerator generator, SerializationContext context) {
                generator.writeString(value.toPlainString());
            }
        });
        return module;
    }
}
