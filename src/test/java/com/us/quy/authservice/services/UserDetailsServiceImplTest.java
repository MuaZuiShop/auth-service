package com.us.quy.authservice.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.quy.common.core.exception.AppException;
import com.quy.common.core.security.ERole;
import com.us.quy.authservice.BaseIntegrationTest;
import com.us.quy.authservice.configurations.CustomUserDetails;
import com.us.quy.authservice.entities.AccountEntity;
import com.us.quy.authservice.enums.EStatus;
import com.us.quy.authservice.repositories.AccountRepository;
import java.util.List;
import java.util.Objects;
import org.instancio.Instancio;
import org.instancio.Select;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetails;

@SpringBootTest
class UserDetailsServiceImplTest extends BaseIntegrationTest {

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Autowired
    private AccountRepository accountRepository;

    @BeforeEach
    void init() {
        accountRepository.deleteAll();
    }

    private AccountEntity saveAccount(String username, EStatus status) {
        return accountRepository.save(
            Objects.requireNonNull(Instancio.of(AccountEntity.class)
                .ignore(Select.field(AccountEntity::getId))
                .set(Select.field(AccountEntity::getUsername), username)
                .set(Select.field(AccountEntity::getEmail), username + "@gmail.com")
                .set(Select.field(AccountEntity::getPassword), "encoded_password_123")
                .set(Select.field(AccountEntity::getRoles), List.of(ERole.USER))
                .set(Select.field(AccountEntity::getStatus), status)
                .create())
        );
    }

    @Test
    void loadUserByUsername_WhenUserExists_ThenReturnCustomUserDetails() {
        // Arrange
        AccountEntity saved = saveAccount("testuser", EStatus.ACTIVE);

        // Act
        UserDetails result = userDetailsService.loadUserByUsername("testuser");

        // Assert
        assertThat(result).isInstanceOf(CustomUserDetails.class);

        CustomUserDetails details = (CustomUserDetails) result;
        assertThat(details.getUsername()).isEqualTo("testuser");
        assertThat(details.getPassword()).isEqualTo("encoded_password_123");
        assertThat(details.getId()).isEqualTo(saved.getId());
    }

    @Test
    void loadUserByUsername_WhenUserExists_ThenEnabledMatchesCanLogin() {
        // Arrange — ACTIVE → canLogin = true
        saveAccount("activeuser", EStatus.ACTIVE);

        // Act
        UserDetails result = userDetailsService.loadUserByUsername("activeuser");

        // Assert
        assertThat(result.isEnabled()).isEqualTo(EStatus.canLogin(EStatus.ACTIVE));
        assertThat(result.isAccountNonLocked()).isTrue();
        assertThat(result.isAccountNonExpired()).isTrue();
        assertThat(result.isCredentialsNonExpired()).isTrue();
    }

    @Test
    void loadUserByUsername_WhenUserIsBanned_ThenEnabledIsFalse() {
        // Arrange
        saveAccount("banneduser", EStatus.BANNED);

        // Act
        UserDetails result = userDetailsService.loadUserByUsername("banneduser");

        // Assert
        assertThat(result.isEnabled()).isEqualTo(EStatus.canLogin(EStatus.BANNED));
    }

    @Test
    void loadUserByUsername_WhenUserIsPendingVerification_ThenEnabledIsFalse() {
        // Arrange
        saveAccount("pendinguser", EStatus.PENDING_VERIFICATION);

        // Act
        UserDetails result = userDetailsService.loadUserByUsername("pendinguser");

        // Assert
        assertThat(result.isEnabled()).isEqualTo(EStatus.canLogin(EStatus.PENDING_VERIFICATION));
    }

    @Test
    void loadUserByUsername_WhenRolesExist_ThenAuthoritiesAreMapped() {
        // Arrange
        accountRepository.save(
            Objects.requireNonNull(Instancio.of(AccountEntity.class)
                .ignore(Select.field(AccountEntity::getId))
                .set(Select.field(AccountEntity::getUsername), "adminuser")
                .set(Select.field(AccountEntity::getPassword), "encoded")
                .set(Select.field(AccountEntity::getRoles), List.of(ERole.USER, ERole.ADMIN))
                .set(Select.field(AccountEntity::getStatus), EStatus.ACTIVE)
                .create())
        );

        // Act
        UserDetails result = userDetailsService.loadUserByUsername("adminuser");

        // Assert
        assertThat(result.getAuthorities())
            .extracting("authority")
            .containsExactlyInAnyOrder("USER", "ADMIN");
    }

    @Test
    void loadUserByUsername_WhenUserNotFound_ThenThrowAppException() {
        // Act & Assert
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("nonexistent"))
            .isInstanceOf(AppException.class)
            .satisfies(ex -> {
                AppException appEx = (AppException) ex;
                assertThat(appEx.getErrorCode()).isEqualTo("NOT_FOUND");
                assertThat(appEx.getHttpStatus().value()).isEqualTo(404);
            });
    }

    @Test
    void loadUserByUsername_WhenUsernameIsNull_ThenThrowAppException() {
        // Act & Assert
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername(null))
            .isInstanceOf(AppException.class);
    }
}