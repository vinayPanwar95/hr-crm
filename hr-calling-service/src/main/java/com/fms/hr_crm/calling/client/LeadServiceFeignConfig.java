package com.fms.hr_crm.calling.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

/**
 * Wires up the {@link LeadServiceFeignClient} declarative HTTP client.
 *
 * <p>Uses Spring's built-in {@code HttpServiceProxyFactory} backed by {@code RestClient},
 * which requires no Spring Cloud dependency and is compatible with Spring Boot 4.x.
 *
 * <p>The {@code X-Internal-Service: calling-service} header is added as a default
 * header so every request to hr-lead-service is authenticated as an internal call.
 */
@Configuration
public class LeadServiceFeignConfig {

    @Bean
    public LeadServiceFeignClient leadServiceFeignClient(
            @Value("${services.lead-service.url:http://localhost:8081}") String baseUrl) {

        var restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("X-Internal-Service", "calling-service")
                .build();

        var adapter = RestClientAdapter.create(restClient);
        var factory = HttpServiceProxyFactory.builderFor(adapter).build();
        return factory.createClient(LeadServiceFeignClient.class);
    }
}