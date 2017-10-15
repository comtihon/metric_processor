package com.metric.processor.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ServerConfig {
    @Value("${processor.warning_temperature}")
    private Double fireEventRate;

    @Value("${processor.warnings_for_event}")
    private int eventsToFire;

    @Value("${processor.cache.registry_key}")
    private String registryKey;

    public Double getFireEventRate() {
        return fireEventRate;
    }

    public int getEventsToFire() {
        return eventsToFire;
    }

    public String getRegistryKey() {
        return registryKey;
    }
}
