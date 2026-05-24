package com.fms.hr_crm.common.registry;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the service registry client.
 *
 * <p>Add the following to each service's {@code application.yaml}:
 * <pre>
 * registry:
 *   enabled: true
 *   url: ${REGISTRY_URL:http://localhost:8761}
 *   service-name: hr-your-service        # must match spring.application.name
 *   service-url: ${SERVICE_URL:http://localhost:PORT}
 * </pre>
 *
 * @param enabled     set to {@code true} to activate auto-registration
 * @param url         base URL of the service registry (hr-service-registry)
 * @param serviceName this service's logical name used as the registry key
 * @param serviceUrl  the URL that other services use to reach this instance
 */
@ConfigurationProperties(prefix = "registry")
public record RegistryProperties(
        boolean enabled,
        String url,
        String serviceName,
        String serviceUrl
) {}