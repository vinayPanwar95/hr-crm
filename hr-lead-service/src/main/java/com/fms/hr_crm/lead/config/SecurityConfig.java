package com.fms.hr_crm.lead.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // Use cookie-based CSRF so Thymeleaf can read the token without needing a session.
        // HttpSessionCsrfTokenRepository causes "Cannot create session after response committed"
        // when th:action is inside a th:each loop (session creation is deferred past stream start).
        var csrfTokenRepo = CookieCsrfTokenRepository.withHttpOnlyFalse();
        var csrfHandler = new CsrfTokenRequestAttributeHandler();

        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/h2-console/**").permitAll()
                .requestMatchers("/", "/dashboard").permitAll()
                .requestMatchers("/leads/**").permitAll()                  // UI — open for local dev
                .requestMatchers("/recruiters", "/recruiters/**").permitAll()  // Recruiter dashboard + POST
                .requestMatchers("/api/recruiters/**").permitAll()         // Recruiter autocomplete API
                .requestMatchers("/api/leads/internal/**").permitAll()     // service-to-service internal
                .anyRequest().authenticated()
            )
            .csrf(csrf -> csrf
                .csrfTokenRepository(csrfTokenRepo)
                .csrfTokenRequestHandler(csrfHandler)
                .ignoringRequestMatchers("/h2-console/**")  // H2 console uses POST internally
            )
            .headers(headers -> headers
                .frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin)  // H2 console uses iframes
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/dashboard", true)
                .permitAll()
            )
            .logout(logout -> logout
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            );

        return http.build();
    }
}