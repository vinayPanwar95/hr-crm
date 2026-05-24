package com.fms.hr_crm.calling.service;

import com.fms.hr_crm.calling.model.entity.PasswordResetToken;
import com.fms.hr_crm.calling.model.entity.RecruiterRole;
import com.fms.hr_crm.calling.model.entity.RecruiterUser;
import com.fms.hr_crm.calling.repository.PasswordResetTokenRepository;
import com.fms.hr_crm.calling.repository.RecruiterUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

/**
 * Handles recruiter registration, password reset token lifecycle, and password updates.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RecruiterAuthService {

    private final RecruiterUserRepository     userRepo;
    private final PasswordResetTokenRepository tokenRepo;
    private final PasswordEncoder             passwordEncoder;

    /**
     * Generates a password-reset token for the given email.
     * Returns the raw token string so the caller can display or email it.
     * Returns {@link Optional#empty()} if no account with that email exists.
     */
    @Transactional
    public Optional<String> requestPasswordReset(String email) {
        log.info("requestPasswordReset() — email=*** (redacted)");

        return userRepo.findByEmail(email).map(user -> {
            // Invalidate any existing tokens for this user
            tokenRepo.deleteExpiredOrUsed(Instant.now());

            var rawToken = UUID.randomUUID().toString().replace("-", "");
            var entity = new PasswordResetToken();
            entity.setToken(rawToken);
            entity.setRecruiterUser(user);
            entity.setExpiresAt(Instant.now().plus(1, ChronoUnit.HOURS));
            tokenRepo.save(entity);

            log.info("requestPasswordReset() — token created for userId={}", user.getId());
            return rawToken;
        });
    }

    /**
     * Validates a reset token and updates the password.
     *
     * @return {@code true} on success, {@code false} if token is invalid/expired/used
     */
    @Transactional
    public boolean resetPassword(String rawToken, String newPassword) {
        log.info("resetPassword() — attempting reset with token=***");

        var tokenOpt = tokenRepo.findByToken(rawToken);
        if (tokenOpt.isEmpty()) {
            log.warn("resetPassword() — token not found");
            return false;
        }

        var token = tokenOpt.get();
        if (token.isUsed() || token.getExpiresAt().isBefore(Instant.now())) {
            log.warn("resetPassword() — token expired or already used, userId={}",
                    token.getRecruiterUser().getId());
            return false;
        }

        var user = token.getRecruiterUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepo.save(user);

        token.setUsed(true);
        tokenRepo.save(token);

        log.info("resetPassword() — password updated for userId={}", user.getId());
        return true;
    }

    /**
     * Provisions a recruiter account from hr-lead-service with a one-time temporary password.
     * The {@code mustChangePassword} flag is set so the recruiter is forced to change it on login.
     * If the username already exists (re-provisioning), the password is reset.
     */
    @Transactional
    public RecruiterUser provisionRecruiter(UUID recruiterId, String username, String email,
                                             String fullName, String tempPassword, UUID tenantId) {
        log.info("provisionRecruiter() — username={} recruiterId={} tenantId={}", username, recruiterId, tenantId);

        var existing = userRepo.findByUsername(username);
        if (existing.isPresent()) {
            // Re-provision: reset password and force change
            var user = existing.get();
            user.setPasswordHash(passwordEncoder.encode(tempPassword));
            user.setMustChangePassword(true);
            var saved = userRepo.save(user);
            log.info("provisionRecruiter() — re-provisioned existing userId={}", saved.getId());
            return saved;
        }

        return createRecruiter(username, email, fullName, tempPassword, tenantId,
                RecruiterRole.RECRUITER, true);
    }

    /**
     * Changes the password for an authenticated user and clears the mustChangePassword flag.
     *
     * @return {@code true} on success, {@code false} if currentPassword is wrong
     */
    @Transactional
    public boolean changePassword(String username, String currentPassword, String newPassword) {
        log.info("changePassword() — username={}", username);

        var userOpt = userRepo.findByUsername(username);
        if (userOpt.isEmpty()) {
            log.warn("changePassword() — user not found: {}", username);
            return false;
        }

        var user = userOpt.get();
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            log.warn("changePassword() — wrong current password for username={}", username);
            return false;
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setMustChangePassword(false);
        userRepo.save(user);
        log.info("changePassword() — password changed for userId={}", user.getId());
        return true;
    }

    /**
     * Creates a new recruiter account. Throws if username or email already exists.
     */
    @Transactional
    public RecruiterUser createRecruiter(String username, String email,
                                         String fullName, String rawPassword,
                                         UUID tenantId, RecruiterRole role) {
        return createRecruiter(username, email, fullName, rawPassword, tenantId, role, false);
    }

    /**
     * Creates a new recruiter account. Throws if username or email already exists.
     */
    @Transactional
    public RecruiterUser createRecruiter(String username, String email,
                                         String fullName, String rawPassword,
                                         UUID tenantId, RecruiterRole role,
                                         boolean mustChangePassword) {
        log.info("createRecruiter() — username={} tenantId={}", username, tenantId);

        if (userRepo.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already taken: " + username);
        }
        if (userRepo.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already registered: " + email);
        }

        var user = new RecruiterUser();
        user.setUsername(username);
        user.setEmail(email);
        user.setFullName(fullName);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setTenantId(tenantId);
        user.setRole(role);
        user.setMustChangePassword(mustChangePassword);

        var saved = userRepo.save(user);
        log.info("createRecruiter() — created userId={} mustChangePassword={}", saved.getId(), mustChangePassword);
        return saved;
    }
}