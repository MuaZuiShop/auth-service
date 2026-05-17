package com.us.quy.authservice.configurations;

import com.quy.common.core.security.ERole;
import com.us.quy.authservice.entities.AccountEntity;
import com.us.quy.authservice.enums.EStatus;
import com.us.quy.authservice.repositories.AccountRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
@Slf4j
@EnableAsync
public class ApplicationConfig {
    @Value("${spring.application.admin.default.username}")
    private String adminUsername;

    @Value("${spring.application.admin.default.password}")
    private String adminPassword;

    @Value("${spring.application.admin.default.email}")
    private String adminEmail;

    private final AccountRepository accountRepository;

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @ConditionalOnProperty(
        prefix = "spring",
        value = "datasource.driver-class-name",
        havingValue = "org.postgresql.Driver")
    ApplicationRunner applicationRunner() {
        log.info("Initializing application.....");
        return args -> {
            if (!accountRepository.existsAccountEntityByUsername(adminUsername)) {
                AccountEntity account = AccountEntity.builder()
                    .username(adminUsername)
                    .email(adminEmail)
                    .password(passwordEncoder().encode(adminPassword))
                    .roles(List.of(ERole.USER, ERole.ADMIN))
                    .status(EStatus.ACTIVE)
                    .build();
                accountRepository.save(account);
                log.warn("Create: admin account has been created: username = {} email = {}, password = {} ",
                    adminUsername, adminEmail, adminPassword);
            } else {
                log.warn("Admin account: username = {} email = {}, password = {} ",
                    adminUsername,
                    adminEmail,
                    adminPassword);
            }
            log.info("Application initialization completed .....");
        };
    }
}
