package com.fms.hr_crm.registry;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Lightweight service registry — Eureka-compatible REST API.
 * Runs on port 8761 (same as Eureka convention).
 *
 * <p>All services auto-register via hr-common's {@code ServiceRegistryAutoConfiguration}
 * when {@code registry.enabled=true} is set in their config.
 */
@SpringBootApplication
@EnableScheduling
public class ServiceRegistryApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServiceRegistryApplication.class, args);
    }
}