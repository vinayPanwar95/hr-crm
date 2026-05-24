package com.fms.hr_crm.calling.config;

import com.fms.hr_crm.calling.security.PostLoginSuccessHandler;
import com.fms.hr_crm.calling.security.RecruiterUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(
            RecruiterUserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder) {
        var provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           DaoAuthenticationProvider authProvider,
                                           PostLoginSuccessHandler successHandler) throws Exception {
        // Cookie-based CSRF avoids "Cannot create session after response committed"
        // which happens when Thymeleaf starts streaming before the deferred CSRF token
        // (session-based) is resolved by SpringActionTagProcessor.
        var csrfHandler = new CsrfTokenRequestAttributeHandler();

        http
            .authenticationProvider(authProvider)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/error").permitAll()
                .requestMatchers("/login", "/auth/**").permitAll()
                // Internal service-to-service endpoint — protected by header, not session
                .requestMatchers("/api/internal/**").permitAll()
                // Twilio webhooks — signature-validated, no session auth
                .requestMatchers("/webhook/**").permitAll()
                // H2 console for local dev (both with and without trailing slash)
                .requestMatchers("/h2-console", "/h2-console/**").permitAll()
                // UI + API
                .requestMatchers("/calls/**", "/api/calls/**").authenticated()
                .requestMatchers("/campaigns/**", "/api/campaigns/**").authenticated()
                .anyRequest().authenticated()
            )
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(csrfHandler)
                .ignoringRequestMatchers("/webhook/**", "/h2-console", "/h2-console/**", "/api/internal/**")
            )
            .headers(headers -> headers
                .frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin)
            )
            .formLogin(form -> form
                .loginPage("/login")
                .successHandler(successHandler)
                .permitAll()
            )
            .logout(logout -> logout
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            );

        return http.build();
    }
}
