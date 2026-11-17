package com.bankos.common.config;

import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer bigDecimalAsStringCustomizer() {
        return builder -> {
            SimpleModule module = new SimpleModule("bankos-money");
            module.addSerializer(BigDecimal.class, ToStringSerializer.instance);
            builder.modulesToInstall(module);
        };
    }
}
