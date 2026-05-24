//package com.fms.hr_crm.calling.config;
//
//import com.fms.hr_crm.calling.model.entity.RecruiterRole;
//import com.fms.hr_crm.calling.service.RecruiterAuthService;
//import com.fms.hr_crm.calling.repository.RecruiterUserRepository;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.boot.ApplicationArguments;
//import org.springframework.boot.ApplicationRunner;
//import org.springframework.context.annotation.Profile;
//import org.springframework.stereotype.Component;
//
///**
// * Seeds a default admin account on first startup (local/dev only).
// *
// * <p>Default credentials: username=admin  password=Admin@1234
// * Change the password immediately after first login.
// */
//@Component
//@Profile("!test")
//@RequiredArgsConstructor
//@Slf4j
//public class DataInitializer implements ApplicationRunner {
//
//    private final RecruiterUserRepository userRepo;
//    private final RecruiterAuthService    authService;
//
//    @Override
//    public void run(ApplicationArguments args) {
//        if (!userRepo.existsByUsername("admin")) {
//            authService.createRecruiter(
//                    "admin",
//                    "admin@hrcrm.local",
//                    "Admin User",
//                    "Admin@1234",
//                    java.util.UUID.fromString("00000000-0000-0000-0000-000000000001"),
//                    RecruiterRole.ADMIN
//            );
//            log.info("DataInitializer — default admin created (username=admin, password=Admin@1234)");
//        } else {
//            log.debug("DataInitializer — admin account already exists, skipping seed");
//        }
//    }
//}