package com.fms.hr_crm.registry.service;

import com.fms.hr_crm.registry.model.RegisterRequest;
import com.fms.hr_crm.registry.model.ServiceInstance;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory service registry. Instances are evicted if no heartbeat is received
 * within {@link #TTL}. Clients should send a heartbeat every 30 seconds.
 */
@Service
@Slf4j
public class RegistryService {

    /** Instance is considered stale after 90 seconds of silence. */
    private static final Duration TTL = Duration.ofSeconds(90);

    private final ConcurrentHashMap<String, ServiceInstance> registry = new ConcurrentHashMap<>();

    /**
     * Registers or re-registers a service instance. Preserves the original
     * {@code registeredAt} timestamp on re-registration so uptime is accurate.
     */
    public ServiceInstance register(RegisterRequest request) {
        var existing = registry.get(request.name());
        var registeredAt = (existing != null) ? existing.registeredAt() : Instant.now();
        var instance = new ServiceInstance(
                request.name(), request.url(), "UP", registeredAt, Instant.now());
        registry.put(request.name(), instance);
        log.info("[Registry] Registered: {} -> {}", request.name(), request.url());
        return instance;
    }

    /**
     * Updates the {@code lastSeen} timestamp for a registered instance.
     * Returns empty if the service is not currently registered.
     */
    public Optional<ServiceInstance> heartbeat(String name) {
        var existing = registry.get(name);
        if (existing == null) {
            log.debug("[Registry] Heartbeat from unknown service: {}", name);
            return Optional.empty();
        }
        var updated = new ServiceInstance(
                existing.name(), existing.url(), "UP", existing.registeredAt(), Instant.now());
        registry.put(name, updated);
        log.debug("[Registry] Heartbeat received: {}", name);
        return Optional.of(updated);
    }

    /**
     * Removes a service instance from the registry.
     *
     * @return true if the instance was present and removed, false otherwise
     */
    public boolean deregister(String name) {
        var removed = registry.remove(name);
        if (removed != null) {
            log.info("[Registry] Deregistered: {}", name);
        }
        return removed != null;
    }

    /** Finds a service instance by name. */
    public Optional<ServiceInstance> find(String name) {
        return Optional.ofNullable(registry.get(name));
    }

    /** Returns all currently registered instances. */
    public Collection<ServiceInstance> findAll() {
        return Collections.unmodifiableCollection(registry.values());
    }

    /** Evicts instances that have not sent a heartbeat within {@link #TTL}. */
    @Scheduled(fixedDelay = 30_000)
    public void evictStale() {
        var cutoff = Instant.now().minus(TTL);
        registry.entrySet().removeIf(entry -> {
            var stale = entry.getValue().lastSeen().isBefore(cutoff);
            if (stale) {
                log.warn("[Registry] Evicting stale instance: {} (last seen: {})",
                        entry.getKey(), entry.getValue().lastSeen());
            }
            return stale;
        });
    }
}