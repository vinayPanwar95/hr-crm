package com.fms.hr_crm.calling.security;

import com.fms.hr_crm.calling.repository.RecruiterUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Loads a {@link com.fms.hr_crm.calling.model.entity.RecruiterUser} from the database
 * and wraps it as a Spring Security {@link UserDetails}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RecruiterUserDetailsService implements UserDetailsService {

    private final RecruiterUserRepository userRepo;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("loadUserByUsername() — username={}", username);

        var recruiter = userRepo.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn("loadUserByUsername() — user not found: {}", username);
                    return new UsernameNotFoundException("User not found: " + username);
                });

        var authority = new SimpleGrantedAuthority("ROLE_" + recruiter.getRole().name());
        return User.builder()
                .username(recruiter.getUsername())
                .password(recruiter.getPasswordHash())
                .authorities(List.of(authority))
                .disabled(!recruiter.isEnabled())
                .build();
    }
}