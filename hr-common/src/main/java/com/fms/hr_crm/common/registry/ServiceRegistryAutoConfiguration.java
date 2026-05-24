package com.fms.hr_crm.common.registry;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration that activates when {@code registry.enabled=true}.
 *
 * <p>Creates two beans:
 * <ul>
 *   <li>{@link ServiceRegistryClient} — used by service-to-service HTTP clients
 *       to resolve peer URLs from the registry. Inject as
 *       {@code Optional<ServiceRegistryClient>} so your client works even when
 *       the registry is disabled.</li>
 *   <li>{@link ServiceRegistrar} — handles the register/heartbeat/deregister
 *       lifecycle for the owning service.</li>
 * </ul>
 *
 * <p>Activated by adding the following to a service's {@code application.yaml}:
 * <pre>
 * registry:
 *   enabled: true
 *   url: ${REGISTRY_URL:http://localhost:8761}
 *   service-name: hr-your-service
 *   service-url: ${SERVICE_URL:http://localhost:PORT}
 * </pre>
 */
@AutoConfiguration
@ConditionalOnProperty(name = "registry.enabled", havingValue = "true")
@EnableConfigurationProperties(RegistryProperties.class)
public class ServiceRegistryAutoConfiguration {

    @Bean
    public ServiceRegistryClient serviceRegistryClient(RegistryProperties props) {
        return new ServiceRegistryClient(props.url());
    }

    @Bean
    public ServiceRegistrar serviceRegistrar(ServiceRegistryClient client, RegistryProperties props) {
        return new ServiceRegistrar(client, props);
    }
}