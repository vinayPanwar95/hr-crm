package com.fms.hr_crm.calling.controller;

import com.fms.hr_crm.calling.service.RecruiterAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Handles login page, forgot-password, and reset-password flows.
 * Spring Security manages the actual credential check at POST /login.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final RecruiterAuthService authService;

    // ── Login ─────────────────────────────────────────────────────────────────

    @GetMapping("/login")
    public String loginPage(
            @RequestParam(required = false) String error,
            @RequestParam(required = false) String logout,
            Model model) {

        if (error != null)  model.addAttribute("error",  "Invalid username or password.");
        if (logout != null) model.addAttribute("message", "You have been logged out.");
        return "auth/login";
    }

    // ── Forgot password ───────────────────────────────────────────────────────

    @GetMapping("/auth/forgot-password")
    public String forgotPasswordPage() {
        return "auth/forgot-password";
    }

    /**
     * Accepts an email, generates a reset token, and shows the reset link.
     * In production this link would be emailed instead of displayed on screen.
     */
    @PostMapping("/auth/forgot-password")
    public String requestReset(
            @RequestParam String email,
            Model model) {

        log.info("requestReset() — received forgot-password request");
        var tokenOpt = authService.requestPasswordReset(email);

        if (tokenOpt.isPresent()) {
            // In production: send tokenOpt.get() via email instead of showing it
            model.addAttribute("resetToken", tokenOpt.get());
            log.info("requestReset() — reset token generated (displayed in dev mode)");
        } else {
            // Always show success to avoid email enumeration
            log.info("requestReset() — email not found, showing generic success");
        }

        model.addAttribute("submitted", true);
        return "auth/forgot-password";
    }

    // ── Reset password ────────────────────────────────────────────────────────

    @GetMapping("/auth/reset-password")
    public String resetPasswordPage(
            @RequestParam(required = false) String token,
            Model model) {

        if (token == null || token.isBlank()) {
            return "redirect:/auth/forgot-password";
        }
        model.addAttribute("token", token);
        return "auth/reset-password";
    }

    // ── Change password (first-login OTP flow) ────────────────────────────────

    @GetMapping("/auth/change-password")
    public String changePasswordPage(
            @AuthenticationPrincipal UserDetails principal,
            Model model) {

        if (principal == null) return "redirect:/login";
        model.addAttribute("username", principal.getUsername());
        return "auth/change-password";
    }

    @PostMapping("/auth/change-password")
    public String doChangePassword(
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam String currentPassword,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            RedirectAttributes ra) {

        if (principal == null) return "redirect:/login";
        log.info("doChangePassword() — username={}", principal.getUsername());

        if (!newPassword.equals(confirmPassword)) {
            ra.addFlashAttribute("error", "New passwords do not match.");
            return "redirect:/auth/change-password";
        }
        if (newPassword.length() < 8) {
            ra.addFlashAttribute("error", "Password must be at least 8 characters.");
            return "redirect:/auth/change-password";
        }
        if (newPassword.equals(currentPassword)) {
            ra.addFlashAttribute("error", "New password must be different from the temporary password.");
            return "redirect:/auth/change-password";
        }

        boolean ok = authService.changePassword(principal.getUsername(), currentPassword, newPassword);
        if (ok) {
            log.info("doChangePassword() — success for username={}", principal.getUsername());
            ra.addFlashAttribute("message", "Password changed successfully. Welcome!");
            return "redirect:/calls";
        } else {
            ra.addFlashAttribute("error", "Current password is incorrect.");
            return "redirect:/auth/change-password";
        }
    }

    @PostMapping("/auth/reset-password")
    public String doReset(
            @RequestParam String token,
            @RequestParam String password,
            @RequestParam String confirmPassword,
            RedirectAttributes ra) {

        log.info("doReset() — processing reset for token=***");

        if (!password.equals(confirmPassword)) {
            ra.addFlashAttribute("error", "Passwords do not match.");
            return "redirect:/auth/reset-password?token=" + token;
        }

        if (password.length() < 8) {
            ra.addFlashAttribute("error", "Password must be at least 8 characters.");
            return "redirect:/auth/reset-password?token=" + token;
        }

        boolean ok = authService.resetPassword(token, password);
        if (ok) {
            log.info("doReset() — password reset successful");
            ra.addFlashAttribute("message", "Password updated successfully. Please log in.");
            return "redirect:/login";
        } else {
            log.warn("doReset() — reset failed (token invalid/expired)");
            ra.addFlashAttribute("error", "Reset link is invalid or has expired. Please request a new one.");
            return "redirect:/auth/forgot-password";
        }
    }
}