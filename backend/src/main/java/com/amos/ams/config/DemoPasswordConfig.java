package com.amos.ams.config;

import com.amos.ams.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@Profile("!test")
public class DemoPasswordConfig {
    private static final Logger log = LoggerFactory.getLogger(DemoPasswordConfig.class);

    @Bean
    CommandLineRunner alignDemoPasswords(UserRepository users, PasswordEncoder encoder) {
        return args -> {
            String hash = encoder.encode("Password123!");
            users.findAll().forEach(user -> {
                if (!encoder.matches("Password123!", user.getPasswordHash())) {
                    user.setPasswordHash(hash);
                    users.save(user);
                }
            });
            log.info("Demo users ready. Sign in with Password123! (admin, qa, manager, supervisor, engineer, tech1, tech2)");
        };
    }
}
