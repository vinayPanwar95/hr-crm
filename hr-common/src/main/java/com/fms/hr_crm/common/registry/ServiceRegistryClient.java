package com.fms.hr_crm.common.registry;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.Optional;

/**
 * HTTP client for communicating with the hr-service-registry.
 *
 * <p>This is a plain class (not a Spring component) — it is created as a
 * {@code @Bean} by {@link ServiceRegistryAutoConfiguration} and is only
 * present in the application context when {@code registry.enabled=true}.
 *
 * <p>Inject it into your service clients as {@code Optional<ServiceRegistryClient>}
 * so that your bean compiles and works even when the registry is disabled:
 * <pre>
 * public MyClient(Optional{@literal <}ServiceRegistryClient{@literal >} registry, ...) { ... }
 * </pre>
 */
@Slf4j
public class ServiceRegistryClient {

    /** Local DTO for deserializing registry responses — keeps Jackson off the public API. */
    private record InstanceResponse(String name, String url, String status) {}

    private final RestClient restClient;

    public ServiceRegistryClient(String registryUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(registryUrl)
                .build();
        log.info("[Registry] Client initialised, pointing at {}", registryUrl);
    }

    /**
     * Registers or re-registers a service instance in the registry.
     * Failures are swallowed — the service will still start normally.
     */
    public void register(String name, String url) {
        try {
            restClient.post()
                    .uri("/registry/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("name", name, "url", url))
                    .retrieve()
                    .toBodilessEntity();
            log.info("[Registry] Registered {} -> {}", name, url);
        } catch (Exception e) {
            log.warn("[Registry] register({}) failed — registry may not be running yet: {}", name, e.getMessage());
        }
    }

    /**
     * Sends a heartbeat for the given service name.
     * Logged at DEBUG level to avoid noise in normal operation.
     */
    public void heartbeat(String name) {
        try {
            restClient.put()
                    .uri("/registry/{name}/heartbeat", name)
                    .retrieve()
                    .toBodilessEntity();
            log.debug("[Registry] Heartbeat sent: {}", name);
        } catch (Exception e) {
            log.debug("[Registry] Heartbeat failed for {}: {}", name, e.getMessage());
        }
    }

    /**
     * Deregisters a service instance. Called on graceful shutdown.
     * Failures are logged but do not prevent the service from stopping.
     */
    public void deregister(String name) {
        try {
            restClient.delete()
                    .uri("/registry/{name}", name)
                    .retrieve()
                    .toBodilessEntity();
            log.info("[Registry] Deregistered {}", name);
        } catch (Exception e) {
            log.warn("[Registry] deregister({}) failed: {}", name, e.getMessage());
        }
    }

    /**
     * Resolves the base URL for a given service name by querying the registry.
     *
     * @param name the logical service name (e.g. "hr-lead-service")
     * @return the URL if found, or empty if the service is not registered or the registry is down
     */
    public Optional<String> resolveUrl(String name) {
        try {
            var response = restClient.get()
                    .uri("/registry/{name}", name)
                    .retrieve()
                    .body(InstanceResponse.class);
            return Optional.ofNullable(response).map(InstanceResponse::url);
        } catch (Exception e) {
            log.debug("[Registry] resolveUrl({}) failed — registry may be down: {}", name, e.getMessage());
            return Optional.empty();
        }
    }
}