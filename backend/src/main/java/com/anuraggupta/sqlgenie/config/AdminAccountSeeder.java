package com.anuraggupta.sqlgenie.config;

import com.anuraggupta.sqlgenie.entity.Role;
import com.anuraggupta.sqlgenie.entity.User;
import com.anuraggupta.sqlgenie.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * There is no public API for creating an ADMIN account - that would be a
 * privilege-escalation hole. This dev-only seeder exists purely so there is
 * an admin account to demo against locally.
 */
@Component
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class AdminAccountSeeder implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.existsByEmail(adminEmail)) {
            return;
        }

        User admin = User.builder()
                .email(adminEmail)
                .passwordHash(passwordEncoder.encode(adminPassword))
                .fullName("Administrator")
                .role(Role.ADMIN)
                .enabled(true)
                .build();
        userRepository.save(admin);
        log.info("Seeded dev admin account: {}", adminEmail);
    }
}
