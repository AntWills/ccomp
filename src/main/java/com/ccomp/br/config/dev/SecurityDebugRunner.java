package com.ccomp.br.config.dev;

import jakarta.servlet.Filter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class SecurityDebugRunner implements ApplicationRunner {

    private final FilterChainProxy filterChainProxy;

    @Override
    public void run(ApplicationArguments args) {
        log.info("============ SPRING SECURITY FILTER CHAINS ============");

        int chainIndex = 1;
        for (SecurityFilterChain chain : filterChainProxy.getFilterChains()) {
            log.info("Chain #{} -> {}", chainIndex++, chain.toString());

            for (Filter filter : chain.getFilters()) {
                log.info("   ↳ {}", filter.getClass().getSimpleName());
            }
        }

        log.info("=======================================================");
    }
}