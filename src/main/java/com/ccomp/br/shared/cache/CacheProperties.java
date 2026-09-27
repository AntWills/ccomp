package com.ccomp.br.shared.cache;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Data
@Component
@ConfigurationProperties(prefix = "app.cache")
public class CacheProperties {
    private Duration defaultTtl = Duration.ofMinutes(5);
    private Map<String, Duration> tiers = new HashMap<>();
    private Map<String, String> tierOverrides = new HashMap<>();

    public Duration getDefaultTtl() {
        return defaultTtl;
    }

    public void setDefaultTtl(Duration defaultTtl) {
        this.defaultTtl = defaultTtl;
    }

    public Map<String, Duration> getTiers() {
        return tiers;
    }

    public void setTiers(Map<String, Duration> tiers) {
        this.tiers = tiers;
    }

    public Map<String, String> getTierOverrides() {
        return tierOverrides;
    }

    public void setTierOverrides(Map<String, String> tierOverrides) {
        this.tierOverrides = tierOverrides;
    }
}
