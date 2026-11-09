package com.ccomp.br.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import jakarta.annotation.PostConstruct;

import java.time.Clock;
import java.time.ZoneId;
import java.util.TimeZone;

@Configuration
public class ClockConfig {
    @Value("${app.clock.zone}")
    private String zoneId;

    @PostConstruct
    void configureJvmDefaultZone() {
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of(zoneId)));
    }

    @Bean
    public Clock clock() {
        ZoneId zone = ZoneId.of(zoneId);
        return Clock.system(zone);
    }
}
