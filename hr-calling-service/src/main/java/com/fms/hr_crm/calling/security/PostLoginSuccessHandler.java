package com.fms.hr_crm.calling.security;

import com.fms.hr_crm.calling.repository.RecruiterUserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * After a successful login, redirects the recruiter to the change-password page
 * if their account was provisioned with a one-time temporary password.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PostLoginSuccessHandler implements AuthenticationSuccessHandler {

    private final RecruiterUserRepository userRepo;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        var username = authentication.getName();
        var mustChange = userRepo.findByUsername(username)
                .map(u -> u.isMustChangePassword())
                .orElse(false);

        if (mustChange) {
            log.info("PostLoginSuccessHandler — user '{}' must change password", username);
            response.sendRedirect("/calls?mustChangePassword=true");
        } else {
            log.info("PostLoginSuccessHandler — user '{}' redirecting to /calls", username);
            response.sendRedirect("/calls");
        }
    }
}