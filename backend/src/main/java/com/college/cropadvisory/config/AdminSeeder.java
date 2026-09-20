package com.college.cropadvisory.config;

import com.college.cropadvisory.model.entity.Role;
import com.college.cropadvisory.model.entity.User;
import com.college.cropadvisory.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Creates the first ADMIN account on startup.
 *
 * <p>Signup only offers FARMER and OFFICER, so the admin role cannot otherwise be obtained and the
 * admin screens would be unreachable. The seeder is inert unless <em>both</em> {@code ADMIN_EMAIL}
 * and {@code ADMIN_PASSWORD} are set, and it does nothing once any admin exists, so it is safe to
 * leave configured across restarts and redeploys.
 */
@Component
public class AdminSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminPassword;

    public AdminSeeder(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       @Value("${app.admin.email:}") String adminEmail,
                       @Value("${app.admin.password:}") String adminPassword) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            seedAdmin();
        } catch (Exception ex) {
            // A convenience bootstrap must never be the reason the application fails to start.
            log.warn("Admin bootstrap failed: {}", ex.getMessage());
        }
    }

    /**
     * @return {@code true} when an admin account was created, {@code false} when the seeder
     *         deliberately did nothing.
     */
    public boolean seedAdmin() {
        if (!StringUtils.hasText(adminEmail) || !StringUtils.hasText(adminPassword)) {
            log.info("ADMIN_EMAIL/ADMIN_PASSWORD not set - skipping admin bootstrap");
            return false;
        }
        if (userRepository.countByRole(Role.ADMIN) > 0) {
            log.info("An admin account already exists - skipping admin bootstrap");
            return false;
        }
        if (Boolean.TRUE.equals(userRepository.existsByEmail(adminEmail))) {
            log.warn("A user already exists with ADMIN_EMAIL {} - not promoting it automatically. "
                    + "Change their role from the admin user list instead.", adminEmail);
            return false;
        }

        User admin = new User();
        admin.setName("Platform Admin");
        admin.setEmail(adminEmail);
        admin.setPasswordHash(passwordEncoder.encode(adminPassword));
        admin.setRole(Role.ADMIN);
        userRepository.save(admin);

        log.info("Bootstrapped admin account for {}", adminEmail);
        return true;
    }
}
