package com.fms.hr_crm.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class GatewayApplication {

	public static void main(String[] args) {
		SpringApplication.run(GatewayApplication.class, args);
	}

	@Bean
	public RouteLocator routeLocator(RouteLocatorBuilder builder) {
		return builder.routes()
			// Route to lead service
			.route("lead-service", r -> r
				.path("/api/leads/**")
				.uri("lb://hr-lead-service"))
			// Route to AI service
			.route("ai-service", r -> r
				.path("/api/ai/**")
				.uri("lb://hr-ai-service"))
			// Route to calling service
			.route("calling-service", r -> r
				.path("/api/calls/**")
				.uri("lb://hr-calling-service"))
			// Route to WhatsApp service
			.route("whatsapp-service", r -> r
				.path("/api/whatsapp/**")
				.uri("lb://hr-whatsapp-service"))
			// Route to ops service
			.route("ops-service", r -> r
				.path("/api/ops/**")
				.uri("lb://hr-ops-service"))
			.build();
	}
}

