package com.fms.hr_crm.lead.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

/**
 * Wires up the {@link CallingServiceClient} declarative HTTP client.
 * Uses Spring's built-in {@code HttpServiceProxyFactory} — no Spring Cloud required.
 */
@Configuration
public class CallingServiceClientConfig {

    @Bean
    public CallingServiceClient callingServiceClient(
            @Value("${services.calling-service.url:http://localhost:8083}") String baseUrl) {

        var restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();

        var adapter = RestClientAdapter.create(restClient);
        var factory = HttpServiceProxyFactory.builderFor(adapter).build();
        return factory.createClient(CallingServiceClient.class);
    }
}